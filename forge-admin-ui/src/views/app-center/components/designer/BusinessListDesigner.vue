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
        <n-select
          class="list-template-select"
          :value="currentTemplateValue"
          :options="listTemplateOptions"
          size="small"
          @update:value="updateListTemplate"
        />
        <n-radio-group :value="listLayoutMode" size="small" @update:value="updateListLayoutMode">
          <n-radio-button value="grid">
            自由画布
          </n-radio-button>
          <n-radio-button value="structured">
            CRUD配置
          </n-radio-button>
        </n-radio-group>
        <n-dropdown trigger="click" :options="listMoreOptions" @select="handleListMoreSelect">
          <n-button class="list-toolbar-more-button" circle size="small" type="primary" title="更多操作">
            <template #icon>
              <n-icon><EllipsisHorizontalOutline /></n-icon>
            </template>
          </n-button>
        </n-dropdown>
      </n-space>
    </div>

    <div v-if="listLayoutMode === 'grid'" class="list-page-switch">
      <div class="page-switch-row">
        <div class="page-switch-title">
          <span class="page-switch-icon">P</span>
          <div>
            <strong>页面设计</strong>
            <small>{{ activeDesignerPage?.pageName || '当前页面' }} · {{ pageTypeText(activeDesignerPage?.pageType) }}</small>
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
        <n-space size="small" align="center" class="page-actions">
          <n-button class="page-tool-button page-icon-button" circle size="small" secondary :title="pageSettingsExpanded ? '收起页面设置' : '页面设置'" @click="pageSettingsExpanded = !pageSettingsExpanded">
            <template #icon>
              <n-icon><SettingsOutline /></n-icon>
            </template>
          </n-button>
          <n-button class="page-tool-button page-icon-button" circle size="small" secondary title="复制页面" @click="duplicateActivePage">
            <template #icon>
              <n-icon><CopyOutline /></n-icon>
            </template>
          </n-button>
          <n-button class="page-tool-button page-icon-button" circle size="small" secondary title="重置当前页面布局" @click="resetActivePageLayout">
            <template #icon>
              <n-icon><RefreshOutline /></n-icon>
            </template>
          </n-button>
          <n-popconfirm
            :show-icon="false"
            positive-text="清空"
            negative-text="取消"
            @positive-click="clearActivePageLayout"
          >
            <template #trigger>
              <n-button class="page-tool-button page-icon-button" circle size="small" secondary title="清空当前页面布局">
                <template #icon>
                  <n-icon><CloseCircleOutline /></n-icon>
                </template>
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
              <n-button class="page-tool-button page-icon-button" circle size="small" secondary type="error" :disabled="isProtectedPage(activePageKey)" title="删除当前页面">
                <template #icon>
                  <n-icon><TrashOutline /></n-icon>
                </template>
              </n-button>
            </template>
            删除当前页面及布局？列表页不允许删除。
          </n-popconfirm>
        </n-space>
      </div>
      <div v-if="activeDesignerPage && pageSettingsExpanded" class="page-config-row">
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
          placeholder="路由片段/页面标识，如 detail/:id"
          title="当前多页面仍由业务对象运行页统一承载，这里用于事件跳转、弹窗页和详情页识别，不是单独前端路由文件"
          @update:value="patchActivePage({ routePath: $event })"
        />
        <n-input
          :value="activeDesignerPage.description"
          size="small"
          placeholder="页面说明"
          @update:value="patchActivePage({ description: $event })"
        />
      </div>
      <div v-if="activeDesignerPage && pageSettingsExpanded" class="page-param-row">
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
      <div v-if="activeDesignerPage?.pageType === 'detail' && pageSettingsExpanded" class="page-data-row">
        <div class="page-param-title">
          详情数据
        </div>
        <n-select
          :value="activeDesignerPage.detailMethod || 'get'"
          :options="requestMethodOptions"
          size="small"
          class="page-method-select"
          @update:value="patchActivePage({ detailMethod: $event || 'get' })"
        />
        <n-input
          :value="activeDesignerPage.detailApi || ''"
          size="small"
          placeholder="详情接口，如 get@/api/customer/:id 或 /api/customer/:id"
          @update:value="patchActivePage({ detailApi: $event })"
        />
        <n-input
          :value="activeDesignerPage.detailDataField || 'data'"
          size="small"
          placeholder="数据字段，如 data"
          @update:value="patchActivePage({ detailDataField: $event || 'data' })"
        />
      </div>
    </div>

    <div class="list-designer-body">
      <main class="list-workspace">
        <ListPageGridDesigner
          v-if="listLayoutMode === 'grid'"
          :model-value="currentPageGridLayout"
          :fields="designFields"
          :model-schema="effectiveModelSchema"
          :layout-type="localSchema.layoutType"
          :page-name="activeDesignerPage?.pageName || '列表页'"
          :pages="designerPages"
          :form-options="formOptions"
          :runtime-crud-props="designerRuntimeCrudProps"
          @update:model-value="handleGridLayoutUpdate"
        />
        <StructuredListPageDesigner
          v-else
          v-model="localSchema"
          :fields="designFields"
          :model-schema="effectiveModelSchema"
          :layout-type="localSchema.layoutType"
          :pages="designerPages"
        />
      </main>
    </div>

    <n-modal
      v-model:show="listPreviewVisible"
      preset="card"
      title="预览当前列表"
      class="list-preview-modal"
      :bordered="false"
      :style="{ width: 'calc(100vw - 32px)', maxWidth: 'calc(100vw - 32px)', height: 'calc(100vh - 32px)' }"
    >
      <ListPageGridDesigner
        :model-value="previewGridLayout"
        :fields="designFields"
        :model-schema="effectiveModelSchema"
        :layout-type="localSchema.layoutType"
        :page-name="activeDesignerPage?.pageName || '列表页'"
        :pages="designerPages"
        :form-options="formOptions"
        :runtime-crud-props="designerRuntimeCrudProps"
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
  CloseCircleOutline,
  CopyOutline,
  EllipsisHorizontalOutline,
  RefreshOutline,
  SettingsOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { saveBusinessObjectDesigner, saveBusinessObjectListLayout } from '@/api/business-app'
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
  createGridBlock,
  createPageModelRef,
  isPageFieldVisible,
  LIST_PAGE_DESIGN_WIDTH,
  LIST_PAGE_GRID_BASE_COL_WIDTH,
  LIST_PAGE_GRID_GAP,
  resolveDefaultTreeConfig,
  syncGridLayoutWithModel,
  syncPageSchemaWithModel,
} from '@/components/lowcode-builder/page/page-schema'
import StructuredListPageDesigner from '@/components/lowcode-builder/page/StructuredListPageDesigner.vue'
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
})

const emit = defineEmits(['update:modelValue', 'update:viewSchema', 'saved', 'dirtyChange'])

const message = useMessage()
const saving = ref(false)
const undoStack = ref([])
const redoStack = ref([])
const activePageKey = ref('list')
const pageSettingsExpanded = ref(false)
const HISTORY_LIMIT = 50
let applyingExternalSchema = false

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
const listLayoutMode = computed(() => localSchema.value.listLayoutMode || 'grid')
const treeLayoutEnabled = computed(() => localSchema.value.layoutType === 'tree-crud')
const currentTemplateValue = computed(() => treeLayoutEnabled.value ? 'tree-crud' : 'simple-crud')
const layoutModeLabel = computed(() => resolveLayoutModeLabel(localSchema.value.layoutType))
const canUndo = computed(() => undoStack.value.length > 0)
const canRedo = computed(() => redoStack.value.length > 0)
const listPreviewVisible = ref(false)
const pageTypeOptions = [
  { label: '列表页', value: 'list' },
  { label: '详情页', value: 'detail' },
  { label: '新增页', value: 'create' },
  { label: '编辑页', value: 'edit' },
  { label: '弹窗页', value: 'dialog' },
  { label: '抽屉页', value: 'drawer' },
  { label: '自定义页', value: 'custom' },
]
const requestMethodOptions = [
  { label: 'GET', value: 'get' },
  { label: 'POST', value: 'post' },
]
const listTemplateOptions = [
  { label: '标准 CRUD 列表', value: 'simple-crud' },
  { label: '左树右表模板', value: 'tree-crud' },
]
const listMoreOptions = computed(() => [
  {
    label: '按字段生成查询条件',
    key: 'resetSearchFields',
  },
  {
    label: '按字段生成表格列',
    key: 'resetTableFields',
  },
  {
    type: 'divider',
    key: 'listMoreDivider',
  },
  {
    label: '新增空白页面',
    key: 'addPage',
  },
  {
    type: 'divider',
    key: 'pageDivider',
  },
  {
    label: '重置当前列表',
    key: 'resetListSchema',
  },
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
const designerRuntimeCrudProps = computed(() => buildDesignerRuntimeCrudProps(localSchema.value, designFields.value))

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

function updateListLayoutMode(value) {
  if (value === listLayoutMode.value)
    return
  const next = {
    ...localSchema.value,
    listLayoutMode: value,
  }
  if (value === 'grid') {
    const grid = next.listGridLayout?.items?.length
      ? next.listGridLayout
      : createDefaultListGridLayout(effectiveModelSchema.value, { layoutType: next.layoutType })
    next.listGridLayout = syncGridLayoutWithModel(grid, effectiveModelSchema.value, { layoutType: next.layoutType })
    next.pages = updateDesignerPageGrid(next.pages || [], 'list', next.listGridLayout)
    next.zones = applyGridLayoutToZones(next.zones || [], next.listGridLayout, effectiveModelSchema.value)
  }
  setLocalSchema(resolveSchema(next, effectiveModelSchema.value))
}

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
      return {
        ...item,
        fieldRefs,
        props: {
          ...(item.props || {}),
          ...normalizedTableProps,
          ...normalizedPreviousProps,
          title: normalizedPreviousProps.title || normalizedTableProps.title || item.props?.title,
          showSearch: searchZone.enabled !== false,
          showImport: normalizedPreviousProps.showImport ?? normalizedTableProps.showImport ?? (hasPreviousToolbar ? toolbarActions.includes('import') : item.props?.showImport),
          showExport: normalizedPreviousProps.showExport ?? normalizedTableProps.showExport ?? (hasPreviousToolbar ? toolbarActions.includes('export') : item.props?.showExport),
          hideBatchDelete: normalizedPreviousProps.hideBatchDelete ?? normalizedTableProps.hideBatchDelete ?? (hasPreviousToolbar ? !toolbarActions.includes('batch-delete') : item.props?.hideBatchDelete),
          enableCustomQuery: normalizedPreviousProps.enableCustomQuery ?? normalizedTableProps.enableCustomQuery ?? (hasPreviousToolbar ? toolbarActions.includes('custom-query') : item.props?.enableCustomQuery),
          defaultSortField: normalizedPreviousProps.defaultSortField || normalizedTableProps.defaultSortField || 'id',
          defaultSortOrder: normalizedPreviousProps.defaultSortOrder || normalizedTableProps.defaultSortOrder || 'desc',
          fieldSettings: {
            ...(searchProps.fieldSettings || {}),
            ...(normalizedTableProps.fieldSettings || {}),
            ...(normalizedPreviousProps.fieldSettings || {}),
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
  updateTreeLayoutEnabled(value === 'tree-crud')
}

function handleGridLayoutUpdate(layout) {
  const synced = syncGridLayoutWithModel(layout, effectiveModelSchema.value, { layoutType: localSchema.value.layoutType })
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
  if (key === 'resetListSchema')
    resetListSchema()
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
  copy.pageType = source.pageType === 'list' || source.pageType === 'detail' ? 'custom' : source.pageType || 'custom'
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
  const gridLayout = page.pageKey === 'detail' || page.pageType === 'detail'
    ? createDefaultDetailGridLayout(effectiveModelSchema.value, localSchema.value.layoutType)
    : page.pageKey === 'list' || page.pageType === 'list'
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
  if (localSchema.value.listLayoutMode === 'grid') {
    const targetBlockType = zoneKey === 'search' ? 'search-form' : 'data-table'
    const nextGridLayout = {
      ...(localSchema.value.listGridLayout || {}),
      items: (localSchema.value.listGridLayout?.items || []).map(item => item.blockType === targetBlockType
        ? { ...item, fieldRefs: nextRefs }
        : item),
    }
    setLocalSchema({
      ...localSchema.value,
      listGridLayout: nextGridLayout,
      pages: updateDesignerPageGrid(localSchema.value.pages || [], 'list', nextGridLayout),
      zones: applyGridLayoutToZones(localSchema.value.zones || [], nextGridLayout, effectiveModelSchema.value),
    })
    return
  }
  setLocalSchema({
    ...localSchema.value,
    zones: (localSchema.value.zones || []).map(zone => zone.zoneKey === zoneKey
      ? { ...zone, fieldRefs: nextRefs }
      : zone),
  })
}

function resetListSchema() {
  const nextLayoutType = localSchema.value.layoutType
  const nextSchema = resolveSchema(createDefaultPageSchema(effectiveModelSchema.value), effectiveModelSchema.value)
  const nextZones = nextLayoutType === 'tree-crud'
    ? updateTreeZone(nextSchema.zones || [], true)
    : nextSchema.zones
  const nextGridLayout = localSchema.value.listLayoutMode === 'grid'
    ? syncGridLayoutWithModel(createDefaultListGridLayout(effectiveModelSchema.value, { layoutType: nextLayoutType }), effectiveModelSchema.value, { layoutType: nextLayoutType })
    : nextSchema.listGridLayout
  setLocalSchema({
    ...nextSchema,
    layoutType: nextLayoutType,
    zones: localSchema.value.listLayoutMode === 'grid'
      ? applyGridLayoutToZones(nextZones || [], nextGridLayout, effectiveModelSchema.value)
      : nextZones,
    listLayoutMode: localSchema.value.listLayoutMode || nextSchema.listLayoutMode || 'grid',
    listGridLayout: nextGridLayout,
    pages: ensureDesignerPages({ ...nextSchema, listGridLayout: nextGridLayout, layoutType: nextLayoutType }, effectiveModelSchema.value),
  })
}

async function saveLayout() {
  if (!props.objectId)
    return
  const schema = normalizeSchemaForSave(resolveSchema(localSchema.value, effectiveModelSchema.value))
  saving.value = true
  try {
    await saveBusinessObjectListLayout(props.objectId, {
      layoutKey: 'list',
      layoutName: '列表布局',
      layoutType: schema.layoutType,
      pageSchema: cloneSchema(schema),
      zones: schema.zones?.filter(zone => ['search', 'table'].includes(zone.zoneKey)) || [],
      settings: {
        listLayoutMode: schema.listLayoutMode,
      },
    })
    const viewSchema = buildCurrentViewSchema(schema)
    await saveBusinessObjectDesigner(props.objectId, {
      pageSchema: cloneSchema(schema),
      viewSchema: cloneSchema(viewSchema),
    })
    setLocalSchema(schema, { external: true })
    emit('update:viewSchema', cloneSchema(viewSchema))
    emit('saved', cloneSchema(schema))
    emit('dirtyChange', false)
    message.success('列表布局已保存')
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

function buildDesignerRuntimeCrudProps(schema = {}, fields = []) {
  const zones = schema.zones || []
  const searchZone = zones.find(zone => zone.zoneKey === 'search') || {}
  const tableZone = zones.find(zone => zone.zoneKey === 'table') || {}
  const editZone = zones.find(zone => zone.zoneKey === 'edit') || {}
  const tableProps = tableZone.props || {}
  const searchProps = searchZone.props || {}
  const editProps = editZone.props || {}
  const apiValues = resolveDesignerDefaultApiValues(schema)
  const fieldMap = buildDesignerFieldMap(fields)
  const editFields = buildDesignerEditSchema(editZone, fieldMap)
  const hookHandlers = buildDesignerCrudHookHandlers(tableProps)
  return {
    lazy: true,
    loadDetailOnEdit: false,
    columns: buildDesignerColumns(tableZone, fieldMap),
    searchSchema: buildDesignerSearchSchema(searchZone, fieldMap),
    editSchema: editFields,
    apiConfig: {
      list: tableProps.listApi || apiValues.listApi,
      detail: tableProps.detailApi || apiValues.detailApi,
      create: tableProps.createApi || apiValues.createApi,
      update: tableProps.updateApi || apiValues.updateApi,
      delete: tableProps.deleteApi || apiValues.deleteApi,
      import: tableProps.importApi || '',
      export: tableProps.exportApi || '',
      tree: tableProps.treeApi || '',
    },
    api: tableProps.api || apiValues.api,
    rowKey: tableProps.rowKey || 'id',
    showSearch: searchZone.enabled !== false && tableProps.showSearch !== false,
    showPagination: tableProps.showPagination !== false,
    searchGridCols: searchProps.gridCols || tableProps.searchGridCols || 4,
    searchLabelWidth: searchProps.labelWidth || tableProps.searchLabelWidth || 'auto',
    searchEnableCollapse: tableProps.searchEnableCollapse !== false,
    searchMaxVisibleFields: tableProps.searchMaxVisibleFields || 3,
    searchYGap: tableProps.searchYGap ?? 16,
    editGridCols: editProps.editGridCols || tableProps.editGridCols || 1,
    editLabelWidth: editProps.editLabelWidth || editProps.labelWidth || tableProps.editLabelWidth || 'auto',
    editLabelPlacement: editProps.editLabelPlacement || editProps.labelPlacement || tableProps.editLabelPlacement || 'left',
    editLabelAlign: editProps.editLabelAlign || editProps.labelAlign || tableProps.editLabelAlign || 'right',
    editSize: editProps.editSize || editProps.size || tableProps.editSize || 'medium',
    editShowFeedback: editProps.editShowFeedback ?? editProps.showFeedback ?? tableProps.editShowFeedback ?? true,
    editXGap: editProps.editXGap ?? editProps.columnGap ?? tableProps.editXGap ?? 16,
    editYGap: editProps.editYGap ?? editProps.rowGap ?? tableProps.editYGap ?? 8,
    modalWidth: tableProps.modalWidth || editProps.modalWidth || '800px',
    detailModalWidth: tableProps.detailModalWidth || 'min(1080px, 92vw)',
    formOpenMode: tableProps.formOpenMode || editProps.formOpenMode || tableProps.modalType || editProps.modalType || 'modal',
    tabWorkspace: tableProps.tabWorkspace || editProps.tabWorkspace || {},
    modalType: tableProps.modalType || editProps.modalType || 'modal',
    drawerPlacement: tableProps.drawerPlacement || 'right',
    hideModalFooter: tableProps.hideModalFooter === true,
    hideDefaultDetailContent: tableProps.hideDefaultDetailContent === true,
    hideToolbar: tableProps.hideToolbar === true,
    hideAdd: tableProps.hideAdd === true,
    hideBatchDelete: tableProps.hideBatchDelete === true,
    showImport: tableProps.showImport === true,
    showExport: tableProps.showExport === true,
    showExportTasks: tableProps.showExportTasks !== false,
    enableCustomQuery: tableProps.enableCustomQuery === true,
    addButtonText: tableProps.addButtonText || '新增',
    exportButtonText: tableProps.exportButtonText || '导出',
    exportFileName: tableProps.exportFileName || '',
    renderMode: tableProps.renderMode || 'table',
    showRenderModeSwitch: tableProps.showRenderModeSwitch !== false,
    tableSize: tableProps.tableSize || 'small',
    bordered: Boolean(tableProps.bordered),
    striped: Boolean(tableProps.striped),
    hideSelection: tableProps.hideSelection === true,
    maxHeight: tableProps.maxHeight || undefined,
    scrollX: tableProps.scrollX || undefined,
    listMethod: tableProps.listMethod || 'get',
    listDataField: tableProps.listDataField || 'records',
    listTotalField: tableProps.listTotalField || 'total',
    isEncrypt: tableProps.isEncrypt === true,
    publicParams: tableProps.publicParams || {},
    publicQuery: tableProps.publicQuery || {},
    formDefaultValues: tableProps.formDefaultValues || {},
    submitDefaultParams: tableProps.submitDefaultParams || {},
    ...hookHandlers,
  }
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
  return resolveDesignerZoneRefs(zone, fieldMap).map((fieldCode) => {
    const field = fieldMap.get(fieldCode) || {}
    const setting = zone.props?.fieldSettings?.[fieldCode] || {}
    return {
      key: fieldCode,
      field: fieldCode,
      title: setting.label || field.label || field.fieldName || fieldCode,
      minWidth: Number(setting.width || field.width || 110),
      align: setting.align || 'left',
      ellipsis: { tooltip: true },
    }
  })
}

function buildDesignerSearchSchema(zone = {}, fieldMap = new Map()) {
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
  const resolved = resolveSchema(schema, effectiveModelSchema.value)
  const listPage = (resolved.pages || []).find(page => page.pageKey === 'list')
  const sourceGrid = listPage?.gridLayout || resolved.listGridLayout
  if (!sourceGrid)
    return resolved
  const syncedGrid = syncGridLayoutWithModel(sourceGrid, effectiveModelSchema.value, { layoutType: resolved.layoutType })
  return {
    ...resolved,
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
  return {
    ...schema,
    layoutType,
    listLayoutMode: schema.listLayoutMode || 'grid',
    pages: ensureDesignerPages(schema, modelSchema),
  }
}

function ensureDesignerPages(schema, modelSchema) {
  const sourcePages = Array.isArray(schema.pages) ? schema.pages : []
  const removedPageSet = new Set(schema.removedPageKeys || [])
  const listPage = sourcePages.find(page => page.pageKey === 'list')
  const detailPage = sourcePages.find(page => page.pageKey === 'detail')
  const hasExplicitDetailGrid = detailPage && Object.prototype.hasOwnProperty.call(detailPage, 'gridLayout')
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
    ...(!removedPageSet.has('detail') || detailPage
      ? [normalizeDesignerPage(detailPage, {
          pageKey: 'detail',
          pageName: '详情页',
          pageType: 'detail',
          routePath: 'detail/:id',
          description: '业务详情页面',
          params: [{ name: 'id', value: ':id' }],
          detailMethod: detailPage?.detailMethod || 'get',
          detailApi: detailPage?.detailApi || '',
          detailDataField: detailPage?.detailDataField || 'data',
          gridLayout: hasExplicitDetailGrid ? detailPage.gridLayout : createDefaultDetailGridLayout(modelSchema, schema.layoutType),
        })]
      : []),
    ...sourcePages
      .filter(page => !['list', 'detail'].includes(page.pageKey))
      .map(page => normalizeDesignerPage(page, {
        pageKey: page.pageKey,
        pageName: page.pageName || '自定义页面',
        pageType: page.pageType || 'custom',
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
    pageType: page?.pageType || defaults.pageType || 'custom',
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
        pageName: pageKey === 'detail' ? '详情页' : '列表页',
        pageType: pageKey,
        routePath: pageKey === 'detail' ? 'detail/:id' : '',
        description: '',
        params: pageKey === 'detail' ? [{ name: 'id', value: ':id' }] : [],
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

function createDefaultDetailGridLayout(modelSchema, layoutType = 'simple-crud') {
  const back = createGridBlock('back-button', modelSchema, { gridX: 0, gridY: 0 })
  const title = createGridBlock('page-title', modelSchema, { gridX: 2, gridY: 0 })
  const detail = createGridBlock('detail-info', modelSchema, { gridX: 0, gridY: 2 })
  if (back) {
    back.gridW = 2
    back.gridH = 1
  }
  if (title) {
    title.label = '详情页标题'
    title.gridW = 10
    title.gridH = 2
    title.props = {
      ...(title.props || {}),
      title: '详情信息',
      subtitle: '查看当前业务记录的完整信息',
      statusText: '详情',
      statusType: 'info',
    }
  }
  if (detail) {
    detail.label = '详情信息'
    detail.gridW = 12
    detail.gridH = 8
    detail.props = {
      ...(detail.props || {}),
      title: '基础信息',
      columnCount: 2,
      bordered: false,
    }
  }
  return {
    cols: 12,
    rowHeight: 32,
    gap: 8,
    designWidth: LIST_PAGE_DESIGN_WIDTH,
    layoutType,
    items: [back, title, detail].filter(Boolean),
  }
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
    return '左树右表模板'
  if (layoutType === 'master-detail-crud')
    return '已启用关联数据'
  return '标准列表'
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
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleListDesignerShortcut)
  window.removeEventListener('forge-list-designer:preview-current-list', openLocalPreview)
})
</script>

<style scoped>
.business-list-designer {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  height: calc(100vh - 106px);
  min-height: 0;
  container-type: inline-size;
}

.list-designer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 40px;
  border-bottom: 1px solid #e5e7eb;
  padding: 6px 10px;
  background: #fff;
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
  color: #111827;
  font-size: 15px;
  line-height: 20px;
  letter-spacing: 0;
}

.list-designer-head p {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-designer-actions {
  flex: 0 0 auto;
  flex-wrap: nowrap;
}

.list-template-select {
  width: 128px;
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
  display: grid;
  gap: 8px;
  padding: 8px 10px 10px;
  border-bottom: 1px solid #eef2f7;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(248, 251, 255, 0.98) 100%),
    radial-gradient(circle at 24px 18px, rgba(37, 99, 235, 0.08), transparent 28%);
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
  gap: 7px;
  min-width: 0;
  flex: 0 0 auto;
}

.page-switch-icon {
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  border-radius: 5px;
  background: #2563eb;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  box-shadow: 0 6px 12px rgba(37, 99, 235, 0.18);
}

.page-switch-title strong {
  display: block;
  color: #0f172a;
  font-size: 12px;
  line-height: 15px;
}

.page-switch-title small {
  display: block;
  max-width: 150px;
  overflow: hidden;
  color: #64748b;
  font-size: 10px;
  line-height: 14px;
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
  grid-template-columns: auto minmax(0, 1fr) auto;
  justify-content: stretch;
  gap: 8px;
}

.page-tab-list {
  display: flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 0;
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
  min-width: 96px;
  max-width: 148px;
  min-height: 30px;
  padding: 4px 8px 4px 21px;
  border: 1px solid #dbe3ee;
  border-radius: 7px;
  background: #fff;
  color: #475569;
  text-align: left;
  cursor: pointer;
  transition: all 0.16s ease;
}

.page-tab-button::before {
  content: '';
  position: absolute;
  top: 10px;
  left: 8px;
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #94a3b8;
  box-shadow: 0 0 0 3px #f1f5f9;
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
  color: #0f172a;
  font-size: 11px;
  font-weight: 700;
}

.page-tab-button small {
  color: #64748b;
  font-size: 9px;
}

.page-tab-button:hover,
.page-tab-button.active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.1);
}

.page-tab-button.active span,
.page-tab-button.active small {
  color: #1d4ed8;
}

.page-actions {
  flex: 0 0 auto;
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

.page-config-row {
  display: grid;
  grid-template-columns:
    minmax(120px, 0.9fr)
    minmax(120px, 0.9fr)
    minmax(108px, 0.7fr)
    minmax(160px, 1.2fr)
    minmax(180px, 1.4fr);
  gap: 8px;
  align-items: end;
  padding: 10px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
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
  padding: 8px 10px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
}

.page-data-row {
  display: grid;
  grid-template-columns: auto 88px minmax(260px, 1fr) minmax(140px, 0.45fr);
  gap: 8px;
  padding: 10px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
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
  overflow: auto;
  background: #f8fafc;
  padding: 8px;
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
  flex: 0 0 auto;
  width: max-content;
  min-width: 100%;
  min-height: 100%;
  height: auto;
  overflow: visible;
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
  width: max-content;
  min-width: 100%;
  min-height: 100%;
  overflow: visible;
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
  width: max-content;
  min-width: 100%;
  min-height: 100%;
  height: auto;
  overflow: visible !important;
  background: #fff;
}

.list-preview-modal :deep(.canvas-zoom-stage) {
  min-height: 100%;
  background: #fff;
}

:global(.list-preview-modal .list-grid-designer.readonly .canvas-zoom-stage) {
  width: max-content;
  min-width: 100%;
  min-height: 100%;
  background: #fff;
}

.list-preview-modal :deep(.canvas-grid) {
  margin: 0;
  min-height: 100%;
  background: #fff;
  box-shadow: none;
}

:global(.list-preview-modal .list-grid-designer.readonly .canvas-grid) {
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
