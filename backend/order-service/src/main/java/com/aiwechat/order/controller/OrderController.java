package com.aiwechat.order.controller;

import com.aiwechat.common.model.dto.ApiResponse;
import com.aiwechat.common.util.UserContextHelper;
import com.aiwechat.order.model.dto.CreateOrderRequest;
import com.aiwechat.order.model.entity.Order;
import com.aiwechat.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrder(
            @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        Long userId = UserContextHelper.requireUserId(httpRequest);
        request.setUserId(userId);
        log.info("收到创建订单请求 - userId: {}", userId);

        Order order = orderService.createOrder(request);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNumber", order.getOrderNumber());
        result.put("status", order.getStatus());
        result.put("totalAmount", order.getTotalAmount());
        result.put("createdAt", order.getCreatedAt());

        return ResponseEntity.ok(ApiResponse.success(result, "订单创建成功"));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrderById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = UserContextHelper.requireUserId(httpRequest);
        log.info("获取订单详情 - id: {}, userId: {}", id, userId);

        Order order = orderService.getOrderById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<Order>> getOrderByNumber(
            @PathVariable String orderNumber,
            HttpServletRequest httpRequest) {
        Long userId = UserContextHelper.requireUserId(httpRequest);
        log.info("获取订单详情 - orderNumber: {}, userId: {}", orderNumber, userId);

        Order order = orderService.getOrderByNumber(orderNumber);
        if (order == null || !order.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Order>>> getUserOrders(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        Long authenticatedUserId = UserContextHelper.requireUserId(httpRequest);
        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权查看他人订单"));
        }

        List<Order> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<ApiResponse<List<Order>>> getUserOrdersByStatus(
            @PathVariable Long userId,
            @PathVariable String status,
            HttpServletRequest httpRequest) {
        Long authenticatedUserId = UserContextHelper.requireUserId(httpRequest);
        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权查看他人订单"));
        }

        List<Order> orders = orderService.getUserOrdersByStatus(userId, status);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PostMapping("/{orderNumber}/status")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable String orderNumber,
            @RequestParam String status) {
        log.warn("订单状态变更请求被拒绝 - 请使用管理端接口: orderNumber={}", orderNumber);
        return ResponseEntity.status(403).body(ApiResponse.error("请通过管理端修改订单状态"));
    }

    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable String orderNumber,
            HttpServletRequest httpRequest) {
        Long userId = UserContextHelper.requireUserId(httpRequest);
        log.info("取消订单 - orderNumber: {}, userId: {}", orderNumber, userId);

        orderService.cancelOrder(orderNumber, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "订单已取消"));
    }
}
