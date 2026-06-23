package com.aiwechat.auth.service.impl;

import com.aiwechat.auth.config.WechatConfig;
import com.aiwechat.auth.model.dto.LoginResponse;
import com.aiwechat.auth.model.entity.User;
import com.aiwechat.auth.repository.UserRepository;
import com.aiwechat.auth.service.AuthFacadeService;
import com.aiwechat.auth.service.TokenService;
import com.aiwechat.common.dto.UserContext;
import com.aiwechat.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacadeServiceImpl implements AuthFacadeService {

    private final WechatConfig wechatConfig;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @Override
    @Transactional
    public LoginResponse login(String code, Map<String, Object> userInfo) {
        String openId = getOpenIdFromWechat(code);
        if (openId == null) {
            return null;
        }

        Optional<User> existingUser = userRepository.findByOpenId(openId);
        User user;
        boolean isNewUser = false;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (userInfo != null && userInfo.containsKey("nickName")) {
                user.setNickname((String) userInfo.get("nickName"));
                user.setAvatarUrl((String) userInfo.get("avatarUrl"));
                userRepository.update(user);
            }
            userRepository.updateLastLogin(openId);
        } else {
            isNewUser = true;
            user = new User();
            user.setOpenId(openId);
            user.setNickname(userInfo != null && userInfo.containsKey("nickName")
                    ? (String) userInfo.get("nickName") : "wechat-user");
            user.setAvatarUrl(userInfo != null ? (String) userInfo.get("avatarUrl") : null);
            user.setStatus("active");
            user.setCreatedAt(LocalDateTime.now());
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.insert(user);
        }

        String token = tokenService.generateToken(user.getId());
        return LoginResponse.of(token, String.valueOf(user.getId()), user.getNickname(), user.getAvatarUrl(), isNewUser);
    }

    @Override
    @Transactional
    public LoginResponse mockLogin(String nickname, String avatarUrl) {
        if (!"dev".equals(activeProfile)) {
            throw new BusinessException("Mock login is only available in development environment");
        }
        log.warn("Mock login 使用中（仅限开发环境）");
        String mockOpenId = "mock_" + System.currentTimeMillis();
        Optional<User> existingUser = userRepository.findByOpenId(mockOpenId);
        User user;
        boolean isNewUser = false;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            userRepository.updateLastLogin(mockOpenId);
        } else {
            isNewUser = true;
            user = new User();
            user.setOpenId(mockOpenId);
            user.setNickname(nickname);
            user.setAvatarUrl(avatarUrl);
            user.setStatus("active");
            user.setCreatedAt(LocalDateTime.now());
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.insert(user);
        }

        String token = tokenService.generateToken(user.getId());
        return LoginResponse.of(token, String.valueOf(user.getId()), user.getNickname(), user.getAvatarUrl(), isNewUser);
    }

    @Override
    public Optional<User> validateTokenAndGetUser(String token) {
        return tokenService.validateToken(token).flatMap(session -> userRepository.findById(session.getUserId()));
    }

    @Override
    public Optional<UserContext> validateTokenAndBuildContext(String token) {
        return tokenService.validateToken(token)
                .flatMap(session -> userRepository.findById(session.getUserId())
                        .map(user -> new UserContext(user.getId(), user.getOpenId(), user.getNickname(), token)));
    }

    @Override
    public void deleteToken(String token) {
        tokenService.deleteToken(token);
    }

    private String getOpenIdFromWechat(String code) {
        if (code != null && (code.startsWith("mock_") || code.contains("mock"))) {
            log.error("拒绝 mock code 登录尝试，请使用真实微信授权");
            return null;
        }

        String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                wechatConfig.getSessionHost(),
                wechatConfig.getAppId(),
                wechatConfig.getAppSecret(),
                code);

        try {
            String responseStr = restTemplate.getForObject(url, String.class);
            if (responseStr == null || responseStr.isEmpty()) {
                return null;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> response = objectMapper.readValue(responseStr, Map.class);
            if (response.containsKey("errcode")) {
                Object errcodeObj = response.get("errcode");
                int errcode = errcodeObj instanceof Integer ? (Integer) errcodeObj : Integer.parseInt(errcodeObj.toString());
                if (errcode != 0) {
                    log.error("Wechat login failed: {}", response.get("errmsg"));
                    return null;
                }
            }
            return response.containsKey("openid") ? (String) response.get("openid") : null;
        } catch (Exception e) {
            log.error("Wechat login error", e);
            return null;
        }
    }
}