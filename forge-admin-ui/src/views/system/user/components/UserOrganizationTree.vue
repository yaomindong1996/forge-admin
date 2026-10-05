<template>
  <div class="org-tree-panel" :class="{ 'is-collapsed': leftOrgPanelCollapsed }">
    <div class="org-tree-header">
      <div class="header-title">
        <div class="header-icon">
          <i class="i-lucide:building-2" />
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
          <i class="i-lucide:users" />
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
      <i class="i-lucide:building-2" />
      <span>组织筛选</span>
    </div>
  </div>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import PremiumTree from '@/components/common/PremiumTree.vue'
import { getLeftOrgNodeIcon, getLeftOrgNodeTone } from '@/stores/system/user-management/utils'

import { useUserManagementStore } from '@/stores/system/userManagementStore'

const store = useUserManagementStore()
const { leftOrgTreeData, leftOrgTreeLoading, leftOrgExpandAll, leftOrgExpandedKeys, leftOrgPanelCollapsed, selectedOrgKeys, selectedOrgNode, isShowAllUsers, orgTreeSummaryText } = storeToRefs(store)
const { handleOrgNodeSelect, handleLeftOrgExpandedKeysChange, toggleOrgExpandAll, toggleLeftOrgPanel, handleSelectAllUsers } = store
</script>
