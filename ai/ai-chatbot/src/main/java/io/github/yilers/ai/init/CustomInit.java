package io.github.yilers.ai.init;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomInit implements CommandLineRunner {
    private final ChatClient.Builder chatClientBuilder;
    private final ResourceLoader resourceLoader;


    @Override
    public void run(String... args) throws Exception {

//        var localSubagentTypes = ClaudeSubagentType.builder()
//                .skillsResources(Collections.singletonList(resourceLoader.getResource("classpath:/skills")))
//                .chatClientBuilder("default",
//                        chatClientBuilder.clone())
////                .braveApiKey(braveApiKey)
//                .build();
//
//        var taskTools = TaskTool.builder()
//                // Add Claude Subagent (local)
//                .subagentTypes(localSubagentTypes)
//                .subagentReferences(ClaudeSubagentReferences.fromResources(Collections.singletonList(resourceLoader.getResource("classpath:/skills"))))
//                .build();
//
//        chatClientBuilder.defaultSystem(p -> p.text("你是一个代码审查专家") // system prompt
//                .param(AgentEnvironment.ENVIRONMENT_INFO_KEY, AgentEnvironment.info())
//                .param(AgentEnvironment.GIT_STATUS_KEY, AgentEnvironment.gitStatus())
//                )
//                // Sub-Agents
//                .defaultToolCallbacks(taskTools).build();
    }
}
