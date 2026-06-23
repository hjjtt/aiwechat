package com.aiwechat.gateway.filter;

import com.aiwechat.common.constants.GatewayHeaders;
import com.aiwechat.common.dto.CommonResponse;
import com.aiwechat.common.dto.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenRelayFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${security.auth-service-url:http://localhost:9091}")
    private String authServiceUrl;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())
                || isAnonymousPath(path)
                || isAdminPath(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            return unauthorized(exchange, "missing_authorization");
        }

        return WebClient.builder().baseUrl(authServiceUrl).build()
                .get()
                .uri("/api/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(UserContextResponse.class)
                .flatMap(response -> {
                    if (response == null || !response.isSuccess() || response.getData() == null) {
                        return unauthorized(exchange, "token_invalid");
                    }

                    UserContext context = response.getData();
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header(GatewayHeaders.USER_ID, String.valueOf(context.getUserId()))
                            .header(GatewayHeaders.OPEN_ID, safe(context.getOpenId()))
                            .header(GatewayHeaders.USER_NICKNAME, safe(context.getNickname()))
                            .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(ex -> {
                    log.warn("Auth validate failed: {}", ex.getMessage());
                    return unauthorized(exchange, "auth_service_unavailable");
                });
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isAnonymousPath(String path) {
        return pathMatcher.match("/api/auth/**", path)
                || pathMatcher.match("/api/wechat/login", path)
                || pathMatcher.match("/api/wechat/mock-login", path)
                || pathMatcher.match("/api/menu/**", path)
                || pathMatcher.match("/api/knowledge/search", path)
                || pathMatcher.match("/api/ai-chat/health", path)
                || pathMatcher.match("/actuator/**", path);
    }

    private boolean isAdminPath(String path) {
        return pathMatcher.match("/api/admin/**", path);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(CommonResponse.error(message));
        } catch (Exception ex) {
            bytes = ("{\"success\":false,\"error\":\"" + message + "\"}").getBytes();
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Data
    private static class UserContextResponse {
        private boolean success;
        private UserContext data;
        private String error;
    }
}
