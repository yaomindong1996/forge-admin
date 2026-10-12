<template>
  <!-- 租户用户查询与管理 -->
  <n-modal
    v-model:show="usersModalVisible"
    :title="`租户用户 - ${currentTenant.tenantName || ''}`"
    preset="card"
    class="tenant-users-dialog"
    :style="{ width: 'min(960px, calc(100vw - 24px))' }"
    :mask-closable="false"
  >
    <div class="tenant-users-modal">
      <!-- 筛选区 -->
      <div class="tenant-users-search">
        <n-space :wrap="true" :size="[8, 8]">
          <n-input
            v-model:value="userSearchParams.username"
            placeholder="用户名"
            clearable
            size="small"
            class="tenant-users-filter-control"
            @clear="handleUserSearch"
            @keyup.enter="handleUserSearch"
          />
          <n-input
            v-model:value="userSearchParams.realName"
            placeholder="真实姓名"
            clearable
            size="small"
            class="tenant-users-filter-control"
            @clear="handleUserSearch"
            @keyup.enter="handleUserSearch"
          />
          <n-input
            v-model:value="userSearchParams.phone"
            placeholder="手机号"
            clearable
            size="small"
            class="tenant-users-filter-control"
            @clear="handleUserSearch"
            @keyup.enter="handleUserSearch"
          />
          <n-select
            v-model:value="userSearchParams.userStatus"
            placeholder="用户状态"
            clearable
            size="small"
            class="tenant-users-filter-status"
            :options="userStatusOptions"
          />
          <n-button size="small" type="primary" @click="handleUserSearch">
            <template #icon>
              <i class="i-material-symbols:search" />
            </template>
            查询
          </n-button>
          <n-button size="small" @click="handleUserSearchReset">
            重置
          </n-button>
        </n-space>
      </div>

      <!-- 工具栏 -->
      <div class="tenant-users-toolbar">
        <NTag type="info" size="small">
          共 {{ userPagination.itemCount }} 个用户
        </NTag>
        <n-button size="small" @click="loadTenantUsers">
          <template #icon>
            <i class="i-material-symbols:refresh" />
          </template>
          刷新
        </n-button>
      </div>

      <!-- 表格 -->
      <n-spin :show="usersLoading">
        <n-data-table
          :columns="userTableColumns"
          :data="tenantUsers"
          :pagination="userPaginationConfig"
          :row-key="row => row.id"
          :scroll-x="USER_TABLE_SCROLL_X"
          remote
          striped
          size="small"
          @update:page="handleUserPageChange"
          @update:page-size="handleUserPageSizeChange"
        />
      </n-spin>
    </div>

    <template #footer>
      <n-space justify="end">
        <n-button @click="usersModalVisible = false">
          关闭
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { listTenantUsers, removeTenantUser } from '@/api/system/tenant'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'

const emit = defineEmits(['changed'])
const userStore = useUserStore()
const USER_TYPE_DICT = 'sys_user_type'
const USER_STATUS_DICT = 'sys_user_status'
/** 列宽合计，供表格横向滚动，避免撑破弹窗 */
const USER_TABLE_SCROLL_X = 1040
const usersModalVisible = ref(false)
const usersLoading = ref(false)
const currentTenant = ref({})
const tenantUsers = ref([])
const userSearchParams = ref({
  username: '',
  realName: '',
  phone: '',
  userStatus: null,
})
const userPagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
})

const { dict } = useDict(USER_TYPE_DICT, USER_STATUS_DICT)

const userStatusOptions = computed(() => toNumberOptions(dict.value[USER_STATUS_DICT]))
const userPaginationConfig = computed(() => ({
  page: userPagination.value.page,
  pageSize: userPagination.value.pageSize,
  itemCount: userPagination.value.itemCount,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100],
}))

const userTableColumns = [
  {
    title: '用户名',
    key: 'username',
    width: 140,
    ellipsis: { tooltip: true },
  },
  {
    title: '真实姓名',
    key: 'realName',
    width: 110,
    ellipsis: { tooltip: true },
  },
  {
    title: '用户类型',
    key: 'userType',
    width: 120,
    render: (row) => {
      return h(DictTag, { dictType: USER_TYPE_DICT, value: row.userType, size: 'small' })
    },
  },
  {
    title: '手机号',
    key: 'phone',
    width: 120,
  },
  {
    title: '邮箱',
    key: 'email',
    width: 180,
    ellipsis: { tooltip: true },
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
    title: '创建时间',
    key: 'createTime',
    width: 170,
  },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    fixed: 'right',
    render: (row) => {
      if (isCurrentTenantUser(row)) {
        return h('span', { class: 'text-disabled cursor-not-allowed' }, '当前登录用户')
      }
      return h(
        'a',
        {
          class: 'text-error cursor-pointer hover:text-error-hover',
          onClick: () => handleRemoveTenantUser(row),
        },
        '移出租户',
      )
    },
  },
]

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

function isCurrentTenantUser(row) {
  return String(row?.id) === String(userStore.userId)
    && String(currentTenant.value?.id) === String(userStore.userInfo?.tenantId)
}

function handleRemoveTenantUser(row) {
  if (!currentTenant.value?.id || !row?.id)
    return
  window.$dialog.warning({
    title: '确认移除',
    content: `确定将用户"${row.username}"移出租户"${currentTenant.value.tenantName}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await removeTenantUser(currentTenant.value.id, row.id)
        if (res.code === 200) {
          window.$message.success('用户已移出租户')
          await loadTenantUsers()
          emit('changed')
        }
      }
      catch (error) {
        console.error('移出租户用户失败:', error)
        window.$message.error(error?.message || '移出租户用户失败')
      }
    },
  })
}

async function open(row) {
  currentTenant.value = row
  usersModalVisible.value = true
  userSearchParams.value = {
    username: '',
    realName: '',
    phone: '',
    userStatus: null,
  }
  userPagination.value.page = 1
  await loadTenantUsers()
}

async function loadTenantUsers() {
  if (!currentTenant.value?.id)
    return
  try {
    usersLoading.value = true
    const params = {
      ...userSearchParams.value,
      pageNum: userPagination.value.page,
      pageSize: userPagination.value.pageSize,
    }
    Object.keys(params).forEach((key) => {
      if (params[key] === '' || params[key] === null || params[key] === undefined) {
        delete params[key]
      }
    })
    const res = await listTenantUsers(currentTenant.value.id, params)
    if (res.code === 200) {
      tenantUsers.value = res.data?.records || []
      userPagination.value.itemCount = res.data?.total || 0
    }
  }
  catch (error) {
    console.error('加载租户用户失败:', error)
    window.$message.error('加载租户用户失败')
  }
  finally {
    usersLoading.value = false
  }
}

function handleUserSearch() {
  userPagination.value.page = 1
  loadTenantUsers()
}

function handleUserSearchReset() {
  userSearchParams.value = {
    username: '',
    realName: '',
    phone: '',
    userStatus: null,
  }
  userPagination.value.page = 1
  loadTenantUsers()
}

function handleUserPageChange(page) {
  userPagination.value.page = page
  loadTenantUsers()
}

function handleUserPageSizeChange(pageSize) {
  userPagination.value.pageSize = pageSize
  userPagination.value.page = 1
  loadTenantUsers()
}

defineExpose({ open })
</script>

<style scoped>
.tenant-users-modal {
  display: grid;
  gap: 12px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 320px;
  overflow: hidden;
}

.tenant-users-search {
  box-sizing: border-box;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-secondary);
}

.tenant-users-filter-control {
  width: 140px;
}

.tenant-users-filter-status {
  width: 120px;
}

.tenant-users-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.tenant-users-modal :deep(.n-data-table) {
  width: 100%;
  max-width: 100%;
}

.tenant-users-modal :deep(.n-spin-container),
.tenant-users-modal :deep(.n-spin-content) {
  width: 100%;
  max-width: 100%;
  min-width: 0;
}
</style>

<style>
/* n-modal teleport 到 body，需非 scoped 约束卡片内容宽度 */
.tenant-users-dialog.n-card,
.tenant-users-dialog .n-card__content {
  max-width: 100%;
  overflow: hidden;
}
</style>
