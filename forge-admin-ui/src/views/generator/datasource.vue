<template>
  <div class="generator-datasource-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/generator/datasource/list',
        detail: 'get@/generator/datasource/:id',
        add: 'post@/generator/datasource/add',
        update: 'post@/generator/datasource/edit',
        delete: 'post@/generator/datasource/remove/:id',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-submit="beforeSubmit"
      row-key="datasourceId"
      :edit-grid-cols="2"
      modal-width="880px"
      add-button-text="新增数据源"
    />
  </div>
</template>

<script setup>
import { NSwitch } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'
import { mapDictOptionValues, toNumberDictOptions } from '@/utils/dict-options'

defineOptions({ name: 'GeneratorDatasource' })

const crudRef = ref(null)
const inlineUpdatingMap = ref({})
const { dict } = useDict(
  'data_db_type',
  'gen_datasource_usage_scope',
  'gen_datasource_risk_level',
  'sys_enable_disable',
  'sys_yes_no',
)

const dbTypeOptions = computed(() => mapDictOptionValues(dict.value.data_db_type, {
  MYSQL: 'MySQL',
  ORACLE: 'Oracle',
  POSTGRESQL: 'PostgreSQL',
  SQLSERVER: 'SQLServer',
}))
const usageScopeOptions = computed(() => dict.value.gen_datasource_usage_scope || [])
const riskLevelOptions = computed(() => dict.value.gen_datasource_risk_level || [])
const enableDisableOptions = computed(() => toNumberDictOptions(dict.value.sys_enable_disable))
const yesNoOptions = computed(() => toNumberDictOptions(dict.value.sys_yes_no))

const driverClassMap = {
  MySQL: 'com.mysql.cj.jdbc.Driver',
  Oracle: 'oracle.jdbc.OracleDriver',
  PostgreSQL: 'org.postgresql.Driver',
  SQLServer: 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
}

const testQueryMap = {
  MySQL: 'SELECT 1',
  Oracle: 'SELECT 1 FROM DUAL',
  PostgreSQL: 'SELECT 1',
  SQLServer: 'SELECT 1',
}

const searchSchema = computed(() => [
  {
    field: 'datasourceName',
    label: '数据源名称',
    type: 'input',
    props: { placeholder: '请输入名称' },
  },
  {
    field: 'usageScope',
    label: '用途',
    type: 'select',
    props: {
      placeholder: '全部用途',
      options: usageScopeOptions.value,
      clearable: true,
    },
  },
])

const tableColumns = computed(() => [
  {
    prop: 'datasourceName',
    label: '名称',
    minWidth: 140,
    ellipsis: true,
  },
  {
    prop: 'datasourceCode',
    label: '编码',
    minWidth: 120,
    ellipsis: true,
  },
  {
    prop: 'dbType',
    label: '数据库',
    width: 110,
    render: row => h(DictTag, { options: dbTypeOptions.value, value: row.dbType, size: 'small' }),
  },
  {
    prop: 'usageScope',
    label: '用途',
    width: 120,
    render: row => h(DictTag, { options: usageScopeOptions.value, value: row.usageScope, size: 'small' }),
  },
  {
    prop: 'url',
    label: '连接地址',
    minWidth: 240,
    ellipsis: true,
  },
  {
    prop: 'username',
    label: '用户名',
    width: 110,
    ellipsis: true,
  },
  {
    prop: 'isDefault',
    label: '默认',
    width: 88,
    align: 'center',
    titleAlign: 'center',
    render: row => renderInlineSwitch(row, 'isDefault', '默认数据源'),
  },
  {
    prop: 'readonly',
    label: '只读',
    width: 88,
    align: 'center',
    titleAlign: 'center',
    render: row => renderInlineSwitch(row, 'readonly', '强制只读'),
  },
  {
    prop: 'allowRuntimeWrite',
    label: '写数据',
    width: 96,
    align: 'center',
    titleAlign: 'center',
    render: row => renderInlineSwitch(row, 'allowRuntimeWrite', '允许写入业务数据', {
      disabled: Number(row.readonly) === 1,
    }),
  },
  {
    prop: 'allowRuntimeDdl',
    label: '改表结构',
    width: 108,
    align: 'center',
    titleAlign: 'center',
    render: row => renderInlineSwitch(row, 'allowRuntimeDdl', '允许自动创建或修改表结构', {
      disabled: Number(row.readonly) === 1,
    }),
  },
  {
    prop: 'riskLevel',
    label: '风险',
    width: 88,
    align: 'center',
    titleAlign: 'center',
    render: row => h(DictTag, { options: riskLevelOptions.value, value: row.riskLevel, size: 'small' }),
  },
  {
    prop: 'isEnabled',
    label: '启用',
    width: 88,
    align: 'center',
    titleAlign: 'center',
    render: row => renderInlineSwitch(row, 'isEnabled', '启用状态'),
  },
  {
    prop: 'action',
    label: '操作',
    width: 210,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '测试连接', key: 'testConnection', type: 'info', onClick: handleTestConnection },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

const editSchema = computed(() => [
  {
    type: 'divider',
    label: '基础信息',
    props: { titlePlacement: 'left' },
    span: 2,
  },
  {
    field: 'datasourceName',
    label: '名称',
    type: 'input',
    rules: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
    props: { placeholder: '如：业务库、报表只读库' },
  },
  {
    field: 'datasourceCode',
    label: '编码',
    type: 'input',
    labelTip: '系统内唯一标识，创建后尽量不要改，低代码会按编码引用该数据源。',
    rules: [{ required: true, message: '请输入数据源编码', trigger: 'blur' }],
    props: { placeholder: '如：biz_mysql、report_readonly' },
  },
  {
    field: 'usageScope',
    label: '用途',
    type: 'select',
    defaultValue: 'LOWCODE_RUNTIME',
    labelTip: '决定这个库能用在哪里：低代码运行、开发导入表结构，或两者都可。',
    rules: [{ required: true, message: '请选择用途', trigger: 'change' }],
    props: {
      placeholder: '请选择用途',
      options: usageScopeOptions.value,
    },
  },
  {
    field: 'riskLevel',
    label: '风险等级',
    type: 'select',
    defaultValue: 'LOW',
    labelTip: '高风险库会默认改为只读，并关闭写数据与改表结构，避免误操作生产库。',
    props: {
      placeholder: '请选择风险等级',
      options: riskLevelOptions.value,
      onUpdateValue: (value, formData) => {
        if (value === 'HIGH' && !formData.datasourceId) {
          formData.readonly = 1
          formData.allowRuntimeWrite = 0
          formData.allowRuntimeDdl = 0
        }
      },
    },
  },

  {
    type: 'divider',
    label: '连接配置',
    props: { titlePlacement: 'left' },
    span: 2,
  },
  {
    field: 'dbType',
    label: '数据库类型',
    type: 'select',
    defaultValue: 'MySQL',
    rules: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
    props: {
      placeholder: '请选择数据库类型',
      options: dbTypeOptions.value,
      onUpdateValue: (value, formData) => {
        if (driverClassMap[value])
          formData.driverClassName = driverClassMap[value]
        if (!formData.testQuery || Object.values(testQueryMap).includes(formData.testQuery))
          formData.testQuery = testQueryMap[value] || 'SELECT 1'
      },
    },
  },
  {
    field: 'driverClassName',
    label: '驱动类名',
    type: 'input',
    defaultValue: 'com.mysql.cj.jdbc.Driver',
    labelTip: '一般随数据库类型自动带出，通常无需手工修改。',
    rules: [{ required: true, message: '请输入驱动类名', trigger: 'blur' }],
    props: { placeholder: '选择数据库类型后会自动填充' },
  },
  {
    field: 'url',
    label: '连接地址',
    type: 'input',
    span: 2,
    labelTip: 'JDBC 连接串。示例：jdbc:mysql://主机:3306/库名?useUnicode=true&characterEncoding=utf8',
    rules: [{ required: true, message: '请输入连接地址', trigger: 'blur' }],
    props: {
      placeholder: 'jdbc:mysql://localhost:3306/forge_admin?useUnicode=true&characterEncoding=utf8',
    },
  },
  {
    field: 'username',
    label: '用户名',
    type: 'input',
    rules: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    props: { placeholder: '数据库登录用户名' },
  },
  {
    field: 'password',
    label: '密码',
    type: 'input',
    rules: [
      {
        required: true,
        message: '请输入密码',
        trigger: 'blur',
        validator: (_rule, value, _callback, source) => {
          if (source.datasourceId && !value)
            return true
          return !!value
        },
      },
    ],
    props: {
      type: 'password',
      placeholder: '编辑时留空表示不修改密码',
      showPasswordOn: 'click',
    },
  },
  {
    field: 'testQuery',
    label: '连通测试语句',
    type: 'input',
    defaultValue: 'SELECT 1',
    labelTip: '点「测试连接」时执行的简单查询，用来确认库是否可达。',
    props: { placeholder: '一般保持默认即可，如 SELECT 1' },
  },

  {
    type: 'divider',
    label: '权限与安全',
    description: '控制低代码能否往这个库写数据、以及能否自动改表结构。',
    props: { titlePlacement: 'left' },
    span: 2,
  },
  {
    field: 'readonly',
    label: '强制只读',
    type: 'switch',
    defaultValue: 0,
    labelTip: '开启后禁止写数据和改表结构，适合只读分析库或高风险生产库。',
    props: {
      checkedValue: 1,
      uncheckedValue: 0,
      onUpdateValue: (value, formData) => {
        if (value === 1) {
          formData.allowRuntimeWrite = 0
          formData.allowRuntimeDdl = 0
        }
      },
    },
  },
  {
    field: 'allowRuntimeWrite',
    label: '允许写数据',
    type: 'switch',
    defaultValue: 1,
    labelTip: '开启后，业务表单可以新增、修改、删除该库里的业务数据。',
    disabled: ({ formData }) => Number(formData?.readonly) === 1,
    props: {
      checkedValue: 1,
      uncheckedValue: 0,
    },
  },
  {
    field: 'allowRuntimeDdl',
    label: '允许改表结构',
    type: 'switch',
    defaultValue: 0,
    labelTip: '开启后，低代码发布时可自动建表、加字段等。生产库建议关闭，改为人工执行变更。',
    disabled: ({ formData }) => Number(formData?.readonly) === 1,
    props: {
      checkedValue: 1,
      uncheckedValue: 0,
    },
  },
  {
    field: 'isDefault',
    label: '设为默认',
    type: 'switch',
    defaultValue: 0,
    labelTip: '默认数据源会优先被低代码页面选用。',
    props: {
      checkedValue: 1,
      uncheckedValue: 0,
    },
  },
  {
    field: 'isEnabled',
    label: '启用',
    type: 'switch',
    defaultValue: 1,
    labelTip: '停用后，运行时将无法再选用该数据源。',
    props: {
      checkedValue: 1,
      uncheckedValue: 0,
    },
  },

  {
    type: 'divider',
    label: '其它',
    props: { titlePlacement: 'left' },
    span: 2,
  },
  {
    field: 'sort',
    label: '排序',
    type: 'number',
    defaultValue: 0,
    props: {
      placeholder: '数字越小越靠前',
      min: 0,
    },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '可选，补充说明用途或注意事项',
      rows: 3,
    },
  },
])

function inlineKey(row, field) {
  return `${row?.datasourceId}::${field}`
}

function isInlineUpdating(row, field) {
  return !!inlineUpdatingMap.value[inlineKey(row, field)]
}

function setInlineUpdating(row, field, updating) {
  const key = inlineKey(row, field)
  const next = { ...inlineUpdatingMap.value }
  if (updating)
    next[key] = true
  else
    delete next[key]
  inlineUpdatingMap.value = next
}

function renderInlineSwitch(row, field, ariaLabel, options = {}) {
  const value = Number(row?.[field]) === 1 ? 1 : 0
  const updating = isInlineUpdating(row, field)
  return h(NSwitch, {
    value,
    size: 'small',
    checkedValue: 1,
    uncheckedValue: 0,
    loading: updating,
    disabled: updating || options.disabled === true,
    ariaLabel,
    onUpdateValue: next => handleInlineSwitchUpdate(row, field, next),
  })
}

async function handleInlineSwitchUpdate(row, field, nextValue) {
  if (!row?.datasourceId || isInlineUpdating(row, field))
    return

  const previous = Number(row[field]) === 1 ? 1 : 0
  const payload = {
    datasourceId: row.datasourceId,
    [field]: nextValue,
  }

  // 打开只读时，同步关掉写数据 / 改表结构
  if (field === 'readonly' && nextValue === 1) {
    payload.allowRuntimeWrite = 0
    payload.allowRuntimeDdl = 0
  }
  // 只读状态下不允许单独打开写入权限
  if ((field === 'allowRuntimeWrite' || field === 'allowRuntimeDdl') && Number(row.readonly) === 1) {
    window.$message.warning('当前数据源为强制只读，请先关闭「只读」')
    return
  }

  setInlineUpdating(row, field, true)
  row[field] = nextValue
  if (field === 'readonly' && nextValue === 1) {
    row.allowRuntimeWrite = 0
    row.allowRuntimeDdl = 0
  }

  try {
    const res = await request.post('/generator/datasource/edit', payload)
    if (res?.code !== 200)
      throw new Error(res?.msg || res?.message || '更新失败')
    window.$message.success('已更新')
    // 设为默认后其它行的「默认」状态可能变化，刷新列表
    if (field === 'isDefault' && nextValue === 1)
      crudRef.value?.refresh()
  }
  catch (error) {
    row[field] = previous
    if (field === 'readonly' && nextValue === 1) {
      // 无法精确回滚联动字段，刷新更稳妥
      crudRef.value?.refresh()
    }
    window.$message.error(error?.message || '更新失败')
  }
  finally {
    setInlineUpdating(row, field, false)
  }
}

function beforeSubmit(formData) {
  if (!formData.password)
    delete formData.password
  if (formData.readonly === 1) {
    formData.allowRuntimeWrite = 0
    formData.allowRuntimeDdl = 0
  }
  if (formData.riskLevel === 'HIGH' && !formData.datasourceId) {
    formData.readonly = 1
    formData.allowRuntimeWrite = 0
    formData.allowRuntimeDdl = 0
  }
  return formData
}

function handleEdit(row) {
  const editData = { ...row, password: '' }
  crudRef.value?.showEdit(editData)
}

function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定删除数据源「${row.datasourceName}」吗？删除后依赖它的低代码页面将无法访问该库。`,
    positiveText: '确定删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post(`/generator/datasource/remove/${row.datasourceId}`)
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

async function handleTestConnection(row) {
  const messageKey = `datasource-test-${row.datasourceId}`
  try {
    window.$message.loading('正在测试连接…', { key: messageKey })
    const res = await request.post(`/generator/datasource/test/${row.datasourceId}`)
    if (res.code === 200)
      window.$message.success('连接成功', { key: messageKey })
    else
      window.$message.error(res.msg || '连接失败', { key: messageKey })
  }
  catch (error) {
    window.$message.error(error?.message || '连接测试失败', { key: messageKey })
  }
}
</script>

<style scoped>
.generator-datasource-page {
  height: 100%;
}
</style>
