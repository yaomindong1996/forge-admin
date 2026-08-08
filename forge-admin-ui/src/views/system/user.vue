<template>
  <div class="system-user-page">
    <!-- 左侧组织树 + 右侧用户列表布局 -->
    <MasterDetailWorkspace
      class="user-layout"
      :collapsed="leftOrgPanelCollapsed"
      :aside-width="220"
      :collapsed-aside-width="72"
    >
      <!-- 左侧组织树面板 -->
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
                :class="{ 'is-selected': isShowAllUsers }"
                @click="handleSelectAllUsers"
              >
                <i class="i-material-symbols:groups-rounded" />
                <span>全部用户</span>
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
            :class="{ 'has-active-filter': selectedOrgNode && !isShowAllUsers }"
            @click="toggleLeftOrgPanel"
          >
            <i class="i-material-symbols:group-work-outline-rounded" />
            <span>组织筛选</span>
          </div>
        </div>
      </template>

      <!-- 右侧用户列表 -->
      <section class="user-list-panel">
        <AiCrudPage
          ref="crudRef"
          api="/system/user"
          :api-config="{
            list: 'get@/system/user/page',
            detail: 'post@/system/user/getById',
            add: 'post@/system/user/add',
            update: 'post@/system/user/edit',
            delete: 'post@/system/user/removeBatch',
            export: 'post@/api/excel/export/sys_user_export',
          }"
          :search-schema="searchSchema"
          :columns="tableColumns"
          :edit-schema="editSchema"
          :before-submit="beforeSubmit"
          :before-load-list="beforeLoadList"
          :before-search="beforeSearch"
          :before-render-form="beforeRenderUserForm"
          :before-render-detail="beforeRenderUserDetail"
          :load-detail-on-edit="true"
          row-key="id"
          :edit-grid-cols="2"
          modal-width="900px"
          add-button-text="新增用户"
          :show-import="true"
          import-api="/system/user/import"
          import-template-url="/api/excel/template/sys_user_import"
          :show-export="true"
          export-button-text="导出用户"
          export-file-name="用户列表.xlsx"
          @selection-change="handleUserSelectionChange"
        >
          <!-- 自定义工具栏提示 -->
          <template #toolbar-start>
            <div v-if="selectedOrgNode && !isShowAllUsers" class="org-filter-tip">
              <NTag type="info" size="small" closable @close="handleClearOrgFilter">
                当前筛选：{{ selectedOrgNode.orgName }}
              </NTag>
            </div>
          </template>
          <template #toolbar-end>
            <n-dropdown
              trigger="click"
              placement="bottom-start"
              :options="userBatchActionOptions"
              @select="handleUserBatchActionSelect"
            >
              <n-button size="small" secondary :disabled="selectedUserIds.length === 0">
                <template #icon>
                  <i class="i-material-symbols:checklist-rounded" />
                </template>
                批量操作
              </n-button>
            </n-dropdown>
          </template>
        </AiCrudPage>
      </section>
    </MasterDetailWorkspace>

    <!-- 重置密码弹窗 -->
    <n-modal
      v-model:show="resetPwdModalVisible"
      title="重置密码"
      preset="card"
      style="width: 400px"
    >
      <n-form
        ref="resetPwdFormRef"
        :model="resetPwdForm"
        :rules="resetPwdRules"
        label-placement="left"
        label-width="80"
      >
        <n-form-item label="新密码" path="password">
          <n-input
            v-model:value="resetPwdForm.password"
            type="password"
            show-password-on="click"
            placeholder="请输入新密码"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="resetPwdModalVisible = false">
            取消
          </n-button>
          <n-button type="primary" :loading="resetPwdLoading" @click="handleConfirmResetPwd">
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 关联管理弹窗（整合授权/组织/岗位/租户） -->
    <n-modal
      v-model:show="relationModalVisible"
      :title="`关联管理 - ${currentUser.username || ''}`"
      preset="card"
      style="width: 880px"
      :mask-closable="false"
    >
      <n-tabs v-model:value="relationActiveTab" type="line" @update:value="handleRelationTabChange">
        <!-- 角色授权 -->
        <n-tab-pane name="auth" tab="角色授权">
          <div class="auth-modal-content">
            <n-alert type="info" :bordered="false" class="batch-action-alert">
              角色授权按组织生效，切换授权组织后会分别加载该组织可用角色和用户已拥有角色。
            </n-alert>
            <n-form label-placement="left" label-width="90" class="batch-action-form">
              <n-form-item label="授权组织">
                <n-select
                  v-model:value="authOrgId"
                  :options="authOrgOptions"
                  placeholder="请选择授权组织"
                  filterable
                  :disabled="authOrgOptions.length === 0"
                />
              </n-form-item>
            </n-form>

            <div class="auth-toolbar">
              <n-input
                v-model:value="roleSearchKeyword"
                class="auth-role-search"
                clearable
                size="small"
                placeholder="按角色名称搜索"
                @clear="handleRoleSearch"
                @keyup.enter="handleRoleSearch"
              >
                <template #prefix>
                  <i class="i-material-symbols:search-rounded" />
                </template>
              </n-input>
              <n-space size="small" class="auth-toolbar-actions">
                <n-button size="small" @click="handleRoleSearch">
                  <template #icon>
                    <i class="i-material-symbols:search-rounded" />
                  </template>
                  查询
                </n-button>
                <n-button size="small" @click="handleCheckAll">
                  <template #icon>
                    <i class="i-material-symbols:check-box-outline" />
                  </template>
                  全选本页
                </n-button>
                <n-button size="small" @click="handleUncheckAll">
                  <template #icon>
                    <i class="i-material-symbols:check-box-outline-blank" />
                  </template>
                  清空选择
                </n-button>
              </n-space>
            </div>

            <div class="auth-tree-container">
              <n-spin :show="authLoading">
                <n-data-table
                  :columns="authRoleColumns"
                  :data="orderedRoleTableData"
                  :checked-row-keys="checkedRoleKeys"
                  :pagination="rolePaginationConfig"
                  :row-key="row => row.id"
                  remote
                  striped
                  size="small"
                  @update:checked-row-keys="handleCheckedKeysChange"
                  @update:page="handleRolePageChange"
                  @update:page-size="handleRolePageSizeChange"
                />
              </n-spin>
            </div>
          </div>
        </n-tab-pane>

        <!-- 组织绑定 -->
        <n-tab-pane name="org" tab="组织绑定">
          <div class="org-modal-content">
            <div class="org-toolbar">
              <n-space size="small">
                <n-button size="small" @click="toggleUserOrgExpandAll">
                  <template #icon>
                    <i :class="orgTreeExpandAll ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
                  </template>
                  {{ orgTreeExpandAll ? '折叠全部' : '展开全部' }}
                </n-button>
              </n-space>
              <div class="org-main-hint">
                <span>已选 {{ checkedOrgKeys.length }} 个组织</span>
                <strong>主组织：{{ mainOrgName || '请选择主组织' }}</strong>
              </div>
            </div>
            <n-form label-placement="left" label-width="90" class="org-main-form">
              <n-form-item label="主组织">
                <n-select
                  v-model:value="mainOrgId"
                  :options="selectedOrgOptions"
                  placeholder="请选择主组织"
                  filterable
                  :disabled="checkedOrgKeys.length === 0"
                />
              </n-form-item>
            </n-form>

            <div class="org-tree-container">
              <n-spin :show="orgLoading">
                <PremiumTree
                  v-if="orgTreeData.length > 0"
                  :data="orgTreeData"
                  checkable
                  :cascade="false"
                  :selected-keys="mainOrgId ? [mainOrgId] : []"
                  :checked-keys="checkedOrgKeys"
                  :expanded-keys="orgTreeExpandedKeys"
                  key-field="id"
                  label-field="orgName"
                  children-field="children"
                  :get-node-icon="getLeftOrgNodeIcon"
                  :get-node-meta="getUserOrgNodeMeta"
                  :get-node-tone="getLeftOrgNodeTone"
                  show-meta
                  @update:expanded-keys="handleOrgExpandedKeysChange"
                  @update:checked-keys="handleOrgCheckedKeysChange"
                />
                <n-empty v-else description="暂无组织数据" />
              </n-spin>
            </div>
          </div>
        </n-tab-pane>

        <!-- 岗位绑定 -->
        <n-tab-pane name="post" tab="岗位绑定">
          <div class="post-modal-content">
            <n-spin :show="postLoading">
              <n-form label-placement="left" label-width="90">
                <n-form-item label="主岗位">
                  <n-select
                    v-model:value="mainPostId"
                    :options="selectedPostOptions"
                    placeholder="请选择主岗位"
                    clearable
                  />
                </n-form-item>
                <n-form-item label="绑定岗位">
                  <n-checkbox-group v-model:value="checkedPostKeys">
                    <n-space vertical>
                      <n-checkbox
                        v-for="post in postList"
                        :key="post.id"
                        :value="post.id"
                      >
                        {{ post.postName }}
                        <NTag v-if="post.postCode" size="small" type="info" :bordered="false" style="margin-left: 6px">
                          {{ post.postCode }}
                        </NTag>
                      </n-checkbox>
                    </n-space>
                  </n-checkbox-group>
                  <n-empty v-if="!postLoading && postList.length === 0" description="暂无岗位数据" size="small" />
                </n-form-item>
              </n-form>
            </n-spin>
          </div>
        </n-tab-pane>

        <!-- 租户绑定（仅管理员可见） -->
        <n-tab-pane v-if="userStore.isAdmin" name="tenant" tab="租户绑定">
          <div class="tenant-modal-content">
            <n-spin :show="tenantLoading">
              <n-form label-placement="left" label-width="90">
                <n-form-item label="绑定租户">
                  <n-checkbox-group v-model:value="checkedTenantKeys">
                    <n-space vertical>
                      <n-checkbox
                        v-for="tenant in tenantOptions"
                        :key="tenant.id"
                        :value="tenant.id"
                      >
                        {{ tenant.tenantName }}
                      </n-checkbox>
                    </n-space>
                  </n-checkbox-group>
                </n-form-item>
                <n-form-item label="默认租户">
                  <n-select
                    v-model:value="defaultTenantId"
                    :options="selectedTenantOptions"
                    placeholder="请选择默认租户"
                  />
                </n-form-item>
              </n-form>
            </n-spin>
          </div>
        </n-tab-pane>
      </n-tabs>

      <template #footer>
        <n-space justify="end">
          <n-button @click="relationModalVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="relationSubmitLoading"
            @click="handleRelationSubmit"
          >
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 批量授权弹窗 -->
    <n-modal
      v-model:show="batchAuthModalVisible"
      title="批量用户授权"
      preset="card"
      style="width: 860px"
      :mask-closable="false"
    >
      <div class="auth-modal-content">
        <n-alert type="info" :bordered="false" class="batch-action-alert">
          已选择 {{ selectedUserIds.length }} 个用户，提交后会给这些用户追加所选角色。
        </n-alert>
        <n-form label-placement="left" label-width="90" class="batch-action-form">
          <n-form-item label="授权租户">
            <n-select
              v-model:value="batchAuthTenantId"
              :options="tenantSelectOptions"
              placeholder="请选择授权租户"
              filterable
            />
          </n-form-item>
          <n-form-item label="授权组织">
            <n-tree-select
              v-model:value="batchAuthOrgId"
              :options="batchAuthOrgTreeOptions"
              placeholder="请选择授权组织"
              filterable
              clearable
              key-field="value"
              label-field="label"
              children-field="children"
            />
          </n-form-item>
        </n-form>

        <div class="auth-toolbar">
          <n-input
            v-model:value="roleSearchKeyword"
            class="auth-role-search"
            clearable
            size="small"
            placeholder="按角色名称搜索"
            @clear="handleRoleSearch"
            @keyup.enter="handleRoleSearch"
          >
            <template #prefix>
              <i class="i-material-symbols:search-rounded" />
            </template>
          </n-input>
          <n-space size="small" class="auth-toolbar-actions">
            <n-button size="small" @click="handleRoleSearch">
              <template #icon>
                <i class="i-material-symbols:search-rounded" />
              </template>
              查询
            </n-button>
            <n-button size="small" @click="handleCheckAll">
              <template #icon>
                <i class="i-material-symbols:check-box-outline" />
              </template>
              全选本页
            </n-button>
            <n-button size="small" @click="handleUncheckAll">
              <template #icon>
                <i class="i-material-symbols:check-box-outline-blank" />
              </template>
              清空选择
            </n-button>
          </n-space>
        </div>

        <div class="auth-tree-container">
          <n-spin :show="authLoading">
            <n-data-table
              :columns="authRoleColumns"
              :data="orderedRoleTableData"
              :checked-row-keys="checkedRoleKeys"
              :pagination="rolePaginationConfig"
              :row-key="row => row.id"
              remote
              striped
              size="small"
              @update:checked-row-keys="handleCheckedKeysChange"
              @update:page="handleRolePageChange"
              @update:page-size="handleRolePageSizeChange"
            />
          </n-spin>
        </div>
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button @click="batchAuthModalVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="batchAuthSubmitLoading"
            @click="handleSubmitBatchAuth"
          >
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 批量加入租户弹窗 -->
    <n-modal
      v-model:show="batchTenantModalVisible"
      title="批量加入租户"
      preset="card"
      style="width: 520px"
      :mask-closable="false"
    >
      <n-alert type="info" :bordered="false" class="batch-action-alert">
        已选择 {{ selectedUserIds.length }} 个用户，提交后会将这些用户加入目标租户。
      </n-alert>
      <n-form label-placement="left" label-width="90" class="batch-action-form">
        <n-form-item label="目标租户">
          <n-select
            v-model:value="batchTenantForm.tenantId"
            :options="tenantSelectOptions"
            placeholder="请选择目标租户"
            filterable
          />
        </n-form-item>
        <n-form-item label="成员类型">
          <n-select
            v-model:value="batchTenantForm.memberType"
            :options="tenantMemberTypeOptions"
            placeholder="请选择成员类型"
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="batchTenantModalVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="batchTenantSubmitLoading"
            @click="handleSubmitBatchTenant"
          >
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
import MasterDetailWorkspace from '@/components/common/MasterDetailWorkspace.vue'
import PremiumTree from '@/components/common/PremiumTree.vue'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { request } from '@/utils'
import { orderRolesWithCurrentFirst } from './user-role-order'

defineOptions({ name: 'SystemUser' })

const USER_TYPE_DICT = 'sys_user_type'
const USER_STATUS_DICT = 'sys_user_status'
const USER_SEX_DICT = 'sys_user_sex'
const ROLE_DATA_SCOPE_DICT = 'sys_role_data_scope'
const ROLE_TYPE_DICT = 'sys_role_type'
const NORMAL_DISABLE_DICT = 'sys_normal_disable'

const crudRef = ref(null)
const userStore = useUserStore()
const selectedUserIds = ref([])

// 左侧组织树相关
const leftOrgTreeData = ref([])
const leftOrgTreeLoading = ref(false)
const leftOrgExpandAll = ref(false)
const leftOrgExpandedKeys = ref([])
const leftOrgPanelCollapsed = ref(false)
const selectedOrgKeys = ref([])
const selectedOrgNode = ref(null)
const isShowAllUsers = ref(true)

// 关联管理弹窗（整合授权/组织/岗位/租户）
const relationModalVisible = ref(false)
const relationActiveTab = ref('auth')
const relationSubmitLoading = ref(false)

// 授权相关
const authLoading = ref(false)
const authSubmitLoading = ref(false)
const batchAuthModalVisible = ref(false)
const batchAuthSubmitLoading = ref(false)
const batchAuthTenantId = ref(null)
const batchAuthOrgId = ref(null)
const batchAuthOrgTreeData = ref([])
const currentUser = ref({})
const roleTableData = ref([])
const roleSearchKeyword = ref('')
const editingUserTenantId = ref(null)
const checkedRoleKeys = ref([])
const authOrgId = ref(null)
const currentUserOrgBindings = ref([])
const rolePagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
})
const orderedRoleTableData = computed(() => orderRolesWithCurrentFirst(roleTableData.value, checkedRoleKeys.value))

// 重置密码相关
const resetPwdModalVisible = ref(false)
const resetPwdLoading = ref(false)
const resetPwdFormRef = ref(null)
const resetPwdForm = ref({
  id: null,
  password: '',
})
const resetPwdRules = {
  password: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码不能少于6位', trigger: 'blur' }],
}

// 组织弹窗相关（用户组织绑定）
const orgLoading = ref(false)
const orgSubmitLoading = ref(false)
const orgTreeData = ref([])
const mainOrgId = ref(null)
const checkedOrgKeys = ref([])
const orgTreeExpandAll = ref(true)
const orgTreeExpandedKeys = ref([])

// 岗位绑定相关
const postLoading = ref(false)
const postSubmitLoading = ref(false)
const postList = ref([])
const checkedPostKeys = ref([])
const mainPostId = ref(null)

// 租户绑定相关
const tenantLoading = ref(false)
const tenantSubmitLoading = ref(false)
const tenantOptions = ref([])
const checkedTenantKeys = ref([])
const defaultTenantId = ref(null)
const batchTenantModalVisible = ref(false)
const batchTenantSubmitLoading = ref(false)
const batchTenantForm = ref({
  tenantId: null,
  memberType: 2,
})

// 行政区划选项（搜索用：虚拟节点可选；编辑用：虚拟节点不可选）
const searchRegionOptions = ref([])
const editRegionOptions = ref([])

const { dict } = useDict(USER_TYPE_DICT, USER_STATUS_DICT, USER_SEX_DICT, ROLE_DATA_SCOPE_DICT, ROLE_TYPE_DICT, NORMAL_DISABLE_DICT)

const userTypeOptions = computed(() => toNumberOptions(dict.value[USER_TYPE_DICT]))
const userStatusOptions = computed(() => toNumberOptions(dict.value[USER_STATUS_DICT]))
const genderOptions = computed(() => toNumberOptions(dict.value[USER_SEX_DICT]))
const roleTypeOptions = computed(() => toNumberOptions(dict.value[ROLE_TYPE_DICT]))
const roleDataScopeOptions = computed(() => toNumberOptions(dict.value[ROLE_DATA_SCOPE_DICT]))
const normalDisableOptions = computed(() => toNumberOptions(dict.value[NORMAL_DISABLE_DICT]))
const tenantSelectOptions = computed(() => tenantOptions.value.map(item => ({
  label: item.tenantName,
  value: item.id,
})))
const authOrgOptions = computed(() => currentUserOrgBindings.value
  .map(item => ({
    label: formatUserOrgBindingLabel(item),
    value: normalizeSingleNumber(item.orgId),
  }))
  .filter(item => item.value !== null))
const batchAuthOrgTreeOptions = computed(() => convertOrgToTreeSelect(batchAuthOrgTreeData.value))
const tenantMemberTypeOptions = computed(() => userTypeOptions.value.filter(item => Number(item.value) !== 0))
const userBatchActionOptions = computed(() => [
  {
    label: '批量授权',
    key: 'auth',
    icon: () => h('i', { class: 'i-material-symbols:verified-user-outline-rounded' }),
  },
  ...(userStore.isAdmin
    ? [{
        label: '加入租户',
        key: 'tenant',
        icon: () => h('i', { class: 'i-material-symbols:group-add' }),
      }]
    : []),
])
const rolePaginationConfig = computed(() => ({
  page: rolePagination.value.page,
  pageSize: rolePagination.value.pageSize,
  itemCount: rolePagination.value.itemCount,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100],
}))
const authRoleColumns = computed(() => [
  {
    type: 'selection',
    multiple: true,
    width: 48,
  },
  {
    title: '角色名称',
    key: 'roleName',
    minWidth: 150,
  },
  {
    title: '权限字符',
    key: 'roleKey',
    minWidth: 140,
  },
  {
    title: '角色类型',
    key: 'roleType',
    width: 110,
    render: row => renderDictTag(
      roleTypeOptions.value,
      resolveRoleDictValue(row, 'roleType'),
      'role-type-tag',
    ),
  },
  {
    title: '数据范围',
    key: 'dataScope',
    width: 140,
    render: row => renderDictTag(
      roleDataScopeOptions.value,
      resolveRoleDictValue(row, 'dataScope'),
      'role-data-scope-tag',
    ),
  },
  {
    title: '状态',
    key: 'roleStatus',
    width: 90,
    render: row => renderDictTag(
      normalDisableOptions.value,
      resolveRoleDictValue(row, 'roleStatus'),
      'role-status-tag',
    ),
  },
])
const selectedTenantOptions = computed(() => tenantOptions.value
  .filter(item => checkedTenantKeys.value.includes(item.id))
  .map(item => ({
    label: item.tenantName,
    value: item.id,
  })))
const selectedOrgOptions = computed(() => flattenOrgNodes(orgTreeData.value)
  .filter(item => checkedOrgKeys.value.includes(normalizeSingleNumber(item.id)))
  .map(item => ({
    label: item.orgName,
    value: normalizeSingleNumber(item.id),
  })))

watch(checkedOrgKeys, (keys) => {
  if (mainOrgId.value && !keys.includes(mainOrgId.value)) {
    mainOrgId.value = keys[0] || null
  }
})

watch(checkedTenantKeys, (keys) => {
  if (defaultTenantId.value && !keys.includes(defaultTenantId.value)) {
    defaultTenantId.value = keys[0] || null
  }
})

watch(authOrgId, (orgId, previousOrgId) => {
  if (!relationModalVisible.value || isSameKey(orgId, previousOrgId))
    return
  checkedRoleKeys.value = []
  rolePagination.value.page = 1
  loadRoleList(resolveOperationTenantId(), orgId)
  loadUserRoles(currentUser.value.id, orgId)
})

watch(batchAuthTenantId, async (tenantId, previousTenantId) => {
  if (!batchAuthModalVisible.value || isSameKey(tenantId, previousTenantId))
    return
  checkedRoleKeys.value = []
  batchAuthOrgId.value = null
  rolePagination.value.page = 1
  await loadBatchAuthOrgTree(tenantId)
  batchAuthOrgId.value = resolvePreferredOrgIdFromTree(batchAuthOrgTreeData.value)
  if (batchAuthOrgId.value)
    loadRoleList(tenantId, batchAuthOrgId.value)
})

watch(batchAuthOrgId, (orgId, previousOrgId) => {
  if (!batchAuthModalVisible.value || isSameKey(orgId, previousOrgId))
    return
  checkedRoleKeys.value = []
  rolePagination.value.page = 1
  if (orgId)
    loadRoleList(batchAuthTenantId.value, orgId)
})

// 岗位选项（仅已勾选的岗位可选为主岗）
const selectedPostOptions = computed(() => postList.value
  .filter(item => checkedPostKeys.value.includes(item.id))
  .map(item => ({
    label: item.postName,
    value: item.id,
  })))

watch(checkedPostKeys, (keys) => {
  if (mainPostId.value && !keys.includes(mainPostId.value)) {
    mainPostId.value = null
  }
})

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
    field: 'username',
    label: '用户名',
    type: 'input',
    props: {
      placeholder: '请输入用户名',
    },
  },
  {
    field: 'realName',
    label: '真实姓名',
    type: 'input',
    props: {
      placeholder: '请输入真实姓名',
    },
  },
  {
    field: 'regionCode',
    label: '行政区划',
    type: 'treeSelect',
    props: {
      placeholder: '请选择行政区划',
      clearable: true,
      filterable: true,
    },
    options: () => searchRegionOptions.value,
  },
  {
    field: 'phone',
    label: '手机号',
    type: 'input',
    props: {
      placeholder: '请输入手机号',
    },
  },
  {
    field: 'userStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: userStatusOptions.value,
    },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'username',
    label: '用户',
    minWidth: 190,
    render: row => h(SystemTableCell, {
      title: resolveUserDisplayName(row),
      subtitle: resolveUserAccountLabel(row),
      interactive: true,
      avatar: true,
      tooltip: `查看用户详情：${row.username || '-'}`,
      onActivate: () => crudRef.value?.showDetail(row),
    }),
  },
  {
    prop: 'phone',
    label: '手机号',
    minWidth: 130,
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
    prop: 'postName',
    label: '岗位',
    minWidth: 130,
    render: row => h(SystemTableCell, {
      values: splitTableCellValues(row.postName || row.postNames),
    }),
  },
  ...(userStore.isAdmin
    ? [{
        prop: 'tenantName',
        label: '所属租户',
        width: 160,
        ellipsis: false,
        render: row => renderTenantNames(row),
      }]
    : []),
  {
    prop: 'userType',
    label: '用户类型',
    width: 100,
    render: row => renderDictTag(userTypeOptions.value, normalizeSingleNumber(row.userType)),
  },
  {
    prop: 'userStatus',
    label: '状态',
    width: 90,
    render: row => renderDictTag(userStatusOptions.value, normalizeSingleNumber(row.userStatus), 'user-status-tag'),
  },
  {
    prop: 'action',
    label: '操作',
    width: 160,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', onClick: handleEdit },
      { label: '禁用', key: 'disable', type: 'warning', onClick: row => handleUpdateStatus(row, 0), visible: row => row.id !== 1 && !isCurrentLoginUser(row.id) && row.userStatus === 1 },
      { label: '启用', key: 'enable', type: 'success', onClick: handleUntieDisable, visible: row => row.id !== 1 && !isCurrentLoginUser(row.id) && row.userStatus !== 1 },
      { label: '关联管理', key: 'relation', onClick: handleRelation, visible: row => !isCurrentLoginUser(row.id) },
      {
        label: '重置密码',
        key: 'resetPwd',
        type: 'warning',
        visible: row => !isCurrentLoginUser(row.id),
        onClick: (row) => {
          resetPwdForm.value = { id: row.id, password: '' }
          resetPwdModalVisible.value = true
        },
      },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete, visible: row => row.id !== 1 && !isCurrentLoginUser(row.id) },
    ],
  },
])

// 编辑表单配置
const editSchema = computed(() => [
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
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
    field: 'realName',
    label: '真实姓名',
    type: 'input',
    rules: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
    props: {
      placeholder: '请输入真实姓名',
    },
  },
  {
    field: 'password',
    label: '密码',
    type: 'input',
    rules: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    props: {
      type: 'password',
      placeholder: '请输入密码',
    },
    vIf: formData => !formData.id,
  },
  {
    field: 'tenantIds',
    label: '所属租户',
    type: 'select',
    defaultValue: userStore.userInfo?.tenantId ? [userStore.userInfo.tenantId] : [],
    multiple: true,
    rules: [{ required: true, type: 'array', message: '请选择所属租户', trigger: 'change' }],
    onChange: ({ value, formData, context }) => {
      const tenantIds = normalizeNumberList(value)
      const tenantId = normalizeSingleNumber(formData?.tenantId)
      if (tenantIds.length === 0) {
        context?.patchFormData?.({ tenantId: null })
        return
      }
      if (tenantId === null || !tenantIds.includes(tenantId)) {
        context?.patchFormData?.({ tenantId: tenantIds[0] })
      }
    },
    props: {
      placeholder: '请选择所属租户',
      options: tenantSelectOptions.value,
      multiple: true,
      disabled: !userStore.isAdmin,
    },
  },
  {
    field: 'tenantId',
    label: '默认租户',
    type: 'select',
    defaultValue: userStore.userInfo?.tenantId,
    rules: [{ required: true, type: 'number', message: '请选择默认租户', trigger: 'change' }],
    options: ({ formData }) => resolveDefaultTenantOptions(formData),
    props: {
      placeholder: '请选择默认租户',
      disabled: !userStore.isAdmin,
    },
  },
  {
    field: 'userType',
    label: '用户类型',
    type: 'select',
    defaultValue: 2,
    rules: [{ required: true, type: 'number', message: '请选择用户类型', trigger: 'change' }],
    props: {
      placeholder: '请选择用户类型',
      options: userTypeOptions.value,
      disabled: !userStore.isAdmin,
    },
  },
  {
    type: 'divider',
    label: '联系信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'phone',
    label: '手机号',
    type: 'input',
    rules: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
    ],
    props: {
      placeholder: '请输入手机号',
    },
  },
  {
    field: 'email',
    label: '邮箱',
    type: 'input',
    rules: [
      { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
    ],
    props: {
      placeholder: '请输入邮箱',
    },
  },
  {
    field: 'idCard',
    label: '身份证号',
    type: 'input',
    rules: [
      { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}([\dX])$)/i, message: '请输入正确的身份证号', trigger: 'blur' },
    ],
    props: {
      placeholder: '请输入身份证号',
    },
  },
  {
    field: 'gender',
    label: '性别',
    type: 'radio',
    defaultValue: 0,
    props: {
      options: genderOptions.value,
    },
  },
  {
    type: 'divider',
    label: '行政区划',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'regionCode',
    label: '行政区划',
    type: 'treeSelect',
    props: {
      placeholder: '请选择行政区划',
      clearable: true,
      filterable: true,
    },
    options: () => editRegionOptions.value,
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
    field: 'userStatus',
    label: '用户状态',
    type: 'radio',
    defaultValue: 1,
    vIf: formData => !isCurrentLoginUser(formData.id),
    props: {
      options: userStatusOptions.value,
    },
  },
  {
    field: 'postIds',
    label: '岗位',
    type: 'select',
    span: 2,
    vIf: formData => !isCurrentLoginUser(formData.id),
    optionSource: {
      api: 'get@/system/post/list',
      // eslint-disable-next-line no-template-curly-in-string
      params: { tenantId: '${tenantId}' },
      valueField: 'id',
      labelField: 'postName',
    },
    props: {
      placeholder: '请选择岗位',
      multiple: true,
      clearable: true,
      filterable: true,
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
    value: normalizeSingleNumber(item.value, item.value),
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

function flattenOrgNodes(list = []) {
  return (list || []).flatMap((item) => {
    const current = [item]
    const children = flattenOrgNodes(item.children || [])
    return [...current, ...children]
  })
}

function convertOrgToTreeSelect(list = []) {
  return (list || []).map(item => ({
    label: item.orgName || item.label || String(item.id),
    value: normalizeSingleNumber(item.id),
    key: normalizeSingleNumber(item.id),
    children: item.children?.length ? convertOrgToTreeSelect(item.children) : undefined,
  }))
}

function resolvePreferredOrgIdFromTree(list = []) {
  const flat = flattenOrgNodes(list)
  const activeOrgId = normalizeSingleNumber(userStore.activeOrgId || userStore.userInfo?.activeOrgId)
  if (activeOrgId !== null && flat.some(item => isSameKey(item.id, activeOrgId)))
    return activeOrgId
  return normalizeSingleNumber(flat[0]?.id)
}

function formatUserOrgBindingLabel(item = {}) {
  const roleNames = Array.isArray(item.roleNames) ? item.roleNames.filter(Boolean) : []
  const suffix = roleNames.length > 0
    ? ` · ${roleNames.slice(0, 2).join('、')}${roleNames.length > 2 ? '等' : ''}`
    : ''
  return `${item.orgName || item.orgId}${Number(item.isMain) === 1 ? ' · 主组织' : ''}${suffix}`
}

function resolveTenantIds(data = {}, fallbackTenantId = null) {
  const tenantIds = normalizeNumberList(data.tenantIds)
  if (tenantIds.length > 0)
    return tenantIds

  const tenantId = normalizeSingleNumber(data.tenantId)
  if (tenantId !== null)
    return [tenantId]

  const fallback = normalizeSingleNumber(fallbackTenantId)
  return fallback !== null ? [fallback] : []
}

function resolveSingleTenantId(data = {}, fallbackTenantId = null) {
  const tenantIds = resolveTenantIds(data, fallbackTenantId)
  if (tenantIds.length === 0)
    return normalizeSingleNumber(fallbackTenantId)

  const tenantId = normalizeSingleNumber(data.tenantId)
  if (tenantId !== null && tenantIds.includes(tenantId))
    return tenantId

  const preferredTenantId = normalizeSingleNumber(fallbackTenantId)
  if (preferredTenantId !== null && tenantIds.includes(preferredTenantId))
    return preferredTenantId

  const currentTenantId = normalizeSingleNumber(userStore.userInfo?.tenantId)
  if (currentTenantId !== null && tenantIds.includes(currentTenantId))
    return currentTenantId

  return tenantIds[0]
}

function resolveDefaultTenantOptions(formData = {}) {
  const tenantIds = resolveTenantIds(formData, userStore.userInfo?.tenantId)
  if (tenantIds.length === 0)
    return tenantSelectOptions.value
  return tenantSelectOptions.value.filter(option => tenantIds.includes(normalizeSingleNumber(option.value)))
}

function normalizeUserFormData(data = {}, fallbackTenantId = null) {
  const next = { ...(data || {}) }
  next.tenantIds = resolveTenantIds(next, fallbackTenantId)
  next.tenantId = resolveSingleTenantId(next, fallbackTenantId)
  next.userType = normalizeSingleNumber(next.userType, 2)
  next.gender = normalizeSingleNumber(next.gender, 0)
  next.userStatus = normalizeSingleNumber(next.userStatus, 1)
  if (next.roleIds !== null && next.roleIds !== undefined)
    next.roleIds = normalizeNumberList(next.roleIds)
  if (next.postIds !== null && next.postIds !== undefined)
    next.postIds = normalizeNumberList(next.postIds)
  return next
}

// 组件挂载时加载左侧组织树
onMounted(() => {
  loadLeftOrgTree()
  loadTenantOptions()
  loadPostList()
})

const orgTreeSummaryText = computed(() => {
  const total = countTreeNodes(leftOrgTreeData.value)
  if (!total)
    return '未加载组织'
  return `${total} 个组织节点`
})

const mainOrgName = computed(() => {
  if (!mainOrgId.value)
    return ''
  const node = findOrgNode(orgTreeData.value, mainOrgId.value)
  return node?.orgName || ''
})

// 获取所有节点的 key
function getAllKeys(list, keys = []) {
  list.forEach((item) => {
    keys.push(item.id)
    if (item.children && item.children.length > 0) {
      getAllKeys(item.children, keys)
    }
  })
  return keys
}

function countTreeNodes(list = []) {
  return list.reduce((total, item) => total + 1 + countTreeNodes(item.children || []), 0)
}

function isSameKey(left, right) {
  return String(left) === String(right)
}

function isCurrentLoginUser(targetUserId) {
  return targetUserId != null && isSameKey(targetUserId, userStore.userId)
}

function handleUserSelectionChange(payload = {}) {
  const keys = Array.isArray(payload) ? payload : payload.keys
  selectedUserIds.value = normalizeNumberList(keys || [])
}

function getSelectedBatchUserIds() {
  const ids = normalizeNumberList(selectedUserIds.value.length > 0 ? selectedUserIds.value : crudRef.value?.getSelectedKeys?.())
  if (ids.length === 0) {
    window.$message.warning('请先选择用户')
    return []
  }
  if (ids.some(id => isCurrentLoginUser(id))) {
    window.$message.warning('批量操作不能包含当前登录用户')
    return []
  }
  return ids
}

function handleUserBatchActionSelect(key) {
  if (key === 'auth') {
    handleOpenBatchAuth()
    return
  }
  if (key === 'tenant') {
    handleOpenBatchTenant()
  }
}

function resolveDefaultBatchTenantId() {
  const searchTenantId = normalizeSingleNumber(crudRef.value?.getSearchParams?.()?.tenantId)
  if (userStore.isAdmin && searchTenantId !== null)
    return searchTenantId

  const currentTenantId = normalizeSingleNumber(userStore.userInfo?.tenantId)
  if (currentTenantId !== null)
    return currentTenantId

  return tenantSelectOptions.value[0]?.value || null
}

function clearBatchSelection() {
  selectedUserIds.value = []
  crudRef.value?.clearSelection?.()
}

function formatTenantNameList(row = {}) {
  const tenantNames = String(row.tenantName || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
  if (tenantNames.length > 0)
    return Array.from(new Set(tenantNames))
  if (row.tenantId)
    return [String(row.tenantId)]
  return []
}

function renderTenantNames(row = {}) {
  return h(SystemTableCell, { values: formatTenantNameList(row) })
}

function splitTableCellValues(value) {
  const rawValues = Array.isArray(value) ? value : [value]
  return rawValues
    .flatMap(item => String(item || '').split(/[、,，]/))
    .map(item => item.trim())
    .filter(Boolean)
}

function resolveUserDisplayName(row = {}) {
  return row.realName || row.nickname || row.username || `用户${row.id || ''}`
}

function resolveUserAccountLabel(row = {}) {
  const username = String(row.username || '').trim()
  return username && username !== resolveUserDisplayName(row) ? `@${username}` : ''
}

function resolveOptionLabel(options = [], value) {
  return options.find(option => isSameKey(option?.value, value))?.label || ''
}

function renderDictTag(options = [], value, className = '') {
  return h('span', { class: ['user-table-tag', className] }, [
    h(DictTag, { options, value, size: 'small', forceTag: true }),
  ])
}

function resolveRoleDictValue(row = {}, field) {
  const aliasMap = {
    roleType: ['roleType', 'role_type', 'type'],
    dataScope: ['dataScope', 'data_scope'],
    roleStatus: ['roleStatus', 'role_status', 'status'],
  }
  const keys = aliasMap[field] || [field]
  const value = keys.map(key => row[key]).find(item => item !== null && item !== undefined && item !== '')
  return normalizeSingleNumber(value, value ?? '')
}

function getLeftOrgNodeIcon(node = {}) {
  return node.children?.length
    ? 'i-material-symbols:account-tree-rounded'
    : 'i-material-symbols:domain-rounded'
}

function getLeftOrgNodeTone(node = {}) {
  if (!node.parentId || Number(node.parentId) === 0)
    return 'folder'
  return node.children?.length ? 'folder' : 'menu'
}

function getUserOrgNodeMeta(node = {}) {
  if (isSameKey(node.id, mainOrgId.value))
    return { value: '主组织' }
  if (checkedOrgKeys.value.includes(normalizeSingleNumber(node.id)))
    return { value: '已选' }
  return null
}

function buildTenantParams(tenantId) {
  const resolvedTenantId = userStore.isAdmin ? tenantId : userStore.userInfo?.tenantId
  return resolvedTenantId ? { tenantId: resolvedTenantId } : {}
}

function resolveOperationTenantId(row = currentUser.value) {
  return row?.tenantId
    || (userStore.isAdmin ? crudRef.value?.getSearchParams?.()?.tenantId : null)
    || userStore.userInfo?.tenantId
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

// 加载左侧组织树
async function loadLeftOrgTree(tenantId) {
  try {
    leftOrgTreeLoading.value = true
    leftOrgTreeData.value = await loadCompleteOrgTree(tenantId)
    if (leftOrgExpandAll.value)
      leftOrgExpandedKeys.value = getAllKeys(leftOrgTreeData.value)

    // 同时加载行政区划选项
    await loadRegionOptions()
  }
  catch (error) {
    console.error('加载组织树失败:', error)
    window.$message.error('加载组织树失败')
  }
  finally {
    leftOrgTreeLoading.value = false
  }
}

// 加载行政区划选项（按当前用户行政区划/数据权限加载完整区划树，含虚拟组织）
async function loadRegionOptions() {
  try {
    const res = await request.get('/system/region/treeAll', { params: { dataRight: true } })
    if (res.code === 200) {
      const data = res.data || []
      // 搜索场景：虚拟节点可选（代表"该区域下所有"）
      searchRegionOptions.value = convertRegionToTreeSelect(data, false)
      // 编辑场景：虚拟节点不可选（避免存入ALL后缀编码）
      editRegionOptions.value = convertRegionToTreeSelect(data, true)
    }
  }
  catch (error) {
    console.error('加载行政区划选项失败:', error)
  }
}

async function loadTenantOptions() {
  try {
    const res = await request.get('/system/tenant/assignable/options')
    if (res.code === 200) {
      tenantOptions.value = (res.data || [])
        .map(item => ({
          ...item,
          id: normalizeSingleNumber(item.id),
        }))
        .filter(item => item.id !== null)
    }
  }
  catch (error) {
    console.error('加载租户选项失败:', error)
  }
}

// 将后端返回的树形数据转换为TreeSelect组件需要的格式
// virtualDisabled: true=编辑表单（虚拟节点不可选），false=搜索筛选（虚拟节点可选）
function convertRegionToTreeSelect(list, virtualDisabled = true) {
  return list.map((item) => {
    const node = {
      label: item.name,
      value: item.code,
      key: item.code,
    }
    if (virtualDisabled && item.code && item.code.endsWith('ALL')) {
      node.disabled = true
    }
    if (item.children && item.children.length > 0) {
      node.children = convertRegionToTreeSelect(item.children, virtualDisabled)
    }
    return node
  })
}

// 左侧组织树节点选择
function handleOrgNodeSelect(keys) {
  selectedOrgKeys.value = keys

  if (keys.length > 0) {
    isShowAllUsers.value = false
    const orgId = keys[0]
    selectedOrgNode.value = findOrgNode(leftOrgTreeData.value, orgId)
    crudRef.value?.refresh()
  }
  else {
    selectedOrgNode.value = null
    crudRef.value?.refresh()
  }
}

// 查找组织节点
function findOrgNode(treeData, orgId) {
  for (const node of treeData) {
    if (node.id === orgId) {
      return node
    }
    if (node.children && node.children.length > 0) {
      const found = findOrgNode(node.children, orgId)
      if (found)
        return found
    }
  }
  return null
}

// 左侧组织树展开节点变化
function handleLeftOrgExpandedKeysChange(keys) {
  leftOrgExpandedKeys.value = keys
}

// 左侧组织树展开/折叠所有
function toggleOrgExpandAll() {
  leftOrgExpandAll.value = !leftOrgExpandAll.value

  if (leftOrgExpandAll.value) {
    leftOrgExpandedKeys.value = getAllKeys(leftOrgTreeData.value)
  }
  else {
    leftOrgExpandedKeys.value = []
  }
}

function toggleUserOrgExpandAll() {
  orgTreeExpandAll.value = !orgTreeExpandAll.value

  if (orgTreeExpandAll.value) {
    orgTreeExpandedKeys.value = getAllKeys(orgTreeData.value)
  }
  else {
    orgTreeExpandedKeys.value = []
  }
}

function toggleLeftOrgPanel() {
  leftOrgPanelCollapsed.value = !leftOrgPanelCollapsed.value
}

// 清除组织筛选
function handleClearOrgFilter() {
  selectedOrgKeys.value = []
  selectedOrgNode.value = null
  isShowAllUsers.value = true
  crudRef.value?.refresh()
}

function handleSelectAllUsers() {
  isShowAllUsers.value = true
  selectedOrgKeys.value = []
  selectedOrgNode.value = null
  crudRef.value?.refresh()
}

// 加载列表数据前的钩子（用于添加组织ID参数）
function beforeLoadList(params) {
  Object.assign(params, buildTenantParams(params.tenantId))
  if (selectedOrgNode.value && !isShowAllUsers.value) {
    params.orgId = selectedOrgNode.value.id
  }
  return params
}

async function beforeSearch(params) {
  selectedOrgKeys.value = []
  selectedOrgNode.value = null
  isShowAllUsers.value = true
  await loadLeftOrgTree(params?.tenantId)
  return params
}

function beforeRenderUserForm(row) {
  const normalized = normalizeUserFormData(row || {}, userStore.userInfo?.tenantId)
  editingUserTenantId.value = normalized.tenantId || null
  return normalized
}

async function beforeRenderUserDetail(data) {
  if (!data?.id) {
    return normalizeUserFormData(data || {}, userStore.userInfo?.tenantId)
  }
  const normalizedData = normalizeUserFormData(data, editingUserTenantId.value || userStore.userInfo?.tenantId)
  const tenantId = normalizedData.tenantId
  const next = {
    ...normalizedData,
    tenantId,
    userTypeLabel: resolveOptionLabel(userTypeOptions.value, normalizeSingleNumber(data.userType)),
    genderLabel: resolveOptionLabel(genderOptions.value, normalizeSingleNumber(data.gender)),
    userStatusLabel: resolveOptionLabel(userStatusOptions.value, normalizeSingleNumber(data.userStatus)),
  }
  if (!userStore.isAdmin && isCurrentLoginUser(data.id)) {
    return next
  }
  if (!tenantId) {
    return next
  }
  try {
    const params = buildTenantParams(tenantId)
    const [postRes] = await Promise.all([
      request.get(`/system/user/${data.id}/posts`, { params }),
    ])
    if (postRes.code === 200)
      next.postIds = normalizeNumberList(postRes.data || [])
  }
  catch (error) {
    console.error('加载用户租户关联信息失败:', error)
  }
  return next
}

// 确认重置密码
async function handleConfirmResetPwd() {
  resetPwdFormRef.value?.validate(async (errors) => {
    if (!errors) {
      try {
        resetPwdLoading.value = true
        const res = await request.post('/system/user/resetPwd', null, {
          params: {
            id: resetPwdForm.value.id,
            password: resetPwdForm.value.password,
          },
        })
        if (res.code === 200) {
          window.$message.success('重置成功')
          resetPwdModalVisible.value = false
        }
      }
      catch {
        window.$message.error('重置失败')
      }
      finally {
        resetPwdLoading.value = false
      }
    }
  })
}

// 更新用户状态
async function handleUpdateStatus(row, status) {
  const actionText = status === 1 ? '启用' : '禁用'
  window.$dialog.warning({
    title: '确认操作',
    content: `确定要${actionText}用户"${row.username}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/user/updateStatus', null, {
          params: { id: row.id, status },
        })
        if (res.code === 200) {
          window.$message.success(`${actionText}成功`)
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error(`${actionText}失败`)
      }
    },
  })
}

// 解封用户
async function handleUntieDisable(row) {
  window.$dialog.warning({
    title: '确认操作',
    content: `确定要启用并解封用户"${row.username}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/user/doUntieDisable', null, {
          params: { id: row.id },
        })
        if (res.code === 200) {
          window.$message.success(`用户已启用`)
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error(`启用失败`)
      }
    },
  })
}

// 表单提交前处理
function beforeSubmit(formData) {
  Object.assign(formData, normalizeUserFormData(formData, userStore.userInfo?.tenantId))
  delete formData.roleIds
  if (!userStore.isAdmin) {
    formData.tenantId = userStore.userInfo?.tenantId
    formData.tenantIds = [userStore.userInfo?.tenantId].filter(Boolean)
    formData.userType = 2
  }
  else if (!formData.tenantIds || formData.tenantIds.length === 0) {
    formData.tenantIds = formData.tenantId ? [formData.tenantId] : [userStore.userInfo?.tenantId].filter(Boolean)
  }
  if (formData.tenantIds.length > 0 && !formData.tenantIds.includes(formData.tenantId)) {
    formData.tenantId = formData.tenantIds[0]
  }
  else if (!formData.tenantId) {
    formData.tenantId = formData.tenantIds[0] || userStore.userInfo?.tenantId
  }
  return formData
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除用户"${row.username}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/user/remove', null, { params: { id: row.id } })
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

// 授权
async function handleOpenBatchAuth() {
  const userIds = getSelectedBatchUserIds()
  if (userIds.length === 0)
    return

  if (tenantOptions.value.length === 0)
    await loadTenantOptions()

  batchAuthTenantId.value = resolveDefaultBatchTenantId()
  if (!batchAuthTenantId.value) {
    window.$message.warning('请选择授权租户')
    return
  }

  batchAuthModalVisible.value = true
  batchAuthOrgId.value = null
  batchAuthOrgTreeData.value = []
  roleSearchKeyword.value = ''
  checkedRoleKeys.value = []
  rolePagination.value.page = 1
  await loadBatchAuthOrgTree(batchAuthTenantId.value)
  batchAuthOrgId.value = selectedOrgNode.value?.id || resolvePreferredOrgIdFromTree(batchAuthOrgTreeData.value)
  if (!batchAuthOrgId.value) {
    window.$message.warning('请选择授权组织')
    return
  }
  await loadRoleList(batchAuthTenantId.value, batchAuthOrgId.value)
}

async function handleRelation(row) {
  currentUser.value = row
  relationModalVisible.value = true
  relationActiveTab.value = 'auth'
  await loadRelationTab('auth', row)
}

async function loadRelationTab(tab, row) {
  const user = row || currentUser.value
  if (!user?.id)
    return
  if (tab === 'auth') {
    roleSearchKeyword.value = ''
    rolePagination.value.page = 1
    checkedRoleKeys.value = []
    authOrgId.value = null
    await loadUserOrgBindings(user.id, resolveOperationTenantId(user))
    authOrgId.value = resolveDefaultAuthOrgId()
    if (authOrgId.value) {
      await loadRoleList(resolveOperationTenantId(user), authOrgId.value)
      await loadUserRoles(user.id, authOrgId.value)
    }
  }
  else if (tab === 'org') {
    mainOrgId.value = null
    checkedOrgKeys.value = []
    await loadOrgTree(resolveOperationTenantId(user))
    await loadUserOrgs(user.id)
  }
  else if (tab === 'post') {
    checkedPostKeys.value = []
    mainPostId.value = null
    await loadPostList(resolveOperationTenantId(user))
    await loadUserPosts(user.id)
  }
  else if (tab === 'tenant') {
    checkedTenantKeys.value = []
    defaultTenantId.value = null
    if (tenantOptions.value.length === 0) {
      await loadTenantOptions()
    }
    await loadUserTenants(user.id)
  }
}

async function handleRelationTabChange(tab) {
  await loadRelationTab(tab, currentUser.value)
}

async function handleRelationSubmit() {
  const tab = relationActiveTab.value
  relationSubmitLoading.value = true
  try {
    if (tab === 'auth') {
      await handleSubmitAuth()
    }
    else if (tab === 'org') {
      await handleSubmitOrg()
    }
    else if (tab === 'post') {
      await handleSubmitPost()
    }
    else if (tab === 'tenant') {
      await handleSubmitTenant()
    }
  }
  finally {
    relationSubmitLoading.value = false
  }
}

async function loadUserOrgBindings(userId, tenantId = resolveOperationTenantId()) {
  try {
    const res = await request.get(`/system/user/${userId}/org-bindings`, {
      params: buildTenantParams(tenantId),
    })
    if (res.code === 200) {
      currentUserOrgBindings.value = res.data || []
      return
    }
  }
  catch (error) {
    console.error('加载用户组织绑定详情失败:', error)
  }
  currentUserOrgBindings.value = []
}

function resolveDefaultAuthOrgId() {
  const activeOrgId = normalizeSingleNumber(userStore.activeOrgId || userStore.userInfo?.activeOrgId)
  if (activeOrgId !== null && currentUserOrgBindings.value.some(item => isSameKey(item.orgId, activeOrgId)))
    return activeOrgId

  const mainOrg = currentUserOrgBindings.value.find(item => Number(item.isMain) === 1)
  if (mainOrg)
    return normalizeSingleNumber(mainOrg.orgId)

  return normalizeSingleNumber(currentUserOrgBindings.value[0]?.orgId)
}

async function loadBatchAuthOrgTree(tenantId) {
  try {
    batchAuthOrgTreeData.value = await loadCompleteOrgTree(tenantId)
  }
  catch (error) {
    console.error('加载批量授权组织失败:', error)
    batchAuthOrgTreeData.value = []
  }
}

// 加载角色列表
async function loadRoleList(tenantId = resolveOperationTenantId(), orgId = authOrgId.value) {
  try {
    authLoading.value = true
    const res = await request.get('/system/role/page', {
      params: {
        pageNum: rolePagination.value.page,
        pageSize: rolePagination.value.pageSize,
        roleName: roleSearchKeyword.value || undefined,
        orgId: normalizeSingleNumber(orgId) || undefined,
        prioritizedUserId: batchAuthModalVisible.value ? undefined : currentUser.value?.id,
        ...buildTenantParams(tenantId),
      },
    })
    if (res.code === 200) {
      roleTableData.value = res.data.list || res.data.records || []
      rolePagination.value.itemCount = Number(res.data.total || 0)
    }
  }
  catch (error) {
    console.error('加载角色列表失败:', error)
    window.$message.error('加载角色列表失败')
  }
  finally {
    authLoading.value = false
  }
}

// 加载用户已有的角色
async function loadUserRoles(userId, orgId = authOrgId.value) {
  const normalizedOrgId = normalizeSingleNumber(orgId)
  if (!normalizedOrgId) {
    checkedRoleKeys.value = []
    return
  }
  try {
    authLoading.value = true
    const res = await request.get(`/system/user/${userId}/org-roles`, {
      params: {
        ...buildTenantParams(resolveOperationTenantId()),
        orgId: normalizedOrgId,
      },
    })
    if (res.code === 200) {
      checkedRoleKeys.value = normalizeNumberList(res.data || [])
    }
  }
  catch (error) {
    console.error('加载用户角色失败:', error)
    window.$message.error('加载用户角色失败')
  }
  finally {
    authLoading.value = false
  }
}

// 选中的角色变化
function handleCheckedKeysChange(keys) {
  checkedRoleKeys.value = normalizeNumberList(keys || [])
}

function handleRoleSearch() {
  rolePagination.value.page = 1
  loadRoleList(
    batchAuthModalVisible.value ? batchAuthTenantId.value : resolveOperationTenantId(),
    batchAuthModalVisible.value ? batchAuthOrgId.value : authOrgId.value,
  )
}

function handleRolePageChange(page) {
  rolePagination.value.page = page
  loadRoleList(
    batchAuthModalVisible.value ? batchAuthTenantId.value : resolveOperationTenantId(),
    batchAuthModalVisible.value ? batchAuthOrgId.value : authOrgId.value,
  )
}

function handleRolePageSizeChange(pageSize) {
  rolePagination.value.pageSize = pageSize
  rolePagination.value.page = 1
  loadRoleList(
    batchAuthModalVisible.value ? batchAuthTenantId.value : resolveOperationTenantId(),
    batchAuthModalVisible.value ? batchAuthOrgId.value : authOrgId.value,
  )
}

// 全选
function handleCheckAll() {
  const currentPageKeys = normalizeNumberList(roleTableData.value.map(role => role.id))
  checkedRoleKeys.value = Array.from(new Set([...checkedRoleKeys.value, ...currentPageKeys]))
}

// 全不选
function handleUncheckAll() {
  checkedRoleKeys.value = []
}

// 提交授权
async function handleSubmitAuth() {
  const orgId = normalizeSingleNumber(authOrgId.value)
  if (!orgId) {
    window.$message.warning('请选择授权组织')
    return
  }
  try {
    authSubmitLoading.value = true
    const res = await request.post(
      `/system/user/${currentUser.value.id}/org-roles`,
      {
        orgId,
        roleIds: normalizeNumberList(checkedRoleKeys.value),
        tenantId: resolveOperationTenantId(),
      },
    )
    if (res.code === 200) {
      window.$message.success('角色授权成功')
      relationModalVisible.value = false
      crudRef.value?.refresh()
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

async function handleSubmitBatchAuth() {
  const userIds = getSelectedBatchUserIds()
  if (userIds.length === 0)
    return
  const tenantId = normalizeSingleNumber(batchAuthTenantId.value)
  if (tenantId === null) {
    window.$message.warning('请选择授权租户')
    return
  }
  const orgId = normalizeSingleNumber(batchAuthOrgId.value)
  if (orgId === null) {
    window.$message.warning('请选择授权组织')
    return
  }
  const roleIds = normalizeNumberList(checkedRoleKeys.value)
  if (roleIds.length === 0) {
    window.$message.warning('请至少选择一个角色')
    return
  }

  try {
    batchAuthSubmitLoading.value = true
    for (const userId of userIds) {
      const currentRes = await request.get(`/system/user/${userId}/org-roles`, {
        params: { tenantId, orgId },
      })
      const mergedRoleIds = Array.from(new Set([
        ...normalizeNumberList(currentRes.code === 200 ? currentRes.data : []),
        ...roleIds,
      ]))
      const res = await request.post(`/system/user/${userId}/org-roles`, {
        tenantId,
        orgId,
        roleIds: mergedRoleIds,
      })
      if (res.code !== 200) {
        throw new Error(`用户 ${userId} 授权失败`)
      }
    }
    window.$message.success('批量授权成功')
    batchAuthModalVisible.value = false
    checkedRoleKeys.value = []
    clearBatchSelection()
    crudRef.value?.refresh()
  }
  catch (error) {
    console.error('批量授权失败:', error)
    window.$message.error('批量授权失败')
  }
  finally {
    batchAuthSubmitLoading.value = false
  }
}

// 加载组织树
async function loadOrgTree(tenantId = resolveOperationTenantId()) {
  try {
    orgLoading.value = true
    orgTreeData.value = await loadCompleteOrgTree(tenantId)
    if (orgTreeExpandAll.value)
      orgTreeExpandedKeys.value = getAllKeys(orgTreeData.value)
  }
  catch (error) {
    console.error('加载组织列表失败:', error)
    window.$message.error('加载组织列表失败')
  }
  finally {
    orgLoading.value = false
  }
}

// 加载用户已绑定的组织
async function loadUserOrgs(userId) {
  try {
    orgLoading.value = true
    const res = await request.get(`/system/user/${userId}/org-bindings`, {
      params: buildTenantParams(resolveOperationTenantId()),
    })
    if (res.code === 200) {
      const bindings = res.data || []
      checkedOrgKeys.value = normalizeNumberList(bindings.map(item => item.orgId))
      const mainBinding = bindings.find(item => Number(item.isMain) === 1)
      mainOrgId.value = normalizeSingleNumber(mainBinding?.orgId) || checkedOrgKeys.value[0] || null
    }
  }
  catch (error) {
    console.error('加载用户组织失败:', error)
    window.$message.error('加载用户组织失败')
  }
  finally {
    orgLoading.value = false
  }
}

// 组织展开的节点变化
function handleOrgExpandedKeysChange(keys) {
  orgTreeExpandedKeys.value = keys
}

// 组织选中的变化
function handleOrgCheckedKeysChange(keys) {
  checkedOrgKeys.value = normalizeNumberList(keys || [])
  if (mainOrgId.value && !checkedOrgKeys.value.includes(mainOrgId.value)) {
    mainOrgId.value = checkedOrgKeys.value[0] || null
  }
  else if (!mainOrgId.value && checkedOrgKeys.value.length > 0) {
    mainOrgId.value = checkedOrgKeys.value[0]
  }
}

// 提交组织绑定
async function handleSubmitOrg() {
  if (checkedOrgKeys.value.length === 0) {
    window.$message.warning('请至少选择一个组织')
    return
  }
  if (!mainOrgId.value || !checkedOrgKeys.value.includes(mainOrgId.value)) {
    window.$message.warning('请选择主组织')
    return
  }

  try {
    orgSubmitLoading.value = true
    const res = await request.post(
      `/system/user/${currentUser.value.id}/orgs`,
      {
        orgIds: checkedOrgKeys.value,
        mainOrgId: mainOrgId.value,
      },
      { params: buildTenantParams(resolveOperationTenantId()) },
    )
    if (res.code === 200) {
      window.$message.success('组织绑定成功')
      relationModalVisible.value = false
      crudRef.value?.refresh()
    }
  }
  catch (error) {
    console.error('组织绑定失败:', error)
    window.$message.error('组织绑定失败')
  }
  finally {
    orgSubmitLoading.value = false
  }
}

// 加载岗位列表
async function loadPostList(tenantId = resolveOperationTenantId()) {
  try {
    postLoading.value = true
    const res = await request.get('/system/post/list', {
      params: buildTenantParams(tenantId),
    })
    if (res.code === 200) {
      postList.value = res.data || []
    }
  }
  catch (error) {
    console.error('加载岗位列表失败:', error)
    window.$message.error('加载岗位列表失败')
  }
  finally {
    postLoading.value = false
  }
}

// 加载用户已绑定的岗位
async function loadUserPosts(userId) {
  try {
    postLoading.value = true
    const res = await request.get(`/system/user/${userId}/posts`, {
      params: buildTenantParams(resolveOperationTenantId()),
    })
    if (res.code === 200) {
      checkedPostKeys.value = normalizeNumberList(res.data || [])
      mainPostId.value = checkedPostKeys.value.length > 0 ? checkedPostKeys.value[0] : null
    }
  }
  catch (error) {
    console.error('加载用户岗位失败:', error)
    window.$message.error('加载用户岗位失败')
  }
  finally {
    postLoading.value = false
  }
}

// 提交岗位绑定
async function handleSubmitPost() {
  if (checkedPostKeys.value.length === 0) {
    window.$message.warning('请至少选择一个岗位')
    return
  }

  try {
    postSubmitLoading.value = true
    const res = await request.post(
      `/system/user/${currentUser.value.id}/posts`,
      {
        postIds: checkedPostKeys.value,
        mainPostId: mainPostId.value,
      },
      { params: buildTenantParams(resolveOperationTenantId()) },
    )
    if (res.code === 200) {
      window.$message.success('岗位绑定成功')
      relationModalVisible.value = false
      crudRef.value?.refresh()
    }
  }
  catch (error) {
    console.error('岗位绑定失败:', error)
    window.$message.error('岗位绑定失败')
  }
  finally {
    postSubmitLoading.value = false
  }
}

// 租户管理
async function handleOpenBatchTenant() {
  if (!userStore.isAdmin) {
    window.$message.warning('只有超级管理员可以批量加入租户')
    return
  }
  const userIds = getSelectedBatchUserIds()
  if (userIds.length === 0)
    return

  if (tenantOptions.value.length === 0)
    await loadTenantOptions()

  batchTenantForm.value = {
    tenantId: resolveDefaultBatchTenantId(),
    memberType: 2,
  }
  batchTenantModalVisible.value = true
}

async function loadUserTenants(userId) {
  try {
    tenantLoading.value = true
    const res = await request.get(`/system/user/${userId}/tenants`)
    if (res.code === 200) {
      const list = res.data || []
      checkedTenantKeys.value = list
        .filter(item => item.status !== 0)
        .map(item => normalizeSingleNumber(item.tenantId))
        .filter(item => item !== null)
      const defaultTenant = list.find(item => item.isDefault === 1)
      defaultTenantId.value = normalizeSingleNumber(defaultTenant?.tenantId) || checkedTenantKeys.value[0] || null
    }
  }
  catch (error) {
    console.error('加载用户租户失败:', error)
    window.$message.error('加载用户租户失败')
  }
  finally {
    tenantLoading.value = false
  }
}

async function handleSubmitTenant() {
  if (checkedTenantKeys.value.length === 0) {
    window.$message.warning('请至少选择一个租户')
    return
  }
  if (!defaultTenantId.value || !checkedTenantKeys.value.includes(defaultTenantId.value)) {
    defaultTenantId.value = checkedTenantKeys.value[0]
  }

  try {
    tenantSubmitLoading.value = true
    const res = await request.post(
      `/system/user/${currentUser.value.id}/tenants`,
      {
        tenantIds: checkedTenantKeys.value,
        defaultTenantId: defaultTenantId.value,
        memberType: normalizeSingleNumber(currentUser.value.userType, 2) === 1 ? 1 : 2,
      },
    )
    if (res.code === 200) {
      window.$message.success('租户绑定成功')
      relationModalVisible.value = false
      crudRef.value?.refresh()
    }
  }
  catch (error) {
    console.error('租户绑定失败:', error)
    window.$message.error('租户绑定失败')
  }
  finally {
    tenantSubmitLoading.value = false
  }
}

async function handleSubmitBatchTenant() {
  const userIds = getSelectedBatchUserIds()
  if (userIds.length === 0)
    return

  const tenantId = normalizeSingleNumber(batchTenantForm.value.tenantId)
  if (tenantId === null) {
    window.$message.warning('请选择目标租户')
    return
  }

  try {
    batchTenantSubmitLoading.value = true
    const res = await request.post('/system/user/batch/tenants', {
      userIds,
      tenantId,
      memberType: normalizeSingleNumber(batchTenantForm.value.memberType, 2),
    })
    if (res.code === 200) {
      window.$message.success('批量加入租户成功')
      batchTenantModalVisible.value = false
      clearBatchSelection()
      crudRef.value?.refresh()
    }
  }
  catch (error) {
    console.error('批量加入租户失败:', error)
    window.$message.error('批量加入租户失败')
  }
  finally {
    batchTenantSubmitLoading.value = false
  }
}
</script>

<style scoped>
.system-user-page {
  height: 100%;
  /* padding: 20px; */
  display: flex;
  flex-direction: column;
}

/* 左右布局 */
.user-layout {
  flex: 1;
  min-height: 0;
}

/* 左侧组织树面板 */
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
  border-bottom: 1px solid var(--border-light, #e5e7eb);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  background: var(--bg-primary, #fff);
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
  color: var(--text-secondary, #4b5563);
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
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #111827);
  line-height: 1.2;
}

.header-copy small {
  font-size: 11px;
  color: var(--text-tertiary, #9ca3af);
  line-height: 1.2;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.org-tree-header :deep(.n-button) {
  color: var(--text-tertiary, #9ca3af);
}

.org-tree-header :deep(.n-button:hover) {
  color: var(--text-secondary, #4b5563);
  background: var(--bg-secondary, #f6f8fb);
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

.org-tree-collapsed-hint {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 14px 6px;
  color: var(--text-tertiary, #9ca3af);
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.org-tree-collapsed-hint i {
  font-size: 22px;
}

.org-tree-collapsed-hint span {
  writing-mode: vertical-rl;
  letter-spacing: 2px;
  font-size: 12px;
  font-weight: 600;
}

.org-tree-collapsed-hint:hover {
  background: var(--bg-secondary, #f6f8fb);
  color: var(--text-secondary, #4b5563);
}

.org-tree-collapsed-hint.has-active-filter {
  color: var(--primary-color, #2563eb);
}

.org-tree-all-node {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  margin-bottom: 4px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary, #4b5563);
  border: 1px solid transparent;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease;
}

.org-tree-all-node:hover {
  background-color: var(--bg-secondary, #f8fafc);
  border-color: var(--border-light, #e5e7eb);
}

.org-tree-all-node.is-selected {
  background: #f3f7ff !important;
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 26%, var(--border-light, #e5e7eb));
  color: var(--primary-color, #2563eb);
  box-shadow: none;
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
  background: var(--bg-secondary, #f8fafc);
  border-color: var(--border-light, #e5e7eb);
}

.org-tree-content :deep(.premium-tree-row.is-selected) {
  background: #f3f7ff;
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 26%, var(--border-light, #e5e7eb));
  color: var(--text-primary, #0f172a);
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
  color: var(--primary-color, #2563eb);
}

.org-tree-content :deep(.premium-tree-row.is-selected .premium-tree-title) {
  color: var(--text-primary, #0f172a);
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

/* 右侧用户列表面板 */
.user-list-panel {
  flex: 1;
  height: 100%;
  min-width: 0;
  overflow: hidden;
}

.user-list-panel :deep(.ai-crud-page) {
  height: 100%;
}

/* 组织筛选提示 */
.org-filter-tip {
  margin-right: 12px;
}

.org-filter-tip :deep(.n-tag) {
  font-size: 13px;
}

.user-table-tag {
  display: inline-flex;
  align-items: center;
}

.user-table-tag :deep(.n-tag) {
  border-radius: 4px;
  font-weight: 500;
}

/* 授权弹窗样式 */
.auth-modal-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 600px;
}

.batch-action-alert {
  margin-bottom: 12px;
}

.batch-action-form {
  margin-bottom: 12px;
}

.auth-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  background-color: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.auth-role-search {
  flex: 1;
  min-width: 180px;
  max-width: 320px;
}

.auth-toolbar-actions {
  flex-shrink: 0;
}

.auth-tree-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border-radius: 4px;
  padding: 0;
  min-height: 300px;
  max-height: 500px;
}

.auth-tree-container :deep(.n-data-table) {
  min-width: 0;
}

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
}

.auth-tree-container::-webkit-scrollbar-thumb:hover {
  background: var(--n-border-color);
}

/* 组织弹窗样式 */
.org-modal-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 600px;
}

.org-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  background-color: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.org-main-hint {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 12px;
}

.org-main-hint strong {
  max-width: 240px;
  overflow: hidden;
  color: #1d4ed8;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.org-tree-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border-radius: 4px;
  padding: 12px;
  min-height: 300px;
  max-height: 400px;
}

.org-tree-container :deep(.premium-tree) {
  padding-top: 2px;
}

.org-tree-container::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.org-tree-container::-webkit-scrollbar-track {
  background: var(--n-scrollbar-color);
  border-radius: 4px;
}

.org-tree-container::-webkit-scrollbar-thumb {
  background: var(--n-scrollbar-color-hover);
  border-radius: 4px;
}

.org-tree-container::-webkit-scrollbar-thumb:hover {
  background: var(--n-border-color);
}

/* ═══════════════════════════════════════
 * 深色模式
 * ═══════════════════════════════════════ */
.dark .org-tree-header {
  background: #0f172a;
  border-bottom-color: #334155;
}

.dark .org-tree-header .header-copy span {
  color: #f1f5f9;
}

.dark .org-tree-header .header-copy small {
  color: #94a3b8;
}

.dark .header-icon {
  background: #162033;
  color: #cbd5e1;
}

.dark .org-tree-content {
  background: #0f172a;
}

.dark .org-tree-collapsed-hint {
  color: #94a3b8;
}

.dark .org-tree-collapsed-hint:hover,
.dark .org-tree-collapsed-hint.has-active-filter {
  background: #162033;
  color: #cbd5e1;
}

.dark .org-tree-all-node {
  color: #e2e8f0;
}

.dark .org-tree-all-node:hover {
  background-color: #162033;
  border-color: #334155;
}

.dark .org-tree-all-node.is-selected {
  background: rgba(37, 99, 235, 0.18) !important;
  border-color: rgba(96, 165, 250, 0.28);
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

.dark .org-tree-header :deep(.n-button) {
  color: #cbd5e1;
}

.dark .org-tree-header :deep(.n-button:hover) {
  color: #cbd5e1;
  background: #162033;
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

.dark .org-filter-tip .n-tag {
  background: #1e293b;
  border-color: #334155;
}

.dark .auth-toolbar {
  background-color: #1e293b;
}

.dark .org-toolbar {
  background-color: #1e293b;
}

.dark .auth-tree-container {
  background: #0f172a;
}

.dark .org-tree-container {
  background: #0f172a;
}

.dark .empty-state {
  background: #0f172a;
}

@media (max-width: 960px) {
  .org-tree-collapsed-hint {
    flex-direction: row;
    padding: 12px;
  }

  .org-tree-collapsed-hint span {
    writing-mode: initial;
    letter-spacing: 0;
  }
}
</style>
