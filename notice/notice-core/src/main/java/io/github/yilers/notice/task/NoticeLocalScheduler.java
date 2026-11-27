package io.github.yilers.notice.task;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 可选本地调度。外部调度调用NoticeTaskDispatcher.dispatch时关闭此开关，任务仍持久化。 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notice.task.enabled", havingValue = "true")
public class NoticeLocalScheduler {
    /** 共用到期扫描与任务执行入口。 */
    private final NoticeTaskDispatcher dispatcher;

    /** 按配置间隔扫描到期及积压任务。 */
    @Scheduled(fixedDelayString = "${notice.task.interval-millis:5000}")
    public void execute() {
        dispatcher.dispatch();
    }
}
