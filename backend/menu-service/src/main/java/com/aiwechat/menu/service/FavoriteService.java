package com.aiwechat.menu.service;

import com.aiwechat.menu.model.dto.FavoriteItemDTO;

import java.util.List;
import java.util.Map;

public interface FavoriteService {

    List<FavoriteItemDTO> getFavorites(Long userId);

    boolean checkFavorite(Long userId, Long menuId);

    boolean addFavorite(Long userId, Long menuId);

    boolean removeFavorite(Long userId, Long menuId);

    Map<String, Object> toggleFavorite(Long userId, Long menuId);

    int getCount(Long userId);
}
