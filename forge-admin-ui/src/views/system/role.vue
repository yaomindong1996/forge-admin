<template>
  <div class="system-role-page">
    <div class="role-workspace">
      <aside class="role-list-panel">
        <div class="role-tabs">
          <button
            v-for="tab in roleTypeTabs"
            :key="tab.value"
            type="button"
            :class="{ 'is-active': String(activeRoleType) === String(tab.value) }"
            @click="handleRoleTypeChange(tab.value)"
          >
            {{ tab.label }}
          </button>
          <n-button quaternary circle size="small" title="新增角色" @click="handleAddRole">
            <template #icon>
              <i class="i-material-symbols:add-rounded" />
            </template>
          </n-button>
        </div>

        <div class="role-search">
          <n-input
            v-model:value="roleKeyword"
            clearable
            size="small"
            placeholder="搜索角色"
            @clear="handleRoleSearch"
            @keyup.enter="handleRoleSearch"
          >
            <template #prefix>
              <i class="i-material-symbols:search-rounded" />
            </template>
          </n-input>
        </div>

        <n-spin :show="roleListLoading" class="role-list-spin">
          <div class="role-list">
            <button
              v-for="role in roleList"
              :key="role.id"
              class="role-list-item"
              :class="{ 'is-selected': currentRole.id === role.id }"
              type="button"
              @click="handleSelectRole(role)"
            >
              <span class="role-list-main">
                <strong :title="role.roleName">{{ role.roleName }}</strong>
                <small :title="role.roleKey || '-'">{{ role.roleKey || '-' }}</small>
              </span>
              <span class="role-list-side">
                <NTag v-if="Number(role.isSystem) === 1" size="small" :bordered="false">
                  系统
                </NTag>
                <span class="role-inline-actions" @click.stop>
                  <button type="button" title="编辑角色" aria-label="编辑角色" @click="handleEdit(role)">
                    <i class="i-material-symbols:edit-outline-rounded" />
                  </button>
                  <button type="button" title="适用组织" aria-label="适用组织" @click="handleRoleOrgScope(role)">
                    <i class="i-material-symbols:account-tree-rounded" />
                  </button>
                  <button type="button" title="权限授权" aria-label="权限授权" @click="handleAuth(role)">
                    <i class="i-material-symbols:admin-panel-settings-outline-rounded" />
                  </button>
                  <button
                    v-if="role.id !== 1"
                    type="button"
                    title="删除角色"
                    aria-label="删除角色"
                    class="type-error"
                    @click="handleDelete(role)"
                  >
                    <i class="i-material-symbols:delete-outline-rounded" />
                  </button>
                </span>
              </span>
            </button>
            <n-empty v-if="!roleListLoading && roleList.length === 0" description="暂无角色" size="small" />
          </div>
        </n-spin>
      </aside>

      <section class="role-user-panel">
        <header class="role-user-header">
          <div class="role-user-title">
            <h2>用户列表</h2>
            <NTag v-if="currentRole.id" size="small" :type="currentRoleScopeTagType" :bordered="false">
              {{ currentRoleScopeLabel }}
            </NTag>
          </div>
          <n-space size="small">
            <n-button
              size="small"
              secondary
              :disabled="!currentRole.id"
              @click="handleRoleOrgScope(currentRole)"
            >
              <template #icon>
                <i class="i-material-symbols:account-tree-rounded" />
              </template>
              适用组织
            </n-button>
            <n-button
              size="small"
              type="primary"
              :disabled="!currentRole.id || roleUserOrgOptions.length === 0"
              @click="handleAddUser"
            >
              <template #icon>
                <i class="i-material-symbols:person-add-rounded" />
              </template>
              添加用户
            </n-button>
            <n-button size="small" secondary :disabled="!currentRole.id" @click="loadRoleUsers">
              <template #icon>
                <i class="i-material-symbols:refresh-rounded" />
              </template>
              刷新
            </n-button>
          </n-space>
        </header>

        <div class="role-user-search">
          <n-input
            v-model:value="roleUserKeyword"
            clearable
            size="small"
            placeholder="搜索账号"
            @clear="handleUserSearch"
            @keyup.enter="handleUserSearch"
          >
            <template #prefix>
              <i class="i-material-symbols:search-rounded" />
            </template>
          </n-input>
          <n-tree-select
            v-model:value="roleUserOrgId"
            placeholder="全部授权组织"
            clearable
            filterable
            size="small"
            :disabled="!currentRole.id || roleUserOrgOptions.length === 0"
            :options="roleUserOrgTreeOptions"
            key-field="value"
            label-field="label"
            children-field="children"
            @update:value="handleRoleUserOrgChange"
          />
          <n-select
            v-model:value="userSearchParams.userStatus"
            placeholder="状态"
            clearable
            size="small"
            :options="userStatusOptions"
            @update:value="handleUserSearch"
          />
          <n-button size="small" type="primary" :disabled="!currentRole.id" @click="handleUserSearch">
            查询
          </n-button>
          <n-button size="small" :disabled="!currentRole.id" @click="handleUserSearchReset">
            重置
          </n-button>
        </div>

        <div class="role-user-table">
          <n-data-table
            :columns="userTableColumns"
            :data="roleUsers"
            :loading="usersLoading"
            :pagination="userPaginationConfig"
            :row-key="row => row.id"
            remote
            striped
            size="small"
            flex-height
            @update:page="handleUserPageChange"
            @update:page-size="handleUserPageSizeChange"
          />
        </div>
      </section>
    </div>

    <div class="crud-driver" aria-hidden="true">
      <AiCrudPage
        ref="crudRef"
        api="/system/role"
        :api-config="{
          list: 'get@/system/role/page',
          detail: 'post@/system/role/getById',
          add: 'post@/system/role/add',
          update: 'post@/system/role/edit',
          delete: 'post@/system/role/removeBatch',
        }"
        :search-schema="searchSchema"
        :columns="tableColumns"
        :edit-schema="editSchema"
        :before-submit="beforeSubmit"
        row-key="id"
        :edit-grid-cols="2"
        edit-label-align="left"
        modal-width="800px"
        add-button-text="新增角色"
        :show-search="false"
        :show-pagination="false"
        :hide-toolbar="true"
        :hide-selection="true"
        :hide-batch-delete="true"
        @submit-success="handleRoleMutationSuccess"
      />
    </div>

    <!-- 授权弹窗 -->
    <n-modal
      v-model:show="authModalVisible"
      :title="`角色授权 - ${currentRole.roleName || ''}`"
      preset="card"
      style="width: 900px"
      :mask-closable="false"
    >
      <div class="auth-modal-content">
        <div class="auth-summary">
          <div class="auth-role-meta">
            <span class="auth-role-name">{{ currentRole.roleName || '-' }}</span>
            <span class="auth-role-key">{{ currentRole.roleKey || '-' }}</span>
          </div>
          <div class="auth-counts">
            <span>{{ currentAuthClientName }}</span>
            <span>可分配 {{ resourceCount }}</span>
            <span>已选择 {{ checkedResourceKeys.length }}</span>
          </div>
        </div>

        <div class="auth-client-tabs">
          <n-tabs
            type="segment"
            size="small"
            :value="currentAuthClientCode"
            @update:value="handleAuthClientChange"
          >
            <n-tab-pane
              v-for="client in authClientTabs"
              :key="client.clientCode"
              :name="client.clientCode"
              :tab="client.clientName"
            />
          </n-tabs>
        </div>

        <div class="auth-toolbar">
          <n-space size="small" align="center">
            <n-button size="small" :disabled="authLoading" @click="toggleExpandAll">
              <template #icon>
                <i :class="treeExpandAll ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
              </template>
              {{ treeExpandAll ? '折叠全部' : '展开全部' }}
            </n-button>
            <n-button size="small" :disabled="authLoading" @click="handleCheckAll">
              <template #icon>
                <i class="i-material-symbols:check-box-outline" />
              </template>
              全选
            </n-button>
            <n-button size="small" :disabled="authLoading" @click="handleUncheckAll">
              <template #icon>
                <i class="i-material-symbols:check-box-outline-blank" />
              </template>
              全不选
            </n-button>
            <n-checkbox v-model:checked="treeCascade" :disabled="authLoading" @update:checked="handleTreeCascadeChange">
              父子联动
            </n-checkbox>
          </n-space>
        </div>

        <n-tabs v-model:value="activeResourceTab" type="line" animated class="auth-tabs">
          <n-tab-pane name="all" tab="全部资源">
            <div class="auth-tree-container">
              <n-spin :show="authLoading">
                <div v-if="authLoading" class="auth-tree-skeleton">
                  <div
                    v-for="index in 10"
                    :key="index"
                    class="auth-tree-skeleton-row"
                    :style="{ paddingLeft: `${(index % 4) * 18}px` }"
                  >
                    <n-skeleton circle size="small" />
                    <n-skeleton text :width="`${72 - (index % 3) * 10}%`" />
                  </div>
                </div>
                <PremiumTree
                  v-else-if="resourceTreeData.length > 0"
                  :data="resourceTreeData"
                  :cascade-data="resourceTreeData"
                  checkable
                  :cascade="treeCascade"
                  :expanded-keys="treeExpandedKeys"
                  :checked-keys="checkedResourceKeys"
                  key-field="id"
                  label-field="resourceName"
                  children-field="children"
                  :get-node-icon="getResourceNodeIcon"
                  :get-node-meta="getResourceNodeMeta"
                  :get-node-tone="getResourceNodeTone"
                  show-meta
                  @update:expanded-keys="handleExpandedKeysChange"
                  @update:checked-keys="handleCheckedKeysChange"
                />
                <n-empty v-else description="暂无资源数据" />
              </n-spin>
            </div>
          </n-tab-pane>

          <n-tab-pane name="menu" tab="菜单">
            <div class="auth-tree-container">
              <n-spin :show="authLoading">
                <div v-if="authLoading" class="auth-tree-skeleton">
                  <div
                    v-for="index in 10"
                    :key="index"
                    class="auth-tree-skeleton-row"
                    :style="{ paddingLeft: `${(index % 4) * 18}px` }"
                  >
                    <n-skeleton circle size="small" />
                    <n-skeleton text :width="`${72 - (index % 3) * 10}%`" />
                  </div>
                </div>
                <PremiumTree
                  v-else-if="menuTreeData.length > 0"
                  :data="menuTreeData"
                  :cascade-data="resourceTreeData"
                  checkable
                  :cascade="treeCascade"
                  :expanded-keys="treeExpandedKeys"
                  :checked-keys="checkedResourceKeys"
                  key-field="id"
                  label-field="resourceName"
                  children-field="children"
                  :get-node-icon="getResourceNodeIcon"
                  :get-node-meta="getResourceNodeMeta"
                  :get-node-tone="getResourceNodeTone"
                  show-meta
                  @update:expanded-keys="handleExpandedKeysChange"
                  @update:checked-keys="handleCheckedKeysChange"
                />
                <n-empty v-else description="暂无菜单数据" />
              </n-spin>
            </div>
          </n-tab-pane>

          <n-tab-pane name="button" tab="按钮">
            <div class="auth-tree-container">
              <n-spin :show="authLoading">
                <div v-if="authLoading" class="auth-tree-skeleton">
                  <div
                    v-for="index in 10"
                    :key="index"
                    class="auth-tree-skeleton-row"
                    :style="{ paddingLeft: `${(index % 4) * 18}px` }"
                  >
                    <n-skeleton circle size="small" />
                    <n-skeleton text :width="`${72 - (index % 3) * 10}%`" />
                  </div>
                </div>
                <PremiumTree
                  v-else-if="buttonTreeData.length > 0"
                  :data="buttonTreeData"
                  :cascade-data="resourceTreeData"
                  checkable
                  :cascade="treeCascade"
                  :expanded-keys="treeExpandedKeys"
                  :checked-keys="checkedResourceKeys"
                  key-field="id"
                  label-field="resourceName"
                  children-field="children"
                  :get-node-icon="getResourceNodeIcon"
                  :get-node-meta="getResourceNodeMeta"
                  :get-node-tone="getResourceNodeTone"
                  show-meta
                  @update:expanded-keys="handleExpandedKeysChange"
                  @update:checked-keys="handleCheckedKeysChange"
                />
                <n-empty v-else description="暂无按钮数据" />
              </n-spin>
            </div>
          </n-tab-pane>

          <n-tab-pane name="api" tab="API接口">
            <div class="auth-tree-container">
              <n-spin :show="authLoading">
                <div v-if="authLoading" class="auth-tree-skeleton">
                  <div
                    v-for="index in 10"
                    :key="index"
                    class="auth-tree-skeleton-row"
                    :style="{ paddingLeft: `${(index % 4) * 18}px` }"
                  >
                    <n-skeleton circle size="small" />
                    <n-skeleton text :width="`${72 - (index % 3) * 10}%`" />
                  </div>
                </div>
                <PremiumTree
                  v-else-if="apiTreeData.length > 0"
                  :data="apiTreeData"
                  :cascade-data="resourceTreeData"
                  checkable
                  :cascade="treeCascade"
                  :expanded-keys="treeExpandedKeys"
                  :checked-keys="checkedResourceKeys"
                  key-field="id"
                  label-field="resourceName"
                  children-field="children"
                  :get-node-icon="getResourceNodeIcon"
                  :get-node-meta="getResourceNodeMeta"
                  :get-node-tone="getResourceNodeTone"
                  show-meta
                  @update:expanded-keys="handleExpandedKeysChange"
                  @update:checked-keys="handleCheckedKeysChange"
                />
                <n-empty v-else description="暂无API数据" />
              </n-spin>
            </div>
          </n-tab-pane>
        </n-tabs>
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button @click="authModalVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="authSubmitLoading"
            :disabled="authLoading"
            @click="handleSubmitAuth"
          >
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 添加用户弹窗 -->
    <UserSelectPanel
      :show="addUserModalVisible"
      :title="`添加用户到角色 - ${currentRole.roleName || ''}`"
      :confirm-loading="addUserLoading"
      :assigned-user-ids="assignedUserIds"
      :tenant-id="currentRole.tenantId"
      :initial-org-id="roleUserOrgId"
      :locked-org-id="roleUserOrgId"
      :direct-org-only="true"
      @update:show="val => addUserModalVisible = val"
      @confirm="handleConfirmAddUsers"
    />

    <!-- 角色适用组织弹窗 -->
    <n-modal
      v-model:show="roleOrgModalVisible"
      :title="`适用组织 - ${currentRole.roleName || ''}`"
      preset="card"
      style="width: 720px"
      :mask-closable="false"
    >
      <div class="role-org-modal-content">
        <div class="role-org-toolbar">
          <div class="role-scope-mode">
            <n-radio-group
              :value="roleScopeMode"
              size="small"
              @update:value="handleRoleScopeModeChange"
            >
              <n-radio-button value="global">
                租户全局
              </n-radio-button>
              <n-radio-button value="custom">
                指定组织
              </n-radio-button>
            </n-radio-group>
            <NTag size="small" :type="roleOrgScopeTagType" :bordered="false">
              {{ roleOrgScopeSummary }}
            </NTag>
          </div>
          <n-space size="small" align="center">
            <n-button size="small" :disabled="roleOrgLoading" @click="toggleRoleOrgExpandAll">
              <template #icon>
                <i :class="roleOrgTreeExpandAll ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
              </template>
              {{ roleOrgTreeExpandAll ? '折叠全部' : '展开全部' }}
            </n-button>
          </n-space>
        </div>
        <div class="auth-tree-container">
          <n-spin :show="roleOrgLoading">
            <PremiumTree
              v-if="roleOrgTreeData.length > 0"
              :data="roleOrgTreeData"
              :checkable="roleScopeMode === 'custom'"
              :cascade="false"
              :expanded-keys="roleOrgExpandedKeys"
              :checked-keys="roleScopeMode === 'global' ? allRoleOrgIds : checkedRoleOrgKeys"
              key-field="id"
              label-field="orgName"
              children-field="children"
              :get-node-icon="getOrgNodeIcon"
              :get-node-tone="getOrgNodeTone"
              @update:expanded-keys="handleRoleOrgExpandedKeysChange"
              @update:checked-keys="handleRoleOrgCheckedKeysChange"
            />
            <n-empty v-else description="暂无组织数据" />
          </n-spin>
        </div>
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button @click="roleOrgModalVisible = false">
            取消
          </n-button>
          <n-button type="primary" :loading="roleOrgSubmitLoading" @click="handleSubmitRoleOrgs">
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, onMounted, ref, watch } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import PremiumTree from '@/components/common/PremiumTree.vue'
import DictTag from '@/components/DictTag.vue'
import UserSelectPanel from '@/components/UserSelectPanel.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { request } from '@/utils'

defineOptions({ name: 'SystemRole' })

const USER_STATUS_DICT = 'sys_user_status'
const ROLE_DATA_SCOPE_DICT = 'sys_role_data_scope'
const ROLE_TYPE_DICT = 'sys_role_type'
const NORMAL_DISABLE_DICT = 'sys_normal_disable'
const YES_NO_DICT = 'sys_yes_no'

const crudRef = ref(null)
const userStore = useUserStore()
const tenantOptions = ref([])
const roleList = ref([])
const roleListLoading = ref(false)
const roleKeyword = ref('')
const activeRoleType = ref(null)
const ROLE_ORG_SCOPE_GLOBAL = 1
const ROLE_ORG_SCOPE_CUSTOM = 2

// 授权相关
const authModalVisible = ref(false)
const authLoading = ref(false)
const authSubmitLoading = ref(false)
const resourceTreeData = ref([])
const checkedResourceKeys = ref([])
const treeExpandAll = ref(true)
const treeExpandedKeys = ref([])
const treeCascade = ref(true) // 父子联动开关
const activeResourceTab = ref('all') // 当前选中的资源类型标签
const clientList = ref([])
const currentAuthClientCode = ref('pc')

// 用户列表相关
const usersLoading = ref(false)
const roleUsers = ref([]) // 角色下的用户列表
const currentRole = ref({})
const addUserModalVisible = ref(false)
const addUserLoading = ref(false)
const assignedUserIds = ref([]) // 当前角色已授权的用户ID列表
const roleUserOrgId = ref(null)
const roleUserKeyword = ref('')
const roleApplicableOrgIds = ref([])
const roleOrgTreeData = ref([])
const userSearchParams = ref({
  userStatus: null,
})
const userPagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
})

// 角色适用组织
const roleOrgModalVisible = ref(false)
const roleOrgLoading = ref(false)
const roleOrgSubmitLoading = ref(false)
const roleScopeMode = ref('global')
const checkedRoleOrgKeys = ref([])
const roleOrgExpandedKeys = ref([])
const roleOrgTreeExpandAll = ref(true)

const { dict } = useDict(USER_STATUS_DICT, ROLE_DATA_SCOPE_DICT, ROLE_TYPE_DICT, NORMAL_DISABLE_DICT, YES_NO_DICT)

const userStatusOptions = computed(() => toNumberOptions(dict.value[USER_STATUS_DICT]))
const dataScopeOptions = computed(() => toNumberOptions(dict.value[ROLE_DATA_SCOPE_DICT]))
const manageableDataScopeOptions = computed(() => {
  if (userStore.isAdmin)
    return dataScopeOptions.value
  const deniedScopes = Number(userStore.userType) === 2 ? [1, 2] : [1]
  return dataScopeOptions.value.filter(item => !deniedScopes.includes(Number(item.value)))
})
const roleTypeOptions = computed(() => toNumberOptions(dict.value[ROLE_TYPE_DICT]))
const roleStatusOptions = computed(() => toNumberOptions(dict.value[NORMAL_DISABLE_DICT]))
const yesNoOptions = computed(() => toNumberOptions(dict.value[YES_NO_DICT]))
const tenantSelectOptions = computed(() => tenantOptions.value.map(item => ({
  label: item.tenantName,
  value: item.id,
})))
const roleTypeTabs = computed(() => {
  const options = roleTypeOptions.value || []
  if (options.length > 0)
    return options.map(item => ({ label: item.label, value: item.value }))
  return [{ label: '角色', value: null }]
})
const isCurrentRoleGlobalScope = computed(() =>
  Number(currentRole.value?.orgScopeType ?? ROLE_ORG_SCOPE_GLOBAL) === ROLE_ORG_SCOPE_GLOBAL,
)
const roleUserOrgOptions = computed(() => {
  const scopedOrgIds = new Set(normalizeNumberList(roleApplicableOrgIds.value))
  return flattenOrgNodes(roleOrgTreeData.value)
    .filter(item => isCurrentRoleGlobalScope.value || scopedOrgIds.has(normalizeSingleNumber(item.id)))
    .map(item => ({
      label: item.orgName,
      value: normalizeSingleNumber(item.id),
    }))
    .filter(item => item.value !== null)
})
const roleUserOrgTreeOptions = computed(() => {
  const scopedOrgIds = new Set(normalizeNumberList(roleApplicableOrgIds.value))
  return buildRoleUserOrgTreeOptions(roleOrgTreeData.value, scopedOrgIds, isCurrentRoleGlobalScope.value)
})
const allRoleOrgIds = computed(() => flattenOrgNodes(roleOrgTreeData.value)
  .map(item => normalizeSingleNumber(item.id))
  .filter(item => item !== null))
const currentRoleScopeLabel = computed(() => {
  if (!currentRole.value?.id)
    return ''
  if (isCurrentRoleGlobalScope.value)
    return '租户全局'
  if (roleApplicableOrgIds.value.length > 0)
    return `${roleApplicableOrgIds.value.length} 个组织`
  return '未设置范围'
})
const currentRoleScopeTagType = computed(() => {
  if (isCurrentRoleGlobalScope.value)
    return 'success'
  return roleApplicableOrgIds.value.length > 0 ? 'info' : 'warning'
})
const roleOrgScopeSummary = computed(() => {
  if (allRoleOrgIds.value.length === 0)
    return '暂无组织'
  if (roleScopeMode.value === 'global')
    return `${allRoleOrgIds.value.length} 个组织`
  if (checkedRoleOrgKeys.value.length === 0)
    return '未设置'
  return `${checkedRoleOrgKeys.value.length} 个组织`
})
const roleOrgScopeTagType = computed(() => {
  if (allRoleOrgIds.value.length === 0 || (roleScopeMode.value === 'custom' && checkedRoleOrgKeys.value.length === 0))
    return 'warning'
  return roleScopeMode.value === 'global' ? 'success' : 'info'
})

// 计算分页配置
const userPaginationConfig = computed(() => ({
  page: userPagination.value.page,
  pageSize: userPagination.value.pageSize,
  itemCount: userPagination.value.itemCount,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100],
}))

// 资源类型映射
const resourceTypeMap = {
  1: { text: '目录', type: 'info', icon: 'i-material-symbols:folder-outline' },
  2: { text: '菜单', type: 'success', icon: 'i-material-symbols:menu' },
  3: { text: '按钮', type: 'warning', icon: 'i-material-symbols:smart-button-outline' },
  4: { text: 'API', type: 'error', icon: 'i-material-symbols:api' },
}

// 计算属性：过滤不同类型的资源
const menuTreeData = computed(() => {
  return filterResourceByType(resourceTreeData.value, [1, 2])
})

const buttonTreeData = computed(() => {
  return filterResourceByType(resourceTreeData.value, [3])
})

const apiTreeData = computed(() => {
  return filterResourceByType(resourceTreeData.value, [4])
})

const resourceCount = computed(() => countResources(resourceTreeData.value))
const authClientTabs = computed(() => {
  if (clientList.value.length > 0)
    return clientList.value
  return [{ clientCode: 'pc', clientName: 'PC端' }]
})
const currentAuthClientName = computed(() => {
  const client = authClientTabs.value.find(item => item.clientCode === currentAuthClientCode.value)
  return client?.clientName || currentAuthClientCode.value || '-'
})

watch(roleUserOrgOptions, (options) => {
  if (roleUserOrgId.value && options.some(item => item.value === roleUserOrgId.value))
    return
  roleUserOrgId.value = null
})

watch(roleTypeTabs, (tabs) => {
  if (activeRoleType.value !== null || tabs.length === 0)
    return
  activeRoleType.value = tabs[0].value
  loadRoleList()
}, { immediate: true })

function countResources(data) {
  if (!Array.isArray(data))
    return 0
  return data.reduce((count, item) => count + 1 + countResources(item.children), 0)
}

// 过滤指定类型的资源
function filterResourceByType(data, types) {
  if (!data || !Array.isArray(data))
    return []

  return data.reduce((result, item) => {
    // 如果当前节点类型匹配
    if (types.includes(item.resourceType)) {
      const newItem = { ...item }
      // 递归处理子节点
      if (item.children && item.children.length > 0) {
        newItem.children = filterResourceByType(item.children, types)
      }
      result.push(newItem)
    }
    else {
      // 当前节点类型不匹配，但需要检查子节点
      if (item.children && item.children.length > 0) {
        const filteredChildren = filterResourceByType(item.children, types)
        if (filteredChildren.length > 0) {
          result.push({
            ...item,
            children: filteredChildren,
          })
        }
      }
    }
    return result
  }, [])
}

function getResourceNodeIcon(node = {}) {
  return resourceTypeMap[Number(node.resourceType)]?.icon || 'i-material-symbols:radio-button-unchecked'
}

function getResourceNodeTone(node = {}) {
  const type = Number(node.resourceType)
  if (type === 1)
    return 'folder'
  if (type === 2)
    return 'menu'
  if (type === 3)
    return 'action'
  if (type === 4)
    return 'api'
  return 'root'
}

function getResourceNodeMeta(node = {}) {
  const childCount = node.children?.length || 0
  if (!childCount)
    return null
  return {
    label: '下级',
    value: childCount,
  }
}

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
    field: 'roleName',
    label: '角色名称',
    type: 'input',
    props: {
      placeholder: '请输入角色名称',
    },
  },
  {
    field: 'roleKey',
    label: '权限字符',
    type: 'input',
    props: {
      placeholder: '请输入权限字符',
    },
  },
  {
    field: 'roleType',
    label: '角色类型',
    type: 'select',
    props: {
      placeholder: '请选择角色类型',
      options: roleTypeOptions.value,
    },
  },
  {
    field: 'roleStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: roleStatusOptions.value,
    },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  ...(userStore.isAdmin
    ? [{
        prop: 'tenantName',
        label: '所属租户',
        width: 160,
        render: row => row.tenantName || row.tenantId || '-',
      }]
    : []),
  {
    prop: 'roleName',
    label: '角色名称',
    width: 150,
  },
  {
    prop: 'roleKey',
    label: '权限字符',
    width: 150,
  },
  {
    prop: 'roleType',
    label: '角色类型',
    width: 120,
    render: (row) => {
      return h(DictTag, { dictType: ROLE_TYPE_DICT, value: row.roleType, size: 'small', forceTag: true })
    },
  },
  {
    prop: 'dataScope',
    label: '数据范围',
    width: 150,
    render: (row) => {
      return h(DictTag, { dictType: ROLE_DATA_SCOPE_DICT, value: row.dataScope, size: 'small', forceTag: true })
    },
  },
  {
    prop: 'sort',
    label: '排序',
    width: 80,
  },
  {
    prop: 'roleStatus',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(DictTag, { dictType: NORMAL_DISABLE_DICT, value: row.roleStatus, size: 'small', forceTag: true })
    },
  },
  {
    prop: 'isSystem',
    label: '系统角色',
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: YES_NO_DICT, value: row.isSystem, size: 'small', forceTag: true })
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
    width: 260,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', onClick: handleEdit },
      { label: '适用组织', key: 'orgScope', type: 'info', onClick: handleRoleOrgScope },
      { label: '查看用户', key: 'viewUsers', onClick: handleViewUsers },
      { label: '添加用户', key: 'addUsers', type: 'success', onClick: handleAddUserFromList },
      { label: '授权', key: 'auth', onClick: handleAuth },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete, visible: row => row.id !== 1 },
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
  {
    field: 'roleName',
    label: '角色名称',
    type: 'input',
    rules: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入角色名称',
    },
  },
  {
    field: 'roleKey',
    label: '权限字符',
    type: 'input',
    rules: [{ required: true, message: '请输入权限字符', trigger: 'blur' }],
    props: {
      placeholder: '请输入权限字符，如：admin',
    },
  },
  {
    field: 'roleType',
    label: '角色类型',
    type: 'select',
    defaultValue: 1,
    rules: [{ required: true, type: 'number', message: '请选择角色类型', trigger: 'change' }],
    props: {
      placeholder: '请选择角色类型',
      options: roleTypeOptions.value,
    },
  },
  {
    field: 'dataScope',
    label: '数据范围',
    type: 'select',
    defaultValue: 2,
    rules: [{ required: true, type: 'number', message: '请选择数据范围', trigger: 'change' }],
    props: {
      placeholder: '请选择数据范围',
      options: manageableDataScopeOptions.value,
    },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'input-number',
    defaultValue: 0,
    props: {
      placeholder: '排序值',
      min: 0,
    },
  },
  {
    type: 'divider',
    label: '状态配置',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'roleStatus',
    label: '角色状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: roleStatusOptions.value,
    },
  },
  {
    field: 'isSystem',
    label: '系统角色',
    type: 'radio',
    defaultValue: 0,
    props: {
      options: yesNoOptions.value,
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

// 用户表格列配置（用于角色用户列表）
const userTableColumns = [
  {
    title: '用户',
    key: 'realName',
    minWidth: 180,
    render: row => h('div', { class: 'role-user-cell' }, [
      h('strong', resolveUserDisplayName(row)),
      h('em', ' / '),
      h('span', row.username || '-'),
    ]),
  },
  {
    title: '手机号',
    key: 'phone',
    width: 130,
    render: row => row.phone || '-',
  },
  {
    title: '状态',
    key: 'userStatus',
    width: 80,
    render: (row) => {
      return h(DictTag, { dictType: USER_STATUS_DICT, value: row.userStatus, size: 'small' })
    },
  },
  {
    title: '操作',
    key: 'action',
    width: 90,
    fixed: 'right',
    render: (row) => {
      return h('a', {
        class: 'text-error cursor-pointer hover:text-error-hover',
        onClick: () => handleRemoveUserRole(row),
      }, '移除')
    },
  },
]

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

function normalizeSingleNumber(value, fallback = null) {
  if (Array.isArray(value)) {
    const first = value.find(item => item !== null && item !== undefined && item !== '')
    return normalizeSingleNumber(first, fallback)
  }
  if (value === null || value === undefined || value === '')
    return fallback
  const numberValue = Number(value)
  return Number.isNaN(numberValue) ? fallback : numberValue
}

function normalizeNumberList(value) {
  const list = Array.isArray(value) ? value : (value === null || value === undefined || value === '' ? [] : [value])
  return Array.from(new Set(list
    .map(item => normalizeSingleNumber(item))
    .filter(item => item !== null)))
}

function resolveUserDisplayName(row = {}) {
  return row.realName || row.name || row.nickname || row.username || `用户${row.id}`
}

function flattenOrgNodes(list = []) {
  return (list || []).flatMap((item) => {
    const current = [item]
    const children = flattenOrgNodes(item.children || [])
    return [...current, ...children]
  })
}

function buildRoleUserOrgTreeOptions(list = [], scopedOrgIds = new Set(), globalScope = false) {
  return (list || [])
    .map((item) => {
      const value = normalizeSingleNumber(item.id)
      const children = buildRoleUserOrgTreeOptions(item.children || [], scopedOrgIds, globalScope)
      const selectable = value !== null && (globalScope || scopedOrgIds.has(value))
      if (!selectable && children.length === 0)
        return null
      return {
        label: item.orgName || item.label || '-',
        value,
        disabled: !selectable,
        children,
      }
    })
    .filter(Boolean)
}

function getOrgNodeIcon(node = {}) {
  if (!node.parentId || Number(node.parentId) === 0)
    return 'i-material-symbols:account-tree-rounded'
  if (node.children?.length)
    return 'i-material-symbols:corporate-fare-rounded'
  return 'i-material-symbols:groups-rounded'
}

function getOrgNodeTone(node = {}) {
  if (!node.parentId || Number(node.parentId) === 0)
    return 'folder'
  return node.children?.length ? 'folder' : 'menu'
}

function buildRoleTenantParams(tenantId = currentRole.value?.tenantId) {
  const resolvedTenantId = userStore.isAdmin ? tenantId : userStore.userInfo?.tenantId
  return resolvedTenantId ? { tenantId: resolvedTenantId } : {}
}

async function loadRoleOrgTree(tenantId = currentRole.value?.tenantId) {
  const res = await request.get('/system/org/tree', {
    params: buildRoleTenantParams(tenantId),
  })
  if (res.code === 200) {
    roleOrgTreeData.value = res.data || []
    if (roleOrgTreeExpandAll.value)
      roleOrgExpandedKeys.value = getAllKeys(roleOrgTreeData.value)
  }
}

async function loadRoleApplicableOrgIds(roleId = currentRole.value?.id) {
  if (!roleId) {
    roleApplicableOrgIds.value = []
    return []
  }
  const res = await request.get(`/system/role/${roleId}/orgs`)
  if (res.code === 200) {
    roleApplicableOrgIds.value = normalizeNumberList(res.data || [])
    return roleApplicableOrgIds.value
  }
  roleApplicableOrgIds.value = []
  return []
}

// 表单提交前处理
function beforeSubmit(formData) {
  if (!formData.id && formData.orgScopeType == null)
    formData.orgScopeType = ROLE_ORG_SCOPE_GLOBAL
  if (!userStore.isAdmin) {
    formData.tenantId = userStore.userInfo?.tenantId
    if (Number(userStore.userType) === 2 && [1, 2].includes(Number(formData.dataScope)))
      formData.dataScope = 5
    else if (Number(formData.dataScope) === 1)
      formData.dataScope = 2
  }
  else if (!formData.tenantId) {
    formData.tenantId = userStore.userInfo?.tenantId
  }
  return formData
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

async function loadRoleList() {
  try {
    roleListLoading.value = true
    const params = {
      pageNum: 1,
      pageSize: 200,
      roleName: roleKeyword.value || undefined,
      roleType: activeRoleType.value === null ? undefined : activeRoleType.value,
    }
    const res = await request.get('/system/role/page', { params })
    if (res.code === 200) {
      roleList.value = res.data?.records || res.data?.list || []
      if (!roleList.value.some(item => item.id === currentRole.value?.id)) {
        const firstRole = roleList.value[0]
        if (firstRole) {
          await handleSelectRole(firstRole)
        }
        else {
          currentRole.value = {}
          roleUsers.value = []
          userPagination.value.itemCount = 0
        }
      }
    }
  }
  catch (error) {
    console.error('加载角色列表失败:', error)
    window.$message.error('加载角色列表失败')
  }
  finally {
    roleListLoading.value = false
  }
}

function handleRoleTypeChange(value) {
  activeRoleType.value = value
  roleKeyword.value = ''
  loadRoleList()
}

function handleRoleSearch() {
  loadRoleList()
}

function handleAddRole() {
  crudRef.value?.showAdd()
}

async function handleSelectRole(row) {
  if (!row?.id)
    return
  currentRole.value = row
  roleUserKeyword.value = ''
  userSearchParams.value = {
    userStatus: null,
  }
  userPagination.value.page = 1
  roleUserOrgId.value = null
  try {
    await Promise.all([
      loadRoleOrgTree(row.tenantId),
      loadRoleApplicableOrgIds(row.id),
    ])
    await loadRoleUsers()
  }
  catch (error) {
    console.error('加载角色用户上下文失败:', error)
    window.$message.error('加载角色用户失败')
  }
}

async function handleRoleMutationSuccess() {
  await loadRoleList()
  crudRef.value?.refresh()
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除角色"${row.roleName}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/role/remove', null, { params: { id: row.id } })
        if (res.code === 200) {
          window.$message.success('删除成功')
          await handleRoleMutationSuccess()
        }
      }
      catch {
        window.$message.error('删除失败')
      }
    },
  })
}

async function handleRoleOrgScope(row) {
  if (!row?.id)
    return
  if (currentRole.value?.id !== row.id)
    await handleSelectRole(row)
  roleOrgModalVisible.value = true
  checkedRoleOrgKeys.value = []
  roleOrgExpandedKeys.value = []
  try {
    roleOrgLoading.value = true
    checkedRoleOrgKeys.value = normalizeNumberList(roleApplicableOrgIds.value)
    roleScopeMode.value = isCurrentRoleGlobalScope.value ? 'global' : 'custom'
  }
  catch (error) {
    console.error('加载角色适用组织失败:', error)
    window.$message.error('加载角色适用组织失败')
  }
  finally {
    roleOrgLoading.value = false
  }
}

function toggleRoleOrgExpandAll() {
  roleOrgTreeExpandAll.value = !roleOrgTreeExpandAll.value
  roleOrgExpandedKeys.value = roleOrgTreeExpandAll.value ? getAllKeys(roleOrgTreeData.value) : []
}

function handleRoleOrgExpandedKeysChange(keys) {
  roleOrgExpandedKeys.value = keys
}

function handleRoleOrgCheckedKeysChange(keys) {
  checkedRoleOrgKeys.value = normalizeNumberList(keys)
}

async function handleSubmitRoleOrgs() {
  const nextOrgIds = roleScopeMode.value === 'global'
    ? []
    : normalizeNumberList(checkedRoleOrgKeys.value)
  if (roleScopeMode.value === 'custom' && nextOrgIds.length === 0) {
    window.$message.warning('请至少选择一个适用组织')
    return
  }
  try {
    roleOrgSubmitLoading.value = true
    const res = await request.post(`/system/role/${currentRole.value.id}/orgs`, nextOrgIds)
    if (res.code === 200) {
      window.$message.success('适用组织保存成功')
      roleOrgModalVisible.value = false
      roleApplicableOrgIds.value = nextOrgIds
      currentRole.value.orgScopeType = roleScopeMode.value === 'global'
        ? ROLE_ORG_SCOPE_GLOBAL
        : ROLE_ORG_SCOPE_CUSTOM
      const listRole = roleList.value.find(item => Number(item.id) === Number(currentRole.value.id))
      if (listRole)
        listRole.orgScopeType = currentRole.value.orgScopeType
      if (!isCurrentRoleGlobalScope.value && roleUserOrgId.value && !roleApplicableOrgIds.value.includes(roleUserOrgId.value)) {
        roleUserOrgId.value = null
      }
      await loadRoleUsers()
    }
  }
  catch (error) {
    console.error('保存适用组织失败:', error)
    window.$message.error('保存适用组织失败')
  }
  finally {
    roleOrgSubmitLoading.value = false
  }
}

function handleRoleScopeModeChange(value) {
  roleScopeMode.value = value
  if (value === 'global')
    checkedRoleOrgKeys.value = normalizeNumberList(allRoleOrgIds.value)
}

// 查看角色用户
async function handleViewUsers(row) {
  await handleSelectRole(row)
}

// 加载角色用户列表
async function loadRoleUsers() {
  if (!currentRole.value?.id) {
    roleUsers.value = []
    userPagination.value.itemCount = 0
    return
  }
  try {
    usersLoading.value = true
    const keyword = String(roleUserKeyword.value || '').trim()
    const params = {
      username: keyword || undefined,
      userStatus: userSearchParams.value.userStatus,
      pageNum: userPagination.value.page,
      pageSize: userPagination.value.pageSize,
      orgId: roleUserOrgId.value || undefined,
    }
    // 过滤空值
    Object.keys(params).forEach((key) => {
      if (params[key] === '' || params[key] === null || params[key] === undefined) {
        delete params[key]
      }
    })

    const res = await request.get(`/system/role/${currentRole.value.id}/users`, { params })
    if (res.code === 200) {
      roleUsers.value = res.data.records || []
      userPagination.value.itemCount = res.data.total || 0
    }
  }
  catch (error) {
    console.error('加载角色用户失败:', error)
    window.$message.error('加载角色用户失败')
  }
  finally {
    usersLoading.value = false
  }
}

// 用户搜索
function handleUserSearch() {
  userPagination.value.page = 1
  loadRoleUsers()
}

function handleRoleUserOrgChange() {
  userPagination.value.page = 1
  loadRoleUsers()
}

// 用户搜索重置
function handleUserSearchReset() {
  roleUserKeyword.value = ''
  userSearchParams.value = {
    userStatus: null,
  }
  userPagination.value.page = 1
  loadRoleUsers()
}

// 用户分页变化
function handleUserPageChange(page) {
  userPagination.value.page = page
  loadRoleUsers()
}

// 用户分页大小变化
function handleUserPageSizeChange(pageSize) {
  userPagination.value.pageSize = pageSize
  userPagination.value.page = 1
  loadRoleUsers()
}

// 移除角色用户
async function handleRemoveUserRole(user) {
  const orgId = normalizeSingleNumber(roleUserOrgId.value)
  window.$dialog.warning({
    title: '确认移除',
    content: orgId === null
      ? `确定移除用户“${user.username}”在全部授权组织下的角色“${currentRole.value.roleName}”吗？`
      : `确定移除用户“${user.username}”在当前授权组织下的角色“${currentRole.value.roleName}”吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        if (orgId === null) {
          const res = await request.post('/system/role/removeUserRole', null, {
            params: {
              roleId: currentRole.value.id,
              userId: user.id,
            },
          })
          if (res.code === 200) {
            window.$message.success('移除成功')
            await loadRoleUsers()
          }
          return
        }
        const currentRes = await request.get(`/system/user/${user.id}/org-roles`, {
          params: {
            tenantId: currentRole.value.tenantId,
            orgId,
          },
        })
        const nextRoleIds = normalizeNumberList(currentRes.code === 200 ? currentRes.data : [])
          .filter(roleId => Number(roleId) !== Number(currentRole.value.id))
        const res = await request.post(`/system/user/${user.id}/org-roles`, {
          tenantId: currentRole.value.tenantId,
          orgId,
          roleIds: nextRoleIds,
        })
        if (res.code === 200) {
          window.$message.success('移除成功')
          await loadRoleUsers()
        }
      }
      catch (error) {
        console.error('移除用户失败:', error)
        window.$message.error('移除用户失败')
      }
    },
  })
}

// 加载角色已授权用户ID列表
async function loadAssignedUserIds() {
  const orgId = normalizeSingleNumber(roleUserOrgId.value)
  if (orgId === null) {
    assignedUserIds.value = []
    return
  }
  try {
    const res = await request.get(`/system/role/${currentRole.value.id}/users`, {
      params: { pageNum: 1, pageSize: 9999, orgId },
    })
    if (res.code === 200 && res.data) {
      assignedUserIds.value = (res.data.records || []).map(u => u.id)
    }
  }
  catch {
    assignedUserIds.value = []
  }
}

// 打开添加用户弹窗
async function handleAddUser() {
  if (!roleUserOrgId.value) {
    window.$message.warning('请选择授权组织后再添加用户')
    return
  }
  await loadAssignedUserIds()
  addUserModalVisible.value = true
}

// 从角色列表直接添加用户
async function handleAddUserFromList(row) {
  await handleSelectRole(row)
  if (!roleUserOrgId.value) {
    window.$message.warning('请先配置角色适用组织')
    return
  }
  await loadAssignedUserIds()
  addUserModalVisible.value = true
}

// 确认添加用户到角色
async function handleConfirmAddUsers(userIds) {
  if (!userIds || userIds.length === 0)
    return
  const orgId = normalizeSingleNumber(roleUserOrgId.value)
  if (orgId === null) {
    window.$message.warning('请选择授权组织')
    return
  }
  try {
    addUserLoading.value = true
    for (const userId of userIds) {
      const currentRes = await request.get(`/system/user/${userId}/org-roles`, {
        params: {
          tenantId: currentRole.value.tenantId,
          orgId,
        },
      })
      const roleIds = Array.from(new Set([
        ...normalizeNumberList(currentRes.code === 200 ? currentRes.data : []),
        normalizeSingleNumber(currentRole.value.id),
      ]))
      const res = await request.post(`/system/user/${userId}/org-roles`, {
        tenantId: currentRole.value.tenantId,
        orgId,
        roleIds,
      })
      if (res.code !== 200) {
        throw new Error(`用户 ${userId} 添加失败`)
      }
    }
    window.$message.success(`成功添加 ${userIds.length} 个用户`)
    addUserModalVisible.value = false
    await loadRoleUsers()
  }
  catch (error) {
    console.error('添加用户失败:', error)
    window.$message.error('添加用户失败')
  }
  finally {
    addUserLoading.value = false
  }
}

// 授权
async function handleAuth(row) {
  if (!row?.id)
    return
  if (currentRole.value?.id !== row.id)
    await handleSelectRole(row)
  authModalVisible.value = true

  await loadClientList()
  if (!authClientTabs.value.some(item => item.clientCode === currentAuthClientCode.value)) {
    currentAuthClientCode.value = authClientTabs.value[0]?.clientCode || 'pc'
  }
  await loadAuthClientResources()
}

// 获取所有节点的 key（用于展开/收起）
function getAllKeys(list, keys = []) {
  list.forEach((item) => {
    keys.push(item.id)
    if (item.children && item.children.length > 0) {
      getAllKeys(item.children, keys)
    }
  })
  return keys
}

// 加载资源树
async function loadResourceTree() {
  try {
    const res = await request.get('/system/resource/assignable-tree', {
      params: { clientCode: currentAuthClientCode.value },
    })
    if (res.code === 200) {
      resourceTreeData.value = res.data || []

      // 如果默认展开，收集所有节点的 key
      if (treeExpandAll.value) {
        treeExpandedKeys.value = getAllKeys(resourceTreeData.value)
      }
    }
  }
  catch (error) {
    console.error('加载资源树失败:', error)
    window.$message.error('加载资源树失败')
  }
}

// 加载角色已有的资源
async function loadRoleResources(roleId) {
  try {
    const res = await request.get(`/system/role/${roleId}/resources`, {
      params: { clientCode: currentAuthClientCode.value },
    })
    if (res.code === 200) {
      checkedResourceKeys.value = res.data || []
    }
  }
  catch (error) {
    console.error('加载角色资源失败:', error)
    window.$message.error('加载角色资源失败')
  }
}

async function loadClientList() {
  try {
    const res = await request.get('/system/client/list')
    if (res.code === 200) {
      clientList.value = res.data || []
    }
  }
  catch (error) {
    console.error('加载客户端列表失败:', error)
  }
}

async function loadAuthClientResources() {
  authLoading.value = true
  checkedResourceKeys.value = []
  resourceTreeData.value = []
  treeExpandedKeys.value = []
  try {
    await Promise.all([
      loadResourceTree(),
      loadRoleResources(currentRole.value.id),
    ])
  }
  finally {
    authLoading.value = false
  }
}

async function handleAuthClientChange(clientCode) {
  currentAuthClientCode.value = clientCode
  activeResourceTab.value = 'all'
  await loadAuthClientResources()
}

// 展开的节点变化
function handleExpandedKeysChange(keys) {
  treeExpandedKeys.value = keys
}

// 选中的资源变化
function handleCheckedKeysChange(keys) {
  checkedResourceKeys.value = keys
}

// 展开/折叠所有
function toggleExpandAll() {
  treeExpandAll.value = !treeExpandAll.value

  if (treeExpandAll.value) {
    // 展开所有：获取所有节点的 key
    treeExpandedKeys.value = getAllKeys(resourceTreeData.value)
  }
  else {
    // 收起所有：清空展开的 key
    treeExpandedKeys.value = []
  }
}

// 全选
function handleCheckAll() {
  const allKeys = getAllKeys(resourceTreeData.value)
  checkedResourceKeys.value = allKeys
}

// 全不选
function handleUncheckAll() {
  checkedResourceKeys.value = []
}

// 父子联动开关变化
function handleTreeCascadeChange() {
  // 切换时保留当前选中状态
}

// 提交授权
async function handleSubmitAuth() {
  try {
    authSubmitLoading.value = true
    const res = await request.post(
      `/system/role/${currentRole.value.id}/resources`,
      checkedResourceKeys.value,
      { params: { clientCode: currentAuthClientCode.value } },
    )
    if (res.code === 200) {
      window.$message.success('授权成功')
      authModalVisible.value = false
    }
  }
  catch (error) {
    console.error('授权失败:', error)
    window.$message.error('授权失败')
  }
  finally {
    authSubmitLoading.value = false
  }
}

onMounted(() => {
  loadTenantOptions()
  loadRoleList()
})
</script>

<style scoped>
.system-role-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.role-workspace {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.role-list-panel {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #dbe4f0;
  background: #fff;
}

.role-tabs {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 2px;
  min-height: 42px;
  padding: 0 8px;
  border-bottom: 1px solid #eef2f7;
}

.role-tabs button {
  min-width: 0;
  height: 34px;
  padding: 0 10px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #334155;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.role-tabs button:hover,
.role-tabs button.is-active {
  color: #2563eb;
}

.role-tabs button.is-active {
  border-bottom-color: #2563eb;
}

.role-tabs :deep(.n-button) {
  margin-left: auto;
}

.role-search {
  flex-shrink: 0;
  padding: 8px;
  border-bottom: 1px solid #eef2f7;
}

.role-list-spin {
  flex: 1;
  min-height: 0;
}

.role-list-spin :deep(.n-spin-container),
.role-list-spin :deep(.n-spin-content) {
  height: 100%;
  min-height: 0;
}

.role-list {
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  padding: 4px 6px 8px;
}

.role-list-item {
  width: 100%;
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 6px;
  margin-bottom: 2px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #fff;
  color: #0f172a;
  cursor: pointer;
  text-align: left;
}

.role-list-item:hover {
  border-color: #e2e8f0;
  background: #f8fafc;
}

.role-list-item.is-selected {
  border-color: rgba(37, 99, 235, 0.18);
  background: rgba(37, 99, 235, 0.08);
  box-shadow: inset 3px 0 0 #2563eb;
}

.role-list-main {
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.role-list-main strong {
  overflow: hidden;
  color: inherit;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-list-main small {
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-list-side {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 4px;
}

.role-inline-actions {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.16s ease;
}

.role-list-item:hover .role-inline-actions,
.role-list-item.is-selected .role-inline-actions {
  opacity: 1;
  pointer-events: auto;
}

.role-inline-actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
}

.role-inline-actions button:hover {
  background: #eef2ff;
  color: #2563eb;
}

.role-inline-actions button.type-error:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.role-inline-actions i {
  font-size: 14px;
}

.role-user-panel {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
}

.role-user-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 42px;
  padding: 7px 10px;
  border-bottom: 1px solid #eef2f7;
}

.role-user-header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 14px;
  font-weight: 650;
}

.role-user-title {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.role-user-search {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: minmax(160px, 240px) minmax(180px, 240px) 120px auto auto;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-bottom: 1px solid #eef2f7;
}

.role-user-table {
  flex: 1;
  min-height: 0;
  padding: 8px 10px 10px;
  overflow: hidden;
}

.role-user-table :deep(.n-data-table) {
  height: 100%;
}

.role-user-cell {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-width: 0;
  gap: 2px;
  white-space: nowrap;
}

.role-user-cell strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
}

.role-user-cell em {
  flex: 0 0 auto;
  color: #cbd5e1;
  font-style: normal;
}

.role-user-cell span {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
}

.crud-driver {
  position: fixed;
  top: 0;
  left: -9999px;
  width: 1px;
  height: 1px;
  overflow: hidden;
  opacity: 0;
}

.crud-driver :deep(.n-modal-container),
.crud-driver :deep(.n-drawer-container) {
  opacity: 1;
}

/* 授权弹窗样式 */
.auth-modal-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 640px;
  min-height: 0;
  color: #1f2937;
}

.auth-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
  margin-bottom: 12px;
}

.auth-role-meta {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.auth-role-name {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.auth-role-key {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 2px 8px;
  border-radius: 6px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 12px;
  line-height: 20px;
}

.auth-counts {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #64748b;
  font-size: 12px;
}

.auth-client-tabs {
  margin-bottom: 12px;
}

.auth-client-tabs :deep(.n-tabs-nav) {
  max-width: 100%;
}

.auth-toolbar {
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.auth-toolbar :deep(.n-checkbox .n-checkbox__label) {
  color: #475569;
  font-size: 13px;
}

.role-org-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  margin-bottom: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.role-scope-mode {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

/* 标签页样式 */
.auth-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.auth-tabs :deep(.n-tabs-nav) {
  padding: 0 4px;
  background-color: transparent;
  border-bottom: 1px solid #e5e7eb;
}

.auth-tabs :deep(.n-tabs-tab) {
  font-weight: 500;
  padding: 10px 14px;
  color: #475569;
}

.auth-tabs :deep(.n-tabs-tab.n-tabs-tab--active) {
  color: #2563eb;
}

.auth-tabs :deep(.n-tabs-pane-wrapper) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.auth-tabs :deep(.n-tab-pane) {
  height: 100%;
  min-height: 0;
}

.auth-tabs :deep(.n-spin-container),
.auth-tabs :deep(.n-spin-content) {
  height: 100%;
  min-height: 0;
}

.auth-tree-container {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px 12px;
  min-height: 0;
  max-height: 450px;
  background-color: #fff;
}

.auth-tree-container :deep(.premium-tree) {
  padding-top: 2px;
}

.auth-tree-skeleton {
  display: flex;
  flex-direction: column;
  gap: 11px;
  padding: 4px 2px;
}

.auth-tree-skeleton-row {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 28px;
}

.auth-tree-skeleton-row :deep(.n-skeleton--circle) {
  flex: 0 0 auto;
}

/* 滚动条样式 */
.auth-tree-container::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.auth-tree-container::-webkit-scrollbar-track {
  background: var(--n-scrollbar-color);
  border-radius: 4px;
}

.auth-tree-container::-webkit-scrollbar-thumb {
  background: var(--n-scrollbar-color-hover);
  border-radius: 4px;
  transition: background 0.2s ease;
}

.auth-tree-container::-webkit-scrollbar-thumb:hover {
  background: var(--n-border-color);
}

/* 弹窗底部按钮样式 */
:deep(.n-card__footer) {
  padding: 14px 24px;
  border-top: 1px solid #e5e7eb;
  background-color: #f8fafc;
}

/* 深色模式 */
.dark .role-workspace,
.dark .role-list-panel,
.dark .role-user-panel {
  background: #0f172a !important;
  border-color: #334155;
}

.dark .role-tabs,
.dark .role-search,
.dark .role-user-header,
.dark .role-user-search {
  border-color: #334155;
  background: #0f172a;
}

.dark .role-tabs button {
  color: #cbd5e1;
}

.dark .role-tabs button:hover,
.dark .role-tabs button.is-active {
  color: #93c5fd;
}

.dark .role-list-item {
  background: #111827;
  color: #e2e8f0;
}

.dark .role-list-item:hover {
  border-color: #334155;
  background: #162033;
}

.dark .role-list-item.is-selected {
  border-color: rgba(96, 165, 250, 0.35);
  background: rgba(37, 99, 235, 0.18);
}

.dark .role-list-main small,
.dark .role-user-cell span {
  color: #94a3b8;
}

.dark .role-user-header h2,
.dark .role-user-cell strong {
  color: #f1f5f9;
}

.dark .role-inline-actions button:hover {
  background: rgba(30, 41, 59, 0.86);
  color: #bfdbfe;
}

.dark .role-inline-actions button.type-error:hover {
  background: rgba(239, 68, 68, 0.14);
  color: #fca5a5;
}

.dark .auth-modal-content {
  color: #e5e7eb;
}

.dark .auth-summary,
.dark .auth-toolbar,
.dark .role-org-toolbar {
  background: #111827;
  border-color: #334155;
}

.dark .auth-role-name {
  color: #f8fafc;
}

.dark .auth-role-key {
  background: #1e3a8a;
  color: #bfdbfe;
}

.dark .auth-counts,
.dark .auth-toolbar :deep(.n-checkbox .n-checkbox__label) {
  color: #94a3b8;
}

.dark .auth-tabs :deep(.n-tabs-nav) {
  border-color: #334155;
}

.dark .auth-tree-container {
  background: #0f172a;
  border-color: #334155;
}

@media (max-width: 1120px) {
  .role-workspace {
    grid-template-columns: 280px minmax(0, 1fr);
  }

  .role-user-search {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 860px) {
  .role-workspace {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(260px, 38vh) minmax(360px, 1fr);
  }

  .role-list-panel {
    border-right: 0;
    border-bottom: 1px solid #dbe4f0;
  }
}
</style>
