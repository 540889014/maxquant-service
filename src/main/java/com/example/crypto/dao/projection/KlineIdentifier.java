package com.example.crypto.dao.projection;

public interface KlineIdentifier {
    String getSymbol();
    String getTimeframe();
    String getExchange();
} 