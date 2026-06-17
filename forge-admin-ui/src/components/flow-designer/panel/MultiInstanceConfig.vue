<script setup>
/**
 * MultiInstanceConfig — 会签设置
 *
 * 字段：
 *   - multiInstanceType: none / parallel / sequential
 *   - completionCondition: all / any / ratio
 *   - passRate: 当 ratio 时的百分比 (1-100)
 *
 * 与 user-task-parser / completion-condition 的字段命名保持一致（'ratio' 而非 'rate'）。
 */
import { computed } from 'vue'

const props = defineProps({
  config: { type: Object, required: true },
  readonly: Boolean,
})

const emit = defineEmits(['update:config'])

const MI_OPTIONS = [
  { label: '不会签（单人审批）', value: 'none' },
  { label: '并行会签（同时审批）', value: 'parallel' },
  { label: '顺序会签（依次审批）', value: 'sequential' },
]

const COND_OPTIONS = [
  { label: '全部通过', value: 'all' },
  { label: '任一通过', value: 'any' },
  { label: '通过比例', value: 'ratio' },
]

function useField(name, fallback) {
  return computed({
    get: () => props.config?.[name] ?? fallback,
    set: v => emit('update:config', { [name]: v }),
  })
}

const multiInstanceType = useField('multiInstanceType', 'none')
const completionCondition = useField('completionCondition', 'all')
const passRate = useField('passRate', 100)

const isMulti = computed(() => multiInstanceType.value !== 'none')
const isRatio = computed(() => completionCondition.value === 'ratio')
</script>

<template>
  <div class="space-y-3">
    <n-form-item label="会签类型" label-placement="left">
      <n-select
        v-model:value="multiInstanceType"
        :options="MI_OPTIONS"
        :disabled="readonly"
      />
    </n-form-item>

    <template v-if="isMulti">
      <n-form-item label="完成条件" label-placement="left">
        <n-select
          v-model:value="completionCondition"
          :options="COND_OPTIONS"
          :disabled="readonly"
        />
      </n-form-item>
      <n-form-item v-if="isRatio" label="通过比例 (%)" label-placement="left">
        <n-input-number
          v-model:value="passRate"
          :min="1"
          :max="100"
          :disabled="readonly"
        />
      </n-form-item>
    </template>

    <div class="text-xs rounded bg-gray-50 px-3 py-2 text-gray-500">
      <p>· 全部通过：所有候选人都需审批通过</p>
      <p>· 任一通过：任一候选人通过即视为通过</p>
      <p>· 通过比例：达到指定通过率即视为通过</p>
    </div>
  </div>
</template>
