package com.example.crypto.dao;

import com.example.crypto.entity.CryptoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CryptoMetadataRepository extends JpaRepository<CryptoMetadata, String> {
    List<CryptoMetadata> findByExchangeAndInstType(String exchange, String instType);
    List<CryptoMetadata> findByExchangeAndInstTypeNot(String exchange, String instType);
    List<CryptoMetadata> findByInstTypeAndExchange(String instType, String exchange);
    List<CryptoMetadata> findByExchange(String exchange);
    List<CryptoMetadata> findByInstType(String instType);
    @Query("SELECT DISTINCT c.exchange FROM CryptoMetadata c")
    List<String> findDistinctExchanges();
} 