import { parseSelectionValues, serializeSelectionValues } from '../selection-multi-value'

export function mergeQuickCreateSelection(currentValue, createdId, multiple = false) {
  if (createdId === null || createdId === undefined || createdId === '')
    return currentValue
  if (!multiple)
    return createdId
  const existing = parseSelectionValues(currentValue, true)
  const exists = existing.some(item => String(item) === String(createdId))
  if (exists)
    return serializeSelectionValues(existing, true)
  return serializeSelectionValues([...existing, createdId], true)
}
