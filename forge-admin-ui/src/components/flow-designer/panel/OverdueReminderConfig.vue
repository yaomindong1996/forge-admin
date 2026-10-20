<script setup>
import { computed, onMounted, ref } from 'vue'
import messageApi from '@/api/message'
import { useDict } from '@/composables/useDict'

const props = defineProps({
  config: { type: Object, required: true },
  readonly: Boolean,
})

const emit = defineEmits(['update:config'])

const { dict, loading: dictLoading } = useDict('sys_message_channel', 'sys_flow_overdue_repeat_mode')

const templateOptions = ref([])
const templateLoading = ref(false)
const templateLoaded = ref(false)

const enabled = useField('overdueReminderEnabled', false)
const templateCode = useField('overdueReminderTemplateCode', 'FLOW_TASK_OVERDUE')
const channels = useArrayField('overdueReminderChannels', ['WEB'])
const repeatMode = useField('overdueReminderRepeatMode', 'once')
const intervalMinutes = useNumberField('overdueReminderIntervalMinutes', 1440)
const maxTimes = useNumberField('overdueReminderMaxTimes', 1)

const dueDateDays = computed({
  get: () => normalizeNumber(props.config?.dueDateDays ?? props.config?.dueDate, 0),
  set: value => emit('update:config', {
    dueDateDays: normalizeNumber(value, 0),
    dueDate: normalizeNumber(value, 0),
  }),
})

const dueDateHours = useNumberField('dueDateHours', 0)

const channelOptions = computed(() => normalizeDictOptions(dict.value.sys_message_channel))
const repeatModeOptions = computed(() => {
  const options = normalizeDictOptions(dict.value.sys_flow_overdue_repeat_mode)
  return options.length
    ? options
    : [
        { label: '仅一次', value: 'once' },
        { label: '按间隔重复', value: 'interval' },
      ]
})
const mergedTemplateOptions = computed(() => mergeTemplateOptions(templateOptions.value, templateCode.value))
const reminderFieldsDisabled = computed(() => props.readonly || !enabled.value)
const isIntervalRepeat = computed(() => repeatMode.value === 'interval')

onMounted(() => {
  ensureTemplatesLoaded()
})

function useField(name, fallback) {
  return computed({
    get: () => props.config?.[name] ?? fallback,
    set: value => emit('update:config', { [name]: value }),
  })
}

function useArrayField(name, fallback) {
  return computed({
    get: () => {
      const value = props.config?.[name]
      if (Array.isArray(value))
        return value
      if (value == null || value === '')
        return fallback
      return String(value).split(',').map(item => item.trim()).filter(Boolean)
    },
    set: value => emit('update:config', { [name]: Array.isArray(value) ? value : [] }),
  })
}

function useNumberField(name, fallback) {
  return computed({
    get: () => normalizeNumber(props.config?.[name], fallback),
    set: value => emit('update:config', { [name]: normalizeNumber(value, fallback) }),
  })
}

async function ensureTemplatesLoaded() {
  if (templateLoaded.value || templateLoading.value)
    return
  await loadTemplates()
}

async function loadTemplates(keyword = '') {
  templateLoading.value = true
  try {
    const res = await messageApi.getTemplatePage({
      pageNum: 1,
      pageSize: 100,
      type: 'SYSTEM',
      keyword: keyword || undefined,
    })
    const records = resolveRecords(res.data)
    templateOptions.value = records
      .filter(item => item?.enabled !== 0)
      .map(normalizeTemplateOption)
      .filter(Boolean)
    templateLoaded.value = true
  }
  catch (error) {
    console.error('加载消息模板失败', error)
  }
  finally {
    templateLoading.value = false
  }
}

function handleRepeatModeChange(value) {
  const patch = { overdueReminderRepeatMode: value || 'once' }
  if (patch.overdueReminderRepeatMode === 'once')
    patch.overdueReminderMaxTimes = 1
  emit('update:config', patch)
}

function normalizeDictOptions(items = []) {
  return (items || [])
    .filter(item => item?.status === undefined || String(item.status) === '1')
    .map(item => ({
      label: item.label,
      value: item.value,
    }))
    .filter(item => item.label && item.value)
}

function resolveRecords(data) {
  if (Array.isArray(data))
    return data
  return data?.records || data?.list || []
}

function normalizeTemplateOption(item) {
  const value = item?.templateCode
  if (!value)
    return null
  return {
    label: item.templateName ? `${item.templateName}（${value}）` : value,
    value,
  }
}

function mergeTemplateOptions(options, currentValue) {
  const map = new Map()
  for (const option of options || []) {
    if (option?.value)
      map.set(option.value, option)
  }
  if (currentValue && !map.has(currentValue))
    map.set(currentValue, { label: currentValue, value: currentValue })
  return Array.from(map.values())
}

function normalizeNumber(value, fallback) {
  const n = Number.parseInt(value, 10)
  return Number.isFinite(n) && n >= 0 ? n : fallback
}
</script>

<template>
  <div class="overdue-reminder-config">
    <n-form label-placement="top" :show-feedback="false">
      <div class="config-section-block">
        <div class="config-section-title">
          处理时限
        </div>
        <div class="duration-grid">
          <n-form-item label="天数" :show-feedback="false">
            <n-input-number
              v-model:value="dueDateDays"
              :min="0"
              :max="365"
              :show-button="false"
              :disabled="readonly"
              class="duration-input"
            >
              <template #suffix>
                天
              </template>
            </n-input-number>
          </n-form-item>
          <n-form-item label="小时" :show-feedback="false">
            <n-input-number
              v-model:value="dueDateHours"
              :min="0"
              :max="23"
              :show-button="false"
              :disabled="readonly"
              class="duration-input"
            >
              <template #suffix>
                小时
              </template>
            </n-input-number>
          </n-form-item>
        </div>
      </div>

      <div class="config-section-block">
        <div class="switch-row">
          <div class="config-section-title">
            逾期提醒
          </div>
          <n-switch v-model:value="enabled" :disabled="readonly" />
        </div>

        <n-form-item label="消息模板">
          <n-select
            v-model:value="templateCode"
            :options="mergedTemplateOptions"
            :loading="templateLoading"
            :disabled="reminderFieldsDisabled"
            placeholder="请选择消息模板"
            filterable
            remote
            clearable
            @focus="ensureTemplatesLoaded"
            @search="loadTemplates"
          />
        </n-form-item>

        <n-form-item label="推送方式">
          <n-select
            v-model:value="channels"
            :options="channelOptions"
            :loading="dictLoading"
            :disabled="reminderFieldsDisabled"
            placeholder="请选择推送方式"
            multiple
            clearable
          />
        </n-form-item>

        <n-form-item label="重复策略">
          <n-select
            :value="repeatMode"
            :options="repeatModeOptions"
            :loading="dictLoading"
            :disabled="reminderFieldsDisabled"
            placeholder="请选择重复策略"
            @update:value="handleRepeatModeChange"
          />
        </n-form-item>

        <div v-if="isIntervalRepeat" class="time-grid">
          <n-form-item label="提醒间隔（分钟）">
            <n-input-number
              v-model:value="intervalMinutes"
              :min="30"
              :max="10080"
              :disabled="reminderFieldsDisabled"
              class="w-full"
            />
          </n-form-item>
          <n-form-item label="最大次数">
            <n-input-number
              v-model:value="maxTimes"
              :min="1"
              :max="30"
              :disabled="reminderFieldsDisabled"
              class="w-full"
            />
          </n-form-item>
        </div>
      </div>
    </n-form>
  </div>
</template>

<style scoped>
.overdue-reminder-config {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.overdue-reminder-config :deep(.n-form-item) {
  margin-bottom: 12px;
}

.duration-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  align-items: start;
  gap: 12px;
}

.duration-input {
  width: 100%;
  min-width: 0;
}

.time-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  align-items: start;
  gap: 12px;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

@media (max-width: 520px) {
  .duration-grid,
  .time-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
