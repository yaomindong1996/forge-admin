/**
 * AI Form Schema 辅助工具
 * 用于动态更新表单配置中的选项数据
 */

import { toRaw } from 'vue'

/**
 * 更新 Schema 中指定字段的选项数据
 *
 * @param {Array} schema - 响应式的表单 schema 数组
 * @param {string} fieldName - 要更新的字段名
 * @param {Array} options - 新的选项数据
 * @returns {boolean} - 是否更新成功
 *
 * @example
 * const schema = reactive([...])
 * updateFieldOptions(schema, 'cityId', [{ label: '北京', value: '1' }])
 */
export function updateFieldOptions(schema, fieldName, options) {
  const fieldIndex = schema.findIndex(field => field.field === fieldName)

  if (fieldIndex === -1) {
    console.warn(`Schema 中未找到字段: ${fieldName}`)
    return false
  }

  // 使用 splice 替换整个字段对象以触发响应式更新
  const field = toRaw(schema[fieldIndex])
  schema.splice(fieldIndex, 1, {
    ...field,
    options,
  })

  return true
}

/**
 * 批量更新多个字段的选项数据
 *
 * @param {Array} schema - 响应式的表单 schema 数组
 * @param {object} optionsMap - 字段名到选项数据的映射
 * @returns {object} - 更新结果统计
 *
 * @example
 * batchUpdateFieldOptions(schema, {
 *   cityId: [{ label: '北京', value: '1' }],
 *   districtId: [{ label: '朝阳区', value: '101' }]
 * })
 */
export function batchUpdateFieldOptions(schema, optionsMap) {
  const result = {
    success: 0,
    failed: 0,
    failedFields: [],
  }

  Object.entries(optionsMap).forEach(([fieldName, options]) => {
    const success = updateFieldOptions(schema, fieldName, options)
    if (success) {
      result.success++
    }
    else {
      result.failed++
      result.failedFields.push(fieldName)
    }
  })

  return result
}

/**
 * 更新字段的其他配置项
 *
 * @param {Array} schema - 响应式的表单 schema 数组
 * @param {string} fieldName - 要更新的字段名
 * @param {object} updates - 要更新的配置项
 * @returns {boolean} - 是否更新成功
 *
 * @example
 * updateFieldConfig(schema, 'email', { disabled: true, placeholder: '邮箱已锁定' })
 */
export function updateFieldConfig(schema, fieldName, updates) {
  const fieldIndex = schema.findIndex(field => field.field === fieldName)

  if (fieldIndex === -1) {
    console.warn(`Schema 中未找到字段: ${fieldName}`)
    return false
  }

  const field = toRaw(schema[fieldIndex])
  schema.splice(fieldIndex, 1, {
    ...field,
    ...updates,
  })

  return true
}

/**
 * 获取字段配置
 *
 * @param {Array} schema - 表单 schema 数组
 * @param {string} fieldName - 字段名
 * @returns {object | null} - 字段配置对象
 */
export function getFieldConfig(schema, fieldName) {
  return schema.find(field => field.field === fieldName) || null
}

/**
 * 设置字段为加载中状态（用于异步加载选项时）
 *
 * @param {Array} schema - 响应式的表单 schema 数组
 * @param {string} fieldName - 字段名
 * @param {boolean} loading - 是否加载中
 * @returns {boolean} - 是否设置成功
 */
export function setFieldLoading(schema, fieldName, loading = true) {
  return updateFieldConfig(schema, fieldName, { loading })
}

/**
 * 创建异步选项更新函数
 * 返回一个函数，该函数会在调用时显示加载状态，获取数据后更新选项
 *
 * @param {Array} schema - 响应式的表单 schema 数组
 * @param {string} fieldName - 字段名
 * @param {Function} fetchFn - 异步获取选项数据的函数
 * @returns {Function} - 异步更新函数
 *
 * @example
 * const updateCityOptions = createAsyncOptionsUpdater(
 *   schema,
 *   'cityId',
 *   async () => {
 *     const res = await api.getCities()
 *     return res.data.map(item => ({ label: item.name, value: item.id }))
 *   }
 * )
 * await updateCityOptions() // 调用更新
 */
export function createAsyncOptionsUpdater(schema, fieldName, fetchFn) {
  return async () => {
    try {
      setFieldLoading(schema, fieldName, true)
      const options = await fetchFn()
      updateFieldOptions(schema, fieldName, options)
      return options
    }
    catch (error) {
      console.error(`加载字段 ${fieldName} 的选项失败:`, error)
      throw error
    }
    finally {
      setFieldLoading(schema, fieldName, false)
    }
  }
}

/**
 * 级联更新：根据父字段的值更新子字段的选项
 *
 * @param {Array} schema - 响应式的表单 schema 数组
 * @param {object} formData - 表单数据对象
 * @param {string} parentField - 父字段名
 * @param {string} childField - 子字段名
 * @param {Function} fetchFn - 根据父字段值获取子选项的异步函数
 * @returns {Function} - 级联更新函数
 *
 * @example
 * const updateDistrictOptions = createCascadeUpdater(
 *   schema,
 *   formData,
 *   'cityId',
 *   'districtId',
 *   async (cityId) => {
 *     const res = await api.getDistricts(cityId)
 *     return res.data.map(item => ({ label: item.name, value: item.id }))
 *   }
 * )
 *
 * // 在 onChange 回调中使用
 * FieldFactory.select('cityId', '城市', [], {
 *   onChange: ({ value }) => updateDistrictOptions(value)
 * })
 */
export function createCascadeUpdater(schema, formData, parentField, childField, fetchFn) {
  return async (parentValue) => {
    try {
      // 清空子字段的值
      if (formData[childField]) {
        formData[childField] = null
      }

      // 如果父字段没有值，清空子字段选项
      if (!parentValue) {
        updateFieldOptions(schema, childField, [])
        return
      }

      // 设置加载状态
      setFieldLoading(schema, childField, true)

      // 获取并更新子字段选项
      const options = await fetchFn(parentValue)
      updateFieldOptions(schema, childField, options)

      return options
    }
    catch (error) {
      console.error(`级联加载 ${childField} 选项失败:`, error)
      throw error
    }
    finally {
      setFieldLoading(schema, childField, false)
    }
  }
}

export default {
  updateFieldOptions,
  batchUpdateFieldOptions,
  updateFieldConfig,
  getFieldConfig,
  setFieldLoading,
  createAsyncOptionsUpdater,
  createCascadeUpdater,
}
