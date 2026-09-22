import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it, vi } from 'vitest'
import {
  createApplicationRuntimeLoadCoordinator,
  resolveApplicationRuntimeLoadKey,
  shouldUseApplicationWorkspaceLoad,
} from '../application-runtime-load'

describe('application runtime route loading', () => {
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

  it('uses workspace for edit, draft preview, or editors in page management', () => {
    expect(shouldUseApplicationWorkspaceLoad({ query: {} })).toBe(false)
    expect(shouldUseApplicationWorkspaceLoad({ query: { pageId: 'page_1' } })).toBe(false)
    expect(shouldUseApplicationWorkspaceLoad({ query: { pageId: 'page_1' } }, true)).toBe(true)
    expect(shouldUseApplicationWorkspaceLoad({ query: { edit: '1' } })).toBe(true)
    expect(shouldUseApplicationWorkspaceLoad({ query: { draft: '1' } })).toBe(true)
  })

  it('loads workspace for editors in page management; published runtime only for viewers', () => {
    const runtimeSource = readFileSync(resolve('src/views/app-center/application-runtime.[applicationCode].vue'), 'utf8')
    expect(runtimeSource).toContain('businessApplicationRuntimeByCode')
    expect(runtimeSource).toContain('shouldUseApplicationWorkspaceLoad(route, canEditApplication.value)')
    // 有编辑权限的页面管理也要 designPreview，否则保存的默认值只在发布后才进正式快照
    expect(runtimeSource).toContain(':design-preview="editing || isDraftMode || canEditApplication"')
    expect(runtimeSource).toContain("import.meta.glob('/src/assets/images/form/*.png', { import: 'default' })")
    expect(runtimeSource).not.toContain('eager: true')
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

  it('waits for object runtime config before mounting the CRUD page', () => {
    const runtimeSource = readFileSync(resolve('src/views/app-center/application-runtime.[applicationCode].vue'), 'utf8')
    const rendererSource = readFileSync(resolve('src/components/lowcode-builder/page/GridBlockRenderer.vue'), 'utf8')

    expect(runtimeSource).toContain(':runtime-crud-loading="isPageBlockRuntimeCrudLoading(block)"')
    expect(rendererSource).toContain('<div v-if="runtimeCrudLoading" class="runtime-crud-loading">')
    expect(rendererSource).toContain('<n-skeleton height="32px" :sharp="false" />')
    expect(rendererSource).not.toContain('<n-spin size="small" />')
    expect(rendererSource).toContain('v-else-if="effectiveRuntimeCrudProps"')
    expect(rendererSource).toContain("defineAsyncComponent(() => import('@/components/ai-form/AiCrudPage.vue'))")
  })

  it('keeps nested tab blocks selectable and configurable from the runtime canvas', () => {
    const runtimeSource = readFileSync(resolve('src/views/app-center/application-runtime.[applicationCode].vue'), 'utf8')
    const rendererSource = readFileSync(resolve('src/components/lowcode-builder/page/GridBlockRenderer.vue'), 'utf8')
    const designerSource = readFileSync(resolve('src/components/lowcode-builder/page/ListPageGridDesigner.vue'), 'utf8')

    expect(runtimeSource).toContain('@child-block-select="handleNestedPageBlockSelect"')
    expect(runtimeSource).toContain('@child-block-menu-select="handleNestedPageBlockMenuSelect"')
    expect(runtimeSource).toContain('@child-block-resize-start="handleNestedPageBlockResizeStart"')
    expect(runtimeSource).toContain('findPageBlockInTree(pageBlocks.value, selectedPageBlockId.value)')
    expect(rendererSource).toContain('class="tab-pane-child"')
    expect(rendererSource).toContain('emit(\'childBlockSelect\', child.id)')
    expect(rendererSource).toContain('class="nested-block-menu-trigger"')
    expect(rendererSource).toContain('v-for="anchor in resizeAnchors"')
    expect(designerSource).toContain('if (blockId && findBlockInTree(blocks.value, blockId))')
    expect(designerSource).not.toContain('if (blockId && blocks.value.some(block => block.id === blockId))')
  })
})
