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
          :hide-modal-footer="isFormReadOnly"
          row-key="id"
          :load-detail-on-edit="true"
          :striped="false"
          :bordered="false"
          :scroll-x="1700"
          max-height="calc(100vh - 420px)"
          :edit-grid-cols="12"
          edit-label-placement="top"
          edit-form-class="data-dataset-edit-form"
          modal-type="modal"
          modal-width="min(1320px, calc(100vw - 32px))"
          add-button-text="新增数据集"
          @load-list-success="handleDatasetLoadSuccess"
          @modal-close="handleDatasetModalClose"
        >
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

          <template #form-publishReadonlyAlert>
            <n-alert type="warning" :show-icon="false">
              当前数据集已发布，内容处于只读状态。如需修改，请先在列表中执行“下架”。
            </n-alert>
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

          <template #form-accessPermissionConfig="{ formData }">
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
                  v-model:value="formData.accessMode"
                  :disabled="isFormReadOnly"
                  @update:value="value => handleAccessModeChange(value, formData)"
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
                    :disabled="isFormReadOnly"
                    @click="addAclItem(formData)"
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
                      v-model:value="item.subjectType"
                      :options="aclSubjectTypeOptions"
                      :disabled="isFormReadOnly"
                      @update:value="() => handleAclSubjectTypeChange(item)"
                    />
                    <n-tree-select
                      v-if="item.subjectType === 'ORG'"
                      v-model:value="item.subjectId"
                      :options="getAclOrgOptions(item)"
                      :disabled="isFormReadOnly"
                      clearable
                      filterable
                      default-expand-all
                      placeholder="选择组织"
                    />
                    <NSelect
                      v-else
                      v-model:value="item.subjectId"
                      :options="getAclSubjectOptions(item)"
                      :disabled="isFormReadOnly"
                      clearable
                      filterable
                      placeholder="选择授权主体"
                    />
                    <NSelect
                      v-model:value="item.accessLevel"
                      :options="accessLevelOptions"
                      :disabled="isFormReadOnly"
                      placeholder="权限级别"
                    />
                    <n-button
                      quaternary
                      type="error"
                      :disabled="isFormReadOnly"
                      @click="removeAclItem(formData, index)"
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

          <template #form-rowScopeConfig="{ formData }">
            <div class="permission-panel row-scope-panel">
              <div class="permission-panel__header">
                <div>
                  <div class="context-panel__eyebrow">
                    Row Scope
                  </div>
                  <div class="permission-panel__title">
                    数据行权限
                  </div>
                  <div class="permission-panel__desc">
                    启用后按登录人的系统数据范围动态生成过滤条件，字段映射只从当前数据集字段中选择。
                  </div>
                </div>
                <n-switch
                  v-model:value="formData.rowScope.enabled"
                  :checked-value="1"
                  :unchecked-value="0"
                  :disabled="isFormReadOnly"
                >
                  <template #checked>
                    启用
                  </template>
                  <template #unchecked>
                    关闭
                  </template>
                </n-switch>
              </div>

              <div class="permission-facts">
                <div class="permission-fact">
                  <span>权限来源</span>
                  <strong>系统角色数据范围</strong>
                </div>
                <div class="permission-fact">
                  <span>字段来源</span>
                  <strong>{{ getRowScopeFieldSourceLabel(formData) }}</strong>
                </div>
                <div class="permission-fact">
                  <span>行政区划策略</span>
                  <strong>{{ getRowScopeStrategyLabel(formData.rowScope.regionStrategy) }}</strong>
                </div>
              </div>

              <n-alert
                v-if="formData.datasetType === 'SQL' && formData.rowScope.enabled === 1"
                type="warning"
                :show-icon="false"
                class="row-scope-alert"
              >
                SQL 数据集启用行权限时，需要在 SQL 的过滤位置预留 /*DATA_SCOPE*/ 占位符。
              </n-alert>

              <n-alert
                v-if="formData.rowScope.enabled === 1 && getRowScopeFieldOptions(formData).length === 0"
                type="warning"
                :show-icon="false"
                class="row-scope-alert"
              >
                当前暂无可选字段，请先保存并同步字段；单表数据集也可以刷新来源表字段后再配置。
              </n-alert>

              <div class="row-scope-grid" :class="{ 'is-disabled': formData.rowScope.enabled !== 1 }">
                <div class="row-scope-field">
                  <label>租户字段</label>
                  <NSelect
                    v-model:value="formData.rowScope.tenantColumn"
                    :options="getRowScopeFieldOptions(formData)"
                    :loading="rowScopeTableFieldLoading"
                    :disabled="isFormReadOnly || formData.rowScope.enabled !== 1"
                    clearable
                    filterable
                    placeholder="选择 tenant_id 字段"
                  />
                </div>
                <div class="row-scope-field">
                  <label>组织字段</label>
                  <NSelect
                    v-model:value="formData.rowScope.orgColumn"
                    :options="getRowScopeFieldOptions(formData)"
                    :loading="rowScopeTableFieldLoading"
                    :disabled="isFormReadOnly || formData.rowScope.enabled !== 1"
                    clearable
                    filterable
                    placeholder="选择组织字段"
                  />
                </div>
                <div class="row-scope-field">
                  <label>用户字段</label>
                  <NSelect
                    v-model:value="formData.rowScope.userColumn"
                    :options="getRowScopeFieldOptions(formData)"
                    :loading="rowScopeTableFieldLoading"
                    :disabled="isFormReadOnly || formData.rowScope.enabled !== 1"
                    clearable
                    filterable
                    placeholder="选择用户字段"
                  />
                </div>
                <div class="row-scope-field">
                  <label>行政区划字段</label>
                  <NSelect
                    v-model:value="formData.rowScope.regionColumn"
                    :options="getRowScopeFieldOptions(formData)"
                    :loading="rowScopeTableFieldLoading"
                    :disabled="isFormReadOnly || formData.rowScope.enabled !== 1"
                    clearable
                    filterable
                    placeholder="选择 region_code 字段"
                  />
                </div>
                <div class="row-scope-field">
                  <label>区划范围</label>
                  <NSelect
                    v-model:value="formData.rowScope.regionStrategy"
                    :options="rowScopeStrategyOptions"
                    :disabled="isFormReadOnly || formData.rowScope.enabled !== 1"
                    placeholder="选择区划策略"
                  />
                </div>
                <div class="row-scope-field row-scope-field--action">
                  <label>字段刷新</label>
                  <n-button
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
                <div class="row-scope-field row-scope-field--wide">
                  <label>备注</label>
                  <NInput
                    v-model:value="formData.rowScope.remark"
                    type="textarea"
                    :disabled="isFormReadOnly || formData.rowScope.enabled !== 1"
                    :autosize="{ minRows: 2, maxRows: 4 }"
                    placeholder="记录该数据集行权限字段口径"
                  />
                </div>
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
import { computed, h, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDataConnectionFields, getDataConnectionList, getDataConnectionTables } from '@/api/data/connection'
import {
  deleteDataDataset,
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
import SqlEditor from '@/components/SqlEditor.vue'
import { request } from '@/utils'

defineOptions({ name: 'DataDataset' })

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
const activeCategoryScope = ref('all')
const selectedCategoryId = ref(null)
const currentFormMode = ref('edit')
const currentEditingDataset = ref(null)
const currentStep = ref(1)
const stepDefinitions = [
  {
    label: '数据集定义',
    caption: '编码 / 分类 / 来源',
    title: '先把数据集身份和接入来源定清楚',
    description: '编码、名称、分类和来源会影响字段同步、参数建模和后续发布方式。',
  },
  {
    label: '查询条件',
    caption: '参数 / 字段 / 约束',
    title: '只定义真正会被下游消费的筛选条件',
    description: '单表模式强调字段映射，SQL 模式强调参数名与语句保持一致。',
  },
  {
    label: '执行设置',
    caption: '行数 / 超时 / 描述',
    title: '控制查询边界，保证运行时稳定',
    description: '通过返回行数和超时限制，把数据集控制在可复用、可治理的范围内。',
  },
  {
    label: '权限控制',
    caption: '访问 / 行权限',
    title: '开放给合适的人，并按数据范围收敛查询结果',
    description: '访问权限控制谁能用数据集，行权限复用登录人的系统数据范围动态过滤数据。',
  },
]
const totalSteps = stepDefinitions.length

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

const datasetTypeOptions = [
  { label: '单表数据集', value: 'TABLE' },
  { label: 'SQL数据集', value: 'SQL' },
]

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

const publishStatusOptions = [
  { label: '未发布', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已下架', value: 2 },
]

const accessModeOptions = [
  { label: '公开', value: 'PUBLIC' },
  { label: '私有', value: 'PRIVATE' },
]

const aclSubjectTypeOptions = [
  { label: '角色', value: 'ROLE' },
  { label: '用户', value: 'USER' },
  { label: '组织', value: 'ORG' },
]

const accessLevelOptions = [
  { label: '查看', value: 'VIEW' },
  { label: '查询', value: 'QUERY' },
  { label: '管理', value: 'MANAGE' },
]

const rowScopeStrategyOptions = [
  { label: '仅本级', value: 'SELF' },
  { label: '本级及直接下级', value: 'SELF_AND_CHILDREN' },
  { label: '本级及所有下级', value: 'SELF_AND_DESCENDANTS' },
]

const dataTypeOptions = [
  { label: '文本 STRING', value: 'STRING' },
  { label: '数值 NUMBER', value: 'NUMBER' },
  { label: '日期 DATE', value: 'DATE' },
  { label: '日期时间 DATETIME', value: 'DATETIME' },
  { label: '布尔 BOOLEAN', value: 'BOOLEAN' },
]

const fieldRoleOptions = [
  { label: '维度', value: 'DIMENSION' },
  { label: '指标', value: 'MEASURE' },
]

const sensitiveLevelOptions = [
  { label: '不脱敏', value: 'NONE' },
  { label: '脱敏展示', value: 'MASK' },
  { label: '隐藏字段', value: 'HIDDEN' },
]

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
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: getPublishStatusTagType(row.publishStatus),
    }, { default: () => getPublishStatusLabel(row.publishStatus) }),
  },
  {
    prop: 'accessMode',
    label: '访问权限',
    width: 110,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: row.accessMode === 'PRIVATE' ? 'warning' : 'success',
    }, { default: () => getAccessModeLabel(row.accessMode) }),
  },
  {
    prop: 'status',
    label: '可用状态',
    width: 110,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: row.status === 1 ? 'success' : 'default',
    }, { default: () => row.status === 1 ? '启用' : '禁用' }),
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
    render: row => renderFieldSelect(row, 'fieldRole', fieldRoleOptions, {
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
    render: row => renderFieldSelect(row, 'sensitiveLevel', sensitiveLevelOptions, { placeholder: '脱敏策略' }),
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

const step1Schema = computed(() => [
  {
    field: 'publishReadonlyAlert',
    label: '',
    type: 'slot',
    slotName: 'publishReadonlyAlert',
    span: 12,
    showFeedback: false,
    vIf: () => isFormReadOnly.value,
  },
  {
    field: 'stepIndicator',
    label: '',
    type: 'slot',
    slotName: 'stepIndicator',
    span: 12,
    showFeedback: false,
  },
  {
    field: '__sectionBasic',
    label: '基础信息',
    type: 'divider',
    span: 12,
    showFeedback: false,
    props: { class: 'dataset-form-divider' },
  },
  {
    field: 'datasetCode',
    label: '数据集编码',
    type: 'input',
    span: 3,
    disabled: () => isFormReadOnly.value,
    rules: [{ required: true, message: '请输入数据集编码', trigger: 'blur' }],
    props: { placeholder: '请输入数据集编码' },
  },
  {
    field: 'datasetName',
    label: '数据集名称',
    type: 'input',
    span: 4,
    disabled: () => isFormReadOnly.value,
    rules: [{ required: true, message: '请输入数据集名称', trigger: 'blur' }],
    props: { placeholder: '请输入数据集名称' },
  },
  {
    field: 'categoryId',
    label: '业务分类',
    type: 'treeSelect',
    span: 5,
    disabled: () => isFormReadOnly.value,
    props: {
      options: categoryTreeSelectOptions.value,
      clearable: true,
      defaultExpandAll: true,
      placeholder: '请选择业务分类',
    },
  },
  {
    field: 'status',
    label: '可用状态',
    type: 'radio',
    span: 4,
    disabled: () => isFormReadOnly.value,
    defaultValue: 1,
    props: { options: statusOptions },
  },
  {
    field: '__sectionSource',
    label: '数据来源',
    type: 'divider',
    span: 12,
    showFeedback: false,
    props: { class: 'dataset-form-divider' },
  },
  {
    field: 'connectionId',
    label: '数据连接',
    type: 'select',
    span: 4,
    disabled: () => isFormReadOnly.value,
    props: { placeholder: '请选择数据连接', options: connectionOptions.value, filterable: true },
    onChange: ({ value, formData }) => handleConnectionChange(value, formData),
  },
  {
    field: 'datasetType',
    label: '数据集类型',
    type: 'radio',
    span: 3,
    disabled: ({ context }) => isFormReadOnly.value || context?.isEdit,
    defaultValue: 'TABLE',
    rules: [{ required: true }],
    props: { options: datasetTypeOptions },
    onChange: ({ value, formData }) => handleDatasetTypeChange(value, formData),
  },
  {
    field: 'tableName',
    label: '数据表',
    type: 'select',
    span: 5,
    disabled: () => isFormReadOnly.value,
    props: {
      placeholder: '请先选择数据连接，再选择数据表',
      options: tableOptions.value,
      loading: tableLoading.value,
      filterable: true,
      clearable: true,
    },
    onChange: ({ value, formData }) => handleTableNameChange(value, formData),
    vIf: formData => formData.datasetType === 'TABLE',
  },
  {
    field: 'sqlText',
    label: '查询SQL',
    type: 'slot',
    slotName: 'sqlText',
    span: 12,
    rules: [{ required: true, message: '请输入查询SQL', trigger: 'blur' }],
    vIf: formData => formData.datasetType === 'SQL',
  },
  {
    field: 'sqlPreviewAction',
    label: '',
    type: 'slot',
    slotName: 'sqlPreviewAction',
    span: 12,
    showFeedback: false,
    vIf: formData => formData.datasetType === 'SQL',
  },
  {
    field: 'sourceGuide',
    label: '',
    type: 'slot',
    slotName: 'sourceGuide',
    span: 12,
    showFeedback: false,
  },
  {
    field: 'stepNavigation',
    label: '',
    type: 'slot',
    slotName: 'stepNavigation',
    span: 12,
    showFeedback: false,
  },
])

const step2Schema = computed(() => [
  {
    field: 'stepIndicator',
    label: '',
    type: 'slot',
    slotName: 'stepIndicator',
    span: 12,
    showFeedback: false,
  },
  {
    field: '__sectionParam',
    label: '查询条件定义',
    type: 'divider',
    span: 12,
    showFeedback: false,
    props: { class: 'dataset-form-divider' },
  },
  {
    field: 'paramGuide',
    label: '',
    type: 'slot',
    slotName: 'paramGuide',
    span: 12,
    showFeedback: false,
  },
  {
    field: 'paramSchemaJson',
    label: '查询条件',
    type: 'slot',
    slotName: 'paramSchemaJson',
    span: 12,
    showFeedback: false,
  },
  {
    field: 'stepNavigation',
    label: '',
    type: 'slot',
    slotName: 'stepNavigation',
    span: 12,
    showFeedback: false,
  },
])

const step3Schema = computed(() => [
  {
    field: 'stepIndicator',
    label: '',
    type: 'slot',
    slotName: 'stepIndicator',
    span: 12,
    showFeedback: false,
  },
  {
    field: '__sectionSetting',
    label: '执行设置',
    type: 'divider',
    span: 12,
    showFeedback: false,
    props: { class: 'dataset-form-divider' },
  },
  {
    field: 'settingGuide',
    label: '',
    type: 'slot',
    slotName: 'settingGuide',
    span: 12,
    showFeedback: false,
  },
  {
    field: 'maxRows',
    label: '最大返回行数',
    type: 'number',
    span: 3,
    disabled: () => isFormReadOnly.value,
    defaultValue: 1000,
    props: { placeholder: '请输入最大返回行数', min: 1, max: 10000 },
  },
  {
    field: 'timeoutSeconds',
    label: '查询超时(秒)',
    type: 'number',
    span: 3,
    disabled: () => isFormReadOnly.value,
    defaultValue: 15,
    props: { placeholder: '请输入超时时间', min: 1, max: 300 },
  },
  {
    field: 'description',
    label: '描述',
    type: 'textarea',
    span: 6,
    disabled: () => isFormReadOnly.value,
    props: { placeholder: '请输入描述', rows: 3 },
  },
  {
    field: 'stepNavigation',
    label: '',
    type: 'slot',
    slotName: 'stepNavigation',
    span: 12,
    showFeedback: false,
  },
])

const step4Schema = computed(() => [
  {
    field: 'stepIndicator',
    label: '',
    type: 'slot',
    slotName: 'stepIndicator',
    span: 12,
    showFeedback: false,
  },
  {
    field: '__sectionAccessPermission',
    label: '访问权限',
    type: 'divider',
    span: 12,
    showFeedback: false,
    props: { class: 'dataset-form-divider' },
  },
  {
    field: 'accessPermissionConfig',
    label: '',
    type: 'slot',
    slotName: 'accessPermissionConfig',
    span: 12,
    showFeedback: false,
  },
  {
    field: '__sectionRowScope',
    label: '数据行权限',
    type: 'divider',
    span: 12,
    showFeedback: false,
    props: { class: 'dataset-form-divider' },
  },
  {
    field: 'rowScopeConfig',
    label: '',
    type: 'slot',
    slotName: 'rowScopeConfig',
    span: 12,
    showFeedback: false,
  },
  {
    field: 'stepNavigation',
    label: '',
    type: 'slot',
    slotName: 'stepNavigation',
    span: 12,
    showFeedback: false,
  },
])

const editSchema = computed(() => {
  if (currentStep.value === 1)
    return step1Schema.value
  if (currentStep.value === 2)
    return step2Schema.value
  if (currentStep.value === 3)
    return step3Schema.value
  return step4Schema.value
})

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

function getPublishStatusLabel(status) {
  return publishStatusOptions.find(item => item.value === status)?.label || '未发布'
}

function getPublishStatusTagType(status) {
  if (status === 1) {
    return 'success'
  }
  if (status === 2) {
    return 'warning'
  }
  return 'default'
}

function getAccessModeLabel(accessMode) {
  return accessMode === 'PRIVATE' ? '私有' : '公开'
}

function getRowScopeStrategyLabel(strategy) {
  const option = rowScopeStrategyOptions.find(item => item.value === strategy)
  return option?.label || '本级及所有下级'
}

function getDatasetSourceGuide(formData) {
  if (formData?.datasetType === 'SQL') {
    return 'SQL 数据集适合多表关联、预聚合和复杂过滤，保存前建议先执行 SQL 预览。'
  }
  return '单表数据集适合标准明细表和维表建模，字段同步会按所选数据表结构生成。'
}

function getDatasetTypeLabel(datasetType) {
  return datasetType === 'SQL' ? 'SQL 数据集' : '单表数据集'
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
  return nextFormData
}

async function handleConnectionChange(connectionId, formData) {
  formData.tableName = null
  clearRowScopeColumns(formData)
  resetRowScopeTableFields()
  if (formData.datasetType === 'TABLE') {
    await loadTableOptions(connectionId)
  }
}

async function handleDatasetTypeChange(datasetType, formData) {
  clearRowScopeColumns(formData)
  resetRowScopeTableFields()
  if (datasetType === 'TABLE') {
    formData.sqlText = null
    await loadTableOptions(formData.connectionId)
    return
  }

  formData.tableName = null
}

async function handleTableNameChange(tableName, formData) {
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
  await Promise.all([
    loadRoleOptions(),
    loadUserOptions(),
    loadOrgOptions(),
  ])
  permissionOptionsLoaded.value = true
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
        value: role.id,
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
        value: user.id,
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
    value: item.id,
    key: item.id,
    children: item.children?.length ? transformOrgTreeOptions(item.children) : undefined,
  }))
}

function beforeSubmit(formData) {
  if (isFormReadOnly.value) {
    return false
  }

  delete formData.publishReadonlyAlert
  delete formData.datasetOverview
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
    subjectId: item.subjectId ?? null,
    accessLevel: normalizeAccessLevel(item.accessLevel),
  }))
}

function normalizeRowScope(rowScope) {
  return {
    ...createDefaultRowScope(),
    ...(rowScope || {}),
    enabled: rowScope?.enabled === 1 ? 1 : 0,
    regionStrategy: rowScope?.regionStrategy || 'SELF_AND_DESCENDANTS',
  }
}

function normalizeAclSubjectType(subjectType) {
  return ['USER', 'ROLE', 'ORG'].includes(subjectType) ? subjectType : 'ROLE'
}

function normalizeAccessLevel(accessLevel) {
  return ['VIEW', 'QUERY', 'MANAGE'].includes(accessLevel) ? accessLevel : 'QUERY'
}

function handleAccessModeChange(value, formData) {
  formData.accessMode = value === 'PRIVATE' ? 'PRIVATE' : 'PUBLIC'
  if (formData.accessMode === 'PRIVATE') {
    loadPermissionOptions()
  }
}

function getAclCount(formData) {
  return Array.isArray(formData?.aclItems) ? formData.aclItems.length : 0
}

function addAclItem(formData) {
  loadPermissionOptions()
  if (!Array.isArray(formData.aclItems)) {
    formData.aclItems = []
  }
  formData.aclItems.push(createDefaultAclItem())
}

function removeAclItem(formData, index) {
  if (!Array.isArray(formData.aclItems)) {
    return
  }
  formData.aclItems.splice(index, 1)
}

function handleAclSubjectTypeChange(item) {
  item.subjectId = null
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
  if (!value || options.some(item => item.value === value)) {
    return options
  }
  return [...options, { label, value }]
}

function appendMissingTreeOption(options, value, label) {
  if (!value || containsTreeValue(options, value)) {
    return options
  }
  return [...options, { label, value, key: value }]
}

function containsTreeValue(options, value) {
  for (const option of options || []) {
    if (option.value === value) {
      return true
    }
    if (containsTreeValue(option.children || [], value)) {
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
    const subjectId = item?.subjectId
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
    rowScope.enabled = 0
    return rowScope
  }

  const columns = [
    rowScope.tenantColumn,
    rowScope.orgColumn,
    rowScope.userColumn,
    rowScope.regionColumn,
  ].map(value => typeof value === 'string' ? value.trim() : value).filter(Boolean)
  if (columns.length === 0) {
    window.$message?.error('启用数据行权限后，至少需要配置一个权限字段')
    return null
  }

  if (formData.datasetType === 'SQL' && !String(formData.sqlText || '').includes('/*DATA_SCOPE*/')) {
    window.$message?.error('SQL 数据集启用行权限时，SQL 中必须包含 /*DATA_SCOPE*/ 占位符')
    return null
  }

  return {
    enabled: 1,
    scopeMode: rowScope.scopeMode || 'SYSTEM_DATA_SCOPE',
    tenantColumn: trimToNull(rowScope.tenantColumn),
    orgColumn: trimToNull(rowScope.orgColumn),
    userColumn: trimToNull(rowScope.userColumn),
    regionColumn: trimToNull(rowScope.regionColumn),
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

async function handlePreviewSql(formData) {
  if (!formData.connectionId) {
    window.$message?.error('请选择数据连接')
    return
  }
  if (!formData.sqlText) {
    window.$message?.error('请输入查询SQL')
    return
  }

  sqlPreviewVisible.value = true
  sqlPreviewLoading.value = true
  sqlPreviewColumns.value = []
  sqlPreviewRows.value = []
  sqlPreviewScrollX.value = 0

  try {
    const res = await request.post('/data/dataset/preview-sql', {
      connectionId: formData.connectionId,
      sqlText: formData.sqlText,
      maxRows: 10,
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

function handleOfflineDataset(row) {
  window.$dialog.warning({
    title: '确认下架',
    content: `下架后数据集“${row.datasetName}”将暂停供下游使用，但可以继续修改，确认继续吗？`,
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
  --studio-bg: linear-gradient(180deg, #f4f7fb 0%, #eef3f8 100%);
  --panel-bg: rgb(255 255 255 / 92%);
  --panel-border: rgb(148 163 184 / 16%);
  --panel-shadow: 0 18px 50px rgb(15 23 42 / 10%);
  --panel-radius: 24px;
  min-height: 100%;
  padding: 20px;
  background: var(--studio-bg);
}

.studio-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(420px, 0.8fr);
  gap: 18px;
  margin-bottom: 18px;
}

.hero-main,
.hero-stat-card,
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
  padding: 28px 30px;
  background:
    radial-gradient(circle at top left, rgb(59 130 246 / 18%), transparent 40%),
    radial-gradient(circle at 90% 25%, rgb(14 165 233 / 16%), transparent 28%),
    linear-gradient(135deg, rgb(255 255 255 / 94%), rgb(248 251 255 / 92%));
}

.hero-kicker,
.panel-kicker {
  margin: 0 0 8px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hero-title {
  margin: 0;
  color: #0f172a;
  font-size: 34px;
  line-height: 1.15;
}

.hero-description {
  max-width: 720px;
  margin: 14px 0 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.8;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.hero-stat-card {
  padding: 22px 20px;
}

.hero-stat-card::after {
  position: absolute;
  inset: auto -18px -24px auto;
  width: 96px;
  height: 96px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgb(14 165 233 / 18%), rgb(37 99 235 / 0%));
  content: '';
}

.hero-stat-label {
  color: #64748b;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-stat-value {
  margin-top: 12px;
  color: #0f172a;
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
}

.hero-stat-note {
  margin-top: 10px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.dataset-workspace {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.workspace-sidebar,
.workspace-main {
  padding: 20px;
}

.sidebar-head,
.toolbar-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.sidebar-head h3,
.toolbar-title-row h3 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
}

.sidebar-shortcuts {
  display: flex;
  gap: 10px;
  margin: 18px 0 14px;
}

.scope-chip {
  padding: 9px 14px;
  color: #334155;
  font-size: 12px;
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
  margin-bottom: 14px;
}

.category-tree-shell {
  min-height: 360px;
  max-height: calc(100vh - 520px);
  padding: 12px;
  overflow: auto;
  background: linear-gradient(180deg, #fbfdff 0%, #f8fbff 100%);
  border: 1px solid #e2e8f0;
  border-radius: 18px;
}

.category-detail-card {
  margin-top: 12px;
  padding: 14px 16px;
  background: linear-gradient(135deg, rgb(15 23 42 / 0.94), rgb(30 41 59 / 0.92));
  border-radius: 18px;
}

.category-detail-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.category-detail-name {
  color: #f8fafc;
  font-size: 18px;
  font-weight: 600;
}

.category-detail-code {
  margin-top: 6px;
  color: rgb(186 230 253 / 0.86);
  font-size: 12px;
}

.category-detail-desc {
  margin: 14px 0 0;
  color: rgb(226 232 240 / 0.8);
  font-size: 13px;
  line-height: 1.7;
}

.main-toolbar {
  margin-bottom: 18px;
  padding: 18px 18px 16px;
  background: linear-gradient(180deg, rgb(248 250 252 / 0.86), rgb(255 255 255 / 0.94));
  border: 1px solid #e2e8f0;
  border-radius: 20px;
}

.toolbar-title-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.toolbar-scope {
  padding: 6px 10px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 600;
  background: #ecfeff;
  border: 1px solid #a5f3fc;
  border-radius: 999px;
}

.toolbar-filters {
  display: grid;
  grid-template-columns: minmax(260px, 1.5fr) repeat(3, minmax(150px, 0.8fr)) auto;
  gap: 12px;
  margin-top: 18px;
}

.toolbar-filter {
  min-width: 0;
}

.toolbar-filter--keyword {
  width: 100%;
}

.asset-name-card {
  display: grid;
  gap: 7px;
}

.asset-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.asset-name {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.asset-code {
  color: #0f766e;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.asset-desc {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
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
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
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
  border: 1px solid #e2e8f0;
  border-radius: 22px;
  background: rgb(255 255 255 / 0.92);
  box-shadow: 0 16px 36px rgb(15 23 42 / 6%);
}

:deep(.dataset-crud .n-data-table-th) {
  background: #f8fafc;
}

:deep(.dataset-crud .n-data-table-tr:hover td) {
  background: #fbfdff;
}

:deep(.n-tree .n-tree-node-content) {
  min-height: 38px;
  padding: 0 8px;
  border-radius: 12px;
  transition:
    background 0.18s ease,
    color 0.18s ease;
}

:deep(.n-tree .n-tree-node-content:hover) {
  background: #eef6ff;
}

:deep(.n-tree .n-tree-node--selected > .n-tree-node-content) {
  background: #e0f2fe;
}

:global(.data-dataset-edit-form) {
  padding: 8px 4px 4px;
}

:global(.data-dataset-edit-form .n-form-item:has(.step-shell)),
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

:global(.data-dataset-edit-form .n-form-item:has(.step-shell) .n-form-item-blank) {
  width: 100% !important;
  max-width: none !important;
}

:global(.data-dataset-edit-form .n-form-item) {
  margin-bottom: 10px;
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: linear-gradient(180deg, #fff 0%, #fcfdff 100%);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

:global(.data-dataset-edit-form .n-form-item:hover) {
  border-color: #cbd5e1;
  box-shadow: 0 10px 24px rgb(15 23 42 / 6%);
  transform: translateY(-1px);
}

:global(.data-dataset-edit-form .n-form-item-blank) {
  width: 100%;
}

:global(.data-dataset-edit-form .n-form-item-label) {
  min-height: 20px;
  margin-bottom: 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 1.4;
}

:global(.data-dataset-edit-form .dataset-form-divider) {
  margin: 14px 0 10px;
  color: #64748b;
}

:global(.data-dataset-edit-form .dataset-form-divider::before),
:global(.data-dataset-edit-form .dataset-form-divider::after) {
  border-top-color: #dbe3ef;
}

:global(.data-dataset-edit-form .dataset-form-divider .n-divider__title) {
  color: #0f172a;
  font-size: 15px;
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
  gap: 16px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
}

:global(.data-dataset-edit-form .n-radio-group .n-space) {
  gap: 8px 18px !important;
}

:global(.data-dataset-edit-form .dataset-context-panel) {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.95fr);
  gap: 18px;
  padding: 18px 20px;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  border: 1px solid #dbe3ef;
  border-radius: 18px;
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
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

:global(.data-dataset-edit-form .context-panel__title) {
  margin-top: 8px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.35;
}

:global(.data-dataset-edit-form .context-panel__desc) {
  margin-top: 8px;
  color: #475569;
  font-size: 13px;
  line-height: 1.8;
}

:global(.data-dataset-edit-form .context-panel__facts) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-content: start;
}

:global(.data-dataset-edit-form .context-fact) {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

:global(.data-dataset-edit-form .context-fact--wide) {
  grid-column: 1 / -1;
}

:global(.data-dataset-edit-form .context-fact span) {
  color: #64748b;
  font-size: 12px;
}

:global(.data-dataset-edit-form .context-fact strong) {
  color: #0f172a;
  font-size: 14px;
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
  gap: 16px;
  padding: 18px 20px;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  border: 1px solid #dbe3ef;
  border-radius: 18px;
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
  gap: 12px;
}

:global(.data-dataset-edit-form .permission-fact) {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

:global(.data-dataset-edit-form .permission-fact span) {
  color: #64748b;
  font-size: 12px;
}

:global(.data-dataset-edit-form .permission-fact strong) {
  overflow: hidden;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.data-dataset-edit-form .acl-editor) {
  padding: 14px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
}

:global(.data-dataset-edit-form .acl-editor__toolbar) {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

:global(.data-dataset-edit-form .acl-editor__title) {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

:global(.data-dataset-edit-form .acl-editor__hint) {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

:global(.data-dataset-edit-form .acl-rows) {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

:global(.data-dataset-edit-form .acl-row) {
  display: grid;
  grid-template-columns: minmax(120px, 0.55fr) minmax(260px, 1.4fr) minmax(150px, 0.7fr) 42px;
  gap: 10px;
  align-items: center;
}

:global(.data-dataset-edit-form .row-scope-alert) {
  border-radius: 14px;
}

:global(.data-dataset-edit-form .row-scope-grid) {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  transition: opacity 0.18s ease;
}

:global(.data-dataset-edit-form .row-scope-grid.is-disabled) {
  opacity: 0.72;
}

:global(.data-dataset-edit-form .row-scope-field) {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

:global(.data-dataset-edit-form .row-scope-field label) {
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.4;
}

:global(.data-dataset-edit-form .row-scope-field--wide) {
  grid-column: 1 / -1;
}

:global(.data-dataset-edit-form .row-scope-field--action .n-button) {
  width: 100%;
}

@media (max-width: 1400px) {
  .studio-hero {
    grid-template-columns: 1fr;
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
    padding: 14px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
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

  :global(.data-dataset-edit-form .permission-panel__header),
  :global(.data-dataset-edit-form .acl-editor__toolbar) {
    flex-direction: column;
  }

  :global(.data-dataset-edit-form .permission-facts),
  :global(.data-dataset-edit-form .row-scope-grid) {
    grid-template-columns: 1fr;
  }

  :global(.data-dataset-edit-form .acl-row) {
    grid-template-columns: 1fr;
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

  :global(.data-dataset-edit-form .sql-preview-action) {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
