package com.aiwechat.aichat.service.impl;

import com.aiwechat.aichat.model.dto.IntentResult;
import com.aiwechat.aichat.service.IntentRecognitionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 意图识别服务实现
 * 调用 ModelScope API，让 LLM 输出结构化 JSON（意图 + 槽位 + 置信度）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRecognitionServiceImpl implements IntentRecognitionService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    private static final String INTENT_PROMPT = """
            你是微信点餐小程序的意图识别器。你必须只输出 JSON，不要输出任何解释文字。
            输出 JSON schema：
            {"intents": ["intent1"], "slots": {"order_number": null, "menu_name": null, "address": null, "phone": null}, "confidence": 0.0}

            候选 intents：
            - menu_inquiry    菜品咨询（菜单、价格、口味、推荐菜品）
            - order_inquiry   订单查询（查订单状态、订单详情）
            - order_cancel    取消订单
            - delivery_inquiry 配送查询（配送进度、配送时间、配送费）
            - refund          退款退货
            - complaint       投诉（对菜品/服务不满）
            - general         其他/通用闲聊

            规则：
            - intents 可包含多个
            - confidence 取值 0~1
            - 未提到订单号则 order_number 为 null；未提到菜名则 menu_name 为 null
            - 未提到地址则 address 为 null；未提到电话则 phone 为 null
            - 如果用户提到多个意图，都要列出来
            """;

    @Override
    public IntentResult recognize(String userId, String question) {
        return recognize(userId, question, null);
    }

    @Override
    public IntentResult recognize(String userId, String question, String conversationHistory) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", INTENT_PROMPT));

            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                messages.add(Map.of("role", "user", "content",
                        "最近的对话历史：\n" + conversationHistory + "\n\n当前问题：" + question));
            } else {
                messages.add(Map.of("role", "user", "content", question));
            }
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

            return parseIntentResponse(responseJson);
        } catch (Exception e) {
            log.warn("意图识别失败，降级为通用意图: {}", e.getMessage());
            IntentResult fallback = new IntentResult();
            fallback.getIntents().add("general");
            fallback.setConfidence(0.5);
            return fallback;
        }
    }

    /**
     * 解析 LLM 返回的意图识别结果
     */
    private IntentResult parseIntentResponse(String responseJson) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(responseJson, new TypeReference<>() {});
            if (responseMap.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    return extractJsonFromContent(content);
                }
            }
        } catch (Exception e) {
            log.warn("解析意图识别响应失败: {}", e.getMessage());
        }

        IntentResult fallback = new IntentResult();
        fallback.getIntents().add("general");
        fallback.setConfidence(0.5);
        return fallback;
    }

    /**
     * 从 LLM 输出中提取 JSON（可能包裹在 ```json ... ``` 中）
     */
    private IntentResult extractJsonFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return defaultIntent();
        }

        content = content.strip();
        if (content.startsWith("```")) {
            int start = content.indexOf('\n');
            if (start > 0) {
                content = content.substring(start + 1);
            } else {
                content = content.substring(3);
            }
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        content = content.strip();

        try {
            return objectMapper.readValue(content, IntentResult.class);
        } catch (Exception e) {
            log.warn("JSON 解析失败，尝试正则提取: {}", e.getMessage());
            return defaultIntent();
        }
    }

    private IntentResult defaultIntent() {
        IntentResult result = new IntentResult();
        result.getIntents().add("general");
        result.setConfidence(0.5);
        return result;
    }
}
