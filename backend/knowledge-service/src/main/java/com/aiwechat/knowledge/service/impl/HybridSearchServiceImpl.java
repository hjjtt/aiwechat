package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult.ChunkResult;
import com.aiwechat.knowledge.model.entity.KnowledgeChunk;
import com.aiwechat.knowledge.repository.KnowledgeChunkRepository;
import com.aiwechat.knowledge.service.HybridSearchService;
import com.aiwechat.knowledge.service.QueryRewriteService;
import com.aiwechat.knowledge.service.RerankService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchServiceImpl implements HybridSearchService {

    private final VectorStore vectorStore;
    private final KnowledgeChunkRepository chunkRepository;
    private final RerankService rerankService;
    private final QueryRewriteService queryRewriteService;
    private final ObjectMapper objectMapper;

    /** 向量召回权重 */
    private static final double VECTOR_WEIGHT = 0.7;
    /** 关键词召回权重 */
    private static final double KEYWORD_WEIGHT = 0.3;
    /** 粗召回倍数 */
    private static final int RECALL_MULTIPLIER = 3;

    @Override
    public KnowledgeSearchResult hybridSearch(String query, int topK) {
        return hybridSearch(query, topK, null);
    }

    @Override
    public KnowledgeSearchResult hybridSearch(String query, int topK, Map<String, Object> filters) {
        long startTime = System.currentTimeMillis();

        // 0. 查询改写
        String searchQuery = query;
        if (queryRewriteService.needsRewrite(query)) {
            searchQuery = queryRewriteService.rewrite(query);
            log.info("查询改写: '{}' -> '{}'", query, searchQuery);
        }

        // 1. 优先菜品名精确匹配
        KnowledgeSearchResult menuResult = menuExactSearch(query);
        if (menuResult != null && !menuResult.getResults().isEmpty()) {
            menuResult.setCostMs(System.currentTimeMillis() - startTime);
            menuResult.setSearchType("exact_menu");
            return menuResult;
        }

        // 2. 粗召回：向量 + 关键词
        int recallCount = topK * RECALL_MULTIPLIER;
        List<ChunkResult> vectorResults = doVectorSearch(searchQuery, recallCount, filters);
        List<ChunkResult> keywordResults = applyFilters(doKeywordSearch(query, recallCount), filters);

        // 3. 初步融合（取并集，保留所有候选）
        List<ChunkResult> allCandidates = mergeCandidates(vectorResults, keywordResults);

        // 4. 重排序精排
        List<ChunkResult> reranked = rerankService.rerank(query, allCandidates, topK);

        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setQuery(query);
        result.setCount(reranked.size());
        result.setResults(reranked);
        result.setCostMs(System.currentTimeMillis() - startTime);
        result.setSearchType("hybrid");

        log.info("混合检索完成 - query: '{}', 改写: '{}', 向量: {} 条, 关键词: {} 条, 候选: {} 条, 精排后: {} 条, 耗时: {}ms",
                query, searchQuery, vectorResults.size(), keywordResults.size(), allCandidates.size(), reranked.size(), result.getCostMs());
        return result;
    }

    @Override
    public KnowledgeSearchResult vectorSearch(String query, int topK) {
        long startTime = System.currentTimeMillis();
        List<ChunkResult> results = doVectorSearch(query, topK, null);

        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setQuery(query);
        result.setCount(results.size());
        result.setResults(results);
        result.setCostMs(System.currentTimeMillis() - startTime);
        result.setSearchType("vector");
        return result;
    }

    @Override
    public KnowledgeSearchResult keywordSearch(String query, int topK) {
        long startTime = System.currentTimeMillis();
        List<ChunkResult> results = doKeywordSearch(query, topK);

        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setQuery(query);
        result.setCount(results.size());
        result.setResults(results);
        result.setCostMs(System.currentTimeMillis() - startTime);
        result.setSearchType("keyword");
        return result;
    }

    @Override
    public KnowledgeSearchResult menuExactSearch(String menuName) {
        if (menuName == null || menuName.trim().isEmpty()) return null;

        // 在 chunk 表中查找菜品名精确匹配
        List<KnowledgeChunk> chunks = chunkRepository.searchByKeywords(menuName, 5);

        List<ChunkResult> results = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            // 检查是否精确包含菜品名
            if (chunk.getContent().contains("菜品名称：" + menuName) ||
                    chunk.getContent().contains("菜品名称:" + menuName) ||
                    chunk.getSource().equals(menuName)) {
                ChunkResult cr = new ChunkResult();
                cr.setContent(chunk.getContent());
                cr.setSource(chunk.getSource());
                cr.setScore(1.0);
                cr.setKeywordScore(1.0);
                cr.setChunkType(chunk.getChunkType());
                cr.setDocumentId(String.valueOf(chunk.getDocumentId()));
                results.add(cr);
            }
        }

        if (results.isEmpty()) return null;

        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setQuery(menuName);
        result.setCount(results.size());
        result.setResults(results);
        result.setSearchType("exact_menu");
        return result;
    }

    /**
     * 合并向量和关键词候选结果（去重）
     */
    private List<ChunkResult> mergeCandidates(List<ChunkResult> vectorResults, List<ChunkResult> keywordResults) {
        Map<String, ChunkResult> merged = new LinkedHashMap<>();

        for (ChunkResult vr : vectorResults) {
            String key = (vr.getDocumentId() != null ? vr.getDocumentId() : "") + "_" + vr.getContent().hashCode();
            merged.put(key, vr);
        }

        for (ChunkResult kr : keywordResults) {
            String key = (kr.getDocumentId() != null ? kr.getDocumentId() : "") + "_" + kr.getContent().hashCode();
            if (!merged.containsKey(key)) {
                merged.put(key, kr);
            }
        }

        return new ArrayList<>(merged.values());
    }

    // ==================== 私有方法 ====================

    private List<ChunkResult> doVectorSearch(String query, int topK, Map<String, Object> filters) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .build();
            List<Document> docs = vectorStore.similaritySearch(searchRequest);

            List<ChunkResult> results = new ArrayList<>();
            for (Document doc : docs) {
                ChunkResult cr = new ChunkResult();
                cr.setContent(doc.getText());
                cr.setSource(getMetaStr(doc, "source"));
                double distance = getMetaDouble(doc, "distance");
                // 距离越小越相似，转换为 0-1 的相似度分数
                double score = Math.max(0, 1.0 - distance);
                cr.setScore(score);
                cr.setVectorScore(score);
                cr.setChunkType(getMetaStr(doc, "chunkType"));
                cr.setDocumentId(getMetaStr(doc, "documentId"));
                cr.setMetadata(doc.getMetadata());
                cr.setEvidenceType("vector");
                cr.setTitle(cr.getSource());
                results.add(cr);
            }
            return applyFilters(results, filters);
        } catch (Exception e) {
            log.warn("向量检索失败，降级为关键词检索: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ChunkResult> doKeywordSearch(String query, int topK) {
        Set<String> seenIds = new HashSet<>();
        List<ChunkResult> results = new ArrayList<>();
        List<String> searchTerms = new ArrayList<>();

        // 1. 完整 query 作为第一个搜索词
        searchTerms.add(query);

        // 2. 按 2-gram 切分中文 query（滑动窗口提取二元组）
        if (query.length() >= 4) {
            for (int i = 0; i <= query.length() - 2; i++) {
                String bigram = query.substring(i, i + 2);
                if (!searchTerms.contains(bigram)) {
                    searchTerms.add(bigram);
                }
            }
        }

        // 3. 按标点和空格分词
        String[] words = query.split("[\\s,，。？！?!.、]+");
        for (String word : words) {
            if (word.length() >= 2 && !searchTerms.contains(word)) {
                searchTerms.add(word);
            }
        }

        for (String keyword : searchTerms) {
            List<KnowledgeChunk> chunks = chunkRepository.searchByKeywords(keyword, topK * 2);
            for (KnowledgeChunk chunk : chunks) {
                if (seenIds.contains(chunk.getId())) continue;
                seenIds.add(chunk.getId());

                ChunkResult cr = new ChunkResult();
                cr.setContent(chunk.getContent());
                cr.setSource(chunk.getSource());

                // 关键词匹配得分
                int matchCount = 0;
                String content = chunk.getContent();
                // 检查完整 query 的子串匹配
                for (int len = Math.min(query.length(), 4); len >= 2; len--) {
                    for (int i = 0; i <= query.length() - len; i++) {
                        String sub = query.substring(i, i + len);
                        if (content.contains(sub)) {
                            matchCount += len;
                            break;
                        }
                    }
                }
                double keywordScore = Math.min(1.0, (double) matchCount / query.length());
                cr.setScore(keywordScore);
                cr.setKeywordScore(keywordScore);
                cr.setChunkType(chunk.getChunkType());
                cr.setDocumentId(String.valueOf(chunk.getDocumentId()));
                cr.setMetadata(parseMetadata(chunk.getMetadata()));
                cr.setEvidenceType("vector");
                cr.setTitle(chunk.getSource());
                results.add(cr);
            }
        }

        // 按关键词得分排序
        results.sort(Comparator.comparingDouble(ChunkResult::getScore).reversed());
        if (results.size() > topK) {
            results = results.subList(0, topK);
        }
        return results;
    }

    private List<ChunkResult> applyFilters(List<ChunkResult> results, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return results;
        }
        Map<String, Object> effectiveFilters = normalizeFilters(filters);
        if (effectiveFilters.isEmpty()) {
            return results;
        }
        return results.stream()
                .filter(result -> matchesFilters(result, effectiveFilters))
                .toList();
    }

    private Map<String, Object> normalizeFilters(Map<String, Object> filters) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key == null || value == null) {
                continue;
            }
            String valueText = value.toString();
            if (valueText.isBlank() || "query".equals(key) || "topK".equals(key)) {
                continue;
            }
            normalized.put(key, valueText);
        }
        return normalized;
    }

    private boolean matchesFilters(ChunkResult result, Map<String, Object> filters) {
        Map<String, Object> metadata = result.getMetadata() != null ? result.getMetadata() : Map.of();
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            String key = filter.getKey();
            String expected = filter.getValue().toString();
            Object actual = metadata.get(key);
            if (actual == null && "chunkType".equals(key)) {
                actual = result.getChunkType();
            }
            if (actual == null && "documentId".equals(key)) {
                actual = result.getDocumentId();
            }
            if (actual == null && "source".equals(key)) {
                actual = result.getSource();
            }
            if (actual == null || !expected.equalsIgnoreCase(actual.toString())) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> parseMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadata, new TypeReference<>() {});
        } catch (Exception e) {
            log.debug("chunk metadata 解析失败: {}", e.getMessage());
            return Map.of();
        }
    }

    private String getMetaStr(Document doc, String key) {
        Object val = doc.getMetadata().get(key);
        return val != null ? val.toString() : null;
    }

    private double getMetaDouble(Document doc, String key) {
        Object val = doc.getMetadata().get(key);
        return val != null ? Double.parseDouble(val.toString()) : 0.0;
    }
}
