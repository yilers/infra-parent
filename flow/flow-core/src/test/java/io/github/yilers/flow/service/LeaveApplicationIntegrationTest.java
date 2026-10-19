package io.github.yilers.flow.service;

import io.github.yilers.flow.leave.config.LeaveProperties;
import io.github.yilers.flow.leave.model.LeaveInfo;
import io.github.yilers.flow.leave.request.LeaveSaveRequest;
import io.github.yilers.flow.leave.service.LeaveService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertEquals("1", leaveService.detail(draft.getId()).getFlowStatus());
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
        assertEquals("8", leaveService.detail(draft.getId()).getFlowStatus());
        login("10", 1L);
        assertEquals(1, leaveService.page("8", 1, 20).total());
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
