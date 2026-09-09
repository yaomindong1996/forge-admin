# 低代码统一参数化只读查询源
> status: completed
> created: 2026-08-10
> complexity: 🔴高

## 1. 背景与目标

低代码表单需要在页面打开、字段失焦、扫码完成或用户点击查询时，根据当前表单值查询外部系统或业务数据库，并把结果交给后续字段事件做回填。典型场景包括 userid 映射人员/门店、手机号查询会员、条码查询商品、单号查询收款，但平台能力必须保持通用，不能出现预售、会员、商品、支付或企微专用代码。

本阶段建设一个统一、受控、可跨环境迁移的只读查询源协议。实施人员只能选择平台已经配置好的外部 API 或已发布数据集，不能在字段配置中直接填写任意 URL、认证信息或 SQL。

完成后应满足：

- 查询源类型固定为 `EXTERNAL_API`、`DATASET`；
- 使用稳定 `sourceKey` 引用资源，避免低代码配置绑定环境内数据库 ID；
- 外部 API 只有显式声明为低代码只读查询源后才可被统一网关执行；
- 外部 API 提供结构化输入/输出契约，运行时拒绝未知参数、缺失必填参数和类型错误；
- 数据集复用发布态、ACL、行权限、字段白名单、掩码、只读连接和缓存能力；
- 统一接口返回一致的来源、数据、分页和字段元信息；
- 统一网关不记录参数值，不接受脚本、任意 URL 或 SQL。

## 2. 复用与架构边界

### 2.1 外部 API

- 继续由 `forge-plugin-external` 负责系统认证、参数映射、响应提取、权限、限流、缓存、重试、日志脱敏和受控出站。
- 新增 `lowcodeQueryEnabled`、`inputSchemaJson`、`outputSchemaJson`，仅描述该 API 是否可用于低代码查询及其输入/输出契约。
- `sourceKey` 格式为 `<systemCode>/<apiCode>`，运行时按当前租户解析。
- `lowcodeQueryEnabled=true` 时只允许 GET、HEAD、POST；POST 必须由管理员明确确认其业务语义为只读查询。PUT、PATCH、DELETE 不能声明为只读查询源。
- 低代码查询源必须启用权限校验并配置权限码，调用仍由现有 `ExternalPermissionGuard` 执行。

### 2.2 数据集

- 继续复用 `forge-plugin-data`，不新建第三套数据库查询资源。
- `sourceKey` 使用已发布数据集的 `datasetCode`。
- 将现有 `DataDatasetRuntimeController` 中的发布态、状态、ACL、连接和字段校验下沉到可复用 `DataDatasetRuntimeService`，原接口与统一网关共享同一执行链。

### 2.3 统一网关

- 由 `forge-plugin-generator` 的低代码运行时提供统一目录、元数据和执行接口。
- 网关只做来源路由和统一响应，不复制外部连接器或数据集的安全逻辑。
- 本阶段不实现字段触发时机、结果字段回填、并发取消或防抖；这些属于下一阶段“统一字段事件”。

## 3. 外部查询契约

### 3.1 输入 Schema

`inputSchemaJson` 为 JSON 数组，每项至少包含：

```json
{
  "name": "keyword",
  "label": "查询条件",
  "type": "string",
  "required": true,
  "maxLength": 128
}
```

- 支持类型：`string`、`integer`、`number`、`boolean`、`object`、`array`。
- `name` 必须唯一且符合安全字段名格式。
- 运行时只允许 Schema 中声明的参数；未知参数直接拒绝，不得透传到外部 API。
- 必填、类型和字符串长度校验失败时返回明确业务错误，不输出参数值。
- 无运行参数的 API 使用空数组 `[]`。

### 3.2 输出 Schema

`outputSchemaJson` 为 JSON 数组，每项至少包含 `name`、`path`，可包含 `label`、`type`：

```json
{
  "name": "displayName",
  "path": "member.name",
  "label": "名称",
  "type": "string"
}
```

本阶段输出 Schema 用于目录元数据、调试和下一阶段设计器选字段，不在服务端擅自裁剪外部 API 的业务结果。响应字段回填由下一阶段按路径显式配置。

## 4. 统一接口

### 4.1 查询源目录

`GET /ai/lowcode/query-source/catalog?keyword=`

- 只返回当前租户、当前用户可访问的来源；
- 外部 API 必须同时满足系统启用、API 启用、低代码查询启用和当前用户权限；
- 数据集必须满足启用、已发布和当前用户 VIEW 权限；
- 目录项返回 `sourceType`、`sourceKey`、`sourceId`、名称和说明，不返回 SQL、URL、固定参数、脚本或认证配置。

### 4.2 查询源元数据

`POST /ai/lowcode/query-source/metadata`

- 输入 `sourceType`、`sourceKey`；
- 返回输入 Schema 与统一字段列表；
- 数据集字段继续按已有可见性和敏感等级规则返回。

### 4.3 执行查询

`POST /ai/lowcode/query-source/execute`

请求包含 `sourceType`、`sourceKey`、`params`，数据集可额外提供 `fields`、`pageNum`、`pageSize`、`maxRows`。统一响应包含：

- `sourceType`、`sourceKey`、`sourceId`；
- `data`；
- 可选 `total`、`pageNum`、`pageSize`；
- 字段元信息 `fields`。

不支持的来源类型、空来源键、未授权/未启用来源或非法参数必须失败关闭。

## 5. 数据库变更

新增幂等 Flyway：`V1.0.102__add_external_api_lowcode_query_contract.sql`。

向 `sys_external_api` 增加：

- `lowcode_query_enabled tinyint(1) NOT NULL DEFAULT 0`；
- `input_schema_json text NULL`；
- `output_schema_json text NULL`；
- 查询目录索引 `(tenant_id, lowcode_query_enabled, api_status, system_id)`。

迁移不得把任何存量 API 自动开放为低代码查询源，不写客户接口、业务字段、凭据或租户 0 数据。

## 6. 前端配置

外部接口管理页增加：

- “可作为低代码只读查询源”开关；
- 输入 Schema、输出 Schema 配置文本；
- 明确提示 POST 仅在目标接口业务语义确认为只读时启用。

前端 API 层增加统一查询源目录、元数据和执行方法，供下一阶段设计器和运行时直接复用。

## 7. 安全与审计

- 统一网关不接受 URL、HTTP Header、认证信息、SQL 或脚本。
- 统一网关日志只记录来源类型、来源摘要/ID、结果数量和耗时，不记录输入值或原始响应。
- 外部 API 权限、限流、缓存、出站安全和脱敏不得在统一网关中绕过。
- 数据集 ACL、行权限、字段掩码和查询边界不得在统一网关中绕过。
- 目录和元数据接口不得返回外部固定参数、请求体模板、转换脚本、数据集 SQL 或连接信息。

## 8. 兼容性

- 原 `/external/proxy/**` 和 `/data/dataset/runtime/**` 接口保持兼容。
- 新字段默认关闭，不改变存量外部 API 的现有调用行为。
- 数据集 Controller 改为委托 Runtime Service，成功与错误语义保持上一阶段加固后的行为。

## 9. 测试策略

- 外部查询契约：Schema 格式、未知参数、必填、类型、长度、只读方法和权限前置条件。
- 外部查询来源：稳定 key 解析、目录最小字段、不可用来源拒绝、执行复用代理。
- 数据集 Runtime Service：发布态、状态、ACL、连接、字段和执行委托。
- 统一网关：两种来源路由、统一结果、非法类型/来源拒绝、目录不泄露配置。
- Flyway：列、索引、默认关闭、information_schema 幂等、无占位符/租户 0/敏感数据。
- 前端：类型检查、Lint 和生产构建。

## 10. 非本阶段范围

- 字段 change/blur/click/onLoad 事件和结果回填；
- 前端防抖、竞态取消、加载态和错误态；
- 事务写命令、子表行级动作；
- H5 低代码运行时；
- 任意 JavaScript 沙箱执行。

## 11. 确认记录

- 2026-08-10：用户授权按既定七阶段路线连续实施，无需阶段间再次确认。

## 12. 完成结论

- 2026-08-10：external、data、generator 和前端能力已完成，统一查询源只接受平台受管的 `EXTERNAL_API` 与 `DATASET`。
- 外部 API 默认关闭低代码查询资格，配置、运行参数和稳定键均执行失败关闭校验；非法 `maxLength` 等 Schema 类型错误统一返回业务异常。
- 数据集目录、元数据和执行继续复用发布态、ACL、行权限、字段治理、只读连接与查询边界。
- 统一网关仅返回最小目录/字段/结果协议，未引入任意 URL、Header、认证、SQL 或脚本入口。
- 自动化验证已完成；真实 MySQL Flyway、真实外部接口和多角色/多租户联调按既定分工留待部署环境补验。
