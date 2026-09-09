/**
 * @fileoverview 数据源规范
 * @description 定义 7 种数据源类型枚举及其校验、解析工具函数。
 *   所有组件 spec 的 dataSources 字段引用此模块的 KIND 常量。
 */

/**
 * 数据源类型定义表
 * @type {Object<string, { kind: string, label: string, desc: string, needApi: boolean, needDictType: boolean }>}
 */
export const DATA_SOURCE_KINDS = {
  static: {
    kind: 'static',
    label: '静态数据',
    desc: '组件内嵌 options 列表',
    needApi: false,
    needDictType: false,
  },
  dict: {
    kind: 'dict',
    label: '数据字典',
    desc: '通过 dictType 从 sys_dict_data 加载',
    needApi: false,
    needDictType: true,
  },
  managed: {
    kind: 'managed',
    label: '托管模型',
    desc: '绑定业务模型，由框架提供数据',
    needApi: false,
    needDictType: false,
  },
  remote: {
    kind: 'remote',
    label: '远程接口',
    desc: '通过 HTTP 接口获取数据',
    needApi: true,
    needDictType: false,
  },
  context: {
    kind: 'context',
    label: '上下文',
    desc: '从当前记录 / 父组件传递',
    needApi: false,
    needDictType: false,
  },
  relation: {
    kind: 'relation',
    label: '关联关系',
    desc: '模型间关联数据',
    needApi: false,
    needDictType: false,
  },
  builtin: {
    kind: 'builtin',
    label: '内置数据',
    desc: '系统内置数据源（用户/组织/区划）',
    needApi: false,
    needDictType: false,
  },
}

/**
 * 获取组件支持的数据源类型详情列表
 * @param {string[]} kinds - 数据源类型列表
 * @returns {Array<{ kind: string, label: string, desc: string, needApi: boolean, needDictType: boolean }>}
 */
export function resolveDataSourceKinds(kinds = []) {
  return kinds
    .map(k => DATA_SOURCE_KINDS[k])
    .filter(Boolean)
}

/**
 * 检查是否为合法数据源类型
 * @param {string} kind
 * @returns {boolean}
 */
export function isDataSourceKind(kind) {
  return Object.prototype.hasOwnProperty.call(DATA_SOURCE_KINDS, kind)
}
