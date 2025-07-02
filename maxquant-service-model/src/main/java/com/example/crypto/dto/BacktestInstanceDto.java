package com.example.crypto.dto;

import com.example.crypto.enums.BacktestStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BacktestInstanceDto {
    private Long id;
    private String name;
    private Long userId;
    private Long strategyTemplateId;
    private String params;
    private BacktestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 