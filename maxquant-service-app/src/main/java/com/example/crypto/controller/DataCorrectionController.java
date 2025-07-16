package com.example.crypto.controller;

import com.example.crypto.models.ApiResponse;
import com.example.crypto.service.DataCorrectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/data-correction")
public class DataCorrectionController {

    private final DataCorrectionService dataCorrectionService;

    public DataCorrectionController(DataCorrectionService dataCorrectionService) {
        this.dataCorrectionService = dataCorrectionService;
    }

    @GetMapping("/kline/fix-close-prices")
    public ApiResponse<String> fixKlineClosePrices() {
        try {
            // 异步执行，避免长时间阻塞HTTP请求
            new Thread(() -> dataCorrectionService.correctKlineClosePrices()).start();
            return ApiResponse.ok("K-line close price correction process has been started in the background.");
        } catch (Exception e) {
            return ApiResponse.fail(500, "Failed to start K-line close price correction: " + e.getMessage());
        }
    }
} 