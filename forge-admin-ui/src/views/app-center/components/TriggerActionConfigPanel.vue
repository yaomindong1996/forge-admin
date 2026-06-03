<template>
  <div class="trigger-action-config">
    <template v-if="actionType === 'START_FLOW'">
      <n-alert type="info" :bordered="false">
        使用“流程与自动化”中配置的主流程。触发条件满足后自动发起，不需要在这里再次选择流程。
      </n-alert>
    </template>

    <template v-else-if="actionType === 'SEND_MESSAGE'">
      <n-form-item label="消息模板">
        <n-input v-model:value="config.templateCode" placeholder="消息模板编码" @update:value="emitConfig" />
      </n-form-item>
      <n-form-item label="接收人规则">
        <n-select
          v-model:value="config.receiverRule"
          :options="receiverRuleOptions"
          clearable
          placeholder="选择接收人规则"
          @update:value="emitConfig"
        />
      </n-form-item>
      <n-form-item v-if="config.receiverRule === 'USERS'" label="指定用户ID">
        <n-input v-model:value="config.receiverIds" placeholder="多个用逗号分隔" @update:value="emitConfig" />
      </n-form-item>
    </template>

    <template v-else-if="actionType === 'CREATE_RECORD'">
      <n-form-item label="目标对象">
        <n-select
          v-model:value="config.targetObjectCode"
          :options="objectOptions"
          clearable
          filterable
          placeholder="选择目标业务对象"
          @update:value="emitConfig"
        />
      </n-form-item>
      <n-form-item label="字段映射">
        <div class="mapping-list">
          <div v-for="(item, index) in config.fieldMapping" :key="item.clientKey" class="mapping-row">
            <n-select
              v-model:value="item.sourceField"
              :options="fieldOptions"
              clearable
              filterable
              placeholder="源字段"
              @update:value="emitConfig"
            />
            <span>→</span>
            <n-input v-model:value="item.targetField" placeholder="目标字段" @update:value="emitConfig" />
            <n-button quaternary circle size="small" @click="removeFieldMapping(index)">
              <template #icon>
                <n-icon><TrashOutline /></n-icon>
              </template>
            </n-button>
          </div>
          <n-button dashed size="small" @click="addFieldMapping">
            添加字段映射
          </n-button>
        </div>
      </n-form-item>
    </template>

    <template v-else-if="actionType === 'UPDATE_FIELD'">
      <n-form-item label="更新字段">
        <n-select
          v-model:value="config.targetField"
          :options="fieldOptions"
          clearable
          filterable
          placeholder="选择字段"
          @update:value="emitConfig"
        />
      </n-form-item>
      <n-form-item label="更新值">
        <n-input v-model:value="config.valueTemplate" placeholder="固定值或 ${fieldCode}" @update:value="emitConfig" />
      </n-form-item>
    </template>

    <template v-else-if="actionType === 'WEBHOOK'">
      <n-alert type="warning" :bordered="false">
        Webhook 本阶段只记录 TODO 执行日志，不发起外部请求。
      </n-alert>
      <n-form-item label="通道引用">
        <n-input v-model:value="config.channelConfigRef" placeholder="通道配置引用" @update:value="emitConfig" />
      </n-form-item>
      <n-form-item label="事件名称">
        <n-input v-model:value="config.eventName" placeholder="例如：opportunity.created" @update:value="emitConfig" />
      </n-form-item>
    </template>

    <n-empty v-else description="请选择动作类型" />
  </div>
</template>

<script setup>
import { TrashOutline } from '@vicons/ionicons5'
import { reactive, watch } from 'vue'

const props = defineProps({
  actionType: {
    type: String,
    default: '',
  },
  modelValue: {
    type: String,
    default: '',
  },
  fieldOptions: {
    type: Array,
    default: () => [],
  },
  objectOptions: {
    type: Array,
    default: () => [],
  },
  receiverRuleOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])

const config = reactive(normalizeConfig(props.actionType, props.modelValue))

watch(() => [props.actionType, props.modelValue], ([actionType, value]) => {
  const next = normalizeConfig(actionType, value)
  if (JSON.stringify(configPayload(config, actionType)) !== JSON.stringify(configPayload(next, actionType)))
    Object.assign(config, next)
}, { deep: true })

function addFieldMapping() {
  config.fieldMapping.push(createMapping({ sourceField: null, targetField: '' }))
  emitConfig()
}

function removeFieldMapping(index) {
  config.fieldMapping.splice(index, 1)
  emitConfig()
}

function emitConfig() {
  emit('update:modelValue', JSON.stringify(configPayload(config, props.actionType)))
}

function normalizeConfig(actionType, value) {
  const source = safeParse(value)
  return {
    useMainFlow: source.useMainFlow !== false,
    templateCode: source.templateCode || '',
    receiverRule: source.receiverRule || null,
    receiverIds: source.receiverIds || '',
    targetObjectCode: source.targetObjectCode || null,
    fieldMapping: normalizeFieldMapping(source.fieldMapping || []),
    targetField: source.targetField || '',
    valueTemplate: source.valueTemplate ?? source.value ?? '',
    channelConfigRef: source.channelConfigRef || '',
    eventName: source.eventName || '',
    actionType,
  }
}

function configPayload(value, actionType) {
  if (actionType === 'START_FLOW') {
    return {
      useMainFlow: true,
    }
  }
  if (actionType === 'SEND_MESSAGE') {
    return {
      templateCode: value.templateCode || '',
      receiverRule: value.receiverRule || '',
      receiverIds: value.receiverIds || '',
    }
  }
  if (actionType === 'CREATE_RECORD') {
    return {
      targetObjectCode: value.targetObjectCode || '',
      fieldMapping: (value.fieldMapping || [])
        .map(item => ({
          sourceField: item.sourceField || '',
          targetField: item.targetField || '',
        }))
        .filter(item => item.sourceField && item.targetField),
    }
  }
  if (actionType === 'UPDATE_FIELD') {
    return {
      targetField: value.targetField || '',
      valueTemplate: value.valueTemplate ?? '',
    }
  }
  if (actionType === 'WEBHOOK') {
    return {
      channelConfigRef: value.channelConfigRef || '',
      eventName: value.eventName || '',
      todo: true,
    }
  }
  return safeParse(props.modelValue)
}

function normalizeFieldMapping(list = []) {
  return list.map(item => createMapping({
    sourceField: item.sourceField || item.formField || item.field || null,
    targetField: item.targetField || '',
  }))
}

function createMapping(values = {}) {
  return {
    clientKey: `mapping_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    ...values,
  }
}

function safeParse(value) {
  if (!value)
    return {}
  try {
    return JSON.parse(value)
  }
  catch {
    return {}
  }
}
</script>

<style scoped>
.trigger-action-config {
  display: block;
  width: 100%;
}

.mapping-list {
  display: grid;
  gap: 8px;
  width: 100%;
}

.mapping-row {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) 24px minmax(140px, 1fr) 32px;
  gap: 8px;
  align-items: center;
}

.mapping-row span {
  color: #64748b;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 720px) {
  .mapping-row {
    grid-template-columns: 1fr;
  }
}
</style>
