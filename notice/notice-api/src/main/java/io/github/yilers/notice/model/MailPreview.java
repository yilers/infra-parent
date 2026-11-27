package io.github.yilers.notice.model;

/** 渲染后的预览，HTML在前端隔离iframe中展示。 */
public record MailPreview(String subject, String content, boolean html) {}
