package io.github.yilers.notice.support;

import static io.github.yilers.notice.support.NoticeSupport.*;

import io.github.yilers.notice.i18n.NoticeMessageCode;
import io.github.yilers.notice.model.*;
import io.github.yilers.notice.model.NoticeTypes.ContentType;
import io.github.yilers.notice.request.MailPreviewRequest;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.regex.*;

/** 仅替换白名单变量，不执行SpEL、脚本或任意模板方法。HTML变量转义，Markdown变量作为文本。 */
@Component
public class MailRenderer {
    private static final Pattern VARIABLE = Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9_]{0,63})}");

    public MailPreview render(MailPreviewRequest request) {
        ContentType type = type(ContentType.class, request.contentType());
        String subject = text(request.subject(), 500, "subject");
        String content = text(request.content(), 600000, "content");
        Map<String, String> params = request.params() == null ? Map.of() : request.params();
        List<TemplateVariable> variables = request.variables() == null ? List.of() : request.variables();
        Set<String> names = new HashSet<>();
        for (TemplateVariable variable : variables) {
            require(
                    variable != null
                            && variable.name() != null
                            && variable.name().matches("[A-Za-z][A-Za-z0-9_]{0,63}")
                            && names.add(variable.name()),
                    NoticeMessageCode.FORMAT_INVALID);
            if (variable.required())
                require(
                        params.get(variable.name()) != null && !params.get(variable.name()).isBlank(),
                        NoticeMessageCode.VARIABLE_MISSING,
                        variable.name());
        }
        subject = replace(subject, names, params, ContentType.TEXT);
        require(!subject.contains("\r") && !subject.contains("\n"), NoticeMessageCode.FORMAT_INVALID);
        content = replace(content, names, params, type);
        if (type == ContentType.MARKDOWN) content = NoticeMarkdownRenderer.render(content);
        return new MailPreview(subject, content, type != ContentType.TEXT);
    }

    private String replace(String input, Set<String> names, Map<String, String> params, ContentType type) {
        Matcher matcher = VARIABLE.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            require(names.contains(name), NoticeMessageCode.VARIABLE_MISSING, name);
            String value = params.getOrDefault(name, "");
            require(value != null && value.length() <= 10000, NoticeMessageCode.FORMAT_INVALID);
            if (type == ContentType.HTML) value = HtmlUtils.htmlEscape(value);
            if (type == ContentType.MARKDOWN) value = value.replaceAll("([\\\\`*_{}\\[\\]()<>#+.!|~-])", "\\\\$1");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        require(!result.toString().contains("${"), NoticeMessageCode.FORMAT_INVALID);
        return result.toString();
    }
}
