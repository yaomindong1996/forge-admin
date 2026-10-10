import { describe, expect, it, vi } from 'vitest'

vi.mock('@/utils/request', () => ({
  request: vi.fn(),
}))

import {
  alignSearchSchemaWithLeftTree,
  applyEmbeddedTreeTableRuntimeProps,
  buildLeftTreeFilterParams,
  buildLeftTreeOptionSource,
  collectTreeFilterValues,
  expandSearchTreeSelectValue,
  findTreePanelProps,
  isEmbeddedTreeTableRuntime,
  normalizeTreeTableNodes,
  resolveLeftTreeSortParams,
  resolveTreeLoadMode,
} from '../runtime-tree-table'
import { buildRuntimeCrudProps } from '../runtime-crud-props'

describe('runtime tree table', () => {
  it('detects embedded tree table and excludes left-tree-right-table layout', () => {
    expect(isEmbeddedTreeTableRuntime({
      layoutType: 'simple-crud',
      options: { treeConfig: { enabled: true, parentField: 'parentId' } },
    })).toBe(true)
    expect(isEmbeddedTreeTableRuntime({
      layoutType: 'tree-crud',
      options: { treeConfig: { enabled: true } },
    })).toBe(false)
    expect(isEmbeddedTreeTableRuntime({
      layoutType: 'list-form',
      options: { treeConfig: { enabled: 'true', parentField: 'parentId' } },
    })).toBe(true)
    expect(isEmbeddedTreeTableRuntime({
      options: { treeConfig: { enabled: false } },
    })).toBe(false)
    // appType 误写入 layoutType=SINGLE 时，仍须显式 enabled=true
    expect(isEmbeddedTreeTableRuntime({
      layoutType: 'SINGLE',
      options: { treeConfig: { enabled: true, parentField: 'parentId' } },
    })).toBe(true)
    // zone 残留 parentField 默认值、未 enabled：不能当嵌入式树表
    expect(isEmbeddedTreeTableRuntime({
      layoutType: 'SINGLE',
      options: { treeConfig: { parentField: 'parentId' } },
    })).toBe(false)
  })

  it('aligns search treeSelect with left-tree optionSource sort and includeChildren', () => {
    const aligned = alignSearchSchemaWithLeftTree([
      { field: 'categoryId', type: 'treeSelect', queryType: 'eq' },
      { field: 'name', type: 'input' },
    ], {
      treePanelProps: {
        filterField: 'categoryId',
        sourceConfigKey: 'biz_category',
        defaultSortField: 'sortNo',
        defaultSortOrder: 'asc',
      },
      runtimeProps: { configKey: 'biz_item', designPreview: true },
    })
    expect(aligned[0].includeChildren).toBe(true)
    expect(aligned[0].optionSource.api).toContain('/ai/crud/biz_category/tree')
    expect(aligned[0].optionSource.api).toContain('designPreview=1')
    expect(aligned[0].optionSource.params).toMatchObject({
      loadMode: 'full',
      orderByColumn: 'sortNo',
      isAsc: 'asc',
    })
    expect(aligned[1].optionSource).toBeUndefined()
  })

  it('expands search tree select values like left-tree filter', () => {
    const options = [
      {
        value: 1,
        label: 'root',
        children: [{ value: 5, label: 'child' }],
      },
    ]
    expect(expandSearchTreeSelectValue(1, options).expanded).toEqual(['1', '5'])
    expect(expandSearchTreeSelectValue(5, options).expanded).toEqual(['5'])
    expect(resolveLeftTreeSortParams({
      treePanelProps: {},
      runtimeProps: {
        configKey: 'org_tree',
        options: { defaultSort: { orderByColumn: 'sortNo', isAsc: 'asc' } },
      },
    })).toEqual({})
    expect(buildLeftTreeOptionSource({
      sourceConfigKey: 'org_tree',
      orderByColumn: 'sortNo',
      isAsc: 'asc',
    }).params).toMatchObject({ orderByColumn: 'sortNo', isAsc: 'asc' })
    expect(findTreePanelProps([
      { blockType: 'AiCrudPage', props: {} },
      { blockType: 'tree-panel', props: { filterField: 'parentId' } },
    ])).toEqual({ filterField: 'parentId' })
  })

  it('builds left-tree filter params with includeChildren by default', () => {
    expect(buildLeftTreeFilterParams({ filterField: 'categoryId', value: 12 })).toEqual({
      categoryId: '12',
      categoryId_includeChildren: true,
    })
    expect(buildLeftTreeFilterParams({
      filterField: 'categoryId',
      value: 12,
      includeChildren: false,
    })).toEqual({
      categoryId: '12',
    })
    expect(buildLeftTreeFilterParams({ filterField: '', value: 1 })).toEqual({})
    expect(buildLeftTreeFilterParams({
      filterField: 'categoryId',
      value: 1,
      expandedValues: [1, 2, 3],
    })).toEqual({
      categoryId: '1,2,3',
      categoryId_includeChildren: true,
    })
  })

  it('collects self and descendant filter values from loaded tree node', () => {
    expect(collectTreeFilterValues({
      id: 1,
      children: [
        { id: 2, children: [{ id: 4 }] },
        { id: 3 },
      ],
    }, { targetField: 'id' })).toEqual(['1', '2', '4', '3'])
  })

  it('normalizes nested children and load mode', () => {
    expect(resolveTreeLoadMode({ loadMode: 'lazy' })).toBe('lazy')
    const nodes = normalizeTreeTableNodes([
      {
        id: 1,
        name: 'root',
        children: [{ id: 2, name: 'child', children: [] }],
      },
    ], { childrenField: 'children' })
    expect(nodes[0].isLeaf).toBe(false)
    expect(nodes[0].children[0].id).toBe(2)
  })

  it('wires tree list api and table props into runtime crud props', () => {
    const props = buildRuntimeCrudProps({
      configKey: 'org_tree',
      layoutType: 'simple-crud',
      apiConfig: {
        list: 'get@/ai/crud/当前配置/page',
        tree: 'get@/ai/crud/当前配置/tree',
      },
      options: {
        treeConfig: {
          enabled: true,
          keyField: 'id',
          parentField: 'parentId',
          childrenField: 'children',
          loadMode: 'full',
        },
      },
    })

    expect(props.apiConfig.list).toContain('/tree')
    expect(props.showPagination).toBe(false)
    expect(props.enableTreeAddChild).toBe(true)
    expect(props.treeConfig.parentField).toBe('parentId')
    expect(props.tableProps.childrenKey).toBe('children')
    expect(props.publicParams.loadMode).toBe('full')
  })

  it('does not activate embedded tree when enabled is omitted (leftover treeConfig)', () => {
    const props = buildRuntimeCrudProps({
      configKey: 'org_tree',
      layoutType: 'SINGLE',
      apiConfig: {
        list: 'get@/ai/crud/当前配置/page',
        tree: 'get@/ai/crud/当前配置/tree',
      },
      options: {
        treeConfig: {
          keyField: 'id',
          parentField: 'parentId',
          childrenField: 'children',
        },
      },
    })
    // 非树表 zone 常残留 parentField=parentId；未显式 enabled 时必须走 /page
    expect(props.apiConfig.list).toContain('/page')
    expect(props.tableProps?.childrenKey).toBeUndefined()
  })

  it('does not activate embedded tree when enabled is explicitly false', () => {
    const props = buildRuntimeCrudProps({
      configKey: 'org_tree',
      layoutType: 'simple-crud',
      apiConfig: {
        list: 'get@/ai/crud/当前配置/page',
        tree: 'get@/ai/crud/当前配置/tree',
      },
      options: {
        treeConfig: {
          enabled: false,
          parentField: 'parentId',
        },
      },
    })
    expect(props.apiConfig.list).toContain('/page')
    expect(props.tableProps?.childrenKey).toBeUndefined()
  })

  it('keeps pagination list api for left-tree-right-table layout', () => {
    const props = buildRuntimeCrudProps({
      configKey: 'org_tree',
      layoutType: 'tree-crud',
      apiConfig: {
        list: 'get@/ai/crud/当前配置/page',
        tree: 'get@/ai/crud/当前配置/tree',
      },
      options: {
        treeConfig: { enabled: true, parentField: 'parentId' },
      },
    })

    expect(props.apiConfig.list).toContain('/page')
    expect(props.treeConfig.parentField).toBe('parentId')
    expect(props.tableProps?.childrenKey).toBeUndefined()
  })

  it('respects explicit disable of add-child on embedded tree', () => {
    const props = applyEmbeddedTreeTableRuntimeProps({
      apiConfig: { list: 'get@/x/page', tree: 'get@/x/tree' },
      enableTreeAddChild: false,
      publicParams: {},
    }, {
      layoutType: 'simple-crud',
      options: { treeConfig: { enabled: true } },
    })
    expect(props.enableTreeAddChild).toBe(false)
    expect(props.apiConfig.list).toBe('get@/x/tree')
  })

  it('forces enableTreeAddChild off for left-tree-right-table when right table is flat', () => {
    const props = buildRuntimeCrudProps({
      layoutType: 'tree-crud',
      options: {
        enableTreeAddChild: true,
        treeConfig: { enabled: false },
      },
      columnsSchema: [],
      searchSchema: [],
      editSchema: [],
    })
    expect(props.enableTreeAddChild).toBe(false)
  })

  it('keeps enableTreeAddChild when left-tree layout marks model embedded tree enabled', () => {
    const props = buildRuntimeCrudProps({
      configKey: 'org_tree',
      layoutType: 'tree-crud',
      apiConfig: {
        list: 'get@/ai/crud/org_tree/page',
        tree: 'get@/ai/crud/org_tree/tree',
      },
      options: {
        enableTreeAddChild: true,
        treeConfig: { enabled: true, parentField: 'parentId' },
      },
      columnsSchema: [],
      searchSchema: [],
      editSchema: [],
    })
    // 左树右表仍不走嵌入式 /tree；但「添加下级」开关可按本表启用状态保留
    expect(props.enableTreeAddChild).toBe(true)
    expect(props.apiConfig.list).toContain('/page')
    expect(props.tableProps?.childrenKey).toBeUndefined()
  })
})
