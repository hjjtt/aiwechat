package com.aiwechat.aichat.controller;

import com.aiwechat.aichat.model.dto.ChatRequest;
import com.aiwechat.aichat.model.dto.ChatResponse;
import com.aiwechat.aichat.service.AICustomerService;
import com.aiwechat.common.util.UserContextHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/ai-chat")
@RequiredArgsConstructor
public class AIChatController {

    private final AICustomerService aiCustomerService;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp");

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {
        Long userId = UserContextHelper.requireUserId(httpRequest);
        request.setUserId(String.valueOf(userId));

        log.info("收到提问请求 - userId: {}, question: {}", userId, request.getQuestion());

        ChatResponse response = aiCustomerService.handleUserQuery(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<Map<String, Object>> getHistory(
            @PathVariable String userId,
            HttpServletRequest httpRequest) {
        Long authenticatedUserId = UserContextHelper.requireUserId(httpRequest);

        if (!String.valueOf(authenticatedUserId).equals(cleanUserId(userId))) {
            log.warn("越权访问聊天历史 - 认证用户: {}, 请求用户: {}", authenticatedUserId, userId);
            return ResponseEntity.status(403).body(Map.of("error", "无权访问"));
        }

        String cleanUserId = String.valueOf(authenticatedUserId);
        log.info("获取对话历史 - userId: {}", cleanUserId);

        String history = aiCustomerService.getChatHistory(cleanUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", cleanUserId);
        result.put("history", history);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "AI Chat Service");
        return ResponseEntity.ok(health);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        UserContextHelper.requireUserId(httpRequest);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件为空"));
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("error", "图片大小不能超过5MB"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "仅支持 JPG/PNG/GIF/WEBP 格式"));
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest().body(Map.of("error", "不支持的文件扩展名"));
        }

        try {
            Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploaded", "chat-images");

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(fileName);

            Files.write(filePath, file.getBytes());
            log.info("图片上传成功: {}, 大小: {} bytes", filePath, file.getSize());

            return ResponseEntity.ok(Map.of("url", "/uploaded/chat-images/" + fileName));

        } catch (Exception e) {
            log.error("图片上传失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "上传失败"));
        }
    }

    private String cleanUserId(String userId) {
        if (userId != null && userId.startsWith("user_")) {
            return userId.substring(5);
        }
        return userId != null ? userId : "";
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
