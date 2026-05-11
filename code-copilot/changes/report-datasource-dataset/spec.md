# 平台级数据连接与数据集能力
> status: review
> created: 2026-05-11
> complexity: 🔴高

## 1. 背景与目标
当前 `forge-report-ui` 报表动态数据主要依赖内部接口、外部接口或静态数据。业务希望新增一种直接通过数据库查询渲染报表的数据加载方式：用户在 admin 端维护数据连接和数据集，报表组件选择数据集后即可按配置查询数据库并渲染，不需要再为每个图表单独开发数据接口。

该能力不能只服务大屏报表。后续 AI 生成报表、ChatBI、数据问答、自动指标推荐等场景都需要复用数据连接、数据集、字段元数据和统一查询执行能力，因此本次按平台级数据资产能力设计。

目标：
- 新增平台级「数据连接」管理，支持配置数据库连接信息、测试连接、读取表和字段元数据。
- 新增平台级「数据集」管理，支持单表数据集和 SQL 数据集。
- 新增数据集字段配置，维护字段显示名、类型、是否维度/指标、聚合方式、筛选能力、脱敏策略等元数据。
- `forge-report-ui` 新增「数据库数据集」数据加载模式，选择数据连接/数据集后直接查询数据库渲染图表。
- 数据集查询支持现有动态请求参数注入机制，复用登录人上下文、画布筛选组件、预设时间范围、自定义值。
- 查询执行具备基本安全边界：只允许查询类 SQL，参数绑定防注入，限制返回条数和超时时间，敏感信息不返回前端。

## 2. 设计原则
- **平台级复用**：数据连接、数据集、字段配置放在独立业务插件中，不能放在 `forge-report-server` 私有包或报表私有表中。
- **元数据优先**：报表、AI 报表、ChatBI 都读取同一套字段语义，避免每个场景重复解析数据库结构。
- **先满足当前范围**：首期支持单表查询和手写复杂查询 SQL，不实现多表可视化 join 建模。
- **安全默认值**：默认只读查询，默认限制行数，密码密文存储，查询 SQL 禁止 DDL/DML、多语句和危险函数。
- **动态参数统一**：报表侧继续使用现有 `dynamicRequestParams` 结构，不再引入第二套参数表达式。
- **兼容现有报表**：已有静态数据、接口动态数据不受影响；新能力作为新增数据加载模式存在。

## 3. 现状与约束
### 3.1 现有能力
- `forge-report-ui` 已有动态接口数据配置和 `dynamicRequestParams` 机制，可把登录人上下文、筛选组件值、预设时间范围等注入请求参数。
- `forge-report-ui/src/api/http.ts` 是报表组件动态数据请求执行入口。
- `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.d.ts` 中 `RequestConfigType` 已保存动态请求相关配置。
- `forge-report-server` 依赖 `forge-plugin-system`、`forge-plugin-ai`、`forge-plugin-external`，可以继续依赖新增平台插件。
- `forge-admin-server` 已依赖 generator、external、ai 等业务插件，可以增加数据资产管理插件。

### 3.2 不能直接复用的点
- `forge-plugin-generator` 里已有 `gen_datasource`，但它是代码生成器私有模型，不适合作为平台级数据连接：
  - 表名、字段名偏 generator 领域。
  - 缺少 `tenant_id`、`create_dept`、`schema_name` 等平台标准字段。
  - 密码加密逻辑位于 generator service，且密钥硬编码，不适合沉淀为通用能力。
  - 表结构和 API 面向代码生成，不包含数据集、字段语义、查询执行权限、数据脱敏等配置。
- 因此本次新增平台级表和插件，不在 `gen_datasource` 上继续扩展。

### 3.3 项目约束
- 后端复杂查询 SQL 必须写 Mapper XML；跨库业务查询执行器使用 JDBC PreparedStatement，不通过 MyBatis 拼接动态 SQL。
- 业务表必须包含标准字段：`id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`。
- 前端分页参数使用 `pageNum` + `pageSize`。
- 启用前端加密链路的管理接口必须补齐 `@ApiEncrypt` / `@ApiDecrypt`。
- 密码、连接串等敏感数据禁止明文返回前端；日志中禁止打印密码和完整 JDBC URL 中的敏感参数。

## 4. 范围
### 4.1 本期包含
- 数据连接 CRUD、启停、测试连接。
- 数据连接下的数据库表列表、字段列表读取。
- 数据集 CRUD，支持两种类型：
  - `TABLE`：单表数据集，基于表名、字段、筛选、排序生成查询 SQL。
  - `SQL`：SQL 数据集，由用户输入查询 SQL，系统做安全校验和参数绑定。
- 数据集字段配置 CRUD 或批量保存。
- 数据集预览与运行时执行 API。
- 报表组件选择数据加载模式为「数据库数据集」，并配置：
  - 数据连接。
  - 数据集。
  - 输出字段映射。
  - 动态参数绑定。
  - 分页/限制条数。
- 查询结果统一转换为图表可消费的 dataset 结构。

### 4.2 本期不包含
- 可视化多表 join 建模。
- 指标血缘、影响分析、数据质量评分。
- 行级权限配置 UI。
- 跨数据源联邦查询。
- 查询缓存、物化视图、定时抽取。
- AI 自动生成 SQL 的完整闭环；本期只为后续 ChatBI 预留元数据和执行接口。

## 5. 术语
| 术语 | 说明 |
|------|------|
| 数据连接 | 一个可访问数据库的连接配置，例如 MySQL 数据库连接 |
| 数据集 | 可被报表/AI/ChatBI 使用的数据查询定义，可能是单表或 SQL |
| 字段配置 | 数据集输出字段的业务语义和展示配置 |
| 运行时查询 | 报表预览、发布页、AI 分析等场景按数据集执行查询 |
| 动态参数 | 来自登录人上下文、画布筛选组件、预设值或自定义值的查询参数 |

## 6. 总体架构
新增平台级插件：

```text
forge/forge-framework/forge-plugin-parent/forge-plugin-data/
├── controller/
│   ├── DataConnectionController.java
│   ├── DataDatasetController.java
│   └── DataDatasetRuntimeController.java
├── service/
│   ├── DataConnectionService.java
│   ├── DataDatasetService.java
│   ├── DataDatasetFieldService.java
│   └── DataQueryExecutor.java
├── service/impl/
├── mapper/
├── entity/
├── dto/
├── vo/
├── enums/
├── support/
│   ├── JdbcDataSourceProvider.java
│   ├── SqlSafetyValidator.java
│   ├── SqlParameterBinder.java
│   ├── TableQueryBuilder.java
│   └── DatasetResultMapper.java
└── resources/mapper/
```

依赖关系：
- `forge-plugin-data` 是平台业务插件。
- `forge-admin-server` 依赖 `forge-plugin-data`，提供 admin 端管理页面 API。
- `forge-report-server` 依赖 `forge-plugin-data`，提供报表运行时查询 API。
- 后续 AI 报表和 ChatBI 继续依赖 `forge-plugin-data`，复用数据集元数据和查询执行器。

前端入口：
- `forge-admin-ui`：新增数据资产管理菜单，用于维护数据连接和数据集。
- `forge-report-ui`：新增报表组件数据加载模式「数据库数据集」。

## 7. 数据模型
### 7.1 data_connection
平台级数据连接表。

```sql
CREATE TABLE `ai_report_data_connection` (
  `id` bigint NOT NULL COMMENT '主键ID', -- 自增
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `connection_code` varchar(64) NOT NULL COMMENT '连接编码',
  `connection_name` varchar(100) NOT NULL COMMENT '连接名称',
  `db_type` varchar(32) NOT NULL COMMENT '数据库类型：MYSQL/POSTGRESQL/ORACLE/SQLSERVER',
  `driver_class_name` varchar(200) NOT NULL COMMENT '驱动类名',
  `jdbc_url` varchar(1000) NOT NULL COMMENT 'JDBC连接地址',
  `username` varchar(128) NOT NULL COMMENT '用户名',
  `password_cipher` varchar(500) DEFAULT NULL COMMENT '密码密文',
  `schema_name` varchar(128) DEFAULT NULL COMMENT '模式名/数据库名',
  `test_sql` varchar(200) NOT NULL DEFAULT 'SELECT 1' COMMENT '测试SQL',
  `pool_config_json` json DEFAULT NULL COMMENT '连接池配置JSON',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_connection_code_tenant` (`tenant_id`, `connection_code`),
  KEY `idx_data_connection_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台数据连接';
```

字段规则：
- `connection_code` 同租户唯一，用于 AI、ChatBI 通过稳定编码引用。
- `password_cipher` 只存密文，新增/修改时加密；查询详情返回 `hasPassword=true`，不返回密文。
- `jdbc_url` 详情页可返回脱敏值，编辑时密码留空表示不修改。
- `schema_name` 用于 MySQL database、PostgreSQL schema、Oracle schema 等元数据查询范围。
- `pool_config_json` 首期可为空，后续扩展最大连接数、最小空闲、连接超时等配置。

### 7.2 data_dataset
平台级数据集表。

```sql
CREATE TABLE `ai_report_data_dataset` (
  `id` bigint NOT NULL COMMENT '主键ID', -- 自增
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `dataset_code` varchar(64) NOT NULL COMMENT '数据集编码',
  `dataset_name` varchar(100) NOT NULL COMMENT '数据集名称',
  `connection_id` bigint NOT NULL COMMENT '数据连接ID',
  `dataset_type` varchar(20) NOT NULL COMMENT '数据集类型：TABLE/SQL',
  `table_name` varchar(200) DEFAULT NULL COMMENT '单表模式表名',
  `sql_text` longtext DEFAULT NULL COMMENT 'SQL模式查询SQL',
  `param_schema_json` json DEFAULT NULL COMMENT '参数定义JSON',
  `default_order_json` json DEFAULT NULL COMMENT '默认排序JSON',
  `max_rows` int NOT NULL DEFAULT 1000 COMMENT '最大返回行数',
  `timeout_seconds` int NOT NULL DEFAULT 15 COMMENT '查询超时时间秒',
  `cache_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用缓存：1是 0否',
  `cache_ttl_seconds` int DEFAULT NULL COMMENT '缓存秒数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_code_tenant` (`tenant_id`, `dataset_code`),
  KEY `idx_data_dataset_connection` (`tenant_id`, `connection_id`),
  KEY `idx_data_dataset_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台数据集';
```

字段规则：
- `dataset_type=TABLE` 时 `table_name` 必填，`sql_text` 为空。
- `dataset_type=SQL` 时 `sql_text` 必填，`table_name` 可为空。
- `param_schema_json` 定义 SQL 参数或单表筛选参数，供报表配置、AI、ChatBI 识别。
- `max_rows` 默认 1000，运行时请求的 limit 不得超过该值。
- `timeout_seconds` 默认 15，避免慢 SQL 拖垮报表页。

`param_schema_json` 示例：

```json
[
  {
    "paramName": "startTime",
    "label": "开始时间",
    "dataType": "DATETIME",
    "required": false,
    "defaultValue": null,
    "operator": ">=",
    "fieldName": "create_time"
  },
  {
    "paramName": "regionCode",
    "label": "区域编码",
    "dataType": "STRING",
    "required": false,
    "defaultValue": null,
    "operator": "=",
    "fieldName": "region_code"
  }
]
```

### 7.3 data_dataset_field
数据集字段配置表。

```sql
CREATE TABLE `ai_report_data_dataset_field` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `dataset_id` bigint NOT NULL COMMENT '数据集ID',
  `field_name` varchar(128) NOT NULL COMMENT '字段名/SQL别名',
  `field_label` varchar(128) DEFAULT NULL COMMENT '字段显示名',
  `source_column` varchar(128) DEFAULT NULL COMMENT '来源列名',
  `db_type` varchar(100) DEFAULT NULL COMMENT '数据库字段类型',
  `data_type` varchar(32) NOT NULL DEFAULT 'STRING' COMMENT '标准类型：STRING/NUMBER/DATE/DATETIME/BOOLEAN',
  `field_role` varchar(20) NOT NULL DEFAULT 'DIMENSION' COMMENT '字段角色：DIMENSION/MEASURE',
  `default_agg` varchar(20) DEFAULT NULL COMMENT '默认聚合：SUM/COUNT/AVG/MAX/MIN',
  `query_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否可筛选',
  `display_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否可展示',
  `sensitive_level` varchar(20) NOT NULL DEFAULT 'NONE' COMMENT '敏感级别：NONE/MASK/HIDDEN',
  `mask_rule` varchar(100) DEFAULT NULL COMMENT '脱敏规则',
  `dict_type` varchar(100) DEFAULT NULL COMMENT '字典类型',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_field` (`tenant_id`, `dataset_id`, `field_name`),
  KEY `idx_data_dataset_field_dataset` (`tenant_id`, `dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台数据集字段';
```

字段规则：
- `field_name` 对 SQL 数据集必须取查询结果列别名。
- `field_role=DIMENSION` 适合分组、分类轴、筛选项。
- `field_role=MEASURE` 适合数值展示和聚合。
- `sensitive_level=HIDDEN` 的字段不返回运行时查询结果。
- `sensitive_level=MASK` 的字段返回前按 `mask_rule` 脱敏。

## 8. 查询模型
### 8.1 TABLE 数据集
TABLE 数据集由系统生成查询：

```sql
SELECT <selected_fields>
FROM <safe_table_name>
WHERE 1 = 1
  AND <field_filter_1>
  AND <field_filter_2>
ORDER BY <safe_order_fields>
LIMIT ?
```

规则：
- 表名必须来自连接元数据或数据集已保存的 `table_name`，禁止前端直接传任意表名执行。
- 字段名必须来自 `data_dataset_field`，禁止前端直接传任意字段名执行。
- 筛选条件来自 `param_schema_json` 和动态参数解析结果。
- 参数值全部通过 PreparedStatement 绑定。
- 查询字段默认使用 `display_enabled=1` 的字段。

### 8.2 SQL 数据集
SQL 数据集由管理员输入查询 SQL，例如：

```sql
SELECT
  city_code AS cityCode,
  city_name AS cityName,
  SUM(order_amount) AS orderAmount
FROM ads_city_order
WHERE stat_date >= :startTime
  AND stat_date <= :endTime
  AND (:cityCode IS NULL OR city_code = :cityCode)
GROUP BY city_code, city_name
ORDER BY orderAmount DESC
```

规则：
- 只允许单条 `SELECT` 或 `WITH ... SELECT` 查询。
- 禁止 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`CREATE`、`TRUNCATE`、`MERGE`、`CALL`、`EXEC`。
- 禁止多语句分号执行。
- 参数占位符使用 `:paramName`，执行前转换为 `?` 并绑定参数。
- SQL 保存时做安全校验，预览/运行时再次校验。
- 必须包一层限制行数，避免用户 SQL 自带无限查询：

```sql
SELECT * FROM (
  <validated_sql>
) t
LIMIT ?
```

不同数据库 limit 方言后续通过 `DbDialect` 扩展，首期 MySQL 优先。

### 8.3 参数类型转换
| 标准类型 | 输入来源 | 后端绑定类型 |
|----------|----------|--------------|
| STRING | 文本、下拉、区域 | String |
| NUMBER | 数字输入、分页 | BigDecimal / Long |
| DATE | 日期选择 | LocalDate |
| DATETIME | 日期时间、T-N预设 | LocalDateTime |
| BOOLEAN | 开关 | Boolean |

动态参数为空时：
- `required=true`：返回参数校验错误，不执行查询。
- `required=false`：TABLE 查询跳过该条件；SQL 查询绑定 null。

## 9. 后端接口设计
所有管理接口走 `forge-admin-server`，运行时接口可同时由 `forge-admin-server` 和 `forge-report-server` 暴露。开启前端加密链路的接口必须按请求/响应补齐 `@ApiDecrypt` / `@ApiEncrypt`。

### 9.1 数据连接接口
| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 分页 | `/data/connection/page` | GET | 按名称、类型、状态分页 |
| 列表 | `/data/connection/list` | GET | 返回启用连接，用于下拉 |
| 详情 | `/data/connection/:id` | GET | 返回脱敏详情 |
| 新增 | `/data/connection` | POST | 新增连接，密码加密 |
| 修改 | `/data/connection` | PUT | 修改连接，密码为空则不变 |
| 删除 | `/data/connection/:id` | DELETE | 被数据集引用时禁止删除 |
| 测试 | `/data/connection/:id/test` | POST | 测试已保存连接 |
| 临时测试 | `/data/connection/test` | POST | 测试未保存连接 |
| 表列表 | `/data/connection/:id/tables` | GET | 按关键字分页读取数据库表 |
| 字段列表 | `/data/connection/:id/tables/:tableName/fields` | GET | 读取表字段 |

详情响应示例：

```json
{
  "id": 1001,
  "connectionCode": "sales_dw",
  "connectionName": "销售数仓",
  "dbType": "MYSQL",
  "driverClassName": "com.mysql.cj.jdbc.Driver",
  "jdbcUrl": "jdbc:mysql://10.***.***.15:3306/sales_dw",
  "username": "report_user",
  "hasPassword": true,
  "schemaName": "sales_dw",
  "status": 1,
  "description": "销售分析数据源"
}
```

### 9.2 数据集接口
| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 分页 | `/data/dataset/page` | GET | 按名称、连接、类型、状态分页 |
| 列表 | `/data/dataset/list` | GET | 返回启用数据集，用于报表选择 |
| 详情 | `/data/dataset/:id` | GET | 返回数据集和字段配置 |
| 新增 | `/data/dataset` | POST | 新增 TABLE 或 SQL 数据集 |
| 修改 | `/data/dataset` | PUT | 修改数据集 |
| 删除 | `/data/dataset/:id` | DELETE | 被报表引用时允许提示风险，首期可软约束 |
| 同步字段 | `/data/dataset/:id/sync-fields` | POST | 从表或 SQL 预览结果同步字段 |
| 字段保存 | `/data/dataset/:id/fields` | PUT | 批量保存字段配置 |
| 预览 | `/data/dataset/:id/preview` | POST | 管理端预览数据 |

数据集保存请求示例：

```json
{
  "datasetCode": "sales_city_day",
  "datasetName": "城市日销售数据集",
  "connectionId": 1001,
  "datasetType": "TABLE",
  "tableName": "ads_city_sales_day",
  "paramSchema": [
    {
      "paramName": "startTime",
      "label": "开始时间",
      "dataType": "DATE",
      "required": false,
      "operator": ">=",
      "fieldName": "stat_date"
    }
  ],
  "maxRows": 1000,
  "timeoutSeconds": 15,
  "status": 1
}
```

### 9.3 运行时查询接口
| 操作 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 执行数据集 | `/data/dataset/runtime/query` | POST | 报表/AI/ChatBI 统一查询入口 |
| 获取元数据 | `/data/dataset/runtime/:id/metadata` | GET | 返回数据集字段、参数定义、类型信息 |

运行时查询请求：

```json
{
  "datasetId": 10001,
  "params": {
    "startTime": "2026-05-01",
    "endTime": "2026-05-11",
    "cityCode": "110000"
  },
  "fields": ["cityName", "orderAmount"],
  "pageNum": 1,
  "pageSize": 50,
  "outputMode": "ECHARTS_DATASET"
}
```

运行时查询响应：

```json
{
  "dimensions": ["cityName", "orderAmount"],
  "source": [
    { "cityName": "北京", "orderAmount": 120000 },
    { "cityName": "上海", "orderAmount": 98000 }
  ],
  "total": 2,
  "pageNum": 1,
  "pageSize": 50,
  "fields": [
    { "fieldName": "cityName", "fieldLabel": "城市", "dataType": "STRING", "fieldRole": "DIMENSION" },
    { "fieldName": "orderAmount", "fieldLabel": "订单金额", "dataType": "NUMBER", "fieldRole": "MEASURE" }
  ]
}
```

## 10. 前端设计
### 10.1 forge-admin-ui 管理端
新增菜单建议：

```text
数据资产
├── 数据连接
└── 数据集管理
```

数据连接页面：
- 使用 `AiCrudPage`。
- 搜索：连接名称、数据库类型、状态。
- 列表：连接名称、连接编码、数据库类型、模式名、用户名、状态、更新时间。
- 表单：数据库类型、连接名称、连接编码、连接地址、驱动、用户、密码、模式名、测试 SQL、描述、状态。
- 操作：编辑、测试连接、查看表、删除。

数据集页面：
- 使用 `AiCrudPage` 加定制弹窗。
- 搜索：数据集名称、数据连接、数据集类型、状态。
- 基础配置：数据集名称、编码、连接、类型、表名或 SQL、最大行数、超时、状态、描述。
- 字段配置 Tab：字段名、显示名、标准类型、角色、聚合、是否可筛选、是否可展示、敏感级别、字典类型、排序。
- 参数配置 Tab：参数名、标签、类型、是否必填、字段名、操作符、默认值。
- 预览 Tab：输入参数后执行 `/data/dataset/:id/preview`。

### 10.2 forge-report-ui 报表配置
在图表数据配置中新增数据加载模式：

```ts
type DataFetchMode = 'static' | 'ajax' | 'request' | 'dataset'
```

新增请求配置字段：

```ts
interface DatasetRequestConfig {
  datasetId?: string | number
  datasetName?: string
  datasetFields?: string[]
  datasetParams?: Record<string, any>
  dynamicRequestParams?: DynamicRequestParamBinding[]
  pageNum?: number
  pageSize?: number
  maxRows?: number
  outputMode?: 'ECHARTS_DATASET'
}
```

配置流程：
- 选择「数据库数据集」。
- 选择数据连接，联动加载该连接下启用数据集。
- 选择数据集后加载字段和参数定义。
- 选择要输出的字段，默认全选可展示字段。
- 使用现有动态参数弹窗，把筛选组件值、登录人上下文、T-N 预设时间绑定到数据集参数。
- 预览数据，成功后写入当前组件 `option.dataset`。

运行时流程：
1. 组件进入数据刷新逻辑。
2. 判断当前模式为 `dataset`。
3. 调用 `resolveDynamicRequestParams()` 解析动态参数。
4. 将解析后的 `Params` 合并为 `params`。
5. 调用 `/forge-report-api/data/dataset/runtime/query`。
6. 把响应中的 `{ dimensions, source }` 写入图表 `option.dataset`。

### 10.3 报表 JSON 保存格式
组件配置中保存数据集模式：

```json
{
  "requestConfig": {
    "requestDataType": "dataset",
    "datasetId": 10001,
    "datasetName": "城市日销售数据集",
    "datasetFields": ["cityName", "orderAmount"],
    "pageNum": 1,
    "pageSize": 50,
    "maxRows": 1000,
    "dynamicRequestParams": [
      {
        "id": "param_startTime",
        "enabled": true,
        "target": "Params",
        "paramName": "startTime",
        "source": "preset",
        "sourceKey": "relativeTimeRange.start",
        "presetType": "relativeTimeRange",
        "presetOffsetDays": -7
      }
    ]
  }
}
```

## 11. 安全设计
### 11.1 密码安全
- 密码只在新增/修改时接收明文，立即加密存储。
- 详情、分页、列表接口不返回 `password_cipher`。
- 编辑时密码为空表示不修改原密码。
- 测试连接日志禁止打印密码。
- 后续可将密钥接入配置中心或 KMS，首期不能使用硬编码常量。

### 11.2 SQL 安全
- SQL 数据集只允许查询语句。
- 禁止多语句、DDL、DML、存储过程、危险函数。
- 参数只允许 `:paramName` 形式，不允许字符串拼接。
- 所有参数使用 PreparedStatement 绑定。
- 表名、字段名从元数据白名单选择。
- 单次查询必须设置超时和最大返回行数。

### 11.3 权限与审计
- 数据连接管理权限：
  - `data:connection:list`
  - `data:connection:add`
  - `data:connection:edit`
  - `data:connection:remove`
  - `data:connection:test`
- 数据集管理权限：
  - `data:dataset:list`
  - `data:dataset:add`
  - `data:dataset:edit`
  - `data:dataset:remove`
  - `data:dataset:preview`
- 运行时查询权限首期可要求登录态；后续按数据集授权。
- 新增、修改、删除、测试连接、预览 SQL 需要记录操作日志。

## 12. 扩展性设计
### 12.1 支持 AI 生成报表
AI 生成报表可读取：
- 数据集名称和描述。
- 字段列表、字段角色、类型、聚合方式。
- 参数定义。
- 预览样例数据。

AI 不直接读取密码或 JDBC URL，只通过数据集元数据和运行时查询接口工作。

### 12.2 支持 ChatBI
ChatBI 可基于 `data_dataset` 和 `data_dataset_field` 构建语义层：
- 维度字段用于分组和筛选。
- 指标字段用于聚合。
- `default_agg` 作为默认指标聚合方式。
- `dict_type` 作为枚举值解释来源。
- `param_schema_json` 作为可问答筛选条件。

### 12.3 支持更多数据库
预留 `DbDialect`：

```text
DbDialect
├── quoteIdentifier(name)
├── buildLimitSql(sql, limit)
├── buildPagedSql(sql, pageNum, pageSize)
├── queryTables(connection, schemaName, keyword, page)
└── queryColumns(connection, schemaName, tableName)
```

首期实现 MySQL；PostgreSQL、Oracle、SQLServer 后续按同一接口扩展。

## 13. 业务规则
- 禁用的数据连接不能被新建数据集选择。
- 已被数据集引用的数据连接不能删除。
- 禁用的数据集不能被报表新选择，但已保存报表回显时要展示名称并提示已禁用。
- TABLE 数据集必须选择表并同步字段后才能启用。
- SQL 数据集必须通过 SQL 安全校验和字段同步后才能启用。
- 数据集字段全部禁用展示时，运行时查询返回错误。
- 报表发布后运行时查询失败，不影响页面加载，但组件显示数据加载失败状态。
- 报表配置保存时只保存 `datasetId`、字段选择和动态参数，不保存连接密码、SQL 明文副本。

## 14. 错误码与提示
| 场景 | 提示 |
|------|------|
| 数据连接不存在 | 数据连接不存在或已删除 |
| 数据连接禁用 | 数据连接已禁用 |
| 测试连接失败 | 数据连接测试失败，请检查地址、用户、密码和网络 |
| 数据集不存在 | 数据集不存在或已删除 |
| 数据集禁用 | 数据集已禁用 |
| SQL 不安全 | SQL 仅允许单条查询语句 |
| 参数缺失 | 缺少必填参数：{paramName} |
| 查询超时 | 数据集查询超时，请缩小筛选范围 |
| 返回行数超限 | 查询结果超过最大行数限制 |

## 15. 影响范围
后端：
- 新增 `forge-plugin-data`。
- 修改 `forge-framework/forge-plugin-parent/pom.xml` 增加模块。
- 修改 `forge-admin-server/pom.xml` 增加依赖。
- 修改 `forge-report-server/pom.xml` 增加依赖。
- 新增 SQL 初始化文件和菜单资源。

前端：
- `forge-admin-ui` 新增数据资产管理页面。
- `forge-report-ui` 新增数据集 API 封装。
- `forge-report-ui` 图表数据配置面板新增数据集模式。
- `forge-report-ui` 数据请求执行链路新增 dataset 分支。
- `chartEditStore` 类型和默认配置扩展。

兼容性：
- 现有动态接口配置不迁移、不改变。
- 现有报表 JSON 没有 dataset 字段时按原逻辑运行。

## 16. 测试策略
后端测试：
- 数据连接 CRUD 编译和基础接口测试。
- 密码新增/修改/详情脱敏测试。
- 测试连接成功/失败测试。
- TABLE 数据集查询构造测试。
- SQL 数据集安全校验测试。
- 参数绑定测试，覆盖字符串、数字、日期、空值、必填缺失。
- 最大行数和超时配置测试。

前端测试：
- `forge-admin-ui` 数据连接页面构建通过。
- `forge-admin-ui` 数据集字段配置交互验证。
- `forge-report-ui` 数据加载模式切换后配置能保存到报表 JSON。
- 发布后重新进入，数据集配置可回显。
- 筛选组件变化后，数据集查询只触发必要刷新，不出现循环请求。

验证命令：

```bash
cd forge && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-data,forge-admin-server,forge-report-server -am compile -DskipTests
```

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm build
```

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-report-ui && pnpm build
```

## 17. 实施拆分建议
### Task 1: 新建平台数据插件与数据库表
- 新建 `forge-plugin-data` 模块。
- 增加三张表实体、Mapper、XML、Service 基础 CRUD。
- admin-server/report-server 增加依赖。

### Task 2: 数据连接管理
- 实现连接分页、详情、新增、修改、删除、测试连接。
- 实现密码加密、脱敏返回、连接池创建和释放。
- 实现表列表、字段列表元数据查询。

### Task 3: 数据集管理
- 实现数据集 CRUD。
- 实现 TABLE/SQL 数据集保存校验。
- 实现字段同步和字段批量保存。
- 实现数据集预览。

### Task 4: 查询执行器
- 实现 TABLE 查询构造。
- 实现 SQL 安全校验。
- 实现命名参数解析和 PreparedStatement 绑定。
- 实现结果转换为 `{ dimensions, source }`。

### Task 5: admin-ui 管理页面
- 新增数据连接页面。
- 新增数据集页面。
- 增加测试连接、同步字段、预览交互。

### Task 6: report-ui 数据集模式
- 新增数据集 API。
- 扩展 `RequestConfigType` 和默认配置。
- 数据配置面板新增「数据库数据集」。
- 请求执行链路新增数据集查询分支。
- 复用现有动态参数配置弹窗。

### Task 7: 联调与回归
- 管理端新建连接和数据集。
- 报表组件选择数据集并绑定 T-N 时间参数。
- 发布报表后重新进入验证配置保存和回显。
- 验证筛选组件联动查询不死循环。

## 18. 风险与应对
| 风险 | 应对 |
|------|------|
| SQL 注入 | SQL 安全校验 + 命名参数 + PreparedStatement |
| 慢 SQL 拖垮服务 | 查询超时 + 最大行数 + 后续缓存扩展 |
| 密码泄露 | 密文存储 + 脱敏返回 + 日志屏蔽 |
| 不同数据库方言差异 | DbDialect 抽象，首期 MySQL 优先 |
| 报表动态参数触发死循环 | 只监听被绑定的筛选组件值，不监听图表 dataset |
| 平台能力被报表私有化 | 插件放在 `forge-plugin-data`，admin/report/AI 共同依赖 |

## 19. 已确认
- 首期是否只要求 MySQL，必须同时支持 PostgreSQL/Oracle/SQLServer 的元数据读取和分页方言。
- 数据连接密码加密是否接入现有 crypto starter 的统一密钥，还是新增数据资产专用密钥配置项。--使用统一秘钥
- 报表运行时查询接口是否需要匿名分享页可访问；如果需要，需要额外设计分享 token 与数据集权限边界。--不需要

## 20. 确认记录（HARD-GATE）
- **确认时间**：已确认
- **确认人**：已确认
- **确认范围**：平台级数据连接、数据集、字段配置、报表数据库数据集加载模式
