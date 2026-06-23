package com.aiwechat.admin.controller;

import com.aiwechat.admin.model.entity.User;
import com.aiwechat.admin.service.AdminUserService;
import com.aiwechat.common.model.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台用户管理控制器
 * 处理用户的增删改查、状态管理等接口
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 获取用户列表（分页+筛选）
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(adminUserService.getUsers(keyword, status, page, size));
    }

    /**
     * 获取用户统计数据
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.success(adminUserService.getStats());
    }

    /**
     * 根据ID获取用户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<User> getUserById(@PathVariable Long id) {
        return ApiResponse.success(adminUserService.getUserById(id));
    }

    /**
     * 更新用户状态
     */
    @PostMapping("/{id}/status")
    public ApiResponse<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        adminUserService.updateUserStatus(id, status);
        return ApiResponse.success(null, "状态更新成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ApiResponse.success(null, "用户删除成功");
    }
}
