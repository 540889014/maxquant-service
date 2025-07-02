package com.example.crypto.controller;

import com.example.crypto.dto.BacktestRunRequest;
import com.example.crypto.service.BacktestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public ResponseEntity<?> runBacktest(@RequestBody BacktestRunRequest request, @RequestHeader("Authorization") String authorizationHeader) {
        try {
            String backtestId = UUID.randomUUID().toString();
            logger.info("Received backtest request, assigning ID: {}", backtestId);
            logger.info("Backtest request details: {}", request);
            
            // The header includes "Bearer ", remove it to get the raw token.
            String token = authorizationHeader.replace("Bearer ", "");

            backtestService.runBacktest(backtestId, request, token);
            return ResponseEntity.ok(Map.of("backtestId", backtestId));
        } catch (Exception e) {
            logger.error("Error initiating backtest", e);
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
} 