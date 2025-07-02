package com.example.crypto.service.impl;

import com.example.crypto.dao.CryptoMetadataRepository;
import com.example.crypto.dao.KlineDataRepository;
import com.example.crypto.dao.RealtimeDataRepository;
import com.example.crypto.dao.DepthDataRepository;
import com.example.crypto.entity.CryptoMetadata;
import com.example.crypto.entity.ForexKline;
import com.example.crypto.entity.KlineData;
import com.example.crypto.entity.RealtimeData;
import com.example.crypto.entity.DepthData;
import com.example.crypto.enums.AssetType;
import com.example.crypto.repository.ForexKlineRepository;
import com.example.crypto.service.MarketService;
import com.example.crypto.dto.KlineDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

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
    private final ForexKlineRepository forexKlineRepository;
    @Autowired @Qualifier("clickhouseJdbcTemplate")
    private JdbcTemplate ck;

    public MarketServiceImpl(KlineDataRepository klineDataRepository,
                             RealtimeDataRepository realtimeDataRepository,
                             CryptoMetadataRepository metadataRepository,
                             DepthDataRepository depthDataRepository,
                             ForexKlineRepository forexKlineRepository) {
        this.klineDataRepository = klineDataRepository;
        this.realtimeDataRepository = realtimeDataRepository;
        this.metadataRepository = metadataRepository;
        this.depthDataRepository = depthDataRepository;
        this.forexKlineRepository = forexKlineRepository;
    }

    @Override
    public List<KlineDataDTO> getKlineData(String symbol, String timeframe, Long startTime, Long endTime, String exchange, AssetType assetType) {
        try {
            if (assetType == AssetType.FOREX) {
                logger.info("獲取外匯 K 線數據: symbol={}, timeframe={}, startTime={}, endTime={}", symbol, timeframe, startTime, endTime);
                List<ForexKline> data = forexKlineRepository.findBySymbolAndPeriodAndTimestampBetween(symbol, timeframe, startTime/1000, endTime/1000);
                return data.stream()
                        .map(k -> new KlineDataDTO(k.getId(), k.getSymbol(), k.getPeriod(), k.getTimestamp(), k.getOpen().doubleValue(), k.getHigh().doubleValue(), k.getLow().doubleValue(), k.getClose().doubleValue(), null, null))
                        .collect(Collectors.toList());
            } else {
                logger.info("獲取 K 線數據: symbol={}, timeframe={}, startTime={}, endTime={}, exchange={}", symbol, timeframe, startTime, endTime, exchange);
                List<KlineData> data = klineDataRepository.findBySymbolAndTimeframeAndTimestampBetweenAndExchange(symbol, timeframe, startTime, endTime, exchange);
                return data.stream()
                        .map(k -> new KlineDataDTO(k.getId(), k.getSymbol(), k.getTimeframe(), k.getTimestamp(), k.getOpenPrice(), k.getHighPrice(), k.getLowPrice(), k.getClosePrice(), k.getVolume(), k.getCreatedAt()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.error("獲取 K 線數據失敗: symbol={}, timeframe={}, exchange={}, assetType={}, error={}", symbol, timeframe, exchange, assetType, e.getMessage(), e);
            throw new RuntimeException("Failed to get kline data", e);
        }
    }

    @Override
    public RealtimeData getRealtimeData(String symbol, String exchange) {
        logger.info("リアルタイムデータの取得: symbol={}, exchange={}", symbol, exchange);
        try {
            RealtimeData data = realtimeDataRepository.findTopBySymbolAndExchangeOrderByTimestampDesc(symbol, exchange).orElse(null);
            logger.debug("リアルタイムデータ取得成功: symbol={}, exchange={}", symbol, exchange);
            return data;
        } catch (Exception e) {
            logger.error("リアルタイムデータの取得に失敗: symbol={}, exchange={}, error={}", symbol, exchange, e.getMessage(), e);
            throw new RuntimeException("Failed to get realtime data", e);
        }
    }

    @Override
    public List<CryptoMetadata> getInstruments(String instType, String exchange) {
        logger.info("契約情報の取得: instType={}, exchange={}", instType, exchange);
        try {
            if (instType != null && exchange != null) {
                return metadataRepository.findByInstTypeAndExchange(instType, exchange);
            } else if (exchange != null) {
                return metadataRepository.findByExchange(exchange);
            } else if (instType != null) {
                return metadataRepository.findByInstType(instType);
            } else {
                return metadataRepository.findAll();
            }
        } catch (Exception e) {
            logger.error("契約情報の取得に失敗: instType={}, exchange={}, error={}", instType, exchange, e.getMessage(), e);
            throw new RuntimeException("Failed to get instruments", e);
        }
    }

    @Override
    public DepthData getDepthData(String symbol, String exchange) {
        logger.info("深度データの取得: symbol={}, exchange={}", symbol, exchange);
        try {
            DepthData data = depthDataRepository.findTopBySymbolAndExchangeOrderByTimestampDesc(symbol, exchange).orElse(null);
            logger.debug("深度データ取得成功: symbol={}, exchange={}", symbol, exchange);
            return data;
        } catch (Exception e) {
            logger.error("深度データの取得に失敗: symbol={}, exchange={}, error={}", symbol, exchange, e.getMessage(), e);
            throw new RuntimeException("Failed to get depth data", e);
        }
    }

    @Override
    public List<DepthData> getDepthDataPage(String symbol,String exchange,
                                            Long startTime,Long endTime,int pageSize){
        long end = endTime==null?System.currentTimeMillis():endTime;
        int limit = pageSize>0? pageSize:100000;
        String sql = """
            SELECT symbol,exchange,timestamp,bids,asks
            FROM   depth_data
            WHERE  symbol=? AND exchange=? AND timestamp>=? AND timestamp<?
            ORDER  BY timestamp ASC
            LIMIT  ?
        """;
        return ck.query(sql,(rs, i)->{
            DepthData d = new DepthData();
            d.setSymbol(rs.getString(1));
            d.setExchange(rs.getString(2));
            d.setTimestamp(rs.getLong(3));
            d.setBids(rs.getString(4));
            d.setAsks(rs.getString(5));
            return d;
        },symbol,exchange,startTime,end,limit);
    }

    @Override
    public List<DepthData> getDepthData(String symbol, String exchange, Long startTime, Long endTime) {
        return getDepthDataPage(symbol, exchange, startTime, endTime, 100000);
    }
}