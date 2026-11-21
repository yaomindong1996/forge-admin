<template>
  <NModal
    :show="providerModal.show"
    preset="card"
    :title="providerModal.isEdit ? '编辑供应商连接' : '添加供应商连接'"
    :style="modalCardStyle"
    :mask-closable="false"
    @update:show="v => !v && store.requestCloseProvider()"
  >
    <NForm ref="providerFormEl" :model="providerModal.form" :rules="providerRules" label-placement="top">
      <NGrid :cols="2" :x-gap="14">
        <NFormItemGi v-if="!providerModal.isEdit" :span="2" label="供应商预设" path="templateKey">
          <NSelect
            :value="formTemplateKey"
            :options="templateSelectOptions"
            filterable
            placeholder="选择常用厂商自动带入协议与默认地址，或自定义连接"
            @update:value="store.applyProviderTemplate"
          />
        </NFormItemGi>
        <NFormItemGi label="连接名称" path="providerName">
          <NInput v-model:value="providerModal.form.providerName" placeholder="如：生产环境 DeepSeek" />
        </NFormItemGi>
        <NFormItemGi label="连接协议" path="adapterCode">
          <NSelect v-model:value="providerModal.form.adapterCode" :options="providerAdapterOptions" placeholder="选择连接协议" />
        </NFormItemGi>
        <NFormItemGi :span="2" label="服务地址 (Base URL)" path="baseUrl">
          <NInput v-model:value="providerModal.form.baseUrl" placeholder="如：https://api.openai.com/v1" />
        </NFormItemGi>
        <NFormItemGi :span="2" label="API Key" path="apiKey">
          <NInput
            v-model:value="providerModal.form.apiKey"
            type="password"
            show-password-on="click"
            :placeholder="providerApiKeyPlaceholder"
          />
        </NFormItemGi>
        <NFormItemGi label="默认供应商" path="isDefault">
          <div class="provider-default-toggle">
            <NSwitch
              v-model:value="providerModal.form.isDefault"
              checked-value="1"
              unchecked-value="0"
              :disabled="providerModal.isEdit && providerModal.form.isDefault === '1'"
            />
            <span class="provider-default-toggle__hint">
              {{ providerModal.form.isDefault === '1'
                ? '当前已是或保存后设为默认供应商'
                : '开启后保存时自动设为当前租户默认供应商' }}
            </span>
          </div>
        </NFormItemGi>
        <NFormItemGi label="连接状态" path="status">
          <NRadioGroup v-model:value="providerModal.form.status">
            <NRadio v-for="item in statusOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </NRadio>
          </NRadioGroup>
        </NFormItemGi>
        <NFormItemGi :span="2" label="备注" path="remark">
          <NInput v-model:value="providerModal.form.remark" type="textarea" :rows="2" placeholder="可选，说明该连接的用途或账号归属" />
        </NFormItemGi>
      </NGrid>

      <NCollapse class="provider-advanced-collapse">
        <NCollapseItem title="高级选项（品牌分类、默认模型、自定义 Logo）" name="advanced">
          <NGrid :cols="2" :x-gap="14">
            <NFormItemGi label="品牌分类" path="providerType">
              <NSelect v-model:value="providerModal.form.providerType" :options="providerTypeOptions" placeholder="选择品牌分类" />
            </NFormItemGi>
            <NFormItemGi label="连接默认模型标识" path="defaultModel">
              <NInput v-model:value="providerModal.form.defaultModel" placeholder="可选，如 deepseek-chat" />
            </NFormItemGi>
            <NFormItemGi :span="2" label="自定义 Logo" path="logo">
              <div class="resource-logo-upload">
                <ImageUpload
                  v-model="providerModal.form.logo"
                  business-type="ai-provider-logo"
                  :limit="1"
                  :file-size="2"
                  :file-type="['png', 'jpg', 'jpeg', 'svg', 'webp']"
                  value-type="string"
                />
                <span class="resource-logo-hint">未上传时自动使用品牌默认图标。</span>
              </div>
            </NFormItemGi>
          </NGrid>
        </NCollapseItem>
      </NCollapse>
    </NForm>
    <template #action>
      <NSpace justify="end">
        <NButton @click="store.requestCloseProvider">
          取消
        </NButton>
        <NButton type="primary" :loading="providerModal.saving" @click="store.handleSaveProvider">
          保存连接
        </NButton>
      </NSpace>
    </template>
  </NModal>

  <NModal
    :show="modelModal.show"
    preset="card"
    :title="modelModal.isEdit ? '编辑模型' : '添加模型'"
    :style="modalCardStyle"
    :mask-closable="false"
    @update:show="v => !v && store.requestCloseModel()"
  >
    <NForm ref="modelFormEl" :model="modelModal.form" :rules="modelRules" label-placement="top">
      <NGrid :cols="2" :x-gap="14">
        <NFormItemGi :span="2" label="所属供应商连接" path="providerId">
          <NSelect
            v-model:value="modelModal.form.providerId"
            :options="providerSelectOptions"
            filterable
            remote
            placeholder="请选择供应商连接"
            @search="store.searchProviderChoices"
          />
        </NFormItemGi>
        <NFormItemGi label="模型标识 (Model ID)" path="modelId">
          <NInput v-model:value="modelModal.form.modelId" placeholder="调用接口时传递的模型编码，如 deepseek-chat" />
        </NFormItemGi>
        <NFormItemGi label="显示名称" path="modelName">
          <NInput v-model:value="modelModal.form.modelName" placeholder="界面展示名称，如 DeepSeek Chat" />
        </NFormItemGi>
        <NFormItemGi label="模型类型" path="modelType">
          <NSelect v-model:value="modelModal.form.modelType" :options="modelTypeOptions" placeholder="请选择模型类型" />
        </NFormItemGi>
        <NFormItemGi label="最大上下文 Token" path="maxTokens">
          <NInputNumber v-model:value="modelModal.form.maxTokens" :min="0" placeholder="可选，如 128000" style="width: 100%" />
        </NFormItemGi>
        <NFormItemGi label="设为该类型默认模型" path="isDefault">
          <NSwitch v-model:value="modelModal.form.isDefault" checked-value="1" unchecked-value="0" />
        </NFormItemGi>
        <NFormItemGi label="模型状态" path="status">
          <NRadioGroup v-model:value="modelModal.form.status">
            <NRadio v-for="item in statusOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </NRadio>
          </NRadioGroup>
        </NFormItemGi>
        <NFormItemGi :span="2" label="描述" path="description">
          <NInput v-model:value="modelModal.form.description" type="textarea" :rows="2" placeholder="可选，补充模型适用场景或说明" />
        </NFormItemGi>
      </NGrid>
    </NForm>
    <template #action>
      <NSpace justify="end">
        <NButton @click="store.requestCloseModel">
          取消
        </NButton>
        <NButton type="primary" :loading="modelModal.saving" @click="store.handleSaveModel">
          保存模型
        </NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<script setup>
import {
  NButton,
  NCollapse,
  NCollapseItem,
  NForm,
  NFormItemGi,
  NGrid,
  NInput,
  NInputNumber,
  NModal,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  NSwitch,
} from 'naive-ui'
import { storeToRefs } from 'pinia'
import { ref, watch } from 'vue'
import ImageUpload from '@/components/image-upload/index.vue'
import { useModelResourceStore } from '@/stores/ai/modelResourceStore'

const store = useModelResourceStore()
const {
  modalCardStyle,
  providerModal,
  providerRules,
  providerApiKeyPlaceholder,
  formTemplateKey,
  templateSelectOptions,
  providerTypeOptions,
  providerAdapterOptions,
  statusOptions,
  modelModal,
  modelRules,
  modelTypeOptions,
  providerSelectOptions,
} = storeToRefs(store)

const providerFormEl = ref(null)
const modelFormEl = ref(null)

watch(providerFormEl, (el) => {
  store.providerFormRef = el
}, { immediate: true })
watch(modelFormEl, (el) => {
  store.modelFormRef = el
}, { immediate: true })
</script>

<style scoped>
.provider-default-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 34px;
}

.provider-default-toggle__hint {
  color: var(--text-tertiary, #6b7280);
  font-size: 12px;
  line-height: 1.4;
}

.provider-advanced-collapse {
  margin-top: 8px;
}

.resource-logo-upload {
  display: flex;
  align-items: center;
  gap: 12px;
}

.resource-logo-hint {
  color: var(--text-tertiary, #6b7280);
  font-size: 12px;
}
</style>
