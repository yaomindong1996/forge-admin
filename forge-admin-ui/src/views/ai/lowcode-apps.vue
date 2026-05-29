<template>
  <div class="lowcode-domain-page">
    <DomainTreePanel
      v-model:keyword="domainKeyword"
      :domains="domains"
      :selected-domain-id="selectedDomainId"
      :loading="domainLoading"
      @select="selectDomain"
      @search="loadDomains"
      @refresh="refreshDomains"
      @create="openDomainEditor(null)"
      @delete="deleteDomain"
    />

    <main class="domain-main">
      <div class="main-toolbar">
        <div>
          <h1>{{ pageTitle }}</h1>
          <p>{{ pageSubtitle }}</p>
        </div>
        <n-space>
          <n-button
            @click="openAiCreateApp"
          >
            <template #icon>
              <n-icon><SparklesOutline /></n-icon>
            </template>
            AI 智能开发
          </n-button>
          <n-button @click="router.push('/ai/lowcode-models')">
            数据模型设计
          </n-button>
          <n-button @click="triggerAppImport">
            导入配置
          </n-button>
          <n-button @click="refreshAll">
            刷新
          </n-button>
          <n-button type="primary" :disabled="selectedDomain?.status === 'DISABLED'" @click="createApp(selectedDomain)">
            新建应用
          </n-button>
        </n-space>
      </div>

      <section class="apps-board">
        <div class="apps-board-head">
          <div>
            <h2>低代码应用</h2>
            <p>{{ appScopeText }}</p>
          </div>
          <div class="filter-strip">
            <n-input
              v-model:value="keyword"
              clearable
              placeholder="搜索应用 / 表名 / configKey"
              @update:value="handleSearch"
            />
            <n-select
              v-model:value="publishStatus"
              clearable
              placeholder="发布状态"
              :options="statusOptions"
              @update:value="handleSearch"
            />
          </div>
        </div>

        <n-spin :show="loading">
          <div v-if="apps.length" class="apps-grid">
            <article v-for="app in apps" :key="app.id" class="app-card">
              <div class="app-topline">
                <n-tag size="small" :type="app.publishStatus === 'PUBLISHED' ? 'success' : 'warning'" :bordered="false">
                  {{ statusLabel(app.publishStatus) }}
                </n-tag>
                <span>{{ formatTime(app.updateTime) }}</span>
              </div>
              <div class="app-title-row">
                <div class="app-mark">
                  {{ appInitial(app) }}
                </div>
                <div>
                  <h3>{{ app.appName || app.tableComment || app.configKey }}</h3>
                  <p>{{ app.objectName || app.tableComment || '未命名对象' }}</p>
                </div>
              </div>
              <div class="app-code-grid">
                <div>
                  <span>领域</span>
                  <code>{{ app.domainName || app.domainCode || '-' }}</code>
                </div>
                <div>
                  <span>对象编码</span>
                  <code>{{ app.objectCode || '-' }}</code>
                </div>
                <div>
                  <span>配置键</span>
                  <code>{{ app.configKey }}</code>
                </div>
                <div>
                  <span>数据表</span>
                  <code>{{ app.tableName || '-' }}</code>
                </div>
              </div>
              <div class="app-version-row">
                <span>草稿 v{{ app.draftVersion || 0 }}</span>
                <span>发布 v{{ app.publishedVersion || 0 }}</span>
              </div>
              <div class="app-actions">
                <n-button size="small" type="primary" @click="openBuilder(app.id)">
                  搭建
                </n-button>
                <n-button size="small" :disabled="app.publishStatus !== 'PUBLISHED'" @click="openRuntime(app.configKey)">
                  打开
                </n-button>
                <n-dropdown
                  trigger="click"
                  :options="getAppActionOptions(app)"
                  @select="key => handleAppActionSelect(key, app)"
                >
                  <n-button size="small" class="more-action-button">
                    <template #icon>
                      <n-icon><EllipsisVertical /></n-icon>
                    </template>
                  </n-button>
                </n-dropdown>
              </div>
            </article>
          </div>
          <n-empty v-else-if="!loading" description="当前筛选下暂无低代码应用">
            <template #extra>
              <n-button type="primary" :disabled="selectedDomain?.status === 'DISABLED'" @click="createApp(selectedDomain)">
                新建应用
              </n-button>
            </template>
          </n-empty>
        </n-spin>

        <div v-if="total > 0" class="apps-pagination">
          <n-pagination
            v-model:page="pageNum"
            v-model:page-size="pageSize"
            :item-count="total"
            :page-sizes="[9, 18, 36]"
            show-size-picker
            @update:page="loadApps"
            @update:page-size="handlePageSizeChange"
          />
        </div>
      </section>
    </main>

    <DomainEditorDrawer
      v-model:show="domainEditorVisible"
      :domain="editingDomain"
      :domains="domains"
      @saved="handleDomainSaved"
    />

    <MoveDomainModal
      v-model:show="moveVisible"
      :app="movingApp"
      :domains="domains"
      @moved="handleMoved"
    />
    <LowcodeCodePreviewModal
      v-model:show="codePreviewVisible"
      :app-id="codePreviewApp?.id"
      :app-name="codePreviewApp?.appName || codePreviewApp?.configKey"
    />
    <n-modal
      v-model:show="aiCreateVisible"
      class="lc-agent-modal-card"
      title="AI 建模向导"
      preset="card"
      style="width: min(1280px, calc(100vw - 24px))"
      :mask-closable="false"
      @update:show="handleAiCreateVisibleUpdate"
    >
      <div class="lc-agent-modal">
        <header class="lc-agent-topbar">
          <div class="lc-agent-brand">
            <span class="lc-agent-logo">AI</span>
            <div>
              <strong>AI 建模向导</strong>
              <span>从需求到应用，一键生成</span>
            </div>
          </div>
          <div class="lc-agent-top-actions">
            <n-button size="small" class="lc-save-indicator">
              <template #icon>
                <n-icon><SaveOutline /></n-icon>
              </template>
              用户确认后保存
            </n-button>
            <n-button size="small" quaternary class="lc-agent-ghost-action" @click="closeAiCreateModal">
              关闭
            </n-button>
          </div>
        </header>

        <section class="lc-agent-input">
          <div class="lc-agent-input-head">
            <div>
              <strong>业务需求</strong>
              <span>描述业务范围、对象、字段、流程、查询条件和页面预期</span>
            </div>
            <div class="lc-agent-chip-row">
              <n-button size="tiny" class="lc-helper-button">
                智能提示词示例
              </n-button>
              <n-button size="tiny" class="lc-helper-button">
                自动识别字段 / 补全需求
              </n-button>
            </div>
          </div>

          <div class="lc-agent-input-grid">
            <n-form class="lc-agent-form" label-placement="top" size="small" :show-feedback="false">
              <n-form-item label="供应商">
                <n-select
                  v-model:value="aiProviderId"
                  clearable
                  filterable
                  :loading="aiProviderLoading"
                  :options="aiProviderOptions"
                  placeholder="使用 Agent 默认供应商"
                  @update:value="handleAiProviderChange"
                />
              </n-form-item>

              <n-form-item label="模型">
                <n-select
                  v-model:value="aiModelId"
                  clearable
                  filterable
                  :loading="aiModelLoading"
                  :options="aiModelOptions"
                  placeholder="使用供应商默认模型"
                />
              </n-form-item>

              <n-form-item label="目标业务领域">
                <n-select
                  v-model:value="aiTargetDomainId"
                  clearable
                  filterable
                  :options="domainSelectOptions"
                  placeholder="不选择则由 AI 自动划分业务领域"
                  @update:value="handleAiTargetDomainChange"
                />
              </n-form-item>

              <n-form-item v-if="aiTargetDomainId" label="已有模型上下文">
                <div class="lc-domain-model-context">
                  <n-checkbox v-model:checked="aiIncludeDomainModels" :disabled="domainModelLoading">
                    携带当前领域已有数据模型
                  </n-checkbox>
                  <n-tag size="small" :bordered="false" :type="domainModelContext.length ? 'success' : 'default'">
                    {{ domainModelLoading ? '加载中' : `${domainModelContext.length} 个模型` }}
                  </n-tag>
                </div>
              </n-form-item>
            </n-form>

            <div class="lc-demand-box">
              <div class="lc-demand-head">
                <strong>需求描述</strong>
                <span>{{ aiTargetDomain ? `优先复用“${aiTargetDomain.domainName}”` : 'AI 自动划分业务领域' }}</span>
              </div>
              <n-input
                v-model:value="aiCreateDescription"
                type="textarea"
                :autosize="{ minRows: 5, maxRows: 8 }"
                maxlength="2000"
                show-count
                placeholder="例如：帮我做一个客户合同回款管理系统，需要客户档案、合同、回款记录，支持按客户、状态、日期查询，合同有金额和签订日期，回款记录需要状态跟踪。"
              />
              <div class="lc-agent-command-text">
                {{ aiTargetDomain ? `将优先在已有领域“${aiTargetDomain.domainName}”中生成模型和应用` : '未指定目标领域，AI 会自动划分或复用业务领域' }}
              </div>
            </div>

            <n-button class="lc-agent-start-card" type="primary" :loading="aiCreateLoading" @click="generateAiAppDraft(false)">
              <div class="lc-agent-start-content">
                <span class="lc-agent-start-icon">AI</span>
                <strong>开始生成</strong>
                <span>一键生成应用</span>
              </div>
            </n-button>
          </div>
        </section>

        <section class="lc-agent-workspace">
          <aside class="lc-step-rail">
            <div class="lc-step-rail-head">
              <strong>生成步骤</strong>
              <span>全流程闭环</span>
            </div>
            <div
              v-for="step in aiSteps"
              :key="step.stepKey"
              class="lc-step-item"
              :class="`is-${step.status || 'pending'}`"
            >
              <span class="lc-step-dot">{{ step.orderNo }}</span>
              <div>
                <strong>{{ step.title }}</strong>
                <span>{{ step.message }}</span>
                <small>{{ stepStatusText(step.status) }}</small>
              </div>
            </div>
          </aside>

          <div class="lc-agent-output">
            <div class="lc-output-head">
              <div>
                <strong>生成预览 / 对话流</strong>
                <span>{{ aiCreateLoading ? '模型正在流式生成协议' : aiCreateResult ? '草稿已生成，等待确认保存' : '等待输入业务需求' }}</span>
              </div>
              <n-tag size="small" :bordered="false" :type="aiCreateResult ? 'success' : aiCreateLoading ? 'warning' : 'info'">
                {{ aiCreateResult ? '已生成' : aiCreateLoading ? '生成中' : '待开始' }}
              </n-tag>
            </div>

            <div v-if="!aiCreateResult && !aiCreateLoading && !aiRawContent && !aiReasoningContent" class="lc-agent-welcome">
              <div class="lc-welcome-copy">
                <h3>欢迎使用 AI 建模向导</h3>
                <p>填写并提交业务需求后，AI 将自动完成从需求理解到应用草稿的基础生成。</p>
              </div>
              <div class="lc-capability-grid">
                <div>
                  <span class="lc-capability-icon">理</span>
                  <strong>智能理解</strong>
                  <p>AI 深度理解业务需求，提炼核心要素</p>
                </div>
                <div>
                  <span class="lc-capability-icon">模</span>
                  <strong>自动建模</strong>
                  <p>自动生成数据模型，建立实体关系</p>
                </div>
                <div>
                  <span class="lc-capability-icon">页</span>
                  <strong>页面生成</strong>
                  <p>一键生成应用页面，支持交互预览</p>
                </div>
                <div>
                  <span class="lc-capability-icon">验</span>
                  <strong>协议校验</strong>
                  <p>自动校验低代码协议，确保规范可落地</p>
                </div>
              </div>
            </div>

            <div v-if="aiCreateLoading || aiRawContent || aiReasoningContent" class="lc-stream-panel">
              <div class="lc-stream-section">
                <div class="lc-stream-head">
                  <div>
                    <strong>模型思考过程</strong>
                    <span v-if="aiReasoningTime">用时 {{ aiReasoningTime }}s</span>
                    <span v-else-if="aiIsReasoningPhase">思考中...</span>
                    <span v-else>跟随模型流式输出</span>
                  </div>
                  <n-tag size="small" :bordered="false" :type="aiIsReasoningPhase ? 'warning' : 'default'">
                    {{ aiIsReasoningPhase ? '推理中' : '实时' }}
                  </n-tag>
                </div>
                <pre v-if="aiReasoningContent" class="lc-stream-content">{{ aiReasoningContent }}</pre>
                <div v-else class="lc-stream-empty">
                  {{ aiCreateLoading ? '等待模型返回推理内容；如果当前模型不支持独立推理流，将直接展示结构化输出。' : '当前模型未返回独立推理内容。' }}
                </div>
              </div>

              <div class="lc-stream-section">
                <div class="lc-stream-head">
                  <div>
                    <strong>结构化输出</strong>
                    <span>JSON 协议流会在结束后自动校验并转为草稿预览</span>
                  </div>
                  <n-tag size="small" :bordered="false" type="info">
                    {{ aiRawContent ? '接收中' : '等待' }}
                  </n-tag>
                </div>
                <pre v-if="aiRawContent" class="lc-stream-content">{{ aiRawContent }}</pre>
                <div v-else class="lc-stream-empty">
                  等待模型开始输出领域、模型和应用协议。
                </div>
              </div>
            </div>

            <template v-if="aiCreateResult">
              <div class="lc-result-head">
                <div>
                  <strong>{{ aiCreateResult.requirementSummary || aiCreateResult.appDraft?.appName || '业务系统草稿' }}</strong>
                  <span>确认后将保存 {{ aiGeneratedDomains.length }} 个领域、{{ aiGeneratedModels.length }} 个模型、{{ aiGeneratedApps.length }} 个应用草稿</span>
                </div>
                <n-tag :bordered="false" :type="aiCreateResult.fallback ? 'warning' : 'success'">
                  {{ aiCreateResult.fallback ? '规则 Agent' : 'AI Agent' }}
                </n-tag>
              </div>

              <div class="lc-summary-grid">
                <div>
                  <span>业务领域</span>
                  <strong>{{ aiGeneratedDomains.length }}</strong>
                </div>
                <div>
                  <span>数据模型</span>
                  <strong>{{ aiGeneratedModels.length }}</strong>
                </div>
                <div>
                  <span>应用页面</span>
                  <strong>{{ aiGeneratedApps.length }}</strong>
                </div>
              </div>

              <div class="lc-result-columns">
                <section class="lc-result-section">
                  <h3>业务领域</h3>
                  <div class="lc-mini-list">
                    <div v-for="domain in aiGeneratedDomains" :key="domain.domainCode" class="lc-mini-row">
                      <div>
                        <strong>{{ domain.domainName }}</strong>
                        <span>{{ domain.domainCode }}</span>
                      </div>
                      <n-tag size="small" :bordered="false" :type="domain.existingDomainId ? 'success' : 'info'">
                        {{ domain.existingDomainId ? '复用' : '新建' }}
                      </n-tag>
                    </div>
                  </div>
                </section>

                <section class="lc-result-section">
                  <h3>数据模型</h3>
                  <div class="lc-mini-list">
                    <div v-for="model in aiGeneratedModels" :key="model.modelCode" class="lc-mini-row">
                      <div>
                        <strong>{{ model.modelName }}</strong>
                        <span>{{ model.modelCode }} · {{ model.modelSchema?.fields?.length || 0 }} 字段</span>
                      </div>
                      <n-button size="tiny" @click="activePreviewModelCode = model.modelCode">
                        预览
                      </n-button>
                    </div>
                  </div>
                </section>

                <section class="lc-result-section">
                  <h3>应用草稿</h3>
                  <div class="lc-mini-list">
                    <div v-for="app in aiGeneratedApps" :key="app.configKey" class="lc-mini-row">
                      <div>
                        <strong>{{ app.appName }}</strong>
                        <span>{{ app.configKey }} · {{ layoutName(app.pageSchema?.layoutType) }}</span>
                      </div>
                    </div>
                  </div>
                </section>
              </div>

              <section v-if="activePreviewModel" class="lc-result-section">
                <h3>表模型预览</h3>
                <div class="lc-model-preview-head">
                  <n-select
                    v-model:value="activePreviewModelCode"
                    size="small"
                    :options="previewModelOptions"
                    placeholder="选择模型"
                  />
                  <n-tag size="small" :bordered="false" type="info">
                    {{ activePreviewModel.modelSchema?.tableName || '-' }}
                  </n-tag>
                </div>
                <n-data-table
                  size="small"
                  :bordered="false"
                  :single-line="false"
                  :columns="modelPreviewColumns"
                  :data="activePreviewFields"
                  :pagination="{ pageSize: 8 }"
                />
              </section>

              <section class="lc-result-section">
                <h3>追加优化</h3>
                <div class="lc-refine-box">
                  <n-input
                    v-model:value="aiAppendInstruction"
                    type="textarea"
                    :autosize="{ minRows: 2, maxRows: 4 }"
                    placeholder="基于上面的草稿继续补充，例如：商品分类和商品基本信息放在同一个领域，表名统一以 tf_f_ 开头，商品页面用分类树过滤。"
                  />
                  <n-button :loading="aiCreateLoading" :disabled="!aiCreateResult" @click="generateAiAppDraft(true)">
                    继续优化
                  </n-button>
                </div>
              </section>

              <section v-if="aiDecisions.length" class="lc-result-section">
                <h3>决策摘要</h3>
                <div class="lc-decision-list">
                  <div v-for="decision in aiDecisions" :key="`${decision.decisionType}-${decision.target}-${decision.value}`" class="lc-decision-row">
                    <div>
                      <strong>{{ decision.title }}</strong>
                      <span>{{ decision.target }}：{{ decision.value }}</span>
                    </div>
                    <p>{{ decision.reason }}</p>
                  </div>
                </div>
              </section>

              <ul v-if="aiNotes.length" class="note-list">
                <li v-for="note in aiNotes" :key="note">
                  {{ note }}
                </li>
              </ul>

              <div class="lc-preview-board">
                <article class="lc-preview-card lc-er-preview">
                  <div class="lc-preview-card-head">
                    <div>
                      <strong>ER 图（数据模型）</strong>
                      <span>{{ aiGeneratedModels.length }} 个模型 · {{ aiRelationCount }} 条关系</span>
                    </div>
                  </div>
                  <div v-if="aiGeneratedModels.length" class="lc-er-mini">
                    <span
                      v-for="(model, index) in aiGeneratedModels.slice(0, 3)"
                      :key="model.modelCode || model.modelName || index"
                      :class="{ 'is-primary': index === 0 }"
                    >
                      {{ model.modelName || model.modelCode }}
                    </span>
                    <i v-if="aiGeneratedModels.length > 1" />
                  </div>
                  <div v-else class="lc-preview-empty">
                    暂无生成模型
                  </div>
                  <n-button
                    size="tiny"
                    class="lc-preview-button"
                    :disabled="!aiGeneratedModels.length"
                    @click="openAiPreviewDetail('er')"
                  >
                    查看详情
                  </n-button>
                </article>

                <article class="lc-preview-card">
                  <div class="lc-preview-card-head">
                    <div>
                      <strong>页面结构</strong>
                      <span>{{ aiGeneratedApps.length }} 个页面草稿</span>
                    </div>
                  </div>
                  <div v-if="aiPageStructureRows.length" class="lc-page-structure-list">
                    <div v-for="item in aiPageStructureRows" :key="item.key">
                      <span>{{ item.name }}</span>
                      <small>{{ item.layout }}</small>
                    </div>
                  </div>
                  <div v-else class="lc-preview-empty">
                    暂无生成页面
                  </div>
                  <n-button
                    size="tiny"
                    class="lc-preview-button"
                    :disabled="!aiGeneratedApps.length"
                    @click="openAiPreviewDetail('pages')"
                  >
                    查看详情
                  </n-button>
                </article>

                <article class="lc-preview-card">
                  <div class="lc-preview-card-head">
                    <div>
                      <strong>字段清单（核心实体）</strong>
                      <span>{{ activePreviewModel?.modelName || aiPrimaryModelName }}</span>
                    </div>
                  </div>
                  <div v-if="aiPreviewFields.length" class="lc-field-mini-table">
                    <div v-for="field in aiPreviewFields" :key="field.field">
                      <span>{{ field.label || field.field }}</span>
                      <small>{{ field.dataType || '-' }}</small>
                      <em>{{ field.componentType || '-' }}</em>
                    </div>
                  </div>
                  <div v-else class="lc-preview-empty">
                    暂无字段清单
                  </div>
                  <n-button
                    size="tiny"
                    class="lc-preview-button"
                    :disabled="!activePreviewModel"
                    @click="openAiPreviewDetail('fields')"
                  >
                    查看详情
                  </n-button>
                </article>

                <article class="lc-preview-card">
                  <div class="lc-preview-card-head">
                    <div>
                      <strong>接口建议</strong>
                      <span>{{ aiApiCount }} 个接口建议</span>
                    </div>
                  </div>
                  <div v-if="aiApiPreviewRows.length" class="lc-api-mini-list">
                    <div v-for="api in aiApiPreviewRows" :key="`${api.method}-${api.path}`">
                      <b :class="`is-${api.method.toLowerCase()}`">{{ api.method }}</b>
                      <span>{{ api.path }}</span>
                    </div>
                  </div>
                  <div v-else class="lc-preview-empty">
                    暂无接口建议
                  </div>
                  <n-button
                    size="tiny"
                    class="lc-preview-button"
                    :disabled="!aiApiPreviewRows.length"
                    @click="openAiPreviewDetail('apis')"
                  >
                    查看详情
                  </n-button>
                </article>
              </div>
            </template>
          </div>
        </section>

        <section class="lc-agent-summary-panel">
          <div class="lc-agent-summary-head">
            <strong>生成结果摘要</strong>
            <span>AI 只生成草稿，确认后保存</span>
          </div>
          <div class="lc-agent-summary-metrics">
            <div>
              <span class="lc-summary-icon">域</span>
              <small>业务领域</small>
              <strong>{{ aiPrimaryDomainLabel }}</strong>
            </div>
            <div>
              <span class="lc-summary-icon">体</span>
              <small>核心实体</small>
              <strong>{{ aiGeneratedModels.length || 0 }} 个</strong>
            </div>
            <div>
              <span class="lc-summary-icon">关</span>
              <small>关联关系</small>
              <strong>{{ aiRelationCount }} 条</strong>
            </div>
            <div>
              <span class="lc-summary-icon">页</span>
              <small>应用页面</small>
              <strong>{{ aiGeneratedApps.length || 0 }} 个</strong>
            </div>
            <div>
              <span class="lc-summary-icon">接</span>
              <small>接口数量</small>
              <strong>{{ aiApiCount }} 个</strong>
            </div>
            <div>
              <span class="lc-summary-icon">验</span>
              <small>校验状态</small>
              <strong>{{ aiValidationStatus }}</strong>
            </div>
            <div class="is-progress-metric">
              <span class="lc-summary-progress">{{ aiCompletionPercent }}</span>
              <small>完成进度</small>
              <strong>{{ aiCreateResult ? '可保存' : aiCreateLoading ? '生成中' : '等待开始生成' }}</strong>
            </div>
          </div>
        </section>
      </div>
      <template #footer>
        <n-space class="lc-agent-footer-actions" justify="end">
          <n-button class="lc-agent-secondary-button" @click="closeAiCreateModal">
            取消
          </n-button>
          <n-button
            class="lc-agent-confirm-button"
            type="primary"
            :loading="aiConfirmSaving"
            :disabled="!aiCreateResult || aiCreateLoading"
            @click="confirmAiAppDraft"
          >
            确认并保存草稿
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal
      v-model:show="aiPreviewDetailVisible"
      preset="card"
      class="lc-ai-preview-detail-modal"
      :title="aiPreviewDetailTitle"
      style="width: min(960px, calc(100vw - 32px))"
    >
      <div class="lc-preview-detail">
        <template v-if="aiPreviewDetailType === 'er'">
          <div class="lc-detail-model-grid">
            <article v-for="model in aiGeneratedModels" :key="model.modelCode || model.modelName" class="lc-detail-model-card">
              <div>
                <strong>{{ model.modelName || model.modelCode }}</strong>
                <span>{{ model.modelCode }} · {{ model.modelSchema?.tableName || '-' }}</span>
              </div>
              <n-tag size="small" :bordered="false" type="info">
                {{ model.modelSchema?.fields?.length || 0 }} 字段
              </n-tag>
            </article>
          </div>
          <section class="lc-detail-section">
            <h3>关联关系</h3>
            <div v-if="aiModelRelationRows.length" class="lc-detail-relation-list">
              <div v-for="relation in aiModelRelationRows" :key="relation.key">
                <strong>{{ relation.source }}</strong>
                <span>{{ relation.type }}</span>
                <strong>{{ relation.target }}</strong>
              </div>
            </div>
            <n-empty v-else description="当前生成结果暂无显式关联关系" />
          </section>
        </template>

        <template v-else-if="aiPreviewDetailType === 'pages'">
          <n-data-table
            size="small"
            :bordered="false"
            :single-line="false"
            :columns="appPreviewColumns"
            :data="aiPageDetailRows"
            :pagination="{ pageSize: 8 }"
          />
        </template>

        <template v-else-if="aiPreviewDetailType === 'fields'">
          <div class="lc-model-preview-head">
            <n-select
              v-model:value="activePreviewModelCode"
              size="small"
              :options="previewModelOptions"
              placeholder="选择模型"
            />
            <n-tag size="small" :bordered="false" type="info">
              {{ activePreviewModel?.modelSchema?.tableName || '-' }}
            </n-tag>
          </div>
          <n-data-table
            size="small"
            :bordered="false"
            :single-line="false"
            :columns="modelPreviewColumns"
            :data="activePreviewFields"
            :pagination="{ pageSize: 10 }"
          />
        </template>

        <template v-else-if="aiPreviewDetailType === 'apis'">
          <n-data-table
            size="small"
            :bordered="false"
            :single-line="false"
            :columns="apiPreviewColumns"
            :data="aiApiDetailRows"
            :pagination="{ pageSize: 10 }"
          />
        </template>
      </div>
    </n-modal>

    <input
      ref="appImportInputRef"
      class="hidden-file-input"
      type="file"
      accept="application/json,.json"
      @change="handleAppImportFile"
    >
  </div>
</template>

<script setup>
import { EllipsisVertical, SaveOutline, SparklesOutline } from '@vicons/ionicons5'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { modelListByProvider, providerPage } from '@/api/ai'
import {
  lowcodeAiStreamGenerateApp,
  lowcodeAppDetail,
  lowcodeAppPage,
  lowcodeCreateDomain,
  lowcodeCreateModel,
  lowcodeDeleteApp,
  lowcodeDeleteDomain,
  lowcodeDomainTree,
  lowcodeDownloadAppCode,
  lowcodeModelList,
  lowcodeSaveDraft,
} from '@/api/lowcode-crud'
import LowcodeCodePreviewModal from '@/components/lowcode-builder/code/LowcodeCodePreviewModal.vue'
import DomainEditorDrawer from '@/components/lowcode-builder/domain/DomainEditorDrawer.vue'
import DomainTreePanel from '@/components/lowcode-builder/domain/DomainTreePanel.vue'
import MoveDomainModal from '@/components/lowcode-builder/domain/MoveDomainModal.vue'
import { cloneSchema, normalizeObjectCode } from '@/components/lowcode-builder/model/model-schema'
import { useDict } from '@/composables/useDict'

defineOptions({ name: 'AiLowcodeApps' })

const { dict } = useDict('lowcode_app_publish_status')

const router = useRouter()
const domainLoading = ref(false)
const loading = ref(false)
const domains = ref([])
const selectedDomainId = ref(null)
const selectedDomain = ref(null)
const apps = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(9)
const keyword = ref('')
const domainKeyword = ref('')
const publishStatus = ref(null)
const domainEditorVisible = ref(false)
const editingDomain = ref(null)
const moveVisible = ref(false)
const movingApp = ref(null)
const appImportInputRef = ref(null)
const codePreviewVisible = ref(false)
const codePreviewApp = ref(null)
const downloadingCodeId = ref(null)
const aiCreateVisible = ref(false)
const aiCreateLoading = ref(false)
const aiConfirmSaving = ref(false)
const aiCreateDescription = ref('')
const aiCreateResult = ref(null)
const aiStreamController = ref(null)
const aiSteps = ref(createInitialAiSteps())
const aiTargetDomainId = ref(null)
const aiRawContent = ref('')
const aiReasoningContent = ref('')
const aiIsReasoningPhase = ref(false)
const aiReasoningStartTime = ref(null)
const aiReasoningEndTime = ref(null)
const aiProviderId = ref(null)
const aiModelId = ref(null)
const aiProviderOptions = ref([])
const aiModelOptions = ref([])
const aiProviderLoading = ref(false)
const aiModelLoading = ref(false)
const aiIncludeDomainModels = ref(true)
const domainModelContext = ref([])
const domainModelLoading = ref(false)
const activePreviewModelCode = ref(null)
const aiPreviewDetailVisible = ref(false)
const aiPreviewDetailType = ref('er')
const aiAppendInstruction = ref('')

const statusOptions = computed(() => dict.value.lowcode_app_publish_status || [])
const flatDomains = computed(() => flattenDomains(domains.value))
const domainSelectOptions = computed(() => flatDomains.value.map(domain => ({
  label: `${'  '.repeat(domain.level || 0)}${domain.domainName} (${domain.domainCode})`,
  value: domain.id,
})))
const aiTargetDomain = computed(() => flatDomains.value.find(domain => domain.id === aiTargetDomainId.value) || null)
const aiReasoningTime = computed(() => {
  if (!aiReasoningStartTime.value)
    return null
  const endTime = aiReasoningEndTime.value || (aiIsReasoningPhase.value ? Date.now() : null)
  return endTime ? Math.max(1, Math.round((endTime - aiReasoningStartTime.value) / 1000)) : null
})
const aiGeneratedDomains = computed(() => aiCreateResult.value?.domains || [])
const aiGeneratedModels = computed(() => {
  const result = aiCreateResult.value
  if (!result)
    return []
  return result.models?.length ? result.models : result.modelDraft ? [result.modelDraft] : []
})
const aiGeneratedApps = computed(() => {
  const result = aiCreateResult.value
  if (!result)
    return []
  return result.apps?.length ? result.apps : result.appDraft ? [result.appDraft] : []
})
const aiDecisions = computed(() => aiCreateResult.value?.decisions || [])
const aiNotes = computed(() => aiCreateResult.value?.generationNotes || [])
const previewModelOptions = computed(() => aiGeneratedModels.value.map(model => ({
  label: `${model.modelName || model.modelCode} (${model.modelCode})`,
  value: model.modelCode,
})))
const activePreviewModel = computed(() => {
  if (!aiGeneratedModels.value.length)
    return null
  return aiGeneratedModels.value.find(model => model.modelCode === activePreviewModelCode.value) || aiGeneratedModels.value[0]
})
const activePreviewFields = computed(() => (activePreviewModel.value?.modelSchema?.fields || []).map((field, index) => ({
  ...field,
  index: index + 1,
})))
const aiPrimaryDomainLabel = computed(() => {
  const domain = aiGeneratedDomains.value[0] || aiTargetDomain.value || selectedDomain.value
  return domain?.domainName || '待识别'
})
const aiPrimaryModelName = computed(() => aiGeneratedModels.value[0]?.modelName || '核心实体')
const aiRelationCount = computed(() => {
  const schemaRelationCount = aiGeneratedModels.value.reduce((count, model) => {
    const relations = model.modelSchema?.relations
    return count + (Array.isArray(relations) ? relations.length : 0)
  }, 0)
  if (schemaRelationCount)
    return schemaRelationCount
  return aiGeneratedApps.value.reduce((count, app) => {
    const refs = app.pageSchema?.modelRefs
    return count + (Array.isArray(refs) ? refs.filter(ref => !ref.primary).length : 0)
  }, 0)
})
const aiModelRelationRows = computed(() => {
  return aiGeneratedModels.value.flatMap((model, modelIndex) => {
    const relations = model.modelSchema?.relations
    if (!Array.isArray(relations))
      return []
    return relations.map((relation, relationIndex) => ({
      key: `${model.modelCode || modelIndex}-${relationIndex}`,
      source: model.modelName || model.modelCode || '-',
      type: relation.relationType || relation.type || relation.cardinality || '关联',
      target: relation.targetModelName || relation.targetModelCode || relation.targetModel || relation.target || '-',
    }))
  })
})
const aiApiDetailRows = computed(() => {
  const targets = aiGeneratedApps.value.length ? aiGeneratedApps.value : aiGeneratedModels.value
  return targets.flatMap(target => buildApiRowsForTarget(target))
})
const aiApiCount = computed(() => aiApiDetailRows.value.length)
const aiCompletionPercent = computed(() => aiCreateResult.value ? '100%' : aiCreateLoading.value ? '...' : '0%')
const aiValidationStatus = computed(() => aiCreateResult.value ? '待确认' : aiCreateLoading.value ? '生成中' : '未校验')
const aiPageStructureRows = computed(() => {
  if (!aiGeneratedApps.value.length)
    return []
  return aiGeneratedApps.value.slice(0, 5).map((app, index) => ({
    key: app.configKey || app.appName || app.menuName || `app-${index}`,
    name: app.appName || app.menuName || app.configKey,
    layout: layoutName(app.pageSchema?.layoutType),
  }))
})
const aiPreviewFields = computed(() => {
  return activePreviewFields.value.slice(0, 4)
})
const aiApiPreviewRows = computed(() => {
  return aiApiDetailRows.value.slice(0, 4)
})
const aiPageDetailRows = computed(() => {
  return aiGeneratedApps.value.map((app, index) => {
    const appModel = resolveAppModel(app)
    const fields = appModel?.modelSchema?.fields || app.modelSchema?.fields || []
    const zones = app.pageSchema?.zones || []
    const modelRefs = app.pageSchema?.modelRefs || []
    return {
      index: index + 1,
      appName: app.appName || app.menuName || app.configKey,
      configKey: app.configKey || '-',
      layout: layoutName(app.pageSchema?.layoutType),
      modelCount: modelRefs.length || (appModel ? 1 : 0),
      fieldCount: fields.length,
      zoneCount: zones.length,
    }
  })
})
const aiPreviewDetailTitle = computed(() => {
  if (aiPreviewDetailType.value === 'er')
    return 'ER 图详情'
  if (aiPreviewDetailType.value === 'pages')
    return '页面结构详情'
  if (aiPreviewDetailType.value === 'fields')
    return '字段清单详情'
  return '接口建议详情'
})
const modelPreviewColumns = [
  { title: '#', key: 'index', width: 48 },
  { title: '字段', key: 'field', minWidth: 120 },
  { title: '列名', key: 'columnName', minWidth: 130 },
  { title: '名称', key: 'label', minWidth: 120 },
  { title: '类型', key: 'dataType', width: 90 },
  { title: '组件', key: 'componentType', width: 120 },
]
const appPreviewColumns = [
  { title: '#', key: 'index', width: 48 },
  { title: '应用页面', key: 'appName', minWidth: 150 },
  { title: '配置标识', key: 'configKey', minWidth: 150 },
  { title: '布局', key: 'layout', width: 110 },
  { title: '模型数', key: 'modelCount', width: 90 },
  { title: '字段数', key: 'fieldCount', width: 90 },
  { title: '区域数', key: 'zoneCount', width: 90 },
]
const apiPreviewColumns = [
  { title: '应用/模型', key: 'ownerName', minWidth: 150 },
  { title: '方法', key: 'method', width: 80 },
  { title: '路径', key: 'path', minWidth: 220 },
  { title: '说明', key: 'description', minWidth: 140 },
]

const pageTitle = computed(() => '低代码应用')
const pageSubtitle = computed(() => {
  if (!selectedDomain.value)
    return '按业务领域组织低代码应用，统一管理对象、规则和发布入口'
  return selectedDomain.value.domainDesc || `领域编码：${selectedDomain.value.domainCode}`
})
const appScopeText = computed(() => selectedDomain.value ? `当前领域：${selectedDomain.value.domainName}` : '当前展示全部业务领域应用')

onMounted(async () => {
  await loadDomains()
  await loadApps()
})

async function loadDomains() {
  domainLoading.value = true
  try {
    const res = await lowcodeDomainTree({
      keyword: domainKeyword.value || undefined,
    })
    domains.value = res.data || []
  }
  finally {
    domainLoading.value = false
  }
}

async function refreshDomains() {
  await loadDomains()
}

async function selectDomain(domain) {
  pageNum.value = 1
  if (!domain) {
    selectedDomainId.value = null
    selectedDomain.value = null
    await loadApps()
    return
  }
  selectedDomainId.value = domain.id
  selectedDomain.value = domain
  await loadApps()
}

async function loadApps() {
  loading.value = true
  try {
    const res = await lowcodeAppPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      publishStatus: publishStatus.value || undefined,
      domainId: selectedDomainId.value || undefined,
    })
    apps.value = res.data?.records || []
    total.value = res.data?.total || 0
  }
  finally {
    loading.value = false
  }
}

function openAiPreviewDetail(type) {
  if (type === 'fields' && !activePreviewModelCode.value)
    activePreviewModelCode.value = aiGeneratedModels.value[0]?.modelCode || null
  aiPreviewDetailType.value = type
  aiPreviewDetailVisible.value = true
}

function resolveAppModel(app) {
  const primaryRef = app.pageSchema?.modelRefs?.find(ref => ref.primary) || app.pageSchema?.modelRefs?.[0]
  const modelCode = app.modelCode || app.objectCode || primaryRef?.modelCode
  return aiGeneratedModels.value.find((model) => {
    const schemaObjectCode = model.modelSchema?.object?.code
    return model.modelCode === modelCode || schemaObjectCode === modelCode || model.modelCode === app.objectCode
  }) || null
}

function buildApiRowsForTarget(target) {
  const objectCode = target.objectCode || target.modelCode || normalizeObjectCode(target.configKey || target.appName || target.modelName)
  if (!objectCode)
    return []
  const ownerName = target.appName || target.modelName || target.menuName || target.configKey || target.modelCode || objectCode
  const basePath = `/api/${objectCode}`
  return [
    { ownerName, method: 'GET', path: `${basePath}/page`, description: '分页查询' },
    { ownerName, method: 'GET', path: `${basePath}/{id}`, description: '详情查询' },
    { ownerName, method: 'POST', path: basePath, description: '新增' },
    { ownerName, method: 'PUT', path: basePath, description: '修改' },
    { ownerName, method: 'DELETE', path: `${basePath}/{id}`, description: '删除' },
  ]
}

async function refreshAll() {
  await Promise.all([
    loadDomains(),
    loadApps(),
  ])
}

function handleSearch() {
  pageNum.value = 1
  loadApps()
}

function handlePageSizeChange() {
  pageNum.value = 1
  loadApps()
}

function createApp(domain) {
  if (!domain) {
    window.$message?.warning('请先选择业务领域，再新建应用')
    return
  }
  if (domain?.status === 'DISABLED') {
    window.$message?.warning('停用领域不能新建应用')
    return
  }
  router.push({
    path: '/ai/lowcode-builder',
    query: domain
      ? {
          domainId: domain.id,
          domainCode: domain.domainCode,
          domainName: domain.domainName,
        }
      : {},
  })
}

function openBuilder(id) {
  router.push(`/ai/lowcode-builder/${id}`)
}

function openRuntime(configKey) {
  const route = router.resolve(`/ai/crud-page/${configKey}`)
  window.open(route.href, '_blank')
}

function openCodePreview(app) {
  codePreviewApp.value = app
  codePreviewVisible.value = true
}

async function downloadAppCode(app) {
  if (!app?.id) {
    window.$message?.warning('请先保存应用草稿')
    return
  }
  downloadingCodeId.value = app.id
  try {
    const blob = await lowcodeDownloadAppCode(app.id, { sourceType: 'DRAFT' })
    downloadBlob(blob, `${app.configKey || 'lowcode-app'}-code.zip`)
    window.$message?.success('代码包下载成功')
  }
  catch (e) {
    window.$message?.error(e?.message || '下载代码失败')
  }
  finally {
    downloadingCodeId.value = null
  }
}

function getAppActionOptions(app) {
  return [
    { label: '代码预览', key: 'codePreview' },
    { label: downloadingCodeId.value === app.id ? '下载中...' : '下载代码', key: 'downloadCode', disabled: downloadingCodeId.value === app.id },
    { label: '迁移领域', key: 'moveDomain' },
    { label: '导出配置', key: 'export' },
    { type: 'divider', key: 'divider' },
    { label: '删除应用', key: 'delete' },
  ]
}

async function handleAppActionSelect(key, app) {
  if (key === 'codePreview') {
    openCodePreview(app)
    return
  }
  if (key === 'downloadCode') {
    await downloadAppCode(app)
    return
  }
  if (key === 'moveDomain') {
    openMoveDomain(app)
    return
  }
  if (key === 'export') {
    await exportApp(app)
    return
  }
  if (key === 'delete')
    deleteApp(app)
}

function openAiCreateApp() {
  aiCreateDescription.value = ''
  aiCreateResult.value = null
  aiAppendInstruction.value = ''
  activePreviewModelCode.value = null
  aiTargetDomainId.value = selectedDomainId.value || null
  resetAiStreamState()
  aiSteps.value = createInitialAiSteps()
  aiCreateVisible.value = true
  loadAiProviderOptions()
  loadDomainModelContext(aiTargetDomainId.value)
}

function handleAiCreateVisibleUpdate(show) {
  if (!show)
    abortAiGeneration()
}

function closeAiCreateModal() {
  abortAiGeneration()
  aiCreateVisible.value = false
}

function abortAiGeneration() {
  aiStreamController.value?.abort()
  aiStreamController.value = null
  aiCreateLoading.value = false
  aiIsReasoningPhase.value = false
}

function generateAiAppDraft(refine = false) {
  if (!aiCreateDescription.value.trim()) {
    window.$message?.warning('请输入需求描述')
    return
  }
  if (refine && !aiAppendInstruction.value.trim()) {
    window.$message?.warning('请输入追加优化要求')
    return
  }
  const previousDraftContext = refine ? buildDraftContext() : undefined
  abortAiGeneration()
  aiCreateLoading.value = true
  aiCreateResult.value = null
  activePreviewModelCode.value = null
  resetAiStreamState()
  aiSteps.value = createInitialAiSteps()
  const description = refine
    ? `${aiCreateDescription.value.trim()}\n\n追加优化要求：${aiAppendInstruction.value.trim()}`
    : aiCreateDescription.value.trim()

  aiStreamController.value = lowcodeAiStreamGenerateApp(
    {
      description,
      domainId: aiTargetDomainId.value || undefined,
      providerId: aiProviderId.value || undefined,
      modelId: aiModelId.value || undefined,
      autoCreateModel: true,
      includeDdl: false,
      existingModels: aiIncludeDomainModels.value ? buildExistingModelContext() : [],
      draftContext: previousDraftContext,
    },
    handleAiStreamEvent,
    () => {
      aiCreateLoading.value = false
      aiStreamController.value = null
      markPendingAiStepsCompleted()
    },
    (message) => {
      aiCreateLoading.value = false
      aiStreamController.value = null
      markAiStepError(message)
      window.$message?.error(message || 'AI 生成业务系统失败')
    },
  )
}

function handleAiStreamEvent(payload) {
  if (payload.event === 'progress') {
    updateAiStep(payload.data)
    return
  }
  if (payload.event === 'chunk') {
    handleAiStreamChunk(payload.data?.content || '')
    return
  }
  if (payload.event === 'result') {
    aiCreateResult.value = payload.data
    activePreviewModelCode.value = aiGeneratedModels.value[0]?.modelCode || null
    aiAppendInstruction.value = ''
    applyAiResultSteps(payload.data)
  }
}

async function loadAiProviderOptions() {
  aiProviderLoading.value = true
  try {
    const res = await providerPage({ pageNum: 1, pageSize: 100, status: '0' })
    const records = (res.data?.records || []).filter(item => item.status === undefined || item.status === '0')
    aiProviderOptions.value = records.map(item => ({
      label: item.providerName,
      value: item.id,
    }))
    if (!aiProviderId.value) {
      const defaultProvider = records.find(item => item.isDefault === '1') || records[0]
      aiProviderId.value = defaultProvider?.id || null
    }
    await loadAiModelOptions(aiProviderId.value)
  }
  catch (e) {
    aiProviderOptions.value = []
    console.warn('[lowcode-apps] 加载 AI 供应商失败:', e.message)
  }
  finally {
    aiProviderLoading.value = false
  }
}

async function handleAiProviderChange(providerId) {
  aiModelId.value = null
  await loadAiModelOptions(providerId)
}

async function loadAiModelOptions(providerId) {
  if (!providerId) {
    aiModelOptions.value = []
    aiModelId.value = null
    return
  }
  aiModelLoading.value = true
  try {
    const res = await modelListByProvider(providerId)
    const models = (res.data || []).filter(item => item.status === undefined || item.status === '0')
    aiModelOptions.value = models.map(item => ({
      label: item.modelName ? `${item.modelName} (${item.modelId})` : item.modelId,
      value: item.id,
      modelCode: item.modelId,
      maxTokens: item.maxTokens,
      isDefault: item.isDefault,
    }))
    const hasSelected = aiModelOptions.value.some(item => item.value === aiModelId.value)
    if (!hasSelected) {
      const defaultModel = aiModelOptions.value.find(item => item.isDefault === '1') || aiModelOptions.value[0]
      aiModelId.value = defaultModel?.value || null
    }
  }
  catch (e) {
    aiModelOptions.value = []
    aiModelId.value = null
    console.warn('[lowcode-apps] 加载 AI 模型失败:', e.message)
  }
  finally {
    aiModelLoading.value = false
  }
}

async function handleAiTargetDomainChange(domainId) {
  await loadDomainModelContext(domainId)
}

async function loadDomainModelContext(domainId) {
  domainModelContext.value = []
  if (!domainId)
    return
  domainModelLoading.value = true
  try {
    const res = await lowcodeModelList({ domainId, status: 'ENABLED' })
    domainModelContext.value = res.data || []
  }
  catch (e) {
    domainModelContext.value = []
    console.warn('[lowcode-apps] 加载领域模型失败:', e.message)
  }
  finally {
    domainModelLoading.value = false
  }
}

function buildExistingModelContext() {
  return domainModelContext.value.map(model => ({
    id: model.id,
    domainId: model.domainId,
    modelCode: model.modelCode,
    modelName: model.modelName,
    modelDesc: model.modelDesc,
    status: model.status,
    tenantEnabled: model.tenantEnabled,
    masterData: model.masterData,
    modelSchema: cloneSchema(model.modelSchema || {}),
  }))
}

function buildDraftContext() {
  return JSON.stringify({
    domains: aiGeneratedDomains.value,
    models: aiGeneratedModels.value,
    apps: aiGeneratedApps.value,
    decisions: aiDecisions.value,
  })
}

function resetAiStreamState() {
  aiRawContent.value = ''
  aiReasoningContent.value = ''
  aiIsReasoningPhase.value = false
  aiReasoningStartTime.value = null
  aiReasoningEndTime.value = null
}

function handleAiStreamChunk(chunkContent) {
  if (!chunkContent)
    return
  if (chunkContent.includes('==================== 思考过程 ====================')) {
    aiIsReasoningPhase.value = true
    aiReasoningStartTime.value = Date.now()
    aiReasoningEndTime.value = null
    aiReasoningContent.value = ''
    const afterDelimiter = chunkContent.split('==================== 思考过程 ====================')[1] || ''
    aiReasoningContent.value += afterDelimiter.replace(/^\n/, '')
    updateAiStep({
      stepKey: 'analyzing',
      title: '理解业务需求',
      status: 'running',
      message: '模型正在分析业务边界和对象关系',
    })
    return
  }
  if (chunkContent.includes('==================== 完整回复 ====================')) {
    aiIsReasoningPhase.value = false
    aiReasoningEndTime.value = Date.now()
    const afterDelimiter = chunkContent.split('==================== 完整回复 ====================')[1] || ''
    aiRawContent.value += afterDelimiter.replace(/^\n/, '')
    return
  }
  if (aiIsReasoningPhase.value) {
    aiReasoningContent.value += chunkContent
  }
  else {
    aiRawContent.value += chunkContent
  }
}

function createInitialAiSteps() {
  return [
    { orderNo: 1, stepKey: 'analyzing', title: '理解业务需求', status: 'pending', message: '等待开始' },
    { orderNo: 2, stepKey: 'domain-planning', title: '划分业务领域', status: 'pending', message: '等待开始' },
    { orderNo: 3, stepKey: 'model-generating', title: '生成数据模型', status: 'pending', message: '等待开始' },
    { orderNo: 4, stepKey: 'page-generating', title: '生成应用页面', status: 'pending', message: '等待开始' },
    { orderNo: 5, stepKey: 'validating', title: '校验低代码协议', status: 'pending', message: '等待开始' },
  ]
}

function stepStatusText(status) {
  if (status === 'running')
    return '进行中'
  if (status === 'completed')
    return '已完成'
  if (status === 'error')
    return '异常'
  return '待开始'
}

function updateAiStep(data = {}) {
  const stepKey = data.stepKey || data.stage
  const index = aiSteps.value.findIndex(step => step.stepKey === stepKey)
  if (index < 0)
    return
  aiSteps.value = aiSteps.value.map((step, currentIndex) => {
    if (currentIndex < index && step.status !== 'error') {
      return { ...step, status: 'completed' }
    }
    if (currentIndex === index) {
      return {
        ...step,
        title: data.title || step.title,
        status: data.status || 'running',
        message: data.message || step.message,
        summary: data.summary || step.summary,
      }
    }
    return step
  })
}

function applyAiResultSteps(result) {
  if (!result?.steps?.length)
    return
  aiSteps.value = result.steps
    .slice()
    .sort((a, b) => (a.orderNo || 0) - (b.orderNo || 0))
    .map(step => ({
      ...step,
      status: step.status || 'completed',
      message: step.message || step.summary || '已完成',
    }))
}

function markPendingAiStepsCompleted() {
  aiSteps.value = aiSteps.value.map(step => ({
    ...step,
    status: step.status === 'pending' || step.status === 'running' ? 'completed' : step.status,
    message: step.status === 'pending' ? '已完成' : step.message,
  }))
}

function markAiStepError(message) {
  const runningIndex = aiSteps.value.findIndex(step => step.status === 'running')
  const index = runningIndex >= 0 ? runningIndex : aiSteps.value.findIndex(step => step.status === 'pending')
  if (index < 0)
    return
  aiSteps.value = aiSteps.value.map((step, currentIndex) => {
    if (currentIndex === index)
      return { ...step, status: 'error', message: message || '生成失败' }
    return step
  })
}

async function confirmAiAppDraft() {
  if (!aiCreateResult.value)
    return
  const domains = aiGeneratedDomains.value
  const models = aiGeneratedModels.value
  const appsToSave = aiGeneratedApps.value
  if (!models.length || !appsToSave.length) {
    window.$message?.warning('AI 结果缺少模型或应用草稿')
    return
  }
  aiConfirmSaving.value = true
  try {
    const domainContext = await ensureAiDomains(domains)
    await saveAiModels(models, domainContext, appsToSave)
    const savedAppIds = await saveAiApps(appsToSave, models, domainContext)
    closeAiCreateModal()
    window.$message?.success(`已保存 ${models.length} 个模型和 ${savedAppIds.length} 个应用草稿`)
    await refreshAll()
    if (savedAppIds[0])
      openBuilder(savedAppIds[0])
  }
  catch (e) {
    window.$message?.error(e?.message || '保存 AI 应用草稿失败')
  }
  finally {
    aiConfirmSaving.value = false
  }
}

async function ensureAiDomains(domains) {
  const domainMap = new Map()
  const domainList = domains?.length ? domains : aiTargetDomain.value ? [aiTargetDomain.value] : []
  if (!domainList.length)
    throw new Error('AI 结果缺少业务领域草稿')

  for (const domain of domainList) {
    const existingId = domain.existingDomainId || domain.id
    if (existingId) {
      domainMap.set(domain.domainCode, {
        id: existingId,
        domainCode: domain.domainCode,
        domainName: domain.domainName,
        menuParentId: domain.menuParentId ?? aiTargetDomain.value?.menuParentId ?? null,
      })
      continue
    }
    const payload = {
      id: null,
      parentId: domain.parentId || 0,
      domainCode: domain.domainCode,
      domainName: domain.domainName,
      domainDesc: domain.domainDesc || aiCreateDescription.value,
      icon: domain.icon || 'apps',
      sort: domain.sort || 0,
      status: domain.status || 'ENABLED',
      menuParentId: domain.menuParentId || null,
      tablePrefix: domain.tablePrefix || `biz_${domain.domainCode}_`,
      configKeyPrefix: domain.configKeyPrefix || `${domain.domainCode}_`,
      defaultAppType: domain.defaultAppType || 'SINGLE',
      defaultLayoutType: domain.defaultLayoutType || 'simple-crud',
      defaultTableMode: domain.defaultTableMode || 'CREATE',
      domainSchema: cloneSchema(domain.domainSchema || {}),
    }
    const res = await lowcodeCreateDomain(payload)
    domainMap.set(domain.domainCode, {
      id: res.data,
      domainCode: domain.domainCode,
      domainName: domain.domainName,
      menuParentId: domain.menuParentId || null,
    })
  }
  return {
    domainMap,
    fallbackDomain: domainMap.values().next().value,
  }
}

async function saveAiModels(models, domainContext, appsToSave) {
  for (const modelDraft of models) {
    const relatedApp = appsToSave.find(app => app.objectCode && app.objectCode === modelDraft.modelCode) || appsToSave[0]
    const modelSchema = cloneSchema(modelDraft.modelSchema || aiCreateResult.value.modelSchema || {})
    const domainInfo = resolveAiDomainInfo(modelSchema?.domain?.code || relatedApp?.domainCode, domainContext)
    applyDomainToModelSchema(modelSchema, domainInfo)
    const modelCode = modelDraft.modelCode || modelSchema.object?.code || modelSchema.tableName
    const modelName = modelDraft.modelName || modelSchema.object?.name || modelSchema.businessName || modelCode
    if (!modelSchema?.fields?.length)
      throw new Error(`模型 ${modelName} 缺少字段配置`)
    await lowcodeCreateModel({
      ...modelDraft,
      id: null,
      domainId: domainInfo.id,
      modelCode,
      modelName,
      modelDesc: modelDraft.modelDesc || aiCreateDescription.value,
      status: modelDraft.status || 'ENABLED',
      tenantEnabled: modelDraft.tenantEnabled !== false,
      masterData: Boolean(modelDraft.masterData),
      modelSchema,
      syncDdl: false,
      confirmSyncDdl: false,
    })
  }
}

async function saveAiApps(appsToSave, models, domainContext) {
  const savedIds = []
  for (const appDraft of appsToSave) {
    const relatedModel = models.find(model => model.modelCode && model.modelCode === appDraft.objectCode) || models[0]
    const modelSchema = cloneSchema(appDraft.modelSchema || relatedModel?.modelSchema || aiCreateResult.value.modelSchema || {})
    const pageSchema = cloneSchema(appDraft.pageSchema || aiCreateResult.value.pageSchema || {})
    const domainInfo = resolveAiDomainInfo(appDraft.domainCode || modelSchema?.domain?.code, domainContext)
    applyDomainToModelSchema(modelSchema, domainInfo)
    const objectCode = appDraft.objectCode || relatedModel?.modelCode || modelSchema.object?.code || ''
    const objectName = appDraft.objectName || relatedModel?.modelName || modelSchema.object?.name || modelSchema.businessName || ''
    const res = await lowcodeSaveDraft({
      ...appDraft,
      id: null,
      domainId: domainInfo.id,
      domainCode: domainInfo.domainCode,
      domainName: domainInfo.domainName,
      objectCode,
      objectName,
      configKey: appDraft.configKey || buildImportedConfigKey(objectCode || 'ai_app'),
      appName: appDraft.appName || `${objectName || 'AI应用'}管理`,
      menuName: appDraft.menuName || appDraft.appName || `${objectName || 'AI应用'}管理`,
      menuParentId: appDraft.menuParentId ?? domainInfo.menuParentId ?? null,
      menuSort: appDraft.menuSort || 0,
      modelSchema,
      pageSchema,
    })
    if (res.data)
      savedIds.push(res.data)
  }
  return savedIds
}

function resolveAiDomainInfo(domainCode, domainContext) {
  if (domainCode && domainContext.domainMap.has(domainCode))
    return domainContext.domainMap.get(domainCode)
  return domainContext.fallbackDomain
}

function applyDomainToModelSchema(modelSchema, domainInfo) {
  if (!modelSchema.domain)
    modelSchema.domain = {}
  modelSchema.domain.id = domainInfo.id
  modelSchema.domain.code = domainInfo.domainCode
  modelSchema.domain.name = domainInfo.domainName
}

function layoutName(layoutType) {
  if (layoutType === 'tree-crud')
    return '左树右表'
  if (layoutType === 'master-detail-crud')
    return '主子表'
  return '标准单表'
}

async function exportApp(app) {
  try {
    const res = await lowcodeAppDetail(app.id)
    const detail = res.data || {}
    downloadJson({
      type: 'LOWCODE_APP_CONFIG',
      version: 1,
      exportedAt: new Date().toISOString(),
      app: {
        id: null,
        configKey: detail.configKey,
        appName: detail.appName,
        domainCode: detail.domainCode,
        domainName: detail.domainName,
        objectCode: detail.objectCode,
        objectName: detail.objectName,
        menuName: detail.menuName,
        menuParentId: detail.menuParentId,
        menuSort: detail.menuSort,
        modelSchema: cloneSchema(detail.modelSchema || {}),
        pageSchema: cloneSchema(detail.pageSchema || {}),
      },
    }, `${detail.configKey || 'app'}-app-config.json`)
  }
  catch (e) {
    window.$message?.error(e?.message || '导出应用配置失败')
  }
}

function triggerAppImport() {
  appImportInputRef.value?.click()
}

async function handleAppImportFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file)
    return
  try {
    const payload = JSON.parse(await file.text())
    const app = payload.app || payload
    if (!app?.modelSchema || !app?.pageSchema) {
      window.$message?.warning('应用配置文件格式不正确')
      return
    }
    const domain = selectedDomain.value
    if (!domain && !app.domainId && !app.domainCode) {
      window.$message?.warning('请先选择业务领域，再导入应用配置')
      return
    }
    const configKey = buildImportedConfigKey(app.configKey || app.modelSchema?.object?.code || 'app')
    const res = await lowcodeSaveDraft({
      id: null,
      domainId: domain?.id || app.domainId || null,
      domainCode: domain?.domainCode || app.domainCode || '',
      domainName: domain?.domainName || app.domainName || '',
      objectCode: app.objectCode || app.modelSchema?.object?.code || '',
      objectName: app.objectName || app.modelSchema?.object?.name || app.modelSchema?.businessName || '',
      configKey,
      appName: app.appName || app.modelSchema?.businessName || configKey,
      menuName: app.menuName || app.appName || app.modelSchema?.businessName || configKey,
      menuParentId: domain?.menuParentId || app.menuParentId || null,
      menuSort: app.menuSort || 0,
      modelSchema: cloneSchema(app.modelSchema),
      pageSchema: cloneSchema(app.pageSchema),
    })
    window.$message?.success('应用配置已导入')
    await refreshAll()
    if (res.data)
      openBuilder(res.data)
  }
  catch (e) {
    window.$message?.error(e?.message || '导入应用配置失败')
  }
}

function buildImportedConfigKey(sourceKey) {
  const suffix = new Date().toISOString().slice(11, 19).replace(/\D/g, '')
  return normalizeObjectCode(`${sourceKey || 'app'}_import_${suffix}`).slice(0, 64)
}

function flattenDomains(nodes, level = 0) {
  const result = []
  for (const node of nodes || []) {
    result.push({ ...node, level })
    if (node.children?.length)
      result.push(...flattenDomains(node.children, level + 1))
  }
  return result
}

function openDomainEditor(domain) {
  editingDomain.value = domain ? { ...domain } : null
  domainEditorVisible.value = true
}

async function handleDomainSaved() {
  await refreshDomains()
}

function deleteDomain(domain) {
  if (!domain?.id)
    return
  window.$dialog.warning({
    title: '确认删除领域',
    content: `确定删除业务领域“${domain.domainName}”吗？存在下级领域或低代码应用时后端会阻止删除。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await lowcodeDeleteDomain(domain.id)
        window.$message?.success('业务领域已删除')
        if (selectedDomainId.value === domain.id) {
          selectedDomainId.value = null
          selectedDomain.value = null
          pageNum.value = 1
        }
        await refreshAll()
      }
      catch (error) {
        window.$message?.error(error?.message || '业务领域删除失败')
      }
    },
  })
}

function openMoveDomain(app) {
  movingApp.value = app
  moveVisible.value = true
}

function deleteApp(app) {
  window.$dialog.warning({
    title: '确认删除应用',
    content: `确定删除低代码应用“${app.appName || app.configKey}”吗？已发布菜单会同步删除，若菜单已被角色授权将无法删除。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await lowcodeDeleteApp(app.id)
        window.$message?.success('应用已删除')
        if (apps.value.length === 1 && pageNum.value > 1)
          pageNum.value -= 1
        await refreshAll()
      }
      catch (error) {
        window.$message?.error(error?.message || '应用删除失败')
      }
    },
  })
}

async function handleMoved() {
  moveVisible.value = false
  movingApp.value = null
  await refreshAll()
}

function statusLabel(status) {
  const item = dict.value.lowcode_app_publish_status?.find(d => d.value === status)
  return item?.label || '草稿'
}

function appInitial(app) {
  const text = app.appName || app.tableComment || app.configKey || '低'
  return text.slice(0, 1).toUpperCase()
}

function formatTime(value) {
  if (!value)
    return ''
  const date = new Date(value)
  return `${date.getMonth() + 1}-${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function downloadJson(payload, filename) {
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' })
  downloadBlob(blob, filename)
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.lowcode-domain-page {
  display: grid;
  min-height: 100%;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  overflow-x: hidden;
  padding: 16px;
  background: #f3f6fa;
}

.domain-main {
  display: grid;
  min-width: 0;
  gap: 14px;
}

.main-toolbar {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.main-toolbar > div {
  min-width: 0;
}

.main-toolbar :deep(.n-space) {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.main-toolbar h1 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.25;
}

.main-toolbar p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 13px;
}

.apps-board {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.apps-board-head {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.apps-board-head > div {
  min-width: 0;
}

.apps-board-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
}

.apps-board-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.filter-strip {
  display: grid;
  min-width: 0;
  width: min(520px, 50%);
  grid-template-columns: minmax(0, 1fr) minmax(130px, 150px);
  gap: 10px;
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.app-card {
  display: grid;
  gap: 12px;
  min-width: 0;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff, #fbfdff);
  padding: 14px;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.app-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.app-topline,
.app-version-row,
.app-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-topline {
  justify-content: space-between;
  color: #94a3b8;
  font-size: 12px;
}

.app-title-row {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.app-title-row > div {
  min-width: 0;
}

.app-mark {
  display: flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #0f172a;
  color: #86efac;
  font-size: 16px;
  font-weight: 800;
}

.app-title-row h3 {
  overflow: hidden;
  margin: 0;
  color: #0f172a;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-title-row p {
  overflow: hidden;
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-code-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.app-code-grid div {
  display: grid;
  min-width: 0;
  overflow: hidden;
  gap: 3px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #f8fafc;
  padding: 8px;
}

.app-code-grid span {
  color: #64748b;
  font-size: 11px;
}

.app-code-grid code {
  overflow: hidden;
  color: #0f172a;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-version-row {
  color: #64748b;
  font-size: 12px;
}

.app-actions {
  flex-wrap: nowrap;
  justify-content: flex-end;
  min-width: 0;
}

.app-actions :deep(.n-button:not(.more-action-button)) {
  flex: 1 1 0;
  min-width: 0;
}

.more-action-button {
  width: 32px;
  min-width: 32px;
  padding: 0;
}

.apps-pagination {
  display: flex;
  justify-content: center;
  padding: 18px 0 2px;
}

.hidden-file-input {
  display: none;
}

.lc-agent-modal-card {
  overflow: hidden;
  border: 1px solid rgba(76, 137, 255, 0.48);
  background: #0b1c3a;
  box-shadow:
    0 24px 80px rgba(3, 18, 48, 0.42),
    0 0 0 1px rgba(43, 136, 255, 0.16),
    0 0 40px rgba(74, 69, 255, 0.22);
}

.lc-agent-modal-card :deep(.n-card-header) {
  display: none;
}

.lc-agent-modal-card :deep(.n-card__content) {
  padding: 0;
}

.lc-agent-modal-card :deep(.n-card__footer) {
  border-top: 1px solid rgba(74, 144, 255, 0.22);
  background: #0b1c3a;
  padding: 12px 18px;
}

.lc-agent-modal {
  display: grid;
  max-height: calc(100vh - 58px);
  min-width: 0;
  overflow: auto;
  gap: 14px;
  background:
    radial-gradient(circle at 18% 0%, rgba(52, 144, 255, 0.28), transparent 34%),
    radial-gradient(circle at 92% 16%, rgba(139, 92, 246, 0.2), transparent 30%),
    linear-gradient(180deg, #102a55 0%, #0c2248 52%, #0a1b38 100%);
  color: #dbeafe;
  padding: 14px;
}

.lc-agent-topbar,
.lc-agent-input-head,
.lc-output-head,
.lc-result-head,
.lc-stream-head,
.lc-mini-row,
.lc-decision-row,
.lc-agent-summary-head {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.lc-agent-topbar {
  min-height: 48px;
  border: 1px solid rgba(62, 126, 255, 0.18);
  border-radius: 8px;
  background: rgba(13, 38, 83, 0.78);
  padding: 8px 12px;
}

.lc-agent-brand {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.lc-agent-logo {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid rgba(103, 232, 249, 0.8);
  border-radius: 8px;
  background: linear-gradient(145deg, #0ea5e9, #3155ff 52%, #7c3aed);
  color: #f8fbff;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 0 24px rgba(34, 211, 238, 0.34);
}

.lc-agent-brand > div,
.lc-agent-input-head > div,
.lc-output-head > div,
.lc-result-head > div,
.lc-mini-row > div,
.lc-decision-row > div,
.lc-agent-summary-head {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.lc-agent-brand strong,
.lc-agent-input-head strong,
.lc-output-head strong,
.lc-result-head strong,
.lc-mini-row strong,
.lc-decision-row strong,
.lc-agent-summary-head strong {
  overflow: hidden;
  color: #f8fbff;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lc-agent-brand span,
.lc-agent-input-head span,
.lc-output-head span,
.lc-result-head span,
.lc-mini-row span,
.lc-decision-row span,
.lc-agent-summary-head span {
  overflow: hidden;
  color: #88a5d8;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lc-agent-top-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
}

.lc-agent-ghost-action {
  --n-text-color: #bfdbfe !important;
  --n-text-color-hover: #ffffff !important;
  --n-color-hover: rgba(59, 130, 246, 0.18) !important;
}

.lc-save-indicator {
  --n-color: rgba(18, 48, 98, 0.82) !important;
  --n-color-hover: rgba(30, 64, 132, 0.9) !important;
  --n-border: 1px solid rgba(96, 165, 250, 0.46) !important;
  --n-border-hover: 1px solid rgba(125, 211, 252, 0.72) !important;
  --n-text-color: #dbeafe !important;
  --n-text-color-hover: #ffffff !important;
}

.lc-agent-input,
.lc-step-rail,
.lc-agent-output,
.lc-result-section,
.lc-agent-summary-panel {
  border: 1px solid rgba(60, 133, 255, 0.32);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(21, 53, 108, 0.84), rgba(12, 34, 74, 0.88)), rgba(14, 37, 78, 0.88);
  box-shadow:
    inset 0 0 0 1px rgba(125, 211, 252, 0.06),
    0 0 22px rgba(37, 99, 235, 0.16);
}

.lc-agent-input {
  display: grid;
  min-width: 0;
  gap: 12px;
  border-color: rgba(34, 211, 238, 0.55);
  box-shadow:
    inset 0 0 0 1px rgba(147, 197, 253, 0.12),
    0 0 0 1px rgba(99, 102, 241, 0.22),
    0 0 34px rgba(14, 165, 233, 0.22),
    0 0 42px rgba(124, 58, 237, 0.18);
  padding: 14px;
}

.lc-agent-input :deep(.n-form-item) {
  margin-bottom: 0;
}

.lc-agent-modal :deep(.n-form-item-label__text) {
  color: #c7d8ff;
  font-size: 12px;
}

.lc-agent-modal :deep(.n-base-selection),
.lc-agent-modal :deep(.n-input) {
  --n-color: rgba(5, 16, 42, 0.82) !important;
  --n-color-focus: rgba(8, 24, 58, 0.92) !important;
  --n-color-hover: rgba(8, 24, 58, 0.92) !important;
  --n-border: 1px solid rgba(96, 165, 250, 0.36) !important;
  --n-border-hover: 1px solid rgba(56, 189, 248, 0.72) !important;
  --n-border-focus: 1px solid rgba(59, 130, 246, 0.9) !important;
  --n-box-shadow-focus: 0 0 0 2px rgba(37, 99, 235, 0.24) !important;
  --n-placeholder-color: #6681b6 !important;
  --n-text-color: #e6f0ff !important;
}

.lc-agent-modal :deep(.n-base-selection-label),
.lc-agent-modal :deep(.n-input__input-el),
.lc-agent-modal :deep(.n-input__textarea-el) {
  color: #e6f0ff;
}

.lc-agent-modal :deep(.n-input-word-count) {
  color: #7893c2;
}

.lc-agent-modal :deep(.n-checkbox__label) {
  color: #bed4ff;
  font-size: 12px;
}

.lc-agent-chip-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.lc-helper-button {
  --n-height: 26px !important;
  --n-color: rgba(14, 39, 92, 0.68) !important;
  --n-color-hover: rgba(30, 64, 132, 0.9) !important;
  --n-border: 1px solid rgba(96, 165, 250, 0.38) !important;
  --n-border-hover: 1px solid rgba(125, 211, 252, 0.72) !important;
  --n-text-color: #b8d6ff !important;
  --n-text-color-hover: #ffffff !important;
  font-size: 12px;
}

.lc-agent-chip-row span {
  border: 1px solid rgba(96, 165, 250, 0.38);
  border-radius: 6px;
  background: rgba(14, 39, 92, 0.68);
  color: #b8d6ff;
  font-size: 12px;
  line-height: 24px;
  padding: 0 9px;
}

.lc-agent-input-grid {
  display: grid;
  min-width: 0;
  align-items: stretch;
  grid-template-columns: 320px minmax(0, 1fr) 160px;
  gap: 14px;
}

.lc-agent-form {
  display: grid;
  min-width: 0;
  align-content: start;
  gap: 10px;
  border-right: 1px solid rgba(96, 165, 250, 0.26);
  padding-right: 14px;
}

.lc-domain-model-context {
  display: flex;
  min-width: 0;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.lc-demand-box {
  display: grid;
  min-width: 0;
  gap: 8px;
}

.lc-demand-head {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.lc-demand-head strong {
  color: #e6f0ff;
  font-size: 13px;
}

.lc-demand-head span,
.lc-agent-command-text {
  overflow: hidden;
  color: #83a3d8;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lc-agent-command-text {
  border: 1px solid rgba(96, 165, 250, 0.26);
  border-radius: 6px;
  background: rgba(3, 12, 30, 0.58);
  padding: 7px 9px;
}

.lc-agent-start-card {
  min-height: 142px;
  border-color: rgba(125, 211, 252, 0.9) !important;
  background:
    radial-gradient(circle at 50% 15%, rgba(255, 255, 255, 0.28), transparent 24%),
    linear-gradient(150deg, #075cff 0%, #2563eb 42%, #7c3aed 100%) !important;
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.2),
    0 0 24px rgba(34, 211, 238, 0.38),
    0 0 30px rgba(124, 58, 237, 0.32) !important;
}

.lc-agent-start-card :deep(.n-button__content) {
  width: 100%;
}

.lc-agent-start-content {
  display: grid;
  width: 100%;
  place-items: center;
  gap: 8px;
}

.lc-agent-start-icon {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border: 2px solid rgba(255, 255, 255, 0.78);
  border-radius: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: 900;
  box-shadow: 0 0 18px rgba(255, 255, 255, 0.34);
}

.lc-agent-start-content strong {
  color: #fff;
  font-size: 21px;
}

.lc-agent-start-content span:last-child {
  color: #dbeafe;
  font-size: 12px;
}

.lc-agent-workspace {
  display: grid;
  min-width: 0;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 14px;
}

.lc-step-rail {
  position: relative;
  display: grid;
  min-width: 0;
  align-content: start;
  gap: 12px;
  padding: 12px;
}

.lc-step-rail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(96, 165, 250, 0.18);
  padding-bottom: 10px;
}

.lc-step-rail-head strong {
  color: #e6f0ff;
  font-size: 14px;
}

.lc-step-rail-head span {
  color: #88a5d8;
  font-size: 12px;
}

.lc-step-item {
  position: relative;
  display: grid;
  min-width: 0;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: 9px;
  color: #88a5d8;
}

.lc-step-item::before {
  position: absolute;
  top: 22px;
  bottom: -15px;
  left: 11px;
  width: 1px;
  background: linear-gradient(180deg, rgba(96, 165, 250, 0.55), rgba(96, 165, 250, 0.08));
  content: '';
}

.lc-step-item:last-child::before {
  display: none;
}

.lc-step-dot {
  position: relative;
  z-index: 1;
  display: grid;
  width: 22px;
  height: 22px;
  margin-top: 1px;
  place-items: center;
  border: 2px solid rgba(148, 163, 184, 0.8);
  border-radius: 50%;
  background: #07142f;
  color: #c7d2fe;
  font-size: 11px;
  font-weight: 800;
  box-shadow: 0 0 0 3px rgba(15, 23, 42, 0.9);
}

.lc-step-dot::after {
  display: none;
}

.lc-step-item > div {
  display: grid;
  min-width: 0;
  gap: 4px;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 5px 8px;
}

.lc-step-item > div > strong,
.lc-step-item > div > span {
  display: block;
  min-width: 0;
  overflow-wrap: anywhere;
}

.lc-step-item > div > strong {
  color: #dbeafe;
  font-size: 13px;
  line-height: 1.35;
}

.lc-step-item > div > span {
  color: #829ac9;
  font-size: 12px;
  line-height: 1.45;
}

.lc-step-item small {
  width: fit-content;
  border: 1px solid rgba(96, 165, 250, 0.2);
  border-radius: 999px;
  color: #9cb8e9;
  font-size: 11px;
  line-height: 18px;
  padding: 0 8px;
}

.lc-step-item.is-running > div {
  border-color: rgba(59, 130, 246, 0.54);
  background: rgba(22, 70, 170, 0.42);
  box-shadow: 0 0 16px rgba(59, 130, 246, 0.24);
}

.lc-step-item.is-running .lc-step-dot {
  border-color: #38bdf8;
  background: #082a66;
}

.lc-step-item.is-running .lc-step-dot::after {
  display: none;
}

.lc-step-item.is-completed .lc-step-dot {
  border-color: #34d399;
  background: #062d2a;
}

.lc-step-item.is-completed .lc-step-dot::after {
  display: none;
}

.lc-step-item.is-error .lc-step-dot {
  border-color: #fb7185;
  background: #3f0b18;
}

.lc-step-item.is-error .lc-step-dot::after {
  display: none;
}

.lc-agent-output {
  position: relative;
  display: grid;
  min-width: 0;
  min-height: 520px;
  align-content: start;
  gap: 12px;
  overflow: hidden;
  padding: 12px;
}

.lc-agent-output::before {
  position: absolute;
  right: -120px;
  bottom: -160px;
  width: 420px;
  height: 300px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.22), transparent 64%);
  content: '';
  pointer-events: none;
}

.lc-output-head {
  position: relative;
  z-index: 1;
  border-bottom: 1px solid rgba(96, 165, 250, 0.18);
  padding-bottom: 10px;
}

.lc-agent-welcome {
  position: relative;
  display: grid;
  min-height: 430px;
  place-items: center;
  overflow: hidden;
  gap: 10px;
  padding: 20px;
  text-align: center;
}

.lc-agent-welcome h3 {
  margin: 0;
  color: #eff6ff;
  font-size: 18px;
}

.lc-agent-welcome p {
  max-width: 520px;
  margin: 0;
  color: #8da7d6;
  font-size: 13px;
  line-height: 1.7;
}

.lc-capability-grid {
  display: grid;
  width: min(820px, 100%);
  min-width: 0;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.lc-capability-grid div {
  display: grid;
  min-width: 0;
  gap: 4px;
  border: 1px solid rgba(96, 165, 250, 0.26);
  border-radius: 8px;
  background: rgba(18, 48, 98, 0.68);
  padding: 14px 10px;
  text-align: left;
}

.lc-capability-grid span {
  color: #67e8f9;
  font-size: 12px;
}

.lc-capability-grid strong {
  overflow: hidden;
  color: #dbeafe;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lc-capability-icon {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border: 1px solid rgba(103, 232, 249, 0.5);
  border-radius: 8px;
  background: rgba(14, 165, 233, 0.16);
  color: #67e8f9 !important;
  font-weight: 800;
}

.lc-capability-grid p {
  margin: 0;
  color: #8da7d6;
  font-size: 12px;
  line-height: 1.5;
}

.lc-stream-panel {
  display: grid;
  min-width: 0;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 10px;
}

.lc-stream-section {
  display: grid;
  min-width: 0;
  gap: 8px;
  border: 1px solid rgba(96, 165, 250, 0.26);
  border-radius: 8px;
  background: rgba(16, 42, 85, 0.78);
  padding: 10px;
}

.lc-stream-content {
  min-height: 150px;
  max-height: 240px;
  overflow: auto;
  margin: 0;
  border: 1px solid rgba(96, 165, 250, 0.28);
  border-radius: 6px;
  background: rgba(10, 27, 58, 0.9);
  color: #bae6fd;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  line-height: 1.6;
  padding: 10px;
  white-space: pre-wrap;
  word-break: break-word;
}

.lc-stream-empty {
  display: grid;
  min-height: 150px;
  place-content: center;
  border: 1px dashed rgba(96, 165, 250, 0.34);
  border-radius: 6px;
  color: #8199c8;
  font-size: 12px;
  line-height: 1.6;
  padding: 12px;
  text-align: center;
}

.lc-preview-board {
  display: grid;
  min-width: 0;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.lc-preview-card {
  display: grid;
  min-width: 0;
  min-height: 158px;
  align-content: space-between;
  gap: 10px;
  border: 1px solid rgba(96, 165, 250, 0.28);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(22, 55, 108, 0.88), rgba(12, 34, 74, 0.92));
  padding: 10px;
}

.lc-preview-card-head {
  display: flex;
  min-width: 0;
  justify-content: space-between;
  gap: 10px;
}

.lc-preview-card-head > div {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.lc-preview-card-head strong {
  overflow: hidden;
  color: #eaf4ff;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lc-preview-card-head span {
  overflow: hidden;
  color: #88a5d8;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lc-preview-button {
  justify-self: end;
  --n-height: 24px !important;
  --n-color: rgba(30, 64, 132, 0.7) !important;
  --n-color-hover: rgba(37, 99, 235, 0.56) !important;
  --n-border: 1px solid rgba(96, 165, 250, 0.36) !important;
  --n-border-hover: 1px solid rgba(125, 211, 252, 0.7) !important;
  --n-text-color: #bfdbfe !important;
  --n-text-color-hover: #ffffff !important;
}

.lc-er-mini {
  position: relative;
  display: grid;
  min-height: 72px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.lc-er-mini span {
  display: grid;
  min-width: 0;
  place-items: center;
  border: 1px solid rgba(96, 165, 250, 0.34);
  border-radius: 6px;
  background: rgba(7, 21, 51, 0.72);
  color: #cfe5ff;
  font-size: 12px;
  text-align: center;
  padding: 6px;
}

.lc-er-mini span.is-primary {
  border-color: rgba(103, 232, 249, 0.58);
  background: rgba(14, 165, 233, 0.16);
}

.lc-er-mini i {
  position: absolute;
  top: 50%;
  right: 28%;
  left: 28%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(125, 211, 252, 0.82), transparent);
}

.lc-page-wireframe {
  display: grid;
  gap: 7px;
  border: 1px solid rgba(96, 165, 250, 0.2);
  border-radius: 6px;
  background: rgba(7, 21, 51, 0.52);
  padding: 10px;
}

.lc-page-wireframe span {
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(96, 165, 250, 0.84), rgba(96, 165, 250, 0.12));
}

.lc-page-wireframe span:nth-child(2) {
  width: 78%;
}

.lc-page-wireframe span:nth-child(3) {
  width: 62%;
}

.lc-page-wireframe span:nth-child(4) {
  width: 88%;
}

.lc-field-mini-table,
.lc-api-mini-list {
  display: grid;
  min-width: 0;
  gap: 6px;
}

.lc-field-mini-table div,
.lc-api-mini-list div {
  display: grid;
  min-width: 0;
  align-items: center;
  gap: 6px;
  border-radius: 6px;
  background: rgba(7, 21, 51, 0.52);
  color: #dbeafe;
  font-size: 12px;
  padding: 6px 8px;
}

.lc-field-mini-table div {
  grid-template-columns: minmax(0, 1fr) 64px 76px;
}

.lc-field-mini-table span,
.lc-field-mini-table small,
.lc-field-mini-table em,
.lc-api-mini-list span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lc-field-mini-table small,
.lc-field-mini-table em {
  color: #8fb2e8;
  font-style: normal;
}

.lc-api-mini-list div {
  grid-template-columns: 42px minmax(0, 1fr);
}

.lc-api-mini-list b {
  border-radius: 4px;
  color: #06142e;
  font-size: 10px;
  line-height: 18px;
  text-align: center;
}

.lc-api-mini-list b.is-get {
  background: #67e8f9;
}

.lc-api-mini-list b.is-post {
  background: #86efac;
}

.lc-api-mini-list b.is-put {
  background: #fbbf24;
}

.lc-api-mini-list b.is-delete {
  background: #fb7185;
}

.lc-summary-grid,
.lc-result-columns,
.lc-agent-summary-metrics {
  display: grid;
  min-width: 0;
  gap: 10px;
}

.lc-summary-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.lc-summary-grid div {
  display: grid;
  min-width: 0;
  gap: 3px;
  border: 1px solid rgba(96, 165, 250, 0.24);
  border-radius: 8px;
  background: rgba(18, 48, 98, 0.76);
  padding: 10px;
}

.lc-agent-summary-metrics div {
  display: grid;
  min-width: 0;
  align-items: center;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 2px 10px;
  border: 1px solid rgba(96, 165, 250, 0.24);
  border-radius: 8px;
  background: rgba(18, 48, 98, 0.76);
  padding: 10px 12px;
}

.lc-summary-grid span,
.lc-agent-summary-metrics small {
  color: #82a2d8;
  font-size: 12px;
  line-height: 1.25;
}

.lc-summary-grid strong,
.lc-agent-summary-metrics strong {
  overflow: hidden;
  color: #f8fbff;
  font-size: 14px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lc-result-columns {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.lc-result-section {
  display: grid;
  min-width: 0;
  gap: 8px;
  padding: 10px;
}

.lc-result-section h3 {
  margin: 0;
  color: #dbeafe;
  font-size: 13px;
}

.lc-model-preview-head,
.lc-refine-box {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  gap: 10px;
}

.lc-model-preview-head :deep(.n-select),
.lc-refine-box :deep(.n-input) {
  flex: 1 1 auto;
  min-width: 0;
}

.lc-refine-box :deep(.n-button) {
  flex: 0 0 auto;
}

.lc-result-section :deep(.n-data-table) {
  --n-border-color: rgba(125, 211, 252, 0.34) !important;
  --n-td-color: rgba(22, 55, 108, 0.92) !important;
  --n-td-color-hover: rgba(31, 75, 142, 0.96) !important;
  --n-th-color: rgba(36, 80, 150, 0.98) !important;
  --n-td-text-color: #eef6ff !important;
  --n-th-text-color: #dbeafe !important;
  --n-merged-th-color: rgba(36, 80, 150, 0.98) !important;
  border-radius: 8px;
  background: rgba(18, 48, 98, 0.9);
  overflow: hidden;
}

.lc-mini-list,
.lc-decision-list {
  display: grid;
  min-width: 0;
  gap: 8px;
}

.lc-mini-row {
  border: 1px solid rgba(96, 165, 250, 0.22);
  border-radius: 8px;
  background: rgba(18, 48, 98, 0.76);
  padding: 8px;
}

.lc-decision-row {
  align-items: flex-start;
  border-bottom: 1px solid rgba(96, 165, 250, 0.18);
  padding-bottom: 8px;
}

.lc-decision-row:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.lc-decision-row p {
  width: min(360px, 45%);
  margin: 0;
  color: #8ea8d7;
  font-size: 12px;
  line-height: 1.5;
}

.note-list {
  margin: 0;
  color: #8ea8d7;
  font-size: 12px;
  line-height: 1.7;
  padding-left: 18px;
}

.lc-agent-summary-panel {
  display: grid;
  min-width: 0;
  gap: 12px;
  padding: 12px;
}

.lc-agent-summary-metrics {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.lc-summary-icon,
.lc-summary-progress {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid rgba(96, 165, 250, 0.38);
  border-radius: 50%;
  background: rgba(14, 39, 92, 0.72);
  color: #67e8f9;
  font-size: 13px;
  font-weight: 800;
  grid-row: 1 / span 2;
}

.lc-summary-progress {
  width: 48px;
  height: 30px;
  border-color: rgba(167, 139, 250, 0.72);
  border-radius: 999px;
  background: rgba(77, 44, 151, 0.78);
  color: #c4b5fd;
  font-size: 12px;
}

.lc-agent-footer-actions {
  width: 100%;
}

.lc-agent-secondary-button {
  --n-text-color: #bfdbfe !important;
  --n-text-color-hover: #ffffff !important;
  --n-border: 1px solid rgba(96, 165, 250, 0.36) !important;
  --n-border-hover: 1px solid rgba(125, 211, 252, 0.76) !important;
  --n-color-hover: rgba(59, 130, 246, 0.14) !important;
}

.lc-agent-confirm-button {
  min-width: 132px;
  box-shadow: 0 0 20px rgba(37, 99, 235, 0.26);
}

/* AI 建模向导：浅色企业后台工作台 */
.lc-agent-modal-card {
  border: 1px solid #d7e5ff;
  background: #f6f9ff;
  box-shadow:
    0 24px 72px rgba(15, 23, 42, 0.22),
    0 0 0 1px rgba(59, 130, 246, 0.08);
}

.lc-agent-modal-card :deep(.n-card__footer) {
  border-top: 1px solid #dbe7fb;
  background: #ffffff;
  padding: 12px 18px;
}

.lc-agent-modal {
  gap: 14px;
  background: linear-gradient(180deg, #f7fbff 0%, #eef5ff 56%, #f8fafc 100%);
  color: #0f172a;
  padding: 16px;
}

.lc-agent-topbar {
  min-height: 58px;
  border: 1px solid #dbe7fb;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 28px rgba(30, 64, 175, 0.08);
  padding: 10px 14px;
}

.lc-agent-logo {
  border-color: #60a5fa;
  background: linear-gradient(145deg, #0ea5e9, #2563eb);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.24);
}

.lc-agent-brand strong,
.lc-agent-input-head strong,
.lc-output-head strong,
.lc-result-head strong,
.lc-mini-row strong,
.lc-decision-row strong,
.lc-agent-summary-head strong {
  color: #0f172a;
}

.lc-agent-brand span,
.lc-agent-input-head span,
.lc-output-head span,
.lc-result-head span,
.lc-mini-row span,
.lc-decision-row span,
.lc-agent-summary-head span {
  color: #64748b;
}

.lc-save-indicator {
  --n-color: #eff6ff !important;
  --n-color-hover: #dbeafe !important;
  --n-border: 1px solid #bfdbfe !important;
  --n-border-hover: 1px solid #60a5fa !important;
  --n-text-color: #1d4ed8 !important;
  --n-text-color-hover: #1e40af !important;
}

.lc-agent-ghost-action {
  --n-text-color: #475569 !important;
  --n-text-color-hover: #1d4ed8 !important;
  --n-color-hover: #eff6ff !important;
}

.lc-agent-input,
.lc-step-rail,
.lc-agent-output,
.lc-result-section,
.lc-agent-summary-panel {
  border: 1px solid #dbe7fb;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(30, 64, 175, 0.07);
}

.lc-agent-input {
  border-color: #bfdbfe;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.98));
  box-shadow:
    0 0 0 1px rgba(96, 165, 250, 0.18),
    0 14px 34px rgba(37, 99, 235, 0.1);
  padding: 14px;
}

.lc-agent-modal :deep(.n-form-item-label__text) {
  color: #334155;
  font-weight: 600;
}

.lc-agent-modal :deep(.n-base-selection),
.lc-agent-modal :deep(.n-input) {
  --n-color: #ffffff !important;
  --n-color-focus: #ffffff !important;
  --n-color-hover: #ffffff !important;
  --n-border: 1px solid #d8e2f0 !important;
  --n-border-hover: 1px solid #60a5fa !important;
  --n-border-focus: 1px solid #2563eb !important;
  --n-box-shadow-focus: 0 0 0 2px rgba(37, 99, 235, 0.14) !important;
  --n-placeholder-color: #94a3b8 !important;
  --n-text-color: #0f172a !important;
}

.lc-agent-modal :deep(.n-base-selection-label),
.lc-agent-modal :deep(.n-input__input-el),
.lc-agent-modal :deep(.n-input__textarea-el) {
  color: #0f172a;
}

.lc-agent-modal :deep(.n-input-word-count),
.lc-agent-modal :deep(.n-checkbox__label) {
  color: #64748b;
}

.lc-helper-button {
  --n-height: 28px !important;
  --n-color: #f8fafc !important;
  --n-color-hover: #eff6ff !important;
  --n-border: 1px solid #dbe7fb !important;
  --n-border-hover: 1px solid #93c5fd !important;
  --n-text-color: #2563eb !important;
  --n-text-color-hover: #1d4ed8 !important;
}

.lc-agent-chip-row span {
  border-color: #dbe7fb;
  background: #f8fafc;
  color: #2563eb;
}

.lc-agent-input-grid {
  grid-template-columns: 300px minmax(0, 1fr) 168px;
}

.lc-agent-form {
  border-right-color: #dbe7fb;
}

.lc-demand-head strong {
  color: #0f172a;
}

.lc-demand-head span,
.lc-agent-command-text {
  color: #64748b;
}

.lc-agent-command-text {
  border-color: #dbe7fb;
  background: #f8fafc;
}

.lc-agent-start-card {
  min-height: 152px;
  border: 0 !important;
  background: linear-gradient(145deg, #0284c7 0%, #2563eb 56%, #4338ca 100%) !important;
  box-shadow:
    0 14px 32px rgba(37, 99, 235, 0.28),
    inset 0 0 0 1px rgba(255, 255, 255, 0.2) !important;
}

.lc-agent-start-icon {
  border-color: rgba(255, 255, 255, 0.72);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.16);
  box-shadow: none;
}

.lc-agent-workspace {
  grid-template-columns: 260px minmax(0, 1fr);
}

.lc-step-rail {
  background: #fbfdff;
  padding: 14px;
}

.lc-step-rail-head {
  border-bottom-color: #e2e8f0;
}

.lc-step-rail-head strong,
.lc-step-item > div > strong,
.lc-result-section h3 {
  color: #0f172a;
}

.lc-step-rail-head span {
  color: #64748b;
}

.lc-step-item {
  grid-template-columns: 30px minmax(0, 1fr);
  align-items: start;
  gap: 10px;
}

.lc-step-item::before {
  top: 30px;
  bottom: -16px;
  left: 14px;
  background: linear-gradient(180deg, #bfdbfe, rgba(191, 219, 254, 0.18));
}

.lc-step-dot {
  display: flex;
  width: 28px;
  height: 28px;
  box-sizing: border-box;
  align-items: center;
  justify-content: center;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  box-shadow: 0 0 0 4px #fbfdff;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.lc-step-item > div {
  gap: 4px;
  border-color: #e2e8f0;
  background: #ffffff;
  padding: 8px 10px;
}

.lc-step-item > div > span {
  display: -webkit-box;
  overflow: hidden;
  color: #64748b;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.lc-step-item small {
  border-color: #e2e8f0;
  background: #f8fafc;
  color: #64748b;
}

.lc-step-item.is-running > div {
  border-color: #93c5fd;
  background: #eff6ff;
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.1);
}

.lc-step-item.is-running .lc-step-dot {
  border-color: #2563eb;
  background: #2563eb;
  color: #ffffff;
}

.lc-step-item.is-completed .lc-step-dot {
  border-color: #10b981;
  background: #10b981;
  color: #ffffff;
}

.lc-step-item.is-error .lc-step-dot {
  border-color: #f43f5e;
  background: #f43f5e;
  color: #ffffff;
}

.lc-agent-output {
  min-height: 540px;
  background: #ffffff;
  overflow: visible;
}

.lc-agent-output::before {
  display: none;
}

.lc-output-head {
  border-bottom-color: #e2e8f0;
}

.lc-agent-welcome {
  min-height: 242px;
  place-items: stretch;
  align-content: start;
  gap: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  padding: 18px;
  text-align: left;
}

.lc-welcome-copy {
  display: grid;
  gap: 4px;
}

.lc-agent-welcome h3 {
  color: #0f172a;
  font-size: 18px;
}

.lc-agent-welcome p {
  max-width: none;
  color: #64748b;
}

.lc-capability-grid div,
.lc-preview-card,
.lc-summary-grid div,
.lc-agent-summary-metrics div,
.lc-mini-row {
  border-color: #e2e8f0;
  background: #ffffff;
}

.lc-capability-grid div {
  grid-template-columns: 34px minmax(0, 1fr);
  align-items: start;
  gap: 2px 10px;
  background: #f8fafc;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
  padding: 12px;
}

.lc-capability-icon {
  grid-row: 1 / span 2;
}

.lc-capability-grid strong,
.lc-preview-card-head strong {
  color: #0f172a;
}

.lc-capability-grid span,
.lc-preview-card-head span,
.lc-capability-grid p,
.note-list,
.lc-decision-row p {
  color: #64748b;
}

.lc-capability-icon {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #2563eb !important;
}

.lc-capability-grid p {
  grid-column: 2;
}

.lc-stream-section {
  border-color: #dbe7fb;
  background: #f8fafc;
}

.lc-stream-content {
  border-color: #dbe7fb;
  background: #ffffff;
  color: #0f172a;
}

.lc-stream-empty {
  border-color: #cbd5e1;
  color: #64748b;
}

.lc-preview-board {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 2px;
}

.lc-preview-card {
  min-height: 166px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
}

.lc-preview-button {
  --n-height: 26px !important;
  --n-color: #eff6ff !important;
  --n-color-hover: #dbeafe !important;
  --n-border: 1px solid #bfdbfe !important;
  --n-border-hover: 1px solid #60a5fa !important;
  --n-text-color: #2563eb !important;
  --n-text-color-hover: #1d4ed8 !important;
}

.lc-er-mini span,
.lc-page-wireframe,
.lc-field-mini-table div,
.lc-api-mini-list div {
  border-color: #dbe7fb;
  background: #f8fafc;
  color: #334155;
}

.lc-er-mini span.is-primary {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
}

.lc-er-mini span:only-of-type {
  grid-column: 1 / -1;
}

.lc-er-mini i {
  background: linear-gradient(90deg, transparent, #60a5fa, transparent);
}

.lc-page-wireframe span {
  background: linear-gradient(90deg, #60a5fa, rgba(96, 165, 250, 0.14));
}

.lc-page-structure-list {
  display: grid;
  min-width: 0;
  gap: 6px;
}

.lc-page-structure-list div {
  display: grid;
  min-width: 0;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  border: 1px solid #dbe7fb;
  border-radius: 6px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  padding: 7px 8px;
}

.lc-page-structure-list span,
.lc-page-structure-list small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lc-page-structure-list small {
  color: #64748b;
}

.lc-preview-empty {
  display: grid;
  min-height: 72px;
  place-items: center;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 12px;
}

.lc-ai-preview-detail-modal :deep(.n-card__content) {
  padding-top: 8px;
}

.lc-preview-detail {
  display: grid;
  max-height: min(680px, calc(100vh - 180px));
  min-width: 0;
  gap: 12px;
  overflow: auto;
}

.lc-detail-model-grid {
  display: grid;
  min-width: 0;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.lc-detail-model-card {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid #dbe7fb;
  border-radius: 8px;
  background: #f8fafc;
  padding: 10px;
}

.lc-detail-model-card > div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.lc-detail-model-card strong,
.lc-detail-model-card span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lc-detail-model-card strong {
  color: #0f172a;
  font-size: 13px;
}

.lc-detail-model-card span {
  color: #64748b;
  font-size: 12px;
}

.lc-detail-section {
  display: grid;
  min-width: 0;
  gap: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  padding: 12px;
}

.lc-detail-section h3 {
  margin: 0;
  color: #0f172a;
  font-size: 14px;
}

.lc-detail-relation-list {
  display: grid;
  min-width: 0;
  gap: 8px;
}

.lc-detail-relation-list div {
  display: grid;
  min-width: 0;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  border: 1px solid #dbe7fb;
  border-radius: 8px;
  background: #f8fafc;
  padding: 8px 10px;
}

.lc-detail-relation-list strong,
.lc-detail-relation-list span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lc-detail-relation-list strong {
  color: #0f172a;
  font-size: 13px;
}

.lc-detail-relation-list span {
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  line-height: 22px;
  padding: 0 10px;
}

.lc-preview-detail :deep(.n-data-table) {
  --n-border-color: #dbe7fb !important;
  --n-td-color: #ffffff !important;
  --n-td-color-hover: #f8fafc !important;
  --n-th-color: #f1f5f9 !important;
  --n-td-text-color: #0f172a !important;
  --n-th-text-color: #334155 !important;
  --n-merged-th-color: #f1f5f9 !important;
  border-radius: 8px;
  overflow: hidden;
}

.lc-field-mini-table small,
.lc-field-mini-table em {
  color: #64748b;
}

.lc-api-mini-list b {
  color: #ffffff;
}

.lc-api-mini-list b.is-get {
  background: #2563eb;
}

.lc-api-mini-list b.is-post {
  background: #059669;
}

.lc-api-mini-list b.is-put {
  background: #d97706;
}

.lc-api-mini-list b.is-delete {
  background: #e11d48;
}

.lc-summary-grid div {
  background: #f8fafc;
}

.lc-summary-grid span,
.lc-agent-summary-metrics small {
  color: #64748b;
}

.lc-summary-grid strong,
.lc-agent-summary-metrics strong {
  color: #0f172a;
}

.lc-result-section {
  background: #fbfdff;
}

.lc-result-section :deep(.n-data-table) {
  --n-border-color: #dbe7fb !important;
  --n-td-color: #ffffff !important;
  --n-td-color-hover: #f8fafc !important;
  --n-th-color: #f1f5f9 !important;
  --n-td-text-color: #0f172a !important;
  --n-th-text-color: #334155 !important;
  --n-merged-th-color: #f1f5f9 !important;
  background: #ffffff;
}

.lc-decision-row {
  border-bottom-color: #e2e8f0;
}

.lc-agent-summary-panel {
  background: #ffffff;
}

.lc-agent-summary-metrics {
  grid-template-columns: repeat(7, minmax(118px, 1fr));
}

.lc-agent-summary-metrics div {
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 2px 10px;
  min-height: 68px;
  background: #f8fafc;
  padding: 10px;
}

.lc-summary-icon,
.lc-summary-progress {
  width: 38px;
  height: 38px;
  border-color: #bfdbfe;
  border-radius: 10px;
  background: #eff6ff;
  color: #2563eb;
}

.lc-summary-progress {
  width: 44px;
  height: 38px;
  border-color: #c4b5fd;
  background: #f5f3ff;
  color: #4f46e5;
}

.lc-agent-secondary-button {
  --n-text-color: #475569 !important;
  --n-text-color-hover: #1d4ed8 !important;
  --n-border: 1px solid #d8e2f0 !important;
  --n-border-hover: 1px solid #60a5fa !important;
  --n-color-hover: #eff6ff !important;
}

.lc-agent-confirm-button {
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.18);
}

@media (max-width: 1180px) {
  .lc-agent-input-grid {
    grid-template-columns: 260px minmax(0, 1fr);
  }

  .lc-agent-start-card {
    grid-column: 1 / -1;
    min-height: 78px;
  }

  .lc-preview-board {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .lc-agent-summary-metrics {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1380px) {
  .apps-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .lowcode-domain-page {
    grid-template-columns: 1fr;
  }

  .filter-strip {
    width: 100%;
  }
}

@media (max-width: 720px) {
  .lowcode-domain-page {
    padding: 10px;
  }

  .lc-agent-modal {
    padding: 12px;
  }

  .main-toolbar,
  .apps-board-head {
    flex-direction: column;
  }

  .main-toolbar :deep(.n-space) {
    width: 100%;
    justify-content: flex-start;
  }

  .filter-strip,
  .lc-agent-workspace,
  .lc-result-columns,
  .lc-agent-input-grid,
  .lc-stream-panel,
  .lc-preview-board,
  .lc-agent-summary-metrics,
  .apps-grid {
    grid-template-columns: 1fr;
  }

  .lc-agent-form {
    border-right: 0;
    border-bottom: 1px solid #dbe7fb;
    padding-right: 0;
    padding-bottom: 12px;
  }

  .lc-agent-topbar,
  .lc-agent-input-head,
  .lc-agent-top-actions,
  .lc-output-head,
  .lc-result-head,
  .lc-decision-row,
  .lc-model-preview-head,
  .lc-refine-box {
    align-items: stretch;
    flex-direction: column;
  }

  .lc-decision-row p {
    width: 100%;
  }

  .lc-agent-top-actions {
    width: 100%;
  }

  .lc-capability-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .apps-board,
  .main-toolbar {
    padding: 12px;
  }

  .app-code-grid {
    grid-template-columns: 1fr;
  }

  .lc-summary-grid {
    grid-template-columns: 1fr;
  }

  .lc-capability-grid {
    grid-template-columns: 1fr;
  }

  .app-actions {
    justify-content: flex-start;
  }

  .apps-pagination :deep(.n-pagination) {
    justify-content: flex-start;
  }
}
</style>
