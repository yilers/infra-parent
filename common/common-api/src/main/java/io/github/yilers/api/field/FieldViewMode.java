package io.github.yilers.api.field;

/** 字段查看级别；角色授权按此顺序取并集，不使用枚举序号入库。 */
public enum FieldViewMode {
    HIDDEN, MASKED, FULL;

    public FieldViewMode union(FieldViewMode other) {
        if (this == FULL || other == FULL) return FULL;
        if (this == MASKED || other == MASKED) return MASKED;
        return HIDDEN;
    }
}
