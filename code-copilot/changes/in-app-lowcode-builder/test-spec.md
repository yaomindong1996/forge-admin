# 单测 Spec — 应用内低代码搭建器
> status: apply
> created: 2026-07-21

## 0. 测试原则

- 遵循 `code-copilot/rules/automated-testing-standard.md`：先记录基线，再按任务增量执行。
- 编排 Schema 使用 Vitest 单测先行；组件交互使用 `@vue/test-utils` 定向验证。
- 本变更不新增后端接口或数据库，后端真实 API/Flyway 不在本轮验证范围。

## 1. 测试框架

| 项目 | 值 |
|---|---|
| 前端测试 | Vitest（`pnpm test`） |
| Vue 组件测试 | `@vue/test-utils` |
| 构建 | Vite（`pnpm build`） |
| Lint | ESLint（`pnpm exec eslint`） |

## 2. 覆盖范围

### P0 — 应用编排 Schema

| 函数 | 场景 | 预期 |
|---|---|---|
| `normalizeInAppBuilder` | 空 options / 旧 options | 生成稳定顶层首页，不改写输入 |
| `createNavigationNode` | 在目录下创建页面/组 | 生成稳定 ID、默认顺序和正确父节点 |
| `moveNavigationNode` | 移动页面、移动首页、循环父子关系 | 正确移动；首页/循环移动被拒绝 |
| `removeNavigationNode` | 删除含子节点目录 | 必须按指定策略处理子节点 |
| `insertPageComponent` | 容器、组件后、页面末尾插入 | 使用既有组件默认属性并返回选中组件 ID |

### P1 — 前端状态与组件

- `mergeInAppBuilderOptions` 保留未知 options。
- 草稿保存失败保持脏状态。
- 页面树空目录显示“在本组创建页面”。
- 组件插入弹窗的搜索、分类和点击插入。

### P2 — 路由和浏览器验收

- 应用运行壳打开首页和对象页占位。
- 编辑者可切换编辑态；普通用户不出现编辑入口。
- 创建目录 → 创建页面 → 插入组件 → 保存 → 刷新 → 预览。

## 3. 执行计划

- [x] Step 1：新增 Schema 红灯用例并执行定向 Vitest。
- [x] Step 2：完成 Schema 实现，确认绿灯。
- [ ] Step 3：完成页面树/组件插入/草稿测试。
- [ ] Step 4：执行定向 ESLint 和 `pnpm build`。
- [ ] Step 5：按条件启动 Vite 完成浏览器验收并记录服务清理。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|---|---|---|---|---|
| 2026-07-21 | 实施前基线 | 待执行 | pending | 新变更尚无前端测试 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|---|---|---|---|---|---|
| 2026-07-21 | Task 1-2 | 编排 Schema 与 options 合并定向 Vitest | `pnpm --dir forge-admin-ui test src/views/app-center/in-app-builder/__tests__/in-app-builder-schema.spec.js` | passed | 5 tests passed；先记录缺少模块的预期 Red 结果，再实现并通过 Green。 |

## 6. 执行证据

- `execution-log.md`：记录每次命令和关键结果。
- 服务启动与停止：本轮尚未启动服务。
