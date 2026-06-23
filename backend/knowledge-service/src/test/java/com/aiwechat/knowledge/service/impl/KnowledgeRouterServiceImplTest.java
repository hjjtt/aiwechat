package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.QueryRouteResult;
import com.aiwechat.knowledge.service.HybridSearchService;
import com.aiwechat.knowledge.service.LazyGraphRagService;
import com.aiwechat.knowledge.service.QueryRoutingService;
import com.aiwechat.knowledge.service.StructuredKnowledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeRouterServiceImplTest {

    @Mock
    private QueryRoutingService queryRoutingService;

    @Mock
    private StructuredKnowledgeService structuredKnowledgeService;

    @Mock
    private LazyGraphRagService lazyGraphRagService;

    @Mock
    private HybridSearchService hybridSearchService;

    private KnowledgeRouterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeRouterServiceImpl(queryRoutingService, structuredKnowledgeService, lazyGraphRagService, hybridSearchService);
    }

    @Test
    @DisplayName("结构化路由命中时应直接返回结构化结果")
    void retrieveShouldReturnStructuredResultWhenHit() {
        when(queryRoutingService.route("宫保鸡丁多少钱")).thenReturn(route("structured", "structured"));
        KnowledgeSearchResult structured = result("structured", 1);
        when(structuredKnowledgeService.search("宫保鸡丁多少钱", 3)).thenReturn(structured);

        KnowledgeSearchResult result = service.retrieve("宫保鸡丁多少钱", 3);

        assertEquals("structured", result.getRouteType());
        assertFalse(result.isFallback());
        verify(hybridSearchService, never()).hybridSearch("宫保鸡丁多少钱", 3);
    }

    @Test
    @DisplayName("结构化路由未命中时应降级到混合检索")
    void retrieveShouldFallbackToHybridWhenStructuredMiss() {
        when(queryRoutingService.route("配送费怎么算")).thenReturn(route("structured", "structured"));
        when(structuredKnowledgeService.search("配送费怎么算", 3)).thenReturn(result("structured", 0));
        when(hybridSearchService.hybridSearch("配送费怎么算", 3)).thenReturn(result("hybrid", 1));

        KnowledgeSearchResult result = service.retrieve("配送费怎么算", 3);

        assertEquals("structured", result.getRouteType());
        assertEquals("structured_fallback_hybrid", result.getSearchType());
        assertTrue(result.isFallback());
        assertEquals(1, result.getEvidences().size());
    }

    @Test
    @DisplayName("LazyGraphRAG 路由命中时应直接返回概念图结果")
    void retrieveShouldUseLazyGraphResult() {
        when(queryRoutingService.route("套餐包含哪些菜")).thenReturn(route("lazy_graph", "lazy_graph"));
        when(lazyGraphRagService.search("套餐包含哪些菜", 3)).thenReturn(result("lazy_graph", 1));

        KnowledgeSearchResult result = service.retrieve("套餐包含哪些菜", 3);

        assertEquals("lazy_graph", result.getRouteType());
        assertEquals("lazy_graph", result.getSearchType());
        assertFalse(result.isFallback());
        verify(hybridSearchService, never()).hybridSearch("套餐包含哪些菜", 3);
        verify(structuredKnowledgeService, never()).search("套餐包含哪些菜", 3);
    }

    @Test
    @DisplayName("LazyGraphRAG 未命中时应走混合检索兜底")
    void retrieveShouldFallbackWhenLazyGraphMiss() {
        when(queryRoutingService.route("套餐包含哪些菜")).thenReturn(route("lazy_graph", "lazy_graph"));
        when(lazyGraphRagService.search("套餐包含哪些菜", 3)).thenReturn(result("lazy_graph", 0));
        when(hybridSearchService.hybridSearch("套餐包含哪些菜", 3)).thenReturn(result("hybrid", 1));

        KnowledgeSearchResult result = service.retrieve("套餐包含哪些菜", 3);

        assertEquals("lazy_graph", result.getRouteType());
        assertEquals("lazy_graph_fallback_hybrid", result.getSearchType());
        assertTrue(result.isFallback());
        verify(structuredKnowledgeService, never()).search("套餐包含哪些菜", 3);
    }

    private QueryRouteResult route(String routeType, String searchType) {
        return QueryRouteResult.builder()
                .query("q")
                .routeType(routeType)
                .searchType(searchType)
                .reason("测试路由")
                .build();
    }

    private KnowledgeSearchResult result(String searchType, int count) {
        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setSearchType(searchType);
        result.setCount(count);
        result.setResults(count > 0 ? List.of(chunk()) : List.of());
        return result;
    }

    private KnowledgeSearchResult.ChunkResult chunk() {
        KnowledgeSearchResult.ChunkResult chunk = new KnowledgeSearchResult.ChunkResult();
        chunk.setContent("证据内容");
        chunk.setSource("证据来源");
        chunk.setScore(0.8);
        return chunk;
    }
}
