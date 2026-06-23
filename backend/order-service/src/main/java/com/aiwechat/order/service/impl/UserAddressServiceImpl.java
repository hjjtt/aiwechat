package com.aiwechat.order.service.impl;

import com.aiwechat.common.exception.BusinessException;
import com.aiwechat.order.model.entity.UserAddress;
import com.aiwechat.order.repository.UserAddressRepository;
import com.aiwechat.order.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressRepository userAddressRepository;

    @Override
    public List<UserAddress> getUserAddresses(Long userId) {
        return userAddressRepository.findByUserId(userId);
    }

    @Override
    public UserAddress getDefaultAddress(Long userId) {
        UserAddress address = userAddressRepository.findDefaultByUserId(userId);
        if (address != null) {
            return address;
        }

        List<UserAddress> addresses = userAddressRepository.findByUserId(userId);
        return addresses.isEmpty() ? null : addresses.get(0);
    }

    @Override
    public UserAddress getAddressById(Long id) {
        return userAddressRepository.findById(id);
    }

    @Override
    @Transactional
    public UserAddress addAddress(UserAddress address) {
        validateAddress(address);

        int addressCount = userAddressRepository.countByUserId(address.getUserId());
        boolean shouldBeDefault = Boolean.TRUE.equals(address.getIsDefault()) || addressCount == 0;
        normalizeAddress(address, shouldBeDefault);

        if (shouldBeDefault) {
            userAddressRepository.clearDefaultByUserId(address.getUserId());
        }

        LocalDateTime now = LocalDateTime.now();
        address.setCreatedAt(now);
        address.setUpdatedAt(now);
        userAddressRepository.insert(address);

        log.info("用户添加地址: userId={}, address={}", address.getUserId(), address.getAddress());
        return userAddressRepository.findById(address.getId());
    }

    @Override
    @Transactional
    public UserAddress updateAddress(Long id, UserAddress address) {
        UserAddress existing = userAddressRepository.findById(id);
        if (existing == null) {
            throw new BusinessException("地址不存在");
        }

        // 强制保持原地址归属，禁止跨用户迁移
        address.setId(id);
        address.setUserId(existing.getUserId());

        validateAddress(address);

        boolean shouldBeDefault = Boolean.TRUE.equals(address.getIsDefault());
        normalizeAddress(address, shouldBeDefault);
        address.setCreatedAt(existing.getCreatedAt());
        address.setUpdatedAt(LocalDateTime.now());

        if (shouldBeDefault) {
            userAddressRepository.clearDefaultByUserId(address.getUserId());
        } else if (Boolean.TRUE.equals(existing.getIsDefault())) {
            address.setIsDefault(true);
        }

        userAddressRepository.update(address);
        return userAddressRepository.findById(id);
    }

    @Override
    public void deleteAddress(Long id) {
        UserAddress existing = userAddressRepository.findById(id);
        if (existing == null) {
            throw new BusinessException("地址不存在");
        }

        userAddressRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long id, Long userId) {
        UserAddress address = userAddressRepository.findById(id);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("地址与用户不匹配");
        }

        userAddressRepository.clearDefaultByUserId(userId);
        userAddressRepository.setDefaultById(id);
    }

    private void validateAddress(UserAddress address) {
        if (address.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (isBlank(address.getContactName())) {
            throw new BusinessException("联系人不能为空");
        }
        if (isBlank(address.getContactPhone())) {
            throw new BusinessException("联系电话不能为空");
        }
        if (isBlank(address.getAddress())) {
            throw new BusinessException("配送地址不能为空");
        }
    }

    private void normalizeAddress(UserAddress address, boolean isDefault) {
        address.setContactName(trimToNull(address.getContactName()));
        address.setContactPhone(trimToNull(address.getContactPhone()));
        address.setProvince(trimToNull(address.getProvince()));
        address.setCity(trimToNull(address.getCity()));
        address.setDistrict(trimToNull(address.getDistrict()));
        address.setAddress(trimToNull(address.getAddress()));
        address.setDetailAddress(trimToNull(address.getDetailAddress()));
        address.setLabel(trimToNull(address.getLabel()));
        address.setIsDefault(isDefault);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
