package io.github.yilers.upm.i18n;

import io.github.yilers.web.i18n.MessageCode;
import io.github.yilers.web.i18n.MessageTextCatalog;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** 按原有中文文本登记校验消息，不改变已有 DTO 和批量结果。 */
@Component
public class UpmMessageTextCatalog implements MessageTextCatalog {

    private static final Map<String, MessageCode> MESSAGES = Stream.concat(
                    Arrays.stream(UpmTextMessage.values()).map(message -> (MessageCode) message),
                    Stream.of(UpmMessage.USER_ACCOUNT_EXISTS))
            .collect(Collectors.toUnmodifiableMap(MessageCode::getDefaultMessage, Function.identity()));

    @Override
    public Map<String, MessageCode> messages() {
        return MESSAGES;
    }
}
