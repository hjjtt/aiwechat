package com.aiwechat.order.repository;

import com.aiwechat.order.model.entity.UserAddress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserAddressRepository extends BaseMapper<UserAddress> {

    List<UserAddress> findByUserId(@Param("userId") Long userId);

    UserAddress findById(@Param("id") Long id);

    UserAddress findDefaultByUserId(@Param("userId") Long userId);

    int countByUserId(@Param("userId") Long userId);

    int insert(UserAddress address);

    int update(UserAddress address);

    int deleteById(@Param("id") Long id);

    int clearDefaultByUserId(@Param("userId") Long userId);

    int setDefaultById(@Param("id") Long id);
}
