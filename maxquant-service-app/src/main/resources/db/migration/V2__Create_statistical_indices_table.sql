-- 创建 statistical_indices 表，用于存储ADF、KPSS和Hurst指数的计算结果
CREATE TABLE statistical_indices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol_pair VARCHAR(255) NOT NULL,
    timeframe VARCHAR(50) NOT NULL,
    exchange VARCHAR(50) NOT NULL,
    adf_value DOUBLE,
    kpss_value DOUBLE,
    hurst_value DOUBLE,
    calculation_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_pair_timeframe_exchange (symbol_pair, timeframe, exchange, calculation_date)
); 