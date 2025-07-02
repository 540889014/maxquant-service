package com.example.crypto.dao;

import com.example.crypto.entity.StatisticalIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface StatisticalIndexRepository extends JpaRepository<StatisticalIndex, Long> {

    List<StatisticalIndex> findByTimeframeAndExchangeAndCalculationDate(String timeframe, String exchange, LocalDate calculationDate);

    void deleteByCalculationDateBefore(LocalDate date);

    StatisticalIndex findBySymbolPairAndTimeframeAndExchangeAndCalculationDate(String symbolPair, String timeframe, String exchange, LocalDate calculationDate);
    
    void deleteByTimeframeAndExchangeAndCalculationDate(String timeframe, String exchange, LocalDate calculationDate);
} 