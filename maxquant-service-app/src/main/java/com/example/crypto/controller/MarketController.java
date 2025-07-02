package com.example.crypto.controller;

import com.example.crypto.entity.DepthData;
import com.example.crypto.entity.KlineData;
import com.example.crypto.entity.RealtimeData;
import com.example.crypto.enums.AssetType;
import com.example.crypto.service.MarketService;
import com.example.crypto.dto.KlineDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.crypto.entity.CryptoMetadata;
import java.util.List;
import java.util.Map;
import com.example.crypto.service.BinanceService;
import com.example.crypto.service.OkxService;

/**
 * 市場データコントローラ
 * 市場データの取得を処理
 */
@RestController
@RequestMapping("/api/v1/market")
public class MarketController {
    private static final Logger logger = LoggerFactory.getLogger(MarketController.class);
    private final MarketService marketService;
    private final BinanceService binanceService;
    private final OkxService okxService;

    public MarketController(MarketService marketService, BinanceService binanceService, OkxService okxService) {
        this.marketService = marketService;
        this.binanceService = binanceService;
        this.okxService = okxService;
    }

    @GetMapping("/kline")
    public ResponseEntity<?> getKlineData(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam Long startTime,
            @RequestParam Long endTime,
            @RequestParam(required = false) String exchange,
            @RequestParam AssetType assetType
    ) {
        logger.info("K線データリクエスト: symbol={}, timeframe={}, startTime={}, endTime={}, assetType={}", symbol, timeframe, startTime, endTime, assetType);
        try {
            if (symbol == null || symbol.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("契約が必要です");
            }
            if (timeframe == null || timeframe.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("時間枠が必要です");
            }
            if (startTime == null || endTime == null || startTime >= endTime) {
                return ResponseEntity.badRequest().body("開始時間和結束時間無効");
            }
            List<KlineDataDTO> data = marketService.getKlineData(symbol, timeframe, startTime, endTime, exchange, assetType);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("獲取 K 線數據失敗: symbol={}, timeframe={}, error={}", symbol, timeframe, e.getMessage(), e);
            return ResponseEntity.badRequest().body("獲取 K 線數據失敗: " + e.getMessage());
        }
    }

    @GetMapping("/realtime")
    public ResponseEntity<?> getRealtimeData(
            @RequestParam String symbol,
            @RequestParam String exchange
    ) {
        logger.info("リアルタイムデータリクエスト: symbol={}, exchange={}", symbol, exchange);
        try {
            if (symbol == null || symbol.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("契約が必要です");
            }
            RealtimeData data = marketService.getRealtimeData(symbol, exchange);
            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("リアルタイムデータが見つかりません: symbol=" + symbol + ", exchange=" + exchange);
            }
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("リアルタイムデータリクエスト処理に失敗: symbol={}, exchange={}, error={}", symbol, exchange, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("リアルタイムデータ取得に失敗: " + e.getMessage());
        }
    }

    @GetMapping("/depth")
    public ResponseEntity<?> getDepthDataPage(
            @RequestParam String symbol,
            @RequestParam String exchange,
            @RequestParam Long startTime,
            @RequestParam(required=false) Long endTime,
            @RequestParam(defaultValue = "100000") int pageSize
    ) {
        var list = marketService.getDepthDataPage(symbol, exchange, startTime, endTime, pageSize);
        Long nextCursor = list.isEmpty()? 0 : list.get(list.size()-1).getTimestamp();
        return ResponseEntity.ok(Map.of(
                "data", list,
                "nextCursor", nextCursor,
                "pageSize", list.size()
        ));
    }

    @GetMapping("/instruments")
    public ResponseEntity<List<CryptoMetadata>> getInstruments(
            @RequestParam(required = false) String instType,
            @RequestParam(required = false) String exchange
    ) {
        if ("binance".equalsIgnoreCase(exchange) && instType != null) {
            return ResponseEntity.ok(binanceService.getInstrumentsByType(instType));
        }
        return ResponseEntity.ok(marketService.getInstruments(instType, exchange));
    }

    @PostMapping("/sync-instrument")
    public ResponseEntity<?> syncInstrument(@RequestBody Map<String, String> payload) {
        String exchange = payload.get("exchange");
        String instId = payload.get("instId");

        if (exchange == null || instId == null) {
            return ResponseEntity.badRequest().body("'exchange' and 'instId' are required.");
        }

        logger.info("手动触发单个合约同步: exchange={}, instId={}", exchange, instId);
        try {
            if ("okx".equalsIgnoreCase(exchange)) {
                okxService.syncInstrumentByInstId(instId);
                return ResponseEntity.ok().body("OKX instrument '" + instId + "' synced successfully.");
            } else if ("binance".equalsIgnoreCase(exchange)) {
                // 如果需要，将来在这里为 aklzae 添加单点同步逻辑
                return ResponseEntity.badRequest().body("Binance single instrument sync is not yet implemented.");
            } else {
                return ResponseEntity.badRequest().body("Unsupported exchange: " + exchange);
            }
        } catch (Exception e) {
            logger.error("单个合约同步失败: exchange={}, instId={}, error={}", exchange, instId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to sync instrument: " + e.getMessage());
        }
    }

    @GetMapping("/sync-binance-instruments")
    public String syncBinanceInstruments() {
        logger.info("手动触发币安合约同步");
        try {
            binanceService.syncInstruments();
            return "币安合约同步成功";
        } catch (Exception e) {
            logger.error("币安合约同步失败: {}", e.getMessage(), e);
            return "币安合约同步失败: " + e.getMessage();
        }
    }
}