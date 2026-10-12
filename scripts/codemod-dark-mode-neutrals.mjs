#!/usr/bin/env node
/**
 * 批量把中性硬编码浅色替换为设计变量，使 .dark 下背景/文字/边框跟随主题。
 * 不改按钮白字(color:#fff)、语义状态色、design-tokens、打印画布、签名板。
 */
import fs from 'node:fs'
import path from 'node:path'

const roots = process.argv.slice(2)
if (!roots.length) {
  console.error('Usage: node scripts/codemod-dark-mode-neutrals.mjs <srcDir>...')
  process.exit(1)
}

const EXT = new Set(['.vue', '.css', '.scss', '.sass'])
const SKIP_SUBSTR = [
  `${path.sep}node_modules${path.sep}`,
  `${path.sep}design-tokens.css`,
  `${path.sep}SignaturePad.vue`,
  `${path.sep}print${path.sep}designer${path.sep}`,
]

const SURFACE_PRIMARY = ['#ffffff', '#fff', 'white']
const SURFACE_SECONDARY = [
  '#fafafa', '#f8fafc', '#f6f8fb', '#f7f8fa', '#fbfcfe', '#f5f7fa', '#f9fafb', '#f6fbfb',
  '#f5f5f5', '#f7f7f7', '#fcfcfc',
]
// 含常用 slate/灰底（原常被当边框色误用在 background 上）
const SURFACE_TERTIARY = [
  '#f1f5f9', '#f2f3f5', '#f4f5f7', '#f3f4f6', '#e2e8f0', '#e5e7eb', '#eeeeee', '#eee',
]
const BORDER = [
  '#e2e8f0', '#e5e7eb', '#e5edf5', '#e5e6eb', '#e8e8e8', '#eeeeee', '#eee', '#eaeaea', '#e8eaed',
  '#cbd5e1', '#d1d5db', '#d9d9d9', '#dee2e6',
]
const TEXT_PRIMARY = [
  '#111827', '#1e293b', '#172033', '#1f2937', '#1d2129', '#0f172a', '#212529',
  '#333333', '#333', '#334155', '#2c3e50', '#27272a', '#1f2329',
]
const TEXT_SECONDARY = [
  '#4e5969', '#64748b', '#6b7280', '#475569', '#586174', '#666666', '#666',
  '#52525b', '#3f3f46',
]
const TEXT_TERTIARY = [
  '#86909c', '#94a3b8', '#9ca3af', '#a8abb2', '#999999', '#999', '#aaa', '#aaaaaa',
  '#71717a', '#a1a1aa',
]
// 深色描边（设计稿里用 slate 当强调边框）
const BORDER_STRONG = ['#334155', '#475569', '#64748b']

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.name === 'node_modules' || entry.name === 'dist' || entry.name.startsWith('.'))
      continue
    const full = path.join(dir, entry.name)
    if (entry.isDirectory())
      walk(full, out)
    else if (EXT.has(path.extname(entry.name)))
      out.push(full)
  }
  return out
}

function shouldSkip(filePath) {
  const n = path.normalize(filePath)
  return SKIP_SUBSTR.some(s => n.includes(s))
}

function hexAlt(list) {
  // 长 hex 优先，避免 #fff 吃掉 #ffffff
  return [...list].sort((a, b) => b.length - a.length).map((h) => {
    if (h.startsWith('#'))
      return h.replace('#', '#')
    return h
  }).map(h => h.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|')
}

/**
 * 在指定 CSS 属性值中替换颜色字面量（跳过已是 var(--x, #hex) 的 fallback）
 */
function replaceProp(content, propNames, hexList, token) {
  const prop = propNames.join('|')
  const colors = hexAlt(hexList)
  const re = new RegExp(
    `((?:^|[;{\\s])(?:${prop})\\s*:\\s*)(?!var\\()([^;{}]*?)(${colors})\\b`,
    'gi',
  )
  return content.replace(re, (full, prefix, mid, _color) => {
    // 若 mid 已有未闭合的 var( 则跳过，避免破坏 var(--x, #fff)
    const open = (mid.match(/var\s*\(/g) || []).length
    const close = (mid.match(/\)/g) || []).length
    if (open > close)
      return full
    return `${prefix}${mid}${token}`
  })
}

function replaceBorderShorthand(content, hexList, token) {
  const colors = hexAlt(hexList)
  const re = new RegExp(
    `((?:^|[;{\\s])border(?:-(?:top|right|bottom|left))?\\s*:\\s*)(?!var\\()([^;{}]*?)(${colors})\\b`,
    'gi',
  )
  return content.replace(re, (full, prefix, mid, _color) => {
    const open = (mid.match(/var\s*\(/g) || []).length
    const close = (mid.match(/\)/g) || []).length
    if (open > close)
      return full
    return `${prefix}${mid}${token}`
  })
}

function replaceGradientStops(content, hexList, token) {
  const colors = hexAlt(hexList)
  const re = new RegExp(`(linear-gradient\\([^;{}]*?)(${colors})\\b`, 'gi')
  return content.replace(re, (full, head, _color) => {
    if (/var\s*\(\s*--[^)]*$/.test(head))
      return full
    return `${head}${token}`
  })
}

function replaceCssVars(content, varNames, hexList, token) {
  const names = varNames.join('|')
  const colors = hexAlt(hexList)
  const re = new RegExp(`((?:${names})\\s*:\\s*)(${colors})\\b`, 'gi')
  return content.replace(re, `$1${token}`)
}

function transform(content) {
  let next = content

  next = replaceProp(next, ['background', 'background-color'], SURFACE_PRIMARY, 'var(--bg-primary)')
  next = replaceProp(next, ['background', 'background-color'], SURFACE_SECONDARY, 'var(--bg-secondary)')
  next = replaceProp(next, ['background', 'background-color'], SURFACE_TERTIARY, 'var(--bg-tertiary)')

  // 组件内部 / Naive 覆盖 CSS 变量
  const BG_VARS = ['--background', '--bg', '--surface', '--panel-bg', '--card-bg', '--home-bg', '--n-color', '--n-color-modal', '--n-color-popover']
  const BORDER_VARS = ['--home-border', '--border', '--palette-separator-color', '--n-border-color', '--n-divider-color']
  const TEXT_VARS = [
    '--n-text-color', '--n-th-text-color', '--n-td-text-color', '--n-placeholder-color',
    '--palette-entry-color', '--home-text', '--text-color',
  ]
  next = replaceCssVars(next, BG_VARS, SURFACE_PRIMARY, 'var(--bg-primary)')
  next = replaceCssVars(next, BG_VARS, SURFACE_SECONDARY, 'var(--bg-secondary)')
  next = replaceCssVars(next, BG_VARS, SURFACE_TERTIARY, 'var(--bg-tertiary)')
  next = replaceCssVars(next, BORDER_VARS, [...BORDER, ...BORDER_STRONG], 'var(--border-light)')
  next = replaceCssVars(next, TEXT_VARS, TEXT_PRIMARY, 'var(--text-primary)')
  next = replaceCssVars(next, TEXT_VARS, TEXT_SECONDARY, 'var(--text-secondary)')
  next = replaceCssVars(next, TEXT_VARS, TEXT_TERTIARY, 'var(--text-tertiary)')

  // rgba 白底 → 主题主背景（保留透明度写法用 color-mix 较稳，但简单场景直接用 token）
  next = next.replace(
    /((?:^|[;{\s])(?:background|background-color)\s*:\s*)rgba\(\s*255\s*,\s*255\s*,\s*255\s*,\s*1(?:\.0+)?\s*\)/gi,
    '$1var(--bg-primary)',
  )
  next = next.replace(
    /((?:^|[;{\s])(?:background|background-color)\s*:\s*)rgba\(\s*255\s*,\s*255\s*,\s*255\s*,\s*(0?\.\d+|1)\s*\)/gi,
    (full, prefix, alpha) => {
      const a = Number(alpha)
      if (!Number.isFinite(a) || a >= 0.98)
        return `${prefix}var(--bg-primary)`
      // 半透明白底：混入主背景
      const pct = Math.round(a * 100)
      return `${prefix}color-mix(in srgb, var(--bg-primary) ${pct}%, transparent)`
    },
  )

  // 模板/脚本内联 style 字符串中的中性色
  next = next.replace(/(style\s*[:=]\s*['"`][^'"`]*?\bcolor\s*:\s*)(#94a3b8|#64748b|#475569|#334155|#666|#999)\b/gi, (full, prefix, color) => {
    const map = {
      '#334155': 'var(--text-primary)',
      '#475569': 'var(--text-secondary)',
      '#64748b': 'var(--text-secondary)',
      '#94a3b8': 'var(--text-tertiary)',
      '#666': 'var(--text-secondary)',
      '#999': 'var(--text-tertiary)',
    }
    return `${prefix}${map[color.toLowerCase()] || color}`
  })

  next = replaceBorderShorthand(next, BORDER, 'var(--border-light)')
  next = replaceProp(next, [
    'border-color',
    'border-top-color',
    'border-right-color',
    'border-bottom-color',
    'border-left-color',
    'outline-color',
  ], BORDER, 'var(--border-light)')
  next = replaceBorderShorthand(next, BORDER_STRONG, 'var(--border-light)')
  next = replaceProp(next, [
    'border-color',
    'border-top-color',
    'border-right-color',
    'border-bottom-color',
    'border-left-color',
    'outline-color',
  ], BORDER_STRONG, 'var(--border-light)')

  // 仅文字色，不碰 color:#fff
  next = replaceProp(next, ['color', 'fill'], TEXT_PRIMARY, 'var(--text-primary)')
  next = replaceProp(next, ['color', 'fill'], TEXT_SECONDARY, 'var(--text-secondary)')
  next = replaceProp(next, ['color', 'fill'], TEXT_TERTIARY, 'var(--text-tertiary)')

  next = replaceGradientStops(next, SURFACE_PRIMARY, 'var(--bg-primary)')
  next = replaceGradientStops(next, SURFACE_SECONDARY, 'var(--bg-secondary)')
  next = replaceGradientStops(next, SURFACE_TERTIARY, 'var(--bg-tertiary)')

  // 页面/布局误用 Naive 局部变量：根节点没有 --n-color，fallback 浅色在暗色下露白
  next = next.replace(/var\(--n-color,\s*(?:#fff(?:fff)?|white)\)/gi, 'var(--bg-primary)')
  next = next.replace(/var\(--n-color,\s*var\(--bg-primary(?:,\s*[^)]*)?\)\)/gi, 'var(--bg-primary)')
  next = next.replace(/var\(--n-color-embedded,\s*var\(--bg-(?:secondary|tertiary)(?:,\s*[^)]*)?\)\)/gi, 'var(--bg-secondary)')
  next = next.replace(/var\(--n-color-embedded,\s*#[0-9a-fA-F]{3,8}\)/g, 'var(--bg-secondary)')
  next = next.replace(/var\(--n-border-color,\s*(?:var\(--border-light(?:,\s*[^)]*)?\)|#[0-9a-fA-F]{3,8})\)/g, 'var(--border-light)')
  next = next.replace(/var\(--n-text-color,\s*(?:var\(--text-primary(?:,\s*[^)]*)?\)|#[0-9a-fA-F]{3,8})\)/g, 'var(--text-primary)')
  next = next.replace(/var\(--n-text-color-2,\s*(?:var\(--text-secondary(?:,\s*[^)]*)?\)|#[0-9a-fA-F]{3,8})\)/g, 'var(--text-secondary)')
  next = next.replace(/var\(--n-text-color-3,\s*(?:var\(--text-tertiary(?:,\s*[^)]*)?\)|#[0-9a-fA-F]{3,8})\)/g, 'var(--text-tertiary)')

  // 灰度阶在 .dark 下不反转，禁止当页面底色
  next = next.replace(
    /((?:^|[;{\s])(?:background|background-color)\s*:\s*)var\(--(?:gray-50|gray-100|bg-page)(?:,\s*[^)]+)?\)/gi,
    '$1var(--bg-primary)',
  )

  // 选中态浅蓝底 / 半透明白遮罩
  next = next.replace(
    /((?:^|[;{\s])(?:background|background-color)\s*:\s*)#edf2ff\b/gi,
    '$1color-mix(in srgb, var(--primary-color, #165dff) 12%, var(--bg-primary))',
  )
  next = next.replace(
    /((?:^|[;{\s])(?:background|background-color)\s*:\s*)rgb\(\s*255\s+255\s+255\s*\/\s*([\d.]+)%?\s*\)/gi,
    (full, prefix, alpha) => {
      const a = Number(alpha)
      const pct = a > 1 ? Math.round(a) : Math.round(a * 100)
      return `${prefix}color-mix(in srgb, var(--bg-primary) ${pct}%, transparent)`
    },
  )

  // JS 主题表面字面量（如 portal-surface）
  next = next.replace(/(['"`])--portal-surface\1\s*:\s*(['"`])#ffffff?\2/gi, '$1--portal-surface$1: $2var(--bg-primary)$2')
  next = next.replace(/(['"`]--portal-surface['"`]\s*:\s*)['"`]#ffffff?['"`]/gi, '$1\'var(--bg-primary)\'')

  // 清理误替换产生的重复 fallback：var(--x, var(--x)) → var(--x)
  next = next.replace(/var\((--[\w-]+),\s*var\(\1\)\)/g, 'var($1)')

  return next
}

let filesTouched = 0

for (const root of roots) {
  const abs = path.resolve(root)
  if (!fs.existsSync(abs)) {
    console.error('skip missing', abs)
    continue
  }
  for (const file of walk(abs)) {
    if (shouldSkip(file))
      continue
    const before = fs.readFileSync(file, 'utf8')
    const after = transform(before)
    if (after !== before) {
      fs.writeFileSync(file, after)
      filesTouched += 1
      console.log('updated', path.relative(process.cwd(), file))
    }
  }
}

console.log(`\nDone. filesTouched=${filesTouched}`)
