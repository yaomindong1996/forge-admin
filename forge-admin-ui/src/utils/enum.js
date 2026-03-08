/**
 * 枚举管理工具
 * 提供枚举数据的获取、缓存和管理功能
 */

import { reactive } from 'vue'
import { request } from './index'

/**
 * 枚举缓存对象
 * 格式: { enumType: [{ label, value, ...otherFields }] }
 */
const enumCache = reactive({})

/**
 * 获取枚举数据
 * @param {string} type - 枚举类型 (如 'USER_CERT', 'USER_EDU' 等)
 * @param {string} version - API 版本 ('v1' 或 'v2'), 默认 'v1'
 * @param {boolean} forceRefresh - 是否强制刷新,忽略缓存, 默认 false
 * @returns {Promise<Array>} 枚举数据数组
 * 
 * @example
 * // 基础用法
 * const userCerts = await getEnum('USER_CERT')
 * // 结果: [{ label: '身份证', value: '1', ... }]
 * 
 * @example
 * // 强制刷新
 * const userCerts = await getEnum('USER_CERT', 'v1', true)
 * 
 * @example
 * // 使用 v2 接口
 * const data = await getEnum('SOME_TYPE', 'v2')
 */
export async function getEnum(type, version = 'v1', forceRefresh = false) {
  if (!type) {
    console.warn('getEnum: 枚举类型不能为空')
    return []
  }

  // 先从缓存中获取 (除非强制刷新)
  if (!forceRefresh && enumCache[type] && enumCache[type].length) {
    return enumCache[type]
  }

  try {
    // 根据版本选择不同的接口
    const url = version === 'v1' ? `/dictionary/${type}` : '/elementData/dictionary'
    const params = version === 'v1' ? {} : { typeId: type }
    
    const res = await request.get(url, { params })
    
    if (res && res.length) {
      let cacheData
      
      if (version === 'v1') {
        // v1 接口数据格式转换
        cacheData = res.map(item => ({
          label: item.dataName || '',
          value: item.dataId || '',
          ...item
        }))
      } else {
        // v2 接口直接使用
        cacheData = res
      }
      
      // 缓存数据
      enumCache[type] = cacheData
      return cacheData
    }
    
    // 即使接口返回空,也缓存空数组,避免重复请求
    enumCache[type] = []
    return []
  } catch (error) {
    console.error(`获取枚举 ${type} 失败:`, error)
    // 发生错误时不缓存,下次可以重试
    return []
  }
}

/**
 * 批量获取多个枚举
 * @param {Array<string|Object>} types - 枚举类型数组
 * @returns {Promise<Object>} 枚举数据对象 { type1: [...], type2: [...] }
 * 
 * @example
 * const enums = await batchGetEnum(['USER_CERT', 'USER_EDU', 'USER_PRO'])
 * console.log(enums.USER_CERT) // [{ label: '身份证', value: '1' }]
 * 
 * @example
 * // 支持不同版本
 * const enums = await batchGetEnum([
 *   { type: 'USER_CERT', version: 'v1' },
 *   { type: 'SOME_TYPE', version: 'v2' }
 * ])
 */
export async function batchGetEnum(types) {
  if (!types || !Array.isArray(types) || types.length === 0) {
    console.warn('batchGetEnum: 枚举类型数组不能为空')
    return {}
  }

  const promises = types.map(item => {
    if (typeof item === 'string') {
      return getEnum(item)
    } else if (item && item.type) {
      return getEnum(item.type, item.version || 'v1', item.forceRefresh)
    }
    return Promise.resolve([])
  })

  const results = await Promise.all(promises)
  
  const enumData = {}
  types.forEach((item, index) => {
    const type = typeof item === 'string' ? item : item.type
    enumData[type] = results[index]
  })
  
  return enumData
}

/**
 * 清除指定枚举缓存
 * @param {string|Array<string>} types - 枚举类型或类型数组
 * 
 * @example
 * clearEnumCache('USER_CERT') // 清除单个
 * clearEnumCache(['USER_CERT', 'USER_EDU']) // 清除多个
 */
export function clearEnumCache(types) {
  if (!types) return
  
  const typeArray = Array.isArray(types) ? types : [types]
  typeArray.forEach(type => {
    if (enumCache[type]) {
      delete enumCache[type]
    }
  })
}

/**
 * 清除所有枚举缓存
 */
export function clearAllEnumCache() {
  Object.keys(enumCache).forEach(key => {
    delete enumCache[key]
  })
}

/**
 * 获取枚举缓存对象 (只读)
 * @returns {Object} 枚举缓存对象
 */
export function getEnumCache() {
  return { ...enumCache }
}

/**
 * 根据 value 查找 label
 * @param {string} type - 枚举类型
 * @param {string|number} value - 枚举值
 * @returns {string} 枚举标签
 * 
 * @example
 * const label = getEnumLabel('USER_CERT', '1')
 * console.log(label) // '身份证'
 */
export function getEnumLabel(type, value) {
  if (!type || value === undefined || value === null) return ''
  
  const enumList = enumCache[type]
  if (!enumList || !Array.isArray(enumList)) return ''
  
  const item = enumList.find(e => String(e.value) === String(value))
  return item ? item.label : ''
}

/**
 * 根据 label 查找 value
 * @param {string} type - 枚举类型
 * @param {string} label - 枚举标签
 * @returns {string|number} 枚举值
 * 
 * @example
 * const value = getEnumValue('USER_CERT', '身份证')
 * console.log(value) // '1'
 */
export function getEnumValue(type, label) {
  if (!type || !label) return ''
  
  const enumList = enumCache[type]
  if (!enumList || !Array.isArray(enumList)) return ''
  
  const item = enumList.find(e => e.label === label)
  return item ? item.value : ''
}

/**
 * 检查枚举是否已缓存
 * @param {string} type - 枚举类型
 * @returns {boolean} 是否已缓存
 */
export function hasEnumCache(type) {
  return !!(enumCache[type] && enumCache[type].length)
}

// 导出枚举缓存对象 (供直接访问)
export { enumCache }

// 默认导出
export default {
  getEnum,
  batchGetEnum,
  clearEnumCache,
  clearAllEnumCache,
  getEnumCache,
  getEnumLabel,
  getEnumValue,
  hasEnumCache,
  enumCache
}
