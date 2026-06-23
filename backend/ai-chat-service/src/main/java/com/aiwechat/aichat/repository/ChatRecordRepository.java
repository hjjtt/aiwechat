package com.aiwechat.aichat.repository;

import com.aiwechat.aichat.model.entity.ChatRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 对话记录数据访问接口
 */
@Mapper
public interface ChatRecordRepository extends BaseMapper<ChatRecord> {

    /**
     * 保存对话记录
     *
     * @param record 对话记录
     * @return 影响行数
     */
    int insert(ChatRecord record);

    /**
     * 查询用户的对话历史
     *
     * @param userId 用户ID
     * @return 对话记录列表
     */
    List<ChatRecord> findByUserId(@Param("userId") String userId);

    /**
     * 查询用户某会话的对话历史
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 对话记录列表
     */
    List<ChatRecord> findByUserIdAndSessionId(
            @Param("userId") String userId,
            @Param("sessionId") String sessionId);

    /**
     * 查询最近的对话记录（用于上下文记忆）
     *
     * @param userId 用户ID
     * @param limit  返回条数
     * @return 最近的对话记录列表
     */
    List<ChatRecord> findRecentByUserId(
            @Param("userId") String userId,
            @Param("limit") int limit);
}
