package com.example.crypto.controller;

import com.example.crypto.entity.Subscription;
import com.example.crypto.enums.AssetType;
import com.example.crypto.models.ApiResponse;
import com.example.crypto.service.OkxService;
import com.example.crypto.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 訂閱コントローラ
 * ユーザの訂閱リクエストを処理
 */
@RestController
@RequestMapping("/api/v1/subscription")
public class SubscriptionController {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);
    private final SubscriptionService subscriptionService;
    private final OkxService okxService;

    public SubscriptionController(SubscriptionService subscriptionService, OkxService okxService) {
        this.subscriptionService = subscriptionService;
        this.okxService = okxService;
    }

    @PostMapping("/subscribe")
    public ApiResponse<Subscription> subscribe(
            @RequestParam String username,
            @RequestParam String symbol,
            @RequestParam String dataType,
            @RequestParam String instType,
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false) String exchange,
            @RequestParam AssetType assetType
    ) {
        logger.info("訂閱リクエスト: username={}, symbol={}, dataType={}, instType={}, timeframe={}, exchange={}, assetType={}", username, symbol, dataType, instType, timeframe, exchange, assetType);
        if (username == null || username.trim().isEmpty()) {
            return ApiResponse.fail(400, "ユーザ名が必要です");
        }
        if (symbol == null || symbol.trim().isEmpty()) {
            return ApiResponse.fail(400, "契約が必要です");
        }
        if (dataType == null || dataType.trim().isEmpty()) {
            return ApiResponse.fail(400, "データタイプが必要です");
        }
        if (instType == null || instType.trim().isEmpty()) {
            return ApiResponse.fail(400, "契約タイプが必要です");
        }
        if (assetType != AssetType.FOREX && (exchange == null || exchange.trim().isEmpty())) {
            return ApiResponse.fail(400, "交易所(exchange)が必要です");
        }
        if (dataType.equals("ohlc") && (timeframe == null || timeframe.trim().isEmpty())) {
            return ApiResponse.fail(400, "OHLC データには時間枠が必要です");
        }

        try {
            Subscription subscription = subscriptionService.subscribe(username, symbol, dataType, instType, timeframe, exchange, assetType);
            if ("depth".equals(dataType)) {
                okxService.updateDepthSubscriptions();
            }
            return ApiResponse.ok(subscription);
        } catch (Exception e) {
            logger.error("訂閱リクエスト処理に失敗: username={}, error={}", username, e.getMessage(), e);
            return ApiResponse.fail(500, "訂閱失敗: " + e.getMessage());
        }
    }

    @PostMapping("/unsubscribe")
    public ApiResponse<String> unsubscribe(
            @RequestParam String username,
            @RequestParam String symbol,
            @RequestParam String dataType,
            @RequestParam String exchange
    ) {
        logger.info("取消訂閱リクエスト: username={}, symbol={}, dataType={}, exchange={}", username, symbol, dataType, exchange);
        if (username == null || username.trim().isEmpty()) {
            return ApiResponse.fail(400, "ユーザ名が必要です");
        }
        if (symbol == null || symbol.trim().isEmpty()) {
            return ApiResponse.fail(400, "契約が必要です");
        }
        if (dataType == null || dataType.trim().isEmpty()) {
            return ApiResponse.fail(400, "データタイプが必要です");
        }
        if (exchange == null || exchange.trim().isEmpty()) {
            return ApiResponse.fail(400, "交易所(exchange)が必要です");
        }

        try {
            subscriptionService.unsubscribe(username, symbol, dataType, exchange);
            // 若為深度數據，更新 WebSocket 訂閱
            if ("depth".equals(dataType)) {
                okxService.updateDepthSubscriptions();
            }
            return ApiResponse.ok("取消訂閱成功");
        } catch (Exception e) {
            logger.error("取消訂閱リクエスト処理に失敗: username={}, symbol={}, dataType={}, exchange={}, error={}", username, symbol, dataType, exchange, e.getMessage(), e);
            return ApiResponse.fail(500, "取消訂閱失敗: " + e.getMessage());
        }
    }

    @GetMapping("/user")
    public ApiResponse<List<Subscription>> getSubscriptionsByUsername(
            @RequestParam String username,
            @RequestParam(required = false) AssetType assetType
    ) {
        if (assetType == null) {
            return ApiResponse.ok(subscriptionService.getSubscriptionsByUsername(username));
        } else {
            return ApiResponse.ok(subscriptionService.getSubscriptionsByUsernameAndAssetType(username, assetType));
        }
    }

    @GetMapping("/type")
    public ApiResponse<List<Subscription>> getSubscriptionsByDataType(
            @RequestParam String dataType,
            @RequestParam String exchange
    ) {
        return ApiResponse.ok(subscriptionService.getSubscriptionsByDataTypeAndExchange(dataType, exchange));
    }
}