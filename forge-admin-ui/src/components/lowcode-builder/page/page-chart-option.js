/**
 * 自由布局图表：柱 / 折 / 面积 / 饼。结构化配置 → 基础 option → 可选 JSON 覆盖合并。
 * 数据融合约定：
 * 1) 静态 categories/series 或列表绑定行生成数据集
 * 2) 外观开关（图例/平滑/配色等）写入基础 option
 * 3) optionOverrides 深合并（可改 grid/tooltip/颜色等）
 * 4) 绑定数据在合并后再写回轴与系列值，避免 JSON 盖掉真实数据
 */

export const PAGE_CHART_TYPES = Object.freeze(['bar', 'line', 'area', 'pie'])

export function resolvePageChartType(blockTypeOrChartType = 'bar') {
  const raw = String(blockTypeOrChartType || 'bar').trim().toLowerCase()
  if (raw === 'bar-chart' || raw === 'bar')
    return 'bar'
  if (raw === 'line-chart' || raw === 'line')
    return 'line'
  if (raw === 'area-chart' || raw === 'area')
    return 'area'
  if (raw === 'pie-chart' || raw === 'pie' || raw === 'doughnut')
    return 'pie'
  return 'bar'
}

const DEFAULT_COLORS = Object.freeze([
  '#3b82f6',
  '#22c55e',
  '#f59e0b',
  '#ef4444',
  '#8b5cf6',
  '#06b6d4',
])

function toNumber(value, fallback = 0) {
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

function isPlainObject(value) {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

/** 浅层友好的深合并：对象递归，数组整段替换 */
export function deepMergeChartOption(base = {}, override = {}) {
  if (!isPlainObject(override))
    return base
  const result = { ...base }
  Object.entries(override).forEach(([key, value]) => {
    if (value === undefined)
      return
    if (isPlainObject(value) && isPlainObject(result[key]))
      result[key] = deepMergeChartOption(result[key], value)
    else
      result[key] = value
  })
  return result
}

export function normalizeCategories(raw = []) {
  if (Array.isArray(raw))
    return raw.map(item => String(item ?? '').trim()).filter(Boolean)
  return String(raw || '')
    .split(/[,，]/)
    .map(item => item.trim())
    .filter(Boolean)
}

export function normalizeSeriesList(raw = [], categoryCount = 0) {
  if (!Array.isArray(raw) || !raw.length) {
    return [{
      name: '数值',
      data: Array.from({ length: categoryCount }, () => 0),
    }]
  }
  return raw.map((item, index) => {
    const data = Array.isArray(item?.data)
      ? item.data.map(v => toNumber(v, 0))
      : String(item?.data ?? '')
        .split(/[,，]/)
        .map(v => toNumber(v.trim(), 0))
    while (data.length < categoryCount)
      data.push(0)
    return {
      name: String(item?.name || `系列${index + 1}`).trim() || `系列${index + 1}`,
      data: data.slice(0, Math.max(categoryCount, data.length)),
    }
  })
}

/**
 * 列表绑定行 → categories + series
 * row: { category/label, value, series/group }
 */
export function buildChartDataFromRows(rows = [], binding = {}) {
  const labelKey = String(binding.labelField || 'category').trim() || 'category'
  const valueKey = String(binding.valueField || 'value').trim() || 'value'
  const seriesKey = String(binding.metaField || 'series').trim() || 'series'
  const list = Array.isArray(rows) ? rows : []
  const categories = []
  const seriesMap = new Map()

  list.forEach((row) => {
    if (!row || typeof row !== 'object')
      return
    const category = String(row[labelKey] ?? row.category ?? row.label ?? '').trim()
    if (!category)
      return
    if (!categories.includes(category))
      categories.push(category)
    const seriesName = String(row[seriesKey] ?? row.series ?? row.group ?? '数值').trim() || '数值'
    if (!seriesMap.has(seriesName))
      seriesMap.set(seriesName, {})
    seriesMap.get(seriesName)[category] = toNumber(row[valueKey] ?? row.value, 0)
  })

  const series = [...seriesMap.entries()].map(([name, byCategory]) => ({
    name,
    data: categories.map(cat => toNumber(byCategory[cat], 0)),
  }))

  return {
    categories,
    series: series.length ? series : [{ name: '数值', data: categories.map(() => 0) }],
  }
}

function resolveColors(payload = {}) {
  const raw = payload.colors
  if (Array.isArray(raw) && raw.length)
    return raw.map(item => String(item || '').trim()).filter(Boolean)
  return [...DEFAULT_COLORS]
}

function resolveLegendPosition(payload = {}) {
  const pos = String(payload.legendPosition || 'top-right').trim()
  if (pos === 'top')
    return { top: 0, left: 'center' }
  if (pos === 'bottom')
    return { bottom: 0, left: 'center' }
  if (pos === 'left')
    return { left: 0, top: 'middle', orient: 'vertical' }
  return { top: 0, right: 0 }
}

function buildChartTitle(title = '') {
  const text = String(title || '').trim()
  if (!text)
    return undefined
  return {
    text,
    left: 0,
    top: 0,
    textStyle: {
      fontSize: 14,
      fontWeight: 600,
      color: 'var(--text-primary, #0f172a)',
    },
  }
}

/** 饼图数据：categories + series[0].data，或 payload.pieItems */
export function buildPieSeriesData(payload = {}) {
  if (Array.isArray(payload.pieItems) && payload.pieItems.length) {
    return payload.pieItems.map((item, index) => ({
      name: String(item?.name || item?.label || `项${index + 1}`).trim() || `项${index + 1}`,
      value: toNumber(item?.value ?? item?.count, 0),
    })).filter(item => item.name)
  }
  const categories = normalizeCategories(payload.categories)
  const seriesList = normalizeSeriesList(payload.series, categories.length)
  const values = seriesList[0]?.data || []
  return categories.map((name, index) => ({
    name,
    value: toNumber(values[index], 0),
  }))
}

function buildPieChartOption(payload = {}) {
  const title = String(payload.title || '').trim()
  const colors = resolveColors(payload)
  const showLegend = payload.showLegend !== false
  const legendPos = resolveLegendPosition(payload)
  const pieData = buildPieSeriesData(payload)
  const doughnut = payload.doughnut === true || String(payload.pieVariant || '').trim() === 'doughnut'
  const innerRadius = Math.max(0, Math.min(70, Number(payload.innerRadius) || (doughnut ? 48 : 0)))
  const outerRadius = Math.max(innerRadius + 8, Math.min(90, Number(payload.outerRadius) || 68))
  const roseTypeRaw = String(payload.roseType || 'none').trim()
  const roseType = ['radius', 'area'].includes(roseTypeRaw) ? roseTypeRaw : false
  const showLabel = payload.showLabel !== false
  const showPercent = payload.showPercent !== false
  const titleOffset = title ? 28 : 0
  const legendOffset = showLegend ? 28 : 0

  const base = {
    color: colors,
    title: buildChartTitle(title),
    tooltip: {
      trigger: 'item',
      formatter: showPercent ? '{b}: {c} ({d}%)' : '{b}: {c}',
    },
    legend: showLegend
      ? {
          ...legendPos,
          top: legendPos.top === 0 ? titleOffset : legendPos.top,
          type: 'scroll',
          textStyle: { color: 'var(--text-secondary, #64748b)', fontSize: 12 },
        }
      : undefined,
    series: [{
      name: String(payload.series?.[0]?.name || '占比').trim() || '占比',
      type: 'pie',
      radius: doughnut || innerRadius > 0
        ? [`${innerRadius}%`, `${outerRadius}%`]
        : `${outerRadius}%`,
      center: ['50%', `${52 + Math.round((titleOffset + legendOffset) / 8)}%`],
      roseType: roseType || undefined,
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: Math.max(0, Math.min(12, Number(payload.pieBorderRadius) || 4)),
        borderColor: 'var(--bg-primary, #fff)',
        borderWidth: Math.max(0, Math.min(6, Number(payload.pieBorderWidth) || 2)),
      },
      label: showLabel
        ? {
            show: true,
            formatter: showPercent ? '{b}\n{d}%' : '{b}',
            color: 'var(--text-secondary, #64748b)',
            fontSize: 11,
          }
        : { show: false },
      labelLine: showLabel ? { length: 10, length2: 8 } : { show: false },
      data: pieData.length ? pieData : [{ name: '暂无数据', value: 0 }],
      emphasis: {
        itemStyle: { shadowBlur: 8, shadowColor: 'rgba(15, 23, 42, 0.12)' },
      },
    }],
  }

  return applyChartOptionOverrides(base, payload.optionOverrides, {
    chartType: 'pie',
    categories: pieData.map(item => item.name),
    series: [{ name: '占比', data: pieData.map(item => item.value) }],
    pieData,
    dataLocked: payload.dataLocked === true,
  })
}

/**
 * @param {'bar'|'line'|'area'|'pie'|string} chartType
 * @param {object} payload
 */
export function buildPageChartOption(chartType = 'bar', payload = {}) {
  const resolvedType = resolvePageChartType(chartType)
  if (resolvedType === 'pie')
    return buildPieChartOption(payload)

  const categories = normalizeCategories(payload.categories)
  const seriesList = normalizeSeriesList(payload.series, categories.length)
  const isBar = resolvedType === 'bar'
  const isArea = resolvedType === 'area'
  const isLineLike = resolvedType === 'line' || isArea
  const showLegend = payload.showLegend !== false
  const title = String(payload.title || '').trim()
  const colors = resolveColors(payload)
  const legendPos = resolveLegendPosition(payload)
  const hasLegend = showLegend && seriesList.length > 1
  const titleOffset = title ? 28 : 0
  const legendOffset = hasLegend ? 28 : 0
  const rotate = Math.max(0, Math.min(60, Number(payload.categoryLabelRotate) || 0))
  const barMaxWidth = Math.max(8, Math.min(80, Number(payload.barMaxWidth) || 36))
  const showSplitLine = payload.showSplitLine !== false
  const showArea = isArea || payload.showArea === true
  const areaOpacity = isArea
    ? Math.max(0.08, Math.min(0.45, Number(payload.areaOpacity) || 0.22))
    : Math.max(0.04, Math.min(0.4, Number(payload.areaOpacity) || 0.12))

  const base = {
    color: colors,
    title: buildChartTitle(title),
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: isBar ? 'shadow' : 'line' },
    },
    legend: hasLegend
      ? {
          ...legendPos,
          top: legendPos.top === 0 ? titleOffset : legendPos.top,
          type: 'scroll',
          textStyle: { color: 'var(--text-secondary, #64748b)', fontSize: 12 },
        }
      : undefined,
    grid: {
      left: Number(payload.gridLeft) || 8,
      right: Number(payload.gridRight) || 12,
      top: titleOffset + legendOffset + (Number(payload.gridTopPad) || 8),
      bottom: Number(payload.gridBottom) || (rotate > 0 ? 28 : 8),
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: categories.length ? categories : ['暂无分类'],
      boundaryGap: isBar,
      axisLabel: {
        color: 'var(--text-secondary, #64748b)',
        fontSize: 11,
        rotate,
      },
      axisLine: { lineStyle: { color: 'var(--border-color, #e2e8f0)' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      name: String(payload.yAxisName || '').trim() || undefined,
      splitLine: showSplitLine
        ? { lineStyle: { color: 'var(--border-light, #f1f5f9)', type: 'dashed' } }
        : { show: false },
      axisLabel: { color: 'var(--text-secondary, #64748b)', fontSize: 11 },
    },
    series: seriesList.map(item => ({
      name: item.name,
      type: isBar ? 'bar' : 'line',
      data: item.data.length ? item.data : [0],
      barMaxWidth,
      smooth: isLineLike && payload.smooth !== false,
      symbol: isBar ? undefined : 'circle',
      symbolSize: isBar ? undefined : Math.max(4, Math.min(14, Number(payload.symbolSize) || 6)),
      areaStyle: showArea && isLineLike
        ? { opacity: areaOpacity }
        : undefined,
      itemStyle: isBar
        ? { borderRadius: [4, 4, 0, 0] }
        : undefined,
    })),
  }

  return applyChartOptionOverrides(base, payload.optionOverrides, {
    chartType: resolvedType,
    categories,
    series: seriesList,
    dataLocked: payload.dataLocked === true,
  })
}

/**
 * 将用户 JSON 覆盖合并进基础 option；dataLocked 时绑定/结构化数据优先写回轴与系列值。
 */
export function applyChartOptionOverrides(baseOption = {}, overrides, context = {}) {
  let next = baseOption
  const parsed = parseChartOptionOverrides(overrides)
  if (parsed)
    next = deepMergeChartOption(next, parsed)

  if (context.dataLocked === false)
    return next

  const chartType = resolvePageChartType(context.chartType || 'bar')
  if (chartType === 'pie') {
    const pieData = Array.isArray(context.pieData) && context.pieData.length
      ? context.pieData
      : buildPieSeriesData({
        categories: context.categories,
        series: context.series,
      })
    const prevSeries = Array.isArray(next.series) ? next.series[0] : {}
    next = {
      ...next,
      series: [{
        ...(isPlainObject(prevSeries) ? prevSeries : {}),
        type: 'pie',
        data: pieData.length ? pieData : [{ name: '暂无数据', value: 0 }],
      }],
    }
    return next
  }

  const categories = normalizeCategories(context.categories)
  const seriesList = normalizeSeriesList(context.series, categories.length)
  const isBar = chartType === 'bar'
  if (categories.length) {
    next = {
      ...next,
      xAxis: {
        ...(isPlainObject(next.xAxis) ? next.xAxis : {}),
        type: 'category',
        data: categories,
      },
    }
  }
  if (seriesList.length) {
    const prevSeries = Array.isArray(next.series) ? next.series : []
    next = {
      ...next,
      series: seriesList.map((item, index) => {
        const prev = isPlainObject(prevSeries[index]) ? prevSeries[index] : {}
        return {
          ...prev,
          name: item.name,
          type: prev.type || (isBar ? 'bar' : 'line'),
          data: item.data,
        }
      }),
    }
  }
  return next
}

export function parseChartOptionOverrides(raw) {
  if (raw == null || raw === '')
    return null
  if (isPlainObject(raw))
    return raw
  if (typeof raw !== 'string')
    return null
  const text = raw.trim()
  if (!text)
    return null
  try {
    const parsed = JSON.parse(text)
    return isPlainObject(parsed) ? parsed : null
  }
  catch {
    return null
  }
}

export function stringifyChartOptionOverrides(raw, fallback = '{\n  \n}') {
  if (isPlainObject(raw))
    return JSON.stringify(raw, null, 2)
  if (typeof raw === 'string' && raw.trim())
    return raw
  return fallback
}

export function parseCommaList(text = '') {
  return String(text || '')
    .split(/[,，]/)
    .map(item => item.trim())
    .filter(Boolean)
}

export function parseNumberList(text = '') {
  return String(text || '')
    .split(/[,，]/)
    .map(item => item.trim())
    .filter(item => item !== '')
    .map(item => toNumber(item, 0))
}

export { DEFAULT_COLORS }
