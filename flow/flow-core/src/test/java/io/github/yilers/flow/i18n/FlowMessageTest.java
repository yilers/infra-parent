package io.github.yilers.flow.i18n;

import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.i18n.InfraMessageResolver;
import io.github.yilers.web.i18n.LocalizedTextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowMessageTest {

    @Test
    void allMessagesExistInBothLanguages() throws Exception {
        ResourceBundle chinese = ResourceBundle.getBundle("i18n.flow-messages", Locale.ROOT);
        Properties english = new Properties();
        try (var input = getClass().getResourceAsStream("/i18n/flow-messages_en.properties")) {
            english.load(input);
        }
        for (FlowMessage message : FlowMessage.values()) {
            assertEquals(message.getDefaultMessage(), chinese.getString(message.getKey()));
            assertTrue(english.containsKey(message.getKey()), message.getKey());
        }
    }

    @Test
    void resolvesTaskMessagesAndValidationWithoutUpm() {
        InfraMessageResolver resolver = new InfraMessageResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-US");
        assertEquals("The task has been handled, does not exist, or is not accessible. Refresh the pending list.",
                resolver.resolve(FlowMessage.TASK_CHANGED, new Object[0], request));
        assertEquals("No valid approver was resolved for node “Review”. Check the assignee configuration.",
                resolver.resolve(FlowMessage.NODE_ASSIGNEE_MISSING, new Object[]{"Review"}, request));
        assertEquals("Select a target user.", new LocalizedTextResolver(resolver, List.of(new FlowMessageTextCatalog()))
                .resolve("请选择目标用户", request));
        assertEquals("The current tenant has no published process: leave",
                resolver.resolve(FlowMessage.ASSIGNEE_FLOW_NOT_PUBLISHED, new Object[]{"leave"}, request));
        assertEquals("Unsupported CC recipient: role:12",
                resolver.resolve(FlowMessage.CC_RECIPIENT_UNSUPPORTED, new Object[]{"role:12"}, request));
        assertEquals("当前租户没有已发布的流程：leave",
                new CommonException(FlowMessage.ASSIGNEE_FLOW_NOT_PUBLISHED, "leave").getMessage());
        CommonException ruleError = new CommonException(FlowMessage.ASSIGNEE_RULE_EMPTY, FlowMessage.RULE_APPLICANT);
        assertEquals("“申请人”未找到有效审批人，请检查部门主管配置", ruleError.getMessage());
        assertEquals("No eligible approver was found for “Applicant”. Check the department manager configuration.",
                resolver.resolve(ruleError.getMessageCode(), ruleError.getMessageArguments(), request));
        CommonException exception = new CommonException(404, FlowMessage.LEAVE_NOT_FOUND);
        assertEquals(404, exception.getCode());
        assertEquals("请假申请不存在或无权查看", exception.getMessage());
    }
}
