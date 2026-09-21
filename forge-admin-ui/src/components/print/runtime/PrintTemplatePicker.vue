<script setup>
import { ArrowBackOutline, RefreshOutline } from '@vicons/ionicons5'
import { NAlert, NButton, NEmpty, NIcon, NSelect } from 'naive-ui'
import { computed, onBeforeUnmount, watch } from 'vue'
import { loadPrintFile } from '@/api/print'
import { usePrintRuntimeStore } from '@/stores/print/printRuntimeStore'
import PrintPreview from './PrintPreview.vue'
import PrintPreviewSkeleton from './PrintPreviewSkeleton.vue'

const props = defineProps({ record: { type: Object, required: true } })
const emit = defineEmits(['back'])
const store = usePrintRuntimeStore()
const options = computed(() => store.options.map(item => ({
  label: `${item.templateName} · v${item.versionNo}`,
  value: item.id,
})))
const selectedTemplateName = computed(() => store.options.find(item => item.id === store.selectedId)?.templateName || '')

watch(() => props.record, value => store.open(value), { immediate: true, deep: true })
onBeforeUnmount(() => store.close())
</script>

<template>
  <section class="runtime-picker">
    <header class="runtime-chrome">
      <button type="button" class="chrome-icon" title="返回" aria-label="返回" @click="emit('back')">
        <NIcon :component="ArrowBackOutline" />
      </button>
      <strong class="chrome-title">单据打印</strong>
      <NSelect
        :value="store.selectedId"
        :options="options"
        placeholder="选择打印模板"
        aria-label="打印模板"
        size="small"
        class="template-select"
        @update:value="store.select"
      />
      <button
        type="button"
        class="chrome-icon"
        title="重新读取已保存数据"
        aria-label="重新读取已保存数据"
        :disabled="store.loading"
        @click="store.open(record)"
      >
        <NIcon :component="RefreshOutline" />
      </button>
      <div class="chrome-spacer" />
      <span v-if="store.prepared && !store.loading" class="chrome-meta">已保存数据</span>
    </header>

    <NAlert v-if="store.error" type="error" :bordered="false" class="runtime-alert">
      {{ store.error }}
    </NAlert>
    <NAlert v-else-if="store.eventError" type="warning" :bordered="false" class="runtime-alert">
      <div class="alert-row">
        <span>{{ store.eventError }}</span>
        <NButton text size="tiny" :loading="store.eventPending" @click="store.event(store.pendingEvent)">
          重试
        </NButton>
      </div>
    </NAlert>

    <PrintPreviewSkeleton v-if="store.loading" />
    <PrintPreview
      v-else-if="store.prepared"
      :key="store.prepared.executionId"
      embedded
      data-label=""
      :template="store.prepared.template"
      :context="store.prepared.context"
      :catalog="store.prepared.catalog.fields"
      :template-version="store.prepared.templateVersionId"
      :template-name="selectedTemplateName"
      :resolve-file="loadPrintFile"
      @execution="store.event"
      @error="store.failed"
    />
    <NEmpty
      v-else-if="!store.error"
      class="runtime-empty"
      :description="store.options.length ? '请选择打印模板' : '当前记录没有可用的已发布打印模板'"
    />
  </section>
</template>

<style scoped>
.runtime-picker {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--bg-primary, #fff);
}
.runtime-chrome {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: none;
  min-height: 40px;
  padding: 4px 8px;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
}
.chrome-title {
  flex: none;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}
.template-select {
  width: min(280px, 42vw);
}
.chrome-spacer {
  flex: 1 1 auto;
  min-width: 8px;
}
.chrome-meta {
  color: var(--text-tertiary, #64748b);
  font-size: 11px;
  white-space: nowrap;
}
.chrome-icon {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 4px;
  color: var(--text-primary, #334155);
  background: transparent;
  cursor: pointer;
  font-size: 15px;
}
.chrome-icon:hover:not(:disabled) {
  color: var(--primary-color);
  border-color: color-mix(in srgb, var(--primary-color) 24%, transparent);
  background: color-mix(in srgb, var(--primary-color) 8%, transparent);
}
.chrome-icon:disabled {
  opacity: 0.32;
  cursor: not-allowed;
}
.runtime-alert {
  flex: none;
  padding: 6px 10px;
}
.alert-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.runtime-empty {
  flex: 1;
  display: grid;
  place-items: center;
}
.runtime-picker :deep(.print-preview),
.runtime-picker :deep(.print-preview-skeleton) {
  flex: 1;
  min-height: 0;
}
@media (max-width: 640px) {
  .chrome-meta {
    display: none;
  }
  .template-select {
    width: min(200px, 48vw);
  }
}
</style>
