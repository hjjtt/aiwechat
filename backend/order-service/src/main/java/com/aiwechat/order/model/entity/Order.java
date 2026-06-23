package com.aiwechat.order.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单实体类
 */
@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号（业务唯一标识）
     */
    private String orderNumber;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单状态：pending-待处理, confirmed-已确认, preparing-准备中, delivering-配送中, completed-已完成, cancelled-已取消
     */
    private String status;

    /**
     * 订单金额
     */
    private BigDecimal totalAmount;

    /**
     * 配送地址
     */
    private String deliveryAddress;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 备注
     */
    private String remark;

    /**
     * 订单明细列表（不持久化，仅用于返回）
     */
    @TableField(exist = false)
    private List<OrderItem> items;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
