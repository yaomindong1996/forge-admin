<template>
  <section class="user-list-panel">
    <AiCrudPage
      biz-form-object-type="USER"
      :ref="store.attachList"
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
      :edit-grid-cols="24"
      edit-label-placement="top"
      edit-label-align="left"
      :edit-x-gap="16"
      :edit-y-gap="8"
      edit-form-class="system-edit-form user-edit-form"
      edit-size="small"
      modal-width="1080px"
      add-button-text="新增用户"
      :show-import="true"
      import-api="/system/user/import"
      import-template-url="/api/excel/template/sys_user_import"
      :show-export="true"
      export-button-text="导出用户"
      export-file-name="用户列表.xlsx"
      @selection-change="handleUserSelectionChange"
    >
      <template #form-avatar="{ value, formData, updateValue }">
        <UserAvatarField
          :model-value="value || formData?.avatar || ''"
          :name-hint="formData?.realName || formData?.username || ''"
          @update:model-value="next => updateValue?.(next)"
        />
      </template>
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
</template>

<script setup>
import { NTag } from 'naive-ui'
import { storeToRefs } from 'pinia'
import { AiCrudPage } from '@/components/ai-form'
import { useUserManagementStore } from '@/stores/system/userManagementStore'
import UserAvatarField from './UserAvatarField.vue'

const store = useUserManagementStore()
const { selectedUserIds, selectedOrgNode, isShowAllUsers, searchSchema, tableColumns, editSchema, userBatchActionOptions } = storeToRefs(store)
const { beforeRenderUserForm, beforeRenderUserDetail, beforeSubmit, handleClearOrgFilter, beforeLoadList, beforeSearch, handleUserSelectionChange, handleUserBatchActionSelect } = store
</script>
