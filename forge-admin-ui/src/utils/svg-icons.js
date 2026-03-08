// 工具函数：动态导入 SVG 图标
export async function loadSvgIcons() {
  try {
    // 导入所有图标（包括子目录和根目录中的图标）
    const allIconModules = import.meta.glob('@/assets/icons/**/*.svg', { eager: true, as: 'component' })

    const components = {}
    const names = []

    // 处理所有图标
    for (const path in allIconModules) {
      // 匹配子目录中的图标：icons/目录名/文件名.svg
      const subdirMatch = path.match(/icons\/([\w-]+)\/([\w-]+)\.svg$/)
      if (subdirMatch) {
        const dirName = subdirMatch[1]
        const fileName = subdirMatch[2]
        // 保持与之前一致的命名方式，不添加 local: 前缀
        const name = `${fileName}`
        components[name] = allIconModules[path]
        names.push(name)
        continue
      }

      // 匹配根目录中的图标：icons/文件名.svg
      const rootMatch = path.match(/icons\/([\w-]+)\.svg$/)
      if (rootMatch) {
        const name = rootMatch[1]
        components[name] = allIconModules[path]
        names.push(name)
      }
    }

    return { components, names }
  } catch (error) {
    console.error('加载 SVG 图标失败:', error)
    return { components: {}, names: [] }
  }
}
