# 流程通知配置通用化（事件 × 渠道矩阵）— 改造文档

> 状态：**实现完成，已通过定向单测、后端编译和前端构建验证；真实数据库/消息渠道 E2E 待环境验收**。

---

## 一、背景与目标

### 现状问题
1. 待办企微推送开关在**连接级**（`sys_social_config.todo_push_enabled`），一开全租户所有流程都推，无法按流程控制
2. 卡片模板全局一个（`FLOW_TODO_CARD`），无法按流程定制文案
3. 卡片内容只注入流程元数据（taskTitle/processName/startUserName/url），低代码业务表单字段进不了卡片
4. 通知类型只覆盖"新待办"：审批结果只走站内信、抄送只落库不推任何渠道

### 目标方案（已确认）
流程模型上加「通知与推送」配置块，采用**事件 × 渠道矩阵**：

| 事件 | 可选渠道 |
|---|---|
| todo（新待办） | 站内信(锁定必选)/邮件/短信/企微 |
| result（审批结果） | 站内信/邮件/短信/企微 |
| cc（流程通过抄送） | 站内信/邮件/企微 |

关键设计原则：
- **渠道列表动态读**：消息插件内置渠道（站内/邮件/短信）+ `sys_social_config` 已启用连接的平台（现仅企微）；钉钉/飞书未接适配器前不出现在勾选列表
- **配置收敛为一个 JSON 字段** `sys_flow_model.notify_config`，不按渠道铺列
- **未配置 = 现状行为**：站内信 + 连接开了 `todoPushEnabled` 就推企微，升级零回归
- **短信设防**：接口返回 `costWarning=true`，前端勾选时给成本提示
- **显式勾选企微渠道时不受连接级 `todoPushEnabled` 限制**（避免"配置了不生效"），但 H5 地址仍必须配置

### notify_config JSON 结构
```json
{
  "todo":   {"channels": ["WEB", "COLLABORATION"], "templateCode": null},
  "result": {"channels": ["WEB", "EMAIL"]},
  "cc":     {"channels": ["WEB"]}
}
```
渠道常量：`WEB` / `EMAIL` / `SMS` / `COLLABORATION`。事件 key：`todo` / `result` / `cc`。

---

## 二、已完成改动（后端，代码已写完）

### ① DB Migration ✅
**文件**：`forge-server/db/migration/V1.0.128__flow_model_notify_config.sql`
- `sys_flow_model` 加 `notify_config` JSON 列（AFTER `todo_detail_url_template`）
- 防重复保护（information_schema 判断列存在）
- ⚠️ 顺带确认最新版本号：写此文时 migration 目录最新为 V1.0.128，若期间有新增需顺延版本号

### ② FlowModel 实体 ✅
**文件**：`forge-plugin-flow/.../flow/entity/FlowModel.java`
- 加 `private String notifyConfig;`（第 133 行附近），带 Javadoc

### ③ FlowNotifyConfig 解析类（新建）✅
**文件**：`forge-plugin-flow/.../flow/support/FlowNotifyConfig.java`
- 常量：`EVENT_TODO/EVENT_RESULT/EVENT_CC`、`CHANNEL_WEB/CHANNEL_EMAIL/CHANNEL_SMS/CHANNEL_COLLABORATION`
- `parse(String json)`：空/非法返回 null（回退默认行为）
- `channelConfigOf(String eventKey)`：取事件渠道配置
- `ChannelConfig{ channels, templateCode }`，顶层扁平 key（todo/result/cc → config）

### ④ FlowTaskNotifyEvent ✅
**文件**：`forge-plugin-flow/.../flow/event/FlowTaskNotifyEvent.java`
- 新增枚举值 `PROCESS_RESULT`
- 新增字段 `variables`（Map）、`rejected`（Boolean，结果是否驳回）
- `todo()` 加 variables 参数重载
- 新增工厂方法 `processResult(business, variables, rejected)`

### ⑤ FlowTaskEventListener 发布点 ✅
**文件**：`forge-plugin-flow/.../flow/listener/FlowTaskEventListener.java`
- L209（handleTaskCreated）与 L350（候选组转办）两处 `todo` 发布均传入 `readTaskVariables(task)`
- L474 `handleProcessCompleted` 处发布 `FlowTaskNotifyEvent.processResult(business, processVariables, rejected)`

### ⑥ FlowTaskNotifyListener 多渠道分发（核心）✅
**文件**：`forge-plugin-flow/.../flow/listener/FlowTaskNotifyListener.java`

改造后的方法结构（供后续 review 时对照）：
| 方法 | 职责 |
|---|---|
| `sendTaskCreatedMessage(flowTask, business, variables)` | 入口：`loadFlowModel` → `FlowNotifyConfig.parse` → 无配置走默认行为（WEB + 企微卡片跟随连接开关）；有配置按 `todo.channels` 循环 switch 分发 |
| `sendTaskWebMessage(...)` | 站内信（幂等键 `taskId`） |
| `sendTaskChannelMessage(...)` | 邮件/短信通用发送（幂等键 `taskId:渠道`） |
| `sendTaskCollaborationCard(..., model, templateCodeOverride, variables, requireTodoPushEnabled)` | 企微卡片；矩阵显式勾选时 `requireTodoPushEnabled=false`；变量经 `mergeVariables` 注入 params |
| `resolveTodoPushConnection(boolean requireTodoPushEnabled)` | 连接解析，按需要求开关 |
| `resolveCardTemplateCode(platform, defaultTemplateCode, templateCodeOverride)` | 模板编码解析：模型覆盖 > 事件默认编码 > 平台差异化 > 通用，支持 result/cc 用不同默认编码 |
| `sendProcessResult(business, variables, rejected)` | 审批结果分发（PROCESS_RESULT 事件） |
| `sendCcChannelMessages(business, userIds, variables)` | 抄送渠道推送（挂接在原 `sendProcessCc` 落库之后） |
| `resolveChannelConfig(business, eventKey)` | result/cc 事件按需查模型配置 |
| `baseTaskParams` / `mergeVariables` / `baseBusinessParams` | 公共参数组装 |

幂等键规则：`FLOW_TODO` 业务类型 + taskId（WEB）或 `taskId:CHANNEL`（其他渠道）。

### ⑦ 渠道查询接口（新建）✅
**文件**：`forge-flow/forge-flow-server/.../controller/FlowNotifyChannelController.java`
- `GET /api/flow/notify-channels` → `RespInfo<List<Map>>`
- 返回项字段：`channel` / `name` / `type`(builtin|connection) / `platforms` / `alwaysOn` / `costWarning`
- WEB alwaysOn=true；SMS costWarning=true；协同渠道聚合已启用平台名（"企业协同（企业微信）"）
- 协同模块不可用时静默降级只返回内置渠道

### 零改动模块（设计红利）
forge-plugin-collaboration（企微适配器）、全部 forge-starter-*、forge-plugin-message、forge-plugin-generator、forge-h5-ui。

---

## 三、完成情况与环境验收项

### 1. 后端编译验证 ✅
已使用 JDK 17 执行：
```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
mvn compile -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am -DskipTests
# 然后编 flow-server：
mvn compile -pl forge-flow/forge-flow-server -am -DskipTests
```
Flow 插件与 Flow Server 均 `BUILD SUCCESS`。

### 2. 前端 model.vue「通知与推送」矩阵 UI ✅
**文件**：`forge-admin-ui/src/views/flow/model.vue`
- 在现有 `notifyType`（webhook 通知）配置区旁，新增"通知与推送"Card 区块
- 结构建议：三行（新待办/审批结果/抄送）× 动态渠道 Checkbox
  - 渠道列表调 `GET /api/flow/notify-channels`（在 `api/flow.js` 加 `getFlowNotifyChannels()`）
  - 站内信行勾选框 `disabled`（alwaysOn）并默认选中
  - 勾短信时 `costWarning=true` 显示计费提示（NPopconfirm 或文案提醒）
  - 已保存配置回显：解析 `form.notifyConfig` JSON 到矩阵
- 提交：矩阵状态序列化回 `notifyConfig` JSON 存入 form；全空/全默认时不传该字段（保持 NULL 走默认行为）
- 表单 model 对象加 `notifyConfig: null` 字段（编辑接口返回 entity 自带）

### 3. 结果/抄送卡片模板种子 ✅
- 结果卡片默认编码：`FLOW_RESULT_CARD`（+ `FLOW_RESULT_CARD_WECOM`）；抄送：`FLOW_CC_CARD`
- 新增一个 migration（V1.0.129）往 `sys_message_template` 插种子，写法参照 V1.0.72（有防重复 INSERT 保护）
- 不插种子也不会挂：listener 有 `buildDefaultResultCardDescription` / `buildDefaultCcCardDescription` 内置兜底排版，只是模板管理里搜不到（避免重蹈"前后割裂"）

### 4. 回归验证
- 老流程（notify_config 为 NULL）：待办 → 站内信 + 企微（连接开关开着时）行为不变
- 新配置流程：只勾站内信 → 企微不推；勾企微但连接开关没开 → 也推（矩阵优先）
- 多渠道幂等：同一 taskId 各渠道独立幂等键，不会互相挤掉
- 企微卡片变量注入：`{业务字段}` 占位符渲染（模板 params 机制）

上述路径已完成代码级、定向单测和构建验证；真实数据库迁移、站内信/邮件/短信/企业协同投递仍需在集成环境执行 E2E。

### 5. 已知遗留问题（非本次范围）
- 全量初始化SQL.sql 落后于 migration（缺 FLOW_TODO_CARD 种子等，forge_schema_history 为空表）——用户明确本次不管，但发新环境会踩
- 短信渠道实际投递依赖消息模块 SMS 通道配置（短信服务商），需在消息中心配置后才真正发出

---

## 四、涉及文件速查

| # | 文件 | 状态 |
|---|---|---|
| 1 | `forge-server/db/migration/V1.0.128__flow_model_notify_config.sql` | ✅ 新建 |
| 2 | `forge-plugin-flow/.../flow/support/FlowNotifyConfig.java` | ✅ 新建 |
| 3 | `forge-plugin-flow/.../flow/entity/FlowModel.java` | ✅ 加 notifyConfig |
| 4 | `forge-plugin-flow/.../flow/event/FlowTaskNotifyEvent.java` | ✅ PROCESS_RESULT + variables + rejected |
| 5 | `forge-plugin-flow/.../flow/listener/FlowTaskEventListener.java` | ✅ 3 处发布点改造 |
| 6 | `forge-plugin-flow/.../flow/listener/FlowTaskNotifyListener.java` | ✅ 多渠道分发核心，编译通过 |
| 7 | `forge-flow-server/.../controller/FlowNotifyChannelController.java` | ✅ 新建，编译通过 |
| 8 | `forge-admin-ui/src/api/flow.js` | ✅ 渠道查询 API |
| 9 | `forge-admin-ui/src/views/flow/model.vue` | ✅ 矩阵配置 UI |
| 10 | `forge-server/db/migration/V1.0.129__flow_result_cc_card_templates.sql` | ✅ 模板种子 |
