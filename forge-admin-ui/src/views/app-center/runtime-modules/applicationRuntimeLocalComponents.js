/** Components registered on application-runtime Options shell (script-setup auto-import parity). */
import { NIcon } from 'naive-ui'
import { defineAsyncComponent } from 'vue'
import draggable from 'vuedraggable'
import {
  AddOutline,
  AppsOutline,
  ArrowBackOutline,
  ArrowDownOutline,
  ArrowRedoOutline,
  ArrowUndoOutline,
  ArrowUpOutline,
  BarChartOutline,
  CheckboxOutline,
  CheckmarkDoneOutline,
  ChevronBackOutline,
  ChevronForwardOutline,
  ColorFillOutline,
  CopyOutline,
  CreateOutline,
  CubeOutline,
  DocumentTextOutline,
  DuplicateOutline,
  EllipsisHorizontalOutline,
  ExpandOutline,
  EyeOffOutline,
  EyeOutline,
  DesktopOutline,
  FlashOutline,
  FolderOpenOutline,
  FunnelOutline,
  GitBranchOutline,
  GitNetworkOutline,
  GridOutline,
  InformationCircleOutline,
  LinkOutline,
  ListOutline,
  MoveOutline,
  NotificationsOutline,
  PaperPlaneOutline,
  PeopleOutline,
  PlayOutline,
  ReaderOutline,
  RemoveOutline,
  ResizeOutline,
  SaveOutline,
  SettingsOutline,
  SquareOutline,
  StatsChartOutline,
  SwapHorizontalOutline,
  TextOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import AuthImage from '@/components/common/AuthImage.vue'
import IconRenderer from '@/components/IconRenderer.vue'
import ApplicationDesignerResourceTree from '@/views/app-center/components/ApplicationDesignerResourceTree.vue'
import DesignerAsyncLoader from '@/views/app-center/components/designer/DesignerAsyncLoader.vue'
import PageSystemMenuMountDialog from '@/views/app-center/components/designer/PageSystemMenuMountDialog.vue'
import ApplicationRuntimeSkeleton from '@/views/app-center/components/portal/ApplicationRuntimeSkeleton.vue'
import PortalPageRenderer from '@/views/app-center/components/portal/PortalPageRenderer.vue'
import SystemPageContentSkeleton from '@/views/app-center/components/portal/SystemPageContentSkeleton.vue'

/** Options-API shells only resolve tags via `components:{}` — returning async comps from setup is not enough. */
const asyncPanelLoader = {
  delay: 0,
  loadingComponent: DesignerAsyncLoader,
}

export const ApplicationObjectsPanel = defineAsyncComponent({
  ...asyncPanelLoader,
  loader: () => import('../application-workspace/ApplicationObjectsPanel.vue'),
})
export const ApplicationExtensionsPanel = defineAsyncComponent({
  ...asyncPanelLoader,
  loader: () => import('../application-workspace/ApplicationExtensionsPanel.vue'),
})
export const ApplicationProcessPanel = defineAsyncComponent({
  ...asyncPanelLoader,
  loader: () => import('../application-workspace/ApplicationProcessPanel.vue'),
})
export const ApplicationSettingsPanel = defineAsyncComponent({
  ...asyncPanelLoader,
  loader: () => import('../components/ApplicationSettingsPanel.vue'),
})
export const BusinessObjectDesignerPage = defineAsyncComponent({
  ...asyncPanelLoader,
  loader: () => import('../object-designer.[objectCode].vue'),
})
export const BusinessProcessPage = defineAsyncComponent({
  ...asyncPanelLoader,
  loader: () => import('../business-process.[processId].vue'),
})
export const ForgeFormDesigner = defineAsyncComponent({
  ...asyncPanelLoader,
  loader: () => import('@/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue'),
})
export const ListPageGridDesigner = defineAsyncComponent({
  ...asyncPanelLoader,
  loader: () => import('@/components/lowcode-builder/page/ListPageGridDesigner.vue'),
})
export const IconSelector = defineAsyncComponent({
  ...asyncPanelLoader,
  loader: () => import('@/components/IconSelector.vue'),
})
export const PageTypeSelector = defineAsyncComponent({
  ...asyncPanelLoader,
  loader: () => import('@/views/app-center/components/designer/PageTypeSelector.vue'),
})
export const PageDesignSettingsPanel = defineAsyncComponent({
  ...asyncPanelLoader,
  loader: () => import('@/views/app-center/components/designer/PageDesignSettingsPanel.vue'),
})
export const PageDesignPublishPanel = defineAsyncComponent({
  ...asyncPanelLoader,
  loader: () => import('@/views/app-center/components/designer/PageDesignPublishPanel.vue'),
})
export const PageManagementSystemView = defineAsyncComponent({
  delay: 0,
  loadingComponent: SystemPageContentSkeleton,
  loader: () => import('@/views/app-center/components/portal/PageManagementSystemView.vue'),
})
export const GridBlockRenderer = defineAsyncComponent({
  delay: 0,
  loadingComponent: DesignerAsyncLoader,
  loader: () => import('@/components/lowcode-builder/page/GridBlockRenderer.vue'),
})
export const FreeLayoutPresetThumb = defineAsyncComponent({
  delay: 0,
  loader: () => import('@/views/app-center/in-app-builder/FreeLayoutPresetThumb.vue'),
})

export const applicationRuntimeLocalComponents = {
  NIcon,
  draggable,
  AddOutline,
  AppsOutline,
  ArrowBackOutline,
  ArrowDownOutline,
  ArrowRedoOutline,
  ArrowUndoOutline,
  ArrowUpOutline,
  BarChartOutline,
  CheckboxOutline,
  CheckmarkDoneOutline,
  ChevronBackOutline,
  ChevronForwardOutline,
  ColorFillOutline,
  CopyOutline,
  CreateOutline,
  CubeOutline,
  DocumentTextOutline,
  DuplicateOutline,
  EllipsisHorizontalOutline,
  ExpandOutline,
  EyeOffOutline,
  EyeOutline,
  DesktopOutline,
  FlashOutline,
  FolderOpenOutline,
  FunnelOutline,
  GitBranchOutline,
  GitNetworkOutline,
  GridOutline,
  InformationCircleOutline,
  LinkOutline,
  ListOutline,
  MoveOutline,
  NotificationsOutline,
  PaperPlaneOutline,
  PeopleOutline,
  PlayOutline,
  ReaderOutline,
  RemoveOutline,
  ResizeOutline,
  SaveOutline,
  SettingsOutline,
  SquareOutline,
  StatsChartOutline,
  SwapHorizontalOutline,
  TextOutline,
  TrashOutline,
  AuthImage,
  IconRenderer,
  ApplicationDesignerResourceTree,
  DesignerAsyncLoader,
  PageSystemMenuMountDialog,
  ApplicationRuntimeSkeleton,
  PortalPageRenderer,
  ApplicationObjectsPanel,
  ApplicationExtensionsPanel,
  ApplicationProcessPanel,
  ApplicationSettingsPanel,
  BusinessObjectDesignerPage,
  BusinessProcessPage,
  ForgeFormDesigner,
  ListPageGridDesigner,
  IconSelector,
  PageTypeSelector,
  PageDesignSettingsPanel,
  PageDesignPublishPanel,
  PageManagementSystemView,
  GridBlockRenderer,
  FreeLayoutPresetThumb,
}
