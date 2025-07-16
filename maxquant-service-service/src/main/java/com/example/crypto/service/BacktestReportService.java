package com.example.crypto.service;

import com.example.crypto.dto.BacktestReportDto;

import java.io.IOException;
import java.util.List;

public interface BacktestReportService {

    BacktestReportDto getBacktestReport(String strategyName, String timestamp) throws IOException;

    String getBacktestLog(String strategyName, String timestamp) throws IOException;

    List<String> getAvailableTimestamps(String strategyName) throws IOException;
    
    void deleteBacktestReport(String strategyName, String timestamp) throws IOException;
    
    void deleteBacktestReportsByStrategyName(String strategyName) throws IOException;
} 