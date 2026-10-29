package io.github.yilers.web.i18n;

import io.github.yilers.core.util.Result;
import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfraMessageResolverTest {

    private final InfraMessageResolver resolver = new InfraMessageResolver();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(resolver);

    @Test
    void resolvesCommonMessagesInChineseAndEnglish() {
        CommonException exception = new CommonException(CommonMessage.SAVE_FAILED);
        assertEquals("保存失败", handler.commonExceptionHandler(exception, request(null)).getMsg());
        assertEquals("保存失败", handler.commonExceptionHandler(exception, request("zh-CN")).getMsg());
        assertEquals("Failed to save.", handler.commonExceptionHandler(exception, request("en-US")).getMsg());
        assertEquals("保存失败", handler.commonExceptionHandler(exception, request("fr-FR")).getMsg());
    }

    @Test
    void keepsLegacyMessagesAndStatusCodes() {
        Result<?> result = handler.commonExceptionHandler(new CommonException(403, "原有中文提示"), request("en-US"));
        assertEquals(403, result.getCode());
        assertEquals("原有中文提示", result.getMsg());
        CommonException translated = new CommonException(400, CommonMessage.UPDATE_CONFLICT);
        assertEquals(400, handler.commonExceptionHandler(translated, request("en-US")).getCode());
    }

    @Test
    void supportsArgumentsAndFallsBackToDefinedMessageForMissingKeys() {
        MessageCode limit = new TestMessage("test.limit", "默认最多 {0} 次");
        CommonException exception = new CommonException(limit, 3);
        assertEquals("默认最多 3 次", exception.getMessage());
        assertEquals("At most 3 attempts are allowed.", resolver.resolve(limit, exception.getMessageArguments(), request("en-US")));
        MessageCode missing = new TestMessage("test.missing", "默认最多 {0} 次");
        assertEquals("默认最多 2 次", resolver.resolve(missing, new Object[]{2}, request("en-US")));
    }

    private MockHttpServletRequest request(String language) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (language != null) {
            request.addHeader("Accept-Language", language);
        }
        return request;
    }

    private record TestMessage(String key, String defaultMessage) implements MessageCode {
        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getDefaultMessage() {
            return defaultMessage;
        }

        @Override
        public String getBasename() {
            return "i18n/test-messages";
        }
    }
}
