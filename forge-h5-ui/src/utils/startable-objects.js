import { resolveMobileMenuIcon, resolveMobileMenuTone } from './mobile-menu.js'

const CONFIG_KEY_PARAMS = ['configKey', 'runtimeConfigKey', 'pageConfigKey']
const CONTEXT_PARAMS = ['appId', 'applicationId']
const OTHER_GROUP_KEY = 'other'

function parseQuery(url = '') {
  const query = String(url).split('?')[1] || ''
  return query.split('&').filter(Boolean).reduce((result, pair) => {
    const [rawKey, rawValue = ''] = pair.split('=')
    try { result[decodeURIComponent(rawKey)] = decodeURIComponent(rawValue) }
    catch { result[rawKey] = rawValue }
    return result
  }, {})
}

// 菜单入口 → configKey 索引；只有用户有菜单权限的单据才出现在发起页。
export function indexMenusByConfigKey(menuItems = []) {
  const index = new Map()
  for (const item of menuItems) {
    const query = parseQuery(item?.target?.url)
    const configKey = CONFIG_KEY_PARAMS.map(name => query[name]).find(Boolean)
    if (configKey && !index.has(configKey)) index.set(configKey, { ...item, query })
  }
  return index
}

export function buildStartUrl(object = {}, menuEntry = {}) {
  // flow=1 让新建页展示「提交审批」，保存后按单据运行态发起。
  const params = { configKey: object.configKey, mode: 'create', flow: '1' }
  for (const name of CONTEXT_PARAMS) {
    if (menuEntry.query?.[name]) params[name] = menuEntry.query[name]
  }
  if (!params.applicationId && object.applicationId) params.applicationId = object.applicationId
  if (object.objectName) params.title = object.objectName
  const query = Object.entries(params)
    .map(([key, value]) => `${key}=${encodeURIComponent(String(value))}`)
    .join('&')
  return `/pages/lowcode-runtime?${query}`
}

function matches(keyword, ...values) {
  return !keyword || values.some(value => String(value || '').toLowerCase().includes(keyword))
}

// 按应用分组并保持接口顺序；未挂应用的单据放到最后的"其他"分组。
export function buildStartableGroups(objects = [], menuIndex = new Map(), keyword = '') {
  const needle = String(keyword || '').trim().toLowerCase()
  const groups = new Map()
  for (const object of Array.isArray(objects) ? objects : []) {
    const menuEntry = menuIndex.get(object?.configKey)
    if (!menuEntry) continue
    const groupKey = object.applicationId ? String(object.applicationId) : OTHER_GROUP_KEY
    const groupLabel = object.applicationName || '其他'
    if (!matches(needle, groupLabel, object.objectName)) continue
    if (!groups.has(groupKey)) groups.set(groupKey, { key: groupKey, label: groupLabel, items: [] })
    const group = groups.get(groupKey)
    if (group.items.some(item => item.configKey === object.configKey)) continue
    const identity = { id: object.objectCode, icon: object.objectIcon, resourceName: object.objectName }
    const tone = resolveMobileMenuTone(identity)
    group.items.push({
      key: `${groupKey}:${object.objectCode}`,
      label: object.objectName || menuEntry.label,
      configKey: object.configKey,
      icon: resolveMobileMenuIcon(identity),
      color: tone.color,
      toneBg: tone.bg,
      url: buildStartUrl(object, menuEntry),
    })
  }
  const list = [...groups.values()]
  const otherIndex = list.findIndex(group => group.key === OTHER_GROUP_KEY)
  if (otherIndex >= 0) list.push(...list.splice(otherIndex, 1))
  return list
}
