package com.github.yilers.upm.service;

import cn.hutool.v7.extra.spring.cglib.CglibUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yilers.core.constant.CommonConst;
import com.github.yilers.upm.entity.Position;
import com.github.yilers.upm.mapper.PositionMapper;
import com.github.yilers.upm.request.PositionRequest;
import com.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

@Service
@RequiredArgsConstructor
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService {
    private final PositionMapper positionMapper;
    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePosition(PositionRequest dto) {
        Position position = findByCode(dto.getPositionCode());
        if (position != null) {
            throw new CommonException("职位编码已存在");
        }
        Position copy = CglibUtil.copy(dto, Position.class);
        copy.setOperable(CommonConst.YES);
        positionMapper.insert(copy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePosition(PositionRequest dto) {
        Position position = findByCode(dto.getPositionCode());
        if (position != null && !position.getId().equals(dto.getId())) {
            throw new CommonException("职位编码已存在");
        }
        Position copy = CglibUtil.copy(dto, Position.class);
        positionMapper.updateById(copy);
    }

    @Override
    public Position findByCode(String code) {
        LambdaQueryWrapper<Position> query = Wrappers.lambdaQuery(Position.class);
        query.eq(Position::getPositionCode, code);
        return positionMapper.selectOne(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Serializable id) {
        Position position = positionMapper.selectById(id);
        if (position == null) {
            throw new CommonException("职位不存在");
        }
        boolean exist = userService.existByPositionId(position.getId());
        if (exist) {
            throw new CommonException("职位下存在用户");
        }
        positionMapper.deleteById(id);
    }
}
