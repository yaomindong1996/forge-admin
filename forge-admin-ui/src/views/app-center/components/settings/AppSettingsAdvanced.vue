<template>
  <section class="settings-section-card">
    <header>
      <h2>高级设置</h2>
      <p>这些设置影响代码导出、运行缓存和版本留存策略。</p>
    </header>
    <n-alert type="warning" :bordered="false" class="settings-info-alert">
      修改代码前缀可能影响后续生成物命名，但不会重命名已发布对象或数据库表。
    </n-alert>
    <n-alert type="info" :bordered="false" class="settings-info-alert">
      缓存策略与版本保留数量会随发布快照保存；当前版本已按版本读取正式快照，但尚未接入独立缓存清理任务。
    </n-alert>
    <n-form label-placement="top">
      <n-grid :cols="2" :x-gap="16" responsive="screen">
        <n-form-item-gi label="代码生成前缀">
          <n-input
            :value="advanced.codePrefix"
            maxlength="20"
            placeholder="例如 crm"
            @update:value="patch({ codePrefix: normalizePrefix($event) })"
          />
        </n-form-item-gi>
        <n-form-item-gi label="运行时缓存策略">
          <n-select :value="advanced.cachePolicy" :options="cacheOptions" @update:value="patch({ cachePolicy: $event })" />
        </n-form-item-gi>
        <n-form-item-gi label="版本保留数量">
          <n-input-number
            :value="advanced.versionRetention"
            :min="5"
            :max="100"
            @update:value="patch({ versionRetention: $event || 20 })"
          />
        </n-form-item-gi>
      </n-grid>
    </n-form>

    <!-- 应用配置导出：整应用或按页勾选 -->
    <div class="app-config-export-panel">
      <div class="app-config-export-copy">
        <strong>导出应用配置</strong>
        <p>导出设计态配置（可按页面勾选，并自动带上依赖对象）。不含业务数据，可在应用中心导入。</p>
      </div>
      <n-button
        secondary
        @click="openExportDialog"
      >
        导出应用配置
      </n-button>
    </div>

    <ApplicationDebugBundleExportDialog
      v-model:show="exportDialogVisible"
      :application-id="resolvedApplicationId"
      :application-code="resolvedApplicationCode"
    />
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useMessage } from 'naive-ui'
import ApplicationDebugBundleExportDialog from '../debug-bundle/ApplicationDebugBundleExportDialog.vue'

const props = defineProps({
  modelValue: { type: Object, required: true },
  applicationId: { type: [Number, String], default: null },
  applicationCode: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])
const message = useMessage()
const exportDialogVisible = ref(false)
const advanced = computed(() => props.modelValue.advanced || {})
const resolvedApplicationId = computed(() => props.applicationId ?? props.modelValue?.id ?? null)
const resolvedApplicationCode = computed(() => props.applicationCode || props.modelValue?.applicationCode || '')
const cacheOptions = [
  { label: '按发布版本缓存', value: 'version' },
  { label: '每次读取最新快照', value: 'none' },
  { label: '短时缓存（5 分钟）', value: 'short' },
]

function openExportDialog() {
  if (!resolvedApplicationId.value) {
    message.warning('应用尚未加载完成，请稍后再试')
    return
  }
  exportDialogVisible.value = true
}

function patch(value) {
  emit('update:modelValue', {
    ...props.modelValue,
    advanced: { ...advanced.value, ...value },
  })
}

function normalizePrefix(value) {
  return String(value || '').toLowerCase().replace(/[^a-z0-9_]/g, '')
}
</script>

<style scoped>
.app-config-export-panel {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--n-divider-color);
}

.app-config-export-copy {
  min-width: 0;
}

.app-config-export-copy strong {
  display: block;
  margin-bottom: 4px;
  font-size: 14px;
}

.app-config-export-copy p {
  margin: 0;
  color: var(--n-text-color-3);
  font-size: 13px;
  line-height: 1.5;
}
</style>
