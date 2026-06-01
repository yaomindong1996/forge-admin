# 大屏动态数据接入 - 代码与文档分析报告

> 分析范围：`forge-project` 工程中与大屏动态数据接入相关的数据源（datasource）和数据集（dataset）模块
> 分析时间：2026-06-01

---

## 一、整体架构概览

大屏动态数据接入体系由以下核心模块组成：

```
forge-project/
├── forge/                          # 后端 Java（Spring Boot）
│   └── forge-framework/forge-plugin-parent/
│       └── forge-plugin-data/     # 数据源/数据集核心插件
│           ├── controller/        # REST API 控制器
│           ├── entity/            # 数据实体（表映射）
│           ├── service/           # 业务逻辑层
│           ├── support/           # 支撑类（SQL安全、参数绑定等）
│           └── vo/                # 视图对象
├── forge-admin-ui/                # 管理后台（Vue3 + Element Plus）
│   └── src/views/forge-plugin-data/
│       ├── connection/            # 数据连接管理
│       └── dataset/               # 数据集管理
├── forge-report-ui/               # 报表大屏前端（Vue3）
│   └── src/
│       ├── api/data/dataset.ts    # 数据集 API 封装
│       ├── api/http.ts            # 统一请求入口（customizeHttp）
│       ├── utils/
│       │   ├── datasetAdapter.ts         # 数据集→组件数据适配
│       │   └── requestDynamicParams.ts   # 动态参数解析
│       ├── hook/useChartDataFetch.hook.ts  # 图表数据获取 Hook
│       └── components/FgAI/aiEngine.ts    # AI 生成组件（含数据集绑定）
└── docs/                          # 设计文档
    └── superpowers/specs/2026-05-10-external-api-proxy-design.md
```

---

## 二、数据源（Data Connection）分析

### 2.1 数据源类型

系统支持通过 **JDBC 数据库连接** 方式接入数据，具体支持的数据库类型由 `DbTypeEnum` 枚举定义。

**后端实体**（`DataConnection.java`）：
```java
// 表：ai_report_data_connection
connectionCode      // 连接编码
connectionName      // 连接名称
dbType              // 数据库类型（MySQL/PostgreSQL/Oracle 等）
driverClassName     // JDBC 驱动类
jdbcUrl             // 连接地址
username            // 用户名
passwordCipher      // 密码（加密存储）
schemaName          // 默认 schema
testSql             // 测试 SQL
poolConfigJson      // 连接池配置（JSON）
status              // 0=禁用, 1=启用
```

**管理界面**（`connection.vue`）：
- 支持 CRUD 操作
- 即时"测试连接"功能（调用后端 `/data/connection/test`）
- 密码编辑时留空表示沿用原密码（安全设计）
- 支持浏览数据库表清单

### 2.2 连接管理 API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/data/connection/page` | POST | 分页查询连接列表 |
| `/data/connection/{id}` | GET | 查询单条连接详情 |
| `/data/connection` | POST | 新增连接 |
| `/data/connection` | PUT | 修改连接 |
| `/data/connection/{id}` | DELETE | 删除连接 |
| `/data/connection/test` | POST | 测试连接是否可用 |
| `/data/connection/{id}/tables` | GET | 获取连接下的表列表 |

### 2.3 安全设计

- **密码加密**：`passwordCipher` 字段存储加密后的密码，不可明文传输
- **行级权限**：通过 `DataDatasetRowScopeService` 实现数据行级过滤
- **字段级脱敏**：`DataDatasetField` 中的 `sensitiveLevel` + `maskRule` 控制字段脱敏
- **访问权限**：`DataDatasetAccessService` 控制数据集的 VIEW / QUERY 权限

---

## 三、数据集（Data Dataset）分析

### 3.1 数据集类型

系统支持两种数据集类型（`DatasetTypeEnum`）：

| 类型 | 说明 | 配置方式 |
|------|------|----------|
| `TABLE` | 单表数据集 | 选择表 + 字段，系统自动生成 SQL |
| `SQL` | SQL 数据集 | 手写 SQL 查询语句 |

**后端实体**（`DataDataset.java`）：
```java
// 表：ai_report_data_dataset
datasetCode        // 数据集编码（唯一标识）
datasetName        // 数据集名称
connectionId       // 关联的数据连接 ID
categoryId         // 分类 ID
datasetType        // TABLE / SQL
tableName          // 表名（TABLE 类型时使用）
sqlText            // SQL 文本（SQL 类型时使用）
paramSchemaJson    // 查询参数定义（JSON 数组）
defaultOrderJson   // 默认排序（JSON）
maxRows            // 最大返回行数（默认 1000）
timeoutSeconds     // 查询超时时间（秒）
cacheEnabled       // 是否启用缓存
cacheTtl           // 缓存 TTL（秒）
publishStatus      // 发布状态：DRAFT / PUBLISHED / OFFLINE
accessMode         // 访问模式
```

### 3.2 数据集字段定义

**字段实体**（`DataDatasetField.java`）：
```java
// 表：ai_report_data_dataset_field
fieldName          // 字段名（对应数据库列名）
fieldLabel         // 字段标签（中文显示名）
sourceColumn       // 源列名
dbType             // 数据库类型
dataType           // 前端数据类型
fieldRole          // DIMENSION（维度）/ MEASURE（指标）
defaultAgg         // 默认聚合方式
queryEnabled       // 是否允许作为查询条件
displayEnabled     // 是否允许在结果中显示
sensitiveLevel     // 敏感等级（HIDDEN / MASK / CLEAR）
maskRule           // 脱敏规则（正则表达式）
dictType           // 字典类型
dateFormat         // 日期格式
dataUnit           // 数据单位
dimensionId        // 关联维度 ID（用于维度翻译）
sort               // 排序
description        // 字段说明
```

**字段角色**（`FieldRoleEnum`）：
- `DIMENSION`：维度字段（用于分组、筛选）
- `MEASURE`：指标字段（用于聚合计算）

### 3.3 动态参数机制

数据集支持通过 `paramSchemaJson` 定义动态查询参数，由 `DatasetParamSchemaParser` 解析。

**参数定义结构**（`DatasetParamSchemaItem`）：
```json
[
  {
    "paramName": "startDate",      // 参数名
    "label": "开始日期",            // 参数标签
    "fieldName": "order_date",     // 映射的数据库字段
    "operator": ">=",              // 操作符：=, !=, >, >=, <, <=, LIKE
    "defaultValue": "T-7",         // 默认值（支持 T-N 格式）
    "required": true               // 是否必填
  }
]
```

**支持的运算符**：`=`, `!=`, `>`, `>=`, `<`, `<=`, `LIKE`

**参数绑定流程**（`DataQueryExecutor.java`）：
1. 解析 `paramSchemaJson` 得到参数定义列表
2. 从请求参数 `Map<String, Object> params` 中匹配参数值
3. 若请求中无值，使用 `defaultValue`
4. 必填参数无值时抛出业务异常
5. 将参数绑定到 SQL 的命名参数（`:paramName`）
6. 通过 `SqlParameterBinder` 转换为 `PreparedStatement` 参数

### 3.4 数据集 API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/data/dataset/page` | POST | 分页查询数据集列表 |
| `/data/dataset/{id}` | GET | 查询数据集详情 |
| `/data/dataset` | POST | 新增数据集 |
| `/data/dataset` | PUT | 修改数据集 |
| `/data/dataset/{id}` | DELETE | 删除数据集 |
| `/data/dataset/{id}/publish` | POST | 发布数据集 |
| `/data/dataset/{id}/offline` | POST | 下线数据集 |
| `/data/dataset/{id}/sync-fields` | POST | 从数据源同步字段 |
| `/data/dataset/{id}/preview` | POST | 预览数据集数据 |
| **`/data/dataset/runtime/query`** | **POST** | **运行时查询（大屏核心接口）** |
| `/data/dataset/runtime/{id}/metadata` | GET | 获取数据集元数据 |

---

## 四、大屏运行时数据接入流程

### 4.1 整体流程

```
大屏组件
  ↓ 请求数据（useChartDataFetch hook）
  ↓
customizeHttp() — 统一请求入口
  ↓ 判断 requestDataType
  ├── STATIC(0) → 返回静态 Mock 数据
  ├── AJAX(1)   → 直接 HTTP 请求（支持内部/外部接口）
  ├── Pond(2)   → 从数据池获取数据
  └── DATASET(3)→ datasetRequest() → 调用后端运行时查询
                                          ↓
                              /data/dataset/runtime/query
                                          ↓
                                  DataDatasetRuntimeController
                                          ↓
                                  DataQueryExecutor.execute()
                                          ↓
                              1. 构建 SQL（TABLE 模式自动生成 / SQL 模式直接使用）
                              2. 绑定动态参数（paramSchemaJson）
                              3. 应用行级权限条件
                              4. 执行 JDBC 查询
                              5. 维度翻译（dimensionId 关联）
                              6. 字段脱敏（maskRule）
                                          ↓
                              DataDatasetQueryResultVO
                              { dimensions, source, total, fields }
                                          ↓
                              datasetAdapter.adaptDatasetForComponent()
                                          ↓
                              适配为组件所需的数据格式
                                  ↓
                              渲染图表/表格
```

### 4.2 运行时查询接口详解

**请求** `POST /data/dataset/runtime/query`：
```json
{
  "datasetId": 123,
  "params": {
    "startDate": "2024-01-01",
    "endDate": "2024-12-31",
    "deptCode": "IT"
  },
  "fields": ["dept_name", "amount"],
  "pageNum": 1,
  "pageSize": 50,
  "maxRows": 1000,
  "outputMode": "ECHARTS_DATASET"
}
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "dimensions": ["dept_name", "amount"],
    "source": [
      {"dept_name": "研发部", "amount": 150000},
      {"dept_name": "市场部", "amount": 120000}
    ],
    "total": 2,
    "pageNum": 1,
    "pageSize": 50,
    "fields": [
      {
        "fieldName": "dept_name",
        "fieldLabel": "部门名称",
        "dataType": "string",
        "fieldRole": "DIMENSION"
      }
    ]
  }
}
```

**访问控制**（按顺序检查）：
1. 数据集是否存在且已发布
2. 数据集和连接是否启用
3. 当前用户是否有 QUERY 权限（`DataDatasetAccessService.requireAccess`）
4. 行级权限条件自动注入（`DataDatasetRowScopeService`）

### 4.3 前端数据适配机制

`datasetAdapter.ts` 是数据集数据适配的核心模块，负责将后端返回的统一格式转换为各组件所需的数据结构。

**适配模式**（`DatasetMappingMode`）：
```typescript
type DatasetMappingMode =
  | 'auto'           // 自动：根据组件类型智能选择
  | 'echartsDataset' // ECharts dataset 格式 { dimensions, source }
  | 'arrayRows'      // 数组行格式 [['name1', 100], ['name2', 200]]
  | 'objectRows'     // 对象数组格式 [{ name: 'name1', value: 100 }]
  | 'nameValue'      // 名称-值对 { name: 'xxx', value: 123 }
  | 'singleValue'    // 单值（KPI 卡片场景）
```

**组件类型自动适配规则**（`adaptDatasetForComponent`）：
- ECharts 组件 → `echartsDataset` 模式（dimensions + source）
- VChart 组件 → `echartsDataset` 模式
- TableScrollBoard → `arrayRows` 模式
- TableList → `nameValue` 模式
- KpiCard → `singleValue` 模式
- 其他 Common 组件 → `objectRows` 模式

### 4.4 动态参数绑定

`requestDynamicParams.ts` 支持 4 种参数来源：

| 来源 | 说明 | 示例 |
|------|------|------|
| `context` | 用户上下文（userId、username、deptCode 等 18 个字段） | `{ source: 'context', sourceKey: 'userId' }` |
| `pageContext` | 页面上下文（区域、对象等信息） | `{ source: 'pageContext', sourceKey: 'regionCode' }` |
| `component` | 其他组件的当前值 | `{ source: 'component', componentId: 'xxx', componentField: 'value' }` |
| `preset` | 预设值（T-N 日期偏移） | `{ source: 'preset', presetType: 'tn-day-start', offsetDays: 7 }` |

**preset 类型**：
- `tn-day-start`：当前日期 00:00:00 偏移 N 天
- `tn-day-end`：当前日期 23:59:59 偏移 N 天

---

## 五、外部接口代理（External API Proxy）

设计文档：`docs/superpowers/specs/2026-05-10-external-api-proxy-design.md`

### 5.1 解决的问题

1. **跨域问题**：前端直接请求外部接口受 CORS 限制
2. **认证信息安全**：API Key / Token 不应暴露在前端代码中
3. **多种认证方式**：Bearer Token、API Key（Header/Query）、Basic Auth 等

### 5.2 实现方案

通过 `forge-plugin-external` 插件实现服务端代理转发：

```
前端 → /forge-report-api/external/proxy/{externalApiId}
                ↓
        ExternalApiProxyController
                ↓
        认证策略（策略模式）
                ↓
        外部接口请求
                ↓
        数据适配（JSONPath / 脚本）
                ↓
        返回标准化数据
```

**认证策略**：
- `NONE`：无认证
- `BEARER_TOKEN`：Bearer Token
- `API_KEY_HEADER`：API Key 放在 Header
- `API_KEY_QUERY`：API Key 放在 Query 参数
- `BASIC_AUTH`：Basic Authentication
- `CUSTOM_HEADER`：自定义 Header

---

## 六、AI 生成大屏时的数据集绑定

`aiEngine.ts` 中的 `applyDatasetRequest` 函数负责将 AI 生成结果中的数据集信息绑定到组件。

**AI 返回的数据集配置**：
```json
{
  "key": "BarLine",
  "request": {
    "datasetId": 123,
    "datasetName": "部门业绩表",
    "datasetFields": ["dept_name", "amount", "target"],
    "datasetParams": { "year": 2024 },
    "datasetPageSize": 50,
    "datasetMaxRows": 1000,
    "datasetOutputMode": "ECHARTS_DATASET",
    "datasetMapping": {
      "mode": "auto",
      "fieldMap": { "xAxis": "dept_name", "yAxis": "amount" },
      "syncHeader": true
    }
  }
}
```

**绑定后的组件请求配置**：
```typescript
instance.request.requestDataType = RequestDataTypeEnum.DATASET  // 3
instance.request.datasetId = 123
instance.request.datasetFields = ['dept_name', 'amount', 'target']
instance.request.datasetParams = { year: 2024 }
instance.request.datasetMapping = { mode: 'auto', ... }
```

---

## 七、关键设计亮点

### 7.1 安全性设计
- 密码加密存储（`passwordCipher`）
- SQL 安全校验（`SqlSafetyValidator`，防止 SQL 注入）
- 行级数据权限（可配置的数据范围过滤）
- 字段级脱敏（支持正则脱敏规则）
- 数据集访问权限（VIEW / QUERY 分离）

### 7.2 灵活性设计
- 支持 `TABLE` 和 `SQL` 两种数据集模式
- 动态参数支持多种来源（用户上下文、组件联动、预设值）
- 数据适配支持 6 种模式，覆盖所有组件类型
- 外部接口支持多种认证方式

### 7.3 性能设计
- 查询结果缓存（`cacheEnabled` + `cacheTtl`）
- 最大行数限制（`maxRows`，默认 1000）
- 查询超时控制（`timeoutSeconds`）
- 定时轮询机制（大屏自动刷新）

---

## 八、待确认/待补充内容

以下信息在当前代码和文档中未找到明确说明，建议向开发团队确认：

1. **WebSocket 数据源**：任务描述中提到 WebSocket 作为数据源类型，但当前代码中未发现 WebSocket 数据接入的实现，需确认是否为规划中功能

2. **数据池（Pond）机制**：`RequestDataTypeEnum.Pond(2)` 在数据获取流程中有使用，但具体的数据池管理和刷新机制需要进一步分析

3. **缓存实现细节**：`cacheEnabled` / `cacheTtl` 字段已定义，但缓存的具体实现（Redis / Caffeine / 其他）需要查看 `DataQueryExecutor` 的完整实现确认

4. **变更文档**：任务要求搜索变更文档中关于 report datasource/dataset 的内容，但 `CHANGELOG.md` 仅有初始版本记录，未找到专项变更说明，建议确认是否有独立的变更日志文件或数据库迁移脚本

---

## 九、附录：关键文件路径索引

| 模块 | 文件路径 |
|------|----------|
| 数据连接控制器 | `forge/.../controller/DataConnectionController.java` |
| 数据集控制器 | `forge/.../controller/DataDatasetController.java` |
| 运行时查询控制器 | `forge/.../controller/DataDatasetRuntimeController.java` |
| 查询执行器 | `forge/.../service/DataQueryExecutor.java` |
| 数据集参数解析 | `forge/.../support/DatasetParamSchemaParser.java` |
| 前端统一请求 | `forge-report-ui/src/api/http.ts` |
| 数据集数据适配 | `forge-report-ui/src/utils/datasetAdapter.ts` |
| 动态参数解析 | `forge-report-ui/src/utils/requestDynamicParams.ts` |
| 图表数据获取 Hook | `forge-report-ui/src/hook/useChartDataFetch.hook.ts` |
| AI 引擎（数据集绑定） | `forge-report-ui/src/components/FgAI/aiEngine.ts` |
| 数据集管理页面 | `forge-admin-ui/src/views/forge-plugin-data/dataset/dataset.vue` |
| 数据连接管理页面 | `forge-admin-ui/src/views/forge-plugin-data/connection/connection.vue` |
| 外部接口代理设计 | `docs/superpowers/specs/2026-05-10-external-api-proxy-design.md` |
