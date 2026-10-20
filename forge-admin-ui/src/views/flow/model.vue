<template>
  <div class="flow-page">
    <!-- 查询与操作区 -->
    <FlowModelToolbar
      v-model:model-name="queryParams.modelName" v-model:category="queryParams.category" v-model:status="queryParams.status"
      :category-options="categoryTreeOptions" :status-options="statusOptions" :loading="loading"
      :sort-mode="sortMode" :sort-saving="sortSaving" :can-sort="dataSource.length > 1"
      @search="handleSearch" @reset="handleReset" @add="handleAdd" @sort="sortMode = true"
      @save-sort="saveModelOrder" @cancel-sort="cancelModelOrder"
    />

    <!-- 卡片列表、局部反馈与固定分页 -->
    <section class="model-workbench" :aria-busy="loading">
      <div class="model-list-context" aria-live="polite">
        <span v-if="hasLoaded">{{ hasAppliedFilters ? '匹配' : '共' }} {{ pagination.itemCount }} 个流程模型</span>
        <span v-else>流程模型</span>
        <span v-if="sortMode">正在调整本页顺序，保存后生效</span>
      </div>
      <div v-if="loadError && dataSource.length" class="model-list-notice">
        <n-alert type="error" :show-icon="false">
          {{ loadError }}
          <n-button text type="primary" :loading="loading" @click="retryQuery">
            重试查询
          </n-button>
        </n-alert>
      </div>
      <div v-else-if="bindingLoadError" class="model-list-notice">
        <n-alert type="warning" :show-icon="false">
          业务绑定加载失败，模型列表仍可查看。
          <n-button text type="primary" :loading="loading" @click="fetchData">
            重试关联
          </n-button>
        </n-alert>
      </div>
      <n-spin :show="loading && dataSource.length > 0" class="model-list-spin">
        <div class="model-list-body">
          <div v-if="dataSource.length" class="model-grid">
            <FlowModelCard
              v-for="item in dataSource" :key="item.id" :item="item"
              :status-label="getLabel('flow_model_status', item.status)" :status-class="statusClass(item.status)"
              :designer-label="designerTypeLabel(item.designerType)" :designer-class="designerTypeClass(item.designerType)"
              :category-label="getCategoryDisplayName(item)" :binding-label="formatBusinessBindings(item)"
              :publication-options="dict.flow_model_publication_state || []"
              :update-label="formatDate(item.updateTime)" :action-options="getActionOptions(item)" :sort-mode="sortMode"
              :deploy-disabled="isModelActionLocked(item, 'deploy')" :busy="isModelActionBusy(item)"
              @design="handleDesign" @deploy="handleDeploy" @instances="handleViewInstances" @action="handleActionSelect"
              @drag-start="handleDragStart" @drop="handleDrop"
            />
          </div>
          <div v-else-if="loading" class="model-grid" aria-label="正在加载流程模型">
            <div v-for="index in 6" :key="index" class="model-skeleton">
              <n-skeleton text width="65%" />
              <n-skeleton text width="40%" />
              <n-skeleton text :repeat="2" />
            </div>
          </div>
          <n-result v-else-if="loadError" status="error" title="模型加载失败" :description="loadError" class="empty-state">
            <template #footer>
              <n-button @click="retryQuery">
                重试查询
              </n-button>
            </template>
          </n-result>
          <IllustratedEmpty
            v-else artwork="workflow" class="empty-state"
            :description="hasAppliedFilters ? '没有匹配的流程模型，试试调整查询条件' : '暂无流程模型，创建后即可开始设计'"
          >
            <template #extra>
              <n-button v-if="hasAppliedFilters" @click="handleReset">
                重置筛选
              </n-button>
              <n-button v-else type="primary" @click="handleAdd">
                新增模型
              </n-button>
            </template>
          </IllustratedEmpty>
        </div>
      </n-spin>
      <div v-if="pagination.itemCount > 0" class="pagination-wrapper">
        <n-pagination
          v-model:page="pagination.page" v-model:page-size="pagination.pageSize" :item-count="pagination.itemCount"
          :page-sizes="[12, 24, 48]" show-size-picker :disabled="sortMode || sortSaving || loading"
          @update:page="fetchData" @update:page-size="handlePageSizeChange"
        />
      </div>
    </section>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
      <NModal
        v-model:show="showModal"
        preset="card"
        :title="modalTitle"
        style="width: min(760px, calc(100vw - 32px))"
        :mask-closable="false"
      >
        <n-form
          :ref="bindFormRef"
          :model="formData"
          :rules="rules"
          label-placement="left"
          label-width="100"
        >
          <n-grid :cols="2" :x-gap="16">
            <n-form-item-gi label="流程模式" path="designerType" :span="2">
              <div class="designer-type-chooser" :class="{ disabled: isEdit }">
                <button
                  v-for="option in designerTypeOptions"
                  :key="option.value"
                  type="button"
                  class="designer-type-option"
                  :class="{ active: formData.designerType === option.value }"
                  :disabled="isEdit"
                  @click="formData.designerType = option.value"
                >
                  <span class="designer-type-icon">
                    <i :class="option.icon" />
                  </span>
                  <span class="designer-type-main">
                    <span class="designer-type-title">{{ option.label }}</span>
                    <span class="designer-type-desc">{{ option.desc }}</span>
                  </span>
                </button>
              </div>
            </n-form-item-gi>
            <n-form-item-gi label="模型名称" path="modelName" :span="2">
              <n-input v-model:value="formData.modelName" placeholder="请输入模型名称" />
            </n-form-item-gi>
            <n-form-item-gi label="模型Key" path="modelKey" :span="2" class="model-key-form-item">
              <n-input
                v-model:value="formData.modelKey"
                placeholder="请输入有意义的模型Key，留空自动生成"
                :disabled="isEdit && [1, 2].includes(Number(formData.status))"
              />
              <div class="model-key-help">
                以字母开头，只能包含字母、数字、下划线或短横线；已发布或挂起模型不能修改。
              </div>
            </n-form-item-gi>
            <n-form-item-gi label="流程分类" path="category" :span="2">
              <NTreeSelect
                v-model:value="formData.category"
                placeholder="请选择分类"
                :options="categoryTreeOptions"
                :default-expand-all="true"
              />
            </n-form-item-gi>
            <n-form-item-gi label="描述" path="description" :span="2">
              <n-input
                v-model:value="formData.description"
                type="textarea"
                placeholder="请输入描述"
                :rows="3"
              />
            </n-form-item-gi>
          </n-grid>
        </n-form>
        <template #footer>
          <n-space justify="end">
            <n-button @click="showModal = false">
              取消
            </n-button>
            <n-button type="primary" :loading="submitLoading" @click="handleSubmit">
              确定
            </n-button>
          </n-space>
        </template>
      </NModal>
    </Teleport>

    <Teleport to="body">
      <NModal
        v-model:show="showStartTestModal"
        preset="card"
        :title="startTestTitle"
        style="width: min(760px, calc(100vw - 32px))"
        content-style="max-height: calc(100vh - 180px); overflow: auto;"
        :mask-closable="false"
      >
        <div class="start-test-modal">
          <div class="start-test-summary">
            <div class="summary-icon">
              <i class="i-material-symbols:play-circle-outline" />
            </div>
            <div class="summary-main">
              <div class="summary-title">
                {{ currentStartModel?.modelName || '-' }}
              </div>
              <div class="summary-subtitle">
                {{ currentStartModel?.modelKey || '-' }}
              </div>
            </div>
            <n-tag type="info" size="small" :bordered="false">
              测试发起
            </n-tag>
          </div>

          <n-alert v-if="startTestAlert" :type="startTestAlert.type" :show-icon="false" class="start-test-alert">
            {{ startTestAlert.text }}
          </n-alert>
          <n-alert
            v-if="startTestPreflightDiagnostics.length"
            type="warning"
            :show-icon="true"
            class="start-test-alert"
          >
            <div>启动前审批人预检发现配置问题：</div>
            <div v-for="diagnostic in startTestPreflightDiagnostics" :key="diagnostic">
              {{ diagnostic }}
            </div>
          </n-alert>

          <div v-if="startTestBusinessFormLoading" class="start-test-form-loading">
            <n-spin size="small" />
            <span>正在加载业务应用表单...</span>
          </div>
          <AiForm
            v-else-if="startTestBusinessFormActive && startTestFormSchema.length"
            :ref="bindStartTestFormRef"
            v-model:value="startTestFormData"
            :schema="startTestFormSchema"
            :grid-cols="startTestBusinessFormLayout.gridCols"
            :label-placement="startTestBusinessFormLayout.labelPlacement"
            :label-width="startTestBusinessFormLayout.labelWidth"
            :show-actions="false"
            :show-feedback="true"
            :context="{ formAssets: startTestBusinessFormAssets }"
            :form-assets="startTestBusinessFormAssets"
          />
          <FlowFormCreateRenderer
            v-else-if="showStartTestModal && startTestFormSchema.length"
            :ref="bindStartTestFormRef"
            v-model="startTestFormData"
            :schema="startTestFormSchema"
          />
          <n-empty
            v-else
            size="small"
            :description="startTestBusinessFormActive
              ? '当前业务应用表单没有可渲染字段，将以空变量发起测试流程'
              : '当前模型没有可渲染的动态表单，将以空变量发起测试流程'"
          />
          <div v-if="startTestApproverNodes.length" class="start-test-approver-section">
            <div class="start-test-approver-title">
              发起人自选审批人
            </div>
            <div class="start-test-approver-tip">
              请为流程设计中标记为“发起人自选”的节点选择审批人。
            </div>
            <n-form label-placement="top">
              <n-form-item
                v-for="node in startTestApproverNodes"
                :key="node.nodeKey"
                :label="node.nodeName || node.nodeKey"
                required
              >
                <UserSelectPicker
                  v-model="startTestApproverSelections[node.nodeKey]"
                  v-model:label-value="startTestApproverLabels[node.nodeKey]"
                  :multiple="node.multiple !== false"
                  :title="`选择${node.nodeName || node.nodeKey}审批人`"
                  :placeholder="node.multiple === false ? '请选择一名审批人' : '请选择一名或多名审批人'"
                />
              </n-form-item>
            </n-form>
          </div>
        </div>

        <template #footer>
          <n-space justify="end">
            <n-button @click="showStartTestModal = false">
              取消
            </n-button>
            <n-button secondary @click="handleViewStarted">
              我发起的
            </n-button>
            <n-button type="primary" :loading="startTestLoading" @click="handleSubmitStartTest">
              发起测试
            </n-button>
          </n-space>
        </template>
      </NModal>
    </Teleport>

    <Teleport to="body">
      <NModal
        v-model:show="showDesignModal"
        :mask-closable="false"
        :close-on-esc="false"
        display-directive="if"
        class="flow-design-modal"
        style="width: 100vw; height: 100vh; max-width: none; margin: 0;"
      >
        <div class="flow-design-modal-shell">
          <FlowDesignPage
            v-if="currentDesignModelId"
            embedded
            :model-id="currentDesignModelId"
            :business-object-code="currentDesignBinding?.objectCode || ''"
            :business-object-name="currentDesignBinding?.objectName || ''"
            :application-id="currentDesignBinding?.applicationId || ''"
            :business-entry-route="currentDesignBinding?.entryRoute || ''"
            :code-app="isCodeAppBinding(currentDesignBinding)"
            @close="handleDesignModalClose"
            @saved="fetchData"
            @deployed="fetchData"
          />
        </div>
      </NModal>
    </Teleport>

    <VersionHistory
      v-if="showVersionHistory"
      :model-id="currentModelId"
      :current-version="currentModelVersion"
      @close="showVersionHistory = false"
      @refresh="fetchData"
    />
  </div>
</template>

<script>
import { useFlowModel } from './composables/useFlowModel'
import { flowModelLocalComponents } from './flowModelLocalComponents'

export default {
  name: 'FlowModel',
  components: {
    ...flowModelLocalComponents,
  },
  setup() {
    const model = useFlowModel()
    // Options API 壳用回调显式连接 composable 的表单引用，保留原有校验入口。
    return {
      ...model,
      bindFormRef: (instance) => { model.formRef.value = instance },
      bindStartTestFormRef: (instance) => { model.startTestFormRef.value = instance },
    }
  },
}
</script>

<style scoped src="./flowModel.css"></style>
