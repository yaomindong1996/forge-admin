<template>
  <div class="system-tenant-package-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/tenant-package"
      :api-config="{
        list: 'get@/system/tenant-package/page',
        detail: 'get@/system/tenant-package/:id',
        add: 'post@/system/tenant-package/add',
        update: 'post@/system/tenant-package/edit',
        delete: 'post@/system/tenant-package/removeBatch',
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
      edit-form-class="system-edit-form"
      edit-size="small"
      modal-width="880px"
      add-button-text="新增套餐"
      :hide-add="!userStore.isAdmin"
      :hide-batch-delete="!userStore.isAdmin"
      :hide-selection="!userStore.isAdmin"
      :before-submit="handleBeforeSubmit"
      :before-render-detail="handleBeforeRenderDetail"
    />
  </div>
</template>

<script setup>
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import {
  ALL_BOOTSTRAP_CODES,
  DEFAULT_FLAG_OPTIONS,
  createTenantPackageEditSchema,
  formFieldsToItems,
  itemsToFormFields,
} from './components/tenant-package/tenant-package-schema'

defineOptions({ name: 'SystemTenantPackage' })

const NORMAL_DISABLE_DICT = 'sys_normal_disable'

const crudRef = ref(null)
const userStore = useUserStore()
const { dict } = useDict(NORMAL_DISABLE_DICT)

const statusOptions = computed(() => toNumberOptions(dict.value[NORMAL_DISABLE_DICT]))

const searchSchema = computed(() => [
  {
    field: 'packageName',
    label: '套餐名称',
    type: 'input',
    props: { placeholder: '请输入套餐名称' },
  },
  {
    field: 'packageCode',
    label: '套餐编码',
    type: 'input',
    props: { placeholder: '请输入套餐编码' },
  },
  {
    field: 'packageStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: statusOptions.value,
      clearable: true,
    },
  },
])

const tableColumns = computed(() => [
  {
    prop: 'packageName',
    label: '套餐',
    minWidth: 200,
    render: row => h(SystemTableCell, {
      title: row.packageName,
      subtitle: row.packageCode,
      interactive: true,
      avatar: true,
      tooltip: `查看套餐：${row.packageName || '-'}`,
      onActivate: () => crudRef.value?.showDetail(row),
    }),
  },
  {
    prop: 'isDefault',
    label: '默认',
    width: 90,
    render: row => h(DictTag, { options: DEFAULT_FLAG_OPTIONS, value: row.isDefault, size: 'small', forceTag: true }),
  },
  {
    prop: 'packageStatus',
    label: '状态',
    width: 100,
    render: row => h(DictTag, { dictType: NORMAL_DISABLE_DICT, value: row.packageStatus, size: 'small' }),
  },
  {
    prop: 'sort',
    label: '排序',
    width: 80,
  },
  {
    prop: 'description',
    label: '说明',
    minWidth: 220,
  },
  {
    prop: 'action',
    label: '操作',
    width: 140,
    fixed: 'right',
    actions: [
      {
        label: '编辑',
        key: 'edit',
        onClick: row => crudRef.value?.showEdit(row),
        visible: () => userStore.isAdmin,
      },
      {
        label: '删除',
        key: 'delete',
        type: 'error',
        onClick: row => crudRef.value?.handleDelete(row),
        visible: () => userStore.isAdmin,
        disabled: row => Number(row?.isDefault) === 1,
        disabledReason: '默认套餐不能删除',
      },
    ],
  },
])

const editSchema = computed(() => createTenantPackageEditSchema({
  statusOptions: statusOptions.value,
}))

function toNumberOptions(options = []) {
  return (options || []).map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

function handleBeforeRenderDetail(data = {}) {
  const fields = itemsToFormFields(data.items || [])
  return {
    ...data,
    bootstrapPackages: fields.bootstrapPackages.length ? fields.bootstrapPackages : [...ALL_BOOTSTRAP_CODES],
    userLimit: fields.userLimit,
    resourceIdsText: fields.resourceIdsText,
  }
}

function handleBeforeSubmit(formData = {}) {
  formData.items = formFieldsToItems(formData)
  delete formData.bootstrapPackages
  delete formData.userLimit
  delete formData.resourceIdsText
  delete formData.itemsSource
  return formData
}
</script>

<style scoped>
.system-tenant-package-page {
  height: 100%;
}
</style>
