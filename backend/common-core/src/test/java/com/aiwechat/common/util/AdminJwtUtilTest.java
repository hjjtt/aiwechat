package com.aiwechat.common.util;

import com.aiwechat.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AdminJwtUtilTest {

    private static final String SECRET = "test-secret-key-for-jwt-unit-tests";

    @Test
    @DisplayName("generate 应该返回三段式 JWT 格式")
    void generateShouldReturnThreePartToken() {
        String token = AdminJwtUtil.generate("admin", SECRET, 3600);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    @DisplayName("validate 合法令牌应该返回正确的 claims")
    void validateValidToken() {
        String token = AdminJwtUtil.generate("admin", SECRET, 3600);
        AdminJwtUtil.JwtClaims claims = AdminJwtUtil.validate(token, SECRET);

        assertEquals("admin", claims.subject());
        assertTrue(claims.expiresAt() > System.currentTimeMillis() / 1000);
        assertTrue(claims.issuedAt() <= System.currentTimeMillis() / 1000);
    }

    @Test
    @DisplayName("validate 过期令牌应该抛出异常")
    void validateExpiredToken() {
        // 生成一个已过期的令牌（过期时间 -1 秒）
        String token = AdminJwtUtil.generate("admin", SECRET, -1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> AdminJwtUtil.validate(token, SECRET));
        assertTrue(ex.getMessage().contains("过期"));
    }

    @Test
    @DisplayName("validate 错误密钥应该抛出签名无效异常")
    void validateWithWrongSecret() {
        String token = AdminJwtUtil.generate("admin", SECRET, 3600);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> AdminJwtUtil.validate(token, "wrong-secret"));
        assertTrue(ex.getMessage().contains("签名"));
    }

    @Test
    @DisplayName("validate 篡改 payload 应该抛出签名无效异常")
    void validateTamperedPayload() {
        String token = AdminJwtUtil.generate("admin", SECRET, 3600);
        String[] parts = token.split("\\.");

        // 篡改 payload 中的 subject
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String tampered = payloadJson.replace("admin", "hacker");
        parts[1] = Base64.getUrlEncoder().withoutPadding().encodeToString(tampered.getBytes(StandardCharsets.UTF_8));

        String tamperedToken = String.join(".", parts);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> AdminJwtUtil.validate(tamperedToken, SECRET));
        assertTrue(ex.getMessage().contains("签名"));
    }

    @Test
    @DisplayName("validate 格式错误应该抛出异常")
    void validateMalformedToken() {
        assertThrows(BusinessException.class,
                () -> AdminJwtUtil.validate("not.a.valid.token.format", SECRET));
    }

    @Test
    @DisplayName("validate 空字符串应该抛出异常")
    void validateEmptyToken() {
        assertThrows(BusinessException.class,
                () -> AdminJwtUtil.validate("", SECRET));
    }

    @Test
    @DisplayName("validate 只有两段的令牌应该抛出异常")
    void validateTwoPartsToken() {
        assertThrows(BusinessException.class,
                () -> AdminJwtUtil.validate("part1.part2", SECRET));
    }

    @Test
    @DisplayName("不同 subject 应该生成不同的令牌")
    void differentSubjectsGenerateDifferentTokens() {
        String token1 = AdminJwtUtil.generate("admin1", SECRET, 3600);
        String token2 = AdminJwtUtil.generate("admin2", SECRET, 3600);

        assertNotEquals(token1, token2);

        AdminJwtUtil.JwtClaims claims1 = AdminJwtUtil.validate(token1, SECRET);
        assertEquals("admin1", claims1.subject());

        AdminJwtUtil.JwtClaims claims2 = AdminJwtUtil.validate(token2, SECRET);
        assertEquals("admin2", claims2.subject());
    }

    @Test
    @DisplayName("同一参数多次生成令牌应该包含不同的 iat")
    void multipleGenerations() throws InterruptedException {
        String token1 = AdminJwtUtil.generate("admin", SECRET, 3600);
        Thread.sleep(1100); // 确保 iat 差异
        String token2 = AdminJwtUtil.generate("admin", SECRET, 3600);

        assertNotEquals(token1, token2);
    }
}
