package com.aiwechat.menu.model.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 收藏菜品 DTO
 */
@Data
public class FavoriteItemDTO {

    private Long id;

    private Long userId;

    private Long menuId;

    private String menuName;

    private String menuDescription;

    private BigDecimal menuPrice;

    private String menuCategory;

    private String menuImageUrl;

    private Integer menuSalesCount;

    private Boolean isFavorite;

    private Integer quantity;

    private String priceFormatted;
}
