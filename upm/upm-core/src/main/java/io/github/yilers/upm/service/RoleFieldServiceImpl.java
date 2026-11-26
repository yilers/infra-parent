package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.RoleField;
import io.github.yilers.upm.mapper.RoleFieldMapper;
import org.springframework.stereotype.Service;

/** 角色字段授权持久化实现。 */
@Service
public class RoleFieldServiceImpl extends ServiceImpl<RoleFieldMapper, RoleField> implements RoleFieldService { }
