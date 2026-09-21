// Bounded document history; view state and preview data never enter snapshots.
export function cloneDocument(value) {
  return JSON.parse(JSON.stringify(value))
}

export function createHistory() {
  return { past: [], future: [] }
}

export function recordChange(history, before, after) {
  if (JSON.stringify(before) === JSON.stringify(after)) {
    return false
  }
  history.past.push(cloneDocument(before))
  if (history.past.length > 50) {
    history.past.shift()
  }
  history.future = []
  return true
}

export function travelHistory(history, current, direction) {
  const source = direction === 'undo' ? history.past : history.future
  const target = direction === 'undo' ? history.future : history.past
  if (!source.length) {
    return current
  }
  target.push(cloneDocument(current))
  return source.pop()
}
