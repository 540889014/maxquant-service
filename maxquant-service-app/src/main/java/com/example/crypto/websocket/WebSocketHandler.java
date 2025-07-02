package com.example.crypto.websocket;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocketハンドラ
 * クライアントとの接続管理とメッセージ送信を処理
 */
@Component
@ServerEndpoint("/ws/market")
public class WebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private static final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        try {
            sessions.put(session.getId(), session);
            logger.info("WebSocketクライアント接続: sessionId={}", session.getId());
        } catch (Exception e) {
            logger.error("WebSocket接続処理に失敗: sessionId={}, error={}", session.getId(), e.getMessage(), e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        try {
            sessions.remove(session.getId());
            logger.info("WebSocketクライアント切断: sessionId={}", session.getId());
        } catch (Exception e) {
            logger.error("WebSocket切断処理に失敗: sessionId={}, error={}", session.getId(), e.getMessage(), e);
        }
    }

    public void sendMessage(String message) {
        logger.debug("WebSocketメッセージ送信: {}", message);
        for (Session session : sessions.values()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                logger.error("WebSocketメッセージ送信に失敗: sessionId={}, error={}", session.getId(), e.getMessage(), e);
            }
        }
    }
}