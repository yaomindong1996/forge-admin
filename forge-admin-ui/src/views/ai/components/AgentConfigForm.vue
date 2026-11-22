<template>
  <!-- eslint-disable vue/no-mutating-props -->
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
          <i :class="item.icon" aria-hidden="true" />
          <span>{{ item.title }}</span>
        </button>
      </div>
    </div>

    <div ref="scrollContainerRef" class="config-scroll-col">
      <n-form ref="agentFormRef" :model="agentForm" :rules="rules" label-placement="top" size="small">
        <!-- 基础信息 -->
        <div id="config-basic" class="config-section">
          <div class="section-heading">
            <i class="i-lucide:info" aria-hidden="true" />
            <span>基础信息</span>
            <small>智能体的名称、编码与描述</small>
          </div>
          <n-grid :cols="2" :x-gap="14">
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
                placeholder="用于列表卡片展示和助手自动路由识别"
              />
            </n-form-item-gi>
          </n-grid>
        </div>

        <!-- 人设与提示词 -->
        <div id="config-prompt" class="config-section">
          <div class="section-heading">
            <i class="i-lucide:user-check" aria-hidden="true" />
            <span>人设与提示词</span>
            <small>定义智能体的角色、目标与行为边界</small>
          </div>
          <n-form-item label="系统提示词" path="systemPrompt">
            <n-input
              v-model:value="agentForm.systemPrompt"
              type="textarea"
              :autosize="{ minRows: 7, maxRows: 15 }"
              placeholder="定义智能体角色、目标、边界和输出要求"
            />
          </n-form-item>
          <n-form-item label="开场白">
            <n-input
              v-model:value="openingStatementModel"
              type="textarea"
              :rows="2"
              placeholder="显示在会话中的首条消息"
            />
          </n-form-item>
        </div>

        <!-- AI 设置 -->
        <div id="config-model" class="config-section">
          <div class="section-heading">
            <i class="i-lucide:layers" aria-hidden="true" />
            <span>模型与参数</span>
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
            <n-grid :cols="2" :x-gap="14">
              <n-form-item-gi label="供应商">
                <n-select
                  v-model:value="agentForm.providerId"
                  :options="providerOptions"
                  clearable
                  filterable
                  placeholder="留空则使用租户默认供应商"
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
                  placeholder="留空则使用供应商默认模型"
                />
              </n-form-item-gi>
            </n-grid>
          </template>

          <div class="param-panel-title">
            生成参数
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

        <!-- 推荐问题 -->
        <div id="config-questions" class="config-section">
          <div class="section-heading">
            <i class="i-lucide:message-circle" aria-hidden="true" />
            <span>推荐问题</span>
            <small>用户进入会话时可点击的快捷问题</small>
          </div>
          <n-form-item label="推荐问题">
            <n-dynamic-tags v-model:value="agentForm.extraConfig.suggestedQuestions" />
          </n-form-item>
        </div>

        <!-- 能力与上下文 -->
        <div id="config-capabilities" class="config-section">
          <div class="section-heading">
            <i class="i-lucide:wrench" aria-hidden="true" />
            <span>能力与上下文</span>
            <small>MCP 工具、技能资料包、知识库</small>
          </div>

          <div class="config-entry-grid">
            <button type="button" class="config-entry-card" @click="emit('openTools')">
              <div class="config-entry-icon">
                <i class="i-lucide:wrench" aria-hidden="true" />
              </div>
              <div class="config-entry-main">
                <div class="config-entry-title">
                  工具、技能与知识库
                </div>
                <div class="config-entry-desc">
                  {{ mcpToolCount }} 个工具 · {{ questionCount }} 个推荐问题
                </div>
                <div class="config-entry-tags">
                  <span class="config-entry-empty">点击装配 MCP / 技能资料 / 知识库</span>
                </div>
              </div>
              <i class="config-entry-arrow i-lucide:chevron-right" aria-hidden="true" />
            </button>

            <button type="button" class="config-entry-card" @click="emit('openContext')">
              <div class="config-entry-icon">
                <i class="i-lucide:book-open" aria-hidden="true" />
              </div>
              <div class="config-entry-main">
                <div class="config-entry-title">
                  上下文配置
                </div>
                <div class="config-entry-desc">
                  {{ contextCount }} 个配置 · {{ enabledContextCount }} 个启用
                </div>
                <div class="config-entry-preview">
                  {{ contextPreview || '导入业务规则、样例或背景知识' }}
                </div>
              </div>
              <i class="config-entry-arrow i-lucide:chevron-right" aria-hidden="true" />
            </button>
          </div>
        </div>
      </n-form>
    </div>
  </div>
</template>

<script setup>
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
  mcpToolCount: { type: Number, default: 0 },
  questionCount: { type: Number, default: 0 },
  contextCount: { type: Number, default: 0 },
  enabledContextCount: { type: Number, default: 0 },
  contextPreview: { type: String, default: '' },
  rules: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['openTools', 'openContext'])

const agentFormRef = ref(null)
const scrollContainerRef = ref(null)
const activeAnchor = ref('config-basic')
let scrollObserver = null

const anchorItems = [
  { title: '基础信息', href: 'config-basic', icon: 'i-lucide:info' },
  { title: '人设与提示词', href: 'config-prompt', icon: 'i-lucide:user-check' },
  { title: '模型与参数', href: 'config-model', icon: 'i-lucide:layers' },
  { title: '推荐问题', href: 'config-questions', icon: 'i-lucide:message-circle' },
  { title: '能力与上下文', href: 'config-capabilities', icon: 'i-lucide:wrench' },
]

const openingStatementModel = computed({
  get: () => props.agentForm.extraConfig?.openingStatement || '',
  set: (val) => {
    const extra = props.agentForm.extraConfig
    if (extra) {
      extra.openingStatement = val
    }
  },
})

function scrollToSection(href) {
  const target = document.getElementById(href)
  const container = scrollContainerRef.value
  if (target && container) {
    const top = target.offsetTop - container.offsetTop - 12
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
      const visible = entries
        .filter(entry => entry.isIntersecting)
        .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)
      if (visible.length > 0) {
        activeAnchor.value = visible[0].target.id
      }
    },
    { root: container, threshold: 0.05, rootMargin: '-8px 0px 0px 0px' },
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
  grid-template-columns: 132px minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  background: var(--bg-primary);
}

.config-anchor-col {
  padding: 12px 8px;
  border-right: 1px solid var(--border-light);
  overflow-y: auto;
}

.anchor-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.anchor-item {
  display: flex;
  width: 100%;
  padding: 7px 10px;
  align-items: center;
  gap: 7px;
  color: var(--text-secondary);
  font: inherit;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 6px;
  transition:
    color 140ms ease,
    background-color 140ms ease;
}

.anchor-item i {
  flex: none;
  font-size: 14px;
}

.anchor-item:hover {
  color: var(--text-primary);
  background: var(--bg-secondary);
}

.anchor-item--active {
  color: var(--primary-color, #2563eb);
  font-weight: 600;
  background: color-mix(in srgb, var(--primary-color, #2563eb) 8%, var(--bg-primary));
}

.config-scroll-col {
  min-height: 0;
  padding: 12px 20px 24px;
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.config-section {
  padding: 14px 0 8px;
  border-bottom: 1px solid var(--border-light);
}

.config-section:first-child {
  padding-top: 4px;
}

.config-section:last-child {
  border-bottom: 0;
}

.section-heading {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.section-heading i {
  color: var(--primary-color, #2563eb);
  font-size: 14px;
  align-self: center;
}

.section-heading span {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 650;
}

.section-heading small {
  color: var(--text-tertiary);
  font-size: 12px;
}

.param-panel-title {
  margin: 4px 0 10px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.param-config-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}

.param-config-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-secondary);
}

.param-meta-cell {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 112px;
}

.param-title-line {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 500;
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
  border: 1px solid var(--border-light);
  background: var(--bg-primary);
  border-radius: 50%;
}

.param-slider-cell {
  min-width: 0;
  flex: 1;
}

.param-number {
  width: 86px;
}

.config-entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.config-entry-card {
  display: flex;
  width: 100%;
  padding: 12px;
  align-items: center;
  gap: 10px;
  text-align: left;
  color: inherit;
  cursor: pointer;
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: 6px;
  transition:
    border-color 140ms ease,
    background-color 140ms ease;
}

.config-entry-card:hover {
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 42%, var(--border-light));
  background: var(--bg-primary);
}

.config-entry-icon {
  display: grid;
  flex: 0 0 auto;
  width: 32px;
  height: 32px;
  color: var(--primary-color, #2563eb);
  font-size: 16px;
  place-items: center;
  background: color-mix(in srgb, var(--primary-color, #2563eb) 9%, var(--bg-primary));
  border-radius: 6px;
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
  margin-top: 2px;
  color: var(--text-tertiary);
  font-size: 11px;
}

.config-entry-tags {
  display: flex;
  gap: 4px;
  margin-top: 6px;
  flex-wrap: wrap;
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
  font-size: 15px;
}

@media (max-width: 960px) {
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
