package com.example.crypto.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class KlineDataDTO {
    private Long id;
    private String symbol;
    private String timeframe;
    private String timestamp;
    private Double openPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double closePrice;
    private Double volume;
    private LocalDateTime createdAt;

    public KlineDataDTO(Long id, String symbol, String timeframe, Long timestamp, Double openPrice, Double highPrice, Double lowPrice, Double closePrice, Double volume, LocalDateTime createdAt) {
        this.id = id;
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.timestamp = Instant.ofEpochMilli(timestamp).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT);
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public Double getOpenPrice() { return openPrice; }
    public void setOpenPrice(Double openPrice) { this.openPrice = openPrice; }
    public Double getHighPrice() { return highPrice; }
    public void setHighPrice(Double highPrice) { this.highPrice = highPrice; }
    public Double getLowPrice() { return lowPrice; }
    public void setLowPrice(Double lowPrice) { this.lowPrice = lowPrice; }
    public Double getClosePrice() { return closePrice; }
    public void setClosePrice(Double closePrice) { this.closePrice = closePrice; }
    public Double getVolume() { return volume; }
    public void setVolume(Double volume) { this.volume = volume; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 