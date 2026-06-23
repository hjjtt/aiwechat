package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.dto.KnowledgeEvidence;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult.ChunkResult;
import com.aiwechat.knowledge.model.entity.KnowledgeChunk;
import com.aiwechat.knowledge.model.entity.MenuItem;
import com.aiwechat.knowledge.repository.KnowledgeChunkRepository;
import com.aiwechat.knowledge.repository.MenuItemRepository;
import com.aiwechat.knowledge.service.StructuredKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StructuredKnowledgeServiceImpl implements StructuredKnowledgeService {

    private final MenuItemRepository menuItemRepository;
    private final KnowledgeChunkRepository chunkRepository;

    @Override
    public KnowledgeSearchResult search(String query, int topK) {
        long start = System.currentTimeMillis();
        List<KnowledgeEvidence> evidences = new ArrayList<>();
        evidences.addAll(searchMenuEvidence(query, topK));
        evidences.addAll(searchRuleEvidence(query, topK));

        List<KnowledgeEvidence> ranked = evidences.stream()
                .sorted(Comparator.comparingDouble(KnowledgeEvidence::getScore).reversed())
                .limit(topK)
                .toList();

        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setQuery(query);
        result.setSearchType("structured");
        result.setRouteType("structured");
        result.setCount(ranked.size());
        result.setCostMs(System.currentTimeMillis() - start);
        result.setEvidences(ranked);
        result.setResults(toChunkResults(ranked));
        return result;
    }

    private List<KnowledgeEvidence> searchMenuEvidence(String query, int topK) {
        if (!isMenuQuery(query)) {
            return List.of();
        }

        List<MenuItem> items = menuItemRepository.findAllAvailable();
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        String normalized = normalize(query);
        List<MenuItem> matched = items.stream()
                .filter(item -> matchesMenu(query, normalized, item))
                .sorted(Comparator.comparing((MenuItem item) -> item.getSalesCount() == null ? 0 : item.getSalesCount()).reversed())
                .limit(topK)
                .toList();

        if (matched.isEmpty() && containsAny(normalized, "推荐", "热销", "招牌", "菜单", "菜品")) {
            matched = items.stream()
                    .sorted(Comparator.comparing((MenuItem item) -> item.getSalesCount() == null ? 0 : item.getSalesCount()).reversed())
                    .limit(topK)
                    .toList();
        }

        return matched.stream()
                .map(item -> KnowledgeEvidence.builder()
                        .type("structured")
                        .source("menu_item")
                        .title(item.getName())
                        .content(buildMenuContent(item))
                        .score(menuScore(query, item))
                        .metadata(menuMetadata(item))
                        .build())
                .toList();
    }

    private List<KnowledgeEvidence> searchRuleEvidence(String query, int topK) {
        if (!isRuleQuery(query)) {
            return List.of();
        }

        List<KnowledgeChunk> chunks = chunkRepository.searchByKeywords(query, Math.max(topK * 3, 6));
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        return chunks.stream()
                .filter(chunk -> isRuleLikeChunk(query, chunk))
                .limit(topK)
                .map(chunk -> KnowledgeEvidence.builder()
                        .type("structured")
                        .source(resolveRuleSource(chunk))
                        .title(StringUtils.hasText(chunk.getSource()) ? chunk.getSource() : "业务规则")
                        .content(chunk.getContent())
                        .score(ruleScore(query, chunk))
                        .metadata(Map.of(
                                "documentId", String.valueOf(chunk.getDocumentId()),
                                "chunkType", nullToEmpty(chunk.getChunkType())
                        ))
                        .build())
                .toList();
    }

    private List<ChunkResult> toChunkResults(List<KnowledgeEvidence> evidences) {
        List<ChunkResult> chunks = new ArrayList<>();
        for (KnowledgeEvidence evidence : evidences) {
            ChunkResult chunk = new ChunkResult();
            chunk.setContent(evidence.getContent());
            chunk.setSource(evidence.getTitle());
            chunk.setTitle(evidence.getTitle());
            chunk.setScore(evidence.getScore());
            chunk.setEvidenceType(evidence.getType());
            chunk.setChunkType(evidence.getSource());
            chunk.setMetadata(evidence.getMetadata());
            chunks.add(chunk);
        }
        return chunks;
    }

    private boolean matchesMenu(String query, String normalized, MenuItem item) {
        return containsNormalized(normalized, item.getName())
                || containsNormalized(normalized, item.getCategory())
                || containsNormalized(normalized, item.getDescription())
                || query.contains(item.getName());
    }

    private double menuScore(String query, MenuItem item) {
        if (query.contains(item.getName())) {
            return 1.0;
        }
        if (StringUtils.hasText(item.getCategory()) && query.contains(item.getCategory())) {
            return 0.88;
        }
        return 0.78;
    }

    private String buildMenuContent(MenuItem item) {
        StringBuilder content = new StringBuilder();
        content.append("菜品名称：").append(item.getName()).append("\n");
        content.append("价格：").append(item.getPrice()).append("元\n");
        content.append("分类：").append(item.getCategory()).append("\n");
        if (StringUtils.hasText(item.getDescription())) {
            content.append("描述：").append(item.getDescription()).append("\n");
        }
        if (item.getSalesCount() != null) {
            content.append("销量：").append(item.getSalesCount()).append("份");
        }
        return content.toString();
    }

    private Map<String, Object> menuMetadata(MenuItem item) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("menuId", item.getId());
        metadata.put("category", item.getCategory());
        metadata.put("price", item.getPrice());
        metadata.put("salesCount", item.getSalesCount());
        return metadata;
    }

    private boolean isRuleLikeChunk(String query, KnowledgeChunk chunk) {
        String text = normalize(query + " " + nullToEmpty(chunk.getSource()) + " " + nullToEmpty(chunk.getChunkType()) + " " + nullToEmpty(chunk.getContent()));
        return containsAny(text, "配送", "起送", "送达", "退款", "取消", "订单", "售后", "发票", "营业", "优惠", "规则");
    }

    private String resolveRuleSource(KnowledgeChunk chunk) {
        String text = normalize(nullToEmpty(chunk.getSource()) + " " + nullToEmpty(chunk.getChunkType()));
        if (containsAny(text, "配送")) return "rule_delivery";
        if (containsAny(text, "订单", "退款", "取消", "售后")) return "rule_order";
        if (containsAny(text, "发票", "营业", "优惠")) return "faq";
        return "rule";
    }

    private double ruleScore(String query, KnowledgeChunk chunk) {
        String content = nullToEmpty(chunk.getContent());
        int hit = 0;
        for (String keyword : new String[]{"配送", "起送", "送达", "退款", "取消", "订单", "售后", "发票", "营业", "优惠"}) {
            if (query.contains(keyword) && content.contains(keyword)) {
                hit++;
            }
        }
        return Math.min(0.95, 0.7 + hit * 0.08);
    }

    private boolean isMenuQuery(String query) {
        String text = normalize(query);
        return containsAny(text, "多少钱", "价格", "售价", "菜单", "菜品", "推荐", "热销", "分类", "招牌", "汤", "饭", "菜");
    }

    private boolean isRuleQuery(String query) {
        String text = normalize(query);
        return containsAny(text, "配送", "配送费", "起送", "送达", "退款", "取消订单", "订单", "售后", "发票", "营业时间", "优惠");
    }

    private boolean containsNormalized(String text, String value) {
        return StringUtils.hasText(value) && text.contains(value.toLowerCase());
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}
