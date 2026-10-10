// 原生 JSON.parse 会吞掉重复键；先扫描结构和键，再交给它校验完整 JSON 语法。
export function parseStrictJson(text, maxBytes = 64 * 1024) {
  if (new TextEncoder().encode(text).byteLength > maxBytes) {
    throw new Error('JSON 文件超过大小限制')
  }
  const value = JSON.parse(text)
  const state = { text, index: 0 }
  scanValue(state, 0)
  return value
}

function skipSpace(state) {
  while (/\s/.test(state.text[state.index] || '') && state.index < state.text.length) {
    state.index++
  }
}

function scanString(state) {
  const pattern = /"(?:[^"\\]|\\.)*"/y
  pattern.lastIndex = state.index
  const match = pattern.exec(state.text)
  state.index = pattern.lastIndex
  return JSON.parse(match[0])
}

function scanValue(state, depth) {
  if (depth > 32) {
    throw new Error('JSON 嵌套超过限制')
  }
  skipSpace(state)
  const character = state.text[state.index]
  if (character === '{') {
    scanObject(state, depth)
  }
  else if (character === '[') {
    scanArray(state, depth)
  }
  else if (character === '"') {
    scanString(state)
  }
  else {
    const token = state.text.slice(state.index).match(/^(?:true|false|null|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)/)
    state.index += token[0].length
  }
}

function scanObject(state, depth) {
  state.index++
  const keys = new Set()
  skipSpace(state)
  while (state.text[state.index] !== '}') {
    const key = scanString(state)
    if (keys.has(key) || ['__proto__', 'prototype', 'constructor'].includes(key)) {
      throw new Error(`JSON 重复或保留键：${key}`)
    }
    keys.add(key)
    skipSpace(state)
    state.index++
    scanValue(state, depth + 1)
    skipSpace(state)
    if (state.text[state.index] === ',') {
      state.index++
      skipSpace(state)
    }
  }
  state.index++
}

function scanArray(state, depth) {
  state.index++
  skipSpace(state)
  while (state.text[state.index] !== ']') {
    scanValue(state, depth + 1)
    skipSpace(state)
    if (state.text[state.index] === ',') {
      state.index++
    }
  }
  state.index++
}
