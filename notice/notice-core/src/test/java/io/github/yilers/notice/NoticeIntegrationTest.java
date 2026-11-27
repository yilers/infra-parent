package io.github.yilers.notice;

import static org.junit.jupiter.api.Assertions.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;

import io.github.yilers.notice.config.*;
import io.github.yilers.notice.controller.NoticeInboxController;
import io.github.yilers.notice.controller.NoticeMessageController;
import io.github.yilers.notice.entity.*;
import io.github.yilers.notice.mapper.*;
import io.github.yilers.notice.model.*;
import io.github.yilers.notice.model.NoticeTypes.*;
import io.github.yilers.notice.request.*;
import io.github.yilers.notice.service.*;
import io.github.yilers.notice.support.*;
import io.github.yilers.notice.task.NoticeTaskDispatcher;
import io.github.yilers.notice.task.NoticeTasks;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;

import net.sf.jsqlparser.expression.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;

/** 真实SQL验证隔离、异步发布快照、个人阅读与SMTP失败重试；不连接实际SMTP或业务库。 */
class NoticeIntegrationTest {
    @AfterEach
    void clear() {
        RequestContextHolder.clear();
    }

    @Test
    void immediateMessageRunsOnlyAfterCommitWithoutScheduler() throws Exception {
        try (Fixture f = new Fixture("MySQL");
                var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TransactionalEventListenerFactory.class);
            context.registerBean(NoticeTaskDispatcher.class,
                    () -> new NoticeTaskDispatcher(f.session.getMapper(NoticeTaskMapper.class), f.tasks));
            context.refresh();
            f.events = context;
            f.login(1, 10);
            var request = new MessageSaveRequest(
                    null, null, "公告", "正文", "TEXT", "ANNOUNCEMENT",
                    new NoticeTarget(true, List.of(), List.of(), List.of(), false),
                    null, null, null, null, null);
            f.tx.executeWithoutResult(status -> {
                f.messages.send(request);
                status.setRollbackOnly();
            });

            Long id = f.tx.execute(status -> {
                Long saved = f.messages.send(request);
                assertEquals(PublishStatus.SCHEDULED.name(), f.messages.detail(saved).getPublishStatus());
                return saved;
            });
            assertNotNull(id);
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            while (!PublishStatus.PUBLISHED.name().equals(f.messages.detail(id).getPublishStatus())
                    && System.nanoTime() < deadline) {
                Thread.sleep(25);
            }
            assertEquals(PublishStatus.PUBLISHED.name(), f.messages.detail(id).getPublishStatus());
            assertEquals(1, f.inbox.page(null, null, 1, 20).total());
        }
    }

    @Test
    void rendersMarkdownDetailsAndKeepsEditableSource() throws Exception {
        try (Fixture f = new Fixture("MySQL")) {
            f.login(1, 10);
            String markdown = "# 维护公告\n\n**维护中**\n\n<span>正文</span>";
            Long id = f.messages.send(new MessageSaveRequest(
                    null, null, "维护公告", markdown, ContentType.MARKDOWN.name(), "ANNOUNCEMENT",
                    new NoticeTarget(true, List.of(), List.of(), List.of(), false),
                    null, null, null, null, null));
            f.tasks.execute();

            var notice = new NoticeMessageController(f.messages).detail(id).getData();
            assertEquals(markdown, notice.getContent());
            assertEquals(ContentType.MARKDOWN.name(), notice.getContentType());
            assertTrue(notice.getRenderedContent().contains("<h1>维护公告</h1>"));
            assertTrue(notice.getRenderedContent().contains("&lt;span&gt;正文&lt;/span&gt;"));

            Long recipientId = f.inbox.page(null, null, 1, 20).records().getFirst().id();
            var item = new NoticeInboxController(f.inbox).detail(recipientId).getData();
            assertEquals(ContentType.HTML.name(), item.contentType());
            assertEquals(notice.getRenderedContent(), item.content());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void publishesFrozenAudienceAndProtectsInbox(String mode) throws Exception {
        try (Fixture f = new Fixture(mode)) {
            f.login(1, 10);
            Long id =
                    f.messages.save(
                            new MessageSaveRequest(
                                    null,
                                    null,
                                    "公告",
                                    "正文",
                                    "TEXT",
                                    "ANNOUNCEMENT",
                                    new NoticeTarget(true, List.of(), List.of(), List.of(), false),
                                    "/home",
                                    "batch",
                                    "batch-1",
                                    "req-1",
                                    null));
            var draft = f.messages.detail(id);
            f.messages.publish(new NoticeVersionRequest(id, draft.getVersion()));
            assertEquals(0, f.inbox.page(null, null, 1, 20).total());
            f.audience = List.of(new NoticeUser(30L, "后来新增", "later@example.com"));
            f.tasks.execute();
            assertEquals("PUBLISHED", f.messages.detail(id).getPublishStatus());
            assertEquals(2, f.messages.counts(id).get("total"));
            assertEquals(1, f.inbox.page(null, null, 1, 20).total());
            var item = f.inbox.page(null, null, 1, 20).records().getFirst();
            assertEquals(1, f.inbox.unread().get("total"));
            f.inbox.read(item.id());
            f.inbox.read(item.id());
            assertEquals(0, f.inbox.unread().get("total"));
            assertEquals(1, f.messages.page(null, null, 1, 20).records().getFirst().getReadCount());
            f.inbox.readAll();
            assertEquals(1, f.messages.counts(id).get("read"));
            f.inbox.remove(item.id());
            assertEquals(0, f.inbox.page(null, null, 1, 20).total());
            assertEquals(2, f.messages.counts(id).get("total"));
            f.login(1, 30);
            assertThrows(CommonException.class, () -> f.inbox.detail(item.id()));
            assertEquals(0, f.inbox.page(null, null, 1, 20).total());
            f.login(2, 10);
            assertThrows(CommonException.class, () -> f.messages.detail(id));
            assertEquals(0, f.inbox.page(null, null, 1, 20).total());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void retriesOnlyFailedMailAndKeepsAttemptHistory(String mode) throws Exception {
        try (Fixture f = new Fixture(mode)) {
            f.login(1, 10);
            Long config =
                    f.settings.saveConfig(
                            new MailConfigSaveRequest(
                                    null,
                                    null,
                                    "SMTP",
                                    "test",
                                    "smtp.example.com",
                                    465,
                                    "TLS",
                                    "user",
                                    "secret",
                                    "sender@example.com",
                                    "UPM",
                                    null,
                                    1,
                                    null));
            var request =
                    new MailSaveRequest(
                            null,
                            null,
                            config,
                            null,
                            "Hi",
                            "TEXT",
                            "Hello",
                            List.of(),
                            Map.of(),
                            List.of(new MailReceiver(null, "a@example.com", "A", Map.of())),
                            null,
                            "batch",
                            "b-1",
                            "mail-req",
                            null);
            Long id = f.mail.save(request);
            assertEquals(id, f.mail.save(request));
            f.mail.send(new NoticeVersionRequest(id, f.mail.detail(id).getVersion()));
            f.sender.failure = new org.springframework.mail.MailAuthenticationException("no");
            f.tasks.execute();
            assertEquals("FAILED", f.mail.detail(id).getSendStatus());
            assertEquals(1, f.mail.recipients(id, 1, 20).records().getFirst().getAttemptCount());
            f.mail.retry(new NoticeVersionRequest(id, f.mail.detail(id).getVersion()));
            f.sender.failure = null;
            f.tasks.execute();
            assertEquals("SUCCESS", f.mail.detail(id).getSendStatus());
            assertEquals(2, f.mail.recipients(id, 1, 20).records().getFirst().getAttemptCount());
            assertEquals(1, f.mail.detail(id).getSuccessCount());
            f.login(2, 10);
            assertThrows(CommonException.class, () -> f.mail.detail(id));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void automaticRetryWaitsUntilDueAndCountsActualAttempts(String mode) throws Exception {
        try (Fixture f = new Fixture(mode)) {
            f.login(1, 10);
            Long config =
                    f.settings.saveConfig(
                            new MailConfigSaveRequest(
                                    null,
                                    null,
                                    "SMTP",
                                    "test",
                                    "smtp.example.com",
                                    465,
                                    "TLS",
                                    "user",
                                    "secret",
                                    "sender@example.com",
                                    "UPM",
                                    null,
                                    1,
                                    null));
            Long id =
                    f.mail.save(
                            new MailSaveRequest(
                                    null,
                                    null,
                                    config,
                                    null,
                                    "Hi",
                                    "TEXT",
                                    "Hello",
                                    List.of(),
                                    Map.of(),
                                    List.of(new MailReceiver(null, "a@example.com", "A", Map.of())),
                                    null,
                                    null,
                                    null,
                                    null,
                                    null));
            f.mail.send(new NoticeVersionRequest(id, f.mail.detail(id).getVersion()));
            f.sender.failure = new org.springframework.mail.MailSendException("temporary");
            f.tasks.execute();
            var first = f.mail.recipients(id, 1, 20).records().getFirst();
            assertEquals("RETRY_WAIT", first.getSendStatus());
            assertNotNull(first.getNextRetryTime());
            assertEquals(1, first.getAttemptCount());
            f.tasks.execute();
            assertEquals(1, f.mail.recipients(id, 1, 20).records().getFirst().getAttemptCount());
            var mapper = f.session.getMapper(NoticeMailRecipientMapper.class);
            first.setNextRetryTime(LocalDateTime.now().minusSeconds(1));
            mapper.updateById(first);
            f.sender.failure = null;
            f.tasks.execute();
            var success = f.mail.recipients(id, 1, 20).records().getFirst();
            assertEquals("SUCCESS", success.getSendStatus());
            assertEquals(2, success.getAttemptCount());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void resumesExpiredLeaseWithoutDuplicatingRecipientsAndHonorsSchedule(String mode) throws Exception {
        try (Fixture f = new Fixture(mode)) {
            f.login(1, 10);
            Long id =
                    f.messages.save(
                            new MessageSaveRequest(
                                    null,
                                    null,
                                    "计划公告",
                                    "正文",
                                    "TEXT",
                                    "ANNOUNCEMENT",
                                    new NoticeTarget(true, List.of(), List.of(), List.of(), false),
                                    null,
                                    null,
                                    null,
                                    null,
                                    LocalDateTime.now().plusDays(1)));
            f.messages.publish(new NoticeVersionRequest(id, f.messages.detail(id).getVersion()));
            f.tasks.execute();
            assertEquals("SCHEDULED", f.messages.detail(id).getPublishStatus());
            var mapper = f.session.getMapper(NoticeMessageMapper.class);
            var main = f.messages.detail(id);
            main.setScheduledTime(LocalDateTime.now().minusSeconds(1));
            main.setLeaseToken("old-worker");
            main.setLeaseUntil(LocalDateTime.now().plusMinutes(1));
            mapper.updateById(main);
            f.tasks.execute();
            assertEquals(0, f.messages.counts(id).get("total"));
            main = f.messages.detail(id);
            main.setLeaseUntil(LocalDateTime.now().minusSeconds(1));
            main.setRecipientCount(1L);
            mapper.updateById(main);
            NoticeMessageRecipient partial = new NoticeMessageRecipient();
            partial.setId(99L);
            partial.setTenantId(1L);
            partial.setMessageId(id);
            partial.setUserId(10L);
            partial.setRecipientName("张三");
            partial.setCreateTime(LocalDateTime.now());
            partial.setUpdateTime(LocalDateTime.now());
            f.session.getMapper(NoticeMessageRecipientMapper.class).insert(partial);
            f.login(2, 99);
            f.tasks.execute();
            assertEquals(2L, RequestContextHolder.getTenantId());
            assertEquals(99L, RequestContextHolder.getUserId());
            f.login(1, 10);
            assertEquals("PUBLISHED", f.messages.detail(id).getPublishStatus());
            assertEquals(2, f.messages.detail(id).getRecipientCount());
            assertEquals(2, f.messages.counts(id).get("total"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void validatesVersionsRollsBackInvalidMailAndAllowsClearingConfiguration(String mode) throws Exception {
        try (Fixture f = new Fixture(mode)) {
            f.login(1, 10);
            Long configId =
                    f.settings.saveConfig(
                            new MailConfigSaveRequest(
                                    null,
                                    null,
                                    "SMTP",
                                    "test",
                                    "smtp.example.com",
                                    465,
                                    "TLS",
                                    "user",
                                    "secret",
                                    "sender@example.com",
                                    "UPM",
                                    "reply@example.com",
                                    1,
                                    null));
            var config = f.settings.config(configId);
            assertThrows(
                    CommonException.class,
                    () ->
                            f.settings.saveConfig(
                                    new MailConfigSaveRequest(
                                            configId,
                                            config.getVersion() + 1,
                                            "SMTP",
                                            "test",
                                            "smtp.example.com",
                                            465,
                                            "TLS",
                                            "user",
                                            "",
                                            "sender@example.com",
                                            "UPM",
                                            null,
                                            1,
                                            null)));
            f.settings.saveConfig(
                    new MailConfigSaveRequest(
                            configId,
                            config.getVersion(),
                            "SMTP",
                            "test",
                            "smtp.example.com",
                            465,
                            "TLS",
                            "user",
                            "",
                            "sender@example.com",
                            "UPM",
                            null,
                            1,
                            null));
            assertNull(f.settings.config(configId).getReplyTo());
            assertThrows(
                    CommonException.class,
                    () ->
                            f.mail.save(
                                    new MailSaveRequest(
                                            null,
                                            null,
                                            configId,
                                            null,
                                            "Hi",
                                            "TEXT",
                                            "${missing}",
                                            List.of(),
                                            Map.of(),
                                            List.of(new MailReceiver(null, "a@example.com", "A", Map.of())),
                                            null,
                                            null,
                                            null,
                                            null,
                                            null)));
            assertEquals(0, f.mail.page(null, 1, 20).total());
            f.settings.deleteConfig(new NoticeVersionRequest(configId, f.settings.config(configId).getVersion()));
            assertThrows(CommonException.class, () -> f.settings.config(configId));
            assertNotNull(
                    f.settings.saveConfig(
                            new MailConfigSaveRequest(
                                    null,
                                    null,
                                    "New SMTP",
                                    "test",
                                    "smtp.example.com",
                                    465,
                                    "TLS",
                                    "user",
                                    "secret",
                                    "sender@example.com",
                                    "UPM",
                                    null,
                                    1,
                                    null)));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void sendsTemplateByCodeAndKeepsPublishedSnapshot(String mode) throws Exception {
        try (Fixture f = new Fixture(mode)) {
            f.login(1, 10);
            Long configId =
                    f.settings.saveConfig(
                            new MailConfigSaveRequest(
                                    null,
                                    null,
                                    "SMTP",
                                    "company",
                                    "smtp.example.com",
                                    465,
                                    "TLS",
                                    "user",
                                    "secret",
                                    "sender@example.com",
                                    "UPM",
                                    null,
                                    1,
                                    null));
            var variables = List.of(new TemplateVariable("name", "姓名", true, "Alex"));
            Long templateId =
                    f.settings.saveTemplate(
                            new MailTemplateSaveRequest(
                                    null,
                                    null,
                                    "通知",
                                    "notify",
                                    "Hi ${name}",
                                    "HTML",
                                    "<p>${name}</p>",
                                    variables,
                                    1,
                                    null));
            var request =
                    new MailTemplateSendRequest(
                            "company",
                            "notify",
                            Map.of("name", "张三"),
                            List.of(new MailReceiver(null, "a@example.com", "A", Map.of())),
                            null,
                            "batch",
                            "b-1",
                            "code-req",
                            null);
            Long id = f.mail.sendTemplate(request);
            assertEquals(id, f.mail.sendTemplate(request));
            var template = f.settings.template(templateId);
            f.settings.saveTemplate(
                    new MailTemplateSaveRequest(
                            templateId,
                            template.getVersion(),
                            "通知",
                            "notify",
                            "Changed ${name}",
                            "TEXT",
                            "Changed ${name}",
                            variables,
                            1,
                            null));
            assertEquals("<p>${name}</p>", f.mail.detail(id).getContent());
            assertEquals("<p>张三</p>", f.mail.preview(id).content());
            f.tasks.execute();
            assertEquals("SUCCESS", f.mail.detail(id).getSendStatus());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void stoppedTenantDoesNotPublishQueuedMessages(String mode) throws Exception {
        try (Fixture f = new Fixture(mode)) {
            f.login(1, 10);
            Long id =
                    f.messages.send(
                            new MessageSaveRequest(
                                    null,
                                    null,
                                    "公告",
                                    "正文",
                                    "TEXT",
                                    "ANNOUNCEMENT",
                                    new NoticeTarget(true, List.of(), List.of(), List.of(), false),
                                    null,
                                    null,
                                    null,
                                    null,
                                    null));
            f.tenantAvailable = false;
            f.tasks.execute();
            assertEquals("FAILED", f.messages.detail(id).getPublishStatus());
            assertEquals(0, f.messages.counts(id).get("total"));
            assertEquals(0, f.inbox.page(null, null, 1, 20).total());
        }
    }

    static class FakeSender extends SmtpMailSender {
        RuntimeException failure;

        FakeSender() {
            super(new MailSecretCipher(new NoticeProperties()), new NoticeProperties());
        }

        @Override
        public String send(
                NoticeMailConfig config,
                String address,
                String name,
                String reply,
                String recipient,
                MailPreview preview,
                String messageId) {
            if (failure != null) throw failure;
            return messageId;
        }
    }

    static class Fixture implements AutoCloseable {
        final EmbeddedDatabase database;
        final SqlSessionTemplate session;
        final TransactionTemplate tx;
        ApplicationEventPublisher events = event -> {};
        final MessageService messages;
        final NoticeInboxService inbox;
        final NoticeTasks tasks;
        final MailService mail;
        final MailConfigurationService settings;
        final FakeSender sender = new FakeSender();
        boolean tenantAvailable = true;
        List<NoticeUser> audience =
                List.of(new NoticeUser(10L, "张三", "a@example.com"), new NoticeUser(20L, "李四", "b@example.com"));

        Fixture(String mode) throws Exception {
            database = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
            try (var connection = database.getConnection();
                    var statement = connection.createStatement()) {
                statement.execute("SET MODE " + mode);
            }
            new ResourceDatabasePopulator(new ClassPathResource("notice-schema.sql")).execute(database);
            MybatisConfiguration config = new MybatisConfiguration();
            config.setMapUnderscoreToCamelCase(true);
            config.addMappers("io.github.yilers.notice.mapper");
            MybatisPlusInterceptor plugins = new MybatisPlusInterceptor();
            plugins.addInnerInterceptor(
                    new TenantLineInnerInterceptor(
                            new TenantLineHandler() {
                                public Expression getTenantId() {
                                    return new LongValue(RequestContextHolder.getTenantId());
                                }
                            }));
            plugins.addInnerInterceptor(new PaginationInnerInterceptor());
            plugins.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
            var factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(database);
            factory.setConfiguration(config);
            factory.setPlugins(plugins);
            session = new SqlSessionTemplate(factory.getObject());
            tx = new TransactionTemplate(new DataSourceTransactionManager(database));
            var support =
                    new NoticeSupport(tools.jackson.databind.json.JsonMapper.builder().findAndAddModules().build());
            var properties = new NoticeProperties();
            properties.setBatchSize(1);
            properties.setSecretKey(Base64.getEncoder().encodeToString(new byte[32]));
            var renderer = new MailRenderer();
            NoticeDataProvider provider =
                    new NoticeDataProvider() {
                        public boolean isTenantAvailable() {
                            return tenantAvailable;
                        }

                        public List<NoticeUser> findAllUsers() {
                            return audience;
                        }

                        public List<NoticeUser> findUsersByIds(List<Long> ids) {
                            return audience.stream().filter(u -> ids.contains(u.id())).toList();
                        }

                        public List<Long> findUserIdsByRoleIds(List<Long> ids) {
                            return List.of();
                        }

                        public List<Long> findUserIdsByDepartmentIds(List<Long> ids) {
                            return List.of();
                        }

                        public List<NoticeDepartment> findDepartments() {
                            return List.of();
                        }

                        public List<NoticeDirectoryEntry> findEntries(DirectoryType type, List<Long> ids) {
                            return List.of();
                        }

                        public NoticePage<NoticeDirectoryEntry> search(
                                DirectoryType type, String keyword, int current, int size) {
                            return new NoticePage<>(0, List.of());
                        }
                    };
            var audienceService = new NoticeAudienceServiceImpl(provider, properties);
            messages =
                    transactional(
                            new NoticeMessageServiceImpl(
                                    session.getMapper(NoticeMessageMapper.class),
                                    session.getMapper(NoticeMessageRecipientMapper.class),
                                    support,
                                    audienceService,
                                    event -> events.publishEvent(event)),
                            tx);
            inbox =
                    new NoticeInboxServiceImpl(
                            session.getMapper(NoticeInboxMapper.class),
                            session.getMapper(NoticeMessageRecipientMapper.class));
            settings =
                    transactional(
                            new MailConfigurationServiceImpl(
                                    session.getMapper(NoticeMailConfigMapper.class),
                                    session.getMapper(NoticeMailTemplateMapper.class),
                                    new MailSecretCipher(properties),
                                    support,
                                    renderer),
                            tx);
            mail =
                    transactional(
                            new NoticeMailServiceImpl(
                                    session.getMapper(NoticeMailMapper.class),
                                    session.getMapper(NoticeMailRecipientMapper.class),
                                    settings,
                                    audienceService,
                                    provider,
                                    support,
                                    renderer,
                                    properties,
                                    event -> events.publishEvent(event)),
                            tx);
            tasks =
                    new NoticeTasks(
                            session.getMapper(NoticeTaskMapper.class),
                            provider,
                            messages,
                            support,
                            properties,
                            settings,
                            mail,
                            sender,
                            tx);
        }

        @SuppressWarnings("unchecked")
        private static <T> T transactional(T target, TransactionTemplate tx) {
            var factory = new org.springframework.transaction.interceptor.TransactionProxyFactoryBean();
            factory.setTarget(target);
            factory.setProxyTargetClass(true);
            factory.setTransactionManager(tx.getTransactionManager());
            java.util.Properties attributes = new java.util.Properties();
            for (String method : List.of("save*", "publish", "send*", "submit", "retry", "delete*"))
                attributes.setProperty(method, "PROPAGATION_REQUIRED");
            factory.setTransactionAttributes(attributes);
            factory.afterPropertiesSet();
            return (T) factory.getObject();
        }

        void login(long tenant, long user) {
            RequestContextHolder.clear();
            RequestContextHolder.setTenantId(tenant);
            RequestContextHolder.setUserId(user);
        }

        public void close() {
            database.shutdown();
        }
    }
}
