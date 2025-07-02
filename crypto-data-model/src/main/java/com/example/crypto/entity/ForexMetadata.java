package com.example.crypto.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "forex_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForexMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String symbol; // e.g., "EUR/USD"

    @Column(nullable = false)
    private String baseCurrency; // e.g., "EUR"

    @Column(nullable = false)
    private String quoteCurrency; // e.g., "USD"

    @Column(nullable = false)
    private String baseCurrencyName; // e.g., "欧元"

    @Column(nullable = false)
    private String quoteCurrencyName; // e.g., "美元"

    @Column(length = 512)
    private String description;
} 