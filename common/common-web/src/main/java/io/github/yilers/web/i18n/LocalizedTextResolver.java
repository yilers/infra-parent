package io.github.yilers.web.i18n;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Map;

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

    /** 只处理标准校验模板，项目自定义消息仍使用原有兼容目录。 */
    public String resolveValidation(FieldError error, HttpServletRequest request) {
        try {
            ConstraintViolation<?> violation = error.unwrap(ConstraintViolation.class);
            String template = violation.getMessageTemplate();
            Map<String, Object> attributes = violation.getConstraintDescriptor().getAttributes();
            return switch (template) {
                case "{jakarta.validation.constraints.NotNull.message}" -> resolveStandard(CommonMessage.VALIDATION_NOT_NULL, request);
                case "{jakarta.validation.constraints.NotBlank.message}" -> resolveStandard(CommonMessage.VALIDATION_NOT_BLANK, request);
                case "{jakarta.validation.constraints.NotEmpty.message}" -> resolveStandard(CommonMessage.VALIDATION_NOT_EMPTY, request);
                case "{jakarta.validation.constraints.Size.message}" -> resolveStandard(CommonMessage.VALIDATION_SIZE, request,
                        String.valueOf(attributes.get("min")), String.valueOf(attributes.get("max")));
                case "{jakarta.validation.constraints.Min.message}" -> resolveStandard(CommonMessage.VALIDATION_MIN, request,
                        String.valueOf(attributes.get("value")));
                case "{jakarta.validation.constraints.Max.message}" -> resolveStandard(CommonMessage.VALIDATION_MAX, request,
                        String.valueOf(attributes.get("value")));
                case "{jakarta.validation.constraints.Positive.message}" -> resolveStandard(CommonMessage.VALIDATION_POSITIVE, request);
                case "{jakarta.validation.constraints.Pattern.message}" -> resolveStandard(CommonMessage.VALIDATION_PATTERN, request,
                        String.valueOf(attributes.get("regexp")));
                default -> resolve(error.getDefaultMessage(), request);
            };
        } catch (IllegalArgumentException e) {
            // 非 Bean Validation 的 FieldError 没有包装 ConstraintViolation。
            return resolve(error.getDefaultMessage(), request);
        }
    }

    private String resolveStandard(MessageCode message, HttpServletRequest request, Object... arguments) {
        return resolver.resolve(message, arguments, request);
    }
}
