<template>
  <div class="generator-table-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/generator/list',
        detail: 'get@/generator/{id}',
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
      v-model:show="showImportModal"
      @success="handleImportSuccess"
    />

    <!-- 字段配置弹窗 -->
    <ColumnConfigModal
      v-model:show="showColumnModal"
      :table-id="currentTableId"
      :table-name="currentTableName"
      @success="handleColumnSuccess"
    />

    <!-- 代码预览弹窗 -->
    <CodePreviewModal
      v-model:show="showPreviewModal"
      :table-name="currentTableName"
    />

    <!-- AI 建表弹窗 -->
    <AiSchemaModal
      v-model:show="showAiSchemaModal"
      @success="handleImportSuccess"
    />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'
import AiSchemaModal from './components/AiSchemaModal.vue'
import CodePreviewModal from './components/CodePreviewModal.vue'
import ColumnConfigModal from './components/ColumnConfigModal.vue'
import ImportTableModal from './components/ImportTableModal.vue'

defineOptions({ name: 'GeneratorTable' })

const crudRef = ref(null)
const showImportModal = ref(false)
const showColumnModal = ref(false)
const showPreviewModal = ref(false)
const showAiSchemaModal = ref(false)
const currentTableId = ref(null)
const currentTableName = ref('')

// 生成方式选项
const genTypeOptions = [
  { label: '下载代码包', value: 'DOWNLOAD' },
  { label: '生成到项目', value: 'PROJECT' },
]

// 模板引擎选项
const templateEngineOptions = [
  { label: 'Velocity', value: 'VELOCITY' },
  { label: 'Freemarker', value: 'FREEMARKER' },
]

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
    render: (row) => {
      const option = genTypeOptions.find(opt => opt.value === row.genType)
      return option ? option.label : row.genType
    },
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
const editSchema = [
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
      options: genTypeOptions,
    },
  },
  {
    field: 'templateEngine',
    label: '模板引擎',
    type: 'select',
    defaultValue: 'VELOCITY',
    props: {
      placeholder: '请选择模板引擎',
      options: templateEngineOptions,
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
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注',
      rows: 2,
    },
  },
]

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
      catch (error) {
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
