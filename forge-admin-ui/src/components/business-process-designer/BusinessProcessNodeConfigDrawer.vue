<script setup>
import { NButton } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import ActionAndApprovalNodeConfig from './ActionAndApprovalNodeConfig.vue'
import {
  createBusinessProcessNodeTemplate,
  getBusinessProcessNodeDefinition,
  isBusinessProcessStartType,
} from './business-process-node-types.js'
import BusinessProcessConditionConfig from './BusinessProcessConditionConfig.vue'
import StartNodeConfig from './StartNodeConfig.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  node: { type: Object, default: null },
  objectId: { type: String, default: '' },
  objectCode: { type: String, default: '' },
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
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits([
  'update:visible',
  'save',
  'openFlowDesigner',
  'refreshFlowModel',
  'refreshFields',
  'editAction',
])

const draftNode = ref(null)
const draftRecordIdSource = ref(null)

const title = computed(() => draftNode.value?.name || '节点配置')
const typeLabel = computed(() => getBusinessProcessNodeDefinition(draftNode.value?.type)?.label || '业务节点')
const isExecutionNode = computed(() => ['ACTION', 'APPROVAL', 'SUB_PROCESS'].includes(draftNode.value?.type))

watch(
  () => [props.visible, props.node?.id],
  ([visible]) => {
    if (visible && props.node) {
      draftNode.value = clone(props.node)
      draftRecordIdSource.value = null
    }
  },
  { immediate: true },
)

function patchConfig(config) {
  if (!draftNode.value)
    return
  draftNode.value.config = clone(config)
  persistDraft()
}

function handleStartType(type) {
  const template = createBusinessProcessNodeTemplate(type)
  const defaultStartNames = ['START_MANUAL', 'START_EVENT', 'START_SCHEDULE']
    .map(startType => createBusinessProcessNodeTemplate(startType).name)
  // 用户没改过名字时跟随触发方式，避免事件/定时触发节点仍叫「手动开始」
  const name = !draftNode.value.name || defaultStartNames.includes(draftNode.value.name)
    ? template.name
    : draftNode.value.name
  draftNode.value = {
    ...draftNode.value,
    name,
    type,
    ports: template.ports,
    config: template.config,
  }
  persistDraft()
}

function patchBranches(branches) {
  draftNode.value.config = {
    ...(draftNode.value.config || {}),
    branches,
  }
  draftNode.value.ports = branches.map(branch => branch.port)
  persistDraft()
}

function persistDraft() {
  if (!draftNode.value || props.readonly)
    return
  emit('save', clone(draftNode.value), {
    recordIdSource: draftRecordIdSource.value,
    reject: () => {},
  })
}

function handleSave() {
  persistDraft()
  emit('update:visible', false)
}

function clone(value) {
  return value == null ? value : JSON.parse(JSON.stringify(value))
}
</script>

<template>
  <section v-if="visible && draftNode" class="node-config-panel" data-node-config="panel">
    <header class="node-config-header">
      <div class="drawer-heading">
        <strong>{{ title }}</strong>
        <span>{{ typeLabel }}</span>
      </div>
      <button type="button" class="node-config-close" aria-label="收起" @click="emit('update:visible', false)">
        ×
      </button>
    </header>

    <div v-if="draftNode" class="drawer-form">
      <label class="name-field">
        <span>节点名称</span>
        <input v-model="draftNode.name" :disabled="readonly" maxlength="80" @change="persistDraft">
      </label>

      <StartNodeConfig
        v-if="isBusinessProcessStartType(draftNode.type)"
        :type="draftNode.type"
        :config="draftNode.config"
        :fields="fields"
        :service-actors="serviceActors"
        @update:type="handleStartType"
        @update:config="patchConfig"
        @update:record-id-source="draftRecordIdSource = $event"
      />

      <ActionAndApprovalNodeConfig
        v-else-if="isExecutionNode"
        :node="draftNode"
        :object-id="objectId"
        :object-code="objectCode"
        :object-name="objectName"
        :objects="objects"
        :fields="fields"
        :flow-models="flowModels"
        :form-assets="formAssets"
        :business-actions="businessActions"
        :message-templates="messageTemplates"
        :capabilities="capabilities"
        :sub-processes="subProcesses"
        @update:config="patchConfig"
        @open-flow-designer="emit('openFlowDesigner', $event)"
        @refresh-flow-model="emit('refreshFlowModel', $event)"
        @refresh-fields="emit('refreshFields', $event)"
        @edit-action="emit('editAction', $event)"
      />

      <BusinessProcessConditionConfig
        v-else-if="draftNode.type === 'CONDITION'"
        :branches="draftNode.config?.branches || []"
        :fields="fields"
        :readonly="readonly"
        @update:branches="patchBranches"
      />

      <label v-else-if="draftNode.type === 'END'" class="name-field">
        <span>结束结果</span>
        <select v-model="draftNode.config.result" @change="persistDraft">
          <option value="SUCCESS">成功完成</option>
          <option value="REJECTED">业务驳回</option>
          <option value="CANCELED">流程取消</option>
          <option value="FAILED">执行失败</option>
        </select>
      </label>
    </div>

    <footer class="drawer-actions">
      <NButton @click="emit('update:visible', false)">
        收起
      </NButton>
      <NButton type="primary" :disabled="readonly" @click="handleSave">
        完成
      </NButton>
    </footer>
  </section>
</template>

<style scoped>
.node-config-panel {
  position: absolute;
  top: 12px;
  right: 12px;
  bottom: 12px;
  z-index: 50;
  display: flex;
  width: min(720px, calc(100% - 24px));
  max-width: calc(100% - 24px);
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  container-name: node-config;
  container-type: inline-size;
  box-sizing: border-box;
  isolation: isolate;
  border: 1px solid rgba(148, 163, 184, 0.34);
  border-radius: 9px;
  background: var(--card-color, #fff);
  box-shadow: 0 18px 46px rgba(15, 23, 42, 0.18);
}

.node-config-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.22);
  padding: 18px 26px 14px;
}

.node-config-close {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-color-3, #64748b);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.node-config-close:hover {
  background: rgba(148, 163, 184, 0.12);
  color: var(--text-color-1, #0f172a);
}

.drawer-heading {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.drawer-heading strong {
  color: var(--text-color-1, #0f172a);
  font-size: 15px;
}

.drawer-heading span {
  color: var(--text-color-3, #64748b);
  font-size: 11px;
}

.drawer-form {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  gap: 22px;
  overflow: auto;
  padding: 20px 26px 24px;
}

.name-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  color: var(--text-color-1, #0f172a);
  font-size: 13px;
  font-weight: 600;
}

.name-field input,
.name-field select,
.branch-row > input {
  min-height: 34px;
  border: 1px solid rgba(148, 163, 184, 0.45);
  border-radius: 6px;
  background: var(--input-color, #fff);
  padding: 6px 9px;
  color: var(--text-color-1, #0f172a);
  font-weight: 400;
  outline: none;
}

.condition-editor {
  border-top: 1px solid rgba(148, 163, 184, 0.25);
  padding-top: 16px;
}

.condition-editor-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}

.condition-editor-head div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.condition-editor-head span,
.condition-editor p {
  color: var(--text-color-3, #64748b);
  font-size: 12px;
}

.condition-editor-head button {
  color: var(--primary-color, #2563eb);
  font-size: 12px;
}

.branch-row {
  display: grid;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  grid-template-columns: 1fr auto auto;
}

.branch-row label {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-color-2, #475569);
  font-size: 12px;
}

.branch-row button {
  color: var(--error-color, #dc2626);
  font-size: 12px;
}

.branch-row button:disabled {
  opacity: 0.4;
}

.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  width: 100%;
  flex: 0 0 auto;
  border-top: 1px solid rgba(148, 163, 184, 0.22);
  padding: 13px 26px 17px;
}

@media (max-width: 760px) {
  .node-config-panel {
    top: auto;
    right: 8px;
    bottom: 8px;
    left: 8px;
    width: auto;
    max-height: min(72%, 620px);
  }

  .node-config-header,
  .drawer-form,
  .drawer-actions {
    padding-right: 14px;
    padding-left: 14px;
  }
}
</style>
