# DictSelect 字典选择器

用于在表单中选择字典项，自动加载字典数据。

## 基础用法

```vue
<template>
  <!-- 单选 -->
  <DictSelect v-model:value="formData.status" dict-type="status" />
  
  <!-- 多选 -->
  <DictSelect v-model:value="formData.types" dict-type="type" multiple />
</template>
```

## Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| value | String/Number/Array | null | v-model 绑定值 |
| dictType | String | - | 字典类型（必填） |
| placeholder | String | '请选择' | 占位符 |
| disabled | Boolean | false | 禁用 |
| clearable | Boolean | true | 可清空 |
| filterable | Boolean | true | 可搜索 |
| multiple | Boolean | false | 多选 |

## 示例

```vue
<script setup>
import { ref } from 'vue'
import DictSelect from '@/components/DictSelect.vue'

const formData = ref({
  status: '',
  types: []
})
</script>

<template>
  <n-form :model="formData">
    <n-form-item label="状态">
      <DictSelect v-model:value="formData.status" dict-type="sys_status" />
    </n-form-item>
    
    <n-form-item label="类型">
      <DictSelect 
        v-model:value="formData.types" 
        dict-type="sys_type" 
        multiple 
      />
    </n-form-item>
  </n-form>
</template>
```