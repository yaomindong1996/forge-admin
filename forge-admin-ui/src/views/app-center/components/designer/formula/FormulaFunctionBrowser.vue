<template>
  <section class="function-browser">
    <header class="function-browser-head">
      <n-input
        v-model:value="keyword"
        size="small"
        clearable
        placeholder="搜索函数"
      />
      <n-button size="small" quaternary :loading="loading" @click="loadFunctions">
        <template #icon>
          <n-icon><RefreshOutline /></n-icon>
        </template>
      </n-button>
    </header>

    <div v-if="loading" class="function-loading">
      <span />
    </div>
    <n-empty
      v-else-if="!filteredFunctions.length"
      size="small"
      description="暂无可用函数"
    />
    <div v-else class="function-workbench">
      <aside class="function-list-panel">
        <div v-if="categoryOptions.length > 1" class="function-categories">
          <button
            v-for="item in categoryOptions"
            :key="item.value"
            type="button"
            class="category-filter"
            :class="{ active: category === item.value }"
            @click="category = item.value"
          >
            {{ item.label }}
          </button>
        </div>
        <div class="function-list">
          <button
            v-for="item in filteredFunctions"
            :key="item.name"
            type="button"
            class="function-item"
            :class="{ active: selectedFunction?.name === item.name }"
            @click="selectedFunctionCode = item.name"
            @dblclick="insertFunction(item)"
          >
            <span class="function-code">{{ item.name }}</span>
            <span class="function-meta">
              <em>{{ item.displayName || item.category || 'Other' }}</em>
              <i>{{ item.description || item.example || '-' }}</i>
            </span>
          </button>
        </div>
      </aside>

      <article class="function-detail">
        <template v-if="selectedFunction">
          <div class="function-detail-head">
            <div>
              <span>{{ selectedFunction.category || 'Other' }}</span>
              <strong>{{ selectedFunction.displayName || selectedFunction.name }}</strong>
              <code>{{ selectedFunction.name }}</code>
            </div>
            <n-tag size="small" :bordered="false">
              {{ selectedFunction.returnType || 'ANY' }}
            </n-tag>
          </div>

          <div class="function-detail-section">
            <span>说明</span>
            <p>{{ selectedFunction.description || '暂无说明' }}</p>
          </div>

          <div class="function-detail-section">
            <span>参数</span>
            <div v-if="parseArgumentSchema(selectedFunction).length" class="argument-list">
              <div
                v-for="argument in parseArgumentSchema(selectedFunction)"
                :key="argument.name"
                class="argument-item"
              >
                <strong>{{ argument.name }}</strong>
                <em>{{ argument.type || 'ANY' }}</em>
                <n-tag v-if="argument.required" size="tiny" type="warning" :bordered="false">
                  必填
                </n-tag>
              </div>
            </div>
            <p v-else>
              无固定参数
            </p>
          </div>

          <div class="function-detail-section">
            <span>示例</span>
            <code>{{ selectedFunction.example || buildInsertText(selectedFunction) }}</code>
          </div>

          <div class="function-detail-actions">
            <n-button
              type="primary"
              size="small"
              :disabled="disabled"
              @click="insertFunction(selectedFunction)"
            >
              插入函数
            </n-button>
          </div>
        </template>
      </article>
    </div>
  </section>
</template>

<script setup>
import { RefreshOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import { getFormulaFunctions } from '@/api/formula'

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['insert'])
const message = useMessage()

const loading = ref(false)
const keyword = ref('')
const category = ref('ALL')
const functions = ref([])
const selectedFunctionCode = ref('')

const categoryOptions = computed(() => {
  const categories = Array.from(new Set(functions.value
    .map(item => item.category || 'Other')
    .filter(Boolean)))
    .sort((a, b) => a.localeCompare(b))
  return [
    { label: '全部', value: 'ALL' },
    ...categories.map(item => ({ label: item, value: item })),
  ]
})

const filteredFunctions = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return functions.value
    .filter(item => category.value === 'ALL' || item.category === category.value)
    .filter((item) => {
      if (!search)
        return true
      return [item.name, item.displayName, item.category, item.description, item.example, item.returnType]
        .some(value => String(value || '').toLowerCase().includes(search))
    })
})
const selectedFunction = computed(() => {
  return filteredFunctions.value.find(item => item.name === selectedFunctionCode.value)
    || filteredFunctions.value[0]
    || null
})

onMounted(loadFunctions)

watch(filteredFunctions, (list) => {
  if (!list.length) {
    selectedFunctionCode.value = ''
    return
  }
  if (!list.some(item => item.name === selectedFunctionCode.value))
    selectedFunctionCode.value = list[0].name
})

async function loadFunctions() {
  loading.value = true
  try {
    const res = await getFormulaFunctions()
    const rows = Array.isArray(res?.data) ? res.data : Array.isArray(res) ? res : []
    functions.value = rows.map(normalizeFunction).filter(item => item.name)
    if (!categoryOptions.value.some(item => item.value === category.value))
      category.value = 'ALL'
    if (!selectedFunctionCode.value && functions.value.length)
      selectedFunctionCode.value = functions.value[0].name
  }
  catch (e) {
    functions.value = []
    message.error(e?.message || '函数列表加载失败')
  }
  finally {
    loading.value = false
  }
}

function normalizeFunction(item = {}) {
  return {
    ...item,
    name: item.name || item.functionCode || '',
    displayName: item.displayName || item.name || item.functionCode || '',
    category: item.category || 'Other',
    returnType: item.returnType || 'ANY',
  }
}

function buildInsertText(item = {}) {
  const name = String(item.name || '').trim()
  if (!name)
    return ''
  const args = parseArgumentSchema(item)
    .filter(argument => argument.required !== false)
    .map(argument => argument.name)
    .filter(Boolean)
  return `${name}(${args.join(', ')})`
}

function insertFunction(item) {
  if (props.disabled)
    return
  emit('insert', buildInsertText(item))
}

function parseArgumentSchema(item = {}) {
  const source = item.argumentSchema
  if (!source)
    return []
  try {
    const parsed = typeof source === 'string' ? JSON.parse(source) : source
    return (Array.isArray(parsed) ? parsed : [])
      .map(argument => ({
        name: String(argument?.name || '').trim(),
        type: argument?.type || 'ANY',
        required: argument?.required !== false,
      }))
      .filter(argument => argument.name)
  }
  catch {
    return []
  }
}

defineExpose({
  loadFunctions,
})
</script>

<style scoped>
.function-browser {
  display: grid;
  gap: 10px;
  width: 100%;
  border-top: 1px solid #d2d8e2;
  background: var(--bg-secondary);
  padding: 10px;
}

.function-browser-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
}

.function-categories {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.category-filter {
  flex: 0 0 auto;
  height: 26px;
  border: 1px solid #d6dde8;
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 12px;
  padding: 0 9px;
}

.category-filter.active {
  border-color: #2563eb;
  background: #eaf2ff;
  color: #1d4ed8;
  font-weight: 700;
}

.function-loading {
  display: grid;
  min-height: 88px;
  place-items: center;
}

.function-loading span {
  width: 20px;
  height: 20px;
  border: 2px solid #d6dde8;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.function-list {
  display: grid;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
}

.function-workbench {
  display: grid;
  grid-template-columns: minmax(220px, 0.72fr) minmax(320px, 1fr);
  gap: 10px;
  min-height: 330px;
}

.function-list-panel,
.function-detail {
  min-width: 0;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 10px;
}

.function-list-panel {
  display: grid;
  align-content: start;
  gap: 9px;
}

.function-item {
  display: grid;
  gap: 5px;
  min-height: 64px;
  border: 1px solid #d6dde8;
  border-radius: 7px;
  background: var(--bg-primary);
  color: var(--text-primary);
  cursor: pointer;
  text-align: left;
  padding: 9px 10px;
}

.function-item:hover:not(:disabled) {
  border-color: #93c5fd;
  background: #f8fbff;
}

.function-item.active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: inset 3px 0 0 #2563eb;
}

.function-item:disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.function-code {
  overflow: hidden;
  color: var(--text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.function-meta {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.function-meta em {
  color: #2563eb;
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
}

.function-meta i {
  display: -webkit-box;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
  line-height: 1.4;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.function-detail {
  display: grid;
  align-content: start;
  gap: 14px;
}

.function-detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 12px;
}

.function-detail-head div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.function-detail-head span,
.function-detail-section span {
  color: var(--text-secondary);
  font-size: 12px;
}

.function-detail-head strong {
  color: var(--text-primary);
  font-size: 16px;
  line-height: 1.35;
}

.function-detail-head code,
.function-detail-section code {
  overflow-x: auto;
  border-radius: 7px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  padding: 7px 8px;
}

.function-detail-section {
  display: grid;
  gap: 7px;
}

.function-detail-section p {
  margin: 0;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.6;
}

.argument-list {
  display: grid;
  gap: 6px;
}

.argument-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 8px;
  border: 1px solid #edf1f6;
  border-radius: 7px;
  background: var(--bg-secondary);
  padding: 8px 9px;
}

.argument-item strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.argument-item em {
  color: #2563eb;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.function-detail-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 760px) {
  .function-workbench {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
