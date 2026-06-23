package com.aiwechat.menu.repository;

import com.aiwechat.menu.model.entity.UserFavorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户收藏数据访问层
 */
@Mapper
public interface UserFavoriteRepository extends BaseMapper<UserFavorite> {

    /**
     * 根据用户 ID 查询收藏列表
     */
    List<UserFavorite> findByUserId(@Param("userId") Long userId);

    /**
     * 检查是否已收藏
     */
    UserFavorite checkFavorite(@Param("userId") Long userId, @Param("menuId") Long menuId);

    /**
     * 统计用户收藏数量
     */
    int countByUserId(@Param("userId") Long userId);
}
