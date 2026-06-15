package io.github.yilers.ai.tool;

import cn.hutool.core.date.LocalDateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

@Slf4j
public class CustomTool {

    @Tool(description = "获取当前时间")
    public String getTimeTool() {
        log.info("getTimeTool调用");
        return LocalDateTimeUtil.now().toString();
    }

    @Tool(description = "根据用户ID查询用户信息")
    public String getUserInfo(@ToolParam(description = "用户ID") Long userId) {
        log.info("getUserInfo 调用 userId={}", userId);
        // 模拟数据库
        if (userId == 1L) {
            return """
                    用户信息：
                    用户ID：1
                    用户名：张辉
                    年龄：26
                    """;
        }
        return "用户不存在";
    }

    @Tool(description = "获取JVM内存信息")
    public String getSystem() {
        Runtime runtime = Runtime.getRuntime();

        long total =
                runtime.totalMemory()
                        / 1024 / 1024;

        long free =
                runtime.freeMemory()
                        / 1024 / 1024;

        return """
                JVM总内存：%s MB
                JVM剩余内存：%s MB
                """
                .formatted(total, free);
    }

    @Tool(description = "获取天气")
    public String getWeather(@ToolParam(description = "城市") String city, @ToolParam(description = "日期") String time) {
        log.info("getWeather 调用 city={}", city);
        return "城市：" + city + " 时间：" + time + " 天气：晴朗 28-35摄氏度";
    }
}
