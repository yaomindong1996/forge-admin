<template>
  <span class="icon-renderer" v-bind="attrs">
    <ForgeSymbol
      v-if="iconType === 'forge'" :name="iconName" :size="effectiveSize"
      :class="customClass" :style="[customStyle, color ? { color } : {}]"
    />
    <Icon v-else-if="iconComponent" :size="effectiveSize" :color="color">
      <component
        :is="iconComponent"
        :class="customClass"
        :style="customStyle"
      />
    </Icon>
    <i
      v-else-if="iconType === 'local'"
      :class="`${iconName} text-${effectiveSize} color-${color}`"
      :style="customStyle"
    />
    <AuthImage
      v-else-if="iconType === 'image'"
      :src="iconName"
      :alt="placeholder || 'icon'"
      :lazy="false"
      img-class="icon-image"
      :img-style="imageStyle"
    />
    <img
      v-else-if="iconType === 'localImage'"
      :src="iconName"
      :alt="placeholder || 'icon'"
      class="icon-image"
      :style="imageStyle"
    >
    <span v-else>{{ placeholder }}</span>
  </span>
</template>

<script setup>
import * as ionicons from '@vicons/ionicons5'
import { Icon } from '@vicons/utils'
import { computed, useAttrs } from 'vue'
import AuthImage from '@/components/common/AuthImage.vue'
import ForgeSymbol from '@/components/common/ForgeSymbol.vue'
import { isLocalImageIcon, resolveLocalImageIconUrl } from '@/utils/local-image-icons'

defineOptions({
  inheritAttrs: false,
})

const props = defineProps({
  // 图标名称,支持前缀格式:
  // - ionicons5:IconName 表示 ionicons5 图标
  // - forge:name 表示统一线性导航符号
  // - local:iconName / i-xxx / 其它图标类名 表示本地图标
  // - local-image:file.png 表示 src/assets/icons/image-icons 下的本地图片图标
  // - fileId / URL 表示上传图片图标
  // - IconName 无前缀优先匹配 ionicons5，未匹配则按图标类名处理
  icon: {
    type: String,
    default: '',
  },
  // 自定义类名
  customClass: {
    type: String,
    default: 'text',
  },
  // 自定义样式
  customStyle: {
    type: [String, Object],
    default: '',
  },
  // 占位符文本(当没有图标时显示)
  placeholder: {
    type: String,
    default: '',
  },
  fontSize: {
    type: [String, Number],
    default: '18',
  },
  size: {
    type: [String, Number],
    default: '',
  },
  color: {
    type: String,
    default: '',
  },
})

const attrs = useAttrs()

const effectiveSize = computed(() => props.size || props.fontSize)

function isDirectImageValue(value) {
  return value.startsWith('http://')
    || value.startsWith('https://')
    || value.startsWith('data:')
    || value.startsWith('blob:')
    || value.startsWith('/api/file/')
    || value.startsWith('forge-file://')
    || /\.(?:png|jpe?g|webp|gif|svg)(?:\?.*)?$/i.test(value)
}

function isFileIdValue(value) {
  return /^[a-f0-9]{32}$/i.test(value)
}

function normalizeSize(size) {
  if (typeof size === 'number')
    return `${size}px`
  const value = String(size || '').trim()
  if (!value)
    return '18px'
  return /^\d+(?:\.\d+)?$/.test(value) ? `${value}px` : value
}

const imageStyle = computed(() => {
  const baseStyle = {
    width: normalizeSize(effectiveSize.value),
    height: normalizeSize(effectiveSize.value),
    objectFit: 'contain',
    display: 'block',
  }

  if (!props.customStyle) {
    return baseStyle
  }

  if (typeof props.customStyle === 'string') {
    const baseText = Object.entries(baseStyle)
      .map(([key, value]) => `${key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}:${value}`)
      .join(';')
    return `${baseText};${props.customStyle}`
  }

  return {
    ...baseStyle,
    ...props.customStyle,
  }
})

// 解析图标类型
const iconType = computed(() => {
  if (!props.icon)
    return null

  const iconValue = props.icon.trim()
  if (!iconValue)
    return null

  if (iconValue.startsWith('forge:'))
    return 'forge'

  if (isLocalImageIcon(iconValue))
    return 'localImage'

  if (isDirectImageValue(iconValue) || isFileIdValue(iconValue))
    return 'image'

  if (iconValue.startsWith('i-'))
    return 'local'

  // 检查是否有前缀
  if (iconValue.includes(':')) {
    const [prefix] = iconValue.split(':')
    if (prefix === 'ionicons5' || prefix === 'local') {
      return prefix
    }
    return 'local'
  }

  // 无前缀时兼容历史 Ionicons 名称；其它值按图标类名处理，避免误请求文件接口。
  return ionicons[iconValue] ? 'ionicons5' : 'local'
})

// 解析图标名称
const iconName = computed(() => {
  if (!props.icon)
    return ''
  const iconValue = props.icon.trim()

  if (iconValue.startsWith('forge:'))
    return iconValue.slice(6)

  if (isLocalImageIcon(iconValue))
    return resolveLocalImageIconUrl(iconValue)

  if (isDirectImageValue(iconValue) || isFileIdValue(iconValue) || iconValue.startsWith('i-'))
    return iconValue

  // 检查是否有前缀
  if (iconValue.includes(':')) {
    const [prefix] = iconValue.split(':')
    if (prefix === 'ionicons5' || prefix === 'local') {
      return iconValue.replace(`${prefix}:`, '')
    }
    return iconValue
  }

  // 无前缀直接返回
  return iconValue
})

// 获取图标组件
const iconComponent = computed(() => {
  if (!iconName.value || !iconType.value)
    return null

  // ionicons5 图标
  if (iconType.value === 'ionicons5') {
    return ionicons[iconName.value] || null
  }

  // 本地图标不需要组件,使用 i 标签
  return null
})
</script>

<style scoped>
.icon-renderer {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  vertical-align: middle;
  flex-shrink: 0;
}
</style>
