package com.example.crypto.service;

import com.example.crypto.entity.CryptoMetadata;
import java.util.List;

/**
 * Binance API服务接口
 */
public interface BinanceService {
    void syncInstruments();
    void syncKlineData(String symbol, String timeframe, Long lastTimestamp);
    void syncKlineData(String symbol, String timeframe);
    void saveRealtimeData(String symbol, double price, long timestamp);
    void saveDepthData(String symbol, String bids, String asks, long timestamp);
    void startWebSocket();
    void updateDepthSubscriptions();
    List<CryptoMetadata> getInstrumentsByType(String instType);
} 