package io.github.yilers.upm.handler;

import cn.hutool.v7.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Application;
import io.github.yilers.upm.request.ApplicationRequest;
import io.github.yilers.upm.service.ApplicationService;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ApplicationHandler {
    private final ApplicationService applicationService;

    public List<Application> findAll() {
        return applicationService.list(Wrappers.<Application>lambdaQuery()
                .orderByAsc(Application::getSortNumber, Application::getId));
    }

    public Application findById(Long id) {
        Application application = applicationService.getById(id);
        if (application == null) {
            throw new CommonException("应用不存在");
        }
        return application;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(ApplicationRequest dto) {
        if (applicationService.findByCode(dto.getCode()) != null) {
            throw new CommonException("应用编码已存在");
        }
        Application application = BeanUtil.copyProperties(dto, Application.class);
        application.setId(null);
        application.setUsable(CommonConst.YES);
        application.setOperable(CommonConst.YES);
        application.setDeleted(CommonConst.NO);
        application.setVersion(1);
        applicationService.save(application);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(ApplicationRequest dto) {
        Application application = findOperable(dto.getId());
        if (dto.getCode() != null && !application.getCode().equals(dto.getCode())) {
            throw new CommonException("应用编码不可修改");
        }
        application.setName(dto.getName());
        application.setIcon(dto.getIcon());
        application.setDescription(dto.getDescription());
        application.setSortNumber(dto.getSortNumber());
        application.setVersion(dto.getVersion());
        if (!applicationService.updateById(application)) {
            throw new CommonException("更新失败 数据已经变更");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void usable(Long id) {
        Application application = findOperable(id);
        application.setUsable(CommonConst.YES.equals(application.getUsable()) ? CommonConst.NO : CommonConst.YES);
        if (!applicationService.updateById(application)) {
            throw new CommonException("更新失败 数据已经变更");
        }
    }

    private Application findOperable(Long id) {
        Application application = findById(id);
        if (!CommonConst.YES.equals(application.getOperable())) {
            throw new CommonException("内置应用不可操作");
        }
        return application;
    }
}
