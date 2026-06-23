package com.aiwechat.auth.controller;

import com.aiwechat.auth.model.dto.LoginResponse;
import com.aiwechat.auth.service.AuthFacadeService;
import com.aiwechat.common.dto.CommonResponse;
import com.aiwechat.common.dto.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthFacadeService authFacadeService;

    @PostMapping("/login")
    public CommonResponse<LoginResponse> login(@RequestBody Map<String, Object> requestBody) {
        String code = (String) requestBody.get("code");
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) requestBody.get("userInfo");

        if (code == null || code.isEmpty()) {
            return CommonResponse.error("login_code_required");
        }

        LoginResponse response = authFacadeService.login(code, userInfo);
        return response == null ? CommonResponse.error("login_failed") : CommonResponse.success(response);
    }

    @GetMapping("/validate")
    public CommonResponse<UserContext> validate(@RequestHeader("Authorization") String authorization) {
        String token = extractBearerToken(authorization);
        return authFacadeService.validateTokenAndBuildContext(token)
                .map(CommonResponse::success)
                .orElseGet(() -> CommonResponse.error("token_invalid"));
    }

    @PostMapping("/logout")
    public CommonResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        authFacadeService.deleteToken(extractBearerToken(authorization));
        return CommonResponse.success(null, "logout_success");
    }

    @GetMapping("/health")
    public CommonResponse<String> health() {
        return CommonResponse.success("UP");
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null) {
            return "";
        }
        return authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
    }
}