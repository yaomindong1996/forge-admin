<template>
  <section class="flow-node-panel">
    <n-empty
      v-if="!flowModelKey"
      size="small"
      description="先在上方绑定流程模型，节点列表会自动读取审批节点"
      class="node-empty"
    />

    <template v-else>
      <n-alert v-if="manualMode" type="warning" :bordered="false" class="node-mode-alert">
        流程节点解析失败，已回退为手动模式；可先在完整绑定设置中检查流程模型，或直接手填节点标识。
      </n-alert>

      <div class="node-table-head node-grid">
        <span>节点</span>
        <span>办理人</span>
        <span>可见分区</span>
        <span>只读分区</span>
        <span class="node-toggle-col" />
      </div>

      <template v-for="row in nodeRows" :key="row.key">
        <div class="node-table-row node-grid" :class="{ 'is-expanded': expandedKeys.has(row.key), 'is-manual': row.manual }" role="button" tabindex="0" @click="toggleRow(row.key)" @keydown.enter="toggleRow(row.key)">
          <span class="node-name">
            <strong>{{ row.taskName }}</strong>
            <small v-if="row.manual" class="node-manual-tag">手动添加</small>
          </span>
          <span class="node-assignee">{{ row.assigneeSummary }}</span>
          <span>{{ visibleSummary(row) }}</span>
          <span>{{ readonlySummary(row) }}</span>
          <span class="node-toggle-col">
            <n-icon class="node-toggle-icon" :class="{ 'is-open': expandedKeys.has(row.key) }"><ChevronDownOutline /></n-icon>
          </span>
        </div>

        <div v-if="expandedKeys.has(row.key)" class="node-config">
          <div class="node-config-grid">
            <div>
              <n-form-item label="可见分区" :show-feedback="false">
                <n-select
                  :value="row.permission.visibleSectionIds"
                  multiple
                  clearable
                  :options="sectionSelectOptions(row.permission.visibleSectionIds)"
                  placeholder="不选表示该节点可见全部分区"
                  @update:value="patchNodePermission(row, { visibleSectionIds: $event })"
                />
              </n-form-item>
              <p class="node-config-hint">
                不选表示该节点可见全部分区。
              </p>
            </div>
            <div>
              <n-form-item label="只读分区" :show-feedback="false">
                <n-select
                  :value="row.permission.readonlySectionIds"
                  multiple
                  clearable
                  :options="sectionSelectOptions(row.permission.readonlySectionIds)"
                  placeholder="不选表示所有分区均可编辑"
                  @update:value="patchNodePermission(row, { readonlySectionIds: $event })"
                />
              </n-form-item>
              <p class="node-config-hint">
                不选表示该节点下所有分区均可编辑。
              </p>
            </div>
          </div>
          <div v-if="row.manual" class="node-manual-tools">
            <n-input
              :value="row.permission.nodeKey"
              size="small"
              placeholder="流程节点标识，如 managerApproval"
              @update:value="patchNodePermission(row, { nodeKey: $event })"
            />
            <n-button circle quaternary type="error" size="small" title="删除节点策略" @click.stop="removeNodePermission(row)">
              <template #icon>
                <n-icon><TrashOutline /></n-icon>
              </template>
            </n-button>
          </div>
        </div>
      </template>

      <div v-if="manualMode || !nodeRows.length" class="node-manual-add">
        <n-button size="small" dashed @click.stop="addManualNodePermission">
          <template #icon>
            <n-icon><AddOutline /></n-icon>
          </template>
          手动添加节点
        </n-button>
      </div>

      <p v-if="!nodeRows.length && !manualMode" class="node-config-hint">
        当前流程模型没有解析到审批节点（userTask）；请确认模型已发布且包含用户任务。
      </p>
    </template>
  </section>
</template>

<script setup>
import { AddOutline, ChevronDownOutline, TrashOutline } from '@vicons/ionicons5'
import { computed, reactive, ref, watch } from 'vue'
import { businessFlowVariables } from '@/api/business-app'
import { normalizeFlowInteraction } from '../in-app-builder/in-app-builder-schema'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({}),
  },
  flowModelKey: {
    type: String,
    default: '',
  },
  objectCode: {
    type: String,
    default: '',
  },
  sectionOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])

const userTasks = ref([])
const loading = reactive({ tasks: false })
const expandedKeys = reactive(new Set())
// 流程模型未选或解析失败时回退手填模式，不阻塞配置。
const manualMode = ref(false)

const draft = computed(() => normalizeFlowInteraction(props.modelValue))
const nodePermissions = computed(() => draft.value.nodePermissions)
const taskKeySet = computed(() => new Set(userTasks.value.map(task => task.taskDefKey)))

const nodeRows = computed(() => {
  const rows = userTasks.value.map((task) => {
    const permission = nodePermissions.value.find(item => item.nodeKey === task.taskDefKey)
    return {
      key: `task:${task.taskDefKey}`,
      taskDefKey: task.taskDefKey,
      taskName: task.taskName || task.taskDefKey,
      assigneeSummary: taskAssigneeSummary(task),
      manual: false,
      permission: permission || { nodeKey: task.taskDefKey, visibleSectionIds: [], readonlySectionIds: [] },
    }
  })
  // 手动行用“位置 + key”生成稳定行标识，编辑 nodeKey 过程中不丢失展开态。
  const manualRows = nodePermissions.value
    .filter(item => item.nodeKey && !taskKeySet.value.has(item.nodeKey))
    .map((item, manualIndex) => ({
      key: `manual:${manualIndex}:${item.nodeKey}`,
      taskDefKey: item.nodeKey,
      taskName: item.nodeKey,
      assigneeSummary: '未匹配到流程节点',
      manual: true,
      permission: item,
    }))
  return [...rows, ...manualRows]
})

watch(() => [props.flowModelKey, props.objectCode], ([modelKey]) => {
  userTasks.value = []
  manualMode.value = false
  if (modelKey)
    loadUserTasks(modelKey)
}, { immediate: true })

async function loadUserTasks(modelKey) {
  loading.tasks = true
  try {
    const res = await businessFlowVariables(modelKey, { objectCode: props.objectCode })
    const data = res.data || {}
    userTasks.value = normalizeUserTasks(data.userTasks || [])
    manualMode.value = false
  }
  catch {
    userTasks.value = []
    manualMode.value = true
  }
  finally {
    loading.tasks = false
  }
}

// 与 BusinessFlowBindingPanel.normalizeUserTasks 保持一致的节点归一化。
function normalizeUserTasks(list = []) {
  return (Array.isArray(list) ? list : [])
    .map(item => ({
      taskDefKey: normalizeText(item.taskDefKey || item.id),
      taskName: normalizeText(item.taskName || item.name),
      assignee: normalizeText(item.assignee || item.assigneeName),
      candidateUsers: normalizeList(item.candidateUsers || item.userCandidates || item.users),
      candidateGroups: normalizeList(item.candidateGroups || item.roleCandidates || item.groups),
    }))
    .filter(item => item.taskDefKey)
}

function normalizeText(value) {
  return String(value ?? '').trim()
}

function normalizeList(value) {
  return (Array.isArray(value) ? value : []).map(item => normalizeText(item)).filter(Boolean)
}

function taskAssigneeSummary(task = {}) {
  if (task.assignee)
    return `审批人：${task.assignee}`
  if (task.candidateGroups?.length)
    return `角色：${task.candidateGroups.join('、')}`
  if (task.candidateUsers?.length)
    return `用户：${task.candidateUsers.join('、')}`
  return '流程设计器配置'
}

function sectionSelectOptions(selected = []) {
  const knownValues = new Set(props.sectionOptions.map(option => option.value))
  const orphanOptions = selected
    .filter(id => !knownValues.has(id))
    .map(id => ({ label: `${id}（已失效）`, value: id, disabled: true, class: 'is-orphan-option' }))
  return [...props.sectionOptions, ...orphanOptions]
}

function visibleSummary(row) {
  const ids = row.permission.visibleSectionIds || []
  return ids.length ? `${ids.length}/${props.sectionOptions.length} 选中` : '全部可见'
}

function readonlySummary(row) {
  const ids = row.permission.readonlySectionIds || []
  return ids.length ? `${ids.length} 个` : '-'
}

function toggleRow(key) {
  if (expandedKeys.has(key))
    expandedKeys.delete(key)
  else
    expandedKeys.add(key)
}

function emitNodePermissions(next) {
  emit('update:modelValue', normalizeFlowInteraction({ ...draft.value, nodePermissions: next }))
}

function patchNodePermission(row, patch) {
  const nextKey = patch.nodeKey !== undefined ? normalizeText(patch.nodeKey) : row.permission.nodeKey
  const exists = nodePermissions.value.some(item => item.nodeKey === row.permission.nodeKey)
  let next
  if (exists) {
    next = nodePermissions.value.map(item => item.nodeKey === row.permission.nodeKey ? { ...item, ...patch, nodeKey: nextKey } : item)
  }
  else {
    const record = { ...row.permission, ...patch, nodeKey: nextKey }
    next = record.nodeKey ? [...nodePermissions.value, record] : nodePermissions.value
  }
  emitNodePermissions(next)
}

function addManualNodePermission() {
  const usedKeys = new Set(nodePermissions.value.map(item => item.nodeKey))
  let index = nodePermissions.value.length + 1
  while (usedKeys.has(`node_${index}`))
    index += 1
  const record = { nodeKey: `node_${index}`, visibleSectionIds: [], readonlySectionIds: [] }
  const manualCount = nodePermissions.value.filter(item => item.nodeKey && !taskKeySet.value.has(item.nodeKey)).length
  emitNodePermissions([...nodePermissions.value, record])
  expandedKeys.add(`manual:${manualCount}:${record.nodeKey}`)
}

function removeNodePermission(row) {
  emitNodePermissions(nodePermissions.value.filter(item => item.nodeKey !== row.permission.nodeKey))
  expandedKeys.delete(row.key)
}
</script>

<style scoped>
.flow-node-panel {
  min-width: 0;
}

.node-empty {
  padding: 40px 0;
}

.node-mode-alert {
  margin-bottom: 12px;
}

.node-grid {
  display: grid;
  grid-template-columns: minmax(180px, 1.3fr) minmax(160px, 1fr) minmax(110px, 0.7fr) minmax(90px, 0.5fr) 36px;
  align-items: center;
  gap: 12px;
}

.node-table-head {
  padding: 6px 12px;
  color: var(--text-tertiary);
  background: var(--bg-secondary);
  font-size: 12px;
  font-weight: 600;
  border-radius: 6px 6px 0 0;
}

.node-table-row {
  min-height: 52px;
  padding: 6px 12px;
  border-bottom: 1px solid #f0f1f2;
  cursor: pointer;
  transition: background 0.15s ease;
}

.node-table-row:hover {
  background: #fafbfc;
}

.node-table-row.is-expanded {
  background: #f8f9fb;
}

.node-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.node-name strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-manual-tag {
  flex: 0 0 auto;
  padding: 1px 6px;
  border-radius: 999px;
  background: #fff7e6;
  color: #bf8700;
  font-size: 10px;
}

.node-assignee {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-table-row > span:not(.node-name):not(.node-assignee):not(.node-toggle-col) {
  color: var(--text-secondary);
  font-size: 12px;
}

.node-toggle-col {
  display: grid;
  place-items: center;
}

.node-toggle-icon {
  color: var(--text-tertiary);
  transition: transform 0.18s ease;
}

.node-toggle-icon.is-open {
  transform: rotate(180deg);
}

.node-config {
  padding: 14px 16px;
  border-bottom: 1px solid #f0f1f2;
  background: #fbfcfd;
}

.node-config-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 24px;
}

.node-config-hint {
  margin: 6px 0 0;
  color: #a9aeb8;
  font-size: 11px;
}

.node-manual-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

.node-manual-add {
  padding: 12px;
}

.flow-node-panel :deep(.is-orphan-option) {
  color: #d03050;
}

@media (max-width: 980px) {
  .node-grid {
    grid-template-columns: minmax(140px, 1.2fr) minmax(120px, 1fr) 36px;
  }

  .node-grid > span:nth-child(3),
  .node-grid > span:nth-child(4) {
    display: none;
  }

  .node-config-grid {
    grid-template-columns: 1fr;
  }
}
</style>
