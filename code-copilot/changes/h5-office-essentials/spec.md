# H5 办公基础能力 Spec（发起审批 + 公告）

> 变更名：`h5-office-essentials`
> 状态：`confirmed`（第 9 章决策已于 2026-10-09 确认，可进入 /apply）
> 创建日期：2026-10-09
> 前置变更：`h5-dingtalk-redesign`（五页签导航、角标 store、通讯录）
> 涉及：新增一个只读目录接口；H5 低代码运行页接入"提交审批 / 撤回 / 修改后重提"（状态流转，需人工审查）

## 1. 背景与目标

### 1.1 背景

`h5-dingtalk-redesign` 完成后，H5 能处理别人发起的审批，但存在两个缺口：

- **不能发起审批**：H5 没有"我能发起哪些审批"的入口；低代码运行页可以新建和保存单据，但不会执行"提交审批"。员工只能回到 PC 端提交。
- **看不到公告**：后端已有面向普通用户的公告接口，H5 没有页面。

### 1.2 目标

1. 新增"发起审批"页：列出当前用户可用、且绑定了审批流程的低代码业务应用，按应用分组，可搜索。
2. 低代码运行页支持单据审批动作：提交审批（含发起人自选审批人）、撤回、驳回后修改重提，并显示审批状态。
3. 新增公告列表和详情：未读标记、置顶、附件、进入详情自动标记已读。
4. 公告未读数计入"消息"页签角标；工作台展示一条最新公告。

### 1.3 不做

- 流程入口（`sys_flow_entry`）：管理端没有配置页面，菜单已下线，本变更不接入。
- 代码单据（如 `sample-purchase-order`）：每种单据要单独写 H5 适配，后续按需另立变更。
- 应用流程动作 `START_PROCESS`（`/ai/business/process/runtime/.../start`）：本变更只做单据主流程；运行页遇到该动作时提示"请在 PC 端发起"。
- 草稿箱：低代码单据保存后即为草稿，从应用列表可以继续编辑和提交，不单独做草稿页。
- 不改审批权限模型，不写授权迁移脚本。
- 不做公告发布、编辑、阅读统计（这些属于管理端功能）。

## 2. 代码现状（Research Findings）

### 2.1 低代码单据发起审批（管理端现有链路）

`forge-admin-ui/src/components/ai-form/crud/composables/useAiCrudPage.part2.js`：

1. 记录先保存，拿到 `recordId`。
2. 取单据运行态 `GET /ai/business/document/{objectCode}/{recordId}/runtime`（`@SaCheckPermission("ai:businessDocument:view")`），返回 `BusinessDocumentRuntimeVO`：
   - `documentStatus` / `documentStatusLabel`、`flowStatus`、`processInstanceId`、`businessKey`、`roundNo`；
   - `runtimeActions[]`：`key`、`label`、`type`、`actionType`、`visible`、`disabled`、`disabledReason`、`objectCode`、`recordId`；
   - `myTask`：当前用户在该单据上的待办（`taskId`、`taskDefKey`、`processInstanceId`）。
3. `actionType` 取值（`BusinessDocumentRuntimeActionPolicy`）：`START_FLOW`、`RESUBMIT_FLOW`、`HANDLE_TASK`、`WITHDRAW_FLOW`、`VIEW_FLOW`；应用流程动作为 `START_PROCESS`。
4. 提交审批（`startFlowAction`，L112）：
   - `GET /ai/business/flow/start-config/{objectCode}`（只校验登录），返回 `initiatorSelectNodes[]`：`nodeKey`、`nodeName`、`multiple`；
   - 有节点时先选审批人，变量为 `{ PROCESS_START_USER: { [nodeKey]: [userId...] } }`；`multiple === false` 只取第一个；每个节点必选（`utils/initiatorSelect.js`）；
   - `POST /ai/business/flow/start`，`BusinessFlowStartDTO`：`objectCode`、`recordId`、`variables`（`@SaCheckPermission("ai:businessFlow:start")`）。
5. 修改后重提（`resubmitFlowAction`，L171）：先保存修改，再 `POST /ai/business/flow/resubmit`，参数 `taskId`、`taskDefKey`、`processInstanceId`、`businessKey`（取自 `myTask` 和运行态）。
6. 撤回（`withdrawFlowAction`，L221）：`POST /ai/business/flow/withdraw`，参数 `objectCode`、`recordId`、`processInstanceId`、`businessKey`、`comment`（`@SaCheckPermission("ai:businessDocument:withdraw")`）。重提与提交共用 `ai:businessFlow:start`。
7. 办理（`HANDLE_TASK`）：跳到待办详情。

### 2.2 H5 低代码运行页现状

- `pages/lowcode-runtime.vue`（576 行）+ `store/modules/lowcodeRuntime.js` + `composables/lowcode/*`。
- 路由参数：`configKey`、`mode`（`list` / `create` / `detail` / `edit`）、`recordId`、`appId`、`applicationId`、`taskId`、`processInstanceId`。
- 渲染配置 `GET /ai/crud-config/render/{configKey}` 已返回 `objectCode` 和 `options.runtimeActions`、`options.flowInteraction`（由 `BusinessApplicationRuntimeConfigOverlayService` 叠加）。
- 保存逻辑已支持"先保存再执行动作"：`requireRecordId` 为真且新建未返回主键时报错（L398）。
- `useLowcodeFlowRuntime.js`（68 行）只处理审批节点动作（同意、驳回、退回、转交）和审批记录，**没有**单据级的提交、撤回、重提。

### 2.3 "可发起审批的应用"目录

- 单据和流程的绑定在 `ai_business_binding`：`target_type = 'OBJECT'`、`target_code = objectCode`、`binding_type = 'FLOW'`、`binding_key = flowModelKey`、`status`。
- 对象：`ai_business_object`（`object_code`、`object_name`、`icon`、`config_key`、`status`、`sort_order`、`del_flag`）。
- 应用：`ai_business_application`（`application_code`、`application_name`、`icon`、`status`、`del_flag`），通过 `ai_business_application_object`（`application_id`、`object_id`、`sort_order`、`del_flag`）关联对象。
- **没有**现成接口能按当前用户列出可发起的审批；`/api/flow/model/enabled` 列的是流程模型，和用户能用的应用对不上。
- H5 工作台菜单来自 `GET /auth/current/menu`，低代码应用菜单的路径里带 `configKey`（`utils/mobile-menu.js` L59–71 解析）。

### 2.4 权限

- `ai:businessFlow:start`（提交、重提）、`ai:businessDocument:view`（运行态）、`ai:businessDocument:withdraw`（撤回）是 `sys_resource` 中的按钮权限，普通员工角色默认没有。
- `start-config` 没有权限注解，只校验登录（`BusinessFlowController` L59–64）。
- H5 目前不保存权限列表；`api.getCurrentPermissions`（`GET /auth/current/permissions`，返回 `List<String>`）已定义但没有调用方。

### 2.5 公告

`SysNoticeController`（`/system/notice`，类上 `@ApiDecrypt @ApiEncrypt`）中面向普通用户、只校验登录（`@ApiPermissionIgnore`）的接口：

| 接口 | 说明 |
|------|------|
| `GET /user/page` | 当前用户可见公告分页（`PageQuery` + `SysNoticeQuery`，可按 `noticeTitle`、`noticeType`、`isTop` 过滤），返回 `SysNoticeVO`，含 `isRead` |
| `GET /user/unread-count` | 当前用户未读公告数 |
| `GET /user/{noticeId}` | 详情；校验发布范围和有效期；会累加阅读次数，**不会**标记已读 |
| `POST /markAsRead?noticeId=` | 标记已读；同样校验可见性 |

- `SysNoticeVO` 主要字段：`noticeId`、`noticeTitle`、`noticeContent`（富文本 HTML）、`noticeType` / `noticeTypeName`、`publishTime`、`publisherName`、`isTop`、`isRead`、`attachments[]`（`fileId`、`fileName`、`fileSize`、`fileUrl`）、`readCount`。
- H5 已有 `utils/message-html.js` 的 `sanitizeMessageHtml`，消息详情用 `rich-text` 渲染净化后的 HTML，公告正文复用。

### 2.6 代理与部署

- H5 开发代理（`vite.config.js` L62–86）：`/api/flow/**` 和 `/ai/business/flow/**` 转到 flow-server（8081），其余转到 app-server（8583）。
- flow-server 和 app-server 都扫描 `forge-plugin-generator` 的 Controller 和 Mapper，`/ai/business/flow/start` 现在就是由 flow-server 处理的。

### 2.7 相关页面行数

| 文件 | 行数 | 说明 |
|------|------|------|
| `pages/lowcode-runtime.vue` | 576 | 只接线，单据流程逻辑放新 composable |
| `pages/message/index.vue` | 492 | 加公告入口行 |
| `pages/index/index.vue` | 288 | 加发起审批入口和最新公告条 |
| `pages/todo.vue` | 365 | 顶栏加"发起"按钮 |

## 3. 发起审批页

### 3.1 入口

- 工作台概览卡片新增"发起审批"快捷入口。
- 待办页顶栏功能区新增"+"按钮。
- 两处都 `navigateTo('/pages/approval/start')`。

### 3.2 页面 `pages/approval/start`

- 二级页面，原生导航栏，标题"发起审批"。
- 顶部搜索框，按应用名或单据名过滤（前端过滤，不发请求）。
- 按业务应用分组：每组一张白色卡片，组标题为应用名，下方 4 列宫格，每项为单据图标（沿用 `AiAppIcon` 和语义色板）和单据名。
- 点击单据：`navigateTo('/pages/lowcode-runtime?configKey=...&mode=create&applicationId=...&title=...')`。
- 空状态（无数据或无权限）："暂无可发起的审批，如需开通请联系管理员"。
- 当前用户没有 `ai:businessFlow:start` 权限时不请求接口，直接显示空状态。

### 3.3 数据口径

新增接口返回"启用了审批流程的单据目录"，H5 再与当前用户菜单取交集，只显示用户有菜单权限的单据：

- 菜单 `configKey` 集合：从 `getCurrentMenu` 结果中按 `mobile-menu.js` 现有规则解析。
- 交集为空的单据不显示。目录接口只返回名称、图标、编码等元数据，不含业务数据；真正的新建、保存、提交都由各自接口做权限校验。

### 3.4 后端接口

在 `forge-plugin-generator` 新增：

| 接口 | 说明 |
|------|------|
| `GET /ai/business/flow/startable-objects` | 当前租户内启用审批流程的单据目录 |

- 放在现有 `BusinessFlowController`（路径前缀 `/ai/business/flow`，H5 经 flow-server 访问）；方法加 `@ApiPermissionIgnore`，只校验登录。
- 返回 `List<BusinessStartableObjectVO>`：`objectCode`、`objectName`、`objectIcon`、`configKey`、`applicationId`、`applicationName`、`applicationIcon`、`sortOrder`。
- SQL 写在新的 `BusinessStartableObjectMapper.xml`：
  - `ai_business_binding`：`target_type = 'OBJECT'`、`binding_type = 'FLOW'`、`status` 为启用、`tenant_id = #{tenantId}`；
  - 关联 `ai_business_object`：`status` 为启用、`del_flag = 0`、`config_key` 非空；
  - 左关联 `ai_business_application_object` 和 `ai_business_application`（均 `del_flag = 0`，应用 `status` 为启用）；同一对象挂在多个应用下时每个应用各出一条；
  - 排序：未挂应用的对象排最后；应用按 `id`（应用表没有排序字段），应用内按关联表 `sort_order`，最后按对象名。
- 状态值用现有枚举比较；XML 里允许字面量（AGENTS.md 5.9）。
- 结果按租户缓存不做，数据量小（通常几十条）。

## 4. 低代码运行页的单据审批动作

### 4.1 状态与动作

- 进入 `detail` / `edit` 模式且有 `recordId`、`objectCode` 时，拉取单据运行态 `GET /ai/business/document/{objectCode}/{recordId}/runtime`。
- 详情顶部显示审批状态卡（`LowcodeDocumentFlowStatus`）：标签文案用后端的 `documentStatusLabel`，下方显示运行态 `message`；`flowStatus` 只决定标签颜色，前端不维护状态文案。
- 默认页脚 `LowcodeRuntimeFooter` 按 `runtimeActions` 中 `visible !== false` 的项追加按钮，有审批按钮时页脚改为单行排列；`disabled` 时置灰，`disabledReason` 显示在状态卡里。页面配置了自定义底部栏（没有默认页脚）时，审批按钮放在状态卡内：

| `actionType` | 按钮 | 行为 |
|--------------|------|------|
| `START_FLOW` | 提交审批（主按钮） | 有未保存修改时先保存；查发起配置；需要时弹出选人；调用 `/ai/business/flow/start` |
| `RESUBMIT_FLOW` | 重新提交（主按钮） | 先保存修改，再调用 `/ai/business/flow/resubmit` |
| `WITHDRAW_FLOW` | 撤回（次按钮） | 二次确认后调用 `/ai/business/flow/withdraw`，`comment` 为"申请人撤回" |
| `HANDLE_TASK` | 去审批 | `navigateTo` 待办详情，带 `myTask.taskId` |
| `VIEW_FLOW` | 不出按钮 | 已有审批记录时间线承担查看 |
| `START_PROCESS` | 不出按钮 | 底部提示"该流程请在 PC 端发起" |

- `create` 模式：渲染配置里没有单据级动作，新建页无法判断对象是否绑定审批，因此只在从发起页进入（URL 带 `flow=1`）且有 `ai:businessFlow:start` 时，在保存按钮旁出现"提交审批"。点击后先校验、确认、保存，再用返回的主键拉运行态；运行态含 `START_FLOW` 时继续提交，否则提示"已保存，当前单据暂不可提交审批"并进入详情。其它入口新建的单据保存后在详情页按运行态提交。
- 选人弹层取消时不发起，提示"已取消提交，单据已保存"并进入详情。
- 动作成功后：进入详情模式并重新加载详情（同时重拉运行态和审批记录），调用 `badgeStore.refresh()`，提示成功。
- 每次拉运行态前先清空旧运行态，避免切换记录时按钮仍指向上一条单据的流程实例。
- 同一动作进行中再次点击时忽略（按动作 key 加锁），与管理端一致。

### 4.2 发起人自选审批人

- 新组件 `components/flow/InitiatorSelectSheet.vue`，用 `AiPopupSheet` 承载。
- 每个节点一段：节点名、单选/可多选、已选成员（姓名标签，点击移除）、"添加"按钮（单选节点已选时显示"更换"）。
- 选人复用通讯录接口 `getContactMembers`（同租户有效成员，支持搜索），列表复用 `ContactAvatar`。
- `multiple === false` 的节点单选；全部节点选齐前"确定"不可用。
- 选择结果组装为 `PROCESS_START_USER`；规则与管理端 `collectInitiatorSelectSelections` 一致，抽成 `utils/initiator-select.js` 并单测。

### 4.3 代码组织

- 新建 `composables/lowcode/useLowcodeDocumentFlow.js`：运行态加载、提交、重提、撤回、跳转待办、加锁。
- 运行态和按钮放 Pinia：`store/modules/documentFlow.js`（`useDocumentFlowStore`：运行态、按钮、提示、加锁 key、新建页是否可提交、当前用户的审批权限）。状态卡和页脚直接读 store，只向页面单层 emit `flow-action`。权限由 composable 写入 store，store 不依赖 auth store，便于单测。
- 选人状态放 Pinia：`store/modules/initiatorSelect.js`（`useInitiatorSelectStore`：节点、选择结果、打开和关闭、返回 Promise）。运行页和弹层都直接读写 store，不经 props 层层传递。
- `pages/lowcode-runtime.vue` 只做接线，行数增量控制在 60 行以内（不超过 640 行）。

### 4.4 权限

- 权限沿用现有 `fetchAccessSnapshot`（登录成功和工作台刷新时加载，持久化在 `authStore.permissions`，退出登录时清空），不新增请求；新增 getter `hasPermission(code)`，兼容超级管理员通配（`*:*:*`）。
- 没有 `ai:businessDocument:view`：不拉运行态，不显示审批按钮。
- 没有 `ai:businessFlow:start`：隐藏"提交审批"和"重新提交"；如果运行态给出了这两个动作，在状态卡提示"暂无提交审批权限，请联系管理员"。
- 没有 `ai:businessDocument:withdraw`：隐藏"撤回"。
- 前端隐藏只是体验，后端注解仍是权限依据；接口返回 403 时提示同样的文案。

## 5. 公告

### 5.1 页面

- **消息页顶部公告入口**：消息列表上方一张白色卡片行，左侧公告图标（橙色色调），标题"公告"，副标题为最新一条公告标题，右侧未读数红点加箭头；没有公告时整行隐藏。点击进入公告列表。
- **工作台最新公告条**：概览卡片下方一行，显示"公告"标签、最新一条标题（优先置顶），右侧箭头；点击进入该公告详情。没有公告时不显示。
- **公告列表** `pages/notice/index`：
  - 二级页面，标题"公告"；
  - 页签：全部 / 未读（未读页签前端按 `isRead` 过滤当前已加载数据，不新增后端参数）；
  - 每行：置顶标签、标题（未读加粗加红点）、公告类型、发布人、发布时间；
  - 分页加载，下拉刷新。
- **公告详情** `pages/notice/detail?noticeId=`：
  - 标题、类型、发布人、发布时间；
  - 正文用 `sanitizeMessageHtml` 净化后交给 `rich-text`；
  - 附件列表：文件名、大小，点击用现有文件下载能力打开（带 Token）；
  - 加载成功后调用 `markAsRead`，成功后刷新角标；返回列表时该行变为已读。

### 5.2 角标

- `badgeStore` 新增 `noticeUnreadCount` 和 `setNoticeUnreadCount()`，`refresh()` 中并行请求 `/system/notice/user/unread-count`。
- "消息"页签角标 = `unreadCount + noticeUnreadCount`；工作台"未读消息"概览数字保持只算站内消息。
- 公告接口失败时保留上次的值，不影响站内消息数。

### 5.3 H5 API

| 方法 | 请求 |
|------|------|
| `getNoticePage(params)` | `GET /system/notice/user/page`，`pageNum`、`pageSize` |
| `getNoticeUnreadCount()` | `GET /system/notice/user/unread-count` |
| `getNoticeDetail(noticeId)` | `GET /system/notice/user/{noticeId}` |
| `markNoticeRead(noticeId)` | `POST /system/notice/markAsRead`，`params: { noticeId }`，`encrypt: true` |

GET 请求不设置 `encrypt`（请求加密只作用于请求体），响应由拦截器统一解密。

## 6. 新增 H5 API（发起审批）

| 方法 | 请求 | 代理目标 |
|------|------|----------|
| `getStartableObjects()` | `GET /ai/business/flow/startable-objects` | flow-server |
| `getBusinessDocumentRuntime(objectCode, recordId)` | `GET /ai/business/document/{objectCode}/{recordId}/runtime` | app-server |
| `getBusinessFlowStartConfig(objectCode)` | `GET /ai/business/flow/start-config/{objectCode}` | flow-server |
| `startBusinessDocumentFlow(data)` | `POST /ai/business/flow/start`，`encrypt: true` | flow-server |
| `resubmitBusinessDocumentFlow(data)` | `POST /ai/business/flow/resubmit`，`encrypt: true` | flow-server |
| `withdrawBusinessDocumentFlow(data)` | `POST /ai/business/flow/withdraw`，`encrypt: true` | flow-server |

路径参数统一 `encodeURIComponent`。

## 7. 路由与契约测试

### 7.1 `pages.json`

新增三个二级页面（原生导航栏）：`pages/approval/start`、`pages/notice/index`、`pages/notice/detail`。

### 7.2 契约测试（`console-design-system.test.js` 追加，不改已有断言）

1. 三个新页面存在于 `pages.json`，且不使用 `"navigationStyle": "custom"`。
2. `api/index.js` 包含第 5.3、6 节的接口路径；`markNoticeRead` 和三个流程写接口带 `encrypt: true`。
3. 工作台和待办页都有跳转 `/pages/approval/start` 的入口。
4. 公告详情使用 `sanitizeMessageHtml`，不出现 `v-html`。
5. `AiTabBar` 的消息角标读取 `badgeStore.messageTabText`，该 getter 为 `unreadCount + noticeUnreadCount`。
6. `pages/lowcode-runtime.vue` 不超过 640 行；`useLowcodeDocumentFlow.js` 存在并处理 `START_FLOW`、`RESUBMIT_FLOW`、`WITHDRAW_FLOW`、`HANDLE_TASK`。

### 7.3 单元测试

- `utils/__tests__/initiator-select.test.js`：必选节点报错、单选截断、空节点跳过。
- `utils/__tests__/document-flow-actions.test.js`：`runtimeActions` 到按钮的映射、隐藏和禁用、无权限时的处理。
- `store/modules/__tests__/badge.test.js` 追加：公告未读计入消息角标；公告接口失败不影响站内消息数。
- `utils/__tests__/startable-objects.test.js`：目录与菜单 `configKey` 取交集、按应用分组、搜索过滤。

### 7.4 后端测试

- `BusinessStartableObjectMapperContractTest`：SQL 带租户条件、`binding_type = 'FLOW'`、`target_type = 'OBJECT'`、对象和应用 `del_flag = 0`，没有 `${}`。
- `BusinessStartableObjectVO` 字段白名单。

## 8. 状态流转与风险（人工审查项）

- **提交、撤回、重提都是状态流转**：H5 不实现任何状态判断，按钮完全由后端运行态的 `runtimeActions` 决定，调用的也是管理端在用的同一组接口，不新增后端状态逻辑。
- **先保存再提交**：保存成功、提交失败时，单据停留在草稿状态，用户可以再次提交；不做自动回滚。
- **选人范围**：发起人自选审批人复用通讯录，只能选同租户有效成员。若流程配置了更窄的候选范围，以后端校验为准，失败时展示后端错误信息。
- **目录接口只校验登录**：只暴露单据名称和编码；H5 再与菜单取交集。实际新建、提交仍受原有权限控制。
- **公告正文 XSS**：必须经过 `sanitizeMessageHtml`；契约测试禁止 `v-html`。
- **回滚**：回滚相关提交即可；后端只新增一个只读接口和 Mapper，没有数据库变更。

## 9. 已确认决策（2026-10-09）

1. 发起范围：只列绑定了审批流程的低代码业务应用，复用低代码运行页填单、保存、提交审批。
2. 入口：工作台概览卡片"发起审批"，待办页顶栏"+"，进入同一个发起页。
3. 权限：保持 `ai:businessFlow:start`、`ai:businessDocument:view` 校验，由管理员给角色授权；H5 没有权限时隐藏按钮并提示联系管理员。撤回用的 `ai:businessDocument:withdraw` 按同一原则处理。
4. 公告：消息页顶部固定公告入口，工作台显示一条最新公告，公告未读数计入消息角标。
