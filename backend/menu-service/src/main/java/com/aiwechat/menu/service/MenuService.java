package com.aiwechat.menu.service;

import com.aiwechat.menu.model.entity.MenuItem;
import java.util.List;

/**
 * 菜品服务接口（前台）
 */
public interface MenuService {

    /**
     * 获取所有上架菜品
     */
    List<MenuItem> getAllAvailableItems();

    /**
     * 根据分类获取菜品
     */
    List<MenuItem> getItemsByCategory(String category);

    /**
     * 搜索菜品
     */
    List<MenuItem> searchItems(String keyword);

    /**
     * 根据菜品编号获取菜品详情
     */
    MenuItem getItemById(String itemId);
}
