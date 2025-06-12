package com.example.crypto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 深度データエンティティ
 * 市場の買い注文と売り注文のデータを格納
 */
@Entity
@Table(name = "depth_data")
public class DepthData {
    @Id
    private String id;
    private String symbol;
    private String bids;
    private String asks;
    private Long timestamp;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getBids() { return bids; }
    public void setBids(String bids) { this.bids = bids; }
    public String getAsks() { return asks; }
    public void setAsks(String asks) { this.asks = asks; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}