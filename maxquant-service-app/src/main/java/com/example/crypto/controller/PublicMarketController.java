package com.example.crypto.controller;

import com.example.crypto.entity.CryptoMetadata;
import com.example.crypto.models.ApiResponse;
import com.example.crypto.service.MarketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公開市場データコントローラ
 * 公開データの取得を処理（認証不要）
 */
@RestController
@RequestMapping("/api/v5/public")
public class PublicMarketController {
    private static final Logger logger = LoggerFactory.getLogger(PublicMarketController.class);
    private final MarketService marketService;

    public PublicMarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping("/instruments")
    public ApiResponse<List<CryptoMetadata>> getInstruments(@RequestParam(required = false) String instType, @RequestParam(required = false) String exchange) {
        logger.info("公開契約情報の取得: instType={}, exchange={}", instType, exchange);
        try {
            List<CryptoMetadata> instruments = marketService.getInstruments(instType, exchange);
            logger.debug("公開契約情報取得成功: {} 件", instruments.size());
            return ApiResponse.ok(instruments);
        } catch (Exception e) {
            logger.error("公開契約情報の取得に失敗: instType={}, exchange={}, error={}", instType, exchange, e.getMessage(), e);
            return ApiResponse.fail(500, "Failed to get instruments: " + e.getMessage());
        }
    }
}