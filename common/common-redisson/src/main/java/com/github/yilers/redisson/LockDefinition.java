package com.github.yilers.redisson;

import lombok.Data;

@Data
public class LockDefinition {
    private final String key;
    private final Long leaseTime;
    private final Long waitTime;

    public LockDefinition(String key, long leaseTime, long waitTime) {
        this.key = key;
        this.leaseTime = leaseTime;
        this.waitTime = waitTime;
    }
}
