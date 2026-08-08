<template>
  <div class="system-org-page">
    <MasterDetailWorkspace
      class="org-workspace"
      :collapsed="leftOrgPanelCollapsed"
      :aside-width="220"
      :collapsed-aside-width="64"
    >
      <template #aside>
        <div class="org-tree-panel" :class="{ 'is-collapsed': leftOrgPanelCollapsed }">
          <div class="org-tree-header">
            <div class="header-title">
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
                title="新增顶级组织"
                @click="handleAddRootOrg"
              >
                <template #icon>
                  <i class="i-material-symbols:add-rounded" />
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

          <div v-show="!leftOrgPanelCollapsed && userStore.isAdmin" class="org-tree-tools">
            <n-select
              v-if="userStore.isAdmin"
              v-model:value="selectedTenantId"
              :options="tenantSelectOptions"
              clearable
              filterable
              size="small"
              placeholder="全部租户"
              @update:value="handleTenantChange"
            />
          </div>

          <div v-show="!leftOrgPanelCollapsed" class="org-tree-content">
            <n-spin :show="leftOrgTreeLoading">
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
                :actions="orgTreeActions"
                @update:selected-keys="handleOrgNodeSelect"
                @update:expanded-keys="handleLeftOrgExpandedKeysChange"
                @action="handleOrgTreeAction"
              />
              <n-empty v-else description="暂无组织数据" size="small" />
            </n-spin>
          </div>

          <div
            v-show="leftOrgPanelCollapsed"
            class="org-tree-collapsed-hint"
            :class="{ 'has-active-filter': selectedOrgNode && !isShowAllOrganizations }"
            @click="toggleLeftOrgPanel"
          >
            <i class="i-material-symbols:account-tree-rounded" />
            <span>组织架构</span>
          </div>
        </div>
      </template>

      <section class="org-detail-panel">
        <n-tabs v-model:value="orgWorkspaceTab" type="line" animated class="org-workspace-tabs">
          <n-tab-pane name="members" tab="用户">
            <div class="member-body">
              <AiCrudPage
                ref="userCrudRef"
                api="/system/user"
                :api-config="{
                  list: 'get@/system/user/page',
                  detail: 'post@/system/user/getById',
                }"
                :search-schema="userSearchSchema"
                :columns="userTableColumns"
                :before-load-list="beforeLoadUserList"
                row-key="id"
                :hide-add="true"
                :hide-batch-delete="true"
                :hide-selection="true"
                :show-render-mode-switch="false"
                :search-grid-cols="3"
                :search-max-visible-fields="3"
                :page-size="10"
                :scroll-x="700"
                :table-props="{ dragScroll: false }"
              >
                <template #toolbar-end>
                  <n-button
                    size="small"
                    type="primary"
                    :disabled="isShowAllOrganizations || !selectedOrgNode"
                    @click="handleOpenAddUser"
                  >
                    <template #icon>
                      <i class="i-material-symbols:person-add-rounded" />
                    </template>
                    添加用户
                  </n-button>
                </template>
              </AiCrudPage>
            </div>
          </n-tab-pane>

          <n-tab-pane name="posts" tab="岗位">
            <header class="panel-header">
              <div>
                <h2>岗位列表</h2>
              </div>
              <n-button
                size="small"
                type="primary"
                :disabled="isShowAllOrganizations || !selectedOrgNode"
                @click="handleAddPost"
              >
                <template #icon>
                  <i class="i-material-symbols:add-rounded" />
                </template>
                新增岗位
              </n-button>
            </header>

            <div class="post-search">
              <n-input
                v-model:value="postKeyword"
                clearable
                size="small"
                placeholder="搜索岗位名称 / 编码"
              >
                <template #prefix>
                  <i class="i-material-symbols:search-rounded" />
                </template>
              </n-input>
              <n-button quaternary circle size="small" title="刷新岗位" :loading="postLoading" @click="loadPostList()">
                <template #icon>
                  <i class="i-material-symbols:refresh-rounded" />
                </template>
              </n-button>
            </div>

            <n-spin :show="postLoading" class="post-spin">
              <div class="post-list">
                <div
                  v-for="post in filteredPostList"
                  :key="post.id"
                  class="post-item"
                  :class="{ 'is-selected': isSameKey(selectedPostKey, post.id) }"
                  role="button"
                  tabindex="0"
                  @click="handlePostSelect(post)"
                  @keydown.enter.prevent="handlePostSelect(post)"
                  @keydown.space.prevent="handlePostSelect(post)"
                >
                  <span class="post-main">
                    <strong>{{ post.postName }}</strong>
                    <small v-if="post.postCode || resolveOptionLabel(postTypeOptions, post.postType)">
                      {{ post.postCode || resolveOptionLabel(postTypeOptions, post.postType) }}
                    </small>
                  </span>
                  <span class="post-side">
                    <DictTag :dict-type="NORMAL_DISABLE_DICT" :value="post.postStatus" size="small" force-tag />
                    <span class="post-actions" @click.stop>
                      <button type="button" title="编辑岗位" aria-label="编辑岗位" @click="handleEditPost(post)">
                        <i class="i-material-symbols:edit-outline-rounded" />
                      </button>
                      <button type="button" title="删除岗位" aria-label="删除岗位" class="type-error" @click="handleDeletePost(post)">
                        <i class="i-material-symbols:delete-outline-rounded" />
                      </button>
                    </span>
                  </span>
                </div>

                <n-empty
                  v-if="!postLoading && filteredPostList.length === 0"
                  description="暂无岗位"
                  size="small"
                  class="post-empty"
                />
              </div>
            </n-spin>
          </n-tab-pane>
        </n-tabs>
      </section>
    </MasterDetailWorkspace>

    <UserSelectModal
      v-model:show="userSelectVisible"
      title="添加组织成员"
      multiple
      :selected-users="[]"
      @confirm="handleAddUsersConfirm"
    />

    <div class="crud-driver" aria-hidden="true">
      <AiCrudPage
        ref="orgCrudRef"
        api="/system/org"
        :api-config="{
          list: 'get@/system/org/lazyTree',
          detail: 'post@/system/org/getById',
          add: 'post@/system/org/add',
          update: 'post@/system/org/edit',
          delete: 'post@/system/org/remove?id=:id',
        }"
        :search-schema="[]"
        :columns="orgDriverColumns"
        :edit-schema="orgEditSchema"
        :before-render-list="beforeRenderOrgList"
        :before-submit="beforeSubmitOrg"
        :before-load-list="beforeLoadOrgDriverList"
        :before-render-form="beforeRenderOrgForm"
        row-key="id"
        :edit-grid-cols="2"
        edit-label-align="left"
        modal-width="900px"
        :show-search="false"
        :show-pagination="false"
        :hide-toolbar="true"
        :hide-selection="true"
        :hide-batch-delete="true"
        :table-props="{
          onLoad: handleOrgDriverLoad,
        }"
        @submit-success="handleOrgMutationSuccess"
      />

      <AiCrudPage
        ref="postCrudRef"
        api="/system/post"
        :api-config="{
          list: 'get@/system/post/page',
          detail: 'post@/system/post/getById',
          add: 'post@/system/post/add',
          update: 'post@/system/post/edit',
          delete: 'post@/system/post/removeBatch',
        }"
        :search-schema="[]"
        :columns="postDriverColumns"
        :edit-schema="postEditSchema"
        :before-submit="beforeSubmitPost"
        :before-load-list="beforeLoadPostDriverList"
        :before-render-form="beforeRenderPostForm"
        row-key="id"
        :edit-grid-cols="2"
        edit-label-align="left"
        modal-width="800px"
        add-button-text="新增岗位"
        :show-search="false"
        :show-pagination="false"
        :hide-toolbar="true"
        :hide-selection="true"
        :hide-batch-delete="true"
        @submit-success="handlePostMutationSuccess"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, h, nextTick, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import MasterDetailWorkspace from '@/components/common/MasterDetailWorkspace.vue'
import PremiumTree from '@/components/common/PremiumTree.vue'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import UserSelectModal from '@/components/common/UserSelectModal.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { request } from '@/utils'

defineOptions({ name: 'SystemOrg' })

const ORG_TYPE_DICT = 'sys_org_type'
const NORMAL_DISABLE_DICT = 'sys_normal_disable'
const POST_TYPE_DICT = 'sys_post_type'
const USER_STATUS_DICT = 'sys_user_status'
const ALL_POST_KEY = '__all__'

const { dict } = useDict(
  ORG_TYPE_DICT,
  NORMAL_DISABLE_DICT,
  POST_TYPE_DICT,
  USER_STATUS_DICT,
)

const userStore = useUserStore()
const orgCrudRef = ref(null)
const postCrudRef = ref(null)
const userCrudRef = ref(null)

const tenantOptions = ref([])
const selectedTenantId = ref(userStore.userInfo?.tenantId || null)
const leftOrgTreeData = ref([])
const leftOrgTreeLoading = ref(false)
const leftOrgExpandAll = ref(false)
const leftOrgExpandedKeys = ref([])
const leftOrgPanelCollapsed = ref(false)
const selectedOrgKeys = ref([])
const selectedOrgNode = ref(null)
const isShowAllOrganizations = ref(true)
const parentOrgOptions = ref([{ label: '顶级组织', value: 0, key: 0 }])
const editRegionOptions = ref([])
const defaultParentId = ref(0)

const postList = ref([])
const postLoading = ref(false)
const postKeyword = ref('')
const selectedPostKey = ref(ALL_POST_KEY)
const selectedPostNode = ref(null)
const orgWorkspaceTab = ref('members')
const userSelectVisible = ref(false)

const tenantSelectOptions = computed(() => tenantOptions.value.map(item => ({
  label: item.tenantName,
  value: item.id,
})))
const orgStatusOptions = computed(() => toNumberOptions(dict.value[NORMAL_DISABLE_DICT]))
const postTypeOptions = computed(() => toNumberOptions(dict.value[POST_TYPE_DICT]))
const postStatusOptions = computed(() => toNumberOptions(dict.value[NORMAL_DISABLE_DICT]))
const userStatusOptions = computed(() => toNumberOptions(dict.value[USER_STATUS_DICT]))

const orgTreeSummaryText = computed(() => {
  const total = countTreeNodes(leftOrgTreeData.value)
  if (!total)
    return '未加载组织'
  return `${total} 个组织节点`
})

const filteredPostList = computed(() => {
  const keyword = String(postKeyword.value || '').trim().toLowerCase()
  if (!keyword)
    return postList.value
  return postList.value.filter((item) => {
    return String(item.postName || '').toLowerCase().includes(keyword)
      || String(item.postCode || '').toLowerCase().includes(keyword)
  })
})

const orgDriverColumns = computed(() => [
  { prop: 'orgName', label: '组织名称', minWidth: 180 },
  { prop: 'leaderName', label: '负责人', width: 120 },
])

const postDriverColumns = computed(() => [
  { prop: 'postName', label: '岗位名称', minWidth: 160 },
  { prop: 'postCode', label: '岗位编码', width: 140 },
])

const orgEditSchema = computed(() => [
  ...(userStore.isAdmin
    ? [{
        field: 'tenantId',
        label: '所属租户',
        type: 'select',
        defaultValue: resolveSelectedTenantId(),
        rules: [{ required: true, type: 'number', message: '请选择所属租户', trigger: 'change' }],
        props: {
          placeholder: '请选择所属租户',
          options: tenantSelectOptions.value,
        },
      }]
    : []),
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'parentId',
    label: '上级组织',
    type: 'treeSelect',
    defaultValue: defaultParentId.value,
    props: {
      placeholder: '请选择上级组织',
      clearable: true,
      filterable: true,
      defaultExpandAll: true,
    },
    options: () => parentOrgOptions.value,
  },
  {
    field: 'orgName',
    label: '组织名称',
    type: 'input',
    rules: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入组织名称',
    },
  },
  {
    field: 'orgType',
    label: '组织类型',
    type: 'select',
    defaultValue: dict.value[ORG_TYPE_DICT]?.[0]?.value || '2',
    rules: [{ required: true, message: '请选择组织类型', trigger: 'change' }],
    props: {
      placeholder: '请选择组织类型',
      options: dict.value[ORG_TYPE_DICT] || [],
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
  {
    type: 'divider',
    label: '负责人信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'leaderName',
    label: '负责人',
    type: 'input',
    props: {
      placeholder: '请输入负责人姓名',
    },
  },
  {
    field: 'phone',
    label: '联系电话',
    type: 'input',
    rules: [
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
    ],
    props: {
      placeholder: '请输入联系电话',
    },
  },
  {
    field: 'address',
    label: '地址',
    type: 'input',
    span: 2,
    props: {
      placeholder: '请输入组织地址',
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
    field: 'orgStatus',
    label: '组织状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: orgStatusOptions.value,
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

const postEditSchema = computed(() => [
  ...(userStore.isAdmin
    ? [{
        field: 'tenantId',
        label: '所属租户',
        type: 'select',
        defaultValue: resolveSelectedTenantId(),
        rules: [{ required: true, type: 'number', message: '请选择所属租户', trigger: 'change' }],
        props: {
          placeholder: '请选择所属租户',
          options: tenantSelectOptions.value,
        },
      }]
    : []),
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

const userSearchSchema = computed(() => [
  {
    field: 'keyword',
    label: '用户',
    type: 'input',
    props: {
      placeholder: '用户名 / 真实姓名',
    },
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
      clearable: true,
      options: userStatusOptions.value,
    },
  },
])

const userTableColumns = computed(() => [
  {
    prop: 'realName',
    label: '用户',
    minWidth: 180,
    render: row => h(SystemTableCell, {
      title: resolveUserDisplayName(row),
      subtitle: resolveUserAccountLabel(row),
      interactive: true,
      avatar: true,
      tooltip: `查看用户详情：${row.username || resolveUserDisplayName(row)}`,
      onActivate: () => userCrudRef.value?.showDetail(row),
    }),
  },
  {
    prop: 'phone',
    label: '手机号',
    width: 130,
    render: row => row.phone || '-',
  },
  {
    prop: 'postName',
    label: '岗位',
    minWidth: 150,
    ellipsis: { tooltip: true },
    render: row => row.postName || '-',
  },
  ...(!selectedOrgNode.value || isShowAllOrganizations.value
    ? [{
        prop: 'orgName',
        label: '所属组织',
        minWidth: 180,
        render: row => h(SystemTableCell, {
          values: splitTableCellValues(row.orgName || row.orgNames),
        }),
      }]
    : []),
  {
    prop: 'userStatus',
    label: '状态',
    width: 90,
    render: row => h(DictTag, { dictType: USER_STATUS_DICT, value: row.userStatus, size: 'small', forceTag: true }),
  },
  {
    prop: 'actions',
    label: '操作',
    width: 136,
    fixed: 'right',
    actions: [
      {
        label: '设负责人',
        key: 'setLeader',
        type: 'primary',
        visible: () => Boolean(selectedOrgNode.value && !isShowAllOrganizations.value),
        onClick: row => handleSetOrgLeader(row),
      },
      {
        label: '移除',
        key: 'remove',
        type: 'error',
        visible: () => Boolean(selectedOrgNode.value && !isShowAllOrganizations.value),
        onClick: row => handleRemoveUserFromOrg(row),
      },
    ],
  },
])

onMounted(async () => {
  await loadTenantOptions()
  await loadRegionOptions()
  await loadParentOrgOptions()
  await loadLeftOrgTree()
  await loadPostList()
})

function toNumberOptions(options = []) {
  return (options || []).map(item => ({
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

function isSameKey(left, right) {
  return String(left) === String(right)
}

function resolveOptionLabel(options = [], value) {
  const normalized = normalizeSingleNumber(value, value)
  const match = (options || []).find(item => isSameKey(item.value, normalized))
  return match?.label
}

function buildTenantParams(tenantId = resolveSelectedTenantId()) {
  const resolvedTenantId = userStore.isAdmin ? tenantId : userStore.userInfo?.tenantId
  return resolvedTenantId ? { tenantId: resolvedTenantId } : {}
}

function resolveSelectedTenantId(row) {
  return row?.tenantId
    || selectedTenantId.value
    || userStore.userInfo?.tenantId
}

async function loadTenantOptions() {
  try {
    const res = await request.get('/system/tenant/assignable/options')
    if (res.code === 200) {
      tenantOptions.value = res.data || []
      if (userStore.isAdmin && !selectedTenantId.value && tenantOptions.value.length > 0) {
        selectedTenantId.value = tenantOptions.value[0].id
      }
    }
  }
  catch (error) {
    console.error('加载租户选项失败:', error)
  }
}

async function handleTenantChange() {
  selectedOrgKeys.value = []
  selectedOrgNode.value = null
  isShowAllOrganizations.value = true
  selectedPostKey.value = ALL_POST_KEY
  selectedPostNode.value = null
  await loadParentOrgOptions()
  await loadLeftOrgTree()
  await loadPostList()
  refreshRightPanels()
}

async function loadParentOrgOptions(tenantId = resolveSelectedTenantId()) {
  try {
    const res = await request.get('/system/org/tree', {
      params: buildTenantParams(tenantId),
    })
    if (res.code === 200) {
      parentOrgOptions.value = [
        { label: '顶级组织', value: 0, key: 0 },
        ...convertOrgToTreeSelect(res.data || []),
      ]
    }
  }
  catch (error) {
    console.error('加载上级组织选项失败:', error)
  }
}

async function loadRegionOptions() {
  try {
    const res = await request.get('/system/region/treeAll', { params: { dataRight: true } })
    if (res.code === 200) {
      editRegionOptions.value = convertRegionToTreeSelect(res.data || [], true)
    }
  }
  catch (error) {
    console.error('加载行政区划选项失败:', error)
  }
}

function convertOrgToTreeSelect(list = []) {
  return (list || []).map(item => ({
    label: item.orgName || item.label || String(item.id),
    value: normalizeSingleNumber(item.id),
    key: normalizeSingleNumber(item.id),
    children: item.children?.length ? convertOrgToTreeSelect(item.children) : undefined,
  }))
}

function convertRegionToTreeSelect(list, virtualDisabled = true) {
  return (list || []).map((item) => {
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

function orgTreeActions() {
  return [
    {
      key: 'addChild',
      title: '新增下级组织',
      label: '新增下级',
      icon: 'i-material-symbols:add-circle-outline-rounded',
      type: 'primary',
    },
    {
      key: 'edit',
      title: '编辑组织',
      label: '编辑',
      icon: 'i-material-symbols:edit-outline-rounded',
    },
    {
      key: 'delete',
      title: '删除组织',
      label: '删除',
      icon: 'i-material-symbols:delete-outline-rounded',
      type: 'error',
    },
  ]
}

function handleOrgTreeAction(action, node) {
  if (action.key === 'addChild') {
    handleAddChildOrg(node)
    return
  }
  if (action.key === 'edit') {
    handleEditOrg(node)
    return
  }
  if (action.key === 'delete')
    handleDeleteOrg(node)
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

async function loadLeftOrgTree(tenantId = resolveSelectedTenantId()) {
  try {
    leftOrgTreeLoading.value = true
    leftOrgTreeData.value = await loadCompleteOrgTree(tenantId)
    leftOrgExpandedKeys.value = leftOrgExpandAll.value ? getAllKeys(leftOrgTreeData.value) : []
    if (selectedOrgNode.value) {
      const nextSelected = findOrgNode(leftOrgTreeData.value, selectedOrgNode.value.id)
      selectedOrgNode.value = nextSelected
      selectedOrgKeys.value = nextSelected ? [nextSelected.id] : []
      isShowAllOrganizations.value = !nextSelected
    }
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

async function handleOrgNodeSelect(keys) {
  selectedOrgKeys.value = keys
  selectedPostKey.value = ALL_POST_KEY
  selectedPostNode.value = null

  if (keys.length > 0) {
    isShowAllOrganizations.value = false
    selectedOrgNode.value = findOrgNode(leftOrgTreeData.value, keys[0])
  }
  else {
    selectedOrgNode.value = null
  }

  await loadPostList()
  refreshRightPanels()
}

function handleLeftOrgExpandedKeysChange(keys) {
  leftOrgExpandedKeys.value = keys
}

function toggleLeftOrgPanel() {
  leftOrgPanelCollapsed.value = !leftOrgPanelCollapsed.value
}

function beforeLoadOrgDriverList(params) {
  Object.assign(params, buildTenantParams(params.tenantId))
  return params
}

function beforeRenderOrgList(list) {
  return (list || []).map(item => ({
    ...item,
    isLeaf: !item.hasChildren,
  }))
}

function handleOrgDriverLoad(node) {
  return new Promise((resolve) => {
    request.get(`/system/org/children/${node.id}`, {
      params: buildTenantParams(resolveSelectedTenantId(node)),
    })
      .then((res) => {
        if (res.code === 200) {
          node.children = (res.data || []).map(item => ({
            ...item,
            isLeaf: !item.hasChildren,
          }))
        }
        resolve()
      })
      .catch(() => {
        resolve()
      })
  })
}

function beforeSubmitOrg(formData) {
  if (!userStore.isAdmin) {
    formData.tenantId = userStore.userInfo?.tenantId
  }
  else if (!formData.tenantId) {
    formData.tenantId = resolveSelectedTenantId()
  }
  return formData
}

async function beforeRenderOrgForm(row) {
  const tenantId = resolveSelectedTenantId(row)
  await loadParentOrgOptions(tenantId)
  if (row)
    return row
  return {
    tenantId,
    parentId: defaultParentId.value,
  }
}

async function handleAddRootOrg() {
  const tenantId = resolveSelectedTenantId()
  await loadParentOrgOptions(tenantId)
  defaultParentId.value = 0
  orgCrudRef.value?.showAdd({ tenantId, parentId: 0 })
  await nextTick()
  defaultParentId.value = 0
}

async function handleAddChildOrg(row = selectedOrgNode.value) {
  if (!row) {
    window.$message.warning('请选择上级组织')
    return
  }
  const tenantId = resolveSelectedTenantId(row)
  await loadParentOrgOptions(tenantId)
  defaultParentId.value = row.id
  orgCrudRef.value?.showAdd({ tenantId, parentId: row.id })
  await nextTick()
  defaultParentId.value = 0
}

async function handleEditOrg(row) {
  if (!row)
    return
  await loadParentOrgOptions(resolveSelectedTenantId(row))
  orgCrudRef.value?.showEdit(row)
}

function handleDeleteOrg(row) {
  if (!row)
    return
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除组织"${row.orgName}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/org/remove', null, { params: { id: row.id } })
        if (res.code === 200) {
          window.$message.success('删除成功')
          if (isSameKey(selectedOrgNode.value?.id, row.id)) {
            selectedOrgNode.value = null
            selectedOrgKeys.value = []
            isShowAllOrganizations.value = true
          }
          await handleOrgMutationSuccess()
        }
      }
      catch {
        window.$message.error('删除失败')
      }
    },
  })
}

async function handleOrgMutationSuccess() {
  await loadParentOrgOptions()
  await loadLeftOrgTree()
  await loadPostList()
  refreshRightPanels()
}

async function loadPostList() {
  try {
    postLoading.value = true
    const params = {
      ...buildTenantParams(),
    }
    if (selectedOrgNode.value && !isShowAllOrganizations.value)
      params.orgId = selectedOrgNode.value.id

    const res = await request.get('/system/post/list', { params })
    if (res.code === 200) {
      postList.value = res.data || []
      if (selectedPostKey.value !== ALL_POST_KEY) {
        const nextSelected = postList.value.find(item => isSameKey(item.id, selectedPostKey.value))
        selectedPostNode.value = nextSelected || null
        if (!nextSelected)
          selectedPostKey.value = ALL_POST_KEY
      }
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

function handlePostSelect(post) {
  if (isSameKey(selectedPostKey.value, post.id)) {
    selectedPostKey.value = ALL_POST_KEY
    selectedPostNode.value = null
    refreshRightPanels()
    return
  }
  selectedPostKey.value = post.id
  selectedPostNode.value = post
  refreshRightPanels()
}

function beforeLoadPostDriverList(params) {
  Object.assign(params, buildTenantParams(params.tenantId))
  if (selectedOrgNode.value && !isShowAllOrganizations.value)
    params.orgId = selectedOrgNode.value.id
  return params
}

function beforeSubmitPost(formData) {
  if (!userStore.isAdmin) {
    formData.tenantId = userStore.userInfo?.tenantId
  }
  else if (!formData.tenantId) {
    formData.tenantId = resolveSelectedTenantId()
  }
  return formData
}

function beforeRenderPostForm(row) {
  if (row)
    return row
  return {
    tenantId: resolveSelectedTenantId(),
    orgId: selectedOrgNode.value?.id,
  }
}

function handleAddPost() {
  if (!selectedOrgNode.value || isShowAllOrganizations.value) {
    window.$message.warning('请选择一个组织后再新增岗位')
    return
  }
  postCrudRef.value?.showAdd({
    tenantId: resolveSelectedTenantId(),
    orgId: selectedOrgNode.value.id,
  })
}

function handleEditPost(row) {
  if (!row)
    return
  postCrudRef.value?.showEdit(row)
}

function handleDeletePost(row) {
  if (!row)
    return
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
          if (isSameKey(selectedPostKey.value, row.id)) {
            selectedPostKey.value = ALL_POST_KEY
            selectedPostNode.value = null
          }
          await handlePostMutationSuccess()
        }
      }
      catch {
        window.$message.error('删除失败')
      }
    },
  })
}

async function handlePostMutationSuccess() {
  await loadPostList()
  refreshRightPanels()
}

function beforeLoadUserList(params) {
  Object.assign(params, buildTenantParams(params.tenantId))
  if (selectedOrgNode.value && !isShowAllOrganizations.value) {
    params.orgId = selectedOrgNode.value.id
    params.directOrgOnly = true
  }
  if (selectedPostNode.value && selectedPostKey.value !== ALL_POST_KEY)
    params.postId = selectedPostNode.value.id
  return params
}

function refreshUserList() {
  userCrudRef.value?.refresh()
}

function refreshRightPanels() {
  refreshUserList()
}

function handleOpenAddUser() {
  if (!selectedOrgNode.value || isShowAllOrganizations.value) {
    window.$message.warning('请选择一个组织后再添加用户')
    return
  }
  userSelectVisible.value = true
}

async function handleAddUsersConfirm(users) {
  const selectedUsers = (Array.isArray(users) ? users : [users]).filter(Boolean)
  if (selectedUsers.length === 0)
    return
  if (!selectedOrgNode.value || isShowAllOrganizations.value) {
    window.$message.warning('请选择一个组织后再添加用户')
    return
  }

  try {
    for (const user of selectedUsers) {
      const bindRes = await request.post(`/system/user/${user.id}/org`, null, {
        params: {
          orgId: selectedOrgNode.value.id,
          isMain: 0,
        },
      })
      if (bindRes.code !== 200)
        throw new Error(bindRes.msg || '绑定组织失败')
      if (selectedPostNode.value)
        await bindUserToSelectedPost(user.id)
    }
    window.$message.success('用户添加成功')
    refreshUserList()
  }
  catch (error) {
    console.error('添加组织成员失败:', error)
    window.$message.error('添加组织成员失败')
  }
}

async function bindUserToSelectedPost(userId) {
  const postId = normalizeSingleNumber(selectedPostNode.value?.id)
  if (!postId)
    return

  const tenantId = resolveSelectedTenantId(selectedOrgNode.value)
  const currentRes = await request.get(`/system/user/${userId}/posts`, {
    params: buildTenantParams(tenantId),
  })
  const currentPostIds = currentRes.code === 200 ? normalizeNumberList(currentRes.data || []) : []
  const mergedPostIds = Array.from(new Set([...currentPostIds, postId]))
  const mainPostId = currentPostIds[0] || postId
  const bindPostRes = await request.post(
    `/system/user/${userId}/posts`,
    {
      postIds: mergedPostIds,
      mainPostId,
    },
    { params: buildTenantParams(tenantId) },
  )
  if (bindPostRes.code !== 200)
    throw new Error(bindPostRes.msg || '绑定岗位失败')
}

function handleRemoveUserFromOrg(row) {
  if (!selectedOrgNode.value || isShowAllOrganizations.value) {
    window.$message.warning('请选择组织后再移除用户')
    return
  }
  window.$dialog.warning({
    title: '移除组织成员',
    content: `确定将"${resolveUserDisplayName(row)}"从"${selectedOrgNode.value.orgName}"移除吗？`,
    positiveText: '移除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post(`/system/user/${row.id}/org/unbind`, null, {
          params: { orgId: selectedOrgNode.value.id },
        })
        if (res.code === 200) {
          window.$message.success('已移除')
          refreshUserList()
        }
      }
      catch (error) {
        console.error('移除组织成员失败:', error)
        window.$message.error('移除组织成员失败')
      }
    },
  })
}

async function handleSetOrgLeader(row) {
  if (!selectedOrgNode.value || isShowAllOrganizations.value) {
    window.$message.warning('请选择组织后再设置负责人')
    return
  }
  try {
    const detailRes = await request.post('/system/org/getById', null, {
      params: { id: selectedOrgNode.value.id },
    })
    const orgDetail = detailRes.code === 200 ? (detailRes.data || {}) : {}
    const leaderName = resolveUserDisplayName(row)
    const res = await request.post('/system/org/edit', {
      ...selectedOrgNode.value,
      ...orgDetail,
      id: selectedOrgNode.value.id,
      leaderId: row.id,
      leaderName,
    })
    if (res.code === 200) {
      window.$message.success('负责人设置成功')
      await loadParentOrgOptions()
      await loadLeftOrgTree()
    }
  }
  catch (error) {
    console.error('设置组织负责人失败:', error)
    window.$message.error('设置组织负责人失败')
  }
}

function normalizeNumberList(values = []) {
  return (Array.isArray(values) ? values : [values])
    .map(value => normalizeSingleNumber(value))
    .filter(value => value !== null && value !== undefined)
}

function resolveUserDisplayName(row = {}) {
  return row.realName || row.name || row.nickname || row.username || `用户${row.id}`
}

function resolveUserAccountLabel(row = {}) {
  const username = String(row.username || '').trim()
  return username && username !== resolveUserDisplayName(row) ? `@${username}` : ''
}

function splitTableCellValues(value) {
  const rawValues = Array.isArray(value) ? value : [value]
  return rawValues
    .flatMap(item => String(item || '').split(/[、,，]/))
    .map(item => item.trim())
    .filter(Boolean)
}
</script>

<style scoped>
.system-org-page {
  height: 100%;
  min-height: 0;
}

.org-workspace {
  height: 100%;
  min-height: 0;
}

.org-tree-panel,
.org-detail-panel {
  height: 100%;
  min-height: 0;
}

.org-tree-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.org-tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  min-height: 42px;
  padding: 7px 8px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.org-tree-panel.is-collapsed .org-tree-header {
  flex-direction: column;
  padding: 10px 6px;
}

.header-title,
.header-actions {
  display: flex;
  align-items: center;
}

.header-title {
  min-width: 0;
}

.header-actions {
  gap: 4px;
  flex-shrink: 0;
}

.header-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.header-copy span {
  color: #0f172a;
  font-size: 13px;
  font-weight: 650;
  line-height: 1.2;
}

.header-copy small {
  color: #64748b;
  font-size: 11px;
}

.org-tree-tools {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 6px;
  border-bottom: 1px solid #e5e7eb;
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

.org-tree-collapsed-hint:hover,
.org-tree-collapsed-hint.has-active-filter {
  background: rgba(37, 99, 235, 0.06);
  color: #2563eb;
}

.org-detail-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.org-workspace-tabs {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.org-workspace-tabs :deep(.n-tabs-nav) {
  flex-shrink: 0;
  min-height: 40px;
  padding: 0 10px;
  border-bottom: 1px solid #e5e7eb;
}

.org-workspace-tabs :deep(.n-tabs-tab) {
  padding: 9px 12px;
  font-size: 13px;
  font-weight: 600;
}

.org-workspace-tabs :deep(.n-tabs-pane-wrapper) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.org-workspace-tabs :deep(.n-tab-pane) {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.panel-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  padding: 7px 10px;
  border-bottom: 1px solid #e5e7eb;
}

.panel-eyebrow {
  margin: 0 0 4px;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.panel-header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 14px;
  font-weight: 650;
  line-height: 1.3;
}

.post-search {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  padding: 6px 8px;
  border-bottom: 1px solid #e5e7eb;
}

.post-spin {
  flex: 1;
  min-height: 0;
}

.post-spin :deep(.n-spin-content) {
  height: 100%;
}

.post-list {
  height: 100%;
  overflow-y: auto;
  padding: 4px 6px 8px;
}

.post-item {
  width: 100%;
  min-height: 38px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 5px 6px;
  margin-bottom: 2px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #fff;
  color: #0f172a;
  text-align: left;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.post-item:hover {
  border-color: #e2e8f0;
  background: #f8fafc;
}

.post-item.is-selected {
  border-color: rgba(37, 99, 235, 0.18);
  background: rgba(37, 99, 235, 0.08);
  box-shadow: inset 3px 0 0 #2563eb;
}

.post-main {
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.post-main strong {
  overflow: hidden;
  color: inherit;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-main small {
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-side {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 4px;
}

.post-empty {
  margin-top: 48px;
}

.post-actions {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.16s ease;
}

.post-item:hover .post-actions,
.post-item.is-selected .post-actions {
  opacity: 1;
  pointer-events: auto;
}

.post-actions button {
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
  transition:
    background-color 0.16s ease,
    color 0.16s ease;
}

.post-actions button:hover {
  background: #eef2ff;
  color: #2563eb;
}

.post-actions button.type-error:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.post-actions i {
  font-size: 14px;
}

.member-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 8px 10px 10px;
}

.member-body :deep(.ai-crud-page) {
  height: 100%;
  min-height: 0;
}

.member-body :deep(.ai-crud-layout),
.member-body :deep(.ai-crud-content),
.member-body :deep(.ai-crud-table),
.member-body :deep(.ai-table-wrapper),
.member-body :deep(.n-data-table) {
  min-height: 0;
}

.member-body :deep(.ai-crud-table) {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.member-body :deep(.ai-table-wrapper) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.member-body :deep(.n-data-table-wrapper),
.member-body :deep(.n-data-table-base-table),
.member-body :deep(.n-data-table-base-table-body) {
  min-height: 0;
}

.member-user-inline {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-width: 0;
  gap: 2px;
  white-space: nowrap;
}

.member-user-inline strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
}

.member-user-inline em {
  flex: 0 0 auto;
  color: #cbd5e1;
  font-style: normal;
}

.member-user-inline span {
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

.dark .org-tree-header {
  border-bottom-color: #334155;
  background: #0f172a;
}

.dark .header-copy span,
.dark .panel-header h2,
.dark .post-main strong,
.dark .member-user-inline strong {
  color: #f1f5f9;
}

.dark .header-copy small,
.dark .panel-eyebrow,
.dark .post-main small,
.dark .member-user-inline span {
  color: #94a3b8;
}

.dark .org-tree-tools,
.dark .panel-header,
.dark .post-search,
.dark .org-workspace-tabs :deep(.n-tabs-nav) {
  border-color: #334155;
  background: #0f172a;
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

.dark .post-item {
  border-color: transparent;
  background: #111827;
  color: #e2e8f0;
}

.dark .post-item:hover {
  border-color: #334155;
  background: #162033;
}

.dark .post-item.is-selected {
  border-color: rgba(96, 165, 250, 0.35);
  background: rgba(37, 99, 235, 0.18);
  color: #bfdbfe;
}

.dark .post-actions button:hover {
  background: rgba(30, 41, 59, 0.86);
  color: #bfdbfe;
}

.dark .post-actions button.type-error:hover {
  background: rgba(239, 68, 68, 0.14);
  color: #fca5a5;
}
</style>
