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
      :edit-grid-cols="24"
      edit-label-placement="top"
      edit-label-align="left"
      :edit-x-gap="16"
      :edit-y-gap="8"
      edit-form-class="system-edit-form tenant-config-form"
      edit-size="small"
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
import { getTenantPackageDetail, listTenantPackageOptions } from '@/api/system/tenantPackage'
import { AiCrudPage } from '@/components/ai-form'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { defaultLayout } from '@/settings'
import { useAppStore, useTenantStore, useUserStore } from '@/store'
import { applyTenantConfig } from '@/utils/tenant-config'
import { prepareTenantAppearance, writeTenantAppearance } from './components/tenant/tenant-appearance'
import { createTenantEditSchema } from './components/tenant/tenant-schema'
import { itemsToFormFields } from './components/tenant-package/tenant-package-schema'
import TenantAppearanceEditor from './components/tenant/TenantAppearanceEditor.vue'
import TenantUsersDialog from './components/tenant/TenantUsersDialog.vue'

defineOptions({ name: 'SystemTenant' })

const DEFAULT_TENANT_ID = 1
const NORMAL_DISABLE_DICT = 'sys_normal_disable'

const crudRef = ref(null)
const userStore = useUserStore()
const usersDialog = ref(null)
const businessDatasources = ref([])
const tenantPackages = ref([])
const { dict } = useDict(NORMAL_DISABLE_DICT)
const tenantStatusOptions = computed(() => toNumberOptions(dict.value[NORMAL_DISABLE_DICT]))
const businessDatasourceOptions = computed(() => businessDatasources.value.map(item => ({
  label: `${item.datasourceName} (${item.datasourceCode || item.dbType})`,
  value: item.datasourceId,
})))
const packageOptions = computed(() => tenantPackages.value.map(item => ({
  label: item.isDefault === 1 ? `${item.packageName}（默认）` : item.packageName,
  value: item.id,
})))
const defaultPackageId = computed(() => {
  const preferred = tenantPackages.value.find(item => Number(item.isDefault) === 1)
  return preferred?.id || tenantPackages.value[0]?.id || null
})

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
    prop: 'packageId',
    label: '套餐',
    width: 120,
    render: (row) => {
      const pkg = tenantPackages.value.find(item => String(item.id) === String(row.packageId))
      return pkg?.packageName || (row.packageId ? String(row.packageId) : '-')
    },
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
  packageOptions: packageOptions.value,
  onPackageChange: handlePackageChange,
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

async function loadTenantPackages() {
  try {
    const res = await listTenantPackageOptions()
    tenantPackages.value = res.data || []
  }
  catch (error) {
    console.error('加载租户套餐失败:', error)
    window.$message?.warning?.('租户套餐加载失败')
  }
}

async function applyPackageDefaults(packageId, formData) {
  if (!packageId || !formData)
    return
  try {
    const res = await getTenantPackageDetail(packageId)
    const detail = res.data || {}
    const fields = itemsToFormFields(detail.items || [])
    formData.bootstrapPackages = fields.bootstrapPackages
    formData.userLimit = fields.userLimit
  }
  catch (error) {
    console.error('加载套餐详情失败:', error)
    window.$message?.warning?.('套餐详情加载失败，可手动勾选初始化包')
  }
}

async function handlePackageChange({ value, formData }) {
  await applyPackageDefaults(value, formData)
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
    content: `确定要删除租户「${row.tenantName}」吗？将自动清理该租户下的用户绑定；仅属于该租户的账号也会删除。系统管理员账号不会被删除。删除后无法恢复。`,
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
      catch (error) {
        window.$message.error(error?.message || '删除失败')
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
  // 编辑时不提交引导包；新建勾选结果原样提交（空数组=不引导，缺省由后端用推荐默认）
  if (formData?.id) {
    delete formData.bootstrapPackages
  }
  else if (!formData.packageId && defaultPackageId.value) {
    formData.packageId = defaultPackageId.value
  }
  return prepareTenantAppearance(formData)
}

async function handleBeforeRenderDetail(data = {}) {
  if (!data?.id && !data.packageId && defaultPackageId.value) {
    data.packageId = defaultPackageId.value
    await applyPackageDefaults(data.packageId, data)
  }
  return data
}

onMounted(() => {
  loadBusinessDatasources()
  loadTenantPackages()
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
