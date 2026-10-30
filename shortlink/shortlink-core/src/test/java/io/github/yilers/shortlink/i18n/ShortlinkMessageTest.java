package io.github.yilers.shortlink.i18n;

import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.i18n.InfraMessageResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortlinkMessageTest {

    @Test
    void allMessagesExistInBothLanguages() throws Exception {
        ResourceBundle chinese = ResourceBundle.getBundle("i18n.shortlink-messages", Locale.ROOT);
        Properties english = new Properties();
        try (var input = getClass().getResourceAsStream("/i18n/shortlink-messages_en.properties")) {
            english.load(input);
        }
        for (ShortlinkMessage message : ShortlinkMessage.values()) {
            assertEquals(message.getDefaultMessage(), chinese.getString(message.getKey()));
            assertTrue(english.containsKey(message.getKey()), message.getKey());
        }
    }

    @Test
    void resolvesLiteralTemplateAndBusinessMessagesWithoutUpm() {
        InfraMessageResolver resolver = new InfraMessageResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-US");
        assertEquals("模板必须包含{code}", new CommonException(ShortlinkMessage.TEMPLATE_CODE_REQUIRED).getMessage());
        assertEquals("The template must contain {code}.",
                resolver.resolve(ShortlinkMessage.TEMPLATE_CODE_REQUIRED, new Object[0], request));
        assertEquals("Short link not found.",
                resolver.resolve(ShortlinkMessage.LINK_NOT_FOUND, new Object[0], request));
    }
}
