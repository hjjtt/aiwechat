package com.aiwechat.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库评测用例实体
 */
@Data
@TableName("knowledge_eval_cases")
public class KnowledgeEvalCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 测试问题 */
    private String question;

    /** 标准答案 */
    private String expectedAnswer;

    /** 期望命中的来源标识 */
    private String expectedSource;

    /** 问题分类：menu, price, delivery, order_rule, faq, multi_hop */
    private String category;

    /** 难度：easy, medium, hard */
    private String difficulty;

    /** 是否启用 */
    private Boolean enabled;

    private LocalDateTime createdAt;
}
