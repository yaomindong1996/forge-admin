<script setup>
import { NAlert, NButton, NModal } from 'naive-ui'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import PrintTemplatePicker from '@/components/print/runtime/PrintTemplatePicker.vue'
import { flowPrintIdentityKey, useFlowPrintContextStore } from '@/stores/print/flowPrintContextStore'

const props = defineProps({
  row: { type: Object, default: null },
  scene: { type: String, required: true },
  taskFormInfo: { type: Object, default: null },
  businessContext: { type: Object, default: null },
  dirty: Boolean,
  disabled: Boolean,
})

const store = useFlowPrintContextStore()
const visible = ref(false)
const rowKey = computed(() => flowPrintIdentityKey(props.row || {}, props.scene))

watch(rowKey, (value, previous) => {
  if (previous && value !== previous)
    close()
})

onBeforeUnmount(close)

async function confirmSavedData() {
  if (!props.dirty)
    return true
  if (window.$dialog?.warning) {
    return new Promise((resolve) => {
      let settled = false
      const finish = (value) => {
        if (!settled) {
          settled = true
          resolve(value)
        }
      }
      window.$dialog.warning({
        title: '打印已保存数据',
        content: '当前表单有未保存修改。打印只读取服务端已保存记录，是否继续？',
        positiveText: '继续打印',
        negativeText: '返回保存',
        onPositiveClick: () => finish(true),
        onNegativeClick: () => finish(false),
        onClose: () => finish(false),
      })
    })
  }
  window.$message?.warning('当前表单有未保存修改，请先保存后再打印')
  return false
}

async function open() {
  if (!props.row || props.disabled || store.loading)
    return
  if (!await confirmSavedData())
    return
  const record = await store.sync({
    row: props.row,
    scene: props.scene,
    formInfo: props.taskFormInfo,
    businessContext: props.businessContext,
  })
  if (!record) {
    window.$message?.error(store.error || '流程打印上下文加载失败')
    return
  }
  visible.value = true
}

function close() {
  visible.value = false
  store.reset()
}

function updateVisible(value) {
  if (!value)
    close()
}
</script>

<template>
  <NButton
    size="small"
    secondary
    :loading="store.loading"
    :disabled="disabled || !row"
    aria-label="打印当前流程单据"
    @click="open"
  >
    <template #icon>
      <i class="i-lucide:printer" />
    </template>
    打印
  </NButton>

  <NModal
    :show="visible"
    preset="card"
    title="流程单据打印"
    class="flow-print-modal"
    :mask-closable="false"
    :close-on-esc="false"
    @update:show="updateVisible"
  >
    <NAlert v-if="store.error" type="error" class="flow-print-error">
      {{ store.error }}
    </NAlert>
    <PrintTemplatePicker v-if="store.record" :record="store.record" />
  </NModal>
</template>

<style scoped>
.flow-print-modal {
  width: min(1280px, calc(100vw - 32px));
  height: calc(100vh - 32px);
}

.flow-print-modal :deep(.n-card__content) {
  min-height: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.flow-print-error {
  margin: 10px 12px 0;
}
</style>
