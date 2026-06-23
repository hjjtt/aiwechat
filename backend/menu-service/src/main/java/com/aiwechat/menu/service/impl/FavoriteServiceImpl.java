package com.aiwechat.menu.service.impl;

import com.aiwechat.menu.model.dto.FavoriteItemDTO;
import com.aiwechat.menu.model.entity.MenuItem;
import com.aiwechat.menu.model.entity.UserFavorite;
import com.aiwechat.menu.repository.MenuItemRepository;
import com.aiwechat.menu.repository.UserFavoriteRepository;
import com.aiwechat.menu.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final UserFavoriteRepository userFavoriteRepository;
    private final MenuItemRepository menuItemRepository;

    @Override
    public List<FavoriteItemDTO> getFavorites(Long userId) {
        List<UserFavorite> favorites = userFavoriteRepository.findByUserId(userId);
        if (favorites.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> menuIds = favorites.stream()
                .map(UserFavorite::getMenuId)
                .collect(Collectors.toList());

        List<MenuItem> menuItems = menuItemRepository.selectBatchIds(menuIds);
        Map<Long, MenuItem> menuItemMap = menuItems.stream()
                .collect(Collectors.toMap(MenuItem::getId, item -> item));

        List<FavoriteItemDTO> result = new ArrayList<>();
        for (UserFavorite favorite : favorites) {
            MenuItem menuItem = menuItemMap.get(favorite.getMenuId());
            if (menuItem != null) {
                FavoriteItemDTO dto = new FavoriteItemDTO();
                dto.setId(favorite.getId());
                dto.setUserId(favorite.getUserId());
                dto.setMenuId(favorite.getMenuId());
                dto.setMenuName(menuItem.getName());
                dto.setMenuDescription(menuItem.getDescription());
                dto.setMenuPrice(menuItem.getPrice());
                dto.setMenuCategory(menuItem.getCategory());
                dto.setMenuImageUrl(menuItem.getImageUrl());
                dto.setMenuSalesCount(menuItem.getSalesCount());
                dto.setIsFavorite(true);
                dto.setQuantity(0);
                dto.setPriceFormatted(formatPrice(menuItem.getPrice()));
                result.add(dto);
            }
        }
        return result;
    }

    @Override
    public boolean checkFavorite(Long userId, Long menuId) {
        UserFavorite favorite = userFavoriteRepository.checkFavorite(userId, menuId);
        return favorite != null;
    }

    @Override
    public boolean addFavorite(Long userId, Long menuId) {
        UserFavorite existing = userFavoriteRepository.checkFavorite(userId, menuId);
        if (existing != null) {
            return false;
        }

        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setMenuId(menuId);
        favorite.setCreatedAt(LocalDateTime.now());

        return userFavoriteRepository.insert(favorite) > 0;
    }

    @Override
    public boolean removeFavorite(Long userId, Long menuId) {
        UserFavorite favorite = userFavoriteRepository.checkFavorite(userId, menuId);
        if (favorite == null) {
            return false;
        }

        return userFavoriteRepository.deleteById(favorite.getId()) > 0;
    }

    @Override
    public Map<String, Object> toggleFavorite(Long userId, Long menuId) {
        Map<String, Object> result = new HashMap<>();

        UserFavorite existing = userFavoriteRepository.checkFavorite(userId, menuId);
        if (existing != null) {
            userFavoriteRepository.deleteById(existing.getId());
            result.put("isFavorite", false);
            result.put("message", "已取消收藏");
        } else {
            UserFavorite favorite = new UserFavorite();
            favorite.setUserId(userId);
            favorite.setMenuId(menuId);
            favorite.setCreatedAt(LocalDateTime.now());
            userFavoriteRepository.insert(favorite);
            result.put("isFavorite", true);
            result.put("message", "已添加收藏");
        }

        return result;
    }

    @Override
    public int getCount(Long userId) {
        return userFavoriteRepository.countByUserId(userId);
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "0.00";
        }
        return price.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }
}
