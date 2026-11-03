/**
 * 平台管理表单卡片头图标：只用系统 ForgeSymbol 白名单（forge:*），
 * 不依赖 UnoCSS 动态 material-symbols class。
 * @see src/components/common/forgeSymbols.js
 */
export const SYSTEM_EDIT_CARD_ICONS = [
  'forge:module',
  'forge:platform',
  'forge:layers',
  'forge:shield',
  'forge:actions',
  'forge:apps',
  'forge:collaboration',
  'forge:connection',
  'forge:builder',
  'forge:database',
  'forge:approval',
  'forge:monitor',
  'forge:message',
  'forge:schedule',
]

/** 保留空实现，兼容 main.js 旧调用 */
export function ensureSystemEditCardIconsMounted() {}
