package io.github.yilers.upm.i18n;

import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.i18n.InfraMessageResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpmMessageTest {

    @Test
    void resolvesMessagesFromUpmModuleResources() {
        InfraMessageResolver resolver = new InfraMessageResolver();
        HttpServletRequest english = mock(HttpServletRequest.class);
        when(english.getHeader("Accept-Language")).thenReturn("en-US");
        when(english.getLocales()).thenAnswer(invocation -> Collections.enumeration(java.util.List.of(Locale.US)));
        assertEquals("User not found.", resolver.resolve(UpmMessage.USER_NOT_FOUND, new Object[0], english));
        assertEquals("用户不存在", resolver.resolve(UpmMessage.USER_NOT_FOUND, new Object[0], null));
        assertEquals("用户不存在", new CommonException(UpmMessage.USER_NOT_FOUND).getMessage());
        assertEquals("Too many failed login attempts. Please try again in 10 minutes.",
                resolver.resolve(UpmMessage.LOGIN_ACCOUNT_LOCKED, new Object[]{10}, english));
        assertEquals("登录失败次数过多，请10分钟后再试",
                new CommonException(UpmMessage.LOGIN_ACCOUNT_LOCKED, 10).getMessage());
    }

    @Test
    void allDefinedMessagesHaveChineseAndEnglishEntries() throws Exception {
        ResourceBundle chinese = ResourceBundle.getBundle("i18n.upm-messages", Locale.ROOT);
        Properties english = new Properties();
        try (var input = getClass().getResourceAsStream("/i18n/upm-messages_en.properties")) {
            english.load(input);
        }
        for (UpmMessage message : UpmMessage.values()) {
            assertEquals(message.getDefaultMessage(), chinese.getString(message.getKey()));
            org.junit.jupiter.api.Assertions.assertTrue(english.keySet().contains(message.getKey()));
        }
    }
}
