<script setup>
import { defineAsyncComponent, onMounted, ref, watch } from 'vue'
import MasterDetailWorkspace from '@/components/common/MasterDetailWorkspace.vue'
import { useModelResourceStore } from '@/stores/ai/modelResourceStore'
import KnowledgeRetrievalDefaultsPanel from './KnowledgeRetrievalDefaultsPanel.vue'
import ModelImportDialog from './ModelImportDialog.vue'
import ModelResourceList from './ModelResourceList.vue'
import ProviderModelDialogs from './ProviderModelDialogs.vue'
import ProviderRail from './ProviderRail.vue'
import ProviderTemplateSetup from './ProviderTemplateSetup.vue'
import '@/styles/ai-workspace.css'
import '../hub/hub-market.css'
import './resource-console.css'
import './provider-workspace.css'
import './model-market.css'
import './configuration-workspace.css'

const props = defineProps({ initial: { type: String, default: 'models' } })
const StorePage = defineAsyncComponent(() => import('../../store-instance/store.vue'))

const sections = [
  { key: 'models', label: '供应商与模型', icon: 'i-lucide:cpu' },
  { key: 'stores', label: '向量存储', icon: 'i-lucide:database' },
  { key: 'retrieval', label: '检索默认值', icon: 'i-lucide:sliders-horizontal' },
]

const section = ref(sections.some(item => item.key === props.initial) ? props.initial : 'models')
const store = useModelResourceStore()
const modelsInitialized = ref(false)

function ensureModelsInitialized() {
  if (modelsInitialized.value)
    return
  modelsInitialized.value = true
  void store.initialize()
}

watch(() => props.initial, (value) => {
  if (sections.some(item => item.key === value))
    section.value = value
})

watch(section, (value) => {
  if (value === 'models')
    ensureModelsInitialized()
})

onMounted(() => {
  if (section.value === 'models')
    ensureModelsInitialized()
})
</script>

<template>
  <main class="ai-workspace configuration-workspace" aria-label="模型与服务配置">
    <nav class="configuration-navigation" aria-label="配置分区">
      <button
        v-for="item in sections"
        :key="item.key"
        type="button"
        :class="{ active: section === item.key }"
        :aria-current="section === item.key ? 'page' : undefined"
        @click="section = item.key"
      >
        <i :class="item.icon" aria-hidden="true" />
        <span>{{ item.label }}</span>
      </button>
    </nav>

    <section class="configuration-content">
      <div
        v-if="section === 'models'"
        class="ai-resource-console provider-model-console"
        :class="{ 'is-provider-browser': store.compactPanel === 'providers' }"
      >
        <MasterDetailWorkspace class="model-console" :aside-width="264" :min-aside-width="220" :max-aside-width="340">
          <template #aside>
            <ProviderRail />
          </template>
          <template #main>
            <ProviderTemplateSetup v-if="store.selectedTemplate && !store.selectedProvider" />
            <ModelResourceList v-else />
          </template>
        </MasterDetailWorkspace>
        <ProviderModelDialogs />
        <ModelImportDialog />
      </div>

      <div v-else-if="section === 'stores'" class="configuration-content__panel">
        <StorePage />
      </div>

      <div v-else-if="section === 'retrieval'" class="configuration-content__panel is-scroll">
        <KnowledgeRetrievalDefaultsPanel />
      </div>
    </section>
  </main>
</template>

<style scoped>
.configuration-content__panel {
  flex: 1;
  min-height: 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.configuration-content__panel.is-scroll {
  overflow: auto;
}
</style>
