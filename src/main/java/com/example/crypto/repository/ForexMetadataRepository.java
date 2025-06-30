package com.example.crypto.repository;

import com.example.crypto.entity.ForexMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForexMetadataRepository extends JpaRepository<ForexMetadata, Long> {
    boolean existsBySymbol(String symbol);
} 