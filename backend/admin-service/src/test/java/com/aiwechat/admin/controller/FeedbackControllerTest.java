package com.aiwechat.admin.controller;

import com.aiwechat.admin.service.FeedbackService;
import com.aiwechat.common.model.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackControllerTest {

    @Mock
    private FeedbackService feedbackService;

    @Mock
    private HttpServletRequest httpRequest;

    private FeedbackController controller;

    @BeforeEach
    void setUp() {
        controller = new FeedbackController(feedbackService);
    }

    // ---- submitFeedback ----

    @Test
    @DisplayName("submitFeedback 评分 1-5 应该成功")
    void submitFeedbackValidRating() {
        when(httpRequest.getHeader("X-User-Id")).thenReturn("14");

        Map<String, Object> body = Map.of(
                "sessionId", "sess-abc",
                "question", "菜品推荐",
                "aiAnswer", "宫保鸡丁",
                "rating", 5,
                "comment", "很好"
        );

        ApiResponse<Void> response = controller.submitFeedback(body, httpRequest);

        assertTrue(response.isSuccess());
        verify(feedbackService).submitFeedback(14L, "sess-abc", "菜品推荐", "宫保鸡丁", 5, "很好");
    }

    @Test
    @DisplayName("submitFeedback 缺少 rating 应该返回错误")
    void submitFeedbackMissingRating() {
        when(httpRequest.getHeader("X-User-Id")).thenReturn("14");

        Map<String, Object> body = Map.of("comment", "还行");

        ApiResponse<Void> response = controller.submitFeedback(body, httpRequest);

        assertFalse(response.isSuccess());
        verify(feedbackService, never()).submitFeedback(anyLong(), any(), any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("submitFeedback rating 为 0 应该返回错误")
    void submitFeedbackRatingZero() {
        when(httpRequest.getHeader("X-User-Id")).thenReturn("14");

        Map<String, Object> body = Map.of("rating", 0);

        ApiResponse<Void> response = controller.submitFeedback(body, httpRequest);

        assertFalse(response.isSuccess());
        verify(feedbackService, never()).submitFeedback(anyLong(), any(), any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("submitFeedback rating 为 6 应该返回错误")
    void submitFeedbackRatingTooHigh() {
        when(httpRequest.getHeader("X-User-Id")).thenReturn("14");

        Map<String, Object> body = Map.of("rating", 6);

        ApiResponse<Void> response = controller.submitFeedback(body, httpRequest);

        assertFalse(response.isSuccess());
        verify(feedbackService, never()).submitFeedback(anyLong(), any(), any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("submitFeedback rating 为字符串类型应该返回错误")
    void submitFeedbackRatingString() {
        when(httpRequest.getHeader("X-User-Id")).thenReturn("14");

        Map<String, Object> body = Map.of("rating", "five");

        ApiResponse<Void> response = controller.submitFeedback(body, httpRequest);

        assertFalse(response.isSuccess());
        verify(feedbackService, never()).submitFeedback(anyLong(), any(), any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("submitFeedback 缺少 X-User-Id 应该抛出 BusinessException")
    void submitFeedbackNoUserId() {
        when(httpRequest.getHeader("X-User-Id")).thenReturn(null);

        Map<String, Object> body = Map.of("rating", 5);

        assertThrows(Exception.class, () -> controller.submitFeedback(body, httpRequest));
        verify(feedbackService, never()).submitFeedback(anyLong(), any(), any(), any(), anyInt(), any());
    }

    // ---- getUserFeedback ----

    @Test
    @DisplayName("getUserFeedback 查看自己的反馈应该成功")
    void getUserFeedbackOwn() {
        when(httpRequest.getHeader("X-User-Id")).thenReturn("14");

        ApiResponse<List<FeedbackService.FeedbackItem>> response = controller.getUserFeedback(14L, httpRequest);

        assertTrue(response.isSuccess());
        verify(feedbackService).getFeedbackByUserId(14L);
    }

    @Test
    @DisplayName("getUserFeedback 查看他人反馈应该返回错误")
    void getUserFeedbackOtherUser() {
        when(httpRequest.getHeader("X-User-Id")).thenReturn("14");

        ApiResponse<List<FeedbackService.FeedbackItem>> response = controller.getUserFeedback(99L, httpRequest);

        assertFalse(response.isSuccess());
        verify(feedbackService, never()).getFeedbackByUserId(anyLong());
    }

    @Test
    @DisplayName("getUserFeedback 缺少 X-User-Id 应该抛出异常")
    void getUserFeedbackNoAuth() {
        when(httpRequest.getHeader("X-User-Id")).thenReturn(null);

        assertThrows(Exception.class, () -> controller.getUserFeedback(14L, httpRequest));
        verify(feedbackService, never()).getFeedbackByUserId(anyLong());
    }
}
