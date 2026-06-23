package com.aiwechat.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_concept_chunks")
public class KnowledgeConceptChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conceptId;

    private String chunkId;

    private Long documentId;

    private Double weight;

    private String source;

    private LocalDateTime createdAt;
}
