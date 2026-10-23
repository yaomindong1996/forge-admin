<template>
  <div class="field-select-slots-panel">
    <n-form-item label="菜单头部">
      <n-input
        :value="propsMap.dropdownHeader || ''"
        clearable
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 3 }"
        placeholder="下拉菜单顶部说明（#header）"
        @update:value="patch({ dropdownHeader: $event || undefined })"
      />
    </n-form-item>
    <n-form-item label="菜单操作区">
      <n-input
        :value="propsMap.dropdownAction || ''"
        clearable
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 3 }"
        placeholder="菜单底部操作说明（#action）"
        @update:value="patch({ dropdownAction: $event || undefined })"
      />
    </n-form-item>
    <n-form-item label="空数据文案">
      <n-input
        :value="propsMap.emptyText || ''"
        clearable
        placeholder="无选项时展示（#empty）"
        @update:value="patch({ emptyText: $event || undefined })"
      />
    </n-form-item>
    <n-form-item label="下拉箭头图标">
      <IconSelector
        :model-value="propsMap.arrowIcon || ''"
        @update:model-value="patch({ arrowIcon: $event || undefined })"
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

function patch(next) {
  emit('update', next)
}
</script>

<style scoped>
.field-select-slots-panel {
  display: contents;
}

.field-select-slots-panel > :deep(.n-form-item .icon-selector),
.field-select-slots-panel > :deep(.n-form-item .n-button) {
  width: 100%;
}
</style>
