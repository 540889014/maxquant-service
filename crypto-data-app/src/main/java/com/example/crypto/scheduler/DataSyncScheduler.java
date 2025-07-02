package com.example.crypto.scheduler;

import com.example.crypto.service.OkxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * データ同期スケジューラ
 * OKXから契約情報とK線データを定期的に取得
 */
@Component
public class DataSyncScheduler {
    private static final Logger logger = LoggerFactory.getLogger(DataSyncScheduler.class);
    private final OkxService okxService;

    public DataSyncScheduler(OkxService okxService) {
        this.okxService = okxService;
    }

    @Scheduled(fixedRate = 3600000)
    public void syncData() {
        logger.info("データ同期タスクを開始");
        try {
            okxService.syncInstruments();
            logger.info("データ同期タスクが正常に完了");
        } catch (Exception e) {
            logger.error("データ同期タスクに失敗: {}", e.getMessage(), e);
        }
    }
}