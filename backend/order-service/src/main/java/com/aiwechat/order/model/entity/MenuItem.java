package com.aiwechat.order.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 菜品实体类（仅保留订单服务需要的字段）
 */
@Data
@TableName("menu_items")
public class MenuItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 菜品名称
     */
    private String name;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 是否上架：1-上架，0-下架
     */
    private Integer isAvailable;
}
