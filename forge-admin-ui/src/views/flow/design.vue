<template>
  <div class="model-design-page">
    <!-- 顶部工具栏 -->
    <div class="top-bar">
      <div class="top-bar-left">
        <n-button text @click="handleBack">
          <template #icon>
            <i :class="embedded ? 'i-material-symbols:close' : 'i-material-symbols:arrow-back'" />
          </template>
          {{ embedded ? '关闭' : '返回' }}
        </n-button>
        <n-divider vertical />
        <div class="workspace-tabs">
          <button
            type="button"
            class="workspace-tab"
            :class="{ active: workspaceMode === 'design' }"
            @click="setWorkspaceMode('design')"
          >
            <i class="i-material-symbols:account-tree-outline" />
            <span>流程设计</span>
          </button>
          <button
            type="button"
            class="workspace-tab"
            :class="{ active: workspaceMode === 'settings' }"
            @click="setWorkspaceMode('settings')"
          >
            <i class="i-material-symbols:tune" />
            <span>更多设置</span>
          </button>
        </div>
      </div>
      <div class="top-bar-right">
        <n-button @click="handleOpenVersionHistory">
          <template #icon>
            <i class="i-material-symbols:history" />
          </template>
          更改记录
        </n-button>
        <n-button :type="isAiPanelActive ? 'primary' : 'default'" @click="toggleAiPanel">
          <template #icon>
            <i class="i-material-symbols:auto-awesome" />
          </template>
          AI生成
        </n-button>
        <n-button :loading="saving" @click="handleSaveDraft">
          <template #icon>
            <i class="i-material-symbols:save" />
          </template>
          保存草稿
        </n-button>
        <n-button type="primary" :loading="deploying" @click="handleDeploy">
          <template #icon>
            <i class="i-material-symbols:rocket-launch" />
          </template>
          发布部署
        </n-button>
      </div>
    </div>

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
            ref="modelerRef"
            :key="designerRenderKey"
            :xml="bpmnXml"
            :form-asset-options="nodeFormAssetOptions"
            :form-field-catalog="formFieldCatalog"
            :process-config="processConfig"
            @change="handleBpmnChange"
            @ready="handleModelerReady"
            @import-start="handleDiagramImportStart"
            @import-end="handleDiagramImportEnd"
          />
          <FlowModeler
            v-else
            ref="modelerRef"
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
                  <i class="i-material-symbols:auto-awesome ai-generating-icon" />
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
              <span class="settings-field-label">流程类型</span>
              <n-input v-model:value="modelInfo.flowType" placeholder="approval" size="small" />
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
              <label class="settings-field">
                <span class="settings-field-label">表单类型</span>
                <n-select
                  v-model:value="modelInfo.formType"
                  :options="businessManagedFormTypeOptions"
                  size="small"
                  @update:value="handleBusinessManagedFormTypeChange"
                />
              </label>
              <label v-if="businessObjectPickerVisible" class="settings-field">
                <span class="settings-field-label">业务应用</span>
                <n-select
                  v-model:value="manualBusinessObjectCode"
                  :options="businessObjectOptions"
                  :loading="businessObjectLoading"
                  placeholder="选择应用管理中的业务对象"
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
                  v-if="appManagedFormTypeActive && businessContextActive"
                  :node-form="businessGlobalFormNode"
                  :form-assets="nodeFormAssetOptions"
                  show-all-modes
                  @update="handleBusinessGlobalFormUpdate"
                />
                <n-empty
                  v-else-if="appManagedFormTypeActive"
                  size="small"
                  description="请先选择业务应用，再选择应用管理中维护的表单"
                />
                <n-empty
                  v-else
                  size="small"
                  description="当前流程不使用发起表单"
                />
              </div>
              <label v-if="modelInfo.formType === 'external'" class="settings-field">
                <span class="settings-field-label">外置表单</span>
                <n-input v-model:value="modelInfo.formUrl" placeholder="/views/leave/apply" size="small" />
              </label>
              <div v-if="appManagedFormTypeActive" class="settings-tip">
                表单来源于应用管理的表单设计；流程侧只选择表单资产，节点字段权限在节点抽屉维护。
              </div>
            </template>
            <template v-else>
              <label class="settings-field">
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
                <n-input v-model:value="modelInfo.formUrl" placeholder="/views/leave/apply" size="small" />
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
                  审批设置
                </div>
                <div class="settings-pane-desc">
                  控制提交人撤回权限，以及同一审批人重复出现时的自动处理规则。
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
                  <i class="i-material-symbols:auto-awesome ai-flow-title-icon" />
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

            <div ref="aiMessageListRef" class="ai-flow-body">
              <div v-if="aiMessages.length === 0" class="ai-flow-empty">
                <div class="ai-empty-hero">
                  <i class="i-material-symbols:auto-awesome ai-empty-icon" />
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
                          <i class="i-material-symbols:psychology reasoning-icon" />
                          <span class="reasoning-label">思考过程</span>
                          <span v-if="msg.reasoningTime" class="reasoning-duration">用时 {{ msg.reasoningTime }}s</span>
                          <span v-else-if="msg.isReasoning" class="reasoning-duration thinking">思考中...</span>
                        </div>
                        <i class="i-material-symbols:expand-more reasoning-toggle" :class="{ expanded: expandedReasonings[index] }" />
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
              <div ref="aiMessageEndRef" class="ai-message-end" />
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
                        <i class="i-material-symbols:expand-more model-trigger-chevron" />
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
            ref="formDesignerRef"
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

<script setup>
import { NTag, NTreeSelect } from 'naive-ui'
import { computed, defineAsyncComponent, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { modelListByProvider, providerPage } from '@/api/ai'
import { businessFlowFormAssets, businessFlowModelBindings, businessObjectList } from '@/api/business-app'
import flowApi from '@/api/flow'
import { streamFlowGenerate } from '@/api/flow-generator'
import NodePropertiesPanel from '@/components/bpmn/NodePropertiesPanel.vue'
import { DingFlowDesigner } from '@/components/flow-designer'
import FlowPropertyPanelShell from '@/components/flow/FlowPropertyPanelShell.vue'
import FlowFormCreateDesigner from '@/components/form-create/FlowFormCreateDesigner.vue'
import FlowFormCreateRenderer from '@/components/form-create/FlowFormCreateRenderer.vue'
import { useDict } from '@/composables/useDict'
import { useTabStore } from '@/store'
import { toNumberDictOptions } from '@/utils/dict-options'
import BusinessFlowFormAssetSelect from '@/views/app-center/components/designer/BusinessFlowFormAssetSelect.vue'
import { buildFlowCategoryTreeOptions, resolveFlowCategoryValue } from './utils/categoryOptions'
import { buildLocalFormFieldCatalog } from './utils/form-field-catalog'
import VersionHistory from './version.vue'

const props = defineProps({
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
  businessEntryRoute: {
    type: String,
    default: '',
  },
  codeApp: {
    type: Boolean,
    default: false,
  },
})
const emit = defineEmits(['close', 'saved', 'deployed'])
const route = useRoute()
const router = useRouter()
const tabStore = useTabStore()
const message = window.$message
const { dict } = useDict('flow_designer_type', 'flow_process_form_type', 'flow_auto_approval_mode', 'flow_model_status')

const FlowModeler = defineAsyncComponent(() => import('@/components/bpmn/FlowModeler.vue'))

const embedded = computed(() => props.embedded)
const explicitBusinessObjectCode = computed(() => routeQueryText(props.businessObjectCode || route.query.businessObjectCode || route.query.objectCode))
const explicitBusinessObjectName = computed(() => routeQueryText(props.businessObjectName || route.query.businessObjectName || route.query.objectName))
const explicitBusinessEntryRoute = computed(() => routeQueryText(props.businessEntryRoute || route.query.businessEntryRoute))
const resolvedBusinessBinding = ref(null)
const manualBusinessObjectCode = ref('')
const manualBusinessObjectName = ref('')
const businessObjectOptions = ref([])
const businessObjectLoading = ref(false)
const businessObjectCode = computed(() => explicitBusinessObjectCode.value || routeQueryText(resolvedBusinessBinding.value?.objectCode) || manualBusinessObjectCode.value)
const businessContextActive = computed(() => !!businessObjectCode.value)
const businessContextName = computed(() => explicitBusinessObjectName.value || routeQueryText(resolvedBusinessBinding.value?.objectName) || manualBusinessObjectName.value || businessObjectCode.value)
const businessEntryRoute = computed(() => explicitBusinessEntryRoute.value || routeQueryText(resolvedBusinessBinding.value?.entryRoute))
const effectiveCodeApp = computed(() => {
  return props.codeApp
    || parseBooleanWithDefault(routeQueryText(route.query.codeApp), false)
    || (!explicitBusinessObjectCode.value && parseBooleanWithDefault(resolvedBusinessBinding.value?.codeApp, false))
})

const saving = ref(false)
const deploying = ref(false)
const pageLoading = ref(true)
const diagramLoadingCount = ref(0)
const modelerRef = ref(null)
const bpmnXml = ref('')
const hasChanges = ref(false)
watch(hasChanges, (value) => {
  if (!props.embedded)
    tabStore.setTabDirty(route.fullPath, value, '当前流程设计存在未保存的更改')
}, { immediate: true })
const aiSending = ref(false)
const aiPrompt = ref('')
const aiSessionId = ref('')
const aiMessages = ref([])
const aiDraft = ref(null)
const showAiXmlPreview = ref(false)
const showVersionHistory = ref(false)
const aiAbortController = ref(null)
const aiRawContent = ref('')
const aiReasoningContent = ref('')
const aiIsReasoningPhase = ref(false)
const aiReasoningStartTime = ref(null)
const aiReasoningEndTime = ref(null)
const aiCurrentStage = ref('')
const expandedReasonings = ref({})
const aiMessageListRef = ref(null)
const aiMessageEndRef = ref(null)
const reasoningContentRefs = ref([])
const showModelPanel = ref(false)
const syncingModel = ref(false)
let businessSelectionClearTimer = null

const aiProviderId = ref(null)
const aiModelId = ref(null)
const providerOptions = ref([])
const modelOptions = ref([])

const workspaceMode = ref('design')
const rightActiveTab = ref('flow')

const modelInfo = reactive({
  id: '',
  modelName: '',
  modelKey: '',
  category: '',
  flowType: '',
  designerType: 'approval',
  allowSubmitterWithdraw: true,
  autoApprovalMode: 'none',
  formType: 'dynamic',
  formId: null,
  formUrl: '',
  formJson: '',
  description: '',
  status: 0,
  version: 1,
  startListener: '',
  endListener: '',
})

const showFormDesigner = ref(false)
const formDesignerRef = ref(null)
const formSchema = ref([])
const formOptions = ref([])
const formFieldCatalog = ref([])
const showFormPreview = ref(false)
const businessFormAssets = ref([])

const categoryTreeOptions = ref([])
const modelerInstance = ref(null)
const dockedElement = ref(null)

const designerTypeOptions = computed(() => dict.value.flow_designer_type || [])
const formTypeOptions = computed(() => dict.value.flow_process_form_type || [])
const businessManagedFormTypeOptions = computed(() => [...formTypeOptions.value]
  .sort((a, b) => Number(b.value === 'business') - Number(a.value === 'business')))
const autoApprovalModeOptions = computed(() => (dict.value.flow_auto_approval_mode || []).map(item => ({
  ...item,
  desc: item.remark,
})))
const modelStatusOptions = computed(() => toNumberDictOptions(dict.value.flow_model_status))

const aiExamples = [
  { label: '请假审批', text: '生成一个请假审批流程：3天以内直属上级审批，超过3天先直属上级再HR审批。' },
  { label: '报销审批', text: '把当前流程改成报销审批：金额超过5000增加财务经理审批，否则财务专员审批。' },
  { label: '驳回路径', text: '给当前流程增加驳回路径，审批不通过时回到发起人修改。' },
]

const aiStages = [
  { key: 'analyzing', label: '分析需求' },
  { key: 'generating', label: '生成流程' },
  { key: 'reasoning', label: '推理结构' },
  { key: 'complete', label: '完成' },
]

const currentAiStageIndex = computed(() => {
  return aiStages.findIndex(s => s.key === aiCurrentStage.value)
})

const statusTag = computed(() => {
  const option = modelStatusOptions.value.find(item => item.value === Number(modelInfo.status))
  return option
    ? { type: option.listClass || 'default', label: option.label }
    : { type: 'default', label: modelInfo.status ?? '未知' }
})

const isReadonly = computed(() => modelInfo.status === 1)

const designerLoading = computed(() => pageLoading.value || diagramLoadingCount.value > 0)

const aiGeneratingCanvasHintVisible = computed(() => aiSending.value && !designerLoading.value)

const aiCanvasHintText = computed(() => {
  const stageText = aiStages.find(s => s.key === aiCurrentStage.value)?.label || '生成流程'
  return `${stageText}中，请耐心等待，完成后可一键加载到画布`
})

const currentProviderLabel = computed(() => {
  const item = providerOptions.value.find(p => p.value === aiProviderId.value)
  return item?.label || '未选择供应商'
})

const currentModelLabel = computed(() => {
  const item = modelOptions.value.find(m => m.value === aiModelId.value)
  if (!item)
    return '请选择模型'
  return item.modelCode || item.label
})

const designerType = computed(() => normalizeDesignerType(modelInfo.designerType))
const isApprovalDesigner = computed(() => designerType.value === 'approval')
const isBusinessDesigner = computed(() => designerType.value === 'business')
const designerRenderKey = computed(() => `${modelInfo.id || 'new'}:${designerType.value}`)
const designerTypeLabel = computed(() => {
  return designerTypeOptions.value.find(item => item.value === designerType.value)?.label || designerType.value
})
const businessPanelTitle = computed(() => getElementTitle(dockedElement.value))
const businessPanelIcon = computed(() => getElementIcon(dockedElement.value))

const processConfig = computed(() => ({
  allowSubmitterWithdraw: modelInfo.allowSubmitterWithdraw !== false,
  autoApprovalMode: normalizeAutoApprovalMode(modelInfo.autoApprovalMode),
}))

const appManagedFormTypeActive = computed(() => isAppManagedFormType(modelInfo.formType))
const businessFormConfigActive = computed(() => businessContextActive.value || appManagedFormTypeActive.value)
const businessObjectPickerVisible = computed(() => {
  return appManagedFormTypeActive.value
    && !explicitBusinessObjectCode.value
    && !routeQueryText(resolvedBusinessBinding.value?.objectCode)
})

const nodeFormAssetOptions = computed(() => {
  if (businessContextActive.value) {
    return businessFormAssets.value.map(asset => ({
      ...asset,
      label: `${asset.formName || asset.formKey}（${asset.formKey}）`,
      value: asset.formKey,
      formKey: asset.formKey,
      formName: asset.formName || asset.formKey,
      fieldCatalog: asset.fieldCatalog || [],
      formMode: asset.formMode || asset.type || 'BUSINESS_OBJECT_FORM',
      sourceType: asset.sourceType || asset.source || '',
      fieldCount: asset.fieldCount,
      fieldPreview: asset.fieldPreview || [],
      providerKey: asset.providerKey || '',
      providerName: asset.providerName || '',
      objectName: asset.objectName || '',
      source: asset.source || 'business',
    })).filter(item => item.value)
  }
  return formOptions.value.map(item => ({
    label: item.label || item.formKey || item.value,
    value: item.formKey || item.value,
    formKey: item.formKey || item.value,
    formName: item.label || item.formKey || item.value,
    currentVersionId: item.currentVersionId,
    fieldCatalog: [],
    source: 'flowForm',
  })).filter(item => item.value)
})

const businessGlobalFormRef = computed(() => {
  if (!businessFormConfigActive.value || !appManagedFormTypeActive.value)
    return {}
  return parseBusinessGlobalFormRef(modelInfo.formJson)
})

const businessGlobalFormNode = computed(() => {
  const ref = businessGlobalFormRef.value
  const asset = findBusinessFormAsset(
    ref.formKey,
    ref.providerKey,
    ref.formMode || ref.type || ref.formRef?.formMode || ref.formRef?.type,
  )
  return {
    formMode: ref.formMode || ref.type || asset?.formMode || asset?.type || 'BUSINESS_OBJECT_FORM',
    formKey: ref.formKey || '',
    formName: ref.formName || asset?.formName || '',
    providerKey: ref.providerKey || asset?.providerKey || '',
    formUrl: ref.formUrl || asset?.formUrl || '',
    viewKey: ref.viewKey || asset?.viewKey || 'default',
    formRef: ref.formRef || ref,
  }
})

const selectedBusinessGlobalFormAsset = computed(() => {
  if (!businessContextActive.value)
    return null
  const current = businessGlobalFormNode.value
  return findBusinessFormAsset(current.formKey, current.providerKey, current.formMode)
})

const formConfigStatus = computed(() => {
  if (businessFormConfigActive.value) {
    if (modelInfo.formType === 'none')
      return { label: '无表单', type: 'default' }
    if (modelInfo.formType === 'external') {
      return modelInfo.formUrl
        ? { label: '已配置', type: 'success' }
        : { label: '未配置', type: 'warning' }
    }
    if (!businessContextActive.value)
      return { label: '未选应用', type: 'warning' }
    if (businessGlobalFormNode.value.formKey)
      return { label: '已配置', type: 'success' }
    return { label: '未配置', type: 'warning' }
  }
  if (modelInfo.formType === 'none')
    return { label: '无表单', type: 'default' }
  if (modelInfo.formType === 'external') {
    return modelInfo.formUrl
      ? { label: '已配置', type: 'success' }
      : { label: '未配置', type: 'warning' }
  }
  if (modelInfo.formId || modelInfo.formJson || formSchema.value.length)
    return { label: '已配置', type: 'success' }
  return { label: '未配置', type: 'warning' }
})

const autoApprovalModeLabel = computed(() => {
  return autoApprovalModeOptions.value.find(item => item.value === processConfig.value.autoApprovalMode)?.label || processConfig.value.autoApprovalMode
})

const isAiPanelActive = computed(() => workspaceMode.value === 'settings' && rightActiveTab.value === 'ai')

const settingsTreeGroups = computed(() => [
  {
    label: '基础设置',
    children: [
      {
        key: 'flow',
        label: '流程属性',
        desc: '分类 / 类型 / 说明',
        icon: 'i-material-symbols:tune',
      },
      {
        key: 'form',
        label: '表单配置',
        desc: '发起表单',
        icon: 'i-material-symbols:dynamic-form',
      },
      ...(isApprovalDesigner.value
        ? [{
            key: 'approval',
            label: '审批设置',
            desc: '撤回 / 自动审批',
            icon: 'i-material-symbols:approval-delegation-outline',
          }]
        : []),
      {
        key: 'description',
        label: '说明',
        desc: '业务备注',
        icon: 'i-material-symbols:notes',
      },
    ],
  },
  {
    label: '增强能力',
    children: [
      {
        key: 'ai',
        label: 'AI助手',
        desc: '生成 / 修改流程',
        icon: 'i-material-symbols:auto-awesome',
      },
    ],
  },
])

function setWorkspaceMode(mode) {
  workspaceMode.value = mode
}

function openSettingsPanel(key) {
  workspaceMode.value = 'settings'
  rightActiveTab.value = key
}

function getSettingsBadge(key) {
  if (key === 'form')
    return formConfigStatus.value.label
  if (key === 'approval')
    return processConfig.value.autoApprovalMode === 'none' ? '人工审批' : '自动'
  if (key === 'ai' && aiSending.value)
    return '生成中'
  return ''
}

function getSettingsBadgeClass(key) {
  if (key === 'form')
    return `is-${formConfigStatus.value.type}`
  if (key === 'approval')
    return processConfig.value.autoApprovalMode === 'none' ? 'is-default' : 'is-info'
  if (key === 'ai')
    return 'is-info'
  return ''
}

watch(aiProviderId, async (val, old) => {
  if (val && val !== old) {
    await loadModelOptions(val, false)
  }
})

watch(
  () => [modelInfo.allowSubmitterWithdraw, modelInfo.autoApprovalMode],
  () => {
    if (!syncingModel.value && !pageLoading.value)
      hasChanges.value = true
  },
)

watch(
  () => modelInfo.designerType,
  (value) => {
    const normalized = normalizeDesignerType(value)
    if (value !== normalized) {
      modelInfo.designerType = normalized
      return
    }
    dockedElement.value = null
    modelerInstance.value = null
    if (normalized === 'business' && rightActiveTab.value === 'approval')
      rightActiveTab.value = 'flow'
  },
)

function getRouteModelId() {
  const id = route.query.id
  return Array.isArray(id) ? id[0] : id
}

function resetNewModelState() {
  Object.assign(modelInfo, {
    id: '',
    modelName: '新流程',
    modelKey: `process_${Date.now()}`,
    category: '',
    flowType: '',
    designerType: 'approval',
    allowSubmitterWithdraw: true,
    autoApprovalMode: 'none',
    formType: 'dynamic',
    formId: null,
    formUrl: '',
    formJson: '',
    description: '',
    status: 0,
    version: 1,
    startListener: '',
    endListener: '',
  })
  bpmnXml.value = ''
  formSchema.value = []
  formFieldCatalog.value = []
  businessFormAssets.value = []
  resolvedBusinessBinding.value = null
  manualBusinessObjectCode.value = ''
  manualBusinessObjectName.value = ''
  dockedElement.value = null
  modelerInstance.value = null
  workspaceMode.value = 'design'
  rightActiveTab.value = 'flow'
}

onMounted(async () => {
  try {
    await loadCategories()
    await loadForms()
    await loadProviderOptions()

    const modelId = props.modelId || getRouteModelId()
    if (modelId) {
      await loadModel(modelId)
    }
    else {
      resetNewModelState()
    }
  }
  finally {
    pageLoading.value = false
  }
})

watch(() => route.query.id, async (value, oldValue) => {
  if (props.embedded || value === oldValue)
    return

  pageLoading.value = true
  try {
    const modelId = getRouteModelId()
    if (modelId) {
      await loadModel(modelId)
    }
    else {
      resetNewModelState()
    }
    await nextTick()
    await modelerRef.value?.setXML?.(bpmnXml.value || '')
    hasChanges.value = false
  }
  finally {
    pageLoading.value = false
  }
})

watch(() => props.modelId, async (value, oldValue) => {
  if (!props.embedded || !value || value === oldValue)
    return
  pageLoading.value = true
  try {
    await loadModel(value)
    await nextTick()
    await modelerRef.value?.setXML?.(bpmnXml.value || '')
    hasChanges.value = false
  }
  finally {
    pageLoading.value = false
  }
})

watch(businessObjectCode, async (value, oldValue) => {
  if (value === oldValue || !modelInfo.id || syncingModel.value)
    return
  await refreshFormFieldCatalog()
})

onUnmounted(() => {
  if (!props.embedded)
    tabStore.setTabDirty(route.fullPath, false)
  if (businessSelectionClearTimer) {
    clearTimeout(businessSelectionClearTimer)
    businessSelectionClearTimer = null
  }
  if (aiAbortController.value) {
    aiAbortController.value.abort()
    aiAbortController.value = null
  }
})

async function loadProviderOptions(preserveSelection = false) {
  try {
    const res = await providerPage({ pageNum: 1, pageSize: 100 })
    const records = (res.data?.records || []).filter(item => item.status === undefined || item.status === '0')
    providerOptions.value = records.map(item => ({
      label: item.providerName,
      value: item.id,
    }))

    const hasSelectedProvider = providerOptions.value.some(item => item.value === aiProviderId.value)
    if (!hasSelectedProvider) {
      aiProviderId.value = null
    }

    if (!aiProviderId.value && records.length > 0) {
      const defaultProvider = records.find(item => item.isDefault === '1') || records[0]
      aiProviderId.value = defaultProvider?.id || null
    }

    await loadModelOptions(aiProviderId.value, preserveSelection)
  }
  catch (e) {
    console.warn('[FlowDesign] 加载供应商列表失败:', e.message)
    providerOptions.value = []
  }
}

async function loadModelOptions(selectedProviderId, preserveSelection = false) {
  if (!selectedProviderId) {
    modelOptions.value = []
    aiModelId.value = null
    return
  }
  try {
    const res = await modelListByProvider(selectedProviderId)
    const models = (res.data || []).filter(item => item.status === undefined || item.status === '0')
    modelOptions.value = models.map(item => ({
      label: item.modelName ? `${item.modelName} (${item.modelId})` : item.modelId,
      value: item.modelId,
      modelCode: item.modelId,
      maxTokens: item.maxTokens,
      isDefault: item.isDefault,
    }))

    const hasSelectedModel = modelOptions.value.some(item => item.value === aiModelId.value)
    if (!preserveSelection || !hasSelectedModel) {
      const defaultModel = modelOptions.value.find(item => item.isDefault === '1') || modelOptions.value[0]
      aiModelId.value = defaultModel?.value || null
    }
  }
  catch (e) {
    console.warn('[FlowDesign] 加载模型列表失败:', e.message)
    modelOptions.value = []
    aiModelId.value = null
  }
}

async function loadCategories() {
  try {
    const res = await flowApi.getCategoryTreeSelect(false)
    if (res.code === 200) {
      categoryTreeOptions.value = buildFlowCategoryTreeOptions(res.data || [])
    }
  }
  catch (error) {
    console.error('加载分类失败:', error)
  }
}

async function loadForms() {
  try {
    const res = await flowApi.getEnabledForms()
    if (res.code === 200) {
      formOptions.value = (res.data || []).map(item => ({
        label: item.formName,
        value: item.id,
        formKey: item.formKey,
        currentVersionId: item.currentVersionId,
      }))
    }
  }
  catch (error) {
    console.error('加载表单列表失败:', error)
  }
}

async function refreshFormFieldCatalog(formDetail = null) {
  if (businessContextActive.value) {
    await refreshBusinessFormFieldCatalog()
    return
  }
  businessFormAssets.value = []
  if (appManagedFormTypeActive.value) {
    formFieldCatalog.value = []
    return
  }
  if (modelInfo.formType !== 'dynamic') {
    formFieldCatalog.value = []
    return
  }

  let detail = formDetail
  if (!detail && modelInfo.formId) {
    const option = formOptions.value.find(item => item.value === modelInfo.formId)
    detail = option ? { formKey: option.formKey, currentVersionId: option.currentVersionId } : null
  }

  const formKey = detail?.formKey
  const versionId = detail?.currentVersionId
  if (formKey || versionId) {
    try {
      const res = await flowApi.getFormFieldCatalog({
        formKey,
        versionId,
        modelKey: modelInfo.modelKey,
      })
      if (res.code === 200) {
        const remoteCatalog = res.data || []
        formFieldCatalog.value = remoteCatalog.length ? remoteCatalog : resolveLocalFormFieldCatalog()
        if (remoteCatalog.length)
          return
      }
    }
    catch (error) {
      console.warn('[FlowDesign] 加载表单字段目录失败:', error?.message || error)
    }
  }

  formFieldCatalog.value = resolveLocalFormFieldCatalog()
}

async function loadBusinessObjectOptions() {
  if (businessObjectOptions.value.length || businessObjectLoading.value)
    return
  businessObjectLoading.value = true
  try {
    const res = await businessObjectList({})
    const list = Array.isArray(res.data) ? res.data : []
    const seen = new Set()
    businessObjectOptions.value = list
      .filter((item) => {
        const objectCode = routeQueryText(item?.objectCode)
        if (!objectCode || seen.has(objectCode))
          return false
        seen.add(objectCode)
        return true
      })
      .map(item => ({
        label: `${item.objectName || item.objectCode}（${item.objectCode}）`,
        value: item.objectCode,
        object: item,
      }))
  }
  catch (error) {
    console.warn('[FlowDesign] 加载业务应用列表失败:', error?.message || error)
    businessObjectOptions.value = []
  }
  finally {
    businessObjectLoading.value = false
  }
}

async function handleBusinessObjectChange(value) {
  manualBusinessObjectCode.value = routeQueryText(value)
  const selected = businessObjectOptions.value.find(item => item.value === manualBusinessObjectCode.value)?.object
  manualBusinessObjectName.value = routeQueryText(selected?.objectName) || manualBusinessObjectCode.value
  modelInfo.formType = normalizeAppManagedFormType(modelInfo.formType)
  modelInfo.formId = null
  modelInfo.formUrl = ''
  modelInfo.formJson = ''
  formSchema.value = []
  businessFormAssets.value = []
  formFieldCatalog.value = []
  if (manualBusinessObjectCode.value)
    await refreshBusinessFormFieldCatalog()
  hasChanges.value = true
}

async function handleBusinessManagedFormTypeChange(value) {
  if (value === 'none') {
    modelInfo.formType = 'none'
    modelInfo.formId = null
    modelInfo.formUrl = ''
    modelInfo.formJson = ''
    formSchema.value = []
    formFieldCatalog.value = []
    hasChanges.value = true
    return
  }
  if (value === 'external') {
    modelInfo.formType = 'external'
    modelInfo.formId = null
    modelInfo.formJson = ''
    formSchema.value = []
    formFieldCatalog.value = []
    hasChanges.value = true
    return
  }
  modelInfo.formType = value || 'business'
  modelInfo.formId = null
  modelInfo.formUrl = ''
  formSchema.value = []
  if (!businessContextActive.value) {
    await loadBusinessObjectOptions()
    modelInfo.formJson = ''
    formFieldCatalog.value = []
    hasChanges.value = true
    return
  }
  await refreshBusinessFormFieldCatalog()
  hasChanges.value = true
}

async function resolveBusinessBindingForModel() {
  if (explicitBusinessObjectCode.value) {
    resolvedBusinessBinding.value = null
    return
  }

  const modelKey = routeQueryText(modelInfo.modelKey)
  if (!modelKey) {
    resolvedBusinessBinding.value = null
    return
  }

  try {
    const res = await businessFlowModelBindings(modelKey)
    const bindings = res.code === 200 && Array.isArray(res.data)
      ? res.data.filter(item => routeQueryText(item?.objectCode))
      : []
    resolvedBusinessBinding.value = bindings[0] || null
  }
  catch (error) {
    console.warn('[FlowDesign] 反查业务对象绑定失败:', modelKey, error?.message || error)
    resolvedBusinessBinding.value = null
  }
}

async function refreshBusinessFormFieldCatalog() {
  if (!businessObjectCode.value) {
    businessFormAssets.value = []
    formFieldCatalog.value = []
    return
  }
  try {
    const res = await businessFlowFormAssets(businessObjectCode.value, { includeInternal: true })
    const assets = normalizeBusinessFormAssets(res.data?.formAssets || [])
    businessFormAssets.value = assets
    ensureBusinessGlobalFormSelection(assets)
    formFieldCatalog.value = collectBusinessAssetFields(selectedBusinessGlobalFormAsset.value
      ? [selectedBusinessGlobalFormAsset.value]
      : assets)
  }
  catch (error) {
    console.warn('[FlowDesign] 加载业务表单字段目录失败:', error?.message || error)
    businessFormAssets.value = []
    formFieldCatalog.value = []
  }
}

function normalizeBusinessFormAssets(assets = []) {
  return (Array.isArray(assets) ? assets : [])
    .map((asset) => {
      const formKey = routeQueryText(asset?.formKey || asset?.key || asset?.id)
      const fields = Array.isArray(asset?.fieldCatalog)
        ? asset.fieldCatalog
        : Array.isArray(asset?.fields) ? asset.fields : []
      return {
        ...asset,
        formKey,
        formName: routeQueryText(asset?.formName || asset?.name || asset?.label) || formKey,
        fieldCatalog: normalizeBusinessFieldCatalog(fields),
      }
    })
    .filter(asset => asset.formKey)
}

function handleBusinessGlobalFormUpdate(payload = {}) {
  const formKey = routeQueryText(payload.formKey)
  modelInfo.formType = normalizeAppManagedFormType(modelInfo.formType)
  modelInfo.formId = null
  modelInfo.formUrl = ''
  formSchema.value = []

  if (!formKey) {
    modelInfo.formJson = ''
    formFieldCatalog.value = collectBusinessAssetFields(businessFormAssets.value)
    hasChanges.value = true
    return
  }

  const asset = findBusinessFormAsset(
    formKey,
    payload.providerKey,
    payload.formMode || payload.formRef?.formMode || payload.formRef?.type,
  ) || {}
  modelInfo.formJson = buildBusinessGlobalFormJson({
    ...asset,
    ...payload,
    formKey,
  })
  formFieldCatalog.value = collectBusinessAssetFields(asset.formKey ? [asset] : businessFormAssets.value)
  hasChanges.value = true
}

function ensureBusinessGlobalFormSelection(assets = businessFormAssets.value) {
  if (!businessContextActive.value)
    return
  if (!isAppManagedFormType(modelInfo.formType))
    return
  const availableAssets = Array.isArray(assets) ? assets.filter(asset => routeQueryText(asset?.formKey)) : []
  if (!availableAssets.length)
    return

  const currentRef = parseBusinessGlobalFormRef(modelInfo.formJson)
  const currentFormKey = routeQueryText(currentRef.formKey)
  const currentProviderKey = routeQueryText(currentRef.providerKey)
  const currentFormMode = normalizeBusinessFormMode(
    currentRef.formMode || currentRef.type || currentRef.formRef?.formMode || currentRef.formRef?.type,
  )
  const currentAsset = currentFormKey
    ? findBusinessFormAssetInList(availableAssets, currentFormKey, currentProviderKey, currentFormMode)
    : null

  if (isAppManagedFormType(modelInfo.formType) && currentAsset) {
    const normalizedFormJson = buildBusinessGlobalFormJson({
      ...currentAsset,
      ...(currentRef.formRef || {}),
      ...currentRef,
      formKey: currentFormKey,
    })
    if (normalizedFormJson)
      modelInfo.formJson = normalizedFormJson
    return
  }

  const nextAsset = currentAsset || availableAssets[0]
  modelInfo.formType = normalizeAppManagedFormType(modelInfo.formType)
  modelInfo.formId = null
  modelInfo.formUrl = ''
  formSchema.value = []
  modelInfo.formJson = buildBusinessGlobalFormJson(nextAsset)
}

function buildBusinessGlobalFormJson(source = {}) {
  const formKey = routeQueryText(source.formKey || source.key || source.value)
  if (!formKey)
    return ''
  const formMode = normalizeBusinessFormMode(source.formMode || source.type, 'BUSINESS_OBJECT_FORM')
  const providerKey = routeQueryText(source.providerKey)
  const formRef = {
    ...(source.formRef || {}),
    objectCode: source.objectCode || businessObjectCode.value,
    objectName: source.objectName || businessContextName.value,
    type: formMode,
    formMode,
    formKey,
    formName: source.formName || source.name || source.label || formKey,
    providerKey,
    formUrl: source.formUrl || '',
    viewKey: source.viewKey || 'default',
  }
  return JSON.stringify({
    type: formMode,
    formMode,
    objectCode: formRef.objectCode,
    objectName: formRef.objectName,
    formKey,
    formName: formRef.formName,
    providerKey,
    formUrl: formRef.formUrl,
    viewKey: formRef.viewKey,
    formRef,
  })
}

function parseBusinessGlobalFormRef(value) {
  const text = routeQueryText(value)
  if (!text)
    return {}
  try {
    const parsed = JSON.parse(text)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
      return {}
    const formRef = parsed.formRef && typeof parsed.formRef === 'object' && !Array.isArray(parsed.formRef)
      ? parsed.formRef
      : {}
    return {
      ...formRef,
      ...parsed,
      formRef: {
        ...formRef,
        objectCode: formRef.objectCode || parsed.objectCode || businessObjectCode.value,
        formMode: formRef.formMode || formRef.type || parsed.formMode || parsed.type,
        type: formRef.type || formRef.formMode || parsed.type || parsed.formMode,
        formKey: formRef.formKey || parsed.formKey,
        formName: formRef.formName || parsed.formName,
        providerKey: formRef.providerKey || parsed.providerKey,
        formUrl: formRef.formUrl || parsed.formUrl,
        viewKey: formRef.viewKey || parsed.viewKey,
      },
    }
  }
  catch (error) {
    console.warn('[FlowDesign] 解析业务全局表单引用失败:', error?.message || error)
    return {}
  }
}

function findBusinessFormAsset(formKey, providerKey = '', formMode = '') {
  return findBusinessFormAssetInList(businessFormAssets.value, formKey, providerKey, formMode)
}

function findBusinessFormAssetInList(assets = [], formKey, providerKey = '', formMode = '') {
  const key = routeQueryText(formKey)
  const provider = routeQueryText(providerKey)
  const mode = normalizeBusinessFormMode(formMode)
  if (!key)
    return null
  const availableAssets = Array.isArray(assets) ? assets : []
  const sameFormKey = availableAssets.filter(asset => routeQueryText(asset?.formKey) === key)
  if (!sameFormKey.length)
    return null
  const exactAsset = sameFormKey.find(asset =>
    (!mode || normalizeBusinessFormMode(asset.formMode || asset.type, 'BUSINESS_OBJECT_FORM') === mode)
    && (!provider || routeQueryText(asset.providerKey) === provider),
  )
  if (exactAsset)
    return exactAsset
  if (provider) {
    const providerAsset = sameFormKey.find(asset => routeQueryText(asset.providerKey) === provider)
    if (providerAsset)
      return providerAsset
  }
  if (mode) {
    const modeAsset = sameFormKey.find(asset =>
      normalizeBusinessFormMode(asset.formMode || asset.type, 'BUSINESS_OBJECT_FORM') === mode,
    )
    if (modeAsset)
      return modeAsset
  }
  return sameFormKey[0] || null
}

function normalizeBusinessFormMode(value, fallback = '') {
  const normalized = routeQueryText(value).toUpperCase()
  if (normalized === 'BUSINESS_CODE_FORM' || normalized === 'BUSINESS_OBJECT_FORM' || normalized === 'EXTERNAL')
    return normalized
  return fallback
}

function isAppManagedFormType(value) {
  return value === 'business' || value === 'dynamic'
}

function normalizeAppManagedFormType(value) {
  return isAppManagedFormType(value) ? value : 'business'
}

function collectBusinessAssetFields(assets = []) {
  const result = []
  const used = new Set()
  ;(Array.isArray(assets) ? assets : []).forEach((asset) => {
    const fields = Array.isArray(asset?.fieldCatalog)
      ? asset.fieldCatalog
      : normalizeBusinessFieldCatalog(asset?.fields || [])
    fields.forEach((field) => {
      const code = routeQueryText(field?.field || field?.fieldCode || field?.code || field?.name)
      if (!code || used.has(code))
        return
      used.add(code)
      result.push({
        field: code,
        label: field?.label || field?.fieldLabel || field?.fieldName || field?.title || code,
        componentType: field?.componentType || field?.type || field?.fieldType || '',
        dataType: field?.dataType || '',
        required: field?.required === true,
        readonly: field?.readonly === true || field?.systemField === true || field?.writable === false,
      })
    })
  })
  return result
}

function normalizeBusinessFieldCatalog(fields = []) {
  return (Array.isArray(fields) ? fields : [])
    .map((field) => {
      const code = routeQueryText(field?.field || field?.fieldCode || field?.code || field?.name)
      if (!code)
        return null
      return {
        ...field,
        field: code,
        label: field?.label || field?.fieldLabel || field?.fieldName || field?.title || code,
        componentType: field?.componentType || field?.type || field?.fieldType || '',
        dataType: field?.dataType || '',
        required: field?.required === true,
        readonly: field?.readonly === true || field?.systemField === true || field?.writable === false,
      }
    })
    .filter(Boolean)
}

function resolveLocalFormFieldCatalog() {
  return buildLocalFormFieldCatalog(formSchema.value)
}

function routeQueryText(value) {
  return String(Array.isArray(value) ? value[0] || '' : value || '').trim()
}

function goBackToBusinessApp() {
  if (props.embedded) {
    emit('close')
    return
  }
  if (businessEntryRoute.value) {
    router.push(normalizeBusinessEntryRoute(businessEntryRoute.value))
    return
  }
  router.push({
    path: `/app-center/object/${businessObjectCode.value}/designer`,
    query: {
      panel: 'flow-app',
      codeApp: effectiveCodeApp.value ? '1' : undefined,
    },
  })
}

function normalizeBusinessEntryRoute(value) {
  const route = decodeHtmlEntities(value)
  if (!route)
    return route
  const [path, query = ''] = route.split('?')
  const params = new URLSearchParams(query)
  if (!params.get('panel'))
    params.set('panel', 'flow-app')
  if (effectiveCodeApp.value && !params.get('codeApp'))
    params.set('codeApp', '1')
  return `${path}?${params.toString()}`
}

function decodeHtmlEntities(value) {
  return routeQueryText(value)
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, '\'')
}

function normalizeDesignerType(value) {
  return value === 'business' ? 'business' : 'approval'
}

function normalizeAutoApprovalMode(value) {
  return ['firstOnly', 'consecutive', 'none'].includes(value) ? value : 'none'
}

function parseBooleanWithDefault(value, fallback) {
  if (value == null || value === '')
    return fallback
  const normalized = String(value).trim().toLowerCase()
  if (['true', '1', 'y', 'yes'].includes(normalized))
    return true
  if (['false', '0', 'n', 'no'].includes(normalized))
    return false
  return fallback
}

function readFlowableAttr(el, name) {
  if (!el)
    return null
  const nsValue = el.getAttributeNS?.('http://flowable.org/bpmn', name)
  if (nsValue != null && nsValue !== '')
    return nsValue
  return el.getAttribute?.(`flowable:${name}`) || el.getAttribute?.(name) || null
}

function extractProcessConfigFromXml(xml) {
  if (!xml)
    return { allowSubmitterWithdraw: true, autoApprovalMode: 'none' }
  try {
    const doc = new DOMParser().parseFromString(xml, 'application/xml')
    if (doc.querySelector('parsererror'))
      return { allowSubmitterWithdraw: true, autoApprovalMode: 'none' }
    const processEl = findElementByLocalName(doc, 'process')
    return {
      allowSubmitterWithdraw: parseBooleanWithDefault(
        readFlowableAttr(processEl, 'allowSubmitterWithdraw'),
        true,
      ),
      autoApprovalMode: normalizeAutoApprovalMode(readFlowableAttr(processEl, 'autoApprovalMode')),
    }
  }
  catch {
    return { allowSubmitterWithdraw: true, autoApprovalMode: 'none' }
  }
}

function applyProcessConfigFromXml(xml) {
  const config = extractProcessConfigFromXml(xml)
  modelInfo.allowSubmitterWithdraw = config.allowSubmitterWithdraw
  modelInfo.autoApprovalMode = config.autoApprovalMode
}

function applyProcessConfigToXml(xml) {
  if (!xml)
    return ''
  try {
    const doc = new DOMParser().parseFromString(xml, 'application/xml')
    if (doc.querySelector('parsererror'))
      return xml

    const processEl = findElementByLocalName(doc, 'process')
    if (!processEl)
      return xml

    const definitionsEl = doc.documentElement
    if (definitionsEl && !definitionsEl.getAttribute('xmlns:flowable'))
      definitionsEl.setAttribute('xmlns:flowable', 'http://flowable.org/bpmn')

    processEl.setAttributeNS(
      'http://flowable.org/bpmn',
      'flowable:allowSubmitterWithdraw',
      String(processConfig.value.allowSubmitterWithdraw !== false),
    )
    processEl.setAttributeNS(
      'http://flowable.org/bpmn',
      'flowable:autoApprovalMode',
      normalizeAutoApprovalMode(processConfig.value.autoApprovalMode),
    )
    return new XMLSerializer().serializeToString(doc)
  }
  catch {
    return xml
  }
}

async function getXmlForSave() {
  const currentXml = modelerRef.value?.getXML
    ? await modelerRef.value.getXML()
    : bpmnXml.value
  const xml = applyProcessConfigToXml(currentXml)
  if (xml)
    bpmnXml.value = xml
  return xml
}

async function loadModel(id) {
  syncingModel.value = true
  try {
    const res = await flowApi.getModelDetail(id)
    if (res.code === 200 && res.data) {
      bpmnXml.value = ''
      formSchema.value = []
      formFieldCatalog.value = []
      dockedElement.value = null
      Object.assign(modelInfo, res.data)
      modelInfo.designerType = normalizeDesignerType(res.data.designerType)
      modelInfo.category = resolveFlowCategoryValue(res.data.category, categoryTreeOptions.value)
      bpmnXml.value = res.data.bpmnXml || ''
      applyProcessConfigFromXml(bpmnXml.value)
      await resolveBusinessBindingForModel()

      if (res.data.formType === 'business') {
        formSchema.value = []
      }
      else if (res.data.formJson) {
        try {
          const parsedFormSchema = JSON.parse(res.data.formJson)
          formSchema.value = Array.isArray(parsedFormSchema) ? parsedFormSchema : []
        }
        catch (e) {
          console.error('解析表单配置失败:', e)
          formSchema.value = []
        }
      }
      else {
        formSchema.value = []
      }
      await refreshFormFieldCatalog()
    }
  }
  catch (error) {
    console.error('加载模型失败:', error)
  }
  finally {
    syncingModel.value = false
  }
}

function handleOpenVersionHistory() {
  if (!modelInfo.id) {
    window.$message?.warning('请先保存模型后查看更改记录')
    return
  }
  showVersionHistory.value = true
}

async function handleVersionHistoryRefresh() {
  if (!modelInfo.id)
    return

  await loadModel(modelInfo.id)
  await nextTick()
  await modelerRef.value?.setXML?.(bpmnXml.value || '')
  hasChanges.value = false
}

function handleFormTypeChange(value) {
  if (value !== 'dynamic') {
    formSchema.value = []
    modelInfo.formId = null
    modelInfo.formJson = ''
    formFieldCatalog.value = []
  }
  if (value !== 'external') {
    modelInfo.formUrl = ''
  }
  if (value === 'dynamic')
    refreshFormFieldCatalog()
}

async function handleFormSelect(formId) {
  if (!formId) {
    formSchema.value = []
    modelInfo.formJson = ''
    formFieldCatalog.value = resolveLocalFormFieldCatalog()
    return
  }

  try {
    const res = await flowApi.getFormById(formId)
    if (res.code === 200 && res.data) {
      if (res.data.formSchema) {
        formSchema.value = JSON.parse(res.data.formSchema)
        modelInfo.formJson = res.data.formSchema
      }
      await refreshFormFieldCatalog(res.data)
    }
  }
  catch (error) {
    console.error('加载表单失败:', error)
    message.error('加载表单失败')
  }
}

function handleSaveFormSchema(schema) {
  formSchema.value = schema
  modelInfo.formJson = JSON.stringify(schema)
  showFormDesigner.value = false
  hasChanges.value = true
  formFieldCatalog.value = resolveLocalFormFieldCatalog()
  message.success('表单设计已保存')
}

function toggleAiPanel() {
  if (isAiPanelActive.value) {
    workspaceMode.value = 'design'
    return
  }
  openSettingsPanel('ai')
}

async function handleAiSend() {
  const prompt = aiPrompt.value.trim()
  if (!prompt) {
    message.warning('请输入流程需求')
    return
  }
  if (aiSending.value) {
    return
  }
  if (!aiProviderId.value) {
    message.warning('请选择AI供应商')
    return
  }
  if (!aiModelId.value) {
    message.warning('请选择对话模型')
    return
  }

  try {
    aiSending.value = true
    aiDraft.value = null
    aiRawContent.value = ''
    aiReasoningContent.value = ''
    aiIsReasoningPhase.value = false
    aiReasoningStartTime.value = null
    aiReasoningEndTime.value = null
    aiCurrentStage.value = 'analyzing'

    if (!aiSessionId.value) {
      aiSessionId.value = `flow_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
    }
    aiMessages.value.push({ role: 'user', content: prompt })
    aiMessages.value.push({
      role: 'assistant',
      content: '正在分析流程需求...',
      streaming: true,
      isReasoning: false,
      reasoning: '',
      reasoningTime: null,
    })
    aiPrompt.value = ''
    scrollAiToBottom()

    const currentXml = await modelerRef.value?.getXML(true)

    aiAbortController.value = streamFlowGenerate(
      {
        sessionId: aiSessionId.value,
        description: prompt,
        providerId: aiProviderId.value,
        modelId: aiModelId.value,
        flowModelId: modelInfo.id || modelInfo.modelKey || '',
        aiModelName: currentModelLabel.value,
        modelKey: modelInfo.modelKey || `process_${Date.now()}`,
        modelName: modelInfo.modelName || '新流程',
        category: modelInfo.category || '',
        flowType: modelInfo.flowType || '',
        formType: modelInfo.formType || 'dynamic',
        currentBpmnXml: currentXml || bpmnXml.value || '',
        currentFormJson: modelInfo.formJson || '',
        temperature: 0.2,
        maxTokens: 12000,
      },
      handleFlowSSEChunk,
      handleFlowSSEComplete,
      handleFlowSSEError,
    )
  }
  catch (error) {
    console.error('AI生成流程失败:', error)
    message.error(error.message || 'AI生成流程失败')
    aiSending.value = false
    aiCurrentStage.value = ''
    updateLastAiAssistantMessage('AI生成流程失败', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime: null })
  }
}

function handleAbortAi() {
  if (aiAbortController.value) {
    aiAbortController.value.abort()
    aiAbortController.value = null
  }
  aiSending.value = false
  aiCurrentStage.value = ''
  updateLastAiAssistantMessage('已停止生成', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime: null })
}

function handleFlowSSEChunk({ event, data }) {
  if (event === 'progress') {
    aiCurrentStage.value = 'generating'
    updateLastAiAssistantMessage(data.message || '正在生成流程配置...', { reasoning: aiReasoningContent.value, isReasoning: aiIsReasoningPhase.value, reasoningTime: null })
    scrollAiToBottom()
    return
  }

  if (event !== 'chunk') {
    return
  }

  const chunkContent = data.content || ''
  if (!chunkContent) {
    return
  }

  if (chunkContent.includes('==================== 思考过程 ====================')) {
    aiIsReasoningPhase.value = true
    aiCurrentStage.value = 'reasoning'
    aiReasoningStartTime.value = Date.now()
    aiReasoningEndTime.value = null
    aiReasoningContent.value = ''
    const afterDelimiter = chunkContent.split('==================== 思考过程 ====================')[1] || ''
    aiReasoningContent.value += afterDelimiter.replace('\n', '')
    updateLastAiAssistantMessage('', { reasoning: aiReasoningContent.value, isReasoning: true, reasoningTime: null })
  }
  else if (chunkContent.includes('==================== 完整回复 ====================')) {
    aiIsReasoningPhase.value = false
    aiCurrentStage.value = 'generating'
    aiReasoningEndTime.value = Date.now()
    const reasoningTime = aiReasoningStartTime.value ? Math.round((aiReasoningEndTime.value - aiReasoningStartTime.value) / 1000) : null
    const afterDelimiter = chunkContent.split('==================== 完整回复 ====================')[1] || ''
    aiRawContent.value += afterDelimiter.replace('\n', '')
    updateLastAiAssistantMessage(aiRawContent.value || '正在生成 BPMN XML...', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime })
  }
  else if (aiIsReasoningPhase.value) {
    aiReasoningContent.value += chunkContent
    updateLastAiAssistantMessage('', { reasoning: aiReasoningContent.value, isReasoning: true, reasoningTime: null })
  }
  else {
    aiRawContent.value += chunkContent
    updateLastAiAssistantMessage(aiRawContent.value || '正在生成 BPMN XML...', { reasoning: aiReasoningContent.value, isReasoning: false })
  }
  scrollAiToBottom()
}

function handleFlowSSEComplete(data) {
  aiSending.value = false
  aiAbortController.value = null
  aiCurrentStage.value = 'complete'

  if (data?.sessionId) {
    aiSessionId.value = data.sessionId
  }

  const reasoningTime = aiReasoningStartTime.value ? Math.round((Date.now() - aiReasoningStartTime.value) / 1000) : null

  const parsed = parseAiFlowResponse(aiRawContent.value)
  if (parsed?.bpmnXml) {
    aiDraft.value = normalizeAiFlowDraft(parsed)
    updateLastAiAssistantMessage(aiDraft.value.summary || aiDraft.value.description || '已生成流程配置，可一键加载到画布。', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime })
  }
  else {
    updateLastAiAssistantMessage(aiRawContent.value || 'AI未返回可解析的流程配置', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime })
    message.warning('AI响应未解析到 BPMN XML，请继续追问或调整需求')
  }
  scrollAiToBottom()
}

function handleFlowSSEError(errorMessage) {
  aiSending.value = false
  aiAbortController.value = null
  aiCurrentStage.value = ''
  const reasoningTime = aiReasoningStartTime.value ? Math.round((Date.now() - aiReasoningStartTime.value) / 1000) : null
  updateLastAiAssistantMessage(`生成失败: ${errorMessage}`, { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime })
  message.error(errorMessage || 'AI生成流程失败')
}

function updateLastAiAssistantMessage(content, reasoningData = null) {
  const last = aiMessages.value[aiMessages.value.length - 1]
  if (last && last.role === 'assistant') {
    last.content = content
    last.streaming = aiSending.value
    if (reasoningData) {
      last.reasoning = reasoningData.reasoning
      last.isReasoning = reasoningData.isReasoning
      if (reasoningData.reasoningTime !== undefined && reasoningData.reasoningTime !== null) {
        last.reasoningTime = reasoningData.reasoningTime
      }
      if (reasoningData.isReasoning) {
        expandedReasonings.value[aiMessages.value.length - 1] = true
      }
    }
  }
  else {
    aiMessages.value.push({
      role: 'assistant',
      content,
      streaming: aiSending.value,
      isReasoning: reasoningData?.isReasoning || false,
      reasoning: reasoningData?.reasoning || '',
      reasoningTime: reasoningData?.reasoningTime || null,
    })
    if (reasoningData?.isReasoning) {
      expandedReasonings.value[aiMessages.value.length - 1] = true
    }
  }
}

function toggleReasoning(idx) {
  expandedReasonings.value[idx] = !expandedReasonings.value[idx]
  scrollAiToBottom()
}

function scrollAiToBottom() {
  nextTick(() => {
    requestAnimationFrame(() => {
      scrollActiveReasoningToBottom()
      if (aiMessageEndRef.value) {
        aiMessageEndRef.value.scrollIntoView({ block: 'end' })
      }
      if (aiMessageListRef.value) {
        aiMessageListRef.value.scrollTop = aiMessageListRef.value.scrollHeight
      }
      requestAnimationFrame(() => {
        scrollActiveReasoningToBottom()
        if (aiMessageListRef.value) {
          aiMessageListRef.value.scrollTop = aiMessageListRef.value.scrollHeight
        }
      })
    })
  })
}

function setReasoningContentRef(el, index) {
  if (el) {
    reasoningContentRefs.value[index] = el
  }
  else {
    delete reasoningContentRefs.value[index]
  }
}

function scrollActiveReasoningToBottom() {
  const activeIndex = aiMessages.value.findLastIndex(msg => msg.role === 'assistant' && msg.isReasoning)
  const reasoningEl = reasoningContentRefs.value[activeIndex]
  if (reasoningEl) {
    reasoningEl.scrollTop = reasoningEl.scrollHeight
  }
}

watch(() => aiMessages.value.length, () => scrollAiToBottom())
watch(() => [aiRawContent.value, aiReasoningContent.value, aiDraft.value], () => scrollAiToBottom(), { flush: 'post' })
watch(aiSending, (val) => {
  if (val)
    scrollAiToBottom()
})

async function handleApplyAiDraft() {
  if (!aiDraft.value?.bpmnXml) {
    message.warning('暂无可加载的流程配置')
    return
  }

  try {
    if (aiDraft.value.modelName) {
      modelInfo.modelName = aiDraft.value.modelName
    }
    if (!modelInfo.id && aiDraft.value.modelKey) {
      modelInfo.modelKey = aiDraft.value.modelKey
    }
    if (aiDraft.value.category) {
      modelInfo.category = resolveFlowCategoryValue(aiDraft.value.category, categoryTreeOptions.value)
    }
    if (aiDraft.value.flowType) {
      modelInfo.flowType = aiDraft.value.flowType
    }
    if (aiDraft.value.description) {
      modelInfo.description = aiDraft.value.description
    }
    if (aiDraft.value.formType) {
      modelInfo.formType = aiDraft.value.formType
    }
    if (aiDraft.value.formJson) {
      modelInfo.formJson = typeof aiDraft.value.formJson === 'string'
        ? aiDraft.value.formJson
        : JSON.stringify(aiDraft.value.formJson)
      try {
        formSchema.value = JSON.parse(modelInfo.formJson)
      }
      catch {
        formSchema.value = []
      }
    }

    const safeModelKey = syncSafeModelKey(modelInfo.modelKey || aiDraft.value.modelKey)
    const xmlToImport = normalizeAiBpmnXml(aiDraft.value.bpmnXml, safeModelKey)
    const displayIssue = getBpmnDisplayIssue(xmlToImport)
    if (displayIssue) {
      throw new Error(displayIssue)
    }

    aiDraft.value = {
      ...aiDraft.value,
      bpmnXml: xmlToImport,
    }
    applyProcessConfigFromXml(xmlToImport)
    bpmnXml.value = xmlToImport
    await modelerRef.value?.setXML(xmlToImport)
    hasChanges.value = true
    message.success('AI流程配置已加载到画布')
  }
  catch (error) {
    console.error('加载AI流程配置失败:', error)
    message.error(`加载失败: ${error.message}`)
  }
}

function handleNewAiSession() {
  if (aiAbortController.value) {
    aiAbortController.value.abort()
    aiAbortController.value = null
  }
  aiSending.value = false
  aiCurrentStage.value = ''
  aiSessionId.value = ''
  aiPrompt.value = ''
  aiMessages.value = []
  aiDraft.value = null
  aiRawContent.value = ''
  aiReasoningContent.value = ''
  aiIsReasoningPhase.value = false
  aiReasoningStartTime.value = null
  aiReasoningEndTime.value = null
  expandedReasonings.value = {}
}

function handlePreviewAiXml() {
  showAiXmlPreview.value = true
}

function normalizeAiFlowDraft(draft) {
  const modelKey = normalizeBpmnId(draft.modelKey || modelInfo.modelKey || `process_${Date.now()}`)
  return {
    ...draft,
    modelKey,
    modelName: draft.modelName || modelInfo.modelName || 'AI生成流程',
    bpmnXml: normalizeAiBpmnXml(draft.bpmnXml, modelKey),
  }
}

function normalizeAiBpmnXml(xml, modelKey) {
  const safeModelKey = normalizeBpmnId(modelKey)
  const extractedXml = extractBpmnXml(xml) || (xml || '').trim()
  const bpmnXml = ensureProcessId(extractedXml, safeModelKey)
  return repairBpmnXml(bpmnXml, safeModelKey)
}

function parseAiFlowResponse(content) {
  if (!content)
    return null

  const cleaned = stripCodeFence(content)
  const jsonCandidates = [
    cleaned,
    extractJsonObject(cleaned),
  ].filter(Boolean)

  for (const candidate of jsonCandidates) {
    try {
      return JSON.parse(candidate)
    }
    catch {
      // try next candidate
    }
  }

  const xml = extractBpmnXml(cleaned)
  if (xml) {
    return {
      bpmnXml: xml,
      summary: '已从AI响应中提取 BPMN XML',
    }
  }
  return null
}

function stripCodeFence(content) {
  let text = content.trim()
  if (text.startsWith('```json')) {
    text = text.slice(7)
  }
  else if (text.startsWith('```xml')) {
    text = text.slice(6)
  }
  else if (text.startsWith('```')) {
    text = text.slice(3)
  }
  if (text.endsWith('```')) {
    text = text.slice(0, -3)
  }
  return text.trim()
}

function extractJsonObject(text) {
  const start = text.indexOf('{')
  const end = text.lastIndexOf('}')
  if (start >= 0 && end > start) {
    return text.slice(start, end + 1)
  }
  return ''
}

function extractBpmnXml(text) {
  const match = text.match(/(?:<\?xml[\s\S]*?\?>\s*)?<(?:[\w.-]+:)?definitions\b[\s\S]*<\/(?:[\w.-]+:)?definitions>/)
  return match ? match[0].trim() : ''
}

function ensureProcessId(xml, modelKey) {
  const safeModelKey = normalizeBpmnId(modelKey)
  if (!xml || !safeModelKey)
    return xml
  const doc = parseXmlDocument(xml)
  if (!doc)
    return xml

  const processEl = findElementByLocalName(doc, 'process')
  if (!processEl)
    return xml

  const originalProcessId = processEl.getAttribute('id')
  processEl.setAttribute('id', safeModelKey)
  processEl.setAttribute('isExecutable', 'true')

  findElementsByLocalName(doc, 'participant').forEach((el) => {
    if (!originalProcessId || el.getAttribute('processRef') === originalProcessId) {
      el.setAttribute('processRef', safeModelKey)
    }
  })

  const hasCollaboration = !!findElementByLocalName(doc, 'collaboration')
  findElementsByLocalName(doc, 'BPMNPlane').forEach((el) => {
    if (!hasCollaboration || !originalProcessId || !el.getAttribute('bpmnElement') || el.getAttribute('bpmnElement') === originalProcessId) {
      el.setAttribute('bpmnElement', safeModelKey)
    }
  })

  return new XMLSerializer().serializeToString(doc)
}

function syncSafeModelKey(modelKey) {
  const safeModelKey = normalizeBpmnId(modelKey)
  if (modelInfo.modelKey !== safeModelKey) {
    const originalModelKey = modelInfo.modelKey || modelKey
    modelInfo.modelKey = safeModelKey
    if (originalModelKey && originalModelKey !== safeModelKey) {
      message.warning(`流程Key已调整为 ${safeModelKey}，BPMN流程ID不能以数字开头或包含特殊字符`)
    }
  }
  return safeModelKey
}

function normalizeBpmnId(value) {
  let id = String(value || '').trim()
  if (!id)
    id = `process_${Date.now()}`

  id = id
    .replace(/[^\w.-]/g, '_')
    .replace(/_+/g, '_')

  if (!/^[a-z_]/i.test(id))
    id = `process_${id}`

  return id || `process_${Date.now()}`
}

function repairBpmnXml(xml, modelKey) {
  if (!xml)
    return xml
  const parseError = getXmlParseError(xml)
  const displayIssue = parseError ? '' : getBpmnDisplayIssue(xml)
  if (!parseError && !displayIssue && hasBpmnDiagram(xml))
    return xml

  const repaired = rebuildDiagramInfo(xml, modelKey)
  if (!repaired)
    return xml

  const repairedError = getXmlParseError(repaired)
  const repairedDisplayIssue = repairedError ? '' : getBpmnDisplayIssue(repaired)
  if (repairedError) {
    console.warn('[FlowDesign] BPMN XML 修复后仍不可解析:', repairedError)
    return xml
  }
  if (repairedDisplayIssue) {
    console.warn('[FlowDesign] BPMN XML 修复后仍不可展示:', repairedDisplayIssue)
    return xml
  }
  console.warn('[FlowDesign] AI 返回的 BPMNDI 坐标信息无效，已重新生成图形信息:', parseError || displayIssue || '缺少 BPMNDiagram')
  return repaired
}

function parseXmlDocument(xml) {
  try {
    const doc = new DOMParser().parseFromString(xml, 'application/xml')
    if (doc.querySelector('parsererror'))
      return null
    return doc
  }
  catch {
    return null
  }
}

function getXmlParseError(xml) {
  try {
    const doc = new DOMParser().parseFromString(xml, 'application/xml')
    const error = doc.querySelector('parsererror')
    return error?.textContent || ''
  }
  catch (error) {
    return error.message
  }
}

function hasBpmnDiagram(xml) {
  const doc = parseXmlDocument(xml)
  if (!doc)
    return /<(?:[\w.-]+:)?BPMNDiagram\b/.test(xml)
  return !!findElementByLocalName(doc, 'BPMNDiagram')
}

function getBpmnDisplayIssue(xml) {
  const doc = parseXmlDocument(xml)
  if (!doc)
    return 'BPMN XML 语法不合法，无法解析'

  const definitionsEl = findElementByLocalName(doc, 'definitions')
  const rootChildren = definitionsEl ? Array.from(definitionsEl.children) : []
  const displayRoots = rootChildren.filter(el => ['process', 'collaboration'].includes(el.localName))
  if (displayRoots.length === 0)
    return 'BPMN XML 缺少 process 或 collaboration，无法在画布展示'

  const planeEl = findElementByLocalName(doc, 'BPMNPlane')
  if (!planeEl)
    return 'BPMN XML 缺少 BPMNPlane 图形平面'

  const rootIds = new Set(displayRoots.map(el => el.getAttribute('id')).filter(Boolean))
  const planeElement = planeEl.getAttribute('bpmnElement')
  if (!planeElement || !rootIds.has(planeElement))
    return 'BPMNPlane 的 bpmnElement 未指向真实的 process/collaboration'

  return ''
}

function rebuildDiagramInfo(xml, modelKey) {
  const semanticXml = stripDiagramInfo(xml)
  const semanticError = getXmlParseError(semanticXml)
  if (semanticError) {
    console.warn('[FlowDesign] BPMN 语义 XML 不可解析，无法重建图形信息:', semanticError)
    return ''
  }

  const doc = new DOMParser().parseFromString(semanticXml, 'application/xml')
  const processEl = findElementByLocalName(doc, 'process')
  if (!processEl)
    return ''

  const processId = processEl.getAttribute('id') || modelKey
  const nodes = []
  const flows = []

  Array.from(processEl.children).forEach((el) => {
    const id = el.getAttribute('id')
    if (!id)
      return
    if (el.localName === 'sequenceFlow') {
      flows.push({
        id,
        sourceRef: el.getAttribute('sourceRef'),
        targetRef: el.getAttribute('targetRef'),
      })
      return
    }
    if (isBpmnFlowNode(el.localName)) {
      nodes.push({
        id,
        type: el.localName,
      })
    }
  })

  if (nodes.length === 0)
    return ''

  const bounds = layoutBpmnNodes(nodes, flows)
  const diagramXml = buildDiagramXml(processId, nodes, flows, bounds)
  const normalizedSemanticXml = ensureDiagramNamespaces(semanticXml)
  return normalizedSemanticXml.replace(/<\/((?:[\w.-]+:)?definitions)>\s*$/i, `${diagramXml}\n</$1>`)
}

function stripDiagramInfo(xml) {
  const openMatch = xml.match(/<([\w.-]+:)?BPMNDiagram\b/)
  const start = openMatch?.index ?? -1
  if (start < 0)
    return xml

  const closeTag = `</${openMatch[1] || ''}BPMNDiagram>`
  const end = xml.indexOf(closeTag, start)
  if (end >= 0) {
    return `${xml.slice(0, start)}${xml.slice(end + closeTag.length)}`
  }

  const definitionsEnd = xml.search(/<\/(?:[\w.-]+:)?definitions>\s*$/i)
  if (definitionsEnd >= 0) {
    return `${xml.slice(0, start)}${xml.slice(definitionsEnd)}`
  }
  return xml.slice(0, start)
}

function ensureDiagramNamespaces(xml) {
  const namespaces = [
    ['bpmndi', 'http://www.omg.org/spec/BPMN/20100524/DI'],
    ['dc', 'http://www.omg.org/spec/DD/20100524/DC'],
    ['di', 'http://www.omg.org/spec/DD/20100524/DI'],
  ]
  return namespaces.reduce((text, [prefix, uri]) => {
    if (text.includes(`xmlns:${prefix}=`))
      return text
    return text.replace(/<(?:[\w.-]+:)?definitions\b/, match => `${match} xmlns:${prefix}="${uri}"`)
  }, xml)
}

function findElementByLocalName(doc, localName) {
  return Array.from(doc.getElementsByTagName('*')).find(el => el.localName === localName) || null
}

function findElementsByLocalName(doc, localName) {
  return Array.from(doc.getElementsByTagName('*')).filter(el => el.localName === localName)
}

function isBpmnFlowNode(localName) {
  return localName.endsWith('Event')
    || localName.endsWith('Task')
    || localName.endsWith('Gateway')
    || ['subProcess', 'callActivity'].includes(localName)
}

function layoutBpmnNodes(nodes, flows) {
  const nodeMap = new Map(nodes.map(node => [node.id, node]))
  const rankMap = new Map()
  const outgoing = new Map()
  const incomingCount = new Map(nodes.map(node => [node.id, 0]))

  flows.forEach((flow) => {
    if (!nodeMap.has(flow.sourceRef) || !nodeMap.has(flow.targetRef))
      return
    if (!outgoing.has(flow.sourceRef))
      outgoing.set(flow.sourceRef, [])
    outgoing.get(flow.sourceRef).push(flow.targetRef)
    incomingCount.set(flow.targetRef, (incomingCount.get(flow.targetRef) || 0) + 1)
  })

  const startNodes = nodes.filter(node => node.type === 'startEvent' || incomingCount.get(node.id) === 0)
  const queue = startNodes.length > 0 ? [...startNodes] : [nodes[0]]
  queue.forEach(node => rankMap.set(node.id, 0))

  while (queue.length > 0) {
    const node = queue.shift()
    const nextRank = (rankMap.get(node.id) || 0) + 1
    ;(outgoing.get(node.id) || []).forEach((targetId) => {
      if (rankMap.has(targetId))
        return
      rankMap.set(targetId, nextRank)
      queue.push(nodeMap.get(targetId))
    })
  }

  nodes.forEach((node, index) => {
    if (!rankMap.has(node.id))
      rankMap.set(node.id, index)
  })

  const rankCounts = new Map()
  const bounds = new Map()
  nodes.forEach((node) => {
    const rank = rankMap.get(node.id)
    const lane = rankCounts.get(rank) || 0
    rankCounts.set(rank, lane + 1)
    const size = getBpmnNodeSize(node.type)
    bounds.set(node.id, {
      x: 100 + rank * 180,
      y: 120 + lane * 140,
      ...size,
    })
  })
  return bounds
}

function getBpmnNodeSize(type) {
  if (type.endsWith('Gateway'))
    return { width: 50, height: 50 }
  if (type.endsWith('Event'))
    return { width: 36, height: 36 }
  return { width: 100, height: 80 }
}

function buildDiagramXml(processId, nodes, flows, bounds) {
  const lines = [
    '  <bpmndi:BPMNDiagram id="BPMNDiagram_1">',
    `    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="${escapeXmlAttr(processId)}">`,
  ]

  nodes.forEach((node) => {
    const b = bounds.get(node.id)
    lines.push(`      <bpmndi:BPMNShape id="${escapeXmlAttr(node.id)}_di" bpmnElement="${escapeXmlAttr(node.id)}">`)
    lines.push(`        <dc:Bounds x="${b.x}" y="${b.y}" width="${b.width}" height="${b.height}" />`)
    lines.push('      </bpmndi:BPMNShape>')
  })

  flows.forEach((flow) => {
    const source = bounds.get(flow.sourceRef)
    const target = bounds.get(flow.targetRef)
    if (!source || !target)
      return
    const waypoints = buildWaypoints(source, target)
    lines.push(`      <bpmndi:BPMNEdge id="${escapeXmlAttr(flow.id)}_di" bpmnElement="${escapeXmlAttr(flow.id)}">`)
    waypoints.forEach(point => lines.push(`        <di:waypoint x="${point.x}" y="${point.y}" />`))
    lines.push('      </bpmndi:BPMNEdge>')
  })

  lines.push('    </bpmndi:BPMNPlane>')
  lines.push('  </bpmndi:BPMNDiagram>')
  return lines.join('\n')
}

function buildWaypoints(source, target) {
  const sourceRight = { x: source.x + source.width, y: source.y + Math.round(source.height / 2) }
  const targetLeft = { x: target.x, y: target.y + Math.round(target.height / 2) }
  if (targetLeft.x > sourceRight.x) {
    return [sourceRight, targetLeft]
  }

  const sourceBottom = { x: source.x + Math.round(source.width / 2), y: source.y + source.height }
  const targetBottom = { x: target.x + Math.round(target.width / 2), y: target.y + target.height }
  const routeY = Math.max(sourceBottom.y, targetBottom.y) + 40
  return [
    sourceBottom,
    { x: sourceBottom.x, y: routeY },
    { x: targetBottom.x, y: routeY },
    targetBottom,
  ]
}

function escapeXmlAttr(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
}

async function handleBpmnChange(xml) {
  if (xml) {
    bpmnXml.value = xml
    hasChanges.value = true
  }
  else {
    try {
      const newXml = await modelerRef.value?.getXML(true)
      if (newXml) {
        bpmnXml.value = newXml
        hasChanges.value = true
      }
    }
    catch (error) {
      console.error('获取 XML 失败:', error)
    }
  }
}

function handleDiagramImportStart() {
  diagramLoadingCount.value += 1
}

function handleDiagramImportEnd() {
  diagramLoadingCount.value = Math.max(0, diagramLoadingCount.value - 1)
}

async function handleSaveDraft() {
  try {
    saving.value = true

    normalizeBusinessGlobalFormBeforeSave()
    const xml = await getXmlForSave()

    if (!xml) {
      window.$message?.warning('流程图验证失败，请检查后重试')
      return
    }

    const data = {
      ...modelInfo,
      bpmnXml: xml,
      formJson: modelInfo.formJson || (formSchema.value.length > 0 ? JSON.stringify(formSchema.value) : ''),
    }

    let res
    if (modelInfo.id) {
      res = await flowApi.updateModel(data)
    }
    else {
      res = await flowApi.createModel(data)
      if (res.code === 200 && res.data) {
        modelInfo.id = res.data.id
      }
    }

    if (res.code === 200) {
      window.$message?.success('保存成功')
      hasChanges.value = false
      emit('saved', { ...modelInfo })
    }
    else {
      window.$message?.error(res.message || '保存失败')
    }
  }
  catch (error) {
    console.error('保存失败:', error)
    window.$message?.error('保存失败')
  }
  finally {
    saving.value = false
  }
}

function normalizeBusinessGlobalFormBeforeSave() {
  if (!businessFormConfigActive.value)
    return
  if (modelInfo.formType === 'none' || modelInfo.formType === 'external')
    return
  if (!businessContextActive.value)
    return
  if (isAppManagedFormType(modelInfo.formType) && routeQueryText(parseBusinessGlobalFormRef(modelInfo.formJson).formKey))
    return
  ensureBusinessGlobalFormSelection()
  if (isAppManagedFormType(modelInfo.formType) && routeQueryText(parseBusinessGlobalFormRef(modelInfo.formJson).formKey))
    return
  modelInfo.formType = normalizeAppManagedFormType(modelInfo.formType)
  modelInfo.formId = null
  modelInfo.formUrl = ''
  modelInfo.formJson = ''
  formSchema.value = []
}

async function handleDeploy() {
  try {
    deploying.value = true

    const xml = await getXmlForSave()
    if (!xml) {
      window.$message?.error('流程图验证失败，无法部署。请检查：\n1. 所有连接线是否完整连接\n2. 是否包含开始和结束节点')
      return
    }

    await handleSaveDraft()

    if (!modelInfo.id) {
      window.$message?.error('请先保存模型')
      return
    }

    const res = await flowApi.deployModel(modelInfo.id)
    if (res.code === 200) {
      window.$message?.success('部署成功')
      await loadModel(modelInfo.id)
      emit('deployed', { ...modelInfo })
    }
    else {
      window.$message?.error(res.message || '部署失败')
    }
  }
  catch (error) {
    console.error('部署失败:', error)
    window.$message?.error('部署失败')
  }
  finally {
    deploying.value = false
  }
}

function handleBack() {
  const close = () => {
    if (props.embedded)
      emit('close')
    else
      router.back()
  }

  if (hasChanges.value) {
    window.$dialog?.warning({
      title: '提示',
      content: '有未保存的更改，确定要离开吗？',
      positiveText: '保存并离开',
      negativeText: '直接离开',
      onPositiveClick: async () => {
        await handleSaveDraft()
        close()
      },
      onNegativeClick: () => {
        close()
      },
    })
  }
  else {
    close()
  }
}

function handleModelerReady(modeler) {
  modelerInstance.value = modeler || modelerRef.value?.modeler?.() || null
}

function handleBusinessElementSelect(element) {
  if (!isBusinessDesigner.value) {
    dockedElement.value = null
    return
  }
  if (businessSelectionClearTimer) {
    clearTimeout(businessSelectionClearTimer)
    businessSelectionClearTimer = null
  }
  if (element) {
    dockedElement.value = element
    return
  }
  businessSelectionClearTimer = setTimeout(() => {
    dockedElement.value = null
    businessSelectionClearTimer = null
  }, 80)
}

function handleBusinessPanelClose() {
  if (businessSelectionClearTimer) {
    clearTimeout(businessSelectionClearTimer)
    businessSelectionClearTimer = null
  }
  dockedElement.value = null
  modelerRef.value?.clearSelection?.()
}

function getElementTitle(el) {
  if (!el)
    return '属性设置'
  const typeNames = {
    'bpmn:StartEvent': '开始节点',
    'bpmn:EndEvent': '结束节点',
    'bpmn:UserTask': '用户任务',
    'bpmn:ServiceTask': '服务任务',
    'bpmn:ScriptTask': '脚本任务',
    'bpmn:BusinessRuleTask': '业务规则任务',
    'bpmn:ManualTask': '手工任务',
    'bpmn:ExclusiveGateway': '排他网关',
    'bpmn:ParallelGateway': '并行网关',
    'bpmn:InclusiveGateway': '包容网关',
    'bpmn:SequenceFlow': '序列流',
    'bpmn:SubProcess': '子流程',
    'bpmn:CallActivity': '调用活动',
  }
  return el.businessObject?.name || typeNames[el.type] || '属性设置'
}

function getElementIcon(el) {
  const iconMap = {
    'bpmn:StartEvent': 'i-material-symbols:play-circle-outline',
    'bpmn:EndEvent': 'i-material-symbols:stop-circle-outline',
    'bpmn:UserTask': 'i-material-symbols:person-check-outline',
    'bpmn:ServiceTask': 'i-material-symbols:settings-outline',
    'bpmn:ScriptTask': 'i-material-symbols:code-blocks-outline',
    'bpmn:BusinessRuleTask': 'i-material-symbols:rule-settings-outline',
    'bpmn:ManualTask': 'i-material-symbols:pan-tool-outline',
    'bpmn:ExclusiveGateway': 'i-material-symbols:conversion-path-outline',
    'bpmn:ParallelGateway': 'i-material-symbols:call-split-outline',
    'bpmn:InclusiveGateway': 'i-material-symbols:merge-type-outline',
    'bpmn:SequenceFlow': 'i-material-symbols:arrow-right-alt',
    'bpmn:SubProcess': 'i-material-symbols:account-tree-outline',
    'bpmn:CallActivity': 'i-material-symbols:call-made',
  }
  return iconMap[el?.type] || 'i-material-symbols:tune'
}
</script>

<style scoped>
.model-design-page {
  height: 100%;
  max-height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f8fafc;
}

.top-bar {
  height: 52px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.top-bar-left,
.top-bar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.top-bar-left {
  flex: 1;
  min-width: 0;
}

.workspace-tabs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 36px;
  padding: 3px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #f8fafc;
}

.workspace-tab {
  height: 28px;
  min-width: 96px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 12px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background 150ms ease,
    color 150ms ease,
    box-shadow 150ms ease;
}

.workspace-tab i {
  font-size: 17px;
}

.workspace-tab:hover {
  color: #0f172a;
  background: #eef2f7;
}

.workspace-tab.active {
  color: #2563eb;
  background: #fff;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.08),
    inset 0 0 0 1px #bfdbfe;
}

.business-context-banner {
  min-height: 38px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
  color: #0f766e;
  background: #ecfdf5;
  border-bottom: 1px solid #bbf7d0;
  font-size: 13px;
}

.business-context-banner__main {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.business-context-banner__main strong {
  min-width: 0;
  overflow: hidden;
  color: #064e3b;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

.main-content.is-settings {
  background: #f3f6fb;
}

.center-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.designer-container {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: #f8fafc;
  position: relative;
}

.business-properties-panel {
  position: absolute;
  top: 12px;
  right: 0;
  bottom: 12px;
  z-index: 70;
  width: 520px;
  max-width: min(520px, calc(100vw - 40px));
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #dbe3ee;
  border-right: none;
  border-radius: 8px 0 0 8px;
  background: #fff;
  box-shadow: -10px 0 24px rgba(15, 23, 42, 0.12);
  contain: layout paint;
  overflow: hidden;
}

.business-properties-panel__body {
  flex: 1;
  min-height: 0;
}

.designer-loading-mask {
  position: absolute;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(248, 250, 252, 0.82);
  backdrop-filter: blur(3px);
}

.ai-generating-mask {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(248, 250, 252, 0.78);
  backdrop-filter: blur(2px);
  pointer-events: auto;
}

.ai-generating-panel {
  width: min(420px, 92%);
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.14);
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  text-align: center;
}

.ai-generating-badge {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef2ff;
  color: #4f46e5;
  box-shadow: inset 0 0 0 1px #c7d2fe;
}

.ai-generating-icon {
  font-size: 24px;
  animation: aiGeneratingPulse 1.4s ease-in-out infinite;
}

.ai-generating-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ai-generating-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.ai-generating-desc {
  font-size: 13px;
  line-height: 1.6;
  color: #475569;
}

.ai-generating-stage-list {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.ai-generating-stage {
  min-width: 0;
  height: 30px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 0 6px;
  font-size: 11px;
  color: #64748b;
  background: #f8fafc;
  white-space: nowrap;
}

.ai-generating-stage.done {
  border-color: #bbf7d0;
  color: #047857;
  background: #f0fdf4;
}

.ai-generating-stage.active {
  border-color: #c7d2fe;
  color: #4338ca;
  background: #eef2ff;
  font-weight: 600;
}

.ai-generating-stage-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #cbd5e1;
  flex-shrink: 0;
}

.ai-generating-stage.done .ai-generating-stage-dot {
  background: #10b981;
}

.ai-generating-stage.active .ai-generating-stage-dot {
  background: #6366f1;
  animation: aiGeneratingPulse 1.4s ease-in-out infinite;
}

@keyframes aiGeneratingPulse {
  0%,
  100% {
    opacity: 0.72;
    transform: scale(1);
  }

  50% {
    opacity: 1;
    transform: scale(1.08);
  }
}

.designer-loading-fade-enter-active,
.designer-loading-fade-leave-active {
  transition: opacity 180ms ease;
}

.designer-loading-fade-enter-from,
.designer-loading-fade-leave-to {
  opacity: 0;
}

.panel-tab-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #6366f1;
  animation: dotPulse 1.5s ease-in-out infinite;
}

@keyframes dotPulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

.settings-workspace::-webkit-scrollbar,
.settings-tree-nav::-webkit-scrollbar,
.settings-section-pane::-webkit-scrollbar,
.ai-flow-body::-webkit-scrollbar,
.model-list::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}

.settings-workspace::-webkit-scrollbar-track,
.settings-tree-nav::-webkit-scrollbar-track,
.settings-section-pane::-webkit-scrollbar-track,
.ai-flow-body::-webkit-scrollbar-track,
.model-list::-webkit-scrollbar-track {
  background: transparent;
}

.settings-workspace::-webkit-scrollbar-thumb,
.settings-tree-nav::-webkit-scrollbar-thumb,
.settings-section-pane::-webkit-scrollbar-thumb,
.ai-flow-body::-webkit-scrollbar-thumb,
.model-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 2px;
}

.settings-tip {
  border-left: 3px solid #2563eb;
  border-radius: 6px;
  padding: 10px 12px;
  background: #eff6ff;
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

.settings-workspace {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
  background: #f3f6fb;
}

.settings-tree-nav {
  width: 236px;
  flex: 0 0 236px;
  min-height: 0;
  padding: 14px 12px;
  border-left: 1px solid #dbe3ee;
  background: #f8fafc;
  overflow-y: auto;
}

.settings-tree-group + .settings-tree-group {
  margin-top: 14px;
}

.settings-tree-group-title {
  padding: 0 6px 6px;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
}

.settings-tree-item {
  position: relative;
  width: 100%;
  min-height: 58px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 9px 8px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 150ms ease,
    background 150ms ease,
    box-shadow 150ms ease;
}

.settings-tree-item + .settings-tree-item {
  margin-top: 6px;
}

.settings-tree-item:hover {
  border-color: #dbe3ee;
  background: #fff;
}

.settings-tree-item.active {
  border-color: #bfdbfe;
  background: #fff;
  box-shadow: inset 3px 0 0 #2563eb;
}

.settings-tree-item i {
  flex-shrink: 0;
  margin-top: 2px;
  color: #64748b;
  font-size: 18px;
}

.settings-tree-item.active i,
.settings-tree-item.active .settings-tree-item-title {
  color: #2563eb;
}

.settings-tree-item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.settings-tree-item-title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.settings-tree-item-desc {
  color: #64748b;
  font-size: 11px;
  line-height: 1.4;
}

.settings-tree-badge {
  max-width: 52px;
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #475569;
  font-size: 10px;
  line-height: 17px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.settings-tree-badge.is-success {
  background: #dcfce7;
  color: #15803d;
}

.settings-tree-badge.is-warning {
  background: #fef3c7;
  color: #b45309;
}

.settings-tree-badge.is-default {
  background: #e2e8f0;
  color: #475569;
}

.settings-tree-badge.is-info {
  background: #dbeafe;
  color: #2563eb;
}

.settings-detail-pane {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
}

.settings-section-pane {
  flex: 1;
  min-height: 0;
  width: min(760px, 100%);
  align-self: center;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px 28px;
  overflow-y: auto;
}

.settings-pane-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.settings-pane-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.settings-pane-desc {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.settings-field {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.settings-field-label {
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.settings-form-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.approval-setting-row,
.approval-setting-block {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  padding: 14px 16px;
}

.approval-setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.approval-setting-main {
  min-width: 0;
}

.approval-setting-title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.approval-setting-desc {
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.approval-mode-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.approval-mode-option {
  width: 100%;
  min-height: 64px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 150ms ease,
    background 150ms ease,
    box-shadow 150ms ease;
}

.approval-mode-option:hover:not(:disabled) {
  border-color: #93c5fd;
  background: #f8fbff;
}

.approval-mode-option.active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: inset 3px 0 0 #2563eb;
}

.approval-mode-option:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.approval-mode-check {
  width: 18px;
  height: 18px;
  flex: 0 0 18px;
  margin-top: 1px;
  border: 1px solid #cbd5e1;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: #fff;
}

.approval-mode-option.active .approval-mode-check {
  border-color: #2563eb;
  background: #2563eb;
}

.approval-mode-check i {
  font-size: 16px;
}

.approval-mode-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.approval-mode-title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
}

.approval-mode-desc {
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.ai-flow-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ai-flow-header {
  padding: 10px 14px;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.ai-flow-header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ai-flow-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e1b4b;
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-flow-title-icon {
  color: #6366f1;
  font-size: 18px;
}

.ai-flow-subtitle {
  font-size: 12px;
  color: #64748b;
}

.ai-stage-progress {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  gap: 4px;
  flex-shrink: 0;
  overflow-x: auto;
}

.ai-stage-step {
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.ai-stage-step-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e2e8f0;
  transition: all 200ms ease;
  flex-shrink: 0;
}

.ai-stage-step.done .ai-stage-step-dot {
  background: #10b981;
}

.ai-stage-step.active .ai-stage-step-dot {
  background: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
}

.ai-stage-step-label {
  font-size: 11px;
  color: #94a3b8;
}

.ai-stage-step.done .ai-stage-step-label {
  color: #10b981;
}

.ai-stage-step.active .ai-stage-step-label {
  color: #6366f1;
  font-weight: 600;
}

.ai-stage-step + .ai-stage-step::before {
  content: '';
  display: block;
  width: 16px;
  height: 1px;
  background: #e2e8f0;
  margin-right: 4px;
}

.ai-flow-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 10px 12px;
  background: #f8fafc;
  scroll-behavior: smooth;
}

.ai-flow-body::-webkit-scrollbar {
  width: 4px;
}

.ai-flow-body::-webkit-scrollbar-track {
  background: transparent;
}

.ai-flow-body::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 2px;
}

.ai-flow-empty {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-empty-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 20px 0;
}

.ai-empty-icon {
  font-size: 32px;
  color: #6366f1;
}

.ai-empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e1b4b;
}

.ai-empty-tip {
  font-size: 12px;
  color: #64748b;
}

.ai-example-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-example {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  padding: 10px 12px;
  cursor: pointer;
  transition:
    border-color 150ms ease,
    box-shadow 150ms ease;
}

.ai-example:hover {
  border-color: #6366f1;
  box-shadow: 0 1px 3px rgba(99, 102, 241, 0.1);
}

.ai-example-icon {
  color: #6366f1;
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 1px;
}

.ai-example-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.ai-example-label {
  font-size: 13px;
  font-weight: 600;
  color: #1e1b4b;
}

.ai-example-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.ai-message-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ai-message-end {
  height: 1px;
}

.ai-message {
  display: flex;
  gap: 8px;
}

.ai-message.user {
  flex-direction: row-reverse;
}

.ai-message-avatar {
  flex-shrink: 0;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
}

.user-avatar {
  background: #6366f1;
  color: #fff;
}

.ai-avatar {
  background: #eef2ff;
  color: #6366f1;
}

.ai-message-body {
  max-width: 85%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ai-message.user .ai-message-body {
  align-items: flex-end;
}

.reasoning-section {
  border: 1px solid #e0e7ff;
  border-radius: 8px;
  background: #f5f3ff;
  overflow: hidden;
}

.reasoning-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  cursor: pointer;
  transition: background 150ms ease;
}

.reasoning-header:hover {
  background: #ede9fe;
}

.reasoning-header-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.reasoning-icon {
  color: #6366f1;
  font-size: 16px;
}

.reasoning-label {
  font-size: 12px;
  font-weight: 600;
  color: #4f46e5;
}

.reasoning-duration {
  font-size: 11px;
  color: #818cf8;
}

.reasoning-duration.thinking {
  color: #6366f1;
  animation: dotPulse 1.5s ease-in-out infinite;
}

.reasoning-toggle {
  color: #94a3b8;
  font-size: 18px;
  transition: transform 200ms ease;
}

.reasoning-toggle.expanded {
  transform: rotate(180deg);
}

.reasoning-content {
  padding: 8px 10px;
  font-size: 12px;
  color: #4f46e5;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  border-top: 1px solid #e0e7ff;
  background: #ede9fe;
  max-height: 200px;
  overflow-y: auto;
}

.ai-message-content {
  border-radius: 8px;
  padding: 8px 10px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  background: #fff;
  color: #1e1b4b;
  border: 1px solid #e2e8f0;
  font-size: 13px;
}

.ai-message.user .ai-message-content {
  background: #eef2ff;
  border-color: #c7d2fe;
}

.ai-message-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.message-typing {
  display: flex;
  gap: 4px;
  padding-top: 6px;
}

.message-typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #6366f1;
  animation: typingBounce 1.4s ease-in-out infinite;
}

.message-typing span:nth-child(2) {
  animation-delay: 0.2s;
}

.message-typing span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typingBounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-6px);
    opacity: 1;
  }
}

.ai-draft {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  background: #f0fdf4;
}

.ai-draft-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
  font-weight: 700;
  color: #14532d;
  font-size: 13px;
}

.ai-draft-desc {
  margin-bottom: 10px;
  color: #166534;
  font-size: 12px;
  line-height: 1.5;
}

.xml-preview-container {
  max-height: 60vh;
  overflow: auto;
  background: #f8f9fa;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
}

:deep(.xml-preview-container .n-code) {
  background: transparent;
}

:deep(.xml-preview-container .n-code pre) {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  color: #1e1e1e;
  background: transparent;
}

:deep(.xml-preview-container .n-code .hljs) {
  background: transparent;
}

.ai-flow-input {
  flex-shrink: 0;
  padding: 10px 12px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

.ai-flow-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.ai-actions-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-actions-right {
  display: flex;
  align-items: center;
}

.model-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
  color: #64748b;
  transition: all 150ms ease;
}

.model-trigger:hover {
  border-color: #6366f1;
  color: #6366f1;
}

.model-trigger.active {
  border-color: #6366f1;
  background: #eef2ff;
  color: #6366f1;
}

.model-trigger.empty {
  border-color: #fca5a5;
  color: #ef4444;
}

.model-trigger-icon {
  font-size: 14px;
  color: #6366f1;
}

.model-trigger-provider {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-trigger-divider {
  color: #cbd5e1;
}

.model-trigger-model {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}

.model-trigger-chevron {
  font-size: 16px;
  color: #94a3b8;
}

.model-panel {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.model-panel-section {
  padding: 10px;
}

.model-panel-section + .model-panel-section {
  border-top: 1px solid #e2e8f0;
}

.model-panel-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.model-panel-empty-tip {
  font-size: 11px;
  color: #ef4444;
  font-weight: 400;
}

.model-panel-count {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 400;
}

.model-panel-empty {
  padding: 12px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}

.model-list {
  max-height: 200px;
  overflow-y: auto;
}

.model-list-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 150ms ease;
}

.model-list-item:hover {
  background: #f1f5f9;
}

.model-list-item.active {
  background: #eef2ff;
}

.model-list-item-main {
  display: flex;
  align-items: center;
  gap: 6px;
}

.model-list-item-name {
  font-size: 13px;
  font-weight: 500;
  color: #1e1b4b;
}

.model-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: #eef2ff;
  color: #6366f1;
  font-weight: 600;
}

.model-list-item-desc {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.flow-form-designer-modal-body {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

@media (max-width: 1280px) {
  .settings-tree-nav {
    width: 212px;
    flex-basis: 212px;
  }
}

@media (max-width: 1024px) {
  .top-bar {
    padding: 0 12px;
    height: 48px;
  }

  .top-bar-left {
    gap: 6px;
  }

  .center-area {
    min-height: 0;
  }

  .workspace-tab {
    min-width: 86px;
    padding: 0 10px;
  }

  .settings-tree-nav {
    width: 196px;
    flex-basis: 196px;
    padding: 12px 10px;
  }

  .settings-section-pane {
    padding: 20px;
  }
}

@media (max-width: 768px) {
  .top-bar {
    align-items: stretch;
    height: auto;
    min-height: 52px;
    padding: 8px 10px;
    gap: 8px;
  }

  .top-bar-left,
  .top-bar-right {
    flex-wrap: wrap;
    gap: 6px;
  }

  .workspace-tabs {
    order: 3;
    width: 100%;
  }

  .workspace-tab {
    flex: 1;
  }

  .settings-workspace {
    flex-direction: column-reverse;
  }

  .settings-tree-nav {
    width: 100%;
    flex: 0 0 auto;
    display: flex;
    gap: 8px;
    padding: 8px;
    border-left: none;
    border-bottom: 1px solid #e2e8f0;
    overflow-x: auto;
    overflow-y: hidden;
  }

  .settings-tree-group {
    min-width: 220px;
  }

  .settings-tree-group + .settings-tree-group {
    margin-top: 0;
  }
}

@media (max-height: 760px) {
  .ai-flow-header {
    padding: 7px 10px;
  }

  .ai-flow-subtitle {
    display: none;
  }

  .ai-stage-progress {
    padding: 5px 10px;
  }

  .ai-flow-body {
    padding: 8px 10px;
  }

  .ai-flow-input {
    padding: 7px 9px;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

:deep(button:focus-visible),
:deep(input:focus-visible),
:deep(select:focus-visible),
:deep(textarea:focus-visible) {
  outline: 2px solid #6366f1;
  outline-offset: 2px;
}
</style>
