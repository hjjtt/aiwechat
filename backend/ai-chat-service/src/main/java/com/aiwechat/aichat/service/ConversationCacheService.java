package com.aiwechat.aichat.service;

import java.util.List;

/**
 * 对话缓存服务接口
 * 专门用于缓存高频问答和会话状态
 */
public interface ConversationCacheService {

    /**
     * 缓存问题答案对
     *
     * @param question 问题
     * @param answer   答案
     */
    void cacheQA(String question, String answer);

    /**
     * 获取缓存的答案
     *
     * @param question 问题
     * @return 缓存的答案，如果没有则返回null
     */
    String getCachedAnswer(String question);

    /**
     * 缓存会话状态
     *
     * @param sessionId 会话ID
     * @param status    状态
     */
    void cacheSessionStatus(String sessionId, String status);

    /**
     * 获取会话状态
     *
     * @param sessionId 会话ID
     * @return 会话状态
     */
    String getSessionStatus(String sessionId);

    /**
     * 删除会话缓存
     *
     * @param sessionId 会话ID
     */
    void deleteSession(String sessionId);

    /**
     * 获取高频问题列表
     *
     * @return 高频问题列表
     */
    List<String> getFrequentQuestions();

    /**
     * 缓存热门问题
     *
     * @param question 问题
     */
    void cacheHotQuestion(String question);
}
