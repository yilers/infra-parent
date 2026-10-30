package io.github.yilers.web.i18n;

import io.github.yilers.core.util.Result;
import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfraMessageResolverTest {

    private final InfraMessageResolver resolver = new InfraMessageResolver();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(resolver,
            new LocalizedTextResolver(resolver, java.util.List.of()));

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
        assertEquals("模板必须包含{code}", new CommonException(new TestMessage("test.literal", "模板必须包含{code}")).getMessage());
    }

    @Test
    void translatesRegisteredValidationMessagesAndKeepsUnknownMessages() throws Exception {
        MessageCode code = new TestMessage("test.validation", "账号不能为空");
        MessageTextCatalog catalog = () -> Map.of("账号不能为空", code);
        GlobalExceptionHandler advice = new GlobalExceptionHandler(resolver,
                new LocalizedTextResolver(resolver, List.of(catalog)));
        BindException error = new BindException(new Object(), "request");
        error.addError(new FieldError("request", "account", "账号不能为空"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertEquals("Account is required.", advice.bindExceptionHandler(response, request("en-US"), error).getMsg());
        assertEquals(400, response.getStatus());
        MethodParameter parameter = new MethodParameter(getClass().getDeclaredMethod("validationArgument", String.class), 0);
        MethodArgumentNotValidException argumentError = new MethodArgumentNotValidException(parameter, error.getBindingResult());
        Result<?> result = (Result<?>) advice.methodArgumentNotValidException(new MockHttpServletResponse(),
                request("en-US"), argumentError);
        assertEquals("Account is required.", result.getMsg());
        assertEquals("账号不能为空", ((Result<?>) advice.methodArgumentNotValidException(new MockHttpServletResponse(),
                request(null), argumentError)).getMsg());
        assertEquals("ID is required.", new LocalizedTextResolver(resolver, List.of(new CommonMessageTextCatalog()))
                .resolve("id不能为空", request("en-US")));
        assertEquals("未知校验", new LocalizedTextResolver(resolver, List.of(catalog))
                .resolve("未知校验", request("en-US")));
        assertEquals("Account is required.", advice.exceptionHandler(new IllegalArgumentException("账号不能为空"),
                request("en-US")).getMsg());
        assertEquals("账号不能为空", advice.exceptionHandler(new IllegalStateException("账号不能为空"),
                request("en-US")).getMsg());
    }

    private void validationArgument(String value) {
        // 提供方法参数供 Spring 构造校验异常。
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
