package com.example.crypto.dao;

import com.example.crypto.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 訂閱リポジトリインターフェース
 * 訂閱情報のデータベース操作を定義
 */
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUsername(String username);
    List<Subscription> findByDataType(String dataType);
}