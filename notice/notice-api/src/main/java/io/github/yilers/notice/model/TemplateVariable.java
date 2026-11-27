package io.github.yilers.notice.model;

/** 模板变量定义，名称只允许字母、数字和下划线，不允许执行表达式。 */
public record TemplateVariable(String name, String description, boolean required, String example) {}
