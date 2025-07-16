# 删除回测报告接口

## 接口信息

| 项目 | 值 |
|------|-----|
| 接口地址 | `DELETE /api/backtest-reports/{strategyName}/{timestamp}` |
| 请求方法 | DELETE |
| Content-Type | application/json |

## 入参

### 路径参数

| 参数名 | 类型 | 必填 | 描述 | 示例 |
|--------|------|------|------|------|
| strategyName | String | 是 | 策略名称 | "MyStrategy" |
| timestamp | String | 是 | 回测报告时间戳 | "20231201_120000" |

### 请求头

| 参数名 | 类型 | 必填 | 描述 | 示例 |
|--------|------|------|------|------|
| Authorization | String | 是 | JWT Token | "Bearer eyJhbGciOiJIUzI1NiIs..." |
| Content-Type | String | 是 | 内容类型 | "application/json" |

## 出参

### 成功响应 (200 OK)

**响应体**: `String`

**示例**:
```
Backtest report deleted successfully: MyStrategy/20231201_120000
```

### 资源未找到 (404 Not Found)

**响应体**: `String`

**示例**:
```
Backtest report not found: MyStrategy/20231201_120000
```

### 服务器错误 (500 Internal Server Error)

**响应体**: `String`

**示例**:
```
Error deleting backtest report: Invalid report path
```

## 调用示例

```bash
curl -X DELETE \
  "http://localhost:8080/api/backtest-reports/MyStrategy/20231201_120000" \
  -H "Authorization: Bearer your-jwt-token" \
  -H "Content-Type: application/json"
```

```javascript
const response = await fetch('/api/backtest-reports/MyStrategy/20231201_120000', {
  method: 'DELETE',
  headers: {
    'Authorization': 'Bearer your-jwt-token',
    'Content-Type': 'application/json'
  }
});

const result = await response.text();
``` 