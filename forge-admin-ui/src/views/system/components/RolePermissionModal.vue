<template>
  <n-modal
    v-model:show="visible"
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
          <n-button @click="visible = false">
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
        <n-button @click="visible = false">
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
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { request } from '@/utils'
import RolePermissionSettings from './RolePermissionSettings.vue'

defineOptions({ name: 'RolePermissionModal' })

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  role: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:show', 'saved'])

const USER_TYPE_DICT = 'sys_user_type'
const ROLE_DATA_SCOPE_DICT = 'sys_role_data_scope'
const ROLE_TYPE_DICT = 'sys_role_type'
const NORMAL_DISABLE_DICT = 'sys_normal_disable'
const YES_NO_DICT = 'sys_yes_no'

const userStore = useUserStore()
const { dict } = useDict(USER_TYPE_DICT, ROLE_DATA_SCOPE_DICT, ROLE_TYPE_DICT, NORMAL_DISABLE_DICT, YES_NO_DICT)

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value),
})

const currentRole = computed(() => props.role || {})
const authLoading = ref(false)
const authLoadFailed = ref(false)
const authSubmitLoading = ref(false)
const resourceTreeData = ref([])
const checkedResourceKeys = ref([])
const dataScopeLoading = ref(false)
const dataScopeLoadFailed = ref(false)
const dataScopeSettings = ref({ defaultDataScope: 5, modules: [] })
const clientList = ref([])
const currentAuthClientCode = ref('pc')

const manageableDataScopeOptions = computed(() => {
  const dataScopeOptions = toNumberOptions(dict.value[ROLE_DATA_SCOPE_DICT])
  if (userStore.isAdmin)
    return dataScopeOptions
  const deniedScopes = Number(userStore.userType) === 2 ? [1, 2] : [1]
  return dataScopeOptions.filter(item => !deniedScopes.includes(Number(item.value)))
})

const authClientTabs = computed(() => {
  if (clientList.value.length > 0)
    return clientList.value
  return [{ clientCode: 'pc', clientName: 'PC端' }]
})
const currentAuthClientName = computed(() => {
  const client = authClientTabs.value.find(item => item.clientCode === currentAuthClientCode.value)
  return client?.clientName || currentAuthClientCode.value || '-'
})

let loadSequence = 0

watch([() => props.show, () => props.role?.id], ([show, roleId]) => {
  if (show && roleId)
    refreshAuthData()
}, { immediate: true })

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

async function refreshAuthData() {
  const roleId = currentRole.value.id
  if (!roleId)
    return
  const sequence = ++loadSequence
  resourceTreeData.value = []
  checkedResourceKeys.value = []
  authLoadFailed.value = false
  dataScopeLoadFailed.value = false
  dataScopeSettings.value = {
    defaultDataScope: resolveFallbackDataScope(),
    modules: [],
  }

  await Promise.all([
    loadClientList(),
    loadRoleDataScopes(sequence),
  ])

  if (!authClientTabs.value.some(item => item.clientCode === currentAuthClientCode.value))
    currentAuthClientCode.value = authClientTabs.value[0]?.clientCode || 'pc'

  await loadAuthClientResources(sequence)
}

async function loadClientList() {
  try {
    const res = await request.get('/system/client/list')
    if (res.code === 200)
      clientList.value = res.data || []
  }
  catch (error) {
    console.error('加载客户端列表失败:', error)
  }
}

async function loadResourceTree() {
  try {
    const res = await request.get('/system/resource/assignable-tree', {
      params: { clientCode: currentAuthClientCode.value },
    })
    if (res.code === 200) {
      resourceTreeData.value = res.data || []
      return
    }
    throw new Error(res.message || '资源树响应异常')
  }
  catch (error) {
    authLoadFailed.value = true
    console.error('加载资源树失败:', error)
    window.$message.error('加载资源树失败')
  }
}

async function loadRoleResources(roleId) {
  try {
    const res = await request.get(`/system/role/${roleId}/resources`, {
      params: { clientCode: currentAuthClientCode.value, includeParents: true },
    })
    if (res.code === 200) {
      checkedResourceKeys.value = filterAssignableCheckedKeys(res.data, resourceTreeData.value)
      return
    }
    throw new Error(res.message || '角色资源响应异常')
  }
  catch (error) {
    authLoadFailed.value = true
    console.error('加载角色资源失败:', error)
    window.$message.error('加载角色资源失败')
  }
}

/**
 * 回填角色已授权资源时只保留授权树中真实存在的非目录资源：
 * 目录由后端按已选页面自动补齐，若把历史绑定中的“孤儿目录”（其下页面已全部取消）
 * 回填并原样全量提交，会让角色反复出现点开为空的菜单入口。
 * 资源树不可用时回退为原始列表，避免误清空回填数据。
 */
function filterAssignableCheckedKeys(rawIds, tree) {
  const list = Array.isArray(rawIds) ? rawIds : []
  if (!Array.isArray(tree) || tree.length === 0)
    return list
  const assignableIds = new Set()
  const walk = (nodes) => {
    for (const node of nodes || []) {
      if (Number(node.resourceType) !== 1)
        assignableIds.add(String(node.id))
      if (node.children && node.children.length > 0)
        walk(node.children)
    }
  }
  walk(tree)
  return list.filter(id => assignableIds.has(String(id)))
}

async function loadAuthClientResources(sequence) {
  authLoading.value = true
  authLoadFailed.value = false
  checkedResourceKeys.value = []
  resourceTreeData.value = []
  try {
    // 顺序执行：回填勾选依赖资源树剔除目录类资源
    await loadResourceTree()
    await loadRoleResources(currentRole.value.id)
  }
  finally {
    if (sequence === loadSequence)
      authLoading.value = false
  }
}

async function loadRoleDataScopes(sequence) {
  dataScopeLoading.value = true
  dataScopeLoadFailed.value = false
  try {
    const res = await request.get(`/system/role/${currentRole.value.id}/dataScopes`)
    if (res.code === 200) {
      applyRoleDataScopeSettings(res.data)
      return
    }
    dataScopeLoadFailed.value = true
    dataScopeSettings.value = createFallbackDataScopeSettings()
  }
  catch (error) {
    dataScopeLoadFailed.value = true
    dataScopeSettings.value = createFallbackDataScopeSettings()
    console.warn('当前后端未提供角色数据权限明细接口，已按角色数据范围降级展示:', error)
  }
  finally {
    if (sequence === loadSequence)
      dataScopeLoading.value = false
  }
}

async function handleAuthClientChange(clientCode) {
  currentAuthClientCode.value = clientCode
  await loadAuthClientResources(loadSequence)
}

async function handleSubmitAuth() {
  if (authLoading.value || dataScopeLoading.value || authLoadFailed.value || dataScopeLoadFailed.value) {
    window.$message.error('权限配置尚未完整加载，请关闭弹窗后重试')
    return
  }

  let functionPermissionSaved = false
  try {
    authSubmitLoading.value = true
    const resourceRes = await request.post(
      `/system/role/${currentRole.value.id}/resources`,
      checkedResourceKeys.value,
      { params: { clientCode: currentAuthClientCode.value } },
    )
    if (resourceRes.code !== 200)
      throw new Error(resourceRes.message || '功能权限保存失败')
    functionPermissionSaved = true

    const dataScopeRes = await request.post(`/system/role/${currentRole.value.id}/dataScopes`, {
      defaultDataScope: dataScopeSettings.value.defaultDataScope,
      moduleScopes: dataScopeSettings.value.modules.map(module => ({
        moduleCode: module.moduleCode,
        dataScope: module.dataScope,
      })),
    })
    if (dataScopeRes.code !== 200)
      throw new Error(dataScopeRes.message || '数据权限保存失败')
    applyRoleDataScopeSettings(dataScopeRes.data)

    window.$message.success('角色权限配置已保存')
    visible.value = false
    emit('saved')
  }
  catch (error) {
    console.error('角色权限配置保存失败:', error)
    const message = error?.message || '保存失败'
    window.$message.error(functionPermissionSaved
      ? `功能权限已保存，数据权限保存失败：${message}`
      : `功能权限保存失败：${message}`)
  }
  finally {
    authSubmitLoading.value = false
  }
}

function resolveFallbackDataScope() {
  return Number(currentRole.value?.dataScope) || 5
}

function createFallbackDataScopeSettings() {
  return {
    defaultDataScope: resolveFallbackDataScope(),
    modules: [],
  }
}

function normalizeRoleDataScopeSettings(settings = {}) {
  const defaultDataScope = Number(settings.defaultDataScope) || resolveFallbackDataScope()
  return {
    defaultDataScope,
    modules: (settings.modules || []).map(module => ({
      ...module,
      dataScope: module.dataScope == null ? null : Number(module.dataScope),
      effectiveDataScope: Number(module.effectiveDataScope ?? defaultDataScope) || defaultDataScope,
    })),
  }
}

function applyRoleDataScopeSettings(settings = {}) {
  dataScopeSettings.value = normalizeRoleDataScopeSettings(settings)
}
</script>

<style scoped>
.auth-modal-content {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  color: var(--text-primary);
  background: var(--bg-secondary);
}

.auth-workspace-header {
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 0 0 auto;
  gap: 16px;
  min-height: 56px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-primary);
}

.auth-header-main {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 12px;
}

.auth-breadcrumb {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 10px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
}

.auth-breadcrumb-divider {
  color: #cbd5e1;
  font-weight: 500;
}

.auth-role-badge,
.auth-client-badge {
  display: inline-flex;
  align-items: center;
  max-width: 260px;
  min-width: 0;
  gap: 6px;
  padding: 3px 9px;
  border: 1px solid #e0e7ff;
  border-radius: 6px;
  background: #eef2ff;
  color: #4338ca;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
}

.auth-role-badge {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auth-role-key {
  max-width: 260px;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auth-header-actions {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 10px;
}

.auth-client-tabs {
  flex: 0 0 auto;
  padding: 8px 16px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-primary);
}

.auth-client-tabs :deep(.n-tabs-nav) {
  max-width: 100%;
}

.auth-load-alert {
  flex: 0 0 auto;
  margin: 12px 16px 0;
}

.auth-floating-actions {
  position: fixed;
  right: 24px;
  bottom: 22px;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 10px;
  background: color-mix(in srgb, var(--bg-primary) 94%, transparent);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(10px);
}

:global(.role-permission-modal.n-card) {
  display: flex;
  flex-direction: column;
  width: 100vw;
  height: 100vh;
  max-width: 100vw;
  max-height: 100vh;
  border-radius: 0;
}

:global(.role-permission-modal .n-card-header) {
  display: none;
}

:global(.role-permission-modal .n-card-content) {
  flex: 1;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

:global(.role-permission-modal .n-card__content) {
  flex: 1;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

.dark .auth-modal-content {
  color: #e5e7eb;
  background: #0f172a;
}

.dark .auth-workspace-header,
.dark .auth-client-tabs {
  background: #111827;
  border-color: var(--border-light);
}

.dark .auth-breadcrumb {
  color: #f8fafc;
}

.dark .auth-role-badge,
.dark .auth-client-badge {
  background: #1e3a8a;
  border-color: #1d4ed8;
  color: #bfdbfe;
}

.dark .auth-role-key,
.dark .auth-breadcrumb-divider {
  color: var(--text-tertiary);
}

.dark .auth-floating-actions {
  border-color: rgba(51, 65, 85, 0.92);
  background: rgba(15, 23, 42, 0.92);
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.34);
}

@media (max-width: 760px) {
  .auth-workspace-header {
    align-items: stretch;
    flex-direction: column;
  }

  .auth-header-main,
  .auth-header-actions {
    width: 100%;
  }

  .auth-header-main {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  .auth-header-actions {
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .auth-floating-actions {
    right: 12px;
    bottom: 12px;
    left: 12px;
    justify-content: flex-end;
  }
}
</style>
