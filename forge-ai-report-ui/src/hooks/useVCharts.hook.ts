import { watch } from 'vue'
import { VChart, type ITheme } from '@visactor/vchart'

import light from '@visactor/vchart-theme/public/light.json'
import dark from '@visactor/vchart-theme/public/dark.json'
import vScreenVolcanoBlue from '@visactor/vchart-theme/public/vScreenVolcanoBlue.json'
import vScreenClean from '@visactor/vchart-theme/public/vScreenClean.json'
import vScreenOutskirts from '@visactor/vchart-theme/public/vScreenOutskirts.json'
import vScreenBlueOrange from '@visactor/vchart-theme/public/vScreenBlueOrange.json'
import vScreenFinanceYellow from '@visactor/vchart-theme/public/vScreenFinanceYellow.json'
import vScreenWenLvCyan from '@visactor/vchart-theme/public/vScreenWenLvCyan.json'
import vScreenElectricGreen from '@visactor/vchart-theme/public/vScreenElectricGreen.json'
import vScreenECommercePurple from '@visactor/vchart-theme/public/vScreenECommercePurple.json'
import vScreenRedBlue from '@visactor/vchart-theme/public/vScreenRedBlue.json'
import vScreenPartyRed from '@visactor/vchart-theme/public/vScreenPartyRed.json'
// 行业色板
import veODesignLightFinance from '@visactor/vchart-theme/public/veODesignLightFinance.json'
import veODesignDarkFinance from '@visactor/vchart-theme/public/veODesignDarkFinance.json'

import veODesignLightGovernment from '@visactor/vchart-theme/public/veODesignLightGovernment.json'
import veODesignDarkGovernment from '@visactor/vchart-theme/public/veODesignDarkGovernment.json'

import veODesignLightConsumer from '@visactor/vchart-theme/public/veODesignLightConsumer.json'
import veODesignDarkConsumer from '@visactor/vchart-theme/public/veODesignDarkConsumer.json'

import veODesignLightAutomobile from '@visactor/vchart-theme/public/veODesignLightAutomobile.json'
import veODesignDarkAutomobile from '@visactor/vchart-theme/public/veODesignDarkAutomobile.json'

import veODesignLightCulturalTourism from '@visactor/vchart-theme/public/veODesignLightCulturalTourism.json'
import veODesignDarkCulturalTourism from '@visactor/vchart-theme/public/veODesignDarkCulturalTourism.json'

import veODesignLightMedical from '@visactor/vchart-theme/public/veODesignLightMedical.json'
import veODesignDarkMedical from '@visactor/vchart-theme/public/veODesignDarkMedical.json'

import veODesignLightNewEnergy from '@visactor/vchart-theme/public/veODesignLightNewEnergy.json'
import veODesignDarkNewEnergy from '@visactor/vchart-theme/public/veODesignDarkNewEnergy.json'

const themeMap = {
  // 明亮
  light: light,
  // 暗黑
  dark: dark,
  // 火山蓝
  vScreenVolcanoBlue: vScreenVolcanoBlue,
  // 党建红
  vScreenPartyRed: vScreenPartyRed,
  // 清新蜡笔
  vScreenClean: vScreenClean,
  // 郊外
  vScreenOutskirts: vScreenOutskirts,
  // 汽车蓝橙
  vScreenBlueOrange: vScreenBlueOrange,
  // 金融黄
  vScreenFinanceYellow: vScreenFinanceYellow,
  // 文旅青
  vScreenWenLvCyan: vScreenWenLvCyan,
  // 电力绿
  vScreenElectricGreen: vScreenElectricGreen,
  // 电商紫
  vScreenECommercePurple: vScreenECommercePurple,
  // 红蓝
  vScreenRedBlue: vScreenRedBlue,
  // 金融行业色板
  veODesignLightFinance: veODesignLightFinance,
  veODesignDarkFinance: veODesignDarkFinance,
  // 政府行业色板
  veODesignLightGovernment: veODesignLightGovernment,
  veODesignDarkGovernment: veODesignDarkGovernment,
  // 消费行业色板
  veODesignLightConsumer: veODesignLightConsumer,
  veODesignDarkConsumer: veODesignDarkConsumer,
  // 汽车行业色板
  veODesignLightAutomobile: veODesignLightAutomobile,
  veODesignDarkAutomobile: veODesignDarkAutomobile,
  // 文旅行业色板
  veODesignLightCulturalTourism: veODesignLightCulturalTourism,
  veODesignDarkCulturalTourism: veODesignDarkCulturalTourism,
  // 医疗行业色板
  veODesignLightMedical: veODesignLightMedical,
  veODesignDarkMedical: veODesignDarkMedical,
  // 新能源行业色板
  veODesignLightNewEnergy: veODesignLightNewEnergy,
  veODesignDarkNewEnergy: veODesignDarkNewEnergy
}

export const useVCharts = () => {
  const getThemeMap = () => {
    return themeMap
  }

  // 注册主题（支持自定义主题）
  const registerTheme = (themeName: keyof typeof themeMap, theme: any) => {
    VChart.ThemeManager.registerTheme(themeName, (themeMap[themeName] as any) || theme)
  }

  // 设置当前主题
  const setCurrentTheme = (themeName = 'vScreenVolcanoBlue') => {
    VChart.ThemeManager.setCurrentTheme(themeName)
  }

  // 判断主题是否存在
  const themeExist = (name: string): boolean => {
    return VChart.ThemeManager.themeExist(name)
  }

  // 获取主题
  const getTheme = (name: string): ITheme => {
    return VChart.ThemeManager.getTheme(name)
  }

  // 获取当前主题
  const getCurrentTheme = (): ITheme => {
    return VChart.ThemeManager.getCurrentTheme()
  }

  // 设置主题
  const setTheme = (name: keyof typeof themeMap): boolean => {
    if (themeExist(name)) {
      setCurrentTheme(name)
      return true
    } else {
      // 先注册
      const theme = themeMap[name]
      if (theme) {
        registerTheme(name, theme)
        setCurrentTheme(name)
        return true
      } else {
        // 注册默认主题
        registerTheme('vScreenVolcanoBlue', vScreenVolcanoBlue)
      }
    }
    return false
  }

  return {
    getThemeMap,
    registerTheme,
    setCurrentTheme,
    themeExist,
    getTheme,
    setTheme,
    getCurrentTheme
  }
}

// 主题初始化
export const useInitVChartsTheme = (chartEditStore: any) => {
  const vCharts = useVCharts()

  const initVChartsThemeIns = watch(
    () => chartEditStore.getEditCanvasConfig.vChartThemeName,
    (newTheme: string) => {
      vCharts.setTheme(newTheme as any)
    },
    {
      immediate: true
    }
  )

  return {
    initVChartsThemeIns
  }
}
