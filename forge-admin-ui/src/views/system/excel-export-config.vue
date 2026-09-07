<template>
  <div class="excel-export-config-page">
    <div class="excel-page-context">
      <div class="excel-page-context__identity">
        <span class="excel-page-context__icon i-lucide:file-spreadsheet" aria-hidden="true" />
        <div>
          <div class="excel-page-context__title">
            导出方案
          </div>
          <div class="excel-page-context__description">
            维护各业务页面的下载文件和导入模板，先设置方案，再调整列顺序。
          </div>
        </div>
      </div>
      <span class="excel-page-context__tip">配置保存后，新下载立即生效</span>
    </div>
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/system/excel/export-config/page',
        detail: 'post@/system/excel/export-config/detail',
        add: 'post@/system/excel/export-config',
        update: 'put@/system/excel/export-config',
        delete: 'delete@/system/excel/export-config/:id',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-submit="normalizeConfigBeforeSubmit"
      :before-render-form="beforeRenderForm"
      row-key="id"
      add-button-text="新建导出方案"
      :load-detail-on-edit="true"
      :edit-grid-cols="1"
      edit-label-width="112px"
      form-open-mode="drawer"
      :modal-width="isNarrow ? '100vw' : '720px'"
      :hide-form-section-nav="true"
      :search-grid-cols="isNarrow ? 1 : 3"
      :search-max-visible-fields="3"
      :search-y-gap="8"
      :scroll-x="isNarrow ? 338 : 980"
      :table-props="{ showRefresh: false }"
      :hide-selection="true"
      :hide-batch-delete="true"
      :show-render-mode-switch="false"
    >
      <template #toolbar-end>
        <NButton size="small" @click="handleRefresh">
          <template #icon>
            <span class="i-lucide:refresh-cw" aria-hidden="true" />
          </template>
          刷新列表
        </NButton>
      </template>
      <template #toolbar-right-start>
        <span class="excel-toolbar-hint">管理下载文件、导入模板与展示列</span>
      </template>

      <template #form-planIntro>
        <div class="form-intro">
          <span class="form-intro__icon i-lucide:file-check-2" aria-hidden="true" />
          <div>
            <strong>先告诉我们这份文件给谁用</strong>
            <p>方案名称和用途会显示在业务页面，技术绑定信息已放到下方的高级设置中。</p>
          </div>
        </div>
      </template>

      <template #form-technicalSettings="{ formData }">
        <NCollapse v-model:expanded-names="technicalExpanded">
          <NCollapseItem name="technical" title="高级设置：连接业务数据">
            <template #header-extra>
              <span class="technical-summary">
                {{ hasTechnicalBinding(formData) ? '已完成绑定' : '待技术人员完善' }}
              </span>
            </template>
            <div class="technical-settings">
              <p class="technical-settings__intro">
                这部分决定系统从哪里读取数据。日常调整文件名称和列顺序时无需修改。
              </p>
              <label class="technical-field">
                <span>数据来源服务 <em>*</em></span>
                <NInput
                  :value="formData.dataSourceBean"
                  placeholder="由技术人员填写，例如：sysUserService"
                  @update:value="value => patchForm(formData, 'dataSourceBean', value)"
                />
              </label>
              <label class="technical-field">
                <span>查询方式 <em>*</em></span>
                <NInput
                  :value="formData.queryMethod"
                  placeholder="由技术人员填写，例如：page 或 list"
                  @update:value="value => patchForm(formData, 'queryMethod', value)"
                />
              </label>
              <label class="technical-field technical-field--switch">
                <span>分批读取数据</span>
                <div>
                  <NSwitch
                    :value="formData.pageable === true"
                    aria-label="分批读取数据"
                    @update:value="value => patchForm(formData, 'pageable', value)"
                  />
                  <small>数据量较大时建议开启，降低一次查询压力。</small>
                </div>
              </label>
              <div class="technical-field-grid">
                <label class="technical-field">
                  <span>默认排序字段</span>
                  <NInput
                    :value="formData.sortField"
                    placeholder="例如：create_time"
                    @update:value="value => patchForm(formData, 'sortField', value)"
                  />
                </label>
                <label class="technical-field">
                  <span>排序方向</span>
                  <NSelect
                    :value="formData.sortOrder"
                    :options="sortOrderOptions"
                    clearable
                    placeholder="选择升序或降序"
                    @update:value="value => patchForm(formData, 'sortOrder', value)"
                  />
                </label>
              </div>
            </div>
          </NCollapseItem>
        </NCollapse>
      </template>
    </AiCrudPage>

    <!-- 列配置管理弹窗 -->
    <NModal
      v-model:show="showColumnModal"
      preset="card"
      :title="`${currentConfigName || '导出方案'} · 文件列`"
      :style="{ width: isNarrow ? '100vw' : 'min(1120px, 94vw)' }"
      :mask-closable="false"
      :close-on-esc="false"
      :closable="false"
      class="excel-column-modal"
    >
      <ExcelColumnConfig
        v-if="showColumnModal"
        :config-key="currentConfigKey"
        :config-name="currentConfigName"
        :config-type="currentConfigType"
        @close="showColumnModal = false"
      />
    </NModal>

    <!-- 复制配置弹窗 -->
    <NModal
      v-model:show="showCopyModal"
      preset="dialog"
      title="复制配置"
      positive-text="创建副本"
      negative-text="取消"
      @positive-click="handleConfirmCopy"
    >
      <div class="copy-intro">
        <span class="copy-intro__icon i-lucide:copy" aria-hidden="true" />
        <div>
          <strong>复制“{{ copySourceName || '当前方案' }}”</strong>
          <p>副本会默认停用，确认内容无误后再启用，不会影响原方案。</p>
        </div>
      </div>
      <NForm ref="copyFormRef" :model="copyForm" label-placement="top">
        <NFormItem
          label="副本标识（系统识别用）"
          path="newConfigKey"
          :rule="{ required: true, message: '请输入副本标识', trigger: 'blur' }"
        >
          <NInput
            v-model:value="copyForm.newConfigKey"
            placeholder="英文、数字或下划线，例如：user_list_export_v2"
          />
          <template #feedback>
            仅用于系统区分方案，业务展示名称会自动标记为“副本”。
          </template>
        </NFormItem>
      </NForm>
    </NModal>
  </div>
</template>

<script setup>
import { useMediaQuery } from '@vueuse/core'
import { NButton, NCollapse, NCollapseItem, NDropdown, NForm, NFormItem, NInput, NModal, NSelect, NSwitch } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { managedFetch } from '@/composables/useGlobalLoading'
import { useAuthStore } from '@/store'
import { request } from '@/utils/request'
import ExcelConfigStatusSwitch from './components/ExcelConfigStatusSwitch.vue'
import ExcelColumnConfig from './excel-column-config.vue'

defineOptions({ name: 'ExcelExportConfig' })

const CONFIG_TYPE_DICT = 'sys_excel_config_type'
const STATUS_DICT = 'sys_enable_disable'
const sortOrderOptions = [
  { label: '升序', value: 'ASC' },
  { label: '降序', value: 'DESC' },
]

const authStore = useAuthStore()
const crudRef = ref(null)
const isNarrow = useMediaQuery('(max-width: 640px)')
const showColumnModal = ref(false)
const showCopyModal = ref(false)
const copyFormRef = ref(null)
const technicalExpanded = ref([])
const formEditing = ref(false)
const currentConfigKey = ref('')
const currentConfigName = ref('')
const currentConfigType = ref('BOTH')
const copySourceName = ref('')
const copyForm = ref({
  sourceId: null,
  newConfigKey: '',
})
const { dict } = useDict(CONFIG_TYPE_DICT, STATUS_DICT)
const configTypeOptions = computed(() => dict.value[CONFIG_TYPE_DICT] || [])
const statusOptions = computed(() => toNumberOptions(dict.value[STATUS_DICT]))

// 搜索表单配置
const searchSchema = computed(() => [
  {
    field: 'exportName',
    label: '方案名称',
    type: 'input',
    props: {
      placeholder: '搜索业务名称，如用户列表',
    },
  },
  {
    field: 'configType',
    label: '使用方式',
    type: 'select',
    props: {
      placeholder: '全部方式',
      clearable: true,
      options: configTypeOptions.value,
    },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '全部状态',
      clearable: true,
      options: statusOptions.value,
    },
  },
  {
    field: 'configKey',
    label: '系统标识',
    type: 'input',
    props: {
      placeholder: '按系统标识精确查找',
    },
  },
])

// 表格列配置
const tableColumns = computed(() => {
  const nameColumn = {
    prop: 'exportName',
    label: '方案名称',
    minWidth: isNarrow.value ? 190 : 240,
    render: row => h(SystemTableCell, {
      title: row.exportName || '未命名方案',
      subtitle: row.configKey,
      interactive: true,
      tooltip: `编辑方案：${row.exportName || row.configKey || '-'}`,
      onActivate: () => handleEdit(row),
    }),
  }
  const statusColumn = {
    prop: 'status',
    label: '启用状态',
    width: 126,
    render: row => renderStatusSwitch(row),
  }

  if (isNarrow.value) {
    return [
      nameColumn,
      statusColumn,
      {
        prop: 'action',
        label: '操作',
        width: 48,
        fixed: 'right',
        align: 'center',
        render: row => renderMobileActions(row),
      },
    ]
  }

  return [
    nameColumn,
    {
      prop: 'configType',
      label: '使用方式',
      width: 108,
      render: row => h(DictTag, { dictType: CONFIG_TYPE_DICT, value: row.configType || 'BOTH', size: 'small' }),
    },
    {
      prop: 'fileNameTemplate',
      label: '下载文件',
      minWidth: 190,
      render: row => renderFileSummary(row),
    },
    {
      prop: 'maxRows',
      label: '数据量限制',
      width: 112,
      render: row => h('span', { class: 'capacity-text' }, formatCapacity(row)),
    },
    statusColumn,
    {
      prop: 'updateTime',
      label: '最近更新',
      width: 164,
      render: row => row.updateTime || row.createTime || '-',
    },
    {
      prop: 'action',
      label: '操作',
      width: 168,
      fixed: 'right',
      actions: [
        { label: '编辑', key: 'edit', onClick: handleEdit },
        { label: '设置文件列', key: 'columns', type: 'info', onClick: handleManageColumns },
        { label: '下载预览', key: 'test', onClick: handleTestExport, visible: row => isExportConfig(row) && Number(row.status) === 1 },
        { label: '创建副本', key: 'copy', onClick: handleCopy },
        { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
      ],
    },
  ]
})

function renderMobileActions(row) {
  const options = [
    { label: '编辑方案', key: 'edit' },
    { label: '设置文件列', key: 'columns' },
  ]
  if (isExportConfig(row) && Number(row.status) === 1) {
    options.push({ label: '下载预览', key: 'preview' })
  }
  options.push(
    { label: '创建副本', key: 'copy' },
    { type: 'divider', key: 'divider' },
    { label: '删除方案', key: 'delete' },
  )
  return h(NDropdown, {
    trigger: 'click',
    options,
    onSelect: key => handleMobileAction(key, row),
  }, {
    default: () => h(NButton, {
      'text': true,
      'aria-label': `${row.exportName || '此方案'}的更多操作`,
    }, { default: () => h('span', { 'class': 'i-lucide:ellipsis', 'aria-hidden': true }) }),
  })
}

function handleMobileAction(key, row) {
  const actions = {
    edit: handleEdit,
    columns: handleManageColumns,
    preview: handleTestExport,
    copy: handleCopy,
    delete: handleDelete,
  }
  actions[key]?.(row)
}

function beforeRenderForm(row) {
  formEditing.value = Boolean(row)
  technicalExpanded.value = row && !hasTechnicalBinding(row) ? ['technical'] : []
  if (!row) {
    return {
      id: null,
      configType: 'BOTH',
      status: 1,
      sheetName: 'Sheet1',
      maxRows: 10000,
      autoTrans: true,
      pageable: false,
      includeSample: false,
    }
  }
  return {
    ...row,
    status: Number(row.status) === 0 ? 0 : 1,
    configType: row.configType || 'BOTH',
    sheetName: row.sheetName || 'Sheet1',
    maxRows: row.maxRows || 10000,
    autoTrans: row.autoTrans !== false,
    pageable: row.pageable === true,
    includeSample: row.includeSample === true,
  }
}

function renderFileSummary(row) {
  if (!isExportConfig(row)) {
    return h('span', { class: 'empty-text' }, '导入模板')
  }
  return h(SystemTableCell, {
    title: row.fileNameTemplate || `${row.exportName || '导出文件'}.xlsx`,
    subtitle: `工作表：${row.sheetName || 'Sheet1'}`,
  })
}

function formatCapacity(row) {
  if (!isExportConfig(row)) {
    return row.includeSample ? '模板含示例' : '空白模板'
  }
  const maxRows = Number(row.maxRows)
  if (!Number.isFinite(maxRows) || maxRows <= 0) {
    return '系统默认'
  }
  return `最多 ${maxRows.toLocaleString()} 行`
}

function renderStatusSwitch(row) {
  return h(ExcelConfigStatusSwitch, {
    row,
    options: statusOptions.value,
    onRefresh: () => crudRef.value?.refresh(),
  })
}

function hasTechnicalBinding(formData) {
  return Boolean(String(formData?.dataSourceBean || '').trim() && String(formData?.queryMethod || '').trim())
}

function patchForm(formData, field, value) {
  formData[field] = value
}

// 编辑表单配置
const editSchema = computed(() => [
  {
    field: 'planIntro',
    type: 'slot',
    slotName: 'planIntro',
    showLabel: false,
  },
  {
    type: 'divider',
    label: '方案信息',
    props: {
      titlePlacement: 'left',
    },
    span: 1,
  },
  {
    field: 'exportName',
    label: '方案名称',
    type: 'input',
    rules: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
    props: {
      placeholder: '让业务人员一眼认出用途，如：用户列表导出',
      maxlength: 100,
      showCount: true,
    },
  },
  {
    field: 'configType',
    label: '使用方式',
    type: 'radio',
    defaultValue: 'BOTH',
    rules: [{ required: true, message: '请选择使用方式', trigger: 'change' }],
    props: {
      options: configTypeOptions.value,
    },
    help: '仅导出用于下载数据，仅导入用于下载模板，导入导出同时支持两种操作。',
  },
  {
    field: 'configKey',
    label: '系统标识',
    type: 'input',
    disabled: formEditing.value,
    rules: [{ required: true, message: '请输入系统标识', trigger: 'blur' }],
    props: {
      placeholder: '英文、数字或下划线，例如：user_list_export',
    },
    help: '由技术人员提供，保存后不建议修改。',
  },
  {
    type: 'divider',
    label: '下载文件',
    props: {
      titlePlacement: 'left',
    },
    span: 1,
    vIf: formData => isExportType(formData.configType),
  },
  {
    field: 'sheetName',
    label: '工作表名称',
    type: 'input',
    vIf: formData => isExportType(formData.configType),
    props: {
      placeholder: 'Excel 底部页签名称，如：用户列表',
      maxlength: 31,
    },
    help: '留空时使用“Sheet1”。',
  },
  {
    field: 'fileNameTemplate',
    label: '下载文件名',
    type: 'input',
    vIf: formData => isExportType(formData.configType),
    props: {
      placeholder: '例如：用户列表_{date}.xlsx',
    },
    help: '可插入 {date} 和 {time}，系统下载时会自动替换为当前日期和时间。',
  },
  {
    field: 'maxRows',
    label: '最多导出',
    type: 'number',
    defaultValue: 10000,
    vIf: formData => isExportType(formData.configType),
    props: {
      placeholder: '单次最多导出多少行',
      min: 1,
      max: 1000000,
    },
    help: '建议按实际业务量设置，避免一次下载过多数据。',
  },
  {
    field: 'autoTrans',
    label: '显示业务名称',
    type: 'switch',
    defaultValue: true,
    vIf: formData => isExportType(formData.configType),
    props: {
      checkedValue: true,
      uncheckedValue: false,
    },
    help: '开启后，状态、类型等编码会优先导出为易读的中文名称。',
  },
  {
    field: 'technicalSettings',
    type: 'slot',
    slotName: 'technicalSettings',
    showLabel: false,
    vIf: formData => isExportType(formData.configType),
  },
  {
    type: 'divider',
    label: '导入模板',
    props: {
      titlePlacement: 'left',
    },
    span: 1,
    vIf: formData => isImportType(formData.configType),
  },
  {
    field: 'includeSample',
    label: '附带示例数据',
    type: 'switch',
    defaultValue: false,
    vIf: formData => isImportType(formData.configType),
    props: {
      checkedValue: true,
      uncheckedValue: false,
    },
    help: '开启后，下载的导入模板会包含一行填写示例。',
  },
  {
    field: 'remark',
    label: '使用说明',
    type: 'textarea',
    props: {
      placeholder: '说明适用页面、使用对象或注意事项，方便其他管理员理解',
      rows: 3,
      maxlength: 500,
      showCount: true,
    },
  },
])

function isExportConfig(row) {
  return isExportType(row?.configType)
}

function isExportType(configType) {
  return (configType || 'BOTH') !== 'IMPORT'
}

function isImportType(configType) {
  return (configType || 'BOTH') !== 'EXPORT'
}

function normalizeConfigBeforeSubmit(formData) {
  const configType = formData.configType || 'BOTH'
  const exportEnabled = isExportType(configType)
  if (exportEnabled && (!formData.dataSourceBean || !formData.queryMethod)) {
    technicalExpanded.value = ['technical']
    window.$message.error('请让技术人员完善“高级设置”中的数据来源服务和查询方式')
    return false
  }
  const payload = {
    ...formData,
    configKey: String(formData.configKey || '').trim(),
    exportName: String(formData.exportName || '').trim(),
    configType,
    allowImport: configType !== 'EXPORT',
    dataSourceBean: exportEnabled ? String(formData.dataSourceBean || '').trim() : null,
    queryMethod: exportEnabled ? String(formData.queryMethod || '').trim() : null,
    maxRows: exportEnabled ? formData.maxRows : null,
    pageable: exportEnabled ? formData.pageable : false,
    sortField: exportEnabled ? String(formData.sortField || '').trim() || null : null,
    sortOrder: exportEnabled ? String(formData.sortOrder || '').trim().toUpperCase() || null : null,
  }
  if (formData.id) {
    delete payload.status
  }
  else {
    payload.status = 1
  }
  delete payload.planIntro
  delete payload.technicalSettings
  return payload
}

function toNumberOptions(options = []) {
  return (options || []).map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

// 刷新
function handleRefresh() {
  crudRef.value?.refresh()
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit({
    ...row,
    __modalTitle: `编辑“${row.exportName || row.configKey || '导出方案'}”`,
  })
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: `删除“${row.exportName || row.configKey}”？`,
    content: '删除后，业务页面将无法再使用此方案，关联的文件列设置也会一并删除且无法恢复。若只是暂时不用，建议关闭列表中的启用开关。',
    positiveText: '确认删除',
    negativeText: '取消',
    maskClosable: false,
    onPositiveClick: async () => {
      try {
        const res = await request.delete(`/system/excel/export-config/${row.id}`)
        if (res.code === 200) {
          window.$message.success('导出方案已删除')
          crudRef.value?.refresh()
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
  if (!row?.configKey) {
    window.$message.error('当前方案缺少系统标识，无法设置文件列')
    return
  }
  currentConfigKey.value = row.configKey
  currentConfigName.value = row.exportName
  currentConfigType.value = row.configType || 'BOTH'
  showColumnModal.value = true
}

// 导出测试
async function handleTestExport(row) {
  if (!isExportConfig(row)) {
    window.$message.warning('仅导入方案没有可预览的下载文件')
    return
  }
  if (Number(row.status) !== 1) {
    window.$message.warning('请先启用方案，再下载预览文件')
    return
  }
  try {
    // 使用系统统一的请求前缀
    const token = authStore.accessToken
    const baseUrl = import.meta.env.VITE_REQUEST_PREFIX || '/dev-api'
    const url = `${baseUrl}/system/excel/export-config/test/${row.id}`

    // 使用 fetch 下载文件
    const response = await managedFetch(url, {
      method: 'GET',
      headers: {
        Authorization: token ? `Bearer ${token}` : '',
      },
    }, {
      globalLoadingType: 'export',
      globalLoadingText: '正在生成预览文件，请稍候...',
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

    window.$message.success('预览文件已下载')
  }
  catch (error) {
    console.error('导出测试失败:', error)
    window.$message.error(`生成预览文件失败：${error.message || '未知错误'}`)
  }
}

// 复制配置
function handleCopy(row) {
  copySourceName.value = row.exportName || row.configKey || ''
  copyForm.value = {
    sourceId: row.id,
    newConfigKey: `${row.configKey}_copy`,
  }
  showCopyModal.value = true
}

// 确认复制
async function handleConfirmCopy() {
  const messageKey = 'excel-config-copy'

  try {
    await copyFormRef.value?.validate()
    window.$message.loading('正在创建副本...', {
      key: messageKey,
      duration: 600000,
    })

    try {
      const res = await request.post('/system/excel/export-config/copy', null, {
        params: {
          id: copyForm.value.sourceId,
          newConfigKey: copyForm.value.newConfigKey,
        },
      })

      if (res.code === 200) {
        window.$message.success('副本已创建，默认处于停用状态')
        showCopyModal.value = false
        crudRef.value?.refresh()
        return true
      }
      window.$message.error(res.respMsg || '创建副本失败')
      return false
    }
    finally {
      window.$message.destroy?.(messageKey, 0)
    }
  }
  catch (error) {
    if (!Array.isArray(error) && error?.message !== '验证失败') {
      window.$message.error(`创建副本失败：${error.message || '未知错误'}`)
    }
    return false
  }
}
</script>

<style scoped>
.excel-export-config-page {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
}

.excel-page-context {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  padding: 6px 12px;
  border: 1px solid var(--border-light);
  border-bottom: 0;
  border-radius: 6px 6px 0 0;
  background: var(--bg-primary);
}

.excel-page-context__identity {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.excel-page-context__icon {
  flex: 0 0 auto;
  color: var(--primary-color);
  font-size: 22px;
}

.excel-page-context__title {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
}

.excel-page-context__description,
.excel-page-context__tip,
.excel-toolbar-hint {
  color: var(--text-tertiary);
  font-size: 12px;
}

.excel-page-context__description {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.excel-page-context__tip {
  flex: 0 0 auto;
  margin-left: 16px;
}

:deep(.ai-crud-page) {
  flex: 1;
  min-height: 0;
  border-radius: 0 0 6px 6px;
}

.form-intro,
.copy-intro {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--bg-secondary);
}

.form-intro__icon,
.copy-intro__icon {
  flex: 0 0 auto;
  margin-top: 1px;
  color: var(--primary-color);
  font-size: 18px;
}

.form-intro strong,
.copy-intro strong {
  display: block;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.form-intro p,
.copy-intro p,
.technical-settings__intro {
  margin: 3px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.6;
}

.copy-intro {
  margin-bottom: 14px;
}

.technical-summary {
  color: var(--text-tertiary);
  font-size: 12px;
  font-weight: 400;
}

.technical-settings {
  display: grid;
  gap: 12px;
  padding: 2px 0 4px;
}

.technical-settings__intro {
  margin: 0;
}

.technical-field {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  color: var(--text-secondary);
  font-size: 13px;
}

.technical-field > span em {
  color: var(--error-color);
  font-style: normal;
}

.technical-field--switch {
  align-items: start;
}

.technical-field--switch > span {
  padding-top: 3px;
}

.technical-field--switch > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.technical-field small {
  color: var(--text-tertiary);
  font-size: 12px;
}

.technical-field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.technical-field-grid .technical-field {
  grid-template-columns: 1fr;
  gap: 6px;
  align-items: start;
}

:deep(.capacity-text),
:deep(.empty-text) {
  color: var(--text-secondary);
  font-size: 12px;
}

@media (max-width: 640px) {
  .excel-page-context {
    align-items: flex-start;
  }

  .excel-page-context__tip,
  .excel-toolbar-hint {
    display: none;
  }

  .excel-page-context__description {
    white-space: normal;
  }

  .technical-field,
  .technical-field-grid {
    grid-template-columns: 1fr;
  }

  .technical-field {
    gap: 6px;
  }

  .technical-field--switch > div {
    align-items: flex-start;
  }
}
</style>
