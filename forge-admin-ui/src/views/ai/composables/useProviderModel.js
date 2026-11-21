import { ref } from 'vue'
import { applyProviderModelPart1 } from './useProviderModel.part1.js'

const SERVICE_CONFIG_TABS = [
  { key: 'models', label: '供应商与模型' },
  { key: 'stores', label: '向量存储' },
  { key: 'retrieval', label: '检索默认值' },
]

export function useProviderModel() {
  const api = applyProviderModelPart1()
  const { __impl, mut, ...publicApi } = api

  const configTab = ref('models')
  const templatePickerShow = ref(false)

  function handleOpenTemplatePicker() {
    templatePickerShow.value = true
  }

  function handleApplyTemplate(template) {
    publicApi.providerModal.isEdit = false
    publicApi.providerModal.form = {
      ...publicApi.createProviderForm(),
      providerName: template.name || '',
      providerType: template.providerType || 'openai',
      adapterCode: 'openai_compatible',
      baseUrl: template.defaultBaseUrl || '',
      remark: template.description || '',
    }
    publicApi.providerModal.show = true
  }

  return {
    ...publicApi,
    configTab,
    serviceConfigTabs: SERVICE_CONFIG_TABS,
    templatePickerShow,
    handleOpenTemplatePicker,
    handleApplyTemplate,
  }
}
