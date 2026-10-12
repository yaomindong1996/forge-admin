<template>
  <div class="flow-form-create-designer">
    <div class="designer-toolbar">
      <div>
        <div class="designer-title">
          FormCreate 动态表单
        </div>
        <div class="designer-subtitle">
          右侧基础配置中的“字段”就是变量/业务字段 ID，可按业务模型字段维护
        </div>
      </div>
      <n-space size="small">
        <n-button size="small" @click="handleReset">
          清空
        </n-button>
        <n-button size="small" type="primary" @click="handleSave">
          保存配置
        </n-button>
      </n-space>
    </div>
    <div class="designer-shell">
      <FcDesigner
        ref="designerRef"
        :height="height"
        :config="designerConfig"
        :locale="designerLocale"
        @create="queueFlush"
        @copy="queueFlush"
        @delete="queueFlush"
        @drag="queueFlush"
        @paste-rule="queueFlush"
        @sort-up="queueFlush"
        @sort-down="queueFlush"
        @change-field="queueFlush"
      />
    </div>
  </div>
</template>

<script setup>
import FcDesigner from '@form-create/designer'
import designerZhCn from '@form-create/designer/locale/zh-cn.es'
import { getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { hydrateForgeBusinessPreviewRules, installForgeBusinessComponents } from '@/views/app-center/components/designer/form-first/forgeBusinessComponents'
import { buildDefaultFormOptions, cloneValue, DEFAULT_FORM_ITEM_GAP, installFormCreate, normalizeFormCreateRules } from './formCreateBridge'

const props = defineProps({
  modelValue: {
    type: [Array, String],
    default: () => [],
  },
  height: {
    type: String,
    default: '100%',
  },
})
const emit = defineEmits(['update:modelValue', 'save'])
const FORM_ITEM_GAP_OPTION_KEY = 'forgeFormItemGap'
const MIN_FORM_ITEM_GAP = 0
const MAX_FORM_ITEM_GAP = 80

installFormCreate(getCurrentInstance()?.appContext?.app)

const designerRef = ref(null)
const syncingDesigner = ref(false)
const flushTimer = ref(null)
const previewHydrateTimer = ref(null)
const localRulesSnapshot = ref('')
const formItemGap = ref(parseFormItemGapValue(DEFAULT_FORM_ITEM_GAP))
const designerLocale = designerZhCn
let loadSeq = 0
let destroyed = false

const designerConfig = {
  showAi: false,
  showInputData: false,
  showLanguage: false,
  showDevice: false,
  showComponentName: false,
  fieldReadonly: false,
  showSaveBtn: false,
  showPreviewBtn: true,
  showJsonPreview: true,
  exitConfirm: false,
  formOptions: buildDesignerFormOptions(),
  formRule: {
    append: true,
    rule: () => [
      {
        type: 'inputNumber',
        field: FORM_ITEM_GAP_OPTION_KEY,
        title: '字段行间距（px）',
        value: parseFormItemGapValue(DEFAULT_FORM_ITEM_GAP),
        props: {
          min: MIN_FORM_ITEM_GAP,
          max: MAX_FORM_ITEM_GAP,
          step: 2,
          precision: 0,
          controlsPosition: 'right',
          placeholder: '默认 20',
        },
        on: {
          change: handleFormItemGapChange,
        },
      },
    ],
  },
}

watch(
  () => props.modelValue,
  () => loadDesignerRules(),
  { deep: true },
)

onMounted(() => {
  destroyed = false
  ensureForgeBusinessComponents()
  loadDesignerRules()
})

onBeforeUnmount(() => {
  destroyed = true
  loadSeq += 1
  if (flushTimer.value)
    window.clearTimeout(flushTimer.value)
  if (previewHydrateTimer.value)
    window.clearTimeout(previewHydrateTimer.value)
  flushTimer.value = null
  previewHydrateTimer.value = null
})

function loadDesignerRules() {
  const seq = ++loadSeq
  nextTick(async () => {
    if (destroyed || seq !== loadSeq || !designerRef.value)
      return
    ensureForgeBusinessComponents()
    const normalizedRules = normalizeFormCreateRules(props.modelValue)
    const gap = resolveFormItemGapFromRules(normalizedRules)
    formItemGap.value = gap
    const rules = applyFormItemGapToRules(await hydrateForgeBusinessPreviewRules(normalizedRules), gap)
    if (destroyed || seq !== loadSeq || !designerRef.value)
      return
    const snapshot = JSON.stringify(rules)
    if (snapshot === localRulesSnapshot.value)
      return
    syncingDesigner.value = true
    designerRef.value.setRule(cloneValue(rules))
    designerRef.value.setOption(buildDesignerFormOptions(gap))
    localRulesSnapshot.value = snapshot
    nextTick(() => {
      syncingDesigner.value = false
    })
  })
}

function queueFlush() {
  if (destroyed || syncingDesigner.value)
    return
  if (flushTimer.value)
    window.clearTimeout(flushTimer.value)
  flushTimer.value = window.setTimeout(() => {
    flushTimer.value = null
    flushDesigner()
    queuePreviewHydration()
  }, 160)
}

function flushDesigner() {
  if (destroyed || !designerRef.value)
    return []
  const rules = designerRef.value.getRule?.() || []
  const gap = readDesignerFormItemGap()
  const clonedRules = applyFormItemGapToRules(normalizeFormCreateRules(rules), gap)
  localRulesSnapshot.value = JSON.stringify(clonedRules)
  emit('update:modelValue', clonedRules)
  return clonedRules
}

function ensureForgeBusinessComponents() {
  installForgeBusinessComponents(designerRef.value, resolveCurrentFields(), {
    installBaseRules: false,
  })
}

function resolveCurrentFields() {
  return normalizeFormCreateRules(props.modelValue)
    .map((rule) => {
      const fieldCode = rule.field || rule.fieldName || rule.name || ''
      if (!fieldCode)
        return null
      return {
        fieldCode,
        fieldName: rule.title || rule.label || fieldCode,
      }
    })
    .filter(Boolean)
}

function queuePreviewHydration() {
  if (previewHydrateTimer.value)
    window.clearTimeout(previewHydrateTimer.value)
  previewHydrateTimer.value = window.setTimeout(() => {
    previewHydrateTimer.value = null
    refreshPreviewOptions()
  }, 220)
}

async function refreshPreviewOptions() {
  const seq = loadSeq
  if (destroyed || !designerRef.value || syncingDesigner.value)
    return
  const rules = designerRef.value.getRule?.() || []
  const gap = readDesignerFormItemGap()
  const hydratedRules = applyFormItemGapToRules(await hydrateForgeBusinessPreviewRules(rules), gap)
  const designer = designerRef.value
  if (destroyed || seq !== loadSeq || !designer || syncingDesigner.value)
    return
  const nextSnapshot = JSON.stringify(hydratedRules)
  if (nextSnapshot === localRulesSnapshot.value)
    return
  syncingDesigner.value = true
  designer.setRule?.(cloneValue(hydratedRules))
  localRulesSnapshot.value = nextSnapshot
  nextTick(() => {
    if (destroyed)
      return
    syncingDesigner.value = false
  })
}

function handleFormItemGapChange(value) {
  if (destroyed || syncingDesigner.value)
    return
  const gap = parseFormItemGapValue(value)
  if (gap === formItemGap.value)
    return
  formItemGap.value = gap
  applyDesignerFormItemGap()
}

function applyDesignerFormItemGap() {
  if (destroyed || !designerRef.value)
    return
  const rules = applyFormItemGapToRules(normalizeFormCreateRules(designerRef.value.getRule?.() || []), formItemGap.value)
  const snapshot = JSON.stringify(rules)
  syncingDesigner.value = true
  designerRef.value.setRule?.(cloneValue(rules))
  designerRef.value.setOption?.(buildDesignerFormOptions(formItemGap.value))
  localRulesSnapshot.value = snapshot
  emit('update:modelValue', rules)
  nextTick(() => {
    if (destroyed)
      return
    syncingDesigner.value = false
  })
}

function handleReset() {
  if (!designerRef.value)
    return
  designerRef.value.setRule([])
  flushDesigner()
}

function handleSave() {
  const rules = flushDesigner()
  emit('save', rules)
}

function getRules() {
  return flushDesigner()
}

defineExpose({
  getRules,
})

function buildDesignerFormOptions(gap = formItemGap.value) {
  const options = buildDefaultFormOptions()
  return {
    ...options,
    form: {
      ...options.form,
      [FORM_ITEM_GAP_OPTION_KEY]: parseFormItemGapValue(gap),
    },
  }
}

function readDesignerFormItemGap() {
  const options = designerRef.value?.getOptions?.() || designerRef.value?.getOption?.() || {}
  const gap = parseFormItemGapValue(options?.form?.[FORM_ITEM_GAP_OPTION_KEY])
  formItemGap.value = gap
  return gap
}

function resolveFormItemGapFromRules(rules = []) {
  let resolved = null
  walkRules(rules, (rule) => {
    if (resolved !== null)
      return
    const value = rule?.wrap?.style?.marginBottom
    if (value !== undefined && value !== null && value !== '')
      resolved = parseFormItemGapValue(value)
  })
  return resolved ?? parseFormItemGapValue(DEFAULT_FORM_ITEM_GAP)
}

function applyFormItemGapToRules(rules = [], gap = formItemGap.value) {
  const marginBottom = `${parseFormItemGapValue(gap)}px`
  const nextRules = cloneValue(Array.isArray(rules) ? rules : [])
  walkRules(nextRules, (rule) => {
    if (!rule || typeof rule !== 'object')
      return
    const wrap = rule.wrap && typeof rule.wrap === 'object' && !Array.isArray(rule.wrap)
      ? rule.wrap
      : {}
    const style = wrap.style && typeof wrap.style === 'object' && !Array.isArray(wrap.style)
      ? wrap.style
      : {}
    style.marginBottom = marginBottom
    wrap.style = style
    rule.wrap = wrap
  })
  return nextRules
}

function walkRules(rules = [], visitor) {
  if (!Array.isArray(rules))
    return
  rules.forEach((rule) => {
    visitor(rule)
    if (Array.isArray(rule?.children))
      walkRules(rule.children, visitor)
  })
}

function parseFormItemGapValue(value) {
  const numberValue = typeof value === 'number'
    ? value
    : Number.parseFloat(String(value ?? DEFAULT_FORM_ITEM_GAP).replace('px', ''))
  if (!Number.isFinite(numberValue))
    return parseFormItemGapValue(DEFAULT_FORM_ITEM_GAP)
  return Math.min(MAX_FORM_ITEM_GAP, Math.max(MIN_FORM_ITEM_GAP, Math.round(numberValue)))
}
</script>

<style scoped>
.flow-form-create-designer {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #d7dde7;
  border-radius: 8px;
  background: var(--bg-primary);
  overflow: hidden;
}

.designer-toolbar {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 16px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-secondary);
}

.designer-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.designer-subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: #667085;
}

.designer-shell {
  flex: 1;
  min-width: 0;
  min-height: 0;
  background: var(--bg-secondary);
  overflow: auto;
}

:deep(.fc-designer),
:deep(._fc-designer) {
  --fc-primary: #2563eb;
  border: 0;
  min-width: 1060px;
  height: 100%;
}

:deep(.fc-designer ._fc-l),
:deep(.fc-designer ._fc-r),
:deep(._fc-designer ._fc-l),
:deep(._fc-designer ._fc-r) {
  background: var(--bg-primary);
}

@media (max-width: 900px) {
  .designer-toolbar {
    min-height: auto;
    align-items: flex-start;
    flex-direction: column;
    padding: 12px;
  }
}
</style>
