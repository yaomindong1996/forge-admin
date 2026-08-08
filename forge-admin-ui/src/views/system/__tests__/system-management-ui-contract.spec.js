import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

function readSource(relativeUrl) {
  return readFileSync(new URL(relativeUrl, import.meta.url), 'utf8')
}

function readCssBlock(source, selector) {
  const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const matched = source.match(new RegExp(`${escapedSelector}\\s*\\{([^}]*)\\}`))
  return matched?.[1] || ''
}

describe('system management control sizing', () => {
  it('uses the Naive UI component height for every button size', () => {
    const source = readSource('../../../styles/theme.css')

    for (const [selector, fallback] of [
      ['.n-button', '34px'],
      ['.n-button--small-type', '28px'],
      ['.n-button--tiny-type', '22px'],
      ['.n-button--large-type', '40px'],
    ]) {
      expect(readCssBlock(source, selector)).toContain(`height: var(--n-height, ${fallback}) !important;`)
    }
  })
})

describe('system organization tree scrolling', () => {
  it.each(['../org.vue', '../user.vue', '../post.vue'])(
    'keeps the left tree independently scrollable in %s',
    (relativeUrl) => {
      const source = readSource(relativeUrl)
      const block = readCssBlock(source, '.org-tree-content')

      expect(block).toMatch(/min-height:\s*0;/)
      expect(block).toMatch(/overflow-y:\s*auto;/)
      expect(block).toMatch(/overscroll-behavior:\s*contain;/)
      expect(block).toMatch(/scrollbar-gutter:\s*stable;/)
    },
  )
})
