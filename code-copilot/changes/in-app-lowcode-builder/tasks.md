# 任务拆分 — 应用内低代码搭建器（前端交互升级）
> status: apply
> change: `in-app-lowcode-builder`
> dependency: `app-first-lowcode-workbench`
> 原则：前端优先；复用既有页面/表单/列表/规则/流程设计资产；不改后端、不改 Flyway、不改全局 `sys_resource`；一个任务一个可独立提交的原子变更。

## 前置条件

- [x] 用户确认 `spec.md` 第 9 章的路由、组件范围、外部页面边界和发布快照结论。
- [x] 核查 `GET .../workspace` 返回 `application.options`，且 `PUT /ai/business/application` 能无损保存未知的 `options.inAppBuilder` 键。
- [x] 核查既有应用版本快照包含 `options`；若不包含，先更新 Spec 范围，不进入实现。
- [x] 读取 `code-copilot/rules/automated-testing-standard.md`，并创建本变更的 `test-spec.md`、`execution-log.md` 后再执行任何编码任务。
- [ ] 保持 `app-first-lowcode-workbench` 的既有未提交/用户变更不被覆盖；本变更只叠加新的前端文件和明确关联的入口文件。

## 阶段总览

| 阶段 | 目标 | Task |
|---|---|---|
| Phase 0 | 编排模型与兼容基线 | 1-2 |
| Phase 1 | 应用运行壳与入口 | 3-4 |
| Phase 2 | 页面组、页面与空态搭建 | 5-6 |
| Phase 3 | 组件插入、属性与业务深链 | 7-9 |
| Phase 4 | 草稿、预览、权限和验证 | 10-12 |

## Task 1: 应用内编排 Schema 与纯函数基线

> status: completed

- **目标**：建立 `options.inAppBuilder` 的前端 Schema、默认首页、旧配置归一化和节点 ID/排序工具，作为所有应用内导航和页面操作的唯一前端模型。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/in-app-builder/in-app-builder-schema.js` — 新增编排常量、默认首页、normalize、create/move/remove node、组件插入位置和引用校验纯函数。
  - `forge-admin-ui/src/views/app-center/in-app-builder/__tests__/in-app-builder-schema.spec.js` — 新增默认首页、目录移动、删除处理、失效对象引用和插入位置单测。
  - `forge-admin-ui/src/components/lowcode-builder/page/page-schema.js` — 仅在需要公开现有默认组件工厂时补充无副作用导出；不得改变旧 Schema 输出。
- **关键签名**：
  ```js
  export function normalizeInAppBuilder(raw, application, objects) {}
  export function createNavigationNode(schema, input) {}
  export function moveNavigationNode(schema, nodeId, targetParentId, targetIndex) {}
  export function removeNavigationNode(schema, nodeId, strategy) {}
  export function insertPageComponent(schema, pageId, component, target) {}
  ```
- **验收标准**：
  - 空/旧 options 可得到一个稳定的顶层首页，且不修改原对象。
  - 首页不能移动到页面组；删除页面组必须显式指定子节点策略。
  - 不复制业务对象的完整 Schema；对象页只保存引用。
  - 纯函数单测覆盖率达到 Spec 目标。

## Task 2: 应用 options 读写适配与草稿脏状态

> status: completed

- **目标**：在不改变后端协议的前提下，从应用工作台数据读取/合并 `options.inAppBuilder`，并按既有应用更新接口保存草稿。
- **涉及文件**：
  - `forge-admin-ui/src/api/business-application.js` — 复用现有应用读取/更新 API，新增纯前端 options 解析/合并辅助函数（如需要）。
  - `forge-admin-ui/src/views/app-center/in-app-builder/useInAppBuilderDraft.js` — 新增加载、脏状态、保存、丢弃、冲突提示和 options 合并 composable。
  - `forge-admin-ui/src/views/app-center/in-app-builder/__tests__/useInAppBuilderDraft.spec.js` — 新增未知 options 保留、首次保存、保存失败和丢弃草稿用例。
  - `forge-admin-ui/src/views/app-center/application.[applicationCode].vue` — 仅增加进入搭建器的上下文参数/返回路径，不重复加载应用聚合。
- **关键签名**：
  ```js
  export function mergeInAppBuilderOptions(applicationOptions, builderSchema) {}
  export function useInAppBuilderDraft(applicationCode) {}
  ```
- **验收标准**：
  - 保存不会覆盖 options 中已有的发布、入口或扩展配置。
  - 保存失败不会让界面误显示为已保存。
  - 退出存在未保存更改时给出确认。
  - 如接口契约不满足，停止并回填 Spec，不以 LocalStorage 作为共享持久化替代。

## Task 3: 应用运行壳与前端路由

> status: in_progress

- **目标**：为应用增加前端运行壳路由，使用应用级导航而非系统全局侧栏，并复用现有页面/入口运行能力。
- **涉及文件**：
  - `forge-admin-ui/src/router/index.js` — 新增应用运行壳和编辑态路由，保留现有工作台/预览路由。
  - `forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue` — 新增应用运行壳，加载已发布/可用应用编排并负责页面切换。
  - `forge-admin-ui/src/views/app-center/in-app-builder/ApplicationRuntimeShell.vue` — 新增统一顶栏、应用导航区和内容区壳层。
  - `forge-admin-ui/src/views/app-center/in-app-builder/ApplicationRuntimePageRenderer.vue` — 新增首页、内容页、对象页和既有入口页的受控渲染/占位逻辑。
- **关键行为**：
  - 顶栏提供应用切换/返回应用中心；只有具备既有应用编辑权限的用户显示“编辑应用”。
  - 应用左侧只显示应用页面树，不显示用户管理、角色管理等系统菜单。
  - 未发布或失效对象/入口显示业务化占位和“去配置”动作。
- **验收标准**：
  - 打开应用运行壳时可选中首页和应用内页面。
  - 已有 `/ai/crud-page/:configKey`、对象设计器和工作台路由不受影响。
  - 外部/嵌入入口只能经既有打开信息/安全路径渲染。

## Task 4: 工作台到应用的双入口与最小权限呈现

- **目标**：让用户在应用工作台和运行壳之间清晰切换，避免出现“两个低代码入口”的认知割裂。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/application-workspace/ApplicationWorkspaceHeader.vue` — 增加“进入应用/编辑应用”操作与状态文案。
  - `forge-admin-ui/src/views/app-center/application.[applicationCode].vue` — 编排工作台、运行壳、编辑态之间的跳转和返回路径。
  - `forge-admin-ui/src/views/app-center/in-app-builder/ApplicationRuntimeShell.vue` — 显示编辑入口、返回工作台入口和权限不足提示。
  - `forge-admin-ui/src/composables/usePermission.js` 或既有权限工具 — 复用当前应用编辑权限判断；仅在确有缺口时修改。
- **验收标准**：
  - 普通使用者看到“打开应用”，不看到“编辑应用”。
  - 编辑者能从运行壳进入编辑态，退出后回到原页面。
  - UI 文案统一为“应用搭建/高级配置”，不展示“低代码 A/低代码 B”。

## Task 5: 页面组、页面创建与树操作

> status: completed-static

- **目标**：提供不依赖拖拽的页面组/页面创建、移动、排序和删除交互。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/in-app-builder/ApplicationNavigationTree.vue` — 新增树渲染、展开、空目录动作和节点更多菜单。
  - `forge-admin-ui/src/views/app-center/in-app-builder/PageNodeCreateModal.vue` — 新增页面组/页面创建表单、所属目录选择和模板选择。
  - `forge-admin-ui/src/views/app-center/in-app-builder/MoveNavigationNodeModal.vue` — 新增“移动到”目录选择交互。
  - `forge-admin-ui/src/views/app-center/in-app-builder/InAppBuilder.vue` — 连接树操作与草稿状态。
  - `forge-admin-ui/src/views/app-center/in-app-builder/__tests__/ApplicationNavigationTree.spec.js` — 覆盖目录空态、创建默认父级、移动和删除确认。
- **关键行为**：
  - 目录空态文案为“在本组创建页面/移动已有页面到此组”。
  - 右键/更多菜单支持重命名、移动到、上移、下移、删除；拖拽排序只允许作为可选增强。
  - 首页初始化且固定顶层。
- **验收标准**：
  - 用户无需拖拽就能完成“新建销售管理目录 → 在目录下创建商机管理页面”。
  - 删除有子节点目录时必须选择处理策略，不能静默丢失页面。
- **实际实现**：页面树的更多菜单复用 `createNavigationNode`、`moveNavigationNode`、`removeNavigationNode`；支持在页面组内新建页面/页面组、重命名、移动到、上移、下移和删除。删除包含子项的页面组时，弹窗要求选择“同时删除子项”或“移动到指定页面组/顶级菜单”。首页不显示删除操作。

## Task 6: 空白页、介绍页与页面模板引导

> status: in_progress

- **目标**：让新页面先显示可理解的介绍和推荐操作，而非直接暴露复杂画布。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/in-app-builder/ApplicationPageCanvas.vue` — 新增中间页面画布容器与编辑选择态。
  - `forge-admin-ui/src/views/app-center/in-app-builder/EmptyPageGuide.vue` — 新增空白/介绍页引导、常用组件推荐和页面模板卡片。
  - `forge-admin-ui/src/views/app-center/in-app-builder/page-template-catalog.js` — 新增首页、介绍、业务数据、空白内容和入口页的轻量模板定义。
  - `forge-admin-ui/src/views/app-center/in-app-builder/InAppBuilder.vue` — 连接创建页、模板落地与选中状态。
- **验收标准**：
  - 新建首页或空白页后显示简洁引导和不超过六个首屏推荐操作。
  - 选择模板后产生可编辑的页面配置，不生成另一份对象 Schema。
  - 不使用渐变背景、巨大 Hero、装饰性插画或多层卡片嵌套。
- **当前实现**：新建内容/介绍/入口页可选择空白、介绍、业务概览或数据工作台模板；模板只根据已注册区块目录创建 `gridLayout.items`，空白页仍显示不超过六个的组件推荐，不复制业务对象 Schema。

## Task 7: 悬浮组件插入与既有拖拽组件库复用

> status: completed-static

- **目标**：在复用现有组件目录/默认属性/拖拽能力的基础上，新增点击式组件选择和确定插入位置的交互。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/in-app-builder/ComponentInsertPopover.vue` — 新增悬浮 `+`、搜索、分类和点击插入弹窗。
  - `forge-admin-ui/src/views/app-center/in-app-builder/component-insert-catalog.js` — 从现有组件目录映射常用/业务/内容/高级分组，不复制组件定义。
  - `forge-admin-ui/src/views/app-center/in-app-builder/ApplicationPageCanvas.vue` — 提供页面/容器/组件后的插入锚点，并复用 `BuilderCanvas` 或已注册 renderer。
  - `forge-admin-ui/src/components/lowcode-builder/page/ComponentPalette.vue` — 仅抽取可复用目录筛选/拖拽数据逻辑或暴露必要 props，保持旧设计器行为。
  - `forge-admin-ui/src/views/app-center/in-app-builder/__tests__/ComponentInsertPopover.spec.js` — 覆盖搜索、分类、插入锚点优先级和空页插入。
- **验收标准**：
  - 点击推荐组件或弹窗中的组件，可立即插入当前页面。
  - 插入优先级符合 Spec 3.3，插入后自动选中组件。
  - 现有 `application/x-lowcode-component` 拖拽协议仍可在高级场景工作。
- **实际实现**：组件入口使用 Naive UI `NPopover`，固定在页面树右缘；组件目录直接读取 `listPageBlockCatalog`。中间区域使用 `GridBlockRenderer` 渲染为最终页面流，编辑态复用列表设计器的拖拽手柄、八向尺寸锚点、选中框、原位占位阴影与更多操作层；不渲染网格背景或完整自由画布工作台。
- **本轮收敛**：拖拽手柄改为页面内容区内的自由移动；拖动过程显示原位条纹双胞胎阴影，释放到另一组件上会交换位置；八向锚点持久化实际宽高和位置。更多菜单新增“配置”，打开右侧轻量面板用于标题、宽度、高度和删除；默认区块高度按类型收敛，避免首次插入过高。新建按钮统一灰底居中；添加组件按钮带图标，默认在中间页面左下角且可在页面内拖动。

## Task 8: 右侧上下文和组件属性编辑复用

> status: completed-static

- **目标**：以一个克制的右侧面板承载空态推荐、页面设置、业务页设置和现有组件属性编辑。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/in-app-builder/BuilderContextPanel.vue` — 新增状态切换、面板标题和统一操作区。
  - `forge-admin-ui/src/views/app-center/in-app-builder/PageSettingsPanel.vue` — 新增页面标题、图标、说明、目录与轻量可见性预留配置。
  - `forge-admin-ui/src/components/lowcode-builder/page/ComponentPropertyPanel.vue` — 以适配方式复用；仅补充必要的受控 props/emit，不改变现有页面设计器契约。
  - `forge-admin-ui/src/views/app-center/in-app-builder/InAppBuilder.vue` — 统一选中页面/组件、插入目标和草稿更新。
- **验收标准**：
  - 空页、页面、组件、业务数据页四种状态均只显示相关配置。
  - 组件属性修改通过既有 Schema 生效，旧低代码页面属性编辑无回归。
  - 右侧不同时堆叠导航、组件货架、属性和高级配置。
- **实际实现**：应用运行壳不嵌入列表设计器工作台，也不复制右侧属性面板；应用页只展示可直接发布的页面内容，复杂属性继续由既有专业设计器维护。

## Task 9: 业务数据页与高级配置回流

> status: in_progress

- **目标**：在应用搭建器中创建/配置 CRUD、表单、规则和流程页面，同时保持对象设计器为唯一事实来源。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/in-app-builder/BusinessPageSetupPanel.vue` — 新增对象选择、运行模式、`pageKey`、默认参数和快捷配置入口。
  - `forge-admin-ui/src/views/app-center/in-app-builder/ApplicationRuntimePageRenderer.vue` — 对象页引用既有 `AiCrudPage`/页面运行逻辑或受控深链。
  - `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue` — 接收并保留应用返回上下文，必要时支持打开指定 list/form/detail 分区。
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue` — 仅在需要时适配“从应用搭建器进入”的返回动作。
  - `forge-admin-ui/src/views/app-center/in-app-builder/__tests__/BusinessPageSetupPanel.spec.js` — 覆盖对象引用、失效 `pageKey`、深链参数与不复制 Schema。
- **验收标准**：
  - 可在应用内新建“客户管理”业务页，选择对象与列表/表单/详情页面。
  - 点击“配置列表/表单/规则/流程”进入当前已有设计器并可返回原应用页面。
  - 应用 options 不保存业务对象完整字段/表单/流程 JSON。
- **当前实现**：新建业务数据页时选择对象并只保存 `objectRef`；对象页提供“配置列表 / 配置表单 / 配置详情 / 配置流程”入口，均跳转既有对象设计器并携带 `objectId`、目标面板和 `returnTo`，不写入对象 Schema。

## Task 10: 预览、发布提示与版本一致性

> status: in_progress

- **目标**：清晰区分应用草稿与运行态，并复用已有发布/历史入口。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/in-app-builder/InAppBuilderHeader.vue` — 新增保存草稿、预览、发布、退出和脏状态提示。
  - `forge-admin-ui/src/views/app-center/in-app-builder/InAppBuilder.vue` — 编排草稿预览与发布跳转。
  - `forge-admin-ui/src/views/app-center/application-workspace/ApplicationPublishPanel.vue` — 增加应用内编排会随应用版本发布的可见提示（如既有快照确认支持）。
  - `forge-admin-ui/src/views/app-center/in-app-builder/__tests__/InAppBuilderHeader.spec.js` — 覆盖保存、退出确认、预览和发布跳转。
- **验收标准**：
  - 保存草稿不宣称已发布；预览显式标识草稿。
  - 发布入口复用既有面板，不新增第二套版本或回滚 API。
  - 若发布快照未覆盖 options，则本任务阻断并回填 Spec，不做误导性 UI。
- **当前实现**：编辑态提供保存草稿、草稿预览和进入既有发布面板；草稿预览会先保存，预览页显式标记“草稿预览”。退出存在未保存修改时必须确认，放弃修改会按已保存 `options` 重建前端编排模型。

## Task 11: 简洁视觉、可访问性与响应式收口

- **目标**：统一运行壳和编辑态的轻量企业风格，保证页面树、画布、右侧面板在常用桌面宽度下可用。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/in-app-builder/in-app-builder.css` — 新增局部样式 token、三栏布局、紧凑状态和响应式降级。
  - `forge-admin-ui/src/views/app-center/in-app-builder/ApplicationRuntimeShell.vue` — 应用主题根节点、键盘/焦点与窄屏菜单折叠。
  - `forge-admin-ui/src/views/app-center/in-app-builder/InAppBuilder.vue` — 三栏尺寸、空态和 loading/error 状态。
  - `forge-admin-ui/src/views/app-center/in-app-builder/ApplicationNavigationTree.vue` — aria 标签、可见焦点和键盘可操作的更多菜单。
- **验收标准**：
  - 不引入渐变、重阴影、无意义大标题或嵌套卡片。
  - 1440px 桌面下页面树、画布、右栏可同时操作；窄屏可折叠两侧区域。
  - 所有图标按钮有 title/aria-label，空态、加载态和错误态可理解。

## Task 12: 增量验证、文档回填与 HARD-GATE 后收尾

- **目标**：按项目自动化测试规范验证前端交互、已有设计器回归和应用 options 持久化，并更新变更记录。
- **涉及文件**：
  - `code-copilot/changes/in-app-lowcode-builder/test-spec.md` — 新增测试矩阵、已有基线和浏览器验收脚本。
  - `code-copilot/changes/in-app-lowcode-builder/execution-log.md` — 追加每次命令、结果、警告、服务清理和人工验收。
  - `code-copilot/changes/in-app-lowcode-builder/spec.md` — 回填已决待澄清项、实际接口契约和审查结论。
  - `code-copilot/changes/in-app-lowcode-builder/tasks.md` — 更新任务状态和实际文件。
- **验证命令**：以 `automated-testing-standard.md` 和实际 package scripts 为准，至少包括新增 Vitest 定向用例、相关 Vue/JS ESLint 与 `pnpm --dir forge-admin-ui build`；如启动服务和浏览器验收，必须记录启动/停止状态。
- **验收标准**：
  - 所有自动化命令、跳过原因和手工验收结果可追溯。
  - 不因本变更引入后端、Flyway 或全局菜单改动。
  - 完成 Spec 合规和代码质量审查后才允许归档。

## HARD-GATE

用户已于 2026-07-21 明确确认开始实施。按 Task 1 顺序执行；若第 1～2 Task 发现 options 保存或版本快照契约不成立，立即暂停并将所需后端工作拆为新提案。

## Task 13: 发布菜单与页面权限同步

> status: in_progress

- **目标**：发布时将显式暴露的应用页面/页面组同步到 `sys_resource`，并按页面访问范围同步角色资源授权；草稿保存不修改系统菜单。
- **涉及文件**：`BusinessApplicationPublishService`、新增页面运行/菜单同步服务、`MenuRegisterAdapter` 及 Admin 实现、发布步骤、应用发布快照、必要 Flyway/Mapper/DTO/VO。
- **验收标准**：菜单与角色关系可重复同步；页面删除/隐藏只停用；回滚会恢复页面菜单；发布失败不得留下半更新菜单。

## Task 14: 已发布运行配置、入口与发布校验

> status: pending

- **目标**：运行页只读取已发布快照并根据当前权限过滤页面；发布检查覆盖首页、对象引用、菜单权限、变量动作目标。

## Task 15: 页面设置、版本差异与回滚呈现

> status: pending

- **目标**：在页面设置中配置入口/可见性/角色；发布面板展示结构化检查和版本差异；回滚提示会同步系统菜单。

## Task 16: 容器子组件与跨页移动

> status: pending

- **目标**：完整支持容器内选择、排序、移出，以及有事务语义的跨页面移动。

## Task 17: 页面变量、动作与响应式预览

> status: pending

- **目标**：实现白名单变量/动作协议及三断点预览，不开放任意脚本。

## Task 18: 第二阶段增量测试与回填

> status: pending

- **目标**：补后端单测、前端定向用例、静态检查、构建和发布链路验收记录。
