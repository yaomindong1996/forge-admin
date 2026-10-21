import { describe, expect, it } from 'vitest'
import {
  applyChartOptionOverrides,
  buildChartDataFromRows,
  buildPageChartOption,
  deepMergeChartOption,
  parseCommaList,
  parseNumberList,
  parseChartOptionOverrides,
} from '../page-chart-option'
import { nestedChildShellStyle, resolveChartBlockHeight } from '../gridBlockRendererUtils'

describe('page-chart-option', () => {
  it('builds categories and series from bound rows', () => {
    const result = buildChartDataFromRows([
      { category: '年假', value: 12, series: '申请' },
      { category: '事假', value: 8, series: '申请' },
      { category: '年假', value: 10, series: '通过' },
      { category: '事假', value: 5, series: '通过' },
    ], {
      labelField: 'category',
      valueField: 'value',
      metaField: 'series',
    })
    expect(result.categories).toEqual(['年假', '事假'])
    expect(result.series).toEqual([
      { name: '申请', data: [12, 8] },
      { name: '通过', data: [10, 5] },
    ])
  })

  it('builds bar option with title and rounded bars', () => {
    const option = buildPageChartOption('bar', {
      title: '请假类型',
      categories: ['年假', '事假'],
      series: [{ name: '数量', data: [3, 2] }],
    })
    expect(option.title.text).toBe('请假类型')
    expect(option.xAxis.data).toEqual(['年假', '事假'])
    expect(option.series[0].type).toBe('bar')
    expect(option.series[0].itemStyle.borderRadius).toEqual([4, 4, 0, 0])
  })

  it('builds smooth line option', () => {
    const option = buildPageChartOption('line', {
      categories: ['1月', '2月'],
      series: [{ name: '趋势', data: [1, 4] }],
      smooth: true,
      showArea: true,
    })
    expect(option.series[0].type).toBe('line')
    expect(option.series[0].smooth).toBe(true)
    expect(option.series[0].areaStyle.opacity).toBe(0.12)
  })

  it('parses comma separated lists', () => {
    expect(parseCommaList('年假, 事假，病假')).toEqual(['年假', '事假', '病假'])
    expect(parseNumberList('12, 8.5，abc')).toEqual([12, 8.5, 0])
  })

  it('deep-merges optionOverrides and keeps bound data when locked', () => {
    const option = buildPageChartOption('bar', {
      categories: ['A', 'B'],
      series: [{ name: '数量', data: [3, 2] }],
      optionOverrides: {
        grid: { left: 32 },
        tooltip: { trigger: 'item' },
        xAxis: { data: ['假数据'] },
        series: [{ data: [99, 88], itemStyle: { color: '#f00' } }],
      },
      dataLocked: true,
    })
    expect(option.grid.left).toBe(32)
    expect(option.tooltip.trigger).toBe('item')
    expect(option.xAxis.data).toEqual(['A', 'B'])
    expect(option.series[0].data).toEqual([3, 2])
    expect(option.series[0].itemStyle.color).toBe('#f00')
  })

  it('parses optionOverrides JSON string', () => {
    expect(parseChartOptionOverrides('{ "grid": { "left": 20 } }')).toEqual({ grid: { left: 20 } })
    expect(parseChartOptionOverrides('[1,2]')).toBeNull()
    expect(deepMergeChartOption({ a: 1, b: { c: 2 } }, { b: { d: 3 } })).toEqual({
      a: 1,
      b: { c: 2, d: 3 },
    })
  })

  it('allows overrides to replace data when unlocked', () => {
    const base = buildPageChartOption('bar', {
      categories: ['A'],
      series: [{ name: '数量', data: [1] }],
    })
    const merged = applyChartOptionOverrides(base, {
      xAxis: { data: ['X'] },
      series: [{ name: '覆盖', data: [9] }],
    }, { dataLocked: false, chartType: 'bar' })
    expect(merged.xAxis.data).toEqual(['X'])
    expect(merged.series[0].data).toEqual([9])
  })
})

describe('resolveChartBlockHeight', () => {
  it('prefers style.height from resize anchors over props.height', () => {
    expect(resolveChartBlockHeight({
      props: { height: 260, style: { height: 420 } },
    })).toBe(420)
  })

  it('falls back to props.height when style is fluid', () => {
    expect(resolveChartBlockHeight({
      props: { height: 300, style: { height: '100%' } },
    })).toBe(300)
  })
})

describe('nestedChildShellStyle width anchors', () => {
  it('uses justifySelf start for fixed width so grid stretch cannot ignore width', () => {
    const style = nestedChildShellStyle({
      blockType: 'bar-chart',
      props: {
        height: 260,
        style: { widthMode: 'fixed', width: 320, height: 280 },
      },
    })
    expect(style.width).toBe('320px')
    expect(style.justifySelf).toBe('start')
    // 锚点在壳外，图表壳不能 overflow:hidden，否则预设布局里锚点被裁成半圆
    expect(style.overflow).toBe('visible')
  })

  it('keeps stretch for full-width nest children', () => {
    const style = nestedChildShellStyle({
      blockType: 'bar-chart',
      props: {
        height: 260,
        style: { widthMode: 'full', width: '100%', height: 280 },
      },
    })
    expect(style.width).toBe('100%')
    expect(style.justifySelf).toBe('stretch')
    expect(style.overflow).toBe('visible')
  })
})
