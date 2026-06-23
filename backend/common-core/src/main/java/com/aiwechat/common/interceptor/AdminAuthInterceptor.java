package com.aiwechat.common.interceptor;

import com.aiwechat.common.model.dto.ApiResponse;
import com.aiwechat.common.util.AdminJwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Value("${admin.auth.secret:}")
    private String adminSecret;

    private final ObjectMapper objectMapper;

    public AdminAuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (adminSecret == null || adminSecret.isEmpty()) {
            log.warn("Admin auth secret 未配置，拒绝所有管理后台请求");
            sendUnauthorized(response, "管理后台未配置鉴权密钥，请联系管理员");
            return false;
        }

        String token = request.getHeader("X-Admin-Secret");
        if (token == null || token.isEmpty()) {
            log.warn("管理后台请求缺少鉴权头 - uri: {}", request.getRequestURI());
            sendUnauthorized(response, "缺少管理后台鉴权凭证");
            return false;
        }

        try {
            AdminJwtUtil.JwtClaims claims = AdminJwtUtil.validate(token, adminSecret);
            request.setAttribute(AdminJwtUtil.ATTR_CLAIMS, claims);
            return true;
        } catch (Exception e) {
            log.warn("管理后台鉴权失败 - uri: {} - {}", request.getRequestURI(), e.getMessage());
            sendUnauthorized(response, e.getMessage());
            return false;
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
