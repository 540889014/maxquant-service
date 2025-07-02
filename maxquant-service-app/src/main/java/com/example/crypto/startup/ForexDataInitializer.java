package com.example.crypto.startup;

import com.example.crypto.entity.ForexMetadata;
import com.example.crypto.repository.ForexMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ForexDataInitializer implements CommandLineRunner {

    private final ForexMetadataRepository forexMetadataRepository;

    private static final Map<String, String> CURRENCY_NAMES = Map.ofEntries(
            Map.entry("EUR", "欧元"),
            Map.entry("USD", "美元"),
            Map.entry("JPY", "日元"),
            Map.entry("GBP", "英镑"),
            Map.entry("CHF", "瑞士法郎"),
            Map.entry("AUD", "澳大利亚元"),
            Map.entry("CAD", "加拿大元"),
            Map.entry("NZD", "新西兰元"),
            Map.entry("SEK", "瑞典克朗"),
            Map.entry("NOK", "挪威克朗")
    );

    private static final List<String> FOREX_DATA = Arrays.asList(
            "EUR/USD - 全球最交易活跃的币对，流动性极高，波动性适中。",
            "USD/JPY - 第二大交易币对，日本经济稳定，波动性较低。",
            "GBP/USD - 流动性高，英镑与美元的组合，波动性适中。",
            "USD/CHF - 瑞士法郎为避险货币，波动性低，适合低风险交易。",
            "AUD/USD - 澳大利亚经济稳定，商品相关性低，波动性适中。",
            "USD/CAD - 加拿大经济与美国高度相关，波动性较低。",
            "NZD/USD - 新西兰经济稳定，波动性适中，流动性良好。",
            "EUR/JPY - 欧元与日元组合，流动性高，波动性可控。",
            "GBP/JPY - 英镑与日元，流动性较高，但波动性稍高，需谨慎。",
            "EUR/GBP - 欧元与英镑，欧洲经济相关性高，波动性较低。",
            "EUR/CHF - 欧元与瑞士法郎，避险特性，波动性低。",
            "GBP/CHF - 英镑与瑞士法郎，避险组合，波动性适中。",
            "AUD/JPY - 澳大利亚元与日元，流动性好，波动性可控。",
            "CAD/JPY - 加拿大元与日元，经济相关性稳定，波动性较低。",
            "NZD/JPY - 新西兰元与日元，流动性适中，波动性可控。",
            "EUR/CAD - 欧元与加拿大元，欧洲与北美经济稳定，波动性低。",
            "AUD/CAD - 澳大利亚元与加拿大元，商品经济相关，波动性适中。",
            "NZD/CAD - 新西兰元与加拿大元，经济稳定性较高。",
            "USD/SEK - 美元与瑞典克朗，北欧经济稳定，波动性较低。",
            "USD/NOK - 美元与挪威克朗，北欧经济稳定，波动性适中。"
    );

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        for (String line : FOREX_DATA) {
            String[] parts = line.split(" - ", 2);
            String symbol = parts[0];
            String description = parts[1];

            if (!forexMetadataRepository.existsBySymbol(symbol)) {
                String[] currencies = symbol.split("/");
                String baseCurrency = currencies[0];
                String quoteCurrency = currencies[1];

                ForexMetadata metadata = new ForexMetadata(
                        null,
                        symbol,
                        baseCurrency,
                        quoteCurrency,
                        CURRENCY_NAMES.getOrDefault(baseCurrency, baseCurrency),
                        CURRENCY_NAMES.getOrDefault(quoteCurrency, quoteCurrency),
                        description
                );
                forexMetadataRepository.save(metadata);
            }
        }
    }
} 