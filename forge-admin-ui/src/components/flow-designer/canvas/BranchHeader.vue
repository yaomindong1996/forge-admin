<script setup>
/**
 * BranchHeader — 分支首部条件徽章
 *
 * 用法：在每条分支线的首端（紧贴网关下方）渲染一个带 condition 文本的胶囊，
 * 标识 "if (days > 3)" / "默认" 等。
 *
 * Props:
 *   - edge       branchEdge（含 condition / isDefault / branchId）
 *   - position   { x, y } 画布坐标
 *
 * Events:
 *   - click  点击徽章 → 父组件可打开条件配置抽屉
 */
import { computed } from 'vue'

const props = defineProps({
  edge: { type: Object, required: true },
  position: { type: Object, required: true },
})

defineEmits(['click'])

const labelText = computed(() => {
  if (props.edge?.isDefault)
    return '默认'
  const t = String(props.edge?.condition || '')
  if (!t)
    return '配置条件'
  return t.length > 18 ? `${t.slice(0, 18)}…` : t
})

const colorClass = computed(() => {
  if (props.edge?.isDefault)
    return 'border-warning text-warning bg-warning/5'
  if (props.edge?.condition)
    return 'border-primary text-primary bg-primary/5'
  return 'border-dashed border-gray-300 text-gray-400 bg-white'
})
</script>

<template>
  <div
    class="branch-header text-xs absolute z-10 flex cursor-pointer items-center border rounded-full px-2 py-0.5 hover:shadow-sm"
    :class="colorClass"
    :style="{
      left: `${position.x}px`,
      top: `${position.y}px`,
      transform: 'translate(-50%, -50%)',
    }"
    @click.stop="$emit('click', edge)"
  >
    {{ labelText }}
  </div>
</template>
