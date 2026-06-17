<template>
  <div class="dict-type-select">
    <div class="dict-select-row">
      <n-select
        :value="value"
        class="dict-select-control"
        :options="options"
        :loading="loading"
        :disabled="disabled"
        tag
        filterable
        clearable
        size="small"
        placeholder="选择系统字典或输入字典编码"
        :filter="filterDictOption"
        @focus="loadDictTypes"
        @update:value="handleValueUpdate"
      />
      <div class="dict-select-actions">
        <n-button class="create-dict-button" size="small" type="primary" :disabled="disabled" @click="openCreateModal">
          新增字典
        </n-button>
      </div>
    </div>

    <n-modal
      v-model:show="createVisible"
      title="新增字典"
      preset="card"
      style="width: 620px"
      :mask-closable="false"
    >
      <n-form label-placement="top" size="small">
        <n-grid :cols="2" :x-gap="12">
          <n-form-item-gi label="字典名称">
            <n-input v-model:value="dictForm.dictName" placeholder="例如：合同状态" />
          </n-form-item-gi>
          <n-form-item-gi label="字典类型">
            <n-input
              :value="dictForm.dictType"
              placeholder="contract_status"
              @update:value="dictForm.dictType = normalizeDictType($event)"
            />
          </n-form-item-gi>
        </n-grid>
        <n-form-item label="备注">
          <n-input v-model:value="dictForm.remark" type="textarea" :rows="2" placeholder="可为空" />
        </n-form-item>

        <n-divider>字典项</n-divider>
        <div class="dict-item-list">
          <div
            v-for="(item, index) in dictForm.items"
            :key="index"
            class="dict-item-row"
          >
            <n-input v-model:value="item.dictLabel" size="small" placeholder="标签，如：启用" />
            <n-input v-model:value="item.dictValue" size="small" placeholder="值，如：1" />
            <n-select v-model:value="item.listClass" size="small" :options="tagTypeOptions" />
            <n-button text type="error" size="small" @click="removeDictItem(index)">
              删除
            </n-button>
          </div>
          <n-button dashed block size="small" @click="addDictItem">
            添加字典项
          </n-button>
        </div>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="createVisible = false">
            取消
          </n-button>
          <n-button type="primary" :loading="saving" @click="saveDict">
            保存到字典库
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { clearDictCache } from '@/composables/useDict'
import { request } from '@/utils'

const props = defineProps({
  value: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  compact: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:value'])

const loading = ref(false)
const saving = ref(false)
const createVisible = ref(false)
const systemDictTypes = ref([])
const loaded = ref(false)

const tagTypeOptions = [
  { label: '默认', value: 'default' },
  { label: '成功', value: 'success' },
  { label: '信息', value: 'info' },
  { label: '警告', value: 'warning' },
  { label: '错误', value: 'error' },
]

const dictForm = reactive(createDefaultDictForm())

const options = computed(() => {
  const map = new Map()
  systemDictTypes.value.forEach((item) => {
    if (item.dictType) {
      map.set(item.dictType, {
        label: formatDictTypeLabel(item),
        value: item.dictType,
        dictName: item.dictName,
        dictType: item.dictType,
      })
    }
  })
  props.fields.forEach((field) => {
    if (field.dictType && !map.has(field.dictType)) {
      map.set(field.dictType, {
        label: field.dictName ? `${field.dictName} · ${field.dictType}` : field.dictType,
        value: field.dictType,
        dictName: field.dictName,
        dictType: field.dictType,
      })
    }
  })
  if (props.value && !map.has(props.value)) {
    map.set(props.value, { label: props.value, value: props.value, dictType: props.value })
  }
  return Array.from(map.values())
})

onMounted(loadDictTypes)

async function loadDictTypes() {
  if (loading.value || loaded.value)
    return
  loading.value = true
  try {
    const res = await request.get('/system/dict/type/list', {
      params: { dictStatus: 1 },
    })
    systemDictTypes.value = Array.isArray(res.data) ? res.data : []
    loaded.value = true
  }
  catch (error) {
    console.error('[DictTypeSelect] 加载字典类型失败:', error)
  }
  finally {
    loading.value = false
  }
}

function handleValueUpdate(value) {
  const nextValue = value || ''
  if (nextValue === props.value)
    return
  emit('update:value', nextValue)
}

function formatDictTypeLabel(item = {}) {
  if (!item.dictType)
    return ''
  return item.dictName ? `${item.dictName} · ${item.dictType}` : item.dictType
}

function filterDictOption(pattern = '', option = {}) {
  const keyword = String(pattern || '').trim().toLowerCase()
  if (!keyword)
    return true
  return [option.label, option.value, option.dictName, option.dictType]
    .filter(Boolean)
    .some(item => String(item).toLowerCase().includes(keyword))
}

function openCreateModal() {
  Object.assign(dictForm, createDefaultDictForm(props.value))
  createVisible.value = true
}

function createDefaultDictForm(defaultType = '') {
  return {
    dictName: '',
    dictType: normalizeDictType(defaultType),
    remark: '',
    items: [
      { dictLabel: '启用', dictValue: '1', listClass: 'success' },
      { dictLabel: '禁用', dictValue: '0', listClass: 'error' },
    ],
  }
}

function normalizeDictType(value) {
  return String(value || '')
    .trim()
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W/g, '_')
    .replace(/_+/g, '_')
    .toLowerCase()
    .replace(/^[^a-z]+/, '')
}

function addDictItem() {
  dictForm.items.push({
    dictLabel: '',
    dictValue: '',
    listClass: 'default',
  })
}

function removeDictItem(index) {
  dictForm.items.splice(index, 1)
}

async function saveDict() {
  if (!dictForm.dictName || !dictForm.dictType) {
    window.$message?.warning('请填写字典名称和字典类型')
    return
  }
  const items = dictForm.items
    .map((item, index) => ({
      ...item,
      dictSort: index + 1,
    }))
    .filter(item => item.dictLabel && item.dictValue)
  if (!items.length) {
    window.$message?.warning('请至少添加一个字典项')
    return
  }

  saving.value = true
  try {
    await request.post('/system/dict/type/add', {
      dictName: dictForm.dictName,
      dictType: dictForm.dictType,
      dictStatus: 1,
      remark: dictForm.remark,
    })
    for (const item of items) {
      await request.post('/system/dict/data/add', {
        dictType: dictForm.dictType,
        dictLabel: item.dictLabel,
        dictValue: item.dictValue,
        dictSort: item.dictSort,
        dictStatus: 1,
        isDefault: 'N',
        listClass: item.listClass || 'default',
        parentDictCode: 0,
      })
    }
    clearDictCache(dictForm.dictType)
    handleValueUpdate(dictForm.dictType)
    loaded.value = false
    await loadDictTypes()
    createVisible.value = false
    window.$message?.success('字典已保存')
  }
  catch (error) {
    window.$message?.error(error?.message || '保存字典失败')
  }
  finally {
    saving.value = false
  }
}
</script>

<style scoped>
.dict-type-select {
  width: 100%;
}

.dict-select-row {
  display: grid;
  gap: 8px;
}

.dict-select-control {
  min-width: 0;
  width: 100%;
}

.dict-select-control :deep(.n-base-selection) {
  min-height: 32px;
  border-color: #cbd5e1;
  background: #fff;
}

.dict-select-control :deep(.n-base-selection-label) {
  color: #111827;
  font-weight: 500;
}

.dict-select-actions {
  display: flex;
  justify-content: flex-end;
}

.create-dict-button {
  min-width: 92px;
  height: 32px;
  min-height: 32px;
  font-weight: 700;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.18);
}

.create-dict-button:hover {
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.24);
}

.create-dict-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: #fff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
}

.dict-item-list {
  display: grid;
  gap: 8px;
}

.dict-item-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(100px, 0.8fr) 120px 48px;
  gap: 8px;
  align-items: center;
}
</style>
