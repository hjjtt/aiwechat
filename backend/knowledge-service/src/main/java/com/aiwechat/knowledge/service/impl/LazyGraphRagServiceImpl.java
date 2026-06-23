package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.dto.KnowledgeEvidence;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult.ChunkResult;
import com.aiwechat.knowledge.model.dto.LazyGraphIndexReport;
import com.aiwechat.knowledge.model.entity.KnowledgeChunk;
import com.aiwechat.knowledge.model.entity.KnowledgeConcept;
import com.aiwechat.knowledge.model.entity.KnowledgeConceptChunk;
import com.aiwechat.knowledge.model.entity.KnowledgeConceptEdge;
import com.aiwechat.knowledge.repository.KnowledgeChunkRepository;
import com.aiwechat.knowledge.repository.KnowledgeConceptChunkRepository;
import com.aiwechat.knowledge.repository.KnowledgeConceptEdgeRepository;
import com.aiwechat.knowledge.repository.KnowledgeConceptRepository;
import com.aiwechat.knowledge.service.LazyGraphRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LazyGraphRagServiceImpl implements LazyGraphRagService {

    private static final int MAX_CONCEPTS_PER_CHUNK = 12;
    private static final Pattern LABELED_CONCEPT_PATTERN = Pattern.compile("(菜品名称|分类|来源|标题|问题|答案|规则)[:：]\\s*([^\\n，,。；;]+)");
    private static final Pattern CHINESE_PHRASE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5A-Za-z0-9]{2,18}");

    private static final List<String> DOMAIN_CONCEPTS = List.of(
            "配送费", "配送范围", "起送价", "免配送费", "配送时间", "营业时间", "取消订单",
            "退款", "售后", "发票", "优惠", "推荐菜", "热销菜", "套餐", "过敏原",
            "配料", "门店", "自取", "催单", "订单状态", "支付", "满减"
    );

    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeConceptRepository conceptRepository;
    private final KnowledgeConceptChunkRepository conceptChunkRepository;
    private final KnowledgeConceptEdgeRepository conceptEdgeRepository;

    @Override
    public List<String> extractConcepts(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        Set<String> concepts = new LinkedHashSet<>();
        for (String concept : DOMAIN_CONCEPTS) {
            if (text.contains(concept)) {
                concepts.add(concept);
            }
        }

        Matcher labelMatcher = LABELED_CONCEPT_PATTERN.matcher(text);
        while (labelMatcher.find()) {
            addCleanConcept(concepts, labelMatcher.group(2));
        }

        Matcher phraseMatcher = CHINESE_PHRASE_PATTERN.matcher(text);
        while (phraseMatcher.find() && concepts.size() < MAX_CONCEPTS_PER_CHUNK) {
            String phrase = phraseMatcher.group();
            if (isUsefulConcept(phrase)) {
                addCleanConcept(concepts, phrase);
            }
        }

        return concepts.stream()
                .filter(StringUtils::hasText)
                .limit(MAX_CONCEPTS_PER_CHUNK)
                .toList();
    }

    @Override
    @Transactional
    public LazyGraphIndexReport rebuildIndex() {
        long start = System.currentTimeMillis();
        conceptEdgeRepository.deleteAll();
        conceptChunkRepository.deleteAll();
        conceptRepository.deleteAll();

        List<KnowledgeChunk> chunks = chunkRepository.selectList(null);
        if (chunks == null || chunks.isEmpty()) {
            return LazyGraphIndexReport.builder().build();
        }

        Map<String, KnowledgeConcept> conceptMap = new LinkedHashMap<>();
        List<KnowledgeConceptChunk> mentions = new ArrayList<>();
        Map<String, EdgeAccumulator> edgeMap = new LinkedHashMap<>();
        int chunksIndexed = 0;

        for (KnowledgeChunk chunk : chunks) {
            List<String> concepts = extractConcepts(buildConceptSourceText(chunk));
            if (concepts.isEmpty()) {
                continue;
            }
            chunksIndexed++;

            List<KnowledgeConcept> chunkConcepts = new ArrayList<>();
            for (String conceptName : concepts) {
                KnowledgeConcept concept = conceptMap.computeIfAbsent(normalize(conceptName), ignored -> createConcept(conceptName, chunk));
                concept.setFrequency(concept.getFrequency() + 1);
                chunkConcepts.add(concept);
            }

            for (KnowledgeConcept concept : chunkConcepts) {
                KnowledgeConceptChunk mention = new KnowledgeConceptChunk();
                mention.setConceptId(concept.getId());
                mention.setChunkId(chunk.getId());
                mention.setDocumentId(chunk.getDocumentId());
                mention.setWeight(computeMentionWeight(concept.getName(), chunk));
                mention.setSource(chunk.getSource());
                mention.setCreatedAt(LocalDateTime.now());
                mentions.add(mention);
            }

            for (int i = 0; i < chunkConcepts.size(); i++) {
                for (int j = i + 1; j < chunkConcepts.size(); j++) {
                    Long left = chunkConcepts.get(i).getId();
                    Long right = chunkConcepts.get(j).getId();
                    String key = edgeKey(left, right);
                    edgeMap.computeIfAbsent(key, ignored -> new EdgeAccumulator(Math.min(left, right), Math.max(left, right)))
                            .increment();
                }
            }
        }

        for (KnowledgeConcept concept : conceptMap.values()) {
            conceptRepository.incrementFrequency(concept.getId(), concept.getFrequency());
        }

        if (!mentions.isEmpty()) {
            conceptChunkRepository.batchInsert(mentions);
        }
        List<KnowledgeConceptEdge> edges = edgeMap.values().stream()
                .map(EdgeAccumulator::toEdge)
                .toList();
        if (!edges.isEmpty()) {
            conceptEdgeRepository.batchInsert(edges);
        }

        LazyGraphIndexReport report = LazyGraphIndexReport.builder()
                .chunksScanned(chunks.size())
                .chunksIndexed(chunksIndexed)
                .conceptsCreated(conceptMap.size())
                .conceptMentionsCreated(mentions.size())
                .edgesCreated(edges.size())
                .build();
        log.info("LazyGraphRAG 索引重建完成: {}, costMs={}", report, System.currentTimeMillis() - start);
        return report;
    }

    @Override
    public KnowledgeSearchResult search(String query, int topK) {
        long start = System.currentTimeMillis();
        List<String> queryConcepts = extractConcepts(query);
        List<String> normalizedNames = queryConcepts.stream().map(this::normalize).toList();

        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setQuery(query);
        result.setRouteType("lazy_graph");
        result.setSearchType("lazy_graph");

        if (normalizedNames.isEmpty()) {
            result.setCount(0);
            result.setResults(List.of());
            result.setEvidences(List.of());
            result.setCostMs(System.currentTimeMillis() - start);
            return result;
        }

        List<KnowledgeConcept> matchedConcepts = conceptRepository.selectByNormalizedNames(normalizedNames);
        if (matchedConcepts == null || matchedConcepts.isEmpty()) {
            result.setCount(0);
            result.setResults(List.of());
            result.setEvidences(List.of());
            result.setCostMs(System.currentTimeMillis() - start);
            return result;
        }

        Set<Long> conceptIds = matchedConcepts.stream().map(KnowledgeConcept::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<KnowledgeConceptEdge> neighborEdges = conceptEdgeRepository.selectNeighbors(new ArrayList<>(conceptIds), Math.max(topK * 4, 12));
        if (neighborEdges != null) {
            for (KnowledgeConceptEdge edge : neighborEdges) {
                conceptIds.add(edge.getSourceConceptId());
                conceptIds.add(edge.getTargetConceptId());
            }
        }

        List<KnowledgeConceptChunk> conceptChunks = conceptChunkRepository.selectByConceptIds(new ArrayList<>(conceptIds), Math.max(topK * 8, 20));
        List<ScoredChunk> scoredChunks = scoreChunks(conceptChunks, matchedConcepts, conceptIds);
        List<ScoredChunk> topChunks = scoredChunks.stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .toList();

        List<KnowledgeEvidence> evidences = topChunks.stream()
                .map(item -> toEvidence(item, queryConcepts, matchedConcepts))
                .toList();
        result.setEvidences(evidences);
        result.setResults(evidences.stream().map(this::toChunkResult).toList());
        result.setCount(evidences.size());
        result.setCostMs(System.currentTimeMillis() - start);
        return result;
    }

    private List<ScoredChunk> scoreChunks(List<KnowledgeConceptChunk> conceptChunks,
                                          List<KnowledgeConcept> matchedConcepts,
                                          Set<Long> expandedConceptIds) {
        if (conceptChunks == null || conceptChunks.isEmpty()) {
            return List.of();
        }
        Set<Long> directConceptIds = matchedConcepts.stream().map(KnowledgeConcept::getId).collect(Collectors.toSet());
        Map<String, ScoredChunk> scored = new LinkedHashMap<>();

        for (KnowledgeConceptChunk conceptChunk : conceptChunks) {
            KnowledgeChunk chunk = chunkRepository.selectById(conceptChunk.getChunkId());
            if (chunk == null) {
                continue;
            }
            ScoredChunk current = scored.computeIfAbsent(chunk.getId(), ignored -> new ScoredChunk(chunk, 0.0));
            double boost = directConceptIds.contains(conceptChunk.getConceptId()) ? 1.0 : 0.45;
            if (expandedConceptIds.contains(conceptChunk.getConceptId())) {
                boost += 0.15;
            }
            current.addScore((conceptChunk.getWeight() == null ? 1.0 : conceptChunk.getWeight()) * boost);
        }
        return new ArrayList<>(scored.values());
    }

    private KnowledgeEvidence toEvidence(ScoredChunk item, List<String> queryConcepts, List<KnowledgeConcept> matchedConcepts) {
        KnowledgeChunk chunk = item.chunk();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("queryConcepts", queryConcepts);
        metadata.put("matchedConcepts", matchedConcepts.stream().map(KnowledgeConcept::getName).toList());
        metadata.put("documentId", chunk.getDocumentId());
        metadata.put("chunkId", chunk.getId());
        metadata.put("chunkType", chunk.getChunkType());

        return KnowledgeEvidence.builder()
                .type("lazy_graph")
                .source("concept_graph")
                .title(StringUtils.hasText(chunk.getSource()) ? chunk.getSource() : "概念图证据")
                .content(chunk.getContent())
                .score(Math.min(1.0, item.score() / 4.0))
                .metadata(metadata)
                .build();
    }

    private ChunkResult toChunkResult(KnowledgeEvidence evidence) {
        ChunkResult chunk = new ChunkResult();
        chunk.setContent(evidence.getContent());
        chunk.setSource(evidence.getTitle());
        chunk.setTitle(evidence.getTitle());
        chunk.setScore(evidence.getScore());
        chunk.setEvidenceType("lazy_graph");
        chunk.setChunkType(evidence.getSource());
        chunk.setMetadata(evidence.getMetadata());
        return chunk;
    }

    private KnowledgeConcept createConcept(String name, KnowledgeChunk chunk) {
        KnowledgeConcept concept = new KnowledgeConcept();
        concept.setName(name);
        concept.setNormalizedName(normalize(name));
        concept.setConceptType(resolveConceptType(name, chunk));
        concept.setFrequency(0);
        concept.setCreatedAt(LocalDateTime.now());
        concept.setUpdatedAt(LocalDateTime.now());
        conceptRepository.insert(concept);
        return concept;
    }

    private String resolveConceptType(String name, KnowledgeChunk chunk) {
        String text = normalize(name + " " + nullToEmpty(chunk.getSource()) + " " + nullToEmpty(chunk.getChunkType()));
        if (containsAny(text, "菜", "汤", "饭", "菜单", "推荐", "热销")) return "menu";
        if (containsAny(text, "配送", "订单", "退款", "取消", "售后", "规则")) return "rule";
        if (containsAny(text, "发票", "营业", "自取", "催单")) return "faq";
        return "concept";
    }

    private double computeMentionWeight(String concept, KnowledgeChunk chunk) {
        double weight = 1.0;
        if (StringUtils.hasText(chunk.getSource()) && chunk.getSource().contains(concept)) {
            weight += 1.0;
        }
        if (StringUtils.hasText(chunk.getKeywords()) && chunk.getKeywords().contains(concept)) {
            weight += 0.5;
        }
        if (StringUtils.hasText(chunk.getChunkType()) && ("menu".equals(chunk.getChunkType()) || "rule".equals(chunk.getChunkType()))) {
            weight += 0.25;
        }
        return weight;
    }

    private String buildConceptSourceText(KnowledgeChunk chunk) {
        return String.join("\n",
                nullToEmpty(chunk.getSource()),
                nullToEmpty(chunk.getChunkType()),
                nullToEmpty(chunk.getKeywords()),
                nullToEmpty(chunk.getContent()));
    }

    private void addCleanConcept(Set<String> concepts, String raw) {
        String cleaned = raw == null ? "" : raw
                .replaceAll("[：:，,。；;、\\[\\]（）()]", " ")
                .trim();
        if (cleaned.length() >= 2 && cleaned.length() <= 18) {
            concepts.add(cleaned);
        }
    }

    private boolean isUsefulConcept(String phrase) {
        if (phrase.length() < 2 || phrase.length() > 18) {
            return false;
        }
        if (phrase.matches("\\d+")) {
            return false;
        }
        return containsAny(phrase, "菜", "汤", "饭", "配送", "订单", "退款", "售后", "发票",
                "营业", "优惠", "规则", "套餐", "门店", "自取", "催单", "价格", "分类", "时间");
    }

    private String edgeKey(Long left, Long right) {
        return Math.min(left, right) + ":" + Math.max(left, right);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase().replaceAll("\\s+", "");
    }

    private boolean containsAny(String text, String... keywords) {
        String normalized = normalize(text);
        for (String keyword : keywords) {
            if (normalized.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static class ScoredChunk {
        private final KnowledgeChunk chunk;
        private double score;

        private ScoredChunk(KnowledgeChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }

        private KnowledgeChunk chunk() {
            return chunk;
        }

        private double score() {
            return score;
        }

        private void addScore(double delta) {
            this.score += delta;
        }
    }

    private static class EdgeAccumulator {
        private final Long source;
        private final Long target;
        private int weight = 0;

        private EdgeAccumulator(Long source, Long target) {
            this.source = source;
            this.target = target;
        }

        private void increment() {
            weight++;
        }

        private KnowledgeConceptEdge toEdge() {
            KnowledgeConceptEdge edge = new KnowledgeConceptEdge();
            edge.setSourceConceptId(source);
            edge.setTargetConceptId(target);
            edge.setEdgeType("co_occurs");
            edge.setWeight(weight);
            edge.setCreatedAt(LocalDateTime.now());
            edge.setUpdatedAt(LocalDateTime.now());
            return edge;
        }
    }
}
