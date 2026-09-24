package io.github.yilers.upm.handler;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 内存数据库验证迁移数据不变量，不替代MySQL/PostgreSQL实库迁移验收。
 */
class ApplicationMigrationTest {
    @Test
    void fullScriptsContainCurrentSchemaAndApplicationSeedsWithoutMigration() throws Exception {
        for (String dialect : new String[]{"mysql", "postgres"}) {
            String mode = "mysql".equals(dialect) ? "MySQL" : "PostgreSQL";
            try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:full_" + dialect + ";MODE=" + mode)) {
                String sql = Files.readString(Path.of("../../sql/full/" + dialect + ".sql"));
                org.junit.jupiter.api.Assertions.assertFalse(sql.contains("ALTER TABLE upm_permission ADD"));
                // 只验证应用关联涉及的表及种子；H2不支持的引擎选项和内联注释不参与此测试。
                var statements = java.util.regex.Pattern.compile(
                        "(?s)(?:CREATE TABLE|INSERT INTO) `?(?:upm_application|upm_permission|upm_role|upm_role_permission|upm_tenant)`?\\s*\\([^;]+;")
                        .matcher(sql);
                while (statements.find()) {
                    String statement = statements.group().replaceAll("(?s)\\) ENGINE=[^;]+;", ");");
                    if (statement.startsWith("CREATE TABLE")) {
                        statement = statement.replaceAll("\\s+COMMENT\\s+'(?:''|[^'])*'", "");
                    }
                    RunScript.execute(connection, new StringReader(statement));
                }
                assertEquals(1, count(connection, "SELECT COUNT(*) FROM upm_application WHERE code='infra' AND operable=0"));
                assertEquals(49, count(connection, "SELECT COUNT(*) FROM upm_permission"));
                assertEquals(0, count(connection, "SELECT COUNT(*) FROM upm_permission WHERE app_id<>1 OR app_id IS NULL"));
                assertEquals(1, count(connection, "SELECT COUNT(*) FROM upm_permission WHERE permission_code='system:tenant:sync' AND source_id IS NULL"));
                assertEquals(1, count(connection, "SELECT COUNT(*) FROM upm_role_permission WHERE role_id=10 AND permission_id=195"));
                assertEquals(10, count(connection, "SELECT COUNT(*) FROM upm_role_permission rp JOIN upm_permission p ON p.id=rp.permission_id WHERE p.component='system/application/index' OR p.permission_code LIKE 'system:application:%'"));
            }
        }
    }

    @Test
    void mysqlMenuIdsDoNotRequireWindowFunctionsAndSkipDeletedTenants() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:applicationMysqlIds;MODE=MySQL")) {
            RunScript.execute(connection, new StringReader("""
                    CREATE TABLE upm_tenant (id BIGINT PRIMARY KEY, deleted INT);
                    CREATE TABLE upm_permission (id BIGINT PRIMARY KEY, tenant_id BIGINT, parent_id BIGINT,
                        component VARCHAR(100), device VARCHAR(20), deleted INT);
                    INSERT INTO upm_tenant VALUES (1, 0), (10, 1), (20, 0);
                    INSERT INTO upm_permission VALUES (200, 1, 100, 'system/menu/index', 'web', 0);
                    """));
            String sql = Files.readString(Path.of("../../sql/migration/20260907_application.mysql.sql"));
            int start = sql.indexOf("CREATE TEMPORARY TABLE migration_application_menu AS");
            String statement = sql.substring(start, sql.indexOf(';', start) + 1);
            org.junit.jupiter.api.Assertions.assertFalse(statement.contains("ROW_NUMBER"));
            RunScript.execute(connection, new StringReader(statement));
            assertEquals(2, count(connection, "SELECT COUNT(*) FROM migration_application_menu"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM migration_application_menu WHERE tenant_id=1 AND parent_id=100 AND menu_id=205"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM migration_application_menu WHERE tenant_id=20 AND parent_id=0 AND menu_id=210"));
        }
    }

    @Test
    void migrationPreservesMenuIdsAndFlagsAndAddsTenantLocalApplications() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:applicationMigration;MODE=PostgreSQL")) {
            RunScript.execute(connection, new StringReader("""
                    CREATE TABLE upm_tenant (id BIGINT PRIMARY KEY, deleted INT DEFAULT 0);
                    CREATE TABLE upm_role (id BIGINT PRIMARY KEY, tenant_id BIGINT, role_code VARCHAR(64), deleted INT DEFAULT 0);
                    CREATE TABLE upm_permission (
                        id BIGINT PRIMARY KEY, parent_id BIGINT, tenant_id BIGINT,
                        permission_name VARCHAR(100), permission_type INT, permission_code VARCHAR(100),
                        menu_url VARCHAR(255), component VARCHAR(100), menu_icon VARCHAR(100),
                        sort_number INT, device VARCHAR(20), operable INT, usable INT, deleted INT DEFAULT 0, version INT);
                    CREATE TABLE upm_role_permission (role_id BIGINT, permission_id BIGINT, tenant_id BIGINT, device VARCHAR(20));
                    INSERT INTO upm_tenant VALUES (1, 0), (20, 0);
                    INSERT INTO upm_role VALUES (10, 1, 'platformAdmin', 0), (20, 1, 'tenantAdmin', 0), (30, 20, 'platformAdmin', 0);
                    INSERT INTO upm_permission (id, parent_id, tenant_id, component, device, operable, usable)
                    VALUES (100, 0, 1, '', 'web', 0, 1), (101, 100, 1, 'system/menu/index', 'web', 0, 1),
                           (200, 0, 20, '', 'app', 1, 0);
                    INSERT INTO upm_role_permission VALUES (10, 101, 1, 'web');
                    """));
            String sql = Files.readString(Path.of("../../sql/migration/20260907_application.postgres.sql"));
            RunScript.execute(connection, new StringReader(sql));
            assertEquals(2, count(connection, "SELECT COUNT(*) FROM upm_application WHERE operable=0 AND code='infra'"));
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM upm_permission p LEFT JOIN upm_application a ON a.id=p.app_id AND a.tenant_id=p.tenant_id WHERE a.id IS NULL"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM upm_permission WHERE id=200 AND usable=0 AND operable=1 AND device='app' AND app_id=20"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM upm_permission WHERE id=101 AND parent_id=100 AND operable=0"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM upm_role_permission WHERE role_id=10 AND permission_id=101"));
            assertEquals(13, count(connection, "SELECT COUNT(*) FROM upm_permission"));
            assertEquals(16, count(connection, "SELECT COUNT(*) FROM upm_role_permission"));
        }
    }

    private int count(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }
}
