<script setup>
/**
 * CarbonCopyConfig — 抄送人节点
 *
 * 字段：candidateUsers / candidateUserNames / implementation
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})
const emit = defineEmits(['update:config'])

const candidateUsersText = computed({
  get: () => (props.node.config?.candidateUsers || []).join(','),
  set: v => emit('update:config', {
    candidateUsers: String(v || '').split(/[,，\s]+/).filter(Boolean),
  }),
})

const implementation = computed({
  get: () => props.node.config?.implementation || '',
  set: v => emit('update:config', { implementation: v }),
})
</script>

<template>
  <div class="space-y-3">
    <n-form-item label="抄送用户 ID" label-placement="left">
      <n-input
        v-model:value="candidateUsersText"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 4 }"
        placeholder="多个用户 ID 用英文逗号分隔"
        :disabled="readonly"
      />
    </n-form-item>
    <n-form-item label="抄送服务表达式" label-placement="left">
      <n-input
        v-model:value="implementation"
        :disabled="readonly"
        placeholder="${ccService.send(...)}"
      />
    </n-form-item>
    <div class="text-xs rounded bg-gray-50 px-3 py-2 text-gray-500">
      抄送只通知，不需要审批，流程会立即流转到下一节点。
    </div>
  </div>
</template>
