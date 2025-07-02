package com.example.crypto.service.impl;

import com.example.crypto.dao.SubscriptionRepository;
import com.example.crypto.entity.Subscription;
import com.example.crypto.enums.AssetType;
import com.example.crypto.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionServiceImpl.class);
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public Subscription subscribe(String username, String symbol, String dataType, String instType, String timeframe, String exchange, AssetType assetType) {
        logger.info("訂閱処理: username={}, symbol={}, dataType={}, instType={}, timeframe={}, exchange={}, assetType={}", username, symbol, dataType, instType, timeframe, exchange, assetType);
        try {
            Subscription subscription = new Subscription();
            subscription.setUsername(username);
            subscription.setSymbol(symbol);
            subscription.setDataType(dataType);
            subscription.setInstType(instType);
            subscription.setTimeframe(dataType.equals("ohlc") ? timeframe : null);
            subscription.setExchange(exchange);
            subscription.setCreatedAt(LocalDateTime.now());
            subscription.setAssetType(assetType);
            Subscription saved = subscriptionRepository.save(subscription);
            logger.debug("訂閱成功: subscriptionId={}", saved.getId());
            return saved;
        } catch (Exception e) {
            logger.error("訂閱処理に失敗: username={}, error={}", username, e.getMessage(), e);
            throw new RuntimeException("Failed to subscribe", e);
        }
    }

    @Override
    public void unsubscribe(String username, String symbol, String dataType, String exchange) {
        logger.info("取消訂閱処理: username={}, symbol={}, dataType={}, exchange={}", username, symbol, dataType, exchange);
        try {
            // This logic might need adjustment if exchange is not sufficient to uniquely identify a subscription.
            List<Subscription> subscriptions = subscriptionRepository.findByUsername(username);
            Subscription target = subscriptions.stream()
                    .filter(sub -> sub.getSymbol().equals(symbol) && sub.getDataType().equals(dataType) && sub.getExchange().equals(exchange))
                    .findFirst()
                    .orElse(null);
            if (target != null) {
                subscriptionRepository.delete(target);
                logger.debug("取消訂閱成功: subscriptionId={}", target.getId());
            } else {
                logger.warn("未找到訂閱記錄: username={}, symbol={}, dataType={}, exchange={}", username, symbol, dataType, exchange);
            }
        } catch (Exception e) {
            logger.error("取消訂閱処理に失敗: username={}, symbol={}, dataType={}, exchange={}, error={}", username, symbol, dataType, exchange, e.getMessage(), e);
            throw new RuntimeException("Failed to unsubscribe", e);
        }
    }

    @Override
    public List<Subscription> getSubscriptionsByUsername(String username) {
        logger.info("ユーザの訂閱情報を取得: username={}", username);
        try {
            List<Subscription> subscriptions = subscriptionRepository.findByUsername(username);
            logger.debug("ユーザの訂閱情報取得成功: username={}, 件数={}", username, subscriptions.size());
            return subscriptions;
        } catch (Exception e) {
            logger.error("ユーザの訂閱情報取得に失敗: username={}, error={}", username, e.getMessage(), e);
            throw new RuntimeException("Failed to get subscriptions by username", e);
        }
    }

    @Override
    public List<Subscription> getSubscriptionsByDataType(String dataType) {
        logger.info("データタイプの訂閱情報を取得: dataType={}", dataType);
        try {
            List<Subscription> subscriptions = subscriptionRepository.findByDataType(dataType);
            logger.debug("データタイプの訂閱情報取得成功: dataType={}, 件数={}", dataType, subscriptions.size());
            return subscriptions;
        } catch (Exception e) {
            logger.error("データタイプの訂閱情報取得に失敗: dataType={}, error={}", dataType, e.getMessage(), e);
            throw new RuntimeException("Failed to get subscriptions by data type", e);
        }
    }

    @Override
    public List<Subscription> getSubscriptionsByUsernameAndAssetType(String username, AssetType assetType) {
        logger.info("ユーザの訂閱情報を取得: username={}, assetType={}", username, assetType);
        try {
            List<Subscription> subscriptions = subscriptionRepository.findByUsernameAndAssetType(username, assetType);
            logger.debug("ユーザの訂閱情報取得成功: username={}, assetType={}, 件数={}", username, assetType, subscriptions.size());
            return subscriptions;
        } catch (Exception e) {
            logger.error("ユーザの訂閱情報取得に失敗: username={}, assetType={}, error={}", username, assetType, e.getMessage(), e);
            throw new RuntimeException("Failed to get subscriptions by username and asset type", e);
        }
    }
    
    @Override
    public List<Subscription> getSubscriptionsByDataTypeAndExchange(String dataType, String exchange) {
        logger.info("データタイプの訂閱情報を取得: dataType={}, exchange={}", dataType, exchange);
        try {
            List<Subscription> subscriptions = subscriptionRepository.findByDataTypeAndExchange(dataType, exchange);
            logger.debug("データタイプの訂閱情報取得成功: dataType={}, exchange={}, 件数={}", dataType, exchange, subscriptions.size());
            return subscriptions;
        } catch (Exception e) {
            logger.error("データタイプの訂閱情報取得に失敗: dataType={}, exchange={}, error={}", dataType, exchange, e.getMessage(), e);
            throw new RuntimeException("Failed to get subscriptions by data type and exchange", e);
        }
    }

    @Override
    public List<String> getActiveSubscriptions() {
        logger.info("获取所有活跃的订阅");
        try {
            List<Subscription> subscriptions = subscriptionRepository.findAll();
            List<String> symbols = subscriptions.stream()
                    .filter(s -> "depth".equals(s.getDataType()))
                    .map(Subscription::getSymbol)
                    .distinct()
                    .collect(Collectors.toList());
            logger.debug("活跃订阅获取成功: 件数={}", symbols.size());
            return symbols;
        } catch (Exception e) {
            logger.error("获取活跃订阅失败: error={}", e.getMessage(), e);
            throw new RuntimeException("Failed to get active subscriptions", e);
        }
    }

    @Override
    public List<String> getActiveSubscriptions(String exchange) {
        return subscriptionRepository.findByExchange(exchange).stream()
                .map(Subscription::getSymbol)
                .distinct()
                .collect(Collectors.toList());
    }
}