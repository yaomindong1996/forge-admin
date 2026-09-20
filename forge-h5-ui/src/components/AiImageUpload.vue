<template>
  <view class="ai-image-upload-shell" :class="{ 'is-uploading': uploading }">
    <view class="ai-image-upload" @click="openPicker">
      <slot :src="modelValue" :uploading="uploading">
        <view class="ai-image-upload__avatar">
          <AiAuthImage :src="modelValue" :fallback="fallback" mode="aspectFill" />
          <view v-if="!readonly" class="ai-image-upload__mask">
            <AiIcon icon="/static/icons/ai-icon/camera.svg" color="#ffffff" size="sm" />
            <text>{{ uploading ? '上传中' : '更换' }}</text>
          </view>
        </view>
      </slot>
    </view>

    <!-- #ifdef H5 -->
    <AiAvatarCropper
      v-model="cropVisible"
      :source="cropSource"
      :size="size"
      :quality="quality"
      @confirm="handleCropConfirm"
      @cancel="cancelCrop"
    />
    <!-- #endif -->
  </view>
</template>

<script setup>
import { onUnmounted, ref } from 'vue'
import AiAuthImage from '@/components/AiAuthImage.vue'
// #ifdef H5
import AiAvatarCropper from '@/components/AiAvatarCropper.vue'
// #endif
import AiIcon from '@/components/AiIcon.vue'
import { useAuthStore } from '@/store'
import { toast } from '@/utils/notify'
import { uploadRuntimeFile } from '@/utils/runtime-file-upload'

const props = defineProps({
  modelValue: {
    type: [String, Object],
    default: '',
  },
  fallback: {
    type: String,
    default: '',
  },
  businessType: {
    type: String,
    default: 'image',
  },
  crop: {
    type: Boolean,
    default: true,
  },
  size: {
    type: Number,
    default: 512,
  },
  quality: {
    type: Number,
    default: 0.9,
  },
  readonly: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'success', 'error', 'uploadStart', 'uploadEnd'])
const authStore = useAuthStore()
const uploading = ref(false)
const cropVisible = ref(false)
const cropSource = ref('')
let cropObjectUrl = ''

async function openPicker() {
  if (props.readonly || uploading.value) {
    return
  }
  const picked = await chooseImageFile()
  if (!picked?.url) {
    return
  }

  try {
    let useCropper = false
    // #ifdef H5
    useCropper = props.crop
    // #endif
    if (!useCropper) {
      await uploadFile(picked.file, picked.url)
      return
    }

    // #ifdef H5
    const file = picked.file || await urlToFile(picked.url)
    if (!file.type?.startsWith('image/')) {
      toast('请选择图片文件', { type: 'warning' })
      return
    }
    openCropper(URL.createObjectURL(file))
    // #endif
  }
  catch (error) {
    console.error('选择图片失败:', error)
    toast(error?.message || '选择图片失败', { type: 'error' })
  }
}

function chooseImageFile() {
  return new Promise((resolve) => {
    uni.chooseImage({
      count: 1,
      sizeType: ['original', 'compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const file = res.tempFiles?.[0]?.file || res.tempFiles?.[0]
        resolve({
          url: res.tempFilePaths?.[0] || file?.path || file?.url || '',
          file,
        })
      },
      fail: () => resolve(null),
    })
  })
}

async function urlToFile(url) {
  const response = await fetch(url)
  const blob = await response.blob()
  return new File([blob], `avatar-${Date.now()}.${resolveExtension(blob.type)}`, { type: blob.type || 'image/jpeg' })
}

function openCropper(url) {
  releaseCropObjectUrl()
  cropObjectUrl = url
  cropSource.value = url
  cropVisible.value = true
}

function cancelCrop() {
  cropVisible.value = false
  cropSource.value = ''
  releaseCropObjectUrl()
}

async function handleCropConfirm({ file }) {
  if (!file || uploading.value) {
    return
  }
  try {
    await uploadFile(file)
  }
  catch (error) {
    console.error('裁剪上传失败:', error)
    toast(error?.message || '裁剪上传失败', { type: 'error' })
  }
  finally {
    cancelCrop()
  }
}

async function uploadFile(file, filePath = '') {
  uploading.value = true
  emit('uploadStart')
  try {
    const uploaded = await uploadRuntimeFile({
      file,
      filePath: filePath || file?.path || file?.tempFilePath,
      fileName: file?.name || `image-${Date.now()}.jpg`,
      businessType: props.businessType,
      authStore,
    })
    emit('update:modelValue', uploaded.id)
    emit('success', uploaded.data)
  }
  catch (error) {
    emit('error', error)
    throw error
  }
  finally {
    uploading.value = false
    emit('uploadEnd')
  }
}

function resolveExtension(type) {
  if (type?.includes('png')) {
    return 'png'
  }
  if (type?.includes('webp')) {
    return 'webp'
  }
  return 'jpg'
}

function releaseCropObjectUrl() {
  // #ifdef H5
  if (cropObjectUrl?.startsWith('blob:')) {
    URL.revokeObjectURL(cropObjectUrl)
  }
  // #endif
  cropObjectUrl = ''
}

onUnmounted(() => {
  releaseCropObjectUrl()
})
</script>

<style lang="scss" scoped>
.ai-image-upload-shell,
.ai-image-upload {
  display: inline-flex;
}

.ai-image-upload__avatar {
  position: relative;
  width: 156rpx;
  height: 156rpx;
  overflow: hidden;
  border-radius: 40rpx;
  background: linear-gradient(135deg, #dbeafe, #e0e7ff);
}

.ai-image-upload__mask {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 10rpx 8rpx;
  background: rgba(15, 23, 42, 0.64);
}

.ai-image-upload__mask text {
  display: block;
  color: #ffffff;
  font-size: 20rpx;
  font-weight: 850;
}

</style>
