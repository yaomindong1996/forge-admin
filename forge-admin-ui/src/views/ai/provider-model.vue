<template>
  <div class="ai-provider-model-page">
    <nav class="service-config-tabs" aria-label="模型与服务配置分区">
      <button
        v-for="tab in serviceConfigTabs"
        :key="tab.key"
        type="button"
        class="service-config-tab"
        :class="{ 'is-active': configTab === tab.key }"
        :aria-current="configTab === tab.key ? 'page' : undefined"
        @click="configTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </nav>

    <main v-if="configTab === 'models'" class="provider-workspace">
      <aside class="provider-list-panel" aria-label="供应商列表">
        <div class="provider-list-panel__header">
          <h2>供应商列表</h2>
          <div class="provider-list-panel__header-actions">
            <NTag size="small" :bordered="false">
              {{ providerPagination.itemCount }} 个
            </NTag>
            <NButton
              secondary
              size="small"
              @click="handleOpenTemplatePicker"
            >
              预设
            </NButton>
            <NButton
              type="primary"
              circle
              size="small"
              aria-label="新增供应商"
              title="新增供应商"
              @click="handleAddProvider"
            >
              <template #icon>
                <i class="ai-icon:plus" aria-hidden="true" />
              </template>
            </NButton>
          </div>
        </div>
        <div class="provider-list-filters">
          <n-input
            v-model:value="providerSearch.name"
            placeholder="搜索供应商名称"
            clearable
            @keyup.enter="handleProviderSearch"
          >
            <template #prefix>
              <i class="ai-icon:search" aria-hidden="true" />
            </template>
          </n-input>
          <div class="provider-list-filters__row">
            <n-select v-model:value="providerSearch.type" placeholder="全部类型" clearable :options="providerTypeOptions" />
            <n-select v-model:value="providerSearch.status" placeholder="全部状态" clearable :options="statusOptions" />
          </div>
          <div class="provider-list-filters__actions">
            <NButton type="primary" size="small" @click="handleProviderSearch">
              查询
            </NButton>
            <NButton size="small" @click="handleResetProviderSearch">
              重置
            </NButton>
          </div>
        </div>

        <n-spin :show="providerLoading">
          <div class="provider-list">
            <button
              v-for="provider in providerList"
              :key="provider.id"
              type="button"
              class="provider-list-item"
              :class="{ 'provider-list-item--selected': selectedProvider?.id === provider.id }"
              :aria-pressed="selectedProvider?.id === provider.id"
              @click="handleSelectProvider(provider)"
            >
              <span class="provider-list-item__avatar">
                <AuthImage
                  v-if="provider.logo"
                  :src="provider.logo"
                  :img-style="providerListAvatarStyle"
                />
                <span v-else>{{ providerInitial(provider) }}</span>
              </span>
              <span class="provider-list-item__content">
                <span class="provider-list-item__title">
                  <strong>{{ provider.providerName }}</strong>
                  <NTag v-if="provider.isDefault === '1'" type="success" size="small" :bordered="false">
                    默认
                  </NTag>
                  <DictTag dict-type="ai_status" :value="provider.status" size="small" />
                </span>
                <span class="provider-list-item__meta">
                  <span>{{ providerModelCount(provider) }} 个模型</span>
                  <span>{{ formatProviderTime(provider.updateTime || provider.createTime) }}</span>
                </span>
              </span>
              <i class="provider-list-item__arrow ai-icon:chevron-right" aria-hidden="true" />
            </button>
            <n-empty v-if="!providerLoading && providerList.length === 0" description="暂无供应商" size="small" />
          </div>
        </n-spin>

        <div class="provider-list-pagination">
          <span>共 {{ providerPagination.itemCount }} 条</span>
          <n-pagination
            :page="providerPagination.pageNum"
            :page-size="providerPagination.pageSize"
            :item-count="providerPagination.itemCount"
            :page-sizes="providerPageSizes"
            show-size-picker
            size="small"
            @update:page="handleProviderPageChange"
            @update:page-size="handleProviderPageSizeChange"
          />
        </div>
      </aside>

      <section class="provider-detail-panel" aria-label="供应商配置">
        <template v-if="selectedProvider">
          <div class="provider-detail-header">
            <div class="provider-detail-header__identity">
              <div class="provider-detail-header__avatar">
                <AuthImage
                  v-if="selectedProvider.logo"
                  :src="selectedProvider.logo"
                  :img-style="providerAvatarStyle"
                />
                <span v-else>{{ providerInitial(selectedProvider) }}</span>
              </div>
              <div>
                <div class="provider-detail-header__title">
                  <h2>{{ selectedProvider.providerName }}</h2>
                  <DictTag dict-type="ai_status" :value="selectedProvider.status" size="small" />
                  <NTag v-if="selectedProvider.isDefault === '1'" type="success" size="small" :bordered="false">
                    默认供应商
                  </NTag>
                </div>
                <div class="provider-detail-header__time">
                  <span>创建时间：{{ formatProviderTime(selectedProvider.createTime) }}</span>
                  <span>更新时间：{{ formatProviderTime(selectedProvider.updateTime) }}</span>
                </div>
              </div>
            </div>
            <div class="provider-detail-header__actions">
              <NButton type="primary" ghost :loading="providerEditing" @click="handleEditProvider(selectedProvider)">
                编辑配置
              </NButton>
              <NButton
                v-if="selectedProvider.isDefault !== '1'"
                text
                class="text-success"
                :loading="providerDefaultUpdatingId === selectedProvider.id"
                @click="handleSetDefault(selectedProvider)"
              >
                设为默认
              </NButton>
              <NPopconfirm @positive-click="handleDeleteProvider(selectedProvider.id)">
                <template #trigger>
                  <NButton text class="text-error">
                    删除
                  </NButton>
                </template>
                确定删除供应商“{{ selectedProvider.providerName }}”吗？
              </NPopconfirm>
              <NButton quaternary circle aria-label="关闭供应商详情" title="关闭" @click="clearSelectedProvider">
                <i class="ai-icon:x" />
              </NButton>
            </div>
          </div>

          <div class="provider-config-content">
            <section class="config-section">
              <div class="config-section__title">
                <i class="ai-icon:settings" aria-hidden="true" />
                <strong>基础信息</strong>
              </div>
              <div class="config-form-grid">
                <div class="config-field">
                  <span>供应商名称</span>
                  <n-input :value="selectedProvider.providerName" readonly />
                </div>
                <div class="config-field">
                  <span>供应商类型</span>
                  <div class="config-field__value">
                    <DictTag dict-type="ai_provider_type" :value="selectedProvider.providerType" size="small" />
                  </div>
                </div>
                <div class="config-field">
                  <span>连接协议</span>
                  <div class="config-field__value">
                    <DictTag dict-type="ai_provider_adapter_type" :value="selectedProvider.adapterCode" size="small" />
                  </div>
                </div>
                <div class="config-field">
                  <span>默认供应商</span>
                  <div class="config-field__value">
                    {{ selectedProvider.isDefault === '1' ? '是' : '否' }}
                  </div>
                </div>
                <div class="config-field config-field--full">
                  <span>Base URL</span>
                  <n-input :value="selectedProvider.baseUrl" readonly />
                </div>
                <div class="config-field config-field--full">
                  <span>备注</span>
                  <n-input :value="selectedProvider.remark || '暂无备注'" type="textarea" :rows="2" readonly />
                </div>
              </div>
            </section>

            <section class="config-section">
              <div class="config-section__title">
                <i class="ai-icon:lock" aria-hidden="true" />
                <strong>API Key / 密钥配置</strong>
              </div>
              <div class="secret-config-row">
                <div class="config-field">
                  <span>API Key</span>
                  <div class="secret-config-input">
                    <n-input
                      :value="selectedProvider.apiKey || '未配置'"
                      type="password"
                      readonly
                      show-password-on="click"
                    />
                    <NButton secondary @click="handleFetchModels">
                      验证连接
                    </NButton>
                  </div>
                  <small>密钥仅展示脱敏值，编辑时留空表示不修改。</small>
                </div>
              </div>
            </section>
          </div>

          <section class="model-section">
            <div class="model-section__heading">
              <i class="ai-icon:apps" aria-hidden="true" />
              <strong>模型管理</strong>
              <NTag size="small" :bordered="false">
                {{ modelPagination.itemCount }} 个
              </NTag>
            </div>
            <div class="model-tab-toolbar">
              <div class="model-tab-toolbar__left">
                <span>默认模型用于连接测试，以及未显式指定模型时的调用。</span>
                <n-select
                  v-model:value="modelSearch.modelType"
                  placeholder="全部类型"
                  clearable
                  :options="modelTypeOptions"
                  size="small"
                  style="width: 130px"
                  @update:value="handleModelSearch"
                />
              </div>
              <div class="model-tab-toolbar__actions">
                <NButton secondary @click="handleFetchModels">
                  <template #icon>
                    <i class="ai-icon:download" />
                  </template>
                  获取模型
                </NButton>
                <NButton type="primary" @click="handleAddModel()">
                  <template #icon>
                    <i class="ai-icon:plus" />
                  </template>
                  新增模型
                </NButton>
              </div>
            </div>
            <n-data-table
              :columns="modelColumns"
              :data="modelList"
              :loading="modelLoading"
              :row-key="row => row.id"
              :scroll-x="1030"
              size="small"
              class="model-table"
            />
            <div class="model-pagination">
              <n-pagination
                :page="modelPagination.pageNum"
                :page-size="modelPagination.pageSize"
                :item-count="modelPagination.itemCount"
                :page-sizes="modelPageSizes"
                :prefix="paginationPrefix"
                show-size-picker
                show-quick-jumper
                size="small"
                @update:page="handleModelPageChange"
                @update:page-size="handleModelPageSizeChange"
              />
            </div>
          </section>
        </template>

        <div v-else class="provider-detail-empty">
          <i class="ai-icon:settings" aria-hidden="true" />
          <h2>请选择供应商</h2>
          <p>从左侧列表选择一个供应商，查看基础配置和模型信息。</p>
        </div>
      </section>
    </main>

    <StoreInstanceView v-else-if="configTab === 'stores'" />
    <KnowledgeRetrievalDefaultsPanel v-else-if="configTab === 'retrieval'" />

    <ProviderTemplatePickerModal v-model:show="templatePickerShow" @select="handleApplyTemplate" />

    <n-modal
      v-model:show="providerModal.show"
      preset="card"
      :title="providerModal.isEdit ? '编辑供应商' : '新增供应商'"
      :style="modalCardStyle"
    >
      <n-form ref="providerFormRef" :model="providerModal.form" :rules="providerRules" label-placement="left" label-width="100">
        <div class="modal-form-grid">
          <n-form-item label="供应商名称" path="providerName">
            <n-input v-model:value="providerModal.form.providerName" placeholder="请输入供应商名称" />
          </n-form-item>
          <n-form-item label="供应商类型" path="providerType">
            <n-select v-model:value="providerModal.form.providerType" placeholder="请选择类型" :options="providerTypeOptions" />
          </n-form-item>
          <n-form-item label="连接协议" path="adapterCode">
            <n-select v-model:value="providerModal.form.adapterCode" placeholder="请选择连接协议" :options="providerAdapterOptions" />
          </n-form-item>
          <n-form-item label="状态" path="status">
            <n-radio-group v-model:value="providerModal.form.status">
              <n-radio v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </n-radio>
            </n-radio-group>
          </n-form-item>
          <n-form-item label="Logo" path="logo">
            <n-upload
              :action="`${uploadPrefix}/system/file/upload`"
              :headers="uploadHeaders"
              :max="1"
              accept=".png,.jpg,.jpeg,.svg,.webp"
              :default-upload="true"
              @finish="handleLogoUploadFinish"
            >
              <div v-if="providerModal.form.logo" class="logo-preview">
                <AuthImage :src="providerModal.form.logo" :img-style="{ width: '64px', height: '64px', borderRadius: '12px', objectFit: 'cover' }" />
                <NButton text size="tiny" type="error" class="logo-remove" aria-label="移除供应商 Logo" @click.stop="providerModal.form.logo = ''">
                  <i class="ai-icon:x" />
                </NButton>
              </div>
              <NButton v-else size="small">
                <i class="ai-icon:upload" /> 上传Logo
              </NButton>
            </n-upload>
          </n-form-item>
          <n-form-item class="form-item--full" label="Base URL" path="baseUrl">
            <n-input v-model:value="providerModal.form.baseUrl" placeholder="留空自动使用默认地址，如 https://dashscope.aliyuncs.com/compatible-mode" />
          </n-form-item>
          <n-form-item class="form-item--full" label="API Key" path="apiKey">
            <div class="form-control-stack">
              <n-input
                v-model:value="providerModal.form.apiKey"
                type="password"
                show-password-on="click"
                :placeholder="providerApiKeyPlaceholder"
              />
              <span v-if="providerModal.isEdit" class="field-tip">已保存密钥不会回显，留空表示保持不变。</span>
            </div>
          </n-form-item>
          <n-form-item class="form-item--full" label="备注" path="remark">
            <n-input v-model:value="providerModal.form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
          </n-form-item>
        </div>
      </n-form>
      <template #action>
        <div class="modal-footer-actions">
          <NButton @click="providerModal.show = false">
            取消
          </NButton>
          <NButton type="primary" :loading="providerModal.saving" @click="handleSaveProvider">
            确定
          </NButton>
        </div>
      </template>
    </n-modal>

    <n-modal v-model:show="modelModal.show" preset="card" :title="modelModal.isEdit ? '编辑模型' : '新增模型'" :style="modalCardStyle">
      <n-form ref="modelFormRef" :model="modelModal.form" :rules="modelRules" label-placement="left" label-width="100">
        <div class="modal-form-grid">
          <n-form-item label="供应商" path="providerId">
            <n-select v-model:value="modelModal.form.providerId" placeholder="请选择供应商" :options="providerSelectOptions" :disabled="modelModal.isEdit" />
          </n-form-item>
          <n-form-item label="模型类型" path="modelType">
            <n-select v-model:value="modelModal.form.modelType" placeholder="请选择模型类型" :options="modelTypeOptions" />
          </n-form-item>
          <n-form-item label="模型标识" path="modelId">
            <n-input v-model:value="modelModal.form.modelId" placeholder="如 gpt-4o" />
          </n-form-item>
          <n-form-item label="模型名称" path="modelName">
            <n-input v-model:value="modelModal.form.modelName" placeholder="如 GPT-4o" />
          </n-form-item>
          <n-form-item label="最大Token" path="maxTokens">
            <n-input-number v-model:value="modelModal.form.maxTokens" placeholder="128000" :min="0" style="width: 100%" />
          </n-form-item>
          <n-form-item label="上下文窗口" path="contextWindow">
            <n-input-number v-model:value="modelModal.form.contextWindow" placeholder="128000" :min="0" style="width: 100%" />
          </n-form-item>
          <n-form-item class="form-item--full" label="模型能力" path="capabilityCodes">
            <n-select v-model:value="modelModal.form.capabilityCodes" multiple clearable :options="modelCapabilityOptions" placeholder="请选择模型实际支持的能力" />
          </n-form-item>
          <n-form-item label="输入价格" path="inputPricePerMillionCent">
            <n-input-number v-model:value="modelModal.form.inputPricePerMillionCent" placeholder="分/百万 Token" :min="0" :precision="0" style="width: 100%" />
          </n-form-item>
          <n-form-item label="输出价格" path="outputPricePerMillionCent">
            <n-input-number v-model:value="modelModal.form.outputPricePerMillionCent" placeholder="分/百万 Token" :min="0" :precision="0" style="width: 100%" />
          </n-form-item>
          <n-form-item label="排序号" path="sortOrder">
            <n-input-number v-model:value="modelModal.form.sortOrder" placeholder="排序值" :min="0" style="width: 100%" />
          </n-form-item>
          <n-form-item label="默认模型" path="isDefault">
            <n-radio-group v-model:value="modelModal.form.isDefault">
              <n-radio v-for="opt in isDefaultOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </n-radio>
            </n-radio-group>
          </n-form-item>
          <n-form-item label="状态" path="status">
            <n-radio-group v-model:value="modelModal.form.status">
              <n-radio v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </n-radio>
            </n-radio-group>
          </n-form-item>
          <n-form-item label="图标" path="icon">
            <n-upload
              :action="`${uploadPrefix}/system/file/upload`"
              :headers="uploadHeaders"
              :max="1"
              accept=".png,.jpg,.jpeg,.svg,.webp"
              :default-upload="true"
              @finish="handleIconUploadFinish"
            >
              <div v-if="modelModal.form.icon" class="logo-preview">
                <AuthImage :src="modelModal.form.icon" :img-style="{ width: '64px', height: '64px', borderRadius: '12px', objectFit: 'cover' }" />
                <NButton text size="tiny" type="error" class="logo-remove" aria-label="移除模型图标" @click.stop="modelModal.form.icon = ''">
                  <i class="ai-icon:x" />
                </NButton>
              </div>
              <NButton v-else size="small">
                <i class="ai-icon:upload" /> 上传图标
              </NButton>
            </n-upload>
          </n-form-item>
          <n-form-item class="form-item--full" label="描述" path="description">
            <n-input v-model:value="modelModal.form.description" type="textarea" :rows="3" placeholder="请输入模型描述" />
          </n-form-item>
        </div>
      </n-form>
      <template #action>
        <div class="modal-footer-actions">
          <NButton @click="modelModal.show = false">
            取消
          </NButton>
          <NButton type="primary" :loading="modelModal.saving" @click="handleSaveModel">
            确定
          </NButton>
        </div>
      </template>
    </n-modal>

    <n-modal
      v-model:show="fetchModelsModal.show"
      preset="card"
      title="获取可用模型"
      :style="fetchModelsModalStyle"
      :content-style="fetchModelsModalContentStyle"
      :mask-closable="false"
    >
      <template v-if="fetchModelsModal.loading">
        <div class="fetch-models-loading">
          <i class="ai-icon:loader animate-spin" aria-hidden="true" />
          <p>正在从供应商拉取可用模型…</p>
        </div>
      </template>

      <template v-else-if="fetchModelsModal.error">
        <n-result
          status="error"
          title="获取模型失败"
          :description="fetchModelsModal.error"
          size="small"
          class="fetch-models-result"
        >
          <template #footer>
            <NButton size="small" @click="handleFetchModels">
              重试
            </NButton>
          </template>
        </n-result>
      </template>

      <template v-else>
        <div class="fetch-models-categories">
          <n-radio-group v-model:value="fetchModelsModalCategory" size="small">
            <n-radio-button v-for="cat in fetchModelsModalCategories" :key="cat.value" :value="cat.value">
              {{ cat.label }}
            </n-radio-button>
          </n-radio-group>
        </div>
        <div class="fetch-models-toolbar">
          <span class="fetch-models-count">发现 {{ fetchModelsModal.models.length }} 个可用模型</span>
          <n-input
            v-model:value="fetchModelsModalSearch"
            size="small"
            clearable
            placeholder="搜索模型 ID"
            class="fetch-models-search"
          />
        </div>
        <div class="fetch-models-list">
          <template v-for="group in fetchModelsModalGroups" :key="group.type">
            <div class="fetch-models-group-title">
              <n-checkbox
                size="small"
                :checked="isGroupChecked(group.type)"
                :indeterminate="isGroupIndeterminate(group.type)"
                @update:checked="checked => handleToggleGroup(group.type, checked)"
              />
              <span class="fetch-models-group-name">{{ group.label }}</span>
              <span class="fetch-models-group-count">{{ group.models.length }}</span>
            </div>
            <div v-for="model in group.models" :key="model.id" class="fetch-models-item">
              <n-checkbox :checked="fetchModelsModal.checked[model.id]" @update:checked="checked => handleToggleModel(model.id, checked)">
                <code class="fetch-models-model-id" :title="model.id">{{ model.id }}</code>
              </n-checkbox>
              <n-popselect
                v-model:value="fetchModelsModal.modelTypes[model.id]"
                :options="modelTypeOverrideOptions"
                trigger="click"
                size="small"
                class="fetch-models-type-select"
              >
                <n-tag size="small" :type="modelTypeTagColor(fetchModelsModal.modelTypes[model.id] || inferModelType(model.id))">
                  {{ modelTypeLabel(fetchModelsModal.modelTypes[model.id] || inferModelType(model.id)) }}
                </n-tag>
              </n-popselect>
            </div>
          </template>
          <n-empty
            v-if="fetchModelsModalGroups.length === 0"
            size="small"
            description="没有匹配的模型"
            style="padding: 24px 0"
          />
        </div>
      </template>
      <template #footer>
        <div class="modal-footer-actions">
          <NButton
            v-if="!fetchModelsModal.loading && !fetchModelsModal.error"
            @click="fetchModelsModal.show = false"
          >
            取消
          </NButton>
          <NButton
            v-if="!fetchModelsModal.loading && !fetchModelsModal.error"
            type="primary"
            :loading="fetchModelsModal.importing"
            :disabled="fetchModelsModalVisibleCheckedCount === 0"
            @click="handleImportModels"
          >
            导入 {{ fetchModelsModalVisibleCheckedCount }} 个模型
          </NButton>
          <NButton v-if="fetchModelsModal.error" @click="fetchModelsModal.show = false">
            关闭
          </NButton>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script>
import { useProviderModel } from './composables/useProviderModel'
import { providerModelLocalComponents } from './providerModelLocalComponents'

export default {
  name: 'ProviderModel',
  components: {
    ...providerModelLocalComponents,
  },
  setup() {
    return useProviderModel()
  },
}
</script>

<style scoped src="./providerModel.css"></style>
