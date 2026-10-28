package io.github.yilers.shortlink.service;

import io.github.yilers.shortlink.request.ShortlinkCreateRequest;
import io.github.yilers.shortlink.request.ShortlinkBatchRequest;
import io.github.yilers.shortlink.request.ShortlinkRenewRequest;
import io.github.yilers.shortlink.response.ShortlinkResponse;
import java.util.List;

/**
 * 短链本地调用契约。调用方必须预先建立可信租户上下文；不要求HTTP签名或登录用户。
 * 批量创建整批事务回滚，最多1000条；幂等requestId在同一配置内唯一。
 */
public interface ShortlinkService {
    ShortlinkResponse create(ShortlinkCreateRequest request);
    List<ShortlinkResponse> createBatch(List<ShortlinkCreateRequest> requests);
    List<ShortlinkResponse> createBatch(ShortlinkBatchRequest request);
    ShortlinkResponse find(Long configId, String requestId);
    void renew(ShortlinkRenewRequest request);
    void disable(Long id, Integer version);
    void enable(Long id, Integer version);
    void delete(Long id, Integer version);
}
