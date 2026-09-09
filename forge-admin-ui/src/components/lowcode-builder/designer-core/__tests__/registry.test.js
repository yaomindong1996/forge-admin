/**
 * @file 注册表测试 — 验证 105 个组件全部注册、唯一性、过滤功能
 * @description transfer 同时是 field 和 widget，后者覆盖前者，所以实际 105 个唯一 type
 */

import { describe, it, expect, beforeAll } from 'vitest'
import {
  listAllComponents,
  listComponents,
  listComponentsByCategory,
  getComponentSpec,
  hasComponent,
  listAllTypes,
  getRegistryStats,
  clearRegistry,
  registerComponents,
} from '../index'

describe('Component Registry', () => {
  beforeAll(() => {
    // index.js 自动注册，无需手动调用
  })

  it('should register all 105 components (transfer 双重身份合并)', () => {
    const all = listAllComponents()
    expect(all.length).toBe(105)
  })

  it('should have unique type for each component', () => {
    const all = listAllComponents()
    const types = all.map(s => s.type)
    const uniqueTypes = new Set(types)
    expect(uniqueTypes.size).toBe(types.length)
  })

  it('should filter by scope — listComponents includes F+L for any scope', () => {
    // listComponents(scope) 返回 scope === target || scope === 'F+L'
    const formScope = listComponents('F')
    const listScope = listComponents('L')
    const both = listAllComponents().filter(s => s.scope === 'F+L')
    // formScope 包含 F + F+L，listScope 包含 L + F+L
    expect(formScope.length).toBeGreaterThan(0)
    expect(listScope.length).toBeGreaterThan(0)
    expect(both.length).toBeGreaterThan(0)
  })

  it('should filter by category', () => {
    const field = listComponentsByCategory('field')
    const layout = listComponentsByCategory('layout')
    const business = listComponentsByCategory('business')
    const page = listComponentsByCategory('page')
    const media = listComponentsByCategory('media')
    const widget = listComponentsByCategory('widget')
    expect(field.length).toBe(32) // transfer 被 widget 版本覆盖
    expect(layout.length).toBe(15)
    expect(business.length).toBe(21) // 12 (business-components) + 9 (zone-action)
    expect(page.length).toBe(13) // 7 (page-components) + 6 (zone-action)
    expect(media.length).toBe(6)
    expect(widget.length).toBe(18)
  })

  it('should include 15 zone action components', () => {
    const zoneActions = listAllComponents().filter(s => s.group === '操作' && s.category === 'page')
    // zone-action 组件 category 是 'page'，group 是 '操作'
    // 实际 15 个 zone 组件全在 zone-action-components.js
    const zoneCount = listAllComponents().filter(s =>
      ['query-set', 'custom-query', 'import-button', 'export-button',
       'add-button', 'reset-button', 'action-button', 'button-group',
       'link', 'info-panel', 'steps', 'timeline', 'empty-state',
       'custom-html', 'stats-strip'].includes(s.type)
    ).length
    expect(zoneCount).toBe(15)
  })

  it('should include 4 wrapper node types', () => {
    expect(hasComponent('tabPane')).toBe(true)
    expect(hasComponent('collapseItem')).toBe(true)
    expect(hasComponent('col')).toBe(true)
    expect(hasComponent('tableCell')).toBe(true)
  })

  it('should mark containers correctly', () => {
    const containers = listAllComponents().filter(s => s.container === true)
    // 容器：grid, table, card, tabs, collapse, box, space + tabPane, collapseItem, col, tableCell + split (widget)
    expect(containers.length).toBeGreaterThanOrEqual(12)
    expect(hasComponent('card')).toBe(true)
    expect(getComponentSpec('card').container).toBe(true)
    expect(getComponentSpec('divider').container).toBe(false)
  })

  it('should have maxDepth for all containers', () => {
    const containers = listAllComponents().filter(s => s.container === true)
    containers.forEach(spec => {
      expect(spec.maxDepth).toBeDefined()
      expect(spec.maxDepth).toBeGreaterThanOrEqual(1)
    })
  })

  it('should return stats', () => {
    const stats = getRegistryStats()
    expect(stats.total).toBe(105) // transfer 双重身份合并
    expect(stats.byCategory.field).toBe(32) // transfer 被 widget 覆盖
    expect(stats.byCategory.layout).toBe(15)
  })
})
