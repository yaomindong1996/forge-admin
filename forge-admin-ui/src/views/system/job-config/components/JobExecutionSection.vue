<template>
  <div class="execution-section">
    <NFormItem label="调用方式" path="invokeMode">
      <NRadioGroup
        :value="form.invokeMode"
        name="invoke-mode"
        @update:value="handleInvokeModeChange"
      >
        <NRadioButton
          v-for="option in invokeModeOptions"
          :key="option.value"
          :value="option.value"
        >
          {{ option.label }}
        </NRadioButton>
      </NRadioGroup>
    </NFormItem>

    <div v-if="form.invokeMode === JOB_INVOKE_SINGLE" class="single-execution">
      <NFormItem label="执行方式" path="executeMode">
        <NRadioGroup
          :value="form.executeMode"
          name="execute-mode"
          @update:value="update('executeMode', $event)"
        >
          <NRadioButton value="HANDLER">
            任务处理器
          </NRadioButton>
          <NRadioButton value="BEAN">
            本地服务方法
          </NRadioButton>
          <NRadioButton value="RPC">
            远程服务
          </NRadioButton>
        </NRadioGroup>
      </NFormItem>

      <div v-if="form.executeMode === 'HANDLER'" class="target-panel">
        <NFormItem label="任务处理器" path="executorHandler">
          <NSelect
            :value="form.executorHandler"
            :options="handlerOptions"
            :loading="loading"
            filterable
            clearable
            placeholder="搜索并选择任务处理器"
            :filter="filterHandler"
            @update:value="update('executorHandler', $event || '')"
          />
        </NFormItem>
        <div v-if="selectedHandler" class="handler-summary">
          <div class="summary-icon">
            <i class="i-material-symbols:deployed-code-outline-rounded" />
          </div>
          <div>
            <strong>{{ selectedHandler.label }}</strong>
            <p>{{ selectedHandler.description }}</p>
            <span>{{ selectedHandler.group }}</span>
          </div>
        </div>
        <NAlert v-if="selectedHandler?.historical" type="warning" :show-icon="true">
          当前任务使用未注册的历史处理器。保存前请确认运行环境仍提供该处理器。
        </NAlert>
      </div>

      <div v-else-if="form.executeMode === 'BEAN'" class="technical-grid">
        <NFormItem label="服务 Bean" path="executorBean">
          <NInput
            :value="form.executorBean"
            placeholder="例如：inventoryCloseJob"
            @update:value="update('executorBean', $event)"
          />
        </NFormItem>
        <NFormItem label="执行方法" path="executorMethod">
          <NInput
            :value="form.executorMethod"
            placeholder="例如：execute"
            @update:value="update('executorMethod', $event)"
          />
        </NFormItem>
      </div>

      <div v-else class="technical-grid">
        <NFormItem label="远程服务" path="executorService">
          <NInput
            :value="form.executorService"
            placeholder="例如：inventory-service"
            @update:value="update('executorService', $event)"
          />
        </NFormItem>
        <NFormItem label="远程处理器" path="executorHandler">
          <NInput
            :value="form.executorHandler"
            placeholder="例如：inventoryCloseHandler"
            @update:value="update('executorHandler', $event)"
          />
        </NFormItem>
      </div>
    </div>

    <div v-else-if="form.invokeMode === JOB_INVOKE_FLOW" class="flow-execution">
      <div class="flow-grid">
        <NFormItem label="已发布流程模型" path="flowModelKey">
          <NSelect
            :value="form.flowModelKey || null"
            :options="flowModelOptions"
            :loading="flowModelLoading"
            filterable
            clearable
            placeholder="选择流程模型"
            @update:value="handleFlowModelChange"
          />
        </NFormItem>
        <NFormItem label="流程版本" path="flowModelVersion">
          <NSelect
            :value="form.flowModelVersion"
            :options="flowVersionOptions"
            :loading="flowVersionLoading"
            :disabled="!form.flowModelKey"
            placeholder="选择已发布版本"
            @update:value="handleFlowVersionChange"
          />
        </NFormItem>
      </div>

      <div v-if="selectedFlowModel" class="flow-model-summary">
        <div class="summary-icon">
          <i class="i-material-symbols:account-tree-outline-rounded" />
        </div>
        <div>
          <strong>{{ selectedFlowModel.label }}</strong>
          <span>{{ selectedFlowModel.value }}</span>
        </div>
      </div>

      <NDescriptions
        v-if="form.flowDeploymentId || form.flowProcessDefinitionId"
        class="flow-snapshot"
        :column="1"
        label-placement="left"
        bordered
        size="small"
      >
        <NDescriptionsItem label="部署 ID">
          {{ form.flowDeploymentId || '-' }}
        </NDescriptionsItem>
        <NDescriptionsItem label="流程定义 ID">
          {{ form.flowProcessDefinitionId || '-' }}
        </NDescriptionsItem>
      </NDescriptions>
    </div>

    <div class="parameter-toggle">
      <NCheckbox :checked="parameterEnabled" @update:checked="handleParameterToggle">
        配置任务参数
      </NCheckbox>
    </div>
    <NFormItem v-if="parameterEnabled" label="任务参数（JSON）" path="jobParam">
      <div class="parameter-editor">
        <NInput
          :value="form.jobParam"
          type="textarea"
          :rows="6"
          placeholder="例如：{&quot;warehouseId&quot;: 1001}"
          @update:value="update('jobParam', $event)"
        />
        <NButton size="small" secondary @click="formatParameter">
          <template #icon>
            <i class="i-material-symbols:data-object-rounded" />
          </template>
          格式化 JSON
        </NButton>
      </div>
    </NFormItem>
  </div>
</template>

<script setup>
import {
  NAlert,
  NButton,
  NCheckbox,
  NDescriptions,
  NDescriptionsItem,
  NFormItem,
  NInput,
  NRadioButton,
  NRadioGroup,
  NSelect,
} from 'naive-ui'
import { computed, ref, watch } from 'vue'
import {
  formatJobParameter,
  JOB_INVOKE_FLOW,
  JOB_INVOKE_SINGLE,
} from '../job-config-form'

const props = defineProps({
  form: {
    type: Object,
    required: true,
  },
  executors: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  invokeModeOptions: {
    type: Array,
    default: () => [],
  },
  flowModelOptions: {
    type: Array,
    default: () => [],
  },
  flowVersionOptions: {
    type: Array,
    default: () => [],
  },
  flowModelLoading: {
    type: Boolean,
    default: false,
  },
  flowVersionLoading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'updateField',
  'invokeModeChange',
  'flowModelChange',
  'flowVersionChange',
])
const parameterEnabled = ref(Boolean(props.form.jobParam))

const handlerOptions = computed(() => {
  const options = props.executors
    .filter(item => item.executeMode === 'HANDLER')
    .map(item => ({
      label: item.displayName || item.code,
      value: item.code,
      code: item.code,
      description: item.description || '本地任务处理器',
      group: item.group || 'DEFAULT',
    }))
  const current = props.form.executorHandler
  if (current && !options.some(item => item.value === current)) {
    options.unshift({
      label: `${current}（历史配置）`,
      value: current,
      code: current,
      description: '该处理器未出现在当前服务注册目录中',
      group: '历史配置',
      historical: true,
    })
  }
  return options
})

const selectedHandler = computed(() => handlerOptions.value.find(item => item.value === props.form.executorHandler))
const selectedFlowModel = computed(() => props.flowModelOptions
  .find(item => item.value === props.form.flowModelKey))

watch(() => props.form.jobParam, (value) => {
  if (value)
    parameterEnabled.value = true
})

function update(field, value) {
  emit('updateField', { field, value })
}

function handleInvokeModeChange(value) {
  emit('invokeModeChange', value)
}

function handleFlowModelChange(value) {
  emit('flowModelChange', value || '')
}

function handleFlowVersionChange(value) {
  emit('flowVersionChange', value ?? null)
}

function filterHandler(pattern, option) {
  const keyword = String(pattern || '').toLowerCase()
  return [option.label, option.code, option.description, option.group]
    .some(value => String(value || '').toLowerCase().includes(keyword))
}

function handleParameterToggle(checked) {
  parameterEnabled.value = checked
  if (!checked)
    update('jobParam', '')
}

function formatParameter() {
  try {
    update('jobParam', formatJobParameter(
      props.form.jobParam,
      props.form.invokeMode === JOB_INVOKE_FLOW,
    ))
    window.$message.success('JSON 格式正确')
  }
  catch (error) {
    window.$message.error(error.message)
  }
}
</script>

<style scoped>
.execution-section,
.single-execution,
.flow-execution,
.target-panel {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.technical-grid,
.flow-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 18px;
}

.handler-summary {
  margin: -2px 0 12px;
  padding: 12px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: color-mix(in srgb, var(--primary-color) 4%, var(--card-color));
}

.flow-model-summary {
  margin: -2px 0 12px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-left: 3px solid var(--success-color);
  background: color-mix(in srgb, var(--success-color) 5%, var(--card-color));
}

.flow-model-summary > div:last-child {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.flow-model-summary strong {
  color: var(--text-color-1);
  font-size: 13px;
}

.flow-model-summary span {
  overflow: hidden;
  color: var(--text-color-3);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flow-snapshot {
  margin-bottom: 12px;
}

.flow-snapshot :deep(.n-descriptions-table-content) {
  overflow-wrap: anywhere;
}

.summary-icon {
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  color: var(--primary-color);
  font-size: 18px;
}

.handler-summary strong,
.handler-summary p {
  margin: 0;
}

.handler-summary strong {
  color: var(--text-color-1);
  font-size: 13px;
}

.handler-summary p,
.handler-summary span {
  color: var(--text-color-3);
  font-size: 12px;
}

.handler-summary p {
  margin-top: 3px;
}

.parameter-toggle {
  margin: 4px 0 10px;
  padding-top: 12px;
  border-top: 1px solid var(--divider-color);
}

.parameter-editor {
  width: 100%;
  display: flex;
  align-items: flex-end;
  flex-direction: column;
  gap: 8px;
}

@media (max-width: 680px) {
  .technical-grid {
    grid-template-columns: 1fr;
  }

  .flow-grid {
    grid-template-columns: 1fr;
  }
}
</style>
