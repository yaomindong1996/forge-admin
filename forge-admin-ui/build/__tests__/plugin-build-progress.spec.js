import { describe, expect, it } from 'vitest'
import { estimateTransformPercent, formatProgressBar, pluginBuildProgress } from '../plugin-build-progress.js'

describe('forge-build-progress', () => {
  it('格式化固定宽度进度条', () => {
    expect(formatProgressBar(0)).toBe(`${'░'.repeat(24)}   0%`)
    expect(formatProgressBar(100)).toBe(`${'█'.repeat(24)} 100%`)
  })

  it('transform 阶段渐进接近 90%，不会提前满格', () => {
    expect(estimateTransformPercent(1)).toBeGreaterThan(0)
    expect(estimateTransformPercent(2800)).toBeLessThan(70)
    expect(estimateTransformPercent(50000)).toBeLessThanOrEqual(90)
    expect(estimateTransformPercent(50000)).toBeGreaterThan(89.9)
  })

  it('TTY 用回车刷新，收尾换行并到达 100%', () => {
    const chunks = []
    const stdout = { isTTY: true, write: text => chunks.push(text) }
    const plugin = pluginBuildProgress({ stdout, isTTY: true })
    plugin.buildStart()
    plugin.transform()
    plugin.renderStart()
    plugin.renderChunk()
    plugin.closeBundle()
    expect(chunks.some(text => text.includes('\r'))).toBe(true)
    expect(chunks.at(-1)).toContain('100%')
    expect(chunks.at(-1)).toContain('build complete')
    expect(chunks.at(-1).endsWith('\n')).toBe(true)
  })
})
