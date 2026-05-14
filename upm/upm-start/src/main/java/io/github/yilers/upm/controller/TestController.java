package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.v7.core.util.RandomUtil;
import cn.hutool.v7.http.HttpUtil;
import io.github.yilers.core.util.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Slf4j
@RestController
@Tag(name = "测试")
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


    @SaIgnore
    @GetMapping("/testRetry")
    @SneakyThrows
    public Result<?> testRetry() {
        RetryTemplate retryTemplate = new RetryTemplate();
        RetryPolicy build = RetryPolicy.builder().maxRetries(2).delay(Duration.ofSeconds(3)).build();
        retryTemplate.setRetryPolicy(build);
        String result = retryTemplate.execute(() -> {
            log.info("开始执行");
            throw new RuntimeException("发生异常");
        });

        log.info("完成");
        return Result.ok();
    }


    @SaIgnore
    @GetMapping("/testLimit")
    @ConcurrencyLimit(limit = 1, policy = ConcurrencyLimit.ThrottlePolicy.REJECT, limitString = "")
    public Result<?> testLimit() throws Exception {
        System.out.println(1);
        Thread.sleep(5000L);
        return Result.ok();
    }

    @SaIgnore
    @GetMapping("/testConcurrencyLimit")
    public void testConcurrencyLimit() throws Exception {

        for (int i = 0; i < 10; i++) {
            ThreadUtil.execAsync(() -> {
                String s = HttpUtil.get("http://192.168.0.131:9000/testLimit");
                System.out.println(s);
            }, false);
        }
    }
}
