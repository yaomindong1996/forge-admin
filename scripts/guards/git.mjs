import fs from 'node:fs/promises'
import { spawnSync } from 'node:child_process'
import { maxFileBytes } from './content.mjs'

const maxGitBytes = 128 * 1024 * 1024

function git(root, args, input) {
  // 不允许调用者通过 GIT_INDEX_FILE/GIT_WORK_TREE 等重定向到另一份干净索引。
  const env = Object.fromEntries(Object.entries(process.env).filter(([key]) => !key.startsWith('GIT_')))
  const result = spawnSync('git', ['--no-replace-objects', '-C', root, ...args], {
    input, env: { ...env, GIT_OPTIONAL_LOCKS: '0', GIT_NO_LAZY_FETCH: '1' },
    maxBuffer: maxGitBytes, timeout: 30000,
  })
  if (result.status !== 0 || result.error) {
    throw new Error('Git 不可用、仓库/对象读取失败或超过检查限制')
  }
  return result.stdout
}

function decode(bytes) {
  return new TextDecoder('utf-8', { fatal: true }).decode(bytes)
}

export async function readInventory(directory) {
  const root = await fs.realpath(directory)
  const top = decode(git(root, ['rev-parse', '--show-toplevel'])).trimEnd()
  if (await fs.realpath(top) !== root) {
    throw new Error('检查目录必须是 Git 仓库顶层')
  }
  const raw = git(root, ['ls-files', '--stage', '-z'])
  const indexed = decode(raw).split('\0').filter(Boolean).map(parseEntry)
  const others = decode(git(root, ['ls-files', '--others', '--exclude-standard', '-z']))
    .split('\0').filter(Boolean)
  return { root, indexed, others, raw }
}

function parseEntry(line) {
  const match = line.match(/^(\d{6}) ([\da-f]{40}|[\da-f]{64}) ([0-3])\t([\s\S]+)$/)
  if (!match) {
    throw new Error('Git 索引格式不可检查')
  }
  const [, mode, oid, stage, relative] = match
  if (stage !== '0' || !['100644', '100755'].includes(mode)) {
    throw new Error(`索引有冲突、软链接或子模块：${JSON.stringify(relative)}`)
  }
  return { mode, oid, relative }
}

function objectSizes(root, records) {
  const ids = [...new Set(records.map(record => record.oid))]
  if (!ids.length) {
    return []
  }
  const text = decode(git(root, ['cat-file', '--batch-check'], ids.join('\n') + '\n'))
  const lines = text.trimEnd().split('\n')
  if (lines.length !== ids.length) {
    throw new Error('Git blob 数量不一致')
  }
  return lines.map((line, index) => {
    const match = line.match(/^([\da-f]+) blob (\d+)$/)
    if (!match || match[1] !== ids[index] || Number(match[2]) > maxFileBytes) {
      throw new Error('Git 索引 blob 无法读取或超过 64 MiB；请检查索引文件')
    }
    return { oid: ids[index], size: Number(match[2]) }
  })
}

// 每批上限 32 MiB，最大单文件 64 MiB；不把整个仓库 blob 一次性载入内存。
export function* readIndexContents(root, records) {
  let batch = []
  let size = 0
  for (const object of objectSizes(root, records)) {
    if (batch.length && (size + object.size > 32 * 1024 * 1024 || batch.length >= 256)) {
      yield* readBatch(root, batch)
      batch = []
      size = 0
    }
    batch.push(object)
    size += object.size
  }
  if (batch.length) {
    yield* readBatch(root, batch)
  }
}

function* readBatch(root, objects) {
  const bytes = git(root, ['cat-file', '--batch'], objects.map(object => object.oid).join('\n') + '\n')
  let offset = 0
  for (const object of objects) {
    const end = bytes.indexOf(10, offset)
    const header = bytes.subarray(offset, end).toString('ascii')
    const start = end + 1
    offset = start + object.size + 1
    if (end < 0 || header !== `${object.oid} blob ${object.size}` || bytes[offset - 1] !== 10) {
      throw new Error('Git blob 数据不完整')
    }
    yield { oid: object.oid, bytes: bytes.subarray(start, offset - 1) }
  }
  if (offset !== bytes.length) {
    throw new Error('Git blob 多余数据')
  }
}

export function assertIndexUnchanged(inventory) {
  if (!git(inventory.root, ['ls-files', '--stage', '-z']).equals(inventory.raw)) {
    throw new Error('检查期间 Git 索引发生变化，请重试')
  }
}
