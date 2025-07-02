package com.example.crypto.dto;

import lombok.Data;

@Data
public class BacktestPattern {
    private BacktestPatternEnum patternName;
    private PatternParams patternParams;
} 