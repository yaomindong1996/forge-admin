import fs from 'node:fs'
import { createRequire } from 'node:module'
import { FileSystemIconLoader } from '@iconify/utils/lib/loader/node-loaders'
import presetRemToPx from '@unocss/preset-rem-to-px'
import { defineConfig, presetAttributify, presetIcons, presetWind3 } from 'unocss'
import { getIcons } from './build/index.js'
// 统一的响应式字体插件，集成了静态和动态规则
import { responsiveFontUnifiedPreset } from './src/plugins/responsive-font-unified.js'

const require = createRequire(import.meta.url)

/** 从本仓库已安装的 @iconify/json 读集合，避免依赖未安装的 @iconify-json/* 或外网 CDN */
function loadIconifyCollection(name) {
  return () => {
    try {
      const filePath = require.resolve(`@iconify/json/json/${name}.json`)
      return JSON.parse(fs.readFileSync(filePath, 'utf8'))
    }
    catch (error) {
      console.warn(`[unocss] icon collection unavailable: ${name}`, error?.message || error)
      return { prefix: name, icons: {} }
    }
  }
}

const icons = getIcons()
export default defineConfig({
  presets: [
    responsiveFontUnifiedPreset(),
    presetWind3(),
    presetAttributify(),
    presetIcons({
      warn: false,
      prefix: ['ai-', 'i-', 'mdi-', 'fa-'],
      extraProperties: {
        display: 'inline-block',
        width: '1em',
        height: '1em',
      },
      collections: {
        'ai-icon': FileSystemIconLoader('./src/assets/icons/ai-icon'),
        'icon': FileSystemIconLoader('./src/assets/icons/ai-icon'),
        // 业务页面大量使用 i-material-symbols:*；不挂本地集合时 HMR/离线会整站图标变空
        'material-symbols': loadIconifyCollection('material-symbols'),
        'material-symbols-light': loadIconifyCollection('material-symbols-light'),
        'lucide': loadIconifyCollection('lucide'),
        // 侧栏折叠按钮 MenuCollapse / SideFlyoutHeader 使用 i-line-md-menu-*-left；
        // 未挂本地集合时生产构建无法生成该类，按钮只剩空壳。
        'line-md': loadIconifyCollection('line-md'),
        'mdi': loadIconifyCollection('mdi'),
        'fa': loadIconifyCollection('fa'),
        'fa6-solid': loadIconifyCollection('fa6-solid'),
        'simple-icons': loadIconifyCollection('simple-icons'),
        'ix': loadIconifyCollection('ix'),
        'streamline-plump-color': loadIconifyCollection('streamline-plump-color'),
      },
    }),
    presetRemToPx({ baseFontSize: 4 }),
  ],
  // 折叠态切换用动态 class，额外 safelist 避免只扫到其中一个分支
  safelist: [
    ...icons.map(icon => `${icon} ${icon}?mask`.split(' ')).flat(),
    'i-line-md-menu-fold-left',
    'i-line-md-menu-unfold-left',
    'i-line-md:menu-fold-left',
    'i-line-md:menu-unfold-left',
  ],
  shortcuts: [
    ['wh-full', 'w-full h-full'],
    ['f-c-c', 'flex justify-center items-center'],
    ['flex-col', 'flex flex-col'],
    ['card-border', 'border border-solid border-light_border dark:border-dark_border'],
    ['auto-bg', 'bg-white dark:bg-dark'],
    ['auto-bg-hover', 'hover:bg-#eaf0f1 hover:dark:bg-#1b2429'],
    ['auto-bg-highlight', 'bg-#eaf0f1 dark:bg-#1b2429'],
    ['text-highlight', 'rounded-4 px-8 py-2 auto-bg-highlight'],
  ],
  rules: [
    [
      'card-shadow',
      { 'box-shadow': '0 1px 2px -2px #00000029, 0 3px 6px #0000001f, 0 5px 12px 4px #00000017' },
    ],
  ],
  theme: {
    colors: {
      primary: 'var(--primary-color)',
      dark: '#18181c',
      light_border: '#efeff5',
      dark_border: '#2d2d30',
      // 语义化颜色
      info: '#909399',
      success: '#67C23A',
      warning: '#E6A23C',
      error: '#F56C6C',
    },
  },
})
