package com.example.crypto.service;

/**
 * OKX APIサービスインターフェース
 */
public interface OkxService {
    void syncInstruments();
    void syncInstrumentByInstId(String instId); // 新增方法：按ID同步单个合约
    void syncKlineData(String symbol, String timeframe, Long lastTimestamp); // 添加 lastTimestamp 參數
    void syncKlineData(String symbol, String timeframe); // 添加重载方法，只接受两个参数
    void saveRealtimeData(String symbol, double price, long timestamp);
    void saveDepthData(String symbol, String bids, String asks, long timestamp);
    void startWebSocket();
    void updateDepthSubscriptions();
}