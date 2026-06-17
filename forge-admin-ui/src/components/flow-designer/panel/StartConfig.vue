<script setup>
/**
 * StartConfig — 开始节点配置（initiator + 表单引用）
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})
const emit = defineEmits(['update:config'])

function field(name, fallback = '') {
  return computed({
    get: () => props.node.config?.[name] ?? fallback,
    set: v => emit('update:config', { [name]: v }),
  })
}
const initiator = field('initiator', 'initiator')
const formKey = field('formKey')
const formUrl = field('formUrl')
</script>

<template>
  <div class="space-y-3">
    <n-form-item label="发起人变量" label-placement="left">
      <n-input v-model:value="initiator" :disabled="readonly" placeholder="initiator" />
    </n-form-item>
    <n-form-item label="表单 Key" label-placement="left">
      <n-input v-model:value="formKey" :disabled="readonly" placeholder="可选" />
    </n-form-item>
    <n-form-item label="表单 URL" label-placement="left">
      <n-input v-model:value="formUrl" :disabled="readonly" placeholder="可选：外部表单地址" />
    </n-form-item>
  </div>
</template>
