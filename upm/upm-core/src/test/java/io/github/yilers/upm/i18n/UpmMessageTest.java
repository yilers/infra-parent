package io.github.yilers.upm.i18n;

import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.i18n.InfraMessageResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpmMessageTest {

    @Test
    void resolvesMessagesFromUpmModuleResources() {
        InfraMessageResolver resolver = new InfraMessageResolver();
        HttpServletRequest english = mock(HttpServletRequest.class);
        when(english.getHeader("Accept-Language")).thenReturn("en-US");
        when(english.getLocales()).thenReturn(Collections.enumeration(java.util.List.of(Locale.US)));
        assertEquals("User not found.", resolver.resolve(UpmMessage.USER_NOT_FOUND, new Object[0], english));
        assertEquals("用户不存在", resolver.resolve(UpmMessage.USER_NOT_FOUND, new Object[0], null));
        assertEquals("用户不存在", new CommonException(UpmMessage.USER_NOT_FOUND).getMessage());
    }
}
