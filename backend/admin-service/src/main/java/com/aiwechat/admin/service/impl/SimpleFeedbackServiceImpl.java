package com.aiwechat.admin.service.impl;

import com.aiwechat.admin.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleFeedbackServiceImpl implements FeedbackService {

    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void submitFeedback(Long userId, String sessionId, String question, String aiAnswer, int rating, String comment) {
        jdbcTemplate.update(
                "INSERT INTO feedback (user_id, session_id, question, ai_answer, rating, comment, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                userId, sessionId, question, aiAnswer, rating, comment, LocalDateTime.now());
        log.info("用户 {} 提交反馈，评分: {}", userId, rating);
    }

    @Override
    public List<FeedbackItem> getFeedbackByUserId(Long userId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, session_id, question, ai_answer, rating, comment, created_at FROM feedback WHERE user_id = ? ORDER BY created_at DESC",
                (rs, rowNum) -> new FeedbackItem(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("session_id"),
                        rs.getString("question"),
                        rs.getString("ai_answer"),
                        rs.getInt("rating"),
                        rs.getString("comment"),
                        rs.getTimestamp("created_at").toLocalDateTime().format(FMT)
                ), userId);
    }

    @Override
    public List<FeedbackItem> getAllFeedback() {
        return jdbcTemplate.query(
                "SELECT id, user_id, session_id, question, ai_answer, rating, comment, created_at FROM feedback ORDER BY created_at DESC",
                (rs, rowNum) -> new FeedbackItem(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("session_id"),
                        rs.getString("question"),
                        rs.getString("ai_answer"),
                        rs.getInt("rating"),
                        rs.getString("comment"),
                        rs.getTimestamp("created_at").toLocalDateTime().format(FMT)
                ));
    }

    @Override
    public double getAverageRating() {
        Double avg = jdbcTemplate.queryForObject("SELECT AVG(rating) FROM feedback", Double.class);
        return avg != null ? avg : 0.0;
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM feedback", Integer.class);
        stats.put("totalFeedback", total != null ? total : 0);
        stats.put("averageRating", getAverageRating());

        Integer good = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM feedback WHERE rating >= 4", Integer.class);
        stats.put("goodFeedback", good != null ? good : 0);

        Integer poor = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM feedback WHERE rating <= 2", Integer.class);
        stats.put("poorFeedback", poor != null ? poor : 0);

        return stats;
    }

    @Override
    public void deleteFeedback(Long id) {
        jdbcTemplate.update("DELETE FROM feedback WHERE id = ?", id);
    }

    @Override
    public void batchDeleteFeedback(List<Long> ids) {
        jdbcTemplate.update("DELETE FROM feedback WHERE id IN (" +
                ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("0") + ")");
    }

    @Override
    public void clearAll() {
        jdbcTemplate.update("DELETE FROM feedback");
    }
}
