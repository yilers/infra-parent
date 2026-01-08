package com.github.yilers.redisson;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LockMetadataCache {

    private static final ConcurrentMap<Method, LockDefinition> CACHE = new ConcurrentHashMap<>();

    public static LockDefinition getLockDefinition(Method method) {
        return CACHE.computeIfAbsent(method, m -> {
            DistributedLock annotation = m.getAnnotation(DistributedLock.class);
            if (annotation == null) {
                return null;
            }
            return new LockDefinition(annotation.key(), annotation.leaseTime(), annotation.waitTime());
        });
    }
}
