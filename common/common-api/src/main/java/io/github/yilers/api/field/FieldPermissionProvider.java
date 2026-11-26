package io.github.yilers.api.field;

import java.util.Map;

/**
 * 从可信登录态获取当前调用人的字段策略；key格式为resource:field。
 * UPM提供本地实现，独立业务服务可通过远程实现接入；公共Web层不依赖UPM实现。
 */
public interface FieldPermissionProvider {
    Map<String, FieldAccess> currentAccess();
}
