# 增量测试计划 - 页面搭建器数据体验统一改造

> change: `page-builder-data-experience`
> created: 2026-08-16

## 1. 本轮范围

- P0：六类数据区块绑定对象后的字段目录预加载与缓存复用。
- P1：组件目录中文业务名称、业务描述、技术名次要标注与中文分组。
- P2：未绑定数据源引导态、引导按钮事件和属性面板联动。
- P3：数据模板创建后保留在页面画布，现有表单设计与对象设计入口可达。
- 回归：既有 app-center 测试、AiCrudPage 存量路径、前端生产构建。

## 2. 自动化验证

| 优先级 | 验证项 | 命令 | 预期 |
|---|---|---|---|
| P0 | 修改前 app-center 基线 | `pnpm vitest run src/views/app-center` | 记录真实通过/失败数，历史失败单独标注 |
| P0 | 页面搭建器目标测试 | `pnpm vitest run <本轮新增或修改的测试文件>` | 字段加载、引导态、模板落点均通过 |
| P0 | app-center 回归 | `pnpm vitest run src/views/app-center` | 不新增失败 |
| P1 | 相关文件 ESLint | `pnpm exec eslint <本轮变更的 JS/Vue/测试文件>` | 0 error |
| P1 | 前端生产构建 | `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 构建成功 |
| P2 | 差异检查 | `git diff --check` | 无空白错误 |

所有 Node 命令前执行 `source ~/.nvm/nvm.sh && nvm use v20.19.0`。

## 3. 浏览器验证

在不修改数据库和业务运行态的前提下启动本轮 Vite 服务，至少验证：

1. 组件目录主标题显示“数据列表”“数据表单”，技术名仅为次要信息。
2. 空白页加入数据表单后显示数据源引导态，点击入口可定位属性面板。
3. 数据模板创建后停留画布，仍可进入现有表单设计器。
4. 若本地后端和可用对象数据存在，选择业务对象后验证字段立即渲染；否则明确记录为跳过的真实服务 E2E。

## 4. 跳过边界

- 本变更不改后端和数据库，不执行 Maven/Flyway 验证。
- 预售单已发布对象页的真实接口回归依赖用户本地 Admin/数据库状态；没有可用环境时只覆盖静态契约、组件测试和构建，并在执行日志记录跳过原因。

## 5. Review Fix 增量范围

| 优先级 | 验证项 | 覆盖内容 |
|---|---|---|
| P0 | 对象切换 | 对象变化时重置字段/查询配置；同对象不重置 |
| P0 | AiForm 字段解析 | 显式引用顺序、字段隐藏、空引用安全默认字段 |
| P0 | 嵌套运行时上下文 | 子区块独立字段/CRUD/loading/数据源解析，完整树预加载与缓存守卫 |
| P0 | AiForm 独立提交 | 仅发布运行态提交、create API 与默认参数、成功/失败、loading/reset |
| P1 | 失效对象引导 | `valid === false` 或无法匹配当前对象时显示数据源引导并停止预加载 |

Fix 继续复用 Apply 基线：app-center 的 `app-entry-targets.spec.js` 两条移动端 URL 断言为历史失败；真实服务 UAT 跳过边界不变。

## 6. Data Experience Follow-up 增量验证

- 字段查询规则：运行时应在 `DATASET` 查询源下透传配置的 `pageNum/pageSize/maxRows`，接口源不附加分页参数。
- 字段查询参数：常用上下文选项（当前登录用户 ID、租户 ID、组织 ID、扫码内容）应可直接选择，自定义路径作为高级兜底。
- 应用工作台：对象添加动作合并为菜单；对象列表、页面入口列表及窄屏工作台导航不得通过固定 `min-width` 强制横向滚动。
- 本轮命令：`pnpm vitest run src/components/ai-form/__tests__/field-event-runtime.spec.js`、相关文件 ESLint、生产构建、`git diff --check`。
