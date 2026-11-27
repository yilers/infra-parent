package io.github.yilers.notice.support;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/** 通知与邮件共用的 Markdown 渲染器，转义原始 HTML 并过滤不安全的链接协议。 */
public final class NoticeMarkdownRenderer {
    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER =
            HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true).build();

    private NoticeMarkdownRenderer() {}

    public static String render(String source) {
        return RENDERER.render(PARSER.parse(source));
    }
}
