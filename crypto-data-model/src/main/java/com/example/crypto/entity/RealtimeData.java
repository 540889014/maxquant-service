package com.example.crypto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * リアルタイムデータエンティティ
 * 最新の市場データを格納
 */
@Entity
@Table(name = "realtime_data")
public class RealtimeData {
    @Id
    private String id;
    private String symbol;
    private Double price;
    private Long timestamp;
    private LocalDateTime createdAt;
    private String exchange;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
}