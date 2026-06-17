<script setup>
/**
 * ListenerConfig — 任务监听器 + 执行监听器
 *
 * 字段：taskListeners[] / executionListeners[]，每项 { event, type, value }
 *   - type: class / expression / delegateExpression
 */
import { computed } from 'vue'

const props = defineProps({
  config: { type: Object, required: true },
  readonly: Boolean,
})

const emit = defineEmits(['update:config'])

const TASK_EVENTS = [
  { label: 'create（创建）', value: 'create' },
  { label: 'assignment（分配）', value: 'assignment' },
  { label: 'complete（完成）', value: 'complete' },
  { label: 'delete（删除）', value: 'delete' },
]
const EXEC_EVENTS = [
  { label: 'start（开始）', value: 'start' },
  { label: 'end（结束）', value: 'end' },
  { label: 'take（流转）', value: 'take' },
]
const TYPES = [
  { label: 'Java 类', value: 'class' },
  { label: '表达式', value: 'expression' },
  { label: '代理表达式', value: 'delegateExpression' },
]

const taskListeners = computed({
  get: () => Array.isArray(props.config?.taskListeners) ? props.config.taskListeners : [],
  set: v => emit('update:config', { taskListeners: v }),
})
const executionListeners = computed({
  get: () => Array.isArray(props.config?.executionListeners) ? props.config.executionListeners : [],
  set: v => emit('update:config', { executionListeners: v }),
})

function addTask() {
  taskListeners.value = [...taskListeners.value, { event: 'create', type: 'class', value: '' }]
}
function removeTask(i) {
  const arr = [...taskListeners.value]
  arr.splice(i, 1)
  taskListeners.value = arr
}
function addExec() {
  executionListeners.value = [...executionListeners.value, { event: 'start', type: 'class', value: '' }]
}
function removeExec(i) {
  const arr = [...executionListeners.value]
  arr.splice(i, 1)
  executionListeners.value = arr
}
function updateItem(list, i, patch) {
  const arr = [...list]
  arr[i] = { ...arr[i], ...patch }
  return arr
}
</script>

<template>
  <div class="space-y-4">
    <div>
      <div class="mb-2 flex items-center justify-between">
        <span class="text-sm font-medium">任务监听器</span>
        <n-button v-if="!readonly" size="tiny" @click="addTask">
          + 新增
        </n-button>
      </div>
      <div v-if="taskListeners.length === 0" class="text-xs rounded bg-gray-50 px-3 py-2 text-gray-400">
        暂无任务监听器
      </div>
      <div v-else class="space-y-2">
        <div
          v-for="(l, i) in taskListeners"
          :key="i"
          class="flex items-center gap-2 border border-gray-100 rounded p-2"
        >
          <n-select
            :value="l.event"
            :options="TASK_EVENTS"
            class="!w-32"
            :disabled="readonly"
            @update:value="taskListeners = updateItem(taskListeners, i, { event: $event })"
          />
          <n-select
            :value="l.type"
            :options="TYPES"
            class="!w-28"
            :disabled="readonly"
            @update:value="taskListeners = updateItem(taskListeners, i, { type: $event })"
          />
          <n-input
            :value="l.value"
            placeholder="实现值"
            :disabled="readonly"
            @update:value="taskListeners = updateItem(taskListeners, i, { value: $event })"
          />
          <n-button v-if="!readonly" size="tiny" type="error" @click="removeTask(i)">
            删除
          </n-button>
        </div>
      </div>
    </div>

    <n-divider />

    <div>
      <div class="mb-2 flex items-center justify-between">
        <span class="text-sm font-medium">执行监听器</span>
        <n-button v-if="!readonly" size="tiny" @click="addExec">
          + 新增
        </n-button>
      </div>
      <div v-if="executionListeners.length === 0" class="text-xs rounded bg-gray-50 px-3 py-2 text-gray-400">
        暂无执行监听器
      </div>
      <div v-else class="space-y-2">
        <div
          v-for="(l, i) in executionListeners"
          :key="i"
          class="flex items-center gap-2 border border-gray-100 rounded p-2"
        >
          <n-select
            :value="l.event"
            :options="EXEC_EVENTS"
            class="!w-32"
            :disabled="readonly"
            @update:value="executionListeners = updateItem(executionListeners, i, { event: $event })"
          />
          <n-select
            :value="l.type"
            :options="TYPES"
            class="!w-28"
            :disabled="readonly"
            @update:value="executionListeners = updateItem(executionListeners, i, { type: $event })"
          />
          <n-input
            :value="l.value"
            placeholder="实现值"
            :disabled="readonly"
            @update:value="executionListeners = updateItem(executionListeners, i, { value: $event })"
          />
          <n-button v-if="!readonly" size="tiny" type="error" @click="removeExec(i)">
            删除
          </n-button>
        </div>
      </div>
    </div>
  </div>
</template>
