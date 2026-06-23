package com.aiwechat.auth.controller;

import com.aiwechat.auth.model.dto.LoginResponse;
import com.aiwechat.auth.service.AuthFacadeService;
import com.aiwechat.common.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Profile("dev")
public class DevMockAuthController {

    private final AuthFacadeService authFacadeService;

    @PostMapping("/mock-login")
    public CommonResponse<LoginResponse> mockLogin(@RequestBody Map<String, Object> requestBody) {
        String nickname = (String) requestBody.getOrDefault("nickname", "wechat-user");
        String avatarUrl = (String) requestBody.get("avatarUrl");
        return CommonResponse.success(authFacadeService.mockLogin(nickname, avatarUrl));
    }
}
