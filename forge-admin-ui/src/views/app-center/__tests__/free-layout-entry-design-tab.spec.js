/**
 * 对象列表页叠了自由布局伴生块时：运行态 Portal 渲染伴生块，
 * 编辑入口必须进 designTab=page，不能只开列表设计壳。
 */
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { pageHasFreeLayoutCompanionBlocks } from '../in-app-builder/page-shape-design'

const root = resolve(import.meta.dirname, '..')

describe('free-layout entry design tab', () => {
  it('detects leave-home style companion layouts', () => {
    const page = {
      layout: {
        gridLayout: {
          items: [
            { id: 'q', blockType: 'quick-entry' },
            { id: 's', blockType: 'stats-strip' },
            { id: 'b', blockType: 'bar-chart' },
            { id: 'l', blockType: 'line-chart' },
            { id: 'c', blockType: 'AiCrudPage' },
          ],
        },
      },
    }
    expect(pageHasFreeLayoutCompanionBlocks(page)).toBe(true)
  })

  it('entry tab resolution prefers page when companions exist', () => {
    const part4 = readFileSync(resolve(root, 'runtime-modules/useApplicationRuntime.part4.js'), 'utf8')
    expect(part4).toContain('pageHasFreeLayoutCompanionBlocks(resolvePageLayoutForDesign(pageId))')
    expect(part4).toContain('return \'page\'')
    expect(part4).toContain('resolveDeclaredPageShape')
    // 伴生页仍保留列表 Tab，可切回对象列表设计
    expect(part4).toContain('LIST_DESIGN_SHAPES.has(resolveDeclaredPageShape())')
  })

  it('editing without designTab writes page for companion layouts', () => {
    const part1 = readFileSync(resolve(root, 'runtime-modules/useApplicationRuntime.part1.js'), 'utf8')
    expect(part1).toContain('designTab: \'page\'')
    expect(part1).toContain('伴生自由布局必须写入 designTab=page')
  })
})
