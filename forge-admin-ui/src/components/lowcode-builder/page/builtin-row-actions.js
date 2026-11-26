/**
 * 列表行内置操作（编辑/详情/删除）：设计器可改名、隐藏/删除，运行态按配置注入。
 */

export const BUILTIN_ROW_ACTION_KEYS = ['edit', 'detail', 'delete', 'addChild']

export const DEFAULT_BUILTIN_ROW_ACTIONS = [
  { key: 'edit', label: '编辑', type: 'primary', position: 'row', builtin: true, visible: true },
  { key: 'detail', label: '查看详情', type: 'info', position: 'row', builtin: true, visible: true },
  { key: 'delete', label: '删除', type: 'error', position: 'row', builtin: true, visible: true },
]

export function isBuiltinRowActionKey(key = '') {
  return BUILTIN_ROW_ACTION_KEYS.includes(String(key || '').trim())
}

function normalizeKey(key = '') {
  return String(key || '').trim()
}

/**
 * 合并设计器保存的覆盖项与默认内置操作。
 * list 为空/缺省时返回全部默认可见；已保存的项保留 label/type/visible。
 */
export function normalizeBuiltinRowActions(list = [], { treeRuntime = false } = {}) {
  const overrides = new Map()
  ;(Array.isArray(list) ? list : []).forEach((item) => {
    const key = normalizeKey(item?.key)
    if (!isBuiltinRowActionKey(key) || key === 'addChild')
      return
    overrides.set(key, item)
  })
  const addChildOverride = (Array.isArray(list) ? list : []).find(item => normalizeKey(item?.key) === 'addChild')

  const defaults = DEFAULT_BUILTIN_ROW_ACTIONS.map(item => ({ ...item }))
  if (treeRuntime) {
    defaults.splice(2, 0, {
      key: 'addChild',
      label: '添加下级',
      type: 'success',
      position: 'row',
      builtin: true,
      visible: true,
    })
  }

  return defaults.map((item) => {
    const override = item.key === 'addChild' ? addChildOverride : overrides.get(item.key)
    const next = {
      ...item,
      ...(override && typeof override === 'object' ? override : {}),
      key: item.key,
      position: 'row',
      builtin: true,
      clientKey: `builtin_${item.key}`,
      actionType: 'builtin',
    }
    next.visible = override?.visible === false ? false : true
    next.label = String(next.label || item.label || item.key).trim() || item.label
    next.type = next.type || item.type
    return next
  })
}

export function resolveVisibleBuiltinRowActions(list = [], options = {}) {
  return normalizeBuiltinRowActions(list, options).filter(action => action.visible !== false)
}

export function builtinRowActionBehaviorLabel(action = {}) {
  const key = normalizeKey(action.key)
  if (key === 'edit')
    return '内置 · 编辑'
  if (key === 'detail')
    return '内置 · 查看详情'
  if (key === 'delete')
    return '内置 · 删除'
  if (key === 'addChild')
    return '内置 · 添加下级'
  return '内置操作'
}
