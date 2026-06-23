package com.aiwechat.aichat.service;

import com.aiwechat.aichat.model.dto.IntentResult;

/**
 * 意图识别服务接口
 * 通过 LLM 识别用户意图并提取槽位信息
 */
public interface IntentRecognitionService {

    /**
     * 识别用户意图
     *
     * @param userId   用户ID
     * @param question 用户输入
     * @return 意图识别结果
     */
    IntentResult recognize(String userId, String question);
}
