import path from 'node:path'
import { applyTextReplacements } from '../forge-shared/rename.mjs'

// ID/功能编码属于跨端协议，不是宿主品牌；仅保护完整标识，Java 包前缀仍正常改名。
export function identityContext(context, descriptor) {
  const values = [...new Set([descriptor.id, ...descriptor.features])].sort((a, b) => b.length - a.length)
  const escaped = values.map(value => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
  const identity = `(?<![\\w.-])(?:${escaped.join('|')})(?![\\w.-])`
  const pattern = new RegExp(`(${identity})`, 'g')
  const javaPattern = new RegExp(`(^[\\t ]*(?:package|import)[\\t ]+[^;\\r\\n]*;|${identity})`, 'gm')
  const directoryMap = { ...context.artifactMap }
  delete directoryMap[descriptor.id]
  const transformText = (file, content, replacements) => {
    // 同名的 Maven artifact 必须改，不能因 ID 与 artifact 同名而把依赖留在原坐标。
    if (path.basename(file) === 'pom.xml') {
      return applyTextReplacements(content, replacements)
    }
    const java = file.endsWith('.java')
    return content.split(java ? javaPattern : pattern).map((part, index) => {
      const packageReference = java && /^[\t ]*(?:package|import)[\t ]/.test(part)
      return index % 2 && !packageReference ? part : applyTextReplacements(part, replacements)
    }).join('')
  }
  return { ...context, directoryMap, transformText }
}
