<template>
  <div class="flow-user-group-page">
    <AiCrudPage
      ref="crudRef"
      api="/api/flow/org/groups"
      :api-config="{
        list: 'get@/api/flow/org/groups/page',
        detail: 'get@/api/flow/org/groups/:id',
        add: 'post@/api/flow/org/groups',
        update: 'put@/api/flow/org/groups',
        delete: 'delete@/api/flow/org/groups/:id',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-submit="beforeSubmit"
      row-key="id"
      :edit-grid-cols="2"
      edit-label-placement="left"
      edit-label-align="left"
      edit-label-width="92px"
      modal-width="720px"
      add-button-text="新增用户组"
      :load-detail-on-edit="true"
      :hide-selection="true"
      :hide-batch-delete="true"
      :search-grid-cols="3"
      :search-max-visible-fields="3"
      :search-y-gap="8"
      search-label-width="72px"
    />

    <UserSelectModal
      v-model:show="memberModalVisible"
      :title="`维护成员：${activeGroup?.groupName || ''}`"
      multiple
      :user-status="1"
      allow-empty
      :selected-users="memberSelectionUsers"
      @confirm="handleMemberSelection"
    />
  </div>
</template>

<script setup>
import { computed, h, ref } from 'vue'
import flowApi from '@/api/flow'
import { AiCrudPage } from '@/components/ai-form'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import UserSelectModal from '@/components/common/UserSelectModal.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { toNumberDictOptions } from '@/utils/dict-options'

defineOptions({ name: 'FlowUserGroup' })

const crudRef = ref(null)
const memberModalVisible = ref(false)
const memberLoading = ref(false)
const activeGroup = ref(null)
const members = ref([])
const { dict } = useDict('sys_enable_disable')
const statusOptions = computed(() => toNumberDictOptions(dict.value.sys_enable_disable))

const searchSchema = computed(() => [
  {
    field: 'keyword',
    label: '关键字',
    type: 'input',
    props: {
      placeholder: '编码或名称',
      clearable: true,
    },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '全部状态',
      options: statusOptions.value,
      clearable: true,
    },
  },
])

const tableColumns = computed(() => [
  {
    prop: 'groupName',
    label: '用户组',
    minWidth: 220,
    render: row => h(SystemTableCell, {
      title: row.groupName || '-',
      subtitle: row.groupCode || '未设置编码',
      interactive: true,
      avatar: true,
      tooltip: `编辑用户组：${row.groupName || row.groupCode || '-'}`,
      onActivate: () => crudRef.value?.showEdit(row),
    }),
  },
  {
    prop: 'memberCount',
    label: '成员数',
    width: 88,
  },
  {
    prop: 'status',
    label: '状态',
    width: 88,
    render: row => h(DictTag, {
      options: statusOptions.value,
      value: row.status,
      forceTag: true,
    }),
  },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 220,
    ellipsis: { tooltip: true },
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    width: 168,
  },
  {
    prop: 'action',
    label: '操作',
    width: 180,
    fixed: 'right',
    actions: [
      { label: '成员', key: 'members', type: 'primary', onClick: openMembers },
      { label: '编辑', key: 'edit', type: 'primary', onClick: row => crudRef.value?.showEdit(row) },
      { label: '删除', key: 'delete', type: 'error', onClick: row => crudRef.value?.handleDelete(row) },
    ],
  },
])

const editSchema = computed(() => [
  {
    type: 'divider',
    label: '基础信息',
    span: 2,
    props: { titlePlacement: 'left' },
  },
  {
    field: 'groupCode',
    label: '用户组编码',
    type: 'input',
    rules: [
      { required: true, message: '请输入用户组编码', trigger: 'blur' },
      { pattern: /^\w[\w.:-]*$/, message: '仅支持字母、数字及 _ . : -', trigger: 'blur' },
    ],
    props: {
      placeholder: '如 finance_reviewers',
    },
    vIf: formData => !formData.id,
  },
  {
    field: 'groupName',
    label: '用户组名称',
    type: 'input',
    rules: [{ required: true, message: '请输入用户组名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入用户组名称',
    },
  },
  {
    field: 'status',
    label: '状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: statusOptions.value,
    },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: {
      rows: 3,
      placeholder: '可选',
    },
  },
])

const memberSelectionUsers = computed(() => members.value.map(item => ({
  id: String(item.userId),
  realName: item.realName || item.username || String(item.userId),
  name: item.realName || item.username || String(item.userId),
  username: item.username || String(item.userId),
})))

function beforeSubmit(formData) {
  const payload = {
    id: formData.id,
    groupCode: formData.groupCode,
    groupName: formData.groupName,
    status: formData.status,
    remark: formData.remark,
  }
  for (const key of ['groupCode', 'groupName', 'remark']) {
    if (typeof payload[key] === 'string')
      payload[key] = payload[key].trim()
  }
  payload.status = payload.status === null || payload.status === undefined || payload.status === ''
    ? 1
    : Number(payload.status)
  if (payload.id)
    delete payload.groupCode
  else
    delete payload.id
  return payload
}

async function openMembers(row) {
  if (memberLoading.value)
    return
  memberLoading.value = true
  try {
    const response = await flowApi.getUserGroupMembers(row.id)
    activeGroup.value = row
    members.value = Array.isArray(response.data) ? response.data : []
    memberModalVisible.value = true
  }
  catch (error) {
    window.$message?.error(error?.message || '加载用户组成员失败，请稍后重试')
  }
  finally {
    memberLoading.value = false
  }
}

async function handleMemberSelection(selectedUsers) {
  const selected = Array.isArray(selectedUsers) ? selectedUsers : []
  const currentIds = new Set(members.value.map(item => String(item.userId)))
  const selectedIds = new Set(selected.map(item => String(item?.id)).filter(Boolean))
  const added = [...selectedIds].filter(id => !currentIds.has(id))
  const removed = [...currentIds].filter(id => !selectedIds.has(id))

  if (!activeGroup.value || (!added.length && !removed.length))
    return

  memberLoading.value = true
  try {
    if (added.length)
      await flowApi.addUserGroupMembers(activeGroup.value.id, { userIds: added })
    if (removed.length)
      await flowApi.removeUserGroupMembers(activeGroup.value.id, { userIds: removed })
    window.$message?.success('成员已更新')
    memberModalVisible.value = false
    const response = await flowApi.getUserGroupMembers(activeGroup.value.id)
    members.value = Array.isArray(response.data) ? response.data : []
    crudRef.value?.refresh?.()
  }
  catch (error) {
    window.$message?.error(error?.message || '成员更新失败，请稍后重试')
  }
  finally {
    memberLoading.value = false
  }
}
</script>

<style scoped>
.flow-user-group-page {
  min-height: 0;
  height: 100%;
}
</style>
