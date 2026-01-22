package io.github.yilers.upm.destroy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * 服务销毁
 *
 * @author zhanghui
 * @since 2024/8/16 上午9:27
 */

@Slf4j
@Component
public class DestroyRunner implements ApplicationListener<ContextClosedEvent> {

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        Object source = event.getSource();
        log.info("服务销毁:{}", source);
    }
}
