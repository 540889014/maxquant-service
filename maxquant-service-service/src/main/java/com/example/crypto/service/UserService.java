package com.example.crypto.service;

import com.example.crypto.entity.User;
import com.example.crypto.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理服务
 * 处理用户相关操作
 */
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserDao userDao;

    public User addUser(String username, String password, String role) {
        logger.debug("添加用户: username={}, role={}", username, role);
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // 在实际应用中应使用密码加密
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        return userDao.save(user);
    }

    public void deleteUser(Long userId) {
        logger.debug("删除用户: userId={}", userId);
        userDao.deleteById(userId);
    }

    public User updateUserPassword(Long userId, String newPassword) {
        logger.debug("修改用户密码: userId={}", userId);
        User user = userDao.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(newPassword); // 在实际应用中应使用密码加密
        return userDao.save(user);
    }

    public List<User> getAllUsers() {
        logger.debug("获取所有用户");
        return userDao.findAll();
    }

    public User getUserById(Long userId) {
        logger.debug("获取用户信息: userId={}", userId);
        return userDao.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
} 