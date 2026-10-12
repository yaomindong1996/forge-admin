<template>
  <div class="business-flow-form-asset-select">
    <template v-if="formMode !== 'EXTERNAL'">
      <div v-if="availableAssets.length" class="asset-card-grid">
        <button
          v-for="asset in availableAssets"
          :key="assetKey(asset)"
          type="button"
          class="asset-card"
          :class="{ selected: selectedAssetKey === assetKey(asset) }"
          :disabled="disabled"
          @click="handleAssetChange(assetKey(asset))"
        >
          <span class="asset-card__head">
            <span class="asset-card__title">{{ asset.formName || asset.formKey || '未命名表单' }}</span>
            <span class="asset-card__source" :class="sourceClass(asset)">
              {{ sourceLabel(asset) }}
            </span>
          </span>
          <span class="asset-card__meta">
            {{ pageLabel(asset) }} · {{ resolveFieldCount(asset) }} 个字段
          </span>
          <span class="asset-card__preview">
            {{ fieldPreviewText(asset) }}
          </span>
        </button>
      </div>
      <n-empty v-else size="small" description="暂无可用表单资产" />
      <n-button
        v-if="selectedAssetKey && !disabled"
        class="asset-clear-button"
        text
        size="small"
        @click="handleAssetChange(null)"
      >
        清除绑定
      </n-button>
    </template>
    <n-collapse v-else display-directive="show" class="external-form-collapse">
      <n-collapse-item title="开发者高级配置" name="external">
        <n-input
          :value="nodeForm.formUrl"
          placeholder="外部表单地址"
          @update:value="value => emit('update', { formUrl: value || '' })"
        />
      </n-collapse-item>
    </n-collapse>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  nodeForm: {
    type: Object,
    required: true,
  },
  formAssets: {
    type: Array,
    default: () => [],
  },
  disabled: Boolean,
  showAllModes: Boolean,
})

const emit = defineEmits(['update'])

const formMode = computed(() => normalizeMode(props.nodeForm?.formMode))
const availableAssets = computed(() => props.formAssets
  .filter(asset => props.showAllModes || normalizeMode(asset.formMode || asset.type) === formMode.value)
  .filter(asset => asset.formKey))
const selectedAssetKey = computed(() => {
  const formKey = normalizeText(props.nodeForm?.formKey)
  if (!formKey)
    return null
  const providerKey = normalizeText(props.nodeForm?.providerKey)
  const currentMode = normalizeMode(props.nodeForm?.formMode || props.nodeForm?.formRef?.formMode || props.nodeForm?.formRef?.type)
  const exactAsset = availableAssets.value.find(asset =>
    normalizeText(asset.formKey) === formKey
    && normalizeMode(asset.formMode || asset.type) === currentMode
    && normalizeText(asset.providerKey) === providerKey,
  )
  if (exactAsset)
    return assetKey(exactAsset)
  const current = availableAssets.value.find(asset =>
    normalizeText(asset.formKey) === formKey
    && (!providerKey || normalizeText(asset.providerKey) === providerKey),
  )
  return current ? assetKey(current) : null
})

function handleAssetChange(value) {
  const asset = availableAssets.value.find(item => assetKey(item) === value)
  if (!asset) {
    emit('update', {
      formKey: '',
      formName: '',
      providerKey: '',
      formUrl: '',
      formRef: {},
    })
    return
  }
  const normalizedMode = normalizeMode(asset.formMode || asset.type, asset.source)
  const providerKey = asset.providerKey || ''
  emit('update', {
    formMode: normalizedMode === 'DYNAMIC' ? '' : normalizedMode,
    source: asset.source || '',
    formKey: asset.formKey || '',
    formName: asset.formName || asset.formKey || '',
    providerKey,
    applicationId: asset.applicationId || '',
    pageId: asset.pageId || '',
    pageCode: asset.pageCode || '',
    pageName: asset.pageName || '',
    pageType: asset.pageType || '',
    sourceFormKey: asset.sourceFormKey || '',
    formUrl: asset.formUrl || '',
    viewKey: asset.viewKey || 'default',
    formRef: normalizedMode === 'BUSINESS_CODE_FORM'
      ? {
          type: 'BUSINESS_CODE_FORM',
          formMode: 'BUSINESS_CODE_FORM',
          objectCode: asset.objectCode || '',
          objectName: asset.objectName || '',
          providerKey,
          formKey: asset.formKey || '',
          formName: asset.formName || asset.formKey || '',
          formUrl: asset.formUrl || '',
          viewKey: asset.viewKey || 'default',
        }
      : normalizedMode === 'DYNAMIC'
        ? {
            type: 'DYNAMIC',
            formMode: '',
            formKey: asset.formKey || '',
            formName: asset.formName || asset.formKey || '',
            viewKey: asset.viewKey || 'default',
          }
        : {
            type: 'BUSINESS_OBJECT_FORM',
            formMode: 'BUSINESS_OBJECT_FORM',
            objectCode: asset.objectCode || '',
            objectName: asset.objectName || '',
            applicationId: asset.applicationId || '',
            pageId: asset.pageId || '',
            pageCode: asset.pageCode || '',
            pageName: asset.pageName || '',
            pageType: asset.pageType || '',
            sourceFormKey: asset.sourceFormKey || '',
            formKey: asset.formKey || '',
            formName: asset.formName || asset.formKey || '',
            viewKey: asset.viewKey || 'default',
          },
  })
}

function pageLabel(asset = {}) {
  const pageName = normalizeText(asset.pageName)
  const pageId = normalizeText(asset.pageId)
  const formKey = normalizeText(asset.formKey)
  if (pageName || pageId)
    return `页面：${pageName || pageId}`
  return formKey || '-'
}

function assetKey(asset = {}) {
  return `${normalizeMode(asset.formMode || asset.type)}::${asset.providerKey || ''}::${asset.formKey || ''}`
}

function sourceLabel(asset = {}) {
  const type = String(asset.sourceType || asset.source || asset.formMode || asset.type || '').toLowerCase()
  if (type.includes('flow'))
    return '流程表单'
  if (type.includes('code') || normalizeMode(asset.formMode || asset.type, asset.source) === 'BUSINESS_CODE_FORM')
    return asset.providerName || '代码 Provider'
  if (type.includes('external'))
    return '外部地址'
  return asset.objectName ? '业务对象' : '业务表单'
}

function sourceClass(asset = {}) {
  const mode = normalizeMode(asset.formMode || asset.type)
  if (mode === 'BUSINESS_CODE_FORM')
    return 'code'
  if (mode === 'EXTERNAL')
    return 'external'
  return 'business'
}

function resolveFieldCount(asset = {}) {
  if (Number.isFinite(Number(asset.fieldCount)))
    return Number(asset.fieldCount)
  const fields = Array.isArray(asset.fieldCatalog) ? asset.fieldCatalog : Array.isArray(asset.fields) ? asset.fields : []
  return fields.length
}

function fieldPreviewText(asset = {}) {
  const fields = Array.isArray(asset.fieldCatalog)
    ? asset.fieldCatalog
    : Array.isArray(asset.fields) ? asset.fields : []
  const preview = Array.isArray(asset.fieldPreview) && asset.fieldPreview.length
    ? asset.fieldPreview
    : fields
        .map(field => field?.label || field?.fieldName || field?.field || field?.fieldCode)
        .filter(Boolean)
        .slice(0, 5)
  return preview.length ? `字段：${preview.join(' / ')}` : '暂无字段预览'
}

function normalizeMode(value, source = '') {
  const normalized = String(value || '').trim().toUpperCase()
  if (normalized === 'BUSINESS_CODE_FORM' || normalized === 'EXTERNAL' || normalized === 'BUSINESS_OBJECT_FORM')
    return normalized
  if (normalized === 'DYNAMIC' || String(source || '').toLowerCase() === 'flowform')
    return 'DYNAMIC'
  return 'BUSINESS_OBJECT_FORM'
}

function normalizeText(value) {
  return String(value || '').trim()
}
</script>

<style scoped>
.business-flow-form-asset-select {
  width: 100%;
}

.asset-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.asset-card {
  min-width: 0;
  padding: 12px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 150ms ease,
    box-shadow 150ms ease,
    background 150ms ease;
}

.asset-card:hover:not(:disabled),
.asset-card.selected {
  border-color: #2563eb;
  background: #f8fbff;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.08);
}

.asset-card:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.asset-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.asset-card__title,
.asset-card__meta,
.asset-card__preview {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-card__title {
  font-size: 13px;
  font-weight: 700;
}

.asset-card__source {
  flex-shrink: 0;
  padding: 2px 6px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}

.asset-card__source.business {
  color: #047857;
  background: #dcfce7;
}

.asset-card__source.code {
  color: #1d4ed8;
  background: #dbeafe;
}

.asset-card__source.external {
  color: var(--text-secondary);
  background: var(--bg-tertiary);
}

.asset-card__meta {
  margin-top: 7px;
  color: var(--text-secondary);
  font-size: 12px;
}

.asset-card__preview {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
}

.asset-clear-button {
  margin-top: 8px;
}

.external-form-collapse :deep(.n-collapse-item__header) {
  padding: 0 0 8px;
  font-size: 13px;
}
</style>
