package com.example.crypto.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class BacktestRequest {
    private String strategyCode;
    private List<String> symbols;
    private String startDate;
    private String endDate;
    private String dataType;
    private String timeframe;
    private Map<String, Object> params;
    private BacktestPattern backtestPattern;

    // Default constructor
    public BacktestRequest() {
    }

    // Getters and Setters
    public String getStrategyCode() {
        return strategyCode;
    }

    public void setStrategyCode(String strategyCode) {
        this.strategyCode = strategyCode;
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public BacktestPattern getBacktestPattern() {
        return backtestPattern;
    }

    public void setBacktestPattern(BacktestPattern backtestPattern) {
        this.backtestPattern = backtestPattern;
    }
} 