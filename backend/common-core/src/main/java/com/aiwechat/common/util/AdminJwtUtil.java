package com.aiwechat.common.util;

import com.aiwechat.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

/**
 * 管理后台 JWT 工具类
 * 使用 HMAC-SHA256 签名，不依赖外部 JWT 库
 */
public final class AdminJwtUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String HMAC_ALGO = "HmacSHA256";

    public static final String ATTR_CLAIMS = "admin.jwt.claims";

    private AdminJwtUtil() {}

    public static String generate(String subject, String secret, long expirySeconds) {
        try {
            long now = System.currentTimeMillis() / 1000;

            String headerB64 = b64Encode(MAPPER.writeValueAsBytes(
                    Map.of("alg", "HS256", "typ", "JWT")));
            String payloadB64 = b64Encode(MAPPER.writeValueAsBytes(
                    Map.of("sub", subject, "iat", now, "exp", now + expirySeconds)));

            String signature = b64Encode(hmacSha256(headerB64 + "." + payloadB64, secret));
            return headerB64 + "." + payloadB64 + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("生成管理后台令牌失败", e);
        }
    }

    public static JwtClaims validate(String token, String secret) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException("无效的管理后台令牌格式");
        }

        byte[] expectedSig = hmacSha256(parts[0] + "." + parts[1], secret);
        byte[] actualSig;
        try {
            actualSig = Base64.getUrlDecoder().decode(parts[2]);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的管理后台令牌编码");
        }

        if (!MessageDigest.isEqual(expectedSig, actualSig)) {
            throw new BusinessException("管理后台令牌签名无效");
        }

        Map<String, Object> payload;
        try {
            payload = MAPPER.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    new TypeReference<>() {});
        } catch (Exception e) {
            throw new BusinessException("管理后台令牌解析失败");
        }

        Object expObj = payload.get("exp");
        if (!(expObj instanceof Number exp)) {
            throw new BusinessException("管理后台令牌缺少过期时间");
        }

        if (System.currentTimeMillis() / 1000 > exp.longValue()) {
            throw new BusinessException("管理后台令牌已过期");
        }

        String sub = (String) payload.get("sub");
        if (sub == null || sub.isBlank()) {
            throw new BusinessException("管理后台令牌缺少用户信息");
        }

        Object iatObj = payload.get("iat");
        long iat = iatObj instanceof Number n ? n.longValue() : 0L;

        return new JwtClaims(sub, iat, exp.longValue());
    }

    public record JwtClaims(String subject, long issuedAt, long expiresAt) {}

    private static String b64Encode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 计算失败", e);
        }
    }
}
