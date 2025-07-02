package com.example.crypto.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StrategyTemplateDetailDto extends StrategyTemplateDto {
    private String script;
} 