package com.aiwechat.admin.repository;

import com.aiwechat.admin.model.entity.ChatRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天记录数据访问层，继承 MyBatis Plus BaseMapper
 */
@Mapper
public interface ChatRecordRepository extends BaseMapper<ChatRecord> {

    /**
     * 新增聊天记录
     */
    int insert(ChatRecord chatRecord);

    /**
     * 根据用户ID查询聊天记录
     */
    List<ChatRecord> findByUserId(@Param("userId") String userId);

    /**
     * 分页查询聊天记录（支持用户ID和关键词筛选）
     */
    List<ChatRecord> findAll(@Param("userId") String userId,
                              @Param("keyword") String keyword,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    /**
     * 统计聊天记录数量（支持用户ID和关键词筛选）
     */
    int countRecords(@Param("userId") String userId, @Param("keyword") String keyword);

    /**
     * 根据ID查询聊天记录
     */
    ChatRecord findById(@Param("id") Long id);

    /**
     * 根据ID删除聊天记录
     */
    int deleteById(@Param("id") Long id);

    /**
     * 批量删除聊天记录
     */
    int batchDelete(@Param("ids") List<Long> ids);

    /**
     * 清空所有聊天记录
     */
    int clearAll();

    /**
     * 删除指定天数之前的聊天记录
     */
    int deleteBeforeDays(@Param("days") int days);

    /**
     * 查询所有存在聊天记录的用户ID
     */
    List<String> findAllUserIds();
}
