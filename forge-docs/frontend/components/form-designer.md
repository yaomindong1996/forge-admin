# FormDesigner 表单设计器

可视化拖拽表单构建器，支持通过拖拽组件到画布来快速生成表单。

## 使用方式

```vue
<template>
  <FormDesigner v-model="formSchema" @save="onSave" />
</template>

<script setup>
import { ref } from 'vue'
import FormDesigner from '@/components/form-designer/FormDesigner.vue'

const formSchema = ref([])

function onSave(schema) {
  console.log('表单 Schema:', schema)
}
</script>
```

## Props

| 属性 | 类型 | 说明 |
|------|------|------|
| `modelValue` | Array | 表单字段 Schema 数组（v-model） |
| `initialSchema` | Array | 初始 Schema（用于加载已有表单） |

## Events

| 事件 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `(schema: Array)` | Schema 变更 |
| `save` | `(schema: Array)` | 保存表单 |
| `cancel` | - | 取消编辑 |

## 暴露方法

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `getFormSchema()` | `Array` | 获取当前表单 Schema |
| `setFormSchema(schema)` | - | 设置表单 Schema |

## 支持的组件类型

### 基础组件（10 个）

| 组件 | 类型值 | 说明 |
|------|--------|------|
| 输入框 | `input` | 文本输入 |
| 多行文本 | `textarea` | 多行文本输入 |
| 数字 | `number` | 数字输入 |
| 下拉选择 | `select` | 下拉选项 |
| 单选框 | `radio` | 单选按钮组 |
| 复选框 | `checkbox` | 多选按钮组 |
| 日期选择 | `date` | 日期选择器 |
| 时间选择 | `time` | 时间选择器 |
| 开关 | `switch` | 开关组件 |
| 滑块 | `slider` | 滑块组件 |

### 高级组件（6 个）

| 组件 | 类型值 | 说明 |
|------|--------|------|
| 上传 | `upload` | 文件上传 |
| 富文本 | `richText` | 富文本编辑器 |
| 级联选择 | `cascader` | 级联选择器 |
| 树选择 | `treeSelect` | 树形选择器 |
| 颜色选择 | `colorPicker` | 颜色选择器 |
| 评分 | `rate` | 评分组件 |

### 布局组件（3 个）

| 组件 | 类型值 | 说明 |
|------|--------|------|
| 分割线 | `divider` | 分割线 |
| 标题 | `title` | 标题 |
| 描述 | `description` | 描述文本 |

## 布局结构

三面板布局：

1. **左侧** — 组件面板（基础/高级/布局组件分类）
2. **中间** — 设计画布（支持拖拽排序，使用 `vuedraggable`）
3. **右侧** — 属性编辑器（编辑选中组件的属性）

## FormItemRender — 字段渲染器

负责根据 Schema 渲染单个表单字段。

```vue
<FormItemRender
  :item="fieldSchema"
  v-model="formData[fieldSchema.key]"
  :disabled="false"
/>
```

### Props

| 属性 | 类型 | 说明 |
|------|------|------|
| `item` | Object | 字段 Schema（必填） |
| `modelValue` | any | 字段值 |
| `disabled` | Boolean | 是否禁用 |
| `preview` | Boolean | 预览模式 |

### Events

| 事件 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `(value)` | 值变更 |
| `change` | `(value)` | 值变更 |

## FormPreview — 表单预览

从 Schema 渲染完整表单，支持验证和提交。

```vue
<template>
  <FormPreview
    :form-items="formSchema"
    @submit="onSubmit"
  />
</template>
```

### Props

| 属性 | 类型 | 说明 |
|------|------|------|
| `formItems` | Array | 表单 Schema |
| `schema` | Array | 备用 Schema |

### Events

| 事件 | 参数 | 说明 |
|------|------|------|
| `submit` | `(formData)` | 表单提交 |
| `change` | `(formData)` | 表单数据变更 |

### 暴露方法

| 方法 | 说明 |
|------|------|
| `getFormData()` | 获取表单数据 |
| `setFormData(data)` | 设置表单数据 |
| `validate()` | 表单验证 |
| `reset()` | 重置表单 |

## 依赖

- `vuedraggable`
- `naive-ui`
- `FormItemRender.vue`
- `FormPreview.vue`
