package io.github.yilers.upm.i18n;

import io.github.yilers.upm.controller.UserController;
import io.github.yilers.upm.handler.UserBatchHandler;
import io.github.yilers.upm.handler.UserHandler;
import io.github.yilers.upm.request.UserBatchAddRequest;
import io.github.yilers.upm.response.UserBatchAddResponse;
import io.github.yilers.upm.response.UserBatchRowResponse;
import io.github.yilers.web.i18n.InfraMessageResolver;
import io.github.yilers.web.i18n.LocalizedTextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpmTextMessageTest {

    @Test
    void allTextMessagesHaveBothTranslations() throws Exception {
        ResourceBundle chinese = ResourceBundle.getBundle("i18n.upm-messages", Locale.ROOT);
        Properties english = new Properties();
        try (var input = getClass().getResourceAsStream("/i18n/upm-messages_en.properties")) {
            english.load(input);
        }
        for (UpmTextMessage message : UpmTextMessage.values()) {
            assertEquals(message.getDefaultMessage(), chinese.getString(message.getKey()));
            assertTrue(english.containsKey(message.getKey()));
        }
    }

    @Test
    void batchResponseTranslatesMessagesWithoutChangingStructure() {
        LocalizedTextResolver resolver = new LocalizedTextResolver(new InfraMessageResolver(),
                List.of(new UpmMessageTextCatalog()));
        UserBatchHandler handler = mock(UserBatchHandler.class);
        UserController controller = new UserController(mock(UserHandler.class), handler, resolver);
        UserBatchAddRequest request = new UserBatchAddRequest();
        UserBatchRowResponse row = new UserBatchRowResponse();
        row.setRowKey("row-1");
        row.setMessages(List.of("账号已存在", "所属部门不存在", "未知错误"));
        UserBatchAddResponse response = new UserBatchAddResponse();
        response.setRows(List.of(row));
        response.setFailureCount(1);
        when(handler.add(request)).thenReturn(response);
        HttpServletRequest http = mock(HttpServletRequest.class);
        when(http.getHeader("Accept-Language")).thenReturn("en-US");
        when(http.getLocales()).thenAnswer(invocation -> Collections.enumeration(List.of(Locale.US)));

        assertEquals("Role ID is required.", resolver.resolve("角色id不能为空", http));
        assertEquals("Authentication platform is required.", resolver.resolve("认证平台不能为空", http));
        assertEquals("Application name is required.", resolver.resolve("应用名称不能为空", http));
        assertEquals("机构名称不能为空", resolver.resolve("机构名称不能为空", null));

        UserBatchAddResponse result = controller.batchAdd(request, http).getData();
        assertEquals(1, result.getFailureCount());
        assertEquals("row-1", result.getRows().getFirst().getRowKey());
        assertEquals(List.of("The account already exists.", "Department not found.", "未知错误"),
                result.getRows().getFirst().getMessages());
    }
}
