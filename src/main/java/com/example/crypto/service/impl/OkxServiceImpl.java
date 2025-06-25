package com.example.crypto.service.impl;

import com.example.crypto.config.ProxyConfig;
import com.example.crypto.dao.CryptoMetadataRepository;
import com.example.crypto.dao.DepthDataRepository;
import com.example.crypto.dao.KlineDataRepository;
import com.example.crypto.dao.RealtimeDataRepository;
import com.example.crypto.entity.CryptoMetadata;
import com.example.crypto.entity.DepthData;
import com.example.crypto.entity.KlineData;
import com.example.crypto.entity.RealtimeData;
import com.example.crypto.entity.Subscription;
import com.example.crypto.service.ClickHouseService;
import com.example.crypto.service.OkxService;
import com.example.crypto.service.SubscriptionService;
import com.example.crypto.websocket.MarketWebSocketEndpoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;

/**
 * OKX APIサービスの実施クラス
 * OKXから契約情報、K線、リアルタイム、深度データを取得
 */
@Service
@EnableScheduling
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
    private final ProxyConfig proxyConfig;
    private final ClickHouseService clickHouseService;

    @Value("${okx.api-url}")
    private String apiUrl;

    @Value("${okx.ws-url}")
    private String wsUrl;

    private WebSocket webSocket;
    private final Set<String> subscribedDepthSymbols = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> lastTimestamps = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = Integer.MAX_VALUE; // unlimited
    private static final long INITIAL_RECONNECT_DELAY = 5; // seconds
    private static final long MAX_RECONNECT_DELAY = 300;   // cap delay to 5 min

    // Cache for the latest depth data for each symbol
    private final Map<String, DepthDataHolder> latestDepthDataCache = new ConcurrentHashMap<>();

    // Private record to hold data in the cache
    private record DepthDataHolder(String symbol, String bids, String asks, long timestamp) {}

    private static final List<String> INST_TYPES = Arrays.asList("SPOT", "FUTURES", "SWAP", "OPTION", "MARGIN");

    @Autowired
    public OkxServiceImpl(
        CryptoMetadataRepository metadataRepository,
        KlineDataRepository klineDataRepository,
        RealtimeDataRepository realtimeDataRepository,
        DepthDataRepository depthDataRepository,
        MarketWebSocketEndpoint webSocketEndpoint,
        SubscriptionService subscriptionService,
        ProxyConfig proxyConfig,
        ClickHouseService clickHouseService
    ) {
        this.metadataRepository = metadataRepository;
        this.klineDataRepository = klineDataRepository;
        this.realtimeDataRepository = realtimeDataRepository;
        this.depthDataRepository = depthDataRepository;
        this.webSocketEndpoint = webSocketEndpoint;
        this.subscriptionService = subscriptionService;
        this.proxyConfig = proxyConfig;
        this.clickHouseService = clickHouseService;
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (proxyConfig.isEnabled() && proxyConfig.getHost() != null && proxyConfig.getPort() > 0) {
            Proxy.Type type = "socks".equalsIgnoreCase(proxyConfig.getType()) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
            builder.proxy(new Proxy(type, new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort())));
        }
        this.client = builder.build();
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
            JsonNode root = mapper.readTree(response);
            JsonNode data = root.get("data");

            List<KlineData> klineList = new ArrayList<>();
            long latestTimestamp = lastTimestamp != null ? lastTimestamp : 0;
            for (JsonNode node : data) {
                long timestamp = node.get(0).asLong();

                Optional<KlineData> existingKline = klineDataRepository.findBySymbolAndTimeframeAndTimestampAndExchange(symbol, timeframe, timestamp, "okx");

                KlineData kline;
                if (existingKline.isPresent()) {
                    kline = existingKline.get();
                    logger.debug("K線數據已存在，將進行更新: symbol={}, timeframe={}, timestamp={}", symbol, timeframe, timestamp);
                } else {
                    kline = new KlineData();
                    kline.setSymbol(symbol);
                    kline.setTimeframe(timeframe);
                    kline.setTimestamp(timestamp);
                    kline.setExchange("okx");
                    kline.setCreatedAt(LocalDateTime.now());
                }

                kline.setOpenPrice(node.get(1).asDouble());
                kline.setHighPrice(node.get(2).asDouble());
                kline.setLowPrice(node.get(3).asDouble());
                kline.setClosePrice(node.get(4).asDouble());
                kline.setVolume(node.get(5).asDouble());

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

    @Override
    public void syncInstruments() {
        for (String instType : INST_TYPES) {
            logger.info("契約情報の同期を開始: instType={}, apiUrl={}", instType, apiUrl);
            try {
                Request request = new Request.Builder().url(apiUrl + "/api/v5/public/instruments?instType=" + instType).build();
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
                    metadata.setExchange("okx");
                    metadata.setCtVal(node.has("ctVal") ? node.get("ctVal").asText() : null);
                    metadata.setCtMult(node.has("ctMult") ? node.get("ctMult").asText() : null);
                    metadata.setCtValCcy(node.has("ctValCcy") ? node.get("ctValCcy").asText() : null);
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
    public void syncInstrumentByInstId(String instId) {
        logger.info("按ID同步单个合约信息: instId={}, apiUrl={}", instId, apiUrl);
        try {
            Request request = new Request.Builder()
                    .url(apiUrl + "/api/v5/public/instruments?instId=" + instId)
                    .build();
            String response = client.newCall(request).execute().body().string();
            logger.debug("单个合约信息获取成功: instId={}, response={}", instId, response);
            JsonNode data = mapper.readTree(response).get("data");

            if (data != null && data.isArray() && data.size() > 0) {
                JsonNode node = data.get(0);
                CryptoMetadata metadata = new CryptoMetadata();
                metadata.setId(node.get("instId").asText());
                metadata.setInstType(node.get("instType").asText());
                metadata.setInstId(node.get("instId").asText());
                metadata.setBaseCcy(node.get("baseCcy").asText());
                metadata.setQuoteCcy(node.get("quoteCcy").asText());
                metadata.setState(node.get("state").asText());
                metadata.setExchange("okx");
                metadata.setCtVal(node.has("ctVal") ? node.get("ctVal").asText() : null);
                metadata.setCtMult(node.has("ctMult") ? node.get("ctMult").asText() : null);
                metadata.setCtValCcy(node.has("ctValCcy") ? node.get("ctValCcy").asText() : null);
                
                metadataRepository.save(metadata);
                logger.info("单个合约信息已保存到数据库: instId={}", instId);
            } else {
                logger.warn("按ID同步合约信息失败: 未找到合约信息 instId={}", instId);
            }
        } catch (Exception e) {
            logger.error("按ID同步单个合约信息失败: instId={}, error={}", instId, e.getMessage(), e);
            throw new RuntimeException("Failed to sync instrument by ID: " + instId, e);
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
            data.setExchange("okx");
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
        // This method is called from the WebSocket listener, so it should be very fast.
        // It just updates the cache with the latest data for the symbol.
        latestDepthDataCache.put(symbol, new DepthDataHolder(symbol, bids, asks, timestamp));
        logger.trace("深度數據已緩存: symbol={}", symbol);
    }

    @Scheduled(fixedRate = 2000)
    public void persistCachedData() {
        Set<String> symbolsToPersist = new HashSet<>(latestDepthDataCache.keySet());
        if (symbolsToPersist.isEmpty()) {
            return;
        }
        logger.debug("Persisting {} depth data records from cache.", symbolsToPersist.size());

        List<DepthData> batchToSave = new ArrayList<>();
        for (String symbol : symbolsToPersist) {
            DepthDataHolder holder = latestDepthDataCache.remove(symbol);
            if (holder != null) {
                DepthData data = new DepthData();
                data.setSymbol(holder.symbol());
                data.setBids(holder.bids());
                data.setAsks(holder.asks());
                data.setTimestamp(holder.timestamp());
                data.setExchange("okx");
                data.setCreatedAt(LocalDateTime.now());
                batchToSave.add(data);
            }
        }

        if (!batchToSave.isEmpty()) {
            clickHouseService.saveDepthData(batchToSave);
            logger.info("Successfully persisted {} depth data records to ClickHouse.", batchToSave.size());
        }
    }

    @Scheduled(fixedRate = 25000)
    public void sendPing() {
        if (webSocket != null) {
            boolean sent = webSocket.send("ping");
            if (sent) {
                logger.debug("Sent WebSocket ping");
            } else {
                logger.warn("Failed to send WebSocket ping, queue might be full. Scheduling reconnect.");
                scheduleReconnect();
            }
        }
    }

    @Override
    public void startWebSocket() {
        if (wsUrl == null || wsUrl.isEmpty()) {
            logger.error("WebSocket URL is not configured. Cannot start WebSocket.");
            return;
        }
        
        logger.info("Attempting to start WebSocket connection to {}", wsUrl);

        // Close any existing connection before creating a new one.
        if (webSocket != null) {
            webSocket.close(1000, "Initiating new connection");
        }
        
        try {
            // Re-creating client to respect potential ws-specific proxy settings
            OkHttpClient.Builder wsBuilder = new OkHttpClient.Builder();
            if (proxyConfig.isEnabled() && proxyConfig.getHost() != null && proxyConfig.getWsPort() > 0) {
                Proxy.Type wsType = "socks".equalsIgnoreCase(proxyConfig.getWsType()) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
                wsBuilder.proxy(new Proxy(wsType, new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getWsPort())));
            }
            OkHttpClient wsClient = wsBuilder
                    .pingInterval(20, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .build();
            Request request = new Request.Builder().url(wsUrl).build();
            webSocket = wsClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket ws, okhttp3.Response response) {
                    logger.info("WebSocket connection established successfully with {}", wsUrl);
                    webSocket = ws;
                    reconnectAttempts = 0; // Reset counter on successful connection
                    subscribedDepthSymbols.clear();
                    updateDepthSubscriptions(); 
                }

                @Override
                public void onMessage(WebSocket ws, String text) {
                    if ("pong".equalsIgnoreCase(text)) {
                        logger.debug("Received WebSocket pong.");
                        return;
                    }
                    logger.debug("WebSocketメッセージ受信: {}", text);
                    try {
                        JsonNode node = mapper.readTree(text);
                        if (node.has("data")) {
                            JsonNode dataArray = node.get("data");
                            if (dataArray.isArray()) {
                                for (JsonNode data : dataArray) {
                                    String channel = node.get("arg").get("channel").asText();
                                    String symbol = node.get("arg").get("instId").asText();
                                    long timestamp = Long.parseLong(data.get("ts").asText());

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
                                            logger.trace("深度データを送信: symbol={}", symbol);
                                        } else {
                                            logger.trace("未訂閱的深度數據: symbol={}", symbol);
                                        }
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
                    logger.warn("WebSocket is closing: code={}, reason={}", code, reason);
                    webSocket = null;
                    scheduleReconnect();
                }

                @Override
                public void onFailure(WebSocket ws, Throwable t, okhttp3.Response response) {
                    logger.error("WebSocket connection failed: {}", t.getMessage(), t);
                    webSocket = null;
                    scheduleReconnect();
                }
            });
        } catch (Exception e) {
            logger.error("Failed to initiate WebSocket connection: {}", e.getMessage(), e);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (isReconnecting.get()) {
            logger.debug("Reconnection attempt is already in progress.");
            return;
        }

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            logger.error("Max reconnect attempts ({}) reached. Giving up.", MAX_RECONNECT_ATTEMPTS);
            return;
        }
        
        if (isReconnecting.compareAndSet(false, true)) {
            reconnectAttempts++;
            long delay = (long) (INITIAL_RECONNECT_DELAY * Math.pow(2, Math.max(0, reconnectAttempts - 1)));
            delay = Math.min(delay, MAX_RECONNECT_DELAY);
            delay += ThreadLocalRandom.current().nextLong(1000) / 1000.0; // add 0-1s jitter
            logger.info("WebSocket disconnected. Scheduling reconnect attempt #{} in {} seconds.", reconnectAttempts, delay);

            scheduler.schedule(() -> {
                try {
                    logger.info("Executing reconnect attempt #{}", reconnectAttempts);
                    startWebSocket();
                } finally {
                    isReconnecting.set(false);
                }
            }, delay, TimeUnit.SECONDS);
        }
    }

    @Override
    public void updateDepthSubscriptions() {
        if (webSocket == null) {
            logger.warn("WebSocket is not connected. Cannot update subscriptions.");
            return;
        }
        logger.info("更新 WebSocket 深度數據訂閱");
        try {
            List<Subscription> activeSubscriptions = subscriptionService.getSubscriptionsByDataTypeAndExchange("depth", "okx");
            Set<String> newSymbols = activeSubscriptions.stream()
                .map(Subscription::getSymbol)
                .collect(Collectors.toSet());

            Set<String> symbolsToUnsubscribe = new HashSet<>(subscribedDepthSymbols);
            symbolsToUnsubscribe.removeAll(newSymbols);

            Set<String> symbolsToSubscribe = new HashSet<>(newSymbols);
            symbolsToSubscribe.removeAll(subscribedDepthSymbols);

            if (!symbolsToUnsubscribe.isEmpty()) {
                List<String> args = symbolsToUnsubscribe.stream()
                    .map(s -> String.format("{\"channel\": \"books5\", \"instId\": \"%s\"}", s))
                    .collect(Collectors.toList());
                String unsubscribeMessage = String.format("{\"op\": \"unsubscribe\", \"args\": [%s]}", String.join(",", args));
                webSocket.send(unsubscribeMessage);
                subscribedDepthSymbols.removeAll(symbolsToUnsubscribe);
                logger.info("Unsubscribed from: {}", symbolsToUnsubscribe);
            }

            if (!symbolsToSubscribe.isEmpty()) {
                List<String> args = symbolsToSubscribe.stream()
                    .map(s -> String.format("{\"channel\": \"books5\", \"instId\": \"%s\"}", s))
                    .collect(Collectors.toList());
                String subscribeMessage = String.format("{\"op\": \"subscribe\", \"args\": [%s]}", String.join(",", args));
                webSocket.send(subscribeMessage);
                subscribedDepthSymbols.addAll(symbolsToSubscribe);
                logger.info("Subscribed to: {}", symbolsToSubscribe);
            }

        } catch (Exception e) {
            logger.error("更新深度數據訂閱失敗: {}", e.getMessage(), e);
        }
    }
}