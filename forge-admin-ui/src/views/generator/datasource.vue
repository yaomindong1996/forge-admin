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
      modal-width="800px"
      add-button-text="新增数据源"
    />
  </div>
</template>

<script setup>
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'
import { mapDictOptionValues, toNumberDictOptions } from '@/utils/dict-options'

defineOptions({ name: 'GeneratorDatasource' })

const crudRef = ref(null)
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

// 驱动类映射
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

// 搜索表单配置
const searchSchema = computed(() => [
  {
    field: 'datasourceName',
    label: '数据源名称',
    type: 'input',
    props: {
      placeholder: '请输入数据源名称',
    },
  },
  {
    field: 'usageScope',
    label: '用途',
    type: 'select',
    props: {
      placeholder: '请选择用途',
      options: usageScopeOptions.value,
    },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'datasourceName',
    label: '数据源名称',
    width: 150,
  },
  {
    prop: 'datasourceCode',
    label: '数据源编码',
    width: 120,
  },
  {
    prop: 'dbType',
    label: '数据库类型',
    width: 100,
    render: row => h(DictTag, { options: dbTypeOptions.value, value: row.dbType, size: 'small' }),
  },
  {
    prop: 'usageScope',
    label: '用途',
    width: 110,
    render: row => h(DictTag, { options: usageScopeOptions.value, value: row.usageScope, size: 'small' }),
  },
  {
    prop: 'url',
    label: '连接地址',
    minWidth: 220,
    ellipsis: true,
  },
  {
    prop: 'username',
    label: '用户名',
    width: 100,
  },
  {
    prop: 'isDefault',
    label: '默认',
    width: 80,
    render: row => h(DictTag, { options: yesNoOptions.value, value: row.isDefault, size: 'small' }),
  },
  {
    prop: 'readonly',
    label: '只读',
    width: 80,
    render: row => h(DictTag, { options: yesNoOptions.value, value: row.readonly, size: 'small' }),
  },
  {
    prop: 'allowRuntimeWrite',
    label: '写入',
    width: 80,
    render: row => h(DictTag, { options: yesNoOptions.value, value: row.allowRuntimeWrite, size: 'small' }),
  },
  {
    prop: 'allowRuntimeDdl',
    label: 'DDL',
    width: 80,
    render: row => h(DictTag, { options: yesNoOptions.value, value: row.allowRuntimeDdl, size: 'small' }),
  },
  {
    prop: 'riskLevel',
    label: '风险',
    width: 80,
    render: row => h(DictTag, { options: riskLevelOptions.value, value: row.riskLevel, size: 'small' }),
  },
  {
    prop: 'isEnabled',
    label: '状态',
    width: 80,
    render: row => h(DictTag, { options: enableDisableOptions.value, value: row.isEnabled, size: 'small' }),
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '测试连接', key: 'testConnection', type: 'primary', onClick: handleTestConnection },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置
const editSchema = computed(() => [
  {
    field: 'datasourceName',
    label: '数据源名称',
    type: 'input',
    rules: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入数据源名称',
    },
  },
  {
    field: 'datasourceCode',
    label: '数据源编码',
    type: 'input',
    rules: [{ required: true, message: '请输入数据源编码', trigger: 'blur' }],
    props: {
      placeholder: '请输入数据源编码',
    },
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
        // 自动填充驱动类名
        if (driverClassMap[value]) {
          formData.driverClassName = driverClassMap[value]
        }
        if (!formData.testQuery || Object.values(testQueryMap).includes(formData.testQuery)) {
          formData.testQuery = testQueryMap[value] || 'SELECT 1'
        }
      },
    },
  },
  {
    field: 'driverClassName',
    label: '驱动类名',
    type: 'input',
    defaultValue: 'com.mysql.cj.jdbc.Driver',
    rules: [{ required: true, message: '请输入驱动类名', trigger: 'blur' }],
    props: {
      placeholder: '请输入驱动类名',
    },
  },
  {
    field: 'url',
    label: '连接地址',
    type: 'input',
    span: 2,
    rules: [{ required: true, message: '请输入连接地址', trigger: 'blur' }],
    props: {
      placeholder: '如：jdbc:mysql://localhost:3306/database?useUnicode=true&characterEncoding=utf8',
    },
  },
  {
    field: 'username',
    label: '用户名',
    type: 'input',
    rules: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    props: {
      placeholder: '请输入用户名',
    },
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
          // 编辑时密码可以为空（不修改）
          if (source.datasourceId && !value) {
            return true
          }
          return !!value
        },
      },
    ],
    props: {
      type: 'password',
      placeholder: '编辑时留空则不修改密码',
      showPasswordOn: 'click',
    },
  },
  {
    field: 'usageScope',
    label: '用途范围',
    type: 'select',
    defaultValue: 'LOWCODE_RUNTIME',
    rules: [{ required: true, message: '请选择用途范围', trigger: 'change' }],
    props: {
      placeholder: '请选择用途范围',
      options: usageScopeOptions.value,
    },
  },
  {
    field: 'riskLevel',
    label: '风险等级',
    type: 'select',
    defaultValue: 'LOW',
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
    field: 'readonly',
    label: '只读模式',
    type: 'radio',
    defaultValue: 0,
    props: {
      options: yesNoOptions.value,
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
    label: '允许运行写入',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: yesNoOptions.value,
    },
  },
  {
    field: 'allowRuntimeDdl',
    label: '允许运行DDL',
    type: 'radio',
    defaultValue: 0,
    props: {
      options: yesNoOptions.value,
    },
  },
  {
    field: 'testQuery',
    label: '测试SQL',
    type: 'input',
    defaultValue: 'SELECT 1',
    props: {
      placeholder: '请输入测试SQL',
    },
  },
  {
    field: 'isDefault',
    label: '默认数据源',
    type: 'radio',
    defaultValue: 0,
    props: {
      options: yesNoOptions.value,
    },
  },
  {
    field: 'isEnabled',
    label: '启用状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: enableDisableOptions.value,
    },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'number',
    defaultValue: 0,
    props: {
      placeholder: '请输入排序号',
      min: 0,
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

// 表单提交前处理
function beforeSubmit(formData) {
  // 如果密码为空，删除该字段（不更新密码）
  if (!formData.password) {
    delete formData.password
  }
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

// 编辑
function handleEdit(row) {
  // 清空密码字段，不回显
  const editData = { ...row, password: '' }
  crudRef.value?.showEdit(editData)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除数据源"${row.datasourceName}"吗？`,
    positiveText: '确定',
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

// 测试连接
async function handleTestConnection(row) {
  const messageKey = `datasource-test-${row.datasourceId}`
  try {
    window.$message.loading('正在测试连接...', { key: messageKey })
    const res = await request.post(`/generator/datasource/test/${row.datasourceId}`)
    if (res.code === 200) {
      window.$message.success('连接成功', { key: messageKey })
    }
    else {
      window.$message.error(res.msg || '连接失败', { key: messageKey })
    }
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
