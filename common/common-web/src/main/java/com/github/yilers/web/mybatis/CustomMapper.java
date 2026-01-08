package com.github.yilers.web.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

public interface CustomMapper<T> extends BaseMapper<T> {

    Integer insertBatchSomeColumn(@Param("list") Collection<T> batchList);

}
