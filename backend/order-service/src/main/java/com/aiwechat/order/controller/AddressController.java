package com.aiwechat.order.controller;

import com.aiwechat.common.model.dto.ApiResponse;
import com.aiwechat.common.util.UserContextHelper;
import com.aiwechat.order.model.entity.UserAddress;
import com.aiwechat.order.service.UserAddressService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final UserAddressService userAddressService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<UserAddress>>> getUserAddresses(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        Long authenticatedUserId = UserContextHelper.requireUserId(httpRequest);
        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权查看他人地址"));
        }
        return ResponseEntity.ok(ApiResponse.success(userAddressService.getUserAddresses(userId)));
    }

    @GetMapping("/user/{userId}/default")
    public ResponseEntity<ApiResponse<UserAddress>> getDefaultAddress(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        Long authenticatedUserId = UserContextHelper.requireUserId(httpRequest);
        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权查看他人地址"));
        }
        return ResponseEntity.ok(ApiResponse.success(userAddressService.getDefaultAddress(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserAddress>> getAddress(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = UserContextHelper.requireUserId(httpRequest);
        UserAddress address = userAddressService.getAddressById(id);
        if (address == null) {
            return ResponseEntity.notFound().build();
        }
        if (!address.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权访问该地址"));
        }
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserAddress>> addAddress(
            @RequestBody UserAddress address,
            HttpServletRequest httpRequest) {
        Long userId = UserContextHelper.requireUserId(httpRequest);
        address.setUserId(userId);
        log.info("新增地址: userId={}", userId);
        return ResponseEntity.ok(ApiResponse.success(userAddressService.addAddress(address), "地址添加成功"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserAddress>> updateAddress(
            @PathVariable Long id,
            @RequestBody UserAddress address,
            HttpServletRequest httpRequest) {
        Long userId = UserContextHelper.requireUserId(httpRequest);

        UserAddress existing = userAddressService.getAddressById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        if (!existing.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权修改该地址"));
        }

        address.setUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(userAddressService.updateAddress(id, address), "地址更新成功"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = UserContextHelper.requireUserId(httpRequest);

        UserAddress existing = userAddressService.getAddressById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        if (!existing.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权删除该地址"));
        }

        userAddressService.deleteAddress(id);
        return ResponseEntity.ok(ApiResponse.success(null, "地址删除成功"));
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Void>> setDefaultAddress(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = UserContextHelper.requireUserId(httpRequest);
        userAddressService.setDefaultAddress(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "默认地址设置成功"));
    }
}
