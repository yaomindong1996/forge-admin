import { describe, expect, it } from 'vitest'
import {
  flattenResourceTree,
  getResourceBreadcrumbs,
  resolveFreshResourceRow,
  resolveResourceContextRows,
} from '../menu-interaction-utils'

const resources = [
  {
    id: 1,
    resourceName: '系统管理',
    children: [
      {
        id: 2,
        resourceName: '用户管理',
        children: [
          { id: 3, resourceName: '用户查询', children: null },
        ],
      },
      { id: 4, resourceName: '岗位管理', children: undefined },
    ],
  },
  { id: 5, resourceName: '首页', children: null },
]

describe('resolveResourceContextRows', () => {
  it('returns top-level resources when no resource node is selected', () => {
    expect(resolveResourceContextRows(resources, null)).toEqual(resources)
  })

  it('returns only the direct children of the selected resource', () => {
    expect(resolveResourceContextRows(resources, resources[0])).toEqual(resources[0].children)
  })

  it.each([
    { id: 10, children: null },
    { id: 11, children: undefined },
    { id: 12 },
  ])('returns an empty list for a leaf resource %#', (leaf) => {
    expect(resolveResourceContextRows(resources, leaf)).toEqual([])
  })

  it('flattens descendants only inside the selected resource context', () => {
    expect(resolveResourceContextRows(resources, resources[0], { includeDescendants: true }))
      .toEqual([
        expect.objectContaining({ id: 2, level: 0 }),
        expect.objectContaining({ id: 3, level: 1 }),
        expect.objectContaining({ id: 4, level: 0 }),
      ])
  })
})

describe('资源层级面包屑', () => {
  it('根上下文与多层上下文保留真实 ID', () => {
    const flat = flattenResourceTree(resources)
    expect(getResourceBreadcrumbs(flat, null)).toEqual([{ id: 0, label: '全部资源' }])
    expect(getResourceBreadcrumbs(flat, flat[2]).map(item => item.id)).toEqual([0, 1, 2, 3])
  })
  it('只有 parentId 的旧节点支持字符串雪花 ID，不转 Number', () => {
    const flat = [{ id: '2100942720360046593', resourceName: '父级' }, { id: '2100942720360046594', parentId: '2100942720360046593', resourceName: '子级' }]
    expect(getResourceBreadcrumbs(flat, flat[1]).map(item => item.id))
      .toEqual([0, '2100942720360046593', '2100942720360046594'])
  })
  it('父级循环不会死循环或重复节点', () => {
    const flat = [{ id: 1, parentId: 2 }, { id: 2, parentId: 1 }]
    expect(getResourceBreadcrumbs(flat, flat[0]).map(item => item.id)).toEqual([0, 2, 1])
  })
})

describe('resolveFreshResourceRow', () => {
  it('rebinds a stale selected row to the latest tree object', () => {
    const staleParent = {
      id: 1,
      resourceName: '系统管理',
      children: [{ id: 99, resourceName: '已删除资源' }],
    }
    const freshParent = { id: 1, resourceName: '系统管理', children: [] }

    expect(resolveFreshResourceRow([freshParent], staleParent)).toBe(freshParent)
    expect(resolveFreshResourceRow([freshParent], staleParent).children).toEqual([])
  })

  it('clears the selected row when the resource no longer exists', () => {
    expect(resolveFreshResourceRow([{ id: 1 }], { id: 99 })).toBeNull()
  })

  it('keeps an empty selection empty', () => {
    expect(resolveFreshResourceRow([{ id: 1 }], null)).toBeNull()
  })
})
