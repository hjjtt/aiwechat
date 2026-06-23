package com.aiwechat.aichat.service;

/**
 * 用户偏好服务接口
 */
public interface UserPreferenceService {

    /**
     * 从用户输入文本中提取并保存偏好
     *
     * @param userId   用户ID
     * @param userInput 用户输入文本
     * @return 提取并保存的偏好数量
     */
    int extractAndSavePreferences(Long userId, String userInput);
}
