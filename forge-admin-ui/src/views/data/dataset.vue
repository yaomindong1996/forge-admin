<template>
  <div class="dataset-studio">
    <section class="studio-hero">
      <div class="hero-main">
        <p class="hero-kicker">
          Data Asset Workspace
        </p>
        <h1 class="hero-title">
          数据集资产管理台
        </h1>
        <p class="hero-description">
          用分类树组织业务域，用发布流转控制可用性。已发布数据集只读，先下架再修改，保证下游报表与分析消费稳定。
        </p>
      </div>

      <div class="hero-stats">
        <div v-for="card in statCards" :key="card.key" class="hero-stat-card">
          <div class="hero-stat-label">
            {{ card.label }}
          </div>
          <div class="hero-stat-value">
            {{ card.value }}
          </div>
          <div class="hero-stat-note">
            {{ card.note }}
          </div>
        </div>
      </div>
    </section>

    <div class="dataset-workspace">
      <aside class="workspace-sidebar">
        <div class="sidebar-head">
          <div>
            <p class="panel-kicker">
              Taxonomy
            </p>
            <h3>数据集分类</h3>
          </div>
          <n-button type="primary" secondary @click="goToCategoryManage">
            分类管理
          </n-button>
        </div>

        <div class="sidebar-shortcuts">
          <button
            class="scope-chip"
            :class="{ active: activeCategoryScope === 'all' }"
            type="button"
            @click="selectAllCategories"
          >
            全部数据集
          </button>
          <button
            class="scope-chip"
            :class="{ active: activeCategoryScope === 'uncategorized' }"
            type="button"
            @click="selectUncategorized"
          >
            未分类
          </button>
        </div>

        <NInput
          v-model:value="categoryKeyword"
          placeholder="搜索分类名称或编码"
          clearable
          class="category-search"
        >
          <template #prefix>
            <i class="i-material-symbols:search-rounded" />
          </template>
        </NInput>

        <div class="category-tree-shell">
          <n-empty v-if="categoryTreeNodes.length === 0" description="暂无分类，请前往分类管理页配置" size="small" />
          <n-tree
            v-else
            block-line
            :data="categoryTreeNodes"
            :default-expand-all="true"
            :selected-keys="selectedTreeKeys"
            @update:selected-keys="handleCategoryTreeSelect"
          />
        </div>

        <div v-if="selectedCategoryNode" class="category-detail-card">
          <div class="category-detail-top">
            <div>
              <div class="category-detail-name">
                {{ selectedCategoryNode.categoryName }}
              </div>
              <div class="category-detail-code">
                {{ selectedCategoryNode.categoryCode }}
              </div>
            </div>
            <NTag size="small" :type="selectedCategoryNode.status === 1 ? 'success' : 'default'" :bordered="false">
              {{ selectedCategoryNode.status === 1 ? '启用' : '停用' }}
            </NTag>
          </div>
          <p class="category-detail-desc">
            {{ selectedCategoryNode.description || '当前分类暂无补充说明。' }}
          </p>
        </div>
      </aside>

      <section class="workspace-main">
        <div class="main-toolbar">
          <div class="toolbar-title-row">
            <div>
              <p class="panel-kicker">
                Asset Inventory
              </p>
              <h3>数据集列表</h3>
            </div>
            <div class="toolbar-title-meta">
              <span class="toolbar-scope">{{ activeCategoryScopeLabel }}</span>
              <n-button @click="handleResetFilters">
                重置筛选
              </n-button>
              <n-button type="primary" @click="handleAddDataset">
                新增数据集
              </n-button>
            </div>
          </div>

          <div class="toolbar-filters">
            <NInput
              v-model:value="queryForm.datasetName"
              class="toolbar-filter toolbar-filter--keyword"
              clearable
              placeholder="搜索数据集名称"
              @keydown.enter="applySearch"
            >
              <template #prefix>
                <i class="i-material-symbols:search-rounded" />
              </template>
            </NInput>
            <NSelect
              v-model:value="queryForm.connectionId"
              class="toolbar-filter"
              clearable
              filterable
              placeholder="数据连接"
              :options="connectionOptions"
            />
            <NSelect
              v-model:value="queryForm.datasetType"
              class="toolbar-filter"
              clearable
              placeholder="数据集类型"
              :options="datasetTypeOptions"
            />
            <NSelect
              v-model:value="queryForm.publishStatus"
              class="toolbar-filter"
              clearable
              placeholder="发布状态"
              :options="publishStatusOptions"
            />
            <n-button type="primary" @click="applySearch">
              搜索
            </n-button>
          </div>
        </div>

        <AiCrudPage
          ref="crudRef"
          class="dataset-crud"
          :api-config="{
            list: 'get@/data/dataset/page',
            detail: 'get@/data/dataset/:id',
            add: 'post@/data/dataset',
            update: 'put@/data/dataset',
            delete: 'delete@/data/dataset/:id',
          }"
          :show-search="false"
          :hide-toolbar="true"
          :columns="tableColumns"
          :edit-schema="editSchema"
          :before-render-form="beforeRenderForm"
          :before-render-detail="beforeRenderDetail"
          :before-submit="beforeSubmit"
          :hide-modal-footer="true"
          row-key="id"
          :load-detail-on-edit="true"
          :striped="false"
          :bordered="false"
          :scroll-x="1700"
          max-height="calc(100vh - 310px)"
          :edit-grid-cols="12"
          edit-label-placement="top"
          edit-form-class="data-dataset-edit-form"
          modal-type="modal"
          modal-width="min(1480px, calc(100vw - 32px))"
          add-button-text="新增数据集"
          @load-list-success="handleDatasetLoadSuccess"
          @modal-close="handleDatasetModalClose"
        >
          <template #form-datasetEditor="{ formData, updateValue }">
            <div class="dataset-editor-page">
              <header class="dataset-editor-header">
                <div class="dataset-editor-icon">
                  <i class="i-material-symbols:database-outline" />
                </div>
                <div class="dataset-editor-heading">
                  <button class="dataset-editor-breadcrumb" type="button" @click="crudRef?.closeModal()">
                    <span>数据资产管理</span>
                    <i>/</i>
                    <span>数据集定义</span>
                  </button>
                  <div class="dataset-editor-title-row">
                    <h2>{{ formData.datasetName || getDatasetTypeLabel(formData.datasetType) }}</h2>
                    <span class="dataset-status-tag">
                      {{ getPublishStatusLabel(formData.publishStatus) }}
                    </span>
                  </div>
                  <div class="dataset-editor-meta">
                    创建于 {{ formatDatasetDate(formData.createTime) }}
                    <span>|</span>
                    更新于 {{ formatDatasetDate(formData.updateTime) }}
                    <span>|</span>
                    创建人：{{ getDatasetCreatorLabel(formData) }}
                  </div>
                </div>

                <div class="dataset-editor-actions">
                  <n-button
                    type="primary"
                    :disabled="isFormReadOnly"
                    @click="crudRef?.submitForm()"
                  >
                    保存
                  </n-button>
                  <n-button
                    v-if="formData.datasetType === 'SQL'"
                    :loading="sqlPreviewLoading"
                    @click="handlePreviewSql(formData, false)"
                  >
                    预览SQL
                  </n-button>
                  <n-button @click="crudRef?.closeModal()">
                    返回
                  </n-button>
                </div>
              </header>

              <div
                class="dataset-editor-steps"
                :style="{ '--dataset-step-index': currentStep - 1 }"
              >
                <button
                  v-for="(step, index) in stepDefinitions"
                  :key="step.label"
                  class="dataset-editor-step"
                  :class="{
                    'is-active': currentStep === index + 1,
                    'is-completed': currentStep > index + 1,
                  }"
                  type="button"
                  @click="setEditorStep(index + 1, true)"
                >
                  <span v-if="index > 0" class="dataset-editor-step__line" />
                  <span class="dataset-editor-step__content">
                    <span class="dataset-editor-step__index">{{ index + 1 }}</span>
                    <span class="dataset-editor-step__text">
                      <span class="dataset-editor-step__label">{{ step.label }}</span>
                      <span class="dataset-editor-step__caption">{{ step.caption }}</span>
                    </span>
                  </span>
                </button>
              </div>

              <div class="dataset-editor-grid">
                <section class="dataset-edit-panel dataset-edit-panel--basic" data-step-section="1">
                  <div class="panel-section-head">
                    <h3>基础信息</h3>
                  </div>

                  <div class="dataset-form-grid">
                    <label class="dataset-field dataset-field--required">
                      <span>数据集名称</span>
                      <NInput
                        :value="formData.datasetName"
                        :disabled="isFormReadOnly"
                        maxlength="64"
                        show-count
                        placeholder="请输入数据集名称"
                        @update:value="value => formData.datasetName = value"
                      />
                    </label>
                    <label class="dataset-field dataset-field--required">
                      <span>数据集编码</span>
                      <NInput
                        :value="formData.datasetCode"
                        :disabled="isFormReadOnly"
                        placeholder="请输入数据集编码"
                        @update:value="value => formData.datasetCode = value"
                      />
                    </label>
                    <label class="dataset-field">
                      <span>所属目录</span>
                      <n-tree-select
                        :value="formData.categoryId"
                        :options="categoryTreeSelectOptions"
                        :disabled="isFormReadOnly"
                        clearable
                        default-expand-all
                        placeholder="请选择业务分类"
                        @update:value="value => formData.categoryId = value"
                      />
                    </label>
                    <label class="dataset-field dataset-field--required">
                      <span>数据源</span>
                      <div class="data-source-select">
                        <i class="data-source-status-dot" />
                        <NSelect
                          :value="formData.connectionId"
                          :options="connectionOptions"
                          :disabled="isFormReadOnly"
                          filterable
                          clearable
                          placeholder="请选择数据连接"
                          @update:value="value => handleConnectionChange(value, formData)"
                        />
                      </div>
                    </label>
                    <div class="dataset-field dataset-field--required">
                      <span>数据集类型</span>
                      <div class="dataset-type-segment" :class="{ 'is-disabled': isFormReadOnly }">
                        <button
                          v-for="option in datasetTypeOptions"
                          :key="option.value"
                          class="dataset-type-option"
                          :class="{ 'is-active': formData.datasetType === option.value }"
                          type="button"
                          :disabled="isFormReadOnly"
                          @mousedown.stop.prevent
                          @click.stop.prevent="handleDatasetTypeChange(option.value, formData, updateValue)"
                        >
                          {{ option.label }}
                        </button>
                      </div>
                    </div>
                    <label class="dataset-field">
                      <span>可用状态</span>
                      <n-radio-group
                        :value="formData.status"
                        :disabled="isFormReadOnly"
                        @update:value="value => formData.status = value"
                      >
                        <n-radio-button
                          v-for="option in statusOptions"
                          :key="option.value"
                          :value="option.value"
                        >
                          {{ option.label }}
                        </n-radio-button>
                      </n-radio-group>
                    </label>
                    <label v-if="formData.datasetType === 'TABLE'" class="dataset-field dataset-field--wide dataset-field--required">
                      <span>数据表</span>
                      <NSelect
                        :value="formData.tableName"
                        :options="tableOptions"
                        :loading="tableLoading"
                        :disabled="isFormReadOnly"
                        filterable
                        clearable
                        placeholder="请先选择数据连接，再选择数据表"
                        @update:value="value => handleTableNameChange(value, formData)"
                      />
                    </label>
                    <label class="dataset-field dataset-field--wide">
                      <span>描述</span>
                      <NInput
                        :value="formData.description"
                        type="textarea"
                        :disabled="isFormReadOnly"
                        :autosize="{ minRows: 3, maxRows: 5 }"
                        maxlength="200"
                        show-count
                        placeholder="请输入数据集描述"
                        @update:value="value => formData.description = value"
                      />
                    </label>
                    <div class="dataset-field dataset-field--wide">
                      <span>标签</span>
                      <div class="dataset-tag-list">
                        <span
                          v-for="tag in getDatasetTagLabels(formData)"
                          :key="tag"
                          class="dataset-soft-tag"
                        >
                          {{ tag }}
                        </span>
                        <button class="dataset-text-action" type="button" disabled>
                          + 添加标签
                        </button>
                      </div>
                    </div>
                  </div>
                </section>

                <section
                  class="dataset-edit-panel dataset-edit-panel--sql"
                  :class="{ 'is-table-mode': formData.datasetType !== 'SQL' }"
                >
                  <div class="panel-section-head">
                    <h3>{{ formData.datasetType === 'SQL' ? 'SQL编辑器 + 预览结果' : '来源表结构' }}</h3>
                  </div>

                  <div v-if="formData.datasetType === 'SQL'" class="sql-workbench">
                    <div class="sql-editor-shell">
                      <SqlEditor
                        class="dataset-sql-editor"
                        :value="formData.sqlText"
                        :readonly="isFormReadOnly"
                        theme="light"
                        show-fullscreen
                        placeholder="SELECT order_id, order_time, customer_name, amount, status FROM orders WHERE order_time >= :start_time AND order_time < :end_time AND status = :status LIMIT :limit"
                        @update:value="value => formData.sqlText = value"
                      />
                    </div>
                    <div class="sql-preview-shell">
                      <div class="sql-workbench-toolbar">
                        <span>预览结果（前5行）</span>
                        <n-button
                          size="small"
                          :loading="sqlPreviewLoading"
                          :disabled="isFormReadOnly"
                          @click="handlePreviewSql(formData, false)"
                        >
                          刷新预览
                        </n-button>
                      </div>
                      <n-data-table
                        v-if="sqlPreviewColumns.length"
                        size="small"
                        :columns="sqlPreviewColumns"
                        :data="sqlPreviewRows"
                        :loading="sqlPreviewLoading"
                        :pagination="false"
                        :scroll-x="sqlPreviewScrollX"
                        max-height="274px"
                      />
                      <n-empty
                        v-else
                        class="sql-preview-empty"
                        description="暂无预览数据，点击预览SQL获取前5行"
                        size="small"
                      />
                      <div class="sql-preview-note">
                        以上为示例数据，实际结果以运行时为准
                      </div>
                    </div>
                  </div>

                  <div v-else class="table-source-panel">
                    <div class="table-source-summary">
                      <div class="table-source-card">
                        <span>数据连接</span>
                        <strong>{{ formData.connectionId ? getConnectionName(formData.connectionId) : '待选择' }}</strong>
                      </div>
                      <div class="table-source-card">
                        <span>来源数据表</span>
                        <strong>{{ formData.tableName || '待选择' }}</strong>
                      </div>
                      <div class="table-source-card">
                        <span>字段来源</span>
                        <strong>{{ getRowScopeFieldSourceLabel(formData) }}</strong>
                      </div>
                    </div>

                    <div class="table-source-fields">
                      <div class="table-source-fields__head">
                        <span>字段结构</span>
                        <small>{{ getRowScopeFieldOptions(formData).length }} 个字段</small>
                      </div>
                      <div v-if="getRowScopeFieldOptions(formData).length" class="table-source-field-list">
                        <span
                          v-for="field in getRowScopeFieldOptions(formData).slice(0, 18)"
                          :key="field.value"
                          class="table-source-field-chip"
                        >
                          {{ field.label }}
                        </span>
                        <span
                          v-if="getRowScopeFieldOptions(formData).length > 18"
                          class="table-source-field-chip table-source-field-chip--more"
                        >
                          +{{ getRowScopeFieldOptions(formData).length - 18 }}
                        </span>
                      </div>
                      <n-empty
                        v-else
                        size="small"
                        description="暂无字段结构，请先选择数据表并刷新来源字段"
                      />
                    </div>

                    <div class="table-source-actions">
                      <n-button
                        secondary
                        :disabled="isFormReadOnly || !formData.connectionId || !formData.tableName"
                        :loading="rowScopeTableFieldLoading"
                        @click="loadRowScopeTableFields(formData, { force: true })"
                      >
                        刷新来源字段
                      </n-button>
                    </div>
                  </div>
                </section>

                <section class="dataset-edit-panel dataset-edit-panel--params" data-step-section="2">
                  <div class="panel-section-head panel-section-head--inline">
                    <h3>查询参数定义</h3>
                    <n-popover trigger="click" placement="bottom-end" :width="320">
                      <template #trigger>
                        <button class="panel-inline-indicator panel-inline-indicator--button" type="button">
                          参数预览
                        </button>
                      </template>
                      <div class="param-preview-popover">
                        <div v-if="getParamPreviewRows(formData).length" class="param-preview-list">
                          <div
                            v-for="(param, index) in getParamPreviewRows(formData)"
                            :key="`${param.paramName || param.label || param.fieldName}-${index}`"
                            class="param-preview-row"
                          >
                            <strong>{{ param.paramName || '-' }}</strong>
                            <span>{{ getParamPreviewDescription(param, formData.datasetType) }}</span>
                          </div>
                        </div>
                        <n-empty v-else size="small" description="暂无查询参数" />
                      </div>
                    </n-popover>
                  </div>
                  <DatasetParamSchemaEditor
                    :model-value="formData.paramSchemaJson || []"
                    :readonly="isFormReadOnly"
                    :dataset-type="formData.datasetType"
                    :connection-id="formData.connectionId"
                    :table-name="formData.tableName"
                    :sql-text="formData.sqlText"
                    @update:model-value="value => formData.paramSchemaJson = value"
                  />
                </section>

                <section class="dataset-edit-panel dataset-edit-panel--settings" data-step-section="3">
                  <div class="panel-section-head">
                    <h3>执行设置</h3>
                  </div>
                  <div class="execution-settings">
                    <div class="setting-row">
                      <div class="setting-row__head">
                        <span>最大返回行数</span>
                        <strong>{{ formData.maxRows || 10000 }}</strong>
                      </div>
                      <div class="setting-row__control">
                        <n-slider
                          :value="formData.maxRows || 10000"
                          :disabled="isFormReadOnly"
                          :min="100"
                          :max="1000000"
                          :step="100"
                          @update:value="value => formData.maxRows = value"
                        />
                        <n-input-number
                          :value="formData.maxRows"
                          :disabled="isFormReadOnly"
                          :min="100"
                          :max="1000000"
                          :step="100"
                          @update:value="value => formData.maxRows = value"
                        />
                      </div>
                    </div>
                    <div class="setting-row">
                      <div class="setting-row__head">
                        <span>查询超时时间</span>
                        <strong>{{ formData.timeoutSeconds || 60 }} 秒</strong>
                      </div>
                      <div class="setting-row__control">
                        <n-slider
                          :value="formData.timeoutSeconds || 60"
                          :disabled="isFormReadOnly"
                          :min="1"
                          :max="1800"
                          :step="1"
                          @update:value="value => formData.timeoutSeconds = value"
                        />
                        <n-input-number
                          :value="formData.timeoutSeconds"
                          :disabled="isFormReadOnly"
                          :min="1"
                          :max="1800"
                          @update:value="value => formData.timeoutSeconds = value"
                        />
                      </div>
                    </div>
                    <label class="dataset-field dataset-field--full">
                      <span>缓存策略</span>
                      <n-radio-group
                        :value="formData.cacheEnabled === 1 ? 1 : 0"
                        :disabled="isFormReadOnly"
                        @update:value="value => handleCacheStrategyChange(value, formData)"
                      >
                        <n-radio-button :value="0">
                          不缓存
                        </n-radio-button>
                        <n-radio-button :value="1">
                          按时间缓存
                        </n-radio-button>
                        <n-radio-button :value="2" disabled>
                          按依赖缓存
                        </n-radio-button>
                      </n-radio-group>
                    </label>
                    <label v-if="formData.cacheEnabled === 1" class="dataset-field">
                      <span>缓存时长(秒)</span>
                      <n-input-number
                        :value="formData.cacheTtlSeconds"
                        :disabled="isFormReadOnly"
                        :min="1"
                        :max="86400"
                        @update:value="value => formData.cacheTtlSeconds = value"
                      />
                    </label>
                    <label class="dataset-field">
                      <span>结果集编码</span>
                      <NSelect
                        :value="formData.__resultEncoding || 'UTF-8'"
                        :options="resultEncodingOptions"
                        :disabled="isFormReadOnly"
                        @update:value="value => formData.__resultEncoding = value"
                      />
                    </label>
                    <label class="setting-switch-row">
                      <span>允许导出</span>
                      <n-switch
                        :value="formData.__allowExport ?? true"
                        :disabled="isFormReadOnly"
                        @update:value="value => formData.__allowExport = value"
                      />
                    </label>
                  </div>
                </section>

                <section class="dataset-edit-panel dataset-edit-panel--access" data-step-section="4">
                  <div class="panel-section-head">
                    <h3>权限控制</h3>
                  </div>

                  <div class="access-control-block">
                    <div class="access-mode-row">
                      <span>访问范围</span>
                      <n-radio-group
                        :value="formData.accessMode"
                        :disabled="isFormReadOnly"
                        @update:value="value => handleAccessModeChange(value, formData, updateValue)"
                      >
                        <n-radio value="PUBLIC">
                          公开（所有已登录用户可访问）
                        </n-radio>
                        <n-radio value="PRIVATE">
                          私有（仅授权用户/用户组可访问）
                        </n-radio>
                      </n-radio-group>
                    </div>

                    <div class="row-permission-strip">
                      <div class="row-permission-title">
                        <span>行级权限规则</span>
                        <n-checkbox
                          :checked="isRowScopeEnabled(formData)"
                          :disabled="isFormReadOnly"
                          @update:checked="checked => handleRowScopeEnabledChange(checked, formData, updateValue)"
                        >
                          根据用户属性设置权限
                        </n-checkbox>
                      </div>
                      <div class="row-scope-expression">
                        {{ getRowScopeConditionPreview(formData) }}
                      </div>
                      <div class="row-permission-rules">
                        <span
                          v-for="rule in getRowScopeRules(formData).filter(item => item.attribute && item.field)"
                          :key="rule.__key"
                          class="rule-chip"
                        >
                          {{ getRowScopeRuleLabel(rule) }}
                        </span>
                        <span v-if="getRowScopeConfiguredCount(formData) === 0" class="rule-chip rule-chip--empty">
                          暂无规则
                        </span>
                        <button
                          class="dataset-text-action"
                          type="button"
                          :disabled="isFormReadOnly"
                          @click="addRowScopeRule(formData, updateValue)"
                        >
                          + 添加规则
                        </button>
                      </div>
                      <div v-if="getRowScopeRules(formData).length" class="row-scope-rule-mini-list">
                        <div
                          v-for="(rule, index) in getRowScopeRules(formData)"
                          :key="rule.__key || index"
                          class="row-scope-rule-mini"
                        >
                          <NSelect
                            :value="rule.attribute"
                            :options="getRowScopeAttributeOptions(formData, rule)"
                            :disabled="isFormReadOnly || !isRowScopeEnabled(formData)"
                            clearable
                            placeholder="用户属性"
                            @update:value="value => handleRowScopeRuleAttributeChange(formData, rule, value, updateValue)"
                          />
                          <span>=</span>
                          <NSelect
                            :value="rule.field"
                            :options="getRowScopeFieldOptions(formData)"
                            :loading="rowScopeTableFieldLoading"
                            :disabled="isFormReadOnly || !isRowScopeEnabled(formData)"
                            clearable
                            filterable
                            placeholder="数据表字段"
                            @update:value="value => handleRowScopeRuleFieldChange(formData, rule, value, updateValue)"
                          />
                          <n-button
                            quaternary
                            :disabled="isFormReadOnly || !isRowScopeEnabled(formData)"
                            @click="removeRowScopeRule(formData, index, updateValue)"
                          >
                            <template #icon>
                              <i class="i-material-symbols:delete-outline-rounded" />
                            </template>
                          </n-button>
                        </div>
                      </div>
                    </div>

                    <div v-if="formData.accessMode === 'PRIVATE'" class="acl-editor acl-editor--compact">
                      <div class="acl-editor__toolbar">
                        <div>
                          <div class="acl-editor__title">
                            授权对象
                          </div>
                          <div class="acl-editor__hint">
                            已配置 {{ getAclCount(formData) }} 个授权主体。
                          </div>
                        </div>
                        <n-button
                          secondary
                          type="primary"
                          :disabled="isFormReadOnly || permissionOptionsLoading"
                          :loading="permissionOptionsLoading"
                          @click="addAclItem(formData, updateValue)"
                        >
                          <template #icon>
                            <i class="i-material-symbols:add-rounded" />
                          </template>
                          选择用户/用户组
                        </n-button>
                      </div>

                      <n-empty
                        v-if="!formData.aclItems || formData.aclItems.length === 0"
                        description="暂无授权主体"
                        size="small"
                      />
                      <div v-else class="acl-tag-list">
                        <span
                          v-for="(item, index) in formData.aclItems"
                          :key="item.__key || `${item.subjectType || 'ACL'}-${index}`"
                          class="acl-tag"
                        >
                          {{ getAclItemLabel(item) }}
                          <button
                            type="button"
                            :disabled="isFormReadOnly"
                            @click="removeAclItem(formData, index, updateValue)"
                          >
                            ×
                          </button>
                        </span>
                      </div>
                      <div v-if="formData.aclItems && formData.aclItems.length > 0" class="acl-rows">
                        <div
                          v-for="(item, index) in formData.aclItems"
                          :key="item.__key || `${item.subjectType || 'ACL'}-${index}`"
                          class="acl-row"
                        >
                          <NSelect
                            :value="item.subjectType"
                            :options="aclSubjectTypeOptions"
                            :disabled="isFormReadOnly"
                            :to="false"
                            @update:value="value => handleAclSubjectTypeChange(item, value, updateValue)"
                          />
                          <n-tree-select
                            v-if="item.subjectType === 'ORG'"
                            :value="item.subjectId"
                            :options="getAclOrgOptions(item)"
                            :disabled="isFormReadOnly || permissionOptionsLoading"
                            :loading="permissionOptionsLoading"
                            :to="false"
                            :virtual-scroll="false"
                            clearable
                            filterable
                            default-expand-all
                            placeholder="选择组织"
                            @update:value="value => handleAclSubjectIdChange(item, value, updateValue)"
                          />
                          <NSelect
                            v-else
                            :value="item.subjectId"
                            :options="getAclSubjectOptions(item)"
                            :disabled="isFormReadOnly || permissionOptionsLoading"
                            :loading="permissionOptionsLoading"
                            :to="false"
                            :virtual-scroll="false"
                            clearable
                            filterable
                            placeholder="选择授权主体"
                            @update:value="value => handleAclSubjectIdChange(item, value, updateValue)"
                          />
                          <NSelect
                            :value="item.accessLevel"
                            :options="accessLevelOptions"
                            :disabled="isFormReadOnly"
                            :to="false"
                            placeholder="权限级别"
                            @update:value="value => handleAclAccessLevelChange(item, value, updateValue)"
                          />
                          <n-button
                            quaternary
                            type="error"
                            :disabled="isFormReadOnly"
                            @click="removeAclItem(formData, index, updateValue)"
                          >
                            <template #icon>
                              <i class="i-material-symbols:delete-outline-rounded" />
                            </template>
                          </n-button>
                        </div>
                      </div>
                    </div>
                  </div>
                </section>

                <section class="dataset-edit-panel dataset-edit-panel--info">
                  <div class="panel-section-head">
                    <h3>数据集信息</h3>
                  </div>
                  <dl class="dataset-info-grid">
                    <div>
                      <dt>数据集ID</dt>
                      <dd>{{ formData.id || '保存后生成' }}</dd>
                    </div>
                    <div>
                      <dt>数据集类型</dt>
                      <dd>{{ getDatasetTypeLabel(formData.datasetType) }}</dd>
                    </div>
                    <div>
                      <dt>数据源</dt>
                      <dd>{{ formData.connectionId ? getConnectionName(formData.connectionId) : '-' }}</dd>
                    </div>
                    <div>
                      <dt>创建人</dt>
                      <dd>{{ getDatasetCreatorLabel(formData) }}</dd>
                    </div>
                    <div>
                      <dt>创建时间</dt>
                      <dd>{{ formatDatasetDate(formData.createTime) }}</dd>
                    </div>
                    <div>
                      <dt>更新时间</dt>
                      <dd>{{ formatDatasetDate(formData.updateTime) }}</dd>
                    </div>
                    <div>
                      <dt>更新人</dt>
                      <dd>{{ getDatasetUpdaterLabel(formData) }}</dd>
                    </div>
                    <div>
                      <dt>版本号</dt>
                      <dd>
                        {{ getDatasetVersionLabel(formData) }}
                        <span class="dataset-current-version">当前版本</span>
                      </dd>
                    </div>
                  </dl>
                </section>
              </div>
            </div>
          </template>

          <template #form-stepIndicator>
            <div class="step-shell" :class="{ 'is-readonly': isFormReadOnly }" :style="stepShellStyle">
              <div class="step-shell__header">
                <div class="step-shell__intro">
                  <p class="step-shell__eyebrow">
                    Dataset Editing Flow
                  </p>
                  <div class="step-shell__title-row">
                    <h3 class="step-shell__title">
                      {{ currentStepMeta.title }}
                    </h3>
                    <span class="step-shell__progress">
                      STEP {{ currentStep }}/{{ totalSteps }}
                    </span>
                  </div>
                  <p class="step-shell__description">
                    {{ currentStepMeta.description }}
                  </p>
                </div>
                <span class="step-shell__status" :class="{ 'is-readonly': isFormReadOnly }">
                  {{ formModeLabel }}
                </span>
              </div>

              <div class="step-progress" :style="stepProgressWrapStyle">
                <div :style="stepProgressBaseLineStyle" />
                <div :style="stepProgressActiveLineStyle" />
                <template v-for="(step, index) in stepDefinitions" :key="step.label">
                  <div
                    class="step-node"
                    :style="getStepNodeInlineStyle(index)"
                    :class="{
                      'is-active': currentStep === index + 1,
                      'is-completed': currentStep > index + 1,
                    }"
                  >
                    <div class="step-circle">
                      <span v-if="currentStep <= index + 1">{{ index + 1 }}</span>
                      <i v-else class="i-material-symbols:check-rounded" />
                    </div>
                    <div class="step-node__meta">
                      <div class="step-label">
                        {{ step.label }}
                      </div>
                      <div class="step-caption">
                        {{ step.caption }}
                      </div>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </template>

          <template #form-stepNavigation="{ formData }">
            <div class="step-navigation-wrapper" :style="stepNavigationWrapperStyle">
              <div class="step-nav-actions" :style="stepNavigationActionsStyle">
                <div class="step-navigation-meta" :style="stepNavigationMetaStyle">
                  <span class="step-navigation-meta__label">当前步骤</span>
                  <strong class="step-navigation-meta__title">{{ currentStepMeta.label }}</strong>
                  <span class="step-navigation-meta__desc">{{ stepNavigationNote }}</span>
                </div>
                <n-button
                  quaternary
                  @click="crudRef?.closeModal()"
                >
                  取消
                </n-button>
                <n-button
                  v-if="currentStep > 1"
                  quaternary
                  @click="goToPrevStep"
                >
                  <template #icon>
                    <i class="i-material-symbols:arrow-back-rounded" />
                  </template>
                  上一步
                </n-button>
                <n-button
                  v-if="currentStep < totalSteps"
                  type="primary"
                  @click="goToNextStep(formData)"
                >
                  <template #icon>
                    <i class="i-material-symbols:arrow-forward-rounded" />
                  </template>
                  下一步
                </n-button>
                <n-button
                  v-if="currentStep === totalSteps && !isFormReadOnly"
                  type="success"
                  @click="crudRef?.submitForm()"
                >
                  <template #icon>
                    <i class="i-material-symbols:save-rounded" />
                  </template>
                  保存数据集
                </n-button>
              </div>
            </div>
          </template>

          <template #form-sqlText="{ value, updateValue }">
            <SqlEditor
              :value="value"
              :readonly="isFormReadOnly"
              placeholder="SELECT id, name FROM table_name WHERE status = 1"
              @update:value="updateValue"
            />
          </template>

          <template #form-sqlPreviewAction="{ formData }">
            <div class="sql-preview-action">
              <n-button
                type="primary"
                secondary
                :disabled="isFormReadOnly"
                :loading="sqlPreviewLoading"
                @click="handlePreviewSql(formData)"
              >
                预览SQL
              </n-button>
              <n-text depth="3">
                仅执行并展示前 10 条数据，用于校验 SQL 语句
              </n-text>
            </div>
          </template>

          <template #form-sourceGuide="{ formData }">
            <div class="dataset-context-panel">
              <div class="context-panel__main">
                <div class="context-panel__eyebrow">
                  来源配置
                </div>
                <div class="context-panel__title">
                  先确定接入方式，再继续字段同步或 SQL 建模
                </div>
                <div class="context-panel__desc">
                  {{ getDatasetSourceGuide(formData) }}
                </div>
              </div>
              <div class="context-panel__facts">
                <div class="context-fact">
                  <span>数据连接</span>
                  <strong>{{ formData.connectionId ? getConnectionName(formData.connectionId) : '待选择' }}</strong>
                </div>
                <div class="context-fact">
                  <span>接入方式</span>
                  <strong>{{ getDatasetTypeLabel(formData.datasetType) }}</strong>
                </div>
                <div class="context-fact">
                  <span>来源对象</span>
                  <strong>{{ getDatasetSourceSubject(formData) }}</strong>
                </div>
              </div>
            </div>
          </template>

          <template #form-paramGuide="{ formData }">
            <div class="dataset-context-panel dataset-context-panel--muted">
              <div class="context-panel__main">
                <div class="context-panel__eyebrow">
                  条件设计
                </div>
                <div class="context-panel__title">
                  只保留真正会被报表和运行时消费的筛选条件
                </div>
                <div class="context-panel__desc">
                  {{ getDatasetParamGuide(formData) }}
                </div>
              </div>
              <div class="context-panel__facts">
                <div class="context-fact">
                  <span>当前模式</span>
                  <strong>{{ getDatasetTypeLabel(formData.datasetType) }}</strong>
                </div>
                <div class="context-fact">
                  <span>{{ formData.datasetType === 'SQL' ? '识别参数' : '字段准备' }}</span>
                  <strong>{{ getDatasetParamReadiness(formData) }}</strong>
                </div>
                <div class="context-fact">
                  <span>约束要求</span>
                  <strong>{{ getDatasetParamConstraint(formData) }}</strong>
                </div>
              </div>
            </div>
          </template>

          <template #form-paramSchemaJson="{ value, updateValue, formData }">
            <DatasetParamSchemaEditor
              :model-value="value || []"
              :readonly="isFormReadOnly"
              :dataset-type="formData.datasetType"
              :connection-id="formData.connectionId"
              :table-name="formData.tableName"
              :sql-text="formData.sqlText"
              @update:model-value="updateValue"
            />
          </template>

          <template #form-settingGuide="{ formData }">
            <div class="dataset-context-panel dataset-context-panel--compact">
              <div class="context-panel__main">
                <div class="context-panel__eyebrow">
                  运行建议
                </div>
                <div class="context-panel__title">
                  控制查询边界，保证运行稳定性
                </div>
                <div class="context-panel__desc">
                  最大返回行数和超时时间会直接影响下游报表查询体验与系统负载。
                </div>
              </div>
              <div class="context-panel__facts">
                <div class="context-fact">
                  <span>最大行数</span>
                  <strong>{{ formData.maxRows || 1000 }}</strong>
                </div>
                <div class="context-fact">
                  <span>超时时间</span>
                  <strong>{{ formData.timeoutSeconds || 15 }}s</strong>
                </div>
                <div class="context-fact context-fact--wide">
                  <span>治理建议</span>
                  <strong>{{ formData.datasetType === 'SQL' ? '复杂 SQL 建议收紧阈值' : '单表模式建议保持轻量' }}</strong>
                </div>
              </div>
              <div class="context-panel__footnote">
                {{ formData.datasetType === 'SQL' ? '复杂 SQL 建议收紧返回行数和超时时间，避免拖慢报表查询。' : '单表数据集建议保持轻量，先同步字段再逐步补充筛选条件。' }}
              </div>
            </div>
          </template>

          <template #form-accessPermissionConfig="{ formData, updateValue }">
            <div class="permission-panel">
              <div class="permission-panel__header">
                <div>
                  <div class="context-panel__eyebrow">
                    Access Control
                  </div>
                  <div class="permission-panel__title">
                    数据集访问权限
                  </div>
                  <div class="permission-panel__desc">
                    公开数据集保持现有使用方式；私有数据集只对创建人、管理员和授权主体可见可用。
                  </div>
                </div>
                <n-radio-group
                  :value="formData.accessMode"
                  :disabled="isFormReadOnly"
                  @update:value="value => handleAccessModeChange(value, formData, updateValue)"
                >
                  <n-radio-button
                    v-for="option in accessModeOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </n-radio-button>
                </n-radio-group>
              </div>

              <div class="permission-facts">
                <div class="permission-fact">
                  <span>访问模式</span>
                  <strong>{{ getAccessModeLabel(formData.accessMode) }}</strong>
                </div>
                <div class="permission-fact">
                  <span>授权主体</span>
                  <strong>{{ getAclCount(formData) }} 个</strong>
                </div>
                <div class="permission-fact">
                  <span>默认兼容</span>
                  <strong>{{ formData.accessMode === 'PRIVATE' ? '按 ACL 校验' : '保持公开' }}</strong>
                </div>
              </div>

              <div v-if="formData.accessMode === 'PRIVATE'" class="acl-editor">
                <div class="acl-editor__toolbar">
                  <div>
                    <div class="acl-editor__title">
                      授权主体
                    </div>
                    <div class="acl-editor__hint">
                      支持角色、用户和组织授权，查询权限包含查看，管理权限包含查询。
                    </div>
                  </div>
                  <n-button
                    type="primary"
                    secondary
                    :disabled="isFormReadOnly || permissionOptionsLoading"
                    :loading="permissionOptionsLoading"
                    @click="addAclItem(formData, updateValue)"
                  >
                    <template #icon>
                      <i class="i-material-symbols:add-rounded" />
                    </template>
                    添加授权
                  </n-button>
                </div>

                <n-empty
                  v-if="!formData.aclItems || formData.aclItems.length === 0"
                  description="暂无授权主体，当前仅创建人和管理员可访问"
                  size="small"
                />
                <div v-else class="acl-rows">
                  <div
                    v-for="(item, index) in formData.aclItems"
                    :key="item.__key || `${item.subjectType || 'ACL'}-${index}`"
                    class="acl-row"
                  >
                    <NSelect
                      :value="item.subjectType"
                      :options="aclSubjectTypeOptions"
                      :disabled="isFormReadOnly"
                      :to="false"
                      @update:value="value => handleAclSubjectTypeChange(item, value, updateValue)"
                    />
                    <n-tree-select
                      v-if="item.subjectType === 'ORG'"
                      :value="item.subjectId"
                      :options="getAclOrgOptions(item)"
                      :disabled="isFormReadOnly || permissionOptionsLoading"
                      :loading="permissionOptionsLoading"
                      :to="false"
                      :virtual-scroll="false"
                      clearable
                      filterable
                      default-expand-all
                      placeholder="选择组织"
                      @update:value="value => handleAclSubjectIdChange(item, value, updateValue)"
                    />
                    <NSelect
                      v-else
                      :value="item.subjectId"
                      :options="getAclSubjectOptions(item)"
                      :disabled="isFormReadOnly || permissionOptionsLoading"
                      :loading="permissionOptionsLoading"
                      :to="false"
                      :virtual-scroll="false"
                      clearable
                      filterable
                      placeholder="选择授权主体"
                      @update:value="value => handleAclSubjectIdChange(item, value, updateValue)"
                    />
                    <NSelect
                      :value="item.accessLevel"
                      :options="accessLevelOptions"
                      :disabled="isFormReadOnly"
                      :to="false"
                      placeholder="权限级别"
                      @update:value="value => handleAclAccessLevelChange(item, value, updateValue)"
                    />
                    <n-button
                      quaternary
                      type="error"
                      :disabled="isFormReadOnly"
                      @click="removeAclItem(formData, index, updateValue)"
                    >
                      <template #icon>
                        <i class="i-material-symbols:delete-outline-rounded" />
                      </template>
                    </n-button>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template #form-rowScopeConfig="{ formData, updateValue }">
            <div class="permission-panel row-scope-panel">
              <div class="permission-panel__header">
                <div>
                  <div class="context-panel__eyebrow">
                    Row Scope
                  </div>
                  <div class="permission-panel__title">
                    行级权限设置
                  </div>
                  <div class="permission-panel__desc">
                    后端会按当前用户角色绑定的数据范围自动选择生效规则；这里仅维护用户属性与数据表字段的映射，不再手动选择区划范围。
                  </div>
                </div>
                <n-button
                  class="row-permission-add"
                  type="primary"
                  :disabled="isFormReadOnly"
                  @click="addRowScopeRule(formData, updateValue)"
                >
                  <template #icon>
                    <i class="i-material-symbols:add-rounded" />
                  </template>
                  添加
                </n-button>
              </div>

              <div class="permission-facts">
                <div class="permission-fact">
                  <span>权限来源</span>
                  <strong>角色绑定的数据范围</strong>
                </div>
                <div class="permission-fact">
                  <span>字段来源</span>
                  <strong>{{ getRowScopeFieldSourceLabel(formData) }}</strong>
                </div>
                <div class="permission-fact">
                  <span>映射规则</span>
                  <strong>{{ getRowScopeConfiguredCount(formData) }} 条已配置</strong>
                </div>
              </div>

              <div class="row-permission-switch">
                <n-checkbox
                  :checked="isRowScopeEnabled(formData)"
                  :disabled="isFormReadOnly"
                  @update:checked="checked => handleRowScopeEnabledChange(checked, formData, updateValue)"
                >
                  根据用户属性设置权限
                </n-checkbox>
                <span>
                  角色数据范围为“本部门 / 本人 / 行政区划”等模式时，会匹配下方对应字段；未配置的字段不会参与过滤。
                </span>
              </div>

              <n-alert
                v-if="formData.datasetType === 'SQL' && isRowScopeEnabled(formData)"
                type="warning"
                :show-icon="false"
                class="row-scope-alert"
              >
                SQL 数据集启用行权限时，需要在 SQL 的过滤位置预留 /*DATA_SCOPE*/ 占位符。
              </n-alert>

              <n-alert
                v-if="isRowScopeEnabled(formData) && getRowScopeFieldOptions(formData).length === 0"
                type="warning"
                :show-icon="false"
                class="row-scope-alert"
              >
                当前暂无可选字段，请先保存并同步字段；单表数据集也可以刷新来源表字段后再配置。
              </n-alert>

              <div class="row-scope-rule-builder" :class="{ 'is-disabled': !isRowScopeEnabled(formData) }">
                <div class="row-scope-rule-titlebar">
                  <div>
                    <div class="row-scope-rule-title">
                      权限设置
                    </div>
                    <div class="row-scope-rule-desc">
                      规则结构为：用户属性 = 数据表字段，条件关系用于多条规则的可读化拼接。
                    </div>
                  </div>
                  <n-button
                    secondary
                    :disabled="isFormReadOnly || formData.datasetType !== 'TABLE' || !formData.connectionId || !formData.tableName"
                    :loading="rowScopeTableFieldLoading"
                    @click="loadRowScopeTableFields(formData, { force: true })"
                  >
                    <template #icon>
                      <i class="i-material-symbols:refresh-rounded" />
                    </template>
                    刷新字段
                  </n-button>
                </div>

                <div class="row-scope-attribute-strip">
                  <div
                    v-for="option in rowScopeAttributeOptions"
                    :key="option.value"
                    class="row-scope-attribute-chip"
                  >
                    <span>{{ option.label }}</span>
                    <small>{{ option.caption }}</small>
                  </div>
                </div>

                <div class="row-scope-rule-header">
                  <span>用户属性</span>
                  <span />
                  <span>表字段</span>
                  <span>条件关系</span>
                  <span />
                </div>

                <n-empty
                  v-if="getRowScopeRules(formData).length === 0"
                  description="暂无行权限规则，点击右上角添加"
                  size="small"
                  class="row-scope-empty"
                />
                <div
                  v-for="(rule, index) in getRowScopeRules(formData)"
                  :key="rule.__key || index"
                  class="row-scope-rule-row"
                >
                  <NSelect
                    :value="rule.attribute"
                    :options="getRowScopeAttributeOptions(formData, rule)"
                    :disabled="isFormReadOnly || !isRowScopeEnabled(formData)"
                    clearable
                    placeholder="选择用户属性"
                    @update:value="value => handleRowScopeRuleAttributeChange(formData, rule, value, updateValue)"
                  />
                  <div class="row-scope-equals">
                    =
                  </div>
                  <NSelect
                    :value="rule.field"
                    :options="getRowScopeFieldOptions(formData)"
                    :loading="rowScopeTableFieldLoading"
                    :disabled="isFormReadOnly || !isRowScopeEnabled(formData)"
                    clearable
                    filterable
                    placeholder="选择数据表字段"
                    @update:value="value => handleRowScopeRuleFieldChange(formData, rule, value, updateValue)"
                  />
                  <NSelect
                    :value="rule.logic"
                    :options="rowScopeLogicOptions"
                    :disabled="isFormReadOnly || !isRowScopeEnabled(formData) || index === getRowScopeRules(formData).length - 1"
                    placeholder="关系"
                    @update:value="value => handleRowScopeRuleLogicChange(rule, value, updateValue)"
                  />
                  <n-button
                    quaternary
                    class="row-scope-delete"
                    :disabled="isFormReadOnly || !isRowScopeEnabled(formData)"
                    @click="removeRowScopeRule(formData, index, updateValue)"
                  >
                    <template #icon>
                      <i class="i-material-symbols:delete-outline-rounded" />
                    </template>
                  </n-button>
                </div>

                <div class="row-scope-condition-preview">
                  {{ getRowScopeConditionPreview(formData) }}
                </div>
              </div>

              <div class="row-scope-remark">
                <label>备注</label>
                <NInput
                  :value="getRowScopeRemark(formData)"
                  type="textarea"
                  :disabled="isFormReadOnly || !isRowScopeEnabled(formData)"
                  :autosize="{ minRows: 2, maxRows: 4 }"
                  placeholder="记录该数据集行权限字段口径"
                  @update:value="value => handleRowScopeRemarkChange(formData, value, updateValue)"
                />
              </div>
            </div>
          </template>
        </AiCrudPage>
      </section>
    </div>

    <n-modal
      v-model:show="fieldModalVisible"
      preset="card"
      :title="fieldModalTitle"
      :style="{ width: 'min(1320px, calc(100vw - 32px))' }"
      :segmented="{ content: 'soft' }"
      :mask-closable="false"
    >
      <div class="field-config-modal">
        <div class="field-config-head">
          <div>
            <div class="field-config-title">
              字段配置台
            </div>
            <div class="field-config-desc">
              维护显示名称、标准类型、字段角色和扩展属性。筛选/展示开关不再外露，保持运行时默认可用。
            </div>
          </div>
          <div class="field-config-stats">
            <div v-for="item in fieldConfigStats" :key="item.label" class="field-config-stat">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>

        <n-alert v-if="fieldConfigReadonly" type="warning" :show-icon="false" class="field-config-alert">
          当前数据集已发布，字段配置处于只读状态。如需调整，请先下架数据集。
        </n-alert>

        <n-data-table
          class="field-config-table"
          :columns="fieldColumns"
          :data="fieldRows"
          :loading="fieldLoading"
          :pagination="{ pageSize: 10 }"
          :scroll-x="fieldTableScrollX"
          max-height="calc(100vh - 390px)"
          size="small"
          striped
        />
      </div>

      <template #footer>
        <div class="field-config-footer">
          <n-button @click="fieldModalVisible = false">
            关闭
          </n-button>
          <n-button
            v-if="currentFieldDataset?.publishStatus !== 1"
            secondary
            :loading="fieldLoading"
            @click="handleSyncCurrentFields"
          >
            同步字段
          </n-button>
          <n-button
            v-if="currentFieldDataset?.publishStatus !== 1"
            type="primary"
            :loading="fieldSaving"
            @click="handleSaveFieldConfig"
          >
            保存字段配置
          </n-button>
        </div>
      </template>
    </n-modal>

    <n-modal
      v-model:show="sqlPreviewVisible"
      preset="card"
      title="SQL预览结果"
      style="width: 1000px"
      :segmented="{ content: 'soft' }"
    >
      <n-data-table
        :columns="sqlPreviewColumns"
        :data="sqlPreviewRows"
        :loading="sqlPreviewLoading"
        :pagination="{ pageSize: 10 }"
        :scroll-x="sqlPreviewScrollX"
        size="small"
      />
    </n-modal>
  </div>
</template>

<script setup>
import { NInput, NSelect, NTag } from 'naive-ui'
import { computed, h, nextTick, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDataConnectionFields, getDataConnectionList, getDataConnectionTables } from '@/api/data/connection'
import {
  deleteDataDataset,
  getDashboardDatasetImpact,
  getDataDatasetById,
  getDataDatasetCategoryTree,
  offlineDataDataset,
  publishDataDataset,
  saveDataDatasetFields,
  syncDataDatasetFields,
} from '@/api/data/dataset'
import { getDataDimensionList } from '@/api/data/dimension'
import { AiCrudPage } from '@/components/ai-form'
import DatasetParamSchemaEditor from '@/components/data/DatasetParamSchemaEditor.vue'
import DictTag from '@/components/DictTag.vue'
import SqlEditor from '@/components/SqlEditor.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'

defineOptions({ name: 'DataDataset' })

const { dict } = useDict(
  'data_dataset_type',
  'sys_enable_disable',
  'data_dataset_publish_status',
  'data_dataset_access_mode',
  'data_acl_subject_type',
  'data_acl_access_level',
  'data_field_role',
  'data_field_sensitive_level',
)

const router = useRouter()
const crudRef = ref(null)
const connectionOptions = ref([])
const categoryTree = ref([])
const categoryKeyword = ref('')
const tableOptions = ref([])
const dimensionOptions = ref([])
const tableLoading = ref(false)
const rowScopeTableFieldLoading = ref(false)
const loadedTableConnectionId = ref(null)
const loadingTableConnectionId = ref(null)
const rowScopeTableFieldKey = ref('')
const rowScopeTableFieldOptions = ref([])
const fieldModalVisible = ref(false)
const fieldLoading = ref(false)
const fieldSaving = ref(false)
const fieldModalTitle = ref('字段列表')
const fieldRows = ref([])
const currentFieldDataset = ref(null)
const sqlPreviewVisible = ref(false)
const sqlPreviewLoading = ref(false)
const sqlPreviewColumns = ref([])
const sqlPreviewRows = ref([])
const sqlPreviewScrollX = ref(0)
const roleOptions = ref([])
const userOptions = ref([])
const orgTreeOptions = ref([])
const permissionOptionsLoaded = ref(false)
const permissionOptionsLoading = ref(false)
const activeCategoryScope = ref('all')
const selectedCategoryId = ref(null)
const currentFormMode = ref('edit')
const currentEditingDataset = ref(null)
const currentStep = ref(1)
const stepDefinitions = [
  {
    label: '基础信息',
    caption: '定义数据集基本信息与SQL',
    title: '定义数据集基本信息与SQL',
    description: '配置数据集名称、编码、所属目录、数据源和 SQL 或数据表来源。',
  },
  {
    label: '查询条件',
    caption: '配置参数化查询条件',
    title: '配置参数化查询条件',
    description: '维护报表侧可绑定的查询条件，保证参数名、字段映射和默认值可预期。',
  },
  {
    label: '执行设置',
    caption: '设置执行限制与调度策略',
    title: '设置执行限制与调度策略',
    description: '控制返回行数、超时时间和缓存策略，让数据集在大屏运行时保持稳定。',
  },
  {
    label: '权限控制',
    caption: '配置数据访问权限策略',
    title: '配置数据访问权限策略',
    description: '配置公开或私有访问范围，并按用户属性映射行级数据权限。',
  },
]
const totalSteps = stepDefinitions.length
let permissionOptionsRequest = null

const queryForm = reactive({
  datasetName: '',
  connectionId: null,
  datasetType: null,
  publishStatus: null,
})

const datasetStats = reactive({
  total: 0,
  published: 0,
  editable: 0,
})

const datasetTypeOptions = computed(() => dict.value.data_dataset_type || [])

const statusOptions = computed(() => dict.value.sys_enable_disable || [])

const resultEncodingOptions = [
  { label: 'UTF-8', value: 'UTF-8' },
  { label: 'GBK', value: 'GBK' },
]

const publishStatusOptions = computed(() => dict.value.data_dataset_publish_status || [])

const datasetImpactLimit = 10
const datasetImpactVisibleLimit = 6

const accessModeOptions = computed(() => dict.value.data_dataset_access_mode || [])

const aclSubjectTypeOptions = computed(() => dict.value.data_acl_subject_type || [])

const accessLevelOptions = computed(() => dict.value.data_acl_access_level || [])

const rowScopeAttributeOptions = [
  { label: '租户 ID', value: 'tenantColumn', caption: '匹配当前登录租户' },
  { label: '组织 ID', value: 'orgColumn', caption: '匹配用户所属组织' },
  { label: '用户 ID', value: 'userColumn', caption: '匹配当前登录用户' },
  { label: '行政区划', value: 'regionColumn', caption: '匹配地市 / 区县编码' },
]

const rowScopeLogicOptions = [
  { label: 'AND', value: 'AND' },
  { label: 'OR', value: 'OR' },
]

const dataTypeOptions = [
  { label: '文本 STRING', value: 'STRING' },
  { label: '数值 NUMBER', value: 'NUMBER' },
  { label: '日期 DATE', value: 'DATE' },
  { label: '日期时间 DATETIME', value: 'DATETIME' },
  { label: '布尔 BOOLEAN', value: 'BOOLEAN' },
]

const fieldRoleOptions = computed(() => dict.value.data_field_role || [])

const sensitiveLevelOptions = computed(() => dict.value.data_field_sensitive_level || [])

const maskRuleOptions = [
  { label: '默认：保留前2后2', value: '__DEFAULT__' },
  { label: '手机号：隐藏中间4位', value: '(?<=\\d{3})\\d{4}(?=\\d{4})' },
  { label: '身份证：隐藏出生日期', value: '(?<=\\d{6})\\d{8}(?=\\d{4})' },
  { label: '银行卡：保留前4后4', value: '(?<=\\d{4})\\d+(?=\\d{4})' },
]

const dateFormatOptions = [
  { label: 'yyyy-MM-dd', value: 'yyyy-MM-dd' },
  { label: 'yyyy-MM-dd HH:mm:ss', value: 'yyyy-MM-dd HH:mm:ss' },
  { label: 'yyyy/MM/dd', value: 'yyyy/MM/dd' },
  { label: 'yyyy年MM月dd日', value: 'yyyy年MM月dd日' },
]

const dataUnitOptions = [
  { label: '元', value: '元' },
  { label: '万元', value: '万元' },
  { label: '%', value: '%' },
  { label: '人', value: '人' },
  { label: '次', value: '次' },
  { label: '件', value: '件' },
  { label: '天', value: '天' },
]

const supportedParamOperators = ['=', '!=', '>', '>=', '<', '<=', 'LIKE']

const isFormReadOnly = computed(() => currentFormMode.value === 'view' || currentEditingDataset.value?.publishStatus === 1)
const fieldConfigReadonly = computed(() => currentFieldDataset.value?.publishStatus === 1)
const selectedCategoryNode = computed(() => findCategoryById(categoryTree.value, selectedCategoryId.value))
const selectedTreeKeys = computed(() => activeCategoryScope.value === 'category' && selectedCategoryId.value ? [selectedCategoryId.value] : [])
const currentStepMeta = computed(() => stepDefinitions[currentStep.value - 1] || stepDefinitions[0])
const formModeLabel = computed(() => {
  if (currentEditingDataset.value?.publishStatus === 1) {
    return '已发布 · 只读浏览'
  }
  if (currentFormMode.value === 'add') {
    return '新增草稿'
  }
  if (currentFormMode.value === 'view') {
    return '只读查看'
  }
  return '编辑草稿'
})
const stepNavigationNote = computed(() => {
  if (isFormReadOnly.value) {
    return '当前为只读模式，可继续浏览各步骤内容'
  }
  return currentStepMeta.value.caption
})
const stepProgressPercent = computed(() => {
  if (totalSteps <= 1) {
    return 0
  }
  return ((currentStep.value - 1) / (totalSteps - 1)) * 100
})
const stepShellStyle = {
  width: '100%',
  boxSizing: 'border-box',
}
const stepProgressWrapStyle = computed(() => ({
  position: 'relative',
  display: 'grid',
  gridTemplateColumns: `repeat(${totalSteps}, minmax(0, 1fr))`,
  gap: '0',
  width: '100%',
  maxWidth: 'none',
  boxSizing: 'border-box',
  paddingTop: '4px',
}))
const stepProgressBaseLineStyle = {
  position: 'absolute',
  top: '26px',
  left: '22px',
  right: '22px',
  height: '2px',
  background: '#dbe3ef',
}
const stepProgressActiveLineStyle = computed(() => ({
  position: 'absolute',
  top: '26px',
  left: '22px',
  width: `calc((100% - 44px) * ${stepProgressPercent.value / 100})`,
  height: '2px',
  background: 'linear-gradient(90deg, #0f172a 0%, #1d4ed8 100%)',
  transition: 'width 0.24s ease',
}))
const stepNavigationWrapperStyle = {
  display: 'flex',
  justifyContent: 'flex-end',
  alignItems: 'center',
  gap: '16px',
  width: '100%',
  boxSizing: 'border-box',
}
const stepNavigationActionsStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'flex-end',
  flexWrap: 'wrap',
  gap: '12px',
  width: '100%',
}
const stepNavigationMetaStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: '4px',
  minWidth: '0',
  textAlign: 'right',
}

const activeCategoryScopeLabel = computed(() => {
  if (activeCategoryScope.value === 'uncategorized') {
    return '当前范围：未分类数据集'
  }
  if (activeCategoryScope.value === 'category' && selectedCategoryNode.value) {
    return `当前范围：${selectedCategoryNode.value.categoryName}`
  }
  return '当前范围：全部数据集'
})

const statCards = computed(() => [
  {
    key: 'total',
    label: '筛选结果',
    value: datasetStats.total,
    note: '匹配当前分类与搜索条件的数据集总数',
  },
  {
    key: 'published',
    label: '当前页已发布',
    value: datasetStats.published,
    note: '可被报表和运行时直接消费的数据集',
  },
  {
    key: 'editable',
    label: '当前页可编辑',
    value: datasetStats.editable,
    note: '未发布或已下架，允许继续调整结构',
  },
  {
    key: 'category',
    label: '分类总数',
    value: countTreeNodes(categoryTree.value),
    note: '用于业务划分的数据集分类节点数量',
  },
])

const categoryTreeNodes = computed(() => buildCategoryTreeNodes(filterCategoryTree(categoryTree.value, categoryKeyword.value)))
const categoryTreeSelectOptions = computed(() => buildCategorySelectOptions(categoryTree.value))
const fieldConfigStats = computed(() => {
  const rows = fieldRows.value || []
  return [
    { label: '字段总数', value: rows.length },
    { label: '维度字段', value: rows.filter(field => field.fieldRole === 'DIMENSION').length },
    { label: '已绑定维度', value: rows.filter(field => field.dimensionId).length },
    { label: '脱敏字段', value: rows.filter(field => field.sensitiveLevel === 'MASK' || field.sensitiveLevel === 'HIDDEN').length },
  ]
})

const tableColumns = computed(() => [
  {
    prop: 'datasetName',
    label: '数据集资产',
    width: 310,
    render: row => h('div', { class: 'asset-name-card' }, [
      h('div', { class: 'asset-name-row' }, [
        h('div', { class: 'asset-name' }, row.datasetName),
        h(NTag, {
          size: 'small',
          bordered: false,
          type: row.datasetType === 'TABLE' ? 'info' : 'warning',
        }, { default: () => row.datasetType === 'TABLE' ? '单表' : 'SQL' }),
      ]),
      h('div', { class: 'asset-code' }, row.datasetCode),
      h('div', { class: 'asset-desc' }, row.description || '暂无描述'),
    ]),
  },
  {
    prop: 'categoryName',
    label: '业务分类',
    width: 180,
    render: row => h('div', { class: 'asset-category' }, [
      h('div', { class: 'asset-category-name' }, row.categoryName || '未分类'),
      h('div', { class: 'asset-category-code' }, row.categoryCode || '暂未归档'),
    ]),
  },
  {
    prop: 'connectionId',
    label: '数据来源',
    width: 260,
    render: row => h('div', { class: 'asset-source' }, [
      h('div', { class: 'asset-source-name' }, row.connectionName || getConnectionName(row.connectionId)),
      h('div', { class: 'asset-source-detail' }, row.datasetType === 'TABLE'
        ? `表：${row.tableName || '-'}`
        : 'SQL 查询模式'),
    ]),
  },
  {
    prop: 'publishStatus',
    label: '发布状态',
    width: 110,
    render: row => h(DictTag, {
      dictType: 'data_dataset_publish_status',
      value: String(row.publishStatus),
      size: 'small',
    }),
  },
  {
    prop: 'accessMode',
    label: '访问权限',
    width: 110,
    render: row => h(DictTag, {
      dictType: 'data_dataset_access_mode',
      value: row.accessMode,
      size: 'small',
    }),
  },
  {
    prop: 'status',
    label: '可用状态',
    width: 110,
    render: row => h(DictTag, {
      dictType: 'sys_enable_disable',
      value: String(row.status),
      size: 'small',
    }),
  },
  { prop: 'maxRows', label: '最大行数', width: 100 },
  { prop: 'updateTime', label: '更新时间', width: 170 },
  {
    prop: 'action',
    label: '操作',
    width: 320,
    fixed: 'right',
    maxActionButtons: 3,
    actions: [
      { label: '编辑', key: 'edit', type: 'primary', visible: row => row.publishStatus !== 1, onClick: handleEdit },
      { label: '查看', key: 'view', type: 'primary', visible: row => row.publishStatus === 1, onClick: handleViewDataset },
      { label: '发布', key: 'publish', type: 'success', visible: row => row.publishStatus !== 1, onClick: handlePublishDataset },
      { label: '下架', key: 'offline', type: 'warning', visible: row => row.publishStatus === 1, onClick: handleOfflineDataset },
      { label: '字段配置', key: 'fields', type: 'info', onClick: handleViewFields },
      { label: '同步字段', key: 'sync', type: 'info', visible: row => row.publishStatus !== 1, onClick: handleSyncFields },
      { label: '删除', key: 'delete', type: 'error', visible: row => row.publishStatus !== 1, onClick: handleDelete },
    ],
  },
])

const fieldColumns = computed(() => [
  {
    title: '字段名',
    key: 'fieldName',
    width: 180,
    render: row => h('div', { class: 'field-name-cell' }, row.fieldName),
  },
  {
    title: '显示名称',
    key: 'fieldLabel',
    width: 190,
    render: row => renderFieldInput(row, 'fieldLabel', '请输入显示名称'),
  },
  {
    title: '字段说明',
    key: 'description',
    width: 250,
    render: row => renderFieldInput(row, 'description', '字段口径或配置说明'),
  },
  {
    title: '标准类型',
    key: 'dataType',
    width: 160,
    render: row => renderFieldSelect(row, 'dataType', dataTypeOptions, { placeholder: '标准类型' }),
  },
  {
    title: '字段角色',
    key: 'fieldRole',
    width: 140,
    render: row => renderFieldSelect(row, 'fieldRole', fieldRoleOptions.value, {
      placeholder: '字段角色',
      onChange: (value) => {
        if (value !== 'DIMENSION') {
          row.dimensionId = null
        }
      },
    }),
  },
  {
    title: '绑定维度',
    key: 'dimensionId',
    width: 220,
    render: (row) => {
      if (row.fieldRole !== 'DIMENSION') {
        return h('span', { class: 'field-muted-text' }, '指标字段无需绑定')
      }
      return renderFieldSelect(row, 'dimensionId', dimensionOptions.value, {
        placeholder: '选择维度翻译',
        clearable: true,
        filterable: true,
      })
    },
  },
  {
    title: '日期格式',
    key: 'dateFormat',
    width: 190,
    render: (row) => {
      if (!['DATE', 'DATETIME'].includes(row.dataType)) {
        return h('span', { class: 'field-muted-text' }, '非日期字段')
      }
      return renderFieldSelect(row, 'dateFormat', dateFormatOptions, {
        placeholder: '选择或输入格式',
        clearable: true,
        filterable: true,
        tag: true,
      })
    },
  },
  {
    title: '计量单位',
    key: 'dataUnit',
    width: 150,
    render: row => renderFieldSelect(row, 'dataUnit', dataUnitOptions, {
      placeholder: '单位',
      clearable: true,
      filterable: true,
      tag: true,
    }),
  },
  {
    title: '脱敏策略',
    key: 'sensitiveLevel',
    width: 150,
    render: row => renderFieldSelect(row, 'sensitiveLevel', sensitiveLevelOptions.value, { placeholder: '脱敏策略' }),
  },
  {
    title: '脱敏规则',
    key: 'maskRule',
    width: 230,
    render: (row) => {
      if (row.sensitiveLevel !== 'MASK') {
        return h('span', { class: 'field-muted-text' }, row.sensitiveLevel === 'HIDDEN' ? '字段隐藏' : '不脱敏')
      }
      return renderMaskRuleSelect(row)
    },
  },
  {
    title: '排序',
    key: 'sort',
    width: 170,
    render: row => fieldConfigReadonly.value
      ? h('span', { class: 'field-sort-value' }, row.sort ?? 0)
      : h('input', {
          class: 'field-sort-native-input',
          type: 'number',
          min: 0,
          value: row.sort ?? 0,
          onInput: event => row.sort = normalizeSortInput(event.target.value),
        }),
  },
  {
    title: '来源类型',
    key: 'dbType',
    width: 130,
    render: row => h(NTag, { size: 'small', bordered: false }, { default: () => row.dbType || '-' }),
  },
])

const fieldTableScrollX = computed(() => fieldColumns.value.reduce((total, column) => total + (Number(column.width) || 140), 0))

const editSchema = computed(() => [
  {
    field: 'datasetEditor',
    label: '',
    type: 'slot',
    slotName: 'datasetEditor',
    span: 12,
    showFeedback: false,
  },
])

loadConnectionOptions()
loadCategoryTree()
loadDimensionOptions()

async function loadConnectionOptions() {
  try {
    const res = await getDataConnectionList()
    if (res.code === 200 && Array.isArray(res.data)) {
      connectionOptions.value = res.data.map(item => ({
        label: item.connectionName,
        value: item.id,
      }))
    }
  }
  catch (error) {
    console.error('Failed to load connections', error)
  }
}

async function loadDimensionOptions() {
  try {
    const res = await getDataDimensionList()
    if (res.code === 200 && Array.isArray(res.data)) {
      dimensionOptions.value = res.data.map(item => ({
        label: `${item.dimensionName}（${item.dimensionCode}）`,
        value: item.id,
      }))
    }
  }
  catch (error) {
    console.error('Failed to load dimensions', error)
  }
}

async function loadCategoryTree(options = {}) {
  const { silent = false } = options
  try {
    const res = await getDataDatasetCategoryTree()
    if (res.code === 200 && Array.isArray(res.data)) {
      categoryTree.value = res.data
      if (activeCategoryScope.value === 'category' && selectedCategoryId.value && !findCategoryById(categoryTree.value, selectedCategoryId.value)) {
        activeCategoryScope.value = 'all'
        selectedCategoryId.value = null
      }
      return true
    }
    if (!silent) {
      window.$message?.error(res.msg || '加载数据集分类失败')
    }
    return false
  }
  catch (error) {
    console.error('Failed to load dataset categories', error)
    if (!silent) {
      window.$message?.error('加载数据集分类失败')
    }
    return false
  }
}

function getConnectionName(connectionId) {
  const connection = connectionOptions.value.find(item => item.value === connectionId)
  return connection?.label || connectionId || '-'
}

function getCategoryName(categoryId) {
  return findCategoryById(categoryTree.value, categoryId)?.categoryName || '未分类'
}

function formatDatasetDate(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

function setEditorStep(step, shouldScroll = false) {
  currentStep.value = step
  if (shouldScroll) {
    scrollToStepSection(step)
  }
}

async function scrollToStepSection(step) {
  await nextTick()
  const section = document.querySelector(`.data-dataset-edit-form [data-step-section="${step}"]`)
  section?.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
    inline: 'nearest',
  })
}

function getPublishStatusLabel(status) {
  const item = dict.value.data_dataset_publish_status?.find(d => d.value === String(status))
  return item?.label || '未发布'
}

function getAccessModeLabel(accessMode) {
  const item = dict.value.data_dataset_access_mode?.find(d => d.value === accessMode)
  return item?.label || '公开'
}

function getDatasetCreatorLabel(formData) {
  return formData?.createByName || formData?.creatorName || formData?.createBy || '-'
}

function getDatasetUpdaterLabel(formData) {
  return formData?.updateByName || formData?.updaterName || formData?.updateBy || '-'
}

function getDatasetVersionLabel(formData) {
  if (formData?.versionNo) {
    return `v${formData.versionNo}`
  }
  return formData?.id ? 'v1' : '保存后生成'
}

function getDatasetTagLabels(formData) {
  return [
    getDatasetTypeLabel(formData?.datasetType),
    getAccessModeLabel(formData?.accessMode),
    getCategoryName(formData?.categoryId),
  ].filter(Boolean)
}

function getDatasetSourceGuide(formData) {
  if (formData?.datasetType === 'SQL') {
    return 'SQL 数据集适合多表关联、预聚合和复杂过滤，保存前建议先执行 SQL 预览。'
  }
  return '单表数据集适合标准明细表和维表建模，字段同步会按所选数据表结构生成。'
}

function getDatasetTypeLabel(datasetType) {
  const item = dict.value.data_dataset_type?.find(d => d.value === datasetType)
  return item?.label || '单表数据集'
}

function getDatasetSourceSubject(formData) {
  if (formData?.datasetType === 'SQL') {
    return 'SQL 语句'
  }
  return formData?.tableName || '待选择数据表'
}

function getDatasetParamGuide(formData) {
  if (formData?.datasetType === 'SQL') {
    const paramCount = getSqlParamCount(formData?.sqlText)
    return paramCount > 0
      ? `当前 SQL 已识别 ${paramCount} 个命名参数，条件参数名需要与 SQL 中的 :param 完全一致。`
      : '先在 SQL 中写入 :paramName，再回到这里定义参数类型、默认值和是否必填。'
  }
  return '单表模式下每个查询条件都要映射到具体数据表字段，便于运行时安全拼装过滤条件。'
}

function getDatasetParamReadiness(formData) {
  if (formData?.datasetType === 'SQL') {
    return `${getSqlParamCount(formData?.sqlText)} 个命名参数`
  }
  return formData?.tableName ? '已绑定数据表' : '待选择数据表'
}

function getDatasetParamConstraint(formData) {
  if (formData?.datasetType === 'SQL') {
    return '参数名需与 SQL 保持一致'
  }
  return '每项都需要映射字段'
}

function getParamPreviewRows(formData) {
  const value = formData?.paramSchemaJson
  let rows = []
  if (Array.isArray(value)) {
    rows = value
  }
  else if (typeof value === 'string' && value) {
    try {
      const parsed = JSON.parse(value)
      rows = Array.isArray(parsed) ? parsed : []
    }
    catch {
      rows = []
    }
  }

  return rows.filter(row => row?.paramName || row?.label || row?.fieldName)
}

function getParamPreviewDescription(param, datasetType) {
  const parts = [
    param.label || null,
    param.dataType || 'STRING',
    param.required ? '必填' : '可选',
  ]
  if (datasetType === 'TABLE' && param.fieldName) {
    parts.push(`${param.operator || '='} ${param.fieldName}`)
  }
  return parts.filter(Boolean).join(' / ')
}

function getStepNodeInlineStyle(index) {
  if (index > 0 && index < totalSteps - 1) {
    return {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: '12px',
      minWidth: '0',
      position: 'relative',
      zIndex: 1,
      textAlign: 'center',
    }
  }

  if (index === totalSteps - 1) {
    return {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'flex-end',
      gap: '12px',
      minWidth: '0',
      position: 'relative',
      zIndex: 1,
      textAlign: 'right',
    }
  }

  return {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-start',
    gap: '12px',
    minWidth: '0',
    position: 'relative',
    zIndex: 1,
    textAlign: 'left',
  }
}

function getSqlParamCount(sqlText) {
  return extractSqlParamNames(sqlText).length
}

function extractSqlParamNames(sqlText) {
  if (!sqlText) {
    return []
  }

  const matches = sqlText.matchAll(/:([a-z_]\w*)/gi)
  return [...new Set(Array.from(matches, match => match[1]))]
}

function buildCategoryTreeNodes(tree) {
  return tree.map(item => ({
    key: item.id,
    label: item.status === 1 ? item.categoryName : `${item.categoryName} · 停用`,
    children: item.children?.length ? buildCategoryTreeNodes(item.children) : undefined,
  }))
}

function buildCategorySelectOptions(tree) {
  return tree.map(item => ({
    label: item.status === 1 ? item.categoryName : `${item.categoryName}（停用）`,
    value: item.id,
    key: item.id,
    children: item.children?.length ? buildCategorySelectOptions(item.children) : undefined,
  }))
}

function filterCategoryTree(tree, keyword) {
  const normalizedKeyword = keyword?.trim().toLowerCase()
  if (!normalizedKeyword) {
    return tree
  }

  return tree
    .map((item) => {
      const children = filterCategoryTree(item.children || [], keyword)
      const matched = item.categoryName?.toLowerCase().includes(normalizedKeyword)
        || item.categoryCode?.toLowerCase().includes(normalizedKeyword)
      if (!matched && children.length === 0) {
        return null
      }
      return {
        ...item,
        children,
      }
    })
    .filter(Boolean)
}

function findCategoryById(tree, id) {
  if (!id) {
    return null
  }
  for (const item of tree || []) {
    if (item.id === id) {
      return item
    }
    const child = findCategoryById(item.children, id)
    if (child) {
      return child
    }
  }
  return null
}

function countTreeNodes(tree) {
  return (tree || []).reduce((total, item) => total + 1 + countTreeNodes(item.children), 0)
}

function handleCategoryTreeSelect(keys) {
  const nextId = Array.isArray(keys) && keys.length > 0 ? keys[0] : null
  if (!nextId) {
    return
  }
  selectedCategoryId.value = nextId
  activeCategoryScope.value = 'category'
  applySearch()
}

function selectAllCategories() {
  activeCategoryScope.value = 'all'
  selectedCategoryId.value = null
  applySearch()
}

function selectUncategorized() {
  activeCategoryScope.value = 'uncategorized'
  selectedCategoryId.value = null
  applySearch()
}

function buildSearchParams() {
  return {
    datasetName: queryForm.datasetName?.trim() || undefined,
    connectionId: queryForm.connectionId || undefined,
    datasetType: queryForm.datasetType || undefined,
    publishStatus: queryForm.publishStatus ?? undefined,
    categoryId: activeCategoryScope.value === 'category' ? selectedCategoryId.value : undefined,
    uncategorized: activeCategoryScope.value === 'uncategorized' ? true : undefined,
  }
}

function applySearch() {
  crudRef.value?.search(buildSearchParams())
}

function handleResetFilters() {
  queryForm.datasetName = ''
  queryForm.connectionId = null
  queryForm.datasetType = null
  queryForm.publishStatus = null
  activeCategoryScope.value = 'all'
  selectedCategoryId.value = null
  crudRef.value?.search({})
}

function handleDatasetLoadSuccess({ list, total }) {
  datasetStats.total = total || 0
  datasetStats.published = (list || []).filter(item => item.publishStatus === 1).length
  datasetStats.editable = (list || []).filter(item => item.publishStatus !== 1).length
}

function handleAddDataset() {
  handleStepReset()
  currentFormMode.value = 'add'
  currentEditingDataset.value = null
  crudRef.value?.showAdd()
}

function handleEdit(row) {
  handleStepReset()
  currentFormMode.value = 'edit'
  currentEditingDataset.value = row
  crudRef.value?.showEdit({ ...row, __modalTitle: '编辑数据集' })
}

function handleViewDataset(row) {
  handleStepReset()
  currentFormMode.value = 'view'
  currentEditingDataset.value = row
  crudRef.value?.showEdit({ ...row, __modalTitle: '查看数据集' })
}

function handleDatasetModalClose() {
  currentFormMode.value = 'edit'
  currentEditingDataset.value = null
  handleStepReset()
}

function prepareDatasetFormData(sourceData = {}, options = {}) {
  const { applyScopeDefault = false } = options
  const nextFormData = {
    datasetType: 'TABLE',
    status: 1,
    maxRows: 1000,
    timeoutSeconds: 15,
    cacheEnabled: 0,
    cacheTtlSeconds: null,
    accessMode: 'PUBLIC',
    aclItems: [],
    rowScope: createDefaultRowScope(),
    ...sourceData,
  }

  if (applyScopeDefault && !nextFormData.categoryId && activeCategoryScope.value === 'category' && selectedCategoryId.value) {
    nextFormData.categoryId = selectedCategoryId.value
  }

  nextFormData.paramSchemaJson = parseParamSchemaFormValue(nextFormData.paramSchemaJson)
  nextFormData.accessMode = nextFormData.accessMode === 'PRIVATE' ? 'PRIVATE' : 'PUBLIC'
  nextFormData.aclItems = normalizeAclItems(nextFormData.aclItems)
  nextFormData.rowScope = normalizeRowScope(nextFormData.rowScope)
  return nextFormData
}

async function beforeRenderForm(formData) {
  const nextFormData = prepareDatasetFormData(formData || {}, {
    applyScopeDefault: !formData,
  })
  const connectionId = nextFormData.connectionId
  const datasetType = nextFormData.datasetType || 'TABLE'
  if (connectionId && datasetType === 'TABLE') {
    await loadTableOptions(connectionId)
    await loadRowScopeTableFields(nextFormData)
  }
  else {
    resetTableOptions()
    resetRowScopeTableFields()
  }
  if (nextFormData.accessMode === 'PRIVATE') {
    await loadPermissionOptions()
  }
  return nextFormData
}

async function beforeRenderDetail(detailData) {
  const nextFormData = prepareDatasetFormData(detailData || {})
  currentEditingDataset.value = nextFormData
  const connectionId = nextFormData.connectionId
  const datasetType = nextFormData.datasetType || 'TABLE'
  if (connectionId && datasetType === 'TABLE') {
    await loadTableOptions(connectionId)
    await loadRowScopeTableFields(nextFormData)
  }
  else {
    resetTableOptions()
    resetRowScopeTableFields()
  }
  if (nextFormData.accessMode === 'PRIVATE') {
    await loadPermissionOptions()
  }
  return nextFormData
}

async function handleConnectionChange(connectionId, formData) {
  formData.connectionId = connectionId
  formData.tableName = null
  clearRowScopeColumns(formData)
  resetRowScopeTableFields()
  if (formData.datasetType === 'TABLE') {
    await loadTableOptions(connectionId)
  }
}

async function handleDatasetTypeChange(datasetType, formData, updateValue) {
  if (isFormReadOnly.value || formData.datasetType === datasetType) {
    return
  }
  formData.datasetType = datasetType
  clearRowScopeColumns(formData)
  resetRowScopeTableFields()
  if (datasetType === 'TABLE') {
    formData.sqlText = null
    syncSlotForm(updateValue)
    await loadTableOptions(formData.connectionId)
    return
  }

  formData.tableName = null
  if (formData.sqlText === null || formData.sqlText === undefined) {
    formData.sqlText = ''
  }
  syncSlotForm(updateValue)
}

async function handleTableNameChange(tableName, formData) {
  formData.tableName = tableName
  clearRowScopeColumns(formData)
  if (!tableName) {
    resetRowScopeTableFields()
    return
  }
  await loadRowScopeTableFields(formData, { force: true })
}

function resetTableOptions() {
  tableOptions.value = []
  loadedTableConnectionId.value = null
  loadingTableConnectionId.value = null
}

function resetRowScopeTableFields() {
  rowScopeTableFieldKey.value = ''
  rowScopeTableFieldOptions.value = []
}

function toIdString(value) {
  if (value === null || value === undefined || value === '') {
    return null
  }
  return String(value)
}

async function loadTableOptions(connectionId) {
  if (!connectionId) {
    resetTableOptions()
    return
  }
  if (loadedTableConnectionId.value === connectionId && tableOptions.value.length > 0) {
    return
  }
  if (tableLoading.value && loadingTableConnectionId.value === connectionId) {
    return
  }

  tableLoading.value = true
  loadingTableConnectionId.value = connectionId
  try {
    const res = await getDataConnectionTables(connectionId)
    if (res.code === 200 && Array.isArray(res.data)) {
      tableOptions.value = res.data.map(table => ({
        label: table.tableComment ? `${table.tableName}（${table.tableComment}）` : table.tableName,
        value: table.tableName,
      }))
      loadedTableConnectionId.value = connectionId
    }
    else {
      resetTableOptions()
    }
  }
  catch (error) {
    console.error('Failed to load tables', error)
    resetTableOptions()
    window.$message?.error('加载数据表失败')
  }
  finally {
    tableLoading.value = false
    loadingTableConnectionId.value = null
  }
}

async function loadRowScopeTableFields(formData, options = {}) {
  const { force = false } = options
  if (!formData?.connectionId || formData.datasetType !== 'TABLE' || !formData.tableName) {
    resetRowScopeTableFields()
    return
  }

  const nextKey = `${formData.connectionId}:${formData.tableName}`
  if (!force && rowScopeTableFieldKey.value === nextKey && rowScopeTableFieldOptions.value.length > 0) {
    return
  }

  rowScopeTableFieldLoading.value = true
  try {
    const res = await getDataConnectionFields(formData.connectionId, formData.tableName)
    if (res.code === 200 && Array.isArray(res.data)) {
      rowScopeTableFieldOptions.value = res.data.map(field => ({
        label: field.columnComment ? `${field.columnName}（${field.columnComment}）` : field.columnName,
        value: field.columnName,
      }))
      rowScopeTableFieldKey.value = nextKey
    }
    else {
      resetRowScopeTableFields()
    }
  }
  catch (error) {
    console.error('Failed to load row scope table fields', error)
    resetRowScopeTableFields()
    window.$message?.error('加载数据表字段失败')
  }
  finally {
    rowScopeTableFieldLoading.value = false
  }
}

async function loadPermissionOptions() {
  if (permissionOptionsLoaded.value) {
    return
  }
  if (permissionOptionsRequest) {
    return permissionOptionsRequest
  }
  permissionOptionsLoading.value = true
  permissionOptionsRequest = Promise.all([
    loadRoleOptions(),
    loadUserOptions(),
    loadOrgOptions(),
  ])
    .then(() => {
      permissionOptionsLoaded.value = true
    })
    .finally(() => {
      permissionOptionsLoading.value = false
      permissionOptionsRequest = null
    })
  return permissionOptionsRequest
}

async function loadRoleOptions() {
  try {
    const res = await request.get('/system/role/page', {
      params: { pageNum: 1, pageSize: 1000 },
    })
    if (res.code === 200) {
      const rows = res.data?.records || res.data?.list || []
      roleOptions.value = rows.map(role => ({
        label: role.roleKey ? `${role.roleName}（${role.roleKey}）` : role.roleName,
        value: toIdString(role.id),
      }))
    }
  }
  catch (error) {
    console.error('Failed to load roles for dataset ACL', error)
  }
}

async function loadUserOptions() {
  try {
    const res = await request.get('/system/user/page', {
      params: { pageNum: 1, pageSize: 1000 },
    })
    if (res.code === 200) {
      const rows = res.data?.records || res.data?.list || []
      userOptions.value = rows.map(user => ({
        label: user.realName ? `${user.realName}（${user.username}）` : user.username,
        value: toIdString(user.id),
      }))
    }
  }
  catch (error) {
    console.error('Failed to load users for dataset ACL', error)
  }
}

async function loadOrgOptions() {
  try {
    const res = await request.get('/system/org/tree')
    if (res.code === 200) {
      orgTreeOptions.value = transformOrgTreeOptions(res.data || [])
    }
  }
  catch (error) {
    console.error('Failed to load org tree for dataset ACL', error)
  }
}

function transformOrgTreeOptions(tree) {
  return (tree || []).map(item => ({
    label: item.orgName,
    value: toIdString(item.id),
    key: toIdString(item.id),
    children: item.children?.length ? transformOrgTreeOptions(item.children) : undefined,
  }))
}

function beforeSubmit(formData) {
  if (isFormReadOnly.value) {
    return false
  }

  delete formData.datasetOverview
  delete formData.datasetEditor
  delete formData.stepIndicator
  delete formData.stepNavigation
  delete formData.__sectionBasic
  delete formData.__sectionSource
  delete formData.__sectionParam
  delete formData.__sectionSetting
  delete formData.__sectionAccessPermission
  delete formData.__sectionRowScope
  delete formData.sqlPreviewAction
  delete formData.sourceGuide
  delete formData.paramGuide
  delete formData.settingGuide
  delete formData.accessPermissionConfig
  delete formData.rowScopeConfig
  delete formData.__resultEncoding
  delete formData.__allowExport

  if (!formData.connectionId) {
    window.$message?.error('请选择数据连接')
    return false
  }

  if (formData.datasetType === 'TABLE') {
    if (!formData.tableName) {
      window.$message?.error('请选择数据表')
      return false
    }
    formData.sqlText = null
  }
  else if (formData.datasetType === 'SQL') {
    if (!formData.sqlText) {
      window.$message?.error('请输入查询SQL')
      return false
    }
    formData.tableName = null
  }

  const normalizedSchema = normalizeParamSchema(formData.paramSchemaJson, formData.datasetType)
  if (normalizedSchema === null) {
    return false
  }

  const normalizedAclItems = normalizeSubmitAclItems(formData)
  if (normalizedAclItems === null) {
    return false
  }

  const normalizedRowScope = normalizeSubmitRowScope(formData)
  if (normalizedRowScope === null) {
    return false
  }

  formData.paramSchemaJson = normalizedSchema.length > 0
    ? JSON.stringify(normalizedSchema, null, 2)
    : null
  formData.aclItems = normalizedAclItems
  formData.rowScope = normalizedRowScope

  return formData
}

function createDefaultAclItem() {
  return {
    __key: `${Date.now()}-${Math.random()}`,
    subjectType: 'ROLE',
    subjectId: null,
    accessLevel: 'QUERY',
  }
}

function createDefaultRowScope() {
  return {
    enabled: 0,
    scopeMode: 'SYSTEM_DATA_SCOPE',
    tenantColumn: null,
    orgColumn: null,
    userColumn: null,
    regionColumn: null,
    regionStrategy: 'SELF_AND_DESCENDANTS',
    ruleItems: [],
    remark: null,
  }
}

function normalizeAclItems(items) {
  if (!Array.isArray(items)) {
    return []
  }
  return items.map(item => ({
    __key: `${item.subjectType || 'ACL'}-${item.subjectId || 'NEW'}-${item.accessLevel || 'QUERY'}-${Math.random()}`,
    id: item.id,
    subjectType: normalizeAclSubjectType(item.subjectType),
    subjectId: toIdString(item.subjectId),
    accessLevel: normalizeAccessLevel(item.accessLevel),
  }))
}

function normalizeRowScope(rowScope) {
  const normalized = {
    ...createDefaultRowScope(),
    ...(rowScope || {}),
    enabled: rowScope?.enabled === 1 ? 1 : 0,
    regionStrategy: rowScope?.regionStrategy || 'SELF_AND_DESCENDANTS',
  }
  normalized.ruleItems = Array.isArray(rowScope?.ruleItems)
    ? rowScope.ruleItems.map(normalizeRowScopeRule).filter(Boolean)
    : buildRowScopeRuleItems(normalized)
  return normalized
}

function createRowScopeRule(attribute = null, field = null, logic = 'AND') {
  return {
    __key: `${Date.now()}-${Math.random()}`,
    attribute,
    field: field ?? null,
    logic: normalizeRowScopeLogic(logic),
  }
}

function buildRowScopeRuleItems(rowScope) {
  return rowScopeAttributeOptions
    .filter(option => rowScope?.[option.value])
    .map((option, index) => createRowScopeRule(option.value, rowScope[option.value], index === 0 ? 'AND' : 'AND'))
}

function normalizeRowScopeRule(rule) {
  if (!rule || typeof rule !== 'object') {
    return null
  }
  const attribute = rowScopeAttributeOptions.some(option => option.value === rule.attribute) ? rule.attribute : null
  return {
    __key: rule.__key || `${Date.now()}-${Math.random()}`,
    attribute,
    field: trimToNull(rule.field),
    logic: normalizeRowScopeLogic(rule.logic),
  }
}

function normalizeRowScopeLogic(logic) {
  return rowScopeLogicOptions.some(option => option.value === logic) ? logic : 'AND'
}

function normalizeAclSubjectType(subjectType) {
  return ['USER', 'ROLE', 'ORG'].includes(subjectType) ? subjectType : 'ROLE'
}

function normalizeAccessLevel(accessLevel) {
  return ['VIEW', 'QUERY', 'MANAGE'].includes(accessLevel) ? accessLevel : 'QUERY'
}

function syncSlotForm(updateValue) {
  if (typeof updateValue === 'function') {
    updateValue(null)
  }
}

async function handleAccessModeChange(value, formData, updateValue) {
  formData.accessMode = value === 'PRIVATE' ? 'PRIVATE' : 'PUBLIC'
  syncSlotForm(updateValue)
  if (formData.accessMode === 'PRIVATE') {
    await loadPermissionOptions()
    syncSlotForm(updateValue)
  }
}

function getAclCount(formData) {
  return Array.isArray(formData?.aclItems) ? formData.aclItems.length : 0
}

async function addAclItem(formData, updateValue) {
  await loadPermissionOptions()
  if (!Array.isArray(formData.aclItems)) {
    formData.aclItems = []
  }
  formData.aclItems.push(createDefaultAclItem())
  if (formData.accessMode !== 'PRIVATE') {
    formData.accessMode = 'PRIVATE'
  }
  syncSlotForm(updateValue)
}

function removeAclItem(formData, index, updateValue) {
  if (!Array.isArray(formData.aclItems)) {
    return
  }
  formData.aclItems.splice(index, 1)
  syncSlotForm(updateValue)
}

function handleAclSubjectTypeChange(item, value, updateValue) {
  item.subjectType = normalizeAclSubjectType(value)
  item.subjectId = null
  syncSlotForm(updateValue)
}

function handleAclSubjectIdChange(item, value, updateValue) {
  item.subjectId = toIdString(value)
  syncSlotForm(updateValue)
}

function handleAclAccessLevelChange(item, value, updateValue) {
  item.accessLevel = normalizeAccessLevel(value)
  syncSlotForm(updateValue)
}

function getAclItemLabel(item) {
  const subjectTypeLabel = aclSubjectTypeOptions.value.find(option => option.value === item?.subjectType)?.label || '授权主体'
  const accessLevelLabel = accessLevelOptions.value.find(option => option.value === item?.accessLevel)?.label || '查询'
  if (!item?.subjectId) {
    return `${subjectTypeLabel} · 待选择 · ${accessLevelLabel}`
  }
  if (item.subjectType === 'ORG') {
    const option = findTreeOption(orgTreeOptions.value, toIdString(item.subjectId))
    return `${option?.label || `组织 #${item.subjectId}`} · ${accessLevelLabel}`
  }
  const options = item.subjectType === 'USER' ? userOptions.value : roleOptions.value
  const option = options.find(row => toIdString(row.value) === toIdString(item.subjectId))
  return `${option?.label || getAclSubjectFallbackLabel(item)} · ${accessLevelLabel}`
}

function findTreeOption(options, value) {
  const normalizedValue = toIdString(value)
  for (const option of options || []) {
    if (toIdString(option.value) === normalizedValue) {
      return option
    }
    const child = findTreeOption(option.children || [], normalizedValue)
    if (child) {
      return child
    }
  }
  return null
}

function getAclSubjectOptions(item) {
  const baseOptions = item?.subjectType === 'USER' ? userOptions.value : roleOptions.value
  return appendMissingFlatOption(baseOptions, item?.subjectId, getAclSubjectFallbackLabel(item))
}

function getAclOrgOptions(item) {
  return appendMissingTreeOption(orgTreeOptions.value, item?.subjectId, getAclSubjectFallbackLabel(item))
}

function getAclSubjectFallbackLabel(item) {
  if (!item?.subjectId) {
    return ''
  }
  if (item.subjectType === 'USER') {
    return `用户 #${item.subjectId}`
  }
  if (item.subjectType === 'ORG') {
    return `组织 #${item.subjectId}`
  }
  return `角色 #${item.subjectId}`
}

function appendMissingFlatOption(options, value, label) {
  const normalizedValue = toIdString(value)
  if (!normalizedValue || options.some(item => toIdString(item.value) === normalizedValue)) {
    return options
  }
  return [...options, { label, value: normalizedValue }]
}

function appendMissingTreeOption(options, value, label) {
  const normalizedValue = toIdString(value)
  if (!normalizedValue || containsTreeValue(options, normalizedValue)) {
    return options
  }
  return [...options, { label, value: normalizedValue, key: normalizedValue }]
}

function containsTreeValue(options, value) {
  const normalizedValue = toIdString(value)
  for (const option of options || []) {
    if (toIdString(option.value) === normalizedValue) {
      return true
    }
    if (containsTreeValue(option.children || [], normalizedValue)) {
      return true
    }
  }
  return false
}

function getRowScopeFieldOptions(formData) {
  const fields = Array.isArray(formData?.fields) ? formData.fields : []
  const datasetFieldOptions = fields
    .map(field => ({
      label: field.fieldLabel && field.fieldLabel !== field.fieldName
        ? `${field.fieldName}（${field.fieldLabel}）`
        : field.fieldName,
      value: field.sourceColumn || field.fieldName,
    }))
    .filter(item => item.value)
  if (datasetFieldOptions.length > 0) {
    return datasetFieldOptions
  }
  if (formData?.datasetType === 'TABLE') {
    return rowScopeTableFieldOptions.value
  }
  return []
}

function getRowScopeFieldSourceLabel(formData) {
  const fields = Array.isArray(formData?.fields) ? formData.fields : []
  if (fields.length > 0) {
    return `数据集字段 ${fields.length} 个`
  }
  if (formData?.datasetType === 'TABLE' && rowScopeTableFieldOptions.value.length > 0) {
    return `来源表字段 ${rowScopeTableFieldOptions.value.length} 个`
  }
  return '暂无字段'
}

function clearRowScopeColumns(formData) {
  if (!formData?.rowScope) {
    return
  }
  formData.rowScope.tenantColumn = null
  formData.rowScope.orgColumn = null
  formData.rowScope.userColumn = null
  formData.rowScope.regionColumn = null
  formData.rowScope.ruleItems = []
}

function ensureRowScope(formData) {
  if (!formData.rowScope) {
    formData.rowScope = createDefaultRowScope()
  }
  if (!Array.isArray(formData.rowScope.ruleItems)) {
    formData.rowScope.ruleItems = buildRowScopeRuleItems(formData.rowScope)
  }
  return formData.rowScope
}

function getRowScopeRules(formData) {
  return ensureRowScope(formData).ruleItems
}

function isRowScopeEnabled(formData) {
  return formData?.rowScope?.enabled === 1
}

function getRowScopeRemark(formData) {
  return formData?.rowScope?.remark || null
}

function handleRowScopeEnabledChange(checked, formData, updateValue) {
  const rowScope = ensureRowScope(formData)
  rowScope.enabled = checked ? 1 : 0
  if (rowScope.enabled === 1 && rowScope.ruleItems.length === 0) {
    addRowScopeRule(formData, updateValue)
  }
  syncSlotForm(updateValue)
}

function addRowScopeRule(formData, updateValue) {
  const rowScope = ensureRowScope(formData)
  const usedAttributes = new Set(rowScope.ruleItems.map(rule => rule.attribute).filter(Boolean))
  const nextAttribute = rowScopeAttributeOptions.find(option => !usedAttributes.has(option.value))?.value || null
  if (!nextAttribute && rowScope.ruleItems.length >= rowScopeAttributeOptions.length) {
    window.$message?.warning('可配置的用户属性已全部添加')
    return
  }
  if (rowScope.enabled !== 1) {
    rowScope.enabled = 1
  }
  rowScope.ruleItems.push(createRowScopeRule(nextAttribute))
  syncRowScopeColumnsFromRules(rowScope)
  syncSlotForm(updateValue)
}

function removeRowScopeRule(formData, index, updateValue) {
  const rowScope = ensureRowScope(formData)
  rowScope.ruleItems.splice(index, 1)
  syncRowScopeColumnsFromRules(rowScope)
  syncSlotForm(updateValue)
}

function handleRowScopeRuleAttributeChange(formData, rule, value, updateValue) {
  rule.attribute = value
  rule.field = null
  syncRowScopeColumnsFromRules(ensureRowScope(formData))
  syncSlotForm(updateValue)
}

function handleRowScopeRuleFieldChange(formData, rule, value, updateValue) {
  rule.field = value
  syncRowScopeColumnsFromRules(ensureRowScope(formData))
  syncSlotForm(updateValue)
}

function handleRowScopeRuleLogicChange(rule, value, updateValue) {
  rule.logic = normalizeRowScopeLogic(value)
  syncSlotForm(updateValue)
}

function handleRowScopeRemarkChange(formData, value, updateValue) {
  ensureRowScope(formData).remark = value
  syncSlotForm(updateValue)
}

function handleCacheStrategyChange(value, formData) {
  formData.cacheEnabled = value === 1 ? 1 : 0
  if (formData.cacheEnabled === 1 && !formData.cacheTtlSeconds) {
    formData.cacheTtlSeconds = 300
  }
}

function getRowScopeAttributeOptions(formData, currentRule) {
  const usedAttributes = new Set(
    getRowScopeRules(formData)
      .filter(rule => rule !== currentRule)
      .map(rule => rule.attribute)
      .filter(Boolean),
  )
  return rowScopeAttributeOptions.map(option => ({
    ...option,
    disabled: usedAttributes.has(option.value),
  }))
}

function getRowScopeConfiguredCount(formData) {
  return getRowScopeRules(formData).filter(rule => rule.attribute && trimToNull(rule.field)).length
}

function getRowScopeConditionPreview(formData) {
  const rules = getRowScopeRules(formData).filter(rule => rule.attribute && trimToNull(rule.field))
  if (rules.length === 0) {
    return '保存后将按角色数据范围动态拼接过滤条件。'
  }
  return rules.map((rule, index) => {
    const attributeLabel = rowScopeAttributeOptions.find(option => option.value === rule.attribute)?.label || '用户属性'
    const expression = `${attributeLabel} = ${rule.field}`
    if (index === rules.length - 1) {
      return expression
    }
    return `${expression} ${normalizeRowScopeLogic(rule.logic)}`
  }).join(' ')
}

function getRowScopeRuleLabel(rule) {
  const attributeLabel = rowScopeAttributeOptions.find(option => option.value === rule.attribute)?.label || '用户属性'
  return `${attributeLabel} = ${rule.field}`
}

function syncRowScopeColumnsFromRules(rowScope) {
  rowScope.tenantColumn = null
  rowScope.orgColumn = null
  rowScope.userColumn = null
  rowScope.regionColumn = null
  for (const rule of rowScope.ruleItems || []) {
    if (rowScopeAttributeOptions.some(option => option.value === rule.attribute) && trimToNull(rule.field)) {
      rowScope[rule.attribute] = trimToNull(rule.field)
    }
  }
}

function normalizeSubmitAclItems(formData) {
  formData.accessMode = formData.accessMode === 'PRIVATE' ? 'PRIVATE' : 'PUBLIC'
  if (formData.accessMode !== 'PRIVATE') {
    return []
  }

  const normalizedItems = []
  const uniqueSubjects = new Set()
  for (const [index, item] of (formData.aclItems || []).entries()) {
    const subjectType = normalizeAclSubjectType(item?.subjectType)
    const subjectId = toIdString(item?.subjectId)
    const accessLevel = normalizeAccessLevel(item?.accessLevel)
    const isEmptyRow = !item?.subjectId && !item?.accessLevel
    if (isEmptyRow) {
      continue
    }
    if (!subjectId) {
      window.$message?.error(`第${index + 1}行授权主体不能为空`)
      return null
    }
    const uniqueKey = `${subjectType}:${subjectId}`
    if (uniqueSubjects.has(uniqueKey)) {
      window.$message?.error('同一个授权主体只能配置一条权限')
      return null
    }
    uniqueSubjects.add(uniqueKey)
    normalizedItems.push({ subjectType, subjectId, accessLevel })
  }
  return normalizedItems
}

function normalizeSubmitRowScope(formData) {
  const rowScope = normalizeRowScope(formData.rowScope)
  if (rowScope.enabled !== 1) {
    return {
      enabled: 0,
      scopeMode: rowScope.scopeMode || 'SYSTEM_DATA_SCOPE',
      tenantColumn: null,
      orgColumn: null,
      userColumn: null,
      regionColumn: null,
      regionStrategy: rowScope.regionStrategy || 'SELF_AND_DESCENDANTS',
      remark: trimToNull(rowScope.remark),
    }
  }

  const rules = rowScope.ruleItems || []
  if (rules.length === 0) {
    window.$message?.error('启用数据行权限后，至少需要添加一条权限规则')
    return null
  }

  const usedAttributes = new Set()
  const normalizedColumns = {
    tenantColumn: null,
    orgColumn: null,
    userColumn: null,
    regionColumn: null,
  }
  for (const [index, rule] of rules.entries()) {
    if (!rule.attribute) {
      window.$message?.error(`第${index + 1}行用户属性不能为空`)
      return null
    }
    if (!rowScopeAttributeOptions.some(option => option.value === rule.attribute)) {
      window.$message?.error(`第${index + 1}行用户属性无效`)
      return null
    }
    if (usedAttributes.has(rule.attribute)) {
      window.$message?.error('同一个用户属性只能配置一条映射规则')
      return null
    }
    usedAttributes.add(rule.attribute)
    const field = trimToNull(rule.field)
    if (!field) {
      window.$message?.error(`第${index + 1}行表字段不能为空`)
      return null
    }
    normalizedColumns[rule.attribute] = field
  }

  if (formData.datasetType === 'SQL' && !String(formData.sqlText || '').includes('/*DATA_SCOPE*/')) {
    window.$message?.error('SQL 数据集启用行权限时，SQL 中必须包含 /*DATA_SCOPE*/ 占位符')
    return null
  }

  return {
    enabled: 1,
    scopeMode: rowScope.scopeMode || 'SYSTEM_DATA_SCOPE',
    tenantColumn: normalizedColumns.tenantColumn,
    orgColumn: normalizedColumns.orgColumn,
    userColumn: normalizedColumns.userColumn,
    regionColumn: normalizedColumns.regionColumn,
    regionStrategy: rowScope.regionStrategy || 'SELF_AND_DESCENDANTS',
    remark: trimToNull(rowScope.remark),
  }
}

function trimToNull(value) {
  if (typeof value !== 'string') {
    return value ?? null
  }
  const trimmed = value.trim()
  return trimmed || null
}

function parseParamSchemaFormValue(value) {
  if (!value) {
    return []
  }
  if (Array.isArray(value)) {
    return value
  }

  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  }
  catch (error) {
    console.error('Failed to parse dataset param schema', error)
    window.$message?.error('查询参数定义格式异常，已按空配置处理')
    return []
  }
}

function normalizeParamSchema(rows, datasetType) {
  if (!rows) {
    return []
  }
  if (!Array.isArray(rows)) {
    window.$message?.error('查询条件配置格式不正确')
    return null
  }

  const normalizedRows = []
  const paramNames = new Set()

  for (const [index, row] of rows.entries()) {
    const paramName = typeof row?.paramName === 'string' ? row.paramName.trim() : ''
    const label = typeof row?.label === 'string' ? row.label.trim() : ''
    const dataType = typeof row?.dataType === 'string' && row.dataType
      ? row.dataType.trim().toUpperCase()
      : 'STRING'
    const operator = typeof row?.operator === 'string' && row.operator
      ? row.operator.trim().toUpperCase()
      : '='
    const fieldName = typeof row?.fieldName === 'string' ? row.fieldName.trim() : ''
    const defaultValue = row?.defaultValue === '' ? null : row?.defaultValue ?? null
    const required = row?.required === true
    const isEmptyRow = !paramName && !label && !fieldName && defaultValue === null && required === false

    if (isEmptyRow) {
      continue
    }
    if (!paramName) {
      window.$message?.error(`第${index + 1}行缺少条件参数名`)
      return null
    }
    if (paramNames.has(paramName)) {
      window.$message?.error(`条件参数名重复：${paramName}`)
      return null
    }
    if (!supportedParamOperators.includes(operator)) {
      window.$message?.error(`第${index + 1}行匹配方式不支持：${operator}`)
      return null
    }
    if (datasetType === 'TABLE' && !fieldName) {
      window.$message?.error(`第${index + 1}行还未选择数据表字段`)
      return null
    }
    if (datasetType === 'SQL' && fieldName) {
      window.$message?.error(`第${index + 1}行不需要配置数据表字段`)
      return null
    }

    paramNames.add(paramName)
    normalizedRows.push({
      paramName,
      label: label || null,
      dataType,
      required,
      defaultValue,
      operator,
      fieldName: fieldName || null,
    })
  }

  return normalizedRows
}

function renderFieldInput(row, key, placeholder) {
  if (fieldConfigReadonly.value) {
    return row[key] || '-'
  }
  return h(NInput, {
    value: row[key],
    size: 'small',
    placeholder,
    onUpdateValue: value => row[key] = value,
  })
}

function renderFieldSelect(row, key, options, extraProps = {}) {
  if (fieldConfigReadonly.value) {
    const option = options.find(item => item.value === row[key])
    return option?.label || row[key] || '-'
  }

  const { onChange, ...selectProps } = extraProps
  return h(NSelect, {
    value: row[key] ?? null,
    options,
    size: 'small',
    clearable: false,
    ...selectProps,
    onUpdateValue: (value) => {
      row[key] = value
      onChange?.(value)
    },
  })
}

function renderMaskRuleSelect(row) {
  if (fieldConfigReadonly.value) {
    if (!row.maskRule) {
      return '默认：保留前2后2'
    }
    const option = maskRuleOptions.find(item => item.value === row.maskRule)
    return option?.label || row.maskRule
  }

  return h(NSelect, {
    value: row.maskRule || '__DEFAULT__',
    options: maskRuleOptions,
    size: 'small',
    filterable: true,
    tag: true,
    placeholder: '选择脱敏规则',
    onUpdateValue: (value) => {
      row.maskRule = value === '__DEFAULT__' ? null : value
    },
  })
}

function normalizeSortInput(value) {
  const parsed = Number.parseInt(value, 10)
  if (Number.isNaN(parsed) || parsed < 0) {
    return 0
  }
  return parsed
}

async function handlePreviewSql(formData, openModal = true) {
  if (!formData.connectionId) {
    window.$message?.error('请选择数据连接')
    return
  }
  if (!formData.sqlText) {
    window.$message?.error('请输入查询SQL')
    return
  }

  sqlPreviewVisible.value = openModal
  sqlPreviewLoading.value = true
  sqlPreviewColumns.value = []
  sqlPreviewRows.value = []
  sqlPreviewScrollX.value = 0

  try {
    const res = await request.post('/data/dataset/preview-sql', {
      connectionId: formData.connectionId,
      sqlText: formData.sqlText,
      maxRows: 5,
    })
    if (res.code === 200) {
      const columns = res.data?.columns || []
      sqlPreviewColumns.value = columns.map(column => ({
        title: column,
        key: column,
        width: 160,
        ellipsis: { tooltip: true },
        render: row => row[column] ?? '',
      }))
      sqlPreviewRows.value = res.data?.rows || []
      sqlPreviewScrollX.value = Math.max(columns.length * 160, 800)
      window.$message?.success(`SQL校验通过，预览 ${sqlPreviewRows.value.length} 条数据`)
    }
    else {
      window.$message?.error(res.msg || 'SQL预览失败')
    }
  }
  catch (error) {
    window.$message?.error(error?.message || 'SQL预览失败')
  }
  finally {
    sqlPreviewLoading.value = false
  }
}

async function handleViewFields(row) {
  currentFieldDataset.value = row
  fieldModalTitle.value = `字段配置 - ${row.datasetName}`
  fieldModalVisible.value = true
  fieldLoading.value = true
  fieldRows.value = []

  try {
    await loadDimensionOptions()
    const res = await getDataDatasetById(row.id)
    if (res.code === 200) {
      fieldRows.value = normalizeFieldRows(res.data?.fields || [])
    }
    else {
      window.$message?.error(res.msg || '加载字段失败')
    }
  }
  catch (error) {
    console.error('Failed to load dataset fields', error)
    window.$message?.error('加载字段失败')
  }
  finally {
    fieldLoading.value = false
  }
}

function normalizeFieldRows(rows) {
  return (rows || []).map((row, index) => ({
    ...row,
    fieldLabel: row.fieldLabel || row.fieldName,
    dataType: row.dataType || 'STRING',
    fieldRole: row.fieldRole || 'DIMENSION',
    queryEnabled: row.queryEnabled ?? 1,
    displayEnabled: row.displayEnabled ?? 1,
    sensitiveLevel: row.sensitiveLevel || 'NONE',
    sort: row.sort ?? index,
  }))
}

function validateFieldRows(rows) {
  const fieldNames = new Set()
  for (const [index, row] of rows.entries()) {
    const fieldName = typeof row.fieldName === 'string' ? row.fieldName.trim() : ''
    const fieldLabel = typeof row.fieldLabel === 'string' ? row.fieldLabel.trim() : ''
    if (!fieldName) {
      window.$message?.error(`第${index + 1}行缺少字段名`)
      return null
    }
    if (fieldNames.has(fieldName)) {
      window.$message?.error(`字段名重复：${fieldName}`)
      return null
    }
    if (!fieldLabel) {
      window.$message?.error(`字段 ${fieldName} 缺少显示名称`)
      return null
    }
    fieldNames.add(fieldName)
  }

  return rows.map((row, index) => {
    const dataType = row.dataType || 'STRING'
    const fieldRole = row.fieldRole || 'DIMENSION'
    const sensitiveLevel = row.sensitiveLevel || 'NONE'
    const maskRule = row.maskRule === '__DEFAULT__' ? null : row.maskRule
    return {
      ...row,
      fieldName: row.fieldName.trim(),
      fieldLabel: row.fieldLabel.trim(),
      dataType,
      fieldRole,
      queryEnabled: row.queryEnabled ?? 1,
      displayEnabled: row.displayEnabled ?? 1,
      sensitiveLevel,
      dateFormat: ['DATE', 'DATETIME'].includes(dataType) ? row.dateFormat || null : null,
      dataUnit: row.dataUnit || null,
      dimensionId: fieldRole === 'DIMENSION' ? row.dimensionId || null : null,
      maskRule: sensitiveLevel === 'MASK' ? maskRule || null : null,
      sort: row.sort ?? index,
      description: row.description || null,
    }
  })
}

async function handleSaveFieldConfig() {
  if (!currentFieldDataset.value?.id) {
    return
  }
  if (fieldConfigReadonly.value) {
    window.$message?.warning('已发布数据集不可修改字段配置')
    return
  }

  const normalizedRows = validateFieldRows(fieldRows.value)
  if (!normalizedRows) {
    return
  }

  fieldSaving.value = true
  try {
    const res = await saveDataDatasetFields(currentFieldDataset.value.id, normalizedRows)
    if (res.code === 200) {
      window.$message?.success('字段配置已保存')
      fieldRows.value = normalizeFieldRows(normalizedRows)
    }
    else {
      window.$message?.error(res.msg || '保存字段配置失败')
    }
  }
  catch (error) {
    window.$message?.error(error?.message || '保存字段配置失败')
  }
  finally {
    fieldSaving.value = false
  }
}

async function handleSyncCurrentFields() {
  if (currentFieldDataset.value) {
    confirmSyncDatasetFields(currentFieldDataset.value, true)
  }
}

function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除数据集“${row.datasetName}”吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await deleteDataDataset(row.id)
        if (res.code === 200) {
          window.$message?.success('删除成功')
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message?.error(error?.message || '删除失败')
      }
    },
  })
}

async function handleSyncFields(row) {
  confirmSyncDatasetFields(row, true)
}

function confirmSyncDatasetFields(row, openModal = false) {
  window.$dialog.warning({
    title: '确认同步字段',
    content: `同步会重新读取数据源字段，并覆盖数据集“${row.datasetName}”当前字段配置，包括显示名称、字段角色、维度绑定、脱敏规则和排序。确认继续吗？`,
    positiveText: '确认同步',
    negativeText: '取消',
    onPositiveClick: () => syncDatasetFields(row, openModal),
  })
}

async function syncDatasetFields(row, openModal = false) {
  if (openModal) {
    currentFieldDataset.value = row
    fieldModalTitle.value = `字段配置 - ${row.datasetName}`
    fieldModalVisible.value = true
    fieldLoading.value = true
  }
  try {
    window.$message?.loading('正在同步字段...', { duration: 0, key: 'syncFields' })
    await loadDimensionOptions()
    const res = await syncDataDatasetFields(row.id)
    if (res.code === 200) {
      window.$message?.success(`同步成功，共 ${res.data?.length || 0} 个字段`, { key: 'syncFields' })
      fieldRows.value = normalizeFieldRows(res.data || [])
    }
    else {
      window.$message?.error(res.msg || '同步失败', { key: 'syncFields' })
    }
  }
  catch (error) {
    window.$message?.error(error?.message || '同步字段失败', { key: 'syncFields' })
  }
  finally {
    fieldLoading.value = false
  }
}

function handlePublishDataset(row) {
  window.$dialog.warning({
    title: '确认发布',
    content: `发布后数据集“${row.datasetName}”将进入只读状态，仅可查看和下架，确认继续吗？`,
    positiveText: '发布',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await publishDataDataset(row.id)
        if (res.code === 200) {
          window.$message?.success('发布成功')
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message?.error(error?.message || '发布失败')
      }
    },
  })
}

async function loadDatasetImpact(row) {
  try {
    const res = await getDashboardDatasetImpact(row.id, datasetImpactLimit)
    if (res.code === 200) {
      return res.data || []
    }
    window.$message?.warning(res.msg || '数据集影响分析查询失败')
  }
  catch (error) {
    window.$message?.warning(error?.message || '数据集影响分析查询失败')
  }
  return null
}

function renderDatasetImpactContent(row, impacts) {
  if (impacts === null) {
    return `下架后数据集“${row.datasetName}”将暂停供下游使用。当前影响分析查询失败，请确认是否继续下架。`
  }
  if (!impacts.length) {
    return `下架后数据集“${row.datasetName}”将暂停供下游使用。当前未发现 AI 大屏组件血缘影响，确认继续吗？`
  }

  const visibleItems = impacts.slice(0, datasetImpactVisibleLimit)
  return h('div', { class: 'dataset-impact-dialog' }, [
    h('p', null, `下架后数据集“${row.datasetName}”将暂停供下游使用。`),
    h('div', { class: 'dataset-impact-dialog__summary' }, `检测到最近 ${impacts.length} 个 AI 大屏组件使用了该数据集：`),
    h('ul', { class: 'dataset-impact-dialog__list' }, visibleItems.map(item => h('li', { key: item.lineageId || `${item.recordId}-${item.componentIndex}` }, [
      h('strong', null, item.projectName || item.generatedTitle || '未命名大屏'),
      h('span', null, ` / ${item.componentTitle || item.componentKey || `组件${item.componentIndex ?? ''}`}`),
      item.businessName ? h('small', null, `业务：${item.businessName}`) : null,
      item.fieldNames ? h('small', null, `字段：${item.fieldNames}`) : null,
    ]))),
    impacts.length > visibleItems.length
      ? h('div', { class: 'dataset-impact-dialog__more' }, `仅展示前 ${visibleItems.length} 个，请到影响分析接口查看完整结果。`)
      : null,
  ])
}

async function handleOfflineDataset(row) {
  const impacts = await loadDatasetImpact(row)
  window.$dialog.warning({
    title: '确认下架',
    content: () => renderDatasetImpactContent(row, impacts),
    positiveText: '下架',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await offlineDataDataset(row.id)
        if (res.code === 200) {
          window.$message?.success('下架成功')
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message?.error(error?.message || '下架失败')
      }
    },
  })
}

function goToCategoryManage() {
  router.push('/data/dataset-category')
}

function handleStepReset() {
  currentStep.value = 1
}

function canGoToNextStep(formData) {
  if (isFormReadOnly.value) {
    return true
  }

  if (currentStep.value === 1) {
    if (!formData.datasetCode) {
      window.$message?.warning('请输入数据集编码')
      return false
    }
    if (!formData.datasetName) {
      window.$message?.warning('请输入数据集名称')
      return false
    }
    if (!formData.connectionId) {
      window.$message?.warning('请选择数据连接')
      return false
    }
    if (formData.datasetType === 'TABLE' && !formData.tableName) {
      window.$message?.warning('请选择数据表')
      return false
    }
    if (formData.datasetType === 'SQL' && !formData.sqlText) {
      window.$message?.warning('请输入查询SQL')
      return false
    }
  }
  return true
}

function goToNextStep(formData) {
  if (!canGoToNextStep(formData)) {
    return
  }
  if (currentStep.value < totalSteps) {
    currentStep.value++
    if (currentStep.value === totalSteps) {
      loadPermissionOptions()
      loadRowScopeTableFields(formData)
    }
  }
}

function goToPrevStep() {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}
</script>

<style scoped>
.dataset-studio {
  background: #f8fafc;
  min-height: 100%;
  padding: 10px;
}

.studio-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(460px, 0.9fr);
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  padding: 12px 16px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.workspace-sidebar,
.workspace-main {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 12px;
  background: #fff;
}

.hero-main {
  min-width: 0;
}

.hero-kicker,
.panel-kicker {
  margin: 0 0 4px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0;
  text-transform: none;
}

.hero-title {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.2;
  font-weight: 600;
}

.hero-description {
  overflow: hidden;
  max-width: 720px;
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.hero-stat-card {
  min-width: 0;
  padding: 8px 10px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 8px;
  background: #fff;
}

.hero-stat-label {
  overflow: hidden;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-stat-value {
  margin-top: 2px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 600;
  line-height: 1;
  font-family: 'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.studio-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(460px, 0.9fr);
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  padding: 10px 14px;
  overflow: hidden;
  border: 1px solid var(--panel-border);
  border-radius: var(--panel-radius);
  background:
    radial-gradient(circle at top left, rgb(59 130 246 / 14%), transparent 36%),
    radial-gradient(circle at 90% 25%, rgb(14 165 233 / 12%), transparent 24%),
    linear-gradient(135deg, rgb(255 255 255 / 96%), rgb(248 251 255 / 94%));
  box-shadow: var(--panel-shadow);
  backdrop-filter: blur(14px);
}

.workspace-sidebar,
.workspace-main {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--panel-border);
  border-radius: var(--panel-radius);
  background: var(--panel-bg);
  box-shadow: var(--panel-shadow);
  backdrop-filter: blur(14px);
}

.hero-main {
  min-width: 0;
}

.hero-kicker,
.panel-kicker {
  margin: 0 0 3px;
  color: #0f766e;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.hero-title {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.18;
}

.hero-description {
  overflow: hidden;
  max-width: 720px;
  margin: 4px 0 0;
  color: #475569;
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.hero-stat-card {
  min-width: 0;
  padding: 8px 10px;
  border: 1px solid rgb(148 163 184 / 14%);
  border-radius: 10px;
  background: rgb(255 255 255 / 78%);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 80%);
}

.hero-stat-card::after {
  display: none;
}

.hero-stat-label {
  overflow: hidden;
  color: #64748b;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.03em;
  text-overflow: ellipsis;
  text-transform: uppercase;
  white-space: nowrap;
}

.hero-stat-value {
  margin-top: 2px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
}

.hero-stat-note {
  display: none;
}

.dataset-workspace {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
}

.workspace-sidebar,
.workspace-main {
  padding: 10px;
}

.sidebar-head,
.toolbar-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.sidebar-head h3,
.toolbar-title-row h3 {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
  font-weight: 600;
}

.sidebar-shortcuts {
  display: flex;
  gap: 6px;
  margin: 8px 0 8px;
}

.scope-chip {
  padding: 5px 10px;
  color: #334155;
  font-size: 11px;
  font-weight: 600;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    color 0.2s ease,
    background 0.2s ease,
    transform 0.2s ease;
}

.scope-chip.active,
.scope-chip:hover {
  color: #0f766e;
  background: #ecfeff;
  border-color: #99f6e4;
  transform: translateY(-1px);
}

.category-search {
  margin-bottom: 8px;
}

.category-tree-shell {
  min-height: 180px;
  max-height: calc(100vh - 390px);
  padding: 6px;
  overflow: auto;
  background: rgb(248 250 252 / 60%);
  border: 1px solid #e8ecf1;
  border-radius: 10px;
}

.category-detail-card {
  margin-top: 8px;
  padding: 10px 12px;
  background: linear-gradient(135deg, #1e293b, #0f172a);
  border-radius: 10px;
  box-shadow: 0 4px 12px rgb(15 23 42 / 20%);
}

.category-detail-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.category-detail-name {
  color: #f1f5f9;
  font-size: 15px;
  font-weight: 600;
}

.category-detail-code {
  margin-top: 3px;
  color: rgb(148 163 184 / 90%);
  font-size: 11px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.category-detail-desc {
  margin: 8px 0 0;
  color: rgb(148 163 184 / 80%);
  font-size: 12px;
  line-height: 1.6;
}

.main-toolbar {
  margin-bottom: 8px;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #e8ecf1;
  border-radius: 10px;
}

.toolbar-title-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.toolbar-scope {
  padding: 3px 8px;
  color: #1e40af;
  font-size: 11px;
  font-weight: 600;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
}

.toolbar-filters {
  display: grid;
  grid-template-columns: minmax(200px, 1.4fr) repeat(3, minmax(130px, 0.8fr)) auto;
  gap: 6px;
  margin-top: 8px;
}

.toolbar-filter {
  min-width: 0;
}

.toolbar-filter--keyword {
  width: 100%;
}

.asset-name-card {
  display: grid;
  gap: 4px;
}

.asset-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.asset-name {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}

.asset-code {
  color: #64748b;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.02em;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.asset-desc {
  overflow: hidden;
  color: #94a3b8;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-category-name,
.asset-source-name {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}

.asset-category-code,
.asset-source-detail {
  margin-top: 3px;
  color: #94a3b8;
  font-size: 11px;
}

.sql-preview-action {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 34px;
  padding: 2px 0 4px;
}

.field-config-modal {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field-config-head {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(420px, 0.85fr);
  gap: 16px;
  align-items: stretch;
  padding: 18px;
  border: 1px solid #dbe3ef;
  border-radius: 20px;
  background:
    radial-gradient(circle at 8% 0%, rgb(20 184 166 / 14%), transparent 30%),
    linear-gradient(135deg, #fff 0%, #f8fafc 100%);
}

.field-config-title {
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
}

.field-config-desc {
  max-width: 720px;
  margin-top: 8px;
  color: #475569;
  line-height: 1.75;
}

.field-config-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.field-config-stat {
  padding: 12px 14px;
  border: 1px solid rgb(148 163 184 / 18%);
  border-radius: 16px;
  background: rgb(255 255 255 / 80%);
}

.field-config-stat span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.field-config-stat strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 22px;
  font-weight: 800;
}

.field-config-alert {
  border-radius: 14px;
}

.field-config-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.field-config-table {
  width: 100%;
}

:deep(.field-config-table .n-data-table-base-table-body) {
  overflow-x: auto !important;
}

:deep(.field-config-table .n-data-table-td) {
  vertical-align: top;
}

.field-name-cell {
  display: block;
  overflow: hidden;
  color: #0f172a;
  font-weight: 700;
  line-height: 1.45;
  min-width: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-muted-text {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
}

.field-sort-value {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  height: 26px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #0f172a;
  font-weight: 800;
}

.field-sort-native-input {
  width: 112px;
  height: 30px;
  box-sizing: border-box;
  padding: 0 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
  outline: none;
}

.field-sort-native-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgb(37 99 235 / 12%);
}

:deep(.dataset-crud .ai-crud-main) {
  background: transparent;
}

:deep(.dataset-crud .ai-crud-table) {
  overflow: hidden;
  border: 1px solid #e8ecf1;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 4px 16px rgb(15 23 42 / 3%);
}

:deep(.dataset-crud .n-data-table-th) {
  padding: 9px 12px;
  color: #64748b;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  background: #f8fafc;
  border-bottom: 1px solid #e8ecf1;
}

:deep(.dataset-crud .n-data-table-tr:nth-child(even) td) {
  background: rgb(248 250 252 / 50%);
}

:deep(.dataset-crud .n-data-table-tr:hover td) {
  background: rgb(241 245 249 / 80%);
}

:deep(.n-tree .n-tree-node-content) {
  min-height: 34px;
  padding-left: 15px;
}

:global(.data-dataset-edit-form) {
  padding: 4px 2px 0;
}

:global(.data-dataset-edit-form .n-form-item:has(.step-shell)),
:global(.data-dataset-edit-form .n-form-item:has(.dataset-editor-page)),
:global(.data-dataset-edit-form .n-form-item:has(.dataset-context-panel)),
:global(.data-dataset-edit-form .n-form-item:has(.permission-panel)),
:global(.data-dataset-edit-form .n-form-item:has(.dataset-param-editor)),
:global(.data-dataset-edit-form .n-form-item:has(.step-navigation-wrapper)),
:global(.data-dataset-edit-form .n-form-item:has(.sql-preview-action)),
:global(.data-dataset-edit-form .n-form-item:has(.dataset-form-divider)),
:global(.data-dataset-edit-form .n-form-item:has(.n-alert)) {
  padding: 0;
  background: transparent;
  border: 0;
  box-shadow: none;
}

:global(.data-dataset-edit-form .n-form-item:has(.step-shell)) {
  grid-column: 1 / -1 !important;
  width: 100% !important;
  max-width: none !important;
}

:global(.data-dataset-edit-form .n-form-item:has(.dataset-editor-page)) {
  grid-column: 1 / -1 !important;
  width: 100% !important;
  max-width: none !important;
}

:global(.data-dataset-edit-form .n-form-item:has(.step-shell) .n-form-item-blank) {
  width: 100% !important;
  max-width: none !important;
}

:global(.data-dataset-edit-form .n-form-item:has(.dataset-editor-page) .n-form-item-blank) {
  width: 100% !important;
  max-width: none !important;
}

:global(.data-dataset-edit-form .n-form-item) {
  margin-bottom: 8px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: linear-gradient(180deg, #fff 0%, #fcfdff 100%);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

:global(.data-dataset-edit-form .n-form-item:hover) {
  border-color: #cbd5e1;
  box-shadow: 0 6px 16px rgb(15 23 42 / 5%);
  transform: translateY(-1px);
}

:global(.data-dataset-edit-form .n-form-item:has(.permission-panel)) {
  overflow: visible;
  transform: none !important;
}

:global(.data-dataset-edit-form .n-form-item:has(.permission-panel):hover) {
  transform: none !important;
}

:global(.data-dataset-edit-form .n-form-item:has(.dataset-editor-page):hover) {
  border: 0;
  box-shadow: none;
  transform: none;
}

:global(.data-dataset-edit-form .n-form-item-blank) {
  width: 100%;
  overflow: visible;
}

:global(.data-dataset-edit-form .n-form-item-label) {
  min-height: 18px;
  margin-bottom: 6px;
  color: #334155;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.01em;
  line-height: 1.3;
}

:global(.data-dataset-edit-form .dataset-form-divider) {
  margin: 10px 0 6px;
  color: #64748b;
}

:global(.data-dataset-edit-form .dataset-form-divider::before),
:global(.data-dataset-edit-form .dataset-form-divider::after) {
  border-top-color: #dbe3ef;
}

:global(.data-dataset-edit-form .dataset-form-divider .n-divider__title) {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

:global(.data-dataset-edit-form .n-input),
:global(.data-dataset-edit-form .n-input-number),
:global(.data-dataset-edit-form .n-select),
:global(.data-dataset-edit-form .n-tree-select) {
  width: 100%;
}

:global(.data-dataset-edit-form .sql-editor) {
  width: 100%;
}

:global(.data-dataset-edit-form .sql-preview-action) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

:global(.data-dataset-edit-form .n-radio-group .n-space) {
  gap: 8px 18px !important;
}

:global(.data-dataset-edit-form .dataset-context-panel) {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.95fr);
  gap: 12px;
  padding: 12px 14px;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  border: 1px solid #dbe3ef;
  border-radius: 14px;
}

:global(.data-dataset-edit-form .dataset-context-panel--muted) {
  background: linear-gradient(180deg, #fcfdff 0%, #f8fafc 100%);
}

:global(.data-dataset-edit-form .dataset-context-panel--compact) {
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 1fr);
}

:global(.data-dataset-edit-form .context-panel__main) {
  min-width: 0;
}

:global(.data-dataset-edit-form .context-panel__eyebrow) {
  color: #475569;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

:global(.data-dataset-edit-form .context-panel__title) {
  margin-top: 6px;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.35;
}

:global(.data-dataset-edit-form .context-panel__desc) {
  margin-top: 6px;
  color: #475569;
  font-size: 12px;
  line-height: 1.7;
}

:global(.data-dataset-edit-form .context-panel__facts) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  align-content: start;
}

:global(.data-dataset-edit-form .context-fact) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

:global(.data-dataset-edit-form .context-fact--wide) {
  grid-column: 1 / -1;
}

:global(.data-dataset-edit-form .context-fact span) {
  color: #64748b;
  font-size: 11px;
}

:global(.data-dataset-edit-form .context-fact strong) {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
}

:global(.data-dataset-edit-form .context-panel__footnote) {
  grid-column: 1 / -1;
  color: #475569;
  font-size: 12px;
  line-height: 1.8;
  padding-top: 2px;
}

:global(.data-dataset-edit-form .permission-panel) {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 14px;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  border: 1px solid #dbe3ef;
  border-radius: 14px;
  overflow: visible;
}

:global(.data-dataset-edit-form .permission-panel__header) {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

:global(.data-dataset-edit-form .permission-panel__title) {
  margin-top: 8px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.35;
}

:global(.data-dataset-edit-form .permission-panel__desc) {
  max-width: 760px;
  margin-top: 8px;
  color: #475569;
  font-size: 13px;
  line-height: 1.8;
}

:global(.data-dataset-edit-form .permission-facts) {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

:global(.data-dataset-edit-form .permission-fact) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

:global(.data-dataset-edit-form .permission-fact span) {
  color: #64748b;
  font-size: 11px;
}

:global(.data-dataset-edit-form .permission-fact strong) {
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.data-dataset-edit-form .acl-editor) {
  padding: 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: visible;
}

:global(.data-dataset-edit-form .acl-editor__toolbar) {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

:global(.data-dataset-edit-form .acl-editor__title) {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

:global(.data-dataset-edit-form .acl-editor__hint) {
  margin-top: 3px;
  color: #64748b;
  font-size: 11px;
  line-height: 1.5;
}

:global(.data-dataset-edit-form .acl-rows) {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: visible;
}

:global(.data-dataset-edit-form .acl-row) {
  display: grid;
  grid-template-columns: minmax(120px, 0.55fr) minmax(260px, 1.4fr) minmax(150px, 0.7fr) 42px;
  gap: 10px;
  align-items: center;
  overflow: visible;
  position: relative;
  z-index: 2;
}

:global(.data-dataset-edit-form .acl-row:focus-within) {
  z-index: 20;
}

:global(.data-dataset-edit-form .acl-row .v-binder-follower-container) {
  z-index: 4200 !important;
}

:global(.data-dataset-edit-form .row-scope-alert) {
  border-radius: 14px;
}

:global(.data-dataset-edit-form .row-permission-add) {
  min-width: 92px;
  border-radius: 12px;
  background: #0f9f8f;
}

:global(.data-dataset-edit-form .row-permission-switch) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

:global(.data-dataset-edit-form .row-permission-switch span) {
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

:global(.data-dataset-edit-form .row-scope-rule-builder) {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 14px 36px rgb(15 23 42 / 6%);
  transition: opacity 0.18s ease;
}

:global(.data-dataset-edit-form .row-scope-rule-builder.is-disabled) {
  opacity: 0.68;
}

:global(.data-dataset-edit-form .row-scope-rule-titlebar) {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

:global(.data-dataset-edit-form .row-scope-rule-title) {
  color: #0f172a;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.4;
}

:global(.data-dataset-edit-form .row-scope-rule-desc) {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
}

:global(.data-dataset-edit-form .row-scope-attribute-strip) {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

:global(.data-dataset-edit-form .row-scope-attribute-chip) {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

:global(.data-dataset-edit-form .row-scope-attribute-chip span) {
  color: #111827;
  font-size: 13px;
  font-weight: 700;
}

:global(.data-dataset-edit-form .row-scope-attribute-chip small) {
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.data-dataset-edit-form .row-scope-rule-header),
:global(.data-dataset-edit-form .row-scope-rule-row) {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) 28px minmax(220px, 1.35fr) minmax(110px, 0.55fr) 42px;
  gap: 10px;
  align-items: center;
}

:global(.data-dataset-edit-form .row-scope-rule-header) {
  padding: 0 10px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

:global(.data-dataset-edit-form .row-scope-rule-row) {
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

:global(.data-dataset-edit-form .row-scope-rule-row .n-base-selection) {
  border-radius: 12px;
  --n-color: #f1f5f9;
  --n-color-disabled: #f1f5f9;
}

:global(.data-dataset-edit-form .row-scope-equals) {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: #111827;
  font-size: 16px;
  font-weight: 800;
}

:global(.data-dataset-edit-form .row-scope-delete) {
  color: #64748b;
}

:global(.data-dataset-edit-form .row-scope-delete:hover) {
  color: #dc2626;
  background: #fee2e2;
}

:global(.data-dataset-edit-form .dataset-editor-page) {
  --editor-blue: #1677ff;
  --editor-blue-soft: #e8f3ff;
  --editor-bg: #f7f8fa;
  --editor-border: #dcdfe6;
  --editor-border-light: #e5e6eb;
  --editor-text: #1d2129;
  --editor-muted: #86909c;
  --editor-disabled: #c9cdd4;
  --editor-error: #f53f3f;
  --editor-success: #00b42a;
  min-height: min(78vh, 920px);
  padding: 16px;
  color: var(--editor-text);
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: var(--editor-bg);
  border-radius: 4px;
}

:global(.data-dataset-edit-form .dataset-editor-page *),
:global(.data-dataset-edit-form .dataset-editor-page *::before),
:global(.data-dataset-edit-form .dataset-editor-page *::after) {
  box-sizing: border-box;
}

:global(.data-dataset-edit-form .dataset-editor-page button) {
  cursor: pointer;
}

:global(.data-dataset-edit-form .dataset-editor-page button:disabled) {
  cursor: default;
}

:global(.data-dataset-edit-form .dataset-editor-page .n-button) {
  min-height: 36px;
  padding: 0 24px;
  color: var(--editor-text);
  font-size: 14px;
  font-weight: 500;
  border-radius: 4px;
  --n-border-radius: 4px !important;
  --n-border: 1px solid #dcdfe6 !important;
  --n-border-hover: 1px solid #86909c !important;
  --n-border-focus: 1px solid #1677ff !important;
}

:global(.data-dataset-edit-form .dataset-editor-page .n-button--primary-type) {
  color: #fff;
  --n-color: #1677ff !important;
  --n-color-hover: #4096ff !important;
  --n-color-pressed: #0958d9 !important;
  --n-border: 1px solid #1677ff !important;
  --n-border-hover: 1px solid #4096ff !important;
}

:global(.data-dataset-edit-form .dataset-editor-page .n-input),
:global(.data-dataset-edit-form .dataset-editor-page .n-input-number),
:global(.data-dataset-edit-form .dataset-editor-page .n-base-selection) {
  --n-border-radius: 4px !important;
  --n-border: 1px solid #dcdfe6 !important;
  --n-border-hover: 1px solid #86909c !important;
  --n-border-focus: 1px solid #1677ff !important;
  --n-box-shadow-focus: 0 0 0 2px rgb(22 119 255 / 20%) !important;
  --n-placeholder-color: #c9cdd4 !important;
  --n-text-color: #1d2129 !important;
}

:global(.data-dataset-edit-form .dataset-editor-page .n-radio-button) {
  --n-button-border-radius: 4px !important;
  --n-button-color-active: #1677ff !important;
  --n-button-text-color-active: #fff !important;
  --n-button-border-color-active: #1677ff !important;
}

:global(.data-dataset-edit-form .dataset-editor-page .n-switch.n-switch--active) {
  --n-rail-color-active: #1677ff !important;
}

:global(.data-dataset-edit-form .dataset-editor-page .n-slider) {
  --n-fill-color: #1677ff !important;
  --n-fill-color-hover: #1677ff !important;
  --n-handle-color: #1677ff !important;
}

:global(.data-dataset-edit-form .dataset-editor-header) {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid var(--editor-border);
  border-radius: 4px;
  background: #fff;
}

:global(.data-dataset-edit-form .dataset-editor-heading) {
  min-width: 0;
}

:global(.data-dataset-edit-form .dataset-editor-breadcrumb) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 10px;
  padding: 0;
  color: #4e5969;
  font-size: 14px;
  line-height: 1.4;
  border: 0;
  background: transparent;
}

:global(.data-dataset-edit-form .dataset-editor-breadcrumb:hover span:first-child) {
  color: var(--editor-blue);
}

:global(.data-dataset-edit-form .dataset-editor-breadcrumb i) {
  color: var(--editor-disabled);
  font-style: normal;
}

:global(.data-dataset-edit-form .dataset-editor-title-row) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

:global(.data-dataset-edit-form .dataset-editor-title-row h2) {
  max-width: 520px;
  margin: 0;
  overflow: hidden;
  color: var(--editor-text);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.data-dataset-edit-form .dataset-status-tag) {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 2px 8px;
  color: var(--editor-blue);
  font-size: 12px;
  line-height: 1;
  border-radius: 4px;
  background: var(--editor-blue-soft);
}

:global(.data-dataset-edit-form .dataset-editor-meta) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
  color: var(--editor-muted);
  font-size: 12px;
}

:global(.data-dataset-edit-form .dataset-editor-meta span) {
  color: var(--editor-disabled);
}

:global(.data-dataset-edit-form .dataset-editor-actions) {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

:global(.data-dataset-edit-form .dataset-editor-actions .n-button) {
  min-width: 88px;
}

:global(.data-dataset-edit-form .dataset-editor-steps) {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  padding: 16px 20px;
  margin-bottom: 16px;
  border: 1px solid var(--editor-border);
  border-radius: 4px;
  background: #fff;
}

:global(.data-dataset-edit-form .dataset-editor-step) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 24px;
  align-items: start;
  gap: 10px;
  min-width: 0;
  min-height: 52px;
  padding: 0 14px 0 0;
  color: var(--editor-muted);
  text-align: left;
  border: 0;
  background: transparent;
  transition: color 0.18s ease;
}

:global(.data-dataset-edit-form .dataset-editor-step:last-child) {
  grid-template-columns: minmax(0, 1fr);
  padding-right: 0;
}

:global(.data-dataset-edit-form .dataset-editor-step:hover),
:global(.data-dataset-edit-form .dataset-editor-step.is-active),
:global(.data-dataset-edit-form .dataset-editor-step.is-completed) {
  color: var(--editor-text);
}

:global(.data-dataset-edit-form .dataset-editor-step__content) {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

:global(.data-dataset-edit-form .dataset-editor-step__title) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  color: inherit;
  line-height: 1.4;
}

:global(.data-dataset-edit-form .dataset-editor-step__index) {
  flex: 0 0 auto;
  color: inherit;
  font-size: 14px;
  font-weight: 500;
}

:global(.data-dataset-edit-form .dataset-editor-step__label) {
  min-width: 0;
  overflow: hidden;
  color: inherit;
  font-size: 14px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.data-dataset-edit-form .dataset-editor-step__caption) {
  min-width: 0;
  overflow: hidden;
  color: var(--editor-muted);
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.data-dataset-edit-form .dataset-editor-step.is-active .dataset-editor-step__index),
:global(.data-dataset-edit-form .dataset-editor-step.is-active .dataset-editor-step__label),
:global(.data-dataset-edit-form .dataset-editor-step.is-completed .dataset-editor-step__index),
:global(.data-dataset-edit-form .dataset-editor-step.is-completed .dataset-editor-step__label) {
  color: var(--editor-blue);
}

:global(.data-dataset-edit-form .dataset-editor-step.is-active .dataset-editor-step__caption) {
  color: #4e5969;
}

:global(.data-dataset-edit-form .dataset-editor-step__separator) {
  padding-top: 1px;
  color: var(--editor-disabled);
  font-size: 18px;
  line-height: 1;
}

:global(.data-dataset-edit-form .dataset-editor-grid) {
  display: grid;
  grid-template-columns: minmax(360px, 0.72fr) minmax(560px, 1.28fr);
  gap: 16px;
  align-items: stretch;
}

:global(.data-dataset-edit-form .dataset-edit-panel) {
  min-width: 0;
  height: 100%;
  padding: 16px;
  border: 1px solid var(--editor-border);
  border-radius: 4px;
  background: #fff;
}

:global(.data-dataset-edit-form .panel-section-head) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 24px;
  margin-bottom: 16px;
}

:global(.data-dataset-edit-form .panel-section-head h3) {
  margin: 0;
  color: var(--editor-text);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
}

:global(.data-dataset-edit-form .panel-inline-indicator) {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 8px;
  color: var(--editor-blue);
  font-size: 12px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: var(--editor-blue-soft);
}

:global(.data-dataset-edit-form .panel-inline-indicator--button) {
  cursor: pointer;
}

:global(.data-dataset-edit-form .panel-inline-indicator--button:hover) {
  border-color: rgb(22 119 255 / 28%);
}

:global(.param-preview-popover) {
  min-width: 0;
}

:global(.param-preview-list) {
  display: grid;
  gap: 8px;
  max-height: 260px;
  overflow: auto;
}

:global(.param-preview-row) {
  display: grid;
  gap: 3px;
  min-width: 0;
  padding: 8px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #f7f8fa;
}

:global(.param-preview-row strong) {
  overflow: hidden;
  color: #1d2129;
  font-family: 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.param-preview-row span) {
  overflow-wrap: anywhere;
  color: #86909c;
  font-size: 12px;
  line-height: 1.5;
}

:global(.data-dataset-edit-form .dataset-form-grid),
:global(.data-dataset-edit-form .execution-settings) {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 14px 12px;
}

:global(.data-dataset-edit-form .dataset-field) {
  display: flex;
  grid-column: span 6;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

:global(.data-dataset-edit-form .setting-switch-row) {
  display: flex;
  grid-column: span 6;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 34px;
}

:global(.data-dataset-edit-form .dataset-field--wide),
:global(.data-dataset-edit-form .dataset-field--full) {
  grid-column: 1 / -1;
}

:global(.data-dataset-edit-form .dataset-field > span) {
  color: var(--editor-text);
  font-size: 14px;
  font-weight: 500;
}

:global(.data-dataset-edit-form .setting-switch-row > span) {
  color: var(--editor-text);
  font-size: 14px;
  font-weight: 500;
}

:global(.data-dataset-edit-form .dataset-field--required > span::before) {
  margin-right: 4px;
  color: var(--editor-error);
  content: '*';
}

:global(.data-dataset-edit-form .dataset-tag-list),
:global(.data-dataset-edit-form .acl-tag-list) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
}

:global(.data-dataset-edit-form .dataset-soft-tag),
:global(.data-dataset-edit-form .acl-tag) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 26px;
  padding: 0 8px;
  color: var(--editor-blue);
  font-size: 12px;
  border-radius: 4px;
  background: var(--editor-blue-soft);
}

:global(.data-dataset-edit-form .acl-tag button) {
  padding: 0;
  color: var(--editor-blue);
  font-size: 14px;
  line-height: 1;
  border: 0;
  background: transparent;
}

:global(.data-dataset-edit-form .dataset-text-action) {
  padding: 0;
  color: var(--editor-blue);
  font-size: 14px;
  border: 0;
  background: transparent;
}

:global(.data-dataset-edit-form .dataset-text-action:disabled) {
  color: var(--editor-disabled);
}

:global(.data-dataset-edit-form .sql-workbench) {
  display: grid;
  gap: 12px;
}

:global(.data-dataset-edit-form .sql-editor-shell) {
  min-width: 0;
}

:global(.data-dataset-edit-form .dataset-sql-editor) {
  border-color: #1e1e1e;
  border-radius: 4px;
  background: #1e1e1e;
}

:global(.data-dataset-edit-form .dataset-sql-editor .sql-editor__toolbar) {
  min-height: 40px;
  border-bottom: 1px solid #2f3338;
  background: #1e1e1e;
}

:global(.data-dataset-edit-form .dataset-sql-editor .sql-editor__title),
:global(.data-dataset-edit-form .dataset-sql-editor .sql-editor__title i) {
  color: #d4d4d4;
}

:global(.data-dataset-edit-form .dataset-sql-editor .sql-editor__body),
:global(.data-dataset-edit-form .dataset-sql-editor .sql-editor__container),
:global(.data-dataset-edit-form .dataset-sql-editor .cm-editor),
:global(.data-dataset-edit-form .dataset-sql-editor .cm-scroller) {
  min-height: 300px;
  background: #1e1e1e !important;
}

:global(.data-dataset-edit-form .dataset-sql-editor .cm-editor) {
  color: #d4d4d4 !important;
}

:global(.data-dataset-edit-form .dataset-sql-editor .cm-gutters) {
  color: #86909c !important;
  background: #1e1e1e !important;
  border-right: 1px solid #2f3338 !important;
}

:global(.data-dataset-edit-form .dataset-sql-editor .cm-activeLine),
:global(.data-dataset-edit-form .dataset-sql-editor .cm-activeLineGutter) {
  background: #252526 !important;
}

:global(.data-dataset-edit-form .dataset-sql-editor .sql-editor__placeholder) {
  color: #6b7280;
}

:global(.data-dataset-edit-form .sql-workbench-toolbar) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 40px;
  padding: 0 0 10px;
  background: #fff;
}

:global(.data-dataset-edit-form .sql-workbench-toolbar span) {
  color: var(--editor-text);
  font-size: 14px;
  font-weight: 500;
}

:global(.data-dataset-edit-form .sql-preview-shell) {
  min-width: 0;
  padding-top: 2px;
  background: #fff;
}

:global(.data-dataset-edit-form .sql-preview-shell .n-data-table-th) {
  color: #4e5969;
  font-size: 12px;
  font-weight: 500;
  background: var(--editor-bg);
}

:global(.data-dataset-edit-form .sql-preview-shell .n-data-table-td) {
  color: var(--editor-text);
  font-size: 12px;
}

:global(.data-dataset-edit-form .sql-preview-shell .n-data-table-tr:hover .n-data-table-td) {
  background: #f2f3f5;
}

:global(.data-dataset-edit-form .sql-preview-empty) {
  min-height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed var(--editor-border);
  border-radius: 4px;
  background: var(--editor-bg);
}

:global(.data-dataset-edit-form .sql-preview-note) {
  margin-top: 8px;
  color: var(--editor-muted);
  font-size: 12px;
}

:global(.data-dataset-edit-form .table-source-panel) {
  display: grid;
  gap: 12px;
}

:global(.data-dataset-edit-form .table-source-summary) {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  align-items: stretch;
}

:global(.data-dataset-edit-form .table-source-card) {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--editor-border);
  border-radius: 4px;
  background: var(--editor-bg);
}

:global(.data-dataset-edit-form .table-source-card span) {
  color: var(--editor-muted);
  font-size: 12px;
}

:global(.data-dataset-edit-form .table-source-card strong) {
  overflow: hidden;
  color: var(--editor-text);
  font-size: 14px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.data-dataset-edit-form .table-source-fields) {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--editor-border);
  border-radius: 4px;
  background: #fff;
}

:global(.data-dataset-edit-form .table-source-fields__head) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

:global(.data-dataset-edit-form .table-source-fields__head span) {
  color: var(--editor-text);
  font-size: 14px;
  font-weight: 500;
}

:global(.data-dataset-edit-form .table-source-fields__head small) {
  color: var(--editor-muted);
  font-size: 12px;
}

:global(.data-dataset-edit-form .table-source-field-list) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 174px;
  overflow: auto;
}

:global(.data-dataset-edit-form .table-source-field-chip) {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 26px;
  padding: 0 8px;
  overflow: hidden;
  color: #4e5969;
  font-size: 12px;
  border: 1px solid var(--editor-border-light);
  border-radius: 4px;
  background: var(--editor-bg);
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.data-dataset-edit-form .table-source-field-chip--more) {
  color: var(--editor-blue);
  border-color: transparent;
  background: var(--editor-blue-soft);
}

:global(.data-dataset-edit-form .table-source-actions) {
  display: flex;
  justify-content: flex-start;
}

:global(.data-dataset-edit-form .setting-row) {
  display: grid;
  grid-column: 1 / -1;
  gap: 8px;
}

:global(.data-dataset-edit-form .setting-row__head) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--editor-text);
  font-size: 14px;
  font-weight: 500;
}

:global(.data-dataset-edit-form .setting-row__head strong) {
  color: #4e5969;
  font-size: 12px;
  font-weight: 500;
}

:global(.data-dataset-edit-form .setting-row__control) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 128px;
  gap: 12px;
  align-items: center;
}

:global(.data-dataset-edit-form .access-control-block) {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

:global(.data-dataset-edit-form .access-mode-row) {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  min-width: 0;
}

:global(.data-dataset-edit-form .access-mode-row .n-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  min-width: 0;
}

:global(.data-dataset-edit-form .access-mode-row > span),
:global(.data-dataset-edit-form .row-permission-title > span) {
  color: var(--editor-text);
  font-size: 14px;
  font-weight: 500;
}

:global(.data-dataset-edit-form .acl-editor--compact) {
  padding: 0;
  border: 0;
  box-shadow: none;
}

:global(.data-dataset-edit-form .acl-editor__toolbar) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

:global(.data-dataset-edit-form .acl-editor__title) {
  color: var(--editor-text);
  font-size: 14px;
  font-weight: 500;
}

:global(.data-dataset-edit-form .acl-editor__hint) {
  margin-top: 4px;
  color: var(--editor-muted);
  font-size: 12px;
}

:global(.data-dataset-edit-form .acl-rows) {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

:global(.data-dataset-edit-form .acl-row) {
  display: grid;
  grid-template-columns: 94px minmax(180px, 1fr) 96px 36px;
  gap: 8px;
  align-items: center;
  padding: 8px;
  border: 1px solid var(--editor-border-light);
  border-radius: 4px;
  background: var(--editor-bg);
}

:global(.data-dataset-edit-form .row-permission-strip) {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0;
  border: 0;
  background: #fff;
}

:global(.data-dataset-edit-form .row-permission-title) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

:global(.data-dataset-edit-form .row-scope-expression) {
  min-height: 34px;
  padding: 8px;
  overflow-wrap: anywhere;
  color: #4e5969;
  font-family: 'JetBrains Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
  border-radius: 4px;
  background: var(--editor-bg);
}

:global(.data-dataset-edit-form .row-permission-rules) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

:global(.data-dataset-edit-form .rule-chip) {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 28px;
  padding: 0 8px;
  overflow-wrap: anywhere;
  color: var(--editor-blue);
  font-family: 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.5;
  border-radius: 4px;
  background: var(--editor-blue-soft);
  white-space: normal;
}

:global(.data-dataset-edit-form .rule-chip--empty) {
  color: var(--editor-muted);
  background: var(--editor-bg);
}

:global(.data-dataset-edit-form .row-scope-rule-mini-list) {
  display: grid;
  gap: 10px;
}

:global(.data-dataset-edit-form .row-scope-rule-mini) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 20px minmax(0, 1fr) 36px;
  gap: 8px;
  align-items: center;
  min-width: 0;
  padding: 8px;
  border: 1px solid var(--editor-border-light);
  border-radius: 4px;
  background: var(--editor-bg);
}

:global(.data-dataset-edit-form .row-scope-rule-mini .n-select) {
  min-width: 0;
}

:global(.data-dataset-edit-form .row-scope-rule-mini > span) {
  text-align: center;
  color: #4e5969;
  font-weight: 500;
}

:global(.data-dataset-edit-form .dataset-info-grid) {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  margin: 0;
}

:global(.data-dataset-edit-form .dataset-info-grid div) {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  gap: 10px;
  min-width: 0;
}

:global(.data-dataset-edit-form .dataset-info-grid dt) {
  color: var(--editor-muted);
  font-size: 12px;
}

:global(.data-dataset-edit-form .dataset-info-grid dd) {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: var(--editor-text);
  font-size: 12px;
  font-weight: 400;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.data-dataset-edit-form .dataset-current-version) {
  margin-left: 4px;
  color: var(--editor-blue);
}

:global(.data-dataset-edit-form .row-scope-empty) {
  padding: 20px 0;
  background: var(--editor-bg);
  border: 1px dashed var(--editor-border);
  border-radius: 4px;
}

:global(.data-dataset-edit-form .row-scope-condition-preview) {
  min-height: 38px;
  padding: 10px 12px;
  color: #4e5969;
  font-family: 'JetBrains Mono', 'Menlo', monospace;
  font-size: 12px;
  line-height: 1.6;
  background: var(--editor-bg);
  border: 1px solid var(--editor-border-light);
  border-radius: 4px;
}

:global(.data-dataset-edit-form .row-scope-remark) {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background: #fff;
  border: 1px solid var(--editor-border-light);
  border-radius: 4px;
}

:global(.data-dataset-edit-form .row-scope-remark label) {
  color: var(--editor-text);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
}

:global(.data-dataset-edit-form .dataset-editor-page) {
  --editor-blue: #1677ff;
  --editor-blue-soft: #e6f4ff;
  --editor-bg: #f5f7fb;
  --editor-border: #e8e8e8;
  --editor-border-light: #edf1f7;
  --editor-text: #1f2329;
  --editor-muted: #6b7280;
  --editor-disabled: #a8abb2;
  min-height: min(82vh, 980px);
  padding: 20px;
  background: #f5f7fb;
  border-radius: 4px;
}

:global(.data-dataset-edit-form .dataset-editor-header) {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 10px;
  border-color: var(--editor-border);
  background: #fff;
  box-shadow: 0 4px 12px rgb(31 35 41 / 3%);
}

:global(.data-dataset-edit-form .dataset-editor-icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: #fff;
  font-size: 19px;
  border-radius: 8px;
  background: #1677ff;
  box-shadow: 0 6px 12px rgb(22 119 255 / 18%);
}

:global(.data-dataset-edit-form .dataset-editor-breadcrumb) {
  margin-bottom: 4px;
  color: #667085;
  font-size: 12px;
}

:global(.data-dataset-edit-form .dataset-editor-title-row h2) {
  max-width: 680px;
  font-size: 16px;
  font-weight: 600;
}

:global(.data-dataset-edit-form .dataset-status-tag) {
  min-height: 20px;
  padding: 0 7px;
  color: #1677ff;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid #91caff;
  background: #e6f4ff;
}

:global(.data-dataset-edit-form .dataset-editor-actions) {
  align-self: center;
  gap: 10px;
}

:global(.data-dataset-edit-form .dataset-editor-actions .n-button) {
  min-width: 72px;
  min-height: 30px;
  padding: 0 14px;
}

:global(.data-dataset-edit-form .dataset-editor-steps) {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  padding: 12px 18px 14px;
  margin-bottom: 10px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 4px 12px rgb(31 35 41 / 3%);
}

:global(.data-dataset-edit-form .dataset-editor-steps::before) {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 1px;
  background: #edf1f7;
  content: '';
}

:global(.data-dataset-edit-form .dataset-editor-steps::after) {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 25%;
  height: 3px;
  background: #1677ff;
  content: '';
  transform: translateX(calc(var(--dataset-step-index, 0) * 100%));
  transition: transform 0.22s ease;
}

:global(.data-dataset-edit-form .dataset-editor-step) {
  position: relative;
  display: block;
  min-width: 0;
  min-height: 72px;
  padding: 0 10px;
  overflow: visible;
  text-align: center;
  border: 0;
  background: transparent;
}

:global(.data-dataset-edit-form .dataset-editor-step__content) {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 0;
  min-width: 0;
  width: 100%;
  height: 100%;
  background: #fff;
}

:global(.data-dataset-edit-form .dataset-editor-step__line) {
  position: absolute;
  top: 15px;
  right: calc(50% + 18px);
  left: calc(-50% + 18px);
  height: 1px;
  background: #e5e7eb;
  content: '';
}

:global(.data-dataset-edit-form .dataset-editor-step.is-completed + .dataset-editor-step .dataset-editor-step__line) {
  background: #1677ff;
}

:global(.data-dataset-edit-form .dataset-editor-step__index) {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  margin: 0 auto 8px;
  color: #1f2329;
  font-size: 14px;
  font-weight: 700;
  border: 1px solid #d9dee8;
  border-radius: 6px;
  background: #fff;
}

:global(.data-dataset-edit-form .dataset-editor-step__text) {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  min-width: 0;
  width: 100%;
}

:global(.data-dataset-edit-form .dataset-editor-step__label) {
  overflow: hidden;
  max-width: 100%;
  color: #333;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.data-dataset-edit-form .dataset-editor-step__caption) {
  overflow: hidden;
  max-width: 100%;
  color: #999;
  font-size: 12px;
  font-weight: 400;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.data-dataset-edit-form .dataset-editor-step.is-active .dataset-editor-step__index),
:global(.data-dataset-edit-form .dataset-editor-step.is-completed .dataset-editor-step__index) {
  color: #fff;
  border-color: #1677ff;
  background: #1677ff;
}

:global(.data-dataset-edit-form .dataset-editor-step.is-active .dataset-editor-step__label),
:global(.data-dataset-edit-form .dataset-editor-step.is-completed .dataset-editor-step__label) {
  color: #1677ff;
}

:global(.data-dataset-edit-form .dataset-editor-step.is-active .dataset-editor-step__caption) {
  color: #999;
}

:global(.data-dataset-edit-form .dataset-editor-grid) {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 14px;
  align-items: stretch;
}

:global(.data-dataset-edit-form .dataset-edit-panel) {
  min-width: 0;
  height: 100%;
  padding: 22px 24px;
  border-color: var(--editor-border);
  background: #fff;
  box-shadow: 0 8px 22px rgb(31 35 41 / 4%);
}

:global(.data-dataset-edit-form .dataset-edit-panel--basic) {
  grid-column: span 4;
}

:global(.data-dataset-edit-form .dataset-edit-panel--sql) {
  grid-column: span 8;
}

:global(.data-dataset-edit-form .dataset-edit-panel--params),
:global(.data-dataset-edit-form .dataset-edit-panel--access) {
  grid-column: span 7;
}

:global(.data-dataset-edit-form .dataset-edit-panel--settings),
:global(.data-dataset-edit-form .dataset-edit-panel--info) {
  grid-column: span 5;
}

:global(.data-dataset-edit-form .panel-section-head) {
  min-height: 28px;
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

:global(.data-dataset-edit-form .panel-section-head h3) {
  color: #1f2329;
  font-size: 16px;
  font-weight: 600;
}

:global(.data-dataset-edit-form .dataset-edit-panel--basic .dataset-form-grid) {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

:global(.data-dataset-edit-form .dataset-edit-panel--basic .dataset-field),
:global(.data-dataset-edit-form .dataset-edit-panel--basic .dataset-field--wide),
:global(.data-dataset-edit-form .dataset-edit-panel--basic .dataset-field--full) {
  display: grid;
  grid-column: auto;
  grid-template-columns: 94px minmax(0, 1fr);
  align-items: start;
  gap: 12px;
}

:global(.data-dataset-edit-form .dataset-field > span),
:global(.data-dataset-edit-form .setting-switch-row > span) {
  padding-top: 6px;
  color: #4e5969;
  font-size: 13px;
  font-weight: 500;
}

:global(.data-dataset-edit-form .dataset-edit-panel--basic .dataset-tag-list) {
  padding-top: 2px;
}

:global(.data-dataset-edit-form .data-source-select) {
  position: relative;
  min-width: 0;
}

:global(.data-dataset-edit-form .data-source-status-dot) {
  position: absolute;
  top: 50%;
  left: 11px;
  z-index: 1;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #52c41a;
  transform: translateY(-50%);
  box-shadow: 0 0 0 3px rgb(82 196 26 / 12%);
}

:global(.data-dataset-edit-form .data-source-select .n-base-selection-label) {
  padding-left: 16px;
}

:global(.data-dataset-edit-form .dataset-type-segment) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  min-width: 0;
}

:global(.data-dataset-edit-form .dataset-type-option) {
  min-width: 0;
  min-height: 34px;
  padding: 0 12px;
  overflow: hidden;
  color: #4e5969;
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  transition:
    color 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease;
}

:global(.data-dataset-edit-form .dataset-type-option:hover) {
  color: #1677ff;
  border-color: #1677ff;
}

:global(.data-dataset-edit-form .dataset-type-option.is-active) {
  color: #fff;
  border-color: #1677ff;
  background: #1677ff;
}

:global(.data-dataset-edit-form .dataset-type-option:disabled) {
  cursor: not-allowed;
  opacity: 0.58;
}

:global(.data-dataset-edit-form .dataset-soft-tag),
:global(.data-dataset-edit-form .acl-tag),
:global(.data-dataset-edit-form .rule-chip) {
  color: #344054;
  border: 1px solid #eaecf0;
  border-radius: 4px;
  background: #f8fafc;
}

:global(.data-dataset-edit-form .dataset-text-action) {
  color: #1677ff;
  font-weight: 500;
}

:global(.data-dataset-edit-form .sql-workbench) {
  grid-template-columns: minmax(420px, 1.25fr) minmax(320px, 0.75fr);
  gap: 14px;
  align-items: stretch;
}

:global(.data-dataset-edit-form .sql-editor-shell) {
  display: flex;
  min-width: 0;
}

:global(.data-dataset-edit-form .dataset-sql-editor) {
  flex: 1 1 auto;
  min-width: 0;
  height: 100%;
  border: 1px solid #e5e7eb;
  background: #fff;
}

:global(.data-dataset-edit-form .dataset-sql-editor .sql-editor__toolbar) {
  min-height: 42px;
  border-bottom: 1px solid #edf1f7;
  background: #fafafa;
}

:global(.data-dataset-edit-form .dataset-sql-editor .sql-editor__title),
:global(.data-dataset-edit-form .dataset-sql-editor .sql-editor__title i) {
  color: #344054;
}

:global(.data-dataset-edit-form .dataset-sql-editor .sql-editor__body),
:global(.data-dataset-edit-form .dataset-sql-editor .sql-editor__container),
:global(.data-dataset-edit-form .dataset-sql-editor .cm-editor),
:global(.data-dataset-edit-form .dataset-sql-editor .cm-scroller) {
  min-height: 378px;
  background: #fff !important;
}

:global(.data-dataset-edit-form .dataset-sql-editor .cm-editor) {
  color: #1f2329 !important;
}

:global(.data-dataset-edit-form .dataset-sql-editor .cm-gutters) {
  color: #98a2b3 !important;
  background: #fafafa !important;
  border-right: 1px solid #edf1f7 !important;
}

:global(.data-dataset-edit-form .dataset-sql-editor .cm-activeLine),
:global(.data-dataset-edit-form .dataset-sql-editor .cm-activeLineGutter) {
  background: #eef4ff !important;
}

:global(.data-dataset-edit-form .sql-preview-shell) {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 14px;
  border: 1px solid #edf1f7;
  background: #fff;
}

:global(.data-dataset-edit-form .sql-workbench-toolbar) {
  min-height: 34px;
  padding-bottom: 12px;
}

:global(.data-dataset-edit-form .sql-preview-shell .n-data-table) {
  flex: 1 1 auto;
}

:global(.data-dataset-edit-form .sql-preview-shell .n-data-table-th) {
  color: #344054;
  font-weight: 600;
  background: #f7f8fa;
}

:global(.data-dataset-edit-form .sql-preview-shell .n-data-table-td) {
  border-bottom: 1px solid #f0f2f5;
}

:global(.data-dataset-edit-form .execution-settings) {
  gap: 16px;
}

:global(.data-dataset-edit-form .setting-row__control) {
  grid-template-columns: minmax(0, 1fr) 108px;
  gap: 10px;
}

:global(.data-dataset-edit-form .setting-row__control .n-input-number) {
  width: 108px;
  min-width: 108px;
}

:global(.data-dataset-edit-form .setting-row__control .n-input__input-el) {
  text-align: right;
}

:global(.data-dataset-edit-form .access-mode-row) {
  grid-template-columns: 92px minmax(0, 1fr);
}

:global(.data-dataset-edit-form .row-scope-expression) {
  border: 1px solid #edf1f7;
  background: #f7f8fa;
}

:global(.data-dataset-edit-form .dataset-info-grid) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

:global(.data-dataset-edit-form .dataset-info-grid div) {
  grid-template-columns: 72px minmax(0, 1fr);
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

:global(.data-dataset-edit-form .dataset-info-grid div:last-child) {
  border-bottom: 0;
}

:global(.data-dataset-edit-form) {
  padding: 0;
}

:global(.data-dataset-edit-form .dataset-editor-page) {
  min-height: min(78vh, 900px);
  padding: 8px;
}

:global(.data-dataset-edit-form .dataset-editor-header) {
  grid-template-columns: 30px minmax(0, 1fr) auto;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 8px;
  box-shadow: none;
}

:global(.data-dataset-edit-form .dataset-editor-icon) {
  width: 30px;
  height: 30px;
  font-size: 17px;
}

:global(.data-dataset-edit-form .dataset-editor-breadcrumb) {
  display: none;
}

:global(.data-dataset-edit-form .dataset-editor-title-row h2) {
  font-size: 15px;
  line-height: 1.3;
}

:global(.data-dataset-edit-form .dataset-editor-meta) {
  margin-top: 2px;
  font-size: 11px;
}

:global(.data-dataset-edit-form .dataset-editor-actions) {
  gap: 6px;
}

:global(.data-dataset-edit-form .dataset-editor-actions .n-button) {
  min-width: 60px;
  min-height: 28px;
  padding: 0 10px;
}

:global(.data-dataset-edit-form .dataset-editor-steps) {
  padding: 4px 8px 6px;
  margin-bottom: 8px;
  border-radius: 4px;
  box-shadow: none;
}

:global(.data-dataset-edit-form .dataset-editor-step) {
  min-height: 34px;
  padding: 0 6px;
}

:global(.data-dataset-edit-form .dataset-editor-step__content) {
  flex-direction: row;
  justify-content: center;
  gap: 6px;
}

:global(.data-dataset-edit-form .dataset-editor-step__line) {
  display: none;
}

:global(.data-dataset-edit-form .dataset-editor-step__index) {
  width: 22px;
  height: 22px;
  margin: 0;
  font-size: 12px;
  border-radius: 5px;
}

:global(.data-dataset-edit-form .dataset-editor-step__text) {
  width: auto;
}

:global(.data-dataset-edit-form .dataset-editor-step__label) {
  font-size: 13px;
}

:global(.data-dataset-edit-form .dataset-editor-step__caption) {
  display: none;
}

:global(.data-dataset-edit-form .dataset-editor-grid) {
  gap: 10px;
}

:global(.data-dataset-edit-form .dataset-edit-panel) {
  padding: 12px 14px;
  box-shadow: none;
}

:global(.data-dataset-edit-form .panel-section-head) {
  min-height: 22px;
  margin-bottom: 10px;
  padding-bottom: 8px;
}

:global(.data-dataset-edit-form .panel-section-head h3) {
  font-size: 14px;
}

:global(.data-dataset-edit-form .dataset-form-grid),
:global(.data-dataset-edit-form .execution-settings) {
  gap: 10px;
}

:global(.data-dataset-edit-form .dataset-edit-panel--basic .dataset-form-grid) {
  gap: 10px;
}

:global(.data-dataset-edit-form .dataset-edit-panel--basic .dataset-field),
:global(.data-dataset-edit-form .dataset-edit-panel--basic .dataset-field--wide),
:global(.data-dataset-edit-form .dataset-edit-panel--basic .dataset-field--full) {
  grid-template-columns: 82px minmax(0, 1fr);
  gap: 8px;
}

:global(.data-dataset-edit-form .dataset-field > span),
:global(.data-dataset-edit-form .setting-switch-row > span) {
  padding-top: 5px;
  font-size: 12px;
}

:global(.data-dataset-edit-form .sql-workbench) {
  gap: 10px;
}

:global(.dataset-impact-dialog) {
  display: grid;
  gap: 10px;
  line-height: 1.6;
}

:global(.dataset-impact-dialog p) {
  margin: 0;
}

:global(.dataset-impact-dialog__summary) {
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff7ed;
  color: #9a3412;
  font-size: 13px;
}

:global(.dataset-impact-dialog__list) {
  display: grid;
  gap: 8px;
  max-height: 240px;
  margin: 0;
  padding: 0;
  overflow: auto;
  list-style: none;
}

:global(.dataset-impact-dialog__list li) {
  display: grid;
  gap: 3px;
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

:global(.dataset-impact-dialog__list strong) {
  color: #0f172a;
}

:global(.dataset-impact-dialog__list span) {
  color: #475569;
}

:global(.dataset-impact-dialog__list small),
:global(.dataset-impact-dialog__more) {
  color: #64748b;
  font-size: 12px;
}

@media (max-width: 1400px) {
  .studio-hero {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .dataset-workspace {
    grid-template-columns: 1fr;
  }

  .category-tree-shell {
    max-height: 360px;
  }
}

@media (max-width: 960px) {
  .dataset-studio {
    padding: 12px;
  }

  .hero-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-description {
    white-space: normal;
  }

  .toolbar-filters {
    grid-template-columns: 1fr;
  }

  .toolbar-title-row,
  .sidebar-head {
    flex-direction: column;
  }

  .toolbar-title-meta {
    width: 100%;
    justify-content: flex-start;
  }
}

:global(.data-dataset-edit-form .step-shell) {
  width: 100% !important;
  max-width: none !important;
  box-sizing: border-box;
  padding: 24px 28px;
  margin-bottom: 6px;
  background: linear-gradient(180deg, #fff 0%, #f7f9fc 100%);
  border: 1px solid #dbe3ef;
  border-radius: 24px;
  box-shadow: 0 20px 40px rgb(15 23 42 / 8%);
}

:global(.data-dataset-edit-form .step-shell.is-readonly) {
  border-color: #d8dee8;
}

:global(.data-dataset-edit-form .step-shell__header) {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

:global(.data-dataset-edit-form .step-shell__intro) {
  min-width: 0;
}

:global(.data-dataset-edit-form .step-shell__eyebrow) {
  margin: 0 0 10px;
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

:global(.data-dataset-edit-form .step-shell__title-row) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 12px;
}

:global(.data-dataset-edit-form .step-shell__title) {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
  line-height: 1.25;
}

:global(.data-dataset-edit-form .step-shell__progress) {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  color: #0f172a;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  background: #e2e8f0;
  border-radius: 999px;
}

:global(.data-dataset-edit-form .step-shell__description) {
  max-width: 760px;
  margin: 10px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.8;
}

:global(.data-dataset-edit-form .step-shell__status) {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  background: #e2e8f0;
  border-radius: 999px;
}

:global(.data-dataset-edit-form .step-shell__status.is-readonly) {
  color: #92400e;
  background: #fef3c7;
}

:global(.data-dataset-edit-form .step-progress) {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  width: 100%;
  max-width: none;
  gap: 0;
  padding-top: 4px;
}

:global(.data-dataset-edit-form .step-progress::before) {
  position: absolute;
  top: 26px;
  left: 22px;
  right: 22px;
  height: 2px;
  background: #dbe3ef;
  content: '';
}

:global(.data-dataset-edit-form .step-progress::after) {
  position: absolute;
  top: 26px;
  left: 22px;
  width: calc((100% - 44px) * var(--step-progress-percent));
  height: 2px;
  background: linear-gradient(90deg, #0f172a 0%, #1d4ed8 100%);
  content: '';
  transition: width 0.24s ease;
}

:global(.data-dataset-edit-form .step-node) {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  position: relative;
  z-index: 1;
}

:global(.data-dataset-edit-form .step-circle) {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #fff;
  border: 1.5px solid #cbd5e1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #64748b;
  transition: all 0.3s ease;
}

:global(.data-dataset-edit-form .step-node__meta) {
  min-width: 0;
}

:global(.data-dataset-edit-form .step-node:nth-child(3n - 1)) {
  align-items: center;
  text-align: center;
}

:global(.data-dataset-edit-form .step-node:nth-child(3n)) {
  align-items: flex-end;
  text-align: right;
}

:global(.data-dataset-edit-form .step-node.is-active .step-circle) {
  background: #0f172a;
  border-color: #0f172a;
  color: #fff;
  box-shadow: 0 10px 20px rgb(15 23 42 / 18%);
}

:global(.data-dataset-edit-form .step-node.is-completed .step-circle) {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: #fff;
  box-shadow: none;
}

:global(.data-dataset-edit-form .step-node.is-completed .step-circle i) {
  font-size: 18px;
}

:global(.data-dataset-edit-form .step-label) {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
}

:global(.data-dataset-edit-form .step-caption) {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
  white-space: nowrap;
}

:global(.data-dataset-edit-form .step-node.is-active .step-label) {
  color: #0f172a;
  font-weight: 700;
}

:global(.data-dataset-edit-form .step-node.is-active .step-caption) {
  color: #64748b;
}

:global(.data-dataset-edit-form .step-navigation-wrapper) {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  margin-top: 28px;
  padding: 18px 20px 4px;
  border-top: 1px solid #e2e8f0;
  background: linear-gradient(180deg, rgb(255 255 255 / 0%), rgb(255 255 255 / 88%) 18%, #fff 100%);
  position: sticky;
  bottom: 0;
  z-index: 3;
}

:global(.data-dataset-edit-form .step-navigation-meta) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  text-align: right;
}

:global(.data-dataset-edit-form .step-navigation-meta__label) {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

:global(.data-dataset-edit-form .step-navigation-meta__title) {
  color: #0f172a;
  font-size: 16px;
  line-height: 1.35;
}

:global(.data-dataset-edit-form .step-navigation-meta__desc) {
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

:global(.data-dataset-edit-form .step-nav-actions) {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

@media (max-width: 1180px) {
  .field-config-head {
    grid-template-columns: 1fr;
  }

  .field-config-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  :global(.data-dataset-edit-form .dataset-context-panel),
  :global(.data-dataset-edit-form .dataset-context-panel--compact) {
    grid-template-columns: 1fr;
  }

  :global(.data-dataset-edit-form .dataset-editor-header),
  :global(.data-dataset-edit-form .row-permission-title) {
    align-items: flex-start;
    flex-direction: column;
  }

  :global(.data-dataset-edit-form .access-mode-row) {
    grid-template-columns: 1fr;
  }

  :global(.data-dataset-edit-form .dataset-editor-actions) {
    width: 100%;
    justify-content: flex-start;
  }

  :global(.data-dataset-edit-form .dataset-editor-grid),
  :global(.data-dataset-edit-form .sql-workbench),
  :global(.data-dataset-edit-form .table-source-summary) {
    grid-template-columns: 1fr;
  }

  :global(.data-dataset-edit-form .sql-preview-shell) {
    border-top: 1px solid #e2e8f0;
    border-left: 0;
  }

  :global(.data-dataset-edit-form .permission-panel__header),
  :global(.data-dataset-edit-form .acl-editor__toolbar) {
    flex-direction: column;
  }

  :global(.data-dataset-edit-form .permission-facts),
  :global(.data-dataset-edit-form .row-scope-attribute-strip) {
    grid-template-columns: 1fr;
  }

  :global(.data-dataset-edit-form .acl-row),
  :global(.data-dataset-edit-form .row-scope-rule-header),
  :global(.data-dataset-edit-form .row-scope-rule-row),
  :global(.data-dataset-edit-form .row-scope-rule-mini) {
    grid-template-columns: 1fr;
  }

  :global(.data-dataset-edit-form .row-scope-rule-header) {
    display: none;
  }

  :global(.data-dataset-edit-form .row-scope-equals) {
    width: 100%;
  }

  :global(.data-dataset-edit-form .step-progress) {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  :global(.data-dataset-edit-form .step-progress::before),
  :global(.data-dataset-edit-form .step-progress::after) {
    display: none;
  }

  :global(.data-dataset-edit-form .step-node) {
    align-items: flex-start;
    text-align: left;
  }

  :global(.data-dataset-edit-form .step-navigation-wrapper) {
    align-items: flex-start;
    flex-direction: column;
  }

  :global(.data-dataset-edit-form .step-node:nth-child(3n - 1)),
  :global(.data-dataset-edit-form .step-node:nth-child(3n)) {
    align-items: flex-start;
    text-align: left;
  }

  :global(.data-dataset-edit-form .step-nav-actions) {
    width: 100%;
    justify-content: flex-start;
  }

  :global(.data-dataset-edit-form .step-navigation-meta) {
    text-align: left;
  }
}

@media (max-width: 768px) {
  .field-config-stats {
    grid-template-columns: 1fr;
  }

  :global(.data-dataset-edit-form .step-shell) {
    padding: 20px 18px;
  }

  :global(.data-dataset-edit-form .step-shell__header) {
    flex-direction: column;
  }

  :global(.data-dataset-edit-form .context-panel__facts) {
    grid-template-columns: 1fr;
  }

  :global(.data-dataset-edit-form .dataset-editor-title-row h2) {
    max-width: 100%;
    white-space: normal;
  }

  :global(.data-dataset-edit-form .dataset-form-grid),
  :global(.data-dataset-edit-form .execution-settings),
  :global(.data-dataset-edit-form .dataset-info-grid) {
    grid-template-columns: 1fr;
  }

  :global(.data-dataset-edit-form .dataset-field),
  :global(.data-dataset-edit-form .dataset-field--wide),
  :global(.data-dataset-edit-form .dataset-field--full),
  :global(.data-dataset-edit-form .setting-switch-row) {
    grid-column: auto;
  }

  :global(.data-dataset-edit-form .dataset-editor-steps),
  :global(.data-dataset-edit-form .setting-row__control) {
    grid-template-columns: 1fr;
  }

  :global(.data-dataset-edit-form .dataset-editor-step) {
    grid-template-columns: 1fr;
    padding-right: 0;
  }

  :global(.data-dataset-edit-form .dataset-editor-step__separator) {
    display: none;
  }

  :global(.data-dataset-edit-form .sql-preview-action) {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 1180px) {
  :global(.data-dataset-edit-form .dataset-editor-header) {
    grid-template-columns: 34px minmax(0, 1fr);
  }

  :global(.data-dataset-edit-form .dataset-editor-actions) {
    grid-column: 1 / -1;
  }

  :global(.data-dataset-edit-form .dataset-edit-panel--basic),
  :global(.data-dataset-edit-form .dataset-edit-panel--sql),
  :global(.data-dataset-edit-form .dataset-edit-panel--params),
  :global(.data-dataset-edit-form .dataset-edit-panel--settings),
  :global(.data-dataset-edit-form .dataset-edit-panel--access),
  :global(.data-dataset-edit-form .dataset-edit-panel--info) {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  :global(.data-dataset-edit-form .dataset-editor-header) {
    grid-template-columns: 1fr;
  }

  :global(.data-dataset-edit-form .dataset-editor-icon) {
    width: 32px;
    height: 32px;
  }

  :global(.data-dataset-edit-form .dataset-edit-panel--basic .dataset-field),
  :global(.data-dataset-edit-form .dataset-edit-panel--basic .dataset-field--wide),
  :global(.data-dataset-edit-form .dataset-edit-panel--basic .dataset-field--full) {
    grid-template-columns: 1fr;
  }
}
</style>
