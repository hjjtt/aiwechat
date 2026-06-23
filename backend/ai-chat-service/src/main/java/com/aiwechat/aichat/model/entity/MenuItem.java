package com.aiwechat.aichat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 菜品实体类（精简版，用于 AI 上下文查询）
 */
@Data
@TableName("menu_items")
public class MenuItem {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 菜品名称
     */
    private String name;

    /**
     * 菜品描述
     */
    private String description;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 分类：主食、热菜、素菜、汤类、饮品等
     */
    private String category;

    /**
     * 销量
     */
    private Integer salesCount;
}
