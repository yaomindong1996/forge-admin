import { describe, expect, it } from 'vitest'
import {
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
