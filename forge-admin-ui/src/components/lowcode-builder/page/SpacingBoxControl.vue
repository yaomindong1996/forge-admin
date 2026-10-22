<script setup>
/**
 * 四向间距编辑器：方框布局，直观对应上/右/下/左。
 * value 可为数字（四边相同）或 { top, right, bottom, left }。
 */
import { computed } from 'vue'

const props = defineProps({
  value: { type: [Number, String, Object], default: 0 },
  min: { type: Number, default: 0 },
  max: { type: Number, default: 200 },
  label: { type: String, default: '' },
})

const emit = defineEmits(['update:value'])

function toNum(raw, fallback = 0) {
  if (raw == null || raw === '')
    return fallback
  if (typeof raw === 'string') {
    const matched = raw.trim().match(/^-?\d+(\.\d+)?/)
    if (!matched)
      return fallback
    return Number(matched[0])
  }
  const num = Number(raw)
  return Number.isFinite(num) ? num : fallback
}

const sides = computed(() => {
  const raw = props.value
  if (raw && typeof raw === 'object' && !Array.isArray(raw)) {
    return {
      top: toNum(raw.top ?? raw.t, 0),
      right: toNum(raw.right ?? raw.r, 0),
      bottom: toNum(raw.bottom ?? raw.b, 0),
      left: toNum(raw.left ?? raw.l, 0),
    }
  }
  if (typeof raw === 'string' && /\s/.test(raw.trim())) {
    const parts = raw.trim().split(/\s+/).map(part => toNum(part, 0))
    if (parts.length === 2) {
      return { top: parts[0], right: parts[1], bottom: parts[0], left: parts[1] }
    }
    if (parts.length === 3) {
      return { top: parts[0], right: parts[1], bottom: parts[2], left: parts[1] }
    }
    if (parts.length >= 4) {
      return { top: parts[0], right: parts[1], bottom: parts[2], left: parts[3] }
    }
  }
  const n = toNum(raw, 0)
  return { top: n, right: n, bottom: n, left: n }
})

function patchSide(side, next) {
  const value = Math.max(props.min, Math.min(props.max, toNum(next, 0)))
  const nextSides = { ...sides.value, [side]: value }
  const allSame = nextSides.top === nextSides.right
    && nextSides.right === nextSides.bottom
    && nextSides.bottom === nextSides.left
  emit('update:value', allSame
    ? nextSides.top
    : { ...nextSides })
}
</script>

<template>
  <div class="spacing-box" :aria-label="label || '间距'">
    <label class="spacing-box__side is-top">
      <span>上</span>
      <n-input-number
        :value="sides.top"
        :min="min"
        :max="max"
        size="tiny"
        :show-button="false"
        @update:value="patchSide('top', $event)"
      />
    </label>
    <div class="spacing-box__mid">
      <label class="spacing-box__side is-left">
        <span>左</span>
        <n-input-number
          :value="sides.left"
          :min="min"
          :max="max"
          size="tiny"
          :show-button="false"
          @update:value="patchSide('left', $event)"
        />
      </label>
      <div class="spacing-box__preview" aria-hidden="true">
        <i />
      </div>
      <label class="spacing-box__side is-right">
        <span>右</span>
        <n-input-number
          :value="sides.right"
          :min="min"
          :max="max"
          size="tiny"
          :show-button="false"
          @update:value="patchSide('right', $event)"
        />
      </label>
    </div>
    <label class="spacing-box__side is-bottom">
      <span>下</span>
      <n-input-number
        :value="sides.bottom"
        :min="min"
        :max="max"
        size="tiny"
        :show-button="false"
        @update:value="patchSide('bottom', $event)"
      />
    </label>
  </div>
</template>

<style scoped>
.spacing-box {
  display: grid;
  gap: 6px;
  width: 100%;
  padding: 8px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary, #f6f8fb);
}

.spacing-box__mid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 56px minmax(0, 1fr);
  gap: 6px;
  align-items: center;
}

.spacing-box__side {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.spacing-box__side span {
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1;
}

.spacing-box__side.is-top,
.spacing-box__side.is-bottom {
  justify-items: center;
  max-width: 96px;
  margin: 0 auto;
}

.spacing-box__preview {
  display: grid;
  place-items: center;
  height: 40px;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  background: #fff;
}

.spacing-box__preview i {
  display: block;
  width: 18px;
  height: 12px;
  border: 1px solid #94a3b8;
  border-radius: 2px;
  background: #e2e8f0;
}
</style>
