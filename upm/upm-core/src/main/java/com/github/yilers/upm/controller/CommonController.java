package com.github.yilers.upm.controller;


import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.yilers.core.util.Result;
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
public class CommonController {
    private final RequestMappingHandlerMapping handlerMapping;
    public CommonController(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @ApiOperationSupport(order = 1)
    @Operation(summary = "查询所有可以授权的接口")
    @PostMapping(value = "/findAllInterface")
    public Result<List<String>> findAllInterface() {
        List<String> permissionResult = new ArrayList<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        handlerMethods.forEach((info, method) -> {
            SaCheckPermission saCheckPermission = method.getMethod().getAnnotation(SaCheckPermission.class);
            if (saCheckPermission != null) {
                // 只用新版 PathPatterns
                List<String> paths = new ArrayList<>();
                PathPatternsRequestCondition pathPatternsCondition = info.getPathPatternsCondition();
                if (pathPatternsCondition != null) {
                    pathPatternsCondition.getPatterns().forEach(pattern -> {
                        paths.add(pattern.getPatternString());
                    });
                }
                if (CollectionUtil.isNotEmpty(paths)) {
                    String apiName = "未定义接口名称";
                    Operation operation = method.getMethod().getAnnotation(Operation.class);
                    if (ObjectUtil.isNotEmpty(operation) && StrUtil.isNotBlank(operation.summary())) {
                        apiName = operation.summary();
                    }
                    String nm = StrUtil.BRACKET_START + apiName + StrUtil.BRACKET_END;
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
