package io.github.yilers.flow.service;

import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import io.github.yilers.flow.config.FlowTenantConfig;
import io.github.yilers.flow.mapper.WorkflowInboxMapper;
import io.github.yilers.flow.service.impl.WorkflowInboxServiceImpl;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 使用真实Mapper、分页插件和Flow租户插件核验MySQL/PostgreSQL兼容查询。 */
class WorkflowInboxQueryTest {

    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void isolatesTenantsAndDeduplicatesCandidates(String mode) throws Exception {
        try (SqlSession session = session(mode)) {
            RequestContextHolder.setTenantId(1L);
            WorkflowInboxService service = service(session, "10", List.of("10", "user:10", "role:20"));
            var todo = service.todo(null, 1, 1);
            assertEquals(1, todo.total());
            assertEquals(100L, todo.records().getFirst().getInstanceId());
            assertTrue(todo.records().getFirst().isActionable());
            assertEquals(0, service.todo("other", 1, 20).total());
            assertEquals(1, service.done(null, 1, 20).total());
            assertEquals(2, service.started(null, 1, 20).total());
            assertThrows(CommonException.class, () -> service.detail(200L));
            assertThrows(CommonException.class, () -> service.detail(300L));
            assertThrows(CommonException.class, () -> service.todo(null, 0, 20));
            assertThrows(CommonException.class, () -> service.todo(null, 1, 101));

            RequestContextHolder.setTenantId(2L);
            assertEquals(1, service.todo(null, 1, 20).total());
            assertEquals(200L, service.todo(null, 1, 20).records().getFirst().getInstanceId());
            assertThrows(CommonException.class, () -> service.detail(100L));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void detailRequiresParticipationAndRejectsMissingIdentity(String mode) throws Exception {
        try (SqlSession session = session(mode)) {
            RequestContextHolder.setTenantId(1L);
            var service = service(session, "10", List.of("user:10"));
            var detail = service.detail(100L);
            assertEquals(1, detail.tasks().size());
            assertTrue(detail.tasks().getFirst().isActionable());
            assertEquals(2, detail.history().size());
            assertNull(detail.chartJson());

            var outsider = service(session, "99", List.of("user:99"));
            assertThrows(CommonException.class, () -> outsider.detail(100L));
            assertEquals(0, outsider.todo(null, 1, 20).total());
            assertThrows(CommonException.class, () -> service(session, "10", List.of()).todo(null, 1, 20));
            RequestContextHolder.clear();
            assertThrows(CommonException.class, () -> service.todo(null, 1, 20));
        }
    }

    private WorkflowInboxService service(SqlSession session, String userId, List<String> flags) {
        WorkflowIdentityProvider identity = new WorkflowIdentityProvider() {
            public String currentUserId() { return userId; }
            public List<String> currentPermissionFlags() { return flags; }
        };
        return new WorkflowInboxServiceImpl(identity, session.getMapper(WorkflowInboxMapper.class));
    }

    private SqlSession session(String mode) throws Exception {
        var source = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=" + mode + ";DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection connection = source.getConnection(); var statement = connection.createStatement()) {
            // 仅保留当前查询用到的官方字段，测试数据不依赖开发库。
            statement.execute("""
                    CREATE TABLE flow_definition(id BIGINT PRIMARY KEY, tenant_id VARCHAR(40), del_flag CHAR DEFAULT '0',
                      flow_code VARCHAR(40), flow_name VARCHAR(100), version VARCHAR(20), activity_status INT DEFAULT 1);
                    CREATE TABLE flow_instance(id BIGINT PRIMARY KEY, definition_id BIGINT, tenant_id VARCHAR(40),
                      del_flag CHAR DEFAULT '0', business_id VARCHAR(40), flow_status VARCHAR(20), create_by VARCHAR(64),
                      node_name VARCHAR(100), activity_status INT DEFAULT 1, create_time TIMESTAMP, update_time TIMESTAMP, def_json TEXT);
                    CREATE TABLE flow_task(id BIGINT PRIMARY KEY, instance_id BIGINT, tenant_id VARCHAR(40),
                      del_flag CHAR DEFAULT '0', node_name VARCHAR(100), node_type INT DEFAULT 1, create_time TIMESTAMP, update_time TIMESTAMP);
                    CREATE TABLE flow_user(id BIGINT PRIMARY KEY, associated BIGINT, tenant_id VARCHAR(40),
                      del_flag CHAR DEFAULT '0', type CHAR DEFAULT '1', processed_by VARCHAR(80));
                    CREATE TABLE flow_his_task(id BIGINT PRIMARY KEY, instance_id BIGINT, task_id BIGINT, tenant_id VARCHAR(40),
                      del_flag CHAR DEFAULT '0', node_name VARCHAR(100), node_type INT, approver VARCHAR(40), skip_type VARCHAR(10),
                      message VARCHAR(500), flow_status VARCHAR(20), create_time TIMESTAMP, update_time TIMESTAMP);
                    INSERT INTO flow_definition(id,tenant_id,flow_code,flow_name,version) VALUES
                      (1,'1','leave','请假','1'),(2,'2','leave','请假','1');
                    INSERT INTO flow_instance(id,definition_id,tenant_id,business_id,flow_status,create_by) VALUES
                      (100,1,'1','a','1','10'),(101,1,'1','b','8','10'),(200,2,'2','c','1','10'),(300,1,'1','d','1','99');
                    INSERT INTO flow_task(id,instance_id,tenant_id,node_name) VALUES
                      (1000,100,'1','审批'),(2000,200,'2','审批'),(3000,300,'1','未分派审批');
                    INSERT INTO flow_user(id,associated,tenant_id,processed_by) VALUES
                      (1,1000,'1','user:10'),(2,1000,'1','role:20'),(3,2000,'2','user:10');
                    INSERT INTO flow_his_task(id,instance_id,task_id,tenant_id,node_type,approver,skip_type) VALUES
                      (1,100,900,'1',0,'10','PASS'),(2,100,901,'1',1,'10','PASS'),(3,200,902,'2',1,'10','PASS');
                    """);
        }
        var configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), source));
        configuration.setMapUnderscoreToCamelCase(true);
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new FlowTenantConfig().flowTenantLineInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        configuration.addInterceptor(interceptor);
        String resource = "mapper/flow/WorkflowInboxMapper.xml";
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return new MybatisSqlSessionFactoryBuilder().build(configuration).openSession();
    }
}
