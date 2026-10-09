/** Components for flowModel Options shell. */
import {
  CopyOutline,
  CreateOutline,
  PauseCircleOutline,
  PlayCircleOutline,
  TimeOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { NIcon, NModal, NSpin, NTreeSelect } from 'naive-ui'
import { defineAsyncComponent, h } from 'vue'
import AiForm from '@/components/ai-form/AiForm.vue'
import IllustratedEmpty from '@/components/common/IllustratedEmpty.vue'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import DesignerAsyncLoader from '@/views/app-center/components/designer/DesignerAsyncLoader.vue'
import FlowModelCard from './components/FlowModelCard.vue'

function createAsyncLoader(title, description, { overlay = false } = {}) {
  return {
    name: 'FlowModelAsyncLoader',
    render() {
      return h(DesignerAsyncLoader, {
        title,
        description,
        overlay,
      })
    },
  }
}

// Options API 模板只认 components 注册；setup 返回的异步组件不会自动参与解析。
const FlowDesignPage = defineAsyncComponent({
  loader: () => import('./design.vue'),
  loadingComponent: createAsyncLoader('正在打开流程设计器', '首次进入需要准备 BPMN 与表单设计资源', { overlay: true }),
  delay: 120,
  suspensible: false,
})

const FlowFormCreateRenderer = defineAsyncComponent({
  loader: () => import('@/components/form-create/FlowFormCreateRenderer.vue'),
  loadingComponent: {
    name: 'FlowFormRendererAsyncLoader',
    render() {
      return h('div', { class: 'flow-form-renderer-async-loader', style: 'padding: 24px; display:flex; justify-content:center;' }, [
        h(NSpin, { size: 'medium' }),
      ])
    },
  },
  delay: 120,
  suspensible: false,
})

const VersionHistory = defineAsyncComponent({
  loader: () => import('./version.vue'),
  loadingComponent: createAsyncLoader('正在加载版本历史', '首次打开需要准备流程图查看资源', { overlay: true }),
  delay: 120,
  suspensible: false,
})

export const flowModelLocalComponents = {
  NIcon,
  NModal,
  NTreeSelect,
  CopyOutline,
  CreateOutline,
  PauseCircleOutline,
  PlayCircleOutline,
  TimeOutline,
  TrashOutline,
  AiForm,
  UserSelectPicker,
  IllustratedEmpty,
  FlowModelCard,
  DesignerAsyncLoader,
  FlowDesignPage,
  FlowFormCreateRenderer,
  VersionHistory,
}
