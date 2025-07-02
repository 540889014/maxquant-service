
-- 更新KlineData表中的exchange字段为'okx'
UPDATE kline_data SET exchange = 'okx' WHERE exchange IS NULL OR exchange != 'okx';

-- 更新RealtimeData表中的exchange字段为'okx'
UPDATE realtime_data SET exchange = 'okx' WHERE exchange IS NULL OR exchange != 'okx';

-- 更新DepthData表中的exchange字段为'okx'
UPDATE depth_data SET exchange = 'okx' WHERE exchange IS NULL OR exchange != 'okx';

-- 更新subscriptions表中的exchange字段为OKX
UPDATE subscriptions SET exchange = 'okx'; 