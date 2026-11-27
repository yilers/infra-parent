package io.github.yilers.notice.task;

import io.github.yilers.notice.mapper.NoticeTaskMapper;
import io.github.yilers.notice.model.NoticeTypes.TaskKind;

import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.concurrent.*;

/** 非阻塞调度入口，站内发布与SMTP各有独立的并发预算，避免SMTP超时阻塞公告或Spring调度线程。 这里只限流内存执行，不承担任务可靠性；排队、租约和重试状态始终保存在数据库。 */
@Component
@RequiredArgsConstructor
public class NoticeTaskDispatcher {
    /** 每个发送渠道在本节点允许同时处理的任务数。 */
    private static final int TASKS_PER_CHANNEL = 2;

    /** 查找已到期或待恢复的租户任务。 */
    private final NoticeTaskMapper mapper;

    /** 按任务所属租户抢占租约并执行。 */
    private final NoticeTasks tasks;

    /** 使用虚拟线程隔离后台任务和 HTTP 请求线程。 */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /** 站内通知独立的并发配额。 */
    private final Semaphore messageSlots = new Semaphore(TASKS_PER_CHANNEL);

    /** 邮件独立的并发配额，避免慢 SMTP 占用站内通知的资源。 */
    private final Semaphore mailSlots = new Semaphore(TASKS_PER_CHANNEL);

    /** 本地或外部分布式调度可每5秒调用一次，本方法不等待SMTP网络操作。 */
    public void dispatch() {
        LocalDateTime now = LocalDateTime.now();
        java.util.List<NoticeTaskMapper.Task> pending = new java.util.ArrayList<>(mapper.dueMessages(now));
        pending.addAll(mapper.dueMails(now));
        for (var task : pending) submit(task);
    }

    /** 事务成功提交后才提交执行；并发槽已满时由后续扫描补捞。 */
    @TransactionalEventListener
    public void dispatchAfterCommit(NoticeTaskMapper.Task task) {
        submit(task);
    }

    /** 共享提交入口。配额已满时留待下轮扫描。 */
    private void submit(NoticeTaskMapper.Task task) {
        Semaphore slots = TaskKind.MESSAGE.name().equals(task.kind()) ? messageSlots : mailSlots;
        if (!slots.tryAcquire()) return;
        try {
            executor.submit(() -> {
                try {
                    tasks.process(task);
                } finally {
                    slots.release();
                }
            });
        } catch (RejectedExecutionException e) {
            slots.release();
        }
    }

    /** 停止接受新任务，等待在途任务结束。超时任务由租约机制恢复。 */
    @PreDestroy
    public void shutdown() {
        // 不再接收新任务；未完成的数据库任务在下次启动、租约到期后恢复。
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
