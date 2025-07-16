# 删除回测报告API

## 概述
新增了删除回测报告的功能，允许用户删除指定策略和时间戳的回测报告。

## API端点

### DELETE /api/backtest-reports/{strategyName}/{timestamp}

删除指定策略和时间戳的回测报告。

#### 路径参数
- `strategyName` (String): 策略名称
- `timestamp` (String): 回测报告的时间戳

#### 响应

**成功响应 (200 OK)**
```json
"Backtest report deleted successfully: {strategyName}/{timestamp}"
```

**资源未找到 (404 Not Found)**
```json
"Backtest report not found: {strategyName}/{timestamp}"
```

**服务器错误 (500 Internal Server Error)**
```json
"Error deleting backtest report: {error message}"
```

#### 示例

**删除回测报告**
```bash
curl -X DELETE "http://localhost:8080/api/backtest-reports/MyStrategy/20231201_120000"
```

**成功响应**
```
Backtest report deleted successfully: MyStrategy/20231201_120000
```

**资源未找到响应**
```
Backtest report not found: MyStrategy/20231201_120000
```

## 实现细节

### 服务层
- `BacktestReportService.deleteBacktestReport(String strategyName, String timestamp)`: 删除指定的回测报告
- 使用递归删除目录及其所有内容
- 如果报告不存在，抛出 `ResourceNotFoundException`
- 如果路径无效，抛出 `IOException`

### 控制器层
- `BacktestReportController.deleteBacktestReport()`: 处理DELETE请求
- 适当的错误处理和HTTP状态码响应
- 支持中文错误消息

### 安全考虑
- 删除操作是永久性的，无法撤销
- 建议在生产环境中添加适当的权限验证
- 可以考虑添加确认机制或软删除功能

## 注意事项
1. 删除操作会永久删除整个回测报告目录及其所有文件
2. 删除前请确保不再需要该报告
3. 建议在删除前进行备份
4. 该操作不可撤销 