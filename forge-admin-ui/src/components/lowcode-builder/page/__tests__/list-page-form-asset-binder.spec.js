import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { readSplitVueSource } from '@/test-utils/read-split-vue-source'

describe('list page form asset binder', () => {
  it('应用运行态只保留顶部页面资源设为新增编辑，不再放右侧下拉', () => {
    const vueSource = readFileSync(
      resolve('src/views/app-center/application-runtime.[applicationCode].vue'),
      'utf8',
    )
    expect(vueSource).toContain('设为新增编辑表单')
    expect(vueSource).toContain('setFormAssetAsListCrudForm')
    expect(vueSource).toContain('取消绑定')
    expect(vueSource).toContain('clearFormAssetListCrudBinding')
    expect(vueSource).toContain('formatFormAssetUsageSummary')
    expect(vueSource).toContain('selectedPageBlockBoundFormAsset')
    expect(vueSource).not.toContain('page-form-bind-card')
    expect(vueSource).not.toContain('page-form-draft-card')
    expect(vueSource).toContain('请在顶部「页面资源」里点「设为新增编辑表单」')
  })

  it('配置组件属性首屏不再重复表单下拉', () => {
    const propsTab = readFileSync(
      resolve('src/components/lowcode-builder/page/ListPagePropertyPropsTabPart1.vue'),
      'utf8',
    )
    const designer = readSplitVueSource(
      'src/components/lowcode-builder/page/ListPageGridDesigner.vue',
      'useListPageGridDesigner',
    )
    expect(propsTab).not.toContain('page-form-asset-binder')
    expect(designer).toContain('pageFormAssetOptions')
  })
})
