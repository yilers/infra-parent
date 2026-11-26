package io.github.yilers.web.field;

import io.github.yilers.api.field.FieldAccess;
import io.github.yilers.api.field.FieldPermissionProvider;
import io.github.yilers.api.field.FieldViewMode;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.i18n.CommonMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;

import java.util.Map;
import java.util.Objects;

/** 字段策略公共入口；单次HTTP请求批量读取一次策略，不修改响应对象或共享缓存。 */
@Component
@RequiredArgsConstructor
public class FieldPermissionService {
    private final ObjectProvider<FieldPermissionProvider> providers;

    /** 只在当前请求内复用，用户或租户发生改变时不会复用之前的策略。 */
    @SuppressWarnings("unchecked")
    public Map<String, FieldAccess> currentAccess() {
        var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        String key = getClass().getName() + ":" + RequestContextHolder.getTenantId() + ":" + RequestContextHolder.getUserId();
        if (attributes != null) {
            Object cached = attributes.getAttribute(key, RequestAttributes.SCOPE_REQUEST);
            if (cached != null) {
                return (Map<String, FieldAccess>) cached;
            }
        }
        var provider = providers.getIfAvailable();
        Map<String, FieldAccess> result = provider == null ? Map.of() : Map.copyOf(provider.currentAccess());
        if (attributes != null) {
            attributes.setAttribute(key, result, RequestAttributes.SCOPE_REQUEST);
        }
        return result;
    }

    public FieldAccess access(String key) {
        return currentAccess().getOrDefault(key, FieldAccess.HIDDEN);
    }

    /** 查询、排序也可能泄漏字段信息；受保护字段仅完整可见时允许作为条件。 */
    public void checkQuery(String key, String label) {
        if (access(key).viewMode() != FieldViewMode.FULL) {
            throw new CommonException(CommonMessage.FIELD_QUERY_FORBIDDEN, label);
        }
    }

    /**
     * 必须由写入业务在数据范围和原记录校验后调用。未提交字段不参与修改。
     * 完整可见的只读字段可原值回传；隐藏/脱敏字段提交即拒绝，避免猜测原值或回写星号。
     */
    public void checkUpdate(String key, String label, boolean submitted, Object before, Object after) {
        if (!submitted) {
            return;
        }
        FieldAccess access = access(key);
        if (access.editable() || access.viewMode() == FieldViewMode.FULL && Objects.equals(before, after)) {
            return;
        }
        throw new CommonException(CommonMessage.FIELD_WRITE_FORBIDDEN, label);
    }
}
