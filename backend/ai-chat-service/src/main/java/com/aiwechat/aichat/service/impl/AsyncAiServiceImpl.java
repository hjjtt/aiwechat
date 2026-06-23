package com.aiwechat.aichat.service.impl;

import com.aiwechat.aichat.model.dto.ChatRequest;
import com.aiwechat.aichat.model.dto.ChatResponse;
import com.aiwechat.aichat.service.AICustomerService;
import com.aiwechat.aichat.service.AsyncAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步 AI 处理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncAiServiceImpl implements AsyncAiService {

    private final AICustomerService aiCustomerService;

    @Async("aiTaskExecutor")
    @Override
    public void processAsync(String userId, String question) {
        long start = System.currentTimeMillis();

        try {
            log.info("开始异步处理问题 - userId: {}", userId);

            ChatRequest request = new ChatRequest();
            request.setUserId(userId);
            request.setQuestion(question);

            ChatResponse response = aiCustomerService.handleUserQuery(request);

            log.info("异步处理完成 - userId: {}, 耗时: {}ms", userId, System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("异步处理失败 - userId: {}", userId, e);
        }
    }
}
