package com.github.yilers.web.jackson;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
//@Configuration
public class JacksonConfig {

//    @Bean
//    public HttpMessageConverters.ServerBuilder httpMessageConverters() {
//        return new HttpMessageConverters.ServerBuilder()
//                .jsonCustomizer(objectMapper -> {
//
//                    SimpleModule module = new SimpleModule();
//                    module.addSerializer(Long.class, ToStringSerializer.instance);
//                    module.addSerializer(Long.TYPE, ToStringSerializer.instance);
//
//                    objectMapper.registerModule(module);
//                });
//    }
}