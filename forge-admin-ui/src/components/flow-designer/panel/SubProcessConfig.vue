<script setup>
/**
 * SubProcessConfig — 子流程节点配置
 *
 * 子流程内部结构暂不支持画布内编辑（Phase 6 之后再扩展），仅提供 triggeredByEvent 开关。
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})
const emit = defineEmits(['update:config'])

const triggeredByEvent = computed({
  get: () => Boolean(props.node.config?.triggeredByEvent),
  set: v => emit('update:config', { triggeredByEvent: v }),
})
</script>

<template>
  <div class="space-y-3">
    <n-form-item label="事件子流程" label-placement="left">
      <n-switch v-model:value="triggeredByEvent" :disabled="readonly" />
    </n-form-item>
    <div class="text-xs rounded bg-gray-50 px-3 py-2 text-gray-500">
      子流程内部结构暂不支持画布内编辑，请使用高级模式或 BPMN XML 编辑器。
    </div>
  </div>
</template>
