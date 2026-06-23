package com.aiwechat.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_chunks")
public class KnowledgeChunk {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 所属文档ID */
    private Long documentId;

    /** chunk 文本内容 */
    private String content;

    /** chunk 在文档中的顺序 */
    private Integer chunkIndex;

    /** chunk 类型：faq, menu, rule, description */
    private String chunkType;

    /** 来源标识 */
    private String source;

    /** 向量是否已同步 */
    private Boolean vectorSynced;

    /** 关键词（逗号分隔） */
    private String keywords;

    /** 扩展元数据（JSON） */
    private String metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
