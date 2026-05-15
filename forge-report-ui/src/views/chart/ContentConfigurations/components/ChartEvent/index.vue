<template>
  <!-- 事件配置 -->
  <div class="go-chart-configurations-event">
    <n-collapse class="go-mt-3" arrow-placement="right" :default-expanded-names="['1', '2', '3']">
      <n-text depth="3" class="event-id-hint">
        组件 id：
        <n-text>{{ targetData.id }}</n-text>
      </n-text>
      <chart-event-interaction></chart-event-interaction>
      <chart-event-page-action></chart-event-page-action>
      <chart-event-base-handle></chart-event-base-handle>
      <chart-event-advanced-handle></chart-event-advanced-handle>
    </n-collapse>
  </div>
</template>

<script setup lang="ts">
import { ChartEventInteraction } from './components/ChartEventInteraction'
import { ChartEventAdvancedHandle } from './components/ChartEventAdvancedHandle'
import { ChartEventBaseHandle } from './components/ChartEventBaseHandle'
import { ChartEventPageAction } from './components/ChartEventPageAction'
import { useTargetData } from '../hooks/useTargetData.hook'

const { targetData } = useTargetData()
</script>

<style lang="scss" scoped>
@include go('chart-configurations-event') {
  .event-id-hint {
    display: block;
    padding: 4px 12px;
    margin-bottom: 8px;
    font-size: 11px;
    letter-spacing: 0.4px;
  }

  :deep(.n-collapse-item) {
    border-radius: 10px;
    overflow: hidden;
    margin-bottom: 8px;
    border: 1px solid rgba(var(--app-theme-rgb), 0.08);
    background: rgba(15, 23, 42, 0.16);
  }

  :deep(.n-collapse-item__header) {
    padding: 10px 14px;
    font-size: 13px;
    font-weight: 600;
    color: rgba(226, 232, 240, 0.86);
  }

  :deep(.n-collapse-item__content-inner) {
    padding: 8px 14px 14px;
  }

  :deep(.no-data) {
    padding: 20px 0;
  }

  :deep(.n-card) {
    border-radius: 10px;
    margin-bottom: 10px;
    border: 1px solid rgba(var(--app-theme-rgb), 0.08);
    background: rgba(15, 23, 42, 0.18);
  }

  :deep(.n-divider) {
    margin: 8px 0;
  }

  :deep(.n-select) {
    border-radius: 8px;
  }
}
</style>
