package io.github.yilers.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Enumeration;
import java.util.Locale;

/** 系统调用鉴权的公开提示；不依赖 common-web，也不泄露验签细节。 */
final class ApiAuthMessages {

    enum Code {
        PERMISSION_UNDECLARED("api.permission-undeclared", "接口未声明系统调用权限"),
        REQUEST_INVALID("api.request-invalid", "签名入口仅支持无查询串的POST JSON请求"),
        PERMISSION_DENIED("api.permission-denied", "调用凭证未获得接口权限"),
        RATE_LIMITED("api.rate-limited", "调用频率超过限制"),
        AUTH_FAILED("api.auth-failed", "系统调用认证失败");

        private final String key;
        private final String fallback;

        Code(String key, String fallback) {
            this.key = key;
            this.fallback = fallback;
        }
    }

    private static final ResourceBundleMessageSource SOURCE = createSource();

    private ApiAuthMessages() {
    }

    static String resolve(Code code, HttpServletRequest request) {
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        if (request.getHeader("Accept-Language") != null) {
            Enumeration<Locale> locales = request.getLocales();
            while (locales.hasMoreElements()) {
                String language = locales.nextElement().getLanguage();
                if (Locale.ENGLISH.getLanguage().equals(language)) {
                    locale = Locale.ENGLISH;
                    break;
                }
                if (Locale.CHINESE.getLanguage().equals(language)) {
                    break;
                }
            }
        }
        return SOURCE.getMessage(code.key, null, code.fallback, locale);
    }

    private static ResourceBundleMessageSource createSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/auth-messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
