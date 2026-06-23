package com.aiwechat.order.service;

import com.aiwechat.order.model.entity.UserAddress;

import java.util.List;

public interface UserAddressService {

    List<UserAddress> getUserAddresses(Long userId);

    UserAddress getDefaultAddress(Long userId);

    UserAddress getAddressById(Long id);

    UserAddress addAddress(UserAddress address);

    UserAddress updateAddress(Long id, UserAddress address);

    void deleteAddress(Long id);

    void setDefaultAddress(Long id, Long userId);
}
