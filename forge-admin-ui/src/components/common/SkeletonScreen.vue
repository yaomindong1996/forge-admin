<template>
  <div class="skeleton-wrapper">
    <!-- 列表骨架屏 -->
    <div v-if="type === 'list'" class="skeleton-list">
      <div
        v-for="i in rows"
        :key="i"
        class="skeleton-list-item"
      >
        <div v-if="showAvatar" class="skeleton-avatar" />
        <div class="skeleton-content">
          <div class="skeleton-title" />
          <div v-if="showDescription" class="skeleton-description" />
        </div>
      </div>
    </div>

    <!-- 卡片骨架屏 -->
    <div v-else-if="type === 'card'" class="skeleton-card">
      <div v-if="showHeader" class="skeleton-header">
        <div class="skeleton-title-variant" />
        <div class="skeleton-actions" />
      </div>
      <div class="skeleton-body">
        <div class="skeleton-section">
          <div class="skeleton-line" />
          <div class="skeleton-line short" />
        </div>
        <div v-if="showImage" class="skeleton-image" />
        <div class="skeleton-section">
          <div class="skeleton-line" />
          <div class="skeleton-line" />
          <div class="skeleton-line short" />
        </div>
      </div>
    </div>

    <!-- 表格骨架屏 -->
    <div v-else-if="type === 'table'" class="skeleton-table">
      <div class="skeleton-thead">
        <div
          v-for="i in columns"
          :key="i"
          class="skeleton-th"
          :style="{ width: i === columns ? 'auto' : `${100 / columns}%` }"
        />
      </div>
      <div
        v-for="row in rows"
        :key="row"
        class="skeleton-tbody-row"
      >
        <div
          v-for="col in columns"
          :key="col"
          class="skeleton-td"
        />
      </div>
    </div>

    <!-- 表单骨架屏 -->
    <div v-else-if="type === 'form'" class="skeleton-form">
      <div
        v-for="i in fields"
        :key="i"
        class="skeleton-form-item"
      >
        <div class="skeleton-label" />
        <div class="skeleton-input" />
      </div>
    </div>

    <!-- 图表骨架屏 -->
    <div v-else-if="type === 'chart'" class="skeleton-chart">
      <div class="skeleton-chart-header">
        <div class="skeleton-title-variant" />
      </div>
      <div class="skeleton-chart-body">
        <div class="skeleton-chart-bars">
          <div
            v-for="i in 8"
            :key="i"
            class="skeleton-bar"
            :style="{ height: `${Math.random() * 60 + 20}%` }"
          />
        </div>
      </div>
    </div>

    <!-- 仪表盘骨架屏 -->
    <div v-else-if="type === 'dashboard'" class="skeleton-dashboard">
      <div class="skeleton-stats-grid">
        <div
          v-for="i in 4"
          :key="i"
          class="skeleton-stat"
        >
          <div class="skeleton-stat-icon" />
          <div class="skeleton-stat-value" />
          <div class="skeleton-stat-label" />
        </div>
      </div>
      <div class="skeleton-charts">
        <div class="skeleton-chart large" />
        <div class="skeleton-chart large" />
      </div>
    </div>

    <!-- 简单骨架屏（默认） -->
    <div v-else class="skeleton-simple">
      <div class="skeleton-line" />
      <div class="skeleton-line" />
      <div class="skeleton-line short" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  // 骨架屏类型
  type: {
    type: String,
    default: 'simple',
    validator: value => [
      'simple',
      'list',
      'card',
      'table',
      'form',
      'chart',
      'dashboard',
    ].includes(value),
  },
  // 列表/表格的行数
  rows: {
    type: Number,
    default: 5,
  },
  // 表格的列数
  columns: {
    type: Number,
    default: 5,
  },
  // 表单字段数
  fields: {
    type: Number,
    default: 4,
  },
  // 是否显示头像
  showAvatar: {
    type: Boolean,
    default: false,
  },
  // 是否显示描述
  showDescription: {
    type: Boolean,
    default: true,
  },
  // 是否显示头部
  showHeader: {
    type: Boolean,
    default: true,
  },
  // 是否显示图片
  showImage: {
    type: Boolean,
    default: false,
  },
})
</script>

<style scoped>
.skeleton-wrapper {
  width: 100%;
  height: 100%;
}

/* 基础骨架屏效果 */
@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

.skeleton-wrapper > div {
  animation: shimmer 1.5s infinite;
}

/* 简单骨架屏 */
.skeleton-simple {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
}

.skeleton-line {
  height: 16px;
  background: linear-gradient(90deg, var(--gray-200) 25%, var(--gray-100) 50%, var(--gray-200) 75%);
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

.skeleton-line.short {
  width: 60%;
}

/* 列表骨架屏 */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
}

.skeleton-list-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
}

.skeleton-avatar {
  width: 48px;
  height: 48px;
  min-width: 48px;
  border-radius: 50%;
  background: linear-gradient(90deg, var(--gray-200) 25%, var(--gray-100) 50%, var(--gray-200) 75%);
  background-size: 200% 100%;
}

.skeleton-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.skeleton-title {
  height: 18px;
  width: 40%;
  background: linear-gradient(90deg, var(--gray-200) 25%, var(--gray-100) 50%, var(--gray-200) 75%);
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

.skeleton-description {
  height: 14px;
  width: 70%;
  background: linear-gradient(90deg, var(--gray-200) 25%, var(--gray-100) 50%, var(--gray-200) 75%);
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

/* 卡片骨架屏 */
.skeleton-card {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-default);
  overflow: hidden;
}

.skeleton-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-5) var(--space-6);
  border-bottom: 1px solid var(--border-light);
}

.skeleton-title-variant {
  height: 20px;
  width: 30%;
  background: linear-gradient(90deg, var(--gray-200) 25%, var(--gray-100) 50%, var(--gray-200) 75%);
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

.skeleton-actions {
  width: 80px;
  height: 32px;
  background: linear-gradient(90deg, var(--gray-200) 25%, var(--gray-100) 50%, var(--gray-200) 75%);
  background-size: 200% 100%;
  border-radius: var(--radius-md);
}

.skeleton-body {
  padding: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.skeleton-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.skeleton-image {
  height: 200px;
  background: linear-gradient(90deg, var(--gray-200) 25%, var(--gray-100) 50%, var(--gray-200) 75%);
  background-size: 200% 100%;
  border-radius: var(--radius-lg);
}

/* 表格骨架屏 */
.skeleton-table {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-default);
  overflow: hidden;
}

.skeleton-thead {
  display: flex;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-light);
}

.skeleton-th {
  height: 40px;
  background: linear-gradient(90deg, var(--gray-200) 25%, var(--gray-100) 50%, var(--gray-200) 75%);
  background-size: 200% 100%;
}

.skeleton-tbody-row {
  display: flex;
  border-bottom: 1px solid var(--border-light);
}

.skeleton-td {
  height: 48px;
  background: linear-gradient(90deg, var(--gray-200) 25%, var(--gray-100) 50%, var(--gray-200) 75%);
  background-size: 200% 100%;
}

/* 表单骨架屏 */
.skeleton-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  padding: var(--space-6);
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
}

.skeleton-form-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.skeleton-label {
  height: 14px;
  width: 25%;
  background: linear-gradient(90deg, var(--gray-200) 25%, var(--gray-100) 50%, var(--gray-200) 75%);
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

.skeleton-input {
  height: 40px;
  background: linear-gradient(90deg, var(--gray-200) 25%, var(--gray-100) 50%, var(--gray-200) 75%);
  background-size: 200% 100%;
  border-radius: var(--radius-md);
}

/* 图表骨架屏 */
.skeleton-chart {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-default);
  overflow: hidden;
}

.skeleton-chart-header {
  padding: var(--space-5) var(--space-6);
  border-bottom: 1px solid var(--border-light);
}

.skeleton-chart-body {
  padding: var(--space-6);
  height: 300px;
  display: flex;
  align-items: flex-end;
}

.skeleton-chart-bars {
  display: flex;
  gap: var(--space-3);
  width: 100%;
  height: 100%;
  align-items: flex-end;
}

.skeleton-bar {
  flex: 1;
  background: linear-gradient(180deg, var(--primary-400) 0%, var(--primary-200) 100%);
  border-radius: var(--radius-md);
  transition: height var(--transition);
}

/* 仪表盘骨架屏 */
.skeleton-dashboard {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  padding: var(--space-6);
}

.skeleton-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
}

.skeleton-stat {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  align-items: center;
}

.skeleton-stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  background: linear-gradient(90deg, var(--gray-200) 25%, var(--gray-100) 50%, var(--gray-200) 75%);
  background-size: 200% 100%;
}

.skeleton-stat-value {
  width: 60%;
  height: 32px;
  background: linear-gradient(90deg, var(--gray-200) 25%, var(--gray-100) 50%, var(--gray-200) 75%);
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

.skeleton-stat-label {
  width: 40%;
  height: 14px;
  background: linear-gradient(90deg, var(--gray-200) 25%, var(--gray-100) 50%, var(--gray-200) 75%);
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

.skeleton-charts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
}

.skeleton-chart.large {
  height: 300px;
}

/* 暗色模式适配 */
.dark .skeleton-line,
.dark .skeleton-title,
.dark .skeleton-description,
.dark .skeleton-avatar,
.dark .skeleton-input,
.dark .skeleton-th,
.dark .skeleton-td {
  background: linear-gradient(90deg, var(--gray-700) 25%, var(--gray-600) 50%, var(--gray-700) 75%);
}

.dark .skeleton-bar {
  background: linear-gradient(180deg, var(--primary-700) 0%, var(--primary-500) 100%);
}

.dark .skeleton-chart {
  background: var(--gray-800);
  border-color: var(--gray-700);
}

.dark .skeleton-card,
.dark .skeleton-table,
.dark .skeleton-form {
  background: var(--gray-800);
  border-color: var(--gray-700);
}

.dark .skeleton-header {
  border-bottom-color: var(--gray-700);
}

.dark .skeleton-thead {
  background: var(--gray-700);
  border-bottom-color: var(--gray-600);
}

.dark .skeleton-tbody-row {
  border-bottom-color: var(--gray-700);
}

.dark .skeleton-chart-header {
  border-bottom-color: var(--gray-700);
}

/* 响应式 */
@media (max-width: 768px) {
  .skeleton-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .skeleton-charts {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .skeleton-stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
