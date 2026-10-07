import { inflateRawSync } from 'node:zlib'
import { assertDistinctPaths, requireCondition, validateRelative } from './paths.mjs'

export const limits = { archive: 32 * 1024 * 1024, file: 16 * 1024 * 1024,
  total: 128 * 1024 * 1024, count: 10000 }

// 不调用 unzip，也不执行包内脚本；完整校验后才把文件交给安装事务。
export function readZip(buffer) {
  requireCondition(buffer.length <= limits.archive, 'ZIP 超过大小限制')
  const end = findEnd(buffer)
  const count = buffer.readUInt16LE(end + 10)
  const size = buffer.readUInt32LE(end + 12)
  const offset = buffer.readUInt32LE(end + 16)
  requireCondition(!buffer.readUInt16LE(end + 4) && !buffer.readUInt16LE(end + 6)
    && count === buffer.readUInt16LE(end + 8), '不支持分卷 ZIP')
  requireCondition(count > 0 && count <= limits.count && offset + size === end, 'ZIP 目录非法或 ZIP64')
  const state = { buffer, cursor: offset, end, offset, files: new Map(), paths: [], ranges: [], total: 0 }
  for (let index = 0; index < count; index++) {
    readEntry(state)
  }
  requireCondition(state.cursor === end, 'ZIP 目录长度不匹配')
  const names = state.paths.map(value => value.toLowerCase().normalize('NFC'))
  requireCondition(new Set(names).size === names.length, 'ZIP 重复或大小写冲突路径')
  assertDistinctPaths([...state.files.keys()])
  for (const name of state.paths) {
    const prefix = `${name.toLowerCase().normalize('NFC')}/`
    requireCondition(!state.files.has(name) || !names.some(value => value.startsWith(prefix)), 'ZIP 文件目录冲突')
  }
  state.ranges.sort((a, b) => a[0] - b[0])
  requireCondition(state.ranges.every((range, index) => !index || range[0] >= state.ranges[index - 1][1]),
    'ZIP 文件区间重叠')
  return state.files
}

function findEnd(buffer) {
  for (let index = buffer.length - 22; index >= Math.max(0, buffer.length - 65557); index--) {
    if (buffer.readUInt32LE(index) === 0x06054b50 && index + 22 + buffer.readUInt16LE(index + 20) === buffer.length) {
      return index
    }
  }
  throw new Error('ZIP 缺少合法结束目录')
}

function readEntry(state) {
  const { buffer, cursor, end } = state
  requireCondition(cursor + 46 <= end && buffer.readUInt32LE(cursor) === 0x02014b50, 'ZIP 条目损坏')
  const flags = buffer.readUInt16LE(cursor + 8)
  const method = buffer.readUInt16LE(cursor + 10)
  const packed = buffer.readUInt32LE(cursor + 20)
  const size = buffer.readUInt32LE(cursor + 24)
  const nameSize = buffer.readUInt16LE(cursor + 28)
  const extraSize = buffer.readUInt16LE(cursor + 30)
  const commentSize = buffer.readUInt16LE(cursor + 32)
  const next = cursor + 46 + nameSize + extraSize + commentSize
  requireCondition(next <= end && !buffer.readUInt16LE(cursor + 34), 'ZIP 条目越界或分卷')
  requireCondition(buffer.readUInt16LE(cursor + 6) < 45, '不支持 ZIP64')
  validateExtra(buffer.subarray(cursor + 46 + nameSize, cursor + 46 + nameSize + extraSize))
  requireCondition(!(flags & ~0x080e) && !(flags & 1) && [0, 8].includes(method), 'ZIP 加密或压缩格式不支持')
  requireCondition(size <= limits.file && state.total + size <= limits.total, 'ZIP 解压大小超过限制')
  const rawName = buffer.subarray(cursor + 46, cursor + 46 + nameSize)
  requireCondition((flags & 0x800) || rawName.every(value => value < 128), 'ZIP 路径必须是 UTF-8/ASCII')
  const name = new TextDecoder('utf-8', { fatal: true }).decode(rawName)
  const directory = name.endsWith('/')
  const relative = validateRelative(directory ? name.slice(0, -1) : name)
  const mode = buffer.readUInt32LE(cursor + 38) >>> 16
  requireCondition(!mode || !((mode & 0xf000) && (mode & 0xf000) !== (directory ? 0x4000 : 0x8000)),
    'ZIP 包含链接或特殊文件')
  const data = readLocal(state, { flags, method, packed, rawName, local: buffer.readUInt32LE(cursor + 42) })
  const unpacked = method === 0 ? data : inflateRawSync(data, { maxOutputLength: Math.max(1, size) })
  requireCondition(unpacked.length === size && crc32(unpacked) === buffer.readUInt32LE(cursor + 16),
    'ZIP 文件长度或 CRC 校验失败')
  requireCondition(!directory || size === 0, 'ZIP 目录不能包含数据')
  state.paths.push(relative)
  if (!directory) {
    state.files.set(relative, unpacked)
  }
  state.total += size
  state.cursor = next
}

function readLocal(state, entry) {
  const { buffer, offset } = state
  const { local, flags, method, packed, rawName } = entry
  requireCondition(local + 30 <= offset && buffer.readUInt32LE(local) === 0x04034b50, 'ZIP 本地头损坏')
  const nameSize = buffer.readUInt16LE(local + 26)
  const begin = local + 30 + nameSize + buffer.readUInt16LE(local + 28)
  requireCondition(begin + packed <= offset && buffer.readUInt16LE(local + 6) === flags
    && buffer.readUInt16LE(local + 8) === method, 'ZIP 本地头不匹配或越界')
  requireCondition(buffer.subarray(local + 30, local + 30 + nameSize).equals(rawName), 'ZIP 文件名不匹配')
  validateExtra(buffer.subarray(local + 30 + nameSize, begin))
  state.ranges.push([local, begin + packed])
  return buffer.subarray(begin, begin + packed)
}

function validateExtra(buffer) {
  let offset = 0
  while (offset < buffer.length) {
    requireCondition(offset + 4 <= buffer.length, 'ZIP 扩展字段损坏')
    requireCondition(buffer.readUInt16LE(offset) !== 1, '不支持 ZIP64')
    offset += 4 + buffer.readUInt16LE(offset + 2)
    requireCondition(offset <= buffer.length, 'ZIP 扩展字段越界')
  }
}

export function crc32(buffer) {
  let crc = 0xffffffff
  for (const byte of buffer) {
    crc ^= byte
    for (let bit = 0; bit < 8; bit++) {
      crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0)
    }
  }
  return (crc ^ 0xffffffff) >>> 0
}
