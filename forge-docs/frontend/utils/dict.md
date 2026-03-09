# 字典管理

字典数据加载和缓存管理。

## useDict Composable

```js
import { useDict } from '@/composables/useDict'

// 加载多个字典
const { dict, loading, reload, getLabel, getDict } = useDict('sys_status', 'sys_type')

// 访问字典数据
dict.value.sys_status  // [{ label: '启用', value: '1' }, ...]
dict.value.sys_type    // [{ label: '类型A', value: 'a' }, ...]

// 根据值获取标签
const label = getLabel('sys_status', '1')  // '启用'

// 根据值获取字典项
const item = getDict('sys_status', '1')    // { label: '启用', value: '1', ... }

// 重新加载字典
reload('sys_status')  // 重新加载指定字典
reload()              // 重新加载所有字典
```

## 工具函数

```js
import { getDictData, clearDictCache } from '@/composables/useDict'

// 获取字典数据（带缓存）
const list = await getDictData('sys_status')

// 强制重新加载
const list = await getDictData('sys_status', true)

// 清除缓存
clearDictCache('sys_status')  // 清除指定字典缓存
clearDictCache()              // 清除所有缓存
```

## 字典数据格式

```js
[
  {
    label: '启用',       // 显示标签
    value: '1',          // 字典值
    dictCode: 'xxx',     // 字典编码
    dictSort: 1,         // 排序
    listClass: 'success', // 标签样式
    status: 1            // 状态
  },
  // ...
]
```

## 在组件中使用

```vue
<script setup>
import { useDict } from '@/composables/useDict'
import DictTag from '@/components/DictTag.vue'

const { dict } = useDict('sys_status', 'sys_type')
</script>

<template>
  <!-- 使用 DictSelect 组件 -->
  <DictSelect v-model="form.status" dict-type="sys_status" />
  
  <!-- 使用 DictTag 组件 -->
  <DictTag dict-type="sys_status" :value="row.status" />
  
  <!-- 手动渲染 -->
  <n-select 
    v-model:value="form.type" 
    :options="dict.sys_type" 
  />
</template>
```