# 字典组件使用指南

本系统提供了完整的字典数据管理方案，包括字典数据加载、缓存、显示和选择功能。

## 一、字典数据加载（useDict）

### 基本使用

```vue
<script setup>
import { useDict } from '@/composables/useDict'

// 加载单个字典
const { dict } = useDict('case_status')

// 加载多个字典
const { dict, loading, reload } = useDict('case_status', 'matter_type', 'notice_reason')
</script>

<template>
  <div v-if="!loading">
    <!-- 使用字典数据 -->
    <div v-for="item in dict.case_status" :key="item.value">
      {{ item.label }} - {{ item.value }}
    </div>
  </div>
</template>
```

### API 说明

#### useDict(...dictTypes)

**参数：**
- `dictTypes`: 字典类型列表（可变参数）

**返回值：**
```javascript
{
  dict: Ref<Object>,      // 字典数据对象，key 为字典类型，value 为字典列表
  loading: Ref<Boolean>,  // 加载状态
  reload: Function,       // 重新加载函数
  getLabel: Function,     // 根据值获取标签
  getDict: Function       // 根据值获取字典项
}
```

**示例：**
```javascript
const { dict, getLabel, getDict } = useDict('case_status')

// 获取标签
const label = getLabel('case_status', '1') // '待处理'

// 获取字典项
const item = getDict('case_status', '1')
// { label: '待处理', value: '1', ... }
```

### 字典数据格式

```javascript
[
  {
    label: '待处理',        // 字典标签
    value: '1',            // 字典值
    dictCode: 1,           // 字典编码
    dictSort: 1,           // 排序
    cssClass: '',          // CSS 类名
    listClass: 'default',  // 标签类型：default/success/info/warning/error
    isDefault: 0,          // 是否默认
    status: 1,             // 状态
    remark: '',            // 备注
    raw: { ... }           // 原始数据
  }
]
```

### 标签类型说明

`listClass` 字段用于控制 DictTag 组件的显示样式，支持以下类型：

- `default` - 默认样式（显示为普通文字，无标签样式）
- `success` - 成功样式（绿色标签）
- `info` - 信息样式（蓝色标签）
- `warning` - 警告样式（橙色标签）
- `error` - 错误样式（红色标签）

**特别说明：**
- 当 `listClass` 为 `default` 或未设置时，DictTag 会显示为普通文字，不带标签样式
- 如果需要强制显示标签样式，可以通过 `type` 属性指定：`<DictTag :options="dict.xxx" :value="row.xxx" type="default" />`

系统会自动兼容旧的命名方式：
- `primary` → `info`
- `danger` → `error`

## 二、字典标签显示（DictTag）

### 基本使用

```vue
<script setup>
import { useDict } from '@/composables/useDict'
import DictTag from '@/components/DictTag.vue'

const { dict } = useDict('case_status', 'matter_type')
</script>

<template>
  <!-- 方式1：传入 options -->
  <DictTag :options="dict.case_status" :value="row.status" />
  
  <!-- 方式2：传入 dictType（会自动加载） -->
  <DictTag dict-type="case_status" :value="row.status" />
  
  <!-- 自定义标签类型 -->
  <DictTag :options="dict.matter_type" :value="row.matterType" type="success" />
  
  <!-- 自定义尺寸和样式 -->
  <DictTag 
    :options="dict.notice_reason" 
    :value="row.reason" 
    size="medium"
    round
    :bordered="false"
  />
</template>
```

### Props

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| options | Array | null | 字典选项列表（优先使用） |
| dictType | String | '' | 字典类型（当 options 为空时使用） |
| value | String/Number | '' | 字典值 |
| type | String | '' | 标签类型（default/success/warning/error/info） |
| size | String | 'small' | 标签尺寸 |
| round | Boolean | false | 是否圆角 |
| bordered | Boolean | true | 是否显示边框 |
| closable | Boolean | false | 是否可关闭 |

### 在表格中使用

```vue
<script setup>
import { h } from 'vue'
import { useDict } from '@/composables/useDict'
import DictTag from '@/components/DictTag.vue'

const { dict } = useDict('case_status', 'matter_type')

const columns = [
  {
    prop: 'status',
    label: '状态',
    render: (row) => {
      return h(DictTag, {
        options: dict.value.case_status,
        value: row.status
      })
    }
  },
  {
    prop: 'matterType',
    label: '事项类型',
    render: (row) => {
      return h(DictTag, {
        options: dict.value.matter_type,
        value: row.matterType,
        type: 'success'
      })
    }
  }
]
</script>
```

## 三、字典选择器（DictSelect）

### 基本使用

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
  <!-- 单选 -->
  <DictSelect 
    v-model:value="formData.status" 
    dict-type="case_status"
    placeholder="请选择状态"
  />
  
  <!-- 多选 -->
  <DictSelect 
    v-model:value="formData.types" 
    dict-type="matter_type"
    multiple
    placeholder="请选择事项类型"
  />
</template>
```

### Props

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| value | String/Number/Array | null | v-model 绑定值 |
| dictType | String | - | 字典类型（必填） |
| placeholder | String | '请选择' | 占位符 |
| disabled | Boolean | false | 是否禁用 |
| clearable | Boolean | true | 是否可清空 |
| filterable | Boolean | true | 是否可搜索 |
| multiple | Boolean | false | 是否多选 |

### 在 AiForm 中使用

```vue
<script setup>
import { ref } from 'vue'
import { AiForm } from '@/components/ai-form'
import DictSelect from '@/components/DictSelect.vue'

const formData = ref({})

const schema = [
  {
    field: 'status',
    label: '状态',
    type: 'slot',
    slotName: 'status',
    rules: [{ required: true, message: '请选择状态' }]
  }
]
</script>

<template>
  <AiForm v-model:value="formData" :schema="schema">
    <template #status="{ value, updateValue }">
      <DictSelect 
        :value="value" 
        @update:value="updateValue"
        dict-type="case_status"
      />
    </template>
  </AiForm>
</template>
```

### 在 AiCrudPage 中使用

```vue
<script setup>
import { h } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { useDict } from '@/composables/useDict'
import DictTag from '@/components/DictTag.vue'
import DictSelect from '@/components/DictSelect.vue'

const { dict } = useDict('case_status', 'matter_type')

// 搜索表单
const searchSchema = [
  {
    field: 'status',
    label: '状态',
    type: 'slot',
    slotName: 'status'
  }
]

// 表格列
const columns = [
  {
    prop: 'status',
    label: '状态',
    render: (row) => h(DictTag, { options: dict.value.case_status, value: row.status })
  }
]

// 编辑表单
const editSchema = [
  {
    field: 'status',
    label: '状态',
    type: 'slot',
    slotName: 'status',
    rules: [{ required: true, message: '请选择状态' }]
  }
]
</script>

<template>
  <AiCrudPage
    api="/api/cases"
    :search-schema="searchSchema"
    :columns="columns"
    :edit-schema="editSchema"
  >
    <!-- 搜索表单插槽 -->
    <template #search-status="{ value, updateValue }">
      <DictSelect 
        :value="value" 
        @update:value="updateValue"
        dict-type="case_status"
      />
    </template>
    
    <!-- 编辑表单插槽 -->
    <template #form-status="{ value, updateValue }">
      <DictSelect 
        :value="value" 
        @update:value="updateValue"
        dict-type="case_status"
      />
    </template>
  </AiCrudPage>
</template>
```

## 四、字典缓存管理

### 清除缓存

```javascript
import { clearDictCache } from '@/composables/useDict'

// 清除指定字典缓存
clearDictCache('case_status')

// 清除所有字典缓存
clearDictCache()
```

### 重新加载字典

```javascript
const { reload } = useDict('case_status', 'matter_type')

// 重新加载所有字典
await reload()

// 重新加载指定字典
await reload('case_status')
```

## 五、迁移指南

### 旧系统代码

```vue
<script>
export default {
  dicts: ['case_status', 'matter_type', 'notice_reason']
}
</script>

<template>
  <dict-tag :options="dict.type.matter_type" :value="scope.row.matterType"/>
</template>
```

### 新系统代码

```vue
<script setup>
import { useDict } from '@/composables/useDict'
import DictTag from '@/components/DictTag.vue'

const { dict } = useDict('case_status', 'matter_type', 'notice_reason')
</script>

<template>
  <DictTag :options="dict.matter_type" :value="row.matterType"/>
</template>
```

### 主要变化

1. **导入方式**：从 `dicts` 选项改为 `useDict` composable
2. **访问方式**：从 `dict.type.matter_type` 改为 `dict.matter_type`
3. **组件名称**：从 `dict-tag` 改为 `DictTag`（大驼峰命名）
4. **数据格式**：保持兼容，但增加了更多字段

## 六、注意事项

1. **字典类型命名**：字典类型应使用下划线命名法，如 `case_status`、`matter_type`
2. **缓存机制**：字典数据会自动缓存，避免重复请求
3. **状态过滤**：DictSelect 会自动过滤状态为 0 的字典项
4. **样式映射**：DictTag 会根据 listClass 自动映射标签类型
5. **性能优化**：建议在父组件中使用 useDict 加载字典，然后通过 props 传递给子组件

## 七、完整示例

```vue
<script setup>
import { ref, h } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { useDict } from '@/composables/useDict'
import DictTag from '@/components/DictTag.vue'
import DictSelect from '@/components/DictSelect.vue'

const { dict, loading } = useDict('case_status', 'matter_type', 'priority_level')

const searchSchema = [
  {
    field: 'status',
    label: '状态',
    type: 'slot',
    slotName: 'status'
  },
  {
    field: 'matterType',
    label: '事项类型',
    type: 'slot',
    slotName: 'matterType'
  }
]

const columns = [
  { prop: 'caseNo', label: '案件编号', width: 150 },
  { prop: 'caseName', label: '案件名称', width: 200 },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    render: (row) => h(DictTag, { options: dict.value.case_status, value: row.status })
  },
  {
    prop: 'matterType',
    label: '事项类型',
    width: 120,
    render: (row) => h(DictTag, { options: dict.value.matter_type, value: row.matterType })
  },
  {
    prop: 'priority',
    label: '优先级',
    width: 100,
    render: (row) => h(DictTag, { options: dict.value.priority_level, value: row.priority, type: 'warning' })
  }
]

const editSchema = [
  {
    field: 'caseName',
    label: '案件名称',
    type: 'input',
    rules: [{ required: true, message: '请输入案件名称' }]
  },
  {
    field: 'status',
    label: '状态',
    type: 'slot',
    slotName: 'status',
    rules: [{ required: true, message: '请选择状态' }]
  },
  {
    field: 'matterType',
    label: '事项类型',
    type: 'slot',
    slotName: 'matterType',
    rules: [{ required: true, message: '请选择事项类型' }]
  },
  {
    field: 'priority',
    label: '优先级',
    type: 'slot',
    slotName: 'priority'
  }
]
</script>

<template>
  <AiCrudPage
    v-if="!loading"
    api="/api/cases"
    :search-schema="searchSchema"
    :columns="columns"
    :edit-schema="editSchema"
  >
    <!-- 搜索表单插槽 -->
    <template #search-status="{ value, updateValue }">
      <DictSelect :value="value" @update:value="updateValue" dict-type="case_status" />
    </template>
    
    <template #search-matterType="{ value, updateValue }">
      <DictSelect :value="value" @update:value="updateValue" dict-type="matter_type" />
    </template>
    
    <!-- 编辑表单插槽 -->
    <template #form-status="{ value, updateValue }">
      <DictSelect :value="value" @update:value="updateValue" dict-type="case_status" />
    </template>
    
    <template #form-matterType="{ value, updateValue }">
      <DictSelect :value="value" @update:value="updateValue" dict-type="matter_type" />
    </template>
    
    <template #form-priority="{ value, updateValue }">
      <DictSelect :value="value" @update:value="updateValue" dict-type="priority_level" />
    </template>
  </AiCrudPage>
</template>
```
