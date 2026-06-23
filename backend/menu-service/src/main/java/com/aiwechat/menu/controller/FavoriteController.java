package com.aiwechat.menu.controller;

import com.aiwechat.common.model.dto.ApiResponse;
import com.aiwechat.common.util.UserContextHelper;
import com.aiwechat.menu.model.dto.FavoriteItemDTO;
import com.aiwechat.menu.service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/user/{userId}")
    public ApiResponse<List<FavoriteItemDTO>> getFavorites(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        Long authenticatedUserId = UserContextHelper.requireUserId(httpRequest);
        if (!authenticatedUserId.equals(userId)) {
            return ApiResponse.error("无权查看他人收藏");
        }
        return ApiResponse.success(favoriteService.getFavorites(userId));
    }

    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> checkFavorite(
            @RequestParam Long userId,
            @RequestParam Long menuId,
            HttpServletRequest httpRequest) {
        Long authenticatedUserId = UserContextHelper.requireUserId(httpRequest);
        if (!authenticatedUserId.equals(userId)) {
            return ApiResponse.error("无权查看");
        }
        boolean isFavorite = favoriteService.checkFavorite(userId, menuId);
        Map<String, Object> result = new HashMap<>();
        result.put("isFavorite", isFavorite);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<Void> addFavorite(
            @RequestBody Map<String, Long> params,
            HttpServletRequest httpRequest) {
        Long userId = UserContextHelper.requireUserId(httpRequest);
        boolean success = favoriteService.addFavorite(userId, params.get("menuId"));
        return success
                ? ApiResponse.success(null, "添加成功")
                : ApiResponse.error("添加失败，可能已收藏");
    }

    @DeleteMapping
    public ApiResponse<Void> removeFavorite(
            @RequestBody Map<String, Long> params,
            HttpServletRequest httpRequest) {
        Long userId = UserContextHelper.requireUserId(httpRequest);
        boolean success = favoriteService.removeFavorite(userId, params.get("menuId"));
        return success
                ? ApiResponse.success(null, "取消成功")
                : ApiResponse.error("取消失败，收藏不存在");
    }

    @PostMapping("/toggle")
    public ApiResponse<Map<String, Object>> toggleFavorite(
            @RequestBody Map<String, Long> params,
            HttpServletRequest httpRequest) {
        Long userId = UserContextHelper.requireUserId(httpRequest);
        return ApiResponse.success(favoriteService.toggleFavorite(userId, params.get("menuId")));
    }

    @GetMapping("/count/{userId}")
    public ApiResponse<Map<String, Object>> getCount(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        Long authenticatedUserId = UserContextHelper.requireUserId(httpRequest);
        if (!authenticatedUserId.equals(userId)) {
            return ApiResponse.error("无权查看");
        }
        int count = favoriteService.getCount(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return ApiResponse.success(result);
    }
}
