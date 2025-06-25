package com.example.crypto.service;

import com.example.crypto.entity.Subscription;

import java.util.List;

/**
 * 訂閱サービスインターフェース
 * 訂閱情報の操作を定義
 */
public interface SubscriptionService {
    Subscription subscribe(String username, String symbol, String dataType, String instType, String timeframe, String exchange);
    void unsubscribe(String username, String symbol, String dataType, String exchange);
    List<Subscription> getSubscriptionsByUsername(String username);
    List<Subscription> getSubscriptionsByDataType(String dataType);
    List<Subscription> getSubscriptionsByUsernameAndExchange(String username, String exchange);
    List<Subscription> getSubscriptionsByDataTypeAndExchange(String dataType, String exchange);
    List<String> getActiveSubscriptions();
    List<String> getActiveSubscriptions(String exchange);
}