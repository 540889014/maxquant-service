package com.example.crypto.dao;

import com.example.crypto.entity.CryptoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 暗号資産メタデータのリポジトリインターフェース
 * 取引ペア情報のデータベース操作を定義
 */
public interface CryptoMetadataRepository extends JpaRepository<CryptoMetadata, String> {
    List<CryptoMetadata> findByInstType(String instType);
}