package com.aiwechat.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品实体类
 * 对应数据库表 menu_items
 */
@Data
@TableName("menu_items")
public class MenuItem {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 菜品名称 */
    private String name;

    /** 菜品描述 */
    private String description;

    /** 价格 */
    private BigDecimal price;

    /** 分类：主食、热菜、素菜、汤类、饮品等 */
    private String category;

    /** 销量 */
    private Integer salesCount;

    /** 是否上架：1-上架，0-下架 */
    private Integer isAvailable;
}
