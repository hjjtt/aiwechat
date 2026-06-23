package com.aiwechat.admin.repository;

import com.aiwechat.admin.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问层，继承 MyBatis Plus BaseMapper
 */
@Mapper
public interface UserRepository extends BaseMapper<User> {

    /**
     * 根据OpenID查询用户
     */
    User findByOpenId(@Param("openId") String openId);

    /**
     * 根据用户ID查询用户（别名方法）
     */
    User findByUserId(@Param("id") Long id);

    /**
     * 根据ID查询用户
     */
    User findById(@Param("id") Long id);

    /**
     * 新增用户
     */
    int insert(User user);

    /**
     * 更新用户信息
     */
    int update(User user);

    /**
     * 更新最后登录时间
     */
    int updateLastLogin(@Param("id") Long id);

    /**
     * 带筛选条件的分页查询用户列表
     */
    List<User> findAllWithFilter(@Param("keyword") String keyword,
                                  @Param("status") String status,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    /**
     * 带筛选条件的用户数量统计
     */
    int count(@Param("keyword") String keyword, @Param("status") String status);

    /**
     * 统计用户总数
     */
    int countTotal();

    /**
     * 按状态统计用户数量
     */
    int countByStatus(@Param("status") String status);

    /**
     * 根据ID更新用户状态
     */
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    /**
     * 根据ID删除用户
     */
    int deleteById(@Param("id") Long id);
}
