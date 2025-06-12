package com.example.crypto.service.impl;

import com.example.crypto.dao.CryptoMetadataRepository;
import com.example.crypto.dao.KlineDataRepository;
import com.example.crypto.dao.RealtimeDataRepository;
import com.example.crypto.dao.DepthDataRepository;
import com.example.crypto.entity.*;
import com.example.crypto.service.OkxService;
import com.example.crypto.service.SubscriptionService;
import com.example.crypto.websocket.MarketWebSocketEndpoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * OKX APIサービスの実施クラス
 * OKXから契約情報、K線、リアルタイム、深度データを取得
 */
@Service
public class OkxServiceImpl implements OkxService {
    private static final Logger logger = LoggerFactory.getLogger(OkxServiceImpl.class);
    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final CryptoMetadataRepository metadataRepository;
    private final KlineDataRepository klineDataRepository;
    private final RealtimeDataRepository realtimeDataRepository;
    private final DepthDataRepository depthDataRepository;
    private final MarketWebSocketEndpoint webSocketEndpoint;
    private final SubscriptionService subscriptionService;

    @Value("${okx.api-url}")
    private String apiUrl;

    @Value("${okx.ws-url}")
    private String wsUrl;

    private WebSocket webSocket;
    private final Set<String> subscribedDepthSymbols = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> lastTimestamps = new ConcurrentHashMap<>(); // 記錄最新時間戳

    private static final List<String> INST_TYPES = Arrays.asList("SPOT", "FUTURES", "SWAP", "OPTION", "MARGIN");

    public OkxServiceImpl(CryptoMetadataRepository metadataRepository,
                          KlineDataRepository klineDataRepository,
                          RealtimeDataRepository realtimeDataRepository,
                          DepthDataRepository depthDataRepository,
                          MarketWebSocketEndpoint webSocketEndpoint,
                          SubscriptionService subscriptionService) {
        this.metadataRepository = metadataRepository;
        this.klineDataRepository = klineDataRepository;
        this.realtimeDataRepository = realtimeDataRepository;
        this.depthDataRepository = depthDataRepository;
        this.webSocketEndpoint = webSocketEndpoint;
        this.subscriptionService = subscriptionService;
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 9098));
        this.client = new OkHttpClient.Builder().proxy(proxy).build();
    }

    @PostConstruct
    public void init() {
        logger.info("開始初始化 WebSocket 接続");
        startWebSocket();
        try {
            syncInstruments();
        } catch (Exception e) {
            logger.error("手動同步取引データ失敗: {}", e.getMessage(), e);
        }
        updateDepthSubscriptions();
    }

    @Override
    public void syncKlineData(String symbol, String timeframe, Long lastTimestamp) {
        logger.info("K線データの同期を開始: symbol={}, timeframe={}, lastTimestamp={}", symbol, timeframe, lastTimestamp);
        try {
            StringBuilder url = new StringBuilder(apiUrl + "/api/v5/market/candles?instId=" + symbol + "&bar=" + timeframe);
            if (lastTimestamp != null) {
                url.append("&after=").append(lastTimestamp + 1);
            }
            Request request = new Request.Builder()
                    .url(url.toString())
                    .build();
            String response = client.newCall(request).execute().body().string();
            logger.debug("K線データ取得成功: {}", response);
            JsonNode data = mapper.readTree(response).get("data");

            List<KlineData> klineList = new ArrayList<>();
            long latestTimestamp = lastTimestamp != null ? lastTimestamp : 0;
            for (JsonNode node : data) {
                long timestamp = node.get(0).asLong();
                if (klineDataRepository.existsBySymbolAndTimeframeAndTimestamp(symbol, timeframe, timestamp)) {
                    logger.debug("K線數據已存在，跳過: symbol={}, timeframe={}, timestamp={}", symbol, timeframe, timestamp);
                    continue;
                }
                KlineData kline = new KlineData();
                // 不再手動設置 id，讓數據庫自動生成
                kline.setSymbol(symbol);
                kline.setTimeframe(timeframe);
                kline.setTimestamp(timestamp);
                kline.setOpenPrice(node.get(1).asDouble());
                kline.setHighPrice(node.get(2).asDouble());
                kline.setLowPrice(node.get(3).asDouble());
                kline.setClosePrice(node.get(4).asDouble());
                kline.setVolume(node.get(5).asDouble());
                kline.setCreatedAt(LocalDateTime.now());
                klineList.add(kline);
                latestTimestamp = Math.max(latestTimestamp, timestamp);
            }
            klineDataRepository.saveAll(klineList);
            logger.info("K線データをデータベースに保存: {} 件", klineList.size());
            if (latestTimestamp > (lastTimestamp != null ? lastTimestamp : 0)) {
                String key = symbol + "-" + timeframe;
                lastTimestamps.put(key, latestTimestamp);
                logger.debug("更新最新時間戳: key={}, timestamp={}", key, latestTimestamp);
            }
        } catch (Exception e) {
            logger.error("K線データの同期に失敗: symbol={}, timeframe={}, error={}", symbol, timeframe, e.getMessage(), e);
            throw new RuntimeException("Failed to sync kline data", e);
        }
    }

    @Override
    public void syncKlineData(String symbol, String timeframe) {
        syncKlineData(symbol, timeframe, null);
    }

    // 其他方法保持不變
    @Override
    public void syncInstruments() {
        for (String instType : INST_TYPES) {
            logger.info("契約情報の同期を開始: instType={}, apiUrl={}", instType, apiUrl);
            try {
                Request request = new Request.Builder()
                        .url(apiUrl + "/api/v5/public/instruments?instType=" + instType)
                        .build();
                String response = client.newCall(request).execute().body().string();
                logger.debug("契約情報取得成功: instType={}, response={}", instType, response);
                JsonNode data = mapper.readTree(response).get("data");

                List<CryptoMetadata> metadataList = new ArrayList<>();
                for (JsonNode node : data) {
                    CryptoMetadata metadata = new CryptoMetadata();
                    metadata.setId(node.get("instId").asText());
                    metadata.setInstType(node.get("instType").asText());
                    metadata.setInstId(node.get("instId").asText());
                    metadata.setBaseCcy(node.get("baseCcy").asText());
                    metadata.setQuoteCcy(node.get("quoteCcy").asText());
                    metadata.setState(node.get("state").asText());
                    metadataList.add(metadata);
                }
                metadataRepository.saveAll(metadataList);
                logger.info("契約情報をデータベースに保存: instType={}, 件数={}", instType, metadataList.size());
            } catch (Exception e) {
                logger.error("契約情報の同期に失敗: instType={}, error={}", instType, e.getMessage(), e);
                throw new RuntimeException("Failed to sync instruments for instType: " + instType, e);
            }
        }
    }

    @Override
    public void saveRealtimeData(String symbol, double price, long timestamp) {
        logger.debug("リアルタイムデータを保存: symbol={}, price={}, timestamp={}", symbol, price, timestamp);
        try {
            RealtimeData data = new RealtimeData();
            data.setId(UUID.randomUUID().toString());
            data.setSymbol(symbol);
            data.setPrice(price);
            data.setTimestamp(timestamp);
            data.setCreatedAt(LocalDateTime.now());
            realtimeDataRepository.save(data);
            logger.info("リアルタイムデータをデータベースに保存: symbol={}", symbol);
        } catch (Exception e) {
            logger.error("リアルタイムデータの保存に失敗: symbol={}, error={}", symbol, e.getMessage(), e);
            throw new RuntimeException("Failed to save realtime data", e);
        }
    }

    @Override
    public void saveDepthData(String symbol, String bids, String asks, long timestamp) {
        logger.debug("深度データを保存: symbol={}, timestamp={}", symbol, timestamp);
        try {
            DepthData data = new DepthData();
            data.setId(UUID.randomUUID().toString());
            data.setSymbol(symbol);
            data.setBids(bids);
            data.setAsks(asks);
            data.setTimestamp(timestamp);
            data.setCreatedAt(LocalDateTime.now());
            depthDataRepository.save(data);
            logger.info("深度データをデータベースに保存: symbol={}", symbol);
        } catch (Exception e) {
            logger.error("深度データの保存に失敗: symbol={}, error={}", symbol, e.getMessage(), e);
            throw new RuntimeException("Failed to save depth data", e);
        }
    }

    @Override
    public void startWebSocket() {
        logger.info("WebSocket接続を開始: {}", wsUrl);
        if (wsUrl == null || wsUrl.isEmpty()) {
            logger.error("WebSocket URL 未配置或為空");
            throw new IllegalStateException("WebSocket URL is null or empty");
        }
        try {
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 19250));
            OkHttpClient wsClient = new OkHttpClient.Builder().proxy(proxy).build();
            Request request = new Request.Builder().url(wsUrl).build();
            webSocket = wsClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket ws, okhttp3.Response response) {
                    logger.info("WebSocket接続が確立: {}", wsUrl);
                    webSocket = ws;
                    updateDepthSubscriptions();
                }

                @Override
                public void onMessage(WebSocket ws, String text) {
                    logger.debug("WebSocketメッセージ受信: {}", text);
                    try {
                        JsonNode node = mapper.readTree(text);
                        if (node.has("data")) {
                            for (JsonNode data : node.get("data")) {
                                String channel = node.get("arg").get("channel").asText();
                                String symbol = data.get("instId").asText();
                                long timestamp = data.get("ts").asLong();
                                if ("tickers".equals(channel)) {
                                    double price = data.get("last").asDouble();
                                    saveRealtimeData(symbol, price, timestamp);
                                    String message = mapper.writeValueAsString(
                                            Map.of("type", "realtime", "symbol", symbol, "price", price, "timestamp", timestamp));
                                    webSocketEndpoint.sendMessage(message);
                                    logger.debug("リアルタイムデータを送信: symbol={}", symbol);
                                } else if ("books5".equals(channel)) {
                                    if (subscribedDepthSymbols.contains(symbol)) {
                                        String bids = data.get("bids").toString();
                                        String asks = data.get("asks").toString();
                                        saveDepthData(symbol, bids, asks, timestamp);
                                        String message = mapper.writeValueAsString(
                                                Map.of("type", "depth", "symbol", symbol, "bids", bids, "asks", asks, "timestamp", timestamp));
                                        webSocketEndpoint.sendMessage(message);
                                        logger.debug("深度データを送信: symbol={}", symbol);
                                    } else {
                                        logger.debug("未訂閱的深度數據: symbol={}", symbol);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("WebSocketメッセージ処理に失敗: {}", e.getMessage(), e);
                    }
                }

                @Override
                public void onClosing(WebSocket ws, int code, String reason) {
                    logger.warn("WebSocket接続が閉じています: code={}, reason={}", code, reason);
                    webSocket = null;
                    subscribedDepthSymbols.clear();
                }

                @Override
                public void onFailure(WebSocket ws, Throwable t, okhttp3.Response response) {
                    logger.error("WebSocket接続に失敗: {}", t.getMessage(), t);
                    webSocket = null;
                    subscribedDepthSymbols.clear();
                    try {
                        Thread.sleep(5000);
                        logger.info("WebSocket再接続を試みます");
                        startWebSocket();
                    } catch (InterruptedException e) {
                        logger.error("WebSocket再接続中にエラー: {}", e.getMessage(), e);
                    }
                }
            });
        } catch (Exception e) {
            logger.error("WebSocket接続の開始に失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start WebSocket", e);
        }
    }

    @Override
    public void updateDepthSubscriptions() {
        logger.info("更新 WebSocket 深度數據訂閱");
        try {
            List<Subscription> depthSubscriptions = subscriptionService.getSubscriptionsByDataType("depth");
            Set<String> currentSymbols = depthSubscriptions.stream()
                    .map(Subscription::getSymbol)
                    .collect(Collectors.toSet());

            // 取消不再需要的訂閱
            Set<String> toUnsubscribe = subscribedDepthSymbols.stream()
                    .filter(symbol -> !currentSymbols.contains(symbol))
                    .collect(Collectors.toSet());
            for (String symbol : toUnsubscribe) {
                String unsubscribeMsg = "{\"op\": \"unsubscribe\", \"args\": [{\"channel\": \"books5\", \"instId\": \"" + symbol + "\"}]}";
                if (webSocket != null) {
                    webSocket.send(unsubscribeMsg);
                    subscribedDepthSymbols.remove(symbol);
                    logger.info("已取消深度數據訂閱: symbol={}", symbol);
                }
            }

            // 訂閱新的 symbol
            for (Subscription sub : depthSubscriptions) {
                String symbol = sub.getSymbol();
                if (!subscribedDepthSymbols.contains(symbol)) {
                    String subscribeMsg = "{\"op\": \"subscribe\", \"args\": [{\"channel\": \"books5\", \"instId\": \"" + symbol + "\"}]}";
                    if (webSocket != null) {
                        webSocket.send(subscribeMsg);
                        subscribedDepthSymbols.add(symbol);
                        logger.info("已訂閱深度數據: symbol={}", symbol);
                    } else {
                        logger.warn("WebSocket 未連接到，無法訂閱: symbol={}", symbol);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("更新 WebSocket 訂閱失敗: {}", e.getMessage(), e);
        }
    }
}