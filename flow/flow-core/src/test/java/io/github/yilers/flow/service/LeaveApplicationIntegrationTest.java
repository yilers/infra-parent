package io.github.yilers.flow.service;

import io.github.yilers.flow.leave.config.LeaveProperties;
import io.github.yilers.flow.leave.model.LeaveInfo;
import io.github.yilers.flow.leave.request.LeaveSaveRequest;
import io.github.yilers.flow.leave.service.LeaveService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.dromara.warm.flow.core.enums.FlowStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 请假业务与真实引擎、数据库事务一起测试，不在开发库生成测试单据。 */
@SpringJUnitConfig(WorkflowRuntimeIntegrationTest.Config.class)
@TestPropertySource(properties = "flow.leave.enabled=true")
class LeaveApplicationIntegrationTest {

    @Autowired
    private LeaveService leaveService;
    @Autowired
    private LeaveProperties properties;
    @Autowired
    private WorkflowDefinitionService definitionService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private WorkflowInboxService inboxService;

    private String flowCode;

    @BeforeEach
    void setup() {
        login("10", 1L);
        flowCode = "leave_test_" + UUID.randomUUID().toString().substring(0, 8);
        properties.setFlowCode(flowCode);
    }

    @AfterEach
    void cleanup() {
        properties.setFlowCode("leave");
        WorkflowRuntimeIntegrationTest.USER.remove();
        RequestContextHolder.clear();
    }

    @Test
    void applicantNodeAutomaticallySubmitsThenSupportsReturnedEditsInTheSameInstance() {
        publishWithApplication("user:20");
        LeaveInfo draft = leaveService.create(request(null));
        Long instanceId = leaveService.submit(draft.getId(), 1);
        assertFalse(leaveService.detail(draft.getId()).isEditable());
        assertEquals(0, inboxService.todo(flowCode, 1, 20).total());
        assertTrue(inboxService.detail(instanceId).history().stream()
                .anyMatch(item -> item.getSubmission() != null && "PASS".equals(item.getSkipType())));
        login("20", 1L);
        var review = inboxService.todo(flowCode, 1, 20).records().getFirst();
        assertNull(review.getSubmission());
        workflowService.reject(review.getTaskId(), "请补充请假原因", Map.of());
        login("10", 1L);
        LeaveInfo returned = leaveService.detail(draft.getId());
        assertTrue(returned.isEditable());
        assertNotNull(returned.getSubmissionTaskId());
        var applicationTask = inboxService.todo(flowCode, 1, 20).records().getFirst();
        assertEquals("leave", applicationTask.getSubmission().type());
        assertThrows(CommonException.class, () -> workflowService.pass(applicationTask.getTaskId(), "绕过表单", Map.of()));
        assertThrows(CommonException.class, () -> workflowService.reject(applicationTask.getTaskId(), "拒绝自己", Map.of()));
        assertThrows(CommonException.class, () -> leaveService.edit(draft.getId(), request(returned.getVersion())));
        assertThrows(CommonException.class, () -> leaveService.submit(draft.getId(), returned.getVersion()));
        var corrected = new LeaveSaveRequest("PERSONAL", request(null).startTime(), request(null).endTime(),
                new BigDecimal("4"), "补充后的请假原因", returned.getVersion(), returned.getSubmissionTaskId());
        LeaveInfo edited = leaveService.edit(draft.getId(), corrected);
        assertEquals(returned.getVersion() + 1, edited.getVersion());
        assertEquals("补充后的请假原因", edited.getReason());
        assertThrows(CommonException.class, () -> leaveService.edit(draft.getId(), corrected));
        login("20", 1L);
        assertThrows(CommonException.class, () -> leaveService.edit(draft.getId(), corrected));
        assertThrows(CommonException.class, () -> leaveService.submit(draft.getId(), edited.getVersion(), edited.getSubmissionTaskId()));
        login("10", 2L);
        assertThrows(CommonException.class, () -> leaveService.submit(draft.getId(), edited.getVersion(), edited.getSubmissionTaskId()));
        login("10", 1L);
        assertEquals(instanceId, leaveService.submit(draft.getId(), edited.getVersion(), edited.getSubmissionTaskId()));
        assertThrows(CommonException.class, () -> leaveService.submit(draft.getId(), edited.getVersion(), edited.getSubmissionTaskId()));
        assertFalse(leaveService.detail(draft.getId()).isEditable());
        assertEquals(1, inboxService.started(flowCode, 1, 20).total());
        login("20", 1L);
        assertEquals("补充后的请假原因", leaveService.detail(draft.getId()).getReason());
        workflowService.pass(inboxService.todo(flowCode, 1, 20).records().getFirst().getTaskId(), "同意", Map.of());
        login("10", 1L);
        LeaveInfo completed = leaveService.detail(draft.getId());
        assertEquals(FlowStatus.FINISHED.getKey(), completed.getFlowStatus());
        assertFalse(completed.isEditable());
        assertThrows(CommonException.class, () -> leaveService.edit(draft.getId(), corrected));
        assertTrue(inboxService.detail(instanceId).history().stream()
                .anyMatch(item -> "重新提交申请".equals(item.getMessage())));
    }

    @Test
    void applicantCanTakeBackPendingLeaveAndThenEditAndResubmit() {
        publishWithApplication("user:20");
        LeaveInfo draft = leaveService.create(request(null));
        Long instanceId = leaveService.submit(draft.getId(), 1);
        assertFalse(leaveService.detail(draft.getId()).isEditable());

        assertEquals(instanceId, workflowService.takeBack(instanceId));
        LeaveInfo takenBack = leaveService.detail(draft.getId());
        assertEquals(FlowStatus.TASK_BACK.getKey(), takenBack.getFlowStatus());
        assertTrue(takenBack.isEditable());
        assertNotNull(takenBack.getSubmissionTaskId());

        LeaveInfo edited = leaveService.edit(draft.getId(), new LeaveSaveRequest("PERSONAL",
                request(null).startTime(), request(null).endTime(), new BigDecimal("4"),
                "拿回后修改并重新提交", takenBack.getVersion(), takenBack.getSubmissionTaskId()));
        assertEquals(instanceId, leaveService.submit(draft.getId(), edited.getVersion(), edited.getSubmissionTaskId()));
        assertFalse(leaveService.detail(draft.getId()).isEditable());
        assertEquals(FlowStatus.APPROVAL.getKey(), leaveService.detail(draft.getId()).getFlowStatus());
    }

    @Test
    void applicantCanRevokeAnInFlightLeaveAndItCannotBeEditedAfterward() {
        publishWithApplication("user:20");
        LeaveInfo draft = leaveService.create(request(null));
        Long instanceId = leaveService.submit(draft.getId(), 1);

        assertEquals(instanceId, workflowService.revoke(instanceId));
        LeaveInfo cancelled = leaveService.detail(draft.getId());
        assertEquals(FlowStatus.CANCEL.getKey(), cancelled.getFlowStatus());
        assertFalse(cancelled.isEditable());
        assertEquals(0, inboxService.todo(flowCode, 1, 20).total());
        assertThrows(CommonException.class, () -> leaveService.edit(draft.getId(), request(cancelled.getVersion())));
    }

    @Test
    void automaticApplicationSubmissionRollsBackTheWholeStartIfReviewerIsMissing() {
        publishWithApplication("");
        LeaveInfo draft = leaveService.create(request(null));
        assertThrows(CommonException.class, () -> leaveService.submit(draft.getId(), 1));
        assertNull(leaveService.detail(draft.getId()).getInstanceId());
        assertEquals(1, leaveService.detail(draft.getId()).getVersion());
        assertEquals(0, inboxService.started(flowCode, 1, 20).total());
    }

    @Test
    void concurrentResubmissionsCompleteOnlyOneApplicationTask() throws Exception {
        publishWithApplication("user:20");
        LeaveInfo draft = leaveService.create(request(null));
        leaveService.submit(draft.getId(), 1);
        login("20", 1L);
        workflowService.reject(inboxService.todo(flowCode, 1, 20).records().getFirst().getTaskId(), "补充", Map.of());
        login("10", 1L);
        LeaveInfo returned = leaveService.detail(draft.getId());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(() -> resubmit(returned), () -> resubmit(returned)));
            int succeeded = 0;
            for (var result : results) if (Boolean.TRUE.equals(result.get())) succeeded++;
            assertEquals(1, succeeded);
        }
        assertEquals(returned.getVersion() + 1, leaveService.detail(draft.getId()).getVersion());
        assertEquals(1, inboxService.started(flowCode, 1, 20).total());
    }

    @Test
    void draftEditsRequireOwnershipAndTheCurrentVersion() {
        LeaveInfo draft = leaveService.create(request(null));
        assertEquals("DRAFT", draft.getFlowStatus());
        assertNull(draft.getInstanceId());
        assertEquals(1, draft.getVersion());
        assertEquals("10", draft.getApplicantId());
        var edited = leaveService.edit(draft.getId(), request(1));
        assertEquals(2, edited.getVersion());
        assertThrows(CommonException.class, () -> leaveService.edit(draft.getId(), request(1)));
        login("99", 1L);
        assertThrows(CommonException.class, () -> leaveService.detail(draft.getId()));
        assertThrows(CommonException.class, () -> leaveService.edit(draft.getId(), request(2)));
        assertEquals(0, leaveService.page(null, 1, 20).total());
        login("10", 2L);
        assertThrows(CommonException.class, () -> leaveService.detail(draft.getId()));
        assertEquals(0, leaveService.page(null, 1, 20).total());
        login("10", 1L);
        assertThrows(CommonException.class, () -> leaveService.deleteDraft(draft.getId(), 1));
        leaveService.deleteDraft(draft.getId(), 2);
        assertThrows(CommonException.class, () -> leaveService.detail(draft.getId()));
    }

    @Test
    void submitAndApproveExposeBusinessDetailsOnlyToParticipants() {
        publish();
        var draft = leaveService.create(request(null));
        Long instanceId = leaveService.submit(draft.getId(), draft.getVersion());
        assertEquals(instanceId, leaveService.detail(draft.getId()).getInstanceId());
        assertEquals(FlowStatus.APPROVAL.getKey(), leaveService.detail(draft.getId()).getFlowStatus());
        assertThrows(CommonException.class, () -> leaveService.submit(draft.getId(), draft.getVersion()));
        assertThrows(CommonException.class, () -> leaveService.edit(draft.getId(), request(2)));
        assertThrows(CommonException.class, () -> leaveService.deleteDraft(draft.getId(), 2));

        login("99", 1L);
        assertThrows(CommonException.class, () -> leaveService.detail(draft.getId()));
        login("20", 1L);
        var detail = inboxService.detail(instanceId);
        assertNotNull(detail.business());
        assertEquals("leave", detail.business().type());
        assertEquals(draft.getId(), detail.business().id());
        assertEquals("外出办事", leaveService.detail(draft.getId()).getReason());
        workflowService.pass(detail.tasks().getFirst().getTaskId(), "同意", Map.of());
        assertEquals(FlowStatus.FINISHED.getKey(), leaveService.detail(draft.getId()).getFlowStatus());
        login("10", 1L);
        assertEquals(1, leaveService.page(FlowStatus.FINISHED.getKey(), 1, 20).total());
        assertEquals(0, leaveService.page("DRAFT", 1, 20).total());
    }

    @Test
    void missingPublishedFlowRollsBackTheSubmission() {
        var draft = leaveService.create(request(null));
        assertThrows(RuntimeException.class, () -> leaveService.submit(draft.getId(), draft.getVersion()));
        var unchanged = leaveService.detail(draft.getId());
        assertNull(unchanged.getInstanceId());
        assertEquals(1, unchanged.getVersion());
        assertEquals("DRAFT", unchanged.getFlowStatus());
        assertEquals(0, inboxService.started(flowCode, 1, 20).total());
    }

    @Test
    void concurrentSubmissionStartsOnlyOneInstance() throws Exception {
        publish();
        var draft = leaveService.create(request(null));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(() -> submit(draft), () -> submit(draft)));
            long succeeded = 0;
            for (var result : results) {
                if (Boolean.TRUE.equals(result.get())) succeeded++;
            }
            assertEquals(1, succeeded);
        }
        assertEquals(1, inboxService.started(flowCode, 1, 20).total());
    }

    @Test
    void validatesTimeRangeAndHalfHourIncrements() {
        var request = request(null);
        assertThrows(CommonException.class, () -> leaveService.create(new LeaveSaveRequest("SICK",
                request.startTime(), request.startTime(), request.durationHours(), request.reason(), null)));
        assertThrows(CommonException.class, () -> leaveService.create(new LeaveSaveRequest("SICK",
                request.startTime(), request.endTime(), new BigDecimal("0.7"), request.reason(), null)));
        assertThrows(CommonException.class, () -> leaveService.create(new LeaveSaveRequest("SICK",
                request.startTime(), request.endTime(), new BigDecimal("10"), request.reason(), null)));
    }

    private boolean submit(LeaveInfo draft) {
        try {
            login("10", 1L);
            leaveService.submit(draft.getId(), draft.getVersion());
            return true;
        } catch (CommonException expected) {
            return false;
        } finally {
            WorkflowRuntimeIntegrationTest.USER.remove();
            RequestContextHolder.clear();
        }
    }

    private boolean resubmit(LeaveInfo returned) {
        try {
            login("10", 1L);
            leaveService.submit(returned.getId(), returned.getVersion(), returned.getSubmissionTaskId());
            return true;
        } catch (CommonException expected) {
            return false;
        } finally {
            WorkflowRuntimeIntegrationTest.USER.remove();
            RequestContextHolder.clear();
        }
    }

    private void publishWithApplication(String reviewer) {
        String json = """
                {"flowCode":"%s","flowName":"请假","version":"1","modelValue":"MIMIC","nodeList":[
                 {"nodeCode":"start","nodeName":"开始","nodeType":0,"nodeRatio":"0","coordinate":"300,100",
                  "skipList":[{"nowNodeCode":"start","nextNodeCode":"write_leave","skipType":"PASS"}]},
                 {"nodeCode":"write_leave","nodeName":"填写请假信息","nodeType":1,"permissionFlag":"${wfApplicant}",
                  "nodeRatio":"0","coordinate":"300,300",
                  "skipList":[{"nowNodeCode":"write_leave","nextNodeCode":"review","skipType":"PASS"}]},
                 {"nodeCode":"review","nodeName":"主管审核","nodeType":1,"permissionFlag":"%s","nodeRatio":"0",
                  "anyNodeSkip":"write_leave","coordinate":"300,500",
                  "skipList":[{"nowNodeCode":"review","nextNodeCode":"end","skipType":"PASS"}]},
                 {"nodeCode":"end","nodeName":"结束","nodeType":2,"nodeRatio":"0","coordinate":"300,700","skipList":[]}]}
                """.formatted(flowCode, reviewer);
        definitionService.publish(definitionService.create(json));
    }

    private void login(String userId, Long tenantId) {
        WorkflowRuntimeIntegrationTest.USER.set(userId);
        RequestContextHolder.setTenantId(tenantId);
    }

    private LeaveSaveRequest request(Integer version) {
        return new LeaveSaveRequest("PERSONAL", LocalDateTime.of(2026, 10, 20, 9, 0),
                LocalDateTime.of(2026, 10, 20, 18, 0), new BigDecimal("8"), "外出办事", version);
    }

    private void publish() {
        String json = """
                {"flowCode":"%s","flowName":"请假","version":"1","modelValue":"CLASSICS","nodeList":[
                 {"nodeCode":"start","nodeName":"开始","nodeType":0,"nodeRatio":"0","coordinate":"100,100",
                  "skipList":[{"nowNodeCode":"start","nextNodeCode":"review","skipType":"PASS"}]},
                 {"nodeCode":"review","nodeName":"审批","nodeType":1,"permissionFlag":"user:20","nodeRatio":"0","coordinate":"300,100",
                  "skipList":[{"nowNodeCode":"review","nextNodeCode":"end","skipType":"PASS"}]},
                 {"nodeCode":"end","nodeName":"结束","nodeType":2,"nodeRatio":"0","coordinate":"500,100","skipList":[]}]}
                """.formatted(flowCode);
        definitionService.publish(definitionService.create(json));
    }
}
