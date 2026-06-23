package com.aiwechat.order.repository;

import com.aiwechat.order.model.entity.MenuItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Collection;
import java.util.List;

/**
 * 菜品数据访问接口（订单服务专用）
 */
@Mapper
public interface MenuItemRepository extends BaseMapper<MenuItem> {

    /**
     * 根据多个ID批量查询（用于订单创建时校验菜品）
     */
    List<MenuItem> findByIds(@Param("ids") Collection<Long> ids);
}
