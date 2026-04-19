<template>
  <n-modal
    v-model:show="visible"
    :mask-closable="false"
    :close-on-esc="false"
    transform-origin="center"
    preset="card"
    :style="{
      width: '340px',
      padding: '0',
      borderRadius: '12px',
      overflow: 'hidden',
    }"
    :bordered="false"
    :mask-style="{
      backdropFilter: 'blur(4px)',
      backgroundColor: 'rgba(0, 0, 0, 0.3)',
    }"
  >
    <template #header>
      <div class="modal-header">
        <div class="header-left">
          <div class="header-icon">
            <i class="ai-icon:shield-check" />
          </div>
          <span class="header-title">安全验证</span>
        </div>
        <button class="close-btn" aria-label="关闭" @click="handleClose">
          <i class="ai-icon:x" />
        </button>
      </div>
    </template>

    <div class="captcha-body">
      <!-- 验证状态提示 -->
      <div v-if="status === 'success'" class="status-overlay success">
        <div class="status-icon">
          <i class="ai-icon:check-circle" />
        </div>
        <span class="status-text">验证成功</span>
      </div>

      <div v-else-if="status === 'fail'" class="status-overlay fail">
        <div class="status-icon">
          <i class="ai-icon:x-circle" />
        </div>
        <span class="status-text">验证失败，请重试</span>
      </div>

      <!-- 滑块验证区域 -->
      <div v-else class="slider-container">
        <SlideVerify
          ref="slideVerifyRef"
          :w="300"
          :h="180"
          :slider-l="42"
          :slider-r="8"
          :accuracy="8"
          :imgs="images"
          :show-refresh="true"
          refresh-text=""
          text="向右滑动完成验证"
          success-text=""
          fail-text=""
          @success="onSuccess"
          @fail="onFail"
          @refresh="onRefresh"
        />
      </div>

      <!-- 底部提示 -->
      <div class="captcha-footer">
        <i class="ai-icon:shield-check footer-icon" />
        <span>安全验证由系统提供</span>
      </div>
    </div>
  </n-modal>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import SlideVerify from 'vue3-slide-verify'
import 'vue3-slide-verify/dist/style.css'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  images: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:show', 'success', 'fail', 'refresh'])

const visible = ref(props.show)
const slideVerifyRef = ref(null)
const status = ref('idle') // idle, success, fail

// 同步显示状态
watch(() => props.show, (val) => {
  visible.value = val
  if (val) {
    // 打开时重置状态
    status.value = 'idle'
  }
})

watch(visible, (val) => {
  emit('update:show', val)
})

// 验证成功
function onSuccess() {
  status.value = 'success'
  // 延迟关闭，让用户看到成功状态
  setTimeout(() => {
    emit('success')
    handleClose()
  }, 800)
}

// 验证失败
function onFail() {
  status.value = 'fail'
  emit('fail')
  // 延迟重置，让用户看到失败状态
  setTimeout(() => {
    status.value = 'idle'
    reset()
  }, 1000)
}

// 刷新
function onRefresh() {
  status.value = 'idle'
  emit('refresh')
}

// 重置滑块
function reset() {
  nextTick(() => {
    if (slideVerifyRef.value) {
      slideVerifyRef.value.reset()
    }
  })
}

// 关闭弹窗
function handleClose() {
  visible.value = false
  status.value = 'idle'
}

// 暴露方法给父组件
defineExpose({
  reset,
  handleClose,
})
</script>

<style scoped>
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: -20px -20px 0 -20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.5px;
}

.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

.captcha-body {
  position: relative;
  padding: 20px;
  background: #fff;
  min-height: 260px;
}

.slider-container {
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 状态覆盖层 */
.status-overlay {
  position: absolute;
  top: 20px;
  left: 20px;
  right: 20px;
  bottom: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  gap: 12px;
  animation: fadeIn 0.3s ease;
}

.status-overlay.success {
  background: linear-gradient(135deg, #d4edda 0%, #c3e6cb 100%);
}

.status-overlay.fail {
  background: linear-gradient(135deg, #f8d7da 0%, #f5c6cb 100%);
}

.status-icon {
  font-size: 48px;
}

.status-overlay.success .status-icon {
  color: #28a745;
}

.status-overlay.fail .status-icon {
  color: #dc3545;
}

.status-text {
  font-size: 16px;
  font-weight: 500;
}

.status-overlay.success .status-text {
  color: #155724;
}

.status-overlay.fail .status-text {
  color: #721c24;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 底部提示 */
.captcha-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  color: #999;
  font-size: 12px;
}

.footer-icon {
  font-size: 14px;
  color: #52c41a;
}

/* 覆盖 vue3-slide-verify 默认样式 */
:deep(.slide-verify) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8e8e8;
}

:deep(.slide-verify-slider) {
  border-radius: 0 0 8px 8px;
  background: #f7f9fa;
  height: 42px;
}

:deep(.slide-verify-slider-mask-item) {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
  transition: all 0.2s ease;
}

:deep(.slide-verify-slider-mask-item:hover) {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.5);
}

:deep(.slide-verify-slider-mask-item-icon) {
  color: #fff;
  font-size: 18px;
}

:deep(.slide-verify-slider-text) {
  color: #999;
  font-size: 13px;
}

:deep(.slide-verify-refresh-icon) {
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  background: rgba(0, 0, 0, 0.4);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

:deep(.slide-verify-refresh-icon:hover) {
  background: rgba(0, 0, 0, 0.6);
  transform: rotate(180deg);
}

/* 暗色主题适配 */
html.dark .captcha-body {
  background: #1f1f1f;
}

html.dark .status-overlay.success {
  background: linear-gradient(135deg, #1a3d1a 0%, #1e4620 100%);
}

html.dark .status-overlay.fail {
  background: linear-gradient(135deg, #3d1a1a 0%, #461e1e 100%);
}

html.dark .captcha-footer {
  border-top-color: #333;
  color: #666;
}

html.dark :deep(.slide-verify) {
  border-color: #333;
}

html.dark :deep(.slide-verify-slider) {
  background: #2a2a2a;
}

html.dark :deep(.slide-verify-slider-text) {
  color: #666;
}
</style>
