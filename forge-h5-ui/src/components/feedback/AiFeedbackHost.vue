<template>
  <wd-toast selector="forge-feedback-toast" />
  <wd-notify selector="forge-feedback-notify" root-portal />
  <wd-message-box selector="forge-feedback-message" root-portal />
  <wd-action-sheet
    v-model="actionSheetVisible"
    :title="actionSheetTitle"
    :actions="actionSheetActions"
    cancel-text="取消"
    root-portal
    :z-index="12000"
    @select="handleActionSelect"
    @cancel="finishActionSheet(-1)"
    @closed="finishActionSheet(-1)"
  />
</template>

<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { useMessage, useNotify, useToast } from 'wot-design-uni'
import { registerFeedbackHost } from './feedback-host'

const toastController = useToast('forge-feedback-toast')
const notifyController = useNotify('forge-feedback-notify')
const messageController = useMessage('forge-feedback-message')

const actionSheetVisible = ref(false)
const actionSheetTitle = ref('请选择')
const actionSheetActions = ref([])
let actionSheetResolve = null

function showActionSheet(options = {}) {
  finishActionSheet(-1)
  actionSheetTitle.value = options.title || '请选择'
  actionSheetActions.value = (options.actions || options.items || []).map((item, index) => {
    if (typeof item === 'string') {
      return { name: item, value: index }
    }
    return {
      ...item,
      name: item.name || item.label || `选项 ${index + 1}`,
      value: item.value ?? index,
    }
  })
  actionSheetVisible.value = true
  return new Promise((resolve) => {
    actionSheetResolve = resolve
  })
}

function handleActionSelect({ item, index }) {
  finishActionSheet(item?.value ?? index)
}

function finishActionSheet(value) {
  actionSheetVisible.value = false
  if (!actionSheetResolve) return
  const resolve = actionSheetResolve
  actionSheetResolve = null
  resolve(value)
}

const unregister = registerFeedbackHost({
  toast: toastController,
  notify: notifyController,
  message: messageController,
  actionSheet: showActionSheet,
})

onBeforeUnmount(() => {
  finishActionSheet(-1)
  unregister()
})
</script>
