<template>
  <div
    class="enhanced-card"
    :class="[
      { 'card-border': bordered },
      { 'card-hover': hover },
      { 'card-glass': glass },
      `shadow-${shadow}`,
      `radius-${radius}`
    ]"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
    @mousemove="handleMouseMove"
  >
    <div v-if="title || $slots.header" class="card-header">
      <div v-if="title" class="card-title">
        <i v-if="icon" :class="icon" class="title-icon" />
        <span>{{ title }}</span>
        <n-tag v-if="tag" :type="tagType" size="small" class="title-tag">
          {{ tag }}
        </n-tag>
      </div>
      <slot name="header" />
      <div v-if="$slots.actions" class="card-actions">
        <slot name="actions" />
      </div>
    </div>
    
    <div class="card-body" :class="{ 'body-no-padding': !padding }">
      <slot />
    </div>
    
    <div v-if="$slots.footer" class="card-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  // 是否显示边框
  bordered: {
    type: Boolean,
    default: true
  },
  // 是否支持悬停效果
  hover: {
    type: Boolean,
    default: false
  },
  // 是否使用玻璃态效果
  glass: {
    type: Boolean,
    default: false
  },
  // 卡片标题
  title: String,
  // 标题图标
  icon: String,
  // 标题标签
  tag: String,
  // 标签类型
  tagType: {
    type: String,
    default: 'default'
  },
  // 阴影级别
  shadow: {
    type: String,
    default: 'md',
    validator: (value) => ['none', 'xs', 'sm', 'md', 'lg', 'xl', '2xl'].includes(value)
  },
  // 圆角级别
  radius: {
    type: String,
    default: 'lg',
    validator: (value) => ['none', 'sm', 'md', 'lg', 'xl', '2xl', 'full'].includes(value)
  },
  // 是否有内边距
  padding: {
    type: Boolean,
    default: true
  }
})

const mousePosition = ref({ x: 0, y: 0 })

const handleMouseEnter = (e) => {
  updateMousePosition(e)
}

const handleMouseLeave = () => {
  mousePosition.value = { x: 0, y: 0 }
}

const handleMouseMove = (e) => {
  updateMousePosition(e)
}

const updateMousePosition = (e) => {
  const rect = e.currentTarget.getBoundingClientRect()
  const x = ((e.clientX - rect.left) / rect.width) * 100
  const y = ((e.clientY - rect.top) / rect.height) * 100
  mousePosition.value = { x, y }
}
</script>

<style scoped>
.enhanced-card {
  position: relative;
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-card);
  transition: box-shadow var(--transition-base), border-color var(--transition-base);
  overflow: hidden;
}

/* 玻璃态效果 */
.card-glass {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-color: rgba(255, 255, 255, 0.3);
}

.dark .card-glass {
  background: rgba(15, 23, 42, 0.85);
  border-color: rgba(255, 255, 255, 0.08);
}

/* 悬停效果 - SnowAdmin 风格，不上浮，只加深阴影 */
.card-hover:hover {
  box-shadow: var(--shadow-card-hover);
  border-color: var(--border-default);
}

/* 边框 */
.card-border {
  border-color: var(--border-light);
}

/* 阴影级别 */
.shadow-none { box-shadow: none; }
.shadow-xs { box-shadow: var(--shadow-xs); }
.shadow-sm { box-shadow: var(--shadow-sm); }
.shadow-md { box-shadow: var(--shadow-md); }
.shadow-lg { box-shadow: var(--shadow-lg); }
.shadow-xl { box-shadow: var(--shadow-xl); }
.shadow-2xl { box-shadow: var(--shadow-2xl); }

/* 圆角级别 */
.radius-none { border-radius: 0; }
.radius-sm { border-radius: var(--radius-sm); }
.radius-md { border-radius: var(--radius-md); }
.radius-lg { border-radius: var(--radius-lg); }
.radius-xl { border-radius: var(--radius-xl); }
.radius-2xl { border-radius: var(--radius-2xl); }
.radius-full { border-radius: var(--radius-full); }

/* 头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-secondary);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.title-icon {
  font-size: 16px;
  color: var(--primary-500);
}

.title-tag {
  margin-left: 4px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 主体 */
.card-body {
  padding: var(--space-4);
  flex: 1;
}

.body-no-padding {
  padding: 0;
}

/* 底部 */
.card-footer {
  padding: 10px 16px;
  border-top: 1px solid var(--border-light);
  background: var(--bg-secondary);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}
</style>
