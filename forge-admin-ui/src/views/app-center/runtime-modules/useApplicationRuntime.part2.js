/** application-runtime setup part 2. */
import { AddOutline, AppsOutline, ArrowBackOutline, ArrowDownOutline, ArrowRedoOutline, ArrowUndoOutline, ArrowUpOutline, BarChartOutline, CheckboxOutline, CheckmarkDoneOutline, ColorFillOutline, CopyOutline, CreateOutline, CubeOutline, DocumentTextOutline, DuplicateOutline, EllipsisHorizontalOutline, ExpandOutline, EyeOffOutline, EyeOutline, FolderOpenOutline, FunnelOutline, GitBranchOutline, GitNetworkOutline, GridOutline, InformationCircleOutline, ListOutline, MoveOutline, NotificationsOutline, PaperPlaneOutline, PeopleOutline, ReaderOutline, RemoveOutline, ResizeOutline, SaveOutline, SettingsOutline, SquareOutline, StatsChartOutline, SwapHorizontalOutline, TextOutline, TrashOutline } from '@vicons/ionicons5'
import { NIcon, useDialog, useMessage } from 'naive-ui'
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
import {
  buildFreeLayoutPresetGrid,
  freeLayoutPresetCatalog,
  resolveFreeLayoutPreset,
} from '../in-app-builder/free-layout-preset-catalog'
import {
  overlayFormDesignerOnRuntimeCrudProps,
  resolveBoundFormDesignerSchema,
} from '@/components/lowcode-builder/shared/runtime-crud-props'
import { findPageBlockInTree, mapPageBlocksInTree, removePageBlockFromTree, visitPageBlocksInTree } from './page-block-tree'
import { resolvePageBlockShellStyle as computePageBlockShellStyle, readPageBlockLength, resolveDefaultPageBlockHeight, resolveDefaultPageBlockYFromItems, resolvePageBlockFlowGeometry, resolveRootPageBlockCollisions, shouldUsePageFlowStack, normalizePagePadding, resolvePagePaddingCss, normalizePageSurface, migratePageFlowToContentCoords, canvasToContentFlowPoint, contentToCanvasFlowPoint, DEFAULT_PAGE_PADDING, DEFAULT_PAGE_SURFACE } from './page-flow-geometry'
import { flattenNodes, hasPermission, isNavigationVisible } from './runtime-navigation-utils'
import { useBuilderHistory } from './use-builder-history'
import { useRuntimeCrudConfig } from './use-runtime-crud-config'

export function applyApplicationRuntimePart2(deps = {}) {
  const dialog = useDialog()
  const {
    __impl, mut, animatePageBlockSwap, appendPageBlockToContainer, appendPageBlockToGridCell, appendPageBlockToTab, applyDesignerResource, applyNestedPageBlockMove,
    applyPageBlockMove, applyPageBlockSwapPreview, bumpPortalCrudConfigAfterDesignerSave, cancelRenameApplication, clearPageAlignGuides, clearPageBlockSwapPreview, clonePageBlockTree, confirmRenameApplication,
    copySelectedBlockToOtherPage, createFormAssetForCurrentPage, createLegacyBlock, createPageFromTemplate, createQuickNode, currentDesignerDirty, discardAndExitEditing, duplicateNestedPageBlock,
    endComponentButtonMove, endNestedPageBlockMove, endNestedPageBlockResize, endPageBlockMove, endPageBlockResize, ensurePageTitleComponents, enterPageDesign, enterWorkbenchDesign,
    exitToPageManagement, extractNestedPageBlockToCanvas, findFirstBlockWithFormAssetId, finishCatalogPointerDrag, handleApplicationObjectsChanged, handleComponentCatalogClick, handleComponentCatalogDragEnd, handleEmbeddedDesignerSaved,
    handleEmbeddedProcessDesignerSaved, handleExtensionsChanged, handleNestedPageBlockMenuSelect, handleNestedPageBlockMoveStart, handleNestedPageBlockResizeStart, handlePageBlockMoreSelect, handlePageFlowContainerClear, handlePageFlowContainerInsert,
    handlePageFlowDragOver, handlePageFlowDrop, handlePageFlowGridCellDrop, handlePageFlowTabDrop, handlePageTypeSelection, handleProcessPanelNavigate, handleResourceNodeAction, handleRuntimeBack,
    handleRuntimeHeaderMoreSelect, isDraftPreviewMode, isGroupCollapsed, isPageCatalogDrag, itemBorderColorMode, load, markBuilderClean, movePageBlockIntoContainer,
    moveRootPageBlockToGridCell, moveRootPageBlockToTab, normalizePageBlockForContainer, onComponentButtonMove, onNestedPageBlockMove, onPageBlockMove, onPageBlockResize, openBusinessObjectDesign,
    openCustomPageSelector, openDraftPreview, openEmbeddedObjectActions, openExcelPageImport, openFormAssetDesignerForPage, openObjectDesigner, openObjectResourcePreview, openObjectSetup,
    openPageTypeSelector, openProcessDesigner, openWorkspace, pageUsesFreeLayoutCanvas, patchCurrentPageNode, persistApplicationDraft, prefetchRuntimeWorkspacePanels, promoteCurrentPageToListShape,
    provisionPendingFormData, readRouteDesignTab, refreshWorkspaceMetadata, relocateNestedPageBlockToContainer, relocateNestedPageBlockToGridCell, relocateNestedPageBlockToTab, requestExitEditing, resizeNestedPageBlock,
    resolveComponentIcon, resolveComponentPickerGroup, resolveEmptyGuideIcon, resolveEntryDesignTab, resolveFormAssetIdForPage, resolveFormLayoutBlockType, resolvePageBlockBackgroundColor, resolvePageBlockMoreOptions,
    resolvePageBlockMoveIntoOptions, resolvePageBlockSwapTarget, resolvePageContextFieldCatalog, resolvePageDesignTab, resolvePageFlowContainerTargetByPageBlock, resolvePageFlowContainerTargetByRect, resolvePageFlowContainerTargetFromElement, resolvePageFlowContainerTargetFromPoint,
    resolvePageFlowGridCellTarget, resolvePageFlowGridCellTargetByPageBlock, resolvePageFlowGridCellTargetByRect, resolvePageFlowGridCellTargetFromElement, resolvePageFlowGridCellTargetFromPoint, resolvePageFlowTabTarget, resolvePageFlowTabTargetFromPoint, resolvePageShapeKey,
    resolvePageTemplateIcon, resolveResourceNodeBuilderNode, resolveResourceNodeMenuOptions, resolveRuntimeBackLabel, resolveRuntimeBrandEyebrow, resolveRuntimeBrandStatus, resolveRuntimeView, resolveSelectablePageId,
    resolveSystemPageIcon, restoreCurrentObjectPageLayout, returnToEditorFromDraftPreview, saveActiveFormDesigner, saveCurrentDesignerSection, saveDraft, saveNavigationDraft, scheduleNavigationSave,
    selectCreatedDesignerPage, selectDesignerResource, selectIntroTemplate, selectNode, selectPageManagementNode, setFormDataProvisionState, snapPageBlockPosition, startComponentButtonMove,
    startPageBlockMove, startPageBlockResize, startRenameApplication, switchPageDesignTab, switchRuntimeView, syncActiveFormAssetForPage, syncCatalogDropActiveClass, toggleGroupExpanded,
    updatePageAlignGuides, updatePageBlockBackgroundColor, updatePageBlockSize, updateResizeCollisionHighlights, updateSelectedBlockAppearance, warmCurrentPortalPageCrud, route, router,
    message, tenantStore, formComponentIconModules, formComponentIconCache, formComponentIconFileByBlockType, userStore, asyncPanelLoader, ApplicationObjectsPanel,
    ApplicationExtensionsPanel, ApplicationProcessPanel, ApplicationSettingsPanel, runtimeWorkspacePanelImporters, BusinessObjectDesignerPage, BusinessProcessPage, ForgeFormDesigner, ListPageGridDesigner,
    IconSelector, PageTypeSelector, PageDesignSettingsPanel, PageDesignPublishPanel, PageManagementSystemView, GridBlockRenderer, application, objects,
    builder, processSelectableObjects, loadError, loading, editCanvasBootstrapping, saving, restoringObjectPageLayout, editing,
    activePageDesignTab, runtimeViewMode, exitEditingVisible, selectedNodeId, enhanceContextPageId, newNodePopoverVisible, selectedPageTemplateKey, iconPickerVisible, iconPickerNodeId,
    navigationActionVisible, navigationActionMode, navigationActionNodeId, navigationActionForm, systemMenuMountVisible, systemMenuMountNodeId, systemMenuMountNode, componentPopoverVisible,
    componentKeyword, savedSignature, selectedPageBlockId, nestHostPinnedId, draggingPageBlockId, dragPreview, pageAlignGuides, PAGE_ALIGN_SNAP_PX, configPanelVisible,
    inspectorTab, componentButtonPosition, componentButtonMoveCtx, catalogDragBlockType, suppressCatalogClick, activePageFlowTabTarget, activePageFlowGridTarget, activePageFlowContainerTarget,
    resizeCollisionBlockIds, formDesignerObjectContextByObjectId, formDesignerObjectContextLoadingIds, formDesignerMode, designerTransitionLoading, runtimeTreeFilter, runtimeTreeFilterByBlockId, runtimeTreeActiveKeyByBlockId,
    formDesignerFromPageManagement, activeFormAssetId, activePageShapeDesign, pageTypeSelectorVisible, layoutPresetPickerVisible, pageTypeSelectorParentId, pageTypeSelectorDefaultType, sidebarCollapsed, renamingApplication,
    renameApplicationValue, renameSaving, renameInputRef, collapsedGroupIds, copyBlockVisible, copyBlockId, copyBlockTargetPageId, blockBackgroundPickerVisible,
    backgroundPickerBlockId, formAssetSelectorOpen, formAssetSelectorKeyword, formDataProvisioningByAssetId, objectSetupVisible, selectedDesignerResourceKey, workspaceExtensions, workspaceEntries,
    portalCrudConfigRevision, portalCrudSeed, isDraftMode, activeFlowContext, embeddedDesignerRef, embeddedDesignerDirty, embeddedDesignerSaving, embeddedProcessDesignerVisible,
    embeddedProcessDesignerId, applicationRuntimeLoadCoordinator, componentPickerGroupOptions, runtimeHeaderMoreOptions, pageManagementSystemPages, systemPageIconMap, currentSystemPage, workbenchPage,
    systemPageNavigationRoutes, designerResourceGroups, activeDesignerResource, designerSection, pageBuilderResourceActive, canEditApplication, usePortalDesignPreview, activeDesignerObject,
    activeResourceConfigKey, previewableResourceActive, legacyBlockTypeMap, pageBlockResizeAnchors, pageBlockRecommendedColors, groupOptions, navigationActionNode, iconPickerNode,
    navigationIconValue, navigationActionTitle, navigationActionHasChildren, navigationDeleteImpact, navigationDeleteTip, moveGroupOptions, navigationNodes, currentNode,
    currentPage, currentPageLayoutPolluted, currentPageManagementTitle, pageDesignObject, currentGridLayout, pageBlocks, pageFlowStackMode, pageCanvasPadding,
    pageCanvasPaddingCss, pageCanvasSurface, pageCanvasSurfaceStyle, designerGridLayout, appPageTargetOptions, pageFlowHeight, hasCustomComponentButtonPosition, componentButtonStyle, selectedPageBlock, formAssets, filteredFormAssets,
    pageFormAssetOptions, activeFormAsset, activeFormDesignerSchema, activeFormAssetBlock, activeFormDesignerObjectRef, activeFormDesignerContext, activeFormDesignerRelations, activeFormDesignerActions,
    activeFormFields, activeFormDataState, selectedPageBlockFormAssetId, selectedPageBlockFormAsset, selectedPageBlockRuntimeObjectRef, selectedPageBlockIsCrud, selectedPageBlockSupportsDataSource, selectedPageBlockUsesObjectRuntime,
    selectedPageBlockRuntimeObjectId, selectedPageBlockObjectDesignerPanel, selectedPageBlockFormDataState, runtimeObjectFormOptions, pageTemplateOptions, selectedPageBlockFields, selectedPageBlockFormDesignerSchema, applicationGridModelSchema,
    dragPreviewBlock, nestedMovingPageBlockId, copyBlockPageOptions, dirty, canSaveActiveFormDesigner, loadErrorTitle, filteredComponents, componentPickerGroups,
    recommendedComponents,
    runtimeCrudPropsByObjectId, runtimeCrudLoadingObjectIds, runtimeCrudUnavailableObjectIds, loadRuntimeCrudProps, resetRuntimeCrudConfig, historyReady, canUndo, canRedo, resetBuilderHistory,
  
  } = deps
  function createFormAssetForPageCrud(pageId) {
    const page = builder.value?.pages?.[pageId]
    const items = page?.layout?.gridLayout?.items || []
    const crud = items.find(item => item?.blockType === 'AiCrudPage')
    if (!crud)
      return
    const pageNode = builder.value.nodes.find(node => node.id === pageId)
    const name = `${pageNode?.title || '页面'}数据表单`
    const result = createInAppFormAsset(builder.value, {
      name,
      formDesignerSchema: createDefaultFormDesignerSchema({
        objectCode: application.value?.applicationCode || 'application',
        objectName: name,
        formName: name,
      }),
    })
    const resultPage = result.schema.pages?.[pageId]
    const resultItems = resultPage?.layout?.gridLayout?.items || []
    builder.value = {
      ...result.schema,
      pages: {
        ...result.schema.pages,
        [pageId]: {
          ...resultPage,
          layout: {
            ...resultPage.layout,
            gridLayout: {
              ...resultPage.layout.gridLayout,
              items: resultItems.map(item => item.id === crud.id
                ? {
                    ...item,
                    props: {
                      ...(item.props || {}),
                      formAssetId: result.formAssetId,
                      formAssetFieldsInitialized: false,
                    },
                  }
                : item),
            },
          },
        },
      },
    }
    selectedPageBlockId.value = crud.id
    activeFormAssetId.value = result.formAssetId
    formDesignerMode.value = false
    configPanelVisible.value = true
    inspectorTab.value = 'data'
  }
  __impl.createFormAssetForPageCrud = createFormAssetForPageCrud

  function resolveNextNavigationTitle(type = 'page') {
    const prefix = type === 'group' ? '页面组' : '页面'
    const count = (builder.value?.nodes || []).filter(node => node.type === type).length + 1
    return `${prefix} ${count}`
  }
  __impl.resolveNextNavigationTitle = resolveNextNavigationTitle

  function applyPageTemplate(pageId, templateKey, initialBlockType = '') {
    const template = resolveInAppPageTemplate(templateKey)
    const blockTypes = initialBlockType ? [initialBlockType] : template.blockTypes
    const items = blockTypes
      .map((blockType, index) => attachDefaultRuntimeObject(createGridBlock(blockType, { businessName: currentNode.value?.title || '应用页面', fields: [] }, { gridX: 0, gridY: index * 2 })))
      .filter(Boolean)
    const page = builder.value?.pages?.[pageId]
    if (!page)
      return
    builder.value = {
      ...builder.value,
      pages: {
        ...builder.value.pages,
        [pageId]: {
          ...page,
          layout: {
            ...page.layout,
            items: [],
            // 空白页不再被旧页面迁移逻辑补入标题组件。
            pageTitleComponentInitialized: true,
            gridLayout: { cols: 12, rowHeight: 32, gap: 8, designWidth: 1366, layoutType: 'simple-crud', items },
          },
        },
      },
    }
  }
  __impl.applyPageTemplate = applyPageTemplate

  /**
   * 自由布局栅格预设：整页替换为带组件的 grid-layout。
   * @param {string} pageId
   * @param {string} presetKey ops-overview | entry-workbench | analytics-board
   * @param {{ replace?: boolean, pageTitle?: string }} options
   */
  function applyFreeLayoutPreset(pageId, presetKey, options = {}) {
    const page = builder.value?.pages?.[pageId]
    if (!page)
      return false
    const preset = resolveFreeLayoutPreset(presetKey)
    if (!preset)
      return false
    const existingItems = page.layout?.gridLayout?.items || []
    if (existingItems.length && options.replace !== true)
      return false
    const pageTitle = options.pageTitle
      || builder.value?.nodes?.find(node => node.id === pageId)?.title
      || '应用页面'
    const gridLayout = buildFreeLayoutPresetGrid(presetKey, { pageTitle })
    if (!gridLayout)
      return false
    builder.value = {
      ...builder.value,
      pages: {
        ...builder.value.pages,
        [pageId]: {
          ...page,
          layout: {
            ...page.layout,
            items: [],
            pageTitleComponentInitialized: true,
            appliedLayoutPreset: preset.key,
            gridLayout,
          },
        },
      },
    }
    selectedPageBlockId.value = gridLayout.items?.[0]?.id || ''
    return true
  }
  __impl.applyFreeLayoutPreset = applyFreeLayoutPreset
  __impl.freeLayoutPresetCatalog = freeLayoutPresetCatalog

  function applyCurrentFreeLayoutPreset(presetKey, options = {}) {
    const pageId = currentNode.value?.id || selectedNodeId.value
    if (!pageId) {
      message.warning('请先选择页面')
      return false
    }
    const hasBlocks = (pageBlocks.value || []).length > 0
    const run = () => {
      const ok = applyFreeLayoutPreset(pageId, presetKey, {
        replace: true,
        pageTitle: currentNode.value?.title || '',
      })
      if (ok) {
        message.success('已应用布局预设，可继续调整数据源')
        layoutPresetPickerVisible.value = false
      }
      else {
        message.warning('应用布局预设失败')
      }
      return ok
    }
    if (hasBlocks && options.skipConfirm !== true) {
      const preset = resolveFreeLayoutPreset(presetKey)
      dialog.warning({
        title: '替换当前页面布局？',
        content: `将清空画布上的组件，并应用「${preset?.label || '布局预设'}」。此操作可再手动调整，但当前组件会被覆盖。`,
        positiveText: '替换并应用',
        negativeText: '取消',
        onPositiveClick: () => {
          run()
        },
      })
      return false
    }
    return run()
  }
  __impl.applyCurrentFreeLayoutPreset = applyCurrentFreeLayoutPreset
  __impl.openLayoutPresetPicker = () => {
    layoutPresetPickerVisible.value = true
  }

  function resolveNavigationMoreOptions(node) {
    const siblings = (builder.value?.nodes || []).filter(item => item.parentId === node.parentId).sort((a, b) => a.sort - b.sort)
    const index = siblings.findIndex(item => item.id === node.id)
    return [
      ...(
        node.type === 'group'
          ? [
              { label: '在本组新建页面', key: 'create-page', icon: () => renderNavigationMenuIcon(AddOutline) },
              { label: '在本组新建页面组', key: 'create-group', icon: () => renderNavigationMenuIcon(FolderOpenOutline) },
              { type: 'divider', key: 'create-divider' },
            ]
          : []
      ),
      { label: '重命名', key: 'rename', icon: () => renderNavigationMenuIcon(CreateOutline) },
      { label: '更改图标', key: 'icon', icon: () => renderNavigationMenuIcon(ColorFillOutline) },
      { label: isNavigationVisible(node) ? '隐藏菜单' : '显示菜单', key: 'toggle-visible', icon: () => renderNavigationMenuIcon(isNavigationVisible(node) ? EyeOutline : EyeOffOutline) },
      ...(
        node.type === 'page'
          ? [{
              label: isPageSystemMenuMounted(node) ? '修改系统菜单挂载' : '挂载到系统菜单',
              key: 'system-menu-mount',
              icon: () => renderNavigationMenuIcon(GridOutline),
            }]
          : []
      ),
      { label: '复制', key: 'duplicate', icon: () => renderNavigationMenuIcon(CopyOutline) },
      { label: '移动至', key: 'move', icon: () => renderNavigationMenuIcon(MoveOutline) },
      { type: 'divider', key: 'move-divider' },
      { label: '上移', key: 'move-up', disabled: index <= 0, icon: () => renderNavigationMenuIcon(ArrowUpOutline) },
      { label: '下移', key: 'move-down', disabled: index < 0 || index >= siblings.length - 1, icon: () => renderNavigationMenuIcon(ArrowDownOutline) },
      { type: 'divider', key: 'danger-divider' },
      { label: '删除', key: 'delete', icon: () => renderNavigationMenuIcon(TrashOutline) },
    ]
  }
  __impl.resolveNavigationMoreOptions = resolveNavigationMoreOptions

  function renderNavigationMenuIcon(icon) {
    return h(NIcon, { size: 16, class: 'navigation-menu-icon' }, { default: () => h(icon) })
  }
  __impl.renderNavigationMenuIcon = renderNavigationMenuIcon

  function handleNavigationMoreSelect(key, node) {
    if (key === 'create-page' || key === 'create-group') {
      createQuickNode(key === 'create-group' ? 'group' : 'page', node.id)
      return
    }
    if (key === 'move-up' || key === 'move-down') {
      moveNavigationByOffset(node, key === 'move-up' ? -1 : 1)
      return
    }
    if (key === 'toggle-visible') {
      toggleNavigationVisible(node)
      return
    }
    if (key === 'system-menu-mount') {
      openSystemMenuMountDialog(node)
      return
    }
    if (key === 'duplicate') {
      duplicateNavigationNode(node)
      return
    }
    if (key === 'icon') {
      iconPickerNodeId.value = node.id
      iconPickerVisible.value = true
      nextTick(() => document.querySelector('.navigation-icon-picker .icon-selector button')?.click())
      return
    }
    navigationActionNodeId.value = node.id
    navigationActionMode.value = key
    navigationActionForm.value = {
      title: node.title || '',
      icon: node.icon || '',
      parentId: node.parentId || null,
      deleteStrategy: 'delete-children',
      targetParentId: null,
    }
    navigationActionVisible.value = true
  }
  __impl.handleNavigationMoreSelect = handleNavigationMoreSelect

  function toggleNavigationVisible(node) {
    const isVisible = isNavigationVisible(node)
    builder.value = {
      ...builder.value,
      nodes: builder.value.nodes.map(item => item.id === node.id
        ? { ...item, navigationVisible: !isVisible }
        : item),
    }
    scheduleNavigationSave()
  }
  __impl.toggleNavigationVisible = toggleNavigationVisible

  function openSystemMenuMountDialog(node) {
    if (!node || node.type !== 'page')
      return
    systemMenuMountNodeId.value = node.id
    systemMenuMountVisible.value = true
  }
  __impl.openSystemMenuMountDialog = openSystemMenuMountDialog

  function patchNavigationNodeMount(nodeId, partial = {}) {
    if (!nodeId || !builder.value)
      return
    builder.value = {
      ...builder.value,
      nodes: builder.value.nodes.map((item) => {
        if (item.id !== nodeId)
          return item
        return applyPageMountFieldsToNode(item, partial)
      }),
    }
    scheduleNavigationSave()
  }
  __impl.patchNavigationNodeMount = patchNavigationNodeMount

  function confirmSystemMenuMount(partial = {}) {
    const nodeId = systemMenuMountNodeId.value
    if (!nodeId)
      return
    patchNavigationNodeMount(nodeId, {
      ...partial,
      systemMenuVisible: true,
    })
    systemMenuMountVisible.value = false
    message.success('系统菜单挂载已写入草稿，应用发布后生效')
  }
  __impl.confirmSystemMenuMount = confirmSystemMenuMount

  function unmountSystemMenu() {
    const nodeId = systemMenuMountNodeId.value
    if (!nodeId)
      return
    patchNavigationNodeMount(nodeId, { systemMenuVisible: false })
    systemMenuMountVisible.value = false
    message.success('已取消系统菜单挂载（应用发布后生效）')
  }
  __impl.unmountSystemMenu = unmountSystemMenu

  function duplicateNavigationNode(node) {
    const copyId = `${node.type}_${Date.now()}`
    const copy = {
      ...JSON.parse(JSON.stringify(node)),
      id: copyId,
      title: `${node.title} 副本`,
      sort: Number(node.sort || 0) + 1,
    }
    builder.value = {
      ...builder.value,
      nodes: [...builder.value.nodes, copy],
      pages: node.type === 'page' && builder.value.pages[node.id]
        ? { ...builder.value.pages, [copyId]: JSON.parse(JSON.stringify(builder.value.pages[node.id])) }
        : builder.value.pages,
    }
    if (copy.type === 'page')
      selectNode(copy.id)
    scheduleNavigationSave()
  }
  __impl.duplicateNavigationNode = duplicateNavigationNode

  function moveNavigationByOffset(node, offset) {
    const siblings = (builder.value?.nodes || []).filter(item => item.parentId === node.parentId).sort((a, b) => a.sort - b.sort)
    const index = siblings.findIndex(item => item.id === node.id)
    if (index < 0 || index + offset < 0 || index + offset >= siblings.length)
      return
    try {
      builder.value = moveNavigationNode(builder.value, node.id, node.parentId, offset < 0 ? index - 1 : index + 1)
      scheduleNavigationSave()
    }
    catch (error) {
      message.error(error?.message || '页面排序失败')
    }
  }
  __impl.moveNavigationByOffset = moveNavigationByOffset

  function confirmNavigationAction() {
    const node = navigationActionNode.value
    if (!node)
      return
    try {
      if (navigationActionMode.value === 'rename') {
        const title = navigationActionForm.value.title.trim()
        if (!title) {
          message.warning('请输入名称')
          return
        }
        builder.value = {
          ...builder.value,
          nodes: builder.value.nodes.map(item => item.id === node.id ? { ...item, title } : item),
          pages: node.type === 'page'
            ? { ...builder.value.pages, [node.id]: { ...builder.value.pages[node.id], title } }
            : builder.value.pages,
        }
      }
      if (navigationActionMode.value === 'icon') {
        builder.value = {
          ...builder.value,
          nodes: builder.value.nodes.map(item => item.id === node.id ? { ...item, icon: navigationActionForm.value.icon.trim() } : item),
        }
      }
      if (navigationActionMode.value === 'move')
        builder.value = moveNavigationNode(builder.value, node.id, navigationActionForm.value.parentId)
      if (navigationActionMode.value === 'delete') {
        const strategy = navigationActionHasChildren.value
          ? {
              type: navigationActionForm.value.deleteStrategy,
              targetParentId: navigationActionForm.value.targetParentId,
            }
          : undefined
        if (strategy?.type === 'move-children' && !strategy.targetParentId && strategy.targetParentId !== null) {
          message.warning('请选择子项移动目标')
          return
        }
        builder.value = removeNavigationNode(builder.value, node.id, strategy)
        if (!builder.value.nodes.some(item => item.id === selectedNodeId.value))
          selectNode(builder.value.homePageId)
        // 资源树选中项可能指向已删除页面，回落到首页节点，避免设计区停留在空资源。
        if (String(activeDesignerResource.value?.pageId || '') === String(node.id)) {
          applyDesignerResource({
            key: `page-custom:${String(builder.value.homePageId)}`,
            kind: 'page-custom',
            pageId: String(builder.value.homePageId),
          })
        }
      }
      navigationActionVisible.value = false
      scheduleNavigationSave()
    }
    catch (error) {
      message.error(error?.message || '页面操作失败')
    }
  }
  __impl.confirmNavigationAction = confirmNavigationAction

  function isNavigationGroupDescendant(candidateId, nodeId) {
    if (!candidateId || !nodeId)
      return false
    const nodes = builder.value?.nodes || []
    let current = nodes.find(item => item.id === candidateId)
    const visited = new Set()
    while (current?.parentId && !visited.has(current.id)) {
      if (current.parentId === nodeId)
        return true
      visited.add(current.id)
      current = nodes.find(item => item.id === current.parentId)
    }
    return false
  }
  __impl.isNavigationGroupDescendant = isNavigationGroupDescendant

  function insertComponent(component) {
    const blockType = component?.blockType
    if (!blockType)
      return
    // 只有画布主动点选的容器才作为落入目标（货架刚加出的容器不算）
    const pinnedId = String(nestHostPinnedId.value || '').trim()
    const selected = pinnedId
      ? findPageBlockInTree(pageBlocks.value, pinnedId)
      : null
    const hostId = selected?.id || ''
    let nested = false
    if (selected && ['card', 'box-layout'].includes(selected.blockType)) {
      nested = !!__impl.appendPageBlockToContainer?.(blockType, hostId)
    }
    else if (selected?.blockType === 'tabs') {
      const tabKey = selected.props?.activeTabKey
        || selected.props?.tabs?.[0]?.key
        || ''
      if (tabKey) {
        const before = JSON.stringify(pageBlocks.value)
        __impl.appendPageBlockToTab?.(blockType, hostId, tabKey)
        nested = before !== JSON.stringify(pageBlocks.value)
      }
    }
    else if (selected?.blockType === 'grid-layout') {
      const before = JSON.stringify(pageBlocks.value)
      __impl.appendPageBlockToGridCell?.(blockType, hostId, '', 0)
      nested = before !== JSON.stringify(pageBlocks.value)
    }
    if (!nested)
      appendPageBlock(blockType)
    // 连续添加：保持弹层打开并清空搜索；嵌套时保持画布点选宿主方便连加
    componentKeyword.value = ''
    if (nested && hostId) {
      selectedPageBlockId.value = hostId
      nestHostPinnedId.value = hostId
    }
    nextTick(() => {
      componentPopoverVisible.value = true
    })
  }
  __impl.insertComponent = insertComponent

  function updateCurrentGridLayout(gridLayout) {
    const pageId = resolveActiveDesignerPageId()
    if (!pageId || !builder.value)
      return
    let page = builder.value.pages?.[pageId]
    if (!page && isWorkbenchPageId(pageId)) {
      const ensured = ensureWorkbenchPageInBuilder(builder.value)
      builder.value = ensured.schema
      page = ensured.page
    }
    if (!page)
      return
    const previous = page.layout?.gridLayout && typeof page.layout.gridLayout === 'object'
      ? page.layout.gridLayout
      : {}
    const nextLayout = {
      ...previous,
      ...(gridLayout && typeof gridLayout === 'object' ? gridLayout : {}),
    }
    // 属性面板回写整份 layout 时可能丢掉 pagePadding / pageSurface，这里强制保留。
    if (nextLayout.pagePadding == null && previous.pagePadding != null)
      nextLayout.pagePadding = previous.pagePadding
    if (nextLayout.pageSurface == null && previous.pageSurface != null)
      nextLayout.pageSurface = previous.pageSurface
    // 与当前布局深相等时跳过，避免属性面板回声写入污染撤销栈
    if (JSON.stringify(previous) === JSON.stringify(nextLayout))
      return
    builder.value = {
      ...builder.value,
      pages: {
        ...builder.value.pages,
        [pageId]: {
          ...page,
          layout: { ...page.layout, items: [], gridLayout: nextLayout },
        },
      },
    }
  }
  __impl.updateCurrentGridLayout = updateCurrentGridLayout

  function ensurePageFlowContentCoords(layout = {}, pad = pageCanvasPadding.value) {
    if (pageFlowStackMode.value || layout?.pageFlowCoordSpace === 'content')
      return layout
    return migratePageFlowToContentCoords(layout, pad)
  }
  __impl.ensurePageFlowContentCoords = ensurePageFlowContentCoords

  function updatePageCanvasPadding(partial = {}) {
    const previous = pageCanvasPadding.value
    const next = normalizePagePadding({
      ...previous,
      ...(partial && typeof partial === 'object' ? partial : {}),
    })
    // 绝对自由布局：先迁到内容区坐标，再改 padding；上下左右都即时生效，无需再平移 X/Y。
    let layout = { ...(currentGridLayout.value || {}) }
    if (!pageFlowStackMode.value)
      layout = ensurePageFlowContentCoords(layout, previous)
    layout = { ...layout, pagePadding: next }
    updateCurrentGridLayout(layout)
  }
  __impl.updatePageCanvasPadding = updatePageCanvasPadding

  function updatePageCanvasSurface(partial = {}) {
    const previous = pageCanvasSurface.value
    const next = normalizePageSurface({
      ...previous,
      ...(partial && typeof partial === 'object' ? partial : {}),
    })
    const layout = {
      ...(currentGridLayout.value || {}),
      pageSurface: next,
    }
    updateCurrentGridLayout(layout)
  }
  __impl.updatePageCanvasSurface = updatePageCanvasSurface

  function resetPageCanvasSurface() {
    updatePageCanvasSurface({ ...DEFAULT_PAGE_SURFACE })
  }
  __impl.resetPageCanvasSurface = resetPageCanvasSurface

  /** 编辑态写画布时锁定目标页，避免 currentNode 漂移把组件写到首页 */
  function resolveActiveDesignerPageId() {
    const routeId = String(route.query.pageId || '').trim()
    const selectedId = String(selectedNodeId.value || '').trim()
    if (editing.value && routeId)
      return routeId
    return selectedId || routeId
  }
  __impl.resolveActiveDesignerPageId = resolveActiveDesignerPageId

  function appendPageBlock(blockType) {
    const meta = resolveListPageBlockMeta(blockType)
    if (!meta)
      return
    if (meta.unique && pageBlocks.value.some(block => block.blockType === blockType)) {
      message.info(`${meta.title} 每个页面只能添加一个`)
      return
    }
    const nextY = resolveNextPageBlockStackY(pageBlocks.value)
    let block = createGridBlock(blockType, applicationGridModelSchema.value, {
      gridX: 0,
      gridY: pageBlocks.value.length * 2,
    })
    if (!block)
      return
    block = attachDefaultRuntimeObject(block)
    block = attachSingleFormAsset(block)
    // 优先保留 createGridBlock / props 里已声明的高度，避免图表等被默认 96 压扁后溢出叠层
    const schemaHeight = readPageBlockLength(block.props?.style?.height)
    const propHeight = Number(block.props?.height)
    const height = Math.max(
      resolveDefaultPageBlockHeight(block),
      schemaHeight,
      Number.isFinite(propHeight) && propHeight > 0 ? Math.round(propHeight) : 0,
    )
    // 内容区相对坐标：通栏 X=0，Y 从内容区顶向下堆叠
    if (!pageFlowStackMode.value)
      updateCurrentGridLayout(ensurePageFlowContentCoords({ ...(currentGridLayout.value || {}) }))
    block = {
      ...block,
      props: {
        ...(block.props || {}),
        style: {
          ...(block.props?.style || {}),
          pageFlowX: 0,
          pageFlowY: Math.max(0, nextY),
          pageFlowHeight: height,
          widthMode: block.props?.style?.widthMode || 'full',
          heightMode: block.props?.style?.heightMode
            || (['grid-layout', 'card', 'box-layout', 'tabs'].includes(blockType) ? 'auto' : 'fixed'),
          width: '100%',
          height,
        },
      },
    }
    updatePageBlocks([...pageBlocks.value, block], { resolveCollisions: true, changedBlockId: block.id })
    selectedPageBlockId.value = block.id
    preloadPageBlockCrudRuntimeProps(block)
    // 新块堆在画布底部时若不滚入视口，用户会以为「没加上」
    scrollPageBlockIntoView(block.id)
  }
  __impl.appendPageBlock = appendPageBlock

  /** 自由布局：把指定块滚进 .page-surface 可视区（添加/嵌套后尤其必要） */
  function scrollPageBlockIntoView(blockId) {
    const id = String(blockId || '').trim()
    if (!id)
      return
    const run = () => {
      const el = document.querySelector(`[data-page-block-id="${id}"]`)
      if (!el)
        return
      // 组件弹层打开时原生 scrollIntoView 常滚错容器或被挡住，显式滚 .page-surface
      const surface = el.closest('.page-surface') || document.querySelector('.page-surface')
      if (surface) {
        const er = el.getBoundingClientRect()
        const sr = surface.getBoundingClientRect()
        const pad = 24
        if (er.top < sr.top + pad)
          surface.scrollTop += er.top - sr.top - pad
        else if (er.bottom > sr.bottom - pad)
          surface.scrollTop += er.bottom - sr.bottom + pad
        return
      }
      el.scrollIntoView?.({ behavior: 'smooth', block: 'nearest', inline: 'nearest' })
    }
    nextTick(() => {
      requestAnimationFrame(() => {
        run()
        // 布局高度可能晚一帧才稳定（富文本/日历等）
        window.setTimeout(run, 80)
      })
    })
  }
  __impl.scrollPageBlockIntoView = scrollPageBlockIntoView

  /** 加号新增：始终落在现有根级组件下方，自上而下排列 */
  function resolveNextPageBlockStackY(items = []) {
    const gap = 16
    const topPadding = 20
    if (!items.length)
      return topPadding
    return items.reduce((bottom, item, index) => {
      const style = item?.props?.style || {}
      const y = Number.isFinite(Number(style.pageFlowY)) && Number(style.pageFlowY) >= 0
        ? Number(style.pageFlowY)
        : resolveDefaultPageBlockYFromItems(items, index)
      const height = Number(style.pageFlowHeight) > 0
        ? Number(style.pageFlowHeight)
        : resolveDefaultPageBlockHeight(item)
      return Math.max(bottom, y + height + gap)
    }, topPadding)
  }
  __impl.resolveNextPageBlockStackY = resolveNextPageBlockStackY

  function supportsFormAsset(block = {}) {
    return DATA_FIELD_BLOCK_TYPES.includes(block.blockType)
  }
  __impl.supportsFormAsset = supportsFormAsset

  function bindSingleFormToCompatibleBlocks() {
    if (formAssets.value.length !== 1 || !builder.value)
      return
    const nextBuilder = {
      ...builder.value,
      pages: Object.fromEntries(Object.entries(builder.value.pages || {}).map(([pageId, page]) => {
        const items = page?.layout?.gridLayout?.items
        if (!Array.isArray(items))
          return [pageId, page]
        return [pageId, {
          ...page,
          layout: {
            ...page.layout,
            gridLayout: {
              ...page.layout.gridLayout,
              items: items.map(item => attachSingleFormAsset(item)),
            },
          },
        }]
      })),
    }
    if (JSON.stringify(nextBuilder) === JSON.stringify(builder.value))
      return
    builder.value = nextBuilder
  }
  __impl.bindSingleFormToCompatibleBlocks = bindSingleFormToCompatibleBlocks

  function attachSingleFormAsset(block = {}) {
    if (formAssets.value.length !== 1 || !supportsFormAsset(block))
      return block
    const asset = formAssets.value[0]
    if (block.props?.formAssetId && block.props.formAssetId !== asset.id)
      return block
    const fieldRefs = resolveFormAssetFields(asset).map(field => field.fieldCode)
    const synced = syncFormBoundFieldRefs({
      formFieldCodes: fieldRefs,
      searchFieldRefs: block.props?.searchFieldRefs,
    })
    const currentSettings = block.props?.fieldSettings || {}
    const allBoundFieldsHidden = synced.fieldRefs.length > 0 && synced.fieldRefs.every(field => currentSettings[field]?.visible === false)
    return {
      ...block,
      fieldRefs: synced.fieldRefs,
      props: {
        ...(block.props || {}),
        formAssetId: asset.id,
        formAssetFieldsInitialized: synced.fieldRefs.length > 0,
        fieldSettings: createFormFieldVisibilitySettings(currentSettings, synced.fieldRefs, !block.props?.formAssetId || allBoundFieldsHidden),
        ...(['AiCrudPage', 'search-form'].includes(block.blockType)
          ? { searchFieldRefs: synced.searchFieldRefs }
          : {}),
      },
    }
  }
  __impl.attachSingleFormAsset = attachSingleFormAsset

  function resolveFormAssetFields(asset = {}) {
    const schema = normalizeFormDesignerSchema(asset.formDesignerSchema || {})
    const fieldsByCode = new Map(buildAutoFieldAssets(schema).fields.map(field => [field.fieldCode || field.field, field]))
    const widgetNodes = []
    const childFields = []
    const appendComponentFields = (components = []) => {
      ;(Array.isArray(components) ? components : []).forEach((component, index) => {
        const fieldCode = component?.fieldBinding?.fieldCode || component?.field || ''
        const componentKey = component?.componentKey || component?.type || ''
        if (componentKey === 'subTable') {
          const relationKey = String(component?.props?.relationKey || fieldCode || component?.id || '').trim()
          const header = String(component?.props?.header || component?.label || relationKey || '子表').trim()
          const columns = Array.isArray(component?.props?.columns) ? component.props.columns : []
          columns.forEach((column, columnIndex) => {
            const childCode = String(column?.fieldCode || column?.field || column?.key || column?.value || '').trim()
            if (!childCode || !relationKey)
              return
            childFields.push({
              field: `${relationKey}__${childCode}`,
              fieldCode: `${relationKey}__${childCode}`,
              sourceField: childCode,
              label: column.label || column.title || childCode,
              fieldName: column.label || column.title || childCode,
              modelCode: relationKey,
              modelName: header,
              sourceLabel: header,
              fieldScope: 'child',
              listVisible: true,
              formVisible: true,
              fieldStatus: 'ENABLED',
              systemField: false,
              dataType: column.dataType || column.fieldType || 'STRING',
              _order: columnIndex,
            })
          })
        }
        else if (component?.fieldBinding?.mode === 'virtual' && componentKey && isPageWidgetComponentKey(componentKey)) {
          widgetNodes.push({
            field: fieldCode || component?.id || `widget_${componentKey}_${index}`,
            label: component.label || componentKey,
            componentKey,
            type: componentKey,
            nodeType: 'widget',
            span: component.layout?.span ?? component.span ?? 1,
            props: component.props || {},
            fieldBinding: { mode: 'virtual' },
            showLabel: false,
            showFeedback: false,
            formVisible: true,
            listVisible: false,
          })
        }
        else if (component?.fieldBinding?.mode !== 'virtual' && isFieldComponent(component) && fieldCode && !fieldsByCode.has(fieldCode)) {
          fieldsByCode.set(fieldCode, createFieldFromComponent(component, index))
        }
        appendComponentFields(component?.children || [])
      })
    }
    appendComponentFields(schema.components)
    const dataFields = [...fieldsByCode.values()].map((field, index) => ({
      ...field,
      field: field.field || field.fieldCode || field.fieldBinding?.fieldCode,
      fieldCode: field.fieldCode || field.field || field.fieldBinding?.fieldCode,
      sourceField: field.sourceField || field.field || field.fieldCode || field.fieldBinding?.fieldCode,
      fieldName: field.fieldName || field.label || field.fieldCode || `字段 ${index + 1}`,
      label: field.fieldName || field.label || field.fieldCode || `字段 ${index + 1}`,
      listVisible: field.listVisible !== false,
      formVisible: field.formVisible !== false,
      fieldStatus: field.fieldStatus || 'ENABLED',
      systemField: Boolean(field.systemField),
    })).filter(field => field.fieldCode && field.field)
    return [...dataFields, ...childFields, ...widgetNodes]
  }
  __impl.resolveFormAssetFields = resolveFormAssetFields

  function resolvePageBlockFields(block = {}) {
    const runtimeFields = resolvePageBlockRuntimeCrudProps(block)?.fieldCatalog || []
    const formAssetId = block?.props?.formAssetId || (formAssets.value.length === 1 ? formAssets.value[0]?.id : '')
    const asset = formAssets.value.find(item => item.id === formAssetId)
    const formFields = asset ? resolveFormAssetFields(asset) : []
    return mergePageFieldCatalogs(formFields, runtimeFields)
  }
  __impl.resolvePageBlockFields = resolvePageBlockFields

  /**
   * PortalPageRenderer 专用：仅解析区块关联的表单资产字段（含 widget 虚拟组件）。
   * 与 resolvePageBlockFields 不同，这里不合并运行时 CRUD 字段，
   * 因为 PortalPageRenderer 内部已自行加载运行时配置，并在 resolveBlockFields 中做合并。
   */
  function resolvePortalFormFields(block = {}) {
    const formAssetId = block?.props?.formAssetId || (formAssets.value.length === 1 ? formAssets.value[0]?.id : '')
    if (!formAssetId)
      return []
    const asset = formAssets.value.find(item => item.id === formAssetId)
    return asset ? resolveFormAssetFields(asset) : []
  }
  __impl.resolvePortalFormFields = resolvePortalFormFields

  /**
   * 应用页不是让业务人员再次填写接口地址的地方：
   * - 业务对象页天然绑定当前对象；
   * - 单对象应用无需选择，直接绑定唯一对象；
   * - 多对象的普通内容页不猜测数据源，仍保持安全的静态预览。
   */
  function resolvePageBlockObjectRef(block = {}, pageNode = currentNode.value) {
    const blockRef = block?.props?.objectRef || block?.props?.businessObjectRef
    // 业务应用存在多个关联对象时，未显式选择数据源的页面组件默认复用主对象。
    // 这是列表设计器原本的默认行为，不能退回成“当前配置”这种不可运行占位值。
    const primaryObject = objects.value.find(item => String(item.objectRole || '').toUpperCase() === 'PRIMARY')
    const candidate = blockRef || pageNode?.objectRef || primaryObject || (objects.value.length === 1 ? objects.value[0] : null)
    if (!candidate)
      return null
    const objectId = candidate.objectId ?? candidate.id
    const objectCode = candidate.objectCode || ''
    if (objectId === undefined || objectId === null || objectId === '') {
      const matched = objects.value.find(item => objectCode && String(item.objectCode || '') === String(objectCode))
      if (!matched)
        return null
      return {
        ...matched,
        ...candidate,
        objectId: matched.objectId ?? matched.id,
        valid: candidate.valid !== false && matched.valid !== false,
      }
    }
    const matched = objects.value.find(item => String(item.objectId ?? item.id ?? '') === String(objectId))
    return {
      ...(matched || {}),
      ...candidate,
      objectId: String(objectId),
      objectCode: matched?.objectCode || objectCode || '',
      valid: Boolean(matched) && candidate.valid !== false && matched.valid !== false,
    }
  }
  __impl.resolvePageBlockObjectRef = resolvePageBlockObjectRef

  function isValidPageBlockObjectRef(objectRef) {
    return objectRef?.valid !== false
      && Boolean(objectRef?.objectId ?? objectRef?.id ?? objectRef?.objectCode)
  }
  __impl.isValidPageBlockObjectRef = isValidPageBlockObjectRef

  function isPageBlockDataSourceConfigured(block = {}) {
    return isValidPageBlockObjectRef(resolvePageBlockObjectRef(block))
  }
  __impl.isPageBlockDataSourceConfigured = isPageBlockDataSourceConfigured

  /**
   * 早期应用页创建的区块会把列表设计器的无上下文占位值持久化为
   * `/ai/crud/当前配置`。加载时按每页实际绑定的业务对象完成一次迁移，
   * 让右侧属性面板与真实运行接口都展示同一个 configKey。
   */
  function hydratePageCrudApiPlaceholders() {
    if (!builder.value?.pages || !builder.value?.nodes)
      return
    const nodeById = new Map(builder.value.nodes.map(node => [node.id, node]))
    let changed = false
    const pages = Object.fromEntries(Object.entries(builder.value.pages).map(([pageId, page]) => {
      const items = page?.layout?.gridLayout?.items
      if (!Array.isArray(items))
        return [pageId, page]
      const pageNode = nodeById.get(pageId)
      const nextItems = items.map((block) => {
        if (block?.blockType !== 'AiCrudPage')
          return block
        const objectRef = resolvePageBlockObjectRef(block, pageNode)
        const configKey = isValidPageBlockObjectRef(objectRef) ? objectRef.configKey || '' : ''
        if (!configKey)
          return block
        const serializedProps = JSON.stringify(block.props || {})
        const nextSerializedProps = serializedProps.replaceAll('/ai/crud/当前配置', `/ai/crud/${configKey}`)
        if (nextSerializedProps === serializedProps)
          return block
        changed = true
        return { ...block, props: JSON.parse(nextSerializedProps) }
      })
      if (!nextItems.some((item, index) => item !== items[index]))
        return [pageId, page]
      return [pageId, {
        ...page,
        layout: {
          ...page.layout,
          gridLayout: { ...page.layout.gridLayout, items: nextItems },
        },
      }]
    }))
    if (changed) {
      const nextBuilder = { ...builder.value, pages }
      if (JSON.stringify(nextBuilder) !== JSON.stringify(builder.value))
        builder.value = nextBuilder
    }
  }
  __impl.hydratePageCrudApiPlaceholders = hydratePageCrudApiPlaceholders

  function resolveRuntimeObjectCacheKey(objectRef) {
    if (!isValidPageBlockObjectRef(objectRef))
      return ''
    return String(objectRef.objectId ?? objectRef.id ?? objectRef.objectCode ?? '').trim()
  }
  __impl.resolveRuntimeObjectCacheKey = resolveRuntimeObjectCacheKey

  function attachDefaultRuntimeObject(block = {}) {
    if (!isDataFieldBlockType(block.blockType) || block.props?.objectRef || block.props?.businessObjectRef)
      return block
    const objectRef = resolvePageBlockObjectRef(block)
    if (!objectRef)
      return block
    return {
      ...block,
      props: {
        ...(block.props || {}),
        objectRef: {
          objectId: String(objectRef.objectId ?? objectRef.id),
          objectCode: objectRef.objectCode || '',
          objectName: objectRef.objectName || '',
        },
      },
    }
  }
  __impl.attachDefaultRuntimeObject = attachDefaultRuntimeObject

  function resolveBlockBoundFormDesignerSchema(block = {}) {
    return resolveBoundFormDesignerSchema(block, formAssets.value)
  }

  function resolvePageBlockRuntimeCrudProps(block = {}) {
    if (!isDataFieldBlockType(block.blockType) && block.blockType !== 'tree-panel')
      return null
    const objectRef = resolvePageBlockObjectRef(block)
    if (!isValidPageBlockObjectRef(objectRef))
      return null
    const cacheKey = resolveRuntimeObjectCacheKey(objectRef)
    if (!cacheKey)
      return null
    if (!runtimeCrudPropsByObjectId.value[cacheKey])
      preloadPageBlockCrudRuntimeProps(block)
    const runtimeProps = runtimeCrudPropsByObjectId.value[cacheKey] || null
    if (!runtimeProps)
      return null
    const treePanelProps = findTreePanelProps(pageBlocks.value) || {}
    const searchSchema = alignSearchSchemaWithLeftTree(runtimeProps.searchSchema, {
      treePanelProps,
      runtimeProps,
    })
    let alignedProps = searchSchema === runtimeProps.searchSchema
      ? runtimeProps
      : { ...runtimeProps, searchSchema }
    // 页面资源绑定的表单覆盖新增/编辑布局（否则一直用对象旧 formDesignerSchema）
    alignedProps = overlayFormDesignerOnRuntimeCrudProps(
      alignedProps,
      resolveBlockBoundFormDesignerSchema(block),
    ) || alignedProps
    const blockId = String(block?.id || '').trim()
    const treeFilter = blockId ? runtimeTreeFilterByBlockId.value[blockId] : null
    const pageTreeFilter = runtimeTreeFilter.value
    const activeTreeFilter = treeFilter && Object.keys(treeFilter).length ? treeFilter : pageTreeFilter
    if (!activeTreeFilter || !Object.keys(activeTreeFilter).length)
      return alignedProps
    return {
      ...alignedProps,
      publicParams: {
        ...(alignedProps.publicParams || {}),
        ...activeTreeFilter,
      },
    }
  }
  __impl.resolvePageBlockRuntimeCrudProps = resolvePageBlockRuntimeCrudProps

  function isPageBlockRuntimeCrudLoading(block = {}) {
    if (!isDataFieldBlockType(block.blockType) && block.blockType !== 'tree-panel')
      return false
    const objectRef = resolvePageBlockObjectRef(block)
    if (!isValidPageBlockObjectRef(objectRef))
      return false
    const cacheKey = resolveRuntimeObjectCacheKey(objectRef)
    if (!cacheKey)
      return false
    if (!runtimeCrudPropsByObjectId.value[cacheKey] && !runtimeCrudUnavailableObjectIds.has(cacheKey))
      preloadPageBlockCrudRuntimeProps(block)
    return runtimeCrudLoadingObjectIds.has(cacheKey)
  }
  __impl.isPageBlockRuntimeCrudLoading = isPageBlockRuntimeCrudLoading

  function preloadCurrentPageCrudRuntimeProps() {
    // 运行态（非编辑）页面由 PortalPageRenderer 自行加载渲染配置，
    // 这里再预加载会重复请求同一接口；编辑器画布才消费 runtimeCrudPropsByObjectId
    if (!editing.value)
      return
    visitPageBlocksInTree(pageBlocks.value, preloadPageBlockCrudRuntimeProps)
  }
  __impl.preloadCurrentPageCrudRuntimeProps = preloadCurrentPageCrudRuntimeProps

  function preloadPageBlockCrudRuntimeProps(block = {}) {
    if (!isDataFieldBlockType(block.blockType) && block.blockType !== 'tree-panel')
      return
    const objectRef = resolvePageBlockObjectRef(block)
    if (!isValidPageBlockObjectRef(objectRef))
      return
    const cacheKey = resolveRuntimeObjectCacheKey(objectRef)
    const objectId = objectRef?.objectId ?? objectRef?.id
    if (!cacheKey || objectId === undefined || objectId === null || objectId === '')
      return
    if (runtimeCrudPropsByObjectId.value[cacheKey] || runtimeCrudLoadingObjectIds.has(cacheKey) || runtimeCrudUnavailableObjectIds.has(cacheKey))
      return
    runtimeCrudLoadingObjectIds.add(cacheKey)
    void loadRuntimeCrudProps(objectRef, cacheKey)
  }
  __impl.preloadPageBlockCrudRuntimeProps = preloadPageBlockCrudRuntimeProps

  /**
   * 表单设计器需要对象级字段资产、关系与动作上下文。
   * 与对象设计器同源调用 businessObjectDesigner，按对象缓存；失败仅降级为无上下文。
   */
  async function ensureFormDesignerObjectContext(objectRef) {
    const cacheKey = resolveRuntimeObjectCacheKey(objectRef)
    const objectId = objectRef?.objectId ?? objectRef?.id
    if (!cacheKey || objectId === undefined || objectId === null || objectId === '')
      return
    const cached = formDesignerObjectContextByObjectId.value[cacheKey]
    if ((cached && Array.isArray(cached.fields)) || formDesignerObjectContextLoadingIds.has(cacheKey))
      return
    formDesignerObjectContextLoadingIds.add(cacheKey)
    try {
      const designer = (await businessObjectDesigner(objectId)).data || {}
      formDesignerObjectContextByObjectId.value = {
        ...formDesignerObjectContextByObjectId.value,
        [cacheKey]: {
          objectCode: designer.objectCode || objectRef.objectCode || '',
          objectName: designer.objectName || objectRef.objectName || '',
          relations: Array.isArray(designer.relations) ? designer.relations : [],
          actions: Array.isArray(designer.designerOptions?.actions) ? designer.designerOptions.actions : [],
          fields: normalizeObjectDesignerFieldCatalog(designer.modelSchema?.fields || designer.fields || []),
          formDesignerSchema: designer.formDesignerSchema || designer.designerOptions?.formDesignerSchema || null,
        },
      }
    }
    catch (error) {
      console.warn('[application-runtime] 加载表单设计器对象上下文失败', error?.message || error)
    }
    finally {
      formDesignerObjectContextLoadingIds.delete(cacheKey)
    }
  }
  __impl.ensureFormDesignerObjectContext = ensureFormDesignerObjectContext

  watch(activeFormDesignerObjectRef, (objectRef) => {
    if (!objectRef)
      return
    void ensureFormDesignerObjectContext(objectRef)
    const cacheKey = resolveRuntimeObjectCacheKey(objectRef)
    const objectId = objectRef.objectId ?? objectRef.id
    if (!cacheKey || objectId === undefined || objectId === null || objectId === '')
      return
    if (runtimeCrudPropsByObjectId.value[cacheKey] || runtimeCrudLoadingObjectIds.has(cacheKey) || runtimeCrudUnavailableObjectIds.has(cacheKey))
      return
    runtimeCrudLoadingObjectIds.add(cacheKey)
    void loadRuntimeCrudProps(objectRef, cacheKey)
  }, { immediate: true })

  watch(
    () => activeFormDesignerContext.value?.formDesignerSchema,
    (objectSchema) => {
      if (activeFormDesignerSchema.value?.components?.length)
        return
      const normalized = presentFormDesignerSchema(objectSchema || {})
      if (!normalized.components?.length)
        return
      const signature = JSON.stringify(normalized)
      if (signature === mut._activeFormDesignerSchemaSignature)
        return
      mut._activeFormDesignerSchemaSignature = signature
      activeFormDesignerSchema.value = normalized
    },
  )

  function createFormFieldVisibilitySettings(currentSettings = {}, fieldRefs = [], forceVisible = false) {
    const settings = {}
    fieldRefs.filter(Boolean).forEach((field) => {
      const current = currentSettings?.[field] || {}
      settings[field] = forceVisible || !Object.prototype.hasOwnProperty.call(current, 'visible')
        ? { ...current, visible: true }
        : { ...current }
    })
    return settings
  }
  __impl.createFormFieldVisibilitySettings = createFormFieldVisibilitySettings

  function resolveFormAssetIdentity(asset = {}) {
    return String(asset?.id || asset?.formKey || '').trim()
  }

  /** 同一表单可能同时有 id 与 formKey，绑定/查找时两边都要认 */
  function resolveFormAssetMatchKeys(formAssetId = '') {
    const key = String(formAssetId || '').trim()
    if (!key)
      return []
    const asset = formAssets.value.find(item => String(item?.id || '').trim() === key
      || String(item?.formKey || '').trim() === key)
    if (!asset)
      return [key]
    return [...new Set([
      String(asset.id || '').trim(),
      String(asset.formKey || '').trim(),
      key,
    ].filter(Boolean))]
  }

  function formAssetIdMatches(boundId = '', formAssetId = '') {
    const bound = String(boundId || '').trim()
    if (!bound)
      return false
    return resolveFormAssetMatchKeys(formAssetId).includes(bound)
  }

  function findFormAssetByIdentity(formAssetId = '') {
    const key = String(formAssetId || '').trim()
    if (!key)
      return null
    return formAssets.value.find(item => String(item?.id || '').trim() === key
      || String(item?.formKey || '').trim() === key) || null
  }

  function applyFormAssetToBlock(block, formAssetId = '') {
    const nextId = String(formAssetId || '').trim()
    // 清空绑定时只摘掉 formAssetId，保留列表已有 fieldRefs，避免误伤查询列
    if (!nextId) {
      return {
        ...block,
        props: {
          ...(block.props || {}),
          formAssetId: '',
          formAssetFieldsInitialized: false,
        },
      }
    }
    const asset = findFormAssetByIdentity(nextId)
    const fieldRefs = asset ? resolveFormAssetFields(asset).map(field => field.fieldCode).filter(Boolean) : []
    return {
      ...block,
      fieldRefs: fieldRefs.length ? fieldRefs : block.fieldRefs,
      props: {
        ...(block.props || {}),
        formAssetId: nextId,
        formAssetFieldsInitialized: fieldRefs.length > 0,
        fieldSettings: createFormFieldVisibilitySettings(block.props?.fieldSettings, fieldRefs, true),
        ...(['AiCrudPage', 'search-form'].includes(block.blockType) && fieldRefs.length
          ? { searchFieldRefs: fieldRefs.slice(0, 8) }
          : {}),
      },
    }
  }

  function updateSelectedBlockFormAsset(formAssetId) {
    if (!selectedPageBlock.value)
      return
    const targetId = selectedPageBlock.value.id
    updatePageBlocks(mapPageBlocksInTree(pageBlocks.value, item => (
      item.id === targetId ? applyFormAssetToBlock(item, formAssetId) : item
    )))
  }
  __impl.updateSelectedBlockFormAsset = updateSelectedBlockFormAsset

  function selectFormAssetFromPicker(formAssetId) {
    updateSelectedBlockFormAsset(formAssetId)
    formAssetSelectorOpen.value = false
    formAssetSelectorKeyword.value = ''
  }
  __impl.selectFormAssetFromPicker = selectFormAssetFromPicker

  function listPageBlocksFlat() {
    const list = []
    visitPageBlocksInTree(pageBlocks.value, block => list.push(block))
    return list
  }

  function listPageCrudBlocksFlat() {
    return listPageBlocksFlat().filter(item => ['AiCrudPage', 'AiForm', 'data-table'].includes(item?.blockType))
  }

  /**
   * 页面资源：把指定表单设为当前页某个列表的新增/编辑表单。
   * 多 CRUD 时必须先选中目标列表；仅一个时自动绑定。
   */
  function setFormAssetAsListCrudForm(formAssetId = '') {
    const nextId = String(formAssetId || '').trim()
    const asset = findFormAssetByIdentity(nextId)
    if (!asset) {
      message.warning('表单不存在')
      return false
    }
    const listBlocks = listPageCrudBlocksFlat()
    const selected = selectedPageBlock.value
    const selectedIsList = selected && ['AiCrudPage', 'AiForm', 'data-table'].includes(selected.blockType)
    let target = selectedIsList ? selected : null
    if (!target && listBlocks.length === 1)
      target = listBlocks[0]
    if (!target && listBlocks.length > 1) {
      message.warning('本页有多个数据列表，请先在画布点选要绑定的那个，再点「设为新增编辑表单」')
      return false
    }
    if (!target) {
      message.warning('当前页还没有数据列表，请先选中或放入一个列表组件')
      return false
    }
    const bindId = resolveFormAssetIdentity(asset) || nextId
    if (!selectedIsList || selected?.id !== target.id)
      selectedPageBlockId.value = target.id
    updatePageBlocks(mapPageBlocksInTree(pageBlocks.value, item => (
      item.id === target.id ? applyFormAssetToBlock(item, bindId) : item
    )))
    // 绑定变更后清掉对象级 CRUD 缓存，下一帧按新 formAsset 重新叠表单
    const objectRef = resolvePageBlockObjectRef(target)
    const cacheKey = resolveRuntimeObjectCacheKey(objectRef)
    if (cacheKey) {
      const nextCache = { ...runtimeCrudPropsByObjectId.value }
      delete nextCache[cacheKey]
      runtimeCrudPropsByObjectId.value = nextCache
      runtimeCrudUnavailableObjectIds.delete(cacheKey)
      preloadPageBlockCrudRuntimeProps(target)
    }
    configPanelVisible.value = true
    inspectorTab.value = 'data'
    scrollPageBlockIntoView(target.id)
    const targetLabel = target.label || resolveListPageBlockMeta(target.blockType)?.title || '数据列表'
    message.success(`已将「${asset.name || bindId}」绑定到「${targetLabel}」的新增/编辑`)
    return true
  }
  __impl.setFormAssetAsListCrudForm = setFormAssetAsListCrudForm

  function isFormAssetBoundToListCrud(formAssetId = '') {
    const key = String(formAssetId || '').trim()
    if (!key)
      return false
    return listPageBlocksFlat().some((item) => {
      if (!['AiCrudPage', 'AiForm', 'data-table'].includes(item?.blockType))
        return false
      return formAssetIdMatches(item?.props?.formAssetId, key)
    })
  }
  __impl.isFormAssetBoundToListCrud = isFormAssetBoundToListCrud

  function resolveFormAssetUsagePageTitle(pageId = '') {
    const id = String(pageId || '').trim()
    if (!id)
      return '未命名页面'
    if (isWorkbenchPageId(id))
      return '个人工作台'
    const node = (builder.value?.nodes || []).find(item => String(item?.id || '') === id)
    return String(node?.title || node?.name || id).trim() || id
  }

  /**
   * 扫描全应用 builder：某表单资产被哪些页面 / 组件引用（用于页面资源闭环展示）。
   */
  function collectFormAssetUsages(formAssetId = '') {
    const key = String(formAssetId || '').trim()
    if (!key)
      return []
    const currentPageId = String(resolveActiveDesignerPageId() || '').trim()
    const usages = []
    const pages = builder.value?.pages && typeof builder.value.pages === 'object'
      ? builder.value.pages
      : {}
    Object.entries(pages).forEach(([pageId, page]) => {
      const items = page?.layout?.gridLayout?.items || page?.layout?.items || []
      visitPageBlocksInTree(items, (block) => {
        if (!formAssetIdMatches(block?.props?.formAssetId, key))
          return
        const meta = resolveListPageBlockMeta(block.blockType)
        usages.push({
          pageId,
          pageTitle: resolveFormAssetUsagePageTitle(pageId),
          blockId: block.id,
          blockType: block.blockType,
          blockLabel: block.label || meta?.title || block.blockType || '组件',
          isListCrud: ['AiCrudPage', 'AiForm', 'data-table'].includes(block.blockType),
          isCurrentPage: String(pageId) === currentPageId,
        })
      })
    })
    return usages
  }
  __impl.collectFormAssetUsages = collectFormAssetUsages

  function formatFormAssetUsageSummary(formAssetId = '') {
    const usages = collectFormAssetUsages(formAssetId)
    if (!usages.length)
      return '尚未绑定到任何组件'
    return usages.map((item) => {
      const pagePart = item.isCurrentPage ? '本页' : `页面「${item.pageTitle}」`
      return `${pagePart} · ${item.blockLabel}`
    }).join('；')
  }
  __impl.formatFormAssetUsageSummary = formatFormAssetUsageSummary

  /** 点击引用芯片：本页则选中并滚到组件；跨页只提示去对应页面 */
  function focusFormAssetUsage(usage = {}) {
    const blockId = String(usage?.blockId || '').trim()
    const pageId = String(usage?.pageId || '').trim()
    if (usage?.isCurrentPage && blockId) {
      selectPageBlock(blockId)
      scrollPageBlockIntoView(blockId)
      configPanelVisible.value = true
      inspectorTab.value = 'data'
      return
    }
    if (pageId) {
      message.info(`该表单绑定在页面「${usage.pageTitle || pageId}」，请打开该页后再查看组件`)
      return
    }
    message.info('未找到对应组件')
  }
  __impl.focusFormAssetUsage = focusFormAssetUsage

  /**
   * 取消「设为新增编辑表单」：仅清当前页对该表单的绑定（与设置动作同作用域）。
   */
  function clearFormAssetListCrudBinding(formAssetId = '') {
    const key = String(formAssetId || '').trim()
    if (!key)
      return false
    const asset = findFormAssetByIdentity(key)
    const targets = listPageBlocksFlat().filter(item => formAssetIdMatches(item?.props?.formAssetId, key))
    if (!targets.length) {
      message.info('当前页没有组件绑定该表单')
      return false
    }
    updatePageBlocks(mapPageBlocksInTree(pageBlocks.value, item => (
      formAssetIdMatches(item?.props?.formAssetId, key)
        ? applyFormAssetToBlock(item, '')
        : item
    )))
    targets.forEach((target) => {
      const objectRef = resolvePageBlockObjectRef(target)
      const cacheKey = resolveRuntimeObjectCacheKey(objectRef)
      if (!cacheKey)
        return
      const nextCache = { ...runtimeCrudPropsByObjectId.value }
      delete nextCache[cacheKey]
      runtimeCrudPropsByObjectId.value = nextCache
      runtimeCrudUnavailableObjectIds.delete(cacheKey)
    })
    message.success(`已取消「${asset?.name || key}」作为新增/编辑表单`)
    return true
  }
  __impl.clearFormAssetListCrudBinding = clearFormAssetListCrudBinding

  function updateSelectedPageBlockRuntimeObject(objectId) {
    if (!selectedPageBlock.value)
      return
    const object = objects.value.find(item => String(item.objectId ?? item.id ?? '') === String(objectId || ''))
    if (!object)
      return
    const previousObjectKey = resolveRuntimeObjectCacheKey(resolvePageBlockObjectRef(selectedPageBlock.value))
    const nextObjectKey = resolveRuntimeObjectCacheKey(object)
    const objectChanged = previousObjectKey !== nextObjectKey
    const configKey = String(object.configKey || '').trim()
    const apiPrefix = configKey ? `/ai/crud/${configKey}` : ''
    const objectApiProps = apiPrefix
      ? {
          api: apiPrefix,
          listApi: `get@${apiPrefix}/page`,
          detailApi: `get@${apiPrefix}/:id`,
          createApi: `post@${apiPrefix}`,
          updateApi: `put@${apiPrefix}`,
          deleteApi: `delete@${apiPrefix}/:id`,
          importApi: `post@${apiPrefix}/import`,
          exportApi: `post@${apiPrefix}/export`,
        }
      : {}
    const nextProps = {
      ...(selectedPageBlock.value.props || {}),
      ...objectApiProps,
      objectRef: {
        objectId: String(object.objectId ?? object.id),
        objectCode: object.objectCode || '',
        objectName: object.objectName || '',
        configKey: object.configKey || '',
      },
    }
    if (objectChanged) {
      delete nextProps.fieldSettings
      delete nextProps.searchFieldRefs
      delete nextProps.searchFieldSettings
    }
    const nextBlock = {
      ...selectedPageBlock.value,
      fieldRefs: objectChanged ? [] : selectedPageBlock.value.fieldRefs,
      props: nextProps,
    }
    // 数据源切换不影响坐标和尺寸，直接更新布局，避免触发根页面碰撞重算。
    updateCurrentGridLayout({
      ...currentGridLayout.value,
      items: mapPageBlocksInTree(pageBlocks.value, block => block.id === nextBlock.id ? nextBlock : block),
    })
    runtimeCrudUnavailableObjectIds.delete(resolveRuntimeObjectCacheKey(object))
    preloadPageBlockCrudRuntimeProps(nextBlock)
  }
  __impl.updateSelectedPageBlockRuntimeObject = updateSelectedPageBlockRuntimeObject

  function createFormAssetForSelectedBlock() {
    if (!currentNode.value)
      return
    const blockTitle = selectedPageBlock.value?.label || resolveListPageBlockMeta(selectedPageBlock.value?.blockType)?.title || '页面'
    const name = `${currentNode.value.title}${blockTitle === 'AiForm' ? '录入表单' : '数据表单'}`
    const result = createInAppFormAsset(builder.value, {
      name,
      formDesignerSchema: createDefaultFormDesignerSchema({
        objectCode: application.value?.applicationCode || 'application',
        objectName: name,
        formName: name,
      }),
    })
    builder.value = result.schema
    updateSelectedBlockFormAsset(result.formAssetId)
    activeFormAssetId.value = result.formAssetId
    formDesignerMode.value = true
  }
  __impl.createFormAssetForSelectedBlock = createFormAssetForSelectedBlock

  function createStandaloneFormAsset() {
    const name = `${application.value?.applicationName || '应用'}表单`
    const result = createInAppFormAsset(builder.value, {
      name,
      formDesignerSchema: createDefaultFormDesignerSchema({
        objectCode: application.value?.applicationCode || 'application',
        objectName: name,
        formName: name,
      }),
    })
    builder.value = result.schema
    bindSingleFormToCompatibleBlocks()
    openFormAssetDesigner(result.formAssetId)
  }
  __impl.createStandaloneFormAsset = createStandaloneFormAsset

  function openFormAssetDesigner(formAssetId) {
    const asset = findFormAssetByIdentity(formAssetId)
    // 统一落到资产 id，避免 formKey 与 id 混用导致设计器 v-if="activeFormAsset" 空白
    const normalizedId = String(asset?.id || formAssetId || '').trim()
    if (!normalizedId || !asset) {
      message.warning('表单不存在或尚未加载完成，请稍后重试')
      return
    }
    activeFormAssetId.value = normalizedId
    activePageShapeDesign.value = resolvePageShapeDesignContext(normalizedId)
      || resolvePageShapeDesignContext(resolveFormAssetIdentity(asset))
    formDesignerMode.value = true
  }
  __impl.openFormAssetDesigner = openFormAssetDesigner

  function resolvePageShapeDesignContext(formAssetId) {
    const supportedTypes = new Set(['form', 'list', 'list-form', 'tree-list', 'tree-table'])
    for (const node of builder.value?.nodes || []) {
      if (node?.type !== 'page')
        continue
      const page = builder.value?.pages?.[node.id]
      const block = findPageBlockByFormAssetId(page?.layout?.gridLayout?.items || [], formAssetId)
      if (!block)
        continue
      const layoutType = page?.layout?.gridLayout?.layoutType
      const pageType = supportedTypes.has(node.pageTemplate)
        ? node.pageTemplate
        : supportedTypes.has(node.pageShape)
          ? node.pageShape
          : layoutType === 'tree-crud'
            ? 'tree-table'
            : supportedTypes.has(layoutType)
              ? layoutType
              : 'list-form'
      const objectRef = block.props?.objectRef || node.objectRef || {}
      return {
        pageId: node.id,
        pageType,
        formAssetId,
        objectId: objectRef.objectId || null,
        objectCode: objectRef.objectCode || '',
        objectName: objectRef.objectName || node.title || '',
        runtimeDatasourceId: objectRef.runtimeDatasourceId || null,
        createMode: objectRef.createMode || '',
        importDatasourceId: objectRef.importDatasourceId || objectRef.runtimeDatasourceId || null,
        importTableName: objectRef.importTableName || '',
      }
    }
    return null
  }
  __impl.resolvePageShapeDesignContext = resolvePageShapeDesignContext

  function findPageBlockByFormAssetId(blocks, formAssetId) {
    for (const block of blocks || []) {
      if (formAssetIdMatches(block?.props?.formAssetId, formAssetId))
        return block
      const nested = [
        ...(block?.children || []),
        ...(block?.props?.tabs || []).flatMap(tab => tab?.children || []),
        ...(block?.props?.cells || []).flatMap(cell => cell?.children || []),
      ]
      const matched = findPageBlockByFormAssetId(nested, formAssetId)
      if (matched)
        return matched
    }
    return null
  }
  __impl.findPageBlockByFormAssetId = findPageBlockByFormAssetId

  function syncActivePageShapeObject() {
    const context = activePageShapeDesign.value
    if (!context)
      return
    const patchObjectRef = objectRef => ({
      ...(objectRef || {}),
      objectId: context.objectId || objectRef?.objectId || null,
      objectCode: context.objectCode,
      objectName: context.objectName,
      runtimeDatasourceId: context.runtimeDatasourceId ?? objectRef?.runtimeDatasourceId ?? null,
      createMode: context.createMode || objectRef?.createMode || '',
      importDatasourceId: context.importDatasourceId ?? objectRef?.importDatasourceId ?? context.runtimeDatasourceId ?? null,
      importTableName: context.importTableName || objectRef?.importTableName || '',
      valid: true,
    })
    builder.value = {
      ...builder.value,
      nodes: builder.value.nodes.map(node => node.id === context.pageId
        ? { ...node, objectRef: patchObjectRef(node.objectRef) }
        : node),
      pages: Object.fromEntries(Object.entries(builder.value.pages || {}).map(([pageId, page]) => [
        pageId,
        pageId === context.pageId
          ? {
              ...page,
              layout: {
                ...page.layout,
                gridLayout: {
                  ...page.layout?.gridLayout,
                  items: mapPageBlocksInTree(page.layout?.gridLayout?.items || [], block =>
                    String(block?.props?.formAssetId || '') === String(context.formAssetId)
                      ? {
                          ...block,
                          props: {
                            ...(block.props || {}),
                            objectRef: patchObjectRef(block.props?.objectRef),
                          },
                        }
                      : block),
                },
              },
            }
          : page,
      ])),
    }
  }
  __impl.syncActivePageShapeObject = syncActivePageShapeObject

  function openPageBlockConfiguration(block = {}) {
    if (!editing.value || !block?.id)
      return
    selectedPageBlockId.value = block.id
    nestHostPinnedId.value = ['card', 'box-layout', 'tabs', 'grid-layout'].includes(block.blockType)
      ? block.id
      : ''
    inspectorTab.value = 'properties'
    // 先反馈选中框，属性面板放到下一帧，减轻重型块切换时的卡顿感
    requestAnimationFrame(() => {
      if (selectedPageBlockId.value === block.id)
        configPanelVisible.value = true
    })
  }
  __impl.openPageBlockConfiguration = openPageBlockConfiguration

  function handlePageBlockDataSourceRequest(blockId) {
    const block = findPageBlockInTree(pageBlocks.value, blockId)
    if (!editing.value || !block)
      return
    selectedPageBlockId.value = block.id
    configPanelVisible.value = true
    inspectorTab.value = 'data'
  }
  __impl.handlePageBlockDataSourceRequest = handlePageBlockDataSourceRequest

  function editSelectedBlockFormAsset() {
    const formAssetId = selectedPageBlockFormAssetId.value
    if (!formAssetId)
      return
    openFormAssetDesigner(formAssetId)
  }
  __impl.editSelectedBlockFormAsset = editSelectedBlockFormAsset

  function openSelectedBlockFormDesigner() {
    if (selectedPageBlockFormAssetId.value) {
      editSelectedBlockFormAsset()
      return
    }
    createFormAssetForSelectedBlock()
  }
  __impl.openSelectedBlockFormDesigner = openSelectedBlockFormDesigner

  function updateActiveFormDesignerSchema(schema) {
    if (!activeFormAsset.value)
      return
    const normalizedSchema = normalizeFormDesignerSchema(schema)
    const formAssetId = activeFormAsset.value.id
    let nextBuilder = updateInAppFormAsset(builder.value, formAssetId, {
      name: normalizedSchema.formName || activeFormAsset.value.name,
      formDesignerSchema: normalizedSchema,
    })
    const nextAsset = nextBuilder.formAssets.find(asset => asset.id === formAssetId)
    const fieldRefs = resolveFormAssetFields(nextAsset).map(field => field.fieldCode)
    nextBuilder = {
      ...nextBuilder,
      pages: Object.fromEntries(Object.entries(nextBuilder.pages || {}).map(([pageId, page]) => {
        const items = page?.layout?.gridLayout?.items
        if (!Array.isArray(items))
          return [pageId, page]
        return [pageId, {
          ...page,
          layout: {
            ...page.layout,
            gridLayout: {
              ...page.layout.gridLayout,
              items: items.map((item) => {
                if (item?.props?.formAssetId !== formAssetId)
                  return item
                const synced = syncFormBoundFieldRefs({
                  formFieldCodes: fieldRefs,
                  searchFieldRefs: item.props?.searchFieldRefs,
                })
                return {
                  ...item,
                  fieldRefs: synced.fieldRefs,
                  props: {
                    ...(item.props || {}),
                    formAssetFieldsInitialized: synced.fieldRefs.length > 0,
                    fieldSettings: createFormFieldVisibilitySettings(item.props?.fieldSettings, synced.fieldRefs),
                    ...(['AiCrudPage', 'search-form'].includes(item.blockType)
                      ? { searchFieldRefs: synced.searchFieldRefs }
                      : {}),
                  },
                }
              }),
            },
          },
        }]
      })),
    }
    // 设计器 props 回写/分区派生若未产生实质差异，不要弄脏草稿签名
    if (JSON.stringify(nextBuilder) === JSON.stringify(builder.value || {}))
      return
    builder.value = nextBuilder
  }
  __impl.updateActiveFormDesignerSchema = updateActiveFormDesignerSchema

  function returnToPageDesigner() {
    // 以当前选中页为准。设计上下文里的 pageId 可能是另一张也绑了表单的页面，
    // 用它去挂表单会把刚设计的内容换成那张页上的空表单。
    const pageId = selectedNodeId.value || activePageShapeDesign.value?.pageId
    formDesignerMode.value = false
    // 如果用户从页面管理视图进入表单设计器，返回时退出编辑模式，避免出现设计工作台
    if (formDesignerFromPageManagement.value) {
      exitToPageManagement()
      return
    }
    // 返回后仍停在表单/列表设计页，必须重新挂上当前页面的表单，否则画布是空的
    if (pageId) {
      const entryTab = resolveEntryDesignTab(pageId)
      activePageDesignTab.value = entryTab === 'page' ? 'form' : entryTab
      syncActiveFormAssetForPage(pageId)
      const nextQuery = {
        ...route.query,
        pageId,
        edit: '1',
        designResource: `page-custom:${pageId}`,
        designTab: activePageDesignTab.value === resolveEntryDesignTab(pageId) ? undefined : activePageDesignTab.value,
      }
      // 对象页从全屏表单设计返回时，清掉误带的 designTab=page，否则表单/列表 Tab 会被藏掉
      if (nextQuery.designTab === 'page' && resolvePageShapeKey(pageId) !== 'custom')
        delete nextQuery.designTab
      router.replace({ query: nextQuery })
    }
    else {
      activeFormAssetId.value = ''
      activePageShapeDesign.value = null
    }
  }
  __impl.returnToPageDesigner = returnToPageDesigner

  function updatePageBlocks(items, options = {}) {
    const nextItems = options.resolveCollisions
      ? resolveRootPageBlockCollisions(items, options.changedBlockId)
      : items
    updateCurrentGridLayout({ ...currentGridLayout.value, items: nextItems })
  }
  __impl.updatePageBlocks = updatePageBlocks

  /** 卡片 / 盒子：整块主体就是投放区（对齐栅格 cell 命中） */
  function isSimpleBodyContainer(block = {}) {
    return ['card', 'box-layout'].includes(block?.blockType)
  }
  __impl.isSimpleBodyContainer = isSimpleBodyContainer

  function resolvePageBlockShellStyle(block) {
    return computePageBlockShellStyle(block, pageBlocks.value, {
      pageId: String(route.query.pageId || selectedNodeId.value || currentNode.value?.id || ''),
      pagePadding: pageCanvasPadding.value,
      pageFlowCoordSpace: currentGridLayout.value?.pageFlowCoordSpace,
    })
  }
  __impl.resolvePageBlockShellStyle = resolvePageBlockShellStyle

  function openPagePropertiesPanel() {
    if (!editing.value)
      return
    selectedPageBlockId.value = ''
    nestHostPinnedId.value = ''
    inspectorTab.value = 'properties'
    configPanelVisible.value = true
  }
  __impl.openPagePropertiesPanel = openPagePropertiesPanel

  function handlePageFlowBlankClick(event) {
    if (!editing.value)
      return
    if (event.target?.closest?.('[data-page-block-id]'))
      return
    openPagePropertiesPanel()
  }
  __impl.handlePageFlowBlankClick = handlePageFlowBlankClick

  function selectPageBlock(blockId) {
    if (!editing.value)
      return
    selectedPageBlockId.value = blockId
    const block = findPageBlockInTree(pageBlocks.value, blockId)
    nestHostPinnedId.value = block && ['card', 'box-layout', 'tabs', 'grid-layout'].includes(block.blockType)
      ? block.id
      : ''
    inspectorTab.value = 'properties'
    // 画布 @block-activate 只调本函数；需同步打开右侧配置，避免先点「数据」才出现属性
    if (blockId) {
      requestAnimationFrame(() => {
        if (selectedPageBlockId.value === blockId)
          configPanelVisible.value = true
      })
    }
  }
  __impl.selectPageBlock = selectPageBlock

  function handleNestedPageBlockSelect(blockId) {
    const block = findPageBlockInTree(pageBlocks.value, blockId)
    if (!block)
      return
    openPageBlockConfiguration(block)
  }
  __impl.handleNestedPageBlockSelect = handleNestedPageBlockSelect

  function handleInlineTextUpdate({ blockId, patch }) {
    if (!blockId || !patch)
      return
    updatePageBlocks(pageBlocks.value.map(item => item.id === blockId
      ? { ...item, props: { ...(item.props || {}), ...patch } }
      : item))
  }
  __impl.handleInlineTextUpdate = handleInlineTextUpdate

  function handleRuntimeTreeSelect(payload = {}) {
    const blockId = String(payload.blockId || '').trim()
    if (!blockId)
      return
    const nextActiveKeys = { ...runtimeTreeActiveKeyByBlockId.value }
    const nextFilters = { ...runtimeTreeFilterByBlockId.value }
    nextActiveKeys[blockId] = payload.key || '__all__'
    if (!payload.filterField || payload.clear || payload.value === undefined
      || payload.value === null || payload.value === '') {
      runtimeTreeFilter.value = {}
      delete nextFilters[blockId]
    }
    else {
      const nextFilter = buildLeftTreeFilterParams({
        filterField: payload.filterField,
        value: payload.value,
        includeChildren: payload.includeChildren !== false,
        expandedValues: payload.expandedValues,
      })
      runtimeTreeFilter.value = nextFilter
      nextFilters[blockId] = nextFilter
    }
    runtimeTreeActiveKeyByBlockId.value = nextActiveKeys
    runtimeTreeFilterByBlockId.value = nextFilters
  }
  __impl.handleRuntimeTreeSelect = handleRuntimeTreeSelect

  function resolvePagePreviewBlock(block) {
    return {
      ...block,
      props: {
        ...(block.props || {}),
        style: {
          ...(block.props?.style || {}),
          width: '100%',
          height: '100%',
          minHeight: '',
          maxHeight: '',
          margin: 0,
        },
      },
    }
  }
  __impl.resolvePagePreviewBlock = resolvePagePreviewBlock

  function handleComponentCatalogDragStart(event, component) {
    if (!editing.value || !component?.blockType)
      return
    catalogDragBlockType.value = component.blockType
    suppressCatalogClick.value = true
    event.dataTransfer.effectAllowed = 'copy'
    event.dataTransfer.setData('application/x-forge-app-page-block', component.blockType)
    // 兼容列表设计器的原生拖拽协议，确保从同一套左侧组件面板拖入运行时 Tabs 时可命中。
    event.dataTransfer.setData('application/x-list-block', component.blockType)
    // 使用表单设计器相同的布局拖拽协议，避免 Popover/浏览器清理自定义页面协议。
    event.dataTransfer.setData('application/x-forge-form-layout', JSON.stringify({
      componentKey: component.blockType,
      label: component.title || component.label || component.blockType,
    }))
  }
  __impl.handleComponentCatalogDragStart = handleComponentCatalogDragStart

  function startCatalogPointerDrag(event, component) {
    if (!editing.value || !component?.blockType)
      return
    catalogDragBlockType.value = component.blockType
    suppressCatalogClick.value = false
    mut.catalogPointerDragCtx = {
      blockType: component.blockType,
      startX: event.clientX,
      startY: event.clientY,
      moved: false,
      pointerId: event.pointerId,
    }
    try {
      event.currentTarget?.setPointerCapture?.(event.pointerId)
    }
    catch {
      // ignore capture failure
    }
    window.addEventListener('pointermove', handleCatalogPointerMove, { passive: false })
    window.addEventListener('pointerup', finishCatalogPointerDrag, { once: true })
    window.addEventListener('pointercancel', finishCatalogPointerDrag, { once: true })
  }
  __impl.startCatalogPointerDrag = startCatalogPointerDrag

  function normalizeContainerDropTarget(target = null) {
    if (!target)
      return null
    const containerId = String(target.containerId || target.blockId || '')
    const containerType = String(target.containerType || target.blockType || '')
    if (!containerId)
      return null
    return { containerId, blockId: containerId, containerType }
  }
  __impl.normalizeContainerDropTarget = normalizeContainerDropTarget

  function handleCatalogPointerMove(event) {
    if (!mut.catalogPointerDragCtx)
      return
    const ctx = mut.catalogPointerDragCtx
    if (!ctx.moved && Math.hypot(event.clientX - ctx.startX, event.clientY - ctx.startY) < 6)
      return
    ctx.moved = true
    suppressCatalogClick.value = true
    event.preventDefault()
    // 拖动中不关闭组件浮层（关闭会触发 pointercancel，导致栅格命中失败）
    // 仅用样式屏蔽浮层命中，松手后再收起
    document.body.classList.add('is-catalog-pointer-dragging')
    activePageFlowTabTarget.value = resolvePageFlowTabTargetFromPoint(event)
    const gridTarget = resolvePageFlowGridCellTargetFromPoint(event)
    activePageFlowGridTarget.value = gridTarget
      ? { containerId: gridTarget.blockId, cellKey: gridTarget.cellKey, cellIndex: gridTarget.cellIndex }
      : null
    activePageFlowContainerTarget.value = gridTarget
      ? null
      : normalizeContainerDropTarget(resolvePageFlowContainerTargetFromPoint(event))
    syncCatalogDropActiveClass(
      activePageFlowGridTarget.value?.containerId
      || activePageFlowContainerTarget.value?.containerId
      || activePageFlowTabTarget.value?.blockId
      || '',
    )
    // 组件库拖入时也显示十字标线，便于对准栅格
    const flow = document.querySelector('.application-page-flow')
    const flowRect = flow?.getBoundingClientRect?.()
    if (flowRect) {
      pageAlignGuides.value = {
        visible: true,
        cross: {
          x: Math.round(event.clientX - flowRect.left + (flow.scrollLeft || 0)),
          y: Math.round(event.clientY - flowRect.top + (flow.scrollTop || 0)),
        },
        x: [],
        y: [],
      }
    }
  }
  __impl.handleCatalogPointerMove = handleCatalogPointerMove

  __impl.createFormAssetForPageCrud = createFormAssetForPageCrud
  __impl.resolveNextNavigationTitle = resolveNextNavigationTitle
  __impl.applyPageTemplate = applyPageTemplate
  __impl.resolveNavigationMoreOptions = resolveNavigationMoreOptions
  __impl.renderNavigationMenuIcon = renderNavigationMenuIcon
  __impl.handleNavigationMoreSelect = handleNavigationMoreSelect
  __impl.toggleNavigationVisible = toggleNavigationVisible
  __impl.openSystemMenuMountDialog = openSystemMenuMountDialog
  __impl.patchNavigationNodeMount = patchNavigationNodeMount
  __impl.confirmSystemMenuMount = confirmSystemMenuMount
  __impl.unmountSystemMenu = unmountSystemMenu
  __impl.duplicateNavigationNode = duplicateNavigationNode
  __impl.moveNavigationByOffset = moveNavigationByOffset
  __impl.confirmNavigationAction = confirmNavigationAction
  __impl.isNavigationGroupDescendant = isNavigationGroupDescendant
  __impl.insertComponent = insertComponent
  __impl.updateCurrentGridLayout = updateCurrentGridLayout
  __impl.ensurePageFlowContentCoords = ensurePageFlowContentCoords
  __impl.updatePageCanvasPadding = updatePageCanvasPadding
  __impl.updatePageCanvasSurface = updatePageCanvasSurface
  __impl.resetPageCanvasSurface = resetPageCanvasSurface
  __impl.resolveActiveDesignerPageId = resolveActiveDesignerPageId
  __impl.appendPageBlock = appendPageBlock
  __impl.resolveNextPageBlockStackY = resolveNextPageBlockStackY
  __impl.supportsFormAsset = supportsFormAsset
  __impl.bindSingleFormToCompatibleBlocks = bindSingleFormToCompatibleBlocks
  __impl.attachSingleFormAsset = attachSingleFormAsset
  __impl.resolveFormAssetFields = resolveFormAssetFields
  __impl.resolvePageBlockFields = resolvePageBlockFields
  __impl.resolvePortalFormFields = resolvePortalFormFields
  __impl.resolvePageBlockObjectRef = resolvePageBlockObjectRef
  __impl.isValidPageBlockObjectRef = isValidPageBlockObjectRef
  __impl.isPageBlockDataSourceConfigured = isPageBlockDataSourceConfigured
  __impl.hydratePageCrudApiPlaceholders = hydratePageCrudApiPlaceholders
  __impl.resolveRuntimeObjectCacheKey = resolveRuntimeObjectCacheKey
  __impl.attachDefaultRuntimeObject = attachDefaultRuntimeObject
  __impl.resolvePageBlockRuntimeCrudProps = resolvePageBlockRuntimeCrudProps
  __impl.isPageBlockRuntimeCrudLoading = isPageBlockRuntimeCrudLoading
  __impl.preloadCurrentPageCrudRuntimeProps = preloadCurrentPageCrudRuntimeProps
  __impl.preloadPageBlockCrudRuntimeProps = preloadPageBlockCrudRuntimeProps
  __impl.ensureFormDesignerObjectContext = ensureFormDesignerObjectContext
  __impl.createFormFieldVisibilitySettings = createFormFieldVisibilitySettings
  __impl.updateSelectedBlockFormAsset = updateSelectedBlockFormAsset
  __impl.selectFormAssetFromPicker = selectFormAssetFromPicker
  __impl.setFormAssetAsListCrudForm = setFormAssetAsListCrudForm
  __impl.isFormAssetBoundToListCrud = isFormAssetBoundToListCrud
  __impl.collectFormAssetUsages = collectFormAssetUsages
  __impl.formatFormAssetUsageSummary = formatFormAssetUsageSummary
  __impl.focusFormAssetUsage = focusFormAssetUsage
  __impl.clearFormAssetListCrudBinding = clearFormAssetListCrudBinding
  __impl.updateSelectedPageBlockRuntimeObject = updateSelectedPageBlockRuntimeObject
  __impl.createFormAssetForSelectedBlock = createFormAssetForSelectedBlock
  __impl.createStandaloneFormAsset = createStandaloneFormAsset
  __impl.openFormAssetDesigner = openFormAssetDesigner
  __impl.resolvePageShapeDesignContext = resolvePageShapeDesignContext
  __impl.findPageBlockByFormAssetId = findPageBlockByFormAssetId
  __impl.syncActivePageShapeObject = syncActivePageShapeObject
  __impl.openPageBlockConfiguration = openPageBlockConfiguration
  __impl.handlePageBlockDataSourceRequest = handlePageBlockDataSourceRequest
  __impl.editSelectedBlockFormAsset = editSelectedBlockFormAsset
  __impl.openSelectedBlockFormDesigner = openSelectedBlockFormDesigner
  __impl.updateActiveFormDesignerSchema = updateActiveFormDesignerSchema
  __impl.returnToPageDesigner = returnToPageDesigner
  __impl.updatePageBlocks = updatePageBlocks
  __impl.isSimpleBodyContainer = isSimpleBodyContainer
  __impl.resolvePageBlockShellStyle = resolvePageBlockShellStyle
  __impl.openPagePropertiesPanel = openPagePropertiesPanel
  __impl.handlePageFlowBlankClick = handlePageFlowBlankClick
  __impl.selectPageBlock = selectPageBlock
  __impl.handleNestedPageBlockSelect = handleNestedPageBlockSelect
  __impl.handleInlineTextUpdate = handleInlineTextUpdate
  __impl.handleRuntimeTreeSelect = handleRuntimeTreeSelect
  __impl.resolvePagePreviewBlock = resolvePagePreviewBlock
  __impl.handleComponentCatalogDragStart = handleComponentCatalogDragStart
  __impl.startCatalogPointerDrag = startCatalogPointerDrag
  __impl.normalizeContainerDropTarget = normalizeContainerDropTarget
  __impl.handleCatalogPointerMove = handleCatalogPointerMove

  return {
    ...deps,
  }
}
