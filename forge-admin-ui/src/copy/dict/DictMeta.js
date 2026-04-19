import { mergeRecursive } from '@/utils/ruoyi'
import DictOptions from './DictOptions'

/**
 * @classdesc 字典元数据
 * @property {string} type 类型
 * @property {Function} request 请求
 * @property {string} label 标签字段
 * @property {string} value 值字段
 */
export default class DictMeta {
  constructor(options) {
    this.type = options.type
    this.request = options.request
    this.responseConverter = options.responseConverter
    this.labelField = options.labelField
    this.valueField = options.valueField
    this.lazy = options.lazy === true
  }
}

/**
 * 解析字典元数据
 * @param {object} options
 * @returns {DictMeta}
 */
DictMeta.parse = function (options) {
  let opts = null
  if (typeof options === 'string') {
    opts = DictOptions.metas[options] || {}
    opts.type = options
  }
  else if (typeof options === 'object') {
    opts = options
  }
  opts = mergeRecursive(DictOptions.metas['*'], opts)
  return new DictMeta(opts)
}
