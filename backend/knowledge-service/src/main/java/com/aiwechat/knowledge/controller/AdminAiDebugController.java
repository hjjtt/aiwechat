package com.aiwechat.knowledge.controller;

import com.aiwechat.common.model.dto.ApiResponse;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.QueryRouteResult;
import com.aiwechat.knowledge.service.KnowledgeRouterService;
import com.aiwechat.knowledge.service.LazyGraphRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiDebugController {

    private final KnowledgeRouterService knowledgeRouterService;
    private final LazyGraphRagService lazyGraphRagService;

    @GetMapping("/query/route")
    public ApiResponse<QueryRouteResult> route(@RequestParam("query") String query) {
        log.info("AI Query 路由调试: query={}", query);
        return ApiResponse.success(knowledgeRouterService.route(query));
    }

    @GetMapping("/retrieve/debug")
    public ApiResponse<KnowledgeSearchResult> retrieveDebug(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {
        log.info("AI 检索调试: query={}, topK={}", query, topK);
        return ApiResponse.success(knowledgeRouterService.retrieve(query, topK));
    }

    @GetMapping("/graph/path/debug")
    public ApiResponse<KnowledgeSearchResult> lazyGraphDebug(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {
        log.info("LazyGraphRAG 路径调试: query={}, topK={}", query, topK);
        return ApiResponse.success(lazyGraphRagService.search(query, topK));
    }
}
