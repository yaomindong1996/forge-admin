<template>
  <n-modal
    :show="show"
    preset="card"
    title="导入应用配置"
    :style="{ width: 'min(520px, 94vw)' }"
    :mask-closable="!importing"
    :closable="!importing"
    @update:show="emit('update:show', $event)"
  >
    <n-alert type="info" :bordered="false" class="bundle-alert">
      将客户环境导出的 <code>.forge-app.json</code> 导入到当前环境。
      会新建完整应用（对象、页面、流程、打印、扩展、触发器、入口等），应用编码自动追加后缀避免冲突；不导入业务行数据。
    </n-alert>

    <div v-if="!result" class="bundle-upload">
      <n-upload
        :default-upload="false"
        :max="1"
        accept=".json,.forge-app.json,application/json"
        :disabled="importing"
        @change="handleFileChange"
      >
        <n-upload-dragger>
          <div class="upload-copy">
            <strong>{{ fileName || '选择或拖入应用配置文件' }}</strong>
            <small>仅支持 forge-application-bundle JSON</small>
          </div>
        </n-upload-dragger>
      </n-upload>
      <n-checkbox v-model:checked="autoPublish" :disabled="importing" class="publish-option">
        导入完成后尝试一键发布（失败不影响设计态导入）
      </n-checkbox>
    </div>

    <div v-if="importing || result" class="bundle-steps">
      <div
        v-for="step in displaySteps"
        :key="step.key"
        class="bundle-step"
        :class="stepStatusClass(step.status)"
      >
        <span class="step-dot" />
        <div class="step-copy">
          <strong>{{ step.label }}</strong>
          <small v-if="step.message">{{ step.message }}</small>
        </div>
      </div>
    </div>

    <n-alert
      v-if="result?.warnings?.length"
      type="warning"
      :bordered="false"
      class="bundle-alert"
      title="导入完成，但有警告"
    >
      <ul>
        <li v-for="(warning, index) in result.warnings" :key="index">
          {{ warning }}
        </li>
      </ul>
    </n-alert>

    <template #footer>
      <n-space justify="end">
        <n-button :disabled="importing" @click="emit('update:show', false)">
          {{ result ? '关闭' : '取消' }}
        </n-button>
        <n-button
          v-if="!result"
          type="primary"
          :loading="importing"
          :disabled="!selectedFile"
          @click="runImport"
        >
          开始导入
        </n-button>
        <n-button
          v-else
          type="primary"
          @click="openImported"
        >
          打开应用
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { importBusinessApplicationDebugBundle } from '@/api/business-application'

const props = defineProps({
  show: { type: Boolean, default: false },
})
const emit = defineEmits(['update:show', 'imported'])

const message = useMessage()
const selectedFile = ref(null)
const fileName = ref('')
const importing = ref(false)
const autoPublish = ref(true)
const result = ref(null)

const pendingSteps = [
  { key: 'parse', label: '解析配置包', status: 'PENDING', message: '' },
  { key: 'application', label: '创建应用', status: 'PENDING', message: '' },
  { key: 'objects', label: '创建业务对象', status: 'PENDING', message: '' },
  { key: 'design', label: '还原字段与表单设计', status: 'PENDING', message: '' },
  { key: 'ddl', label: '同步数据表结构', status: 'PENDING', message: '' },
  { key: 'builder', label: '还原页面与导航', status: 'PENDING', message: '' },
  { key: 'companions', label: '还原流程/打印/扩展等', status: 'PENDING', message: '' },
  { key: 'publish', label: '导入后发布', status: 'PENDING', message: '' },
  { key: 'done', label: '导入完成', status: 'PENDING', message: '' },
]

const liveSteps = ref(pendingSteps.map(step => ({ ...step })))

const displaySteps = computed(() => {
  if (result.value?.steps?.length)
    return result.value.steps
  return liveSteps.value
})

watch(() => props.show, (visible) => {
  if (!visible)
    return
  selectedFile.value = null
  fileName.value = ''
  importing.value = false
  autoPublish.value = true
  result.value = null
  liveSteps.value = pendingSteps.map(step => ({ ...step }))
})

function handleFileChange({ fileList }) {
  const item = fileList?.[0]
  selectedFile.value = item?.file || null
  fileName.value = item?.name || selectedFile.value?.name || ''
}

function stepStatusClass(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'SUCCESS')
    return 'is-success'
  if (value === 'FAILED')
    return 'is-failed'
  if (value === 'RUNNING')
    return 'is-running'
  return 'is-pending'
}

async function runImport() {
  if (!selectedFile.value || importing.value)
    return
  importing.value = true
  result.value = null
  liveSteps.value = pendingSteps.map((step, index) => ({
    ...step,
    status: index === 0 ? 'RUNNING' : 'PENDING',
  }))
  // 长请求期间用时间推进视觉步骤，接口返回后以服务端 steps 为准
  const timer = window.setInterval(() => {
    const next = liveSteps.value.findIndex(step => step.status === 'PENDING')
    const running = liveSteps.value.findIndex(step => step.status === 'RUNNING')
    if (running >= 0 && next > running) {
      liveSteps.value = liveSteps.value.map((step, index) => {
        if (index === running)
          return { ...step, status: 'SUCCESS' }
        if (index === next)
          return { ...step, status: 'RUNNING' }
        return step
      })
    }
  }, 1200)
  try {
    const response = await importBusinessApplicationDebugBundle(selectedFile.value, autoPublish.value)
    result.value = response.data || response
    message.success(`已导入应用 ${result.value.applicationCode || ''}`)
    emit('imported', result.value)
  }
  catch (error) {
    message.error(error?.message || '导入失败')
    liveSteps.value = liveSteps.value.map(step => (
      step.status === 'RUNNING'
        ? { ...step, status: 'FAILED', message: error?.message || '失败' }
        : step
    ))
  }
  finally {
    window.clearInterval(timer)
    importing.value = false
  }
}

function openImported() {
  emit('imported', result.value)
  emit('update:show', false)
}
</script>

<style scoped>
.bundle-alert {
  margin-bottom: 14px;
}

.bundle-upload {
  margin-bottom: 12px;
}

.publish-option {
  display: block;
  margin-top: 12px;
}

.upload-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 0;
}

.upload-copy strong {
  font-size: 14px;
}

.upload-copy small {
  color: var(--n-text-color-3);
}

.bundle-steps {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.bundle-step {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--n-color-embedded);
}

.step-dot {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--n-text-color-disabled);
  flex-shrink: 0;
}

.bundle-step.is-running .step-dot {
  background: var(--n-primary-color);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--n-primary-color) 20%, transparent);
}

.bundle-step.is-success .step-dot {
  background: var(--n-success-color);
}

.bundle-step.is-failed .step-dot {
  background: var(--n-error-color);
}

.step-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.step-copy strong {
  font-size: 13px;
}

.step-copy small {
  color: var(--n-text-color-3);
  word-break: break-all;
}
</style>
