package io.github.yilers.shortlink;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import io.github.yilers.auth.api.ApiCallerContext;
import io.github.yilers.shortlink.entity.*;
import io.github.yilers.shortlink.mapper.*;
import io.github.yilers.shortlink.request.ShortlinkCreateRequest;
import io.github.yilers.shortlink.request.ShortlinkBatchRequest;
import io.github.yilers.shortlink.service.*;
import io.github.yilers.shortlink.support.*;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.mybatis.MyMetaObjectHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** 在隔离的H2库验证号段事务、幂等、并发和租户授权，不触碰开发库。 */
@SpringJUnitConfig(ShortlinkIntegrationTest.Config.class)
class ShortlinkIntegrationTest {
    @Autowired ShortlinkService service;
    @Autowired ShortlinkConfigMapper configs;
    @Autowired ShortlinkDomainMapper domains;
    @Autowired ShortlinkNamespaceMapper namespaces;
    @Autowired ShortlinkSequenceMapper sequences;
    @Autowired ShortlinkConfigGrantMapper grants;
    @Autowired JdbcTemplate jdbc;
    @Autowired ShortlinkManagementService management;
    @Autowired NamespaceKeyCipher namespaceKeys;
    @Autowired ShortlinkPermutation permutation;
    Long configId;
    Long namespaceId;
    Long domainId;

    @BeforeEach
    void setup() {
        RequestContextHolder.setTenantId(1L);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ShortlinkConfig config = new ShortlinkConfig();
        config.setName("测试"); config.setCode(suffix); config.setTargetType("URL"); init(config); configs.insert(config);
        ShortlinkDomain domain = new ShortlinkDomain();
        domain.setHost(suffix + ".example.com"); domain.setScheme("https"); domain.setPublicPort(9000);
        init(domain); domains.insert(domain);
        ShortlinkNamespace namespace = new ShortlinkNamespace();
        namespace.setConfigId(config.getId()); namespace.setDomainId(domain.getId()); namespace.setPathCode("a");
        namespace.setGenerateType("AUTO"); namespace.setCodeLength(6); init(namespace); namespaces.insert(namespace);
        sequences.initialize(namespace.getId(), 1L, 1000);
        configId = config.getId(); namespaceId = namespace.getId(); domainId = domain.getId();
    }

    @AfterEach
    void cleanup() { ApiCallerContext.clear(); RequestContextHolder.clear(); }

    private static void init(io.github.yilers.api.base.BaseAllColumnDomain<?> entity) {
        entity.setUsable(1); entity.setOperable(1); entity.setDeleted(0); entity.setVersion(1);
    }

    private ShortlinkCreateRequest request(String id, String target) {
        ShortlinkCreateRequest value = new ShortlinkCreateRequest();
        value.setNamespaceId(namespaceId); value.setRequestId(id); value.setTargetUrl(target);
        return value;
    }

    @Test
    void retriesAreIdempotentButConflictingPayloadsAndTenantsAreRejected() {
        var original = service.create(request("same", "https://example.com/a"));
        assertTrue(original.shortUrl().contains(":9000/a/"));
        assertEquals(original.id(), service.create(request("same", "https://example.com/a")).id());
        assertThrows(CommonException.class, () -> service.create(request("same", "https://example.com/b")));
        RequestContextHolder.setTenantId(2L);
        assertThrows(CommonException.class, () -> service.create(request("other", "https://example.com/a")));
    }

    @Test
    void failedBatchRollsBackLinksButNeverReusesAllocatedNumbers() {
        var good = request("good", "https://example.com/a");
        var bad = request("bad", "not-a-url");
        assertThrows(CommonException.class, () -> service.createBatch(List.of(good, bad)));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM shortlink_link WHERE config_id=?", Integer.class, configId));
        assertEquals(1000L, jdbc.queryForObject("SELECT max_allocated_id FROM shortlink_sequence WHERE namespace_id=?", Long.class, namespaceId));
        var result = service.create(good);
        assertEquals("000002", result.code());
    }

    @Test
    void batchRetriesAreAtomicAndRequireIdenticalOrderedContent() {
        var batch = new ShortlinkBatchRequest();
        batch.setBatchId("batch-test");
        batch.setNamespaceId(namespaceId);
        var first = new ShortlinkBatchRequest.Item();
        first.setBusinessId("one");
        first.setTargetUrl("https://example.com/one");
        var second = new ShortlinkBatchRequest.Item();
        second.setTargetUrl("https://example.com/two");
        batch.setItems(List.of(first, second));
        var created = service.createBatch(batch);
        var repeated = service.createBatch(batch);
        assertEquals(created, repeated);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM shortlink_link WHERE config_id=? AND batch_id=?", Integer.class, configId, "batch-test"));
        first.setTargetUrl("https://example.com/changed");
        assertThrows(CommonException.class, () -> service.createBatch(batch));
        first.setTargetUrl("https://example.com/one");
        batch.setItems(List.of(second, first));
        assertThrows(CommonException.class, () -> service.createBatch(batch));
        batch.setBatchId("batch-failed");
        second.setTargetUrl("invalid-url");
        assertThrows(CommonException.class, () -> service.createBatch(batch));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM shortlink_batch WHERE config_id=? AND batch_id=?", Integer.class, configId, "batch-failed"));
    }

    @Test
    void concurrentRequestsNeverProduceDuplicateCodes() throws Exception {
        try (var executor = Executors.newFixedThreadPool(6)) {
            List<Callable<String>> work = IntStream.range(0, 24).<Callable<String>>mapToObj(i -> () -> {
                RequestContextHolder.setTenantId(1L);
                try { return service.create(request("concurrent-" + i, "https://example.com/" + i)).code(); }
                finally { RequestContextHolder.clear(); }
            }).toList();
            List<Future<String>> results = executor.invokeAll(work);
            Set<String> codes = new HashSet<>();
            for (Future<String> result : results) codes.add(result.get());
            assertEquals(24, codes.size());
        }
    }

    @Test
    void systemCallerNeedsAnExplicitConfigGrant() {
        ApiCallerContext.set(new ApiCallerContext.Caller(33L, 1L));
        assertThrows(CommonException.class, () -> service.create(request("grant", "https://example.com")));
        ShortlinkConfigGrant grant = new ShortlinkConfigGrant();
        grant.setConfigId(configId); grant.setCredentialId(33L); init(grant); grants.insert(grant);
        assertNotNull(service.create(request("grant", "https://example.com")).id());
    }

    @Test
    void disabledLinksCanBeReenabledAndDeletedIdentifiersCannotBeReused() {
        var original = service.create(request("lifecycle", "https://example.com"));
        service.disable(original.id(), original.version());
        var disabled = service.find(configId, "lifecycle");
        assertEquals(0, disabled.usable());
        service.enable(disabled.id(), disabled.version());
        var enabled = service.find(configId, "lifecycle");
        assertEquals(1, enabled.usable());
        service.delete(enabled.id(), enabled.version());
        assertThrows(CommonException.class, () -> service.find(configId, "lifecycle"));
        assertThrows(CommonException.class, () -> service.create(request("lifecycle", "https://example.com")));
    }

    @Test
    void newNamespaceUsesImmutableEncryptedKeyAndDoesNotChangeLegacyMappings() {
        var path = new io.github.yilers.shortlink.request.ShortlinkNamespaceRequest();
        path.setConfigId(configId);
        path.setDomainId(domainId);
        path.setPathCode("b");
        path.setGenerateType("AUTO");
        path.setCodeLength(6);
        Long newId = management.saveNamespace(path);
        ShortlinkNamespace saved = namespaces.selectById(newId);
        assertEquals(ShortlinkRules.ALGORITHM_FF1_V1, saved.getCodeAlgorithm());
        assertNotNull(saved.getCodeKeyCipher());
        assertEquals(32, namespaceKeys.decryptKey(saved.getCodeKeyCipher()).length);
        assertFalse(tools.jackson.databind.json.JsonMapper.builder().build()
                .writeValueAsString(saved).contains(saved.getCodeKeyCipher()));

        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            ShortlinkCreateRequest value = request("permuted-" + i, "https://example.com");
            value.setNamespaceId(newId);
            String code = service.create(value).code();
            if (i == 0) {
                assertEquals(permutation.permute(1, 6, namespaceKeys.decryptKey(saved.getCodeKeyCipher())), code);
            }
            codes.add(code);
        }
        assertEquals(20, codes.size());
        assertTrue(codes.stream().allMatch(code -> code.matches("[0-9A-Za-z]{6}")));
        assertEquals("000001", service.create(request("legacy", "https://example.com")).code());

        // 即使调用者直接尝试更新，映射规则和密钥也不允许被updateById覆盖。
        String oldCipher = saved.getCodeKeyCipher();
        saved.setCodeAlgorithm(ShortlinkRules.ALGORITHM_SEQUENTIAL);
        saved.setCodeKeyCipher("tampered");
        namespaces.updateById(saved);
        ShortlinkNamespace reloaded = namespaces.selectById(newId);
        assertEquals(ShortlinkRules.ALGORITHM_FF1_V1, reloaded.getCodeAlgorithm());
        assertEquals(oldCipher, reloaded.getCodeKeyCipher());
    }

    @Configuration
    @EnableTransactionManagement
    @MapperScan("io.github.yilers.shortlink.mapper")
    @Import({ShortlinkServiceImpl.class, ShortlinkSegmentService.class, ShortlinkCodeGenerator.class,
            ShortlinkManagementService.class, ShortlinkPermutation.class})
    static class Config {
        @Bean DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                    .setName("shortlink-test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
                    .addScript("shortlink-test-schema.sql").build();
        }
        @Bean JdbcTemplate jdbc(DataSource dataSource) { return new JdbcTemplate(dataSource); }
        @Bean DataSourceTransactionManager transactionManager(DataSource dataSource) { return new DataSourceTransactionManager(dataSource); }
        @Bean LocalValidatorFactoryBean validator() { return new LocalValidatorFactoryBean(); }
        @Bean ShortlinkCache cache() { return mock(ShortlinkCache.class); }
        @Bean NamespaceKeyCipher namespaceKeys() {
            return new NamespaceKeyCipher(Base64.getEncoder().encodeToString(new byte[32]));
        }
        @Bean MybatisPlusInterceptor interceptor() {
            var result = new MybatisPlusInterceptor();
            result.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
                @Override public Expression getTenantId() {
                    return new LongValue(Objects.requireNonNull(RequestContextHolder.getTenantId()));
                }
            }));
            result.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
            return result;
        }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource source, MybatisPlusInterceptor interceptor) throws Exception {
            var factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(source);
            factory.setGlobalConfig(new GlobalConfig().setMetaObjectHandler(new MyMetaObjectHandler()));
            factory.setPlugins(interceptor);
            var configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            factory.setConfiguration(configuration);
            return factory.getObject();
        }
    }
}
