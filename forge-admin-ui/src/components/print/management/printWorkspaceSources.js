import { printSourceFromQuery } from './printRouteContext'

function object(value) {
  if (typeof value === 'string') {
    try {
      return object(JSON.parse(value))
    }
    catch { return {} }
  }
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
}
export const printSourceKey = source => source ? `${source.pageId || source.formKey}:${source.objectCode}` : ''

/** 与后端来源检查一致：只接受页面实际引用且仍属于应用的对象。 */
export function printWorkspaceSources(application, objects = []) {
  const options = object(application?.options)
  const builder = object(options.inAppBuilder)
  const nodes = Array.isArray(builder.nodes) ? [...builder.nodes] : []
  const pages = object(builder.pages)
  if (!nodes.length && !Object.keys(pages).length && !builder.legacyObjectPageMigrated) {
    const primary = objects.find(item => item.objectCode === options.primaryObjectCode)
    if (primary) {
      const token = primary.objectCode.trim().toLowerCase().replace(/[^a-z0-9_]+/g, '_').replace(/^_+|_+$/g, '')
      nodes.push({ id: `page_${token || 'legacy_object'}`, type: 'page', title: primary.objectName, objectRef: { ...primary } })
    }
  }
  const result = new Map()
  for (const node of nodes.filter(item => item?.type === 'page')) {
    const visit = (value) => {
      if (!value || typeof value !== 'object')
        return
      const ref = object(value.objectRef)
      const model = objects.find(item => String(item.objectId) === String(ref.objectId)
        && item.objectCode === ref.objectCode && item.configKey === ref.configKey && item.configKey)
      if (model && (!Object.hasOwn(ref, 'valid') || ref.valid === true)) {
        const source = printSourceFromQuery({ applicationId: String(application.id), sourceType: 'LOWCODE', pageId: String(node.id), objectCode: model.objectCode })
        if (source) {
          const key = printSourceKey(source)
          result.set(key, { value: key, label: `${node.title || node.name || '页面'} / ${model.objectName || '业务表单'}`, source })
        }
      }
      Object.values(value).forEach(visit)
    }
    visit(node)
    visit(pages[node.id])
  }
  return [...result.values()]
}
