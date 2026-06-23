package com.aiwechat.knowledge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeEvidence {

    /** 证据类型：structured, vector, graph */
    private String type;

    /** 来源类型：menu_item, rule, faq, vector_chunk, graph_path */
    private String source;

    /** 证据标题 */
    private String title;

    /** 证据正文 */
    private String content;

    /** 证据分数 */
    private double score;

    /** 扩展元数据 */
    private Map<String, Object> metadata;
}
