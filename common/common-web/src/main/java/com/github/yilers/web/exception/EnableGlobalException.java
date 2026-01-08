package com.github.yilers.web.exception;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 *
 * @author zhanghui
 * @date 2023/8/2 10:33
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Configuration
@Import(GlobalExceptionHandler.class)
public @interface EnableGlobalException {

}
