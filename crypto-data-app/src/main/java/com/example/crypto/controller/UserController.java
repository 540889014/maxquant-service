package com.example.crypto.controller;

import com.example.crypto.entity.User;
import com.example.crypto.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 用户管理控制器
 * 处理用户相关请求
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/add")
    public ResponseEntity<User> addUser(@RequestParam String username, @RequestParam String password, @RequestParam String role) {
        logger.debug("添加用户请求: username={}, role={}", username, role);
        User user = userService.addUser(username, password, role);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable Long userId) {
        logger.debug("删除用户请求: userId={}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.ok("User deleted successfully");
    }

    @PutMapping("/update-password/{userId}")
    public ResponseEntity<User> updateUserPassword(@PathVariable Long userId, @RequestParam String newPassword) {
        logger.debug("修改用户密码请求: userId={}", userId);
        User user = userService.updateUserPassword(userId, newPassword);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {
        logger.debug("获取所有用户请求");
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
} 