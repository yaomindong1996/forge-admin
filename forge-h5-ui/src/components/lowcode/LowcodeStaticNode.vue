<template>
  <view class="lowcode-static" :class="`lowcode-static--${descriptor.kind}`">
    <button v-if="descriptor.renderer === 'back-button'" class="lowcode-static__back" @click="emitAction({ ...node, type: 'back' })">
      <wd-icon name="arrow-left" size="17px" />
      <text>{{ nodeProps.text || label || '返回' }}</text>
    </button>
    <wd-divider v-else-if="descriptor.renderer === 'divider'">{{ label }}</wd-divider>
    <view v-else-if="descriptor.renderer === 'spacer'" :style="{ height: `${spacerHeight}rpx` }" />

    <view v-else-if="['group-title', 'section-title', 'page-title', 'text-title'].includes(descriptor.renderer)" class="lowcode-static__title">
      <text>{{ label || displayText }}</text>
      <text v-if="subtitle" class="lowcode-static__subtitle">{{ subtitle }}</text>
    </view>

    <view v-else-if="['paragraph', 'text-tip', 'safe-rich-text', 'safe-markdown', 'code'].includes(descriptor.renderer)" class="lowcode-static__text" :class="{ 'is-code': descriptor.renderer === 'code' }">
      {{ displayText || '-' }}
    </view>

    <view v-else-if="['statistic', 'number-animation'].includes(descriptor.renderer)" class="lowcode-static__statistic">
      <text class="lowcode-static__statistic-label">{{ label }}</text>
      <text class="lowcode-static__statistic-value">{{ displayValue }}</text>
      <text v-if="subtitle" class="lowcode-static__subtitle">{{ subtitle }}</text>
    </view>

    <view v-else-if="['info-panel', 'announcement'].includes(descriptor.renderer)" class="lowcode-static__notice">
      <wd-icon name="info-circle" size="17px" />
      <view>
        <text v-if="label" class="lowcode-static__notice-title">{{ label }}</text>
        <text class="lowcode-static__notice-text">{{ displayText }}</text>
      </view>
    </view>

    <view v-else-if="descriptor.renderer === 'tag-list'" class="lowcode-static__tags">
      <wd-tag v-for="(item, index) in displayItems" :key="itemKey(item, index)" plain type="primary">
        {{ itemLabel(item) }}
      </wd-tag>
    </view>

    <view v-else-if="['steps', 'timeline'].includes(descriptor.renderer)" class="lowcode-static__sequence">
      <view v-for="(item, index) in displayItems" :key="itemKey(item, index)" class="lowcode-static__sequence-item">
        <view class="lowcode-static__sequence-dot" :class="{ 'is-active': index <= activeIndex }" />
        <view class="lowcode-static__sequence-copy">
          <text>{{ itemLabel(item) }}</text>
          <text v-if="itemDescription(item)" class="lowcode-static__subtitle">{{ itemDescription(item) }}</text>
        </view>
      </view>
    </view>

    <view v-else-if="['list', 'log', 'descriptions', 'stats-strip'].includes(descriptor.renderer)" class="lowcode-static__list">
      <view v-for="(item, index) in displayItems" :key="itemKey(item, index)" class="lowcode-static__list-row">
        <text>{{ itemLabel(item) }}</text>
        <text class="lowcode-static__list-value">{{ itemValue(item) }}</text>
      </view>
    </view>

    <AiEmpty v-else-if="descriptor.renderer === 'empty-state'" :title="label || '暂无数据'" :description="displayText" />

    <view v-else-if="descriptor.renderer === 'avatar'" class="lowcode-static__profile">
      <AiAuthImage class="lowcode-static__avatar" :src="mediaSource" mode="aspectFill" />
      <view>
        <text>{{ label || displayText || '未命名' }}</text>
        <text v-if="subtitle" class="lowcode-static__subtitle">{{ subtitle }}</text>
      </view>
    </view>

    <button v-else-if="descriptor.renderer === 'audio' && mediaSource" class="lowcode-static__audio" @click="toggleAudio">
      <wd-icon :name="audioPlaying ? 'pause-circle' : 'play-circle'" size="22px" />
      <text>{{ audioPlaying ? '暂停音频' : (label || '播放音频') }}</text>
    </button>
    <video v-else-if="descriptor.renderer === 'video' && mediaSource" class="lowcode-static__video" :src="mediaSource" controls />

    <LowcodeMachineCode
      v-else-if="['barcode', 'qrcode'].includes(descriptor.renderer)"
      :kind="descriptor.renderer" :value="displayValue" :title="label" :options="nodeProps"
    />

    <LowcodeCalendarWidget
      v-else-if="descriptor.renderer === 'calendar'"
      :value="displayValue" :items="displayItems"
    />

    <LowcodeWatermark
      v-else-if="descriptor.renderer === 'watermark'"
      :content="displayText || '内部资料'" :options="nodeProps"
    />

    <view v-else-if="descriptor.renderer === 'countdown'" class="lowcode-static__countdown">
      {{ displayValue || '--:--:--' }}
    </view>

    <view v-else-if="['action-button', 'button-group'].includes(descriptor.renderer)" class="lowcode-static__actions">
      <AiButton
        v-for="(action, index) in actionItems"
        :key="itemKey(action, index)"
        size="sm"
        :variant="action.variant || 'secondary'"
        :disabled="readonly || action.disabled === true"
        @click="emitAction(action)"
      >
        {{ itemLabel(action) || '操作' }}
      </AiButton>
    </view>

    <button v-else-if="descriptor.renderer === 'link'" class="lowcode-static__link" @click="emitAction(node)">
      {{ label || displayText || '打开链接' }}
    </button>

    <LowcodeDataCards
      v-else-if="['mobile-crud', 'card-table', 'tree-panel', 'sub-table', 'sub-table-tabs'].includes(descriptor.renderer)"
      :title="label" :items="displayItems" :columns="dataColumns"
      :tree="descriptor.renderer === 'tree-panel'" :row-key-field="nodeProps.rowKey || 'id'"
      :title-field="nodeProps.titleField || ''"
    />

    <view v-else-if="['search-form', 'toolbar', 'form', 'step-form'].includes(descriptor.renderer)" class="lowcode-static__business">
      <wd-icon name="view-list" size="18px" />
      <text>{{ label || '业务组件' }}</text>
      <text class="lowcode-static__subtitle">已按移动端页面结构适配</text>
    </view>

    <LowcodeUnsupported
      v-else
      :component-type="descriptor.sourceType"
      :reason="unsupportedReason"
      :value="displayValue"
      :blocked="descriptor.capability === MOBILE_COMPONENT_CAPABILITY.BLOCKED"
    />
  </view>
</template>

<script setup>
import { computed, onUnmounted, ref } from 'vue'
import AiAuthImage from '@/components/AiAuthImage.vue'
import AiButton from '@/components/AiButton.vue'
import AiEmpty from '@/components/AiEmpty.vue'
import LowcodeCalendarWidget from './LowcodeCalendarWidget.vue'
import LowcodeDataCards from './LowcodeDataCards.vue'
import LowcodeMachineCode from './LowcodeMachineCode.vue'
import LowcodeUnsupported from './LowcodeUnsupported.vue'
import LowcodeWatermark from './LowcodeWatermark.vue'
import { MOBILE_COMPONENT_CAPABILITY, resolveMobileComponent } from './mobile-component-registry'

const props = defineProps({
  node: { type: Object, default: () => ({}) },
  data: { type: Object, default: () => ({}) },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['action'])
const audioPlaying = ref(false)
let audioContext
const descriptor = computed(() => resolveMobileComponent(
  props.node.nodeType || props.node.componentKey || props.node.type,
  { platform: resolveRuntimePlatform() },
))
const nodeProps = computed(() => props.node.props || {})
const label = computed(() => {
  const field = nodeProps.value.dataBinding?.titleField
  return props.node.label || (field ? readPath(props.data, field) : '') || nodeProps.value.title || ''
})
const subtitle = computed(() => nodeProps.value.subtitle || nodeProps.value.description || '')
const displayValue = computed(() => {
  const field = nodeProps.value.field || nodeProps.value.dataField || nodeProps.value.dataBinding?.valueField || props.node.fieldRef
  if (field) return readPath(props.data, field)
  return nodeProps.value.value ?? nodeProps.value.number ?? props.node.value ?? ''
})
const displayText = computed(() => plainText(
  nodeProps.value.text
  ?? nodeProps.value.content
  ?? nodeProps.value.markdown
  ?? nodeProps.value.html
  ?? subtitle.value
  ?? displayValue.value,
))
const displayItems = computed(() => {
  const source = nodeProps.value.items || nodeProps.value.rows || nodeProps.value.records || nodeProps.value.treeData
    || nodeProps.value.options || nodeProps.value.tags || nodeProps.value.steps || displayValue.value
  if (Array.isArray(source)) return source
  if (typeof source === 'string') {
    try { const parsed = JSON.parse(source); return Array.isArray(parsed) ? parsed : [] }
    catch { return [] }
  }
  if (source && typeof source === 'object') {
    const rows = source.records || source.list || source.rows || source.children
    return Array.isArray(rows) ? rows : []
  }
  return []
})
const dataColumns = computed(() => {
  const source = nodeProps.value.columns || nodeProps.value.columnsSchema || nodeProps.value.fields
  if (Array.isArray(source)) return source
  if (typeof source === 'string') {
    try { const parsed = JSON.parse(source); return Array.isArray(parsed) ? parsed : [] }
    catch { return [] }
  }
  return []
})
const actionItems = computed(() => descriptor.value.renderer === 'button-group'
  ? (Array.isArray(nodeProps.value.actions) ? nodeProps.value.actions : displayItems.value)
  : [props.node])
const activeIndex = computed(() => Number(nodeProps.value.active ?? nodeProps.value.current ?? 0))
const spacerHeight = computed(() => Math.max(12, Number(nodeProps.value.height || props.node.h || 32)))
const mediaSource = computed(() => safeMediaSource(nodeProps.value.src || nodeProps.value.url || displayValue.value))
const unsupportedReason = computed(() => {
  if (descriptor.value.renderer === 'web-view') return '内嵌网页需要配置移动端域名白名单，当前仅展示占位。'
  if (descriptor.value.renderer === 'blocked-dynamic-component') return '移动端禁止执行设计协议中的动态 Vue/脚本组件。'
  return descriptor.value.reason || '该组件当前以安全占位方式呈现。'
})

function emitAction(action) {
  emit('action', { ...nodeProps.value, ...action })
}

function toggleAudio() {
  if (!mediaSource.value) return
  if (!audioContext) {
    audioContext = uni.createInnerAudioContext()
    audioContext.src = mediaSource.value
    audioContext.onEnded(() => { audioPlaying.value = false })
    audioContext.onStop(() => { audioPlaying.value = false })
    audioContext.onError(() => { audioPlaying.value = false })
  }
  if (audioPlaying.value) audioContext.pause()
  else audioContext.play()
  audioPlaying.value = !audioPlaying.value
}

function itemKey(item, index) {
  return String(item?.id || item?.key || item?.value || index)
}

function itemLabel(item) {
  if (item === undefined || item === null) return ''
  if (typeof item !== 'object') return String(item)
  return String(item.label || item.title || item.name || item.text || item.value || '')
}

function itemValue(item) {
  if (!item || typeof item !== 'object') return ''
  return String(item.value ?? item.content ?? item.description ?? '')
}

function itemDescription(item) {
  return item && typeof item === 'object' ? String(item.description || item.content || item.time || '') : ''
}

function readPath(source, path) {
  return String(path || '').split('.').filter(Boolean).reduce((value, key) => value?.[key], source)
}

function plainText(value) {
  return String(value ?? '')
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function safeMediaSource(value) {
  const source = String(value || '').trim()
  return /^(https?:\/\/|\/)/i.test(source) ? source : ''
}

function resolveRuntimePlatform() {
  let value = 'mini-program'
  // #ifdef H5
  value = 'h5'
  // #endif
  return value
}

onUnmounted(() => {
  audioContext?.destroy?.()
  audioContext = null
})
</script>

<style lang="scss" scoped>
.lowcode-static { margin-bottom: 20rpx; }
.lowcode-static__back { display: inline-flex; align-items: center; gap: 8rpx; margin: 0; padding: 12rpx 16rpx; border: 1rpx solid #e2e8f0; border-radius: 10rpx; color: #475569; font-size: 23rpx; background: #fff; }
.lowcode-static__back::after { border: 0; }
.lowcode-static__title { display: flex; flex-direction: column; gap: 6rpx; color: #0f172a; font-size: 30rpx; font-weight: 800; }
.lowcode-static__subtitle { display: block; margin-top: 4rpx; color: #94a3b8; font-size: 21rpx; font-weight: 400; }
.lowcode-static__text { padding: 18rpx 20rpx; border-radius: 12rpx; color: #475569; font-size: 25rpx; line-height: 1.65; background: #f8fafc; white-space: pre-wrap; }
.lowcode-static__text.is-code { overflow-x: auto; color: #dbeafe; font-family: monospace; background: #0f172a; }
.lowcode-static__statistic { display: flex; flex-direction: column; padding: 22rpx; border: 1rpx solid #e2e8f0; border-radius: 14rpx; background: #fff; }
.lowcode-static__statistic-label { color: #64748b; font-size: 22rpx; }
.lowcode-static__statistic-value { margin-top: 8rpx; color: #0f172a; font-size: 42rpx; font-weight: 850; }
.lowcode-static__notice { display: flex; align-items: flex-start; gap: 12rpx; padding: 18rpx 20rpx; border: 1rpx solid #bfdbfe; border-radius: 12rpx; color: #1d4ed8; background: #eff6ff; }
.lowcode-static__notice-title, .lowcode-static__notice-text { display: block; font-size: 23rpx; line-height: 1.5; }
.lowcode-static__notice-title { font-weight: 750; }
.lowcode-static__tags, .lowcode-static__actions { display: flex; flex-wrap: wrap; gap: 12rpx; }
.lowcode-static__sequence { display: flex; flex-direction: column; gap: 4rpx; }
.lowcode-static__sequence-item { display: flex; min-height: 64rpx; gap: 14rpx; }
.lowcode-static__sequence-dot { width: 16rpx; height: 16rpx; flex: 0 0 auto; margin-top: 7rpx; border: 4rpx solid #cbd5e1; border-radius: 50%; background: #fff; }
.lowcode-static__sequence-dot.is-active { border-color: #2563eb; }
.lowcode-static__sequence-copy { display: flex; flex-direction: column; color: #334155; font-size: 24rpx; }
.lowcode-static__list { overflow: hidden; border: 1rpx solid #e2e8f0; border-radius: 12rpx; }
.lowcode-static__list-row { display: flex; justify-content: space-between; gap: 18rpx; padding: 16rpx 18rpx; border-bottom: 1rpx solid #eef2f7; color: #475569; font-size: 23rpx; }
.lowcode-static__list-row:last-child { border-bottom: 0; }
.lowcode-static__list-value { color: #0f172a; text-align: right; }
.lowcode-static__profile { display: flex; align-items: center; gap: 16rpx; }
.lowcode-static__avatar { width: 84rpx; height: 84rpx; overflow: hidden; border-radius: 50%; }
.lowcode-static__video { width: 100%; border-radius: 14rpx; }
.lowcode-static__audio { display: inline-flex; align-items: center; gap: 10rpx; margin: 0; padding: 14rpx 18rpx; border: 1rpx solid #bfdbfe; border-radius: 12rpx; color: #2563eb; font-size: 23rpx; background: #eff6ff; }
.lowcode-static__audio::after { border: 0; }
.lowcode-static__machine-code, .lowcode-static__business { display: flex; align-items: center; gap: 14rpx; padding: 20rpx; border: 1rpx dashed #bfdbfe; border-radius: 12rpx; color: #475569; background: #f8fbff; }
.lowcode-static__countdown { color: #0f172a; font-size: 42rpx; font-weight: 850; letter-spacing: 3rpx; }
.lowcode-static__link { margin: 0; padding: 0; border: 0; color: #2563eb; font-size: 24rpx; line-height: 1.4; background: transparent; }
.lowcode-static__link::after { border: 0; }
</style>
