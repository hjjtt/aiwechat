package com.aiwechat.auth.service;

import com.aiwechat.auth.model.entity.UserSession;

import java.util.Optional;

public interface TokenService {

    String generateToken(Long userId);

    Optional<UserSession> validateToken(String token);

    void deleteToken(String token);
}
