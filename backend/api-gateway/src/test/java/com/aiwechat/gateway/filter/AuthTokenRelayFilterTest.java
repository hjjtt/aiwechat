package com.aiwechat.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AuthTokenRelayFilterTest {

    private AuthTokenRelayFilter filter;
    private Method isAnonymousPathMethod;

    @BeforeEach
    void setUp() throws Exception {
        filter = new AuthTokenRelayFilter();
        isAnonymousPathMethod = AuthTokenRelayFilter.class.getDeclaredMethod("isAnonymousPath", String.class);
        isAnonymousPathMethod.setAccessible(true);
    }

    private boolean invokeIsAnonymousPath(String path) {
        try {
            return (boolean) isAnonymousPathMethod.invoke(filter, path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest(name = "匿名路径: {0}")
    @ValueSource(strings = {
            "/api/auth/login",
            "/api/auth/mock-login",
            "/api/auth/validate",
            "/api/auth/logout",
            "/api/auth/health",
            "/api/wechat/login",
            "/api/wechat/mock-login",
            "/api/menu/items",
            "/api/menu/category/热菜",
            "/api/menu/search?keyword=宫保鸡丁",
            "/api/knowledge/search",
            "/api/ai-chat/health",
            "/actuator/health"
    })
    @DisplayName("这些路径应该免鉴权")
    void shouldAllowAnonymous(String path) {
        assertTrue(invokeIsAnonymousPath(path), path + " 应该是匿名路径");
    }

    static Stream<Arguments> protectedPaths() {
        return Stream.of(
                // 用户鉴权接口
                Arguments.of("/api/ai-chat/ask", "AI 聊天提问"),
                Arguments.of("/api/ai-chat/history/14", "AI 聊天历史"),
                Arguments.of("/api/ai-chat/upload", "AI 聊天图片上传"),
                Arguments.of("/api/orders", "订单列表"),
                Arguments.of("/api/orders/user/14", "用户订单"),
                Arguments.of("/api/orders/ORD123/status", "订单状态修改"),
                Arguments.of("/api/orders/ORD123/cancel", "取消订单"),
                Arguments.of("/api/addresses/user/14", "用户地址"),
                Arguments.of("/api/addresses/1", "地址详情"),
                Arguments.of("/api/favorites/user/14", "用户收藏"),
                Arguments.of("/api/favorites/check", "收藏检查"),
                Arguments.of("/api/upload/avatar", "头像上传"),
                Arguments.of("/api/upload/file", "文件上传"),
                // 管理端接口（走 isAdminPath，不匿名）
                Arguments.of("/api/admin/orders", "管理端订单"),
                Arguments.of("/api/admin/menus", "管理端菜单"),
                Arguments.of("/api/admin/knowledge/upload", "管理端知识库上传"),
                Arguments.of("/api/admin/knowledge/list", "管理端知识库列表"),
                Arguments.of("/api/admin/knowledge/sync", "管理端知识库同步"),
                Arguments.of("/api/admin/knowledge/clear", "管理端知识库清空"),
                Arguments.of("/api/admin/knowledge/init-menu", "管理端知识库初始化"),
                Arguments.of("/api/admin/login", "管理端登录"),
                // 知识库写接口（原路径已废弃，但仍应被拦）
                Arguments.of("/api/knowledge/upload", "知识库上传(旧路径)"),
                Arguments.of("/api/knowledge/list", "知识库列表(旧路径)"),
                Arguments.of("/api/knowledge/clear", "知识库清空(旧路径)")
        );
    }

    @ParameterizedTest(name = "需要鉴权: {1}")
    @MethodSource("protectedPaths")
    @DisplayName("这些路径必须鉴权")
    void shouldRequireAuth(String path, String description) {
        assertFalse(invokeIsAnonymousPath(path), description + " 不应该是匿名路径");
    }
}
