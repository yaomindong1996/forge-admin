# 任务拆分 — 低代码表单设计器顺滑拖拽改造
> 拆分顺序：数据模型 → 接口协议 → 底层实现 → 上层编排 → 入口层
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> 每个任务必须精确到文件路径和函数签名

## 前置条件

- [x] 用户确认 `code-copilot/changes/lowcode-form-designer-smooth-dnd/spec.md`。
- [x] 确认首期不改后端接口和数据库结构。
- [x] 确认主链路替换 `BusinessFormCreateDesigner`，FormCreate 只作为回退。
- [x] 执行 `/test` 或阶段验证前读取 `code-copilot/rules/automated-testing-standard.md`。

## Task 1: 设计器 Schema 操作工具

> 本轮状态：已完成首批组件树查找、插入、移动、删除、复制、属性更新工具；后续补单元测试。

- **目标**：提供直接操作 `formDesignerSchema.components` 的纯函数，避免组件里散落深拷贝和路径处理。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js` — 增强组件树路径、插入、移动、删除、复制、容器判断工具。
  - `forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.spec.js` — 新增或补充工具函数测试。
- **关键签名**：
  ```js
  export function findDesignerComponentPath(schema, componentId) {}
  export function insertDesignerComponent(schema, target, component) {}
  export function moveDesignerComponent(schema, sourceId, target) {}
  export function removeDesignerComponent(schema, componentId) {}
  export function duplicateDesignerComponent(schema, componentId) {}
  export function canAcceptDesignerChild(parentComponent, childComponent) {}
  ```
- **验收点**：
  - 字段能插入根画布、卡片、标签页、折叠项。
  - 字段移动后不丢失 `fieldBinding`、`validation`、`props`。
  - 删除字段组件不删除字段资产。

## Task 2: 拖拽状态与动效 Composable

> 本轮状态：左侧字段/布局拖入仍保留 native drag；画布内已有节点移动已改为 Pointer 拖拽，拖拽手柄 `pointerdown` 开启自管会话，`pointermove` 直接驱动跟手镜像和共享落点 active 状态，`pointerup` 一次性调用 `moveDesignerComponent()`。落点状态已抽到 `designerDragState.js` 共享，保证同一时间只出现一个单层灰色虚线落点块。FLIP 重排和更完整的独立 composable 仍待后续实现。

- **目标**：封装拖拽会话、落点计算、拖拽镜像、原位幽灵、双胞胎阴影和 reduced-motion 降级。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/useForgeDesignerDrag.js` — 新增拖拽状态和指针事件逻辑。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/drag-animation.css` — 新增拖拽动效 token 和 class。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/useForgeDesignerDrag.spec.js` — 新增落点计算测试。
- **关键签名**：
  ```js
  export function useForgeDesignerDrag(options) {}
  function startDrag(payload, pointerEvent) {}
  function updateDrag(pointerEvent) {}
  function endDrag(pointerEvent) {}
  function cancelDrag() {}
  function resolveDropTarget(pointer, registry) {}
  ```
- **验收点**：
  - 拖拽镜像使用 `transform: translate3d()` 跟手。
  - 原位组件进入 ghost 状态。
  - 目标落点显示同尺寸 placeholder 和第二层偏移阴影。
  - 可拖拽元素 hover 后显示拖拽手柄，鼠标样式从 `grab` 到 `grabbing` 变化。
  - 禁止拖拽的元素 hover 后有 disabled 视觉状态和不可拖原因。
  - `prefers-reduced-motion: reduce` 下禁用重排动画，仅保留落点高亮。

## Task 3: 原生画布组件 ForgeFormCanvas

> 本轮状态：已完成首版画布、真实表单控件预览、节点悬浮手柄、选中工具条、1-6 列根级 grid、行列间距、空容器投影和栅格列投放限制；CRUD 区块设计态已改为真实 `AiCrudPage` lazy 渲染并复用子字段生成 search/edit/columns，且支持通过字段用途配置决定查询条件、表格列和编辑弹窗字段；卡片、标签页、折叠面板已改为真实 Naive 组件包裹设计器节点；高级跟手镜像与 FLIP 动效待后续增强。

- **目标**：实现中间主画布，直接渲染 Forge Schema，并提供选择、拖入、重排、容器嵌套能力。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue` — 新增画布组件。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue` — 新增递归节点组件。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeDropIndicator.vue` — 新增落点和双胞胎阴影组件。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeDragOverlay.vue` — 新增跟手镜像组件。
- **关键签名**：
  ```vue
  <ForgeFormCanvas
    v-model:schema="localSchema"
    :fields="fields"
    :selected-id="selectedId"
    @update:selected-id="selectedId = $event"
    @dirty-change="emit('dirtyChange', true)"
  />
  ```
- **验收点**：
  - 根画布、空容器、非空容器都有明确 drop zone。
  - 选中组件显示稳定边框和工具条，不造成布局跳动。
  - 画布视觉层级清楚，字段、容器、落点、选中态不会互相抢焦点。
  - 1366x768 下画布仍是主要视觉区域，顶部和侧栏不挤压核心操作。
  - 拖动过程不频繁提交完整 schema，落下后一次性更新。

## Task 4: 字段库与布局组件面板

> 本轮状态：已完成字段/布局面板首版，支持搜索、字段分组、基础组件分组、布局分组、hover 手柄、点击添加和拖拽 payload；2026-06-16 补充输入/选择/业务组件分组，新增表格布局和 CRUD 区块模板入口，拖拽手柄统一为 6 点 DragOutlined SVG；本轮进一步把组件库改为更紧凑的工具箱样式，分组标题带数量，字段列表和组件按钮视觉统一，并对齐 `AiFormItem` 已支持组件，新增颜色选择、滑块、评分、按钮单选、级联、树选择、远程选择、日期/时间范围、年月和文本展示模板；本轮新增按钮组件入口，拖入后按真实 Naive `NButton` 在画布预览，可在属性面板配置点击交互。

- **目标**：把左侧字段资产和布局组件做成高效可拖动面板，降低用户理解成本。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue` — 新增字段资产面板。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeLayoutPalette.vue` — 新增布局组件面板。
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue` — 接入新字段库或移除旧重复字段库。
- **关键签名**：
  ```vue
  <ForgeFieldShelf
    :fields="primaryDesignFields"
    :used-field-set="usedFieldSet"
    @append-field="appendField"
    @start-drag="drag.startFromShelf"
  />
  ```
- **验收点**：
  - 字段按未使用、已使用、系统字段分组。
  - 已使用字段不可重复拖入，但可点击定位画布中的组件。
  - 布局组件包含标题、卡片、标签页、折叠面板、栅格行。
  - 字段和布局条目 hover 后有明确可拖反馈：手柄图标、背景、边框或轻阴影。
  - 字段库支持搜索或快速过滤，避免字段多时用户找不到目标。
  - 空状态和禁用状态文案直接说明下一步动作。

## Task 5: 属性面板与快捷操作

> 本轮状态：已完成属性面板首版，支持“基础 / 布局 / 状态”页签、常用字段属性、表单列数、row 栅格列数/间距、col 容器属性和回到表单属性；2026-06-16 调整为更紧凑的工具面板视觉，选中摘要和属性分区更清晰；本轮改为右侧栏默认收起，点击节点“更多 -> 配置”再打开，并新增 CRUD 区块的 `AiCrudPage.apiConfig`、行主键、查询分页、导入导出和弹窗配置；本轮进一步按飞书式线型 Tab 重做配置结构，补齐 `AiForm` 的尺寸、标签、间距、反馈和表单样式配置，以及 `AiCrudPage` 的搜索表单、表格、编辑表单、弹窗、分页、工具栏和导入导出配置；颜色相关配置已改为颜色选择器，表单列数改为 1-6 滑块 + 数字输入，样式配置新增 padding/margin 四边编辑，并修正窄面板数字输入被加减按钮挤压的问题；基础配置页签已改为折叠分组，CRUD 页签新增字段用途矩阵；本轮删除重复的 `selected-summary`，按当前 Naive UI 类型定义补充 Card/Tabs/Collapse/日期时间常用 props，并把 CRUD API、弹窗分页、导入导出和开关类低频项收到“更多配置”抽屉；本轮新增组件级“更多属性”抽屉，覆盖输入、数字、选择、单选、多选、开关、滑块、评分、颜色、级联、树选、日期时间、Card、Tabs、Collapse 的可序列化 Naive props，多选选项支持 option props JSON；CRUD 字段选中后新增查询条件、表格列和编辑弹窗的字段级配置；本轮将更多属性标签改为中文说明 + prop 名，CRUD 面板改为“字段与列配置”卡片，列标题/列宽/对齐/固定/省略/排序直接可见，并移除 CRUD 画布中重复的顶部标题块和底部字段节点区域；本轮为 Tabs/Collapse 增加子页签/面板管理，可新增、重命名、设置 name、排序、删除和进入内容配置；新增“交互”页签，支持为组件声明 click/change/focus/blur/clear/mounted 事件和 setValue/clearValue/setOptions/openModal/apiRequest 等动作；新增“源码”页签，可查看并编辑当前组件 JSON 或整个表单 Schema JSON。

- **目标**：选中字段或布局后，右侧显示业务可理解的属性，替代 FormCreate 通用属性编辑器。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue` — 新增属性面板。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldPropertyEditor.vue` — 新增字段属性编辑。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeLayoutPropertyEditor.vue` — 新增布局属性编辑。
  - `forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js` — 增加属性更新工具。
- **关键签名**：
  ```js
  export function updateDesignerComponent(schema, componentId, patch) {}
  export function updateDesignerLayout(schema, patch) {}
  ```
- **验收点**：
  - 字段标题、占位符、列跨度、必填、只读、默认值可编辑。
  - 字典字段可编辑 `dictType` 但不得硬编码选项。
  - 布局标题、描述、折叠状态、标签名可编辑。
  - 快捷操作支持复制、删除、隐藏、上移、下移。
  - 属性按“基础 / 布局 / 校验 / 高级”分组，默认展示常用配置。
  - 属性输入控件必须有 label，不能只靠 placeholder。
  - 删除、隐藏等高风险操作有明确颜色、确认或撤销路径。

## Task 6: ForgeFormDesigner 容器组件

> 本轮状态：已完成主容器首版并接入 `BusinessFormDesigner`，默认新版画布，旧 FormCreate 画布可回退；新增左侧组件库收起/展开以扩大画布；本轮新增右侧属性栏默认收起和窄轨入口，属性栏打开后切换节点保持配置面板连续工作。

- **目标**：整合字段库、画布、属性面板、顶部工具条，形成替代 `BusinessFormCreateDesigner` 的主组件。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue` — 新增主设计器。
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue` — 将主表单画布替换为 `ForgeFormDesigner`。
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessFormCreateDesigner.vue` — 保留回退，不再作为默认主链路。
- **关键签名**：
  ```vue
  <ForgeFormDesigner
    ref="forgeFormDesignerRef"
    v-model="localFormDesignerSchema"
    :fields="primaryDesignFields"
    :object-code="objectCode"
    :object-name="objectName"
    @dirty-change="emit('dirtyChange', $event)"
  />
  ```
- **验收点**：
  - 组件职责清楚：主容器只做编排，拖拽逻辑、schema 操作、节点渲染、属性编辑分别下沉。
  - `BusinessFormDesigner` 原有 `appendField()`、`appendAllUnusedFields()`、`saveLayout()`、`syncDesignerDraft()` 行为保持。
  - 保存后 `syncFormDesignerSchemaToPageSchema()` 继续生成 `fieldSettings` 和 `formLayout`。
  - 可通过高级回退开关临时打开旧 FormCreate 设计器。

## Task 6.5: 代码清晰度与注释整理

> 本轮状态：已新增 `forge-form-designer/README.md`，说明模块职责和 Schema 纯函数边界。

- **目标**：控制新设计器复杂度，保证组件细分、逻辑清晰、关键算法有必要注释。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue` — 检查主容器是否只负责布局编排和事件协调。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/useForgeDesignerDrag.js` — 为 FLIP、碰撞检测、落点计算增加意图注释。
  - `forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js` — 为组件树路径和移动操作增加必要注释。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/README.md` — 如组件数量较多，新增模块职责说明。
- **关键签名**：
  ```md
  # forge-form-designer 模块说明
  - ForgeFormDesigner: 主编排
  - ForgeFormCanvas: 画布
  - useForgeDesignerDrag: 拖拽状态与落点计算
  - formDesignerSchema: Schema 纯函数
  ```
- **验收点**：
  - 单个复杂组件不同时承担字段库、画布、拖拽、属性面板四类职责。
  - 关键算法有注释解释“为什么这样做”，普通赋值和事件转发不写无效注释。
  - 命名能直接表达用途，不出现泛化命名堆积。
  - 后续维护者能从 README 或文件名快速理解模块结构。

## Task 7: 运行态一致性增强

> 本轮状态：已让 `AiFormLayoutNodes` 消费 `row.props.columns`，设计态栅格列数可传递到运行态；更完整的浏览器验证待后续补充。

- **目标**：保证设计态输出的布局节点在 `AiFormLayoutNodes` 中渲染一致。
- **涉及文件**：
  - `forge-admin-ui/src/components/ai-form/AiFormLayoutNodes.vue` — 补齐设计器输出节点所需的 class/style/span 支持。
  - `forge-admin-ui/src/views/ai/crud-page.vue` — 检查 `hydrateRuntimeLayoutNode()` 对新增布局 meta 的水合。
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue` — 检查 `buildRuntimeFormLayoutNode()` 输出。
- **关键签名**：
  ```js
  function hydrateRuntimeLayoutNode(node = {}, fieldMap, usedFields) {}
  function buildRuntimeFormLayoutNode(component = {}, index = 0, fieldSet, gridColumns) {}
  ```
- **验收点**：
  - 设计器中的卡片、分组、标签页、折叠面板发布后可见。
  - 字段跨度和表单列数发布后一致。
  - 旧 `formCreateRule` 不再影响新主链路结果。

## Task 8: CRM 跟进记录推荐模板

- **目标**：针对 `crm_follow_record` 提供开箱即用的清晰布局模板，验证真实业务对象体验。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/formTemplates.js` — 新增推荐模板定义。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeTemplatePicker.vue` — 新增模板选择入口。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue` — 接入模板应用。
- **关键签名**：
  ```js
  export function buildFollowRecordFormTemplate(fields, options = {}) {}
  export function applyDesignerTemplate(schema, template) {}
  ```
- **验收点**：
  - 跟进记录可一键生成“基础信息 / 跟进对象 / 跟进内容 / 时间负责人”布局。
  - 模板只使用已有字段资产，不创建数据库字段。
  - 缺失字段时模板自动跳过并提示。

## Task 9: 可访问性与键盘操作

- **目标**：拖拽不是唯一操作路径，满足后台工具基本可访问性。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue` — 补焦点、键盘和 aria。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue` — 补表单 label 和 focus 管理。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/drag-animation.css` — 补 reduced-motion。
- **关键签名**：
  ```js
  function handleNodeKeydown(event, component) {}
  function moveSelectedComponent(direction) {}
  ```
- **验收点**：
  - Tab 顺序与视觉顺序一致。
  - 选中节点有可见 focus ring。
  - 键盘支持上移、下移、删除、复制。
  - 可拖拽对象不仅依赖 hover，也要有可聚焦的手柄按钮或快捷操作。
  - reduced-motion 下无强制动画。

## Task 10: 验证与文档

- **目标**：按项目自动化测试标准完成验证，并沉淀操作说明。
- **涉及文件**：
  - `code-copilot/changes/lowcode-form-designer-smooth-dnd/test-spec.md` — 新增测试方案。
  - `code-copilot/changes/lowcode-form-designer-smooth-dnd/execution-log.md` — 追加执行日志。
  - `forge-docs/guide/conventions.md` 或专题文档 — 如需，补充表单设计器协议说明。
- **关键命令**：
  ```bash
  cd forge-admin-ui && pnpm build
  cd forge-admin-ui && pnpm lint:fix
  ```
- **验收点**：
  - 前端构建通过。
  - `crm_follow_record` 可完成设计、保存、预览、发布、打开运行应用。
  - 拖拽体验在普通动效和 reduced-motion 下均可用。
  - `execution-log.md` 记录命令、结果、警告、跳过项和服务清理情况。
