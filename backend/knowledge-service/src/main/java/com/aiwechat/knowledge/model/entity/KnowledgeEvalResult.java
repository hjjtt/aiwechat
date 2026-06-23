package com.aiwechat.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库评测结果实体
 */
@Data
@TableName("knowledge_eval_results")
public class KnowledgeEvalResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 评测用例 ID */
    private Long evalCaseId;

    /** 测试问题 */
    private String question;

    /** 检索类型 */
    private String searchType;

    /** Top1 是否命中 */
    private Boolean top1Hit;

    /** Top3 是否命中 */
    private Boolean top3Hit;

    /** 返回的第一条结果内容 */
    private String topResult;

    /** 返回的第一条结果来源 */
    private String topResultSource;

    /** 检索得分 */
    private Double topScore;

    /** 检索耗时（ms） */
    private Long costMs;

    /** 返回结果总数 */
    private Integer resultCount;

    /** 评测时间 */
    private LocalDateTime evaluatedAt;
}
