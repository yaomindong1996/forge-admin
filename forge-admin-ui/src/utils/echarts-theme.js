/**
 * ECharts 图表主题配置
 * 提供统一、美观的图表样式
 */

// 获取CSS变量值
function getCssVar(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

// 判断是否为暗色模式
function isDarkMode() {
  return document.documentElement.classList.contains('dark')
}

/**
 * 获取当前主题配置
 */
export function getEChartsTheme() {
  const dark = isDarkMode()
  
  // 颜色
  const primary = getCssVar('--primary-500') || '#3b82f6'
  const primaryLight = getCssVar('--primary-300') || '#93c5fd'
  const primaryDark = getCssVar('--primary-700') || '#1d4ed8'
  
  const success = getCssVar('--success-500') || '#22c55e'
  const warning = getCssVar('--warning-500') || '#f59e0b'
  const error = getCssVar('--error-500') || '#ef4444'
  const info = getCssVar('--info-500') || '#0ea5e9'
  
  // 文字颜色
  const textPrimary = getCssVar('--text-primary') || (dark ? '#f1f5f9' : '#0f172a')
  const textSecondary = getCssVar('--text-secondary') || (dark ? '#94a3b8' : '#64748b')
  const textTertiary = getCssVar('--text-tertiary') || (dark ? '#64748b' : '#94a3b8')
  
  // 背景颜色
  const bgPrimary = getCssVar('--bg-primary') || (dark ? '#0f172a' : '#ffffff')
  const bgSecondary = getCssVar('--bg-secondary') || (dark ? '#1e293b' : '#f8fafc')
  
  // 边框颜色
  const borderDefault = getCssVar('--border-default') || (dark ? '#334155' : '#e2e8f0')
  
  return {
    // 基础配色方案
    color: [
      primary,
      primaryLight,
      success,
      warning,
      error,
      info,
      '#8b5cf6',
      '#ec4899',
      '#14b8a6',
      '#f97316'
    ],
    
    // 背景色
    backgroundColor: bgPrimary,
    
    // 文本样式
    textStyle: {
      color: textPrimary,
      fontFamily: getCssVar('--font-family-sans') || 'Inter, sans-serif',
      fontSize: 14,
      fontWeight: 'normal'
    },
    
    // 标题样式
    title: {
      textStyle: {
        color: textPrimary,
        fontSize: 18,
        fontWeight: 600,
        fontFamily: getCssVar('--font-family-sans') || 'Inter, sans-serif'
      },
      subtextStyle: {
        color: textSecondary,
        fontSize: 12
      }
    },
    
    // 图例样式
    legend: {
      textStyle: {
        color: textSecondary,
        fontSize: 12
      },
      itemGap: 20,
      itemWidth: 14,
      itemHeight: 14
    },
    
    // 坐标轴样式
    categoryAxis: {
      axisLine: {
        lineStyle: {
          color: borderDefault
        }
      },
      axisTick: {
        lineStyle: {
          color: borderDefault
        }
      },
      axisLabel: {
        color: textSecondary,
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: borderDefault,
          type: 'dashed'
        }
      }
    },
    
    // 数值轴样式
    valueAxis: {
      axisLine: {
        lineStyle: {
          color: borderDefault
        }
      },
      axisTick: {
        lineStyle: {
          color: borderDefault
        }
      },
      axisLabel: {
        color: textSecondary,
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: borderDefault,
          type: 'dashed'
        }
      },
      splitArea: {
        areaStyle: {
          color: [bgSecondary, bgPrimary]
        }
      }
    },
    
    // 时间轴样式
    timeAxis: {
      axisLine: {
        lineStyle: {
          color: borderDefault
        }
      },
      axisTick: {
        lineStyle: {
          color: borderDefault
        }
      },
      axisLabel: {
        color: textSecondary,
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: borderDefault,
          type: 'dashed'
        }
      }
    },
    
    // 工具提示样式
    tooltip: {
      backgroundColor: dark ? 'rgba(30, 41, 59, 0.95)' : 'rgba(255, 255, 255, 0.95)',
      borderColor: borderDefault,
      borderWidth: 1,
      borderRadius: 8,
      padding: [8, 12],
      textStyle: {
        color: textPrimary,
        fontSize: 13
      },
      extraCssText: 'box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); backdrop-filter: blur(10px);'
    },
    
    // 数据区域缩放
    dataZoom: {
      backgroundColor: bgSecondary,
      borderColor: borderDefault,
      textStyle: {
        color: textSecondary
      }
    },
    
    // 图形系列样式
    series: {
      // 线图
      lineStyle: {
        width: 2,
        curveness: 0.4
      },
      // 面积图
      areaStyle: {
        opacity: 0.1
      },
      // 柱状图
      barStyle: {
        borderRadius: [4, 4, 0, 0]
      },
      // 散点图
      scatterStyle: {
        borderWidth: 1,
        borderColor: bgPrimary
      },
      // 饼图
      pieStyle: {
        borderWidth: 1,
        borderColor: bgPrimary
      },
      // 仪表盘
      gaugeStyle: {
        borderWidth: 1,
        borderColor: bgPrimary
      }
    },
    
    // 进度条
    progressBar: {
      barStyle: {
        borderRadius: 4
      }
    },
    
    // 标记点
    markPoint: {
      symbol: 'pin',
      symbolSize: 30,
      itemStyle: {
        borderWidth: 2,
        borderColor: bgPrimary
      },
      label: {
        color: textPrimary,
        fontSize: 12
      }
    },
    
    // 标记线
    markLine: {
      symbol: ['none', 'none'],
      lineStyle: {
        width: 2,
        type: 'dashed'
      },
      label: {
        color: textSecondary,
        fontSize: 12
      }
    },
    
    // 动画配置
    animation: true,
    animationDuration: 1000,
    animationEasing: 'cubicOut',
    animationDelay: (idx) => idx * 100
  }
}

/**
 * 获取渐变色生成函数
 */
export function generateGradientColor(color, alpha = 1) {
  const hex = color.replace('#', '')
  const r = parseInt(hex.substr(0, 2), 16)
  const g = parseInt(hex.substr(2, 2), 16)
  const b = parseInt(hex.substr(4, 2), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

/**
 * 生成图表渐变色
 */
export function generateChartGradient(ctx, color, direction = 'vertical') {
  const primary = getCssVar('--primary-500') || '#3b82f6'
  const chartColor = color || primary
  
  if (direction === 'vertical') {
    const gradient = ctx.createLinearGradient(0, 0, 0, 400)
    gradient.addColorStop(0, generateGradientColor(chartColor, 0.8))
    gradient.addColorStop(1, generateGradientColor(chartColor, 0.1))
    return gradient
  } else {
    const gradient = ctx.createLinearGradient(0, 0, 400, 0)
    gradient.addColorStop(0, generateGradientColor(chartColor, 0.8))
    gradient.addColorStop(1, generateGradientColor(chartColor, 0.1))
    return gradient
  }
}

/**
 * 常用图表配置预设
 */
export const chartPresets = {
  // 折线图
  line: {
    smooth: true,
    showSymbol: false,
    symbolSize: 8,
    hoverAnimation: true,
    emphasis: {
      focus: 'series',
      scale: true
    }
  },
  
  // 面积图
  area: {
    smooth: true,
    showSymbol: false,
    areaStyle: {
      opacity: 0.2
    },
    hoverAnimation: true,
    emphasis: {
      focus: 'series',
      scale: true
    }
  },
  
  // 柱状图
  bar: {
    barMaxWidth: 60,
    hoverAnimation: true,
    emphasis: {
      focus: 'series'
    }
  },
  
  // 饼图
  pie: {
    radius: ['40%', '70%'],
    avoidLabelOverlap: true,
    itemStyle: {
      borderRadius: 8,
      borderWidth: 2
    },
    label: {
      show: true,
      formatter: '{b}: {d}%'
    },
    emphasis: {
      label: {
        show: true,
        fontSize: 14,
        fontWeight: 'bold'
      },
      itemStyle: {
        shadowBlur: 10,
        shadowOffsetX: 0,
        shadowColor: 'rgba(0, 0, 0, 0.5)'
      }
    }
  },
  
  // 散点图
  scatter: {
    symbolSize: 10,
    hoverAnimation: true,
    emphasis: {
      focus: 'series',
      scale: true
    }
  },
  
  // 雷达图
  radar: {
    shape: 'circle',
    splitNumber: 5,
    axisName: {
      color: getCssVar('--text-secondary') || '#64748b'
    },
    splitLine: {
      lineStyle: {
        color: getCssVar('--border-default') || '#e2e8f0'
      }
    },
    splitArea: {
      areaStyle: {
        color: [getCssVar('--bg-secondary') || '#f8fafc', getCssVar('--bg-primary') || '#ffffff']
      }
    }
  },
  
  // 仪表盘
  gauge: {
    startAngle: 180,
    endAngle: 0,
    min: 0,
    max: 100,
    splitNumber: 10,
    itemStyle: {
      color: getCssVar('--primary-500') || '#3b82f6'
    },
    progress: {
      show: true,
      width: 18
    },
    pointer: {
      show: true,
      length: '90%',
      width: 8
    },
    axisLine: {
      lineStyle: {
        width: 18,
        color: [
          [0.3, getCssVar('--success-500') || '#22c55e'],
          [0.7, getCssVar('--warning-500') || '#f59e0b'],
          [1, getCssVar('--error-500') || '#ef4444']
        ]
      }
    },
    axisTick: {
      splitNumber: 5,
      length: 8
    },
    axisLabel: {
      fontSize: 12,
      color: getCssVar('--text-secondary') || '#64748b'
    },
    title: {
      fontSize: 14,
      color: getCssVar('--text-primary') || '#0f172a'
    },
    detail: {
      fontSize: 24,
      color: getCssVar('--text-primary') || '#0f172a'
    }
  }
}

/**
 * 响应式配置
 */
export function getResponsiveConfig(containerWidth) {
  if (containerWidth < 768) {
    return {
      grid: { left: 10, right: 10, top: 50, bottom: 50 },
      legend: { orient: 'horizontal', top: 10 },
      xAxis: { axisLabel: { fontSize: 10 } },
      yAxis: { axisLabel: { fontSize: 10 } }
    }
  } else if (containerWidth < 1024) {
    return {
      grid: { left: 40, right: 20, top: 60, bottom: 60 },
      legend: { orient: 'horizontal', top: 10 },
      xAxis: { axisLabel: { fontSize: 11 } },
      yAxis: { axisLabel: { fontSize: 11 } }
    }
  } else {
    return {
      grid: { left: 60, right: 30, top: 70, bottom: 70 },
      legend: { orient: 'horizontal', top: 10 },
      xAxis: { axisLabel: { fontSize: 12 } },
      yAxis: { axisLabel: { fontSize: 12 } }
    }
  }
}

/**
 * 暗色主题切换
 */
export function toggleEChartsTheme(chartInstance) {
  if (!chartInstance) return
  
  const theme = getEChartsTheme()
  const option = chartInstance.getOption()
  
  // 保存原始配置
  if (!chartInstance._originalOption) {
    chartInstance._originalOption = JSON.parse(JSON.stringify(option))
  }
  
  chartInstance.setOption({
    backgroundColor: theme.backgroundColor,
    textStyle: theme.textStyle,
    title: theme.title,
    legend: theme.legend,
    xAxis: theme.xAxis,
    yAxis: theme.yAxis,
    tooltip: theme.tooltip
  })
}

/**
 * 应用主题到图表实例
 */
export function applyThemeToChart(chartInstance, customTheme = {}) {
  if (!chartInstance) return
  
  const theme = { ...getEChartsTheme(), ...customTheme }
  chartInstance.setOption(theme, { notMerge: false, lazyUpdate: true })
}

export default {
  getEChartsTheme,
  generateGradientColor,
  generateChartGradient,
  chartPresets,
  getResponsiveConfig,
  toggleEChartsTheme,
  applyThemeToChart
}