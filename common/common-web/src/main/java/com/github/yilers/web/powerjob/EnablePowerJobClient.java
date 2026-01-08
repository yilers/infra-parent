package com.github.yilers.web.powerjob;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Configuration
@Import({PowerJobClientConfig.class})
public @interface EnablePowerJobClient {
}
