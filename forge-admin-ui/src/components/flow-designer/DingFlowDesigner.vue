<script setup>
/**
 * DingFlowDesigner — 钉钉样式流程设计器主组件
 *
 * 对外接口与原 FlowModeler.vue 1:1 兼容：
 *   Props: xml / readonly
 *   Events: change(xml) / ready / importStart(@import-start) / importEnd(@import-end)
 *   Methods: setXML / getXML / reset / undo / redo
 *
 * pitfalls #7 防回环：v-model 风格父组件回写 props.xml 时，比对 lastEmittedXml 跳过。
 * pitfalls #8 BPMNPlane 修复：JSON→XML 时 convertJsonToBpmn 总写真实 process id。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, toRef, watch } from 'vue'
import { normalizeFlowFieldCatalog, serializeFlowFormPermissions } from '@/utils/flow-field-permissions'
import { layoutDesignerFlow } from './canvas/designer-layout.js'
import { pointFromDock } from './canvas/edge-dock.js'
import FlowCanvasHeader from './canvas/FlowCanvasHeader.vue'
import {
  BranchAddButton,
  BranchHeader,
  CanvasModeToolbar,
  EdgeLayer,
  FlowCanvas,
  MergeNode,
  NodeContextMenu,
  NodeRenderer,
} from './canvas/index.js'
import { useFlowDesigner, useFlowHistory } from './composables/index.js'
import { useApprovalNodeMerge } from './composables/useApprovalNodeMerge.js'
import { useFreeCanvasInteraction } from './composables/useFreeCanvasInteraction.js'
import { NODE_PRESENTATION } from './constants/node-presentation.js'
import { convertBpmnToJson, convertJsonToBpmn } from './converter/index.js'
import ApprovalNodeMergeModal from './panel/ApprovalNodeMergeModal.vue'
import { NodeConfigDrawer } from './panel/index.js'
import NodeShelf from './shelf/NodeShelf.vue'

const props = defineProps({
  xml: { type: String, default: '' },
  processName: { type: String, default: '' },
  readonly: { type: Boolean, default: false },
  formAssetOptions: { type: Array, default: () => [] },
  formFieldCatalog: { type: Array, default: () => [] },
  autoBindBusinessForm: { type: Boolean, default: false },
  defaultFormKey: { type: String, default: '' },
  processConfig: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['change', 'ready', 'importStart', 'importEnd'])

const designer = useFlowDesigner()
const history = useFlowHistory(designer.flowJson, { maxStack: 50 })
const canvasRef = ref(null)
const panelHost = ref(null)
const isImporting = ref(false)
const lastEmittedXml = ref('')

const drawerVisible = ref(false)
const drawerNodeId = ref(null)
const drawerFocusEdgeId = ref(null)
const drawerRef = ref(null)
const drawerNode = computed(() => designer.getNode(drawerNodeId.value))
const drawerOutgoingEdges = computed(() => drawerNodeId.value ? designer.getOutgoingEdges(drawerNodeId.value) : [])

const contextMenu = ref({ visible: false, position: { x: 0, y: 0 }, node: null })
const { mergeOwnerId, openNodeMerge, confirmNodeMerge } = useApprovalNodeMerge({
  designer,
  history,
  readonly: toRef(props, 'readonly'),
  drawerVisible,
  onMerged: handleNodeClick,
})

// layoutResult 用 computed 同步求值，避免 ref+watch 异步时序导致新增节点
// 先用旧位置渲染（position=undefined → 节点堆叠在左上角）的问题
const layoutResult = computed(() => layoutDesignerFlow(designer.flowJson.value))

const freeCanvas = useFreeCanvasInteraction({
  designer,
  history,
  layoutResult,
  canvasRef,
  readonly: toRef(props, 'readonly'),
  onNodePlaced: (id) => {
    designer.selectedNodeId.value = id
    drawerNodeId.value = id
    drawerFocusEdgeId.value = null
    drawerVisible.value = true
  },
})

/** 预览线 SVG 必须用像素尺寸；width/height=100% 在无高度父级上会变成 0，虚线直接看不见 */
const linkPreviewSvgSize = computed(() => {
  const b = layoutResult.value.canvasBounds || { maxX: 1000, maxY: 800, minX: 0, minY: 0 }
  const w = Math.ceil(Math.max(Number(b.maxX) || 0, Number(b.minX) || 0, 800) + 400)
  const h = Math.ceil(Math.max(Number(b.maxY) || 0, Number(b.minY) || 0, 600) + 400)
  return {
    width: Number.isFinite(w) ? Math.min(Math.max(w, 1200), 20000) : 1200,
    height: Number.isFinite(h) ? Math.min(Math.max(h, 1200), 20000) : 1200,
  }
})

const linkPreview = computed(() => {
  const state = freeCanvas.linking.value
  if (!state)
    return null
  if (state.mode === 'reconnect') {
    const fixed = layoutResult.value.nodePositions.get(state.fixedId)
    if (!fixed)
      return null
    const edge = designer.getEdge?.(state.edgeId)
      || designer.flowJson.value.edges.find(e => e.id === state.edgeId)
    // 固定端用真实锚点，移动端跟鼠标（与新建连线一致的蓝虚线）
    const fixedDock = state.which === 'target'
      ? (edge?.uiSourceDock || null)
      : (edge?.uiTargetDock || null)
    const fp = fixedDock
      ? pointFromDock(fixed, fixedDock)
      : null
    const fx = fp?.x ?? (fixed.x + fixed.width / 2)
    const fy = fp?.y ?? (state.which === 'target' ? fixed.y + fixed.height : fixed.y)
    if (state.which === 'target') {
      return {
        x1: fx,
        y1: fy,
        x2: state.x,
        y2: state.y,
        tipX: state.x,
        tipY: state.y,
      }
    }
    return {
      x1: state.x,
      y1: state.y,
      x2: fx,
      y2: fy,
      tipX: state.x,
      tipY: state.y,
    }
  }
  if (!state.sourceId)
    return null
  const source = layoutResult.value.nodePositions.get(state.sourceId)
  if (!source)
    return null
  // 从源锚点（指定边或自动底边）拉出蓝虚线
  let x1 = source.x + source.width / 2
  let y1 = source.y + source.height
  if (state.sourceDock?.side) {
    const p = pointFromDock(source, state.sourceDock)
    if (p) {
      x1 = p.x
      y1 = p.y
    }
  }
  return {
    x1,
    y1,
    x2: state.x,
    y2: state.y,
    tipX: state.x,
    tipY: state.y,
  }
})

const reconnectingState = computed(() => {
  const state = freeCanvas.linking.value
  if (state?.mode !== 'reconnect')
    return null
  return { edgeId: state.edgeId, which: state.which }
})

/** 普通对象传给 EdgeLayer，避免 Map 作为 prop 时取不到 path */
const edgePathRecord = computed(() => {
  const map = layoutResult.value.edgePaths
  const out = Object.create(null)
  if (map && typeof map.forEach === 'function') {
    map.forEach((path, id) => {
      out[id] = path
    })
  }
  return out
})

function onDesignerNodeClick(node) {
  if (freeCanvas.onNodeClick(node))
    return
  freeCanvas.clearEdgeSelection()
  handleNodeClick(node)
}

// 侧栏展开后按剩余画布重算适配，不让当前节点藏到配置区背后。
watch(drawerVisible, async () => {
  await nextTick()
  autoFitToScreen()
})

watch(
  () => props.xml,
  async (xml) => {
    if (xml && xml === lastEmittedXml.value)
      return
    if (xml)
      await importXml(xml)
    else
      designer.reset()
  },
  { immediate: true },
)

watch(
  () => props.processConfig,
  (config) => {
    applyProcessConfig(config)
  },
  { immediate: true, deep: true },
)

watch(
  () => [props.formAssetOptions, props.defaultFormKey, props.autoBindBusinessForm],
  () => {
    applyDefaultBusinessFormBindings()
  },
  { immediate: true, deep: true },
)

function normalizeProcessConfig(config = {}) {
  const mode = ['firstOnly', 'consecutive', 'none'].includes(config.autoApprovalMode)
    ? config.autoApprovalMode
    : 'none'
  const rejectStrategy = (() => {
    const text = String(config.rejectStrategy || '').trim().toUpperCase()
    if (!text)
      return 'MANUAL'
    if (text === 'TO_END' || text === 'END' || text === 'TERMINATE')
      return 'TO_END'
    if (text === 'MANUAL' || text === 'NONE' || text === 'OFF')
      return 'MANUAL'
    if (text === 'TO_INITIATOR_MODIFY' || text === 'TO_START' || text === 'MODIFY')
      return 'TO_INITIATOR_MODIFY'
    return 'MANUAL'
  })()
  return {
    allowSubmitterWithdraw: config.allowSubmitterWithdraw !== false,
    autoApprovalMode: mode,
    rejectStrategy,
  }
}

function applyProcessConfig(config = {}) {
  const normalized = normalizeProcessConfig(config)
  const current = designer.flowJson.value.config || {}
  if (
    current.allowSubmitterWithdraw === normalized.allowSubmitterWithdraw
    && current.autoApprovalMode === normalized.autoApprovalMode
    && current.rejectStrategy === normalized.rejectStrategy
  ) {
    return
  }
  designer.flowJson.value = {
    ...designer.flowJson.value,
    config: {
      ...current,
      ...normalized,
    },
  }
}

async function importXml(xml) {
  let businessFormBindingChanged = false
  isImporting.value = true
  emit('importStart')
  try {
    const json = convertBpmnToJson(xml)
    // 未声明自由布局时清掉残留坐标，避免旧 Diagram Bounds 把树状流程拖成蜘蛛网
    if (!json.config?.freeLayout) {
      for (const node of json.nodes || [])
        delete node.uiPosition
    }
    designer.loadJson(json)
    businessFormBindingChanged = applyDefaultBusinessFormBindings()
    history.clear()
    await nextTick()
    autoFitToScreen()
    emit('importEnd', true, null)
  }
  catch (err) {
    console.error('[DingFlowDesigner] importXml failed', err)
    emit('importEnd', false, err)
  }
  finally {
    isImporting.value = false
    if (businessFormBindingChanged)
      scheduleEmit()
  }
}

function applyDefaultBusinessFormBindings() {
  if (props.readonly || !props.autoBindBusinessForm)
    return false
  const assets = normalizeBusinessFormAssets(props.formAssetOptions)
  if (!assets.length)
    return false
  const preferred = assets.find(asset => asset.formKey === String(props.defaultFormKey || '').trim()) || assets[0]
  let changed = false
  const nodes = designer.flowJson.value.nodes.map((node) => {
    if (node?.nodeType !== 'approver')
      return node
    const config = node.config || {}
    const current = findMatchingFormAsset(assets, config)
    const asset = current || preferred
    const shouldReplaceAsset = !current
    const shouldClearInlineForm = Boolean(config.formJson || config.formUrl)
    const shouldInitializePermissions = !Array.isArray(config.formFieldPermissions)
      || config.formFieldPermissions.length === 0
    if (!shouldReplaceAsset && !shouldClearInlineForm && !shouldInitializePermissions)
      return node
    changed = true
    return {
      ...node,
      config: {
        ...config,
        formType: 'dynamic',
        formMode: asset.formMode,
        formKey: asset.formKey,
        formName: asset.formName,
        providerKey: asset.providerKey,
        formJson: '',
        formUrl: asset.formUrl,
        viewKey: asset.viewKey,
        formRef: buildBusinessFormRef(asset),
        formFieldPermissions: shouldReplaceAsset || shouldInitializePermissions
          ? buildDefaultFieldPermissions(asset.fieldCatalog)
          : config.formFieldPermissions,
      },
    }
  })
  if (!changed)
    return false
  designer.flowJson.value = {
    ...designer.flowJson.value,
    nodes,
  }
  return true
}

function normalizeBusinessFormAssets(source = []) {
  return (Array.isArray(source) ? source : [])
    .map((item) => {
      const formKey = String(item?.formKey || item?.value || '').trim()
      if (!formKey)
        return null
      const fields = Array.isArray(item?.fieldCatalog)
        ? item.fieldCatalog
        : Array.isArray(item?.fields) ? item.fields : []
      return {
        ...item,
        formKey,
        formName: item.formName || item.label || formKey,
        formMode: normalizeBusinessFormMode(item.formMode || item.type),
        providerKey: String(item.providerKey || ''),
        formUrl: String(item.formUrl || ''),
        viewKey: String(item.viewKey || 'default'),
        fieldCatalog: fields,
      }
    })
    .filter(Boolean)
}

function findMatchingFormAsset(assets, config = {}) {
  const formKey = String(config.formKey || '').trim()
  if (!formKey)
    return null
  const providerKey = String(config.providerKey || config.formRef?.providerKey || '')
  const formMode = normalizeBusinessFormMode(
    config.formMode || config.formRef?.formMode || config.formRef?.type,
  )
  return assets.find(asset => asset.formKey === formKey
    && asset.formMode === formMode
    && asset.providerKey === providerKey)
  || assets.find(asset => asset.formKey === formKey && asset.providerKey === providerKey)
  || assets.find(asset => asset.formKey === formKey)
  || null
}

function normalizeBusinessFormMode(value) {
  const mode = String(value || 'BUSINESS_OBJECT_FORM').toUpperCase()
  if (mode === 'BUSINESS_CODE_FORM' || mode === 'EXTERNAL')
    return mode
  return 'BUSINESS_OBJECT_FORM'
}

function buildBusinessFormRef(asset) {
  return {
    ...(asset.formRef || {}),
    type: asset.formMode,
    formMode: asset.formMode,
    objectCode: asset.objectCode || '',
    objectName: asset.objectName || '',
    applicationId: asset.applicationId || '',
    pageId: asset.pageId || '',
    pageCode: asset.pageCode || '',
    pageName: asset.pageName || '',
    pageType: asset.pageType || '',
    sourceFormKey: asset.sourceFormKey || '',
    providerKey: asset.providerKey,
    formKey: asset.formKey,
    formName: asset.formName,
    formUrl: asset.formUrl,
    viewKey: asset.viewKey,
  }
}

function buildDefaultFieldPermissions(fieldCatalog = []) {
  const seen = new Set()
  const catalog = normalizeFlowFieldCatalog(fieldCatalog)
  const fields = catalog
    .map((item) => {
      const field = String(item?.field || '').trim()
      if (!field)
        return null
      const permissionKey = item.scope === 'child'
        ? `child:${item.childKey}:${item.childField || field}`
        : item.scope === 'array'
          ? `array:${item.arrayKey}:${item.itemField || field}`
          : `main:${field}`
      if (seen.has(permissionKey))
        return null
      seen.add(permissionKey)
      const required = item?.required === true || item?.sourceRequired === true
      return {
        field,
        fieldCode: field,
        label: String(item?.label || item?.title || item?.fieldName || field).trim(),
        ...(item.scope === 'child'
          ? { scope: 'child', childKey: item.childKey, childField: item.childField || field }
          : (item.scope === 'array'
              ? { scope: 'array', arrayKey: item.arrayKey, itemField: item.itemField || field }
              : {})),
        visible: true,
        editable: item.scope !== 'child',
        readable: true,
        writable: item.scope !== 'child',
        required,
      }
    })
    .filter(Boolean)

  const arrays = Array.from(new Map(catalog
    .filter(item => item.scope === 'array' && item.arrayKey)
    .map(item => [item.arrayKey, {
      arrayKey: item.arrayKey,
      label: item.arrayLabel || item.arrayKey,
      readable: true,
      allowCreate: false,
      allowUpdate: true,
      allowDelete: false,
    }])).values())

  return serializeFlowFormPermissions(fields, [], arrays)
}

let emitTimer = null
function scheduleEmit() {
  if (props.readonly || isImporting.value)
    return
  clearTimeout(emitTimer)
  emitTimer = setTimeout(() => {
    try {
      const xml = convertJsonToBpmn(designer.flowJson.value)
      lastEmittedXml.value = xml
      emit('change', xml)
    }
    catch (err) {
      console.warn('[DingFlowDesigner] convertJsonToBpmn failed', err)
    }
  }, 200)
}

watch(designer.flowJson, scheduleEmit)

function handleNodeClick(node, options = {}) {
  if (!node)
    return
  designer.selectedNodeId.value = node.id
  drawerNodeId.value = node.id
  drawerFocusEdgeId.value = options.focusEdgeId || null
  drawerVisible.value = true
}

function handleBranchHeaderClick(edge) {
  const gatewayNode = designer.getNode(edge.source)
  handleNodeClick(gatewayNode, { focusEdgeId: edge.id })
}

function handleDrawerVisibleUpdate(visible) {
  drawerVisible.value = visible
  if (!visible)
    drawerFocusEdgeId.value = null
}

function handleNodeDelete(node) {
  if (props.readonly)
    return
  history.snapshot()
  try {
    designer.deleteNode(node.id)
  }
  catch (e) {
    console.warn(e)
  }
}

function handleContextMenu({ event, node }) {
  contextMenu.value = {
    visible: true,
    position: { x: event.clientX, y: event.clientY },
    node,
  }
}

function handleContextAction(actionId, node) {
  contextMenu.value.visible = false
  if (!node)
    return
  switch (actionId) {
    case 'edit':
      handleNodeClick(node)
      break
    case 'merge':
      openNodeMerge(node)
      break
    case 'copy':
      history.snapshot()
      try {
        designer.copyNode(node.id)
      }
      catch (e) {
        console.warn(e)
      }
      break
    case 'move-up':
      history.snapshot()
      try {
        designer.moveNodeUp(node.id)
      }
      catch (e) {
        console.warn(e)
      }
      break
    case 'move-down':
      history.snapshot()
      try {
        designer.moveNodeDown(node.id)
      }
      catch (e) {
        console.warn(e)
      }
      break
    case 'delete':
      handleNodeDelete(node)
      break
  }
}

function openNewNode(newId) {
  applyDefaultBusinessFormBindings()
  freeCanvas.clearEdgeSelection()
  designer.selectedNodeId.value = newId
  drawerNodeId.value = newId
  drawerFocusEdgeId.value = null
  drawerVisible.value = true
}

/** 底部工具栏「＋」锚点文案：选中可用节点时显示名称 */
const toolbarAddAnchorLabel = computed(() => {
  const id = designer.selectedNodeId.value
  if (!id)
    return ''
  const node = designer.getNode(id)
  if (!node || node.nodeType === 'end')
    return ''
  return String(node.name || node.id || '').trim()
})

/** 卡片内「添加」：固定从该节点后插入 */
function handleNodeAdd(node, type) {
  if (props.readonly || !node?.id || !type)
    return
  history.snapshot()
  try {
    openNewNode(designer.addNode(node.id, type))
  }
  catch (e) {
    console.warn(e)
  }
}

/** 底部工具栏「＋」：有选中节点/边则串入链路，否则放置游离节点（网关仍需锚点） */
function handleToolbarAddNode(type) {
  if (props.readonly || !type)
    return
  history.snapshot()
  try {
    const gatewayTypes = new Set(['condition', 'parallel', 'inclusive'])
    const selectedId = designer.selectedNodeId.value
    const selected = selectedId ? designer.getNode(selectedId) : null
    const edgeId = freeCanvas.selectedEdgeId.value
    const edge = edgeId
      ? designer.flowJson.value.edges.find(item => item.id === edgeId)
      : null

    let newId
    if (edge?.source) {
      newId = designer.addNode(edge.source, type)
    }
    else if (selected && selected.nodeType !== 'end') {
      newId = designer.addNode(selected.id, type)
    }
    else if (gatewayTypes.has(type)) {
      const start = designer.flowJson.value.nodes.find(n => n.nodeType === 'start')
      if (!start)
        throw new Error('缺少开始节点，无法添加分支')
      newId = designer.addNode(start.id, type)
    }
    else {
      const b = layoutResult.value.canvasBounds || { minX: 0, minY: 0, maxX: 800, maxY: 600 }
      newId = designer.placeNode(type, {
        x: Math.round((b.minX + b.maxX) / 2 - 90),
        y: Math.round((b.minY + b.maxY) / 2),
      })
    }

    openNewNode(newId)
  }
  catch (e) {
    console.warn(e)
  }
}

function handleDrawerSave(patch, nodeId) {
  history.snapshot()
  try {
    designer.updateNode(nodeId, patch)
  }
  catch (e) {
    console.warn(e)
  }
}

function handleDrawerEdgeUpdate(edgeId, patch) {
  try {
    designer.updateEdge(edgeId, patch)
  }
  catch (e) {
    console.warn(e)
  }
}

function handleAddBranch(gatewayId) {
  if (props.readonly)
    return
  history.snapshot()
  try {
    const result = designer.addBranch(gatewayId)
    designer.selectedNodeId.value = gatewayId
    drawerNodeId.value = gatewayId
    drawerFocusEdgeId.value = result.edgeId
    drawerVisible.value = true
  }
  catch (e) {
    console.warn(e)
  }
}

function nodeCenterX(pos) {
  return pos.x + pos.width / 2
}

function branchLaneY(gatewayPos, targetPos) {
  return (gatewayPos.y + gatewayPos.height + targetPos.y) / 2
}

function segmentLength(a, b) {
  if (!a || !b)
    return 0
  return Math.hypot(b.x - a.x, b.y - a.y)
}

function findLongestSegment(points, predicate) {
  let best = null
  for (let i = 1; i < points.length; i += 1) {
    const a = points[i - 1]
    const b = points[i]
    if (!predicate(a, b))
      continue
    const length = segmentLength(a, b)
    if (!best || length > best.length)
      best = { a, b, length }
  }
  return best
}

function branchHeaderPosition(edge, gatewayPos, targetPos) {
  const path = layoutResult.value.edgePaths.get(edge.id)?.points || []
  const horizontal = findLongestSegment(path, (a, b) => Math.abs(a.y - b.y) < 1 && Math.abs(a.x - b.x) > 24)
  if (horizontal) {
    return {
      x: (horizontal.a.x + horizontal.b.x) / 2,
      y: horizontal.a.y - 18,
    }
  }

  const vertical = findLongestSegment(path, (a, b) => Math.abs(a.x - b.x) < 1 && Math.abs(a.y - b.y) > 24)
  if (vertical) {
    const gatewayCenterX = nodeCenterX(gatewayPos)
    const targetCenterX = nodeCenterX(targetPos)
    const labelOnLeft = Math.abs(gatewayCenterX - targetCenterX) < 1 || targetCenterX <= gatewayCenterX
    return {
      x: targetCenterX + (labelOnLeft ? -86 : 86),
      y: (vertical.a.y + vertical.b.y) / 2,
    }
  }

  const gatewayCenterX = nodeCenterX(gatewayPos)
  const targetCenterX = nodeCenterX(targetPos)
  return {
    x: Math.abs(gatewayCenterX - targetCenterX) < 1 ? targetCenterX - 86 : (gatewayCenterX + targetCenterX) / 2,
    y: branchLaneY(gatewayPos, targetPos),
  }
}

function avoidHeaderCollision(position, placed) {
  const next = { ...position }
  let guard = 0
  while (
    guard < 8
    && placed.some(item => Math.abs(item.x - next.x) < 128 && Math.abs(item.y - next.y) < 30)
  ) {
    next.y += 30
    guard += 1
  }
  placed.push(next)
  return next
}

/** 选中连线时隐藏两端节点中心端口，避免与线端点圆左右错位叠双圈 */
function shouldShowNodePorts(nodeId) {
  if (reconnectingState.value || freeCanvas.tool.value === 'connect' || freeCanvas.linking.value?.mode === 'new')
    return true
  const edgeId = freeCanvas.selectedEdgeId.value
  if (!edgeId)
    return true
  const edge = designer.flowJson.value.edges.find(item => item.id === edgeId)
  if (!edge)
    return true
  return edge.source !== nodeId && edge.target !== nodeId
}

const branchHeaders = computed(() => {
  const out = []
  const placed = []
  for (const node of designer.flowJson.value.nodes) {
    if (!['condition', 'parallel', 'inclusive'].includes(node.nodeType))
      continue
    const gwPos = layoutResult.value.nodePositions.get(node.id)
    if (!gwPos)
      continue
    for (const edge of designer.getOutgoingEdges(node.id)) {
      const targetPos = layoutResult.value.nodePositions.get(edge.target)
      if (!targetPos)
        continue
      const cardPosition = layoutResult.value.branchCards.get(edge.id)
      const position = cardPosition || branchHeaderPosition(edge, gwPos, targetPos)
      out.push({
        edge,
        card: Boolean(cardPosition),
        compact: node.nodeType === 'parallel',
        index: designer.getOutgoingEdges(node.id).findIndex(item => item.id === edge.id) + 1,
        gatewayType: node.nodeType,
        position: avoidHeaderCollision(position, placed),
      })
    }
  }
  return out
})

const branchAddButtons = computed(() => {
  const out = []
  for (const node of designer.flowJson.value.nodes) {
    if (node.nodeType !== 'condition')
      continue
    const gwPos = layoutResult.value.nodePositions.get(node.id)
    if (!gwPos)
      continue
    const outgoing = designer.getOutgoingEdges(node.id)
    const targetPositions = outgoing
      .map(edge => layoutResult.value.nodePositions.get(edge.target))
      .filter(Boolean)
    if (!targetPositions.length)
      continue
    const gatewayBottomY = gwPos.y + gwPos.height
    const downstreamTargets = targetPositions
      .filter(pos => pos.y >= gatewayBottomY)
      .sort((a, b) => a.y - b.y)
    const anchorTarget = downstreamTargets[0]
      || targetPositions
        .slice()
        .sort((a, b) => Math.abs(a.y - gatewayBottomY) - Math.abs(b.y - gatewayBottomY))[0]
    out.push({
      gatewayId: node.id,
      position: {
        x: nodeCenterX(gwPos),
        y: layoutResult.value.branchJunctions.get(node.id)
          ?? (anchorTarget ? Math.min(branchLaneY(gwPos, anchorTarget), gatewayBottomY + 40) : gatewayBottomY + 40),
      },
    })
  }
  return out
})

const mergeMarkers = computed(() => {
  const out = []
  for (const node of designer.flowJson.value.nodes) {
    if (!node.config?.mergeNode)
      continue
    const pos = layoutResult.value.nodePositions.get(node.id)
    if (!pos)
      continue
    out.push({ id: node.id, position: { x: pos.x + pos.width / 2, y: pos.y - 12 } })
  }
  return out
})

async function setXML(xml) {
  await importXml(xml)
}

function commitOpenDrawerDraft() {
  if (drawerVisible.value)
    drawerRef.value?.commitDraft?.()
}

function getXML(_formatted = false) {
  commitOpenDrawerDraft()
  return convertJsonToBpmn({
    ...designer.flowJson.value,
    config: {
      ...(designer.flowJson.value.config || {}),
      ...normalizeProcessConfig(props.processConfig),
    },
  })
}

function reset() {
  designer.reset()
  history.clear()
}

function undo() {
  history.undo()
}

function redo() {
  history.redo()
}

defineExpose({
  setXML,
  getXML,
  reset,
  undo,
  redo,
  designer,
  history,
})

async function handleAutoLayout() {
  freeCanvas.autoLayout()
  await nextTick()
  autoFitToScreen()
}

/** 自动居中：把画布内容 fit 到视口中央 */
function autoFitToScreen() {
  const canvas = canvasRef.value
  if (!canvas?.containerRef)
    return
  const container = canvas.containerRef
  const b = layoutResult.value.canvasBounds
  if (!b || b.maxX <= b.minX || b.maxY <= b.minY)
    return
  const contentW = b.maxX - b.minX
  const contentH = b.maxY - b.minY
  canvas.fitToScreen(contentW, contentH, container.clientWidth, container.clientHeight, 72, b.minX, b.minY)
}

let unbindHistoryKeys = null

onMounted(async () => {
  await nextTick()
  autoFitToScreen()
  if (!props.readonly)
    unbindHistoryKeys = history.bindKeyboard(window)
  emit('ready')
})

onBeforeUnmount(() => {
  clearTimeout(emitTimer)
  unbindHistoryKeys?.()
  unbindHistoryKeys = null
})
</script>

<template>
  <div
    class="ding-flow-designer relative h-full w-full" :class="{ 'is-panel-open': drawerVisible }"
    :style="{ '--flow-branch-color': NODE_PRESENTATION.condition.color }"
  >
    <NodeShelf
      :readonly="readonly"
      :tool="freeCanvas.tool.value"
      @connect-tool="freeCanvas.setTool('connect')"
    />
    <FlowCanvas
      ref="canvasRef"
      :readonly="readonly"
      :tool="freeCanvas.tool.value"
      :content-bounds="layoutResult.canvasBounds"
      @canvas-drop="freeCanvas.onCanvasDrop"
      @canvas-drag-over="freeCanvas.onCanvasDragOver"
      @canvas-click="(e) => { if (!e?.target?.closest?.('[data-node-id], .edge-path, .edge-endpoint, .edge-endpoint-group, .edge-bend, .edge-seg-handle')) freeCanvas.clearEdgeSelection() }"
    >
      <template #overlay>
        <FlowCanvasHeader
          :process-name="processName || designer.flowJson.value.processName"
          :can-undo="history.canUndo.value" :can-redo="history.canRedo.value" :readonly="readonly"
          @undo="undo" @redo="redo" @auto-layout="handleAutoLayout"
        />
        <CanvasModeToolbar
          :tool="freeCanvas.tool.value"
          :readonly="readonly"
          :add-anchor-label="toolbarAddAnchorLabel"
          @update:tool="freeCanvas.setTool"
          @auto-layout="handleAutoLayout"
          @add-node="handleToolbarAddNode"
          @connect="freeCanvas.setTool('connect')"
        />
      </template>
      <template #edges>
        <EdgeLayer
          neutral
          interactive
          :edges="designer.flowJson.value.edges"
          :paths="edgePathRecord"
          :canvas-bounds="layoutResult.canvasBounds"
          path-type="orthogonal"
          :selected-edge-id="freeCanvas.selectedEdgeId.value"
          :reconnecting="reconnectingState"
          :selected-waypoint-index="freeCanvas.selectedWaypointIndex.value"
          :show-labels="false"
          @edge-click="freeCanvas.selectEdge"
          @hit-pointer-down="freeCanvas.onEdgeHitPointerDown"
          @endpoint-down="freeCanvas.onEndpointDown"
          @bend-down="freeCanvas.onBendDown"
          @segment-down="freeCanvas.onSegmentDown"
        />
        <!-- 蓝虚线预览（新建/改接共用），盖在最上层 -->
        <svg
          v-if="linkPreview"
          class="link-preview pointer-events-none absolute left-0 top-0 overflow-visible"
          :width="linkPreviewSvgSize.width"
          :height="linkPreviewSvgSize.height"
          :viewBox="`0 0 ${linkPreviewSvgSize.width} ${linkPreviewSvgSize.height}`"
          style="z-index: 50"
        >
          <line
            :x1="linkPreview.x1"
            :y1="linkPreview.y1"
            :x2="linkPreview.x2"
            :y2="linkPreview.y2"
            stroke="#356de0"
            stroke-width="2"
            stroke-dasharray="6 4"
            stroke-linecap="round"
          />
          <circle
            :cx="linkPreview.x1"
            :cy="linkPreview.y1"
            r="3.5"
            fill="#356de0"
          />
          <circle
            :cx="linkPreview.tipX"
            :cy="linkPreview.tipY"
            r="5"
            fill="#fff"
            stroke="#356de0"
            stroke-width="2"
          />
        </svg>
        <!-- 拖拽对齐标线：横/竖对齐或近似直线时出现 -->
        <svg
          v-if="freeCanvas.alignmentGuides.value.length"
          class="alignment-guides pointer-events-none absolute left-0 top-0 overflow-visible"
          :width="linkPreviewSvgSize.width"
          :height="linkPreviewSvgSize.height"
          :viewBox="`0 0 ${linkPreviewSvgSize.width} ${linkPreviewSvgSize.height}`"
          style="z-index: 45"
        >
          <line
            v-for="(guide, gi) in freeCanvas.alignmentGuides.value"
            :key="`guide-${guide.orientation}-${gi}`"
            :x1="guide.orientation === 'v' ? guide.position : guide.start"
            :y1="guide.orientation === 'v' ? guide.start : guide.position"
            :x2="guide.orientation === 'v' ? guide.position : guide.end"
            :y2="guide.orientation === 'v' ? guide.end : guide.position"
            stroke="#356de0"
            stroke-width="1"
            stroke-dasharray="4 3"
            opacity="0.85"
          />
        </svg>
      </template>
      <template #nodes>
        <NodeRenderer
          v-for="node in designer.flowJson.value.nodes"
          :key="node.id"
          :node="node"
          :position="layoutResult.nodePositions.get(node.id)"
          :selected="designer.selectedNodeId.value === node.id"
          :readonly="readonly"
          :outgoing-count="designer.getOutgoingEdges(node.id).length"
          :show-ports="shouldShowNodePorts(node.id)"
          :ports-active="Boolean(reconnectingState) || freeCanvas.tool.value === 'connect' || freeCanvas.linking.value?.mode === 'new'"
          :reattach-which="reconnectingState?.which || (freeCanvas.linking.value?.mode === 'new' ? 'target' : null)"
          :connect-source="freeCanvas.linking.value?.mode === 'new' && freeCanvas.linking.value?.sourceId === node.id"
          @click="onDesignerNodeClick"
          @delete="handleNodeDelete"
          @context-menu="handleContextMenu"
          @pointer-down="freeCanvas.onNodePointerDown"
          @port-pointer-down="freeCanvas.startLink"
          @add="handleNodeAdd"
          @connect="freeCanvas.setTool('connect')"
        />

        <!-- 选中卡片内「添加」为主；底部「＋」兜底；条件网关另保留加分支 -->

        <BranchHeader
          v-for="(bh, idx) in branchHeaders"
          :key="`bh-${bh.edge.id}-${idx}`"
          :edge="bh.edge"
          :position="bh.position"
          :card="bh.card"
          :compact="bh.compact"
          :index="bh.index"
          :gateway-type="bh.gatewayType"
          :fields="formFieldCatalog"
          @click="handleBranchHeaderClick"
        />

        <BranchAddButton
          v-for="btn in branchAddButtons"
          :key="`branch-add-${btn.gatewayId}`"
          :position="btn.position"
          :readonly="readonly"
          @click="handleAddBranch(btn.gatewayId)"
        />

        <MergeNode
          v-for="m in mergeMarkers"
          :key="`merge-${m.id}`"
          :position="m.position"
        />
      </template>
    </FlowCanvas>

    <NodeContextMenu
      :visible="contextMenu.visible"
      :position="contextMenu.position"
      :node="contextMenu.node"
      :readonly="readonly"
      @close="contextMenu.visible = false"
      @action="handleContextAction"
    />

    <!-- 配置区只占当前设计器，不遮住顶部保存/发布，也不遮罩画布 -->
    <ApprovalNodeMergeModal :owner-id="mergeOwnerId" :flow="designer.flowJson.value" @confirm="confirmNodeMerge" />
    <div ref="panelHost" class="ding-flow-panel-host" />
    <NodeConfigDrawer
      ref="drawerRef"
      :visible="drawerVisible"
      :node="drawerNode"
      :outgoing-edges="drawerOutgoingEdges"
      :nodes="designer.flowJson.value.nodes"
      :form-asset-options="formAssetOptions"
      :form-field-catalog="formFieldCatalog"
      :focus-edge-id="drawerFocusEdgeId"
      :readonly="readonly"
      :mount-to="panelHost"
      @update:visible="handleDrawerVisibleUpdate"
      @save="handleDrawerSave"
      @update:edge="handleDrawerEdgeUpdate"
    />
  </div>
</template>

<style scoped>
.ding-flow-designer {
  display: flex;
  min-width: 0;
  min-height: 0;
  background: var(--bg-secondary, #f6f8fb);
}
.link-preview {
  z-index: 2;
}
.ding-flow-designer > :deep(.flow-canvas) {
  flex: 1;
  min-width: 0;
  width: auto;
}
.ding-flow-panel-host {
  position: relative;
  flex: none;
  width: 0;
  min-height: 0;
}
.ding-flow-designer :deep(.branch-node-shell .branch-node-diamond) {
  border-color: var(--border-light, #dfe4ec);
  background: var(--bg-primary, #fff);
  box-shadow: none;
}
.ding-flow-designer :deep(.branch-node-icon) {
  color: var(--flow-branch-color);
}
.ding-flow-designer :deep(.branch-node-shell.is-selected .branch-node-diamond) {
  border-color: var(--primary-color, #356de0);
}
.ding-flow-designer :deep(.branch-add-btn) {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border-color: var(--primary-color, #356de0);
  color: var(--primary-color, #356de0);
  background: var(--bg-primary, #fff);
  box-shadow: none;
  font-size: 12px;
}
.is-panel-open .ding-flow-panel-host {
  width: min(400px, 38%);
  border-left: 1px solid var(--border-light, #e5e7eb);
}
@media (max-width: 760px) {
  .is-panel-open .ding-flow-panel-host {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    width: min(400px, 100%);
    z-index: 40;
  }
}
</style>
