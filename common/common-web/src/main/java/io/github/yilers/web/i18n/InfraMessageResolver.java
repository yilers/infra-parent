package io.github.yilers.web.i18n;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按消息所属模块和请求语言解析提示，不替换业务系统自己的 Spring MessageSource。
 */
@Component
public class InfraMessageResolver {

    private final Map<String, ResourceBundleMessageSource> sources = new ConcurrentHashMap<>();

    /** 缺少请求语言或不支持时回退到中文，缺少消息时使用定义中的默认值。 */
    public String resolve(MessageCode message, Object[] arguments, HttpServletRequest request) {
        ResourceBundleMessageSource source = sources.computeIfAbsent(message.getBasename(), this::createSource);
        return source.getMessage(message.getKey(), arguments, message.getDefaultMessage(), resolveLocale(request));
    }

    private ResourceBundleMessageSource createSource(String basename) {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename(basename);
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }

    private Locale resolveLocale(HttpServletRequest request) {
        if (request == null || request.getHeader("Accept-Language") == null) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        Enumeration<Locale> locales = request.getLocales();
        while (locales.hasMoreElements()) {
            String language = locales.nextElement().getLanguage();
            if (Locale.ENGLISH.getLanguage().equals(language)) {
                return Locale.ENGLISH;
            }
            if (Locale.CHINESE.getLanguage().equals(language)) {
                return Locale.SIMPLIFIED_CHINESE;
            }
        }
        return Locale.SIMPLIFIED_CHINESE;
    }
}
