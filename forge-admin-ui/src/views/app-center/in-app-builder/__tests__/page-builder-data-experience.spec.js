import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { readSplitVueSource } from '@/test-utils/read-split-vue-source'
import { inAppPageTemplateCatalog } from '../page-template-catalog'

describe('page builder data experience', () => {
  it('describes every data template as a page-canvas workflow', () => {
    const dataTemplates = inAppPageTemplateCatalog.filter(template => template.dataTemplate)
    const descriptions = dataTemplates.map(template => template.description)

    expect(descriptions).toHaveLength(4)
    expect(dataTemplates.every(template => template.pageType === 'object')).toBe(true)
    expect(descriptions.filter(description => description.includes('页面画布'))).toHaveLength(2)
  })

  it('keeps a newly created data template on the page canvas', () => {
    const source = readSplitVueSource('src/views/app-center/application-runtime.[applicationCode].vue', 'useApplicationRuntime')
    const createFormStart = source.indexOf('function createFormAssetForPageCrud(pageId)')
    expect(createFormStart).toBeGreaterThanOrEqual(0)
    const createFormEnd = source.indexOf('\n  function resolveNextNavigationTitle', createFormStart)
    expect(createFormEnd).toBeGreaterThan(createFormStart)
    const createFormSource = source.slice(createFormStart, createFormEnd)

    expect(createFormSource).toContain('configPanelVisible.value = true')
    expect(createFormSource).toContain('inspectorTab.value = \'data\'')
    expect(createFormSource).toContain('formDesignerMode.value = false')
    expect(createFormSource).not.toContain('formDesignerMode.value = true')
  })

  it('lets page creation choose a runtime datasource and import an existing table', () => {
    const source = readFileSync(resolve('src/views/app-center/components/designer/PageTypeSelector.vue'), 'utf8')
    expect(source).toContain("genDatasourceEnabled('LOWCODE_RUNTIME')")
    expect(source).toContain('引用现有数据表')
    expect(source).toContain('inferFormFieldsFromColumns')
  })

  it('opens the data inspector when a block asks to select its source', () => {
    const source = readSplitVueSource('src/views/app-center/application-runtime.[applicationCode].vue', 'useApplicationRuntime')

    expect(source).toContain('@request-data-source="handlePageBlockDataSourceRequest"')
    expect(source).toMatch(/function handlePageBlockDataSourceRequest|handlePageBlockDataSourceRequest =/)
    expect(source).toContain('@click="openSelectedBlockFormDesigner"')
  })

  it('uses top 页面资源 as the only add/edit form binder entry', () => {
    const vueSource = readFileSync(
      resolve('src/views/app-center/application-runtime.[applicationCode].vue'),
      'utf8',
    )
    expect(vueSource).toContain('设为新增编辑表单')
    expect(vueSource).toContain('setFormAssetAsListCrudForm')
    expect(vueSource).not.toContain('page-form-bind-card')
    expect(vueSource).not.toContain('page-form-draft-card')
    expect(vueSource).toContain('请在顶部「页面资源」里点「设为新增编辑表单」')
  })

  it('lets page resources set a form as list add/edit form', () => {
    const vueSource = readFileSync(
      resolve('src/views/app-center/application-runtime.[applicationCode].vue'),
      'utf8',
    )
    const runtimeSource = readSplitVueSource('src/views/app-center/application-runtime.[applicationCode].vue', 'useApplicationRuntime')
    const portalSource = readFileSync(
      resolve('src/views/app-center/components/portal/PortalPageRenderer.vue'),
      'utf8',
    )
    expect(vueSource).toContain('设为新增编辑表单')
    expect(vueSource).toContain('setFormAssetAsListCrudForm')
    expect(vueSource).toContain('取消绑定')
    expect(vueSource).toContain('clearFormAssetListCrudBinding')
    expect(vueSource).toContain('focusFormAssetUsage')
    expect(vueSource).toContain('collectFormAssetUsages')
    expect(vueSource).toContain(':form-assets="formAssets"')
    expect(runtimeSource).toMatch(/function setFormAssetAsListCrudForm|setFormAssetAsListCrudForm =/)
    expect(runtimeSource).toMatch(/function clearFormAssetListCrudBinding|clearFormAssetListCrudBinding =/)
    expect(runtimeSource).toMatch(/function collectFormAssetUsages|collectFormAssetUsages =/)
    expect(runtimeSource).toMatch(/function focusFormAssetUsage|focusFormAssetUsage =/)
    expect(runtimeSource).toContain('已将「')
    expect(runtimeSource).toContain('已取消「')
    expect(runtimeSource).toContain('overlayFormDesignerOnRuntimeCrudProps')
    expect(portalSource).toContain('overlayFormDesignerOnRuntimeCrudProps')
    expect(portalSource).toContain('resolveBoundFormDesignerSchema')
  })

  it('clears object-specific field configuration only when the business object changes', () => {
    const source = readSplitVueSource('src/views/app-center/application-runtime.[applicationCode].vue', 'useApplicationRuntime')
    const switchStart = source.indexOf('function updateSelectedPageBlockRuntimeObject(objectId)')
    const altStart = source.indexOf('updateSelectedPageBlockRuntimeObject =')
    const start = switchStart >= 0 ? switchStart : altStart
    const switchEnd = source.indexOf('\nfunction createFormAssetForSelectedBlock', start)
    const altEnd = source.indexOf('createFormAssetForSelectedBlock =', start)
    const end = switchEnd > 0 ? switchEnd : (altEnd > 0 ? altEnd : start + 3000)
    const switchSource = source.slice(start, end)

    expect(switchSource).toContain('const objectChanged = previousObjectKey !== nextObjectKey')
    expect(switchSource).toContain('fieldRefs: objectChanged ? [] : selectedPageBlock.value.fieldRefs')
    expect(switchSource).toContain('delete nextProps.fieldSettings')
    expect(switchSource).toContain('delete nextProps.searchFieldRefs')
    expect(switchSource).toContain('delete nextProps.searchFieldSettings')
  })

  it('preloads the full block tree and forwards object-scoped resolvers to nested blocks', () => {
    const runtimeSource = readSplitVueSource('src/views/app-center/application-runtime.[applicationCode].vue', 'useApplicationRuntime')
    const rendererSource = readSplitVueSource('src/components/lowcode-builder/page/GridBlockRenderer.vue', 'useGridBlockRenderer')

    expect(runtimeSource).toContain('visitPageBlocksInTree(pageBlocks.value, preloadPageBlockCrudRuntimeProps)')
    expect(runtimeSource).toContain('runtimeCrudLoadingObjectIds.has(cacheKey)')
    expect(runtimeSource).toContain(':block-fields-resolver="resolvePageBlockFields"')
    expect(runtimeSource).toContain(':runtime-crud-props-resolver="resolvePageBlockRuntimeCrudProps"')
    expect(runtimeSource).toContain(':data-source-configured-resolver="isPageBlockDataSourceConfigured"')
    expect(rendererSource).toContain(':fields="resolveNestedBlockFields(child)"')
    expect(rendererSource).toContain(':runtime-crud-props="resolveNestedBlockRuntimeCrudProps(child)"')
    expect(rendererSource).toContain(':runtime-crud-loading="resolveNestedBlockRuntimeCrudLoading(child)"')
  })

  it('allows standalone form persistence only in published runtime mode', () => {
    const runtimeSource = readSplitVueSource('src/views/app-center/application-runtime.[applicationCode].vue', 'useApplicationRuntime')
    const rendererSource = readSplitVueSource('src/components/lowcode-builder/page/GridBlockRenderer.vue', 'useGridBlockRenderer')

    expect(runtimeSource).toContain(':runtime-interactive="!editing && !isDraftMode"')
    expect(rendererSource).toContain('@submit="handleAiFormSubmit"')
    expect(rendererSource).toContain('props.block.props?.createApi || props.runtimeCrudProps?.apiConfig?.create')
    expect(rendererSource).toContain('window.$message?.success(\'提交成功\')')
  })
})
