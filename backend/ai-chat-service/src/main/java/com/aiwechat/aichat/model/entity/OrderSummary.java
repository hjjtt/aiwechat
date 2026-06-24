package com.aiwechat.aichat.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class OrderSummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNumber;

    private Long userId;

    private String status;

    private BigDecimal totalAmount;

    private String contactName;

    private String contactPhone;

    private String deliveryAddress;

    private LocalDateTime createdAt;
}
