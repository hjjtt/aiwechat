package com.aiwechat.admin.service;

import com.aiwechat.admin.model.entity.User;

import java.util.Map;

/**
 * 管理后台用户服务接口
 */
public interface AdminUserService {

    /**
     * 获取用户列表（分页+筛选）
     *
     * @param keyword 搜索关键词（昵称/OpenID）
     * @param status  用户状态筛选
     * @param page    页码
     * @param size    每页数量
     * @return 分页数据
     */
    Map<String, Object> getUsers(String keyword, String status, int page, int size);

    /**
     * 获取用户统计数据
     *
     * @return 统计信息
     */
    Map<String, Object> getStats();

    /**
     * 根据ID获取用户详情
     *
     * @param id 用户ID
     * @return 用户信息
     */
    User getUserById(Long id);

    /**
     * 更新用户状态
     *
     * @param id     用户ID
     * @param status 新状态
     */
    void updateUserStatus(Long id, String status);

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    void deleteUser(Long id);
}
