package com.example.crypto.service;

import com.example.crypto.entity.DepthData;
import java.util.List;

public interface ClickHouseService {
    void saveDepthData(List<DepthData> depthDataList);
} 