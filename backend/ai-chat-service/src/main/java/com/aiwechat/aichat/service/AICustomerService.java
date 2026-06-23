package com.aiwechat.aichat.service;

import com.aiwechat.aichat.model.dto.ChatRequest;
import com.aiwechat.aichat.model.dto.ChatResponse;

/**
 * AI 客服服务接口
 */
public interface AICustomerService {

    /**
     * 处理用户提问
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse handleUserQuery(ChatRequest request);

    /**
     * 获取对话历史
     *
     * @param userId 用户ID
     * @return 对话历史字符串
     */
    String getChatHistory(String userId);

    /**
     * 判断是否需要转人工客服
     *
     * @param userId     用户ID
     * @param question   用户问题
     * @param aiResponse AI回复
     * @return 是否需要转人工
     */
    boolean shouldTransferToHuman(String userId, String question, String aiResponse);
}
