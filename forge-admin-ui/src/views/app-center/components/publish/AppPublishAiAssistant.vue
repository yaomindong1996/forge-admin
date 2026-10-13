<template>
  <section class="publish-section-card">
    <header>
      <div><h2>应用 AI 助理</h2><p>复用 Forge 已有智能体，并限定可访问的已发布页面与操作类型。</p></div>
      <n-tag :type="statusTag.type" :bordered="false">
        {{ statusTag.label }}
      </n-tag>
    </header>
    <n-alert v-if="!application.lastPublishVersion" type="warning" :bordered="false" class="publish-card-alert">
      可以先完成绑定，但 AI 助理只有在应用发布且启用后才能使用。
    </n-alert>
    <n-form label-placement="top">
      <n-form-item label="启用应用助理">
        <n-switch v-model:value="form.enabled" />
      </n-form-item>
      <n-grid :cols="2" :x-gap="16" responsive="screen">
        <n-form-item-gi label="绑定已有智能体" required>
          <n-select
            v-model:value="form.agentId"
            :options="agentOptions"
            :loading="loadingAgents"
            :disabled="!form.enabled"
            filterable
            clearable
            placeholder="选择已启用智能体"
            @update:value="syncAgentCode"
          />
        </n-form-item-gi>
        <n-form-item-gi label="助理能力">
          <n-checkbox-group v-model:value="form.capabilities" :disabled="!form.enabled">
            <n-space>
              <n-checkbox value="query">
                查询数据
              </n-checkbox>
              <n-checkbox value="form">
                填写表单
              </n-checkbox>
              <n-checkbox value="analysis">
                分析统计
              </n-checkbox>
            </n-space>
          </n-checkbox-group>
        </n-form-item-gi>
      </n-grid>
      <n-form-item label="可访问页面">
        <n-select
          v-model:value="form.pageIds"
          multiple
          filterable
          :disabled="!form.enabled"
          :options="pageOptions"
          placeholder="选择助理可以访问的页面"
        />
      </n-form-item>
      <n-form-item label="应用指令">
        <n-input
          v-model:value="form.instructions"
          type="textarea"
          :disabled="!form.enabled"
          :autosize="{ minRows: 3, maxRows: 6 }"
          maxlength="1000"
          show-count
          placeholder="说明助理在当前应用中的职责与边界"
        />
      </n-form-item>
      <n-space justify="end">
        <n-button secondary @click="router.push('/ai/agent')">
          <template #icon>
            <n-icon><SparklesOutline /></n-icon>
          </template>
          管理智能体
        </n-button>
        <n-button type="primary" :loading="saving" @click="save">
          <template #icon>
            <n-icon><SaveOutline /></n-icon>
          </template>
          保存 AI 配置
        </n-button>
      </n-space>
    </n-form>
  </section>
</template>

<script setup>
import { SaveOutline, SparklesOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { agentList } from '@/api/ai'
import {
  businessApplicationAiAssistantStatus,
  saveBusinessApplicationAiAssistantConfig,
} from '@/api/business-application'
import { parseJsonObject } from '../portal/portal-config'

const props = defineProps({
  application: { type: Object, required: true },
  pages: { type: Array, default: () => [] },
})
const emit = defineEmits(['changed'])
const message = useMessage()
const router = useRouter()
const agents = ref([])
const loadingAgents = ref(false)
const saving = ref(false)
const remoteStatus = ref(null)
const form = reactive(defaultForm())

const agentOptions = computed(() => agents.value.map(agent => ({
  label: `${agent.agentName || agent.agentCode} · ${agent.agentCode}`,
  value: String(agent.id),
})))
const pageOptions = computed(() => props.pages.filter(page => page.type === 'page').map(page => ({
  label: page.title || page.id,
  value: String(page.id),
})))
const statusTag = computed(() => {
  if (remoteStatus.value?.available)
    return { label: '已启用', type: 'success' }
  if (form.enabled && form.agentId)
    return { label: '待发布', type: 'warning' }
  return { label: '未绑定', type: 'default' }
})

watch(() => props.application.aiAssistantConfig, hydrate, { immediate: true })
onMounted(() => Promise.all([loadAgents(), loadStatus()]))

function hydrate() {
  Object.assign(form, defaultForm(), parseJsonObject(props.application.aiAssistantConfig))
  form.agentId = form.agentId == null ? null : String(form.agentId)
  form.pageIds = (form.pageIds || []).map(String)
}

async function loadAgents() {
  loadingAgents.value = true
  try {
    const response = await agentList()
    agents.value = response.data || []
  }
  finally {
    loadingAgents.value = false
  }
}

async function loadStatus() {
  const response = await businessApplicationAiAssistantStatus(props.application.id)
  remoteStatus.value = response.data || null
}

function syncAgentCode(agentId) {
  const agent = agents.value.find(item => String(item.id) === String(agentId || ''))
  form.agentCode = agent?.agentCode || ''
}

async function save() {
  if (form.enabled && !form.agentId) {
    message.error('请选择要绑定的智能体')
    return
  }
  saving.value = true
  try {
    await saveBusinessApplicationAiAssistantConfig(props.application.id, { ...form })
    await loadStatus()
    emit('changed')
    message.success('AI 助理配置已保存，重新发布后生效')
  }
  catch (error) {
    message.error(error?.message || '保存 AI 助理配置失败')
  }
  finally {
    saving.value = false
  }
}

function defaultForm() {
  return {
    enabled: false,
    agentId: null,
    agentCode: '',
    pageIds: [],
    capabilities: ['query'],
    instructions: '',
  }
}
</script>
