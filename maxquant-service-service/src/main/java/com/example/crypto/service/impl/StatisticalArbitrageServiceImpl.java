package com.example.crypto.service.impl;

import com.example.crypto.dao.CryptoMetadataRepository;
import com.example.crypto.dao.KlineDataRepository;
import com.example.crypto.dao.StatisticalIndexRepository;
import com.example.crypto.entity.CryptoMetadata;
import com.example.crypto.entity.KlineData;
import com.example.crypto.entity.StatisticalIndex;
import com.example.crypto.service.StatisticalArbitrageService;
import com.example.crypto.service.SubscriptionService;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.commons.math3.stat.StatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * 统计套利研究平台服务实现类
 * 实现ADF检验、KPSS检验和Hurst指数计算功能
 */
@Service
public class StatisticalArbitrageServiceImpl implements StatisticalArbitrageService {
    private static final Logger logger = LoggerFactory.getLogger(StatisticalArbitrageServiceImpl.class);
    private final CryptoMetadataRepository metadataRepository;
    private final KlineDataRepository klineDataRepository;
    private final SubscriptionService subscriptionService;
    private final StatisticalIndexRepository statisticalIndexRepository;

    public StatisticalArbitrageServiceImpl(CryptoMetadataRepository metadataRepository,
                                           KlineDataRepository klineDataRepository,
                                           SubscriptionService subscriptionService,
                                           StatisticalIndexRepository statisticalIndexRepository) {
        this.metadataRepository = metadataRepository;
        this.klineDataRepository = klineDataRepository;
        this.subscriptionService = subscriptionService;
        this.statisticalIndexRepository = statisticalIndexRepository;
    }

    @Override
    public List<Map<String, Object>> performAdfTest(String timeframe, String exchange) {
        logger.info("执行ADF检验: timeframe={}, exchange={}", timeframe, exchange);
        List<Map<String, Object>> results = new ArrayList<>();
        LocalDate today = LocalDate.now();
        List<StatisticalIndex> indices = statisticalIndexRepository.findByTimeframeAndExchangeAndCalculationDate(timeframe, exchange, today);
        if (!indices.isEmpty()) {
            for (StatisticalIndex index : indices) {
                if (index.getAdfValue() != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("pair", index.getSymbolPair());
                    result.put("test", "ADF");
                    result.put("value", index.getAdfValue());
                    results.add(result);
                }
            }
            return results;
        }
        // 如果数据库中没有数据，则进行计算
        List<String> symbols = subscriptionService.getActiveSubscriptions(exchange);
        if (symbols.size() < 2) {
            logger.warn("订阅合约数量不足，无法执行ADF检验: symbols={}", symbols.size());
            return results;
        }
        for (int i = 0; i < symbols.size(); i++) {
            for (int j = i + 1; j < symbols.size(); j++) {
                String symbol1 = symbols.get(i);
                String symbol2 = symbols.get(j);
                String pair = symbol1 + " - " + symbol2;
                Map<String, Object> result = new HashMap<>();
                result.put("pair", pair);
                result.put("test", "ADF");
                double adfValue = calculateAdfValue(symbol1, symbol2, timeframe, exchange);
                result.put("value", adfValue);
                results.add(result);
                // 存储到数据库
                saveStatisticalIndex(pair, timeframe, exchange, adfValue, null, null);
            }
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> performKpssTest(String timeframe, String exchange) {
        logger.info("执行KPSS检验: timeframe={}, exchange={}", timeframe, exchange);
        List<Map<String, Object>> results = new ArrayList<>();
        LocalDate today = LocalDate.now();
        List<StatisticalIndex> indices = statisticalIndexRepository.findByTimeframeAndExchangeAndCalculationDate(timeframe, exchange, today);
        if (!indices.isEmpty()) {
            for (StatisticalIndex index : indices) {
                if (index.getKpssValue() != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("pair", index.getSymbolPair());
                    result.put("test", "KPSS");
                    result.put("value", index.getKpssValue());
                    results.add(result);
                }
            }
            return results;
        }
        // 如果数据库中没有数据，则进行计算
        List<String> symbols = subscriptionService.getActiveSubscriptions(exchange);
        if (symbols.size() < 2) {
            logger.warn("订阅合约数量不足，无法执行KPSS检验: symbols={}", symbols.size());
            return results;
        }
        for (int i = 0; i < symbols.size(); i++) {
            for (int j = i + 1; j < symbols.size(); j++) {
                String symbol1 = symbols.get(i);
                String symbol2 = symbols.get(j);
                String pair = symbol1 + " - " + symbol2;
                Map<String, Object> result = new HashMap<>();
                result.put("pair", pair);
                result.put("test", "KPSS");
                double kpssValue = calculateKpssValue(symbol1, symbol2, timeframe, exchange);
                result.put("value", kpssValue);
                results.add(result);
                // 存储到数据库
                saveStatisticalIndex(pair, timeframe, exchange, null, kpssValue, null);
            }
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> performHurstExponentCalculation(String timeframe, String exchange) {
        logger.info("执行Hurst指数计算: timeframe={}, exchange={}", timeframe, exchange);
        List<Map<String, Object>> results = new ArrayList<>();
        LocalDate today = LocalDate.now();
        List<StatisticalIndex> indices = statisticalIndexRepository.findByTimeframeAndExchangeAndCalculationDate(timeframe, exchange, today);
        if (!indices.isEmpty()) {
            for (StatisticalIndex index : indices) {
                if (index.getHurstValue() != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("pair", index.getSymbolPair());
                    result.put("test", "Hurst Exponent");
                    result.put("value", index.getHurstValue());
                    results.add(result);
                }
            }
            return results;
        }
        // 如果数据库中没有数据，则进行计算
        List<String> symbols = subscriptionService.getActiveSubscriptions(exchange);
        if (symbols.size() < 2) {
            logger.warn("订阅合约数量不足，无法执行Hurst指数计算: symbols={}", symbols.size());
            return results;
        }
        for (int i = 0; i < symbols.size(); i++) {
            for (int j = i + 1; j < symbols.size(); j++) {
                String symbol1 = symbols.get(i);
                String symbol2 = symbols.get(j);
                String pair = symbol1 + " - " + symbol2;
                Map<String, Object> result = new HashMap<>();
                result.put("pair", pair);
                result.put("test", "Hurst Exponent");
                double hurstValue = calculateHurstExponent(symbol1, symbol2, timeframe, exchange);
                result.put("value", hurstValue);
                results.add(result);
                // 存储到数据库
                saveStatisticalIndex(pair, timeframe, exchange, null, null, hurstValue);
            }
        }
        return results;
    }

    private double calculateAdfValue(String symbol1, String symbol2, String timeframe, String exchange) {
        // 设置时间范围为过去一个月
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (30L * 24 * 60 * 60 * 1000); // 过去30天

        // 获取两个合约的收盘价数据
        List<KlineData> data1 = klineDataRepository.findBySymbolAndTimeframeAndTimestampBetweenAndExchange(symbol1, timeframe, startTime, endTime, exchange);
        List<KlineData> data2 = klineDataRepository.findBySymbolAndTimeframeAndTimestampBetweenAndExchange(symbol2, timeframe, startTime, endTime, exchange);
        if (data1.isEmpty() || data2.isEmpty()) {
            logger.warn("数据不足，无法计算ADF值: symbol1={}, symbol2={}", symbol1, symbol2);
            return 0.0;
        }

        // 按照时间对齐数据
        List<KlineData> alignedData1 = new ArrayList<>();
        List<KlineData> alignedData2 = new ArrayList<>();
        alignDataByTimestamp(data1, data2, alignedData1, alignedData2);

        if (alignedData1.isEmpty() || alignedData2.isEmpty()) {
            logger.warn("数据时间不相交，无法计算ADF值: symbol1={}, symbol2={}", symbol1, symbol2);
            return 0.0;
        }

        // 提取收盘价
        double[] prices1 = alignedData1.stream().mapToDouble(KlineData::getClosePrice).toArray();
        double[] prices2 = alignedData2.stream().mapToDouble(KlineData::getClosePrice).toArray();

        // 确保数据长度一致
        int length = Math.min(prices1.length, prices2.length);
        if (length < 2) {
            logger.warn("数据长度不足，无法计算ADF值: length={}", length);
            return 0.0;
        }

        // 回归分析
        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        double[][] x = new double[length][1];
        for (int i = 0; i < length; i++) {
            x[i][0] = prices2[i];
        }
        regression.newSampleData(prices1, x);
        double[] coefficients = regression.estimateRegressionParameters();
        double[] residuals = new double[length];
        for (int i = 0; i < length; i++) {
            residuals[i] = prices1[i] - coefficients[0] - coefficients[1] * prices2[i];
        }

        // 计算残差的一阶差分
        double[] diffResiduals = new double[length - 1];
        for (int i = 1; i < length; i++) {
            diffResiduals[i - 1] = residuals[i] - residuals[i - 1];
        }

        // 对残差进行回归分析以计算ADF统计量
        OLSMultipleLinearRegression adfRegression = new OLSMultipleLinearRegression();
        double[][] adfX = new double[length - 1][1];
        for (int i = 0; i < length - 1; i++) {
            adfX[i][0] = residuals[i];
        }
        adfRegression.newSampleData(diffResiduals, adfX);
        double[] adfCoefficients = adfRegression.estimateRegressionParameters();
        double adfStatistic = adfCoefficients[1] / adfRegression.estimateRegressionParametersStandardErrors()[1];

        return adfStatistic;
    }

    private double calculateKpssValue(String symbol1, String symbol2, String timeframe, String exchange) {
        // 设置时间范围为过去一个月
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (30L * 24 * 60 * 60 * 1000); // 过去30天

        // 获取两个合约的收盘价数据
        List<KlineData> data1 = klineDataRepository.findBySymbolAndTimeframeAndTimestampBetweenAndExchange(symbol1, timeframe, startTime, endTime, exchange);
        List<KlineData> data2 = klineDataRepository.findBySymbolAndTimeframeAndTimestampBetweenAndExchange(symbol2, timeframe, startTime, endTime, exchange);
        if (data1.isEmpty() || data2.isEmpty()) {
            logger.warn("数据不足，无法计算KPSS值: symbol1={}, symbol2={}", symbol1, symbol2);
            return Double.NaN;
        }

        // 按照时间对齐数据
        List<KlineData> alignedData1 = new ArrayList<>();
        List<KlineData> alignedData2 = new ArrayList<>();
        alignDataByTimestamp(data1, data2, alignedData1, alignedData2);

        if (alignedData1.size() < 20) { // 需要足够的数据点
            logger.warn("对齐后的数据点过少，无法计算KPSS值: size={}", alignedData1.size());
            return Double.NaN;
        }

        // 1. 计算价比价差序列 (P_A / P_B - 1)
        double[] spread = new double[alignedData1.size()];
        for (int i = 0; i < alignedData1.size(); i++) {
            double price1 = alignedData1.get(i).getClosePrice();
            double price2 = alignedData2.get(i).getClosePrice();
            if (price2 == 0) {
                logger.warn("除数为零，无法计算价差: symbol2={}, timestamp={}", symbol2, alignedData2.get(i).getTimestamp());
                return Double.NaN; // 或者其他错误处理
            }
            spread[i] = price1 / price2 - 1;
        }

        int n = spread.length;

        // 2. 去均值: e_t = Spread_t - mean(Spread)
        double meanSpread = StatUtils.mean(spread);
        double[] residuals = new double[n];
        for (int i = 0; i < n; i++) {
            residuals[i] = spread[i] - meanSpread;
        }

        // 3. 计算累积和: S_t = Sum of e_i from 1 to t
        double[] cumulativeSum = new double[n];
        cumulativeSum[0] = residuals[0];
        for (int i = 1; i < n; i++) {
            cumulativeSum[i] = cumulativeSum[i - 1] + residuals[i];
        }

        // 4. 计算 KPSS 统计量的分子部分
        double sumOfSquares = 0.0;
        for (double cs : cumulativeSum) {
            sumOfSquares += cs * cs;
        }

        // 5. 估计长期方差 (Newey-West)
        double longRunVariance = calculateNeweyWestVariance(residuals);
        if (longRunVariance <= 0) {
             logger.warn("长期方差计算结果非正，无法计算KPSS统计量. 使用普通方差作为备用。");
             longRunVariance = StatUtils.variance(residuals); // Fallback
             if(longRunVariance <= 0) return Double.NaN;
        }


        // 6. 计算最终的 KPSS 统计量
        double kpssStatistic = (sumOfSquares / (n * n)) / longRunVariance;

        return kpssStatistic;
    }

    /**
     * 使用 Newey-West 估计器计算长期方差.
     * @param series 输入序列 (已经去均值)
     * @return 长期方差
     */
    private double calculateNeweyWestVariance(double[] series) {
        int n = series.length;
        // 自动选择滞后阶数 l, floor(4 * (T/100)^(2/9)) 是常用选择
        int lag = (int) Math.floor(4 * Math.pow((double) n / 100.0, 2.0 / 9.0));

        // gamma_0: 0阶自协方差 (即方差)
        double longRunVariance = autocovariance(series, 0);

        // 加上加权的 j 阶自协方差 (j from 1 to lag)
        for (int j = 1; j <= lag; j++) {
            double bartlettWeight = 1.0 - ((double) j / (lag + 1.0));
            double cov = autocovariance(series, j);
            longRunVariance += 2 * bartlettWeight * cov;
        }
        return longRunVariance;
    }

    /**
     * 计算序列在给定滞后阶数下的自协方差.
     * @param series 输入序列 (已经去均值)
     * @param lag 滞后阶数
     * @return 自协方差
     */
    private double autocovariance(double[] series, int lag) {
        double sum = 0;
        int n = series.length;
        if (lag < 0 || lag >= n) {
            throw new IllegalArgumentException("Lag must be non-negative and less than series length.");
        }
        // 因为序列已经去均值, E[X_t] = 0.
        // Cov(X_t, X_{t-k}) = E[X_t * X_{t-k}]
        for (int t = lag; t < n; t++) {
            sum += series[t] * series[t - lag];
        }
        return sum / n;
    }

    private double calculateHurstExponent(String symbol1, String symbol2, String timeframe, String exchange) {
        // 设置时间范围为过去一个月
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (30L * 24 * 60 * 60 * 1000); // 过去30天

        // 获取两个合约的收盘价数据
        List<KlineData> data1 = klineDataRepository.findBySymbolAndTimeframeAndTimestampBetweenAndExchange(symbol1, timeframe, startTime, endTime, exchange);
        List<KlineData> data2 = klineDataRepository.findBySymbolAndTimeframeAndTimestampBetweenAndExchange(symbol2, timeframe, startTime, endTime, exchange);
        if (data1.isEmpty() || data2.isEmpty()) {
            logger.warn("数据不足，无法计算Hurst指数: symbol1={}, symbol2={}", symbol1, symbol2);
            return 0.0;
        }

        // 按照时间对齐数据
        List<KlineData> alignedData1 = new ArrayList<>();
        List<KlineData> alignedData2 = new ArrayList<>();
        alignDataByTimestamp(data1, data2, alignedData1, alignedData2);

        if (alignedData1.isEmpty() || alignedData2.isEmpty()) {
            logger.warn("数据时间不相交，无法计算Hurst指数: symbol1={}, symbol2={}", symbol1, symbol2);
            return 0.0;
        }

        // 提取收盘价
        double[] prices1 = alignedData1.stream().mapToDouble(KlineData::getClosePrice).toArray();
        double[] prices2 = alignedData2.stream().mapToDouble(KlineData::getClosePrice).toArray();

        // 确保数据长度一致
        int length = Math.min(prices1.length, prices2.length);
        if (length < 2) {
            logger.warn("数据长度不足，无法计算Hurst指数: length={}", length);
            return 0.0;
        }

        // 计算价差
        double[] spread = new double[length];
        for (int i = 0; i < length; i++) {
            spread[i] = prices1[i] - prices2[i];
        }

        // 计算Hurst指数
        int maxLag = length / 2;
        double[] rsValues = new double[maxLag];
        double[] logLags = new double[maxLag];
        for (int lag = 1; lag <= maxLag; lag++) {
            double[] subSeries = new double[length - lag];
            for (int i = 0; i < length - lag; i++) {
                subSeries[i] = spread[i + lag] - spread[i];
            }
            double mean = StatUtils.mean(subSeries);
            double[] deviations = new double[subSeries.length];
            for (int i = 0; i < subSeries.length; i++) {
                deviations[i] = subSeries[i] - mean;
            }
            double[] cumulativeDeviations = new double[subSeries.length];
            cumulativeDeviations[0] = deviations[0];
            for (int i = 1; i < subSeries.length; i++) {
                cumulativeDeviations[i] = cumulativeDeviations[i - 1] + deviations[i];
            }
            double range = StatUtils.max(cumulativeDeviations) - StatUtils.min(cumulativeDeviations);
            double stdDev = Math.sqrt(StatUtils.variance(subSeries));
            rsValues[lag - 1] = range / stdDev;
            logLags[lag - 1] = Math.log(lag);
        }

        // 使用对数变换后的R/S值进行回归分析
        double[] logRsValues = new double[rsValues.length];
        for (int i = 0; i < rsValues.length; i++) {
            logRsValues[i] = Math.log(rsValues[i]);
        }

        OLSMultipleLinearRegression hurstRegression = new OLSMultipleLinearRegression();
        double[][] hurstX = new double[logLags.length][1];
        for (int i = 0; i < logLags.length; i++) {
            hurstX[i][0] = logLags[i];
        }
        hurstRegression.newSampleData(logRsValues, hurstX);
        double hurstExponent = hurstRegression.estimateRegressionParameters()[1];

        // 确保Hurst指数在合理范围内
        hurstExponent = Math.max(0.0, Math.min(1.0, hurstExponent));

        return hurstExponent;
    }

    // 添加辅助方法以按照时间对齐数据
    private void alignDataByTimestamp(List<KlineData> data1, List<KlineData> data2, List<KlineData> alignedData1, List<KlineData> alignedData2) {
        Map<Long, KlineData> data1Map = new HashMap<>();
        Map<Long, KlineData> data2Map = new HashMap<>();

        for (KlineData kline : data1) {
            data1Map.put(kline.getTimestamp(), kline);
        }

        for (KlineData kline : data2) {
            data2Map.put(kline.getTimestamp(), kline);
        }

        Set<Long> commonTimestamps = new HashSet<>(data1Map.keySet());
        commonTimestamps.retainAll(data2Map.keySet());

        for (Long timestamp : commonTimestamps) {
            alignedData1.add(data1Map.get(timestamp));
            alignedData2.add(data2Map.get(timestamp));
        }

        // 按时间戳排序
        alignedData1.sort(Comparator.comparingLong(KlineData::getTimestamp));
        alignedData2.sort(Comparator.comparingLong(KlineData::getTimestamp));
    }

    @Override
    @Transactional
    public void recalculateIndices(String timeframe, String exchange) {
        logger.info("重新计算统计套利指标: timeframe={}, exchange={}", timeframe, exchange);
        // 清空当前时间框架和交易所的旧数据
        LocalDate today = LocalDate.now();
        statisticalIndexRepository.deleteByTimeframeAndExchangeAndCalculationDate(timeframe, exchange, today);
        
        // 重新计算并存储新的指标数据
        List<String> symbols = subscriptionService.getActiveSubscriptions(exchange);
        if (symbols.size() < 2) {
            logger.warn("订阅合约数量不足，无法重新计算统计套利指标: symbols={}", symbols.size());
            return;
        }
        for (int i = 0; i < symbols.size(); i++) {
            for (int j = i + 1; j < symbols.size(); j++) {
                String symbol1 = symbols.get(i);
                String symbol2 = symbols.get(j);
                String pair = symbol1 + " - " + symbol2;
                
                double adfValue = calculateAdfValue(symbol1, symbol2, timeframe, exchange);
                double kpssValue = calculateKpssValue(symbol1, symbol2, timeframe, exchange);
                double hurstValue = calculateHurstExponent(symbol1, symbol2, timeframe, exchange);
                
                // 存储到数据库
                saveStatisticalIndex(pair, timeframe, exchange, adfValue, kpssValue, hurstValue);
            }
        }
        logger.info("统计套利指标重新计算完成: timeframe={}, exchange={}", timeframe, exchange);
    }

    // 定时任务，每天凌晨更新指数数据
    @Scheduled(cron = "0 0 0 * * ?")
    public void updateIndicesDaily() {
        logger.info("开始执行每日统计指数更新任务");
        List<String> exchanges = Arrays.asList("okx", "binance"); // 假设的交易所列表
        List<String> timeframes = Arrays.asList("1m", "5m", "15m", "1h", "4h", "1d");
        for (String exchange : exchanges) {
            for (String timeframe : timeframes) {
                recalculateIndices(timeframe, exchange);
            }
        }
        logger.info("每日统计指数更新任务完成");
    }

    // 保存指数数据到数据库
    private void saveStatisticalIndex(String symbolPair, String timeframe, String exchange, Double adfValue, Double kpssValue, Double hurstValue) {
        StatisticalIndex index = statisticalIndexRepository.findBySymbolPairAndTimeframeAndExchangeAndCalculationDate(symbolPair, timeframe, exchange, LocalDate.now());
        if (index == null) {
            index = new StatisticalIndex();
            index.setSymbolPair(symbolPair);
            index.setTimeframe(timeframe);
            index.setExchange(exchange);
            index.setCalculationDate(LocalDate.now());
        }
        if (adfValue != null) {
            index.setAdfValue(adfValue);
        }
        if (kpssValue != null) {
            index.setKpssValue(kpssValue);
        }
        if (hurstValue != null) {
            index.setHurstValue(hurstValue);
        }
        statisticalIndexRepository.save(index);
    }
} 