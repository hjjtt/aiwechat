package com.aiwechat.aichat.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 意图识别结果 DTO
 */
@Data
public class IntentResult {

    /**
     * 意图列表（可多意图）
     * 如：menu_inquiry, order_inquiry, delivery_inquiry, complaint, refund, general
     */
    private List<String> intents = new ArrayList<>();

    /**
     * 槽位字典（提取的关键信息）
     * 如：order_id, menu_name, address, phone 等
     */
    private Map<String, String> slots = new HashMap<>();

    /**
     * 置信度 0~1
     */
    private double confidence;

    /**
     * 主意图（intents 中的第一个）
     */
    public String getPrimaryIntent() {
        return intents != null && !intents.isEmpty() ? intents.get(0) : "general";
    }

    /**
     * 是否包含某个意图
     */
    public boolean hasIntent(String intent) {
        return intents != null && intents.contains(intent);
    }

    /**
     * 获取槽位值
     */
    public String getSlot(String key) {
        return slots != null ? slots.get(key) : null;
    }

    /**
     * 是否存在某个槽位且非空
     */
    public boolean hasSlot(String key) {
        return slots != null && slots.containsKey(key) && slots.get(key) != null && !slots.get(key).isEmpty();
    }
}
