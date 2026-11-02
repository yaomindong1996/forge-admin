<!--
  本类型字典项管理：列表 + 按权限新增/修改/删除/上下移排序（编辑不改键值）。
-->
<template>
  <div class="dict-quick-manage">
    <div class="dict-quick-manage__toolbar">
      <span class="dict-quick-manage__type">{{ dictType }}</span>
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

    <n-spin :show="loading">
      <n-empty v-if="!loading && !rows.length" description="暂无字典项，可新增后使用" />
      <div v-else class="dict-quick-manage__list">
        <div
          v-for="(row, index) in rows"
          :key="String(row.dictCode || row.value)"
          class="dict-quick-manage__row"
        >
          <div class="dict-quick-manage__meta">
            <strong>{{ row.label }}</strong>
            <span class="dict-quick-manage__value">{{ row.value }}</span>
            <n-tag size="small" :type="isEnabled(row) ? 'success' : 'default'" :bordered="false">
              {{ isEnabled(row) ? '正常' : '禁用' }}
            </n-tag>
          </div>
          <div class="dict-quick-manage__actions">
            <n-button
              v-if="canEdit"
              text
              type="primary"
              size="tiny"
              :disabled="index === 0 || sorting"
              aria-label="上移"
              @click="moveRow(index, -1)"
            >
              上移
            </n-button>
            <n-button
              v-if="canEdit"
              text
              type="primary"
              size="tiny"
              :disabled="index >= rows.length - 1 || sorting"
              aria-label="下移"
              @click="moveRow(index, 1)"
            >
              下移
            </n-button>
            <n-button
              v-if="canEdit"
              text
              type="primary"
              size="tiny"
              @click="openEdit(row)"
            >
              修改
            </n-button>
            <n-button
              v-if="canRemove"
              text
              type="error"
              size="tiny"
              @click="confirmRemove(row)"
            >
              删除
            </n-button>
          </div>
        </div>
      </div>
    </n-spin>

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

function isEnabled(row) {
  const status = row.status !== undefined ? row.status : row.raw?.dictStatus
  return Number(status) === 1
}

function rowSort(row, fallbackIndex = 0) {
  const raw = row?.dictSort ?? row?.raw?.dictSort
  const num = Number(raw)
  return Number.isFinite(num) ? num : fallbackIndex
}

async function reload() {
  if (!dictType.value)
    return
  loading.value = true
  try {
    rows.value = await getDictData(dictType.value, true)
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

async function moveRow(index, delta) {
  if (!canEdit.value || sorting.value || typeof props.definition.update !== 'function')
    return
  const target = index + delta
  if (target < 0 || target >= rows.value.length)
    return
  const current = rows.value[index]
  const neighbor = rows.value[target]
  let currentSort = rowSort(current, index)
  let neighborSort = rowSort(neighbor, target)
  if (currentSort === neighborSort) {
    currentSort = index
    neighborSort = target
  }
  const currentPayload = {
    ...props.definition.buildEditInitialValues?.(current, ctx.value),
    dictSort: neighborSort,
  }
  const neighborPayload = {
    ...props.definition.buildEditInitialValues?.(neighbor, ctx.value),
    dictSort: currentSort,
  }
  sorting.value = true
  try {
    await props.definition.update(currentPayload, ctx.value)
    await props.definition.update(neighborPayload, ctx.value)
    await reload()
    emit('changed', { action: 'sorted' })
  }
  catch (error) {
    window.$message?.error(error?.message || '排序失败')
  }
  finally {
    sorting.value = false
  }
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
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.dict-quick-manage__type {
  color: var(--text-secondary, #8c8c8c);
  font-size: 12px;
  word-break: break-all;
}

.dict-quick-manage__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: min(52vh, 420px);
  overflow: auto;
}

.dict-quick-manage__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid color-mix(in srgb, #262626 8%, transparent);
  border-radius: 4px;
  background: var(--bg-primary, #fff);
}

.dict-quick-manage__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.dict-quick-manage__meta strong {
  color: #262626;
  font-size: 13px;
  font-weight: 600;
}

.dict-quick-manage__value {
  color: #8c8c8c;
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.dict-quick-manage__actions {
  display: inline-flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 4px;
}
</style>
