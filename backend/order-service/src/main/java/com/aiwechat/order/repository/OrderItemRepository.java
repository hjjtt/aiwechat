package com.aiwechat.order.repository;

import com.aiwechat.order.model.entity.OrderItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 订单明细数据访问接口
 */
@Mapper
public interface OrderItemRepository extends BaseMapper<OrderItem> {

    /**
     * 根据订单ID查询明细列表
     */
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    /**
     * 插入订单明细
     */
    int insert(OrderItem orderItem);

    /**
     * 批量插入订单明细
     */
    int batchInsert(@Param("items") List<OrderItem> items);

    /**
     * 删除订单明细
     */
    int deleteByOrderId(@Param("orderId") Long orderId);
}
