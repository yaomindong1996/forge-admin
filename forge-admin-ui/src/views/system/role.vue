<template>
  <div class="system-role-page">
    <MasterDetailWorkspace
      class="role-workspace"
      persist-key="system-role"
      :aside-width="260"
      main-width="minmax(0, 1fr)"
    >
      <template #aside>
        <div class="role-list-panel">
          <div class="role-selector-header">
            <div class="role-selector-title">
              <span class="role-selector-icon">
                <i class="i-material-symbols:shield-outline-rounded" />
              </span>
              <span class="role-selector-copy">
                <strong>角色管理</strong>
                <small>{{ roleList.length }} 个角色</small>
              </span>
            </div>
            <n-button size="small" type="primary" circle title="新增角色" @click="handleAddRole">
              <template #icon>
                <i class="i-material-symbols:add-rounded" />
              </template>
            </n-button>
          </div>

          <div class="role-selector-tools">
            <n-input
              v-model:value="roleKeyword"
              class="role-search"
              clearable
              size="small"
              placeholder="搜索角色名称..."
              @clear="handleRoleSearch"
              @keyup.enter="handleRoleSearch"
            >
              <template #prefix>
                <i class="i-material-symbols:search-rounded" />
              </template>
            </n-input>
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
            </div>
          </div>

          <n-spin :show="roleListLoading" class="role-list-spin">
            <div class="role-list">
              <div
                v-for="role in roleList"
                :key="role.id"
                class="role-list-item"
                :class="{ 'is-selected': currentRole.id === role.id }"
                role="button"
                tabindex="0"
                @click="handleSelectRole(role)"
                @keydown.enter.prevent="handleSelectRole(role)"
                @keydown.space.prevent="handleSelectRole(role)"
              >
                <span class="role-list-main">
                  <span class="role-list-title">
                    <strong :title="role.roleName">{{ role.roleName }}</strong>
                    <span v-if="isRoleDisabled(role)" class="role-disabled-badge">
                      {{ resolveRoleStatusLabel(role) }}
                    </span>
                  </span>
                  <span class="role-card-meta">
                    <span>{{ resolveRoleDataScopeLabel(role) }}</span>
                  </span>
                </span>
                <span class="role-list-side" @click.stop>
                  <n-dropdown
                    trigger="click"
                    placement="bottom-end"
                    :menu-props="getRoleDropdownMenuProps"
                    :options="getRoleActionOptions(role)"
                    @select="key => handleRoleCardAction(key, role)"
                  >
                    <button type="button" class="role-card-menu" title="角色操作" aria-label="角色操作" @click.stop>
                      <i class="i-material-symbols:more-vert" />
                    </button>
                  </n-dropdown>
                </span>
              </div>
              <n-empty v-if="!roleListLoading && roleList.length === 0" description="暂无角色" size="small" />
            </div>
          </n-spin>
        </div>
      </template>

      <section class="role-user-panel">
        <header class="role-user-header">
          <div class="role-user-title">
            <div class="role-user-heading">
              <h2>{{ currentRole.roleName || '请选择角色' }}</h2>
              <small v-if="currentRole.roleKey">{{ currentRole.roleKey }}</small>
            </div>
            <div v-if="currentRole.id" class="role-user-badges">
              <NTag size="small" type="info" :bordered="false">
                {{ currentRoleDataScopeLabel }}
              </NTag>
              <NTag size="small" :type="currentRoleScopeTagType" :bordered="false">
                {{ currentRoleScopeLabel }}
              </NTag>
              <NTag size="small" :bordered="false">
                共 {{ roleUserTotal }} 名成员
              </NTag>
            </div>
          </div>
          <n-space size="small">
            <n-button
              size="small"
              type="primary"
              :disabled="!canAddUserToCurrentRole"
              @click="handleAddUser"
            >
              <template #icon>
                <i class="i-material-symbols:person-add-rounded" />
              </template>
              {{ addUserButtonText }}
            </n-button>
            <n-button
              size="small"
              quaternary
              circle
              title="刷新"
              :disabled="!currentRole.id"
              @click="refreshRoleUsers"
            >
              <template #icon>
                <i class="i-material-symbols:refresh-rounded" />
              </template>
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
            placeholder="选择授权组织"
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
          <div class="role-user-search-actions">
            <n-button class="role-user-search-action" size="small" type="primary" :disabled="!currentRole.id" @click="handleUserSearch">
              查询
            </n-button>
            <n-button class="role-user-search-action" size="small" :disabled="!currentRole.id" @click="handleUserSearchReset">
              重置
            </n-button>
          </div>
        </div>

        <div class="member-body">
          <AiCrudPage
            v-if="currentRole.id"
            ref="roleUserCrudRef"
            api="/system/role"
            :api-config="roleUserApiConfig"
            :search-schema="[]"
            :columns="roleUserTableColumns"
            :before-load-list="beforeLoadRoleUserList"
            row-key="id"
            :hide-add="true"
            :hide-batch-delete="true"
            :hide-selection="true"
            :show-search="false"
            :show-render-mode-switch="false"
            :page-size="10"
            :scroll-x="760"
            :table-props="{
              dragScroll: false,
              showToolbar: false,
              showRefresh: false,
              showDensity: false,
              showColumnFilter: false,
            }"
            @load-list-success="handleRoleUserLoadSuccess"
          />
        </div>
      </section>
    </MasterDetailWorkspace>

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

    <!-- 角色权限配置弹窗 -->
    <n-modal
      v-model:show="authModalVisible"
      preset="card"
      class="role-permission-modal"
      style="width: 100vw; max-width: 100vw"
      :mask-closable="false"
    >
      <div class="auth-modal-content">
        <header class="auth-workspace-header">
          <div class="auth-header-main">
            <div class="auth-breadcrumb">
              <span>角色综合授权</span>
              <span class="auth-breadcrumb-divider">/</span>
              <span class="auth-role-badge">
                <i class="i-material-symbols:business-center" />
                {{ currentRole.roleName || '-' }}
              </span>
            </div>
            <div class="auth-role-key">
              {{ currentRole.roleKey || '-' }}
            </div>
          </div>

          <div class="auth-header-actions">
            <span class="auth-client-badge">{{ currentAuthClientName }}</span>
            <n-button @click="authModalVisible = false">
              取消
            </n-button>
            <n-button
              type="primary"
              :loading="authSubmitLoading"
              :disabled="authLoading || dataScopeLoading || authLoadFailed || dataScopeLoadFailed"
              @click="handleSubmitAuth"
            >
              保存配置
            </n-button>
          </div>
        </header>

        <div v-if="authClientTabs.length > 1" class="auth-client-tabs">
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

        <n-alert v-if="authLoadFailed || dataScopeLoadFailed" type="error" :show-icon="false" class="auth-load-alert">
          权限配置加载不完整，请关闭弹窗后重试
        </n-alert>

        <RolePermissionSettings
          v-model:checked-keys="checkedResourceKeys"
          v-model:data-scope-settings="dataScopeSettings"
          :resource-tree="resourceTreeData"
          :loading="authLoading"
          :data-scope-loading="dataScopeLoading"
          :data-scope-options="manageableDataScopeOptions"
          :link-page-and-actions="false"
        />

        <div class="auth-floating-actions">
          <n-button @click="authModalVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="authSubmitLoading"
            :disabled="authLoading || dataScopeLoading || authLoadFailed || dataScopeLoadFailed"
            @click="handleSubmitAuth"
          >
            保存配置
          </n-button>
        </div>
      </div>
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

<script>
import { rolePageLocalComponents } from './rolePageLocalComponents'
import { useRolePage } from './composables/useRolePage'

export default {
  name: 'RolePage',
  components: {
    ...rolePageLocalComponents,
  },
  setup() {
    return useRolePage()
  },
}
</script>

<style scoped src="./rolePage.css"></style>
