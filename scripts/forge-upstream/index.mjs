#!/usr/bin/env node
import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { bootstrap } from './bootstrap.mjs'
import { planUpgrade, applyUpgrade, verifyBaseline } from './upgrade.mjs'

const selfRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')

function argumentsOf(argv) {
  const args = { command: argv[0] || 'status' }
  const allowed = new Set(['root', 'source', 'ref', 'target', 'options', 'generator'])
  for (let i = 1; i < argv.length; i += 2) {
    const key = argv[i].replace(/^--/, '')
    if (!allowed.has(key) || !argv[i + 1] || argv[i + 1].startsWith('--')) {
      throw new Error(`非法参数：${argv[i]}`)
    }
    args[key] = argv[i + 1]
  }
  return args
}

async function main() {
  const args = argumentsOf(process.argv.slice(2))
  const root = path.resolve(args.root || selfRoot)
  if (args.command === 'init') {
    for (const key of ['source', 'ref', 'target', 'options']) {
      if (!args[key]) throw new Error(`init 缺少 --${key}`)
    }
    const options = JSON.parse(await fs.readFile(args.options, 'utf8'))
    const result = await bootstrap({ source: path.resolve(args.source), ref: args.ref,
      target: path.resolve(args.target), options, generator: path.resolve(args.generator || selfRoot) })
    console.info(JSON.stringify({ target: result.target, upstream: result.upstream,
      baseCommit: result.baseCommit }, null, 2))
  }
  else if (args.command === 'plan') {
    if (!args.source || !args.ref) throw new Error('plan 需要 --source 社区目录 --ref 社区提交')
    const plan = await planUpgrade({ root, source: path.resolve(args.source), ref: args.ref })
    console.info(JSON.stringify({ upstream: plan.upstream, changes: plan.changes,
      blocked: plan.blocked, conflicts: plan.conflicts }, null, 2))
    if (plan.blocked.length) process.exitCode = 2
  }
  else if (args.command === 'apply') console.info(JSON.stringify(await applyUpgrade(root), null, 2))
  else if (['status', 'verify'].includes(args.command)) {
    const state = await verifyBaseline(root)
    console.info(JSON.stringify({ upstream: state.upstream, baseCommit: state.baseCommit,
      mappingVersion: state.mappingVersion, generatorSha256: state.generatorSha256 }, null, 2))
  }
  else throw new Error('用法：forge:upstream init | status | plan | apply | verify；详细参数见 README.md')
}

main().catch((error) => {
  console.error(`[forge:upstream] ${error.message}`)
  process.exitCode = 1
})
