package com.example.crypto.websocket;

import com.example.crypto.service.WebSocketMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocketエンドポイント
 * クライアントとの接続管理とメッセージ送信を処理
 */
@Service
public class MarketWebSocketEndpoint implements WebSocketMessageService {
    private static final Logger logger = LoggerFactory.getLogger(MarketWebSocketEndpoint.class);
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public MarketWebSocketEndpoint(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void broadcastDepthUpdate(String symbol, Object data) {
        String destination = "/topic/market/depth/" + symbol;
        try {
            messagingTemplate.convertAndSend(destination, data);
            logger.trace("深度データを送信: destination={}, data={}", destination, data);
        } catch (Exception e) {
            logger.error("深度データの送信に失敗: destination={}, error={}", destination, e.getMessage(), e);
        }
    }

    @Override
    public void broadcastRealtimeUpdate(String symbol, Object data) {
        String destination = "/topic/market/realtime/" + symbol;
        try {
            messagingTemplate.convertAndSend(destination, data);
            logger.trace("リアルタイムデータを送信: destination={}, data={}", destination, data);
        } catch (Exception e) {
            logger.error("リアルタイムデータの送信に失敗: destination={}, error={}", destination, e.getMessage(), e);
        }
    }
}