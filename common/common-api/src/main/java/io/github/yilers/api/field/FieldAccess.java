package io.github.yilers.api.field;

/** 当前调用人的最终字段策略；允许写入必须同时拥有完整查看权限。 */
public record FieldAccess(FieldViewMode viewMode, boolean editable) {
    public static final FieldAccess HIDDEN = new FieldAccess(FieldViewMode.HIDDEN, false);
    public static final FieldAccess FULL = new FieldAccess(FieldViewMode.FULL, true);

    public FieldAccess {
        if (viewMode == null) throw new IllegalArgumentException("字段查看方式不能为空");
        editable = editable && viewMode == FieldViewMode.FULL;
    }

    public FieldAccess union(FieldAccess other) {
        return new FieldAccess(viewMode.union(other.viewMode), editable || other.editable);
    }
}
