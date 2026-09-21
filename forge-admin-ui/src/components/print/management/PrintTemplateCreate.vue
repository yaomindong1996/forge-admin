<script setup>
import { NAlert, NInput, NModal } from 'naive-ui'
import { ref, watch } from 'vue'
import { usePrintTemplateStore } from '@/stores/print/printTemplateStore'

const props = defineProps({ show: Boolean, source: { type: Object, default: null } })
const emit = defineEmits(['update:show', 'created'])
const store = usePrintTemplateStore()
const name = ref('')
const busy = ref(false)
const error = ref('')
watch(() => props.show, () => {
  name.value = ''
  error.value = ''
})
async function create() {
  if (!props.source || !name.value.trim() || busy.value)
    return false
  busy.value = true
  error.value = ''
  try {
    const row = await store.create(props.source, name.value.trim())
    emit('created', row)
    emit('update:show', false)
  }
  catch (reason) {
    error.value = reason.message || '新建失败'
  }
  finally {
    busy.value = false
  }
  return false
}
</script>

<template>
  <NModal :show="show" preset="dialog" title="新建打印模板" positive-text="创建并设计" negative-text="取消" :loading="busy" :mask-closable="false" :positive-button-props="{ disabled: !name.trim() || !source }" @positive-click="create" @negative-click="!busy && emit('update:show', false)" @close="!busy && emit('update:show', false)">
    <NAlert v-if="error" type="error">
      {{ error }}
    </NAlert>
    <p>模板自动关联当前表单。发布后，还需随应用发布才能用于单据打印。</p>
    <NInput v-model:value="name" :input-props="{ 'aria-label': '打印模板名称' }" placeholder="模板名称" :maxlength="100" :disabled="busy" />
  </NModal>
</template>
