<script setup>
/**
 * EndConfig — 结束节点（normal / terminate）
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})
const emit = defineEmits(['update:config'])

const END_OPTIONS = [
  { label: '正常结束', value: 'normal' },
  { label: '强制终止流程', value: 'terminate' },
]

const endType = computed({
  get: () => props.node.config?.endType || 'normal',
  set: v => emit('update:config', { endType: v }),
})
</script>

<template>
  <div class="space-y-3">
    <n-form-item label="结束类型" label-placement="left">
      <n-radio-group v-model:value="endType" :disabled="readonly">
        <n-space>
          <n-radio v-for="opt in END_OPTIONS" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </n-radio>
        </n-space>
      </n-radio-group>
    </n-form-item>
    <div class="text-xs rounded bg-gray-50 px-3 py-2 text-gray-500">
      <p>· 正常结束：当前路径完成</p>
      <p>· 强制终止：直接结束整个流程实例（不等待其他并行分支）</p>
    </div>
  </div>
</template>
