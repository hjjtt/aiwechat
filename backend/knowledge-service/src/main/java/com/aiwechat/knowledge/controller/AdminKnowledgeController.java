package com.aiwechat.knowledge.controller;

import com.aiwechat.common.model.dto.ApiResponse;
import com.aiwechat.knowledge.model.dto.EvalReport;
import com.aiwechat.knowledge.model.dto.LazyGraphIndexReport;
import com.aiwechat.knowledge.service.KnowledgeBaseService;
import com.aiwechat.knowledge.service.KnowledgeEvalService;
import com.aiwechat.knowledge.service.KnowledgeSyncService;
import com.aiwechat.knowledge.service.LazyGraphRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
public class AdminKnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeSyncService knowledgeSyncService;
    private final KnowledgeEvalService knowledgeEvalService;
    private final LazyGraphRagService lazyGraphRagService;

    @PostMapping("/upload")
    public ApiResponse<List<String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "metadata", required = false) String metadata) {
        log.info("接收到文档上传请求: {}", file.getOriginalFilename());
        List<String> ids = knowledgeBaseService.uploadDocument(file, metadata);
        return ApiResponse.success(ids);
    }

    @PostMapping("/upload/batch")
    public ApiResponse<List<KnowledgeBaseService.UploadResult>> uploadBatch(
            @RequestParam("files") MultipartFile[] files) {
        log.info("接收到批量文档上传请求: {} 个文件", files.length);
        List<KnowledgeBaseService.UploadResult> results = knowledgeBaseService.uploadDocuments(files);
        return ApiResponse.success(results);
    }

    @PostMapping("/add-text")
    public ApiResponse<String> addText(
            @RequestParam("content") String content,
            @RequestParam("title") String title) {
        log.info("接收到文本添加请求: {}", title);
        String docId = knowledgeBaseService.addText(content, title);
        return ApiResponse.success(docId);
    }

    @PostMapping("/add-texts")
    public ApiResponse<List<String>> addTexts(
            @RequestBody List<KnowledgeBaseService.TextItem> texts) {
        log.info("接收到批量文本添加请求: {} 条", texts.size());
        List<String> ids = knowledgeBaseService.addTexts(texts);
        return ApiResponse.success(ids);
    }

    @DeleteMapping("/{documentId}")
    public ApiResponse<Boolean> deleteDocument(@PathVariable String documentId) {
        log.info("接收到删除文档请求: {}", documentId);
        boolean result = knowledgeBaseService.deleteDocument(documentId);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/clear")
    public ApiResponse<Void> clear() {
        log.info("接收到清空知识库请求");
        knowledgeBaseService.clear();
        return ApiResponse.success(null, "知识库已清空");
    }

    @PostMapping("/init-menu")
    public ApiResponse<Void> initMenuKnowledge() {
        log.info("接收到初始化菜单知识库请求");
        knowledgeBaseService.initMenuKnowledge();
        return ApiResponse.success(null, "菜单知识库初始化完成");
    }

    @PostMapping("/init-default")
    public ApiResponse<KnowledgeBaseService.InitSummary> initDefaultKnowledge() {
        log.info("接收到初始化默认知识库请求");
        KnowledgeBaseService.InitSummary summary = knowledgeBaseService.initDefaultKnowledge();
        return ApiResponse.success(summary, "默认知识库初始化完成");
    }

    @PostMapping("/sync")
    public ApiResponse<Void> syncKnowledge() {
        log.info("接收到手动同步请求");
        knowledgeSyncService.syncAll();
        return ApiResponse.success(null, "知识库同步完成");
    }

    @GetMapping("/list")
    public ApiResponse<List<KnowledgeBaseService.DocumentInfo>> listDocuments() {
        List<KnowledgeBaseService.DocumentInfo> docs = knowledgeBaseService.listDocuments();
        return ApiResponse.success(docs);
    }

    @GetMapping("/sync/status")
    public ApiResponse<KnowledgeSyncService.SyncStatus> getSyncStatus() {
        KnowledgeSyncService.SyncStatus status = knowledgeSyncService.getSyncStatus();
        return ApiResponse.success(status);
    }

    @PostMapping("/lazy-graph/rebuild")
    public ApiResponse<LazyGraphIndexReport> rebuildLazyGraph() {
        log.info("接收到 LazyGraphRAG 概念图重建请求");
        LazyGraphIndexReport report = lazyGraphRagService.rebuildIndex();
        return ApiResponse.success(report, "LazyGraphRAG 概念图重建完成");
    }

    @GetMapping("/lazy-graph/concepts/extract")
    public ApiResponse<List<String>> extractConcepts(@RequestParam("text") String text) {
        return ApiResponse.success(lazyGraphRagService.extractConcepts(text));
    }

    // ==================== 评测接口 ====================

    /**
     * 运行评测
     */
    @PostMapping("/eval/run")
    public ApiResponse<EvalReport> runEval(
            @RequestParam(value = "category", required = false) String category) {
        log.info("接收到评测请求 - category: {}", category);
        EvalReport report = knowledgeEvalService.runEval(category);
        return ApiResponse.success(report);
    }

    /**
     * 初始化默认评测数据
     */
    @PostMapping("/eval/init")
    public ApiResponse<Integer> initEvalCases() {
        int count = knowledgeEvalService.initDefaultEvalCases();
        return ApiResponse.success(count, "初始化 " + count + " 条评测用例");
    }

    /**
     * 获取最近评测结果
     */
    @GetMapping("/eval/results")
    public ApiResponse<List<Map<String, Object>>> getEvalResults(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        List<Map<String, Object>> results = knowledgeEvalService.getLatestResults(limit);
        return ApiResponse.success(results);
    }
}
