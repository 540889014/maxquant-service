package com.example.crypto.dto;

import com.example.crypto.models.ParameterDirection;
import lombok.Data;

@Data
public class StrategyParameterDto {
    private Long id;
    private String name;
    private String dataType;
    private ParameterDirection direction;
    private String defaultValue;
} 