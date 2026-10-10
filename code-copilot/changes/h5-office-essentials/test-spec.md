# H5 办公基础能力 Test Spec

## 1. 环境

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
cd forge-h5-ui && npx -y pnpm@9 install
```

后端编译和测试需要本机 JDK 17 和 Maven；本机没有时记录"跳过"，由用户在真实环境补测。

## 2. 前端 Node 单测

```bash
cd forge-h5-ui && node --test src/utils/__tests__ src/store/modules/__tests__
```

基线：`h5-mobile-redesign` 完成后为 105 通过、0 失败。本变更只新增和追加用例，已有用例必须全部保持通过。

| 用例 | 文件 | 预期 |
|------|------|------|
| 新页面路由 | `console-design-system.test.js` | `approval/start`、`notice/index`、`notice/detail` 在 `pages.json` 中，且不用 `custom` 导航 |
| 接口契约 | 同上 | 第 5.3、6 节接口路径存在；`markNoticeRead`、提交、重提、撤回带 `encrypt: true` |
| 发起入口 | 同上 | 工作台和待办页都有跳转 `/pages/approval/start` 的入口 |
| 公告正文安全 | 同上 | 公告详情使用 `sanitizeMessageHtml` 和 `rich-text`，不出现 `v-html` |
| 消息角标 | 同上 | `AiTabBar` 消息角标读取 `unreadCount` 和 `noticeUnreadCount` |
| 运行页规模 | 同上 | `lowcode-runtime.vue` 不超过 640 行；`useLowcodeDocumentFlow.js` 处理四种动作 |
| 自选审批人 | `initiator-select.test.js` | 未选节点报"请选择「节点名」的审批人"；`multiple === false` 只保留第一个；无 `nodeKey` 的节点跳过 |
| 动作映射 | `document-flow-actions.test.js` | `visible === false` 不出按钮；`disabled` 带原因；无权限时隐藏并给出提示文案；`START_PROCESS` 和 `VIEW_FLOW` 不出按钮 |
| 发起目录 | `startable-objects.test.js` | 与菜单 `configKey` 取交集；按应用分组且保持后端顺序；搜索同时匹配应用名和单据名 |
| 权限判断 | `permission.test.js` | 精确匹配；`*:*:*` 通配；空列表返回 false |
| 角标 store | `store/modules/__tests__/badge.test.js` | 公告未读计入消息角标；公告接口失败时站内消息数正常更新，公告数保留旧值 |

## 3. 构建

```bash
cd forge-h5-ui && npx -y pnpm@9 build:h5
```

构建必须通过。已知且可接受的提示：`badge.js` 动态导入 `@/api`。

## 4. 后端

```bash
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Penable-tests -Dtest='BusinessStartableObject*' -Dsurefire.failIfNoSpecifiedTests=false
```

| 用例 | 预期 |
|------|------|
| SQL 契约 | 含 `tenant_id = #{tenantId}`、`target_type = 'OBJECT'`、`binding_type = 'FLOW'`、对象和应用 `del_flag = 0`；没有 `${}` |
| 字段白名单 | VO 只有第 3.4 节列出的 8 个字段 |
| 接口鉴权 | 方法带 `@ApiPermissionIgnore`；类上保留 `@ApiDecrypt`、`@ApiEncrypt` |

## 5. 联调验收（用户执行）

需要 admin-server、flow-server、app-server 和 H5 同时启动，准备一个绑定了审批流程的低代码应用，员工角色授予 `ai:businessFlow:start`、`ai:businessDocument:view`、`ai:businessDocument:withdraw`。

1. 工作台"发起审批"和待办"+"进入发起页，只看到有菜单权限的单据。
2. 新建单据并直接"提交审批"：先保存，再发起；带自选审批人节点时弹出选人，未选齐不能确定。
3. 提交后详情显示"审批中"，待办页"我发起的"出现该单据，审批人收到待办。
4. 审批人驳回到发起人：发起人打开单据看到"重新提交"，修改后重提成功。
5. 审批中撤回：状态变为已撤回，按钮消失。
6. 去掉员工的 `ai:businessFlow:start`：发起页显示空状态；单据详情不显示提交按钮并提示联系管理员。
7. 管理端发布一条公告：消息页签角标加 1，消息页顶部公告入口和工作台公告条显示标题；打开详情后角标减 1，列表变为已读；附件可以下载。
8. 公告正文含 `<script>`、`onerror` 等内容时不执行。

## 6. 视觉验收

- 本地 `npx -y pnpm@9 dev:h5`，390×844 视口截图：发起页、低代码新建页底部按钮、选人弹层、公告列表、公告详情、消息页公告入口、工作台公告条。
- 后端未启动时只能看到空状态和错误态，记录为"待用户联调"。
