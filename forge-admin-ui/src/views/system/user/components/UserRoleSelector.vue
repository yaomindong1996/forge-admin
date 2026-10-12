<template>
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
        :scroll-x="800"
        remote
        striped
        size="small"
        @update:checked-row-keys="handleCheckedKeysChange"
        @update:page="handleRolePageChange"
        @update:page-size="handleRolePageSizeChange"
      />
    </n-spin>
  </div>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import { useUserManagementStore } from '@/stores/system/userManagementStore'

const store = useUserManagementStore()
const { authLoading, roleSearchKeyword, checkedRoleKeys, orderedRoleTableData, rolePaginationConfig, authRoleColumns } = storeToRefs(store)
const { handleCheckedKeysChange, handleRoleSearch, handleRolePageChange, handleRolePageSizeChange, handleCheckAll, handleUncheckAll } = store
</script>
