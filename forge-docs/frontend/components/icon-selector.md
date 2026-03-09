# IconSelector 图标选择器

图标选择组件，支持搜索和分类展示。

## 基础用法

```vue
<template>
  <IconSelector v-model:value="formData.icon" />
</template>

<script setup>
import { ref } from 'vue'

const formData = ref({
  icon: ''
})
</script>
```

## Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| value | String | '' | v-model 绑定值 |
| placeholder | String | '请选择图标' | 占位符 |
| disabled | Boolean | false | 禁用 |

## 事件

| 事件 | 参数 | 说明 |
|------|------|------|
| update:value | icon | 图标变化 |