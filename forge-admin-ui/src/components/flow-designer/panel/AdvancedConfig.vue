<script setup>
/**
 * AdvancedConfig — 高级（advanced）节点的 rawXml 只读 / 编辑视图
 *
 * 默认 readonly=true，避免误改。如需编辑可手动切换 textarea 编辑模式 → 但风险自负
 * （rawXml 必须保持合法 BPMN 子树）。
 */
import { computed, ref } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})
const emit = defineEmits(['update:node'])

const editing = ref(false)

const rawXml = computed({
  get: () => props.node.rawXml || '',
  set: (v) => {
    if (props.readonly)
      return
    emit('update:node', { ...props.node, rawXml: v })
  },
})

function toggleEdit() {
  if (props.readonly)
    return
  editing.value = !editing.value
}
</script>

<template>
  <div class="space-y-3">
    <div class="text-xs border border-warning/30 rounded bg-warning/5 px-3 py-2 text-warning">
      该节点类型（{{ node.bpmnElementType }}）暂不被钉钉样式编辑器支持。
      <br>原始 BPMN XML 已保留，保存时原样回写；如需修改请使用高级模式编辑。
    </div>
    <n-form-item label="原始 BPMN XML" label-placement="top">
      <n-input
        :value="rawXml"
        type="textarea"
        :autosize="{ minRows: 6, maxRows: 14 }"
        :disabled="readonly || !editing"
        @update:value="rawXml = $event"
      />
    </n-form-item>
    <div v-if="!readonly" class="flex justify-end">
      <n-button size="small" @click="toggleEdit">
        {{ editing ? '锁定' : '编辑' }}
      </n-button>
    </div>
  </div>
</template>
