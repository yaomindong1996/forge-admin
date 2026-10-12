<template>
  <section class="image-job-result" aria-live="polite">
    <!-- 任务提交不代表当前账号有结果查询权限 -->
    <NAlert v-if="!canQuery" type="info" :show-icon="false">
      图片任务已提交；当前账号没有图片结果查询权限，请联系管理员。
    </NAlert>
    <NAlert v-else-if="subscribeError" type="warning" :show-icon="false">
      {{ subscribeError }}
    </NAlert>
    <template v-else-if="job">
      <!-- 查询状态与任务业务状态分离，暂停不伪装生成失败 -->
      <div class="image-job-heading">
        <span>图片任务</span>
        <DictTag dict-type="ai_image_generate_status" :value="job.result.status" :bordered="false" />
      </div>
      <NAlert v-if="job.paused" type="warning" :show-icon="false">
        {{ job.error }}
        <NButton size="small" secondary class="query-button" @click="resume">
          重新查询
        </NButton>
      </NAlert>
      <div v-else-if="!terminal" class="image-job-pending">
        <NSpin size="small" />
        <span>图片任务已提交，正在等待结果…</span>
      </div>
      <AuthImage
        v-if="job.result.status === 'success'"
        :src="job.result.resultFileId"
        alt="AI 生成图片"
        preview
        :img-style="{ maxWidth: '100%', maxHeight: '400px', objectFit: 'contain' }"
      />
      <NAlert v-else-if="job.result.status === 'failed'" type="error" :show-icon="false">
        {{ job.result.errorMsg }}
      </NAlert>
    </template>
  </section>
</template>

<script setup>
import { NAlert, NButton, NSpin } from 'naive-ui'
import { computed, watch } from 'vue'
import AuthImage from '@/components/common/AuthImage.vue'
import DictTag from '@/components/DictTag.vue'
import { useImageJob } from '@/composables/ai/useImageJob'
import { isImageJobTerminal } from '@/stores/ai/image-job-utils'

const props = defineProps({ recordId: { type: [String, Number], required: true } })
const emit = defineEmits(['settled'])
const { job, canQuery, subscribeError, resume } = useImageJob(() => props.recordId)
const terminal = computed(() => isImageJobTerminal(job.value?.result))
watch(() => terminal.value && job.value?.result, (result) => {
  if (result) {
    emit('settled', result)
  }
}, { immediate: true })
</script>

<style scoped>
.image-job-result {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  min-width: 0;
  gap: 12px;
}
.image-job-heading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
}
.image-job-pending {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-tertiary);
}
.query-button {
  margin-left: 12px;
}
</style>
