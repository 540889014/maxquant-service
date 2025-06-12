package com.example.crypto.scheduler;

import com.example.crypto.entity.Subscription;
import com.example.crypto.service.OkxService;
import com.example.crypto.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 訂閱データ同期スケジューラ
 * OHLCデータの定時取得を処理
 */
@Component
public class SubscriptionScheduler {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionScheduler.class);
    private final SubscriptionService subscriptionService;
    private final OkxService okxService;

    // 記錄每個訂閱的最新時間戳
    private final Map<String, Long> lastTimestamps = new HashMap<>();

    public SubscriptionScheduler(SubscriptionService subscriptionService, OkxService okxService) {
        this.subscriptionService = subscriptionService;
        this.okxService = okxService;
    }

    @Scheduled(fixedRate = 300000) // 每 5 分鐘執行一次
    public void syncSubscribedOhlcData() {
        logger.info("OHLCデータ同期タスクを開始");
        try {
            List<Subscription> subscriptions = subscriptionService.getSubscriptionsByDataType("ohlc");
            logger.debug("OHLC訂閱件数: {}", subscriptions.size());
            for (Subscription sub : subscriptions) {
                String symbol = sub.getSymbol();
                String timeframe = sub.getTimeframe() != null ? sub.getTimeframe() : "1h";
                String key = symbol + "-" + timeframe;
                Long lastTimestamp = lastTimestamps.getOrDefault(key, null);
                okxService.syncKlineData(symbol, timeframe, lastTimestamp);
                logger.info("OHLCデータを同期: symbol={}, timeframe={}", symbol, timeframe);
                // 更新最新時間戳（假設 syncKlineData 更新了 lastTimestamps）
            }
            logger.info("OHLCデータ同期タスクが正常に完了");
        } catch (Exception e) {
            logger.error("OHLCデータ同期タスクに失敗: {}", e.getMessage(), e);
        }
    }
}