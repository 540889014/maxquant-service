package com.example.crypto.dto;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

public record BacktestRunRequest(
        @JsonProperty("strategyCode") String strategyCode,
        @JsonProperty("SYMBOLS") List<SymbolGroup> symbols,
        @JsonProperty("BACKTEST") BacktestSection backtest,
        @JsonProperty("DATA_TYPE") DataTypeSection dataType,
        @JsonProperty("PARAMS") Map<String, Object> params,
        @JsonProperty("LOG_LEVEL") String logLevel
) {
    public record BacktestSection(@JsonProperty("OUTPUT_REPORT") boolean outputReport, @JsonProperty("START_TIME") long startTime, @JsonProperty("END_TIME") long endTime, @JsonProperty("BACKTEST_PATTERN") BacktestPatternSection backtestPattern) {}
    public record BacktestPatternSection(@JsonProperty("PATTERN_NAME") String patternName, @JsonProperty("PATTERN_PARAMS") PatternParamsSection patternParams) {}
    public record PatternParamsSection(@JsonProperty("miss_ratio") double missRatio, @JsonProperty("slippage") double slippage) {}
    public record OhlcConf(@JsonProperty("TYPE") String type, @JsonProperty("TIME_TYPE") String timeType, @JsonProperty("USE") boolean use) {}
    public record SymbolGroup(@JsonProperty("WITHOUT_TIME") List<String> withoutTime) {}
    public record DataTypeSection(@JsonProperty("USE_ORDER_BOOK") boolean useOrderBook, @JsonProperty("USE_TRADE") boolean useTrade, @JsonProperty("OHLC") List<OhlcConf> ohlc) {}
} 