package com.aiwechat.knowledge.service;

import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult.ChunkResult;

import java.util.List;

/**
 * 重排序服务接口
 * 对粗召回结果进行精排
 */
public interface RerankService {

    /**
     * 重排序
     * @param query 原始查询
     * @param candidates 粗召回候选结果
     * @param topK 最终返回数量
     * @return 重排序后的结果
     */
    List<ChunkResult> rerank(String query, List<ChunkResult> candidates, int topK);
}
