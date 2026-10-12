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
const DOLLAR = '$'

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
function updateTask(i, patch) {
  taskListeners.value = updateItem(taskListeners.value, i, patch)
}
function updateExec(i, patch) {
  executionListeners.value = updateItem(executionListeners.value, i, patch)
}
function getValueLabel(type) {
  const map = {
    class: 'Java 类名',
    expression: '表达式',
    delegateExpression: '代理表达式',
  }
  return map[type] || '实现值'
}
function getValuePlaceholder(type) {
  const map = {
    class: 'com.example.workflow.ApprovalTaskListener',
    expression: `${DOLLAR}{approvalListener.onComplete(task)}`,
    delegateExpression: `${DOLLAR}{approvalTaskListener}`,
  }
  return map[type] || '请输入实现值'
}
</script>

<template>
  <div class="listener-config">
    <section class="listener-section">
      <div class="listener-section-header">
        <span class="listener-section-title">任务监听器</span>
        <n-button
          v-if="!readonly"
          size="small"
          quaternary
          circle
          title="新增任务监听器"
          @click="addTask"
        >
          <template #icon>
            <i class="i-material-symbols:add-rounded" />
          </template>
        </n-button>
      </div>
      <div v-if="taskListeners.length === 0" class="listener-empty">
        暂无任务监听器
      </div>
      <div v-else class="listener-list">
        <div
          v-for="(l, i) in taskListeners"
          :key="i"
          class="listener-card"
        >
          <div class="listener-card-header">
            <span class="listener-index">任务 {{ i + 1 }}</span>
            <n-button
              v-if="!readonly"
              size="small"
              type="error"
              text
              title="删除任务监听器"
              @click="removeTask(i)"
            >
              <template #icon>
                <i class="i-material-symbols:delete-outline-rounded" />
              </template>
            </n-button>
          </div>
          <div class="listener-grid">
            <n-form-item label="触发事件" label-placement="top" :show-feedback="false">
              <n-select
                :value="l.event"
                :options="TASK_EVENTS"
                :disabled="readonly"
                @update:value="updateTask(i, { event: $event })"
              />
            </n-form-item>
            <n-form-item label="实现类型" label-placement="top" :show-feedback="false">
              <n-select
                :value="l.type"
                :options="TYPES"
                :disabled="readonly"
                @update:value="updateTask(i, { type: $event })"
              />
            </n-form-item>
          </div>
          <n-form-item :label="getValueLabel(l.type)" label-placement="top" :show-feedback="false">
            <n-input
              :value="l.value"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 5 }"
              :placeholder="getValuePlaceholder(l.type)"
              :disabled="readonly"
              @update:value="updateTask(i, { value: $event })"
            />
          </n-form-item>
        </div>
      </div>
    </section>

    <n-divider />

    <section class="listener-section">
      <div class="listener-section-header">
        <span class="listener-section-title">执行监听器</span>
        <n-button
          v-if="!readonly"
          size="small"
          quaternary
          circle
          title="新增执行监听器"
          @click="addExec"
        >
          <template #icon>
            <i class="i-material-symbols:add-rounded" />
          </template>
        </n-button>
      </div>
      <div v-if="executionListeners.length === 0" class="listener-empty">
        暂无执行监听器
      </div>
      <div v-else class="listener-list">
        <div
          v-for="(l, i) in executionListeners"
          :key="i"
          class="listener-card"
        >
          <div class="listener-card-header">
            <span class="listener-index">执行 {{ i + 1 }}</span>
            <n-button
              v-if="!readonly"
              size="small"
              type="error"
              text
              title="删除执行监听器"
              @click="removeExec(i)"
            >
              <template #icon>
                <i class="i-material-symbols:delete-outline-rounded" />
              </template>
            </n-button>
          </div>
          <div class="listener-grid">
            <n-form-item label="触发事件" label-placement="top" :show-feedback="false">
              <n-select
                :value="l.event"
                :options="EXEC_EVENTS"
                :disabled="readonly"
                @update:value="updateExec(i, { event: $event })"
              />
            </n-form-item>
            <n-form-item label="实现类型" label-placement="top" :show-feedback="false">
              <n-select
                :value="l.type"
                :options="TYPES"
                :disabled="readonly"
                @update:value="updateExec(i, { type: $event })"
              />
            </n-form-item>
          </div>
          <n-form-item :label="getValueLabel(l.type)" label-placement="top" :show-feedback="false">
            <n-input
              :value="l.value"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 5 }"
              :placeholder="getValuePlaceholder(l.type)"
              :disabled="readonly"
              @update:value="updateExec(i, { value: $event })"
            />
          </n-form-item>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.listener-config {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.listener-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.listener-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
}

.listener-section-title {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
}

.listener-empty {
  border: 1px dashed #dbe3ef;
  border-radius: 8px;
  padding: 14px;
  background: var(--bg-secondary);
  color: var(--text-tertiary);
  font-size: 12px;
  text-align: center;
}

.listener-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.listener-card {
  border: 1px solid #e5eaf3;
  border-radius: 8px;
  padding: 12px;
  background: var(--bg-primary);
}

.listener-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.listener-index {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.listener-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}

.listener-card :deep(.n-form-item) {
  margin-bottom: 10px;
}

.listener-card :deep(.n-form-item:last-child) {
  margin-bottom: 0;
}
</style>
