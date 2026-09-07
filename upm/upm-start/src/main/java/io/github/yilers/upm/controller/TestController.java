package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.v7.core.util.RandomUtil;
import cn.hutool.v7.http.HttpUtil;
import io.github.yilers.core.util.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

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
        return Result.ok("住址");
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


    private RestClient restClient;
    @Resource
    private ObjectMapper objectMapper;
    @SaIgnore
    @PostMapping("testOcr")
    public String testOcr(@RequestPart("file") MultipartFile file) throws IOException {
        final String OLLAMA_URL =
                "http://172.16.10.128:11434";
        final String MODEL =
                "hf.co/PaddlePaddle/PaddleOCR-VL-1.6-GGUF:latest";
        if (restClient == null) {
            restClient = RestClient.builder()
                    .baseUrl(OLLAMA_URL)
                    .build();
        }
        // 1. 图片转 Base64
        String base64 = Base64.getEncoder()
                .encodeToString(file.getBytes());

        // 2. 构造 Ollama 请求
        Map<String, Object> request = Map.of(
                "model", MODEL,
                "messages", new Object[]{
                        Map.of(
                                "role", "user",
                                "content", "OCR:",
                                "images", new String[]{base64}
                        )
                },
                "stream", false
        );

        // 3. 调用 Ollama
        String response = restClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

        // 4. 解析 response.message.content
        JsonNode root = objectMapper.readTree(response);
        System.out.println(root);
        return root.path("message")
                .path("content")
                .asString();
    }

    @SaIgnore
    @GetMapping("/testText")
    public String testText() {
        return "住址：xxxxx";
    }
}
