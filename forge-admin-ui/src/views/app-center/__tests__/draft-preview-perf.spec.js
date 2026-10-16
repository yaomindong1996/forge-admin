import fs from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

function readSource(relativePath) {
  return fs.readFileSync(path.resolve(process.cwd(), relativePath), 'utf8')
}

/**
 * 列表/表单设计点「预览」必须轻量：仅脏时保存、预览落盘不重挂设计器。
 */
describe('draft preview performance contracts', () => {
  it('preview save skips embedded designer reload and heavy workspace refresh', () => {
    const part4 = readSource('src/views/app-center/runtime-modules/useApplicationRuntime.part4.js')
    const objectDesigner = readSource('src/views/app-center/composables/useObjectDesigner.part1.js')
    expect(part4).toContain('suppressHeavyEmbeddedSaveSideEffects')
    expect(part4).toContain('forPreview')
    expect(part4).toContain('reload: !forPreview')
    expect(part4).toContain('预览落盘：跳过 saved 回调里的 workspace 全量刷新')
    expect(objectDesigner).toContain('const reload = options.reload !== false')
    expect(objectDesigner).toContain('if (reload)')
  })

  it('keeps list/form designer instances across draft preview with v-show', () => {
    const runtime = readSource('src/views/app-center/application-runtime.[applicationCode].vue')
    const part4 = readSource('src/views/app-center/runtime-modules/useApplicationRuntime.part4.js')
    expect(part4).toContain('keepListDesignerAlive')
    expect(part4).toContain('keepFormDesignerAlive')
    expect(runtime).toContain('v-if="keepListDesignerAlive"')
    expect(runtime).toContain('v-show="showListDesignerSurface"')
    expect(runtime).toContain('v-if="keepFormDesignerAlive"')
    expect(runtime).toContain('v-show="showFormDesignerSurface"')
  })

  it('keeps draft portal warm short and prefetches preview chunks before navigation', () => {
    const part1 = readSource('src/views/app-center/runtime-modules/useApplicationRuntime.part1.js')
    const part5 = readSource('src/views/app-center/runtime-modules/useApplicationRuntime.part5.js')
    const localComps = readSource('src/components/lowcode-builder/page/gridBlockRendererLocalComponents.js')
    expect(part1).toContain('timeoutMs: editing.value || isDraftMode.value ? 2400 : 3200')
    expect(part5).toContain('function prefetchDraftPreviewChunks')
    expect(part5).toContain('prefetchDraftPreviewChunks()')
    expect(localComps).toContain('blockAsyncLoader')
    expect(localComps).toContain("import('@/components/ai-form/AiCrudPage.vue')")
  })

  it('warms edit-entry chunks by design tab instead of always pulling free-layout stack', () => {
    const part1 = readSource('src/views/app-center/runtime-modules/useApplicationRuntime.part1.js')
    const formDesigner = readSource('src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue')
    const formPart1 = readSource('src/views/app-center/components/designer/forge-form-designer/composables/useForgeFormDesignerPage.part1.js')
    expect(part1).toContain('function buildRuntimeWarmChunks')
    expect(part1).toContain("tab === 'form'")
    expect(part1).toContain('ForgeFormDesigner.vue')
    expect(part1).toContain('setTimeout(resolve, 900)')
    expect(formDesigner).toContain('shelfHydrated')
    expect(formPart1).toContain('shelfHydrated')
    expect(formPart1).toContain('requestIdleCallback(hydrateShelf')
  })
})
