/** application-runtime setup part 4. */
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
import { createGridBlock, DATA_FIELD_BLOCK_TYPES, isDataFieldBlockType, listPageBlockCatalog, resolveListPageBlockMeta } from '@/components/lowcode-builder/page/page-schema'
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
  pageHasFreeLayoutCompanionBlocks,
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
import { findPageBlockInTree, mapPageBlocksInTree, removePageBlockFromTree, visitPageBlocksInTree } from './page-block-tree'
import { resolvePageBlockShellStyle as computePageBlockShellStyle, readPageBlockLength, resolveDefaultPageBlockHeight, resolveDefaultPageBlockYFromItems, resolvePageBlockFlowGeometry, resolveRootPageBlockCollisions, applyMeasuredPageBlockHeights, shouldUsePageFlowStack, normalizePagePadding, resolvePagePaddingCss, migratePageFlowToContentCoords, canvasToContentFlowPoint, contentToCanvasFlowPoint, snapResizeRectToPeers, DEFAULT_PAGE_PADDING } from './page-flow-geometry'
import { flattenNodes, hasPermission, isNavigationVisible } from './runtime-navigation-utils'
import { useBuilderHistory } from './use-builder-history'
import { useRuntimeCrudConfig } from './use-runtime-crud-config'

export function applyApplicationRuntimePart4(deps = {}) {
  const {
    __impl, mut, appendPageBlock, appendPageBlockToContainer, appendPageBlockToGridCell, appendPageBlockToTab, applyNestedPageBlockMove, applyPageTemplate,
    attachDefaultRuntimeObject, attachSingleFormAsset, bindSingleFormToCompatibleBlocks, cancelRenameApplication, clonePageBlockTree, confirmNavigationAction, confirmRenameApplication, confirmSystemMenuMount,
    copySelectedBlockToOtherPage, createFormAssetForCurrentPage, createFormAssetForPageCrud, createFormAssetForSelectedBlock, createFormFieldVisibilitySettings, createPageFromTemplate, createQuickNode, createStandaloneFormAsset,
    duplicateNavigationNode, duplicateNestedPageBlock, editSelectedBlockFormAsset, endNestedPageBlockMove, endNestedPageBlockResize, ensureFormDesignerObjectContext, ensurePageFlowContentCoords, ensurePageTitleComponents,
    enterPageDesign, enterWorkbenchDesign, extractNestedPageBlockToCanvas, findFirstBlockWithFormAssetId, findPageBlockByFormAssetId, finishCatalogPointerDrag, handleApplicationObjectsChanged, handleCatalogPointerMove,
    handleComponentCatalogClick, handleComponentCatalogDragEnd, handleComponentCatalogDragStart, handleInlineTextUpdate, handleNavigationMoreSelect, handleNestedPageBlockMenuSelect, handleNestedPageBlockMoveStart, handleNestedPageBlockResizeStart,
    handleNestedPageBlockSelect, handlePageBlockDataSourceRequest, handlePageBlockMoreSelect, handlePageFlowBlankClick, handlePageFlowContainerClear, handlePageFlowContainerInsert, handlePageFlowDragOver, handlePageFlowDrop,
    handlePageFlowGridCellDrop, handlePageFlowTabDrop, handlePageTypeSelection, handleRuntimeTreeSelect, hydratePageCrudApiPlaceholders, insertComponent, isGroupCollapsed, isNavigationGroupDescendant,
    isPageBlockDataSourceConfigured, isPageBlockRuntimeCrudLoading, isPageCatalogDrag, isSimpleBodyContainer, isValidPageBlockObjectRef, itemBorderColorMode, load, markBuilderClean,
    moveNavigationByOffset, movePageBlockIntoContainer, normalizeContainerDropTarget, normalizePageBlockForContainer, onNestedPageBlockMove, openBusinessObjectDesign, openCustomPageSelector, openDraftPreview,
    openExcelPageImport, openFormAssetDesigner, openFormAssetDesignerForPage, openObjectDesigner, openObjectResourcePreview, openObjectSetup, openPageBlockConfiguration, openPageTypeSelector,
    openSelectedBlockFormDesigner, openSystemMenuMountDialog, patchCurrentPageNode, patchNavigationNodeMount, prefetchRuntimeWorkspacePanels, preloadCurrentPageCrudRuntimeProps, preloadPageBlockCrudRuntimeProps, readRouteDesignTab,
    refreshWorkspaceMetadata, relocateNestedPageBlockToContainer, relocateNestedPageBlockToGridCell, relocateNestedPageBlockToTab, renderNavigationMenuIcon, resizeNestedPageBlock, resolveActiveDesignerPageId, resolveComponentIcon,
    resolveComponentPickerGroup, resolveEmptyGuideIcon, resolveFormAssetFields, resolveFormAssetIdForPage, resolveFormLayoutBlockType, resolveNavigationMoreOptions, resolveNextNavigationTitle, resolveNextPageBlockStackY,
    resolvePageBlockBackgroundColor, resolvePageBlockFields, resolvePageBlockMoreOptions, resolvePageBlockMoveIntoOptions, resolvePageBlockObjectRef, resolvePageBlockRuntimeCrudProps, resolvePageBlockShellStyle, resolvePageContextFieldCatalog,
    resolvePageFlowContainerTargetByPageBlock, resolvePageFlowContainerTargetByRect, resolvePageFlowContainerTargetFromElement, resolvePageFlowContainerTargetFromPoint, resolvePageFlowGridCellTarget, resolvePageFlowGridCellTargetByPageBlock, resolvePageFlowGridCellTargetByRect, resolvePageFlowGridCellTargetFromElement,
    resolvePageFlowGridCellTargetFromPoint, resolvePageFlowTabTarget, resolvePageFlowTabTargetFromPoint, resolvePagePreviewBlock, resolvePageShapeDesignContext, resolvePageTemplateIcon, resolvePortalFormFields, resolveRuntimeObjectCacheKey,
    resolveRuntimeView, resolveSelectablePageId, resolveSystemPageIcon, restoreCurrentObjectPageLayout, returnToPageDesigner, selectCreatedDesignerPage, selectFormAssetFromPicker, selectIntroTemplate, selectNode,
    selectPageBlock, selectPageManagementNode, startCatalogPointerDrag, startRenameApplication, supportsFormAsset, switchPageDesignTab, syncActiveFormAssetForPage, syncActivePageShapeObject,
    syncCatalogDropActiveClass, toggleGroupExpanded, toggleNavigationVisible, unmountSystemMenu, updateActiveFormDesignerSchema, updateCurrentGridLayout, updatePageBlockBackgroundColor, updatePageBlockSize,
    updatePageBlocks, updatePageCanvasPadding, updateSelectedBlockAppearance, updateSelectedBlockFormAsset, updateSelectedPageBlockRuntimeObject, warmCurrentPortalPageCrud, route, router,
    message, tenantStore, formComponentIconModules, formComponentIconCache, formComponentIconFileByBlockType, userStore, asyncPanelLoader, ApplicationObjectsPanel,
    ApplicationExtensionsPanel, ApplicationProcessPanel, ApplicationSettingsPanel, runtimeWorkspacePanelImporters, BusinessObjectDesignerPage, BusinessProcessPage, ForgeFormDesigner, ListPageGridDesigner,
    IconSelector, PageTypeSelector, PageDesignSettingsPanel, PageDesignPublishPanel, PageManagementSystemView, GridBlockRenderer, application, objects,
    builder, processSelectableObjects, loadError, loading, editCanvasBootstrapping, saving, restoringObjectPageLayout, editing,
    activePageDesignTab, runtimeViewMode, exitEditingVisible, selectedNodeId, enhanceContextPageId, newNodePopoverVisible, selectedPageTemplateKey, iconPickerVisible, iconPickerNodeId,
    navigationActionVisible, navigationActionMode, navigationActionNodeId, navigationActionForm, systemMenuMountVisible, systemMenuMountNodeId, systemMenuMountNode, componentPopoverVisible,
    componentKeyword, savedSignature, selectedPageBlockId, draggingPageBlockId, dragPreview, pageAlignGuides, PAGE_ALIGN_SNAP_PX, configPanelVisible,
    inspectorTab, componentButtonPosition, componentButtonMoveCtx, catalogDragBlockType, suppressCatalogClick, activePageFlowTabTarget, activePageFlowGridTarget, activePageFlowContainerTarget,
    resizeCollisionBlockIds, formDesignerObjectContextByObjectId, formDesignerObjectContextLoadingIds, formDesignerMode, designerTransitionLoading, runtimeTreeFilter, runtimeTreeFilterByBlockId, runtimeTreeActiveKeyByBlockId,
    formDesignerFromPageManagement, activeFormAssetId, activePageShapeDesign, pageTypeSelectorVisible, pageTypeSelectorParentId, pageTypeSelectorDefaultType, sidebarCollapsed, renamingApplication,
    renameApplicationValue, renameSaving, renameInputRef, collapsedGroupIds, copyBlockVisible, copyBlockId, copyBlockTargetPageId, blockBackgroundPickerVisible,
    backgroundPickerBlockId, formAssetSelectorOpen, formAssetSelectorKeyword, formDataProvisioningByAssetId, objectSetupVisible, selectedDesignerResourceKey, workspaceExtensions, workspaceEntries,
    portalCrudConfigRevision, portalCrudSeed, isDraftMode, activeFlowContext, embeddedDesignerRef, embeddedDesignerDirty, embeddedDesignerSaving, suppressHeavyEmbeddedSaveSideEffects, listDesignerSessionActive, formDesignerSessionActive, embeddedProcessDesignerVisible,
    embeddedProcessDesignerId, applicationRuntimeLoadCoordinator, componentPickerGroupOptions, runtimeHeaderMoreOptions, pageManagementSystemPages, systemPageIconMap, currentSystemPage, workbenchPage,
    systemPageNavigationRoutes, designerResourceGroups, activeDesignerResource, designerSection, pageBuilderResourceActive, canEditApplication, usePortalDesignPreview, activeDesignerObject,
    activeResourceConfigKey, previewableResourceActive, legacyBlockTypeMap, pageBlockResizeAnchors, pageBlockRecommendedColors, groupOptions, navigationActionNode, iconPickerNode,
    navigationIconValue, navigationActionTitle, navigationActionHasChildren, navigationDeleteImpact, navigationDeleteTip, moveGroupOptions, navigationNodes, currentNode,
    currentPage, currentPageLayoutPolluted, currentPageManagementTitle, pageDesignObject, currentGridLayout, pageBlocks, pageFlowStackMode, pageCanvasPadding,
    pageCanvasPaddingCss, designerGridLayout, appPageTargetOptions, pageFlowHeight, hasCustomComponentButtonPosition, componentButtonStyle, selectedPageBlock, formAssets, filteredFormAssets,
    pageFormAssetOptions, activeFormAsset, activeFormDesignerSchema, activeFormAssetBlock, activeFormDesignerObjectRef, activeFormDesignerContext, activeFormDesignerRelations, activeFormDesignerActions,
    activeFormFields, activeFormDataState, selectedPageBlockFormAssetId, selectedPageBlockFormAsset, selectedPageBlockRuntimeObjectRef, selectedPageBlockIsCrud, selectedPageBlockSupportsDataSource, selectedPageBlockUsesObjectRuntime,
    selectedPageBlockRuntimeObjectId, selectedPageBlockObjectDesignerPanel, selectedPageBlockFormDataState, runtimeObjectFormOptions, pageTemplateOptions, selectedPageBlockFields, selectedPageBlockFormDesignerSchema, applicationGridModelSchema,
    dragPreviewBlock, nestedMovingPageBlockId, copyBlockPageOptions, dirty, canSaveActiveFormDesigner, loadErrorTitle, filteredComponents, componentPickerGroups,
    recommendedComponents,
    runtimeCrudPropsByObjectId, runtimeCrudLoadingObjectIds, runtimeCrudUnavailableObjectIds, loadRuntimeCrudProps, resetRuntimeCrudConfig, historyReady, canUndo, canRedo, resetBuilderHistory, runWithoutHistory,
    undoBuilder, redoBuilder, handleBuilderShortcut,
  
  } = deps
  const PAGE_CHART_BLOCK_TYPES = new Set(['bar-chart', 'line-chart', 'area-chart', 'pie-chart'])

  function startPageBlockResize(block, event, anchor = 'bottom-right') {
    if (event.button !== 0)
      return
    const node = event.currentTarget.closest('[data-page-block-id]')
    const flow = node?.parentElement
    const rect = node?.getBoundingClientRect?.()
    const flowRect = flow?.getBoundingClientRect?.()
    if (!rect || !flowRect)
      return
    event.preventDefault()
    selectPageBlock(block.id)
    if (!pageFlowStackMode.value)
      updateCurrentGridLayout(ensurePageFlowContentCoords({ ...(currentGridLayout.value || {}) }))
    const pad = pageCanvasPadding.value
    mut.pageBlockResizeCtx = {
      blockId: block.id,
      blockType: block.blockType,
      anchor,
      startX: event.clientX,
      startY: event.clientY,
      originWidth: rect.width,
      originHeight: rect.height,
      originX: rect.left - flowRect.left + (flow.scrollLeft || 0),
      originY: rect.top - flowRect.top + (flow.scrollTop || 0),
      canvasPadLeft: pad.left,
      canvasPadRight: pad.right,
      canvasPadTop: pad.top,
      widthMode: block.props?.style?.widthMode || 'full',
      canvasWidth: Math.max(240, flowRect.width),
      shellNode: node,
      resizeRaf: 0,
      pendingEvent: null,
      collisionTick: 0,
      lastApplied: null,
    }
    resizeCollisionBlockIds.value = []
    window.addEventListener('pointermove', onPageBlockResize)
    window.addEventListener('pointerup', endPageBlockResize)
    window.addEventListener('pointercancel', endPageBlockResize)
  }
  __impl.startPageBlockResize = startPageBlockResize

  function onPageBlockResize(event) {
    const ctx = mut.pageBlockResizeCtx
    if (!ctx)
      return
    ctx.pendingEvent = event
    if (ctx.resizeRaf)
      return
    ctx.resizeRaf = window.requestAnimationFrame(() => {
      ctx.resizeRaf = 0
      const pending = ctx.pendingEvent
      ctx.pendingEvent = null
      if (pending && mut.pageBlockResizeCtx === ctx)
        applyPageBlockResizeFrame(pending)
    })
  }
  __impl.onPageBlockResize = onPageBlockResize

  function applyPageBlockResizeFrame(event) {
    if (!mut.pageBlockResizeCtx)
      return
    const ctx = mut.pageBlockResizeCtx
    const widthDelta = event.clientX - ctx.startX
    const heightDelta = event.clientY - ctx.startY
    const anchor = ctx.anchor || 'bottom-right'
    let width = ctx.originWidth
    let height = ctx.originHeight
    let canvasX = ctx.originX
    let canvasY = ctx.originY
    const resizingWidth = anchor.includes('left') || anchor.includes('right')
    if (anchor.includes('right'))
      width = ctx.originWidth + widthDelta
    if (anchor.includes('left')) {
      width = ctx.originWidth - widthDelta
      canvasX = ctx.originX + widthDelta
    }
    if (anchor.includes('bottom'))
      height = ctx.originHeight + heightDelta
    if (anchor.includes('top')) {
      height = ctx.originHeight - heightDelta
      canvasY = ctx.originY + heightDelta
    }
    const padLeft = Number(ctx.canvasPadLeft) || 24
    const padRight = Number(ctx.canvasPadRight) || 24
    const padTop = Number(ctx.canvasPadTop) || 24
    const canvasWidth = Number(ctx.canvasWidth) || 1200
    const maxRight = Math.max(padLeft + 180, canvasWidth - padRight)
    canvasX = Math.max(padLeft, canvasX)
    canvasY = Math.max(padTop, canvasY)
    let nextW = Math.min(Math.max(180, width), Math.max(180, maxRight - canvasX))
    if (anchor.includes('left') && nextW < width)
      canvasX = Math.max(padLeft, canvasX + (width - nextW))
    nextW = Math.round(Math.min(Math.max(180, nextW), Math.max(180, maxRight - canvasX)))
    let pageFlowHeight = Math.round(Math.max(40, height))
    // 缩放边缘吸附其它根级块，并显示对齐标线
    const peerRects = pageBlocks.value
      .filter(item => item?.id && item.id !== ctx.blockId)
      .map((item) => {
        const style = resolvePageBlockShellStyle(item)
        return {
          x: readPageBlockLength(style.left, 0),
          y: readPageBlockLength(style.top, 0),
          width: readPageBlockLength(item.props?.style?.pageFlowWidth, style.width)
            || readPageBlockLength(style.width, nextW),
          height: readPageBlockLength(item.props?.style?.pageFlowHeight, style.height)
            || readPageBlockLength(style.height, pageFlowHeight),
        }
      })
    const snapped = snapResizeRectToPeers(
      { x: canvasX, y: canvasY, width: nextW, height: pageFlowHeight },
      peerRects,
      anchor,
      PAGE_ALIGN_SNAP_PX,
    )
    canvasX = Math.max(padLeft, snapped.x)
    canvasY = Math.max(padTop, snapped.y)
    nextW = Math.round(Math.min(Math.max(180, snapped.width), Math.max(180, maxRight - canvasX)))
    pageFlowHeight = Math.round(Math.max(40, snapped.height))
    const keepFullWidth = !resizingWidth && (ctx.widthMode === 'full' || ctx.widthMode === 'auto')
    const guideWidth = keepFullWidth
      ? Math.max(180, canvasWidth - padLeft - padRight)
      : nextW
    // 标线与即将写入的壳尺寸同一套数值，避免先跳线后变宽
    pageAlignGuides.value = {
      visible: true,
      edges: {
        left: Math.round(keepFullWidth ? padLeft : canvasX),
        right: Math.round((keepFullWidth ? padLeft : canvasX) + guideWidth),
        top: Math.round(canvasY),
        bottom: Math.round(canvasY + pageFlowHeight),
      },
      x: snapped.lines?.x || [],
      y: snapped.lines?.y || [],
    }
    // 先写 DOM，标线与壳同帧对齐；再写 schema
    const shellNode = ctx.shellNode
    if (shellNode?.style) {
      if (!keepFullWidth) {
        shellNode.style.width = `${nextW}px`
        shellNode.style.maxWidth = `${nextW}px`
      }
      shellNode.style.height = `${pageFlowHeight}px`
      shellNode.style.minHeight = `${pageFlowHeight}px`
      if (!keepFullWidth)
        shellNode.style.left = `${Math.round(canvasX)}px`
      shellNode.style.top = `${Math.round(canvasY)}px`
    }
    const content = canvasToContentFlowPoint(canvasX, canvasY, {
      left: padLeft,
      right: padRight,
      top: padTop,
      bottom: 0,
    })
    ctx.collisionTick = (ctx.collisionTick || 0) + 1
    if (ctx.collisionTick % 2 === 1) {
      updateResizeCollisionHighlights(ctx.blockId, {
        x: keepFullWidth ? padLeft : canvasX,
        y: canvasY,
        width: guideWidth,
        height: pageFlowHeight,
      })
    }
    const appliedKey = `${canvasX},${canvasY},${nextW},${pageFlowHeight},${keepFullWidth ? 1 : 0}`
    if (ctx.lastApplied === appliedKey)
      return
    ctx.lastApplied = appliedKey
    updatePageBlocks(pageBlocks.value.map((item) => {
      if (item.id !== ctx.blockId)
        return item
      const prev = item.props?.style || {}
      const nextStyle = {
        ...prev,
        heightMode: 'fixed',
        height: pageFlowHeight,
        pageFlowHeight,
        minHeight: pageFlowHeight,
        pageFlowY: content.y,
      }
      if (keepFullWidth) {
        nextStyle.widthMode = prev.widthMode === 'auto' ? 'auto' : 'full'
        nextStyle.width = nextStyle.widthMode === 'auto' ? 'auto' : '100%'
        delete nextStyle.pageFlowWidth
        nextStyle.pageFlowX = 0
      }
      else {
        nextStyle.widthMode = 'fixed'
        nextStyle.width = nextW
        nextStyle.pageFlowWidth = `${nextW}px`
        // 仅左右拖且未横向挪位时保持 pageFlowX=0，文档流堆叠下定宽仍生效
        nextStyle.pageFlowX = resizingWidth && Math.abs(content.x) < 1 ? 0 : content.x
      }
      // 拖拽过程不写 props.height，避免图表每帧重绘；松手再同步
      return {
        ...item,
        props: {
          ...(item.props || {}),
          style: nextStyle,
        },
      }
    }))
  }
  __impl.applyPageBlockResizeFrame = applyPageBlockResizeFrame

  /** 用画布 DOM 实测矩形做碰撞高亮，避免 full 宽 geo=100000 误判一直黄框 */
  function readPageBlockCanvasRect(blockId) {
    const flow = document.querySelector('.application-page-flow')
    const node = document.querySelector(`[data-page-block-id="${blockId}"]`)
    const flowRect = flow?.getBoundingClientRect?.()
    const rect = node?.getBoundingClientRect?.()
    if (!flowRect || !rect)
      return null
    const x = Math.round(rect.left - flowRect.left + (flow.scrollLeft || 0))
    const y = Math.round(rect.top - flowRect.top + (flow.scrollTop || 0))
    return {
      x,
      y,
      width: Math.round(rect.width),
      height: Math.round(rect.height),
      right: x + Math.round(rect.width),
      bottom: y + Math.round(rect.height),
    }
  }
  __impl.readPageBlockCanvasRect = readPageBlockCanvasRect

  function updateResizeCollisionHighlights(blockId, rect = {}) {
    const gap = 8
    const moving = {
      x: Number(rect.x) || 0,
      y: Number(rect.y) || 0,
      width: Number(rect.width) || 0,
      height: Number(rect.height) || 0,
    }
    moving.right = moving.x + moving.width
    moving.bottom = moving.y + moving.height
    const hits = []
    pageBlocks.value.forEach((item) => {
      if (!item?.id || item.id === blockId)
        return
      const peer = readPageBlockCanvasRect(item.id)
      if (!peer)
        return
      const overlaps = moving.x < peer.right + gap
        && moving.right + gap > peer.x
        && moving.y < peer.bottom + gap
        && moving.bottom + gap > peer.y
      if (overlaps)
        hits.push(item.id)
    })
    resizeCollisionBlockIds.value = hits
  }
  __impl.updateResizeCollisionHighlights = updateResizeCollisionHighlights

  /** 拖拽预览避开其它根级块：重叠则下推到对方下方 */
  function avoidRootOverlapCanvasPosition(blockId, x, y, width, height, gap = 16) {
    let nextY = Number(y) || 0
    const nextX = Number(x) || 0
    const w = Number(width) || 0
    const h = Number(height) || 0
    let guard = 0
    while (guard++ < 48) {
      let hit = null
      pageBlocks.value.forEach((item) => {
        if (!item?.id || item.id === blockId)
          return
        const peer = readPageBlockCanvasRect(item.id)
        if (!peer)
          return
        const candidate = {
          x: nextX,
          y: nextY,
          width: w,
          height: h,
          right: nextX + w,
          bottom: nextY + h,
        }
        const overlaps = candidate.x < peer.right + gap
          && candidate.right + gap > peer.x
          && candidate.y < peer.bottom + gap
          && candidate.bottom + gap > peer.y
        if (!overlaps)
          return
        if (!hit || peer.bottom > hit.bottom)
          hit = peer
      })
      if (!hit)
        break
      nextY = hit.bottom + gap
    }
    return { x: nextX, y: nextY }
  }
  __impl.avoidRootOverlapCanvasPosition = avoidRootOverlapCanvasPosition

  function endPageBlockResize() {
    const ctx = mut.pageBlockResizeCtx
    const resizedBlockId = ctx?.blockId || ''
    const resizedBlockType = ctx?.blockType || ''
    if (ctx?.resizeRaf) {
      window.cancelAnimationFrame(ctx.resizeRaf)
      ctx.resizeRaf = 0
    }
    // 冲掉最后一帧 pending，保证松手尺寸落盘
    if (ctx?.pendingEvent)
      applyPageBlockResizeFrame(ctx.pendingEvent)
    mut.pageBlockResizeCtx = null
    resizeCollisionBlockIds.value = []
    clearPageAlignGuides()
    window.removeEventListener('pointermove', onPageBlockResize)
    window.removeEventListener('pointerup', endPageBlockResize)
    window.removeEventListener('pointercancel', endPageBlockResize)
    if (!resizedBlockId)
      return
    // 图表：松手一次同步 props.height，壳与画布对齐
    let nextItems = pageBlocks.value
    if (PAGE_CHART_BLOCK_TYPES.has(resizedBlockType)) {
      nextItems = pageBlocks.value.map((item) => {
        if (item.id !== resizedBlockId)
          return item
        const shellH = Number(item.props?.style?.pageFlowHeight) || Number(item.props?.style?.height) || 0
        if (!(shellH >= 160))
          return item
        return {
          ...item,
          props: {
            ...(item.props || {}),
            height: Math.max(160, Math.round(shellH)),
          },
        }
      })
    }
    updatePageBlocks(nextItems, { resolveCollisions: true, changedBlockId: resizedBlockId })
  }
  __impl.endPageBlockResize = endPageBlockResize

  function startPageBlockMove(block, event) {
    if (event.button !== 0)
      return
    const node = event.currentTarget.closest('[data-page-block-id]')
    const flow = node?.parentElement
    const rect = node?.getBoundingClientRect?.()
    const flowRect = flow?.getBoundingClientRect?.()
    if (!rect || !flowRect)
      return
    event.preventDefault()
    selectPageBlock(block.id)
    if (!pageFlowStackMode.value)
      updateCurrentGridLayout(ensurePageFlowContentCoords({ ...(currentGridLayout.value || {}) }))
    const pad = pageCanvasPadding.value
    const originX = Math.round(rect.left - flowRect.left + (flow.scrollLeft || 0))
    const originY = Math.round(rect.top - flowRect.top + (flow.scrollTop || 0))
    mut.pageBlockMoveCtx = {
      blockId: block.id,
      startX: event.clientX,
      startY: event.clientY,
      originX,
      originY,
      originClientLeft: rect.left,
      originClientTop: rect.top,
      width: rect.width,
      height: rect.height,
      activeSwapTargetId: '',
      blockSlots: new Map(pageBlocks.value.map((item) => {
        const style = resolvePageBlockShellStyle(item)
        const contentX = readPageBlockLength(item.props?.style?.pageFlowX, 0)
        const contentY = readPageBlockLength(item.props?.style?.pageFlowY, 0)
        const canvas = contentToCanvasFlowPoint(contentX, contentY, pad)
        return [item.id, {
          x: canvas.x || readPageBlockLength(style.left, pad.left),
          y: canvas.y || readPageBlockLength(style.top, pad.top),
          contentX,
          contentY,
        }]
      })),
      canvasPad: pad.left,
      canvasPadTop: pad.top,
      maxX: Math.max(pad.left, flowRect.width - rect.width - pad.right),
      maxY: Math.max(pad.top, flowRect.height - rect.height),
    }
    dragPreview.value = { blockId: block.id, x: originX, y: originY, width: rect.width, height: rect.height }
    draggingPageBlockId.value = block.id
    resizeCollisionBlockIds.value = []
    window.addEventListener('pointermove', onPageBlockMove)
    window.addEventListener('pointerup', endPageBlockMove)
    window.addEventListener('pointercancel', endPageBlockMove)
  }
  __impl.startPageBlockMove = startPageBlockMove

  function onPageBlockMove(event) {
    mut.pendingPageBlockMoveEvent = event
    if (mut.pageBlockMoveFrame)
      return
    mut.pageBlockMoveFrame = window.requestAnimationFrame(() => {
      mut.pageBlockMoveFrame = 0
      const nextEvent = mut.pendingPageBlockMoveEvent
      mut.pendingPageBlockMoveEvent = null
      if (nextEvent)
        applyPageBlockMove(nextEvent)
    })
  }
  __impl.onPageBlockMove = onPageBlockMove

  function applyPageBlockMove(event) {
    if (!mut.pageBlockMoveCtx)
      return
    const ctx = mut.pageBlockMoveCtx
    const padLeft = Number(ctx.canvasPad) || 24
    const padTop = Number(ctx.canvasPadTop) || 24
    let pageFlowX = Math.round(Math.max(padLeft, Math.min(ctx.maxX, ctx.originX + event.clientX - ctx.startX)))
    let pageFlowY = Math.round(Math.max(padTop, ctx.originY + event.clientY - ctx.startY))
    const snapped = snapPageBlockPosition(ctx, pageFlowX, pageFlowY)
    pageFlowX = Math.round(Math.max(padLeft, Math.min(ctx.maxX, snapped.x)))
    pageFlowY = Math.max(padTop, snapped.y)
    // 防止吸附后右边缘越界
    const maxRight = (ctx.maxX || 0) + Number(ctx.width || 0)
    if (pageFlowX + Number(ctx.width || 0) > maxRight)
      pageFlowX = Math.max(padLeft, maxRight - Number(ctx.width || 0))
    // 根级禁止叠放：预览位置若压住其它块则下推（拖入容器另走 tab/grid）
    const avoided = avoidRootOverlapCanvasPosition(
      ctx.blockId,
      pageFlowX,
      pageFlowY,
      Number(ctx.width) || 0,
      Number(ctx.height) || 0,
      16,
    )
    pageFlowX = avoided.x
    pageFlowY = Math.max(padTop, avoided.y)
    dragPreview.value = { ...dragPreview.value, x: pageFlowX, y: pageFlowY }
    updatePageAlignGuides(ctx, pageFlowX, pageFlowY)
    // 仍重叠时标橙（理论上避让后应为空）
    updateResizeCollisionHighlights(ctx.blockId, {
      x: pageFlowX,
      y: pageFlowY,
      width: Number(ctx.width) || 0,
      height: Number(ctx.height) || 0,
    })
    ctx.activeTabTarget = resolvePageFlowTabTargetFromPoint(event)
    ctx.activeGridTarget = resolvePageFlowGridCellTargetFromPoint(event)
    // 自由拖放：拖动中不做实时对调，避免其它元素跟着弹跳
    if (ctx.activeSwapTargetId)
      clearPageBlockSwapPreview(ctx)
  }
  __impl.applyPageBlockMove = applyPageBlockMove

  function snapPageBlockPosition(ctx, x, y) {
    const width = Number(ctx.width || 0)
    const height = Number(ctx.height || 0)
    const moving = {
      left: x,
      top: y,
      right: x + width,
      bottom: y + height,
      cx: x + width / 2,
      cy: y + height / 2,
    }
    let nextX = x
    let nextY = y
    let bestX = null
    let bestY = null
    pageBlocks.value.forEach((item) => {
      if (!item?.id || item.id === ctx.blockId)
        return
      const style = resolvePageBlockShellStyle(item)
      const left = readPageBlockLength(style.left, 0)
      const top = readPageBlockLength(style.top, 0)
      const w = readPageBlockLength(item.props?.style?.pageFlowWidth, style.width) || width
      const h = readPageBlockLength(item.props?.style?.pageFlowHeight, style.height) || height
      const target = {
        left,
        top,
        right: left + w,
        bottom: top + h,
        cx: left + w / 2,
        cy: top + h / 2,
      }
      ;[
        [moving.left, target.left],
        [moving.left, target.right],
        [moving.left, target.cx],
        [moving.cx, target.left],
        [moving.cx, target.right],
        [moving.cx, target.cx],
        [moving.right, target.left],
        [moving.right, target.right],
        [moving.right, target.cx],
      ].forEach(([from, to]) => {
        const delta = Math.abs(from - to)
        if (delta > PAGE_ALIGN_SNAP_PX)
          return
        if (bestX && delta >= bestX.delta)
          return
        bestX = { delta, to: Math.round(to), nextX: Math.round(x + (to - from)) }
      })
      ;[
        [moving.top, target.top],
        [moving.top, target.bottom],
        [moving.top, target.cy],
        [moving.cy, target.top],
        [moving.cy, target.bottom],
        [moving.cy, target.cy],
        [moving.bottom, target.top],
        [moving.bottom, target.bottom],
        [moving.bottom, target.cy],
      ].forEach(([from, to]) => {
        const delta = Math.abs(from - to)
        if (delta > PAGE_ALIGN_SNAP_PX)
          return
        if (bestY && delta >= bestY.delta)
          return
        bestY = { delta, to: Math.round(to), nextY: Math.round(y + (to - from)) }
      })
    })
    if (bestX)
      nextX = bestX.nextX
    if (bestY)
      nextY = bestY.nextY
    // 每个轴最多一条吸附线，避免标线刷屏
    ctx._snapLines = {
      x: bestX ? [bestX.to] : [],
      y: bestY ? [bestY.to] : [],
    }
    return { x: nextX, y: nextY }
  }
  __impl.snapPageBlockPosition = snapPageBlockPosition

  function updatePageAlignGuides(ctx, x, y) {
    const width = Number(ctx.width || 0)
    const height = Number(ctx.height || 0)
    pageAlignGuides.value = {
      visible: true,
      edges: {
        left: Math.round(x),
        right: Math.round(x + width),
        top: Math.round(y),
        bottom: Math.round(y + height),
      },
      x: ctx._snapLines?.x || [],
      y: ctx._snapLines?.y || [],
    }
  }
  __impl.updatePageAlignGuides = updatePageAlignGuides

  function clearPageAlignGuides() {
    pageAlignGuides.value = { visible: false, edges: null, x: [], y: [] }
  }
  __impl.clearPageAlignGuides = clearPageAlignGuides

  function endPageBlockMove(event) {
    if (mut.pageBlockMoveFrame) {
      window.cancelAnimationFrame(mut.pageBlockMoveFrame)
      mut.pageBlockMoveFrame = 0
      mut.pendingPageBlockMoveEvent = null
    }
    const ctx = mut.pageBlockMoveCtx
    if (ctx) {
      const tabTarget = ctx.activeTabTarget
        || (event ? resolvePageFlowTabTargetFromPoint(event) : null)
      if (tabTarget && moveRootPageBlockToTab(ctx.blockId, tabTarget.blockId, tabTarget.tabKey)) {
        mut.pageBlockMoveCtx = null
        draggingPageBlockId.value = ''
        dragPreview.value = null
        clearPageAlignGuides()
        window.removeEventListener('pointermove', onPageBlockMove)
        window.removeEventListener('pointerup', endPageBlockMove)
        return
      }
      const gridTarget = ctx.activeGridTarget
        || (event ? resolvePageFlowGridCellTargetFromPoint(event) : null)
      if (gridTarget && moveRootPageBlockToGridCell(ctx.blockId, gridTarget.blockId, gridTarget.cellKey)) {
        mut.pageBlockMoveCtx = null
        draggingPageBlockId.value = ''
        dragPreview.value = null
        clearPageAlignGuides()
        window.removeEventListener('pointermove', onPageBlockMove)
        window.removeEventListener('pointerup', endPageBlockMove)
        return
      }
      if (ctx.activeSwapTargetId)
        clearPageBlockSwapPreview(ctx)
      const finalX = Math.round(dragPreview.value?.x ?? ctx.originX)
      const finalY = Math.round(dragPreview.value?.y ?? ctx.originY)
      const content = canvasToContentFlowPoint(finalX, finalY, pageCanvasPadding.value)
      // 落到预览位置；根级重叠由 resolveCollisions 钉住主动块、下推其它块（嵌入容器除外）
      updatePageBlocks(
        pageBlocks.value.map(item => item.id === ctx.blockId
          ? {
              ...item,
              props: {
                ...(item.props || {}),
                style: {
                  ...(item.props?.style || {}),
                  pageFlowX: content.x,
                  pageFlowY: content.y,
                },
              },
            }
          : item),
        { resolveCollisions: true, changedBlockId: ctx.blockId },
      )
    }
    mut.pageBlockMoveCtx = null
    draggingPageBlockId.value = ''
    dragPreview.value = null
    resizeCollisionBlockIds.value = []
    clearPageAlignGuides()
    window.removeEventListener('pointermove', onPageBlockMove)
    window.removeEventListener('pointerup', endPageBlockMove)
    window.removeEventListener('pointercancel', endPageBlockMove)
  }
  __impl.endPageBlockMove = endPageBlockMove

  function applyPageBlockSwapPreview(ctx, targetId) {
    const previousTargetId = ctx.activeSwapTargetId
    const targetNode = document.querySelector(`[data-page-block-id="${targetId}"]`)
    const previousTargetNode = previousTargetId ? document.querySelector(`[data-page-block-id="${previousTargetId}"]`) : null
    const targetRect = targetNode?.getBoundingClientRect?.()
    const previousTargetRect = previousTargetNode?.getBoundingClientRect?.()
    const originSlot = ctx.blockSlots.get(ctx.blockId)
    const previousTargetSlot = previousTargetId ? ctx.blockSlots.get(previousTargetId) : null
    if (!originSlot)
      return
    updatePageBlocks(pageBlocks.value.map((item) => {
      if (item.id === previousTargetId && previousTargetSlot) {
        return {
          ...item,
          props: {
            ...(item.props || {}),
            style: {
              ...(item.props?.style || {}),
              pageFlowX: previousTargetSlot.contentX ?? previousTargetSlot.x,
              pageFlowY: previousTargetSlot.contentY ?? previousTargetSlot.y,
            },
          },
        }
      }
      if (item.id === targetId) {
        return {
          ...item,
          props: {
            ...(item.props || {}),
            style: {
              ...(item.props?.style || {}),
              pageFlowX: originSlot.contentX ?? originSlot.x,
              pageFlowY: originSlot.contentY ?? originSlot.y,
            },
          },
        }
      }
      return item
    }))
    if (previousTargetId && previousTargetRect)
      animatePageBlockSwap(previousTargetId, previousTargetRect)
    if (targetRect)
      animatePageBlockSwap(targetId, targetRect)
    ctx.activeSwapTargetId = targetId
  }
  __impl.applyPageBlockSwapPreview = applyPageBlockSwapPreview

  function clearPageBlockSwapPreview(ctx) {
    if (!ctx.activeSwapTargetId)
      return
    const targetId = ctx.activeSwapTargetId
    const targetNode = document.querySelector(`[data-page-block-id="${targetId}"]`)
    const targetRect = targetNode?.getBoundingClientRect?.()
    const targetSlot = ctx.blockSlots.get(targetId)
    if (targetSlot) {
      updatePageBlocks(pageBlocks.value.map(item => item.id === targetId
        ? {
            ...item,
            props: {
              ...(item.props || {}),
              style: {
                ...(item.props?.style || {}),
                pageFlowX: targetSlot.contentX ?? targetSlot.x,
                pageFlowY: targetSlot.contentY ?? targetSlot.y,
              },
            },
          }
        : item))
    }
    if (targetRect)
      animatePageBlockSwap(targetId, targetRect)
    ctx.activeSwapTargetId = ''
  }
  __impl.clearPageBlockSwapPreview = clearPageBlockSwapPreview

  function animatePageBlockSwap(blockId, previousRect) {
    window.requestAnimationFrame(() => {
      const node = document.querySelector(`[data-page-block-id="${blockId}"]`)
      const nextRect = node?.getBoundingClientRect?.()
      if (!node || !nextRect)
        return
      const deltaX = previousRect.left - nextRect.left
      const deltaY = previousRect.top - nextRect.top
      if (Math.abs(deltaX) < 1 && Math.abs(deltaY) < 1)
        return
      node.animate([
        { transform: `translate(${deltaX}px, ${deltaY}px)` },
        { transform: 'translate(0, 0)' },
      ], {
        duration: 280,
        easing: 'cubic-bezier(0.22, 0.8, 0.24, 1)',
        fill: 'both',
      })
    })
  }
  __impl.animatePageBlockSwap = animatePageBlockSwap

  function moveRootPageBlockToTab(blockId, containerId, tabKey) {
    const source = pageBlocks.value.find(item => item.id === blockId)
    const container = findPageBlockInTree(pageBlocks.value, containerId)
    if (!source || container?.blockType !== 'tabs' || source.id === container.id)
      return false
    const tabs = Array.isArray(container.props?.tabs) ? container.props.tabs : []
    if (!tabs.some(tab => tab.key === tabKey))
      return false
    const nested = normalizePageBlockForContainer(source, 'tabs')
    const withoutSource = pageBlocks.value.filter(item => item.id !== blockId)
    updatePageBlocks(mapPageBlocksInTree(withoutSource, item => item.id === containerId
      ? {
          ...item,
          props: {
            ...(item.props || {}),
            tabs: item.props.tabs.map(tab => tab.key === tabKey
              ? { ...tab, children: [...(tab.children || []), nested] }
              : tab),
          },
        }
      : item))
    selectedPageBlockId.value = containerId
    return true
  }
  __impl.moveRootPageBlockToTab = moveRootPageBlockToTab

  function moveRootPageBlockToGridCell(blockId, containerId, cellKey) {
    const source = pageBlocks.value.find(item => item.id === blockId)
    const container = findPageBlockInTree(pageBlocks.value, containerId)
    if (!source || !container || source.id === container.id)
      return false
    if (['card', 'panel-box', 'box-layout'].includes(container.blockType)) {
      // 卡片/盒子允许放入栅格；标签页及其它布局容器仍放根级
      if (['tabs', 'box-layout', 'card', 'panel-box'].includes(source.blockType)) {
        message.warning('卡片/盒子内请放入普通组件或栅格，其它布局容器请放在画布根级')
        return false
      }
      const nested = normalizePageBlockForContainer(source, container.blockType)
      const withoutSource = pageBlocks.value.filter(item => item.id !== blockId)
      updatePageBlocks(mapPageBlocksInTree(withoutSource, (item) => {
        if (item.id !== containerId)
          return item
        return __impl.expandContainerShellForNestedChild(
          { ...item, children: [...(item.children || []), nested] },
          nested,
        )
      }))
      selectedPageBlockId.value = nested.id
      message.success(container.blockType === 'card' ? '组件已放入卡片' : '组件已放入盒子')
      return true
    }
    if (container.blockType !== 'grid-layout')
      return false
    if (['grid-layout', 'tabs'].includes(source.blockType)) {
      message.warning('栅格格子内请放入普通组件，勿再嵌套栅格或标签页')
      return false
    }
    const cells = Array.isArray(container.props?.cells) ? container.props.cells : []
    let targetKey = String(cellKey || '')
    if (!cells.some(cell => String(cell.key) === targetKey))
      targetKey = String(cells[0]?.key || '')
    if (!targetKey)
      return false
    const nested = normalizePageBlockForContainer(source, 'grid-layout')
    const withoutSource = pageBlocks.value.filter(item => item.id !== blockId)
    updatePageBlocks(mapPageBlocksInTree(withoutSource, (item) => {
      if (item.id !== containerId)
        return item
      const next = {
        ...item,
        props: {
          ...(item.props || {}),
          cells: (item.props?.cells || []).map(cell => String(cell.key) === targetKey
            ? { ...cell, children: [...(cell.children || []), nested] }
            : cell),
        },
      }
      return __impl.expandContainerShellForNestedChild(next, nested)
    }))
    selectedPageBlockId.value = nested.id
    message.success('组件已放入栅格')
    return true
  }
  __impl.moveRootPageBlockToGridCell = moveRootPageBlockToGridCell

  function resolvePageBlockSwapTarget(blockId, movingRectOverride) {
    const movingNode = document.querySelector(`[data-page-block-id="${blockId}"]`)
    const movingRect = movingRectOverride || movingNode?.getBoundingClientRect?.()
    if (!movingRect)
      return ''
    let matchedId = ''
    let maxArea = 0
    document.querySelectorAll('[data-page-block-id]').forEach((node) => {
      const targetId = node.dataset.pageBlockId
      if (!targetId || targetId === blockId)
        return
      const rect = node.getBoundingClientRect()
      const overlapWidth = Math.max(0, Math.min(movingRect.right, rect.right) - Math.max(movingRect.left, rect.left))
      const overlapHeight = Math.max(0, Math.min(movingRect.bottom, rect.bottom) - Math.max(movingRect.top, rect.top))
      const area = overlapWidth * overlapHeight
      if (area > maxArea) {
        maxArea = area
        matchedId = targetId
      }
    })
    return maxArea >= 900 ? matchedId : ''
  }
  __impl.resolvePageBlockSwapTarget = resolvePageBlockSwapTarget

  function startComponentButtonMove(event) {
    if (event.button !== 0)
      return
    // 双击恢复默认 sticky，避免长页后卡在顶部拖不下来
    if (event.detail >= 2) {
      componentButtonPosition.value = { x: null, y: null }
      componentButtonMoveCtx.value = null
      return
    }
    const button = event.currentTarget
    const host = button.closest('.page-surface')
    const buttonRect = button.getBoundingClientRect()
    const hostRect = host?.getBoundingClientRect()
    if (!hostRect)
      return
    // sticky 默认态：用内容区坐标作拖拽原点（含 scrollTop）
    const scrollTop = host?.scrollTop || 0
    const scrollLeft = host?.scrollLeft || 0
    componentButtonMoveCtx.value = {
      host,
      button,
      startX: event.clientX,
      startY: event.clientY,
      originTop: buttonRect.top - hostRect.top + scrollTop,
      originLeft: buttonRect.left - hostRect.left + scrollLeft,
    }
    window.addEventListener('pointermove', onComponentButtonMove)
    window.addEventListener('pointerup', endComponentButtonMove)
  }
  __impl.startComponentButtonMove = startComponentButtonMove

  function onComponentButtonMove(event) {
    if (!componentButtonMoveCtx.value)
      return
    const nextTop = componentButtonMoveCtx.value.originTop + event.clientY - componentButtonMoveCtx.value.startY
    const nextLeft = componentButtonMoveCtx.value.originLeft + event.clientX - componentButtonMoveCtx.value.startX
    const host = componentButtonMoveCtx.value.host
    const hostRect = host?.getBoundingClientRect()
    const buttonRect = componentButtonMoveCtx.value.button?.getBoundingClientRect()
    // 用 scrollHeight，避免长页时 maxY 只等于可视高度，拖不到下方
    const hostScrollH = Math.max(host?.scrollHeight || 0, hostRect?.height || 0, pageFlowHeight.value || 0)
    const maxX = Math.max(12, (hostRect?.width || 0) - (buttonRect?.width || 44) - 12)
    const maxY = Math.max(12, hostScrollH - (buttonRect?.height || 44) - 12)
    componentButtonPosition.value = {
      x: Math.round(Math.max(12, Math.min(maxX, nextLeft))),
      y: Math.round(Math.max(12, Math.min(maxY, nextTop))),
    }
  }
  __impl.onComponentButtonMove = onComponentButtonMove

  function endComponentButtonMove() {
    componentButtonMoveCtx.value = null
    window.removeEventListener('pointermove', onComponentButtonMove)
    window.removeEventListener('pointerup', endComponentButtonMove)
  }
  __impl.endComponentButtonMove = endComponentButtonMove

  function createLegacyBlock(item, index) {
    const block = createGridBlock(legacyBlockTypeMap[item.componentKey] || 'info-panel', { fields: [] }, { gridX: index % 2 ? 6 : 0, gridY: Math.floor(index / 2) * 4 })
    if (!block)
      return null
    return { ...block, label: item.props?.title || item.label || block.label, props: { ...block.props, title: item.props?.title || item.label || block.label, subtitle: item.props?.description || item.props?.subtitle || item.props?.content || '' } }
  }
  __impl.createLegacyBlock = createLegacyBlock

  async function saveActiveFormDesigner(returnAfter = true, options = {}) {
    const forPreview = options.forPreview === true
    const context = activePageShapeDesign.value
    if (!context)
      return saveDraft()
    if (!application.value || !activeFormAsset.value || saving.value)
      return false
    const objectName = String(context.objectName || '').trim()
    const objectCode = String(context.objectCode || '').trim()
    if (!objectName) {
      message.warning('请填写数据表名称')
      return false
    }
    if (!/^[a-z]\w{1,47}$/i.test(objectCode)) {
      message.warning('数据表编码需以字母开头，仅含字母、数字和下划线（2-48 位）')
      return false
    }
    const designer = buildBusinessObjectDesignerPayloadFromFormAsset(
      activeFormAsset.value,
      // activeFormFields 已剔除子表合成列；再滤系统字段后写主对象目录
      activeFormFields.value.filter(field => field?.systemField !== true),
    )
    if (!designer.fields.length) {
      message.warning('请先从左侧添加至少一个字段组件')
      return false
    }
    saving.value = true
    try {
      syncActivePageShapeObject()
      const response = await designBusinessApplicationPage(application.value.id, {
        pageId: context.pageId,
        pageType: context.pageType,
        formAssetId: context.formAssetId,
        objectId: context.objectId || null,
        objectCode,
        objectName,
        runtimeDatasourceId: context.runtimeDatasourceId || null,
        createMode: context.createMode || '',
        importDatasourceId: context.importDatasourceId || context.runtimeDatasourceId || null,
        importTableName: context.importTableName || '',
        fields: designer.fields,
        formDesignerSchema: designer.formDesignerSchema,
        builder: builder.value,
      })
      const saved = response.data || {}
      if (saved.builder)
        builder.value = saved.builder
      resetBuilderHistory(builder.value)
      const pageId = saved.pageId || context.pageId
      if (activePageShapeDesign.value) {
        activePageShapeDesign.value = {
          ...activePageShapeDesign.value,
          pageId,
          objectId: saved.objectId || activePageShapeDesign.value.objectId || null,
          objectCode: saved.objectCode || objectCode,
          objectName: saved.objectName || objectName,
          createMode: '',
        }
      }
      embeddedDesignerDirty.value = false
      if (forPreview) {
        // 预览落盘：不拉 workspace，只刷门户 CRUD，设计器保活不重挂
        bumpPortalCrudConfigAfterDesignerSave()
        return true
      }
      await refreshWorkspaceMetadata({ syncBuilder: true, markClean: true })
      // refreshWorkspaceMetadata 已 reset + bump portalCrudConfigRevision；
      // 编辑画布若仍打开再补一次预加载（返回页面管理时 preload 会因 !editing 直接跳过）
      preloadCurrentPageCrudRuntimeProps()
      // 设计器可能在保存回写后继续派生 pageSections，再对齐一次避免假脏
      if (dirty.value)
        await markBuilderClean()
      if (returnAfter) {
        formDesignerMode.value = false
        if (formDesignerFromPageManagement.value) {
          // 从页面管理视图进入的，保存后返回页面管理视图
          selectPageManagementNode(pageId)
          exitToPageManagement()
        }
        else {
          selectCreatedDesignerPage(pageId)
          syncActiveFormAssetForPage(pageId)
        }
      }
      message.success('页面、数据对象和字段已保存')
      return true
    }
    catch (error) {
      message.error(error?.message || '页面设计保存失败，请稍后重试')
      return false
    }
    finally {
      saving.value = false
    }
  }
  __impl.saveActiveFormDesigner = saveActiveFormDesigner

  /**
   * 导航树操作（删除/排序/隐藏/系统菜单挂载等）后静默自动保存到草稿。
   * 不弹成功提示，失败时提示用户手动保存。
   * 防抖 300ms，避免连续操作多次调接口。
   */
  function scheduleNavigationSave() {
    if (mut.navigationSaveTimer)
      clearTimeout(mut.navigationSaveTimer)
    mut.navigationSaveTimer = setTimeout(() => {
      mut.navigationSaveTimer = null
      void saveNavigationDraft()
    }, 300)
  }
  __impl.scheduleNavigationSave = scheduleNavigationSave

  async function saveNavigationDraft() {
    if (!application.value || !dirty.value)
      return
    if (saving.value) {
      mut.navigationSavePending = true
      return
    }
    saving.value = true
    try {
      await persistApplicationDraft()
      savedSignature.value = JSON.stringify(builder.value)
    }
    catch {
      message.error('导航配置保存失败，请点击"保存草稿"手动保存')
    }
    finally {
      saving.value = false
      if (mut.navigationSavePending) {
        mut.navigationSavePending = false
        void saveNavigationDraft()
      }
    }
  }
  __impl.saveNavigationDraft = saveNavigationDraft

  async function saveDraft(options = {}) {
    if (!application.value || saving.value)
      return false
    saving.value = true
    let draftPersisted = false
    try {
      await persistApplicationDraft()
      draftPersisted = true
      const provisionSummary = await provisionPendingFormData()
      if (provisionSummary.builderChanged)
        await persistApplicationDraft()
      if (provisionSummary.succeeded > 0)
        await refreshWorkspaceMetadata({ syncBuilder: true, markClean: true })
      else
        await markBuilderClean()
      // refresh / 设计器回写后可能仍有一拍归一化差异，再对齐一次
      if (dirty.value)
        await markBuilderClean()
      if (!options.quiet) {
        if (provisionSummary.failed > 0) {
          message.warning(`表单草稿已保存；${provisionSummary.firstError || '数据存储暂未准备完成，可在数据配置中重试'}`)
        }
        else if (provisionSummary.succeeded > 0) {
          message.success('表单和数据存储已准备完成')
        }
        else {
          message.success('应用草稿已保存')
        }
      }
      return true
    }
    catch (error) {
      message.error(draftPersisted
        ? '表单已保存，但数据存储与页面连接未完整保存，请重试'
        : error?.message || '草稿保存失败，请稍后重试')
      return false
    }
    finally { saving.value = false }
  }
  __impl.saveDraft = saveDraft

  async function persistApplicationDraft() {
    const options = mergeInAppBuilderOptions(application.value.options, builder.value)
    await updateBusinessApplication({
      id: application.value.id,
      applicationCode: application.value.applicationCode,
      applicationName: application.value.applicationName,
      suiteCode: application.value.suiteCode,
      icon: application.value.icon,
      description: application.value.description,
      status: application.value.status,
      options: JSON.stringify(options),
    })
    application.value.options = JSON.stringify(options)
  }
  __impl.persistApplicationDraft = persistApplicationDraft

  async function provisionPendingFormData() {
    const targets = collectFormDataProvisionTargets(builder.value, objects.value)
    const summary = { succeeded: 0, failed: 0, builderChanged: false, firstError: '' }

    // 并行发起所有表单的 provision 请求，避免多表单串行叠加
    targets.forEach((target) => {
      setFormDataProvisionState(target.formAssetId, { status: 'preparing', message: '正在准备表单数据存储' })
    })
    const results = await Promise.allSettled(
      targets.map(target =>
        provisionBusinessApplicationFormData(application.value.id, target.request)
          .then(response => ({ target, response: response.data || {} }))
          .catch(error => ({ target, error })),
      ),
    )

    // 按原始顺序应用结果，避免并发写 builder 产生冲突
    for (const result of results) {
      if (result.status === 'fulfilled' && !result.value.error) {
        const { target, response: provisioned } = result.value
        const bound = bindProvisionedFormData(builder.value, target.formAssetId, provisioned)
        if (bound.changed) {
          builder.value = bound.schema
          summary.builderChanged = true
        }
        if (provisioned.unchanged) {
          setFormDataProvisionState(target.formAssetId, { status: 'ready', message: '表单数据无变化' })
          continue
        }
        summary.succeeded += 1
        if (provisioned.ddlWarning) {
          setFormDataProvisionState(target.formAssetId, { status: 'warning', message: provisioned.ddlWarning })
          summary.ddlWarnings = (summary.ddlWarnings || 0) + 1
        }
        else {
          setFormDataProvisionState(target.formAssetId, { status: 'ready', message: '表单数据已准备完成' })
        }
      }
      else {
        const { target, error } = result.status === 'fulfilled' ? result.value : { target: null, error: result.reason }
        const errorMessage = error?.message || '数据存储准备失败，请稍后重试'
        summary.failed += 1
        summary.firstError ||= errorMessage
        if (target?.formAssetId)
          setFormDataProvisionState(target.formAssetId, { status: 'error', message: errorMessage })
      }
    }
    return summary
  }
  __impl.provisionPendingFormData = provisionPendingFormData

  function setFormDataProvisionState(formAssetId, state) {
    if (!formAssetId)
      return
    formDataProvisioningByAssetId.value = {
      ...formDataProvisioningByAssetId.value,
      [formAssetId]: state,
    }
  }
  __impl.setFormDataProvisionState = setFormDataProvisionState

  function requestExitEditing() {
    if (currentDesignerDirty()) {
      exitEditingVisible.value = true
      return
    }
    exitToPageManagement()
  }
  __impl.requestExitEditing = requestExitEditing

  function isDraftPreviewMode() {
    return isDraftMode.value && !editing.value
  }
  __impl.isDraftPreviewMode = isDraftPreviewMode

  function resolveRuntimeBackLabel() {
    if (editing.value)
      return '页面管理'
    if (isDraftPreviewMode())
      return '返回编辑'
    return '应用中心'
  }
  __impl.resolveRuntimeBackLabel = resolveRuntimeBackLabel

  function resolveRuntimeBrandEyebrow() {
    if (editing.value)
      return '页面设计'
    if (isDraftPreviewMode())
      return '草稿预览'
    if (runtimeViewMode.value === 'process')
      return '业务流程'
    if (runtimeViewMode.value === 'enhance')
      return '增强'
    if (runtimeViewMode.value === 'settings')
      return '应用设置'
    if (runtimeViewMode.value === 'publish')
      return '应用发布'
    return '页面管理'
  }
  __impl.resolveRuntimeBrandEyebrow = resolveRuntimeBrandEyebrow

  /** 顶栏加粗标题：页面管理看当前页名，其它分区看应用名 */
  function resolveRuntimeHeaderTitle() {
    if (editing.value || isDraftPreviewMode())
      return currentPageManagementTitle.value || application.value?.applicationName || '未命名应用'
    if (runtimeViewMode.value === 'pages')
      return currentPageManagementTitle.value || application.value?.applicationName || '未命名应用'
    return application.value?.applicationName || '未命名应用'
  }
  __impl.resolveRuntimeHeaderTitle = resolveRuntimeHeaderTitle

  function resolveRuntimeBrandStatus() {
    if (editing.value)
      return dirty.value ? '未保存修改' : '已保存到草稿'
    if (isDraftPreviewMode())
      return '只读预览'
    return currentPageManagementTitle.value
  }
  __impl.resolveRuntimeBrandStatus = resolveRuntimeBrandStatus

  async function openRuntimeWorkbench() {
    if (runtimeViewMode.value !== 'pages')
      await switchRuntimeView('pages')
    selectPageManagementNode(WORKBENCH_PAGE_ID)
  }
  __impl.openRuntimeWorkbench = openRuntimeWorkbench

  function handleRuntimeBack() {
    if (editing.value) {
      requestExitEditing()
      return
    }
    if (isDraftPreviewMode()) {
      returnToEditorFromDraftPreview()
      return
    }
    openWorkspace()
  }
  __impl.handleRuntimeBack = handleRuntimeBack

  function returnToEditorFromDraftPreview(options = {}) {
    const pageId = String(route.query.pageId || selectedNodeId.value || '').trim()
    // 预览若在新标签打开且未指定目标 Tab：优先关窗回到编辑页；同标签则恢复 edit=1
    const requestedTab = String(options?.designTab || '').trim()
    if (!requestedTab && window.opener && !window.opener.closed) {
      try {
        window.opener.focus?.()
      }
      catch {
        // ignore cross-origin focus errors
      }
      window.close()
      // 部分浏览器不允许脚本关窗，继续走路由回编辑
    }
    const nextQuery = { ...route.query, edit: '1' }
    delete nextQuery.draft
    // 保留 returnEdit，编辑顶栏继续显示「预览」并可一键回预览
    if (!nextQuery.returnEdit)
      nextQuery.returnEdit = '1'
    if (pageId)
      nextQuery.pageId = pageId
    if (requestedTab) {
      const defaultTab = resolveEntryDesignTab(pageId)
      if (requestedTab === defaultTab)
        delete nextQuery.designTab
      else
        nextQuery.designTab = requestedTab
      activePageDesignTab.value = resolvePageDesignTab(requestedTab, pageId)
    }
    router.replace({
      name: 'BusinessApplicationRuntime',
      params: { applicationCode: application.value?.applicationCode || route.params.applicationCode },
      query: nextQuery,
    })
  }
  __impl.returnToEditorFromDraftPreview = returnToEditorFromDraftPreview

  function exitToPageManagement(options = {}) {
    formDesignerMode.value = false
    formDesignerFromPageManagement.value = false
    activePageShapeDesign.value = null
    activeFormAssetId.value = ''
    selectedPageBlockId.value = ''
    selectedDesignerResourceKey.value = ''
    configPanelVisible.value = false
    componentPopoverVisible.value = false
    activePageDesignTab.value = 'form'
    // 返回页面管理后仍高亮刚编辑的页，方便在左侧菜单里找到它
    const keepSelection = options.keepSelection !== false
    const highlightPageId = keepSelection
      ? (selectedNodeId.value || resolveSelectablePageId(route.query.pageId))
      : ''
    if (highlightPageId)
      selectedNodeId.value = highlightPageId
    editing.value = false
    const nextQuery = { ...route.query }
    delete nextQuery.edit
    delete nextQuery.draft
    delete nextQuery.returnEdit
    delete nextQuery.designResource
    delete nextQuery.designTab
    delete nextQuery.designSection
    // 保留 pageId，左侧选中与中间预览一致；不要留 design* 设计态参数
    if (highlightPageId && !isPageManagementSystemPageId(highlightPageId))
      nextQuery.pageId = highlightPageId
    else
      delete nextQuery.pageId
    router.replace({ query: nextQuery })
  }
  __impl.exitToPageManagement = exitToPageManagement

  function currentDesignerDirty() {
    if (pageBuilderResourceActive.value)
      return dirty.value
    return dirty.value || embeddedDesignerDirty.value
  }
  __impl.currentDesignerDirty = currentDesignerDirty

  async function saveCurrentDesignerSection(options = {}) {
    const forPreview = options.forPreview === true
    // 非编辑模式下（页面管理视图等），导航树操作只修改了 builder，直接保存草稿
    if (!editing.value && canEditApplication.value && dirty.value)
      return await saveDraft()
    if (editing.value && activePageDesignTab.value === 'form') {
      const formSaved = await saveActiveFormDesigner(false, { forPreview })
      if (!formSaved)
        return false
      // 表单保存后若本地归一化仍留下差异，静默落盘，避免连弹两次“保存成功”
      if (dirty.value) {
        const draftSaved = await saveDraft({ quiet: true })
        bumpPortalCrudConfigAfterDesignerSave()
        return draftSaved
      }
      bumpPortalCrudConfigAfterDesignerSave()
      return true
    }
    if (editing.value && activePageDesignTab.value === 'list') {
      if (!pageDesignObject.value)
        return false
      embeddedDesignerSaving.value = true
      // 预览落盘：跳过 saved 回调里的 workspace 全量刷新，只刷门户 CRUD 缓存
      if (forPreview)
        suppressHeavyEmbeddedSaveSideEffects.value = true
      try {
        const saved = await embeddedDesignerRef.value?.save?.({
          reload: !forPreview,
          quiet: forPreview,
        })
        if (saved !== false)
          embeddedDesignerDirty.value = false
        if (dirty.value) {
          const draftSaved = await saveDraft({ quiet: saved !== false })
          bumpPortalCrudConfigAfterDesignerSave()
          return draftSaved
        }
        bumpPortalCrudConfigAfterDesignerSave()
        return saved !== false
      }
      finally {
        suppressHeavyEmbeddedSaveSideEffects.value = false
        embeddedDesignerSaving.value = false
      }
    }
    if (pageBuilderResourceActive.value)
      return saveDraft()
    if (designerSection.value === 'settings') {
      if (dirty.value)
        return await saveDraft()
      return true
    }
    embeddedDesignerSaving.value = true
    try {
      const saved = await embeddedDesignerRef.value?.save?.()
      if (saved !== false)
        embeddedDesignerDirty.value = false
      if (dirty.value) {
        const draftSaved = await saveDraft({ quiet: saved !== false })
        bumpPortalCrudConfigAfterDesignerSave()
        return draftSaved
      }
      bumpPortalCrudConfigAfterDesignerSave()
      return saved !== false
    }
    finally {
      embeddedDesignerSaving.value = false
    }
  }
  __impl.saveCurrentDesignerSection = saveCurrentDesignerSection

  /** 列表/表单草稿保存后立刻刷新门户 CRUD 预览，不必等到发布 */
  function bumpPortalCrudConfigAfterDesignerSave() {
    resetRuntimeCrudConfig()
    portalCrudSeed.value = {}
    portalCrudConfigRevision.value += 1
    if (editing.value || isDraftMode.value)
      preloadCurrentPageCrudRuntimeProps()
  }
  __impl.bumpPortalCrudConfigAfterDesignerSave = bumpPortalCrudConfigAfterDesignerSave

  function applyDesignerResource(resource) {
    if (!resource?.key)
      return
    selectedDesignerResourceKey.value = resource.key
    formDesignerMode.value = false
    embeddedDesignerDirty.value = false
    activeFlowContext.value = {}
    if (resource.kind === 'page-custom' && resource.pageId)
      selectNode(resource.pageId)
    router.replace({
      query: {
        ...route.query,
        designResource: resource.key,
        designSection: undefined,
        pageId: resource.kind === 'page-custom' ? resource.pageId : undefined,
        edit: '1',
      },
    })
  }
  __impl.applyDesignerResource = applyDesignerResource

  function selectDesignerResource(resource) {
    if (!resource?.key || (resource.key === activeDesignerResource.value?.key && !formDesignerMode.value))
      return
    if (!currentDesignerDirty()) {
      applyDesignerResource(resource)
      return
    }
    if (!window.$dialog) {
      message.warning('请先保存当前设计，再切换功能')
      return
    }
    window.$dialog.warning({
      title: '保存当前设计',
      content: '当前功能存在未保存修改。保存后再切换，避免丢失配置。',
      positiveText: '保存并切换',
      negativeText: '留在当前',
      onPositiveClick: async () => {
        if (await saveCurrentDesignerSection())
          applyDesignerResource(resource)
      },
    })
  }
  __impl.selectDesignerResource = selectDesignerResource

  function resolveResourceNodeBuilderNode(resource) {
    if (!resource?.pageId)
      return null
    return (builder.value?.nodes || []).find(item => String(item.id) === String(resource.pageId)) || null
  }
  __impl.resolveResourceNodeBuilderNode = resolveResourceNodeBuilderNode

  function resolveResourceNodeMenuOptions(resource) {
    const node = resolveResourceNodeBuilderNode(resource)
    return node ? resolveNavigationMoreOptions(node) : []
  }
  __impl.resolveResourceNodeMenuOptions = resolveResourceNodeMenuOptions

  function handleResourceNodeAction({ key, node } = {}) {
    const builderNode = resolveResourceNodeBuilderNode(node)
    if (!builderNode) {
      message.warning('该资源不支持结构编辑')
      return
    }
    handleNavigationMoreSelect(key, builderNode)
  }
  __impl.handleResourceNodeAction = handleResourceNodeAction

  // 扩展面板里"打开对象动作设计"已并入业务流程画布，直接落到业务流程列表。
  function openEmbeddedObjectActions() {
    const resource = designerResourceGroups.value
      .flatMap(group => group.nodes || [])
      .find(node => node.kind === 'automation-processes')
    if (resource)
      selectDesignerResource(resource)
  }
  __impl.openEmbeddedObjectActions = openEmbeddedObjectActions

  // 流程列表面板：内嵌画布，避免路由跳转导致上下文丢失。
  function openProcessDesigner(payload = {}) {
    const processId = String(payload?.processId || '')
    if (!processId)
      return
    embeddedProcessDesignerId.value = processId
    embeddedProcessDesignerVisible.value = true
  }
  __impl.openProcessDesigner = openProcessDesigner

  async function handleEmbeddedProcessDesignerSaved() {
    embeddedProcessDesignerVisible.value = false
    await refreshWorkspaceMetadata()
  }
  __impl.handleEmbeddedProcessDesignerSaved = handleEmbeddedProcessDesignerSaved

  // 流程列表面板的“应用发布”入口：跳转到独立发布页
  function handleProcessPanelNavigate(section) {
    if (section === 'releases') {
      router.push({ name: 'BusinessApplicationPublish', params: { applicationCode: route.params.applicationCode } })
    }
  }
  __impl.handleProcessPanelNavigate = handleProcessPanelNavigate

  /** 顶栏「应用发布」：跳转独立发布页（与中间 Tab「页面访问」配置面板区分） */
  function openApplicationPublishFromDesign() {
    const applicationCode = application.value?.applicationCode || route.params.applicationCode
    if (!applicationCode) {
      message.warning('缺少应用编码，无法打开发布页')
      return
    }
    const query = { fromPageDesign: '1' }
    const pageId = String(currentNode.value?.id || route.query.pageId || selectedNodeId.value || '').trim()
    if (pageId)
      query.pageId = pageId
    if (route.query.draft === '1')
      query.draft = '1'
    if (route.query.edit === '1' || editing.value)
      query.edit = '1'
    if (route.query.returnEdit === '1' || route.query.edit === '1' || editing.value || route.query.draft === '1')
      query.returnEdit = '1'
    router.push({
      name: 'BusinessApplicationPublish',
      params: { applicationCode },
      query,
    })
  }
  __impl.openApplicationPublishFromDesign = openApplicationPublishFromDesign

  async function handleExtensionsChanged() {
    await refreshWorkspaceMetadata()
  }
  __impl.handleExtensionsChanged = handleExtensionsChanged

  async function handleEmbeddedDesignerSaved(savedSchema) {
    embeddedDesignerDirty.value = false
    if (activePageDesignTab.value === 'list')
      promoteCurrentPageToListShape(savedSchema)
    // 预览前轻量落盘：只同步页面形态 + 刷门户 CRUD，避免 workspace 全量拉取把设计器打回重载
    if (suppressHeavyEmbeddedSaveSideEffects.value) {
      if (dirty.value)
        await persistApplicationDraft()
      bumpPortalCrudConfigAfterDesignerSave()
      return
    }
    // 列表设计回写页面区块后可能弄脏 builder，这里一并落盘并清脏，避免退出仍提示未保存。
    if (dirty.value)
      await persistApplicationDraft()
    await refreshWorkspaceMetadata({ syncBuilder: true, markClean: true })
    if (dirty.value)
      await markBuilderClean()
  }
  __impl.handleEmbeddedDesignerSaved = handleEmbeddedDesignerSaved

  /**
   * “表单页”创建时区块写入了 formOnly / showSearch:false 等表单形态锁死配置，
   * 且区块上的 showImport/searchFieldRefs 等快照会优先于运行时配置。
   * 用户在该页保存列表设计后，列表设计就是该页列表区块的事实来源：
   * 同步查询字段快照、解除形态限制并清除模板遗留的工具栏覆盖项，
   * 否则渲染页面会缺查询条件或批量操作，与列表设计结果不一致。
   */
  function promoteCurrentPageToListShape(savedSchema = null) {
    const pageId = selectedNodeId.value
    if (!pageId)
      return
    const node = (builder.value?.nodes || []).find(item => item.id === pageId)
    const isFormShapeNode = node?.pageTemplate === 'form' || node?.objectRef?.pageMode === 'form'
    const savedZones = Array.isArray(savedSchema?.zones) ? savedSchema.zones : []
    const savedSearchZone = savedZones.find(zone => zone?.zoneKey === 'search')
    // 优先取本次列表设计保存的查询区配置，缓存的运行时配置仅作兜底（缓存可能落后于草稿）。
    const latestSearchRefs = (savedSearchZone && Array.isArray(savedSearchZone.fieldRefs)
      ? savedSearchZone.fieldRefs
      : (runtimeCrudPropsByObjectId.value[resolveRuntimeObjectCacheKey(node?.objectRef || {})]?.searchSchema || [])
          .map(field => field?.field))
      .map(fieldCode => String(fieldCode || '').trim())
      .filter(Boolean)
    const searchEnabled = savedSearchZone ? savedSearchZone.enabled !== false : true
    // 模板/早期快照遗留的覆盖项一律清除，交回运行时配置（列表设计编译结果）接管。
    const legacyOverrideKeys = ['formOnly', 'showImport', 'showExport', 'enableCustomQuery', 'hideAdd', 'hideToolbar', 'hideBatchDelete', 'hideSelection']
    let blockPromoted = false
    const items = mapPageBlocksInTree(pageBlocks.value, (block) => {
      if (block.blockType !== 'AiCrudPage')
        return block
      const blockProps = block.props || {}
      const hasSearchRefs = Array.isArray(blockProps.searchFieldRefs)
      const searchRefsChanged = latestSearchRefs.length > 0
        && (!hasSearchRefs
          || blockProps.searchFieldRefs.length !== latestSearchRefs.length
          || blockProps.searchFieldRefs.some(fieldCode => !latestSearchRefs.includes(String(fieldCode))))
      // 静态预览（previewLiveData=false）会禁用导入/导出/自定义查询等批量操作，
      // 列表页必须按实时数据预览渲染，才能和列表设计的工具栏配置一致。
      const needsLivePreview = blockProps.previewLiveData !== true
      const hasLegacyOverrides = legacyOverrideKeys.some(key => key in blockProps)
        || blockProps.showSearch !== searchEnabled
        || needsLivePreview
      if (!searchRefsChanged && !hasLegacyOverrides)
        return block
      blockPromoted = true
      const nextProps = {
        ...blockProps,
        showSearch: searchEnabled,
        previewLiveData: true,
        previewMode: 'realList',
        objectRef: blockProps.objectRef
          ? { ...blockProps.objectRef, pageKey: 'list', pageMode: 'list' }
          : blockProps.objectRef,
      }
      if (latestSearchRefs.length)
        nextProps.searchFieldRefs = latestSearchRefs
      else
        delete nextProps.searchFieldRefs
      for (const key of legacyOverrideKeys)
        delete nextProps[key]
      return { ...block, props: nextProps }
    })
    if (!blockPromoted && !isFormShapeNode)
      return
    if (blockPromoted)
      updatePageBlocks(items)
    if (isFormShapeNode) {
      builder.value = {
        ...builder.value,
        nodes: builder.value.nodes.map(item => item.id === pageId
          ? {
              ...item,
              pageTemplate: 'list',
              objectRef: item.objectRef
                ? { ...item.objectRef, pageKey: 'list', pageMode: 'list' }
                : item.objectRef,
            }
          : item),
      }
    }
    scheduleNavigationSave()
  }
  __impl.promoteCurrentPageToListShape = promoteCurrentPageToListShape

  function handleRuntimeHeaderMoreSelect(key) {
    if (key === 'undo') {
      undoBuilder()
      return
    }
    if (key === 'redo') {
      redoBuilder()
      return
    }
    if (key === 'object-design') {
      openBusinessObjectDesign()
      return
    }
    if (key === 'exit-editing')
      requestExitEditing()
  }
  __impl.handleRuntimeHeaderMoreSelect = handleRuntimeHeaderMoreSelect

  function discardAndExitEditing() {
    historyReady.value = false
    builder.value = ensurePageTitleComponents(normalizeInAppBuilder(application.value?.options, application.value, objects.value))
    builder.value = ensureWorkbenchPageInBuilder(builder.value).schema
    savedSignature.value = JSON.stringify(builder.value)
    resetBuilderHistory(builder.value)
    exitEditingVisible.value = false
    exitToPageManagement()
  }
  __impl.discardAndExitEditing = discardAndExitEditing

  function openWorkspace() {
    router.push({ path: '/app-center' })
  }
  __impl.openWorkspace = openWorkspace

  async function switchRuntimeView(view) {
    const next = resolveRuntimeView(view)
    // 发布功能已统一到独立发布页，runtime 不再内嵌发布面板
    if (String(view || '').toLowerCase() === 'publish') {
      router.push({ name: 'BusinessApplicationPublish', params: { applicationCode: route.params.applicationCode } })
      return
    }
    runtimeViewMode.value = next
    router.replace({
      query: {
        ...route.query,
        view: next === 'pages' ? undefined : next,
      },
    })
  }
  __impl.switchRuntimeView = switchRuntimeView

  // 页面级 Tab（编辑模式）——严格按创建时的页面形态展示
  const PAGE_DESIGN_TABS = new Set(['page', 'form', 'list', 'settings', 'publish'])

  /**
   * 创建时页面形态：
   * custom → 自由布局（仅页面设计）
   * form → 表单页（仅表单设计）
   * list → 列表页（仅列表设计）
   * list-form / tree-list / tree-table → 表单 + 列表设计
   */
  function resolvePageShapeKey(pageId = '') {
    const routePageId = String(route.query.pageId || '').trim()
    const id = String(pageId || routePageId || selectedNodeId.value || '').trim()
    const designTab = readRouteDesignTab()
    if (isWorkbenchPageId(id) || isWorkbenchPageId(routePageId))
      return 'custom'
    // 编辑态 URL 明确是自由布局时，始终按 custom 展示（不因 selectedNodeId 漂移或首页回退误判）
    if (designTab === 'page' && (!routePageId || !id || routePageId === id))
      return 'custom'

    const node = (builder.value?.nodes || []).find(item => String(item.id) === id)
    return resolvePageShapeFromNode(node, { designTab: designTab === 'page' && routePageId === id ? 'page' : '' })
  }
  __impl.resolvePageShapeKey = resolvePageShapeKey

  function pageUsesFreeLayoutCanvas(pageId) {
    return resolvePageShapeKey(pageId) === 'custom'
  }
  __impl.pageUsesFreeLayoutCanvas = pageUsesFreeLayoutCanvas

  function resolvePageLayoutForDesign(pageId = '') {
    const id = String(pageId || route.query.pageId || selectedNodeId.value || '').trim()
    if (!id)
      return null
    if (isWorkbenchPageId(id))
      return builder.value?.pages?.[WORKBENCH_PAGE_ID] || null
    return builder.value?.pages?.[id] || null
  }

  /** 节点声明的形态（不受 URL designTab=page 强制成 custom） */
  function resolveDeclaredPageShape(pageId = '') {
    const id = String(pageId || route.query.pageId || selectedNodeId.value || '').trim()
    if (isWorkbenchPageId(id))
      return 'custom'
    const node = (builder.value?.nodes || []).find(item => String(item.id) === id)
    return resolvePageShapeFromNode(node)
  }

  function resolveEntryDesignTab(pageId) {
    // 与运行态 Portal 对齐：画布有伴生块时先进页面设计，避免只看到对象列表壳
    if (pageHasFreeLayoutCompanionBlocks(resolvePageLayoutForDesign(pageId)))
      return 'page'
    const shape = resolvePageShapeKey(pageId)
    if (shape === 'custom')
      return 'page'
    if (shape === 'list')
      return 'list'
    return 'form'
  }
  __impl.resolveEntryDesignTab = resolveEntryDesignTab

  const FORM_DESIGN_SHAPES = new Set(['form', 'list-form', 'tree-list', 'tree-table'])
  const LIST_DESIGN_SHAPES = new Set(['list', 'list-form', 'tree-list', 'tree-table'])

  function resolvePageDesignTab(value, pageId = '') {
    const normalized = String(Array.isArray(value) ? value[0] : value || '').trim()
    const resolvedPageId = String(pageId || route.query.pageId || selectedNodeId.value || '').trim()
    const node = (builder.value?.nodes || []).find(item => String(item.id) === resolvedPageId)
    // 刷新时 nodes 尚未加载：先信任 URL designTab，避免 list 被误判成 page 落到空白对象卡
    if (!node && PAGE_DESIGN_TABS.has(normalized))
      return normalized

    const shape = resolvePageShapeKey(resolvedPageId)
    // 明确的 page Tab 不再被改写成 form（否则自由布局中间会空白）
    if (normalized === 'page')
      return shape === 'custom' || readRouteDesignTab() === 'page' ? 'page' : resolveEntryDesignTab(resolvedPageId)
    if (normalized === 'form' && !FORM_DESIGN_SHAPES.has(shape))
      return resolveEntryDesignTab(resolvedPageId)
    if (normalized === 'list' && !LIST_DESIGN_SHAPES.has(shape))
      return resolveEntryDesignTab(resolvedPageId)
    if (PAGE_DESIGN_TABS.has(normalized))
      return normalized
    return resolveEntryDesignTab(resolvedPageId)
  }
  __impl.resolvePageDesignTab = resolvePageDesignTab

  // Ref is owned by part1 (watches/load need it early); part4 re-resolves once shape helpers exist.
  activePageDesignTab.value = resolvePageDesignTab(route.query.designTab, route.query.pageId)

  const currentPageShape = computed(() => {
    // 编辑态优先用 URL pageId，避免 selectedNodeId 短暂落在旧对象页时把 Tab 打成表单/列表
    const routeId = String(route.query.pageId || '').trim()
    if (editing.value && readRouteDesignTab() === 'page')
      return 'custom'
    return resolvePageShapeKey(routeId || selectedNodeId.value)
  })
  const isObjectBoundPage = computed(() => currentPageShape.value !== 'custom')
  const isFreeLayoutPage = computed(() => currentPageShape.value === 'custom')
  const showFreeLayoutCanvas = computed(() => {
    if (!editing.value || formDesignerMode.value)
      return false
    if (['list', 'settings', 'publish'].includes(activePageDesignTab.value))
      return false
    // URL 明确自由布局时，即使 activeTab 曾被写成 form 也要出画布
    if (readRouteDesignTab() === 'page')
      return activePageDesignTab.value !== 'form' || !showFormDesignTab.value
    if (activePageDesignTab.value === 'form' && showFormDesignTab.value)
      return false
    return isFreeLayoutPage.value || activePageDesignTab.value === 'page'
  })
  const showFormDesignWorkbench = computed(() => {
    if (formDesignerMode.value)
      return true
    if (!showFormDesignTab.value)
      return false
    return editing.value && activePageDesignTab.value === 'form'
  })
  const showPageDesignTab = computed(() => {
    if (readRouteDesignTab() === 'page' || currentPageShape.value === 'custom')
      return true
    // 对象列表页叠了自由布局伴生块：仍要露出「页面设计」，与运行态所见一致
    return pageHasFreeLayoutCompanionBlocks(resolvePageLayoutForDesign())
  })
  // 表单/列表 Tab 看节点真实形态；designTab=page 时不再隐藏，方便伴生页切回对象设计
  const showFormDesignTab = computed(() => FORM_DESIGN_SHAPES.has(resolveDeclaredPageShape()))
  const showListDesignTab = computed(() => LIST_DESIGN_SHAPES.has(resolveDeclaredPageShape()))
  const isPageDesignTabActive = computed(() => showPageDesignTab.value && (activePageDesignTab.value === 'page' || showFreeLayoutCanvas.value))

  // —— 自由布局：容器 height:auto 撑开后，把实测高度写回 pageFlowHeight 并碰撞下推 ——
  function collectMeasuredPageBlockHeights() {
    if (typeof document === 'undefined')
      return new Map()
    const flow = document.querySelector('.application-page-flow')
    if (!flow)
      return new Map()
    const measured = new Map()
    flow.querySelectorAll(':scope > .application-page-block[data-page-block-id]').forEach((node) => {
      const id = node.getAttribute('data-page-block-id')
      const height = Math.round(node.getBoundingClientRect().height)
      if (id && height > 0)
        measured.set(id, height)
    })
    return measured
  }

  function syncFreeLayoutMeasuredHeights() {
    if (mut.syncingFreeLayoutHeights)
      return false
    // 拖拽/缩放过程中的瞬时尺寸不要回写，避免与手势抢历史
    if (mut.pageBlockMoveCtx || mut.pageBlockResizeCtx || draggingPageBlockId.value)
      return false
    if (!editing.value || pageFlowStackMode.value || !showFreeLayoutCanvas.value)
      return false
    const { items, changed } = applyMeasuredPageBlockHeights(
      pageBlocks.value,
      collectMeasuredPageBlockHeights(),
    )
    if (!changed)
      return false
    mut.syncingFreeLayoutHeights = true
    try {
      // 实测高度是系统对齐，不能进撤销栈，否则「移入布局/复制」后 ⌘Z 会先撤高度而不是业务操作
      runWithoutHistory(() => {
        updatePageBlocks(items, { resolveCollisions: true })
      })
    }
    finally {
      nextTick(() => {
        mut.syncingFreeLayoutHeights = false
      })
    }
    return true
  }
  __impl.syncFreeLayoutMeasuredHeights = syncFreeLayoutMeasuredHeights

  function scheduleFreeLayoutHeightSync() {
    if (mut.freeLayoutHeightSyncTimer)
      clearTimeout(mut.freeLayoutHeightSyncTimer)
    mut.freeLayoutHeightSyncTimer = setTimeout(() => {
      mut.freeLayoutHeightSyncTimer = null
      syncFreeLayoutMeasuredHeights()
    }, 80)
  }

  function bindFreeLayoutHeightObserver() {
    if (typeof ResizeObserver === 'undefined' || typeof document === 'undefined')
      return
    const flow = document.querySelector('.application-page-flow')
    if (!flow)
      return
    if (mut.freeLayoutHeightObserverFlow !== flow) {
      mut.freeLayoutHeightObserver?.disconnect?.()
      mut.freeLayoutHeightObserverFlow = flow
      mut.freeLayoutHeightObserver = new ResizeObserver(() => scheduleFreeLayoutHeightSync())
      mut.freeLayoutHeightObserver.observe(flow)
    }
    // 新增根级块后补 observe；同一节点重复 observe 无害
    flow.querySelectorAll(':scope > .application-page-block').forEach((node) => {
      mut.freeLayoutHeightObserver.observe(node)
    })
    scheduleFreeLayoutHeightSync()
  }

  function unbindFreeLayoutHeightObserver() {
    if (mut.freeLayoutHeightSyncTimer) {
      clearTimeout(mut.freeLayoutHeightSyncTimer)
      mut.freeLayoutHeightSyncTimer = null
    }
    mut.freeLayoutHeightObserver?.disconnect?.()
    mut.freeLayoutHeightObserver = null
    mut.freeLayoutHeightObserverFlow = null
  }

  watch(
    [
      editing,
      showFreeLayoutCanvas,
      pageFlowStackMode,
      editCanvasBootstrapping,
      () => pageBlocks.value.map(item => item.id).join('|'),
    ],
    async ([isEditing, showCanvas, stackMode, bootstrapping]) => {
      if (!isEditing || !showCanvas || stackMode) {
        unbindFreeLayoutHeightObserver()
        return
      }
      // 画布 skeleton 期间节点 display:none，高度为 0；等 bootstrap 结束后再量
      if (bootstrapping)
        return
      await nextTick()
      bindFreeLayoutHeightObserver()
      // 布局稳定后再补一次，避免首屏 ResizeObserver 漏触发
      scheduleFreeLayoutHeightSync()
      setTimeout(() => scheduleFreeLayoutHeightSync(), 200)
    },
    { immediate: true },
  )
  onBeforeUnmount(() => unbindFreeLayoutHeightObserver())

  // 进过列表/表单设计后，草稿预览期间保活实例，返回编辑不再整页重挂
  watch(
    [editing, activePageDesignTab],
    ([isEditing, tab]) => {
      if (!isEditing)
        return
      if (tab === 'list')
        listDesignerSessionActive.value = true
      if (tab === 'form' || formDesignerMode.value)
        formDesignerSessionActive.value = true
    },
    { immediate: true },
  )
  const keepListDesignerAlive = computed(() => (
    Boolean(pageDesignObject.value)
    && listDesignerSessionActive.value
    && (editing.value || isDraftPreviewMode())
  ))
  const keepFormDesignerAlive = computed(() => (
    formDesignerSessionActive.value
    && showFormDesignTab.value
    && (editing.value || isDraftPreviewMode() || formDesignerMode.value)
  ))
  const showListDesignerSurface = computed(() => editing.value && activePageDesignTab.value === 'list')
  const showFormDesignerSurface = computed(() => showFormDesignWorkbench.value)
  // 草稿预览下切设置/发布时只显示对应面板，避免与页面预览叠层串样式
  const showRuntimePageSurface = computed(() => {
    if (showListDesignerSurface.value || showFormDesignerSurface.value)
      return false
    if (showFreeLayoutCanvas.value)
      return true
    if (editing.value || runtimeViewMode.value !== 'pages')
      return false
    if (isDraftPreviewMode() && ['settings', 'publish'].includes(activePageDesignTab.value))
      return false
    return true
  })

  __impl.startPageBlockResize = startPageBlockResize
  __impl.onPageBlockResize = onPageBlockResize
  __impl.updateResizeCollisionHighlights = updateResizeCollisionHighlights
  __impl.endPageBlockResize = endPageBlockResize
  __impl.startPageBlockMove = startPageBlockMove
  __impl.onPageBlockMove = onPageBlockMove
  __impl.applyPageBlockMove = applyPageBlockMove
  __impl.snapPageBlockPosition = snapPageBlockPosition
  __impl.updatePageAlignGuides = updatePageAlignGuides
  __impl.clearPageAlignGuides = clearPageAlignGuides
  __impl.endPageBlockMove = endPageBlockMove
  __impl.applyPageBlockSwapPreview = applyPageBlockSwapPreview
  __impl.clearPageBlockSwapPreview = clearPageBlockSwapPreview
  __impl.animatePageBlockSwap = animatePageBlockSwap
  __impl.moveRootPageBlockToTab = moveRootPageBlockToTab
  __impl.moveRootPageBlockToGridCell = moveRootPageBlockToGridCell
  __impl.resolvePageBlockSwapTarget = resolvePageBlockSwapTarget
  __impl.startComponentButtonMove = startComponentButtonMove
  __impl.onComponentButtonMove = onComponentButtonMove
  __impl.endComponentButtonMove = endComponentButtonMove
  __impl.createLegacyBlock = createLegacyBlock
  __impl.saveActiveFormDesigner = saveActiveFormDesigner
  __impl.scheduleNavigationSave = scheduleNavigationSave
  __impl.saveNavigationDraft = saveNavigationDraft
  __impl.saveDraft = saveDraft
  __impl.persistApplicationDraft = persistApplicationDraft
  __impl.provisionPendingFormData = provisionPendingFormData
  __impl.setFormDataProvisionState = setFormDataProvisionState
  __impl.requestExitEditing = requestExitEditing
  __impl.isDraftPreviewMode = isDraftPreviewMode
  __impl.resolveRuntimeBackLabel = resolveRuntimeBackLabel
  __impl.resolveRuntimeBrandEyebrow = resolveRuntimeBrandEyebrow
  __impl.resolveRuntimeHeaderTitle = resolveRuntimeHeaderTitle
  __impl.resolveRuntimeBrandStatus = resolveRuntimeBrandStatus
  __impl.openRuntimeWorkbench = openRuntimeWorkbench
  __impl.handleRuntimeBack = handleRuntimeBack
  __impl.returnToEditorFromDraftPreview = returnToEditorFromDraftPreview
  __impl.exitToPageManagement = exitToPageManagement
  __impl.currentDesignerDirty = currentDesignerDirty
  __impl.saveCurrentDesignerSection = saveCurrentDesignerSection
  __impl.bumpPortalCrudConfigAfterDesignerSave = bumpPortalCrudConfigAfterDesignerSave
  __impl.applyDesignerResource = applyDesignerResource
  __impl.selectDesignerResource = selectDesignerResource
  __impl.resolveResourceNodeBuilderNode = resolveResourceNodeBuilderNode
  __impl.resolveResourceNodeMenuOptions = resolveResourceNodeMenuOptions
  __impl.handleResourceNodeAction = handleResourceNodeAction
  __impl.openEmbeddedObjectActions = openEmbeddedObjectActions
  __impl.openProcessDesigner = openProcessDesigner
  __impl.handleEmbeddedProcessDesignerSaved = handleEmbeddedProcessDesignerSaved
  __impl.handleProcessPanelNavigate = handleProcessPanelNavigate
  __impl.handleExtensionsChanged = handleExtensionsChanged
  __impl.handleEmbeddedDesignerSaved = handleEmbeddedDesignerSaved
  __impl.promoteCurrentPageToListShape = promoteCurrentPageToListShape
  __impl.handleRuntimeHeaderMoreSelect = handleRuntimeHeaderMoreSelect
  __impl.discardAndExitEditing = discardAndExitEditing
  __impl.openWorkspace = openWorkspace
  __impl.switchRuntimeView = switchRuntimeView
  __impl.resolvePageShapeKey = resolvePageShapeKey
  __impl.pageUsesFreeLayoutCanvas = pageUsesFreeLayoutCanvas
  __impl.resolveEntryDesignTab = resolveEntryDesignTab
  __impl.resolvePageDesignTab = resolvePageDesignTab

  return {
    ...deps,
    PAGE_DESIGN_TABS, FORM_DESIGN_SHAPES, LIST_DESIGN_SHAPES, activePageDesignTab, currentPageShape, isObjectBoundPage, isFreeLayoutPage, showFreeLayoutCanvas,
    showFormDesignWorkbench, showPageDesignTab, showFormDesignTab, showListDesignTab, isPageDesignTabActive,
    keepListDesignerAlive, keepFormDesignerAlive, showListDesignerSurface, showFormDesignerSurface,
    showRuntimePageSurface, syncFreeLayoutMeasuredHeights,
  }
}
