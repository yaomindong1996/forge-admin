<script setup>
/**
 * CallActivityConfig — 调用活动（calledElement = 被调用流程的 process id）
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})
const emit = defineEmits(['update:config'])

const calledElement = computed({
  get: () => props.node.config?.calledElement || '',
  set: v => emit('update:config', { calledElement: v }),
})
</script>

<template>
  <div class="space-y-3">
    <n-form-item label="被调用流程 ID" label-placement="left">
      <n-input v-model:value="calledElement" :disabled="readonly" placeholder="如：subProcess_xxx" />
    </n-form-item>
    <div class="text-xs rounded bg-gray-50 px-3 py-2 text-gray-500">
      调用活动会启动一个独立流程实例，主流程在被调用流程结束后继续。
    </div>
  </div>
</template>
