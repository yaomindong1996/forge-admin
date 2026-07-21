<template>
  <div class="schedule-section">
    <div class="schedule-toolbar">
      <NFormItem label="调度方式" path="scheduleType">
        <NRadioGroup
          class="schedule-mode"
          :value="form.scheduleType"
          @update:value="handleScheduleTypeChange"
        >
          <NRadioButton
            v-for="option in scheduleTypeOptions"
            :key="option.value"
            :value="option.value"
          >
            <span class="mode-option">
              <i :class="resolveScheduleTypeIcon(option.value)" />
              {{ option.label }}
            </span>
          </NRadioButton>
        </NRadioGroup>
      </NFormItem>

      <NFormItem label="任务时区" path="timezone">
        <NSelect
          :value="form.timezone"
          :options="timezoneOptions"
          :loading="timezoneLoading"
          filterable
          :clearable="false"
          placeholder="搜索并选择 IANA 时区"
          @update:value="handleTimezoneChange"
        />
      </NFormItem>
    </div>

    <div v-if="form.scheduleType === JOB_SCHEDULE_CRON" class="schedule-config">
      <NFormItem label="周期计划" path="cronExpression">
        <CronBuilder
          :model-value="form.cronExpression"
          @update:model-value="handleCronChange"
        />
      </NFormItem>
    </div>

    <div v-else class="schedule-config once-config">
      <NFormItem label="执行时间" path="fireOnceTime">
        <NDatePicker
          class="once-time-picker"
          :value="form.fireOnceTime"
          type="datetime"
          format="yyyy-MM-dd HH:mm:ss"
          :actions="['confirm']"
          clearable
          placeholder="选择执行日期和时间"
          @update:value="handleOnceTimeChange"
        />
      </NFormItem>
      <span class="field-hint">该时间按 {{ form.timezone || '所选任务时区' }} 解释，计划触发一次后自动结束</span>
    </div>

    <JobSchedulePreview
      :schedule-type="form.scheduleType"
      :timezone="form.timezone"
      :description="preview.description"
      :next-fire-times="preview.nextFireTimes"
      :loading="previewLoading"
      :error="previewError"
    />
  </div>
</template>

<script setup>
import { NDatePicker, NFormItem, NRadioButton, NRadioGroup, NSelect } from 'naive-ui'
import { onBeforeUnmount, reactive, ref, watch } from 'vue'
import { previewJobCron } from '@/api/system/job'
import CronBuilder from '@/components/job/CronBuilder.vue'
import { isOnceTimeInFuture, JOB_SCHEDULE_CRON, JOB_SCHEDULE_ONCE } from '../job-config-form'
import JobSchedulePreview from './JobSchedulePreview.vue'

const props = defineProps({
  form: {
    type: Object,
    required: true,
  },
  scheduleTypeOptions: {
    type: Array,
    default: () => [],
  },
  timezoneOptions: {
    type: Array,
    default: () => [],
  },
  timezoneLoading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['updateField', 'preview'])
const preview = reactive({ description: '', nextFireTimes: [] })
const previewLoading = ref(false)
const previewError = ref('')
let previewTimer
let previewSequence = 0

watch(
  () => [
    props.form.scheduleType,
    props.form.cronExpression,
    props.form.fireOnceTime,
    props.form.timezone,
  ],
  schedulePreview,
  { immediate: true },
)

onBeforeUnmount(() => clearTimeout(previewTimer))

function handleCronChange(value) {
  emit('updateField', { field: 'cronExpression', value })
}

function resolveScheduleTypeIcon(value) {
  return value === JOB_SCHEDULE_ONCE
    ? 'i-material-symbols:event-outline-rounded'
    : 'i-material-symbols:event-repeat-outline-rounded'
}

function handleScheduleTypeChange(value) {
  emit('updateField', { field: 'scheduleType', value })
}

function handleTimezoneChange(value) {
  emit('updateField', { field: 'timezone', value })
}

function handleOnceTimeChange(value) {
  emit('updateField', { field: 'fireOnceTime', value })
}

function schedulePreview([scheduleType, expression, fireOnceTime, timezone]) {
  clearTimeout(previewTimer)
  ++previewSequence
  previewLoading.value = false
  previewError.value = ''
  if (scheduleType === JOB_SCHEDULE_ONCE) {
    preview.description = fireOnceTime ? '仅执行一次，计划触发后自动结束' : '请选择一次性执行时间'
    preview.nextFireTimes = fireOnceTime ? [fireOnceTime] : []
    if (fireOnceTime && !isOnceTimeInFuture(fireOnceTime, timezone)) {
      preview.description = ''
      preview.nextFireTimes = []
      previewError.value = `执行时间必须晚于当前时刻（${timezone || '任务时区'}）`
    }
    emitPreview()
    return
  }
  if (scheduleType !== JOB_SCHEDULE_CRON || !expression || !timezone) {
    preview.description = '请选择周期计划和任务时区'
    preview.nextFireTimes = []
    emitPreview()
    return
  }
  previewTimer = setTimeout(() => loadPreview(expression, timezone), 260)
}

async function loadPreview(expression, timezone) {
  const sequence = ++previewSequence
  previewLoading.value = true
  try {
    const response = await previewJobCron(expression, timezone)
    if (sequence !== previewSequence)
      return
    preview.description = response.data?.description || ''
    preview.nextFireTimes = response.data?.nextFireTimes || []
    previewError.value = ''
  }
  catch (error) {
    if (sequence !== previewSequence)
      return
    preview.description = ''
    preview.nextFireTimes = []
    previewError.value = resolveErrorMessage(error)
  }
  finally {
    if (sequence === previewSequence) {
      previewLoading.value = false
      emitPreview()
    }
  }
}

function emitPreview() {
  emit('preview', {
    description: preview.description,
    nextFireTimes: [...preview.nextFireTimes],
    error: previewError.value,
  })
}

function resolveErrorMessage(error) {
  return error?.response?.data?.msg
    || error?.response?.data?.message
    || error?.message
    || '执行计划校验失败'
}
</script>

<style scoped>
.schedule-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.schedule-toolbar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(280px, 1fr);
  gap: 16px;
}

.schedule-mode {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.schedule-mode :deep(.n-radio-button) {
  min-width: 0;
  text-align: center;
}

.mode-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.mode-option i {
  font-size: 16px;
}

.schedule-config {
  min-width: 0;
}

.once-config {
  padding-bottom: 12px;
}

.once-time-picker {
  width: 100%;
}

.field-hint {
  display: block;
  margin-top: -14px;
  color: var(--text-color-3);
  font-size: 12px;
}

@media (max-width: 720px) {
  .schedule-toolbar {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
