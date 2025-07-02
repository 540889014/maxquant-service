-- ユーザテーブルを作成
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- デフォルトユーザを挿入（admin/admin123，パスワードは平文）
INSERT INTO users (username, password) VALUES
    ('admin', 'admin123') ON CONFLICT DO NOTHING;

-- 契約メタデータテーブルを作成
CREATE TABLE IF NOT EXISTS crypto_metadata (
    id VARCHAR(255) PRIMARY KEY,
    inst_type VARCHAR(50),
    inst_id VARCHAR(255),
    base_ccy VARCHAR(50),
    quote_ccy VARCHAR(50),
    state VARCHAR(50)
    );

-- K線データテーブルを作成
CREATE TABLE IF NOT EXISTS kline_data (
                                          id BIGSERIAL PRIMARY KEY, -- 改為 BIGSERIAL 自增
                                          symbol VARCHAR(255),
    timeframe VARCHAR(50),
    timestamp BIGINT,
    open_price DOUBLE PRECISION,
    high_price DOUBLE PRECISION,
    low_price DOUBLE PRECISION,
    close_price DOUBLE PRECISION,
    volume DOUBLE PRECISION,
    created_at TIMESTAMP,
    CONSTRAINT unique_kline UNIQUE (symbol, timeframe, timestamp)
    );

-- リアルタイムデータテーブルを作成
CREATE TABLE IF NOT EXISTS realtime_data (
    id VARCHAR(255) PRIMARY KEY,
    symbol VARCHAR(255),
    price DOUBLE PRECISION,
    timestamp BIGINT,
    created_at TIMESTAMP
    );

-- 深度データテーブルを作成
CREATE TABLE IF NOT EXISTS depth_data (
    id VARCHAR(255) PRIMARY KEY,
    symbol VARCHAR(255),
    bids TEXT,
    asks TEXT,
    timestamp BIGINT,
    created_at TIMESTAMP
    );

-- 訂閱情報テーブルを作成
CREATE TABLE IF NOT EXISTS subscriptions (
                                             id SERIAL PRIMARY KEY,
                                             username VARCHAR(50) NOT NULL,
    symbol VARCHAR(255) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    inst_type VARCHAR(50) NOT NULL,
    timeframe VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_username FOREIGN KEY (username) REFERENCES users(username)
    );