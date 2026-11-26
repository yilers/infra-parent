package io.github.yilers.upm.permission;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import io.github.yilers.upm.handler.UserHandler;
import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.permission.DataPermission;
import io.github.yilers.web.permission.DataPermissionAspect;
import io.github.yilers.web.permission.DataPermissionContextHolder;
import io.github.yilers.web.permission.DataScope;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.support.StaticApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 通过真实MyBatis插件和H2执行SQL，覆盖默认权限、自定义范围、别名及上下文生命周期。 */
class CustomDataPermissionHandlerTest {

    private SqlSession session;
    private ScopeMapper mapper;
    private UserHandler userHandler;
    private StaticApplicationContext applicationContext;
    private MockedStatic<SpringUtil> spring;
    private MockedStatic<StpUtil> login;

    @BeforeEach
    void setUp() throws Exception {
        userHandler = mock(UserHandler.class);
        applicationContext = new StaticApplicationContext();
        applicationContext.getBeanFactory().registerSingleton("scope", new ScopeBean());
        spring = mockStatic(SpringUtil.class);
        spring.when(() -> SpringUtil.getBean(UserHandler.class)).thenReturn(userHandler);
        spring.when(SpringUtil::getApplicationContext).thenReturn(applicationContext);
        login = mockStatic(StpUtil.class);
        login.when(StpUtil::getLoginIdAsLong).thenReturn(99L);

        var datasource = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:scope_" + UUID.randomUUID(), "sa", "");
        var configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), datasource));
        configuration.setLocalCacheScope(org.apache.ibatis.session.LocalCacheScope.STATEMENT);
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new CustomDataPermissionHandler()));
        configuration.addInterceptor(interceptor);
        configuration.addMapper(ScopeMapper.class);
        session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession();
        try (var statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE scope_order(id BIGINT, dept_id BIGINT, create_id BIGINT, project_id BIGINT)");
            statement.execute("INSERT INTO scope_order VALUES (1,10,99,100),(2,20,100,200),(3,30,99,300)");
            statement.execute("CREATE TABLE scope_dept(id BIGINT)");
            statement.execute("INSERT INTO scope_dept VALUES (10),(20),(30)");
        }
        mapper = session.getMapper(ScopeMapper.class);
    }

    @AfterEach
    void tearDown() {
        DataPermissionContextHolder.clear();
        if (session != null) session.close();
        if (login != null) login.close();
        if (spring != null) spring.close();
        if (applicationContext != null) applicationContext.close();
    }

    @Test
    void preservesDefaultAllDepartmentAndSelfScopesWithAndWithoutAliases() {
        when(userHandler.findDataScopeByUserId(99L)).thenReturn(null);
        assertEquals(List.of(1L, 2L, 3L), mapper.defaultAliased());
        assertEquals(List.of(1L, 2L, 3L), mapper.defaultUnaliased());

        // 清理MyBatis会话查询缓存，确保每次确实执行插件和数据库查询。
        session.clearCache();
        when(userHandler.findDataScopeByUserId(99L)).thenReturn(List.of(10L));
        assertEquals(List.of(1L), mapper.defaultAliased());
        assertEquals(List.of(1L), mapper.defaultUnaliased());
        assertEquals(List.of(1L), mapper.defaultJoined());

        session.clearCache();
        when(userHandler.findDataScopeByUserId(99L)).thenReturn(List.of(-1L));
        assertEquals(List.of(1L, 3L), mapper.defaultAliased());
        assertEquals(List.of(1L, 3L), mapper.defaultUnaliased());
    }

    @Test
    void defaultEmptyScopeStillRejectsQuery() {
        when(userHandler.findDataScopeByUserId(99L)).thenReturn(List.of());
        assertThrows(RuntimeException.class, () -> mapper.defaultAliased());
    }

    @Test
    void mapperFallbackSupportsBeanAndStaticExpressions() {
        assertEquals(List.of(1L, 2L), mapper.customBean());
        assertEquals(List.of(1L, 3L), mapper.customCurrentUser());
        verifyNoInteractions(userHandler);
    }

    @Test
    void customScopeOverridesDefaultAndPersistsAcrossTwoSqlStatements() {
        var operations = proxy(new ScopeOperations(mapper));
        assertEquals(List.of(List.of(2L), List.of(2L)), operations.queryTwice(List.of(200L)));
        assertNull(DataPermissionContextHolder.get());
        verifyNoInteractions(userHandler);
    }

    @Test
    void defaultScopeAlsoWorksThroughAspectWithAliasedJoin() {
        when(userHandler.findDataScopeByUserId(99L)).thenReturn(List.of(20L));
        assertEquals(List.of(2L), proxy(new ScopeOperations(mapper)).defaultJoined());
        assertNull(DataPermissionContextHolder.get());
    }

    @Test
    void emptyCustomCollectionReturnsNoRowsRatherThanDefaultOrAllRows() {
        assertEquals(List.of(List.of(), List.of()), proxy(new ScopeOperations(mapper)).queryTwice(List.of()));
        verifyNoInteractions(userHandler);
    }

    @Test
    void invalidCustomResultsFailClosedAndClearContext() {
        var operations = proxy(new ScopeOperations(mapper));
        assertThrows(CommonException.class, () -> operations.queryTwice(null));
        assertThrows(CommonException.class, () -> operations.queryTwice(List.of("1) OR 1=1")));
        assertThrows(CommonException.class, operations::invalidType);
        assertNull(DataPermissionContextHolder.get());
        verifyNoInteractions(userHandler);
    }

    @Test
    void nestedCallRestoresOuterScopeAfterSuccessAndException() {
        var inner = proxy(new InnerOperations(mapper));
        var outer = proxy(new OuterOperations(mapper, inner));
        assertEquals(List.of(List.of(1L), List.of(2L), List.of(1L)), outer.query(List.of(100L)));
        assertEquals(List.of(1L), outer.queryAfterInnerFailure(List.of(100L)));
        assertNull(DataPermissionContextHolder.get());
    }

    @Test
    void customScopeHandlesDifferentAliasesOfSameTable() {
        assertEquals(List.of(2L), proxy(new ScopeOperations(mapper)).selfJoined(List.of(200L)));
    }

    @Test
    void interfaceAnnotationUsesParamAliasWithRealArguments() {
        var factory = new AspectJProxyFactory(new InterfaceOperations(mapper));
        factory.setProxyTargetClass(false);
        factory.addAdvisor(new DataPermissionAspect());
        InterfaceApi operations = factory.getProxy();
        assertEquals(List.of(2L), operations.query(List.of(200L)));
        assertNull(DataPermissionContextHolder.get());
    }

    @Test
    void dataScopeMatchesEachJoinedTableAndAppliesBothConditions() {
        var operations = proxy(new ScopeOperations(mapper));
        assertEquals(List.of(1L), operations.joinedScopes(List.of(100L, 200L), List.of(10L)));
        assertEquals(List.of(), operations.joinedScopes(List.of(200L), List.of(10L)));
        assertNull(DataPermissionContextHolder.get());
    }

    @Test
    void aliasedConditionsExecuteInMysqlAndPostgresModes() throws Exception {
        when(userHandler.findDataScopeByUserId(99L)).thenReturn(List.of(10L));
        for (String mode : new String[]{"MySQL", "PostgreSQL"}) {
            try (var statement = session.getConnection().createStatement()) {
                statement.execute("SET MODE " + mode);
            }
            assertEquals(List.of(1L), mapper.defaultJoined());
            assertEquals(List.of(1L, 2L), mapper.customBean());
            assertEquals(List.of(2L), proxy(new ScopeOperations(mapper)).selfJoined(List.of(200L)));
        }
    }

    private <T> T proxy(T target) {
        var factory = new AspectJProxyFactory(target);
        factory.addAdvisor(new DataPermissionAspect());
        return factory.getProxy();
    }

    public interface ScopeMapper {
        @Select("SELECT o.id FROM scope_order o ORDER BY o.id")
        @DataPermission(tableName = "scope_order")
        List<Long> defaultAliased();

        @Select("SELECT id FROM scope_order ORDER BY id")
        @DataPermission(tableName = "scope_order")
        List<Long> defaultUnaliased();

        @Select("SELECT o.id FROM scope_order o LEFT JOIN scope_dept d ON o.dept_id=d.id ORDER BY o.id")
        @DataPermission(tableName = "scope_order")
        List<Long> defaultJoined();

        @Select("SELECT o.id FROM scope_order o ORDER BY o.id")
        @DataPermission(tableName = "scope_order", columnName = "project_id", columnValue = "@scope.allowedIds()")
        List<Long> customBean();

        @Select("SELECT o.id FROM scope_order o ORDER BY o.id")
        @DataPermission(tableName = "scope_order", columnName = "create_id",
                columnValue = "{T(cn.dev33.satoken.stp.StpUtil).getLoginIdAsLong()}")
        List<Long> customCurrentUser();

        @Select("SELECT o.id FROM scope_order o ORDER BY o.id")
        List<Long> unannotatedAliased();

        @Select("SELECT x.id FROM scope_order x ORDER BY x.id")
        List<Long> unannotatedOtherAlias();

        // 若第二个别名未过滤，另外两行也能与project_id=200的a行关联，结果会错误地出现三行。
        @Select("SELECT b.id FROM scope_order a INNER JOIN scope_order b ON 1=1 ORDER BY b.id")
        List<Long> unannotatedSelfJoin();

        @Select("SELECT o.id FROM scope_order o JOIN scope_dept d ON o.dept_id=d.id ORDER BY o.id")
        List<Long> unannotatedJoined();
    }

    public static class ScopeBean {
        public List<Long> allowedIds() { return List.of(100L, 200L); }
    }

    public static class ScopeOperations {
        private final ScopeMapper mapper;
        public ScopeOperations(ScopeMapper mapper) { this.mapper = mapper; }

        @DataPermission(tableName = "scope_order", columnName = "project_id", columnValue = "#allowedIds")
        public List<List<Long>> queryTwice(List<?> allowedIds) {
            return List.of(mapper.unannotatedAliased(), mapper.unannotatedOtherAlias());
        }

        @DataPermission(tableName = "scope_order")
        public List<Long> defaultJoined() { return mapper.unannotatedJoined(); }

        @DataPermission(tableName = "scope_order", columnName = "project_id", columnValue = "'1 = 1'")
        public List<Long> invalidType() { return mapper.unannotatedAliased(); }

        @DataPermission(tableName = "scope_order", columnName = "project_id", columnValue = "#p0")
        public List<Long> selfJoined(List<Long> ids) { return mapper.unannotatedSelfJoin(); }

        @DataScope({
                @DataPermission(tableName = "scope_order", columnName = "project_id", columnValue = "#p0"),
                @DataPermission(tableName = "scope_dept", columnName = "id", columnValue = "#p1")
        })
        public List<Long> joinedScopes(List<Long> projectIds, List<Long> departmentIds) {
            return mapper.unannotatedJoined();
        }
    }

    public static class InnerOperations {
        private final ScopeMapper mapper;
        public InnerOperations(ScopeMapper mapper) { this.mapper = mapper; }

        @DataPermission(tableName = "scope_order", columnName = "project_id", columnValue = "{200}")
        public List<Long> query() { return mapper.unannotatedOtherAlias(); }

        @DataPermission(tableName = "scope_order", columnName = "project_id", columnValue = "{200}")
        public void fail() { throw new IllegalStateException("测试内层异常"); }
    }

    public static class OuterOperations {
        private final ScopeMapper mapper;
        private final InnerOperations inner;
        public OuterOperations(ScopeMapper mapper, InnerOperations inner) { this.mapper = mapper; this.inner = inner; }

        @DataPermission(tableName = "scope_order", columnName = "project_id", columnValue = "#arg0")
        public List<List<Long>> query(List<Long> ids) {
            var first = mapper.unannotatedAliased();
            var second = inner.query();
            return List.of(first, second, mapper.unannotatedAliased());
        }

        @DataPermission(tableName = "scope_order", columnName = "project_id", columnValue = "#a0")
        public List<Long> queryAfterInnerFailure(List<Long> ids) {
            try { inner.fail(); } catch (IllegalStateException expected) { }
            return mapper.unannotatedOtherAlias();
        }

    }

    public interface InterfaceApi {
        @DataPermission(tableName = "scope_order", columnName = "project_id", columnValue = "#projects")
        List<Long> query(@Param("projects") List<Long> ids);
    }

    public static class InterfaceOperations implements InterfaceApi {
        private final ScopeMapper mapper;
        public InterfaceOperations(ScopeMapper mapper) { this.mapper = mapper; }
        @Override public List<Long> query(List<Long> incoming) { return mapper.unannotatedAliased(); }
    }
}
