package com.example.crypto.dao;

import com.example.crypto.entity.KlineData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.crypto.dao.projection.KlineIdentifier;

import java.util.List;
import java.util.Optional;

/**
 * K線データリポジトリインターフェース
 * K線データのデータベース操作を定義
 */
public interface KlineDataRepository extends JpaRepository<KlineData, Long> { // id 類型改為 Long
    @Query("SELECT k FROM KlineData k WHERE k.symbol = :symbol AND k.timeframe = :timeframe AND k.timestamp BETWEEN :startTime AND :endTime AND k.exchange = :exchange")
    List<KlineData> findBySymbolAndTimeframeAndTimestampBetweenAndExchange(@Param("symbol") String symbol, @Param("timeframe") String timeframe, @Param("startTime") Long startTime, @Param("endTime") Long endTime, @Param("exchange") String exchange);

    Optional<KlineData> findBySymbolAndTimeframeAndTimestampAndExchange(String symbol, String timeframe, Long timestamp, String exchange);

    @Query("SELECT DISTINCT k.symbol as symbol, k.timeframe as timeframe, k.exchange as exchange FROM KlineData k")
    List<KlineIdentifier> findDistinctIdentifiers();

    List<KlineData> findBySymbolAndTimeframeAndExchangeOrderByTimestampAsc(String symbol, String timeframe, String exchange);
}