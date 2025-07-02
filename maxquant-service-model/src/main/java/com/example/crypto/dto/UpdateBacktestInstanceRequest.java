package com.example.crypto.dto;

import lombok.Data;

@Data
public class UpdateBacktestInstanceRequest {
    private String name;
    private Long strategyTemplateId;
    private String params; // JSON string
} 