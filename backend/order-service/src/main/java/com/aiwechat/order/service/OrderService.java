package com.aiwechat.order.service;

import com.aiwechat.order.model.dto.CreateOrderRequest;
import com.aiwechat.order.model.entity.Order;
import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单
     */
    Order createOrder(CreateOrderRequest request);

    /**
     * 根据ID获取订单
     */
    Order getOrderById(Long id);

    /**
     * 根据订单号获取订单
     */
    Order getOrderByNumber(String orderNumber);

    /**
     * 获取用户的订单列表
     */
    List<Order> getUserOrders(Long userId);

    /**
     * 获取用户某状态的订单
     */
    List<Order> getUserOrdersByStatus(Long userId, String status);

    /**
     * 更新订单状态
     */
    void updateOrderStatus(String orderNumber, String status);

    /**
     * 取消订单
     */
    void cancelOrder(String orderNumber, Long userId);
}
