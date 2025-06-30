package com.example.crypto.entity;

import com.example.crypto.enums.BacktestStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "backtest_instances")
public class BacktestInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long userId;

    private Long strategyTemplateId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String params;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BacktestStatus status = BacktestStatus.NOT_RUN;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
} 