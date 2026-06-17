<script setup>
/**
 * ScriptConfig — 脚本任务
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})
const emit = defineEmits(['update:config'])

const FORMAT_OPTIONS = [
  { label: 'JavaScript', value: 'javascript' },
  { label: 'Groovy', value: 'groovy' },
  { label: 'Python (Jython)', value: 'python' },
]

function field(name, fallback = '') {
  return computed({
    get: () => props.node.config?.[name] ?? fallback,
    set: v => emit('update:config', { [name]: v }),
  })
}
const scriptFormat = field('scriptFormat', 'javascript')
const script = field('script')
const async = field('async', false)
</script>

<template>
  <div class="space-y-3">
    <n-form-item label="脚本语言" label-placement="left">
      <n-select v-model:value="scriptFormat" :options="FORMAT_OPTIONS" :disabled="readonly" />
    </n-form-item>
    <n-form-item label="脚本内容" label-placement="left">
      <n-input
        v-model:value="script"
        type="textarea"
        :autosize="{ minRows: 6, maxRows: 14 }"
        :disabled="readonly"
        placeholder="// 在此编写脚本"
      />
    </n-form-item>
    <n-form-item label="异步执行" label-placement="left">
      <n-switch v-model:value="async" :disabled="readonly" />
    </n-form-item>
  </div>
</template>
