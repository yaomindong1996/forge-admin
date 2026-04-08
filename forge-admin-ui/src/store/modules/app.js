import { generate, getRgbStr } from '@arco-design/color'
import { useDark } from '@vueuse/core'
import { defineStore } from 'pinia'
import { defaultLayout, defaultPrimaryColor, naiveThemeOverrides } from '@/settings'
import { defaultThemeConfig, applyThemeConfig } from '@/config/theme.config'

export const useAppStore = defineStore('app', {
  state: () => ({
    collapsed: false,
    isDark: useDark(),
    layout: import.meta.env.VITE_DEFAULT_LAYOUT || defaultLayout,
    primaryColor: defaultPrimaryColor,
    naiveThemeOverrides,
    selectedTopMenuId: null, // 当前选中的顶部菜单ID
    themeConfig: defaultThemeConfig, // 主题配置
    routeGuardCompleted:null
  }),
  actions: {
    switchCollapsed() {
      this.collapsed = !this.collapsed
    },
    setCollapsed(b) {
      this.collapsed = b
    },
    toggleDark() {
      this.isDark = !this.isDark
    },
    setLayout(v) {
      this.layout = v
    },
    setPrimaryColor(color) {
      this.primaryColor = color
    },
    setThemeColor(color = this.primaryColor, isDark = this.isDark) {
      document.body.style.setProperty('--primary-color', color)
      const colors = generate(color, {
        list: true,
        dark: isDark,
      })
      this.naiveThemeOverrides.common = {
        ...this.naiveThemeOverrides.common,
        primaryColor: colors[5],
        primaryColorHover: colors[4],
        primaryColorSuppl: colors[4],
        primaryColorPressed: colors[6],
      }
    },
    setSelectedTopMenuId(id) {
      this.selectedTopMenuId = id
    },
    setThemeConfig(config) {
      this.themeConfig = { ...this.themeConfig, ...config }
      // 同步外层的 primaryColor 和 themeConfig.primaryColor
      if (config.primaryColor) {
        this.primaryColor = config.primaryColor
        this.setThemeColor(config.primaryColor)
      }
      applyThemeConfig(this.themeConfig, this.isDark)
    },
    updateHeaderConfig(headerConfig) {
      this.themeConfig.header = { ...this.themeConfig.header, ...headerConfig }
      applyThemeConfig(this.themeConfig, this.isDark)
    },
    updateTopMenuConfig(topMenuConfig) {
      this.themeConfig.topMenu = { ...this.themeConfig.topMenu, ...topMenuConfig }
      applyThemeConfig(this.themeConfig, this.isDark)
    },
    updateSideMenuConfig(sideMenuConfig) {
      this.themeConfig.sideMenu = { ...this.themeConfig.sideMenu, ...sideMenuConfig }
      applyThemeConfig(this.themeConfig, this.isDark)
    },
    applyCurrentTheme() {
      applyThemeConfig(this.themeConfig, this.isDark)
    },
    // 添加设置路由守卫完成状态的方法
    setRouteGuardCompleted(completed) {
      this.routeGuardCompleted = completed
    },
    updateNaiveThemeOverrides(common) {
      this.naiveThemeOverrides.common = {
        ...this.naiveThemeOverrides.common,
        ...common
      }
    },
  },
  persist: {
    key: `${import.meta.env.VITE_TENANT || 'default'}_app`,
    pick: ['collapsed', 'layout', 'primaryColor', 'naiveThemeOverrides', 'themeConfig'],
    storage: sessionStorage,
  },
})
