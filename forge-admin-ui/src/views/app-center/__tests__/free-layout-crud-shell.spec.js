import fs from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

function readSource(relativePath) {
  return fs.readFileSync(path.resolve(process.cwd(), relativePath), 'utf8')
}

/**
 * 自由布局页上的 AiCrudPage 必须走裸组件（属性面板可直接控制工具栏/样式），
 * 禁止再嵌套对象列表伴生块 RuntimeListGridFlow。
 */
describe('free layout CRUD shell suppress', () => {
  it('application free-layout canvas suppresses runtime list grid shell', () => {
    const runtime = readSource('src/views/app-center/application-runtime.[applicationCode].vue')
    expect(runtime).toContain(':suppress-runtime-list-grid="isFreeLayoutPage"')
  })

  it('portal multi-block pages suppress embedded CRUD list grid shell', () => {
    const portal = readSource('src/views/app-center/components/portal/PortalPageRenderer.vue')
    expect(portal).toContain('suppressEmbeddedCrudListGrid')
    expect(portal).toContain('blocks.value.length > 1')
    expect(portal).toContain(':suppress-runtime-list-grid="suppressEmbeddedCrudListGrid"')
  })

  it('GridBlockRenderer honors suppressRuntimeListGrid before opening list shell', () => {
    const composable = readSource('src/components/lowcode-builder/page/composables/useGridBlockRenderer.js')
    expect(composable).toContain('if (props.suppressRuntimeListGrid)')
    expect(composable).toContain('shouldRenderRuntimeListGridShell')
  })

  it('free-layout CRUD prefers block form chrome over object runtime', () => {
    const composable = readSource('src/components/lowcode-builder/page/composables/useGridBlockRenderer.js')
    expect(composable).toContain('preferBlockFormChrome: props.suppressRuntimeListGrid === true')
    const merge = readSource('src/components/lowcode-builder/page/runtime-crud-block-props.js')
    expect(merge).toContain('preferBlockFormChrome')
  })

  it('selecting a canvas block opens the properties inspector', () => {
    const runtime = readSource('src/views/app-center/runtime-modules/useApplicationRuntime.part2.js')
    expect(runtime).toMatch(/function selectPageBlock[\s\S]*configPanelVisible\.value = true/)
  })

  it('panelOnly frame edits sync pageFlowX/Y/Width/Height for free-layout canvas', () => {
    const part4 = readSource('src/components/lowcode-builder/page/composables/useListPageGridDesigner.part4.js')
    expect(part4).toContain('function buildPageFlowStyleFromFrame')
    expect(part4).toContain('function buildPanelOnlyPageFlowStyle')
    expect(part4).toContain('pageFlowX:')
    expect(part4).toContain('pageFlowY:')
    expect(part4).toContain('pageFlowHeight:')
    expect(part4).toContain('buildPageFlowStyleFromFrame(next, nextWidthMode)')
    expect(part4).toContain('findBlockInTree(blocks.value, id)')
    const part2 = readSource('src/components/lowcode-builder/page/composables/useListPageGridDesigner.part2.js')
    expect(part2).toContain('props.panelOnly && Number.isFinite(flowX)')
    expect(part2).toContain('componentStyle.pageFlowWidth')
  })

  it('width/height mode toggles update nested blocks via findBlockInTree', () => {
    const part4 = readSource('src/components/lowcode-builder/page/composables/useListPageGridDesigner.part4.js')
    const widthFn = part4.slice(part4.indexOf('function setBlockWidthMode'), part4.indexOf('function setBlockHeightMode'))
    const heightFn = part4.slice(part4.indexOf('function setBlockHeightMode'), part4.indexOf('function setBlockContentAlign'))
    expect(widthFn).toContain('findBlockInTree(blocks.value, id)')
    expect(heightFn).toContain('findBlockInTree(blocks.value, id)')
    expect(widthFn).toContain('mapBlocksInTree(blocks.value')
    expect(heightFn).toContain('mapBlocksInTree(blocks.value')
    // 禁止再只找顶层：嵌套图表会静默失败
    expect(widthFn).not.toContain('blocks.value.find(block => block.id === id)')
    expect(heightFn).not.toContain('blocks.value.find(block => block.id === id)')
  })

  it('free-layout runtime preview keeps scrollable page surface without fill-host lock', () => {
    const runtime = readSource('src/views/app-center/application-runtime.[applicationCode].vue')
    expect(runtime).toContain(":class=\"{ 'is-fill': !isFreeLayoutPage }\"")
    expect(runtime).toContain(':fill-host="!isFreeLayoutPage"')
    expect(runtime).toContain('pageSurfaceHostStyle')
  })

  it('fill-host inline form keeps constrained height so form body can scroll', () => {
    const portal = readSource('src/views/app-center/components/portal/PortalPageRenderer.vue')
    const fillBlockRule = portal.match(
      /\.portal-page-renderer\.is-fill\s+\.portal-page-block:has\(\.ai-crud-page\.is-inline-form-active\)\s*\{[^}]+\}/,
    )?.[0] || ''
    // fill 宿主：块用 height:0 + flex 锁高，滚动交给 .inline-form-panel-body
    expect(fillBlockRule).toContain('height: 0 !important')
    expect(fillBlockRule).not.toContain('height: auto')
    expect(portal).toMatch(
      /\.portal-page-renderer\.is-fill\s+\.portal-page-block:has\(\.ai-crud-page\.is-inline-form-active\)\s+:deep\(\.inline-form-panel-body\)\s*\{[\s\S]*?overflow:\s*auto/,
    )
  })

  it('fill-host formOnly pages scroll on page-surface instead of clipping', () => {
    const portal = readSource('src/views/app-center/components/portal/PortalPageRenderer.vue')
    const canvas = readSource('src/views/app-center/runtime-modules/application-runtime-canvas.css')
    expect(portal).toContain('.portal-page-renderer.is-fill:has(.ai-crud-page.is-form-only)')
    expect(portal).toMatch(/is-form-only[\s\S]*?overflow:\s*visible\s*!important/)
    expect(canvas).toContain('.page-surface.is-fill:has(.ai-crud-page.is-form-only)')
    expect(canvas).toMatch(/\.page-surface\.is-fill:has\(\.ai-crud-page\.is-form-only\)\s*\{[\s\S]*?overflow:\s*auto/)
  })

  it('card surface applies style.borderRadius so appearance panel radius takes effect', () => {
    const renderer = readSource('src/components/lowcode-builder/page/composables/useGridBlockRenderer.js')
    expect(renderer).toMatch(/layoutCardStyle[\s\S]*borderRadius:\s*toCssSize/)
    expect(renderer).toContain('style.borderRadius')
  })

  it('panel-box is included in free-layout container drop hit-testing', () => {
    const part3 = readSource('src/views/app-center/runtime-modules/useApplicationRuntime.part3.js')
    expect(part3).toContain('data-page-block-type="panel-box"')
    expect(part3).toMatch(/blockType === 'card' \|\| blockType === 'box-layout' \|\| blockType === 'panel-box'/)
  })

  it('data-table renders live AiCrudPage when runtime object props exist', () => {
    const renderer = readSource('src/components/lowcode-builder/page/GridBlockRenderer.vue')
    expect(renderer).toContain("block.blockType === 'data-table'")
    expect(renderer).toContain('effectiveRuntimeCrudProps')
    expect(renderer).toContain('选择数据源')
  })

  it('panelOnly style tab exposes appearance controls (not gated by panelOnly)', () => {
    const styleTab = readSource('src/components/lowcode-builder/page/ListPagePropertyStyleTab.vue')
    expect(styleTab).toContain('外观样式')
    expect(styleTab).toContain('文字排版')
    expect(styleTab).toContain('透明度 (Opacity)')
    expect(styleTab).not.toContain('v-if="!panelOnly"')
  })

  it('page inspector panel scrolls when content exceeds viewport', () => {
    const canvasCss = readSource('src/views/app-center/runtime-modules/application-runtime-canvas.css')
    expect(canvasCss).toMatch(/\.page-canvas-padding-panel\s*\{[\s\S]*?overflow:\s*auto/)
    const shellCss = readSource('src/views/app-center/runtime-modules/application-runtime-shell.css')
    expect(shellCss).toMatch(/\.runtime-inspector\s*\{[\s\S]*?overflow:\s*hidden/)
    expect(shellCss).toContain('.runtime-inspector > .application-form-source-config')
  })

  it('free-layout syncs measured grow-block heights then resolves collisions', () => {
    const geometry = readSource('src/views/app-center/runtime-modules/page-flow-geometry.js')
    expect(geometry).toContain('function applyMeasuredPageBlockHeights')
    expect(geometry).toContain('function isPageFlowGrowBlock')
    const part4 = readSource('src/views/app-center/runtime-modules/useApplicationRuntime.part4.js')
    expect(part4).toContain('syncFreeLayoutMeasuredHeights')
    expect(part4).toContain('editCanvasBootstrapping')
    expect(part4).toContain('resolveCollisions: true')
  })

  it('readonly blockStyle strips accidental selection chrome', () => {
    const composable = readSource('src/components/lowcode-builder/page/composables/useGridBlockRenderer.js')
    expect(composable).toContain('stripAccidentalSelectionChrome')
    expect(composable).toContain('props.readonly')
    const part4 = readSource('src/components/lowcode-builder/page/composables/useListPageGridDesigner.part4.js')
    expect(part4).toContain('selectionChromeClearPatch')
    expect(part4).toContain('isAccidentalSelectionChrome')
  })

  it('catalog click keeps popover open and can nest into selected container', () => {
    const part2 = readSource('src/views/app-center/runtime-modules/useApplicationRuntime.part2.js')
    expect(part2).toMatch(/function insertComponent[\s\S]*componentPopoverVisible\.value = true/)
    expect(part2).toContain("['card', 'panel-box', 'box-layout'].includes(selected.blockType)")
    expect(part2).toContain('appendPageBlockToTab')
    expect(part2).toContain('appendPageBlockToGridCell')
    expect(part2).toContain('.page-surface')
    expect(part2).toContain('function scrollPageBlockIntoView')
  })

  it('page-block chrome mounts only on hover/selected to keep selection snappy', () => {
    const runtime = readSource('src/views/app-center/application-runtime.[applicationCode].vue')
    expect(runtime).toContain('hoveredPageBlockId')
    expect(runtime).toContain('selectedPageBlockId === block.id || hoveredPageBlockId === block.id')
    const part2 = readSource('src/views/app-center/runtime-modules/useApplicationRuntime.part2.js')
    expect(part2).toMatch(/function openPageBlockConfiguration[\s\S]*requestAnimationFrame/)
    const grid = readSource('src/components/lowcode-builder/page/composables/useGridBlockRenderer.js')
    expect(grid).toContain('function showNestedChrome')
    expect(grid).toContain('hoveredNestedBlockId')
  })

  it('catalog nest only uses canvas-pinned container host', () => {
    const part2 = readSource('src/views/app-center/runtime-modules/useApplicationRuntime.part2.js')
    expect(part2).toContain('nestHostPinnedId')
    expect(part2).toMatch(/function insertComponent[\s\S]*pinnedId/)
    expect(part2).toMatch(/function openPageBlockConfiguration[\s\S]*nestHostPinnedId\.value/)
  })

  it('nested click prefers card/panel host before selecting inner chart', () => {
    const part2 = readSource('src/views/app-center/runtime-modules/useApplicationRuntime.part2.js')
    expect(part2).toContain('findDirectParentPageBlock')
    expect(part2).toContain('NEST_CONTENT_HOST_TYPES')
    expect(part2).toMatch(/function handleNestedPageBlockSelect[\s\S]*NEST_CONTENT_HOST_TYPES\.includes\(parent\.blockType\)/)
    const tree = readSource('src/views/app-center/runtime-modules/page-block-tree.js')
    expect(tree).toContain('export function findDirectParentPageBlock')
    expect(tree).toContain("['card', 'panel-box', 'box-layout']")
  })

  it('catalog pointerup without move does not close popover before click', () => {
    const part3 = readSource('src/views/app-center/runtime-modules/useApplicationRuntime.part3.js')
    expect(part3).toMatch(/if \(!ctx\?\.moved\) \{[\s\S]*catalogDragBlockType\.value = ''[\s\S]*return/)
    const finish = part3.slice(part3.indexOf('function finishCatalogPointerDrag'))
    const movedCloseIdx = finish.indexOf('componentPopoverVisible.value = false')
    const earlyReturnIdx = finish.indexOf('if (!ctx?.moved)')
    expect(movedCloseIdx).toBeGreaterThan(earlyReturnIdx)
  })
})
