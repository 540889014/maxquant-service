package com.example.crypto.controller;

import com.example.crypto.entity.DepthData;
import com.example.crypto.entity.KlineData;
import com.example.crypto.entity.RealtimeData;
import com.example.crypto.service.MarketService;
import com.example.crypto.dto.KlineDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 市場データコントローラ
 * 市場データの取得を処理
 */
@RestController
@RequestMapping("/api/v1/market")
public class MarketController {
    private static final Logger logger = LoggerFactory.getLogger(MarketController.class);
    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping("/kline")
    public ResponseEntity<?> getKlineData(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam Long startTime,
            @RequestParam Long endTime) {
        logger.info("K線データリクエスト: symbol={}, timeframe={}, startTime={}, endTime={}", symbol, timeframe, startTime, endTime);
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
            List<KlineDataDTO> data = marketService.getKlineData(symbol, timeframe, startTime, endTime);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("獲取 K 線數據失敗: symbol={}, timeframe={}, error={}", symbol, timeframe, e.getMessage(), e);
            return ResponseEntity.badRequest().body("獲取 K 線數據失敗: " + e.getMessage());
        }
    }

    @GetMapping("/realtime")
    public ResponseEntity<?> getRealtimeData(@RequestParam(required = false) String symbol) { // 設為可選
        logger.info("リアルタイムデータリクエスト: symbol={}", symbol);
        try {
            if (symbol == null || symbol.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("契約が必要です");
            }
            RealtimeData data = marketService.getRealtimeData(symbol);
            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("リアルタイムデータが見つかりません: symbol=" + symbol);
            }
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("リアルタイムデータリクエスト処理に失敗: symbol={}, error={}", symbol, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("リアルタイムデータ取得に失敗: " + e.getMessage());
        }
    }

    @GetMapping("/depth")
    public ResponseEntity<?> getDepthData(@RequestParam String symbol) {
        logger.info("深度データリクエスト: symbol={}", symbol);
        try {
            if (symbol == null || symbol.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("契約が必要です");
            }
            DepthData data = marketService.getDepthData(symbol);
            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("深度データが見つかりません: symbol=" + symbol);
            }
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("深度データリクエスト処理に失敗: symbol={}, error={}", symbol, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("深度データ取得に失敗: " + e.getMessage());
        }
    }
}