import path from 'node:path'
import { parseStrictJson } from '../forge-plugin/json.mjs'
import { editBlock } from '../forge-plugin/pom.mjs'

// 分段表达规则，全文扫描自身也无需豁免文档、测试或检查脚本。
const namespaces = ['com.mdframe.forge' + '.ee', 'com/mdframe/forge' + '/ee']
export const maxFileBytes = 64 * 1024 * 1024
export const requiredPoms = new Map([
  ['forge-server/pom.xml', 'modules'],
  ['forge-server/forge-admin-server/pom.xml', 'dependencies'],
])
export const installedPaths = ['forge-server/plugins/', 'forge-admin-ui/src/views/plugins/']

export function inspectContent(relative, bytes) {
  const rules = []
  if (namespaces.some(value => relative.includes(value) || bytes.includes(Buffer.from(value)))) {
    rules.push('企业版包前缀禁止提交')
  }
  if (path.posix.basename(relative) === 'forge-plugin.json') {
    inspectDescriptor(bytes, rules)
  }
  if (path.posix.basename(relative) === 'pom.xml') {
    inspectPom(relative, bytes, rules)
  }
  return rules
}

function inspectDescriptor(bytes, rules) {
  try {
    const value = parseStrictJson(new TextDecoder('utf-8', { fatal: true }).decode(bytes))
    if (!value || Array.isArray(value) || value.edition !== 'community') {
      rules.push('插件描述必须明确 edition=community')
    }
  }
  catch {
    // JSON.parse 的错误可能包含原文片段；对外仅返回规则，不泄露文件内容。
    rules.push('插件描述 JSON 损坏、重复键或超过限制')
  }
}

function inspectPom(relative, bytes, rules) {
  const text = bytes.toString('utf8')
  const required = requiredPoms.get(relative)
  if (!required && !text.includes('forge-plugins')) {
    return
  }
  try {
    const markers = [...text.matchAll(/forge-plugins(?::[\w-]+)*/g)].map(match => match[0])
    if (markers.some(value => !/^forge-plugins:(modules|dependencies):(begin|end)$/.test(value))) {
      throw new Error('未知标记')
    }
    for (const kind of ['modules', 'dependencies']) {
      if (required === kind || markers.some(value => value.startsWith(`forge-plugins:${kind}:`))) {
        inspectBlock(text, markers, kind)
      }
    }
  }
  catch {
    rules.push('POM 插件标记必须完整、唯一、直属且为空')
  }
}

function inspectBlock(text, markers, kind) {
  if (markers.filter(value => value.startsWith(`forge-plugins:${kind}:`)).length !== 2) {
    throw new Error('标记重复或缺失')
  }
  editBlock(text, kind, { old: [], next: [] })
}
