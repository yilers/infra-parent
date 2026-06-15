package io.github.yilers.ai.config;

import lombok.RequiredArgsConstructor;
import org.springaicommunity.agent.common.task.subagent.SubagentType;
import org.springaicommunity.agent.tools.*;
import org.springaicommunity.agent.tools.task.TaskTool;
import org.springaicommunity.agent.tools.task.claude.ClaudeSubagentExecutor;
import org.springaicommunity.agent.tools.task.claude.ClaudeSubagentReferences;
import org.springaicommunity.agent.tools.task.claude.ClaudeSubagentResolver;
import org.springaicommunity.agent.utils.CommandLineQuestionHandler;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class AiConfig {
    private final ResourceLoader resourceLoader;


    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
//        Path agentsPath = Path.of("/Users/jzy/IdeaProjects/infra-parent/ai/ai-chatbot/.agents").toAbsolutePath();
        String skillsDir = "/Users/jzy/IdeaProjects/infra-parent/ai/ai-chatbot/src/main/resources/skills";
        ChatClient.Builder builder = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultAdvisors(ToolCallAdvisor.builder().conversationHistoryEnabled(false).build());

        return
                builder
                // custom skills
                .defaultTools(AskUserQuestionTool.builder()
                                .questionHandler(new CommandLineQuestionHandler())
                                .build())

                .defaultTools(toolSpec -> toolSpec.callbacks(SkillsTool.builder()
                        .addSkillsResource(resourceLoader.getResource("classpath:/skills"))
                        .build()))

                .defaultTools(toolSpec -> toolSpec.callbacks(TaskTool.builder()
                        .subagentReferences(ClaudeSubagentReferences.fromResource(resourceLoader.getResource("classpath:/agents")))
                        .subagentTypes(new SubagentType(
                                new ClaudeSubagentResolver(),
                                new ClaudeSubagentExecutor(Map.of("default", builder),
                                        new ArrayList<>(),
                                        List.of(skillsDir)
                                ))
                        )
                        .build()))
//                .defaultTools(TodoWriteTool.builder().build())
                // defaultTools
                .defaultTools(FileSystemTools.builder().build())
                .defaultTools(ShellTools.builder().build())
                .build();
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(40).chatMemoryRepository(new InMemoryChatMemoryRepository()).build();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore
                .builder(embeddingModel)
                .build();
    }



}
