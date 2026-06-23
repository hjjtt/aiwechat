package com.aiwechat.knowledge.service;

import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;

import java.util.List;
import java.util.Map;

/**
 * 混合检索服务接口
 * 融合向量召回、关键词召回、metadata 过滤
 */
public interface HybridSearchService {

    /**
     * 混合检索
     * @param query 查询文本
     * @param topK 返回结果数
     * @return 融合排序后的检索结果
     */
    KnowledgeSearchResult hybridSearch(String query, int topK);

    /**
     * 带过滤条件的混合检索
     * @param query 查询文本
     * @param topK 返回结果数
     * @param filters metadata 过滤条件
     * @return 融合排序后的检索结果
     */
    KnowledgeSearchResult hybridSearch(String query, int topK, Map<String, Object> filters);

    /**
     * 纯向量检索
     */
    KnowledgeSearchResult vectorSearch(String query, int topK);

    /**
     * 纯关键词检索
     */
    KnowledgeSearchResult keywordSearch(String query, int topK);

    /**
     * 菜品名精确匹配检索
     */
    KnowledgeSearchResult menuExactSearch(String menuName);
}
