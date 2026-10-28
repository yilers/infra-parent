package io.github.yilers.upm.credential;

import io.github.yilers.auth.api.ApiPermission;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

/** 从当前进程的机器接口注解读取权限目录；微服务部署不能据此扫描其他进程。 */
@Component
public class ApiPermissionCatalog {
    private final RequestMappingHandlerMapping mappings;

    public ApiPermissionCatalog(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings) {
        this.mappings = mappings;
    }

    public record Capability(String code, String name, String group) {
    }

    public List<Capability> list() {
        Map<String, Capability> result = new TreeMap<>();
        mappings.getHandlerMethods().forEach((mapping, method) -> {
            ApiPermission annotation = method.getMethodAnnotation(ApiPermission.class);
            if (annotation != null) {
                Capability value = new Capability(annotation.code(), annotation.name(), annotation.group());
                Capability previous = result.putIfAbsent(value.code(), value);
                if (previous != null && !previous.equals(value)) {
                    throw new IllegalStateException("系统调用权限code重复且声明不一致：" + value.code());
                }
            }
        });
        return List.copyOf(result.values());
    }
}
