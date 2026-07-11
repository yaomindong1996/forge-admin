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
              <NButton text type="primary" @click.stop="handleEditAgent(agent)">
                编辑信息
              </NButton>
              <NButton
                text
                :type="agent.status === '0' ? 'warning' : 'success'"
                @click.stop="handleTogglePublish(agent)"
              >
                {{ agent.status === '0' ? '下线' : '发布' }}
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
            <NButton class="draft-save-button" :loading="saveLoading" @click="handleSaveDraft">
              保存
            </NButton>
            <n-popover v-model:show="publishMenuVisible" trigger="click" placement="bottom-end" :width="336">
              <template #trigger>
                <NButton class="publish-button" type="primary" :loading="publishLoading">
                  发布
                  <template #icon>
                    <i class="ai-icon:chevron-down" />
                  </template>
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
            <n-form ref="agentFormRef" :model="agentForm" :rules="agentRules" label-placement="top">
              <div class="panel-section panel-card prompt-card">
                <div class="section-title compact-section-title">
                  <i class="ai-icon:user-check" />
                  <span>提示词</span>
                  <small>{{ agentForm.agentName || agentForm.agentCode }}</small>
                </div>
                <n-grid :cols="1" :x-gap="14">
                  <n-form-item-gi label="系统提示词" path="systemPrompt">
                    <n-input
                      v-model:value="agentForm.systemPrompt"
                      type="textarea"
                      :autosize="{ minRows: 8, maxRows: 16 }"
                      placeholder="定义智能体角色、目标、边界和输出要求"
                    />
                  </n-form-item-gi>
                  <n-form-item-gi label="开场白">
                    <n-input
                      v-model:value="agentForm.extraConfig.openingStatement"
                      type="textarea"
                      :rows="3"
                      placeholder="显示在右侧测试会话的首条消息"
                    />
                  </n-form-item-gi>
                </n-grid>
              </div>

              <div class="config-entry-grid">
                <button type="button" class="config-entry-card" @click="toolModalVisible = true">
                  <div class="config-entry-icon">
                    <i class="ai-icon:tool" />
                  </div>
                  <div class="config-entry-main">
                    <div class="config-entry-title">
                      工具与技能
                    </div>
                    <div class="config-entry-desc">
                      {{ selectedMcpToolLabels.length }} 个 MCP · {{ suggestedQuestionCount }} 个推荐问题
                    </div>
                    <div class="config-entry-tags">
                      <NTag
                        v-for="label in selectedMcpToolLabels.slice(0, 3)"
                        :key="label"
                        size="small"
                        round
                      >
                        {{ label }}
                      </NTag>
                      <span v-if="!selectedMcpToolLabels.length" class="config-entry-empty">未配置工具</span>
                    </div>
                  </div>
                  <i class="config-entry-arrow ai-icon:chevron-right" />
                </button>

                <button type="button" class="config-entry-card" @click="contextModalVisible = true">
                  <div class="config-entry-icon context-entry-icon">
                    <i class="ai-icon:book-open" />
                  </div>
                  <div class="config-entry-main">
                    <div class="config-entry-title">
                      上下文
                    </div>
                    <div class="config-entry-desc">
                      {{ contextConfigs.length }} 个配置 · {{ enabledContextCount }} 个启用
                    </div>
                    <div class="config-entry-preview">
                      {{ contextConfigs[0]?.configName || '导入知识、规则或样例作为上下文' }}
                    </div>
                  </div>
                  <i class="config-entry-arrow ai-icon:chevron-right" />
                </button>
              </div>
            </n-form>
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
      >
        <div class="modal-section">
          <n-form label-placement="top">
            <n-form-item label="MCP 工具">
              <div class="w-full">
                <n-space v-if="selectedMcpToolLabels.length" size="small" class="mb-2">
                  <n-tag
                    v-for="tool in selectedMcpToolLabels"
                    :key="tool"
                    size="small"
                    type="warning"
                    :bordered="false"
                  >
                    {{ tool }}（预留）
                  </n-tag>
                </n-space>
                <n-text depth="3">
                  MCP 能力目录尚未开放配置。历史选项仅保留展示，不会自动启用或改写。
                </n-text>
              </div>
            </n-form-item>
            <n-form-item label="推荐问题">
              <n-dynamic-tags v-model:value="agentForm.extraConfig.suggestedQuestions" />
            </n-form-item>
            <n-form-item label="Skill 预留">
              <n-dynamic-tags v-model:value="agentForm.extraConfig.skills" />
            </n-form-item>
          </n-form>
        </div>
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

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  agentAdd,
  agentDelete,
  agentGetById,
  agentPage,
  agentUpdate,
  contextConfigAdd,
  contextConfigDelete,
  contextConfigList,
  contextConfigUpdate,
  modelListByProvider,
  providerPage,
  routePolicyPage,
  streamAgentChat,
} from '@/api/ai'
import { useDict } from '@/composables/useDict'
import { generateUUID } from '@/utils'

defineOptions({ name: 'AiAgent' })

const { dict } = useDict('ai_agent_model_selection_mode')

const viewMode = ref('market')
const agentLoading = ref(false)
const saveLoading = ref(false)
const publishLoading = ref(false)
const modelLoading = ref(false)
const toolModalVisible = ref(false)
const contextModalVisible = ref(false)
const baseModalVisible = ref(false)
const publishMenuVisible = ref(false)
const baseSaveLoading = ref(false)
const agentFormRef = ref(null)
const baseFormRef = ref(null)
const messageListRef = ref(null)
const agentList = ref([])
const providerList = ref([])
const modelList = ref([])
const routePolicyList = ref([])
const contextConfigs = ref([])
const deletedContextIds = ref([])
const expandedContextNames = ref([])
const chatMessages = ref([])
const chatInput = ref('')
const chatSending = ref(false)
const chatStatusText = ref('等待输入')
const sessionId = ref(generateUUID())
const pagination = reactive({ page: 1, pageSize: 12, itemCount: 0 })
const filter = reactive({ keyword: '', status: 'all' })
const baseMode = ref('create')
const baseEditingAgent = ref(null)
const reasoningDelimiter = '==================== 思考过程 ===================='
const answerDelimiter = '==================== 完整回复 ===================='
let chatAbortController = null
let chatScrollFrame = null
let activeChatStreamId = null
let hydratingProvider = false

const defaultPrompt = `你是一个企业级智能体。请根据用户问题给出准确、清晰、可执行的回答。

要求：
1. 优先基于已配置的上下文和工具能力回答。
2. 不确定的信息要说明限制，不要编造。
3. 输出结构清晰，必要时给出步骤或清单。`

const agentForm = reactive(createEmptyAgent())
const modelParamEnabled = reactive({
  temperature: true,
  maxTokens: true,
})
const baseForm = reactive({
  id: null,
  agentName: '',
  agentCode: '',
  description: '',
})

const statusFilterOptions = [
  { label: '全部状态', value: 'all' },
  { label: '已发布', value: '0' },
  { label: '草稿', value: '1' },
]

const contextTypeOptions = [
  { label: 'SPEC', value: 'SPEC' },
  { label: 'RULE', value: 'RULE' },
  { label: 'SAMPLE', value: 'SAMPLE' },
]

const reservedMcpToolOptions = [
  { label: '知识库检索', value: 'knowledge_search' },
  { label: '数据库查询', value: 'database_query' },
  { label: 'HTTP 请求', value: 'http_request' },
  { label: '文件读取', value: 'file_reader' },
  { label: '工作流触发', value: 'workflow_trigger' },
  { label: '代码执行', value: 'code_executor' },
]

const baseRules = {
  agentName: [{ required: true, message: '请输入智能体名称', trigger: 'blur' }],
  agentCode: [
    { required: true, message: '请输入智能体编码', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]{2,49}$/,
      message: '编码需以小写字母开头，仅支持小写字母、数字和下划线',
      trigger: 'blur',
    },
  ],
}

const agentRules = {
  systemPrompt: [{ required: true, message: '请输入系统提示词', trigger: 'blur' }],
}

const providerOptions = computed(() => providerList.value.map(provider => ({
  label: provider.providerName,
  value: toIdString(provider.id),
})))

const modelOptions = computed(() => modelList.value.map(model => ({
  label: model.modelName ? `${model.modelName}（${model.modelId}）` : model.modelId,
  value: model.modelId,
})))

const modelSelectionModeOptions = computed(() => dict.value.ai_agent_model_selection_mode || [])
const routePolicyOptions = computed(() => routePolicyList.value.map(policy => ({ label: policy.policyName, value: toIdString(policy.id) })))

const selectedProviderName = computed(() => {
  const provider = providerList.value.find(item => toIdString(item.id) === toIdString(agentForm.providerId))
  return provider?.providerName || '未选择供应商'
})

const selectedModelLabel = computed(() => {
  if (agentForm.modelSelectionMode === 'POLICY') {
    return routePolicyList.value.find(policy => toIdString(policy.id) === toIdString(agentForm.routePolicyId))?.policyName || '选择路由策略'
  }
  if (!agentForm.modelName) {
    if (!agentForm.providerId) {
      return '选择模型'
    }
    return getProviderDefaultModel(agentForm.providerId) || selectedProviderName.value
  }
  const selectedModel = modelList.value.find(model => model.modelId === agentForm.modelName)
  return selectedModel?.modelName || agentForm.modelName
})

const pageCount = computed(() => Math.max(1, Math.ceil(pagination.itemCount / pagination.pageSize)))

const baseModalTitle = computed(() => baseMode.value === 'create' ? '创建智能体' : '编辑基础信息')
const enabledContextCount = computed(() => contextConfigs.value.filter(config => config.enabled).length)
const selectedMcpToolLabels = computed(() => (agentForm.extraConfig.mcpTools || []).map(getMcpToolLabel))
const suggestedQuestionCount = computed(() => (agentForm.extraConfig.suggestedQuestions || []).filter(Boolean).length)
const publishTimeText = computed(() => {
  if (agentForm.status !== '0') {
    return '尚未发布'
  }
  const publishedAt = agentForm.updateTime || agentForm.createTime
  return publishedAt ? `发布于 ${formatRelativeTime(publishedAt)}` : '已发布'
})

const openingStatement = computed(() => {
  return agentForm.extraConfig.openingStatement || `你好，我是${agentForm.agentName || '智能体'}，可以开始测试。`
})

const previewMessages = computed(() => {
  if (chatMessages.value.length) {
    return chatMessages.value
  }
  return [{
    id: 'opening',
    role: 'assistant',
    content: openingStatement.value,
    time: formatTime(new Date()),
  }]
})

const suggestedQuestionList = computed(() => {
  return (agentForm.extraConfig.suggestedQuestions || []).filter(Boolean).slice(0, 4)
})

const canSendChat = computed(() => {
  return !!agentForm.id
    && agentForm.status === '0'
    && !!agentForm.agentCode
    && !!chatInput.value.trim()
    && !chatSending.value
})

watch(() => filter.keyword, () => {
  pagination.page = 1
  loadAgents()
})

watch(() => filter.status, () => {
  pagination.page = 1
  loadAgents()
})

watch(() => pagination.page, () => {
  if (pagination.page > pageCount.value) {
    pagination.page = pageCount.value
  }
})

watch(() => pagination.pageSize, () => {
  pagination.page = 1
})

watch(() => chatMessages.value.length, () => scrollChatToBottom())

watch(() => agentForm.providerId, async (providerId) => {
  const shouldResetModel = !hydratingProvider
  await loadModels(providerId)
  if (shouldResetModel) {
    agentForm.modelName = null
  }
})

function createEmptyAgent() {
  return {
    id: null,
    agentName: '',
    agentCode: '',
    description: '',
    systemPrompt: defaultPrompt,
    providerId: null,
    modelName: null,
    modelSelectionMode: 'PINNED',
    routePolicyId: null,
    temperature: 0.7,
    maxTokens: 4000,
    status: '1',
    extraConfig: createDefaultExtraConfig(),
  }
}

function createDefaultExtraConfig() {
  return {
    openingStatement: '',
    suggestedQuestions: [],
    mcpTools: [],
    skills: [],
  }
}

function resetAgentForm(agent = createEmptyAgent()) {
  modelParamEnabled.temperature = agent.temperature !== null && agent.temperature !== undefined
  modelParamEnabled.maxTokens = agent.maxTokens !== null && agent.maxTokens !== undefined
  Object.assign(agentForm, {
    ...createEmptyAgent(),
    ...agent,
    id: toIdString(agent.id) || null,
    providerId: toIdString(agent.providerId) || null,
    modelName: agent.modelName || null,
    modelSelectionMode: agent.modelSelectionMode || 'PINNED',
    routePolicyId: toIdString(agent.routePolicyId) || null,
    temperature: Number(agent.temperature ?? 0.7),
    maxTokens: Number(agent.maxTokens ?? 4000),
    status: agent.status ?? '1',
    extraConfig: parseExtraConfig(agent.extraConfig),
  })
}

function parseExtraConfig(value) {
  let parsed = {}
  if (value) {
    if (typeof value === 'string') {
      try {
        parsed = JSON.parse(value)
      }
      catch {
        parsed = {}
      }
    }
    else if (typeof value === 'object') {
      parsed = value
    }
  }
  const defaults = createDefaultExtraConfig()
  return {
    ...defaults,
    ...parsed,
    suggestedQuestions: Array.isArray(parsed.suggestedQuestions) ? parsed.suggestedQuestions : [],
    mcpTools: Array.isArray(parsed.mcpTools) ? parsed.mcpTools : [],
    skills: Array.isArray(parsed.skills) ? parsed.skills : [],
  }
}

function serializeAgent(status) {
  const extraConfig = {
    ...agentForm.extraConfig,
    contextCount: contextConfigs.value.length,
  }
  return {
    id: agentForm.id,
    agentName: agentForm.agentName,
    agentCode: agentForm.agentCode,
    description: agentForm.description,
    systemPrompt: agentForm.systemPrompt,
    providerId: toIdString(agentForm.providerId) || null,
    modelName: agentForm.modelName,
    modelSelectionMode: agentForm.modelSelectionMode || 'PINNED',
    routePolicyId: agentForm.modelSelectionMode === 'POLICY' ? toIdString(agentForm.routePolicyId) || null : null,
    temperature: modelParamEnabled.temperature ? toNullableNumber(agentForm.temperature) : null,
    maxTokens: modelParamEnabled.maxTokens ? toNullableInteger(agentForm.maxTokens) : null,
    status: status ?? agentForm.status,
    extraConfig: JSON.stringify(extraConfig),
  }
}

async function loadAgents() {
  agentLoading.value = true
  try {
    const params = {
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      keyword: filter.keyword.trim() || undefined,
      status: filter.status === 'all' ? undefined : filter.status,
    }
    const res = await agentPage(params)
    if (res.code === 200 && res.data) {
      const records = res.data.records || []
      pagination.itemCount = Number(res.data.total || 0)
      if (!records.length && pagination.page > 1 && pagination.itemCount > 0) {
        pagination.page = Math.min(pagination.page - 1, pageCount.value)
        await loadAgents()
        return
      }
      agentList.value = records
    }
  }
  catch (error) {
    window.$message.error(error.message || '加载智能体失败')
  }
  finally {
    agentLoading.value = false
  }
}

async function handleAgentPageSizeChange(pageSize) {
  pagination.pageSize = pageSize
  pagination.page = 1
  await loadAgents()
}

async function loadProviders() {
  try {
    const res = await providerPage({ pageNum: 1, pageSize: 200, status: '0' })
    if (res.code === 200 && res.data) {
      providerList.value = res.data.records || []
    }
  }
  catch {}
}

async function loadRoutePolicies() {
  try {
    const res = await routePolicyPage({ pageNum: 1, pageSize: 200, status: '0' })
    if (res.code === 200)
      routePolicyList.value = res.data?.records || []
  }
  catch {}
}

async function loadModels(providerId) {
  modelList.value = []
  if (!providerId) {
    return
  }

  modelLoading.value = true
  try {
    const res = await modelListByProvider(providerId)
    if (res.code === 200) {
      modelList.value = res.data || []
    }
  }
  catch {}
  finally {
    modelLoading.value = false
  }
}

function toIdString(value) {
  return value === null || value === undefined || value === '' ? '' : String(value)
}

function toNullableNumber(value) {
  if (value === null || value === undefined || value === '') {
    return null
  }
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue : null
}

function toNullableInteger(value) {
  const numericValue = toNullableNumber(value)
  return numericValue === null ? null : Math.round(numericValue)
}

async function loadAgentContexts(agentCode) {
  contextConfigs.value = []
  deletedContextIds.value = []
  expandedContextNames.value = []
  if (!agentCode) {
    return
  }

  try {
    const res = await contextConfigList(agentCode)
    if (res.code === 200) {
      contextConfigs.value = (res.data || []).map(config => ({
        ...config,
        localKey: `context_${config.id}`,
        enabled: config.status !== '1',
      }))
      expandedContextNames.value = contextConfigs.value.length ? [contextConfigs.value[0].localKey] : []
    }
  }
  catch {}
}

function getProviderDefaultModel(providerId) {
  const provider = providerList.value.find(item => toIdString(item.id) === toIdString(providerId))
  return provider?.defaultModel || ''
}

function getAgentInitial(agent) {
  return (agent.agentName || agent.agentCode || 'A').charAt(0).toUpperCase()
}

function getAgentGradient(code) {
  const gradients = [
    'linear-gradient(135deg, #2563eb 0%, #0891b2 100%)',
    'linear-gradient(135deg, #059669 0%, #0d9488 100%)',
    'linear-gradient(135deg, #7c3aed 0%, #2563eb 100%)',
    'linear-gradient(135deg, #d97706 0%, #dc2626 100%)',
    'linear-gradient(135deg, #475569 0%, #0f766e 100%)',
  ]
  const seed = (code || 'agent').split('').reduce((total, char) => total + char.charCodeAt(0), 0)
  return gradients[seed % gradients.length]
}

function getAgentTools(agent) {
  return parseExtraConfig(agent.extraConfig).mcpTools || []
}

function getMcpToolLabel(value) {
  return reservedMcpToolOptions.find(option => option.value === value)?.label || value
}

function getContextPreview(context) {
  const content = (context.configContent || '').replace(/\s+/g, ' ').trim()
  if (!content) {
    return '暂无内容'
  }
  return content.length > 48 ? `${content.slice(0, 48)}...` : content
}

function formatTime(date) {
  const h = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

function formatRelativeTime(value) {
  const normalizedValue = typeof value === 'string' ? value.replace(/-/g, '/') : value
  const timestamp = new Date(normalizedValue).getTime()
  if (!Number.isFinite(timestamp)) {
    return '未知时间'
  }

  const diffSeconds = Math.max(0, Math.floor((Date.now() - timestamp) / 1000))
  const minute = 60
  const hour = 60 * minute
  const day = 24 * hour
  const month = 30 * day
  const year = 365 * day

  if (diffSeconds < minute) {
    return '刚刚'
  }
  if (diffSeconds < hour) {
    return `${Math.floor(diffSeconds / minute)} 分钟前`
  }
  if (diffSeconds < day) {
    return `${Math.floor(diffSeconds / hour)} 小时前`
  }
  if (diffSeconds < month) {
    return `${Math.floor(diffSeconds / day)} 天前`
  }
  if (diffSeconds < year) {
    return `${Math.floor(diffSeconds / month)} 个月前`
  }
  return `${Math.floor(diffSeconds / year)} 年前`
}

async function openWorkbench(agent) {
  viewMode.value = 'builder'
  baseModalVisible.value = false
  chatInput.value = ''
  await hydrateAgentForm(agent)
  await loadAgentContexts(agent.agentCode)
  resetConversation()
  await nextTick()
  agentFormRef.value?.restoreValidation?.()
}

async function hydrateAgentForm(agent) {
  hydratingProvider = true
  resetAgentForm(agent)
  try {
    await nextTick()
    await loadModels(agent.providerId)
  }
  finally {
    hydratingProvider = false
  }
}

function resetBaseForm(agent = {}) {
  Object.assign(baseForm, {
    id: toIdString(agent.id) || null,
    agentName: agent.agentName || '',
    agentCode: agent.agentCode || '',
    description: agent.description || '',
  })
}

function openBaseModal(agent = null) {
  baseMode.value = agent?.id ? 'edit' : 'create'
  baseEditingAgent.value = agent
  resetBaseForm(agent || {})
  baseModalVisible.value = true
  nextTick(() => baseFormRef.value?.restoreValidation?.())
}

function handleOpenWorkbenchBaseSettings() {
  baseMode.value = 'edit'
  baseEditingAgent.value = serializeAgent()
  resetBaseForm(agentForm)
  baseModalVisible.value = true
  nextTick(() => baseFormRef.value?.restoreValidation?.())
}

function resetModelParams() {
  modelParamEnabled.temperature = true
  modelParamEnabled.maxTokens = true
  agentForm.temperature = 0.7
  agentForm.maxTokens = 4000
}

async function handleCreateAgent() {
  openBaseModal()
}

async function handleEditAgent(agent) {
  try {
    const res = await agentGetById(agent.id)
    if (res.code === 200 && res.data) {
      openBaseModal(res.data)
    }
  }
  catch (error) {
    window.$message.error(error.message || '加载智能体详情失败')
  }
}

async function handleOpenDesigner(agent) {
  try {
    const res = await agentGetById(agent.id)
    if (res.code === 200 && res.data) {
      await openWorkbench(res.data)
    }
  }
  catch (error) {
    window.$message.error(error.message || '加载智能体详情失败')
  }
}

function buildBaseAgentPayload(status = '1') {
  return {
    ...createEmptyAgent(),
    agentName: baseForm.agentName,
    agentCode: baseForm.agentCode,
    description: baseForm.description,
    providerId: providerList.value[0]?.id ? toIdString(providerList.value[0].id) : null,
    status,
    extraConfig: JSON.stringify(createDefaultExtraConfig()),
  }
}

function mergeBaseAgentPayload(agent) {
  return {
    ...agent,
    id: baseForm.id,
    agentName: baseForm.agentName,
    agentCode: baseForm.agentCode,
    description: baseForm.description,
    extraConfig: typeof agent.extraConfig === 'string'
      ? agent.extraConfig
      : JSON.stringify(parseExtraConfig(agent.extraConfig)),
  }
}

async function findAgentByCode(agentCode) {
  if (!agentCode) {
    return null
  }
  const res = await agentPage({ pageNum: 1, pageSize: 10, keyword: agentCode })
  if (res.code !== 200) {
    return null
  }
  const records = res.data?.records || []
  return records.find(agent => agent.agentCode === agentCode) || records[0] || null
}

async function handleSaveBaseInfo() {
  await baseFormRef.value?.validate()
  baseSaveLoading.value = true
  try {
    const payload = baseMode.value === 'create'
      ? buildBaseAgentPayload('1')
      : mergeBaseAgentPayload(baseEditingAgent.value || {})
    const res = payload.id ? await agentUpdate(payload) : await agentAdd(payload)
    if (res.code !== 200) {
      window.$message.error(res.msg || '保存失败')
      return
    }

    const editingCurrentWorkbench = viewMode.value === 'builder'
      && baseMode.value === 'edit'
      && toIdString(payload.id) === toIdString(agentForm.id)
    await loadAgents()
    baseModalVisible.value = false
    if (editingCurrentWorkbench) {
      agentForm.agentName = payload.agentName
      agentForm.agentCode = payload.agentCode
      agentForm.description = payload.description
    }
    window.$message.success('保存成功')

    if (baseMode.value === 'create') {
      const savedAgent = agentList.value.find(agent => agent.agentCode === payload.agentCode)
        || await findAgentByCode(payload.agentCode)
      if (savedAgent) {
        await openWorkbench(savedAgent)
      }
    }
  }
  catch (error) {
    window.$message.error(error.message || '保存失败')
  }
  finally {
    baseSaveLoading.value = false
  }
}

async function validateContextConfigs() {
  const invalidIndex = contextConfigs.value.findIndex(config => !config.configName?.trim() || !config.configContent?.trim())
  if (invalidIndex > -1) {
    expandedContextNames.value = [contextConfigs.value[invalidIndex].localKey]
    window.$message.error('请补全上下文配置名称和内容')
    return false
  }
  return true
}

async function saveAgent(status) {
  await agentFormRef.value?.validate()
  const contextValid = await validateContextConfigs()
  if (!contextValid) {
    return false
  }

  const payload = serializeAgent(status)
  const res = payload.id ? await agentUpdate(payload) : await agentAdd(payload)
  if (res.code !== 200) {
    window.$message.error(res.msg || '保存失败')
    return false
  }

  await syncContextConfigs(payload.agentCode)
  await loadAgents()
  const savedAgent = agentList.value.find(agent => agent.agentCode === payload.agentCode)
    || await findAgentByCode(payload.agentCode)
  if (savedAgent) {
    await hydrateAgentForm(savedAgent)
    await loadAgentContexts(savedAgent.agentCode)
  }
  return true
}

async function handleSaveDraft() {
  saveLoading.value = true
  try {
    const saved = await saveAgent(agentForm.status || '1')
    if (saved) {
      window.$message.success('保存成功')
    }
  }
  catch (error) {
    window.$message.error(error.message || '保存失败')
  }
  finally {
    saveLoading.value = false
  }
}

async function handlePublishAgent() {
  publishLoading.value = true
  try {
    const saved = await saveAgent('0')
    if (saved) {
      window.$message.success('发布成功')
    }
  }
  catch (error) {
    window.$message.error(error.message || '发布失败')
  }
  finally {
    publishLoading.value = false
  }
}

async function handlePublishMenuUpdate() {
  publishMenuVisible.value = false
  await handlePublishAgent()
}

function handlePendingPublishAction(label) {
  window.$message.info(`${label}功能待定`)
}

async function handleTogglePublish(agent) {
  const nextStatus = agent.status === '0' ? '1' : '0'
  try {
    const res = await agentUpdate({ ...agent, status: nextStatus })
    if (res.code === 200) {
      window.$message.success(nextStatus === '0' ? '发布成功' : '已下线')
      await loadAgents()
    }
    else {
      window.$message.error(res.msg || '操作失败')
    }
  }
  catch (error) {
    window.$message.error(error.message || '操作失败')
  }
}

async function handleDeleteAgent(agent) {
  try {
    const res = await agentDelete(agent.id)
    if (res.code === 200) {
      window.$message.success('删除成功')
      await loadAgents()
    }
    else {
      window.$message.error(res.msg || '删除失败')
    }
  }
  catch (error) {
    window.$message.error(error.message || '删除失败')
  }
}

async function syncContextConfigs(agentCode) {
  for (const id of deletedContextIds.value) {
    await contextConfigDelete(id)
  }

  for (const config of contextConfigs.value) {
    const payload = {
      id: config.id,
      agentCode,
      configName: config.configName,
      configContent: config.configContent,
      configType: config.configType || 'SPEC',
      sort: config.sort ?? 0,
      status: config.enabled ? '0' : '1',
    }

    if (payload.id) {
      await contextConfigUpdate(payload)
    }
    else {
      await contextConfigAdd(payload)
    }
  }
  deletedContextIds.value = []
}

function addContextConfig() {
  const localKey = `context_${Date.now()}_${Math.random().toString(36).slice(2)}`
  contextConfigs.value.push({
    localKey,
    agentCode: agentForm.agentCode,
    configName: '',
    configContent: '',
    configType: 'SPEC',
    sort: contextConfigs.value.length + 1,
    enabled: true,
  })
  expandedContextNames.value = [localKey]
}

function removeContextConfig(index) {
  const [removed] = contextConfigs.value.splice(index, 1)
  if (removed?.id) {
    deletedContextIds.value.push(removed.id)
  }
  expandedContextNames.value = expandedContextNames.value.filter(name => name !== removed?.localKey)
  if (!expandedContextNames.value.length && contextConfigs.value.length) {
    const nextIndex = Math.min(index, contextConfigs.value.length - 1)
    expandedContextNames.value = [contextConfigs.value[nextIndex].localKey]
  }
}

function backToMarket() {
  stopChat()
  viewMode.value = 'market'
}

function resetConversation() {
  stopChat()
  sessionId.value = generateUUID()
  chatMessages.value = []
  chatStatusText.value = agentForm.status === '0' ? '等待输入' : '发布后可测试'
}

function useSuggestedQuestion(question) {
  chatInput.value = question
  sendChatMessage()
}

async function sendChatMessage() {
  if (!canSendChat.value) {
    if (agentForm.status !== '0') {
      window.$message.warning('请先发布智能体后再测试')
    }
    return
  }

  const content = chatInput.value.trim()
  const now = formatTime(new Date())
  const assistantMessage = reactive({
    id: generateUUID(),
    role: 'assistant',
    content: '',
    reasoning: '',
    isReasoning: false,
    reasoningTime: null,
    time: now,
    streaming: true,
  })
  const streamId = assistantMessage.id
  const streamState = {
    isReasoning: false,
    reasoningStartTime: null,
  }
  const pendingPayloads = []
  let chunkFlushFrame = null

  function flushPendingChunks() {
    if (chunkFlushFrame !== null) {
      cancelAnimationFrame(chunkFlushFrame)
      chunkFlushFrame = null
    }
    if (activeChatStreamId !== streamId || !pendingPayloads.length) {
      pendingPayloads.length = 0
      return
    }

    const payloads = pendingPayloads.splice(0)
    for (const payload of payloads) {
      handleAgentSSEChunk(payload, assistantMessage, streamState)
    }
    scrollChatToBottom()
  }

  function scheduleChunkFlush(payload) {
    if (activeChatStreamId !== streamId) {
      return
    }
    pendingPayloads.push(payload)
    if (chunkFlushFrame !== null) {
      return
    }
    chunkFlushFrame = requestAnimationFrame(() => {
      chunkFlushFrame = null
      flushPendingChunks()
    })
  }

  chatMessages.value.push({
    id: generateUUID(),
    role: 'user',
    content,
    time: now,
  })
  chatMessages.value.push(assistantMessage)
  chatInput.value = ''
  chatSending.value = true
  activeChatStreamId = streamId
  chatStatusText.value = '生成中'
  await nextTick()
  scrollChatToBottom()

  chatAbortController = streamAgentChat(
    {
      agentCode: agentForm.agentCode,
      message: content,
      userInput: content,
      sessionId: sessionId.value,
      providerId: toIdString(agentForm.providerId) || null,
      modelName: agentForm.modelName,
      temperature: modelParamEnabled.temperature ? toNullableNumber(agentForm.temperature) : null,
      maxTokens: modelParamEnabled.maxTokens ? toNullableInteger(agentForm.maxTokens) : null,
    },
    (payload) => {
      scheduleChunkFlush(payload)
    },
    (data) => {
      flushPendingChunks()
      if (data?.sessionId) {
        sessionId.value = data.sessionId
      }
      finishAgentReasoning(assistantMessage, streamState)
      assistantMessage.streaming = false
      chatSending.value = false
      chatStatusText.value = '完成'
      chatAbortController = null
      activeChatStreamId = null
      scrollChatToBottom()
    },
    (message) => {
      flushPendingChunks()
      finishAgentReasoning(assistantMessage, streamState)
      assistantMessage.streaming = false
      assistantMessage.content = assistantMessage.content || `测试失败：${message}`
      chatSending.value = false
      chatStatusText.value = '测试失败'
      chatAbortController = null
      activeChatStreamId = null
      window.$message.error(message || '测试失败')
      scrollChatToBottom()
    },
  )
}

function handleAgentSSEChunk(payload, assistantMessage, streamState) {
  const event = typeof payload === 'string' ? 'chunk' : payload?.event
  const data = typeof payload === 'string' ? { content: payload } : payload?.data || {}

  if (event === 'progress') {
    chatStatusText.value = data.message || '生成中'
    return
  }

  const chunkContent = data.content || data.message || ''
  if (!chunkContent) {
    return
  }

  appendAgentChunk(assistantMessage, streamState, chunkContent)
  chatStatusText.value = streamState.isReasoning ? '思考中' : '生成中'
}

function appendAgentChunk(assistantMessage, streamState, chunkContent) {
  let remaining = chunkContent

  while (remaining) {
    const reasoningIndex = remaining.indexOf(reasoningDelimiter)
    const answerIndex = remaining.indexOf(answerDelimiter)
    const nextDelimiter = getNextDelimiter(reasoningIndex, answerIndex)

    if (!nextDelimiter) {
      appendAgentText(assistantMessage, streamState, remaining)
      return
    }

    if (nextDelimiter.index > 0) {
      appendAgentText(assistantMessage, streamState, remaining.slice(0, nextDelimiter.index))
    }

    if (nextDelimiter.type === 'reasoning') {
      streamState.isReasoning = true
      streamState.reasoningStartTime = Date.now()
      assistantMessage.isReasoning = true
      assistantMessage.reasoning = ''
    }
    else {
      finishAgentReasoning(assistantMessage, streamState)
    }

    remaining = remaining.slice(nextDelimiter.index + nextDelimiter.text.length).replace(/^\n+/, '')
  }
}

function appendAgentText(assistantMessage, streamState, text) {
  if (!text) {
    return
  }

  if (streamState.isReasoning) {
    assistantMessage.reasoning += text
  }
  else {
    assistantMessage.content += text
  }
}

function finishAgentReasoning(assistantMessage, streamState) {
  if (!streamState.isReasoning) {
    assistantMessage.isReasoning = false
    return
  }

  streamState.isReasoning = false
  assistantMessage.isReasoning = false
  if (streamState.reasoningStartTime) {
    assistantMessage.reasoningTime = Math.max(1, Math.round((Date.now() - streamState.reasoningStartTime) / 1000))
  }
}

function getNextDelimiter(reasoningIndex, answerIndex) {
  const candidates = []
  if (reasoningIndex > -1) {
    candidates.push({ type: 'reasoning', index: reasoningIndex, text: reasoningDelimiter })
  }
  if (answerIndex > -1) {
    candidates.push({ type: 'answer', index: answerIndex, text: answerDelimiter })
  }
  return candidates.sort((a, b) => a.index - b.index)[0] || null
}

function stopChat() {
  if (chatAbortController) {
    chatAbortController.abort()
    chatAbortController = null
  }
  activeChatStreamId = null
  if (chatScrollFrame !== null) {
    cancelAnimationFrame(chatScrollFrame)
    chatScrollFrame = null
  }
  if (chatSending.value) {
    const last = chatMessages.value[chatMessages.value.length - 1]
    if (last) {
      last.streaming = false
      last.isReasoning = false
    }
  }
  chatSending.value = false
}

function scrollChatToBottom() {
  if (chatScrollFrame !== null) {
    return
  }
  chatScrollFrame = requestAnimationFrame(() => {
    chatScrollFrame = null
    const el = messageListRef.value
    if (el) {
      scrollLatestReasoningToBottom(el)
      el.scrollTop = el.scrollHeight
    }
  })
}

function scrollLatestReasoningToBottom(container) {
  const reasoningContents = container.querySelectorAll('.reasoning-content')
  const latestReasoning = reasoningContents[reasoningContents.length - 1]
  if (latestReasoning) {
    latestReasoning.scrollTop = latestReasoning.scrollHeight
  }
}

onMounted(() => {
  loadProviders()
  loadRoutePolicies()
  loadAgents()
})

onBeforeUnmount(() => {
  stopChat()
})
</script>

<style scoped>
.agent-console-page {
  min-height: 100%;
  padding: 24px;
  color: #0f172a;
  overflow: auto;
}

.agent-market {
  width: 100%;
  min-width: 0;
}

.agent-market :deep(.n-spin-content) {
  width: 100%;
}

.agent-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.workbench-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.page-title,
.workbench-title {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0;
}

.page-subtitle,
.workbench-subtitle,
.chat-subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.agent-filter {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 14px;
  margin-bottom: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.85);
  border-radius: 10px;
}

.filter-search {
  max-width: 420px;
}

.filter-status {
  width: 140px;
}

.agent-grid {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.agent-card {
  display: flex;
  min-height: 250px;
  flex-direction: column;
  padding: 18px;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.04);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.agent-card:hover {
  border-color: rgba(59, 130, 246, 0.35);
  box-shadow: 0 16px 38px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.agent-card:focus-visible {
  outline: 2px solid rgba(37, 99, 235, 0.45);
  outline-offset: 3px;
}

.agent-card-header,
.chat-agent,
.workbench-title-block {
  display: flex;
  align-items: center;
  gap: 12px;
}

.agent-avatar,
.chat-avatar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  border-radius: 12px;
}

.agent-card-title {
  min-width: 0;
  flex: 1;
}

.agent-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.agent-name {
  overflow: hidden;
  font-size: 16px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-code {
  margin-top: 4px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.agent-description {
  display: -webkit-box;
  min-height: 44px;
  margin: 16px 0;
  overflow: hidden;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.agent-meta-grid {
  display: grid;
  grid-template-columns: 1fr 88px;
  gap: 10px;
  margin-bottom: 14px;
}

.agent-meta-item {
  min-width: 0;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 8px;
}

.agent-meta-item span {
  display: block;
  margin-bottom: 4px;
  color: #94a3b8;
  font-size: 12px;
}

.agent-meta-item strong {
  display: block;
  overflow: hidden;
  color: #1e293b;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-chip-row {
  display: flex;
  min-height: 24px;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.agent-design-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 7px 10px;
  margin: 0 0 12px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 650;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  opacity: 0.9;
}

.muted-chip {
  color: #94a3b8;
  font-size: 12px;
}

.agent-card-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 12px;
  margin-top: auto;
  border-top: 1px solid #eef2f7;
}

.pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  margin-top: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 10px;
}

.pagination-total {
  color: #64748b;
  font-size: 12px;
}

.agent-base-modal {
  width: min(560px, calc(100vw - 32px));
}

.empty-state {
  display: flex;
  min-height: 320px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #94a3b8;
  background: #fff;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
}

.empty-state i {
  font-size: 32px;
}

.agent-workbench {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 112px);
  min-height: 640px;
  overflow: hidden;
}

.workbench-title-block {
  flex: 0 1 340px;
  min-width: 240px;
}

.workbench-control-cluster {
  display: flex;
  flex: 1 1 auto;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  min-width: 0;
}

.model-command-card {
  flex: 0 1 520px;
  width: min(520px, 100%);
  min-width: 320px;
  padding: 8px;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 16px;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
}

.model-command-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.agent-settings-pill,
.active-model-pill,
.model-settings-trigger,
.restore-button,
.publish-update-button,
.publish-action-row,
.multi-model-link {
  border: 0;
  appearance: none;
}

.agent-settings-pill,
.active-model-pill {
  display: flex;
  align-items: center;
  gap: 7px;
  height: 32px;
  min-width: 0;
  padding: 0 10px;
  color: #344054;
  font-size: 13px;
  font-weight: 650;
  background: #fff;
  border: 1px solid #e4e7ec;
  border-radius: 10px;
}

.agent-settings-pill {
  flex: 0 0 auto;
  cursor: pointer;
  transition:
    color 0.2s ease,
    background 0.2s ease,
    border-color 0.2s ease;
}

.agent-settings-pill:hover {
  color: #155eef;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.active-model-pill {
  flex: 1 1 auto;
  max-width: none;
  color: #1e293b;
  cursor: pointer;
  background: #eff6ff;
  border-color: #a4bcfd;
  transition:
    background 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.active-model-pill:hover {
  background: #e0ecff;
  border-color: #84adff;
  box-shadow: inset 0 0 0 1px rgba(21, 94, 239, 0.06);
}

.model-pill-icon,
.model-selector-icon {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  color: #155eef;
  background: #fff;
  border: 1px solid rgba(21, 94, 239, 0.16);
  border-radius: 8px;
}

.model-pill-icon {
  width: 22px;
  height: 22px;
}

.model-pill-name {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-pill-arrow {
  flex: 0 0 auto;
  color: #155eef;
  font-size: 14px;
}

.model-chat-badge,
.selector-chat-badge {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  height: 19px;
  padding: 0 6px;
  color: #155eef;
  font-size: 10px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: 0.04em;
  background: #eef4ff;
  border: 1px solid #a4bcfd;
  border-radius: 6px;
}

.model-dropdown-panel {
  padding: 2px;
}

.model-selector-card {
  display: grid;
  grid-template-columns: 34px minmax(118px, 0.82fr) 1px minmax(210px, 1.18fr) auto;
  gap: 8px;
  align-items: center;
  min-height: 48px;
  padding: 7px 8px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 13px;
}

.model-selector-icon {
  width: 34px;
  height: 34px;
  color: #475467;
  background: #fff;
  border-color: rgba(15, 23, 42, 0.06);
}

.model-selector-field {
  min-width: 0;
}

.model-selector-label {
  display: block;
  padding: 0 12px;
  margin-bottom: -2px;
  color: #98a2b3;
  font-size: 11px;
  font-weight: 650;
}

.model-selector-divider {
  width: 1px;
  height: 26px;
  background: #e4e7ec;
}

.model-provider-select,
.model-name-select {
  min-width: 0;
}

.model-provider-select :deep(.n-base-selection),
.model-name-select :deep(.n-base-selection) {
  --n-border: 0 !important;
  --n-border-hover: 0 !important;
  --n-border-active: 0 !important;
  --n-border-focus: 0 !important;
  --n-box-shadow-focus: none !important;
  background: transparent;
}

.model-provider-select :deep(.n-base-selection-label),
.model-name-select :deep(.n-base-selection-label) {
  padding: 0 12px;
  background: transparent;
}

.model-settings-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 32px;
  padding: 0 10px;
  color: #155eef;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  background: #fff;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  transition:
    background 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease;
}

.model-settings-trigger:hover {
  background: #eff6ff;
  border-color: #93c5fd;
  transform: translateY(-1px);
}

.model-param-panel {
  width: 100%;
  margin-top: 12px;
  padding: 2px 0;
}

.param-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 2px 2px 12px;
  border-bottom: 1px solid #eef2f7;
}

.param-panel-title {
  color: #101828;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.35;
}

.param-panel-subtitle {
  margin-top: 3px;
  color: #667085;
  font-size: 12px;
}

.param-config-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 0;
}

.param-config-row {
  display: grid;
  grid-template-columns: 118px minmax(170px, 1fr) 96px;
  gap: 14px;
  align-items: center;
  padding: 12px;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 12px;
}

.param-meta-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.param-title-line {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.param-title-line span:first-child {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.param-help {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  color: #98a2b3;
  font-size: 11px;
  font-weight: 800;
  background: #f2f4f7;
  border-radius: 50%;
}

.param-slider-cell {
  min-width: 0;
}

.param-number,
.token-param-input {
  width: 100%;
}

.agent-console-page :deep(.n-input-number .n-input__input-el) {
  color: #0f172a !important;
  font-weight: 650;
  text-align: left;
}

.agent-console-page :deep(.n-input-number .n-input-wrapper) {
  padding-right: 10px;
  padding-left: 10px;
}

.model-param-panel :deep(.n-input-number .n-input) {
  background: #f8fafc;
  border-radius: 10px;
}

.model-param-panel :deep(.n-slider) {
  --n-fill-color: #2563eb !important;
  --n-fill-color-hover: #155eef !important;
  --n-handle-color: #fff !important;
}

.multi-model-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 2px 0;
  color: #155eef;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  background: transparent;
}

.workbench-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.draft-save-button,
.publish-button {
  height: 50px;
  min-width: 78px;
  border-radius: 16px;
}

.draft-save-button {
  color: #344054;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.045);
}

.publish-button {
  min-width: 88px;
  font-weight: 800;
  box-shadow: 0 12px 24px rgba(21, 94, 239, 0.24);
}

.publish-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 2px;
}

.publish-status-card,
.publish-action-card {
  padding: 12px;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 16px;
}

.publish-status-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.publish-eyebrow {
  color: #98a2b3;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.3;
}

.publish-time-text {
  margin-top: 4px;
  color: #101828;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.35;
}

.restore-button {
  height: 30px;
  padding: 0 12px;
  color: #155eef;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  background: #f8fafc;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  transition:
    background 0.2s ease,
    border-color 0.2s ease;
}

.restore-button:hover {
  background: #eff6ff;
  border-color: #93c5fd;
}

.publish-update-button {
  width: 100%;
  height: 42px;
  color: #fff;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  background: #155eef;
  border-radius: 12px;
  box-shadow: 0 12px 24px rgba(21, 94, 239, 0.22);
  transition:
    background 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.publish-update-button:hover:not(:disabled) {
  background: #004eeb;
  box-shadow: 0 14px 26px rgba(21, 94, 239, 0.28);
  transform: translateY(-1px);
}

.publish-update-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.publish-action-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.publish-action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 40px;
  padding: 0 12px 0 10px;
  color: #101828;
  font-size: 13px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
  background: #f8fafc;
  border-radius: 12px;
  transition:
    background 0.2s ease,
    transform 0.2s ease;
}

.publish-action-row:hover {
  background: #eef2f7;
  transform: translateY(-1px);
}

.publish-action-left {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.publish-action-icon {
  color: #0f172a;
  font-size: 15px;
}

.publish-action-arrow {
  flex: 0 0 auto;
  color: #101828;
  font-size: 14px;
}

.workbench-body {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 0;
  overflow: hidden;
  background: #f7f8fb;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 18px;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.07);
}

.orchestration-panel,
.chat-panel {
  min-height: 0;
  height: 100%;
  background: #fff;
}

.orchestration-panel {
  display: flex;
  flex: 0 0 50%;
  width: 50%;
  min-width: 0;
  flex-direction: column;
  background: #f7f8fb;
}

.orchestration-header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 56px;
  padding: 0 22px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid rgba(226, 232, 240, 0.82);
}

.orchestration-title {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.35;
}

.orchestration-subtitle {
  margin-top: 2px;
  color: #667085;
  font-size: 12px;
}

.orchestration-scroll {
  flex: 1;
  min-height: 0;
  padding: 18px 22px 26px;
  overflow-x: hidden;
  overflow-y: auto;
}

.panel-section {
  padding: 18px;
}

.panel-card {
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.035);
}

.panel-card:last-child {
  margin-bottom: 0;
}

.prompt-card {
  background:
    linear-gradient(#fff, #fff) padding-box,
    linear-gradient(135deg, rgba(68, 76, 231, 0.25), rgba(14, 165, 233, 0.08)) border-box;
  border-color: transparent;
}

.config-entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.config-entry-card {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
  padding: 14px;
  color: inherit;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.035);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.config-entry-card:hover {
  border-color: rgba(37, 99, 235, 0.34);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.config-entry-icon {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: #155eef;
  background: #eff6ff;
  border-radius: 12px;
}

.context-entry-icon {
  color: #047857;
  background: #ecfdf3;
}

.config-entry-icon i {
  font-size: 18px;
}

.config-entry-main {
  min-width: 0;
  flex: 1;
}

.config-entry-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
}

.config-entry-desc,
.config-entry-preview,
.config-entry-empty {
  color: #667085;
  font-size: 12px;
}

.config-entry-desc {
  margin-top: 3px;
}

.config-entry-preview {
  margin-top: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.config-entry-tags {
  display: flex;
  min-height: 22px;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.config-entry-arrow {
  flex: 0 0 auto;
  color: #98a2b3;
  font-size: 18px;
}

.agent-config-modal,
.agent-context-modal {
  width: min(760px, calc(100vw - 32px));
}

.agent-context-modal {
  width: min(920px, calc(100vw - 32px));
}

.modal-section {
  padding-top: 2px;
}

.context-modal-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 2px 0 14px;
}

.context-modal-hint {
  color: #667085;
  font-size: 13px;
  line-height: 1.55;
}

.context-modal-body {
  max-height: min(62vh, 640px);
  padding-right: 4px;
  overflow-x: hidden;
  overflow-y: auto;
}

.context-modal-empty {
  margin: 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.section-title,
.section-title span {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title {
  margin-bottom: 14px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.section-title i {
  color: #2563eb;
  font-size: 18px;
}

.compact-section-title small {
  min-width: 0;
  overflow: hidden;
  color: #667085;
  font-size: 12px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.context-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.context-panel-header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 16px;
  border-bottom: 1px solid #eef2f7;
}

.context-panel-header .section-title {
  margin-bottom: 0;
}

.context-collapse-scroll {
  flex: 1;
  min-height: 0;
}

.context-collapse-wrap {
  padding: 12px;
}

.context-collapse :deep(.n-collapse-item) {
  overflow: hidden;
  margin-bottom: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.context-collapse :deep(.n-collapse-item:last-child) {
  margin-bottom: 0;
}

.context-collapse :deep(.n-collapse-item__header) {
  align-items: center;
  padding: 12px;
}

.context-collapse :deep(.n-collapse-item__header-main) {
  min-width: 0;
}

.context-collapse :deep(.n-collapse-item__content-wrapper) {
  padding: 0 12px 12px;
}

.context-collapse-title {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.context-collapse-title strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.context-collapse-title small {
  display: -webkit-box;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.context-collapse-extra {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 8px;
}

.context-form {
  padding: 12px;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 8px;
}

.context-field-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 108px 86px;
  gap: 10px;
  margin-bottom: 10px;
}

.context-field {
  min-width: 0;
}

.context-field-label {
  margin-bottom: 6px;
  color: #64748b;
  font-size: 12px;
}

.context-content-input {
  width: 100%;
}

.context-content-input :deep(textarea) {
  min-height: 156px;
}

.sort-input {
  width: 100%;
}

.context-empty {
  display: flex;
  min-height: 180px;
  align-items: center;
  justify-content: center;
  margin: 12px;
  padding: 24px;
  color: #94a3b8;
  text-align: center;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
}

.chat-panel {
  display: flex;
  flex: 1 1 50%;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
  background: #f9fafb;
  border-left: 1px solid rgba(226, 232, 240, 0.92);
}

.chat-header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  min-height: 58px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.94);
  border-bottom: 1px solid rgba(226, 232, 240, 0.82);
}

.chat-title {
  color: #111827;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.25;
}

.chat-scroll {
  min-height: 0;
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  background:
    radial-gradient(circle at 20% 0%, rgba(219, 234, 254, 0.55), transparent 34%),
    linear-gradient(180deg, #f9fafb 0%, #fff 100%);
}

.chat-message-list {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  gap: 16px;
  padding: 22px 18px 28px;
}

.chat-message {
  display: flex;
  width: fit-content;
  min-width: 0;
  gap: 10px;
  max-width: min(88%, 760px);
}

.message-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-avatar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  background: #0f172a;
  border-radius: 50%;
}

.message-assistant .message-avatar {
  background: #2563eb;
}

.message-body {
  min-width: 0;
  max-width: 100%;
}

.message-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.message-user .message-meta {
  justify-content: flex-end;
}

.reasoning-section {
  overflow: hidden;
  margin-bottom: 8px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.reasoning-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-bottom: 1px solid #e2e8f0;
}

.reasoning-header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-size: 12px;
  font-weight: 650;
}

.reasoning-header-left i {
  color: #64748b;
  font-size: 15px;
}

.reasoning-header-left small {
  color: #94a3b8;
  font-weight: 400;
}

.reasoning-content {
  max-height: 180px;
  padding: 10px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.65;
  white-space: pre-wrap;
}

.message-bubble {
  min-height: 24px;
  padding: 12px 14px;
  color: #1e293b;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.045);
}

.message-user .message-bubble {
  color: #fff;
  background: #155eef;
  border-color: #155eef;
  box-shadow: 0 10px 24px rgba(21, 94, 239, 0.22);
}

.stream-cursor {
  display: inline-block;
  width: 6px;
  height: 16px;
  margin-left: 4px;
  vertical-align: -2px;
  background: currentcolor;
  animation: blink 1s infinite;
}

.suggestion-row {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 16px 0;
  background: linear-gradient(180deg, rgba(249, 250, 251, 0), #f9fafb 44%);
}

.suggestion-chip {
  max-width: 100%;
  padding: 6px 10px;
  overflow: hidden;
  color: #2563eb;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  transition:
    background 0.2s ease,
    border-color 0.2s ease;
}

.suggestion-chip:hover {
  background: #dbeafe;
  border-color: #93c5fd;
}

.chat-composer {
  flex: 0 0 auto;
  padding: 14px 16px 16px;
  background: linear-gradient(180deg, rgba(249, 250, 251, 0.2) 0%, #f9fafb 34%, #f9fafb 100%);
  border-top: 1px solid rgba(226, 232, 240, 0.72);
}

.chat-composer :deep(.n-input) {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}

.chat-composer :deep(textarea) {
  line-height: 1.6;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.composer-status {
  color: #94a3b8;
  font-size: 12px;
}

@keyframes blink {
  0%,
  45% {
    opacity: 1;
  }
  46%,
  100% {
    opacity: 0;
  }
}

.dark .agent-console-page {
  color: #e2e8f0;
}

.dark .agent-filter,
.dark .agent-card,
.dark .pagination-wrap,
.dark .model-command-card,
.dark .model-selector-card,
.dark .agent-settings-pill,
.dark .active-model-pill,
.dark .model-settings-trigger,
.dark .param-config-row,
.dark .publish-status-card,
.dark .publish-action-card,
.dark .publish-action-row,
.dark .draft-save-button,
.dark .workbench-body,
.dark .orchestration-panel,
.dark .orchestration-header,
.dark .panel-card,
.dark .config-entry-card,
.dark .context-panel,
.dark .chat-panel,
.dark .empty-state {
  background: rgba(15, 23, 42, 0.86);
  border-color: rgba(51, 65, 85, 0.8);
}

.dark .page-subtitle,
.dark .workbench-subtitle,
.dark .chat-subtitle,
.dark .agent-code,
.dark .agent-description,
.dark .pagination-total,
.dark .composer-status,
.dark .message-meta,
.dark .muted-chip {
  color: #94a3b8;
}

.dark .agent-meta-item,
.dark .context-empty,
.dark .context-form,
.dark .reasoning-section,
.dark .context-collapse :deep(.n-collapse-item) {
  background: rgba(30, 41, 59, 0.72);
  border-color: rgba(51, 65, 85, 0.8);
}

.dark .agent-meta-item strong,
.dark .context-collapse-title strong,
.dark .param-title-line span:first-child,
.dark .param-panel-title,
.dark .model-pill-name,
.dark .publish-time-text,
.dark .publish-action-row,
.dark .publish-action-icon,
.dark .publish-action-arrow,
.dark .orchestration-title,
.dark .chat-title,
.dark .config-entry-title,
.dark .section-title,
.dark .message-bubble {
  color: #e2e8f0;
}

.dark .restore-button {
  color: #93c5fd;
  background: rgba(37, 99, 235, 0.16);
  border-color: rgba(147, 197, 253, 0.35);
}

.dark .panel-section,
.dark .orchestration-header,
.dark .context-panel-header,
.dark .chat-header,
.dark .chat-composer,
.dark .reasoning-header,
.dark .suggestion-row,
.dark .agent-card-footer {
  border-color: rgba(51, 65, 85, 0.8);
}

.dark .param-panel-header {
  border-color: rgba(51, 65, 85, 0.8);
}

.dark .context-collapse-title small,
.dark .context-field-label,
.dark .orchestration-subtitle,
.dark .compact-section-title small,
.dark .config-entry-desc,
.dark .config-entry-preview,
.dark .config-entry-empty,
.dark .context-modal-hint,
.dark .reasoning-content,
.dark .param-panel-subtitle,
.dark .model-selector-label,
.dark .publish-eyebrow {
  color: #94a3b8;
}

.dark .model-selector-divider {
  background: rgba(51, 65, 85, 0.9);
}

.dark .model-pill-icon,
.dark .model-selector-icon {
  color: #93c5fd;
  background: rgba(30, 41, 59, 0.92);
  border-color: rgba(51, 65, 85, 0.9);
}

.dark .model-param-panel :deep(.n-input-number .n-input) {
  background: rgba(30, 41, 59, 0.92);
}

.dark .agent-console-page :deep(.n-input-number .n-input__input-el) {
  color: #e2e8f0 !important;
}

.dark .config-entry-icon {
  color: #93c5fd;
  background: rgba(37, 99, 235, 0.18);
}

.dark .context-entry-icon {
  color: #86efac;
  background: rgba(5, 150, 105, 0.18);
}

.dark .agent-design-hint {
  color: #93c5fd;
  background: rgba(37, 99, 235, 0.16);
  border-color: rgba(59, 130, 246, 0.28);
}

.dark .chat-scroll {
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.75) 0%, rgba(15, 23, 42, 0.92) 100%);
}

.dark .message-bubble {
  background: rgba(30, 41, 59, 0.92);
  border-color: rgba(51, 65, 85, 0.9);
}

.dark .chat-composer :deep(.n-input) {
  background: rgba(30, 41, 59, 0.92);
}

@media (min-width: 1580px) {
  .agent-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1380px) {
  .workbench-header {
    flex-wrap: wrap;
  }

  .workbench-control-cluster {
    flex: 1 1 100%;
  }
}

@media (max-width: 1180px) {
  .agent-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .agent-workbench {
    height: auto;
    min-height: 0;
    overflow: visible;
  }

  .model-command-card {
    min-width: 0;
    width: 100%;
  }

  .workbench-body {
    flex-direction: column;
    height: auto;
    overflow: visible;
  }

  .orchestration-panel,
  .chat-panel {
    flex: none;
    width: 100%;
    height: auto;
    max-height: none;
  }

  .orchestration-scroll {
    overflow: visible;
  }

  .chat-panel {
    min-height: 640px;
    border-top: 1px solid rgba(226, 232, 240, 0.92);
    border-left: 0;
  }

  .chat-scroll {
    min-height: 380px;
  }
}

@media (max-width: 760px) {
  .agent-console-page {
    padding: 14px;
  }

  .agent-toolbar,
  .workbench-header,
  .agent-filter {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-search,
  .filter-status {
    width: 100%;
    max-width: none;
  }

  .agent-grid,
  .agent-meta-grid,
  .config-entry-grid,
  .model-selector-card,
  .param-config-row,
  .context-field-grid {
    grid-template-columns: 1fr;
  }

  .workbench-control-cluster {
    flex-direction: column;
  }

  .model-command-card {
    min-width: 0;
  }

  .model-command-tabs {
    align-items: stretch;
    flex-direction: column;
  }

  .active-model-pill {
    max-width: none;
  }

  .model-selector-divider {
    display: none;
  }

  .model-param-panel {
    width: 100%;
  }

  .context-collapse-extra {
    gap: 6px;
  }

  .orchestration-header,
  .chat-header,
  .context-modal-toolbar {
    align-items: stretch;
    height: auto;
    flex-direction: column;
  }

  .orchestration-scroll,
  .chat-message-list {
    padding: 14px;
  }

  .panel-section {
    padding: 14px;
  }

  .workbench-actions,
  .composer-actions {
    justify-content: flex-end;
  }

  .pagination-wrap {
    align-items: stretch;
    flex-direction: column;
  }

  .chat-message {
    max-width: 100%;
  }
}
</style>
