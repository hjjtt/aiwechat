package com.aiwechat.aichat.service.impl;

import com.aiwechat.aichat.service.UserPreferenceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用户偏好服务实现
 * 通过调用 AI 模型从用户输入中提取偏好信息
 */
@Slf4j
@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** ModelScope API 密钥 */
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    /** ModelScope API 基础URL */
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    /** 默认模型 */
    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Override
    public int extractAndSavePreferences(Long userId, String userInput) {
        if (userId == null || userInput == null || userInput.trim().isEmpty()) {
            return 0;
        }

        try {
            // 调用AI提取偏好
            String extractedJson = extractPreferencesFromAI(userInput);

            if (extractedJson == null || extractedJson.isEmpty()) {
                return 0;
            }

            log.debug("AI 提取偏好结果: {}", extractedJson);

            // TODO: 后续对接用户偏好存储，当前仅日志记录
            return 0;

        } catch (Exception e) {
            log.error("提取用户偏好失败 - userId: {}, input: {}", userId, userInput, e);
            return 0;
        }
    }

    /**
     * 调用AI提取偏好信息
     */
    private String extractPreferencesFromAI(String userInput) {
        String prompt = buildPreferenceExtractionPrompt(userInput);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

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
                            return (String) message.get("content");
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("调用AI提取偏好失败", e);
        }

        return null;
    }

    /**
     * 构建偏好提取的 Prompt
     */
    private String buildPreferenceExtractionPrompt(String userInput) {
        return """
                你是一个用户偏好提取助手。请从以下用户输入中提取偏好信息。

                用户输入: "%s"

                重要规则：
                1. 只能提取用户明确、直接表达的偏好，不要推测或假设
                2. 用户没有提到的内容绝对不要提取
                3. 如果不确定用户是否表达了这个偏好，坚决不提取

                判断标准：
                - "我喜欢辣" = 明确表达偏好
                - "我不吃辣" = 明确表达忌口
                - "我一般吃牛肉" = 明确表达喜好
                - "我不吃辣有什么推荐的吗" = 没有明确偏好，不提取任何内容
                - "今天吃什么" = 没有偏好，不提取任何内容

                请以JSON格式返回：
                {"preferences": [
                    {"type": "taste", "key": "spicy", "value": "辣", "confidence": 0.95}
                ]}

                偏好类型和键值（只能使用这些）：
                - taste: spicy(辣), sweet(甜), sour(酸), bitter(苦), salty(咸), mild(清淡), heavy(重口)
                - avoid: spicy(辣), peanut(花生), seafood(海鲜), beef(牛肉), chicken(鸡肉), pork(猪肉), vegetarian(素食)
                - favorite: beef(牛肉), chicken(鸡肉), pork(猪肉), seafood(海鲜), vegetable(蔬菜)
                - price: low-price(实惠), medium-price(适中), high-price(高端)

                置信度规则（必须严格遵守）：
                - 0.95: 用户非常明确表达的偏好（包含"喜欢"、"不爱"、"不吃"、"最愛"等强烈词汇）
                - 0.90: 用户明确表达的偏好（如"我爱吃辣"、"我不吃花生"）
                - 低于0.90: 不要提取

                如果没有提取到任何偏好，返回：{"preferences": []}

                只返回JSON，不要其他内容。
                """.formatted(userInput);
    }
}
