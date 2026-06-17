<script setup>
/**
 * ConditionConfig — 网关条件配置
 *
 * 显示该网关所有出边的 condition / isDefault：
 * - 用户在此选择哪条边作为默认（互斥单选）
 * - 编辑每条边的 condition 表达式
 *
 * 因为 ConditionConfig 涉及网关的所有出边而不仅是节点本身的 config，本组件接收
 * outgoingEdges + onEdgeUpdate 作为额外 props（注入式 — 由 NodeConfigDrawer 父组件传入）。
 *
 * Props:
 *   - node                  网关节点
 *   - outgoingEdges         网关的出边数组
 *   - readonly
 *
 * Events:
 *   - update:edge(edgeId, patch)
 *   - update:config(patch)   仅写 defaultFlowId 到节点 config，便于 markBranches 兼容
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  outgoingEdges: { type: Array, default: () => [] },
  readonly: Boolean,
})

const emit = defineEmits(['update:edge', 'update:config'])

const edges = computed(() => props.outgoingEdges || [])

function setDefault(edgeId) {
  if (props.readonly)
    return
  for (const e of edges.value)
    emit('update:edge', e.id, { isDefault: e.id === edgeId, condition: e.id === edgeId ? '' : e.condition })
  emit('update:config', { defaultFlowId: edgeId })
}

function updateCondition(edgeId, value) {
  emit('update:edge', edgeId, { condition: value, conditionType: 'expression' })
}
</script>

<template>
  <div class="space-y-3">
    <div class="text-sm text-gray-600">
      该网关共 {{ edges.length }} 条出边
    </div>
    <div v-if="edges.length === 0" class="text-xs rounded bg-gray-50 px-3 py-2 text-gray-400">
      暂无出边，先在画布上从该网关连接到下游节点
    </div>
    <div
      v-for="e in edges"
      :key="e.id"
      class="border border-gray-100 rounded p-3 space-y-2"
    >
      <div class="flex items-center justify-between">
        <span class="text-sm font-medium">分支 {{ e.branchId || e.id }} → {{ e.target }}</span>
        <n-tag v-if="e.isDefault" type="warning" size="small">
          默认分支
        </n-tag>
      </div>

      <n-form-item label="条件表达式" label-placement="left">
        <n-input
          :value="e.condition"
          :disabled="readonly || e.isDefault"
          placeholder="${days > 3}"
          @update:value="updateCondition(e.id, $event)"
        />
      </n-form-item>
      <div class="flex items-center gap-2">
        <n-radio
          :checked="e.isDefault"
          :disabled="readonly"
          @click="setDefault(e.id)"
        >
          作为默认分支
        </n-radio>
      </div>
    </div>
  </div>
</template>
