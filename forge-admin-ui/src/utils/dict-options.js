function normalizeOptions(options) {
  return Array.isArray(options) ? options : []
}

export function toNumberDictOptions(options) {
  return normalizeOptions(options).map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

export function toBooleanDictOptions(options) {
  return normalizeOptions(options).map(item => ({
    ...item,
    value: item.value === true || item.value === 1 || item.value === '1' || item.value === 'true',
  }))
}

export function mapDictOptionValues(options, valueMap = {}) {
  return normalizeOptions(options).map(item => ({
    ...item,
    value: Object.prototype.hasOwnProperty.call(valueMap, item.value)
      ? valueMap[item.value]
      : item.value,
  }))
}

export function stringifyDictValue(value) {
  if (value === null || value === undefined)
    return value
  return String(value)
}

export function toDictSelectValue(value, multiple = false) {
  if (value === null || value === undefined || value === '')
    return multiple ? [] : null
  if (multiple)
    return (Array.isArray(value) ? value : String(value).split(',')).map(item => stringifyDictValue(item))
  return stringifyDictValue(value)
}

export function fromDictSelectValue(val, boundValue, multiple = false) {
  if (val === null || val === undefined || val === '')
    return val
  if (multiple)
    return (Array.isArray(val) ? val : []).map(item => coerceBoundDictItem(item, boundSampleType(boundValue)))
  return coerceBoundDictItem(val, typeof boundValue)
}

function boundSampleType(boundValue) {
  if (Array.isArray(boundValue) && boundValue.length)
    return typeof boundValue[0]
  return typeof boundValue
}

function coerceBoundDictItem(val, type) {
  if (type !== 'number')
    return val
  const number = Number(val)
  return Number.isNaN(number) ? val : number
}

export function normalizeDictOptionValue(options, value, fallback = null) {
  if (value === null || value === undefined || value === '')
    return fallback

  const normalizedOptions = normalizeOptions(options)
  if (normalizedOptions.length === 0)
    return value

  return normalizedOptions.some(item => String(item.value) === String(value)) ? value : fallback
}
