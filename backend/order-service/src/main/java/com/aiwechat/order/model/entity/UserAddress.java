package com.aiwechat.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_addresses")
public class UserAddress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String contactName;

    private String contactPhone;

    private String province;

    private String city;

    private String district;

    private String address;

    private String detailAddress;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Boolean isDefault;

    private String label;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
