<template>
  <div class="dict-data-page">
    <!-- 面包屑导航 -->
    <n-page-header @back="handleBack" style="margin-bottom: 16px">
      <template #title>
        <span>字典数据管理</span>
        <n-tag v-if="dictTypeName" type="info" size="small" style="margin-left: 12px">
          {{ dictTypeName }}
        </n-tag>
      </template>
      <template #subtitle>
        <span v-if="currentDictType">字典类型：{{ currentDictType }}</span>
      </template>
    </n-page-header>

    <AiCrudPage
      ref="crudRef"
      api="/system/dict/data"
      :api-config="{
        list: 'get@/system/dict/data/page',
        detail: 'post@/system/dict/data/getById',
        add: 'post@/system/dict/data/add',
        update: 'post@/system/dict/data/edit',
        delete: 'post@/system/dict/data/remove'
      }"
      :load-detail-on-edit="true"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :public-params="publicParams"
      row-key="dictCode"
      add-button-text="新增字典数据"
      :before-submit="handleBeforeSubmit"
      :before-render-form="handleBeforeRenderForm"
      :edit-grid-cols="2"
      :lazy="true"
    />
  </div>
</template>

<script setup>
import { ref, h, computed, onMounted, nextTick } from 'vue'
import { NTag, NPageHeader } from 'naive-ui'
import { AiCrudPage } from '@/components/ai-form'
import { useRouter, useRoute } from 'vue-router'
import { request } from '@/utils'

defineOptions({ name: 'DictData',title:'字典数据' })

const router = useRouter()
const route = useRoute()
const crudRef = ref(null)

// 当前字典类型
const currentDictType = ref('')
const dictTypeName = ref('')

// 字典类型选项
const dictTypeOptions = ref([])

// 字典状态选项
const statusOptions = [
  { label: '正常', value: 1 },
  { label: '禁用', value: 0 }
]

// 是否默认选项
const isDefaultOptions = [
  { label: '是', value: 'Y' },
  { label: '否', value: 'N' }
]

// 标签类型选项
const tagTypeOptions = [
  { label: '默认', value: 'default' },
  { label: '成功', value: 'success' },
  { label: '信息', value: 'info' },
  { label: '警告', value: 'warning' },
  { label: '错误', value: 'error' }
]

// 公共查询参数
const publicParams = computed(() => {
  return currentDictType.value ? { dictType: currentDictType.value } : {}
})

// 搜索表单配置
const searchSchema = [
  {
    field: 'dictLabel',
    label: '字典标签',
    type: 'input',
    props: {
      placeholder: '请输入字典标签'
    }
  },
  {
    field: 'dictValue',
    label: '字典键值',
    type: 'input',
    props: {
      placeholder: '请输入字典键值'
    }
  },
  {
    field: 'dictStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: statusOptions
    }
  }
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'dictCode',
    label: '字典编码',
    width: 100
  },
  {
    prop: 'dictLabel',
    label: '字典标签',
    width: 150
  },
  {
    prop: 'dictValue',
    label: '字典键值',
    width: 150
  },
  {
    prop: 'dictSort',
    label: '排序',
    width: 100
  },
  {
    prop: 'dictStatus',
    label: '状态',
    width: 100,
    render: (row) => {
      return h(NTag,
        { type: row.dictStatus === 1 ? 'success' : 'error', size: 'small' },
        { default: () => row.dictStatus === 1 ? '正常' : '禁用' }
      )
    }
  },
  {
    prop: 'isDefault',
    label: '是否默认',
    width: 100,
    render: (row) => {
      return h(NTag,
        { type: row.isDefault === 'Y' ? 'success' : 'default', size: 'small' },
        { default: () => row.isDefault === 'Y' ? '是' : '否' }
      )
    }
  },
  {
    prop: 'listClass',
    label: '标签类型',
    width: 120,
    render: (row) => {
      if (!row.listClass) return '-'
      const typeMap = {
        'default': { text: '默认', type: 'default' },
        'success': { text: '成功', type: 'success' },
        'info': { text: '信息', type: 'info' },
        'warning': { text: '警告', type: 'warning' },
        'error': { text: '错误', type: 'error' },
        // 兼容旧的命名
        'primary': { text: '信息', type: 'info' },
        'danger': { text: '错误', type: 'error' }
      }
      const config = typeMap[row.listClass] || { text: row.listClass, type: 'default' }
      
      // 如果是 default 类型，显示普通文字
      if (config.type === 'default') {
        return h('span', config.text)
      }
      
      // 其他类型显示标签
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    }
  },
  {
    prop: 'cssClass',
    label: '样式属性',
    width: 120
  },
  {
    prop: 'remark',
    label: '备注',
    width: 200
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180
  },
  {
    prop: 'action',
    label: '操作',
    width: 120,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete }
    ]
  }
])

// 编辑表单配置
const editSchema = computed(() => [
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'dictType',
    label: '字典类型',
    type: 'input',
    rules: [{ required: true, message: '字典类型不能为空', trigger: 'blur' }],
    defaultValue: currentDictType.value,
    props: {
      placeholder: '字典类型',
      disabled: true,
      readonly: true
    }
  },
  {
    field: 'dictLabel',
    label: '字典标签',
    type: 'input',
    rules: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
    props: {
      placeholder: '请输入字典标签'
    }
  },
  {
    field: 'dictValue',
    label: '字典键值',
    type: 'input',
    rules: [{ required: true, message: '请输入字典键值', trigger: 'blur' }],
    props: {
      placeholder: '请输入字典键值'
    }
  },
  {
    field: 'dictSort',
    label: '排序',
    type: 'input-number',
    defaultValue: 0,
    props: {
      placeholder: '排序值',
      min: 0
    }
  },
  {
    field: 'dictStatus',
    label: '状态',
    type: 'radio',
    defaultValue: 1,
    rules: [{ required: true, type: 'number', message: '请选择状态', trigger: 'change' }],
    props: {
      options: statusOptions
    }
  },
  {
    field: 'isDefault',
    label: '是否默认',
    type: 'radio',
    defaultValue: 'N',
    props: {
      options: isDefaultOptions
    }
  },
  {
    type: 'divider',
    label: '扩展信息',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'listClass',
    label: '标签类型',
    type: 'select',
    defaultValue: 'default',
    props: {
      placeholder: '请选择标签类型',
      options: tagTypeOptions
    }
  },
  {
    field: 'cssClass',
    label: '样式属性',
    type: 'input',
    props: {
      placeholder: '请输入样式属性（可选）'
    }
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注',
      rows: 3
    }
  }
])

// 加载字典类型选项
async function loadDictTypeOptions() {
  try {
    const res = await request.get('/system/dict/type/list')
    if (res.code === 200) {
      dictTypeOptions.value = (res.data || []).map(item => ({
        label: item.dictName,
        value: item.dictType
      }))
    }
  } catch (error) {
    console.error('加载字典类型选项失败:', error)
  }
}

// 表单渲染前处理（新增时设置默认值）
function handleBeforeRenderForm(data) {
  // 如果是新增（data 为 null），设置默认的 dictType
  if (!data && currentDictType.value) {
    return {
      dictType: currentDictType.value
    }
  }
  return data
}

// 提交前处理
function handleBeforeSubmit(formData) {
  // 确保 dictType 始终是当前的字典类型
  if (currentDictType.value) {
    formData.dictType = currentDictType.value
  }
  return formData
}

// 返回
function handleBack() {
  router.push('/system/dictType')
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: '确定要删除该字典数据吗？删除后将无法恢复！',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/dict/data/remove', null, {
          params: { dictCode: row.dictCode }
        })
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
        }
      } catch (error) {
        window.$message.error('删除失败')
      }
    }
  })
}

// 初始化
onMounted(() => {
  // 从路由参数获取字典类型
  if (route.query.dictType) {
    currentDictType.value = route.query.dictType
    dictTypeName.value = route.query.dictName || ''
  }

  // 加载字典类型选项
  loadDictTypeOptions()

  // 延迟加载列表数据，确保 publicParams 已更新
  nextTick(() => {
    crudRef.value?.loadList()
  })
})
</script>

<style scoped>
.dict-data-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.dict-data-page :deep(.ai-crud-page) {
  flex: 1;
}
</style>
