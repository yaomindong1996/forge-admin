/**
 * 字典数据管理 Composable
 * 用于加载和缓存字典数据
 * 
 * 使用示例：
 * import { useDict } from '@/composables/useDict'
 * 
 * // 在 setup 中使用
 * const { dict } = useDict('case_status', 'matter_type')
 * 
 * // 访问字典数据
 * dict.case_status // [{ label: '待处理', value: '1', ... }, ...]
 */

import { ref, onMounted } from 'vue'
import { request } from '@/utils'

// 全局字典缓存
const dictCache = new Map()

/**
 * 加载字典数据
 * @param {String} dictType - 字典类型
 * @returns {Promise<Array>} 字典数据列表
 */
async function loadDictData(dictType) {
  try {
    const res = await request.get('/system/dict/data/list', {
      params: { dictType }
    })
    
    if (res.code === 200) {
      // 转换为标准格式：{ label, value, ... }
      const dictList = (res.data || []).map(item => ({
        label: item.dictLabel,
        value: item.dictValue,
        dictCode: item.dictCode,
        dictSort: item.dictSort,
        cssClass: item.cssClass,
        listClass: item.listClass || 'default', // 默认为 default
        isDefault: item.isDefault,
        status: item.dictStatus !== undefined ? item.dictStatus : item.status, // 兼容不同字段名
        remark: item.remark,
        raw: item // 保留原始数据
      }))
      
      // 按排序字段排序
      dictList.sort((a, b) => (a.dictSort || 0) - (b.dictSort || 0))
      
      return dictList
    }
    
    return []
  } catch (error) {
    console.error(`加载字典 ${dictType} 失败:`, error)
    return []
  }
}

/**
 * 获取字典数据（带缓存）
 * @param {String} dictType - 字典类型
 * @param {Boolean} forceReload - 是否强制重新加载
 * @returns {Promise<Array>} 字典数据列表
 */
async function getDictData(dictType, forceReload = false) {
  if (!forceReload && dictCache.has(dictType)) {
    return dictCache.get(dictType)
  }
  
  const data = await loadDictData(dictType)
  dictCache.set(dictType, data)
  return data
}

/**
 * 清除字典缓存
 * @param {String} dictType - 字典类型，不传则清除所有
 */
function clearDictCache(dictType) {
  if (dictType) {
    dictCache.delete(dictType)
  } else {
    dictCache.clear()
  }
}

/**
 * 字典 Composable
 * @param  {...String} dictTypes - 字典类型列表
 * @returns {Object} { dict, loading, reload }
 */
export function useDict(...dictTypes) {
  const dict = ref({})
  const loading = ref(false)
  
  /**
   * 加载所有字典
   */
  async function loadAllDicts() {
    if (dictTypes.length === 0) return
    
    loading.value = true
    
    try {
      const promises = dictTypes.map(type => getDictData(type))
      const results = await Promise.all(promises)
      
      dictTypes.forEach((type, index) => {
        dict.value[type] = results[index]
      })
    } catch (error) {
      console.error('加载字典失败:', error)
    } finally {
      loading.value = false
    }
  }
  
  /**
   * 重新加载字典
   * @param  {...String} types - 要重新加载的字典类型，不传则重新加载所有
   */
  async function reload(...types) {
    const typesToReload = types.length > 0 ? types : dictTypes
    
    loading.value = true
    
    try {
      const promises = typesToReload.map(type => getDictData(type, true))
      const results = await Promise.all(promises)
      
      typesToReload.forEach((type, index) => {
        dict.value[type] = results[index]
      })
    } catch (error) {
      console.error('重新加载字典失败:', error)
    } finally {
      loading.value = false
    }
  }
  
  /**
   * 根据字典值获取标签
   * @param {String} dictType - 字典类型
   * @param {String|Number} value - 字典值
   * @returns {String} 字典标签
   */
  function getLabel(dictType, value) {
    const dictList = dict.value[dictType] || []
    const item = dictList.find(d => String(d.value) === String(value))
    return item ? item.label : value
  }
  
  /**
   * 根据字典值获取字典项
   * @param {String} dictType - 字典类型
   * @param {String|Number} value - 字典值
   * @returns {Object|null} 字典项
   */
  function getDict(dictType, value) {
    const dictList = dict.value[dictType] || []
    return dictList.find(d => String(d.value) === String(value)) || null
  }
  
  // 组件挂载时加载字典
  onMounted(() => {
    loadAllDicts()
  })
  
  return {
    dict,
    loading,
    reload,
    getLabel,
    getDict
  }
}

/**
 * 导出工具函数
 */
export {
  getDictData,
  clearDictCache
}
