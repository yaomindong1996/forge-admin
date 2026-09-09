/**
 * @fileoverview 统一组件注册表
 * @description 管理 102 个统一组件的 ComponentSpec，提供注册、查询、别名解析等 API。
 *   所有设计器（表单 / 列表 / 页面）消费同一份注册表数据。
 */

/** @type {Map<string, import('./types').ComponentSpec>} */
const specMap = new Map()

/** @type {Map<string, string>} alias → type 映射 */
const aliasMap = new Map()

/**
 * 注册单个组件 spec
 * @param {import('./types').ComponentSpec} spec
 */
export function registerComponent(spec) {
  if (!spec?.type) {
    throw new Error(`[designer-core] registerComponent: spec.type is required`)
  }
  if (specMap.has(spec.type)) {
    console.warn(`[designer-core] registerComponent: type "${spec.type}" already registered, overwriting`)
  }
  specMap.set(spec.type, Object.freeze({ ...spec, aliases: spec.aliases || [] }))
  for (const alias of (spec.aliases || [])) {
    if (aliasMap.has(alias) && aliasMap.get(alias) !== spec.type) {
      console.warn(`[designer-core] alias "${alias}" was mapped to "${aliasMap.get(alias)}", now remapped to "${spec.type}"`)
    }
    aliasMap.set(alias, spec.type)
  }
}

/**
 * 批量注册组件 spec
 * @param {import('./types').ComponentSpec[]} specs
 */
export function registerComponents(specs) {
  for (const spec of specs) {
    registerComponent(spec)
  }
}

/**
 * 按 type 或 alias 获取组件 spec
 * @param {string} typeOrAlias
 * @returns {import('./types').ComponentSpec | undefined}
 */
export function getComponentSpec(typeOrAlias) {
  if (specMap.has(typeOrAlias)) {
    return specMap.get(typeOrAlias)
  }
  const resolvedType = aliasMap.get(typeOrAlias)
  return resolvedType ? specMap.get(resolvedType) : undefined
}

/**
 * 解析 type 或 alias 为统一 type 名
 * @param {string} typeOrAlias
 * @returns {string}
 */
export function resolveComponentType(typeOrAlias) {
  if (specMap.has(typeOrAlias)) {
    return typeOrAlias
  }
  return aliasMap.get(typeOrAlias) || typeOrAlias
}

/**
 * 检查 type 或 alias 是否已注册
 * @param {string} typeOrAlias
 * @returns {boolean}
 */
export function hasComponent(typeOrAlias) {
  return specMap.has(typeOrAlias) || aliasMap.has(typeOrAlias)
}

/**
 * 获取全部已注册组件（按注册顺序）
 * @returns {import('./types').ComponentSpec[]}
 */
export function listAllComponents() {
  return Array.from(specMap.values())
}

/**
 * 按 scope 过滤组件列表
 * @param {'F' | 'L' | 'F+L'} scope
 * @returns {import('./types').ComponentSpec[]}
 */
export function listComponents(scope) {
  if (!scope) return listAllComponents()
  return listAllComponents().filter(spec => spec.scope === scope || spec.scope === 'F+L')
}

/**
 * 按 category 过滤组件列表
 * @param {string} category - field | layout | business | page | media | widget
 * @param {string} [scope] - 可选 scope 过滤
 * @returns {import('./types').ComponentSpec[]}
 */
export function listComponentsByCategory(category, scope) {
  let result = listAllComponents().filter(spec => spec.category === category)
  if (scope) {
    result = result.filter(spec => spec.scope === scope || spec.scope === 'F+L')
  }
  return result
}

/**
 * 按 group 分组返回组件列表
 * @param {string} [scope] - 可选 scope 过滤
 * @returns {Map<string, import('./types').ComponentSpec[]>}
 */
export function groupComponents(scope) {
  const components = scope ? listComponents(scope) : listAllComponents()
  const groups = new Map()
  for (const spec of components) {
    const group = spec.group || 'other'
    if (!groups.has(group)) {
      groups.set(group, [])
    }
    groups.get(group).push(spec)
  }
  return groups
}

/**
 * 获取所有已注册的 type 列表
 * @returns {string[]}
 */
export function listAllTypes() {
  return Array.from(specMap.keys())
}

/**
 * 获取所有 alias → type 映射
 * @returns {Map<string, string>}
 */
export function getAliasMap() {
  return new Map(aliasMap)
}

/**
 * 获取注册表统计信息
 * @returns {{ total: number, byCategory: Object<string, number>, byScope: Object<string, number>, aliases: number }}
 */
export function getRegistryStats() {
  const byCategory = {}
  const byScope = {}
  for (const spec of specMap.values()) {
    byCategory[spec.category] = (byCategory[spec.category] || 0) + 1
    byScope[spec.scope] = (byScope[spec.scope] || 0) + 1
  }
  return {
    total: specMap.size,
    byCategory,
    byScope,
    aliases: aliasMap.size,
  }
}

/**
 * 清空注册表（仅用于测试）
 */
export function clearRegistry() {
  specMap.clear()
  aliasMap.clear()
}
