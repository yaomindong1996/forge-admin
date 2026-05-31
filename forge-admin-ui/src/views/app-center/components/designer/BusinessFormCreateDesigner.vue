<template>
  <div class="business-form-create-designer">
    <div class="form-create-toolbar">
      <div>
        <h3>表单设计</h3>
        <p>使用组件画布设计最终表单，字段绑定会同步到业务对象字段资产。</p>
      </div>
      <n-space size="small">
        <n-button size="small" secondary @click="resetFromFields">
          按字段生成
        </n-button>
        <n-button size="small" secondary @click="repairRefs">
          清理失效字段
        </n-button>
      </n-space>
    </div>
    <div class="form-create-canvas">
      <FcDesigner
        ref="designerRef"
        :height="height"
        :config="designerConfig"
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
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { cloneValue, installFormCreate } from '@/components/form-create/formCreateBridge'
import { repairFormDesignerFieldRefs } from './form-first/fieldReferenceUtils'
import { hydrateForgeBusinessPreviewRules, installForgeBusinessComponents } from './form-first/forgeBusinessComponents'
import { forgeSchemaToFormCreate } from './form-first/forgeToFormCreate'
import { formCreateToForgeSchema } from './form-first/formCreateToForge'
import { createComponentFromField, createDefaultFormDesignerSchema, normalizeFormDesignerSchema } from './form-first/formDesignerSchema'

const props = defineProps({
  modelValue: {
    type: Object,
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  objectCode: {
    type: String,
    default: '',
  },
  objectName: {
    type: String,
    default: '',
  },
  height: {
    type: String,
    default: '680px',
  },
})

const emit = defineEmits(['update:modelValue', 'dirtyChange'])

installFormCreate(getCurrentInstance()?.appContext?.app)

const designerRef = ref(null)
const syncingDesigner = ref(false)
const flushTimer = ref(null)
const previewHydrateTimer = ref(null)
const localSnapshot = ref('')
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
  formOptions: {
    form: {
      labelPosition: 'right',
      labelWidth: '100px',
      size: 'default',
    },
    submitBtn: false,
    resetBtn: false,
  },
}

const normalizedSchema = computed(() => normalizeFormDesignerSchema(props.modelValue || createDefaultFormDesignerSchema({
  objectCode: props.objectCode,
  objectName: props.objectName,
  fields: props.fields,
})))

watch(
  () => [props.modelValue, props.fields, props.objectCode, props.objectName],
  () => loadDesigner(),
  { deep: true },
)

onMounted(() => {
  destroyed = false
  ensureForgeBusinessComponents()
  loadDesigner()
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

function loadDesigner() {
  const seq = ++loadSeq
  nextTick(async () => {
    if (destroyed || seq !== loadSeq || !designerRef.value)
      return
    ensureForgeBusinessComponents()
    const { rules: rawRules, options } = forgeSchemaToFormCreate({
      schema: normalizedSchema.value,
      fields: props.fields,
    })
    const rules = await hydrateForgeBusinessPreviewRules(rawRules)
    const designer = designerRef.value
    if (destroyed || seq !== loadSeq || !designer)
      return
    const snapshot = JSON.stringify({ rules, options })
    if (snapshot === localSnapshot.value)
      return
    syncingDesigner.value = true
    designer.setRule?.(cloneValue(rules))
    designer.setOption?.(cloneValue(options))
    localSnapshot.value = snapshot
    nextTick(() => {
      if (destroyed)
        return
      syncingDesigner.value = false
    })
  })
}

function ensureForgeBusinessComponents() {
  installForgeBusinessComponents(designerRef.value)
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
    return normalizedSchema.value
  const rules = designerRef.value.getRule?.() || []
  const options = designerRef.value.getOptions?.() || designerRef.value.getOption?.() || {}
  const schema = formCreateToForgeSchema({
    rules,
    options,
    objectCode: props.objectCode,
    objectName: props.objectName,
    formKey: normalizedSchema.value.formKey,
    formName: normalizedSchema.value.formName,
    fields: props.fields,
  })
  localSnapshot.value = JSON.stringify({ rules, options })
  emit('update:modelValue', schema)
  emit('dirtyChange', true)
  return schema
}

function queuePreviewHydration() {
  if (previewHydrateTimer.value)
    window.clearTimeout(previewHydrateTimer.value)
  previewHydrateTimer.value = window.setTimeout(() => refreshPreviewOptions(), 220)
}

async function refreshPreviewOptions() {
  const seq = loadSeq
  if (destroyed || !designerRef.value || syncingDesigner.value)
    return
  const rules = designerRef.value.getRule?.() || []
  const options = designerRef.value.getOptions?.() || designerRef.value.getOption?.() || {}
  const hydratedRules = await hydrateForgeBusinessPreviewRules(rules)
  const designer = designerRef.value
  if (destroyed || seq !== loadSeq || !designer || syncingDesigner.value)
    return
  const nextSnapshot = JSON.stringify({ rules: hydratedRules, options })
  if (nextSnapshot === localSnapshot.value)
    return
  syncingDesigner.value = true
  designer.setRule?.(cloneValue(hydratedRules))
  localSnapshot.value = nextSnapshot
  nextTick(() => {
    if (destroyed)
      return
    syncingDesigner.value = false
  })
}

function resetFromFields() {
  const schema = createDefaultFormDesignerSchema({
    objectCode: props.objectCode,
    objectName: props.objectName,
    fields: props.fields,
  })
  emit('update:modelValue', schema)
  emit('dirtyChange', true)
  nextTick(loadDesigner)
}

function repairRefs() {
  const schema = repairFormDesignerFieldRefs(flushDesigner(), props.fields, 'mark')
  emit('update:modelValue', schema)
  emit('dirtyChange', true)
  nextTick(loadDesigner)
}

function appendField(field = {}) {
  const fieldCode = field.fieldCode || field.field
  if (!fieldCode)
    return
  const schema = normalizeFormDesignerSchema(props.modelValue || normalizedSchema.value)
  if (schema.components.some(component => component.fieldBinding?.fieldCode === fieldCode))
    return
  schema.components.push(createComponentFromField(field, schema.components.length))
  emit('update:modelValue', schema)
  emit('dirtyChange', true)
  nextTick(loadDesigner)
}

defineExpose({
  flushDesigner,
  resetFromFields,
  repairRefs,
  appendField,
})
</script>

<style scoped>
.business-form-create-designer {
  min-width: 0;
  min-height: 720px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.form-create-toolbar {
  min-height: 62px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.form-create-toolbar h3,
.form-create-toolbar p {
  margin: 0;
}

.form-create-toolbar h3 {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.form-create-toolbar p {
  margin-top: 3px;
  font-size: 12px;
  color: #64748b;
}

.form-create-canvas {
  min-width: 0;
  min-height: 0;
  background: #f8fafc;
  overflow: auto;
}

:deep(.fc-designer),
:deep(._fc-designer) {
  --fc-primary: #2563eb;
  border: 0;
  min-width: 1060px;
}

:deep(.fc-designer ._fc-l),
:deep(.fc-designer ._fc-r),
:deep(._fc-designer ._fc-l),
:deep(._fc-designer ._fc-r) {
  background: #fff;
}

@media (max-width: 900px) {
  .business-form-create-designer {
    min-height: 660px;
  }

  .form-create-toolbar {
    align-items: flex-start;
  }
}
</style>
