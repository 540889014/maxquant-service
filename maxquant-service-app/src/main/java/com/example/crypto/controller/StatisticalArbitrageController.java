package com.example.crypto.controller;

import com.example.crypto.models.ApiResponse;
import com.example.crypto.service.StatisticalArbitrageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统计套利研究平台控制器
 * 处理ADF检验、KPSS检验和Hurst指数计算请求
 */
@RestController
@RequestMapping("/api/v1/statistical-arbitrage")
public class StatisticalArbitrageController {
    private static final Logger logger = LoggerFactory.getLogger(StatisticalArbitrageController.class);
    private final StatisticalArbitrageService statisticalArbitrageService;

    public StatisticalArbitrageController(StatisticalArbitrageService statisticalArbitrageService) {
        this.statisticalArbitrageService = statisticalArbitrageService;
    }

    @GetMapping("/adf-test")
    public ApiResponse<List<Map<String, Object>>> getAdfTestResults(@RequestParam String timeframe, @RequestParam String exchange) {
        logger.info("请求ADF检验结果: timeframe={}, exchange={}", timeframe, exchange);
        try {
            List<Map<String, Object>> results = statisticalArbitrageService.performAdfTest(timeframe, exchange);
            return ApiResponse.ok(results);
        } catch (Exception e) {
            logger.error("获取ADF检验结果失败: error={}", e.getMessage(), e);
            return ApiResponse.fail(500, "获取ADF检验结果失败: " + e.getMessage());
        }
    }

    @GetMapping("/kpss-test")
    public ApiResponse<List<Map<String, Object>>> getKpssTestResults(@RequestParam String timeframe, @RequestParam String exchange) {
        logger.info("请求KPSS检验结果: timeframe={}, exchange={}", timeframe, exchange);
        try {
            List<Map<String, Object>> results = statisticalArbitrageService.performKpssTest(timeframe, exchange);
            return ApiResponse.ok(results);
        } catch (Exception e) {
            logger.error("获取KPSS检验结果失败: error={}", e.getMessage(), e);
            return ApiResponse.fail(500, "获取KPSS检验结果失败: " + e.getMessage());
        }
    }

    @GetMapping("/hurst-exponent")
    public ApiResponse<List<Map<String, Object>>> getHurstExponentResults(@RequestParam String timeframe, @RequestParam String exchange) {
        logger.info("请求Hurst指数结果: timeframe={}, exchange={}", timeframe, exchange);
        try {
            List<Map<String, Object>> results = statisticalArbitrageService.performHurstExponentCalculation(timeframe, exchange);
            return ApiResponse.ok(results);
        } catch (Exception e) {
            logger.error("获取Hurst指数结果失败: error={}", e.getMessage(), e);
            return ApiResponse.fail(500, "获取Hurst指数结果失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/recalculate")
    public ApiResponse<String> recalculateIndices(@RequestParam String timeframe, @RequestParam String exchange) {
        logger.info("请求重新计算统计套利指标: timeframe={}, exchange={}", timeframe, exchange);
        try {
            statisticalArbitrageService.recalculateIndices(timeframe, exchange);
            return ApiResponse.ok("统计套利指标重新计算成功");
        } catch (Exception e) {
            logger.error("重新计算统计套利指标失败: error={}", e.getMessage(), e);
            return ApiResponse.fail(500, "重新计算统计套利指标失败: " + e.getMessage());
        }
    }
} 