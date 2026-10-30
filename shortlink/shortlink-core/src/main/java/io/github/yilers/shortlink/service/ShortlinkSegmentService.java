package io.github.yilers.shortlink.service;

import io.github.yilers.shortlink.mapper.ShortlinkSequenceMapper;
import io.github.yilers.shortlink.support.ShortlinkRules;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.shortlink.i18n.ShortlinkMessage;
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
        if (tenantId == null) throw new CommonException(ShortlinkMessage.TENANT_REQUIRED);
        Long high = mapper.lockHighWatermark(namespaceId, tenantId);
        if (high == null) throw new CommonException(ShortlinkMessage.SEGMENT_UNINITIALIZED);
        long maximum = ShortlinkRules.capacity(length) - 1;
        if (high >= maximum) throw new CommonException(ShortlinkMessage.SEGMENT_EXHAUSTED);
        long next = Math.min(maximum, Math.addExact(high, ShortlinkRules.SEGMENT_SIZE));
        if (mapper.advance(namespaceId, tenantId, high, next) != 1) throw new CommonException(ShortlinkMessage.SEGMENT_CONFLICT);
        return new Segment(high + 1, next);
    }
}
