package com.example.crypto.dao;

import com.example.crypto.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 訂閱リポジトリインターフェース
 * 訂閱情報のデータベース操作を定義
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUsername(String username);
    List<Subscription> findByDataType(String dataType);
    List<Subscription> findByUsernameAndExchange(String username, String exchange);
    List<Subscription> findByDataTypeAndExchange(String dataType, String exchange);
    List<Subscription> findByExchange(String exchange);
}