package io.github.yilers.shortlink.mapper;

import io.github.yilers.shortlink.entity.ShortlinkNamespace;
import io.github.yilers.web.mybatis.CustomMapper;

/** 短链路径配置数据访问。默认保留租户过滤。 */
public interface ShortlinkNamespaceMapper extends CustomMapper<ShortlinkNamespace> {
}
