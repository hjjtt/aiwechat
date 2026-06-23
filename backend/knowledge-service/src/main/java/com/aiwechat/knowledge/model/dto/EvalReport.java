package com.aiwechat.knowledge.model.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class EvalReport {

    /** 总评测数量 */
    private int totalCases;

    /** Top1 命中数 */
    private int top1Hits;

    /** Top3 命中数 */
    private int top3Hits;

    /** Top5 命中数 */
    private int top5Hits;

    /** 无结果数 */
    private int noResults;

    /** 错召回数 */
    private int wrongRecalls;

    /** Top3 外命中数（说明召回到了，但排序偏后） */
    private int rankingWeakCases;

    /** Top1 命中率 */
    private double top1HitRate;

    /** Top3 命中率 */
    private double top3HitRate;

    /** Top5 命中率 */
    private double top5HitRate;

    /** 无答案率 */
    private double noAnswerRate;

    /** 错召率 */
    private double wrongRecallRate;

    /** Top3 外命中率 */
    private double rankingWeakRate;

    /** 返回任意结果的覆盖率 */
    private double resultCoverageRate;

    /** 平均耗时（ms） */
    private double avgCostMs;

    /** 按分类统计 */
    private Map<String, CategoryStats> categoryStats;

    /** 按搜索类型统计：exact_menu, hybrid, keyword, vector */
    private Map<String, SearchStats> searchTypeStats;

    /** 按实际检索模式统计：exact_menu, keyword_degraded, hybrid_mixed 等 */
    private Map<String, SearchStats> retrievalModeStats;

    /** 失败原因统计 */
    private Map<String, Integer> failureReasonStats;

    /** 典型失败样本 */
    private List<FailureSample> failureSamples;

    @Data
    public static class CategoryStats {
        private int total;
        private int top1Hits;
        private int top3Hits;
        private int top5Hits;
        private int noResults;
        private int wrongRecalls;
        private double top1Rate;
        private double top3Rate;
        private double top5Rate;
        private double noResultRate;
        private double avgCostMs;
    }

    @Data
    public static class SearchStats {
        private int total;
        private int top1Hits;
        private int top3Hits;
        private int top5Hits;
        private double top1Rate;
        private double top3Rate;
        private double top5Rate;
        private double avgCostMs;
    }

    @Data
    public static class FailureSample {
        private String question;
        private String category;
        private String difficulty;
        private String searchType;
        private String retrievalMode;
        private String failureReason;
        private String expectedSource;
        private String expectedAnswer;
        private String topResultSource;
        private String topResultExcerpt;
    }
}
