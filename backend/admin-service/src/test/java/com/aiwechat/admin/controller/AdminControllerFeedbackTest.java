package com.aiwechat.admin.controller;

import com.aiwechat.admin.service.FeedbackService;
import com.aiwechat.admin.service.HumanTransferService;
import com.aiwechat.admin.service.PerformanceMonitorService;
import com.aiwechat.common.model.dto.ApiResponse;
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
class AdminControllerFeedbackTest {

    @Mock
    private PerformanceMonitorService performanceMonitorService;

    @Mock
    private FeedbackService feedbackService;

    @Mock
    private HumanTransferService humanTransferService;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(performanceMonitorService, feedbackService, humanTransferService);
    }

    // ---- getAllFeedback ----

    @Test
    @DisplayName("getAllFeedback 应该返回全部反馈列表")
    void getAllFeedback() {
        List<FeedbackService.FeedbackItem> mockList = List.of(
                new FeedbackService.FeedbackItem(1L, 14L, "s1", "问题", "回答", 5, "好评", "2026-04-24 10:00:00")
        );
        when(feedbackService.getAllFeedback()).thenReturn(mockList);

        ApiResponse<List<FeedbackService.FeedbackItem>> response = controller.getAllFeedback();

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        verify(feedbackService).getAllFeedback();
    }

    // ---- getFeedbackStats ----

    @Test
    @DisplayName("getFeedbackStats 应该返回统计数据")
    void getFeedbackStats() {
        Map<String, Object> mockStats = Map.of(
                "totalFeedback", 10,
                "averageRating", 4.2,
                "goodFeedback", 7,
                "poorFeedback", 1
        );
        when(feedbackService.getStats()).thenReturn(mockStats);

        ApiResponse<Map<String, Object>> response = controller.getFeedbackStats();

        assertTrue(response.isSuccess());
        assertEquals(10, response.getData().get("totalFeedback"));
        assertEquals(4.2, response.getData().get("averageRating"));
    }

    // ---- deleteFeedback ----

    @Test
    @DisplayName("deleteFeedback 应该调用 service 删除指定 ID")
    void deleteFeedback() {
        ApiResponse<Void> response = controller.deleteFeedback(42L);

        assertTrue(response.isSuccess());
        verify(feedbackService).deleteFeedback(42L);
    }

    // ---- batchDeleteFeedback ----

    @Test
    @DisplayName("batchDeleteFeedback 应该批量删除")
    void batchDeleteFeedback() {
        Map<String, List<Long>> body = Map.of("ids", List.of(1L, 2L, 3L));

        ApiResponse<Void> response = controller.batchDeleteFeedback(body);

        assertTrue(response.isSuccess());
        verify(feedbackService).batchDeleteFeedback(List.of(1L, 2L, 3L));
    }

    @Test
    @DisplayName("batchDeleteFeedback ids 为空应该返回错误")
    void batchDeleteFeedbackEmptyIds() {
        Map<String, List<Long>> body = Map.of("ids", List.of());

        ApiResponse<Void> response = controller.batchDeleteFeedback(body);

        assertFalse(response.isSuccess());
        verify(feedbackService, never()).batchDeleteFeedback(anyList());
    }

    @Test
    @DisplayName("batchDeleteFeedback ids 为 null 应该返回错误")
    void batchDeleteFeedbackNullIds() {
        Map<String, List<Long>> body = Map.of();

        ApiResponse<Void> response = controller.batchDeleteFeedback(body);

        assertFalse(response.isSuccess());
        verify(feedbackService, never()).batchDeleteFeedback(anyList());
    }

    // ---- clearAllFeedback ----

    @Test
    @DisplayName("clearAllFeedback 应该清空全部反馈")
    void clearAllFeedback() {
        ApiResponse<Void> response = controller.clearAllFeedback();

        assertTrue(response.isSuccess());
        verify(feedbackService).clearAll();
    }
}
