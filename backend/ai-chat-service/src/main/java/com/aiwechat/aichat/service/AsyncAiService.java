package com.aiwechat.aichat.service;

/**
 * 异步 AI 处理服务接口
 */
public interface AsyncAiService {

    /**
     * 异步处理用户问题
     *
     * @param userId   用户ID
     * @param question 用户问题
     */
    void processAsync(String userId, String question);
}
