<template>
  <div
    class="user-avatar-field"
    :class="{
      'is-dragging': dragging,
      'is-uploading': uploading,
      'is-disabled': disabled,
      'has-avatar': Boolean(previewSrc),
    }"
    @dragenter.prevent="onDragEnter"
    @dragover.prevent="onDragOver"
    @dragleave.prevent="onDragLeave"
    @drop.prevent="onDrop"
  >
    <!-- 左侧预览：大圆 + 右下角相机角标 -->
    <div class="user-avatar-field__preview-wrap">
      <button
        type="button"
        class="user-avatar-field__face"
        :disabled="disabled || uploading"
        :title="previewSrc ? '更换头像' : '上传头像'"
        @click="triggerPick"
      >
        <img
          v-if="previewSrc"
          :src="previewSrc"
          alt=""
          class="user-avatar-field__image"
        >
        <span v-else class="user-avatar-field__initial">{{ initial }}</span>
        <span v-if="uploading" class="user-avatar-field__loading" aria-hidden="true">
          <i class="i-material-symbols:progress-activity user-avatar-field__spin" />
        </span>
      </button>
      <button
        type="button"
        class="user-avatar-field__camera"
        :disabled="disabled || uploading"
        title="上传头像"
        @click="triggerPick"
      >
        <i class="i-material-symbols:photo-camera-outline-rounded" />
      </button>
      <button
        v-if="previewSrc && !disabled"
        type="button"
        class="user-avatar-field__clear"
        title="移除头像"
        :disabled="uploading"
        @click.stop="clearAvatar"
      >
        <i class="i-material-symbols:close-rounded" />
      </button>
    </div>

    <!-- 右侧仅保留说明；点击左侧圆/相机角标即可上传 -->
    <p class="user-avatar-field__tips">
      <span>支持 JPG、PNG 格式</span>
      <span>建议尺寸 200x200px 以上，大小不超过 {{ maxSizeMb }}MB</span>
    </p>

    <input
      ref="inputRef"
      class="user-avatar-field__input"
      type="file"
      accept="image/png,image/jpeg,image/webp"
      :disabled="disabled || uploading"
      @change="onFileChange"
    >
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useAuthStore } from '@/store'
import { generateUUID, resolveRenderableFileUrl } from '@/utils'

defineOptions({ name: 'UserAvatarField' })

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  nameHint: {
    type: String,
    default: '',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  maxSizeMb: {
    type: Number,
    default: 2,
  },
})

const emit = defineEmits(['update:modelValue'])

const authStore = useAuthStore()
const inputRef = ref(null)
const previewSrc = ref('')
const uploading = ref(false)
const dragging = ref(false)
let dragDepth = 0
let localObjectUrl = ''

const uploadUrl = `${import.meta.env.VITE_REQUEST_PREFIX || ''}/api/file/upload`

const initial = computed(() => {
  const text = String(props.nameHint || '用').trim()
  return (text.slice(0, 1) || '用').toUpperCase()
})

watch(
  () => props.modelValue,
  async (value) => {
    releaseLocalObjectUrl()
    if (!value) {
      previewSrc.value = ''
      return
    }
    try {
      previewSrc.value = await resolveRenderableFileUrl(value)
    }
    catch {
      previewSrc.value = ''
    }
  },
  { immediate: true },
)

function triggerPick() {
  if (props.disabled || uploading.value)
    return
  inputRef.value?.click()
}

function onFileChange(event) {
  const file = event.target?.files?.[0]
  event.target.value = ''
  if (file)
    void uploadFile(file)
}

function onDragEnter() {
  if (props.disabled || uploading.value)
    return
  dragDepth += 1
  dragging.value = true
}

function onDragOver() {
  if (props.disabled || uploading.value)
    return
  dragging.value = true
}

function onDragLeave() {
  dragDepth = Math.max(0, dragDepth - 1)
  if (dragDepth === 0)
    dragging.value = false
}

function onDrop(event) {
  dragDepth = 0
  dragging.value = false
  if (props.disabled || uploading.value)
    return
  const file = event.dataTransfer?.files?.[0]
  if (file)
    void uploadFile(file)
}

function clearAvatar() {
  if (props.disabled || uploading.value)
    return
  releaseLocalObjectUrl()
  previewSrc.value = ''
  emit('update:modelValue', '')
}

async function uploadFile(file) {
  const validationError = validateFile(file)
  if (validationError) {
    window.$message?.warning(validationError)
    return
  }

  uploading.value = true
  try {
    const prepared = await normalizeSquareImage(file)
    const formData = new FormData()
    formData.append('file', prepared)
    formData.append('businessType', 'avatar')

    const response = await fetch(uploadUrl, {
      method: 'POST',
      headers: {
        'Authorization': authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
        'X-Timestamp': Date.now().toString(),
        'X-Nonce': generateUUID(),
      },
      body: formData,
    })
    const res = await response.json()
    if (!response.ok || !(res?.code === 200 || res?.respCode === '0000') || !res?.data)
      throw new Error(res?.msg || res?.message || '头像上传失败')

    const fileId = res.data.fileId || res.data.filePath || res.data.id
    if (!fileId)
      throw new Error('头像上传失败')

    releaseLocalObjectUrl()
    if (res.data.accessUrl) {
      previewSrc.value = res.data.accessUrl
    }
    else {
      localObjectUrl = URL.createObjectURL(prepared)
      previewSrc.value = localObjectUrl
    }
    emit('update:modelValue', String(fileId))
    window.$message?.success('头像已上传')
  }
  catch (error) {
    window.$message?.error(error?.message || '头像上传失败')
  }
  finally {
    uploading.value = false
  }
}

function validateFile(file) {
  const allowed = ['image/png', 'image/jpeg', 'image/webp']
  if (!allowed.includes(file.type))
    return '仅支持 JPG、PNG、WebP 图片'
  if (file.size > props.maxSizeMb * 1024 * 1024)
    return `图片大小不能超过 ${props.maxSizeMb}MB`
  return ''
}

function normalizeSquareImage(file, outputSize = 512) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const image = new Image()
    image.onload = () => {
      try {
        const size = Math.min(image.width, image.height)
        const sx = (image.width - size) / 2
        const sy = (image.height - size) / 2
        const canvas = document.createElement('canvas')
        canvas.width = outputSize
        canvas.height = outputSize
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('图片处理失败'))
          return
        }
        ctx.drawImage(image, sx, sy, size, size, 0, 0, outputSize, outputSize)
        canvas.toBlob((blob) => {
          URL.revokeObjectURL(url)
          if (!blob) {
            reject(new Error('图片处理失败'))
            return
          }
          const name = String(file.name || 'avatar').replace(/\.\w+$/, '') || 'avatar'
          resolve(new File([blob], `${name}.jpg`, { type: 'image/jpeg' }))
        }, 'image/jpeg', 0.92)
      }
      catch (error) {
        URL.revokeObjectURL(url)
        reject(error)
      }
    }
    image.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('无法读取图片'))
    }
    image.src = url
  })
}

function releaseLocalObjectUrl() {
  if (localObjectUrl?.startsWith('blob:'))
    URL.revokeObjectURL(localObjectUrl)
  localObjectUrl = ''
}
</script>

<style scoped>
.user-avatar-field {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  min-height: 72px;
}

.user-avatar-field.is-dragging .user-avatar-field__face {
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--primary-color) 24%, transparent);
}

.user-avatar-field.is-disabled {
  opacity: 0.65;
}

.user-avatar-field__preview-wrap {
  position: relative;
  flex: none;
  width: 72px;
  height: 72px;
}

.user-avatar-field__face {
  position: relative;
  width: 72px;
  height: 72px;
  padding: 0;
  overflow: hidden;
  border: 0;
  /* 圆角方图，不用正圆 */
  border-radius: 10px;
  cursor: pointer;
  color: var(--primary-color);
  background: color-mix(in srgb, var(--primary-color) 12%, #eef3fb);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--primary-color) 14%, transparent);
}

.user-avatar-field__face:disabled {
  cursor: default;
}

.user-avatar-field__image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-avatar-field__initial {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0.02em;
  user-select: none;
}

.user-avatar-field__loading {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(15, 23, 42, 0.42);
  color: #fff;
  font-size: 20px;
}

.user-avatar-field__spin {
  animation: user-avatar-spin 0.8s linear infinite;
}

@keyframes user-avatar-spin {
  to { transform: rotate(360deg); }
}

.user-avatar-field__camera {
  position: absolute;
  right: -1px;
  bottom: -1px;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: 2px solid var(--bg-primary, #fff);
  border-radius: 999px;
  background: var(--bg-primary, #fff);
  color: var(--primary-color);
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.12);
}

.user-avatar-field__camera:hover:not(:disabled) {
  color: var(--primary-hover-color, var(--primary-color));
}

.user-avatar-field__camera:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.user-avatar-field__clear {
  position: absolute;
  top: -2px;
  right: -2px;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  padding: 0;
  border: 1px solid var(--border-light, #e8edf5);
  border-radius: 999px;
  background: var(--bg-primary, #fff);
  color: var(--text-tertiary, #8b95a8);
  cursor: pointer;
  font-size: 12px;
  line-height: 1;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}

.user-avatar-field__clear:hover:not(:disabled) {
  color: var(--error-color, #d03050);
  border-color: color-mix(in srgb, var(--error-color, #d03050) 35%, var(--border-light));
}

.user-avatar-field__tips {
  display: flex;
  margin: 0;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
  color: var(--text-tertiary, #8b95a8);
  font-size: 12px;
  line-height: 1.5;
}

.user-avatar-field__input {
  display: none;
}
</style>
