import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

/**
 * 列表设计器预览必须锁定列表页布局，不能把「页面2」画布串进预览。
 * 用源码契约测试锁死 openLocalPreview / 模板回落行为，避免回归。
 */
describe('list designer preview page lock', () => {
  const part2 = readFileSync(
    resolve(__dirname, '../composables/useBusinessListDesigner.part2.js'),
    'utf8',
  )
  const vueSource = readFileSync(
    resolve(__dirname, '../BusinessListDesigner.vue'),
    'utf8',
  )
  const runtimePreview = readFileSync(
    resolve(__dirname, '../../../runtime-modules/useApplicationRuntime.part5.js'),
    'utf8',
  )

  it('defines resolveListPagePreviewLayout that prefers pageKey/pageType list', () => {
    expect(part2).toContain('function resolveListPagePreviewLayout')
    expect(part2).toContain("page?.pageKey === 'list' || page?.pageType === 'list'")
    expect(part2).toContain('listPage?.gridLayout || localSchema.value?.listGridLayout')
  })

  it('openLocalPreview only uses live canvas when active pageKey is list', () => {
    expect(part2).toContain('const onListPage = activePageKey.value === \'list\'')
    expect(part2).toContain('activeDesignerPage.value?.pageKey === \'list\'')
    expect(part2).not.toContain('activeDesignerPage.value?.pageType === \'list\'')
    expect(part2).toContain('resolveListPagePreviewLayout()')
    expect(part2).toContain('多页面时预览必须锁定「列表页」布局')
  })

  it('listOnlyPreviewLayout never falls back to active-page designerPreviewGridLayout', () => {
    expect(part2).toContain('const listOnlyPreviewLayout = computed')
    expect(part2).toContain('禁止 designerPreviewGridLayout')
    expect(vueSource).toContain(':model-value="listOnlyPreviewLayout"')
    expect(vueSource).not.toContain('listPreviewLayout || designerPreviewGridLayout')
    expect(vueSource).toContain(':page-name="listPreviewPageName"')
    expect(vueSource).toContain('listPreviewWatermark')
  })

  it('application draft preview keeps in-place navigation and locks pageKey to list', () => {
    expect(runtimePreview).toContain('buildDraftPreviewQuery')
    expect(runtimePreview).toContain("pageKey: 'list'")
    expect(runtimePreview).toContain('openDraftPreviewInPlace')
    // 顶栏预览不能改成列表设计器弹窗（会慢、切 Tab 卡住）
    expect(runtimePreview).not.toContain('forge-list-designer:preview-current-list')
  })
})
