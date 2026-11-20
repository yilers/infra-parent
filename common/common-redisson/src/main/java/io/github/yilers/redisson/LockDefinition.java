package io.github.yilers.redisson;

import lombok.Data;
import java.util.concurrent.TimeUnit;

@Data
public class LockDefinition {
    private final String key;
    private final long lockTime;
    private final TimeUnit lockTimeUnit;
    private final long waitTimeout;
    private final TimeUnit waitTimeoutUnit;

    public LockDefinition(String key, long lockTime, TimeUnit lockTimeUnit, long waitTimeout, TimeUnit waitTimeoutUnit) {
        this.key = key;
        this.lockTime = lockTime;
        this.lockTimeUnit = lockTimeUnit;
        this.waitTimeout = waitTimeout;
        this.waitTimeoutUnit = waitTimeoutUnit;
    }
}
