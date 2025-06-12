package com.example.crypto.service.impl;

import com.example.crypto.dao.SubscriptionRepository;
import com.example.crypto.entity.Subscription;
import com.example.crypto.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 訂閱サービスの実施クラス
 * 訂閱情報の操作を処理
 */
@Service
public class SubscriptionServiceImpl implements SubscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionServiceImpl.class);
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public Subscription subscribe(String username, String symbol, String dataType, String instType, String timeframe) {
        logger.info("訂閱処理: username={}, symbol={}, dataType={}, instType={}, timeframe={}", username, symbol, dataType, instType, timeframe);
        try {
            Subscription subscription = new Subscription();
            subscription.setUsername(username);
            subscription.setSymbol(symbol);
            subscription.setDataType(dataType);
            subscription.setInstType(instType);
            subscription.setTimeframe(dataType.equals("ohlc") ? timeframe : null);
            Subscription saved = subscriptionRepository.save(subscription);
            logger.debug("訂閱成功: subscriptionId={}", saved.getId());
            return saved;
        } catch (Exception e) {
            logger.error("訂閱処理に失敗: username={}, error={}", username, e.getMessage(), e);
            throw new RuntimeException("Failed to subscribe", e);
        }
    }

    @Override
    public void unsubscribe(String username, String symbol, String dataType) {
        logger.info("取消訂閱処理: username={}, symbol={}, dataType={}", username, symbol, dataType);
        try {
            List<Subscription> subscriptions = subscriptionRepository.findByUsername(username);
            Subscription target = subscriptions.stream()
                    .filter(sub -> sub.getSymbol().equals(symbol) && sub.getDataType().equals(dataType))
                    .findFirst()
                    .orElse(null);
            if (target != null) {
                subscriptionRepository.delete(target);
                logger.debug("取消訂閱成功: subscriptionId={}", target.getId());
            } else {
                logger.warn("未找到訂閱記錄: username={}, symbol={}, dataType={}", username, symbol, dataType);
            }
        } catch (Exception e) {
            logger.error("取消訂閱処理に失敗: username={}, symbol={}, dataType={}, error={}", username, symbol, dataType, e.getMessage(), e);
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
}