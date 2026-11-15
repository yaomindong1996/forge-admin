<script setup>
import { NAlert, NButton } from 'naive-ui'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  BUSINESS_PROCESS_NODE_DRAG_MIME,
  getBusinessProcessNodeDefinition,
  isBusinessProcessStartType,
} from './business-process-node-types.js'
import {
  businessProcessHashInput,
  validateBusinessProcessGraph,
} from './business-process-schema.js'
import BusinessProcessCanvas from './BusinessProcessCanvas.vue'
import BusinessProcessNodeConfigDrawer from './BusinessProcessNodeConfigDrawer.vue'
import { useBusinessProcessDesigner } from './useBusinessProcessDesigner.js'

const props = defineProps({
  schema: { type: Object, required: true },
  processName: { type: String, default: '业务流程' },
  readonly: { type: Boolean, default: false },
  autoSaveDelay: { type: Number, default: 900 },
  saveState: { type: String, default: 'idle' },
  saveError: { type: String, default: '' },
  serverValidation: { type: Object, default: null },
  objectId: { type: String, default: '' },
  objectName: { type: String, default: '' },
  fields: { type: Array, default: () => [] },
  objects: { type: Array, default: () => [] },
  flowModels: { type: Array, default: () => [] },
  formAssets: { type: Array, default: () => [] },
  businessActions: { type: Array, default: () => [] },
  messageTemplates: { type: Array, default: () => [] },
  capabilities: { type: Array, default: () => [] },
  subProcesses: { type: Array, default: () => [] },
  serviceActors: { type: Array, default: () => [] },
})

const emit = defineEmits([
  'update:schema',
  'save',
  'validate',
  'openFlowDesigner',
  'refreshFlowModel',
  'refreshFields',
  'editAction',
  'dirtyChange',
  'locateIssue',
  'reload',
])

const designer = useBusinessProcessDesigner(props.schema)
const drawerVisible = ref(false)
const operationError = ref('')
const issuesExpanded = ref(true)
const draggingNodeType = ref('')
let autoSaveTimer = null

const palette = ['CONDITION', 'ACTION', 'APPROVAL', 'SUB_PROCESS']
  .map(type => ({ type, ...getBusinessProcessNodeDefinition(type) }))

const selectedNode = computed(() => designer.getNode(designer.selectedNodeId.value))
const clientValidation = computed(() => validateBusinessProcessGraph(designer.schema.value))
const issues = computed(() => mergeIssues(
  clientValidation.value.issues,
  props.serverValidation?.issues || props.serverValidation?.errors || [],
))

const saveStatus = computed(() => {
  const states = {
    idle: { label: designer.isDirty.value ? '有未保存修改' : '草稿已同步', tone: 'idle' },
    saving: { label: '正在保存…', tone: 'saving' },
    saved: { label: '刚刚已保存', tone: 'saved' },
    conflict: { label: '草稿发生冲突', tone: 'conflict' },
    error: { label: '保存失败', tone: 'error' },
  }
  return states[props.saveState] || states.idle
})

const canEditSelected = computed(() => selectedNode.value && !props.readonly)
const canCopySelected = computed(() => canEditSelected.value
  && !isBusinessProcessStartType(selectedNode.value.type)
  && selectedNode.value.type !== 'END'
  && designer.getOutgoingEdges(selectedNode.value.id).length === 1)
const canDeleteSelected = computed(() => canEditSelected.value
  && !isBusinessProcessStartType(selectedNode.value.type)
  && selectedNode.value.type !== 'END')

const initialStart = designer.schema.value.nodes.find(node => isBusinessProcessStartType(node.type))
designer.selectNode(initialStart?.id)

watch(() => designer.schema.value, () => {
  emit('update:schema', designer.exportSchema())
  if (designer.isDirty.value && !props.readonly)
    scheduleAutoSave()
}, { deep: true })

watch(() => designer.isDirty.value, (value) => {
  emit('dirtyChange', value)
})

watch(() => props.schema, (value) => {
  if (businessProcessHashInput(value) === businessProcessHashInput(designer.schema.value))
    return
  designer.setSchema(value, { markSaved: true })
  designer.clearHistory()
}, { deep: true })

// 旧流程草稿可能只保存了流程模型，没有保存应用页面级表单引用。
// 表单资产目录加载后，为已有审批节点自动补齐当前应用页面资产，避免用户再手工找 formKey。
watch(() => props.formAssets, () => {
  bindDefaultApprovalForms()
}, { deep: true })

watch(() => props.saveState, (value, previous) => {
  if (value === 'saved' && previous !== 'saved') {
    designer.markSaved()
    clearAutoSave()
  }
})

function handleAddNode(type) {
  operationError.value = ''
  const insertionNode = selectedNode.value || initialStart
  try {
    const outgoing = designer.getOutgoingEdges(insertionNode.id)
    if (outgoing.length !== 1)
      throw new Error('当前节点有多个结果出口，请使用对应分支线上的 + 添加节点')
    handleInsertNode({ edgeId: outgoing[0].id, type })
  }
  catch (error) {
    operationError.value = error.message
  }
}

function handleInsertNode({ edgeId, type }) {
  operationError.value = ''
  try {
    const nodeId = designer.insertNodeOnEdge(edgeId, type, buildInsertOverrides(type))
    designer.selectNode(nodeId)
    drawerVisible.value = true
    draggingNodeType.value = ''
  }
  catch (error) {
    operationError.value = error.message
    draggingNodeType.value = ''
  }
}

function buildInsertOverrides(type) {
  const objectCode = designer.schema.value.subject?.objectCode
  if (type === 'ACTION')
    return { config: { objectCode } }
  if (type !== 'APPROVAL')
    return {}
  const source = (props.formAssets || []).find(item => item?.formKey)
  if (!source)
    return {}
  const formKey = String(source.formKey || '')
  if (!formKey)
    return {}
  return {
    config: {
      versionPolicy: 'PINNED_AT_APPLICATION_PUBLISH',
      formAsset: {
        formKey,
        formName: source.formName || source.name || source.label || formKey,
        formMode: source.formMode || source.type || 'BUSINESS_OBJECT_FORM',
        providerKey: source.providerKey || undefined,
        applicationId: source.applicationId || undefined,
        pageId: source.pageId || undefined,
        pageCode: source.pageCode || undefined,
        pageName: source.pageName || undefined,
        pageType: source.pageType || undefined,
        sourceFormKey: source.sourceFormKey || undefined,
      },
    },
  }
}

function handlePaletteDragStart(event, type) {
  if (props.readonly) {
    event.preventDefault()
    return
  }
  draggingNodeType.value = type
  if (!event.dataTransfer)
    return
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData(BUSINESS_PROCESS_NODE_DRAG_MIME, type)
  event.dataTransfer.setData('text/plain', type)
}

function handlePaletteDragEnd() {
  draggingNodeType.value = ''
}

function handleNodeSelect(node) {
  designer.selectNode(node.id)
  bindDefaultApprovalForm(node.id)
  drawerVisible.value = true
}

function bindDefaultApprovalForm(nodeId) {
  const node = designer.getNode(nodeId)
  if (node?.type !== 'APPROVAL' || node.config?.formAsset?.formKey)
    return
  const formAsset = buildInsertOverrides('APPROVAL').config?.formAsset
  if (!formAsset?.formKey)
    return
  designer.updateNode(nodeId, {
    config: {
      ...(node.config || {}),
      formAsset,
    },
  })
}

function bindDefaultApprovalForms() {
  if (props.readonly || !Array.isArray(props.formAssets) || !props.formAssets.length)
    return
  const source = props.formAssets.find(item => item?.formKey)
  if (!source)
    return
  // 打开设计器时目录回填默认 formKey 不应算作用户改动，否则空开空关也会自动保存并刷新更新时间。
  const wasDirty = designer.isDirty.value
  let repairedLegacy = false
  designer.schema.value.nodes
    .filter(node => node?.type === 'APPROVAL')
    .forEach((node) => {
      const current = node.config?.formAsset || {}
      const currentKey = String(current.formKey || '')
      if (currentKey && resolveAvailableFormAsset(props.formAssets, current))
        return
      // 历史草稿可能保存 sourceFormKey/formAssetId 或旧页面 formKey；
      // 目录返回稳定 app_* formKey 后，按唯一匹配结果修复引用并保留节点其它配置。
      const repaired = currentKey
        ? resolveLegacyFormAsset(props.formAssets, current)
        : null
      const formAsset = toFormAssetRef(repaired || source)
      if (currentKey && !repaired)
        return
      if (currentKey && repaired)
        repairedLegacy = true
      designer.updateNode(node.id, {
        config: {
          ...(node.config || {}),
          formAsset,
        },
      })
    })
  if (!wasDirty && !repairedLegacy && designer.isDirty.value) {
    designer.markSaved()
    clearAutoSave()
  }
}

function resolveAvailableFormAsset(assets, current = {}) {
  const key = stringValue(current.formKey)
  if (!key)
    return null
  return (assets || []).find(asset => stringValue(asset?.formKey) === key) || null
}

function resolveLegacyFormAsset(assets, current = {}) {
  const key = stringValue(current.formKey)
  const pageId = stringValue(current.pageId)
  const candidates = (assets || []).filter((asset) => {
    const assetKey = stringValue(asset?.formKey)
    if (!assetKey || assetKey === key)
      return false
    const sourceKey = stringValue(asset?.sourceFormKey)
    const sourceAssetId = stringValue(asset?.sourceAssetId)
    return sourceKey === key
      || sourceAssetId === key
      || assetKey.endsWith(`_form_${key}`)
      || (pageId && stringValue(asset?.pageId) === pageId)
  })
  return candidates.length === 1 ? candidates[0] : null
}

function toFormAssetRef(item) {
  return {
    formKey: String(item.formKey || ''),
    formName: item.formName || item.name || item.label || item.formKey,
    formMode: item.formMode || item.type || 'BUSINESS_OBJECT_FORM',
    providerKey: item.providerKey || undefined,
    applicationId: item.applicationId || undefined,
    pageId: item.pageId || undefined,
    pageCode: item.pageCode || undefined,
    pageName: item.pageName || undefined,
    pageType: item.pageType || undefined,
    sourceFormKey: item.sourceFormKey || undefined,
  }
}

function stringValue(value) {
  return value == null ? '' : String(value)
}

function handleCopyNode() {
  if (!canCopySelected.value)
    return
  operationError.value = ''
  try {
    const nodeId = designer.copyNode(selectedNode.value.id)
    designer.selectNode(nodeId)
  }
  catch (error) {
    operationError.value = error.message
  }
}

function handleDeleteNode(node = selectedNode.value) {
  if (!node || props.readonly || isBusinessProcessStartType(node.type) || node.type === 'END')
    return
  designer.selectNode(node.id)
  const dialog = window.$dialog
  if (!dialog) {
    performDeleteNode(node)
    return
  }
  dialog.warning({
    title: '删除节点',
    content: `确认删除“${node.name || '当前节点'}”吗？删除后会自动恢复前后节点连线。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: () => performDeleteNode(node),
  })
}

function performDeleteNode(node) {
  operationError.value = ''
  try {
    designer.deleteNode(node.id)
    drawerVisible.value = false
  }
  catch (error) {
    operationError.value = error.message
  }
}

function handleDrawerSave(node, metadata = {}) {
  const current = designer.getNode(node.id)
  if (!current)
    return
  operationError.value = ''
  try {
    if (isBusinessProcessStartType(current.type) && node.type !== current.type) {
      designer.changeStartType(node.id, node.type, {
        name: node.name,
        config: node.config,
        recordIdSource: metadata.recordIdSource,
      })
      return
    }
    designer.updateNode(node.id, {
      name: node.name,
      ports: node.ports,
      config: node.config,
    }, { replaceConfig: true })
  }
  catch (error) {
    metadata.reject?.()
    operationError.value = error.message
    drawerVisible.value = true
  }
}

function handleValidate() {
  emit('validate', designer.exportSchema(), clientValidation.value)
}

function handleSave(reason = 'manual') {
  if (props.readonly || props.saveState === 'saving')
    return
  clearAutoSave()
  emit('save', designer.exportSchema(), {
    reason,
    hashInput: businessProcessHashInput(designer.schema.value),
    clientValidation: clientValidation.value,
  })
}

function scheduleAutoSave() {
  clearAutoSave()
  if (props.saveState === 'conflict')
    return
  const delay = Math.max(Number(props.autoSaveDelay) || 0, 0)
  autoSaveTimer = window.setTimeout(() => handleSave('auto'), delay)
}

function clearAutoSave() {
  if (autoSaveTimer != null) {
    window.clearTimeout(autoSaveTimer)
    autoSaveTimer = null
  }
}

function locateIssue(item) {
  if (item.nodeId)
    designer.selectNode(item.nodeId)
  emit('locateIssue', item)
}

function issueLocation(item) {
  const node = item?.nodeId ? designer.getNode(item.nodeId) : null
  return node ? `节点：${node.name || '未命名节点'}` : '流程结构'
}

function issuePath(item) {
  return item?.fieldPath || item?.path || ''
}

function issueSuggestion(item) {
  return item?.suggestion || item?.action || ''
}

function issueCopyText(item) {
  return [
    item?.message,
    issueLocation(item),
    item?.code ? `错误编码：${item.code}` : '',
    issuePath(item) ? `配置位置：${issuePath(item)}` : '',
    issueSuggestion(item) ? `处理建议：${issueSuggestion(item)}` : '',
  ].filter(Boolean).join('\n')
}

async function copyIssue(item) {
  const content = issueCopyText(item)
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(content)
    }
    else {
      const textarea = document.createElement('textarea')
      textarea.value = content
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      textarea.remove()
    }
    window.$message?.success('流程问题已复制')
  }
  catch {
    window.$message?.error('复制失败，请选中文本后复制')
  }
}

function handleBeforeUnload(event) {
  if (!designer.isDirty.value)
    return
  event.preventDefault()
  event.returnValue = ''
}

function mergeIssues(...lists) {
  const items = lists.flat()
  const nodeIssueCodes = new Set(items
    .filter(item => item?.nodeId)
    .map(item => item.code)
    .filter(Boolean))
  const result = []
  const keys = new Set()
  for (const item of items) {
    if (!item?.nodeId && issuePath(item).startsWith('dependencies.') && nodeIssueCodes.has(item.code))
      continue
    const key = `${item.code || ''}\u0000${item.nodeId || ''}\u0000${issuePath(item)}`
    if (keys.has(key))
      continue
    keys.add(key)
    result.push(item)
  }
  return result
}

onMounted(() => window.addEventListener('beforeunload', handleBeforeUnload))
onBeforeUnmount(() => {
  clearAutoSave()
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

defineExpose({
  designer,
  markSaved: designer.markSaved,
  exportSchema: designer.exportSchema,
  hasUnsavedChanges: designer.isDirty,
})
</script>

<template>
  <section class="business-process-designer h-full min-h-150 flex flex-col overflow-hidden">
    <header class="designer-toolbar">
      <div class="designer-title-block">
        <span>业务流程</span>
        <strong>{{ processName }}</strong>
        <em>{{ schema.processCode }}</em>
      </div>
      <div class="toolbar-actions">
        <span class="save-state" :class="`is-${saveStatus.tone}`">
          <span class="save-state-dot" />
          {{ saveStatus.label }}
        </span>
        <span class="toolbar-divider" />
        <NButton size="small" quaternary :disabled="!designer.canUndo.value || readonly" @click="designer.undo()">
          撤销
        </NButton>
        <NButton size="small" quaternary :disabled="!designer.canRedo.value || readonly" @click="designer.redo()">
          重做
        </NButton>
        <span class="toolbar-divider" />
        <NButton size="small" quaternary :disabled="!canCopySelected" @click="handleCopyNode">
          复制
        </NButton>
        <NButton size="small" quaternary type="error" :disabled="!canDeleteSelected" @click="handleDeleteNode()">
          删除
        </NButton>
        <span class="toolbar-divider" />
        <NButton data-designer-action="validate" size="small" secondary @click="handleValidate">
          检查流程
        </NButton>
        <NButton
          data-designer-action="save"
          size="small"
          type="primary"
          :loading="saveState === 'saving'"
          :disabled="readonly"
          @click="handleSave('manual')"
        >
          保存草稿
        </NButton>
      </div>
    </header>

    <NAlert v-if="saveState === 'conflict'" type="error" :bordered="false" class="conflict-alert">
      <div class="conflict-content">
        <span>草稿已被其他人更新。请刷新到最新版本后再继续编辑，当前修改不会自动覆盖远端草稿。</span>
        <NButton data-designer-action="reload" size="tiny" type="error" @click="emit('reload')">
          刷新草稿
        </NButton>
      </div>
    </NAlert>
    <NAlert v-else-if="saveState === 'error'" type="error" :bordered="false" class="conflict-alert">
      {{ saveError || '草稿保存失败，请检查网络后重试。' }}
    </NAlert>
    <NAlert v-if="operationError" type="warning" :bordered="false" closable @close="operationError = ''">
      {{ operationError }}
    </NAlert>

    <div class="designer-stage min-h-0 flex flex-col flex-1">
      <div class="designer-body min-h-0 flex flex-1">
        <aside class="node-palette">
          <div class="pane-heading">
            <strong>节点库</strong>
            <span>拖拽或单击添加到画布</span>
          </div>
          <button
            v-for="item in palette"
            :key="item.type"
            type="button"
            class="palette-item"
            :data-node-type="item.type"
            :disabled="readonly"
            :draggable="!readonly"
            @dragstart="handlePaletteDragStart($event, item.type)"
            @dragend="handlePaletteDragEnd"
            @click="handleAddNode(item.type)"
          >
            <span class="palette-icon" :class="`tone-${item.tone}`">
              <svg v-if="item.type === 'CONDITION'" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L22 12L12 22L2 12Z" /></svg>
              <svg v-else-if="item.type === 'ACTION'" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" /></svg>
              <svg v-else-if="item.type === 'APPROVAL'" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /><path d="M9 12l2 2 4-4" /></svg>
              <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" /><rect x="7" y="7" width="10" height="10" rx="1" /></svg>
            </span>
            <span class="palette-text">
              <strong>{{ item.label }}</strong>
              <small>{{ item.type === 'APPROVAL' ? '等待审批结果' : item.type === 'CONDITION' ? '分支条件判断' : item.type === 'SUB_PROCESS' ? '调用子流程' : '执行业务操作' }}</small>
            </span>
          </button>
          <div class="palette-boundary">
            <svg viewBox="0 0 16 16" width="12" height="12" fill="currentColor" opacity="0.5"><path d="M8 1a7 7 0 100 14A7 7 0 008 1zm0 2.5a1 1 0 110 2 1 1 0 010-2zM7 7h2v5H7V7z" /></svg>
            <span>审批节点引用已发布模型；会签、驳回、退回和字段权限在流程设计器中配置。</span>
          </div>
        </aside>

        <main class="canvas-pane min-w-0 flex-1">
          <BusinessProcessCanvas
            :schema="designer.schema.value"
            :selected-node-id="designer.selectedNodeId.value"
            :readonly="readonly"
            :palette="palette"
            :dragging-node-type="draggingNodeType"
            @node-select="handleNodeSelect"
            @node-delete="handleDeleteNode"
            @insert-node="handleInsertNode"
          />
        </main>

        <aside class="issue-pane" :class="{ 'is-collapsed': !issuesExpanded }">
          <button type="button" class="issue-pane-head" @click="issuesExpanded = !issuesExpanded">
            <span>
              <strong>流程问题</strong>
              <em>{{ issues.length }}</em>
            </span>
            <span>{{ issuesExpanded ? '收起' : '展开' }}</span>
          </button>
          <div v-if="issuesExpanded" class="issue-list">
            <div
              v-for="(item, index) in issues"
              :key="`${item.code}-${item.nodeId}-${index}`"
              class="issue-item"
              :class="{ 'issue-item--warning': item.level === 'WARNING' }"
            >
              <button
                type="button"
                class="issue-locate"
                data-issue-locate
                @click="locateIssue(item)"
              >
                <strong>{{ item.message }}</strong>
                <span>{{ issueLocation(item) }}</span>
                <span v-if="item.code">错误编码：{{ item.code }}</span>
                <span v-if="issuePath(item)">配置位置：{{ issuePath(item) }}</span>
                <span v-if="issueSuggestion(item)">处理建议：{{ issueSuggestion(item) }}</span>
              </button>
              <button
                type="button"
                class="issue-copy"
                data-issue-copy
                title="复制完整问题"
                @click.stop="copyIssue(item)"
              >
                复制
              </button>
            </div>
            <div v-if="!issues.length" class="issue-empty">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="issue-empty-icon"><path d="M22 11.08V12a10 10 0 11-5.93-9.14" /><path d="M22 4L12 14.01l-3-3" /></svg>
              <span>流程结构完整</span>
              <small>发布前仍需执行服务端依赖与权限校验</small>
            </div>
          </div>
        </aside>
      </div>

      <BusinessProcessNodeConfigDrawer
        :visible="drawerVisible"
        :node="selectedNode"
        :readonly="readonly"
        :object-id="objectId"
        :object-code="designer.schema.value.subject?.objectCode"
        :object-name="objectName"
        :fields="fields"
        :objects="objects"
        :flow-models="flowModels"
        :form-assets="formAssets"
        :business-actions="businessActions"
        :message-templates="messageTemplates"
        :capabilities="capabilities"
        :sub-processes="subProcesses"
        :service-actors="serviceActors"
        @update:visible="drawerVisible = $event"
        @save="handleDrawerSave"
        @open-flow-designer="emit('openFlowDesigner', $event)"
        @refresh-flow-model="emit('refreshFlowModel', $event)"
        @refresh-fields="emit('refreshFields', $event)"
        @edit-action="emit('editAction', $event)"
      />
    </div>
  </section>
</template>

<style scoped>
.business-process-designer {
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: var(--card-color, #fff);
}

.designer-toolbar {
  display: flex;
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.25);
  padding: 8px 14px;
}

.designer-title-block {
  display: grid;
  min-width: 0;
  align-items: baseline;
  column-gap: 8px;
  grid-template-columns: auto minmax(0, 1fr);
}

.designer-title-block > span {
  color: var(--text-color-3, #64748b);
  font-size: 11px;
  grid-column: 1 / -1;
}

.designer-title-block strong {
  overflow: hidden;
  color: var(--text-color-1, #0f172a);
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.designer-title-block em {
  color: var(--text-color-3, #64748b);
  font-size: 11px;
  font-style: normal;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.toolbar-divider {
  display: inline-block;
  width: 1px;
  height: 18px;
  margin: 0 4px;
  background: rgba(148, 163, 184, 0.25);
  vertical-align: middle;
}

.save-state {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-right: 4px;
  color: var(--text-color-3, #64748b);
  font-size: 12px;
}

.save-state-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.save-state.is-saved {
  color: var(--success-color, #15803d);
}

.save-state.is-conflict,
.save-state.is-error {
  color: var(--error-color, #dc2626);
}

.save-state.is-saving {
  color: var(--primary-color, #2563eb);
}

.save-state.is-saving .save-state-dot {
  animation: save-pulse 1.2s ease-in-out infinite;
}

@keyframes save-pulse {
  0%,
  100% {
    opacity: 0.4;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.4);
  }
}

.conflict-alert {
  border-bottom: 1px solid rgba(239, 68, 68, 0.16);
}

.conflict-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.designer-body {
  background: var(--body-color, #f7f9fa);
}

.designer-stage {
  position: relative;
}

.node-palette {
  width: 220px;
  flex: 0 0 220px;
  overflow-y: auto;
  border-right: 1px solid rgba(148, 163, 184, 0.2);
  background: var(--card-color, #fff);
  padding: 16px 12px;
}

.pane-heading {
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin: 0 4px 14px;
}

.pane-heading strong {
  color: var(--text-color-1, #0f172a);
  font-size: 13px;
  font-weight: 600;
}

.pane-heading span {
  color: var(--text-color-3, #64748b);
  font-size: 11px;
}

.palette-item {
  position: relative;
  display: flex;
  width: 100%;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background: var(--card-color, #fff);
  padding: 10px 10px;
  text-align: left;
  cursor: grab;
  transition:
    border-color 140ms ease,
    transform 140ms ease,
    box-shadow 140ms ease;
}

.palette-item:active:not(:disabled) {
  cursor: grabbing;
}

.palette-item[draggable='true']::after {
  position: absolute;
  top: 50%;
  right: 8px;
  color: var(--text-color-3, #cbd5e1);
  content: '⋮⋮';
  font-size: 10px;
  letter-spacing: -3px;
  transform: translateY(-50%);
}

.palette-item:hover:not(:disabled) {
  border-color: rgba(37, 99, 235, 0.36);
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.06);
  transform: translateX(2px);
}

.palette-icon {
  display: flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 7px;
  color: #fff;
}

.palette-icon.tone-condition {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.palette-icon.tone-action {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

.palette-icon.tone-approval {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
}

.palette-icon.tone-sub-process {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
}

.palette-text {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.palette-text strong {
  color: var(--text-color-1, #0f172a);
  font-size: 12.5px;
  font-weight: 600;
}

.palette-text small {
  color: var(--text-color-3, #94a3b8);
  font-size: 11px;
}

.palette-boundary {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 14px;
  border-radius: 6px;
  background: rgba(148, 163, 184, 0.06);
  padding: 10px;
  color: var(--text-color-3, #94a3b8);
  font-size: 11px;
  line-height: 1.55;
}

.palette-boundary svg {
  flex-shrink: 0;
  margin-top: 1px;
}

.canvas-pane {
  position: relative;
}

.issue-pane {
  width: 250px;
  flex: 0 0 250px;
  overflow: hidden;
  border-left: 1px solid rgba(148, 163, 184, 0.25);
  background: var(--card-color, #fff);
  transition:
    width 160ms ease,
    flex-basis 160ms ease;
}

.issue-pane.is-collapsed {
  width: 48px;
  flex-basis: 48px;
}

.issue-pane-head {
  display: flex;
  width: 100%;
  height: 46px;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(148, 163, 184, 0.22);
  padding: 0 12px;
  color: var(--text-color-3, #64748b);
  font-size: 11px;
}

.issue-pane-head > span:first-child {
  display: flex;
  align-items: center;
  gap: 7px;
}

.issue-pane-head strong {
  color: var(--text-color-1, #0f172a);
  font-size: 12px;
}

.issue-pane-head em {
  min-width: 20px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.09);
  padding: 1px 6px;
  color: var(--error-color, #dc2626);
  font-style: normal;
}

.issue-pane.is-collapsed .issue-pane-head {
  height: 100%;
  justify-content: center;
  padding: 12px 0;
  writing-mode: vertical-rl;
}

.issue-list {
  max-height: calc(100% - 46px);
  overflow-y: auto;
  padding: 10px;
}

.issue-item {
  position: relative;
  display: flex;
  width: 100%;
  margin-bottom: 7px;
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-left: 4px solid rgba(239, 68, 68, 0.6);
  border-radius: 6px;
  background: rgba(254, 242, 242, 0.5);
  text-align: left;
  user-select: text;
}

.issue-item--warning {
  border-color: rgba(245, 158, 11, 0.2);
  border-left-color: rgba(245, 158, 11, 0.7);
  background: rgba(255, 251, 235, 0.6);
}

.issue-item--warning .issue-locate:hover {
  background: rgba(254, 243, 199, 0.4);
}

.issue-locate {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  border: 0;
  background: transparent;
  padding: 8px 48px 8px 9px;
  text-align: left;
  cursor: pointer;
}

.issue-locate:hover {
  background: rgba(254, 226, 226, 0.4);
}

.issue-locate strong {
  color: var(--text-color-1, #0f172a);
  font-size: 12px;
  line-height: 1.45;
  user-select: text;
}

.issue-locate span {
  color: var(--text-color-3, #64748b);
  font-size: 10px;
  line-height: 1.4;
  overflow-wrap: anywhere;
  user-select: text;
}

.issue-copy {
  position: absolute;
  top: 7px;
  right: 7px;
  border: 0;
  background: transparent;
  color: var(--primary-color, #2563eb);
  font-size: 11px;
  cursor: pointer;
}

.issue-copy:hover {
  text-decoration: underline;
}

.issue-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 24px 8px;
  text-align: center;
}

.issue-empty-icon {
  color: var(--success-color, #15803d);
  opacity: 0.5;
  margin-bottom: 2px;
}

.issue-empty span {
  color: var(--text-color-2, #334155);
  font-size: 13px;
  font-weight: 500;
}

.issue-empty small {
  color: var(--text-color-3, #94a3b8);
  font-size: 11px;
  line-height: 1.5;
}

@media (max-width: 1100px) {
  .issue-pane {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .issue-pane {
    transition: none;
  }
}
</style>
