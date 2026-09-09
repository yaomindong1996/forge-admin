/**
 * @fileoverview 布局 spec 属性 key 与渲染器真实消费 key 对齐测试（C1 红线）
 * @description spec propsSchema 声明的属性 key / 枚举值必须与画布渲染器真实消费的一一对齐，
 *   否则属性面板改了不生效（用户反馈的"预览与设计不一致"同类根因）。
 *   渲染器侧 key 集合以测试内字面量为红线：改渲染器或 spec 时此处失败即提醒双向同步。
 */

import { describe, expect, it } from 'vitest'
import { getComponentSpec } from '../index'

describe('layout spec consumption alignment (C1 spec key 对齐渲染器红线)', () => {
  it('grid propsSchema keys must match GridBlockRenderer consumption exactly', () => {
    const spec = getComponentSpec('grid')
    // GridBlockRenderer gridLayoutStyle / gridCellStyle 消费：
    // columns / gutter(列间距) / rowGap / alignItems / justifyItems / cellMinHeight / cellBackground / showCellBorder
    expect(Object.keys(spec.propsSchema.properties)).toEqual([
      'columns',
      'gutter',
      'rowGap',
      'cellMinHeight',
      'alignItems',
      'justifyItems',
      'showCellBorder',
      'cellBackground',
    ])
    // 已废弃的旧 key（声明了但渲染器不消费）不得回流
    const legacyKeys = ['minChildHeight', 'verticalAlign', 'horizontalAlign', 'showBorder', 'showBackground']
    for (const key of legacyKeys) {
      expect(spec.propsSchema.properties[key]).toBeUndefined()
    }
  })

  it('grid align option values must be valid CSS grid keywords (stretch/start/center/end)', () => {
    const spec = getComponentSpec('grid')
    const values = prop => spec.propsSchema.properties[prop].options.map(option => option.value)
    expect(values('alignItems')).toEqual(['stretch', 'start', 'center', 'end'])
    expect(values('justifyItems')).toEqual(['stretch', 'start', 'center', 'end'])
  })

  it('box propsSchema direction values must be valid flexDirection (row/column)', () => {
    const spec = getComponentSpec('box')
    // GridBlockRenderer boxLayoutStyle: flexDirection: props.direction || 'row'
    const direction = spec.propsSchema.properties.direction
    expect(direction.options.map(option => option.value)).toEqual(['row', 'column'])
    expect(direction.default).toBe('row')
    // 渲染器不消费的 showDivider 不得声明（无效属性入口）
    expect(spec.propsSchema.properties.showDivider).toBeUndefined()
  })
})
