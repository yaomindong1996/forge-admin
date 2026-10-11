import fs from 'node:fs/promises'
import path from 'node:path'
import os from 'node:os'
import { createHash } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { git, resolveCommit, treeEntries } from './git.mjs'
import { generatorFiles, validateOptions, cleanSnapshot } from './policy.mjs'
import { readPomVersion } from '../forge-shared/version.mjs'
import { frontendVersionChanges } from '../forge-shared/release-info.mjs'

export async function generatorDigest(root) {
  const hash = createHash('sha256')
  for (const file of generatorFiles) {
    hash.update(file).update('\0').update(await fs.readFile(path.join(root, file))).update('\0')
  }
  return hash.digest('hex')
}

export async function copyGenerator(source, target) {
  for (const file of generatorFiles) {
    const sourceFile = path.join(source, file)
    if (!(await fs.lstat(sourceFile)).isFile()) throw new Error(`映射工具不是普通文件：${file}`)
    await fs.mkdir(path.dirname(path.join(target, file)), { recursive: true })
    await fs.copyFile(sourceFile, path.join(target, file))
  }
}

// 不 checkout、不 stash 社区目录；只导出指定提交，忽略该目录现有未提交改动。
export async function generateSnapshot({ source, ref, options, generator, productAnchor }) {
  validateOptions(options)
  const commit = resolveCommit(source, ref)
  treeEntries(source, commit)
  const temp = await fs.mkdtemp(path.join(os.tmpdir(), 'forge-upstream-'))
  try {
    const archive = path.join(temp, 'source.tar')
    const extracted = path.join(temp, 'source')
    const snapshot = path.join(temp, 'normalized')
    await fs.mkdir(extracted)
    git(source, ['archive', '--format=tar', `--output=${archive}`, commit])
    execFileSync('tar', ['-xf', archive, '-C', extracted], { timeout: 120000 })
    const version = await readPomVersion(path.join(extracted, 'forge-server/pom.xml'))
    await copyGenerator(generator, extracted)
    const args = [path.join(extracted, 'scripts/forge-create/create-project.mjs'), snapshot]
    for (const [key, value] of Object.entries(options)) args.push(`--${key}`, String(value))
    execFileSync(process.execPath, args, { cwd: extracted, timeout: 300000, maxBuffer: 8 * 1024 * 1024 })
    await cleanSnapshot(snapshot, options)
    if (productAnchor) await pinProductVersion(snapshot, options, productAnchor)
    return { temp, snapshot, commit, version }
  }
  catch (error) {
    // temp 来自 mkdtemp，失败时也只删除本次临时生成物。
    await fs.rm(temp, { recursive: true, force: true })
    throw error
  }
}

// 产品版本是私有产品的决策；社区升版只更新 upstream，不擅自给产品升版。
async function pinProductVersion(root, options, version) {
  const pom = path.join(root, `${options['artifact-prefix']}-server/pom.xml`)
  const content = await fs.readFile(pom, 'utf8')
  await fs.writeFile(pom, content.replace(/<revision>[^<]+<\/revision>/, `<revision>${version}</revision>`))
  for (const { file, manifest, expected } of await frontendVersionChanges(root)) {
    manifest.version = expected
    await fs.writeFile(file, `${JSON.stringify(manifest, null, 2)}\n`)
  }
}
