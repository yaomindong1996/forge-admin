<template>
  <div class="role-permission-workbench">
    <aside class="permission-sidebar" aria-label="业务模块导航">
      <div class="sidebar-search">
        <n-input
          v-model:value="keyword"
          clearable
          placeholder="搜索业务模块或权限"
          aria-label="搜索业务模块或权限"
          size="small"
        >
          <template #prefix>
            <i class="i-material-symbols:search" aria-hidden="true" />
          </template>
        </n-input>

        <n-dropdown
          trigger="click"
          placement="bottom-start"
          :options="globalBatchOptions"
          @select="handleGlobalBatchSelect"
        >
          <n-button
            size="small"
            secondary
            :disabled="loading || workspaceModules.length === 0"
            class="batch-menu-button is-sidebar-batch"
          >
            <template #icon>
              <i class="i-material-symbols:admin-panel-settings" />
            </template>
            <span>全局授权</span>
            <i class="i-material-symbols:keyboard-arrow-down batch-menu-chevron" aria-hidden="true" />
          </n-button>
        </n-dropdown>
      </div>

      <div class="module-nav-list">
        <div v-if="loading" class="module-skeleton-list" aria-label="业务模块加载中">
          <div v-for="index in 8" :key="`module-skeleton-${index}`" class="module-skeleton-item">
            <n-skeleton circle size="small" />
            <n-skeleton text :width="index % 3 === 0 ? '58%' : '72%'" />
            <n-skeleton circle size="small" class="module-skeleton-status" />
          </div>
        </div>

        <template v-else>
          <button
            v-for="module in filteredNavigationModules"
            :key="module.key"
            type="button"
            class="module-nav-item"
            :class="{ 'is-active': activeModuleKey === module.key }"
            @click="activeModuleKey = module.key"
          >
            <span class="module-nav-main">
              <i :class="moduleIconClass(module)" aria-hidden="true" />
              <span>{{ module.name }}</span>
            </span>
            <span
              class="module-nav-status"
              :class="`is-${moduleStatus(module)}`"
              :title="moduleStatusLabel(moduleStatus(module))"
            >
              <i :class="moduleStatusIcon(moduleStatus(module))" aria-hidden="true" />
            </span>
          </button>

          <n-empty
            v-if="filteredNavigationModules.length === 0"
            description="没有匹配的业务模块"
            size="small"
          />
        </template>
      </div>

      <div class="sidebar-legend">
        <span><i class="i-material-symbols:check-circle" />已满配</span>
        <span><i class="i-material-symbols:do-not-disturb-on" />部分配</span>
        <span><i class="i-material-symbols:radio-button-unchecked" />未配置</span>
      </div>
    </aside>

    <main class="permission-main">
      <div class="permission-main-toolbar">
        <div class="toolbar-title">
          <strong>{{ activeModule?.name || '业务权限' }}</strong>
          <span>{{ selectedActionCount }} / {{ totalActionCount }} 个功能点已授权</span>
        </div>

        <div class="toolbar-controls">
          <div
            v-if="linkPageAndActions"
            class="cascade-switch"
            title="开启后，勾选菜单会自动全选下方操作，取消菜单会清空下方操作"
          >
            <n-switch v-model:value="cascadeEnabled" size="small" aria-label="父子联动" />
            <span>{{ cascadeEnabled ? '父子联动开' : '父子联动关' }}</span>
          </div>

          <div v-else class="cascade-switch is-independent" title="页面入口和功能权限分别授权">
            <i class="i-material-symbols:call-split" aria-hidden="true" />
            <span>入口与功能独立授权</span>
          </div>

          <div class="toolbar-divider" />

          <div class="collapse-pill-control" role="group" aria-label="展开折叠控制">
            <button type="button" :disabled="activeDetailPages.length === 0" @click="toggleCollapseAll(false)">
              <i class="i-material-symbols:unfold-more" aria-hidden="true" />
              展开
            </button>
            <button type="button" :disabled="activeDetailPages.length === 0" @click="toggleCollapseAll(true)">
              <i class="i-material-symbols:unfold-less" aria-hidden="true" />
              折叠
            </button>
          </div>

          <div class="toolbar-divider" />

          <n-dropdown
            v-if="defaultScopeEditable"
            trigger="click"
            placement="bottom-end"
            :options="defaultScopeDropdownOptions"
            :disabled="dataScopeLoading"
            @select="updateDefaultScope"
          >
            <button
              type="button"
              class="default-scope-button"
              :disabled="dataScopeLoading"
              title="页面选择跟随角色默认时，会使用这里的角色默认数据范围"
              aria-label="角色默认数据范围"
            >
              <span class="default-scope-copy">
                <span class="default-scope-eyebrow">角色默认：</span>
                <strong>
                  <i :class="scopeIconClass(dataScopeSettings.defaultDataScope)" aria-hidden="true" />
                  {{ currentDefaultScopeLabel }}
                </strong>
              </span>
              <i class="i-material-symbols:keyboard-arrow-down default-scope-chevron" aria-hidden="true" />
            </button>
          </n-dropdown>

          <div v-else class="default-scope-button is-readonly" title="应用工作台不修改角色的全局默认数据范围">
            <span class="default-scope-copy">
              <span class="default-scope-eyebrow">角色默认：</span>
              <strong>
                <i :class="scopeIconClass(dataScopeSettings.defaultDataScope)" aria-hidden="true" />
                {{ currentDefaultScopeLabel }}
              </strong>
            </span>
            <i class="i-material-symbols:lock-outline default-scope-lock" aria-hidden="true" />
          </div>

          <n-dropdown
            trigger="click"
            placement="bottom-end"
            :options="moduleBatchOptions"
            @select="handleModuleBatchSelect"
          >
            <n-button size="small" :disabled="loading || activePages.length === 0" class="batch-menu-button">
              <template #icon>
                <i class="i-material-symbols:rule-settings" />
              </template>
              <span>当前模块操作</span>
              <i class="i-material-symbols:keyboard-arrow-down batch-menu-chevron" aria-hidden="true" />
            </n-button>
          </n-dropdown>
        </div>
      </div>

      <div class="permission-spin">
        <div
          v-if="loading || dataScopeLoading"
          class="page-card-list is-skeleton"
          role="region"
          aria-label="页面权限加载中"
        >
          <section
            v-for="index in 4"
            :key="`page-skeleton-${index}`"
            class="permission-page-card skeleton-page-card"
          >
            <header class="page-card-header">
              <div class="page-card-title skeleton-page-title">
                <n-skeleton circle :width="26" :height="26" />
                <n-skeleton text :width="index % 2 === 0 ? '148px' : '184px'" />
                <n-skeleton text width="66px" />
              </div>

              <div class="page-card-actions skeleton-page-actions">
                <n-skeleton text width="86px" />
                <n-skeleton text width="58px" />
                <n-skeleton text width="74px" />
                <n-skeleton text width="74px" />
              </div>
            </header>

            <div class="page-card-body">
              <section class="data-scope-panel" aria-label="数据权限范围加载中">
                <div class="section-heading">
                  <n-skeleton circle :width="16" :height="16" />
                  <n-skeleton text width="92px" />
                  <n-skeleton text width="120px" />
                </div>

                <div class="scope-option-grid">
                  <n-skeleton v-for="scopeIndex in 4" :key="`scope-skeleton-${scopeIndex}`" :height="40" />
                </div>
              </section>

              <section class="function-panel" aria-label="业务功能权限加载中">
                <div class="section-heading">
                  <n-skeleton circle :width="16" :height="16" />
                  <n-skeleton text width="92px" />
                  <n-skeleton text width="130px" />
                </div>

                <div class="function-skeleton-grid">
                  <n-skeleton
                    v-for="actionIndex in 10"
                    :key="`action-skeleton-${actionIndex}`"
                    text
                    :width="actionIndex % 3 === 0 ? '74px' : '58px'"
                  />
                </div>
              </section>
            </div>
          </section>
        </div>

        <div v-else class="page-card-list" role="region" aria-label="页面权限配置">
          <section
            v-for="page in activePages"
            :key="page.key"
            class="permission-page-card"
          >
            <header class="page-card-header" :class="{ 'is-compact': !page.hasDetails }">
              <div class="page-card-title">
                <button
                  v-if="page.hasDetails"
                  type="button"
                  class="collapse-button"
                  :aria-expanded="!isCollapsed(page.key)"
                  @click="togglePageCollapse(page.key)"
                >
                  <i :class="isCollapsed(page.key) ? 'i-material-symbols:chevron-right' : 'i-material-symbols:keyboard-arrow-down'" />
                </button>

                <n-checkbox
                  v-if="page.accessItem"
                  :checked="isPageAccessChecked(page)"
                  :indeterminate="isPagePartiallyChecked(page)"
                  @update:checked="checked => togglePageAccess(page, checked)"
                >
                  <span class="page-name">{{ page.name }}</span>
                </n-checkbox>

                <span v-else class="page-name">{{ page.name }}</span>

                <span v-if="page.accessItem" class="page-entry-tag">
                  <i class="i-material-symbols:dashboard-customize" />
                  页面入口
                </span>

                <span v-else-if="page.accessUnavailableLabel" class="page-entry-tag is-warning">
                  <i class="i-material-symbols:pending-actions" />
                  {{ page.accessUnavailableLabel }}
                </span>

                <span v-else-if="page.objectPermission" class="page-entry-tag is-object">
                  <i class="i-material-symbols:database" />
                  对象权限
                </span>
              </div>

              <div class="page-card-actions">
                <span v-if="page.actionItems.length" class="page-action-count">
                  已选功能 <strong>{{ selectedActionCountInPage(page) }}</strong> / {{ configurableActions(page).length }}
                </span>
                <n-button
                  v-if="page.accessItem"
                  size="tiny"
                  :disabled="!page.accessItem || !hasAnyActionChecked(page)"
                  @click="selectPageAccessOnly(page)"
                >
                  <template #icon>
                    <i class="i-material-symbols:widgets" />
                  </template>
                  仅菜单
                </n-button>
                <n-button
                  v-if="page.actionItems.length"
                  size="tiny"
                  :disabled="configurableActions(page).length === 0 || areAllActionsChecked(page)"
                  @click="togglePageAllActions(page, true)"
                >
                  <template #icon>
                    <i class="i-material-symbols:checklist" />
                  </template>
                  全选功能
                </n-button>
                <n-button
                  v-if="page.actionItems.length"
                  size="tiny"
                  :disabled="selectedActionCountInPage(page) === 0"
                  @click="togglePageAllActions(page, false)"
                >
                  <template #icon>
                    <i class="i-material-symbols:playlist-remove" />
                  </template>
                  清空功能
                </n-button>
                <n-button
                  v-if="page.auxActionLabel && page.dataScopeModule"
                  text
                  type="primary"
                  size="small"
                  @click="emit('auxAction', page.auxActionPayload)"
                >
                  <template #icon>
                    <i class="i-material-symbols:settings" />
                  </template>
                  {{ page.auxActionLabel }}
                </n-button>
              </div>
            </header>

            <div
              v-if="page.hasDetails"
              v-show="!isCollapsed(page.key)"
              class="page-card-body"
              :class="{ 'is-single': !page.showDataScopePanel || !page.showFunctionPanel }"
            >
              <section v-if="page.showDataScopePanel" class="data-scope-panel" aria-label="数据权限范围">
                <div class="section-heading">
                  <i class="i-material-symbols:database" aria-hidden="true" />
                  <span>数据权限范围</span>
                  <small>该页面能看哪些数据</small>
                </div>

                <div v-if="page.dataScopeModule" class="scope-option-grid">
                  <button
                    v-for="option in pageScopeOptions"
                    :key="String(option.value)"
                    type="button"
                    class="scope-option"
                    :class="{ 'is-selected': isScopeSelected(page.dataScopeModule, option.value) }"
                    @click="updateModuleScope(page.dataScopeModule.moduleCode, option.value)"
                  >
                    <span class="scope-radio">
                      <span />
                    </span>
                    <i :class="scopeIconClass(option.value)" aria-hidden="true" />
                    <span>{{ option.label }}</span>
                  </button>
                </div>
                <p v-if="showsFlowRelatedHint(page.dataScopeModule)" class="flow-related-hint">
                  此对象已开启流程经手可见：除上面范围外，还能看到自己发起、审批、被抄送的单据（只读）。待办仍在流程工作台处理。
                </p>

                <div v-else class="scope-unavailable is-warning">
                  <span>{{ page.dataScopeUnavailableText || '当前页面未接入数据权限配置' }}</span>
                  <n-button
                    v-if="page.auxActionLabel"
                    text
                    type="primary"
                    size="small"
                    @click="emit('auxAction', page.auxActionPayload)"
                  >
                    {{ page.auxActionLabel }}
                  </n-button>
                </div>
              </section>

              <section v-if="page.showFunctionPanel" class="function-panel" aria-label="业务功能权限">
                <div class="section-heading">
                  <i class="i-material-symbols:tune" aria-hidden="true" />
                  <span>业务功能权限</span>
                  <small>该页面能执行哪些操作</small>
                </div>

                <div v-if="page.actionItems.length > 0" class="function-section-list">
                  <div
                    v-for="section in permissionSections(page)"
                    :key="section.key"
                    class="function-section"
                    :class="`is-${section.key}`"
                  >
                    <div class="function-section-heading">
                      <span>{{ section.label }}</span>
                      <small>{{ section.items.length }} 项</small>
                    </div>

                    <div class="function-grid">
                      <n-checkbox
                        v-for="permission in section.items"
                        :key="permission.key"
                        :checked="isPermissionChecked(permission)"
                        :indeterminate="isPermissionPartiallyChecked(permission)"
                        :disabled="permission.disabled || permission.resourceIds.length === 0"
                        @update:checked="checked => toggleAction(page, permission, checked)"
                      >
                        <span class="function-permission-label">
                          <span class="function-permission-name">{{ permission.label }}</span>
                          <span class="function-source-tags" aria-label="权限来源">
                            <span
                              v-for="source in permission.sources"
                              :key="source.type"
                              class="function-source-tag"
                              :class="`is-${source.kind}`"
                            >
                              {{ source.label }}
                            </span>
                          </span>
                        </span>
                      </n-checkbox>
                    </div>
                  </div>
                </div>

                <n-empty
                  v-else
                  description="该页面暂无可配置功能"
                  size="small"
                  class="function-empty"
                />
              </section>
            </div>
          </section>

          <n-empty
            v-if="!loading && activePages.length === 0"
            description="当前模块暂无可配置页面或功能"
            size="small"
            class="workspace-empty"
          />
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, h, ref, watch } from 'vue'

defineOptions({ name: 'RolePermissionSettings' })

const props = defineProps({
  resourceTree: {
    type: Array,
    default: () => [],
  },
  permissionModules: {
    type: Array,
    default: () => [],
  },
  checkedKeys: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  dataScopeSettings: {
    type: Object,
    default: () => ({ defaultDataScope: 5, modules: [] }),
  },
  dataScopeOptions: {
    type: Array,
    default: () => [],
  },
  dataScopeLoading: {
    type: Boolean,
    default: false,
  },
  defaultScopeEditable: {
    type: Boolean,
    default: true,
  },
  linkPageAndActions: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:checkedKeys', 'update:dataScopeSettings', 'auxAction'])

const INHERIT_SCOPE = '__inherit__'
const keyword = ref('')
const activeModuleKey = ref('')
const cascadeEnabled = ref(true)
const collapsedPageKeys = ref(new Set())

const checkedKeySet = computed(() => new Set(props.checkedKeys.map(String)))
const dataScopeModuleMap = computed(() => new Map((props.dataScopeSettings.modules || [])
  .map(module => [module.moduleCode, module])))
const workspaceModules = computed(() => normalizePermissionModules(
  props.permissionModules.length
    ? props.permissionModules
    : buildWorkspaceModules(props.resourceTree),
))
const totalActionCount = computed(() => workspaceModules.value.reduce((total, module) =>
  total + module.pages.reduce((pageTotal, page) => pageTotal + configurableActions(page).length, 0), 0))
const selectedActionCount = computed(() => workspaceModules.value.reduce((total, module) =>
  total + module.pages.reduce((pageTotal, page) => pageTotal + selectedActionCountInPage(page), 0), 0))

const filteredNavigationModules = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  if (!search)
    return workspaceModules.value
  return workspaceModules.value.filter(module => module.name.toLowerCase().includes(search)
    || module.path.toLowerCase().includes(search)
    || module.pages.some(page => page.name.toLowerCase().includes(search)
      || page.path.toLowerCase().includes(search)
      || page.actionItems.some(item => item.label.toLowerCase().includes(search))))
})

const activeModule = computed(() => filteredNavigationModules.value.find(module => module.key === activeModuleKey.value)
  || filteredNavigationModules.value[0]
  || null)
const activePages = computed(() => {
  if (!activeModule.value)
    return []
  const search = keyword.value.trim().toLowerCase()
  if (!search || activeModule.value.name.toLowerCase().includes(search))
    return activeModule.value.pages
  return activeModule.value.pages.filter(page => page.name.toLowerCase().includes(search)
    || page.path.toLowerCase().includes(search)
    || page.actionItems.some(item => item.label.toLowerCase().includes(search)))
})
const activeDetailPages = computed(() => activePages.value.filter(page => page.hasDetails))
const pageScopeOptions = computed(() => [
  {
    label: `跟随角色默认（当前${dataScopeOptionText(props.dataScopeSettings.defaultDataScope)}）`,
    value: INHERIT_SCOPE,
  },
  ...props.dataScopeOptions.map(option => ({
    ...option,
    label: `固定为${option.label}`,
  })),
])
const currentDefaultScopeLabel = computed(() => dataScopeOptionText(props.dataScopeSettings.defaultDataScope))
const defaultScopeDropdownOptions = computed(() => props.dataScopeOptions.map(option => ({
  label: option.label,
  key: Number(option.value),
  icon: renderDropdownIcon(scopeIconClass(option.value)),
})))
const globalBatchOptions = computed(() => batchOptions('global', props.loading || workspaceModules.value.length === 0))
const moduleBatchOptions = computed(() => batchOptions('module', props.loading || activePages.value.length === 0))

watch(filteredNavigationModules, (modules) => {
  if (modules.some(module => module.key === activeModuleKey.value))
    return
  activeModuleKey.value = modules[0]?.key || ''
}, { immediate: true })

watch(workspaceModules, (modules) => {
  const availablePageKeys = new Set(modules.flatMap(module => module.pages.map(page => page.key)))
  collapsedPageKeys.value = new Set([...collapsedPageKeys.value].filter(key => availablePageKeys.has(key)))
}, { immediate: true })

watch([workspaceModules, () => props.checkedKeys], () => {
  normalizePageAccessKeys()
}, { immediate: true })

function buildWorkspaceModules(tree) {
  const modules = []
  const assignedIds = new Set()
  const dataScopeCodes = [...dataScopeModuleMap.value.keys()]

  function ensureModule(rawModule) {
    const key = rawModule?.key || 'module:default'
    let module = modules.find(item => item.key === key)
    if (!module) {
      module = {
        key,
        name: rawModule?.name || '业务模块',
        path: rawModule?.path || '',
        pages: [],
      }
      modules.push(module)
    }
    return module
  }

  function walk(nodes, breadcrumbs = [], currentModule = null) {
    for (const node of nodes || []) {
      const type = Number(node.resourceType)
      const nextBreadcrumbs = [1, 2].includes(type)
        ? [...breadcrumbs, node.resourceName].filter(Boolean)
        : breadcrumbs
      const nextModule = type === 1
        ? {
            key: `module:${node.id}`,
            name: node.resourceName || '未命名模块',
            path: breadcrumbs.join(' / '),
          }
        : currentModule

      if (type === 2) {
        const ownedActions = collectOwnedActions(node)
        const childMenus = (node.children || []).some(child => Number(child.resourceType) === 2)
        if (ownedActions.length > 0 || !childMenus) {
          const items = groupPermissionItems(ownedActions, node.resourceName)
          const accessItem = {
            key: `access:${node.id}`,
            label: '页面入口',
            resourceIds: [node.id],
            permission: String(node.perms || '').trim(),
          }
          const actionResourceIds = uniqueIds(items.flatMap(item => item.resourceIds))
          const resourceIds = uniqueIds([...accessItem.resourceIds, ...actionResourceIds])
          resourceIds.forEach(id => assignedIds.add(String(id)))
          const module = ensureModule(nextModule || {
            key: `module:single:${node.id}`,
            name: node.resourceName || '业务模块',
            path: breadcrumbs.join(' / '),
          })
          const moduleCode = resolveDataScopeModuleCode([node, ...ownedActions], dataScopeCodes)
          module.pages.push({
            key: `page:${node.id}`,
            name: node.resourceName || '未命名页面',
            path: nextBreadcrumbs.slice(0, -1).join(' / '),
            accessItem,
            actionItems: items,
            resourceIds,
            moduleCode,
            dataScopeModule: moduleCode ? dataScopeModuleMap.value.get(moduleCode) : null,
          })
        }
      }

      const structuralChildren = (node.children || []).filter(child => [1, 2].includes(Number(child.resourceType)))
      walk(structuralChildren, nextBreadcrumbs, nextModule)
    }
  }

  walk(tree)

  const unassignedActions = collectAllActions(tree)
    .filter(node => !assignedIds.has(String(node.id)))
  if (unassignedActions.length > 0) {
    const items = groupPermissionItems(unassignedActions, '')
    const moduleCode = resolveDataScopeModuleCode(unassignedActions, dataScopeCodes)
    ensureModule({
      key: 'module:other',
      name: '其他业务权限',
      path: '',
    }).pages.push({
      key: 'page:other',
      name: '未归类权限',
      path: '',
      accessItem: null,
      actionItems: items,
      resourceIds: uniqueIds(items.flatMap(item => item.resourceIds)),
      moduleCode,
      dataScopeModule: moduleCode ? dataScopeModuleMap.value.get(moduleCode) : null,
    })
  }

  return modules
    .filter(module => module.pages.length > 0)
}

function normalizePermissionModules(modules) {
  return (modules || []).map((module, moduleIndex) => ({
    ...module,
    key: module.key || `module:prepared:${moduleIndex}`,
    name: module.name || '业务模块',
    path: module.path || '',
    pages: (module.pages || []).map((page, pageIndex) => {
      const actionItems = (page.actionItems || []).map((item, itemIndex) => ({
        ...item,
        key: item.key || `permission:${moduleIndex}:${pageIndex}:${itemIndex}`,
        label: item.label || '使用',
        resourceIds: uniqueIds(item.resourceIds || []),
        permissions: uniqueIds(item.permissions || []),
        sources: Array.isArray(item.sources) ? item.sources : [],
        disabled: Boolean(item.disabled),
      }))
      const accessItem = page.accessItem
        ? {
            ...page.accessItem,
            resourceIds: uniqueIds(page.accessItem.resourceIds || []),
          }
        : null
      const moduleCode = page.moduleCode || page.dataScopeModule?.moduleCode || ''
      const showDataScopePanel = page.showDataScopePanel === undefined
        ? true
        : Boolean(page.showDataScopePanel)
      const showFunctionPanel = page.showFunctionPanel === undefined
        ? true
        : Boolean(page.showFunctionPanel)

      return {
        ...page,
        key: page.key || `page:prepared:${moduleIndex}:${pageIndex}`,
        name: page.name || '未命名页面',
        path: page.path || '',
        accessItem,
        actionItems,
        moduleCode,
        dataScopeModule: moduleCode ? dataScopeModuleMap.value.get(moduleCode) || null : null,
        resourceIds: uniqueIds([
          ...(accessItem?.resourceIds || []),
          ...actionItems.flatMap(item => item.resourceIds),
        ]),
        showDataScopePanel,
        showFunctionPanel,
        hasDetails: showDataScopePanel || showFunctionPanel,
      }
    }),
  })).filter(module => module.pages.length > 0)
}

function collectOwnedActions(menuNode) {
  const result = []
  function walk(nodes) {
    for (const node of nodes || []) {
      const type = Number(node.resourceType)
      if (type === 2)
        continue
      if ([3, 4].includes(type))
        result.push(node)
      walk(node.children)
    }
  }
  walk(menuNode.children)
  return result
}

function collectAllActions(nodes, result = []) {
  for (const node of nodes || []) {
    if ([3, 4].includes(Number(node.resourceType)))
      result.push(node)
    collectAllActions(node.children, result)
  }
  return result
}

function groupPermissionItems(nodes, pageName) {
  const groups = new Map()
  for (const node of nodes) {
    const type = Number(node.resourceType)
    const actionKey = permissionActionKey(node)
    const key = `${permissionSectionKeyByType(type)}:${actionKey}`
    const current = groups.get(key) || {
      key,
      actionKey,
      sectionKey: permissionSectionKeyByType(type),
      label: '',
      resourceIds: [],
      permissions: [],
      resourceTypes: new Set(),
      hasBusinessLabel: false,
    }
    current.resourceIds.push(node.id)
    if (node.perms)
      current.permissions.push(String(node.perms).trim())
    current.resourceTypes.add(type)
    const label = normalizePermissionLabel(node.resourceName, pageName, actionKey)
    const isBusinessLabel = type === 3
    if (!current.label || (isBusinessLabel && !current.hasBusinessLabel)) {
      current.label = label
      current.hasBusinessLabel = isBusinessLabel
    }
    groups.set(key, current)
  }
  return [...groups.values()].map(({ hasBusinessLabel, resourceTypes, ...item }) => ({
    ...item,
    resourceIds: uniqueIds(item.resourceIds),
    permissions: uniqueIds(item.permissions),
    sources: permissionSourceMetas(resourceTypes),
  }))
}

function permissionSections(page) {
  const sections = [
    {
      key: 'button',
      label: '页面按钮',
      items: [],
    },
    {
      key: 'service',
      label: '后台服务',
      items: [],
    },
    {
      key: 'resource',
      label: '其他资源',
      items: [],
    },
  ]
  const sectionMap = new Map(sections.map(section => [section.key, section]))
  for (const item of page.actionItems || []) {
    const key = item.sectionKey || permissionSectionKeyByType(item.sources?.[0]?.type)
    const section = sectionMap.get(key) || sectionMap.get('resource')
    section.items.push(item)
  }
  return sections.filter(section => section.items.length > 0)
}

function permissionSectionKeyByType(type) {
  if (Number(type) === 3)
    return 'button'
  if (Number(type) === 4)
    return 'service'
  return 'resource'
}

function permissionSourceMetas(resourceTypes) {
  const sortedTypes = [...resourceTypes].sort((left, right) => left - right)
  return sortedTypes.map((type) => {
    if (type === 3) {
      return {
        type,
        kind: 'button',
        label: '按钮',
      }
    }
    if (type === 4) {
      return {
        type,
        kind: 'service',
        label: '服务',
      }
    }
    return {
      type,
      kind: 'resource',
      label: '资源',
    }
  })
}

function permissionActionKey(node) {
  const permission = String(node.perms || '')
  const segments = permission.split(':').filter(segment => segment && segment !== 'api')
  const rawAction = segments[segments.length - 1] || String(node.resourceName || node.id)
  const normalized = rawAction.toLowerCase().replace(/_/g, '-')
  const aliases = {
    page: 'view',
    list: 'view',
    query: 'view',
    get: 'detail',
    getbyid: 'detail',
    add: 'create',
    save: 'create',
    update: 'edit',
    remove: 'delete',
  }
  return aliases[normalized] || normalized
}

function normalizePermissionLabel(resourceName, pageName, actionKey) {
  let label = String(resourceName || '')
    .replace(/(?:按钮权限|按钮|接口权限|接口|API)$/i, '')
    .trim()
  if (pageName && label.startsWith(pageName))
    label = label.slice(pageName.length).trim()
  const fallbackLabels = {
    view: '查看',
    detail: '查看详情',
    create: '新增',
    edit: '编辑',
    delete: '删除',
    export: '导出',
    import: '导入',
  }
  return label || fallbackLabels[actionKey] || '使用'
}

function resolveDataScopeModuleCode(nodes, availableCodes) {
  const permissions = nodes
    .map(node => String(node.perms || '').trim())
    .filter(Boolean)

  return [...availableCodes]
    .sort((left, right) => right.length - left.length)
    .find(code => permissions.some(permission => permission === code || permission.startsWith(`${code}:`)))
}

function uniqueIds(ids) {
  const seen = new Set()
  return ids.filter((id) => {
    if (id == null || seen.has(String(id)))
      return false
    seen.add(String(id))
    return true
  })
}

function renderDropdownIcon(className) {
  return () => h('i', { class: className })
}

function batchOptions(scope, disabled) {
  const prefix = scope === 'global' ? '全部' : '当前模块'
  return [
    {
      label: `只开放${prefix}菜单入口`,
      key: `${scope}:menus`,
      disabled,
      icon: renderDropdownIcon('i-material-symbols:menu-open'),
    },
    {
      label: `开放${prefix}菜单和功能权限`,
      key: `${scope}:all`,
      disabled,
      icon: renderDropdownIcon('i-material-symbols:verified-user'),
    },
    {
      key: `${scope}:divider`,
      type: 'divider',
    },
    {
      label: `清空${prefix}授权`,
      key: `${scope}:clear`,
      disabled,
      icon: renderDropdownIcon('i-material-symbols:delete-sweep'),
      props: {
        class: 'permission-dropdown-danger',
      },
    },
  ]
}

function updateCheckedKeys(resourceIds, checked) {
  const next = new Map(props.checkedKeys.map(id => [String(id), id]))
  for (const id of resourceIds) {
    if (checked)
      next.set(String(id), id)
    else
      next.delete(String(id))
  }
  emit('update:checkedKeys', [...next.values()])
}

function isPermissionChecked(permission) {
  return permission.resourceIds.length > 0
    && permission.resourceIds.every(id => checkedKeySet.value.has(String(id)))
}

function isPermissionPartiallyChecked(permission) {
  const selectedCount = permission.resourceIds.filter(id => checkedKeySet.value.has(String(id))).length
  return selectedCount > 0 && selectedCount < permission.resourceIds.length
}

function isPageAccessChecked(page) {
  if (!page.accessItem)
    return page.actionItems.some(isPermissionChecked)
  return isPermissionChecked(page.accessItem)
}

function isPagePartiallyChecked(page) {
  if (!page.accessItem) {
    const checkedCount = page.resourceIds.filter(id => checkedKeySet.value.has(String(id))).length
    return checkedCount > 0 && checkedCount < page.resourceIds.length
  }
  return !isPermissionChecked(page.accessItem) && hasAnyActionChecked(page)
}

function selectedActionCountInPage(page) {
  return configurableActions(page).filter(isPermissionChecked).length
}

function configurableActions(page) {
  return (page.actionItems || []).filter(item => !item.disabled && item.resourceIds.length > 0)
}

function hasAnyActionChecked(page) {
  return page.actionItems.some(item =>
    item.resourceIds.some(id => checkedKeySet.value.has(String(id))))
}

function areAllActionsChecked(page) {
  const actions = configurableActions(page)
  return actions.length > 0 && actions.every(isPermissionChecked)
}

function togglePageAccess(page, checked) {
  if (!page.accessItem) {
    updateCheckedKeys(page.resourceIds, checked)
    return
  }
  let ids = page.accessItem.resourceIds
  if (props.linkPageAndActions && checked && cascadeEnabled.value)
    ids = uniqueIds(page.resourceIds)
  else if (props.linkPageAndActions && !checked)
    ids = page.resourceIds
  updateCheckedKeys(ids, checked)
}

function toggleAction(page, permission, checked) {
  const ids = checked && page.accessItem && props.linkPageAndActions
    ? uniqueIds([...permission.resourceIds, ...page.accessItem.resourceIds])
    : permission.resourceIds
  updateCheckedKeys(ids, checked)
}

function togglePageAllActions(page, checked) {
  const actionIds = configurableActions(page).flatMap(item => item.resourceIds)
  const ids = checked && page.accessItem && props.linkPageAndActions
    ? uniqueIds([...actionIds, ...page.accessItem.resourceIds])
    : uniqueIds(actionIds)
  updateCheckedKeys(ids, checked)
}

function selectPageAccessOnly(page) {
  if (!page.accessItem)
    return
  const next = new Map(props.checkedKeys.map(id => [String(id), id]))
  const actionIds = page.actionItems.flatMap(item => item.resourceIds)
  for (const id of uniqueIds(actionIds))
    next.delete(String(id))
  for (const id of page.accessItem.resourceIds)
    next.set(String(id), id)
  emit('update:checkedKeys', [...next.values()])
}

function normalizePageAccessKeys() {
  if (!props.linkPageAndActions || workspaceModules.value.length === 0 || props.checkedKeys.length === 0)
    return
  const next = new Map(props.checkedKeys.map(id => [String(id), id]))
  let changed = false
  for (const module of workspaceModules.value) {
    for (const page of module.pages) {
      if (!page.accessItem)
        continue
      const hasChildPermission = page.actionItems.some(item =>
        item.resourceIds.some(id => next.has(String(id))))
      if (!hasChildPermission)
        continue
      for (const id of page.accessItem.resourceIds) {
        if (!next.has(String(id))) {
          next.set(String(id), id)
          changed = true
        }
      }
    }
  }
  if (changed)
    emit('update:checkedKeys', [...next.values()])
}

function handleGlobalBatchSelect(key) {
  const actionMap = {
    'global:menus': selectMenusInAllModules,
    'global:all': selectAllPermissions,
    'global:clear': clearAllPermissions,
  }
  actionMap[key]?.()
}

function handleModuleBatchSelect(key) {
  const actionMap = {
    'module:menus': selectMenusInActiveModule,
    'module:all': selectAllInActiveModule,
    'module:clear': deselectAllInActiveModule,
  }
  actionMap[key]?.()
}

function selectAllPermissions() {
  updateCheckedKeys(uniqueIds(workspaceModules.value.flatMap(module =>
    module.pages.flatMap(page => page.resourceIds))), true)
}

function selectMenusInAllModules() {
  selectMenusInModules(workspaceModules.value)
}

function clearAllPermissions() {
  updateCheckedKeys(uniqueIds(workspaceModules.value.flatMap(module =>
    module.pages.flatMap(page => page.resourceIds))), false)
}

function selectAllInActiveModule() {
  if (!activeModule.value)
    return
  updateCheckedKeys(uniqueIds(activeModule.value.pages.flatMap(page => page.resourceIds)), true)
}

function selectMenusInActiveModule() {
  if (!activeModule.value)
    return
  selectMenusInModules([activeModule.value])
}

function selectMenusInModules(modules) {
  const next = new Map(props.checkedKeys.map(id => [String(id), id]))
  const pages = modules.flatMap(module => module.pages)
  const menuIds = pages
    .filter(page => page.accessItem)
    .flatMap(page => page.accessItem.resourceIds)
  const actionIds = pages
    .flatMap(page => page.actionItems.flatMap(item => item.resourceIds))
  for (const id of uniqueIds(actionIds))
    next.delete(String(id))
  for (const id of uniqueIds(menuIds))
    next.set(String(id), id)
  emit('update:checkedKeys', [...next.values()])
}

function deselectAllInActiveModule() {
  if (!activeModule.value)
    return
  updateCheckedKeys(uniqueIds(activeModule.value.pages.flatMap(page => page.resourceIds)), false)
}

function moduleStatus(module) {
  const resourceIds = uniqueIds(module.pages.flatMap(page => page.resourceIds))
  const selectedCount = resourceIds.filter(id => checkedKeySet.value.has(String(id))).length
  if (selectedCount === 0)
    return 'none'
  if (selectedCount === resourceIds.length)
    return 'all'
  return 'partial'
}

function moduleStatusLabel(status) {
  const labels = {
    all: '已全配',
    partial: '部分配置',
    none: '未配置',
  }
  return labels[status] || '未配置'
}

function moduleStatusIcon(status) {
  const icons = {
    all: 'i-material-symbols:check-circle',
    partial: 'i-material-symbols:do-not-disturb-on',
    none: 'i-material-symbols:radio-button-unchecked',
  }
  return icons[status] || icons.none
}

function moduleIconClass(module) {
  const name = module.name || ''
  if (name.includes('系统'))
    return 'i-material-symbols:settings'
  if (name.includes('用户') || name.includes('人事'))
    return 'i-material-symbols:group'
  if (name.includes('流程') || name.includes('审批'))
    return 'i-material-symbols:account-tree'
  if (name.includes('合同') || name.includes('文档'))
    return 'i-material-symbols:contract'
  if (name.includes('财务') || name.includes('收款'))
    return 'i-material-symbols:database'
  return 'i-material-symbols:widgets'
}

function isCollapsed(pageKey) {
  return collapsedPageKeys.value.has(pageKey)
}

function togglePageCollapse(pageKey) {
  const next = new Set(collapsedPageKeys.value)
  if (next.has(pageKey))
    next.delete(pageKey)
  else
    next.add(pageKey)
  collapsedPageKeys.value = next
}

function toggleCollapseAll(collapse) {
  if (!activeModule.value)
    return
  const next = new Set(collapsedPageKeys.value)
  activeModule.value.pages.filter(page => page.hasDetails).forEach((page) => {
    if (collapse)
      next.add(page.key)
    else
      next.delete(page.key)
  })
  collapsedPageKeys.value = next
}

function isScopeSelected(module, value) {
  if (value === INHERIT_SCOPE)
    return module.dataScope == null
  return module.dataScope != null && Number(module.dataScope) === Number(value)
}

function showsFlowRelatedHint(module) {
  if (!module?.flowRelatedVisible)
    return false
  const scope = module.dataScope ?? props.dataScopeSettings.defaultDataScope
  return ![1, 2].includes(Number(scope))
}

function dataScopeOptionText(value) {
  return props.dataScopeOptions.find(option => Number(option.value) === Number(value))?.label || '未设置'
}

function scopeIconClass(value) {
  if (value === INHERIT_SCOPE)
    return 'i-material-symbols:account-tree'
  const icons = {
    1: 'i-material-symbols:public',
    2: 'i-material-symbols:domain',
    3: 'i-material-symbols:groups',
    4: 'i-material-symbols:hub',
    5: 'i-material-symbols:person',
    7: 'i-material-symbols:map',
  }
  return icons[Number(value)] || 'i-material-symbols:database'
}

function updateDefaultScope(value) {
  if (!props.defaultScopeEditable)
    return
  emit('update:dataScopeSettings', {
    ...props.dataScopeSettings,
    defaultDataScope: value,
    modules: (props.dataScopeSettings.modules || []).map(module => ({
      ...module,
      effectiveDataScope: module.dataScope ?? value,
    })),
  })
}

function updateModuleScope(moduleCode, value) {
  const nextValue = value === INHERIT_SCOPE ? null : value
  emit('update:dataScopeSettings', {
    ...props.dataScopeSettings,
    modules: (props.dataScopeSettings.modules || []).map(module => module.moduleCode === moduleCode
      ? { ...module, dataScope: nextValue, effectiveDataScope: nextValue ?? props.dataScopeSettings.defaultDataScope }
      : module),
  })
}
</script>

<style scoped>
.role-permission-workbench {
  display: grid;
  grid-template-columns: 224px minmax(0, 1fr);
  flex: 1 1 auto;
  height: auto;
  min-height: 0;
  background: #f8fafc;
  color: #0f172a;
}

.permission-sidebar {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border-right: 1px solid #e2e8f0;
  background: #fff;
  box-shadow: 2px 0 8px rgba(15, 23, 42, 0.025);
}

.sidebar-search {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 0 0 auto;
  padding: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.batch-menu-button {
  min-width: 0;
}

.batch-menu-button.is-sidebar-batch {
  width: 100%;
  --n-border: 1px solid #cbd5e1 !important;
  --n-border-hover: 1px solid #94a3b8 !important;
  --n-border-pressed: 1px solid #64748b !important;
  --n-border-focus: 1px solid #2563eb !important;
  --n-color: #fff !important;
  --n-color-hover: #f8fafc !important;
  --n-color-pressed: #f1f5f9 !important;
}

.batch-menu-button :deep(.n-button__content) {
  min-width: 0;
  gap: 5px;
}

.batch-menu-button span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-menu-chevron {
  flex: 0 0 auto;
  margin-left: 2px;
  color: #64748b;
  font-size: 16px;
}

.module-nav-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px;
  scrollbar-gutter: stable;
}

.module-skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.module-skeleton-item {
  display: flex;
  align-items: center;
  min-height: 38px;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #f8fafc;
}

.module-skeleton-item :deep(.n-skeleton:nth-child(2)) {
  flex: 1;
  min-width: 0;
}

.module-skeleton-status {
  margin-left: auto;
}

.module-nav-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 38px;
  gap: 10px;
  padding: 8px 10px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #475569;
  cursor: pointer;
  font-size: 13px;
  line-height: 1.4;
  text-align: left;
  transition:
    background-color 0.18s ease,
    color 0.18s ease;
}

.module-nav-item:hover {
  background: #f8fafc;
  color: #0f172a;
}

.module-nav-item.is-active {
  background: #eef2ff;
  color: #3730a3;
  font-weight: 600;
}

.module-nav-main {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 8px;
}

.module-nav-main i {
  flex: 0 0 auto;
  color: #94a3b8;
  font-size: 16px;
}

.module-nav-item.is-active .module-nav-main i {
  color: #4f46e5;
}

.module-nav-main span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.module-nav-status {
  flex: 0 0 auto;
  display: inline-flex;
  font-size: 16px;
}

.module-nav-status.is-all {
  color: #10b981;
}

.module-nav-status.is-partial {
  color: #f59e0b;
}

.module-nav-status.is-none {
  color: #cbd5e1;
}

.sidebar-legend {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 10px 12px;
  border-top: 1px solid #f1f5f9;
  background: rgba(248, 250, 252, 0.72);
  color: #64748b;
  font-size: 11px;
}

.sidebar-legend span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  white-space: nowrap;
}

.sidebar-legend i {
  font-size: 13px;
}

.sidebar-legend span:nth-child(1) i {
  color: #10b981;
}

.sidebar-legend span:nth-child(2) i {
  color: #f59e0b;
}

.sidebar-legend span:nth-child(3) i {
  color: #cbd5e1;
}

.permission-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.permission-main-toolbar {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 58px;
  padding: 10px 18px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.toolbar-title {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 2px;
}

.toolbar-title strong {
  overflow: hidden;
  color: #1e293b;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toolbar-title span {
  color: #64748b;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.toolbar-controls {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.cascade-switch {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 28px;
  padding: 0 10px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.cascade-switch span {
  user-select: none;
}

.cascade-switch.is-independent {
  border-color: #e2e8f0;
  background: #f8fafc;
  color: #475569;
  cursor: default;
}

.cascade-switch.is-independent i {
  font-size: 15px;
}

.toolbar-divider {
  width: 1px;
  height: 18px;
  background: #cbd5e1;
}

.collapse-pill-control {
  display: inline-flex;
  align-items: center;
  gap: 0;
  height: 28px;
  padding: 2px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #f1f5f9;
}

.collapse-pill-control button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  height: 22px;
  gap: 4px;
  padding: 0 8px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #475569;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.collapse-pill-control button:hover:not(:disabled) {
  background: #fff;
  color: #3730a3;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.collapse-pill-control button:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.collapse-pill-control i {
  font-size: 14px;
}

.default-scope-button {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  min-width: 148px;
  height: 28px;
  gap: 8px;
  padding: 0 8px 0 10px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  color: #334155;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.default-scope-button:hover:not(:disabled) {
  border-color: #bfdbfe;
  background: #f8fbff;
  box-shadow: 0 4px 12px rgba(30, 64, 175, 0.08);
}

.default-scope-button:active:not(:disabled) {
  transform: translateY(1px);
}

.default-scope-button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.default-scope-button.is-readonly {
  cursor: default;
}

.default-scope-lock {
  flex: 0 0 auto;
  color: #94a3b8;
  font-size: 14px;
}

.default-scope-copy {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  gap: 0;
  line-height: 1;
}

.default-scope-eyebrow {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.default-scope-copy strong {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  gap: 4px;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.default-scope-copy strong i {
  flex: 0 0 auto;
  font-size: 14px;
}

.default-scope-chevron {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 16px;
}

.permission-spin,
.permission-spin :deep(.n-spin-content) {
  flex: 1;
  min-height: 0;
  width: 100%;
}

.permission-spin {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.permission-spin :deep(.n-spin-container) {
  height: 100%;
  min-height: 0;
}

.permission-spin :deep(.n-spin-content) {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.page-card-list {
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 18px 96px;
  scrollbar-gutter: stable;
}

.permission-page-card {
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.permission-page-card + .permission-page-card {
  margin-top: 14px;
}

.permission-page-card:hover {
  border-color: #c7d2fe;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.07);
}

.skeleton-page-card:hover {
  border-color: #e2e8f0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.page-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 52px;
  padding: 10px 14px;
  border-bottom: 1px solid #e2e8f0;
  background: rgba(248, 250, 252, 0.86);
}

.page-card-header.is-compact {
  border-bottom: 0;
}

.page-card-title {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 10px;
}

.skeleton-page-title {
  flex: 1;
}

.collapse-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  font-size: 19px;
  transition:
    background-color 0.18s ease,
    color 0.18s ease;
}

.collapse-button:hover {
  background: #e2e8f0;
  color: #334155;
}

.page-name {
  color: #1e293b;
  font-size: 14px;
  font-weight: 700;
}

.page-entry-tag {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 4px;
  padding: 2px 7px;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  background: #fff;
  color: #64748b;
  font-size: 11px;
  font-weight: 600;
}

.page-entry-tag i {
  font-size: 13px;
}

.page-entry-tag.is-warning {
  border-color: #fde68a;
  background: #fffbeb;
  color: #b45309;
}

.page-entry-tag.is-object {
  border-color: #dbeafe;
  background: #eff6ff;
  color: #2563eb;
}

.page-card-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.skeleton-page-actions {
  flex: 0 0 auto;
}

.page-action-count {
  padding: 2px 8px;
  border: 1px solid #f1f5f9;
  border-radius: 5px;
  background: #fff;
  color: #64748b;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.page-action-count strong {
  color: #4f46e5;
}

.page-card-body {
  display: grid;
  grid-template-columns: minmax(280px, 42%) minmax(0, 1fr);
  min-height: 160px;
}

.page-card-body.is-single {
  grid-template-columns: minmax(0, 1fr);
}

.page-card-body.is-single .data-scope-panel {
  border-right: 0;
}

.data-scope-panel,
.function-panel {
  min-width: 0;
  padding: 18px;
}

.data-scope-panel {
  border-right: 1px solid #f1f5f9;
  background: rgba(250, 250, 250, 0.58);
}

.flow-related-hint {
  margin: 10px 0 0;
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
  font-weight: 400;
  line-height: 1.5;
}

.section-heading {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  margin-bottom: 14px;
  color: #1e293b;
  font-size: 13px;
  font-weight: 700;
}

.section-heading > i {
  color: #2563eb;
  font-size: 16px;
}

.function-panel .section-heading > i {
  color: #059669;
}

.section-heading small {
  color: #64748b;
  font-size: 11px;
  font-weight: 400;
}

.scope-option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.scope-option {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 40px;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  color: #475569;
  cursor: pointer;
  font-size: 13px;
  line-height: 1.35;
  text-align: left;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.scope-option:hover {
  border-color: #93c5fd;
  background: #f8fafc;
}

.scope-option.is-selected {
  border-color: #3b82f6;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(37, 99, 235, 0.1);
}

.scope-radio {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 14px;
  height: 14px;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  background: #fff;
}

.scope-option.is-selected .scope-radio {
  border-color: #3b82f6;
  background: #3b82f6;
}

.scope-radio span {
  display: none;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #fff;
}

.scope-option.is-selected .scope-radio span {
  display: block;
}

.scope-option > i {
  flex: 0 0 auto;
  color: #94a3b8;
  font-size: 15px;
}

.scope-option.is-selected > i {
  color: #2563eb;
}

.scope-option > span:last-child {
  min-width: 0;
  overflow-wrap: anywhere;
}

.scope-unavailable {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 92px;
  border: 1px dashed #cbd5e1;
  border-radius: 7px;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 13px;
}

.scope-unavailable:has(.n-button) {
  flex-direction: column;
  gap: 6px;
}

.scope-unavailable.is-warning {
  border-color: #fde68a;
  background: #fffbeb;
  color: #b45309;
}

.function-section-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.function-section {
  min-width: 0;
}

.function-section + .function-section {
  padding-top: 14px;
  border-top: 1px dashed #e2e8f0;
}

.function-section-heading {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.function-section-heading::before {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #2563eb;
  content: '';
}

.function-section.is-service .function-section-heading::before {
  background: #059669;
}

.function-section-heading small {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 500;
}

.function-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 14px 24px;
}

.function-grid :deep(.n-checkbox) {
  min-height: 24px;
}

.function-grid :deep(.n-checkbox__label) {
  min-width: 0;
  color: #334155;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.function-permission-label {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  gap: 6px;
  vertical-align: middle;
}

.function-permission-name {
  min-width: 0;
  overflow-wrap: anywhere;
}

.function-source-tags {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 4px;
}

.function-source-tag {
  display: inline-flex;
  align-items: center;
  height: 17px;
  padding: 0 5px;
  border: 1px solid #dbeafe;
  border-radius: 4px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 10px;
  font-weight: 700;
  line-height: 16px;
}

.function-source-tag.is-service {
  border-color: #dcfce7;
  background: #f0fdf4;
  color: #059669;
}

.function-source-tag.is-pending {
  border-color: #fde68a;
  background: #fffbeb;
  color: #b45309;
}

.function-skeleton-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 14px 24px;
}

.function-empty,
.workspace-empty {
  padding: 26px 0;
}

:global(.dark) .role-permission-workbench,
:global(.dark) .permission-main-toolbar {
  background: #0f172a;
  color: #e5e7eb;
}

:global(.dark) .permission-sidebar,
:global(.dark) .permission-page-card,
:global(.dark) .page-entry-tag,
:global(.dark) .page-action-count,
:global(.dark) .scope-option {
  background: #111827;
  border-color: #334155;
}

:global(.dark) .permission-sidebar,
:global(.dark) .permission-main-toolbar,
:global(.dark) .page-card-header,
:global(.dark) .data-scope-panel {
  border-color: #334155;
}

:global(.dark) .function-source-tag {
  border-color: #1d4ed8;
  background: rgba(37, 99, 235, 0.16);
  color: #93c5fd;
}

:global(.dark) .function-source-tag.is-service {
  border-color: #047857;
  background: rgba(5, 150, 105, 0.16);
  color: #86efac;
}

:global(.dark) .page-card-header,
:global(.dark) .data-scope-panel,
:global(.dark) .collapse-pill-control,
:global(.dark) .sidebar-legend {
  background: #1e293b;
}

:global(.dark) .collapse-pill-control {
  border-color: #334155;
}

:global(.dark) .default-scope-button {
  border-color: #334155;
  background: linear-gradient(180deg, #1e293b 0%, #172033 100%);
  color: #cbd5e1;
}

:global(.dark) .default-scope-button:hover:not(:disabled) {
  border-color: #475569;
  background: #1e293b;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.22);
}

:global(.dark) .default-scope-eyebrow,
:global(.dark) .default-scope-chevron {
  color: #cbd5e1;
}

:global(.dark) .default-scope-copy strong {
  color: #93c5fd;
}

:global(.dark) .collapse-pill-control button {
  color: #cbd5e1;
}

:global(.dark) .collapse-pill-control button:hover:not(:disabled) {
  background: #0f172a;
  color: #c7d2fe;
}

:global(.dark) .toolbar-title strong,
:global(.dark) .page-name,
:global(.dark) .section-heading,
:global(.dark) .function-grid :deep(.n-checkbox__label) {
  color: #f8fafc;
}

:global(.dark) .toolbar-title span,
:global(.dark) .page-entry-tag,
:global(.dark) .page-action-count,
:global(.dark) .section-heading small {
  color: #94a3b8;
}

:global(.dark) .cascade-switch {
  border-color: #1d4ed8;
  background: rgba(37, 99, 235, 0.16);
  color: #bfdbfe;
}

:global(.dark) .module-nav-item {
  color: #cbd5e1;
}

:global(.dark) .module-nav-item:hover {
  background: #1e293b;
  color: #f8fafc;
}

:global(.dark) .module-skeleton-item {
  background: #1e293b;
}

:global(.dark) .module-nav-item.is-active {
  background: rgba(79, 70, 229, 0.18);
}

:global(.dark) .scope-option.is-selected {
  background: rgba(79, 70, 229, 0.18);
}

:global(.permission-dropdown-danger) {
  color: #dc2626;
}

@media (max-width: 1180px) {
  .permission-main-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-controls {
    justify-content: flex-start;
  }
}

@media (max-width: 900px) {
  .role-permission-workbench {
    grid-template-columns: minmax(0, 1fr);
  }

  .permission-sidebar {
    max-height: 210px;
    border-right: 0;
    border-bottom: 1px solid #e2e8f0;
  }

  .page-card-body {
    grid-template-columns: minmax(0, 1fr);
  }

  .data-scope-panel {
    border-right: 0;
    border-bottom: 1px solid #f1f5f9;
  }
}

@media (max-width: 640px) {
  .page-card-list {
    padding: 12px;
  }

  .page-card-header,
  .toolbar-controls {
    align-items: stretch;
    flex-direction: column;
  }

  .page-card-title,
  .page-card-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .default-scope-button,
  .toolbar-controls :deep(.n-button),
  .collapse-pill-control {
    width: 100%;
  }

  .collapse-pill-control button {
    flex: 1;
  }

  .toolbar-divider {
    display: none;
  }

  .scope-option-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (prefers-reduced-motion: reduce) {
  .module-nav-item,
  .permission-page-card,
  .collapse-button,
  .scope-option {
    transition: none;
  }
}
</style>
