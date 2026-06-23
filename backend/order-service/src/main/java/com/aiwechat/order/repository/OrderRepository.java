package com.aiwechat.order.repository;

import com.aiwechat.order.model.entity.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 订单数据访问接口
 */
@Mapper
public interface OrderRepository extends BaseMapper<Order> {

    /**
     * 根据ID查询
     */
    Order findById(@Param("id") Long id);

    /**
     * 根据订单号查询
     */
    Order findByOrderNumber(@Param("orderNumber") String orderNumber);

    /**
     * 查询用户的订单列表
     */
    List<Order> findByUserId(@Param("userId") Long userId);

    /**
     * 查询用户某状态的订单
     */
    List<Order> findByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") String status);

    /**
     * 保存订单
     */
    int insert(Order order);

    /**
     * 更新订单状态
     */
    int updateStatus(
            @Param("orderNumber") String orderNumber,
            @Param("status") String status);

    /**
     * 更新订单
     */
    int update(Order order);

    /**
     * 分页查询所有订单（支持状态和关键词筛选）
     */
    List<Order> findAllWithFilter(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 统计订单数量
     */
    int count(@Param("status") String status, @Param("keyword") String keyword);

    /**
     * 按状态统计订单数量
     */
    int countByStatus(@Param("status") String status);

    /**
     * 按日期统计订单数量
     */
    int countByDate(@Param("date") String date);

    /**
     * 统计当日订单金额
     */
    Double sumAmountByDate(@Param("date") String date);
}
