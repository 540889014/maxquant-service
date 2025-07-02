package com.example.crypto.service;

public interface WebSocketMessageService {
    void broadcastDepthUpdate(String symbol, Object data);
    void broadcastRealtimeUpdate(String symbol, Object data);
} 