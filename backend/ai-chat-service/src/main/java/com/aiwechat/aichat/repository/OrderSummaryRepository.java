package com.aiwechat.aichat.repository;

import com.aiwechat.aichat.model.entity.OrderSummary;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderSummaryRepository extends BaseMapper<OrderSummary> {

    @Select("SELECT id, order_number, user_id, status, total_amount, contact_name, contact_phone, delivery_address, created_at FROM orders WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<OrderSummary> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}
