<template>
  <div
    class="user-avatar"
    :style="{ width: `${size}px`, height: `${size}px`, background: gradientBg }"
  >
    <span class="avatar-text" :style="{ fontSize: `${Math.floor(size * 0.4)}px` }">
      {{ displayName }}
    </span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, default: '' },
  size: { type: Number, default: 24 },
})

const displayName = computed(() => {
  const n = props.name || '?'
  return n.charAt(0).toUpperCase()
})

function hashCode(str) {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash)
  }
  return hash
}

const gradientBg = computed(() => {
  const presets = [
    ['#667eea', '#764ba2'],
    ['#f093fb', '#f5576c'],
    ['#4facfe', '#00f2fe'],
    ['#43e97b', '#38f9d7'],
    ['#fa709a', '#fee140'],
    ['#a18cd1', '#fbc2eb'],
    ['#ff9a9e', '#fecfef'],
    ['#ffecd2', '#fcb69f'],
    ['#84fab0', '#8fd3f4'],
    ['#a6c1ee', '#fbc2eb'],
  ]
  const idx = Math.abs(hashCode(props.name || 'U')) % presets.length
  const [c1, c2] = presets[idx]
  return `linear-gradient(135deg, ${c1} 0%, ${c2} 100%)`
})
</script>

<style scoped>
.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  flex-shrink: 0;
}

.avatar-text {
  color: #fff;
  font-weight: 600;
  user-select: none;
}
</style>
