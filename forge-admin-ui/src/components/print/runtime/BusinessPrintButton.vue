<script setup>
import { NButton, NModal } from 'naive-ui'
import { computed, watch } from 'vue'
import PrintTemplatePicker from '@/components/print/runtime/PrintTemplatePicker.vue'
import { useBusinessPrint } from '@/components/print/runtime/useBusinessPrint'

const props = defineProps({
  businessSourceId: { type: [String, Number], default: null },
  sourceCode: { type: String, required: true },
  sourceType: {
    type: String,
    default: 'SERVICE',
    validator: value => ['SERVICE', 'DATASET'].includes(value),
  },
  objectCode: { type: String, default: '' },
  recordId: { type: [String, Number], required: true },
  scene: { type: String, default: 'DETAIL' },
  params: { type: Object, default: () => ({}) },
  label: { type: String, default: '打印' },
  title: { type: String, default: '业务单据打印' },
  size: { type: String, default: 'small' },
  disabled: Boolean,
})
const emit = defineEmits(['open', 'close', 'error'])
const print = useBusinessPrint(computed(() => ({
  businessSourceId: props.businessSourceId,
  sourceCode: props.sourceCode,
  sourceType: props.sourceType,
  objectCode: props.objectCode,
  recordId: props.recordId,
  scene: props.scene,
  params: props.params,
})))

watch(print.error, (value) => {
  if (value) {
    window.$message?.error?.(value)
    emit('error', value)
  }
})

async function open() {
  const record = await print.open()
  if (record)
    emit('open', record)
}

function close() {
  print.close()
  emit('close')
}
</script>

<template>
  <NButton :size="size" :disabled="disabled" :loading="print.loading.value" secondary @click="open">
    <template #icon>
      <i class="i-lucide:printer" />
    </template>
    {{ label }}
  </NButton>

  <NModal
    :show="print.visible.value"
    preset="card"
    :title="title"
    class="business-print-modal"
    :mask-closable="false"
    :close-on-esc="false"
    @update:show="value => !value && close()"
  >
    <PrintTemplatePicker v-if="print.record.value" :record="print.record.value" @back="close" />
  </NModal>
</template>

<style scoped>
.business-print-modal {
  width: min(1280px, calc(100vw - 32px));
  height: calc(100vh - 32px);
}

.business-print-modal :deep(.n-card__content) {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0;
}
</style>
