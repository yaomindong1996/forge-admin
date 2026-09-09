# 低代码事务型业务命令
> status: completed
> created: 2026-08-10
> complexity: 🔴高

## 1. 背景与目标

Forge 已有 `BusinessActionExecutionService`、动态 CRUD 写入、动作步骤、幂等日志和设计态自动化入口，但这些能力目前同时承载本地数据库写入、消息、流程和领域动作，运行时仍可读取草稿动作，浏览器还可提交任意 `context`。这会让低代码实施人员误以为所有步骤都能在同一事务中回滚，也不足以安全承载“保存一条业务记录后原子调整另一条记录”的场景。

本阶段不新增任何预售、会员、商品、支付或企业微信专用代码，而是在现有 BusinessAction 上形成通用事务型命令底座：

- 页面、触发器和流程回调只执行不可变发布快照中的动作；
- 所有真实执行必须携带格式受控且稳定的幂等键；
- `LOCAL_TRANSACTION` 仅允许同一 Forge 主数据源内的本地数据步骤，并由单一事务覆盖；
- `ORCHESTRATION` 明确表示可能包含流程、消息或领域副作用，不承诺跨系统原子回滚；
- 更新支持条件比较、乐观并发和一个 SQL 内的数值增减/上下界保护；
- 输入只从结构化表单、权威记录和服务端可信上下文映射；
- 动作设计器以结构化配置为主，不要求客户写 Java/Vue，也不提供任意 API 或脚本执行入口；
- 执行日志记录发布动作版本和执行模式，但不保存表单值、凭据、SQL 或外部响应。

## 2. 架构归属与边界

继续增强现有 `BusinessActionExecutionService`，不建设第二套命令引擎。

```text
发布态页面 / 触发器 / 流程回调
  -> BusinessActionExecutionController
  -> 最新不可变业务对象发布快照
  -> 幂等日志 RUNNING 预留（REQUIRES_NEW）
  -> 执行模式与步骤边界校验
  -> LOCAL_TRANSACTION：单一事务执行本地数据步骤
  -> SUCCESS / FAILED 审计终态
```

本阶段的本地原子事务仅覆盖 Forge 主数据源。低代码外接数据库即使允许运行态写入，也不由默认事务管理器覆盖，因此必须拒绝进入 `LOCAL_TRANSACTION`。跨库、外部 API、消息和流程只能使用 `ORCHESTRATION`，其一致性依赖幂等、补偿或后续人工治理，不得显示为“失败自动全部回滚”。

## 3. 动作协议

动作配置新增受控字段：

```json
{
  "executionMode": "LOCAL_TRANSACTION",
  "inputSchema": [
    {
      "name": "quantity",
      "label": "本次数量",
      "type": "number",
      "required": true,
      "min": 1
    }
  ],
  "steps": []
}
```

### 3.1 执行模式

| 模式 | 允许范围 | 一致性承诺 |
|---|---|---|
| `LOCAL_TRANSACTION` | `CREATE_RECORD`、`UPDATE_FIELD`、`ADJUST_NUMBER`、只包含这些步骤的 `FOREACH` | Forge 主数据源单事务，任一步失败全部回滚 |
| `ORCHESTRATION` | 可包含领域动作、流程和消息等已注册执行器 | 逐步执行与幂等审计，不承诺跨系统原子回滚 |

新建动作默认 `LOCAL_TRANSACTION`。存量动作未声明模式时，如果只包含本地数据步骤则按本地事务执行；包含其它步骤时按 `ORCHESTRATION` 兼容，但发布检查给出明确风险提示。

### 3.2 结构化输入

`inputSchema` 只支持 `text`、`number`、`integer`、`boolean`、`date`、`datetime` 和 `select`。字段名必须符合安全标识符规则，禁止 `tenantId`、`userId`、`activeOrgId`、`role`、`permission`、`createBy`、`updateBy`、`__proto__` 等身份、审计和原型污染名称。

运行时只保留 `inputSchema` 声明的 `formData` 字段；未知字段失败关闭。没有输入 Schema 的存量动作可继续读取动作步骤显式引用的表单字段，但仍拒绝危险键。浏览器 `context` 只允许 `routeQuery`，且只能作为普通业务参数；行数据必须由 `recordId` 在服务端重新读取，不能信任浏览器提交的 `row`。

服务端可信上下文通过 `SYSTEM` 来源提供：`userId`、`tenantId`、`activeOrgId`、`username`、`realName`、`correlationId`、`recordId`、`objectCode`。这些值只来自可信 Session/执行身份，客户端同名字段不能覆盖。

### 3.3 条件更新

`UPDATE_FIELD` 继续通过 `fieldMappings/staticValues` 构建写入字段，并新增：

```json
{
  "expectedFieldMappings": [
    { "targetField": "status", "value": "PENDING" }
  ]
}
```

期望条件与数据权限、租户条件、逻辑删除条件一起进入同一条 `UPDATE`。影响行数为 0 时按并发冲突失败，整个本地事务回滚，禁止先查后改的竞态实现。

### 3.4 数值原子调整

新增 `ADJUST_NUMBER` 步骤：

```json
{
  "stepType": "ADJUST_NUMBER",
  "stepConfig": {
    "targetConfigKey": "order_item",
    "targetRecordIdField": "record.itemId",
    "adjustments": [
      { "targetField": "pickedQuantity", "sourceType": "form", "sourceField": "quantity", "operator": "ADD", "min": 0 },
      { "targetField": "pendingQuantity", "sourceType": "form", "sourceField": "quantity", "operator": "SUBTRACT", "min": 0 }
    ],
    "expectedFieldMappings": [
      { "targetField": "status", "value": "ACTIVE" }
    ]
  }
}
```

同一步的多个数值字段必须在一条 `UPDATE` 中调整；`SUBTRACT` 转为负 delta；`min/max` 作为数据库条件校验。字段、目标配置和记录 ID 都必须来自已发布动作配置与结构化映射，浏览器不能提交列名、表名或 SQL。

## 4. 发布态与幂等

- `/ai/business/action/execute` 只解析最新 `PUBLISHED` 业务对象版本，不读取 `designer_options` 草稿；
- 设计器预览继续走专用 `/preview`，需要设计权限且不产生副作用；
- `idempotencyKey` 长度 8～128，只允许安全字符；缺失或非法直接拒绝执行；
- 页面每次用户确认动作时生成一次键并随请求发送；同一网络请求重试复用同一键；
- 触发器、流程回调继续使用其确定性事件键；
- 唯一域包含 tenant、object、record、action、published version、idempotency key；
- 同键同摘要返回既有结果；同键异摘要失败；RUNNING/FAILED 不重复产生副作用；
- 执行日志只记录 SHA-256 请求摘要、动作版本、模式、结果和可信身份引用，不记录输入值。

## 5. 字段和数据权限

- 创建、更新、条件和数值调整全部复用 `DynamicCrudService` 的发布态配置、字段白名单、不可变字段过滤、租户和数据权限；
- 条件字段和调整字段也必须存在于发布态模型/动态表白名单；
- `id`、tenant/audit/delete 字段禁止通过动作写入；
- `LOCAL_TRANSACTION` 中所有目标配置必须属于 Forge 主数据源；
- 对未找到、无权限、条件不匹配和数值越界统一返回安全业务错误，不泄露表名、列名、SQL 或当前数据库值。

## 6. 设计器与运行时

- 自动化动作增加执行模式说明、结构化输入字段和本地事务步骤入口；
- 提供“创建记录”“更新字段”“调整数值”结构化卡片，目标对象和字段来自对象/模型目录；
- 本地事务模式选择流程、消息、领域动作时立即提示并禁止保存/发布；
- 高级 JSON 仅作为实施开发者兼容入口，后端仍执行同一白名单校验；不增加 URL、Header、认证、SQL 或脚本配置；
- 手动动作输入表单从 `inputSchema` 生成；运行时自动生成幂等键并不再发送整行数据到 `context`；
- 成功后支持刷新列表或无操作，失败保留弹窗输入供用户修正。

## 7. 发布检查

发布时失败关闭校验：

- actionCode 唯一、稳定且格式正确；
- executionMode、输入字段类型、步骤类型和结构符合白名单；
- 本地事务不得出现 `START_FLOW`、`SEND_MESSAGE`、`DOMAIN_ACTION` 或未知步骤，递归 `FOREACH` 同样校验；
- 本地事务步骤必须 `rollbackOnFailure=true`，禁止配置继续执行；
- CREATE/UPDATE/ADJUST 目标 configKey、字段映射和记录 ID 映射完整；
- 条件、输入和映射中禁止危险身份字段、危险路径、URL、Header、认证、SQL、script/handler；
- ORCHESTRATION 显示非原子风险，不宣称失败全部回滚。

## 8. 测试策略

- 服务：发布态解析、必填幂等、同键同/异摘要、可信上下文和危险输入；
- 事务：多步骤成功提交、任一步失败回滚、非本地主数据源拒绝；
- 更新：字段白名单、数据权限、同 SQL 期望条件、数值多字段原子调整和上下界；
- 发布：递归步骤白名单、模式边界、输入 Schema、危险键和兼容动作；
- 前端：输入 Schema 表单、幂等键生成/复用、请求不携带整行 context、设计器结构化协议；
- Java 定向测试、generator 聚合编译、前端 Vitest/ESLint/生产构建和差异静态检查。

## 9. 非本阶段范围

- 子表行按钮、当前子行上下文和父子行协议（阶段 5）；
- H5/企业微信容器、扫码 SDK 和移动导航（阶段 6）；
- 离线草稿、冲突重放、金额精度总治理和长期审计归档（阶段 7）；
- 分布式事务、跨库 XA 或第三方接口自动补偿；
- 任意 JavaScript、Groovy、SpEL、URL 或 SQL 动作执行。

## 10. 确认记录

- 2026-08-10：用户授权按既定七阶段路线连续实施，无需阶段间再次确认。

## 11. 实施结论

- 2026-08-11：后端发布态执行、版本化幂等、本地事务边界、条件更新、原子数值调整、输入投影与可信上下文全部完成。
- 2026-08-11：`AiCrudPage` 已按 `inputSchema` 生成输入表单，页面幂等键按同载荷重试复用，请求不再提交浏览器整行数据。
- 2026-08-11：自动化设计器已提供执行模式、输入字段及 `CREATE_RECORD`、`UPDATE_FIELD`、`ADJUST_NUMBER` 结构化配置，本地模式不开放流程、消息或领域步骤入口。
- 2026-08-11：显式 `inputSchema: []` 按“禁止额外输入”处理；只有完全缺失 `inputSchema` 的存量动作保留兼容输入语义。
- 自动化验证已通过；真实 MySQL/Flyway、租户和数据权限、浏览器弱网与事务回滚 E2E 按既定分工由部署环境补验。
