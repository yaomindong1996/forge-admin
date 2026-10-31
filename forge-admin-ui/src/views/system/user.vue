<template>
  <div class="system-user-page">
    <!-- 左侧组织树 + 右侧用户列表布局 -->
    <MasterDetailWorkspace
      class="user-layout"
      persist-key="system-user"
      :collapsed="leftOrgPanelCollapsed"
      :aside-width="260"
      :collapsed-aside-width="72"
    >
      <!-- 左侧组织树面板 -->
      <template #aside>
        <UserOrganizationTree />
      </template>

      <!-- 右侧用户列表 -->
      <UserListPanel />
    </MasterDetailWorkspace>
    <UserPasswordModal />
    <UserRelationModal />
    <UserBatchAuthModal />
    <UserBatchTenantModal />
    <RolePermissionModal
      v-model:show="rolePermissionModalVisible"
      :role="currentPermissionRole"
      @saved="loadRoleList"
    />
  </div>
</template>

<script setup>
import { getActivePinia, storeToRefs } from 'pinia'
import { onBeforeUnmount, onMounted, watchEffect } from 'vue'
import MasterDetailWorkspace from '@/components/common/MasterDetailWorkspace.vue'
import { useDict } from '@/composables/useDict'
import { NORMAL_DISABLE_DICT, ROLE_DATA_SCOPE_DICT, ROLE_TYPE_DICT, USER_SEX_DICT, USER_STATUS_DICT, USER_TYPE_DICT } from '@/stores/system/user-management/utils'
import { useUserManagementStore } from '@/stores/system/userManagementStore'
import RolePermissionModal from './components/RolePermissionModal.vue'
import UserBatchAuthModal from './user/components/UserBatchAuthModal.vue'
import UserBatchTenantModal from './user/components/UserBatchTenantModal.vue'
import UserListPanel from './user/components/UserListPanel.vue'
import UserOrganizationTree from './user/components/UserOrganizationTree.vue'
import UserPasswordModal from './user/components/UserPasswordModal.vue'
import UserRelationModal from './user/components/UserRelationModal.vue'

defineOptions({ name: 'SystemUser' })

const pinia = getActivePinia()
const store = useUserManagementStore()
const { leftOrgPanelCollapsed, rolePermissionModalVisible, currentPermissionRole } = storeToRefs(store)
const { loadRoleList } = store
const { dict } = useDict(USER_TYPE_DICT, USER_STATUS_DICT, USER_SEX_DICT, ROLE_DATA_SCOPE_DICT, ROLE_TYPE_DICT, NORMAL_DISABLE_DICT)
watchEffect(() => store.setDictionaries(dict.value))

onMounted(() => {
  store.loadLeftOrgTree()
  store.loadTenantOptions()
  store.loadPostList()
})

onBeforeUnmount(() => {
  // 先停止领域 watcher 再清空草稿；旧异步请求只持有已释放实例。
  store.$dispose()
  store.resetWorkspace()
  delete pinia.state.value[store.$id]
})
</script>

<style src="./user/styles/layout.css"></style>

<style src="./user/styles/organization.css"></style>

<style src="./user/styles/workbench.css"></style>

<style src="./user/styles/overview.css"></style>

<style src="./user/styles/membership.css"></style>

<style src="./user/styles/roles.css"></style>
