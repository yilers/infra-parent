package io.github.yilers.shortlink.service;

import io.github.yilers.shortlink.mapper.ShortlinkSequenceMapper;
import io.github.yilers.shortlink.support.ShortlinkRules;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 独立事务分配号段，业务创建失败只浪费号码，不回退高水位。 */
@Service
@RequiredArgsConstructor
public class ShortlinkSegmentService {
    private final ShortlinkSequenceMapper mapper;

    public record Segment(long first, long last) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Segment allocate(Long namespaceId, int length) {
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) throw new CommonException("缺少可信租户上下文");
        Long high = mapper.lockHighWatermark(namespaceId, tenantId);
        if (high == null) throw new CommonException("短链号段未初始化");
        long maximum = ShortlinkRules.capacity(length) - 1;
        if (high >= maximum) throw new CommonException("短链号段已耗尽，请创建新namespace");
        long next = Math.min(maximum, Math.addExact(high, ShortlinkRules.SEGMENT_SIZE));
        if (mapper.advance(namespaceId, tenantId, high, next) != 1) throw new CommonException("号段分配冲突");
        return new Segment(high + 1, next);
    }
}
