package com.aiwechat.auth.service.impl;

import com.aiwechat.auth.model.entity.UserSession;
import com.aiwechat.auth.repository.UserSessionRepository;
import com.aiwechat.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final UserSessionRepository userSessionRepository;

    @Value("${app.token.expire-hours:72}")
    private int expireHours;

    @Override
    public String generateToken(Long userId) {
        userSessionRepository.deleteByUserId(userId);

        String token = UUID.randomUUID().toString().replace("-", "");
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setToken(token);
        session.setExpiresAt(LocalDateTime.now().plusHours(expireHours));
        session.setCreatedAt(LocalDateTime.now());
        session.setLastActiveAt(LocalDateTime.now());
        userSessionRepository.insert(session);
        return token;
    }

    @Override
    public Optional<UserSession> validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }

        Optional<UserSession> session = userSessionRepository.findByToken(token);
        session.ifPresent(value -> {
            if (value.getExpiresAt() != null && value.getExpiresAt().isAfter(LocalDateTime.now())) {
                userSessionRepository.updateLastActive(token);
            }
        });
        return session.filter(value -> value.getExpiresAt() == null || value.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Override
    public void deleteToken(String token) {
        validateToken(token).ifPresent(session -> userSessionRepository.deleteByUserId(session.getUserId()));
    }
}
