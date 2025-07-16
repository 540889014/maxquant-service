package com.example.crypto.controller;

import com.example.crypto.dto.BacktestReportDto;
import com.example.crypto.exception.ResourceNotFoundException;
import com.example.crypto.models.ApiResponse;
import com.example.crypto.service.BacktestReportService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/backtest-reports")
public class BacktestReportController {

    private final BacktestReportService backtestReportService;

    public BacktestReportController(BacktestReportService backtestReportService) {
        this.backtestReportService = backtestReportService;
    }

    @GetMapping("/{strategyName}")
    public ApiResponse<List<String>> getAvailableTimestamps(@PathVariable String strategyName) {
        try {
            List<String> timestamps = backtestReportService.getAvailableTimestamps(strategyName);
            return ApiResponse.ok(timestamps);
        } catch (IOException e) {
            return ApiResponse.fail(500, "Failed to get available timestamps.");
        }
    }

    @GetMapping("/{strategyName}/{timestamp}")
    public ApiResponse<BacktestReportDto> getBacktestReport(
            @PathVariable String strategyName,
            @PathVariable String timestamp) {
        try {
            BacktestReportDto report = backtestReportService.getBacktestReport(strategyName, timestamp);
            return ApiResponse.ok(report);
        } catch (IOException e) {
            return ApiResponse.fail(500, "Failed to get backtest report.");
        }
    }

    @GetMapping(value = "/{strategyName}/{timestamp}/log", produces = "text/plain;charset=UTF-8")
    public ApiResponse<String> getBacktestLog(
            @PathVariable String strategyName,
            @PathVariable String timestamp) {
        try {
            String logContent = backtestReportService.getBacktestLog(strategyName, timestamp);
            return ApiResponse.ok(logContent);
        } catch (IOException e) {
            return ApiResponse.fail(500, "Error reading log file: " + e.getMessage());
        }
    }

    @DeleteMapping("/{strategyName}/{timestamp}")
    public ApiResponse<String> deleteBacktestReport(
            @PathVariable String strategyName,
            @PathVariable String timestamp) {
        try {
            backtestReportService.deleteBacktestReport(strategyName, timestamp);
            return ApiResponse.ok("Backtest report deleted successfully: " + strategyName + "/" + timestamp);
        } catch (ResourceNotFoundException e) {
            return ApiResponse.fail(404, "Backtest report not found: " + e.getMessage());
        } catch (IOException e) {
            return ApiResponse.fail(500, "Error deleting backtest report: " + e.getMessage());
        }
    }
} 