<template>
  <div class="agent-config-form">
    <div class="config-anchor-col">
      <div class="anchor-list">
        <button
          v-for="item in anchorItems"
          :key="item.href"
          type="button"
          class="anchor-item"
          :class="{ 'anchor-item--active': activeAnchor === item.href }"
          @click="scrollToSection(item.href)"
        >
          <i :class="item.icon" />
          <span>{{ item.title }}</span>
        </button>
      </div>
    </div>

    <div ref="scrollContainerRef" class="config-scroll-col">
      <n-form ref="agentFormRef" :model="agentForm" :rules="rules" label-placement="top">
        <!-- ==================== 基础信息 ==================== -->
        <div id="config-basic" class="config-section">
          <div class="section-heading">
            <i class="ai-icon:info-circle" />
            <span>基础信息</span>
            <small>智能体的名称、编码与描述</small>
          </div>
          <n-grid :cols="2" :x-gap="16">
            <n-form-item-gi label="智能体名称" path="agentName">
              <n-input v-model:value="agentForm.agentName" placeholder="如 合同审查助手" />
            </n-form-item-gi>
            <n-form-item-gi label="智能体编码" path="agentCode">
              <n-input
                v-model:value="agentForm.agentCode"
                placeholder="contract_reviewer"
                :disabled="!!agentForm.id"
              />
            </n-form-item-gi>
            <n-form-item-gi label="描述" path="description" :span="2">
              <n-input
                v-model:value="agentForm.description"
                type="textarea"
                :rows="2"
                placeholder="用于列表卡片展示和会话识别"
              />
            </n-form-item-gi>
          </n-grid>
        </div>

        <!-- ==================== 人设与提示词 ==================== -->
        <div id="config-prompt" class="config-section">
          <div class="section-heading">
            <i class="ai-icon:user-check" />
            <span>人设与提示词</span>
            <small>定义智能体的角色、目标与行为边界</small>
          </div>
          <n-form-item label="系统提示词" path="systemPrompt">
            <n-input
              v-model:value="agentForm.systemPrompt"
              type="textarea"
              :autosize="{ minRows: 8, maxRows: 16 }"
              placeholder="定义智能体角色、目标、边界和输出要求"
            />
          </n-form-item>
          <n-form-item label="开场白">
            <n-input
              v-model:value="openingStatementModel"
              type="textarea"
              :rows="3"
              placeholder="显示在会话中的首条消息"
            />
          </n-form-item>
        </div>

        <!-- ==================== AI 设置 ==================== -->
        <div id="config-model" class="config-section">
          <div class="section-heading">
            <i class="ai-icon:layers" />
            <span>AI 设置</span>
            <small>模型选择与生成参数</small>
          </div>

          <n-form-item label="模型选择方式">
            <n-select
              v-model:value="agentForm.modelSelectionMode"
              :options="modelSelectionModeOptions"
            />
          </n-form-item>

          <template v-if="agentForm.modelSelectionMode === 'POLICY'">
            <n-form-item label="路由策略">
              <n-select
                v-model:value="agentForm.routePolicyId"
                :options="routePolicyOptions"
                filterable
                clearable
                placeholder="选择路由策略"
              />
            </n-form-item>
          </template>

          <template v-else>
            <n-grid :cols="2" :x-gap="16">
              <n-form-item-gi label="供应商">
                <n-select
                  v-model:value="agentForm.providerId"
                  :options="providerOptions"
                  clearable
                  filterable
                  :disabled="agentForm.modelSelectionMode === 'POLICY'"
                  placeholder="选择供应商"
                />
              </n-form-item-gi>
              <n-form-item-gi label="模型">
                <n-select
                  v-model:value="agentForm.modelName"
                  :options="modelOptions"
                  :loading="modelLoading"
                  clearable
                  filterable
                  tag
                  :disabled="agentForm.modelSelectionMode === 'POLICY'"
                  placeholder="选择或输入模型"
                />
              </n-form-item-gi>
            </n-grid>
          </template>

          <n-divider style="margin: 12px 0" />

          <div class="param-panel-title">
            参数配置
          </div>
          <div class="param-config-list">
            <div class="param-config-row">
              <div class="param-meta-cell">
                <div class="param-title-line">
                  <span>温度</span>
                  <n-tooltip trigger="hover">
                    <template #trigger>
                      <span class="param-help">?</span>
                    </template>
                    控制回答发散程度，越高越有创造性。
                  </n-tooltip>
                </div>
                <n-switch v-model:value="modelParamEnabled.temperature" size="small" />
              </div>
              <div class="param-slider-cell">
                <n-slider
                  v-model:value="agentForm.temperature"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :disabled="!modelParamEnabled.temperature"
                />
              </div>
              <n-input-number
                v-model:value="agentForm.temperature"
                :min="0"
                :max="1"
                :step="0.01"
                :precision="2"
                :show-button="false"
                :disabled="!modelParamEnabled.temperature"
                size="small"
                class="param-number"
              />
            </div>

            <div class="param-config-row">
              <div class="param-meta-cell">
                <div class="param-title-line">
                  <span>最大 Token</span>
                  <n-tooltip trigger="hover">
                    <template #trigger>
                      <span class="param-help">?</span>
                    </template>
                    限制单次回复长度，越大可输出内容越长。
                  </n-tooltip>
                </div>
                <n-switch v-model:value="modelParamEnabled.maxTokens" size="small" />
              </div>
              <div class="param-slider-cell">
                <n-slider
                  v-model:value="agentForm.maxTokens"
                  :min="256"
                  :max="32000"
                  :step="256"
                  :disabled="!modelParamEnabled.maxTokens"
                />
              </div>
              <n-input-number
                v-model:value="agentForm.maxTokens"
                :min="256"
                :max="128000"
                :step="256"
                :precision="0"
                :show-button="false"
                :disabled="!modelParamEnabled.maxTokens"
                size="small"
                class="param-number"
              />
            </div>
          </div>
        </div>

        <!-- ==================== 推荐问题 ==================== -->
        <div id="config-questions" class="config-section">
          <div class="section-heading">
            <i class="ai-icon:message-circle" />
            <span>推荐问题</span>
            <small>用户进入会话时可点击的快捷问题</small>
          </div>
          <n-form-item label="推荐问题">
            <n-dynamic-tags v-model:value="agentForm.extraConfig.suggestedQuestions" />
          </n-form-item>
        </div>

        <!-- ==================== 能力与上下文 ==================== -->
        <div id="config-capabilities" class="config-section">
          <div class="section-heading">
            <i class="ai-icon:tool" />
            <span>能力与上下文</span>
            <small>工具、技能与知识上下文的绑定</small>
          </div>

          <div class="config-entry-grid">
            <button type="button" class="config-entry-card" @click="emit('open-tools')">
              <div class="config-entry-icon">
                <i class="ai-icon:tool" />
              </div>
              <div class="config-entry-main">
                <div class="config-entry-title">
                  工具与技能
                </div>
                <div class="config-entry-desc">
                  {{ mcpToolCount }} 个 MCP · {{ questionCount }} 个推荐问题
                </div>
                <div class="config-entry-tags">
                  <NTag
                    v-for="label in mcpToolLabels.slice(0, 3)"
                    :key="label"
                    size="small"
                    round
                  >
                    {{ label }}
                  </NTag>
                  <span v-if="!mcpToolLabels.length" class="config-entry-empty">未配置工具</span>
                </div>
              </div>
              <i class="config-entry-arrow ai-icon:chevron-right" />
            </button>

            <button type="button" class="config-entry-card" @click="emit('open-context')">
              <div class="config-entry-icon context-entry-icon">
                <i class="ai-icon:book-open" />
              </div>
              <div class="config-entry-main">
                <div class="config-entry-title">
                  上下文
                </div>
                <div class="config-entry-desc">
                  {{ contextCount }} 个配置 · {{ enabledContextCount }} 个启用
                </div>
                <div class="config-entry-preview">
                  {{ contextPreview || '导入知识、规则或样例作为上下文' }}
                </div>
              </div>
              <i class="config-entry-arrow ai-icon:chevron-right" />
            </button>
          </div>
        </div>
      </n-form>
    </div>
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

defineOptions({ name: 'AgentConfigForm' })

const props = defineProps({
  agentForm: { type: Object, required: true },
  modelParamEnabled: { type: Object, required: true },
  providerOptions: { type: Array, default: () => [] },
  modelOptions: { type: Array, default: () => [] },
  modelSelectionModeOptions: { type: Array, default: () => [] },
  routePolicyOptions: { type: Array, default: () => [] },
  modelLoading: { type: Boolean, default: false },
  mcpToolLabels: { type: Array, default: () => [] },
  mcpToolCount: { type: Number, default: 0 },
  questionCount: { type: Number, default: 0 },
  contextCount: { type: Number, default: 0 },
  enabledContextCount: { type: Number, default: 0 },
  contextPreview: { type: String, default: '' },
  rules: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['open-tools', 'open-context'])

const agentFormRef = ref(null)
const scrollContainerRef = ref(null)
const activeAnchor = ref('config-basic')
let scrollObserver = null

const anchorItems = [
  { title: '基础信息', href: 'config-basic', icon: 'ai-icon:info-circle' },
  { title: '人设与提示词', href: 'config-prompt', icon: 'ai-icon:user-check' },
  { title: 'AI 设置', href: 'config-model', icon: 'ai-icon:layers' },
  { title: '推荐问题', href: 'config-questions', icon: 'ai-icon:message-circle' },
  { title: '能力与上下文', href: 'config-capabilities', icon: 'ai-icon:tool' },
]

// 开场白双向绑定到 extraConfig.openingStatement
const openingStatementModel = computed({
  get: () => props.agentForm.extraConfig?.openingStatement || '',
  set: (val) => {
    if (props.agentForm.extraConfig) {
      props.agentForm.extraConfig.openingStatement = val
    }
  },
})

function scrollToSection(href) {
  const target = document.getElementById(href)
  const container = scrollContainerRef.value
  if (target && container) {
    const top = target.offsetTop - container.offsetTop - 16
    container.scrollTo({ top: Math.max(0, top), behavior: 'smooth' })
    activeAnchor.value = href
  }
}

function setupScrollObserver() {
  const container = scrollContainerRef.value
  if (!container || !('IntersectionObserver' in window))
    return

  scrollObserver = new IntersectionObserver(
    (entries) => {
      // 取交叉区域里 top 最小的 section
      const visible = entries
        .filter(entry => entry.isIntersecting)
        .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)
      if (visible.length > 0) {
        activeAnchor.value = visible[0].target.id
      }
    },
    { root: container, threshold: 0.03, rootMargin: '-10px 0px 0px 0px' },
  )

  anchorItems.forEach((item) => {
    const el = document.getElementById(item.href)
    if (el)
      scrollObserver.observe(el)
  })
}

onMounted(() => {
  setupScrollObserver()
})

onBeforeUnmount(() => {
  if (scrollObserver)
    scrollObserver.disconnect()
})

defineExpose({
  validate: () => agentFormRef.value?.validate(),
})
</script>

<style scoped>
.agent-config-form {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 16px;
  min-height: 0;
}

.config-anchor-col {
  padding-top: 4px;
  border-right: 1px solid rgba(226, 232, 240, 0.6);
}

.anchor-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  position: sticky;
  top: 8px;
}

.anchor-item {
  display: flex;
  width: 100%;
  padding: 8px 10px;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 6px;
  transition:
    color 0.15s ease,
    background 0.15s ease;
}

.anchor-item:hover {
  color: #0369a1;
  background: rgba(3, 105, 161, 0.06);
}

.anchor-item--active {
  color: #0369a1;
  font-weight: 600;
  background: rgba(3, 105, 161, 0.1);
}

.config-scroll-col {
  min-height: 0;
  max-height: calc(100vh - 220px);
  padding-right: 8px;
  overflow-y: auto;
}

.config-section {
  margin-bottom: 18px;
  padding: 16px;
  background: var(--bg-primary);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
}

.section-heading {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.7);
}

.section-heading i {
  color: #0369a1;
}

.section-heading span {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
}

.section-heading small {
  margin-left: auto;
  color: var(--text-secondary);
  font-size: 11px;
}

.param-panel-title {
  margin-bottom: 10px;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.param-config-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.param-config-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.param-meta-cell {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  min-width: 110px;
}

.param-title-line {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-secondary);
  font-size: 12px;
}

.param-help {
  display: inline-flex;
  width: 14px;
  height: 14px;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  font-size: 10px;
  cursor: help;
  background: var(--bg-tertiary);
  border-radius: 50%;
}

.param-slider-cell {
  min-width: 0;
  flex: 1;
}

.param-number {
  width: 90px;
}

.config-entry-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.config-entry-card {
  display: flex;
  width: 100%;
  padding: 14px;
  align-items: center;
  gap: 12px;
  text-align: left;
  color: inherit;
  cursor: pointer;
  background: var(--bg-secondary);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 8px;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.config-entry-card:hover {
  border-color: rgba(3, 105, 161, 0.4);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.06);
}

.config-entry-icon {
  display: grid;
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  color: #0369a1;
  font-size: 18px;
  place-items: center;
  background: rgba(3, 105, 161, 0.1);
  border-radius: 9px;
}

.context-entry-icon {
  color: #7c3aed;
  background: rgba(124, 58, 237, 0.1);
}

.config-entry-main {
  min-width: 0;
  flex: 1;
}

.config-entry-title {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.config-entry-desc {
  margin-top: 3px;
  color: var(--text-secondary);
  font-size: 11px;
}

.config-entry-tags {
  display: flex;
  gap: 4px;
  margin-top: 6px;
  overflow: hidden;
}

.config-entry-empty {
  color: var(--text-tertiary);
  font-size: 11px;
}

.config-entry-preview {
  overflow: hidden;
  margin-top: 6px;
  color: var(--text-tertiary);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.config-entry-arrow {
  flex: 0 0 auto;
  color: var(--text-tertiary);
}

@media (max-width: 1024px) {
  .agent-config-form {
    grid-template-columns: 1fr;
  }
  .config-anchor-col {
    display: none;
  }
  .config-entry-grid {
    grid-template-columns: 1fr;
  }
}
</style>
