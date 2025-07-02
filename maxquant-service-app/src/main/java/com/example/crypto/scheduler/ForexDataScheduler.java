package com.example.crypto.scheduler;

import com.example.crypto.dto.FcsApiResponseDto;
import com.example.crypto.entity.ForexKline;
import com.example.crypto.entity.ForexMetadata;
import com.example.crypto.repository.ForexKlineRepository;
import com.example.crypto.repository.ForexMetadataRepository;
import com.example.crypto.service.FcsApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ForexDataScheduler {

    private final FcsApiService fcsApiService;
    private final ForexMetadataRepository forexMetadataRepository;
    private final ForexKlineRepository forexKlineRepository;

    private static final Map<String, Long> PERIOD_SECONDS_MAP = Map.of(
            "10m", 600L,
            "15m", 900L,
            "30m", 1800L,
            "1h", 3600L,
            "1d", 86400L
    );

    @Scheduled(cron = "0 0 20 * * MON-FRI", zone = "Asia/Shanghai")
    @Transactional
    public void syncAllForexData() {
        log.info("Starting scheduled forex kline data synchronization...");
        List<ForexMetadata> allForexPairs = forexMetadataRepository.findAll();
        log.warn("Found {} forex pairs to sync. This will result in {} API calls.", allForexPairs.size(), allForexPairs.size());

        for (ForexMetadata metadata : allForexPairs) {
            try {
                syncDataForSymbol(metadata.getSymbol(), "5m");
                
                log.info("Request successful for {}. Waiting for 20 seconds before next request...", metadata.getSymbol());
                Thread.sleep(20000); // 20-second delay
            } catch (Exception e) {
                log.error("Failed to sync data for symbol: {}. Error: {}", metadata.getSymbol(), e.getMessage());
                // Optional: decide if you want to wait even after a failure
                try {
                    Thread.sleep(20000); 
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        log.info("Finished scheduled forex kline data synchronization.");
    }

    @Scheduled(cron = "0 30 20 * * *", zone = "Asia/Shanghai")
    @Transactional
    public void aggregateKlines() {
        log.info("Starting kline aggregation task...");
        List<ForexMetadata> allForexPairs = forexMetadataRepository.findAll();
        List<String> targetPeriods = List.of("10m", "15m", "30m", "1h", "1d");

        for (ForexMetadata metadata : allForexPairs) {
            for (String period : targetPeriods) {
                try {
                    aggregateSymbolToPeriod(metadata.getSymbol(), period);
                } catch (Exception e) {
                    log.error("Failed to aggregate klines for symbol {} to period {}", metadata.getSymbol(), period, e);
                }
            }
        }
        log.info("Finished kline aggregation task.");
    }

    private void aggregateSymbolToPeriod(String symbol, String targetPeriod) {
        long targetPeriodSeconds = PERIOD_SECONDS_MAP.get(targetPeriod);
        // Fetch last 25 hours of 5-minute data to be safe
        long startTimestamp = Instant.now().minus(25, ChronoUnit.HOURS).getEpochSecond();

        List<ForexKline> sourceKlines = forexKlineRepository.findBySymbolAndPeriodAndTimestampGreaterThanEqualOrderByTimestampAsc(symbol, "5m", startTimestamp);

        if (sourceKlines.isEmpty()) {
            return;
        }

        Map<Long, List<ForexKline>> groupedKlines = sourceKlines.stream()
                .collect(Collectors.groupingBy(kline -> kline.getTimestamp() - (kline.getTimestamp() % targetPeriodSeconds)));

        List<ForexKline> newKlines = new ArrayList<>();
        for (Map.Entry<Long, List<ForexKline>> entry : groupedKlines.entrySet()) {
            Long newTimestamp = entry.getKey();
            List<ForexKline> bucket = entry.getValue();

            if (forexKlineRepository.existsBySymbolAndPeriodAndTimestamp(symbol, targetPeriod, newTimestamp)) {
                continue;
            }

            BigDecimal open = bucket.stream().min(Comparator.comparing(ForexKline::getTimestamp)).get().getOpen();
            BigDecimal high = bucket.stream().map(ForexKline::getHigh).max(BigDecimal::compareTo).get();
            BigDecimal low = bucket.stream().map(ForexKline::getLow).min(BigDecimal::compareTo).get();
            BigDecimal close = bucket.stream().max(Comparator.comparing(ForexKline::getTimestamp)).get().getClose();

            newKlines.add(new ForexKline(null, symbol, targetPeriod, newTimestamp, open, high, low, close));
        }

        if (!newKlines.isEmpty()) {
            forexKlineRepository.saveAll(newKlines);
            log.info("Aggregated and saved {} new klines for symbol {} with period {}", newKlines.size(), symbol, targetPeriod);
        }
    }

    private void syncDataForSymbol(String symbol, String period) {
        log.info("Fetching data for {}/{}", symbol, period);
        FcsApiResponseDto response = fcsApiService.fetchHistory(symbol, period);
        if (response == null || !response.isStatus() || response.getResponse() == null) {
            log.error("Received invalid response from FCSAPI for symbol: {}", symbol);
            return;
        }

        List<ForexKline> klinesToSave = new ArrayList<>();
        response.getResponse().values().forEach(dto -> {
            if (!forexKlineRepository.existsBySymbolAndPeriodAndTimestamp(symbol, period, dto.getTimestamp())) {
                ForexKline kline = new ForexKline(
                        null,
                        symbol,
                        period,
                        dto.getTimestamp(),
                        new BigDecimal(dto.getOpen()),
                        new BigDecimal(dto.getHigh()),
                        new BigDecimal(dto.getLow()),
                        new BigDecimal(dto.getClose())
                );
                klinesToSave.add(kline);
            }
        });

        if (!klinesToSave.isEmpty()) {
            forexKlineRepository.saveAll(klinesToSave);
            log.info("Saved {} new kline records for symbol: {}", klinesToSave.size(), symbol);
        } else {
            log.info("No new kline data to save for symbol: {}", symbol);
        }
    }
} 