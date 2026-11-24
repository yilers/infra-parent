package io.github.yilers.web.util;

import cn.hutool.v7.core.io.resource.ResourceUtil;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

@Slf4j
@UtilityClass
public class Ip2RegionUtil {
    /** 文件查询器数量固定，全部借出时由官方查询池等待可用查询器。 */
    private static final int IPV6_SEARCHERS = 15;

    private static final Ip2Region ip2Region = initialize();

    /**
     * IPv4全内存查询，IPv6不缓存数据库和向量索引。
     * JAR内资源不能直接进行随机文件读取，因此IPv6以流式复制方式落到进程独享的临时文件。
     * 两个输入流加载完即关闭，IPv6文件句柄在JVM正常退出时统一关闭，再删除临时文件。
     */
    private Ip2Region initialize() {
        Path ipv6File = null;
        Ip2Region service = null;
        try (InputStream stream4 = ResourceUtil.getStream("ip2region_v4.xdb");
             InputStream stream6 = ResourceUtil.getStream("ip2region_v6.xdb")) {
            Config v4Config = Config.custom()
                    .setCachePolicy(Config.BufferCache)
                    .setXdbInputStream(stream4)
                    .asV4();

            ipv6File = Files.createTempFile("infra-ip2region-v6-", ".xdb");
            Files.copy(stream6, ipv6File, StandardCopyOption.REPLACE_EXISTING);
            Config v6Config = Config.custom()
                    .setCachePolicy(Config.NoCache)
                    .setSearchers(IPV6_SEARCHERS)
                    .setXdbFile(ipv6File.toFile())
                    .asV6();
            service = Ip2Region.create(v4Config, v6Config);

            Ip2Region initializedService = service;
            Path initializedFile = ipv6File;
            Runtime.getRuntime().addShutdownHook(new Thread(
                    () -> closeResources(initializedService, initializedFile), "ip2region-shutdown"));
            return service;
        } catch (Exception e) {
            // 初始化失败也不遗留已经打开的服务和已复制的临时文件。
            closeResources(service, ipv6File);
            throw new IllegalStateException("IP归属地数据库初始化失败", e);
        }
    }

    /** 关闭查询池后再删除临时数据库，避免文件仍被查询器持有。 */
    private void closeResources(Ip2Region service, Path ipv6File) {
        if (service != null) {
            try {
                service.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("关闭IP归属地查询服务时被中断", e);
            }
        }
        if (ipv6File != null) {
            try {
                Files.deleteIfExists(ipv6File);
            } catch (Exception e) {
                log.warn("清理IPv6临时数据库失败：{}", ipv6File, e);
            }
        }
    }

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
