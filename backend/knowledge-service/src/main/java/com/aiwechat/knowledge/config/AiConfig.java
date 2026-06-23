package com.aiwechat.knowledge.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 核心配置类
 * 仅配置知识库 embedding 与向量存储
 */
@Slf4j
@Configuration
public class AiConfig {

    /**
     * 配置向量存储（内存模式）
     * 使用 SimpleVectorStore 作为内存向量存储
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        log.info("初始化 SimpleVectorStore（内存模式）");
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
