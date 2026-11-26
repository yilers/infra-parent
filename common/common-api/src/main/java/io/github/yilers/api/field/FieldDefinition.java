package io.github.yilers.api.field;

import java.util.Arrays;
import java.util.List;

/** 由代码提供的字段目录元数据；管理端只登记这些声明，不接收任意字段名。 */
public record FieldDefinition(String resource, String resourceName, String field,
                              String name, FieldMaskType mask, boolean writeSupported) {
    public String key() { return resource + ":" + field; }

    public static FieldDefinition from(FieldPermission annotation) {
        return new FieldDefinition(annotation.resource(), annotation.resourceName(),
                annotation.field(), annotation.name(), annotation.mask(), annotation.writeSupported());
    }

    public static List<FieldDefinition> fromResponse(Class<?> type) {
        var methods = Arrays.stream(type.getMethods()).map(m -> m.getAnnotation(FieldPermission.class));
        var fields = Arrays.stream(type.getDeclaredFields()).map(f -> f.getAnnotation(FieldPermission.class));
        return java.util.stream.Stream.concat(methods, fields).filter(java.util.Objects::nonNull)
                .map(FieldDefinition::from).distinct().toList();
    }
}
