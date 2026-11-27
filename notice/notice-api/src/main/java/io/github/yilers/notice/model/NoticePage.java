package io.github.yilers.notice.model;

import java.util.List;

/** 分页结果，不向API调用方暴露MyBatis实现。 */
public record NoticePage<T>(long total, List<T> records) {}
