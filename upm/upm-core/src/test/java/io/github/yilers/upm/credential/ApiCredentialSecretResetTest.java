package io.github.yilers.upm.credential;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import io.github.yilers.upm.mapper.ApiCredentialMapper;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/** 使用独立H2数据库核验重置SQL、租户过滤与乐观锁。 */
class ApiCredentialSecretResetTest {
    private JdbcDataSource dataSource;
    private ApiSecretCipher cipher;
    private SqlSessionFactory sessionFactory;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:credential_reset_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        cipher = new ApiSecretCipher(Base64.getEncoder().encodeToString(new byte[32]));
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE upm_api_credential (
                    id BIGINT PRIMARY KEY, name VARCHAR(100), access_key VARCHAR(64), secret_cipher VARCHAR(512),
                    expire_time TIMESTAMP, rate_limit INT, tenant_id BIGINT, version INT, usable INT, operable INT,
                    deleted INT, create_id BIGINT, create_time TIMESTAMP, update_time TIMESTAMP
                )
                """);
            try (var insert = connection.prepareStatement("""
                INSERT INTO upm_api_credential(id,name,access_key,secret_cipher,rate_limit,tenant_id,version,usable,operable,deleted)
                VALUES(1,'调用系统','existing-key',?,600,10,1,1,1,0)
                """)) {
                insert.setString(1, cipher.encrypt("old-secret"));
                insert.executeUpdate();
            }
        }
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), dataSource));
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(RequestContextHolder.getTenantId());
            }
        }));
        configuration.addInterceptor(interceptor);
        configuration.addMapper(ApiCredentialMapper.class);
        sessionFactory = new MybatisSqlSessionFactoryBuilder().build(configuration);
        RequestContextHolder.setTenantId(10L);
    }

    @AfterEach
    void cleanUp() {
        RequestContextHolder.clear();
    }

    @Test
    void resetsSecretWithoutChangingKeyAndRejectsStaleVersionOrAnotherTenant() throws Exception {
        try (SqlSession session = sessionFactory.openSession(true)) {
            ApiCredentialMapper mapper = session.getMapper(ApiCredentialMapper.class);
            ApiCredentialHandler handler = new ApiCredentialHandler(mapper, null, null, cipher);
            var result = handler.resetSecret(1L, 1);
            assertEquals("existing-key", result.accessKey());
            assertNotEquals("old-secret", result.secret());
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT * FROM upm_api_credential WHERE id=1")) {
                assertTrue(rows.next());
                assertEquals(result.secret(), cipher.decrypt(rows.getString("secret_cipher")));
                assertEquals(2, rows.getInt("version"));
                assertEquals(600, rows.getInt("rate_limit"));
                assertEquals(1, rows.getInt("usable"));
                assertEquals("调用系统", rows.getString("name"));
            }
            assertThrows(CommonException.class, () -> handler.resetSecret(1L, 1));
            RequestContextHolder.setTenantId(20L);
            assertThrows(CommonException.class, () -> handler.resetSecret(1L, 2));
        }
    }
}
