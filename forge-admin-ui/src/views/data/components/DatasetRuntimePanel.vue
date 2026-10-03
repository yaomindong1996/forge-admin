<template>
  <!-- 独立配置面板，共享状态由 Pinia 提供 -->
  <section class="dataset-edit-panel dataset-edit-panel--settings" data-step-section="3">
    <div class="panel-section-head">
      <h3>执行设置</h3>
    </div>
    <div class="execution-settings">
      <div class="setting-row">
        <div class="setting-row__head">
          <span>最大返回行数</span>
          <strong>{{ formData.maxRows || 10000 }}</strong>
        </div>
        <div class="setting-row__control">
          <n-slider
            :value="formData.maxRows || 10000"
            :disabled="isFormReadOnly"
            :min="100"
            :max="1000000"
            :step="100"
            @update:value="value => updateDatasetFormField(formData, 'maxRows', value, updateValue)"
          />
          <n-input-number
            :value="formData.maxRows"
            :disabled="isFormReadOnly"
            :min="100"
            :max="1000000"
            :step="100"
            @update:value="value => updateDatasetFormField(formData, 'maxRows', value, updateValue)"
          />
        </div>
      </div>
      <div class="setting-row">
        <div class="setting-row__head">
          <span>查询超时时间</span>
          <strong>{{ formData.timeoutSeconds || 60 }} 秒</strong>
        </div>
        <div class="setting-row__control">
          <n-slider
            :value="formData.timeoutSeconds || 60"
            :disabled="isFormReadOnly"
            :min="1"
            :max="1800"
            :step="1"
            @update:value="value => updateDatasetFormField(formData, 'timeoutSeconds', value, updateValue)"
          />
          <n-input-number
            :value="formData.timeoutSeconds"
            :disabled="isFormReadOnly"
            :min="1"
            :max="1800"
            @update:value="value => updateDatasetFormField(formData, 'timeoutSeconds', value, updateValue)"
          />
        </div>
      </div>
      <label class="dataset-field dataset-field--full">
        <span>缓存策略</span>
        <n-radio-group
          :value="formData.cacheEnabled === 1 ? 1 : 0"
          :disabled="isFormReadOnly"
          @update:value="value => handleCacheStrategyChange(value, formData, updateValue)"
        >
          <n-radio-button :value="0">
            不缓存
          </n-radio-button>
          <n-radio-button :value="1">
            按时间缓存
          </n-radio-button>

        </n-radio-group>
      </label>
      <label v-if="formData.cacheEnabled === 1" class="dataset-field">
        <span>缓存时长(秒)</span>
        <n-input-number
          :value="formData.cacheTtlSeconds"
          :disabled="isFormReadOnly"
          :min="1"
          :max="86400"
          @update:value="value => updateDatasetFormField(formData, 'cacheTtlSeconds', value, updateValue)"
        />
      </label>
      <label class="dataset-field">
        <span>结果集编码</span>
        <NSelect
          :value="formData.__resultEncoding || 'UTF-8'"
          :options="resultEncodingOptions"
          :disabled="isFormReadOnly"
          @update:value="value => updateDatasetFormField(formData, '__resultEncoding', value, updateValue)"
        />
      </label>
      <label class="setting-switch-row">
        <span>允许导出</span>
        <n-switch
          :value="formData.__allowExport ?? true"
          :disabled="isFormReadOnly"
          @update:value="value => updateDatasetFormField(formData, '__allowExport', value, updateValue)"
        />
      </label>
    </div>
  </section>
</template>

<script>
import { useDatasetWorkspaceContext } from '@/stores/data/datasetWorkspaceStore'
import { datasetLocalComponents } from '../datasetLocalComponents'

export default {
  components: { ...datasetLocalComponents },
  props: { formData: { type: Object, required: true }, updateValue: { type: Function, required: true } },
  setup() { return useDatasetWorkspaceContext() },
}
</script>

<style scoped src="../dataset-editor.css"></style>
