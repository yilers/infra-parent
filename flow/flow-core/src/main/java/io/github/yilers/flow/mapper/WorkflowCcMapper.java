package io.github.yilers.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.yilers.flow.entity.WorkflowCc;

/** 自动抄送接收记录。唯一索引保证同一次节点通过不会重复写给同一人。 */
public interface WorkflowCcMapper extends BaseMapper<WorkflowCc> {
}
