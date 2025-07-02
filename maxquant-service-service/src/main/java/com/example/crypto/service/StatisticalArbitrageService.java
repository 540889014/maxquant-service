package com.example.crypto.service;

import java.util.List;
import java.util.Map;

/**
 * 统计套利研究平台服务接口
 * 提供ADF检验、KPSS检验和Hurst指数计算功能
 */
public interface StatisticalArbitrageService {
    List<Map<String, Object>> performAdfTest(String timeframe, String exchange);
    List<Map<String, Object>> performKpssTest(String timeframe, String exchange);
    List<Map<String, Object>> performHurstExponentCalculation(String timeframe, String exchange);
    void recalculateIndices(String timeframe, String exchange);
} 