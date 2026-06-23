package com.aiwechat.admin.controller;

import com.aiwechat.admin.service.AdminChatRecordService;
import com.aiwechat.common.model.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理后台聊天记录管理控制器
 * 处理聊天记录的查询、删除、清理等接口
 */
@RestController
@RequestMapping("/api/admin/chat-records")
@RequiredArgsConstructor
public class AdminChatRecordController {

    private final AdminChatRecordService adminChatRecordService;

    /**
     * 获取聊天记录列表（分页+筛选）
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getChatRecords(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(adminChatRecordService.getChatRecords(userId, keyword, page, size));
    }

    /**
     * 获取聊天记录统计数据
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.success(adminChatRecordService.getStats());
    }

    /**
     * 根据ID删除聊天记录
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteById(@PathVariable Long id) {
        boolean deleted = adminChatRecordService.deleteById(id);
        return deleted
                ? ApiResponse.success(null, "删除成功")
                : ApiResponse.error("删除失败，记录可能不存在");
    }

    /**
     * 批量删除聊天记录
     */
    @PostMapping("/batch-delete")
    public ApiResponse<Map<String, Object>> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        int count = adminChatRecordService.batchDelete(ids);
        Map<String, Object> result = Map.of("deletedCount", count);
        return ApiResponse.success(result, "批量删除完成");
    }

    /**
     * 清空所有聊天记录
     */
    @DeleteMapping("/clear")
    public ApiResponse<Map<String, Object>> clearAll() {
        int count = adminChatRecordService.clearAll();
        Map<String, Object> result = Map.of("deletedCount", count);
        return ApiResponse.success(result, "清空完成");
    }

    /**
     * 按天数清理聊天记录
     */
    @PostMapping("/clean-by-days")
    public ApiResponse<Map<String, Object>> cleanByDays(@RequestBody Map<String, Integer> body) {
        int days = body.getOrDefault("days", 30);
        int count = adminChatRecordService.cleanByDays(days);
        Map<String, Object> result = Map.of("deletedCount", count);
        return ApiResponse.success(result, "清理完成");
    }

    /**
     * 获取所有存在聊天记录的用户ID
     */
    @GetMapping("/user-ids")
    public ApiResponse<List<String>> getAllUserIds() {
        return ApiResponse.success(adminChatRecordService.getAllUserIds());
    }
}
