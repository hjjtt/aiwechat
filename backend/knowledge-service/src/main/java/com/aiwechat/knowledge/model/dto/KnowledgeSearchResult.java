package com.aiwechat.knowledge.model.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class KnowledgeSearchResult {

    private String query;

    private int count;

    private List<ChunkResult> results;

    /** 检索耗时（毫秒） */
    private long costMs;

    /** 检索类型：vector, keyword, hybrid */
    private String searchType;

    /** Query 路由类型：structured, vector, hybrid, graph */
    private String routeType;

    /** 路由原因，便于调试 */
    private String routeReason;

    /** 是否发生兜底检索 */
    private boolean fallback;

    /** 统一证据列表 */
    private List<KnowledgeEvidence> evidences;

    @Data
    public static class ChunkResult {

        private String content;

        private String source;

        /** 综合得分 */
        private double score;

        /** 向量相似度得分 */
        private Double vectorScore;

        /** 关键词匹配得分 */
        private Double keywordScore;

        /** chunk 类型 */
        private String chunkType;

        /** 所属文档 ID */
        private String documentId;

        /** 元数据 */
        private Map<String, Object> metadata;

        /** 证据类型：structured, vector, graph */
        private String evidenceType;

        /** 证据标题 */
        private String title;
    }
}
