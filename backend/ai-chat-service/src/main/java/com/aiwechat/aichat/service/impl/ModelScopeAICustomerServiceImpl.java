package com.aiwechat.aichat.service.impl;

import com.aiwechat.aichat.model.dto.ChatRequest;
import com.aiwechat.aichat.model.dto.ChatResponse;
import com.aiwechat.aichat.model.dto.IntentResult;
import com.aiwechat.aichat.model.dto.KnowledgeSearchResult;
import com.aiwechat.aichat.model.dto.MemoryContext;
import com.aiwechat.aichat.model.entity.ChatRecord;
import com.aiwechat.aichat.repository.ChatRecordRepository;
import com.aiwechat.aichat.service.AICustomerService;
import com.aiwechat.aichat.service.HumanTransferService;
import com.aiwechat.aichat.service.IntentRecognitionService;
import com.aiwechat.aichat.service.KnowledgeBaseService;
import com.aiwechat.aichat.service.SessionMemoryService;
import com.aiwechat.aichat.service.UserPreferenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 客服核心实现类
 * 集成：意图识别 + 三层记忆管理 + RAG 知识库 + 多模型轮询 + 多模态图片识别
 *
 * 核心流程：意图识别 → RAG检索 → 路由Prompt + 三层记忆注入 → 调用LLM → 更新记忆 → 保存记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelScopeAICustomerServiceImpl implements AICustomerService {

    private final ChatRecordRepository chatRecordRepository;
    private final HumanTransferService humanTransferService;
    private final UserPreferenceService userPreferenceService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final IntentRecognitionService intentRecognitionService;
    private final SessionMemoryService sessionMemoryService;
    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Value("${spring.ai.openai.models:qwen/Qwen3-1.7B}")
    private List<String> models;

    @Value("${spring.servlet.multipart.location:./uploaded}")
    private String uploadDir;

    @Value("${spring.ai.openai.model-index:0}")
    private int modelIndex;

    @Value("${chat.history.limit:10}")
    private int chatHistoryLimit;

    private int currentModelIndex = 0;

    @PostConstruct
    void logAiConfig() {
        String keyPrefix;
        int keyLength = apiKey == null ? 0 : apiKey.length();
        if (apiKey == null || apiKey.isBlank()) {
            keyPrefix = "(empty)";
        } else {
            int end = Math.min(8, apiKey.length());
            keyPrefix = apiKey.substring(0, end);
        }

        log.info("AI config loaded - baseUrl: {}, defaultModel: {}, modelIndex: {}, models: {}, apiKeyPrefix: {}, apiKeyLength: {}",
                baseUrl, model, modelIndex, models, keyPrefix, keyLength);
    }

    /**
     * 处理用户提问（核心方法）
     * 流程：意图识别 → RAG检索 → 路由Prompt + 三层记忆注入 → 调用LLM → 判断转人工 → 更新记忆 → 保存记录
     */
    @Override
    @Transactional
    public ChatResponse handleUserQuery(ChatRequest request) {
        String userId = request.getUserId();
        String question = request.getQuestion();
        String sessionId = request.getSessionId();
        List<String> images = request.getImages();

        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        log.info("处理用户问题 - userId: {}, sessionId: {}, question: {}", userId, sessionId, question);

        // ========== 1. 意图识别 ==========
        IntentResult intent = null;
        if (images == null || images.isEmpty()) {
            try {
                intent = intentRecognitionService.recognize(userId, question);
                log.info("意图识别结果 - intents: {}, confidence: {}, slots: {}",
                        intent.getIntents(), intent.getConfidence(), intent.getSlots());
            } catch (Exception e) {
                log.warn("意图识别失败，使用默认意图: {}", e.getMessage());
            }
        }

        // ========== 2. RAG 知识库检索 ==========
        KnowledgeSearchResult searchResult = null;
        List<String> sources = new ArrayList<>();

        if (images == null || images.isEmpty()) {
            try {
                searchResult = knowledgeBaseService.search(question, 3);
                if (searchResult != null && searchResult.getResults() != null && !searchResult.getResults().isEmpty()) {
                    log.info("RAG 检索到 {} 条相关知识", searchResult.getResults().size());
                    sources = searchResult.getResults().stream()
                            .map(KnowledgeSearchResult.ChunkResult::getSource)
                            .distinct()
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.warn("RAG 检索失败，使用通用知识", e);
            }
        }

        // ========== 3. 获取三层记忆上下文 ==========
        MemoryContext memory = sessionMemoryService.getMemoryContext(userId, sessionId);

        // ========== 4. 更新关键事实（从意图识别的槽位中提取） ==========
        if (intent != null && intent.getSlots() != null && !intent.getSlots().isEmpty()) {
            // 过滤掉 null 和空值的槽位
            Map<String, String> validSlots = intent.getSlots().entrySet().stream()
                    .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (!validSlots.isEmpty()) {
                sessionMemoryService.updateFacts(userId, sessionId, validSlots);
                // 重新获取更新后的记忆
                memory = sessionMemoryService.getMemoryContext(userId, sessionId);
            }
        }

        // ========== 5. 构建增强 Prompt ==========
        String fullPrompt = buildEnhancedPrompt(question, request, intent, searchResult, memory);

        // ========== 6. 调用 LLM ==========
        String aiReply = callModelScopeApi(fullPrompt, images);

        // ========== 7. 保存到三层记忆 ==========
        sessionMemoryService.saveMessage(userId, sessionId, question, aiReply);

        // ========== 8. 判断是否需要转人工 ==========
        boolean shouldTransfer = shouldTransferToHuman(userId, question, aiReply);
        if (shouldTransfer) {
            log.info("检测到需要转人工 - userId: {}", userId);
            HumanTransferService.TransferResult result = humanTransferService.transferToHuman(userId, sessionId, question);
            saveChatRecord(userId, sessionId, question, aiReply);
            extractUserPreferences(userId, question);
            return new ChatResponse(result.message(), sources.isEmpty() ? List.of("通用知识") : sources, sessionId);
        }

        // ========== 9. 保存对话记录 + 提取偏好 ==========
        saveChatRecord(userId, sessionId, question, aiReply);
        extractUserPreferences(userId, question);
        return new ChatResponse(aiReply, sources.isEmpty() ? List.of("通用知识") : sources, sessionId);
    }

    /**
     * 构建增强 Prompt（意图路由 + RAG + 三层记忆）
     */
    private String buildEnhancedPrompt(String question, ChatRequest request,
                                        IntentResult intent, KnowledgeSearchResult searchResult,
                                        MemoryContext memory) {
        // 1. 根据意图选择 System Prompt
        String intentType = intent != null ? intent.getPrimaryIntent() : "general";
        String systemPrompt = PromptRouter.getSystemPrompt(intentType);

        // 2. 获取时间信息
        LocalDateTime now = LocalDateTime.now();
        String currentDate = now.toLocalDate().toString();
        String dayOfWeek = getDayOfWeekChinese(now);
        String currentTime = now.toLocalTime().toString().substring(0, 5);

        // 3. 构建用户信息
        String userInfo = buildUserInfo(request);

        // 4. 构建 RAG 知识库上下文
        String ragContext = buildRagContext(searchResult);

        // 5. 获取记忆文本（三层：Facts + Summary + Recent）
        String memoryText = memory != null ? memory.toPromptText() : "";

        return String.format("""
                %s

                【当前时间信息】
                - 日期：%s
                - 星期：%s
                - 时间：%s

                重要规则：
                - 如果用户询问时间、日期、星期等问题，必须以上述【当前时间信息】为准。
                - 如果用户询问"贺锦天"是谁，统一回答：贺锦天是我们的老板/负责人。
                - 优先使用【相关知识库内容】中的信息回答，如果知识库有相关内容必须基于知识库作答。
                - 如果知识库内容与问题无关或为空，使用通用知识回答。
                - 如果记忆中有关键事实（订单号、地址等），优先使用记忆中的信息。

                %s

                %s

                %s

                %s

                用户最新提问：%s

                请按照以下要求生成回复：
                1. 语气与风格：保持热情、耐心、简洁。使用口语化表达。
                2. 结构化：如果问题涉及多个方面，请分点说明。
                3. 准确性：根据用户信息和记忆进行个性化回复。涉及日期时间的问题必须使用上述时间信息。
                4. 行动引导：在回答末尾，根据问题自然引导用户下一步操作。
                5. 格式：直接输出纯文本，不要使用 Markdown，项目符号等任何格式符号。

                现在，请开始你的回答：
                """, systemPrompt, currentDate, dayOfWeek, currentTime,
                ragContext, userInfo, memoryText,
                memoryText.isEmpty() ? "（暂无对话记忆）" : "",
                question);
    }

    /**
     * 构建 RAG 知识库上下文
     */
    private String buildRagContext(KnowledgeSearchResult searchResult) {
        if (searchResult == null || searchResult.getResults() == null || searchResult.getResults().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【相关知识库内容】\n");
        if (searchResult.getRouteType() != null) {
            sb.append("检索路由：").append(searchResult.getRouteType());
            if (searchResult.getSearchType() != null) {
                sb.append(" / ").append(searchResult.getSearchType());
            }
            if (searchResult.isFallback()) {
                sb.append("（已兜底）");
            }
            sb.append("\n");
        }

        if (searchResult.getEvidences() != null && !searchResult.getEvidences().isEmpty()) {
            for (int i = 0; i < searchResult.getEvidences().size(); i++) {
                var evidence = searchResult.getEvidences().get(i);
                sb.append(String.format("  [%d] 类型：%s，来源：%s，标题：%s\n      内容：%s\n\n",
                        i + 1,
                        evidence.getType() != null ? evidence.getType() : "unknown",
                        evidence.getSource() != null ? evidence.getSource() : "unknown",
                        evidence.getTitle() != null ? evidence.getTitle() : "未命名证据",
                        evidence.getContent()));
            }
        } else {
            for (int i = 0; i < searchResult.getResults().size(); i++) {
                var result = searchResult.getResults().get(i);
                sb.append(String.format("  [%d] 类型：%s，来源：%s\n      内容：%s\n\n",
                        i + 1,
                        result.getEvidenceType() != null ? result.getEvidenceType() : "vector",
                        result.getSource(), result.getContent()));
            }
        }
        return sb.toString();
    }

    /**
     * 构建用户信息
     */
    private String buildUserInfo(ChatRequest request) {
        if (request == null) return "（暂无用户信息）";

        StringBuilder userInfo = new StringBuilder();
        userInfo.append("【用户信息】\n");

        boolean hasInfo = false;
        if (request.getNickname() != null) {
            userInfo.append("- 昵称：").append(request.getNickname()).append("\n");
            hasInfo = true;
        }
        if (request.getOrderCount() != null) {
            userInfo.append("- 订单总数：").append(request.getOrderCount()).append("单\n");
            hasInfo = true;
        }
        if (request.getPendingOrderCount() != null && request.getPendingOrderCount() > 0) {
            userInfo.append("- 待付款：").append(request.getPendingOrderCount()).append("单\n");
            hasInfo = true;
        }
        if (request.getDeliveringOrderCount() != null && request.getDeliveringOrderCount() > 0) {
            userInfo.append("- 配送中：").append(request.getDeliveringOrderCount()).append("单\n");
            hasInfo = true;
        }
        if (request.getCompletedOrderCount() != null) {
            userInfo.append("- 已完成：").append(request.getCompletedOrderCount()).append("单\n");
            hasInfo = true;
        }
        if (request.getDefaultAddress() != null && !request.getDefaultAddress().isEmpty()) {
            userInfo.append("- 默认收货地址：").append(request.getDefaultAddress()).append("\n");
            hasInfo = true;
        }

        return hasInfo ? userInfo.toString() : "（暂无用户信息）";
    }

    private String getDayOfWeekChinese(LocalDateTime now) {
        return switch (now.getDayOfWeek()) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }

    /**
     * 调用 ModelScope API（多模型轮询）
     */
    private String callModelScopeApi(String prompt, List<String> images) {
        if (models == null || models.isEmpty()) {
            models = List.of("qwen/Qwen3-1.7B", "ZhipuAI/GLM-4.7-Flash");
        }

        String lastError = "";

        if (images != null && !images.isEmpty()) {
            return callModelScopeApiWithImage(prompt, images);
        }

        int maxRetries = models.size();

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            String currentModel = models.get(currentModelIndex);

            try {
                log.debug("尝试模型: {}, 第{}次请求", currentModel, attempt + 1);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", currentModel);

                List<Object> messages = new ArrayList<>();
                Map<String, Object> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", prompt);

                messages.add(userMessage);
                requestBody.put("messages", messages);
                requestBody.put("stream", false);
                requestBody.put("enable_thinking", false);

                String responseJson = restClient.post()
                        .uri(baseUrl + "/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);

                if (responseMap.containsKey("choices")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        if (choice.containsKey("message")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> message = (Map<String, Object>) choice.get("message");
                            if (message.containsKey("content")) {
                                String content = (String) message.get("content");
                                content = cleanAIResponse(content);
                                log.info("模型 {} 调用成功", currentModel);
                                return content;
                            }
                        }
                    }
                }

                log.warn("模型 {} 响应格式异常，尝试下一个模型", currentModel);
                lastError = "响应格式异常";

            } catch (Exception e) {
                log.warn("模型 {} 调用失败: {}", currentModel, e.getMessage());
                lastError = e.getMessage();
            }

            currentModelIndex = (currentModelIndex + 1) % models.size();
        }

        log.error("所有模型都调用失败: {}", lastError);
        return "抱歉，AI 服务暂时不可用，请稍后重试。";
    }

    /**
     * 调用 ModelScope 视觉模型（图片识别）
     */
    private String callModelScopeApiWithImage(String prompt, List<String> images) {
        String visionModel = "moonshotai/Kimi-K2.5";
        log.info("检测到图片，使用视觉模型: {}", visionModel);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", visionModel);

            List<Object> messages = new ArrayList<>();
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");

            List<Map<String, Object>> contentList = new ArrayList<>();

            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", prompt.isEmpty() ? "请描述这张图片的内容" : prompt);
            contentList.add(textContent);

            for (String imageUrl : images) {
                String base64Image = convertImageToBase64(imageUrl);
                if (base64Image != null) {
                    Map<String, Object> imageContent = new HashMap<>();
                    imageContent.put("type", "image_url");
                    Map<String, String> imageUrlObj = new HashMap<>();
                    imageUrlObj.put("url", base64Image);
                    imageContent.put("image_url", imageUrlObj);
                    contentList.add(imageContent);
                }
            }

            userMessage.put("content", contentList);
            messages.add(userMessage);
            requestBody.put("messages", messages);
            requestBody.put("stream", false);

            String responseJson = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);

            if (responseMap.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    if (choice.containsKey("message")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        if (message.containsKey("content")) {
                            String content = (String) message.get("content");
                            content = cleanAIResponse(content);
                            log.info("视觉模型 {} 识别成功", visionModel);
                            return content;
                        }
                    }
                }
            }

            log.warn("视觉模型 {} 响应格式异常", visionModel);
            return "图片识别失败，请稍后重试。";

        } catch (Exception e) {
            log.error("视觉模型 {} 调用失败: {}", visionModel, e.getMessage());
            return "抱歉，图片识别服务暂时不可用：" + e.getMessage();
        }
    }

    private String convertImageToBase64(String imageUrl) {
        try {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : ".jpg";
            String mimeType = switch (extension.toLowerCase()) {
                case ".png" -> "image/png";
                case ".gif" -> "image/gif";
                case ".webp" -> "image/webp";
                default -> "image/jpeg";
            };

            java.nio.file.Path imagePath = java.nio.file.Paths.get(uploadDir, "chat-images", fileName);
            if (!java.nio.file.Files.exists(imagePath)) {
                log.warn("图片文件不存在: {}", imagePath);
                return null;
            }

            byte[] imageBytes = java.nio.file.Files.readAllBytes(imagePath);
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            return "data:" + mimeType + ";base64," + base64;

        } catch (Exception e) {
            log.error("图片转Base64失败: {}", e.getMessage());
            return null;
        }
    }

    private String cleanAIResponse(String content) {
        if (content == null || content.isEmpty()) return content;
        content = content.trim();
        while (content.startsWith("\"") || content.startsWith("'") ||
               content.startsWith("```") || content.startsWith("**") ||
               content.startsWith("「") || content.startsWith("『")) {
            content = content.substring(1).trim();
        }
        while (content.endsWith("\"") || content.endsWith("'") ||
               content.endsWith("```") || content.endsWith("**") ||
               content.endsWith("」") || content.endsWith("』")) {
            content = content.substring(0, content.length() - 1).trim();
        }
        return content;
    }

    @Override
    @Cacheable(value = "chatHistory", key = "#userId", unless = "#result.isEmpty()")
    public String getChatHistory(String userId) {
        List<ChatRecord> recentRecords = chatRecordRepository.findRecentByUserId(userId, chatHistoryLimit);

        if (recentRecords.isEmpty()) return "";

        recentRecords.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));

        StringBuilder history = new StringBuilder();
        for (ChatRecord record : recentRecords) {
            if ("user".equals(record.getRole())) {
                history.append("用户: ").append(record.getQuestion()).append("\n");
            } else if ("assistant".equals(record.getRole())) {
                history.append("客服: ").append(record.getAnswer()).append("\n");
            }
        }

        return history.toString();
    }

    @Override
    public boolean shouldTransferToHuman(String userId, String question, String aiResponse) {
        return humanTransferService.shouldTransfer(userId, question, aiResponse);
    }

    @Transactional
    @CacheEvict(value = "chatHistory", key = "#userId")
    public void saveChatRecord(String userId, String sessionId, String question, String answer) {
        LocalDateTime now = LocalDateTime.now();

        ChatRecord userRecord = new ChatRecord();
        userRecord.setUserId(userId);
        userRecord.setSessionId(sessionId);
        userRecord.setQuestion(question);
        userRecord.setAnswer(null);
        userRecord.setRole("user");
        userRecord.setCreatedAt(now);
        chatRecordRepository.insert(userRecord);

        ChatRecord assistantRecord = new ChatRecord();
        assistantRecord.setUserId(userId);
        assistantRecord.setSessionId(sessionId);
        assistantRecord.setQuestion(null);
        assistantRecord.setAnswer(answer);
        assistantRecord.setRole("assistant");
        assistantRecord.setCreatedAt(now.plusSeconds(1));
        chatRecordRepository.insert(assistantRecord);
    }

    private void extractUserPreferences(String userId, String question) {
        try {
            Long parsedUserId;
            try {
                parsedUserId = Long.parseLong(userId);
            } catch (NumberFormatException e) {
                log.warn("无法解析userId: {}", userId);
                return;
            }

            int savedCount = userPreferenceService.extractAndSavePreferences(parsedUserId, question);
            if (savedCount > 0) {
                log.info("成功提取并保存用户 {} 的 {} 条偏好", userId, savedCount);
            }
        } catch (Exception e) {
            log.error("提取用户偏好失败 - userId: {}", userId, e);
        }
    }
}
