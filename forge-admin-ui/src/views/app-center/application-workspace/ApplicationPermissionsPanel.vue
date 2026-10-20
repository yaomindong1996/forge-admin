<template>
  <div class="permissions-panel">
    <header class="panel-heading">
      <div class="heading-copy">
        <h2>应用角色授权</h2>
        <p>选择角色，在一个工作台内配置页面入口、对象数据范围和功能权限。</p>
      </div>

      <div class="heading-actions">
        <span v-if="dirty" class="dirty-state">
          <i class="i-material-symbols:edit-note" aria-hidden="true" />
          有未保存修改
        </span>
        <n-button
          type="primary"
          :loading="saving"
          :disabled="!selectedRoleId || roleGrantLoading || !dirty"
          @click="saveCurrentRole"
        >
          <template #icon>
            <n-icon><SaveOutline /></n-icon>
          </template>
          保存当前角色
        </n-button>
      </div>
    </header>

    <section class="role-toolbar" aria-label="当前授权角色">
      <div class="role-selector">
        <span class="role-selector-label">配置角色</span>
        <n-select
          :value="selectedRoleId"
          :options="roleOptions"
          :loading="catalogLoading"
          filterable
          :consistent-menu-width="false"
          placeholder="选择要配置的角色"
          @update:value="requestRoleSwitch"
        />
      </div>

      <template v-if="selectedRole && draft">
        <div class="role-identity">
          <span class="role-icon">
            <n-icon><ShieldCheckmarkOutline /></n-icon>
          </span>
          <div>
            <strong>{{ selectedRole.roleName }}</strong>
            <span>{{ selectedRole.roleKey || '未设置角色标识' }}</span>
          </div>
        </div>

        <div class="role-overview">
          <span>角色默认数据范围</span>
          <n-tag size="small" :bordered="false" type="info">
            {{ dataScopeLabel(selectedRole.defaultDataScope) }}
          </n-tag>
          <span class="role-overview-divider" />
          <span>{{ currentSummary.pages }} 个页面入口</span>
          <span>{{ currentSummary.actions }} 个功能权限</span>
          <span>{{ currentSummary.dataScopes }} 个对象范围覆盖</span>
        </div>
      </template>
    </section>

    <n-alert type="info" :bordered="false" class="scope-note">
      页面入口决定角色能否在应用门户看到该页，与「系统菜单挂载」无关（不必挂系统菜单也能授权）。对象数据范围和功能权限需单独勾选，不会互相隐式授权。
    </n-alert>

    <n-alert v-if="loadError" type="error" :bordered="false" class="load-alert">
      {{ loadError }}
      <template #action>
        <n-button text type="primary" @click="loadWorkspace">
          <template #icon>
            <n-icon><ReloadOutline /></n-icon>
          </template>
          重试
        </n-button>
      </template>
    </n-alert>

    <div class="permission-workbench-shell">
      <n-empty
        v-if="!catalogLoading && workspace.roles.length === 0"
        size="small"
        description="没有可维护角色"
        class="workspace-empty"
      />

      <n-empty
        v-else-if="!catalogLoading && !selectedRole"
        size="small"
        description="选择角色后配置应用权限"
        class="workspace-empty"
      />

      <RolePermissionSettings
        v-else
        v-model:checked-keys="checkedResourceKeys"
        v-model:data-scope-settings="roleDataScopeSettings"
        :permission-modules="permissionModules"
        :loading="catalogLoading || roleGrantLoading"
        :data-scope-loading="roleGrantLoading"
        :data-scope-options="manageableDataScopeOptions"
        :default-scope-editable="false"
        :link-page-and-actions="false"
        @aux-action="handleAuxAction"
      />
    </div>

    <ApplicationDataScopeAdapterModal
      v-model:show="adapterVisible"
      :object="adapterObject"
      :saving="adapterSaving"
      @save="saveDataScopeAdapter"
    />
  </div>
</template>

<script setup>
import {
  ReloadOutline,
  SaveOutline,
  ShieldCheckmarkOutline,
} from '@vicons/ionicons5'
import { useDialog, useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  businessApplicationPermissionWorkspace,
  businessApplicationRolePermission,
  saveBusinessApplicationDataScopeAdapter,
  saveBusinessApplicationRolePermission,
} from '@/api/business-application'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import RolePermissionSettings from '@/views/system/components/RolePermissionSettings.vue'
import {
  applyApplicationDataScopeSettings,
  buildApplicationDataScopeSettings,
  buildApplicationPermissionModules,
  buildRolePermissionPayload,
  cloneRolePermission,
  createLatestRequestGuard,
  rolePermissionEquals,
  summarizeRolePermission,
} from '../application-permission-utils'
import ApplicationDataScopeAdapterModal from './ApplicationDataScopeAdapterModal.vue'

const props = defineProps({
  application: {
    type: Object,
    default: null,
  },
  initialObjects: {
    type: Array,
    default: () => [],
  },
})

const ROLE_DATA_SCOPE_DICT = 'sys_role_data_scope'
const EMPTY_WORKSPACE = { roles: [], pages: [], objects: [] }

const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const { dict } = useDict(ROLE_DATA_SCOPE_DICT)

const workspace = ref({ ...EMPTY_WORKSPACE })
const catalogLoading = ref(false)
const roleGrantLoading = ref(false)
const saving = ref(false)
const loadError = ref('')
const selectedRoleId = ref(null)
const draft = ref(null)
const savedGrant = ref(null)
const roleGrantCache = ref({})
const adapterVisible = ref(false)
const adapterObject = ref(null)
const adapterSaving = ref(false)
const handledAdapterRequest = ref('')
const workspaceGuard = createLatestRequestGuard()
const roleGrantGuard = createLatestRequestGuard()

const applicationCode = computed(() => props.application?.applicationCode || route.params.applicationCode || '')
const selectedRole = computed(() => workspace.value.roles.find(
  role => String(role.roleId) === String(selectedRoleId.value),
) || null)
const roleOptions = computed(() => workspace.value.roles.map(role => ({
  label: `${role.roleName}${role.roleKey ? ` · ${role.roleKey}` : ''}`,
  value: String(role.roleId),
})))
const dataScopeOptions = computed(() => (dict.value[ROLE_DATA_SCOPE_DICT] || []).map(item => ({
  ...item,
  value: Number(item.value),
})))
const manageableDataScopeOptions = computed(() => {
  if (userStore.isAdmin)
    return dataScopeOptions.value
  const deniedScopes = Number(userStore.userType) === 2 ? [1, 2] : [1]
  return dataScopeOptions.value.filter(item => !deniedScopes.includes(Number(item.value)))
})
const dirty = computed(() => Boolean(
  draft.value && savedGrant.value && !rolePermissionEquals(draft.value, savedGrant.value),
))
const currentSummary = computed(() => summarizeRolePermission(workspace.value, draft.value || {}))
const permissionModules = computed(() => buildApplicationPermissionModules(workspace.value))
const checkedResourceKeys = computed({
  get: () => draft.value?.resourceIds || [],
  set: (resourceIds) => {
    if (draft.value)
      draft.value = { ...draft.value, resourceIds: [...resourceIds] }
  },
})
const roleDataScopeSettings = computed({
  get: () => buildApplicationDataScopeSettings(workspace.value, draft.value || {}),
  set: (settings) => {
    if (draft.value)
      draft.value = applyApplicationDataScopeSettings(draft.value, settings)
  },
})

watch(applicationCode, (code) => {
  if (code)
    loadWorkspace()
}, { immediate: true })

watch([
  () => route.query.dataScopeObjectId,
  () => workspace.value.objects,
], ([objectId, objects]) => {
  if (!objectId || !objects.length)
    return
  const requestToken = String(objectId)
  if (handledAdapterRequest.value === requestToken)
    return
  handledAdapterRequest.value = requestToken
  const query = { ...route.query }
  delete query.dataScopeObjectId
  router.replace({ path: route.path, query })
  handleAuxAction({ type: 'DATA_SCOPE_ADAPTER', objectId })
})

watch(() => route.query.dataScopeObjectId, (objectId) => {
  if (!objectId)
    handledAdapterRequest.value = ''
})

async function loadWorkspace() {
  const code = applicationCode.value
  if (!code)
    return
  const requestId = workspaceGuard.next()
  roleGrantGuard.next()
  catalogLoading.value = true
  loadError.value = ''
  try {
    const response = await businessApplicationPermissionWorkspace(code)
    if (!workspaceGuard.isCurrent(requestId))
      return
    workspace.value = {
      ...EMPTY_WORKSPACE,
      ...(unwrapResponse(response, '应用权限目录加载失败') || {}),
    }
    roleGrantCache.value = {}
    const firstRoleId = workspace.value.roles[0]?.roleId
    if (firstRoleId !== null && firstRoleId !== undefined)
      await loadRolePermission(firstRoleId)
    else
      clearRoleGrant()
  }
  catch (error) {
    if (workspaceGuard.isCurrent(requestId)) {
      workspace.value = { ...EMPTY_WORKSPACE }
      clearRoleGrant()
      loadError.value = error?.message || '应用权限目录加载失败'
    }
  }
  finally {
    if (workspaceGuard.isCurrent(requestId))
      catalogLoading.value = false
  }
}

function requestRoleSwitch(roleId) {
  if (String(roleId) === String(selectedRoleId.value))
    return
  if (!dirty.value) {
    loadRolePermission(roleId)
    return
  }
  dialog.warning({
    title: '切换角色',
    content: '当前角色有未保存修改，切换后将放弃这些修改。',
    positiveText: '放弃并切换',
    negativeText: '继续编辑',
    onPositiveClick: () => loadRolePermission(roleId),
  })
}

async function loadRolePermission(roleId) {
  const code = applicationCode.value
  if (!code || roleId === null || roleId === undefined)
    return
  const requestId = roleGrantGuard.next()
  const stableRoleId = String(roleId)
  selectedRoleId.value = stableRoleId
  roleGrantLoading.value = true
  loadError.value = ''
  const role = workspace.value.roles.find(item => String(item.roleId) === stableRoleId)
  draft.value = cloneRolePermission({
    roleId: stableRoleId,
    roleName: role?.roleName,
    roleKey: role?.roleKey,
    defaultDataScope: role?.defaultDataScope,
  })
  savedGrant.value = cloneRolePermission(draft.value)
  try {
    const response = await businessApplicationRolePermission(code, stableRoleId)
    if (!roleGrantGuard.isCurrent(requestId))
      return
    const grant = cloneRolePermission(unwrapResponse(response, '角色权限加载失败'))
    roleGrantCache.value = { ...roleGrantCache.value, [stableRoleId]: grant }
    draft.value = cloneRolePermission(grant)
    savedGrant.value = cloneRolePermission(grant)
  }
  catch (error) {
    if (roleGrantGuard.isCurrent(requestId)) {
      loadError.value = error?.message || '角色权限加载失败'
      message.error(loadError.value)
    }
  }
  finally {
    if (roleGrantGuard.isCurrent(requestId))
      roleGrantLoading.value = false
  }
}

async function saveCurrentRole() {
  if (!draft.value || !selectedRoleId.value || !dirty.value)
    return
  saving.value = true
  try {
    const response = await saveBusinessApplicationRolePermission(
      applicationCode.value,
      selectedRoleId.value,
      buildRolePermissionPayload(draft.value),
    )
    const grant = cloneRolePermission(unwrapResponse(response, '角色权限保存失败'))
    roleGrantCache.value = { ...roleGrantCache.value, [selectedRoleId.value]: grant }
    draft.value = cloneRolePermission(grant)
    savedGrant.value = cloneRolePermission(grant)
    message.success('应用权限已保存')
  }
  catch (error) {
    message.error(error?.message || '角色权限保存失败')
  }
  finally {
    saving.value = false
  }
}

function clearRoleGrant() {
  selectedRoleId.value = null
  draft.value = null
  savedGrant.value = null
}

function unwrapResponse(response, fallbackMessage) {
  if (response?.code !== 200)
    throw new Error(response?.message || fallbackMessage)
  return response.data
}

function dataScopeLabel(value) {
  return dataScopeOptions.value.find(item => Number(item.value) === Number(value))?.label || `范围 ${value ?? '-'}`
}

function handleAuxAction(payload) {
  if (payload?.type !== 'DATA_SCOPE_ADAPTER' || payload.objectId == null)
    return
  if (dirty.value) {
    message.warning('当前角色有未保存修改，请先保存或放弃修改后再配置数据范围适配。')
    return
  }
  const object = workspace.value.objects.find(
    item => String(item.objectId) === String(payload.objectId),
  )
  if (!object) {
    message.error('未找到当前应用绑定的业务对象')
    return
  }
  adapterObject.value = object
  adapterVisible.value = true
}

async function saveDataScopeAdapter(payload) {
  if (!adapterObject.value?.objectId)
    return
  if (dirty.value) {
    message.warning('当前角色有未保存修改，请先保存或放弃修改后再保存适配。')
    return
  }
  adapterSaving.value = true
  try {
    const response = await saveBusinessApplicationDataScopeAdapter(
      applicationCode.value,
      adapterObject.value.objectId,
      payload,
    )
    const updatedObject = unwrapResponse(response, '数据范围适配保存失败')
    workspace.value = {
      ...workspace.value,
      objects: workspace.value.objects.map(object => String(object.objectId) === String(updatedObject.objectId)
        ? updatedObject
        : object),
    }
    adapterObject.value = updatedObject
    adapterVisible.value = false
    if (selectedRoleId.value != null) {
      const nextCache = { ...roleGrantCache.value }
      delete nextCache[selectedRoleId.value]
      roleGrantCache.value = nextCache
      await loadRolePermission(selectedRoleId.value)
    }
    message.success('数据范围适配已保存')
  }
  catch (error) {
    message.error(error?.message || '数据范围适配保存失败')
  }
  finally {
    adapterSaving.value = false
  }
}
</script>

<style scoped>
.permissions-panel {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 12px;
}

.panel-heading {
  display: flex;
  min-height: 48px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.heading-copy h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
}

.heading-copy p {
  margin: 4px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.heading-actions,
.role-overview,
.role-identity,
.dirty-state {
  display: flex;
  align-items: center;
}

.heading-actions {
  flex: 0 0 auto;
  gap: 12px;
}

.dirty-state {
  gap: 4px;
  color: #d97706;
  font-size: 12px;
  font-weight: 600;
}

.dirty-state i {
  font-size: 16px;
}

.role-toolbar {
  display: flex;
  min-height: 64px;
  align-items: center;
  gap: 18px;
  padding: 10px 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
}

.role-selector {
  display: grid;
  flex: 0 0 280px;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 10px;
}

.role-selector-label {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.role-identity {
  min-width: 0;
  gap: 9px;
}

.role-icon {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border-radius: 7px;
  background: color-mix(in srgb, var(--primary-color) 12%, transparent);
  color: var(--primary-color);
}

.role-identity > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.role-identity strong {
  color: var(--text-primary);
  font-size: 14px;
}

.role-identity span:not(.role-icon) {
  color: var(--text-tertiary);
  font-size: 11px;
}

.role-overview {
  min-width: 0;
  flex: 1;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 12px;
}

.role-overview-divider {
  width: 1px;
  height: 16px;
  margin: 0 2px;
  background: var(--border-light);
}

.scope-note,
.load-alert {
  flex: 0 0 auto;
}

.permission-workbench-shell {
  display: flex;
  min-height: 580px;
  flex: 1;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary);
}

.permission-workbench-shell :deep(.role-permission-workbench) {
  width: 100%;
}

.workspace-empty {
  margin: auto;
}

@media (max-width: 980px) {
  .role-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .role-selector {
    flex-basis: auto;
  }

  .role-overview {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .panel-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .heading-actions {
    width: 100%;
    justify-content: space-between;
  }

  .role-selector {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
