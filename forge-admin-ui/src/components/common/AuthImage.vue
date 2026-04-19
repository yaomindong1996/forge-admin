<template>
  <img
    :src="imageSrc"
    :alt="alt"
    :class="imgClass"
    :style="imgStyle"
    @error="handleError"
    @load="handleLoad"
  >
</template>

<script setup>
/**
 * AuthImage - 支持认证的图片组件
 *
 * 用法：
 * <AuthImage :src="fileId" />
 * <AuthImage :src="fileId" fallback="/default.png" />
 * <AuthImage :src="fileId" :width="100" :height="100" />
 */
import { onUnmounted, ref, watch } from 'vue'
import { useAuthStore } from '@/store'
import { getFileUrl } from '@/utils'

const props = defineProps({
  // 图片地址：可以是 fileId、filePath 或完整 URL
  src: {
    type: String,
    default: '',
  },
  // 加载失败时的默认图片
  fallback: {
    type: String,
    default: '',
  },
  // alt 属性
  alt: {
    type: String,
    default: '',
  },
  // 自定义 class
  imgClass: {
    type: [String, Array, Object],
    default: '',
  },
  // 自定义 style
  imgStyle: {
    type: [String, Object],
    default: '',
  },
})

const emit = defineEmits(['load', 'error'])

const authStore = useAuthStore()
const imageSrc = ref('')
let currentBlobUrl = null

// 判断是否需要认证
function needsAuth(url) {
  if (!url)
    return false
  // data URL 和 blob URL 不需要认证
  if (url.startsWith('data:') || url.startsWith('blob:'))
    return false
  // 外部 URL 不需要认证
  if ((url.startsWith('http://') || url.startsWith('https://')) && !url.includes('/api/file/'))
    return false
  // 本地文件接口需要认证
  return true
}

// 加载图片
async function loadImage() {
  // 清理之前的 blob URL
  if (currentBlobUrl) {
    URL.revokeObjectURL(currentBlobUrl)
    currentBlobUrl = null
  }

  if (!props.src) {
    imageSrc.value = props.fallback || ''
    return
  }

  // 转换为完整 URL
  let url = props.src
  if (!url.startsWith('http://') && !url.startsWith('https://') && !url.startsWith('data:') && !url.startsWith('blob:')) {
    url = getFileUrl(props.src)
  }

  // 不需要认证的图片直接使用
  if (!needsAuth(url)) {
    imageSrc.value = url
    return
  }

  // 需要认证的图片，使用 fetch 获取
  try {
    const token = authStore.accessToken
    const response = await fetch(url, {
      headers: {
        Authorization: token ? `Bearer ${token}` : '',
      },
    })

    if (response.ok) {
      const blob = await response.blob()
      currentBlobUrl = URL.createObjectURL(blob)
      imageSrc.value = currentBlobUrl
    }
    else {
      console.warn('AuthImage: 图片加载失败', url)
      imageSrc.value = props.fallback || ''
    }
  }
  catch (error) {
    console.warn('AuthImage: 图片加载异常', error)
    imageSrc.value = props.fallback || ''
  }
}

function handleError() {
  if (props.fallback && imageSrc.value !== props.fallback) {
    imageSrc.value = props.fallback
  }
  emit('error')
}

function handleLoad() {
  emit('load')
}

// 监听 src 变化
watch(() => props.src, () => {
  loadImage()
}, { immediate: true })

// 组件卸载时清理 blob URL
onUnmounted(() => {
  if (currentBlobUrl) {
    URL.revokeObjectURL(currentBlobUrl)
  }
})
</script>
