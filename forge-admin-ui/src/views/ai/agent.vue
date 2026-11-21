<template>
  <div class="agent-console-page">
    <div v-if="viewMode === 'market'" class="agent-market">
      <div class="agent-toolbar">
        <div>
          <div class="page-title">
            智能体管理
          </div>
          <div class="page-subtitle">
            共 {{ pagination.itemCount }} 个智能体
          </div>
        </div>
        <NButton type="primary" size="large" @click="handleCreateAgent">
          <template #icon>
            <i class="ai-icon:plus" />
          </template>
          创建智能体
        </NButton>
      </div>

      <div class="agent-filter">
        <n-input
          v-model:value="filter.keyword"
          clearable
          placeholder="搜索名称、编码或描述"
          class="filter-search"
        >
          <template #prefix>
            <i class="ai-icon:search" />
          </template>
        </n-input>
        <n-select
          v-model:value="filter.status"
          :options="statusFilterOptions"
          class="filter-status"
        />
        <NButton secondary @click="loadAgents">
          <template #icon>
            <i class="ai-icon:refresh-cw" />
          </template>
          刷新
        </NButton>
      </div>

      <n-spin :show="agentLoading">
        <div v-if="agentList.length" class="agent-grid">
          <article
            v-for="agent in agentList"
            :key="agent.id"
            class="agent-card"
            tabindex="0"
            @click="handleOpenDesigner(agent)"
            @keydown.enter.prevent="handleOpenDesigner(agent)"
            @keydown.space.prevent="handleOpenDesigner(agent)"
          >
            <div class="agent-card-header">
              <div class="agent-avatar" :style="{ background: getAgentGradient(agent.agentCode) }">
                {{ getAgentInitial(agent) }}
              </div>
              <div class="agent-card-title">
                <div class="agent-name-row">
                  <span class="agent-name">{{ agent.agentName }}</span>
                  <NTag :type="agent.status === '0' ? 'success' : 'warning'" size="small" round>
                    {{ agent.status === '0' ? '已发布' : '草稿' }}
                  </NTag>
                </div>
                <div class="agent-code">
                  {{ agent.agentCode }}
                </div>
              </div>
            </div>

            <p class="agent-description">
              {{ agent.description || '暂无描述' }}
            </p>

            <div class="agent-meta-grid">
              <div class="agent-meta-item">
                <span>模型</span>
                <strong>{{ agent.modelName || getProviderDefaultModel(agent.providerId) || '-' }}</strong>
              </div>
              <div class="agent-meta-item">
                <span>温度</span>
                <strong>{{ agent.temperature ?? '0.70' }}</strong>
              </div>
            </div>

            <div class="agent-chip-row">
              <NTag v-for="tool in getAgentTools(agent).slice(0, 3)" :key="tool" size="small" round>
                {{ getMcpToolLabel(tool) }}
              </NTag>
              <NTag v-if="getAgentTools(agent).length > 3" size="small" round>
                +{{ getAgentTools(agent).length - 3 }}
              </NTag>
              <span v-if="!getAgentTools(agent).length" class="muted-chip">未配置 MCP</span>
            </div>

            <div class="agent-design-hint">
              点击卡片进入设计表单
            </div>

            <div class="agent-card-footer" @click.stop>
              <NButton text type="primary" @click.stop="goToChat(agent)">
                测试对话
              </NButton>
              <NButton text type="primary" @click.stop="handleEditAgent(agent)">
                编辑信息
              </NButton>
              <NPopconfirm
                v-if="agent.status === '0'"
                @positive-click="handleTogglePublish(agent)"
              >
                <template #trigger>
                  <NButton text type="warning" @click.stop>
                    下线
                  </NButton>
                </template>
                下线后用户将无法访问该智能体，确定下线吗？
              </NPopconfirm>
              <NButton
                v-else
                text
                type="success"
                @click.stop="handleTogglePublish(agent)"
              >
                发布
              </NButton>
              <NPopconfirm @positive-click="handleDeleteAgent(agent)">
                <template #trigger>
                  <NButton text type="error">
                    删除
                  </NButton>
                </template>
                确定删除该智能体吗？
              </NPopconfirm>
            </div>
          </article>
        </div>

        <div v-else class="empty-state">
          <i class="ai-icon:message-circle" />
          <span>暂无智能体</span>
        </div>
      </n-spin>

      <div v-if="pagination.itemCount > 0" class="pagination-wrap">
        <span class="pagination-total">共 {{ pagination.itemCount }} 个智能体</span>
        <n-pagination
          v-model:page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="[6, 12, 24, 48]"
          show-size-picker
          size="small"
          @update:page="loadAgents"
          @update:page-size="handleAgentPageSizeChange"
        />
      </div>

      <n-modal
        v-model:show="baseModalVisible"
        preset="card"
        :title="baseModalTitle"
        class="agent-base-modal"
        :bordered="false"
      >
        <n-form ref="baseFormRef" :model="baseForm" :rules="baseRules" label-placement="top">
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
              placeholder="用于列表卡片展示和会话识别"
            />
          </n-form-item>
        </n-form>
        <template #footer>
          <div class="modal-footer">
            <NButton @click="baseModalVisible = false">
              取消
            </NButton>
            <NButton type="primary" :loading="baseSaveLoading" @click="handleSaveBaseInfo">
              保存
            </NButton>
          </div>
        </template>
      </n-modal>
    </div>

    <div v-else class="agent-workbench">
      <div class="workbench-header">
        <div class="workbench-title-block">
          <NButton quaternary circle @click="backToMarket">
            <template #icon>
              <i class="ai-icon:arrow-left" />
            </template>
          </NButton>
          <div>
            <div class="workbench-title">
              {{ agentForm.id ? agentForm.agentName || '编辑智能体' : '创建智能体' }}
            </div>
            <div class="workbench-subtitle">
              {{ agentForm.agentCode || '未设置编码' }}
            </div>
          </div>
        </div>
        <div class="workbench-control-cluster">
          <div class="model-command-card">
            <div class="model-command-tabs">
              <button type="button" class="agent-settings-pill" @click="handleOpenWorkbenchBaseSettings">
                <i class="ai-icon:settings" />
                <span>Agent 设置</span>
              </button>
              <n-popover trigger="click" placement="bottom-end" :width="560">
                <template #trigger>
                  <button type="button" class="active-model-pill">
                    <span class="model-pill-icon">
                      <i class="ai-icon:layers" />
                    </span>
                    <span class="model-pill-name">{{ selectedModelLabel }}</span>
                    <span class="model-chat-badge">CHAT</span>
                    <i class="model-pill-arrow ai-icon:chevron-down" />
                  </button>
                </template>
                <div class="model-dropdown-panel">
                  <div class="model-selector-card model-mode-selector">
                    <div class="model-selector-field">
                      <span class="model-selector-label">模型选择方式</span>
                      <n-select v-model:value="agentForm.modelSelectionMode" :options="modelSelectionModeOptions" size="small" :bordered="false" />
                    </div>
                    <div v-if="agentForm.modelSelectionMode === 'POLICY'" class="model-selector-field model-field">
                      <span class="model-selector-label">路由策略</span>
                      <n-select v-model:value="agentForm.routePolicyId" :options="routePolicyOptions" filterable clearable size="small" :bordered="false" placeholder="选择路由策略" />
                    </div>
                  </div>
                  <div class="model-selector-card">
                    <div class="model-selector-icon">
                      <i class="ai-icon:layers" />
                    </div>
                    <div class="model-selector-field provider-field">
                      <span class="model-selector-label">供应商</span>
                      <n-select
                        v-model:value="agentForm.providerId"
                        :options="providerOptions"
                        class="model-provider-select"
                        clearable
                        filterable
                        size="small"
                        :bordered="false"
                        :disabled="agentForm.modelSelectionMode === 'POLICY'"
                        placeholder="选择供应商"
                      />
                    </div>
                    <div class="model-selector-divider" />
                    <div class="model-selector-field model-field">
                      <span class="model-selector-label">模型</span>
                      <n-select
                        v-model:value="agentForm.modelName"
                        :options="modelOptions"
                        :loading="modelLoading"
                        class="model-name-select"
                        clearable
                        filterable
                        tag
                        size="small"
                        :bordered="false"
                        :disabled="agentForm.modelSelectionMode === 'POLICY'"
                        placeholder="选择或输入模型"
                      />
                    </div>
                    <span class="selector-chat-badge">CHAT</span>
                  </div>

                  <div class="model-param-panel">
                    <div class="param-panel-header">
                      <div>
                        <div class="param-panel-title">
                          参数配置
                        </div>
                        <div class="param-panel-subtitle">
                          调整当前模型的生成风格和回复长度
                        </div>
                      </div>
                      <NButton quaternary circle size="small" @click="resetModelParams">
                        <template #icon>
                          <i class="ai-icon:refresh-ccw" />
                        </template>
                      </NButton>
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

                    <button type="button" class="multi-model-link">
                      <span>多个模型进行调试</span>
                      <i class="ai-icon:arrow-right" />
                    </button>
                  </div>
                </div>
              </n-popover>
            </div>
          </div>

          <div class="workbench-actions">
            <NButton size="small" ghost :disabled="!agentForm.id" title="在新页面打开独立对话" @click="goToChat">
              对话
            </NButton>
            <NButton class="draft-save-button" :loading="saveLoading" @click="handleSaveDraft">
              保存
            </NButton>
            <div class="publish-btn-group">
              <NButton
                class="publish-main-button"
                type="primary"
                :loading="publishLoading"
                :disabled="publishLoading"
                @click="handlePublishAgent"
              >
                发布
              </NButton>
              <n-popover v-model:show="publishMenuVisible" trigger="click" placement="bottom-end" :width="336">
                <template #trigger>
                  <NButton class="publish-caret-button" type="primary" :loading="publishLoading">
                    <i class="ai-icon:chevron-down" />
                  </NButton>
                </template>
                <div class="publish-panel">
                  <section class="publish-status-card">
                    <div class="publish-status-head">
                      <div>
                        <div class="publish-eyebrow">
                          最新发布
                        </div>
                        <div class="publish-time-text">
                          {{ publishTimeText }}
                        </div>
                      </div>
                      <button type="button" class="restore-button" @click="handlePendingPublishAction('恢复')">
                        恢复
                      </button>
                    </div>
                    <button
                      type="button"
                      class="publish-update-button"
                      :disabled="publishLoading"
                      @click="handlePublishMenuUpdate"
                    >
                      更新
                    </button>
                  </section>

                  <section class="publish-action-card">
                    <button type="button" class="publish-action-row" @click="handlePendingPublishAction('运行')">
                      <span class="publish-action-left">
                        <i class="publish-action-icon ai-icon:send" />
                        <span>运行</span>
                      </span>
                      <i class="publish-action-arrow ai-icon:arrow-right" />
                    </button>
                    <button type="button" class="publish-action-row" @click="handlePendingPublishAction('嵌入网站')">
                      <span class="publish-action-left">
                        <i class="publish-action-icon ai-icon:code" />
                        <span>嵌入网站</span>
                      </span>
                      <i class="publish-action-arrow ai-icon:arrow-right" />
                    </button>
                    <button type="button" class="publish-action-row" @click="handlePendingPublishAction('探索')">
                      <span class="publish-action-left">
                        <i class="publish-action-icon ai-icon:message-circle" />
                        <span>在 “探索” 中打开</span>
                      </span>
                      <i class="publish-action-arrow ai-icon:arrow-right" />
                    </button>
                    <button type="button" class="publish-action-row" @click="handlePendingPublishAction('访问 API')">
                      <span class="publish-action-left">
                        <i class="publish-action-icon ai-icon:link" />
                        <span>访问 API</span>
                      </span>
                      <i class="publish-action-arrow ai-icon:arrow-right" />
                    </button>
                  </section>
                </div>
              </n-popover>
            </div>
          </div>
        </div>
      </div>

      <div class="workbench-body dify-workbench-body">
        <section class="orchestration-panel">
          <div class="orchestration-header">
            <div>
              <div class="orchestration-title">
                编排
              </div>
              <div class="orchestration-subtitle">
                配置智能体人设、提示词、工具与上下文
              </div>
            </div>
            <NButton size="small" secondary @click="handleSaveDraft">
              保存草稿
            </NButton>
          </div>

          <div class="orchestration-scroll">
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
          </div>
        </section>

        <section class="chat-panel">
          <div class="chat-header">
            <div class="chat-agent">
              <div class="chat-avatar" :style="{ background: getAgentGradient(agentForm.agentCode) }">
                {{ getAgentInitial(agentForm) }}
              </div>
              <div>
                <div class="chat-title">
                  调试与预览
                </div>
                <div class="chat-subtitle">
                  {{ agentForm.status === '0' ? '已发布配置' : '草稿不可调用' }}
                </div>
              </div>
            </div>
            <NButton size="small" tertiary :disabled="chatSending" @click="resetConversation">
              <template #icon>
                <i class="ai-icon:refresh-ccw" />
              </template>
              新会话
            </NButton>
          </div>

          <div ref="messageListRef" class="chat-scroll">
            <div class="chat-message-list">
              <div
                v-for="message in previewMessages"
                :key="message.id"
                class="chat-message"
                :class="message.role === 'user' ? 'message-user' : 'message-assistant'"
              >
                <div class="message-avatar">
                  <i v-if="message.role === 'assistant'" class="ai-icon:message-circle" />
                  <span v-else>我</span>
                </div>
                <div class="message-body">
                  <div class="message-meta">
                    <span>{{ message.role === 'user' ? '我' : agentForm.agentName || '智能体' }}</span>
                    <span>{{ message.time }}</span>
                  </div>
                  <div v-if="message.reasoning && message.reasoning.trim()" class="reasoning-section">
                    <div class="reasoning-header">
                      <div class="reasoning-header-left">
                        <i class="ai-icon:brain" />
                        <span>思考过程</span>
                        <small v-if="message.reasoningTime">用时 {{ message.reasoningTime }}s</small>
                        <small v-else-if="message.isReasoning">思考中...</small>
                      </div>
                    </div>
                    <div class="reasoning-content">
                      {{ message.reasoning }}
                    </div>
                  </div>
                  <div v-if="message.content || (message.streaming && !message.isReasoning)" class="message-bubble">
                    <template v-if="message.content">
                      {{ message.content }}
                    </template>
                    <span v-if="message.streaming && !message.isReasoning" class="stream-cursor" />
                  </div>
                </div>
              </div>
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

          <div class="chat-composer">
            <n-input
              v-model:value="chatInput"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 5 }"
              placeholder="输入测试问题"
              :disabled="chatSending"
              @keydown.enter.exact.prevent="sendChatMessage"
            />
            <div class="composer-actions">
              <span class="composer-status">
                {{ chatStatusText }}
              </span>
              <NButton v-if="chatSending" type="warning" @click="stopChat">
                停止
              </NButton>
              <NButton v-else type="primary" :disabled="!canSendChat" @click="sendChatMessage">
                <template #icon>
                  <i class="ai-icon:send" />
                </template>
                发送
              </NButton>
            </div>
          </div>
        </section>
      </div>

      <n-modal
        v-model:show="toolModalVisible"
        preset="card"
        title="工具与技能"
        class="agent-config-modal"
        :bordered="false"
        style="width: 680px;"
      >
        <n-tabs type="segment" animated>
          <!-- 工具绑定 Tab -->
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
                    <n-tag size="tiny" :type="tool.toolSource === 'mcp' ? 'info' : 'default'">
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

              <!-- 添加工具表单 -->
              <div v-if="showAddTool" class="add-tool-form">
                <n-divider />
                <n-form inline>
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

          <!-- MCP 远程工具快速勾选 Tab -->
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

          <!-- 技能资料包 Tab -->
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

          <!-- 技能绑定 Tab -->
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
            </div>
            <template #footer>
              <div class="modal-footer">
                <NButton type="primary" :loading="skillSaveLoading" @click="saveSkillBindings">
                  保存
                </NButton>
              </div>
            </template>
          </n-tab-pane>

          <!-- 知识库 Tab -->
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

          <!-- 推荐问题 Tab -->
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
            <NButton type="primary" @click="toolModalVisible = false">
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
              <i class="ai-icon:plus" />
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
                  <NTag size="small" :type="context.enabled ? 'success' : 'warning'" round>
                    {{ context.configType || 'SPEC' }}
                  </NTag>
                  <n-switch v-model:value="context.enabled" size="small" />
                  <NPopconfirm @positive-click="removeContextConfig(index)">
                    <template #trigger>
                      <NButton quaternary circle size="small" type="error" @click.stop>
                        <template #icon>
                          <i class="ai-icon:trash-2" />
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
                    :autosize="{ minRows: 7, maxRows: 14 }"
                    placeholder="上下文内容会在调用智能体时注入"
                  />
                </div>
              </div>
            </n-collapse-item>
          </n-collapse>
        </div>
        <div v-else class="context-empty context-modal-empty">
          暂无上下文配置，点击上方添加
        </div>
        <template #footer>
          <div class="modal-footer">
            <NButton type="primary" @click="contextModalVisible = false">
              完成
            </NButton>
          </div>
        </template>
      </n-modal>
    </div>
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
