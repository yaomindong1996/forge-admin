<template>
  <view class="lowcode-selector">
    <button class="lowcode-selector__trigger" :disabled="effectiveDisabled" @click="open">
      <text class="lowcode-selector__value" :class="{ 'is-placeholder': !displayText }">
        {{ displayText || effectivePlaceholder }}
      </text>
      <text v-if="displayText && clearable && !effectiveDisabled" class="lowcode-selector__clear" @click.stop="clear">×</text>
      <wd-icon v-else name="arrow-right" size="16px" color="#94a3b8" />
    </button>

    <AiPopupSheet
      v-model="visible"
      :title="field.label || '请选择'"
      :description="selectorDescription"
      max-height="90vh"
      body-max-height="calc(90vh - 250rpx - env(safe-area-inset-bottom))"
      :close-on-mask="!loading"
    >
      <view v-if="searchable" class="lowcode-selector__search">
        <AiField
          v-model="keyword"
          :placeholder="`搜索${field.label || '选项'}`"
          clearable
          @confirm="search"
          @clear="search"
        />
        <AiButton size="sm" variant="secondary" :loading="loading" @click="search">查询</AiButton>
      </view>

      <view v-if="loading && !visibleOptions.length" class="lowcode-selector__state">正在加载…</view>
      <view v-else-if="loadError && !visibleOptions.length" class="lowcode-selector__state is-error">
        <text>{{ loadError }}</text>
        <AiButton size="sm" variant="secondary" @click="reload">重试</AiButton>
      </view>
      <AiEmpty v-else-if="!visibleOptions.length" title="暂无可选数据" description="请检查字段数据源或筛选条件" />

      <view v-else class="lowcode-selector__options">
        <button
          v-for="option in visibleOptions"
          :key="option.value"
          class="lowcode-selector__option"
          :class="{ 'is-selected': isSelected(option.value), 'is-disabled': option.disabled }"
          :disabled="option.disabled"
          :style="{ paddingLeft: `${24 + Math.min(option.level || 0, 5) * 28}rpx` }"
          @click="toggle(option)"
        >
          <view class="lowcode-selector__indicator" :class="{ 'is-multiple': config.multiple }">
            <text v-if="isSelected(option.value)">✓</text>
          </view>
          <view class="lowcode-selector__copy">
            <text class="lowcode-selector__label">{{ option.label }}</text>
            <text v-if="optionMeta(option)" class="lowcode-selector__meta">{{ optionMeta(option) }}</text>
          </view>
        </button>

        <button v-if="hasMore" class="lowcode-selector__more" :disabled="loading" @click="loadMore">
          {{ loading ? '加载中…' : '加载更多' }}
        </button>
      </view>

      <template #footer>
        <view class="lowcode-selector__footer">
          <AiButton variant="secondary" block :disabled="loading" @click="visible = false">取消</AiButton>
          <AiButton block :disabled="loading" @click="confirm">确定{{ selectedIds.length ? `（${selectedIds.length}）` : '' }}</AiButton>
        </view>
      </template>
    </AiPopupSheet>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import api from '@/api'
import AiButton from '@/components/AiButton.vue'
import AiEmpty from '@/components/AiEmpty.vue'
import AiField from '@/components/AiField.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import {
  buildMobileSelectionPatch,
  flattenMobileSelectorOptions,
  normalizeMobileSelectorConfig,
  normalizeMobileSelectorOptions,
  parseMobileSelectionValues,
  parseRegisteredOptionApi,
  resolveMobileSelectionLabels,
  resolveMobileSelectorKind,
  resolveMobileSelectorParams,
  serializeMobileSelectionValues,
} from '@/utils/mobile-selector-runtime'

const props = defineProps({
  field: { type: Object, default: () => ({}) },
  modelValue: { type: [String, Number, Array], default: '' },
  options: { type: Array, default: () => [] },
  formData: { type: Object, default: () => ({}) },
  context: { type: Object, default: () => ({}) },
  disabled: { type: Boolean, default: false },
  clearable: { type: Boolean, default: true },
  placeholder: { type: String, default: '请选择' },
})

const emit = defineEmits(['update:modelValue', 'change', 'selection'])
const visible = ref(false)
const loading = ref(false)
const loadError = ref('')
const keyword = ref('')
const optionTree = ref([])
const selectedIds = ref([])
const pageNum = ref(1)
const total = ref(0)
const loadedCount = ref(0)

const config = computed(() => normalizeMobileSelectorConfig(props.field))
const selectorKind = computed(() => resolveMobileSelectorKind(props.field, props.options))
const allOptions = computed(() => flattenMobileSelectorOptions(optionTree.value))
const visibleOptions = computed(() => {
  if (searchable.value || !keyword.value.trim()) return allOptions.value
  const text = keyword.value.trim().toLowerCase()
  return allOptions.value.filter(option => option.label.toLowerCase().includes(text))
})
const selectedLabels = computed(() => resolveMobileSelectionLabels(props.modelValue, optionTree.value, props.field, props.formData))
const displayText = computed(() => selectedLabels.value.join('、') || parseMobileSelectionValues(props.modelValue).join('、'))
const searchable = computed(() => ['user', 'record', 'query-source', 'custom-api'].includes(selectorKind.value))
const hasMore = computed(() => searchable.value && loadedCount.value < total.value)
const selectorDescription = computed(() => config.value.multiple ? '可选择多项，确认后统一回填' : '选择一项后点击确定')
const linkageBlocked = computed(() => {
  const linkage = props.field.props?.linkageContext
  return linkage?.emptyStrategy === 'disabled' && (linkage.sourceValue === undefined || linkage.sourceValue === null || linkage.sourceValue === '')
})
const effectiveDisabled = computed(() => props.disabled || linkageBlocked.value)
const effectivePlaceholder = computed(() => linkageBlocked.value ? '请先选择上级字段' : props.placeholder)

async function open() {
  if (effectiveDisabled.value) return
  selectedIds.value = parseMobileSelectionValues(props.modelValue)
  visible.value = true
  keyword.value = ''
  await reload()
}

async function reload() {
  pageNum.value = 1
  optionTree.value = []
  loadedCount.value = 0
  await loadPage(true)
}

async function search() {
  await reload()
}

async function loadMore() {
  if (loading.value || !hasMore.value) return
  pageNum.value += 1
  await loadPage(false)
}

async function loadPage(reset) {
  if (loading.value) return
  loading.value = true
  loadError.value = ''
  try {
    const result = await requestOptions()
    const normalized = normalizeMobileSelectorOptions(result.source, config.value, { kind: selectorKind.value })
    optionTree.value = reset ? normalized : [...optionTree.value, ...normalized]
    loadedCount.value = reset ? normalized.length : loadedCount.value + normalized.length
    total.value = Number(result.total ?? loadedCount.value)
  }
  catch (error) {
    loadError.value = error?.message || '选项加载失败'
  }
  finally {
    loading.value = false
  }
}

async function requestOptions() {
  const kind = selectorKind.value
  const resolvedParams = resolveMobileSelectorParams(config.value.params, props.formData, props.context)
  if (kind === 'static' || kind === 'tree') return { source: props.options, total: props.options.length }
  if (kind === 'blocked-api') throw new Error('该字段配置了不允许访问的选项接口')
  if (kind === 'user') {
    const response = await api.getUserPage({ ...resolvedParams, keyword: keyword.value || undefined, pageNum: pageNum.value, pageSize: 30 })
    return pagedResult(response)
  }
  if (kind === 'org') {
    const response = await api.getOrgTree(resolvedParams)
    return { source: response, total: countTreeRows(response?.data) }
  }
  if (kind === 'region') {
    const response = await api.getRegionTree({ ...resolvedParams, ...(config.value.rootCode ? { rootCode: config.value.rootCode } : {}), ...(config.value.dataRight !== undefined ? { dataRight: config.value.dataRight } : {}) })
    return { source: response, total: countTreeRows(response?.data) }
  }
  if (kind === 'record') {
    const response = await api.queryBusinessRecordSelector({
      suiteCode: config.value.suiteCode,
      objectCode: config.value.objectCode,
      businessObjectCode: config.value.businessObjectCode || config.value.objectCode,
      targetObjectCode: config.value.targetObjectCode || config.value.objectCode,
      keyword: keyword.value || undefined,
      keywordFields: config.value.keywordFields,
      displayFields: config.value.displayFields,
      searchParams: resolveMobileSelectorParams(config.value.searchParams, props.formData, props.context),
      fieldMappings: config.value.fieldMappings,
    }, { pageNum: pageNum.value, pageSize: 30 })
    return pagedResult(response)
  }
  if (kind === 'query-source') {
    const params = { ...resolvedParams }
    if (keyword.value) params[config.value.keywordParam] = keyword.value
    const response = await api.executeLowcodeQuerySource({
      sourceType: config.value.querySourceType,
      sourceKey: config.value.querySourceKey,
      params,
      pageNum: pageNum.value,
      pageSize: 30,
    })
    return pagedResult(response)
  }
  const endpoint = parseRegisteredOptionApi(config.value.api, config.value.method)
  if (!endpoint) throw new Error('该字段配置了不允许访问的选项接口')
  const params = { ...resolvedParams, pageNum: pageNum.value, pageSize: 30 }
  if (keyword.value) params[config.value.keywordParam] = keyword.value
  const response = await api.executeLowcodeOptionRequest({ ...endpoint, params })
  return pagedResult(response)
}

function pagedResult(response) {
  const data = response?.data || {}
  const rows = Array.isArray(data) ? data : data.records || data.rows || data.list || data.data || []
  return { source: rows, total: Number(data.total ?? rows.length) }
}

function toggle(option) {
  if (option.disabled) return
  const value = String(option.value)
  if (!config.value.multiple) {
    selectedIds.value = [value]
    return
  }
  selectedIds.value = isSelected(value)
    ? selectedIds.value.filter(item => item !== value)
    : [...selectedIds.value, value]
}

function confirm() {
  const selectedOptions = selectedIds.value.map(id => allOptions.value.find(option => String(option.value) === id)).filter(Boolean)
  const records = selectedOptions.map(option => ({ ...option._raw, _raw: option._raw }))
  const labels = selectedOptions.map(option => option.label)
  const value = serializeMobileSelectionValues(selectedIds.value, config.value.multiple)
  const normalized = normalizeSearchSelection(value)
  const patch = { ...buildMobileSelectionPatch(props.field, records, labels), ...normalized.patch }
  emit('update:modelValue', normalized.value)
  emit('selection', { value: normalized.value, labels, records, patch })
  emit('change', normalized.value)
  visible.value = false
}

function clear() {
  const value = config.value.multiple ? '' : ''
  const patch = buildMobileSelectionPatch(props.field, [], [])
  emit('update:modelValue', value)
  emit('selection', { value, labels: [], records: [], patch })
  emit('change', value)
}

function isSelected(value) {
  return selectedIds.value.includes(String(value))
}

function optionMeta(option) {
  const raw = option._raw || {}
  if (selectorKind.value === 'user') return [raw.username, raw.deptName || raw.orgName].filter(Boolean).join(' · ')
  if (raw.description || raw.remark) return String(raw.description || raw.remark)
  return raw.code && String(raw.code) !== String(option.value) ? String(raw.code) : ''
}

function normalizeSearchSelection(value) {
  if (!props.context?.isSearch || config.value.multiple) return { value, patch: {} }
  const fieldName = String(props.field.field || '')
  if (!fieldName) return { value, patch: {} }
  if (selectorKind.value === 'region' && String(value).endsWith('ALL')) {
    return { value: String(value).replace(/ALL$/, ''), patch: { [`${fieldName}_includeChildren`]: true } }
  }
  if (selectorKind.value === 'org') return { value, patch: { [`${fieldName}_includeChildren`]: true } }
  return { value, patch: { [`${fieldName}_includeChildren`]: undefined } }
}

function countTreeRows(rows = []) {
  return (Array.isArray(rows) ? rows : []).reduce((count, row) => count + 1 + countTreeRows(row?.children), 0)
}
</script>

<style lang="scss" scoped>
.lowcode-selector__trigger { display: flex; width: 100%; min-height: 76rpx; align-items: center; gap: 12rpx; margin: 0; padding: 14rpx 18rpx; border: 1rpx solid var(--forge-color-border, #e2e8f0); border-radius: 12rpx; color: #334155; text-align: left; background: #fff; box-sizing: border-box; }
.lowcode-selector__trigger::after, .lowcode-selector__option::after, .lowcode-selector__more::after { border: 0; }
.lowcode-selector__trigger[disabled] { background: #f8fafc; opacity: .72; }
.lowcode-selector__value { min-width: 0; overflow: hidden; flex: 1; font-size: 25rpx; text-overflow: ellipsis; white-space: nowrap; }
.lowcode-selector__value.is-placeholder { color: #94a3b8; }
.lowcode-selector__clear { display: flex; width: 42rpx; height: 42rpx; align-items: center; justify-content: center; border-radius: 50%; color: #94a3b8; font-size: 32rpx; background: #f1f5f9; }
.lowcode-selector__search { display: flex; align-items: center; gap: 12rpx; margin-bottom: 18rpx; }
.lowcode-selector__search :deep(.ai-field) { min-width: 0; flex: 1; }
.lowcode-selector__state { display: flex; min-height: 200rpx; flex-direction: column; align-items: center; justify-content: center; gap: 18rpx; color: #64748b; font-size: 24rpx; }
.lowcode-selector__state.is-error { color: #dc2626; }
.lowcode-selector__options { overflow: hidden; border: 1rpx solid #e2e8f0; border-radius: 14rpx; background: #fff; }
.lowcode-selector__option { display: flex; width: 100%; min-height: 88rpx; align-items: center; gap: 16rpx; margin: 0; padding-top: 14rpx; padding-right: 20rpx; padding-bottom: 14rpx; border: 0; border-bottom: 1rpx solid #eef2f7; border-radius: 0; color: #334155; text-align: left; background: #fff; box-sizing: border-box; }
.lowcode-selector__option.is-selected { background: #eff6ff; }
.lowcode-selector__option.is-disabled { opacity: .48; }
.lowcode-selector__indicator { display: flex; width: 34rpx; height: 34rpx; flex: 0 0 auto; align-items: center; justify-content: center; border: 2rpx solid #cbd5e1; border-radius: 50%; color: #fff; font-size: 23rpx; background: #fff; box-sizing: border-box; }
.lowcode-selector__indicator.is-multiple { border-radius: 7rpx; }
.is-selected .lowcode-selector__indicator { border-color: #2563eb; background: #2563eb; }
.lowcode-selector__copy { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 3rpx; }
.lowcode-selector__label { overflow: hidden; font-size: 25rpx; text-overflow: ellipsis; white-space: nowrap; }
.lowcode-selector__meta { overflow: hidden; color: #94a3b8; font-size: 21rpx; text-overflow: ellipsis; white-space: nowrap; }
.lowcode-selector__more { width: 100%; margin: 0; border: 0; border-radius: 0; color: #2563eb; font-size: 23rpx; background: #f8fbff; }
.lowcode-selector__footer { display: grid; grid-template-columns: 1fr 1.5fr; gap: 14rpx; }
</style>
