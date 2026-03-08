# 定时任务管理 REST API 文档

## 任务配置管理

### 1. 分页查询任务列表
```http
GET /job/config/page?pageNo=1&pageSize=10
```

查询参数：
- pageNo: 页码（默认1）
- pageSize: 每页大小（默认10）
- jobName: 任务名称（模糊查询，可选）
- jobGroup: 任务分组（可选）
- executeMode: 执行模式（BEAN/HANDLER，可选）
- status: 状态（0-停止 1-运行，可选）

响应示例：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "jobName": "dataSync",
        "jobGroup": "DEFAULT",
        "description": "数据同步任务",
        "executorHandler": "dataSyncHandler",
        "cronExpression": "0 0/5 * * * ?",
        "status": 1,
        "executeMode": "HANDLER",
        "createTime": "2025-01-01 10:00:00"
      }
    ],
    "total": 1,
    "current": 1,
    "size": 10
  }
}
```

### 2. 查询任务详情
```http
GET /job/config/{id}
```

### 3. 添加任务
```http
POST /job/config
Content-Type: application/json

{
  "jobName": "dataCleanup",
  "jobGroup": "DEFAULT",
  "description": "数据清理任务",
  "executorHandler": "dataCleanupHandler",
  "cronExpression": "0 0 2 * * ?",
  "jobParam": "{\"days\": 30}",
  "status": 1,
  "executeMode": "HANDLER",
  "retryCount": 3
}
```

### 4. 更新任务
```http
PUT /job/config
Content-Type: application/json

{
  "id": 1,
  "jobName": "dataCleanup",
  "jobGroup": "DEFAULT",
  "description": "数据清理任务（已更新）",
  "executorHandler": "dataCleanupHandler",
  "cronExpression": "0 0 3 * * ?",
  "jobParam": "{\"days\": 60}",
  "status": 1,
  "executeMode": "HANDLER",
  "retryCount": 3
}
```

### 5. 删除任务
```http
DELETE /job/config/{id}
```

### 6. 启动任务
```http
POST /job/config/{id}/start
```

### 7. 停止任务
```http
POST /job/config/{id}/stop
```

### 8. 立即执行一次
```http
POST /job/config/{id}/trigger
```

### 9. 更新Cron表达式
```http
POST /job/config/{id}/cron?cronExpression=0 0/10 * * * ?
```

## 任务日志管理

### 1. 分页查询日志
```http
GET /job/log/page?pageNo=1&pageSize=10
```

查询参数：
- pageNo: 页码（默认1）
- pageSize: 每页大小（默认10）
- jobName: 任务名称（模糊查询，可选）
- jobGroup: 任务分组（可选）
- status: 状态（0-失败 1-成功，可选）

响应示例：
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "jobName": "dataSync",
        "jobGroup": "DEFAULT",
        "executorHandler": "dataSyncHandler",
        "triggerTime": "2025-01-01 10:00:00",
        "startTime": "2025-01-01 10:00:00",
        "endTime": "2025-01-01 10:00:05",
        "duration": 5000,
        "status": 1,
        "result": "同步成功，共处理100条数据"
      }
    ],
    "total": 1,
    "current": 1,
    "size": 10
  }
}
```

### 2. 查询日志详情
```http
GET /job/log/{id}
```

### 3. 清理日志
```http
DELETE /job/log/clean?days=30
```

参数说明：
- days: 保留最近N天的日志，默认30天

响应示例：
```json
{
  "code": 0,
  "msg": "success",
  "data": 150
}
```

## 常用Cron表达式示例

```
每5秒执行一次: */5 * * * * ?
每分钟执行一次: 0 * * * * ?
每5分钟执行一次: 0 0/5 * * * ?
每小时执行一次: 0 0 * * * ?
每天凌晨2点执行: 0 0 2 * * ?
每周一凌晨2点执行: 0 0 2 ? * MON
每月1号凌晨2点执行: 0 0 2 1 * ?
```

## 错误码说明

| 错误码 | 说明 |
|-------|------|
| 0 | 成功 |
| -1 | 系统错误 |
| 400 | 参数错误 |
| 404 | 任务不存在 |
| 500 | 服务器内部错误 |
