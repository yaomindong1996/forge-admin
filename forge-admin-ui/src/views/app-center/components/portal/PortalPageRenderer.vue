<template>
  <section
    class="portal-page-renderer"
    :class="{ 'is-fill': fillHost }"
    :data-forge-app="applicationCode"
    :data-forge-page="pageId"
  >
    <RuntimeScopedStyles :styles="runtimeScopedStyles" />
    <ExtensionSandboxHost ref="extensionSandboxRef" />
    <iframe
      v-if="externalUrl"
      class="portal-external-frame"
      :src="externalUrl"
      :title="node?.title || '外部页面'"
      sandbox="allow-forms allow-modals allow-popups allow-same-origin allow-scripts"
    />
    <div
      v-else-if="showContentSkeleton"
      class="portal-content-skeleton"
      aria-busy="true"
      aria-label="页面内容加载中"
    >
      <n-skeleton height="36px" :sharp="false" />
      <n-skeleton text :repeat="3" />
      <n-skeleton height="220px" :sharp="false" style="margin-top: 12px" />
      <n-skeleton height="220px" :sharp="false" style="margin-top: 12px" />
    </div>
    <div
      v-else-if="blocks.length"
      class="portal-page-flow"
      :class="{ 'is-fill': fillHost, 'is-content-sized': contentSizedFlow }"
      :style="portalFlowStyle"
    >
      <section
        v-for="(block, index) in blocks"
        :key="block.id || `${block.blockType}-${index}`"
        class="portal-page-block"
        :class="{
          'is-fill': fillHost && blocks.length === 1,
          'is-runtime-form': isRuntimeAutoHeightBlock(block),
        }"
        :data-page-block-type="block.blockType"
        :style="resolveBlockShellStyle(block, index)"
      >
        <GridBlockRenderer
          :block="block"
          :fields="resolveBlockFields(block)"
          :runtime-crud-props="resolveRuntimeCrudProps(block)"
          :runtime-crud-loading="isRuntimeCrudLoading(block)"
          :data-source-configured="isDataSourceConfigured(block)"
          :runtime-interactive="true"
          :runtime-extension-hooks="runtimeExtensionHooks"
          :block-fields-resolver="resolveBlockFields"
          :runtime-crud-props-resolver="resolveRuntimeCrudProps"
          :runtime-crud-loading-resolver="isRuntimeCrudLoading"
          :data-source-configured-resolver="isDataSourceConfigured"
          :runtime-tree-active-key="runtimeTreeActiveKey"
          :selected="false"
          :readonly="!configurable"
          @runtime-tree-select="handleRuntimeTreeSelect"
        />
      </section>
    </div>
    <PortalEmptyState
      v-else
      type="empty"
      title="页面尚未配置内容"
      description="请由应用管理员进入页面设计器完成编排并重新发布。"
      :show-back="false"
    />
  </section>
</template>

<script setup>
import { NSkeleton } from 'naive-ui'
import { computed, defineAsyncComponent, h, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { crudConfigRender } from '@/api/ai'
import { executePublishedExtensionHook } from '@/api/business-extension'
import { buildRuntimeCrudProps } from '@/components/lowcode-builder/shared/runtime-crud-props'
import {
  alignSearchSchemaWithLeftTree,
  buildLeftTreeFilterParams,
  findTreePanelProps,
} from '@/components/lowcode-builder/shared/runtime-tree-table'
import ExtensionSandboxHost from '@/components/lowcode-extension/js/ExtensionSandboxHost.vue'
import {
  materializeRuntimeScopedCss,
  runRuntimeExtensions,
  selectScopedCssExtensions,
} from '@/components/lowcode-extension/runtime/application-extension-runtime'
import RuntimeScopedStyles from '@/components/lowcode-extension/runtime/RuntimeScopedStyles'
import { isRuntimeAutoHeightBlock, resolvePortalPageBlocks, shouldUseContentSizedFlow, sortBlocksByPageFlowY } from './portal-page-runtime-layout'
import { normalizePagePadding, resolvePagePaddingCss, resolvePageBlockShellStyle as computePageBlockShellStyle, DEFAULT_PAGE_PADDING } from '@/views/app-center/runtime-modules/page-flow-geometry'
import PortalEmptyState from './PortalEmptyState.vue'
import { isDataFieldBlockType } from '@/components/lowcode-builder/page/page-schema'

const PortalBlockAsyncLoader = {
  name: 'PortalBlockAsyncLoader',
  render() {
    return h('div', { class: 'portal-content-skeleton portal-content-skeleton--block', 'aria-busy': 'true' }, [
      h(NSkeleton, { height: '32px', sharp: false }),
      h(NSkeleton, { text: true, repeat: 4 }),
    ])
  },
}

const GridBlockRenderer = defineAsyncComponent({
  delay: 0,
  loadingComponent: PortalBlockAsyncLoader,
  loader: () => import('@/components/lowcode-builder/page/GridBlockRenderer.vue'),
})

const route = useRoute()

const props = defineProps({
  node: { type: Object, default: null },
  page: { type: Object, default: null },
  objects: { type: Array, default: () => [] },
  entries: { type: Array, default: () => [] },
  extensions: { type: Array, default: () => [] },
  applicationId: { type: String, default: '' },
  applicationCode: { type: String, default: '' },
  pageId: { type: String, default: '' },
  configurable: { type: Boolean, default: false },
  designPreview: { type: Boolean, default: false },
  fillHost: { type: Boolean, default: false },
  /**
   * 表单/对象保存后由父级递增。用于清空本组件 CRUD 缓存并带上请求参数，
   * 避免返回页面管理后仍用旧的 render 结果（新增表单配置不生效）。
   */
  crudConfigRevision: { type: [Number, String], default: 0 },
  /**
   * 外部注入的表单字段解析函数。
   * 当 PortalPageRenderer 渲染包含表单设计器 Schema 的区块时，
   * 该函数负责从表单资产中提取字段（含 widget 虚拟组件），
   * 并与后端 CRUD fieldCatalog 合并后作为 GridBlockRenderer 的 fields。
   */
  formFieldsResolver: { type: Function, default: null },
  /**
   * 父级在骨架阶段预热好的 CRUD props（objectKey → props）。
   * 有 seed 时首屏可直接渲染，避免壳出来后再等 render。
   */
  seedRuntimeCrudProps: { type: Object, default: () => ({}) },
})

const runtimeCrudPropsByKey = ref({})
const loadingKeys = ref(new Set())
const unavailableKeys = ref(new Set())
const extensionSandboxRef = ref(null)
const pageInitKeys = ref(new Set())
const pageInitDefaultsByObject = ref({})
const contentBootstrapStarted = ref(false)
// 树节点高亮是区块级状态；树对右表的筛选是页面级状态，避免左树 blockId 与右表 blockId 不一致时过滤失效。
const runtimeTreeFilter = ref({})
const runtimeTreeActiveKey = ref('__all__')

const extensionPageContext = computed(() => ({
  applicationId: props.applicationId,
  applicationCode: props.applicationCode,
  pageId: props.pageId || String(props.node?.id || ''),
  entryId: resolveEntry(props.node?.entryRef)?.id || props.node?.entryRef?.entryId || null,
}))
const scopedCssExtensions = computed(() => selectScopedCssExtensions(
  props.extensions,
  extensionPageContext.value,
))
const runtimeScopedStyles = computed(() => scopedCssExtensions.value
  .map((item, index) => ({
    id: String(item.id || item.extensionId || item.extensionCode || index),
    css: materializeRuntimeScopedCss(item, extensionPageContext.value),
  }))
  .filter(item => item.css.trim()))

const contentSizedOptions = computed(() => ({
  fillHost: props.fillHost,
  runtimePreview: true,
  pageId: props.pageId || props.node?.id || '',
}))

const blocks = computed(() => {
  const items = resolvePortalPageBlocks({
    page: props.page,
    node: props.node,
    resolveObjectRef,
    normalizeLegacyBlock,
  })
  return shouldUseContentSizedFlow(items, contentSizedOptions.value)
    ? sortBlocksByPageFlowY(items)
    : items
})

const contentSizedFlow = computed(() => shouldUseContentSizedFlow(blocks.value, contentSizedOptions.value))

/** 有数据块但 CRUD 尚未就绪时，整页内容骨架代替空白 */
const showContentSkeleton = computed(() => {
  if (!blocks.value.length || externalUrl.value)
    return false
  const dataBlocks = []
  visitBlocks(blocks.value, (block) => {
    if (isRuntimeDataBlock(block))
      dataBlocks.push(block)
  })
  if (!dataBlocks.length)
    return false
  const anyReady = dataBlocks.some((block) => {
    const key = resolveObjectKey(resolveObjectRef(block) || resolveObjectRef(props.node || {}))
    return Boolean(key && runtimeCrudPropsByKey.value[key])
  })
  if (anyReady)
    return false
  const stillLoading = dataBlocks.some((block) => {
    const key = resolveObjectKey(resolveObjectRef(block) || resolveObjectRef(props.node || {}))
    return Boolean(key && loadingKeys.value.has(key))
  })
  // 刚挂载、预加载尚未写入 loadingKeys 时也先盖骨架，避免闪空白
  return stillLoading || !contentBootstrapStarted.value
})

const pagePaddingCss = computed(() => resolvePagePaddingCss(
  resolvePortalRuntimePagePadding(
    props.page?.layout?.gridLayout?.pagePadding || props.page?.layout?.pagePadding,
  ),
))

/** 门户正式页：默认 8px；历史默认 24 四边收敛为 8，避免灰底大留白 */
function resolvePortalRuntimePagePadding(raw) {
  const pad = normalizePagePadding(raw, DEFAULT_PAGE_PADDING)
  const looksLikeLegacyDefault = [pad.top, pad.right, pad.bottom, pad.left].every(v => v === 24)
  if (looksLikeLegacyDefault || raw == null || raw === '')
    return { ...DEFAULT_PAGE_PADDING }
  return {
    top: Math.min(pad.top, 12),
    right: Math.min(pad.right, 12),
    bottom: Math.min(pad.bottom, 12),
    left: Math.min(pad.left, 12),
  }
}

const portalFlowStyle = computed(() => {
  const style = { padding: pagePaddingCss.value }
  if (!props.fillHost && !contentSizedFlow.value)
    style.minHeight = `${pageHeight.value}px`
  return style
})

const externalUrl = computed(() => {
  const raw = props.page?.externalUrl
    || props.page?.url
    || props.node?.externalUrl
    || resolveEntry(props.node?.entryRef)?.entryUrl
  if (!raw)
    return ''
  try {
    const url = new URL(String(raw), window.location.origin)
    return ['http:', 'https:'].includes(url.protocol) ? url.toString() : ''
  }
  catch {
    return ''
  }
})

const pageHeight = computed(() => blocks.value.reduce((bottom, block, index) => {
  const style = block.props?.style || {}
  const top = finiteNumber(style.pageFlowY, resolveDefaultBlockY(block, index))
  const height = finiteNumber(
    style.pageFlowHeight,
    readLength(style.height) || resolveDefaultBlockHeight(block),
  )
  return Math.max(bottom, top + height + 28)
}, 620))

watch(
  () => [props.node?.id, blocks.value, props.designPreview, props.configurable, props.crudConfigRevision, props.seedRuntimeCrudProps],
  (next, prev) => {
    const nextPreview = next?.[2]
    const nextConfigurable = next?.[3]
    const nextRevision = next?.[4]
    const prevPreview = prev?.[2]
    const prevConfigurable = prev?.[3]
    const prevRevision = prev?.[4]
    // 换页时保留已加载的对象 render 缓存，避免同对象反复打 /render + designPreview 草稿准备
    const invalidateAll = !prev
      || nextPreview !== prevPreview
      || nextConfigurable !== prevConfigurable
      || nextRevision !== prevRevision
    if (invalidateAll) {
      runtimeCrudPropsByKey.value = {}
      loadingKeys.value = new Set()
      unavailableKeys.value = new Set()
    }
    if (!prev || next?.[0] !== prev?.[0] || next?.[1] !== prev?.[1]) {
      runtimeTreeFilter.value = {}
      runtimeTreeActiveKey.value = '__all__'
    }
    applySeedRuntimeCrudProps()
    // PAGE_INIT 默认值按页隔离
    pageInitKeys.value = new Set()
    pageInitDefaultsByObject.value = {}
    contentBootstrapStarted.value = false
    visitBlocks(blocks.value, preloadRuntimeCrudProps)
    contentBootstrapStarted.value = true
  },
  { immediate: true, deep: true },
)

function applySeedRuntimeCrudProps() {
  const seed = props.seedRuntimeCrudProps
  if (!seed || typeof seed !== 'object')
    return
  const entries = Object.entries(seed).filter(([, value]) => value && typeof value === 'object')
  if (!entries.length)
    return
  const next = { ...runtimeCrudPropsByKey.value }
  let changed = false
  entries.forEach(([key, value]) => {
    if (!key || next[key])
      return
    next[key] = value
    changed = true
  })
  if (changed)
    runtimeCrudPropsByKey.value = next
}

function resolveObjectRef(source = {}) {
  const raw = source.objectRef || source.props?.objectRef || source.props?.runtimeObjectRef
  if (raw && typeof raw === 'object')
    return enrichObjectRef(raw)
  if (source === props.node && props.node?.objectRef)
    return enrichObjectRef(props.node.objectRef)
  return null
}

function enrichObjectRef(objectRef) {
  const targetId = String(objectRef?.objectId ?? objectRef?.id ?? '')
  const targetCode = String(objectRef?.objectCode || '')
  const object = props.objects.find(item => (
    (targetId && String(item.objectId ?? item.id ?? '') === targetId)
    || (targetCode && String(item.objectCode || '') === targetCode)
  ))
  return {
    ...(object || {}),
    ...(objectRef || {}),
    objectId: objectRef?.objectId ?? objectRef?.id ?? object?.objectId ?? object?.id,
    objectCode: objectRef?.objectCode || object?.objectCode || '',
    configKey: objectRef?.configKey || object?.configKey || '',
  }
}

function resolveObjectKey(objectRef) {
  return String(objectRef?.objectId ?? objectRef?.id ?? objectRef?.objectCode ?? objectRef?.configKey ?? '')
}

function preloadRuntimeCrudProps(block) {
  // 提示面板等装饰块不能参与 CRUD 预加载，否则会把对象 key 标成 unavailable，拖垮同页 AiCrudPage
  if (!isRuntimeDataBlock(block))
    return
  const objectRef = resolveObjectRef(block) || resolveObjectRef(props.node || {})
  const key = resolveObjectKey(objectRef)
  if (!key || runtimeCrudPropsByKey.value[key] || loadingKeys.value.has(key) || unavailableKeys.value.has(key))
    return
  const configKey = String(objectRef?.configKey || '').trim()
  if (!configKey) {
    unavailableKeys.value = new Set([...unavailableKeys.value, key])
    return
  }
  loadingKeys.value = new Set([...loadingKeys.value, key])
  void loadRuntimeCrudProps(configKey, objectRef, key)
}

async function loadRuntimeCrudProps(configKey, objectRef, key) {
  try {
    // 页面管理对可编辑用户优先读设计草稿，避免保存默认值后必须发布应用才生效。
    // 正式门户 configurable=false，仍只走已发布配置。
    let designPreview = props.designPreview || props.configurable
    const runtimeEntryId = resolveRuntimeEntryId(configKey)
    const revision = String(props.crudConfigRevision || '').trim()
    const renderOptions = {
      needTip: false,
      appId: runtimeEntryId,
      applicationId: props.applicationId,
      pageId: props.pageId || String(props.node?.id || ''),
      // 表单保存后 revision 变化，避免沿用同 URL 的旧 render 结果
      ...(revision ? { params: { configRev: revision } } : {}),
    }
    let config = null
    try {
      config = (await crudConfigRender(configKey, designPreview, renderOptions)).data
    }
    catch (error) {
      if (!designPreview)
        throw error
      // 设计预览失败时，未发布配置再打正式 render 只会再报「尚未发布」，直接放弃
      const message = String(error?.message || error?.msg || '')
      if (message.includes('尚未发布') || message.includes('无业务对象设计') || message.includes('不能预览设计草稿'))
        throw error
      designPreview = false
      config = (await crudConfigRender(configKey, false, renderOptions)).data
    }
    if (!config || typeof config !== 'object')
      throw new Error('业务对象运行配置为空')
    runtimeCrudPropsByKey.value = {
      ...runtimeCrudPropsByKey.value,
      [key]: {
        ...buildRuntimeCrudProps(config, { designPreview }),
        title: config.title || objectRef.objectName || '',
      },
    }
    await runPageInit(blockForObjectKey(key), objectRef, key)
  }
  catch (error) {
    unavailableKeys.value = new Set([...unavailableKeys.value, key])
    console.warn('[application-portal] 加载业务对象运行配置失败', error?.message || error)
  }
  finally {
    const next = new Set(loadingKeys.value)
    next.delete(key)
    loadingKeys.value = next
  }
}

function resolveRuntimeEntryId(configKey) {
  const normalized = String(configKey || '').trim()
  if (!normalized)
    return null
  const entry = props.entries.find(item => String(item?.configKey || '').trim() === normalized)
  return entry?.id ?? entry?.entryId ?? null
}

function resolveRuntimeCrudProps(block) {
  if (!isRuntimeDataBlock(block))
    return null
  const objectRef = resolveObjectRef(block) || resolveObjectRef(props.node || {})
  const key = resolveObjectKey(objectRef)
  if (key && !runtimeCrudPropsByKey.value[key] && !loadingKeys.value.has(key) && !unavailableKeys.value.has(key))
    preloadRuntimeCrudProps(block)
  const runtimeProps = key ? runtimeCrudPropsByKey.value[key] || null : null
  if (!runtimeProps)
    return null
  const treeFilter = runtimeTreeFilter.value
  const treePanelProps = findTreePanelProps(blocks.value) || {}
  const hasTreePanel = Boolean(treePanelProps && Object.keys(treePanelProps).length)
    || blocks.value.some(item => item?.blockType === 'tree-panel')
  const searchSchema = alignSearchSchemaWithLeftTree(runtimeProps.searchSchema, {
    treePanelProps,
    runtimeProps,
  })
  const pageRouteParams = resolveDeclaredPageRouteParams()
  // 页面已有 tree-panel 时，不要再套 TreeCrudTemplate，也不要把右表渲成嵌套树
  if (hasTreePanel && block?.blockType === 'AiCrudPage') {
    const { treeConfig: _treeConfig, ...runtimeOptions } = runtimeProps.options || {}
    return {
      ...runtimeProps,
      suppressTreeCrudShell: true,
      treeConfig: {},
      options: runtimeOptions,
      searchSchema,
      publicParams: {
        ...(runtimeProps.publicParams || {}),
        ...pageRouteParams,
        ...(treeFilter || {}),
      },
      formDefaultValues: {
        ...(runtimeProps.formDefaultValues || {}),
        ...pageRouteParams,
        ...(pageInitDefaultsByObject.value[key] || {}),
      },
    }
  }
  return {
    ...runtimeProps,
    searchSchema,
    publicParams: {
      ...(runtimeProps.publicParams || {}),
      ...pageRouteParams,
      ...(treeFilter || {}),
    },
    formDefaultValues: {
      ...(runtimeProps.formDefaultValues || {}),
      ...pageRouteParams,
      ...(pageInitDefaultsByObject.value[key] || {}),
    },
  }
}

/** 把页面声明的入参从 route.query 取出，供列表过滤 / 表单默认值使用。 */
function resolveDeclaredPageRouteParams() {
  const declared = props.node?.pageParams
    || props.node?.settings?.pageParams
    || props.page?.params
    || []
  if (!Array.isArray(declared) || !declared.length)
    return {}
  const query = route.query || {}
  const result = {}
  declared.forEach((item) => {
    const name = String(item?.name || '').trim()
    if (!name)
      return
    const raw = query[name]
    if (raw !== undefined && raw !== null && String(raw) !== '')
      result[name] = Array.isArray(raw) ? raw[0] : raw
    else if (item.defaultValue !== undefined && item.defaultValue !== null && String(item.defaultValue) !== '')
      result[name] = item.defaultValue
  })
  return result
}

function blockForObjectKey(key) {
  let found = null
  visitBlocks(blocks.value, (block) => {
    if (!found && resolveObjectKey(resolveObjectRef(block)) === key)
      found = block
  })
  return found || blocks.value[0] || {}
}

async function runPageInit(block, objectRef, key) {
  if (!key || pageInitKeys.value.has(key))
    return
  pageInitKeys.value = new Set([...pageInitKeys.value, key])
  try {
    const result = await executeHookForBlock('PAGE_INIT', {}, block, objectRef, null)
    pageInitDefaultsByObject.value = {
      ...pageInitDefaultsByObject.value,
      [key]: result.record,
    }
  }
  catch (error) {
    window.$message?.error(error?.message || '页面初始化增强执行失败')
  }
}

function runtimeExtensionHooks(block) {
  const objectRef = resolveObjectRef(block) || resolveObjectRef(props.node || {}) || {}
  return {
    beforeSubmit: async (data, runtimeApi) => (
      await executeHookForBlock('BEFORE_SUBMIT', data, block, objectRef, runtimeApi)
    ).record,
    afterSubmit: async (payload, runtimeApi) => {
      await executeHookForBlock('AFTER_SUBMIT', payload?.data || {}, block, objectRef, runtimeApi)
    },
    formChange: async (payload, runtimeApi) => (
      await executeHookForBlock('FORM_CHANGE', payload?.record || payload?.data || {}, block, objectRef, runtimeApi)
    ).record,
    beforeRowAction: async (payload, runtimeApi) => {
      await executeHookForBlock('ROW_ACTION', payload?.row || {}, block, objectRef, runtimeApi, payload?.action)
      return true
    },
  }
}

async function executeHookForBlock(hookCode, record, block, objectRef, runtimeApi, action = null) {
  const context = {
    ...extensionPageContext.value,
    objectId: objectRef?.objectId || objectRef?.id || null,
    entryId: resolveBlockEntryId(block),
  }
  return runRuntimeExtensions({
    extensions: props.extensions,
    hookCode,
    context,
    record,
    fieldCatalog: resolveBlockFields(block),
    sandboxExecute: (...args) => extensionSandboxRef.value?.execute?.(...args),
    serverExecute: extension => executeServerExtension(extension, hookCode, context, record, action),
    triggerAction: (actionCode, payload) => runtimeApi?.triggerAction?.(actionCode, payload),
  })
}

async function executeServerExtension(extension, hookCode, context, record, action) {
  const response = await executePublishedExtensionHook({
    applicationId: props.applicationId,
    objectId: context.objectId || null,
    entryId: context.entryId || null,
    extensionId: String(extension.id),
    hookCode,
    input: {
      record,
      ...(action ? { actionCode: action.actionCode || action.key || '' } : {}),
    },
  })
  return response?.data || {}
}

function resolveBlockEntryId(block = {}) {
  const entryRef = block.entryRef || block.props?.entryRef || props.node?.entryRef
  return resolveEntry(entryRef)?.id || entryRef?.entryId || entryRef?.id || null
}

function isRuntimeCrudLoading(block) {
  return loadingKeys.value.has(resolveObjectKey(resolveObjectRef(block) || resolveObjectRef(props.node || {})))
}

function isDataSourceConfigured(block) {
  return Boolean(resolveObjectKey(resolveObjectRef(block) || resolveObjectRef(props.node || {})))
}

function isRuntimeDataBlock(block = {}) {
  return isDataFieldBlockType(block?.blockType) || block?.blockType === 'tree-panel'
}

function handleRuntimeTreeSelect(payload = {}) {
  runtimeTreeActiveKey.value = payload.clear || !payload.key ? '__all__' : String(payload.key)
  if (!payload.filterField || payload.clear || payload.value === undefined || payload.value === null || payload.value === '') {
    runtimeTreeFilter.value = {}
    return
  }
  runtimeTreeFilter.value = buildLeftTreeFilterParams({
    filterField: payload.filterField,
    value: payload.value,
    includeChildren: payload.includeChildren !== false,
    expandedValues: payload.expandedValues,
  })
}

function resolveBlockFields(block) {
  const runtimeFields = resolveRuntimeCrudProps(block)?.fieldCatalog
  // 通过外部注入的 formFieldsResolver 合并表单设计器字段（含 widget 虚拟组件）
  if (props.formFieldsResolver) {
    const formFields = props.formFieldsResolver(block)
    if (Array.isArray(formFields) && formFields.length) {
      if (Array.isArray(runtimeFields) && runtimeFields.length) {
        return mergePortalFieldCatalogs(formFields, runtimeFields)
      }
      return formFields
    }
  }
  if (Array.isArray(runtimeFields) && runtimeFields.length)
    return runtimeFields
  if (Array.isArray(block.fields))
    return block.fields
  if (Array.isArray(block.props?.fields))
    return block.props.fields
  return []
}

/**
 * 合并表单设计器字段和运行时 CRUD 字段。
 * 优先保留表单设计器侧的 widget / 虚拟组件信息，
 * 运行时字段覆盖同 fieldCode 的数据字段元信息。
 */
function mergePortalFieldCatalogs(formFields = [], runtimeFields = []) {
  const runtimeByField = new Map(
    (Array.isArray(runtimeFields) ? runtimeFields : [])
      .filter(f => f?.field || f?.fieldCode)
      .map(f => [String(f.field || f.fieldCode).trim(), f]),
  )
  const merged = []
  const used = new Set()
  for (const field of formFields) {
    const code = String(field.field || field.fieldCode || '').trim()
    if (!code)
      continue
    const runtimeMatch = runtimeByField.get(code)
    // widget 虚拟节点保持原样，数据字段合并运行时元信息
    if (field.nodeType === 'widget') {
      merged.push(field)
    }
    else {
      merged.push({ ...field, ...(runtimeMatch || {}) })
    }
    used.add(code)
  }
  runtimeByField.forEach((field, code) => {
    if (!used.has(code))
      merged.push(field)
  })
  return merged
}

function resolveBlockShellStyle(block, index) {
  if (props.fillHost && blocks.value.length === 1) {
    return {
      position: 'relative',
      inset: 'auto',
      width: '100%',
      height: '100%',
      textAlign: block.props?.style?.textAlign || block.props?.textAlign || block.props?.align || 'left',
    }
  }
  const style = block.props?.style || {}
  const heightMode = style.heightMode || 'fixed'
  const customHeight = finiteNumber(style.pageFlowHeight, readLength(style.height) || resolveDefaultBlockHeight(block))
  const runtimeAutoHeight = isRuntimeAutoHeightBlock(block) && heightMode !== 'fixed'
  const contentSized = contentSizedFlow.value
  const isContainer = ['grid-layout', 'card', 'box-layout', 'tabs'].includes(block.blockType)

  // 文档流：彻底丢掉 pageFlowX/Y，避免 top 残留把块钉死在旧坐标
  if (contentSized) {
    const shell = {
      position: 'relative',
      left: 'auto',
      top: 'auto',
      right: 'auto',
      bottom: 'auto',
      width: '100%',
      height: 'auto',
      textAlign: style.textAlign || block.props?.textAlign || block.props?.align || 'left',
      boxSizing: 'border-box',
      overflow: 'visible',
    }
    if (isContainer && heightMode !== 'full') {
      shell.minHeight = `${Math.max(40, customHeight)}px`
      return shell
    }
    if (heightMode === 'fixed') {
      shell.height = `${Math.max(40, customHeight)}px`
      shell.minHeight = `${Math.max(40, customHeight)}px`
    }
    else if (['AiCrudPage', 'AiTable', 'data-table'].includes(block.blockType) && !runtimeAutoHeight) {
      // 左树右表 + 搜索/工具栏需要更高可视区，过小会被 overflow:hidden 压成“有数据看不见”
      const minHeight = Math.max(customHeight, 560)
      shell.height = `${minHeight}px`
      shell.minHeight = `${minHeight}px`
    }
    else {
      shell.minHeight = `${Math.max(40, customHeight)}px`
    }
    return shell
  }

  const gridLayout = props.page?.layout?.gridLayout || {}
  const pagePadding = resolvePortalRuntimePagePadding(gridLayout.pagePadding || props.page?.layout?.pagePadding)
  return computePageBlockShellStyle(block, blocks.value, {
    pageId: props.pageId || props.node?.id || '',
    pagePadding,
    pageFlowCoordSpace: gridLayout.pageFlowCoordSpace,
    // 强制走绝对布局分支（调用方已排除文档流）
    fillHost: false,
    runtimePreview: true,
  })
}

function resolveDefaultBlockHeight(block = {}) {
  if (block.blockType === 'page-title')
    return 96
  if (['divider', 'custom-html'].includes(block.blockType))
    return 88
  if (['stats-strip', 'info-panel', 'AiForm'].includes(block.blockType))
    return 128
  if (['AiCrudPage', 'AiTable', 'data-table', 'search-form', 'toolbar'].includes(block.blockType))
    return 560
  return 116
}

function resolveDefaultBlockY(block, index) {
  return blocks.value.slice(0, Math.max(0, index)).reduce(
    (top, item) => top + finiteNumber(item.props?.style?.pageFlowHeight, resolveDefaultBlockHeight(item)) + 16,
    20,
  )
}

function normalizeLegacyBlock(item, index) {
  const legacyTypes = {
    'intro': 'page-title',
    'metric-card': 'stats-strip',
    'business-list': 'AiCrudPage',
    'business-form': 'AiForm',
    'todo': 'info-panel',
    'chart': 'stats-strip',
    'text': 'custom-html',
    'image': 'info-panel',
    'columns': 'grid-layout',
  }
  return {
    ...item,
    id: item.id || `legacy-${index + 1}`,
    blockType: item.blockType || legacyTypes[item.type] || item.type || 'custom-html',
    props: { ...(item.props || {}), ...(item.content ? { content: item.content } : {}) },
  }
}

function resolveEntry(entryRef) {
  const entryId = String(entryRef?.entryId ?? entryRef?.id ?? '')
  return props.entries.find(item => String(item.id ?? item.entryId ?? '') === entryId) || null
}

function visitBlocks(source, visitor) {
  ;(source || []).forEach((block) => {
    visitor(block)
    const childCollections = [block.children, block.props?.children, block.props?.items]
    childCollections.filter(Array.isArray).forEach(children => visitBlocks(children, visitor))
  })
}

function finiteNumber(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) && number >= 0 ? number : fallback
}

function readLength(value) {
  const number = Number.parseFloat(String(value || ''))
  return Number.isFinite(number) ? number : 0
}
</script>

<style scoped>
.portal-page-renderer {
  position: relative;
  min-width: 0;
  min-height: 100%;
}

.portal-page-renderer.is-fill {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1;
  flex-direction: column;
}

.portal-content-skeleton {
  display: grid;
  flex: 1;
  align-content: start;
  gap: 10px;
  min-height: 320px;
  padding: 8px;
  box-sizing: border-box;
}

.portal-content-skeleton--block {
  min-height: 160px;
  padding: 12px;
  border: 1px dashed #e5e7eb;
  border-radius: 6px;
  background: #fafafa;
}

.portal-page-flow {
  position: relative;
  width: 100%;
  min-width: 0;
  overflow: visible;
}

.portal-page-flow.is-content-sized {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 8px;
  box-sizing: border-box;
}

.portal-page-flow.is-fill {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  box-sizing: border-box;
  /* 单页填满时由内部 AiCrudPage / 表格滚动；外层 auto 会在 padding+100% 时误出竖条 */
  overflow: hidden;
}

.portal-page-flow.is-fill.is-content-sized {
  overflow-y: auto;
  overflow-x: hidden;
}

.portal-page-block.is-fill {
  flex: 1;
  min-height: 0;
}

.portal-page-block {
  min-width: 0;
  overflow: hidden;
  border-radius: 0;
}

.portal-page-block :deep(.grid-block) {
  height: 100% !important;
  min-height: 0;
}

.portal-page-block.is-runtime-form :deep(.grid-block),
.portal-page-block[data-page-block-type='page-title'] :deep(.grid-block),
.portal-page-block[data-page-block-type='workspace-summary-metrics'] :deep(.grid-block),
.portal-page-block[data-page-block-type='info-panel'] :deep(.grid-block),
.portal-page-block[data-page-block-type='empty-state'] :deep(.grid-block),
.portal-page-block[data-page-block-type='stats-strip'] :deep(.grid-block),
.portal-page-block[data-page-block-type='custom-html'] :deep(.grid-block) {
  height: auto !important;
  min-height: 0;
  overflow: visible;
}

.portal-page-flow.is-content-sized .portal-page-block {
  position: relative !important;
  top: auto !important;
  right: auto !important;
  bottom: auto !important;
  left: auto !important;
  width: 100% !important;
  overflow: visible;
  /* 高度交给 resolveBlockShellStyle 的内联 style；不能 min-height:0，
   * 否则 AiCrudPage（flex + overflow:hidden）会在运行态被压成空白。 */
}

.portal-page-flow.is-content-sized .portal-page-block.is-runtime-form,
.portal-page-flow.is-content-sized .portal-page-block[data-page-block-type='page-title'],
.portal-page-flow.is-content-sized .portal-page-block[data-page-block-type='workspace-summary-metrics'],
.portal-page-flow.is-content-sized .portal-page-block[data-page-block-type='info-panel'],
.portal-page-flow.is-content-sized .portal-page-block[data-page-block-type='empty-state'],
.portal-page-flow.is-content-sized .portal-page-block[data-page-block-type='stats-strip'],
.portal-page-flow.is-content-sized .portal-page-block[data-page-block-type='custom-html'] {
  height: auto !important;
  min-height: 0 !important;
}

.portal-page-flow.is-content-sized .portal-page-block :deep(.grid-block) {
  height: 100% !important;
}

/* 左树右表 / 列表块：防止内容流高度不足把表格压成空白 */
.portal-page-block[data-page-block-type='AiCrudPage'] {
  min-height: 560px;
}

.portal-page-block[data-page-block-type='AiCrudPage'] :deep(.tree-crud-layout),
.portal-page-block[data-page-block-type='AiCrudPage'] :deep(.ai-crud-preview) {
  min-height: 520px;
}

.portal-page-flow.is-content-sized .portal-page-block.is-runtime-form :deep(.grid-block),
.portal-page-flow.is-content-sized .portal-page-block[data-page-block-type='page-title'] :deep(.grid-block),
.portal-page-flow.is-content-sized .portal-page-block[data-page-block-type='workspace-summary-metrics'] :deep(.grid-block),
.portal-page-flow.is-content-sized .portal-page-block[data-page-block-type='info-panel'] :deep(.grid-block),
.portal-page-flow.is-content-sized .portal-page-block[data-page-block-type='empty-state'] :deep(.grid-block),
.portal-page-flow.is-content-sized .portal-page-block[data-page-block-type='stats-strip'] :deep(.grid-block),
.portal-page-flow.is-content-sized .portal-page-block[data-page-block-type='custom-html'] :deep(.grid-block) {
  height: auto !important;
}

/* 内联编辑/详情：保持填满高度，由 AiCrudPage 表单区域内部滚动 */
.portal-page-flow:has(.ai-crud-page.is-inline-form-active) {
  display: flex;
  min-height: 0 !important;
  flex: 1 1 auto;
  flex-direction: column;
  padding: 8px;
  box-sizing: border-box;
  overflow: hidden;
}

.portal-page-block:has(.ai-crud-page.is-inline-form-active) {
  position: relative !important;
  inset: auto !important;
  width: 100% !important;
  flex: 1 1 auto;
  min-height: 0 !important;
  height: auto !important;
  overflow: hidden;
}

.portal-page-block:has(.ai-crud-page.is-inline-form-active) :deep(.grid-block) {
  height: 100% !important;
  min-height: 0;
  overflow: hidden;
}

.portal-external-frame {
  width: 100%;
  min-height: calc(100vh - 96px);
  border: 0;
  background: #fff;
}

@media (max-width: 768px) {
  .portal-page-flow {
    display: grid;
    gap: 8px;
    min-height: 0 !important;
    padding: 8px;
  }

  .portal-page-block {
    position: relative !important;
    inset: auto !important;
    width: 100% !important;
    height: auto !important;
    min-height: 120px;
    overflow-x: auto;
  }

  .portal-page-block:has(:deep(.block-AiCrudPage)),
  .portal-page-block:has(:deep(.block-AiTable)),
  .portal-page-block:has(:deep(.block-data-table)) {
    min-height: 440px;
  }

  .portal-page-block :deep(.n-data-table),
  .portal-page-block :deep(.n-form) {
    max-width: 100%;
  }

  .portal-page-block :deep(.n-data-table-base-table) {
    min-width: 680px;
  }
}
</style>
