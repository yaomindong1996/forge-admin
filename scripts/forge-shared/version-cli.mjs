import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { frontendVersionChanges, readReleaseProject } from './release-info.mjs'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const mode = process.argv[2] || '--check'
try {
  if (!['--check', '--sync'].includes(mode)) throw new Error('用法：version-cli.mjs --check 或 --sync')
  const changes = await frontendVersionChanges(root)
  if (mode === '--sync') {
    for (const { file, manifest, expected } of changes) {
      manifest.version = expected
      await fs.writeFile(file, `${JSON.stringify(manifest, null, 2)}\n`)
      console.info(`同步 ${path.relative(root, file)} → ${expected}`)
    }
  }
  else if (changes.length) {
    const files = changes.map(item => path.relative(root, item.file)).join('、')
    throw new Error(`前端版本与根 POM 不一致：${files}；请运行 version-cli.mjs --sync`)
  }
  const version = (await readReleaseProject(root)).version
  const action = mode === '--sync' ? '同步' : '检查'
  console.info(`发行版本 ${version}，版本${action}完成`)
}
catch (error) {
  console.error(error.message)
  process.exitCode = 1
}
