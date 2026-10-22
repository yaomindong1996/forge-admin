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
import {
  nestedChildShellStyle,
  resolveChartBlockHeight,
  resolveSurfaceBackgroundStyle,
} from '../gridBlockRendererUtils'

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

  it('builds area chart with filled series by default', () => {
    const option = buildPageChartOption('area-chart', {
      categories: ['1月', '2月'],
      series: [{ name: '申请', data: [2, 5] }],
    })
    expect(option.series[0].type).toBe('line')
    expect(option.series[0].areaStyle.opacity).toBe(0.22)
  })

  it('builds pie / doughnut options from categories', () => {
    const pie = buildPageChartOption('pie', {
      title: '假种占比',
      categories: ['年假', '事假'],
      series: [{ name: '占比', data: [12, 8] }],
      showPercent: true,
    })
    expect(pie.series[0].type).toBe('pie')
    expect(pie.series[0].data).toEqual([
      { name: '年假', value: 12 },
      { name: '事假', value: 8 },
    ])
    expect(pie.tooltip.formatter).toContain('{d}%')

    const doughnut = buildPageChartOption('pie-chart', {
      categories: ['A', 'B'],
      series: [{ data: [1, 2] }],
      doughnut: true,
      innerRadius: 40,
      outerRadius: 70,
    })
    expect(doughnut.series[0].radius).toEqual(['40%', '70%'])
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
        style: { widthMode: 'fixed', width: 320, height: 280, heightMode: 'fixed' },
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
        style: { widthMode: 'full', width: '100%', height: 280, heightMode: 'fixed' },
      },
    })
    expect(style.width).toBe('100%')
    expect(style.justifySelf).toBe('stretch')
    expect(style.overflow).toBe('visible')
  })

  it('treats auto width as hug content, not fill container', () => {
    const style = nestedChildShellStyle({
      blockType: 'bar-chart',
      props: {
        height: 260,
        style: { widthMode: 'auto', width: 'auto', heightMode: 'fixed', height: 280 },
      },
    })
    expect(style.width).toBe('auto')
    expect(style.justifySelf).toBe('start')
  })

  it('nested charts always use pixel height so they can expand the grid shell', () => {
    const autoStyle = nestedChildShellStyle({
      blockType: 'bar-chart',
      props: {
        height: 260,
        style: { widthMode: 'full', heightMode: 'auto', height: 'auto' },
      },
    })
    expect(autoStyle.height).toBe('280px')
    expect(autoStyle.minHeight).toBe('280px')
    expect(autoStyle.flex).toBeUndefined()
    expect(autoStyle.maxHeight).toBeUndefined()

    const fullStyle = nestedChildShellStyle({
      blockType: 'line-chart',
      props: {
        height: 260,
        style: { widthMode: 'full', heightMode: 'full', height: '100%' },
      },
    })
    // full 在栅格内也改成像素高，避免 height:100% 跟矮壳走、撑不开宿主
    expect(fullStyle.height).toBe('280px')
    expect(fullStyle.minHeight).toBe('280px')
    expect(fullStyle.flex).toBeUndefined()
    expect(fullStyle.maxHeight).toBeUndefined()
  })

  it('keeps fixed nested chart height without capping to parent 100%', () => {
    const style = nestedChildShellStyle({
      blockType: 'area-chart',
      props: {
        height: 260,
        style: { widthMode: 'full', heightMode: 'fixed', height: 300 },
      },
    })
    expect(style.height).toBe('300px')
    expect(style.minHeight).toBe('300px')
    expect(style.maxHeight).toBeUndefined()
  })

  it('exports isNestedFillHeightChild only for fill-container mode', async () => {
    const { isNestedFillHeightChild } = await import('../gridBlockRendererUtils')
    expect(isNestedFillHeightChild({ props: { style: { heightMode: 'full' } } })).toBe(true)
    expect(isNestedFillHeightChild({ props: { style: { heightMode: 'auto' } } })).toBe(false)
    expect(isNestedFillHeightChild({ props: { style: { heightMode: 'fixed' } } })).toBe(false)
  })

  it('applies nested X/Y as relative offsets', () => {
    const style = nestedChildShellStyle({
      blockType: 'text-title',
      props: {
        style: { widthMode: 'full', x: 24, y: 12 },
      },
    })
    expect(style.left).toBe('24px')
    expect(style.top).toBe('12px')
  })
})

describe('resolveSurfaceBackgroundStyle', () => {
  it('builds gradient background', () => {
    const style = resolveSurfaceBackgroundStyle({
      backgroundMode: 'gradient',
      gradientFrom: '#111111',
      gradientTo: '#eeeeee',
      gradientAngle: 90,
    })
    expect(style.backgroundImage).toContain('linear-gradient(90deg')
    expect(style.backgroundImage).toContain('#111111')
  })
})
