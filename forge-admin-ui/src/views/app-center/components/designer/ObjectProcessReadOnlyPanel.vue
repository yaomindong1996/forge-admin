<template>
  <section class="process-summary" aria-labelledby="object-process-summary-title">
    <div class="process-summary-head">
      <div class="process-summary-icon">
        <n-icon><GitBranchOutline /></n-icon>
      </div>
      <div class="process-summary-title">
        <h3 id="object-process-summary-title">流程信息</h3>
        <p>这里只展示当前对象参与的业务流程，编排与发布统一在应用工作台完成。</p>
      </div>
      <n-button text type="primary" @click="openWorkspace">
        去应用工作台配置
        <template #icon>
          <n-icon><ArrowForwardOutline /></n-icon>
        </template>
      </n-button>
    </div>

    <n-spin :show="loading">
      <div v-if="processes.length" class="process-list">
        <div v-for="process in processes" :key="process.id || process.processCode" class="process-row">
          <div class="process-name">
            <strong>{{ process.processName || process.processCode || '未命名流程' }}</strong>
            <code v-if="process.processCode">{{ process.processCode }}</code>
          </div>
          <div class="process-meta">
            <DictTag dict-type="ai_business_process_design_status" :value="process.designStatus || process.status" />
            <DictTag
              v-if="process.startNodeType"
              dict-type="ai_business_process_trigger_type"
              :value="normalizeStartNodeType(process.startNodeType)"
            />
          </div>
        </div>
      </div>
      <n-empty
        v-else
        size="small"
        :description="loadFailed ? '暂时无法读取流程列表，请在应用工作台查看' : '当前对象尚未参与业务流程'"
      />
    </n-spin>

    <p class="process-summary-footnote">
      旧触发器、流程绑定和业务动作仍按原配置运行；新配置请使用业务流程画布。
    </p>
  </section>
</template>

<script setup>
import { ArrowForwardOutline, GitBranchOutline } from '@vicons/ionicons5'
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { businessObjectProcesses } from '@/api/business-process'
import DictTag from '@/components/DictTag.vue'

const props = defineProps({
  objectCode: {
    type: String,
    default: '',
  },
  applicationCode: {
    type: String,
    default: '',
  },
})

const router = useRouter()
const loading = ref(false)
const loadFailed = ref(false)
const processes = ref([])

watch(() => props.objectCode, loadProcesses, { immediate: true })

async function loadProcesses() {
  const objectCode = String(props.objectCode || '').trim()
  if (!objectCode) {
    processes.value = []
    return
  }
  loading.value = true
  loadFailed.value = false
  try {
    const response = await businessObjectProcesses(props.objectCode)
    processes.value = Array.isArray(response.data) ? response.data : []
  }
  catch {
    processes.value = []
    loadFailed.value = true
  }
  finally {
    loading.value = false
  }
}

function normalizeStartNodeType(value) {
  return String(value || '').replace(/^START_/, '')
}

function openWorkspace() {
  const applicationCode = String(props.applicationCode || '').trim()
  if (!applicationCode) {
    router.push('/app-center')
    return
  }
  router.push({
    name: 'BusinessApplicationWorkspace',
    params: { applicationCode },
    query: { section: 'automation' },
  })
}
</script>

<style scoped>
.process-summary {
  margin-top: 18px;
  border: 1px solid #dfe5ef;
  border-radius: 10px;
  background: var(--bg-primary);
  padding: 16px;
}

.process-summary-head {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.process-summary-icon {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 9px;
  background: #eef4ff;
  color: #245bdb;
  font-size: 18px;
}

.process-summary-title h3,
.process-summary-title p {
  margin: 0;
}

.process-summary-title h3 {
  color: var(--text-primary);
  font-size: 14px;
}

.process-summary-title p {
  margin-top: 3px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.process-list {
  overflow: hidden;
  border: 1px solid #edf0f5;
  border-radius: 8px;
}

.process-row {
  display: flex;
  min-height: 52px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 8px 12px;
}

.process-row + .process-row {
  border-top: 1px solid #edf0f5;
}

.process-name strong,
.process-name code {
  display: block;
}

.process-name strong {
  color: var(--text-primary);
  font-size: 13px;
}

.process-name code {
  margin-top: 2px;
  color: #8f959e;
  font-size: 11px;
}

.process-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.process-summary-footnote {
  margin: 12px 0 0;
  color: #8f959e;
  font-size: 11px;
}

@media (max-width: 720px) {
  .process-summary-head {
    grid-template-columns: 36px minmax(0, 1fr);
  }

  .process-summary-head > .n-button {
    grid-column: 2;
    justify-self: start;
  }

  .process-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
