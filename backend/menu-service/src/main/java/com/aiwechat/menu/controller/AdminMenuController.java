package com.aiwechat.menu.controller;

import com.aiwechat.common.model.dto.ApiResponse;
import com.aiwechat.menu.model.entity.MenuItem;
import com.aiwechat.menu.service.AdminMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 菜品管理控制器（后台管理）
 */
@RestController
@RequestMapping("/api/admin/menus")
@RequiredArgsConstructor
public class AdminMenuController {

    private final AdminMenuService adminMenuService;

    /**
     * 分页获取菜品列表
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getItems(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(adminMenuService.getItems(category, keyword, page, size));
    }

    /**
     * 获取菜品统计数据
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.success(adminMenuService.getStats());
    }

    /**
     * 获取所有分类
     */
    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.success(adminMenuService.getAllCategories());
    }

    /**
     * 根据主键ID获取菜品详情
     */
    @GetMapping("/{id}")
    public ApiResponse<MenuItem> getItemById(@PathVariable Long id) {
        return ApiResponse.success(adminMenuService.getItemById(id));
    }

    /**
     * 新增菜品
     */
    @PostMapping
    public ApiResponse<Void> addItem(@RequestBody MenuItem item) {
        adminMenuService.addItem(item);
        return ApiResponse.success(null, "菜品添加成功");
    }

    /**
     * 更新菜品
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> updateItem(@PathVariable Long id, @RequestBody MenuItem item) {
        item.setId(id);
        adminMenuService.updateItem(item);
        return ApiResponse.success(null, "菜品更新成功");
    }

    /**
     * 删除菜品
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable Long id) {
        adminMenuService.deleteItem(id);
        return ApiResponse.success(null, "菜品删除成功");
    }

    /**
     * 更新菜品上架状态
     */
    @PostMapping("/{id}/availability")
    public ApiResponse<Void> updateAvailability(
            @PathVariable Long id,
            @RequestParam boolean available) {
        adminMenuService.updateAvailability(id, available);
        return ApiResponse.success(null, "状态更新成功");
    }

    /**
     * 批量更新菜品上架状态
     */
    @PostMapping("/batch-availability")
    public ApiResponse<Integer> batchUpdateAvailability(
            @RequestBody Map<String, Object> requestBody) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) requestBody.get("ids");
        Boolean available = (Boolean) requestBody.get("available");
        int count = adminMenuService.batchUpdateAvailability(ids, available);
        return ApiResponse.success(count, "批量更新成功");
    }
}
