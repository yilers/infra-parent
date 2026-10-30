package io.github.yilers.flow.i18n;

import io.github.yilers.web.i18n.MessageCode;
import io.github.yilers.web.i18n.MessageTextCatalog;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** 工作流请求校验文本的兼容映射，不依赖 UPM。 */
@Component
public class FlowMessageTextCatalog implements MessageTextCatalog {

    private static final Map<String, MessageCode> MESSAGES = Stream.of(
            FlowMessage.COMMENT_TOO_LONG, FlowMessage.TARGET_REQUIRED,
            FlowMessage.TARGET_ID_INVALID, FlowMessage.ACTION_NOTE_TOO_LONG,
            FlowMessage.ARGUMENT_TYPE_UNSUPPORTED,
            FlowMessage.ARGUMENT_USER_PAGE_INVALID,
            FlowMessage.ARGUMENT_ASSIGNEE_PAGE_INVALID,
            FlowMessage.ARGUMENT_IDENTITY_TYPE_UNSUPPORTED,
            FlowMessage.ARGUMENT_ROLE_UNAVAILABLE,
            FlowMessage.ARGUMENT_DEPT_UNAVAILABLE,
            FlowMessage.ARGUMENT_FLAG_REQUIRED,
            FlowMessage.ARGUMENT_FLAG_INVALID,
            FlowMessage.ARGUMENT_DEFINITION_ID,
            FlowMessage.ARGUMENT_TASK_ID,
            FlowMessage.ARGUMENT_FLOW_CODE,
            FlowMessage.ARGUMENT_BUSINESS_ID,
            FlowMessage.ARGUMENT_BUSINESS_TYPE,
            FlowMessage.ARGUMENT_FLOW_NAME,
            FlowMessage.ARGUMENT_DEFINITION_JSON)
            .collect(Collectors.toUnmodifiableMap(FlowMessage::getDefaultMessage, Function.identity()));

    @Override
    public Map<String, MessageCode> messages() {
        return MESSAGES;
    }
}
