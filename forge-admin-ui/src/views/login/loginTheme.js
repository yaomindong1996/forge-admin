import { naiveThemeOverrides } from '@/settings'

/**
 * 登录页控件尺寸独立；主色跟随租户/全局主题，避免与登录后控制台色差。
 */
export function buildLoginThemeOverrides(common = {}) {
  const baseCommon = naiveThemeOverrides.common || {}
  return {
    common: {
      ...baseCommon,
      ...common,
      borderRadius: '6px',
    },
    Input: { heightLarge: '40px' },
    Button: {
      ...(naiveThemeOverrides.Button || {}),
      heightLarge: '44px',
      borderRadiusLarge: '4px',
    },
  }
}

export const loginThemeOverrides = buildLoginThemeOverrides()
