<template>
  <n-modal
    v-model:show="visible"
    preset="card"
    :title="title"
    style="width: 860px; max-width: 90vw"
    :mask-closable="false"
    @after-leave="handleClose"
  >
    <div class="user-select-panel">
      <!-- 左侧：组织树 -->
      <div class="panel-left">
        <div class="panel-left-header">
          <n-input
            v-model:value="orgSearchText"
            placeholder="搜索组织"
            clearable
            size="small"
          >
            <template #prefix>
              <i class="i-material-symbols:search text-14" />
            </template>
          </n-input>
        </div>
        <div class="panel-left-tree">
          <n-spin :show="orgLoading" size="small">
            <PremiumTree
              v-if="displayOrgTreeData.length > 0"
              :data="displayOrgTreeData"
              :selected-keys="selectedOrgKeys"
              :expanded-keys="displayOrgExpandedKeys"
              key-field="id"
              label-field="orgName"
              children-field="children"
              :get-node-icon="getOrgNodeIcon"
              :get-node-tone="getOrgNodeTone"
              @update:selected-keys="handleOrgSelect"
              @update:expanded-keys="handleOrgExpandedKeysChange"
            />
            <n-empty v-if="!orgLoading && orgTreeData.length === 0" description="暂无组织" size="small" />
            <n-empty v-else-if="!orgLoading && displayOrgTreeData.length === 0" description="未匹配到组织" size="small" />
          </n-spin>
        </div>
      </div>

      <!-- 右侧：用户列表 -->
      <div class="panel-right">
        <div class="panel-right-header">
          <n-input
            v-model:value="searchKeyword"
            placeholder="姓名 / 账号 / 手机号"
            clearable
            size="small"
            style="flex: 1"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <i class="i-material-symbols:search text-14" />
            </template>
          </n-input>
          <n-button size="small" type="primary" @click="handleSearch">
            搜索
          </n-button>
        </div>

        <div class="panel-right-list">
          <n-spin :show="userLoading" size="small">
            <n-checkbox-group v-model:value="checkedUserIds" class="user-checkbox-group">
              <div
                v-for="user in userList"
                :key="user.id"
                class="user-item"
                :class="{ 'is-checked': checkedUserIds.includes(user.id), 'is-assigned': assignedSet.has(user.id) }"
              >
                <n-checkbox :value="user.id" :disabled="assignedSet.has(user.id)" class="user-item-checkbox" />
                <div class="user-item-avatar">
                  <i class="i-material-symbols:person text-18" />
                </div>
                <div class="user-item-info">
                  <span class="user-item-name">{{ user.realName || user.username }}</span>
                  <span class="user-item-account">{{ user.username }}</span>
                  <span v-if="user.orgName" class="user-item-org">{{ user.orgName }}</span>
                  <span v-if="assignedSet.has(user.id)" class="user-item-assigned-tag">已授权</span>
                </div>
              </div>
            </n-checkbox-group>
            <n-empty v-if="!userLoading && userList.length === 0" description="暂无用户" size="small" class="mt-4" />
          </n-spin>
        </div>

        <!-- 分页 -->
        <div class="panel-right-footer">
          <span class="selected-count">已选 {{ checkedUserIds.length }} 人</span>
          <n-pagination
            v-model:page="pagination.page"
            :item-count="pagination.total"
            :page-size="pagination.pageSize"
            :page-sizes="[20, 50, 100]"
            size="small"
            show-size-picker
            @update:page="loadUserList"
            @update:page-size="handlePageSizeChange"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <n-space justify="end">
        <n-button @click="handleClose">
          取消
        </n-button>
        <n-button type="primary" :disabled="checkedUserIds.length === 0" :loading="confirmLoading" @click="handleConfirm">
          确定 ({{ checkedUserIds.length }})
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { getOrganizationNodeIcon as getOrgNodeIcon } from '@/components/common/organization-tree-icons'
import PremiumTree from '@/components/common/PremiumTree.vue'
import { request } from '@/utils'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: '选择用户',
  },
  confirmLoading: {
    type: Boolean,
    default: false,
  },
  assignedUserIds: {
    type: Array,
    default: () => [],
  },
  tenantId: {
    type: [Number, String],
    default: null,
  },
  initialOrgId: {
    type: [Number, String],
    default: null,
  },
  lockedOrgId: {
    type: [Number, String],
    default: null,
  },
  directOrgOnly: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:show', 'confirm'])

// 已授权用户 ID 集合（用于快速判断）
const assignedSet = computed(() => new Set(props.assignedUserIds))

const visible = ref(false)
const orgLoading = ref(false)
const userLoading = ref(false)

// 组织树
const orgTreeData = ref([])
const orgSearchText = ref('')
const selectedOrgKeys = ref([])
const orgExpandedKeys = ref([])

// 用户列表
const searchKeyword = ref('')
const userList = ref([])
const checkedUserIds = ref([])

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

const displayOrgTreeData = computed(() => {
  const keyword = orgSearchText.value.trim().toLowerCase()
  if (!keyword)
    return orgTreeData.value
  return filterOrgTree(orgTreeData.value, keyword)
})

const displayOrgExpandedKeys = computed(() => {
  if (orgSearchText.value.trim())
    return getAllKeys(displayOrgTreeData.value)
  return orgExpandedKeys.value
})

watch(() => props.show, async (val) => {
  visible.value = val
  if (val) {
    checkedUserIds.value = []
    selectedOrgKeys.value = resolveInitialOrgKeys()
    orgExpandedKeys.value = []
    searchKeyword.value = ''
    pagination.page = 1
    await loadOrgTree()
    await loadUserList()
  }
})

watch(visible, (val) => {
  emit('update:show', val)
})

async function loadOrgTree() {
  orgLoading.value = true
  try {
    const res = await request.get('/system/org/tree', {
      params: buildTenantParams(),
    })
    if (res.code === 200) {
      orgTreeData.value = res.data || []
      orgExpandedKeys.value = getAllKeys(orgTreeData.value)
    }
  }
  catch (e) {
    console.error('加载组织树失败', e)
  }
  finally {
    orgLoading.value = false
  }
}

async function loadUserList() {
  userLoading.value = true
  try {
    const params = {
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      ...buildTenantParams(),
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    if (selectedOrgKeys.value.length > 0) {
      params.orgId = selectedOrgKeys.value[0]
      if (props.directOrgOnly) {
        params.directOrgOnly = true
      }
    }
    const res = await request.get('/system/user/page', { params })
    if (res.code === 200 && res.data) {
      userList.value = (res.data.records || []).map(u => ({
        ...u,
        orgName: u.orgName || u.deptName || '',
      }))
      pagination.total = res.data.total || 0
    }
  }
  catch (e) {
    console.error('加载用户列表失败', e)
  }
  finally {
    userLoading.value = false
  }
}

function buildTenantParams() {
  return props.tenantId ? { tenantId: props.tenantId } : {}
}

function handleOrgSelect(keys) {
  if (props.lockedOrgId) {
    selectedOrgKeys.value = [Number(props.lockedOrgId)]
    return
  }
  selectedOrgKeys.value = keys
  pagination.page = 1
  loadUserList()
}

function resolveInitialOrgKeys() {
  const orgId = props.lockedOrgId || props.initialOrgId
  return orgId ? [Number(orgId)] : []
}

function handleOrgExpandedKeysChange(keys) {
  if (!orgSearchText.value.trim())
    orgExpandedKeys.value = keys
}

function getAllKeys(list = [], keys = []) {
  list.forEach((item) => {
    keys.push(item.id)
    if (item.children?.length)
      getAllKeys(item.children, keys)
  })
  return keys
}

function filterOrgTree(list = [], keyword) {
  return list
    .map((item) => {
      const children = filterOrgTree(item.children || [], keyword)
      const matched = String(item.orgName || '').toLowerCase().includes(keyword)
      if (!matched && children.length === 0)
        return null
      return {
        ...item,
        children,
      }
    })
    .filter(Boolean)
}

function getOrgNodeTone(node = {}) {
  if (!node.parentId || Number(node.parentId) === 0)
    return 'folder'
  return node.children?.length ? 'folder' : 'menu'
}

function handleSearch() {
  pagination.page = 1
  loadUserList()
}

function handlePageSizeChange(size) {
  pagination.pageSize = size
  pagination.page = 1
  loadUserList()
}

function handleConfirm() {
  emit('confirm', [...checkedUserIds.value])
}

function handleClose() {
  visible.value = false
  checkedUserIds.value = []
}
</script>

<style scoped>
.user-select-panel {
  display: flex;
  height: 500px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.panel-left {
  width: 220px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e5e7eb;
  background: #f9fafb;
}

.panel-left-header {
  padding: 10px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.panel-left-tree {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.panel-left-tree :deep(.n-spin-content) {
  width: 100%;
  align-items: stretch;
}

.panel-left-tree :deep(.premium-tree) {
  padding-top: 2px;
}

.panel-left-tree :deep(.premium-tree-row) {
  min-height: 30px;
  border-radius: 5px;
  border-color: transparent;
}

.panel-left-tree :deep(.premium-tree-row:hover) {
  background: #f8fafc;
  border-color: #e5e7eb;
}

.panel-left-tree :deep(.premium-tree-row.is-selected) {
  background: #f3f7ff;
  border-color: color-mix(in srgb, #2563eb 26%, #e5e7eb);
  color: #0f172a;
  box-shadow: none;
}

.panel-left-tree :deep(.premium-tree-icon) {
  width: 16px;
  height: 16px;
  margin-right: 5px;
  color: #94a3b8;
}

.panel-left-tree :deep(.premium-tree-icon i) {
  font-size: 13px;
}

.panel-left-tree :deep(.premium-tree-row.is-selected .premium-tree-icon) {
  color: #2563eb;
}

.panel-left-tree :deep(.premium-tree-row.is-selected .premium-tree-title) {
  color: #0f172a;
  font-weight: 650;
}

.panel-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.panel-right-header {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.panel-right-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
}

.user-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.user-item:hover {
  background: #f3f4f6;
}

.user-item.is-checked {
  background: #eff6ff;
}

.user-item.is-assigned {
  opacity: 0.7;
  cursor: default;
}

.user-item-assigned-tag {
  font-size: 11px;
  color: #fff;
  background: #10b981;
  padding: 1px 6px;
  border-radius: 3px;
  white-space: nowrap;
  flex-shrink: 0;
}

.user-item-checkbox {
  flex-shrink: 0;
}

.user-item-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e5e7eb;
  color: #6b7280;
  flex-shrink: 0;
}

.user-item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-item-name {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
  white-space: nowrap;
}

.user-item-account {
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
}

.user-item-org {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.panel-right-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.selected-count {
  font-size: 12px;
  color: #2563eb;
  font-weight: 500;
}

/* 深色模式 */
.dark .user-select-panel {
  border-color: #374151;
}

.dark .panel-left {
  border-color: #374151;
  background: #111827;
}

.dark .panel-left-header {
  border-color: #374151;
}

.dark .panel-left-tree :deep(.premium-tree-row:hover) {
  background: #162033;
  border-color: #334155;
}

.dark .panel-left-tree :deep(.premium-tree-row.is-selected) {
  background: rgba(37, 99, 235, 0.18);
  border-color: rgba(96, 165, 250, 0.28);
  box-shadow: none;
}

.dark .panel-left-tree :deep(.premium-tree-row.is-selected .premium-tree-title) {
  color: #f1f5f9;
}

.dark .panel-right-header,
.dark .panel-right-footer {
  border-color: #374151;
}

.dark .user-item:hover {
  background: #1f2937;
}

.dark .user-item.is-checked {
  background: #172554;
}

.dark .user-item.is-assigned {
  opacity: 0.6;
}

.dark .user-item-assigned-tag {
  background: #059669;
}

.dark .user-item-avatar {
  background: #374151;
  color: #9ca3af;
}

.dark .user-item-name {
  color: #f3f4f6;
}
</style>
