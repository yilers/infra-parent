package io.github.yilers.upm.controller;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.github.yilers.core.util.Result;
import io.github.yilers.web.permission.DataPermissionResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/common")
@Tag(name = "公共")
@ApiSupport(order = 20, author = "yilers")
public class CommonController {
    private final RequestMappingHandlerMapping handlerMapping;
    public CommonController(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @ApiOperationSupport(order = 1)
    @Operation(summary = "查询支持用户数据权限配置的接口")
    @PostMapping(value = "/findAllInterface")
    public Result<List<String>> findAllInterface() {
        List<String> permissionResult = new ArrayList<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        handlerMethods.forEach((info, method) -> {
            DataPermissionResource resource = method.getMethod().getAnnotation(DataPermissionResource.class);
            if (resource != null) {
                // 只用新版 PathPatterns
                List<String> paths = new ArrayList<>();
                PathPatternsRequestCondition pathPatternsCondition = info.getPathPatternsCondition();
                if (pathPatternsCondition != null) {
                    pathPatternsCondition.getPatterns().forEach(pattern -> {
                        paths.add(pattern.getPatternString());
                    });
                }
                if (CollectionUtil.isNotEmpty(paths)) {
                    String nm = StrUtil.BRACKET_START + resource.name() + StrUtil.BRACKET_END;
                    paths.forEach(pt -> permissionResult.add(pt + nm));
                }
            }
        });

        List<String> list = CollectionUtil.sortByPinyin(permissionResult.stream().filter(api ->
                !api.startsWith("/" + StrUtil.BRACKET_START)
                        && !api.startsWith("/error")
                        && !api.contains("/api-docs")
                        && !api.contains("/swagger")).collect(Collectors.toList()));
        return Result.ok(list);
    }
}
