package com.example.crypto.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 訂閱エンティティ
 * ユーザの訂閱情報をデータベースに保存
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @Column(name = "inst_type", nullable = false)
    private String instType;

    @Column(name = "timeframe") // OHLC 行情の時間枠
    private String timeframe;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private String exchange;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public String getInstType() { return instType; }
    public void setInstType(String instType) { this.instType = instType; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
}