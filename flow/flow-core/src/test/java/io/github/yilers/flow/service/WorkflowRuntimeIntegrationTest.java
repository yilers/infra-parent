package io.github.yilers.flow.service;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import io.github.yilers.flow.config.FlowPermissionConfig;
import io.github.yilers.flow.config.FlowQueryConfig;
import io.github.yilers.flow.config.FlowTenantConfig;
import io.github.yilers.flow.config.FlowCcListener;
import io.github.yilers.flow.mapper.WorkflowCcMapper;
import io.github.yilers.flow.model.WorkflowWorkItem;
import io.github.yilers.flow.service.impl.WorkflowDefinitionServiceImpl;
import io.github.yilers.flow.service.impl.WorkflowInboxServiceImpl;
import io.github.yilers.flow.service.impl.WorkflowServiceImpl;
import io.github.yilers.flow.service.impl.WorkflowTaskActionConfig;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.enums.PublishStatus;
import org.dromara.warm.flow.core.enums.FlowStatus;
import org.apache.ibatis.session.SqlSessionFactory;
import org.dromara.warm.flow.spring.boot.config.FlowAutoConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 不依赖开发数据库和Redis，用真实引擎验证发布、发起、或签、退回与事务回滚。 */
@SpringJUnitConfig(WorkflowRuntimeIntegrationTest.Config.class)
@TestPropertySource(properties = "flow.leave.enabled=true")
class WorkflowRuntimeIntegrationTest {

    static final ThreadLocal<String> USER = new ThreadLocal<>();

    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private WorkflowDefinitionService definitionService;
    @Autowired
    private WorkflowInboxService inboxService;
    @Autowired
    private WorkflowCcMapper ccMapper;

    @BeforeEach
    void login() {
        login("10");
    }

    @AfterEach
    void logout() {
        USER.remove();
        RequestContextHolder.clear();
    }

    @Test
    void expiredDefinitionIsNotAnEditableDraftAndOldInstancesKeepRunning() {
        String code = publish("definition_status_review", "user:20");
        Long originalId = definitionService.findPublishedId(code);
        Long instanceId = workflowService.start(code, "status-review", Map.of());
        CommonException used = assertThrows(CommonException.class,
                () -> definitionService.update(originalId, editableDesign(originalId)));
        assertTrue(used.getMessage().contains("已有流程实例"));
        definitionService.copy(originalId);
        Long copiedId = definitionService.versions(code).stream()
                .filter(version -> !originalId.equals(version.id())).findFirst().orElseThrow().id();
        definitionService.publish(copiedId);
        assertEquals(PublishStatus.EXPIRED.getKey(), definitionService.get(originalId).publishStatus());
        CommonException expired = assertThrows(CommonException.class,
                () -> definitionService.update(originalId, editableDesign(originalId)));
        assertTrue(expired.getMessage().contains("已失效"));
        assertThrows(CommonException.class, () -> definitionService.delete(originalId));
        assertThrows(CommonException.class,
                () -> definitionService.update(copiedId, editableDesign(copiedId)));
        definitionService.unpublish(copiedId);
        definitionService.update(copiedId, editableDesign(copiedId));

        login("20");
        workflowService.pass(inboxService.todo(code, 1, 20).records().getFirst().getTaskId(), "同意", Map.of());
        assertEquals(instanceId, inboxService.done(code, 1, 20).records().getFirst().getInstanceId());
    }

    @Test
    void dynamicRulesPersistApplicantSnapshotAcrossApprovalAndReturn() {
        String code = "dynamic_rules";
        String json = """
                {"flowCode":"dynamic_rules","flowName":"动态办理人测试","version":"1","modelValue":"CLASSICS",
                 "nodeList":[
                  {"nodeCode":"start","nodeName":"开始","nodeType":0,"nodeRatio":"0","coordinate":"100,100",
                   "skipList":[{"nowNodeCode":"start","nextNodeCode":"apply","skipType":"PASS"}]},
                  {"nodeCode":"apply","nodeName":"申请人提交","nodeType":1,"permissionFlag":"${wfApplicant}",
                   "nodeRatio":"0","coordinate":"300,100",
                   "skipList":[{"nowNodeCode":"apply","nextNodeCode":"review","skipType":"PASS"}]},
                  {"nodeCode":"review","nodeName":"本部门审核","nodeType":1,"permissionFlag":"${wfDepartmentApprovers}",
                   "nodeRatio":"0","coordinate":"500,100",
                   "skipList":[{"nowNodeCode":"review","nextNodeCode":"parent","skipType":"PASS"},
                               {"nowNodeCode":"review","nextNodeCode":"apply","skipType":"REJECT"}]},
                  {"nodeCode":"parent","nodeName":"上级主管审核","nodeType":1,"permissionFlag":"${wfParentDepartmentApprovers}",
                   "nodeRatio":"0","coordinate":"700,100",
                   "skipList":[{"nowNodeCode":"parent","nextNodeCode":"end","skipType":"PASS"}]},
                  {"nodeCode":"end","nodeName":"结束","nodeType":2,"nodeRatio":"0","coordinate":"900,100","skipList":[]}]}
                """;
        Long definitionId = definitionService.create(json);
        definitionService.publish(definitionId);
        assertThrows(CommonException.class, () -> workflowService.start(code, "forged", Map.of("wfApplicant", List.of("99"))));
        login("99");
        assertThrows(CommonException.class, () -> workflowService.start(code, "missing-assignee", Map.of()));
        assertEquals(0, inboxService.started(code, 1, 20).total());
        login("10");
        Long instanceId = workflowService.start(code, "dynamic-1", Map.of());
        var apply = inboxService.todo(code, 1, 20).records().getFirst();
        assertEquals("申请人提交", apply.getNodeName());
        workflowService.pass(apply.getTaskId(), "提交", Map.of());
        login("20");
        var review = inboxService.todo(code, 1, 20).records().getFirst();
        assertThrows(CommonException.class, () -> workflowService.pass(review.getTaskId(), "伪造", Map.of("wfParentDepartmentApprovers", List.of("99"))));
        workflowService.reject(review.getTaskId(), "请补充", Map.of());
        login("10");
        workflowService.pass(inboxService.todo(code, 1, 20).records().getFirst().getTaskId(), "再次提交", Map.of());
        login("20");
        workflowService.pass(inboxService.todo(code, 1, 20).records().getFirst().getTaskId(), "同意", Map.of());
        assertEquals(0, inboxService.todo(code, 1, 20).total());
        login("30");
        assertEquals(0, inboxService.todo(code, 1, 20).total());
        login("40");
        var parent = inboxService.todo(code, 1, 20).records().getFirst();
        assertEquals("上级主管审核", parent.getNodeName());
        assertEquals(instanceId, parent.getInstanceId());
        workflowService.pass(parent.getTaskId(), "同意", Map.of());
        assertEquals("8", inboxService.detail(instanceId).instance().getFlowStatus());
    }

    @Test
    void applicantCanTakeBackToTheirLatestCompletedTaskAndOnlyTheyCanRevoke() {
        String code = "applicant_take_back";
        Long definitionId = definitionService.create("""
                {"flowCode":"applicant_take_back","flowName":"申请人拿回测试","version":"1","modelValue":"CLASSICS",
                 "nodeList":[
                  {"nodeCode":"start","nodeName":"开始","nodeType":0,"nodeRatio":"0","coordinate":"100,100",
                   "skipList":[{"nowNodeCode":"start","nextNodeCode":"apply","skipType":"PASS"}]},
                  {"nodeCode":"apply","nodeName":"申请提交","nodeType":1,"permissionFlag":"${wfApplicant}",
                   "nodeRatio":"0","coordinate":"300,100",
                   "skipList":[{"nowNodeCode":"apply","nextNodeCode":"review","skipType":"PASS"}]},
                  {"nodeCode":"review","nodeName":"审批","nodeType":1,"permissionFlag":"user:20",
                   "nodeRatio":"0","coordinate":"500,100",
                   "skipList":[{"nowNodeCode":"review","nextNodeCode":"end","skipType":"PASS"}]},
                  {"nodeCode":"end","nodeName":"结束","nodeType":2,"nodeRatio":"0","coordinate":"700,100","skipList":[]}]}""");
        definitionService.publish(definitionId);

        Long instanceId = workflowService.start(code, "take-back-1", Map.of());
        var submitTask = inboxService.todo(code, 1, 20).records().getFirst();
        workflowService.pass(submitTask.getTaskId(), "提交申请", Map.of());
        assertEquals(1, inboxService.started(code, 1, 20).records().getFirst().isCanTakeBack() ? 1 : 0);

        login("99");
        assertThrows(CommonException.class, () -> workflowService.takeBack(instanceId));
        assertThrows(CommonException.class, () -> workflowService.revoke(instanceId));

        login("10");
        assertEquals(instanceId, workflowService.takeBack(instanceId));
        var returned = inboxService.detail(instanceId);
        assertEquals(FlowStatus.TASK_BACK.getKey(), returned.instance().getFlowStatus());
        assertEquals("apply", returned.tasks().getFirst().getNodeCode());
        assertTrue(returned.tasks().getFirst().isActionable());
        assertFalse(inboxService.started(code, 1, 20).records().getFirst().isCanTakeBack());

        assertEquals(instanceId, workflowService.revoke(instanceId));
        assertEquals(FlowStatus.CANCEL.getKey(), inboxService.detail(instanceId).instance().getFlowStatus());
        assertFalse(inboxService.detail(instanceId).tasks().getFirst().isActionable());
    }

    @Test
    void transferMovesTaskToTargetAndDelegateReturnsTaskToDelegator() {
        String transferCode = publish("task_transfer", "user:20", false, "0", taskActionsExt(true, false));
        Long transferInstance = workflowService.start(transferCode, "transfer-1", Map.of());
        login("20");
        Long transferTaskId = inboxService.todo(transferCode, 1, 20).records().getFirst().getTaskId();
        WorkflowWorkItem transferItem = inboxService.detail(transferInstance).tasks().getFirst();
        assertTrue(transferItem.isCanTransfer());
        assertFalse(transferItem.isCanDelegate());
        String transferChart = inboxService.detail(transferInstance).chartJson();
        assertTrue(transferChart.contains("flowTaskActionsDisplay"));
        assertTrue(transferChart.contains("\"transferEnabled\":true"));
        assertTrue(transferChart.contains("\"delegationEnabled\":false"));
        assertEquals(transferInstance, workflowService.transfer(transferTaskId, 30L, "请代为办理"));
        assertEquals(0, inboxService.todo(transferCode, 1, 20).total());
        login("30");
        assertEquals(1, inboxService.todo(transferCode, 1, 20).total());
        workflowService.pass(inboxService.todo(transferCode, 1, 20).records().getFirst().getTaskId(), "已办理", Map.of());
        assertEquals(0, inboxService.todo(transferCode, 1, 20).total());

        String delegateCode = publish("task_delegate", "user:20", false, "0", taskActionsExt(false, true));
        Long delegateInstance = workflowService.start(delegateCode, "delegate-1", Map.of());
        login("20");
        Long delegateTaskId = inboxService.todo(delegateCode, 1, 20).records().getFirst().getTaskId();
        WorkflowWorkItem delegateItem = inboxService.detail(delegateInstance).tasks().getFirst();
        assertFalse(delegateItem.isCanTransfer());
        assertTrue(delegateItem.isCanDelegate());
        String delegateChart = inboxService.detail(delegateInstance).chartJson();
        assertTrue(delegateChart.contains("\"transferEnabled\":false"));
        assertTrue(delegateChart.contains("\"delegationEnabled\":true"));
        assertEquals(delegateInstance, workflowService.delegate(delegateTaskId, 30L, "请先审核"));
        assertEquals(0, inboxService.todo(delegateCode, 1, 20).total());
        login("30");
        workflowService.pass(inboxService.todo(delegateCode, 1, 20).records().getFirst().getTaskId(), "委派处理中", Map.of());
        assertEquals(0, inboxService.todo(delegateCode, 1, 20).total());
        login("20");
        assertEquals(1, inboxService.todo(delegateCode, 1, 20).total());
        workflowService.pass(inboxService.todo(delegateCode, 1, 20).records().getFirst().getTaskId(), "最终确认", Map.of());
        assertEquals("8", inboxService.detail(delegateInstance).instance().getFlowStatus());
    }

    @Test
    void taskAssignmentIsDeniedUnlessEnabledOnTheApprovalNode() {
        String code = publish("task_actions_disabled", "user:20");
        workflowService.start(code, "task-actions-disabled", Map.of());
        login("20");
        Long taskId = inboxService.todo(code, 1, 20).records().getFirst().getTaskId();

        assertThrows(CommonException.class, () -> workflowService.transfer(taskId, 30L, null));
        assertThrows(CommonException.class, () -> workflowService.delegate(taskId, 30L, null));
        WorkflowWorkItem item = inboxService.detail(inboxService.todo(code, 1, 20).records().getFirst().getInstanceId())
                .tasks().getFirst();
        assertFalse(item.isCanTransfer());
        assertFalse(item.isCanDelegate());
    }

    @Test
    void completeOrSignAndKeepBusinessDataPrivate() {
        String code = publish("runtime_or", "user:20@@user:30");
        Long instanceId = workflowService.start(code, "leave-1", Map.of("secret", "不可返回浏览器"));
        var mine = inboxService.detail(instanceId);
        assertEquals("1", mine.instance().getFlowStatus());
        assertFalse(mine.chartJson().contains("listenerPath"));
        assertFalse(mine.chartJson().contains("permissionFlag"));
        assertTrue(mine.chartJson().contains("审批人甲、审批人乙"));
        assertTrue(mine.chartJson().contains("permissionLabel"));
        assertFalse(mine.chartJson().contains("不可返回浏览器"));

        login("99");
        assertThrows(CommonException.class, () -> inboxService.detail(instanceId));
        login("20");
        assertTrue(inboxService.detail(instanceId).chartJson().contains("审批人甲、审批人乙"));
        var task = inboxService.todo(code, 1, 20).records().getFirst();
        assertEquals(instanceId, task.getInstanceId());
        login("30");
        assertEquals(1, inboxService.todo(code, 1, 20).total());
        login("20");
        workflowService.pass(task.getTaskId(), "同意", Map.of());
        assertEquals("8", inboxService.detail(instanceId).instance().getFlowStatus());
        assertEquals(1, inboxService.done(code, 1, 20).total());
        assertThrows(CommonException.class, () -> workflowService.pass(task.getTaskId(), "重复提交", Map.of()));
        login("30");
        assertEquals(0, inboxService.todo(code, 1, 20).total());
    }

    @Test
    void rejectsToConfiguredNodeAndRollsBackUnassignedTasks() {
        String code = publish("runtime_reject", "user:20", true);
        Long instanceId = workflowService.start(code, "leave-2", Map.of());
        workflowService.pass(inboxService.todo(code, 1, 20).records().getFirst().getTaskId(), "提交申请", Map.of());
        login("20");
        var task = inboxService.todo(code, 1, 20).records().getFirst();
        workflowService.reject(task.getTaskId(), "请补充说明", Map.of());
        assertEquals("9", inboxService.detail(instanceId).instance().getFlowStatus());
        assertEquals("REJECT", inboxService.done(code, 1, 20).records().getFirst().getSkipType());

        login("10");
        String emptyCode = publish("runtime_empty", "");
        assertThrows(CommonException.class, () -> workflowService.start(emptyCode, "leave-empty", Map.of()));
        assertEquals(0, inboxService.started(emptyCode, 1, 20).total());
    }

    @Test
    void concurrentApproversOnlyAdvanceAnOrSignTaskOnce() throws Exception {
        String code = publish("runtime_concurrent", "user:20@@user:30");
        workflowService.start(code, "leave-3", Map.of());
        login("20");
        Long taskId = inboxService.todo(code, 1, 20).records().getFirst().getTaskId();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(
                    () -> approveAs("20", taskId), () -> approveAs("30", taskId)));
            assertEquals(1, results.stream().filter(result -> {
                try { return Boolean.TRUE.equals(result.get()); }
                catch (Exception e) { throw new AssertionError(e); }
            }).count());
        }
    }

    private boolean approveAs(String userId, Long taskId) {
        try {
            login(userId);
            workflowService.pass(taskId, "同意", Map.of());
            return true;
        } catch (CommonException expected) {
            return false;
        } finally {
            logout();
        }
    }

    @Test
    void countersignUsesConcreteUserIdsAndCannotVoteTwice() {
        String code = publish("runtime_countersign", "user:20@@user:30", false, "100");
        Long instanceId = workflowService.start(code, "leave-sign", Map.of());
        login("20");
        Long taskId = inboxService.todo(code, 1, 20).records().getFirst().getTaskId();
        workflowService.pass(taskId, "同意", Map.of());
        assertEquals("1", inboxService.detail(instanceId).instance().getFlowStatus());
        assertEquals(0, inboxService.todo(code, 1, 20).total());
        assertThrows(CommonException.class, () -> workflowService.pass(taskId, "重复投票", Map.of()));
        login("30");
        workflowService.pass(taskId, "同意", Map.of());
        assertEquals("8", inboxService.detail(instanceId).instance().getFlowStatus());
    }

    @Test
    void ccIsDeliveredOnlyWhenNodePassesAndOnlyRecipientCanReadIt() {
        String code = "runtime_cc";
        String encoded = URLEncoder.encode("""
                {"enabled":true,"includeChildDepartments":false,"recipients":[
                  {"storageId":"user:40","handlerName":"知会人"},
                  {"storageId":"${wfApplicant}","handlerName":"申请人"}]}
                """, StandardCharsets.UTF_8);
        String ext = FlowEngine.jsonConvert.objToStr(List.of(Map.of("code", "flowCc", "value", encoded)));
        String json = """
                {"flowCode":"runtime_cc","flowName":"抄送测试","version":"1","modelValue":"CLASSICS",
                 "nodeList":[
                  {"nodeCode":"start","nodeName":"开始","nodeType":0,"nodeRatio":"0","coordinate":"100,100",
                   "skipList":[{"nowNodeCode":"start","nextNodeCode":"review","skipType":"PASS"}]},
                  {"nodeCode":"review","nodeName":"审批","nodeType":1,"permissionFlag":"user:20",
                   "nodeRatio":"0","ext":%s,"coordinate":"300,100",
                   "skipList":[{"nowNodeCode":"review","nextNodeCode":"end","skipType":"PASS"},
                               {"nowNodeCode":"review","nextNodeCode":"start","skipType":"REJECT"}]},
                  {"nodeCode":"end","nodeName":"结束","nodeType":2,"nodeRatio":"0","coordinate":"500,100","skipList":[]}]}
                """.formatted(FlowEngine.jsonConvert.objToStr(ext));
        definitionService.publish(definitionService.create(json));
        Long instanceId = workflowService.start(code, "cc-test", Map.of());
        assertEquals("申请人", inboxService.started(code, 1, 20).records().getFirst().getInitiatorName());
        login("20");
        Long taskId = inboxService.todo(code, 1, 20).records().getFirst().getTaskId();
        assertEquals(0, inboxService.cc(code, 1, 20).total());
        workflowService.pass(taskId, "通过", Map.of());
        assertEquals("审批人甲", inboxService.done(code, 1, 20).records().getFirst().getApproverName());
        assertEquals(0, inboxService.cc(code, 1, 20).total());
        login("40");
        var cc = inboxService.cc(code, 1, 20);
        assertEquals(1, cc.total());
        assertEquals(instanceId, cc.records().getFirst().getInstanceId());
        assertEquals("知会人", ccMapper.selectList(null).stream()
                .filter(record -> "40".equals(record.getRecipientId())).findFirst().orElseThrow().getRecipientName());
        assertEquals(null, cc.records().getFirst().getReadTime());
        RequestContextHolder.setTenantId(2L);
        assertEquals(0, inboxService.cc(code, 1, 20).total());
        assertThrows(CommonException.class, () -> inboxService.detail(instanceId));
        RequestContextHolder.setTenantId(1L);
        assertEquals(instanceId, inboxService.detail(instanceId).instance().getInstanceId());
        inboxService.readCc(cc.records().getFirst().getId());
        assertTrue(inboxService.cc(code, 1, 20).records().getFirst().getReadTime() != null);
        inboxService.readCc(cc.records().getFirst().getId());
        login("99");
        assertEquals(0, inboxService.cc(code, 1, 20).total());
        assertThrows(CommonException.class, () -> inboxService.detail(instanceId));
        assertThrows(CommonException.class, () -> inboxService.readCc(cc.records().getFirst().getId()));
        login("10");
        assertEquals(1, inboxService.cc(code, 1, 20).total());
    }

    @Test
    void ccWaitsForTheWholeCountersignAndDoesNotFireOnReject() {
        String ext = ccExt("user:40");
        String countersign = publish("cc_countersign", "user:20@@user:30", false, "100", ext);
        workflowService.start(countersign, "cc-sign", Map.of());
        login("20");
        Long taskId = inboxService.todo(countersign, 1, 20).records().getFirst().getTaskId();
        workflowService.pass(taskId, "同意", Map.of());
        login("40");
        assertEquals(0, inboxService.cc(countersign, 1, 20).total());
        login("30");
        workflowService.pass(taskId, "同意", Map.of());
        login("40");
        assertEquals(1, inboxService.cc(countersign, 1, 20).total());

        login("10");
        String rejected = publish("cc_rejected", "user:20", true, "0", ext);
        workflowService.start(rejected, "cc-reject", Map.of());
        workflowService.pass(inboxService.todo(rejected, 1, 20).records().getFirst().getTaskId(), "提交", Map.of());
        login("20");
        workflowService.reject(inboxService.todo(rejected, 1, 20).records().getFirst().getTaskId(), "退回", Map.of());
        login("40");
        assertEquals(0, inboxService.cc(rejected, 1, 20).total());
    }

    private String ccExt(String flag) {
        String json = """
                {"enabled":true,"includeChildDepartments":false,"recipients":[{"storageId":"%s","handlerName":"知会人"}]}
                """.formatted(flag);
        return FlowEngine.jsonConvert.objToStr(List.of(Map.of("code", "flowCc",
                "value", URLEncoder.encode(json, StandardCharsets.UTF_8))));
    }

    private String taskActionsExt(boolean transfer, boolean delegation) {
        String json = "{\"transferEnabled\":" + transfer + ",\"delegationEnabled\":" + delegation + "}";
        return FlowEngine.jsonConvert.objToStr(List.of(Map.of("code", WorkflowTaskActionConfig.EXT_CODE,
                "value", URLEncoder.encode(json, StandardCharsets.UTF_8))));
    }

    private void login(String userId) {
        USER.set(userId);
        RequestContextHolder.setTenantId(1L);
    }

    private String publish(String code, String assignees) {
        return publish(code, assignees, false, "0");
    }

    /** 与前端设计器一致：引擎导出JSON不含数据库主键，保存编辑时补上当前定义ID。 */
    private String editableDesign(Long definitionId) {
        DefJson design = FlowEngine.jsonConvert.strToBean(definitionService.exportJson(definitionId), DefJson.class);
        design.setId(definitionId);
        return FlowEngine.jsonConvert.objToStr(design);
    }

    private String publish(String code, String assignees, boolean submissionNode) {
        return publish(code, assignees, submissionNode, "0");
    }

    private String publish(String code, String assignees, boolean submissionNode, String ratio) {
        return publish(code, assignees, submissionNode, ratio, null);
    }

    private String publish(String code, String assignees, boolean submissionNode, String ratio, String ext) {
        String json = """
                {"flowCode":"%s","flowName":"请假测试","version":"1","modelValue":"CLASSICS",
                 "nodeList":[
                  {"nodeCode":"start","nodeName":"申请","nodeType":0,"permissionFlag":"user:10","nodeRatio":"0","coordinate":"100,100",
                   "skipList":[{"nowNodeCode":"start","nextNodeCode":"review","skipType":"PASS"}]},
                  {"nodeCode":"review","nodeName":"部门审批","nodeType":1,"permissionFlag":"%s","nodeRatio":"%s","coordinate":"300,100",
                   "skipList":[{"nowNodeCode":"review","nextNodeCode":"end","skipType":"PASS"},
                               {"nowNodeCode":"review","nextNodeCode":"start","skipType":"REJECT"}]},
                  {"nodeCode":"end","nodeName":"结束","nodeType":2,"nodeRatio":"0","coordinate":"500,100","skipList":[]}]}
                """.formatted(code, assignees, ratio);
        if (ext != null) {
            json = json.replace("\"nodeRatio\":\"%s\",\"coordinate\":\"300,100\"".formatted(ratio),
                    "\"nodeRatio\":\"%s\",\"ext\":%s,\"coordinate\":\"300,100\""
                            .formatted(ratio, FlowEngine.jsonConvert.objToStr(ext)));
        }
        if (submissionNode) {
            // 引擎不允许退回开始节点；可重新提交的申请应建模为一个普通业务节点。
            json = json.replace("\"nextNodeCode\":\"review\"", "\"nextNodeCode\":\"apply\"")
                    .replace("\"nextNodeCode\":\"start\"", "\"nextNodeCode\":\"apply\"")
                    .replace("{\"nodeCode\":\"review\"", """
                            {"nodeCode":"apply","nodeName":"申请提交","nodeType":1,"permissionFlag":"user:10",
                             "nodeRatio":"0","coordinate":"200,100","skipList":[
                               {"nowNodeCode":"apply","nextNodeCode":"review","skipType":"PASS"}]},
                            {"nodeCode":"review"
                            """.stripTrailing());
        }
        definitionService.publish(definitionService.create(json));
        return code;
    }

    @Configuration
    @EnableTransactionManagement
    @Import({FlowAutoConfig.class, FlowQueryConfig.class, FlowTenantConfig.class, FlowPermissionConfig.class,
            FlowCcListener.class,
            WorkflowServiceImpl.class, WorkflowInboxServiceImpl.class, WorkflowDefinitionServiceImpl.class,
            io.github.yilers.flow.service.impl.WorkflowAssigneeResolver.class,
            io.github.yilers.flow.leave.config.LeaveProperties.class,
            io.github.yilers.flow.leave.service.LeaveServiceImpl.class,
            io.github.yilers.flow.leave.service.LeaveApplicationTasks.class,
            io.github.yilers.flow.leave.service.LeaveBusinessProvider.class})
    static class Config {

        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                    .setName("flow-runtime-" + java.util.UUID.randomUUID() + ";MODE=MySQL")
                    .setScriptEncoding("UTF-8").addScript(new ClassPathResource("flow-engine-schema.sql").getPath())
                    .addScript("flow-leave-schema.sql").addScript("flow-cc-schema.sql").build();
        }

        @Bean
        DataSourceTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor(FlowTenantConfig tenantConfig) {
            var interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(tenantConfig.flowTenantLineInnerInterceptor());
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
            return interceptor;
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource, MybatisPlusInterceptor interceptor) throws Exception {
            var factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            // 与真实宿主一致启用公共填充器，覆盖任务deleteByIds触发updateFill的路径。
            factory.setGlobalConfig(new com.baomidou.mybatisplus.core.config.GlobalConfig()
                    .setMetaObjectHandler(new io.github.yilers.web.mybatis.MyMetaObjectHandler()));
            factory.setPlugins(interceptor);
            factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml"));
            var configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            factory.setConfiguration(configuration);
            return factory.getObject();
        }

        @Bean
        WorkflowIdentityProvider identityProvider() {
            return new WorkflowIdentityProvider() {
                public String currentUserId() { return USER.get(); }
                public String currentUserName() { return USER.get(); }
                public List<String> currentPermissionFlags() { return List.of(USER.get(), "user:" + USER.get()); }
                public Map<String, String> resolvePermissionNames(List<String> flags) {
                    return Map.of("user:10", "申请人", "user:20", "审批人甲", "user:30", "审批人乙",
                            "user:40", "知会人");
                }
                public List<String> resolveUserIds(List<String> flags) {
                    return flags.stream().map(flag -> flag.startsWith("user:") ? flag.substring(5) : flag).toList();
                }
                public List<String> resolveCcUserIds(String applicantId, List<String> recipients, boolean includeChildDepartments) {
                    return recipients.stream().flatMap(recipient -> {
                        var rule = io.github.yilers.flow.model.WorkflowAssigneeRule.fromExpression(recipient);
                        return rule == null ? resolveUserIds(List.of(recipient)).stream()
                                : resolveDynamicAssignees(applicantId, rule).stream();
                    }).distinct().toList();
                }
                public Map<String, io.github.yilers.flow.model.WorkflowUserInfo> resolveUsers(List<String> userIds) {
                    Map<String, io.github.yilers.flow.model.WorkflowUserInfo> users = Map.of(
                            "10", new io.github.yilers.flow.model.WorkflowUserInfo("10", "申请人"),
                            "20", new io.github.yilers.flow.model.WorkflowUserInfo("20", "审批人甲"),
                            "30", new io.github.yilers.flow.model.WorkflowUserInfo("30", "审批人乙"),
                            "40", new io.github.yilers.flow.model.WorkflowUserInfo("40", "知会人"));
                    return userIds.stream().filter(users::containsKey)
                            .collect(java.util.stream.Collectors.toMap(id -> id, users::get));
                }
                public List<String> resolveDynamicAssignees(String applicantId, io.github.yilers.flow.model.WorkflowAssigneeRule rule) {
                    if ("99".equals(applicantId)) return List.of();
                    return switch (rule) {
                        case APPLICANT -> List.of(applicantId);
                        case DEPARTMENT_APPROVERS -> List.of("20", "30");
                        case PARENT_DEPARTMENT_APPROVERS -> List.of("40");
                    };
                }
            };
        }
    }
}
