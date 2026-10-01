<template>
  <div ref="pageRef" class="system-menu-page">
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
        <div class="list-toolbar">
          <div class="list-context">
            <span class="context-title">{{ currentContextTitle }}</span>
            <span class="context-subtitle">{{ displayRows.length }} 项资源</span>
          </div>

          <div class="list-toolbar-right">
            <div class="batch-actions">
              <NCheckbox
                size="small"
                :checked="allDisplayRowsChecked"
                :indeterminate="displayRowsCheckIndeterminate"
                :disabled="displayRows.length === 0"
                @update:checked="handleDisplayRowsCheckedChange"
              >
                本页
              </NCheckbox>
              <span class="checked-count">已选 {{ checkedResourceIds.length }}</span>
              <NButton size="tiny" secondary :disabled="checkedResourceIds.length === 0" @click="openBatchMigrate">
                <template #icon>
                  <i class="i-material-symbols:drive-file-move-outline" />
                </template>
                批量迁移
              </NButton>
              <NButton
                size="tiny"
                type="error"
                secondary
                :disabled="checkedResourceIds.length === 0"
                :loading="batchActionLoading"
                @click="handleBatchDelete"
              >
                <template #icon>
                  <i class="i-material-symbols:delete-outline" />
                </template>
                批量删除
              </NButton>
            </div>

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
              <n-select
                v-model:value="resourceTypeFilter"
                size="small"
                clearable
                placeholder="类型"
                :options="resourceTypeFilterOptions"
                class="type-filter"
              />
              <n-select
                v-model:value="visibleFilter"
                size="small"
                clearable
                placeholder="状态"
                :options="visibleFilterOptions"
                class="visible-filter"
              />
            </div>
          </div>
        </div>

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
              <span>暂无资源</span>
            </div>

            <div
              v-for="row in displayRows"
              :key="row.id"
              class="resource-list-row"
              :class="{ 'is-active': activeResource?.id === row.id, 'is-checked': checkedResourceIdSet.has(row.id) }"
              @click="selectedRow = row"
            >
              <div class="resource-list-check" @click.stop>
                <NCheckbox
                  size="small"
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
                    <span class="resource-name">{{ row.resourceName || '-' }}</span>
                    <span class="resource-type-badge">
                      <span
                        class="resource-type-dot"
                        :style="{ backgroundColor: getResourceTypeConfig(row.resourceType).color || '#adb5bd' }"
                      />
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
                <button v-else class="sort-chip" @click.stop="row._editingSort = true">
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
                <NTooltip trigger="hover">
                  <template #trigger>
                    <NButton quaternary circle size="tiny" type="primary" class="table-icon-action" @click.stop="handleEdit(row)">
                      <template #icon>
                        <i class="i-material-symbols:edit-outline" />
                      </template>
                    </NButton>
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
                      <NButton quaternary circle size="tiny" class="table-icon-action" @click.stop>
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

      <aside class="detail-pane">
        <template v-if="activeResource">
          <div class="detail-head">
            <div class="detail-title-row">
              <IconRenderer
                v-if="getRenderableIcon(activeResource)"
                :icon="getRenderableIcon(activeResource)"
                :font-size="18"
                custom-style="color: var(--primary-color, #4c6ef5)"
              />
              <div class="detail-title-text">
                <span>{{ activeResource.resourceName }}</span>
                <small>{{ getResourceTypeText(activeResource.resourceType) }}</small>
              </div>
            </div>
            <DictTag
              :options="visibleOptions"
              :value="activeResource.visible"
              size="small"
              :bordered="false"
              force-tag
            />
          </div>

          <div class="detail-actions">
            <NButton size="small" type="primary" @click="handleEdit(activeResource)">
              <template #icon>
                <i class="i-material-symbols:edit-outline" />
              </template>
              编辑
            </NButton>
            <NButton size="small" @click="handleAdd(activeResource)">
              <template #icon>
                <i class="i-material-symbols:add" />
              </template>
              新增子项
            </NButton>
            <NButton size="small" type="error" secondary @click="handleDelete(activeResource)">
              <template #icon>
                <i class="i-material-symbols:delete-outline" />
              </template>
              删除
            </NButton>
          </div>

          <div class="detail-scroll cus-scroll-y">
            <div class="detail-section">
              <span class="section-label">基础信息</span>
              <dl class="detail-grid">
                <div>
                  <dt>客户端</dt>
                  <dd>{{ getClientDisplayName(activeResource.clientCode) }}</dd>
                </div>
                <div>
                  <dt>排序</dt>
                  <dd>{{ activeResource.sort ?? 0 }}</dd>
                </div>
                <div>
                  <dt>路由</dt>
                  <dd>{{ activeResource.path || '-' }}</dd>
                </div>
                <div>
                  <dt>组件</dt>
                  <dd>{{ activeResource.component || '-' }}</dd>
                </div>
                <div>
                  <dt>权限标识</dt>
                  <dd>{{ activeResource.perms || '-' }}</dd>
                </div>
                <div>
                  <dt>API</dt>
                  <dd>{{ activeResource.apiMethod || '-' }} {{ activeResource.apiUrl || '' }}</dd>
                </div>
              </dl>
            </div>

            <div class="detail-section">
              <span class="section-label">子资源概览</span>
              <div class="child-summary">
                <span>目录/菜单 {{ activeChildSummary.menu }}</span>
                <span>按钮 {{ activeChildSummary.button }}</span>
                <span>API {{ activeChildSummary.api }}</span>
              </div>
            </div>

            <div v-if="activeResource.remark" class="detail-section">
              <span class="section-label">备注</span>
              <p class="remark-text">
                {{ activeResource.remark }}
              </p>
            </div>
          </div>
        </template>

        <div v-else class="detail-empty">
          <i class="i-material-symbols:ads-click" />
          <span>选择左侧节点或中间列表项查看详情</span>
        </div>
      </aside>
    </div>

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

<script>
import { menuPageLocalComponents } from './menuPageLocalComponents'
import { useMenuPage } from './composables/useMenuPage'

export default {
  name: 'MenuPage',
  components: {
    ...menuPageLocalComponents,
  },
  setup() {
    return useMenuPage()
  },
}
</script>

<style scoped src="./menuPage.css"></style>
