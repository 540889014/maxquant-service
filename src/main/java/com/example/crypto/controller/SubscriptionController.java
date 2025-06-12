package com.example.crypto.controller;

import com.example.crypto.entity.Subscription;
import com.example.crypto.service.SubscriptionService;
import com.example.crypto.service.OkxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> subscribe(
            @RequestParam String username,
            @RequestParam String symbol,
            @RequestParam String dataType,
            @RequestParam String instType,
            @RequestParam(required = false) String timeframe) {
        logger.info("訂閱リクエスト: username={}, symbol={}, dataType={}, instType={}, timeframe={}", username, symbol, dataType, instType, timeframe);
        try {
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("ユーザ名が必要です");
            }
            if (symbol == null || symbol.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("契約が必要です");
            }
            if (dataType == null || dataType.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("データタイプが必要です");
            }
            if (instType == null || instType.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("契約タイプが必要です");
            }
            if (dataType.equals("ohlc") && (timeframe == null || timeframe.trim().isEmpty())) {
                return ResponseEntity.badRequest().body("OHLC データには時間枠が必要です");
            }

            Subscription subscription = subscriptionService.subscribe(username, symbol, dataType, instType, timeframe);
            if ("depth".equals(dataType)) {
                okxService.updateDepthSubscriptions();
            }
            return ResponseEntity.ok(subscription);
        } catch (Exception e) {
            logger.error("訂閱リクエスト処理に失敗: username={}, error={}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("訂閱失敗: " + e.getMessage());
        }
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(
            @RequestParam String username,
            @RequestParam String symbol,
            @RequestParam String dataType) {
        logger.info("取消訂閱リクエスト: username={}, symbol={}, dataType={}", username, symbol, dataType);
        try {
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("ユーザ名が必要です");
            }
            if (symbol == null || symbol.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("契約が必要です");
            }
            if (dataType == null || dataType.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("データタイプが必要です");
            }

            subscriptionService.unsubscribe(username, symbol, dataType);
            // 若為深度數據，更新 WebSocket 訂閱
            if ("depth".equals(dataType)) {
                okxService.updateDepthSubscriptions();
            }
            return ResponseEntity.ok("取消訂閱成功");
        } catch (Exception e) {
            logger.error("取消訂閱リクエスト処理に失敗: username={}, symbol={}, dataType={}, error={}", username, symbol, dataType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("取消訂閱失敗: " + e.getMessage());
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getSubscriptionsByUsername(@RequestParam String username) {
        logger.info("ユーザの訂閱情報リクエスト: username={}", username);
        try {
            List<Subscription> subscriptions = subscriptionService.getSubscriptionsByUsername(username);
            return ResponseEntity.ok(subscriptions);
        } catch (Exception e) {
            logger.error("ユーザの訂閱情報リクエスト処理に失敗: username={}, error={}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ユーザの訂閱情報取得に失敗: " + e.getMessage());
        }
    }

    @GetMapping("/type")
    public ResponseEntity<?> getSubscriptionsByDataType(@RequestParam String dataType) {
        logger.info("データタイプの訂閱情報リクエスト: dataType={}", dataType);
        try {
            List<Subscription> subscriptions = subscriptionService.getSubscriptionsByDataType(dataType);
            return ResponseEntity.ok(subscriptions);
        } catch (Exception e) {
            logger.error("データタイプの訂閱情報リクエスト処理に失敗: dataType={}, error={}", dataType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("データタイプの訂閱情報取得に失敗: " + e.getMessage());
        }
    }
}