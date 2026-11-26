package io.github.yilers.upm.handler;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.yilers.upm.entity.RoleField;
import io.github.yilers.upm.mapper.RoleFieldMapper;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 通过真实MyBatis SQL验证删除关联后可以反复重新授权，不依赖逻辑删除字段。 */
class RoleFieldPhysicalDeleteTest {
    @Test
    void deletingAssociationRemovesRowAndAllowsRepeatedGranting() throws Exception {
        var datasource = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:role_field_" + UUID.randomUUID(), "sa", "");
        var configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), datasource));
        configuration.addMapper(RoleFieldMapper.class);
        try (var session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession()) {
            try (var statement = session.getConnection().createStatement()) {
                statement.execute("CREATE TABLE upm_role_field (tenant_id BIGINT NOT NULL,role_id BIGINT NOT NULL,field_id BIGINT NOT NULL,view_mode VARCHAR(20) NOT NULL,editable BOOLEAN NOT NULL,UNIQUE(tenant_id,role_id,field_id))");
            }
            assertFalse(TableInfoHelper.getTableInfo(RoleField.class).isWithLogicDelete());
            var mapper = session.getMapper(RoleFieldMapper.class);
            for (int round = 0; round < 3; round++) {
                RoleField grant = new RoleField();
                grant.setTenantId(1L);
                grant.setRoleId(10L);
                grant.setFieldId(20L);
                grant.setViewMode("FULL");
                grant.setEditable(true);
                assertEquals(1, mapper.insert(grant));
                assertEquals(1, mapper.delete(Wrappers.<RoleField>lambdaQuery()
                        .eq(RoleField::getTenantId, 1L).eq(RoleField::getRoleId, 10L).eq(RoleField::getFieldId, 20L)));
                try (var statement = session.getConnection().createStatement();
                     var result = statement.executeQuery("SELECT COUNT(*) FROM upm_role_field")) {
                    assertTrue(result.next());
                    assertEquals(0, result.getInt(1));
                }
            }
        }
    }
}
