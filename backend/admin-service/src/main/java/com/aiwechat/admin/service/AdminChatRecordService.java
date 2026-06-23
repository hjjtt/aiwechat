package com.aiwechat.admin.service;

import com.aiwechat.admin.repository.ChatRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台聊天记录服务（直接实现类，非接口）
 */
@Service
@RequiredArgsConstructor
public class AdminChatRecordService {

    private final ChatRecordRepository chatRecordRepository;

    /**
     * 获取聊天记录列表（分页+筛选）
     *
     * @param userId  用户ID（可选）
     * @param keyword 搜索关键词（可选）
     * @param page    页码
     * @param size    每页数量
     * @return 分页数据
     */
    public Map<String, Object> getChatRecords(String userId, String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<?> records = chatRecordRepository.findAll(userId, keyword, offset, size);
        int total = chatRecordRepository.countRecords(userId, keyword);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        return result;
    }

    /**
     * 获取聊天记录统计数据
     *
     * @return 统计信息
     */
    public Map<String, Object> getStats() {
        int total = chatRecordRepository.countRecords(null, null);
        List<String> userIds = chatRecordRepository.findAllUserIds();

        Map<String, Object> result = new HashMap<>();
        result.put("totalRecords", total);
        result.put("totalUsers", userIds.size());
        return result;
    }

    /**
     * 根据ID删除聊天记录
     *
     * @param id 记录ID
     * @return 是否删除成功
     */
    public boolean deleteById(Long id) {
        return chatRecordRepository.deleteById(id) > 0;
    }

    /**
     * 批量删除聊天记录
     *
     * @param ids 要删除的记录ID列表
     * @return 删除的记录数
     */
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return chatRecordRepository.batchDelete(ids);
    }

    /**
     * 清空所有聊天记录
     *
     * @return 删除的记录数
     */
    public int clearAll() {
        return chatRecordRepository.clearAll();
    }

    /**
     * 按天数清理聊天记录
     *
     * @param天数 保留最近几天的记录
     * @return 删除的记录数
     */
    public int cleanByDays(int days) {
        return chatRecordRepository.deleteBeforeDays(days);
    }

    /**
     * 获取所有存在聊天记录的用户ID
     *
     * @return 用户ID列表
     */
    public List<String> getAllUserIds() {
        return chatRecordRepository.findAllUserIds();
    }
}
