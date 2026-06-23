package com.aiwechat.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_documents")
public class KnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String source;

    /** 文件路径 */
    private String filePath;

    /** 文件类型：pdf, txt, menu, faq, rule */
    private String fileType;

    /** 业务类型：menu, delivery, order_rule, faq, promotion */
    private String bizType;

    /** 分类标签 */
    private String category;

    /** 状态：active, deleted */
    private String status;

    /** 文档版本号 */
    private Integer version;

    /** 分块数量 */
    private Integer chunkCount;

    /** 扩展元数据（JSON） */
    private String metadata;

    /** 内容哈希，用于去重 */
    private String contentHash;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
