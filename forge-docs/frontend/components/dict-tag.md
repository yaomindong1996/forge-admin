# DictTag 字典标签

用于显示字典值对应的标签，自动匹配字典项并渲染为标签。

## 基础用法

```vue
<template>
  <!-- 使用字典类型 -->
  <DictTag dict-type="status" :value="row.status" />
  
  <!-- 使用选项列表 -->
  <DictTag :options="dict.status" :value="row.status" />
  
  <!-- 指定标签类型 -->
  <DictTag dict-type="status" :value="row.status" type="success" />
</template>
```

## Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| options | Array | null | 字典选项列表（优先） |
| dictType | String | '' | 字典类型 |
| value | String/Number | '' | 字典值 |
| type | String | '' | 标签类型：default/success/warning/error/info |
| size | String | 'small' | 标签尺寸 |
| round | Boolean | false | 圆角 |
| bordered | Boolean | true | 边框 |

## 标签类型映射

字典项的 `listClass` 字段自动映射为标签类型：

| listClass | 标签类型 |
|-----------|----------|
| default | 普通文本（不显示标签） |
| success | 绿色标签 |
| info | 蓝色标签 |
| warning | 橙色标签 |
| error/danger | 红色标签 |

## 示例

```vue
<script setup>
import { useDict } from '@/composables/useDict'

const { dict } = useDict('sys_status')
</script>

<template>
  <n-data-table :columns="columns" :data="data" />
</template>

<script>
const columns = [
  { 
    prop: 'status', 
    label: '状态',
    render: (row) => h(DictTag, { 
      dictType: 'sys_status', 
      value: row.status 
    })
  }
]
</script>
```