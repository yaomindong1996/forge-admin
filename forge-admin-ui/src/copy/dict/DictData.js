/**
 * @classdesc 字典数据
 * @property {string} label 标签
 * @property {*} value 标签
 * @property {object} raw 原始数据
 */
export default class DictData {
  constructor(label, value, raw) {
    this.label = label
    this.value = value
    this.raw = raw
  }
}
