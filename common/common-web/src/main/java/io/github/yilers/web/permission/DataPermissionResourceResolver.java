package io.github.yilers.web.permission;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 数据权限资源路径解析工具。
 *
 * @author hui.zhang
 */
@UtilityClass
public class DataPermissionResourceResolver {

    /**
     * 优先返回 Spring 实际匹配的路由模板，无法获取时回退到请求路径。
     *
     * @param request 当前请求
     * @return 路由模板或请求路径
     */
    public static String resolve(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? request.getServletPath() : pattern.toString();
    }
}
