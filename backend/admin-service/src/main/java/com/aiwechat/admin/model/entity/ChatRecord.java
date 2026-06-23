package com.aiwechat.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天记录实体类，对应 chat_records 表
 */
@Data
@TableName("chat_records")
public class ChatRecord {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private String userId;

    /** 会话ID */
    private String sessionId;

    /** 用户提问内容 */
    private String question;

    /** AI回答内容 */
    private String answer;

    /** 知识库来源 */
    private String sources;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 角色：user/assistant */
    private String role;
}
