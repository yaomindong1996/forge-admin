<script setup>
import { computed } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'
import {
  FREE_LAYOUT_TONE_COLORS,
  isHexColor,
  resolveWidgetLinkAction,
} from './free-layout-widget-kit.js'

const TYPE_ICON = {
  info: 'InformationCircleOutline',
  success: 'CheckmarkCircleOutline',
  warning: 'WarningOutline',
  error: 'CloseCircleOutline',
}

const TYPE_TONE = {
  info: 'info',
  success: 'success',
  warning: 'warning',
  error: 'danger',
}

const props = defineProps({
  title: { type: String, default: '提示信息' },
  content: { type: String, default: '' },
  type: { type: String, default: 'info' },
  icon: { type: String, default: '' },
  color: { type: String, default: '' },
  showIcon: { type: Boolean, default: true },
  linkType: { type: String, default: 'none' },
  targetPageId: { type: String, default: '' },
  href: { type: String, default: '' },
  interactive: { type: Boolean, default: false },
})

const emit = defineEmits(['select'])

const resolvedType = computed(() => {
  const t = String(props.type || 'info').trim()
  return ['info', 'success', 'warning', 'error'].includes(t) ? t : 'info'
})

const panelStyle = computed(() => {
  if (isHexColor(props.color)) {
    const color = String(props.color).trim()
    return {
      '--fl-info-color': color,
      '--fl-info-soft': softFromHex(color),
    }
  }
  const toneKey = TYPE_TONE[resolvedType.value] || 'info'
  const palette = FREE_LAYOUT_TONE_COLORS[toneKey] || FREE_LAYOUT_TONE_COLORS.info
  return {
    '--fl-info-color': palette.color,
    '--fl-info-soft': palette.soft,
  }
})

const resolvedIcon = computed(() =>
  String(props.icon || '').trim() || TYPE_ICON[resolvedType.value] || TYPE_ICON.info,
)

const clickable = computed(() => {
  if (!props.interactive)
    return false
  return resolveWidgetLinkAction({
    linkType: props.linkType,
    targetPageId: props.targetPageId,
    href: props.href,
  }).type !== 'none'
})

function softFromHex(hex) {
  const raw = String(hex).replace('#', '')
  const full = raw.length === 3
    ? raw.split('').map(ch => ch + ch).join('')
    : raw.slice(0, 6)
  const r = Number.parseInt(full.slice(0, 2), 16)
  const g = Number.parseInt(full.slice(2, 4), 16)
  const b = Number.parseInt(full.slice(4, 6), 16)
  if (![r, g, b].every(Number.isFinite))
    return 'rgba(2, 132, 199, 0.12)'
  return `rgba(${r}, ${g}, ${b}, 0.12)`
}

function onClick() {
  if (!clickable.value)
    return
  emit('select', {
    linkType: props.linkType,
    targetPageId: props.targetPageId,
    href: props.href,
  })
}
</script>

<template>
  <button
    type="button"
    class="info-panel"
    :class="{ 'is-interactive': clickable }"
    :style="panelStyle"
    :disabled="interactive && !clickable"
    @click="onClick"
  >
    <span
      v-if="showIcon"
      class="info-panel-icon"
      aria-hidden="true"
    >
      <IconRenderer :icon="resolvedIcon" :size="20" />
    </span>
    <span class="info-panel-copy">
      <strong v-if="title">{{ title }}</strong>
      <span v-if="content">{{ content }}</span>
    </span>
    <span
      v-if="clickable"
      class="info-panel-arrow"
      aria-hidden="true"
    >›</span>
  </button>
</template>

<style scoped>
.info-panel {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid color-mix(in srgb, var(--fl-info-color) 22%, #e2e8f0);
  border-radius: 14px;
  background:
    linear-gradient(135deg, var(--fl-info-soft), transparent 70%),
    #fff;
  text-align: left;
  cursor: default;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.info-panel.is-interactive {
  cursor: pointer;
}

.info-panel.is-interactive:hover {
  border-color: color-mix(in srgb, var(--fl-info-color) 45%, #cbd5e1);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.info-panel:disabled {
  cursor: default;
}

.info-panel-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  flex-shrink: 0;
  color: var(--fl-info-color);
  background: color-mix(in srgb, var(--fl-info-color) 14%, #fff);
}

.info-panel-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

.info-panel-copy strong {
  font-size: 14px;
  font-weight: 650;
  color: #0f172a;
  line-height: 1.35;
}

.info-panel-copy span {
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
  white-space: pre-wrap;
}

.info-panel-arrow {
  flex-shrink: 0;
  font-size: 18px;
  color: color-mix(in srgb, var(--fl-info-color) 70%, #94a3b8);
  line-height: 1;
  margin-top: 6px;
}
</style>
