# 单测 Spec — 系统管理页面交互一致性修复
> status: apply
> created: 2026-08-08

## 0. 测试原则

- 复用现有 Vitest、ESLint 和前端生产构建基线，只验证本轮差异。
- 菜单状态逻辑先写失败测试，再实现最小修复。
- CSS 行为使用静态契约保护关键 flex/尺寸属性，最终视觉和滚动能力尽量通过浏览器验证。
- 不启动本轮不需要的后端、Flow、MySQL 或 Redis。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| 前端单测 | Vitest 2 + jsdom |
| 组件框架 | Vue 3.5 + Naive UI |
| 静态检查 | ESLint 9 + `git diff --check` |
| 集成检查 | Vite production build |

## 2. 覆盖范围

### P0 — 菜单状态逻辑

| 方法 | 场景 | 输入 | 预期结果 |
|------|------|------|----------|
| `resolveResourceContextRows` | 全部资源根上下文 | `currentNode=null` | 返回顶级资源 |
| `resolveResourceContextRows` | 有直接子项 | 当前节点含 `children` | 返回直接子项 |
| `resolveResourceContextRows` | 叶节点 | `children=null/undefined` | 返回空数组 |
| `resolveResourceContextRows` | 当前节点搜索 | `includeDescendants=true` | 仅展开当前节点后代 |
| `resolveFreshResourceRow` | 刷新后资源仍存在 | 旧对象 + 新扁平树 | 返回新对象引用 |
| `resolveFreshResourceRow` | 资源已删除 | 旧对象 + 无对应 ID | 返回 `null` |

### P1 — UI 样式契约

- `theme.css` 的默认、小号、微型和大号按钮高度均使用 `var(--n-height)`。
- `org.vue`、`user.vue`、`post.vue` 的 `.org-tree-content` 同时包含 `min-height: 0`、`overflow-y: auto`、`overscroll-behavior: contain` 和 `scrollbar-gutter: stable`。

### P2 — 集成与视觉

- 目标 Vue/JS/测试文件通过 ESLint。
- 前端 production build 通过。
- 前端开发服务可启动；若后端不可用，浏览器验证限定为可访问页面和静态几何检查，并在日志明确说明菜单数据链路未做真实 E2E。

## 3. 执行计划

- [x] Step 1: 执行新增 Vitest，确认菜单回归测试在实现前失败。
- [x] Step 2: 实现菜单修复并确认定向 Vitest 通过。
- [ ] Step 3: 实现共享尺寸和三页滚动样式，执行样式契约测试。
- [ ] Step 4: 执行目标 ESLint、生产构建和 `git diff --check`。
- [ ] Step 5: 启动前端开发服务并执行可用范围内的浏览器检查。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-15 | 前端生产构建 | `pnpm build` | passed | 见 `user-reported-platform-issues/execution-log.md`；当前代码在此后已有提交，不能替代本轮验证 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-08-08 | 菜单交互与 UI 样式契约 Red | Vitest | `pnpm exec vitest run src/views/system/__tests__/menu-interaction-utils.spec.js src/views/system/__tests__/system-management-ui-contract.spec.js` | failed，菜单工具无法导入；样式契约 4/4 失败 | 预期 Red 基线 |
| 2026-08-08 | 菜单交互与 UI 样式契约 Green | Vitest | 同上 | passed，2 files / 13 tests | 无 |

## 6. 执行证据

- `execution-log.md`：同目录增量记录。
- 关键接口：接口协议无变更；真实资源树数据验证视本地后端可用性执行。
- 关键数据库检查：无数据库变更，不执行。
- 服务启动与停止：只管理本轮启动的前端开发服务。
