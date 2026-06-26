package io.github.yilers.ai.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.v7.core.io.resource.Resource;
import cn.hutool.v7.core.io.resource.ResourceUtil;
import io.github.yilers.ai.tool.CustomTool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai")
@Tag(name = "ai")
@RequiredArgsConstructor
public class AIController {
    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private VectorStore vectorStore;

    @SaIgnore
    @PostMapping("/chat")
    @Operation(summary = "Chat with AI")
    public String chat(String chatId, String message) {
        return chatClient
                .prompt()
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,
                        chatId
                ))
                .tools(new CustomTool())
                .user(message)
                .call()
                .content();
    }

    @SaIgnore
    @GetMapping("/rag/init")
    public String init() {

        vectorStore = SimpleVectorStore
                .builder(embeddingModel)
                .build();

        List<Document> docs = List.of(
                new Document("""
                        佳之易退款规则：
                        
                        1. 支持7天无理由退款
                        2. 虚拟商品不支持退款
                        3. 超过7天不可退款
                        """),

                new Document("""
                        佳之易工作时间：
                        
                        周一到周五：
                        9:00-18:00
                        """),

                new Document("""
                        关于佳之易
                        
                        佳之易网络有限公司成立于2015年，注册资本1400万元，位于西安市高新区锦业路69号瞪羚谷创业研发园A座1106，公司以打造最专业的数字产品交易平台为愿景（包含游戏娱乐、影视音频、知识付费、生活服务、电信充值等），以为中小企业提供全套解决方案为使命，使企业之间的交易更加数字化、安全化、便捷化是佳之易网络人的奋斗目标。目前已完成A轮亿元级融资。
                        2016年公司开发的数字产品交易平台——"玖佰SUP系统"开放至今，交易量逐月攀升，目前月交易量达12亿/月。平台已有2000余家供采方自由交易，现已跻身行业前三。2017年度交易量52亿元，2018年度交易量112.20亿元，公司适时启动上市。
                        佳之易网络立足西安，放眼全球，重构数字产品交易模式，带动传统产业向数字化转型发展。公司将以数字产品交易平台为载体，打造数字经济生态，成为行业最大的数字产品交易平台。并且和全球互联网巨头建立API合作关系，在整个互联网体系中，做好数字产品交易服务商的职责。引领供货方、平台、交易方、需求方，形成数产交易产业链，为产业链的供需各方提供技术支持和交易保障，为整条产业链的发展保驾护航。
                        """)
        );

        vectorStore.add(docs);

        return "知识库初始化完成";
    }

    @SaIgnore
    @PostMapping("/chat/rag")
    @Operation(summary = "Chat with AI jzy")
    public String ragChat(String chatId, String message) {
        // 向量检索
        List<Document> documents =
                vectorStore.similaritySearch(message);

        String context =
                documents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n"));

        // 拼Prompt
        String prompt = """
                你是佳之易AI助手。
         
                请基于以下知识库回答问题：
                
                %s
                
                用户问题：
                %s
                """
                .formatted(context, message);

        return chatClient
                .prompt(prompt)
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,
                        chatId
                ))
                .tools(new CustomTool())
                .call()
                .content();
    }


    @SaIgnore
    @PostMapping(value = "/chat/agent")
    @SneakyThrows
    @Operation(summary = "Chat with AI Code Reviewer")
    public String agentChat(String chatId, String message) {

        // Prompt
        Resource resource = ResourceUtil.getResource("classpath:/agents/code-reviewer.md");
        String prompt = resource.readUtf8Str();

        return chatClient
                .prompt()
                .system(prompt)
                .user(message)
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,
                        chatId
                ))
                .tools(new CustomTool())
                .call()
                .content();
    }

    @SaIgnore
    @GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<ServerSentEvent<String>> stream(String chatId, String message) {
        return chatClient
                .prompt()
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,
                        chatId
                ))
                .system("You are a helpful assistant.")
                .user(message)
                .stream()
                .content()
                .map(content ->
                        ServerSentEvent
                                .builder(content)
                                .build()
                );
    }
}
