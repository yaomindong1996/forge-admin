# 低代码设计器统一改造 — 组件物料协议与属性面板引擎
> status: apply（P1 完成 + P2 统一面板/属性引擎提前落地）
> created: 2026-09-04
> complexity: 🔴复杂
> 配套文档：`docs/低代码设计器统一改造完整方案.md`、`docs/统一组件物料清单.md`

## 1. 背景与目标

### 为什么做

当前低代码设计器存在 **5 套并行体系**（ForgeFormDesigner / ListPageGridDesigner / LowcodePageBuilder / BusinessFormDesigner / BusinessFormCreateDesigner），导致：

1. **组件库不共用**：表单用 `componentKey` 体系（33 个字段组件），列表用 `blockType` 体系（约 60 种区块），命名/分组/尺寸语义全不同
2. **属性面板不共用**：三套手写模板面板（ForgePropertyPanel 10,279 行巨石 / ListPageGridDesigner 内联面板 / ComponentPropertyPanel），加属性 = 改巨石文件 → 属性越来越不全的恶性循环
3. **同一组件两边能力不对等**：AiCrudPage 在列表侧有完整 6 分区配置（查询/表格/工具栏/默认参数/树/接口），表单侧只有简化版——"残血版 vs 完整版"
4. **预览与设计不一致**：双渲染器（设计自绘 vs 运行 AiForm）必然漂移；栅格语义冲突（设计 24 列 vs 运行 gridCols）
5. **数据源配置 4 套模型并行**：optionSource / transfer dataSourceType / 挂件 dataBinding / CRUD 接口组

### 做完后的效果（可验证）

- **物料同源**：三个设计器左侧面板数据来自同一份 component-spec（102 个统一组件）
- **属性面板 schema 驱动**：加属性只改 spec JSON，不再改巨石文件
- **AiCrudPage 两边同配置**：任何设计器里都是完整版（列表侧 6 分区基准）
- **dataTransfer 只剩一个协议** `application/x-forge-designer`
- **净删除 2 万行以上重复实现**

### 改造主线

**一份物料协议 + 一套 schema 驱动属性面板 + 一个渲染器（设计态 = 运行态 + 编辑壳）**

### 统一基准决策（用户已确认 2026-09-02）

| 维度 | 基准 | 说明 |
|------|------|------|
| CRUD 组件配置 | **列表侧** | 列表侧 6 分区配置为迁移基准，表单侧独有项（展开行配置）合并入 |
| 画布交互手感 | **表单侧** | 拖拽/锚点/组件块拖动/删除以表单侧为验收标准 |
| 渲染架构 | **列表侧** | 画布与运行共用渲染器（设计 = 运行 + 编辑壳） |
| 冗余文件 | **同步删除** | 每阶段只留一条实现路径 |

## 2. 代码现状（Research Findings）

### 2.1 四处物料定义（各自独立）

#### 表单项：FIELD_COMPONENT_PALETTE_GROUPS
- **文件**：`views/app-center/components/designer/form-first/fieldComponentCatalog.js`（123 行）
- **结构**：3 个分组（输入 8 个 / 选择 17 个 / 业务 8 个），共 33 个字段组件
- **元数据**：`{ componentKey, label }`，极简
- **默认值**：`FIELD_COMPONENT_DEFAULTS` 提供 fieldType/dataType/queryType 等

#### 列表区块：listPageBlockCatalog
- **文件**：`components/lowcode-builder/page/page-schema.js:580`（2852 行文件）
- **结构**：约 60 种 blockType，9 个分组（data/action/page/layout/extra/media/navigation 等）
- **元数据**：`{ blockType, group, title, desc, defaultW, defaultH, container, unique, multiField, onlyFor }`
- **尺寸语义**：12 列 gridW + gridH 像素网格（与表单 24 列 span 不同）

#### 页面挂件：pageWidgetCatalog（唯一共享）
- **文件**：`components/lowcode-builder/shared/page-widget-schema.js`（585 行）
- **结构**：21 个挂件（rich-text/transfer/watermark/vue-component/html-tag/markdown/barcode/qrcode/calendar/code/countdown/descriptions/announcement/list/log/number-animation/breadcrumb/menu/pagination/split）
- **元数据**：`{ blockType, componentKey, group, title, label, desc, defaultW, defaultH, container }`
- **问题**：虽然两边都引用，但各自包了一层不同元数据映射

#### 旧版 Zone 组件：canvasComponentCatalog（审计补全发现）
- **文件**：`components/lowcode-builder/page/page-schema.js:30`（21 项）
- **结构**：LowcodePageBuilder 早期 zone-based 设计器的物料，含 6 个独立功能组件 + 14 个字段包装组件 + 1 个已有组件（data-table）
- **独立组件（6 个）**：query-set / custom-query / import-button / export-button / add-button / reset-button
- **字段包装（14 个）**：field-input / field-textarea / field-number / field-select / field-dict-select / field-tree-select / field-org-tree-select / field-user-select / field-region-tree-select / field-cascader / field-date / field-datetime / field-switch / field-upload / field-image-upload — 这些是对基础字段组件的 zone 包装，通过 aliases 映射解决
- **元数据**：`{ group, componentKey, title, desc, zones, defaultWidth, defaultHeight, multiField }`

#### 表单侧布局项：ForgeFieldShelf layoutItems
- **文件**：`designer/forge-form-designer/ForgeFieldShelf.vue:262`（10 项）
- **结构**：row / table / AiCrudPage / subTable / button / title / AiFormSectionTitle / card / tabs / collapse + 20 个 pageWidgetCatalog 映射
- **问题**：componentKey 命名与 listPageBlockCatalog 不一致（如 button vs action-button, title vs section-divider）

### 2.2 三套属性面板（各自独立）

| 面板 | 文件 | 行数 | 驱动方式 |
|------|------|------|---------|
| ForgePropertyPanel | `designer/forge-form-designer/ForgePropertyPanel.vue` | **10,279** | 手写模板，Tab：基础配置/CRUD配置/样式/交互 |
| ListPageGridDesigner 内联 | `lowcode-builder/page/ListPageGridDesigner.vue` | **13,866**（含约 5000 行属性面板） | 手写模板，Tab：属性/样式/交互 |
| ComponentPropertyPanel | `lowcode-builder/page/ComponentPropertyPanel.vue` | 562 | 手写模板 |

**核心问题**：三套全是手写模板而非 schema 驱动——每加一个属性要改巨石文件，这是"属性不全"的恶性循环根源。

### 2.3 数据源 4 套模型

| 模型 | 覆盖组件 | 来源文件 |
|------|---------|---------|
| A. optionSource | select/radio/checkbox/cascader/treeSelect | ForgePropertyPanel 内部 |
| B. dataSourceType | 仅 transfer | page-widget-schema.js `createPageWidgetDefaultProps` |
| C. dataBinding | 20 个挂件 | page-widget-schema.js `createWidgetDataBinding` |
| D. CRUD 接口组 | AiCrudPage | ListPageGridDesigner 内部 |

运行时 `AiFormItem.resolveOptionSource` 已支持 7 类源，缺的是**配置面统一**。

### 2.4 发现与风险

1. **巨石文件**：ForgePropertyPanel 10,279 行 + ListPageGridDesigner 13,866 行 + application-runtime 7,725 行 = 31,870 行，任何改动高风险
2. **栅格语义冲突**：设计态 24 列 span，运行态 gridCols（默认 1），`AiFormLayoutNodes` 352-354 行有补丁 hack
3. **三协议并存**：application-runtime 3301-3307 行同一组件要往 dataTransfer 塞 3 种 MIME（`x-list-block` / `x-forge-form-layout` / `x-forge-app-page-block`）
4. **存量 schema 兼容**：现有页面使用 row/grid-layout/fcRow/fcTable/elCard/elTabs/elCollapse/section-divider/AiFormSectionTitle 等历史类型名
5. **componentTypeAlias 散落别名**：ForgeFormDesigner.vue:1012-1062 有 30+ 个运行时别名未集中管理，需全量迁入 component-spec 的 aliases
6. **AiFormSectionTitle 身份模糊**：代码中 divider（分隔线）和 AiFormSectionTitle（表单分隔线）是两个不同运行时组件（ForgeFormDesigner.vue:1396-1405），统一后 AiFormSectionTitle 作为独立组件 formSectionTitle，divider 保留为独立分隔线组件
7. **canvasComponentCatalog 第四套物料**：page-schema.js:30-231 有 21 个旧版 zone 组件未被物料清单覆盖，其中 6 个是独立功能组件（query-set/custom-query/import-button/export-button/add-button/reset-button），14 个是字段包装组件（field-* 系列）

## 3. 功能点

### Phase 1：物料协议（P1，2-3 周）

- [ ] **F1.1**：创建 `designer-core/spec/component-spec.js` 统一组件注册表，102 个组件逐一登记
  - 输入：四处物料目录（fieldComponentCatalog / listPageBlockCatalog / pageWidgetCatalog / canvasComponentCatalog）
  - 处理：取并集去重，统一 schema（type/scope/aliases/propsSchema/container/layout/print/dataSources）
  - 输出：一份权威 component-spec，三个设计器消费同一数据源
- [ ] **F1.2**：aliases 存量兼容映射（含审计补全）
  - 布局容器：row → grid, fcRow → grid, grid-layout → grid, col → grid-col
  - 表格布局：table → table, tableGrid → table, fcTable → table
  - 卡片/标签/折叠：card → card, elCard → card, tabs → tabs, elTabs → tabs, collapse → collapse, elCollapse → collapse
  - 分组标题：title → groupTitle, fcTitle → groupTitle, sectionTitle → groupTitle, groupHeader → groupTitle, titleBlock → groupTitle, section → groupTitle, section-divider → groupTitle
  - 表单分隔线：divider → formSectionTitle, elDivider → formSectionTitle, AiFormSectionTitle → formSectionTitle, formSectionTitle → formSectionTitle
  - 按钮合并：button → action-button
  - CRUD 别名：crud → AiCrudPage, crudBlock → AiCrudPage, aiCrudPage → AiCrudPage
  - 字段组件别名：inputNumber → number, radioGroup → radio, checkboxGroup → checkbox, datePicker → date, timePicker → time, colorPicker → color, upload → fileUpload
  - Zone 字段包装（14 个）：field-input → input, field-textarea → textarea, field-number → number, field-select → select, field-dict-select → dictSelect, field-tree-select → treeSelect, field-org-tree-select → orgTreeSelect, field-user-select → userSelect, field-region-tree-select → regionTreeSelect, field-cascader → cascader, field-date → date, field-datetime → datetime, field-switch → switch, field-upload → fileUpload, field-image-upload → imageUpload
- [ ] **F1.3**：统一拖拽 MIME 协议
  - 输入：现存 3 种 MIME（`x-list-block` / `x-forge-form-layout` / `x-forge-app-page-block`）
  - 处理：定义 `application/x-forge-designer` 统一 payload 规范
  - 输出：`designer-core/dnd/protocols.js`，三处设计器统一引用
- [ ] **F1.4**：DataSourceSpec 七类源声明
  - 每个组件在 spec 中声明支持的数据源 kind（static/dict/managed/remote/context/relation/builtin）
  - 输出：组件 spec 中的 `dataSources: []` 字段

### Phase 2：属性面板引擎（P2，3-4 周）

- [ ] **F2.1**：SpecPropertyPanel — schema 驱动的通用属性面板
  - 输入：component-spec 的 propsSchema
  - 处理：递归渲染属性编辑器（文本/数字/开关/选择/颜色/JSON/代码/自定义编辑器）
  - 输出：一个 Vue 组件替代三套手写面板
- [ ] **F2.2**：PropertyPanelShell — 统一外壳（组件|页面 切换 + 属性/样式/交互 三 Tab）
- [ ] **F2.3**：DataSourceEditor — 首个 customEditor 试点
  - 七类源选择器 + 静态数据编辑 + 远程接口配置（受管 API 选择器 + 手工输入双模式）
- [ ] **F2.4**：CRUD 组件 propsSchema 声明化（以列表侧 6 分区为基准）
  - CrudHookRulesEditor / CrudDefaultParamsEditor 挂为 customEditor
- [ ] **F2.5**：组件树面板 — 画布节点大纲树（点击定位/拖拽调序/显隐锁定删除）

### Phase 3：画布统一 + 渲染器统一（P2.5 + P3，7-10 周）

- [ ] **F3.1**：designer-dnd 统一拖拽引擎（Pointer Events 为主，HTML5 DnD 兜底）
- [ ] **F3.2**：列表画布自由网格 → 流式栅格迁移（schema 版本化迁移函数）
- [ ] **F3.3**：RuntimeNode 递归渲染器 + EditorShell（设计画布 = 运行组件 + 编辑壳）
- [ ] **F3.4**：ForgeFormCanvasNode 自绘渲染退役
- [ ] **F3.5**：表单打印轻量版（AiForm 只读 + 打印 CSS + 基础打印设置）

## 4. 业务规则

### 4.1 物料协议规则

- 每个组件有唯一 `type`（统一类型名），历史名称进 `aliases`
- `scope` 声明可用设计器：`F`（表单）/ `L`（列表）/ `F+L`（两者皆可）
- `container: true` 的组件可嵌套子组件，`maxDepth` 默认 4
- `propsSchema` 定义属性面板的 JSON Schema，驱动 SpecPropertyPanel 自动渲染
- `dataSources` 声明该组件支持的数据源类型子集

### 4.2 属性面板规则

- **禁止手写组件专属模板分支**——所有属性编辑通过 propsSchema 或 customEditor 挂载
- 通用属性（id/label/span/hidden/background/border/padding/class）自动注入，不在各 spec 重复
- 复杂编辑器（公式/联动/数据源/CRUD 配置）作为 customEditor 组件挂载

### 4.3 画布交互规则

- 选中态：2px 主色描边 + 浮动工具条
- 边缘选容器 / 内容穿透选子组件 / 双击逐层下钻
- 拖入预览：插入指示线 + 容器高亮
- 拒绝态：红色提示 + 原因文案（"请拖到具体格子里" / "该容器不支持放入这个组件"）
- span 拖点：1/24 步进吸附
- 键盘：Del 删除 / Ctrl+D 复制 / Ctrl+Z 撤销 / ↑↓ 调序 / Esc 取消

### 4.4 数据源规则

- 一个通用 DataSourceEditor，组件在 spec 声明支持哪些 kind
- remote 一律"受管 API 选择器 + 手工输入"双模式
- 预览态远程源显示"远程数据 N 条（设计态不加载）"占位提示

## 5. 数据变更

本阶段（P1 物料协议）不涉及数据库变更，纯前端重构。

后续阶段可能涉及：
| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|----------|------|
| 扩展 | sys_app_page | schema_version | schema 版本号字段（已有 VIEW_SCHEMA_VERSION 先例） |

## 6. 接口变更

本阶段不涉及后端接口变更。

## 7. 影响范围

### 直接影响文件（P1）

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `components/lowcode-builder/designer-core/spec/component-spec.js` | **新增** | 统一组件注册表（102 个组件 spec） |
| `components/lowcode-builder/designer-core/dnd/protocols.js` | **新增** | 统一拖拽 MIME 协议 |
| `components/lowcode-builder/designer-core/spec/data-source-spec.js` | **新增** | DataSourceSpec 七类源定义 |
| `views/app-center/components/designer/form-first/fieldComponentCatalog.js` | **修改** | 改为从 component-spec 派生 |
| `components/lowcode-builder/page/page-schema.js` | **修改** | listPageBlockCatalog 改为从 component-spec 派生 |
| `components/lowcode-builder/shared/page-widget-schema.js` | **修改** | pageWidgetCatalog 改为从 component-spec 派生 |

### 间接影响（P2/P3 阶段）

- ForgePropertyPanel.vue（10,279 行）→ 被 SpecPropertyPanel 替代后删除
- ListPageGridDesigner.vue 内联属性面板（约 5,000 行）→ 拆分为独立面板后替代
- BusinessFormDesigner.vue（2,684 行）→ 与 ForgeFormDesigner 合并
- ForgeFormCanvasNode.vue 自绘渲染（2,578 行）→ 被 RuntimeNode + EditorShell 替代
- application-runtime 三协议兼容段 → 统一为单协议

## 8. 风险与关注点

| 风险 | 等级 | 应对 |
|------|------|------|
| 存量页面 schema 兼容 | 高 | aliases 承接历史类型名；迁移函数带单测；旧 schema 打开时自动转换 |
| 巨石文件改动回归 | 高 | P1 不动巨石文件，只建统一注册表 + 桥接层；P2 渐进替换 |
| 102 个组件 spec 工作量大 | 中 | 按分类分批：字段组件(33) → 布局容器(11) → 业务区块(12) → 页面区块(16) → 媒体(4) → 挂件(20) → Zone操作组件(6) |
| 三处物料消费方多 | 中 | 新注册表先建桥接函数，各消费方逐步切换，不断旧链路 |
| 表单分区权限依赖拍平结构 | 中 | pageSections 保留为权限视图独立计算，不碰渲染树 |
| 嵌套放开后拖拽复杂度 | 中 | maxDepth=4；命中判定沿用列表侧已验证的格子级命中 |

## 8.5 测试策略

- **测试范围**：
  - P1：component-spec 完整性测试（102 个组件全覆盖）、aliases 映射正确性、桥接函数输出一致性
  - P2：SpecPropertyPanel 渲染测试（每种组件类型至少一个 spec 驱动渲染）、DataSourceEditor 七类源切换
  - P3：渲染一致性快照测试（设计/预览/运行/打印四种目标 DOM 结构比对）
- **覆盖率目标**：核心 spec 解析和 aliases 映射 100%；属性面板渲染 80%
- **独立 Test Spec**：是，每阶段独立 test-spec.md

## 9. 待澄清

- [x] CRUD 组件配置以列表侧为基准 ✅（用户已确认 2026-09-02）
- [x] 画布交互以表单侧为基准 ✅（用户已确认 2026-09-02）
- [x] 同步删除冗余文件 ✅（用户已确认 2026-09-02）
- [ ] 画布统一流式栅格（span/24），列表自由像素画布退役？——建议：是
- [ ] 右栏「组件|页面」+「属性/样式/交互」三 Tab？——建议：是
- [ ] 嵌套深度 maxDepth=4 + 组件树面板？——建议：是
- [ ] 数据源 DataSourceSpec 七类源 + DataSourceEditor 试点？——建议：是
- [ ] 表单画布加「弹窗/抽屉/整页」三档宿主宽度切换？——建议：是
- [ ] 打印排期：轻量版随 P0 先行，完整版随 P3？——建议：是

## 10. 技术决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 物料协议格式 | JS 对象注册表（非 JSON 文件） | 需要引用 Vue 组件、函数（customEditor/validator），纯 JSON 不够 |
| 属性面板驱动 | JSON Schema + customEditor 扩展 | 标准 JSON Schema 覆盖 80% 属性，剩余 20% 用 customEditor 组件 |
| 拖拽引擎 | Pointer Events 为主 | HTML5 DnD 的 ghost 不可控、不支持移动端；Pointer Events 更灵活 |
| 画布模型 | 流式栅格（24 列 span） | 运行页本来就把自由坐标重新流式排版；列表页天然纵向堆叠 |
| schema 版本化 | 版本号 + 迁移函数 | 已有 VIEW_SCHEMA_VERSION 先例，渐进迁移 |
| 文件组织 | `designer-core/` 独立目录 | 与现有设计器解耦，各设计器逐步消费 |

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|------------|------|
| Task 1 基础结构与类型定义 | ✅ | `designer-core/spec/types.js`、`registry.js`、`index.js` | Map 注册表 + get/list/filter API |
| Task 2 字段组件 spec（33 个） | ✅ | `designer-core/spec/field-components.js` | 含 componentTypeAlias + field-* 包装别名 |
| Task 3 布局容器 spec（11 个） | ✅ | `designer-core/spec/layout-components.js` | formSectionTitle 独立组件 |
| Task 4 业务区块 spec（12 个） | ✅ | `designer-core/spec/business-components.js` | AiCrudPage 列表侧基准 |
| Task 5 页面/媒体/Zone 操作 spec（26 个） | ✅ | `page-components.js`、`media-components.js`、`zone-action-components.js` | 含审计补全 6 个 Zone 组件 |
| Task 6 页面挂件 spec（18 个） | ✅ | `widget-components.js` | scope F+L（表单侧同步可见） |
| Task 7 DataSourceSpec + 拖拽协议 | ✅ | `data-source-spec.js`、`dnd/protocols.js` | 七类源声明 |
| Task 8 桥接层（四处消费方） | ✅ | `spec/bridge.js`、`fieldComponentCatalog.js`、`page-widget-schema.js`、`page-schema.js` | 新增 `FORM_COMPONENT_KEY_OVERRIDES`（grid→row、groupTitle→title、formSectionTitle→AiFormSectionTitle、action-button→button） |
| Task 9 单元测试 | ✅ | `__tests__/registry/aliases/bridge/data-source.test.js` | 红线测试 8 个：列表 59 项精确匹配、表单映射、scope 可见性 |
| Task 10 canvasComponentCatalog 桥接 | ✅ | `spec/bridge.js`、`page-schema.js` | 21 项输出一致 |
| Task 11 文档与收尾 | ✅ | 本文件 + `execution-log.md` + `tasks.md` | |

### P2 提前落地（本轮增量，2026-09-05）

> 用户反馈“页面上没变化、组件不统一、属性不全、嵌套不好使”，本轮把 P2 的统一面板/属性引擎/嵌套放开直接落地到两个设计器 UI。

| 增量项 | 状态 | 实际改动文件 | 说明 |
|--------|------|------------|------|
| SpecPropertyPanel schema 驱动属性面板引擎 | ✅ | `designer-core/panel/SpecPropertyPanel.vue` | 文本/数字/开关/选择/颜色/JSON 编辑器；排除表跳过手写属性 |
| 列表侧属性面板 spec 兜底 | ✅ | `ListPageGridDesigner.vue` | 无手写分支的 blockType 自动渲染 spec 属性（栅格列数/间距等已可配） |
| 表单侧属性面板 spec 兜底 | ✅ | `ForgePropertyPanel.vue` | 排除表含 columns（row 面板已有手写栅格总列数） |
| 统一左侧组件面板 UnifiedComponentPalette | ✅ | `designer-core/panel/UnifiedComponentPalette.vue` | scope F/L 过滤同源展示；emits itemDragStart/End/itemClick/totalChange |
| 列表侧接入统一面板 | ✅ | `ListPageGridDesigner.vue` | 替换旧 palette；删死代码约 190 行（groupedBlocks/iconMap/handlers/18 个 unused icon imports） |
| 表单侧接入统一面板 | ✅ | `ForgeFieldShelf.vue` | 组件库 Tab 接 scope=F；双链路拖拽（字段模板 x-forge-form-template / 布局 x-forge-form-layout）；删手写 iconMap+layoutItems 约 260 行 |
| 嵌套放开 + 深度保护 | ✅ | `ListPageGridDesigner.vue` | card/tabs/grid-layout/box-layout 可入格子（maxDepth=2）；appendContainerChild/appendGridCellChild 双入口深度保护 |
| spec 层 scope/alias 修正 | ✅ | `zone-action-components.js`、`widget-components.js`、`media-components.js` | action-button 合并 button（物料清单 L181）；17 挂件+barcode/qrcode+transfer 改 F+L |

### 验证记录（2026-09-05）

- `npx vitest run src/components/lowcode-builder src/views/app-center/components/designer`：**25 文件 201 测试全过**（含 8 个红线测试）
- `npx vitest run`（全量）：1012/1014 过；2 个失败为 `application-designer-phase-e-contract.spec.js` 存量问题（期望旧实现 `createQuickNode('page')`，目标文件已演进为 `openPageTypeSelector()`，测试与目标文件均与本变更无 diff）
- ESLint 10 个改动文件零 error（`spec/bridge.js` 剩 10 个 JSDoc warning 为历史遗留，不阻断）
- `pnpm build` 构建验证：见 `execution-log.md`

## 12. 审查结论

（待 /review 后填写）

## 13. 确认记录（HARD-GATE）

- **确认时间**：（待用户确认）
- **确认人**：（待用户确认）
