import fs from 'node:fs/promises'
import { exists } from '../forge-shared/files.mjs'

const pluginModulesBegin = '<!-- forge-plugins:modules:begin -->'
const pluginModulesEnd = '<!-- forge-plugins:modules:end -->'

export async function replacePomModules(pomFile, modules) {
  if (!(await exists(pomFile))) {
    return
  }
  const content = await fs.readFile(pomFile, 'utf8')
  await fs.writeFile(pomFile, renderPomModules(content, modules))
}

export function renderPomModules(content, modules) {
  const moduleBody = content.match(/<modules>([\s\S]*?)<\/modules>/)?.[1] || ''
  const pluginMarkers = readEmptyPluginModuleMarkers(content, moduleBody)
  const moduleContent = [
    '    <modules>',
    ...modules.map(moduleName => `        <module>${moduleName}</module>`),
    ...pluginMarkers,
    '    </modules>',
  ].join('\n')
  return content.includes('<modules>')
    ? content.replace(/[\t ]*<modules>[\s\S]*?<\/modules>/, moduleContent)
    : content
}

// 裁剪只接受干净模板；已有插件或损坏标记必须拒绝，避免丢配置。
function readEmptyPluginModuleMarkers(content, moduleBody) {
  const begins = content.split(pluginModulesBegin).length - 1
  const ends = content.split(pluginModulesEnd).length - 1
  if (begins === 0 && ends === 0) {
    return []
  }
  const markers = moduleBody.match(
    /^[\t ]*<!-- forge-plugins:modules:begin -->\s*<!-- forge-plugins:modules:end -->/m,
  )
  if (begins !== 1 || ends !== 1 || !markers) {
    throw new Error('POM 插件 modules 标记必须唯一、完整、位于 modules 内且为空；'
      + '请使用干净模板生成工程')
  }
  return [markers[0]]
}
