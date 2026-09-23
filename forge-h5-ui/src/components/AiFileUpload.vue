<template>
  <view class="ai-file-upload">
    <view v-if="fileItems.length" class="ai-file-upload__list">
      <view v-for="item in fileItems" :key="item.id" class="ai-file-upload__item">
        <AiIcon icon="/static/icons/ai-icon/file-text.svg" color="#4266f7" size="sm" />
        <text class="ai-file-upload__name">{{ item.name }}</text>
        <button v-if="!readonly" class="ai-file-upload__remove" @click="removeFile(item.id)">移除</button>
      </view>
    </view>
    <button v-if="!readonly" class="ai-file-upload__trigger" :disabled="uploading" @click="chooseFile">
      <AiIcon icon="/static/icons/ai-icon/upload.svg" color="#4266f7" size="sm" />
      <text>{{ uploading ? '上传中…' : '上传附件' }}</text>
    </button>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import AiIcon from '@/components/AiIcon.vue'
import { useAuthStore } from '@/store'
import { toast } from '@/utils/notify'
import { uploadRuntimeFile } from '@/utils/runtime-file-upload'

const props = defineProps({
  modelValue: { type: [String, Array], default: '' },
  readonly: { type: Boolean, default: false },
  businessType: { type: String, default: 'flow_attachment' },
  maxCount: { type: Number, default: 9 },
})
const emit = defineEmits(['update:modelValue', 'success'])
const authStore = useAuthStore()
const uploading = ref(false)

const fileItems = computed(() => normalizeFiles(props.modelValue))

async function chooseFile() {
  if (uploading.value || fileItems.value.length >= props.maxCount) return
  try {
    const picked = await pickFile()
    if (!picked?.file && !picked?.filePath) return
    uploading.value = true
    const uploaded = await uploadFile(picked)
    const next = [...fileItems.value, uploaded]
    emitValue(next)
    emit('success', uploaded)
  }
  catch (error) {
    console.error('上传附件失败:', error)
    toast(error?.message || '附件上传失败', { type: 'error' })
  }
  finally { uploading.value = false }
}

function pickFile() {
  return new Promise((resolve) => {
    // #ifdef H5
    const input = document.createElement('input')
    input.type = 'file'
    input.onchange = () => {
      const file = input.files?.[0]
      resolve(file ? { file, name: file.name } : null)
    }
    input.click()
    // #endif
    // #ifndef H5
    uni.chooseMessageFile({
      count: 1,
      type: 'file',
      success: (res) => {
        const item = res.tempFiles?.[0]
        resolve(item ? { filePath: item.path || item.tempFilePath, name: item.name || item.path?.split('/').pop() } : null)
      },
      fail: () => resolve(null),
    })
    // #endif
  })
}

async function uploadFile(picked) {
  const uploaded = await uploadRuntimeFile({
    file: picked.file,
    filePath: picked.filePath,
    fileName: picked.name || picked.file?.name || `attachment-${Date.now()}`,
    businessType: props.businessType,
    isPrivate: true,
    authStore,
  })
  return { id: uploaded.id, name: uploaded.name }
}

function removeFile(id) { emitValue(fileItems.value.filter(item => item.id !== id)) }
function emitValue(items) { emit('update:modelValue', items.map(item => item.id).join(',')) }
function normalizeFiles(value) {
  if (!value) return []
  if (Array.isArray(value)) return value.map(item => typeof item === 'object' ? { id: String(item.id || item.fileId || item.value || ''), name: item.name || item.fileName || item.originalName || String(item.id || item.fileId || '') } : { id: String(item), name: String(item) }).filter(item => item.id)
  return String(value).split(',').map(value => value.trim()).filter(Boolean).map(id => ({ id, name: id }))
}
</script>

<style lang="scss" scoped>
.ai-file-upload { display: flex; flex-direction: column; gap: 12rpx; }
.ai-file-upload__list { display: flex; flex-direction: column; gap: 10rpx; }
.ai-file-upload__item { display: flex; min-width: 0; min-height: 88rpx; align-items: center; gap: 12rpx; padding: 0 16rpx; border: 1rpx solid #e5e6eb; border-radius: var(--radius-sm); background: #f5f7ff; }
.ai-file-upload__name { overflow: hidden; flex: 1; color: #4e5969; font-size: 23rpx; text-overflow: ellipsis; white-space: nowrap; }
.ai-file-upload__remove { min-height: 88rpx; margin: 0; padding: 0 16rpx; border: 0; color: #4e5969; font-size: 22rpx; line-height: 88rpx; background: transparent; }
.ai-file-upload__remove::after, .ai-file-upload__trigger::after { border: 0; }
.ai-file-upload__trigger { display: inline-flex; width: fit-content; min-height: 88rpx; align-items: center; gap: 10rpx; margin: 0; padding: 0 24rpx; border: 1rpx dashed #4266f7; border-radius: var(--radius-sm); color: #4266f7; font-size: 24rpx; background: #f5f7ff; }
.ai-file-upload__trigger[disabled] { opacity: .6; }
</style>
