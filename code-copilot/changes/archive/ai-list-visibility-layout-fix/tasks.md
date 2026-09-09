# 任务拆分 — AI 列表可见性与通用布局修复

> 执行方式：当前工作区内直接实现；保留所有无关用户改动，不自动提交或推送。

## 前置条件

- [x] 已确认两个分页接口与 `AiCrudPage` 的 `records/total` 解析兼容。
- [x] 已确认共享 `AiTable` 内置统一空状态。
- [x] 已获得用户实现确认。

## Task 1：恢复两个页面的列表可用高度

- **目标**：让数据正文、分页和空状态获得稳定的可见区域。
- **涉及文件**：
  - `forge-admin-ui/src/views/ai/prompt-template.vue`
  - `forge-admin-ui/src/views/ai/dashboard-generate-record.vue`
- [x] 移除页面级固定 `max-height`。
- [x] 移除页面级固定 `scroll-x`，使用共享组件自动计算。
- [x] 根容器改为明确高度的纵向 Flex 布局并正确处理最小高度与溢出。
- [x] 保持现有搜索、表格列、操作和弹窗逻辑不变。

## Task 2：增量验证与回填

- **目标**：确认共享空状态未回归、目标页面静态质量与生产构建通过。
- **涉及文件**：
  - `code-copilot/changes/ai-list-visibility-layout-fix/spec.md`
  - `code-copilot/changes/ai-list-visibility-layout-fix/tasks.md`
  - `code-copilot/changes/ai-list-visibility-layout-fix/test-spec.md`
  - `code-copilot/changes/ai-list-visibility-layout-fix/execution-log.md`
- [x] 执行 `AiTable.spec.js` 定向测试。
- [x] 执行两个目标 Vue 文件的 ESLint。
- [x] 使用 Node `v20.19.0` 执行前端生产构建。
- [x] 执行静态布局检查和 `git diff --check`。
- [x] 环境允许时浏览器检查有数据和无数据场景，否则记录具体限制。
- [x] 回填 Spec、Task、Test Spec 和执行日志。
