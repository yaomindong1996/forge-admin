<template>
  <n-modal
    :show="show"
    preset="card"
    title="导入应用"
    :style="{ width: 'min(520px, 94vw)' }"
    :mask-closable="!importing"
    :closable="!importing"
    class="app-import-dialog"
    @update:show="emit('update:show', $event)"
  >
    <!-- 说明：导入进当前系统数据库，不是写到本地工程目录 -->
    <div v-if="!result" class="import-intro">
      <p class="import-intro__lead">
        把别处导出的应用配置文件，装进<strong>当前系统</strong>，生成一个新应用。
      </p>
      <ul class="import-intro__list">
        <li>会出现在「我的应用」列表里，可直接打开编辑</li>
        <li>包含页面、表单、流程、打印等设计内容</li>
        <li>不会写入你电脑上的本地项目源码</li>
        <li>不会带入原来的业务数据，只带结构和设计</li>
      </ul>
    </div>

    <div v-if="!result" class="bundle-upload">
      <n-upload
        :default-upload="false"
        :max="1"
        accept=".json,.forge-app.json,application/json"
        :disabled="importing"
        @change="handleFileChange"
      >
        <n-upload-dragger class="import-dragger">
          <div class="upload-copy">
            <i class="i-lucide:file-json upload-copy__icon" aria-hidden="true" />
            <strong>{{ fileName || '选择或拖入配置文件' }}</strong>
            <small>支持 <code>.forge-app.json</code> 或同格式 JSON</small>
          </div>
        </n-upload-dragger>
      </n-upload>
      <n-checkbox v-model:checked="autoPublish" :disabled="importing" class="publish-option">
        导入后尝试发布（失败也不影响应用已导入）
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
      title="导入完成，但有提醒"
    >
      <ul class="warning-list">
        <li v-for="(warning, index) in result.warnings" :key="index">
          {{ warning }}
        </li>
      </ul>
    </n-alert>

    <div v-if="result" class="import-done">
      <p>
        应用已创建：
        <strong>{{ result.applicationName || result.applicationCode }}</strong>
      </p>
      <p class="import-done__hint">编码 {{ result.applicationCode }}（已自动加后缀，避免和现有应用冲突）</p>
    </div>

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
  { key: 'parse', label: '读取配置文件', status: 'PENDING', message: '' },
  { key: 'application', label: '创建应用', status: 'PENDING', message: '' },
  { key: 'objects', label: '创建业务对象', status: 'PENDING', message: '' },
  { key: 'design', label: '还原表单设计', status: 'PENDING', message: '' },
  { key: 'ddl', label: '创建数据表', status: 'PENDING', message: '' },
  { key: 'builder', label: '还原页面导航', status: 'PENDING', message: '' },
  { key: 'companions', label: '还原流程与打印等', status: 'PENDING', message: '' },
  { key: 'publish', label: '尝试发布', status: 'PENDING', message: '' },
  { key: 'done', label: '完成', status: 'PENDING', message: '' },
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
.import-intro {
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px solid #dbe5f5;
  border-radius: 8px;
  background: #f8fbff;
}

.import-intro__lead {
  margin: 0 0 10px;
  color: #0f172a;
  font-size: 13px;
  line-height: 1.65;
}

.import-intro__list {
  margin: 0;
  padding-left: 18px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.8;
}

.bundle-upload {
  margin-bottom: 4px;
}

.import-dragger {
  padding: 8px;
}

.upload-copy {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 18px 12px;
  text-align: center;
}

.upload-copy__icon {
  font-size: 28px;
  color: #2563eb;
}

.upload-copy strong {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}

.upload-copy small {
  color: #94a3b8;
  font-size: 12px;
}

.upload-copy code {
  padding: 0 4px;
  border-radius: 3px;
  background: #f1f5f9;
  font-size: 11px;
}

.publish-option {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

.publish-option :deep(.n-checkbox) {
  display: inline-flex;
  align-items: center;
}

.publish-option :deep(.n-checkbox__label) {
  display: inline;
  line-height: 1.4;
  white-space: normal;
}

.bundle-alert {
  margin-top: 12px;
}

.warning-list {
  margin: 0;
  padding-left: 18px;
}

.bundle-steps {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
}

.bundle-step {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8fafc;
}

.step-dot {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 50%;
  background: #cbd5e1;
  flex-shrink: 0;
}

.bundle-step.is-running .step-dot {
  background: #2563eb;
  box-shadow: 0 0 0 4px rgb(37 99 235 / 18%);
}

.bundle-step.is-success .step-dot {
  background: #16a34a;
}

.bundle-step.is-failed .step-dot {
  background: #dc2626;
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
  color: #94a3b8;
  word-break: break-all;
}

.import-done {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f0fdf4;
  color: #166534;
  font-size: 13px;
  line-height: 1.6;
}

.import-done p {
  margin: 0;
}

.import-done__hint {
  margin-top: 4px !important;
  color: #64748b;
  font-size: 12px;
}
</style>
