# 报表多画布与下钻页面能力
> status: propose
> created: 2026-05-15
> complexity: 🔴复杂

## 1. 背景与目标
当前 `forge-report-ui` 的大屏项目是单画布模型：一个项目只保存一份画布配置和组件列表。实际大屏业务通常不是单页展示，而是从总览页点击区域、指标、企业、设备等对象后进入下钻详情页，再支持返回或继续下钻。

目标：
- 一个报表项目内支持多个画布页面，在同一个 `/chart/home/:id` 编辑器中创建、复制、删除、重命名、排序和切换页面。
- 每个页面拥有独立的画布配置、请求配置和组件列表。
- 组件可配置点击跳转到项目内另一个页面，并携带下钻参数。
- 预览和发布链接仍以项目为入口，默认打开首页，也可通过 `pageId` 指定打开页面。
- 旧单画布项目无需迁移数据库，加载时自动兼容为默认首页。

## 2. 代码现状（Research Findings）
### 2.1 相关入口与链路
- `forge-report-ui/src/router/modules/chart.route.ts`：`/chart/home/:id(.*)*` 进入 `views/chart/index.vue` 工作空间。
- `forge-report-ui/src/router/modules/preview.route.ts`：`/chart/preview/:id(.*)*` 进入预览包装页。
- `forge-admin-ui/src/views/report/design.vue`：后台通过 SSO iframe 嵌入报表子系统，实际多画布能力落在 `forge-report-ui`。
- `forge-report-ui/src/views/chart/ContentEdit/hooks/useLayout.hook.ts`：编辑器加载项目详情，解析 `componentData` 后调用 `useSync().updateComponent()` 回填当前画布。
- `forge-report-ui/src/views/preview/utils/storage.ts`：预览页按路由项目 ID 从 sessionStorage/localStorage/后端读取 `componentData` 并回填 store。

### 2.2 现有实现
- `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.ts`：store 当前只包含一份 `editCanvasConfig`、`requestGlobalConfig`、`componentList`，`getStorageInfo()` 也只返回单画布结构。
- `forge-report-ui/src/api/project/index.ts`：`buildProjectPayload()` 将 `chartEditStore.getStorageInfo()` 序列化后写入 `ai_report_project.component_data`。
- `forge/forge-report-server/src/main/java/com/mdframe/forge/report/project/domain/ReportProject.java`：后端实体只有一个 `componentData` longtext 字段，可承载升级后的 JSON。
- `forge-report-ui/src/views/preview/components/PreviewRenderList/index.vue`：预览页渲染当前 store 的 `componentList`。
- `forge-report-ui/src/hooks/useLifeHandler.hook.ts`：运行时已支持组件基础事件和生命周期事件。
- `forge-report-ui/src/enums/eventEnum.ts`：基础事件包含 `click`、`dblclick`、`mouseenter`、`mouseleave`。
- `forge-report-ui/src/settings/animations/index.ts`：项目已内置 animate.css 动画配置，可复用到页面切换动画。

### 2.3 发现与风险
- 可以不改数据库表结构，先扩展 `componentData` JSON 协议并做旧数据兼容。
- 现有工具 `fetchRouteParamsLocation()` 通过 hash 最后一段解析项目 ID，预览指定页面应使用 query 参数 `pageId`，避免破坏 ID 解析。
- 现有组件事件支持用户编写 JS，但下钻跳转应优先提供结构化配置，避免要求用户写代码。
- 多画布切换会影响撤销/重做、复制粘贴、自动保存、预览、发布截图、JSON 导入导出，需要统一处理当前页面 flush 与 restore。

## 3. 功能点
- [ ] 项目 JSON 协议升级为多页面结构，兼容旧单页面 JSON。
- [ ] 编辑器内新增页面管理 UI，支持新增、复制、删除、重命名、排序、设置首页、切换页面。
- [ ] 页面切换时保存当前页面状态，并恢复目标页面状态。
- [ ] 自动保存、手动预览、发布保存多页面完整项目数据。
- [ ] 预览页支持默认首页和 `?pageId=` 指定页面。
- [ ] 组件事件配置新增「页面跳转」动作，支持点击/双击触发。
- [ ] 页面跳转支持携带固定值、组件数据字段、当前页面上下文参数。
- [ ] 目标页面可读取下钻上下文，用于动态请求参数注入。
- [ ] 页面切换支持基础过渡动画：无、淡入、左滑、右滑、缩放。

## 4. 业务规则
- 一个项目至少保留一个页面，最后一个页面不能删除。
- 每个项目必须有一个首页 `homePageId`，发布和预览默认打开首页。
- 页面 ID 使用前端生成 UUID，页面名称同项目内允许重复但建议 UI 提示。
- 切换页面前必须先把当前 `editCanvasConfig`、`requestGlobalConfig`、`componentList` 写回当前页面。
- 删除页面时，如存在组件动作跳转到该页面，需要提示用户确认并清理失效动作。
- 跳转动作仅允许跳转项目内页面，不允许填写外部 URL。
- 下钻参数只保存结构化配置，不执行任意字符串脚本。
- 旧项目没有 `pages` 字段时，自动包装成一个默认页面，不改变旧 JSON 读取能力。

## 5. 数据变更
| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 无 | `ai_report_project` | - | 继续使用 `component_data` longtext 保存多页面 JSON |

### 5.1 新 JSON 协议
```json
{
  "version": 2,
  "homePageId": "page-home",
  "activePageId": "page-home",
  "pageTransition": "fade",
  "pages": [
    {
      "id": "page-home",
      "name": "首页",
      "sort": 1,
      "editCanvasConfig": {},
      "requestGlobalConfig": {},
      "componentList": []
    }
  ],
  "sharedRequestGlobalConfig": {}
}
```

### 5.2 兼容旧 JSON
旧结构：
```json
{
  "editCanvasConfig": {},
  "requestGlobalConfig": {},
  "componentList": []
}
```

加载时转换为：
```json
{
  "version": 2,
  "homePageId": "default-page",
  "activePageId": "default-page",
  "pages": [
    {
      "id": "default-page",
      "name": "首页",
      "sort": 1,
      "editCanvasConfig": {},
      "requestGlobalConfig": {},
      "componentList": []
    }
  ]
}
```

## 6. 接口变更
| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 复用 | `/forge-report-api/report/project/{id}` | GET | 返回原 `componentData`，前端解析多页面 JSON |
| 复用 | `/forge-report-api/report/project` | PUT | 保存升级后的完整 `componentData` |
| 复用 | `/forge-report-api/report/project/publish/{id}` | POST | 发布 URL 仍为项目预览地址 |

## 7. 影响范围
- `forge-report-ui` 编辑器 store、页面加载、自动保存、预览、发布、截图。
- `forge-report-ui` 组件事件配置面板和预览运行时事件执行。
- `forge-report-ui` 动态请求参数解析，新增页面上下文来源。
- `forge-report-ui` JSON 导入导出和代码编辑页。
- `forge-report-server` 理论上不改表结构和接口，只需回归保存/读取加解密链路。

## 8. 风险与关注点
- 不涉及资金、权限变更、状态流转。
- 多页面 JSON 体积会明显变大，需确认 `component_data longtext` 足够承载。
- 页面切换时如果没有统一 flush 当前页面，容易出现“切页后编辑丢失”。
- 预览页页面跳转会触发组件卸载重建，数据池和定时请求必须清理后重新初始化。
- 旧项目兼容逻辑必须集中在工具函数，避免各入口各自判断导致行为不一致。
- 用户事件现有 JS 机制仍存在，应与结构化跳转动作并存，避免破坏已有项目。

## 8.5 测试策略
- **测试范围**：多页面协议单元测试、前端生产构建、编辑器手动流程、预览发布流程。
- **覆盖率目标**：新增协议转换、页面切换、跳转动作解析工具需要覆盖核心分支。
- **独立 Test Spec**：否，任务中包含回归清单。

## 9. 待澄清
- [ ] 第一版页面管理 UI 放在左侧图层区上方、底部栏，还是新增独立「页面」面板？
- [ ] 下钻参数第一版是否只支持固定值和组件字段，还是同时支持登录人上下文、页面上下文？
- [ ] 页面切换动画是否做项目级默认值，还是每个跳转动作单独配置？
- [ ] 发布截图使用首页截图，还是允许选择封面页面？

## 10. 技术决策
- 首期不新增表，优先升级 `componentData` JSON 协议，降低后端和迁移风险。
- 预览页通过 `?pageId=` 指定页面，保留 `/chart/preview/:id` 项目入口。
- 事件跳转使用结构化 `actions` 配置，不要求用户编写 JS。
- 页面上下文保存在运行时 store 中，并接入现有 `dynamicRequestParams` 机制。
- Figma 式连线原型编辑不纳入第一版，后续可基于结构化跳转动作继续扩展。

## 11. 执行日志
| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Task 1 | done | `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.d.ts`, `forge-report-ui/src/packages/index.d.ts`, `forge-report-ui/src/packages/public/publicConfig.ts`, `forge-report-ui/src/views/chart/hooks/useSync.hook.ts`, `forge-report-ui/src/utils/reportPages.ts` | 多页面协议、结构化动作类型、旧组件 actions 兼容、协议工具；`pnpm build` 通过 |
| Task 2 | done | `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.ts`, `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.d.ts`, `forge-report-ui/src/views/chart/ContentEdit/hooks/useLayout.hook.ts`, `forge-report-ui/src/views/preview/utils/storage.ts` | Store 增加页面列表、首页、当前页、页面上下文与切换动作；编辑/预览加载接入多页面协议；`pnpm build` 通过 |
| Task 3 | done | `forge-report-ui/src/views/chart/ContentPages/index.vue`, `forge-report-ui/src/views/chart/ContentPages/components/PageListItem.vue`, `forge-report-ui/src/views/chart/index.vue`, `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.ts` | 编辑器左侧新增画布页面管理面板，支持新增、复制、删除、重命名、设置首页、上移/下移、切换页面；`pnpm build` 通过 |
| Task 4 | done | `forge-report-ui/src/api/project/index.ts`, `forge-report-ui/src/utils/reportPages.ts`, `forge-report-ui/src/views/chart/hooks/useAutoSave.hook.ts`, `forge-report-ui/src/views/chart/ContentHeader/headerRightBtn/index.vue`, `forge-report-ui/src/views/project/components/ProjectCreateModal/index.vue`, `forge-report-ui/src/views/project/layout/components/ProjectLayoutCreate/components/CreateModal/index.vue` | 保存、自动保存、预览、发布链路改为项目级多页面 JSON；新建项目默认写入 v2 协议；`pnpm build` 通过 |
| Task 5 | done | `forge-report-ui/src/views/preview/suspenseIndex.vue`, `forge-report-ui/src/views/preview/utils/storage.ts`, `forge-report-ui/src/hooks/useChartDataPondFetch.hook.ts`, `forge-report-ui/src/utils/reportPages.ts` | 预览默认解析首页，支持 `?pageId=`，提供运行态切页方法和页面转场；切页重建渲染列表并清理旧数据池 watcher/轮询；`pnpm build` 通过 |
| Task 6 | done | `forge-report-ui/src/enums/eventEnum.ts`, `forge-report-ui/src/hooks/useLifeHandler.hook.ts`, `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.ts`, `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.d.ts`, `forge-report-ui/src/views/chart/ContentConfigurations/components/ChartEvent/index.vue`, `forge-report-ui/src/views/chart/ContentConfigurations/components/ChartEvent/components/ChartEventPageAction/index.vue`, `forge-report-ui/src/views/chart/ContentConfigurations/components/ChartEvent/components/ChartEventPageAction/index.ts`, `forge-report-ui/src/views/preview/suspenseIndex.vue`, `forge-report-ui/src/views/preview/utils/storage.ts` | 事件配置新增页面跳转动作面板，支持点击/双击跳转项目内页面并可选转场；运行时合并旧基础事件 JS 与结构化动作；`pnpm build` 通过 |
| Task 7 | pending | - | 下钻上下文参数 |
| Task 8 | pending | - | 动画与回归 |

## 12. 审查结论
待实现后审查。

## 13. 确认记录（HARD-GATE）
- **确认时间**：2026-05-15
- **确认人**：需求方确认多画布与大屏下钻方向合理，要求开始拆任务推进。
