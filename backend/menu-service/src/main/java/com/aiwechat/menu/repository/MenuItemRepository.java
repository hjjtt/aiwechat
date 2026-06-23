package com.aiwechat.menu.repository;

import com.aiwechat.menu.model.entity.MenuItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Collection;
import java.util.List;

/**
 * 菜品数据访问接口
 */
@Mapper
public interface MenuItemRepository extends BaseMapper<MenuItem> {

    /**
     * 根据菜品编号查询
     */
    MenuItem findByItemId(@Param("itemId") String itemId);

    /**
     * 查询所有上架菜品
     */
    List<MenuItem> findAllAvailable();

    /**
     * 根据分类查询上架菜品
     */
    List<MenuItem> findByCategory(@Param("category") String category);

    /**
     * 搜索菜品（按名称或描述模糊匹配）
     */
    List<MenuItem> search(@Param("keyword") String keyword);

    /**
     * 新增菜品
     */
    int insert(MenuItem menuItem);

    /**
     * 更新菜品
     */
    int update(MenuItem menuItem);

    /**
     * 更新上架状态（按菜品编号）
     */
    int updateAvailability(
            @Param("itemId") String itemId,
            @Param("isAvailable") Integer isAvailable);

    /**
     * 根据主键ID查询
     */
    MenuItem findById(@Param("id") Long id);

    /**
     * 根据多个主键ID批量查询
     */
    List<MenuItem> findByIds(@Param("ids") Collection<Long> ids);

    // ========== 后台管理相关 ==========

    /**
     * 分页查询菜品（支持分类和关键词筛选）
     */
    List<MenuItem> findAllWithFilter(
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 统计菜品数量（支持分类和关键词筛选）
     */
    int count(@Param("category") String category, @Param("keyword") String keyword);

    /**
     * 统计总菜品数
     */
    int countTotal();

    /**
     * 统计上架菜品数
     */
    int countAvailable();

    /**
     * 统计分类数量
     */
    int countCategories();

    /**
     * 获取所有分类列表
     */
    List<String> findAllCategories();

    /**
     * 根据主键ID删除菜品
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据主键ID更新上架状态
     */
    int updateAvailabilityById(
            @Param("id") Long id,
            @Param("isAvailable") Integer isAvailable);
}
