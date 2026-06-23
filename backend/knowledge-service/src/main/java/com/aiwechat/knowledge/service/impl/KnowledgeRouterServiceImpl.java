package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.dto.KnowledgeEvidence;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult.ChunkResult;
import com.aiwechat.knowledge.model.dto.QueryRouteResult;
import com.aiwechat.knowledge.service.HybridSearchService;
import com.aiwechat.knowledge.service.KnowledgeRouterService;
import com.aiwechat.knowledge.service.LazyGraphRagService;
import com.aiwechat.knowledge.service.QueryRoutingService;
import com.aiwechat.knowledge.service.StructuredKnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgeRouterServiceImpl implements KnowledgeRouterService {

    private final QueryRoutingService queryRoutingService;
    private final StructuredKnowledgeService structuredKnowledgeService;
    private final LazyGraphRagService lazyGraphRagService;
    private final HybridSearchService hybridSearchService;

    @Override
    public QueryRouteResult route(String query) {
        return queryRoutingService.route(query);
    }

    @Override
    public KnowledgeSearchResult retrieve(String query, int topK) {
        QueryRouteResult route = queryRoutingService.route(query);

        if ("structured".equals(route.getRouteType())) {
            KnowledgeSearchResult structured = structuredKnowledgeService.search(query, topK);
            applyRoute(structured, route, false);
            if (structured.getResults() != null && !structured.getResults().isEmpty()) {
                return structured;
            }

            KnowledgeSearchResult fallback = hybridSearchService.hybridSearch(query, topK);
            applyRoute(fallback, route, true);
            fallback.setSearchType("structured_fallback_hybrid");
            ensureEvidences(fallback, "vector");
            return fallback;
        }

        if ("lazy_graph".equals(route.getRouteType())) {
            KnowledgeSearchResult lazyGraph = lazyGraphRagService.search(query, topK);
            applyRoute(lazyGraph, route, false);
            if (lazyGraph.getResults() != null && !lazyGraph.getResults().isEmpty()) {
                return lazyGraph;
            }

            KnowledgeSearchResult fallback = hybridSearchService.hybridSearch(query, topK);
            applyRoute(fallback, route, true);
            fallback.setSearchType("lazy_graph_fallback_hybrid");
            ensureEvidences(fallback, "vector");
            return fallback;
        }

        KnowledgeSearchResult result = hybridSearchService.hybridSearch(query, topK);
        applyRoute(result, route, false);
        ensureEvidences(result, "vector");
        return result;
    }

    private void applyRoute(KnowledgeSearchResult result, QueryRouteResult route, boolean fallback) {
        result.setRouteType(route.getRouteType());
        result.setRouteReason(route.getReason());
        result.setFallback(fallback);
    }

    private void ensureEvidences(KnowledgeSearchResult result, String type) {
        if (result.getEvidences() != null && !result.getEvidences().isEmpty()) {
            return;
        }
        if (result.getResults() == null) {
            result.setEvidences(List.of());
            return;
        }
        List<KnowledgeEvidence> evidences = result.getResults().stream()
                .map(chunk -> toEvidence(chunk, type))
                .toList();
        result.setEvidences(evidences);
        for (ChunkResult chunk : result.getResults()) {
            if (chunk.getEvidenceType() == null) {
                chunk.setEvidenceType(type);
            }
            if (chunk.getTitle() == null) {
                chunk.setTitle(chunk.getSource());
            }
        }
    }

    private KnowledgeEvidence toEvidence(ChunkResult chunk, String type) {
        return KnowledgeEvidence.builder()
                .type(type)
                .source(chunk.getChunkType() != null ? chunk.getChunkType() : "vector_chunk")
                .title(chunk.getSource())
                .content(chunk.getContent())
                .score(chunk.getScore())
                .metadata(chunk.getMetadata() != null ? chunk.getMetadata() : Map.of())
                .build();
    }
}
