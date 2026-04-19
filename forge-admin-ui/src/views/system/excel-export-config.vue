<template>
  <div class="excel-export-config-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/system/excel/export-config/page',
        detail: 'post@/system/excel/export-config/detail',
        add: 'post@/system/excel/export-config',
        update: 'put@/system/excel/export-config',
        delete: 'delete@/system/excel/export-config/{ids}',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="id"
      add-button-text="新增导出配置"
      :load-detail-on-edit="true"
      :edit-grid-cols="2"
      modal-width="1000px"
    >
      <!-- 工具栏扩展按钮 -->
      <template #toolbar-end>
        <NButton type="info" size="small" @click="handleRefresh">
          <template #icon>
            <i class="i-material-symbols:refresh" />
          </template>
          刷新
        </NButton>
      </template>
    </AiCrudPage>

    <!-- 列配置管理弹窗 -->
    <NModal
      v-model:show="showColumnModal"
      preset="card"
      title="列配置管理"
      :style="{ width: '1200px' }"
      :mask-closable="false"
    >
      <ExcelColumnConfig
        v-if="showColumnModal"
        :config-key="currentConfigKey"
        :config-name="currentConfigName"
      />
    </NModal>

    <!-- 复制配置弹窗 -->
    <NModal
      v-model:show="showCopyModal"
      preset="dialog"
      title="复制配置"
      positive-text="确定"
      negative-text="取消"
      @positive-click="handleConfirmCopy"
    >
      <NForm ref="copyFormRef" :model="copyForm" label-placement="left" label-width="120">
        <NFormItem
          label="新配置键"
          path="newConfigKey"
          :rule="{ required: true, message: '请输入新配置键', trigger: 'blur' }"
        >
          <NInput
            v-model:value="copyForm.newConfigKey"
            placeholder="请输入新配置键，如：user_list_export_v2"
          />
        </NFormItem>
      </NForm>
    </NModal>
  </div>
</template>

<script setup>
import { NButton, NForm, NFormItem, NInput, NModal, NTag } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { useAuthStore } from '@/store'
import { request } from '@/utils'
import ExcelColumnConfig from './excel-column-config.vue'

defineOptions({ name: 'ExcelExportConfig' })

const authStore = useAuthStore()
const crudRef = ref(null)
const showColumnModal = ref(false)
const showCopyModal = ref(false)
const copyFormRef = ref(null)
const currentConfigKey = ref('')
const currentConfigName = ref('')
const copyForm = ref({
  sourceId: null,
  newConfigKey: '',
})

// 搜索表单配置
const searchSchema = [
  {
    field: 'configKey',
    label: '配置键',
    type: 'input',
    props: {
      placeholder: '请输入配置键',
    },
  },
  {
    field: 'exportName',
    label: '导出名称',
    type: 'input',
    props: {
      placeholder: '请输入导出名称',
    },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: [
        { label: '启用', value: 1 },
        { label: '禁用', value: 0 },
      ],
    },
  },
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'configKey',
    label: '配置键',
    minWidth: 180,
    showOverflowTooltip: true,
  },
  {
    prop: 'exportName',
    label: '导出名称',
    minWidth: 150,
  },
  {
    prop: 'sheetName',
    label: 'Sheet名称',
    width: 120,
  },
  {
    prop: 'dataSourceBean',
    label: '数据源Bean',
    minWidth: 150,
    showOverflowTooltip: true,
  },
  {
    prop: 'queryMethod',
    label: '查询方法',
    width: 120,
  },
  {
    prop: 'maxRows',
    label: '最大行数',
    width: 100,
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(NTag, {
        type: row.status === 1 ? 'success' : 'default',
        size: 'small',
      }, {
        default: () => row.status === 1 ? '启用' : '禁用',
      })
    },
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', onClick: handleEdit },
      { label: '列配置', key: 'columns', type: 'info', onClick: handleManageColumns },
      { label: '导出测试', key: 'test', onClick: handleTestExport },
      { label: '复制配置', key: 'copy', onClick: handleCopy },
      { label: '禁用', key: 'disable', type: 'warning', onClick: handleToggleStatus, visible: row => row.status === 1 },
      { label: '启用', key: 'enable', type: 'success', onClick: handleToggleStatus, visible: row => row.status !== 1 },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置
const editSchema = [
  // ==================== 基础信息 ====================
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'configKey',
    label: '配置键',
    type: 'input',
    rules: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
    props: {
      placeholder: '请输入唯一配置键，如：user_list_export',
    },
  },
  {
    field: 'exportName',
    label: '导出名称',
    type: 'input',
    rules: [{ required: true, message: '请输入导出名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入导出名称，如：用户列表导出',
    },
  },
  {
    field: 'sheetName',
    label: 'Sheet名称',
    type: 'input',
    props: {
      placeholder: '请输入Sheet名称，默认：Sheet1',
    },
  },
  {
    field: 'fileNameTemplate',
    label: '文件名模板',
    type: 'input',
    span: 2,
    props: {
      placeholder: '支持占位符：{date}、{time}，如：用户列表_{date}.xlsx',
    },
    help: '支持占位符：{date}（日期如20240101）、{time}（时间如120530）',
  },

  // ==================== 数据源配置 ====================
  {
    type: 'divider',
    label: '数据源配置',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'dataSourceBean',
    label: '数据源Bean',
    type: 'input',
    rules: [{ required: true, message: '请输入数据源Bean名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入Service Bean名称，如：sysUserService',
    },
  },
  {
    field: 'queryMethod',
    label: '查询方法',
    type: 'input',
    rules: [{ required: true, message: '请输入查询方法名', trigger: 'blur' }],
    props: {
      placeholder: '请输入查询方法名，如：list、page',
    },
  },
  {
    field: 'pageable',
    label: '分页查询',
    type: 'switch',
    props: {
      checkedValue: true,
      uncheckedValue: false,
    },
  },
  {
    field: 'maxRows',
    label: '最大行数',
    type: 'input-number',
    props: {
      placeholder: '最大导出条数',
      min: 1,
      max: 1000000,
    },
  },

  // ==================== 高级配置 ====================
  {
    type: 'divider',
    label: '高级配置',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'autoTrans',
    label: '字典翻译',
    type: 'switch',
    props: {
      checkedValue: true,
      uncheckedValue: false,
    },
    help: '是否自动翻译字典类型字段',
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      options: [
        { label: '启用', value: 1 },
        { label: '禁用', value: 0 },
      ],
    },
  },
  {
    field: 'sortField',
    label: '排序字段',
    type: 'input',
    props: {
      placeholder: '请输入排序字段名',
    },
  },
  {
    field: 'sortOrder',
    label: '排序方向',
    type: 'select',
    props: {
      options: [
        { label: '升序', value: 'ASC' },
        { label: '降序', value: 'DESC' },
      ],
    },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注信息',
      rows: 3,
    },
  },
]

// 刷新
function handleRefresh() {
  crudRef.value?.reload()
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 获取更多操作菜单
function getMoreOptions(row) {
  return [
    {
      label: '导出测试',
      key: 'test',
      icon: () => h('i', { class: 'i-material-symbols:download' }),
    },
    {
      label: '复制配置',
      key: 'copy',
      icon: () => h('i', { class: 'i-material-symbols:content-copy' }),
    },
    {
      label: row.status === 1 ? '禁用' : '启用',
      key: 'toggle',
      icon: () => h('i', { class: row.status === 1 ? 'i-material-symbols:block' : 'i-material-symbols:check-circle' }),
    },
    {
      type: 'divider',
    },
    {
      label: '删除',
      key: 'delete',
      icon: () => h('i', { class: 'i-material-symbols:delete' }),
      props: {
        style: 'color: #d03050',
      },
    },
  ]
}

// 处理更多操作
function handleMoreAction(key, row) {
  switch (key) {
    case 'test':
      handleTestExport(row)
      break
    case 'copy':
      handleCopy(row)
      break
    case 'toggle':
      handleToggleStatus(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除导出配置“${row.exportName}”吗？删除后将同时删除所有关联的列配置！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.delete(`/system/excel/export-config/${row.id}`)
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.reload()
        }
        else {
          window.$message.error(res.respMsg || '删除失败')
        }
      }
      catch (error) {
        window.$message.error(`删除失败：${error.message || '未知错误'}`)
      }
    },
  })
}

// 管理列配置
function handleManageColumns(row) {
  currentConfigKey.value = row.configKey
  currentConfigName.value = row.exportName
  showColumnModal.value = true
}

// 导出测试
async function handleTestExport(row) {
  try {
    // 使用系统统一的请求前缀
    const token = authStore.accessToken
    const baseUrl = import.meta.env.VITE_REQUEST_PREFIX || '/dev-api'
    const url = `${baseUrl}/system/excel/export-config/test/${row.id}`

    console.log('导出测试URL:', url)
    console.log('Token:', token ? 'exists' : 'missing')

    // 使用 fetch 下载文件
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        Authorization: token ? `Bearer ${token}` : '',
      },
    })

    if (!response.ok) {
      const error = await response.text()
      console.error('导出失败响应:', error)
      throw new Error(error || '导出失败')
    }

    // 获取文件名（从 Content-Disposition 头中获取）
    const contentDisposition = response.headers.get('Content-Disposition')
    let fileName = `${row.exportName}_测试.xlsx`
    if (contentDisposition) {
      const match = contentDisposition.match(/filename\*=utf-8''(.+)/)
      if (match) {
        fileName = decodeURIComponent(match[1])
      }
    }

    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)

    // 创建下载链接
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    // 释放 blob URL
    setTimeout(() => URL.revokeObjectURL(blobUrl), 100)

    window.$message.success('导出成功')
  }
  catch (error) {
    console.error('导出测试失败:', error)
    window.$message.error(`导出失败：${error.message || '未知错误'}`)
  }
}

// 复制配置
function handleCopy(row) {
  copyForm.value = {
    sourceId: row.id,
    newConfigKey: `${row.configKey}_copy`,
  }
  showCopyModal.value = true
}

// 确认复制
async function handleConfirmCopy() {
  try {
    await copyFormRef.value?.validate()
    const loading = window.$message.loading('正在复制配置...', { duration: 0 })

    try {
      const res = await request.post('/system/excel/export-config/copy', null, {
        params: {
          id: copyForm.value.sourceId,
          newConfigKey: copyForm.value.newConfigKey,
        },
      })

      if (res.code === 200) {
        window.$message.success('复制成功')
        showCopyModal.value = false
        crudRef.value?.reload()
      }
      else {
        window.$message.error(res.respMsg || '复制失败')
      }
    }
    finally {
      loading.destroy()
    }
  }
  catch (error) {
    if (error?.message !== '验证失败') {
      window.$message.error(`复制失败：${error.message || '未知错误'}`)
    }
  }
}

// 切换状态
async function handleToggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1

  try {
    const res = await request.put('/system/excel/export-config/status', null, {
      params: {
        id: row.id,
        status: newStatus,
      },
    })

    if (res.code === 200) {
      window.$message.success('状态更新成功')
    }
    else {
      window.$message.error(res.respMsg || '状态更新失败')
    }
  }
  catch (error) {
    window.$message.error(`状态更新失败：${error.message || '未知错误'}`)
  }
}
</script>

<style scoped>
.excel-export-config-page {
  height: 100%;
}
</style>
