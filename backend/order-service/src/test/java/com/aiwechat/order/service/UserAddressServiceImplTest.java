package com.aiwechat.order.service;

import com.aiwechat.common.exception.BusinessException;
import com.aiwechat.order.model.entity.UserAddress;
import com.aiwechat.order.repository.UserAddressRepository;
import com.aiwechat.order.service.impl.UserAddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceImplTest {

    @Mock
    private UserAddressRepository repository;

    private UserAddressServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserAddressServiceImpl(repository);
    }

    @Test
    @DisplayName("updateAddress 应该强制保持原地址归属，忽略客户端传入的 userId")
    void updateAddressShouldPreserveOwnership() {
        Long originalUserId = 14L;
        Long attackerUserId = 99L;
        Long addressId = 1L;

        UserAddress existing = new UserAddress();
        existing.setId(addressId);
        existing.setUserId(originalUserId);
        existing.setContactName("张三");
        existing.setContactPhone("13800138000");
        existing.setAddress("北京市朝阳区xxx");
        existing.setIsDefault(true);
        existing.setCreatedAt(LocalDateTime.now());

        when(repository.findById(addressId)).thenReturn(existing);
        when(repository.update(any(UserAddress.class))).thenReturn(1);
        when(repository.findById(addressId)).thenReturn(existing);

        UserAddress updateRequest = new UserAddress();
        updateRequest.setUserId(attackerUserId);
        updateRequest.setContactName("张三");
        updateRequest.setContactPhone("13800138000");
        updateRequest.setAddress("北京市朝阳区yyy");
        updateRequest.setIsDefault(true);

        service.updateAddress(addressId, updateRequest);

        // userId 应该被强制恢复为原归属者
        assertEquals(originalUserId, updateRequest.getUserId(),
                "userId 应该保持为原地址所有者，不能被篡改");
    }

    @Test
    @DisplayName("setDefaultAddress 应该拒绝非归属用户操作")
    void setDefaultAddressShouldRejectNonOwner() {
        Long ownerUserId = 14L;
        Long otherUserId = 99L;

        UserAddress address = new UserAddress();
        address.setId(1L);
        address.setUserId(ownerUserId);

        when(repository.findById(1L)).thenReturn(address);

        assertThrows(BusinessException.class,
                () -> service.setDefaultAddress(1L, otherUserId),
                "非归属用户不应该能设置默认地址");
    }

    @Test
    @DisplayName("addAddress 在 userId 为空时应该抛异常")
    void addAddressShouldRejectNullUserId() {
        UserAddress address = new UserAddress();
        address.setContactName("张三");
        address.setContactPhone("13800138000");
        address.setAddress("北京市朝阳区xxx");

        assertThrows(BusinessException.class,
                () -> service.addAddress(address),
                "userId 为空时应该拒绝");
    }
}
