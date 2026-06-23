package com.aiwechat.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_concepts")
public class KnowledgeConcept {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String normalizedName;

    /** menu, rule, faq, concept */
    private String conceptType;

    private Integer frequency;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
