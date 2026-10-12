<template>
  <section class="flow-interaction-panel">
    <header class="panel-heading">
      <div>
        <h2>节点配置</h2>
        <p>节点和办理人来自当前流程模型；这里只配置应用表单在各审批节点的分区呈现。</p>
      </div>
      <n-tag v-if="flowModelKey" size="small" :bordered="false" type="info">
        {{ flowModelKey }}
      </n-tag>
    </header>

    <section class="node-config-section">
      <n-spin :show="tasksLoading">
        <div v-if="nodeRows.length" class="node-table" role="table" aria-label="流程节点分区配置">
          <div class="node-table-header" role="row">
            <span>节点</span>
            <span>办理人</span>
            <span>可见分区</span>
            <span>只读分区</span>
            <span aria-hidden="true" />
          </div>
          <article
            v-for="row in nodeRows"
            :key="row.nodeKey"
            class="node-table-row"
            :class="{ expanded: expandedNodeKey === row.nodeKey, stale: row.stale }"
          >
            <button type="button" class="node-row-summary" @click="toggleNode(row.nodeKey)">
              <span class="node-name-cell">
                <strong>{{ row.nodeName }}</strong>
                <small v-if="row.stale">原流程节点 · {{ row.nodeKey }}</small>
              </span>
              <span>{{ row.assigneeSummary }}</span>
              <span>{{ visibleSectionSummary(row) }}</span>
              <span>{{ row.readonlySectionIds.length }} 个</span>
              <n-icon class="node-expand-icon">
                <ChevronUpOutline v-if="expandedNodeKey === row.nodeKey" />
                <ChevronDownOutline v-else />
              </n-icon>
            </button>

            <div v-if="expandedNodeKey === row.nodeKey" class="node-row-editor">
              <n-alert v-if="row.stale" type="warning" :bordered="false">
                当前流程模型已找不到该节点。配置会继续保留，请确认流程改版后是否仍需使用。
              </n-alert>
              <div v-if="fallbackManualMode" class="manual-node-key">
                <n-form-item label="流程节点标识">
                  <n-input
                    :value="row.nodeKey"
                    placeholder="流程解析失败时可手动填写"
                    @update:value="renameNodePermission(row.nodeKey, $event)"
                  />
                </n-form-item>
                <n-button quaternary type="error" @click="removeNodePermission(row.nodeKey)">
                  删除策略
                </n-button>
              </div>
              <div class="node-section-grid">
                <n-form-item label="可见分区">
                  <n-select
                    :value="row.visibleSectionIds"
                    :options="sectionOptions(row.visibleSectionIds)"
                    :render-label="renderSectionOption"
                    multiple
                    filterable
                    clearable
                    placeholder="不选表示全部可见"
                    @update:value="patchNodePermission(row.nodeKey, { visibleSectionIds: $event })"
                  />
                  <small class="field-hint">不选表示该节点可见全部分区</small>
                </n-form-item>
                <n-form-item label="只读分区">
                  <n-select
                    :value="row.readonlySectionIds"
                    :options="sectionOptions(row.readonlySectionIds)"
                    :render-label="renderSectionOption"
                    multiple
                    filterable
                    clearable
                    placeholder="不选表示全部可编辑"
                    @update:value="patchNodePermission(row.nodeKey, { readonlySectionIds: $event })"
                  />
                  <small class="field-hint">不选表示该节点下所有分区均可编辑</small>
                </n-form-item>
              </div>
            </div>
          </article>
        </div>

        <n-empty
          v-else-if="!flowModelKey"
          size="small"
          description="先在上方绑定流程模型，节点会自动显示在这里"
        />
        <n-empty
          v-else-if="tasksError"
          size="small"
          :description="tasksError"
        >
          <template #extra>
            <n-button size="small" secondary @click="addNodePermission">
              手动添加节点策略
            </n-button>
          </template>
        </n-empty>
        <n-empty v-else size="small" description="当前流程模型没有可配置的审批节点" />
      </n-spin>

      <div v-if="fallbackManualMode && nodeRows.length" class="manual-node-footer">
        <span>流程节点解析失败，当前使用历史手动配置兜底。</span>
        <n-button size="small" secondary @click="addNodePermission">
          <template #icon>
            <n-icon><AddOutline /></n-icon>
          </template>
          添加节点策略
        </n-button>
      </div>
    </section>

    <n-collapse class="advanced-config-collapse" arrow-placement="right">
      <n-collapse-item name="approval-actions">
        <template #header>
          <span class="collapse-title">
            审批操作
            <i v-if="draft.approvalActions.length" title="已有配置" />
          </span>
        </template>
        <section class="advanced-section">
          <div class="section-heading">
            <span>配置移动端底部操作栏可用的流程按钮和按钮权限。</span>
            <n-button size="small" type="primary" secondary @click="addApprovalAction">
              <template #icon>
                <n-icon><AddOutline /></n-icon>
              </template>
              新增按钮
            </n-button>
          </div>
          <div v-for="(action, index) in draft.approvalActions" :key="action.actionId || index" class="action-row">
            <n-input :value="action.label" placeholder="按钮名称" @update:value="patchApprovalAction(index, { label: $event })" />
            <n-select :value="action.operation" :options="operationOptions" @update:value="patchApprovalAction(index, { operation: $event })" />
            <n-input :value="action.permissionKey" placeholder="权限标识（可选）" @update:value="patchApprovalAction(index, { permissionKey: $event })" />
            <n-select :value="action.permissionStrategy" :options="permissionStrategyOptions" @update:value="patchApprovalAction(index, { permissionStrategy: $event })" />
            <n-switch :value="action.enabled !== false" @update:value="patchApprovalAction(index, { enabled: $event })" />
            <n-button circle quaternary type="error" title="删除审批按钮" @click="removeApprovalAction(index)">
              <template #icon>
                <n-icon><TrashOutline /></n-icon>
              </template>
            </n-button>
          </div>
          <n-empty v-if="!draft.approvalActions.length" size="small" description="未配置审批按钮" />
        </section>
      </n-collapse-item>

      <n-collapse-item name="timeline">
        <template #header>
          <span class="collapse-title">
            审批时间轴
            <i v-if="draft.timeline.enabled" title="已有配置" />
          </span>
        </template>
        <section class="advanced-section timeline-section">
          <div class="section-heading">
            <span>仅开启后，移动端才会按流程实例加载历史记录。</span>
            <n-switch :value="draft.timeline.enabled" @update:value="patchTimeline({ enabled: $event })" />
          </div>
          <n-form-item v-if="draft.timeline.enabled" label="区块标题">
            <n-input :value="draft.timeline.title" placeholder="审批记录" @update:value="patchTimeline({ title: $event })" />
          </n-form-item>
        </section>
      </n-collapse-item>

      <n-collapse-item name="callbacks">
        <template #header>
          <span class="collapse-title">
            完成回调
            <i v-if="hasCallbackConfig" title="已有配置" />
          </span>
        </template>
        <section class="advanced-section callback-section">
          <p>审批结果到达后调用当前对象的业务动作，不在这里复制动作步骤。</p>
          <div class="callback-grid">
            <n-form-item label="审批通过后">
              <n-select
                :value="draft.callbacks.approvedActionCode || ''"
                :options="actionOptions"
                filterable
                clearable
                placeholder="可选"
                @update:value="patchCallbacks({ approvedActionCode: $event || '' })"
              />
            </n-form-item>
            <n-form-item label="审批拒绝后">
              <n-select
                :value="draft.callbacks.rejectedActionCode || ''"
                :options="actionOptions"
                filterable
                clearable
                placeholder="可选"
                @update:value="patchCallbacks({ rejectedActionCode: $event || '' })"
              />
            </n-form-item>
          </div>
        </section>
      </n-collapse-item>
    </n-collapse>
  </section>
</template>

<script setup>
import { AddOutline, ChevronDownOutline, ChevronUpOutline, TrashOutline } from '@vicons/ionicons5'
import { computed, h, ref, watch } from 'vue'
import { normalizeFlowInteraction } from '../in-app-builder/in-app-builder-schema'
import {
  buildFlowNodePermissionRows,
  buildPageSectionOptions,
} from './application-flow-interaction'

const props = defineProps({
  modelValue: { type: Object, default: () => ({}) },
  actions: { type: Array, default: () => [] },
  flowModelKey: { type: String, default: '' },
  userTasks: { type: Array, default: () => [] },
  pageSections: { type: Array, default: () => [] },
  tasksLoading: { type: Boolean, default: false },
  tasksError: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

const expandedNodeKey = ref('')
const draft = computed(() => normalizeFlowInteraction(props.modelValue))
const fallbackManualMode = computed(() => Boolean(props.flowModelKey && props.tasksError))
const nodeRows = computed(() => {
  if (!props.flowModelKey)
    return []
  return buildFlowNodePermissionRows(
    fallbackManualMode.value ? [] : props.userTasks,
    draft.value.nodePermissions,
  )
})
const operationOptions = [
  { label: '同意', value: 'approve' },
  { label: '拒绝', value: 'reject' },
  { label: '退回', value: 'return' },
  { label: '委派', value: 'delegate' },
]
const permissionStrategyOptions = [
  { label: '无权限时隐藏', value: 'hide' },
  { label: '无权限时禁用', value: 'disable' },
]
const actionOptions = computed(() => props.actions.map((action) => {
  const value = String(action?.actionCode || '').trim()
  return value ? { label: `${action?.actionName || value}（${value}）`, value } : null
}).filter(Boolean))
const hasCallbackConfig = computed(() => Boolean(
  draft.value.callbacks.approvedActionCode || draft.value.callbacks.rejectedActionCode,
))

watch(nodeRows, (rows) => {
  if (expandedNodeKey.value && rows.some(row => row.nodeKey === expandedNodeKey.value))
    return
  expandedNodeKey.value = rows.find(row => row.visibleSectionIds.length || row.readonlySectionIds.length)?.nodeKey || ''
}, { immediate: true })

function emitPatch(patch = {}) {
  emit('update:modelValue', normalizeFlowInteraction({ ...draft.value, ...patch }))
}

function toggleNode(nodeKey) {
  expandedNodeKey.value = expandedNodeKey.value === nodeKey ? '' : nodeKey
}

function visibleSectionSummary(row) {
  return row.visibleSectionIds.length ? `${row.visibleSectionIds.length}/${props.pageSections.length || row.visibleSectionIds.length} 选中` : '全部'
}

function sectionOptions(configuredIds) {
  return buildPageSectionOptions(props.pageSections, configuredIds)
}

function renderSectionOption(option) {
  return h('span', { class: option.invalid ? 'invalid-section-option' : '' }, option.label)
}

function addApprovalAction() {
  emitPatch({ approvalActions: [...draft.value.approvalActions, {
    actionId: `flow_action_${Date.now()}`,
    operation: 'approve',
    label: '同意',
    permissionKey: '',
    permissionStrategy: 'hide',
    enabled: true,
  }] })
}

function patchApprovalAction(index, patch) {
  emitPatch({ approvalActions: draft.value.approvalActions.map((action, actionIndex) => actionIndex === index ? { ...action, ...patch } : action) })
}

function removeApprovalAction(index) {
  emitPatch({ approvalActions: draft.value.approvalActions.filter((_, actionIndex) => actionIndex !== index) })
}

function patchTimeline(patch) {
  emitPatch({ timeline: { ...draft.value.timeline, ...patch } })
}

function addNodePermission() {
  const nodeKey = `node_${Date.now()}`
  emitPatch({ nodePermissions: [...draft.value.nodePermissions, { nodeKey, visibleSectionIds: [], readonlySectionIds: [] }] })
  expandedNodeKey.value = nodeKey
}

function patchNodePermission(nodeKey, patch) {
  const matched = draft.value.nodePermissions.some(permission => permission.nodeKey === nodeKey)
  const permissions = matched
    ? draft.value.nodePermissions.map(permission => permission.nodeKey === nodeKey ? { ...permission, ...patch } : permission)
    : [...draft.value.nodePermissions, { nodeKey, visibleSectionIds: [], readonlySectionIds: [], ...patch }]
  emitPatch({ nodePermissions: permissions })
}

function renameNodePermission(nodeKey, nextNodeKey) {
  const normalized = String(nextNodeKey || '').trim()
  emitPatch({
    nodePermissions: draft.value.nodePermissions.map(permission => permission.nodeKey === nodeKey ? { ...permission, nodeKey: normalized } : permission),
  })
  expandedNodeKey.value = normalized
}

function removeNodePermission(nodeKey) {
  emitPatch({ nodePermissions: draft.value.nodePermissions.filter(permission => permission.nodeKey !== nodeKey) })
  expandedNodeKey.value = ''
}

function patchCallbacks(patch) {
  emitPatch({ callbacks: { ...draft.value.callbacks, ...patch } })
}
</script>

<style scoped>
.flow-interaction-panel {
  display: grid;
  min-width: 0;
  gap: 0;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-primary);
}

.panel-heading {
  display: flex;
  min-height: 64px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--border-light);
}

.panel-heading h2,
.panel-heading p {
  margin: 0;
}

.panel-heading h2 {
  font-size: 16px;
}

.panel-heading p,
.advanced-section p {
  margin-top: 4px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.node-config-section {
  padding: 14px 18px 18px;
  border-bottom: 1px solid var(--border-light);
}

.node-table {
  border: 1px solid var(--border-light);
  border-radius: 6px;
  overflow: hidden;
}

.node-table-header,
.node-row-summary {
  display: grid;
  grid-template-columns: minmax(180px, 1.1fr) minmax(160px, 1fr) 120px 110px 24px;
  align-items: center;
  gap: 12px;
}

.node-table-header {
  min-height: 38px;
  padding: 0 14px;
  background: var(--bg-secondary);
  color: #646a73;
  font-size: 12px;
}

.node-table-row + .node-table-row {
  border-top: 1px solid var(--border-light);
}

.node-row-summary {
  width: 100%;
  min-height: 52px;
  cursor: pointer;
  border: 0;
  background: var(--bg-primary);
  padding: 0 14px;
  color: var(--text-primary);
  text-align: left;
}

.node-row-summary:hover,
.node-table-row.expanded .node-row-summary {
  background: #f7f9fc;
}

.node-table-row.stale .node-name-cell strong,
:deep(.invalid-section-option) {
  color: #d03050;
}

.node-name-cell strong,
.node-name-cell small {
  display: block;
}

.node-name-cell small {
  margin-top: 3px;
  color: #d03050;
  font-size: 11px;
}

.node-expand-icon {
  justify-self: end;
  color: var(--text-tertiary);
}

.node-row-editor {
  padding: 14px;
  border-top: 1px solid var(--border-light);
  background: #fafbfc;
}

.node-section-grid,
.callback-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.node-section-grid :deep(.n-form-item-feedback-wrapper) {
  min-height: 0;
}

.field-hint {
  display: block;
  margin-top: 5px;
  color: var(--text-tertiary);
  font-size: 11px;
}

.manual-node-key,
.manual-node-footer,
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.manual-node-key :deep(.n-form-item) {
  width: min(420px, 100%);
}

.manual-node-footer {
  margin-top: 12px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.advanced-config-collapse :deep(.n-collapse-item) {
  margin: 0;
  border-top: 0;
}

.advanced-config-collapse :deep(.n-collapse-item + .n-collapse-item) {
  border-top: 1px solid var(--border-light);
}

.advanced-config-collapse :deep(.n-collapse-item__header-main) {
  min-height: 48px;
  padding: 0 18px;
}

.advanced-config-collapse :deep(.n-collapse-item__content-inner) {
  padding: 0;
}

.collapse-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.collapse-title i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #18a058;
}

.advanced-section {
  padding: 14px 18px 18px;
  border-top: 1px solid #edf0f3;
}

.action-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) 120px minmax(160px, 1.2fr) 150px 36px 34px;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

.timeline-section :deep(.n-form-item) {
  max-width: 360px;
  margin-top: 12px;
}

@media (max-width: 1100px) {
  .node-table-header {
    display: none;
  }

  .node-row-summary {
    grid-template-columns: minmax(180px, 1fr) minmax(140px, 1fr) 90px 80px 24px;
  }

  .action-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .node-row-summary {
    grid-template-columns: minmax(0, 1fr) 24px;
  }

  .node-row-summary > span:not(.node-name-cell) {
    display: none;
  }

  .node-section-grid,
  .callback-grid,
  .action-row {
    grid-template-columns: 1fr;
  }
}
</style>
