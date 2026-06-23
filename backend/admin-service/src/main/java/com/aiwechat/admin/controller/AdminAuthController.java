package com.aiwechat.admin.controller;

import com.aiwechat.common.model.dto.ApiResponse;
import com.aiwechat.common.util.AdminJwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    @Value("${admin.auth.secret:}")
    private String adminSecret;

    @Value("${admin.auth.username:admin}")
    private String adminUsername;

    @Value("${admin.auth.password:your_admin_password_here}")
    private String adminPassword;

    @Value("${admin.auth.jwt-expiry-hours:24}")
    private int jwtExpiryHours;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        if (username == null || password == null) {
            return ApiResponse.error("用户名和密码不能为空");
        }

        if (!adminUsername.equals(username) || !adminPassword.equals(password)) {
            log.warn("管理后台登录失败 - 用户名: {}", username);
            return ApiResponse.error("用户名或密码错误");
        }

        String token = AdminJwtUtil.generate(username, adminSecret, jwtExpiryHours * 3600L);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", username);

        log.info("管理后台登录成功 - 用户名: {}", username);
        return ApiResponse.success(data, "登录成功");
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(HttpServletRequest request) {
        AdminJwtUtil.JwtClaims claims = (AdminJwtUtil.JwtClaims) request.getAttribute(AdminJwtUtil.ATTR_CLAIMS);

        Map<String, Object> profile = new HashMap<>();
        profile.put("username", claims != null ? claims.subject() : adminUsername);
        profile.put("role", "admin");
        return ApiResponse.success(profile);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success(null, "登出成功");
    }
}
