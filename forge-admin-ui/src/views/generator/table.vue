<template>
  <div class="generator-table-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/generator/list',
        detail: 'get@/generator/:id',
        update: 'post@/generator/edit',
        delete: 'post@/generator/remove',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="tableId"
      :edit-grid-cols="2"
      modal-width="900px"
      :show-add-button="false"
      :before-render-detail="handleBeforeRenderDetail"
      :before-submit="handleBeforeSubmit"
    >
      <!-- 自定义顶部操作栏 -->
      <template #toolbar-end>
        <n-button type="primary" size="small" @click="showAiSchemaModal = true">
          <template #icon>
            <i class="i-material-symbols:auto-awesome" />
          </template>
          AI 建表
        </n-button>
        <n-button type="primary" size="small" @click="showImportModal = true">
          <template #icon>
            <i class="i-material-symbols:add" />
          </template>
          导入表
        </n-button>
      </template>
    </AiCrudPage>

    <!-- 导入表弹窗 -->
    <ImportTableModal
      v-if="showImportModal"
      v-model:show="showImportModal"
      @success="handleImportSuccess"
    />

    <!-- 字段配置弹窗 -->
    <ColumnConfigModal
      v-if="showColumnModal"
      v-model:show="showColumnModal"
      :table-id="currentTableId"
      :table-name="currentTableName"
      @success="handleColumnSuccess"
    />

    <!-- 代码预览弹窗 -->
    <CodePreviewModal
      v-if="showPreviewModal"
      v-model:show="showPreviewModal"
      :table-name="currentTableName"
    />

    <!-- AI 建表弹窗 -->
    <AiSchemaModal
      v-if="showAiSchemaModal"
      v-model:show="showAiSchemaModal"
      @success="handleImportSuccess"
    />
  </div>
</template>

<script setup>
import { NModal } from 'naive-ui'
import { computed, defineAsyncComponent, h, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'
import DesignerAsyncLoader from '@/views/app-center/components/designer/DesignerAsyncLoader.vue'

defineOptions({ name: 'GeneratorTable' })

const GeneratorModalAsyncLoader = {
  name: 'GeneratorModalAsyncLoader',
  setup() {
    return () => h(NModal, {
      show: true,
      preset: 'card',
      title: '正在加载',
      style: 'width: min(520px, calc(100vw - 32px))',
      maskClosable: false,
    }, {
      default: () => h(DesignerAsyncLoader, {
        title: '正在加载弹窗',
        description: '首次打开需要准备组件资源',
        overlay: true,
      }),
    })
  },
}

function defineLazyGeneratorModal(loader) {
  return defineAsyncComponent({
    loader,
    loadingComponent: GeneratorModalAsyncLoader,
    delay: 120,
    suspensible: false,
  })
}

const AiSchemaModal = defineLazyGeneratorModal(() => import('./components/AiSchemaModal.vue'))
const CodePreviewModal = defineLazyGeneratorModal(() => import('./components/CodePreviewModal.vue'))
const ColumnConfigModal = defineLazyGeneratorModal(() => import('./components/ColumnConfigModal.vue'))
const ImportTableModal = defineLazyGeneratorModal(() => import('./components/ImportTableModal.vue'))

const crudRef = ref(null)
const showImportModal = ref(false)
const showColumnModal = ref(false)
const showPreviewModal = ref(false)
const showAiSchemaModal = ref(false)
const currentTableId = ref(null)
const currentTableName = ref('')
const menuParentOptions = ref([{ label: '顶级资源', value: 0, key: 0 }])
const { dict } = useDict('gen_generation_type', 'gen_template_engine')

const genTypeOptions = computed(() => dict.value.gen_generation_type || [])
const templateEngineOptions = computed(() => dict.value.gen_template_engine || [])

// 搜索表单配置
const searchSchema = [
  {
    field: 'tableName',
    label: '表名',
    type: 'input',
    props: {
      placeholder: '请输入表名',
    },
  },
  {
    field: 'tableComment',
    label: '表描述',
    type: 'input',
    props: {
      placeholder: '请输入表描述',
    },
  },
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'tableName',
    label: '表名',
    width: 180,
  },
  {
    prop: 'tableComment',
    label: '表描述',
    width: 180,
    ellipsis: true,
  },
  {
    prop: 'className',
    label: '实体类名',
    width: 150,
  },
  {
    prop: 'moduleName',
    label: '模块名',
    width: 100,
  },
  {
    prop: 'packageName',
    label: '包路径',
    minWidth: 200,
    ellipsis: true,
  },
  {
    prop: 'genType',
    label: '生成方式',
    width: 100,
    render: row => h(DictTag, { options: genTypeOptions.value, value: row.genType, size: 'small' }),
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    actions: [
      { label: '配置', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '字段', key: 'column', type: 'primary', onClick: handleColumnConfig },
      { label: '预览', key: 'preview', type: 'primary', onClick: handlePreview },
      { label: '生成', key: 'download', type: 'primary', onClick: handleDownload },
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
    field: 'tableName',
    label: '表名',
    type: 'input',
    props: {
      disabled: true,
      placeholder: '表名',
    },
  },
  {
    field: 'tableComment',
    label: '表描述',
    type: 'input',
    props: {
      placeholder: '请输入表描述',
    },
  },
  {
    field: 'className',
    label: '实体类名',
    type: 'input',
    rules: [{ required: true, message: '请输入实体类名', trigger: 'blur' }],
    props: {
      placeholder: '请输入实体类名',
    },
  },
  {
    field: 'businessName',
    label: '业务名称',
    type: 'input',
    props: {
      placeholder: '请输入业务名称（首字母小写）',
    },
  },
  {
    field: 'functionName',
    label: '功能名称',
    type: 'input',
    props: {
      placeholder: '请输入功能名称',
    },
  },
  {
    field: 'author',
    label: '作者',
    type: 'input',
    props: {
      placeholder: '请输入作者',
    },
  },
  {
    type: 'divider',
    label: '生成配置',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'moduleName',
    label: '模块名',
    type: 'input',
    props: {
      placeholder: '请输入模块名',
    },
  },
  {
    field: 'packageName',
    label: '包路径',
    type: 'input',
    span: 2,
    props: {
      placeholder: '请输入包路径',
    },
  },
  {
    field: 'genType',
    label: '生成方式',
    type: 'select',
    defaultValue: 'DOWNLOAD',
    props: {
      placeholder: '请选择生成方式',
      options: genTypeOptions.value,
    },
  },
  {
    field: 'templateEngine',
    label: '模板引擎',
    type: 'select',
    defaultValue: 'VELOCITY',
    props: {
      placeholder: '请选择模板引擎',
      options: templateEngineOptions.value,
    },
  },
  {
    field: 'genPath',
    label: '生成路径',
    type: 'input',
    span: 2,
    props: {
      placeholder: '请输入生成路径（仅生成到项目时有效）',
    },
  },
  {
    field: 'menuParentId',
    label: '发布目录',
    type: 'treeSelect',
    defaultValue: 0,
    span: 2,
    props: {
      placeholder: '请选择生成菜单的父级目录或菜单',
      clearable: true,
      filterable: true,
      keyField: 'value',
      labelField: 'label',
      childrenField: 'children',
    },
    options: () => menuParentOptions.value,
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注',
      rows: 2,
    },
  },
])

onMounted(loadMenuParentOptions)

async function loadMenuParentOptions() {
  try {
    const res = await request.get('/system/resource/tree')
    if (res.code === 200) {
      menuParentOptions.value = [
        { label: '顶级资源', value: 0, key: 0 },
        ...convertMenuParentOptions(res.data || []),
      ]
    }
  }
  catch (error) {
    console.warn('[GeneratorTable] 加载菜单资源树失败:', error)
  }
}

function convertMenuParentOptions(list = []) {
  return list.flatMap((item) => {
    const children = convertMenuParentOptions(item.children || [])
    const resourceType = Number(item.resourceType)
    if (![1, 2].includes(resourceType)) {
      return children
    }
    return [{
      label: `${item.resourceName}（${resourceType === 1 ? '目录' : '菜单'}）`,
      value: item.id,
      key: item.id,
      children: children.length > 0 ? children : undefined,
    }]
  })
}

function parseOptions(options) {
  if (!options)
    return {}
  if (typeof options === 'string') {
    try {
      const parsed = JSON.parse(options)
      return parsed && typeof parsed === 'object' ? parsed : {}
    }
    catch {
      return {}
    }
  }
  return typeof options === 'object' ? { ...options } : {}
}

function normalizeMenuParentId(value) {
  if (value === null || value === undefined || value === '')
    return 0
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : 0
}

function handleBeforeRenderDetail(data) {
  const options = parseOptions(data?.options)
  return {
    ...data,
    options,
    menuParentId: normalizeMenuParentId(data?.menuParentId ?? options.menuParentId),
  }
}

function handleBeforeSubmit(formData) {
  const options = parseOptions(formData.options)
  options.menuParentId = normalizeMenuParentId(formData.menuParentId)
  const data = {
    ...formData,
    options,
  }
  delete data.menuParentId
  return data
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除表配置"${row.tableName}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/generator/remove', [row.tableId])
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error('删除失败')
      }
    },
  })
}

// 字段配置
function handleColumnConfig(row) {
  currentTableId.value = row.tableId
  currentTableName.value = row.tableName
  showColumnModal.value = true
}

// 预览代码
function handlePreview(row) {
  currentTableName.value = row.tableName
  showPreviewModal.value = true
}

// 下载代码
async function handleDownload(row) {
  try {
    window.$message.loading('正在生成代码...', { duration: 0, key: 'download' })
    const res = await request.get(`/generator/download/${row.tableName}`, {
      responseType: 'blob',
    })

    // 创建下载链接
    const blob = new Blob([res], { type: 'application/zip' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${row.tableName}.zip`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)

    window.$message.success('代码生成成功', { key: 'download' })
  }
  catch (error) {
    console.error('下载失败:', error)
    window.$message.error('代码生成失败', { key: 'download' })
  }
}

// 导入成功
function handleImportSuccess() {
  crudRef.value?.refresh()
}

// 字段配置成功
function handleColumnSuccess() {
  // 可以刷新列表或其他操作
}
</script>

<style scoped>
.generator-table-page {
  height: 100%;
}
</style>
