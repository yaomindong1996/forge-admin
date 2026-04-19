import { FileSystemIconLoader } from '@iconify/utils/lib/loader/node-loaders'
import presetRemToPx from '@unocss/preset-rem-to-px'
import { defineConfig, presetAttributify, presetIcons, presetWind3 } from 'unocss'
import { getIcons } from './build/index.js'
// 统一的响应式字体插件，集成了静态和动态规则
import { responsiveFontUnifiedPreset } from './src/plugins/responsive-font-unified.js'

const icons = getIcons()
export default defineConfig({
  presets: [
    responsiveFontUnifiedPreset(),
    presetWind3(),
    presetAttributify(),
    presetIcons({
      warn: true,
      prefix: ['ai-', 'i-', 'mdi-', 'fa-'],
      extraProperties: {
        display: 'inline-block',
        width: '1em',
        height: '1em',
      },
      collections: {
        icon: FileSystemIconLoader('./src/assets/icons/ai-icon'),
      },
    }),
    presetRemToPx({ baseFontSize: 4 }),
  ],
  safelist: icons.map(icon => `${icon} ${icon}?mask`.split(' ')).flat(),
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
