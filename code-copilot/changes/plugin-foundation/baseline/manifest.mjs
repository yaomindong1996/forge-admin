import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import fs from 'node:fs/promises'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

// 不忽略任何输出文件：包括点文件、二进制图片和空文件，不归一化内容或时间戳。
export async function collectManifest(root) {
  const files = []
  async function visit(directory) {
    for (const entry of await fs.readdir(directory, { withFileTypes: true })) {
      const filename = path.join(directory, entry.name)
      if (entry.isDirectory()) {
        await visit(filename)
        continue
      }
      if (!entry.isFile()) {
        throw new Error(`基线不接受软链接或特殊文件：${filename}`)
      }
      const bytes = await fs.readFile(filename)
      files.push({
        path: path.relative(root, filename).split(path.sep).join('/'),
        sha256: createHash('sha256').update(bytes).digest('hex'),
        bytes: bytes.length,
      })
    }
  }
  await visit(path.resolve(root))
  files.sort((left, right) => left.path < right.path ? -1 : left.path > right.path ? 1 : 0)
  return { schemaVersion: 1, fileCount: files.length, files }
}

export function compareManifests(expected, actual) {
  const oldFiles = new Map(expected.files.map(file => [file.path, file]))
  const newFiles = new Map(actual.files.map(file => [file.path, file]))
  const missing = expected.files.filter(file => !newFiles.has(file.path)).map(file => file.path)
  const added = actual.files.filter(file => !oldFiles.has(file.path)).map(file => file.path)
  const changed = actual.files.filter(file => {
    const old = oldFiles.get(file.path)
    return old && (old.sha256 !== file.sha256 || old.bytes !== file.bytes)
  }).map(file => file.path)
  return { missing, added, changed }
}

async function main() {
  const [command, directory, manifestFile] = process.argv.slice(2)
  if (!['record', 'verify'].includes(command) || !directory || !manifestFile) {
    throw new Error('用法：node manifest.mjs <record|verify> <生成工程目录> <清单.json>')
  }
  const actual = await collectManifest(directory)
  if (command === 'record') {
    // wx 防止后续阶段覆盖 T0 证据。更新基线必须另行确认，不能为了消除差异重录。
    await fs.writeFile(manifestFile, `${JSON.stringify(actual, null, 2)}\n`, { flag: 'wx' })
    console.log(`已记录 ${actual.fileCount} 个文件：${manifestFile}`)
    return
  }
  const expected = JSON.parse(await fs.readFile(manifestFile, 'utf8'))
  const differences = compareManifests(expected, actual)
  console.log(JSON.stringify({ fileCount: actual.fileCount, ...differences }, null, 2))
  assert.deepEqual(differences, { missing: [], added: [], changed: [] }, '生成结果与 T0 基线不一致')
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  main().catch(error => {
    console.error(error.message)
    process.exitCode = 1
  })
}
