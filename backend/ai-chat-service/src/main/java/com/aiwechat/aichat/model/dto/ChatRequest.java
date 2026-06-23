package com.aiwechat.aichat.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 聊天请求 DTO
 */
@Data
public class ChatRequest {

    /**
     * 用户ID（必填）
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 用户问题（必填）
     */
    @NotBlank(message = "问题内容不能为空")
    private String question;

    /**
     * 会话ID（可选，为空时自动生成）
     */
    private String sessionId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 订单总数
     */
    private Integer orderCount;

    /**
     * 待付款订单数
     */
    private Integer pendingOrderCount;

    /**
     * 配送中订单数
     */
    private Integer deliveringOrderCount;

    /**
     * 已完成订单数
     */
    private Integer completedOrderCount;

    /**
     * 默认收货地址
     */
    private String defaultAddress;

    /**
     * 图片URL列表（用于多模态识别）
     */
    private List<String> images;
}
