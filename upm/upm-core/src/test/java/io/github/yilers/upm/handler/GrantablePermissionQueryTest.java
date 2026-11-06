package io.github.yilers.upm.handler;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 验证授权范围 SQL 对角色、权限及租户的过滤，不代替实库测试。 */
class GrantablePermissionQueryTest {
    @Test
    void onlyPermissionsFromEnabledRolesAndMatchingTenantAreGrantable() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        var document = factory.newDocumentBuilder().parse(getClass().getResourceAsStream("/mapper/PermissionMapper.xml"));
        var selects = document.getElementsByTagName("select");
        String sql = null;
        for (int i = 0; i < selects.getLength(); i++) {
            Element element = (Element) selects.item(i);
            if ("findGrantablePermissions".equals(element.getAttribute("id"))) {
                sql = element.getTextContent();
                break;
            }
        }
        org.junit.jupiter.api.Assertions.assertNotNull(sql);
        sql = sql.replace("#{userId}", "1").replace("#{notDeleted}", "0")
                .replace("#{usable}", "1").replace("#{device}", "'web'").replace("#{appId}", "10");
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:grantable;MODE=PostgreSQL");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE upm_user (id BIGINT, tenant_id BIGINT, deleted INT)");
            statement.execute("CREATE TABLE upm_user_role (user_id BIGINT, role_id BIGINT, tenant_id BIGINT)");
            statement.execute("CREATE TABLE upm_role (id BIGINT, tenant_id BIGINT, deleted INT, usable INT)");
            statement.execute("CREATE TABLE upm_role_permission (role_id BIGINT, permission_id BIGINT, tenant_id BIGINT)");
            statement.execute("CREATE TABLE upm_permission (id BIGINT, tenant_id BIGINT, deleted INT, usable INT, device VARCHAR(10), app_id BIGINT)");
            statement.execute("INSERT INTO upm_user VALUES (1, 1, 0)");
            statement.execute("INSERT INTO upm_role VALUES (1,1,0,1),(2,1,0,0),(3,1,1,1),(4,2,0,1)");
            statement.execute("INSERT INTO upm_user_role VALUES (1,1,1),(1,2,1),(1,3,1),(1,4,1)");
            statement.execute("INSERT INTO upm_permission VALUES (100,1,0,1,'web',10),(101,1,0,1,'web',10),"
                    + "(102,1,0,0,'web',10),(103,1,0,1,'app',10),(104,1,0,1,'web',20),"
                    + "(105,1,0,1,'web',10),(106,2,0,1,'web',10),(107,1,1,1,'web',10)");
            statement.execute("INSERT INTO upm_role_permission VALUES (1,100,1),(2,101,1),(1,102,1),(1,103,1),"
                    + "(1,104,1),(3,105,1),(4,106,2),(1,107,1)");
            List<Long> ids = new ArrayList<>();
            try (var result = statement.executeQuery(sql)) {
                while (result.next()) {
                    ids.add(result.getLong(1));
                }
            }
            assertEquals(List.of(100L), ids);
        }
    }
}
