import fs from 'node:fs/promises'
import path from 'node:path'
import { renameSourceTree } from '../forge-shared/rename.mjs'
import { safeTarget } from './paths.mjs'

export function decodeUtf8(buffer) {
  return new TextDecoder('utf-8', { fatal: true }).decode(buffer)
}

function binary(data) {
  if (data.includes(0)) {
    return true
  }
  try {
    decodeUtf8(data)
    return false
  }
  catch {
    return true
  }
}

export async function renameComponent(directory, files, context) {
  const binaries = [...files].filter(([, data]) => binary(data))
  // 共用规则面向源码文本；未知扩展名的二进制不能被 UTF-8 解码后写坏。
  for (const [relative] of binaries) {
    await fs.unlink(path.join(directory, relative))
  }
  await renameSourceTree(directory, context)
  for (const [relative, data] of binaries) {
    const renamed = renamedBinaryPath(relative, context)
    const target = await safeTarget(directory, renamed)
    await fs.mkdir(path.dirname(target), { recursive: true })
    await fs.writeFile(target, data, { flag: 'wx' })
  }
}

function renamedBinaryPath(relative, context) {
  const parts = relative.split('/')
  const leaf = parts.pop()
  const directoryMap = context.directoryMap ?? context.artifactMap
  const directory = parts.map(part => directoryMap[part] || part).join('/')
  const packagePath = context.options.basePackage.replaceAll('.', '/')
  const renamed = directory.replace(/^src\/(main|test)\/java\/com\/mdframe\/forge(?=\/|$)/,
    (_, sourceSet) => `src/${sourceSet}/java/${packagePath}`)
  return renamed ? `${renamed}/${leaf}` : leaf
}
