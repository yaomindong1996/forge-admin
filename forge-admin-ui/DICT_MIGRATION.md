# 字典组件迁移指南

## 快速迁移步骤

### 1. 替换导入语句

**旧代码：**

```vue
<script>
export default {
  dicts: ['case_status', 'matter_type', 'notice_reason']
}
</script>
```

**新代码：**

```vue
<script setup>
import DictSelect from '@/components/DictSelect.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

const { dict } = useDict('case_status', 'matter_type', 'notice_reason')
</script>
```

### 2. 替换字典访问方式

**旧代码：**

```vue
<template>
  <dict-tag :options="dict.type.matter_type" :value="scope.row.matterType" />
</template>
```

**新代码：**

```vue
<template>
  <DictTag :options="dict.matter_type" :value="row.matterType" />
</template>
```

### 3. 替换字典选择器

**旧代码：**

```vue
<el-select v-model="form.status">
  <el-option
    v-for="item in dict.type.case_status"
    :key="item.value"
    :label="item.label"
    :value="item.value"
  />
</el-select>
```

**新代码：**

```vue
<DictSelect v-model:value="form.status" dict-type="case_status" />
```

### 4. 在表格中使用

**旧代码：**

```vue
<el-table-column label="状态" prop="status">
  <template #default="scope">
    <dict-tag :options="dict.type.case_status" :value="scope.row.status"/>
  </template>
</el-table-column>
```

**新代码（使用 render 函数）：**

```vue
<script setup>
import { h } from 'vue'

const columns = [
  {
    prop: 'status',
    label: '状态',
    render: row => h(DictTag, { options: dict.value.case_status, value: row.status })
  }
]
</script>
```

## 完整迁移示例

### 旧代码（Options API）

```vue
<template>
  <div>
    <el-form :model="form">
      <el-form-item label="状态">
        <el-select v-model="form.status">
          <el-option
            v-for="item in dict.type.case_status"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="类型">
        <el-select v-model="form.matterType">
          <el-option
            v-for="item in dict.type.matter_type"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <el-table :data="tableData">
      <el-table-column label="状态" prop="status">
        <template #default="scope">
          <dict-tag :options="dict.type.case_status" :value="scope.row.status" />
        </template>
      </el-table-column>

      <el-table-column label="类型" prop="matterType">
        <template #default="scope">
          <dict-tag :options="dict.type.matter_type" :value="scope.row.matterType" />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
export default {
  dicts: ['case_status', 'matter_type', 'notice_reason'],
  data() {
    return {
      form: {
        status: '',
        matterType: ''
      },
      tableData: []
    }
  }
}
</script>
```

### 新代码（Composition API + AiCrudPage）

```vue
<template>
  <AiCrudPage
    api="/api/cases"
    :search-schema="searchSchema"
    :columns="columns"
    :edit-schema="editSchema"
  >
    <!-- 搜索表单插槽 -->
    <template #search-status="{ value, updateValue }">
      <DictSelect :value="value" dict-type="case_status" @update:value="updateValue" />
    </template>

    <template #search-matterType="{ value, updateValue }">
      <DictSelect :value="value" dict-type="matter_type" @update:value="updateValue" />
    </template>

    <!-- 编辑表单插槽 -->
    <template #form-status="{ value, updateValue }">
      <DictSelect :value="value" dict-type="case_status" @update:value="updateValue" />
    </template>

    <template #form-matterType="{ value, updateValue }">
      <DictSelect :value="value" dict-type="matter_type" @update:value="updateValue" />
    </template>
  </AiCrudPage>
</template>

<script setup>
import { h } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictSelect from '@/components/DictSelect.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

const { dict } = useDict('case_status', 'matter_type', 'notice_reason')

// 搜索表单
const searchSchema = [
  {
    field: 'status',
    label: '状态',
    type: 'slot',
    slotName: 'status'
  },
  {
    field: 'matterType',
    label: '类型',
    type: 'slot',
    slotName: 'matterType'
  }
]

// 表格列
const columns = [
  {
    prop: 'status',
    label: '状态',
    width: 100,
    render: row => h(DictTag, { options: dict.value.case_status, value: row.status })
  },
  {
    prop: 'matterType',
    label: '类型',
    width: 120,
    render: row => h(DictTag, { options: dict.value.matter_type, value: row.matterType })
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
  },
  {
    field: 'matterType',
    label: '类型',
    type: 'slot',
    slotName: 'matterType',
    rules: [{ required: true, message: '请选择类型' }]
  }
]
</script>
```

## 批量替换正则表达式

可以使用以下正则表达式进行批量替换：

### 1. 替换 dict.type.xxx 为 dict.xxx

**查找：** `dict\.type\.(\w+)`
**替换：** `dict.$1`

### 2. 替换 dict-tag 为 DictTag

**查找：** `<dict-tag`
**替换：** `<DictTag`

**查找：** `</dict-tag>`
**替换：** `</DictTag>`

### 3. 替换 scope.row 为 row

**查找：** `scope\.row`
**替换：** `row`

## 注意事项

1. **导入组件**：确保在每个使用字典的文件中导入 `useDict`、`DictTag` 和 `DictSelect`
2. **响应式访问**：在 render 函数中使用 `dict.value.xxx` 而不是 `dict.xxx`
3. **字典类型**：确保字典类型名称与后端一致
4. **缓存机制**：新系统会自动缓存字典数据，无需手动管理
5. **状态过滤**：DictSelect 会自动过滤状态为 0 的字典项

## 测试清单

迁移完成后，请测试以下功能：

- [ ] 字典数据能正常加载
- [ ] DictTag 能正确显示字典标签
- [ ] DictSelect 能正常选择字典项
- [ ] 表格中的字典标签显示正常
- [ ] 搜索表单中的字典选择器工作正常
- [ ] 编辑表单中的字典选择器工作正常
- [ ] 多选字典选择器工作正常
- [ ] 字典缓存机制工作正常

## 常见问题

### Q1: 字典数据加载失败？

**A:** 检查后端接口 `/system/dict/data/list` 是否正常，参数 `dictType` 是否正确。

### Q2: DictTag 不显示？

**A:** 检查 `options` 或 `dictType` 是否正确传递，`value` 是否与字典值匹配。

### Q3: DictSelect 选项为空？

**A:** 检查 `dictType` 是否正确，字典数据是否已加载。

### Q4: 在 render 函数中访问 dict 报错？

**A:** 在 render 函数中使用 `dict.value.xxx` 而不是 `dict.xxx`。

### Q5: 字典数据不更新？

**A:** 使用 `reload()` 函数重新加载字典，或使用 `clearDictCache()` 清除缓存。

## 获取帮助

如有问题，请查看：

- [字典组件使用指南](./src/components/DICT_USAGE_GUIDE.md)
- [字典管理页面](./src/views/system/dictType.vue)
- [字典数据页面](./src/views/system/dictData.vue)
- [字典组件示例](./src/views/system/dictDemo.vue)
