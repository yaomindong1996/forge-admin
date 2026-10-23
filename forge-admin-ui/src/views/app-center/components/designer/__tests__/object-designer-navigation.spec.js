import fs from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'
import { readSplitVueSource } from '@/test-utils/read-split-vue-source'
import {
  pickBusinessObjectIdentity,
  resolveDataModelTab,
  resolveStandaloneObjectDesignerSection,
  standaloneObjectDesignerSections,
} from '../object-designer-navigation'

function readSource(relativePath) {
  return fs.readFileSync(path.resolve(process.cwd(), relativePath), 'utf8')
}

function readObjectDesigner() {
  return readSplitVueSource('src/views/app-center/object-designer.[objectCode].vue', 'useObjectDesigner')
}

function readListDesigner() {
  return readSplitVueSource('src/views/app-center/components/designer/BusinessListDesigner.vue', 'useBusinessListDesigner')
}

describe('standalone object designer navigation', () => {
  it('exposes object-owned configuration dimensions including tree model', () => {
    expect(standaloneObjectDesignerSections.map(item => item.key)).toEqual([
      'basic',
      'fields',
      'data-model',
      'tree-model',
    ])
  })

  it('maps legacy deep links into the standalone sections', () => {
    expect(resolveStandaloneObjectDesignerSection('relations')).toBe('data-model')
    expect(resolveStandaloneObjectDesignerSection('permission')).toBe('tree-model')
    expect(resolveStandaloneObjectDesignerSection('tree-model')).toBe('tree-model')
    expect(resolveStandaloneObjectDesignerSection('flow-app')).toBe('fields')
    expect(resolveStandaloneObjectDesignerSection('list')).toBe('fields')
    expect(resolveStandaloneObjectDesignerSection('triggers')).toBe('fields')
  })

  it('keeps resolveDataModelTab for legacy modelTab bookmarks', () => {
    expect(resolveDataModelTab('permission')).toBe('tree-model')
    expect(resolveDataModelTab('tree-model')).toBe('tree-model')
    expect(resolveDataModelTab('flow-app')).toBe('relations')
  })

  it('keeps application-owned designers out of the standalone object designer', () => {
    const objectDesigner = readObjectDesigner()
    const listDesigner = readListDesigner()
    const gridDesigner = readSplitVueSource('src/components/lowcode-builder/page/ListPageGridDesigner.vue', 'useListPageGridDesigner')

    expect(objectDesigner).not.toContain('activePanel === \'default-view\'')
    expect(objectDesigner).not.toContain('activePanel === \'triggers\'')
    expect(objectDesigner).not.toContain('BusinessTriggerConfigPanel')
    expect(objectDesigner).not.toContain('BusinessActionDesigner')
    expect(objectDesigner).toContain('const compatibilityPanel = [\'publish\', \'advanced\'].includes(normalizedPanel)')
    expect(listDesigner).not.toContain('class="list-custom-actions-entry"')
    expect(listDesigner).toContain('const visibleListCustomActions = computed(() => props.defaultViewOnly ? [] : listCustomActions.value)')
    expect(listDesigner).toMatch(/if \(!props\.defaultViewOnly\)\s*\n?\s*await saveBusinessObjectActions/)
    expect(gridDesigner).toContain('customActionsEditable')
    expect(gridDesigner).toContain('工具栏按钮与导入导出')
    expect(gridDesigner).toContain('自定义操作按钮')
  })

  it('hosts tree-model configuration inside list design property panel', () => {
    const listDesigner = readListDesigner()
    const gridDesigner = readSplitVueSource('src/components/lowcode-builder/page/ListPageGridDesigner.vue', 'useListPageGridDesigner')
    const treeModelPanel = readSource('src/views/app-center/components/designer/BusinessPermissionFlowPanel.vue')
    const processPanel = readSource('src/views/app-center/components/designer/ObjectProcessReadOnlyPanel.vue')
    const objectDesigner = readObjectDesigner()

    expect(listDesigner).not.toContain('class="list-tree-model-entry"')
    expect(listDesigner).toContain('@update:model-schema="handleGridModelSchemaUpdate"')
    expect(gridDesigner).toContain('class="list-tree-model-property"')
    expect(gridDesigner).toContain('树形模型')
    expect(gridDesigner).toContain('updateEmbeddedTreeEnabled')
    expect(treeModelPanel).toContain('配置对象的父子层级、显示字段和加载方式。')
    expect(treeModelPanel).not.toContain('数据策略')
    expect(treeModelPanel).not.toContain('updateDataScope')
    expect(objectDesigner).toContain('@update:model-schema="handleListModelSchemaUpdate"')
    expect(objectDesigner).toContain('<ObjectProcessReadOnlyPanel')
    expect(processPanel).toContain('businessObjectProcesses(props.objectCode)')
    expect(processPanel).toContain('去应用工作台配置')
  })

  it('keeps standalone tree-model panel for legacy deep links', () => {
    const objectDesigner = readObjectDesigner()
    const designerShell = readSource('src/views/app-center/components/designer/BusinessObjectDesignerShell.vue')
    const navigation = readSource('src/views/app-center/components/designer/object-designer-navigation.js')

    expect(navigation).toContain('{ key: \'tree-model\', label: \'树形模型\' }')
    expect(objectDesigner).not.toContain('<n-tab-pane name="tree-model"')
    expect(objectDesigner).not.toContain('<n-tab-pane name="flow-app"')
    expect(objectDesigner).not.toContain('<n-tab-pane name="permission"')
    expect(objectDesigner).toContain('activePanel === \'tree-model\'')
    expect(designerShell).toContain('key: \'tree-model\', label: \'树形模型\'')
    expect(designerShell).not.toContain('key: \'permission\', label: \'数据范围适配\'')
  })

  it('guides standalone users to the application process workspace', () => {
    const objectDesigner = readObjectDesigner()

    expect(objectDesigner).toContain('流程与自动化配置已移至应用工作台')
    expect(objectDesigner).toContain('触发器、流程绑定和业务动作已统一为业务流程画布')
    expect(objectDesigner).toContain('@click="openProcessWorkspace"')
  })

  it('loads the object by query objectId even when the route code is a generic fallback', () => {
    const colliding = { id: '1910000000000000001', objectCode: 'business_object', objectName: '打卡' }
    expect(pickBusinessObjectIdentity({
      queryObjectId: '2089974506884993026',
      objectByCode: colliding,
    })).toEqual({ id: '2089974506884993026' })
    expect(pickBusinessObjectIdentity({
      queryObjectId: ['2089974506884993026'],
    })).toEqual({ id: '2089974506884993026' })
    expect(pickBusinessObjectIdentity({ objectByCode: colliding })).toEqual(colliding)

    const objectDesigner = readObjectDesigner()
    expect(objectDesigner).toContain('pickBusinessObjectIdentity({ queryObjectId })')
    expect(objectDesigner).not.toContain('return object?.id ? object : { id }')
  })
})
