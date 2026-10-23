<template>
  <!-- 包装 n-date-picker：统一接线 Naive DatePicker Slots；业务绑定由 AiFormItem 透传 -->
  <n-date-picker v-bind="$attrs">
    <template v-if="dateIcon" #date-icon>
      <IconRenderer :icon="dateIcon" :size="16" />
    </template>
    <template v-if="dateFooter" #footer>
      <div class="ai-form-date-picker-footer">
        {{ dateFooter }}
      </div>
    </template>
    <template v-if="prevYearIcon" #prev-year>
      <IconRenderer :icon="prevYearIcon" :size="16" />
    </template>
    <template v-if="prevMonthIcon" #prev-month>
      <IconRenderer :icon="prevMonthIcon" :size="16" />
    </template>
    <template v-if="nextMonthIcon" #next-month>
      <IconRenderer :icon="nextMonthIcon" :size="16" />
    </template>
    <template v-if="nextYearIcon" #next-year>
      <IconRenderer :icon="nextYearIcon" :size="16" />
    </template>
    <template v-if="separator" #separator>
      {{ separator }}
    </template>
  </n-date-picker>
</template>

<script setup>
import { NDatePicker } from 'naive-ui'
import IconRenderer from '@/components/IconRenderer.vue'

defineOptions({
  name: 'AiFormDatePicker',
  inheritAttrs: false,
})

defineProps({
  dateIcon: { type: String, default: '' },
  dateFooter: { type: String, default: '' },
  prevYearIcon: { type: String, default: '' },
  prevMonthIcon: { type: String, default: '' },
  nextMonthIcon: { type: String, default: '' },
  nextYearIcon: { type: String, default: '' },
  /** 范围选择分隔符（#separator） */
  separator: { type: String, default: '' },
})
</script>

<style scoped>
.ai-form-date-picker-footer {
  padding: 4px 8px;
  font-size: 12px;
  color: var(--n-text-color-3, #8b95a8);
  line-height: 1.4;
}
</style>
