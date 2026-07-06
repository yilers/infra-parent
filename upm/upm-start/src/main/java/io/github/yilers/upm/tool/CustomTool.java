package io.github.yilers.upm.tool;

import cn.hutool.core.date.LocalDateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class CustomTool {

    @Tool(name = "GetTimeTool", description = "获取当前时间")
    public String getTimeTool() {
        log.info("getTimeTool调用");
        return LocalDateTimeUtil.now().toString();
    }

//    @Tool(description = "根据用户ID查询用户信息")
//    public String getUserInfo(@ToolParam(description = "用户ID") Long userId) {
//        log.info("getUserInfo 调用 userId={}", userId);
//        // 模拟数据库
//        if (userId == 1L) {
//            return """
//                    用户信息：
//                    用户ID：1
//                    用户名：张辉
//                    年龄：26
//                    """;
//        }
//        return "用户不存在";
//    }
//
//    @Tool(description = "获取JVM内存信息")
//    public String getSystem() {
//        Runtime runtime = Runtime.getRuntime();
//
//        long total =
//                runtime.totalMemory()
//                        / 1024 / 1024;
//
//        long free =
//                runtime.freeMemory()
//                        / 1024 / 1024;
//
//        return """
//                JVM总内存：%s MB
//                JVM剩余内存：%s MB
//                """
//                .formatted(total, free);
//    }
//
//    @Tool(description = "获取天气")
//    public String getWeather(@ToolParam(description = "城市") String city, @ToolParam(description = "日期") String time) {
//        log.info("getWeather 调用 city={}", city);
//        return "城市：" + city + " 时间：" + time + " 天气：晴朗 28-35摄氏度";
//    }

    @Tool(name = "GetSupplierBalance", description = "获取供货商余额")
    public String getSupplierBalance(@ToolParam(description = "供货商ID") Long supplierId) {
        log.info("getSupplierBalance 调用 supplierId={}", supplierId);
        return "供货商ID：" + supplierId + " 余额：100000.00元";
    }

    @Tool(name= "GetSupplierOrderList", description = "获取供货商订单过去一年数据 返回List<Map<String, Object>>")
    public List<Map<String, Object>> getSupplierOrderList(@ToolParam(description = "供货商ID") Long supplierId) {
        // 模拟一年数据 每天一条 包含日期 销售单量 销售金额
        List<Map<String, Object>> list = new ArrayList<>();
        log.info("getSupplierOrderList 调用 supplierId={}", supplierId);
        for (int i = 0; i < 365; i++) {
            // 从2025-01-01 到2025-12-31 单量随机 金额随机
            Map<String, Object> map = Map.of(
                    "date", LocalDateTimeUtil.now().minusDays(i).toString(),
                    "orderCount", (int) (Math.random() * 100),
                    "orderAmount", String.format("%.2f", Math.random() * 10000)
            );
            list.add(map);
        }
        return list;
    }
}
