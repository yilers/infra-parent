package io.github.yilers.upm.log;

import cn.hutool.v7.json.JSONUtil;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogParameterSanitizerTest {
    @Test
    void masksNestedObjectsAndArraysWithoutChangingOriginalArguments() throws Exception {
        Map<String, Object> body = Map.of("items", List.of(Map.of("password", "body-secret", "name", "张三")));
        var method = Endpoints.class.getDeclaredMethod("mixed", Map.class, String.class, int.class);
        String result = LogParameterSanitizer.sanitize(method,
                new Object[]{body, "query-secret", 10}, new String[]{"password"});
        assertFalse(result.contains("body-secret"));
        assertFalse(result.contains("query-secret"));
        var array = JSONUtil.parseArray(result);
        assertEquals("***", array.getStr(1));
        assertEquals(10, array.getInt(2));
        assertTrue(result.contains("张三"));
        assertEquals("body-secret", ((Map<?, ?>) ((List<?>) body.get("items")).get(0)).get("password"));
    }

    @Test
    void supportsRequestParamValueAndNullArguments() throws Exception {
        var method = Endpoints.class.getDeclaredMethod("query", String.class, String.class);
        String result = LogParameterSanitizer.sanitize(method,
                new Object[]{"sensitive", null}, new String[]{"token"});
        assertFalse(result.contains("sensitive"));
        assertEquals("***", JSONUtil.parseArray(result).getStr(0));
    }

    @Test
    void supportsRetainedJavaParameterNames() throws Exception {
        var method = Endpoints.class.getDeclaredMethod("unnamed", String.class);
        assertTrue(method.getParameters()[0].isNamePresent());
        String result = LogParameterSanitizer.sanitize(method, new Object[]{"sensitive"}, new String[]{"password"});
        assertEquals("***", JSONUtil.parseArray(result).getStr(0));
    }

    private static class Endpoints {
        void mixed(@RequestBody Map<String, Object> body, @RequestParam(name = "password") String value, int page) { }
        void query(@RequestParam("token") String value, String optional) { }
        void unnamed(@RequestParam String password) { }
    }
}
