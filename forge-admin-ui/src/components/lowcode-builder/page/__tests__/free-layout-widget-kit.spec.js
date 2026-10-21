import { describe, expect, it } from 'vitest'
import {
  normalizeQuickEntryItem,
  normalizeStatMetric,
  resolveToneCssVars,
  resolveTonePalette,
  resolveWidgetLinkAction,
} from '../free-layout-widget-kit.js'

describe('free-layout-widget-kit', () => {
  it('resolves preset and custom tone palettes', () => {
    expect(resolveTonePalette('success').color).toBe('#16a34a')
    expect(resolveTonePalette('custom', '#ff5500')).toMatchObject({
      tone: 'custom',
      color: '#ff5500',
    })
    expect(resolveToneCssVars('primary')['--fl-tone-color']).toBe('#2563eb')
  })

  it('normalizes quick-entry linkType from legacy targetPageId / href', () => {
    expect(normalizeQuickEntryItem({ title: 'A', targetPageId: 'page_x' })).toMatchObject({
      linkType: 'page',
      targetPageId: 'page_x',
    })
    expect(normalizeQuickEntryItem({ title: 'B', href: 'https://example.com' })).toMatchObject({
      linkType: 'url',
      href: 'https://example.com',
    })
    expect(normalizeQuickEntryItem({ title: 'C' }).linkType).toBe('none')
  })

  it('resolves widget link actions for page and url', () => {
    expect(resolveWidgetLinkAction({ linkType: 'page', targetPageId: 'p1' })).toEqual({
      type: 'page',
      targetPageId: 'p1',
      action: 'navigate',
    })
    expect(resolveWidgetLinkAction({ linkType: 'url', href: 'https://a.com' })).toEqual({
      type: 'url',
      href: 'https://a.com',
      action: 'openUrl',
    })
    expect(resolveWidgetLinkAction({}).type).toBe('none')
  })

  it('normalizes stats metrics with tone and optional link', () => {
    expect(normalizeStatMetric({
      label: '访问',
      value: '10',
      icon: 'EyeOutline',
      tone: 'info',
      targetPageId: 'page_stats',
    })).toMatchObject({
      label: '访问',
      icon: 'EyeOutline',
      tone: 'info',
      linkType: 'page',
      targetPageId: 'page_stats',
    })
  })
})
