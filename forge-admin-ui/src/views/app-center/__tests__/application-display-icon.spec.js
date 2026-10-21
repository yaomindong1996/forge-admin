import { describe, expect, it } from 'vitest'
import { resolveApplicationDisplayIcon } from '../application-display-icon'

describe('resolveApplicationDisplayIcon', () => {
  it('keeps stable ionicons / forge / image icons', () => {
    expect(resolveApplicationDisplayIcon('ionicons5:CalendarOutline')).toBe('ionicons5:CalendarOutline')
    expect(resolveApplicationDisplayIcon('forge:module')).toBe('forge:module')
    expect(resolveApplicationDisplayIcon('/api/file/abc')).toBe('/api/file/abc')
  })

  it('maps legacy lucide market icons to ionicons so cards are not empty', () => {
    expect(resolveApplicationDisplayIcon('i-lucide:calendar-days')).toBe('ionicons5:CalendarOutline')
    expect(resolveApplicationDisplayIcon('i-lucide:user-plus')).toBe('ionicons5:PersonAddOutline')
  })

  it('falls back when icon missing or unknown dynamic uno class', () => {
    expect(resolveApplicationDisplayIcon('')).toBe('ionicons5:AppsOutline')
    expect(resolveApplicationDisplayIcon('i-lucide:unknown-thing')).toBe('ionicons5:AppsOutline')
  })
})
