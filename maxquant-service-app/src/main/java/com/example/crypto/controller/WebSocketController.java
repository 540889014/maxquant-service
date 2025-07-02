package com.example.crypto.controller;

import com.example.crypto.service.OkxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.example.crypto.config.ProxyConfig;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * WebSocketコントローラ
 * OKXのリアルタイムデータをクライアントに配信
 */
@Component
@ServerEndpoint("/ws/market")
public class WebSocketController {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);
    private static final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final OkxService okxService;
    private final ObjectMapper mapper = new ObjectMapper();
    private Session session;
    private final ProxyConfig proxyConfig;

    @Value("${okx.ws-url}")
    private String wsUrl;

    @Autowired
    public WebSocketController(OkxService okxService, ProxyConfig proxyConfig) {
        this.okxService = okxService;
        this.proxyConfig = proxyConfig;
        logger.info("WebSocketController 構造函數初始化，wsUrl={}", wsUrl);
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        logger.info("開始初始化 WebSocket 接続");
        connectToOkx();
    }

    private void connectToOkx() {
        logger.info("WebSocket接続を開始: {}", wsUrl);
        if (wsUrl == null || wsUrl.isEmpty()) {
            logger.error("WebSocket URL 未配置或為空");
            throw new IllegalStateException("WebSocket URL is null or empty");
        }
        try {
            OkHttpClient.Builder wsBuilder = new OkHttpClient.Builder();
            if (proxyConfig.isEnabled() && proxyConfig.getHost() != null && proxyConfig.getWsPort() > 0) {
                Proxy.Type wsType = "socks".equalsIgnoreCase(proxyConfig.getWsType()) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
                wsBuilder.proxy(new Proxy(wsType, new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getWsPort())));
            }
            OkHttpClient client = wsBuilder.build();
            Request request = new Request.Builder().url(wsUrl).build();
            client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    logger.debug("WebSocketメッセージ受信: {}", text);
                    try {
                        JsonNode node = mapper.readTree(text);
                        if (node.has("data")) {
                            for (JsonNode data : node.get("data")) {
                                String symbol = data.get("instId").asText();
                                double price = data.get("last").asDouble();
                                long timestamp = data.get("ts").asLong();
                                okxService.saveRealtimeData(symbol, price, timestamp);
                                String message = mapper.writeValueAsString(
                                        Map.of("symbol", symbol, "price", price, "timestamp", timestamp));
                                for (Session session : sessions.values()) {
                                    session.getBasicRemote().sendText(message);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("WebSocketメッセージ処理に失敗: {}", e.getMessage(), e);
                    }
                }
            });
        } catch (Exception e) {
            logger.error("WebSocket接続の開始に失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start WebSocket", e);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        logger.info("WebSocket接続確立: sessionId={}", session.getId());
        try {
            // WebSocketクライアント初期化（如需用到代理，使用ProxyConfig参数）
            if (proxyConfig.isEnabled() && proxyConfig.getHost() != null && proxyConfig.getWsPort() > 0) {
                Proxy.Type wsType = "socks".equalsIgnoreCase(proxyConfig.getWsType()) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
                Proxy proxy = new Proxy(wsType, new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getWsPort()));
                // ... 使用 proxy 进行后续操作 ...
            } else {
                // ... 不使用代理的逻辑 ...
            }
        } catch (Exception e) {
            logger.error("WebSocketクライアント初期化失敗: error={}", e.getMessage(), e);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
    }
}