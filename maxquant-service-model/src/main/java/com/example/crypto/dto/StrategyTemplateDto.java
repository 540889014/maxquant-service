package com.example.crypto.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StrategyTemplateDto {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StrategyParameterDto> parameters;
} 