package com.aiwechat.order.service.impl;

import com.aiwechat.common.exception.BusinessException;
import com.aiwechat.order.model.dto.CreateOrderRequest;
import com.aiwechat.order.model.entity.MenuItem;
import com.aiwechat.order.model.entity.Order;
import com.aiwechat.order.model.entity.OrderItem;
import com.aiwechat.order.repository.MenuItemRepository;
import com.aiwechat.order.repository.OrderItemRepository;
import com.aiwechat.order.repository.OrderRepository;
import com.aiwechat.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;

    @Override
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        log.info("创建订单 - userId: {}", request.getUserId());

        // 生成订单号
        String orderNumber = generateOrderNumber();

        // 创建订单
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUserId(request.getUserId());
        order.setStatus("pending");

        // 从数据库查询真实价格计算订单金额，防止前端篡改
        BigDecimal calculatedTotal = calculateOrderTotalFromDb(request.getItems());
        order.setTotalAmount(calculatedTotal);

        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setContactName(request.getContactName());
        order.setContactPhone(request.getContactPhone());
        order.setRemark(request.getRemark());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // 保存订单
        orderRepository.insert(order);
        log.info("订单已保存 - orderId: {}, orderNumber: {}, totalAmount: {}",
                order.getId(), orderNumber, calculatedTotal);

        // 保存订单明细
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            saveOrderItems(order.getId(), request.getItems());
        }

        return order;
    }

    /**
     * 从数据库查询真实价格计算订单总金额，防止前端篡改价格
     */
    private BigDecimal calculateOrderTotalFromDb(List<CreateOrderRequest.CartItem> items) {
        if (items == null || items.isEmpty()) {
            log.warn("订单项为空，无法创建订单");
            throw new BusinessException("请选择要购买的菜品");
        }

        for (CreateOrderRequest.CartItem item : items) {
            log.debug("校验订单项: menuId={}, quantity={}", item.getMenuId(), item.getQuantity());
            if (item.getMenuId() == null) {
                log.warn("菜品ID为空: item={}", item);
                throw new BusinessException("菜品ID不能为空，请刷新页面后重试");
            }
            if (item.getQuantity() == null) {
                log.warn("菜品数量为空: menuId={}", item.getMenuId());
                throw new BusinessException("菜品数量不能为空");
            }
            if (item.getQuantity() <= 0) {
                log.warn("菜品数量无效: menuId={}, quantity={}", item.getMenuId(), item.getQuantity());
                throw new BusinessException("菜品数量必须大于 0");
            }
        }

        List<Long> menuIds = items.stream()
                .map(CreateOrderRequest.CartItem::getMenuId)
                .toList();

        log.debug("查询菜品信息: menuIds={}", menuIds);
        List<MenuItem> menuItems = menuItemRepository.findByIds(menuIds);
        Map<Long, MenuItem> menuMap = menuItems.stream()
                .collect(Collectors.toMap(MenuItem::getId, m -> m));

        log.debug("查询到菜品数量: {}, 请求数量: {}", menuMap.size(), menuIds.size());

        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.CartItem item : items) {
            MenuItem menuItem = menuMap.get(item.getMenuId());
            if (menuItem == null) {
                log.warn("菜品不存在: menuId={}", item.getMenuId());
                throw new BusinessException("菜品不存在或已下架，请刷新页面后重试");
            }
            if (menuItem.getIsAvailable() == null || menuItem.getIsAvailable() != 1) {
                log.warn("菜品已下架: menuId={}, name={}", item.getMenuId(), menuItem.getName());
                throw new BusinessException("菜品【" + menuItem.getName() + "】已下架，请重新选择");
            }

            item.setPrice(menuItem.getPrice());
            item.setName(menuItem.getName());
            total = total.add(menuItem.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        log.info("订单总金额计算完成: total={}", total);
        return total;
    }

    /**
     * 保存订单明细
     */
    private void saveOrderItems(Long orderId, List<CreateOrderRequest.CartItem> items) {
        List<OrderItem> orderItems = items.stream().map(item -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orderId);
            orderItem.setMenuId(item.getMenuId());
            orderItem.setMenuName(item.getName());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(item.getPrice());
            // 计算小计
            BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            orderItem.setSubtotal(subtotal);
            orderItem.setCreatedAt(LocalDateTime.now());
            return orderItem;
        }).collect(Collectors.toList());

        orderItemRepository.batchInsert(orderItems);
        log.info("订单明细已保存 - orderId: {}, itemCount: {}", orderId, orderItems.size());
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Override
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public List<Order> getUserOrdersByStatus(Long userId, String status) {
        return orderRepository.findByUserIdAndStatus(userId, status);
    }

    @Override
    @Transactional
    public void updateOrderStatus(String orderNumber, String status) {
        log.info("更新订单状态 - orderNumber: {}, status: {}", orderNumber, status);

        Order order = orderRepository.findByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在: " + orderNumber);
        }

        // 验证状态转换是否合法
        validateStatusTransition(order.getStatus(), status);

        orderRepository.updateStatus(orderNumber, status);
        log.info("订单状态已更新 - orderNumber: {}, newStatus: {}", orderNumber, status);
    }

    /**
     * 验证订单状态转换是否合法
     */
    private void validateStatusTransition(String currentStatus, String newStatus) {
        boolean valid = switch (currentStatus) {
            case "pending" -> newStatus.equals("confirmed") || newStatus.equals("cancelled");
            case "confirmed" -> newStatus.equals("preparing") || newStatus.equals("cancelled");
            case "preparing" -> newStatus.equals("delivering");
            case "delivering" -> newStatus.equals("completed");
            default -> false;
        };

        if (!valid) {
            throw new BusinessException("不允许的状态转换: " + currentStatus + " -> " + newStatus);
        }
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNumber, Long userId) {
        log.info("取消订单 - orderNumber: {}, userId: {}", orderNumber, userId);

        Order order = orderRepository.findByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在: " + orderNumber);
        }

        // 验证用户
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        // 只有待确认的订单可以取消
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不允许取消，当前状态: " + order.getStatus());
        }

        orderRepository.updateStatus(orderNumber, "cancelled");
        log.info("订单已取消 - orderNumber: {}", orderNumber);
    }

    /**
     * 生成订单号
     * 格式：ORD + 年月日 + 6位随机数
     */
    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD" + date + random;
    }
}
