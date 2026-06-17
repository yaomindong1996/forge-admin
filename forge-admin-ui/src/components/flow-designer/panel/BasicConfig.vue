<script setup>
/**
 * BasicConfig — 节点基础属性（id 只读 / name / documentation）
 *
 * 与 NodePropertiesPanel.vue:109-128 字段对齐：节点ID（只读）/ 节点名称 / 节点描述
 * 通过 v-model:node 双向绑定到 NodeConfigDrawer 的 draftNode。
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})

const emit = defineEmits(['update:node'])

const name = computed({
  get: () => props.node?.name || '',
  set: (v) => {
    emit('update:node', { ...props.node, name: v })
  },
})

const documentation = computed({
  get: () => props.node?.config?.documentation || '',
  set: (v) => {
    emit('update:node', {
      ...props.node,
      config: { ...props.node.config, documentation: v },
    })
  },
})
</script>

<template>
  <div class="basic-config space-y-3">
    <n-form-item label="节点 ID" label-placement="left">
      <n-input :value="node.id" readonly />
    </n-form-item>
    <n-form-item label="节点名称" label-placement="left">
      <n-input v-model:value="name" :disabled="readonly" placeholder="请输入节点名称" />
    </n-form-item>
    <n-form-item label="节点描述" label-placement="left">
      <n-input
        v-model:value="documentation"
        type="textarea"
        :disabled="readonly"
        :autosize="{ minRows: 2, maxRows: 4 }"
        placeholder="可选：用于说明节点用途"
      />
    </n-form-item>
  </div>
</template>
