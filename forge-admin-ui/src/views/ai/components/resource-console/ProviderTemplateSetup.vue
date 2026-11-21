<template>
  <div class="provider-setup resource-scroll">
    <NSpin :show="providerLoading" class="provider-setup__spin">
      <ResourceEmpty v-if="providerError" title="连接加载失败" :description="providerError" icon="i-lucide:circle-alert">
        <NButton @click="store.loadProviders">
          重新加载连接
        </NButton>
      </ResourceEmpty>
      <section v-else-if="!providerLoading" class="provider-setup__content">
        <ProviderMark :provider="selectedTemplate" />
        <h2>{{ providerSearch.name ? '没有匹配的连接' : `接入 ${selectedTemplate.name}` }}</h2>
        <p>{{ providerSearch.name ? '清除连接搜索，或新建一个连接。' : '先保存供应商连接，再添加或导入该连接可用的模型。' }}</p>
        <dl class="provider-setup__details">
          <dt>连接协议</dt>
          <dd><DictTag dict-type="ai_provider_adapter_type" :value="selectedTemplate.adapterCode" size="small" /></dd>
          <dt>预设地址</dt>
          <dd><code>{{ selectedTemplate.baseUrl || '需填写账号对应的服务地址' }}</code></dd>
          <dt>接入说明</dt>
          <dd>{{ selectedTemplate.connectionHint || '填写实际服务地址与密钥，以供应商支持的接口为准。' }}</dd>
        </dl>
        <div class="provider-setup__actions">
          <NButton v-if="providerSearch.name" @click="store.handleResetProviderSearch">
            清除搜索
          </NButton>
          <NButton type="primary" @click="store.handleAddProvider(selectedTemplate)">
            <template #icon>
              <i class="i-lucide:plug" />
            </template>
            配置供应商
          </NButton>
          <a v-if="selectedTemplate.documentationUrl" :href="selectedTemplate.documentationUrl" target="_blank" rel="noopener noreferrer">
            接入文档 <i class="i-lucide:external-link" />
          </a>
        </div>
        <p class="provider-setup__note">
          模板不保存密钥，也不会自动测试或调用模型。
        </p>
      </section>
    </NSpin>
  </div>
</template>

<script setup>
import { NButton, NSpin } from 'naive-ui'
import { storeToRefs } from 'pinia'
import DictTag from '@/components/DictTag.vue'
import { useModelResourceStore } from '@/stores/ai/modelResourceStore'
import ProviderMark from './ProviderMark.vue'
import ResourceEmpty from './ResourceEmpty.vue'

const store = useModelResourceStore()
const { selectedTemplate, providerError, providerLoading, providerSearch } = storeToRefs(store)
</script>
