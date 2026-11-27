package io.github.yilers.notice;

import static org.junit.jupiter.api.Assertions.*;

import io.github.yilers.notice.model.TemplateVariable;
import io.github.yilers.notice.request.MailPreviewRequest;
import io.github.yilers.notice.support.MailRenderer;
import io.github.yilers.web.exception.CommonException;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/** 覆盖模板缺参、表达式拒绝、HTML转义和Markdown真实渲染。 */
class MailRendererTest {
    private final MailRenderer renderer = new MailRenderer();
    private final List<TemplateVariable> variables = List.of(new TemplateVariable("name", "姓名", true, "Alex"));

    @Test
    void escapesHtmlAndRejectsMissingVariables() {
        var result =
                renderer.render(
                        new MailPreviewRequest(
                                "Hi ${name}",
                                "HTML",
                                "<p>${name}</p>",
                                variables,
                                Map.of("name", "<img src=x onerror=alert(1)>")));
        assertTrue(result.content().contains("&lt;img"));
        assertFalse(result.content().contains("<img"));
        assertThrows(
                CommonException.class,
                () -> renderer.render(new MailPreviewRequest("Hi", "HTML", "${name}", variables, Map.of())));
        assertThrows(
                CommonException.class,
                () ->
                        renderer.render(
                                new MailPreviewRequest(
                                        "Hi", "HTML", "${T(java.lang.Runtime).getRuntime()}", List.of(), Map.of())));
    }

    @Test
    void rendersMarkdownAndKeepsTextPlain() {
        var markdown =
                renderer.render(
                        new MailPreviewRequest(
                                "Hi",
                                "MARKDOWN",
                                "# Title\n\nHello ${name}",
                                variables,
                                Map.of("name", "<script>alert(1)</script>")));
        assertTrue(markdown.html());
        assertTrue(markdown.content().contains("<h1>Title</h1>"));
        assertFalse(markdown.content().contains("<script>"));
        var text =
                renderer.render(new MailPreviewRequest("Hi", "TEXT", "${name}", variables, Map.of("name", "**Alex**")));
        assertFalse(text.html());
        assertEquals("**Alex**", text.content());
    }

    @Test
    void markdownVariablesCannotInjectImagesOrLinks() {
        var value =
                renderer.render(
                        new MailPreviewRequest(
                                "Hi",
                                "MARKDOWN",
                                "${name}",
                                variables,
                                Map.of("name", "![x](https://example.com/track)")));
        assertFalse(value.content().contains("<img"));
        assertFalse(value.content().contains("<a "));
    }
}
