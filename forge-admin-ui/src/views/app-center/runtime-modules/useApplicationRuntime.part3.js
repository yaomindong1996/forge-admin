/** application-runtime setup part 3. */
import { AddOutline, AppsOutline, ArrowBackOutline, ArrowDownOutline, ArrowRedoOutline, ArrowUndoOutline, ArrowUpOutline, BarChartOutline, CheckboxOutline, CheckmarkDoneOutline, ColorFillOutline, CopyOutline, CreateOutline, CubeOutline, DocumentTextOutline, DuplicateOutline, EllipsisHorizontalOutline, ExpandOutline, EyeOffOutline, EyeOutline, FolderOpenOutline, FunnelOutline, GitBranchOutline, GitNetworkOutline, GridOutline, InformationCircleOutline, ListOutline, MoveOutline, NotificationsOutline, PaperPlaneOutline, PeopleOutline, ReaderOutline, RemoveOutline, ResizeOutline, SaveOutline, SettingsOutline, SquareOutline, StatsChartOutline, SwapHorizontalOutline, TextOutline, TrashOutline } from '@vicons/ionicons5'
import { NIcon, useMessage } from 'naive-ui'
import { computed, defineAsyncComponent, h, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import draggable from 'vuedraggable'
import { businessObjectDesigner } from '@/api/business-app'
import { businessApplicationRuntimeByCode, businessApplicationWorkspaceByCode, designBusinessApplicationPage, initializeBusinessApplicationExcel, previewBusinessApplicationExcel, provisionBusinessApplicationFormData, updateBusinessApplication } from '@/api/business-application'
import defaultLogo from '@/assets/images/logo.png'
import AuthImage from '@/components/common/AuthImage.vue'
import IconRenderer from '@/components/IconRenderer.vue'
import { createDefaultBlockStyle, createGridBlock, DATA_FIELD_BLOCK_TYPES, isDataFieldBlockType, listPageBlockCatalog, resolveListPageBlockMeta } from '@/components/lowcode-builder/page/page-schema'
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
import { buildBusinessObjectDesignerPayloadFromFormAsset, normalizeObjectDesignerFieldCatalog, syncFormBoundFieldRefs } from '../in-app-builder/page-form-object-promotion'
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
import { findGridCellHostForChild, findPageBlockInTree, mapPageBlocksInTree, removePageBlockFromTree, visitPageBlocksInTree } from './page-block-tree'
import { resolvePageBlockShellStyle as computePageBlockShellStyle, readPageBlockLength, resolveDefaultPageBlockHeight, resolveDefaultPageBlockYFromItems, resolvePageBlockFlowGeometry, resolveRootPageBlockCollisions, shouldUsePageFlowStack, normalizePagePadding, resolvePagePaddingCss, migratePageFlowToContentCoords, canvasToContentFlowPoint, contentToCanvasFlowPoint, DEFAULT_PAGE_PADDING } from './page-flow-geometry'
import { flattenNodes, hasPermission, isNavigationVisible } from './runtime-navigation-utils'
import { useBuilderHistory } from './use-builder-history'
import { useRuntimeCrudConfig } from './use-runtime-crud-config'

export function applyApplicationRuntimePart3(deps = {}) {
  const {
    __impl, mut, animatePageBlockSwap, appendPageBlock, applyDesignerResource, applyPageBlockMove, applyPageBlockSwapPreview, applyPageTemplate,
    attachDefaultRuntimeObject, attachSingleFormAsset, bindSingleFormToCompatibleBlocks, bumpPortalCrudConfigAfterDesignerSave, cancelRenameApplication, clearPageAlignGuides, clearPageBlockSwapPreview, confirmNavigationAction,
    confirmRenameApplication, confirmSystemMenuMount, createFormAssetForCurrentPage, createFormAssetForPageCrud, createFormAssetForSelectedBlock, createFormFieldVisibilitySettings, createLegacyBlock, createPageFromTemplate,
    createQuickNode, createStandaloneFormAsset, currentDesignerDirty, discardAndExitEditing, duplicateNavigationNode, editSelectedBlockFormAsset, endComponentButtonMove, endPageBlockMove,
    endPageBlockResize, ensureFormDesignerObjectContext, ensurePageFlowContentCoords, ensurePageTitleComponents, enterPageDesign, enterWorkbenchDesign, exitToPageManagement, findFirstBlockWithFormAssetId,
    findPageBlockByFormAssetId, handleApplicationObjectsChanged, handleCatalogPointerMove, handleComponentCatalogDragStart, handleEmbeddedDesignerSaved, handleEmbeddedProcessDesignerSaved, handleExtensionsChanged, handleInlineTextUpdate,
    handleNavigationMoreSelect, handleNestedPageBlockSelect, handlePageBlockDataSourceRequest, handlePageFlowBlankClick, handlePageTypeSelection, handleProcessPanelNavigate, handleResourceNodeAction, handleRuntimeBack,
    handleRuntimeHeaderMoreSelect, handleRuntimeTreeSelect, hydratePageCrudApiPlaceholders, insertComponent, isDraftPreviewMode, isGroupCollapsed, isNavigationGroupDescendant, isPageBlockDataSourceConfigured,
    isPageBlockRuntimeCrudLoading, isSimpleBodyContainer, isValidPageBlockObjectRef, load, markBuilderClean, moveNavigationByOffset, moveRootPageBlockToGridCell, moveRootPageBlockToTab,
    normalizeContainerDropTarget, onComponentButtonMove, onPageBlockMove, onPageBlockResize, openBusinessObjectDesign, openCustomPageSelector, openDraftPreview, openEmbeddedObjectActions,
    openExcelPageImport, openFormAssetDesigner, openFormAssetDesignerForPage, openObjectDesigner, openObjectResourcePreview, openObjectSetup, openPageBlockConfiguration, openPageTypeSelector,
    openProcessDesigner, openSelectedBlockFormDesigner, openSystemMenuMountDialog, openWorkspace, pageUsesFreeLayoutCanvas, patchCurrentPageNode, patchNavigationNodeMount, persistApplicationDraft,
    prefetchRuntimeWorkspacePanels, preloadCurrentPageCrudRuntimeProps, preloadPageBlockCrudRuntimeProps, promoteCurrentPageToListShape, provisionPendingFormData, readRouteDesignTab, refreshWorkspaceMetadata, renderNavigationMenuIcon,
    requestExitEditing, resolveActiveDesignerPageId, resolveComponentIcon, resolveComponentPickerGroup, resolveEmptyGuideIcon, resolveEntryDesignTab, resolveFormAssetFields, resolveFormAssetIdForPage,
    resolveNavigationMoreOptions, resolveNextNavigationTitle, resolveNextPageBlockStackY, resolvePageBlockFields, resolvePageBlockObjectRef, resolvePageBlockRuntimeCrudProps, resolvePageBlockShellStyle, resolvePageBlockSwapTarget,
    resolvePageContextFieldCatalog, resolvePageDesignTab, resolvePagePreviewBlock, resolvePageShapeDesignContext, resolvePageShapeKey, resolvePageTemplateIcon, resolvePortalFormFields, resolveResourceNodeBuilderNode,
    resolveResourceNodeMenuOptions, resolveRuntimeBackLabel, resolveRuntimeBrandEyebrow, resolveRuntimeBrandStatus, resolveRuntimeObjectCacheKey, resolveRuntimeView, resolveSelectablePageId, resolveSystemPageIcon,
    restoreCurrentObjectPageLayout, returnToEditorFromDraftPreview, returnToPageDesigner, saveActiveFormDesigner, saveCurrentDesignerSection, saveDraft, saveNavigationDraft, scheduleNavigationSave,
    selectCreatedDesignerPage, selectDesignerResource, selectFormAssetFromPicker, selectIntroTemplate, selectNode, selectPageBlock, selectPageManagementNode, setFormDataProvisionState,
    snapPageBlockPosition, startCatalogPointerDrag, startComponentButtonMove, startPageBlockMove, startPageBlockResize, startRenameApplication, supportsFormAsset, switchPageDesignTab,
    switchRuntimeView, syncActiveFormAssetForPage, syncActivePageShapeObject, toggleGroupExpanded, toggleNavigationVisible, unmountSystemMenu, updateActiveFormDesignerSchema, updateCurrentGridLayout,
    updatePageAlignGuides, updatePageBlocks, updatePageCanvasPadding, updateResizeCollisionHighlights, updateSelectedBlockFormAsset, updateSelectedPageBlockRuntimeObject, warmCurrentPortalPageCrud, route,
    router, message, tenantStore, formComponentIconModules, formComponentIconCache, formComponentIconFileByBlockType, userStore, asyncPanelLoader,
    ApplicationObjectsPanel, ApplicationExtensionsPanel, ApplicationProcessPanel, ApplicationSettingsPanel, runtimeWorkspacePanelImporters, BusinessObjectDesignerPage, BusinessProcessPage, ForgeFormDesigner,
    ListPageGridDesigner, IconSelector, PageTypeSelector, PageDesignSettingsPanel, PageDesignPublishPanel, PageManagementSystemView, GridBlockRenderer, application,
    objects, builder, processSelectableObjects, loadError, loading, editCanvasBootstrapping, saving, restoringObjectPageLayout,
    editing, runtimeViewMode, exitEditingVisible, selectedNodeId, enhanceContextPageId, newNodePopoverVisible, selectedPageTemplateKey, iconPickerVisible,
    iconPickerNodeId, navigationActionVisible, navigationActionMode, navigationActionNodeId, navigationActionForm, systemMenuMountVisible, systemMenuMountNodeId, systemMenuMountNode,
    componentPopoverVisible, componentKeyword, savedSignature, selectedPageBlockId, draggingPageBlockId, dragPreview, pageAlignGuides, PAGE_ALIGN_SNAP_PX,
    configPanelVisible, inspectorTab, componentButtonPosition, componentButtonMoveCtx, catalogDragBlockType, suppressCatalogClick, activePageFlowTabTarget, activePageFlowGridTarget,
    activePageFlowContainerTarget, resizeCollisionBlockIds, formDesignerObjectContextByObjectId, formDesignerObjectContextLoadingIds, formDesignerMode, designerTransitionLoading, runtimeTreeFilter, runtimeTreeFilterByBlockId,
    runtimeTreeActiveKeyByBlockId, formDesignerFromPageManagement, activeFormAssetId, activePageShapeDesign, pageTypeSelectorVisible, pageTypeSelectorParentId, pageTypeSelectorDefaultType, sidebarCollapsed,
    renamingApplication, renameApplicationValue, renameSaving, renameInputRef, collapsedGroupIds, copyBlockVisible, copyBlockId, copyBlockTargetPageId,
    blockBackgroundPickerVisible, backgroundPickerBlockId, formAssetSelectorOpen, formAssetSelectorKeyword, formDataProvisioningByAssetId, objectSetupVisible, selectedDesignerResourceKey, workspaceExtensions,
    workspaceEntries, portalCrudConfigRevision, portalCrudSeed, isDraftMode, activeFlowContext, embeddedDesignerRef, embeddedDesignerDirty, embeddedDesignerSaving,
    embeddedProcessDesignerVisible, embeddedProcessDesignerId, applicationRuntimeLoadCoordinator, componentPickerGroupOptions, runtimeHeaderMoreOptions, pageManagementSystemPages, systemPageIconMap, currentSystemPage,
    workbenchPage, systemPageNavigationRoutes, designerResourceGroups, activeDesignerResource, designerSection, pageBuilderResourceActive, canEditApplication, usePortalDesignPreview,
    activeDesignerObject, activeResourceConfigKey, previewableResourceActive, legacyBlockTypeMap, pageBlockResizeAnchors, pageBlockRecommendedColors, groupOptions, navigationActionNode,
    iconPickerNode, navigationIconValue, navigationActionTitle, navigationActionHasChildren, navigationDeleteImpact, navigationDeleteTip, moveGroupOptions, navigationNodes,
    currentNode, currentPage, currentPageLayoutPolluted, currentPageManagementTitle, pageDesignObject, currentGridLayout, pageBlocks, pageFlowStackMode,
    pageCanvasPadding, pageCanvasPaddingCss, designerGridLayout, appPageTargetOptions, pageFlowHeight, hasCustomComponentButtonPosition, componentButtonStyle, selectedPageBlock, formAssets,
    filteredFormAssets, pageFormAssetOptions, activeFormAsset, activeFormDesignerSchema, activeFormAssetBlock, activeFormDesignerObjectRef, activeFormDesignerContext, activeFormDesignerRelations,
    activeFormDesignerActions, activeFormFields, activeFormDataState, selectedPageBlockFormAssetId, selectedPageBlockFormAsset, selectedPageBlockRuntimeObjectRef, selectedPageBlockIsCrud, selectedPageBlockSupportsDataSource,
    selectedPageBlockUsesObjectRuntime, selectedPageBlockRuntimeObjectId, selectedPageBlockObjectDesignerPanel, selectedPageBlockFormDataState, runtimeObjectFormOptions, pageTemplateOptions, selectedPageBlockFields, selectedPageBlockFormDesignerSchema,
    applicationGridModelSchema, dragPreviewBlock, nestedMovingPageBlockId, copyBlockPageOptions, dirty, canSaveActiveFormDesigner, loadErrorTitle, filteredComponents,
    componentPickerGroups, recommendedComponents,
    runtimeCrudPropsByObjectId, runtimeCrudLoadingObjectIds, runtimeCrudUnavailableObjectIds, loadRuntimeCrudProps, resetRuntimeCrudConfig, historyReady, canUndo, canRedo, resetBuilderHistory,
  
  } = deps
  function syncCatalogDropActiveClass(containerId = '') {
    document.querySelectorAll('.application-page-block.is-catalog-drop-active').forEach((el) => {
      el.classList.remove('is-catalog-drop-active')
    })
    if (!containerId)
      return
    document.querySelector(`[data-page-block-id="${containerId}"]`)?.classList.add('is-catalog-drop-active')
  }
  __impl.syncCatalogDropActiveClass = syncCatalogDropActiveClass

  function finishCatalogPointerDrag(event) {
    const ctx = mut.catalogPointerDragCtx
    mut.catalogPointerDragCtx = null
    window.removeEventListener('pointermove', handleCatalogPointerMove)
    window.removeEventListener('pointercancel', finishCatalogPointerDrag)
    // 先在关闭浮层/去掉拖拽 class 之前锁定投放目标，避免布局抖动导致松手重新命中失败
    const blockType = ctx?.blockType || ''
    const tabTarget = ctx?.moved
      ? (activePageFlowTabTarget.value || resolvePageFlowTabTargetFromPoint(event))
      : null
    const gridTarget = ctx?.moved
      ? (activePageFlowGridTarget.value
        ? {
            blockId: activePageFlowGridTarget.value.containerId,
            cellKey: activePageFlowGridTarget.value.cellKey,
            cellIndex: activePageFlowGridTarget.value.cellIndex,
          }
        : resolvePageFlowGridCellTargetFromPoint(event))
      : null
    const containerTarget = ctx?.moved
      ? normalizeContainerDropTarget(
        activePageFlowContainerTarget.value || resolvePageFlowContainerTargetFromPoint(event),
      )
      : null
    document.body.classList.remove('is-catalog-pointer-dragging')
    syncCatalogDropActiveClass('')
    clearPageAlignGuides()
    activePageFlowTabTarget.value = null
    activePageFlowGridTarget.value = null
    activePageFlowContainerTarget.value = null
    // 未拖动：交给 click → insertComponent；若此处关浮层会先卸掉按钮，click 丢失
    if (!ctx?.moved) {
      catalogDragBlockType.value = ''
      return
    }
    componentPopoverVisible.value = false
    if (tabTarget)
      appendPageBlockToTab(blockType, tabTarget.blockId, tabTarget.tabKey)
    else if (gridTarget) {
      const host = findPageBlockInTree(pageBlocks.value, gridTarget.blockId)
      const beforeCount = JSON.stringify(pageBlocks.value)
      appendPageBlockToGridCell(blockType, gridTarget.blockId, gridTarget.cellKey, gridTarget.cellIndex)
      const inserted = beforeCount !== JSON.stringify(pageBlocks.value)
      if (inserted) {
        if (host?.blockType === 'card')
          message.success('组件已放入卡片')
        else if (host?.blockType === 'box-layout')
          message.success('组件已放入盒子')
        else
          message.success('组件已放入栅格')
      }
    }
    else if (containerTarget) {
      if (appendPageBlockToContainer(blockType, containerTarget.containerId))
        message.success(containerTarget.containerType === 'card' ? '组件已放入卡片' : '组件已放入容器')
    }
    else
      appendPageBlock(blockType)
    catalogDragBlockType.value = ''
  }
  __impl.finishCatalogPointerDrag = finishCatalogPointerDrag

  function handleComponentCatalogDragEnd() {
    // drop 事件在部分浏览器中晚于 dragend 到达，延迟清理拖拽类型。
    window.setTimeout(() => {
      if (mut.catalogPointerDragCtx)
        return
      catalogDragBlockType.value = ''
      suppressCatalogClick.value = false
    }, 250)
  }
  __impl.handleComponentCatalogDragEnd = handleComponentCatalogDragEnd

  function handleComponentCatalogClick(component, event) {
    if (suppressCatalogClick.value) {
      event.preventDefault()
      event.stopPropagation()
      suppressCatalogClick.value = false
      return
    }
    insertComponent(component)
  }
  __impl.handleComponentCatalogClick = handleComponentCatalogClick

  function isPageCatalogDrag(event) {
    return Boolean(catalogDragBlockType.value)
      || Array.from(event.dataTransfer?.types || []).some(type => [
        'application/x-forge-app-page-block',
        'application/x-list-block',
        'application/x-forge-form-layout',
      ].includes(type))
  }
  __impl.isPageCatalogDrag = isPageCatalogDrag

  function resolvePageFlowTabTarget(event) {
    const pointTarget = Number.isFinite(event.clientX) && Number.isFinite(event.clientY)
      ? document.elementFromPoint(event.clientX, event.clientY)
      : null
    const target = pointTarget?.closest?.('[data-grid-container-id][data-grid-tab-key]')
      || event.target?.closest?.('[data-grid-container-id][data-grid-tab-key]')
    if (!target || !event.currentTarget?.contains?.(target))
      return null
    const rect = target.getBoundingClientRect?.()
    const style = window.getComputedStyle?.(target)
    if (!rect || rect.width <= 0 || rect.height <= 0 || style?.display === 'none' || style?.visibility === 'hidden')
      return null
    const blockId = String(target.dataset.gridContainerId || '')
    const tabKey = String(target.dataset.gridTabKey || '')
    return blockId && tabKey ? { blockId, tabKey } : null
  }
  __impl.resolvePageFlowTabTarget = resolvePageFlowTabTarget

  function resolvePageFlowTabTargetFromPoint(event) {
    if (!Number.isFinite(event.clientX) || !Number.isFinite(event.clientY))
      return null
    const pointTarget = document.elementFromPoint(event.clientX, event.clientY)
    const target = pointTarget?.closest?.('[data-grid-container-id][data-grid-tab-key]')
    if (!target)
      return null
    const rect = target.getBoundingClientRect?.()
    if (!rect || rect.width <= 0 || rect.height <= 0)
      return null
    const blockId = String(target.dataset.gridContainerId || '')
    const tabKey = String(target.dataset.gridTabKey || '')
    return blockId && tabKey ? { blockId, tabKey } : null
  }
  __impl.resolvePageFlowTabTargetFromPoint = resolvePageFlowTabTargetFromPoint

  function resolvePageFlowGridCellTarget(event) {
    return resolvePageFlowGridCellTargetFromPoint(event)
      || resolvePageFlowGridCellTargetFromElement(event.target)
  }
  __impl.resolvePageFlowGridCellTarget = resolvePageFlowGridCellTarget

  function resolvePageFlowGridCellTargetFromPoint(event) {
    if (!Number.isFinite(event.clientX) || !Number.isFinite(event.clientY))
      return null
    const x = event.clientX
    const y = event.clientY
    // 卡片/盒子优先按整块几何命中，避免中间区域被子节点/遮罩挡住时落到画布根级
    const hostHit = resolvePageFlowGridCellTargetByPageBlock(x, y)
    if (hostHit && (hostHit.cellKey === '__body__' || findPageBlockInTree(pageBlocks.value, hostHit.blockId)?.blockType === 'grid-layout'))
      return hostHit
    const stack = typeof document.elementsFromPoint === 'function'
      ? document.elementsFromPoint(x, y)
      : [document.elementFromPoint(x, y)].filter(Boolean)
    for (const el of stack) {
      const hit = resolvePageFlowGridCellTargetFromElement(el)
      if (hit)
        return hit
    }
    return resolvePageFlowGridCellTargetByRect(x, y) || hostHit
  }
  __impl.resolvePageFlowGridCellTargetFromPoint = resolvePageFlowGridCellTargetFromPoint

  function resolvePageFlowGridCellTargetByRect(x, y) {
    const root = document.querySelector('.application-page-flow')
    if (!root)
      return null
    const cells = root.querySelectorAll('[data-grid-container-id][data-grid-cell-key]')
    let best = null
    let bestArea = Number.POSITIVE_INFINITY
    cells.forEach((el) => {
      const rect = el.getBoundingClientRect?.()
      if (!rect || rect.width <= 0 || rect.height <= 0)
        return
      if (x < rect.left || x > rect.right || y < rect.top || y > rect.bottom)
        return
      const area = rect.width * rect.height
      if (area >= bestArea)
        return
      bestArea = area
      best = el
    })
    return best ? resolvePageFlowGridCellTargetFromElement(best) : null
  }
  __impl.resolvePageFlowGridCellTargetByRect = resolvePageFlowGridCellTargetByRect

  /** 指针落在栅格区块任意位置时，按最近格子命中（对齐表单设计器「拖进容器」手感） */
  function resolvePageFlowGridCellTargetByPageBlock(x, y) {
    const root = document.querySelector('.application-page-flow')
    if (!root)
      return null
    const candidates = []
    root.querySelectorAll(
      '[data-page-block-id][data-page-block-type="grid-layout"], [data-page-block-id][data-page-block-type="card"], [data-page-block-id][data-page-block-type="box-layout"]',
    ).forEach((el) => {
      const rect = el.getBoundingClientRect?.()
      if (!rect || rect.width <= 0 || rect.height <= 0)
        return
      if (x < rect.left || x > rect.right || y < rect.top || y > rect.bottom)
        return
      candidates.push({ el, area: rect.width * rect.height })
    })
    candidates.sort((a, b) => a.area - b.area)
    for (const { el } of candidates) {
      const blockId = String(el.dataset.pageBlockId || '')
      const blockType = String(el.dataset.pageBlockType || '')
      if (!blockId)
        continue
      if (blockType === 'card' || blockType === 'box-layout')
        return { blockId, cellKey: '__body__', cellIndex: 0 }
      const cellNodes = [...el.querySelectorAll('[data-grid-cell-key]')]
      if (!cellNodes.length) {
        const block = findPageBlockInTree(pageBlocks.value, blockId)
        const first = Array.isArray(block?.props?.cells) ? block.props.cells[0] : null
        if (first?.key)
          return { blockId, cellKey: String(first.key), cellIndex: 0 }
        continue
      }
      let best = null
      let bestDist = Number.POSITIVE_INFINITY
      cellNodes.forEach((cellEl, index) => {
        const rect = cellEl.getBoundingClientRect?.()
        if (!rect || rect.width <= 0 || rect.height <= 0)
          return
        const cx = rect.left + rect.width / 2
        const cy = rect.top + rect.height / 2
        const dist = Math.hypot(x - cx, y - cy)
        const inside = x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom
        const score = inside ? dist / 4 : dist
        if (score >= bestDist)
          return
        bestDist = score
        best = { el: cellEl, index }
      })
      if (best) {
        const hit = resolvePageFlowGridCellTargetFromElement(best.el)
        if (hit)
          return hit
        return {
          blockId,
          cellKey: String(best.el.dataset.gridCellKey || ''),
          cellIndex: best.index,
        }
      }
    }
    return null
  }
  __impl.resolvePageFlowGridCellTargetByPageBlock = resolvePageFlowGridCellTargetByPageBlock

  function resolvePageFlowGridCellTargetFromElement(element) {
    const simpleHost = element?.closest?.('[data-page-block-type="card"][data-page-block-id], [data-page-block-type="box-layout"][data-page-block-id]')
    if (simpleHost?.dataset?.pageBlockId) {
      return {
        blockId: String(simpleHost.dataset.pageBlockId),
        cellKey: '__body__',
        cellIndex: 0,
      }
    }
    const target = element?.closest?.('[data-grid-container-id][data-grid-cell-key]')
      || element?.closest?.('[data-grid-cell-key]')
    if (!target)
      return null
    const rect = target.getBoundingClientRect?.()
    if (!rect || rect.width <= 0 || rect.height <= 0)
      return null
    const style = window.getComputedStyle?.(target)
    if (style?.display === 'none' || style?.visibility === 'hidden')
      return null
    const blockId = String(target.dataset.gridContainerId || target.closest?.('[data-page-block-id]')?.dataset?.pageBlockId || '')
    const cellKey = String(target.dataset.gridCellKey || target.dataset.cellKey || '')
    const cellIndex = Number(target.dataset.cellIndex)
    return blockId && cellKey
      ? { blockId, cellKey, cellIndex: Number.isFinite(cellIndex) ? cellIndex : -1 }
      : null
  }
  __impl.resolvePageFlowGridCellTargetFromElement = resolvePageFlowGridCellTargetFromElement

  /** 卡片 / 盒子：命中容器中部即可放入（对齐栅格拖入） */
  function resolvePageFlowContainerTargetFromPoint(event) {
    if (!Number.isFinite(event.clientX) || !Number.isFinite(event.clientY))
      return null
    const x = event.clientX
    const y = event.clientY
    const stack = typeof document.elementsFromPoint === 'function'
      ? document.elementsFromPoint(x, y)
      : [document.elementFromPoint(x, y)].filter(Boolean)
    for (const el of stack) {
      const hit = resolvePageFlowContainerTargetFromElement(el)
      if (hit)
        return hit
    }
    return resolvePageFlowContainerTargetByRect(x, y)
      || resolvePageFlowContainerTargetByPageBlock(x, y)
  }
  __impl.resolvePageFlowContainerTargetFromPoint = resolvePageFlowContainerTargetFromPoint

  function resolvePageFlowContainerTargetFromElement(element) {
    const target = element?.closest?.('[data-page-container-id][data-page-container-type]')
      || element?.closest?.('[data-page-block-type="card"], [data-page-block-type="box-layout"]')
    if (!target)
      return null
    const containerType = String(target.dataset.pageContainerType || target.dataset.pageBlockType || '')
    if (!['card', 'box-layout'].includes(containerType))
      return null
    const rect = target.getBoundingClientRect?.()
    if (!rect || rect.width <= 0 || rect.height <= 0)
      return null
    const blockId = String(
      target.dataset.pageContainerId
      || target.dataset.pageBlockId
      || target.closest?.('[data-page-block-id]')?.dataset?.pageBlockId
      || '',
    )
    return blockId ? { blockId, containerType } : null
  }
  __impl.resolvePageFlowContainerTargetFromElement = resolvePageFlowContainerTargetFromElement

  function resolvePageFlowContainerTargetByRect(x, y) {
    const root = document.querySelector('.application-page-flow')
    if (!root)
      return null
    const nodes = root.querySelectorAll(
      '[data-page-container-id][data-page-container-type], [data-page-block-type="card"], [data-page-block-type="box-layout"]',
    )
    let best = null
    let bestArea = Number.POSITIVE_INFINITY
    nodes.forEach((el) => {
      const containerType = String(el.dataset.pageContainerType || el.dataset.pageBlockType || '')
      if (!['card', 'box-layout'].includes(containerType))
        return
      const rect = el.getBoundingClientRect?.()
      if (!rect || rect.width <= 0 || rect.height <= 0)
        return
      if (x < rect.left || x > rect.right || y < rect.top || y > rect.bottom)
        return
      const area = rect.width * rect.height
      if (area >= bestArea)
        return
      bestArea = area
      best = el
    })
    return best ? resolvePageFlowContainerTargetFromElement(best) : null
  }
  __impl.resolvePageFlowContainerTargetByRect = resolvePageFlowContainerTargetByRect

  /** 指针落在卡片/盒子整块上时也命中（对齐栅格 byPageBlock） */
  function resolvePageFlowContainerTargetByPageBlock(x, y) {
    const root = document.querySelector('.application-page-flow')
    if (!root)
      return null
    const candidates = []
    root.querySelectorAll('[data-page-block-id][data-page-block-type="card"], [data-page-block-id][data-page-block-type="box-layout"]').forEach((el) => {
      const rect = el.getBoundingClientRect?.()
      if (!rect || rect.width <= 0 || rect.height <= 0)
        return
      if (x < rect.left || x > rect.right || y < rect.top || y > rect.bottom)
        return
      candidates.push({ el, area: rect.width * rect.height })
    })
    candidates.sort((a, b) => a.area - b.area)
    for (const { el } of candidates) {
      const hit = resolvePageFlowContainerTargetFromElement(el)
      if (hit)
        return hit
    }
    return null
  }
  __impl.resolvePageFlowContainerTargetByPageBlock = resolvePageFlowContainerTargetByPageBlock

  function handlePageFlowDragOver(event) {
    if (!isPageCatalogDrag(event))
      return
    event.preventDefault()
    activePageFlowTabTarget.value = resolvePageFlowTabTargetFromPoint(event) || resolvePageFlowTabTarget(event)
    const gridTarget = resolvePageFlowGridCellTargetFromPoint(event) || resolvePageFlowGridCellTarget(event)
    activePageFlowGridTarget.value = gridTarget
      ? { containerId: gridTarget.blockId, cellKey: gridTarget.cellKey, cellIndex: gridTarget.cellIndex }
      : null
    activePageFlowContainerTarget.value = gridTarget
      ? null
      : normalizeContainerDropTarget(resolvePageFlowContainerTargetFromPoint(event))
    event.dataTransfer.dropEffect = 'copy'
  }
  __impl.handlePageFlowDragOver = handlePageFlowDragOver

  function handlePageFlowDrop(event) {
    const blockType = event.dataTransfer?.getData('application/x-forge-app-page-block')
      || event.dataTransfer?.getData('application/x-list-block')
      || resolveFormLayoutBlockType(event)
      || catalogDragBlockType.value
    if (!blockType)
      return
    const tabTarget = activePageFlowTabTarget.value || resolvePageFlowTabTargetFromPoint(event) || resolvePageFlowTabTarget(event)
    if (tabTarget) {
      event.preventDefault()
      event.stopPropagation()
      appendPageBlockToTab(blockType, tabTarget.blockId, tabTarget.tabKey)
      catalogDragBlockType.value = ''
      activePageFlowTabTarget.value = null
      activePageFlowGridTarget.value = null
      activePageFlowContainerTarget.value = null
      return
    }
    const gridTarget = activePageFlowGridTarget.value
      ? {
          blockId: activePageFlowGridTarget.value.containerId,
          cellKey: activePageFlowGridTarget.value.cellKey,
          cellIndex: activePageFlowGridTarget.value.cellIndex,
        }
      : (resolvePageFlowGridCellTargetFromPoint(event) || resolvePageFlowGridCellTarget(event))
    if (gridTarget) {
      event.preventDefault()
      event.stopPropagation()
      appendPageBlockToGridCell(blockType, gridTarget.blockId, gridTarget.cellKey, gridTarget.cellIndex)
      catalogDragBlockType.value = ''
      activePageFlowTabTarget.value = null
      activePageFlowGridTarget.value = null
      activePageFlowContainerTarget.value = null
      return
    }
    const containerTarget = normalizeContainerDropTarget(
      activePageFlowContainerTarget.value || resolvePageFlowContainerTargetFromPoint(event),
    )
    if (containerTarget) {
      event.preventDefault()
      event.stopPropagation()
      appendPageBlockToContainer(blockType, containerTarget.containerId)
      catalogDragBlockType.value = ''
      activePageFlowTabTarget.value = null
      activePageFlowGridTarget.value = null
      activePageFlowContainerTarget.value = null
      message.success(containerTarget.containerType === 'card' ? '组件已放入卡片' : '组件已放入容器')
      return
    }
    event.preventDefault()
    event.stopPropagation()
    appendPageBlock(blockType)
    catalogDragBlockType.value = ''
    activePageFlowTabTarget.value = null
    activePageFlowGridTarget.value = null
    activePageFlowContainerTarget.value = null
  }
  __impl.handlePageFlowDrop = handlePageFlowDrop

  function resolveFormLayoutBlockType(event) {
    const raw = event.dataTransfer?.getData('application/x-forge-form-layout')
    if (!raw)
      return ''
    try {
      return String(JSON.parse(raw)?.componentKey || '').trim()
    }
    catch {
      return ''
    }
  }
  __impl.resolveFormLayoutBlockType = resolveFormLayoutBlockType

  function handlePageFlowTabDrop(payload = {}) {
    if (!editing.value)
      return
    const blockType = String(payload.blockType || '').trim()
    const blockId = String(payload.blockId || '').trim()
    const tabKey = String(payload.tabKey || '').trim()
    if (!blockType || !blockId || !tabKey)
      return
    appendPageBlockToTab(blockType, blockId, tabKey)
    catalogDragBlockType.value = ''
  }
  __impl.handlePageFlowTabDrop = handlePageFlowTabDrop

  function handlePageFlowGridCellDrop(payload = {}) {
    if (!editing.value)
      return
    const blockType = String(payload.blockType || '').trim()
    const blockId = String(payload.blockId || '').trim()
    const cellKey = String(payload.cellKey || '').trim()
    if (!blockType || !blockId)
      return
    appendPageBlockToGridCell(blockType, blockId, cellKey, payload.cellIndex)
    catalogDragBlockType.value = ''
  }
  __impl.handlePageFlowGridCellDrop = handlePageFlowGridCellDrop

  function handlePageFlowContainerInsert(payload = {}) {
    if (!editing.value)
      return
    const blockType = String(payload.blockType || '').trim()
    const blockId = String(payload.blockId || '').trim()
    const containerType = String(payload.containerType || '').trim()
    if (!blockType || !blockId)
      return
    if (containerType === 'grid-layout') {
      appendPageBlockToGridCell(blockType, blockId, payload.cellKey, payload.cellIndex)
      return
    }
    if (containerType === 'tabs') {
      appendPageBlockToTab(blockType, blockId, payload.tabKey)
      return
    }
    appendPageBlockToContainer(blockType, blockId)
  }
  __impl.handlePageFlowContainerInsert = handlePageFlowContainerInsert

  function handlePageFlowContainerClear(payload = {}) {
    if (!editing.value)
      return
    const blockId = String(payload.blockId || '').trim()
    const container = findPageBlockInTree(pageBlocks.value, blockId)
    if (!container)
      return
    if (container.blockType === 'grid-layout') {
      const cellKey = String(payload.cellKey || '')
      const cells = (Array.isArray(container.props?.cells) ? container.props.cells : []).map(cell => (
        !cellKey || cell.key === cellKey
          ? { ...cell, children: [] }
          : cell
      ))
      updatePageBlocks(mapPageBlocksInTree(pageBlocks.value, item => item.id === blockId
        ? { ...item, props: { ...(item.props || {}), cells } }
        : item))
      selectedPageBlockId.value = blockId
      return
    }
    if (container.blockType === 'tabs') {
      const tabKey = String(payload.tabKey || '')
      const tabs = (Array.isArray(container.props?.tabs) ? container.props.tabs : []).map(tab => (
        !tabKey || tab.key === tabKey
          ? { ...tab, children: [] }
          : tab
      ))
      updatePageBlocks(mapPageBlocksInTree(pageBlocks.value, item => item.id === blockId
        ? { ...item, props: { ...(item.props || {}), tabs } }
        : item))
      selectedPageBlockId.value = blockId
      return
    }
    updatePageBlocks(mapPageBlocksInTree(pageBlocks.value, item => item.id === blockId
      ? { ...item, children: [] }
      : item))
    selectedPageBlockId.value = blockId
  }
  __impl.handlePageFlowContainerClear = handlePageFlowContainerClear

  function appendPageBlockToContainer(blockType, containerId) {
    const meta = resolveListPageBlockMeta(blockType)
    const container = findPageBlockInTree(pageBlocks.value, containerId)
    if (!meta || !container || !['card', 'box-layout'].includes(container.blockType))
      return false
    // 容器内禁止再嵌套同级大容器，避免拖进后看不见
    if (['grid-layout', 'tabs', 'box-layout', 'card'].includes(blockType)) {
      message.warning('卡片/盒子内请放入普通组件，布局容器请放在画布根级')
      return false
    }
    let block = createGridBlock(blockType, applicationGridModelSchema.value, {
      gridX: 0,
      gridY: (container.children || []).length * 2,
    })
    if (!block)
      return false
    block = attachDefaultRuntimeObject(block)
    block = attachSingleFormAsset(block)
    block = normalizePageBlockForContainer(block)
    updatePageBlocks(mapPageBlocksInTree(pageBlocks.value, item => item.id === containerId
      ? { ...item, children: [...(item.children || []), block] }
      : item))
    selectedPageBlockId.value = block.id
    preloadPageBlockCrudRuntimeProps(block)
    __impl.scrollPageBlockIntoView?.(block.id)
    return true
  }
  __impl.appendPageBlockToContainer = appendPageBlockToContainer

  function handleNestedPageBlockMenuSelect(payload = {}) {
    const blockId = String(payload.block?.id || '').trim()
    if (!blockId)
      return
    if (payload.key === 'delete') {
      updatePageBlocks(removePageBlockFromTree(pageBlocks.value, blockId))
      if (selectedPageBlockId.value === blockId)
        selectedPageBlockId.value = ''
      return
    }
    if (payload.key === 'duplicate')
      duplicateNestedPageBlock(blockId)
  }
  __impl.handleNestedPageBlockMenuSelect = handleNestedPageBlockMenuSelect

  function duplicateNestedPageBlock(blockId) {
    const source = findPageBlockInTree(pageBlocks.value, blockId)
    if (!source)
      return
    const copy = clonePageBlockTree(source)
    let inserted = false
    updatePageBlocks(mapPageBlocksInTree(pageBlocks.value, (block) => {
      if (inserted)
        return block
      if (Array.isArray(block.children)) {
        const index = block.children.findIndex(child => child.id === blockId)
        if (index >= 0) {
          inserted = true
          const children = [...block.children]
          children.splice(index + 1, 0, copy)
          return { ...block, children }
        }
      }
      if (Array.isArray(block.props?.tabs)) {
        let changed = false
        const tabs = block.props.tabs.map((tab) => {
          const index = (tab.children || []).findIndex(child => child.id === blockId)
          if (index < 0)
            return tab
          changed = true
          inserted = true
          const children = [...tab.children]
          children.splice(index + 1, 0, copy)
          return { ...tab, children }
        })
        if (changed)
          return { ...block, props: { ...(block.props || {}), tabs } }
      }
      if (Array.isArray(block.props?.cells)) {
        let changed = false
        const cells = block.props.cells.map((cell) => {
          const index = (cell.children || []).findIndex(child => child.id === blockId)
          if (index < 0)
            return cell
          changed = true
          inserted = true
          const children = [...cell.children]
          children.splice(index + 1, 0, copy)
          return { ...cell, children }
        })
        if (changed)
          return { ...block, props: { ...(block.props || {}), cells } }
      }
      return block
    }))
    if (inserted)
      selectedPageBlockId.value = copy.id
  }
  __impl.duplicateNestedPageBlock = duplicateNestedPageBlock

  function clonePageBlockTree(source = {}) {
    const stamp = Date.now()
    let seq = 0
    const walk = (node) => {
      if (!node || typeof node !== 'object')
        return node
      const next = {
        ...node,
        id: `${node.blockType || 'block'}_${stamp}_${seq++}`,
      }
      if (Array.isArray(node.children))
        next.children = node.children.map(walk)
      if (Array.isArray(node.props?.tabs) || Array.isArray(node.props?.cells)) {
        next.props = { ...(node.props || {}) }
        if (Array.isArray(node.props?.tabs)) {
          next.props.tabs = node.props.tabs.map(tab => ({
            ...tab,
            children: (tab.children || []).map(walk),
          }))
        }
        if (Array.isArray(node.props?.cells)) {
          next.props.cells = node.props.cells.map(cell => ({
            ...cell,
            children: (cell.children || []).map(walk),
          }))
        }
      }
      return next
    }
    return walk(JSON.parse(JSON.stringify(source)))
  }
  __impl.clonePageBlockTree = clonePageBlockTree

  function appendPageBlockToGridCell(blockType, containerId, cellKey, cellIndex = -1) {
    const meta = resolveListPageBlockMeta(blockType)
    const container = findPageBlockInTree(pageBlocks.value, containerId)
    if (!meta || !container)
      return
    // 卡片/盒子复用栅格命中协议（data-grid-cell-key=__body__），落到 children
    if (['card', 'box-layout'].includes(container.blockType)) {
      appendPageBlockToContainer(blockType, containerId)
      return
    }
    if (container.blockType !== 'grid-layout')
      return
    const rawCells = Array.isArray(container.props?.cells) && container.props.cells.length
      ? container.props.cells
      : [{ key: 'cell_1', title: '栅格 1', span: 24, children: [] }]
    const cells = rawCells.map((cell, index) => ({
      ...cell,
      key: String(cell.key || `cell_${index + 1}`),
    }))
    let targetKey = String(cellKey || '')
    if (!cells.some(cell => cell.key === targetKey)) {
      const idx = Number(cellIndex)
      if (Number.isFinite(idx) && idx >= 0 && idx < cells.length)
        targetKey = cells[idx].key
    }
    if (!cells.some(cell => cell.key === targetKey))
      targetKey = cells[0]?.key || ''
    if (!targetKey)
      return
    let block = createGridBlock(blockType, applicationGridModelSchema.value, {
      gridX: 0,
      gridY: (cells.find(cell => cell.key === targetKey)?.children || []).length * 2,
    })
    if (!block)
      return
    block = attachDefaultRuntimeObject(block)
    block = attachSingleFormAsset(block)
    block = normalizePageBlockForContainer(block)
    const nextCells = cells.map(cell => cell.key === targetKey
      ? { ...cell, children: [...(cell.children || []), block] }
      : cell)
    updatePageBlocks(mapPageBlocksInTree(pageBlocks.value, item => item.id === containerId
      ? { ...item, props: { ...(item.props || {}), cells: nextCells } }
      : item))
    selectedPageBlockId.value = block.id
    preloadPageBlockCrudRuntimeProps(block)
    __impl.scrollPageBlockIntoView?.(block.id)
  }
  __impl.appendPageBlockToGridCell = appendPageBlockToGridCell

  function appendPageBlockToTab(blockType, containerId, tabKey) {
    const meta = resolveListPageBlockMeta(blockType)
    const container = findPageBlockInTree(pageBlocks.value, containerId)
    if (!meta || container?.blockType !== 'tabs')
      return
    const tabs = Array.isArray(container.props?.tabs) && container.props.tabs.length
      ? container.props.tabs
      : [{ key: 'tab1', title: '标签一', children: [] }]
    if (!tabs.some(tab => tab.key === tabKey))
      return
    let block = createGridBlock(blockType, applicationGridModelSchema.value, {
      gridX: 0,
      gridY: (tabs.find(tab => tab.key === tabKey)?.children || []).length * 2,
    })
    if (!block)
      return
    block = attachDefaultRuntimeObject(block)
    block = attachSingleFormAsset(block)
    block = normalizePageBlockForContainer(block)
    const nextTabs = tabs.map(tab => tab.key === tabKey
      ? { ...tab, children: [...(tab.children || []), block] }
      : tab)
    updatePageBlocks(mapPageBlocksInTree(pageBlocks.value, item => item.id === containerId
      ? { ...item, props: { ...(item.props || {}), tabs: nextTabs } }
      : item))
    selectedPageBlockId.value = block.id
    preloadPageBlockCrudRuntimeProps(block)
    __impl.scrollPageBlockIntoView?.(block.id)
  }
  __impl.appendPageBlockToTab = appendPageBlockToTab

  function resolvePageBlockMoreOptions(block = {}) {
    const borderWidth = Number(block.props?.style?.borderWidth || 0)
    const borderMode = itemBorderColorMode(block)
    const isThemeBorder = borderMode === 'theme' && borderWidth > 0
    const menuLabel = (label, active) => active ? `✓  ${label}` : label
    const moveIntoOptions = resolvePageBlockMoveIntoOptions(block)
    return [
      { label: '配置', key: 'configure', icon: () => renderNavigationMenuIcon(SettingsOutline) },
      { type: 'divider', key: 'configureDivider' },
      { label: '复制到当前页面', key: 'duplicate', icon: () => renderNavigationMenuIcon(DuplicateOutline) },
      { label: '复制到其他页面', key: 'copyToPage', disabled: !copyBlockPageOptions.value.length, icon: () => renderNavigationMenuIcon(CopyOutline) },
      {
        label: '与上一组件并排',
        key: 'arrangeBesidePrevious',
        disabled: pageBlocks.value.findIndex(item => item.id === block.id) <= 0,
        icon: () => renderNavigationMenuIcon(SwapHorizontalOutline),
      },
      {
        label: '移入布局',
        key: 'moveIntoLayout',
        icon: () => renderNavigationMenuIcon(GitBranchOutline),
        children: moveIntoOptions.length
          ? moveIntoOptions
          : [{ label: '暂无布局组合和标签页', key: 'moveInto:empty', disabled: true }],
      },
      {
        label: '尺寸',
        key: 'size',
        icon: () => renderNavigationMenuIcon(SettingsOutline),
        children: [
          { type: 'group', label: '宽度', key: 'widthGroup', children: ['auto', 'full', 'fixed'].map(mode => ({ label: menuLabel({ auto: '默认宽度', full: '填充容器', fixed: '固定宽度' }[mode], (block.props?.style?.widthMode || 'full') === mode), key: `size:width:${mode}`, icon: () => renderNavigationMenuIcon(mode === 'auto' ? RemoveOutline : mode === 'full' ? SwapHorizontalOutline : ResizeOutline) })) },
          { type: 'group', label: '高度', key: 'heightGroup', children: ['fixed', 'auto', 'full'].map(mode => ({ label: menuLabel({ fixed: '默认高度', auto: '适应内容', full: '填充容器' }[mode], (block.props?.style?.heightMode || 'fixed') === mode), key: `size:height:${mode}`, icon: () => renderNavigationMenuIcon(mode === 'auto' ? ResizeOutline : mode === 'full' ? ExpandOutline : RemoveOutline) })) },
        ],
      },
      {
        label: '更换背景色',
        key: 'backgroundColor',
        icon: () => renderNavigationMenuIcon(ColorFillOutline),
      },
      {
        label: '背景描边',
        key: 'backgroundBorder',
        icon: () => renderNavigationMenuIcon(SquareOutline),
        children: [
          { label: menuLabel('跟随主题', isThemeBorder), key: 'border:theme', icon: () => renderNavigationMenuIcon(SettingsOutline) },
          {
            label: '粗细',
            key: 'borderWidth',
            children: [
              { label: menuLabel('跟随主题', isThemeBorder), key: 'borderWidth:theme', icon: () => renderNavigationMenuIcon(SettingsOutline) },
              ...[0, 0.5, 1, 2, 3, 4].map(width => ({ label: menuLabel(`${width} px`, borderWidth === width && borderMode !== 'theme'), key: `borderWidth:${width}`, icon: () => renderNavigationMenuIcon(RemoveOutline) })),
            ],
          },
          {
            label: '颜色',
            key: 'borderColor',
            children: [
              { label: menuLabel('跟随主题', borderMode === 'theme'), key: 'border:theme', icon: () => renderNavigationMenuIcon(SettingsOutline) },
              { label: menuLabel('浅灰', borderMode === '#d0d3d8'), key: 'border:#d0d3d8', icon: () => renderNavigationMenuIcon(SquareOutline) },
              { label: menuLabel('深灰', borderMode === '#86909c'), key: 'border:#86909c', icon: () => renderNavigationMenuIcon(SquareOutline) },
              { label: menuLabel('蓝色', borderMode === '#3370ff'), key: 'border:#3370ff', icon: () => renderNavigationMenuIcon(SquareOutline) },
            ],
          },
        ],
      },
      { type: 'divider', key: 'dangerDivider' },
      { label: '删除', key: 'delete', icon: () => renderNavigationMenuIcon(TrashOutline) },
    ]
  }
  __impl.resolvePageBlockMoreOptions = resolvePageBlockMoreOptions

  function arrangePageBlockBesidePrevious(blockId) {
    const items = pageBlocks.value || []
    const index = items.findIndex(item => item.id === blockId)
    if (index <= 0) {
      message.warning('上面没有可并排的组件')
      return false
    }
    const previous = items[index - 1]
    const current = items[index]
    if (!previous || !current)
      return false
    const left = normalizePageBlockForContainer(previous)
    const right = normalizePageBlockForContainer(current)
    const grid = createGridBlock('grid-layout', { fields: [] }, { gridX: 0, gridY: 0 })
    const gridId = `grid_side_${Date.now().toString(36)}`
    const row = {
      ...grid,
      id: gridId,
      label: '并排布局',
      props: {
        ...(grid.props || {}),
        columns: 24,
        gutter: 16,
        rowGap: 16,
        cellMinHeight: 88,
        alignItems: 'stretch',
        justifyItems: 'stretch',
        showCellBorder: false,
        cellBackground: 'transparent',
        cells: [
          { key: 'side_left', title: '左侧', span: 12, children: [left] },
          { key: 'side_right', title: '右侧', span: 12, children: [right] },
        ],
        style: {
          ...createDefaultBlockStyle('full'),
          ...(grid.props?.style || {}),
          widthMode: 'full',
          width: '100%',
          heightMode: 'auto',
          height: 'auto',
          backgroundColor: 'transparent',
          borderColor: 'transparent',
          borderWidth: 0,
          padding: 0,
          pageFlowX: 0,
          pageFlowY: Number(previous.props?.style?.pageFlowY) || 0,
        },
      },
      children: [],
    }
    const nextItems = [
      ...items.slice(0, index - 1),
      row,
      ...items.slice(index + 1),
    ]
    updatePageBlocks(nextItems, { resolveCollisions: true, changedBlockId: gridId })
    selectedPageBlockId.value = gridId
    message.success('已与上一组件并排；选中格子内组件可继续拖左右锚点调列宽')
    return true
  }
  __impl.arrangePageBlockBesidePrevious = arrangePageBlockBesidePrevious

  function handlePageBlockMoreSelect(key, block) {
    const index = pageBlocks.value.findIndex(item => item.id === block.id)
    if (index < 0)
      return
    selectPageBlock(block.id)
    if (key === 'arrangeBesidePrevious') {
      arrangePageBlockBesidePrevious(block.id)
      return
    }
    if (key === 'configure') {
      configPanelVisible.value = true
      return
    }
    if (key === 'copyToPage') {
      copyBlockId.value = block.id
      copyBlockTargetPageId.value = ''
      copyBlockVisible.value = true
      return
    }
    if (key.startsWith('moveInto:')) {
      movePageBlockIntoContainer(block.id, key.slice('moveInto:'.length))
      return
    }
    if (key === 'backgroundColor') {
      backgroundPickerBlockId.value = block.id
      blockBackgroundPickerVisible.value = true
      return
    }
    if (key.startsWith('size:')) {
      updatePageBlockSize(block, key)
      return
    }
    if (key.startsWith('background:')) {
      updateSelectedBlockAppearance({ backgroundColor: key.slice('background:'.length) || 'transparent' })
      return
    }
    if (key.startsWith('borderWidth:')) {
      const value = key.slice('borderWidth:'.length)
      updateSelectedBlockAppearance(value === 'theme'
        ? { borderColor: 'theme', borderWidth: 1 }
        : { borderWidth: Number(value) || 0 })
      return
    }
    if (key.startsWith('border:')) {
      updateSelectedBlockAppearance({ borderColor: key.slice('border:'.length) || 'theme' })
      return
    }
    const items = [...pageBlocks.value]
    if (key === 'duplicate') {
      const copy = JSON.parse(JSON.stringify(block))
      copy.id = `${block.blockType}_${Date.now()}`
      copy.label = `${block.label || resolveListPageBlockMeta(block.blockType)?.title || '区块'} 副本`
      items.splice(index + 1, 0, copy)
      updatePageBlocks(items)
      selectedPageBlockId.value = copy.id
      return
    }
    if (key === 'delete') {
      items.splice(index, 1)
      updatePageBlocks(items)
      selectedPageBlockId.value = ''
      return
    }
    updatePageBlocks(items)
  }
  __impl.handlePageBlockMoreSelect = handlePageBlockMoreSelect

  function resolvePageBlockMoveIntoOptions(block = {}) {
    return pageBlocks.value
      .filter((candidate) => {
        if (!candidate?.id || candidate.id === block.id)
          return false
        const meta = resolveListPageBlockMeta(candidate.blockType)
        return meta?.container === true && ['grid-layout', 'box-layout', 'card', 'tabs'].includes(candidate.blockType)
      })
      .map((candidate) => {
        const meta = resolveListPageBlockMeta(candidate.blockType)
        return {
          label: `${meta?.title || '布局组合'} · ${candidate.label || meta?.title || '未命名布局'}`,
          key: `moveInto:${candidate.id}`,
          icon: () => renderNavigationMenuIcon(candidate.blockType === 'tabs' ? DocumentTextOutline : MoveOutline),
        }
      })
  }
  __impl.resolvePageBlockMoveIntoOptions = resolvePageBlockMoveIntoOptions

  function movePageBlockIntoContainer(blockId, containerId) {
    if (!blockId || !containerId || blockId === containerId)
      return
    const source = pageBlocks.value.find(item => item.id === blockId)
    const container = pageBlocks.value.find(item => item.id === containerId)
    if (!source || !container)
      return

    const nested = normalizePageBlockForContainer(source)
    const nextItems = pageBlocks.value
      .filter(item => item.id !== blockId)
      .map((item) => {
        if (item.id !== containerId)
          return item
        if (item.blockType === 'grid-layout') {
          const cells = Array.isArray(item.props?.cells) && item.props.cells.length
            ? item.props.cells.map(cell => ({ ...cell, children: [...(cell.children || [])] }))
            : [{ key: 'cell_1', title: '栅格 1', span: 24, children: [] }]
          cells[0] = { ...cells[0], children: [...cells[0].children, nested] }
          return { ...item, props: { ...(item.props || {}), cells } }
        }
        if (item.blockType === 'tabs') {
          const tabs = Array.isArray(item.props?.tabs) && item.props.tabs.length
            ? item.props.tabs.map(tab => ({ ...tab, children: [...(tab.children || [])] }))
            : [{ key: 'tab1', title: '标签一', children: [] }]
          tabs[0] = { ...tabs[0], children: [...tabs[0].children, nested] }
          return { ...item, props: { ...(item.props || {}), tabs } }
        }
        return { ...item, children: [...(item.children || []), nested] }
      })
    updatePageBlocks(nextItems, { resolveCollisions: true, changedBlockId: containerId })
    selectedPageBlockId.value = containerId
    message.success(`组件已移入${container.label || resolveListPageBlockMeta(container.blockType)?.title || '布局组合'}`)
  }
  __impl.movePageBlockIntoContainer = movePageBlockIntoContainer

  function normalizePageBlockForContainer(block) {
    const {
      pageFlowX,
      pageFlowY,
      pageFlowWidth,
      pageFlowHeight,
      x,
      y,
      left,
      top,
      // 顶层 createDefaultBlockStyle 默认 height:100% + heightMode:fixed；
      // 进嵌套后百分比无参照，必须从 style 里解构丢掉，不能只设 undefined（仍可能被残留）。
      height: _topLevelHeight,
      heightMode: _topLevelHeightMode,
      ...containerStyle
    } = block.props?.style || {}
    const isChart = ['bar-chart', 'line-chart'].includes(block?.blockType)
    const chartHeight = Math.max(
      200,
      Number(block?.props?.height) || 0,
      readPageBlockLength(_topLevelHeight),
      280,
    )
    return {
      ...JSON.parse(JSON.stringify(block)),
      props: {
        ...(block.props || {}),
        style: {
          ...containerStyle,
          // 嵌套容器内默认铺满宽、高度跟内容，禁止带出画布绝对坐标/100% 固定高
          widthMode: 'full',
          width: '100%',
          maxWidth: '100%',
          // 图表必须保留固定壳高，否则 height:100% / 默认 96 会把画布挤出格子叠在一起
          heightMode: isChart ? 'fixed' : 'auto',
          ...(isChart ? { height: chartHeight } : {}),
          x: 0,
          y: 0,
          left: 0,
          top: 0,
        },
      },
    }
  }
  __impl.normalizePageBlockForContainer = normalizePageBlockForContainer

  function updatePageBlockSize(block, key) {
    const [, axis, mode] = key.split(':')
    const style = block.props?.style || {}
    const nextStyle = { ...style }
    if (axis === 'width') {
      nextStyle.widthMode = mode
      nextStyle.width = mode === 'full' ? '100%' : mode === 'auto' ? 'auto' : Math.max(280, Math.min(640, readPageBlockLength(style.width, 640)))
      nextStyle.pageFlowWidth = mode === 'full' ? 'calc(100% - 48px)' : `${readPageBlockLength(nextStyle.width, mode === 'auto' ? 520 : 640)}px`
    }
    if (axis === 'height')
      nextStyle.heightMode = mode
    updatePageBlocks(
      pageBlocks.value.map(item => item.id === block.id ? { ...item, props: { ...(item.props || {}), style: nextStyle } } : item),
      { resolveCollisions: true, changedBlockId: block.id },
    )
  }
  __impl.updatePageBlockSize = updatePageBlockSize

  function resolvePageBlockBackgroundColor(block = {}) {
    const value = String(block.props?.style?.backgroundColor || '').trim()
    return value && value !== 'transparent' ? value : '#FFFFFF00'
  }
  __impl.resolvePageBlockBackgroundColor = resolvePageBlockBackgroundColor

  function updatePageBlockBackgroundColor(block, value) {
    if (!block?.id)
      return
    selectPageBlock(block.id)
    updateSelectedBlockAppearance({ backgroundColor: value || 'transparent' })
  }
  __impl.updatePageBlockBackgroundColor = updatePageBlockBackgroundColor

  function copySelectedBlockToOtherPage() {
    const block = pageBlocks.value.find(item => item.id === copyBlockId.value)
    const targetPage = builder.value?.pages?.[copyBlockTargetPageId.value]
    if (!block || !targetPage)
      return
    const targetLayout = targetPage.layout?.gridLayout || {
      cols: 12,
      rowHeight: 32,
      gap: 8,
      designWidth: 1366,
      layoutType: 'simple-crud',
      items: [],
    }
    const copy = JSON.parse(JSON.stringify(block))
    copy.id = `${block.blockType}_${Date.now()}`
    copy.label = `${block.label || resolveListPageBlockMeta(block.blockType)?.title || '组件'} 副本`
    const targetItems = [...(targetLayout.items || []), copy]
    builder.value = {
      ...builder.value,
      pages: {
        ...builder.value.pages,
        [copyBlockTargetPageId.value]: {
          ...targetPage,
          layout: { ...targetPage.layout, gridLayout: { ...targetLayout, items: targetItems } },
        },
      },
    }
    copyBlockVisible.value = false
    message.success('组件已复制到目标页面')
  }
  __impl.copySelectedBlockToOtherPage = copySelectedBlockToOtherPage

  function updateSelectedBlockAppearance(patch = {}) {
    if (!selectedPageBlock.value)
      return
    const borderColorMode = patch.borderColor || itemBorderColorMode(selectedPageBlock.value)
    const normalizeBorderColor = borderColorMode === 'theme' ? 'var(--primary-color, #3370ff)' : borderColorMode
    updatePageBlocks(mapPageBlocksInTree(pageBlocks.value, item => item.id === selectedPageBlock.value.id
      ? {
          ...item,
          props: {
            ...(item.props || {}),
            style: {
              ...(item.props?.style || {}),
              ...patch,
              ...(Object.prototype.hasOwnProperty.call(patch, 'borderColor')
                ? { borderColor: normalizeBorderColor, borderColorMode }
                : {}),
              borderStyle: Number(patch.borderWidth ?? item.props?.style?.borderWidth ?? 0) > 0 ? 'solid' : 'none',
            },
          },
        }
      : item))
  }
  __impl.updateSelectedBlockAppearance = updateSelectedBlockAppearance

  function itemBorderColorMode(block = {}) {
    return block.props?.style?.borderColorMode || (block.props?.style?.borderColor === 'var(--primary-color, #3370ff)' ? 'theme' : block.props?.style?.borderColor) || 'theme'
  }
  __impl.itemBorderColorMode = itemBorderColorMode

  function handleNestedPageBlockResizeStart(payload = {}) {
    const block = payload.block
    const event = payload.event
    if (!block?.id || !event || event.button !== 0)
      return
    const node = event.currentTarget?.closest?.('[data-grid-child-id]')
    const rect = node?.getBoundingClientRect?.()
    if (!rect)
      return
    event.preventDefault()
    handleNestedPageBlockSelect(block.id)
    const parent = node?.parentElement
    const parentRect = parent?.getBoundingClientRect?.()
    const parentStyle = parent ? window.getComputedStyle(parent) : null
    const parentPadX = parentStyle
      ? (Number.parseFloat(parentStyle.paddingLeft) || 0) + (Number.parseFloat(parentStyle.paddingRight) || 0)
      : 0
    const cellHost = findGridCellHostForChild(pageBlocks.value, block.id)
    // 栅格内左右拖：按整行栅格宽度换算列宽，不再锁死在当前格子里
    const gridRoot = node?.closest?.('[data-page-block-type="grid-layout"],[data-page-block-type="grid"]')
      || node?.closest?.('.application-page-block')
    const gridRect = gridRoot?.getBoundingClientRect?.()
    const cellMaxWidth = parentRect?.width
      ? Math.max(120, Math.floor(parentRect.width - parentPadX))
      : 0
    const gridMaxWidth = gridRect?.width
      ? Math.max(120, Math.floor(gridRect.width))
      : cellMaxWidth
    mut.nestedPageBlockResizeCtx = {
      blockId: block.id,
      blockType: block.blockType || '',
      anchor: payload.anchor || 'bottom-right',
      startX: event.clientX,
      startY: event.clientY,
      originWidth: rect.width,
      originHeight: rect.height,
      maxWidth: cellHost ? gridMaxWidth : cellMaxWidth,
      cellHost,
    }
    window.addEventListener('pointermove', resizeNestedPageBlock)
    window.addEventListener('pointerup', endNestedPageBlockResize)
  }
  __impl.handleNestedPageBlockResizeStart = handleNestedPageBlockResizeStart

  function handleNestedPageBlockMoveStart(payload = {}) {
    const block = payload.block
    const event = payload.event
    if (!editing.value || !block?.id || !event || event.button !== 0)
      return
    const node = event.currentTarget?.closest?.('[data-grid-child-id]')
      || event.target?.closest?.('[data-grid-child-id]')
    const flow = document.querySelector('.application-page-flow')
    const rect = node?.getBoundingClientRect?.()
    const flowRect = flow?.getBoundingClientRect?.()
    if (!rect || !flowRect)
      return
    event.preventDefault()
    event.stopPropagation()
    handleNestedPageBlockSelect(block.id)
    const originX = Math.round(rect.left - flowRect.left + (flow.scrollLeft || 0))
    const originY = Math.round(rect.top - flowRect.top + (flow.scrollTop || 0))
    mut.nestedPageBlockMoveCtx = {
      blockId: block.id,
      startX: event.clientX,
      startY: event.clientY,
      originX,
      originY,
      width: rect.width,
      height: rect.height,
      maxX: Math.max(0, flowRect.width - rect.width),
      moved: false,
    }
    nestedMovingPageBlockId.value = block.id
    dragPreview.value = {
      blockId: block.id,
      x: originX,
      y: originY,
      width: rect.width,
      height: rect.height,
      nested: true,
    }
    document.body.classList.add('is-nested-page-block-moving')
    window.addEventListener('pointermove', onNestedPageBlockMove, { passive: false })
    window.addEventListener('pointerup', endNestedPageBlockMove)
    window.addEventListener('pointercancel', endNestedPageBlockMove)
  }
  __impl.handleNestedPageBlockMoveStart = handleNestedPageBlockMoveStart

  function onNestedPageBlockMove(event) {
    mut.pendingNestedPageBlockMoveEvent = event
    if (mut.nestedPageBlockMoveFrame)
      return
    mut.nestedPageBlockMoveFrame = window.requestAnimationFrame(() => {
      mut.nestedPageBlockMoveFrame = 0
      const nextEvent = mut.pendingNestedPageBlockMoveEvent
      mut.pendingNestedPageBlockMoveEvent = null
      if (nextEvent)
        applyNestedPageBlockMove(nextEvent)
    })
  }
  __impl.onNestedPageBlockMove = onNestedPageBlockMove

  function applyNestedPageBlockMove(event) {
    if (!mut.nestedPageBlockMoveCtx)
      return
    event.preventDefault?.()
    const ctx = mut.nestedPageBlockMoveCtx
    if (!ctx.moved && Math.hypot(event.clientX - ctx.startX, event.clientY - ctx.startY) > 3)
      ctx.moved = true
    const pageFlowX = Math.round(Math.max(0, Math.min(ctx.maxX, ctx.originX + event.clientX - ctx.startX)))
    const pageFlowY = Math.round(Math.max(0, ctx.originY + event.clientY - ctx.startY))
    dragPreview.value = { ...dragPreview.value, x: pageFlowX, y: pageFlowY }
    pageAlignGuides.value = {
      visible: true,
      cross: {
        x: Math.round(pageFlowX + ctx.width / 2),
        y: Math.round(pageFlowY + ctx.height / 2),
      },
      x: [],
      y: [],
    }
    ctx.activeTabTarget = resolvePageFlowTabTargetFromPoint(event)
    ctx.activeGridTarget = resolvePageFlowGridCellTargetFromPoint(event)
    activePageFlowGridTarget.value = ctx.activeGridTarget
      ? {
          containerId: ctx.activeGridTarget.blockId,
          cellKey: ctx.activeGridTarget.cellKey,
          cellIndex: ctx.activeGridTarget.cellIndex,
        }
      : null
    ctx.activeContainerTarget = ctx.activeGridTarget
      ? null
      : normalizeContainerDropTarget(resolvePageFlowContainerTargetFromPoint(event))
    activePageFlowContainerTarget.value = ctx.activeContainerTarget
  }
  __impl.applyNestedPageBlockMove = applyNestedPageBlockMove

  function endNestedPageBlockMove(event) {
    if (mut.nestedPageBlockMoveFrame) {
      window.cancelAnimationFrame(mut.nestedPageBlockMoveFrame)
      mut.nestedPageBlockMoveFrame = 0
      mut.pendingNestedPageBlockMoveEvent = null
    }
    const ctx = mut.nestedPageBlockMoveCtx
    mut.nestedPageBlockMoveCtx = null
    window.removeEventListener('pointermove', onNestedPageBlockMove)
    window.removeEventListener('pointerup', endNestedPageBlockMove)
    window.removeEventListener('pointercancel', endNestedPageBlockMove)
    document.body.classList.remove('is-nested-page-block-moving')
    nestedMovingPageBlockId.value = ''
    activePageFlowGridTarget.value = null
    activePageFlowContainerTarget.value = null
    clearPageAlignGuides()
    const preview = dragPreview.value
    dragPreview.value = null
    if (!ctx?.moved)
      return
    const blockId = ctx.blockId
    const tabTarget = ctx.activeTabTarget
      || (event ? resolvePageFlowTabTargetFromPoint(event) : null)
    if (tabTarget && relocateNestedPageBlockToTab(blockId, tabTarget.blockId, tabTarget.tabKey))
      return
    const gridTarget = ctx.activeGridTarget
      || (event ? resolvePageFlowGridCellTargetFromPoint(event) : null)
    if (gridTarget && relocateNestedPageBlockToGridCell(blockId, gridTarget.blockId, gridTarget.cellKey))
      return
    const containerTarget = normalizeContainerDropTarget(
      ctx.activeContainerTarget || (event ? resolvePageFlowContainerTargetFromPoint(event) : null),
    )
    if (containerTarget && relocateNestedPageBlockToContainer(blockId, containerTarget.containerId))
      return
    extractNestedPageBlockToCanvas(
      blockId,
      Math.round(preview?.x ?? ctx.originX),
      Math.round(preview?.y ?? ctx.originY),
      Math.round(preview?.width ?? ctx.width),
      Math.round(preview?.height ?? ctx.height),
    )
  }
  __impl.endNestedPageBlockMove = endNestedPageBlockMove

  function relocateNestedPageBlockToGridCell(blockId, containerId, cellKey) {
    const source = findPageBlockInTree(pageBlocks.value, blockId)
    const container = findPageBlockInTree(pageBlocks.value, containerId)
    if (!source || !container)
      return false
    if (['card', 'box-layout'].includes(container.blockType))
      return relocateNestedPageBlockToContainer(blockId, containerId)
    if (container.blockType !== 'grid-layout')
      return false
    // 禁止拖进自身或自己的子孙
    if (source.id === containerId || findPageBlockInTree([source], containerId))
      return false
    const cells = Array.isArray(container.props?.cells) ? container.props.cells : []
    let targetKey = String(cellKey || '')
    if (!cells.some(cell => String(cell.key) === targetKey))
      targetKey = String(cells[0]?.key || '')
    if (!targetKey)
      return false
    // 已在目标格则不动
    const alreadyInTarget = cells.some(cell => String(cell.key) === targetKey
      && (cell.children || []).some(child => child.id === blockId))
    if (alreadyInTarget)
      return true
    const nested = normalizePageBlockForContainer(source)
    const withoutSource = removePageBlockFromTree(pageBlocks.value, blockId)
    updatePageBlocks(mapPageBlocksInTree(withoutSource, item => item.id === containerId
      ? {
          ...item,
          props: {
            ...(item.props || {}),
            cells: (item.props?.cells || []).map(cell => String(cell.key) === targetKey
              ? { ...cell, children: [...(cell.children || []), nested] }
              : cell),
          },
        }
      : item))
    selectedPageBlockId.value = blockId
    return true
  }
  __impl.relocateNestedPageBlockToGridCell = relocateNestedPageBlockToGridCell

  function relocateNestedPageBlockToTab(blockId, containerId, tabKey) {
    const source = findPageBlockInTree(pageBlocks.value, blockId)
    const container = findPageBlockInTree(pageBlocks.value, containerId)
    if (!source || container?.blockType !== 'tabs' || source.id === containerId)
      return false
    const tabs = Array.isArray(container.props?.tabs) ? container.props.tabs : []
    if (!tabs.some(tab => tab.key === tabKey))
      return false
    const nested = normalizePageBlockForContainer(source)
    const withoutSource = removePageBlockFromTree(pageBlocks.value, blockId)
    updatePageBlocks(mapPageBlocksInTree(withoutSource, item => item.id === containerId
      ? {
          ...item,
          props: {
            ...(item.props || {}),
            tabs: (item.props?.tabs || []).map(tab => tab.key === tabKey
              ? { ...tab, children: [...(tab.children || []), nested] }
              : tab),
          },
        }
      : item))
    selectedPageBlockId.value = blockId
    return true
  }
  __impl.relocateNestedPageBlockToTab = relocateNestedPageBlockToTab

  function relocateNestedPageBlockToContainer(blockId, containerId) {
    const source = findPageBlockInTree(pageBlocks.value, blockId)
    const container = findPageBlockInTree(pageBlocks.value, containerId)
    if (!source || !container || !['card', 'box-layout'].includes(container.blockType))
      return false
    if (source.id === containerId || findPageBlockInTree([source], containerId))
      return false
    if ((container.children || []).some(child => child.id === blockId))
      return true
    const nested = normalizePageBlockForContainer(source)
    const withoutSource = removePageBlockFromTree(pageBlocks.value, blockId)
    updatePageBlocks(mapPageBlocksInTree(withoutSource, item => item.id === containerId
      ? { ...item, children: [...(item.children || []), nested] }
      : item))
    selectedPageBlockId.value = blockId
    return true
  }
  __impl.relocateNestedPageBlockToContainer = relocateNestedPageBlockToContainer

  function extractNestedPageBlockToCanvas(blockId, x, y, width, height) {
    const source = findPageBlockInTree(pageBlocks.value, blockId)
    if (!source)
      return false
    // 已是根级则只更新坐标
    if (pageBlocks.value.some(item => item.id === blockId)) {
      updatePageBlocks(pageBlocks.value.map(item => item.id === blockId
        ? {
            ...item,
            props: {
              ...(item.props || {}),
              style: {
                ...(item.props?.style || {}),
                pageFlowX: x,
                pageFlowY: y,
                pageFlowWidth: `${width}px`,
                pageFlowHeight: `${height}px`,
                widthMode: 'fixed',
                width,
                heightMode: 'fixed',
                height,
              },
            },
          }
        : item))
      selectedPageBlockId.value = blockId
      return true
    }
    const withoutSource = removePageBlockFromTree(pageBlocks.value, blockId)
    const restored = {
      ...JSON.parse(JSON.stringify(source)),
      props: {
        ...(source.props || {}),
        style: {
          ...(source.props?.style || {}),
          pageFlowX: x,
          pageFlowY: y,
          pageFlowWidth: `${width}px`,
          pageFlowHeight: `${height}px`,
          widthMode: 'fixed',
          width,
          heightMode: 'fixed',
          height,
        },
      },
    }
    updatePageBlocks([...withoutSource, restored])
    selectedPageBlockId.value = blockId
    return true
  }
  __impl.extractNestedPageBlockToCanvas = extractNestedPageBlockToCanvas

  function resizeNestedPageBlock(event) {
    const ctx = mut.nestedPageBlockResizeCtx
    if (!ctx)
      return
    const dx = event.clientX - ctx.startX
    const dy = event.clientY - ctx.startY
    let width = ctx.originWidth
    let height = ctx.originHeight
    const resizingWidth = ctx.anchor.includes('left') || ctx.anchor.includes('right')
    const resizingHeight = ctx.anchor.includes('top') || ctx.anchor.includes('bottom')
    if (ctx.anchor.includes('right'))
      width += dx
    if (ctx.anchor.includes('left'))
      width -= dx
    if (ctx.anchor.includes('bottom'))
      height += dy
    if (ctx.anchor.includes('top'))
      height -= dy
    const maxWidth = Number(ctx.maxWidth) > 0 ? Number(ctx.maxWidth) : Number.POSITIVE_INFINITY
    const nextWidth = Math.min(maxWidth, Math.max(120, Math.round(width)))
    const nextHeight = Math.max(40, Math.round(height))
    const host = ctx.cellHost
    const nextSpan = host && resizingWidth && ctx.originWidth > 0
      ? Math.min(
          host.columns,
          Math.max(2, Math.round(host.span * (nextWidth / ctx.originWidth))),
        )
      : null

    updatePageBlocks(mapPageBlocksInTree(pageBlocks.value, (block) => {
      // 左右锚点：改所在格子的 span，图表始终通栏填满格子（真正改变页面上的宽度）
      if (host && nextSpan != null && block.id === host.gridId) {
        const cells = Array.isArray(block.props?.cells) ? block.props.cells : []
        return {
          ...block,
          props: {
            ...(block.props || {}),
            cells: cells.map((cell) => {
              if (String(cell.key) !== String(host.cellKey))
                return cell
              return { ...cell, span: nextSpan }
            }),
          },
        }
      }
      if (block.id !== ctx.blockId)
        return block
      const prevStyle = block.props?.style || {}
      const nextStyle = { ...prevStyle }
      if (host && nextSpan != null) {
        // 栅格内横向只改列宽，组件保持铺满格子
        nextStyle.widthMode = 'full'
        nextStyle.width = '100%'
        nextStyle.maxWidth = '100%'
        delete nextStyle.pageFlowWidth
      }
      else if (resizingWidth) {
        const useFullWidth = Number.isFinite(maxWidth) && nextWidth >= maxWidth - 2
        nextStyle.widthMode = useFullWidth ? 'full' : 'fixed'
        nextStyle.width = useFullWidth ? '100%' : `${nextWidth}px`
        nextStyle.maxWidth = '100%'
      }
      if (resizingHeight) {
        nextStyle.heightMode = 'fixed'
        nextStyle.height = `${nextHeight}px`
        nextStyle.minHeight = nextHeight
        nextStyle.pageFlowHeight = nextHeight
      }
      const nextProps = {
        ...(block.props || {}),
        style: nextStyle,
      }
      if (resizingHeight && ['bar-chart', 'line-chart'].includes(block.blockType))
        nextProps.height = Math.max(160, nextHeight)
      return {
        ...block,
        props: nextProps,
      }
    }))
  }
  __impl.resizeNestedPageBlock = resizeNestedPageBlock

  function endNestedPageBlockResize() {
    mut.nestedPageBlockResizeCtx = null
    window.removeEventListener('pointermove', resizeNestedPageBlock)
    window.removeEventListener('pointerup', endNestedPageBlockResize)
  }
  __impl.endNestedPageBlockResize = endNestedPageBlockResize

  __impl.syncCatalogDropActiveClass = syncCatalogDropActiveClass
  __impl.finishCatalogPointerDrag = finishCatalogPointerDrag
  __impl.handleComponentCatalogDragEnd = handleComponentCatalogDragEnd
  __impl.handleComponentCatalogClick = handleComponentCatalogClick
  __impl.isPageCatalogDrag = isPageCatalogDrag
  __impl.resolvePageFlowTabTarget = resolvePageFlowTabTarget
  __impl.resolvePageFlowTabTargetFromPoint = resolvePageFlowTabTargetFromPoint
  __impl.resolvePageFlowGridCellTarget = resolvePageFlowGridCellTarget
  __impl.resolvePageFlowGridCellTargetFromPoint = resolvePageFlowGridCellTargetFromPoint
  __impl.resolvePageFlowGridCellTargetByRect = resolvePageFlowGridCellTargetByRect
  __impl.resolvePageFlowGridCellTargetByPageBlock = resolvePageFlowGridCellTargetByPageBlock
  __impl.resolvePageFlowGridCellTargetFromElement = resolvePageFlowGridCellTargetFromElement
  __impl.resolvePageFlowContainerTargetFromPoint = resolvePageFlowContainerTargetFromPoint
  __impl.resolvePageFlowContainerTargetFromElement = resolvePageFlowContainerTargetFromElement
  __impl.resolvePageFlowContainerTargetByRect = resolvePageFlowContainerTargetByRect
  __impl.resolvePageFlowContainerTargetByPageBlock = resolvePageFlowContainerTargetByPageBlock
  __impl.handlePageFlowDragOver = handlePageFlowDragOver
  __impl.handlePageFlowDrop = handlePageFlowDrop
  __impl.resolveFormLayoutBlockType = resolveFormLayoutBlockType
  __impl.handlePageFlowTabDrop = handlePageFlowTabDrop
  __impl.handlePageFlowGridCellDrop = handlePageFlowGridCellDrop
  __impl.handlePageFlowContainerInsert = handlePageFlowContainerInsert
  __impl.handlePageFlowContainerClear = handlePageFlowContainerClear
  __impl.appendPageBlockToContainer = appendPageBlockToContainer
  __impl.handleNestedPageBlockMenuSelect = handleNestedPageBlockMenuSelect
  __impl.duplicateNestedPageBlock = duplicateNestedPageBlock
  __impl.clonePageBlockTree = clonePageBlockTree
  __impl.appendPageBlockToGridCell = appendPageBlockToGridCell
  __impl.appendPageBlockToTab = appendPageBlockToTab
  __impl.resolvePageBlockMoreOptions = resolvePageBlockMoreOptions
  __impl.handlePageBlockMoreSelect = handlePageBlockMoreSelect
  __impl.arrangePageBlockBesidePrevious = arrangePageBlockBesidePrevious
  __impl.resolvePageBlockMoveIntoOptions = resolvePageBlockMoveIntoOptions
  __impl.movePageBlockIntoContainer = movePageBlockIntoContainer
  __impl.normalizePageBlockForContainer = normalizePageBlockForContainer
  __impl.updatePageBlockSize = updatePageBlockSize
  __impl.resolvePageBlockBackgroundColor = resolvePageBlockBackgroundColor
  __impl.updatePageBlockBackgroundColor = updatePageBlockBackgroundColor
  __impl.copySelectedBlockToOtherPage = copySelectedBlockToOtherPage
  __impl.updateSelectedBlockAppearance = updateSelectedBlockAppearance
  __impl.itemBorderColorMode = itemBorderColorMode
  __impl.handleNestedPageBlockResizeStart = handleNestedPageBlockResizeStart
  __impl.handleNestedPageBlockMoveStart = handleNestedPageBlockMoveStart
  __impl.onNestedPageBlockMove = onNestedPageBlockMove
  __impl.applyNestedPageBlockMove = applyNestedPageBlockMove
  __impl.endNestedPageBlockMove = endNestedPageBlockMove
  __impl.relocateNestedPageBlockToGridCell = relocateNestedPageBlockToGridCell
  __impl.relocateNestedPageBlockToTab = relocateNestedPageBlockToTab
  __impl.relocateNestedPageBlockToContainer = relocateNestedPageBlockToContainer
  __impl.extractNestedPageBlockToCanvas = extractNestedPageBlockToCanvas
  __impl.resizeNestedPageBlock = resizeNestedPageBlock
  __impl.endNestedPageBlockResize = endNestedPageBlockResize

  return {
    ...deps,
  }
}
