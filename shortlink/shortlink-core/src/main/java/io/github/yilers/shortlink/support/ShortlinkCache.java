package io.github.yilers.shortlink.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yilers.shortlink.entity.ShortlinkLink;
import io.github.yilers.shortlink.mapper.ShortlinkLinkMapper;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 仅缓存已命中的短链，不缓存不存在的码。写入前先删除缓存并持有namespace写锁至事务结束；
 * 读取时持读锁回填，避免并发读取把旧值重新写入缓存。状态和有效期每次访问仍要校验。
 */
@Component
@RequiredArgsConstructor
public class ShortlinkCache {
    private static final Duration MAX_TTL = Duration.ofMinutes(30);
    private static final long LOCK_TIMEOUT_SECONDS = 5;
    private static final String KEY_PREFIX = "infra:shortlink:link:";
    private static final String LOCK_PREFIX = "infra:shortlink:lock:";
    private final StringRedisTemplate redis;
    private final RedissonClient redisson;
    private final ShortlinkLinkMapper links;

    public record Target(String url, LocalDateTime expireTime, Integer usable) {
    }

    public Target find(Long namespaceId, String code) {
        Long tenantId = requireTenant();
        RLock lock = redisson.getReadWriteLock(lockKey(tenantId, namespaceId)).readLock();
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!locked) return load(namespaceId, code);
            String key = cacheKey(tenantId, namespaceId, code);
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                String[] parts = cached.split("\\n", 3);
                if (parts.length == 3) {
                    return new Target(parts[2], "-".equals(parts[1]) ? null : LocalDateTime.parse(parts[1]),
                            Integer.parseInt(parts[0]));
                }
                redis.delete(key);
            }
            Target target = load(namespaceId, code);
            if (target != null && target.usable() != null && target.usable() == ShortlinkRules.ENABLED) {
                Duration ttl = target.expireTime() == null ? MAX_TTL : Duration.between(LocalDateTime.now(), target.expireTime());
                if (ttl.compareTo(MAX_TTL) > 0) ttl = MAX_TTL;
                if (!ttl.isNegative() && !ttl.isZero()) {
                    redis.opsForValue().set(key, target.usable() + "\n"
                            + (target.expireTime() == null ? "-" : target.expireTime()) + "\n" + target.url(), ttl);
                }
            }
            return target;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return load(namespaceId, code);
        } catch (RuntimeException e) {
            // Redis故障时回源数据库，不依赖缓存判断短链是否有效。
            return load(namespaceId, code);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    /** 必须在事务内调用；Redis不可用则拒绝修改，防止恢复后读取已过时的缓存。 */
    public void invalidateBeforeUpdate(ShortlinkLink link) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("短链缓存失效必须在事务中执行");
        }
        Long tenantId = requireTenant();
        RLock lock = redisson.getReadWriteLock(lockKey(tenantId, link.getNamespaceId())).writeLock();
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!locked) throw new CommonException("短链正在访问，请稍后重试");
            redis.delete(cacheKey(tenantId, link.getNamespaceId(), link.getShortCode()));
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (lock.isHeldByCurrentThread()) lock.unlock();
                }
            });
            locked = false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CommonException("短链修改被中断");
        } catch (RuntimeException e) {
            if (e instanceof CommonException common) throw common;
            throw new CommonException("短链缓存服务不可用，暂不能修改");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    private Target load(Long namespaceId, String code) {
        ShortlinkLink link = links.selectOne(new LambdaQueryWrapper<ShortlinkLink>()
                .eq(ShortlinkLink::getNamespaceId, namespaceId).eq(ShortlinkLink::getShortCode, code));
        return link == null ? null : new Target(link.getTargetUrl(), link.getExpireTime(), link.getUsable());
    }

    private static Long requireTenant() {
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) throw new CommonException("缺少可信租户上下文");
        return tenantId;
    }

    private static String cacheKey(Long tenantId, Long namespaceId, String code) {
        return KEY_PREFIX + tenantId + ":" + namespaceId + ":" + code;
    }

    private static String lockKey(Long tenantId, Long namespaceId) {
        return LOCK_PREFIX + tenantId + ":" + namespaceId;
    }
}
