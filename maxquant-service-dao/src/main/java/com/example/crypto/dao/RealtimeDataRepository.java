package com.example.crypto.dao;

import com.example.crypto.entity.RealtimeData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * リアルタイムデータのリポジトリインターフェース
 * 最新市場データのデータベース操作を定義
 */
public interface RealtimeDataRepository extends JpaRepository<RealtimeData, String> {
    Optional<RealtimeData> findTopBySymbolOrderByTimestampDesc(String symbol);
    Optional<RealtimeData> findTopBySymbolAndExchangeOrderByTimestampDesc(String symbol, String exchange);
}