package io.github.yilers.shortlink.support;

import io.github.yilers.shortlink.service.ShortlinkSegmentService;
import io.github.yilers.shortlink.entity.ShortlinkNamespace;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/** 进程内消费已提交号段；不同节点由数据库行锁保证号段不重叠。重启允许丢弃未用号码。 */
@Component
@RequiredArgsConstructor
public class ShortlinkCodeGenerator {
    private final ShortlinkSegmentService segments;
    private final NamespaceKeyCipher namespaceKeys;
    private final ShortlinkPermutation permutation;
    private final ConcurrentHashMap<Key, Cursor> cursors = new ConcurrentHashMap<>();

    private record Key(Long tenantId, Long namespaceId) {
    }

    private static final class Cursor {
        private long next;
        private long last = -1;
    }

    public String next(ShortlinkNamespace namespace) {
        Long namespaceId = namespace.getId();
        int length = namespace.getCodeLength();
        boolean legacy = ShortlinkRules.ALGORITHM_SEQUENTIAL.equals(namespace.getCodeAlgorithm());
        if (!legacy && !ShortlinkRules.ALGORITHM_FF1_V1.equals(namespace.getCodeAlgorithm())) {
            throw new CommonException("短码算法未配置或不受支持");
        }
        // 先校验主密钥，避免主密钥错误时仍持续占用已提交的号段。
        byte[] key = legacy ? null : namespaceKeys.decryptKey(namespace.getCodeKeyCipher());
        try {
            Cursor cursor = cursors.computeIfAbsent(
                    new Key(RequestContextHolder.getTenantId(), namespaceId), ignored -> new Cursor());
            long sequence;
            synchronized (cursor) {
                if (cursor.next > cursor.last) {
                    ShortlinkSegmentService.Segment segment = segments.allocate(namespaceId, length);
                    cursor.next = segment.first();
                    cursor.last = segment.last();
                }
                sequence = cursor.next++;
            }
            return legacy ? ShortlinkRules.encode(sequence, length) : permutation.permute(sequence, length, key);
        } finally {
            if (key != null) {
                java.util.Arrays.fill(key, (byte) 0);
            }
        }
    }
}
