# Excel 导入导出增强功能

## 功能概述

在原有 Excel 导出功能基础上，新增以下增强功能：

1. **异步导出** - 支持大数据量后台导出，避免请求超时
2. **导入模板下载** - 根据配置自动生成带示例数据的模板
3. **导入错误报告** - 记录导入错误，生成可下载的错误报告 Excel

## API 接口

### 1. 导入相关

#### 下载导入模板
```http
GET /api/excel/template/{configKey}
```

**响应**: Excel 文件下载

---

#### 导入 Excel 数据
```http
POST /api/excel/import/{configKey}
Content-Type: multipart/form-data

file: [Excel 文件]
```

**响应示例**:
```json
{
  "success": true,
  "totalRows": 100,
  "successRows": 95,
  "failedRows": 5,
  "summary": "共 100 行，成功 95 行，失败 5 行",
  "errors": [
    {
      "rowNum": 3,
      "columnName": "手机号",
      "rawValue": "12345",
      "errorType": "格式错误",
      "errorMessage": "手机号格式不正确",
      "suggestion": "请输入 11 位手机号"
    }
  ],
  "errorReportPath": "/tmp/forge-excel-import/xxx_error.xlsx"
}
```

---

#### 下载导入错误报告
```http
GET /api/excel/error-report/{taskId}
```

**响应**: Excel 错误报告文件下载

---

### 2. 异步导出相关

#### 提交异步导出任务
```http
POST /api/excel/async-export/{configKey}
Content-Type: application/json

{
  "fileName": "用户导出_20250101.xlsx",
  "deptId": 1,
  "status": 1
}
```

**响应示例**:
```json
{
  "taskId": "abc123def456",
  "message": "导出任务已提交，请稍后查询状态"
}
```

---

#### 查询任务状态
```http
GET /api/excel/async-export/status/{taskId}
```

**响应示例**:
```json
{
  "taskId": "abc123def456",
  "configKey": "user_export",
  "fileName": "用户导出_20250101.xlsx",
  "status": 1,
  "fileSize": 102400,
  "dataCount": 1000,
  "createTime": "2025-01-01T10:00:00",
  "finishTime": "2025-01-01T10:01:30"
}
```

**状态说明**:
- `0` - 处理中
- `1` - 完成
- `2` - 失败

---

#### 下载导出文件
```http
GET /api/excel/async-export/download/{taskId}
```

**响应**: Excel 文件下载

---

#### 轮询导出结果（前端友好）
```http
GET /api/excel/async-export/{taskId}
```

**响应示例**:
```json
{
  "taskId": "abc123def456",
  "status": 1,
  "statusText": "完成",
  "fileName": "用户导出_20250101.xlsx",
  "fileSize": 102400,
  "createTime": "2025-01-01T10:00:00",
  "finishTime": "2025-01-01T10:01:30"
}
```

---

## 数据库配置

执行 `forge/forge-admin/sql/excel_enhanced.sql` 创建以下表：

- `sys_excel_export_task` - 异步导出任务表
- `sys_excel_import_log` - 导入日志表
- `sys_excel_import_error` - 导入错误详情表

同时更新现有表结构：
- `sys_excel_export_config` - 添加 `include_sample`、`allow_import` 字段
- `sys_excel_column_config` - 添加导入相关字段

---

## 列配置字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| importable | tinyint | 是否可导入（0-否，1-是） |
| required | tinyint | 是否必填（0-否，1-是） |
| example_value | varchar | 示例值（用于模板） |
| validation_rule | varchar | 校验规则（正则表达式） |
| validation_message | varchar | 校验失败提示信息 |

---

## 使用示例

### 前端调用示例（Vue 3）

```javascript
// 下载导入模板
async function downloadTemplate() {
  const res = await axios.get('/api/excel/template/user_export', {
    responseType: 'blob'
  });
  const url = window.URL.createObjectURL(new Blob([res.data]));
  const link = document.createElement('a');
  link.href = url;
  link.download = '用户导入模板.xlsx';
  link.click();
}

// 导入数据
async function importData(file) {
  const formData = new FormData();
  formData.append('file', file);
  
  const res = await axios.post('/api/excel/import/user_export', formData);
  console.log(res.data);
  
  if (res.data.failedRows > 0) {
    // 下载错误报告
    const errorRes = await axios.get(`/api/excel/error-report/${res.data.taskId}`, {
      responseType: 'blob'
    });
    // ... 下载处理
  }
}

// 异步导出
async function exportData() {
  const res = await axios.post('/api/excel/async-export/user_export', {
    fileName: '用户导出.xlsx'
  });
  
  const taskId = res.data.taskId;
  
  // 轮询状态
  const pollInterval = setInterval(async () => {
    const statusRes = await axios.get(`/api/excel/async-export/${taskId}`);
    
    if (statusRes.data.status === 1) {
      clearInterval(pollInterval);
      // 下载文件
      const fileRes = await axios.get(`/api/excel/async-export/download/${taskId}`, {
        responseType: 'blob'
      });
      // ... 下载处理
    }
  }, 2000);
}
```

---

## 注意事项

1. **临时文件清理** - 导出文件默认保留 24 小时，建议配置定时任务清理
2. **大文件处理** - 异步导出适合 1000+ 条数据，小数据量建议同步导出
3. **内存优化** - 导入使用流式处理，避免 OOM
4. **生产环境** - 建议将临时文件目录配置到独立磁盘，使用 Redis 存储任务状态
