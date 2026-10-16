<template>
  <div class="image-generate-page">
    <!-- 图片配置与显式提交 -->
    <NCard size="small" title="图片生成">
      <NAlert v-if="modelError" type="warning" class="form-notice" :show-icon="false">
        {{ modelError }}
      </NAlert>
      <NAlert v-if="!canQuery" type="info" class="form-notice" :show-icon="false">
        当前账号没有图片生成权限，请联系管理员。
      </NAlert>
      <ImageGenerationForm
        v-model:form="form" :model-options="modelOptions" :submitting="submitting" :allowed="canQuery"
        @generate="generate"
      />
    </NCard>
    <!-- 与 Agent 工具卡共用结果与轮询生命周期 -->
    <NCard v-if="recordId" size="small" title="生成结果">
      <ImageJobResult :record-id="recordId" @settled="refreshKey += 1" />
    </NCard>
    <!-- 无历史权限不发送历史接口请求 -->
    <ImageGenerationHistory v-if="canHistory" :refresh-key="refreshKey" @select="recordId = $event" />
  </div>
</template>

<script setup>
import { NAlert, NCard, useMessage } from 'naive-ui'
import ImageJobResult from '../components/image/ImageJobResult.vue'
import ImageGenerationForm from './components/ImageGenerationForm.vue'
import ImageGenerationHistory from './components/ImageGenerationHistory.vue'
import { useImageGeneration } from './useImageGeneration'

const { form, modelOptions, modelError, submitting, recordId, refreshKey, canQuery, canHistory, generate }
  = useImageGeneration(useMessage())
</script>

<style scoped>
.image-generate-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-content: start;
  gap: 12px;
  min-width: 0;
}
.form-notice {
  margin-bottom: 12px;
}
</style>
