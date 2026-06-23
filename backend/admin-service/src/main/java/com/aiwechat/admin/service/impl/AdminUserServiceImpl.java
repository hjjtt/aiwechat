package com.aiwechat.admin.service.impl;

import com.aiwechat.admin.model.entity.User;
import com.aiwechat.admin.repository.UserRepository;
import com.aiwechat.admin.service.AdminUserService;
import com.aiwechat.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    @Override
    public Map<String, Object> getUsers(String keyword, String status, int page, int size) {
        int offset = (page - 1) * size;
        List<User> users = userRepository.findAllWithFilter(keyword, status, offset, size);
        int total = userRepository.count(keyword, status);

        Map<String, Object> result = new HashMap<>();
        result.put("users", users);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        return result;
    }

    @Override
    public Map<String, Object> getStats() {
        int total = userRepository.countTotal();
        int active = userRepository.countByStatus("active");
        int banned = userRepository.countByStatus("banned");

        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", total);
        result.put("activeUsers", active);
        result.put("bannedUsers", banned);
        return result;
    }

    @Override
    public User getUserById(Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public void updateUserStatus(Long id, String status) {
        int rows = userRepository.updateStatusById(id, status);
        if (rows == 0) {
            throw new BusinessException("用户状态更新失败，用户可能不存在");
        }
    }

    @Override
    public void deleteUser(Long id) {
        int rows = userRepository.deleteById(id);
        if (rows == 0) {
            throw new BusinessException("用户删除失败，用户可能不存在");
        }
    }
}
