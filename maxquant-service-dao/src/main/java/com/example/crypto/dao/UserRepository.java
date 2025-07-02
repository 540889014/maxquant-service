package com.example.crypto.dao;

import com.example.crypto.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * ユーザリポジトリインターフェース
 * ユーザ情報のデータベース操作を定義
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}