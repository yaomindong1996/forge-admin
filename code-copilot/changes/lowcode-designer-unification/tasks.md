# 任务拆分 — 低代码设计器统一改造（P1 物料协议阶段）

> 拆分顺序：基础结构 → 组件注册 → 协议层 → 桥接适配 → 测试验证
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> 每个任务必须精确到文件路径和函数签名

## 执行状态总览（2026-09-05 回填）

- [x] Task 1-11 全部完成（P1 物料协议阶段收口，105 个组件 spec 注册 + 桥接层 + 红线测试）
- [x] P2 提前落地：SpecPropertyPanel 属性引擎、UnifiedComponentPalette 统一面板（两侧接入）、列表嵌套放开（maxDepth=2）、spec 层 scope/alias 修正 —— 详见 spec.md 第 11 节「P2 提前落地」
- [ ] P2 剩余：PropertyPanelShell 外壳、DataSourceEditor 试点、组件树面板、CRUD propsSchema 声明化
- [ ] P3 未启动：统一拖拽引擎/渲染器/流式栅格迁移

## 前置条件

- [x] spec.md 已通过审阅并确认（统一基准决策 2026-09-02 用户确认）
- [ ] 待澄清项（第 9 节）部分已确认，剩余 6 项按建议值执行
- [x] 前端开发环境可用（`pnpm dev` 正常启动）

---

## Task 1: 创建 designer-core 基础结构与类型定义

- **目标**: 建立 `designer-core/spec/` 目录，定义 ComponentSpec 类型和注册表骨架
- **涉及文件**:
    - `components/lowcode-builder/designer-core/spec/types.js` — 新增，ComponentSpec / DataSourceSpec / PropsSchema 类型定义（JSDoc）
    - `components/lowcode-builder/designer-core/spec/registry.js` — 新增，组件注册表（Map + get/list/filter API）
    - `components/lowcode-builder/designer-core/index.js` — 新增，统一导出入口
- **关键签名**:
  ```js
  /**
   * @typedef {Object} ComponentSpec
   * @property {string} type - 统一类型名（唯一标识）
   * @property {string[]} aliases - 存量兼容别名
   * @property {string} scope - 'F' | 'L' | 'F+L'
   * @property {string} category - field | layout | business | page | media | widget
   * @property {string} group - 分组名（输入/选择/业务/布局/数据/操作/页面/媒体/导航）
   * @property {string} label - 显示名称
   * @property {string} desc - 功能描述
   * @property {Object} layout - { defaultSpan, minSpan, maxSpan }
   * @property {boolean} container - 是否容器
   * @property {number} maxDepth - 最大嵌套深度（容器时有效）
   * @property {string[]} accept - 容器可接受的子组件类型（空=全部）
   * @property {Object} propsSchema - JSON Schema 属性定义
   * @property {string[]} dataSources - 支持的数据源类型
   * @property {Object} print - { hidden, breakAvoid }
   * @property {Object} fieldDefaults - 字段组件默认值（fieldType/dataType/queryType 等）
   * @property {Object} meta - 额外元数据（unique/multiField/requireFields/onlyFor 等）
   */

  /** 注册组件 spec */
  export function registerComponent(spec) {}
  /** 批量注册 */
  export function registerComponents(specs) {}
  /** 按 type 或 alias 获取 spec */
  export function getComponentSpec(typeOrAlias) {}
  /** 按 scope 过滤列表 */
  export function listComponents(scope) {}
  /** 按 category + scope 过滤 */
  export function listComponentsByCategory(category, scope) {}
  ```

## Task 2: 注册字段组件 spec（33 个 · scope: F）

- **目标**: 将 33 个表单字段组件注册到统一注册表，含完整 propsSchema 和 fieldDefaults，同时补齐运行时别名
- **涉及文件**:
    - `components/lowcode-builder/designer-core/spec/field-components.js` — 新增，33 个字段组件 spec 定义
    - `components/lowcode-builder/designer-core/spec/registry.js` — 修改，导入并注册字段组件
- **关键签名**:
  ```js
  // 每个组件一个 spec 对象，示例：
  export const inputSpec = {
    type: 'input',
    aliases: ['inputNumber', 'text'],  // 运行时别名全量迁入
    scope: 'F',
    category: 'field',
    group: '输入',
    label: '输入框',
    desc: '单行文本录入',
    layout: { defaultSpan: 12 },
    container: false,
    propsSchema: {
      properties: {
        maxLength: { type: 'number', title: '最大长度' },
        prefix: { type: 'string', title: '前缀' },
        suffix: { type: 'string', title: '后缀' },
        clearable: { type: 'boolean', title: '可清空', default: true },
        showCount: { type: 'boolean', title: '显示字数', default: false },
      },
    },
    dataSources: [],
    print: { hidden: false, breakAvoid: false },
    fieldDefaults: { fieldType: 'TEXT', dataType: 'varchar', length: 128, queryType: 'like' },
  }
  // 其余 32 个组件类似，每个组件的 aliases 包含 componentTypeAlias + field-* 包装名
  // 如 number 的 aliases: ['inputNumber', 'integer', 'field-number']
  //    select 的 aliases: ['field-select']
  //    date 的 aliases: ['datePicker', 'field-date']
  ```
- **数据来源**: `fieldComponentCatalog.js` FIELD_COMPONENT_DEFAULTS + `ForgeFormDesigner.vue` componentTypeAlias + `page-schema.js` canvasComponentCatalog field-* 系列 + 物料清单第 1 节

## Task 3: 注册布局容器 spec（11 个 · scope: F+L）

- **目标**: 将 11 个布局容器注册到统一注册表，含 aliases 映射和嵌套规则（新增 formSectionTitle 独立组件）
- **涉及文件**:
    - `components/lowcode-builder/designer-core/spec/layout-components.js` — 新增
    - `components/lowcode-builder/designer-core/spec/registry.js` — 修改，导入并注册
- **关键签名**:
  ```js
  export const gridSpec = {
    type: 'grid',
    aliases: ['row', 'fcRow', 'grid-layout'],
    scope: 'F+L',
    category: 'layout',
    group: '布局',
    label: '栅格布局',
    desc: '单行多列栅格容器，每格独立 span',
    layout: { defaultSpan: 24 },
    container: true,
    maxDepth: 4,
    accept: [],
    propsSchema: {
      properties: {
        columns: { type: 'number', title: '总列数', default: 24, min: 1, max: 24 },
        gutter: { type: 'number', title: '列间距', default: 16 },
        rowGap: { type: 'number', title: '行距', default: 0 },
      },
    },
  }
  // formSectionTitle 作为独立组件（与 divider 分离）
  export const formSectionTitleSpec = {
    type: 'formSectionTitle',
    aliases: ['AiFormSectionTitle', 'aiFormSectionTitle', 'formSectionTitle',
              'FormSectionTitle', 'divider', 'elDivider'],
    scope: 'F+L',
    category: 'layout',
    group: '布局',
    label: '表单分隔线',
    desc: '表单分组标题与分隔线（与 divider 纯分隔线区分）',
  }
  // 其余 9 个：table, card, tabs, collapse, box, divider, spacer, space, groupTitle
  ```

## Task 4: 注册业务区块 spec（12 个 · scope F+L 为主）

- **目标**: 将 12 个业务区块注册到统一注册表，AiCrudPage 按列表侧 6 分区基准声明 propsSchema
- **涉及文件**:
    - `components/lowcode-builder/designer-core/spec/business-components.js` — 新增
    - `components/lowcode-builder/designer-core/spec/registry.js` — 修改
- **关键签名**:
  ```js
  export const aiCrudPageSpec = {
    type: 'AiCrudPage',
    aliases: ['crud', 'crudBlock', 'aiCrudPage'],  // 运行时别名
    scope: 'F+L',
    category: 'business',
    group: '数据',
    label: '数据列表',
    desc: '一体化 CRUD：筛选+表格+新增/编辑/删除弹窗',
    layout: { defaultSpan: 24 },
    container: false,
    meta: { unique: true },
    propsSchema: {
      properties: {
        // 六分区（以列表侧为基准）
        _section_queryTable: { type: 'section', title: '查询与列表字段' },
        searchLayout: { type: 'customEditor', editor: 'CrudSearchLayoutEditor' },
        tableColumns: { type: 'customEditor', editor: 'CrudTableColumnsEditor' },
        pagination: { type: 'boolean', title: '分页', default: true },
        striped: { type: 'boolean', title: '斑马纹', default: false },
        maxHeight: { type: 'number', title: '最大高度' },
        // ... 其余 5 个分区
      },
    },
    dataSources: ['managed'],  // 对象 CRUD 接口
  }
  ```
- **数据来源**: 物料清单第 3 节 + ListPageGridDesigner 内联属性面板配置

## Task 5: 注册页面与内容区块 + 媒体区块 + Zone 操作组件 spec（26 个）

- **目标**: 将 16 个页面区块 + 4 个媒体区块 + **6 个 Zone 操作组件**（审计补全）注册到统一注册表
- **涉及文件**:
    - `components/lowcode-builder/designer-core/spec/page-components.js` — 新增（16 个页面区块）
    - `components/lowcode-builder/designer-core/spec/media-components.js` — 新增（4 个媒体区块）
    - `components/lowcode-builder/designer-core/spec/zone-action-components.js` — **新增**（6 个 Zone 操作组件）
    - `components/lowcode-builder/designer-core/spec/registry.js` — 修改
- **关键签名**:
  ```js
  // action-button 合并表单侧 button
  export const actionButtonSpec = {
    type: 'action-button',
    aliases: ['button'],
    scope: 'F+L',
    category: 'page',
    group: '操作',
    label: '按钮',
    desc: '单个命令按钮',
  }

  // === 审计补全：6 个 Zone 操作组件（canvasComponentCatalog） ===
  export const querySetSpec = {
    type: 'query-set',
    aliases: [],
    scope: 'L',
    category: 'business',
    group: '操作',
    label: '查询集',
    desc: '选择查询字段、调整顺序',
    meta: { zones: ['search', 'table'], multiField: true },
  }
  export const customQuerySpec = {
    type: 'custom-query',
    aliases: [],
    scope: 'L',
    category: 'business',
    group: '操作',
    label: '自定义查询',
    desc: '高级查询入口',
    meta: { zones: ['search', 'table'] },
  }
  export const importButtonSpec = {
    type: 'import-button',
    aliases: [],
    scope: 'L',
    category: 'business',
    group: '操作',
    label: '导入',
    desc: 'Excel 批量导入',
    meta: { zones: ['table'] },
  }
  export const exportButtonSpec = {
    type: 'export-button',
    aliases: [],
    scope: 'L',
    category: 'business',
    group: '操作',
    label: '导出',
    desc: 'Excel 动态导出',
    meta: { zones: ['table'] },
  }
  export const addButtonSpec = {
    type: 'add-button',
    aliases: [],
    scope: 'L',
    category: 'business',
    group: '操作',
    label: '新增',
    desc: '打开新增表单',
    meta: { zones: ['table'] },
  }
  export const resetButtonSpec = {
    type: 'reset-button',
    aliases: [],
    scope: 'L',
    category: 'business',
    group: '操作',
    label: '重置',
    desc: '清空当前表单',
    meta: { zones: ['search'] },
  }
  ```

## Task 6: 注册页面挂件 spec（20 个 · scope: F+L）

- **目标**: 将 20 个页面挂件注册到统一注册表，消除"两边可配置集合不同"的问题
- **涉及文件**:
    - `components/lowcode-builder/designer-core/spec/widget-components.js` — 新增
    - `components/lowcode-builder/designer-core/spec/registry.js` — 修改
- **关键签名**:
  ```js
  // 挂件 spec 示例（含数据源声明）
  export const richTextSpec = {
    type: 'rich-text',
    aliases: [],
    scope: 'F+L',
    category: 'widget',
    group: '内容',
    label: '富文本框',
    desc: '带工具栏的富文本编辑器/展示',
    layout: { defaultSpan: 16 },
    propsSchema: {
      properties: {
        title: { type: 'string', title: '标题' },
        content: { type: 'string', title: '内容', format: 'html' },
        editorMode: { type: 'select', title: '编辑模式', options: ['visual', 'source'], default: 'visual' },
        toolbarMode: { type: 'select', title: '工具栏模式', options: ['default', 'minimal', 'full'] },
        readonly: { type: 'boolean', title: '只读', default: false },
        minHeight: { type: 'number', title: '最小高度', default: 180 },
      },
    },
    dataSources: ['static', 'context', 'remote'],  // 补齐列表侧数据源配置
  }
  // transfer 双重身份：字段+挂件，scope F+L
  export const transferSpec = {
    type: 'transfer',
    aliases: [],
    scope: 'F+L',
    category: 'widget',  // 双重身份，widget 为主
    // ... fieldDefaults 保留字段属性
  }
  ```
- **数据来源**: page-widget-schema.js `pageWidgetCatalog` + `createPageWidgetDefaultProps`

## Task 7: 创建 DataSourceSpec 与统一拖拽协议

- **目标**: 定义 DataSourceSpec 七类源规范和统一拖拽 MIME 协议
- **涉及文件**:
    - `components/lowcode-builder/designer-core/spec/data-source-spec.js` — 新增，DataSourceSpec + DataSourceKind 定义
    - `components/lowcode-builder/designer-core/dnd/protocols.js` — 新增，统一 MIME + payload 规范
    - `components/lowcode-builder/designer-core/index.js` — 修改，导出新增模块
- **关键签名**:
  ```js
  // data-source-spec.js
  export const DATA_SOURCE_KINDS = [
    { kind: 'static',   label: '静态数据' },
    { kind: 'dict',     label: '数据字典' },
    { kind: 'managed',  label: '受管 API' },
    { kind: 'remote',   label: '自定义接口' },
    { kind: 'context',  label: '当前记录' },
    { kind: 'relation', label: '子表明细' },
    { kind: 'builtin',  label: '系统内置' },
  ]

  // protocols.js
  export const DESIGNER_MIME = 'application/x-forge-designer'
  export function createDragPayload(spec, context = {}) {}
  export function parseDragPayload(dataTransfer) {}
  ```

## Task 8: 创建桥接层 — 兼容现有四处物料消费方

- **目标**: 让现有 ForgeFieldShelf / ListPageGridDesigner / ForgePropertyPanel / LowcodePageBuilder 不改代码也能从统一注册表获取数据
- **涉及文件**:
    - `components/lowcode-builder/designer-core/spec/bridge.js` — 新增，桥接函数
    - `views/app-center/components/designer/form-first/fieldComponentCatalog.js` — 修改，导出桥接包装
    - `components/lowcode-builder/shared/page-widget-schema.js` — 修改，导出桥接包装
    - `components/lowcode-builder/page/page-schema.js` — 修改，canvasComponentCatalog 导出桥接包装
- **关键签名**:
  ```js
  // bridge.js
  /** 从统一注册表生成 FIELD_COMPONENT_PALETTE_GROUPS 兼容格式 */
  export function toFieldPaletteGroups() {}
  /** 从统一注册表生成 FIELD_COMPONENT_DEFAULTS 兼容格式 */
  export function toFieldComponentDefaults() {}
  /** 从统一注册表生成 pageWidgetCatalog 兼容格式 */
  export function toPageWidgetCatalog() {}
  /** 从统一注册表生成 pageWidgetComponentKeys */
  export function toPageWidgetComponentKeys() {}
  /** 从统一注册表生成 listPageBlockCatalog 兼容格式 */
  export function toListPageBlockCatalog() {}
  /** 从统一注册表生成 canvasComponentCatalog 兼容格式（21 项） */
  export function toCanvasComponentCatalog() {}
  /** 按 type 或 alias 解析为统一 spec */
  export function resolveComponentSpec(typeOrAlias) {}
  ```

## Task 9: 单元测试 — spec 完整性 + aliases 映射 + 桥接一致性

- **目标**: 验证 102 个组件全部注册、aliases 正确映射（含 30+ 运行时别名 + 14 个 field-* 包装）、桥接函数输出与现有数据一致
- **涉及文件**:
    - `components/lowcode-builder/designer-core/__tests__/registry.test.js` — 新增，注册表测试
    - `components/lowcode-builder/designer-core/__tests__/aliases.test.js` — 新增，别名映射测试
    - `components/lowcode-builder/designer-core/__tests__/bridge.test.js` — 新增，桥接一致性测试
    - `components/lowcode-builder/designer-core/__tests__/data-source.test.js` — 新增，数据源 spec 测试
- **关键签名**:
  ```js
  // registry.test.js
  describe('Component Registry', () => {
    test('should register all 102 components', () => {})
    test('should have unique type for each component', () => {})
    test('should filter by scope F/L/F+L', () => {})
    test('should filter by category', () => {})
    test('should include 6 zone action components (audit补全)', () => {})
  })

  // aliases.test.js
  describe('Aliases Mapping', () => {
    test('row/fcRow/grid-layout should resolve to grid', () => {})
    test('elCard should resolve to card', () => {})
    test('button should resolve to action-button', () => {})
    test('section-divider should resolve to groupTitle', () => {})
    test('AiFormSectionTitle/divider/elDivider should resolve to formSectionTitle', () => {})
    test('crud/crudBlock/aiCrudPage should resolve to AiCrudPage', () => {})
    test('inputNumber/integer should resolve to number', () => {})
    test('radioGroup should resolve to radio', () => {})
    test('checkboxGroup should resolve to checkbox', () => {})
    test('datePicker should resolve to date', () => {})
    test('field-input should resolve to input', () => {})
    test('field-dict-select should resolve to dictSelect', () => {})
    test('field-image-upload should resolve to imageUpload', () => {})
  })

  // bridge.test.js
  describe('Bridge Compatibility', () => {
    test('toFieldPaletteGroups should match original 35 items', () => {})
    test('toPageWidgetCatalog should match original 21 items', () => {})
    test('toFieldComponentDefaults should match original defaults', () => {})
    test('toListPageBlockCatalog should match original ~42 items', () => {})
    test('toCanvasComponentCatalog should match original 21 items', () => {})
  })
  ```

## Task 10: 桥接层扩展 — 兼容 canvasComponentCatalog 消费方

- **目标**: 为 canvasComponentCatalog 创建桥接函数，确保旧版 LowcodePageBuilder zone 设计器从统一注册表获取数据
- **涉及文件**:
    - `components/lowcode-builder/designer-core/spec/bridge.js` — 修改，新增 canvas 桥接函数
    - `components/lowcode-builder/page/page-schema.js` — 修改，canvasComponentCatalog 改为从桥接层派生
- **关键签名**:
  ```js
  // bridge.js 新增
  /** 从统一注册表生成 canvasComponentCatalog 兼容格式（21 项） */
  export function toCanvasComponentCatalog() {}
  /** 从统一注册表生成 pageZoneCatalog 兼容格式 */
  export function toPageZoneCatalog() {}
  ```

## Task 11: 文档与收尾

- **目标**: 生成组件 spec 速查文档，更新 DESIGN.md 和 AGENTS.md 相关引用
- **涉及文件**:
    - `components/lowcode-builder/designer-core/spec/README.md` — 新增，102 个组件 spec 速查表
    - `forge-admin-ui/DESIGN.md` — 修改，新增 designer-core 引用说明
    - `code-copilot/changes/lowcode-designer-unification/execution-log.md` — 新增，执行日志
