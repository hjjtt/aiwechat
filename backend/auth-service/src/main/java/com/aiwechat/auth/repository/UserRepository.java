package com.aiwechat.auth.repository;

import com.aiwechat.auth.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserRepository extends BaseMapper<User> {

    Optional<User> findByOpenId(@Param("openId") String openId);

    Optional<User> findById(@Param("id") Long id);

    int insert(User user);

    int update(User user);

    int updateLastLogin(@Param("openId") String openId);
}
