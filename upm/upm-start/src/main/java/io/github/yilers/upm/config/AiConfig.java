package io.github.yilers.upm.config;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.agent.common.task.subagent.SubagentReference;
import org.springaicommunity.agent.common.task.subagent.SubagentType;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springaicommunity.agent.tools.task.TaskOutputTool;
import org.springaicommunity.agent.tools.task.TaskTool;
import org.springaicommunity.agent.tools.task.claude.ClaudeSubagentExecutor;
import org.springaicommunity.agent.tools.task.claude.ClaudeSubagentReferences;
import org.springaicommunity.agent.tools.task.claude.ClaudeSubagentResolver;
import org.springaicommunity.agent.tools.task.repository.DefaultTaskRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiConfig {
    private final ResourceLoader resourceLoader;

    @Bean
    public ChatClient chatClient(
            ChatModel chatModel,
            ChatMemory chatMemory,
            ObjectProvider<SkillsTool> skillsToolProvider) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(TodoWriteTool.builder().build());

        // skills 可选加载
        skillsToolProvider.ifAvailable(tool -> {
            builder.defaultTools(tool);
            log.info("已加载 SkillsTool");
        });

        // agents 可选加载
        registerTaskToolIfAgentsPresent(builder, chatModel);

        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "ai.skills", name = "enabled", havingValue = "true")
    public ToolCallback skillsTool() throws IOException {
        Resource skillsResource = resourceLoader.getResource("classpath:/skills");
        if (!skillsResource.exists()) {
            throw new FileNotFoundException("skills 资源不存在: " + skillsResource.getDescription());
        }
        return SkillsTool.builder()
                .addSkillsResource(skillsResource)
                .build();
    }

    private void registerTaskToolIfAgentsPresent(ChatClient.Builder builder, ChatModel chatModel) {
        Resource agentsResource = resourceLoader.getResource("classpath:/agents");
        List<SubagentReference> subagentReferences;
        subagentReferences = ClaudeSubagentReferences.fromResource(agentsResource);
        if (subagentReferences.isEmpty()) {
            log.info("未加载 TaskTool，agents 下没有 subagent 定义文件");
            return;
        }

        Enumeration<URL> skillResources;
        try {
            skillResources = resourceLoader.getClassLoader().getResources("skills");
        } catch (IOException e) {
            log.warn("获取 skills 资源失败，使用空列表: {}", e.getMessage());
            skillResources = Collections.emptyEnumeration();
        }

        ChatClient.Builder subagentBuilder = ChatClient.builder(chatModel)
                .defaultTools(TaskOutputTool.builder()
                        .taskRepository(new DefaultTaskRepository())
                        .build());

        List<String> skillPaths = Collections.list(skillResources).stream()
                .map(URL::getPath)
                .toList();

        Object taskTool = TaskTool.builder()
                .subagentReferences(subagentReferences)
                .subagentTypes(new SubagentType(
                        new ClaudeSubagentResolver(),
                        new ClaudeSubagentExecutor(
                                Map.of("default", subagentBuilder),
                                new ArrayList<>(),
                                skillPaths
                        )
                ))
                .build();

        builder.defaultTools(taskTool);
        log.info("已加载 TaskTool，subagent 数量: {}", subagentReferences.size());
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(100).chatMemoryRepository(new InMemoryChatMemoryRepository()).build();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore
                .builder(embeddingModel)
                .build();
    }



}
