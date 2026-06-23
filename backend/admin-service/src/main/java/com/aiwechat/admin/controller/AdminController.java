package com.aiwechat.admin.controller;

import com.aiwechat.admin.service.FeedbackService;
import com.aiwechat.admin.service.HumanTransferService;
import com.aiwechat.admin.service.PerformanceMonitorService;
import com.aiwechat.common.model.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台通用控制器
 * 处理系统监控、健康检查、转接队列、反馈等通用管理接口
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PerformanceMonitorService performanceMonitorService;
    private final FeedbackService feedbackService;
    private final HumanTransferService humanTransferService;

    /**
     * 获取系统性能指标
     */
    @GetMapping("/metrics")
    public ApiResponse<PerformanceMonitorService.PerformanceMetrics> getMetrics() {
        return ApiResponse.success(performanceMonitorService.getMetrics());
    }

    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    public ApiResponse<PerformanceMonitorService.HealthStatus> getHealth() {
        return ApiResponse.success(performanceMonitorService.getHealthStatus());
    }

    /**
     * 获取人工转接队列
     */
    @GetMapping("/transfer/queue")
    public ApiResponse<HumanTransferService.TransferQueue> getTransferQueue() {
        return ApiResponse.success(humanTransferService.getTransferQueue());
    }

    /**
     * 接受转接请求
     */
    @PostMapping("/transfer/{transferId}/accept")
    public ApiResponse<Map<String, Object>> acceptTransfer(
            @PathVariable String transferId,
            @RequestBody Map<String, String> body) {
        String agentId = body.get("agentId");
        boolean accepted = humanTransferService.acceptTransfer(transferId, agentId);

        Map<String, Object> result = new HashMap<>();
        result.put("accepted", accepted);
        result.put("transferId", transferId);
        return ApiResponse.success(result, accepted ? "已接受转接" : "接受转接失败");
    }

    /**
     * 获取所有反馈列表
     */
    @GetMapping("/feedback")
    public ApiResponse<java.util.List<FeedbackService.FeedbackItem>> getAllFeedback() {
        return ApiResponse.success(feedbackService.getAllFeedback());
    }

    /**
     * 获取反馈统计信息
     */
    @GetMapping("/feedback/stats")
    public ApiResponse<Map<String, Object>> getFeedbackStats() {
        return ApiResponse.success(feedbackService.getStats());
    }

    /**
     * 删除单条反馈
     */
    @DeleteMapping("/feedback/{id}")
    public ApiResponse<Void> deleteFeedback(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
        return ApiResponse.success(null, "删除成功");
    }

    /**
     * 批量删除反馈
     */
    @PostMapping("/feedback/batch-delete")
    public ApiResponse<Void> batchDeleteFeedback(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.error("请选择要删除的反馈");
        }
        feedbackService.batchDeleteFeedback(ids);
        return ApiResponse.success(null, "批量删除成功");
    }

    /**
     * 清空所有反馈
     */
    @DeleteMapping("/feedback/clear")
    public ApiResponse<Void> clearAllFeedback() {
        feedbackService.clearAll();
        return ApiResponse.success(null, "清空成功");
    }
}
