<template>
  <div class="cron-builder">
    <NRadioGroup v-model:value="mode" size="small" name="cron-mode">
      <NRadioButton value="simple">
        简单设置
      </NRadioButton>
      <NRadioButton value="expert">
        Cron 专家模式
      </NRadioButton>
    </NRadioGroup>

    <div v-if="mode === 'simple'" class="simple-controls">
      <div class="control-field frequency-field">
        <label>执行频率</label>
        <NSelect
          v-model:value="schedule.type"
          :options="SIMPLE_SCHEDULE_TYPES"
          @update:value="emitSimpleCron"
        />
      </div>

      <div v-if="schedule.type === 'interval'" class="control-field value-field">
        <label>间隔分钟</label>
        <NInputNumber
          v-model:value="schedule.intervalMinutes"
          :min="1"
          :max="59"
          @update:value="emitSimpleCron"
        >
          <template #suffix>
            分钟
          </template>
        </NInputNumber>
      </div>

      <div v-else-if="schedule.type === 'hourly'" class="control-field value-field">
        <label>第几分钟</label>
        <NInputNumber
          v-model:value="schedule.minute"
          :min="0"
          :max="59"
          @update:value="emitSimpleCron"
        >
          <template #suffix>
            分
          </template>
        </NInputNumber>
      </div>

      <div v-else-if="schedule.type === 'weekly'" class="control-field value-field">
        <label>星期</label>
        <NSelect
          v-model:value="schedule.weekday"
          :options="WEEKDAY_OPTIONS"
          @update:value="emitSimpleCron"
        />
      </div>

      <div v-else-if="schedule.type === 'monthly'" class="control-field value-field">
        <label>每月日期</label>
        <NInputNumber
          v-model:value="schedule.dayOfMonth"
          :min="1"
          :max="31"
          @update:value="emitSimpleCron"
        >
          <template #suffix>
            日
          </template>
        </NInputNumber>
      </div>

      <div v-if="['daily', 'weekly', 'monthly'].includes(schedule.type)" class="control-field time-field">
        <label>执行时间</label>
        <NTimePicker
          :formatted-value="schedule.time"
          format="HH:mm"
          value-format="HH:mm"
          @update:formatted-value="handleTimeChange"
        />
      </div>
    </div>

    <div v-else class="expert-control">
      <label>Cron 表达式</label>
      <NInput
        :value="modelValue"
        placeholder="Quartz 6 段表达式，例如 0 0 2 * * ?"
        @update:value="emit('update:modelValue', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { NInput, NInputNumber, NRadioButton, NRadioGroup, NSelect, NTimePicker } from 'naive-ui'
import { reactive, ref, watch } from 'vue'
import {
  buildCronExpression,
  createDefaultSimpleSchedule,
  parseCronExpression,
  SIMPLE_SCHEDULE_TYPES,
  WEEKDAY_OPTIONS,
} from './cron-builder'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue', 'modeChange'])
const mode = ref('simple')
const schedule = reactive(createDefaultSimpleSchedule())
let internalExpression = ''

watch(() => props.modelValue, (value) => {
  if (value === internalExpression) {
    internalExpression = ''
    return
  }
  const parsed = parseCronExpression(value)
  if (parsed) {
    Object.assign(schedule, parsed)
    mode.value = 'simple'
  }
  else if (value) {
    mode.value = 'expert'
  }
}, { immediate: true })

watch(mode, (value) => {
  emit('modeChange', value)
  if (value === 'simple')
    emitSimpleCron()
})

function emitSimpleCron() {
  const expression = buildCronExpression(schedule)
  internalExpression = expression
  emit('update:modelValue', expression)
}

function handleTimeChange(value) {
  if (!value)
    return
  schedule.time = value
  emitSimpleCron()
}
</script>

<style scoped>
.cron-builder {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.simple-controls {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(150px, 0.8fr) minmax(160px, 0.8fr);
  gap: 14px;
  align-items: end;
}

.control-field,
.expert-control {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

label {
  color: var(--text-color-2);
  font-size: 13px;
  font-weight: 600;
}

.control-field :deep(.n-input-number),
.control-field :deep(.n-time-picker) {
  width: 100%;
}

@media (max-width: 900px) {
  .simple-controls {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 560px) {
  .simple-controls {
    grid-template-columns: 1fr;
  }
}
</style>
