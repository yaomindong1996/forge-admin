<template>
  <div class="input-area">
    <div class="input-col">
      <div class="composer" :class="{ 'is-focused': focused }">
        <div class="input-attachments" :class="{ 'is-empty': !attach.length }">
          <ImageUpload
            ref="imageUploadRef"
            v-model="attach"
            value-type="array"
            business-type="ai-chat"
            :limit="4"
            :multiple="true"
            :show-tip="false"
            :disabled="isStreaming"
          />
        </div>

        <NInput
          v-model:value="text"
          type="textarea"
          :bordered="false"
          :autosize="{ minRows: 1, maxRows: 5 }"
          placeholder="发消息…"
          :disabled="isStreaming"
          @focus="focused = true"
          @blur="focused = false"
          @keydown.enter.exact="onEnter"
        />

        <div class="composer-bar">
          <div class="composer-tools">
            <NTooltip>
              <template #trigger>
                <button
                  type="button"
                  class="tool-chip"
                  :disabled="isStreaming"
                  aria-label="添加图片"
                  @click="triggerAttach"
                >
                  <NIcon size="16">
                    <AddOutline />
                  </NIcon>
                </button>
              </template>
              添加图片
            </NTooltip>
            <span class="tool-divider" aria-hidden="true" />
            <span class="composer-hint">Enter 发送</span>
          </div>

          <NButton
            circle
            :type="isStreaming ? 'error' : 'primary'"
            size="small"
            class="send-btn"
            :disabled="!isStreaming && !text.trim()"
            @click="isStreaming ? emit('stop') : emit('send')"
          >
            <template #icon>
              <NIcon>
                <StopCircleOutline v-if="isStreaming" />
                <SendOutline v-else />
              </NIcon>
            </template>
          </NButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { AddOutline, SendOutline, StopCircleOutline } from '@vicons/ionicons5'
import { NButton, NIcon, NInput, NTooltip } from 'naive-ui'
import { computed, ref } from 'vue'
import ImageUpload from '@/components/image-upload/index.vue'

defineOptions({ name: 'ChatInput' })

const props = defineProps({
  modelValue: { type: String, default: '' },
  attachments: { type: Array, default: () => [] },
  isStreaming: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'update:attachments', 'send', 'stop'])

const text = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v),
})
const attach = computed({
  get: () => props.attachments,
  set: v => emit('update:attachments', v),
})

const imageUploadRef = ref(null)
const focused = ref(false)

function triggerAttach() {
  imageUploadRef.value?.triggerUpload?.()
}

function onEnter(e) {
  if (e?.shiftKey)
    return
  e?.preventDefault()
  emit('send')
}

function clearAttachments() {
  imageUploadRef.value?.clear?.()
}

defineExpose({ clearAttachments })
</script>

<style scoped>
.input-area {
  flex: none;
  padding: 8px 20px 14px;
  background: linear-gradient(180deg, transparent 0%, var(--surface) 28%);
}

.input-col {
  max-width: var(--chat-col);
  margin: 0 auto;
}

/* 豆包式悬浮输入条：浅底、大圆角、轻阴影，默认单行高度 */
.composer {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px 8px;
  background: var(--bg-soft, #f5f7fa);
  border: 1px solid color-mix(in srgb, var(--border, #e5e7eb) 80%, transparent);
  border-radius: 22px;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.04),
    0 8px 24px rgba(15, 23, 42, 0.04);
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    background 0.15s ease;
}

.composer.is-focused {
  background: var(--surface, #fff);
  border-color: color-mix(in srgb, var(--primary, #3b82f6) 55%, var(--border, #e5e7eb));
  box-shadow:
    0 0 0 3px color-mix(in srgb, var(--primary, #3b82f6) 12%, transparent),
    0 8px 24px rgba(15, 23, 42, 0.06);
}

:global(.dark) .composer {
  background: color-mix(in srgb, var(--bg-soft, #1e293b) 88%, #000);
}

:global(.dark) .composer.is-focused {
  background: var(--surface, #131c2b);
}

.input-attachments.is-empty {
  height: 0;
  margin: 0;
  padding: 0;
  overflow: hidden;
  pointer-events: none;
}

.input-attachments :deep(.upload-trigger) {
  display: none;
}

.input-attachments :deep(.image-list) {
  gap: 8px;
}

.input-attachments :deep(.image-item) {
  width: 56px;
  height: 56px;
  margin: 0 0 4px;
  border-radius: 10px;
}

.input-attachments :deep(.image-name) {
  display: none;
}

.composer :deep(.n-input) {
  background: transparent !important;
}

.composer :deep(.n-input-wrapper) {
  padding: 0 !important;
}

/* 光标 / 占位符垂直对齐：textarea 与 placeholder 共用同一套字号、行高、内边距 */
.composer :deep(.n-input .n-input__textarea-el),
.composer :deep(.n-input__placeholder) {
  font-size: 14px;
  line-height: 22px;
}

.composer :deep(.n-input .n-input__textarea-el) {
  min-height: 22px !important;
  max-height: 140px;
  padding: 6px 4px !important;
  resize: none;
}

.composer :deep(.n-input__placeholder) {
  top: 6px !important;
  left: 4px !important;
  right: 4px !important;
  margin: 0 !important;
}

.composer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 32px;
  padding-top: 2px;
}

.composer-tools {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.tool-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 999px;
  background: transparent;
  color: var(--text-muted, #94a3b8);
  cursor: pointer;
  transition:
    color 0.15s ease,
    background 0.15s ease,
    border-color 0.15s ease;
}

.tool-chip:hover:not(:disabled) {
  color: var(--primary, #2563eb);
  background: color-mix(in srgb, var(--primary, #2563eb) 8%, transparent);
  border-color: color-mix(in srgb, var(--primary, #2563eb) 16%, transparent);
}

.tool-chip:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.tool-divider {
  width: 1px;
  height: 14px;
  background: var(--border, #e5e7eb);
  flex: none;
}

.composer-hint {
  color: var(--text-muted, #94a3b8);
  font-size: 11px;
  user-select: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.send-btn {
  flex-shrink: 0;
  width: 32px !important;
  height: 32px !important;
}
</style>
