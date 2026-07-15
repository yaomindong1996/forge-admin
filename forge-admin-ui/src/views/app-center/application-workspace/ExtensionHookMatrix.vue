<template>
  <div class="hook-matrix">
    <div class="hook-matrix-heading">
      <div>
        <strong>触发时机</strong>
        <span>选择业务动作及执行前后位置，同一条增强只绑定一个触发点。</span>
      </div>
      <div v-if="selectedHook" class="selected-hook">
        <DictTag v-if="hookDict.length" :options="hookDict" :value="selectedHook" :bordered="false" />
        <span v-else class="hook-fallback-tag">{{ hookLabel(selectedHook) }}</span>
        <small>{{ selectedHookDescription }}</small>
      </div>
    </div>

    <section v-for="group in hookGroups" :key="group.key" class="hook-group">
      <header>
        <strong>{{ group.title }}</strong>
        <span>{{ group.description }}</span>
      </header>
      <div class="hook-group-rows">
        <div v-for="row in group.rows" :key="row.key" class="hook-row">
          <span class="hook-operation">{{ row.label }}</span>
          <div class="hook-options">
            <button
              v-for="hook in row.hooks"
              :key="hook.value"
              type="button"
              class="hook-option"
              :class="{
                active: modelValue === hook.value,
                unavailable: !isAllowed(hook.value),
              }"
              :disabled="disabled || !isAllowed(hook.value)"
              :title="isAllowed(hook.value) ? hookDescription(hook.value) : '当前增强类型或 Java 处理器不支持该触发点'"
              @click="selectHook(hook.value)"
            >
              <span>{{ hookLabel(hook.value) }}</span>
              <small>{{ hook.stage }}</small>
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  allowedHooks: {
    type: Array,
    default: null,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])
const { dict } = useDict('ai_business_extension_hook')

const hookGroups = [
  {
    key: 'write',
    title: '数据写入',
    description: '在数据落库前校验或处理，落库后执行通知和后续动作。',
    rows: [
      operation('create', '新增', 'BEFORE_CREATE', 'AFTER_CREATE'),
      operation('update', '修改', 'BEFORE_UPDATE', 'AFTER_UPDATE'),
      operation('delete', '删除', 'BEFORE_DELETE', 'AFTER_DELETE'),
    ],
  },
  {
    key: 'read',
    title: '数据读取',
    description: '查询前调整条件，查询后处理返回结果。',
    rows: [
      operation('list', '列表查询', 'BEFORE_LIST', 'AFTER_LIST'),
      operation('detail', '详情查询', 'BEFORE_DETAIL', 'AFTER_DETAIL'),
      operation('summary', '汇总统计', 'BEFORE_SUMMARY', 'AFTER_SUMMARY'),
    ],
  },
  {
    key: 'exchange',
    title: '数据交换',
    description: '围绕导入和导出任务执行校验、转换及完成处理。',
    rows: [
      operation('import', '数据导入', 'BEFORE_IMPORT', 'AFTER_IMPORT'),
      operation('export', '数据导出', 'BEFORE_EXPORT', 'AFTER_EXPORT'),
    ],
  },
  {
    key: 'page',
    title: '页面交互',
    description: '用于页面初始化、字段联动、提交和行级操作。',
    rows: [
      singleOperation('page-init', '页面初始化', 'PAGE_INIT', '页面事件'),
      singleOperation('form-change', '字段变化', 'FORM_CHANGE', '页面事件'),
      operation('submit', '表单提交', 'BEFORE_SUBMIT', 'AFTER_SUBMIT'),
      singleOperation('row-action', '列表行操作', 'ROW_ACTION', '页面事件'),
    ],
  },
]

const hookFallbackLabels = {
  BEFORE_CREATE: '新增前',
  AFTER_CREATE: '新增后',
  BEFORE_UPDATE: '修改前',
  AFTER_UPDATE: '修改后',
  BEFORE_DELETE: '删除前',
  AFTER_DELETE: '删除后',
  BEFORE_IMPORT: '导入前',
  AFTER_IMPORT: '导入后',
  BEFORE_EXPORT: '导出前',
  AFTER_EXPORT: '导出后',
  BEFORE_LIST: '列表查询前',
  AFTER_LIST: '列表查询后',
  BEFORE_DETAIL: '详情查询前',
  AFTER_DETAIL: '详情查询后',
  BEFORE_SUMMARY: '汇总前',
  AFTER_SUMMARY: '汇总后',
  PAGE_INIT: '页面初始化',
  FORM_CHANGE: '字段变化',
  BEFORE_SUBMIT: '提交前',
  AFTER_SUBMIT: '提交后',
  ROW_ACTION: '列表行操作',
}

const hookFallbackDescriptions = {
  BEFORE_CREATE: '新增记录写入数据库前执行',
  AFTER_CREATE: '新增记录写入数据库后执行',
  BEFORE_UPDATE: '修改记录写入数据库前执行',
  AFTER_UPDATE: '修改记录写入数据库后执行',
  BEFORE_DELETE: '删除记录前执行',
  AFTER_DELETE: '删除记录后执行',
  BEFORE_IMPORT: '数据导入处理前执行',
  AFTER_IMPORT: '数据导入完成后执行',
  BEFORE_EXPORT: '数据导出处理前执行',
  AFTER_EXPORT: '数据导出完成后执行',
  BEFORE_LIST: '列表查询条件执行前调用',
  AFTER_LIST: '列表查询结果返回前调用',
  BEFORE_DETAIL: '详情查询执行前调用',
  AFTER_DETAIL: '详情数据返回前调用',
  BEFORE_SUMMARY: '汇总统计执行前调用',
  AFTER_SUMMARY: '汇总统计结果返回前调用',
  PAGE_INIT: '页面根节点初始化时调用',
  FORM_CHANGE: '白名单表单字段发生变化时调用',
  BEFORE_SUBMIT: '表单提交进入业务处理前调用',
  AFTER_SUBMIT: '表单提交成功后调用',
  ROW_ACTION: '用户触发受控列表行操作时调用',
}

const allowedHookSet = computed(() => props.allowedHooks === null
  ? null
  : new Set(props.allowedHooks || []))
const selectedHook = computed(() => props.modelValue || '')
const hookDict = computed(() => dict.value.ai_business_extension_hook || [])
const selectedHookDescription = computed(() => hookDescription(selectedHook.value))

function operation(key, label, before, after) {
  return {
    key,
    label,
    hooks: [
      { value: before, stage: '执行前' },
      { value: after, stage: '执行后' },
    ],
  }
}

function singleOperation(key, label, value, stage) {
  return { key, label, hooks: [{ value, stage }] }
}

function hookItem(value) {
  return hookDict.value.find(item => String(item.value) === String(value)) || null
}

function hookLabel(value) {
  return hookItem(value)?.label || hookFallbackLabels[value] || '业务触发点'
}

function hookDescription(value) {
  return hookItem(value)?.remark || hookItem(value)?.raw?.remark || hookFallbackDescriptions[value] || ''
}

function isAllowed(value) {
  return allowedHookSet.value === null || allowedHookSet.value.has(value)
}

function selectHook(value) {
  if (!props.disabled && isAllowed(value))
    emit('update:modelValue', value)
}
</script>

<style scoped>
.hook-matrix {
  display: grid;
  width: 100%;
  gap: 10px;
}

.hook-matrix-heading,
.selected-hook,
.hook-group header,
.hook-row {
  display: flex;
  align-items: center;
}

.hook-matrix-heading {
  justify-content: space-between;
  gap: 16px;
}

.hook-matrix-heading > div:first-child,
.hook-group header {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
}

.hook-matrix-heading strong {
  color: var(--text-primary, #1d2129);
  font-size: 14px;
}

.hook-matrix-heading span,
.hook-group header span,
.selected-hook small {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.selected-hook {
  justify-content: flex-end;
  gap: 8px;
}

.hook-fallback-tag {
  padding: 2px 7px;
  border-radius: 4px;
  color: var(--primary-color, #165dff);
  background: color-mix(in srgb, var(--primary-color, #165dff) 8%, var(--bg-primary, #fff));
  font-size: 12px;
}

.hook-group {
  overflow: hidden;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}

.hook-group header {
  padding: 9px 11px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  background: var(--bg-secondary, #f7f8fa);
}

.hook-group header strong {
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
}

.hook-group-rows {
  display: grid;
}

.hook-row {
  min-height: 44px;
  gap: 12px;
  padding: 6px 10px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.hook-row:last-child {
  border-bottom: 0;
}

.hook-operation {
  width: 90px;
  flex: 0 0 90px;
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
  font-weight: 600;
}

.hook-options {
  display: grid;
  min-width: 0;
  flex: 1;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
}

.hook-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 30px;
  padding: 5px 9px;
  cursor: pointer;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 5px;
  color: var(--text-secondary, #4e5969);
  background: var(--bg-primary, #fff);
  font-size: 12px;
  text-align: left;
}

.hook-option:hover:not(:disabled),
.hook-option.active {
  border-color: var(--primary-color, #165dff);
}

.hook-option.active {
  color: var(--primary-color, #165dff);
  background: color-mix(in srgb, var(--primary-color, #165dff) 7%, var(--bg-primary, #fff));
  font-weight: 600;
}

.hook-option small {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.hook-option.unavailable {
  cursor: not-allowed;
  opacity: 0.38;
}

@media (max-width: 720px) {
  .hook-matrix-heading,
  .hook-row {
    align-items: stretch;
    flex-direction: column;
  }

  .selected-hook {
    justify-content: flex-start;
  }

  .hook-operation {
    width: auto;
    flex-basis: auto;
  }
}
</style>
