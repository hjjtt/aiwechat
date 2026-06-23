package com.aiwechat.aichat.model.dto;

import lombok.Data;
import java.util.List;

/**
 * 聊天响应 DTO
 */
@Data
public class ChatResponse {

    /**
     * AI回答内容
     */
    private String reply;

    /**
     * 知识来源列表
     */
    private List<String> sources;

    /**
     * 会话ID
     */
    private String sessionId;

    public ChatResponse() {
    }

    public ChatResponse(String reply, List<String> sources) {
        this.reply = reply;
        this.sources = sources;
    }

    public ChatResponse(String reply, List<String> sources, String sessionId) {
        this.reply = reply;
        this.sources = sources;
        this.sessionId = sessionId;
    }
}
