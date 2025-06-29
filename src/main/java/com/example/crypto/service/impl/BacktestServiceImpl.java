package com.example.crypto.service.impl;

import com.example.crypto.dto.BacktestRequest;
import com.example.crypto.dto.BacktestRunRequest;
import com.example.crypto.service.BacktestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Optional;

@Service
public class BacktestServiceImpl implements BacktestService {

    private static final Logger logger = LoggerFactory.getLogger(BacktestServiceImpl.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Value("${backtest.python.executable:python3}")
    private String pythonExecutable;

    @Value("${backtest.python.runner-script-path:quantlib/backtest_runner.py}")
    private String runnerScriptPath;

    public BacktestServiceImpl(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Async
    @Override
    public void runBacktest(String backtestId, BacktestRequest request, String token) {
        String destination = "/topic/backtest/" + backtestId;
        Path tempScript = null;
        Path tempConfig = null;
        try {
            // 1. 参数校验
            if (request.getStrategyCode() == null || request.getStrategyCode().isEmpty()) {
                throw new IllegalArgumentException("Strategy code cannot be null or empty.");
            }
            if (request.getSymbols() == null || request.getSymbols().isEmpty()) {
                throw new IllegalArgumentException("Symbols list cannot be null or empty.");
            }

            // 2. 保存策略代码到临时文件
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "strategies");
            Files.createDirectories(tempDir);
            tempScript = Files.createTempFile(tempDir, "strategy-" + backtestId, ".py");
            Files.writeString(tempScript, request.getStrategyCode());
            logger.info("Strategy code saved to temporary file: {}", tempScript.toAbsolutePath());

            // 3. 定位Python启动脚本
            File launcherFile = new File(runnerScriptPath);
            if (!launcherFile.exists() || !launcherFile.isFile()) {
                throw new IOException("Could not find backtest runner script at the configured path: " + runnerScriptPath);
            }
            String launcherPath = launcherFile.getAbsolutePath();

            // 4. 严格按照模板构建JSON配置
            Map<String, Object> config = new HashMap<>();
            
            // --- Root Level ---
            config.put("STRATEGY_FILE", tempScript.toAbsolutePath().toString());
            config.put("STRATEGY_NAME", request.getParams() != null && request.getParams().containsKey("strategy_name") ? request.getParams().get("strategy_name").toString() : "MyCryptoStrategy");
            config.put("RESULT_ID", backtestId);
            config.put("USER_TOKEN", token);
            config.put("LOG_LEVEL", "INFO");
            config.put("STUDIO_DB", "sqlite:////Users/xiaoxiao_coder/Desktop/quant_project/backtest/finonelib/finone_crypto.db");
            config.put("CSV_OUTPUT_PATH", "backtest_results/");
            config.put("IS_OPTIMIZE", false);
            config.put("RATES_URL", "http://localhost:8080"); // Should be configurable
            config.put("PARAMS", Collections.emptyList()); // Top-level PARAMS is an empty list

            // --- BACKTEST object ---
            Map<String, Object> backtestConfig = new HashMap<>();
            backtestConfig.put("START_TIME", convertDateToTimestamp(request.getStartDate()));
            backtestConfig.put("END_TIME", convertDateToTimestamp(request.getEndDate()));
            backtestConfig.put("BACKTEST_PATTERN", Map.of("PATTERN_NAME", "online", "PATTERN_PARAMS", Collections.emptyMap()));
            backtestConfig.put("OUTPUT_REPORT", true);
            backtestConfig.put("PERFORMANCE", Collections.emptyMap());
            backtestConfig.put("SEND_HEARTBEAT", false);
            backtestConfig.put("HEARTBEAT_INTERVAL", 60000);
            config.put("BACKTEST", backtestConfig);

            // --- DATA_TYPE object ---
            Map<String, Object> dataTypeConfig = new HashMap<>();
            dataTypeConfig.put("USE_ORDER_BOOK", request.getDataType().equals("depth"));
            dataTypeConfig.put("USE_TRADE", false);
            dataTypeConfig.put("OHLC", List.of(Map.of(
                "TYPE", request.getDataType().equals("depth") ? "MID" : "OHLC",
                "TIME_TYPE", convertTimeframe(request.getTimeframe()),
                "USE", true
            )));
            config.put("DATA_TYPE", dataTypeConfig);

            // --- SYMBOLS array ---
            List<Map<String, Object>> symbolsList = new ArrayList<>();
            Map<String, Object> symbolGroup = new HashMap<>();
            symbolGroup.put("PARAMS_NAME", request.getParams() != null && request.getParams().containsKey("strategy_name") ? request.getParams().get("strategy_name").toString() : "Default-Strategy-Params");
            symbolGroup.put("WITHOUT_TIME", request.getSymbols());
            
            List<Map<String, Object>> strategyParamsList = new ArrayList<>();
            if (request.getParams() != null) {
                request.getParams().forEach((key, value) -> {
                    if (!key.equals("strategy_name")) { // Exclude strategy_name from params
                        Map<String, Object> param = new HashMap<>();
                        param.put("NAME", key);
                        param.put("VALUE", value);
                        param.put("TYPE", getParamType(value));
                        strategyParamsList.add(param);
                    }
                });
            }
            symbolGroup.put("PARAMS", strategyParamsList);
            symbolsList.add(symbolGroup);
            config.put("SYMBOLS", symbolsList);
            
            // 5. 将配置写入临时JSON文件
            tempConfig = Files.createTempFile("config-" + backtestId, ".json");
            Files.writeString(tempConfig, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config));
            logger.info("Backtest config saved to temporary file: {}", tempConfig.toAbsolutePath());
            
            // 6. 执行新的命令格式 (使用--config)
            List<String> command = new ArrayList<>();
            command.add(pythonExecutable);
            command.add("-u");
            command.add(launcherPath);
            command.add("--config");
            command.add(tempConfig.toAbsolutePath().toString());

            logger.info("Executing backtest command for ID {}: {}", backtestId, String.join(" ", command));
            sendMessage(destination, "log", "Backtest starting with launcher script...");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[Python Output] {}: {}", backtestId, line);
                    if (line.startsWith("RESULT:")) {
                        String jsonOutput = line.substring("RESULT:".length());
                        sendMessage(destination, "result", jsonOutput);
                    } else {
                        // All other non-result lines are considered logs
                        sendMessage(destination, "log", line);
                    }
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                sendMessage(destination, "error", "Backtest process timed out after 10 minutes.");
                return;
            }
             int exitCode = process.exitValue();
            if (exitCode != 0) {
                 logger.error("Backtest script for ID {} failed with exit code {}.", backtestId, exitCode);
                 sendMessage(destination, "error", "Backtest script failed with exit code " + exitCode + ".");
            } else {
                sendMessage(destination, "log", "Backtest finished successfully.");
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Failed to execute backtest script for ID {}", backtestId, e);
            sendMessage(destination, "error", "Failed to run backtest: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            if (tempScript != null) {
                try {
                    Files.delete(tempScript);
                    logger.info("Deleted temporary script: {}", tempScript.getFileName());
                } catch (IOException e) {
                    logger.error("Failed to delete temporary script: {}", tempScript.getFileName(), e);
                }
            }
            if (tempConfig != null) {
                try {
                    Files.delete(tempConfig);
                    logger.info("Deleted temporary config file: {}", tempConfig.getFileName());
                } catch (IOException e) {
                    logger.error("Failed to delete temporary config file: {}", tempConfig.getFileName(), e);
                }
            }
            sendMessage(destination, "finished", "Backtest execution completed.");
        }
    }

    @Async
    @Override
    public void runBacktest(String backtestId, BacktestRunRequest req, String token) {
        String destination = "/topic/backtest/" + backtestId;
        Path tempScript = null;
        Path tempConfig = null;
        try {
            if (req.strategyCode() == null || req.strategyCode().isEmpty()) {
                throw new IllegalArgumentException("strategyCode missing");
            }

            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "strategies");
            Files.createDirectories(tempDir);
            tempScript = Files.createTempFile(tempDir, "strategy-" + backtestId, ".py");
            Files.writeString(tempScript, req.strategyCode());

            File launcherFile = new File(runnerScriptPath);
            if (!launcherFile.exists()) throw new IOException("runner script not found");

            Map<String,Object> cfg = new HashMap<>();
            cfg.put("STRATEGY_FILE", tempScript.toString());
            cfg.put("STRATEGY_NAME", "MyStrategy");
            cfg.put("ASSET_TYPE", "CRYPTO");
            cfg.put("RESULT_ID", backtestId);
            cfg.put("USER_TOKEN", token);
            cfg.put("LOG_LEVEL", Optional.ofNullable(req.logLevel()).orElse("INFO"));
            cfg.put("STUDIO_DB", "finone_crypto.db");
            cfg.put("CSV_OUTPUT_PATH", "backtest_results/");
            cfg.put("IS_OPTIMIZE", false);
            cfg.put("RATES_URL", "http://localhost:8080");

            // BACKTEST
            Map<String,Object> bt = new HashMap<>();
            bt.put("START_TIME", req.backtest().startTime());
            bt.put("END_TIME", req.backtest().endTime());
            
            // BACKTEST_PATTERN
            Map<String, Object> backtestPattern = new HashMap<>();
            if (req.backtest().backtestPattern() != null) {
                backtestPattern.put("PATTERN_NAME", req.backtest().backtestPattern().patternName());
                
                Map<String, Object> patternParams = new HashMap<>();
                if (req.backtest().backtestPattern().patternParams() != null) {
                    patternParams.put("miss_ratio", req.backtest().backtestPattern().patternParams().missRatio());
                    patternParams.put("slippage", req.backtest().backtestPattern().patternParams().slippage());
                }
                backtestPattern.put("PATTERN_PARAMS", patternParams);
            } else {
                backtestPattern.put("PATTERN_NAME", "OHLC");
                backtestPattern.put("PATTERN_PARAMS", Map.of());
            }
            
            bt.put("BACKTEST_PATTERN", backtestPattern);
            bt.put("OUTPUT_REPORT", req.backtest().outputReport());
            bt.put("PERFORMANCE", Map.of());
            bt.put("SEND_HEARTBEAT", false);
            bt.put("HEARTBEAT_INTERVAL", 60000);
            cfg.put("BACKTEST", bt);

            // DATA_TYPE
            Map<String,Object> dt = new HashMap<>();
            dt.put("USE_ORDER_BOOK", req.dataType().useOrderBook());
            dt.put("USE_TRADE", req.dataType().useTrade());
            
            List<Map<String,Object>> ohlcList = req.dataType().ohlc().stream()
                .map(o -> {
                    Map<String, Object> ohlcMap = new HashMap<>();
                    ohlcMap.put("TYPE", o.type());
                    ohlcMap.put("TIME_TYPE", o.timeType());
                    ohlcMap.put("USE", o.use());
                    return ohlcMap;
                })
                .collect(Collectors.toList());

            dt.put("OHLC", ohlcList);
            cfg.put("DATA_TYPE", dt);

            // SYMBOLS (Corrected based on user's sample)
            List<Map<String, Object>> symbolsList = req.symbols().stream()
                .map(s -> {
                    Map<String, Object> symbolGroup = new HashMap<>();
                    symbolGroup.put("WITHOUT_TIME", s.withoutTime());
                    return symbolGroup;
                })
                .collect(Collectors.toList());
            cfg.put("SYMBOLS", symbolsList);

            // PARAMS (Corrected based on user's sample)
            List<Map<String, Object>> paramsList = new ArrayList<>();
            if (req.params() != null) {
                req.params().forEach((key, value) -> {
                    Map<String, Object> param = new HashMap<>();
                    param.put("NAME", key);
                    param.put("VALUE", value);
                    param.put("TYPE", "IN"); 
                    param.put("VALUE_TYPE", getParamType(value));
                    paramsList.add(param);
                });
            }
            cfg.put("PARAMS", paramsList);
            
            // 5. 将配置写入临时JSON文件
            tempConfig = Files.createTempFile("config-" + backtestId, ".json");
            Files.writeString(tempConfig, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cfg));
            logger.info("Backtest config for ID {} saved to: {}", backtestId, tempConfig.toAbsolutePath());

            // 6. 执行命令
            List<String> command = List.of(pythonExecutable, "-u", launcherFile.getAbsolutePath(), "--config", tempConfig.toString());
            logger.info("Executing backtest command for ID {}: {}", backtestId, String.join(" ", command));
            sendMessage(destination, "log", "Backtest starting with runner script...");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[Python Output] {}: {}", backtestId, line);
                    if (line.startsWith("RESULT:")) {
                        sendMessage(destination, "result", line.substring("RESULT:".length()));
                    } else {
                        sendMessage(destination, "log", line);
                    }
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                sendMessage(destination, "error", "Backtest process timed out after 10 minutes.");
                return;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("Backtest script for ID {} failed with exit code {}.", backtestId, exitCode);
                sendMessage(destination, "error", "Backtest script failed with exit code " + exitCode + ".");
            } else {
                sendMessage(destination, "log", "Backtest finished successfully.");
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Failed to execute backtest script for ID {}", backtestId, e);
            sendMessage(destination, "error", "Failed to run backtest: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            if (tempScript != null) {
                try {
                    Files.delete(tempScript);
                } catch (IOException e) {
                    logger.error("Failed to delete temp script: {}", tempScript.getFileName(), e);
                }
            }
            if (tempConfig != null) {
                try {
                    Files.delete(tempConfig);
                } catch (IOException e) {
                    logger.error("Failed to delete temp config: {}", tempConfig.getFileName(), e);
                }
            }
            sendMessage(destination, "finished", "Backtest execution completed.");
        }
    }

    private static String getParamType(Object value) {
        if (value instanceof Integer || value instanceof Long) return "INT";
        if (value instanceof Double || value instanceof Float) return "DECIMAL";
        if (value instanceof Boolean) return "BOOL";
        return "STRING";
    }

    private String mapTimeTypeBack(String timeType) {
        return switch (timeType) {
            case "ONE_MINUTE" -> "1m";
            case "FIVE_MINUTE" -> "5m";
            case "FIFTEEN_MINUTE" -> "15m";
            case "THIRTY_MINUTE" -> "30m";
            case "ONE_HOUR" -> "1h";
            case "FOUR_HOUR" -> "4h";
            case "ONE_DAY" -> "1d";
            default -> "15m";
        };
    }

    private long convertDateToTimestamp(String dateString) {
        if (dateString == null) return System.currentTimeMillis();
        try {
            // Assuming format is yyyy-MM-dd
            return new SimpleDateFormat("yyyy-MM-dd").parse(dateString).getTime();
        } catch (ParseException e) {
            logger.error("Failed to parse date string: {}. Returning current time.", dateString, e);
            return System.currentTimeMillis();
        }
    }

    private String convertTimeframe(String timeframe) {
        if (timeframe == null) return "FIFTEEN_MINUTE"; // Default
        return switch (timeframe) {
            case "1m" -> "ONE_MINUTE";
            case "5m" -> "FIVE_MINUTE";
            case "15m" -> "FIFTEEN_MINUTE";
            case "30m" -> "THIRTY_MINUTE";
            case "1h" -> "ONE_HOUR";
            case "4h" -> "FOUR_HOUR";
            case "1d" -> "ONE_DAY";
            default -> "FIFTEEN_MINUTE";
        };
    }

    private void sendMessage(String destination, String type, String payload) {
        try {
            Map<String, Object> message;
            if ("result".equals(type)) {
                message = Map.of("type", type, "data", objectMapper.readValue(payload, Object.class));
            } else {
                message = Map.of("type", type, "data", payload);
            }
            messagingTemplate.convertAndSend(destination, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize message for WebSocket", e);
        }
    }
} 