<template>
  <div class="business-list-designer">
    <div class="list-designer-head">
      <div class="list-designer-title">
        <h3>列表设计</h3>
        <p>{{ layoutModeLabel }} · {{ designerPages.length }} 个页面</p>
      </div>
      <n-space class="list-designer-actions" size="small" align="center">
        <n-button class="list-toolbar-icon-button" circle size="small" secondary :disabled="!canUndo" title="撤销" @click="undoSchema">
          <template #icon>
            <n-icon><ArrowUndoOutline /></n-icon>
          </template>
        </n-button>
        <n-button class="list-toolbar-icon-button" circle size="small" secondary :disabled="!canRedo" title="重做" @click="redoSchema">
          <template #icon>
            <n-icon><ArrowRedoOutline /></n-icon>
          </template>
        </n-button>
        <n-dropdown trigger="click" :options="listMoreOptions" @select="handleListMoreSelect">
          <n-button class="list-toolbar-more-button" circle size="small" type="primary" title="更多操作">
            <template #icon>
              <n-icon><EllipsisHorizontalOutline /></n-icon>
            </template>
          </n-button>
        </n-dropdown>
      </n-space>
    </div>

    <div class="list-page-switch">
      <div class="page-switch-row">
        <div class="page-switch-title">
          <span class="page-switch-icon">P</span>
          <div class="page-switch-copy">
            <n-dropdown
              trigger="click"
              :options="listTemplateOptions"
              @select="updateListTemplate"
            >
              <button class="list-template-trigger" type="button">
                <n-icon><LayersOutline /></n-icon>
                <span>{{ activeTemplateLabel }}</span>
                <n-icon class="template-chevron">
                  <ChevronDownOutline />
                </n-icon>
              </button>
            </n-dropdown>
            <small>{{ pageTypeText(activeDesignerPage?.pageType) }} · {{ designerPages.length }} 个页面</small>
          </div>
        </div>
        <div class="page-tab-list">
          <button
            v-for="page in designerPages"
            :key="page.pageKey"
            type="button"
            class="page-tab-button"
            :class="[{ active: page.pageKey === activePageKey }, `type-${page.pageType || 'custom'}`]"
            @click="switchActivePage(page.pageKey)"
          >
            <span class="page-tab-name">{{ page.pageName || page.pageKey }}</span>
            <small>{{ pageTypeText(page.pageType) }} · {{ page.pageKey }}</small>
          </button>
          <n-button class="page-tool-button page-icon-button" circle size="small" secondary type="primary" title="新增页面" @click="addDesignerPage">
            <template #icon>
              <n-icon><AddOutline /></n-icon>
            </template>
          </n-button>
        </div>
        <div class="page-actions">
          <n-button class="page-tool-button page-icon-button" circle size="small" secondary :disabled="!canUndo" title="撤销" @click="undoSchema">
            <template #icon>
              <n-icon><ArrowUndoOutline /></n-icon>
            </template>
          </n-button>
          <n-button class="page-tool-button page-icon-button" circle size="small" secondary :disabled="!canRedo" title="重做" @click="redoSchema">
            <template #icon>
              <n-icon><ArrowRedoOutline /></n-icon>
            </template>
          </n-button>
          <n-button class="page-tool-button page-icon-button" circle size="small" secondary :title="pageSettingsExpanded ? '收起页面设置' : '页面设置'" @click="pageSettingsExpanded = !pageSettingsExpanded">
            <template #icon>
              <n-icon><DocumentTextOutline /></n-icon>
            </template>
          </n-button>
          <n-dropdown trigger="click" placement="bottom-end" :options="pageActionOptions" @select="handlePageActionSelect">
            <n-button class="page-tool-button page-icon-button" circle size="small" secondary title="页面操作">
              <template #icon>
                <n-icon><EllipsisHorizontalOutline /></n-icon>
              </template>
            </n-button>
          </n-dropdown>
        </div>
      </div>
      <div v-if="activeDesignerPage && pageSettingsExpanded" class="page-settings-popover">
        <div class="page-settings-head">
          <strong>页面设置</strong>
          <span>{{ activeDesignerPage.pageName || activeDesignerPage.pageKey }}</span>
        </div>
        <div class="page-config-row">
          <n-input
            :value="activeDesignerPage.pageName"
            size="small"
            placeholder="页面名称"
            @update:value="patchActivePage({ pageName: $event })"
          />
          <n-input
            :value="activeDesignerPage.pageKey"
            size="small"
            placeholder="页面编码"
            :disabled="isProtectedPage(activeDesignerPage.pageKey)"
            @update:value="updateActivePageKey($event)"
          />
          <n-select
            :value="activeDesignerPage.pageType || 'custom'"
            :options="pageTypeOptions"
            size="small"
            @update:value="patchActivePage({ pageType: $event })"
          />
          <n-input
            :value="activeDesignerPage.routePath"
            size="small"
            placeholder="路由片段/页面标识，如 dialog/customer"
            title="当前多页面仍由业务对象运行页统一承载，这里用于事件跳转、弹窗页和自定义页面识别，不是单独前端路由文件"
            @update:value="patchActivePage({ routePath: $event })"
          />
          <n-input
            :value="activeDesignerPage.description"
            size="small"
            placeholder="页面说明"
            @update:value="patchActivePage({ description: $event })"
          />
        </div>
        <div class="page-param-row">
          <div class="page-param-title">
            页面入参
          </div>
          <div
            v-for="(param, paramIdx) in (activeDesignerPage.params || [])"
            :key="paramIdx"
            class="page-param-item"
          >
            <n-input
              :value="param.name"
              size="tiny"
              placeholder="参数名"
              @update:value="updateActivePageParam(paramIdx, { name: normalizePageParamName($event) })"
            />
            <n-input
              :value="param.value"
              size="tiny"
              placeholder="默认值 / 字段映射"
              @update:value="updateActivePageParam(paramIdx, { value: $event })"
            />
            <n-button size="tiny" quaternary circle title="删除参数" @click="removeActivePageParam(paramIdx)">
              <template #icon>
                <n-icon><TrashOutline /></n-icon>
              </template>
            </n-button>
          </div>
          <n-button class="page-param-add-button" size="tiny" dashed @click="addActivePageParam">
            + 参数
          </n-button>
        </div>
        <div class="page-settings-footer">
          <n-button size="tiny" secondary title="复制页面" @click="duplicateActivePage">
            <template #icon>
              <n-icon><CopyOutline /></n-icon>
            </template>
            复制
          </n-button>
          <n-button size="tiny" secondary title="重置当前页面布局" @click="resetActivePageLayout">
            <template #icon>
              <n-icon><RefreshOutline /></n-icon>
            </template>
            重置
          </n-button>
          <n-popconfirm
            :show-icon="false"
            positive-text="清空"
            negative-text="取消"
            @positive-click="clearActivePageLayout"
          >
            <template #trigger>
              <n-button size="tiny" secondary title="清空当前页面布局">
                <template #icon>
                  <n-icon><CloseCircleOutline /></n-icon>
                </template>
                清空
              </n-button>
            </template>
            清空当前页面画布上的所有组件？
          </n-popconfirm>
          <n-popconfirm
            :show-icon="false"
            positive-text="删除"
            negative-text="取消"
            :disabled="isProtectedPage(activePageKey)"
            @positive-click="removeActivePage"
          >
            <template #trigger>
              <n-button size="tiny" secondary type="error" :disabled="isProtectedPage(activePageKey)" title="删除当前页面">
                <template #icon>
                  <n-icon><TrashOutline /></n-icon>
                </template>
                删除
              </n-button>
            </template>
            删除当前页面及布局？列表页不允许删除。
          </n-popconfirm>
        </div>
      </div>
    </div>

    <section v-if="!defaultViewOnly" class="list-custom-actions-entry">
      <div class="custom-actions-entry-copy">
        <strong>自定义操作</strong>
        <span>配置列表顶部按钮和每行操作按钮，当前共 {{ listCustomActions.length }} 个。</span>
      </div>
      <div class="custom-actions-entry-preview">
        <span v-for="action in customActionPreviewItems" :key="action.clientKey || action.key" class="custom-action-preview-chip">
          {{ action.positionLabel }} · {{ action.label || '自定义操作' }}
        </span>
        <span v-if="!customActionPreviewItems.length" class="custom-action-preview-empty">
          暂无自定义操作
        </span>
      </div>
      <n-space class="custom-actions-entry-buttons" size="small">
        <n-button size="small" secondary @click="createToolbarCustomAction">
          新增顶部按钮
        </n-button>
        <n-button size="small" secondary @click="createRowCustomAction">
          新增行操作
        </n-button>
        <n-button size="small" type="primary" @click="openCustomActionManager">
          配置全部操作
        </n-button>
      </n-space>
    </section>

    <div class="list-designer-body">
      <main class="list-workspace">
        <ListPageGridDesigner
          ref="listGridDesignerRef"
          :model-value="designerGridLayout"
          :fields="designFields"
          :model-schema="effectiveModelSchema"
          :layout-type="localSchema.layoutType"
          :page-name="activeDesignerPage?.pageName || '列表页'"
          :pages="designerPages"
          :form-options="formOptions"
          :runtime-crud-props="designerRuntimeCrudProps"
          :custom-actions="visibleListCustomActions"
          :custom-actions-editable="!defaultViewOnly"
          @update:model-value="handleGridLayoutUpdate"
          @update:custom-actions="handleListCustomActionsUpdate"
        />
      </main>
    </div>

    <n-modal
      v-model:show="listPreviewVisible"
      preset="card"
      title="预览当前列表"
      class="list-preview-modal"
      :bordered="false"
      :style="{ width: 'calc(100vw - 32px)', maxWidth: 'calc(100vw - 32px)', minWidth: 0, height: 'calc(100vh - 32px)' }"
    >
      <ListPageGridDesigner
        :model-value="designerPreviewGridLayout"
        :fields="designFields"
        :model-schema="effectiveModelSchema"
        :layout-type="localSchema.layoutType"
        :page-name="activeDesignerPage?.pageName || '列表页'"
        :pages="designerPages"
        :form-options="formOptions"
        :runtime-crud-props="designerRuntimeCrudProps"
        :custom-actions="visibleListCustomActions"
        readonly
      />
    </n-modal>
  </div>
</template>

<script setup>
import {
  AddOutline,
  ArrowRedoOutline,
  ArrowUndoOutline,
  ChevronDownOutline,
  CloseCircleOutline,
  CopyOutline,
  DocumentTextOutline,
  EllipsisHorizontalOutline,
  FunnelOutline,
  GridOutline,
  LayersOutline,
  ListOutline,
  PrintOutline,
  RefreshOutline,
  SparklesOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { NIcon as NaiveIcon, useMessage } from 'naive-ui'
import { computed, h, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { saveBusinessObjectActions, saveBusinessObjectDesigner, saveBusinessObjectListLayout } from '@/api/business-app'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import {
  applyCrudHookRules,
  CRUD_HOOK_RULE_TARGETS,
  normalizeCrudHookRules,
} from '@/components/lowcode-builder/page/crud-hook-rules'
import ListPageGridDesigner from '@/components/lowcode-builder/page/ListPageGridDesigner.vue'
import {
  applyGridLayoutToZones,
  bootstrapGridLayoutFromZones,
  buildPageDesignModelSchema,
  createDefaultListGridLayout,
  createDefaultPageSchema,
  createPageModelRef,
  isPageFieldVisible,
  LIST_PAGE_DESIGN_WIDTH,
  LIST_PAGE_GRID_BASE_COL_WIDTH,
  LIST_PAGE_GRID_GAP,
  resolveDefaultTreeConfig,
  syncGridLayoutWithModel,
  syncPageSchemaWithModel,
} from '@/components/lowcode-builder/page/page-schema'
import {
  appendDesignPreviewToApiConfig,
  appendDesignPreviewToApiValue,
} from '@/components/lowcode-builder/shared/runtime-crud-props'
import { createViewSchemaFromPageSchema } from './form-first/viewSchema'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  modelValue: {
    type: Object,
    default: null,
  },
  modelSchema: {
    type: Object,
    default: () => ({}),
  },
  fields: {
    type: Array,
    default: () => [],
  },
  viewSchema: {
    type: Object,
    default: null,
  },
  formOptions: {
    type: Array,
    default: () => [],
  },
  designerOptions: {
    type: Object,
    default: () => ({}),
  },
  designerActions: {
    type: Array,
    default: () => [],
  },
  defaultViewOnly: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'update:viewSchema', 'update:designerActions', 'saved', 'dirtyChange'])

const message = useMessage()
const saving = ref(false)
const undoStack = ref([])
const redoStack = ref([])
const listGridDesignerRef = ref(null)
const activePageKey = ref('list')
const pageSettingsExpanded = ref(false)
const HISTORY_LIMIT = 50
let applyingExternalSchema = false
const VALID_PAGE_TYPES = new Set(['list', 'create', 'edit', 'dialog', 'drawer', 'custom'])

const baseModelSchema = computed(() => {
  const modelFields = props.modelSchema?.fields || []
  return {
    ...(props.modelSchema || {}),
    fields: modelFields.length ? modelFields : props.fields.map(toPageField),
  }
})

const localSchema = ref(resolveSchema(props.modelValue, resolveDesignModelSchema(props.modelValue, baseModelSchema.value)))
const effectiveModelSchema = computed(() => resolveDesignModelSchema(localSchema.value, baseModelSchema.value))
const designFields = computed(() => effectiveModelSchema.value.fields || [])
const layoutModeLabel = computed(() => resolveLayoutModeLabel(localSchema.value.layoutType))
const canUndo = computed(() => undoStack.value.length > 0)
const canRedo = computed(() => redoStack.value.length > 0)
const listPreviewVisible = ref(false)
const templateSelectValue = ref(resolveTemplateSelectValue(localSchema.value.layoutType))
const listCustomActions = ref([])
const customActionPreviewItems = computed(() => {
  return listCustomActions.value.slice(0, 6).map(action => ({
    ...action,
    positionLabel: action.position === 'toolbar' ? '顶部' : action.position === 'detail' ? '详情' : '行内',
  }))
})
const pageTypeOptions = [
  { label: '列表页', value: 'list' },
  { label: '新增页', value: 'create' },
  { label: '编辑页', value: 'edit' },
  { label: '弹窗页', value: 'dialog' },
  { label: '抽屉页', value: 'drawer' },
  { label: '自定义页', value: 'custom' },
]
const listTemplateOptions = [
  { label: '标准列表模板', key: 'simple-crud', icon: renderMenuIcon(ListOutline) },
  { label: '左树右表模板', key: 'tree-crud', icon: renderMenuIcon(GridOutline) },
]
const activeTemplateLabel = computed(() => {
  return listTemplateOptions.find(item => item.key === templateSelectValue.value)?.label || '列表模板'
})
const listMoreOptions = computed(() => [
  {
    label: '按字段生成查询条件',
    key: 'resetSearchFields',
    icon: renderMenuIcon(FunnelOutline),
  },
  {
    label: '按字段生成表格列',
    key: 'resetTableFields',
    icon: renderMenuIcon(GridOutline),
  },
  {
    type: 'divider',
    key: 'listMoreDivider',
  },
  {
    label: '新增空白页面',
    key: 'addPage',
    icon: renderMenuIcon(AddOutline),
  },
  {
    label: '打印列表',
    key: 'printDesign',
    icon: renderMenuIcon(PrintOutline),
  },
  {
    type: 'divider',
    key: 'pageDivider',
  },
  {
    label: '重置当前列表',
    key: 'resetListSchema',
    icon: renderMenuIcon(RefreshOutline),
  },
])
const pageActionOptions = computed(() => [
  { label: '按字段生成查询条件', key: 'resetSearchFields', icon: renderMenuIcon(FunnelOutline) },
  { label: '按字段生成表格列', key: 'resetTableFields', icon: renderMenuIcon(GridOutline) },
  { type: 'divider', key: 'pageActionDivider1' },
  { label: '新增空白页面', key: 'addPage', icon: renderMenuIcon(AddOutline) },
  { type: 'divider', key: 'pageActionDivider2' },
  { label: '复制页面', key: 'duplicate', icon: renderMenuIcon(CopyOutline) },
  { label: '重置布局', key: 'reset', icon: renderMenuIcon(SparklesOutline) },
  { label: '重置当前列表', key: 'resetListSchema', icon: renderMenuIcon(RefreshOutline) },
])
const designerPages = computed(() => localSchema.value.pages || [])
const activeDesignerPage = computed(() => designerPages.value.find(page => page.pageKey === activePageKey.value) || designerPages.value[0] || null)
const currentPageGridLayout = computed(() => activeDesignerPage.value?.gridLayout || localSchema.value.listGridLayout || {})
const previewGridLayout = computed(() => {
  const persistedGrid = currentPageGridLayout.value || localSchema.value.listGridLayout
  const source = Array.isArray(persistedGrid?.items)
    ? persistedGrid
    : bootstrapGridLayoutFromZones(localSchema.value.zones || [], effectiveModelSchema.value, { layoutType: localSchema.value.layoutType })
  return syncGridLayoutWithModel(source, effectiveModelSchema.value, { layoutType: localSchema.value.layoutType })
})
function filterDesignerGridBlocks(grid) {
  if (!grid || !Array.isArray(grid.items))
    return grid
  return {
    ...grid,
    items: grid.items.filter(item => item.blockType !== 'page-title'),
  }
}
const designerGridLayout = computed(() => filterDesignerGridBlocks(currentPageGridLayout.value))
const designerPreviewGridLayout = computed(() => filterDesignerGridBlocks(previewGridLayout.value))
const visibleListCustomActions = computed(() => props.defaultViewOnly ? [] : listCustomActions.value)
const designerRuntimeCrudProps = computed(() => buildDesignerRuntimeCrudProps(localSchema.value, designFields.value, visibleListCustomActions.value))

function resolveTemplateSelectValue(layoutType = '') {
  return layoutType === 'tree-crud' ? 'tree-crud' : 'simple-crud'
}

function renderMenuIcon(icon) {
  if (!icon)
    return undefined
  return () => h(NaiveIcon, null, { default: () => h(icon) })
}

watch(
  () => props.designerActions,
  (value) => {
    const mappedActions = normalizeDesignerActionsForList(value)
    const fallbackActions = mappedActions.length ? mappedActions : collectSchemaCustomActions(localSchema.value)
    if (!isSameSchema(fallbackActions, listCustomActions.value))
      listCustomActions.value = fallbackActions
  },
  { deep: true, immediate: true },
)

watch(
  () => props.modelValue,
  (value) => {
    const next = resolveSchema(value, resolveDesignModelSchema(value, baseModelSchema.value))
    setLocalSchema(next, { external: true })
  },
  { deep: true },
)

watch(
  baseModelSchema,
  (value) => {
    const next = resolveSchema(localSchema.value, resolveDesignModelSchema(localSchema.value, value))
    setLocalSchema(next)
  },
  { deep: true },
)

watch(
  localSchema,
  (value) => {
    if (applyingExternalSchema) {
      applyingExternalSchema = false
      return
    }
    if (!isSameSchema(value, props.modelValue)) {
      emit('update:modelValue', cloneSchema(value))
      emit('update:viewSchema', cloneSchema(buildCurrentViewSchema(value)))
      emit('dirtyChange', true)
    }
  },
  { deep: true },
)

watch(
  () => localSchema.value.layoutType,
  (value) => {
    templateSelectValue.value = resolveTemplateSelectValue(value)
  },
  { immediate: true },
)

function createCleanTemplateGridLayout(layoutType, schema = localSchema.value) {
  const defaultGrid = createDefaultListGridLayout(effectiveModelSchema.value, { layoutType })
  const listPage = (schema.pages || []).find(page => page.pageKey === 'list')
  const previousGrid = listPage?.gridLayout || schema.listGridLayout || {}
  const previousItems = previousGrid.items || []
  const zones = schema.zones || []
  const searchZone = zones.find(zone => zone.zoneKey === 'search') || {}
  const tableZone = zones.find(zone => zone.zoneKey === 'table') || {}
  const previousCrud = previousItems.find(item => item.blockType === 'AiCrudPage')
    || previousItems.find(item => item.blockType === 'data-table')
    || previousItems.find(item => item.blockType === 'AiTable')
  const previousTree = previousItems.find(item => item.blockType === 'tree-panel')
  const previousToolbar = previousItems.find(item => item.blockType === 'toolbar')
  const tableProps = tableZone.props || {}
  const searchProps = searchZone.props || {}
  const toolbarActions = previousToolbar?.props?.actions || []
  const hasPreviousToolbar = Boolean(previousToolbar)
  const templateBlockTypes = new Set(['tree-panel', 'AiCrudPage', 'data-table', 'AiTable', 'search-form', 'toolbar'])

  const items = defaultGrid.items.map((item) => {
    if (item.blockType === 'tree-panel') {
      return {
        ...item,
        props: {
          ...(item.props || {}),
          ...(tableProps.treeConfig || {}),
          ...(previousTree?.props || {}),
          style: item.props?.style,
          events: previousTree?.props?.events || item.props?.events || [],
        },
      }
    }
    if (item.blockType === 'AiCrudPage') {
      const previousProps = previousCrud?.props || {}
      const normalizedTableProps = layoutType === 'tree-crud'
        ? tableProps
        : omitTreeRuntimeProps(tableProps)
      const normalizedPreviousProps = layoutType === 'tree-crud'
        ? previousProps
        : omitTreeRuntimeProps(previousProps)
      const fieldRefs = previousCrud?.fieldRefs?.length
        ? previousCrud.fieldRefs
        : tableZone.fieldRefs?.length
          ? tableZone.fieldRefs
          : item.fieldRefs
      const hasPreviousSearchRefs = Object.prototype.hasOwnProperty.call(normalizedPreviousProps, 'searchFieldRefs')
      const hasSearchZoneRefs = Array.isArray(searchZone.fieldRefs)
      const searchFieldRefs = hasPreviousSearchRefs
        ? normalizedPreviousProps.searchFieldRefs || []
        : hasSearchZoneRefs
          ? searchZone.fieldRefs || []
          : item.props?.searchFieldRefs || []
      return {
        ...item,
        fieldRefs,
        props: {
          ...(item.props || {}),
          ...normalizedTableProps,
          ...normalizedPreviousProps,
          title: normalizedPreviousProps.title || normalizedTableProps.title || item.props?.title,
          showSearch: searchZone.enabled !== false,
          showImport: normalizedPreviousProps.showImport ?? normalizedTableProps.showImport ?? (hasPreviousToolbar ? toolbarActions.includes('import') : item.props?.showImport ?? true),
          showExport: normalizedPreviousProps.showExport ?? normalizedTableProps.showExport ?? (hasPreviousToolbar ? toolbarActions.includes('export') : item.props?.showExport ?? true),
          hideBatchDelete: normalizedPreviousProps.hideBatchDelete ?? normalizedTableProps.hideBatchDelete ?? (hasPreviousToolbar ? !toolbarActions.includes('batch-delete') : item.props?.hideBatchDelete ?? false),
          enableCustomQuery: normalizedPreviousProps.enableCustomQuery ?? normalizedTableProps.enableCustomQuery ?? (hasPreviousToolbar ? toolbarActions.includes('custom-query') : item.props?.enableCustomQuery ?? true),
          defaultSortField: normalizedPreviousProps.defaultSortField || normalizedTableProps.defaultSortField || 'id',
          defaultSortOrder: normalizedPreviousProps.defaultSortOrder || normalizedTableProps.defaultSortOrder || 'desc',
          fieldSettings: {
            ...(normalizedTableProps.fieldSettings || {}),
            ...(normalizedPreviousProps.fieldSettings || {}),
          },
          searchFieldRefs,
          searchFieldSettings: {
            ...(searchProps.fieldSettings || {}),
            ...(normalizedPreviousProps.searchFieldSettings || {}),
          },
          style: item.props?.style,
          events: normalizedPreviousProps.events || item.props?.events || [],
        },
      }
    }
    return item
  })
  const preservedCustomItems = previousItems
    .filter(item => !templateBlockTypes.has(item.blockType))
    .map((item) => {
      if (layoutType !== 'tree-crud')
        return item
      const gridX = Number(item.gridX || 0)
      const styleX = Number(item.props?.style?.x ?? gridX * (LIST_PAGE_GRID_BASE_COL_WIDTH + LIST_PAGE_GRID_GAP))
      if (gridX >= 3 && styleX >= 3 * (LIST_PAGE_GRID_BASE_COL_WIDTH + LIST_PAGE_GRID_GAP))
        return item
      return {
        ...item,
        gridX: 3,
        gridW: Math.min(9, Math.max(1, Number(item.gridW || 9))),
        props: {
          ...(item.props || {}),
          style: {
            ...(item.props?.style || {}),
            x: Math.max(3 * (LIST_PAGE_GRID_BASE_COL_WIDTH + LIST_PAGE_GRID_GAP), Number(item.props?.style?.x || 0)),
            widthMode: item.props?.style?.widthMode === 'fixed' ? 'fixed' : item.props?.style?.widthMode,
          },
        },
      }
    })

  return syncGridLayoutWithModel(
    {
      ...defaultGrid,
      items: [...items, ...preservedCustomItems],
    },
    effectiveModelSchema.value,
    { layoutType },
  )
}

function updateTreeLayoutEnabled(enabled) {
  const nextLayoutType = enabled
    ? 'tree-crud'
    : isRelationLayout(localSchema.value, effectiveModelSchema.value) ? 'master-detail-crud' : 'simple-crud'
  const next = {
    ...localSchema.value,
    layoutType: nextLayoutType,
    zones: updateTreeZone(localSchema.value.zones || [], enabled),
  }

  const templateGrid = createCleanTemplateGridLayout(nextLayoutType, next)
  next.listGridLayout = templateGrid
  next.pages = updateDesignerPageGrid(next.pages || [], 'list', templateGrid)
  next.zones = applyGridLayoutToZones(next.zones || [], templateGrid, effectiveModelSchema.value)

  setLocalSchema(resolveSchema(next, effectiveModelSchema.value))
}

function updateListTemplate(value) {
  if (!value)
    return
  templateSelectValue.value = value
  updateTreeLayoutEnabled(value === 'tree-crud')
}

function handleGridLayoutUpdate(layout) {
  const originalGrid = currentPageGridLayout.value
  const pageTitleBlocks = Array.isArray(originalGrid?.items)
    ? originalGrid.items.filter(item => item.blockType === 'page-title')
    : []
  const mergedLayout = pageTitleBlocks.length
    ? { ...layout, items: [...pageTitleBlocks, ...(layout.items || [])] }
    : layout
  const synced = syncGridLayoutWithModel(mergedLayout, effectiveModelSchema.value, { layoutType: localSchema.value.layoutType })
  const pageKey = activePageKey.value || 'list'
  const pages = updateDesignerPageGrid(localSchema.value.pages || [], pageKey, synced)
  const nextSchema = {
    ...localSchema.value,
    pages,
  }
  if (pageKey === 'list') {
    nextSchema.listGridLayout = synced
    nextSchema.zones = applyGridLayoutToZones(localSchema.value.zones || [], synced, effectiveModelSchema.value)
  }
  setLocalSchema(nextSchema)
}

function switchActivePage(pageKey) {
  activePageKey.value = pageKey || 'list'
}

function handleListMoreSelect(key = '') {
  if (key === 'resetSearchFields') {
    resetZoneFields('search')
    return
  }
  if (key === 'resetTableFields') {
    resetZoneFields('table')
    return
  }
  if (key === 'addPage') {
    addDesignerPage()
    return
  }
  if (key === 'printDesign') {
    openPrintPreview()
    return
  }
  if (key === 'resetListSchema')
    resetListSchema()
}

/** 打开预览并触发浏览器打印，像 Word 一样输出当前列表设计稿 */
function openPrintPreview() {
  listPreviewVisible.value = true
  setTimeout(() => window.print(), 400)
}

function handlePageActionSelect(key = '') {
  if (['resetSearchFields', 'resetTableFields', 'addPage', 'resetListSchema'].includes(key)) {
    handleListMoreSelect(key)
    return
  }
  if (key === 'duplicate') {
    duplicateActivePage()
    return
  }
  if (key === 'reset') {
    resetActivePageLayout()
  }
}

function addDesignerPage() {
  const pageKey = createUniquePageKey('page')
  const page = {
    pageKey,
    pageName: `页面 ${designerPages.value.length + 1}`,
    pageType: 'custom',
    routePath: '',
    description: '',
    params: [],
    gridLayout: {
      ...createDefaultListGridLayout(effectiveModelSchema.value, { layoutType: localSchema.value.layoutType }),
      items: [],
    },
  }
  setLocalSchema({
    ...localSchema.value,
    pages: [...(localSchema.value.pages || []), page],
  })
  activePageKey.value = pageKey
}

function duplicateActivePage() {
  const source = activeDesignerPage.value
  if (!source)
    return
  const pageKey = createUniquePageKey(source.pageKey || 'page')
  const copy = cloneSchema(source)
  copy.pageKey = pageKey
  copy.pageName = `${source.pageName || '页面'} 副本`
  copy.pageType = source.pageType === 'list' ? 'custom' : normalizeDesignerPageType(source.pageType || 'custom')
  copy.routePath = ''
  setLocalSchema({
    ...localSchema.value,
    pages: [...(localSchema.value.pages || []), copy],
  })
  activePageKey.value = pageKey
}

function removeActivePage() {
  if (isProtectedPage(activePageKey.value))
    return
  const removedKey = activePageKey.value
  const nextPages = (localSchema.value.pages || []).filter(page => page.pageKey !== activePageKey.value)
  const nextActive = nextPages.find(page => page.pageKey === 'list')?.pageKey || nextPages[0]?.pageKey || 'list'
  setLocalSchema({
    ...localSchema.value,
    pages: nextPages,
    removedPageKeys: Array.from(new Set([...(localSchema.value.removedPageKeys || []), removedKey])),
  })
  activePageKey.value = nextActive
}

function resetActivePageLayout() {
  const page = activeDesignerPage.value
  if (!page)
    return
  const gridLayout = page.pageKey === 'list' || page.pageType === 'list'
    ? createDefaultListGridLayout(effectiveModelSchema.value, { layoutType: localSchema.value.layoutType })
    : {
        ...createDefaultListGridLayout(effectiveModelSchema.value, { layoutType: localSchema.value.layoutType }),
        items: [],
      }
  const pages = updateDesignerPageGrid(localSchema.value.pages || [], page.pageKey, gridLayout)
  const nextSchema = {
    ...localSchema.value,
    pages,
  }
  if (page.pageKey === 'list') {
    nextSchema.listGridLayout = gridLayout
    nextSchema.zones = applyGridLayoutToZones(localSchema.value.zones || [], gridLayout, effectiveModelSchema.value)
  }
  setLocalSchema(nextSchema)
}

function clearActivePageLayout() {
  const page = activeDesignerPage.value
  if (!page)
    return
  const gridLayout = {
    ...(page.gridLayout || {}),
    cols: 12,
    rowHeight: 32,
    gap: 8,
    designWidth: page.gridLayout?.designWidth || LIST_PAGE_DESIGN_WIDTH,
    layoutType: localSchema.value.layoutType,
    items: [],
  }
  const pages = updateDesignerPageGrid(localSchema.value.pages || [], page.pageKey, gridLayout)
  const nextSchema = {
    ...localSchema.value,
    pages,
  }
  if (page.pageKey === 'list') {
    nextSchema.listGridLayout = gridLayout
    nextSchema.zones = applyGridLayoutToZones(localSchema.value.zones || [], gridLayout, effectiveModelSchema.value)
  }
  setLocalSchema(nextSchema)
}

function patchActivePage(patch) {
  const page = activeDesignerPage.value
  if (!page)
    return
  setLocalSchema({
    ...localSchema.value,
    pages: (localSchema.value.pages || []).map(item => item.pageKey === page.pageKey
      ? { ...item, ...patch }
      : item),
  })
}

function updateActivePageKey(value) {
  const page = activeDesignerPage.value
  if (!page || isProtectedPage(page.pageKey))
    return
  const nextKey = normalizePageKey(value)
  if (!nextKey || nextKey === page.pageKey)
    return
  if ((localSchema.value.pages || []).some(item => item.pageKey === nextKey)) {
    message.warning('页面编码已存在')
    return
  }
  const rewrittenSchema = rewritePageEventTargets(localSchema.value, page.pageKey, nextKey)
  const pages = (rewrittenSchema.pages || []).map(item => item.pageKey === page.pageKey
    ? { ...item, pageKey: nextKey }
    : item)
  setLocalSchema({
    ...rewrittenSchema,
    pages,
  })
  activePageKey.value = nextKey
}

function addActivePageParam() {
  const page = activeDesignerPage.value
  if (!page)
    return
  patchActivePage({
    params: [...(page.params || []), { name: '', value: '' }],
  })
}

function updateActivePageParam(paramIdx, patch) {
  const page = activeDesignerPage.value
  if (!page)
    return
  const params = [...(page.params || [])]
  params[paramIdx] = { ...(params[paramIdx] || {}), ...patch }
  patchActivePage({ params })
}

function removeActivePageParam(paramIdx) {
  const page = activeDesignerPage.value
  if (!page)
    return
  const params = [...(page.params || [])]
  params.splice(paramIdx, 1)
  patchActivePage({ params })
}

function createUniquePageKey(prefix = 'page') {
  const base = normalizePageKey(prefix) || 'page'
  const existing = new Set((localSchema.value.pages || []).map(page => page.pageKey))
  let index = 1
  let key = `${base}_${Date.now()}`
  while (existing.has(key)) {
    key = `${base}_${Date.now()}_${index}`
    index += 1
  }
  return key
}

function normalizePageKey(value) {
  return String(value || '')
    .trim()
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W/g, '_')
    .replace(/_+/g, '_')
    .toLowerCase()
    .replace(/^[^a-z]+/, '')
}

function normalizePageParamName(value) {
  return String(value || '')
    .trim()
    .replace(/[^\w.-]/g, '')
}

function isProtectedPage(pageKey = '') {
  return pageKey === 'list'
}

function pageTypeText(pageType = 'custom') {
  return pageTypeOptions.find(item => item.value === pageType)?.label || '自定义页'
}

function normalizeDesignerPageType(pageType = 'custom') {
  const value = String(pageType || 'custom')
  return VALID_PAGE_TYPES.has(value) ? value : 'custom'
}

function rewritePageEventTargets(schema, oldKey, nextKey) {
  const cloned = cloneSchema(schema)
  cloned.pages = (cloned.pages || []).map(page => ({
    ...page,
    gridLayout: rewriteGridLayoutPageEventTargets(page.gridLayout, oldKey, nextKey),
  }))
  cloned.listGridLayout = rewriteGridLayoutPageEventTargets(cloned.listGridLayout, oldKey, nextKey)
  return cloned
}

function rewriteGridLayoutPageEventTargets(gridLayout, oldKey, nextKey) {
  if (!gridLayout)
    return gridLayout
  return {
    ...gridLayout,
    items: rewriteBlocksPageEventTargets(gridLayout.items || [], oldKey, nextKey),
  }
}

function rewriteBlocksPageEventTargets(blocks = [], oldKey, nextKey) {
  return blocks.map((block) => {
    const props = { ...(block.props || {}) }
    if (Array.isArray(props.events)) {
      props.events = props.events.map(event => event.targetPageKey === oldKey
        ? { ...event, targetPageKey: nextKey }
        : event)
    }
    if (props.fieldSettings && typeof props.fieldSettings === 'object') {
      props.fieldSettings = Object.fromEntries(Object.entries(props.fieldSettings).map(([fieldKey, setting]) => [
        fieldKey,
        setting?.targetPageKey === oldKey ? { ...setting, targetPageKey: nextKey } : setting,
      ]))
    }
    if (Array.isArray(props.tabs)) {
      props.tabs = props.tabs.map(tab => ({
        ...tab,
        children: rewriteBlocksPageEventTargets(tab.children || [], oldKey, nextKey),
      }))
    }
    return {
      ...block,
      props,
      children: rewriteBlocksPageEventTargets(block.children || [], oldKey, nextKey),
    }
  })
}

function resetZoneFields(zoneKey = '') {
  const nextRefs = designFields.value
    .filter(field => isPageFieldVisible(field, zoneKey))
    .map(field => field.field)
  const targetBlockTypes = zoneKey === 'search'
    ? ['search-form', 'AiCrudPage']
    : ['data-table', 'AiTable', 'AiCrudPage']
  const nextGridLayout = {
    ...(localSchema.value.listGridLayout || {}),
    items: (localSchema.value.listGridLayout?.items || []).map((item) => {
      if (!targetBlockTypes.includes(item.blockType))
        return item
      if (zoneKey === 'search' && item.blockType === 'AiCrudPage') {
        return {
          ...item,
          props: {
            ...(item.props || {}),
            searchFieldRefs: nextRefs,
          },
        }
      }
      return { ...item, fieldRefs: nextRefs }
    }),
  }
  setLocalSchema({
    ...localSchema.value,
    listLayoutMode: 'grid',
    listGridLayout: nextGridLayout,
    pages: updateDesignerPageGrid(localSchema.value.pages || [], 'list', nextGridLayout),
    zones: applyGridLayoutToZones(localSchema.value.zones || [], nextGridLayout, effectiveModelSchema.value),
  })
}

function resetListSchema() {
  const nextLayoutType = localSchema.value.layoutType
  const nextSchema = resolveSchema(createDefaultPageSchema(effectiveModelSchema.value), effectiveModelSchema.value)
  const nextZones = nextLayoutType === 'tree-crud'
    ? updateTreeZone(nextSchema.zones || [], true)
    : nextSchema.zones
  const nextGridLayout = syncGridLayoutWithModel(createDefaultListGridLayout(effectiveModelSchema.value, { layoutType: nextLayoutType }), effectiveModelSchema.value, { layoutType: nextLayoutType })
  setLocalSchema({
    ...nextSchema,
    layoutType: nextLayoutType,
    zones: applyGridLayoutToZones(nextZones || [], nextGridLayout, effectiveModelSchema.value),
    listLayoutMode: 'grid',
    listGridLayout: nextGridLayout,
    pages: ensureDesignerPages({ ...nextSchema, listGridLayout: nextGridLayout, layoutType: nextLayoutType }, effectiveModelSchema.value),
  })
}

async function saveLayout() {
  if (!props.objectId)
    return
  const schema = normalizeSchemaForSave(resolveSchema(localSchema.value, effectiveModelSchema.value))
  const designerActions = props.defaultViewOnly
    ? cloneSchema(props.designerActions || [])
    : buildDesignerActionsForSave()
  const invalidApiAction = props.defaultViewOnly
    ? null
    : designerActions.find(action => action.status !== 0 && isInvalidDesignerApiAction(action))
  if (invalidApiAction) {
    const actionName = invalidApiAction.actionName || '自定义操作'
    message.warning(`请为“${actionName}”配置接口地址或能力标识`)
    throw new Error(`请为“${actionName}”配置接口地址或能力标识`)
  }
  saving.value = true
  try {
    if (!props.defaultViewOnly)
      await saveBusinessObjectActions(props.objectId, designerActions)
    await saveBusinessObjectListLayout(props.objectId, {
      layoutKey: 'list',
      layoutName: '列表布局',
      layoutType: schema.layoutType,
      pageSchema: cloneSchema(schema),
      zones: schema.zones?.filter(zone => ['search', 'table'].includes(zone.zoneKey)) || [],
      settings: {
        listLayoutMode: 'grid',
      },
    })
    const viewSchema = buildCurrentViewSchema(schema)
    const designerPayload = {
      pageSchema: cloneSchema(schema),
      viewSchema: cloneSchema(viewSchema),
    }
    if (!props.defaultViewOnly) {
      designerPayload.designerOptions = cloneSchema({
        ...(props.designerOptions || {}),
        actions: designerActions,
      })
    }
    await saveBusinessObjectDesigner(props.objectId, designerPayload)
    setLocalSchema(schema, { external: true })
    emit('update:viewSchema', cloneSchema(viewSchema))
    if (!props.defaultViewOnly)
      emit('update:designerActions', cloneSchema(designerActions))
    emit('saved', cloneSchema(schema))
    emit('dirtyChange', false)
    message.success('列表布局已保存')
    return schema
  }
  catch (error) {
    message.error(error?.message || '列表布局保存失败')
    throw error
  }
  finally {
    saving.value = false
  }
}

function buildCurrentViewSchema(schema = localSchema.value) {
  return createViewSchemaFromPageSchema(normalizeSchemaForSave(schema), designFields.value, props.viewSchema || {})
}

function buildDesignerRuntimeCrudProps(schema = {}, fields = [], customActions = []) {
  const zones = schema.zones || []
  const searchZone = zones.find(zone => zone.zoneKey === 'search') || {}
  const tableZone = zones.find(zone => zone.zoneKey === 'table') || {}
  const editZone = zones.find(zone => zone.zoneKey === 'edit') || {}
  const tableProps = tableZone.props || {}
  const searchProps = searchZone.props || {}
  const editProps = editZone.props || {}
  const formLayout = schema.layout || {}
  const apiValues = resolveDesignerDefaultApiValues(schema)
  const fieldMap = buildDesignerFieldMap(fields)
  const editFields = buildDesignerEditSchema(editZone, fieldMap)
  const hookHandlers = buildDesignerCrudHookHandlers(tableProps)
  const runtimeActions = normalizeListCustomActions(customActions)
  const resolvedFormOpenMode = resolveDesignerFormOpenMode(formLayout, tableProps, editProps)
  const resolvedModalType = resolveDesignerModalType(resolvedFormOpenMode, formLayout, tableProps, editProps)
  return {
    lazy: true,
    designPreview: true,
    loadDetailOnEdit: false,
    columns: buildDesignerColumns(tableZone, fieldMap),
    searchSchema: buildDesignerSearchSchema(searchZone, fieldMap),
    editSchema: editFields,
    apiConfig: appendDesignPreviewToApiConfig({
      list: tableProps.listApi || apiValues.listApi,
      detail: tableProps.detailApi || apiValues.detailApi,
      create: tableProps.createApi || apiValues.createApi,
      update: tableProps.updateApi || apiValues.updateApi,
      delete: tableProps.deleteApi || apiValues.deleteApi,
      import: tableProps.importApi || '',
      export: tableProps.exportApi || '',
      tree: tableProps.treeApi || '',
    }),
    api: appendDesignPreviewToApiValue(tableProps.api || apiValues.api),
    rowKey: tableProps.rowKey || 'id',
    showSearch: searchZone.enabled !== false && tableProps.showSearch !== false,
    showPagination: tableProps.showPagination !== false,
    searchGridCols: searchProps.gridCols || tableProps.searchGridCols || 4,
    searchLabelWidth: searchProps.labelWidth || tableProps.searchLabelWidth || 'auto',
    searchEnableCollapse: tableProps.searchEnableCollapse !== false,
    searchMaxVisibleFields: tableProps.searchMaxVisibleFields || 3,
    searchYGap: tableProps.searchYGap ?? 16,
    editGridCols: formLayout.gridColumns || formLayout.gridCols || editProps.editGridCols || tableProps.editGridCols || 1,
    editLabelWidth: formLayout.labelWidth || editProps.editLabelWidth || editProps.labelWidth || tableProps.editLabelWidth || 'auto',
    editLabelPlacement: formLayout.labelPlacement || editProps.editLabelPlacement || editProps.labelPlacement || tableProps.editLabelPlacement || 'left',
    editLabelAlign: formLayout.labelAlign || editProps.editLabelAlign || editProps.labelAlign || tableProps.editLabelAlign || 'right',
    editSize: formLayout.size || editProps.editSize || editProps.size || tableProps.editSize || 'medium',
    editShowFeedback: editProps.editShowFeedback ?? editProps.showFeedback ?? tableProps.editShowFeedback ?? true,
    editXGap: formLayout.columnGap ?? editProps.editXGap ?? editProps.columnGap ?? tableProps.editXGap ?? 16,
    editYGap: formLayout.rowGap ?? editProps.editYGap ?? editProps.rowGap ?? tableProps.editYGap ?? 8,
    modalWidth: formLayout.modalWidth || editProps.modalWidth || tableProps.modalWidth || '800px',
    detailModalWidth: formLayout.detailModalWidth || formLayout.modalWidth || editProps.detailModalWidth || editProps.modalWidth || tableProps.detailModalWidth || tableProps.modalWidth || '800px',
    formOpenMode: resolvedFormOpenMode,
    tabWorkspace: formLayout.tabWorkspace || editProps.tabWorkspace || tableProps.tabWorkspace || {},
    modalType: resolvedModalType,
    drawerPlacement: formLayout.drawerPlacement || editProps.drawerPlacement || tableProps.drawerPlacement || 'right',
    hideModalFooter: tableProps.hideModalFooter === true,
    hideDefaultDetailContent: tableProps.hideDefaultDetailContent === true,
    hideToolbar: tableProps.hideToolbar === true,
    hideAdd: tableProps.hideAdd === true,
    hideBatchDelete: tableProps.hideBatchDelete === true,
    showImport: tableProps.showImport !== false,
    showExport: tableProps.showExport !== false,
    showExportTasks: tableProps.showExportTasks !== false,
    enableCustomQuery: tableProps.enableCustomQuery !== false,
    addButtonText: tableProps.addButtonText || '新增',
    exportButtonText: tableProps.exportButtonText || '导出',
    exportFileName: tableProps.exportFileName || '',
    renderMode: tableProps.renderMode || 'table',
    showRenderModeSwitch: tableProps.showRenderModeSwitch !== false,
    tableSize: tableProps.tableSize || 'medium',
    bordered: Boolean(tableProps.bordered),
    striped: Boolean(tableProps.striped),
    hideSelection: tableProps.hideSelection === true,
    maxHeight: tableProps.maxHeight || undefined,
    scrollX: tableProps.scrollX || undefined,
    resizable: tableProps.resizable !== false,
    expandConfig: tableProps.expandConfig || {},
    listMethod: tableProps.listMethod || 'get',
    listDataField: tableProps.listDataField || 'records',
    listTotalField: tableProps.listTotalField || 'total',
    isEncrypt: tableProps.isEncrypt === true,
    publicParams: tableProps.publicParams || {},
    publicQuery: tableProps.publicQuery || {},
    formDefaultValues: tableProps.formDefaultValues || {},
    submitDefaultParams: tableProps.submitDefaultParams || {},
    toolbarActions: runtimeActions.filter(action => (action.position || 'toolbar') === 'toolbar'),
    runtimeActions: runtimeActions.filter(action => (action.position || 'row') === 'row'),
    ...hookHandlers,
  }
}

function resolveDesignerFormOpenMode(formLayout = {}, tableProps = {}, editProps = {}) {
  const value = formLayout.formOpenMode || formLayout.modalType || editProps.formOpenMode || tableProps.formOpenMode || editProps.modalType || tableProps.modalType || 'modal'
  const mode = String(value || '').trim()
  if (mode === 'tabWorkspace' || mode.toLowerCase() === 'tabworkspace')
    return 'tabWorkspace'
  const normalized = mode.toLowerCase()
  return ['modal', 'drawer', 'flat'].includes(normalized) ? normalized : 'modal'
}

function resolveDesignerModalType(formOpenMode = 'modal', formLayout = {}, tableProps = {}, editProps = {}) {
  if (['modal', 'drawer'].includes(formOpenMode))
    return formOpenMode
  const value = formLayout.modalType || editProps.modalType || tableProps.modalType || 'modal'
  const normalized = String(value || '').trim().toLowerCase()
  return ['modal', 'drawer'].includes(normalized) ? normalized : 'modal'
}

function buildDesignerCrudHookHandlers(tableProps = {}) {
  const rules = normalizeCrudHookRules(tableProps.crudHookRules || {}, tableProps.beforeSubmitRules || [])
  return CRUD_HOOK_RULE_TARGETS.reduce((handlers, target) => {
    const list = (rules[target.value] || []).filter(rule => rule.field)
    if (list.length)
      handlers[target.value] = data => applyCrudHookRules(data, list)
    return handlers
  }, {})
}

function handleListCustomActionsUpdate(actions = []) {
  if (props.defaultViewOnly)
    return
  const nextActions = normalizeListCustomActions(actions)
  listCustomActions.value = nextActions
  setLocalSchema(syncSchemaCustomActions(localSchema.value, nextActions))
  const managedActions = nextActions.map((action, index) => listActionToDesignerAction(action, index, { preserveDraftParams: true }))
  emit('update:designerActions', cloneSchema(mergeUnmanagedChildRowActions(managedActions)))
  emit('dirtyChange', true)
}

function openCustomActionManager() {
  listGridDesignerRef.value?.openCustomActionManager?.()
}

function createToolbarCustomAction() {
  listGridDesignerRef.value?.createAndEditCustomAction?.('toolbar')
}

function createRowCustomAction() {
  listGridDesignerRef.value?.createAndEditCustomAction?.('row')
}

function buildDesignerActionsForSave() {
  const managedActions = normalizeListCustomActions(listCustomActions.value)
    .map((action, index) => listActionToDesignerAction(action, index))
  return mergeUnmanagedChildRowActions(managedActions)
}

function normalizeDesignerActionsForList(actions = []) {
  if (!Array.isArray(actions))
    return []
  return normalizeListCustomActions(actions
    .filter(action => !isChildRowDesignerAction(action))
    .map(designerActionToListAction))
}

function mergeUnmanagedChildRowActions(managedActions = []) {
  const childActions = (Array.isArray(props.designerActions) ? props.designerActions : [])
    .filter(isChildRowDesignerAction)
  return [...managedActions, ...cloneSchema(childActions)]
}

function isChildRowDesignerAction(action = {}) {
  return String(action.actionPosition || action.position || '')
    .replace(/[-\s]+/g, '_')
    .toUpperCase() === 'CHILD_ROW'
}

function syncSchemaCustomActions(schema = {}, actions = []) {
  const normalizedActions = normalizeListCustomActions(actions)
  const syncGrid = (grid = {}) => {
    if (!grid || !Array.isArray(grid.items))
      return grid
    let changed = false
    const items = grid.items.map((item) => {
      if (!['AiCrudPage', 'data-table', 'AiTable', 'toolbar'].includes(item?.blockType))
        return item
      changed = true
      return {
        ...item,
        props: {
          ...(item.props || {}),
          customActions: cloneSchema(normalizedActions),
        },
      }
    })
    return changed ? { ...grid, items } : grid
  }

  const listGridLayout = syncGrid(schema.listGridLayout)
  const pages = Array.isArray(schema.pages)
    ? schema.pages.map(page => ({
        ...page,
        gridLayout: syncGrid(page?.gridLayout),
      }))
    : schema.pages
  const zones = Array.isArray(schema.zones)
    ? schema.zones.map((zone) => {
        if (zone?.zoneKey !== 'table')
          return zone
        return {
          ...zone,
          props: {
            ...(zone.props || {}),
            customActions: cloneSchema(normalizedActions),
          },
        }
      })
    : schema.zones

  return {
    ...schema,
    listGridLayout,
    pages,
    zones,
  }
}

function designerActionToListAction(action = {}, index = 0) {
  const actionType = normalizeDesignerActionType(action.actionType)
  const config = parsePlainObject(action.actionConfig)
  const position = normalizeListActionPosition(action.actionPosition)
  const key = normalizeActionCode(action.actionCode || action.key || action.actionName) || `custom_${index + 1}`
  const routePath = actionType === 'OPEN_EXTERNAL'
    ? (config.url || action.routePath || '')
    : actionType === 'CALL_API'
      ? (config.url || action.routePath || '')
      : actionType === 'TRIGGER'
        ? (config.triggerCode || action.routePath || '')
        : actionType === 'COMMAND'
          ? ''
          : config.targetPath || action.routePath || ''
  return {
    clientKey: action.clientKey || config.clientKey || key,
    key,
    label: action.actionName || action.label || '自定义操作',
    position,
    type: action.type || resolveDefaultActionButtonType(actionType),
    actionType: designerActionTypeToListType(actionType),
    visible: action.visible !== false,
    routePath,
    targetFormKey: config.targetFormKey || action.targetFormKey || '',
    openTarget: config.openTarget || action.openTarget || (actionType === 'OPEN_EXTERNAL' ? '_blank' : '_self'),
    permissionKey: action.permissionKey || action.permissionCode || action.permission || '',
    permissionCode: action.permissionKey || action.permissionCode || action.permission || '',
    permissionStrategy: action.permissionStrategy === 'disable' ? 'disable' : 'hide',
    confirmText: action.confirmText || (action.confirmRequired ? `确认执行“${action.actionName || action.label || '该操作'}”？` : ''),
    displayCondition: action.displayCondition || config.displayCondition || '',
    successBehavior: action.successBehavior || config.successBehavior || 'none',
    successMessage: action.successMessage || config.successMessage || '',
    failureMessage: action.failureMessage || config.failureMessage || '',
    params: normalizeActionParams(config.params || action.params || []),
    actionConfig: normalizeListActionConfig(designerActionTypeToListType(actionType), config),
    status: action.status ?? 1,
    sortOrder: action.sortOrder ?? index * 10 + 10,
  }
}

function listActionToDesignerAction(action = {}, index = 0, options = {}) {
  const normalized = normalizeListCustomAction(action, index)
  const actionType = listActionTypeToDesignerType(normalized.actionType)
  return {
    clientKey: normalized.clientKey || normalized.key,
    actionCode: normalizeActionCode(normalized.key || normalized.label) || `custom_${index + 1}`,
    actionName: normalized.label || '自定义操作',
    actionPosition: normalizeDesignerActionPosition(normalized.position),
    actionType,
    visible: normalized.visible !== false,
    permissionKey: normalized.permissionKey || normalized.permissionCode || '',
    permission: normalized.permissionKey || normalized.permissionCode || '',
    permissionStrategy: normalized.permissionStrategy === 'disable' ? 'disable' : 'hide',
    confirmRequired: Boolean(normalized.confirmText),
    confirmText: normalized.confirmText || '',
    successMessage: normalized.successMessage || '',
    failureMessage: normalized.failureMessage || '',
    status: normalized.status ?? 1,
    sortOrder: normalized.sortOrder ?? index * 10 + 10,
    actionConfig: buildDesignerActionConfig(normalized, actionType, options),
  }
}

function buildDesignerActionConfig(action = {}, actionType = 'OPEN_PAGE', options = {}) {
  const config = {
    ...parsePlainObject(action.actionConfig),
    clientKey: action.clientKey || action.actionConfig?.clientKey || action.key || '',
  }
  const sourceParams = actionType === 'CALL_API'
    ? config.params || []
    : action.params?.length ? action.params : config.params || []
  const params = options.preserveDraftParams
    ? normalizeActionParams(sourceParams).map(({ clientKey, ...param }) => param)
    : toDesignerActionParams(sourceParams)
  if (actionType === 'CALL_API') {
    return {
      ...config,
      method: normalizeActionApiMethod(config.method || 'POST'),
      url: config.url || action.routePath || '',
      capabilityCode: config.capabilityCode || '',
      successBehavior: action.successBehavior || config.successBehavior || '',
      successMessage: action.successMessage || config.successMessage || '',
      failureMessage: action.failureMessage || config.failureMessage || '',
      params,
    }
  }
  if (actionType === 'START_FLOW') {
    return {
      ...config,
      useMainFlow: true,
    }
  }
  if (actionType === 'TRIGGER') {
    return {
      ...config,
      triggerCode: config.triggerCode || action.triggerCode || action.routePath || '',
      params,
    }
  }
  if (actionType === 'COMMAND') {
    return {
      ...config,
      formSchema: Array.isArray(config.formSchema) ? config.formSchema : [],
      steps: Array.isArray(config.steps) ? config.steps : [],
      successBehavior: action.successBehavior || config.successBehavior || 'refreshList',
      params,
    }
  }
  if (actionType === 'OPEN_EXTERNAL') {
    return {
      ...config,
      url: action.routePath || config.url || '',
      openTarget: action.openTarget || config.openTarget || '_blank',
      params,
      successBehavior: action.successBehavior || config.successBehavior || '',
    }
  }
  return {
    ...config,
    targetPath: action.routePath || config.targetPath || '',
    targetFormKey: action.targetFormKey || config.targetFormKey || '',
    openTarget: action.openTarget || config.openTarget || '_self',
    params,
    successBehavior: action.successBehavior || config.successBehavior || '',
  }
}

function normalizeListCustomActions(actions = []) {
  if (!Array.isArray(actions))
    return []
  return actions
    .filter(action => action && typeof action === 'object')
    .map(normalizeListCustomAction)
}

function normalizeListCustomAction(action = {}, index = 0) {
  const actionType = normalizeListActionType(action.actionType)
  const key = normalizeActionCode(action.key || action.actionCode || action.label) || `custom_${index + 1}`
  const config = normalizeListActionConfig(actionType, action.actionConfig)
  return {
    ...action,
    clientKey: action.clientKey || key,
    key,
    label: action.label || action.actionName || '自定义操作',
    position: normalizeListActionPosition(action.position || action.actionPosition),
    type: action.type || resolveDefaultListButtonType(actionType),
    actionType,
    visible: action.visible !== false,
    routePath: action.routePath || resolveListActionRoutePath(actionType, config),
    targetFormKey: action.targetFormKey || config.targetFormKey || '',
    openTarget: action.openTarget || config.openTarget || (actionType === 'external' ? '_blank' : '_self'),
    permissionKey: action.permissionKey || action.permissionCode || action.permission || '',
    permissionCode: action.permissionKey || action.permissionCode || action.permission || '',
    permissionStrategy: action.permissionStrategy === 'disable' ? 'disable' : 'hide',
    confirmText: action.confirmText || '',
    displayCondition: action.displayCondition || config.displayCondition || '',
    successBehavior: action.successBehavior || config.successBehavior || 'none',
    successMessage: action.successMessage || config.successMessage || '',
    failureMessage: action.failureMessage || config.failureMessage || '',
    params: normalizeActionParams(action.params?.length ? action.params : config.params || []),
    actionConfig: config,
    status: action.status ?? 1,
    sortOrder: action.sortOrder ?? index * 10 + 10,
  }
}

function normalizeListActionConfig(actionType = 'route', config = {}) {
  const source = parsePlainObject(config)
  if (actionType === 'CALL_API') {
    return {
      ...source,
      apiConfigId: source.apiConfigId === undefined || source.apiConfigId === null ? null : String(source.apiConfigId),
      apiCode: String(source.apiCode || '').trim(),
      apiName: String(source.apiName || '').trim(),
      method: normalizeActionApiMethod(source.method || source.reqMethod || source.apiMethod || 'POST'),
      url: String(source.url || source.apiUrl || source.urlPath || source.path || '').trim(),
      capabilityCode: String(source.capabilityCode || '').trim(),
      params: normalizeActionParams(source.params || source.paramMappings || []),
    }
  }
  if (actionType === 'START_FLOW') {
    return {
      ...source,
      useMainFlow: true,
    }
  }
  if (actionType === 'COMMAND') {
    return {
      ...source,
      formSchema: Array.isArray(source.formSchema) ? source.formSchema : [],
      steps: Array.isArray(source.steps) ? source.steps : [],
      successBehavior: String(source.successBehavior || '').trim() || 'refreshList',
      params: normalizeActionParams(source.params || []),
    }
  }
  return {
    ...source,
    params: normalizeActionParams(source.params || []),
  }
}

function normalizeActionParams(params = []) {
  if (!Array.isArray(params))
    return []
  return params
    .filter(param => param && typeof param === 'object')
    .map((param, index) => ({
      clientKey: param.clientKey || `param_${Date.now()}_${index}`,
      name: String(param.name || '').trim(),
      target: ['path', 'query', 'body', 'header'].includes(param.target) ? param.target : '',
      sourceType: ['rowField', 'routeQuery', 'static', 'system'].includes(param.sourceType) ? param.sourceType : 'rowField',
      sourceField: String(param.sourceField || '').trim(),
      value: param.value === undefined || param.value === null ? '' : String(param.value),
    }))
}

function toDesignerActionParams(params = []) {
  return normalizeActionParams(params)
    .filter(param => param.name && (param.sourceType === 'static' ? param.value !== '' : param.sourceField))
    .map(({ clientKey, ...param }) => param)
}

function collectSchemaCustomActions(schema = {}) {
  const grids = [
    schema.listGridLayout,
    ...(Array.isArray(schema.pages) ? schema.pages.map(page => page?.gridLayout).filter(Boolean) : []),
  ]
  const actions = []
  grids.forEach((grid) => {
    ;(grid?.items || []).forEach((item) => {
      if (Array.isArray(item?.props?.customActions))
        actions.push(...item.props.customActions)
    })
  })
  const tableZone = (schema.zones || []).find(zone => zone.zoneKey === 'table')
  if (Array.isArray(tableZone?.props?.customActions))
    actions.push(...tableZone.props.customActions)
  return normalizeListCustomActions(deduplicateListActions(actions))
}

function deduplicateListActions(actions = []) {
  const seen = new Set()
  return actions.filter((action) => {
    const key = action?.key || action?.actionCode || action?.label
    if (!key)
      return true
    const normalized = String(key)
    if (seen.has(normalized))
      return false
    seen.add(normalized)
    return true
  })
}

function isInvalidDesignerApiAction(action = {}) {
  if (normalizeDesignerActionType(action.actionType) !== 'CALL_API')
    return false
  const config = parsePlainObject(action.actionConfig)
  return !String(config.url || config.apiUrl || config.urlPath || '').trim()
    && !String(config.capabilityCode || '').trim()
}

function normalizeDesignerActionType(value = '') {
  const normalized = String(value || 'OPEN_PAGE')
    .replace(/([a-z])([A-Z])/g, '$1_$2')
    .replace(/[-\s]+/g, '_')
    .toUpperCase()
  if (['CALL_API', 'REQUEST'].includes(normalized))
    return 'CALL_API'
  if (['START_FLOW', 'START_APPROVAL'].includes(normalized))
    return 'START_FLOW'
  if (normalized === 'OPEN_EXTERNAL')
    return 'OPEN_EXTERNAL'
  if (normalized === 'TRIGGER')
    return 'TRIGGER'
  if (normalized === 'COMMAND')
    return 'COMMAND'
  return 'OPEN_PAGE'
}

function designerActionTypeToListType(actionType = 'OPEN_PAGE') {
  if (actionType === 'CALL_API')
    return 'CALL_API'
  if (actionType === 'START_FLOW')
    return 'START_FLOW'
  if (actionType === 'TRIGGER')
    return 'TRIGGER'
  if (actionType === 'COMMAND')
    return 'COMMAND'
  if (actionType === 'OPEN_EXTERNAL')
    return 'external'
  return 'route'
}

function listActionTypeToDesignerType(actionType = 'route') {
  const normalized = normalizeListActionType(actionType)
  if (normalized === 'CALL_API')
    return 'CALL_API'
  if (normalized === 'START_FLOW')
    return 'START_FLOW'
  if (normalized === 'TRIGGER')
    return 'TRIGGER'
  if (normalized === 'COMMAND')
    return 'COMMAND'
  if (normalized === 'external')
    return 'OPEN_EXTERNAL'
  return 'OPEN_PAGE'
}

function normalizeListActionType(value = '') {
  const raw = String(value || 'route')
  const upper = raw.replace(/[-\s]+/g, '_').toUpperCase()
  if (['CALL_API', 'REQUEST'].includes(upper))
    return 'CALL_API'
  if (['START_FLOW', 'START_APPROVAL'].includes(upper))
    return 'START_FLOW'
  if (upper === 'TRIGGER')
    return 'TRIGGER'
  if (upper === 'COMMAND')
    return 'COMMAND'
  if (upper === 'EXTERNAL')
    return 'external'
  if (upper === 'REFRESH')
    return 'refresh'
  return 'route'
}

function normalizeListActionPosition(value = '') {
  const normalized = String(value || 'row').replace(/[-\s]+/g, '_').toUpperCase()
  if (normalized === 'TOOLBAR')
    return 'toolbar'
  if (normalized === 'DETAIL')
    return 'detail'
  return 'row'
}

function normalizeDesignerActionPosition(value = '') {
  const normalized = normalizeListActionPosition(value)
  if (normalized === 'toolbar')
    return 'TOOLBAR'
  if (normalized === 'detail')
    return 'DETAIL'
  return 'ROW'
}

function resolveDefaultActionButtonType(actionType = 'OPEN_PAGE') {
  if (actionType === 'START_FLOW')
    return 'success'
  if (actionType === 'CALL_API')
    return 'primary'
  if (actionType === 'TRIGGER')
    return 'warning'
  if (actionType === 'COMMAND')
    return 'success'
  return 'default'
}

function resolveDefaultListButtonType(actionType = 'route') {
  if (actionType === 'START_FLOW')
    return 'success'
  if (actionType === 'CALL_API')
    return 'primary'
  if (actionType === 'TRIGGER')
    return 'warning'
  if (actionType === 'COMMAND')
    return 'success'
  return 'default'
}

function resolveListActionRoutePath(actionType = 'route', config = {}) {
  if (actionType === 'external')
    return config.url || ''
  if (actionType === 'CALL_API')
    return config.url || ''
  if (actionType === 'TRIGGER')
    return config.triggerCode || ''
  if (actionType === 'COMMAND')
    return ''
  return config.targetPath || ''
}

function normalizeActionApiMethod(value = '') {
  const method = String(value || 'POST')
    .replace('-', '_')
    .toUpperCase()
  return ['GET', 'POST', 'POST_ENCRYPT', 'PUT', 'DELETE', 'PATCH'].includes(method) ? method : 'POST'
}

function normalizeActionCode(value = '') {
  return String(value || '')
    .trim()
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '')
    .toLowerCase()
    .slice(0, 64)
}

function parsePlainObject(value) {
  if (!value)
    return {}
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
    }
    catch {
      return {}
    }
  }
  return typeof value === 'object' && !Array.isArray(value) ? { ...value } : {}
}

function resolveDesignerDefaultApiValues(schema = {}) {
  const modelSchema = effectiveModelSchema.value || {}
  const key = schema.configKey
    || modelSchema.configKey
    || modelSchema.object?.configKey
    || modelSchema.object?.code
    || modelSchema.objectCode
    || modelSchema.modelCode
    || ''
  const prefix = key ? `/ai/crud/${key}` : '/ai/crud/当前配置'
  return {
    api: prefix,
    listApi: `get@${prefix}/page`,
    detailApi: `get@${prefix}/:id`,
    createApi: `post@${prefix}`,
    updateApi: `put@${prefix}`,
    deleteApi: `delete@${prefix}/:id`,
  }
}

function buildDesignerFieldMap(fields = []) {
  return new Map((Array.isArray(fields) ? fields : [])
    .map(field => [field.field || field.fieldCode, field])
    .filter(([fieldCode]) => fieldCode))
}

function buildDesignerColumns(zone = {}, fieldMap = new Map()) {
  return resolveDesignerZoneRefs(zone, fieldMap).filter((fieldCode) => {
    const setting = zone.props?.fieldSettings?.[fieldCode] || {}
    return setting.visible !== false
  }).map((fieldCode) => {
    const field = fieldMap.get(fieldCode) || {}
    const setting = zone.props?.fieldSettings?.[fieldCode] || {}
    return {
      key: fieldCode,
      field: fieldCode,
      title: setting.label || field.label || field.fieldName || fieldCode,
      minWidth: Number(setting.width || field.width || 110),
      align: setting.align || undefined,
      ellipsis: { tooltip: true },
    }
  })
}

function buildDesignerSearchSchema(zone = {}, fieldMap = new Map()) {
  // 搜索区仅在用户显式配置了搜索字段时才生成搜索表单项，
  // 避免新建列表页时自动把对象全部字段填充为搜索输入框。
  if (!Array.isArray(zone.fieldRefs) || !zone.fieldRefs.length)
    return []
  return resolveDesignerZoneRefs(zone, fieldMap).map((fieldCode) => {
    const field = fieldMap.get(fieldCode) || {}
    const setting = zone.props?.fieldSettings?.[fieldCode] || {}
    return buildDesignerRuntimeField(field, setting, 'search')
  })
}

function buildDesignerEditSchema(zone = {}, fieldMap = new Map()) {
  const refs = resolveDesignerZoneRefs(zone, fieldMap)
  const settings = zone.props?.fieldSettings || {}
  const fields = refs.map((fieldCode) => {
    const field = fieldMap.get(fieldCode) || {}
    return buildDesignerRuntimeField(field, settings[fieldCode] || {}, 'form')
  })
  const layout = hydrateDesignerFormLayout(zone.props?.formLayout || [], new Map(fields.map(field => [field.field, field])))
  return layout.length ? layout : fields
}

function resolveDesignerZoneRefs(zone = {}, fieldMap = new Map()) {
  const refs = Array.isArray(zone.fieldRefs) && zone.fieldRefs.length
    ? zone.fieldRefs
    : Array.from(fieldMap.keys())
  return refs.filter(fieldCode => fieldMap.has(fieldCode))
}

function buildDesignerRuntimeField(field = {}, setting = {}, mode = 'form') {
  const fieldCode = field.field || field.fieldCode || ''
  const type = normalizeDesignerRuntimeFieldType(setting.componentType || field.componentType || field.dataType)
  const runtimeField = {
    field: fieldCode,
    label: setting.label || field.label || field.fieldName || fieldCode,
    type,
    placeholder: setting.placeholder || field.placeholder || (mode === 'search' ? `请输入${field.label || fieldCode}` : `请输入${field.label || fieldCode}`),
    span: Number(setting.span || field.span || 1),
    clearable: true,
    options: setting.options || field.options || [],
    dictType: setting.dictType || field.dictType || '',
    props: {
      ...(setting.props || {}),
    },
  }
  ;[
    'labelWidth',
    'required',
    'requiredMessage',
    'rules',
    'trigger',
    'readonly',
    'disabled',
    'defaultValue',
    'componentStyle',
    'componentClass',
    'formItemStyle',
    'formItemClass',
    'showLabel',
  ].forEach((key) => {
    if (setting[key] !== undefined)
      runtimeField[key] = setting[key]
  })
  return runtimeField
}

function hydrateDesignerFormLayout(layout = [], fieldMap = new Map(), usedFields = new Set()) {
  return (Array.isArray(layout) ? layout : [])
    .map(node => hydrateDesignerFormLayoutNode(node, fieldMap, usedFields))
    .filter(Boolean)
}

function hydrateDesignerFormLayoutNode(node = {}, fieldMap = new Map(), usedFields = new Set()) {
  if (!node || typeof node !== 'object')
    return null
  if (node.nodeType === 'field') {
    const field = fieldMap.get(node.field)
    if (!field)
      return null
    usedFields.add(node.field)
    return {
      ...field,
      nodeType: 'field',
      key: node.key || field.field,
      span: node.span || field.span,
      gridStyle: node.gridStyle || field.gridStyle,
    }
  }
  const children = hydrateDesignerFormLayout(node.children || [], fieldMap, usedFields)
  if (!children.length && !isStandaloneDesignerLayoutNode(node))
    return null
  return {
    ...node,
    children,
  }
}

function isStandaloneDesignerLayoutNode(node = {}) {
  return ['divider', 'groupTitle', 'button'].includes(node.nodeType || node.componentKey || node.type)
}

function normalizeDesignerRuntimeFieldType(type = '') {
  if (['int', 'bigint', 'decimal', 'number', 'inputNumber'].includes(type))
    return 'number'
  if (['datetime', 'date', 'time'].includes(type))
    return type
  if (['textarea', 'select', 'dictSelect', 'checkbox', 'radio', 'switch', 'treeSelect', 'userSelect'].includes(type))
    return type
  return 'input'
}

function syncDesignerDraft() {
  const schema = normalizeSchemaForSave(resolveSchema(localSchema.value, effectiveModelSchema.value))
  const viewSchema = buildCurrentViewSchema(schema)
  setLocalSchema(schema, { external: true })
  emit('update:modelValue', cloneSchema(schema))
  emit('update:viewSchema', cloneSchema(viewSchema))
  return {
    dirty: !isSameSchema(schema, props.modelValue) || !isSameSchema(viewSchema, props.viewSchema || {}),
    pageSchema: cloneSchema(schema),
    viewSchema: cloneSchema(viewSchema),
  }
}

function normalizeSchemaForSave(schema = localSchema.value) {
  const resolved = ensureGridListSchema(resolveSchema(schema, effectiveModelSchema.value), effectiveModelSchema.value)
  const listPage = (resolved.pages || []).find(page => page.pageKey === 'list')
  const sourceGrid = listPage?.gridLayout || resolved.listGridLayout
  if (!sourceGrid)
    return { ...resolved, listLayoutMode: 'grid' }
  const syncedGrid = syncGridLayoutWithModel(sourceGrid, effectiveModelSchema.value, { layoutType: resolved.layoutType })
  return {
    ...resolved,
    listLayoutMode: 'grid',
    listGridLayout: syncedGrid,
    pages: updateDesignerPageGrid(resolved.pages || [], 'list', syncedGrid),
    zones: applyGridLayoutToZones(resolved.zones || [], syncedGrid, effectiveModelSchema.value),
  }
}

function resolveSchema(pageSchema, modelSchema) {
  const source = cloneSchema(pageSchema || createDefaultPageSchema(modelSchema))
  const layoutType = inferLayoutType(source, modelSchema)
  const schema = syncPageSchemaWithModel(
    {
      ...source,
      layoutType,
    },
    modelSchema,
  )
  const gridSchema = ensureGridListSchema({
    ...schema,
    layoutType,
  }, modelSchema)
  return {
    ...gridSchema,
    pages: ensureDesignerPages(gridSchema, modelSchema),
  }
}

function ensureGridListSchema(schema = {}, modelSchema = effectiveModelSchema.value) {
  const listPageGrid = (schema.pages || []).find(page => page?.pageKey === 'list')?.gridLayout
  const sourceGrid = listPageGrid || schema.listGridLayout
  const fallbackGrid = sourceGrid?.items?.length && isStandardListGridLayout(sourceGrid)
    ? sourceGrid
    : createDefaultListGridLayout(modelSchema, { layoutType: schema.layoutType })
  const syncedGrid = syncGridLayoutWithModel(fallbackGrid, modelSchema, { layoutType: schema.layoutType })
  return {
    ...schema,
    listLayoutMode: 'grid',
    listGridLayout: syncedGrid,
    pages: updateDesignerPageGrid(schema.pages || [], 'list', syncedGrid),
    zones: applyGridLayoutToZones(schema.zones || [], syncedGrid, modelSchema),
  }
}

function isStandardListGridLayout(gridLayout = {}) {
  return Array.isArray(gridLayout.items) && gridLayout.items.some(item => item?.blockType === 'AiCrudPage')
}

function ensureDesignerPages(schema, modelSchema) {
  const sourcePages = Array.isArray(schema.pages) ? schema.pages : []
  const listPage = sourcePages.find(page => page.pageKey === 'list')
  const listGridLayout = listPage?.gridLayout
    || schema.listGridLayout
    || createDefaultListGridLayout(modelSchema, { layoutType: schema.layoutType })
  const pages = [
    normalizeDesignerPage(listPage, {
      pageKey: 'list',
      pageName: '列表页',
      pageType: 'list',
      routePath: '',
      description: '主列表页面',
      params: [],
      detailMethod: 'get',
      detailApi: '',
      detailDataField: 'data',
      gridLayout: listGridLayout,
    }),
    ...sourcePages
      .filter(page => !['list', 'detail'].includes(page.pageKey))
      .map(page => normalizeDesignerPage(page, {
        pageKey: page.pageKey,
        pageName: page.pageName || '自定义页面',
        pageType: normalizeDesignerPageType(page.pageType || 'custom'),
        routePath: page.routePath || '',
        description: page.description || '',
        params: page.params || [],
        detailMethod: page.detailMethod || 'get',
        detailApi: page.detailApi || '',
        detailDataField: page.detailDataField || 'data',
        gridLayout: page.gridLayout || { cols: 12, rowHeight: 32, gap: 8, designWidth: LIST_PAGE_DESIGN_WIDTH, layoutType: schema.layoutType, items: [] },
      })),
  ]
  if (!pages.some(page => page.pageKey === activePageKey.value))
    activePageKey.value = 'list'
  return pages
}

function normalizeDesignerPage(page, defaults) {
  return {
    ...defaults,
    ...(page || {}),
    pageKey: page?.pageKey || defaults.pageKey,
    pageName: page?.pageName || defaults.pageName,
    pageType: normalizeDesignerPageType(page?.pageType || defaults.pageType || 'custom'),
    routePath: page?.routePath ?? defaults.routePath ?? '',
    description: page?.description ?? defaults.description ?? '',
    params: Array.isArray(page?.params) ? page.params : defaults.params || [],
    detailMethod: page?.detailMethod || defaults.detailMethod || 'get',
    detailApi: page?.detailApi ?? defaults.detailApi ?? '',
    detailDataField: page?.detailDataField || defaults.detailDataField || 'data',
    gridLayout: page?.gridLayout || defaults.gridLayout,
  }
}

function updateDesignerPageGrid(pages = [], pageKey = 'list', gridLayout) {
  if (!pages.some(page => page.pageKey === pageKey)) {
    return [
      ...pages,
      {
        pageKey,
        pageName: pageKey === 'list' ? '列表页' : '页面',
        pageType: pageKey === 'list' ? 'list' : 'custom',
        routePath: '',
        description: '',
        params: [],
        detailMethod: 'get',
        detailApi: '',
        detailDataField: 'data',
        gridLayout,
      },
    ]
  }
  return pages.map(page => page.pageKey === pageKey
    ? { ...page, gridLayout }
    : page)
}

function setLocalSchema(schema, options = {}) {
  if (isSameSchema(schema, localSchema.value))
    return
  if (!options.external && options.recordHistory !== false) {
    pushHistorySnapshot(localSchema.value)
    redoStack.value = []
  }
  applyingExternalSchema = !!options.external
  localSchema.value = schema
}

function pushHistorySnapshot(schema) {
  undoStack.value = [...undoStack.value, cloneSchema(schema)].slice(-HISTORY_LIMIT)
}

function undoSchema() {
  if (!canUndo.value)
    return
  const currentSchema = cloneSchema(localSchema.value)
  const previousSchema = undoStack.value[undoStack.value.length - 1]
  undoStack.value = undoStack.value.slice(0, -1)
  redoStack.value = [currentSchema, ...redoStack.value].slice(0, HISTORY_LIMIT)
  setLocalSchema(previousSchema, { recordHistory: false })
}

function redoSchema() {
  if (!canRedo.value)
    return
  const currentSchema = cloneSchema(localSchema.value)
  const nextSchema = redoStack.value[0]
  redoStack.value = redoStack.value.slice(1)
  pushHistorySnapshot(currentSchema)
  setLocalSchema(nextSchema, { recordHistory: false })
}

function handleListDesignerShortcut(event) {
  const key = event.key?.toLowerCase?.()
  const isUndoKey = (event.metaKey || event.ctrlKey) && !event.shiftKey && key === 'z'
  const isRedoKey = (event.metaKey || event.ctrlKey) && ((event.shiftKey && key === 'z') || key === 'y')
  if (!isUndoKey && !isRedoKey)
    return
  const target = event.target
  if (target?.closest?.('input, textarea, [contenteditable="true"]'))
    return
  event.preventDefault()
  if (isRedoKey)
    redoSchema()
  else
    undoSchema()
}

function openLocalPreview() {
  listPreviewVisible.value = true
}

function omitTreeRuntimeProps(props = {}) {
  const { treeConfig, ...rest } = props || {}
  return rest
}

function inferLayoutType(pageSchema, modelSchema) {
  if (isTreeLayout(pageSchema, modelSchema))
    return 'tree-crud'
  if (isRelationLayout(pageSchema, modelSchema))
    return 'master-detail-crud'
  return 'simple-crud'
}

function isTreeLayout(pageSchema = {}, modelSchema = {}) {
  const hasPageTreeConfig = Boolean(pageSchema.zones?.find(zone => zone.zoneKey === 'table')?.props?.treeConfig?.enabled)
  const listPageGridLayout = Array.isArray(pageSchema.pages)
    ? pageSchema.pages.find(page => page?.pageKey === 'list')?.gridLayout
    : null
  const hasTreeGridBlock = Boolean(
    pageSchema.listGridLayout?.items?.some(item => item.blockType === 'tree-panel')
    || listPageGridLayout?.items?.some(item => item.blockType === 'tree-panel'),
  )
  if (pageSchema.layoutType === 'simple-crud' && !hasTreeGridBlock)
    return false
  return modelSchema?.appType === 'TREE'
    || modelSchema?.treeConfig?.enabled === true
    || pageSchema.layoutType === 'tree-crud'
    || hasPageTreeConfig
    || hasTreeGridBlock
}

function isRelationLayout(pageSchema = {}, modelSchema = {}) {
  return modelSchema?.appType === 'MASTER_DETAIL'
    || (pageSchema.modelRefs || []).some(ref => ref && !ref.primary)
    || (modelSchema.pageModelRefs || []).some(ref => ref && !ref.primary)
    || (modelSchema.fields || []).some(field => field?.modelCode && field.field !== (field.sourceField || field.field))
}

function resolveLayoutModeLabel(layoutType) {
  if (layoutType === 'tree-crud')
    return '自由画布 · 已套用左树右表'
  if (layoutType === 'master-detail-crud')
    return '自由画布 · 已启用关联数据'
  return '自由画布'
}

function updateTreeZone(zones = [], enabled) {
  const defaultTreeConfig = resolveDefaultTreeConfig(effectiveModelSchema.value, effectiveModelSchema.value?.treeConfig || {})
  return zones.map((zone) => {
    if (zone.zoneKey !== 'table')
      return zone
    const props = { ...(zone.props || {}) }
    if (enabled) {
      props.treeConfig = {
        ...defaultTreeConfig,
        ...(props.treeConfig || {}),
        enabled: true,
      }
    }
    else {
      delete props.treeConfig
    }
    return {
      ...zone,
      props,
    }
  })
}

function resolveDesignModelSchema(pageSchema, modelSchema) {
  const refs = mergePrimaryModelRef(pageSchema?.modelRefs || [], modelSchema || {})
  return buildPageDesignModelSchema(modelSchema || {}, refs)
}

function mergePrimaryModelRef(modelRefs, modelSchema) {
  if (!Array.isArray(modelRefs) || !modelRefs.length)
    return []
  const primaryRef = createPageModelRef({ modelSchema }, { primary: true })
  const refs = modelRefs.map(ref => ref?.primary
    ? {
        ...ref,
        modelCode: primaryRef.modelCode || ref.modelCode,
        modelName: primaryRef.modelName || ref.modelName,
        tableName: primaryRef.tableName || ref.tableName,
        relations: primaryRef.relations?.length ? primaryRef.relations : ref.relations,
        fields: primaryRef.fields,
      }
    : ref)
  if (!refs.some(ref => ref?.primary))
    refs.unshift(primaryRef)
  return refs
}

function toPageField(field) {
  return {
    ...field,
    field: field.field || field.fieldCode,
    label: field.label || field.fieldName || field.fieldCode,
    comment: field.remark || field.fieldName,
    columnName: field.columnName,
    dataType: field.dataType,
    componentType: field.componentType,
    dictType: field.dictType,
    required: field.required,
    systemField: field.systemField,
    readonly: field.readonly,
    searchable: field.searchable,
    listVisible: field.listVisible,
    formVisible: field.formVisible,
    fieldStatus: field.fieldStatus,
    basicProps: { ...(field.basicProps || {}) },
    advancedProps: { ...(field.advancedProps || {}) },
  }
}

defineExpose({
  saveLayout,
  syncDesignerDraft,
})

onMounted(() => {
  window.addEventListener('keydown', handleListDesignerShortcut)
  window.addEventListener('forge-list-designer:preview-current-list', openLocalPreview)
  // 初始规范化后的schema若与已保存草稿不一致（如旧草稿缺grid布局被重建），
  // 标记为脏，否则用户看到的配置会因保存按钮禁用而无法落盘。
  if (!isSameSchema(localSchema.value, props.modelValue))
    emit('dirtyChange', true)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleListDesignerShortcut)
  window.removeEventListener('forge-list-designer:preview-current-list', openLocalPreview)
})
</script>

<style scoped>
.business-list-designer {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  height: calc(100vh - 106px);
  min-height: 0;
  container-type: inline-size;
  background: #f8f9fa;
}

.list-designer-head {
  display: none;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 48px;
  border-bottom: 1px solid #e4e4e7;
  padding: 7px 12px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
}

.list-designer-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
  flex: 1 1 auto;
}

.list-designer-head h3 {
  flex: 0 0 auto;
  margin: 0;
  color: #18181b;
  font-size: 13px;
  line-height: 18px;
  letter-spacing: 0;
}

.list-designer-head p {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: #71717a;
  font-size: 11px;
  line-height: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-designer-actions {
  flex: 0 0 auto;
  flex-wrap: nowrap;
}

.list-toolbar-icon-button {
  flex: 0 0 auto;
  --n-color: #f8fafc !important;
  --n-color-hover: #eef6ff !important;
  --n-color-pressed: #dbeafe !important;
  --n-color-focus: #f8fafc !important;
  --n-border: 1px solid #dbe3ee !important;
  --n-border-hover: 1px solid #93c5fd !important;
  --n-border-pressed: 1px solid #60a5fa !important;
  --n-border-focus: 1px solid #93c5fd !important;
  --n-text-color: #475569 !important;
  --n-text-color-hover: #1d4ed8 !important;
}

.list-page-switch {
  position: relative;
  z-index: 10;
  display: grid;
  gap: 0;
  padding: 8px 12px;
  border-bottom: 1px solid #e4e4e7;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(10px);
}

.list-custom-actions-entry {
  display: grid;
  grid-template-columns: minmax(180px, 0.9fr) minmax(260px, 1.4fr) auto;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid #dbe3ee;
  background: #f8fbff;
}

.custom-actions-entry-copy {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.custom-actions-entry-copy strong {
  color: #111827;
  font-size: 13px;
  font-weight: 700;
}

.custom-actions-entry-copy span,
.custom-action-preview-empty {
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.custom-actions-entry-preview {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  overflow: hidden;
}

.custom-action-preview-chip {
  max-width: 180px;
  overflow: hidden;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  line-height: 24px;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 9px;
}

.custom-actions-entry-buttons {
  flex-wrap: nowrap;
}

.page-switch-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
}

.page-switch-title {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
  flex: 1 1 auto;
}

.page-switch-copy {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.list-template-trigger {
  display: inline-grid;
  grid-template-columns: auto minmax(0, auto) auto;
  gap: 5px;
  align-items: center;
  justify-self: start;
  max-width: 168px;
  min-height: 24px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #27272a;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  line-height: 16px;
  padding: 2px 5px;
  text-align: left;
  transition:
    background 160ms ease,
    color 160ms ease;
}

.list-template-trigger:hover {
  background: #f4f4f5;
  color: #1d4ed8;
}

.list-template-trigger span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-template-trigger :deep(.n-icon) {
  color: #64748b;
  font-size: 14px;
}

.list-template-trigger:hover :deep(.n-icon) {
  color: #2563eb;
}

.list-template-trigger .template-chevron {
  font-size: 12px;
}

.page-switch-icon {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 1px solid #dbeafe;
  border-radius: 7px;
  background: #eef2ff;
  color: #3153d8;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}

.page-switch-title small {
  display: block;
  max-width: 150px;
  overflow: hidden;
  color: #a1a1aa;
  font-size: 10px;
  line-height: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.page-switch-row,
.page-config-row,
.page-param-row,
.page-data-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.page-switch-row {
  display: grid;
  grid-template-columns: minmax(150px, 0.8fr) minmax(240px, 1.4fr) minmax(260px, 0.9fr);
  align-items: center;
  justify-content: stretch;
  gap: 12px;
}

.page-tab-list {
  display: flex;
  align-items: center;
  justify-content: center;
  justify-self: center;
  gap: 2px;
  min-width: 0;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 3px;
  border: 1px solid rgba(228, 228, 231, 0.8);
  border-radius: 8px;
  background: rgba(244, 244, 245, 0.9);
  scrollbar-width: none;
}

.page-tab-list::-webkit-scrollbar {
  display: none;
}

.page-tab-button {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 1px;
  flex: 0 0 auto;
  min-width: 76px;
  max-width: 142px;
  min-height: 26px;
  padding: 4px 10px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: #71717a;
  text-align: center;
  cursor: pointer;
  transition: all 0.16s ease;
}

.page-tab-button::before {
  display: none;
}

.page-tab-button.type-list::before {
  background: #2563eb;
  box-shadow: 0 0 0 3px #dbeafe;
}

.page-tab-button.type-detail::before {
  background: #16a34a;
  box-shadow: 0 0 0 3px #dcfce7;
}

.page-tab-button.type-dialog::before,
.page-tab-button.type-drawer::before {
  background: #d97706;
  box-shadow: 0 0 0 3px #fef3c7;
}

.page-tab-name,
.page-tab-button small {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.page-tab-name {
  color: inherit;
  font-size: 12px;
  font-weight: 700;
}

.page-tab-button small {
  display: none;
  color: #71717a;
  font-size: 9px;
}

.page-tab-button:hover,
.page-tab-button.active {
  border-color: transparent;
  background: rgba(228, 228, 231, 0.75);
  box-shadow: none;
}

.page-tab-button.active {
  border-color: #fff;
  background: #fff;
  color: #27272a;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.page-tab-button.active span,
.page-tab-button.active small {
  color: inherit;
}

.page-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  min-width: 0;
}

.page-tool-button {
  min-width: 0;
  font-weight: 600;
}

.page-icon-button {
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
}

.page-settings-popover {
  position: absolute;
  z-index: 40;
  top: calc(100% + 8px);
  right: 12px;
  display: grid;
  gap: 10px;
  width: min(760px, calc(100vw - 360px));
  min-width: 520px;
  padding: 12px;
  border: 1px solid #e4e4e7;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(14px);
}

.page-settings-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.page-settings-head strong {
  color: #27272a;
  font-size: 13px;
  line-height: 18px;
}

.page-settings-head span {
  min-width: 0;
  overflow: hidden;
  color: #71717a;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.page-config-row {
  display: grid;
  grid-template-columns:
    minmax(112px, 0.9fr)
    minmax(112px, 0.9fr)
    minmax(104px, 0.72fr)
    minmax(150px, 1.2fr)
    minmax(150px, 1.25fr);
  gap: 8px;
  align-items: end;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.page-config-row :deep(.n-input),
.page-config-row :deep(.n-base-selection) {
  width: 100%;
}

.page-param-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 42px;
  padding: 8px;
  border: 1px solid #f1f1f2;
  border-radius: 8px;
  background: #fafafa;
  box-shadow: none;
}

.page-data-row {
  display: grid;
  grid-template-columns: auto 88px minmax(260px, 1fr) minmax(140px, 0.45fr);
  gap: 8px;
  padding: 8px;
  border: 1px solid #f1f1f2;
  border-radius: 8px;
  background: #fafafa;
  box-shadow: none;
}

.page-settings-footer {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  padding-top: 2px;
}

.page-method-select {
  width: 88px;
}

.page-param-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: #eef6ff;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.page-param-title::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #2563eb;
}

.page-param-item {
  display: grid;
  grid-template-columns: minmax(96px, 0.85fr) minmax(150px, 1fr) 24px;
  gap: 4px;
  align-items: center;
  flex: 0 1 330px;
  min-width: 300px;
  padding: 4px;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
  background: #f8fafc;
}

.page-param-add-button {
  flex: 0 0 auto;
}

.list-toolbar-text-button {
  --n-color: #eef6ff !important;
  --n-color-hover: #dbeafe !important;
  --n-color-pressed: #bfdbfe !important;
  --n-color-focus: #eef6ff !important;
  --n-border: 1px solid #bfdbfe !important;
  --n-border-hover: 1px solid #93c5fd !important;
  --n-border-pressed: 1px solid #60a5fa !important;
  --n-border-focus: 1px solid #93c5fd !important;
  --n-text-color: #1d4ed8 !important;
  --n-text-color-hover: #1e40af !important;
  --n-text-color-pressed: #1e3a8a !important;
  --n-text-color-focus: #1d4ed8 !important;
  font-weight: 600;
}

.list-toolbar-more-button {
  --n-color: #2563eb !important;
  --n-color-hover: #1d4ed8 !important;
  --n-color-pressed: #1e40af !important;
  --n-color-focus: #2563eb !important;
  --n-border: 1px solid #2563eb !important;
  --n-border-hover: 1px solid #1d4ed8 !important;
  --n-border-pressed: 1px solid #1e40af !important;
  --n-border-focus: 1px solid #2563eb !important;
  --n-text-color: #fff !important;
  --n-text-color-hover: #fff !important;
  --n-text-color-pressed: #fff !important;
  --n-text-color-focus: #fff !important;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.18);
}

.list-toolbar-more-button :deep(.n-button__icon),
.list-toolbar-more-button :deep(.n-icon) {
  color: #fff !important;
}

.list-designer-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  min-height: 0;
}

.list-workspace {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: #f8f9fa;
  padding: 0;
}

.list-preview-modal :deep(.n-card__content),
.list-preview-modal :deep(.n-card-content) {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  height: auto;
  max-height: none;
  overflow: hidden;
  padding: 12px;
  background: #fff;
}

.list-preview-modal :deep(.n-card) {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 32px);
  max-height: calc(100vh - 32px);
  background: #fff;
  overflow: hidden;
}

:global(.list-preview-modal.n-card) {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 32px);
  max-height: calc(100vh - 32px);
  overflow: hidden;
  background: #fff;
}

:global(.list-preview-modal .n-card-header) {
  flex: 0 0 auto;
  height: 58px;
  padding: 14px 18px;
  border-bottom: 1px solid #eef2f7;
}

:global(.list-preview-modal .n-card__content),
:global(.list-preview-modal .n-card-content) {
  display: flex;
  align-items: flex-start;
  flex: 1 1 auto;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  width: 100%;
  height: auto;
  max-height: none;
  overflow: auto;
  padding: 12px;
  background: #fff;
  overscroll-behavior: contain;
}

.list-preview-modal :deep(.n-card-header) {
  flex: 0 0 auto;
  height: 58px;
  padding: 14px 18px;
}

.list-preview-modal :deep(.list-grid-designer.readonly) {
  flex: 1 1 auto;
  height: 100%;
  min-height: 0;
  background: #fff;
  overflow: hidden;
}

:global(.list-preview-modal .list-grid-designer.readonly) {
  flex: 1 1 auto;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: hidden;
  background: #fff;
}

.list-preview-modal :deep(.canvas-panel) {
  flex: 1 1 auto;
  min-height: 0;
  border: 0;
  overflow: hidden;
  background: #fff;
}

:global(.list-preview-modal .list-grid-designer.readonly .canvas-panel) {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border: 0;
  background: #fff;
}

.list-preview-modal :deep(.canvas-scroll) {
  flex: 1 1 auto;
  height: 100%;
  max-height: calc(100vh - 116px);
  min-height: 0;
  overflow: auto !important;
  overscroll-behavior: contain;
  background: #fff;
}

:global(.list-preview-modal .list-grid-designer.readonly .canvas-scroll) {
  width: 100%;
  max-width: 100%;
  min-width: 100%;
  min-height: 100%;
  height: 100%;
  overflow: auto !important;
  background: #fff;
}

.list-preview-modal :deep(.canvas-zoom-stage) {
  min-height: 100%;
  background: #fff;
}

:global(.list-preview-modal .list-grid-designer.readonly .canvas-zoom-stage) {
  width: 100%;
  max-width: 100%;
  min-width: 100%;
  min-height: 100%;
  overflow: visible;
  background: #fff;
}

.list-preview-modal :deep(.canvas-grid) {
  margin: 0;
  min-height: 100%;
  background: #fff;
  box-shadow: none;
}

:global(.list-preview-modal .list-grid-designer.readonly .canvas-grid) {
  max-width: 100%;
  min-height: 100%;
  margin: 0;
  background: #fff;
  box-shadow: none;
}

@container (max-width: 1180px) {
  .list-designer-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  .list-designer-title {
    width: 100%;
  }

  .list-designer-actions {
    justify-content: flex-start;
    max-width: 100%;
    overflow-x: auto;
    padding-bottom: 1px;
  }

  .page-switch-row {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .page-actions {
    grid-column: 2;
    justify-content: flex-end;
  }

  .page-config-row {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }

  .page-param-item {
    flex-basis: 360px;
  }
}
</style>
