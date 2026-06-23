package com.aiwechat.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_concept_edges")
public class KnowledgeConceptEdge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sourceConceptId;

    private Long targetConceptId;

    /** co_occurs, same_source 等 */
    private String edgeType;

    private Integer weight;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
