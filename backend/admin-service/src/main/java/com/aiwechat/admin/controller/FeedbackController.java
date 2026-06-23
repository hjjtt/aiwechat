package com.aiwechat.admin.controller;

import com.aiwechat.admin.service.FeedbackService;
import com.aiwechat.common.model.dto.ApiResponse;
import com.aiwechat.common.util.UserContextHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ApiResponse<Void> submitFeedback(@RequestBody Map<String, Object> body, HttpServletRequest httpRequest) {
        Long userId = UserContextHelper.requireUserId(httpRequest);
        String sessionId = (String) body.getOrDefault("sessionId", "");
        String question = (String) body.getOrDefault("question", "");
        String aiAnswer = (String) body.getOrDefault("aiAnswer", "");

        int rating;
        Object ratingObj = body.get("rating");
        if (ratingObj instanceof Number) {
            rating = ((Number) ratingObj).intValue();
        } else {
            return ApiResponse.error("评分不能为空");
        }

        if (rating < 1 || rating > 5) {
            return ApiResponse.error("评分必须在 1-5 之间");
        }

        String comment = (String) body.getOrDefault("comment", "");
        feedbackService.submitFeedback(userId, sessionId, question, aiAnswer, rating, comment);
        return ApiResponse.success(null, "反馈提交成功");
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<FeedbackService.FeedbackItem>> getUserFeedback(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        Long authenticatedUserId = UserContextHelper.requireUserId(httpRequest);
        if (!authenticatedUserId.equals(userId)) {
            return ApiResponse.error("无权查看他人反馈");
        }
        return ApiResponse.success(feedbackService.getFeedbackByUserId(userId));
    }
}
