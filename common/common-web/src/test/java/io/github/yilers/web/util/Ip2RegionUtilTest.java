package io.github.yilers.web.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** 使用实际内嵌数据库，验证两种查询模式及IPv6查询池的并发访问。 */
class Ip2RegionUtilTest {

    @Test
    void searchesBothIpVersionsFromPackagedResources() {
        assertRegion(Ip2RegionUtil.region("113.92.157.29"));
        assertRegion(Ip2RegionUtil.region("240e:3b7:3272:d8d0:db09:c067:8d59:539e"));
    }

    @Test
    void supportsConcurrentFileBasedIpv6Queries() throws Exception {
        // 请求数超过查询池容量，验证借出和归还后仍能正常查询。
        try (var executor = Executors.newFixedThreadPool(20)) {
            var queries = new ArrayList<Callable<String>>();
            for (int i = 0; i < 40; i++) {
                queries.add(() -> Ip2RegionUtil.region("240e:3b7:3272:d8d0:db09:c067:8d59:539e"));
            }
            for (var result : executor.invokeAll(queries)) {
                assertRegion(result.get());
            }
        }
    }

    private void assertRegion(String region) {
        assertNotNull(region);
        assertFalse(region.isBlank());
        assertNotEquals("未知", region);
    }
}
