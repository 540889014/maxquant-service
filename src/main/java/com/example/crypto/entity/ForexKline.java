package com.example.crypto.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "forex_kline", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"symbol", "period", "timestamp"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForexKline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String period;

    @Column(nullable = false)
    private Long timestamp;

    @Column(precision = 18, scale = 8)
    private BigDecimal open;

    @Column(precision = 18, scale = 8)
    private BigDecimal high;

    @Column(precision = 18, scale = 8)
    private BigDecimal low;

    @Column(precision = 18, scale = 8)
    private BigDecimal close;
} 