<template>
  <!-- eslint-disable vue/no-unused-refs -->
  <div class="agent-console-page">
    <section v-if="viewMode === 'market'" class="resource-main agent-market">
      <header class="resource-heading">
        <div class="resource-heading__identity">
          <span class="resource-heading__icon" aria-hidden="true">
            <i class="i-lucide:bot" />
          </span>
          <div>
            <h1>智能体管理</h1>
            <p>配置智能体人设、提示词、知识库与 MCP / 技能资料，调试预览后发布使用。</p>
          </div>
        </div>
      </header>

      <div class="resource-query">
        <div class="resource-query__fields">
          <n-input
            v-model:value="filter.keyword"
            clearable
            size="small"
            placeholder="搜索名称、编码或描述"
            class="resource-query__input"
            @keyup.enter="loadAgents"
          >
            <template #prefix>
              <i class="i-lucide:search" aria-hidden="true" />
            </template>
          </n-input>
          <n-select
            v-model:value="filter.status"
            :options="statusFilterOptions"
            size="small"
            class="resource-query__select"
          />
          <NButton size="small" @click="loadAgents">
            查询
          </NButton>
          <NButton size="small" quaternary @click="resetAgentSearch">
            重置
          </NButton>
        </div>
        <div class="resource-query__actions">
          <NButton type="primary" size="small" @click="handleCreateAgent">
            <template #icon>
              <i class="i-lucide:plus" aria-hidden="true" />
            </template>
            创建智能体
          </NButton>
        </div>
      </div>

      <div class="resource-result-meta">
        <span>共 {{ pagination.itemCount }} 个智能体</span>
        <span>打开配置 → 调试预览 → 发布对话</span>
      </div>

      <div class="resource-scroll">
        <n-spin :show="agentLoading" class="resource-list-spin">
          <div v-if="agentList.length" class="resource-grid">
            <article
              v-for="agent in agentList"
              :key="agent.id"
              class="resource-card"
            >
              <header class="resource-card__header">
                <span class="resource-card__icon" aria-hidden="true">
                  <i class="i-lucide:bot" />
                </span>
                <div class="resource-card__identity">
                  <button
                    type="button"
                    class="resource-card__title"
                    @click="handleOpenDesigner(agent)"
                  >
                    {{ agent.agentName }}
                  </button>
                  <code :title="agent.agentCode">{{ agent.agentCode }}</code>
                </div>
                <NTag
                  :type="agent.status === '0' ? 'success' : 'warning'"
                  size="small"
                  :bordered="false"
                >
                  {{ agent.status === '0' ? '已发布' : '草稿' }}
                </NTag>
              </header>

              <div class="resource-card__body">
                <p class="resource-card__description" :title="agent.description">
                  {{ agent.description || '暂无描述' }}
                </p>
                <div class="resource-card__binding">
                  <i class="i-lucide:link-2" aria-hidden="true" />
                  <span>
                    {{
                      agent.modelSelectionMode === 'POLICY'
                        ? '按路由策略选择模型'
                        : agent.modelName || getProviderDefaultModel(agent.providerId) || '使用默认模型'
                    }}
                  </span>
                  <span class="resource-card__secondary">
                    T={{ agent.temperature ?? '0.70' }}
                  </span>
                </div>
              </div>

              <footer class="resource-card__footer">
                <span :title="agent.updateTime || agent.createTime">
                  {{ formatRelativeTime(agent.updateTime || agent.createTime) }}
                </span>
                <div class="resource-card__actions">
                  <NButton text size="small" @click="handleOpenDesigner(agent)">
                    打开配置
                  </NButton>
                  <span class="resource-action-separator" aria-hidden="true" />
                  <NButton text size="small" @click="goToChat(agent)">
                    测试对话
                  </NButton>
                  <NDropdown
                    :options="getAgentCardActions(agent)"
                    trigger="click"
                    @select="key => handleAgentCardAction(key, agent)"
                  >
                    <NButton
                      quaternary
                      circle
                      size="tiny"
                      title="更多操作"
                      aria-label="更多操作"
                    >
                      <i class="i-lucide:more-horizontal" aria-hidden="true" />
                    </NButton>
                  </NDropdown>
                </div>
              </footer>
            </article>
          </div>

          <div v-else-if="!agentLoading" class="resource-empty">
            <div class="resource-empty__icon" aria-hidden="true">
              <i class="i-lucide:bot" />
            </div>
            <h3>{{ filter.keyword || filter.status !== 'all' ? '没有匹配的智能体' : '创建第一个智能体' }}</h3>
            <p>配置角色与提示词，选择模型，按需关联知识库、MCP 工具与技能资料。</p>
            <div class="resource-empty__actions">
              <NButton v-if="filter.keyword || filter.status !== 'all'" size="small" @click="resetAgentSearch">
                清除筛选
              </NButton>
              <NButton v-else type="primary" size="small" @click="handleCreateAgent">
                创建智能体
              </NButton>
            </div>
          </div>
        </n-spin>
      </div>

      <footer v-if="pagination.itemCount > 0" class="resource-pagination">
        <span>共 {{ pagination.itemCount }} 个智能体</span>
        <n-pagination
          v-model:page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="[12, 24, 48]"
          show-size-picker
          size="small"
          @update:page="loadAgents"
          @update:page-size="handleAgentPageSizeChange"
        />
      </footer>
    </section>

    <section v-else class="agent-workbench">
      <header class="workbench-header">
        <div class="workbench-identity">
          <button
            type="button"
            class="agent-back"
            title="返回智能体列表"
            aria-label="返回智能体列表"
            @click="backToMarket"
          >
            <i class="i-lucide:arrow-left" aria-hidden="true" />
            <span>返回列表</span>
          </button>
          <div class="workbench-heading-copy">
            <div class="workbench-heading-title">
              <h1>{{ agentForm.id ? agentForm.agentName || '编辑智能体' : '创建智能体' }}</h1>
              <NTag
                :type="agentForm.status === '0' ? 'success' : 'warning'"
                size="small"
                :bordered="false"
              >
                {{ agentForm.status === '0' ? '已发布' : '草稿' }}
              </NTag>
            </div>
            <p class="workbench-subtitle">
              <code>{{ agentForm.agentCode || '未设置编码' }}</code>
              <span aria-hidden="true">·</span>
              <span>{{ selectedModelLabel }}</span>
            </p>
          </div>
        </div>

        <div class="workbench-actions">
          <NButton quaternary size="small" @click="handleOpenWorkbenchBaseSettings">
            <template #icon>
              <i class="i-lucide:settings" aria-hidden="true" />
            </template>
            基础信息
          </NButton>
          <NButton
            quaternary
            size="small"
            :disabled="!agentForm.id"
            title="在新页面打开独立对话"
            @click="goToChat()"
          >
            <template #icon>
              <i class="i-lucide:messages-square" aria-hidden="true" />
            </template>
            独立对话
          </NButton>
          <NButton
            secondary
            size="small"
            :loading="saveLoading"
            :disabled="publishLoading"
            @click="handleSaveDraft"
          >
            保存
          </NButton>
          <NButton
            type="primary"
            size="small"
            :loading="publishLoading"
            :disabled="saveLoading"
            @click="handlePublishAgent"
          >
            {{ agentForm.status === '0' ? '更新发布' : '发布' }}
          </NButton>
        </div>
      </header>

      <div class="workbench-body">
        <section class="orchestration-panel" aria-label="智能体配置">
          <AgentConfigForm
            ref="agentFormRef"
            :agent-form="agentForm"
            :model-param-enabled="modelParamEnabled"
            :provider-options="providerOptions"
            :model-options="modelOptions"
            :model-selection-mode-options="modelSelectionModeOptions"
            :route-policy-options="routePolicyOptions"
            :model-loading="modelLoading"
            :mcp-tool-labels="selectedMcpToolLabels"
            :mcp-tool-count="selectedMcpToolLabels.length"
            :question-count="suggestedQuestionCount"
            :context-count="contextConfigs.length"
            :enabled-context-count="enabledContextCount"
            :context-preview="contextConfigs[0]?.configName || ''"
            :rules="agentRules"
            @open-tools="toolModalVisible = true"
            @open-context="contextModalVisible = true"
          />
        </section>

        <section class="chat-panel" aria-label="调试与预览">
          <header class="chat-header">
            <div class="chat-header__title">
              <i class="i-lucide:message-square" aria-hidden="true" />
              <h2>调试与预览</h2>
              <span class="chat-header__hint">
                {{ agentForm.status === '0' ? '已发布可试聊' : '发布后可试聊' }}
              </span>
            </div>
            <NButton size="tiny" quaternary :disabled="chatSending" @click="resetConversation">
              <template #icon>
                <i class="i-lucide:rotate-ccw" aria-hidden="true" />
              </template>
              新会话
            </NButton>
          </header>

          <div ref="messageListRef" class="chat-scroll">
            <div class="chat-message-list" @click="handlePreviewContentClick">
              <article
                v-for="message in previewMessages"
                :key="message.id"
                class="chat-message"
                :class="message.role === 'user' ? 'message-user' : 'message-assistant'"
              >
                <div class="message-meta">
                  <i
                    :class="message.role === 'user' ? 'i-lucide:user' : 'i-lucide:bot'"
                    aria-hidden="true"
                  />
                  <span>{{ message.role === 'user' ? '我' : agentForm.agentName || '智能体' }}</span>
                  <time>{{ message.time }}</time>
                </div>

                <details
                  v-if="message.reasoning && message.reasoning.trim()"
                  class="reasoning-section"
                  :open="message.isReasoning"
                >
                  <summary class="reasoning-header">
                    <i class="i-lucide:brain" aria-hidden="true" />
                    <span>思考过程</span>
                    <small v-if="message.reasoningTime">用时 {{ message.reasoningTime }}s</small>
                    <small v-else-if="message.isReasoning">思考中…</small>
                  </summary>
                  <div class="reasoning-content">
                    {{ message.reasoning }}
                  </div>
                </details>

                <div
                  v-if="message.content || (message.streaming && !message.isReasoning)"
                  class="message-bubble"
                >
                  <template v-if="message.role === 'user'">
                    {{ message.content }}
                  </template>
                  <div
                    v-else-if="message.content"
                    class="preview-markdown"
                    v-html="renderPreviewMarkdown(message)"
                  />
                  <span v-if="message.streaming && !message.isReasoning" class="stream-cursor" />
                </div>
              </article>
            </div>
          </div>

          <div v-if="suggestedQuestionList.length && !chatMessages.length" class="suggestion-row">
            <button
              v-for="question in suggestedQuestionList"
              :key="question"
              type="button"
              class="suggestion-chip"
              @click="useSuggestedQuestion(question)"
            >
              {{ question }}
            </button>
          </div>

          <footer class="chat-composer">
            <n-input
              v-model:value="chatInput"
              type="textarea"
              size="small"
              :autosize="{ minRows: 2, maxRows: 5 }"
              placeholder="输入测试问题，Enter 发送（Shift+Enter 换行）"
              :disabled="chatSending"
              @keydown.enter.exact.prevent="sendChatMessage"
            />
            <div class="composer-actions">
              <span class="composer-status">
                {{ chatStatusText }}
              </span>
              <NButton v-if="chatSending" size="small" type="warning" secondary @click="stopChat">
                停止
              </NButton>
              <NButton v-else size="small" type="primary" :disabled="!canSendChat" @click="sendChatMessage">
                <template #icon>
                  <i class="i-lucide:send" aria-hidden="true" />
                </template>
                发送
              </NButton>
            </div>
          </footer>
        </section>
      </div>
    </section>

    <n-modal
      v-model:show="baseModalVisible"
      preset="card"
      :title="baseModalTitle"
      class="agent-base-modal"
      :bordered="false"
    >
      <n-form ref="baseFormRef" :model="baseForm" :rules="baseRules" label-placement="top" size="small">
        <n-form-item label="智能体名称" path="agentName">
          <n-input v-model:value="baseForm.agentName" placeholder="如 合同审查助手" />
        </n-form-item>
        <n-form-item label="智能体编码" path="agentCode">
          <n-input
            v-model:value="baseForm.agentCode"
            :disabled="!!baseForm.id"
            placeholder="contract_reviewer"
          />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input
            v-model:value="baseForm.description"
            type="textarea"
            :rows="3"
            placeholder="用于列表卡片展示和助手自动路由识别"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <div class="modal-footer">
          <NButton size="small" @click="baseModalVisible = false">
            取消
          </NButton>
          <NButton size="small" type="primary" :loading="baseSaveLoading" @click="handleSaveBaseInfo">
            保存
          </NButton>
        </div>
      </template>
    </n-modal>

    <n-modal
      v-model:show="toolModalVisible"
      preset="card"
      title="工具、技能与知识库"
      class="agent-config-modal"
      :bordered="false"
    >
      <n-tabs type="segment" animated size="small">
        <n-tab-pane name="tools" tab="工具绑定">
          <div class="modal-section">
            <div class="tool-section-header">
              <span class="tool-section-title">已绑定工具 ({{ agentTools.length }})</span>
              <n-button size="tiny" type="primary" @click="showAddTool = true">
                添加工具
              </n-button>
            </div>
            <n-empty v-if="!agentTools.length" description="暂无绑定工具" size="small" class="my-4" />
            <div v-else class="tool-list">
              <div v-for="tool in agentTools" :key="tool.id" class="tool-item">
                <div class="tool-info">
                  <n-tag size="tiny" :type="tool.toolSource === 'mcp' ? 'info' : 'default'" :bordered="false">
                    {{ tool.toolSource }}
                  </n-tag>
                  <span class="tool-key">{{ tool.toolKey }}</span>
                  <span v-if="tool.toolGroup" class="tool-group">{{ tool.toolGroup }}</span>
                </div>
                <div class="tool-actions">
                  <n-switch v-model:value="tool.enabled" size="small" @update:value="(val) => updateToolEnabled(tool, val)" />
                  <n-button text type="error" size="tiny" @click="removeTool(tool)">
                    解除
                  </n-button>
                </div>
              </div>
            </div>

            <div v-if="showAddTool" class="add-tool-form">
              <n-divider />
              <n-form inline size="small">
                <n-form-item label="来源">
                  <n-input v-model:value="newTool.source" placeholder="mcp/builtin/capability" size="small" style="width: 120px" />
                </n-form-item>
                <n-form-item label="工具标识">
                  <n-input v-model:value="newTool.key" placeholder="tool_key" size="small" style="width: 180px" />
                </n-form-item>
                <n-form-item label="分组">
                  <n-input v-model:value="newTool.group" placeholder="分组" size="small" style="width: 100px" />
                </n-form-item>
                <n-button size="small" type="primary" :disabled="!newTool.source || !newTool.key" @click="addTool">
                  确认
                </n-button>
                <n-button size="small" @click="showAddTool = false">
                  取消
                </n-button>
              </n-form>
            </div>
          </div>
        </n-tab-pane>

        <n-tab-pane name="mcp" tab="MCP 远程工具">
          <div class="modal-section">
            <AgentResourceBindingPanel
              v-if="agentForm.id"
              :agent-id="agentForm.id"
              mode="mcp"
              @changed="loadAgentTools"
            />
            <n-empty v-else description="请先保存智能体后再绑定 MCP 工具" size="small" />
          </div>
        </n-tab-pane>

        <n-tab-pane name="skill-packages" tab="技能资料包">
          <div class="modal-section">
            <AgentResourceBindingPanel
              v-if="agentForm.id"
              :agent-id="agentForm.id"
              mode="skill-package"
            />
            <n-empty v-else description="请先保存智能体后再绑定技能资料包" size="small" />
          </div>
        </n-tab-pane>

        <n-tab-pane name="skills" tab="技能绑定">
          <div class="modal-section">
            <n-spin :show="skillLoading">
              <n-checkbox-group v-model:value="boundSkillIds">
                <div class="skill-check-list">
                  <div v-for="skill in allSkills" :key="skill.id" class="skill-check-item">
                    <n-checkbox :value="skill.id">
                      <span class="skill-label">{{ skill.name }}</span>
                      <n-tag v-if="skill.category" size="tiny" :bordered="false">
                        {{ skill.category }}
                      </n-tag>
                    </n-checkbox>
                  </div>
                </div>
              </n-checkbox-group>
              <n-empty v-if="!allSkills.length && !skillLoading" description="暂无可用技能" size="small" />
            </n-spin>
            <div v-if="allSkills.length" class="modal-inline-footer">
              <NButton size="small" type="primary" :loading="skillSaveLoading" @click="saveSkillBindings">
                保存技能绑定
              </NButton>
            </div>
          </div>
        </n-tab-pane>

        <n-tab-pane name="knowledge" tab="知识库">
          <div class="modal-section">
            <div class="tool-section-header">
              <span class="tool-section-title">关联知识库 ({{ agentForm.knowledgeIds.length }})</span>
            </div>
            <p class="knowledge-tip">
              勾选的知识库会作为对话检索的默认来源，随「保存/发布」一起生效。
            </p>
            <n-spin :show="knowledgeLoading">
              <n-checkbox-group v-model:value="agentForm.knowledgeIds">
                <div class="skill-check-list">
                  <div v-for="k in knowledgeOptions" :key="k.id" class="skill-check-item">
                    <n-checkbox :value="Number(k.id)">
                      <span class="skill-label">{{ k.knowledgeName }}</span>
                      <n-tag v-if="k.description" size="tiny" :bordered="false">
                        {{ k.description }}
                      </n-tag>
                    </n-checkbox>
                  </div>
                </div>
              </n-checkbox-group>
              <n-empty v-if="!knowledgeOptions.length && !knowledgeLoading" description="暂无可用知识库" size="small" />
            </n-spin>
          </div>
        </n-tab-pane>

        <n-tab-pane name="questions" tab="推荐问题">
          <div class="modal-section">
            <n-form-item label="推荐问题">
              <n-dynamic-tags v-model:value="agentForm.extraConfig.suggestedQuestions" />
            </n-form-item>
          </div>
        </n-tab-pane>
      </n-tabs>
      <template #footer>
        <div class="modal-footer">
          <NButton size="small" type="primary" @click="toolModalVisible = false">
            完成
          </NButton>
        </div>
      </template>
    </n-modal>

    <n-modal
      v-model:show="contextModalVisible"
      preset="card"
      title="上下文配置"
      class="agent-context-modal"
      :bordered="false"
    >
      <div class="context-modal-toolbar">
        <div class="context-modal-hint">
          上下文会在调用智能体时注入，适合放规则、样例和领域知识。
        </div>
        <NButton size="small" type="primary" secondary @click="addContextConfig">
          <template #icon>
            <i class="i-lucide:plus" aria-hidden="true" />
          </template>
          添加上下文
        </NButton>
      </div>

      <div v-if="contextConfigs.length" class="context-modal-body">
        <n-collapse v-model:expanded-names="expandedContextNames" class="context-collapse">
          <n-collapse-item
            v-for="(context, index) in contextConfigs"
            :key="context.localKey"
            :name="context.localKey"
          >
            <template #header>
              <div class="context-collapse-title">
                <strong>{{ context.configName || `上下文 ${index + 1}` }}</strong>
                <small>{{ getContextPreview(context) }}</small>
              </div>
            </template>
            <template #header-extra>
              <div class="context-collapse-extra" @click.stop>
                <NTag size="small" :type="context.enabled ? 'success' : 'warning'" :bordered="false">
                  {{ context.configType || 'SPEC' }}
                </NTag>
                <n-switch v-model:value="context.enabled" size="small" />
                <NPopconfirm @positive-click="removeContextConfig(index)">
                  <template #trigger>
                    <NButton quaternary circle size="small" type="error" @click.stop>
                      <template #icon>
                        <i class="i-lucide:trash-2" aria-hidden="true" />
                      </template>
                    </NButton>
                  </template>
                  确定删除该上下文配置吗？
                </NPopconfirm>
              </div>
            </template>

            <div class="context-form">
              <div class="context-field-grid">
                <div class="context-field context-field-name">
                  <div class="context-field-label">
                    名称
                  </div>
                  <n-input v-model:value="context.configName" size="small" placeholder="配置名称" />
                </div>
                <div class="context-field">
                  <div class="context-field-label">
                    类型
                  </div>
                  <n-select v-model:value="context.configType" :options="contextTypeOptions" size="small" />
                </div>
                <div class="context-field">
                  <div class="context-field-label">
                    排序
                  </div>
                  <n-input-number
                    v-model:value="context.sort"
                    size="small"
                    :min="0"
                    :show-button="false"
                    class="sort-input"
                  />
                </div>
              </div>
              <div class="context-field">
                <div class="context-field-label">
                  内容
                </div>
                <n-input
                  v-model:value="context.configContent"
                  type="textarea"
                  class="context-content-input"
                  :autosize="{ minRows: 6, maxRows: 12 }"
                  placeholder="上下文内容会在调用智能体时注入"
                />
              </div>
            </div>
          </n-collapse-item>
        </n-collapse>
      </div>
      <div v-else class="context-empty">
        暂无上下文配置，点击上方添加
      </div>
      <template #footer>
        <div class="modal-footer">
          <NButton size="small" type="primary" @click="contextModalVisible = false">
            完成
          </NButton>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script>
import { agentLocalComponents } from './agentLocalComponents'
import { useAgentPage } from './composables/useAgentPage'

export default {
  name: 'AiAgent',
  components: {
    ...agentLocalComponents,
  },
  setup(_props, { expose }) {
    return useAgentPage(expose)
  },
}
</script>

<style scoped src="./agent-shell.css"></style>

<style scoped src="./agent-panels.css"></style>
