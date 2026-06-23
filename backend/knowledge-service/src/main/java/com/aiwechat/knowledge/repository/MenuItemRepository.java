package com.aiwechat.knowledge.repository;

import com.aiwechat.knowledge.model.entity.MenuItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 菜品数据访问层
 */
@Mapper
public interface MenuItemRepository extends BaseMapper<MenuItem> {

    /**
     * 查询所有上架菜品
     * @return 上架菜品列表
     */
    List<MenuItem> findAllAvailable();
}
