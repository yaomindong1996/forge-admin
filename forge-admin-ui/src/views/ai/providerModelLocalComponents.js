/** Components for providerModel Options shell. */
import { NButton, NPopconfirm, NTag } from 'naive-ui'
import { defineAsyncComponent } from 'vue'
import AuthImage from '@/components/common/AuthImage.vue'
import DictTag from '@/components/DictTag.vue'
import KnowledgeRetrievalDefaultsPanel from './components/resource-console/KnowledgeRetrievalDefaultsPanel.vue'
import ProviderTemplatePickerModal from './components/resource-console/ProviderTemplatePickerModal.vue'

const StoreInstanceView = defineAsyncComponent(() => import('./store-instance/store.vue'))

export const providerModelLocalComponents = {
  NButton,
  NPopconfirm,
  NTag,
  AuthImage,
  DictTag,
  ProviderTemplatePickerModal,
  KnowledgeRetrievalDefaultsPanel,
  StoreInstanceView,
}
