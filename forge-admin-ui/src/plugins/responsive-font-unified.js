/**
 * 统一的UnoCSS响应式字体插件
 * 集成了静态规则和动态规则，提供完整的响应式字体解决方案
 */

import { definePreset } from 'unocss'

export const responsiveFontUnifiedPreset = definePreset(() => {
  // 静态规则 - 预定义常用字体大小，提高性能
  const staticFontSizeRules = [
    // 生成常用的px字体大小规则
    ['text-10px', { 'font-size': 'calc(10px * var(--font-scale, 1)) !important' }],
    ['text-11px', { 'font-size': 'calc(11px * var(--font-scale, 1)) !important' }],
    ['text-12px', { 'font-size': 'calc(12px * var(--font-scale, 1)) !important' }],
    ['text-13px', { 'font-size': 'calc(13px * var(--font-scale, 1)) !important' }],
    ['text-14px', { 'font-size': 'calc(14px * var(--font-scale, 1)) !important' }],
    ['text-15px', { 'font-size': 'calc(15px * var(--font-scale, 1)) !important' }],
    ['text-16px', { 'font-size': 'calc(16px * var(--font-scale, 1)) !important' }],
    ['text-17px', { 'font-size': 'calc(17px * var(--font-scale, 1)) !important' }],
    ['text-18px', { 'font-size': 'calc(18px * var(--font-scale, 1)) !important' }],
    ['text-19px', { 'font-size': 'calc(19px * var(--font-scale, 1)) !important' }],
    ['text-20px', { 'font-size': 'calc(20px * var(--font-scale, 1)) !important' }],
    ['text-21px', { 'font-size': 'calc(21px * var(--font-scale, 1)) !important' }],
    ['text-22px', { 'font-size': 'calc(22px * var(--font-scale, 1)) !important' }],
    ['text-23px', { 'font-size': 'calc(23px * var(--font-scale, 1)) !important' }],
    ['text-24px', { 'font-size': 'calc(24px * var(--font-scale, 1)) !important' }],
    ['text-25px', { 'font-size': 'calc(25px * var(--font-scale, 1)) !important' }],
    ['text-26px', { 'font-size': 'calc(26px * var(--font-scale, 1)) !important' }],
    ['text-27px', { 'font-size': 'calc(27px * var(--font-scale, 1)) !important' }],
    ['text-28px', { 'font-size': 'calc(28px * var(--font-scale, 1)) !important' }],
    ['text-29px', { 'font-size': 'calc(29px * var(--font-scale, 1)) !important' }],
    ['text-30px', { 'font-size': 'calc(30px * var(--font-scale, 1)) !important' }],
    ['text-31px', { 'font-size': 'calc(31px * var(--font-scale, 1)) !important' }],
    ['text-32px', { 'font-size': 'calc(32px * var(--font-scale, 1)) !important' }],
    ['text-33px', { 'font-size': 'calc(33px * var(--font-scale, 1)) !important' }],
    ['text-34px', { 'font-size': 'calc(34px * var(--font-scale, 1)) !important' }],
    ['text-35px', { 'font-size': 'calc(35px * var(--font-scale, 1)) !important' }],
    ['text-36px', { 'font-size': 'calc(36px * var(--font-scale, 1)) !important' }],
    ['text-10', { 'font-size': 'calc(10px * var(--font-scale, 1)) !important' }],
    ['text-11', { 'font-size': 'calc(11px * var(--font-scale, 1)) !important' }],
    ['text-12', { 'font-size': 'calc(12px * var(--font-scale, 1)) !important' }],
    ['text-13', { 'font-size': 'calc(13px * var(--font-scale, 1)) !important' }],
    ['text-14', { 'font-size': 'calc(14px * var(--font-scale, 1)) !important' }],
    ['text-15', { 'font-size': 'calc(15px * var(--font-scale, 1)) !important' }],
    ['text-16', { 'font-size': 'calc(16px * var(--font-scale, 1)) !important' }],
    ['text-17', { 'font-size': 'calc(17px * var(--font-scale, 1)) !important' }],
    ['text-18', { 'font-size': 'calc(18px * var(--font-scale, 1)) !important' }],
    ['text-19', { 'font-size': 'calc(19px * var(--font-scale, 1)) !important' }],
    ['text-20', { 'font-size': 'calc(20px * var(--font-scale, 1)) !important' }],
    ['text-21', { 'font-size': 'calc(21px * var(--font-scale, 1)) !important' }],
    ['text-22', { 'font-size': 'calc(22px * var(--font-scale, 1)) !important' }],
    ['text-23', { 'font-size': 'calc(23px * var(--font-scale, 1)) !important' }],
    ['text-24', { 'font-size': 'calc(24px * var(--font-scale, 1)) !important' }],
    ['text-25', { 'font-size': 'calc(25px * var(--font-scale, 1)) !important' }],
    ['text-26', { 'font-size': 'calc(26px * var(--font-scale, 1)) !important' }],
    ['text-27', { 'font-size': 'calc(27px * var(--font-scale, 1)) !important' }],
    ['text-28', { 'font-size': 'calc(28px * var(--font-scale, 1)) !important' }],
    ['text-29', { 'font-size': 'calc(29px * var(--font-scale, 1)) !important' }],
    ['text-30', { 'font-size': 'calc(30px * var(--font-scale, 1)) !important' }],
    ['text-31', { 'font-size': 'calc(31px * var(--font-scale, 1)) !important' }],
    ['text-32', { 'font-size': 'calc(32px * var(--font-scale, 1)) !important' }],
    ['text-33', { 'font-size': 'calc(33px * var(--font-scale, 1)) !important' }],
    ['text-34', { 'font-size': 'calc(34px * var(--font-scale, 1)) !important' }],
    ['text-35', { 'font-size': 'calc(35px * var(--font-scale, 1)) !important' }],
    ['text-36', { 'font-size': 'calc(36px * var(--font-scale, 1)) !important' }],

    // 预定义尺寸
    ['text-xs', { 'font-size': 'calc(12px * var(--font-scale, 1)) !important' }],
    ['text-sm', { 'font-size': 'calc(14px * var(--font-scale, 1)) !important' }],
    ['text-base', { 'font-size': 'calc(16px * var(--font-scale, 1)) !important' }],
    ['text-lg', { 'font-size': 'calc(18px * var(--font-scale, 1)) !important' }],
    ['text-xl', { 'font-size': 'calc(20px * var(--font-scale, 1)) !important' }]
  ]

  // 静态行高规则
  const staticLineHeightRules = [
    ['leading-12', { 'line-height': 'calc(12px * var(--font-scale, 1)) !important' }],
    ['leading-14', { 'line-height': 'calc(14px * var(--font-scale, 1)) !important' }],
    ['leading-16', { 'line-height': 'calc(16px * var(--font-scale, 1)) !important' }],
    ['leading-18', { 'line-height': 'calc(18px * var(--font-scale, 1)) !important' }],
    ['leading-20', { 'line-height': 'calc(20px * var(--font-scale, 1)) !important' }],
    ['leading-22', { 'line-height': 'calc(22px * var(--font-scale, 1)) !important' }],
    ['leading-24', { 'line-height': 'calc(24px * var(--font-scale, 1)) !important' }],
    ['leading-26', { 'line-height': 'calc(26px * var(--font-scale, 1)) !important' }],
    ['leading-28', { 'line-height': 'calc(28px * var(--font-scale, 1)) !important' }],
    ['leading-30', { 'line-height': 'calc(30px * var(--font-scale, 1)) !important' }],
    ['leading-32', { 'line-height': 'calc(32px * var(--font-scale, 1)) !important' }]
  ]

  // 动态规则 - 处理任意数值，提供更大的灵活性
  const dynamicRules = [
    // 处理text-xs, text-sm等预定义尺寸
    [/^text-(xs|sm|base|lg|xl)$/, ([_, size]) => {
      const sizes = {
        'xs': '12px',
        'sm': '14px',
        'base': '16px',
        'lg': '18px',
        'xl': '20px'
      }

      if (sizes[size]) {
        return {
          'font-size': `calc(${sizes[size]} * var(--font-scale, 1)) !important`
        }
      }
    }],

    // 处理line-height
    [/^leading-(\d+)$/, ([_, size]) => {
      const value = parseInt(size)
      if (value > 0) {
        return {
          'line-height': `calc(${value}px * var(--font-scale, 1)) !important`
        }
      }
    }]
  ]

  // 合并所有规则 - 静态规则优先，动态规则作为补充
  const allRules = [...staticFontSizeRules,...staticLineHeightRules,...dynamicRules]

  return {
    name: 'responsive-font-unified-preset',
    rules: allRules,

    // 快捷方式
    shortcuts: [
      // 响应式标题
      ['responsive-h1', 'text-32px font-bold'],
      ['responsive-h2', 'text-24px font-bold'],
      ['responsive-h3', 'text-18px font-semibold'],
      ['responsive-h4', 'text-16px font-semibold'],
      ['responsive-h5', 'text-14px font-medium'],
      ['responsive-h6', 'text-12px font-medium'],

      // 响应式正文
      ['responsive-text', 'text-14px'],
      ['responsive-text-sm', 'text-12px'],
      ['responsive-text-lg', 'text-16px'],

      // 响应式辅助文本
      ['responsive-caption', 'text-12px text-gray-500']
    ]
  }
})

export default responsiveFontUnifiedPreset
