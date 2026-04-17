import { packagesList } from '@/packages'
import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'

// 组件描述信息
export interface ComponentDescriptor {
  key: string
  title: string
  package: PackagesCategoryEnum
  category: string
  categoryName: string
  chartFrame?: ChartFrameEnum
  redirectComponent?: string
  defaultW: number
  defaultH: number
  configType: ConfigType
}

// 从 packagesList 构建扁平化的 key → descriptor 映射
let registryCache: Map<string, ComponentDescriptor> | null = null

export function getComponentRegistry(): Map<string, ComponentDescriptor> {
  if (registryCache) return registryCache

  registryCache = new Map()
  const packages: PackagesCategoryEnum[] = [
    PackagesCategoryEnum.CHARTS,
    PackagesCategoryEnum.VCHART,
    PackagesCategoryEnum.INFORMATIONS,
    PackagesCategoryEnum.TABLES,
    PackagesCategoryEnum.DECORATES,
    PackagesCategoryEnum.PHOTOS,
    PackagesCategoryEnum.ICONS
  ]

  packages.forEach(pkg => {
    const list = packagesList[pkg]
    if (!list) return
    list.forEach((item: ConfigType) => {
      if (item.disabled) return
      registryCache!.set(item.key, {
        key: item.key,
        title: item.title,
        package: item.package,
        category: item.category,
        categoryName: item.categoryName,
        chartFrame: item.chartFrame,
        redirectComponent: item.redirectComponent,
        defaultW: chartInitConfig.w,
        defaultH: chartInitConfig.h,
        configType: item
      })
    })
  })

  return registryCache
}

// 根据 key 查找 ConfigType
export function findConfigTypeByKey(key: string): ConfigType | undefined {
  const registry = getComponentRegistry()
  return registry.get(key)?.configType
}

// 获取组件目录文本（可附在 AI 请求中供后端参考）
export function getComponentCatalogText(): string {
  const registry = getComponentRegistry()
  const lines: string[] = []
  const packages: PackagesCategoryEnum[] = [
    PackagesCategoryEnum.CHARTS,
    PackagesCategoryEnum.VCHART,
    PackagesCategoryEnum.INFORMATIONS,
    PackagesCategoryEnum.TABLES,
    PackagesCategoryEnum.DECORATES,
    PackagesCategoryEnum.PHOTOS,
    PackagesCategoryEnum.ICONS
  ]

  packages.forEach(pkg => {
    const items: ComponentDescriptor[] = []
    registry.forEach(desc => {
      if (desc.package === pkg) items.push(desc)
    })
    if (items.length === 0) return
    lines.push(`\n[${pkg}]`)
    items.forEach(item => {
      lines.push(`  - ${item.key}: ${item.title} (${item.categoryName})`)
    })
  })

  return lines.join('\n')
}

// 获取所有可用的组件 key 列表
export function getAvailableComponentKeys(): string[] {
  const registry = getComponentRegistry()
  return Array.from(registry.keys())
}
