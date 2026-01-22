package io.github.yilers.auth;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 鉴权
 * @author zhanghui
 * @since  2023/8/2 10:15
 */

@Slf4j
//@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Autowired(required = false)
    private IgnoreProperties ignoreProperties;

    /**
     * 注册 Sa-Token 拦截器，打开注解式鉴权功能
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> url = ignoreProperties.getUrl();
        url.add("/**/favicon.ico");
        url.add("/**/doc.html");
        url.add("/**/css/**");
        url.add("/**/js/**");
        url.add("/**/v3/**");
        url.add("/**/public/**");
        url.add("/error");
        url.add("/actuator/**");
        url.add("/instances/**");
        url.add("/admin/**");
        url.add("/**/*.js");
        url.add("/**/*.css");
        url.add("/**/*.woff");
        url.add("/**/*.ttf");
        url.add("/static/**");
        url.add("/applications");
        url.add("/index.html");
        // 注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry.addInterceptor(new SaInterceptor(handler ->
                StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(ignoreProperties.getUrl());
        log.info("SaTokenConfig拦截器注入成功");
    }

}
