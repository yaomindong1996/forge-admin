<script setup>
/**
 * DingFlowDesigner — 钉钉样式流程设计器主组件
 *
 * 对外接口与原 FlowModeler.vue 1:1 兼容：
 *   Props: xml / readonly
 *   Events: change(xml) / ready / import-start / import-end(success, error)
 *   Methods: setXML / getXML / reset / undo / redo
 *
 * pitfalls #7 防回环：v-model 风格父组件回写 props.xml 时，比对 lastEmittedXml 跳过。
 * pitfalls #8 BPMNPlane 修复：JSON→XML 时 convertJsonToBpmn 总写真实 process id。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  AddNodeButton,
  BranchHeader,
  EdgeLayer,
  FlowCanvas,
  layoutFlow,
  MergeNode,
  NodeContextMenu,
  NodeRenderer,
} from './canvas/index.js'
import { useFlowDesigner, useFlowHistory } from './composables/index.js'
import { convertBpmnToJson, convertJsonToBpmn } from './converter/index.js'
import { NodeConfigDrawer } from './panel/index.js'

const props = defineProps({
  xml: { type: String, default: '' },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['change', 'ready', 'import-start', 'import-end'])

const designer = useFlowDesigner()
const history = useFlowHistory(designer.flowJson, { maxStack: 50 })
const canvasRef = ref(null)
const isImporting = ref(false)
const lastEmittedXml = ref('')

const drawerVisible = ref(false)
const drawerNodeId = ref(null)
const drawerNode = computed(() => designer.getNode(drawerNodeId.value))
const drawerOutgoingEdges = computed(() =>
  drawerNodeId.value ? designer.getOutgoingEdges(drawerNodeId.value) : [],
)

const contextMenu = ref({ visible: false, position: { x: 0, y: 0 }, node: null })

const layoutResult = ref({
  nodePositions: new Map(),
  edgePaths: new Map(),
  canvasBounds: { minX: 0, minY: 0, maxX: 0, maxY: 0 },
})

function recomputeLayout() {
  layoutResult.value = layoutFlow(designer.flowJson.value)
}

watch(designer.flowJson, recomputeLayout, { immediate: true })

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

async function importXml(xml) {
  isImporting.value = true
  emit('import-start')
  try {
    const json = convertBpmnToJson(xml)
    designer.loadJson(json)
    history.clear()
    await nextTick()
    emit('import-end', true, null)
  }
  catch (err) {
    console.error('[DingFlowDesigner] importXml failed', err)
    emit('import-end', false, err)
  }
  finally {
    isImporting.value = false
  }
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

function handleNodeClick(node) {
  designer.selectedNodeId.value = node.id
  drawerNodeId.value = node.id
  drawerVisible.value = true
}

function handleNodeDelete(node) {
  if (props.readonly)
    return
  history.snapshot()
  try { designer.deleteNode(node.id) }
  catch (e) { console.warn(e) }
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
    case 'copy':
      history.snapshot()
      try { designer.copyNode(node.id) }
      catch (e) { console.warn(e) }
      break
    case 'move-up':
      history.snapshot()
      try { designer.moveNodeUp(node.id) }
      catch (e) { console.warn(e) }
      break
    case 'move-down':
      history.snapshot()
      try { designer.moveNodeDown(node.id) }
      catch (e) { console.warn(e) }
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
    designer.selectedNodeId.value = newId
    drawerNodeId.value = newId
    drawerVisible.value = true
  }
  catch (e) { console.warn(e) }
}

function handleDrawerSave(patch, nodeId) {
  history.snapshot()
  try { designer.updateNode(nodeId, patch) }
  catch (e) { console.warn(e) }
}

function handleEdgeUpdate(edgeId, patch) {
  history.snapshot()
  try { designer.updateEdge(edgeId, patch) }
  catch (e) { console.warn(e) }
}

const addButtonPositions = computed(() => {
  const list = []
  for (const node of designer.flowJson.value.nodes) {
    if (node.nodeType === 'end')
      continue
    const pos = layoutResult.value.nodePositions.get(node.id)
    if (!pos)
      continue
    list.push({
      id: node.id,
      position: { x: pos.x + pos.width / 2, y: pos.y + pos.height + 25 },
    })
  }
  return list
})

const branchHeaders = computed(() => {
  const out = []
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
      out.push({
        edge,
        position: {
          x: targetPos.x + targetPos.width / 2,
          y: (gwPos.y + gwPos.height + targetPos.y) / 2,
        },
      })
    }
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

async function setXML(xml) { await importXml(xml) }
function getXML(_formatted = false) { return convertJsonToBpmn(designer.flowJson.value) }
function reset() { designer.reset(); history.clear() }
function undo() { history.undo() }
function redo() { history.redo() }

defineExpose({
  setXML,
  getXML,
  reset,
  undo,
  redo,
  designer,
  history,
})

onMounted(async () => {
  await nextTick()
  emit('ready')
})

onBeforeUnmount(() => {
  clearTimeout(emitTimer)
})
</script>

<template>
  <div class="ding-flow-designer relative h-full w-full bg-white">
    <FlowCanvas ref="canvasRef" :readonly="readonly">
      <template #edges>
        <EdgeLayer
          :edges="designer.flowJson.value.edges"
          :paths="layoutResult.edgePaths"
          :canvas-bounds="layoutResult.canvasBounds"
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
          @click="handleNodeClick(designer.getNode(bh.edge.source))"
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

    <NodeConfigDrawer
      v-model:visible="drawerVisible"
      :node="drawerNode"
      :readonly="readonly"
      @save="handleDrawerSave"
    />
  </div>
</template>
