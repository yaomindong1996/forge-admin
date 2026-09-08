<template>
  <div class="data-connection-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/data/connection/page',
        detail: 'get@/data/connection/:id',
        add: 'post@/data/connection',
        update: 'put@/data/connection',
        delete: 'delete@/data/connection/:id',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-render-form="beforeRenderForm"
      :before-render-detail="beforeRenderDetail"
      :before-submit="beforeSubmit"
      row-key="id"
      :scroll-x="1100"
      :edit-grid-cols="2"
      edit-label-placement="left"
      edit-label-align="left"
      edit-label-width="92px"
      edit-form-class="data-connection-edit-form"
      modal-width="960px"
      add-button-text="新增连接"
      :load-detail-on-edit="true"
      :hide-selection="true"
      :hide-batch-delete="true"
      :search-grid-cols="3"
      :search-max-visible-fields="3"
      :search-y-gap="8"
      search-label-width="72px"
      @modal-close="handleModalClose"
    >
      <template #form-dbType="{ formData }">
        <n-select
          :value="formData.dbType"
          :options="dbTypeOptions"
          placeholder="请选择数据库类型"
          @update:value="value => handleDbTypeChange(formData, value)"
        />
      </template>

      <template #form-host="{ formData }">
        <n-input
          :value="formData.host"
          placeholder="例如 127.0.0.1"
          @update:value="value => updateAccessField(formData, 'host', value)"
        />
      </template>

      <template #form-port="{ formData }">
        <n-input
          :value="formData.port"
          placeholder="例如 3306"
          @update:value="value => updateAccessField(formData, 'port', value)"
        />
      </template>

      <template #form-database="{ formData }">
        <n-input
          :value="formData.database"
          placeholder="库名 / Schema"
          @update:value="value => updateAccessField(formData, 'database', value)"
        />
      </template>

      <template #form-connectionPreview="{ formData }">
        <p class="connection-inline-tip">
          将连到 {{ formData.jdbcUrl || '待生成连接地址' }}
        </p>
      </template>

      <template #form-jdbcUrl="{ formData }">
        <n-input
          :value="formData.jdbcUrl"
          placeholder="自动根据上面的地址生成"
          @update:value="value => updateJdbcUrl(formData, value)"
        />
      </template>

      <template #form-connectionProbe="{ formData }">
        <div class="connection-probe-bar">
          <n-button
            size="small"
            :loading="connectionProbeLoading"
            @click="handleProbeConnection(formData)"
          >
            测试能否连上
          </n-button>
          <span class="connection-inline-tip">
            {{ currentEditingConnection?.id && !formData.password ? '未填密码时测已保存的连接。' : '先测通再保存。' }}
          </span>
        </div>
      </template>
    </AiCrudPage>

    <n-modal
      v-model:show="tableModalVisible"
      preset="card"
      :title="tableModalTitle"
      style="width: 980px"
      :segmented="{ content: 'soft' }"
    >
      <div class="modal-toolbar">
        <n-input
          v-model:value="tableKeyword"
          clearable
          placeholder="按表名或注释搜索"
          style="width: 260px"
          @keyup.enter="loadConnectionTables"
        />
        <n-button type="primary" :loading="tableLoading" @click="loadConnectionTables">
          查询
        </n-button>
      </div>
      <n-data-table
        :columns="connectionTableColumns"
        :data="connectionTables"
        :loading="tableLoading"
        :pagination="{ pageSize: 10 }"
        size="small"
        striped
      />
    </n-modal>

    <n-modal
      v-model:show="fieldModalVisible"
      preset="card"
      :title="fieldModalTitle"
      style="width: 900px"
      :segmented="{ content: 'soft' }"
    >
      <n-data-table
        :columns="fieldColumns"
        :data="fieldRows"
        :loading="fieldLoading"
        :pagination="{ pageSize: 10 }"
        size="small"
        striped
      />
    </n-modal>
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, ref } from 'vue'
import {
  deleteDataConnection,
  getDataConnectionFields,
  getDataConnectionTables,
  testDataConnection,
  testDataConnectionTemp,
} from '@/api/data/connection'
import { AiCrudPage } from '@/components/ai-form'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { toNumberDictOptions } from '@/utils/dict-options'
import {
  applyDbTypeDefaults,
  DRIVER_CLASS_MAP,
  getJdbcEndpoint,
  hydrateConnectionForm,
  suggestConnectionCode,
  syncAccessToJdbcUrl,
  syncJdbcUrlToAccess,
} from './connection-form'

defineOptions({ name: 'DataConnection' })

const { dict } = useDict('data_db_type', 'sys_enable_disable')

const crudRef = ref(null)
const tableModalVisible = ref(false)
const tableModalTitle = ref('数据表')
const tableLoading = ref(false)
const tableKeyword = ref('')
const connectionTables = ref([])
const currentConnection = ref(null)
const currentEditingConnection = ref(null)
const connectionProbeLoading = ref(false)
const fieldModalVisible = ref(false)
const fieldModalTitle = ref('字段列表')
const fieldLoading = ref(false)
const fieldRows = ref([])
const testingConnectionIds = ref(new Set())

const dbTypeOptions = computed(() => dict.value.data_db_type || [])
const statusOptions = computed(() => toNumberDictOptions(dict.value.sys_enable_disable))

function setRowTesting(id, loading) {
  const next = new Set(testingConnectionIds.value)
  if (loading)
    next.add(id)
  else
    next.delete(id)
  testingConnectionIds.value = next
}

const searchSchema = computed(() => [
  {
    field: 'connectionName',
    label: '连接名称',
    type: 'input',
    props: {
      placeholder: '请输入连接名称',
      clearable: true,
    },
  },
  {
    field: 'dbType',
    label: '数据库',
    type: 'select',
    props: {
      placeholder: '请选择数据库类型',
      options: dbTypeOptions.value,
      clearable: true,
    },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: statusOptions.value,
      clearable: true,
    },
  },
])

const tableColumns = computed(() => {
  const testingIds = testingConnectionIds.value
  return [
    {
      prop: 'connectionName',
      label: '连接',
      minWidth: 220,
      render: row => h(SystemTableCell, {
        title: row.connectionName || '-',
        subtitle: row.connectionCode && row.connectionCode !== row.connectionName
          ? row.connectionCode
          : (row.connectionCode ? '' : '未设置编码'),
        interactive: true,
        tooltip: `编辑连接：${row.connectionName || row.connectionCode || '-'}`,
        onActivate: () => handleEdit(row),
      }),
    },
    {
      prop: 'dbType',
      label: '数据库',
      width: 110,
      render: row => h(DictTag, {
        dictType: 'data_db_type',
        value: row.dbType,
        size: 'small',
      }),
    },
    {
      prop: 'jdbcUrl',
      label: '地址',
      minWidth: 240,
      ellipsis: { tooltip: true },
      render: row => getJdbcEndpoint(row.jdbcUrl) || row.jdbcUrl || '-',
    },
    {
      prop: 'username',
      label: '账号',
      width: 120,
      ellipsis: { tooltip: true },
    },
    {
      prop: 'status',
      label: '状态',
      width: 110,
      render: row => h(DictTag, {
        dictType: 'sys_enable_disable',
        value: String(row.status),
        size: 'small',
      }),
    },
    {
      prop: 'updateTime',
      label: '更新时间',
      width: 180,
    },
    {
      prop: 'action',
      label: '操作',
      width: 168,
      fixed: 'right',
      maxActionButtons: 2,
      actions: [
        { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
        {
          label: '测试连接',
          key: 'test',
          type: 'info',
          onClick: handleTest,
          disabled: row => testingIds.has(row?.id),
          disabledReason: '正在测试，请稍候…',
        },
        { label: '查看表', key: 'tables', type: 'info', onClick: handleViewTables },
        { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
      ],
    },
  ]
})

const connectionTableColumns = [
  {
    title: '数据表',
    key: 'tableName',
    width: 300,
    render: row => h('div', { class: 'table-name-cell' }, [
      h('div', { class: 'table-name' }, row.tableName),
      h('div', { class: 'table-comment' }, row.tableComment || '暂无注释'),
    ]),
  },
  {
    title: '表类型',
    key: 'tableType',
    width: 120,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: 'info',
    }, { default: () => row.tableType || 'TABLE' }),
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: row => h('a', {
      class: 'text-primary cursor-pointer hover:text-primary-hover',
      onClick: () => handleViewFields(row),
    }, '查看字段'),
  },
]

const fieldColumns = [
  { title: '字段名', key: 'columnName', width: 200 },
  { title: '字段类型', key: 'columnType', width: 180 },
  { title: '字段注释', key: 'columnComment' },
  {
    title: '可空',
    key: 'nullable',
    width: 90,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: row.nullable ? 'default' : 'warning',
    }, { default: () => row.nullable ? '是' : '否' }),
  },
  {
    title: '主键',
    key: 'primaryKey',
    width: 90,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: row.primaryKey ? 'success' : 'default',
    }, { default: () => row.primaryKey ? '是' : '否' }),
  },
]

const editSchema = computed(() => [
  {
    type: 'divider',
    label: '基础信息',
    span: 2,
    props: { titlePlacement: 'left' },
  },
  {
    field: 'connectionName',
    label: '连接名称',
    type: 'input',
    rules: [{ required: true, message: '请填写连接名称', trigger: 'blur' }],
    props: {
      placeholder: '例如：采购业务库',
    },
  },
  {
    field: 'connectionCode',
    label: '连接编码',
    type: 'input',
    rules: [{ required: true, message: '请填写连接编码', trigger: 'blur' }],
    description: '数据集引用这个编码。不填会按名称生成。',
    props: { placeholder: '例如：erp_prod' },
    editDisabled: true,
  },
  {
    field: 'status',
    label: '启用',
    type: 'switch',
    defaultValue: 1,
    checkedValue: 1,
    uncheckedValue: 0,
  },
  {
    type: 'divider',
    label: '连接信息',
    span: 2,
    props: { titlePlacement: 'left' },
  },
  {
    field: 'dbType',
    label: '数据库',
    type: 'slot',
    slotName: 'dbType',
    rules: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
  },
  {
    field: 'host',
    label: '主机',
    type: 'slot',
    slotName: 'host',
    rules: [{ required: true, message: '请填写主机地址', trigger: 'blur' }],
  },
  {
    field: 'port',
    label: '端口',
    type: 'slot',
    slotName: 'port',
  },
  {
    field: 'database',
    label: '库名',
    type: 'slot',
    slotName: 'database',
  },
  {
    field: 'connectionPreview',
    type: 'slot',
    slotName: 'connectionPreview',
    span: 2,
    showFeedback: false,
    showLabel: false,
  },
  {
    field: 'username',
    label: '账号',
    type: 'input',
    rules: [{ required: true, message: '请填写数据库账号', trigger: 'blur' }],
    props: { placeholder: '数据库登录名' },
  },
  {
    field: 'password',
    label: '密码',
    type: 'input',
    rules: [
      {
        required: true,
        trigger: 'blur',
        validator: (_rule, value, _callback, source) => {
          if (source?.id && !value)
            return true
          return !!value
        },
        message: '请填写密码',
      },
    ],
    description: currentEditingConnection.value?.id ? '改已有连接时密码留空表示不改。' : undefined,
    props: {
      type: 'password',
      placeholder: currentEditingConnection.value?.id ? '留空表示不改' : '数据库密码',
      showPasswordOn: 'click',
    },
  },
  {
    field: 'connectionProbe',
    type: 'slot',
    slotName: 'connectionProbe',
    span: 2,
    showFeedback: false,
    showLabel: false,
  },
  {
    type: 'divider',
    label: '高级',
    span: 2,
    props: { titlePlacement: 'left' },
  },
  {
    field: 'jdbcUrl',
    label: '连接串',
    type: 'slot',
    slotName: 'jdbcUrl',
    span: 2,
    description: '一般不用改。有特殊参数时直接改这里。',
  },
  {
    field: 'schemaName',
    label: 'Schema',
    type: 'input',
    props: { placeholder: '多数情况可留空' },
  },
  {
    field: 'testSql',
    label: '测试语句',
    type: 'input',
    defaultValue: 'SELECT 1',
    props: { placeholder: 'SELECT 1' },
  },
  {
    field: 'description',
    label: '说明',
    type: 'textarea',
    span: 2,
    props: { placeholder: '这台库给谁用，可选', rows: 2 },
  },
])

function beforeRenderForm(formData) {
  currentEditingConnection.value = null
  return hydrateConnectionForm({
    ...(formData || {}),
    password: '',
  })
}

function beforeRenderDetail(detailData) {
  currentEditingConnection.value = detailData ? { ...detailData } : null
  return hydrateConnectionForm({
    ...(detailData || {}),
    password: '',
  })
}

function beforeSubmit(formData) {
  const payload = { ...formData }
  if (!String(payload.connectionCode || '').trim())
    payload.connectionCode = suggestConnectionCode(payload.connectionName)
  if (!payload.driverClassName)
    payload.driverClassName = DRIVER_CLASS_MAP[String(payload.dbType || 'MYSQL').toUpperCase()]
  if (payload.host)
    syncAccessToJdbcUrl(payload)
  else if (!String(payload.jdbcUrl || '').trim())
    syncAccessToJdbcUrl(payload)
  if (!payload.password)
    delete payload.password
  delete payload.host
  delete payload.port
  delete payload.database
  delete payload.jdbcExtra
  delete payload.connectionPreview
  delete payload.connectionProbe
  return payload
}

function handleDbTypeChange(formData, value) {
  applyDbTypeDefaults(formData, value)
}

function updateAccessField(formData, field, value) {
  formData[field] = value
  syncAccessToJdbcUrl(formData)
}

function updateJdbcUrl(formData, value) {
  formData.jdbcUrl = value
  syncJdbcUrlToAccess(formData)
}

function handleModalClose() {
  currentEditingConnection.value = null
  connectionProbeLoading.value = false
}

function handleEdit(row) {
  currentEditingConnection.value = row
  crudRef.value?.showEdit({ ...row, password: '' })
}

function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除数据连接“${row.connectionName}”吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await deleteDataConnection(row.id)
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

async function handleTest(row) {
  if (!row?.id || testingConnectionIds.value.has(row.id))
    return

  setRowTesting(row.id, true)
  const msgKey = `testConn:${row.id}`
  const label = row.connectionName || row.id
  window.$message?.loading?.(`正在测试连接「${label}」...`, { key: msgKey })
  let succeeded = false
  let resultMsg = ''
  try {
    const res = await testDataConnection(row.id)
    if (res?.code === 200 && res.data) {
      succeeded = true
      resultMsg = `连接「${label}」测试成功`
    }
    else {
      resultMsg = res?.msg || `连接「${label}」测试失败`
    }
  }
  catch (error) {
    resultMsg = error?.message || '连接测试失败'
  }
  finally {
    setRowTesting(row.id, false)
    window.$message?.destroy?.(msgKey, 0)
    setTimeout(() => {
      if (succeeded)
        window.$message?.success?.(resultMsg, { duration: 3000 })
      else
        window.$message?.error?.(resultMsg, { duration: 4000 })
    }, 50)
  }
}

async function handleProbeConnection(formData) {
  if (!formData.driverClassName)
    formData.driverClassName = DRIVER_CLASS_MAP[String(formData.dbType || 'MYSQL').toUpperCase()]
  if (!formData.jdbcUrl && formData.host)
    syncAccessToJdbcUrl(formData)
  if (!formData.dbType || !formData.jdbcUrl || !formData.username) {
    window.$message?.warning('请先填数据库类型、地址和账号')
    return
  }

  connectionProbeLoading.value = true
  try {
    let res
    if (currentEditingConnection.value?.id && !formData.password) {
      res = await testDataConnection(currentEditingConnection.value.id)
    }
    else {
      if (!formData.password) {
        window.$message?.warning('请先输入密码后再测试草稿连接')
        return
      }
      res = await testDataConnectionTemp({
        dbType: formData.dbType,
        driverClassName: formData.driverClassName,
        jdbcUrl: formData.jdbcUrl,
        username: formData.username,
        password: formData.password,
        testSql: formData.testSql || 'SELECT 1',
      })
    }

    if (res.code === 200 && res.data)
      window.$message?.success('连接测试通过')
    else
      window.$message?.error(res.msg || '连接测试失败')
  }
  catch (error) {
    window.$message?.error(error?.message || '连接测试失败')
  }
  finally {
    connectionProbeLoading.value = false
  }
}

async function handleViewTables(row) {
  currentConnection.value = row
  tableModalTitle.value = `数据表 - ${row.connectionName}`
  tableKeyword.value = ''
  tableModalVisible.value = true
  await loadConnectionTables()
}

async function loadConnectionTables() {
  if (!currentConnection.value?.id)
    return

  tableLoading.value = true
  try {
    const res = await getDataConnectionTables(currentConnection.value.id, tableKeyword.value || undefined)
    if (res.code === 200)
      connectionTables.value = res.data || []
    else
      window.$message.error(res.msg || '加载数据表失败')
  }
  catch {
    window.$message.error('加载数据表失败')
  }
  finally {
    tableLoading.value = false
  }
}

async function handleViewFields(row) {
  if (!currentConnection.value?.id)
    return

  fieldModalTitle.value = `字段列表 - ${row.tableName}`
  fieldModalVisible.value = true
  fieldLoading.value = true
  fieldRows.value = []

  try {
    const res = await getDataConnectionFields(currentConnection.value.id, row.tableName)
    if (res.code === 200)
      fieldRows.value = res.data || []
    else
      window.$message.error(res.msg || '加载字段失败')
  }
  catch {
    window.$message.error('加载字段失败')
  }
  finally {
    fieldLoading.value = false
  }
}
</script>

<style scoped>
.data-connection-page {
  height: 100%;
}

.data-connection-page :deep(.ai-crud-page) {
  height: 100%;
}

.modal-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}

.table-name-cell {
  display: grid;
  gap: 2px;
}

.table-name {
  color: var(--text-primary, #0f172a);
  font-size: 13px;
  line-height: 20px;
}

.table-comment {
  color: var(--text-tertiary, #94a3b8);
  font-size: 12px;
  line-height: 16px;
}

:global(.data-connection-edit-form .connection-inline-tip) {
  margin: 0;
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
  line-height: 18px;
  word-break: break-all;
}

:global(.data-connection-edit-form .connection-probe-bar) {
  display: flex;
  gap: 8px;
  align-items: center;
}

:global(.data-connection-edit-form .n-form-item-blank) {
  width: 100%;
}
</style>
