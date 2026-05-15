# 任务清单：report-multi-page-canvas
> status: propose
> created: 2026-05-15
> 拆分顺序：数据模型 → 协议工具 → Store 编排 → 编辑器入口 → 预览运行时 → 事件与参数 → 验证

## 前置条件
- [x] 已确认 `/chart/home/:id` 实际位于 `forge-report-ui` 子应用。
- [x] 已确认后端 `ai_report_project.component_data` 为 `longtext`，可先承载多页面 JSON。
- [x] 已确认项目已有组件基础事件、动态参数、animate.css 动画能力可复用。
- [ ] 确认第一版页面管理 UI 放置位置。
- [ ] 确认第一版跳转参数来源范围。

## 任务总览

| Task | 名称 | 状态 | 优先级 |
|------|------|------|--------|
| Task 1 | 多页面协议与兼容工具 | completed | P0 |
| Task 2 | chartEditStore 多页面状态 | completed | P0 |
| Task 3 | 编辑器页面管理面板 | completed | P0 |
| Task 4 | 保存、自动保存、预览、发布改造 | completed | P0 |
| Task 5 | 预览页多页面运行时 | completed | P0 |
| Task 6 | 组件页面跳转动作配置 | completed | P0 |
| Task 7 | 下钻上下文与动态参数接入 | pending | P0 |
| Task 8 | JSON 导入导出和代码编辑兼容 | pending | P1 |
| Task 9 | 构建验证与手动回归 | pending | P0 |

---

## Task 1: 多页面协议与兼容工具

**目标**: 建立多页面 JSON 类型和协议转换工具，保证旧单画布项目可无感加载。

**状态**: completed

**涉及文件**:
- `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.d.ts` — 新增 `ReportCanvasPage`、`ReportMultiPageStorage`、`ReportPageTransition`、`ComponentAction`、`DrillParamBinding` 类型。
- `forge-report-ui/src/utils/reportPages.ts` — 新增多页面协议工具。
- `forge-report-ui/src/packages/public/publicConfig.ts` — 为组件事件补充默认 `actions: []`。
- `forge-report-ui/src/views/chart/hooks/useSync.hook.ts` — 加载旧组件时补齐 `events.actions` 默认值。

**关键签名**:
```ts
export function normalizeProjectStorage(storage: unknown, fallbackName?: string): ReportMultiPageStorage
export function createDefaultPage(name?: string): ReportCanvasPage
export function extractPageStorage(project: ReportMultiPageStorage, pageId?: string): ChartEditStorage
export function updatePageStorage(project: ReportMultiPageStorage, pageId: string, storage: ChartEditStorage): ReportMultiPageStorage
export function clonePageWithNewIds(page: ReportCanvasPage, name?: string): ReportCanvasPage
```

**验收标准**:
- 旧结构 `{ editCanvasConfig, requestGlobalConfig, componentList }` 可转换为 `version=2` 的单页面项目。
- 新结构可按 `pageId` 提取单页画布数据。
- 复制页面时页面 ID 和组件 ID 均重新生成。

**验证**:
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/store/modules/chartEditStore/chartEditStore.d.ts src/packages/index.d.ts src/packages/public/publicConfig.ts src/views/chart/hooks/useSync.hook.ts src/utils/reportPages.ts
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**结果**:
- ESLint 无错误；`publicConfig.ts` 被项目 ignore 规则跳过。
- `pnpm build` 通过；输出的 lottie `eval`、Rollup 循环 chunk、CSS `:deep()`、chunk size 均为既有警告。

---

## Task 2: chartEditStore 多页面状态

**目标**: Store 支持项目页面列表、当前页面、首页、页面切换和项目级保存快照。

**状态**: completed

**涉及文件**:
- `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.ts` — 增加多页面 state/getters/actions。
- `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.d.ts` — 增加 store 类型定义。
- `forge-report-ui/src/views/chart/ContentEdit/hooks/useLayout.hook.ts` — 使用 `loadProjectStorage()` 代替直接 `updateComponent()`。
- `forge-report-ui/src/views/preview/utils/storage.ts` — 复用 store 的多页面加载能力。

**关键签名**:
```ts
loadProjectStorage(storage: unknown, initialPageId?: string): Promise<void>
getProjectStorageInfo(): ReportMultiPageStorage
flushCurrentPage(): void
switchPage(pageId: string): Promise<void>
createPage(name?: string): Promise<string>
duplicatePage(pageId: string): Promise<string>
deletePage(pageId: string): Promise<void>
renamePage(pageId: string, name: string): void
setHomePage(pageId: string): void
```

**验收标准**:
- 切换页面前当前页面改动不会丢失。
- 切换页面后画布尺寸、背景、组件列表、请求配置正确恢复。
- 删除当前页后能自动切换到相邻页面或首页。

**验证**:
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/store/modules/chartEditStore/chartEditStore.ts src/store/modules/chartEditStore/chartEditStore.d.ts src/views/chart/ContentEdit/hooks/useLayout.hook.ts src/views/preview/utils/storage.ts src/utils/reportPages.ts
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**结果**:
- ESLint 无错误。
- `pnpm build` 通过；输出的 lottie `eval`、Rollup 循环 chunk、CSS `:deep()`、chunk size 均为既有警告。

---

## Task 3: 编辑器页面管理面板

**目标**: 在 `/chart/home/:id` 工作空间内提供多画布页面管理入口。

**状态**: completed

**涉及文件**:
- `forge-report-ui/src/views/chart/ContentPages/index.vue` — 新增页面管理面板。
- `forge-report-ui/src/views/chart/ContentPages/components/PageListItem.vue` — 页面项组件。
- `forge-report-ui/src/views/chart/index.vue` — 挂载页面面板。
- `forge-report-ui/src/views/chart/ContentConfigurations/index.vue` 或布局相关文件 — 调整编辑区布局，避免遮挡画布。
- `forge-report-ui/src/store/modules/chartHistoryStore/chartHistoryStore.ts` — 页面切换时清理或隔离历史栈。

**关键交互**:
- 新增空白页面。
- 复制当前页面。
- 删除页面。
- 重命名页面。
- 设置为首页。
- 切换页面。

**验收标准**:
- 至少一个页面时禁止删除。
- 当前页高亮，首页有明确标记。
- 页面切换不影响画布缩放计算。

**验证**:
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/chart/ContentPages/index.vue src/views/chart/ContentPages/components/PageListItem.vue src/views/chart/index.vue src/store/modules/chartEditStore/chartEditStore.ts
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**结果**:
- ESLint 无错误。
- `pnpm build` 通过；输出的 lottie `eval`、Rollup 循环 chunk、CSS `:deep()`、chunk size 均为既有警告。

---

## Task 4: 保存、自动保存、预览、发布改造

**目标**: 所有项目保存链路写入多页面完整 JSON，而不是仅保存当前页面。

**状态**: completed

**涉及文件**:
- `forge-report-ui/src/api/project/index.ts` — `buildProjectPayload()` 支持 `ReportMultiPageStorage`。
- `forge-report-ui/src/views/chart/hooks/useAutoSave.hook.ts` — 保存前 `flushCurrentPage()`，使用 `getProjectStorageInfo()`。
- `forge-report-ui/src/views/chart/ContentHeader/headerRightBtn/index.vue` — 预览、发布、保存校验使用多页面完整数据。
- `forge-report-ui/src/utils/reportPages.ts` — 新增默认多页面项目创建工具。
- `forge-report-ui/src/views/project/components/ProjectCreateModal/index.vue` — 新建项目默认写入多页面协议。
- `forge-report-ui/src/views/project/layout/components/ProjectLayoutCreate/components/CreateModal/index.vue` — 新建项目默认写入多页面协议。

**关键签名**:
```ts
export const buildProjectPayload = (
  rawId: string | string[] | number,
  storageInfo: ChartEditStorage | ReportMultiPageStorage,
  indexImg?: string
) => Partial<ForgeProject>
```

**验收标准**:
- 自动保存后刷新编辑器，多页面和组件均存在。
- 预览打开的是当前编辑的完整项目数据。
- 发布后后端 `componentData` 与前端预期一致。
- 新建项目直接是 `version=2` 多页面协议。

**验证**:
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/utils/reportPages.ts src/api/project/index.ts src/views/chart/hooks/useAutoSave.hook.ts src/views/chart/ContentHeader/headerRightBtn/index.vue src/views/project/components/ProjectCreateModal/index.vue src/views/project/layout/components/ProjectLayoutCreate/components/CreateModal/index.vue
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**结果**:
- ESLint 无错误。
- `pnpm build` 通过；输出的 lottie `eval`、Rollup 循环 chunk、CSS `:deep()`、chunk size 均为既有警告。

---

## Task 5: 预览页多页面运行时

**目标**: 预览页支持项目内页面切换、query 指定页面和页面过渡动画。

**状态**: completed

**涉及文件**:
- `forge-report-ui/src/views/preview/suspenseIndex.vue` — 根据当前页面渲染画布，增加 transition 包裹。
- `forge-report-ui/src/views/preview/utils/storage.ts` — 支持 `?pageId=` 和 `homePageId`。
- `forge-report-ui/src/views/preview/components/PreviewRenderList/index.vue` — 页面切换时重新初始化数据池。
- `forge-report-ui/src/views/preview/hooks/useStore.hook.ts` — 多页面运行时 store 同步。
- `forge-report-ui/src/hooks/useChartDataPondFetch.hook.ts` — 清理旧页面数据池 watcher 和轮询。
- `forge-report-ui/src/utils/reportPages.ts` — 增加预览初始页面解析工具。

**关键签名**:
```ts
switchPreviewPage(pageId: string, context?: Record<string, any>, transition?: ReportPageTransition): Promise<void>
resolveInitialPreviewPage(project: ReportMultiPageStorage, queryPageId?: string): string
```

**验收标准**:
- `/chart/preview/:id` 默认打开首页。
- `/chart/preview/:id?pageId=xxx` 打开指定页面。
- 页面切换后组件重新渲染，数据请求重新执行。
- 无效 `pageId` 自动回退首页。

**验证**:
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/utils/reportPages.ts src/views/preview/utils/storage.ts src/views/preview/suspenseIndex.vue src/hooks/useChartDataPondFetch.hook.ts
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**结果**:
- ESLint 无错误。
- `pnpm build` 通过；输出的 lottie `eval`、Rollup 循环 chunk、CSS `:deep()`、chunk size 均为既有警告。

---

## Task 6: 组件页面跳转动作配置

**目标**: 在组件事件配置中新增结构化「页面跳转」动作，不要求用户写 JS。

**状态**: completed

**涉及文件**:
- `forge-report-ui/src/enums/eventEnum.ts` — 新增 action 类型枚举。
- `forge-report-ui/src/views/chart/ContentConfigurations/components/ChartEvent/index.vue` — 挂载动作配置入口。
- `forge-report-ui/src/views/chart/ContentConfigurations/components/ChartEvent/components/ChartEventPageAction/index.vue` — 新增页面跳转动作配置组件。
- `forge-report-ui/src/hooks/useLifeHandler.hook.ts` — 运行时合并结构化动作与原基础事件。
- `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.ts` — 增加运行时页面转场覆盖值。
- `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.d.ts` — 增加运行时页面转场类型。
- `forge-report-ui/src/views/preview/suspenseIndex.vue` — 支持动作级临时转场优先级。
- `forge-report-ui/src/views/preview/utils/storage.ts` — 切页时写入运行时转场。

**关键签名**:
```ts
export enum ComponentActionType {
  GO_PAGE = 'goPage'
}

export function useComponentActions(chartConfig: CreateComponentType | CreateComponentGroupType): Record<string, Function>
```

**验收标准**:
- 组件可配置点击跳转目标页面。
- 目标页面下拉来自当前项目页面列表。
- 删除目标页面后配置提示失效或被清理。
- 原有基础事件 JS 仍可正常执行。

**验证**:
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/enums/eventEnum.ts src/hooks/useLifeHandler.hook.ts src/store/modules/chartEditStore/chartEditStore.ts src/store/modules/chartEditStore/chartEditStore.d.ts src/views/preview/utils/storage.ts src/views/preview/suspenseIndex.vue src/views/chart/ContentConfigurations/components/ChartEvent/index.vue src/views/chart/ContentConfigurations/components/ChartEvent/components/ChartEventPageAction/index.vue
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm build
```

**结果**:
- ESLint 无错误。
- `pnpm build` 通过；输出的 lottie `eval`、Rollup 循环 chunk、CSS `:deep()`、chunk size 均为既有警告。

---

## Task 7: 下钻上下文与动态参数接入

**目标**: 页面跳转可携带下钻参数，目标页面的数据请求可通过动态参数读取。

**涉及文件**:
- `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.ts` — 新增 `runtimePageContext` 管理。
- `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.d.ts` — 动态参数来源增加 `pageContext`。
- `forge-report-ui/src/utils/reportDrill.ts` — 新增下钻参数解析工具。
- `forge-report-ui/src/utils/requestDynamicParams.ts` — 增加页面上下文取值来源。
- `forge-report-ui/src/views/chart/ContentConfigurations/components/ChartEvent/components/ChartEventPageAction/index.vue` — 增加参数绑定 UI。
- `forge-report-ui/src/views/chart/ContentConfigurations/components/ChartData/components/ChartDataRequest/components/RequestTargetConfig/index.vue` — 动态参数来源增加「页面上下文」。

**关键签名**:
```ts
export function resolveDrillParams(
  bindings: DrillParamBinding[],
  eventPayload: any,
  pageContext: Record<string, any>
): Record<string, any>
```

**验收标准**:
- 从首页点击携带 `regionCode` 后，详情页请求参数可读取 `pageContext.regionCode`。
- 参数为空时按动态参数 fallback 逻辑处理。
- 不执行任意用户脚本。

---

## Task 8: JSON 导入导出和代码编辑兼容

**目标**: 保证 JSON 编辑、导入、导出对新旧协议都可用。

**涉及文件**:
- `forge-report-ui/src/views/edit/index.vue` — JSON 编辑页支持多页面协议。
- `forge-report-ui/src/views/chart/ContentEdit/components/EditTools/index.vue` — 导入/导出使用完整项目协议。
- `forge-report-ui/src/views/chart/ContentEdit/components/EditTools/utils/index.ts` — 导出 JSON 包含 pages。
- `forge-report-ui/src/views/chart/ContentEdit/components/EditTools/hooks/useFile.hooks.ts` — 导入时 normalize。
- `forge-report-ui/src/views/chart/ContentEdit/components/EditTools/hooks/useSyncUpdate.hook.ts` — 同步到预览时使用项目级 storage。

**验收标准**:
- 导出文件包含所有页面。
- 导入旧单页面 JSON 可自动变成一个页面。
- 导入新多页面 JSON 可恢复全部页面。
- JSON 编辑同步后编辑器和预览均正确显示。

---

## Task 9: 构建验证与手动回归

**目标**: 完成构建和关键路径验证。

**涉及文件**:
- `code-copilot/changes/report-multi-page-canvas/spec.md` — 更新执行日志。
- `code-copilot/changes/report-multi-page-canvas/tasks.md` — 更新任务状态。
- `code-copilot/changes/report-multi-page-canvas/test-report.md` — 记录验证结果。

**验证命令**:
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-report-ui && pnpm build
```

**手动回归清单**:
- 新建项目后进入编辑器，默认有「首页」页面。
- 新增第二个页面，分别添加不同组件，刷新后仍存在。
- 复制页面后组件 ID 不重复。
- 删除页面后跳转动作无失效报错。
- 首页组件点击跳转详情页，并携带下钻参数。
- 详情页组件动态请求可读取页面上下文参数。
- 预览默认打开首页，`?pageId=` 打开指定页。
- 发布后用发布链接验证页面跳转和下钻参数。
- 老项目加载、保存、预览不报错。

**验收标准**:
- `pnpm build` 通过。
- 手动回归清单全部通过或明确记录遗留问题。
