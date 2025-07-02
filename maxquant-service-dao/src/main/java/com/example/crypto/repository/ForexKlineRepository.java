package com.example.crypto.repository;

import com.example.crypto.entity.ForexKline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForexKlineRepository extends JpaRepository<ForexKline, Long> {
    
    boolean existsBySymbolAndPeriodAndTimestamp(String symbol, String period, Long timestamp);

    Optional<ForexKline> findFirstBySymbolAndPeriodOrderByTimestampDesc(String symbol, String period);

    List<ForexKline> findBySymbolAndPeriodAndTimestampGreaterThanEqualOrderByTimestampAsc(String symbol, String period, Long startTimestamp);

    List<ForexKline> findBySymbolAndPeriodAndTimestampBetween(String symbol, String period, Long startTime, Long endTime);
} 