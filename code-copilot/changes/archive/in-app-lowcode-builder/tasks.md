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

> status: completed

- **目标**：运行页只读取已发布快照并根据当前权限过滤页面；发布检查覆盖首页、对象引用、菜单权限、变量动作目标。
- **本轮细化**：
  - 新增页面数据依赖检查器，替换无条件“必须且只能一个主对象”的发布门禁。
  - 单对象关联自动规范为 `PRIMARY`；多对象显式绑定允许无主对象。
  - 新增已发布运行配置服务和 `/by-code/:applicationCode/runtime` 接口；`edit=1`/`draft=1` 继续读取草稿。
  - 补齐 `PAGE_MENUS` 发布步骤名称及部分失败副作用语义。
  - 正式运行过滤保留隐藏但有权直达的页面，无权/失效 `pageId` 真正回退到可访问首页；同应用系统菜单切换 `pageId` 会即时响应，超级管理员不受页面角色筛选限制。

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

> status: in_progress

- **目标**：补后端单测、前端定向用例、静态检查、构建和发布链路验收记录。
- **当前结果**：生成器 reactor 测试编译、26 个定向 JUnit、前端 ESLint、9 个 Schema Vitest、生产构建和 `git diff --check` 已通过；真实角色、发布菜单和数据库 E2E 按用户分工保留人工验收。

## Task 19: 统一新建应用与设计器内对象起步

> status: completed

- **目标**：应用中心只保留一个普通新建入口，创建后直达页面设计；数据模板缺少对象时在设计器内创建、绑定或导入对象，不再把用户送回工作台自行理解主对象。
- **结果**：设计器复用对象面板抽屉；打开对象配置前先保存脏草稿，对象变化只刷新元数据、不重载画布，并在页面模板起步场景自动回到对象已就绪状态。

## Task 20: 设计器内发布抽屉

> status: completed

- **目标**：复用 `ApplicationPublishPanel` 在页面设计器内完成检查、阻断项定位、发布和历史查看；工作台发布页保留为高级配置兼容入口。
- **结果**：发布前自动保存草稿，抽屉打开后自动检查；页面阻断定位到页面，对象阻断打开数据对象抽屉，其余高级问题才进入工作台。

## Task 21: 页面表单升级为 CRUD 数据对象

> status: completed

- **目标**：修复页面表单字段在列表字段抽屉中无法选中的问题，并让“先设计表单、再创建数据页”的用户在当前 `AiCrudPage` 内完成业务对象创建与绑定。
- **涉及文件**：
  - `forge-admin-ui/src/components/lowcode-builder/page/page-schema.js` — 提供页面字段覆盖稳定模型字段的同步模型构造器。
  - `forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue` — 布局回写使用当前 `fields` 清洗字段引用。
  - `forge-admin-ui/src/views/app-center/in-app-builder/page-form-object-promotion.js` — 将页面表单转换为业务对象字段/表单设计保存载荷。
  - `forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue` — `AiCrudPage` 无对象引导、已有对象直绑、新对象自动回绑及设计草稿字段兜底。
  - `forge-admin-ui/src/views/app-center/application-workspace/ApplicationObjectsPanel.vue` — 页面表单升级入口和创建后关联事件。
  - `forge-admin-ui/src/views/app-center/components/BusinessObjectWizardDrawer.vue` — 支持由页面表单预填对象名称与说明。
  - 对应 Vitest、Spec、测试记录与长期决策/踩坑记录。
- **实施顺序**：
  1. 先新增纯函数回归测试，复现空 `modelSchema.fields` 清空当前字段目录以及页面表单转换载荷。
  2. 修复列表设计器字段同步契约。
  3. 实现页面表单升级、对象自动关联和当前 CRUD 自动绑定。
  4. 增加未绑定对象的就地说明与操作入口，并验证新建对象设计草稿字段回显。
  5. 执行增量 ESLint、Vitest、生产构建和 `git diff --check`，回填结果。
- **验收标准**：页面表单字段可选择、隐藏、排序且不会回写后消失；未绑定 CRUD 不再把页面表单描述为真实数据源；从页面表单创建对象后当前区块获得显式 `objectRef`；不自动执行 DDL 或发布。
- **结果**：列表设计器使用当前 `fields` 构造字段清洗模型，页面表单字段回写不再被空稳定模型删除；未绑定 CRUD 显示业务数据源引导和字段草稿说明；可选择已有对象，或将当前页面表单转换为业务对象字段/表单设计草稿，创建后自动加入应用并回绑当前区块；对象未发布时编辑态可读取设计草稿字段。未执行数据库同步和对象发布。

## Task 22: 保存表单时自动准备数据存储

> status: completed

- **目标**：用“保存表单”替代手工“从当前表单创建对象”，隐藏普通用户不需要理解的业务对象、数据源和对象角色，并消除连续两个抽屉。
- **涉及文件**：
  - `forge-server/.../dto/businessapp/BusinessApplicationFormDataProvisionDTO.java`、`vo/businessapp/BusinessApplicationFormDataVO.java` — 自动准备请求与稳定对象引用响应。
  - `forge-server/.../service/businessapp/BusinessApplicationFormDataService.java` — 原子选择默认可写 `LOWCODE_RUNTIME` 数据源、创建/复用托管对象、同步设计并关联应用。
  - `forge-server/.../controller/BusinessApplicationController.java` — 提供应用编辑权限下的表单数据准备接口。
  - `forge-admin-ui/src/api/business-application.js` — 封装表单数据准备请求。
  - `forge-admin-ui/src/views/app-center/in-app-builder/page-form-data-provisioning.js` — 识别需要准备数据的表单、构造请求并为所有未绑定 CRUD 写回对象引用。
  - `forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue` — 保存后自动准备、合并反馈、就地失败重试和高级设置入口。
  - `ApplicationObjectsPanel.vue` 与 `BusinessObjectWizardDrawer.vue` — 移除页面表单手工升级的主路径预填协议，保留高级对象管理。
  - 对应 JUnit、Vitest、Spec、测试记录与长期决策/踩坑记录。
- **实施顺序**：
  1. 新增失败用例，覆盖默认可写数据源、重复保存复用、空字段拒绝、单表单多 CRUD 只创建一次以及嵌套区块绑定。
  2. 实现后端原子聚合接口，不执行 DDL 或发布。
  3. 接入应用草稿保存链路，并将技术术语和手工升级入口移入高级设置。
  4. 执行增量 JUnit、ESLint、Vitest、生产构建和 `git diff --check`，回填测试记录。
- **验收标准**：用户只需保存表单即可让关联数据列表获得可持久化数据模型；重复保存不产生重复对象；没有可写运行数据源时表单草稿仍保存并提供单层重试提示；主路径不再打开对象向导；不自动执行 DDL 或发布。
- **结果**：数据页面模板现直接创建页面并进入表单设计；表单保存后由应用聚合接口选择默认可写运行数据源，创建或复用按应用和表单稳定标识管理的内部对象，同步字段/表单 Schema、关联应用并回绑所有对应未绑定 CRUD。页面级或区块级手工数据绑定不会被覆盖；失败时保留已保存表单并原位重试。普通界面使用“表单数据 / 数据存储”，对象、数据源和角色只在高级设置出现；未执行 DDL、对象发布或应用发布。

## Task 23: 修复自动回绑后的未发布提示

> status: completed

- **目标**：保存表单自动准备数据存储后，应用设计器继续使用对象草稿配置，不再因 `AiCrudPage` 自动加载正式运行接口而提示“低代码应用尚未发布”。
- **实施范围**：应用设计/草稿模式调用 CRUD 渲染接口时启用 `designPreview`，并为回绑对象的所有 CRUD 端点统一追加设计预览参数；正式运行模式保持发布门禁不变。
- **验收标准**：设计态渲染配置和列表请求均带设计预览标记；详情、新增、修改、删除等标准端点不丢标记且不重复追加；正式运行参数不变；补充定向 Vitest、ESLint、构建与差异检查记录。
- **结果**：应用编辑/草稿模式复用既有 `designPreview` 协议读取对象草稿渲染配置，所有 CRUD 端点统一追加 `designPreview=1`，正式运行端点保持不变。设计画布默认使用静态结构预览并阻止提交，不再在托管对象尚未发布或数据库尚未同步时自动加载列表；显式开启真实数据预览后才请求带设计授权标记的草稿接口。ESLint、9 个定向 Vitest、生产构建和 `git diff --check` 均通过。

## Task 24: 修复模型编码溢出并自动生成应用编码

> status: completed

- **目标**：修复保存表单时 `model_code` 超过 48 位导致的数据存储创建失败，并让普通用户新建应用时不再手工维护技术编码。
- **涉及文件**：
  - `BusinessNamingService.java` 与对应 JUnit — 统一模型编码最大长度为 48，覆盖长业务域/对象组合和前缀去重。
  - `BusinessApplicationFormDataServiceTest.java` — 覆盖长应用编码自动创建托管对象时的模型编码边界。
  - `BusinessApplicationService.java`、创建响应 VO、Controller 与 Service 测试 — 后端生成租户内唯一应用编码并返回最终 ID/编码。
  - `ApplicationEditorDrawer.vue` — 普通路径隐藏应用编码，高级设置可选填写，创建后使用服务端最终编码跳转。
  - `namingUtils.js` 与 Vitest — 前端手工对象创建同样限制模型编码为 48 位。
  - 当前变更 Spec、测试记录与长期决策/踩坑记录。
- **实施顺序**：
  1. 先补模型编码 48 位、长表单托管对象和应用编码自动生成/重名避让测试。
  2. 修复后端模型命名和应用创建响应契约。
  3. 收起前端应用编码字段并接入最终创建结果，同时对齐前端模型编码边界。
  4. 执行后端 `test-compile`、定向 JUnit、前端 ESLint/Vitest/生产构建和差异检查，回填结果。
- **验收标准**：长应用/表单名称可成功准备数据存储且 `modelCode.length <= 48`；应用编码留空可创建并在租户内自动避重；显式非法或重复编码仍失败；创建后路由使用后端最终编码；不修改数据库结构、不启动真实服务或执行 DDL。
- **结果**：后端模型编码归一化和组合统一限制为 48 位，长表单托管对象回归通过；应用编码为空时按业务域/名称生成，未知中文使用稳定摘要，租户重名自动追加序号。创建接口返回 ID 与最终编码，前端普通路径收起编码字段、创建后按服务端结果跳转；手工对象创建的前端命名工具同步 48 位边界。未修改数据库结构或执行 DDL。

## Task 25: 自动托管表单创建并同步数据表

> status: completed

- **目标**：让页面表单保存后的“数据存储已准备完成”具备真实物理表，消除自动回绑后访问 `AiCrudPage` 报“数据表不存在”的半完成状态。
- **涉及文件**：
  - `BusinessApplicationFormDataService.java` — 使用显式事务模板先提交托管对象/字段/表单元数据，再触发数据库同步；筛选允许自动建表的数据源并转换用户友好错误。
  - `BusinessObjectTableMappingService.java` — 增加仅接受 `PAGE_FORM` 托管对象的内部自动同步入口，复用差异预览、安全 DDL 白名单和同步结果记录，不放宽手工同步门禁。
  - `BusinessApplicationFormDataServiceTest.java` — 覆盖首次建表、重复保存同步新增字段、元数据提交先于 DDL、DDL 失败后可复用重试、无自动建表数据源失败关闭。
  - `BusinessObjectDatabaseSyncServiceTest.java` — 覆盖托管对象自动同步无需手工确认、非托管对象拒绝、禁用 DDL/非追加变更拒绝以及安全 CREATE/ADD 执行。
  - 当前 Spec、测试计划、执行日志和长期记忆。
- **实施顺序**：
  1. 先补失败测试，断言当前 `provision()` 没有同步数据表，并覆盖事务提交发生在 DDL 调用之前。
  2. 为表映射服务补托管对象专用同步入口；入口内部再次校验 `managedBy=PAGE_FORM`，只执行现有安全追加式 DDL。
  3. 将表单元数据准备放入显式事务模板，事务返回后再同步数据库；同步失败转换为“表单草稿已保存，但数据表创建失败”的可重试错误。
  4. 数据源选择增加 `allowRuntimeDdl=1`，普通业务对象和手工数据库同步协议保持不变。
  5. 先执行生成器 `test-compile`，再执行定向 Surefire；最后执行 `git diff --check` 并回填结果。
- **验收标准**：首次保存自动托管表单会执行一次安全建表；再次保存新增字段会同步缺失列；同一表单不重复创建对象；DDL 失败时元数据事务已经提交且下次保存可重试；非托管对象不能调用自动同步；未启用自动建表的数据源得到普通用户可理解的提示；不启动真实数据库或实际执行 DDL。
- **结果**：表单元数据在显式事务中先提交，随后仅对来源一致的 `PAGE_FORM` 托管对象执行安全建表/追加字段；DDL 失败保留表单设计并允许原位重试。新对象只选择允许自动建表的数据源，历史对象则以数据源当前真实能力和 DDL 预检为准，开启开关后可直接重新保存补建表。生成器 29 模块 `test-compile` 成功，定向 21 个 JUnit 用例全部通过；未启动真实数据库或执行 DDL。

## Task 26: 收敛真实预览、查询条件与发布数据库状态

> status: completed

- **目标**：自动托管表单绑定后可以直接使用真实草稿 CRUD；查询条件刷新后稳定保留；发布检查自动消化安全数据库差异，不再用陈旧 `OUT_OF_SYNC` 误阻断。
- **涉及文件**：
  - `page-form-data-provisioning.js` 及 Vitest — 自动绑定时一次性启用真实预览，复用既有托管对象时补齐旧区块且不覆盖手工对象/用户后续选择。
  - `application-runtime.[applicationCode].vue` — 合并表单字段与异步运行字段，避免只返回 ID 的瞬态目录清理查询配置。
  - `GridBlockRenderer.vue` 与可测试纯函数 — 查询 Schema 使用 `searchFieldRefs`，静态预览提示区分“未绑定接口”和“主动关闭真实预览”。
  - `BusinessApplicationFormDataService.java` — 提供应用级托管表单安全数据库重同步入口。
  - `BusinessApplicationPublishService.java`、Controller 与 JUnit — 发布检查和最终发布前调用托管数据表重同步，手工对象门禁保持不变。
- **实施顺序**：先补字段合并、查询引用、旧托管绑定和发布前同步红灯用例；再实现最小修复；最后按 JDK 17、Node `v20.19.0` 执行定向测试、后端测试编译、前端 ESLint/构建和差异检查。
- **验收标准**：已选查询字段不因异步目录切换丢失；查询区不再错误跟随列表列；自动托管绑定后无需再理解接口开关即可查询/新增；用户主动关闭预览后保持关闭；发布前安全差异自动同步，高风险差异继续清晰阻断；不启动真实数据库或实际执行 DDL。
- **结果**：页面字段目录现在稳定合并表单字段和运行字段，查询条件独立使用 `searchFieldRefs`，并覆盖显式空配置、旧页面回退及遗留初始化标记修复。发布检查与最终发布都会先同步当前应用自己的 `PAGE_FORM` 托管表，手工对象、其它应用对象和非追加式 DDL 仍受原门禁约束。前端 15 个 Vitest、后端 32 个 JUnit、目标 ESLint、生产构建和 `git diff --check` 均通过；未启动真实数据库或执行 DDL。

## Task 27: 修复真实预览循环并压缩发布重复操作

> status: completed

- **目标**：真实预览的列表接口只在请求条件实际变化时调用；打开发布抽屉不再隐式执行全量预检；相同设计版本已经 `IN_SYNC` 的自动托管表不重复扫描数据库结构。
- **涉及文件**：
  - `runtime-crud-props.js` 与 Vitest — 生成稳定预览请求签名，证明状态文案变化不改变请求身份。
  - `GridBlockRenderer.vue` — 以稳定签名监听真实预览，其他运行数据监听改为叶子 source，消除区块对象替换造成的误触发。
  - `application-runtime.[applicationCode].vue` — 发布抽屉只负责打开，不再自动调用完整发布检查。
  - `BusinessApplicationFormDataService.java` 与 JUnit — 复用相同设计版本的 `IN_SYNC` 证据，只同步真正需要检查的托管表。
- **实施顺序**：先补预览请求签名和已同步托管表跳过测试，再实现前后端最小修复；随后执行目标 ESLint/Vitest、生成器 `test-compile`、定向 JUnit、生产构建和差异检查。
- **验收标准**：一次真实列表预览只产生一次列表请求；成功/失败状态写回不触发下一次请求；直接发布不再先自动预检；已同步表的发布准备额外自动同步执行 0 次，失同步表仍执行 1 次；高风险同步门禁、对象最终表校验和发布运行步骤证据保持不变。
- **结果**：真实预览改为监听稳定请求签名，结果状态和文案写回不再触发 `loadList()`；其余运行数据监听也改为逐项叶子 source。打开发布抽屉只加载历史，显式检查与直接发布分离；相同设计版本 `IN_SYNC` 的自动托管表跳过额外同步，失同步表仍进入安全同步。Node `v20.19.0` 下 17 个 Vitest、目标 ESLint、8792 模块生产构建通过；JDK 17 下 29 模块测试编译和 23 个 JUnit 通过；`git diff --check` 无输出。

## Task 28: 修复动态页面查询条件运行协议

> status: completed

- **目标**：页面配置的查询字段、查询方式、查询组件和映射字段真正进入动态 CRUD 请求与 SQL，解决输入条件后列表不筛选的问题。
- **涉及文件**：
  - `runtime-crud-props.js` 与 Vitest — 合并 `searchFieldRefs` 和 `searchFieldSettings`，生成稳定、安全的页面查询 Schema 及查询方式请求元数据。
  - `GridBlockRenderer.vue` — 将页面查询 Schema 用于 `AiCrudPage`，并把查询方式元数据合入动态列表和导出公共请求参数。
  - `DynamicCrudQuery.java`、`DynamicCrudController.java` — 解析并隔离查询方式控制参数，保留现有平铺查询字段协议。
  - `DynamicCrudService.java` — 查询白名单覆盖动态配置已公开的查询/列表/编辑字段，并校验页面请求的查询方式覆盖。
  - 前后端定向测试、当前 Spec、测试计划、执行日志和长期记忆。
- **实施顺序**：先补页面查询设置和后端参数解析/白名单红灯用例；再实现最小协议修复；最后执行目标 Vitest/ESLint、生成器 `test-compile`、定向 JUnit、生产构建和差异检查。
- **验收标准**：表单字段即使不在对象原始查询 Schema 中，页面选为查询条件后也能生成 WHERE；“包含/等于/区间/多值”和映射字段按页面配置生效；控制参数不进入 SQL 字段；传统对象查询页面保持兼容；不启动数据库、后端、Vite 或浏览器。
- **结果**：页面运行 Schema 已合并 `searchFieldRefs` 与 `searchFieldSettings`，查询方式、查询组件及映射字段会进入当前 `AiCrudPage`；列表和导出通过独立 `_searchTypes` 控制参数传输查询方式。后端将控制参数与业务字段隔离，只允许动态配置已公开的查询/列表/编辑字段及固定操作符，传统对象查询配置继续作为默认协议。Node `v20.19.0` 下目标 ESLint 0 error/0 warning、3 个 Vitest 文件 19 tests、8792 模块生产构建通过；JDK 17 下生成器 reactor 29 模块测试编译和 2 类 6 个 JUnit 通过；`git diff --check` 无输出。未启动数据库、后端、Vite 或浏览器，真实 Network 与数据库筛选结果留人工验收。

## Task 29: 收口应用创建后的字段、预览、对象状态、值协议与菜单行为

> status: completed

- **目标**：修复新建应用实测中的五类断点，让字段占位、草稿 CRUD、数据库同步摘要、人员/组织保存值和系统菜单生成遵循一致协议。
- **涉及文件**：
  - `ForgePropertyPanel.vue` 与占位纯函数/Vitest — 字段名称变化自动同步选择类、输入类占位，保护用户手工值，并覆盖字段资产面板保存入口。
  - `runtime-crud-props.js`、`GridBlockRenderer.vue`、`AiCrudConfigService.java`、`DynamicCrudService.java` 及定向测试 — 补齐区块基础 API 的设计预览参数，并允许业务应用编辑权限访问草稿 CRUD。
  - `BusinessApplicationObjectService.java` 及 JUnit — 保留最后一次真实结构检查状态，不因非结构设计版本变化长期显示“待检查”。
  - `AiFormItem.vue`、`LowcodeRuntimeConfigBuilder.java`、`DynamicCrudService.java` 及前后端测试 — 人员和组织主值只保存 ID，展示名写入独立字段，错误同字段映射自动纠正并拒绝非数字 `bigint` 选择值。
  - `BusinessApplicationPageMenuPublishService.java`、应用内页面 Schema、业务对象/低代码发布菜单开关及测试 — 默认同步空菜单列表，应用协调发布不创建 AI 能力菜单或通用业务域目录，显式菜单行为和旧低代码发布兼容。
- **实施顺序**：先补本轮 Spec 与失败测试，再实施前后端最小修复；使用 Node `v20.19.0` 和 JDK 17 执行定向测试、测试编译、聚合构建与差异检查。
- **验收标准**：字段改名后自动占位正确且手工值不变；应用编辑者可直接使用草稿 CRUD；真实 `IN_SYNC` 摘要不再被版本漂移降级；人员/组织主字段保存 ID、名称字段保存文本且非法姓名写入被拒绝；默认发布不创建任何系统菜单或业务域目录，显式开启时仍可同步；不启动真实服务或数据库，不执行 DDL/Flyway。
- **结果**：字段名称变化会按输入/选择语义同步默认占位并保护手工占位；应用编辑权限可访问带 `designPreview=1` 的草稿 CRUD，正式运行发布门禁不变；对象列表保留最近一次真实数据库检查状态；人员和组织主字段只保存数字 ID，展示名与主字段同名的错误配置会改写到独立名称字段，后端拒绝姓名/组织名写入 `bigint`；新页面默认不生成系统菜单，应用协调发布与回滚关闭旧低代码菜单同步，不创建 AI 能力下的应用菜单或通用业务域菜单目录，显式开启页面菜单和旧低代码直接发布仍兼容。Node `v20.19.0` 下 ESLint、5 个 Vitest 文件 33 tests、8864 模块生产构建通过；JDK 17 下生成器 105 个测试源编译、两组 16 + 10 个定向 JUnit、Admin 47 模块聚合打包通过；`git diff --check` 无输出。未启动服务、数据库或浏览器，未执行 Flyway、DDL、真实发布或菜单写入。
