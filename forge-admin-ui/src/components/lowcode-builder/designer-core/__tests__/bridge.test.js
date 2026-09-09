/**
 * @file 桥接层测试 — 验证桥接函数输出格式与现有消费方兼容
 */

import { describe, expect, it } from 'vitest'
import {
  FORM_COMPONENT_KEY_OVERRIDES,
  getComponentSpec,
  isPaletteUnionSpec,
  LIST_CANVAS_PENDING_TYPES,
  listAllComponents,
  listComponents,
  PALETTE_ZONE_ONLY_TYPES,
  resolveBridgeSpec,
  toCanvasComponentCatalog,
  toFieldComponentDefaults,
  toFieldComponentDefaultsMap,
  toFieldPaletteGroups,
  toListPageBlockCatalog,
  toPageWidgetCatalog,
  toPageWidgetComponentKeys,
  toPageZoneCatalog,
} from '../index'

describe('bridge Compatibility', () => {
  // ─── 表单设计器桥接 ──────────────────────────────────────

  it('toFieldPaletteGroups should return groups with title + items[{ componentKey, label }]', () => {
    const groups = toFieldPaletteGroups()
    expect(Array.isArray(groups)).toBe(true)
    expect(groups.length).toBeGreaterThanOrEqual(3) // 输入、选择、业务
    groups.forEach((group) => {
      expect(group.title).toBeDefined()
      expect(typeof group.title).toBe('string')
      expect(Array.isArray(group.items)).toBe(true)
      group.items.forEach((item) => {
        expect(item.componentKey).toBeDefined()
        expect(item.label).toBeDefined()
      })
    })
  })

  it('toFieldPaletteGroups should contain known component keys including transfer', () => {
    const groups = toFieldPaletteGroups()
    const allKeys = groups.flatMap(g => g.items.map(i => i.componentKey))
    expect(allKeys).toContain('input')
    expect(allKeys).toContain('select')
    expect(allKeys).toContain('transfer')
    expect(allKeys).toContain('userSelect')
    expect(allKeys).toContain('orgTreeSelect')
    expect(allKeys).toHaveLength(33)
  })

  it('toFieldComponentDefaults should return object keyed by componentType', () => {
    const defaults = toFieldComponentDefaults()
    expect(typeof defaults).toBe('object')
    expect(Object.keys(defaults).length).toBeGreaterThan(0)
    Object.values(defaults).forEach((val) => {
      expect(typeof val).toBe('object')
      expect(val.fieldType).toBeDefined()
    })
  })

  it('toFieldComponentDefaultsMap should include aliases', () => {
    const map = toFieldComponentDefaultsMap()
    // 主 type
    expect(map.input).toBeDefined()
    expect(map.number).toBeDefined()
    // 别名（bridge aliasExtra）
    expect(map.inputNumber).toBeDefined()
    expect(map.orgSelect).toBeDefined()
    expect(map.upload).toBeDefined()
    // 别名与主 type 值一致
    expect(map.inputNumber).toEqual(map.number)
    expect(map.orgSelect).toEqual(map.orgTreeSelect)
  })

  // ─── 页面挂件桥接 ──────────────────────────────────────

  it('toPageWidgetCatalog should return 20 items (18 widgets + barcode + qrcode)', () => {
    const catalog = toPageWidgetCatalog()
    expect(Array.isArray(catalog)).toBe(true)
    expect(catalog.length).toBe(20)
    catalog.forEach((item) => {
      expect(item.componentKey).toBeDefined()
      expect(item.blockType).toBeDefined()
      expect(item.title).toBeDefined()
      expect(item.label).toBeDefined()
      expect(typeof item.defaultW).toBe('number')
      expect(typeof item.defaultH).toBe('number')
    })
  })

  it('toPageWidgetCatalog should use English group keys', () => {
    const catalog = toPageWidgetCatalog()
    const groups = new Set(catalog.map(i => i.group))
    // 应包含英文 group key
    for (const g of groups) {
      expect(['content', 'data', 'advanced', 'navigation', 'layout', 'media']).toContain(g)
    }
  })

  it('toPageWidgetCatalog should include barcode and qrcode', () => {
    const catalog = toPageWidgetCatalog()
    const keys = catalog.map(i => i.componentKey)
    expect(keys).toContain('barcode')
    expect(keys).toContain('qrcode')
  })

  it('toPageWidgetComponentKeys should return 26 strings', () => {
    const keys = toPageWidgetComponentKeys()
    expect(Array.isArray(keys)).toBe(true)
    expect(keys.length).toBe(26)
    keys.forEach((key) => {
      expect(typeof key).toBe('string')
    })
  })

  // ─── 列表页区块桥接 ──────────────────────────────────────

  it('toListPageBlockCatalog should return blocks with blockType, defaultW, defaultH', () => {
    const catalog = toListPageBlockCatalog()
    expect(Array.isArray(catalog)).toBe(true)
    expect(catalog.length).toBeGreaterThan(10)
    catalog.forEach((item) => {
      expect(item.blockType).toBeDefined()
      expect(item.title).toBeDefined()
    })
    // 关键业务组件应包含 defaultW / defaultH
    const crud = catalog.find(i => i.blockType === 'AiCrudPage')
    expect(crud).toBeDefined()
    expect(crud.defaultW).toBe(12)
    expect(crud.defaultH).toBe(14)
    expect(crud.techTitle).toBe('AiCrudPage')
  })

  it('toListPageBlockCatalog should exclude wrapper nodes and field components', () => {
    const catalog = toListPageBlockCatalog()
    const types = catalog.map(i => i.blockType)
    expect(types).not.toContain('tabPane')
    expect(types).not.toContain('collapseItem')
    expect(types).not.toContain('col')
    expect(types).not.toContain('tableCell')
    expect(types).not.toContain('input')
    expect(types).not.toContain('select')
  })

  // ─── 页面画布桥接 ──────────────────────────────────────

  it('toCanvasComponentCatalog should return items with componentKey and pixel dimensions', () => {
    const catalog = toCanvasComponentCatalog()
    expect(Array.isArray(catalog)).toBe(true)
    expect(catalog.length).toBeGreaterThan(0)
    catalog.forEach((item) => {
      expect(item.componentKey).toBeDefined()
      expect(item.title).toBeDefined()
    })
    // 查询集应有精确像素尺寸
    const qs = catalog.find(i => i.componentKey === 'query-set')
    expect(qs).toBeDefined()
    expect(qs.defaultWidth).toBe(640)
    expect(qs.defaultHeight).toBe(132)
  })

  it('toCanvasComponentCatalog should include zones and multiField', () => {
    const catalog = toCanvasComponentCatalog()
    const qs = catalog.find(i => i.componentKey === 'query-set')
    expect(qs.zones).toEqual(['search', 'table'])
    expect(qs.multiField).toBe(true)
  })

  // ─── 页面 Zone 桥接 ──────────────────────────────────────

  it('toPageZoneCatalog should return zones with zoneKey and componentKey', () => {
    const catalog = toPageZoneCatalog()
    expect(Array.isArray(catalog)).toBe(true)
    expect(catalog.length).toBeGreaterThan(0)
    const searchZone = catalog.find(z => z.zoneKey === 'search')
    expect(searchZone).toBeDefined()
    expect(searchZone.componentKey).toBe('search-form')
  })

  // ─── 通用桥接 ──────────────────────────────────────

  it('resolveBridgeSpec should resolve type or alias to spec', () => {
    const spec = resolveBridgeSpec('elCard')
    expect(spec).not.toBeNull()
    expect(spec.type).toBe('card')

    const spec2 = resolveBridgeSpec('AiCrudPage')
    expect(spec2).not.toBeNull()
    expect(spec2.type).toBe('AiCrudPage')
  })

  it('resolveBridgeSpec should return null for unknown', () => {
    expect(resolveBridgeSpec('unknown-component')).toBeNull()
  })
})

describe('bridge Legacy Type Compatibility (存量类型名回归红线)', () => {
  // GridBlockRenderer 等存量消费方按历史 blockType 分发渲染分支；
  // 桥接层改类型名会直接造成拖入后无法渲染，此组测试是红线。

  it('toListPageBlockCatalog must keep legacy layout blockTypes for canvas renderer', () => {
    const types = toListPageBlockCatalog().map(i => i.blockType)
    expect(types).toContain('grid-layout')
    expect(types).toContain('box-layout')
    expect(types).toContain('section-divider')
    // spec 新名不得混入列表面板（渲染器无对应分支）
    expect(types).not.toContain('grid')
    expect(types).not.toContain('box')
    expect(types).not.toContain('groupTitle')
    expect(types).not.toContain('table')
    expect(types).not.toContain('collapse')
    expect(types).not.toContain('formSectionTitle')
  })

  it('toListPageBlockCatalog must not leak zone action components', () => {
    const types = toListPageBlockCatalog().map(i => i.blockType)
    for (const zoneType of ['query-set', 'custom-query', 'import-button', 'export-button', 'add-button', 'reset-button']) {
      expect(types).not.toContain(zoneType)
    }
  })

  it('toListPageBlockCatalog must match legacy catalog exactly (59 items)', () => {
    const catalog = toListPageBlockCatalog()
    expect(catalog).toHaveLength(59)
    const legacyTypes = [
      'AiCrudPage',
      'AiForm',
      'AiTable',
      'action-button',
      'audio-player',
      'avatar',
      'back-button',
      'box-layout',
      'button-group',
      'card',
      'custom-html',
      'data-table',
      'detail-info',
      'divider',
      'empty-state',
      'grid-layout',
      'iframe',
      'info-panel',
      'link',
      'page-title',
      'paragraph',
      'search-form',
      'section-divider',
      'signature-pad',
      'space',
      'spacer',
      'statistic',
      'stats-strip',
      'step-form',
      'steps',
      'sub-table-tabs',
      'tabs',
      'tag-list',
      'text-tip',
      'text-title',
      'timeline',
      'toolbar',
      'tree-panel',
      'video-player',
    ]
    const types = catalog.map(i => i.blockType)
    const uniqueTypes = new Set(types)
    for (const type of legacyTypes) {
      expect(uniqueTypes.has(type)).toBe(true)
    }
    // 挂件类也应包含（rich-text/markdown 等 20 个）
    for (const widgetType of ['rich-text', 'markdown', 'watermark', 'vue-component', 'html-tag', 'qrcode', 'barcode', 'menu', 'pagination', 'split']) {
      expect(uniqueTypes.has(widgetType)).toBe(true)
    }
  })

  it('toCanvasComponentCatalog must keep zone canvas scoped (7 registry items)', () => {
    const keys = toCanvasComponentCatalog().map(i => i.componentKey)
    expect(keys).toHaveLength(7)
    for (const zoneType of ['query-set', 'custom-query', 'import-button', 'export-button', 'add-button', 'reset-button', 'data-table']) {
      expect(keys).toContain(zoneType)
    }
  })
})

describe('bridge Form Palette Compatibility (表单统一面板回归红线)', () => {
  // ForgeFormCanvas 的 createForgeLayoutComponent 按历史 componentKey 分发分支；
  // 统一面板 spec.type 若无映射直接拖入会落到默认 title 分支，此组测试是红线。

  it('fORM_COMPONENT_KEY_OVERRIDES must map spec types to legacy canvas keys', () => {
    expect(FORM_COMPONENT_KEY_OVERRIDES).toEqual({
      'grid': 'row',
      'groupTitle': 'title',
      'formSectionTitle': 'AiFormSectionTitle',
      'action-button': 'button',
    })
    // 映射源必须是注册表有效类型
    for (const type of Object.keys(FORM_COMPONENT_KEY_OVERRIDES)) {
      expect(getComponentSpec(type)).toBeDefined()
    }
  })

  it('button alias must resolve to action-button (物料清单合并规则)', () => {
    const spec = getComponentSpec('button')
    expect(spec).toBeDefined()
    expect(spec.type).toBe('action-button')
    expect(spec.scope).toBe('F+L')
  })

  it('form palette scope F must expose field templates + widgets + layout keys', () => {
    const formSpecs = listComponents('F')
    const types = new Set(formSpecs.map(s => s.type))
    // 字段模板
    for (const fieldType of ['input', 'select', 'transfer', 'userSelect']) {
      expect(types.has(fieldType)).toBe(true)
    }
    // 页面挂件（表单画布 isPageWidgetComponentKey 全支持）
    for (const widgetType of ['rich-text', 'markdown', 'watermark', 'vue-component', 'barcode', 'qrcode']) {
      expect(types.has(widgetType)).toBe(true)
    }
    // 布局/业务（映射后可拖入画布）
    for (const layoutType of ['grid', 'table', 'card', 'tabs', 'collapse', 'groupTitle', 'formSectionTitle', 'action-button', 'AiCrudPage', 'subTable']) {
      expect(types.has(layoutType)).toBe(true)
    }
  })

  it('widget scope change must not break list catalog (59 items unchanged)', () => {
    // 挂件改 F+L 后，列表目录仍应精确 59 项（L/F+L 匹配语义不变）
    expect(toListPageBlockCatalog()).toHaveLength(59)
    const widgetKeys = toPageWidgetComponentKeys()
    expect(widgetKeys).toHaveLength(26)
    expect(widgetKeys).toContain('transfer')
    expect(widgetKeys).toContain('barcode')
    expect(widgetKeys).toContain('audio-player')
  })
})

describe('bridge Palette Union (B1/B2 统一合集两侧同显红线)', () => {
  // 两侧设计器面板 scope='ALL' 同显同一份组件合集（仅排除容器内部包装节点），
  // 数量一致是用户验收标准；画布能力差异由接入方 itemDisabledReason 禁用态呈现，不再从面板隐藏。

  it('isPaletteUnionSpec should keep all non-wrapper components (101 items)', () => {
    const union = listAllComponents().filter(isPaletteUnionSpec)
    expect(union).toHaveLength(101)
    // 包装节点（tabPane/collapseItem/col/tableCell）不进面板
    expect(union.map(s => s.group)).not.toContain('包装节点')
  })

  it('palette union must cover form-only and list-only components simultaneously', () => {
    const types = new Set(listAllComponents().filter(isPaletteUnionSpec).map(s => s.type))
    // 关联子表类组件（subTable 为表单专属，sub-table-tabs 双画布）
    for (const formOnly of ['subTable', 'sub-table-tabs']) {
      expect(types.has(formOnly)).toBe(true)
    }
    // 列表画布支持（scope L/F+L，列表区块目录内）
    for (const listOnly of ['data-table', 'search-form', 'AiCrudPage']) {
      expect(types.has(listOnly)).toBe(true)
    }
    // zone 专属组件也在合集中同显（两侧均禁用态）
    for (const zoneType of PALETTE_ZONE_ONLY_TYPES) {
      expect(types.has(zoneType)).toBe(true)
    }
  })

  it('pALETTE_ZONE_ONLY_TYPES must stay excluded from list catalog (single source of truth)', () => {
    // 常量与 Legacy 红线测试字面量保持同步：zone 专属组件不得进入列表区块目录
    expect(PALETTE_ZONE_ONLY_TYPES).toEqual(['query-set', 'custom-query', 'import-button', 'export-button', 'add-button', 'reset-button'])
    const types = new Set(toListPageBlockCatalog().map(i => i.blockType))
    for (const type of PALETTE_ZONE_ONLY_TYPES) {
      expect(types.has(type)).toBe(false)
    }
  })

  it('lIST_CANVAS_PENDING_TYPES must stay excluded from list catalog', () => {
    // 列表画布渲染器暂无分支的类型：面板同显但列表侧禁用（待流式画布放开）
    expect(LIST_CANVAS_PENDING_TYPES).toEqual(['table', 'collapse', 'formSectionTitle'])
    const types = new Set(toListPageBlockCatalog().map(i => i.blockType))
    for (const type of LIST_CANVAS_PENDING_TYPES) {
      expect(types.has(type)).toBe(false)
    }
  })
})
