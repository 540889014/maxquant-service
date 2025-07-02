package com.example.crypto.service.impl;

import com.example.crypto.entity.DepthData;
import com.example.crypto.service.ClickHouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClickHouseServiceImpl implements ClickHouseService {

    private final JdbcTemplate clickhouseJdbcTemplate;

    @Autowired
    public ClickHouseServiceImpl(@Qualifier("clickhouseJdbcTemplate") JdbcTemplate clickhouseJdbcTemplate) {
        this.clickhouseJdbcTemplate = clickhouseJdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS depth_data (" +
                "    symbol String," +
                "    exchange String," +
                "    timestamp UInt64," +
                "    bids String," +
                "    asks String" +
                ") ENGINE = MergeTree() ORDER BY (symbol, exchange, timestamp)";
        clickhouseJdbcTemplate.execute(sql);
    }

    @Override
    public void saveDepthData(List<DepthData> depthDataList) {
        if (depthDataList == null || depthDataList.isEmpty()) {
            return;
        }
        try {
            String sql = "INSERT INTO depth_data (symbol, exchange, timestamp, bids, asks) VALUES (?, ?, ?, ?, ?)";

            List<Object[]> batchArgs = depthDataList.stream()
                    .map(data -> new Object[]{
                            data.getSymbol(),
                            data.getExchange(),
                            data.getTimestamp(),
                            data.getBids(),
                            data.getAsks()
                    })
                    .collect(Collectors.toList());

            clickhouseJdbcTemplate.batchUpdate(sql, batchArgs);
        } catch (Exception e) {
            // Add proper logging here
            e.printStackTrace();
        }
    }
} 