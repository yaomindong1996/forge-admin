import { parsePom, child, field, replaceNodeText } from './xml.mjs'
import { finalModule } from './project.mjs'
import { requireCondition } from './paths.mjs'

export function preparePoms(context, records) {
  const oldRecords = context.config.plugins
  const result = {}
  result[context.rootPom] = editBlock(context.files[context.rootPom], 'modules', {
    old: moduleItems(context, oldRecords), next: moduleItems(context, records) })
  result[context.adminPom] = editBlock(context.files[context.adminPom], 'dependencies', {
    old: dependencyItems(context, oldRecords), next: dependencyItems(context, records) })
  return result
}

function moduleItems(context, records) {
  return records.filter(item => item.server).map(item => `<module>plugins/${finalModule(context, item)}</module>`)
}

function dependencyItems(context, records) {
  return records.filter(item => item.server).map(item => '<dependency>'
    + `<groupId>${context.options.groupId}</groupId><artifactId>${finalModule(context, item)}</artifactId>`
    + '<version>${revision}</version></dependency>')
}

export function editBlock(text, kind, items) {
  const root = parsePom(text)
  const parent = child(root, kind)
  const begin = findMarker(root, parent, `forge-plugins:${kind}:begin`)
  const end = findMarker(root, parent, `forge-plugins:${kind}:end`)
  requireCondition(begin.end < end.start, 'POM 插件标记倒序')
  const current = text.slice(begin.end, end.start)
  requireCondition(compact(current) === compact(items.old.join('')), 'POM 插件区块与配置登记不一致')
  const outside = parent.children.filter(node => node.start < begin.end || node.end > end.start)
  const nextNames = items.next.map(value => fieldFromItem(value, kind))
  const outsideNames = outside.map(node => kind === 'modules' ? node.text.trim() : field(node, 'artifactId'))
  requireCondition(new Set(nextNames).size === nextNames.length
    && nextNames.every(name => !outsideNames.includes(name)), '插件 module/dependency 与宿主重名')
  const indent = text.slice(text.lastIndexOf('\n', begin.start) + 1, begin.start)
  requireCondition(!indent.trim(), 'POM 插件标记须单独占行')
  const newline = text.includes('\r\n') ? '\r\n' : '\n'
  const body = items.next.map(value => `${indent}${value}${newline}`).join('')
  return text.slice(0, begin.end) + newline + body + indent + text.slice(end.start)
}

function findMarker(root, parent, name) {
  const matches = []
  const visit = node => {
    matches.push(...node.comments.filter(comment => comment.text === name).map(comment => ({ node, comment })))
    node.children.forEach(visit)
  }
  visit(root)
  requireCondition(matches.length === 1 && matches[0].node === parent, 'POM 插件标记缺失/重复/不在直属节点')
  return matches[0].comment
}

function compact(text) {
  return text.replace(/\s/g, '')
}

function fieldFromItem(text, kind) {
  const node = parsePom(`<project>${text}</project>`).children[0]
  return kind === 'modules' ? node.text.trim() : field(node, 'artifactId')
}

export function validateSourcePom(text, module) {
  const root = parsePom(text)
  const parent = child(root, 'parent')
  requireCondition(field(root, 'artifactId') === module && !child(root, 'modules', false),
    '插件 POM 必须为声明的单模块')
  requireCondition(field(parent, 'groupId') === 'com.mdframe.forge' && field(parent, 'artifactId') === 'forge-server'
    && field(parent, 'version') === '${revision}', '插件 parent 必须继承 Forge 根 POM/revision')
  const version = field(root, 'version', false)
  requireCondition(version === undefined || version === '${revision}', '插件 Maven 版本须继承 revision')
  const group = field(root, 'groupId', false)
  requireCondition(group === undefined || group === 'com.mdframe.forge', '插件 groupId 非法')
  const properties = child(root, 'properties', false)
  requireCondition(!properties || !child(properties, 'revision', false), '插件不能覆盖工程 revision')
}

export function normalizeParentPath(text) {
  const parent = child(parsePom(text), 'parent')
  const relative = child(parent, 'relativePath', false)
  if (relative) {
    if (relative.closeStart === undefined) {
      return text.slice(0, relative.start) + '<relativePath>../../pom.xml</relativePath>' + text.slice(relative.end)
    }
    return replaceNodeText(text, relative, '../../pom.xml')
  }
  return text.slice(0, parent.closeStart) + '<relativePath>../../pom.xml</relativePath>' + text.slice(parent.closeStart)
}
