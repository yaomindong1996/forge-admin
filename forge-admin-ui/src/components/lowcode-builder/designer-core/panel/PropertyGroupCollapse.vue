<script setup>
/**
 * PropertyGroupCollapse — 属性分组折叠组件
 * @description 用于 SpecPropertyPanel 中"高级"属性的分组折叠。
 *   点击标题栏切换展开/收起；展开时显示子内容，收起时隐藏。
 *   紧凑风格与设计器面板的中高信息密度一致。
 */
import { ChevronForwardOutline } from '@vicons/ionicons5'
import { ref } from 'vue'

const props = defineProps({
  /** 分组标题 */
  title: { type: String, required: true },
  /** 默认是否展开 */
  defaultExpanded: { type: Boolean, default: false },
  /** 子项数量（显示在标题右侧徽章中） */
  count: { type: Number, default: 0 },
})

const expanded = ref(props.defaultExpanded)

function toggle() {
  expanded.value = !expanded.value
}
</script>

<template>
  <div class="property-group-collapse" :class="{ expanded }">
    <button type="button" class="property-group-collapse-header" @click="toggle">
      <n-icon :size="12" class="property-group-collapse-arrow">
        <ChevronForwardOutline />
      </n-icon>
      <span class="property-group-collapse-title">{{ title }}</span>
      <span v-if="count > 0" class="property-group-collapse-badge">{{ count }}</span>
    </button>
    <div v-show="expanded" class="property-group-collapse-body">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.property-group-collapse {
  grid-column: 1 / -1;
}

.property-group-collapse-header {
  display: flex;
  align-items: center;
  gap: 5px;
  width: 100%;
  padding: 4px 0;
  border: none;
  border-bottom: 1px dashed var(--n-border-color, #e5e7eb);
  background: transparent;
  color: var(--n-text-color, #64748b);
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  cursor: pointer;
  transition: color 120ms ease;
}

.property-group-collapse-header:hover {
  color: var(--n-text-color, #334155);
}

.property-group-collapse-arrow {
  transition: transform 200ms ease;
  flex-shrink: 0;
}

.property-group-collapse.expanded .property-group-collapse-arrow {
  transform: rotate(90deg);
}

.property-group-collapse-title {
  flex: 1;
  text-align: left;
}

.property-group-collapse-badge {
  display: inline-grid;
  place-items: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--n-border-color, #e5e7eb);
  color: var(--n-text-color, #64748b);
  font-size: 10px;
  font-weight: 500;
  line-height: 1;
}

.property-group-collapse-body {
  display: contents;
}
</style>
