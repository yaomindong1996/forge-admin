# AiForm 表单组件

通过 JSON Schema 配置动态渲染表单，支持多种字段类型和验证规则。

## 基础用法

```vue
<template>
  <AiForm
    :schema="formSchema"
    v-model:value="formData"
    :grid-cols="2"
    @submit="handleSubmit"
  />
</template>

<script setup>
import { ref } from 'vue'

const formData = ref({})

const formSchema = [
  { field: 'name', label: '姓名', type: 'input', required: true },
  { field: 'age', label: '年龄', type: 'number' },
  { field: 'status', label: '状态', type: 'select', options: [
    { label: '启用', value: 1 },
    { label: '禁用', value: 0 }
  ]}
]
</script>
```

## Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| schema | Array | [] | 表单字段配置 |
| value | Object | {} | 表单数据 (v-model) |
| gridCols | Number | 1 | 栅格列数 |
| labelPlacement | String | 'left' | 标签位置：left/top |
| labelWidth | String/Number | 'auto' | 标签宽度 |
| showActions | Boolean | true | 是否显示操作按钮 |
| enableCollapse | Boolean | false | 是否启用折叠 |
| maxVisibleFields | Number | 6 | 折叠时最大显示字段数 |

## Schema 字段配置

| 属性 | 类型 | 说明 |
|------|------|------|
| field | String | 字段名 |
| label | String | 标签文本 |
| type | String | 字段类型 |
| required | Boolean | 是否必填 |
| rules | Array | 验证规则 |
| options | Array | 选项（select/radio/checkbox） |
| defaultValue | Any | 默认值 |
| span | Number | 栅格占位 |
| vIf | Function/Boolean | 条件显示 |
| onChange | Function | 值变化回调 |

## 支持的字段类型

| 类型 | 说明 |
|------|------|
| input | 输入框 |
| textarea | 文本域 |
| number | 数字输入 |
| select | 下拉选择 |
| radio | 单选 |
| checkbox | 多选 |
| date | 日期 |
| datetime | 日期时间 |
| switch | 开关 |
| slot | 自定义插槽 |

## 方法

```js
// 验证表单
await formRef.value.validate()

// 重置验证
formRef.value.restoreValidation()

// 重置表单
formRef.value.reset()

// 获取表单数据
const data = formRef.value.getFormData()
```

## 事件

| 事件 | 参数 | 说明 |
|------|------|------|
| submit | formData | 提交表单 |
| reset | - | 重置表单 |
| cancel | - | 取消 |