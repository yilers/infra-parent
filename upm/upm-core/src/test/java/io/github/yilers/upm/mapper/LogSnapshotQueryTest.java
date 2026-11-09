package io.github.yilers.upm.mapper;

import io.github.yilers.api.base.BasePageRequest;
import io.github.yilers.upm.entity.Log;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 验证日志查询使用操作时的姓名快照，而非用户当前姓名。 */
class LogSnapshotQueryTest {
    @Test
    void searchesAndReturnsSnapshotInBothDatabaseModes() throws Exception {
        Configuration configuration = new Configuration();
        try (var xml = getClass().getResourceAsStream("/mapper/LogMapper.xml")) {
            assertNotNull(xml);
            new XMLMapperBuilder(xml, configuration, "mapper/LogMapper.xml", configuration.getSqlFragments()).parse();
        }
        Log condition = new Log();
        condition.setOperator("张三");
        BasePageRequest<Log> request = new BasePageRequest<>();
        request.setData(condition);
        var boundSql = configuration.getMappedStatement(LogMapper.class.getName() + ".findByPage")
                .getBoundSql(Map.of("request", request));
        assertEquals(1, boundSql.getParameterMappings().size());
        for (String mode : new String[]{"MySQL", "PostgreSQL"}) {
            try (var connection = DriverManager.getConnection("jdbc:h2:mem:log_snapshot_" + mode + ";MODE=" + mode);
                 var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE upm_log (id BIGINT, module VARCHAR(50), action VARCHAR(50), success INT, "
                        + "create_id BIGINT, create_name VARCHAR(50), method VARCHAR(255), params VARCHAR(255), ip VARCHAR(50), "
                        + "duration INT, stack_trace VARCHAR(255), create_time TIMESTAMP, tenant_id BIGINT, region VARCHAR(50), deleted INT)");
                statement.execute("CREATE TABLE upm_tenant (id BIGINT, name VARCHAR(50))");
                statement.execute("INSERT INTO upm_tenant VALUES (1, '租户')");
                statement.execute("INSERT INTO upm_log (id, create_id, create_name, tenant_id, deleted) VALUES "
                        + "(1, 10, '张三', 1, 0), (2, 10, '张伟', 1, 0), (3, NULL, NULL, 1, 0)");
                try (var query = connection.prepareStatement(boundSql.getSql())) {
                    query.setString(1, "张三");
                    try (var result = query.executeQuery()) {
                        assertTrue(result.next());
                        assertEquals(1L, result.getLong("id"));
                        assertEquals("张三", result.getString("operatorName"));
                        assertFalse(result.next());
                    }
                }
            }
        }
    }
}
