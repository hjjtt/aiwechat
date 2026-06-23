package com.aiwechat.menu.service.impl;

import com.aiwechat.menu.model.entity.MenuItem;
import com.aiwechat.menu.repository.MenuItemRepository;
import com.aiwechat.menu.service.MenuService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

/**
 * 菜品服务实现类（前台）
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuItemRepository menuItemRepository;

    /**
     * 获取所有上架菜品
     */
    @Override
    public List<MenuItem> getAllAvailableItems() {
        return menuItemRepository.findAllAvailable();
    }

    /**
     * 根据分类获取菜品
     */
    @Override
    public List<MenuItem> getItemsByCategory(String category) {
        return menuItemRepository.findByCategory(category);
    }

    /**
     * 搜索菜品
     */
    @Override
    public List<MenuItem> searchItems(String keyword) {
        return menuItemRepository.search(keyword);
    }

    /**
     * 根据菜品编号获取菜品详情
     */
    @Override
    public MenuItem getItemById(String itemId) {
        return menuItemRepository.findByItemId(itemId);
    }
}
