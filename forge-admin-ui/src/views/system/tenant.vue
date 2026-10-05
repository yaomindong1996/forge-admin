<template>
  <div class="system-tenant-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/tenant"
      :api-config="{
        list: 'get@/system/tenant/page',
        detail: 'post@/system/tenant/getById',
        add: 'post@/system/tenant/add',
        update: 'post@/system/tenant/edit',
        delete: 'post@/system/tenant/removeBatch',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="id"
      :edit-grid-cols="2"
      :edit-x-gap="14"
      :edit-y-gap="4"
      edit-form-class="tenant-config-form"
      modal-width="960px"
      add-button-text="新增租户"
      :hide-add="!userStore.isAdmin"
      :hide-batch-delete="!userStore.isAdmin"
      :hide-selection="!userStore.isAdmin"
      :before-submit="handleBeforeSubmit"
      :before-delete="handleBeforeDelete"
      :before-render-detail="handleBeforeRenderDetail"
      @submit-success="handleSubmitSuccess"
    >
      <!-- 布局与配色 -->
      <template #form-appearance="{ formData }">
        <TenantAppearanceEditor
          :form-data="formData" @layout-change="value => formData.systemLayout = value"
          @theme-change="theme => writeTenantAppearance(formData, theme)"
        />
      </template>
    </AiCrudPage>

    <!-- 单层父子弹窗，用户列表独立维护查询状态 -->
    <TenantUsersDialog ref="usersDialog" @changed="crudRef?.refresh()" />
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, onMounted, ref } from 'vue'
import { listTenantBusinessDatasources, removeTenant } from '@/api/system/tenant'
import { AiCrudPage } from '@/components/ai-form'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { defaultLayout } from '@/settings'
import { useAppStore, useTenantStore, useUserStore } from '@/store'
import { applyTenantConfig } from '@/utils/tenant-config'
import { prepareTenantAppearance, writeTenantAppearance } from './components/tenant/tenant-appearance'
import { createTenantEditSchema } from './components/tenant/tenant-schema'
import TenantAppearanceEditor from './components/tenant/TenantAppearanceEditor.vue'
import TenantUsersDialog from './components/tenant/TenantUsersDialog.vue'

defineOptions({ name: 'SystemTenant' })

const DEFAULT_TENANT_ID = 1
const NORMAL_DISABLE_DICT = 'sys_normal_disable'

const crudRef = ref(null)
const userStore = useUserStore()
const usersDialog = ref(null)
const businessDatasources = ref([])
const { dict } = useDict(NORMAL_DISABLE_DICT)
const tenantStatusOptions = computed(() => toNumberOptions(dict.value[NORMAL_DISABLE_DICT]))
const businessDatasourceOptions = computed(() => businessDatasources.value.map(item => ({
  label: `${item.datasourceName} (${item.datasourceCode || item.dbType})`,
  value: item.datasourceId,
})))

// 搜索表单配置
const searchSchema = computed(() => [
  {
    field: 'tenantName',
    label: '租户名称',
    type: 'input',
    props: {
      placeholder: '请输入租户名称',
    },
  },
  {
    field: 'contactPerson',
    label: '负责人',
    type: 'input',
    props: {
      placeholder: '请输入负责人',
    },
  },
  {
    field: 'contactPhone',
    label: '联系电话',
    type: 'input',
    props: {
      placeholder: '请输入联系电话',
    },
  },
  {
    field: 'tenantStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: tenantStatusOptions.value,
    },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'tenantName',
    label: '租户',
    minWidth: 200,
    render: row => h(SystemTableCell, {
      title: row.tenantName,
      subtitle: row.contactPerson ? `负责人：${row.contactPerson}` : '',
      interactive: true,
      avatar: true,
      tooltip: `查看租户：${row.tenantName || '-'}`,
      onActivate: () => crudRef.value?.showDetail(row),
    }),
  },
  {
    prop: 'contactPhone',
    label: '联系电话',
    width: 130,
  },
  {
    prop: 'userLimit',
    label: '人员上限',
    width: 100,
    render: (row) => {
      return row.userLimit === 0 ? '无限制' : row.userLimit
    },
  },
  {
    prop: 'expireTime',
    label: '过期时间',
    width: 180,
  },
  {
    prop: 'tenantStatus',
    label: '状态',
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: NORMAL_DISABLE_DICT, value: row.tenantStatus, size: 'small' })
    },
  },
  {
    prop: 'defaultBusinessDatasourceCode',
    label: '业务数据源',
    minWidth: 140,
    render: (row) => {
      const datasource = findBusinessDatasource(row.defaultBusinessDatasourceId)
      return h(NTag, {
        size: 'small',
        type: datasource ? 'info' : 'default',
      }, {
        default: () => datasource?.datasourceName || row.defaultBusinessDatasourceCode || '主库回退',
      })
    },
  },
  {
    prop: 'tenantDesc',
    label: '描述',
    minWidth: 180,
  },
  {
    prop: 'action',
    label: '操作',
    width: 160,
    fixed: 'right',
    actions: [
      { label: '用户', key: 'users', onClick: handleViewUsers },
      { label: '编辑', key: 'edit', onClick: handleEdit },
      {
        label: '删除',
        key: 'delete',
        type: 'error',
        onClick: handleDelete,
        visible: () => userStore.isAdmin,
        disabled: isDefaultTenant,
        disabledReason: '默认租户不能删除',
      },
    ],
  },
])

// —— 租户编辑 Schema ——
const editSchema = computed(() => createTenantEditSchema({
  tenantStatusOptions: tenantStatusOptions.value,
  businessDatasourceOptions: businessDatasourceOptions.value,
}))

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

function findBusinessDatasource(datasourceId) {
  if (datasourceId === null || datasourceId === undefined)
    return null
  return businessDatasources.value.find(item => String(item.datasourceId) === String(datasourceId)) || null
}

async function loadBusinessDatasources() {
  try {
    const res = await listTenantBusinessDatasources()
    businessDatasources.value = res.data || []
  }
  catch (error) {
    console.error('加载租户业务数据源失败:', error)
    window.$message?.warning?.('租户业务数据源加载失败')
  }
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

function isDefaultTenant(row) {
  return Number(row?.id) === DEFAULT_TENANT_ID
}

function handleBeforeDelete(rows = []) {
  if (rows.some(isDefaultTenant)) {
    window.$message.warning('默认租户不能删除')
    return false
  }
  return true
}

// 删除
function handleDelete(row) {
  if (isDefaultTenant(row)) {
    window.$message.warning('默认租户不能删除')
    return
  }
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除租户"${row.tenantName}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await removeTenant(row.id)
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

function handleViewUsers(row) {
  usersDialog.value?.open(row)
}

// —— 租户外观保存：与登录时共用同一应用入口，保留主题扩展字段 ——
async function handleSubmitSuccess() {
  const tenantStore = useTenantStore()
  const config = await tenantStore.loadTenantConfig(userStore.userInfo?.tenantId)
  if (config) {
    await applyTenantConfig(config, useAppStore())
    window.$message.success('主题配置已更新')
  }
}

function handleBeforeSubmit(formData) {
  formData.systemLayout = formData.systemLayout || defaultLayout
  const datasource = findBusinessDatasource(formData.defaultBusinessDatasourceId)
  formData.defaultBusinessDatasourceId = datasource?.datasourceId || null
  formData.defaultBusinessDatasourceCode = datasource?.datasourceCode || null
  return prepareTenantAppearance(formData)
}

function handleBeforeRenderDetail(data) {
  return data
}

onMounted(() => {
  loadBusinessDatasources()
})
</script>

<style scoped>
.system-tenant-page {
  height: 100%;
}
:deep(.tenant-config-form) {
  padding: 2px 2px 0;
}
</style>
