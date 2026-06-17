<script setup>
/**
 * MergeNode — 汇合点（仅作纯粹视觉提示）
 *
 * 当一个节点入度 ≥ 2 且 config.mergeNode=true 时，画布在该节点上方位置渲染一个
 * 小圆点+横线，提示这是分支汇合处。组件本身不参与点击交互。
 */
defineProps({
  position: { type: Object, required: true }, // { x, y }
})
</script>

<template>
  <div
    class="merge-node-marker absolute z-0 flex items-center justify-center"
    :style="{
      left: `${position.x - 8}px`,
      top: `${position.y - 8}px`,
      width: '16px',
      height: '16px',
    }"
  >
    <span class="block h-2 w-2 rounded-full bg-gray-300" />
  </div>
</template>
