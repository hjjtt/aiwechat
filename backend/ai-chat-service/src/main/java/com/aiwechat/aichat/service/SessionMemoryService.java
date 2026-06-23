package com.aiwechat.aichat.service;

import com.aiwechat.aichat.model.dto.MemoryContext;

import java.util.Map;

/**
 * 会话记忆管理服务接口
 * 三层记忆架构：Facts(关键事实) + Summary(增量摘要) + Recent(近期会话)
 */
public interface SessionMemoryService {

    /**
     * 获取用户的记忆上下文
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 记忆上下文
     */
    MemoryContext getMemoryContext(String userId, String sessionId);

    /**
     * 保存一轮对话到记忆中
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param question  用户问题
     * @param answer    AI回答
     */
    void saveMessage(String userId, String sessionId, String question, String answer);

    /**
     * 更新关键事实（从意图识别的槽位中提取）
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param facts     事实键值对
     */
    void updateFacts(String userId, String sessionId, Map<String, String> facts);

    /**
     * 获取关键事实
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 事实字典
     */
    Map<String, String> getFacts(String userId, String sessionId);

    /**
     * 清除会话记忆
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     */
    void clearMemory(String userId, String sessionId);
}
