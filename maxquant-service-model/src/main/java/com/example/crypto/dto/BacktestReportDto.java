package com.example.crypto.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BacktestReportDto {

    private List<Map<String, String>> performance;
    private List<Map<String, String>> orders;
    private List<Map<String, String>> trades;
    private List<Map<String, String>> periodicPerformance;
    private List<Map<String, String>> dailyIndicators;
    private List<Map<String, String>> portfolioDetails;
} 