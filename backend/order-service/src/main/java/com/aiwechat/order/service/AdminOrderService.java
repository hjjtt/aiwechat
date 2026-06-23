package com.aiwechat.order.service;

import com.aiwechat.order.model.entity.Order;
import com.aiwechat.order.model.entity.OrderItem;
import com.aiwechat.order.repository.OrderItemRepository;
import com.aiwechat.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 后台订单管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 订单状态列表
     */
    private static final List<String> ORDER_STATUSES = Arrays.asList(
            "pending",    // 待处理
            "confirmed",  // 已确认
            "preparing",  // 准备中
            "delivering", // 配送中
            "completed",  // 已完成
            "cancelled"   // 已取消
    );

    private static final Map<String, String> STATUS_MAP = new HashMap<>();
    static {
        STATUS_MAP.put("pending", "待处理");
        STATUS_MAP.put("confirmed", "已确认");
        STATUS_MAP.put("preparing", "准备中");
        STATUS_MAP.put("delivering", "配送中");
        STATUS_MAP.put("completed", "已完成");
        STATUS_MAP.put("cancelled", "已取消");
    }

    /**
     * 分页查询订单
     */
    public Map<String, Object> getOrders(String status, String keyword, int page, int size) {
        int offset = (page - 1) * size;

        List<Order> orders = orderRepository.findAllWithFilter(
                status,
                keyword,
                offset,
                size
        );

        // 查询每个订单的商品明细
        for (Order order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            order.setItems(items);
        }

        int total = orderRepository.count(status, keyword);

        // 转换状态为中文
        List<Map<String, Object>> resultOrders = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNumber", order.getOrderNumber());
            map.put("userId", order.getUserId());
            map.put("status", order.getStatus());
            map.put("statusText", STATUS_MAP.getOrDefault(order.getStatus(), order.getStatus()));
            map.put("totalAmount", order.getTotalAmount());
            map.put("contactName", order.getContactName());
            map.put("contactPhone", order.getContactPhone());
            map.put("deliveryAddress", order.getDeliveryAddress());
            map.put("remark", order.getRemark());
            map.put("items", order.getItems());
            map.put("createdAt", order.getCreatedAt());
            map.put("updatedAt", order.getUpdatedAt());
            resultOrders.add(map);
        }

        return Map.of(
                "records", resultOrders,
                "total", total,
                "page", page,
                "size", size
        );
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStats() {
        int totalOrders = orderRepository.count(null, null);
        int pendingOrders = orderRepository.countByStatus("pending");
        int completedOrders = orderRepository.countByStatus("completed");
        int cancelledOrders = orderRepository.countByStatus("cancelled");

        // 计算今日订单数
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        int todayOrders = orderRepository.countByDate(today);

        // 计算今日收入
        Double todayRevenue = orderRepository.sumAmountByDate(today);
        todayRevenue = todayRevenue != null ? todayRevenue : 0.0;

        return Map.of(
                "totalOrders", totalOrders,
                "pendingOrders", pendingOrders,
                "completedOrders", completedOrders,
                "cancelledOrders", cancelledOrders,
                "todayOrders", todayOrders,
                "todayRevenue", todayRevenue
        );
    }

    /**
     * 获取订单详情
     */
    public Order getOrderById(Long id) {
        Order order = orderRepository.findById(id);
        if (order != null) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            order.setItems(items);
        }
        return order;
    }

    /**
     * 更新订单状态
     */
    public void updateOrderStatus(Long id, String status) {
        log.info("更新订单状态 - id: {}, status: {}", id, status);
        Order order = orderRepository.findById(id);
        if (order != null) {
            orderRepository.updateStatus(order.getOrderNumber(), status);
        }
    }

    /**
     * 批量更新订单状态
     */
    public int batchUpdateStatus(List<Long> ids, String status) {
        int count = 0;
        for (Long id : ids) {
            Order order = orderRepository.findById(id);
            if (order != null) {
                orderRepository.updateStatus(order.getOrderNumber(), status);
                count++;
            }
        }
        return count;
    }

    /**
     * 获取所有订单状态
     */
    public List<String> getAllStatuses() {
        return ORDER_STATUSES;
    }
}
