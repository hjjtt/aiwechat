package com.aiwechat.menu.service;

import com.aiwechat.menu.model.entity.MenuItem;
import java.util.List;
import java.util.Map;

/**
 * 菜品管理服务接口（后台管理）
 */
public interface AdminMenuService {

    /**
     * 分页获取菜品列表（支持分类和关键词筛选）
     */
    Map<String, Object> getItems(String category, String keyword, int page, int size);

    /**
     * 获取菜品统计数据
     */
    Map<String, Object> getStats();

    /**
     * 获取所有分类
     */
    List<String> getAllCategories();

    /**
     * 根据主键ID获取菜品详情
     */
    MenuItem getItemById(Long id);

    /**
     * 新增菜品
     */
    void addItem(MenuItem item);

    /**
     * 更新菜品
     */
    void updateItem(MenuItem item);

    /**
     * 删除菜品
     */
    void deleteItem(Long id);

    /**
     * 更新菜品上架状态
     */
    void updateAvailability(Long id, boolean available);

    /**
     * 批量更新菜品上架状态
     */
    int batchUpdateAvailability(List<Long> ids, boolean available);
}
