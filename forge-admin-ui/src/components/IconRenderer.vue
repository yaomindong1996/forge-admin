<template>
  <Icon v-if="iconComponent" :size="fontSize" :color="color">
    <component
      :is="iconComponent"
      :class="customClass"
      :style="customStyle"
    />
  </Icon>
  <i
    v-else-if="iconType === 'local'"
    :class="`${iconName} text-${fontSize} color-${color}`"
    :style="customStyle"
  />
  <span v-else>{{ placeholder }}</span>
</template>

<script setup>
import * as ionicons from '@vicons/ionicons5'
import { Icon } from '@vicons/utils'
import { computed } from 'vue'

const props = defineProps({
  // 图标名称,支持前缀格式:
  // - ionicons5:IconName 表示 ionicons5 图标
  // - local:iconName 表示本地图标
  // - IconName 无前缀默认为 ionicons5
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
  color: {
    type: String,
    default: '',
  },
})

// 解析图标类型
const iconType = computed(() => {
  if (!props.icon)
    return null
  // 检查是否有前缀
  if (typeof props.icon === 'string' && props.icon.includes(':')) {
    const [prefix] = props.icon.split(':')
    return prefix // 'ionicons5' 或 'local'
  }

  // 无前缀默认为 ionicons5
  return 'ionicons5'
})

// 解析图标名称
const iconName = computed(() => {
  if (!props.icon)
    return ''
  // 检查是否有前缀
  if (typeof props.icon === 'string' && props.icon.includes(':')) {
    const [prefix] = props.icon.split(':')
    return props.icon.replace(`${prefix}:`, '')
  }

  // 无前缀直接返回
  return props.icon
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
/* 图标默认样式可以在这里定义 */
</style>
