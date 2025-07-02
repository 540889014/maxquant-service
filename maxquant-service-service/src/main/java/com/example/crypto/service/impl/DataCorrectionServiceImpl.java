package com.example.crypto.service.impl;

import com.example.crypto.dao.KlineDataRepository;
import com.example.crypto.dao.projection.KlineIdentifier;
import com.example.crypto.entity.KlineData;
import com.example.crypto.service.DataCorrectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DataCorrectionServiceImpl implements DataCorrectionService {

    private static final Logger logger = LoggerFactory.getLogger(DataCorrectionServiceImpl.class);
    private final KlineDataRepository klineDataRepository;

    public DataCorrectionServiceImpl(KlineDataRepository klineDataRepository) {
        this.klineDataRepository = klineDataRepository;
    }

    @Override
    @Transactional
    public void correctKlineClosePrices() {
        logger.info("开始执行K线收盘价历史数据修正任务...");
        List<KlineIdentifier> identifiers = klineDataRepository.findDistinctIdentifiers();
        logger.info("发现 {} 个独立的K线品种需要检查。", identifiers.size());

        AtomicInteger totalCorrectedCount = new AtomicInteger(0);

        identifiers.forEach(id -> {
            logger.info("正在处理: symbol={}, timeframe={}, exchange={}", id.getSymbol(), id.getTimeframe(), id.getExchange());
            List<KlineData> klines = klineDataRepository.findBySymbolAndTimeframeAndExchangeOrderByTimestampAsc(
                    id.getSymbol(), id.getTimeframe(), id.getExchange()
            );

            if (klines.size() < 2) {
                logger.info("数据点不足（少于2个），跳过处理。");
                return;
            }

            int correctedCount = 0;
            for (int i = 0; i < klines.size() - 1; i++) {
                KlineData currentBar = klines.get(i);
                KlineData nextBar = klines.get(i + 1);

                if (!currentBar.getClosePrice().equals(nextBar.getOpenPrice())) {
                    currentBar.setClosePrice(nextBar.getOpenPrice());
                    correctedCount++;
                }
            }

            if (correctedCount > 0) {
                klineDataRepository.saveAll(klines);
                logger.info("修正了 {} 条K线数据的收盘价。", correctedCount);
                totalCorrectedCount.addAndGet(correctedCount);
            } else {
                logger.info("该品种数据无需修正。");
            }
        });

        logger.info("K线收盘价历史数据修正任务完成。总共修正了 {} 条数据。", totalCorrectedCount.get());
    }
} 