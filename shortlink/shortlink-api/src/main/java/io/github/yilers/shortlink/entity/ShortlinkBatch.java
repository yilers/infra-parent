package io.github.yilers.shortlink.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.yilers.api.base.BaseAllColumnDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 批次及完整请求摘要；记录保留以保证重试幂等。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shortlink_batch")
public class ShortlinkBatch extends BaseAllColumnDomain<ShortlinkBatch> {
    private Long configId;
    private String batchId;
    private String requestHash;
    private Integer itemCount;
}
