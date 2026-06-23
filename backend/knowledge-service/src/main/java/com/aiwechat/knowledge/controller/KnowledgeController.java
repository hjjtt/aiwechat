package com.aiwechat.knowledge.controller;

import com.aiwechat.common.model.dto.ApiResponse;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.QueryRouteResult;
import com.aiwechat.knowledge.service.HybridSearchService;
import com.aiwechat.knowledge.service.KnowledgeBaseService;
import com.aiwechat.knowledge.service.KnowledgeRouterService;
import com.aiwechat.knowledge.service.LazyGraphRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final HybridSearchService hybridSearchService;
    private final KnowledgeRouterService knowledgeRouterService;
    private final LazyGraphRagService lazyGraphRagService;

    @GetMapping("/search")
    public ApiResponse<KnowledgeSearchResult> search(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {
        KnowledgeSearchResult result = knowledgeRouterService.retrieve(query, topK);
        return ApiResponse.success(result);
    }

    /**
     * 统一检索入口：先路由，再执行结构化/混合/图谱兜底检索。
     */
    @GetMapping("/retrieve")
    public ApiResponse<KnowledgeSearchResult> retrieve(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {
        KnowledgeSearchResult result = knowledgeRouterService.retrieve(query, topK);
        return ApiResponse.success(result);
    }

    /**
     * Query 路由调试。
     */
    @GetMapping("/route")
    public ApiResponse<QueryRouteResult> route(@RequestParam("query") String query) {
        return ApiResponse.success(knowledgeRouterService.route(query));
    }

    /**
     * 混合检索接口（向量 + 关键词 + metadata 过滤）
     */
    @GetMapping("/hybrid-search")
    public ApiResponse<KnowledgeSearchResult> hybridSearch(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK,
            @RequestParam Map<String, Object> filters) {
        KnowledgeSearchResult result = hybridSearchService.hybridSearch(query, topK, filters);
        return ApiResponse.success(result);
    }

    /**
     * 纯向量检索
     */
    @GetMapping("/vector-search")
    public ApiResponse<KnowledgeSearchResult> vectorSearch(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {
        KnowledgeSearchResult result = hybridSearchService.vectorSearch(query, topK);
        return ApiResponse.success(result);
    }

    /**
     * 纯关键词检索
     */
    @GetMapping("/keyword-search")
    public ApiResponse<KnowledgeSearchResult> keywordSearch(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {
        KnowledgeSearchResult result = hybridSearchService.keywordSearch(query, topK);
        return ApiResponse.success(result);
    }

    /**
     * 菜品名精确匹配检索
     */
    @GetMapping("/menu-search")
    public ApiResponse<KnowledgeSearchResult> menuSearch(
            @RequestParam("name") String menuName) {
        KnowledgeSearchResult result = hybridSearchService.menuExactSearch(menuName);
        return ApiResponse.success(result);
    }

    /**
     * LazyGraphRAG 概念图检索。
     */
    @GetMapping("/lazy-graph-search")
    public ApiResponse<KnowledgeSearchResult> lazyGraphSearch(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {
        return ApiResponse.success(lazyGraphRagService.search(query, topK));
    }
}
