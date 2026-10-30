package io.github.yilers.flow.service.impl;

import io.github.yilers.flow.model.WorkflowAssigneeRule;
import io.github.yilers.flow.i18n.FlowMessage;
import io.github.yilers.web.exception.CommonException;
import org.dromara.warm.flow.core.FlowEngine;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 节点扩展字段中的自动抄送配置。收件人标识在节点通过时解析为有效账号。 */
public record WorkflowCcConfig(boolean enabled, boolean includeChildDepartments, List<String> recipients) {

    public static final String EXT_CODE = "flowCc";
    private static final int MAX_RECIPIENTS = 100;

    /** Warm-Flow 的 ext 是 code/value 数组；value 使用 URI 编码，避免设计器按逗号拆分。 */
    public static WorkflowCcConfig fromExt(String ext) {
        if (ext == null || ext.isBlank()) return new WorkflowCcConfig(false, false, List.of());
        try {
            List<Map<String, Object>> entries = FlowEngine.jsonConvert.strToList(ext);
            if (entries == null) return new WorkflowCcConfig(false, false, List.of());
            for (Map<String, Object> entry : entries) {
                if (!EXT_CODE.equals(entry.get("code"))) continue;
                Object raw = entry.get("value");
                if (!(raw instanceof String encoded)) throw new IllegalArgumentException("抄送配置不是字符串");
                Map<String, Object> data = FlowEngine.jsonConvert.strToMap(
                        URLDecoder.decode(encoded, StandardCharsets.UTF_8));
                if (data == null || !(data.get("enabled") instanceof Boolean enabled)
                        || !(data.get("includeChildDepartments") instanceof Boolean includeChildren)
                        || !(data.get("recipients") instanceof List<?> rows)) {
                    throw new IllegalArgumentException("抄送配置缺少必要字段");
                }
                List<String> recipients = new ArrayList<>();
                for (Object row : rows) {
                    if (!(row instanceof Map<?, ?> recipient) || !(recipient.get("storageId") instanceof String flag)) {
                        throw new IllegalArgumentException("抄送对象格式不正确");
                    }
                    recipients.add(flag);
                }
                WorkflowCcConfig config = new WorkflowCcConfig(enabled, includeChildren, List.copyOf(recipients));
                config.validate();
                return config;
            }
            return new WorkflowCcConfig(false, false, List.of());
        } catch (CommonException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CommonException(FlowMessage.CC_CONFIG_INVALID, e.getMessage());
        }
    }

    private void validate() {
        if (!enabled) return;
        if (recipients.isEmpty() || recipients.size() > MAX_RECIPIENTS) {
            throw new CommonException(FlowMessage.CC_RECIPIENT_COUNT);
        }
        Set<String> unique = new LinkedHashSet<>(recipients);
        if (unique.size() != recipients.size()) throw new CommonException(FlowMessage.CC_RECIPIENT_DUPLICATE);
        for (String flag : recipients) {
            if (flag == null || (!flag.matches("(?:user|role|dept):[1-9][0-9]{0,18}")
                    && WorkflowAssigneeRule.fromExpression(flag) == null)) {
                throw new CommonException(FlowMessage.CC_RECIPIENT_UNSUPPORTED, flag);
            }
            if (!flag.startsWith("${")) {
                try {
                    Long.parseLong(flag.substring(flag.indexOf(':') + 1));
                } catch (NumberFormatException e) {
                    throw new CommonException(FlowMessage.CC_RECIPIENT_ID_RANGE, flag);
                }
            }
        }
    }
}
