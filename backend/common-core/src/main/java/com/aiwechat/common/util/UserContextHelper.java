package com.aiwechat.common.util;

import com.aiwechat.common.constants.GatewayHeaders;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 从网关透传的请求头中提取已认证用户信息
 */
public final class UserContextHelper {

    private UserContextHelper() {}

    public static Long getUserId(HttpServletRequest request) {
        String header = request.getHeader(GatewayHeaders.USER_ID);
        if (header == null || header.isBlank()) {
            return null;
        }
        return Long.parseLong(header.trim());
    }

    public static Long requireUserId(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            throw new com.aiwechat.common.exception.BusinessException("用户身份缺失，请重新登录");
        }
        return userId;
    }

    public static String getOpenId(HttpServletRequest request) {
        return request.getHeader(GatewayHeaders.OPEN_ID);
    }

    public static String getNickname(HttpServletRequest request) {
        return request.getHeader(GatewayHeaders.USER_NICKNAME);
    }
}
