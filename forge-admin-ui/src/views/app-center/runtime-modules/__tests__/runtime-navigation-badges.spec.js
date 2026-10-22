import { describe, expect, it } from 'vitest'
import {
  createEmptyNavigationBadges,
  formatNavigationBadgeCount,
  resolveSystemPageBadge,
} from '../runtime-navigation-badges'

describe('runtime navigation badges', () => {
  it('maps todo / cc counts and notice unread to the correct system pages', () => {
    const badges = { todoCount: 12, ccCount: 3, messageUnread: 2 }
    expect(resolveSystemPageBadge('system:todo', badges)).toEqual({ count: 12, dot: false })
    expect(resolveSystemPageBadge('system:cc', badges)).toEqual({ count: 3, dot: false })
    expect(resolveSystemPageBadge('system:messages', badges)).toEqual({ count: 0, dot: true })
    expect(resolveSystemPageBadge('system:workbench', badges)).toEqual({ count: 0, dot: false })
  })

  it('formats badge text with a 99+ cap', () => {
    expect(formatNavigationBadgeCount(0)).toBe('')
    expect(formatNavigationBadgeCount(12)).toBe('12')
    expect(formatNavigationBadgeCount(120)).toBe('99+')
  })

  it('starts from empty counters', () => {
    expect(createEmptyNavigationBadges()).toEqual({
      todoCount: 0,
      ccCount: 0,
      messageUnread: 0,
    })
  })
})
