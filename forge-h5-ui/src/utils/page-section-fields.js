const DEFAULT_SECTION = { sectionId: 'section_default', sectionType: 'card', title: '基本信息', collapsible: false }

function isContentSection(section) {
  return Boolean(section) && String(section.sectionType || '') !== 'child_table' && !section.relationKey
}

/**
 * 分区里漏掉的主表字段补进第一个内容分区。
 * 设计态改字段编码后分区引用可能停留在旧编码，移动端按分区渲染时会把这些字段整块丢掉；
 * 被节点权限隐藏的字段不补，保持原有可见性。
 */
export function completePageSectionFields(sections = [], mainFields = []) {
  const list = Array.isArray(sections) ? sections : []
  if (!list.length)
    return list
  const referenced = new Set(list.flatMap(section =>
    Array.isArray(section?.fields) ? section.fields.map(String) : []))
  const missing = (Array.isArray(mainFields) ? mainFields : [])
    .filter(field => field && field.hidden !== true)
    .map(field => String(field.field || ''))
    .filter(code => code && !referenced.has(code))
  if (!missing.length)
    return list
  const targetIndex = list.findIndex(isContentSection)
  if (targetIndex < 0)
    return [{ ...DEFAULT_SECTION, fields: missing }, ...list]
  return list.map((section, index) => index === targetIndex
    ? { ...section, fields: [...(Array.isArray(section.fields) ? section.fields : []), ...missing] }
    : section)
}
