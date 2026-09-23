<template>
  <view class="ai-signature-pad" :class="{ 'is-disabled': disabled }">
    <wd-signature
      ref="signatureRef"
      :height="`${height}rpx`"
      :disabled="disabled"
      :disable-scroll="true"
      :enable-history="true"
      pen-color="#1d2129"
      background-color="#ffffff"
      @signing="handleSigning"
      @clear="handleClear"
      @confirm="handleConfirm"
    >
      <template #footer="{ clear: clearCanvas, confirm, revoke, restore, canUndo, canRedo }">
        <view v-if="!disabled" class="ai-signature-pad__actions">
          <wd-button size="small" plain :disabled="!canUndo || uploading" @click="revoke">撤销</wd-button>
          <wd-button size="small" plain :disabled="!canRedo || uploading" @click="restore">恢复</wd-button>
          <wd-button size="small" plain :disabled="!hasSignature() || uploading" @click="clearCanvas">清空</wd-button>
          <wd-button size="small" :loading="uploading" :disabled="!hasInk" @click="confirm">保存签名</wd-button>
        </view>
      </template>
    </wd-signature>
    <text v-if="modelValue && !hasInk" class="ai-signature-pad__saved">签名已保存，重新签写将覆盖原签名</text>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/store'
import { toast } from '@/utils/notify'
import { uploadRuntimeFile } from '@/utils/runtime-file-upload'

const props = defineProps({
  modelValue: { type: String, default: '' },
  height: { type: Number, default: 260 },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'success', 'error'])
const authStore = useAuthStore()
const signatureRef = ref(null)
const hasInk = ref(false)
const uploading = ref(false)
let pendingConfirmation = null
let pendingTimer = null

function handleSigning() {
  if (!hasInk.value) {
    hasInk.value = true
    if (props.modelValue) emit('update:modelValue', '')
  }
}

function handleClear() {
  hasInk.value = false
  emit('update:modelValue', '')
}

async function handleConfirm(result) {
  if (!result?.success || !result?.tempFilePath) {
    const error = new Error('签名图片生成失败')
    rejectPending(error)
    emit('error', error)
    toast(error.message, { type: 'error' })
    return
  }
  if (uploading.value) return

  uploading.value = true
  try {
    const uploaded = await uploadRuntimeFile({
      filePath: result.tempFilePath,
      fileName: `signature-${Date.now()}.png`,
      businessType: 'flow_signature',
      isPrivate: true,
      authStore,
    })
    emit('update:modelValue', uploaded.id)
    emit('success', uploaded.data)
    hasInk.value = false
    resolvePending(uploaded.id)
  }
  catch (error) {
    emit('error', error)
    rejectPending(error)
    toast(error?.message || '签名图片保存失败', { type: 'error' })
  }
  finally {
    uploading.value = false
  }
}

function clear() {
  if (signatureRef.value?.clear) signatureRef.value.clear()
  else handleClear()
}

function hasSignature() {
  return Boolean(props.modelValue) || hasInk.value
}

function upload() {
  if (props.modelValue && !hasInk.value) return Promise.resolve(props.modelValue)
  if (!hasInk.value) return Promise.reject(new Error('请完成手写签名'))
  if (pendingConfirmation) return pendingConfirmation.promise

  let resolvePromise
  let rejectPromise
  const promise = new Promise((resolve, reject) => {
    resolvePromise = resolve
    rejectPromise = reject
  })
  pendingConfirmation = { promise, resolve: resolvePromise, reject: rejectPromise }
  pendingTimer = setTimeout(() => rejectPending(new Error('签名图片生成超时，请重试')), 10000)
  signatureRef.value?.confirm?.()
  return promise
}

function resolvePending(fileId) {
  if (!pendingConfirmation) return
  clearPendingTimer()
  pendingConfirmation.resolve(fileId)
  pendingConfirmation = null
}

function rejectPending(error) {
  if (!pendingConfirmation) return
  clearPendingTimer()
  pendingConfirmation.reject(error)
  pendingConfirmation = null
}

function clearPendingTimer() {
  if (pendingTimer) clearTimeout(pendingTimer)
  pendingTimer = null
}

defineExpose({ hasSignature, upload, clear })
</script>

<style lang="scss" scoped>
.ai-signature-pad {
  overflow: hidden;
  border: 1rpx solid var(--border-color, #c9cdd4);
  border-radius: 12rpx;
  background: #ffffff;
}

.ai-signature-pad.is-disabled { opacity: .72; }

.ai-signature-pad__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12rpx;
  padding: 14rpx 16rpx 16rpx;
}

.ai-signature-pad__saved {
  display: block;
  padding: 0 16rpx 16rpx;
  color: var(--text-secondary, #4e5969);
  font-size: 22rpx;
}
</style>
