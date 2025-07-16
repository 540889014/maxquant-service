package com.example.crypto.controller;

import com.example.crypto.dto.KlineDataDTO;
import com.example.crypto.entity.CryptoMetadata;
import com.example.crypto.entity.RealtimeData;
import com.example.crypto.enums.AssetType;
import com.example.crypto.models.ApiResponse;
import com.example.crypto.service.BinanceService;
import com.example.crypto.service.MarketService;
import com.example.crypto.service.OkxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ApiResponse<List<KlineDataDTO>> getKlineData(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam Long startTime,
            @RequestParam Long endTime,
            @RequestParam(required = false) String exchange,
            @RequestParam AssetType assetType
    ) {
        logger.info("K線データリクエスト: symbol={}, timeframe={}, startTime={}, endTime={}, assetType={}", symbol, timeframe, startTime, endTime, assetType);
        // Error cases will be implicitly cast to ApiResponse<List<KlineDataDTO>>
        // This is safe because the data field is null in fail responses.
        if (symbol == null || symbol.trim().isEmpty()) {
            return ApiResponse.fail(400, "契約が必要です");
        }
        if (timeframe == null || timeframe.trim().isEmpty()) {
            return ApiResponse.fail(400, "時間枠が必要です");
        }
        if (startTime == null || endTime == null || startTime >= endTime) {
            return ApiResponse.fail(400, "開始時間和結束時間無効");
        }
        try {
            List<KlineDataDTO> data = marketService.getKlineData(symbol, timeframe, startTime, endTime, exchange, assetType);
            return ApiResponse.ok(data);
        } catch (Exception e) {
            logger.error("獲取 K 線數據失敗: symbol={}, timeframe={}, error={}", e.getMessage(), e);
            return ApiResponse.fail(500, "獲取 K 線數據失敗: " + e.getMessage());
        }
    }

    @GetMapping("/realtime")
    public ApiResponse<RealtimeData> getRealtimeData(
            @RequestParam String symbol,
            @RequestParam String exchange
    ) {
        logger.info("リアルタイムデータリクエスト: symbol={}, exchange={}", symbol, exchange);
        if (symbol == null || symbol.trim().isEmpty()) {
            return ApiResponse.fail(400, "契約が必要です");
        }
        try {
            RealtimeData data = marketService.getRealtimeData(symbol, exchange);
            if (data == null) {
                return ApiResponse.fail(404, "リアルタイムデータが見つかりません: symbol=" + symbol + ", exchange=" + exchange);
            }
            return ApiResponse.ok(data);
        } catch (Exception e) {
            logger.error("リアルタイムデータリクエスト処理に失敗: symbol={}, exchange={}, error={}", symbol, exchange, e.getMessage(), e);
            return ApiResponse.fail(500, "リアルタイムデータ取得に失敗: " + e.getMessage());
        }
    }

    @GetMapping("/depth")
    public ApiResponse<Map<String, Object>> getDepthDataPage(
            @RequestParam String symbol,
            @RequestParam String exchange,
            @RequestParam Long startTime,
            @RequestParam(required=false) Long endTime,
            @RequestParam(defaultValue = "100000") int pageSize
    ) {
        var list = marketService.getDepthDataPage(symbol, exchange, startTime, endTime, pageSize);
        Long nextCursor = list.isEmpty()? 0 : list.get(list.size()-1).getTimestamp();
        return ApiResponse.ok(Map.of(
                "data", list,
                "nextCursor", nextCursor,
                "pageSize", list.size()
        ));
    }

    @GetMapping("/instruments")
    public ApiResponse<List<CryptoMetadata>> getInstruments(
            @RequestParam(required = false) String instType,
            @RequestParam(required = false) String exchange
    ) {
        if ("binance".equalsIgnoreCase(exchange) && instType != null) {
            return ApiResponse.ok(binanceService.getInstrumentsByType(instType));
        }
        return ApiResponse.ok(marketService.getInstruments(instType, exchange));
    }

    @PostMapping("/sync-instrument")
    public ApiResponse<String> syncInstrument(@RequestBody Map<String, String> payload) {
        String exchange = payload.get("exchange");
        String instId = payload.get("instId");

        if (exchange == null || instId == null) {
            return ApiResponse.fail(400, "'exchange' and 'instId' are required.");
        }

        logger.info("手动触发单个合约同步: exchange={}, instId={}", exchange, instId);
        try {
            if ("okx".equalsIgnoreCase(exchange)) {
                okxService.syncInstrumentByInstId(instId);
                return ApiResponse.ok("OKX instrument '" + instId + "' synced successfully.");
            } else if ("binance".equalsIgnoreCase(exchange)) {
                return ApiResponse.fail(400, "Binance single instrument sync is not yet implemented.");
            } else {
                return ApiResponse.fail(400, "Unsupported exchange: " + exchange);
            }
        } catch (Exception e) {
            logger.error("单个合约同步失败: exchange={}, instId={}, error={}", exchange, instId, e.getMessage(), e);
            return ApiResponse.fail(500, "Failed to sync instrument: " + e.getMessage());
        }
    }

    @GetMapping("/sync-binance-instruments")
    public ApiResponse<String> syncBinanceInstruments() {
        logger.info("手动触发币安合约同步");
        try {
            binanceService.syncInstruments();
            return ApiResponse.ok("币安合约同步成功");
        } catch (Exception e) {
            logger.error("币安合约同步失败: {}", e.getMessage(), e);
            return ApiResponse.fail(500, "币安合约同步失败: " + e.getMessage());
        }
    }
}