<template>
  <section class="expression-workbench" :class="{ compact }">
    <header class="expression-workbench-head">
      <div>
        <strong>表达式编辑器</strong>
        <span>使用字段、函数、运算符和常量构建计算逻辑</span>
      </div>
      <div class="expression-actions">
        <n-button size="small" secondary :disabled="disabled || !value" @click="formatExpression">
          <template #icon>
            <i class="i-mdi:format-align-left" />
          </template>
          格式化
        </n-button>
        <n-button size="small" secondary :disabled="disabled || !value" @click="clearExpression">
          <template #icon>
            <i class="i-mdi:trash-can-outline" />
          </template>
          清空
        </n-button>
      </div>
    </header>

    <div class="expression-layout">
      <aside class="field-panel">
        <div class="panel-title">
          <strong>字段浏览</strong>
          <n-tag size="tiny" :bordered="false">
            {{ filteredFields.length }}
          </n-tag>
        </div>
        <n-input
          v-model:value="fieldKeyword"
          clearable
          size="small"
          placeholder="搜索字段名称或编码"
        >
          <template #prefix>
            <i class="i-mdi:magnify" />
          </template>
        </n-input>

        <div class="field-groups">
          <n-empty v-if="!filteredFieldGroups.length" size="small" description="暂无可用字段" />
          <section
            v-for="group in filteredFieldGroups"
            :key="group.key"
            class="field-group"
          >
            <button
              type="button"
              class="field-group-head"
              @click="toggleFieldGroup(group.key)"
            >
              <i :class="isFieldGroupExpanded(group.key) ? 'i-mdi:chevron-down' : 'i-mdi:chevron-right'" />
              <span>{{ group.label }}</span>
              <em>{{ group.items.length }}</em>
            </button>
            <n-collapse-transition :show="isFieldGroupExpanded(group.key)">
              <div class="field-list">
                <button
                  v-for="item in group.items"
                  :key="item.code"
                  type="button"
                  class="field-item"
                  :disabled="disabled"
                  @click="insertField(item)"
                >
                  <span class="field-type-mark" :class="item.typeClass">
                    {{ item.typeShort }}
                  </span>
                  <span class="field-main">
                    <strong>{{ item.name }}</strong>
                    <em>{{ item.code }}</em>
                  </span>
                  <span class="field-type-label">{{ item.typeLabel }}</span>
                </button>
              </div>
            </n-collapse-transition>
          </section>
        </div>
      </aside>

      <main class="expression-panel">
        <div class="expression-panel-head">
          <span>表达式</span>
          <div class="operator-toolbar">
            <button
              v-for="token in operatorTokens"
              :key="token"
              type="button"
              :disabled="disabled"
              @click="insertOperator(token)"
            >
              {{ token }}
            </button>
            <button type="button" :disabled="disabled" @click="insertNumberToken">
              0
            </button>
            <button type="button" :disabled="disabled" @click="insertStringToken">
              ''
            </button>
          </div>
        </div>

        <div class="expression-codebox">
          <div class="line-gutter">
            1
          </div>
          <n-input
            ref="expressionInputRef"
            class="expression-input"
            :value="value"
            :disabled="disabled"
            type="textarea"
            :autosize="expressionAutosize"
            :placeholder="placeholder || '例如: unitPrice * quantity'"
            @update:value="handleExpressionChange"
          />
        </div>

        <footer class="dependency-strip">
          <div class="dependency-head">
            <span>依赖字段</span>
            <em>共 {{ dependencyFields.length }} 个字段</em>
          </div>
          <div v-if="dependencyFields.length" class="dependency-chips">
            <button
              v-for="item in dependencyFields"
              :key="item.code"
              type="button"
              class="dependency-chip"
              :disabled="disabled"
              @click="insertField(item)"
            >
              <strong>{{ item.code }}</strong>
              <span>{{ item.typeLabel }}</span>
              <i
                class="i-mdi:close"
                @click.stop="removeDependency(item.code)"
              />
            </button>
          </div>
          <n-empty v-else size="small" description="点击左侧字段后自动生成依赖" />
        </footer>
      </main>

      <aside class="function-panel">
        <div class="panel-title">
          <strong>函数浏览</strong>
          <n-button size="tiny" quaternary :loading="functionLoading" @click="loadFunctions">
            <template #icon>
              <i class="i-mdi:refresh" />
            </template>
          </n-button>
        </div>
        <n-input
          v-model:value="functionKeyword"
          clearable
          size="small"
          placeholder="搜索函数"
        >
          <template #prefix>
            <i class="i-mdi:magnify" />
          </template>
        </n-input>

        <n-spin :show="functionLoading">
          <div class="function-tree">
            <n-empty
              v-if="!filteredFunctionGroups.length && !functionLoading"
              size="small"
              :description="functionLoadError || '暂无可用函数'"
            />
            <section
              v-for="group in filteredFunctionGroups"
              :key="group.key"
              class="function-group"
            >
              <button
                type="button"
                class="function-group-head"
                @click="toggleFunctionGroup(group.key)"
              >
                <i :class="isFunctionGroupExpanded(group.key) ? 'i-mdi:chevron-down' : 'i-mdi:chevron-right'" />
                <span>{{ group.label }}</span>
                <em>{{ group.items.length }}</em>
              </button>
              <n-collapse-transition :show="isFunctionGroupExpanded(group.key)">
                <div class="function-list">
                  <button
                    v-for="item in group.items"
                    :key="item.name"
                    type="button"
                    class="function-item"
                    :class="{ active: selectedFunction?.name === item.name }"
                    :disabled="disabled"
                    @click="selectAndInsertFunction(item)"
                  >
                    <span class="function-signature">{{ buildFunctionSignature(item) }}</span>
                    <span class="function-description">{{ item.description || item.example || '暂无说明' }}</span>
                    <span class="function-example">{{ item.example || buildFunctionSnippet(item) }}</span>
                  </button>
                </div>
              </n-collapse-transition>
            </section>
          </div>
        </n-spin>

        <article v-if="selectedFunction" class="function-inspector">
          <span>{{ selectedFunction.category || 'Other' }}</span>
          <strong>{{ selectedFunction.displayName || selectedFunction.name }}</strong>
          <code>{{ selectedFunction.name }}</code>
          <p>{{ selectedFunction.description || '暂无函数说明' }}</p>
          <div class="function-meta-grid">
            <div>
              <em>返回</em>
              <b>{{ selectedFunction.returnType || 'ANY' }}</b>
            </div>
            <div>
              <em>参数</em>
              <b>{{ parseArgumentSchema(selectedFunction).length || '不限' }}</b>
            </div>
          </div>
        </article>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { getFormulaFunctions } from '@/api/formula'

const props = defineProps({
  value: {
    type: String,
    default: '',
  },
  fieldOptions: {
    type: Array,
    default: () => [],
  },
  allFields: {
    type: Array,
    default: () => [],
  },
  dependsOn: {
    type: Array,
    default: () => [],
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  placeholder: {
    type: String,
    default: '',
  },
  compact: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'update:value',
  'update:dependsOn',
  'insertToken',
  'insertStringToken',
])

const operatorTokens = ['+', '-', '*', '/', '(', ')', '==', '!=', '&&', '||']
const fieldKeyword = ref('')
const functionKeyword = ref('')
const expressionInputRef = ref(null)
const expandedFieldGroups = ref([])
const expandedFunctionGroups = ref([])
const functionLoading = ref(false)
const functionLoadError = ref('')
const functions = ref([])
const selectedFunctionCode = ref('')

const expressionAutosize = computed(() => {
  return props.compact
    ? { minRows: 12, maxRows: 16 }
    : { minRows: 14, maxRows: 18 }
})

const allowedFieldCodes = computed(() => {
  return new Set((props.fieldOptions || [])
    .map(item => item?.value)
    .filter(Boolean)
    .map(String))
})

const normalizedFields = computed(() => {
  const source = props.allFields?.length
    ? props.allFields
    : (props.fieldOptions || []).map(item => ({
        fieldCode: item.value,
        fieldName: item.label,
      }))
  return source
    .map(normalizeField)
    .filter((item) => {
      if (!item.code || item.raw?.fieldStatus === 'HIDDEN')
        return false
      return !allowedFieldCodes.value.size || allowedFieldCodes.value.has(item.code)
    })
})

const filteredFields = computed(() => {
  const keyword = fieldKeyword.value.trim().toLowerCase()
  if (!keyword)
    return normalizedFields.value
  return normalizedFields.value.filter((item) => {
    return [item.code, item.name, item.typeLabel, item.groupLabel]
      .some(value => String(value || '').toLowerCase().includes(keyword))
  })
})

const filteredFieldGroups = computed(() => {
  return buildGroupedList(filteredFields.value, item => item.groupKey, item => item.groupLabel)
})

const dependencyFields = computed(() => {
  const codes = Array.isArray(props.dependsOn) ? props.dependsOn : []
  return codes
    .map((code) => {
      return normalizedFields.value.find(item => item.code === code)
        || normalizeField({ fieldCode: code, fieldName: code })
    })
    .filter(item => item.code)
})

const normalizedFunctions = computed(() => {
  return functions.value.map(normalizeFunction).filter(item => item.name)
})

const filteredFunctions = computed(() => {
  const keyword = functionKeyword.value.trim().toLowerCase()
  if (!keyword)
    return normalizedFunctions.value
  return normalizedFunctions.value.filter((item) => {
    return [item.name, item.displayName, item.category, item.description, item.example, item.returnType]
      .some(value => String(value || '').toLowerCase().includes(keyword))
  })
})

const filteredFunctionGroups = computed(() => {
  return buildGroupedList(filteredFunctions.value, item => item.category || 'Other', item => formatFunctionCategory(item.category))
})

const selectedFunction = computed(() => {
  return filteredFunctions.value.find(item => item.name === selectedFunctionCode.value)
    || filteredFunctions.value[0]
    || null
})

onMounted(() => {
  resetFieldGroups()
  loadFunctions()
})

watch(filteredFieldGroups, () => {
  resetFieldGroups()
})

watch(filteredFunctionGroups, (groups) => {
  const keys = groups.map(item => item.key)
  if (!expandedFunctionGroups.value.length)
    expandedFunctionGroups.value = keys
  else
    expandedFunctionGroups.value = expandedFunctionGroups.value.filter(key => keys.includes(key))
  if (!filteredFunctions.value.some(item => item.name === selectedFunctionCode.value))
    selectedFunctionCode.value = filteredFunctions.value[0]?.name || ''
})

function handleExpressionChange(nextValue) {
  emit('update:value', nextValue)
}

function insertField(field) {
  if (props.disabled || !field?.code)
    return
  insertSnippet(field.code)
  appendDependency(field.code)
}

function insertOperator(token) {
  insertSnippet(token)
}

function insertNumberToken() {
  insertSnippet('0')
}

function insertStringToken() {
  insertSnippet('\'\'', { cursorOffset: 1 })
  emit('insertStringToken')
}

function selectAndInsertFunction(item) {
  if (!item?.name)
    return
  selectedFunctionCode.value = item.name
  if (props.disabled)
    return
  insertSnippet(buildFunctionSnippet(item), { functionSnippet: true })
}

function insertSnippet(snippet, options = {}) {
  const token = String(snippet || '')
  if (!token)
    return
  const current = String(props.value || '')
  const textarea = getTextareaElement()
  const start = textarea?.selectionStart ?? current.length
  const end = textarea?.selectionEnd ?? current.length
  const before = current.slice(0, start)
  const after = current.slice(end)
  const prefix = needsPrefixSpace(before, token) ? ' ' : ''
  const suffix = needsSuffixSpace(after, token) ? ' ' : ''
  const nextValue = `${before}${prefix}${token}${suffix}${after}`
  const cursorPosition = before.length + prefix.length + resolveCursorOffset(token, options)
  emit('update:value', nextValue)
  nextTick(() => {
    const nextTextarea = getTextareaElement()
    if (!nextTextarea)
      return
    nextTextarea.focus()
    nextTextarea.setSelectionRange(cursorPosition, cursorPosition)
  })
}

function resolveCursorOffset(token, options = {}) {
  if (Number.isInteger(options.cursorOffset))
    return options.cursorOffset
  if (options.functionSnippet) {
    const openIndex = token.indexOf('(')
    const closeIndex = token.lastIndexOf(')')
    if (openIndex >= 0 && closeIndex === openIndex + 1)
      return openIndex + 1
  }
  return token.length
}

function needsPrefixSpace(before, token) {
  if (!before)
    return false
  if (/\s$/.test(before))
    return false
  return !/^[),+\-*/%<>=&|]/.test(token)
}

function needsSuffixSpace(after, token) {
  if (!after)
    return false
  if (/^\s/.test(after))
    return false
  return !/[([+\-*/%<>=&|]$/.test(token)
}

function getTextareaElement() {
  const root = expressionInputRef.value?.$el
  return root?.querySelector?.('textarea') || null
}

function appendDependency(code) {
  const value = String(code || '').trim()
  if (!value)
    return
  const current = Array.isArray(props.dependsOn) ? props.dependsOn : []
  if (current.includes(value))
    return
  emit('update:dependsOn', [...current, value])
}

function removeDependency(code) {
  if (props.disabled)
    return
  const current = Array.isArray(props.dependsOn) ? props.dependsOn : []
  emit('update:dependsOn', current.filter(item => item !== code))
}

function formatExpression() {
  if (props.disabled)
    return
  const formatted = String(props.value || '')
    .replace(/\s+/g, ' ')
    .replace(/\s*([()+\-*/%,])\s*/g, ' $1 ')
    .replace(/\s*([=!<>]=|&&|\|\|)\s*/g, ' $1 ')
    .replace(/\s+/g, ' ')
    .trim()
  emit('update:value', formatted)
}

function clearExpression() {
  if (props.disabled)
    return
  emit('update:value', '')
  emit('update:dependsOn', [])
}

async function loadFunctions() {
  functionLoading.value = true
  functionLoadError.value = ''
  try {
    const res = await getFormulaFunctions()
    const rows = Array.isArray(res?.data) ? res.data : Array.isArray(res) ? res : []
    functions.value = rows
    if (!selectedFunctionCode.value && rows.length)
      selectedFunctionCode.value = normalizeFunction(rows[0]).name
  }
  catch (e) {
    functions.value = []
    functionLoadError.value = e?.message || '函数加载失败'
  }
  finally {
    functionLoading.value = false
  }
}

function normalizeField(item = {}) {
  const code = String(item.fieldCode || item.field || item.value || '').trim()
  const name = String(item.fieldName || item.label || item.name || code).replace(`（${code}）`, '')
  const type = resolveFieldType(item)
  const groupLabel = item.groupName || item.fieldGroup || item.category || resolveFieldGroup(type)
  return {
    code,
    name,
    raw: item,
    groupKey: groupLabel,
    groupLabel,
    typeLabel: type.label,
    typeShort: type.short,
    typeClass: type.className,
  }
}

function resolveFieldType(item = {}) {
  const fieldType = String(item.fieldType || item.businessFieldType || item.type || '').toUpperCase()
  const componentType = String(item.componentType || item.component || '').toLowerCase()
  const dataType = String(item.dataType || '').toLowerCase()
  const code = String(item.fieldCode || item.field || '').toLowerCase()
  if (['NUMBER', 'MONEY'].includes(fieldType) || ['int', 'integer', 'bigint', 'decimal', 'double', 'float'].includes(dataType) || componentType.includes('number') || includesAny(code, ['amount', 'count', 'qty', 'price', 'num'])) {
    return { label: '数字', short: '123', className: 'numeric' }
  }
  if (['DATE', 'DATETIME', 'TIME'].includes(fieldType) || ['date', 'datetime', 'time'].includes(componentType) || dataType.includes('date') || dataType.includes('time')) {
    return { label: '日期', short: '日', className: 'date' }
  }
  if (['DICT', 'RADIO', 'CHECKBOX', 'SWITCH'].includes(fieldType) || ['select', 'radio', 'checkbox', 'dictselect', 'switch'].includes(componentType)) {
    return { label: '选项', short: '选', className: 'option' }
  }
  if (['USER', 'DEPT', 'ORG', 'REGION', 'REFERENCE'].includes(fieldType) || ['userselect', 'orgtreeselect', 'regiontreeselect', 'objectreference'].includes(componentType)) {
    return { label: '关联', short: '链', className: 'relation' }
  }
  return { label: '文本', short: 'T', className: 'text' }
}

function resolveFieldGroup(type) {
  if (type.label === '数字')
    return '数值字段'
  if (type.label === '日期')
    return '日期字段'
  if (type.label === '关联')
    return '关联字段'
  if (type.label === '选项')
    return '选项字段'
  return '文本字段'
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

function buildGroupedList(items, keyGetter, labelGetter) {
  const groups = new Map()
  items.forEach((item) => {
    const key = String(keyGetter(item) || 'Other')
    if (!groups.has(key)) {
      groups.set(key, {
        key,
        label: labelGetter(item) || key,
        items: [],
      })
    }
    groups.get(key).items.push(item)
  })
  return Array.from(groups.values())
}

function buildFunctionSnippet(item = {}) {
  const name = String(item.name || '').trim()
  if (!name)
    return ''
  const args = parseArgumentSchema(item)
    .filter(argument => argument.required !== false)
    .map(argument => argument.name)
    .filter(Boolean)
  return `${name}(${args.join(', ')})`
}

function buildFunctionSignature(item = {}) {
  const args = parseArgumentSchema(item).map(argument => argument.name).filter(Boolean)
  return `${item.name || ''}(${args.join(', ')})`
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

function formatFunctionCategory(category) {
  const value = String(category || 'Other')
  const normalized = value.toLowerCase()
  if (normalized.includes('math'))
    return '数学函数'
  if (normalized.includes('string') || normalized.includes('text'))
    return '文本函数'
  if (normalized.includes('date') || normalized.includes('time'))
    return '日期和时间函数'
  if (normalized.includes('logic') || normalized.includes('condition'))
    return '逻辑函数'
  if (normalized.includes('stat') || normalized.includes('aggregate'))
    return '统计函数'
  if (normalized.includes('custom'))
    return '自定义函数'
  return value
}

function toggleFieldGroup(key) {
  toggleKey(expandedFieldGroups, key)
}

function toggleFunctionGroup(key) {
  toggleKey(expandedFunctionGroups, key)
}

function toggleKey(targetRef, key) {
  if (targetRef.value.includes(key)) {
    targetRef.value = targetRef.value.filter(item => item !== key)
    return
  }
  targetRef.value = [...targetRef.value, key]
}

function isFieldGroupExpanded(key) {
  return expandedFieldGroups.value.includes(key)
}

function isFunctionGroupExpanded(key) {
  return expandedFunctionGroups.value.includes(key)
}

function resetFieldGroups() {
  expandedFieldGroups.value = filteredFieldGroups.value.map(item => item.key)
}

function includesAny(value, keywords = []) {
  return keywords.some(keyword => value.includes(keyword))
}
</script>

<style scoped>
.expression-workbench {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  width: 100%;
  min-width: 0;
  min-height: 560px;
  border: 0;
  border-radius: 0;
  background: var(--bg-primary);
  padding: 0;
}

.expression-workbench.compact {
  gap: 8px;
  min-height: 0;
  padding: 0;
}

.expression-workbench.compact .expression-workbench-head {
  min-height: 0;
}

.expression-workbench.compact .expression-workbench-head strong {
  font-size: 14px;
}

.expression-workbench.compact .expression-workbench-head span {
  display: none;
}

.expression-workbench-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.expression-workbench-head > div:first-child {
  display: grid;
  gap: 4px;
}

.expression-workbench-head strong {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 800;
}

.expression-workbench-head span {
  color: var(--text-secondary);
  font-size: 12px;
}

.expression-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.expression-layout {
  display: grid;
  grid-template-columns: minmax(240px, 280px) minmax(620px, 1fr) minmax(360px, 420px);
  align-items: stretch;
  gap: 10px;
  width: 100%;
  min-height: 520px;
  min-width: 0;
  height: 100%;
}

.expression-workbench.compact .expression-layout {
  gap: 8px;
  grid-template-columns: minmax(230px, 270px) minmax(600px, 1fr) minmax(350px, 400px);
  min-height: 0;
  height: 100%;
}

.field-panel,
.expression-panel,
.function-panel {
  align-self: stretch;
  box-sizing: border-box;
  min-width: 0;
  min-height: 0;
  height: 100%;
  border: 1px solid #dce4ef;
  border-radius: 8px;
  background: var(--bg-primary);
  overflow: hidden;
}

.field-panel,
.function-panel {
  display: grid;
  gap: 8px;
  padding: 8px;
}

.field-panel {
  grid-template-rows: auto auto minmax(0, 1fr);
}

.function-panel {
  grid-template-rows: auto auto minmax(0, 1fr) auto;
}

.expression-workbench.compact .field-panel,
.expression-workbench.compact .function-panel {
  gap: 7px;
  padding: 8px;
}

.expression-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
}

.panel-title,
.expression-panel-head,
.dependency-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.panel-title strong,
.expression-panel-head > span,
.dependency-head span {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 800;
}

.field-groups,
.function-tree {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 0;
  height: 100%;
  overflow-y: auto;
  padding-right: 2px;
}

.field-group,
.function-group {
  display: grid;
  gap: 6px;
}

.field-group-head,
.function-group-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px;
  width: 100%;
  height: 30px;
  border: 0;
  border-radius: 6px;
  background: #f3f6fa;
  color: var(--text-primary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
  padding: 0 8px;
  text-align: left;
}

.expression-workbench.compact .field-group-head,
.expression-workbench.compact .function-group-head {
  height: 26px;
  font-size: 11px;
}

.field-group-head span,
.function-group-head span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-group-head em,
.function-group-head em,
.dependency-head em {
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
}

.field-list,
.function-list {
  display: grid;
  gap: 5px;
}

.field-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 38px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-primary);
  cursor: pointer;
  padding: 6px 7px;
  text-align: left;
}

.expression-workbench.compact .field-item {
  min-height: 42px;
  padding: 6px;
}

.expression-workbench.compact .field-type-label {
  display: none;
}

.field-item:hover:not(:disabled) {
  border-color: #9cc4ff;
  background: #f4f8ff;
}

.field-item:disabled,
.function-item:disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.field-type-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 20px;
  border-radius: 5px;
  font-size: 10px;
  font-weight: 900;
  line-height: 1;
}

.field-type-mark.numeric {
  background: #e8f2ff;
  color: #2563eb;
}

.field-type-mark.date {
  background: #e8f7f3;
  color: #0f766e;
}

.field-type-mark.option {
  background: #fff5db;
  color: #a16207;
}

.field-type-mark.relation {
  background: #eef2ff;
  color: #4f46e5;
}

.field-type-mark.text {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.field-main {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.field-main strong {
  overflow: hidden;
  color: #1d3557;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-main em {
  overflow: hidden;
  color: var(--text-secondary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-type-label {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 11px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.expression-panel-head {
  min-height: 44px;
  border-bottom: 1px solid var(--border-light);
  padding: 8px 10px;
}

.expression-workbench.compact .expression-panel-head {
  min-height: 36px;
  padding: 5px 8px;
}

.operator-toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 5px;
}

.operator-toolbar button {
  min-width: 30px;
  height: 26px;
  border: 1px solid #d7dfeb;
  border-radius: 5px;
  background: var(--bg-secondary);
  color: #253247;
  cursor: pointer;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 800;
  padding: 0 7px;
}

.expression-workbench.compact .operator-toolbar button {
  min-width: 27px;
  height: 24px;
  font-size: 11px;
  padding: 0 6px;
}

.operator-toolbar button:hover:not(:disabled) {
  border-color: #2563eb;
  color: #1d4ed8;
}

.operator-toolbar button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.expression-codebox {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  min-height: 0;
  background: #fbfdff;
}

.line-gutter {
  border-right: 1px solid var(--border-light);
  background: #f4f7fb;
  color: var(--text-tertiary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 32px;
  padding-top: 13px;
  text-align: center;
}

.expression-workbench.compact .line-gutter {
  padding-top: 9px;
}

.expression-input {
  min-width: 0;
  min-height: 0;
  height: 100%;
}

.expression-input :deep(.n-input) {
  height: 100%;
  border-radius: 0;
  background: transparent;
}

.expression-input :deep(.n-input-wrapper),
.expression-input :deep(.n-input__textarea) {
  min-height: 0;
  height: 100%;
}

.expression-input :deep(.n-input .n-input__border),
.expression-input :deep(.n-input .n-input__state-border) {
  display: none;
}

.expression-input :deep(.n-input__textarea-el) {
  min-height: 380px;
  padding: 16px 18px;
  color: var(--text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 14px;
  line-height: 1.75;
}

.expression-workbench.compact .expression-input :deep(.n-input__textarea-el) {
  min-height: 330px;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.6;
}

.dependency-strip {
  display: grid;
  gap: 8px;
  border-top: 1px solid var(--border-light);
  background: var(--bg-primary);
  padding: 10px;
}

.expression-workbench.compact .dependency-strip {
  gap: 6px;
  padding: 7px 8px;
}

.dependency-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.dependency-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  max-width: 100%;
  height: 28px;
  border: 1px solid #cfe0ff;
  border-radius: 6px;
  background: #f0f6ff;
  color: #1d4ed8;
  cursor: pointer;
  padding: 0 8px;
}

.expression-workbench.compact .dependency-chip {
  height: 24px;
  padding: 0 7px;
}

.dependency-chip strong {
  overflow: hidden;
  max-width: 160px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dependency-chip span {
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
}

.dependency-chip i {
  color: var(--text-secondary);
  font-size: 14px;
}

.function-tree {
  max-height: none;
}

.function-panel :deep(.n-spin-container),
.function-panel :deep(.n-spin-content) {
  display: grid;
  min-height: 0;
  height: 100%;
}

.expression-workbench.compact .function-tree {
  max-height: none;
}

.function-item {
  display: grid;
  gap: 4px;
  width: 100%;
  border: 1px solid #e1e7f0;
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-primary);
  cursor: pointer;
  padding: 8px;
  text-align: left;
}

.expression-workbench.compact .function-item {
  gap: 2px;
  padding: 6px 7px;
}

.function-item:hover:not(:disabled) {
  border-color: #6ee7b7;
  background: #f0fdf9;
}

.function-item.active {
  border-color: #2563eb;
  background: #eef6ff;
  box-shadow: inset 3px 0 0 #2563eb;
}

.function-signature {
  overflow: hidden;
  color: #1d3557;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.function-description {
  display: -webkit-box;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.expression-workbench.compact .function-description {
  -webkit-line-clamp: 2;
}

.function-example {
  overflow: hidden;
  color: #0f766e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.expression-workbench.compact .function-example {
  display: block;
}

.function-inspector {
  display: grid;
  gap: 7px;
  border-top: 1px solid var(--border-light);
  padding-top: 10px;
}

.expression-workbench.compact .function-inspector {
  gap: 5px;
  padding-top: 7px;
}

.expression-workbench.compact .function-inspector p,
.expression-workbench.compact .function-meta-grid {
  display: grid;
}

.expression-workbench.compact .function-inspector p {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.function-inspector > span {
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 700;
}

.function-inspector strong {
  color: var(--text-primary);
  font-size: 14px;
}

.function-inspector code {
  overflow-x: auto;
  border-radius: 6px;
  background: var(--bg-secondary);
  color: #1d3557;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  padding: 6px 7px;
}

.function-inspector p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.55;
}

.function-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.function-meta-grid div {
  display: grid;
  gap: 2px;
  border-radius: 6px;
  background: var(--bg-secondary);
  padding: 7px;
}

.function-meta-grid em {
  color: var(--text-secondary);
  font-size: 11px;
  font-style: normal;
}

.function-meta-grid b {
  color: var(--text-primary);
  font-size: 12px;
}

@media (max-width: 1120px) {
  .expression-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .field-panel,
  .function-panel {
    max-height: none;
  }

  .function-tree {
    max-height: none;
  }
}

@media (max-width: 720px) {
  .expression-workbench-head,
  .expression-panel-head {
    align-items: stretch;
    flex-direction: column;
  }

  .expression-actions,
  .operator-toolbar {
    justify-content: flex-start;
  }

  .expression-codebox {
    grid-template-columns: 34px minmax(0, 1fr);
  }
}
</style>
