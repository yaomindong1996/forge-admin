<template>
  <!-- 高级关系维护：标准卡片弹窗 + 顶部分页，与批量授权等弹窗风格一致 -->
  <n-modal
    v-model:show="relationModalVisible"
    title="高级关系维护"
    preset="card"
    class="user-relation-modal"
    :style="{ width: 'min(960px, calc(100vw - 32px))' }"
    :mask-closable="false"
  >
    <div class="relation-modal-body">
      <div class="relation-user-line">
        <span class="relation-user-avatar" aria-hidden="true">
          {{ (currentUser.username || currentUser.realName || '用').slice(0, 1).toUpperCase() }}
        </span>
        <div class="relation-user-copy">
          <strong>{{ currentUser.realName || currentUser.username || '-' }}</strong>
          <small>
            {{ currentUser.username ? `@${currentUser.username}` : '仅用于多组织 / 多租户等复杂维护' }}
          </small>
        </div>
      </div>

      <n-tabs
        class="relation-tabs"
        type="line"
        :value="relationActiveTab"
        @update:value="handleRelationTabChange"
      >
        <n-tab-pane
          v-for="tab in relationTabItems"
          :key="tab.key"
          :name="tab.key"
          :tab="tab.label"
        />
      </n-tabs>

      <p class="relation-tab-desc">
        {{ currentRelationTabMeta?.desc || '只处理复杂组织、角色、岗位和租户关系' }}
      </p>

      <n-alert type="info" :bordered="false" class="batch-action-alert">
        普通新增/编辑已经可以一次性设置组织、角色和岗位。这里仅用于多组织、多租户、历史数据修正等高级维护场景。
      </n-alert>

      <div class="relation-content-body">
        <UserRelationOverview />
        <UserRelationRoles />
        <UserRelationOrganizations />
        <UserRelationPosts />
        <UserRelationTenants />
      </div>
    </div>

    <template #footer>
      <n-space justify="end">
        <n-button @click="relationModalVisible = false">
          {{ relationActiveTab === 'overview' ? '关闭' : '取消' }}
        </n-button>
        <n-button
          v-if="relationActiveTab !== 'overview'"
          type="primary"
          :loading="relationSubmitLoading"
          @click="handleRelationSubmit"
        >
          保存更改
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import { useUserManagementStore } from '@/stores/system/userManagementStore'

import UserRelationOrganizations from './UserRelationOrganizations.vue'
import UserRelationOverview from './UserRelationOverview.vue'
import UserRelationPosts from './UserRelationPosts.vue'
import UserRelationRoles from './UserRelationRoles.vue'
import UserRelationTenants from './UserRelationTenants.vue'

const store = useUserManagementStore()
const {
  relationModalVisible,
  relationActiveTab,
  relationSubmitLoading,
  currentUser,
  relationTabItems,
  currentRelationTabMeta,
} = storeToRefs(store)
const { handleRelationTabChange, handleRelationSubmit } = store
</script>

<style scoped>
.relation-modal-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
}

.relation-user-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.relation-user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--primary-color, #2f6fed) 12%, transparent);
  color: var(--primary-color, #2f6fed);
  font-size: 13px;
  font-weight: 650;
}

.relation-user-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 2px;
}

.relation-user-copy strong {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 650;
  line-height: 1.3;
}

.relation-user-copy small {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.3;
}

.relation-tabs {
  margin-top: 2px;
}

.relation-tab-desc {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}

.relation-content-body {
  display: flex;
  flex-direction: column;
  min-height: 360px;
  max-height: min(52vh, 520px);
  overflow: auto;
}
</style>
