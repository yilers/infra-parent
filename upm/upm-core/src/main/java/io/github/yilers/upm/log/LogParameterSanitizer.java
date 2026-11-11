package io.github.yilers.upm.log;

import cn.hutool.v7.json.JSONArray;
import cn.hutool.v7.json.JSONObject;
import cn.hutool.v7.json.JSONUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** 在序列化副本上递归脱敏，避免修改 Controller 实际收到的业务参数。 */
final class LogParameterSanitizer {
    private static final String MASK = "***";

    private LogParameterSanitizer() {
    }

    static String sanitize(Method method, Object[] arguments, String[] hiddenFields) {
        Set<String> hidden = new HashSet<>(Arrays.asList(hiddenFields));
        Parameter[] parameters = method.getParameters();
        JSONArray result = new JSONArray();
        for (int i = 0; i < arguments.length; i++) {
            Parameter parameter = parameters[i];
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            String name = requestParam == null ? "" : requestParam.name();
            if (name.isEmpty() && requestParam != null) {
                name = requestParam.value();
            }
            Object argument = arguments[i];
            if (hidden.contains(name) || parameter.isNamePresent() && hidden.contains(parameter.getName())) {
                result.addValue(MASK);
            } else if (argument instanceof ServletRequest || argument instanceof ServletResponse
                    || argument instanceof MultipartFile || argument instanceof MultipartFile[]
                    || argument instanceof InputStream || argument instanceof OutputStream) {
                result.addValue("[不记录此类型参数]");
            } else {
                // 包裹成数组后统一解析，兼容字符串、数字、null 及复杂对象。
                JSONArray copy = JSONUtil.parseArray(JSONUtil.toJsonStr(new Object[]{argument}));
                Object value = copy.get(0);
                hide(value, hidden);
                result.addValue(value);
            }
        }
        return result.toString();
    }

    private static void hide(Object value, Set<String> hidden) {
        if (value instanceof JSONObject object) {
            for (String key : object.keySet()) {
                if (hidden.contains(key)) {
                    object.putValue(key, MASK);
                } else {
                    hide(object.get(key), hidden);
                }
            }
        } else if (value instanceof JSONArray array) {
            for (int i = 0; i < array.size(); i++) {
                hide(array.get(i), hidden);
            }
        }
    }
}
