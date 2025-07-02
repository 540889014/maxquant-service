package com.example.crypto.dao;

import com.example.crypto.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * 用户DAO接口
 * 定义用户数据库操作
 */
public interface UserDao extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
} 