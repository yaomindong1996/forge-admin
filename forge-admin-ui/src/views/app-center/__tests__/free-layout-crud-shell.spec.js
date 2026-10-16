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
    expect(part4).toContain('function buildPanelOnlyPageFlowStyle')
    expect(part4).toContain('pageFlowX:')
    expect(part4).toContain('pageFlowY:')
    expect(part4).toContain('pageFlowHeight:')
    expect(part4).toContain('...buildPanelOnlyPageFlowStyle(next, nextWidthMode)')
    const part2 = readSource('src/components/lowcode-builder/page/composables/useListPageGridDesigner.part2.js')
    expect(part2).toContain('props.panelOnly && Number.isFinite(flowX)')
    expect(part2).toContain('componentStyle.pageFlowWidth')
  })

  it('free-layout runtime preview keeps scrollable page surface without fill-host lock', () => {
    const runtime = readSource('src/views/app-center/application-runtime.[applicationCode].vue')
    expect(runtime).toContain(":class=\"{ 'is-fill': !isFreeLayoutPage }\"")
    expect(runtime).toContain(':fill-host="!isFreeLayoutPage"')
  })

  it('panelOnly style tab exposes appearance controls (not gated by panelOnly)', () => {
    const styleTab = readSource('src/components/lowcode-builder/page/ListPagePropertyStyleTab.vue')
    expect(styleTab).toContain('外观样式')
    expect(styleTab).toContain('文字排版')
    expect(styleTab).toContain('透明度 (Opacity)')
    expect(styleTab).not.toContain('v-if="!panelOnly"')
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
    expect(part2).toContain("['card', 'box-layout'].includes(selected.blockType)")
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

  it('catalog pointerup without move does not close popover before click', () => {
    const part3 = readSource('src/views/app-center/runtime-modules/useApplicationRuntime.part3.js')
    expect(part3).toMatch(/if \(!ctx\?\.moved\) \{[\s\S]*catalogDragBlockType\.value = ''[\s\S]*return/)
    const finish = part3.slice(part3.indexOf('function finishCatalogPointerDrag'))
    const movedCloseIdx = finish.indexOf('componentPopoverVisible.value = false')
    const earlyReturnIdx = finish.indexOf('if (!ctx?.moved)')
    expect(movedCloseIdx).toBeGreaterThan(earlyReturnIdx)
  })
})
