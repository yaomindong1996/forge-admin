/** application-runtime setup part 5. */
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
import { resolvePageBlockShellStyle as computePageBlockShellStyle, readPageBlockLength, resolveDefaultPageBlockHeight, resolveDefaultPageBlockYFromItems, resolvePageBlockFlowGeometry, resolveRootPageBlockCollisions, shouldUsePageFlowStack, normalizePagePadding, resolvePagePaddingCss, migratePageFlowToContentCoords, canvasToContentFlowPoint, contentToCanvasFlowPoint, DEFAULT_PAGE_PADDING } from './page-flow-geometry'
import { flattenNodes, hasPermission, isNavigationVisible } from './runtime-navigation-utils'
import { useBuilderHistory } from './use-builder-history'
import { useRuntimeCrudConfig } from './use-runtime-crud-config'

export function applyApplicationRuntimePart5(deps = {}) {
  const {
    __impl, mut, animatePageBlockSwap, appendPageBlock, appendPageBlockToContainer, appendPageBlockToGridCell, appendPageBlockToTab, applyDesignerResource,
    applyNestedPageBlockMove, applyPageBlockMove, applyPageBlockSwapPreview, applyPageTemplate, applyFreeLayoutPreset, applyCurrentFreeLayoutPreset, openLayoutPresetPicker, attachDefaultRuntimeObject, attachSingleFormAsset, bindSingleFormToCompatibleBlocks, bumpPortalCrudConfigAfterDesignerSave,
    clearPageAlignGuides, clearPageBlockSwapPreview, clonePageBlockTree, confirmNavigationAction, confirmSystemMenuMount, copySelectedBlockToOtherPage, createFormAssetForPageCrud, createFormAssetForSelectedBlock,
    createFormFieldVisibilitySettings, createLegacyBlock, createPageFromTemplate, createQuickNode, createStandaloneFormAsset, currentDesignerDirty, discardAndExitEditing, duplicateNavigationNode,
    duplicateNestedPageBlock, editSelectedBlockFormAsset, endComponentButtonMove, endNestedPageBlockMove, endNestedPageBlockResize, endPageBlockMove, endPageBlockResize, ensureFormDesignerObjectContext,
    ensurePageFlowContentCoords, ensurePageTitleComponents, exitToPageManagement, extractNestedPageBlockToCanvas, findPageBlockByFormAssetId, finishCatalogPointerDrag, handleCatalogPointerMove, handleComponentCatalogClick,
    handleComponentCatalogDragEnd, handleComponentCatalogDragStart, handleEmbeddedDesignerSaved, handleEmbeddedProcessDesignerSaved, handleExtensionsChanged, handleInlineTextUpdate, handleNavigationMoreSelect, handleNestedPageBlockMenuSelect,
    handleNestedPageBlockMoveStart, handleNestedPageBlockResizeStart, handleNestedPageBlockSelect, handlePageBlockDataSourceRequest, handlePageBlockMoreSelect, handlePageFlowBlankClick, handlePageFlowContainerClear, handlePageFlowContainerInsert,
    handlePageFlowDragOver, handlePageFlowDrop, handlePageFlowGridCellDrop, handlePageFlowTabDrop, handlePageTypeSelection, handleProcessPanelNavigate, handleResourceNodeAction, handleRuntimeBack,
    handleRuntimeHeaderMoreSelect, handleRuntimeTreeSelect, hydratePageCrudApiPlaceholders, insertComponent, isDraftPreviewMode, isGroupCollapsed, isNavigationGroupDescendant, isPageBlockDataSourceConfigured,
    isPageBlockRuntimeCrudLoading, isPageCatalogDrag, isSimpleBodyContainer, isValidPageBlockObjectRef, itemBorderColorMode, load, markBuilderClean, moveNavigationByOffset,
    movePageBlockIntoContainer, moveRootPageBlockToGridCell, moveRootPageBlockToTab, normalizeContainerDropTarget, normalizePageBlockForContainer, onComponentButtonMove, onNestedPageBlockMove, onPageBlockMove,
    onPageBlockResize, openEmbeddedObjectActions, openFormAssetDesigner, openPageBlockConfiguration, openPagePropertiesPanel, openPageTypeSelector, openProcessDesigner, openSelectedBlockFormDesigner, openSystemMenuMountDialog,
    openRuntimeWorkbench, openWorkspace, pageUsesFreeLayoutCanvas, patchNavigationNodeMount, persistApplicationDraft, prefetchRuntimeWorkspacePanels, preloadCurrentPageCrudRuntimeProps, preloadPageBlockCrudRuntimeProps, promoteCurrentPageToListShape,
    provisionPendingFormData, readRouteDesignTab, relocateNestedPageBlockToContainer, relocateNestedPageBlockToGridCell, relocateNestedPageBlockToTab, renderNavigationMenuIcon, requestExitEditing, resizeNestedPageBlock,
    resolveActiveDesignerPageId, resolveComponentIcon, resolveComponentPickerGroup, resolveEmptyGuideIcon, resolveEntryDesignTab, resolveFormAssetFields, resolveFormLayoutBlockType, resolveNavigationMoreOptions,
    resolveNextNavigationTitle, resolveNextPageBlockStackY, resolvePageBlockBackgroundColor, resolvePageBlockFields, resolvePageBlockMoreOptions, resolvePageBlockMoveIntoOptions, resolvePageBlockObjectRef, resolvePageBlockRuntimeCrudProps,
    resolvePageBlockShellStyle, resolvePageBlockSwapTarget, resolvePageContextFieldCatalog, resolvePageDesignTab, resolvePageFlowContainerTargetByPageBlock, resolvePageFlowContainerTargetByRect, resolvePageFlowContainerTargetFromElement, resolvePageFlowContainerTargetFromPoint,
    resolvePageFlowGridCellTarget, resolvePageFlowGridCellTargetByPageBlock, resolvePageFlowGridCellTargetByRect, resolvePageFlowGridCellTargetFromElement, resolvePageFlowGridCellTargetFromPoint, resolvePageFlowTabTarget, resolvePageFlowTabTargetFromPoint, resolvePagePreviewBlock,
    resolvePageShapeDesignContext, resolvePageShapeKey, resolvePageTemplateIcon, resolvePortalFormFields, resolveResourceNodeBuilderNode, resolveResourceNodeMenuOptions, resolveRuntimeBackLabel, resolveRuntimeBrandEyebrow,
    resolveRuntimeHeaderTitle, resolveRuntimeBrandStatus, resolveRuntimeObjectCacheKey, resolveRuntimeView, resolveSelectablePageId, resolveSystemPageIcon, returnToEditorFromDraftPreview, returnToPageDesigner, saveActiveFormDesigner,
    saveCurrentDesignerSection, saveDraft, saveNavigationDraft, scheduleNavigationSave, selectCreatedDesignerPage, selectDesignerResource, selectFormAssetFromPicker, selectIntroTemplate,
    selectNode, selectPageBlock, setFormDataProvisionState, snapPageBlockPosition, startCatalogPointerDrag, startComponentButtonMove, startPageBlockMove, startPageBlockResize,
    supportsFormAsset, switchRuntimeView, syncActivePageShapeObject, syncCatalogDropActiveClass, toggleGroupExpanded, toggleNavigationVisible, unmountSystemMenu, updateActiveFormDesignerSchema,
    updateCurrentGridLayout, updatePageAlignGuides, updatePageBlockBackgroundColor, updatePageBlockSize, updatePageBlocks, updatePageCanvasPadding, updateResizeCollisionHighlights, updateSelectedBlockAppearance,
    updateSelectedBlockFormAsset, updateSelectedPageBlockRuntimeObject, warmCurrentPortalPageCrud, route, router, message, tenantStore, formComponentIconModules,
    formComponentIconCache, formComponentIconFileByBlockType, userStore, asyncPanelLoader, ApplicationObjectsPanel, ApplicationExtensionsPanel, ApplicationProcessPanel, ApplicationSettingsPanel,
    runtimeWorkspacePanelImporters, BusinessObjectDesignerPage, BusinessProcessPage, ForgeFormDesigner, ListPageGridDesigner, IconSelector, PageTypeSelector, PageDesignSettingsPanel,
    PageDesignPublishPanel, PageManagementSystemView, GridBlockRenderer, application, objects, builder, processSelectableObjects, loadError,
    loading, editCanvasBootstrapping, saving, previewTabLoading, restoringObjectPageLayout, editing, runtimeViewMode, exitEditingVisible, selectedNodeId,
    enhanceContextPageId, newNodePopoverVisible, selectedPageTemplateKey, iconPickerVisible, iconPickerNodeId, navigationActionVisible, navigationActionMode, navigationActionNodeId,
    navigationActionForm, systemMenuMountVisible, systemMenuMountNodeId, systemMenuMountNode, componentPopoverVisible, componentKeyword, savedSignature, selectedPageBlockId,
    draggingPageBlockId, dragPreview, pageAlignGuides, PAGE_ALIGN_SNAP_PX, configPanelVisible, inspectorTab, componentButtonPosition, componentButtonMoveCtx,
    catalogDragBlockType, suppressCatalogClick, activePageFlowTabTarget, activePageFlowGridTarget, activePageFlowContainerTarget, resizeCollisionBlockIds, formDesignerObjectContextByObjectId, formDesignerObjectContextLoadingIds,
    formDesignerMode, designerTransitionLoading, runtimeTreeFilter, runtimeTreeFilterByBlockId, runtimeTreeActiveKeyByBlockId, formDesignerFromPageManagement, activeFormAssetId, activePageShapeDesign,
    pageTypeSelectorVisible, layoutPresetPickerVisible, pageTypeSelectorParentId, pageTypeSelectorDefaultType, sidebarCollapsed, renamingApplication, renameApplicationValue, renameSaving, renameInputRef,
    collapsedGroupIds, copyBlockVisible, copyBlockId, copyBlockTargetPageId, blockBackgroundPickerVisible, backgroundPickerBlockId, formAssetSelectorOpen, formAssetSelectorKeyword,
    formDataProvisioningByAssetId, objectSetupVisible, selectedDesignerResourceKey, workspaceExtensions, workspaceEntries, portalCrudConfigRevision, portalCrudSeed, isDraftMode,
    activeFlowContext, embeddedDesignerRef, embeddedDesignerDirty, embeddedDesignerSaving, embeddedProcessDesignerVisible, embeddedProcessDesignerId, applicationRuntimeLoadCoordinator, componentPickerGroupOptions,
    runtimeHeaderMoreOptions, pageManagementSystemPages, systemPageIconMap, currentSystemPage, workbenchPage, systemPageNavigationRoutes, designerResourceGroups, activeDesignerResource,
    designerSection, pageBuilderResourceActive, canEditApplication, usePortalDesignPreview, activeDesignerObject, activeResourceConfigKey, previewableResourceActive, legacyBlockTypeMap,
    pageBlockResizeAnchors, pageBlockRecommendedColors, groupOptions, navigationActionNode, iconPickerNode, navigationIconValue, navigationActionTitle, navigationActionHasChildren,
    navigationDeleteImpact, navigationDeleteTip, moveGroupOptions, navigationNodes, currentNode, currentPage, currentPageLayoutPolluted, currentPageManagementTitle,
    pageDesignObject, currentGridLayout, pageBlocks, pageFlowStackMode, pageCanvasPadding, pageCanvasPaddingCss, designerGridLayout, appPageTargetOptions, pageFlowHeight,
    hasCustomComponentButtonPosition, componentButtonStyle, selectedPageBlock, formAssets, filteredFormAssets, pageFormAssetOptions, activeFormAsset, activeFormDesignerSchema,
    activeFormAssetBlock, activeFormDesignerObjectRef, activeFormDesignerContext, activeFormDesignerRelations, activeFormDesignerActions, activeFormFields, activeFormDataState, selectedPageBlockFormAssetId,
    selectedPageBlockFormAsset, selectedPageBlockRuntimeObjectRef, selectedPageBlockIsCrud, selectedPageBlockSupportsDataSource, selectedPageBlockUsesObjectRuntime, selectedPageBlockRuntimeObjectId, selectedPageBlockObjectDesignerPanel, selectedPageBlockFormDataState,
    runtimeObjectFormOptions, pageTemplateOptions, freeLayoutPresetOptions, selectedPageBlockFields, selectedPageBlockFormDesignerSchema, applicationGridModelSchema, dragPreviewBlock, nestedMovingPageBlockId, copyBlockPageOptions,
    dirty, canSaveActiveFormDesigner, loadErrorTitle, filteredComponents, componentPickerGroups, recommendedComponents, PAGE_DESIGN_TABS, FORM_DESIGN_SHAPES,
    LIST_DESIGN_SHAPES, activePageDesignTab, currentPageShape, isObjectBoundPage, isFreeLayoutPage, showFreeLayoutCanvas, showFormDesignWorkbench, showPageDesignTab,
    showFormDesignTab, showListDesignTab, isPageDesignTabActive,
    runtimeCrudPropsByObjectId, runtimeCrudLoadingObjectIds, runtimeCrudUnavailableObjectIds, loadRuntimeCrudProps, resetRuntimeCrudConfig, historyReady, canUndo, canRedo, resetBuilderHistory,
  
  } = deps
  function switchPageDesignTab(tab) {
    const requested = String(tab || '').trim()
    // 预览 Tab：草稿预览里回到预览内容；编辑态则同页进入草稿预览（保留顶栏预览入口）
    if (requested === 'preview') {
      if (previewTabLoading.value)
        return
      if (isDraftPreviewMode()) {
        const entry = resolveEntryDesignTab(selectedNodeId.value)
        activePageDesignTab.value = entry
        if (formDesignerMode.value)
          formDesignerMode.value = false
        const nextQuery = { ...route.query }
        if (nextQuery.designTab == null || nextQuery.designTab === '')
          return
        delete nextQuery.designTab
        router.replace({ query: nextQuery })
        return
      }
      if (editing.value) {
        void openDraftPreviewInPlace()
        return
      }
    }
    // 草稿预览：设置/发布就地切换；列表/表单/页面设计回编辑态（预览 Tab 在编辑顶栏仍保留）
    if (isDraftPreviewMode()) {
      const next = resolvePageDesignTab(requested, selectedNodeId.value)
      if (next === 'settings' || next === 'publish') {
        activePageDesignTab.value = next
        if (formDesignerMode.value)
          formDesignerMode.value = false
        const defaultTab = resolveEntryDesignTab(selectedNodeId.value)
        const designTab = next === defaultTab ? undefined : next
        const current = route.query.designTab == null || route.query.designTab === ''
          ? undefined
          : String(route.query.designTab)
        if (current === designTab)
          return
        router.replace({
          query: {
            ...route.query,
            designTab,
          },
        })
        return
      }
      returnToEditorFromDraftPreview({ designTab: next })
      return
    }
    const next = resolvePageDesignTab(tab, selectedNodeId.value)
    activePageDesignTab.value = next
    if (formDesignerMode.value)
      formDesignerMode.value = false
    if (next === 'form' && selectedNodeId.value)
      syncActiveFormAssetForPage(selectedNodeId.value)
    const defaultTab = resolveEntryDesignTab(selectedNodeId.value)
    const designTab = next === defaultTab ? undefined : next
    const current = route.query.designTab == null || route.query.designTab === ''
      ? undefined
      : String(route.query.designTab)
    if (current === designTab)
      return
    router.replace({
      query: {
        ...route.query,
        designTab,
      },
    })
  }
  __impl.switchPageDesignTab = switchPageDesignTab

  function resolveFormAssetIdForPage(pageId) {
    const page = builder.value?.pages?.[pageId]
    const items = page?.layout?.gridLayout?.items || []
    const fromBlock = findFirstBlockWithFormAssetId(items)
    if (fromBlock)
      return fromBlock
    const assets = builder.value?.formAssets || []
    if (!assets.length)
      return ''
    const node = builder.value?.nodes?.find(item => item.id === pageId)
    return assets.find(asset => asset.name === node?.title)?.id || assets[0].id || ''
  }
  __impl.resolveFormAssetIdForPage = resolveFormAssetIdForPage

  function findFirstBlockWithFormAssetId(blocks = []) {
    for (const block of blocks || []) {
      const formAssetId = String(block?.props?.formAssetId || '').trim()
      if (formAssetId)
        return formAssetId
      const nested = [
        ...(block?.children || []),
        ...(block?.props?.tabs || []).flatMap(tab => tab?.children || []),
        ...(block?.props?.cells || []).flatMap(cell => cell?.children || []),
      ]
      const matched = findFirstBlockWithFormAssetId(nested)
      if (matched)
        return matched
    }
    return ''
  }
  __impl.findFirstBlockWithFormAssetId = findFirstBlockWithFormAssetId

  function syncActiveFormAssetForPage(pageId) {
    const formAssetId = resolveFormAssetIdForPage(pageId)
    if (!formAssetId) {
      activeFormAssetId.value = ''
      activePageShapeDesign.value = null
      return
    }
    if (activeFormAssetId.value !== formAssetId)
      activeFormAssetId.value = formAssetId
    activePageShapeDesign.value = resolvePageShapeDesignContext(formAssetId)
  }
  __impl.syncActiveFormAssetForPage = syncActiveFormAssetForPage

  function createFormAssetForCurrentPage() {
    if (!currentNode.value || !application.value)
      return
    const name = currentNode.value.title || '未命名表单'
    const objectRef = currentNode.value.objectRef || {}
    const result = createInAppFormAsset(builder.value, {
      name,
      formKey: objectRef.objectCode ? `${objectRef.objectCode}_form` : undefined,
      formDesignerSchema: createDefaultFormDesignerSchema({
        objectCode: objectRef.objectCode || application.value.applicationCode || 'application',
        objectName: objectRef.objectName || name,
        formName: name,
      }),
    })
    const page = result.schema.pages?.[currentNode.value.id]
    const items = page?.layout?.gridLayout?.items || []
    const nextItems = items.map((item) => {
      if (item?.blockType !== 'AiCrudPage' && item?.blockType !== 'AiForm')
        return item
      return {
        ...item,
        props: {
          ...(item.props || {}),
          formAssetId: result.formAssetId,
        },
      }
    })
    builder.value = {
      ...result.schema,
      pages: {
        ...result.schema.pages,
        [currentNode.value.id]: {
          ...page,
          layout: {
            ...(page?.layout || {}),
            gridLayout: {
              ...(page?.layout?.gridLayout || {}),
              items: nextItems,
            },
          },
        },
      },
    }
    activeFormAssetId.value = result.formAssetId
    activePageShapeDesign.value = resolvePageShapeDesignContext(result.formAssetId)
  }
  __impl.createFormAssetForCurrentPage = createFormAssetForCurrentPage

  function patchCurrentPageNode(partial = {}) {
    if (!currentNode.value || !builder.value)
      return
    const pageId = currentNode.value.id
    const settingsFields = ['systemMenuVisible', 'navigationVisible', 'mountTarget', 'menuName', 'menuParentId', 'mobileMenuParentId', 'menuSort', 'printWatermark', 'pageParams']
    builder.value = {
      ...builder.value,
      nodes: builder.value.nodes.map((item) => {
        if (item.id !== pageId)
          return item
        const next = { ...item, ...partial }
        const settingsPatch = {}
        for (const key of settingsFields) {
          if (Object.prototype.hasOwnProperty.call(partial, key)) {
            settingsPatch[key] = key === 'navigationVisible'
              ? partial[key] !== false
              : partial[key]
          }
        }
        if (Object.keys(settingsPatch).length > 0) {
          next.settings = {
            ...(next.settings || item.settings || {}),
            ...settingsPatch,
          }
        }
        return next
      }),
    }
    // 页面发布面板的挂载配置属于导航树草稿。自动保存可避免用户直接
    // 切换到“应用发布”时，发布服务仍读取旧的菜单挂载状态。
    scheduleNavigationSave()
  }
  __impl.patchCurrentPageNode = patchCurrentPageNode

  async function restoreCurrentObjectPageLayout() {
    const pageId = String(currentNode.value?.id || selectedNodeId.value || route.query.pageId || '').trim()
    if (!pageId || !builder.value || restoringObjectPageLayout.value)
      return
    restoringObjectPageLayout.value = true
    try {
      builder.value = restoreObjectBoundPageLayout(builder.value, pageId)
      selectedPageBlockId.value = builder.value.pages?.[pageId]?.layout?.gridLayout?.items?.[0]?.id || ''
      syncActiveFormAssetForPage(pageId)
      await persistApplicationDraft()
      savedSignature.value = JSON.stringify(builder.value)
      message.success('已恢复为列表/表单布局，自由布局组件已清除')
    }
    catch (error) {
      message.error(error?.message || '恢复列表/表单布局失败')
    }
    finally {
      restoringObjectPageLayout.value = false
    }
  }
  __impl.restoreCurrentObjectPageLayout = restoreCurrentObjectPageLayout

  function selectPageManagementNode(pageId) {
    if (isWorkbenchPageId(pageId) && builder.value) {
      const ensured = ensureWorkbenchPageInBuilder(builder.value)
      if (ensured.created || ensured.upgraded)
        builder.value = ensured.schema
    }
    selectedNodeId.value = pageId
    selectedDesignerResourceKey.value = ''
    if (editing.value)
      return
    // 页面管理态：选中与 URL pageId、中间预览必须同页，避免残留 design* 或旧 pageId 串预览
    const nextQuery = {
      pageId: pageId || undefined,
    }
    const currentPageId = route.query.pageId == null || route.query.pageId === ''
      ? undefined
      : String(route.query.pageId)
    const hasDesignerResidue = route.query.designResource != null
      || route.query.edit != null
      || route.query.designTab != null
      || route.query.designSection != null
      || route.query.draft != null
    if (!hasDesignerResidue && currentPageId === nextQuery.pageId)
      return
    router.replace({ query: nextQuery })
  }
  __impl.selectPageManagementNode = selectPageManagementNode

  async function startRenameApplication() {
    renameApplicationValue.value = application.value?.applicationName || ''
    renamingApplication.value = true
    await nextTick()
    renameInputRef.value?.focus?.()
  }
  __impl.startRenameApplication = startRenameApplication

  function cancelRenameApplication() {
    renamingApplication.value = false
    renameApplicationValue.value = ''
  }
  __impl.cancelRenameApplication = cancelRenameApplication

  async function confirmRenameApplication() {
    const newName = String(renameApplicationValue.value || '').trim()
    if (!newName || !application.value) {
      cancelRenameApplication()
      return
    }
    if (newName === application.value.applicationName) {
      cancelRenameApplication()
      return
    }
    renameSaving.value = true
    try {
      await updateBusinessApplication({
        id: application.value.id,
        applicationCode: application.value.applicationCode,
        applicationName: newName,
        suiteCode: application.value.suiteCode,
        status: application.value.status,
        options: application.value.options,
      })
      application.value = { ...application.value, applicationName: newName }
      message.success('应用名称已修改')
      cancelRenameApplication()
      await refreshWorkspaceMetadata()
    }
    catch (error) {
      message.error(error?.message || '修改应用名称失败')
    }
    finally {
      renameSaving.value = false
    }
  }
  __impl.confirmRenameApplication = confirmRenameApplication

  function enterPageDesign(pageId) {
    // 记录用户是否从页面管理视图（非编辑模式）进入
    formDesignerFromPageManagement.value = !editing.value
    selectedNodeId.value = pageId
    // 与左侧 Portal 预览对齐：有自由布局画布内容时进「页面设计」，纯对象 CRUD 才进表单设计
    const entryTab = resolveEntryDesignTab(pageId)
    activePageDesignTab.value = entryTab
    if (entryTab === 'form')
      syncActiveFormAssetForPage(pageId)
    // 所有状态（编辑模式、选中页面、设计资源）统一通过一次路由更新驱动：
    // watch(route.query.edit) 设置 editing，watch(route.query.pageId) 设置 selectedNodeId，
    // watch(route.query.designResource) 设置 selectedDesignerResourceKey。
    // 避免在路由生效前同步修改这些 ref 触发 watch 产生不带 edit 的并发 router.replace。
    router.replace({
      query: {
        ...route.query,
        pageId,
        edit: '1',
        designResource: `page-custom:${pageId}`,
        designTab: entryTab === 'form' ? undefined : entryTab,
      },
    })
    // 进入自由布局时立刻回写 pageShape，避免刷新后再次误判
    if (entryTab === 'page' && builder.value && !isWorkbenchPageId(pageId)) {
      const node = (builder.value.nodes || []).find(item => String(item.id) === String(pageId))
      const repaired = ensureFreeLayoutPageNode(node)
      if (node && repaired !== node) {
        builder.value = {
          ...builder.value,
          nodes: builder.value.nodes.map(item => String(item.id) === String(pageId) ? repaired : item),
        }
      }
    }
  }
  __impl.enterPageDesign = enterPageDesign

  function enterWorkbenchDesign() {
    if (!builder.value)
      return
    const ensured = ensureWorkbenchPageInBuilder(builder.value)
    if (ensured.created || ensured.upgraded) {
      builder.value = ensured.schema
      if (ensured.created)
        message.success('已初始化个人工作台默认布局')
      else if (ensured.upgraded)
        message.success('已更新个人工作台默认布局')
    }
    enterPageDesign(WORKBENCH_PAGE_ID)
  }
  __impl.enterWorkbenchDesign = enterWorkbenchDesign

  function openCustomPageSelector() {
    openPageTypeSelector(null, 'custom')
  }
  __impl.openCustomPageSelector = openCustomPageSelector

  async function openExcelPageImport() {
    if (!application.value?.id)
      return
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = '.xlsx,.xls'
    input.addEventListener('change', async () => {
      const file = input.files?.[0]
      if (!file)
        return
      saving.value = true
      try {
        await previewBusinessApplicationExcel(file)
        await initializeBusinessApplicationExcel(application.value.id, file)
        await refreshWorkspaceMetadata()
        message.success('已根据 Excel 生成数据对象，请选择页面形态继续设计')
        openPageTypeSelector()
      }
      catch (error) {
        message.error(error?.message || '从 Excel 创建页面失败')
      }
      finally {
        saving.value = false
      }
    })
    input.click()
  }
  __impl.openExcelPageImport = openExcelPageImport

  function openFormAssetDesignerForPage(pageId) {
    const page = builder.value?.pages?.[pageId]
    const formAssetId = page?.layout?.gridLayout?.items?.find(item => item?.props?.formAssetId)?.props?.formAssetId
    if (formAssetId)
      openFormAssetDesigner(formAssetId)
  }
  __impl.openFormAssetDesignerForPage = openFormAssetDesignerForPage

  function resolveDraftPreviewPageId() {
    const selected = String(selectedNodeId.value || '').trim()
    const routeId = String(route.query.pageId || '').trim()
    const resourcePageId = String(activeDesignerResource.value?.pageId || '').trim()
    return (isPageManagementSystemPageId(resourcePageId) && resourcePageId)
      || (isPageManagementSystemPageId(selected) && selected)
      || routeId
      || resolveActiveDesignerPageId()
      || selected
      || WORKBENCH_PAGE_ID
  }

  async function prepareDraftPreviewNavigation() {
    // 仅脏时落盘：干净状态直接进预览，避免列表/表单每次点预览都全量保存+重载
    if (currentDesignerDirty() && !await saveCurrentDesignerSection({ forPreview: true }))
      return null
    // 自由编排页面走应用运行壳；对象页面/数据结构落到对象自身的 CRUD 运行页预览。
    if (!pageBuilderResourceActive.value && previewableResourceActive.value) {
      openObjectResourcePreview()
      return null
    }
    return resolveDraftPreviewPageId()
  }

  /** 顶栏「预览」Tab：同页切到草稿预览，避免新开窗口后丢 Tab */
  /** 草稿预览入口固定 pageKey=list；自定义/弹窗等二级页只允许运行态跳转进入，不改顶栏预览交互 */
  function buildDraftPreviewQuery(previewPageId) {
    return {
      pageId: previewPageId,
      draft: '1',
      returnEdit: '1',
      pageKey: 'list',
    }
  }

  /** 点预览瞬间先丢异步 chunk，与保存并行，缩短进草稿后的资源等待 */
  function prefetchDraftPreviewChunks() {
    void Promise.all([
      import('@/components/lowcode-builder/page/GridBlockRenderer.vue').catch(() => {}),
      import('@/components/ai-form/AiCrudPage.vue').catch(() => {}),
      import('@/components/lowcode-builder/shared/PageWidgetRenderer.vue').catch(() => {}),
      import('@/components/lowcode-builder/page/RuntimeListGridFlow.vue').catch(() => {}),
      import('@/views/app-center/components/portal/PortalPageRenderer.vue').catch(() => {}),
    ])
  }

  async function openDraftPreviewInPlace() {
    if (previewTabLoading.value)
      return
    previewTabLoading.value = true
    prefetchDraftPreviewChunks()
    const dirty = currentDesignerDirty()
    const loadingMsg = message.loading(
      dirty ? '正在保存草稿并打开预览…' : '正在打开预览…',
      { duration: 0 },
    )
    try {
      const previewPageId = await prepareDraftPreviewNavigation()
      if (!previewPageId)
        return
      await router.replace({
        name: 'BusinessApplicationRuntime',
        params: { applicationCode: application.value.applicationCode },
        query: buildDraftPreviewQuery(previewPageId),
      })
    }
    finally {
      loadingMsg?.destroy?.()
      previewTabLoading.value = false
    }
  }
  __impl.openDraftPreviewInPlace = openDraftPreviewInPlace

  async function openDraftPreview() {
    const previewPageId = await prepareDraftPreviewNavigation()
    if (!previewPageId)
      return
    const target = router.resolve({
      name: 'BusinessApplicationRuntime',
      params: { applicationCode: application.value.applicationCode },
      query: buildDraftPreviewQuery(previewPageId),
    })
    const win = window.open(target.href, '_blank')
    if (!win) {
      // 浏览器弹窗拦截器阻止了新窗口，回退到同标签页导航
      console.warn('[openDraftPreview] window.open 被浏览器拦截，回退到 router.push')
      router.push(target)
    }
  }
  __impl.openDraftPreview = openDraftPreview

  function openObjectResourcePreview() {
    const configKey = activeResourceConfigKey.value
    if (!configKey) {
      message.warning('该业务对象还没有可用的运行配置')
      return
    }
    const kind = activeDesignerResource.value?.kind || ''
    const formMode = kind === 'page-form'
    const query = { designPreview: '1' }
    if (formMode) {
      query.runtimeOpenMode = 'CREATE_FORM'
      query.pageKey = 'create'
      query.mode = 'create'
    }
    else {
      query.runtimeOpenMode = 'LIST'
      query.pageKey = 'list'
    }
    const target = router.resolve({ path: `/ai/crud-page/${configKey}`, query })
    const win = window.open(target.href, '_blank', 'noopener,noreferrer')
    if (!win) {
      // 浏览器弹窗拦截器阻止了新窗口，回退到同标签页导航
      console.warn('[openObjectResourcePreview] window.open 被浏览器拦截，回退到 router.push')
      router.push(target)
    }
  }
  __impl.openObjectResourcePreview = openObjectResourcePreview

  function openObjectDesigner(panel = 'list', targetObjectRef) {
    const objectRef = targetObjectRef || currentNode.value?.objectRef || selectedPageBlockRuntimeObjectRef.value
    const target = resolveObjectDesignerNavigationTarget(objectRef, objects.value)
    if (!target?.objectCode)
      return
    const detailTab = panel === 'detail' ? 'detail' : panel === 'form' ? 'form' : 'list'
    router.push({
      name: 'BusinessObjectDesigner',
      params: { objectCode: target.objectCode },
      query: {
        objectId: target.objectId,
        panel,
        detailTab,
        returnTo: route.fullPath,
      },
    })
  }
  __impl.openObjectDesigner = openObjectDesigner

  function openBusinessObjectDesign() {
    const objectRef = currentNode.value?.objectRef || selectedPageBlockRuntimeObjectRef.value
    if (resolveObjectDesignerNavigationTarget(objectRef, objects.value)?.objectCode) {
      openObjectDesigner('fields', objectRef)
      return
    }
    openObjectSetup()
  }
  __impl.openBusinessObjectDesign = openBusinessObjectDesign

  async function openObjectSetup() {
    if (dirty.value && !await saveDraft())
      return
    objectSetupVisible.value = true
  }
  __impl.openObjectSetup = openObjectSetup

  async function handleApplicationObjectsChanged(change = null) {
    const shouldBindCurrentCrud = Boolean(
      change?.objectId
      && selectedPageBlock.value?.blockType === 'AiCrudPage'
      && !selectedPageBlockRuntimeObjectRef.value,
    )
    await refreshWorkspaceMetadata()
    if (shouldBindCurrentCrud) {
      updateSelectedPageBlockRuntimeObject(change.objectId)
      objectSetupVisible.value = false
    }
  }
  __impl.handleApplicationObjectsChanged = handleApplicationObjectsChanged

  async function refreshWorkspaceMetadata(options = {}) {
    const code = route.params.applicationCode
    if (!code)
      return
    const syncBuilder = options.syncBuilder === true
      || (options.syncBuilder !== false && !dirty.value)
    const markClean = options.markClean === true
      || (options.markClean !== false && !dirty.value)
    const response = await businessApplicationWorkspaceByCode(code)
    const workspace = response.data || {}
    application.value = workspace.application || application.value
    objects.value = workspace.objects || []
    workspaceExtensions.value = workspace.extensions || []
    workspaceEntries.value = workspace.entries || []
    // 切 edit 不再整页 load 后，这里必须把 workspace 草稿同步回 builder。
    // 否则页面管理仍渲染内存里未规范化的布局（例如落成默认「提示信息」面板）。
    if (application.value && syncBuilder) {
      builder.value = ensurePageTitleComponents(
        normalizeInAppBuilder(application.value.options, application.value, objects.value),
      )
      resetBuilderHistory(builder.value)
      bindSingleFormToCompatibleBlocks()
    }
    resetRuntimeCrudConfig()
    // 草稿配置已变：通知页面管理 Portal 丢掉本地 CRUD 缓存并重新拉 render
    portalCrudSeed.value = {}
    portalCrudConfigRevision.value += 1
    hydratePageCrudApiPlaceholders()
    // bind/hydrate 可能继续改 builder；签名必须在全部本地归一化之后再落，否则保存后仍显示未保存。
    if (markClean)
      await markBuilderClean()
    else
      await nextTick()
    preloadCurrentPageCrudRuntimeProps()
    // preload 触发的对象上下文加载不改 builder；若仍有同步归一化尾差再对齐一次
    if (markClean && dirty.value)
      await markBuilderClean()
  }
  __impl.refreshWorkspaceMetadata = refreshWorkspaceMetadata
  __impl.switchPageDesignTab = switchPageDesignTab
  __impl.resolveFormAssetIdForPage = resolveFormAssetIdForPage
  __impl.findFirstBlockWithFormAssetId = findFirstBlockWithFormAssetId
  __impl.syncActiveFormAssetForPage = syncActiveFormAssetForPage
  __impl.createFormAssetForCurrentPage = createFormAssetForCurrentPage
  __impl.patchCurrentPageNode = patchCurrentPageNode
  __impl.restoreCurrentObjectPageLayout = restoreCurrentObjectPageLayout
  __impl.selectPageManagementNode = selectPageManagementNode
  __impl.startRenameApplication = startRenameApplication
  __impl.cancelRenameApplication = cancelRenameApplication
  __impl.confirmRenameApplication = confirmRenameApplication
  __impl.enterPageDesign = enterPageDesign
  __impl.enterWorkbenchDesign = enterWorkbenchDesign
  __impl.openCustomPageSelector = openCustomPageSelector
  __impl.openExcelPageImport = openExcelPageImport
  __impl.openFormAssetDesignerForPage = openFormAssetDesignerForPage
  __impl.openDraftPreview = openDraftPreview
  __impl.openObjectResourcePreview = openObjectResourcePreview
  __impl.openObjectDesigner = openObjectDesigner
  __impl.openBusinessObjectDesign = openBusinessObjectDesign
  __impl.openObjectSetup = openObjectSetup
  __impl.handleApplicationObjectsChanged = handleApplicationObjectsChanged
  __impl.refreshWorkspaceMetadata = refreshWorkspaceMetadata

  // Start load only after every part has registered __impl handlers (avoids race with part1 immediate watch).
  watch([
    () => route.params.applicationCode,
    () => route.query.edit,
    () => route.query.draft,
    canEditApplication,
  ], ([, edit]) => {
    editing.value = edit === '1'
    applicationRuntimeLoadCoordinator.run(resolveApplicationRuntimeLoadKey(route, canEditApplication.value))
  }, { immediate: true })

  return {
    ...deps,
  }
}
