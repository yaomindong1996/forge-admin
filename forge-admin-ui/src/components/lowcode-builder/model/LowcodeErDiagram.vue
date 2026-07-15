<template>
  <section class="er-diagram-shell">
    <div class="er-diagram-head">
      <div>
        <div class="section-title">
          {{ title }}
        </div>
        <p>{{ resolvedSubtitle }}</p>
      </div>
      <div class="head-actions">
        <span v-if="editable" class="edit-hint">
          从字段右侧连接点拖到目标字段，即可创建关系
        </span>
        <div class="legend-strip">
          <span><i class="legend-main" />配置关系</span>
          <span><i class="legend-inferred" />字段推断</span>
          <span><i class="legend-pk" />PK</span>
          <span><i class="legend-fk" />FK</span>
        </div>
        <n-button size="small" secondary :disabled="!diagramTables.length" @click="downloadSvg">
          <template #icon>
            <n-icon><DownloadOutline /></n-icon>
          </template>
          下载 SVG
        </n-button>
      </div>
    </div>

    <div v-if="diagramTables.length" class="er-canvas-wrap">
      <svg
        ref="svgRef"
        class="er-svg"
        :viewBox="`0 0 ${canvasWidth} ${canvasHeight}`"
        :width="canvasWidth"
        :height="canvasHeight"
        role="img"
        :aria-label="title"
        @mousemove="handlePointerMove"
        @mouseup="stopInteraction"
        @mouseleave="stopInteraction"
      >
        <defs>
          <marker
            :id="markerMainId"
            viewBox="0 0 10 10"
            refX="10"
            refY="5"
            markerWidth="8"
            markerHeight="8"
            orient="auto-start-reverse"
          >
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#2563eb" opacity="0.82" />
          </marker>
          <marker
            :id="markerInferredId"
            viewBox="0 0 10 10"
            refX="10"
            refY="5"
            markerWidth="8"
            markerHeight="8"
            orient="auto-start-reverse"
          >
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#16a34a" opacity="0.78" />
          </marker>
          <filter :id="shadowId" x="-8%" y="-8%" width="116%" height="116%">
            <feDropShadow dx="0" dy="4" stdDeviation="6" flood-color="#0f172a" flood-opacity="0.12" />
          </filter>
        </defs>

        <rect :width="canvasWidth" :height="canvasHeight" rx="10" fill="#f8fafc" />
        <path
          v-if="connectionPreviewPath"
          :d="connectionPreviewPath"
          class="connection-preview"
          fill="none"
          stroke="#165dff"
          stroke-width="2"
          stroke-dasharray="6 4"
          :marker-end="`url(#${markerMainId})`"
        />
        <g>
          <path
            v-for="relation in renderedRelations"
            :key="`${relation.key}_hit`"
            :d="relation.path"
            class="relation-hit-area"
            :class="{ selectable: relation.relationKey }"
            fill="none"
            stroke="transparent"
            stroke-width="14"
            @click.stop="selectRelation(relation)"
          />
          <path
            v-for="relation in renderedRelations"
            :key="relation.key"
            class="relation-line"
            :class="{ selected: isSelectedRelation(relation), selectable: relation.relationKey }"
            :d="relation.path"
            fill="none"
            :stroke="relation.color"
            :stroke-width="relation.inferred ? 1.5 : 1.9"
            :stroke-dasharray="relation.inferred ? '7 5' : ''"
            :opacity="relation.inferred ? 0.68 : 0.82"
            :marker-end="markerUrl(relation)"
            @click.stop="selectRelation(relation)"
          >
            <title>{{ relation.tooltip }}</title>
          </path>
          <g
            v-for="relation in renderedRelations"
            :key="`${relation.key}_label`"
            :class="{ 'relation-label-selectable': relation.relationKey }"
            @click.stop="selectRelation(relation)"
          >
            <rect
              :x="relation.labelX - relation.labelWidth / 2"
              :y="relation.labelY - 14"
              :width="relation.labelWidth"
              height="20"
              rx="6"
              fill="#ffffff"
              stroke="#d8dee8"
            />
            <text
              :x="relation.labelX"
              :y="relation.labelY"
              text-anchor="middle"
              fill="#0f172a"
              font-size="11"
              font-weight="700"
            >
              {{ relation.label }}
            </text>
          </g>
        </g>

        <g>
          <g
            v-for="table in diagramTables"
            :key="table.code"
            class="er-table-group"
            :transform="`translate(${table.x}, ${table.y})`"
            @mousedown="startDrag($event, table.code)"
          >
            <rect
              :width="TABLE_W"
              :height="table.height"
              rx="8"
              fill="#ffffff"
              :stroke="table.primary ? '#1e40af' : '#cbd5e1'"
              :stroke-width="table.primary ? 2 : 1.2"
              :filter="`url(#${shadowId})`"
            />
            <rect :width="TABLE_W" :height="HEADER_H" rx="8" :fill="table.color" />
            <rect :y="HEADER_H - 8" :width="TABLE_W" height="8" :fill="table.color" />
            <text
              :x="PADDING"
              y="20"
              fill="#ffffff"
              font-size="13"
              font-weight="700"
            >
              {{ truncate(table.name, 20) }}
            </text>
            <text
              :x="TABLE_W - PADDING"
              y="20"
              text-anchor="end"
              fill="#dbeafe"
              font-size="10"
              font-weight="700"
            >
              {{ table.primary ? 'MAIN' : 'MODEL' }}
            </text>
            <text
              :x="PADDING"
              y="34"
              fill="#dbeafe"
              font-size="10"
            >
              {{ truncate(table.tableName || table.code, 34) }}
            </text>

            <g
              v-for="field in table.fields"
              :key="`${table.code}_${field.field}`"
              class="er-field-row"
              :class="{ connecting: isConnectionSource(table.code, field.field) }"
              :transform="`translate(0, ${field.y})`"
              @mouseup="finishConnection($event, table.code, field.field)"
            >
              <rect x="0" y="-15" :width="TABLE_W" height="23" fill="transparent" />
              <rect
                v-if="field.badge"
                :x="PADDING"
                y="-12"
                width="32"
                height="16"
                rx="4"
                :fill="field.badgeColor"
                opacity="0.95"
              />
              <text
                v-if="field.badge"
                :x="PADDING + 16"
                y="0"
                text-anchor="middle"
                fill="#ffffff"
                font-size="9"
                font-weight="800"
              >
                {{ field.badge }}
              </text>
              <text
                :x="field.badge ? PADDING + 40 : PADDING"
                y="0"
                :fill="field.systemField ? '#64748b' : '#0f172a'"
                font-size="11"
                :font-weight="field.primaryKey || field.foreignKey ? 700 : 500"
              >
                {{ truncate(field.columnName || field.field, field.badge ? 24 : 30) }}
              </text>
              <text
                :x="TABLE_W - PADDING"
                y="0"
                text-anchor="end"
                fill="#64748b"
                font-size="10"
              >
                {{ truncate(field.dataType || '-', 12) }}
              </text>
              <circle
                v-if="editable"
                class="field-connector"
                :class="{ active: isConnectionSource(table.code, field.field) }"
                :cx="TABLE_W - 7"
                cy="-4"
                r="5"
                @mousedown.stop="startConnection($event, table.code, field.field)"
                @mouseup.stop="finishConnection($event, table.code, field.field)"
              >
                <title>拖动连接 {{ table.name }}.{{ field.columnName || field.field }}</title>
              </circle>
            </g>
          </g>
        </g>
      </svg>
    </div>

    <n-empty v-else :description="emptyText" />
  </section>
</template>

<script setup>
import { DownloadOutline } from '@vicons/ionicons5'
import { computed, ref, watch } from 'vue'

defineOptions({ name: 'LowcodeErDiagram' })

const props = defineProps({
  title: {
    type: String,
    default: 'ER 图',
  },
  subtitle: {
    type: String,
    default: '',
  },
  models: {
    type: Array,
    default: () => [],
  },
  primaryModelCode: {
    type: String,
    default: '',
  },
  downloadFileName: {
    type: String,
    default: '',
  },
  emptyText: {
    type: String,
    default: '暂无可绘制的数据模型',
  },
  editable: {
    type: Boolean,
    default: false,
  },
  selectedRelationKey: {
    type: [String, Number],
    default: '',
  },
})

const emit = defineEmits(['connect', 'relationSelect'])

const TABLE_W = 292
const HEADER_H = 42
const ROW_H = 24
const PADDING = 12
const TABLE_GAP_X = 96
const TABLE_GAP_Y = 70
const CANVAS_PADDING = 28
const palette = ['#1e40af', '#0f766e', '#7c3aed', '#be123c', '#c2410c', '#047857', '#4338ca', '#0369a1']
const systemFieldNames = new Set(['id', 'tenantId', 'createBy', 'createTime', 'createDept', 'updateBy', 'updateTime', 'delFlag'])
const systemColumnNames = new Set(['id', 'tenant_id', 'create_by', 'create_time', 'create_dept', 'update_by', 'update_time', 'del_flag'])

const uid = Math.random().toString(36).slice(2, 9)
const markerMainId = `er-arrow-main-${uid}`
const markerInferredId = `er-arrow-inferred-${uid}`
const shadowId = `er-card-shadow-${uid}`
const svgRef = ref(null)
const tablePositions = ref({})
const dragState = ref(null)
const connectionDraft = ref(null)

const baseModels = computed(() => normalizeModels(props.models))
const modelUniverse = computed(() => withRelationTargets(baseModels.value))
const defaultLayoutMap = computed(() => buildDefaultLayout(modelUniverse.value))
const rawRelations = computed(() => buildRelations(modelUniverse.value))
const relationFieldMap = computed(() => buildRelationFieldMap(rawRelations.value))
const diagramTables = computed(() => modelUniverse.value.map((model, index) => {
  const defaultPosition = defaultLayoutMap.value[model.code] || { x: CANVAS_PADDING, y: CANVAS_PADDING }
  const position = tablePositions.value[model.code] || defaultPosition
  const fields = model.fields.map((field, fieldIndex) => {
    const badge = resolveFieldBadge(model.code, field)
    return {
      ...field,
      y: HEADER_H + 22 + fieldIndex * ROW_H,
      badge,
      badgeColor: resolveBadgeColor(badge),
      foreignKey: badge === 'FK',
    }
  })
  return {
    ...model,
    x: position.x,
    y: position.y,
    color: model.primary ? '#1e40af' : palette[index % palette.length],
    fields,
    height: HEADER_H + fields.length * ROW_H + 14,
  }
}))
const tableMap = computed(() => new Map(diagramTables.value.map(table => [table.code, table])))
const renderedRelations = computed(() => rawRelations.value
  .map(relation => resolveRelationPath(relation, tableMap.value))
  .filter(Boolean))
const canvasWidth = computed(() => {
  const maxRight = diagramTables.value.reduce((max, table) => Math.max(max, table.x + TABLE_W + CANVAS_PADDING), 760)
  return Math.ceil(maxRight)
})
const canvasHeight = computed(() => {
  const maxBottom = diagramTables.value.reduce((max, table) => Math.max(max, table.y + table.height + CANVAS_PADDING), 420)
  return Math.ceil(maxBottom)
})
const resolvedSubtitle = computed(() => {
  if (props.subtitle)
    return props.subtitle
  return `${diagramTables.value.length} 个模型，${renderedRelations.value.length} 条关系`
})
const connectionPreviewPath = computed(() => {
  const draft = connectionDraft.value
  if (!draft)
    return ''
  const midX = (draft.startX + draft.currentX) / 2
  return `M ${draft.startX} ${draft.startY} C ${midX} ${draft.startY}, ${midX} ${draft.currentY}, ${draft.currentX} ${draft.currentY}`
})

watch(
  modelUniverse,
  (models) => {
    const defaults = buildDefaultLayout(models)
    const next = {}
    models.forEach((model) => {
      next[model.code] = tablePositions.value[model.code] || defaults[model.code]
    })
    tablePositions.value = next
  },
  { immediate: true },
)

function normalizeModels(models = []) {
  const map = new Map()
  ;(models || []).forEach((item, index) => {
    const model = normalizeModel(item, index)
    if (model && !map.has(model.code))
      map.set(model.code, model)
  })
  const primaryCode = props.primaryModelCode || ''
  return Array.from(map.values()).sort((left, right) => {
    if (left.code === primaryCode)
      return -1
    if (right.code === primaryCode)
      return 1
    return left.name.localeCompare(right.name, 'zh-Hans-CN')
  })
}

function normalizeModel(item = {}, index = 0) {
  const schema = item.modelSchema || item
  const code = String(item.modelCode || schema.object?.code || schema.tableName || `model_${index + 1}`).trim()
  if (!code)
    return null
  const name = item.modelName || schema.object?.name || schema.businessName || code
  const fields = normalizeFields(schema.fields || [])
  return {
    id: item.id || schema.id || null,
    code,
    name,
    tableName: schema.tableName || item.tableName || code,
    primary: Boolean(props.primaryModelCode && props.primaryModelCode === code),
    schema,
    fields,
    relations: Array.isArray(schema.relations) ? schema.relations : [],
  }
}

function normalizeFields(fields = []) {
  const rows = (fields || []).map(field => ({
    field: field.field || field.columnName || 'field',
    columnName: field.columnName || field.field || 'field',
    label: field.label || field.field || field.columnName || '字段',
    dataType: field.dataType || field.type || '',
    primaryKey: Boolean(field.primaryKey) || field.field === 'id' || field.columnName === 'id',
    systemField: Boolean(field.systemField) || systemFieldNames.has(field.field) || systemColumnNames.has(field.columnName),
  }))
  if (!rows.some(field => field.field === 'id' || field.columnName === 'id')) {
    rows.unshift({
      field: 'id',
      columnName: 'id',
      label: 'ID',
      dataType: 'bigint',
      primaryKey: true,
      systemField: true,
    })
  }
  return rows
}

function withRelationTargets(models) {
  const map = new Map(models.map(model => [model.code, model]))
  models.forEach((model) => {
    model.relations.forEach((relation) => {
      const targetCode = relation?.targetObjectCode
      if (!targetCode || map.has(targetCode))
        return
      map.set(targetCode, {
        id: null,
        code: targetCode,
        name: targetCode,
        tableName: '',
        primary: false,
        schema: {},
        fields: normalizeFields([{ field: 'id', columnName: 'id', dataType: 'bigint', primaryKey: true, systemField: true }]),
        relations: [],
      })
    })
  })
  return Array.from(map.values())
}

function buildDefaultLayout(models) {
  const layout = {}
  if (!models.length)
    return layout
  const cols = models.length <= 2 ? models.length : models.length <= 8 ? 3 : 4
  let cursorY = CANVAS_PADDING
  for (let rowStart = 0; rowStart < models.length; rowStart += cols) {
    const rowModels = models.slice(rowStart, rowStart + cols)
    const rowHeights = rowModels.map(model => HEADER_H + normalizeFields(model.fields).length * ROW_H + 14)
    const rowHeight = Math.max(...rowHeights)
    rowModels.forEach((model, columnIndex) => {
      layout[model.code] = {
        x: CANVAS_PADDING + columnIndex * (TABLE_W + TABLE_GAP_X),
        y: cursorY,
      }
    })
    cursorY += rowHeight + TABLE_GAP_Y
  }
  return layout
}

function buildRelations(models) {
  const modelMap = new Map(models.map(model => [model.code, model]))
  const relations = []
  const seen = new Set()
  models.forEach((model) => {
    model.relations.forEach((relation, index) => {
      const normalized = normalizeExplicitRelation(model, relation, index, modelMap)
      if (!normalized)
        return
      seen.add(relationIdentity(normalized))
      relations.push(normalized)
    })
  })
  buildInferredRelations(models).forEach((relation) => {
    const key = relationIdentity(relation)
    if (seen.has(key))
      return
    seen.add(key)
    relations.push(relation)
  })
  return relations
}

function normalizeExplicitRelation(sourceModel, relation = {}, index, modelMap) {
  const targetCode = relation.targetObjectCode
  if (!targetCode || !modelMap.has(targetCode))
    return null
  const type = relation.relationType || 'REFERENCE'
  const sourceField = relation.sourceField || ''
  const targetField = relation.targetField || 'id'
  if (!sourceField && type !== 'ONE_TO_MANY')
    return null
  const labelMap = {
    REFERENCE: 'N:1',
    ONE_TO_MANY: '1:N',
    ONE_TO_ONE: '1:1',
  }
  const label = labelMap[type] || type
  if (type === 'ONE_TO_MANY') {
    return {
      key: `explicit_${sourceModel.code}_${targetCode}_${sourceField}_${targetField}_${index}`,
      from: sourceModel.code,
      to: targetCode,
      fromField: sourceField || 'id',
      toField: targetField || 'id',
      label,
      color: '#2563eb',
      inferred: false,
      relationKey: relation.relationKey || relation.clientKey || relation.id || '',
      tooltip: `${sourceModel.name}.${sourceField || 'id'} -> ${targetCode}.${targetField || 'id'} (${type})`,
    }
  }
  return {
    key: `explicit_${targetCode}_${sourceModel.code}_${targetField}_${sourceField}_${index}`,
    from: targetCode,
    to: sourceModel.code,
    fromField: targetField || 'id',
    toField: sourceField,
    label,
    color: '#2563eb',
    inferred: false,
    relationKey: relation.relationKey || relation.clientKey || relation.id || '',
    tooltip: `${targetCode}.${targetField || 'id'} -> ${sourceModel.name}.${sourceField} (${type})`,
  }
}

function buildInferredRelations(models) {
  const result = []
  models.forEach((sourceModel) => {
    sourceModel.fields.forEach((field) => {
      if (field.primaryKey || field.systemField)
        return
      const targetModel = models.find(target => target.code !== sourceModel.code && fieldLooksLikeTarget(field, target))
      if (!targetModel)
        return
      result.push({
        key: `inferred_${targetModel.code}_${sourceModel.code}_${field.field}`,
        from: targetModel.code,
        to: sourceModel.code,
        fromField: 'id',
        toField: field.field,
        label: '1:N',
        color: '#16a34a',
        inferred: true,
        tooltip: `${targetModel.name}.id -> ${sourceModel.name}.${field.columnName || field.field} (字段名推断)`,
      })
    })
  })
  return result
}

function fieldLooksLikeTarget(field, targetModel) {
  const fieldNames = new Set([
    snakeCase(field.field),
    snakeCase(field.columnName),
  ].filter(Boolean))
  return targetEntityNames(targetModel).some(name =>
    fieldNames.has(`${name}_id`) || fieldNames.has(`${name}_code`) || fieldNames.has(`${name}_no`),
  )
}

function targetEntityNames(model) {
  const names = new Set([snakeCase(model.code), snakeCase(model.tableName)].filter(Boolean))
  Array.from(names).forEach((name) => {
    names.add(name.replace(/^biz_/, ''))
    names.add(name.replace(/^sys_/, ''))
    names.add(name.replace(/^tf_[a-z]_/, ''))
    const parts = name.split('_').filter(Boolean)
    if (parts.length > 1)
      names.add(parts[parts.length - 1])
  })
  return Array.from(names).filter(Boolean)
}

function buildRelationFieldMap(relations) {
  const map = new Map()
  relations.forEach((relation) => {
    addRelationField(map, relation.from, relation.fromField)
    addRelationField(map, relation.to, relation.toField)
  })
  return map
}

function addRelationField(map, modelCode, field) {
  if (!modelCode || !field)
    return
  if (!map.has(modelCode))
    map.set(modelCode, new Set())
  map.get(modelCode).add(field)
}

function resolveFieldBadge(modelCode, field) {
  if (field.primaryKey)
    return 'PK'
  const relationFields = relationFieldMap.value.get(modelCode)
  if (relationFields?.has(field.field) || relationFields?.has(field.columnName))
    return 'FK'
  if (field.systemField)
    return 'SYS'
  return ''
}

function resolveBadgeColor(badge) {
  const map = {
    PK: '#f59e0b',
    FK: '#2563eb',
    SYS: '#64748b',
  }
  return map[badge] || '#94a3b8'
}

function resolveRelationPath(relation, tables) {
  const fromTable = tables.get(relation.from)
  const toTable = tables.get(relation.to)
  if (!fromTable || !toTable)
    return null
  const fromY = fieldY(fromTable, relation.fromField)
  const toY = fieldY(toTable, relation.toField)
  const fromCenterX = fromTable.x + TABLE_W / 2
  const toCenterX = toTable.x + TABLE_W / 2
  let fromX = fromTable.x + TABLE_W
  let toX = toTable.x
  if (fromCenterX > toCenterX) {
    fromX = fromTable.x
    toX = toTable.x + TABLE_W
  }
  const midX = (fromX + toX) / 2
  const labelX = (fromX + toX) / 2
  const labelY = (fromY + toY) / 2 - 8
  return {
    ...relation,
    path: `M ${fromX} ${fromY} C ${midX} ${fromY}, ${midX} ${toY}, ${toX} ${toY}`,
    labelX,
    labelY,
    labelWidth: Math.max(42, String(relation.label || '').length * 8 + 22),
  }
}

function fieldY(table, fieldName) {
  const index = table.fields.findIndex(field => field.field === fieldName || field.columnName === fieldName)
  const fieldIndex = index >= 0 ? index : 0
  return table.y + HEADER_H + 22 + fieldIndex * ROW_H
}

function relationIdentity(relation) {
  return `${relation.from}:${relation.fromField}:${relation.to}:${relation.toField}`
}

function markerUrl(relation) {
  return `url(#${relation.inferred ? markerInferredId : markerMainId})`
}

function startDrag(event, code) {
  if (event.button !== 0)
    return
  const point = toSvgPoint(event)
  const position = tablePositions.value[code] || defaultLayoutMap.value[code]
  dragState.value = {
    code,
    offsetX: point.x - position.x,
    offsetY: point.y - position.y,
  }
  event.preventDefault()
}

function handlePointerMove(event) {
  const point = toSvgPoint(event)
  if (dragState.value) {
    tablePositions.value = {
      ...tablePositions.value,
      [dragState.value.code]: {
        x: Math.max(12, point.x - dragState.value.offsetX),
        y: Math.max(12, point.y - dragState.value.offsetY),
      },
    }
  }
  if (connectionDraft.value) {
    connectionDraft.value = {
      ...connectionDraft.value,
      currentX: point.x,
      currentY: point.y,
    }
  }
}

function stopDrag() {
  dragState.value = null
}

function stopInteraction() {
  stopDrag()
  connectionDraft.value = null
}

function startConnection(event, modelCode, field) {
  if (!props.editable || event.button !== 0)
    return
  const point = resolveFieldConnectionPoint(modelCode, field) || toSvgPoint(event)
  connectionDraft.value = {
    sourceModelCode: modelCode,
    sourceField: field,
    startX: point.x,
    startY: point.y,
    currentX: point.x,
    currentY: point.y,
  }
  dragState.value = null
  event.preventDefault()
}

function finishConnection(event, targetModelCode, targetField) {
  const draft = connectionDraft.value
  if (!draft)
    return
  connectionDraft.value = null
  if (draft.sourceModelCode === targetModelCode && draft.sourceField === targetField)
    return
  emit('connect', {
    sourceModelCode: draft.sourceModelCode,
    sourceField: draft.sourceField,
    targetModelCode,
    targetField,
  })
  event.preventDefault()
}

function resolveFieldConnectionPoint(modelCode, fieldName) {
  const table = tableMap.value.get(modelCode)
  if (!table)
    return null
  const field = table.fields.find(item => item.field === fieldName || item.columnName === fieldName)
  if (!field)
    return null
  return {
    x: table.x + TABLE_W - 7,
    y: table.y + field.y - 4,
  }
}

function isConnectionSource(modelCode, field) {
  return connectionDraft.value?.sourceModelCode === modelCode
    && connectionDraft.value?.sourceField === field
}

function isSelectedRelation(relation) {
  return Boolean(relation.relationKey)
    && String(relation.relationKey) === String(props.selectedRelationKey || '')
}

function selectRelation(relation) {
  if (!relation.relationKey)
    return
  emit('relationSelect', relation.relationKey)
}

function toSvgPoint(event) {
  const svg = svgRef.value
  if (!svg)
    return { x: 0, y: 0 }
  const point = svg.createSVGPoint()
  point.x = event.clientX
  point.y = event.clientY
  return point.matrixTransform(svg.getScreenCTM().inverse())
}

function downloadSvg() {
  if (!svgRef.value)
    return
  const clone = svgRef.value.cloneNode(true)
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  clone.setAttribute('width', String(canvasWidth.value))
  clone.setAttribute('height', String(canvasHeight.value))
  const source = new XMLSerializer().serializeToString(clone)
  const blob = new Blob([source], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = resolveDownloadName()
  link.click()
  URL.revokeObjectURL(url)
}

function resolveDownloadName() {
  const name = props.downloadFileName || `${props.title || 'er-diagram'}.svg`
  const safeName = String(name).replace(/[\\/:*?"<>|\r\n]/g, '_')
  return safeName.toLowerCase().endsWith('.svg') ? safeName : `${safeName}.svg`
}

function snakeCase(value) {
  return String(value || '')
    .trim()
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W/g, '_')
    .replace(/_+/g, '_')
    .toLowerCase()
    .replace(/^_+|_+$/g, '')
}

function truncate(value, maxLength) {
  const text = String(value || '')
  return text.length > maxLength ? `${text.slice(0, maxLength - 1)}…` : text
}
</script>

<style scoped>
.er-diagram-shell {
  display: grid;
  gap: 12px;
  min-width: 0;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.er-diagram-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.er-diagram-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.edit-hint {
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 5px;
  color: var(--text-secondary, #4e5969);
  background: var(--bg-secondary, #f7f8fa);
  font-size: 11px;
  line-height: 24px;
  padding: 0 8px;
  white-space: nowrap;
}

.legend-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #475569;
  font-size: 12px;
  white-space: nowrap;
}

.legend-strip span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.legend-strip i {
  display: inline-block;
  width: 18px;
  height: 3px;
  border-radius: 999px;
}

.legend-main {
  background: #2563eb;
}

.legend-inferred {
  background: repeating-linear-gradient(90deg, #16a34a 0 7px, transparent 7px 11px);
}

.legend-pk {
  background: #f59e0b;
}

.legend-fk {
  background: #2563eb;
}

.er-canvas-wrap {
  overflow: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.er-svg {
  display: block;
  min-width: 760px;
  cursor: default;
}

.er-table-group {
  cursor: move;
}

.er-table-group:hover > rect:first-child {
  stroke: #60a5fa;
}

.relation-hit-area.selectable,
.relation-line.selectable,
.relation-label-selectable {
  cursor: pointer;
}

.relation-line.selected {
  stroke: var(--primary-color, #165dff) !important;
  stroke-width: 3 !important;
  opacity: 1 !important;
}

.connection-preview {
  pointer-events: none;
}

.er-field-row.connecting > rect:first-child {
  fill: rgb(22 93 255 / 7%);
}

.field-connector {
  cursor: crosshair;
  fill: #fff;
  stroke: var(--primary-color, #165dff);
  stroke-width: 1.8;
  transition: r 0.15s ease, fill 0.15s ease;
}

.field-connector:hover,
.field-connector.active {
  r: 7px;
  fill: var(--primary-color, #165dff);
}

@media (max-width: 900px) {
  .er-diagram-head {
    display: grid;
  }

  .head-actions {
    justify-content: flex-start;
  }

  .legend-strip {
    flex-wrap: wrap;
    white-space: normal;
  }
}
</style>
