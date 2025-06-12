package com.example.crypto.dao;

import com.example.crypto.entity.DepthData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * 深度データのリポジトリインターフェース
 * 市場深度データのデータベース操作を定義
 */
public interface DepthDataRepository extends JpaRepository<DepthData, String> {
    Optional<DepthData> findTopBySymbolOrderByTimestampDesc(String symbol);
}