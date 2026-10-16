import { readdirSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it, vi } from 'vitest'
import {
  createApplicationRuntimeLoadCoordinator,
  prefetchApplicationRuntimeChunk,
  resolveApplicationRuntimeLoadKey,
  shouldUseApplicationWorkspaceLoad,
} from '../application-runtime-load'

function readApplicationRuntimeSource() {
  const vuePath = resolve('src/views/app-center/application-runtime.[applicationCode].vue')
  const modulesDir = resolve('src/views/app-center/runtime-modules')
  const partFiles = readdirSync(modulesDir)
    .filter(name => /^useApplicationRuntime(?:\.part\d+)?\.js$/.test(name))
    .sort()
    .map(name => readFileSync(resolve(modulesDir, name), 'utf8'))
  return [readFileSync(vuePath, 'utf8'), ...partFiles].join('\n')
}

function readJoinedSources(paths) {
  return paths.map(p => readFileSync(resolve(p), 'utf8')).join('\n')
}

function readGridBlockRendererSource() {
  const pageDir = resolve('src/components/lowcode-builder/page')
  const names = [
    'GridBlockRenderer.vue',
    'gridBlockRendererLocalComponents.js',
    'gridBlockRendererUtils.js',
    'composables/useGridBlockRenderer.js',
  ]
  return readJoinedSources(names.map(name => `${pageDir}/${name}`))
}

function readListPageGridDesignerSource() {
  const pageDir = resolve('src/components/lowcode-builder/page')
  const files = [
    'ListPageGridDesigner.vue',
    'listPageDesignerLocalComponents.js',
    'composables/useListPageGridDesigner.js',
    ...readdirSync(resolve(pageDir, 'composables'))
      .filter(name => /^useListPageGridDesigner\.part\d+\.js$/.test(name))
      .sort()
      .map(name => `composables/${name}`),
  ]
  return readJoinedSources(files.map(name => `${pageDir}/${name}`))
}

describe('application runtime route loading', () => {
  it('exposes a stable runtime chunk prefetch helper', async () => {
    expect(typeof prefetchApplicationRuntimeChunk).toBe('function')
    const first = prefetchApplicationRuntimeChunk()
    const second = prefetchApplicationRuntimeChunk()
    expect(first).toBe(second)
    await first
  }, 15000)
  it('coalesces repeated notifications for the same route state', async () => {
    let release
    const load = vi.fn(() => new Promise((resolve) => {
      release = resolve
    }))
    const coordinator = createApplicationRuntimeLoadCoordinator(load)

    const first = coordinator.run('hr_apply|draft')
    const second = coordinator.run('hr_apply|draft')
    release()
    await Promise.all([first, second])
    await coordinator.run('hr_apply|draft')

    expect(load).toHaveBeenCalledTimes(1)
  })

  it('runs only the latest different route queued during a load', async () => {
    const releases = []
    const load = vi.fn(() => new Promise(resolve => releases.push(resolve)))
    const coordinator = createApplicationRuntimeLoadCoordinator(load)

    const task = coordinator.run('app-a|draft')
    coordinator.run('app-b|draft')
    coordinator.run('app-b|runtime')
    releases.shift()()
    await Promise.resolve()
    releases.shift()()
    await task

    expect(load.mock.calls.map(call => call[0])).toEqual([
      'app-a|draft',
      'app-b|runtime',
    ])
  })

  it('keeps page selection out of the workspace load key', () => {
    const first = resolveApplicationRuntimeLoadKey({
      params: { applicationCode: 'hr_apply' },
      query: { pageId: 'page_1', draft: '1' },
    })
    const second = resolveApplicationRuntimeLoadKey({
      params: { applicationCode: 'hr_apply' },
      query: { pageId: 'page_2', draft: '1' },
    })

    expect(second).toBe(first)
  })

  it('does not reload workspace when editors toggle edit or draft query', () => {
    const pageManagement = resolveApplicationRuntimeLoadKey({
      params: { applicationCode: 'hr_apply' },
      query: { pageId: 'page_1' },
    }, true)
    const pageDesign = resolveApplicationRuntimeLoadKey({
      params: { applicationCode: 'hr_apply' },
      query: { pageId: 'page_1', edit: '1' },
    }, true)
    const draftPreview = resolveApplicationRuntimeLoadKey({
      params: { applicationCode: 'hr_apply' },
      query: { pageId: 'page_1', draft: '1' },
    }, true)

    expect(pageDesign).toBe(pageManagement)
    expect(draftPreview).toBe(pageManagement)
  })

  it('uses workspace for edit, draft preview, or editors in page management', () => {
    expect(shouldUseApplicationWorkspaceLoad({ query: {} })).toBe(false)
    expect(shouldUseApplicationWorkspaceLoad({ query: { pageId: 'page_1' } })).toBe(false)
    expect(shouldUseApplicationWorkspaceLoad({ query: { pageId: 'page_1' } }, true)).toBe(true)
    expect(shouldUseApplicationWorkspaceLoad({ query: { edit: '1' } })).toBe(true)
    expect(shouldUseApplicationWorkspaceLoad({ query: { draft: '1' } })).toBe(true)
  })

  it('loads workspace for editors in page management; published runtime only for viewers', () => {
    const runtimeSource = readApplicationRuntimeSource()
    expect(runtimeSource).toContain('businessApplicationRuntimeByCode')
    expect(runtimeSource).toContain('shouldUseApplicationWorkspaceLoad(route, canEditApplication.value)')
    // 工作台内有编辑权限时走 designPreview：未发布对象可列表/新增；正式门户路由仍只看已发布配置。
    // configurable 必须为 false，否则 GridBlockRenderer 会按设计态画虚线边框。
    expect(runtimeSource).toContain(':design-preview="usePortalDesignPreview"')
    expect(runtimeSource).toContain('canEditApplication.value')
    expect(runtimeSource).toContain('const usePortalDesignPreview = computed(() => (')
    expect(runtimeSource).not.toContain(':design-preview="editing || isDraftMode || canEditApplication"')
    expect(runtimeSource).toContain(':configurable="false"')
    expect(runtimeSource).toContain(':crud-config-revision="portalCrudConfigRevision"')
    expect(runtimeSource).toContain('import.meta.glob(\'/src/assets/images/form/*.png\', { import: \'default\' })')
    expect(runtimeSource).not.toContain('eager: true')
  })

  it('exposes form/list design tabs for tree-list and tree-table page shapes', () => {
    const runtimeSource = readApplicationRuntimeSource()
    expect(runtimeSource).toContain('const FORM_DESIGN_SHAPES = new Set([\'form\', \'list-form\', \'tree-list\', \'tree-table\'])')
    expect(runtimeSource).toContain('const LIST_DESIGN_SHAPES = new Set([\'list\', \'list-form\', \'tree-list\', \'tree-table\'])')
  })

  it('includes editor workspace access in the load key', () => {
    const withoutEdit = resolveApplicationRuntimeLoadKey({
      params: { applicationCode: 'hr_apply' },
      query: { pageId: 'page_1' },
    }, false)
    const withEdit = resolveApplicationRuntimeLoadKey({
      params: { applicationCode: 'hr_apply' },
      query: { pageId: 'page_1' },
    }, true)
    expect(withEdit).not.toBe(withoutEdit)
  })

  it('does not reload when an editor enters page design', () => {
    const pageManagement = resolveApplicationRuntimeLoadKey({
      params: { applicationCode: 'hr_apply' },
      query: {},
    }, true)
    const editing = resolveApplicationRuntimeLoadKey({
      params: { applicationCode: 'hr_apply' },
      query: { edit: '1', pageId: 'page_new', designTab: 'page' },
    }, true)
    expect(editing).toBe(pageManagement)
  })

  it('invalidates portal crud cache after form save refresh', () => {
    const runtimeSource = readApplicationRuntimeSource()
    const portalSource = readFileSync(resolve('src/views/app-center/components/portal/PortalPageRenderer.vue'), 'utf8')
    expect(runtimeSource).toContain('portalCrudConfigRevision.value += 1')
    expect(runtimeSource).toContain('normalizeInAppBuilder(application.value.options, application.value, objects.value)')
    expect(runtimeSource).toContain('if (application.value && syncBuilder)')
    expect(runtimeSource).toContain('if (markClean)')
    expect(runtimeSource).toContain('savedSignature.value = JSON.stringify(builder.value || {})')
    // 换页不强制 remount Portal，靠 revision 与内部缓存失效；同对象跨页复用 render
    expect(runtimeSource).toContain(`:key="\`portal:\${portalCrudConfigRevision}\`"`)
    expect(portalSource).toContain('crudConfigRevision')
    expect(portalSource).toContain('configRev')
    expect(portalSource).toContain('invalidateAll')
  })

  it('marks draft clean after save refresh mutations to avoid double success toasts', () => {
    const runtimeSource = readApplicationRuntimeSource()
    expect(runtimeSource).toContain('refreshWorkspaceMetadata({ syncBuilder: true, markClean: true })')
    expect(runtimeSource).toContain('await saveDraft({ quiet: true })')
    expect(runtimeSource).toContain('bind/hydrate 可能继续改 builder')
    expect(runtimeSource).toContain('async function markBuilderClean')
    expect(runtimeSource).toContain('await markBuilderClean()')
  })

  it('prefetches workspace tab panels after application load for editors', () => {
    const runtimeSource = readApplicationRuntimeSource()
    expect(runtimeSource).toContain('prefetchRuntimeWorkspacePanels')
    expect(runtimeSource).toContain('requestIdleCallback')
    expect(runtimeSource).toContain('<keep-alive>')
  })

  it('waits for object runtime config before mounting the CRUD page', () => {
    const runtimeSource = readApplicationRuntimeSource()
    const rendererSource = readGridBlockRendererSource()
    const portalSource = readFileSync(resolve('src/views/app-center/components/portal/PortalPageRenderer.vue'), 'utf8')

    expect(runtimeSource).toContain(':runtime-crud-loading="isPageBlockRuntimeCrudLoading(block)"')
    expect(runtimeSource).toContain('warmCurrentPortalPageCrud')
    expect(runtimeSource).toContain('seed-runtime-crud-props')
    expect(runtimeSource).toContain('buildRuntimeWarmChunks')
    expect(runtimeSource).toContain('import(\'@/components/ai-form/AiCrudPage.vue\')')
    expect(runtimeSource).toContain('import(\'@/components/lowcode-builder/page/RuntimeListGridFlow.vue\')')
    expect(runtimeSource).toContain('editCanvasBootstrapping')
    expect(runtimeSource).toContain('resolveEntryDesignTab(pageId) === \'page\'')
    expect(portalSource).toContain('seedRuntimeCrudProps')
    expect(portalSource).toContain('portal-content-skeleton')
    expect(rendererSource).toContain('<div v-if="runtimeCrudLoading" class="runtime-crud-loading">')
    expect(rendererSource).toContain('<n-skeleton height="32px" :sharp="false" />')
    expect(rendererSource).not.toContain('<n-spin size="small" />')
    expect(rendererSource).toContain('shouldRenderRuntimeListGridShell')
    expect(rendererSource).toContain('RuntimeListGridFlow')
    expect(rendererSource).not.toContain('ListPageGridDesigner')
    expect(rendererSource).toContain('v-else-if="effectiveRuntimeCrudProps"')
    // 自由布局首屏：AiCrudPage 异步注册，避免 GridBlockRenderer 同步拖入整棵 CRUD 依赖树
    expect(rendererSource).toContain("import('@/components/ai-form/AiCrudPage.vue')")
    expect(rendererSource).toContain('blockAsyncLoader')
  })

  it('keeps list design tab after refresh when nodes are not loaded yet', () => {
    const runtimeSource = readApplicationRuntimeSource()
    expect(runtimeSource).toContain('// 刷新时 nodes 尚未加载：先信任 URL designTab，避免 list 被误判成 page 落到空白对象卡')
    expect(runtimeSource).toContain('if (!node && PAGE_DESIGN_TABS.has(normalized))')
    expect(runtimeSource).toContain('// 应用节点加载完成后，按 URL designTab 重新对齐')
    expect(runtimeSource).toContain('|| (requestedPageId && [\'list\', \'form\', \'settings\', \'publish\'].includes(designTab))')
    expect(runtimeSource).toContain('// 编辑态 URL 已明确指向某页时，不要静默落到首页对象页')
  })

  it('keeps a designer placeholder while a newly created page is being mounted', () => {
    const runtimeSource = readApplicationRuntimeSource()
    expect(runtimeSource).toContain('designerTransitionLoading')
    expect(runtimeSource).toContain('正在保存页面草稿并挂载表单资产，请稍候')
    expect(runtimeSource).toContain('await nextTick()')
  })

  it('wires runtime tree selection back into the page CRUD filter', () => {
    const runtimeSource = readApplicationRuntimeSource()
    const rendererSource = readGridBlockRendererSource()
    const portalSource = readFileSync(resolve('src/views/app-center/components/portal/PortalPageRenderer.vue'), 'utf8')
    expect(runtimeSource).toContain('@runtime-tree-select="handleRuntimeTreeSelect"')
    expect(runtimeSource).toContain('const runtimeTreeFilter = ref({})')
    expect(runtimeSource).toContain('runtimeTreeFilterByBlockId')
    expect(runtimeSource).toContain('publicParams: {')
    expect(runtimeSource).toContain('block.blockType !== \'tree-panel\'')
    expect(rendererSource).toContain('blockId: props.block.id')
    expect(rendererSource).toContain('@runtime-tree-select="emit(\'runtimeTreeSelect\', $event)"')
    expect(portalSource).toContain('@runtime-tree-select="handleRuntimeTreeSelect"')
    expect(portalSource).toContain(':runtime-tree-active-key="runtimeTreeActiveKey"')
    expect(portalSource).toContain('function isRuntimeDataBlock')
    expect(portalSource).toContain('const runtimeTreeFilter = ref({})')
  })

  it('keeps nested tab blocks selectable and configurable from the runtime canvas', () => {
    const runtimeSource = readApplicationRuntimeSource()
    const rendererSource = readGridBlockRendererSource()
    const designerSource = readListPageGridDesignerSource()

    expect(runtimeSource).toContain('@child-block-select="handleNestedPageBlockSelect"')
    expect(runtimeSource).toContain('@child-block-menu-select="handleNestedPageBlockMenuSelect"')
    expect(runtimeSource).toContain('@child-block-resize-start="handleNestedPageBlockResizeStart"')
    expect(runtimeSource).toContain('findPageBlockInTree(pageBlocks.value, selectedPageBlockId.value)')
    expect(rendererSource).toContain('class="tab-pane-child"')
    expect(rendererSource).toContain('emit(\'childBlockSelect\', child.id)')
    expect(rendererSource).toContain('class="nested-block-menu-trigger"')
    expect(rendererSource).toContain('v-for="anchor in resizeAnchors"')
    expect(designerSource).toContain('findBlockInTree(blocks.value, blockId)')
    expect(designerSource).toContain('blockId !== previousId || !selectedBlockId.value')
    expect(designerSource).not.toContain('if (blockId && blocks.value.some(block => block.id === blockId))')
  })
})
