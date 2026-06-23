package com.aiwechat.knowledge.service.chunk;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

/**
 * 知识切分策略接口
 * 不同文档类型使用不同的切分模板和参数
 */
public interface ChunkStrategy {

    /**
     * 获取支持的文档类型
     */
    String getDocumentType();

    /**
     * 切分文档
     * @param content 文档原始内容
     * @param metadata 文档级元数据
     * @return 切分后的 Document 列表
     */
    List<Document> split(String content, Map<String, Object> metadata);

    /**
     * 从内容中提取关键词
     */
    List<String> extractKeywords(String content);
}
