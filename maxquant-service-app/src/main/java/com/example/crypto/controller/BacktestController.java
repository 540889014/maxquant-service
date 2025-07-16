package com.example.crypto.controller;

import com.example.crypto.dto.BacktestRunRequest;
import com.example.crypto.models.ApiResponse;
import com.example.crypto.service.BacktestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backtest")
public class BacktestController {

    private static final Logger logger = LoggerFactory.getLogger(BacktestController.class);
    private final BacktestService backtestService;

    public BacktestController(BacktestService backtestService) {
        this.backtestService = backtestService;
    }

    @PostMapping("/run")
    public ApiResponse<Map<String, String>> runBacktest(@RequestBody BacktestRunRequest request, @RequestHeader("Authorization") String authorizationHeader) {
        try {
            String backtestId = UUID.randomUUID().toString();
            logger.info("Received backtest request, assigning ID: {}", backtestId);
            logger.info("Backtest request details: {}", request);
            
            // The header includes "Bearer ", remove it to get the raw token.
            String token = authorizationHeader.replace("Bearer ", "");

            backtestService.runBacktest(backtestId, request, token);
            return ApiResponse.ok(Map.of("backtestId", backtestId));
        } catch (Exception e) {
            logger.error("Error initiating backtest", e);
            return ApiResponse.fail(400, "Error initiating backtest: " + e.getMessage());
        }
    }
} 