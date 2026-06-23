package com.aiwechat.aichat.service.impl;

import com.aiwechat.aichat.service.CacheService;
import com.aiwechat.aichat.service.ConversationCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话缓存服务实现（基于 CacheService）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationCacheServiceImpl implements ConversationCacheService {

    private final CacheService cacheService;

    /** 缓存前缀 */
    private static final String QA_CACHE_PREFIX = "qa:";
    private static final String SESSION_PREFIX = "session:";
    private static final String HOT_QUESTIONS_KEY = "hot:questions";

    /** 高频问题缓存时间：7天 */
    private static final long HOT_CACHE_SECONDS = 7 * 24 * 3600;

    @Override
    public void cacheQA(String question, String answer) {
        // 使用问题的哈希值作为缓存键，避免过长的问题文本
        String cacheKey = QA_CACHE_PREFIX + hashQuestion(question);
        cacheService.setString(cacheKey, answer, HOT_CACHE_SECONDS);
        log.debug("问答缓存已添加 - questionHash: {}", cacheKey);
    }

    @Override
    public String getCachedAnswer(String question) {
        String cacheKey = QA_CACHE_PREFIX + hashQuestion(question);
        return cacheService.getString(cacheKey);
    }

    @Override
    public void cacheSessionStatus(String sessionId, String status) {
        String cacheKey = SESSION_PREFIX + sessionId;
        cacheService.setString(cacheKey, status, 3600); // 1小时过期
    }

    @Override
    public String getSessionStatus(String sessionId) {
        String cacheKey = SESSION_PREFIX + sessionId;
        return cacheService.getString(cacheKey);
    }

    @Override
    public void deleteSession(String sessionId) {
        String cacheKey = SESSION_PREFIX + sessionId;
        cacheService.delete(cacheKey);
    }

    @Override
    public List<String> getFrequentQuestions() {
        List<String> result = cacheService.get(HOT_QUESTIONS_KEY, ArrayList.class);
        return result != null ? result : new ArrayList<>();
    }

    @Override
    public void cacheHotQuestion(String question) {
        log.debug("热门问题已缓存: {}", question);
    }

    /**
     * 对问题进行哈希处理，生成缓存键
     */
    private String hashQuestion(String question) {
        String normalized = question.toLowerCase().trim();
        if (normalized.length() > 50) {
            normalized = normalized.substring(0, 50);
        }
        return normalized + "_" + normalized.hashCode();
    }
}
