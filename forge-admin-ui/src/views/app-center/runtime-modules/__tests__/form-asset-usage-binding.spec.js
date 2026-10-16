import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('form asset usage / unbind closed loop', () => {
  const part2 = readFileSync(
    resolve('src/views/app-center/runtime-modules/useApplicationRuntime.part2.js'),
    'utf8',
  )
  const vueSource = readFileSync(
    resolve('src/views/app-center/application-runtime.[applicationCode].vue'),
    'utf8',
  )

  it('supports collect / format / clear binding helpers', () => {
    expect(part2).toContain('function collectFormAssetUsages')
    expect(part2).toContain('function formatFormAssetUsageSummary')
    expect(part2).toContain('function focusFormAssetUsage')
    expect(part2).toContain('function findFormAssetByIdentity')
    expect(part2).toContain('formAssetIdMatches')
    expect(part2).toContain('本页有多个数据列表')
    expect(part2).toContain('function clearFormAssetListCrudBinding')
    expect(part2).toContain('尚未绑定到任何组件')
    expect(part2).toContain('applyFormAssetToBlock(item, \'\')')
  })

  it('page resources UI exposes bind, usage chips and unbind', () => {
    expect(vueSource).toContain('设为新增编辑表单')
    expect(vueSource).toContain('取消绑定')
    expect(vueSource).toContain('collectFormAssetUsages')
    expect(vueSource).toContain('focusFormAssetUsage')
    expect(vueSource).toContain('application-form-asset-usage-chip')
    expect(vueSource).toContain('clearFormAssetListCrudBinding')
    expect(vueSource).toContain('selectedPageBlockBoundFormAsset')
    expect(vueSource).toContain('数据列表需要绑定新增/编辑表单')
  })

  it('openFormAssetDesigner normalizes id before entering designer', () => {
    expect(part2).toContain('统一落到资产 id')
    expect(part2).toContain('表单不存在或尚未加载完成')
  })

  it('clearing form asset does not wipe existing fieldRefs', () => {
    const applyStart = part2.indexOf('function applyFormAssetToBlock')
    const applyEnd = part2.indexOf('function updateSelectedBlockFormAsset', applyStart)
    const applyBody = part2.slice(applyStart, applyEnd)
    expect(applyBody).toContain('// 清空绑定时只摘掉 formAssetId')
    expect(applyBody).toContain('formAssetId: \'\'')
    expect(applyBody).toContain('formAssetFieldsInitialized: false')
  })
})
