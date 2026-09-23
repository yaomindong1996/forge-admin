<template>
  <view
    class="ai-icon"
    :class="[`ai-icon--${size}`, { 'ai-icon--tile': tile }]"
    :style="iconStyle"
  />
</template>

<script setup>
import { computed } from 'vue'
import { resolveIconUrl as resolveLocalIconUrl, resolveStaticUrl } from '@/utils/assets'

const props = defineProps({
  icon: {
    type: String,
    default: ''
  },
  name: {
    type: String,
    default: ''
  },
  src: {
    type: String,
    default: ''
  },
  color: {
    type: String,
    default: '#4e5969'
  },
  size: {
    type: String,
    default: 'md',
    validator: value => ['sm', 'md', 'lg', 'xl'].includes(value)
  },
  tile: {
    type: Boolean,
    default: false
  }
})

const rawIcon = computed(() => props.icon || props.src || props.name)

const iconUrl = computed(() => resolveIconUrl(rawIcon.value))

const iconStyle = computed(() => {
  if (!iconUrl.value) {
    return {}
  }
  return {
    backgroundColor: props.color,
    WebkitMask: `url(${iconUrl.value}) center / contain no-repeat`,
    mask: `url(${iconUrl.value}) center / contain no-repeat`,
  }
})

function resolveIconUrl(value) {
  const iconValue = String(value || '').trim()
  if (!iconValue) {
    return ''
  }

  if (isDirectImageValue(iconValue)) {
    return resolveStaticUrl(iconValue)
  }

  if (iconValue.startsWith('local:')) {
    return resolveIconUrl(iconValue.replace('local:', ''))
  }

  if (iconValue.startsWith('ionicons5:')) {
    return toIconifyUrl('ion', iconValue.replace('ionicons5:', ''))
  }

  if (iconValue.startsWith('i-')) {
    return resolveUnoIconUrl(iconValue)
  }

  if (iconValue.includes(':')) {
    const [collection, name] = iconValue.split(':')
    return toIconifyUrl(collection, name)
  }

  if (/[A-Z]/.test(iconValue)) {
    return toIconifyUrl('ion', iconValue)
  }

  return resolveLocalIconUrl(iconValue)
}

function isDirectImageValue(value) {
  return value.startsWith('http://')
    || value.startsWith('https://')
    || value.startsWith('data:')
    || value.startsWith('blob:')
    || value.startsWith('/')
    || /\.(?:png|jpe?g|webp|gif|svg|avif)(?:\?.*)?$/i.test(value)
}

function resolveUnoIconUrl(value) {
  const iconValue = value.replace(/^i-/, '')
  const separatorIndex = iconValue.indexOf(':')
  if (separatorIndex === -1) {
    return ''
  }
  return toIconifyUrl(
    iconValue.slice(0, separatorIndex),
    iconValue.slice(separatorIndex + 1)
  )
}

function toIconifyUrl(collection, name) {
  const iconName = toKebabCase(name)
  if (!collection || !iconName) {
    return ''
  }
  return `https://api.iconify.design/${collection}/${iconName}.svg`
}

function toKebabCase(value) {
  return String(value || '')
    .trim()
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .replace(/[\s_]+/g, '-')
    .toLowerCase()
}
</script>

<style lang="scss" scoped>
.ai-icon {
  display: inline-block;
  flex-shrink: 0;
}

.ai-icon--sm {
  width: 28rpx;
  height: 28rpx;
}

.ai-icon--md {
  width: 38rpx;
  height: 38rpx;
}

.ai-icon--lg {
  width: 48rpx;
  height: 48rpx;
}

.ai-icon--xl {
  width: 64rpx;
  height: 64rpx;
}

.ai-icon--tile {
  border-radius: 18rpx;
}
</style>
