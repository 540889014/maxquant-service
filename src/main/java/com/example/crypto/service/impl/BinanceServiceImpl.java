package com.example.crypto.service.impl;

import com.example.crypto.dao.CryptoMetadataRepository;
import com.example.crypto.dao.KlineDataRepository;
import com.example.crypto.dao.RealtimeDataRepository;
import com.example.crypto.dao.DepthDataRepository;
import com.example.crypto.entity.*;
import com.example.crypto.service.BinanceService;
import com.example.crypto.service.ClickHouseService;
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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.example.crypto.config.ProxyConfig;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Binance API服务的实现类
 * 从Binance获取合约信息、K线、实时、深度数据
 */
@Service
@EnableScheduling
public class BinanceServiceImpl implements BinanceService {
    private static final Logger logger = LoggerFactory.getLogger(BinanceServiceImpl.class);
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
    private static final String PROXY_HOST = "127.0.0.1";
    private static final int PROXY_PORT = 9098;

    @Value("${binance.api-url}")
    private String apiUrl;

    @Value("${binance.ws-url}")
    private String wsUrl;

    @Value("${proxy.enabled}")
    private boolean proxyEnabled;

    @Value("${proxy.host}")
    private String proxyHost;

    @Value("${proxy.port}")
    private int proxyPort;

    private WebSocket webSocket;
    private final Set<String> subscribedDepthSymbols = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> lastTimestamps = new ConcurrentHashMap<>();
    private final Map<String, DepthData> latestDepthDataCache = new ConcurrentHashMap<>();

    @Autowired
    public BinanceServiceImpl(CryptoMetadataRepository metadataRepository,
                              KlineDataRepository klineDataRepository,
                              RealtimeDataRepository realtimeDataRepository,
                              DepthDataRepository depthDataRepository,
                              MarketWebSocketEndpoint webSocketEndpoint,
                              SubscriptionService subscriptionService,
                              ProxyConfig proxyConfig,
                              ClickHouseService clickHouseService) {
        this.metadataRepository = metadataRepository;
        this.klineDataRepository = klineDataRepository;
        this.realtimeDataRepository = realtimeDataRepository;
        this.depthDataRepository = depthDataRepository;
        this.webSocketEndpoint = webSocketEndpoint;
        this.subscriptionService = subscriptionService;
        this.proxyConfig = proxyConfig;
        this.clickHouseService = clickHouseService;
        this.client = createHttpClient();
    }

    @PostConstruct
    public void init() {
        logger.info("开始初始化Binance WebSocket连接");
        startWebSocket();
        try {
            syncInstruments();
        } catch (Exception e) {
            logger.error("手动同步Binance交易数据失败: {}", e.getMessage(), e);
        }
        updateDepthSubscriptions();
    }

    @Override
    public void syncInstruments() {
        logger.info("开始同步Binance合约信息, apiUrl={}", apiUrl);
        List<CryptoMetadata> metadataList = new ArrayList<>();
        
        // 同步现货数据
        syncInstrumentsFromEndpoint(apiUrl + "/api/v3/exchangeInfo", "SPOT", metadataList);
        
        // 同步USDT保证金永续合约数据
        syncInstrumentsFromEndpoint(apiUrl.replace("api.binance.com", "fapi.binance.com") + "/fapi/v1/exchangeInfo", "PERPETUAL", metadataList);
        
        // 同步币本位保证金永续合约数据
        syncInstrumentsFromEndpoint(apiUrl.replace("api.binance.com", "dapi.binance.com") + "/dapi/v1/exchangeInfo", "PERPETUAL_COIN", metadataList);
        
        if (!metadataList.isEmpty()) {
            metadataRepository.saveAll(metadataList);
            logger.info("合约信息保存到数据库: 件数={}", metadataList.size());
        } else {
            logger.warn("没有获取到任何合约信息");
        }
    }
    
    private void syncInstrumentsFromEndpoint(String endpoint, String defaultType, List<CryptoMetadata> metadataList) {
        try {
            Request request = new Request.Builder()
                    .url(endpoint)
                    .build();
            String response = client.newCall(request).execute().body().string();
            logger.debug("合约信息获取成功 from {}: response={}", endpoint, response);
            JsonNode data = mapper.readTree(response).get("symbols");
            
            if (data != null) {
                for (JsonNode node : data) {
                    CryptoMetadata metadata = new CryptoMetadata();
                    metadata.setId(node.get("symbol") != null ? node.get("symbol").asText() : "");
                    String contractType = node.get("contractType") != null && !node.get("contractType").asText().isEmpty() ? node.get("contractType").asText() : defaultType;
                    metadata.setInstType(contractType);
                    metadata.setInstId(node.get("symbol") != null ? node.get("symbol").asText() : "");
                    metadata.setBaseCcy(node.get("baseAsset") != null ? node.get("baseAsset").asText() : "");
                    metadata.setQuoteCcy(node.get("quoteAsset") != null ? node.get("quoteAsset").asText() : "");
                    metadata.setState(node.get("status") != null ? node.get("status").asText() : "");
                    metadata.setExchange("binance");
                    metadataList.add(metadata);
                }
                logger.info("从 {} 获取到 {} 条合约信息", endpoint, data.size());
            } else {
                logger.warn("从 {} 获取到的数据为空", endpoint);
            }
        } catch (Exception e) {
            logger.error("同步Binance合约信息失败 from {}: error={}", endpoint, e.getMessage(), e);
        }
    }

    @Override
    public void syncKlineData(String symbol, String timeframe, Long lastTimestamp) {
        logger.info("开始同步Binance K线数据: symbol={}, timeframe={}, lastTimestamp={}", symbol, timeframe, lastTimestamp);
        try {
            StringBuilder url = new StringBuilder(apiUrl + "/api/v3/klines?symbol=" + symbol + "&interval=" + timeframe);
            if (lastTimestamp != null) {
                url.append("&startTime=").append(lastTimestamp + 1);
            }
            Request request = new Request.Builder()
                    .url(url.toString())
                    .build();
            String response = client.newCall(request).execute().body().string();
            logger.debug("K线数据获取成功: {}", response);
            JsonNode data = mapper.readTree(response);

            List<KlineData> klineList = new ArrayList<>();
            long latestTimestamp = lastTimestamp != null ? lastTimestamp : 0;
            for (JsonNode node : data) {
                long timestamp = node.get(0).asLong();

                Optional<KlineData> existingKline = klineDataRepository.findBySymbolAndTimeframeAndTimestampAndExchange(symbol, timeframe, timestamp, "binance");

                KlineData kline;
                if (existingKline.isPresent()) {
                    // 更新现有记录
                    kline = existingKline.get();
                    logger.debug("K线数据已存在，将进行更新: symbol={}, timeframe={}, timestamp={}", symbol, timeframe, timestamp);
                } else {
                    // 创建新记录
                    kline = new KlineData();
                    kline.setSymbol(symbol);
                    kline.setTimeframe(timeframe);
                    kline.setTimestamp(timestamp);
                    kline.setExchange("binance");
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
            logger.info("K线数据保存到数据库: {} 件", klineList.size());
            if (latestTimestamp > (lastTimestamp != null ? lastTimestamp : 0)) {
                String key = symbol + "-" + timeframe;
                lastTimestamps.put(key, latestTimestamp);
                logger.debug("更新最新时间戳: key={}, timestamp={}", key, latestTimestamp);
            }
        } catch (Exception e) {
            logger.error("同步Binance K线数据失败: symbol={}, timeframe={}, error={}", symbol, timeframe, e.getMessage(), e);
            throw new RuntimeException("Failed to sync kline data from Binance", e);
        }
    }

    @Override
    public void syncKlineData(String symbol, String timeframe) {
        syncKlineData(symbol, timeframe, null);
    }

    @Override
    public void saveRealtimeData(String symbol, double price, long timestamp) {
        logger.debug("保存Binance实时数据: symbol={}, price={}, timestamp={}", symbol, price, timestamp);
        try {
            RealtimeData data = new RealtimeData();
            data.setId(UUID.randomUUID().toString());
            data.setSymbol(symbol);
            data.setPrice(price);
            data.setTimestamp(timestamp);
            data.setExchange("binance");
            data.setCreatedAt(LocalDateTime.now());
            realtimeDataRepository.save(data);
            logger.info("实时数据保存到数据库: symbol={}", symbol);
        } catch (Exception e) {
            logger.error("保存Binance实时数据失败: symbol={}, error={}", symbol, e.getMessage(), e);
            throw new RuntimeException("Failed to save realtime data to Binance", e);
        }
    }

    @Override
    public void saveDepthData(String symbol, String bids, String asks, long timestamp) {
        logger.debug("保存Binance深度数据到缓存: symbol={}, timestamp={}", symbol, timestamp);
        try {
            DepthData data = new DepthData();
            data.setSymbol(symbol);
            data.setBids(bids);
            data.setAsks(asks);
            data.setTimestamp(timestamp);
            data.setExchange("binance");
            data.setCreatedAt(LocalDateTime.now());
            latestDepthDataCache.put(symbol, data);
        } catch (Exception e) {
            logger.error("保存Binance深度数据到缓存失败: symbol={}, error={}", symbol, e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 2000)
    public void persistCachedData() {
        if (latestDepthDataCache.isEmpty()) {
            return;
        }
        
        List<DepthData> batchToSave = new ArrayList<>(latestDepthDataCache.values());
        latestDepthDataCache.clear();

        try {
            clickHouseService.saveDepthData(batchToSave);
            logger.info("成功从缓存中批量保存 {} 条Binance深度数据到ClickHouse。", batchToSave.size());
        } catch (Exception e) {
            logger.error("从缓存批量保存Binance深度数据失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void startWebSocket() {
        logger.info("启动Binance WebSocket连接: {}", wsUrl);
        Request request = new Request.Builder().url(wsUrl).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, okhttp3.Response response) {
                logger.info("Binance WebSocket连接已打开");
                updateDepthSubscriptions();
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                logger.debug("收到Binance WebSocket消息: {}", text);
                try {
                    JsonNode json = mapper.readTree(text);
                    if (json.has("stream") && json.get("stream").asText().contains("depth")) {
                        JsonNode data = json.get("data");
                        String symbol = data.get("s").asText();
                        long timestamp = data.get("E").asLong();
                        String bids = data.get("b").toString();
                        String asks = data.get("a").toString();
                        saveDepthData(symbol, bids, asks, timestamp);
                        webSocketEndpoint.broadcastDepthData(symbol, bids, asks, timestamp, "binance");
                    }
                } catch (Exception e) {
                    logger.error("处理Binance WebSocket消息失败: {}", e.getMessage(), e);
                }
            }

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                logger.info("Binance WebSocket连接正在关闭: code={}, reason={}", code, reason);
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, okhttp3.Response response) {
                logger.error("Binance WebSocket连接失败: {}", t.getMessage(), t);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.error("重连等待被中断: {}", e.getMessage(), e);
                }
                startWebSocket();
            }
        });
    }

    @Override
    public void updateDepthSubscriptions() {
        logger.info("更新Binance深度订阅");
        try {
            List<Subscription> activeSubscriptions = subscriptionService.getSubscriptionsByDataTypeAndExchange("depth", "binance");
            Set<String> newSymbols = activeSubscriptions.stream()
                .map(Subscription::getSymbol)
                .collect(Collectors.toSet());

            // 找出需要取消订阅的
            Set<String> symbolsToUnsubscribe = new HashSet<>(subscribedDepthSymbols);
            symbolsToUnsubscribe.removeAll(newSymbols);

            // 找出需要新增订阅的
            Set<String> symbolsToSubscribe = new HashSet<>(newSymbols);
            symbolsToSubscribe.removeAll(subscribedDepthSymbols);

            if (!symbolsToUnsubscribe.isEmpty()) {
                String unsubscribeMessage = createSubscriptionMessage(new ArrayList<>(symbolsToUnsubscribe), "UNSUBSCRIBE");
                if (webSocket != null) {
                    webSocket.send(unsubscribeMessage);
                    subscribedDepthSymbols.removeAll(symbolsToUnsubscribe);
                    logger.info("已发送Binance深度取消订阅请求: {}", symbolsToUnsubscribe);
                }
            }

            if (!symbolsToSubscribe.isEmpty()) {
                String subscriptionMessage = createSubscriptionMessage(new ArrayList<>(symbolsToSubscribe), "SUBSCRIBE");
                if (webSocket != null) {
                    webSocket.send(subscriptionMessage);
                    subscribedDepthSymbols.addAll(symbolsToSubscribe);
                    logger.info("已发送Binance深度订阅请求: {}", symbolsToSubscribe);
                } else {
                    logger.warn("Binance WebSocket未连接，无法发送订阅请求");
                }
            }
        } catch (Exception e) {
            logger.error("更新Binance深度订阅失败: {}", e.getMessage(), e);
        }
    }

    private String createSubscriptionMessage(List<String> symbols, String method) {
        Map<String, Object> message = new HashMap<>();
        message.put("method", method);
        List<String> params = symbols.stream()
                .map(s -> s.toLowerCase() + "@depth10@100ms")
                .collect(Collectors.toList());
        message.put("params", params);
        message.put("id", UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        try {
            return mapper.writeValueAsString(message);
        } catch (Exception e) {
            logger.error("创建Binance订阅消息失败: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create subscription message for Binance", e);
        }
    }

    private OkHttpClient createHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (proxyEnabled && proxyHost != null && proxyPort > 0) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            builder.proxy(proxy);
            logger.info("使用代理设置创建OkHttpClient: host={}, port={}", proxyHost, proxyPort);
        } else {
            logger.info("不使用代理设置创建OkHttpClient");
        }
        return builder.build();
    }

    @Override
    public List<CryptoMetadata> getInstrumentsByType(String instType) {
        logger.info("获取Binance合约信息 by type: {}", instType);
        List<CryptoMetadata> allInstruments = metadataRepository.findAll();
        List<CryptoMetadata> filteredInstruments = new ArrayList<>();
        if ("SWAP".equalsIgnoreCase(instType)) {
            // 如果前端传递的是SWAP，则返回所有非现货(SPOT)的合约
            for (CryptoMetadata metadata : allInstruments) {
                if (metadata.getExchange().equals("binance") && !metadata.getInstType().equals("SPOT")) {
                    filteredInstruments.add(metadata);
                }
            }
        } else {
            // 否则按指定类型查询
            for (CryptoMetadata metadata : allInstruments) {
                if (metadata.getExchange().equals("binance") && metadata.getInstType().equals(instType)) {
                    filteredInstruments.add(metadata);
                }
            }
        }
        return filteredInstruments;
    }
}