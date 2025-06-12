package com.example.crypto.service.impl;

import com.example.crypto.dao.CryptoMetadataRepository;
import com.example.crypto.dao.KlineDataRepository;
import com.example.crypto.dao.RealtimeDataRepository;
import com.example.crypto.dao.DepthDataRepository;
import com.example.crypto.entity.CryptoMetadata;
import com.example.crypto.entity.KlineData;
import com.example.crypto.entity.RealtimeData;
import com.example.crypto.entity.DepthData;
import com.example.crypto.service.MarketService;
import com.example.crypto.dto.KlineDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 市場データサービスの実施クラス
 * K線、リアルタイム、契約情報、深度データの取得を処理
 */
@Service
public class MarketServiceImpl implements MarketService {
    private static final Logger logger = LoggerFactory.getLogger(MarketServiceImpl.class);
    private final KlineDataRepository klineDataRepository;
    private final RealtimeDataRepository realtimeDataRepository;
    private final CryptoMetadataRepository metadataRepository;
    private final DepthDataRepository depthDataRepository;

    public MarketServiceImpl(KlineDataRepository klineDataRepository,
                             RealtimeDataRepository realtimeDataRepository,
                             CryptoMetadataRepository metadataRepository,
                             DepthDataRepository depthDataRepository) {
        this.klineDataRepository = klineDataRepository;
        this.realtimeDataRepository = realtimeDataRepository;
        this.metadataRepository = metadataRepository;
        this.depthDataRepository = depthDataRepository;
    }

    @Override
    public List<KlineDataDTO> getKlineData(String symbol, String timeframe, Long startTime, Long endTime) {
        try {
            logger.info("獲取 K 線數據: symbol={}, timeframe={}, startTime={}, endTime={}", symbol, timeframe, startTime, endTime);
            List<KlineData> data = klineDataRepository.findBySymbolAndTimeframeAndTimestampBetween(symbol, timeframe, startTime, endTime);
            return data.stream()
                    .map(k -> new KlineDataDTO(k.getId(), k.getSymbol(), k.getTimeframe(), k.getTimestamp(), k.getOpenPrice(), k.getHighPrice(), k.getLowPrice(), k.getClosePrice(), k.getVolume(), k.getCreatedAt()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("獲取 K 線數據失敗: symbol={}, timeframe={}, error={}", symbol, timeframe, e.getMessage(), e);
            throw new RuntimeException("Failed to get kline data", e);
        }
    }

    @Override
    public RealtimeData getRealtimeData(String symbol) {
        logger.info("リアルタイムデータの取得: symbol={}", symbol);
        try {
            RealtimeData data = realtimeDataRepository.findTopBySymbolOrderByTimestampDesc(symbol).orElse(null);
            logger.debug("リアルタイムデータ取得成功: symbol={}", symbol);
            return data;
        } catch (Exception e) {
            logger.error("リアルタイムデータの取得に失敗: symbol={}, error={}", symbol, e.getMessage(), e);
            throw new RuntimeException("Failed to get realtime data", e);
        }
    }

    @Override
    public List<CryptoMetadata> getInstruments(String instType) {
        logger.info("契約情報の取得: instType={}", instType);
        try {
            List<CryptoMetadata> data = instType != null ? metadataRepository.findByInstType(instType) : metadataRepository.findAll();
            logger.debug("契約情報取得成功: {} 件", data.size());
            return data;
        } catch (Exception e) {
            logger.error("契約情報の取得に失敗: instType={}, error={}", instType, e.getMessage(), e);
            throw new RuntimeException("Failed to get instruments", e);
        }
    }

    @Override
    public DepthData getDepthData(String symbol) {
        logger.info("深度データの取得: symbol={}", symbol);
        try {
            DepthData data = depthDataRepository.findTopBySymbolOrderByTimestampDesc(symbol).orElse(null);
            logger.debug("深度データ取得成功: symbol={}", symbol);
            return data;
        } catch (Exception e) {
            logger.error("深度データの取得に失敗: symbol={}, error={}", symbol, e.getMessage(), e);
            throw new RuntimeException("Failed to get depth data", e);
        }
    }
}