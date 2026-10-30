package io.github.yilers.auth.api;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiAuthMessagesTest {

    @Test
    void resolvesEnglishAndFallsBackToChinese() {
        MockHttpServletRequest english = new MockHttpServletRequest();
        english.addHeader("Accept-Language", "en-US");
        assertEquals("System-call authentication failed.",
                ApiAuthMessages.resolve(ApiAuthMessages.Code.AUTH_FAILED, english));
        assertEquals("系统调用认证失败", ApiAuthMessages.resolve(ApiAuthMessages.Code.AUTH_FAILED,
                new MockHttpServletRequest()));
    }
}
