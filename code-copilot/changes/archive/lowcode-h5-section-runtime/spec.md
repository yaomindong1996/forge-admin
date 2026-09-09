# H5 低代码运行时多区域单页改造

> status: complete
> created: 2026-08-14
> complexity: 🔴高

## 1. 背景与目标

当前 H5 运行时（`lowcode-runtime.vue`）的表单模式是"主表单卡片 + 子表卡片"的平铺布局。预售信息登记 PRD 原型图要求的是**多卡片单页 + pill 选择器 + 底部抽屉 + 固定底部操作栏**的移动端交互形态。

**核心差距对照：**

| PRD 原型图要求 | 当前运行时 | 差距 |
|---------------|-----------|------|
| 导购信息卡（只读展示导购姓名/工号/门店） | 主表单平铺 16 个字段，无分区 | 无卡片分区 |
| 收款方式 pill 按钮选择 | dictSelect 下拉选择 | 无 pill 组件 |
| 商品明细内联栅格（扫码+名称+数量紧凑排列） | 子表行内独立 LowcodeForm（2 列布局） | 布局过宽 |
| 操作日志底部抽屉展示 | 子表卡片内联展示（创建/编辑时也显示） | 无底部抽屉 |
| 底部固定操作栏（清空 + 提交） | 底部操作栏只有取消 + 保存 | 操作栏不可配置 |
| 提交按钮执行业务动作 submit_presale | 保存按钮调用 createLowcodeRecord | 无动作型底部按钮 |

**本次改造目标：**

- 在 `formDesignerSchema` 中新增可选的 `pageSections` 和 `bottomBar` 配置
- H5 运行时识别该配置后，按 section 渲染多卡片单页
- 新建 3 个移动端组件：`CardSection`、`PillSelect`、`BottomSheet`
- 底部操作栏支持自定义按钮，可触发业务动作
- 向后兼容：不配置 `pageSections` 的应用走现有渲染逻辑
- 预售种子 SQL 增加 `pageSections` 配置

## 2. 架构归属

`pageSections` 描述"应用表单如何分区展示"，归属 `formDesignerSchema`（与 `components`、`layout`、`settings` 同级），不放入业务对象字段定义，不新增数据库表。

```text
formDesignerSchema
  ├── schemaVersion          ← 现有
  ├── formKey                ← 现有
  ├── formName               ← 现有
  ├── layout                 ← 现有：表单布局参数
  ├── components[]           ← 现有：字段组件定义
  ├── settings.governance    ← 现有：fieldEvents / permission / offlineDraft
  ├── pageSections[]         ← 【新增】多区域分区配置
  └── bottomBar              ← 【新增】底部固定操作栏
```

运行链路：

```text
后端 getLowcodeRenderConfig(configKey)
  → 返回 options.formDesignerSchema（含 pageSections）
  → H5 运行时 hasPageSections 判断
  → PageSectionRenderer 按 section 渲染
  → CardSection / PillSelect / BottomSheet 组件
  → 字段事件 / 业务动作 / 保存机制完全复用现有链路
```

## 3. pageSections 配置协议

### 3.1 整体结构

```json
{
  "pageSections": [
    {
      "sectionId": "guide_info",
      "sectionType": "card",
      "title": "导购信息",
      "collapsible": false,
      "fields": ["salesUserName", "staffNo", "storeName"],
      "fieldOverrides": {},
      "visibleInModes": ["create", "edit", "detail"]
    },
    {
      "sectionId": "presale_items",
      "sectionType": "child_table",
      "relationKey": "presale_items",
      "title": "商品明细",
      "displayMode": "inline_grid",
      "visibleInModes": ["create", "edit", "detail"]
    }
  ],
  "bottomBar": {
    "actions": [
      { "type": "reset", "label": "清空", "variant": "secondary" },
      { "type": "action", "actionCode": "submit_presale", "label": "提交", "variant": "primary", "displayCondition": "status == DRAFT" }
    ]
  }
}
```

### 3.2 section 字段定义

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sectionId` | string | 是 | 分区唯一标识 |
| `sectionType` | string | 是 | `card`（表单字段分区）或 `child_table`（子表分区） |
| `title` | string | 否 | 分区标题，空则不渲染标题栏 |
| `collapsible` | boolean | 否 | 是否可折叠，默认 `false` |
| `fields` | string[] | `card` 必填 | 引用 `components` 中的 `fieldCode`，按此顺序渲染 |
| `fieldOverrides` | object | 否 | 字段级覆盖，key 为 fieldCode，value 为属性覆盖 |
| `relationKey` | string | `child_table` 必填 | 引用 `masterDetailConfig.children` 中的 `relationKey` |
| `displayMode` | string | `child_table` 必填 | `inline_grid` / `card_list` / `bottom_sheet` |
| `visibleInModes` | string[] | 否 | 在哪些模式显示，默认全部。值：`create`/`edit`/`detail` |
| `collapsedByDefault` | boolean | 否 | `collapsible=true` 时默认是否折叠，默认 `false` |

### 3.3 sectionType 详解

**`card` — 表单字段分区**

从 `components` 数组中按 `fields` 列出的 fieldCode 过滤字段，用 `LowcodeForm` 渲染。`fieldOverrides` 可覆盖组件类型（如把 `dictSelect` 改为 `pillSelect`）和 props。

**`child_table` — 子表分区**

从 `masterDetailConfig.children` 中按 `relationKey` 找到子表配置，渲染子表数据。`displayMode` 控制渲染形态：

| displayMode | 说明 | 适用场景 |
|-------------|------|---------|
| `inline_grid` | 内联可编辑，紧凑栅格布局，每行可扫码新增 | 商品明细 |
| `card_list` | 每行一个卡片，只读或可编辑 | 操作日志（创建时不显示） |
| `bottom_sheet` | 默认收起，点击按钮从底部滑入展示 | 操作日志（编辑/详情时） |

### 3.4 bottomBar 配置

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `actions` | array | 是 | 底部按钮列表，按顺序从左到右排列 |
| `actions[].type` | string | 是 | `save` / `reset` / `action` / `cancel` |
| `actions[].actionCode` | string | `type=action` 时必填 | 引用业务动作编码 |
| `actions[].label` | string | 是 | 按钮文案 |
| `actions[].variant` | string | 否 | `primary` / `secondary` / `danger`，默认 `secondary` |
| `actions[].displayCondition` | string | 否 | 显示条件表达式，如 `status == DRAFT` |
| `actions[].confirmText` | string | 否 | 点击前确认弹窗文案 |
| `actions[].successMessage` | string | 否 | 成功提示文案 |

### 3.5 fieldOverrides 支持的覆盖项

```json
{
  "payMethod": {
    "componentKey": "pillSelect",
    "props": {
      "clearable": false
    }
  }
}
```

| 覆盖项 | 说明 |
|--------|------|
| `componentKey` | 覆盖组件类型，当前仅支持 `pillSelect`（替代 `dictSelect`） |
| `props.*` | 覆盖该字段的 props 属性，与 `components[].props` 合并 |

## 4. 实现阶段

### 阶段 1：新建移动端组件

#### 4.1 CardSection.vue

**文件**：`forge-h5-ui/src/components/lowcode/CardSection.vue`

**职责**：带标题栏的卡片容器，可选折叠。复用现有 `.runtime-form-card` 样式风格。

**Props**：

| 名称 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `title` | string | `''` | 卡片标题 |
| `collapsible` | boolean | `false` | 是否可折叠 |
| `collapsedByDefault` | boolean | `false` | 默认是否折叠 |
| `visible` | boolean | `true` | 是否显示 |

**实现要点**：

- 用 `ref(false)` 管理 `collapsed` 状态，初始值取 `collapsedByDefault`
- `collapsible === true` 时标题栏可点击切换 `collapsed`
- `collapsed === true` 时内容区 `v-show="!collapsed"`
- 内容通过默认 slot 渲染
- 样式：白底圆角阴影，复用 `runtime-form-card` 的 `border-radius: 18rpx; box-shadow: 0 10rpx 28rpx rgba(15,23,42,.04)`

**完整代码**：

```vue
<template>
  <view v-if="visible" class="card-section">
    <view v-if="title || collapsible" class="card-section__head" @click="toggle">
      <view class="card-section__title-block">
        <text v-if="title" class="card-section__title">{{ title }}</text>
      </view>
      <text v-if="collapsible" class="card-section__toggle">{{ collapsed ? '展开' : '收起' }}</text>
    </view>
    <view v-show="!collapsed" class="card-section__body">
      <slot />
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  collapsible: { type: Boolean, default: false },
  collapsedByDefault: { type: Boolean, default: false },
  visible: { type: Boolean, default: true },
})

const collapsed = ref(props.collapsedByDefault)

function toggle() {
  if (props.collapsible) collapsed.value = !collapsed.value
}
</script>

<style lang="scss" scoped>
.card-section {
  margin-bottom: 24rpx;
  padding: 26rpx;
  border: 1rpx solid #e7edf5;
  border-radius: 18rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, .04);
}
.card-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 18rpx;
}
.card-section__title-block { display: flex; flex-direction: column; gap: 6rpx; }
.card-section__title { color: var(--text-strong, #1e293b); font-size: 30rpx; font-weight: 850; }
.card-section__toggle { flex: 0 0 auto; color: #2563eb; font-size: 22rpx; font-weight: 700; }
.card-section__body { padding: 0; }
</style>
```

#### 4.2 PillSelect.vue

**文件**：`forge-h5-ui/src/components/lowcode/PillSelect.vue`

**职责**：药丸按钮组选择器，替代移动端下拉选择。水平排列可自动换行。

**Props**：

| 名称 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `modelValue` | string/number | `''` | 当前选中值 |
| `options` | array | `[]` | 选项列表，每项 `{ label, value }` |
| `clearable` | boolean | `true` | 是否可清空（取消选中） |
| `disabled` | boolean | `false` | 是否禁用 |

**Emits**：`update:modelValue`、`change`

**实现要点**：

- 遍历 `options`，每个选项渲染为 pill 按钮
- 选中态：蓝色背景白字；未选中：白底灰字灰边框
- `clearable === true` 时点击已选中项可取消选中（emit `''`）
- `clearable === false` 时必须有选中项（初始化时自动选中第一个或 defaultValue）
- 发射 `change` 事件供 LowcodeForm 的 field-event 机制使用

**完整代码**：

```vue
<template>
  <view class="pill-select" :class="{ 'pill-select--disabled': disabled }">
    <view
      v-for="option in options"
      :key="String(option.value)"
      class="pill-select__item"
      :class="{ 'pill-select__item--active': isSelected(option) }"
      @click="select(option)"
    >
      <text>{{ option.label }}</text>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  options: { type: Array, default: () => [] },
  clearable: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'change'])

function isSelected(option) {
  return String(props.modelValue) === String(option.value)
}

function select(option) {
  if (props.disabled) return
  if (isSelected(option) && props.clearable) {
    emit('update:modelValue', '')
    emit('change', '')
  } else if (!isSelected(option)) {
    emit('update:modelValue', option.value)
    emit('change', option.value)
  }
}
</script>

<style lang="scss" scoped>
.pill-select {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}
.pill-select--disabled { opacity: 0.5; }
.pill-select__item {
  padding: 12rpx 28rpx;
  border: 1rpx solid #d1d9e6;
  border-radius: 999rpx;
  color: #475569;
  font-size: 25rpx;
  background: #fff;
}
.pill-select__item--active {
  border-color: #2563eb;
  color: #fff;
  background: #2563eb;
}
</style>
```

#### 4.3 BottomSheet.vue

**文件**：`forge-h5-ui/src/components/lowcode/BottomSheet.vue`

**职责**：从底部滑入的半屏面板。**复用现有 `AiPopupSheet` 组件**，仅做薄封装增加低代码约定。

**实现要点**：

- 直接使用 `AiPopupSheet` 组件，`placement="bottom"` 固定从底部滑入
- Props 透传：`visible`、`title`、`maxHeight`
- 关闭通过 `@close` emit
- 内容通过默认 slot 渲染

**完整代码**：

```vue
<template>
  <AiPopupSheet
    :model-value="visible"
    placement="bottom"
    :title="title"
    :max-height="maxHeight"
    :mask="true"
    :round="true"
    :show-handle="true"
    :show-close="true"
    :scroll="true"
    @update:model-value="$emit('update:visible', $event)"
  >
    <slot />
  </AiPopupSheet>
</template>

<script setup>
import AiPopupSheet from '@/components/AiPopupSheet.vue'

defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  maxHeight: { type: String, default: '60vh' },
})

defineEmits(['update:visible'])
</script>
```

---

### 阶段 2：扩展 LowcodeField 支持新组件类型

**文件**：`forge-h5-ui/src/components/lowcode/LowcodeField.vue`

在现有模板的 `<AiSelect v-else-if="field.type === 'select' || field.type === 'dictSelect'">` 分支之后，增加 pillSelect 分支：

```vue
<!-- 在 AiSelect 分支之后增加 -->
<PillSelect
  v-else-if="field.type === 'pillSelect'"
  :model-value="modelValue"
  :options="options"
  :clearable="field.props?.clearable !== false"
  :disabled="disabled"
  @update:model-value="updateValue"
  @change="emit('change', $event)"
/>
```

在 `<script setup>` 中导入组件：

```js
import PillSelect from './PillSelect.vue'
```

在 readonly 分支中增加 pillSelect 的只读展示（复用 dictSelect 的只读逻辑）：

```js
// displayValue computed 中增加：
if (props.field.type === 'pillSelect') {
  return props.options.find(item => String(item.value) === String(value))?.label || value || '-'
}
```

在 `LowcodeForm.vue` 的 `fieldOptions` 中增加 pillSelect 的字典选项获取：

```js
// 现有 fieldOptions 函数中：
if (field.type === 'pillSelect') return props.dictOptions[field.dictType || field.props?.dictType] || []
```

---

### 阶段 3：新建 PageSectionRenderer 组件

**文件**：`forge-h5-ui/src/components/lowcode/PageSectionRenderer.vue`

**职责**：遍历 `pageSections` 配置，按 sectionType 渲染 CardSection 或子表区域，并渲染底部操作栏。

**Props**：

| 名称 | 类型 | 说明 |
|------|------|------|
| `sections` | array | pageSections 配置 |
| `mainFields` | array | 主表单字段（从 components 规范化） |
| `mainData` | object | 主表数据 |
| `children` | array | visibleChildren 配置（normalizeChildrenConfig 结果） |
| `childData` | object | 子表数据 |
| `mode` | string | create / edit / detail |
| `dictOptions` | object | 字典选项 |
| `runtimeContext` | object | 运行时上下文 |
| `bottomBar` | object | 底部操作栏配置 |
| `mainFormRef` | object | 主表单 ref（用于 validate） |
| `childFormRefs` | Map | 子表单 ref 集合 |

**Emits**：`main-field-event`、`child-field-event`、`add-child-row`、`remove-child-row`、`set-child-form-ref`、`bottom-action`

**实现要点**：

1. **过滤可见 section**：按 `visibleInModes` 过滤当前模式下的 section
2. **card 类型渲染**：从 `mainFields` 中按 `section.fields` 过滤，应用 `fieldOverrides`，用 `CardSection` 包裹 `LowcodeForm`
3. **child_table 类型渲染**：从 `children` 中按 `relationKey` 找到子表配置，按 `displayMode` 渲染
4. **底部操作栏渲染**：固定定位，按 `bottomBar.actions` 渲染按钮

**完整代码**：

```vue
<template>
  <view class="section-runtime">
    <!-- 渲染各 section -->
    <template v-for="section in visibleSections" :key="section.sectionId">
      <!-- card 类型：表单字段分区 -->
      <CardSection
        v-if="section.sectionType === 'card'"
        :title="section.title"
        :collapsible="section.collapsible || false"
        :collapsed-by-default="section.collapsedByDefault || false"
      >
        <LowcodeForm
          :ref="el => setMainFormRef(el, section)"
          :fields="resolveSectionFields(section)"
          :data="mainData"
          :dict-options="dictOptions"
          :readonly="mode === 'detail'"
          :context="runtimeContext"
          @field-event="payload => emit('main-field-event', payload)"
        />
      </CardSection>

      <!-- child_table 类型：inline_grid -->
      <CardSection
        v-else-if="section.sectionType === 'child_table' && section.displayMode === 'inline_grid'"
        :title="section.title"
      >
        <view class="section-child-head">
          <text class="section-child-count">{{ childRows(section).length }} 条</text>
          <AiButton v-if="mode !== 'detail' && childConfig(section)?.readonly !== true" size="sm" variant="secondary" @click="emit('add-child-row', childConfig(section))">添加</AiButton>
        </view>
        <view v-if="childRows(section).length" class="section-child-list">
          <view v-for="(row, rowIndex) in childRows(section)" :key="String(row.id || rowIndex)" class="section-child-row">
            <view class="section-child-row__head">
              <text class="section-child-row__title">第 {{ rowIndex + 1 }} 条</text>
              <AiButton v-if="mode !== 'detail' && childConfig(section)?.readonly !== true" size="sm" variant="danger" @click="emit('remove-child-row', { child: childConfig(section), index: rowIndex })">删除</AiButton>
            </view>
            <LowcodeForm
              :ref="el => emit('set-child-form-ref', { child: childConfig(section), row, rowIndex, instance: el })"
              :fields="childConfig(section).fields"
              :data="row"
              :dict-options="dictOptions"
              :current-children="childData"
              :readonly="mode === 'detail' || childConfig(section)?.readonly === true"
              :context="runtimeContext"
              @field-event="payload => emit('child-field-event', { child: childConfig(section), row, payload })"
            />
          </view>
        </view>
        <view v-else class="section-child-empty">暂无明细</view>
      </CardSection>

      <!-- child_table 类型：bottom_sheet -->
      <CardSection
        v-else-if="section.sectionType === 'child_table' && section.displayMode === 'bottom_sheet'"
        :title="section.title"
      >
        <view class="section-bottom-sheet-trigger" @click="openSheet(section.sectionId)">
          <text>{{ childRows(section).length }} 条记录</text>
          <text class="section-bottom-sheet-arrow">查看</text>
        </view>
      </CardSection>

      <!-- child_table 类型：card_list -->
      <CardSection
        v-else-if="section.sectionType === 'child_table' && section.displayMode === 'card_list'"
        :title="section.title"
      >
        <view v-if="childRows(section).length" class="section-card-list">
          <view v-for="(row, rowIndex) in childRows(section)" :key="String(row.id || rowIndex)" class="section-card-item">
            <view v-for="field in childConfig(section).fields.filter(f => f.listVisible !== false)" :key="field.field" class="section-card-item__field">
              <text class="section-card-item__label">{{ field.label }}</text>
              <text class="section-card-item__value">{{ formatFieldValue(row[field.field], field) }}</text>
            </view>
          </view>
        </view>
        <view v-else class="section-child-empty">暂无记录</view>
      </CardSection>
    </template>

    <!-- 底部抽屉 -->
    <BottomSheet
      v-if="activeSheetSection"
      v-model:visible="sheetVisible"
      :title="activeSheetSection.title"
      max-height="60vh"
    >
      <view v-if="childRows(activeSheetSection).length" class="section-sheet-list">
        <view v-for="(row, rowIndex) in childRows(activeSheetSection)" :key="String(row.id || rowIndex)" class="section-sheet-item">
          <view v-for="field in childConfig(activeSheetSection).fields.filter(f => f.listVisible !== false)" :key="field.field" class="section-sheet-item__field">
            <text class="section-sheet-item__label">{{ field.label }}</text>
            <text class="section-sheet-item__value">{{ formatFieldValue(row[field.field], field) }}</text>
          </view>
        </view>
      </view>
      <AiEmpty v-else title="暂无记录" />
    </BottomSheet>

    <!-- 底部操作栏 -->
    <view v-if="visibleBottomActions.length && !sheetVisible" class="section-bottom-bar">
      <AiButton
        v-for="(action, index) in visibleBottomActions"
        :key="index"
        :variant="action.variant || 'secondary'"
        :loading="action._loading"
        @click="handleBottomAction(action)"
      >
        {{ action.label }}
      </AiButton>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import AiButton from '@/components/AiButton.vue'
import AiEmpty from '@/components/AiEmpty.vue'
import LowcodeForm from './LowcodeForm.vue'
import CardSection from './CardSection.vue'
import BottomSheet from './BottomSheet.vue'
import { resolveBottomBarActions } from '@/utils/lowcode-runtime'

const props = defineProps({
  sections: { type: Array, default: () => [] },
  mainFields: { type: Array, default: () => [] },
  mainData: { type: Object, default: () => ({}) },
  children: { type: Array, default: () => [] },
  childData: { type: Object, default: () => ({}) },
  mode: { type: String, default: 'create' },
  dictOptions: { type: Object, default: () => ({}) },
  runtimeContext: { type: Object, default: () => ({}) },
  bottomBar: { type: Object, default: () => null },
})

const emit = defineEmits(['main-field-event', 'child-field-event', 'add-child-row', 'remove-child-row', 'set-child-form-ref', 'set-main-form-ref', 'bottom-action'])

const sheetVisible = ref(false)
const activeSheetSectionId = ref('')

const visibleSections = computed(() =>
  props.sections.filter(section => {
    if (!section.visibleInModes || !section.visibleInModes.length) return true
    return section.visibleInModes.includes(props.mode)
  })
)

const visibleBottomActions = computed(() =>
  resolveBottomBarActions(props.bottomBar, props.mainData, props.mode)
)

const activeSheetSection = computed(() =>
  props.sections.find(section => section.sectionId === activeSheetSectionId.value)
)

function childConfig(section) {
  return props.children.find(child => child.relationKey === section.relationKey) || {}
}

function childRows(section) {
  const child = childConfig(section)
  if (!child) return []
  const key = child.modelCode || child.key
  return props.childData[key] || []
}

function resolveSectionFields(section) {
  if (!section.fields?.length) return props.mainFields
  const fieldMap = new Map(props.mainFields.map(f => [f.field, f]))
  return section.fields
    .map(code => {
      const field = fieldMap.get(code)
      if (!field) return null
      const override = section.fieldOverrides?.[code]
      return override ? { ...field, type: override.componentKey || field.type, props: { ...field.props, ...override.props } } : field
    })
    .filter(Boolean)
}

function setMainFormRef(el, section) {
  emit('set-main-form-ref', { el, section })
}

function openSheet(sectionId) {
  activeSheetSectionId.value = sectionId
  sheetVisible.value = true
}

function formatFieldValue(value, field) {
  if (value === undefined || value === null || value === '') return '-'
  if (field.type === 'dictSelect' || field.type === 'pillSelect') {
    const options = props.dictOptions[field.dictType || field.props?.dictType] || []
    return options.find(item => String(item.value) === String(value))?.label || value
  }
  return String(value)
}

function handleBottomAction(action) {
  emit('bottom-action', action)
}
</script>

<style lang="scss" scoped>
.section-runtime { padding-bottom: 140rpx; }
.section-child-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx; }
.section-child-count { color: #94a3b8; font-size: 22rpx; }
.section-child-list { display: flex; flex-direction: column; gap: 18rpx; }
.section-child-row { padding: 20rpx; border: 1rpx solid #eef2f7; border-radius: 16rpx; background: #fbfdff; }
.section-child-row__head { display: flex; align-items: center; justify-content: space-between; gap: 12rpx; margin-bottom: 14rpx; }
.section-child-row__title { color: #334155; font-size: 24rpx; font-weight: 700; }
.section-child-empty { padding: 30rpx 0; color: #94a3b8; font-size: 24rpx; text-align: center; }
.section-bottom-sheet-trigger { display: flex; align-items: center; justify-content: space-between; padding: 16rpx 0; }
.section-bottom-sheet-arrow { color: #2563eb; font-size: 22rpx; font-weight: 700; }
.section-card-list, .section-sheet-list { display: flex; flex-direction: column; gap: 18rpx; }
.section-card-item, .section-sheet-item { padding: 20rpx; border: 1rpx solid #eef2f7; border-radius: 16rpx; background: #fbfdff; }
.section-card-item__field, .section-sheet-item__field { display: flex; justify-content: space-between; gap: 12rpx; margin-bottom: 8rpx; }
.section-card-item__label, .section-sheet-item__label { color: #94a3b8; font-size: 22rpx; }
.section-card-item__value, .section-sheet-item__value { color: #334155; font-size: 24rpx; }
.section-bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  display: flex;
  gap: 12rpx;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(248, 250, 252, .97);
  border-top: 1rpx solid #e7edf5;
  backdrop-filter: blur(8px);
}
.section-bottom-bar > * { flex: 1; }
</style>
```

---

### 阶段 4：改造 lowcode-runtime.vue

**文件**：`forge-h5-ui/src/pages/lowcode-runtime.vue`

#### 4.1 引入 PageSectionRenderer

在 `<script setup>` 的 import 区域增加：

```js
import PageSectionRenderer from '@/components/lowcode/PageSectionRenderer.vue'
```

#### 4.2 新增 computed

```js
const formDesignerSchema = computed(() =>
  config.value?.options?.formDesignerSchema || config.value?.options?.formDesignerSchemaJson
    ? parseJson(config.value.options.formDesignerSchema || config.value.options.formDesignerSchemaJson)
    : {}
)

const hasPageSections = computed(() =>
  Array.isArray(formDesignerSchema.value?.pageSections) &&
  formDesignerSchema.value.pageSections.length > 0
)

const pageSections = computed(() => formDesignerSchema.value?.pageSections || [])
const bottomBar = computed(() => formDesignerSchema.value?.bottomBar || null)
```

#### 4.3 模板增加 section 分支

在 `<template v-else>`（非 list 模式）中，现有 `runtime-form-card` 之前增加判断：

```vue
<template v-else>
  <!-- 新增：pageSections 模式 -->
  <PageSectionRenderer
    v-if="hasPageSections"
    :sections="pageSections"
    :main-fields="mainFields"
    :main-data="mainData"
    :children="visibleChildren"
    :child-data="childData"
    :mode="mode"
    :dict-options="dictOptions"
    :runtime-context="runtimeContext"
    :bottom-bar="bottomBar"
    @main-field-event="handleMainFieldEvent"
    @child-field-event="payload => handleChildFieldEvent(payload.child, payload.row, payload.payload)"
    @add-child-row="addChildRow"
    @remove-child-row="payload => removeChildRow(payload.child, payload.index)"
    @set-child-form-ref="payload => setChildFormRef(payload.child, payload.row, payload.rowIndex, payload.instance)"
    @set-main-form-ref="payload => mainFormRef.value = payload.el"
    @bottom-action="handleBottomAction"
  />

  <!-- 现有逻辑保持不变 -->
  <template v-else>
    <view class="runtime-form-card">
      ...（现有代码不动）
    </view>
  </template>
</template>
```

#### 4.4 新增 handleBottomAction 方法

```js
async function handleBottomAction(action) {
  // 确认弹窗
  if (action.confirmText) {
    const confirmed = await new Promise(resolve =>
      uni.showModal({
        title: '确认',
        content: action.confirmText,
        success: result => resolve(result.confirm),
      })
    )
    if (!confirmed) return
  }

  switch (action.type) {
    case 'save':
      await save()
      break
    case 'reset':
      Object.keys(mainData).forEach(key => delete mainData[key])
      Object.keys(childData).forEach(key => delete childData[key])
      initializeForm()
      toast('已清空', { type: 'info' })
      break
    case 'action':
      await runAction({ actionCode: action.actionCode }, mainData)
      break
    case 'cancel':
      goList()
      break
  }
}
```

#### 4.5 移除现有 footer 的冗余

当 `hasPageSections === true` 时，不渲染现有的 `runtime-footer-actions`（因为底部操作栏由 PageSectionRenderer 渲染）。在现有 footer 外层加条件：

```vue
<view v-if="mode !== 'detail' && !hasPageSections" class="runtime-footer-actions">
  <AiButton variant="secondary" @click="goList">取消</AiButton>
  <AiButton :loading="saving" @click="save">保存</AiButton>
</view>
<view v-else-if="mode === 'detail' && !hasPageSections" class="runtime-footer-actions">
  <AiButton variant="secondary" @click="goList">返回列表</AiButton>
  <AiButton v-if="canEdit" @click="openEdit">编辑</AiButton>
</view>
```

---

### 阶段 5：更新预售种子 SQL

**新建文件**：`forge-server/db/migration/V1.0.109__add_presale_h5_page_sections.sql`

在 `@ps_form_schema` 变量中增加 `pageSections` 和 `bottomBar`，然后 UPDATE 到 `ai_business_object.designer_options` 和 `ai_crud_config.options` 中。

**SQL 脚本**：

```sql
-- 预售信息登记：增加多区域分区配置和底部操作栏

-- 1. 定义 pageSections 配置
SET @ps_page_sections := JSON_ARRAY(
  JSON_OBJECT(
    'sectionId', 'guide_info',
    'sectionType', 'card',
    'title', '导购信息',
    'collapsible', false,
    'fields', JSON_ARRAY('salesUserName', 'staffNo', 'storeName')
  ),
  JSON_OBJECT(
    'sectionId', 'member_info',
    'sectionType', 'card',
    'title', '会员信息',
    'collapsible', false,
    'fields', JSON_ARRAY('memberPhone', 'memberId', 'memberName')
  ),
  JSON_OBJECT(
    'sectionId', 'payment',
    'sectionType', 'card',
    'title', '收款信息',
    'collapsible', false,
    'fields', JSON_ARRAY('payMethod', 'staticPaymentNo', 'staticPaymentInfo', 'cashAmount'),
    'fieldOverrides', JSON_OBJECT(
      'payMethod', JSON_OBJECT('componentKey', 'pillSelect', 'props', JSON_OBJECT('clearable', false))
    )
  ),
  JSON_OBJECT(
    'sectionId', 'remark',
    'sectionType', 'card',
    'title', '备注',
    'collapsible', false,
    'fields', JSON_ARRAY('remark')
  ),
  JSON_OBJECT(
    'sectionId', 'presale_items',
    'sectionType', 'child_table',
    'relationKey', 'presale_items',
    'title', '商品明细',
    'displayMode', 'inline_grid',
    'visibleInModes', JSON_ARRAY('create', 'edit', 'detail')
  ),
  JSON_OBJECT(
    'sectionId', 'operation_logs',
    'sectionType', 'child_table',
    'relationKey', 'operation_logs',
    'title', '操作日志',
    'displayMode', 'bottom_sheet',
    'visibleInModes', JSON_ARRAY('edit', 'detail')
  )
);

-- 2. 定义 bottomBar 配置
SET @ps_bottom_bar := JSON_OBJECT(
  'actions', JSON_ARRAY(
    JSON_OBJECT('type', 'reset', 'label', '清空', 'variant', 'secondary'),
    JSON_OBJECT(
      'type', 'action',
      'actionCode', 'submit_presale',
      'label', '提交',
      'variant', 'primary',
      'displayCondition', 'status == DRAFT',
      'confirmText', '确认提交当前预售单？',
      'successMessage', '预售单已提交'
    )
  )
);

-- 3. 更新业务对象 designer_options 中的 formDesignerSchema
UPDATE ai_business_object
SET designer_options = JSON_SET(
      COALESCE(designer_options, JSON_OBJECT()),
      '$.formDesignerSchema.pageSections', JSON_EXTRACT(@ps_page_sections, '$'),
      '$.formDesignerSchema.bottomBar', JSON_EXTRACT(@ps_bottom_bar, '$')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'PRESALE_REGISTRATION'
  AND object_code = 'PS_PRESALE_ORDER'
  AND del_flag = 0
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options, '$.seedKey')) = 'presale-registration-v1';

-- 4. 更新 crud_config options 中的 formDesignerSchema
UPDATE ai_crud_config
SET options = JSON_SET(
      COALESCE(options, JSON_OBJECT()),
      '$.formDesignerSchema.pageSections', JSON_EXTRACT(@ps_page_sections, '$'),
      '$.formDesignerSchema.bottomBar', JSON_EXTRACT(@ps_bottom_bar, '$')
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND config_key = 'ps_presale_order'
  AND publish_status = 'PUBLISHED';
```

---

### 阶段 6：验证

#### 6.1 向后兼容验证

- 打开任意不带 `pageSections` 的低代码应用（如其他 CRUD 页面）
- 表单模式渲染正常，主表单卡片 + 子表卡片 + 底部保存/取消按钮

#### 6.2 预售新建页验证

- 打开预售登记新建页（`mode=create`）
- 看到分区渲染：导购信息卡 → 会员信息卡 → 收款信息卡（pill 选择器）→ 备注卡 → 商品明细子表
- 导购信息字段在表单加载时自动回填（FORM_LOAD 事件触发）
- 收款方式显示为 pill 按钮，点击切换 静态码/现金
- 选静态码时显示静态码单号 + 收款信息；选现金时显示现金金额
- 会员手机号失焦时触发查询，回填会员ID/姓名
- 底部操作栏显示"清空"和"提交"按钮
- 操作日志 section 不显示（visibleInModes 不含 create）

#### 6.3 预售编辑/详情页验证

- 打开预售编辑页（`mode=edit`）
- 操作日志 section 显示为 bottom_sheet 触发器（点击展开底部抽屉）
- 提交按钮在状态为 DRAFT 时显示，SUBMITTED 后隐藏（displayCondition）
- 商品明细子表可扫码新增、手动输入查询

#### 6.4 保存验证

- 新建预售单 → 填写 → 点击"提交" → 执行 submit_presale 动作 → 状态变为 SUBMITTED
- 列表页看到新记录
- 点击"清空" → 表单重置 → 重新填写

#### 6.5 子表操作验证

- 编辑已提交预售单 → 商品明细每行显示"提货"和"退货"按钮
- 点击提货 → 输入数量 → 执行 record_pickup 动作 → 提货数量更新
- 操作日志底部抽屉刷新显示新日志

## 5. 不在本次范围

- 管理端表单设计器的可视化 section 编辑（后续 Phase 2）
- 应用页面设计器（application-runtime.vue）产出 H5 pageSections 配置（后续 Phase 2）
- 面板迁移（对象瘦身+应用增重）（P2）
- 触发器 WEBHOOK 落地（P3）
- 离线草稿在 section 模式下的适配（后续迭代）

## 6. 风险与回滚

| 风险 | 影响 | 缓解 |
|------|------|------|
| pageSections 配置不存在时渲染异常 | 所有低代码应用受影响 | hasPageSections 判断 + 向后兼容分支 |
| PageSectionRenderer ref 管理与现有 childFormRefs 冲突 | 子表校验失败 | 保持 childFormRefs 的 key 规则不变，通过事件透传 |
| displayCondition 表达式注入 | 安全风险 | 只支持 `field == value` 和 `field != value` 两种模式 |
| 底部操作栏 fixed 定位遮挡内容 | 滚动不到底部 | section-runtime 加 `padding-bottom: 140rpx` |

回滚方式：删除 `pageSections` 和 `bottomBar` 配置；已执行的 Flyway 脚本不得修改或回退文件，需要通过后续迁移移除配置。
