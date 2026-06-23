package com.aiwechat.admin.service;

import java.util.List;
import java.util.Map;

public interface FeedbackService {

    record FeedbackItem(
            Long id,
            Long userId,
            String sessionId,
            String question,
            String aiAnswer,
            int rating,
            String comment,
            String createdAt
    ) {}

    void submitFeedback(Long userId, String sessionId, String question, String aiAnswer, int rating, String comment);

    List<FeedbackItem> getFeedbackByUserId(Long userId);

    List<FeedbackItem> getAllFeedback();

    double getAverageRating();

    Map<String, Object> getStats();

    void deleteFeedback(Long id);

    void batchDeleteFeedback(List<Long> ids);

    void clearAll();
}
