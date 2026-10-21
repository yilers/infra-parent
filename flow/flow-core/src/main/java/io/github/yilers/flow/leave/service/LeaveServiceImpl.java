package io.github.yilers.flow.leave.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.flow.leave.config.LeaveProperties;
import io.github.yilers.flow.leave.constant.LeaveConst;
import io.github.yilers.flow.leave.entity.LeaveApplication;
import io.github.yilers.flow.leave.enums.LeaveTypeEnum;
import io.github.yilers.flow.leave.mapper.LeaveMapper;
import io.github.yilers.flow.leave.model.LeaveInfo;
import io.github.yilers.flow.leave.model.LeavePage;
import io.github.yilers.flow.leave.request.LeaveSaveRequest;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.flow.service.WorkflowInboxService;
import io.github.yilers.flow.service.WorkflowService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.enums.FlowStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * 请假业务示例：单据先保存草稿，再在同一事务内启动流程并绑定实例。
 * 审批状态实时读取实例，不依赖监听器把某个节点通过误写成整单通过。
 */
@Service
@ConditionalOnProperty(prefix = "flow.leave", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private static final BigDecimal DURATION_STEP_HOURS = new BigDecimal(LeaveConst.DURATION_STEP_HOURS);
    private static final BigDecimal MAX_DURATION_HOURS = new BigDecimal(LeaveConst.MAX_DURATION_HOURS);
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(Duration.ofHours(1).toMinutes());

    private final LeaveMapper mapper;
    private final WorkflowIdentityProvider identityProvider;
    private final WorkflowService workflowService;
    private final WorkflowInboxService inboxService;
    private final LeaveProperties properties;
    private final LeaveApplicationTasks applicationTasks;

    @Override
    public LeavePage page(String status, int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new CommonException("页码必须大于0，且每页最多100条");
        }
        String userId = identityProvider.currentUserId();
        Page<LeaveInfo> page = mapper.pageMine(new Page<>(pageNum, pageSize), tenantId(), userId,
                status == null || status.isBlank() ? null : status.trim());
        page.getRecords().forEach(info -> decorate(info, userId));
        return new LeavePage(page.getTotal(), page.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveInfo create(LeaveSaveRequest request) {
        validate(request);
        String userId = identityProvider.currentUserId();
        if (properties.getFlowCode() == null || properties.getFlowCode().isBlank()) {
            throw new CommonException("请先配置请假业务流程编码");
        }
        LeaveApplication leave = new LeaveApplication();
        leave.setTenantId(tenantId());
        leave.setApplicantId(userId);
        String name = identityProvider.currentUserName();
        leave.setApplicantName(name == null || name.isBlank() ? userId : name);
        leave.setLeaveType(request.leaveType());
        leave.setStartTime(request.startTime());
        leave.setEndTime(request.endTime());
        leave.setDurationHours(request.durationHours());
        leave.setReason(request.reason().trim());
        leave.setFlowCode(properties.getFlowCode().trim());
        leave.setVersion(1);
        leave.setCreateTime(LocalDateTime.now());
        leave.setUpdateTime(leave.getCreateTime());
        if (mapper.insert(leave) != 1) {
            throw new CommonException("保存请假草稿失败");
        }
        return decorate(mapper.info(leave.getId(), leave.getTenantId()), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public LeaveInfo edit(Long id, LeaveSaveRequest request) {
        requireId(id);
        validate(request);
        requireVersion(request.version());
        String userId = identityProvider.currentUserId();
        String tenantId = tenantId();
        LeaveInfo original = requireMine(id, tenantId, userId);
        LeaveSaveRequest normalized = new LeaveSaveRequest(request.leaveType(), request.startTime(), request.endTime(),
                request.durationHours(), request.reason().trim(), request.version(), request.submissionTaskId());
        int updated;
        if (original.getInstanceId() == null) {
            if (request.submissionTaskId() != null) throw staleDraft();
            updated = mapper.editDraft(id, tenantId, userId, normalized);
        } else {
            applicationTasks.lockCurrentTask(original.getInstanceId(), request.submissionTaskId(), userId);
            requireVersion(mapper.lockMine(id, tenantId, userId), request.version(), original.getInstanceId());
            updated = mapper.editSubmitted(id, tenantId, userId, original.getInstanceId(), normalized);
        }
        if (updated != 1) {
            throw staleDraft();
        }
        return decorate(mapper.info(id, tenantId), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long submit(Long id, Integer version) {
        // 兼容入口同样由Spring代理开启事务，不能使用接口default方法导致内部调用绕过事务。
        return submit(id, version, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long submit(Long id, Integer version, Long submissionTaskId) {
        requireId(id);
        requireVersion(version);
        String userId = identityProvider.currentUserId();
        String tenantId = tenantId();
        LeaveInfo original = requireMine(id, tenantId, userId);
        if (original.getInstanceId() != null) {
            return resubmit(original, version, submissionTaskId, userId, tenantId);
        }
        if (submissionTaskId != null) throw staleDraft();
        // 同一单据的重复点击或不同窗口提交，由数据库行锁串行化。
        LeaveApplication leave = mapper.lockMine(id, tenantId, userId);
        requireVersion(leave, version, null);
        validateStored(leave);
        // 只给引擎必要路由参数，请假原因等正文保留在业务表，通过受保护详情接口读取。
        Long instanceId = workflowService.start(leave.getFlowCode(), id.toString(), variables(leave));
        if (mapper.bindInstance(id, tenantId, userId, version, instanceId) != 1) {
            throw staleDraft();
        }
        // 业务关联先落库，随后在同一事务内完成唯一申请节点；主管节点绝不自动通过。
        var applicationTask = applicationTasks.initialTask(instanceId);
        if (applicationTask != null) {
            workflowService.submitBusinessTask(applicationTask.getId(), LeaveConst.BUSINESS_TYPE, "提交申请", Map.of());
        }
        return instanceId;
    }

    private Long resubmit(LeaveInfo original, Integer version, Long taskId, String userId, String tenantId) {
        applicationTasks.lockCurrentTask(original.getInstanceId(), taskId, userId);
        LeaveApplication leave = mapper.lockMine(original.getId(), tenantId, userId);
        requireVersion(leave, version, original.getInstanceId());
        validateStored(leave);
        if (mapper.resubmitVersion(leave.getId(), tenantId, userId, leave.getInstanceId(), version) != 1) {
            throw staleDraft();
        }
        // 更新路由字段但不覆盖已固化的动态办理人快照，也不重新创建流程实例。
        return workflowService.submitBusinessTask(taskId, LeaveConst.BUSINESS_TYPE, "重新提交申请", variables(leave));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long id, Integer version) {
        requireId(id);
        requireVersion(version);
        String userId = identityProvider.currentUserId();
        if (mapper.deleteDraft(id, tenantId(), userId, version) != 1) {
            throw staleDraft();
        }
    }

    @Override
    public LeaveInfo detail(Long id) {
        requireId(id);
        String userId = identityProvider.currentUserId();
        LeaveInfo leave = mapper.info(id, tenantId());
        if (leave == null) {
            throw new CommonException(404, "请假申请不存在或无权查看");
        }
        if (!userId.equals(leave.getApplicantId())) {
            if (leave.getInstanceId() == null) {
                throw new CommonException(404, "请假申请不存在或无权查看");
            }
            inboxService.checkInstanceAccess(leave.getInstanceId());
        }
        return decorate(leave, userId);
    }

    private LeaveInfo requireMine(Long id, String tenantId, String userId) {
        LeaveInfo leave = mapper.info(id, tenantId);
        if (leave == null || !userId.equals(leave.getApplicantId())) {
            throw staleDraft();
        }
        return leave;
    }

    private LeaveInfo decorate(LeaveInfo info, String userId) {
        if (!userId.equals(info.getApplicantId())) return info;
        if (info.getInstanceId() == null) {
            info.setEditable(true);
        } else if (FlowStatus.APPROVAL.getKey().equals(info.getFlowStatus())
                || FlowStatus.REJECT.getKey().equals(info.getFlowStatus())) {
            var task = applicationTasks.currentTask(info.getInstanceId());
            if (task != null && applicationTasks.canHandle(task.getId(), userId)) {
                info.setEditable(true);
                info.setSubmissionTaskId(task.getId());
            }
        }
        return info;
    }

    private void requireVersion(LeaveApplication leave, Integer version, Long instanceId) {
        if (leave == null || !Objects.equals(version, leave.getVersion()) || !Objects.equals(instanceId, leave.getInstanceId())) {
            throw staleDraft();
        }
    }

    private void validateStored(LeaveApplication leave) {
        validate(new LeaveSaveRequest(leave.getLeaveType(), leave.getStartTime(), leave.getEndTime(),
                leave.getDurationHours(), leave.getReason(), leave.getVersion()));
    }

    private Map<String, Object> variables(LeaveApplication leave) {
        return Map.of("businessType", LeaveConst.BUSINESS_TYPE, "applicantId", leave.getApplicantId(),
                "leaveType", leave.getLeaveType(), "durationHours", leave.getDurationHours());
    }

    /** 服务契约也校验业务规则，避免其他模块直接调用Service时绕过Controller校验。 */
    private void validate(LeaveSaveRequest request) {
        if (request == null || !LeaveTypeEnum.supports(request.leaveType())) {
            throw new CommonException("请选择有效的请假类型");
        }
        if (request.startTime() == null || request.endTime() == null || !request.endTime().isAfter(request.startTime())) {
            throw new CommonException("请假结束时间必须晚于开始时间");
        }
        BigDecimal hours = request.durationHours();
        if (hours == null || hours.compareTo(DURATION_STEP_HOURS) < 0 || hours.compareTo(MAX_DURATION_HOURS) > 0
                || hours.remainder(DURATION_STEP_HOURS).signum() != 0) {
            throw new CommonException("请假时长须为0.5小时的整数倍，且不超过8760小时");
        }
        BigDecimal intervalMinutes = BigDecimal.valueOf(Duration.between(request.startTime(), request.endTime()).toMinutes());
        if (hours.multiply(MINUTES_PER_HOUR).compareTo(intervalMinutes) > 0) {
            throw new CommonException("请假时长不能超过起止时间的自然时长");
        }
        if (request.reason() == null || request.reason().isBlank()
                || request.reason().trim().length() > LeaveConst.MAX_REASON_LENGTH) {
            throw new CommonException("请填写请假原因，最多1000字");
        }
    }

    private String tenantId() {
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) {
            throw new CommonException("Flow租户上下文不能为空");
        }
        return tenantId.toString();
    }

    private void requireId(Long id) {
        if (id == null || id <= 0) {
            throw new CommonException("请假申请ID必须为正数");
        }
    }

    private void requireVersion(Integer version) {
        if (version == null || version < 1) {
            throw new CommonException("请携带有效的申请版本号");
        }
    }

    private CommonException staleDraft() {
        return new CommonException("申请已变更、当前不可编辑或无权操作，请刷新后重新打开表单");
    }
}
