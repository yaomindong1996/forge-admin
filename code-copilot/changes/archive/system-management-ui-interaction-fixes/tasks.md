# 任务拆分 — 系统管理页面交互一致性修复
> status: complete
> created: 2026-08-08
> 拆分顺序：回归契约 → 菜单状态修复 → 共享样式与页面滚动 → 增量验证

## 前置条件

- [x] 已读取根 `AGENTS.md`、项目记忆、编码规范和自动化测试标准。
- [x] 已创建分支 `fix/system-management-ui-interaction-fixes-20260808`。
- [x] 已确认本变更不涉及后端接口、数据库或权限边界。

## Task 1: 修复菜单资源上下文与刷新重绑

- [x] **目标**：叶节点显示空子集，资源树刷新后当前选中行始终引用最新树节点。
- **涉及文件**：
  - `forge-admin-ui/src/views/system/menu-interaction-utils.js` — 新增资源上下文和最新行解析纯函数。
  - `forge-admin-ui/src/views/system/__tests__/menu-interaction-utils.spec.js` — 覆盖根节点、叶节点、后代展开、重绑和删除场景。
  - `forge-admin-ui/src/views/system/menu.vue` — 接入纯函数并在树刷新后协调 `selectedResourceId/selectedRow`。
- **关键签名**：
  ```js
  export function resolveResourceContextRows(allResources, currentNode, options = {}) {}
  export function resolveFreshResourceRow(flatResources, selectedRow) {}
  ```
- **验收**：点击叶节点时资源列表为空；删除子资源并刷新后，父资源对象的 `children` 不再包含已删除项。

## Task 2: 统一选择控件高度和组织树滚动

- [x] **目标**：同尺寸输入控件与按钮高度一致，三处系统管理组织树可滚动到底部。
- **涉及文件**：
  - `forge-admin-ui/src/styles/theme.css` — 按钮高度使用组件 `--n-height`。
  - `forge-admin-ui/src/views/system/org.vue` — 统一组织树 flex 滚动约束。
  - `forge-admin-ui/src/views/system/user.vue` — 补齐 `min-height: 0` 与滚动边界。
  - `forge-admin-ui/src/views/system/post.vue` — 补齐 `min-height: 0` 与滚动边界。
  - `forge-admin-ui/src/views/system/__tests__/system-management-ui-contract.spec.js` — 校验共享尺寸和三页滚动契约。
- **关键样式**：
  ```css
  .n-button {
    height: var(--n-height, 34px) !important;
  }

  .org-tree-content {
    min-height: 0;
    overflow-y: auto;
    overscroll-behavior: contain;
    scrollbar-gutter: stable;
  }
  ```
- **验收**：默认/大号输入和按钮无高度错位；组织树内容超过面板高度后出现内部滚动且最后节点可见。

## Task 3: 增量验证与文档回填

- [x] **目标**：完成前端定向验证、生产构建和可用范围内的浏览器检查，并记录证据。
- **涉及文件**：
  - `code-copilot/changes/system-management-ui-interaction-fixes/spec.md`
  - `code-copilot/changes/system-management-ui-interaction-fixes/tasks.md`
  - `code-copilot/changes/system-management-ui-interaction-fixes/test-spec.md`
  - `code-copilot/changes/system-management-ui-interaction-fixes/execution-log.md`
- **命令**：
  ```bash
  cd forge-admin-ui
  pnpm exec vitest run src/views/system/__tests__/menu-interaction-utils.spec.js src/views/system/__tests__/system-management-ui-contract.spec.js
  pnpm exec eslint src/views/system/menu.vue src/views/system/menu-interaction-utils.js src/views/system/org.vue src/views/system/user.vue src/views/system/post.vue src/views/system/__tests__/menu-interaction-utils.spec.js src/views/system/__tests__/system-management-ui-contract.spec.js
  source ~/.nvm/nvm.sh && nvm use v20.19.0
  NODE_OPTIONS=--max-old-space-size=8192 pnpm build
  ```
  ```bash
  git diff --check
  ```
- **验收**：实际命令、结果、警告、浏览器覆盖范围和服务清理情况全部写入 `execution-log.md`。
- **结果**：13/13 定向测试、目标 ESLint、生产构建和补丁格式检查通过；浏览器确认 large 输入/按钮均为 `40px`、独立挂载 small 按钮均为 `28px`。后端 `8580` 未启动，未执行真实菜单删除 E2E。
