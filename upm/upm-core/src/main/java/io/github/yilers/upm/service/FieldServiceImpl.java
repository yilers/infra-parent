package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.Field;
import io.github.yilers.upm.mapper.FieldMapper;
import org.springframework.stereotype.Service;

/** 字段目录持久化实现。 */
@Service
public class FieldServiceImpl extends ServiceImpl<FieldMapper, Field> implements FieldService { }
