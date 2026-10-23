/** Components for businessListDesigner Options shell. */
import { defineAsyncComponent } from 'vue'
import { NIcon } from 'naive-ui'
import {
  AddOutline,
  ArrowRedoOutline,
  ArrowUndoOutline,
  ChevronDownOutline,
  CloseCircleOutline,
  CloseOutline,
  CopyOutline,
  DocumentTextOutline,
  EllipsisHorizontalOutline,
  FunnelOutline,
  GridOutline,
  LayersOutline,
  ListOutline,
  PrintOutline,
  RefreshOutline,
  SparklesOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import ListPageGridDesigner from '@/components/lowcode-builder/page/ListPageGridDesigner.vue'

const RuntimeListGridFlow = defineAsyncComponent(() =>
  import('@/components/lowcode-builder/page/RuntimeListGridFlow.vue'),
)

export const businessListDesignerLocalComponents = {
  NIcon,
  AddOutline,
  ArrowRedoOutline,
  ArrowUndoOutline,
  ChevronDownOutline,
  CloseCircleOutline,
  CloseOutline,
  CopyOutline,
  DocumentTextOutline,
  EllipsisHorizontalOutline,
  FunnelOutline,
  GridOutline,
  LayersOutline,
  ListOutline,
  PrintOutline,
  RefreshOutline,
  SparklesOutline,
  TrashOutline,
  ListPageGridDesigner,
  RuntimeListGridFlow,
}
