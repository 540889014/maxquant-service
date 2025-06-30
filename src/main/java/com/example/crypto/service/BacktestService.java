package com.example.crypto.service;

import com.example.crypto.dto.BacktestRequest;
import com.example.crypto.dto.BacktestRunRequest;

public interface BacktestService {
    void runBacktest(String backtestId, BacktestRequest request, String token);
    void runBacktest(String backtestId, BacktestRunRequest request, String token);
    void runBacktest(String backtestId, String paramsJson, String token);
}