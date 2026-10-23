<template>
  <div class="field-date-picker-slots-panel">
    <n-form-item label="日期图标">
      <IconSelector
        :model-value="propsMap.dateIcon || ''"
        @update:model-value="patch({ dateIcon: $event || undefined })"
      />
    </n-form-item>
    <n-form-item label="面板页脚">
      <n-input
        :value="propsMap.dateFooter || ''"
        clearable
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 3 }"
        placeholder="日期面板底部说明（#footer）"
        @update:value="patch({ dateFooter: $event || undefined })"
      />
    </n-form-item>
    <n-form-item label="面板导航图标">
      <div class="nav-icon-grid">
        <div class="icon-field">
          <span class="icon-field-label">上一年</span>
          <IconSelector
            :model-value="propsMap.prevYearIcon || ''"
            @update:model-value="patch({ prevYearIcon: $event || undefined })"
          />
        </div>
        <div class="icon-field">
          <span class="icon-field-label">上一月</span>
          <IconSelector
            :model-value="propsMap.prevMonthIcon || ''"
            @update:model-value="patch({ prevMonthIcon: $event || undefined })"
          />
        </div>
        <div class="icon-field">
          <span class="icon-field-label">下一月</span>
          <IconSelector
            :model-value="propsMap.nextMonthIcon || ''"
            @update:model-value="patch({ nextMonthIcon: $event || undefined })"
          />
        </div>
        <div class="icon-field">
          <span class="icon-field-label">下一年</span>
          <IconSelector
            :model-value="propsMap.nextYearIcon || ''"
            @update:model-value="patch({ nextYearIcon: $event || undefined })"
          />
        </div>
      </div>
    </n-form-item>
    <n-form-item v-if="isRange" label="范围分隔符">
      <n-input
        :value="propsMap.separator || ''"
        clearable
        placeholder="如 至、~（#separator）"
        @update:value="patch({ separator: $event || undefined })"
      />
    </n-form-item>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import IconSelector from '@/components/IconSelector.vue'

const props = defineProps({
  component: { type: Object, required: true },
})

const emit = defineEmits(['update'])

const propsMap = computed(() => props.component?.props || {})
const isRange = computed(() => ['daterange', 'datetimerange'].includes(props.component?.componentKey))

function patch(next) {
  emit('update', next)
}
</script>

<style scoped>
.field-date-picker-slots-panel {
  display: contents;
}

.field-date-picker-slots-panel > :deep(.n-form-item .icon-selector),
.field-date-picker-slots-panel > :deep(.n-form-item .n-button) {
  width: 100%;
}

.nav-icon-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  width: 100%;
}

.icon-field {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.icon-field-label {
  font-size: 12px;
  color: var(--text-tertiary, #8b95a8);
}
</style>
