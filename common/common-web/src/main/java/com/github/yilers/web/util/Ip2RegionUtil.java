package com.github.yilers.web.util;

import cn.hutool.v7.core.io.resource.ResourceUtil;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@UtilityClass
public class Ip2RegionUtil {
    Config v4Config;
    Config v6Config;
    Ip2Region ip2Region;
    static {
        try {
            InputStream stream4 = ResourceUtil.getStream("ip2region_v4.xdb");
            InputStream stream6 = ResourceUtil.getStream("ip2region_v6.xdb");
            v4Config = Config.custom()
                    .setCachePolicy(Config.BufferCache)     // 指定缓存策略:  NoCache / VIndexCache / BufferCache
                    .setSearchers(15)                       // 设置初始化的查询器数量
                    .setXdbInputStream(stream4)      // 设置 v4 xdb 文件的 inputstream 对象
                    // .setXdbFile(File)                    // 设置 v4 xdb File 对象
//            .setXdbPath("ip2region v4 xdb path")    // 设置 v4 xdb 文件的路径
                    .asV4();    // 指定为 v4 配置

            v6Config = Config.custom()
                    .setCachePolicy(Config.BufferCache)     // 指定缓存策略: NoCache / VIndexCache / BufferCache
                    .setSearchers(15)                       // 设置初始化的查询器数量
                     .setXdbInputStream(stream6)      // 设置 v6 xdb 文件的 inputstream 对象
                    // .setXdbFile(File)                    // 设置 v6 xdb File 对象
//                    .setXdbPath("ip2region v6 xdb path")    // 设置 v6 xdb 文件的路径
                    .asV6();    // 指定为 v6 配置
            ip2Region = Ip2Region.create(v4Config, v6Config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 备注：Xdb 三种初始化输入的优先级：XdbInputStream -> XdbFile -> XdbPath
    // setXdbInputStream 仅方便使用者从 jar 包中加载 xdb 文件内容，这时 cachePolicy 只能设置为 Config.BufferCache

    @SneakyThrows
    public String region(String ip) {
        String region = "";
        try {
            long sTime = System.nanoTime();
            region = ip2Region.search(ip);
            long cost = TimeUnit.NANOSECONDS.toMicros((long) (System.nanoTime() - sTime));
            log.info("ip:{},region:{},took:{}μs", ip, region, cost);
        } catch (Exception e) {
            if ("0:0:0:0:0:0:0:1".equals(ip)) {
                return "本机";
            } else {
                return "未知";
            }
        }
        return region;
    }
}
