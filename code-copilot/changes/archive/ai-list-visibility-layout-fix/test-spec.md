# 测试 Spec — AI 列表可见性与通用布局修复

> status: complete
> created: 2026-07-21

## 1. 增量范围

| 级别 | 场景 | 预期 |
|------|------|------|
| P0 | 两个页面根容器高度 | `AiCrudPage` 获得明确可用高度，表格正文不塌陷 |
| P0 | 无数据列表 | 复用 `AiTable` 的 `NEmpty` 空状态并可见 |
| P1 | 多列表格横向滚动 | 由共享组件根据列宽自动计算，固定操作列保持正常 |
| P1 | 页面既有功能 | 搜索、分页、编辑/详情/预览等逻辑不变 |
| P2 | 前端集成 | 目标 ESLint 和生产构建通过 |

## 2. 执行命令

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
cd forge-admin-ui
pnpm exec vitest run src/components/ai-form/__tests__/AiTable.spec.js
pnpm exec eslint src/views/ai/prompt-template.vue src/views/ai/dashboard-generate-record.vue
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

```bash
rg -n 'max-height="calc\(100vh - 300px\)"|:scroll-x="(1560|1600)"|min-height: 100%' \
  forge-admin-ui/src/views/ai/prompt-template.vue \
  forge-admin-ui/src/views/ai/dashboard-generate-record.vue
git diff --check
```

## 3. 浏览器验证

- 复用用户已有服务，不主动停止或重启用户进程。
- 登录后分别访问 `/ai/prompt-template`、`/ai/dashboard-generate-record`。
- 有数据时检查表头、数据行、横向滚动和分页均可见。
- 使用不匹配的筛选条件检查“没有匹配结果”空状态；若无初始数据，检查“暂无数据”空状态。
- 当前环境不能启动浏览器或访问本地服务时，在执行日志中记录限制，不将浏览器验证表述为通过。

## 4. 完成标准

- 两个页面不再包含冲突的固定表格高度和固定横向滚动宽度。
- 定向测试、目标 ESLint、生产构建和差异检查通过。
- 浏览器验证已执行，或有明确的非代码环境限制记录。

## 5. 实际结果

| 验证项 | 结果 |
|--------|------|
| AiTable 定向 Vitest | passed，1 file / 1 test |
| 两个目标页面 ESLint | passed，0 errors / 0 warnings |
| 布局静态检查 | passed，固定 `max-height`、固定 `scroll-x` 和旧 `min-height: 100%` 均无残留；共享 `NEmpty` 存在 |
| 前端生产构建 | passed，8725 modules，1m38s |
| `git diff --check` | passed |
| 浏览器自动化 | blocked，Chromium 在当前 macOS 沙箱中注册 MachPort 被拒绝；未停止用户已有服务 |
