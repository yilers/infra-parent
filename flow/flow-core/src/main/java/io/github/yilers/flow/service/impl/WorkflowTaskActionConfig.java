package io.github.yilers.flow.service.impl;

import org.dromara.warm.flow.core.FlowEngine;

import java.util.List;
import java.util.Map;

/** 审批节点允许执行的任务操作；未配置时默认关闭，兼容已有流程定义。 */
public record WorkflowTaskActionConfig(boolean transferEnabled, boolean delegationEnabled) {

    public static final String EXT_CODE = "flowTaskActions";
    private static final WorkflowTaskActionConfig DISABLED = new WorkflowTaskActionConfig(false, false);

    /** 从流程实例保存的定义快照中读取节点配置，避免后续编辑定义影响运行中流程。 */
    public static WorkflowTaskActionConfig fromChart(String chartJson, String nodeCode) {
        if (chartJson == null || chartJson.isBlank() || nodeCode == null || nodeCode.isBlank()) return DISABLED;
        try {
            Map<String, Object> chart = FlowEngine.jsonConvert.strToMap(chartJson);
            Object nodes = chart == null ? null : chart.get("nodeList");
            if (!(nodes instanceof List<?> list)) return DISABLED;
            for (Object value : list) {
                if (!(value instanceof Map<?, ?> node) || !nodeCode.equals(node.get("nodeCode"))) continue;
                Object ext = node.get("ext");
                String extJson = ext instanceof String text ? text : FlowEngine.jsonConvert.objToStr(ext);
                return fromExt(extJson);
            }
        } catch (RuntimeException ignored) {
            // 历史实例快照格式异常时按未允许处理，不能意外放开操作。
        }
        return DISABLED;
    }

    /** Warm-Flow 的 ext 使用 code/value 数组，配置缺失或损坏时均按关闭处理。 */
    public static WorkflowTaskActionConfig fromExt(String extJson) {
        if (extJson == null || extJson.isBlank()) return DISABLED;
        try {
            List<Map<String, Object>> entries = FlowEngine.jsonConvert.strToList(extJson);
            if (entries == null) return DISABLED;
            for (Map<String, Object> entry : entries) {
                if (!EXT_CODE.equals(entry.get("code"))) continue;
                Object value = entry.get("value");
                if (!(value instanceof String text)) return DISABLED;
                Map<String, Object> config = FlowEngine.jsonConvert.strToMap(
                        java.net.URLDecoder.decode(text, java.nio.charset.StandardCharsets.UTF_8));
                if (config == null) return DISABLED;
                return new WorkflowTaskActionConfig(Boolean.TRUE.equals(config.get("transferEnabled")),
                        Boolean.TRUE.equals(config.get("delegationEnabled")));
            }
        } catch (RuntimeException ignored) {
            // 非法扩展不会阻断流程详情查询，也不会授予额外操作权限。
        }
        return DISABLED;
    }
}
