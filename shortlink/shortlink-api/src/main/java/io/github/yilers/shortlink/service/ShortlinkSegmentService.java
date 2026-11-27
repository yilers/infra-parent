package io.github.yilers.shortlink.service;

/** 短码序号的号段分配契约。 */
public interface ShortlinkSegmentService {
    /** 为当前租户的命名空间分配一个已提交的号段。 */
    Segment allocate(Long namespaceId, int length);

    /** 号段的起止序号，包含两端。 */
    record Segment(long first, long last) {}
}
