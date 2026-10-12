<template>
  <n-modal
    :show="show"
    preset="card"
    class="formula-graph-modal"
    title="公式依赖图"
    :bordered="false"
    @update:show="emit('update:show', $event)"
  >
    <div class="formula-graph-panel">
      <header class="graph-toolbar">
        <div>
          <span>公式字段</span>
          <strong>{{ formulaCount }}</strong>
        </div>
        <div>
          <span>依赖边</span>
          <strong>{{ graphData.edges?.length || 0 }}</strong>
        </div>
        <n-tag v-if="graphData.hasCycle" type="error" :bordered="false">
          循环依赖
        </n-tag>
        <n-button-group size="small" class="graph-zoom-tools">
          <n-button secondary @click="zoomGraph(-0.1)">
            -
          </n-button>
          <n-button secondary @click="resetGraphView">
            {{ Math.round(zoom * 100) }}%
          </n-button>
          <n-button secondary @click="zoomGraph(0.1)">
            +
          </n-button>
        </n-button-group>
        <n-button size="small" :loading="loading" secondary @click="loadGraph">
          刷新
        </n-button>
      </header>

      <n-empty v-if="!graphData.nodes?.length && !loading" size="small" description="暂无公式依赖" />
      <section v-else class="graph-canvas">
        <div class="graph-svg-stage">
          <svg
            class="dependency-svg"
            :width="graphWidth * zoom"
            :height="graphHeight * zoom"
            :viewBox="`0 0 ${graphWidth} ${graphHeight}`"
            role="img"
            aria-label="公式依赖图"
          >
            <defs>
              <marker
                id="formula-graph-arrow"
                markerWidth="10"
                markerHeight="10"
                refX="9"
                refY="3"
                orient="auto"
                markerUnits="strokeWidth"
              >
                <path d="M0,0 L0,6 L9,3 z" fill="#64748b" />
              </marker>
              <marker
                id="formula-graph-arrow-cycle"
                markerWidth="10"
                markerHeight="10"
                refX="9"
                refY="3"
                orient="auto"
                markerUnits="strokeWidth"
              >
                <path d="M0,0 L0,6 L9,3 z" fill="#dc2626" />
              </marker>
            </defs>

            <path
              v-for="edge in layoutEdges"
              :key="edge.id"
              :d="edge.path"
              class="svg-edge"
              :class="edgeClass(edge)"
              :marker-end="edge.cycle ? 'url(#formula-graph-arrow-cycle)' : 'url(#formula-graph-arrow)'"
            />
            <g
              v-for="edge in layoutEdges"
              :key="`${edge.id}-label`"
              class="svg-edge-label"
              :transform="`translate(${edge.labelX}, ${edge.labelY})`"
            >
              <rect x="-54" y="-12" width="108" height="24" rx="12" />
              <text text-anchor="middle" dominant-baseline="middle">{{ edgeLabel(edge) }}</text>
            </g>

            <foreignObject
              v-for="node in layoutNodes"
              :key="node.id"
              :x="node.x"
              :y="node.y"
              :width="nodeWidth"
              :height="nodeHeight"
            >
              <div
                xmlns="http://www.w3.org/1999/xhtml"
                class="svg-node"
                :class="nodeClass(node)"
              >
                <span>{{ nodeTypeLabel(node) }}</span>
                <strong>{{ node.label || node.id }}</strong>
                <em>{{ nodeSubtitle(node) }}</em>
              </div>
            </foreignObject>
          </svg>
        </div>

        <div class="edge-list">
          <strong>依赖关系</strong>
          <div
            v-for="edge in graphData.edges || []"
            :key="edge.id"
            class="graph-edge"
            :class="{ cycle: cycleSet.has(edge.source) && cycleSet.has(edge.target) }"
          >
            <code>{{ edge.source }}</code>
            <span>{{ edge.type }}</span>
            <code>{{ edge.target }}</code>
          </div>
        </div>
      </section>

      <div v-if="graphData.errors?.length" class="graph-errors">
        <span v-for="error in graphData.errors" :key="error">{{ error }}</span>
      </div>
    </div>
  </n-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { getDependencyGraph } from '@/api/formula'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  objectCode: {
    type: String,
    default: '',
  },
  currentFieldCode: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:show'])

const nodeWidth = 190
const nodeHeight = 78
const levelGap = 260
const rowGap = 118
const canvasPaddingX = 48
const canvasPaddingY = 44

const loading = ref(false)
const zoom = ref(1)
const graphData = ref({
  nodes: [],
  edges: [],
  errors: [],
  cyclePath: [],
})

const formulaFields = computed(() => (props.fields || []).filter(field => field?.formulaConfig?.type))
const formulaCount = computed(() => formulaFields.value.length)
const cycleSet = computed(() => new Set(graphData.value.cyclePath || []))
const layout = computed(() => buildLayout(graphData.value.nodes || [], graphData.value.edges || []))
const layoutNodes = computed(() => layout.value.nodes)
const layoutEdges = computed(() => layout.value.edges)
const graphWidth = computed(() => layout.value.width)
const graphHeight = computed(() => layout.value.height)

watch(
  () => props.show,
  (visible) => {
    if (visible)
      loadGraph()
  },
)

async function loadGraph() {
  loading.value = true
  try {
    const res = await getDependencyGraph({
      objectCode: props.objectCode,
      formulas: formulaFields.value.map(toFormulaFieldConfig),
    })
    graphData.value = res?.data ?? res ?? { nodes: [], edges: [], errors: [] }
  }
  catch (e) {
    graphData.value = {
      valid: false,
      nodes: [],
      edges: [],
      errors: [e?.message || '依赖图加载失败'],
      cyclePath: [],
    }
  }
  finally {
    loading.value = false
  }
}

function buildLayout(nodes, edges) {
  const levelMap = new Map()
  nodes.forEach((node) => {
    const type = String(node.type || '').toUpperCase()
    const depth = Number.isFinite(Number(node.depth)) ? Number(node.depth) : 0
    const level = type === 'FORMULA' ? depth + 1 : 0
    levelMap.set(node.id, Math.max(0, level))
  })

  let changed = true
  let guard = 0
  while (changed && guard < nodes.length + edges.length + 4) {
    changed = false
    guard += 1
    edges.forEach((edge) => {
      const sourceLevel = levelMap.get(edge.source) ?? 0
      const targetLevel = levelMap.get(edge.target) ?? 0
      if (targetLevel <= sourceLevel) {
        levelMap.set(edge.target, sourceLevel + 1)
        changed = true
      }
    })
  }

  const grouped = new Map()
  nodes.forEach((node) => {
    const level = levelMap.get(node.id) ?? 0
    if (!grouped.has(level))
      grouped.set(level, [])
    grouped.get(level).push(node)
  })

  const positioned = []
  Array.from(grouped.keys()).sort((a, b) => a - b).forEach((level) => {
    const rows = grouped.get(level)
    rows
      .sort((a, b) => String(a.label || a.id).localeCompare(String(b.label || b.id)))
      .forEach((node, index) => {
        positioned.push({
          ...node,
          x: canvasPaddingX + level * levelGap,
          y: canvasPaddingY + index * rowGap,
        })
      })
  })

  const positionMap = new Map(positioned.map(node => [node.id, node]))
  const layoutEdges = edges.map((edge) => {
    const source = positionMap.get(edge.source)
    const target = positionMap.get(edge.target)
    if (!source || !target)
      return null
    const startX = source.x + nodeWidth
    const startY = source.y + nodeHeight / 2
    const endX = target.x
    const endY = target.y + nodeHeight / 2
    const control = Math.max(64, Math.abs(endX - startX) * 0.48)
    const path = `M ${startX} ${startY} C ${startX + control} ${startY}, ${endX - control} ${endY}, ${endX} ${endY}`
    return {
      ...edge,
      path,
      labelX: (startX + endX) / 2,
      labelY: (startY + endY) / 2 - 10,
      cycle: cycleSet.value.has(edge.source) && cycleSet.value.has(edge.target),
    }
  }).filter(Boolean)

  const maxLevel = Math.max(0, ...Array.from(grouped.keys()))
  const maxRows = Math.max(1, ...Array.from(grouped.values()).map(items => items.length))
  return {
    nodes: positioned,
    edges: layoutEdges,
    width: canvasPaddingX * 2 + nodeWidth + maxLevel * levelGap,
    height: canvasPaddingY * 2 + nodeHeight + (maxRows - 1) * rowGap,
  }
}

function zoomGraph(delta) {
  zoom.value = Math.min(1.6, Math.max(0.7, Number((zoom.value + delta).toFixed(2))))
}

function resetGraphView() {
  zoom.value = 1
}

function nodeClass(node) {
  return [
    String(node.type || 'node').toLowerCase(),
    {
      active: node.fieldCode === props.currentFieldCode,
      cycle: cycleSet.value.has(node.id),
    },
  ]
}

function edgeClass(edge) {
  return [
    String(edge.type || '').toLowerCase(),
    { cycle: edge.cycle },
  ]
}

function nodeTypeLabel(node = {}) {
  const type = String(node.type || 'NODE').toUpperCase()
  return {
    FIELD: '字段',
    FORMULA: '公式字段',
    RELATION: '对象关系',
    OBJECT: '业务单元',
    FUNCTION: '函数',
  }[type] || type
}

function nodeSubtitle(node = {}) {
  if (node.formulaType)
    return node.formulaType
  const metadata = node.metadata || {}
  return metadata.targetObjectCode || metadata.targetField || node.objectCode || '-'
}

function edgeLabel(edge = {}) {
  return edge.label || edge.type || 'DEPENDS'
}

function toFormulaFieldConfig(field = {}) {
  const config = field.formulaConfig || {}
  return {
    fieldCode: field.fieldCode || field.field,
    type: config.type || 'CALC',
    mode: config.mode || 'STORED',
    expression: config.expression || '',
    dependsOn: config.dependsOn || [],
    aggregate: config.aggregate || null,
    condition: config.condition || null,
    lookup: config.lookup || null,
    crossObject: config.crossObject || null,
  }
}
</script>

<style scoped>
.formula-graph-modal {
  width: min(1040px, calc(100vw - 32px));
}

.formula-graph-panel {
  display: grid;
  gap: 14px;
}

.graph-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: var(--bg-secondary);
  padding: 10px 12px;
}

.graph-toolbar > div {
  min-width: 96px;
}

.graph-toolbar span,
.svg-node span,
.svg-node em {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
}

.graph-toolbar strong {
  display: block;
  color: var(--text-primary);
  font-size: 18px;
  line-height: 1.3;
}

.graph-zoom-tools {
  margin-left: auto;
}

.graph-canvas {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 14px;
  min-height: 420px;
}

.graph-svg-stage,
.edge-list {
  align-content: start;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 12px;
}

.graph-svg-stage {
  overflow: auto;
  min-height: 480px;
  background:
    linear-gradient(var(--bg-secondary) 19px, transparent 20px), linear-gradient(90deg, var(--bg-secondary) 19px, transparent 20px), var(--bg-primary);
  background-size: 20px 20px;
}

.dependency-svg {
  display: block;
  min-width: 100%;
  min-height: 100%;
}

.svg-edge {
  fill: none;
  stroke: #64748b;
  stroke-linecap: round;
  stroke-width: 2;
}

.svg-edge.depends_on {
  stroke: #2563eb;
}

.svg-edge.lookup,
.svg-edge.cross_object {
  stroke: #7c3aed;
}

.svg-edge.aggregate {
  stroke: #0891b2;
}

.svg-edge.cycle {
  stroke: #dc2626;
  stroke-width: 2.8;
}

.svg-edge-label rect {
  fill: #fff;
  stroke: #dbe3ee;
}

.svg-edge-label text {
  fill: var(--text-primary);
  font-size: 11px;
  font-weight: 700;
}

.svg-node {
  display: grid;
  gap: 3px;
  box-sizing: border-box;
  width: 190px;
  height: 78px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: var(--bg-secondary);
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.06);
  padding: 9px 10px;
}

.svg-node strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.svg-node em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.svg-node.formula {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.svg-node.relation {
  border-color: #ddd6fe;
  background: #f5f3ff;
}

.svg-node.field {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.svg-node.active {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
}

.svg-node.cycle,
.graph-edge.cycle {
  border-color: #fca5a5;
  background: #fef2f2;
}

.edge-list {
  display: grid;
  align-content: start;
  gap: 8px;
  max-height: 480px;
  overflow-y: auto;
}

.edge-list > strong {
  color: var(--text-primary);
  font-size: 13px;
}

.graph-edge {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary);
  padding: 8px;
}

.graph-edge code {
  overflow: hidden;
  color: var(--text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  text-overflow: ellipsis;
}

.graph-edge span {
  border-radius: 999px;
  background: #e8eef7;
  color: var(--text-primary);
  font-size: 11px;
  font-weight: 700;
  padding: 3px 7px;
}

.graph-errors {
  display: grid;
  gap: 4px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fef2f2;
  color: #991b1b;
  font-size: 12px;
  padding: 10px 12px;
}

@media (max-width: 860px) {
  .graph-toolbar,
  .graph-canvas {
    grid-template-columns: minmax(0, 1fr);
  }

  .graph-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .graph-zoom-tools {
    margin-left: 0;
  }
}
</style>
