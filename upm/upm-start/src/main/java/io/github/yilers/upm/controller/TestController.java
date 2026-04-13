package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.v7.core.util.RandomUtil;
import io.github.yilers.core.util.Result;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TestController {

    @SaIgnore
    @GetMapping("/testIO")
    @SneakyThrows
    public Result<?> testIO() {
        int num = RandomUtil.randomInt(1, 5);
        Thread.sleep(num);
        log.info("完成");
        return Result.ok();
    }
}
