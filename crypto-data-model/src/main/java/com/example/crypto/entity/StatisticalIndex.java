package com.example.crypto.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "statistical_indices")
public class StatisticalIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol_pair", nullable = false)
    private String symbolPair;

    @Column(name = "timeframe", nullable = false)
    private String timeframe;

    @Column(name = "exchange", nullable = false)
    private String exchange;

    @Column(name = "adf_value")
    private Double adfValue;

    @Column(name = "kpss_value")
    private Double kpssValue;

    @Column(name = "hurst_value")
    private Double hurstValue;

    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbolPair() {
        return symbolPair;
    }

    public void setSymbolPair(String symbolPair) {
        this.symbolPair = symbolPair;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public Double getAdfValue() {
        return adfValue;
    }

    public void setAdfValue(Double adfValue) {
        this.adfValue = adfValue;
    }

    public Double getKpssValue() {
        return kpssValue;
    }

    public void setKpssValue(Double kpssValue) {
        this.kpssValue = kpssValue;
    }

    public Double getHurstValue() {
        return hurstValue;
    }

    public void setHurstValue(Double hurstValue) {
        this.hurstValue = hurstValue;
    }

    public LocalDate getCalculationDate() {
        return calculationDate;
    }

    public void setCalculationDate(LocalDate calculationDate) {
        this.calculationDate = calculationDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
} 