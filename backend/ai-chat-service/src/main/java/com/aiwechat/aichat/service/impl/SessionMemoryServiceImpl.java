package com.aiwechat.aichat.service.impl;

import com.aiwechat.aichat.model.dto.MemoryContext;
import com.aiwechat.aichat.service.SessionMemoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话记忆管理服务实现
 * 三层架构：Facts(关键事实) + Summary(增量摘要) + Recent(近期会话)
 * 使用内存存储，会话隔离 key = userId:sessionId
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionMemoryServiceImpl implements SessionMemoryService {

    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    /** 触发摘要的消息条数阈值（12条 = 6轮对话） */
    @Value("${memory.summary.batch-size:12}")
    private int summaryBatchSize;

    /** 摘要最大长度 */
    @Value("${memory.summary.max-length:250}")
    private int summaryMaxLength;

    /** 近期消息保留条数（未摘要的窗口） */
    @Value("${memory.recent.window-size:12}")
    private int recentWindowSize;

    /** 关键事实存储：key=userId:sessionId, value=事实字典 */
    private final ConcurrentHashMap<String, Map<String, String>> factsStore = new ConcurrentHashMap<>();

    /** 摘要存储：key=userId:sessionId, value=摘要文本 */
    private final ConcurrentHashMap<String, String> summaryStore = new ConcurrentHashMap<>();

    /** 未摘要消息计数：key=userId:sessionId, value=计数 */
    private final ConcurrentHashMap<String, Integer> unsummarizedCount = new ConcurrentHashMap<>();

    /** 近期消息存储：key=userId:sessionId, value=消息列表 */
    private final ConcurrentHashMap<String, List<MessagePair>> recentMessages = new ConcurrentHashMap<>();

    /**
     * 消息对（一轮对话：用户问 + AI答）
     */
    record MessagePair(String question, String answer, LocalDateTime time) {}

    // ==================== 接口实现 ====================

    @Override
    public MemoryContext getMemoryContext(String userId, String sessionId) {
        String key = buildKey(userId, sessionId);
        MemoryContext context = new MemoryContext();

        // 第一层：关键事实
        context.setFacts(new HashMap<>(factsStore.getOrDefault(key, Collections.emptyMap())));

        // 第二层：长期摘要
        context.setSummary(summaryStore.getOrDefault(key, ""));

        // 第三层：近期会话
        List<MessagePair> messages = recentMessages.getOrDefault(key, Collections.emptyList());
        context.setRecentMessages(buildRecentText(messages));
        context.setUnsummarizedCount(unsummarizedCount.getOrDefault(key, 0));

        return context;
    }

    @Override
    public void saveMessage(String userId, String sessionId, String question, String answer) {
        String key = buildKey(userId, sessionId);

        // 添加到近期消息
        recentMessages.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new MessagePair(question, answer, LocalDateTime.now()));

        // 增加未摘要计数（每条算1）
        int newCount = unsummarizedCount.merge(key, 2, Integer::sum); // user+assistant = 2条

        // 达到阈值时触发增量摘要
        if (newCount >= summaryBatchSize) {
            triggerSummary(key);
        }
    }

    @Override
    public void updateFacts(String userId, String sessionId, Map<String, String> facts) {
        if (facts == null || facts.isEmpty()) return;
        String key = buildKey(userId, sessionId);
        factsStore.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .putAll(facts);
        log.debug("更新关键事实 - key: {}, facts: {}", key, facts);
    }

    @Override
    public Map<String, String> getFacts(String userId, String sessionId) {
        String key = buildKey(userId, sessionId);
        return new HashMap<>(factsStore.getOrDefault(key, Collections.emptyMap()));
    }

    @Override
    public void clearMemory(String userId, String sessionId) {
        String key = buildKey(userId, sessionId);
        factsStore.remove(key);
        summaryStore.remove(key);
        unsummarizedCount.remove(key);
        recentMessages.remove(key);
        log.info("会话记忆已清除 - key: {}", key);
    }

    // ==================== 摘要生成 ====================

    /**
     * 触发增量摘要生成
     */
    private void triggerSummary(String key) {
        try {
            List<MessagePair> messages = recentMessages.get(key);
            if (messages == null || messages.isEmpty()) return;

            String oldSummary = summaryStore.getOrDefault(key, "");
            int unsummarized = unsummarizedCount.getOrDefault(key, 0);

            // 获取未摘要的消息
            int totalMessages = messages.size();
            int summarizeFrom = Math.max(0, totalMessages - unsummarized);
            List<MessagePair> newMessages = messages.subList(summarizeFrom, totalMessages);

            String newSummary = generateIncrementalSummary(oldSummary, newMessages);
            summaryStore.put(key, newSummary);
            unsummarizedCount.put(key, 0);

            // 软清理：保留最近窗口大小的消息
            if (totalMessages > recentWindowSize) {
                List<MessagePair> retained = new ArrayList<>(
                        messages.subList(totalMessages - recentWindowSize, totalMessages));
                recentMessages.put(key, retained);
            }

            log.info("增量摘要已生成 - key: {}, summaryLength: {}", key, newSummary.length());
        } catch (Exception e) {
            log.error("增量摘要生成失败 - key: {}", key, e);
        }
    }

    /**
     * 调用 LLM 生成增量摘要
     */
    private String generateIncrementalSummary(String oldSummary, List<MessagePair> newMessages) {
        String newMessagesText = buildRecentText(newMessages);

        if (newMessagesText.isEmpty()) return oldSummary;

        String prompt = String.format("""
                你是对话摘要专家。请将旧摘要和新对话合并成一个新的摘要。

                要求：
                1. 保留旧摘要中的关键信息（订单号、地址、偏好等）
                2. 整合新对话中的重要内容
                3. 控制总长度在%d字以内
                4. 直接输出摘要内容，不要任何前缀

                旧摘要：%s

                新对话：
                %s
                """, summaryMaxLength,
                oldSummary.isEmpty() ? "（无）" : oldSummary,
                newMessagesText);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", prompt)
            );
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
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");
                if (content != null) {
                    return content.trim();
                }
            }
        } catch (Exception e) {
            log.warn("LLM 摘要生成失败，使用简易摘要: {}", e.getMessage());
        }

        // 降级：简易摘要
        return simpleSummary(oldSummary, newMessagesText);
    }

    /**
     * 简易增量摘要（LLM 不可用时的降级方案）
     */
    private String simpleSummary(String oldSummary, String newMessagesText) {
        String oldPart = oldSummary.length() > 150 ? oldSummary.substring(0, 150) + "..." : oldSummary;
        String newPart = newMessagesText.length() > 150 ? newMessagesText.substring(0, 150) + "..." : newMessagesText;
        if (oldSummary.isEmpty()) {
            return newPart;
        }
        return oldPart + " | " + newPart;
    }

    // ==================== 工具方法 ====================

    private String buildKey(String userId, String sessionId) {
        return userId + ":" + sessionId;
    }

    private String buildRecentText(List<MessagePair> messages) {
        if (messages == null || messages.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (MessagePair msg : messages) {
            sb.append("用户: ").append(msg.question()).append("\n");
            sb.append("客服: ").append(msg.answer()).append("\n");
        }
        return sb.toString();
    }
}
