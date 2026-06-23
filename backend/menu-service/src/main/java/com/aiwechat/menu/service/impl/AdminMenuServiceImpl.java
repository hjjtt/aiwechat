package com.aiwechat.menu.service.impl;

import com.aiwechat.menu.model.entity.MenuItem;
import com.aiwechat.menu.repository.MenuItemRepository;
import com.aiwechat.menu.service.AdminMenuService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜品管理服务实现类（后台管理）
 */
@Service
@RequiredArgsConstructor
public class AdminMenuServiceImpl implements AdminMenuService {

    private final MenuItemRepository menuItemRepository;

    /**
     * 分页获取菜品列表（支持分类和关键词筛选）
     */
    @Override
    public Map<String, Object> getItems(String category, String keyword, int page, int size) {
        // 计算偏移量
        int offset = (page - 1) * size;
        // 查询菜品列表
        List<MenuItem> records = menuItemRepository.findAllWithFilter(category, keyword, offset, size);
        // 查询总数
        int total = menuItemRepository.count(category, keyword);
        // 封装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 获取菜品统计数据
     */
    @Override
    public Map<String, Object> getStats() {
        int total = menuItemRepository.countTotal();
        int available = menuItemRepository.countAvailable();
        int categoryCount = menuItemRepository.countCategories();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("available", available);
        stats.put("categoryCount", categoryCount);
        return stats;
    }

    /**
     * 获取所有分类
     */
    @Override
    public List<String> getAllCategories() {
        return menuItemRepository.findAllCategories();
    }

    /**
     * 根据主键ID获取菜品详情
     */
    @Override
    public MenuItem getItemById(Long id) {
        return menuItemRepository.findById(id);
    }

    /**
     * 新增菜品
     */
    @Override
    public void addItem(MenuItem item) {
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        menuItemRepository.insert(item);
    }

    /**
     * 更新菜品
     */
    @Override
    public void updateItem(MenuItem item) {
        item.setUpdatedAt(LocalDateTime.now());
        menuItemRepository.update(item);
    }

    /**
     * 删除菜品
     */
    @Override
    public void deleteItem(Long id) {
        menuItemRepository.deleteById(id);
    }

    /**
     * 更新菜品上架状态
     */
    @Override
    public void updateAvailability(Long id, boolean available) {
        menuItemRepository.updateAvailabilityById(id, available ? 1 : 0);
    }

    /**
     * 批量更新菜品上架状态
     */
    @Override
    public int batchUpdateAvailability(List<Long> ids, boolean available) {
        int count = 0;
        for (Long id : ids) {
            menuItemRepository.updateAvailabilityById(id, available ? 1 : 0);
            count++;
        }
        return count;
    }
}
