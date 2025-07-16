package com.example.crypto.service.impl;

import com.example.crypto.dto.BacktestReportDto;
import com.example.crypto.exception.ResourceNotFoundException;
import com.example.crypto.service.BacktestReportService;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BacktestReportServiceImpl implements BacktestReportService {

    @Value("${backtest.results.path}")
    private String backtestResultsPath;

    @Override
    public BacktestReportDto getBacktestReport(String strategyName, String timestamp) throws IOException {
        BacktestReportDto reportDto = new BacktestReportDto();

        String basePath = Paths.get(backtestResultsPath, strategyName, timestamp).toString();

        try {
            reportDto.setPerformance(readCsvToMap(Paths.get(basePath, "all_performance.csv").toString()));
            reportDto.setOrders(readCsvToMap(Paths.get(basePath, "all_orders.csv").toString()));
            reportDto.setTrades(readCsvToMap(Paths.get(basePath, "all_trade_record.csv").toString()));
            reportDto.setPeriodicPerformance(readCsvToMap(Paths.get(basePath, "all_periodic_performance.csv").toString()));
            reportDto.setDailyIndicators(readCsvToMap(Paths.get(basePath, "all_process_daily_indicators.csv").toString()));
            reportDto.setPortfolioDetails(readCsvToMap(Paths.get(basePath, "all_backtest_detail.csv").toString()));
        } catch (CsvException e) {
            throw new IOException("Error parsing CSV file", e);
        }

        return reportDto;
    }

    @Override
    public String getBacktestLog(String strategyName, String timestamp) throws IOException {
        String logFilePath = Paths.get(backtestResultsPath, strategyName, timestamp, "backtest_slog.log").toString();
        return new String(java.nio.file.Files.readAllBytes(Paths.get(logFilePath)));
    }

    @Override
    public List<String> getAvailableTimestamps(String strategyName) throws IOException {
        Path strategyPath = Paths.get(backtestResultsPath, strategyName);
        if (!java.nio.file.Files.exists(strategyPath) || !java.nio.file.Files.isDirectory(strategyPath)) {
            return List.of();
        }

        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(strategyPath)) {
            return stream
                    .filter(java.nio.file.Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted(java.util.Comparator.reverseOrder()) // Newest first
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void deleteBacktestReport(String strategyName, String timestamp) throws IOException {
        Path reportPath = Paths.get(backtestResultsPath, strategyName, timestamp);
        
        if (!Files.exists(reportPath)) {
            throw new ResourceNotFoundException("Backtest report not found: " + strategyName + "/" + timestamp);
        }
        
        if (!Files.isDirectory(reportPath)) {
            throw new IOException("Invalid report path: " + reportPath);
        }
        
        // 递归删除目录及其所有内容
        deleteDirectoryRecursively(reportPath);
    }
    
    @Override
    public void deleteBacktestReportsByStrategyName(String strategyName) throws IOException {
        Path strategyPath = Paths.get(backtestResultsPath, strategyName);

        if (!Files.exists(strategyPath)) {
            // If the directory doesn't exist, we can consider it as successfully deleted.
            return;
        }

        if (!Files.isDirectory(strategyPath)) {
            throw new IOException("Invalid strategy path: " + strategyPath);
        }

        deleteDirectoryRecursively(strategyPath);
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                for (Path child : stream.collect(Collectors.toList())) {
                    deleteDirectoryRecursively(child);
                }
            }
        }
        Files.delete(path);
    }

    private List<Map<String, String>> readCsvToMap(String filePath) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> allRows = reader.readAll();
            if (allRows.isEmpty()) {
                return List.of();
            }
            String[] headers = allRows.get(0);
            return allRows.stream().skip(1).map(row -> {
                Map<String, String> map = new java.util.HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    map.put(headers[i], row.length > i ? row[i] : "");
                }
                return map;
            }).collect(Collectors.toList());
        }
    }
} 