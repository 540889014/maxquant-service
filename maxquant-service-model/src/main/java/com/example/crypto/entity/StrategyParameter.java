package com.example.crypto.entity;

import com.example.crypto.models.ParameterDirection;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "strategy_parameter")
@Data
public class StrategyParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_template_id", nullable = false)
    private StrategyTemplate strategyTemplate;

    @Column(nullable = false)
    private String name;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParameterDirection direction;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 