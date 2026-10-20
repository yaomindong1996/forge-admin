<template>
  <div class="model-design-page">
    <!-- 顶部工具栏 -->
    <FlowDesignToolbar
      :name="modelInfo.modelName"
      :status="statusTag"
      :mode="workspaceMode"
      :embedded="embedded"
      :ai-active="isAiPanelActive"
      :saving="saving"
      :deploying="deploying"
      @back="handleBack"
      @mode="setWorkspaceMode"
      @history="handleOpenVersionHistory"
      @ai="toggleAiPanel"
      @save="handleSaveDraft"
      @deploy="handleDeploy"
    />

    <div v-if="businessContextActive" class="business-context-banner">
      <div class="business-context-banner__main">
        <i class="i-material-symbols:domain" />
        <span>当前流程已绑定业务对象</span>
        <strong>{{ businessContextName }}</strong>
      </div>
      <n-button text type="primary" @click="goBackToBusinessApp">
        返回业务应用
      </n-button>
    </div>

    <!-- 主体内容 -->
    <div class="main-content" :class="{ 'is-settings': workspaceMode === 'settings' }">
      <!-- 中间区域（画布） -->
      <div v-if="workspaceMode === 'design'" class="center-area">
        <!-- 流程设计器 -->
        <div class="designer-container">
          <DingFlowDesigner
            v-if="isApprovalDesigner"
            :ref="el => bindElementRef('modelerRef', el)"
            :key="designerRenderKey"
            :xml="bpmnXml"
            :process-name="modelInfo.modelName"
            :form-asset-options="nodeFormAssetOptions"
            :form-field-catalog="formFieldCatalog"
            :auto-bind-business-form="businessContextActive"
            :default-form-key="businessGlobalFormNode.formKey || businessFormKey"
            :process-config="processConfig"
            @change="handleBpmnChange"
            @ready="handleModelerReady"
            @import-start="handleDiagramImportStart"
            @import-end="handleDiagramImportEnd"
          />
          <FlowModeler
            v-else
            :ref="el => bindElementRef('modelerRef', el)"
            :key="designerRenderKey"
            :xml="bpmnXml"
            @change="handleBpmnChange"
            @ready="handleModelerReady"
            @selection-change="handleBusinessElementSelect"
            @import-start="handleDiagramImportStart"
            @import-end="handleDiagramImportEnd"
          />
          <Transition name="designer-loading-fade">
            <div v-if="designerLoading" class="designer-loading-mask">
              <n-spin size="large">
                <template #description>
                  {{ pageLoading ? '正在加载流程数据...' : '正在渲染流程图...' }}
                </template>
              </n-spin>
            </div>
          </Transition>
          <Transition name="designer-loading-fade">
            <div v-if="aiGeneratingCanvasHintVisible" class="ai-generating-mask">
              <div class="ai-generating-panel">
                <div class="ai-generating-badge">
                  <i class="ai-generating-icon i-material-symbols:auto-awesome" />
                </div>
                <div class="ai-generating-main">
                  <div class="ai-generating-title">
                    AI 正在生成流程图
                  </div>
                  <div class="ai-generating-desc">
                    {{ aiCanvasHintText }}
                  </div>
                </div>
                <div class="ai-generating-stage-list">
                  <div
                    v-for="(stage, idx) in aiStages"
                    :key="stage.key"
                    class="ai-generating-stage"
                    :class="{
                      done: idx < currentAiStageIndex,
                      active: idx === currentAiStageIndex,
                    }"
                  >
                    <span class="ai-generating-stage-dot" />
                    <span>{{ stage.label }}</span>
                  </div>
                </div>
              </div>
            </div>
          </Transition>
        </div>
        <FlowPropertyPanelShell
          v-show="isBusinessDesigner && dockedElement"
          class="business-properties-panel"
          :title="businessPanelTitle"
          description="BPMN 元素属性"
          :icon="businessPanelIcon"
          @close="handleBusinessPanelClose"
        >
          <NodePropertiesPanel
            v-if="dockedElement && modelerInstance"
            :element="dockedElement"
            :modeler="modelerInstance"
            :field-catalog="formFieldCatalog"
            class="business-properties-panel__body"
            @update="handleBpmnChange"
          />
        </FlowPropertyPanelShell>
      </div>

      <div v-else class="settings-workspace">
        <div class="settings-detail-pane">
          <div v-if="rightActiveTab === 'flow'" class="settings-section-pane">
            <div class="settings-pane-header">
              <div>
                <div class="settings-pane-title">
                  流程属性
                </div>
                <div class="settings-pane-desc">
                  设置流程归属、业务类型和可读说明。
                </div>
              </div>
              <NTag :type="statusTag.type" size="small">
                {{ statusTag.label }}
              </NTag>
              <NTag :type="isBusinessDesigner ? 'success' : 'info'" size="small" :bordered="false">
                {{ designerTypeLabel }}
              </NTag>
            </div>

            <label class="settings-field">
              <span class="settings-field-label">流程名称</span>
              <n-input
                v-model:value="modelInfo.modelName"
                placeholder="请输入流程名称"
                size="small"
                :disabled="isReadonly"
              />
            </label>
            <label class="settings-field">
              <span class="settings-field-label">流程编码</span>
              <n-input
                v-model:value="modelInfo.modelKey"
                placeholder="流程编码"
                size="small"
                disabled
              />
            </label>
            <label class="settings-field">
              <span class="settings-field-label">流程分类</span>
              <NTreeSelect
                v-model:value="modelInfo.category"
                :options="categoryTreeOptions"
                placeholder="选择分类"
                size="small"
                :default-expand-all="true"
              />
            </label>
            <label class="settings-field">
              <span class="settings-field-label">流程说明</span>
              <n-input
                v-model:value="modelInfo.description"
                type="textarea"
                :autosize="{ minRows: 3, maxRows: 5 }"
                placeholder="描述流程用途"
                size="small"
              />
            </label>
          </div>

          <div v-else-if="rightActiveTab === 'form'" class="settings-section-pane">
            <div class="settings-pane-header">
              <div>
                <div class="settings-pane-title">
                  表单配置
                </div>
                <div class="settings-pane-desc">
                  配置流程发起时填写的全局表单。
                </div>
              </div>
              <NTag size="small" :type="formConfigStatus.type" :bordered="false">
                {{ formConfigStatus.label }}
              </NTag>
            </div>

            <template v-if="businessFormConfigActive">
              <label v-if="modelInfo.id" class="settings-field">
                <span class="settings-field-label">表单类型</span>
                <n-select
                  v-model:value="modelInfo.formType"
                  :options="businessManagedFormTypeOptions"
                  size="small"
                  @update:value="handleBusinessManagedFormTypeChange"
                />
              </label>
              <label v-if="businessApplicationPickerVisible" class="settings-field">
                <span class="settings-field-label">业务应用</span>
                <n-select
                  :value="effectiveApplicationId || null"
                  :options="businessApplicationOptions"
                  :loading="businessApplicationLoading"
                  placeholder="先选择业务应用"
                  filterable
                  clearable
                  size="small"
                  @focus="loadBusinessApplicationOptions"
                  @update:value="handleBusinessApplicationChange"
                />
              </label>
              <label v-if="businessObjectPickerVisible" class="settings-field">
                <span class="settings-field-label">业务对象</span>
                <n-select
                  :value="businessObjectCode || null"
                  :options="businessObjectOptions"
                  :loading="businessObjectLoading"
                  :disabled="!effectiveApplicationId"
                  :placeholder="effectiveApplicationId ? '选择当前应用的业务对象' : '请先选择业务应用'"
                  filterable
                  clearable
                  size="small"
                  @focus="loadBusinessObjectOptions"
                  @update:value="handleBusinessObjectChange"
                />
              </label>
              <div class="settings-field">
                <span class="settings-field-label">应用表单资产</span>
                <BusinessFlowFormAssetSelect
                  v-if="appManagedFormTypeActive && businessContextActive && effectiveApplicationId"
                  :node-form="businessGlobalFormNode"
                  :form-assets="nodeFormAssetOptions"
                  show-all-modes
                  @update="handleBusinessGlobalFormUpdate"
                />
                <n-empty v-else-if="appManagedFormTypeActive" size="small">
                  <template #description>
                    {{ !effectiveApplicationId ? '请先选择业务应用' : '请先选择业务对象，再选择应用管理中维护的表单' }}
                  </template>
                </n-empty>
                <n-empty
                  v-else
                  size="small"
                  description="当前流程不使用发起表单"
                />
              </div>
              <label v-if="modelInfo.formType === 'external'" class="settings-field">
                <span class="settings-field-label">外置表单</span>
                <n-input
                  v-model:value="modelInfo.formUrl"
                  placeholder="/views/leave/apply"
                  size="small"
                  @blur="refreshFormFieldCatalog"
                />
              </label>
              <div v-if="formFieldCatalogError" class="settings-tip">
                {{ formFieldCatalogError }}
              </div>
              <div v-if="appManagedFormTypeActive" class="settings-tip">
                表单来源于应用管理的表单设计；流程侧只选择表单资产，节点字段权限在节点抽屉维护。
              </div>
            </template>
            <template v-else>
              <label v-if="modelInfo.id" class="settings-field">
                <span class="settings-field-label">表单类型</span>
                <n-select
                  v-model:value="modelInfo.formType"
                  :options="formTypeOptions"
                  size="small"
                  @update:value="handleFormTypeChange"
                />
              </label>
              <label v-if="modelInfo.formType === 'dynamic'" class="settings-field">
                <span class="settings-field-label">已有表单</span>
                <n-select
                  v-model:value="modelInfo.formId"
                  :options="formOptions"
                  placeholder="不选择则使用模型内表单"
                  clearable
                  size="small"
                  @update:value="handleFormSelect"
                />
              </label>
              <label v-if="modelInfo.formType === 'external'" class="settings-field">
                <span class="settings-field-label">外置表单</span>
                <n-input
                  v-model:value="modelInfo.formUrl"
                  placeholder="/views/leave/apply"
                  size="small"
                  @blur="refreshFormFieldCatalog"
                />
              </label>
            </template>
            <div class="settings-tip">
              {{ businessFormConfigActive ? '节点未单独选择表单时继承这里的应用表单资产。' : '发起节点不再单独配置表单；这里的表单会作为流程全局发起表单使用。' }}
            </div>
          </div>

          <div v-else-if="rightActiveTab === 'approval'" class="settings-section-pane">
            <div class="settings-pane-header">
              <div>
                <div class="settings-pane-title">
                  审批与待办
                </div>
                <div class="settings-pane-desc">
                  控制审批退回、提交人撤回、待办跳转，以及同一审批人重复出现时的自动处理规则。
                </div>
              </div>
              <NTag size="small" :type="modelInfo.autoApprovalMode === 'none' ? 'default' : 'info'" :bordered="false">
                {{ autoApprovalModeLabel }}
              </NTag>
            </div>

            <div class="approval-setting-row">
              <div class="approval-setting-main">
                <div class="approval-setting-title">
                  提交人权限
                </div>
                <div class="approval-setting-desc">
                  允许提交人撤销审批中的申请。
                </div>
              </div>
              <n-switch v-model:value="modelInfo.allowSubmitterWithdraw" />
            </div>

            <div class="approval-setting-block">
              <div class="approval-setting-title">
                驳回策略
              </div>
              <div class="approval-setting-desc">
                审批人点击普通“驳回”时按此策略处理。节点单独开启“退回发起人修改”后，该动作始终进入发起人修改节点，不受这里的结束策略影响。
              </div>
              <n-radio-group v-model:value="modelInfo.rejectStrategy" name="rejectStrategy">
                <n-space vertical :size="8">
                  <n-radio value="TO_INITIATOR_MODIFY">
                    普通驳回退回发起人修改（推荐）
                  </n-radio>
                  <n-radio value="TO_END">
                    驳回即结束流程
                  </n-radio>
                  <n-radio value="MANUAL">
                    按流程图设计（不自动补）
                  </n-radio>
                </n-space>
              </n-radio-group>
            </div>

            <div class="approval-setting-row">
              <div class="approval-setting-main">
                <div class="approval-setting-title">
                  指定节点驳回
                </div>
                <div class="approval-setting-desc">
                  开启后，审批人点“驳回”再选择回到哪个已审批节点，流程从该节点继续。关闭后，“驳回”仍按原驳回语义处理。节点上不必再单独配置“允许退回”。
                </div>
              </div>
              <n-switch v-model:value="modelInfo.allowMultiReturn">
                <template #checked>
                  开启
                </template>
                <template #unchecked>
                  关闭
                </template>
              </n-switch>
            </div>

            <div class="approval-setting-block">
              <div class="approval-setting-title">
                待办跳转
              </div>
              <div class="approval-setting-desc">
                配置企业协同待办卡片点击后的落地地址；留空时使用系统默认待办详情页。
              </div>
              <n-input
                v-model:value="modelInfo.todoDetailUrlTemplate"
                size="small"
                clearable
                :placeholder="DEFAULT_TODO_DETAIL_URL_TEMPLATE"
              />
              <div class="approval-setting-desc">
                支持占位符 {taskId}、{businessKey}、{processInstanceId}，相对路径会自动拼接连接的 H5 域名。
              </div>
            </div>

            <div class="approval-setting-block">
              <div class="approval-setting-title">
                自动审批
              </div>
              <div class="approval-setting-desc">
                当同一审批人在流程中重复出现时，按以下策略处理后续节点。
              </div>
              <div class="approval-mode-list">
                <button
                  v-for="option in autoApprovalModeOptions"
                  :key="option.value"
                  type="button"
                  class="approval-mode-option"
                  :class="{ active: modelInfo.autoApprovalMode === option.value }"
                  @click="modelInfo.autoApprovalMode = option.value"
                >
                  <span class="approval-mode-check">
                    <i v-if="modelInfo.autoApprovalMode === option.value" class="i-material-symbols:check-small" />
                  </span>
                  <span class="approval-mode-main">
                    <span class="approval-mode-title">{{ option.label }}</span>
                    <span class="approval-mode-desc">{{ option.desc }}</span>
                  </span>
                </button>
              </div>
            </div>
          </div>

          <div v-else-if="rightActiveTab === 'notification'" class="settings-section-pane notification-settings-pane">
            <div class="settings-pane-header">
              <div>
                <div class="settings-pane-title">
                  通知与推送
                </div>
                <div class="settings-pane-desc">
                  按流程配置通知事件、发送渠道和消息模板；未配置时沿用系统默认通知行为。
                </div>
              </div>
              <NTag size="small" :type="notificationConfigStatus.type" :bordered="false">
                {{ notificationConfigStatus.label }}
              </NTag>
            </div>

            <n-alert type="info" :show-icon="false">
              站内信是新待办的基础通知，模板可在消息中心维护；这里显示的预览使用示例数据。
            </n-alert>

            <n-spin :show="notifyChannelsLoading || notificationTemplatesLoading">
              <div class="notify-matrix-card">
                <div class="notify-matrix-header">
                  <span>事件 × 渠道</span>
                  <n-space>
                    <n-button text size="small" :loading="notificationTemplatesLoading" @click="loadNotificationMetadata(true)">
                      <template #icon>
                        <i class="i-material-symbols:refresh" />
                      </template>
                      刷新渠道和模板
                    </n-button>
                  </n-space>
                </div>
                <div class="notify-matrix">
                  <div
                    v-for="event in notifyEventDefinitions"
                    :key="event.key"
                    class="notify-matrix-row"
                  >
                    <div class="notify-event-copy">
                      <div class="notify-event-label">
                        {{ event.label }}
                      </div>
                      <div class="notify-event-description">
                        {{ event.description }}
                      </div>
                    </div>
                    <div class="notify-event-config">
                      <n-checkbox-group
                        :value="notifyMatrix[event.key]"
                        class="notify-channel-group"
                        @update:value="value => updateNotifyChannels(event.key, value)"
                      >
                        <n-space :size="12" :wrap-item="true">
                          <n-checkbox
                            v-for="channel in channelsForEvent(event.key)"
                            :key="channel.channel"
                            :value="channel.channel"
                            :disabled="event.key === 'todo' && channel.alwaysOn === true"
                          >
                            {{ channel.name }}
                            <n-tooltip v-if="channel.costWarning" trigger="hover" placement="top">
                              <template #trigger>
                                <i class="i-material-symbols:warning-outline ml-1 cursor-help text-warning" />
                              </template>
                              短信按条计费，请确认已配置短信通道后再启用
                            </n-tooltip>
                          </n-checkbox>
                        </n-space>
                      </n-checkbox-group>
                      <div class="notify-template-row">
                        <span class="notify-template-label">消息模板</span>
                        <n-select
                          :value="selectedNotificationTemplateValue(event.key)"
                          :options="notificationTemplateOptions(event.key)"
                          :loading="notificationTemplatesLoading"
                          :disabled="!notifyMatrix[event.key].length"
                          size="small"
                          placeholder="使用系统默认模板"
                          @update:value="value => updateNotificationTemplate(event.key, value)"
                        />
                        <n-button
                          size="small"
                          secondary
                          :disabled="!selectedNotificationTemplateValue(event.key)"
                          @click="previewNotificationTemplate(event.key)"
                        >
                          <template #icon>
                            <i class="i-material-symbols:visibility-outline" />
                          </template>
                          预览
                        </n-button>
                      </div>
                      <div class="notify-template-current">
                        当前模板：{{ notificationTemplateSummary(event.key) }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </n-spin>

            <n-alert v-if="notifySmsSelected" type="warning" :show-icon="false" class="notify-cost-alert">
              当前配置包含短信渠道，流程每次触发都会按短信服务商规则产生费用。
            </n-alert>
            <div v-if="!notifyChannelsLoading && !notifyChannels.length" class="notify-empty-tip">
              暂时无法读取可用渠道，站内信仍会作为基础通知保留；保存后可刷新重试。
            </div>
          </div>

          <div v-else-if="rightActiveTab === 'description'" class="settings-section-pane">
            <div class="settings-pane-header">
              <div>
                <div class="settings-pane-title">
                  说明
                </div>
                <div class="settings-pane-desc">
                  给后续维护者留下流程用途、范围和特殊规则。
                </div>
              </div>
            </div>
            <label class="settings-field">
              <span class="settings-field-label">流程说明</span>
              <n-input
                v-model:value="modelInfo.description"
                type="textarea"
                :autosize="{ minRows: 8, maxRows: 12 }"
                placeholder="例如：适用于正式员工请假，超过 3 天需 HR 复核。"
              />
            </label>
          </div>

          <!-- AI流程助手面板 -->
          <div v-else-if="rightActiveTab === 'ai'" class="ai-flow-panel">
            <div class="ai-flow-header">
              <div class="ai-flow-header-info">
                <div class="ai-flow-title">
                  <i class="ai-flow-title-icon i-material-symbols:auto-awesome" />
                  AI流程助手
                </div>
                <div class="ai-flow-subtitle">
                  自然语言生成或修改当前流程图
                </div>
              </div>
            </div>

            <!-- 生成阶段进度条 -->
            <div v-if="aiSending && currentAiStageIndex >= 0" class="ai-stage-progress">
              <div
                v-for="(stage, idx) in aiStages"
                :key="stage.key"
                class="ai-stage-step" :class="[{
                  done: idx < currentAiStageIndex,
                  active: idx === currentAiStageIndex,
                }]"
              >
                <div class="ai-stage-step-dot" />
                <span class="ai-stage-step-label">{{ stage.label }}</span>
              </div>
            </div>

            <div :ref="el => bindElementRef('aiMessageListRef', el)" class="ai-flow-body">
              <div v-if="aiMessages.length === 0" class="ai-flow-empty">
                <div class="ai-empty-hero">
                  <i class="ai-empty-icon i-material-symbols:auto-awesome" />
                  <div class="ai-empty-title">
                    描述你的流程需求
                  </div>
                  <div class="ai-empty-tip">
                    AI 将自动生成完整的 BPMN 流程图
                  </div>
                </div>
                <div class="ai-example-list">
                  <div
                    v-for="example in aiExamples"
                    :key="example.label"
                    class="ai-example"
                    @click="aiPrompt = example.text"
                  >
                    <div class="ai-example-icon">
                      <i class="i-material-symbols:add-circle-outline" />
                    </div>
                    <div class="ai-example-text">
                      <span class="ai-example-label">{{ example.label }}</span>
                      <span class="ai-example-desc">{{ example.text }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="ai-message-list">
                <div
                  v-for="(msg, index) in aiMessages"
                  :key="index"
                  class="ai-message"
                  :class="msg.role"
                >
                  <div class="ai-message-avatar">
                    <div v-if="msg.role === 'user'" class="avatar user-avatar">
                      我
                    </div>
                    <div v-else class="avatar ai-avatar">
                      AI
                    </div>
                  </div>
                  <div class="ai-message-body">
                    <div v-if="msg.reasoning && msg.reasoning.trim()" class="reasoning-section">
                      <div class="reasoning-header" @click="toggleReasoning(index)">
                        <div class="reasoning-header-left">
                          <i class="reasoning-icon i-material-symbols:psychology" />
                          <span class="reasoning-label">思考过程</span>
                          <span v-if="msg.reasoningTime" class="reasoning-duration">用时 {{ msg.reasoningTime }}s</span>
                          <span v-else-if="msg.isReasoning" class="reasoning-duration thinking">思考中...</span>
                        </div>
                        <i
                          class="reasoning-toggle i-material-symbols:expand-more"
                          :class="{ expanded: expandedReasonings[index] }"
                        />
                      </div>
                      <div
                        v-if="expandedReasonings[index]"
                        :ref="el => setReasoningContentRef(el, index)"
                        class="reasoning-content"
                      >
                        {{ msg.reasoning }}
                      </div>
                    </div>
                    <div class="ai-message-content">
                      <div class="ai-message-text">
                        {{ msg.content }}
                      </div>
                      <div v-if="msg.streaming && msg.isReasoning" class="message-typing">
                        <span /><span /><span />
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="aiDraft" class="ai-draft">
                <div class="ai-draft-title">
                  <span>{{ aiDraft.modelName || modelInfo.modelName || '流程草稿' }}</span>
                  <NTag size="small" type="success">
                    可加载
                  </NTag>
                </div>
                <div class="ai-draft-desc">
                  {{ aiDraft.summary || aiDraft.description || '已生成 BPMN 流程配置' }}
                </div>
                <n-space>
                  <n-button size="small" type="primary" @click="handleApplyAiDraft">
                    <template #icon>
                      <i class="i-material-symbols:download-done" />
                    </template>
                    一键加载
                  </n-button>
                  <n-button size="small" @click="handlePreviewAiXml">
                    <template #icon>
                      <i class="i-material-symbols:code" />
                    </template>
                    预览XML
                  </n-button>
                </n-space>
              </div>
              <div :ref="el => bindElementRef('aiMessageEndRef', el)" class="ai-message-end" />
            </div>

            <div class="ai-flow-input">
              <n-input
                v-model:value="aiPrompt"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 5 }"
                placeholder="例如：生成一个请假审批流程，3天以内直属上级审批，超过3天增加HR审批"
                :disabled="aiSending"
                @keydown.ctrl.enter.prevent="handleAiSend"
                @keydown.meta.enter.prevent="handleAiSend"
              />
              <div class="ai-flow-actions">
                <div class="ai-actions-left">
                  <n-popover
                    v-model:show="showModelPanel"
                    trigger="click"
                    placement="top-start"
                    :show-arrow="false"
                    :width="300"
                    raw
                  >
                    <template #trigger>
                      <button
                        type="button"
                        class="model-trigger" :class="[{ active: showModelPanel, empty: !aiModelId }]"
                        :title="aiModelId ? `${currentProviderLabel} · ${currentModelLabel}` : '请选择对话模型'"
                      >
                        <i class="i-material-symbols:sparkles model-trigger-icon" />
                        <span class="model-trigger-provider">{{ currentProviderLabel }}</span>
                        <span class="model-trigger-divider">·</span>
                        <span class="model-trigger-model">{{ currentModelLabel }}</span>
                        <i class="model-trigger-chevron i-material-symbols:expand-more" />
                      </button>
                    </template>

                    <div class="model-panel">
                      <div class="model-panel-section">
                        <div class="model-panel-label">
                          <span>供应商</span>
                          <span v-if="providerOptions.length === 0" class="model-panel-empty-tip">暂无可用供应商</span>
                        </div>
                        <n-select
                          v-model:value="aiProviderId"
                          :options="providerOptions"
                          placeholder="选择供应商"
                          size="small"
                          filterable
                        />
                      </div>
                      <div class="model-panel-section">
                        <div class="model-panel-label">
                          <span>模型</span>
                          <span v-if="modelOptions.length > 0" class="model-panel-count">{{ modelOptions.length }} 个</span>
                        </div>
                        <div v-if="modelOptions.length === 0" class="model-panel-empty">
                          {{ aiProviderId ? '该供应商暂无可用模型' : '请先选择供应商' }}
                        </div>
                        <div v-else class="model-list">
                          <div
                            v-for="m in modelOptions"
                            :key="m.value"
                            class="model-list-item" :class="[{ active: aiModelId === m.value }]"
                            @click="aiModelId = m.value; showModelPanel = false"
                          >
                            <div class="model-list-item-main">
                              <span class="model-list-item-name">{{ m.modelCode || m.label }}</span>
                              <span v-if="m.isDefault === '1'" class="model-tag">默认</span>
                            </div>
                            <div v-if="m.label && m.label !== m.modelCode" class="model-list-item-desc">
                              {{ m.label }}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </n-popover>
                  <n-button size="small" text @click="handleNewAiSession">
                    新对话
                  </n-button>
                </div>
                <div class="ai-actions-right">
                  <n-button v-if="aiSending" type="error" size="small" @click="handleAbortAi">
                    <template #icon>
                      <i class="i-material-symbols:stop-circle" />
                    </template>
                    停止
                  </n-button>
                  <n-button v-else size="small" type="primary" :disabled="!aiPrompt.trim() || !aiModelId" @click="handleAiSend">
                    <template #icon>
                      <i class="i-material-symbols:send" />
                    </template>
                    发送
                  </n-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <aside class="settings-tree-nav">
          <div
            v-for="group in settingsTreeGroups"
            :key="group.label"
            class="settings-tree-group"
          >
            <div class="settings-tree-group-title">
              {{ group.label }}
            </div>
            <button
              v-for="item in group.children"
              :key="item.key"
              type="button"
              class="settings-tree-item"
              :class="{ active: rightActiveTab === item.key }"
              @click="openSettingsPanel(item.key)"
            >
              <i :class="item.icon" />
              <span class="settings-tree-item-main">
                <span class="settings-tree-item-title">{{ item.label }}</span>
                <span class="settings-tree-item-desc">{{ item.desc }}</span>
              </span>
              <span
                v-if="getSettingsBadge(item.key)"
                class="settings-tree-badge"
                :class="getSettingsBadgeClass(item.key)"
              >
                {{ getSettingsBadge(item.key) }}
              </span>
            </button>
          </div>
        </aside>
      </div>
    </div>

    <!-- 表单设计器弹窗 -->
    <Teleport to="body">
      <n-modal
        v-model:show="showFormDesigner"
        preset="card"
        title="表单设计器"
        style="width: 95vw; height: 90vh"
        content-style="height: calc(90vh - 58px); padding: 0; overflow: hidden;"
        :mask-closable="false"
      >
        <div class="flow-form-designer-modal-body">
          <FlowFormCreateDesigner
            :ref="el => bindElementRef('formDesignerRef', el)"
            v-model="formSchema"
            height="100%"
            @save="handleSaveFormSchema"
          />
        </div>
      </n-modal>
    </Teleport>

    <!-- 表单预览弹窗 -->
    <Teleport to="body">
      <n-modal
        v-model:show="showFormPreview"
        preset="card"
        title="表单预览"
        style="width: 800px"
      >
        <FlowFormCreateRenderer
          v-if="formSchema.length > 0"
          :schema="formSchema"
          read-only
        />
      </n-modal>
    </Teleport>

    <!-- 通知模板预览弹窗 -->
    <Teleport to="body">
      <n-modal
        v-model:show="showNotificationTemplatePreview"
        preset="card"
        :title="notificationPreview?.templateName || '通知模板预览'"
        style="width: min(620px, 92vw)"
      >
        <div v-if="notificationPreview" class="notification-template-preview">
          <div class="notification-template-preview-code">
            {{ notificationPreview.templateCode }}
          </div>
          <div class="notification-template-preview-title">
            {{ renderNotificationTemplate(notificationPreview.titleTemplate) || '通知标题' }}
          </div>
          <div
            class="notification-template-preview-content"
            v-html="sanitizeHtml(renderNotificationTemplate(notificationPreview.contentTemplate) || '暂无模板内容')"
          />
          <div class="notification-template-preview-tip">
            预览变量：流程名称「{{ modelInfo.modelName || '示例流程' }}」、发起人「张三」、结果「已通过」
          </div>
        </div>
      </n-modal>
    </Teleport>

    <Teleport to="body">
      <n-modal
        v-model:show="showAiXmlPreview"
        preset="card"
        title="AI生成的 BPMN XML"
        style="width: min(900px, 92vw); max-height: 80vh"
        content-style="overflow: hidden;"
      >
        <div class="xml-preview-container">
          <n-code
            :code="aiDraft?.bpmnXml || ''"
            language="xml"
            :show-line-numbers="true"
            :word-wrap="true"
          />
        </div>
      </n-modal>
    </Teleport>

    <VersionHistory
      v-if="showVersionHistory"
      :model-id="modelInfo.id"
      :current-version="modelInfo.version"
      @close="showVersionHistory = false"
      @refresh="handleVersionHistoryRefresh"
    />
  </div>
</template>

<script>
import { useFlowDesign } from './composables/useFlowDesign'
import { flowDesignLocalComponents } from './flowDesignLocalComponents'

export default {
  name: 'FlowDesignPage',
  components: {
    ...flowDesignLocalComponents,
  },
  props: {
    embedded: {
      type: Boolean,
      default: false,
    },
    modelId: {
      type: [String, Number],
      default: '',
    },
    businessObjectCode: {
      type: String,
      default: '',
    },
    businessObjectName: {
      type: String,
      default: '',
    },
    businessFormKey: {
      type: String,
      default: '',
    },
    applicationId: {
      type: [String, Number],
      default: '',
    },
    businessEntryRoute: {
      type: String,
      default: '',
    },
    codeApp: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['close', 'saved', 'deployed'],
  setup(props, { emit }) {
    const state = useFlowDesign(props, emit)
    return {
      ...state,
      // Options 薄壳显式把实例回写 composable，便于跟踪和静态检查。
      bindElementRef(key, element) {
        state[key].value = element
      },
    }
  },
}
</script>

<style scoped src="./design.css"></style>
