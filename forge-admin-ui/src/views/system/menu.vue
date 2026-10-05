<template>
  <div ref="pageRef" class="system-menu-page">
    <!-- 客户端与页面级操作 -->
    <div class="menu-workbench-header">
      <div class="header-main">
        <div class="header-title">
          <h2>菜单管理</h2>
        </div>

        <div class="header-actions">
          <NButton size="small" :loading="loading" @click="loadResourceTree">
            <template #icon>
              <i class="i-material-symbols:refresh" />
            </template>
            刷新
          </NButton>
          <NButton size="small" type="primary" @click="handleAddRoot">
            <template #icon>
              <i class="i-material-symbols:add" />
            </template>
            新增顶级菜单
          </NButton>
        </div>
      </div>

      <div class="client-tabs-container">
        <n-tabs type="line" size="small" :value="currentClientCode" @update:value="handleClientTabChange">
          <n-tab-pane name="" tab="全部">
            <template #tab>
              <span class="tab-label">全部</span>
            </template>
          </n-tab-pane>
          <n-tab-pane v-for="client in clientList" :key="client.clientCode" :name="client.clientCode">
            <template #tab>
              <span class="tab-label">{{ client.clientName }}</span>
            </template>
          </n-tab-pane>
        </n-tabs>
      </div>
    </div>

    <div class="menu-workbench-body">
      <!-- 左侧只负责定位目录和菜单 -->
      <aside class="tree-pane">
        <div class="pane-head">
          <div>
            <span class="pane-title">资源结构</span>
            <span class="pane-subtitle">目录 / 菜单</span>
          </div>
          <div class="tree-head-actions">
            <NButton quaternary size="tiny" @click="expandNavigationTree">
              <template #icon>
                <i class="i-material-symbols:unfold-more" />
              </template>
              展开
            </NButton>
            <NButton quaternary size="tiny" @click="collapseNavigationTree">
              <template #icon>
                <i class="i-material-symbols:unfold-less" />
              </template>
              折叠
            </NButton>
          </div>
        </div>

        <n-input
          v-model:value="treeKeyword"
          size="small"
          clearable
          placeholder="筛选左侧结构"
          class="tree-search"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>

        <div class="tree-scroll">
          <div v-if="loading" class="menu-tree-skeleton">
            <div
              v-for="index in 10"
              :key="index"
              class="menu-tree-skeleton-row"
              :style="{ paddingLeft: `${(index % 3) * 16}px` }"
            >
              <n-skeleton circle size="small" />
              <n-skeleton text :width="`${72 - (index % 4) * 8}%`" />
            </div>
          </div>
          <n-tree
            v-else
            block-line
            selectable
            :data="navigationTreeData"
            :expanded-keys="navigationExpandedKeys"
            :selected-keys="navigationSelectedKeys"
            :render-label="renderNavigationLabel"
            key-field="key"
            label-field="label"
            children-field="children"
            @update:selected-keys="handleNavigationSelect"
            @update:expanded-keys="handleNavigationExpandedKeys"
          />
        </div>
      </aside>

      <main class="list-pane">
        <!-- 当前层级与查询；低频筛选按需展开 -->
        <div class="list-toolbar">
          <div class="list-context">
            <nav class="context-breadcrumbs" aria-label="资源层级">
              <template v-for="(item, index) in contextBreadcrumbs" :key="item.id">
                <i v-if="index" class="i-lucide:chevron-right" aria-hidden="true" />
                <button
                  type="button" :aria-current="index === contextBreadcrumbs.length - 1 ? 'page' : undefined"
                  :title="item.label" @click="enterResource(item)"
                >
                  {{ item.label }}
                </button>
              </template>
            </nav>
            <span class="context-subtitle">{{ displayRows.length }} 项资源</span>
          </div>
          <div class="list-toolbar-right">
            <div class="list-filters">
              <n-input
                v-model:value="resourceKeyword"
                size="small"
                clearable
                placeholder="搜索名称 / 路由 / 权限 / API"
                class="keyword-input"
              >
                <template #prefix>
                  <i class="i-material-symbols:search" />
                </template>
              </n-input>
              <n-popover v-model:show="workspace.filtersVisible" trigger="click" placement="bottom-end" :show-arrow="false">
                <template #trigger>
                  <NButton size="small" :type="activeFilterCount ? 'primary' : 'default'" :secondary="!!activeFilterCount">
                    <template #icon>
                      <i class="i-lucide:list-filter" />
                    </template>
                    筛选{{ activeFilterCount ? ` (${activeFilterCount})` : '' }}
                  </NButton>
                </template>
                <div class="resource-filter-panel">
                  <label>资源类型</label>
                  <n-select v-model:value="resourceTypeFilter" size="small" clearable placeholder="全部类型" :options="resourceTypeFilterOptions" />
                  <label>显示状态</label>
                  <n-select v-model:value="visibleFilter" size="small" clearable placeholder="全部状态" :options="visibleFilterOptions" />
                  <div class="filter-actions">
                    <NButton size="small" @click="resetResourceFilters">
                      重置
                    </NButton>
                    <NButton size="small" type="primary" @click="workspace.filtersVisible = false">
                      完成
                    </NButton>
                  </div>
                </div>
              </n-popover>
              <NButton v-if="currentNode" size="small" type="primary" @click="handleAdd(currentNode)">
                <template #icon>
                  <i class="i-lucide:plus" />
                </template>新增子项
              </NButton>
            </div>
          </div>
        </div>
        <!-- 勾选后才出现批量动作，不占据日常浏览首屏 -->
        <div class="resource-selection-bar">
          <NCheckbox
            size="small" :checked="allDisplayRowsChecked" :indeterminate="displayRowsCheckIndeterminate"
            :disabled="displayRows.length === 0" @update:checked="handleDisplayRowsCheckedChange"
          >
            全选当前结果
          </NCheckbox>
          <template v-if="checkedResourceIds.length">
            <span class="checked-count">已选 {{ checkedResourceIds.length }} 项</span>
            <NButton size="tiny" secondary @click="openBatchMigrate">
              批量迁移
            </NButton>
            <NButton size="tiny" type="error" secondary :loading="batchActionLoading" @click="handleBatchDelete">
              批量删除
            </NButton>
            <NButton size="tiny" text @click="clearCheckedResources">
              取消选择
            </NButton>
          </template>
          <span v-else class="selection-hint">点击名称进入下级，点击行查看详情</span>
        </div>
        <!-- 当前层资源列表，自身滚动 -->
        <div class="resource-list-scroll cus-scroll-y">
          <div v-if="loading" class="resource-list-skeleton">
            <div v-for="index in 9" :key="index" class="resource-list-skeleton-row">
              <n-skeleton circle size="small" />
              <div class="resource-list-skeleton-main">
                <n-skeleton text :width="`${42 + (index % 4) * 8}%`" />
                <n-skeleton text :width="`${58 - (index % 3) * 7}%`" />
              </div>
              <n-skeleton text width="34%" />
              <n-skeleton text width="36px" />
              <n-skeleton text width="48px" />
            </div>
          </div>
          <template v-else>
            <div v-if="displayRows.length === 0" class="resource-list-empty">
              <i class="i-material-symbols:database-off-outline" />
              <span>{{ hasSearch ? '没有匹配的资源' : '当前层级暂无资源' }}</span>
              <NButton v-if="hasSearch" size="small" text type="primary" @click="resetResourceFilters">
                清除查询条件
              </NButton>
            </div>

            <div
              v-for="row in displayRows"
              :key="row.id"
              class="resource-list-row"
              :class="{ 'is-active': activeResource?.id === row.id, 'is-checked': checkedResourceIdSet.has(row.id) }"
              @click="openResourceDetail(row)"
            >
              <div class="resource-list-check" @click.stop>
                <NCheckbox
                  size="small"
                  :aria-label="`选择资源 ${row.resourceName}`"
                  :checked="checkedResourceIdSet.has(row.id)"
                  @update:checked="checked => handleResourceCheckedChange(row, checked)"
                />
              </div>

              <div class="resource-list-main">
                <span
                  class="resource-level-spacer"
                  :class="{ 'has-level': getDisplayLevel(row) > 0 }"
                  :style="{ width: `${getDisplayLevel(row) * 18}px` }"
                />
                <span class="resource-icon-shell">
                  <IconRenderer
                    v-if="getRenderableIcon(row)"
                    :icon="getRenderableIcon(row)"
                    :font-size="15"
                    custom-style="color: var(--primary-color, #4C6EF5)"
                  />
                  <IconRenderer
                    v-else
                    :icon="getResourceTypeConfig(row.resourceType).icon"
                    :font-size="15"
                  />
                </span>

                <div class="resource-list-copy">
                  <div class="resource-title-line">
                    <button
                      type="button" class="resource-name" :title="row.resourceName"
                      :aria-label="[1, 2].includes(Number(row.resourceType)) ? `进入 ${row.resourceName} 下级资源` : `查看 ${row.resourceName} 详情`"
                      @click.stop="handleResourceName(row)"
                    >
                      {{ row.resourceName || '-' }}
                      <i v-if="[1, 2].includes(Number(row.resourceType))" class="i-lucide:chevron-right" />
                    </button>
                    <span class="resource-type-badge">
                      {{ getResourceTypeText(row.resourceType) }}
                    </span>
                    <span v-if="!currentClientCode" class="resource-client-chip">
                      {{ getClientDisplayName(row.clientCode) }}
                    </span>
                  </div>
                  <div class="resource-meta-line">
                    {{ getResourceSubtitle(row) }}
                  </div>
                </div>
              </div>

              <div class="resource-route-summary">
                <span>{{ getPrimaryRouteText(row) }}</span>
                <small v-if="getSecondaryRouteText(row)">{{ getSecondaryRouteText(row) }}</small>
              </div>

              <div class="resource-list-sort">
                <NInputNumber
                  v-if="row._editingSort"
                  :value="row.sort"
                  :min="0"
                  :show-button="false"
                  size="small"
                  class="inline-sort-input"
                  @click.stop
                  @update:value="value => handleSortCommit(row, value)"
                  @blur="row._editingSort = false"
                />
                <button v-else class="sort-chip" :aria-label="`调整 ${row.resourceName} 排序`" title="点击调整排序" @click.stop="row._editingSort = true">
                  {{ row.sort ?? 0 }}
                </button>
              </div>
              <div class="resource-list-status">
                <NTooltip trigger="hover">
                  <template #trigger>
                    <NButton
                      quaternary
                      circle
                      size="tiny"
                      class="table-icon-action visibility-icon-button"
                      :aria-label="`${Number(row.visible) === 1 ? '隐藏' : '显示'} ${row.resourceName}`"
                      :class="Number(row.visible) === 1 ? 'is-visible' : 'is-hidden'"
                      @click.stop="handleInlineUpdate(row, 'visible', Number(row.visible) === 1 ? 0 : 1)"
                    >
                      <template #icon>
                        <i :class="Number(row.visible) === 1 ? 'i-material-symbols:visibility' : 'i-material-symbols:visibility-off'" />
                      </template>
                    </NButton>
                  </template>
                  {{ Number(row.visible) === 1 ? '点击隐藏' : '点击显示' }}
                </NTooltip>
              </div>
              <div class="resource-list-actions">
                <button
                  type="button" class="resource-text-action text-info" :aria-label="`查看 ${row.resourceName} 详情`"
                  @click.stop="openResourceDetail(row)"
                >
                  详情
                </button>
                <NTooltip trigger="hover">
                  <template #trigger>
                    <button type="button" class="resource-text-action text-primary" :aria-label="`编辑 ${row.resourceName}`" @click.stop="handleEdit(row)">
                      编辑
                    </button>
                  </template>
                  编辑
                </NTooltip>
                <NDropdown
                  trigger="click"
                  :options="getMoreActionOptions(row)"
                  @select="key => handleMoreAction(key, row)"
                >
                  <NTooltip trigger="hover">
                    <template #trigger>
                      <NButton quaternary circle size="tiny" class="table-icon-action" :aria-label="`${row.resourceName} 更多操作`" @click.stop>
                        <template #icon>
                          <i class="i-material-symbols:more-horiz" />
                        </template>
                      </NButton>
                    </template>
                    更多操作
                  </NTooltip>
                </NDropdown>
              </div>
            </div>
          </template>
        </div>
      </main>
    </div>
    <!-- 按需详情与原有编辑/批量弹层 -->
    <MenuResourceDetail
      :resource="activeResource" :icon="getRenderableIcon(activeResource)"
      :type-label="getResourceTypeText(activeResource?.resourceType)"
      :client-label="getClientDisplayName(activeResource?.clientCode)"
      :visible-options="visibleOptions" :child-summary="activeChildSummary"
      @enter="enterResource" @edit="editResourceFromDetail" @add="addResourceFromDetail" @delete="handleDelete"
    />
    <n-drawer v-model:show="drawerVisible" :width="drawerWidth" placement="right" :trap-focus="false">
      <n-drawer-content :title="drawerTitle" closable>
        <AiForm
          ref="formRef"
          v-model:value="formData"
          :schema="editSchema"
          :grid-cols="2"
          :show-actions="false"
          label-placement="left"
          label-width="96"
          size="small"
        >
          <template #icon="{ value, updateValue }">
            <n-tabs
              type="line"
              size="small"
              animated
              :value="formIconTab"
              @update:value="handleFormIconTabChange"
            >
              <n-tab-pane name="font" tab="字体图标">
                <div class="icon-selector-container">
                  <IconSelector :model-value="getFontIconValue(value)" @update:model-value="updateValue" />
                  <n-input
                    :value="getFontIconValue(value)"
                    placeholder="或手动输入图标名称，如 i-mdi-home"
                    clearable
                    @update:value="updateValue"
                  >
                    <template #prefix>
                      <IconRenderer v-if="getFontIconValue(value)" :icon="getFontIconValue(value)" />
                    </template>
                  </n-input>
                </div>
              </n-tab-pane>
              <n-tab-pane name="image" tab="图片图标">
                <div class="icon-upload-container">
                  <ImageUpload
                    :model-value="getImageIconValue(value)"
                    :limit="1"
                    :file-size="2"
                    :file-type="['png', 'jpg', 'jpeg', 'webp', 'gif', 'svg']"
                    business-type="menu-icon"
                    :show-tip="true"
                    value-type="string"
                    @success="fileData => updateValue(fileData?.fileId || fileData?.filePath || '')"
                    @update:model-value="updateValue"
                  />
                </div>
              </n-tab-pane>
            </n-tabs>
          </template>

          <template #path="{ value, updateValue, formData: slotFormData }">
            <div class="route-select-field">
              <NAutoComplete
                :value="value"
                :options="getAvailableRouteOptions(slotFormData, value)"
                clearable
                placeholder="输入或选择页面路由，如 /system/user"
                :render-label="renderRouteOptionLabel"
                @update:value="routePath => handleRoutePathChange(routePath, updateValue, slotFormData)"
              />
              <div class="route-select-hint">
                可搜索已有页面路由
              </div>
              <div v-if="slotFormData?.component" class="route-select-hint">
                组件路径：{{ slotFormData.component }}
              </div>
            </div>
          </template>

          <template #component="{ value, updateValue, formData: slotFormData }">
            <div class="route-select-field">
              <NAutoComplete
                :value="normalizeComponentValue(value)"
                :options="getAvailableComponentOptions(slotFormData, value)"
                clearable
                placeholder="输入或选择组件路径，如 system/user"
                :render-label="renderComponentOptionLabel"
                @update:value="component => handleComponentPathChange(component, updateValue, slotFormData)"
              />
              <div class="route-select-hint">
                可搜索已有组件路径
              </div>
            </div>
          </template>
        </AiForm>

        <template #footer>
          <n-space justify="end">
            <NButton @click="drawerVisible = false">
              取消
            </NButton>
            <NButton type="primary" :loading="submitLoading" @click="handleDrawerSubmit">
              保存
            </NButton>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <n-modal
      v-model:show="batchMigrateVisible"
      preset="card"
      title="批量迁移归属"
      :bordered="false"
      :mask-closable="!batchActionLoading"
      :style="{ width: 'min(520px, calc(100vw - 32px))' }"
    >
      <div class="batch-migrate-content">
        <n-alert type="info" :bordered="false">
          已选 {{ checkedResourceIds.length }} 项，实际迁移 {{ batchMigrateRootRows.length }} 个根节点
        </n-alert>
        <n-form label-placement="left" label-width="90" size="small">
          <n-form-item label="目标上级">
            <n-tree-select
              v-model:value="batchMigrateParentId"
              :options="batchMigrateParentOptions"
              clearable
              filterable
              default-expand-all
              key-field="value"
              label-field="label"
              children-field="children"
              placeholder="请选择目标上级资源"
            />
          </n-form-item>
        </n-form>
      </div>

      <template #footer>
        <n-space justify="end">
          <NButton :disabled="batchActionLoading" @click="batchMigrateVisible = false">
            取消
          </NButton>
          <NButton type="primary" :loading="batchActionLoading" @click="handleBatchMigrateSubmit">
            确认迁移
          </NButton>
        </n-space>
      </template>
    </n-modal>

    <IconSelector
      ref="tableIconSelectorRef"
      :trigger="false"
      :model-value="tableIconValue"
      @update:model-value="handleTableIconSelected"
    />
  </div>
</template>

<script setup>
import { useMenuPage } from './composables/useMenuPage'
import { menuPageLocalComponents } from './menuPageLocalComponents'

defineOptions({ name: 'MenuPage' })
const {
  AiForm,
  IconRenderer,
  IconSelector,
  ImageUpload,
  MenuResourceDetail,
  NAutoComplete,
  NButton,
  NCheckbox,
  NDropdown,
  NInputNumber,
  NTooltip,
} = menuPageLocalComponents
const api = useMenuPage()
const {
  activeChildSummary,
  activeFilterCount,
  activeResource,
  addResourceFromDetail,
  allDisplayRowsChecked,
  batchActionLoading,
  batchMigrateParentId,
  batchMigrateParentOptions,
  batchMigrateRootRows,
  batchMigrateVisible,
  checkedResourceIdSet,
  checkedResourceIds,
  clearCheckedResources,
  clientList,
  collapseNavigationTree,
  contextBreadcrumbs,
  currentClientCode,
  currentNode,
  displayRows,
  displayRowsCheckIndeterminate,
  drawerTitle,
  drawerVisible,
  drawerWidth,
  editResourceFromDetail,
  editSchema,
  enterResource,
  expandNavigationTree,
  formData,
  formIconTab,
  getAvailableComponentOptions,
  getAvailableRouteOptions,
  getClientDisplayName,
  getDisplayLevel,
  getFontIconValue,
  getImageIconValue,
  getMoreActionOptions,
  getPrimaryRouteText,
  getRenderableIcon,
  getResourceSubtitle,
  getResourceTypeConfig,
  getResourceTypeText,
  getSecondaryRouteText,
  handleAdd,
  handleAddRoot,
  handleBatchDelete,
  handleBatchMigrateSubmit,
  handleClientTabChange,
  handleComponentPathChange,
  handleDelete,
  handleDisplayRowsCheckedChange,
  handleDrawerSubmit,
  handleEdit,
  handleFormIconTabChange,
  handleInlineUpdate,
  handleMoreAction,
  handleNavigationExpandedKeys,
  handleNavigationSelect,
  handleResourceCheckedChange,
  handleResourceName,
  handleRoutePathChange,
  handleSortCommit,
  handleTableIconSelected,
  hasSearch,
  loadResourceTree,
  loading,
  navigationExpandedKeys,
  navigationSelectedKeys,
  navigationTreeData,
  normalizeComponentValue,
  openBatchMigrate,
  openResourceDetail,
  renderComponentOptionLabel,
  renderNavigationLabel,
  renderRouteOptionLabel,
  resetResourceFilters,
  resourceKeyword,
  resourceTypeFilter,
  resourceTypeFilterOptions,
  submitLoading,
  tableIconValue,
  treeKeyword,
  visibleFilter,
  visibleFilterOptions,
  visibleOptions,
  workspace,
  pageRef,
  formRef,
  tableIconSelectorRef,
} = api

// 保留历史页面公开编排接口；模板 ref 直接连接 composable 的真实实例。
defineExpose(api)
</script>

<style scoped src="./menuPage.css"></style>
