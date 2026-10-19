package io.github.yilers.flow.service;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审批中心菜单迁移回归：执行真实增量，并核对全量包含同一修正版。
 * H2不能复现MySQL错误1137，因此另外限制每条语句只引用一次同一临时表。
 */
class WorkflowMigrationScriptTest {

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void insertsContinuousMenusAndPermissionsWithoutReopeningTemporaryTables(String mode) throws Exception {
        Path root = repositoryRoot();
        String suffix = mode.equals("MySQL") ? "mysql" : "postgres";
        Path inbox = root.resolve("sql/migration/20261019_flow_inbox." + suffix + ".sql");
        Path actions = root.resolve("sql/migration/20261019_flow_task_actions." + suffix + ".sql");
        Path leave = root.resolve("sql/migration/20261019_flow_leave." + suffix + ".sql");
        String full = Files.readString(root.resolve("sql/full/" + suffix + ".sql"));
        for (Path script : new Path[]{inbox, actions, leave}) {
            String sql = Files.readString(script);
            assertTrue(full.contains(sql.trim()), "全量脚本必须包含同一份已验证的迁移内容");
            assertSingleTemporaryTableReference(sql);
        }

        var source = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=" + mode, "sa", "");
        try (Connection connection = source.getConnection(); var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE upm_application(id BIGINT, tenant_id BIGINT, code VARCHAR(40), deleted INT);
                    CREATE TABLE upm_permission(id BIGINT PRIMARY KEY, parent_id BIGINT, app_id BIGINT, tenant_id BIGINT,
                      permission_name VARCHAR(100), permission_type INT, menu_url VARCHAR(100), menu_icon VARCHAR(100),
                      component VARCHAR(100), permission_code VARCHAR(100), sort_number INT, device VARCHAR(20),
                      operable INT, usable INT, deleted INT, version INT);
                    CREATE TABLE upm_role(id BIGINT PRIMARY KEY, tenant_id BIGINT);
                    CREATE TABLE upm_role_permission(role_id BIGINT, permission_id BIGINT, tenant_id BIGINT, device VARCHAR(20));
                    CREATE TABLE flow_instance(id BIGINT, tenant_id VARCHAR(40), create_by VARCHAR(64), create_time TIMESTAMP);
                    CREATE TABLE flow_his_task(id BIGINT, tenant_id VARCHAR(40), approver VARCHAR(40), node_type INT,
                      update_time TIMESTAMP, instance_id BIGINT);
                    CREATE TABLE flow_task(id BIGINT, tenant_id VARCHAR(40), instance_id BIGINT);
                    CREATE TABLE flow_user(id BIGINT, tenant_id VARCHAR(40), associated BIGINT, type CHAR, processed_by VARCHAR(80));
                    INSERT INTO upm_application VALUES(1, 1, 'infra', 0);
                    INSERT INTO upm_permission(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,deleted)
                      VALUES(197,0,1,1,'OA审批',0,'flow',0),(203,198,1,1,'原有按钮',2,NULL,0);
                    INSERT INTO upm_role VALUES(10,1),(20,1);
                    """);
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(inbox));
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(actions));
            assertEquals(10, scalar(connection, "SELECT COUNT(*) FROM upm_permission"));
            assertEquals(8, scalar(connection, "SELECT COUNT(*) FROM upm_permission WHERE id BETWEEN 204 AND 211"));
            assertEquals(204, scalar(connection, "SELECT id FROM upm_permission WHERE component='flow/todo/index'"));
            assertEquals(204, scalar(connection, "SELECT parent_id FROM upm_permission WHERE permission_code='flow:task:reject'"));
            assertEquals(16, scalar(connection, "SELECT COUNT(*) FROM upm_role_permission"));
            assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM upm_permission WHERE id=203 AND permission_name='原有按钮'"));
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(leave));
            assertEquals(16, scalar(connection, "SELECT COUNT(*) FROM upm_permission"));
            assertEquals(28, scalar(connection, "SELECT COUNT(*) FROM upm_role_permission"));
            assertEquals(5, scalar(connection, "SELECT sort_number FROM upm_permission WHERE component='flow/leave/index'"));
            assertEquals(2, scalar(connection, "SELECT sort_number FROM upm_permission WHERE component='flow/todo/index'"));
            assertEquals(3, scalar(connection, "SELECT sort_number FROM upm_permission WHERE component='flow/done/index'"));
            assertEquals(4, scalar(connection, "SELECT sort_number FROM upm_permission WHERE component='flow/started/index'"));
        }
    }

    private void assertSingleTemporaryTableReference(String sql) {
        for (String statement : sql.split(";")) {
            for (String table : new String[]{"migration_flow_inbox_ids", "migration_flow_task_ids", "migration_flow_leave_ids"}) {
                long count = Pattern.compile("\\b(?:FROM|JOIN)\\s+" + table + "\\b", Pattern.CASE_INSENSITIVE)
                        .matcher(statement).results().count();
                assertTrue(count <= 1, "同一条语句不能重复读取MySQL临时表：" + table);
            }
        }
    }

    private long scalar(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private Path repositoryRoot() throws Exception {
        Path path = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        while (path != null && !Files.isDirectory(path.resolve("sql/migration"))) {
            path = path.getParent();
        }
        if (path == null) {
            throw new IllegalStateException("未找到仓库SQL目录");
        }
        return path;
    }
}
