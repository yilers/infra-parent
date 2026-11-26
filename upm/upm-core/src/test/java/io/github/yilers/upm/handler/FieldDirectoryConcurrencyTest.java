package io.github.yilers.upm.handler;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.github.yilers.api.field.FieldDefinition;
import io.github.yilers.api.field.FieldMaskType;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Field;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.service.*;
import io.github.yilers.web.context.RequestContextHolder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 以真实事务和行锁验证无唯一索引时的并发登记；不替代MySQL/PostgreSQL实库验收。 */
class FieldDirectoryConcurrencyTest {
    @Test
    void concurrentSynchronizationsCreateOnlyOneActiveField() throws Exception {
        for (String mode : List.of("MySQL", "PostgreSQL")) {
            String url = "jdbc:h2:mem:field_sync_" + mode + ";MODE=" + mode + ";LOCK_TIMEOUT=5000";
            try (Connection database = DriverManager.getConnection(url)) {
                try (var statement = database.createStatement()) {
                    statement.execute("CREATE TABLE upm_tenant (id BIGINT PRIMARY KEY)");
                    statement.execute("INSERT INTO upm_tenant VALUES (1)");
                    statement.execute("CREATE TABLE upm_field (id BIGINT PRIMARY KEY, tenant_id BIGINT, resource_code VARCHAR(80), field_code VARCHAR(80), deleted INT)");
                    statement.execute("CREATE INDEX idx_field_resource ON upm_field (tenant_id,resource_code,field_code)");
                }
                var configuration = new MybatisConfiguration();
                TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "field-concurrency"), Tenant.class);
                TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "field-concurrency"), Field.class);
                ThreadLocal<Connection> transaction = new ThreadLocal<>();
                AtomicLong sequence = new AtomicLong();
                TenantService tenants = mock(TenantService.class);
                FieldService fields = mock(FieldService.class);
                when(tenants.getOne(org.mockito.ArgumentMatchers.<Wrapper<Tenant>>any())).thenAnswer(call -> {
                    Wrapper<Tenant> wrapper = call.getArgument(0);
                    assertTrue(wrapper.getSqlSegment().endsWith("FOR UPDATE"));
                    try (var statement = transaction.get().prepareStatement("SELECT id FROM upm_tenant WHERE id=1 FOR UPDATE");
                         var result = statement.executeQuery()) {
                        assertTrue(result.next());
                    }
                    Tenant tenant = new Tenant();
                    tenant.setId(1L);
                    tenant.setUsable(CommonConst.YES);
                    return tenant;
                });
                when(fields.getOne(org.mockito.ArgumentMatchers.<Wrapper<Field>>any())).thenAnswer(call -> {
                    Wrapper<Field> wrapper = call.getArgument(0);
                    assertTrue(wrapper.getSqlSegment().endsWith("FOR UPDATE"));
                    try (var statement = transaction.get().prepareStatement("SELECT id FROM upm_field WHERE tenant_id=1 AND resource_code='system:user' AND field_code='phone' AND deleted=0 FOR UPDATE");
                         var result = statement.executeQuery()) {
                        if (!result.next()) return null;
                        Field field = new Field();
                        field.setId(result.getLong(1));
                        return field;
                    }
                });
                when(fields.saveOrUpdate(any(Field.class))).thenAnswer(call -> {
                    Field field = call.getArgument(0);
                    if (field.getId() == null) {
                        try (var statement = transaction.get().prepareStatement("INSERT INTO upm_field VALUES (?,1,'system:user','phone',0)")) {
                            statement.setLong(1, sequence.incrementAndGet());
                            statement.executeUpdate();
                        }
                    }
                    return true;
                });
                var handler = new FieldPermissionHandler(fields, mock(RoleFieldService.class), mock(RoleService.class),
                        mock(UserRoleService.class), tenants, List.of(() -> List.of(
                        new FieldDefinition("system:user", "用户资料", "phone", "手机号", FieldMaskType.PHONE, true))));
                var ready = new CountDownLatch(2);
                var start = new CountDownLatch(1);
                try (var workers = Executors.newFixedThreadPool(2)) {
                    var results = java.util.stream.IntStream.range(0, 2).mapToObj(index -> workers.submit(() -> {
                        try (Connection connection = DriverManager.getConnection(url)) {
                            connection.setAutoCommit(false);
                            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                            transaction.set(connection);
                            RequestContextHolder.setTenantId(1L);
                            ready.countDown();
                            assertTrue(start.await(5, TimeUnit.SECONDS));
                            handler.synchronize();
                            connection.commit();
                        } finally {
                            transaction.remove();
                            RequestContextHolder.clear();
                        }
                        return null;
                    })).toList();
                    assertTrue(ready.await(5, TimeUnit.SECONDS));
                    start.countDown();
                    for (var result : results) result.get(10, TimeUnit.SECONDS);
                }
                try (var statement = database.createStatement();
                     var result = statement.executeQuery("SELECT COUNT(*) FROM upm_field WHERE deleted=0")) {
                    assertTrue(result.next());
                    assertEquals(1, result.getInt(1));
                }
            }
        }
    }
}
