package io.github.yilers.upm.handler;

import io.github.yilers.upm.request.UserRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class UserFieldRequestTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void userManagementGetterAnnotationsApplyWithoutChangingPersonalInfo() {
        var factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        factory.registerSingleton("policy", (io.github.yilers.api.field.FieldPermissionProvider) () -> java.util.Map.of(
                "system:user:phone", new io.github.yilers.api.field.FieldAccess(io.github.yilers.api.field.FieldViewMode.MASKED, false)));
        var builder = JsonMapper.builder();
        new io.github.yilers.web.field.FieldPermissionJacksonConfig().fieldPermissionCustomizer(
                new io.github.yilers.web.field.FieldPermissionService(factory.getBeanProvider(io.github.yilers.api.field.FieldPermissionProvider.class)))
                .customize(builder);
        var json = builder.build();
        var original = new io.github.yilers.upm.response.UserInfoResponse();
        original.setPhone("13812345678");
        original.setEmail("a@example.com");
        var response = cn.hutool.v7.extra.spring.cglib.CglibUtil.copy(original, io.github.yilers.upm.response.UserManagementResponse.class);
        assertEquals("138****5678", json.valueToTree(response).get("phone").asString());
        assertFalse(json.valueToTree(response).has("email"));
        assertEquals("13812345678", original.getPhone());
        assertEquals("a@example.com", json.valueToTree(original).get("email").asString());
    }

    @Test
    void distinguishesOmittedFromExplicitNullAndRejectsPresenceFlagSpoofing() {
        UserRequest omitted = mapper.readValue("{}", UserRequest.class);
        assertFalse(omitted.isPhoneProvided());
        assertFalse(omitted.isEmailProvided());
        UserRequest clear = mapper.readValue("{\"phone\":null,\"email\":null}", UserRequest.class);
        assertTrue(clear.isPhoneProvided());
        assertTrue(clear.isEmailProvided());
        UserRequest spoof = mapper.readValue("{\"phone\":\"13812345678\",\"phoneProvided\":false}", UserRequest.class);
        assertTrue(spoof.isPhoneProvided());
        assertFalse(mapper.writeValueAsString(spoof).contains("phoneProvided"));
    }
}
