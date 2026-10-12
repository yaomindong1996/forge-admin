<template>
  <section class="code-form-asset-panel">
    <div class="asset-panel-head">
      <div>
        <h4>业务表单资产</h4>
        <p>维护代码业务表单在流程中的引用信息；字段展示继续在左侧表单、列表和详情设计器中配置。</p>
      </div>
      <n-tag :type="assetDrafts.length ? 'success' : 'warning'" :bordered="false">
        {{ assetDrafts.length ? `${assetDrafts.length} 个资产` : '未配置' }}
      </n-tag>
    </div>

    <n-alert type="info" :bordered="false" class="asset-panel-alert">
      这里维护流程可引用的业务表单资产。业务对象名称来自当前代码应用，Provider 来自后端已注册的代码表单适配器，字段显隐继续在左侧表单、列表和详情设计器配置。
    </n-alert>

    <n-empty v-if="!assetDrafts.length" description="暂无业务表单资产">
      <template #extra>
        <n-button size="small" type="primary" @click="appendAsset">
          新增表单资产
        </n-button>
      </template>
    </n-empty>

    <div v-else class="asset-list">
      <section
        v-for="(asset, index) in assetDrafts"
        :key="asset.clientKey"
        class="asset-card"
      >
        <div class="asset-card-head">
          <div>
            <strong>{{ asset.formName || asset.formKey || `表单资产 ${index + 1}` }}</strong>
            <span>{{ asset.formMode || 'BUSINESS_CODE_FORM' }} · {{ fieldCount(asset) }} 个字段</span>
          </div>
          <n-space size="small">
            <n-button size="tiny" tertiary @click="resetAsset(index)">
              恢复默认
            </n-button>
            <n-popconfirm @positive-click="removeAsset(index)">
              <template #trigger>
                <n-button size="tiny" tertiary type="error">
                  移除
                </n-button>
              </template>
              移除后保存才会生效，可通过新增重新添加。
            </n-popconfirm>
            <n-button size="tiny" tertiary type="primary" @click="appendAsset">
              新增
            </n-button>
          </n-space>
        </div>

        <div class="asset-context-row">
          <span>业务对象：{{ resolveObjectLabel(asset) }}</span>
          <span v-if="asset.formKey">系统表单 Key：{{ asset.formKey }}</span>
          <span>来源：{{ asset.sourceType === 'codeProvider' ? '后端 Provider' : '应用配置' }}</span>
        </div>

        <n-form label-placement="top" size="small" :show-feedback="false">
          <n-grid :cols="2" :x-gap="14" :y-gap="2" responsive="screen">
            <n-form-item-gi>
              <template #label>
                <span class="asset-field-label">
                  Provider
                  <n-tooltip trigger="hover">
                    <template #trigger>
                      <span class="asset-help">?</span>
                    </template>
                    Provider 来源于后端注册的 BusinessCodeFormProvider Spring Bean，平台通过 providerKey/providerName 自动读取；新增业务 Provider 需要在业务模块开发注册。
                  </n-tooltip>
                </span>
              </template>
              <n-select
                v-model:value="asset.providerKey"
                :options="providerOptions"
                clearable
                filterable
                placeholder="选择已注册 Provider"
                @update:value="value => handleProviderChange(asset, value)"
              />
            </n-form-item-gi>
            <n-form-item-gi label="Provider 名称">
              <n-input v-model:value="asset.providerName" placeholder="后端返回的 Provider 展示名" disabled />
            </n-form-item-gi>
            <n-form-item-gi label="表单名称">
              <n-input v-model:value="asset.formName" placeholder="审批节点展示名称" @update:value="markDirty" />
            </n-form-item-gi>
            <n-form-item-gi label="字段数量">
              <n-input :value="`${fieldCount(asset)} 个字段`" disabled />
            </n-form-item-gi>
            <n-form-item-gi :span="2" label="业务表单地址">
              <n-input v-model:value="asset.formUrl" placeholder="/business/xxx-form" @update:value="markDirty" />
            </n-form-item-gi>
            <n-form-item-gi :span="2" label="说明">
              <n-input
                v-model:value="asset.description"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 4 }"
                placeholder="说明这个表单资产的使用场景"
                @update:value="markDirty"
              />
            </n-form-item-gi>
          </n-grid>
        </n-form>

        <div v-if="fieldPreview(asset)" class="asset-field-preview">
          {{ fieldPreview(asset) }}
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  objectCode: {
    type: String,
    default: '',
  },
  objectName: {
    type: String,
    default: '',
  },
  formAssetCatalog: {
    type: Array,
    default: () => [],
  },
  providerCatalog: {
    type: Array,
    default: () => [],
  },
  metadata: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['dirtyChange'])

const assetDrafts = ref([])
const defaultAssetMap = ref(new Map())
const removedAssetKeys = ref(new Set())

const providerOptions = computed(() => {
  const result = []
  const seen = new Set()
  normalizeProviderCatalog(props.providerCatalog).forEach((provider) => {
    if (!provider.providerKey || seen.has(provider.providerKey))
      return
    seen.add(provider.providerKey)
    result.push({
      label: provider.providerName ? `${provider.providerName}（${provider.providerKey}）` : provider.providerKey,
      value: provider.providerKey,
    })
  })
  ;[...props.formAssetCatalog, ...assetDrafts.value].forEach((asset) => {
    const providerKey = text(asset?.providerKey)
    if (!providerKey || seen.has(providerKey))
      return
    seen.add(providerKey)
    result.push({
      label: asset?.providerName ? `${asset.providerName}（${providerKey}）` : providerKey,
      value: providerKey,
    })
  })
  return result
})

watch(
  () => [props.formAssetCatalog, props.providerCatalog, props.metadata],
  () => assignDrafts(),
  { immediate: true, deep: true },
)

function assignDrafts() {
  removedAssetKeys.value = new Set(normalizeAssetKeys(props.metadata?.removedFormAssetKeys))
  const allProviderAssets = hydrateAssetsWithProviderCatalog(
    normalizeAssets(props.formAssetCatalog),
    collectProviderCatalogAssets(),
  )
  defaultAssetMap.value = new Map(allProviderAssets.flatMap(asset => assetKeys(asset).map(key => [key, asset])))
  const providerAssets = allProviderAssets.filter(asset => !isAssetRemoved(asset))
  const metadataAssets = normalizeAssets(props.metadata?.formAssets || []).filter(asset => !isAssetRemoved(asset))
  const source = metadataAssets.length ? mergeAssets(providerAssets, metadataAssets) : providerAssets
  assetDrafts.value = source.map((asset, index) => ({
    ...asset,
    clientKey: `asset_${index}_${asset.formKey || Date.now()}`,
  }))
}

function appendAsset() {
  const asset = createAssetFromProviderCatalog()
  unmarkAssetRemoved(asset)
  assetDrafts.value.push({
    ...asset,
    clientKey: `asset_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
  })
  markDirty()
}

function resetAsset(index) {
  const current = assetDrafts.value[index]
  const defaults = findDefaultAsset(current)
  assetDrafts.value[index] = {
    ...(defaults || createEmptyAsset()),
    clientKey: current.clientKey,
  }
  unmarkAssetRemoved(assetDrafts.value[index])
  markDirty()
}

function removeAsset(index) {
  const current = assetDrafts.value[index]
  const key = primaryAssetKey(current)
  if (key)
    removedAssetKeys.value.add(key)
  assetDrafts.value.splice(index, 1)
  markDirty()
}

function buildMetadata(baseMetadata = {}) {
  const metadata = clone(baseMetadata || {})
  const sourceAssets = Array.isArray(metadata.formAssets) ? metadata.formAssets : []
  const sourceMap = new Map(normalizeAssets([...props.formAssetCatalog, ...collectProviderCatalogAssets(), ...sourceAssets])
    .flatMap(asset => assetKeys(asset).map(key => [key, asset])))
  const formAssets = assetDrafts.value
    .filter(asset => text(asset.formKey) && !isAssetRemoved(asset))
    .map((asset) => {
      const source = findAsset(sourceMap, asset) || {}
      const fields = normalizeFields(source.fields?.length ? source.fields : source.fieldCatalog || asset.fields || asset.fieldCatalog)
      return {
        ...source,
        ...pickAssetConfig(asset),
        objectCode: props.objectCode || source.objectCode || asset.objectCode,
        objectName: asset.objectName || props.objectName || source.objectName,
        formMode: asset.formMode || source.formMode || 'BUSINESS_CODE_FORM',
        type: asset.formMode || source.type || source.formMode || 'BUSINESS_CODE_FORM',
        supportsSave: source.supportsSave !== false,
        fields,
        fieldCatalog: fields,
        fieldCount: fields.length,
      }
    })
  metadata.objectCode = props.objectCode || metadata.objectCode
  metadata.objectName = props.objectName || metadata.objectName
  metadata.formAssets = formAssets
  metadata.removedFormAssetKeys = normalizeAssetKeys([...removedAssetKeys.value])
  if (!Array.isArray(metadata.fields) || !metadata.fields.length) {
    metadata.fields = normalizeFields(formAssets[0]?.fields || formAssets[0]?.fieldCatalog || [])
  }
  return metadata
}

function createEmptyAsset() {
  const code = props.objectCode || 'code_app'
  const name = props.objectName || '代码业务'
  return {
    appName: name,
    objectName: name,
    businessName: name,
    formKey: `${code}_form`,
    formName: `${name}表单`,
    formMode: 'BUSINESS_CODE_FORM',
    type: 'BUSINESS_CODE_FORM',
    providerKey: '',
    providerName: '',
    formUrl: '',
    description: '',
    fields: [],
    fieldCatalog: [],
    supportsSave: true,
  }
}

function createAssetFromProviderCatalog() {
  const firstAsset = collectProviderCatalogAssets()[0]
  return firstAsset ? { ...createEmptyAsset(), ...firstAsset } : createEmptyAsset()
}

function handleProviderChange(asset, providerKey) {
  asset.providerKey = text(providerKey)
  const provider = normalizeProviderCatalog(props.providerCatalog).find(item => item.providerKey === asset.providerKey)
  const providerAsset = findProviderDefaultAsset(provider, asset) || collectProviderCatalogAssets()
    .find(item => item.providerKey === asset.providerKey)
  if (provider) {
    asset.providerName = provider.providerName
  }
  if (providerAsset) {
    const keepName = text(asset.formName)
    Object.assign(asset, {
      ...asset,
      ...providerAsset,
      formName: keepName || providerAsset.formName,
      providerKey: asset.providerKey,
      providerName: provider?.providerName || providerAsset.providerName || asset.providerName,
    })
    unmarkAssetRemoved(asset)
  }
  markDirty()
}

function findProviderDefaultAsset(provider, currentAsset = {}) {
  if (!provider?.assets?.length)
    return null
  const currentFormKey = text(currentAsset.formKey)
  if (currentFormKey) {
    const matched = provider.assets.find(asset => text(asset.formKey) === currentFormKey)
    if (matched)
      return matched
  }
  return provider.assets.find(asset => text(asset.formKey) === provider.defaultFormKey) || provider.assets[0]
}

function normalizeAssets(source = []) {
  return (Array.isArray(source) ? source : [])
    .filter(Boolean)
    .map(asset => ({
      ...clone(asset),
      appName: text(asset.appName),
      objectName: text(asset.objectName || props.objectName),
      businessName: text(asset.businessName || asset.objectName || props.objectName),
      formKey: text(asset.formKey || asset.key || asset.id),
      formName: text(asset.formName || asset.name || asset.label || asset.formKey),
      formMode: text(asset.formMode || asset.type || 'BUSINESS_CODE_FORM'),
      type: text(asset.type || asset.formMode || 'BUSINESS_CODE_FORM'),
      providerKey: text(asset.providerKey),
      providerName: text(asset.providerName),
      formUrl: text(asset.formUrl),
      description: text(asset.description),
      fields: normalizeFields(asset.fields?.length ? asset.fields : asset.fieldCatalog || []),
      fieldCatalog: normalizeFields(asset.fieldCatalog?.length ? asset.fieldCatalog : asset.fields || []),
      supportsSave: asset.supportsSave !== false,
    }))
    .filter(asset => asset.formKey || asset.providerKey)
}

function normalizeProviderCatalog(source = []) {
  return (Array.isArray(source) ? source : [])
    .filter(Boolean)
    .map(provider => ({
      providerKey: text(provider.providerKey),
      providerName: text(provider.providerName || provider.providerKey),
      assets: normalizeAssets(provider.assets || []),
      defaultFormKey: text(provider.defaultFormKey),
      defaultFormName: text(provider.defaultFormName),
      defaultFormUrl: text(provider.defaultFormUrl),
    }))
    .filter(provider => provider.providerKey)
}

function collectProviderCatalogAssets() {
  return normalizeProviderCatalog(props.providerCatalog)
    .flatMap(provider => provider.assets.length
      ? provider.assets.map(asset => ({
          ...asset,
          providerKey: provider.providerKey,
          providerName: provider.providerName,
        }))
      : [{
          ...createEmptyAsset(),
          providerKey: provider.providerKey,
          providerName: provider.providerName,
          formKey: provider.defaultFormKey || `${props.objectCode || 'code_app'}_form`,
          formName: provider.defaultFormName || provider.providerName,
          formUrl: provider.defaultFormUrl,
        }])
}

function hydrateAssetsWithProviderCatalog(assets = [], catalogAssets = []) {
  const catalogMap = new Map(catalogAssets.flatMap(asset => assetKeys(asset).map(key => [key, asset])))
  const result = []
  const seen = new Set()
  assets.forEach((asset) => {
    const catalogAsset = findAsset(catalogMap, asset)
    const fields = asset.fields?.length || asset.fieldCatalog?.length
      ? normalizeFields(asset.fields?.length ? asset.fields : asset.fieldCatalog)
      : normalizeFields(catalogAsset?.fields?.length ? catalogAsset.fields : catalogAsset?.fieldCatalog || [])
    appendAssetResult(result, seen, {
      ...(catalogAsset || {}),
      ...asset,
      fields,
      fieldCatalog: fields,
      fieldCount: fields.length,
    })
  })
  catalogAssets.forEach(asset => appendAssetResult(result, seen, asset))
  return result
}

function mergeAssets(providerAssets = [], metadataAssets = []) {
  const metadataMap = new Map(metadataAssets.flatMap(asset => assetKeys(asset).map(key => [key, asset])))
  const result = []
  const seen = new Set()
  providerAssets.forEach((asset) => {
    const configured = findAsset(metadataMap, asset)
    const merged = configured ? { ...asset, ...pickAssetConfig(configured) } : asset
    appendAssetResult(result, seen, merged)
  })
  metadataAssets.forEach(asset => appendAssetResult(result, seen, asset))
  return result
}

function appendAssetResult(result, seen, asset) {
  const key = assetKeys(asset)[0]
  if (!key || seen.has(key))
    return
  seen.add(key)
  result.push(asset)
}

function pickAssetConfig(asset = {}) {
  const result = {}
  ;['appName', 'objectName', 'businessName', 'formKey', 'formName', 'formMode', 'type', 'providerKey', 'providerName', 'formUrl', 'description'].forEach((key) => {
    result[key] = text(asset[key])
  })
  result.supportsSave = asset.supportsSave !== false
  return result
}

function normalizeFields(source = []) {
  return (Array.isArray(source) ? source : []).map(field => ({ ...clone(field) }))
}

function findDefaultAsset(asset) {
  return findAsset(defaultAssetMap.value, asset)
}

function findAsset(map, asset = {}) {
  for (const key of assetKeys(asset)) {
    if (map.has(key))
      return map.get(key)
  }
  return null
}

function assetKeys(asset = {}) {
  return [
    text(asset.formKey) ? `form:${text(asset.formKey)}` : '',
    text(asset.providerKey) ? `provider:${text(asset.providerKey)}` : '',
  ].filter(Boolean)
}

function primaryAssetKey(asset = {}) {
  return assetKeys(asset)[0] || ''
}

function normalizeAssetKeys(source = []) {
  return (Array.isArray(source) ? source : [])
    .map(key => text(key))
    .filter(Boolean)
}

function isAssetRemoved(asset = {}) {
  return assetKeys(asset).some(key => removedAssetKeys.value.has(key))
}

function unmarkAssetRemoved(asset = {}) {
  assetKeys(asset).forEach(key => removedAssetKeys.value.delete(key))
}

function fieldCount(asset = {}) {
  const fields = asset.fields?.length ? asset.fields : asset.fieldCatalog || []
  return Array.isArray(fields) ? fields.length : 0
}

function fieldPreview(asset = {}) {
  const preview = Array.isArray(asset.fieldPreview) && asset.fieldPreview.length
    ? asset.fieldPreview
    : (asset.fields?.length ? asset.fields : asset.fieldCatalog || [])
        .map(field => field?.label || field?.fieldName || field?.field || field?.fieldCode)
        .filter(Boolean)
        .slice(0, 6)
  return preview.length ? `字段预览：${preview.join(' / ')}` : ''
}

function resolveObjectLabel(asset = {}) {
  const name = text(asset.objectName || props.objectName || asset.businessName || asset.appName)
  const code = text(props.objectCode || asset.objectCode)
  if (name && code)
    return `${name}（${code}）`
  return name || code || '当前代码应用'
}

function markDirty() {
  emit('dirtyChange', true)
}

function clone(value) {
  return JSON.parse(JSON.stringify(value || {}))
}

function text(value) {
  return String(value ?? '').trim()
}

defineExpose({
  assignDrafts,
  buildMetadata,
})
</script>

<style scoped>
.code-form-asset-panel {
  display: grid;
  gap: 14px;
}

.asset-panel-head,
.asset-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.asset-panel-head h4,
.asset-card-head strong {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
  line-height: 22px;
}

.asset-panel-head p,
.asset-card-head span,
.asset-field-preview {
  margin: 3px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.asset-panel-alert {
  border-radius: 8px;
}

.asset-list {
  display: grid;
  gap: 12px;
}

.asset-card {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 14px;
}

.asset-card-head {
  margin-bottom: 12px;
}

.asset-context-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: -2px 0 10px;
}

.asset-context-row span {
  border-radius: 999px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 18px;
  padding: 3px 9px;
}

.asset-field-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.asset-help {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  cursor: help;
  font-size: 11px;
  font-weight: 700;
}

.asset-field-preview {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--border-light);
}
</style>
