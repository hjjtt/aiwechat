package com.aiwechat.aichat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对话记录实体类
 */
@Data
@TableName("chat_records")
public class ChatRecord {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户问题
     */
    private String question;

    /**
     * AI回答
     */
    private String answer;

    /**
     * 知识来源（JSON格式存储）
     */
    private String sources;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 消息类型：user/assistant
     */
    private String role;
}
