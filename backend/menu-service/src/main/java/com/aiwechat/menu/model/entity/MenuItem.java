package com.aiwechat.menu.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品实体类
 */
@Data
@TableName("menu_items")
public class MenuItem {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 菜品编号 */
    private String itemId;
    /** 菜品名称 */
    private String name;
    /** 菜品描述 */
    private String description;
    /** 菜品价格 */
    private BigDecimal price;
    /** 菜品分类 */
    private String category;
    /** 菜品图片地址 */
    private String imageUrl;
    /** 是否上架（1上架 0下架） */
    private Integer isAvailable;
    /** 销售数量 */
    private Integer salesCount;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
