package io.github.yilers.notice;

import static org.junit.jupiter.api.Assertions.*;

import io.github.yilers.notice.mapper.NoticeTaskMapper;
import io.github.yilers.notice.task.NoticeTaskDispatcher;
import io.github.yilers.notice.task.NoticeTasks;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** 邮件阻塞时公告仍能执行，各渠道并发受限，不在Spring调度线程连接SMTP。 */
class NoticeTaskDispatcherTest {
    @Test
    void blockedMailDoesNotBlockInAppPublication() throws Exception {
        AtomicBoolean publish = new AtomicBoolean();
        CountDownLatch mailStarted = new CountDownLatch(2),
                release = new CountDownLatch(1),
                messageDone = new CountDownLatch(1);
        AtomicInteger activeMail = new AtomicInteger(), maximumMail = new AtomicInteger();
        NoticeTaskMapper mapper =
                new NoticeTaskMapper() {
                    public List<Task> dueMessages(LocalDateTime now) {
                        return publish.get() ? List.of(new Task(100L, 1L, "MESSAGE")) : List.of();
                    }

                    public List<Task> dueMails(LocalDateTime now) {
                        return List.of(new Task(1L, 1L, "MAIL"), new Task(2L, 1L, "MAIL"), new Task(3L, 1L, "MAIL"));
                    }
                };
        NoticeTasks tasks =
                new NoticeTasks(null, null, null, null, null, null, null, null, null) {
                    @Override
                    public void process(NoticeTaskMapper.Task task) {
                        if (task.kind().equals("MESSAGE")) {
                            messageDone.countDown();
                            return;
                        }
                        int active = activeMail.incrementAndGet();
                        maximumMail.accumulateAndGet(active, Math::max);
                        mailStarted.countDown();
                        try {
                            release.await(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            activeMail.decrementAndGet();
                        }
                    }
                };
        NoticeTaskDispatcher dispatcher = new NoticeTaskDispatcher(mapper, tasks);
        try {
            dispatcher.dispatch();
            assertTrue(mailStarted.await(2, TimeUnit.SECONDS));
            publish.set(true);
            dispatcher.dispatch();
            assertTrue(messageDone.await(2, TimeUnit.SECONDS));
            assertEquals(2, maximumMail.get());
        } finally {
            release.countDown();
            dispatcher.shutdown();
        }
    }
}
