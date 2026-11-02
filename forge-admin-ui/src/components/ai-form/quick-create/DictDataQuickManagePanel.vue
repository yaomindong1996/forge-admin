<!--
  本类型字典项管理：列表 + 按权限新增/修改/删除；支持拖拽与上下移排序（编辑不改键值）。
-->
<template>
  <div class="dict-quick-manage">
    <!-- 工具栏 -->
    <div class="dict-quick-manage__toolbar">
      <div class="dict-quick-manage__toolbar-main">
        <span class="dict-quick-manage__type" :title="dictType">{{ dictType }}</span>
        <span v-if="canEdit && rows.length > 1" class="dict-quick-manage__hint">
          拖动手柄或使用箭头调整排序
        </span>
      </div>
      <n-button
        v-if="canAdd"
        type="primary"
        size="small"
        @click="openCreate"
      >
        <template #icon>
          <i class="i-lucide:plus" />
        </template>
        新增
      </n-button>
    </div>

    <!-- 列表 -->
    <n-spin :show="loading || sorting">
      <n-empty v-if="!loading && !rows.length" description="暂无字典项，可新增后使用" />
      <draggable
        v-else
        v-model="rows"
        class="dict-quick-manage__list"
        item-key="rowKey"
        handle=".dict-quick-manage__drag"
        :animation="180"
        :disabled="!canEdit || sorting || loading"
        ghost-class="dict-quick-manage__row--ghost"
        chosen-class="dict-quick-manage__row--chosen"
        drag-class="dict-quick-manage__row--drag"
        @end="onDragEnd"
      >
        <template #item="{ element: row, index }">
          <div
            class="dict-quick-manage__row"
            :class="{ 'dict-quick-manage__row--disabled': !isEnabled(row) }"
          >
            <!-- 拖拽手柄 -->
            <button
              v-if="canEdit"
              type="button"
              class="dict-quick-manage__drag"
              :disabled="sorting || rows.length < 2"
              title="拖动排序"
              aria-label="拖动排序"
            >
              <i class="i-lucide:grip-vertical" />
            </button>
            <span v-else class="dict-quick-manage__drag-spacer" aria-hidden="true" />

            <span class="dict-quick-manage__index" aria-hidden="true">{{ index + 1 }}</span>

            <div class="dict-quick-manage__meta">
              <strong class="dict-quick-manage__label">{{ row.label }}</strong>
              <span class="dict-quick-manage__value">{{ row.value }}</span>
              <n-tag
                size="small"
                :type="isEnabled(row) ? 'success' : 'default'"
                :bordered="false"
              >
                {{ isEnabled(row) ? '正常' : '禁用' }}
              </n-tag>
            </div>

            <div class="dict-quick-manage__actions">
              <!-- 排序：图标按钮 -->
              <div v-if="canEdit" class="dict-quick-manage__sort">
                <n-button
                  text
                  type="primary"
                  size="tiny"
                  class="dict-quick-manage__icon-btn"
                  :disabled="index === 0 || sorting"
                  title="上移"
                  aria-label="上移"
                  @click="moveRow(index, -1)"
                >
                  <i class="i-lucide:chevron-up" />
                </n-button>
                <n-button
                  text
                  type="primary"
                  size="tiny"
                  class="dict-quick-manage__icon-btn"
                  :disabled="index >= rows.length - 1 || sorting"
                  title="下移"
                  aria-label="下移"
                  @click="moveRow(index, 1)"
                >
                  <i class="i-lucide:chevron-down" />
                </n-button>
              </div>

              <n-button
                v-if="canEdit"
                text
                type="primary"
                size="tiny"
                @click="openEdit(row)"
              >
                <template #icon>
                  <i class="i-lucide:pencil" />
                </template>
                修改
              </n-button>
              <n-button
                v-if="canRemove"
                text
                type="error"
                size="tiny"
                @click="confirmRemove(row)"
              >
                <template #icon>
                  <i class="i-lucide:trash-2" />
                </template>
                删除
              </n-button>
            </div>
          </div>
        </template>
      </draggable>
    </n-spin>

    <!-- 新增 / 修改弹窗 -->
    <n-modal
      v-model:show="formVisible"
      preset="card"
      :title="editing ? '修改字典项' : '新增字典项'"
      class="dict-quick-manage-form-modal"
      :mask-closable="false"
      :style="{ width: '520px' }"
    >
      <AiForm
        ref="formRef"
        v-model:value="formValue"
        :schema="formSchema"
        :grid-cols="1"
        label-placement="left"
        :label-width="100"
        label-align="right"
        size="small"
        :show-actions="false"
      />
      <template #footer>
        <n-space justify="end">
          <n-button @click="formVisible = false">
            取消
          </n-button>
          <n-button type="primary" :loading="saving" @click="saveForm">
            保存
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import draggable from 'vuedraggable'
import { getDictData } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { hasQuickCreatePermission } from './permissions'
import { resolveDictTypeFromContext } from './presets/dictData'

const AiForm = defineAsyncComponent(() => import('../AiForm.vue'))

const props = defineProps({
  definition: {
    type: Object,
    required: true,
  },
  field: {
    type: Object,
    default: () => ({}),
  },
  parentFormData: {
    type: Object,
    default: () => ({}),
  },
  active: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['changed'])

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const sorting = ref(false)
const rows = ref([])
const formVisible = ref(false)
const editing = ref(false)
const formRef = ref(null)
const formValue = ref({})

const ctx = computed(() => ({
  userStore,
  parentFormData: props.parentFormData,
  config: props.definition,
  field: props.field,
}))

const dictType = computed(() => resolveDictTypeFromContext(ctx.value))

const canAdd = computed(() =>
  hasQuickCreatePermission(userStore, props.definition.addPermission || 'system:dict:add'))
const canEdit = computed(() =>
  hasQuickCreatePermission(userStore, props.definition.editPermission || 'system:dict:edit'))
const canRemove = computed(() =>
  hasQuickCreatePermission(userStore, props.definition.removePermission || 'system:dict:remove'))

const formSchema = computed(() => {
  if (editing.value && typeof props.definition.buildEditSchema === 'function')
    return props.definition.buildEditSchema(ctx.value)
  return props.definition.buildSchema?.(ctx.value) || []
})

function rowKey(row) {
  return String(row?.dictCode ?? row?.raw?.dictCode ?? row?.value ?? '')
}

function isEnabled(row) {
  const status = row.status !== undefined ? row.status : row.raw?.dictStatus
  return Number(status) === 1
}

function rowSort(row, fallbackIndex = 0) {
  const raw = row?.dictSort ?? row?.raw?.dictSort
  const num = Number(raw)
  return Number.isFinite(num) ? num : fallbackIndex
}

function normalizeRows(list = []) {
  return (list || []).map((row, index) => ({
    ...row,
    rowKey: rowKey(row) || `row-${index}`,
    dictSort: rowSort(row, index),
  }))
}

function buildSortPayload(row, dictSort) {
  return {
    ...props.definition.buildEditInitialValues?.(row, ctx.value),
    dictSort,
  }
}

async function reload() {
  if (!dictType.value)
    return
  loading.value = true
  try {
    rows.value = normalizeRows(await getDictData(dictType.value, true))
  }
  finally {
    loading.value = false
  }
}

watch(() => props.active, (active) => {
  if (active)
    reload()
}, { immediate: true })

function openCreate() {
  if (!canAdd.value)
    return
  editing.value = false
  formValue.value = props.definition.buildInitialValues?.(ctx.value) || { dictType: dictType.value }
  formVisible.value = true
}

function openEdit(row) {
  if (!canEdit.value)
    return
  editing.value = true
  formValue.value = props.definition.buildEditInitialValues?.(row, ctx.value) || {}
  formVisible.value = true
}

async function saveForm() {
  try {
    await formRef.value?.validate?.()
  }
  catch {
    return
  }
  saving.value = true
  try {
    const result = editing.value
      ? await props.definition.update?.(formValue.value, ctx.value)
      : await props.definition.submit?.(formValue.value, ctx.value)
    formVisible.value = false
    window.$message?.success(editing.value ? '已更新' : '已新增')
    await reload()
    emit('changed', result || { action: editing.value ? 'updated' : 'created' })
  }
  catch (error) {
    window.$message?.error(error?.message || '保存失败')
  }
  finally {
    saving.value = false
  }
}

/** 按当前列表顺序写回 dictSort（仅提交有变化的项） */
async function persistCurrentOrder() {
  if (!canEdit.value || typeof props.definition.update !== 'function')
    return false
  const payloads = rows.value
    .map((row, index) => ({
      row,
      index,
      changed: rowSort(row, index) !== index,
    }))
    .filter(item => item.changed)
  if (!payloads.length)
    return false

  sorting.value = true
  try {
    for (const item of payloads) {
      await props.definition.update(buildSortPayload(item.row, item.index), ctx.value)
    }
    await reload()
    emit('changed', { action: 'sorted' })
    return true
  }
  catch (error) {
    window.$message?.error(error?.message || '排序失败')
    await reload()
    return false
  }
  finally {
    sorting.value = false
  }
}

async function onDragEnd(evt) {
  if (!canEdit.value || sorting.value)
    return
  if (evt?.oldIndex == null || evt?.newIndex == null || evt.oldIndex === evt.newIndex)
    return
  await persistCurrentOrder()
}

async function moveRow(index, delta) {
  if (!canEdit.value || sorting.value || typeof props.definition.update !== 'function')
    return
  const target = index + delta
  if (target < 0 || target >= rows.value.length)
    return

  const next = rows.value.slice()
  const [moved] = next.splice(index, 1)
  next.splice(target, 0, moved)
  rows.value = next
  await persistCurrentOrder()
}

function confirmRemove(row) {
  if (!canRemove.value)
    return
  window.$dialog?.warning({
    title: '确认删除',
    content: `确定删除「${row.label || row.value}」吗？删除后无法恢复。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const result = await props.definition.remove?.(row, ctx.value)
        window.$message?.success('已删除')
        await reload()
        emit('changed', result || {
          action: 'deleted',
          dictValue: row.value,
        })
      }
      catch (error) {
        window.$message?.error(error?.message || '删除失败')
      }
    },
  })
}
</script>

<style scoped>
.dict-quick-manage__toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.dict-quick-manage__toolbar-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.dict-quick-manage__type {
  color: var(--text-tertiary, #8c8c8c);
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  word-break: break-all;
}

.dict-quick-manage__hint {
  color: var(--text-tertiary, #8c8c8c);
  font-size: 12px;
  line-height: 1.4;
}

.dict-quick-manage__list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: min(52vh, 420px);
  overflow: auto;
  min-height: 0;
  padding: 2px;
}

.dict-quick-manage__row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--border-light, color-mix(in srgb, #262626 8%, transparent));
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease;
}

.dict-quick-manage__row:hover {
  border-color: color-mix(in srgb, var(--primary-color, #1890ff) 35%, transparent);
}

.dict-quick-manage__row--disabled {
  background: color-mix(in srgb, var(--bg-secondary, #f0f2f5) 65%, #fff);
}

.dict-quick-manage__row--ghost {
  opacity: 0.45;
  border-style: dashed;
}

.dict-quick-manage__row--chosen {
  border-color: var(--primary-color, #1890ff);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary-color, #1890ff) 25%, transparent);
}

.dict-quick-manage__row--drag {
  opacity: 0.95;
  cursor: grabbing;
}

.dict-quick-manage__drag,
.dict-quick-manage__drag-spacer {
  flex: 0 0 22px;
  width: 22px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.dict-quick-manage__drag {
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--text-tertiary, #8c8c8c);
  cursor: grab;
  touch-action: none;
}

.dict-quick-manage__drag:hover:not(:disabled) {
  color: var(--primary-color, #1890ff);
  background: color-mix(in srgb, var(--primary-color, #1890ff) 8%, transparent);
}

.dict-quick-manage__drag:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.dict-quick-manage__drag:active:not(:disabled) {
  cursor: grabbing;
}

.dict-quick-manage__drag i {
  font-size: 16px;
}

.dict-quick-manage__index {
  flex: 0 0 20px;
  width: 20px;
  color: var(--text-tertiary, #8c8c8c);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  text-align: center;
}

.dict-quick-manage__meta {
  display: flex;
  flex: 1 1 auto;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.dict-quick-manage__label {
  color: var(--text-primary, #262626);
  font-size: 13px;
  font-weight: 600;
}

.dict-quick-manage__value {
  color: var(--text-tertiary, #8c8c8c);
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.dict-quick-manage__actions {
  display: inline-flex;
  flex: 0 0 auto;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-end;
  gap: 2px;
}

.dict-quick-manage__sort {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  margin-right: 4px;
  line-height: 1;
}

.dict-quick-manage__icon-btn {
  width: 22px;
  min-width: 22px;
  height: 16px;
  padding: 0 !important;
}

.dict-quick-manage__icon-btn i {
  font-size: 15px;
}
</style>
