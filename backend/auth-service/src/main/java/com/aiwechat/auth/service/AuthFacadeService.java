package com.aiwechat.auth.service;

import com.aiwechat.auth.model.dto.LoginResponse;
import com.aiwechat.auth.model.entity.User;
import com.aiwechat.common.dto.UserContext;

import java.util.Map;
import java.util.Optional;

public interface AuthFacadeService {

    LoginResponse login(String code, Map<String, Object> userInfo);

    LoginResponse mockLogin(String nickname, String avatarUrl);

    Optional<User> validateTokenAndGetUser(String token);

    Optional<UserContext> validateTokenAndBuildContext(String token);

    void deleteToken(String token);
}
