package com.example.crypto.config;

import com.example.crypto.websocket.MarketWebSocketEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket設定
 * WebSocketエンドポイントを登録
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);
    private final MarketWebSocketEndpoint webSocketEndpoint;

    public WebSocketConfig(MarketWebSocketEndpoint webSocketEndpoint) {
        this.webSocketEndpoint = webSocketEndpoint;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        try {
            registry.addHandler(webSocketEndpoint, "/ws/market")
                    .setAllowedOrigins("*");
            logger.info("WebSocketハンドラを登録: /ws/market");
        } catch (Exception e) {
            logger.error("WebSocketハンドラの登録に失敗: error={}", e.getMessage(), e);
            throw new RuntimeException("Failed to register WebSocket handler", e);
        }
    }
}