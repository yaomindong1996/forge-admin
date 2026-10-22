/** application-runtime setup part 1. */
import { AddOutline, AppsOutline, ArrowBackOutline, ArrowDownOutline, ArrowRedoOutline, ArrowUndoOutline, ArrowUpOutline, BarChartOutline, CheckboxOutline, CheckmarkDoneOutline, ColorFillOutline, CopyOutline, CreateOutline, CubeOutline, DocumentTextOutline, DuplicateOutline, EllipsisHorizontalOutline, ExpandOutline, EyeOffOutline, EyeOutline, FolderOpenOutline, FunnelOutline, GitBranchOutline, GitNetworkOutline, GridOutline, InformationCircleOutline, ListOutline, MoveOutline, NotificationsOutline, PaperPlaneOutline, PeopleOutline, ReaderOutline, RemoveOutline, ResizeOutline, SaveOutline, SettingsOutline, SquareOutline, StatsChartOutline, SwapHorizontalOutline, TextOutline, TrashOutline } from '@vicons/ionicons5'
import { NIcon, useMessage } from 'naive-ui'
import { computed, h, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import draggable from 'vuedraggable'
import { businessObjectDesigner } from '@/api/business-app'
import { businessApplicationRuntimeByCode, businessApplicationWorkspaceByCode, designBusinessApplicationPage, initializeBusinessApplicationExcel, previewBusinessApplicationExcel, provisionBusinessApplicationFormData, updateBusinessApplication } from '@/api/business-application'
import defaultLogo from '@/assets/images/logo.png'
import AuthImage from '@/components/common/AuthImage.vue'
import IconRenderer from '@/components/IconRenderer.vue'
import { createGridBlock, DATA_FIELD_BLOCK_TYPES, isDataFieldBlockType, listPageBlockCatalog, resolveListPageBlockMeta } from '@/components/lowcode-builder/page/page-schema'
import {
  PAGE_BLOCK_PICKER_GROUPS,
  buildPageBlockPickerGroups,
  listInsertablePageBlocks,
  resolvePageBlockPickerGroup,
} from '@/components/lowcode-builder/page/page-block-picker'
import {
  alignSearchSchemaWithLeftTree,
  buildLeftTreeFilterParams,
  findTreePanelProps,
} from '@/components/lowcode-builder/shared/runtime-tree-table'
import { isPageWidgetComponentKey } from '@/components/lowcode-builder/shared/page-widget-schema'
import { useTenantStore, useUserStore } from '@/store'
import ApplicationDesignerResourceTree from '@/views/app-center/components/ApplicationDesignerResourceTree.vue'
import DesignerAsyncLoader from '@/views/app-center/components/designer/DesignerAsyncLoader.vue'
import PageSystemMenuMountDialog from '@/views/app-center/components/designer/PageSystemMenuMountDialog.vue'
import {
  applyPageMountFieldsToNode,
  isPageSystemMenuMounted,
} from '@/views/app-center/components/designer/page-system-menu-mount'
import ApplicationRuntimeSkeleton from '@/views/app-center/components/portal/ApplicationRuntimeSkeleton.vue'
import { buildAutoFieldAssets, createFieldFromComponent } from '@/views/app-center/components/designer/form-first/autoFieldRegistry'
import { createDefaultFormDesignerSchema, isFieldComponent, normalizeFormDesignerSchema, presentFormDesignerSchema } from '@/views/app-center/components/designer/form-first/formDesignerSchema'
import { filterNavigationNodesByClient } from '@/views/app-center/components/portal/portal-navigation-runtime'
import { collectPortalPageCrudTargets, warmPortalPageCrudProps } from '@/views/app-center/components/portal/portal-page-crud-warm'
import PortalPageRenderer from '@/views/app-center/components/portal/PortalPageRenderer.vue'
import {
  buildApplicationDesignerResourceGroups,
  findApplicationDesignerResource,
  normalizeApplicationDesignerSection,
  resolveApplicationDesignerObject,
  resolveObjectDesignerNavigationTarget,
} from '../application-designer-navigation'
import { createApplicationRuntimeLoadCoordinator, resolveApplicationRuntimeLoadKey, shouldUseApplicationWorkspaceLoad } from '../application-runtime-load'
import {
  createInAppFormAsset,
  createNavigationNode,
  hasPendingLegacyObjectPageMigration,
  mergeInAppBuilderOptions,
  moveNavigationNode,
  normalizeInAppBuilder,
  isOrphanPageFormObject,
  removeNavigationNode,
  resolveNavigationDeleteImpact,
  updateInAppFormAsset,
} from '../in-app-builder/in-app-builder-schema'
import { bindProvisionedFormData, collectFormDataProvisionTargets, mergePageFieldCatalogs } from '../in-app-builder/page-form-data-provisioning'
import {
  buildBusinessObjectDesignerPayloadFromFormAsset,
  filterPrimaryObjectDesignerFields,
  isChildScopedFormFieldAsset,
  normalizeObjectDesignerFieldCatalog,
  syncFormBoundFieldRefs,
} from '../in-app-builder/page-form-object-promotion'
import {
  isPageManagementSystemPageId,
  PAGE_MANAGEMENT_SYSTEM_PAGES,
  resolvePageManagementSelection,
  resolvePageManagementSystemPage,
} from '../in-app-builder/page-management'
import {
  createPageShapeBuilder,
  ensureFreeLayoutPageNode,
  isObjectBoundPageLayoutPolluted,
  resolvePageShapeFromNode,
  restoreObjectBoundPageLayout,
} from '../in-app-builder/page-shape-design'
import {
  createWorkbenchVirtualNode,
  ensureWorkbenchPageInBuilder,
  isWorkbenchPageId,
  WORKBENCH_PAGE_ID,
} from '../in-app-builder/workbench-page'
import { inAppPageTemplateCatalog, resolveInAppPageTemplate } from '../in-app-builder/page-template-catalog'
import { freeLayoutPresetCatalog } from '../in-app-builder/free-layout-preset-catalog'
import {
  ApplicationExtensionsPanel,
  ApplicationObjectsPanel,
  ApplicationProcessPanel,
  ApplicationSettingsPanel,
  BusinessObjectDesignerPage,
  BusinessProcessPage,
  ForgeFormDesigner,
  GridBlockRenderer,
  IconSelector,
  ListPageGridDesigner,
  PageDesignPublishPanel,
  PageDesignSettingsPanel,
  PageManagementSystemView,
  PageTypeSelector,
} from './applicationRuntimeLocalComponents'
import { findPageBlockInTree, mapPageBlocksInTree, removePageBlockFromTree, visitPageBlocksInTree } from './page-block-tree'
import { resolvePageBlockShellStyle as computePageBlockShellStyle, readPageBlockLength, resolveDefaultPageBlockHeight, resolveDefaultPageBlockYFromItems, resolvePageBlockFlowGeometry, resolveRootPageBlockCollisions, shouldUsePageFlowStack, normalizePagePadding, resolvePagePaddingCss, normalizePageSurface, resolvePageSurfaceStyle, migratePageFlowToContentCoords, canvasToContentFlowPoint, contentToCanvasFlowPoint, DEFAULT_PAGE_PADDING, DEFAULT_PAGE_SURFACE } from './page-flow-geometry'
import { flattenNodes, hasPermission, isNavigationVisible } from './runtime-navigation-utils'
import {
  createEmptyNavigationBadges,
  formatNavigationBadgeCount,
  loadRuntimeNavigationBadges,
  resolveSystemPageBadge,
} from './runtime-navigation-badges'
import { useBuilderHistory } from './use-builder-history'
import { useRuntimeCrudConfig } from './use-runtime-crud-config'

export function applyApplicationRuntimePart1() {
  const __impl = {}
  const mut = {
    runtimeWorkspacePanelsPrefetchScheduled: false,
    catalogPointerDragCtx: null,
    _activeFormDesignerSchemaSignature: '',
    nestedPageBlockMoveCtx: null,
    nestedPageBlockMoveFrame: 0,
    pendingNestedPageBlockMoveEvent: null,
    pageBlockResizeCtx: null,
    nestedPageBlockResizeCtx: null,
    pageBlockMoveCtx: null,
    pageBlockMoveFrame: 0,
    pendingPageBlockMoveEvent: null,
    navigationSaveTimer: null,
    navigationSavePending: false,
  }
  const animatePageBlockSwap = (...args) => __impl.animatePageBlockSwap(...args)
  const appendPageBlock = (...args) => __impl.appendPageBlock(...args)
  const appendPageBlockToContainer = (...args) => __impl.appendPageBlockToContainer(...args)
  const appendPageBlockToGridCell = (...args) => __impl.appendPageBlockToGridCell(...args)
  const appendPageBlockToTab = (...args) => __impl.appendPageBlockToTab(...args)
  const applyDesignerResource = (...args) => __impl.applyDesignerResource(...args)
  const applyNestedPageBlockMove = (...args) => __impl.applyNestedPageBlockMove(...args)
  const applyPageBlockMove = (...args) => __impl.applyPageBlockMove(...args)
  const applyPageBlockSwapPreview = (...args) => __impl.applyPageBlockSwapPreview(...args)
  const applyPageTemplate = (...args) => __impl.applyPageTemplate(...args)
  const applyFreeLayoutPreset = (...args) => __impl.applyFreeLayoutPreset(...args)
  const applyCurrentFreeLayoutPreset = (...args) => __impl.applyCurrentFreeLayoutPreset(...args)
  const openLayoutPresetPicker = (...args) => __impl.openLayoutPresetPicker(...args)
  const attachDefaultRuntimeObject = (...args) => __impl.attachDefaultRuntimeObject(...args)
  const attachSingleFormAsset = (...args) => __impl.attachSingleFormAsset(...args)
  const bindSingleFormToCompatibleBlocks = (...args) => __impl.bindSingleFormToCompatibleBlocks(...args)
  const bumpPortalCrudConfigAfterDesignerSave = (...args) => __impl.bumpPortalCrudConfigAfterDesignerSave(...args)
  const cancelRenameApplication = (...args) => __impl.cancelRenameApplication(...args)
  const clearPageAlignGuides = (...args) => __impl.clearPageAlignGuides(...args)
  const clearPageBlockSwapPreview = (...args) => __impl.clearPageBlockSwapPreview(...args)
  const clonePageBlockTree = (...args) => __impl.clonePageBlockTree(...args)
  const confirmNavigationAction = (...args) => __impl.confirmNavigationAction(...args)
  const confirmRenameApplication = (...args) => __impl.confirmRenameApplication(...args)
  const confirmSystemMenuMount = (...args) => __impl.confirmSystemMenuMount(...args)
  const copySelectedBlockToOtherPage = (...args) => __impl.copySelectedBlockToOtherPage(...args)
  const createFormAssetForCurrentPage = (...args) => __impl.createFormAssetForCurrentPage(...args)
  const createFormAssetForPageCrud = (...args) => __impl.createFormAssetForPageCrud(...args)
  const createFormAssetForSelectedBlock = (...args) => __impl.createFormAssetForSelectedBlock(...args)
  const createFormFieldVisibilitySettings = (...args) => __impl.createFormFieldVisibilitySettings(...args)
  const createLegacyBlock = (...args) => __impl.createLegacyBlock(...args)
  const createStandaloneFormAsset = (...args) => __impl.createStandaloneFormAsset(...args)
  const currentDesignerDirty = (...args) => __impl.currentDesignerDirty(...args)
  const discardAndExitEditing = (...args) => __impl.discardAndExitEditing(...args)
  const duplicateNavigationNode = (...args) => __impl.duplicateNavigationNode(...args)
  const duplicateNestedPageBlock = (...args) => __impl.duplicateNestedPageBlock(...args)
  const editSelectedBlockFormAsset = (...args) => __impl.editSelectedBlockFormAsset(...args)
  const endComponentButtonMove = (...args) => __impl.endComponentButtonMove(...args)
  const endNestedPageBlockMove = (...args) => __impl.endNestedPageBlockMove(...args)
  const endNestedPageBlockResize = (...args) => __impl.endNestedPageBlockResize(...args)
  const endPageBlockMove = (...args) => __impl.endPageBlockMove(...args)
  const endPageBlockResize = (...args) => __impl.endPageBlockResize(...args)
  const ensureFormDesignerObjectContext = (...args) => __impl.ensureFormDesignerObjectContext(...args)
  const ensurePageFlowContentCoords = (...args) => __impl.ensurePageFlowContentCoords(...args)
  const enterPageDesign = (...args) => __impl.enterPageDesign(...args)
  const enterWorkbenchDesign = (...args) => __impl.enterWorkbenchDesign(...args)
  const exitToPageManagement = (...args) => __impl.exitToPageManagement(...args)
  const extractNestedPageBlockToCanvas = (...args) => __impl.extractNestedPageBlockToCanvas(...args)
  const findFirstBlockWithFormAssetId = (...args) => __impl.findFirstBlockWithFormAssetId(...args)
  const findPageBlockByFormAssetId = (...args) => __impl.findPageBlockByFormAssetId(...args)
  const finishCatalogPointerDrag = (...args) => __impl.finishCatalogPointerDrag(...args)
  const handleApplicationObjectsChanged = (...args) => __impl.handleApplicationObjectsChanged(...args)
  const handleCatalogPointerMove = (...args) => __impl.handleCatalogPointerMove(...args)
  const handleComponentCatalogClick = (...args) => __impl.handleComponentCatalogClick(...args)
  const handleComponentCatalogDragEnd = (...args) => __impl.handleComponentCatalogDragEnd(...args)
  const handleComponentCatalogDragStart = (...args) => __impl.handleComponentCatalogDragStart(...args)
  const handleEmbeddedDesignerSaved = (...args) => __impl.handleEmbeddedDesignerSaved(...args)
  const handleEmbeddedProcessDesignerSaved = (...args) => __impl.handleEmbeddedProcessDesignerSaved(...args)
  const handleExtensionsChanged = (...args) => __impl.handleExtensionsChanged(...args)
  const handleInlineTextUpdate = (...args) => __impl.handleInlineTextUpdate(...args)
  const handleNavigationMoreSelect = (...args) => __impl.handleNavigationMoreSelect(...args)
  const handleNestedPageBlockMenuSelect = (...args) => __impl.handleNestedPageBlockMenuSelect(...args)
  const handleNestedPageBlockMoveStart = (...args) => __impl.handleNestedPageBlockMoveStart(...args)
  const handleNestedPageBlockResizeStart = (...args) => __impl.handleNestedPageBlockResizeStart(...args)
  const handleNestedPageBlockSelect = (...args) => __impl.handleNestedPageBlockSelect(...args)
  const handlePageBlockDataSourceRequest = (...args) => __impl.handlePageBlockDataSourceRequest(...args)
  const handlePageBlockMoreSelect = (...args) => __impl.handlePageBlockMoreSelect(...args)
  const handlePageFlowBlankClick = (...args) => __impl.handlePageFlowBlankClick(...args)
  const openPagePropertiesPanel = (...args) => __impl.openPagePropertiesPanel(...args)
  const handlePageFlowContainerClear = (...args) => __impl.handlePageFlowContainerClear(...args)
  const handlePageFlowContainerInsert = (...args) => __impl.handlePageFlowContainerInsert(...args)
  const handlePageFlowDragOver = (...args) => __impl.handlePageFlowDragOver(...args)
  const handlePageFlowDrop = (...args) => __impl.handlePageFlowDrop(...args)
  const handlePageFlowGridCellDrop = (...args) => __impl.handlePageFlowGridCellDrop(...args)
  const handlePageFlowTabDrop = (...args) => __impl.handlePageFlowTabDrop(...args)
  const handleProcessPanelNavigate = (...args) => __impl.handleProcessPanelNavigate(...args)
  const handleResourceNodeAction = (...args) => __impl.handleResourceNodeAction(...args)
  const handleRuntimeBack = (...args) => __impl.handleRuntimeBack(...args)
  const handleRuntimeHeaderMoreSelect = (...args) => __impl.handleRuntimeHeaderMoreSelect(...args)
  const handleRuntimeTreeSelect = (...args) => __impl.handleRuntimeTreeSelect(...args)
  const hydratePageCrudApiPlaceholders = (...args) => __impl.hydratePageCrudApiPlaceholders(...args)
  const insertComponent = (...args) => __impl.insertComponent(...args)
  const isDraftPreviewMode = (...args) => __impl.isDraftPreviewMode(...args)
  const isNavigationGroupDescendant = (...args) => __impl.isNavigationGroupDescendant(...args)
  const isPageBlockDataSourceConfigured = (...args) => __impl.isPageBlockDataSourceConfigured(...args)
  const isPageBlockRuntimeCrudLoading = (...args) => __impl.isPageBlockRuntimeCrudLoading(...args)
  const isPageCatalogDrag = (...args) => __impl.isPageCatalogDrag(...args)
  const isSimpleBodyContainer = (...args) => __impl.isSimpleBodyContainer(...args)
  const isValidPageBlockObjectRef = (...args) => __impl.isValidPageBlockObjectRef(...args)
  const itemBorderColorMode = (...args) => __impl.itemBorderColorMode(...args)
  const moveNavigationByOffset = (...args) => __impl.moveNavigationByOffset(...args)
  const movePageBlockIntoContainer = (...args) => __impl.movePageBlockIntoContainer(...args)
  const moveRootPageBlockToGridCell = (...args) => __impl.moveRootPageBlockToGridCell(...args)
  const moveRootPageBlockToTab = (...args) => __impl.moveRootPageBlockToTab(...args)
  const normalizeContainerDropTarget = (...args) => __impl.normalizeContainerDropTarget(...args)
  const normalizePageBlockForContainer = (...args) => __impl.normalizePageBlockForContainer(...args)
  const onComponentButtonMove = (...args) => __impl.onComponentButtonMove(...args)
  const onNestedPageBlockMove = (...args) => __impl.onNestedPageBlockMove(...args)
  const onPageBlockMove = (...args) => __impl.onPageBlockMove(...args)
  const onPageBlockResize = (...args) => __impl.onPageBlockResize(...args)
  const openBusinessObjectDesign = (...args) => __impl.openBusinessObjectDesign(...args)
  const openCustomPageSelector = (...args) => __impl.openCustomPageSelector(...args)
  const openDraftPreview = (...args) => __impl.openDraftPreview(...args)
  const openEmbeddedObjectActions = (...args) => __impl.openEmbeddedObjectActions(...args)
  const openExcelPageImport = (...args) => __impl.openExcelPageImport(...args)
  const openFormAssetDesigner = (...args) => __impl.openFormAssetDesigner(...args)
  const openFormAssetDesignerForPage = (...args) => __impl.openFormAssetDesignerForPage(...args)
  const openObjectDesigner = (...args) => __impl.openObjectDesigner(...args)
  const openObjectResourcePreview = (...args) => __impl.openObjectResourcePreview(...args)
  const openObjectSetup = (...args) => __impl.openObjectSetup(...args)
  const openPageBlockConfiguration = (...args) => __impl.openPageBlockConfiguration(...args)
  const openProcessDesigner = (...args) => __impl.openProcessDesigner(...args)
  const openSelectedBlockFormDesigner = (...args) => __impl.openSelectedBlockFormDesigner(...args)
  const openSystemMenuMountDialog = (...args) => __impl.openSystemMenuMountDialog(...args)
  const openWorkspace = (...args) => __impl.openWorkspace(...args)
  const pageUsesFreeLayoutCanvas = (...args) => __impl.pageUsesFreeLayoutCanvas(...args)
  const patchCurrentPageNode = (...args) => __impl.patchCurrentPageNode(...args)
  const patchNavigationNodeMount = (...args) => __impl.patchNavigationNodeMount(...args)
  const persistApplicationDraft = (...args) => __impl.persistApplicationDraft(...args)
  const preloadCurrentPageCrudRuntimeProps = (...args) => __impl.preloadCurrentPageCrudRuntimeProps(...args)
  const preloadPageBlockCrudRuntimeProps = (...args) => __impl.preloadPageBlockCrudRuntimeProps(...args)
  const promoteCurrentPageToListShape = (...args) => __impl.promoteCurrentPageToListShape(...args)
  const provisionPendingFormData = (...args) => __impl.provisionPendingFormData(...args)
  const refreshWorkspaceMetadata = (...args) => __impl.refreshWorkspaceMetadata(...args)
  const relocateNestedPageBlockToContainer = (...args) => __impl.relocateNestedPageBlockToContainer(...args)
  const relocateNestedPageBlockToGridCell = (...args) => __impl.relocateNestedPageBlockToGridCell(...args)
  const relocateNestedPageBlockToTab = (...args) => __impl.relocateNestedPageBlockToTab(...args)
  const renderNavigationMenuIcon = (...args) => __impl.renderNavigationMenuIcon(...args)
  const requestExitEditing = (...args) => __impl.requestExitEditing(...args)
  const resizeNestedPageBlock = (...args) => __impl.resizeNestedPageBlock(...args)
  const resolveActiveDesignerPageId = (...args) => __impl.resolveActiveDesignerPageId(...args)
  const resolveEntryDesignTab = (...args) => __impl.resolveEntryDesignTab(...args)
  const resolveFormAssetFields = (...args) => __impl.resolveFormAssetFields(...args)
  const resolveFormAssetIdForPage = (...args) => __impl.resolveFormAssetIdForPage(...args)
  const resolveFormLayoutBlockType = (...args) => __impl.resolveFormLayoutBlockType(...args)
  const resolveNavigationMoreOptions = (...args) => __impl.resolveNavigationMoreOptions(...args)
  const resolveNextNavigationTitle = (...args) => __impl.resolveNextNavigationTitle(...args)
  const resolveNextPageBlockStackY = (...args) => __impl.resolveNextPageBlockStackY(...args)
  const resolvePageBlockBackgroundColor = (...args) => __impl.resolvePageBlockBackgroundColor(...args)
  const resolvePageBlockFields = (...args) => __impl.resolvePageBlockFields(...args)
  const resolvePageBlockMoreOptions = (...args) => __impl.resolvePageBlockMoreOptions(...args)
  const resolvePageBlockMoveIntoOptions = (...args) => __impl.resolvePageBlockMoveIntoOptions(...args)
  const resolvePageBlockObjectRef = (...args) => __impl.resolvePageBlockObjectRef(...args)
  const resolvePageBlockRuntimeCrudProps = (...args) => __impl.resolvePageBlockRuntimeCrudProps(...args)
  const resolvePageBlockShellStyle = (...args) => __impl.resolvePageBlockShellStyle(...args)
  const resolvePageBlockSwapTarget = (...args) => __impl.resolvePageBlockSwapTarget(...args)
  const resolvePageDesignTab = (...args) => __impl.resolvePageDesignTab(...args)
  const resolvePageFlowContainerTargetByPageBlock = (...args) => __impl.resolvePageFlowContainerTargetByPageBlock(...args)
  const resolvePageFlowContainerTargetByRect = (...args) => __impl.resolvePageFlowContainerTargetByRect(...args)
  const resolvePageFlowContainerTargetFromElement = (...args) => __impl.resolvePageFlowContainerTargetFromElement(...args)
  const resolvePageFlowContainerTargetFromPoint = (...args) => __impl.resolvePageFlowContainerTargetFromPoint(...args)
  const resolvePageFlowGridCellTarget = (...args) => __impl.resolvePageFlowGridCellTarget(...args)
  const resolvePageFlowGridCellTargetByPageBlock = (...args) => __impl.resolvePageFlowGridCellTargetByPageBlock(...args)
  const resolvePageFlowGridCellTargetByRect = (...args) => __impl.resolvePageFlowGridCellTargetByRect(...args)
  const resolvePageFlowGridCellTargetFromElement = (...args) => __impl.resolvePageFlowGridCellTargetFromElement(...args)
  const resolvePageFlowGridCellTargetFromPoint = (...args) => __impl.resolvePageFlowGridCellTargetFromPoint(...args)
  const resolvePageFlowTabTarget = (...args) => __impl.resolvePageFlowTabTarget(...args)
  const resolvePageFlowTabTargetFromPoint = (...args) => __impl.resolvePageFlowTabTargetFromPoint(...args)
  const resolvePagePreviewBlock = (...args) => __impl.resolvePagePreviewBlock(...args)
  const resolvePageShapeDesignContext = (...args) => __impl.resolvePageShapeDesignContext(...args)
  const resolvePageShapeKey = (...args) => __impl.resolvePageShapeKey(...args)
  const resolvePortalFormFields = (...args) => __impl.resolvePortalFormFields(...args)
  const resolveResourceNodeBuilderNode = (...args) => __impl.resolveResourceNodeBuilderNode(...args)
  const resolveResourceNodeMenuOptions = (...args) => __impl.resolveResourceNodeMenuOptions(...args)
  const resolveRuntimeBackLabel = (...args) => __impl.resolveRuntimeBackLabel(...args)
  const resolveRuntimeBrandEyebrow = (...args) => __impl.resolveRuntimeBrandEyebrow(...args)
  const resolveRuntimeHeaderTitle = (...args) => __impl.resolveRuntimeHeaderTitle(...args)
  const resolveRuntimeBrandStatus = (...args) => __impl.resolveRuntimeBrandStatus(...args)
  const openRuntimeWorkbench = (...args) => __impl.openRuntimeWorkbench(...args)
  const resolveRuntimeObjectCacheKey = (...args) => __impl.resolveRuntimeObjectCacheKey(...args)
  const restoreCurrentObjectPageLayout = (...args) => __impl.restoreCurrentObjectPageLayout(...args)
  const returnToEditorFromDraftPreview = (...args) => __impl.returnToEditorFromDraftPreview(...args)
  const returnToPageDesigner = (...args) => __impl.returnToPageDesigner(...args)
  const saveActiveFormDesigner = (...args) => __impl.saveActiveFormDesigner(...args)
  const saveCurrentDesignerSection = (...args) => __impl.saveCurrentDesignerSection(...args)
  const saveDraft = (...args) => __impl.saveDraft(...args)
  const saveNavigationDraft = (...args) => __impl.saveNavigationDraft(...args)
  const scheduleNavigationSave = (...args) => __impl.scheduleNavigationSave(...args)
  const selectDesignerResource = (...args) => __impl.selectDesignerResource(...args)
  const selectFormAssetFromPicker = (...args) => __impl.selectFormAssetFromPicker(...args)
  const selectPageBlock = (...args) => __impl.selectPageBlock(...args)
  const selectPageManagementNode = (...args) => __impl.selectPageManagementNode(...args)
  const setFormDataProvisionState = (...args) => __impl.setFormDataProvisionState(...args)
  const snapPageBlockPosition = (...args) => __impl.snapPageBlockPosition(...args)
  const startCatalogPointerDrag = (...args) => __impl.startCatalogPointerDrag(...args)
  const startComponentButtonMove = (...args) => __impl.startComponentButtonMove(...args)
  const startPageBlockMove = (...args) => __impl.startPageBlockMove(...args)
  const startPageBlockResize = (...args) => __impl.startPageBlockResize(...args)
  const startRenameApplication = (...args) => __impl.startRenameApplication(...args)
  const supportsFormAsset = (...args) => __impl.supportsFormAsset(...args)
  const switchPageDesignTab = (...args) => __impl.switchPageDesignTab(...args)
  const switchRuntimeView = (...args) => __impl.switchRuntimeView(...args)
  const syncActiveFormAssetForPage = (...args) => __impl.syncActiveFormAssetForPage(...args)
  const syncActivePageShapeObject = (...args) => __impl.syncActivePageShapeObject(...args)
  const syncCatalogDropActiveClass = (...args) => __impl.syncCatalogDropActiveClass(...args)
  const toggleNavigationVisible = (...args) => __impl.toggleNavigationVisible(...args)
  const unmountSystemMenu = (...args) => __impl.unmountSystemMenu(...args)
  const updateActiveFormDesignerSchema = (...args) => __impl.updateActiveFormDesignerSchema(...args)
  const updateCurrentGridLayout = (...args) => __impl.updateCurrentGridLayout(...args)
  const updatePageAlignGuides = (...args) => __impl.updatePageAlignGuides(...args)
  const updatePageBlockBackgroundColor = (...args) => __impl.updatePageBlockBackgroundColor(...args)
  const updatePageBlockSize = (...args) => __impl.updatePageBlockSize(...args)
  const updatePageBlocks = (...args) => __impl.updatePageBlocks(...args)
  const updatePageCanvasPadding = (...args) => __impl.updatePageCanvasPadding(...args)
  const updatePageCanvasSurface = (...args) => __impl.updatePageCanvasSurface(...args)
  const resetPageCanvasSurface = (...args) => __impl.resetPageCanvasSurface(...args)
  const updateResizeCollisionHighlights = (...args) => __impl.updateResizeCollisionHighlights(...args)
  const updateSelectedBlockAppearance = (...args) => __impl.updateSelectedBlockAppearance(...args)
  const updateSelectedBlockFormAsset = (...args) => __impl.updateSelectedBlockFormAsset(...args)
  const setFormAssetAsListCrudForm = (...args) => __impl.setFormAssetAsListCrudForm(...args)
  const isFormAssetBoundToListCrud = (...args) => __impl.isFormAssetBoundToListCrud(...args)
  const collectFormAssetUsages = (...args) => __impl.collectFormAssetUsages(...args)
  const formatFormAssetUsageSummary = (...args) => __impl.formatFormAssetUsageSummary(...args)
  const focusFormAssetUsage = (...args) => __impl.focusFormAssetUsage(...args)
  const clearFormAssetListCrudBinding = (...args) => __impl.clearFormAssetListCrudBinding(...args)
  const updateSelectedPageBlockRuntimeObject = (...args) => __impl.updateSelectedPageBlockRuntimeObject(...args)
  const route = useRoute()
  const router = useRouter()
  const message = useMessage()
  const tenantStore = useTenantStore()
  const formComponentIconModules = import.meta.glob('/src/assets/images/form/*.png', { import: 'default' })
  const formComponentIconCache = reactive({})
  const formComponentIconFileByBlockType = {
    'search-form': 'chaxunbiaodan',
    'toolbar': 'caozuogongjulan',
    'back-button': 'fanhuishangyiye',
    'page-title': 'yemianbiaoti',
    'grid-layout': 'shangebuju',
    'detail-info': 'xiangqingxinxi',
    'AiCrudPage': 'crud',
    'AiTable': 'zhinengbiaoge',
    'AiForm': 'zhinengbiaodan',
    'data-table': 'shujuliebiao',
    'tree-panel': 'shaixuanshu',
    'stats-strip': 'zhibiaokapian',
    'workspace-summary-metrics': 'zhibiaokapian',
    'info-panel': 'tishimianban',
    'custom-html': 'shuomingwenben',
    'action-button': 'button',
    'button-group': 'buttongroup',
    'tag-list': 'biaoqianliebiao',
    'steps': 'buzhoutiao',
    'timeline': 'shijianxian',
    'empty-state': 'kognzhuangtai',
    'card': 'kapianrongqi',
    'tabs': 'tabs',
    'divider': 'fengexian',
    'spacer': 'liubaizhanwei',
    'signature-pad': 'qianming',
    'step-form': 'fenbubiaodan',
    'text-title': 'biaoti',
    'paragraph': 'duanluo',
    'statistic': 'tongjishuzhi',
    'link': 'lianjie',
    'text-tip': 'wenzitishi',
    'audio-player': 'yinpinbofangqi',
    'video-player': 'shipinbofangqi',
    'avatar': 'touxiang',
    'iframe': 'neiqianyemian',
    'box-layout': 'gezibuju',
    'space': 'jianju',
    'sub-table-tabs': 'zibiaotab',
    'section-divider': 'fenzubiaoti',
    'transfer': 'chuansuokuang',
    'watermark': 'shuiyin',
    'vue-component': 'vue',
    'markdown': 'md',
    'barcode': 'tiaoxingma',
    'qrcode': 'erweima',
    'calendar': 'rili',
    'code': 'daima',
    'countdown': 'daojishi',
    'descriptions': 'miaoshu',
    'announcement': 'gognshi',
    'list': 'shujuliebiao',
    'log': 'log',
    'number-animation': 'shuzidonghua',
    'breadcrumb': 'mianbaoxie',
    'menu': 'caidan',
    'pagination': 'fenye',
  }
  const userStore = useUserStore()
  // 设计页 chunk 较大；delay>0 会先空白再出 loader，表现为「白屏一会儿」
  const asyncPanelLoader = {
    delay: 0,
    loadingComponent: DesignerAsyncLoader,
  }
  // Async panels are registered on Options `components:{}` via applicationRuntimeLocalComponents
  // (setup return alone does not resolve PascalCase tags under Options API).

  /** 页面管理空闲时预拉三个 Tab 的 JS chunk，避免首次点击才开始下载大包。 */
  const runtimeWorkspacePanelImporters = [
    () => import('../application-workspace/ApplicationProcessPanel.vue'),
    () => import('../application-workspace/ApplicationExtensionsPanel.vue'),
    () => import('../components/ApplicationSettingsPanel.vue'),
  ]

  function prefetchRuntimeWorkspacePanels() {
    if (mut.runtimeWorkspacePanelsPrefetchScheduled || typeof window === 'undefined')
      return
    mut.runtimeWorkspacePanelsPrefetchScheduled = true
    const run = () => {
      runtimeWorkspacePanelImporters.forEach((load) => {
        void load().catch(() => {})
      })
    }
    if (typeof window.requestIdleCallback === 'function')
      window.requestIdleCallback(run, { timeout: 2500 })
    else
      window.setTimeout(run, 800)
  }
  __impl.prefetchRuntimeWorkspacePanels = prefetchRuntimeWorkspacePanels

  const application = ref(null)
  const objects = ref([])
  const builder = ref(null)
  // 左侧页面删掉后，只由该页面创建的业务对象不能再出现在流程选择里。
  const processSelectableObjects = computed(() => (objects.value || []).filter(item => !isOrphanPageFormObject(item, builder.value)))
  const loadError = ref('')
  // 组件一挂载就显示骨架，避免「白屏 → 再出骨架」；load() finally 会关掉
  const loading = ref(true)
  /** 编辑态自由布局：顶栏已出但画布 chunk/首帧未就绪时，中间盖骨架避免纯白屏 */
  const editCanvasBootstrapping = ref(false)
  const saving = ref(false)
  const previewTabLoading = ref(false)
  const restoringObjectPageLayout = ref(false)
  const editing = ref(route.query.edit === '1')
  // Created early so part1 watches / load / part2 handlers can mutate before part4 re-resolves from URL+shape.
  const _designTabRaw = Array.isArray(route.query.designTab) ? route.query.designTab[0] : route.query.designTab
  const _designTabSeed = String(_designTabRaw || '').trim()
  const activePageDesignTab = ref(
    ['page', 'form', 'list', 'settings', 'publish'].includes(_designTabSeed) ? _designTabSeed : 'page',
  )
  /** Pure view-mode normalizer — must be a real function (not __impl) because runtimeViewMode initializes eagerly. */
  function resolveRuntimeView(value) {
    const normalized = String(Array.isArray(value) ? value[0] : value || '').toLowerCase()
    return ['pages', 'process', 'enhance', 'settings'].includes(normalized) ? normalized : 'pages'
  }
  __impl.resolveRuntimeView = resolveRuntimeView
  const runtimeViewMode = ref(resolveRuntimeView(route.query.view)) // 'pages' | 'process' | 'enhance' | 'settings'
  const exitEditingVisible = ref(false)
  const selectedNodeId = ref('')
  const enhanceContextPageId = computed(() => {
    const fromQuery = String(route.query.pageId || '').trim()
    if (fromQuery && !isPageManagementSystemPageId(fromQuery))
      return fromQuery
    const selected = String(selectedNodeId.value || '').trim()
    if (selected && !isPageManagementSystemPageId(selected))
      return selected
    return ''
  })
  const newNodePopoverVisible = ref(false)
  const selectedPageTemplateKey = ref('blank')
  const iconPickerVisible = ref(false)
  const iconPickerNodeId = ref('')
  const navigationActionVisible = ref(false)
  const navigationActionMode = ref('')
  const navigationActionNodeId = ref('')
  const navigationActionForm = ref({ title: '', icon: '', parentId: null, deleteStrategy: 'delete-children', targetParentId: null })
  const systemMenuMountVisible = ref(false)
  const systemMenuMountNodeId = ref('')
  const systemMenuMountNode = computed(() => (builder.value?.nodes || []).find(item => item.id === systemMenuMountNodeId.value) || null)
  const componentPopoverVisible = ref(false)
  const componentKeyword = ref('')
  const savedSignature = ref('')
  const selectedPageBlockId = ref('')
  /** 仅悬停/选中时挂载块操作条，避免每块都算「更多操作」菜单拖垮选中流畅度 */
  const hoveredPageBlockId = ref('')
  /** 画布点选的容器 id；货架连续添加时仅落入该目标，避免「刚加的 Tabs」吞掉后续组件 */
  const nestHostPinnedId = ref('')
  const draggingPageBlockId = ref('')
  const dragPreview = ref(null)
  const pageAlignGuides = ref({ visible: false, edges: null, x: [], y: [] })
  const PAGE_ALIGN_SNAP_PX = 6
  const configPanelVisible = ref(false)
  const inspectorTab = ref('properties')
  const componentButtonPosition = ref({ x: null, y: null })
  const componentButtonMoveCtx = ref(null)
  const catalogDragBlockType = ref('')
  const suppressCatalogClick = ref(false)
  const activePageFlowTabTarget = ref(null)
  const activePageFlowGridTarget = ref(null)
  const activePageFlowContainerTarget = ref(null)
  const resizeCollisionBlockIds = ref([])

  // 表单设计器的对象设计器上下文（fields/relations/actions），按对象缓存；字段目录供字段资产货架使用。
  const formDesignerObjectContextByObjectId = ref({})
  const formDesignerObjectContextLoadingIds = reactive(new Set())
  const formDesignerMode = ref(false)
  // 新建页面后草稿保存、路由切换和表单资产挂载不是同一个 tick；资产就绪前保留设计器占位，避免出现空白区域。
  const designerTransitionLoading = ref(false)
  // 树节点高亮按区块维护；右表筛选按当前页面维护。
  // 左树与右表通常是两个不同区块，不能用同一个 blockId 关联，否则点击树后过滤会落在树区块自身而不是右表。
  const runtimeTreeFilter = ref({})
  const runtimeTreeFilterByBlockId = ref({})
  const runtimeTreeActiveKeyByBlockId = ref({})
  const formDesignerFromPageManagement = ref(false)
  const activeFormAssetId = ref('')
  const activePageShapeDesign = ref(null)
  const pageTypeSelectorVisible = ref(false)
  const pageTypeSelectorParentId = ref(null)
  const pageTypeSelectorDefaultType = ref('form')
  const layoutPresetPickerVisible = ref(false)
  const sidebarCollapsed = ref(false)
  const renamingApplication = ref(false)
  const renameApplicationValue = ref('')
  const renameSaving = ref(false)
  const renameInputRef = ref(null)
  const collapsedGroupIds = ref(new Set())
  const copyBlockVisible = ref(false)
  const copyBlockId = ref('')
  const copyBlockTargetPageId = ref('')
  const blockBackgroundPickerVisible = ref(false)
  const backgroundPickerBlockId = ref('')
  const formAssetSelectorOpen = ref(false)
  const formAssetSelectorKeyword = ref('')
  const formDataProvisioningByAssetId = ref({})
  const objectSetupVisible = ref(false)
  const selectedDesignerResourceKey = ref(String(route.query.designResource || ''))
  const workspaceExtensions = ref([])
  const workspaceEntries = ref([])
  /** 表单/对象保存后递增，驱动 PortalPageRenderer 丢弃旧 CRUD 缓存并重新 render */
  const portalCrudConfigRevision = ref(0)
  /** 骨架阶段预热的首屏 CRUD props，交给 PortalPageRenderer 避免壳出后内容空白 */
  const portalCrudSeed = ref({})
  const isDraftMode = computed(() => route.query.edit === '1' || route.query.draft === '1')
  const {
    runtimeCrudPropsByObjectId,
    runtimeCrudLoadingObjectIds,
    runtimeCrudUnavailableObjectIds,
    loadRuntimeCrudProps,
    resetRuntimeCrudConfig,
  } = useRuntimeCrudConfig({
    application,
    workspaceEntries,
    pageId: computed(() => String(selectedNodeId.value || route.query.pageId || '').trim()),
    // 草稿/编辑态才允许降级读取对象设计器字段目录做静态预览
    canLoadDesignerSchema: computed(() => editing.value || isDraftMode.value || canEditApplication.value),
  })
  const activeFlowContext = ref({})
  const embeddedDesignerRef = ref(null)
  const embeddedDesignerDirty = ref(false)
  const embeddedDesignerSaving = ref(false)
  /** 预览落盘时跳过 workspace 全量刷新，避免设计器被重挂 */
  const suppressHeavyEmbeddedSaveSideEffects = ref(false)
  /** 本会话进过列表/表单设计后，预览期间用 v-show 保活，回来不重载 */
  const listDesignerSessionActive = ref(false)
  const formDesignerSessionActive = ref(false)
  const embeddedProcessDesignerVisible = ref(false)
  const embeddedProcessDesignerId = ref('')
  const applicationRuntimeLoadCoordinator = createApplicationRuntimeLoadCoordinator(load)

  const componentPickerGroupOptions = PAGE_BLOCK_PICKER_GROUPS.map(group => ({ ...group }))
  const runtimeHeaderMoreOptions = computed(() => [
    ...(editing.value
      ? [
          { label: '撤销', key: 'undo', icon: () => renderNavigationMenuIcon(ArrowUndoOutline), disabled: !canUndo.value || !pageBuilderResourceActive.value },
          { label: '重做', key: 'redo', icon: () => renderNavigationMenuIcon(ArrowRedoOutline), disabled: !canRedo.value || !pageBuilderResourceActive.value },
          { type: 'divider', key: 'header-more-divider-1' },
        ]
      : []),
    { label: '高级数据设置', key: 'object-design', icon: () => renderNavigationMenuIcon(ReaderOutline) },
    ...(editing.value
      ? [
          { type: 'divider', key: 'header-more-divider-2' },
          { label: '退出设计', key: 'exit-editing', icon: () => renderNavigationMenuIcon(ArrowBackOutline) },
        ]
      : []),
  ])
  const pageManagementSystemPages = PAGE_MANAGEMENT_SYSTEM_PAGES
  const systemPageIconMap = {
    'grid': GridOutline,
    'checkbox': CheckboxOutline,
    'checkmark-done': CheckmarkDoneOutline,
    'paper-plane': PaperPlaneOutline,
    'people': PeopleOutline,
    'notifications': NotificationsOutline,
  }
  const navigationBadges = ref(createEmptyNavigationBadges())
  let navigationBadgeTimer = 0
  function resolveSystemPageIcon(icon) {
    return systemPageIconMap[icon] || DocumentTextOutline
  }
  __impl.resolveSystemPageIcon = resolveSystemPageIcon
  function resolveNavigationBadge(pageId) {
    return resolveSystemPageBadge(pageId, navigationBadges.value)
  }
  __impl.resolveNavigationBadge = resolveNavigationBadge
  async function refreshNavigationBadges() {
    if (editing.value)
      return
    try {
      navigationBadges.value = await loadRuntimeNavigationBadges()
    }
    catch {
      navigationBadges.value = createEmptyNavigationBadges()
    }
  }
  __impl.refreshNavigationBadges = refreshNavigationBadges
  function scheduleNavigationBadgeRefresh() {
    if (navigationBadgeTimer)
      window.clearTimeout(navigationBadgeTimer)
    navigationBadgeTimer = window.setTimeout(() => {
      navigationBadgeTimer = 0
      void refreshNavigationBadges()
    }, 80)
  }
  const currentSystemPage = computed(() => resolvePageManagementSystemPage(selectedNodeId.value))
  const workbenchPage = computed(() => builder.value?.pages?.[WORKBENCH_PAGE_ID] || null)
  const systemPageNavigationRoutes = computed(() => ({
    todo: '/workspace/todo',
    done: '/workspace/done',
    sent: '/workspace/started',
    started: '/workspace/started',
    cc: '/workspace/cc',
  }))
  const designerResourceGroups = computed(() => buildApplicationDesignerResourceGroups({
    objects: objects.value,
    designersByObjectId: {},
    pages: builder.value?.nodes || [],
    extensions: workspaceExtensions.value,
  }))
  const activeDesignerResource = computed(() => {
    const routeResourceKey = selectedDesignerResourceKey.value
      || String(route.query.designResource || '')
      || (route.query.pageId ? `page-custom:${String(route.query.pageId)}` : '')
    return findApplicationDesignerResource(
      designerResourceGroups.value,
      routeResourceKey,
      route.query.designSection,
    )
  })
  const designerSection = computed(() => activeDesignerResource.value?.groupKey || normalizeApplicationDesignerSection(route.query.designSection))
  const pageBuilderResourceActive = computed(() => {
    if (activeDesignerResource.value?.kind === 'page-custom')
      return true
    // 个人工作台不在 nodes 资源树里，但仍按自由布局页预览
    return isWorkbenchPageId(route.query.pageId || selectedNodeId.value)
  })

  // 工作台编辑者需要维护管理端和移动端两套页面树；正式门户仍由
  // application-portal.vue 按当前客户端过滤。该权限计算放在导航树之前，
  // 避免导航树首次求值时拿到旧的客户端过滤结果。
  const canEditApplication = computed(() => userStore.isAdmin || hasPermission(userStore.permissions, 'ai:businessApplication:edit') || hasPermission(userStore.apiPermissions, 'ai:businessApplication:edit') || hasPermission(userStore.getDataPermission, 'ai:businessApplication:edit'))
  /** 编辑/草稿，或应用工作台内有编辑权限时：走草稿 CRUD（含新增），不要求先发布应用 */
  const usePortalDesignPreview = computed(() => (
    editing.value
    || isDraftMode.value
    || canEditApplication.value
  ))
  const activeDesignerObject = computed(() => resolveApplicationDesignerObject(objects.value, activeDesignerResource.value?.objectId))
  // 对象页面（表单页/列表页/数据结构）也应能预览，落到对象自身的 CRUD 运行页。
  const activeResourceConfigKey = computed(() => String(activeDesignerObject.value?.configKey || '').trim())
  const previewableResourceActive = computed(() => {
    if (pageBuilderResourceActive.value)
      return true
    const kind = activeDesignerResource.value?.kind || ''
    const objectBacked = kind.startsWith('page-') || kind.startsWith('data-')
    return objectBacked && Boolean(activeResourceConfigKey.value)
  })
  const legacyBlockTypeMap = { 'intro': 'page-title', 'metric-card': 'stats-strip', 'business-list': 'AiCrudPage', 'business-form': 'AiForm', 'todo': 'info-panel', 'chart': 'stats-strip', 'text': 'custom-html', 'image': 'info-panel', 'columns': 'grid-layout', 'divider': 'divider' }
  const pageBlockResizeAnchors = ['top-left', 'top', 'top-right', 'right', 'bottom-right', 'bottom', 'bottom-left', 'left']
  const pageBlockRecommendedColors = ['#3370ff', '#8b5cf6', '#14b8a6', '#f59e0b', '#f97316', '#ef4444', '#4e5969', '#edf4ff', '#f3efff', '#e6fffb', '#fff7e6', '#fff1f0', '#f2f3f5']

  const groupOptions = computed(() => (builder.value?.nodes || []).filter(item => item.type === 'group').map(item => ({ label: item.title, value: item.id })))
  const navigationActionNode = computed(() => builder.value?.nodes.find(item => item.id === navigationActionNodeId.value) || null)
  const iconPickerNode = computed(() => builder.value?.nodes.find(item => item.id === iconPickerNodeId.value) || null)
  const navigationIconValue = computed({
    get: () => iconPickerNode.value?.icon || '',
    set: (icon) => {
      if (!iconPickerNode.value)
        return
      builder.value = {
        ...builder.value,
        nodes: builder.value.nodes.map(item => item.id === iconPickerNode.value.id ? { ...item, icon } : item),
      }
      iconPickerVisible.value = false
      scheduleNavigationSave()
    },
  })
  const navigationActionTitle = computed(() => ({ rename: '重命名', move: '移动到', delete: '危险操作：删除页面或页面组' }[navigationActionMode.value] || '页面操作'))
  const navigationActionHasChildren = computed(() => navigationActionNode.value?.type === 'group' && builder.value?.nodes.some(item => item.parentId === navigationActionNode.value.id))
  const navigationDeleteImpact = computed(() => {
    if (navigationActionMode.value !== 'delete' || !navigationActionNode.value) {
      return {
        removedPageIds: [],
        impactedObjects: [],
        danger: false,
      }
    }
    const strategy = navigationActionHasChildren.value
      ? {
          type: navigationActionForm.value.deleteStrategy,
          targetParentId: navigationActionForm.value.targetParentId,
        }
      : undefined
    return resolveNavigationDeleteImpact(builder.value, navigationActionNode.value.id, strategy, objects.value)
  })
  const navigationDeleteTip = computed(() => {
    if (navigationActionHasChildren.value)
      return '该页面组含有子项，请选择处理方式。删除后导航与页面设计无法恢复。'
    const pageCount = navigationDeleteImpact.value.removedPageIds.length
    const objectCount = navigationDeleteImpact.value.impactedObjects.length
    if (objectCount > 0) {
      return `将删除 ${pageCount || 1} 个页面，并清理 ${objectCount} 个关联业务对象配置（多为页面表单托管对象）。删除后无法恢复。`
    }
    return '删除后无法恢复。页面设计与导航配置将被移除。'
  })
  const moveGroupOptions = computed(() => groupOptions.value.filter(item => item.value !== navigationActionNodeId.value && !isNavigationGroupDescendant(item.value, navigationActionNodeId.value)))
  const navigationNodes = computed(() => {
    const nodes = builder.value?.nodes || []
    // 普通管理端运行用户只展示 pc/BOTH 菜单；H5 菜单由移动端客户端读取。
    // 编辑态和有应用编辑权限的工作台用户保留完整页面树，方便配置、
    // 维护移动端挂载页面；正式门户仍在 application-portal.vue 中按客户端隔离。
    const clientNodes = editing.value || canEditApplication.value
      ? nodes
      : filterNavigationNodesByClient(nodes, 'pc')
    // 有编辑权限的用户在工作台始终能看到所有页面（包括隐藏的），方便恢复显示
    // 普通用户在门户运行时只看到可见的页面
    const shouldShowAll = editing.value || canEditApplication.value
    return flattenNodes(shouldShowAll ? clientNodes : clientNodes.filter(isNavigationVisible), null, 0, collapsedGroupIds.value)
  })
  function readRouteDesignTab() {
    const raw = route.query.designTab
    return String(Array.isArray(raw) ? raw[0] : (raw ?? '')).trim()
  }
  __impl.readRouteDesignTab = readRouteDesignTab

  const currentNode = computed(() => {
    const preferredId = editing.value
      ? String(route.query.pageId || selectedNodeId.value || '').trim()
      : String(selectedNodeId.value || route.query.pageId || '').trim()
    // 个人工作台：编辑态用虚拟节点驱动自由布局画布
    if (editing.value && isWorkbenchPageId(preferredId))
      return createWorkbenchVirtualNode()
    if (isPageManagementSystemPageId(selectedNodeId.value) && !isWorkbenchPageId(preferredId))
      return null
    // 当前内容不能依赖侧栏的折叠状态；分组收起后仍应保留已选页面。
    // 客户端过滤只用于确定可访问范围，不能把 collapsedGroupIds 带进内容解析。
    const nodes = editing.value || canEditApplication.value
      ? builder.value?.nodes || []
      : filterNavigationNodesByClient(builder.value?.nodes || [], 'pc')
    // 页面管理态以左侧选中为准，避免 URL 残留 pageId 导致「高亮 A、预览 B」
    // 编辑态优先 URL pageId（与 designResource 对齐）
    const matched = nodes.find(item => item.id === preferredId)
      || (preferredId !== String(selectedNodeId.value || '')
        ? nodes.find(item => item.id === selectedNodeId.value)
        : null)
    if (matched)
      return matched
    const designTab = readRouteDesignTab()
    // 编辑态 URL 已明确指向某页时，不要静默落到首页对象页（刷新后会出现空白对象卡）
    if (editing.value && preferredId && ['page', 'list', 'form', 'settings', 'publish'].includes(designTab))
      return null
    if (editing.value && designTab === 'page')
      return null
    return (!editing.value ? null : nodes.find(item => item.id === builder.value?.homePageId)) || null
  })
  const currentPage = computed(() => {
    if (!currentNode.value)
      return null
    if (isWorkbenchPageId(currentNode.value.id))
      return builder.value?.pages?.[WORKBENCH_PAGE_ID] || null
    return builder.value?.pages?.[currentNode.value.id] || null
  })
  const currentPageLayoutPolluted = computed(() => {
    if (!canEditApplication.value || !currentNode.value)
      return false
    return isObjectBoundPageLayoutPolluted(currentNode.value, currentPage.value)
  })
  const currentPageManagementTitle = computed(() => currentSystemPage.value?.title || currentNode.value?.title || '页面管理')
  /** 顶栏「个人工作台」胶囊选中：与标题互斥，避免文案重复 */
  const isRuntimeWorkbenchActive = computed(() => (
    runtimeViewMode.value === 'pages'
    && isWorkbenchPageId(selectedNodeId.value)
  ))
  const pageDesignObject = computed(() => {
    const objectRef = currentNode.value?.objectRef
    if (!objectRef?.objectId && !objectRef?.objectCode)
      return null
    return objects.value.find(item => String(item.objectId) === String(objectRef.objectId) || item.objectCode === objectRef.objectCode)
      || {
        objectId: objectRef.objectId,
        objectCode: objectRef.objectCode,
        objectName: objectRef.objectName,
      }
  })
  const currentGridLayout = computed(() => {
    const layout = currentPage.value?.layout || {}
    if (layout.gridLayout && Array.isArray(layout.gridLayout.items))
      return layout.gridLayout
    return {
      cols: 12,
      rowHeight: 32,
      gap: 8,
      designWidth: 1366,
      layoutType: 'simple-crud',
      items: (layout.items || []).map((item, index) => createLegacyBlock(item, index)).filter(Boolean),
    }
  })
  const pageBlocks = computed(() => currentGridLayout.value.items || [])
  const pageFlowStackMode = computed(() => {
    const pageShape = String(currentNode.value?.pageShape || currentNode.value?.pageTemplate || '').trim().toLowerCase()
    return shouldUsePageFlowStack(pageBlocks.value, {
      pageId: String(route.query.pageId || selectedNodeId.value || currentNode.value?.id || ''),
      pageShape,
      // 自由布局页强制绝对定位，保证「左 X / 上 Y」可调
      forceAbsolute: pageShape === 'custom',
    })
  })
  const pageCanvasPadding = computed(() => normalizePagePadding(
    currentGridLayout.value?.pagePadding ?? currentPage.value?.layout?.pagePadding,
    DEFAULT_PAGE_PADDING,
  ))
  const pageCanvasPaddingCss = computed(() => resolvePagePaddingCss(pageCanvasPadding.value))
  const pageCanvasSurface = computed(() => normalizePageSurface(
    currentGridLayout.value?.pageSurface ?? currentPage.value?.layout?.pageSurface,
    DEFAULT_PAGE_SURFACE,
  ))
  const pageCanvasSurfaceStyle = computed(() => resolvePageSurfaceStyle(pageCanvasSurface.value))
  /** 宿主 .page-surface 铺整页背景，避免内容区以下露出默认白底 */
  const pageSurfaceHostStyle = computed(() => {
    const bg = pageCanvasSurfaceStyle.value?.backgroundColor
    return bg ? { backgroundColor: bg } : undefined
  })
  const designerGridLayout = computed(() => currentGridLayout.value)
  /** 跳转目标：系统页（我的待办/已办等）+ 应用内自定义页；系统页在前便于挂件选型 */
  const appPageTargetOptions = computed(() => {
    const systemPages = PAGE_MANAGEMENT_SYSTEM_PAGES.map(page => ({
      pageKey: page.id,
      pageName: page.title,
      pageType: '系统',
    }))
    const customPages = (builder.value?.nodes || [])
      .filter(node => node && node.id && !isWorkbenchPageId(node.id) && !isPageManagementSystemPageId(node.id))
      .map(node => ({
        pageKey: String(node.id),
        pageName: node.title || node.name || String(node.id),
        pageType: node.pageType || node.type || 'custom',
      }))
    return [...systemPages, ...customPages]
  })
  const pageFlowHeight = computed(() => {
    // 通栏文档流：高度随内容走，只保底可拖放区域
    if (pageFlowStackMode.value) {
      const editingFloor = typeof window !== 'undefined'
        ? Math.max(960, Math.round(window.innerHeight - 120))
        : 960
      return editing.value ? editingFloor : 680
    }
    const pad = pageCanvasPadding.value
    const useContentCoords = currentGridLayout.value?.pageFlowCoordSpace === 'content'
    const contentBottom = pageBlocks.value.reduce((bottom, block, index) => {
      const y = Number(block.props?.style?.pageFlowY)
      const height = Number(block.props?.style?.pageFlowHeight)
      const contentY = Number.isFinite(y) && y >= 0 ? y : resolveDefaultPageBlockYFromItems(pageBlocks.value, index)
      const canvasTop = useContentCoords
        ? pad.top + contentY
        : (Number.isFinite(y) && y >= 0 ? y : Math.max(pad.top, contentY))
      return Math.max(bottom, canvasTop + (height > 0 ? height : resolveDefaultPageBlockHeight(block)))
    }, pad.top)
    // 下边距计入画布高度，编辑态再预留可拖放空白
    const editingFloor = typeof window !== 'undefined'
      ? Math.max(960, Math.round(window.innerHeight - 120))
      : 960
    return Math.max(
      contentBottom + pad.bottom + (editing.value ? 280 : 48),
      editing.value ? editingFloor : 680,
    )
  })
  const hasCustomComponentButtonPosition = computed(() => Number.isFinite(componentButtonPosition.value.x) && Number.isFinite(componentButtonPosition.value.y))
  const componentButtonStyle = computed(() => {
    // 默认靠 CSS sticky 贴可视区底；自定义拖拽才写 absolute 坐标
    if (!hasCustomComponentButtonPosition.value)
      return {}
    return {
      left: `${componentButtonPosition.value.x}px`,
      top: `${componentButtonPosition.value.y}px`,
    }
  })
  const selectedPageBlock = computed(() => findPageBlockInTree(pageBlocks.value, selectedPageBlockId.value))
  const formAssets = computed(() => builder.value?.formAssets || [])
  const filteredFormAssets = computed(() => {
    const keyword = formAssetSelectorKeyword.value.trim().toLowerCase()
    return !keyword
      ? formAssets.value
      : formAssets.value.filter(asset => `${asset.name || ''}${asset.id || ''}`.toLowerCase().includes(keyword))
  })
  const pageFormAssetOptions = computed(() => formAssets.value
    .map((asset) => {
      const value = String(asset?.id || asset?.formKey || '').trim()
      if (!value)
        return null
      const fieldCount = (() => {
        try {
          return resolveFormAssetFields(asset).length
        }
        catch {
          return 0
        }
      })()
      return {
        label: `${asset.name || asset.formKey || value}（${fieldCount} 个字段）`,
        value,
      }
    })
    .filter(Boolean))
  // 兼容 id / formKey，避免页面资源点开后 activeFormAsset 对不上导致白屏
  const activeFormAsset = computed(() => {
    const key = String(activeFormAssetId.value || '').trim()
    if (!key)
      return null
    return formAssets.value.find(asset => String(asset?.id || '').trim() === key
      || String(asset?.formKey || '').trim() === key) || null
  })
  // activeFormDesignerSchema：从 computed 改为 ref + watch 签名去重。
  // 原因：normalizeFormDesignerSchema 每次做深拷贝（JSON.parse/JSON.stringify），
  // computed 每次求值都产生新对象引用 → 子组件反复重新渲染 → watch 链回环 →
  // "Maximum recursive updates exceeded"。改为 ref 后只在内容真正变化时才更新引用。
  const activeFormDesignerSchema = ref(normalizeFormDesignerSchema({}))

  watch(
    () => activeFormAsset.value?.formDesignerSchema,
    (source) => {
      const normalized = presentFormDesignerSchema(source || {})
      const signature = JSON.stringify(normalized)
      if (signature !== mut._activeFormDesignerSchemaSignature) {
        mut._activeFormDesignerSchemaSignature = signature
        activeFormDesignerSchema.value = normalized
      }
    },
    { immediate: true },
  )
  // 表单资产被区块引用后，设计器按该区块绑定的业务对象补齐关系/动作上下文。
  const activeFormAssetBlock = computed(() => {
    const asset = activeFormAsset.value
    if (!asset && !activeFormAssetId.value)
      return null
    const keys = new Set([
      String(asset?.id || '').trim(),
      String(asset?.formKey || '').trim(),
      String(activeFormAssetId.value || '').trim(),
    ].filter(Boolean))
    if (!keys.size)
      return null
    let matched = null
    visitPageBlocksInTree(pageBlocks.value, (block) => {
      if (matched || !isDataFieldBlockType(block.blockType))
        return
      const bound = String(block.props?.formAssetId || '').trim()
      if (bound && keys.has(bound))
        matched = block
    })
    return matched
  })
  const activeFormDesignerObjectRef = computed(() => {
    // 表单设计 Tab 默认不进 formDesignerMode；字段资产仍要按绑定对象补齐，
    // 否则货架只会从当前画布抽字段，「未使用」恒为空。
    const objectRef = resolvePageBlockObjectRef(activeFormAssetBlock.value || {})
    return isValidPageBlockObjectRef(objectRef) ? objectRef : null
  })
  const activeFormDesignerContext = computed(() => {
    const cacheKey = activeFormDesignerObjectRef.value ? resolveRuntimeObjectCacheKey(activeFormDesignerObjectRef.value) : ''
    return cacheKey ? formDesignerObjectContextByObjectId.value[cacheKey] || null : null
  })
  const activeFormDesignerRelations = computed(() => activeFormDesignerContext.value?.relations || [])
  const activeFormDesignerActions = computed(() => activeFormDesignerContext.value?.actions || [])
  const activeFormFields = computed(() => {
    const assetFields = activeFormAsset.value ? resolveFormAssetFields(activeFormAsset.value) : []
    const objectRef = activeFormDesignerObjectRef.value
    if (!objectRef) {
      // 未绑定对象时货架仍要去掉子表合成列，避免「字段资产」被明细列撑爆
      return filterPrimaryObjectDesignerFields(assetFields)
    }
    const cacheKey = resolveRuntimeObjectCacheKey(objectRef)
    const designerFields = formDesignerObjectContextByObjectId.value[cacheKey]?.fields || []
    const runtimeFields = runtimeCrudPropsByObjectId.value[cacheKey]?.fieldCatalog || []
    const objectFields = designerFields.length ? designerFields : runtimeFields
    const protectedObjectFields = objectRef.hasBusinessData === true
      ? objectFields.map(field => ({
          ...field,
          locked: true,
          fieldBinding: {
            ...(field.fieldBinding || {}),
            locked: true,
          },
        }))
      : objectFields
    // 对象字段目录是字段资产货架的事实源；画布字段只补充尚未保存的新字段。
    // 不能只用当前表单 schema 当字段资产，否则未使用列表恒为空，删除组件后也无法回到未使用。
    // 子表列（model__field）只给列表选列用，禁止进表单设计器字段资产，否则删拖子表会反复堆积并保存报重复。
    const merged = objectFields.length ? mergePageFieldCatalogs(assetFields, protectedObjectFields) : assetFields
    const subTableCodes = assetFields
      .filter(field => isChildScopedFormFieldAsset(field))
      .map(field => field.fieldCode || field.field)
    return filterPrimaryObjectDesignerFields(merged, subTableCodes)
  })
  const activeFormDataState = computed(() => formDataProvisioningByAssetId.value[activeFormAssetId.value] || { status: 'idle', message: '' })
  const selectedPageBlockFormAssetId = computed(() => {
    const bound = String(selectedPageBlock.value?.props?.formAssetId || '').trim()
    if (bound)
      return bound
    if (formAssets.value.length !== 1)
      return ''
    return String(formAssets.value[0]?.id || formAssets.value[0]?.formKey || '').trim()
  })
  /** 仅认组件上显式绑定的 formAssetId（不含「全页只有一个表单」的隐式兜底） */
  const selectedPageBlockBoundFormAssetId = computed(() => String(selectedPageBlock.value?.props?.formAssetId || '').trim())
  const selectedPageBlockFormAsset = computed(() => {
    const key = selectedPageBlockFormAssetId.value
    if (!key)
      return null
    return formAssets.value.find(asset => String(asset?.id || asset?.formKey || '').trim() === key) || null
  })
  const selectedPageBlockBoundFormAsset = computed(() => {
    const key = selectedPageBlockBoundFormAssetId.value
    if (!key)
      return null
    return formAssets.value.find(asset => String(asset?.id || asset?.formKey || '').trim() === key) || null
  })
  const selectedPageBlockRuntimeObjectRef = computed(() => selectedPageBlock.value ? resolvePageBlockObjectRef(selectedPageBlock.value) : null)
  // 基础列表同样绑定对象 CRUD，数据 Tab 需展示「设计数据表单」等能力
  const selectedPageBlockIsCrud = computed(() => ['AiCrudPage', 'data-table'].includes(selectedPageBlock.value?.blockType))
  const selectedPageBlockSupportsDataSource = computed(() => isDataFieldBlockType(selectedPageBlock.value?.blockType))
  const selectedPageBlockUsesObjectRuntime = computed(() => {
    if (!selectedPageBlockSupportsDataSource.value)
      return false
    return isValidPageBlockObjectRef(selectedPageBlockRuntimeObjectRef.value)
  })
  const selectedPageBlockRuntimeObjectId = computed(() => String(selectedPageBlockRuntimeObjectRef.value?.objectId ?? selectedPageBlockRuntimeObjectRef.value?.id ?? ''))
  const selectedPageBlockObjectDesignerPanel = computed(() => {
    if (selectedPageBlock.value?.blockType === 'AiForm')
      return 'form'
    if (selectedPageBlock.value?.blockType === 'detail-info')
      return 'detail'
    return 'list'
  })
  const selectedPageBlockFormDataState = computed(() => formDataProvisioningByAssetId.value[selectedPageBlockFormAssetId.value] || { status: 'idle', message: '' })
  const runtimeObjectFormOptions = computed(() => objects.value
    .filter(item => item.objectId ?? item.id)
    .map(item => ({
      value: String(item.objectId ?? item.id),
      label: item.objectName || item.objectCode || '未命名表单',
    })))
  const pageTemplateOptions = computed(() => inAppPageTemplateCatalog.filter(template => ['blank', 'intro', 'crud', 'tree-list', 'tree-table', 'master-detail'].includes(template.key)))
  const freeLayoutPresetOptions = computed(() => freeLayoutPresetCatalog.map(item => ({
    key: item.key,
    label: item.label,
    description: item.description,
    thumb: item.thumb,
  })))
  const selectedPageBlockFields = computed(() => {
    if (!selectedPageBlock.value)
      return []
    const blockFields = resolvePageBlockFields(selectedPageBlock.value)
    if (blockFields.length)
      return blockFields
    return resolvePageContextFieldCatalog()
  })
  const selectedPageBlockFormDesignerSchema = computed(() => {
    const formAssetId = selectedPageBlock.value?.props?.formAssetId
      || (formAssets.value.length === 1 ? formAssets.value[0]?.id : '')
    const asset = formAssets.value.find(item => item.id === formAssetId)
      || formAssets.value[0]
    return asset?.formDesignerSchema || null
  })

  function resolvePageContextFieldCatalog() {
    const merged = []
    const seen = new Set()
    formAssets.value.forEach((asset) => {
      resolveFormAssetFields(asset).forEach((field) => {
        const code = field.fieldCode || field.field
        if (!code || seen.has(code))
          return
        seen.add(code)
        merged.push(field)
      })
    })
    return merged
  }
  __impl.resolvePageContextFieldCatalog = resolvePageContextFieldCatalog
  const applicationGridModelSchema = computed(() => {
    // ListPageGridDesigner 会在 modelSchema 改变时同步并回写整个布局。
    // 这里必须保持页面级模型稳定；当前区块切换数据源仅更新 fields prop，避免循环回写卡死。
    const objectRef = resolvePageBlockObjectRef({})
    const cacheKey = resolveRuntimeObjectCacheKey(objectRef)
    const pageRuntimeFields = runtimeCrudPropsByObjectId.value[cacheKey]?.fieldCatalog || []
    return {
      businessName: currentNode.value?.title || '应用页面',
      // 列表设计器用 configKey 生成 /ai/crud/{configKey} 的默认接口。
      // 应用页必须把业务对象的真实配置键带过去，不能触发“当前配置”占位兜底。
      configKey: objectRef?.configKey || '',
      objectCode: objectRef?.objectCode || '',
      object: objectRef
        ? {
            code: objectRef.objectCode || '',
            configKey: objectRef.configKey || '',
          }
        : undefined,
      fields: pageRuntimeFields,
    }
  })
  const dragPreviewBlock = computed(() => dragPreview.value
    ? findPageBlockInTree(pageBlocks.value, dragPreview.value.blockId) || null
    : null)
  const nestedMovingPageBlockId = ref('')

  const copyBlockPageOptions = computed(() => flattenNodes(builder.value?.nodes || [])
    .filter(node => node.type === 'page' && node.id !== currentNode.value?.id)
    .map(node => ({ label: `${'　'.repeat(node.depth || 0)}${node.title}`, value: node.id })))
  const dirty = computed(() => JSON.stringify(builder.value || {}) !== savedSignature.value)

  /** 表单设计器保存：草稿有改动、DDL/对象落库失败重试、或首次尚未创建对象时都可点。 */
  const canSaveActiveFormDesigner = computed(() => {
    if (activeFormDataState.value.status === 'error')
      return true
    if (dirty.value || embeddedDesignerDirty.value)
      return true
    // 新建页面后草稿已落盘但业务对象尚未创建，必须允许首次「保存应用页面设计」
    if (activePageShapeDesign.value && !activePageShapeDesign.value.objectId)
      return true
    return false
  })

  /**
   * 保存后对齐草稿签名。部分 watch/子组件会在 nextTick 后继续归一化 builder，
   * 只写一次签名会立刻再次变脏，表现为「保存成功但退出仍提示未保存」。
   */
  async function markBuilderClean() {
    savedSignature.value = JSON.stringify(builder.value || {})
    await nextTick()
    savedSignature.value = JSON.stringify(builder.value || {})
    embeddedDesignerDirty.value = false
  }
  __impl.markBuilderClean = markBuilderClean

  const {
    historyReady,
    canUndo,
    canRedo,
    resetBuilderHistory,
    runWithoutHistory,
    undoBuilder,
    redoBuilder,
    handleBuilderShortcut,
  } = useBuilderHistory({
    builder,
    scheduleNavigationSave,
    // 快捷键仅在编辑态且非表单设计器时生效
    isHistoryActive: () => editing.value && !formDesignerMode.value,
  })
  const loadErrorTitle = computed(() => isDraftMode.value ? '应用草稿加载失败' : '应用暂不可访问')
  const filteredComponents = computed(() => listInsertablePageBlocks({
    keyword: componentKeyword.value,
  }))
  const componentPickerGroups = computed(() => buildPageBlockPickerGroups({
    keyword: componentKeyword.value,
  }))

  function resolveComponentPickerGroup(item = {}) {
    return resolvePageBlockPickerGroup(item)
  }
  __impl.resolveComponentPickerGroup = resolveComponentPickerGroup

  function resolveComponentIcon(item = {}) {
    const fileName = formComponentIconFileByBlockType[item.blockType]
    if (!fileName)
      return ''
    const path = `/src/assets/images/form/${fileName}.png`
    if (formComponentIconCache[path])
      return formComponentIconCache[path]
    const loader = formComponentIconModules[path]
    if (!loader)
      return ''
    // 组件面板图标按需加载，避免运行页首屏同步打入全部 PNG。
    void loader().then((url) => {
      formComponentIconCache[path] = url
    })
    return formComponentIconCache[path] || ''
  }
  __impl.resolveComponentIcon = resolveComponentIcon

  function resolveEmptyGuideIcon(item = {}) {
    const icons = {
      'page-title': TextOutline,
      'stats-strip': StatsChartOutline,
      'workspace-summary-metrics': StatsChartOutline,
      'AiCrudPage': ListOutline,
      'AiForm': ReaderOutline,
      'custom-html': DocumentTextOutline,
      'info-panel': InformationCircleOutline,
    }
    return icons[item.blockType] || SquareOutline
  }
  __impl.resolveEmptyGuideIcon = resolveEmptyGuideIcon

  const recommendedComponents = computed(() => {
    const recommendedTypes = [
      'workflow-task-list',
      'notice-board',
      'top-ranking',
      'stats-strip',
      'welcome-banner',
      'quick-entry',
      'AiCrudPage',
      'page-title',
    ]
    return recommendedTypes
      .map(blockType => resolveListPageBlockMeta(blockType))
      .filter(Boolean)
      .slice(0, 8)
  })

  // NOTE: applicationRuntimeLoadCoordinator.run watch with immediate:true lives in part5
  // so all __impl handlers exist before the first load() races ahead of later parts.
  watch(() => route.query.designResource, (resourceKey) => {
    selectedDesignerResourceKey.value = String(resourceKey || '')
  })
  watch(() => route.query.designTab, (tab) => {
    const next = resolvePageDesignTab(tab, String(route.query.pageId || selectedNodeId.value || '').trim())
    if (activePageDesignTab.value !== next)
      activePageDesignTab.value = next
  })
  // 应用节点加载完成后，按 URL designTab 重新对齐（修复刷新时 nodes 为空把 list 误写成 page）
  watch([editing, () => builder.value?.nodes, () => route.query.pageId, () => route.query.designTab], () => {
    if (!editing.value || !builder.value)
      return
    const tab = readRouteDesignTab()
    if (!tab)
      return
    const next = resolvePageDesignTab(tab, String(route.query.pageId || selectedNodeId.value || '').trim())
    if (activePageDesignTab.value !== next)
      activePageDesignTab.value = next
  }, { flush: 'post' })
  // 编辑态且 URL 未显式带 designTab 时，按页面内容对齐默认 Tab（避免自由布局预览却进表单/列表设计）
  watch([editing, () => builder.value?.nodes, selectedNodeId, () => route.query.designTab], () => {
    if (!editing.value || !builder.value)
      return
    if (readRouteDesignTab() !== '')
      return
    const pageId = String(route.query.pageId || selectedNodeId.value || '').trim()
    const next = resolveEntryDesignTab(pageId)
    if (activePageDesignTab.value !== next)
      activePageDesignTab.value = next
    // 伴生自由布局必须写入 designTab=page，否则顶栏不出「页面设计」、形态仍按 list 打开列表壳
    if (next === 'page') {
      router.replace({
        query: {
          ...route.query,
          designTab: 'page',
        },
      })
    }
  })
  // designTab=page：强制页面设计 Tab，并回写 pageShape，避免节点形态丢失后只剩表单/列表
  watch([editing, () => route.query.designTab, () => route.query.pageId, () => builder.value?.nodes], () => {
    if (!editing.value || !builder.value)
      return
    if (readRouteDesignTab() !== 'page')
      return
    if (!['settings', 'publish'].includes(activePageDesignTab.value) && activePageDesignTab.value !== 'page')
      activePageDesignTab.value = 'page'
    const pageId = String(route.query.pageId || selectedNodeId.value || '').trim()
    if (!pageId)
      return
    // 自由布局 URL 不要 resolve 到首页；保持 URL 指向的 pageId
    if (selectedNodeId.value !== pageId)
      selectedNodeId.value = pageId
    const node = (builder.value.nodes || []).find(item => String(item.id) === pageId)
    if (!node || node.type !== 'page')
      return
    const repaired = ensureFreeLayoutPageNode(node)
    if (repaired === node)
      return
    builder.value = {
      ...builder.value,
      nodes: builder.value.nodes.map(item => String(item.id) === pageId ? repaired : item),
    }
  }, { flush: 'post' })
  watch(() => route.query.view, (view) => {
    // 发布功能已迁移到独立发布页
    if (String(view || '').toLowerCase() === 'publish') {
      router.replace({ name: 'BusinessApplicationPublish', params: { applicationCode: route.params.applicationCode } })
      return
    }
    const next = resolveRuntimeView(view)
    if (runtimeViewMode.value !== next)
      runtimeViewMode.value = next
  })
  watch(() => activeDesignerResource.value?.key, (activeKey) => {
    activeFlowContext.value = {}
    if (activeDesignerResource.value?.kind === 'page-custom' && activeDesignerResource.value.pageId) {
      const resourcePageId = String(activeDesignerResource.value.pageId)
      const routePageId = String(route.query.pageId || selectedNodeId.value || '').trim()
      // 预览/运行态不要用设计资源回写 pageId，否则会从工作台跳到第一个业务菜单
      if (!editing.value)
        return
      if (isPageManagementSystemPageId(routePageId) && resourcePageId !== routePageId)
        return
      if (resourcePageId !== String(selectedNodeId.value || ''))
        selectNode(resourcePageId)
    }
    const requestedKey = String(route.query.designResource || '')
    if (!editing.value || !activeKey || !requestedKey || requestedKey === activeKey)
      return
    // 本地已指向 requestedKey（新建页刚写入）时，不要用 fallback 资源盖回去
    if (String(selectedDesignerResourceKey.value || '') === requestedKey && requestedKey !== activeKey)
      return
    selectedDesignerResourceKey.value = activeKey
    router.replace({
      query: {
        ...route.query,
        designResource: activeKey,
      },
    })
  })
  watch(() => route.query.pageId, (pageId) => {
    if (!builder.value)
      return
    const requested = String(pageId || '').trim()
    const nextPageId = (editing.value && readRouteDesignTab() === 'page' && requested)
      ? requested
      : (isWorkbenchPageId(requested) || resolvePageManagementSystemPage(requested)
        ? requested
        : resolveSelectablePageId(pageId))
    if (nextPageId === selectedNodeId.value)
      return
    selectedNodeId.value = nextPageId
    selectedPageBlockId.value = ''
    if (editing.value && activePageDesignTab.value === 'form' && !formDesignerMode.value)
      syncActiveFormAssetForPage(nextPageId)
  })
  watch(() => currentNode.value?.id, () => {
    preloadCurrentPageCrudRuntimeProps()
  }, { flush: 'post' })
  watch(editing, (value) => {
    if (value) {
      if (route.query.edit === '1')
        return
      router.replace({ query: { ...route.query, edit: '1' } })
      return
    }
    // 退出编辑：清掉设计态参数，但保留 pageId，避免左侧选中与预览脱节
    const hasDesignerQuery = route.query.edit != null
      || route.query.designResource != null
      || route.query.designTab != null
      || route.query.designSection != null
    if (!hasDesignerQuery)
      return
    const nextQuery = { ...route.query }
    delete nextQuery.edit
    delete nextQuery.designResource
    delete nextQuery.designTab
    delete nextQuery.designSection
    router.replace({ query: nextQuery })
  })
  watch(editing, (value) => {
    // 从运行态切入编辑态时补加载编辑器画布需要的渲染配置
    if (value && builder.value)
      preloadCurrentPageCrudRuntimeProps()
  })
  watch(() => navigationActionForm.value.icon, (icon) => {
    if (navigationActionMode.value !== 'icon' || !navigationActionNode.value)
      return
    builder.value = {
      ...builder.value,
      nodes: builder.value.nodes.map(item => item.id === navigationActionNode.value.id ? { ...item, icon: String(icon || '') } : item),
    }
  })
  onMounted(() => {
    window.addEventListener('keydown', handleBuilderShortcut)
    const onVisible = () => {
      if (document.visibilityState === 'visible')
        scheduleNavigationBadgeRefresh()
    }
    document.addEventListener('visibilitychange', onVisible)
    mut.navigationBadgeVisibilityHandler = onVisible
  })
  onBeforeUnmount(() => {
    if (navigationBadgeTimer)
      window.clearTimeout(navigationBadgeTimer)
    if (mut.navigationBadgeVisibilityHandler) {
      document.removeEventListener('visibilitychange', mut.navigationBadgeVisibilityHandler)
      mut.navigationBadgeVisibilityHandler = null
    }
    endPageBlockResize()
    endPageBlockMove()
    endComponentButtonMove()
    window.removeEventListener('keydown', handleBuilderShortcut)
  })

  function pageHasCrudLikeBlocks(pageId = '') {
    const page = builder.value?.pages?.[String(pageId || '').trim()]
    const items = page?.layout?.gridLayout?.items || page?.layout?.items || []
    let found = false
    visitPageBlocksInTree(items, (block) => {
      if (found)
        return
      if (['AiCrudPage', 'AiForm', 'AiTable', 'data-table'].includes(block?.blockType))
        found = true
    })
    return found
  }

  /**
   * 按即将展示的设计入口预热 JS chunk，避免表单设计页去等自由布局/CRUD 大包。
   */
  function buildRuntimeWarmChunks({ editing: isEditing = false, entryTab = '', pageId = '' } = {}) {
    const safe = loader => loader().catch(() => {})
    if (!isEditing) {
      return [
        safe(() => import('@/components/lowcode-builder/page/GridBlockRenderer.vue')),
        safe(() => import('@/components/ai-form/AiCrudPage.vue')),
        safe(() => import('@/components/lowcode-builder/shared/PageWidgetRenderer.vue')),
        safe(() => import('@/components/lowcode-builder/page/RuntimeListGridFlow.vue')),
      ]
    }
    const tab = String(entryTab || '').trim()
    if (tab === 'form') {
      return [
        safe(() => import('@/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue')),
      ]
    }
    if (tab === 'list') {
      return [
        safe(() => import('@/components/lowcode-builder/page/ListPageGridDesigner.vue')),
        safe(() => import('@/views/app-center/components/designer/BusinessListDesigner.vue')),
      ]
    }
    // 自由布局 / 页面设计：画布优先；仅有 CRUD 块时才预拉 AiCrudPage
    const chunks = [
      safe(() => import('@/components/lowcode-builder/page/GridBlockRenderer.vue')),
      safe(() => import('@/components/lowcode-builder/shared/PageWidgetRenderer.vue')),
    ]
    if (pageHasCrudLikeBlocks(pageId)) {
      chunks.push(safe(() => import('@/components/ai-form/AiCrudPage.vue')))
      chunks.push(safe(() => import('@/components/lowcode-builder/page/RuntimeListGridFlow.vue')))
    }
    // 设置面板不挡首屏，空闲再拉
    if (typeof window !== 'undefined') {
      const prefetchSettings = () => {
        void import('@/views/app-center/components/designer/PageDesignSettingsPanel.vue').catch(() => {})
      }
      if (typeof window.requestIdleCallback === 'function')
        window.requestIdleCallback(prefetchSettings, { timeout: 2500 })
      else
        window.setTimeout(prefetchSettings, 800)
    }
    return chunks
  }

  async function load() {
    const code = String(route.params.applicationCode || '')
    if (!code)
      return
    loading.value = true
    loadError.value = ''
    historyReady.value = false
    portalCrudSeed.value = {}
    resetRuntimeCrudConfig()
    runtimeTreeFilter.value = {}
    runtimeTreeFilterByBlockId.value = {}
    runtimeTreeActiveKeyByBlockId.value = {}
    // 首轮只起一个空 Promise；builder 就绪后再按入口 Tab 定向预热，避免表单设计页去等自由布局 chunk。
    let warmChunksPromise = Promise.resolve()
    try {
      const response = shouldUseApplicationWorkspaceLoad(route, canEditApplication.value)
        ? await businessApplicationWorkspaceByCode(code)
        : await businessApplicationRuntimeByCode(code)
      const payload = response.data || {}
      application.value = payload.application || null
      objects.value = payload.objects || []
      workspaceExtensions.value = payload.extensions || []
      workspaceEntries.value = payload.entries || []
      builder.value = ensurePageTitleComponents(normalizeInAppBuilder(application.value?.options, application.value, objects.value))
      const ensuredWorkbench = ensureWorkbenchPageInBuilder(builder.value)
      builder.value = ensuredWorkbench.schema
      hydratePageCrudApiPlaceholders()
      bindSingleFormToCompatibleBlocks()
      savedSignature.value = JSON.stringify(builder.value)
      resetBuilderHistory(builder.value)
      if (hasPendingLegacyObjectPageMigration(application.value?.options, builder.value)) {
        try {
          await persistApplicationDraft()
          savedSignature.value = JSON.stringify(builder.value)
          message.info('已将旧版业务对象页面恢复到新版页面结构')
        }
        catch (error) {
          message.warning(error?.message || '旧版业务对象页面已恢复到当前页面，但草稿自动保存失败，请手动保存')
        }
      }
      const requestedPageId = String(route.query.pageId || '').trim()
      const designTab = readRouteDesignTab()
      const requestedNodeExists = Boolean(
        requestedPageId
        && (builder.value?.nodes || []).some(item => String(item.id) === requestedPageId),
      )
      // 编辑态带 designTab / 节点已存在时，保留 URL pageId，避免刷新落到首页空白对象卡
      selectedNodeId.value = (
        resolvePageManagementSystemPage(requestedPageId)
        || (requestedPageId && designTab === 'page')
        || (requestedPageId && ['list', 'form', 'settings', 'publish'].includes(designTab))
        || requestedNodeExists
      )
        ? requestedPageId
        : resolveSelectablePageId(route.query.pageId)
      if (editing.value)
        syncActiveFormAssetForPage(selectedNodeId.value)
      await nextTick()

      // 按即将展示的入口定向预热：表单页拉 ForgeFormDesigner，自由布局拉画布，列表页拉列表设计器
      const entryPageId = String(selectedNodeId.value || '').trim()
      const entryTab = editing.value
        ? (designTab || resolveEntryDesignTab(entryPageId))
        : ''
      warmChunksPromise = Promise.all(buildRuntimeWarmChunks({
        editing: editing.value,
        entryTab,
        pageId: entryPageId,
      }))

      // 非编辑态：CRUD 预热与整页骨架并行收束；超时后先出壳，Portal 内部继续用 seed/自拉
      const portalWarmPromise = (!editing.value && !isPageManagementSystemPageId(selectedNodeId.value))
        ? warmCurrentPortalPageCrud()
        : Promise.resolve({})
      if (canEditApplication.value)
        prefetchRuntimeWorkspacePanels()
      // 编辑态：等对口 chunk，但最多卡 900ms，超时先出壳（慢包后台继续）
      if (editing.value) {
        await Promise.race([
          warmChunksPromise,
          new Promise(resolve => setTimeout(resolve, 900)),
        ])
      }
      const seeded = await portalWarmPromise
      if (seeded && typeof seeded === 'object')
        portalCrudSeed.value = seeded
      preloadCurrentPageCrudRuntimeProps()
      scheduleNavigationBadgeRefresh()
    }
    catch (error) {
      application.value = null
      objects.value = []
      builder.value = null
      selectedNodeId.value = ''
      portalCrudSeed.value = {}
      editCanvasBootstrapping.value = false
      loadError.value = String(
        error?.message
        || error?.detail?.rawMessage
        || error?.error?.message
        || '应用配置加载失败，请稍后重试。',
      )
    }
    finally {
      const pageId = String(selectedNodeId.value || route.query.pageId || '').trim()
      const entryTab = editing.value ? resolveEntryDesignTab(pageId) : ''
      // 编辑态：自由布局 / 表单 / 列表都要盖中间过渡，避免顶栏出来后中间纯白好几秒
      const surfaceBoot = editing.value && ['page', 'form', 'list'].includes(entryTab)
      // 编辑态多等一会入口 chunk；非编辑仍短等，避免空白门户
      await Promise.race([
        warmChunksPromise,
        new Promise(resolve => setTimeout(resolve, surfaceBoot ? 1600 : 200)),
      ])
      if (surfaceBoot) {
        editCanvasBootstrapping.value = true
        // 提前激活保活，否则 keepFormDesignerAlive 为 false 时中间既无画布也无 loader
        if (entryTab === 'form')
          formDesignerSessionActive.value = true
        if (entryTab === 'list')
          listDesignerSessionActive.value = true
      }
      loading.value = false
      if (surfaceBoot) {
        await nextTick()
        await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))
        // 等到设计器根节点真正挂上再撤过渡层；超时也撤，避免一直挡操作
        await waitForEditSurfaceReady(entryTab, entryTab === 'page' ? 600 : 4500)
        editCanvasBootstrapping.value = false
      }
    }
  }
  __impl.load = load

  /**
   * 编辑入口过渡：等目标表面 DOM 出现，避免顶栏已出、中间仍纯白。
   */
  async function waitForEditSurfaceReady(entryTab = '', timeoutMs = 2000) {
    if (typeof document === 'undefined')
      return
    const selector = entryTab === 'form'
      ? '.application-form-asset-designer .forge-form-designer'
      : entryTab === 'list'
        ? '.application-design-section'
        : '.application-page-flow'
    const startedAt = Date.now()
    while (Date.now() - startedAt < timeoutMs) {
      if (document.querySelector(selector))
        return
      await new Promise(resolve => setTimeout(resolve, 50))
    }
  }

  async function warmCurrentPortalPageCrud() {
    const pageId = String(selectedNodeId.value || '').trim()
    if (!pageId || !builder.value)
      return {}
    const node = (builder.value.nodes || []).find(item => String(item?.id || '') === pageId) || null
    const page = builder.value.pages?.[pageId] || null
    if (!node && !page)
      return {}
    const targets = collectPortalPageCrudTargets({
      page,
      node,
      objects: objects.value,
      entries: workspaceEntries.value,
    })
    if (!targets.length)
      return {}
    return warmPortalPageCrudProps(targets, {
      // 工作台编辑者预览草稿配置；正式门户路由不会走这条预热
      designPreview: usePortalDesignPreview.value,
      applicationId: application.value?.id,
      pageId,
      // 草稿/编辑预热不宜久等：超时先出壳，Portal 内继续拉（超时过长会被当成「加载资源卡死」）
      timeoutMs: editing.value || isDraftMode.value ? 2400 : 3200,
    })
  }
  __impl.warmCurrentPortalPageCrud = warmCurrentPortalPageCrud

  function selectNode(nodeId) {
    const nextId = nodeId || ''
    if (String(nextId) !== String(selectedNodeId.value || '')) {
      runtimeTreeFilter.value = {}
      runtimeTreeFilterByBlockId.value = {}
      runtimeTreeActiveKeyByBlockId.value = {}
    }
    selectedNodeId.value = nextId
    selectedPageBlockId.value = ''
    // 保留路由现有 edit 参数，选择页面不应切换编辑/运行模式，
    // 也不要用可能过期的 editing.value 重建，避免覆盖并发导航中的 edit 参数。
    const nextQuery = {
      ...route.query,
      pageId: nextId || undefined,
    }
    // 编辑态下 pageId 与 designResource 必须同指一页，否则会改到旧页
    if (nextId && (editing.value || route.query.edit === '1')) {
      const resourceKey = `page-custom:${nextId}`
      selectedDesignerResourceKey.value = resourceKey
      nextQuery.designResource = resourceKey
    }
    router.replace({ query: nextQuery })
  }
  __impl.selectNode = selectNode

  function resolveSelectablePageId(pageId) {
    const nodes = editing.value || canEditApplication.value
      ? builder.value?.nodes || []
      : filterNavigationNodesByClient(builder.value?.nodes || [], 'pc')
    return resolvePageManagementSelection(
      nodes,
      pageId,
      builder.value?.homePageId,
    )
  }
  __impl.resolveSelectablePageId = resolveSelectablePageId

  function ensurePageTitleComponents(schema) {
    if (!schema?.pages || !Array.isArray(schema.nodes))
      return schema
    const nodeMap = new Map(schema.nodes.map(node => [node.id, node]))
    let changed = false
    const pages = Object.fromEntries(Object.entries(schema.pages).map(([pageId, page]) => {
      const node = nodeMap.get(pageId)
      if (node?.type !== 'page' || page?.layout?.pageTitleComponentInitialized)
        return [pageId, page]
      // 不再自动添加 page-title 区块，仅标记为已初始化
      changed = true
      return [pageId, {
        ...page,
        layout: {
          ...(page?.layout || {}),
          pageTitleComponentInitialized: true,
        },
      }]
    }))
    return changed ? { ...schema, pages } : schema
  }
  __impl.ensurePageTitleComponents = ensurePageTitleComponents

  function createQuickNode(type = 'page', parentId = null) {
    const normalizedType = type === 'group' ? 'group' : 'page'
    if (normalizedType === 'page') {
      openPageTypeSelector(parentId)
      return
    }
    const previousIds = new Set(builder.value.nodes.map(item => item.id))
    builder.value = createNavigationNode(builder.value, {
      type: normalizedType,
      title: resolveNextNavigationTitle(normalizedType),
      pageType: 'content',
      parentId,
    })
    const created = builder.value.nodes.find(item => !previousIds.has(item.id))
    newNodePopoverVisible.value = false
    // 确保父分组展开，使新建的子节点可见
    if (parentId && collapsedGroupIds.value.has(parentId)) {
      const next = new Set(collapsedGroupIds.value)
      next.delete(parentId)
      collapsedGroupIds.value = next
    }
    if (created?.type === 'page')
      selectNode(created.id)
    // 页面组仅在内存中创建，后续进入编辑模式会触发路由变化和后端重载，
    // 必须先持久化草稿否则新建的页面组会被重载覆盖。
    if (created?.type === 'group')
      saveDraft()
  }
  __impl.createQuickNode = createQuickNode

  function toggleGroupExpanded(groupId) {
    const next = new Set(collapsedGroupIds.value)
    if (next.has(groupId))
      next.delete(groupId)
    else
      next.add(groupId)
    collapsedGroupIds.value = next
  }
  __impl.toggleGroupExpanded = toggleGroupExpanded

  function isGroupCollapsed(groupId) {
    return collapsedGroupIds.value.has(groupId)
  }
  __impl.isGroupCollapsed = isGroupCollapsed

  function openPageTypeSelector(parentId = null, defaultPageType = 'form') {
    pageTypeSelectorParentId.value = parentId || null
    pageTypeSelectorDefaultType.value = defaultPageType || 'form'
    pageTypeSelectorVisible.value = true
    newNodePopoverVisible.value = false
    // 页面管理态若 URL 仍残留旧 designResource，先清掉，避免确认创建后串回旧页
    if (!editing.value) {
      selectedDesignerResourceKey.value = ''
      if (route.query.designResource || route.query.designTab || route.query.edit) {
        const nextQuery = { ...route.query }
        delete nextQuery.designResource
        delete nextQuery.designTab
        delete nextQuery.designSection
        delete nextQuery.edit
        router.replace({ query: nextQuery })
      }
    }
    // 不要提前进入编辑态：弹窗应叠在当前运行页上，创建成功后再跳转到设计页。
    // 确保父分组展开
    if (parentId && collapsedGroupIds.value.has(parentId)) {
      const next = new Set(collapsedGroupIds.value)
      next.delete(parentId)
      collapsedGroupIds.value = next
    }
  }
  __impl.openPageTypeSelector = openPageTypeSelector

  async function handlePageTypeSelection(selection = {}) {
    // 父页面组由打开弹窗的当前操作上下文决定。不能使用选择器内部上次打开时缓存的 parentId，
    // 否则“新建页面组后立即新增页面”会把旧组传给 schema 校验。
    const parentId = pageTypeSelectorParentId.value == null || pageTypeSelectorParentId.value === ''
      ? null
      : String(pageTypeSelectorParentId.value)
    if (parentId && !builder.value?.nodes?.some(node => String(node.id) === parentId && node.type === 'group')) {
      message.warning('目标页面组已不存在，请在页面树中重新选择页面组')
      return
    }
    designerTransitionLoading.value = true
    try {
      const result = createPageShapeBuilder(builder.value, {
        ...selection,
        parentId,
      })
      builder.value = result.schema
      pageTypeSelectorVisible.value = false
      pageTypeSelectorParentId.value = null
      const customPage = result.selection?.pageType === 'custom' || !result.formAssetId
      if (customPage)
        activePageDesignTab.value = 'page'
      // 先落盘草稿再进编辑：避免进入 edit 时若触发重载，服务端还没有新页
      try {
        await persistApplicationDraft()
        savedSignature.value = JSON.stringify(builder.value)
      }
      catch (persistError) {
        message.warning(persistError?.message || '页面已创建，但草稿暂未保存成功，请稍后手动保存')
      }
      selectCreatedDesignerPage(result.pageId, { designTab: customPage ? 'page' : undefined })
      if (customPage && selection.layoutPresetKey)
        applyFreeLayoutPreset(result.pageId, selection.layoutPresetKey, {
          replace: true,
          pageTitle: selection.pageName,
        })
      if (!result.formAssetId)
        return
      activePageShapeDesign.value = {
        pageId: result.pageId,
        pageType: result.selection.pageType,
        formAssetId: result.formAssetId,
        objectId: null,
        objectCode: result.selection.objectCode,
        objectName: result.selection.objectName,
        runtimeDatasourceId: result.selection.runtimeDatasourceId || null,
        createMode: result.selection.createMode || '',
        importDatasourceId: result.selection.importDatasourceId || null,
        importTableName: result.selection.importTableName || '',
      }
      activeFormAssetId.value = result.formAssetId
      selectedPageBlockId.value = builder.value.pages?.[result.pageId]?.layout?.gridLayout?.items?.[0]?.id || ''
      formDesignerMode.value = true
      await nextTick()
    }
    catch (error) {
      message.error(error?.message || '页面创建失败，请刷新后重试')
    }
    finally {
      designerTransitionLoading.value = false
    }
  }
  __impl.handlePageTypeSelection = handlePageTypeSelection

  function selectIntroTemplate(templateKey) {
    selectedPageTemplateKey.value = templateKey
    createPageFromTemplate(templateKey)
  }
  __impl.selectIntroTemplate = selectIntroTemplate

  function resolvePageTemplateIcon(template = {}) {
    const icons = {
      'blank': DocumentTextOutline,
      'intro': InformationCircleOutline,
      'crud': ListOutline,
      'tree-list': GitNetworkOutline,
      'tree-table': GitBranchOutline,
      'master-detail': ReaderOutline,
    }
    return icons[template.key] || DocumentTextOutline
  }
  __impl.resolvePageTemplateIcon = resolvePageTemplateIcon

  function createPageFromTemplate(templateKey = selectedPageTemplateKey.value, initialBlockType = '', parentId = null) {
    const template = resolveInAppPageTemplate(templateKey)
    const previousIds = new Set(builder.value.nodes.map(item => item.id))
    builder.value = createNavigationNode(builder.value, {
      type: 'page',
      title: resolveNextNavigationTitle('page'),
      parentId,
      pageType: template.dataTemplate ? 'content' : template.pageType || 'content',
      pageTemplate: template.key,
      pageShape: (template.dataTemplate || template.pageType === 'content' || template.key === 'blank' || template.key === 'intro')
        ? 'custom'
        : (['form', 'list', 'list-form', 'tree-list', 'tree-table'].includes(template.key) ? template.key : 'custom'),
    })
    const created = builder.value.nodes.find(item => !previousIds.has(item.id))
    if (created) {
      const freeLayout = created.pageType !== 'object'
      selectCreatedDesignerPage(created.id, { designTab: freeLayout ? 'page' : undefined })
      applyPageTemplate(created.id, template.key, initialBlockType)
      if (template.dataTemplate || initialBlockType === 'AiCrudPage')
        createFormAssetForPageCrud(created.id)
    }
  }
  __impl.createPageFromTemplate = createPageFromTemplate

  function selectCreatedDesignerPage(pageId, options = {}) {
    const resourceKey = `page-custom:${pageId}`
    selectedNodeId.value = pageId
    selectedPageBlockId.value = ''
    selectedDesignerResourceKey.value = resourceKey
    formDesignerMode.value = false
    newNodePopoverVisible.value = false
    const designTab = options.designTab === 'page'
      ? 'page'
      : options.designTab || undefined
    if (designTab)
      activePageDesignTab.value = designTab
    else if (!options.designTab)
      activePageDesignTab.value = 'form'
    // 只走一次路由更新。不要先写 editing.value=true：
    // watch(editing) 会用当时仍带旧 pageId 的 route.query 再 replace，把新建页盖回旧页。
    router.replace({
      query: {
        ...route.query,
        edit: '1',
        designResource: resourceKey,
        designSection: undefined,
        designTab,
        pageId,
        view: undefined,
      },
    })
  }
  __impl.selectCreatedDesignerPage = selectCreatedDesignerPage

  __impl.prefetchRuntimeWorkspacePanels = prefetchRuntimeWorkspacePanels
  __impl.resolveSystemPageIcon = resolveSystemPageIcon
  __impl.readRouteDesignTab = readRouteDesignTab
  __impl.resolvePageContextFieldCatalog = resolvePageContextFieldCatalog
  __impl.markBuilderClean = markBuilderClean
  __impl.resolveComponentPickerGroup = resolveComponentPickerGroup
  __impl.resolveComponentIcon = resolveComponentIcon
  __impl.resolveEmptyGuideIcon = resolveEmptyGuideIcon
  __impl.load = load
  __impl.warmCurrentPortalPageCrud = warmCurrentPortalPageCrud
  __impl.selectNode = selectNode
  __impl.resolveSelectablePageId = resolveSelectablePageId
  __impl.ensurePageTitleComponents = ensurePageTitleComponents
  __impl.createQuickNode = createQuickNode
  __impl.toggleGroupExpanded = toggleGroupExpanded
  __impl.isGroupCollapsed = isGroupCollapsed
  __impl.openPageTypeSelector = openPageTypeSelector
  __impl.handlePageTypeSelection = handlePageTypeSelection
  __impl.selectIntroTemplate = selectIntroTemplate
  __impl.resolvePageTemplateIcon = resolvePageTemplateIcon
  __impl.createPageFromTemplate = createPageFromTemplate
  __impl.selectCreatedDesignerPage = selectCreatedDesignerPage

  return {
    __impl, mut, animatePageBlockSwap, appendPageBlock, appendPageBlockToContainer, appendPageBlockToGridCell, appendPageBlockToTab, applyDesignerResource,
    applyNestedPageBlockMove, applyPageBlockMove, applyPageBlockSwapPreview, applyPageTemplate, applyFreeLayoutPreset, applyCurrentFreeLayoutPreset, openLayoutPresetPicker, attachDefaultRuntimeObject, attachSingleFormAsset, bindSingleFormToCompatibleBlocks, bumpPortalCrudConfigAfterDesignerSave,
    cancelRenameApplication, clearPageAlignGuides, clearPageBlockSwapPreview, clonePageBlockTree, confirmNavigationAction, confirmRenameApplication, confirmSystemMenuMount, copySelectedBlockToOtherPage,
    createFormAssetForCurrentPage, createFormAssetForPageCrud, createFormAssetForSelectedBlock, createFormFieldVisibilitySettings, createLegacyBlock, createPageFromTemplate, createQuickNode, createStandaloneFormAsset,
    currentDesignerDirty, discardAndExitEditing, duplicateNavigationNode, duplicateNestedPageBlock, editSelectedBlockFormAsset, endComponentButtonMove, endNestedPageBlockMove, endNestedPageBlockResize,
    endPageBlockMove, endPageBlockResize, ensureFormDesignerObjectContext, ensurePageFlowContentCoords, ensurePageTitleComponents, enterPageDesign, enterWorkbenchDesign, exitToPageManagement,
    extractNestedPageBlockToCanvas, findFirstBlockWithFormAssetId, findPageBlockByFormAssetId, finishCatalogPointerDrag, handleApplicationObjectsChanged, handleCatalogPointerMove, handleComponentCatalogClick, handleComponentCatalogDragEnd,
    handleComponentCatalogDragStart, handleEmbeddedDesignerSaved, handleEmbeddedProcessDesignerSaved, handleExtensionsChanged, handleInlineTextUpdate, handleNavigationMoreSelect, handleNestedPageBlockMenuSelect, handleNestedPageBlockMoveStart,
    handleNestedPageBlockResizeStart, handleNestedPageBlockSelect, handlePageBlockDataSourceRequest, handlePageBlockMoreSelect, handlePageFlowBlankClick, handlePageFlowContainerClear, handlePageFlowContainerInsert, handlePageFlowDragOver,
    handlePageFlowDrop, handlePageFlowGridCellDrop, handlePageFlowTabDrop, handlePageTypeSelection, handleProcessPanelNavigate, handleResourceNodeAction, handleRuntimeBack, handleRuntimeHeaderMoreSelect,
    handleRuntimeTreeSelect, hydratePageCrudApiPlaceholders, insertComponent, isDraftPreviewMode, isGroupCollapsed, isNavigationGroupDescendant, isPageBlockDataSourceConfigured, isPageBlockRuntimeCrudLoading,
    isPageCatalogDrag, isSimpleBodyContainer, isValidPageBlockObjectRef, itemBorderColorMode, load, markBuilderClean, moveNavigationByOffset, movePageBlockIntoContainer,
    moveRootPageBlockToGridCell, moveRootPageBlockToTab, normalizeContainerDropTarget, normalizePageBlockForContainer, onComponentButtonMove, onNestedPageBlockMove, onPageBlockMove, onPageBlockResize,
    openBusinessObjectDesign, openCustomPageSelector, openDraftPreview, openEmbeddedObjectActions, openExcelPageImport, openFormAssetDesigner, openFormAssetDesignerForPage, openObjectDesigner,
    openObjectResourcePreview, openObjectSetup, openPageBlockConfiguration, openPagePropertiesPanel, openPageTypeSelector, openProcessDesigner, openRuntimeWorkbench, openSelectedBlockFormDesigner, openSystemMenuMountDialog, openWorkspace,
    pageUsesFreeLayoutCanvas, patchCurrentPageNode, patchNavigationNodeMount, persistApplicationDraft, prefetchRuntimeWorkspacePanels, preloadCurrentPageCrudRuntimeProps, preloadPageBlockCrudRuntimeProps, promoteCurrentPageToListShape,
    provisionPendingFormData, readRouteDesignTab, refreshWorkspaceMetadata, relocateNestedPageBlockToContainer, relocateNestedPageBlockToGridCell, relocateNestedPageBlockToTab, renderNavigationMenuIcon, requestExitEditing,
    resizeNestedPageBlock, resolveActiveDesignerPageId, resolveComponentIcon, resolveComponentPickerGroup, resolveEmptyGuideIcon, resolveEntryDesignTab, resolveFormAssetFields, resolveFormAssetIdForPage,
    resolveFormLayoutBlockType, resolveNavigationMoreOptions, resolveNextNavigationTitle, resolveNextPageBlockStackY, resolvePageBlockBackgroundColor, resolvePageBlockFields, resolvePageBlockMoreOptions, resolvePageBlockMoveIntoOptions,
    resolvePageBlockObjectRef, resolvePageBlockRuntimeCrudProps, resolvePageBlockShellStyle, resolvePageBlockSwapTarget, resolvePageContextFieldCatalog, resolvePageDesignTab, resolvePageFlowContainerTargetByPageBlock, resolvePageFlowContainerTargetByRect,
    resolvePageFlowContainerTargetFromElement, resolvePageFlowContainerTargetFromPoint, resolvePageFlowGridCellTarget, resolvePageFlowGridCellTargetByPageBlock, resolvePageFlowGridCellTargetByRect, resolvePageFlowGridCellTargetFromElement, resolvePageFlowGridCellTargetFromPoint, resolvePageFlowTabTarget,
    resolvePageFlowTabTargetFromPoint, resolvePagePreviewBlock, resolvePageShapeDesignContext, resolvePageShapeKey, resolvePageTemplateIcon, resolvePortalFormFields, resolveResourceNodeBuilderNode, resolveResourceNodeMenuOptions,
    resolveRuntimeBackLabel, resolveRuntimeBrandEyebrow, resolveRuntimeHeaderTitle, resolveRuntimeBrandStatus, resolveRuntimeObjectCacheKey, resolveRuntimeView, resolveSelectablePageId, resolveSystemPageIcon, resolveNavigationBadge, formatNavigationBadgeCount, refreshNavigationBadges, restoreCurrentObjectPageLayout,
    returnToEditorFromDraftPreview, returnToPageDesigner, saveActiveFormDesigner, saveCurrentDesignerSection, saveDraft, saveNavigationDraft, scheduleNavigationSave, selectCreatedDesignerPage,
    selectDesignerResource, selectFormAssetFromPicker, selectIntroTemplate, selectNode, selectPageBlock, selectPageManagementNode, setFormDataProvisionState, snapPageBlockPosition,
    startCatalogPointerDrag, startComponentButtonMove, startPageBlockMove, startPageBlockResize, startRenameApplication, supportsFormAsset, switchPageDesignTab, switchRuntimeView,
    syncActiveFormAssetForPage, syncActivePageShapeObject, syncCatalogDropActiveClass, toggleGroupExpanded, toggleNavigationVisible, unmountSystemMenu, updateActiveFormDesignerSchema, updateCurrentGridLayout,
    updatePageAlignGuides, updatePageBlockBackgroundColor, updatePageBlockSize, updatePageBlocks, updatePageCanvasPadding, updatePageCanvasSurface, resetPageCanvasSurface, updateResizeCollisionHighlights, updateSelectedBlockAppearance, updateSelectedBlockFormAsset,
    setFormAssetAsListCrudForm, isFormAssetBoundToListCrud, collectFormAssetUsages, formatFormAssetUsageSummary, focusFormAssetUsage, clearFormAssetListCrudBinding,
    updateSelectedPageBlockRuntimeObject, warmCurrentPortalPageCrud, route, router, message, tenantStore, defaultLogo, WORKBENCH_PAGE_ID, isNavigationVisible, DEFAULT_PAGE_PADDING, DEFAULT_PAGE_SURFACE, formComponentIconModules, formComponentIconCache,
    formComponentIconFileByBlockType, userStore, asyncPanelLoader, ApplicationObjectsPanel, ApplicationExtensionsPanel, ApplicationProcessPanel, ApplicationSettingsPanel, runtimeWorkspacePanelImporters,
    BusinessObjectDesignerPage, BusinessProcessPage, ForgeFormDesigner, ListPageGridDesigner, IconSelector, PageTypeSelector, PageDesignSettingsPanel, PageDesignPublishPanel,
    PageManagementSystemView, GridBlockRenderer, application, objects, builder, processSelectableObjects, loadError, loading,
    editCanvasBootstrapping, saving, previewTabLoading, restoringObjectPageLayout, editing, activePageDesignTab, runtimeViewMode, exitEditingVisible, selectedNodeId, enhanceContextPageId,
    newNodePopoverVisible, selectedPageTemplateKey, iconPickerVisible, iconPickerNodeId, navigationActionVisible, navigationActionMode, navigationActionNodeId, navigationActionForm,
    systemMenuMountVisible, systemMenuMountNodeId, systemMenuMountNode, componentPopoverVisible, componentKeyword, savedSignature, selectedPageBlockId, hoveredPageBlockId, nestHostPinnedId, draggingPageBlockId,
    dragPreview, pageAlignGuides, PAGE_ALIGN_SNAP_PX, configPanelVisible, inspectorTab, componentButtonPosition, componentButtonMoveCtx, catalogDragBlockType,
    suppressCatalogClick, activePageFlowTabTarget, activePageFlowGridTarget, activePageFlowContainerTarget, resizeCollisionBlockIds, formDesignerObjectContextByObjectId, formDesignerObjectContextLoadingIds, formDesignerMode,
    designerTransitionLoading, runtimeTreeFilter, runtimeTreeFilterByBlockId, runtimeTreeActiveKeyByBlockId, formDesignerFromPageManagement, activeFormAssetId, activePageShapeDesign, pageTypeSelectorVisible, layoutPresetPickerVisible,
    pageTypeSelectorParentId, pageTypeSelectorDefaultType, sidebarCollapsed, renamingApplication, renameApplicationValue, renameSaving, renameInputRef, collapsedGroupIds,
    copyBlockVisible, copyBlockId, copyBlockTargetPageId, blockBackgroundPickerVisible, backgroundPickerBlockId, formAssetSelectorOpen, formAssetSelectorKeyword, formDataProvisioningByAssetId,
    objectSetupVisible, selectedDesignerResourceKey, workspaceExtensions, workspaceEntries, portalCrudConfigRevision, portalCrudSeed, isDraftMode, activeFlowContext,
    embeddedDesignerRef, embeddedDesignerDirty, embeddedDesignerSaving, suppressHeavyEmbeddedSaveSideEffects, listDesignerSessionActive, formDesignerSessionActive, embeddedProcessDesignerVisible, embeddedProcessDesignerId, applicationRuntimeLoadCoordinator, componentPickerGroupOptions, runtimeHeaderMoreOptions,
    pageManagementSystemPages, systemPageIconMap, navigationBadges, currentSystemPage, workbenchPage, systemPageNavigationRoutes, designerResourceGroups, activeDesignerResource, designerSection,
    pageBuilderResourceActive, canEditApplication, usePortalDesignPreview, activeDesignerObject, activeResourceConfigKey, previewableResourceActive, legacyBlockTypeMap, pageBlockResizeAnchors,
    pageBlockRecommendedColors, groupOptions, navigationActionNode, iconPickerNode, navigationIconValue, navigationActionTitle, navigationActionHasChildren, navigationDeleteImpact,
    navigationDeleteTip, moveGroupOptions, navigationNodes, currentNode, currentPage, currentPageLayoutPolluted, currentPageManagementTitle, isRuntimeWorkbenchActive, pageDesignObject,
    currentGridLayout, pageBlocks, pageFlowStackMode, pageCanvasPadding, pageCanvasPaddingCss, pageCanvasSurface, pageCanvasSurfaceStyle, pageSurfaceHostStyle, designerGridLayout, appPageTargetOptions, pageFlowHeight, hasCustomComponentButtonPosition,
    componentButtonStyle, selectedPageBlock, formAssets, filteredFormAssets, pageFormAssetOptions, activeFormAsset, activeFormDesignerSchema, activeFormAssetBlock,
    activeFormDesignerObjectRef, activeFormDesignerContext, activeFormDesignerRelations, activeFormDesignerActions, activeFormFields, activeFormDataState, selectedPageBlockFormAssetId, selectedPageBlockBoundFormAssetId, selectedPageBlockFormAsset, selectedPageBlockBoundFormAsset,
    selectedPageBlockRuntimeObjectRef, selectedPageBlockIsCrud, selectedPageBlockSupportsDataSource, selectedPageBlockUsesObjectRuntime, selectedPageBlockRuntimeObjectId, selectedPageBlockObjectDesignerPanel, selectedPageBlockFormDataState, runtimeObjectFormOptions,
    pageTemplateOptions, freeLayoutPresetOptions, selectedPageBlockFields, selectedPageBlockFormDesignerSchema, applicationGridModelSchema, dragPreviewBlock, nestedMovingPageBlockId, copyBlockPageOptions, dirty,
    canSaveActiveFormDesigner, loadErrorTitle, filteredComponents, componentPickerGroups, recommendedComponents,
    runtimeCrudPropsByObjectId, runtimeCrudLoadingObjectIds, runtimeCrudUnavailableObjectIds, loadRuntimeCrudProps, resetRuntimeCrudConfig,
    historyReady, canUndo, canRedo, resetBuilderHistory, runWithoutHistory,
    undoBuilder, redoBuilder, handleBuilderShortcut,
  }
}
