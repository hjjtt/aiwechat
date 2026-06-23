package com.aiwechat.auth.repository;

import com.aiwechat.auth.model.entity.UserSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserSessionRepository extends BaseMapper<UserSession> {

    Optional<UserSession> findByToken(@Param("token") String token);

    int insert(UserSession userSession);

    int updateLastActive(@Param("token") String token);

    int deleteExpired();

    int deleteByUserId(@Param("userId") Long userId);
}
