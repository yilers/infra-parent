package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.yilers.upm.entity.Position;
import io.github.yilers.upm.request.PositionRequest;

import java.io.Serializable;

public interface PositionService extends IService<Position> {
    void savePosition(PositionRequest dto);

    void updatePosition(PositionRequest dto);

    Position findByCode(String code);

    void deleteById(Serializable id);
}
