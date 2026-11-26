package io.github.yilers.web.field;

import io.github.yilers.api.field.*;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** 验证序列化边界及写入边界，不依赖UPM或数据库。 */
class FieldPermissionTest {
    private static final String KEY = "system:user:phone";
    private final AtomicReference<Map<String, FieldAccess>> access = new AtomicReference<>(Map.of());
    private final FieldPermissionService permissions = createPermissions();

    private FieldPermissionService createPermissions() {
        var factory = new DefaultListableBeanFactory();
        factory.registerSingleton("policy", (FieldPermissionProvider) access::get);
        return new FieldPermissionService(factory.getBeanProvider(FieldPermissionProvider.class));
    }

    private JsonMapper mapper() {
        var builder = JsonMapper.builder();
        new FieldPermissionJacksonConfig().fieldPermissionCustomizer(permissions).customize(builder);
        return builder.build();
    }

    @Test
    void nestedResponseUsesCurrentPolicyWithoutMutatingSharedObjects() {
        var person = new Contact("张三", "13812345678");
        var mapper = mapper();
        var payload = Map.of("records", List.of(person));
        assertFalse(mapper.writeValueAsString(payload).contains("phone"));
        access.set(Map.of(KEY, new FieldAccess(FieldViewMode.MASKED, false)));
        assertEquals("138****5678", mapper.valueToTree(payload).get("records").get(0).get("phone").asString());
        access.set(Map.of(KEY, FieldAccess.FULL));
        assertEquals("13812345678", mapper.valueToTree(payload).get("records").get(0).get("phone").asString());
        assertEquals("13812345678", person.phone());
        assertEquals("张三", mapper.valueToTree(payload).get("records").get(0).get("name").asString());
    }

    @Test
    void noProviderDefaultsToHiddenOnlyForAnnotatedFields() {
        var factory = new DefaultListableBeanFactory();
        var builder = JsonMapper.builder();
        new FieldPermissionJacksonConfig().fieldPermissionCustomizer(
                new FieldPermissionService(factory.getBeanProvider(FieldPermissionProvider.class))).customize(builder);
        assertEquals("{\"name\":\"张三\"}", builder.build().writeValueAsString(new Contact("张三", "13812345678")));
    }

    @Test
    void omittedFieldsArePreservedAndMaskedFieldsCannotBeWrittenBack() {
        assertDoesNotThrow(() -> permissions.checkUpdate(KEY, "手机号", false, "13812345678", null));
        assertThrows(CommonException.class, () -> permissions.checkUpdate(KEY, "手机号", true, null, null));
        access.set(Map.of(KEY, new FieldAccess(FieldViewMode.MASKED, false)));
        assertThrows(CommonException.class, () -> permissions.checkUpdate(KEY, "手机号", true, "13812345678", "138****5678"));
        access.set(Map.of(KEY, new FieldAccess(FieldViewMode.FULL, false)));
        assertDoesNotThrow(() -> permissions.checkUpdate(KEY, "手机号", true, "13812345678", "13812345678"));
        assertThrows(CommonException.class, () -> permissions.checkUpdate(KEY, "手机号", true, "13812345678", null));
        access.set(Map.of(KEY, FieldAccess.FULL));
        assertDoesNotThrow(() -> permissions.checkUpdate(KEY, "手机号", true, "13812345678", null));
    }

    @Test
    void multipleRolesTakeUnionAndMaskedCanNeverBeEditable() {
        var masked = new FieldAccess(FieldViewMode.MASKED, true);
        assertFalse(masked.editable());
        assertEquals(FieldViewMode.FULL, masked.union(new FieldAccess(FieldViewMode.FULL, false)).viewMode());
        assertFalse(masked.union(new FieldAccess(FieldViewMode.FULL, false)).editable());
        assertTrue(masked.union(FieldAccess.FULL).editable());
    }

    public record Contact(String name,
            @FieldPermission(resource = "system:user", resourceName = "用户", field = "phone", name = "手机号", mask = FieldMaskType.PHONE)
            String phone) { }
}
