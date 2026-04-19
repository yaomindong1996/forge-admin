# AI 会话管理页面设计

## 1. 背景与目标

### 现状
- 后端已有 `ai_chat_session` 和 `ai_chat_record` 两张表，以及面向当前用户的会话 API（`/ai/session/list`、`/ai/session/{id}/messages`、`DELETE /ai/session/{id}`）
- 前端 `views/ai/` 下仅有 `provider.vue` 和 `model.vue`，**无会话管理页面**
- 现有 API 仅支持查询当前登录用户的会话，管理后台需要查看**所有用户**的会话数据

### 目标
- 开发管理员使用的 AI 会话管理页面，支持查看所有用户的会话和聊天记录
- 展示核心统计指标（会话总数、消息总数、今日会话、Token 消耗、30天趋势）
- 关联 sys_user 表显示用户昵称、头像等核心数据
- 前端页面美观、易操作、用户友好

## 2. 页面布局

采用**单页面集成**方案，一个页面包含统计区 + 左右分栏会话管理：

```
┌──────────────────────────────────────────────────┐
│  统计卡片区（4个 NStatistic）                       │
│  [会话总数] [消息总数] [今日会话] [Token消耗]         │
├──────────────────────────────────────────────────┤
│  趋势图（echarts 双Y轴：近30天每日会话数+消息数）      │
├────────────────┬─────────────────────────────────┤
│  会话列表(左)   │  聊天记录(右)                      │
│  🔍 搜索/筛选   │                                  │
│  ┌──────────┐ │  用户头像 昵称  14:30              │
│  │ 张三 会话  │ │  ┌───────────────────┐           │
│  │ 3条消息   │ │  │ 用户消息(蓝色气泡)  │           │
│  │ 10分钟前  │ │  └───────────────────┘           │
│  ├──────────┤ │                                  │
│  │ 李四 会话  │ │         AI助手  14:31            │
│  │ 8条消息   │ │           ┌───────────────┐     │
│  │ 1小时前   │ │           │ AI回复(灰色气泡)│     │
│  └──────────┘ │           └───────────────┘     │
│               │                                  │
└────────────────┴─────────────────────────────────┘
```

### 2.1 统计卡片区
- 4 个 NStatistic 卡片横排：会话总数、消息总数、今日会话数、Token 消耗总量
- 每个卡片带图标和数字，数字使用大字号

### 2.2 趋势图
- echarts 双Y轴折线图
- X轴：近30天日期
- 左Y轴：每日会话数（折线）
- 右Y轴：每日消息数（折线）
- 使用项目已有的 `echarts-theme.js` 主题适配

### 2.3 会话列表（左侧）
- 顶部：搜索框（用户昵称/会话标题）+ 时间范围选择器 + 状态下拉
- 列表项展示：用户头像 + 用户昵称 + 会话标题 + 消息条数 + 最后活跃时间
- 支持分页加载（滚动加载或分页器）
- 选中会话高亮
- 每项右侧有删除按钮

### 2.4 聊天记录（右侧）
- 类 IM 气泡样式：
  - 用户消息：靠右，蓝色气泡，显示用户头像+昵称+时间
  - AI 回复：靠左，灰色气泡，显示 AI 图标+时间
- 支持消息内容中 Markdown 渲染（代码块等）
- 消息区域自动滚动到底部
- 无选中会话时显示空状态提示

## 3. 后端 API 设计

新增**管理端** Controller，路径前缀 `/ai/admin/session`，不修改现有 AiChatController。

### 3.1 API 列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ai/admin/session/page` | 分页查询所有会话 |
| GET | `/ai/admin/session/{sessionId}/messages` | 查看会话消息记录 |
| DELETE | `/ai/admin/session/{sessionId}` | 删除会话（软删除） |
| GET | `/ai/admin/session/statistics` | 统计数据 |

### 3.2 分页查询会话 `GET /ai/admin/session/page`

**请求参数**（Query）：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页条数，默认10 |
| keyword | String | 否 | 搜索关键词（匹配用户昵称或会话标题） |
| startTime | String | 否 | 开始时间（yyyy-MM-dd） |
| endTime | String | 否 | 结束时间（yyyy-MM-dd） |
| status | String | 否 | 状态（0正常，1已删除） |

**响应数据**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": "session-uuid",
        "userId": 1,
        "nickName": "张三",
        "avatar": "file-id-or-url",
        "sessionName": "帮我写一个用户管理模块",
        "agentCode": "code-assistant",
        "status": "0",
        "messageCount": 8,
        "tokenUsage": 1234,
        "createTime": "2026-04-19 14:30:00",
        "updateTime": "2026-04-19 14:35:00"
      }
    ],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

**SQL 实现**：在 Mapper XML 中写自定义分页 SQL：
```sql
SELECT s.*, u.real_name AS nickName, u.avatar,
       (SELECT COUNT(*) FROM ai_chat_record r WHERE r.session_id = s.id) AS messageCount,
       (SELECT COALESCE(SUM(r.token_usage), 0) FROM ai_chat_record r WHERE r.session_id = s.id) AS tokenUsage
FROM ai_chat_session s
LEFT JOIN sys_user u ON s.user_id = u.id
WHERE s.tenant_id = #{tenantId}
  AND (#{keyword} IS NULL OR u.real_name LIKE CONCAT('%',#{keyword},'%') OR s.session_name LIKE CONCAT('%',#{keyword},'%'))
  AND (#{startTime} IS NULL OR s.create_time >= #{startTime})
  AND (#{endTime} IS NULL OR s.create_time <= #{endTime})
  AND (#{status} IS NULL OR s.status = #{status})
ORDER BY s.update_time DESC
```

### 3.3 查看会话消息 `GET /ai/admin/session/{sessionId}/messages`

复用现有 `AiChatRecordService.listBySession(sessionId)` 逻辑，但在管理端 Controller 中暴露。

**响应数据**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 123,
      "sessionId": "session-uuid",
      "role": "user",
      "content": "帮我写一个用户管理模块",
      "tokenUsage": 50,
      "createTime": "2026-04-19 14:30:00"
    },
    {
      "id": 124,
      "sessionId": "session-uuid",
      "role": "assistant",
      "content": "好的，我来帮你写...",
      "tokenUsage": 200,
      "createTime": "2026-04-19 14:30:05"
    }
  ]
}
```

### 3.4 删除会话 `DELETE /ai/admin/session/{sessionId}`

复用现有 `AiChatSessionService.deleteSession(sessionId)`，软删除（status=1）。

### 3.5 统计数据 `GET /ai/admin/session/statistics`

**响应数据**：
```json
{
  "code": 200,
  "data": {
    "totalSessions": 1234,
    "totalMessages": 5678,
    "todaySessions": 15,
    "totalTokenUsage": 98765,
    "dailyTrend": [
      { "date": "2026-03-20", "sessionCount": 10, "messageCount": 45 },
      { "date": "2026-03-21", "sessionCount": 12, "messageCount": 50 }
    ]
  }
}
```

**SQL 实现**：
```sql
-- 汇总统计
SELECT COUNT(*) AS totalSessions FROM ai_chat_session WHERE tenant_id = #{tenantId} AND status = '0';
SELECT COUNT(*) AS totalMessages FROM ai_chat_record WHERE tenant_id = #{tenantId};
SELECT COUNT(*) AS todaySessions FROM ai_chat_session WHERE tenant_id = #{tenantId} AND status = '0' AND DATE(create_time) = CURDATE();
SELECT COALESCE(SUM(token_usage), 0) AS totalTokenUsage FROM ai_chat_record WHERE tenant_id = #{tenantId};

-- 30天趋势
SELECT DATE(s.create_time) AS date,
       COUNT(DISTINCT s.id) AS sessionCount,
       (SELECT COUNT(*) FROM ai_chat_record r WHERE r.tenant_id = #{tenantId} AND DATE(r.create_time) = DATE(s.create_time)) AS messageCount
FROM ai_chat_session s
WHERE s.tenant_id = #{tenantId} AND s.create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY DATE(s.create_time)
ORDER BY date;
```

## 4. 数据模型变更

### 4.1 新增 DTO

**AiSessionPageQuery**（分页查询条件）：
| 字段 | 类型 | 说明 |
|------|------|------|
| pageNum | Integer | 页码 |
| pageSize | Integer | 每页条数 |
| keyword | String | 搜索关键词 |
| startTime | String | 开始时间 |
| endTime | String | 结束时间 |
| status | String | 状态 |

**AiSessionVO**（会话列表返回对象）：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 会话ID |
| userId | Long | 用户ID |
| nickName | String | 用户昵称（来自 sys_user.real_name） |
| avatar | String | 用户头像（来自 sys_user.avatar） |
| sessionName | String | 会话标题 |
| agentCode | String | Agent编码 |
| status | String | 状态 |
| messageCount | Integer | 消息条数 |
| tokenUsage | Integer | Token消耗 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

**AiSessionStatisticsVO**（统计数据返回对象）：
| 字段 | 类型 | 说明 |
|------|------|------|
| totalSessions | Long | 会话总数 |
| totalMessages | Long | 消息总数 |
| todaySessions | Long | 今日会话数 |
| totalTokenUsage | Long | Token消耗总量 |
| dailyTrend | List<DailyTrendItem> | 30天趋势 |

**DailyTrendItem**：
| 字段 | 类型 | 说明 |
|------|------|------|
| date | String | 日期 |
| sessionCount | Long | 会话数 |
| messageCount | Long | 消息数 |

### 4.2 无数据库表结构变更

使用现有的 `ai_chat_session` 和 `ai_chat_record` 表，无需新增或修改表。

## 5. 后端文件清单

```
forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/
├── admin/
│   └── controller/
│       └── AiSessionAdminController.java     # 管理端会话Controller
├── session/
│   ├── dto/
│   │   ├── AiSessionPageQuery.java           # 分页查询条件
│   │   └── AiSessionVO.java                  # 会话列表VO
│   ├── vo/
│   │   ├── AiSessionStatisticsVO.java        # 统计VO
│   │   └── DailyTrendItem.java               # 趋势项
│   ├── mapper/
│   │   └── AiChatSessionMapper.java          # 新增自定义方法
│   └── service/
│       └── AiChatSessionService.java         # 新增管理端方法
└── resources/mapper/
    └── AiChatSessionMapper.xml               # 新增自定义SQL
```

## 6. 前端文件清单

```
forge-admin-ui/src/
├── api/ai.js                                 # 新增会话管理API
└── views/ai/
    └── session.vue                           # AI会话管理页面
```

## 7. 前端技术要点

- 使用 Naive UI 组件：NStatistic、NCard、NInput、NDatePicker、NSelect、NList、NEmpty、NSpin
- echarts 折线图：复用 `@/utils/echarts-theme.js` 主题
- 聊天气泡：自定义 CSS 实现，用户消息蓝色靠右，AI回复灰色靠左
- 消息内容 Markdown 渲染
- 用户头像使用 `AuthImage` 组件（鉴权图片需带 token）
- 响应式布局：统计卡片横排自适应，左右分栏固定比例（3:7 或 2:3）

## 8. 风险与注意事项

1. **租户隔离**：所有查询必须加 `tenant_id` 条件，项目 TenantLineInnerInterceptor 会自动处理
2. **性能**：分页查询中 messageCount 子查询在大数据量下可能较慢，后续可考虑冗余字段或定时统计
3. **Token 消耗**：目前 `ai_chat_record.token_usage` 字段未被实际写入，统计值可能为 0，需确认后续是否补全
4. **权限**：管理端接口不加 `@ApiPermissionIgnore`，走正常权限体系，在 sys_resource 中配置菜单权限，仅管理员可访问

## 9. 测试策略

- 后端：手动测试分页查询（带/不带筛选条件）、统计接口、删除会话
- 前端：验证会话列表加载、搜索筛选、聊天记录展示、统计图表渲染、删除操作
- 边界：无会话数据的空状态、无选中会话的右侧提示、超长消息内容截断
