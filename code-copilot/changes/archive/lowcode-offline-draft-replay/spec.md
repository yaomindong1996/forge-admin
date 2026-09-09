# 低代码离线草稿、冲突重放与治理

> status: completed
> created: 2026-08-10
> complexity: 🟠中高

## 1. 目标

为低代码表单提供通用离线草稿和受控操作重放基础能力，并把状态、金额和审计治理接入同一条发布态运行链路。浏览器断网时只能保存本地草稿与待重放意图；恢复网络后必须先检查发布版本和记录版本，再由调用方将每条意图交给服务端现有业务动作接口执行。离线数据永远不是可信业务记录，不能绕过租户、权限、字段白名单、事务和幂等校验。

## 2. 数据边界

本地草稿由 `createOfflineDraftStore` 管理，调用方必须用应用/对象/表单编码和当前会话作用域组成受控 `namespace`；运行时不会从浏览器自报身份自动生成可信作用域。草稿包含：

- `draftId`、`applicationCode`、`objectCode`、`formCode`；
- `publishedVersion`、`schemaHash`；
- 可选 `recordId`、`baseRecordVersion`；
- 表单业务值 `data`（只保存 JSON 可序列化值，移除 token、密码、Secret、Header 等敏感键）；
- `replayLog` 操作日志和状态；
- 创建/更新时间、草稿状态和失败原因。

本地存储有大小上限和草稿数量上限；写入失败返回受控 `DRAFT_STORAGE_FAILED`，不影响服务端可信数据。

## 3. 操作重放协议

重放意图必须显式包含：`actionCode`、`objectCode`、`recordId`、已声明的 `formData`、`publishedVersion`、`idempotencyKey`。不得把完整页面快照、浏览器身份字段或凭据作为重放上下文。

同一草稿中 `idempotencyKey` 唯一；已完成的意图不会再次执行。重放器按日志顺序执行，第一条失败即停止并把草稿标为 `REPLAY_FAILED`，不自动跳过或无限重试。服务端仍按当前登录 Session、发布快照、数据权限、业务动作策略和幂等键重新校验。

## 4. 冲突检测

重放前由调用方提供可信 `loadCurrent`：

- `publishedVersion` 不一致 → `PUBLISHED_VERSION_CONFLICT`；
- 草稿有 `baseRecordVersion` 且当前记录版本不一致 → `RECORD_VERSION_CONFLICT`；
- 记录已删除/无权访问 → `RECORD_UNAVAILABLE`。

发现冲突只返回冲突摘要并标记草稿，不自动合并、覆盖或执行副作用。用户确认后可重新加载最新表单生成新草稿/新幂等键。

## 5. 安全治理

- 本地草稿不存储 Authorization、Token、Cookie、AppSecret、AK/SK、密码或原始响应；
- `data` 和重放 payload 只接受 JSON 值，循环引用、函数、Symbol、BigInt 和超限值失败关闭；
- 恢复在线后重放调用方必须显式传入 `execute` 和 `loadCurrent`，库不自行发请求；
- 所有状态转换可审计，错误消息不包含敏感值；
- 清理接口按明确 `draftId` 操作，不递归清理整个浏览器存储。

## 6. 状态、金额与审计治理

### 6.1 状态迁移协议

普通低代码单据的状态迁移使用已发布动作中的 `TRANSITION_STATUS` 结构化步骤，不依赖 Flowable：

```json
{
  "stepType": "TRANSITION_STATUS",
  "stepConfig": {
    "targetConfigKey": "pre_sale_order",
    "targetRecordIdField": "record.id",
    "statusField": "status",
    "fromValue": "DRAFT",
    "toValue": "SUBMITTED"
  }
}
```

`statusField`、`fromValue`、`toValue` 必须是发布快照中的白名单字段和值；执行时把 `fromValue` 作为同一条 SQL `UPDATE` 的 expected-status 条件，影响行数为 0 即并发冲突并回滚。禁止任意跳转、客户端覆盖状态或先查后改。每次成功或失败的状态迁移写入结构化审计摘要。

### 6.2 金额精度协议

动作输入 Schema 支持 `MONEY` 类型。默认 `scale=2`，允许配置 `0..6`；输入必须是十进制定点值，实际小数位超过 scale 时失败关闭，禁止静默四舍五入。金额上下限按展示单位校验，映射到新建/更新步骤时统一转换为最小货币单位 `long`（人民币为分）；不能用浮点计算或把凭据/原值写入日志。已有历史 decimal 字段保持兼容；当前 `MONEY` 协议固定表达“展示单位输入、最小货币单位存储”，未来若开放其它存储单位必须新增显式协议字段，禁止根据字段名猜测。

### 6.3 审计与归档

动作执行日志增加结构化审计字段：`auditEventType`、`statusField`、`statusFrom`、`statusTo`、`changeSummary`、`retentionUntil`。日志只记录动作版本、幂等键、可信操作者引用、状态迁移摘要和脱敏后的字段名，不记录手机号、表单原值、SQL、凭据或外部响应。普通查询/删除接口不得物理删除审计日志；后续留存清理只能由专用归档任务按 `retentionUntil` 处理，归档前保留可审计索引。

### 6.4 运行态草稿接入

表单发布态可配置 `governance.offlineDraft`：`enabled`、`applicationCode`、`formCode`、`replayActionCode`、`recordVersionField`。`AiCrudPage` 只在该配置启用时自动保存/恢复表单草稿；namespace 必须含当前租户、用户、应用、对象和表单编码。断网提交只写本地草稿，只有配置了 `replayActionCode` 才追加受控业务动作重放意图；恢复网络后先调用 `loadCurrent` 检查发布版本/schemaHash/记录版本，再由用户显式确认重放。草稿恢复不等于服务端已保存，不得自动覆盖冲突记录。

## 7. 非本阶段范围

- IndexedDB 大文件/附件离线缓存；
- 自动后台同步、Service Worker 和多标签页锁；
- 业务专用冲突合并算法；
- 修改服务端业务动作协议或新增预售、会员、商品、支付接口。

## 8. 完成结论

- 2026-08-10：新增 `offline-draft-runtime.js` 并从 `@/utils` 导出，提供本地草稿保存/读取/删除、敏感键清洗、大小/数量限制和明确状态。
- 2026-08-10：重放日志以幂等键去重，按顺序执行；发布版本、记录版本或记录可用性冲突在副作用前失败关闭，失败停止后续重放。
- 2026-08-10：5 个离线草稿定向测试通过；目标 ESLint 无错误；前端生产构建和 `git diff --check` 通过。
- 2026-08-11：审计发现状态迁移、金额精度、结构化审计字段和 `AiCrudPage` 草稿接入尚未形成闭环，本阶段重新打开，按本 Spec 增量实现后再恢复完成状态。
- 2026-08-11：新增 `TRANSITION_STATUS` 与 `MONEY` 协议治理，状态迁移通过同 SQL expected-status 条件更新；成功和失败尝试均记录脱敏结构化 `outcome`，动作日志增加留存截止时间和审计索引。
- 2026-08-11：`AiCrudPage` 已接入发布态 `offlineDraft`，支持主子表自动保存/恢复、断网只存本地、恢复在线后重新读取最新发布配置和记录版本，并由用户显式确认重放；设计器提供普通用户配置入口和边界说明。
- 2026-08-11：后端定向 30/30、前端定向 36/36、目标 ESLint、前端生产构建、Flyway 占位符/版本静态检查和 `git diff --check` 全部通过；未启动数据库、Redis、Flow 或外部系统，真实迁移与弱网浏览器 E2E 留给部署环境补验。
