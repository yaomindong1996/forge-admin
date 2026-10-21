/** Local components for GridBlockRenderer. */
import { defineAsyncComponent, h } from 'vue'
import { ChevronBackOutline, CubeOutline } from '@vicons/ionicons5'
import { DesignerGridRenderer } from '@/components/lowcode-builder/designer-core'
import IconRenderer from '@/components/IconRenderer.vue'
import FieldValueRenderer from '@/components/lowcode-builder/shared/FieldValueRenderer.vue'
import InlineRichText from '@/components/lowcode-builder/shared/InlineRichText.vue'
import PageWidgetRenderer from '@/components/lowcode-builder/shared/PageWidgetRenderer.vue'
import ContainerContextMenu from '@/components/lowcode-builder/shared/ContainerContextMenu.vue'

/** 纯 HTML 骨架，避免在 localComponents 顶层 import naive-ui 拖垮单测环境 */
const GridBlockAsyncSkeleton = {
  name: 'GridBlockAsyncSkeleton',
  render() {
    return h('div', {
      class: 'grid-block-async-skeleton',
      style: 'display:grid;gap:8px;padding:8px 0;',
      'aria-busy': 'true',
    }, [
      h('div', { style: 'height:28px;border-radius:4px;background:var(--bg-tertiary,#f0f2f5);' }),
      h('div', { style: 'height:12px;border-radius:4px;background:var(--bg-tertiary,#f0f2f5);' }),
      h('div', { style: 'height:12px;width:72%;border-radius:4px;background:var(--bg-tertiary,#f0f2f5);' }),
    ])
  },
}

/** 区块级重型组件：异步加载，避免自由布局首屏被 AiCrudPage 整树拖死 */
function blockAsyncLoader(loader) {
  return defineAsyncComponent({
    loader,
    delay: 0,
    loadingComponent: GridBlockAsyncSkeleton,
  })
}

const AiCrudPage = blockAsyncLoader(() => import('@/components/ai-form/AiCrudPage.vue'))
const AiForm = blockAsyncLoader(() => import('@/components/ai-form/AiForm.vue'))
const AiTable = blockAsyncLoader(() => import('@/components/ai-form/AiTable.vue'))
const SignaturePad = blockAsyncLoader(() => import('@/components/flow/SignaturePad.vue'))
const QRCodeVue3 = blockAsyncLoader(() => import('qrcode-vue3'))
const WorkspaceSummaryMetrics = blockAsyncLoader(() => import('@/views/workspace/WorkspaceSummaryMetrics.vue'))
const TreeCrudTemplate = blockAsyncLoader(() => import('@/components/page-templates/TreeCrudTemplate.vue'))
const RuntimeListGridFlow = blockAsyncLoader(() => import('@/components/lowcode-builder/page/RuntimeListGridFlow.vue'))
const QuickEntryBlock = blockAsyncLoader(() => import('@/components/lowcode-builder/page/QuickEntryBlock.vue'))
const PageChartBlock = blockAsyncLoader(() => import('@/components/lowcode-builder/page/PageChartBlock.vue'))
const StatsStripBlock = blockAsyncLoader(() => import('@/components/lowcode-builder/page/StatsStripBlock.vue'))
const InfoPanelBlock = blockAsyncLoader(() => import('@/components/lowcode-builder/page/InfoPanelBlock.vue'))

export const gridBlockRendererLocalComponents = {
  ChevronBackOutline,
  CubeOutline,
  QRCodeVue3,
  DesignerGridRenderer,
  AiForm,
  AiTable,
  SignaturePad,
  IconRenderer,
  FieldValueRenderer,
  InlineRichText,
  PageWidgetRenderer,
  ContainerContextMenu,
  WorkspaceSummaryMetrics,
  QuickEntryBlock,
  PageChartBlock,
  StatsStripBlock,
  InfoPanelBlock,
  AiCrudPage,
  TreeCrudTemplate,
  RuntimeListGridFlow,
}
