package com.example.crypto.service;

import com.example.crypto.dto.KlineDataDTO;
import com.example.crypto.entity.CryptoMetadata;
import com.example.crypto.entity.DepthData;
import com.example.crypto.entity.KlineData;
import com.example.crypto.entity.RealtimeData;

import java.util.List;

/**
 * 市場データサービスインターフェース
 * 市場データの取得を定義
 */
public interface MarketService {
    List<KlineDataDTO> getKlineData(String symbol, String timeframe, Long startTime, Long endTime, String exchange);
    RealtimeData getRealtimeData(String symbol, String exchange);
    List<CryptoMetadata> getInstruments(String instType, String exchange);
    DepthData getDepthData(String symbol, String exchange);
    List<DepthData> getDepthData(String symbol, String exchange, Long startTime, Long endTime);
    List<DepthData> getDepthDataPage(String symbol, String exchange, Long startTime, Long endTime, int pageSize);
}