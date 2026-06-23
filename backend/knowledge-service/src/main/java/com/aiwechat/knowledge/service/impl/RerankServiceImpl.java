package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult.ChunkResult;
import com.aiwechat.knowledge.service.RerankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 多因子融合重排序实现
 * 不依赖外部 rerank 模型，使用规则化评分融合
 */
@Slf4j
@Service
public class RerankServiceImpl implements RerankService {

    /** 向量相似度权重 */
    private static final double W_VECTOR = 0.4;
    /** 关键词匹配权重 */
    private static final double W_KEYWORD = 0.25;
    /** chunk 类型权重 */
    private static final double W_TYPE = 0.15;
    /** 内容长度权重（适中长度加分） */
    private static final double W_LENGTH = 0.1;
    /** 精确匹配权重 */
    private static final double W_EXACT = 0.1;

    @Override
    public List<ChunkResult> rerank(String query, List<ChunkResult> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        String lowerQuery = query.toLowerCase();
        String[] queryWords = lowerQuery.split("[\\s,，。？！?!.、]+");

        for (ChunkResult candidate : candidates) {
            double rerankScore = computeRerankScore(query, queryWords, candidate);
            candidate.setScore(rerankScore);
        }

        List<ChunkResult> sorted = candidates.stream()
                .sorted(Comparator.comparingDouble(ChunkResult::getScore).reversed())
                .limit(topK)
                .toList();

        log.debug("重排序完成 - 输入: {} 条, 输出: {} 条", candidates.size(), sorted.size());
        return sorted;
    }

    private double computeRerankScore(String query, String[] queryWords, ChunkResult candidate) {
        double score = 0;
        String content = candidate.getContent() != null ? candidate.getContent().toLowerCase() : "";
        String source = candidate.getSource() != null ? candidate.getSource().toLowerCase() : "";

        // 1. 向量相似度得分
        double vecScore = candidate.getVectorScore() != null ? candidate.getVectorScore() : 0;
        score += W_VECTOR * normalize(vecScore);

        // 2. 关键词匹配得分
        double kwScore = candidate.getKeywordScore() != null ? candidate.getKeywordScore() : 0;
        score += W_KEYWORD * normalize(kwScore);

        // 3. chunk 类型得分（菜单和规则类优先）
        double typeScore = getTypeScore(candidate.getChunkType(), queryWords);
        score += W_TYPE * typeScore;

        // 4. 内容长度得分（适中长度 50-300 字最佳）
        double lengthScore = getLengthScore(content.length());
        score += W_LENGTH * lengthScore;

        // 5. 精确匹配加分
        double exactScore = getExactMatchScore(queryWords, content, source);
        score += W_EXACT * exactScore;

        return score;
    }

    private double normalize(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private double getTypeScore(String chunkType, String[] queryWords) {
        if (chunkType == null) return 0.5;

        // 根据查询意图判断优先类型
        boolean isMenuQuery = containsAny(queryWords, "菜", "价格", "多少钱", "推荐", "有什么");
        boolean isRuleQuery = containsAny(queryWords, "配送", "规则", "能否", "可以", "多久", "时间");
        boolean isFaqQuery = containsAny(queryWords, "为什么", "怎么", "如何", "什么");

        return switch (chunkType) {
            case "menu" -> isMenuQuery ? 1.0 : 0.6;
            case "rule" -> isRuleQuery ? 1.0 : 0.5;
            case "faq" -> isFaqQuery ? 1.0 : 0.5;
            default -> 0.4;
        };
    }

    private double getLengthScore(int length) {
        // 最佳长度 50-300 字
        if (length < 20) return 0.3;
        if (length < 50) return 0.6;
        if (length <= 300) return 1.0;
        if (length <= 500) return 0.7;
        return 0.4;
    }

    private double getExactMatchScore(String[] queryWords, String content, String source) {
        if (queryWords.length == 0) return 0;

        int matches = 0;
        for (String word : queryWords) {
            if (word.length() < 2) continue;
            if (content.contains(word) || source.contains(word)) {
                matches++;
            }
        }

        return (double) matches / queryWords.length;
    }

    private boolean containsAny(String[] words, String... targets) {
        for (String word : words) {
            for (String target : targets) {
                if (word.contains(target) || target.contains(word)) return true;
            }
        }
        return false;
    }
}
