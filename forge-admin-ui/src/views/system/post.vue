<template>
  <div class="system-post-page">
    <MasterDetailWorkspace
      class="post-layout"
      :collapsed="leftOrgPanelCollapsed"
      :aside-width="220"
      :collapsed-aside-width="72"
    >
      <template #aside>
        <div class="org-tree-panel" :class="{ 'is-collapsed': leftOrgPanelCollapsed }">
          <div class="org-tree-header">
            <div class="header-title">
              <div class="header-icon">
                <i class="i-material-symbols:account-tree-rounded" />
              </div>
              <div v-if="!leftOrgPanelCollapsed" class="header-copy">
                <span>组织架构</span>
                <small>{{ orgTreeSummaryText }}</small>
              </div>
            </div>
            <div class="header-actions">
              <n-button
                v-if="!leftOrgPanelCollapsed"
                quaternary
                circle
                size="small"
                title="展开或折叠树节点"
                @click="toggleOrgExpandAll"
              >
                <template #icon>
                  <i :class="leftOrgExpandAll ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
                </template>
              </n-button>
              <n-button
                quaternary
                circle
                size="small"
                :title="leftOrgPanelCollapsed ? '展开左侧组织树' : '收起左侧组织树'"
                @click="toggleLeftOrgPanel"
              >
                <template #icon>
                  <i :class="leftOrgPanelCollapsed ? 'i-material-symbols:chevron-right-rounded' : 'i-material-symbols:left-panel-close-rounded'" />
                </template>
              </n-button>
            </div>
          </div>

          <div v-show="!leftOrgPanelCollapsed" class="org-tree-content">
            <n-spin :show="leftOrgTreeLoading">
              <div
                class="org-tree-all-node"
                :class="{ 'is-selected': isShowAllPosts }"
                @click="handleSelectAllPosts"
              >
                <i class="i-material-symbols:work-outline-rounded" />
                <span>全部岗位</span>
              </div>
              <PremiumTree
                v-if="leftOrgTreeData.length > 0"
                :data="leftOrgTreeData"
                :selected-keys="selectedOrgKeys"
                :expanded-keys="leftOrgExpandedKeys"
                key-field="id"
                label-field="orgName"
                children-field="children"
                :get-node-icon="getLeftOrgNodeIcon"
                :get-node-tone="getLeftOrgNodeTone"
                @update:selected-keys="handleOrgNodeSelect"
                @update:expanded-keys="handleLeftOrgExpandedKeysChange"
              />
              <n-empty v-else description="暂无组织数据" size="small" />
            </n-spin>
          </div>

          <div
            v-show="leftOrgPanelCollapsed"
            class="org-tree-collapsed-hint"
            :class="{ 'has-active-filter': selectedOrgNode && !isShowAllPosts }"
            @click="toggleLeftOrgPanel"
          >
            <i class="i-material-symbols:group-work-outline-rounded" />
            <span>组织筛选</span>
          </div>
        </div>
      </template>

      <section class="post-list-panel">
        <AiCrudPage
          ref="crudRef"
          api="/system/post"
          :api-config="{
            list: 'get@/system/post/page',
            detail: 'post@/system/post/getById',
            add: 'post@/system/post/add',
            update: 'post@/system/post/edit',
            delete: 'post@/system/post/removeBatch',
          }"
          :search-schema="searchSchema"
          :columns="tableColumns"
          :edit-schema="editSchema"
          :before-submit="beforeSubmit"
          :before-load-list="beforeLoadList"
          :before-search="beforeSearch"
          :before-render-form="beforeRenderForm"
          row-key="id"
          :edit-grid-cols="2"
          modal-width="800px"
          add-button-text="新增岗位"
          :hide-selection="false"
        >
          <template #toolbar-start>
            <div v-if="selectedOrgNode && !isShowAllPosts" class="org-filter-tip">
              <n-tag type="info" size="small" closable @close="handleClearOrgFilter">
                当前筛选：{{ selectedOrgNode.orgName }}
              </n-tag>
            </div>
          </template>
        </AiCrudPage>
      </section>
    </MasterDetailWorkspace>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import MasterDetailWorkspace from '@/components/common/MasterDetailWorkspace.vue'
import PremiumTree from '@/components/common/PremiumTree.vue'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { request } from '@/utils'

defineOptions({ name: 'SystemPost' })

const POST_TYPE_DICT = 'sys_post_type'
const NORMAL_DISABLE_DICT = 'sys_normal_disable'

const crudRef = ref(null)
const userStore = useUserStore()
const tenantOptions = ref([])
const leftOrgTreeData = ref([])
const leftOrgTreeLoading = ref(false)
const leftOrgExpandAll = ref(true)
const leftOrgExpandedKeys = ref([])
const leftOrgPanelCollapsed = ref(false)
const selectedOrgKeys = ref([])
const selectedOrgNode = ref(null)
const isShowAllPosts = ref(true)

const { dict } = useDict(POST_TYPE_DICT, NORMAL_DISABLE_DICT)

const postTypeOptions = computed(() => toNumberOptions(dict.value[POST_TYPE_DICT]))
const postStatusOptions = computed(() => toNumberOptions(dict.value[NORMAL_DISABLE_DICT]))
const tenantSelectOptions = computed(() => tenantOptions.value.map(item => ({
  label: item.tenantName,
  value: item.id,
})))

// 搜索表单配置
const searchSchema = computed(() => [
  ...(userStore.isAdmin
    ? [{
        field: 'tenantId',
        label: '所属租户',
        type: 'select',
        props: {
          placeholder: '请选择租户',
          clearable: true,
          options: tenantSelectOptions.value,
        },
      }]
    : []),
  {
    field: 'postName',
    label: '岗位名称',
    type: 'input',
    props: {
      placeholder: '请输入岗位名称',
    },
  },
  {
    field: 'postCode',
    label: '岗位编码',
    type: 'input',
    props: {
      placeholder: '请输入岗位编码',
    },
  },
  {
    field: 'postType',
    label: '岗位类型',
    type: 'select',
    props: {
      placeholder: '请选择岗位类型',
      options: postTypeOptions.value,
    },
  },
  {
    field: 'postStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: postStatusOptions.value,
    },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  ...(userStore.isAdmin
    ? [{
        prop: 'tenantId',
        label: '所属租户',
        width: 160,
        render: row => findTenantName(row.tenantId) || row.tenantId || '-',
      }]
    : []),
  {
    prop: 'postName',
    label: '岗位',
    minWidth: 190,
    render: row => h(SystemTableCell, {
      title: row.postName,
      subtitle: row.postCode,
      interactive: true,
      tooltip: `查看岗位：${row.postName || row.postCode || '-'}`,
      onActivate: () => crudRef.value?.showDetail(row),
    }),
  },
  {
    prop: 'orgName',
    label: '所属组织',
    minWidth: 160,
    render: row => h(SystemTableCell, {
      values: splitTableCellValues(row.orgName || row.orgNames),
    }),
  },
  {
    prop: 'postType',
    label: '岗位类型',
    width: 120,
    render: (row) => {
      return h(DictTag, { dictType: POST_TYPE_DICT, value: row.postType, size: 'small' })
    },
  },
  {
    prop: 'sort',
    label: '排序',
    width: 80,
  },
  {
    prop: 'postStatus',
    label: '状态',
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: NORMAL_DISABLE_DICT, value: row.postStatus, size: 'small' })
    },
  },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 150,
  },
  {
    prop: 'action',
    label: '操作',
    width: 140,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', onClick: handleEdit },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置
const editSchema = computed(() => [
  ...(userStore.isAdmin
    ? [{
        field: 'tenantId',
        label: '所属租户',
        type: 'select',
        defaultValue: userStore.userInfo?.tenantId,
        rules: [{ required: true, type: 'number', message: '请选择所属租户', trigger: 'change' }],
        props: {
          placeholder: '请选择所属租户',
          options: tenantSelectOptions.value,
        },
      }]
    : []),
  // 基础信息
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'postCode',
    label: '岗位编码',
    type: 'input',
    rules: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
    props: {
      placeholder: '请输入岗位编码',
    },
  },
  {
    field: 'postName',
    label: '岗位名称',
    type: 'input',
    rules: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入岗位名称',
    },
  },
  {
    field: 'orgId',
    label: '所属组织',
    type: 'treeSelect',
    required: true,
    rules: [{ required: true, type: 'number', message: '请选择所属组织', trigger: 'change' }],
    optionSource: {
      type: 'tree',
      api: 'get@/system/org/tree',
      // eslint-disable-next-line no-template-curly-in-string
      params: { tenantId: '${tenantId}' },
      valueField: 'id',
      keyField: 'id',
      labelField: 'orgName',
      childrenField: 'children',
    },
    props: {
      placeholder: '请选择所属组织',
      clearable: true,
      filterable: true,
      defaultExpandAll: true,
    },
  },
  {
    field: 'postType',
    label: '岗位类型',
    type: 'radio',
    defaultValue: 2,
    rules: [{ required: true, type: 'number', message: '请选择岗位类型', trigger: 'change' }],
    props: {
      options: postTypeOptions.value,
    },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'number',
    defaultValue: 0,
    props: {
      placeholder: '排序值',
      min: 0,
    },
  },

  // 状态配置
  {
    type: 'divider',
    label: '状态配置',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'postStatus',
    label: '岗位状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: postStatusOptions.value,
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

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

// 组件挂载时加载租户选项
onMounted(() => {
  loadLeftOrgTree()
  loadTenantOptions()
})

const orgTreeSummaryText = computed(() => {
  const total = countTreeNodes(leftOrgTreeData.value)
  if (!total)
    return '未加载组织'
  return `${total} 个组织节点`
})

function buildTenantParams(tenantId) {
  const resolvedTenantId = userStore.isAdmin ? tenantId : userStore.userInfo?.tenantId
  return resolvedTenantId ? { tenantId: resolvedTenantId } : {}
}

function resolveSelectedTenantId(row) {
  return row?.tenantId
    || (userStore.isAdmin ? crudRef.value?.getSearchParams?.()?.tenantId : null)
    || userStore.userInfo?.tenantId
}

function beforeLoadList(params) {
  Object.assign(params, buildTenantParams(params.tenantId))
  if (selectedOrgNode.value && !isShowAllPosts.value) {
    params.orgId = selectedOrgNode.value.id
  }
  return params
}

async function beforeSearch(params) {
  selectedOrgKeys.value = []
  selectedOrgNode.value = null
  isShowAllPosts.value = true
  await loadLeftOrgTree(params?.tenantId)
  return params
}

function beforeSubmit(formData) {
  if (!userStore.isAdmin) {
    formData.tenantId = userStore.userInfo?.tenantId
  }
  else if (!formData.tenantId) {
    formData.tenantId = userStore.userInfo?.tenantId
  }
  return formData
}

function beforeRenderForm(row) {
  if (row) {
    return row
  }
  return {
    tenantId: resolveSelectedTenantId(),
    orgId: selectedOrgNode.value?.id,
  }
}

function findTenantName(tenantId) {
  return tenantOptions.value.find(item => String(item.id) === String(tenantId))?.tenantName
}

async function loadTenantOptions() {
  try {
    const res = await request.get('/system/tenant/assignable/options')
    if (res.code === 200) {
      tenantOptions.value = res.data || []
    }
  }
  catch (error) {
    console.error('加载租户选项失败:', error)
  }
}

function getAllKeys(list, keys = []) {
  list.forEach((item) => {
    keys.push(item.id)
    if (item.children && item.children.length > 0)
      getAllKeys(item.children, keys)
  })
  return keys
}

function countTreeNodes(list = []) {
  return list.reduce((total, item) => total + 1 + countTreeNodes(item.children || []), 0)
}

function isSameKey(left, right) {
  return String(left) === String(right)
}

function getLeftOrgNodeIcon(node = {}) {
  if (!node.parentId || Number(node.parentId) === 0)
    return 'i-material-symbols:account-tree-rounded'
  if (node.children?.length)
    return 'i-material-symbols:account-tree-rounded'
  return 'i-material-symbols:domain-rounded'
}

function getLeftOrgNodeTone(node = {}) {
  if (!node.parentId || Number(node.parentId) === 0)
    return 'folder'
  return node.children?.length ? 'folder' : 'menu'
}

function normalizeOrgTreeNodes(list = []) {
  return (list || []).map(item => ({
    ...item,
    children: Array.isArray(item.children) ? normalizeOrgTreeNodes(item.children) : [],
  }))
}

async function fetchOrgRootNodes(tenantId) {
  const res = await request.get('/system/org/lazyTree', {
    params: {
      parentId: 0,
      ...buildTenantParams(tenantId),
    },
  })
  if (res.code !== 200)
    return []
  return normalizeOrgTreeNodes(res.data || [])
}

async function fetchOrgChildrenNodes(parentId, tenantId) {
  const res = await request.get(`/system/org/children/${parentId}`, {
    params: buildTenantParams(tenantId),
  })
  if (res.code !== 200)
    return []
  return normalizeOrgTreeNodes(res.data || [])
}

async function hydrateOrgTreeNodes(nodes, tenantId) {
  await Promise.all((nodes || []).map(async (node) => {
    if (!node?.hasChildren) {
      node.children = Array.isArray(node.children) ? node.children : []
      return
    }
    const children = await fetchOrgChildrenNodes(node.id, tenantId)
    node.children = children
    if (children.length > 0)
      await hydrateOrgTreeNodes(children, tenantId)
  }))
}

async function loadCompleteOrgTree(tenantId) {
  const roots = await fetchOrgRootNodes(tenantId)
  await hydrateOrgTreeNodes(roots, tenantId)
  return roots
}

async function loadLeftOrgTree(tenantId) {
  try {
    leftOrgTreeLoading.value = true
    leftOrgTreeData.value = await loadCompleteOrgTree(tenantId)
    if (leftOrgExpandAll.value)
      leftOrgExpandedKeys.value = getAllKeys(leftOrgTreeData.value)
  }
  catch (error) {
    console.error('加载组织树失败:', error)
    window.$message.error('加载组织树失败')
  }
  finally {
    leftOrgTreeLoading.value = false
  }
}

function findOrgNode(treeData, orgId) {
  for (const node of treeData) {
    if (isSameKey(node.id, orgId))
      return node
    if (node.children && node.children.length > 0) {
      const found = findOrgNode(node.children, orgId)
      if (found)
        return found
    }
  }
  return null
}

function handleOrgNodeSelect(keys) {
  selectedOrgKeys.value = keys

  if (keys.length > 0) {
    isShowAllPosts.value = false
    selectedOrgNode.value = findOrgNode(leftOrgTreeData.value, keys[0])
  }
  else {
    selectedOrgNode.value = null
  }
  crudRef.value?.refresh()
}

function handleLeftOrgExpandedKeysChange(keys) {
  leftOrgExpandedKeys.value = keys
}

function toggleOrgExpandAll() {
  leftOrgExpandAll.value = !leftOrgExpandAll.value
  leftOrgExpandedKeys.value = leftOrgExpandAll.value ? getAllKeys(leftOrgTreeData.value) : []
}

function toggleLeftOrgPanel() {
  leftOrgPanelCollapsed.value = !leftOrgPanelCollapsed.value
}

function handleClearOrgFilter() {
  selectedOrgKeys.value = []
  selectedOrgNode.value = null
  isShowAllPosts.value = true
  crudRef.value?.refresh()
}

function handleSelectAllPosts() {
  isShowAllPosts.value = true
  selectedOrgKeys.value = []
  selectedOrgNode.value = null
  crudRef.value?.refresh()
}

function splitTableCellValues(value) {
  const rawValues = Array.isArray(value) ? value : [value]
  return rawValues
    .flatMap(item => String(item || '').split(/[、,，]/))
    .map(item => item.trim())
    .filter(Boolean)
}

// 编辑
async function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除岗位"${row.postName}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/post/remove', null, { params: { id: row.id } })
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
</script>

<style scoped>
.system-post-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.post-layout {
  flex: 1;
  min-height: 0;
}

.org-tree-panel {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.org-tree-panel.is-collapsed .org-tree-header {
  flex-direction: column;
  justify-content: flex-start;
  gap: 10px;
  padding: 12px 8px;
}

.org-tree-panel.is-collapsed .header-title,
.org-tree-panel.is-collapsed .header-actions {
  width: 100%;
  justify-content: center;
}

.org-tree-header {
  padding: 8px 10px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  background: #fff;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.header-icon {
  width: 24px;
  height: 24px;
  min-width: 24px;
  min-height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 5px;
  background: transparent;
  color: #64748b;
  box-shadow: none;
}

.header-icon i {
  font-size: 16px;
}

.header-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-copy span {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
}

.header-copy small {
  color: #64748b;
  font-size: 11px;
  line-height: 1.2;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.org-tree-header :deep(.n-button) {
  color: #475569;
}

.org-tree-header :deep(.n-button:hover) {
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
}

.org-tree-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  padding: 6px;
}

.org-tree-content :deep(.n-spin-content) {
  width: 100%;
  align-items: stretch;
}

.org-tree-all-node {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  margin-bottom: 4px;
  border: 1px solid transparent;
  border-radius: 4px;
  color: #334155;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.org-tree-all-node:hover {
  border-color: #e5e7eb;
  background-color: #f8fafc;
}

.org-tree-all-node.is-selected {
  border-color: color-mix(in srgb, #2563eb 26%, #e5e7eb);
  background: #f3f7ff !important;
  box-shadow: none;
  color: #2563eb;
}

.org-tree-all-node i {
  font-size: 15px;
}

.org-tree-content :deep(.premium-tree) {
  padding-top: 2px;
}

.org-tree-content :deep(.premium-tree-row) {
  min-height: 30px;
  border-radius: 5px;
  border-color: transparent;
}

.org-tree-content :deep(.premium-tree-row:hover) {
  background: #f8fafc;
  border-color: #e5e7eb;
}

.org-tree-content :deep(.premium-tree-row.is-selected) {
  background: #f3f7ff;
  border-color: color-mix(in srgb, #2563eb 26%, #e5e7eb);
  color: #0f172a;
  box-shadow: none;
}

.org-tree-content :deep(.premium-tree-icon) {
  width: 16px;
  height: 16px;
  margin-right: 5px;
  color: #94a3b8;
}

.org-tree-content :deep(.premium-tree-icon i) {
  font-size: 13px;
}

.org-tree-content :deep(.premium-tree-row.is-selected .premium-tree-icon) {
  color: #2563eb;
}

.org-tree-content :deep(.premium-tree-row.is-selected .premium-tree-title) {
  color: #0f172a;
  font-weight: 650;
}

.org-tree-content::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.org-tree-content::-webkit-scrollbar-track {
  background: transparent;
}

.org-tree-content::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.org-tree-content::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

.org-tree-collapsed-hint {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 14px 6px;
  color: #64748b;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.org-tree-collapsed-hint i {
  font-size: 22px;
}

.org-tree-collapsed-hint span {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 2px;
  writing-mode: vertical-rl;
}

.org-tree-collapsed-hint:hover {
  background: rgba(37, 99, 235, 0.06);
  color: #2563eb;
}

.org-tree-collapsed-hint.has-active-filter {
  color: #2563eb;
}

.post-list-panel {
  flex: 1;
  height: 100%;
  min-width: 0;
  overflow: hidden;
}

.post-list-panel :deep(.ai-crud-page) {
  height: 100%;
}

.org-filter-tip {
  margin-right: 12px;
}

.org-filter-tip :deep(.n-tag) {
  font-size: 13px;
}

.dark .org-tree-header {
  border-bottom-color: #334155;
  background: #0f172a;
}

.dark .org-tree-header .header-copy span {
  color: #f1f5f9;
}

.dark .org-tree-header .header-copy small {
  color: #94a3b8;
}

.dark .header-icon {
  background: transparent;
  color: #94a3b8;
}

.dark .org-tree-content {
  background: #0f172a;
}

.dark .org-tree-all-node {
  color: #e2e8f0;
}

.dark .org-tree-all-node:hover {
  border-color: #334155;
  background-color: #162033;
}

.dark .org-tree-all-node.is-selected {
  border-color: rgba(96, 165, 250, 0.28);
  background: rgba(37, 99, 235, 0.18) !important;
  color: #60a5fa;
  box-shadow: none;
}

.dark .org-tree-content :deep(.premium-tree-row:hover) {
  background: #162033;
  border-color: #334155;
}

.dark .org-tree-content :deep(.premium-tree-row.is-selected) {
  background: rgba(37, 99, 235, 0.18);
  border-color: rgba(96, 165, 250, 0.28);
  box-shadow: none;
}

.dark .org-tree-content :deep(.premium-tree-row.is-selected .premium-tree-title) {
  color: #f1f5f9;
}

.dark .org-tree-collapsed-hint {
  color: #94a3b8;
}

.dark .org-tree-collapsed-hint:hover,
.dark .org-tree-collapsed-hint.has-active-filter {
  background: rgba(37, 99, 235, 0.14);
  color: #60a5fa;
}

.dark .org-tree-header :deep(.n-button) {
  color: #cbd5e1;
}

.dark .org-tree-header :deep(.n-button:hover) {
  background: rgba(96, 165, 250, 0.12);
  color: #60a5fa;
}

.dark .org-tree-content::-webkit-scrollbar-track {
  background: #1e293b;
}

.dark .org-tree-content::-webkit-scrollbar-thumb {
  background: #475569;
}

.dark .org-tree-content::-webkit-scrollbar-thumb:hover {
  background: #64748b;
}

.dark .org-filter-tip :deep(.n-tag) {
  border-color: #334155;
  background: #1e293b;
}

@media (max-width: 960px) {
  .org-tree-collapsed-hint {
    flex-direction: row;
    padding: 12px;
  }

  .org-tree-collapsed-hint span {
    letter-spacing: 0;
    writing-mode: initial;
  }
}
</style>
