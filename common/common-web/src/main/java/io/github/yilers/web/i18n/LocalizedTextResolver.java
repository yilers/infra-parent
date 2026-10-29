package io.github.yilers.web.i18n;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/** 翻译已登记的文本，未登记时保留原文，兼容既有调用方。 */
@Component
public class LocalizedTextResolver {

    private final InfraMessageResolver resolver;
    private final List<MessageTextCatalog> catalogs;

    public LocalizedTextResolver(InfraMessageResolver resolver, List<MessageTextCatalog> catalogs) {
        this.resolver = resolver;
        this.catalogs = List.copyOf(catalogs);
    }

    public String resolve(String text, HttpServletRequest request) {
        if (text == null) {
            return null;
        }
        for (MessageTextCatalog catalog : catalogs) {
            MessageCode message = catalog.messages().get(text);
            if (message != null) {
                return resolver.resolve(message, new Object[0], request);
            }
        }
        return text;
    }
}
