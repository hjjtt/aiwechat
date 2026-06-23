package com.aiwechat.aichat.model.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 三层记忆上下文 DTO
 * 关键事实(Facts) + 长期摘要(Summary) + 近期会话(Recent)
 */
@Data
public class MemoryContext {

    /**
     * 第一层：关键事实（结构化键值对）
     * 如：order_id=xxx, address=xxx, phone=xxx
     */
    private Map<String, String> facts;

    /**
     * 第二层：长期摘要（早期对话的压缩总结）
     */
    private String summary;

    /**
     * 第三层：近期会话（最近N轮原始对话文本）
     */
    private String recentMessages;

    /**
     * 未摘要的消息条数
     */
    private int unsummarizedCount;

    /**
     * 拼接为完整的记忆文本，注入到 Prompt 中
     */
    public String toPromptText() {
        StringBuilder sb = new StringBuilder();

        // 第一层：关键事实
        if (facts != null && !facts.isEmpty()) {
            sb.append("【关键事实】\n");
            for (Map.Entry<String, String> entry : facts.entrySet()) {
                sb.append("- ").append(entry.getKey()).append("：").append(entry.getValue()).append("\n");
            }
        }

        // 第二层：长期摘要
        if (summary != null && !summary.isEmpty()) {
            sb.append("【历史摘要】\n").append(summary).append("\n");
        }

        // 第三层：近期会话
        if (recentMessages != null && !recentMessages.isEmpty()) {
            sb.append("【近期对话】\n").append(recentMessages).append("\n");
        }

        return sb.toString();
    }
}
