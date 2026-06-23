package com.aiwechat.menu.controller;

import com.aiwechat.common.model.dto.ApiResponse;
import com.aiwechat.menu.model.entity.MenuItem;
import com.aiwechat.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 菜品控制器（前台）
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * 获取所有上架菜品
     */
    @GetMapping("/items")
    public ApiResponse<List<MenuItem>> getAllItems() {
        return ApiResponse.success(menuService.getAllAvailableItems());
    }

    /**
     * 根据分类获取菜品（路径参数）
     */
    @GetMapping("/category/{category}")
    public ApiResponse<List<MenuItem>> getItemsByCategory(@PathVariable String category) {
        return ApiResponse.success(menuService.getItemsByCategory(category));
    }

    /**
     * 根据分类获取菜品（查询参数）
     */
    @GetMapping("/category")
    public ApiResponse<List<MenuItem>> getItemsByCategoryParam(@RequestParam String category) {
        return ApiResponse.success(menuService.getItemsByCategory(category));
    }

    /**
     * 搜索菜品
     */
    @GetMapping("/search")
    public ApiResponse<List<MenuItem>> searchItems(@RequestParam String keyword) {
        return ApiResponse.success(menuService.searchItems(keyword));
    }

    /**
     * 根据菜品编号获取菜品详情
     */
    @GetMapping("/items/{itemId}")
    public ApiResponse<MenuItem> getItem(@PathVariable String itemId) {
        return ApiResponse.success(menuService.getItemById(itemId));
    }
}
