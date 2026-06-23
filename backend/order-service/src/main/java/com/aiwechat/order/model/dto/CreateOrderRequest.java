package com.aiwechat.order.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单创建请求 DTO
 */
@Data
public class CreateOrderRequest {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 配送地址
     */
    private String deliveryAddress;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 订单金额
     */
    private BigDecimal totalAmount;

    /**
     * 菜品列表
     */
    private List<CartItem> items;

    /**
     * 购物车项 DTO
     */
    @Data
    public static class CartItem {
        private Long menuId;
        private String name;
        private BigDecimal price;
        private Integer quantity;
    }
}
