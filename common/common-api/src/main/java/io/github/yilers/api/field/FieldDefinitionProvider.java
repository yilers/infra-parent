package io.github.yilers.api.field;

import java.util.List;

/** 业务模块通过Bean注册受保护响应类型的字段目录，避免扫描全部类路径或持久化实体。 */
public interface FieldDefinitionProvider {
    List<FieldDefinition> definitions();
}
