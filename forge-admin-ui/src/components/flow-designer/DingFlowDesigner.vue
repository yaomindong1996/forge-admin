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
import FlowCanvasHeader from './canvas/FlowCanvasHeader.vue'
import {
  AddNodeButton,
  BranchAddButton,
  BranchHeader,
  EdgeLayer,
  FlowCanvas,
  MergeNode,
  NodeContextMenu,
  NodeRenderer,
} from './canvas/index.js'
import { useFlowDesigner, useFlowHistory } from './composables/index.js'
import { useApprovalNodeMerge } from './composables/useApprovalNodeMerge.js'
import { NODE_PRESENTATION } from './constants/node-presentation.js'
import { convertBpmnToJson, convertJsonToBpmn } from './converter/index.js'
import ApprovalNodeMergeModal from './panel/ApprovalNodeMergeModal.vue'
import { NodeConfigDrawer } from './panel/index.js'

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

function handleAddAfter(afterNodeId, type) {
  if (props.readonly)
    return
  history.snapshot()
  try {
    const newId = designer.addNode(afterNodeId, type)
    applyDefaultBusinessFormBindings()
    designer.selectedNodeId.value = newId
    drawerNodeId.value = newId
    drawerFocusEdgeId.value = null
    drawerVisible.value = true
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

const addButtonPositions = computed(() => {
  const list = []
  for (const node of designer.flowJson.value.nodes) {
    if (['condition', 'parallel', 'inclusive', 'end'].includes(node.nodeType))
      continue
    const pos = layoutResult.value.nodePositions.get(node.id)
    if (!pos)
      continue
    const targetPos = findNearestDownstreamPosition(node.id, pos)
    list.push({
      id: node.id,
      position: {
        x: pos.x + pos.width / 2,
        y: verticalSegmentMidpoint(pos, targetPos),
      },
    })
  }
  return list
})

function findNearestDownstreamPosition(nodeId, sourcePos) {
  if (!sourcePos)
    return null
  const sourceBottomY = sourcePos.y + sourcePos.height
  return designer.getOutgoingEdges(nodeId)
    .map(edge => layoutResult.value.nodePositions.get(edge.target))
    .filter(pos => pos && pos.y >= sourceBottomY)
    .sort((a, b) => a.y - b.y)[0] || null
}

function verticalSegmentMidpoint(sourcePos, targetPos) {
  const sourceBottomY = sourcePos.y + sourcePos.height
  if (!targetPos)
    return sourceBottomY + 28
  const targetTopY = targetPos.y
  const gap = Math.max(targetTopY - sourceBottomY, 44)
  return sourceBottomY + gap / 2
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

onMounted(async () => {
  await nextTick()
  autoFitToScreen()
  emit('ready')
})

onBeforeUnmount(() => {
  clearTimeout(emitTimer)
})
</script>

<template>
  <div
    class="ding-flow-designer relative h-full w-full" :class="{ 'is-panel-open': drawerVisible }"
    :style="{ '--flow-branch-color': NODE_PRESENTATION.condition.color }"
  >
    <FlowCanvas ref="canvasRef" :readonly="readonly" :content-bounds="layoutResult.canvasBounds">
      <template #overlay>
        <FlowCanvasHeader
          :process-name="processName || designer.flowJson.value.processName"
          :can-undo="history.canUndo.value" :can-redo="history.canRedo.value" :readonly="readonly"
          @undo="undo" @redo="redo"
        />
      </template>
      <template #edges>
        <EdgeLayer
          neutral
          :edges="designer.flowJson.value.edges"
          :paths="layoutResult.edgePaths"
          :canvas-bounds="layoutResult.canvasBounds"
          :show-labels="false"
        />
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
          @click="handleNodeClick"
          @delete="handleNodeDelete"
          @context-menu="handleContextMenu"
        />

        <AddNodeButton
          v-for="btn in addButtonPositions"
          :key="`add-${btn.id}`"
          :position="btn.position"
          :readonly="readonly"
          @select="(type) => handleAddAfter(btn.id, type)"
        />

        <BranchHeader
          v-for="(bh, idx) in branchHeaders"
          :key="`bh-${bh.edge.id}-${idx}`"
          :edge="bh.edge"
          :position="bh.position"
          :card="bh.card"
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
  border-radius: 50%;
  border-color: var(--primary-color, #356de0);
  color: var(--primary-color, #356de0);
  background: var(--bg-primary, #fff);
  box-shadow: none;
}
.is-panel-open .ding-flow-panel-host {
  width: min(480px, 44%);
  border-left: 1px solid var(--border-light, #e5e7eb);
}
@media (max-width: 760px) {
  .is-panel-open .ding-flow-panel-host {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    width: min(480px, 100%);
    z-index: 40;
  }
}
</style>
