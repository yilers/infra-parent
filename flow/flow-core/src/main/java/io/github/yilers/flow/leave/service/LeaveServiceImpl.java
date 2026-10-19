package io.github.yilers.flow.leave.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.flow.leave.config.LeaveProperties;
import io.github.yilers.flow.leave.entity.LeaveApplication;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 请假业务示例：单据先保存草稿，再在同一事务内启动流程并绑定实例。
 * 审批状态实时读取实例，不依赖监听器把某个节点通过误写成整单通过。
 */
@Service
@ConditionalOnProperty(prefix = "flow.leave", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private static final Set<String> TYPES = Set.of("PERSONAL", "SICK", "ANNUAL", "COMP_TIME", "OTHER");
    private static final BigDecimal STEP = new BigDecimal("0.5");

    private final LeaveMapper mapper;
    private final WorkflowIdentityProvider identityProvider;
    private final WorkflowService workflowService;
    private final WorkflowInboxService inboxService;
    private final LeaveProperties properties;

    @Override
    public LeavePage page(String status, int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new CommonException("页码必须大于0，且每页最多100条");
        }
        String userId = identityProvider.currentUserId();
        Page<LeaveInfo> page = mapper.pageMine(new Page<>(pageNum, pageSize), tenantId(), userId,
                status == null || status.isBlank() ? null : status.trim());
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
        return mapper.info(leave.getId(), leave.getTenantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveInfo edit(Long id, LeaveSaveRequest request) {
        requireId(id);
        validate(request);
        requireVersion(request.version());
        String userId = identityProvider.currentUserId();
        LeaveSaveRequest normalized = new LeaveSaveRequest(request.leaveType(), request.startTime(), request.endTime(),
                request.durationHours(), request.reason().trim(), request.version());
        if (mapper.editDraft(id, tenantId(), userId, normalized) != 1) {
            throw staleDraft();
        }
        return mapper.info(id, tenantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long submit(Long id, Integer version) {
        requireId(id);
        requireVersion(version);
        String userId = identityProvider.currentUserId();
        String tenantId = tenantId();
        // 同一单据的重复点击或不同窗口提交，由数据库行锁串行化。
        LeaveApplication leave = mapper.lockMine(id, tenantId, userId);
        if (leave == null || leave.getInstanceId() != null || !Objects.equals(version, leave.getVersion())) {
            throw staleDraft();
        }
        validate(new LeaveSaveRequest(leave.getLeaveType(), leave.getStartTime(), leave.getEndTime(),
                leave.getDurationHours(), leave.getReason(), leave.getVersion()));
        // 只给引擎必要路由参数，请假原因等正文保留在业务表，通过受保护详情接口读取。
        Long instanceId = workflowService.start(leave.getFlowCode(), id.toString(), Map.of(
                "businessType", "leave", "applicantId", userId,
                "leaveType", leave.getLeaveType(), "durationHours", leave.getDurationHours()));
        if (mapper.bindInstance(id, tenantId, userId, version, instanceId) != 1) {
            throw staleDraft();
        }
        return instanceId;
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
        return leave;
    }

    /** 服务契约也校验业务规则，避免其他模块直接调用Service时绕过Controller校验。 */
    private void validate(LeaveSaveRequest request) {
        if (request == null || !TYPES.contains(request.leaveType() == null ? "" : request.leaveType())) {
            throw new CommonException("请选择有效的请假类型");
        }
        if (request.startTime() == null || request.endTime() == null || !request.endTime().isAfter(request.startTime())) {
            throw new CommonException("请假结束时间必须晚于开始时间");
        }
        BigDecimal hours = request.durationHours();
        if (hours == null || hours.compareTo(STEP) < 0 || hours.compareTo(new BigDecimal("8760")) > 0
                || hours.remainder(STEP).signum() != 0) {
            throw new CommonException("请假时长须为0.5小时的整数倍，且不超过8760小时");
        }
        BigDecimal intervalMinutes = BigDecimal.valueOf(Duration.between(request.startTime(), request.endTime()).toMinutes());
        if (hours.multiply(BigDecimal.valueOf(60)).compareTo(intervalMinutes) > 0) {
            throw new CommonException("请假时长不能超过起止时间的自然时长");
        }
        if (request.reason() == null || request.reason().isBlank() || request.reason().trim().length() > 1000) {
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
            throw new CommonException("请携带有效的草稿版本号");
        }
    }

    private CommonException staleDraft() {
        return new CommonException("草稿已变更、已提交、不存在或无权操作，请刷新后重试");
    }
}
