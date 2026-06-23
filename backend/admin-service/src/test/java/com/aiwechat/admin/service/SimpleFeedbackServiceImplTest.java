package com.aiwechat.admin.service;

import com.aiwechat.admin.service.impl.SimpleFeedbackServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleFeedbackServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SimpleFeedbackServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SimpleFeedbackServiceImpl(jdbcTemplate);
    }

    // ---- submitFeedback ----

    @Test
    @DisplayName("submitFeedback 应该执行正确的 INSERT SQL")
    void submitFeedbackShouldInsert() {
        service.submitFeedback(1L, "sess-1", "菜品推荐", "宫保鸡丁", 5, "很好吃");

        verify(jdbcTemplate).update(
                eq("INSERT INTO feedback (user_id, session_id, question, ai_answer, rating, comment, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)"),
                eq(1L), eq("sess-1"), eq("菜品推荐"), eq("宫保鸡丁"), eq(5), eq("很好吃"),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("submitFeedback 参数为 null 时不报错，直接传给 JdbcTemplate")
    void submitFeedbackWithNullFields() {
        service.submitFeedback(2L, null, null, null, 3, null);

        verify(jdbcTemplate).update(
                anyString(),
                eq(2L), isNull(), isNull(), isNull(), eq(3), isNull(),
                any(LocalDateTime.class)
        );
    }

    // ---- getFeedbackByUserId ----

    @Test
    @DisplayName("getFeedbackByUserId 应该按 userId 查询并按时间倒序")
    void getFeedbackByUserIdShouldQueryByUserId() {
        List<FeedbackService.FeedbackItem> mockResult = List.of();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L))).thenReturn(mockResult);

        List<FeedbackService.FeedbackItem> result = service.getFeedbackByUserId(1L);

        assertSame(mockResult, result);
        verify(jdbcTemplate).query(
                eq("SELECT id, user_id, session_id, question, ai_answer, rating, comment, created_at FROM feedback WHERE user_id = ? ORDER BY created_at DESC"),
                any(RowMapper.class),
                eq(1L)
        );
    }

    // ---- getAllFeedback ----

    @Test
    @DisplayName("getAllFeedback 应该查询全部反馈并按时间倒序")
    void getAllFeedbackShouldQueryAll() {
        List<FeedbackService.FeedbackItem> mockResult = List.of();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(mockResult);

        List<FeedbackService.FeedbackItem> result = service.getAllFeedback();

        assertSame(mockResult, result);
        verify(jdbcTemplate).query(
                eq("SELECT id, user_id, session_id, question, ai_answer, rating, comment, created_at FROM feedback ORDER BY created_at DESC"),
                any(RowMapper.class)
        );
    }

    // ---- getAverageRating ----

    @Test
    @DisplayName("getAverageRating 无数据时返回 0.0")
    void getAverageRatingEmpty() {
        when(jdbcTemplate.queryForObject(eq("SELECT AVG(rating) FROM feedback"), eq(Double.class)))
                .thenReturn(null);

        assertEquals(0.0, service.getAverageRating());
    }

    @Test
    @DisplayName("getAverageRating 有数据时返回平均值")
    void getAverageRatingWithData() {
        when(jdbcTemplate.queryForObject(eq("SELECT AVG(rating) FROM feedback"), eq(Double.class)))
                .thenReturn(4.5);

        assertEquals(4.5, service.getAverageRating());
    }

    // ---- getStats ----

    @Test
    @DisplayName("getStats 应该返回完整统计：总数、平均分、好评、差评")
    void getStatsShouldReturnAllFields() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM feedback"), eq(Integer.class)))
                .thenReturn(10);
        when(jdbcTemplate.queryForObject(eq("SELECT AVG(rating) FROM feedback"), eq(Double.class)))
                .thenReturn(4.2);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM feedback WHERE rating >= 4"), eq(Integer.class)))
                .thenReturn(7);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM feedback WHERE rating <= 2"), eq(Integer.class)))
                .thenReturn(1);

        Map<String, Object> stats = service.getStats();

        assertEquals(10, stats.get("totalFeedback"));
        assertEquals(4.2, stats.get("averageRating"));
        assertEquals(7, stats.get("goodFeedback"));
        assertEquals(1, stats.get("poorFeedback"));
    }

    @Test
    @DisplayName("getStats 无数据时全部返回零值")
    void getStatsWhenEmpty() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(null);
        when(jdbcTemplate.queryForObject(eq("SELECT AVG(rating) FROM feedback"), eq(Double.class)))
                .thenReturn(null);

        Map<String, Object> stats = service.getStats();

        assertEquals(0, stats.get("totalFeedback"));
        assertEquals(0.0, stats.get("averageRating"));
        assertEquals(0, stats.get("goodFeedback"));
        assertEquals(0, stats.get("poorFeedback"));
    }

    // ---- deleteFeedback ----

    @Test
    @DisplayName("deleteFeedback 应该执行 DELETE WHERE id = ?")
    void deleteFeedbackShouldDeleteById() {
        service.deleteFeedback(42L);
        verify(jdbcTemplate).update("DELETE FROM feedback WHERE id = ?", 42L);
    }

    // ---- batchDeleteFeedback ----

    @Test
    @DisplayName("batchDeleteFeedback 应该拼接 IN 子句删除多条")
    void batchDeleteFeedbackShouldDeleteMultiple() {
        service.batchDeleteFeedback(List.of(1L, 2L, 3L));
        verify(jdbcTemplate).update("DELETE FROM feedback WHERE id IN (1,2,3)");
    }

    @Test
    @DisplayName("batchDeleteFeedback 单个 ID 也能正常工作")
    void batchDeleteFeedbackSingle() {
        service.batchDeleteFeedback(List.of(99L));
        verify(jdbcTemplate).update("DELETE FROM feedback WHERE id IN (99)");
    }

    // ---- clearAll ----

    @Test
    @DisplayName("clearAll 应该执行 DELETE FROM feedback")
    void clearAllShouldDeleteAll() {
        service.clearAll();
        verify(jdbcTemplate).update("DELETE FROM feedback");
    }
}
