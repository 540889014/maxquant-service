package com.example.crypto.dao;

import com.example.crypto.entity.KlineData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * K線データリポジトリインターフェース
 * K線データのデータベース操作を定義
 */
public interface KlineDataRepository extends JpaRepository<KlineData, Long> { // id 類型改為 Long
    @Query("SELECT k FROM KlineData k WHERE k.symbol = :symbol AND k.timeframe = :timeframe AND k.timestamp BETWEEN :startTime AND :endTime")
    List<KlineData> findBySymbolAndTimeframeAndTimestampBetween(@Param("symbol") String symbol, @Param("timeframe") String timeframe, @Param("startTime") Long startTime, @Param("endTime") Long endTime);

    boolean existsBySymbolAndTimeframeAndTimestamp(String symbol, String timeframe, Long timestamp);
}