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

export function normalizeDictOptionValue(options, value, fallback = null) {
  if (value === null || value === undefined || value === '')
    return fallback

  const normalizedOptions = normalizeOptions(options)
  if (normalizedOptions.length === 0)
    return value

  return normalizedOptions.some(item => String(item.value) === String(value)) ? value : fallback
}
