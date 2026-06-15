//package io.github.yilers.ai.init;
//
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//public class RagInit {
//    private final VectorStore vectorStore;
//
//    @PostConstruct
//    public void init() {
//        List<Document> docs = List.of(
//                new Document("""
//                        佳之易退款规则：
//
//                        1. 支持7天无理由退款
//                        2. 虚拟商品不支持退款
//                        3. 超过7天不可退款
//                        """),
//
//                new Document("""
//                        佳之易工作时间：
//
//                        周一到周五：
//                        9:00-18:00
//                        """)
//        );
//
//        vectorStore.add(docs);
//
//        System.out.println("知识库初始化完成");
//    }
//}
