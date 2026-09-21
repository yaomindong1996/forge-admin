import { evaluateExpression } from './expression'
import { PRINT_LIMITS, PrintError } from './types'
import { isSafeFieldPath } from './validate'

export function readOwnPath(value, path) {
  if (!isSafeFieldPath(path)) {
    throw new PrintError('INVALID_FIELD_PATH', '字段路径无效', path)
  }
  return path.split('.').reduce((current, key) => {
    if (!current || typeof current !== 'object' || !Object.hasOwn(current, key)) {
      return null
    }
    const descriptor = Object.getOwnPropertyDescriptor(current, key)
    if (descriptor.get || descriptor.set) {
      throw new PrintError('INVALID_CONTEXT', '打印数据必须是普通 JSON', path)
    }
    return descriptor.value ?? null
  }, value)
}

export function resolveBinding(binding, context) {
  if (binding.source === 'EXPRESSION') {
    const value = evaluateExpression(binding.expression, context)
    if (value !== null && value !== undefined && !['string', 'number', 'boolean'].includes(typeof value)) {
      throw new PrintError('EXPECTED_SCALAR', '表达式只能产生单值', binding.expression)
    }
    return value ?? null
  }
  const value = binding.source === 'CONSTANT' ? binding.value : readOwnPath(context, binding.path)
  if (value !== null && value !== undefined && !['string', 'number', 'boolean'].includes(typeof value)) {
    throw new PrintError('EXPECTED_SCALAR', '此位置只能绑定单值字段', binding.path)
  }
  return value ?? null
}

export function resolveCollection(path, context) {
  const value = readOwnPath(context, path)
  if (value === null) {
    return []
  }
  if (!Array.isArray(value) || value.some(row => !row || typeof row !== 'object' || Array.isArray(row))) {
    throw new PrintError('EXPECTED_COLLECTION', '明细须是记录数组', path)
  }
  if (value.length > PRINT_LIMITS.rows) {
    throw new PrintError('ROW_LIMIT', `明细最多 ${PRINT_LIMITS.rows} 行`, path)
  }
  return value
}
