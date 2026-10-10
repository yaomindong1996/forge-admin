# H5 办公基础能力 Execution Log

## 2026-10-09 提案

- 调研管理端低代码单据的发起链路（`useAiCrudPage.part2.js`）、单据运行态和动作类型（`BusinessDocumentRuntimeActionPolicy`）、`BusinessFlowController` 权限注解、`ai_business_binding` 等表结构、公告用户侧接口（`SysNoticeController`）、H5 低代码运行页和代理配置。
- 结论：后端提交、撤回、重提、公告接口都已存在；缺"可发起单据目录"接口，H5 缺发起页、单据审批动作和公告页面。
- 流程入口 `sys_flow_entry` 没有管理端配置页面，菜单已下线，不纳入本变更。
- 用户确认：只做低代码应用；工作台和待办页两处入口；保持权限校验，由管理员授权；公告放消息页顶部和工作台，未读数计入消息角标。
- 调研中发现撤回另需 `ai:businessDocument:withdraw`，按同一原则处理，已写入 spec。
- 本轮只写文档，没有改代码。

## 2026-10-09 /apply

### 实现

- 后端：`GET /ai/business/flow/startable-objects`（`BusinessStartableObjectVO`、`BusinessStartableObjectMapper` + XML、`BusinessStartableObjectService`），只读，无数据库变更。应用表没有排序字段，排序改为按应用 `id`，spec 3.4 已同步。
- H5 基础：`utils/permission.js`、`authStore.hasPermission`、10 个 API 方法、`badgeStore.noticeUnreadCount` 与 `messageTabText`。
- 公告：列表、详情（`sanitizeMessageHtml` 渲染，未读时标记已读并扣减角标）、消息页顶部入口、工作台概览卡内最新公告条。工作台桌面端栅格区域有契约测试锁定，所以公告条放在概览卡内部，不新增区域。
- 发起审批：`pages/approval/start`（目录与用户菜单 `configKey` 取交集后按应用分组，可搜索）；工作台概览卡"发起审批"和待办页顶栏"+"两个入口。
- 自选审批人：`InitiatorSelectSheet` + `useInitiatorSelectStore`，`open(nodes)` 返回 Promise，规则与管理端 `collectInitiatorSelectSelections` 一致。
- 单据审批：`useDocumentFlowStore` 保存运行态和按钮，`useLowcodeDocumentFlow` 负责提交、重提、撤回、跳转待办；状态卡 `LowcodeDocumentFlowStatus` 和默认页脚直接读 store。`lowcode-runtime.vue` 576 → 613 行。
- 与 spec 的偏差（spec 已同步）：
  - 渲染配置里只有应用流程动作（`START_PROCESS`），没有单据级 `START_FLOW`，新建页无法判断是否绑定审批。新建页"提交审批"只在从发起页进入（`flow=1`）时出现，保存后以运行态为准。
  - 运行态每次加载前清空，防止切换记录时撤回带上上一条单据的流程实例。
  - `documentFlow` store 最初直接引用 auth store，构建多出一条 Vite 动态导入提示；改为由 composable 写入权限，store 不依赖 auth，顺带可在 Node 中单测。

### 验证

- `node --test src/utils/__tests__ src/store/modules/__tests__`：142/142 通过（基线 105，新增 37）。新增 `initiator-select`、`document-flow-actions`、`permission`、`notice`、`startable-objects`、`documentFlow`、`initiatorSelect` 单测，`badge` 追加 2 条，`console-design-system` 追加 5 条契约，`feedback-components` 页面清单补 3 个新页面。已有断言未改；`startable-objects` 的期望 URL 随新增 `flow=1` 参数更新。
- `npx -y pnpm@9 build:h5`：构建成功。两条 Vite 提示均为上一变更 `badge.js` 的有意动态导入（`@/api`、`./auth`，为 Node 单测），本变更没有新增提示。
- 后端：本机没有 JDK/Maven，**编译和 `BusinessStartableObjectMapperContractTest` 未执行**。需执行：`cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Penable-tests -Dtest='BusinessStartableObject*' -Dsurefire.failIfNoSpecifiedTests=false`。

### 预览

- `pnpm dev:h5 --port 3009`，后端未启动；`localStorage.default_auth` 写入假 token 和三项审批权限，接口数据通过 Pinia / 组件状态注入。
- 发起页：无数据时显示"暂无可发起的审批 / 如需开通请联系管理员"；注入 6 个对象后按"人事行政 / 财务 / 其他"分组，图标正常。
- 运行页详情（审批中）：顶部状态卡"审批中 · 流程流转中"，页脚单行"返回列表 / 撤回"。新建（`flow=1`）：页脚"取消 / 保存 / 提交审批"，无状态卡。
- 选人弹层：节点概览（单选节点已选时显示"更换"）、成员列表勾选、确定后 Promise 返回 `{ leader: ['2'], hr: ['3','4','5'] }`，弹层关闭。
- 工作台：概览卡"发起审批"按钮和公告条；消息页签角标 3（站内 2 + 公告 1），工作台"未读消息"仍为 2。消息页顶部公告入口带未读数。
- 预览结束已停止开发服务器，3009 端口已释放。

### 待用户联调

- 真实接口下：目录接口返回、提交（含选人）/ 撤回 / 驳回后重提的完整链路、`disabledReason` 展示、公告标记已读后角标扣减。
- 提交、撤回、重提属于状态流转，H5 只按后端 `runtimeActions` 出按钮，调用管理端同一组接口，需人工审查（spec 第 8 章）。
