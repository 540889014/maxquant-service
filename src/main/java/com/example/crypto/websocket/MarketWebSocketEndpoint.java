package com.example.crypto.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocketエンドポイント
 * クライアントとの接続管理とメッセージ送信を処理
 */
@Component
public class MarketWebSocketEndpoint extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(MarketWebSocketEndpoint.class);
    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            sessions.put(session.getId(), session);
            logger.info("WebSocketクライアント接続: sessionId={}", session.getId());
        } catch (Exception e) {
            logger.error("WebSocket接続処理に失敗: sessionId={}, error={}", session.getId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            sessions.remove(session.getId());
            logger.info("WebSocketクライアント切断: sessionId={}, status={}", session.getId(), status);
        } catch (Exception e) {
            logger.error("WebSocket切断処理に失敗: sessionId={}, error={}", session.getId(), e.getMessage(), e);
            throw e;
        }
    }

    public void sendMessage(String message) {
        logger.debug("WebSocketメッセージ送信: {}", message);
        for (WebSocketSession session : sessions.values()) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                logger.error("WebSocketメッセージ送信に失敗: sessionId={}, error={}", session.getId(), e.getMessage(), e);
            }
        }
    }

    public void broadcastDepthData(String symbol, String bids, String asks, long timestamp, String exchange) {
        try {
            String message = String.format("{\"type\":\"depth\",\"symbol\":\"%s\",\"bids\":%s,\"asks\":%s,\"timestamp\":%d,\"exchange\":\"%s\"}", 
                symbol, bids, asks, timestamp, exchange);
            sendMessage(message);
            logger.info("深度数据广播成功: symbol={}, exchange={}", symbol, exchange);
        } catch (Exception e) {
            logger.error("广播深度数据失败: symbol={}, exchange={}, error={}", symbol, exchange, e.getMessage(), e);
        }
    }
}