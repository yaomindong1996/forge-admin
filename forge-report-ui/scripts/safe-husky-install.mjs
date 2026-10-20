#!/usr/bin/env node
/**
 * 大屏 UI 在 monorepo 子目录；直接 husky install 会因找不到本目录 .git 失败，
 * 进而拖垮发布流水线的 pnpm install。无 git / 无 husky 场景直接跳过。
 */
import { execFileSync, execSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const pkgRoot = resolve(fileURLToPath(new URL('..', import.meta.url)))
const huskyBin = resolve(pkgRoot, 'node_modules/husky/lib/bin.js')

try {
  if (!existsSync(huskyBin)) {
    process.exit(0)
  }
  const gitRoot = execSync('git rev-parse --show-toplevel', {
    cwd: pkgRoot,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'ignore'],
  }).trim()
  const rel = relative(gitRoot, pkgRoot).replace(/\\/g, '/') || '.'
  const huskyDir = rel === '.' ? '.husky' : `${rel}/.husky`
  execFileSync(process.execPath, [huskyBin, 'install', huskyDir], {
    cwd: gitRoot,
    stdio: 'inherit',
  })
}
catch {
  // 发布机 / 无 git 上下文：跳过即可，不影响构建。
  process.exit(0)
}
