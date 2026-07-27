<template>
  <div class="dict-data-page">
    <!-- 面包屑导航 -->
    <NPageHeader style="margin-bottom: 16px" @back="handleBack">
      <template #title>
        <span>字典数据管理</span>
        <NTag v-if="dictTypeName" type="info" size="small" style="margin-left: 12px">
          {{ dictTypeName }}
        </NTag>
      </template>
      <template #subtitle>
        <span v-if="currentDictType">字典类型：{{ currentDictType }}</span>
      </template>
    </NPageHeader>

    <AiCrudPage
      ref="crudRef"
      api="/system/dict/data"
      :api-config="apiConfig"
      :load-detail-on-edit="true"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :public-params="publicParams"
      :before-load-list="handleBeforeLoadList"
      :before-render-list="handleBeforeRenderList"
      row-key="dictCode"
      add-button-text="新增字典数据"
      :before-submit="handleBeforeSubmit"
      :before-delete="handleBeforeDelete"
      :before-render-form="handleBeforeRenderForm"
      :before-render-detail="handleBeforeRenderDetail"
      :edit-grid-cols="2"
      :show-pagination="!isTreeMode"
      :table-props="tableProps"
      :lazy="true"
      @add="handleToolbarAdd"
      @submit-success="handleSubmitSuccess"
    >
      <template #toolbar-end>
        <n-button v-if="isTreeMode" size="small" @click="toggleExpandAll">
          {{ expandAll ? '折叠全部' : '展开全部' }}
        </n-button>
      </template>

      <template #toolbar-right-start>
        <n-radio-group :value="viewMode" size="small" @update:value="handleViewModeChange">
          <n-radio-button v-for="item in viewModeOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </n-radio-button>
        </n-radio-group>
      </template>
    </AiCrudPage>
  </div>
</template>

<script setup>
import { NPageHeader, NTag } from 'naive-ui'
import { computed, h, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AiCrudPage } from '@/components/ai-form'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import { DICT_TAG_TYPE_OPTIONS } from '@/constants/dict-options'
import { request } from '@/utils'

defineOptions({ name: 'DictData', title: '字典数据' })

const ROOT_PARENT_CODE = 0

const router = useRouter()
const route = useRoute()
const crudRef = ref(null)

// 当前字典类型
const currentDictType = ref('')
const dictTypeName = ref('')

const parentDictOptions = ref([createRootOption()])
const allDictData = ref([])
const pendingParentDictCode = ref(ROOT_PARENT_CODE)
const editingDictCode = ref(null)
const latestListParams = ref({})
const viewMode = ref('list')
const expandAll = ref(true)
const expandedKeys = ref([])

const viewModeOptions = [
  { label: '平铺列表', value: 'list' },
  { label: '树形结构', value: 'tree' },
]

// 字典状态选项
const statusOptions = [
  { label: '正常', value: 1 },
  { label: '禁用', value: 0 },
]

// 是否默认选项
const isDefaultOptions = [
  { label: '是', value: 'Y' },
  { label: '否', value: 'N' },
]

const isTreeMode = computed(() => viewMode.value === 'tree')

const apiConfig = computed(() => ({
  list: isTreeMode.value ? 'get@/system/dict/data/list' : 'get@/system/dict/data/page',
  detail: 'post@/system/dict/data/getById',
  add: 'post@/system/dict/data/add',
  update: 'post@/system/dict/data/edit',
  delete: 'post@/system/dict/data/removeBatch',
}))

// 公共查询参数
const publicParams = computed(() => {
  return currentDictType.value ? { dictType: currentDictType.value } : {}
})

const dictLabelMap = computed(() => {
  const map = new Map()
  allDictData.value.forEach((item) => {
    map.set(item.dictCode, item.dictLabel)
  })
  return map
})

const tableProps = computed(() => {
  if (!isTreeMode.value) {
    return {}
  }

  return {
    indent: 24,
    expandOnClick: true,
    expandedRowKeys: expandedKeys.value,
    onUpdateExpandedRowKeys: handleExpandedKeysUpdate,
  }
})

// 搜索表单配置
const searchSchema = [
  {
    field: 'dictLabel',
    label: '字典标签',
    type: 'input',
    props: {
      placeholder: '请输入字典标签',
    },
  },
  {
    field: 'dictValue',
    label: '字典键值',
    type: 'input',
    props: {
      placeholder: '请输入字典键值',
    },
  },
  {
    field: 'dictStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: statusOptions,
    },
  },
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'dictLabel',
    label: '字典项',
    minWidth: 190,
    render: row => h(SystemTableCell, {
      title: row.dictLabel,
      subtitle: row.dictValue,
      interactive: true,
      tooltip: `查看字典项：${row.dictLabel || row.dictValue || '-'}`,
      onActivate: () => crudRef.value?.showDetail(row),
    }),
  },
  {
    prop: 'parentDictCode',
    label: '上级节点',
    width: 160,
    render: row => getParentDictLabel(row.parentDictCode),
  },
  {
    prop: 'dictSort',
    label: '排序',
    width: 100,
  },
  {
    prop: 'dictStatus',
    label: '状态',
    width: 100,
    render: (row) => {
      return h(NTag, { type: row.dictStatus === 1 ? 'success' : 'error', size: 'small' }, { default: () => row.dictStatus === 1 ? '正常' : '禁用' },
      )
    },
  },
  {
    prop: 'isDefault',
    label: '是否默认',
    width: 100,
    render: (row) => {
      return h(NTag, { type: row.isDefault === 'Y' ? 'success' : 'default', size: 'small' }, { default: () => row.isDefault === 'Y' ? '是' : '否' },
      )
    },
  },
  {
    prop: 'listClass',
    label: '标签类型',
    width: 120,
    render: (row) => {
      if (!row.listClass)
        return '-'
      const typeMap = {
        default: { text: '默认', type: 'default' },
        success: { text: '成功', type: 'success' },
        info: { text: '信息', type: 'info' },
        warning: { text: '警告', type: 'warning' },
        error: { text: '错误', type: 'error' },
        // 兼容旧的命名
        primary: { text: '信息', type: 'info' },
        danger: { text: '错误', type: 'error' },
      }
      const config = typeMap[row.listClass] || { text: row.listClass, type: 'default' }

      // 如果是 default 类型，显示普通文字
      if (config.type === 'default') {
        return h('span', config.text)
      }

      // 其他类型显示标签
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    },
  },
  {
    prop: 'cssClass',
    label: '样式属性',
    width: 120,
  },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 180,
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 172,
  },
  {
    prop: 'action',
    label: '操作',
    width: 160,
    fixed: 'right',
    actions: [
      { label: '新增下级', key: 'addChild', type: 'primary', onClick: handleAddChild },
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置
const editSchema = computed(() => [
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'parentDictCode',
    label: '上级节点',
    type: 'treeSelect',
    defaultValue: ROOT_PARENT_CODE,
    props: {
      placeholder: '请选择上级节点',
      clearable: true,
      filterable: true,
      defaultExpandAll: true,
    },
    options: () => parentDictOptions.value,
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
      readonly: true,
    },
  },
  {
    field: 'dictLabel',
    label: '字典标签',
    type: 'input',
    rules: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
    props: {
      placeholder: '请输入字典标签',
    },
  },
  {
    field: 'dictValue',
    label: '字典键值',
    type: 'input',
    rules: [{ required: true, message: '请输入字典键值', trigger: 'blur' }],
    props: {
      placeholder: '请输入字典键值',
    },
  },
  {
    field: 'dictSort',
    label: '排序',
    type: 'number',
    defaultValue: 0,
    props: {
      placeholder: '排序值',
      min: 0,
    },
  },
  {
    field: 'dictStatus',
    label: '状态',
    type: 'radio',
    defaultValue: 1,
    rules: [{ required: true, type: 'number', message: '请选择状态', trigger: 'change' }],
    props: {
      options: statusOptions,
    },
  },
  {
    field: 'isDefault',
    label: '是否默认',
    type: 'radio',
    defaultValue: 'N',
    props: {
      options: isDefaultOptions,
    },
  },
  {
    type: 'divider',
    label: '扩展信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'listClass',
    label: '标签类型',
    type: 'select',
    defaultValue: 'default',
    props: {
      placeholder: '请选择标签类型',
      options: DICT_TAG_TYPE_OPTIONS,
    },
  },
  {
    field: 'cssClass',
    label: '样式属性',
    type: 'input',
    props: {
      placeholder: '请输入样式属性（可选）',
    },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注',
      rows: 3,
    },
  },
])

async function loadParentDictOptions(currentDictCode = null) {
  if (!currentDictType.value) {
    parentDictOptions.value = [createRootOption()]
    allDictData.value = []
    return
  }

  try {
    const res = await request.get('/system/dict/data/list', {
      params: {
        dictType: currentDictType.value,
      },
    })

    if (res.code === 200) {
      allDictData.value = Array.isArray(res.data) ? res.data : []

      const excludedDictCodes = collectExcludedDictCodes(allDictData.value, currentDictCode)
      const availableDictData = allDictData.value.filter(item => !excludedDictCodes.has(item.dictCode))
      const treeData = buildDictTree(availableDictData)

      parentDictOptions.value = [
        createRootOption(),
        ...convertToTreeSelectOptions(treeData),
      ]
    }
  }
  catch (error) {
    console.error('加载上级字典选项失败:', error)
    parentDictOptions.value = [createRootOption()]
  }
}

function createRootOption() {
  return {
    label: '顶级节点',
    value: ROOT_PARENT_CODE,
    key: ROOT_PARENT_CODE,
  }
}

function isRootParentCode(parentDictCode) {
  return parentDictCode === null
    || parentDictCode === undefined
    || parentDictCode === ''
    || Number(parentDictCode) === ROOT_PARENT_CODE
}

function normalizeParentDictCode(parentDictCode) {
  return isRootParentCode(parentDictCode) ? ROOT_PARENT_CODE : parentDictCode
}

function buildDictTree(list) {
  const nodeMap = new Map()
  const tree = []

  list.forEach((item) => {
    nodeMap.set(item.dictCode, {
      ...item,
      children: [],
    })
  })

  nodeMap.forEach((node) => {
    const parentDictCode = normalizeParentDictCode(node.parentDictCode)

    if (!isRootParentCode(parentDictCode) && nodeMap.has(parentDictCode) && parentDictCode !== node.dictCode) {
      nodeMap.get(parentDictCode).children.push(node)
    }
    else {
      tree.push(node)
    }
  })

  sortDictTree(tree)
  return tree
}

function sortDictTree(list) {
  list.sort((left, right) => {
    const leftSort = Number(left.dictSort ?? 0)
    const rightSort = Number(right.dictSort ?? 0)

    if (leftSort !== rightSort) {
      return leftSort - rightSort
    }

    return Number(left.dictCode ?? 0) - Number(right.dictCode ?? 0)
  })

  list.forEach((item) => {
    if (item.children && item.children.length > 0) {
      sortDictTree(item.children)
    }
    else {
      delete item.children
    }
  })
}

function convertToTreeSelectOptions(list) {
  return list.map(item => ({
    label: item.dictLabel,
    value: item.dictCode,
    key: item.dictCode,
    children: item.children && item.children.length > 0
      ? convertToTreeSelectOptions(item.children)
      : undefined,
  }))
}

function collectExcludedDictCodes(list, currentDictCode) {
  if (!currentDictCode) {
    return new Set()
  }

  const childrenMap = new Map()
  list.forEach((item) => {
    const parentDictCode = normalizeParentDictCode(item.parentDictCode)
    if (!isRootParentCode(parentDictCode)) {
      const children = childrenMap.get(parentDictCode) || []
      children.push(item.dictCode)
      childrenMap.set(parentDictCode, children)
    }
  })

  const excluded = new Set([currentDictCode])
  const queue = [currentDictCode]

  while (queue.length > 0) {
    const parentDictCode = queue.shift()
    const children = childrenMap.get(parentDictCode) || []

    children.forEach((dictCode) => {
      if (!excluded.has(dictCode)) {
        excluded.add(dictCode)
        queue.push(dictCode)
      }
    })
  }

  return excluded
}

function filterTreeModeList(list) {
  if (!isTreeMode.value) {
    return list
  }

  const { dictLabel, dictValue, dictStatus } = latestListParams.value

  return list.filter((item) => {
    const labelMatched = !dictLabel || String(item.dictLabel || '').includes(dictLabel)
    const valueMatched = !dictValue || String(item.dictValue || '').includes(dictValue)
    const statusMatched = dictStatus === null
      || dictStatus === undefined
      || dictStatus === ''
      || Number(item.dictStatus) === Number(dictStatus)

    return labelMatched && valueMatched && statusMatched
  })
}

function getAllKeys(list, keys = []) {
  list.forEach((item) => {
    keys.push(item.dictCode)
    if (item.children && item.children.length > 0) {
      getAllKeys(item.children, keys)
    }
  })
  return keys
}

function getParentDictLabel(parentDictCode) {
  if (isRootParentCode(parentDictCode)) {
    return '顶级节点'
  }

  return dictLabelMap.value.get(parentDictCode)
    || dictLabelMap.value.get(Number(parentDictCode))
    || parentDictCode
}

function handleBeforeLoadList(params) {
  latestListParams.value = { ...params }
  return params
}

function handleBeforeRenderList(list) {
  if (!isTreeMode.value) {
    expandedKeys.value = []
    return list
  }

  const treeList = buildDictTree(filterTreeModeList(list))
  expandedKeys.value = expandAll.value ? getAllKeys(treeList) : []
  return treeList
}

// 表单渲染前处理（新增时设置默认值）
function handleBeforeRenderForm(data) {
  if (!data) {
    return {
      dictType: currentDictType.value,
      parentDictCode: pendingParentDictCode.value,
    }
  }

  editingDictCode.value = data.dictCode || null
  return data
}

function handleBeforeRenderDetail(data) {
  return {
    ...data,
    parentDictCode: normalizeParentDictCode(data?.parentDictCode),
  }
}

// 提交前处理
function handleBeforeSubmit(formData) {
  if (formData.dictCode && Number(formData.dictCode) === Number(formData.parentDictCode)) {
    window.$message.warning('上级节点不能选择自己')
    return false
  }

  if (currentDictType.value) {
    formData.dictType = currentDictType.value
  }

  formData.parentDictCode = isRootParentCode(formData.parentDictCode) ? null : formData.parentDictCode
  return formData
}

// 返回
function handleBack() {
  router.push('/system/dictType')
}

function handleExpandedKeysUpdate(keys) {
  expandedKeys.value = keys

  if (!isTreeMode.value) {
    return
  }

  const tableData = crudRef.value?.getTableData() || []
  const allKeys = getAllKeys(tableData)
  expandAll.value = allKeys.length > 0 && keys.length === allKeys.length
}

function toggleExpandAll() {
  expandAll.value = !expandAll.value

  if (expandAll.value) {
    const tableData = crudRef.value?.getTableData() || []
    expandedKeys.value = getAllKeys(tableData)
  }
  else {
    expandedKeys.value = []
  }
}

function handleViewModeChange(value) {
  if (viewMode.value === value) {
    return
  }

  viewMode.value = value

  if (!isTreeMode.value) {
    expandedKeys.value = []
  }

  nextTick(() => {
    crudRef.value?.loadList()
  })
}

async function handleBeforeDelete(rows = []) {
  const dictCodes = rows
    .map(row => row?.dictCode)
    .filter(dictCode => dictCode !== null && dictCode !== undefined && dictCode !== '')

  if (dictCodes.length === 0) {
    window.$message.warning('请选择要删除的字典数据')
    return false
  }

  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${dictCodes.length} 条字典数据吗？删除后将无法恢复！`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/dict/data/removeBatch', dictCodes)
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.clearSelection()
          await loadParentDictOptions()
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error('删除失败')
      }
    },
  })

  return false
}

async function handleToolbarAdd() {
  pendingParentDictCode.value = ROOT_PARENT_CODE
  editingDictCode.value = null
  await loadParentDictOptions()
}

async function handleAddChild(row) {
  pendingParentDictCode.value = row.dictCode
  editingDictCode.value = null
  await loadParentDictOptions()
  crudRef.value?.showAdd()
  await nextTick()
  pendingParentDictCode.value = ROOT_PARENT_CODE
}

// 编辑
async function handleEdit(row) {
  editingDictCode.value = row.dictCode || null
  await loadParentDictOptions(editingDictCode.value)
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
          params: { dictCode: row.dictCode },
        })
        if (res.code === 200) {
          window.$message.success('删除成功')
          await loadParentDictOptions()
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error('删除失败')
      }
    },
  })
}

async function handleSubmitSuccess() {
  pendingParentDictCode.value = ROOT_PARENT_CODE
  editingDictCode.value = null
  await loadParentDictOptions()
}

// 初始化
onMounted(async () => {
  // 从路由参数获取字典类型
  if (route.query.dictType) {
    currentDictType.value = route.query.dictType
    dictTypeName.value = route.query.dictName || ''
  }

  await loadParentDictOptions()

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
