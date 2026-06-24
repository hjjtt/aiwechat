package com.aiwechat.aichat.service;

import com.aiwechat.aichat.model.dto.IntentResult;

/**
 * 意图识别服务接口
 * 通过 LLM 识别用户意图并提取槽位信息
 */
public interface IntentRecognitionService {

    IntentResult recognize(String userId, String question);

    IntentResult recognize(String userId, String question, String conversationHistory);
}
