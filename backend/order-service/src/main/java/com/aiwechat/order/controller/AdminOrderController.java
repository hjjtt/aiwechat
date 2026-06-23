package com.aiwechat.order.controller;

import com.aiwechat.common.model.dto.ApiResponse;
import com.aiwechat.order.model.entity.Order;
import com.aiwechat.order.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台订单管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /**
     * 分页查询所有订单
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> data = adminOrderService.getOrders(status, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 获取订单统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = adminOrderService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrderById(@PathVariable Long id) {
        Order order = adminOrderService.getOrderById(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * 更新订单状态
     */
    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        adminOrderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(null, "状态更新成功"));
    }

    /**
     * 批量更新订单状态
     */
    @PostMapping("/batch-status")
    public ResponseEntity<ApiResponse<Integer>> batchUpdateStatus(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) request.get("ids");
        String status = (String) request.get("status");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("请选择要更新的订单"));
        }
        // 转换为 Long 类型
        List<Long> idList = ids.stream()
                .map(Number::longValue)
                .toList();
        int count = adminOrderService.batchUpdateStatus(idList, status);
        return ResponseEntity.ok(ApiResponse.success(count, "成功更新 " + count + " 条订单状态"));
    }

    /**
     * 获取所有订单状态
     */
    @GetMapping("/statuses")
    public ResponseEntity<ApiResponse<List<String>>> getAllStatuses() {
        List<String> statuses = adminOrderService.getAllStatuses();
        return ResponseEntity.ok(ApiResponse.success(statuses));
    }
}
