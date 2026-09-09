# designer-core 属性面板迁移说明

> 本文档记录表单设计器属性面板（ForgePropertyPanel）从手写属性定义迁移到 spec 驱动（SpecPropertyPanel）的过程与后续扩展指南。

## 迁移背景

表单设计器和列表设计器原先各自维护一套属性编辑面板：

- 表单侧 `ForgePropertyPanel.vue`：手写 `buildNaivePropGroups` 函数（约 430 行）+ 手写抽屉模板（约 55 行）为每个 componentKey 硬编码 Naive UI 属性组
- 列表侧 `ListPageGridDesigner.vue`：已接入 `SpecPropertyPanel` 作为增量补充，通过 `SPEC_PANEL_EXCLUDED_PROPS` 排除已手写的属性

两侧属性配置范围、分组方式、交互完全不一致。

## 迁移内容（Phase 5）

### 已删除

| 内容 | 原位置 | 行数 |
|------|--------|------|
| `buildNaivePropGroups` 函数 | ForgePropertyPanel.vue | ~250 |
| `datePickerPropFields` / `timePickerPropFields` | 同上 | ~36 |
| `isDateLikeComponent` / `isTimeLikeComponent` | 同上 | ~6 |
| `group` / `prop` / `formatPropLabel` 辅助函数 | 同上 | ~18 |
| `propChineseLabels` / `groupChineseLabels` 中文映射 | 同上 | ~114 |
| `updateSelectedProp` / `updateJsonProp` / `resolvePropValue` / `normalizePropValue` | 同上 | ~42 |
| 抽屉手写属性组模板 | 同上 template 区 | ~55 |

### 已替换

"更多属性"抽屉内容改为：

```vue
<SpecPropertyPanel
  :block-type="selectedComponent.componentKey"
  :model-props="selectedComponent.props || {}"
  :exclude-keys="specPanelExcludedProps"
  @update:prop="handleSpecPropUpdate"
/>
```

### 已保留（表单特有业务面板）

- 标识（显示名称 / 组件类型切换 / 绑定字段 / 字段资产锁定提示）
- 栅格快捷配置（滑块 + 动态列 span 编辑）
- 字段组件折叠项（占位提示 / 默认值 / 选项管理 / 字典联动）
- 校验规则（required / pattern / unique）
- 可见性 / 事件规则 / 联动规则（RuntimeRulesEditor）
- 样式 tab、交互 tab
- 表单资产 / 表单属性 tab

### 排除属性策略

`specPanelExcludedProps` 计算属性（ForgePropertyPanel.vue）：

- `button`：排除 `text` / `type` / `size`（已在"按钮组件"折叠项配置，且 text 更新需同步 label）
- `dictSelect`：排除 `dictType`（字典类型由表单侧字典联动逻辑管理）

## 后续扩展指南

### 新增组件属性

1. 在 `designer-core/spec/field-components.js`（或对应分类文件）的 `propsSchema.properties` 中添加属性定义
2. 支持 `group`（分组名）/ `priority`（`common` 平铺 / `advanced` 折叠）/ `desc`（帮助提示）字段
3. 两个设计器自动获得该属性的编辑能力，无需改模板

### 属性分组约定

| 分组名 | priority | 展示方式 |
|--------|----------|----------|
| 输入行为 / 选择行为 | common | 平铺（两列网格） |
| 外观 | common | 平铺（两列网格） |
| 高级 | advanced | 折叠收纳（PropertyGroupCollapse） |
| 装饰 | advanced | 折叠收纳 |

### 列表侧排除表

`SPEC_PANEL_EXCLUDED_PROPS`（ListPageGridDesigner.vue）继续维护：block 特有业务属性（如 AiCrudPage 的 api 配置、grid-layout 的 cells 管理）保留手写面板，通用属性走 SpecPropertyPanel。

## 相关文件

- `designer-core/panel/SpecPropertyPanel.vue` — 统一属性面板引擎（分组 + 分层 + 搜索）
- `designer-core/panel/PropertyGroupCollapse.vue` — 高级属性分组折叠组件
- `designer-core/canvas/DesignerNodeOverlay.vue` — 统一画布节点操作条
- `designer-core/canvas/DesignerGridRenderer.vue` — 统一栅格渲染组件
- `designer-core/spec/registry.js` — 组件 spec 注册表
