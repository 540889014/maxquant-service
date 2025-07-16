# 删除回测报告接口 - 前端开发上下文

## 接口基本信息

### 接口地址
```
DELETE /api/backtest-reports/{strategyName}/{timestamp}
```

### 请求方法
`DELETE`

### Content-Type
`application/json`

## 路径参数

| 参数名 | 类型 | 必填 | 描述 | 示例 |
|--------|------|------|------|------|
| strategyName | String | 是 | 策略名称 | "MyStrategy" |
| timestamp | String | 是 | 回测报告时间戳 | "20231201_120000" |

## 请求示例

### cURL
```bash
curl -X DELETE \
  "http://localhost:8080/api/backtest-reports/MyStrategy/20231201_120000" \
  -H "Authorization: Bearer your-jwt-token" \
  -H "Content-Type: application/json"
```

### JavaScript (Fetch API)
```javascript
const deleteBacktestReport = async (strategyName, timestamp) => {
  try {
    const response = await fetch(`/api/backtest-reports/${strategyName}/${timestamp}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    if (response.ok) {
      const result = await response.text();
      console.log('删除成功:', result);
      return { success: true, message: result };
    } else if (response.status === 404) {
      const errorMessage = await response.text();
      return { success: false, error: 'NOT_FOUND', message: errorMessage };
    } else {
      const errorMessage = await response.text();
      return { success: false, error: 'SERVER_ERROR', message: errorMessage };
    }
  } catch (error) {
    return { success: false, error: 'NETWORK_ERROR', message: error.message };
  }
};
```

### JavaScript (Axios)
```javascript
import axios from 'axios';

const deleteBacktestReport = async (strategyName, timestamp) => {
  try {
    const response = await axios.delete(`/api/backtest-reports/${strategyName}/${timestamp}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    return { success: true, message: response.data };
  } catch (error) {
    if (error.response) {
      const { status, data } = error.response;
      if (status === 404) {
        return { success: false, error: 'NOT_FOUND', message: data };
      } else {
        return { success: false, error: 'SERVER_ERROR', message: data };
      }
    } else {
      return { success: false, error: 'NETWORK_ERROR', message: error.message };
    }
  }
};
```

## 响应格式

### 成功响应 (200 OK)
```json
{
  "status": 200,
  "data": "Backtest report deleted successfully: MyStrategy/20231201_120000",
  "success": true
}
```

### 资源未找到 (404 Not Found)
```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Backtest report not found: MyStrategy/20231201_120000",
  "success": false
}
```

### 服务器错误 (500 Internal Server Error)
```json
{
  "status": 500,
  "error": "SERVER_ERROR",
  "message": "Error deleting backtest report: Invalid report path",
  "success": false
}
```

## 前端组件示例

### Vue.js 组件
```vue
<template>
  <div class="backtest-report-delete">
    <el-button 
      type="danger" 
      size="small" 
      @click="confirmDelete"
      :loading="loading"
      :disabled="loading">
      删除报告
    </el-button>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref } from 'vue';

export default {
  name: 'BacktestReportDelete',
  props: {
    strategyName: {
      type: String,
      required: true
    },
    timestamp: {
      type: String,
      required: true
    }
  },
  emits: ['deleted'],
  setup(props, { emit }) {
    const loading = ref(false);

    const confirmDelete = async () => {
      try {
        await ElMessageBox.confirm(
          `确定要删除回测报告 "${props.strategyName}/${props.timestamp}" 吗？此操作不可撤销。`,
          '确认删除',
          {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning',
          }
        );
        
        await deleteReport();
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('操作失败');
        }
      }
    };

    const deleteReport = async () => {
      loading.value = true;
      try {
        const response = await fetch(`/api/backtest-reports/${props.strategyName}/${props.timestamp}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'Content-Type': 'application/json'
          }
        });

        if (response.ok) {
          const message = await response.text();
          ElMessage.success('删除成功');
          emit('deleted', { strategyName: props.strategyName, timestamp: props.timestamp });
        } else if (response.status === 404) {
          const errorMessage = await response.text();
          ElMessage.error('报告不存在');
        } else {
          const errorMessage = await response.text();
          ElMessage.error('删除失败: ' + errorMessage);
        }
      } catch (error) {
        ElMessage.error('网络错误: ' + error.message);
      } finally {
        loading.value = false;
      }
    };

    return {
      loading,
      confirmDelete
    };
  }
};
</script>
```

### React 组件
```jsx
import React, { useState } from 'react';
import { Button, Modal, message } from 'antd';
import { DeleteOutlined, ExclamationCircleOutlined } from '@ant-design/icons';

const { confirm } = Modal;

const BacktestReportDelete = ({ strategyName, timestamp, onDeleted }) => {
  const [loading, setLoading] = useState(false);

  const showDeleteConfirm = () => {
    confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除回测报告 "${strategyName}/${timestamp}" 吗？此操作不可撤销。`,
      okText: '确定删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: handleDelete,
    });
  };

  const handleDelete = async () => {
    setLoading(true);
    try {
      const response = await fetch(`/api/backtest-reports/${strategyName}/${timestamp}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const result = await response.text();
        message.success('删除成功');
        onDeleted && onDeleted({ strategyName, timestamp });
      } else if (response.status === 404) {
        message.error('报告不存在');
      } else {
        const errorMessage = await response.text();
        message.error('删除失败: ' + errorMessage);
      }
    } catch (error) {
      message.error('网络错误: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Button
      type="primary"
      danger
      size="small"
      icon={<DeleteOutlined />}
      loading={loading}
      onClick={showDeleteConfirm}
    >
      删除报告
    </Button>
  );
};

export default BacktestReportDelete;
```

## 错误处理策略

### 错误类型
1. **NOT_FOUND (404)**: 报告不存在
2. **SERVER_ERROR (500)**: 服务器内部错误
3. **NETWORK_ERROR**: 网络连接错误
4. **UNAUTHORIZED (401)**: 未授权访问

### 错误处理建议
```javascript
const handleDeleteError = (error) => {
  switch (error.type) {
    case 'NOT_FOUND':
      // 显示"报告不存在"提示，可能需要刷新列表
      showMessage('报告不存在，可能已被删除', 'warning');
      refreshReportList();
      break;
    case 'SERVER_ERROR':
      // 显示服务器错误，建议用户稍后重试
      showMessage('服务器错误，请稍后重试', 'error');
      break;
    case 'NETWORK_ERROR':
      // 显示网络错误，建议检查网络连接
      showMessage('网络连接错误，请检查网络', 'error');
      break;
    case 'UNAUTHORIZED':
      // 跳转到登录页面
      redirectToLogin();
      break;
    default:
      showMessage('未知错误', 'error');
  }
};
```

## 集成建议

### 1. 权限控制
```javascript
// 检查用户是否有删除权限
const canDeleteReport = (report) => {
  return report.userId === currentUser.id || currentUser.role === 'ADMIN';
};
```

### 2. 列表更新
```javascript
// 删除成功后更新列表
const handleDeleteSuccess = (deletedReport) => {
  // 从列表中移除已删除的报告
  setReportList(prevList => 
    prevList.filter(report => 
      !(report.strategyName === deletedReport.strategyName && 
        report.timestamp === deletedReport.timestamp)
    )
  );
};
```

### 3. 批量删除
```javascript
// 批量删除多个报告
const batchDeleteReports = async (reports) => {
  const promises = reports.map(report => 
    deleteBacktestReport(report.strategyName, report.timestamp)
  );
  
  const results = await Promise.allSettled(promises);
  const successCount = results.filter(r => r.status === 'fulfilled' && r.value.success).length;
  
  message.success(`成功删除 ${successCount}/${reports.length} 个报告`);
};
```

## 测试用例

### 单元测试示例
```javascript
describe('deleteBacktestReport', () => {
  it('should delete report successfully', async () => {
    const mockResponse = 'Backtest report deleted successfully: TestStrategy/20231201_120000';
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        text: () => Promise.resolve(mockResponse)
      })
    );

    const result = await deleteBacktestReport('TestStrategy', '20231201_120000');
    
    expect(result.success).toBe(true);
    expect(result.message).toBe(mockResponse);
  });

  it('should handle 404 error', async () => {
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: false,
        status: 404,
        text: () => Promise.resolve('Backtest report not found')
      })
    );

    const result = await deleteBacktestReport('TestStrategy', '20231201_120000');
    
    expect(result.success).toBe(false);
    expect(result.error).toBe('NOT_FOUND');
  });
});
```

## 注意事项

1. **确认对话框**: 建议在删除前显示确认对话框，因为删除操作不可撤销
2. **权限验证**: 确保只有有权限的用户才能删除报告
3. **列表刷新**: 删除成功后及时更新报告列表
4. **错误提示**: 提供清晰的错误信息给用户
5. **加载状态**: 在删除过程中显示加载状态
6. **网络重试**: 考虑在网络错误时提供重试机制 